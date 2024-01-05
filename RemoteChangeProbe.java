package Breccia.Web.imager;

import Java.Async;
import Java.UnsourcedInterrupt;
import java.net.URI;
import java.nio.file.Path;
import java.util.Set;

import static Breccia.Web.imager.Imageability.indeterminate;
import static Breccia.Web.imager.Project.looksBrecciaLike;
import static Java.Collections.forEachRemaining;
import static java.lang.Thread.sleep;
import static Java.URI_References.isRemote;
import static Java.URIs.isHTTP;


/** A crawling probe of the formal resources at a remote Web host.  It reads the timestamps
  * of each resource and so determines the imageability of the local source files that depend
  * on it (dependants).  Assigned a particular host from among `formalResources`, the probe
  * determines as imageable any dependant of a resource whose image file does not postdate
  * the resource, then updates the dependant’s `imageabilityDeterminations` accordingly.
  *
  *     @see ImageMould#formalResources
  *     @see ImageMould#imageabilityDeterminations
  */
final class RemoteChangeProbe implements Runnable {


    /** @param host The identifier of the network host whose resources to probe,
      *   concordant with `{@linkplain ImageMould#host(java.net.URI) ImageMould.host}`.
      * @param mould The mould to use.
      */
    RemoteChangeProbe( final String host, final ImageMould<?> mould ) {
        this.host = host;
        this.mould = mould; }



    /** Appends to `b` the causal part of a user report that `ref` is improbeable.
      *
      *     @param ref A URI reference to a remote resource.
      *     @see java.lang.Throwable#getCause()
      *     @see #looksProbeable(URI)
      */
    static void appendImprobeableCause( final URI ref, final StringBuilder b ) {
        b.append( "Unable to access the referent by this form of reference.  Consider marking it " );
        if( !looksBrecciaLike( ref )) b.append( "non-fractal or " );
        b.append( "private." ); }



    /** Whether it appears possible to access the referent of the given reference.
      *
      *     @param ref A URI reference to a remote resource.
      *     @see <a href='https://www.rfc-editor.org/rfc/rfc3986#section-4.1'>
      *       URI generic syntax §4.1, URI reference</a>
      *     @see Java.URI_References#isRemote()
      */
    static boolean looksProbeable( final URI ref ) {
        if( !isRemote( ref )) throw new IllegalArgumentException();
        boolean answer = true;
        if( ref.isOpaque() ) answer = false;
        else {
            final String scheme = ref.getScheme();
            if( scheme != null ) {
                if( ref.getHost() == null ) answer = false; /* Trouble with no justifying use case.
                  Such a hostless URI is allowed a rootless path, making it hard to resolve
                  from outside the network context (e.g. HTTP) implied by the scheme. */
                else if( !isHTTP( scheme )) answer = false; }
            else answer = false; } /* Trouble with no justifying use case, a network-path reference.
              https://www.rfc-editor.org/rfc/rfc3986#section-4.2 */
        return answer; }



    /** The delay in milliseconds before each successive HTTP query to a Web host.
      */
    static final int msQueryInterval = /*TEST*/0; /* Cf. `Crawl-delay`.
      https://en.wikipedia.org/wiki/Robots_exclusion_standard#Crawl-delay_directive */



   // ━━━  R u n n a b l e  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    /** {@inheritDoc} Call once only.  This method is thread safe for a single call.
      */
    public @Async @Override void run() {
        final var rr = mould.formalResources.remote.entrySet().iterator();
        forEachRemaining( rr, (resource, dependants) -> {
            if( !host.equals( resource.getHost() )) return;
            probe( resource, dependants );
            if( rr.hasNext() ) {
                try { sleep( msQueryInterval ); }
                catch( final InterruptedException x ) {
                    Thread.currentThread().interrupt(); // Avoid hiding the fact of interruption.
                    throw new UnsourcedInterrupt( x ); }}}); }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////



    private final String host;



    private final ImageMould<?> mould;



    private void probe( final URI ref, final Set<Path> dependants ) {
        if( !looksProbeable( ref )) throw new IllegalArgumentException();

      // Ensure the resource still needs probing
      // ───────────────────────────────────────
        boolean toProbe = false;
        for( final Path dep: dependants ) {
            final ImageabilityReference depImageability = mould.imageabilityDeterminations.get( dep );
            if( depImageability.get() == indeterminate ) {
                toProbe = true;
                break; }}
        if( !toProbe ) return;

      // Probe the resource
      /* ──────────────────
        Deferred, as per
        `http://reluk.ca/project/Breccia/Web/imager/working_notes.brec.xht#deferral,hTTP,fetches`. */; }}



                                             // Copyright © 2020-2022, 2025  Michael Allan.  Licence MIT.
