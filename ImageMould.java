package Breccia.Web.imager;

import Breccia.parser.*;
import Java.*;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeoutException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import static Breccia.Web.imager.ErrorAtFile.errHead;
import static Breccia.Web.imager.ErrorAtFile.wrnHead;
import static Breccia.Web.imager.ExternalResources.map;
import static Breccia.Web.imager.Imageability.*;
import static Breccia.Web.imager.Project.imageSibling;
import static Breccia.Web.imager.Project.logger;
import static Breccia.Web.imager.Project.looksBrecciaLike;
import static Breccia.Web.imager.Project.zeroBased;
import static Breccia.Web.imager.RemoteChangeProbe.looksProbeable;
import static Breccia.Web.imager.RemoteChangeProbe.msQueryInterval;
import static Breccia.Web.imager.RemoteChangeProbe.improbeableCause;
import static Java.Files.isDirectoryEmpty;
import static Java.Hashing.initialCapacity;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.getPosixFilePermissions;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.isWritable;
import static java.nio.file.Files.list;
import static Java.Paths.to_URI_relativePathReference;
import static Java.StringBuilding.clear;
import static Java.URI_References.isRemote;
import static Java.URIs.unfragmented;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.FINER;


/** A frame in which to form or reform a Web image.
  *
  *     @param <C> The type of source cursor used by this mould.
  */
public final class ImageMould<C extends ReusableCursor> {


    /** Partly makes a mould for `initialize` to finish.
      *
      *     @see #boundaryPath
      *     @see #outputDirectory
      *     @param errorWriter Where to report any warnings or survivable errors that occur
      *       during image formation.
      *     @throws IllegalArgumentException If `boundaryPath` is relative or non-existent.
      *     @throws IllegalArgumentException If `outputDirectory` is not an empty directory.
      */
    public ImageMould( final Path boundaryPath, ImagingOptions opt, final Path outputDirectory,
          final PrintWriter errorWriter ) {
        /* Sanity tests */ {
            Path p = boundaryPath;
            if( !exists( p )) throw new IllegalArgumentException( "No such file or directory: " + p );
            if( !p.isAbsolute() ) throw new IllegalArgumentException( "Not an absolute path: " + p );
            try {
                if( !( isDirectory(p = outputDirectory) && isDirectoryEmpty(p) )) {
                    throw new IllegalArgumentException( "Not an empty directory: " + p ); }}
            catch( IOException x ) { throw new Unhandled( x ); }}
        boundaryPathDirectory = isDirectory(boundaryPath)?  boundaryPath : boundaryPath.getParent();
        this.boundaryPath = boundaryPath;
        this.opt = opt;
        this.outputDirectory = outputDirectory;
        this.errorWriter = errorWriter; }



    public void initialize( final FileTranslator<C> translator ) { this.translator = translator; }



    /** The topmost path of the Web image, which defines its bounds.  This is an absolute path.
      * It comprises or contains the Breccian source files of the image, each accompanied
      * by any previously formed image file, a sibling namesake with a `.xht` extension.
      */
    public final Path boundaryPath;



    /** The directory of the boundary path: either `boundaryPath` itself if it is a directory,
      * otherwise its parent.
      */
    public final Path boundaryPathDirectory;



    /** Tells where to report any survivable errors in the process of image formation,
      * while ensuring the return value of `formImage` will be false.
      *
      * <p>The reporting of errors should be coded such that uncorrected errors
      * repeat with each imaging command.  No such stipulation applies to warnings.</p>
      *
      *     @see #out(int)
      *     @see #wrn()
      */
    public PrintWriter err() {
        hasFailed = true;
        return errorWriter; }



    /** Reports to the user an error at a file.
      *
      *     @see #err()
      */
    public void flag( final ErrorAtFile x ) { flag( x.file, x.getMessage() ); }



    /** Reports to the user an error in `file` at the given line number.
      *
      *     @see #err()
      *     @see #warn(Path,int,String)
      */
    public void flag( final Path file, final int lineNumber, final String message ) {
        err().println( errHead(file,lineNumber) + message ); }



    /** Reports to the user an error in `file`.
      *
      *     @see #err()
      *     @see #warn(Path,String)
      */
    public void flag( final Path file, final String message ) {
        err().println( errHead(file) + message ); }



    /** Reports to the user a parse error associated with `file`.
      *
      *     @see #err()
      */
    public void flag( final Path file, final ParseError x ) {
        flag( file, x.lineNumber, x.getMessage() ); }



    /** Forms or reforms any new files that are required to update the image,
      * writing each to the {@linkplain #outputDirectory output directory}.  Call once only.
      *
      *     @return True on success; false if a survivable error was reported to the error stream
      *       given in the constructor, in which case the image may be incomplete.
      *     @throws UserError If `boundaryPath` is unreadable, or denotes an unwritable directory
      *       or a file that looks un-Breccian.
      */
    public boolean formImage() throws UserError {
        /* Sanity tests on boundary path */ {
            Path p = boundaryPath;
            if( wouldRead(p) && !isReadable(p) ) throw new UserError( "Path is unreadable: " + p );
            if( !isDirectory(p) && !looksBrecciaLike(p) ) {
                throw new UserError( "File looks non-fractal, not a Breccian source file: " + p ); }
            if( !isWritable(p = boundaryPathDirectory) ) {
                throw new UserError( "Directory is unwritable: " + p ); }} /* While writing in itself
              is no responsibility of the mould, skipping unwritable directories is, and the gaurd
              here similar enough to its predecessors to warrant inclusion. */

      // ═══════════════════════════
      // 1. Pull in the source files, sorting them as apodictically imageable or indeterminate  [PSF]
      // ═══════════════════════════
        if( isDirectory( boundaryPath )) {
            out(1).println( "Collating source files" );
            pullDirectory( boundaryPath );
            out(1).println( "  " + imageabilityDeterminations.size() + " collated" ); }
        else pullFile( boundaryPath );
        // Now `imageabilityDeterminations` is structurally complete.  Newly started threads
        // may safely use it for all but structural modification.


      // ════════════════════════════════════
      // 2. Begin reducing the indeterminates, determining the imageability of each
      // ════════════════════════════════════
        if( opt.verbosity() >= 1 ) {
            int c = 0; // Count of indeterminates.
            for( final var det: imageabilityDeterminations.entrySet() ) {
                if( det.getValue().get() == indeterminate ) ++c; }
            if( c > 0 )  out(1).println( "Parsing source files: " + c ); }
        imageabilityDeterminations.forEach( this::formalResources_recordFrom ); // Collate the resources.
        // Now `formalResources` is structurally complete.  Newly started threads
        // may safely use it for all but structural modification.

        int countExpected = -1; // Of source files to translate, or -1 if unknown pending probes.
        if( opt.verbosity() >= 1 ) {
            final int nL = formalResources.local.size();
            final int rL = formalResources.remote.size();
            if( nL == 0 && rL == 0 ) {
                countExpected = 0;
                for( final var det: imageabilityDeterminations.entrySet() ) {
                    if( det.getValue().get() == imageable ) ++countExpected; }
                if( countExpected != 0 ) {
                    out(1).println( "Translating source files: " + countExpected ); }}
            else {
                out(1).println( "Probing referent files: " + nL + " local, " + rL + " remote" );
                out(1).println( "  Translating source files en passent" ); }}

      // Start any probes that are required for remote resources
      // ────────────────
        final Phaser barrier = new Phaser();
        final Map<String,RemoteChangeProbe> probes = new HashMap<>( initialCapacity( 0x100 )); // = 256
          // Network hosts (keys) mapped each to its assigned probe (value).
        formalResources.remote.keySet().forEach( res -> // Ensure a probe is assigned, if called for.
            probes.computeIfAbsent( res.getHost(), host -> {
                final var probe = new RemoteChangeProbe( host, ImageMould.this );
                barrier.register();
                final Thread thread = new Thread( probe, "Probe thread `" + host + "`" ) {
                    public @Override void run() {
                        super.run();
                        barrier.arrive(); }};
                thread.setDaemon( true );
                thread.start();
                return probe; }));

      // Probe the local resources
      // ─────────────────────────
        formalResources.local.forEach( (res, dependants) -> {
            final FileTime resTime; {
                FileTime t = null;
                assert exists( res ); /* Assured by the API of `formalResources.local`.
                  Therefore any failure to read the timestamp of `res` below is unexpected. */
                try { t = getLastModifiedTime( res ); }
                catch( final IOException x ) {
                    logger.warning( () -> "Forcefully re-imaging the dependants of resource `" + res
                      + "` its timestamp being unreadable: " + x ); } // [LUR]
                resTime = t; }
            dependants.forEach( dep -> {
                final ImageabilityReference depImageability = imageabilityDeterminations.get( dep );
                if( depImageability.get() != indeterminate ) return; /* Already determined by an
                  earlier pass of this probe, or (however unlikely) one of the slow remote probes. */
                boolean toReformImage = true;
                if( resTime != null ) { // So letting the null case be forcefully reimaged, as per above.
                    final Path depImage = imageSibling( dep );
                    assert exists( depImage ); /* Guaranteed by the `depImageability` guard above,
                      because already `dep` would have been marked as `imageable` if it had no image.
                      Therefore any failure to read the timestamp of `depImage` below is unexpected. */
                    try { toReformImage = resTime.compareTo(getLastModifiedTime(depImage)) >= 0; }
                      // Viz. iff the formal resource has changed since the image was formed.
                    catch( final IOException x ) {
                        logger.warning( () ->
                          "Forcefully reimaging the source file, the timestamp of its image `"
                          + depImage + "` being unreadable: " + x ); }} // [LUR]
                if( toReformImage ) depImageability.set( imageable ); }); });


      // ═══════════════════════════
      // 3. Translate the imageables as they are determined, extending the image to its bounds
      // ═══════════════════════════
        boolean isFinalPass;
        if( probes.size() == 0 ) {
            isFinalPass = true; // Only one pass is required.
            barrier.forceTermination(); } // Just to be tidy.
        else isFinalPass = false; // At least two will be required.
        final ArrayList<Path> files = new ArrayList<>( // List of translated source files.
          /*initial capacity*/0x1000 ); // = 4096
        for( ;; ) {
            int c = 0; // Count of imageables found during the present pass.

          // Translate any imageables now determined, so forming part of the image
          // ────────────────────────
            for( final var det: imageabilityDeterminations.entrySet() ) {
                final ImageabilityReference iR = det.getValue();
                if( iR.get() != imageable ) continue;
                ++c;
                final Path sourceFile = det.getKey();
                final Path sourceFileRelative = boundaryPathDirectory.relativize( sourceFile );
                boolean wasTranslated = false;
                try {
                    translator.translate( sourceFile,
                      outputDirectory.resolve(sourceFileRelative).getParent() );
                    wasTranslated = true; }
                catch( final ParseError x ) { flag( sourceFile, x ); }
                catch( final ErrorAtFile x ) { flag( x ); }
                if( wasTranslated ) {
                    files.add( sourceFileRelative );
                    iR.set( imaged ); }
                else iR.set( unimageable ); }
            if( isFinalPass ) break;
            if( c > 0 ) continue; // One good turn deserves another by making it likelier.

          // Await further reduction of indeterminates
          // ───────────────────────
            try { barrier.awaitAdvanceInterruptibly​( /*phase*/0, msQueryInterval, MILLISECONDS ); }
            catch( final InterruptedException x ) {
                Thread.currentThread().interrupt(); // Avoid hiding the fact of interruption.
                throw new UnsourcedInterrupt( x ); }
            catch( TimeoutException x ) { continue; } // Reduction is ongoing.
            isFinalPass = true; } // Reduction is complete, the next pass is final.
        if( files.size() != countExpected  ||  countExpected != 0  &&  opt.verbosity() >= 2 ) {
            out(1).println( "  " + files.size() + " translated" );
            if( opt.verbosity() >= 2 ) for( final Path f: files ) out(2).println( "    ↶ " + f ); }


      // ═════════════════════════
      // 4. Finish the image files
      // ═════════════════════════
        countExpected = 0;
        if( opt.verbosity() >= 1 ) {
            for( final var det: imageabilityDeterminations.entrySet() ) {
                if( det.getValue().get() == imaged ) ++countExpected; }
            if( countExpected != 0 ) out(1).println( "Finishing image files: " + countExpected ); }
        files.clear(); // List of finished image files.
        for( final var det: imageabilityDeterminations.entrySet() ) {
            final ImageabilityReference iR = det.getValue();
            if( iR.get() != imaged ) continue;
            final Path sourceFile = det.getKey();
            final Path imageFileRelative = imageSibling( boundaryPathDirectory.relativize( sourceFile ));
            try {
                translator.finish( sourceFile, outputDirectory.resolve( imageFileRelative ));
                files.add( imageFileRelative ); }
            catch( final ErrorAtFile x ) { flag( x ); }}
        if( files.size() != countExpected  ||  countExpected != 0  &&  opt.verbosity() >= 2 ) {
            out(1).println( "  " + files.size() + " finished" );
            if( opt.verbosity() >= 2 ) for( final Path f: files ) out(2).println( "    → " + f ); }
        return !hasFailed; }



    /** Returns a multi-line string comprising an echo of a URI reference together with a column marker.
      *
      *     @param ref The URI reference.
      *     @param p A character pointer formed on the original source line of `ref`.
      *       The value of its `column` field will be ignored if `isAlteredRef`.
      *     @param isAlteredRef Whether `ref` has been altered (by `-reference-mapping` translation)
      *       from the original reference given in source.
      */
    public String markedLine( String ref, CharacterPointer p, boolean isAlteredRef ) {
        return markedLine( ref, p, isAlteredRef, 0 ); }



    /** Returns a multi-line string comprising an echo of a URI reference together with a column marker.
      *
      *     @param ref The URI reference.
      *     @param p A character pointer formed on the original source line of `ref`.
      *       The value of its `column` field will be ignored if `isAlteredRef`.
      *     @param isAlteredRef Whether `ref` has been altered (by `-reference-mapping` translation)
      *       from the original reference given in source.
      *     @param c The zero-based offset in `ref` of the character whose column to mark.
      *       It will be used only if `isAlteredRef`. *//*
      *     @paramImplied #stringBuilder2
      */
    public String markedLine( final String ref, final CharacterPointer p, final boolean isAlteredRef,
          final int c ) {
        final StringBuilder b = clear( stringBuilder2 );
        if( isAlteredRef ) {
            b.append( IntralineCharacterPointer.markedLine( "      ", ref, c, gcc ));
            b.append( "\n    Source line, with original reference:  "
              + "(before `-reference-mapping` translation)\n" );
            b.append( p.line ); }
        else b.append( p.markedLine() );
        return b.toString(); }



    public final ImagingOptions opt;



    /** The output stream for user feedback of verbosity level `v`.  If `v` is greater than the level
      * allowed by command option `-verbosity`, then this method returns a do-nothing dummy stream.
      * Otherwise it returns `System.{@linkplain java.lang.System#out out}`.
      *
      *     @param v Either 1 or 2.
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#verbosity,verbosity-0-'>
      *         Command option `-verbosity`</a>
      *     @see #err()
      *     @see #wrn()
      */
    public PrintStream out( final int v ) { return opt.out( v ); }



    /** The directory in which to write any newly formed image files.
      */
    public final Path outputDirectory;



    /** Warns the user of something in `file` at the line number of the given character pointer.
      *
      *     @see #wrn()
      */
    public void warn( final Path file, final CharacterPointer p, final String message ) {
        warn( file, p.lineNumber, message ); }



    /** Warns the user of something in `file` at the given line number.
      *
      *     @see #wrn()
      *     @see #flag(Path,int,String)
      */
    public void warn( final Path file, final int lineNumber, final String message ) {
        wrn().println( wrnHead(file,lineNumber) + message ); }



    /** Warns the user of something in `file`.
      *
      *     @see #wrn()
      *     @see #flag(Path,String)
      */
    public void warn( final Path file, final String message ) {
        wrn().println( wrnHead(file) + message ); }



    /** Warns the user of something in `file` at the line number of the given character pointer,
      * on condition the warning does not duplicate an earlier one.
      *
      *     @see #wrn()
      */
    public void warnOnce( final Path file, final CharacterPointer p, final String message ) {
        warnOnce( file, p.lineNumber, message ); }



    /** Warns the user of something in `file` at the given line number,
      * on condition the warning does not duplicate an earlier one.
      *
      *     @see #wrn()
      */
    public void warnOnce( final Path file, final int lineNumber, final String message ) {
        final String report = wrnHead(file,lineNumber) + message;
        if( warningsIssued.add( report )) wrn().println( report ); }



    /** Where to report any warnings in the process of image formation.
      *
      *     @see #err()
      *     @see #out(int)
      */
    public PrintWriter wrn() { return errorWriter; }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private final PrintWriter errorWriter; /* Do not write to it directly through this field.
      Instead write to it through the wrapper methods `err` and `wrn`. */



    /** A record of the formal imaging resources of source files whose imageability is initally
      * indeterminate.  Here ‘formal’ means that the content of the resource determines the form
      * of the image of the source file that refers to (or otherwise makes use of) that resource.
      */
    final ExternalResources formalResources = new ExternalResources();



    /** @param f The path of a source file.
      * @param gRef A reference from `f` to a formal resource, encapsulated as a `Granum`.
      * @param sRef The reference in string form, after any applicable `-reference-mapping`
      *   translations.
      * @param isAlteredRef Whether `sRef` was actually changed by such translation.
      * @return Whether the present method call actually recorded the resource (by its `sRef`),
      *   deeming it eligible for inclusion in one of the `formalResources` maps.
      * @see #formalResources
      */
    private boolean formalResources_record( final Path f, final Granum gRef, final String sRef,
          final boolean isAlteredRef ) { /* For what follows,
        cf. the comparably structured code of `BreccianFileTranslator.href`. [RC] */

        final URI uRef; { // The reference in parsed `URI` form.
            try { uRef = new URI( sRef ); }
            catch( final URISyntaxException x ) {
                final int c = isAlteredRef ? 0/*guaranteed within bounds of the unaltered `gRef`*/
                  : zeroBased( x.getIndex() );
                final CharacterPointer p = gRef.characterPointer( c );
                warnOnce( f, p, message( sRef, x, p,isAlteredRef ));
                return false; }} // Without mapping ∵ `x` leaves the intended resource unclear.

      // remote  [RC]
      // ┈┈┈┈┈┈
        if( isRemote( uRef )) {             // Then the resource would be reachable through a network,
            if( !looksProbeable( uRef )) { // the reference being a URI or network-path reference. [RR]
                final CharacterPointer p = gRef.characterPointer();
                final String message = improbeableCause + '\n' + markedLine( sRef, p, isAlteredRef );
                pendingWarnings.add( new Warning( p.lineNumber, message, /*when private*/null, null,
                  gRef.xuncFractalDescent() ));
                return false; } // Without mapping ∵ `formalResources.remote` forbids improbeables.
            map( formalResources.remote, /*resource*/unfragmented(uRef).normalize(), /*dependant*/f ); }

      // local  [RC]
      // ┈┈┈┈┈
        else { /* The resource would be reachable through a file system, the reference being
              an absolute-path reference or relative-path reference. [RR] */
            final Path pRef; { // The reference parsed and resolved as a local file path.
                try { pRef = f.resolveSibling( toPath( uRef, f )); }
                catch( final IllegalArgumentException x ) {
                    final CharacterPointer p = gRef.characterPointer();
                    warnOnce( f, p, x.getMessage() + '\n' + markedLine(sRef,p,isAlteredRef) );
                    return false; }} // Without mapping ∵ `x` leaves the intended resource unclear.
            if( !exists( pRef )) {
                final StringBuilder bMessage = clear( stringBuilder );
                final boolean isTransX = isTransX( pRef, bMessage );
                final boolean wouldPrivatizationSuppress = isAlteredRef && isTransX;
                final CharacterPointer p = gRef.characterPointer();
                final String markedLine = markedLine( sRef, p, isAlteredRef );
                final StringBuilder bMessageWhenPrivate;
                final Level level; {
                    if( wouldPrivatizationSuppress ) {
                        bMessageWhenPrivate = clear( stringBuilder2 ).append( bMessage ).append( ":\n" )
                          .append( markedLine ).append( "\n    Falling back to the original reference");
                        level = FINER;  /* Merely logging in the private case, because this type
                          of inaccessibility is common when a private reference is altered
                          by a `-reference-mapping` translation. */
                        bMessage.append( "; consider marking this reference as private" ); }
                    else {
                        bMessageWhenPrivate = bMessage;
                        level = null; }}
                bMessage.append( ":\n" ).append( markedLine );
                pendingWarnings.add( new Warning( p.lineNumber, bMessage.toString(),
                  bMessageWhenPrivate.toString(), level, gRef.xuncFractalDescent() ));
                return false; } // Without mapping ∵ `formalResources.local` forbids broken references.
            map( formalResources.local, /*resource*/pRef.normalize(), /*dependant*/f ); }
        return true; }



    /** Records all formal resources of source file `f`, provided `f` is of indeterminate imageability;
      * otherwise does nothing.
      *
      *     @see #formalResources
      *     @param f The path of a source file.
      *     @param iR The present imageability determination of `f`.
      */
    private void formalResources_recordFrom( final Path f, final ImageabilityReference iR ) {
        if( iR.get() != indeterminate ) return;
        pendingWarnings.clear();
        final C in = translator.sourceCursor();
        try {
            in.perStateConditionally( f, state -> {
                final Granum gRef; { // The reference encapsulated as a `Granum`.
                    try { gRef = translator.formalReferenceAt( in ); }
                    catch( final ParseError x ) {
                        flag( f, x );
                        iR.set( unimageable ); // The source fails to parse.
                        return /*to continue parsing*/false; } // No point, the parser has halted.
                    if( gRef == null/*not a formal reference*/ ) return /*to continue parsing*/true; } /*

                For what follows, cf. `BreccianFileTranslator.finish(Path,Element)`. */
                final String sRefOriginal = gRef.text().toString(); // The reference in string form.
                final String sRef = translate( sRefOriginal, f );
                  // Applying any `-reference-mapping` translations.
                final boolean isAlteredRef = !sRef.equals( sRefOriginal );
                if( !formalResources_record( f, gRef, sRef, isAlteredRef )  &&  isAlteredRef ) {
                    formalResources_record( f, gRef, sRefOriginal, /*isAlteredRef*/false ); } /*
                      Falling back to `sRefOriginal` (assuming it is equivalent for the purpose);
                      so verifying that at least *it* gets recorded, else warning the user. */
                return /*to continue parsing*/true; });
            while( !in.state().isFinal() ) in.next(); } // API requirement of `isPrivatized`, below.
        catch( final ParseError x ) {
            flag( f, x );
            iR.set( unimageable );
            return; }
        for( final Warning w: pendingWarnings ) {
            final boolean isPrivate = in.isPrivatized( w.xuncFractalDescent );
            final String m = isPrivate ? w.messageWhenPrivate : w.message;
            if( m == null ) continue; // Suppress the warning.
            if( isPrivate && w.level != null ) logger.log( w.level, wrnHead(f,w.lineNumber) + m );
            else warnOnce( f, w.lineNumber, m ); }}



    final GraphemeClusterCounter gcc = new GraphemeClusterCounter();



    private boolean hasFailed;



    /** Source files (keys) mapped each to the present state of its imageability determination (value).
      * The map is thread safe on condition of no concurrent structural modification,
      * structural modification being defined as for `{@linkplain HashMap HashMap}`.
      *
      */ @Async // See `start` of remote probe threads in `formImage`.
    final Map<Path,ImageabilityReference> imageabilityDeterminations = new HashMap<>(
      initialCapacity( 0x2000/*source files*/ )); // = 8192



    /** Map of records of image files that are file-system accessible.
      * Each entry comprises an absolute, normalized file path to an image file (key)
      * mapped to an linear-order array (value) of that file’s imaged body fracta.
      */
    final Map<Path,ImagedBodyFractum[]> imageFilesLocal = new HashMap<>(
      initialCapacity( 0x1000 )); // = 4096



    /** Whether the inaccessibility of `file` is of a type known to result
      * from the `-reference-mapping` translation of a private reference.
      *
      *     @param file A file that `{@linkplain java.nio.file.Files#exists(Path) exists}` not.
      *     @param b Where to append a description of the type of inaccessibility in terms of its
      *       intermediate cause, e.g. ‘No such file or directory’ or ‘File access denied’.
      */
    boolean isTransX( final Path file, final StringBuilder b ) {
        try {
            getPosixFilePermissions( file ); // Merely to learn the cause of inaccessibility.
            assert false;                   // Always it should throw an exception.
            b.append( "No access to this file or directory, cause unknown" );
            return false; }
        catch( final AccessDeniedException x ) {
            b.append( "File access denied" );
            return true; }
        catch( final NoSuchFileException x ) {
            b.append( "No such file or directory" );
            return true; }
        catch( final IOException x ) {
            b.append( x.toString() );
            return false; }}



    /** Returns a multi-line description of a malformed URI reference,
      * fit to include as the message of a user report.
      *
      *     @param ref The malformed URI reference.
      *     @param x The malformation detected in `ref`.
      *     @param p A character pointer formed on the original source line of `ref`.
      *       The value of its `column` field will be ignored if `isAlteredRef`.
      *     @param isAlteredRef Whether `ref` has been altered (by `-reference-mapping` translation)
      *       from the original reference given in source.
      */
    String message( String ref, final URISyntaxException x, CharacterPointer p, boolean isAlteredRef ) {
        return "Malformed URI reference: " + x.getReason() + '\n'
          + markedLine( ref, p, isAlteredRef, zeroBased(x.getIndex()) ); }



    private final ArrayList<Warning> pendingWarnings = new ArrayList<>();



    /** @param d The path of a source directory to pull into the mould.
      */
    private void pullDirectory( final Path d ) {
        try( final Stream<Path> pp = list( d )) {
            for( final Path p: (Iterable<Path>)pp::iterator ) pullPath( p ); }
        catch( IOException x ) { throw new Unhandled( x ); }}



    /** @param f The path of a potential source file to pull into the mould.
      */
    private void pullFile( final Path f ) {
        if( !looksBrecciaLike( f )) return;
        imageabilityDeterminations.computeIfAbsent( f, f_ -> {
            final Imageability i;
            if( opt.toForce() ) i = imageable;
            else {
                final Path fI = imageSibling( f );
                try {
                    if( !exists(fI) || getLastModifiedTime(f).compareTo(getLastModifiedTime(fI)) >= 0 ) {
                        i = imageable; }
                    else i = indeterminate; }
                catch( IOException x ) { throw new Unhandled( x ); }}
            return new ImageabilityReference( i ); }); }



    /** @param p A path to pull into the mould.
      */
    private void pullPath( final Path p ) {
        if( isReadable( p )) { // Herein cf. `formImage`.
            if( isDirectory( p )) {
                if( isWritable( p )) pullDirectory( p );
                else warn( p, "Skipping this unwritable directory" ); }
            else pullFile( p ); }
        else if( wouldRead( p )) warn( p, "Skipping this unreadable path" ); }



    private final StringBuilder stringBuilder = new StringBuilder( /*initial capacity*/0x200 ); // = 512



    private final StringBuilder stringBuilder2 = new StringBuilder( /*initial capacity*/0x200 ); // = 512



    /** Translates to a `Path` instance the given URI reference, with support for tilde expansion.
      * Any tilde prefix is taken to represent the author’s home directory.
      *
      *     @see Java.Paths#toPath(URI,Path)
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#author-home-,author-home-,path'>
      *         Command option `-author-home-directory`</a>
      *     @throws IllegalArgumentException If `reference` has a query or fragment component.
      */
    Path toPath( final URI reference, final Path referrer ) {
        Path p = Paths.toPath( reference, referrer );
        if( !p.isAbsolute() ) {
            final int n = p.getNameCount();
            assert n != 0; // Guaranteed for a relative path, even if empty.
            if( p.getName(0).toString().equals( "~" )) {
                if( n == 1 ) p = opt.authorHomeDirectory();
                else p = opt.authorHomeDirectory().resolve( p.subpath( 1, n )); }}
        return p; }



    /** Applies any due `-reference-mapping` translations to the given reference and returns the result.
      *
      *     @param reference A URI reference.
      *     @param referrer The referring source file, wherein the reference is contained.
      *     @return The same `reference` instance if translation failed; otherwise the translated result
      *       in the form of a new string of equal or different content.
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#reference-ma,reference-ma,translation'>
      *         Command option `-reference-mapping`</a>
      */
    String translate( final String reference, final Path referrer ) {
        for( final var tt: opt.referenceMappings() ) { // For each mapping given on the command line.
            for( final ReferenceTranslation t: tt ) { // For each translation given in the mapping.
                final Matcher m = t.matcher().reset( reference );
                if( m.find() ) {
                    final StringBuilder b = clear( stringBuilder );
                    final String r; { // The effective replacement string.
                        if( t.isBounded() ) {
                            assert referrer.startsWith( boundaryPath );
                            b.append( to_URI_relativePathReference(
                              referrer.getParent().relativize( boundaryPathDirectory )));
                            if( b.length() == 0 ) b.append( '.' ); /* When the `referrer`
                              sits directly in the `boundaryPathDirectory`. */
                            b.append( t.replacement() );
                            r = b.toString();
                            clear( b ); }
                        else r = t.replacement(); }
                    do m.appendReplacement( b, r ); while( m.find() );
                    m.appendTail( b );
                    return b.toString(); }}} // Translation succeeded.
        return reference; } // Translation failed.



    private FileTranslator<C> translator; // Do not modify after `initialize`.



    private final HashSet<String> warningsIssued = new HashSet<>();



    /** Whether path `p` would be read during image formation if it were readable.
      */
    private static boolean wouldRead( final Path p ) { return isDirectory(p) || looksBrecciaLike(p); }



   // ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀


    /** A pending warning to the user, apropos of a granum, whose final issue depends on whether
      * the granum turns out to be private.
      *
      *     @param message Message in case of an unprivatized granum,
      *        or null to suppress the warning in this case.
      *     @param messageWhenPrivate Message in case of a privatized granum,
      *        or null to suppress the warning in this case.
      *     @param level Logging level in case of a privatized granum,
      *        or null to issue the report via `wrn` in this case.
      */
    private static record Warning( int lineNumber, String message, String messageWhenPrivate,
      Level level, int[] xuncFractalDescent ) {}}



// NOTES
// ─────
//   LUR  Logging of unexpected yet recoverable IO errors.  Aside from avoiding a flood of reports
//        on the `err` stream, these lines of code merely serve as examples (the only ones at present)
//        of efficient report formation for logging purposes.
//
//   PSF  Pulling in the source files.  Here using a streamlined process, rather than `pullPath`
//        whose added testing and messaging would be redundant for this topmost (boundary) path.
//
//   RC · Referencing code.  Cf. the comparably structured code of `BreccianFileTranslator.href`.
//
//   RR · Relative reference.  https://www.rfc-editor.org/rfc/rfc3986#section-4.2
//
//   SM · Structural modification of a `HashMap` defined.
//        https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/HashMap.html




                                                   // Copyright © 2020-2022  Michael Allan.  Licence MIT.
