package Breccia.Web.imager;

import Breccia.parser.*;
import Java.*;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import static Breccia.Web.imager.ErrorAtFile.errHead;
import static Breccia.Web.imager.ErrorAtFile.errMsg;
import static Breccia.Web.imager.ErrorAtFile.wrnHead;
import static Breccia.Web.imager.ExternalResources.map;
import static Breccia.Web.imager.Imageability.*;
import static Breccia.Web.imager.Project.imageSibling;
import static Breccia.Web.imager.Project.logger;
import static Breccia.Web.imager.Project.looksBreccian;
import static Breccia.Web.imager.Project.malformationIndex;
import static Breccia.Web.imager.Project.malformationMessage;
import static Breccia.Web.imager.RemoteChangeProbe.looksProbeable;
import static Breccia.Web.imager.RemoteChangeProbe.msQueryInterval;
import static Breccia.Web.imager.RemoteChangeProbe.improbeableMessage;
import static Java.Files.isDirectoryEmpty;
import static Java.Hashing.initialCapacity;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.isWritable;
import static java.nio.file.Files.list;
import static Java.Paths.toPath;
import static Java.Paths.toRelativePathReference;
import static Java.StringBuilding.clear;
import static Java.URI_References.isRemote;
import static Java.URIs.unfragmented;
import static java.util.concurrent.TimeUnit.MILLISECONDS;


/** A frame in which to form or reform a Web image.
  *
  *     @param <C> The type of source cursor used by this mould.
  */
public final class ImageMould<C extends ReusableCursor> {


    /** Partly makes a mould for `initialize` to finish.
      *
      *     @see #boundaryPath
      *     @see #outDirectory
      *     @param errorWriter Where to report any warnings or survivable errors that occur
      *       during image formation.
      *     @throws IllegalArgumentException If `boundaryPath` is relative or non-existent.
      *     @throws IllegalArgumentException If `outDirectory` is not an empty directory.
      */
    public ImageMould( final Path boundaryPath, ImagingOptions opt, final Path outDirectory,
          final PrintWriter errorWriter ) {
        /* Sanity tests */ {
            Path p = boundaryPath;
            if( !exists( p )) throw new IllegalArgumentException( "No such file or directory: " + p );
            if( !p.isAbsolute() ) throw new IllegalArgumentException( "Not an absolute path: " + p );
            try {
                if( !( isDirectory(p = outDirectory) && isDirectoryEmpty(p) )) {
                    throw new IllegalArgumentException( "Not an empty directory: " + p ); }}
            catch( IOException x ) { throw new Unhandled( x ); }}
        boundaryPathDirectory = isDirectory(boundaryPath)?  boundaryPath : boundaryPath.getParent();
        this.boundaryPath = boundaryPath;
        this.opt = opt;
        this.outDirectory = outDirectory;
        this.errorWriter = errorWriter; }



    public final void initialize( final FileTranslator<C> translator ) {
        this.translator = translator; }



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



    /** A record of the formal imaging resources for source files whose imageability
      * is initally indeterminate.  Here ‘formal’ means determining of image form.
      */
    final ExternalResources formalResources = new ExternalResources();



    /** Forms or reforms any new files that are required to update the image,
      * writing each to the {@linkplain #outDirectory output directory}.  Call once only.
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
            if( !isDirectory(p) && !looksBreccian(p) ) {
                throw new UserError( "File looks un-Breccian: " + p ); }
            if( !isWritable(p = boundaryPathDirectory) ) {
                throw new UserError( "Directory is unwritable: " + p ); }} /* While writing in itself
              is no responsibility of the mould, skipping unwritable directories is, and the gaurd
              here similar enough to its predecessors to warrant inclusion. */

      // ═══════════════════════════
      // 1. Pull in the source files, sorting them as apodictically imageable or indeterminate
      // ═══════════════════════════
        if( isDirectory( boundaryPath )) {   // A streamlined process versus that of `pullPath`
            pullDirectory( boundaryPath ); } // whose added testing and messaging would be redundant
        else pullFile( boundaryPath );       // for this topmost path.
        // Now `imageabilityDeterminations` is structurally complete.  Newly started threads
        // may safely use it for all but structural modification.


      // ════════════════════════════════════
      // 2. Begin reducing the indeterminates, determining the imageability of each
      // ════════════════════════════════════
        imageabilityDeterminations.forEach( this::formalResources_recordFrom ); // Collate the resources.
        // Now `formalResources` is structurally complete.  Newly started threads
        // may safely use it for all but structural modification.

      // Start any probes that are required for remote resources
      // ────────────────
        final Phaser barrier = new Phaser();
        final Map<String,RemoteChangeProbe> probes = new HashMap<>( initialCapacity( 256 ));
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
                    logger.warning( () -> "Forcefully reimaging the dependants of resource `" + res
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
        out(2).println( "Translating source to image files" );
        boolean isFinalPass;
        if( probes.size() == 0 ) {
            isFinalPass = true; // Only one pass is required.
            barrier.forceTermination(); } // Just to be tidy.
        else isFinalPass = false; // At least two will be required.
        int count = 0; // Count of translated source files.
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
                out(1).println( "  ↶ " + sourceFileRelative );
                try {
                    translator.translate( sourceFile,
                      outDirectory.resolve(sourceFileRelative).getParent() );
                    ++count;
                    wasTranslated = true; }
                catch( final ParseError x ) { err().println( errMsg( sourceFile, x )); }
                catch( final ErrorAtFile x ) { err().println( errMsg( x )); }
                iR.set( wasTranslated? imaged: unimageable ); }
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
        if( count == 0 ) out(2).println( "    none tranformed" );


      // ═════════════════════════
      // 4. Finish the image files
      // ═════════════════════════
        out(2).println( "Finishing the image files" );
        count = 0; // Count of finished image files.
        for( final var det: imageabilityDeterminations.entrySet() ) {
            final ImageabilityReference iR = det.getValue();
            if( iR.get() != imaged ) continue;
            final Path sourceFile = det.getKey();
            final Path imageFileRelative = imageSibling( boundaryPathDirectory.relativize( sourceFile ));
            out(1).println( "  → " + imageFileRelative );
            try {
                translator.finish( sourceFile, outDirectory.resolve( imageFileRelative ));
                ++count; }
            catch( final ErrorAtFile x ) { err().println( errMsg( x )); }}
        if( count == 0 ) out(2).println( "    none finished" );
        return !hasFailed; }



    /** Source files (keys) mapped each to the present state of its imageability determination (value).
      * The map is thread safe on condition of no concurrent structural modification,
      * structural modification being defined as for `{@linkplain HashMap HashMap}`.
      *
      */ @Async // See `start` of remote probe threads in `formImage`.
    final Map<Path,ImageabilityReference> imageabilityDeterminations = new HashMap<>(
      initialCapacity( 8192/*source files*/ ));



    public final ImagingOptions opt;



    /** The output stream for user feedback of verbosity level `v`.  If `v` is greater than the level
      * allowed by command option `--verbosity`, then this method returns a do-nothing dummy stream.
      * Otherwise it returns `System.{@linkplain java.lang.System#out out}`.
      *
      *     @param v Either 1 or 2.
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht'>
      *         Command option `--verbosity`</a>
      *     @see #err()
      *     @see #wrn()
      */
    public PrintStream out( final int v ) { return opt.out( v ); }



    /** The directory in which to write any newly formed image files.
      */
    public final Path outDirectory;



    /** Where to report any warnings in the process of image formation.
      *
      *     @see #err()
      *     @see #out(int)
      */
    public PrintWriter wrn() { return errorWriter; }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private final PrintWriter errorWriter; /* Do not write to it directly through this field.
      Instead write to it through the wrapper methods `err` and `wrn`. */



    /** Records all formal resources of source file `f`, provided `f` is of indeterminate imageability;
      * otherwise does nothing.
      *
      *     @see #formalResources
      *     @param f The path of a source file.
      *     @param iR The present imageability determination of `f`.
      */
    private void formalResources_recordFrom( final Path f, final ImageabilityReference iR ) {
        if( iR.get() != indeterminate ) return;
        imps.clear(); // List of improbeable-reference occurences.
        final C in = translator.sourceCursor();
        try {
            in.perStateConditionally( f, state -> { /*
                For what follows, cf. `BreccianFileTranslator.finish(Path,Element)`. [RC] */

                final Granum mRef; { // The reference encapsulated as a `Granum`.
                    try { mRef = translator.formalReferenceAt( in ); }
                    catch( final ParseError x ) {
                        err().println( errMsg( f, x ));
                        iR.set( unimageable ); // The source fails to parse.
                        return /*to continue parsing*/false; } // No point, the parser has halted.
                    if( mRef == null/*not a formal reference*/ ) return /*to continue parsing*/true; }
                final String sRefOriginal = mRef.text().toString(); // The reference in string form.
                final String sRef = translate( sRefOriginal, f );
                  // Applying any `--reference-mapping` translations.
                final URI uRef; { // The reference in parsed `URI` form.
                    try { uRef = new URI( sRef ); }
                    catch( final URISyntaxException x ) {
                        final CharacterPointer p = mRef.characterPointer( malformationIndex( x ));
                        err().println( errHead(f,p.lineNumber) + malformationMessage(x,p) );
                        iR.set( unimageable ); // Do not image the file. [UFR]
                        return // Without mapping ∵ `x` leaves the intended resource unclear.
                          /*to continue parsing*/true; }} // To report any further errors.

              // remote  [RC]
              // ┈┈┈┈┈┈
                if( isRemote( uRef )) { // Then the resource would be reachable through a network.
                    if( !looksProbeable( uRef )) {
                        imps.add( new Improbeable( mRef.characterPointer(), mRef.xuncFractalDescent() ));
                        iR.set( unimageable ); // Do not image the file. [UFR]
                        return // Without mapping ∵ `formalResources.remote` forbids improbeables.
                          /*to continue parsing*/true; } // To report/detect any further errors.
                    map( formalResources.remote, /*resource*/unfragmented(uRef).normalize(),
                      /*dependant*/f ); }

              // local  [RC]
              // ┈┈┈┈┈
                else { /* The resource would be reachable through a file system, the reference
                      being an absolute-path reference or relative-path reference [RR]. */
                    final Path pRef; { // The reference parsed and resolved as a local file path.
                        try { pRef = f.resolveSibling( toPath( uRef )); }
                        catch( final IllegalArgumentException x ) {
                            final CharacterPointer p = mRef.characterPointer();
                            err().println( errHead(f,p.lineNumber) + x.getMessage() + '\n'
                              + p.markedLine() );
                            iR.set( unimageable ); // Do not image the file. [UFR]
                            return // Without mapping ∵ `x` leaves the intended resource unclear.
                              /*to continue parsing*/true; }} // To report any further errors.
                    if( !exists( pRef )) { /* Then let the translator warn of it.  Unlike the present
                          code, the translator tests the existence of all referents, whether formal or
                          informal, making it a better place to issue reports of broken references. */
                     // iR.set( imageable ); // Therefore force imaging of this file.
                    //// Overkill, `err` says warnings need not ‘repeat with each imaging command’.
                        return // Without mapping ∵ `formalResources.local` forbids broken references.
                          /*to continue parsing*/true; } // For sake of reporting any further errors.
                    map( formalResources.local, /*resource*/pRef.normalize(), /*dependant*/f ); }
                return true; });
            while( !in.state().isFinal() ) in.next(); } // API requirement of `isPrivatized`, below.
        catch( final ParseError x ) {
            err().println( errMsg( f, x ));
            iR.set( unimageable );
            return; }
        for( final Improbeable imp: imps ) if( !in.isPrivatized( imp.xuncFractalDescent )) {
            final CharacterPointer p = imp.characterPointer;
            err().println( errHead(f,p.lineNumber) + improbeableMessage(p) ); }}



    private boolean hasFailed;



    private final ArrayList<Improbeable> imps = new ArrayList<>();



    /** @param d The path of a source directory to pull into the mould.
      */
    private void pullDirectory( final Path d ) {
        try( final Stream<Path> pp = list( d )) {
            for( final Path p: (Iterable<Path>)pp::iterator ) pullPath( p ); }
        catch( IOException x ) { throw new Unhandled( x ); }}



    /** @param f The path of a potential source file to pull into the mould.
      */
    private void pullFile( final Path f ) {
        if( !looksBreccian( f )) return;
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
                else wrn().println( wrnHead(p) + "Skipping this unwritable directory" ); }
            else pullFile( p ); }
        else if( wouldRead( p )) wrn().println( wrnHead(p) + "Skipping this unreadable path" ); }



    /** Applies any due `--reference-mapping` translations to the given reference and returns the result.
      *
      *     @param ref A URI reference.
      *     @param f The path of the referring source file, wherein `ref` is contained.
      *     @return The same `ref` if translation failed; otherwise the translated result in a new string
      *       of equal or different content.
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht'>
      *         Command option `--reference-mapping`</a>
      */
    String translate( final String ref, final Path f ) {
        for( final var tt: opt.referenceMappings() ) { // For each mapping given on the command line.
            for( final ReferenceTranslation t: tt ) { // For each translation given in the mapping.
                final Matcher m = t.matcher().reset( ref );
                if( m.find() ) {
                    final StringBuilder b = clear( stringBuilder );
                    final String r; {
                        if( t.isBounded() ) {
                            assert f.startsWith( boundaryPath ); /* That ‘the boundary path itself
                              will not appear in the replacement string that results.’
                              http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht */
                            b.append( toRelativePathReference(
                              f.getParent().relativize( boundaryPathDirectory )));
                            if( b.length() == 0 ) b.append( '.' ); // `f` sits in `boundaryPathDirectory`
                            b.append( t.replacement() );
                            r = b.toString();
                            clear( b ); }
                        else r = t.replacement(); }
                    do m.appendReplacement( b, r ); while( m.find() );
                    m.appendTail( b );
                    return b.toString(); }}} // Translation succeeded.
        return ref; } // Translation failed.



    private final StringBuilder stringBuilder = new StringBuilder( /*initial capacity*/0x100/*or 256*/ );



    private FileTranslator<C> translator; // Do not modify after `initialize`.



    /** Whether path `p` would be read during image formation if it were readable.
      */
    private static boolean wouldRead( final Path p ) { return isDirectory(p) || looksBreccian(p); }



   // ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀


    /** An occurence of an improbeable remote reference.
      *
      *     @see RemoteChangeProbe#looksProbeable(URI)
      */
    private static record Improbeable( CharacterPointer characterPointer, int[] xuncFractalDescent ) {}}



// NOTES
// ─────
//   LUR  Logging of unexpected yet recoverable IO errors.  Aside from avoiding a flood of reports
//        on the `err` stream, these lines of code merely serve as examples (the only ones at present)
//        of efficient report formation for logging purposes.
//
//   RC · Cf. the comparably structured referencing code @ `ImageMould.formalResources_recordFrom`.
//
//   RR · Relative reference.  https://www.rfc-editor.org/rfc/rfc3986#section-4.2
//
//   SM · Structural modification of a `HashMap` defined.
//        https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/HashMap.html
//
//   UFR  Marking a source file as `unimageable` after encountering a bad or unsupported form of reference
//        (and reporting it as an error).  Neither of the alternatives seems adequate.
//
//           a) Marking the file as `imageable` would cause the preceding error report to be repeated
//              when the translator prepares to probe the same reference.  Attempting to remedy
//              that repetition by omitting (here) the initial report would risk the greater fault
//              of leaving the error unreported.
//           b) Leaving the imageability as `indeterminate` would risk the foregoing (a)
//              because the file might subsequently be marked as `imageable`.
//
//        Rather leave the file `unimageable` and let the author repair the reference.




                                                   // Copyright © 2020-2022  Michael Allan.  Licence MIT.
