package Breccia.Web.imager;

import Breccia.parser.*;
import Java.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static Breccia.Web.imager.ExternalResources.map;
import static Breccia.Web.imager.Imageability.*;
import static Breccia.Web.imager.Imaging.looksReachable;
import static Breccia.Web.imager.Project.logger;
import static Breccia.Web.imager.RemoteChangeProbe.msQueryInterval;
import static Breccia.Web.imager.TransformError.errMsg;
import static Breccia.Web.imager.TransformError.wrnHead;
import static Java.Files.isDirectoryEmpty;
import static Java.Hashing.initialCapacity;
import static java.nio.file.Files.*;
import static Java.URIs.schemedPattern;
import static Java.URIs.unfragmented;
import static java.util.concurrent.TimeUnit.MILLISECONDS;


/** A frame in which to form or reform a Web image.
  *
  *     @param <C> The type of source cursor used by this mould.
  */
public final class ImageMould<C extends ReusableCursor> {


    /** @see #boundaryPath
      * @see #transformer
      * @see #outDirectory
      * @param errorStream Where to report any warnings or survivable errors that occur
      *   during image formation.
      * @throws IllegalArgumentException If `boundaryPath` is relative or non-existent.
      * @throws IllegalArgumentException If `outDirectory` is not an empty directory.
      */
    public ImageMould( final Path boundaryPath, final FileTransformer<C> transformer,
          final Path outDirectory, final PrintWriter errorStream ) {
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
        this.transformer = transformer;
        this.outDirectory = outDirectory;
        this.errorStream = errorStream; }



    /** The topmost path of the Web image, which defines its extent.  This is an absolute path.
      * It comprises or contains the Breccian source files of the image, each accompanied
      * by any previously formed image file, a sibling namesake with a `.xht` extension.
      */
    public final Path boundaryPath;



    /** The directory of the boundary path: either `boundaryPath` itself if it is a directory,
      * otherwise its parent.
      */
    public final Path boundaryPathDirectory;



    /** Where to report any survivable errors in the process of image formation.
      * Calling this method will result in a false return value from `formImage`.
      *
      *     @see #wrn()
      */
    public PrintWriter err() {
        hasFailed = true;
        return errorStream; }



    /** A record of the formal imaging resources for source files whose imageability
      * is initally indeterminate.  ‘Formal’ means determining of image form.
      */
    final ExternalResources formalResources = new ExternalResources();



    /** Forms or reforms any new files that are required to update the image,
      * writing each to the {@linkplain #outDirectory output directory}
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

      // ═══════════════════════
      // 1. Pull in source files, sorting them as apodictically imageable or indeterminate
      // ═══════════════════════
        if( isDirectory( boundaryPath )) {   // A streamlined process versus that of `pullPath`
            pullDirectory( boundaryPath ); } // whose added testing and messaging would be redundant
        else pullFile( boundaryPath );       // for this topmost path.
        // Now the structuring of `imageabilityDetermination` is complete, newly started threads
        // may safely use it for all but structural modification.


      // ════════════════════════════════════
      // 2. Begin reducing the indeterminates, determining the imageability of each
      // ════════════════════════════════════
        imageabilityDetermination.forEach( this::formalResources_recordFrom ); // Collate the resources.
        // Now the structuring of `formalResources` is complete, newly started threads
        // may safely use it for all but structural modification.

      // Start any change probes that are required for remote resources
      // ───────────────────────
        final Phaser barrier = new Phaser();
        final Map<String,RemoteChangeProbe> probes = new HashMap<>( initialCapacity( 256 ));
          // Network hosts (keys) mapped each to its assigned probe (value).
        formalResources.remote.keySet().forEach( res -> // Ensure a probe is assigned, if called for.
            probes.compute( res.getHost(), (host, probe) -> {
                if( probe != null ) return null; // Already a probe is assigned to the host of `res`.
                probe = new RemoteChangeProbe( host, ImageMould.this );
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
                    logger.fine( () -> "Forcefully reimaging dependants of resource `" + res // [ML]
                      + "` its timestamp being unreadable: " + x );}
                resTime = t; }
            dependants.forEach( dep -> {
                final ImageabilityReference depImageability = imageabilityDetermination.get( dep );
                if( depImageability.get() != indeterminate ) return; /* Already determined by an
                  earlier pass of this probe, or (however unlikely) one of the slow remote probes. */
                boolean toReformImage = true;
                if( resTime != null ) { // So letting the null case be forcefully reimaged, as per above.
                    final Path depImage = Imaging.imageFile( dep );
                    try { toReformImage = resTime.compareTo(getLastModifiedTime(depImage)) >= 0; }
                      // Viz. iff the formal resource has changed since the image was formed.
                    catch( final IOException x ) {
                        logger.fine( () -> "Forcefully reimaging `" + depImage // [ML]
                          + "` its timestamp being unreadable: " + x );}}
                if( toReformImage ) {
                    System.out.println( "   ← " + dep ); // TEST
                    imageabilityDetermination.get(dep).set( imageable ); }});});


      // ═══════════════════════════
      // 3. Transform the imageables as they are determined
      // ═══════════════════════════
        boolean isFinalPass;
        if( probes.size() == 0 ) {
            isFinalPass = true; // Only one pass is required.
            barrier.forceTermination(); } // Just to be tidy.
        else isFinalPass = false; // At least two will be required.
        for( ;; ) {
            int count = 0; // Of imageables found during the present pass.

          // Transform any imageables now determined, so forming part of the image
          // ────────────────────────
            for( final var det: imageabilityDetermination.entrySet() ) {
                final ImageabilityReference iR = det.getValue();
                if( iR.get() != imageable ) continue;
                ++count;
                final Path sourceFile = det.getKey();
                final Path imageDirectory = outDirectory.resolve(
                  boundaryPathDirectory.relativize( sourceFile.getParent() ));
                boolean wasTransformed = false;
                try {
                    transformer.transform( sourceFile, imageDirectory );
                    wasTransformed = true; }
                catch( final ParseError x ) { err().println( errMsg( sourceFile, x )); }
                catch( final TransformError x ) { err().println( errMsg( x )); }
                iR.set( wasTransformed? imaged: unimageable ); }
            if( isFinalPass ) break;
            if( count > 0 ) continue; // One good turn deserves another by making it likelier.

          // Await further reduction of indeterminates
          // ───────────────────────
            try { barrier.awaitAdvanceInterruptibly​( /*phase*/0, msQueryInterval, MILLISECONDS );}
            catch( final InterruptedException x ) {
                Thread.currentThread().interrupt(); // Avoid hiding the fact of interruption.
                throw new UnsourcedInterrupt( x ); }
            catch( TimeoutException x ) { continue; } // Reduction is ongoing.
            isFinalPass = true; } // Reduction is complete, the next pass is final.
        return !hasFailed; }



    /** Source files (keys) mapped each to the present state of its imageability determination (value).
      * The map is thread safe on condition of no concurrent structural modification,
      * structural modification being defined as for `{@linkplain HashMap HashMap}`.
      *
      */ @Async // See `start` of remote probe threads in `formImage`.
    final Map<Path,ImageabilityReference> imageabilityDetermination = new HashMap<>(
      initialCapacity( 8192/*source files*/ ));



    /** The directory in which to write any newly formed image files.
      */
    public final Path outDirectory;



    /** The file transformer to use for image formation.
      */
    public final FileTransformer<C> transformer;



    /** Where to report any warnings in the process of image formation.
      *
      *     @see #err()
      */
    public PrintWriter wrn() { return errorStream; }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private final PrintWriter errorStream; /* Do not write to the stream through this field.
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
        final C in = transformer.sourceCursor();
        try { in.perStateConditionally( f, state -> {
            final Markup mRef; // Marked-up reference.
            try { mRef = transformer.formalReferenceAt( in ); }
            catch( final ParseError x ) {
                err().println( errMsg( f, x ));
                iR.set( unimageable );
                return false; }
            if( mRef == null ) return true;
            final String sRef = mRef.text().toString(); // String reference.
            if( sRef.startsWith("//") || schemedPattern.matcher(sRef).lookingAt() ) { /* Then the
                  resource is reachable only through a network.  The ‘//’ case would indicate a
                  network-path reference.  https://tools.ietf.org/html/rfc3986#section-4.2 */
                URI pRef; // Parsed reference.
                try {
                    pRef = new URI( sRef ); // To parsed form.
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
                Path pRef = f.resolve( sRef ); // To parsed form, and resolved from its context.
                pRef = pRef.normalize();
                map( formalResources.local, /*resource*/pRef, /*dependant*/f ); }
            return true; });}
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



    /** @param f The path of a source file to pull into the mould.
      */
    private void pullFile( final Path f ) {
        if( !looksBreccian( f )) return;
        if( imageabilityDetermination.containsKey( f )) return;
        final Path fImage = Imaging.imageFile( f );
        final Imageability i;
        try {
            if( !exists(fImage) || getLastModifiedTime(f).compareTo(getLastModifiedTime(fImage)) >= 0 ) {
                System.out.println( "   ← " + f ); // TEST
                i = imageable; }
            else i = indeterminate; }
        catch( IOException x ) { throw new Unhandled( x ); }
        imageabilityDetermination.put( f, new ImageabilityReference( i )); }



    /** @param p A path to pull into the mould.
      */
    private void pullPath( final Path p ) {
        if( isReadable( p )) { // Herein cf. `formImage`.
            if( isDirectory( p )) {
                if( isWritable( p )) pullDirectory( p );
                else wrn().println( wrnHead(p) + "Skipping this unwritable directory" ); }
            else pullFile( p ); }
        else if( wouldRead( p )) wrn().println( wrnHead(p) + "Skipping this unreadable path" ); }



    /** Whether path `p` would be read during image formation if it were readable.
      */
    private static boolean wouldRead( final Path p ) { return isDirectory(p) || looksBreccian(p); }}



// NOTES
// ─────
//   ML · Mere logging of the IO error in order to avoid redundant and incomplete reporting.  The same
//        or similar error is almost certain to recur at least once during imaging, each recurrence
//        followed by a report to the user complete with source path and line number.
//
//   SM · Structural modification of a `HashMap` defined.
//        https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/HashMap.html



                                                   // Copyright © 2020-2022  Michael Allan.  Licence MIT.
