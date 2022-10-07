package Breccia.Web.imager;

import Breccia.parser.*;
import Java.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static Breccia.Web.imager.ExternalResources.map;
import static Breccia.Web.imager.Imageability.*;
import static Breccia.Web.imager.Imaging.looksReachable;
import static Breccia.Web.imager.Project.imageFile;
import static Breccia.Web.imager.Project.imageSimpleName;
import static Breccia.Web.imager.Project.logger;
import static Breccia.Web.imager.RemoteChangeProbe.msQueryInterval;
import static Breccia.Web.imager.ErrorAtFile.errMsg;
import static Breccia.Web.imager.ErrorAtFile.wrnHead;
import static Java.Files.isDirectoryEmpty;
import static Java.Hashing.initialCapacity;
import static java.nio.file.Files.*;
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
      *     @see #out(int)
      *     @see #wrn()
      */
    public PrintWriter err() {
        hasFailed = true;
        return errorWriter; }



    /** A record of the formal imaging resources for source files whose imageability
      * is initally indeterminate.  ‘Formal’ means determining of image form.
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
        // Now `imageabilityDetermination` is structurally complete.  Newly started threads
        // may safely use it for all but structural modification.


      // ════════════════════════════════════
      // 2. Begin reducing the indeterminates, determining the imageability of each
      // ════════════════════════════════════
        imageabilityDetermination.forEach( this::formalResources_recordFrom ); // Collate the resources.
        // Now `formalResources` is structurally complete.  Newly started threads
        // may safely use it for all but structural modification.

      // Start any change probes that are required for remote resources
      // ───────────────────────
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

      // Probe the locally reachable resources
      // ─────────────────────────────────────
        formalResources.local.forEach( (res, dependants) -> {
            final FileTime resTime; {
                FileTime t = null;
                try { t = getLastModifiedTime( res ); }
                catch( final IOException x ) {
                    logger.warning( () -> "Forcefully reimaging dependants of resource `" + res // [ML]
                      + "` its timestamp being unreadable: " + x ); }
                resTime = t; }
            dependants.forEach( dep -> {
                final ImageabilityReference depImageability = imageabilityDetermination.get( dep );
                if( depImageability.get() != indeterminate ) return; /* Already determined by an
                  earlier pass of this probe, or (however unlikely) one of the slow remote probes. */
                boolean toReformImage = true;
                if( resTime != null ) { // So letting the null case be forcefully reimaged, as per above.
                    final Path depImage = imageFile( dep );
                    try { toReformImage = resTime.compareTo(getLastModifiedTime(depImage)) >= 0; }
                      // Viz. iff the formal resource has changed since the image was formed.
                    catch( final IOException x ) {
                        logger.warning( () -> "Forcefully reimaging `" + depImage // [ML]
                          + "` its timestamp being unreadable: " + x ); }}
                if( toReformImage ) imageabilityDetermination.get(dep).set( imageable ); }); });


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
            for( final var det: imageabilityDetermination.entrySet() ) {
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
        for( final var det: imageabilityDetermination.entrySet() ) {
            final ImageabilityReference iR = det.getValue();
            if( iR.get() != imaged ) continue;
            final Path sourceFile = det.getKey();
            final Path imageFileRelative = imageFile( boundaryPathDirectory.relativize( sourceFile ));
            out(1).println( "  → " + imageFileRelative );
            try {
                translator.finish( outDirectory.resolve( imageFileRelative ));
                ++count; }
            catch( final ErrorAtFile x ) { err().println( errMsg( x )); }}
        if( count == 0 ) out(2).println( "    none finished" );
        return !hasFailed; }



    /** Source files (keys) mapped each to the present state of its imageability determination (value).
      * The map is thread safe on condition of no concurrent structural modification,
      * structural modification being defined as for `{@linkplain HashMap HashMap}`.
      *
      */ @Async // See `start` of remote probe threads in `formImage`.
    final Map<Path,ImageabilityReference> imageabilityDetermination = new HashMap<>(
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



    /** Records all formal resources of source file `f`, provided it is of indeterminate imageability;
      * otherwise does nothing.
      *
      *     @see #formalResources
      *     @param f The path of a source file.
      *     @param iR The present imageability determination of `f`.
      */
    private void formalResources_recordFrom( final Path f, final ImageabilityReference iR ) {
        if( iR.get() != indeterminate ) return;
        final C in = translator.sourceCursor();
        try { in.perStateConditionally( f, state -> {
            final Markup mRef; // Reference in `Markup` form.
            try { mRef = translator.formalReferenceAt( in ); }
            catch( final ParseError x ) {
                err().println( errMsg( f, x ));
                iR.set( unimageable );
                return false; }
            if( mRef == null ) return true;
            final String sRef = mRef.text().toString(); // Reference in string form.
            if( isRemote( sRef )) { // Then the resource is reachable only through a network.
                URI pRef; // Reference in parsed `URI` form.
                try {
                    pRef = new URI( sRef );
                    if( !looksReachable( pRef )) {
                        throw new URISyntaxException( sRef, "Unrecognized form of reference" ); }}
                catch( final URISyntaxException x ) {
                    err().println( errMsg( f, mRef.lineNumber(), x ));
                    iR.set( unimageable );
                    return false; }
                assert pRef.getHost() != null; /* Assured by `looksReachable`.  Else, as described there,
                  rootless paths would be a possibility, raising the problem of how to resolve them. */
                pRef = unfragmented(pRef).normalize();
                map( formalResources.remote, /*resource*/pRef, /*dependant*/f ); }
            else { /* This `sRef` is an absolute-path reference or relative-path reference (ibid.),
                  making the resource reachable through local file systems. */
                if( sRef.startsWith( "~" )) { // [PUR]
                    if( !wasTildeEncountered ) {
                        logger.config( () ->
                          "Ignoring references with unsupported tilde prefix (~) here and hereafter: "
                          + f );
                        wasTildeEncountered = true; }
                    return true; }
                Path pRef = f.getParent().resolve( sRef ); /* Reference in parsed `Path` form,
                  resolved from its context. */
                if( !exists( pRef )) {
                    wrn().println( wrnHead(f, mRef.lineNumber())
                      + "No such file or directory: " + pRef );
                    return true; }
                pRef = pRef.normalize();
                map( formalResources.local, /*resource*/pRef, /*dependant*/f ); }
            return true; }); }
        catch( final ParseError x ) {
            err().println( errMsg( f, x ));
            iR.set( unimageable ); }}



    private boolean hasFailed;



    private static final boolean isHTTP( final String scheme ) {
        if( scheme.startsWith( "http" )) {
            final int sN = scheme.length();
            if( sN == 4  ) return true;
            if( sN == 5 && scheme.endsWith("s") ) return true; }
        return false; }



    /** @param f The path of a file.
      */
    private static boolean looksBreccian( final Path f ) {
        return f.getFileName().toString().endsWith( ".brec" ); }



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
        imageabilityDetermination.computeIfAbsent( f, f_ -> {
            final Imageability i;
            if( opt.toForce() ) i = imageable;
            else {
                final Path fI = imageFile( f );
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



    private FileTranslator<C> translator; // Do not modify after `initialize`.



    private boolean wasTildeEncountered; // Viz. a file path prefixed by `~`.



    /** Whether path `p` would be read during image formation if it were readable.
      */
    private static boolean wouldRead( final Path p ) { return isDirectory(p) || looksBreccian(p); }}



// NOTES
// ─────
//   ML · Mere logging of the IO error in order to avoid redundant and incomplete reporting.  The same
//        or similar error is almost certain to recur at least once during imaging, each recurrence
//        followed by a report to the user complete with source path and line number.
//
//   PUR  A peculiar URI reference yet unsupported by this image mould.  See *peculiar URI reference*
//        at `http://reluk.ca/project/Breccia/Web/imager/working_notes.brec.xht`.
//
//   SM · Structural modification of a `HashMap` defined.
//        https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/HashMap.html



                                                   // Copyright © 2020-2022  Michael Allan.  Licence MIT.
