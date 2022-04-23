package Breccia.Web.imager;

import Java.Async;
import Java.UnsourcedInterrupt;
import java.net.URI;
import java.nio.file.Path;
import java.util.Set;

import static Breccia.Web.imager.Imageability.*;
import static Java.Collections.forEachRemaining;
import static java.lang.Thread.sleep;


/** A crawling probe of the formal resources at a remote Web host.  It reads the timestamps
  * of each resource and so determines the imageability of the local source files that depend
  * on it (dependants).  Assigned a particular host from among `formalResources`, the probe
  * determines as imageable any dependant of a resource whose image file does not postdate
  * the resource, then updates the dependant’s `imageabilityDetermination` accordingly.
  *
  *     @see ImageMould#formalResources
  *     @see ImageMould#imageabilityDetermination
  */
final class RemoteChangeProbe implements Runnable {


    /** @param host The identifier of the network host whose resources to probe,
      *   concordant with `{@linkplain ImageMould#host(java.net.URI) ImageMould.host}`.
      * @param mould The mould to use.
      */
    RemoteChangeProbe( final String host, final ImageMould<?> mould ) {
        this.host = host;
        this.mould = mould; }



    /** The delay in milliseconds before each successive HTTP query to a Web host.
      */
    static final int msQueryInterval = /*TEST*/100; /* Cf. `Crawl-delay`.
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



    private void probe( final URI resource, final Set<Path> dependants ) {

      // Ensure the resource still needs probing
      // ───────────────────────────────────────
        boolean toProbe = false;
        for( final Path dep: dependants ) {
            final ImageabilityReference depImageability = mould.imageabilityDetermination.get( dep );
            if( depImageability.get() == indeterminate ) {
                toProbe = true;
                break; }}
        if( !toProbe ) return;

      // Probe the resource
      // ──────────────────
        System.err.println( " ——— probe TEST: " + resource );
        /* TODO, the actual probe */; }}



                                                   // Copyright © 2020-2022  Michael Allan.  Licence MIT.
