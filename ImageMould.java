package Breccia.Web.imager;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import Java.*;
import java.util.*;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static Breccia.Web.imager.Imageability.*;
import static Breccia.Web.imager.Pinger.msPingInterval;
import static Breccia.Web.imager.Project.logger;
import static Java.Hashing.initialCapacity;
import static java.nio.file.Files.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.FINE;


/** A frame in which to form or reform a Web image.
  */
public final class ImageMould {


    /** @param bounds The top-level paths that define the extent of the Web image.
      * @param transformer The file transformer to use.
      * @throws UserError If any of `bounds` denotes an unwritable or unreadable directory,
      *   or an unreadable Breccian file.
      */
    public ImageMould( final Set<Path> bounds, final FileTransformer transformer )
          throws UserError {
        for( final Path p: bounds ) { // Test their accessibility up front.
            if( wouldRead(p) && !isReadable(p) ) throw new UserError( "Path is unreadable: " + p );
            if( isDirectory(p) && !isWritable(p) ) {
                throw new UserError( "Directory is unwritable: " + p ); }}
        this.bounds = bounds;
        this.transformer = transformer; }



    /** Map encoding all references to external Breccian documents from source files
      * whose imageability is initally indeterminate.  Each entry is formed as the normalized URI
      * of the referent document (key) mapped to the set (value) of indeterminate source files
      * that refer to it.  ‘External’ here means located outside of the source file.
      *
      * <p>The map is thread safe for all but structural modification, structural modification
      * being defined as for `{@linkplain HashMap HashMap}`.</p>
      *
      */ @Async // See `start` of pingers in `formImage`. [HBS]
    final Map<URI,Set<Path>> documentReferences = new HashMap<>( // Nulled after final use.
      initialCapacity( 8192/*referents*/, 0.75f/*default load factor*/ ));
      // Threads started after completion of this map in stage two of `formImage` may safely
      // access it for all but structural modification.



    /** Forms or reforms the image.
      */
    public void formImage() {

      // ═══════════════════════
      // 1. Pull in source files, sorting them as apodictically imageable or indeterminate
      // ═══════════════════════
        for( final Path p: bounds ) { /* Herein a streamlined process versus that of `pullPath`
              whose added testing and logging would be redundant for these top paths. */
            if( isDirectory( p )) pullDirectory( p );
            else pullFile( p ); }

      // ════════════════════════════════════
      // 2. Begin reducing the indeterminates, determining the imageability of each
      // ════════════════════════════════════
        imageabilityDetermination.forEach( this::documentReferences_initFrom ); // Collate references.

      // Start any referent pingers that are required
      // ──────────────────────────
        final Phaser barrier = new Phaser();
        final Map<String,Pinger> pingers = new HashMap<>(
          // Network hosts (keys) mapped each to its assigned pinger (value).
          initialCapacity( 256, 0.75f/*default load factor*/ ));
        documentReferences.keySet().forEach( ref -> // Ensure a pinger is assigned, if called for.
            pingers.compute( host(ref), (host, pinger) -> {
                if( host == null ) return null; // No network access to `ref`, no pinger required.
                if( pinger != null ) return null; // Already a pinger is assigned to `ref`.
                pinger = new Pinger( host, ImageMould.this );
                barrier.register();
                final Thread thread = new Thread( pinger, "Pinger thread `" + host + "`" ) {
                    public @Override void run() {
                        super.run();
                        barrier.arrive(); }};
                thread.setDaemon( true );
                thread.start(); /* After completion above of the structural modifications
                  to `documentReferences` and `imageabilityDetermination`, q.v.  [HBS, SM] */
                return pinger; }));

      // ═══════════════════════════
      // 3. Transform the imageables as they become determined
      // ═══════════════════════════
        boolean isFinalPass;
        if( pingers.size() == 0 ) {
            isFinalPass = true; // Only one pass is required.
            barrier.forceTermination(); } // Just to be tidy.
        else isFinalPass = false; // At least two will be required.
        for( ;; ) {
            int count = 0; // Transforms completed during the present pass.

          // Transform any imageables now determined, so forming part of the image
          // ────────────────────────
            for( final var det: imageabilityDetermination.entrySet() ) {
                if( det.getValue().get() != imageable ) return;
                try { transformer.transform( /*source file*/det.getKey() ); }
                catch( IOException x ) { throw new Unhandled( x ); }
                ++count; }
            if( isFinalPass ) break;
            if( count > 0 ) continue; // One good turn deserves another by making it likelier.

          // Await further reduction of indeterminates
          // ───────────────────────
            try{ barrier.awaitAdvanceInterruptibly​( /*phase*/0, msPingInterval, MILLISECONDS ); }
            catch( final InterruptedException x ) {
                Thread.currentThread().interrupt(); // Avoid hiding the fact of interruption.
                throw new UnsourcedInterrupt( x ); }
            catch( TimeoutException x ) { continue; } // Reduction is ongoing.
            isFinalPass = true; }} // Reduction is complete, the next pass is final.



    /** Gives the identifier of any network accessible host of a document reference,
      * which (after sanity checks) is simply the formal host part of the reference.
      * A value of null effectively means *same host*: the referent is accessible
      * not through the network, but through a local file system of the same host.
      *
      *     @param ref A document reference, a key
      *       of `{@linkplain #documentReferences documentReferences}`.
      *     @return The explicit host part of `ref`, which may be null.
      *     @see Pinger
      */
    static @AsyncSafe String host( URI ref ) {
        // TEST: Temporarily throwing `IllegalStateException` for any reference that should have been
        // weeded out during initial parsing of the references from their source documents, at which time
        // warnings could be issued (where appropriate) complete with line numbers.  Later these throws
        // to be replaced by counter-assertions.
        if( ref.isOpaque() ) throw new IllegalStateException();
        final String scheme = ref.getScheme();
        final String host = ref.getHost();
        if( host == null ) {
            if( scheme != null ) throw new IllegalStateException(); }
        else if( !isHTTP( scheme )) throw new IllegalStateException();
        return host; }



    /** Source files (keys) mapped each to the present state of its imageability determination (value).
      * The map is thread safe for all but structural modification, structural modification
      * being defined as for `{@linkplain HashMap HashMap}`.
      *
      */ @Async // See `start` of pingers in `formImage`. [HBS]
    final Map<Path,ImageabilityReference> imageabilityDetermination = new HashMap<>(
      initialCapacity( 8192/*source files*/, 0.75f/*default load factor*/ ));



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private final Set<Path> bounds;



    /** Puts to `documentReferences` the external document references of source file `f`
      * on condition its imageability `iR` is presently indeterminate.
      *
      *     @param f The path of a source file.
      *     @param iR The present imageability determination of `f`.
      */
    private void documentReferences_initFrom( final Path f, ImageabilityReference iR ) {
        if( iR.get() != indeterminate ) return;
        ; } // TODO



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
        final Path fImage = f.resolveSibling( f.getFileName() + ".xht" );
        final Imageability i;
        try {
            if( !exists(fImage) || getLastModifiedTime(f).compareTo(getLastModifiedTime(fImage)) >= 0 ) {
                System.out.println( "   ← " + f ); // TEST
                i = imageable; }
            else i = indeterminate; }
        catch( IOException x ) { throw new Unhandled( x ); }
        imageabilityDetermination.put​( f, new ImageabilityReference( i )); }



    /** @param p A path to pull into the mould.
      */
    private void pullPath( final Path p ) {
        if( isReadable( p )) { // Herein cf. `formImage`.
            if( isDirectory( p )) {
                if( isWritable( p )) pullDirectory( p );
                else logger.log( FINE, "Skipping unwritable directory: {0}/", p ); }
            else pullFile( p ); }
        else if( logger.isLoggable(FINE) && wouldRead(p) ) {
            logger.log( FINE, "Skipping unreadable path: {0}", p ); }}



    private final FileTransformer transformer;



    /** Answers whether path `p` would be read during image formation if it were readable.
      */
    private static boolean wouldRead( final Path p ) { return isDirectory(p) || looksBreccian(p); }}



// NOTES
// ─────
//   HBS  The happens-before relations of starting a thread.  ‘A call to `start()` on a thread
//        happens-before any actions in the started thread.’
//        https://docs.oracle.com/javase/specs/jls/se15/html/jls-17.html#jls-17.4.5
//
//   SM · Structural modification of a `HashMap` defined.
//        https://docs.oracle.com/en/java/javase/15/docs/api/java.base/java/util/HashMap.html



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
