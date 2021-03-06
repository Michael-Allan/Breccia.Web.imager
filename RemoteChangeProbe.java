package Breccia.Web.imager;

import Java.Async;
import Java.UnsourcedInterrupt;
import java.net.URI;
import java.nio.file.Path;
import java.util.Set;

import static Breccia.Web.imager.Imageability.*;
import static Java.Collections.forEachRemaining;
import static java.lang.Thread.sleep;


/** A Web crawler to query the timestamps of remote, formal resources and so determine the imageability
  * of their dependent source files.  Assigned a particular network host among the `formalResources`,
  * it determines as imageable any dependant of a hosted resource whose image file does not postdate
  * the resource, then updates the dependant’s `imageabilityDetermination` accordingly.
  *
  *     @see ImageMould#formalResources
  *     @see ImageMould#imageabilityDetermination
  */
final class RemoteChangeProbe implements Runnable {


    /** @param host The identifier of the network host whose resources to probe,
      *   as per `{@linkplain ImageMould#host(java.net.URI) ImageMould.host}`.
      * @param mould The mould to use.
      */
    RemoteChangeProbe( final String host, final ImageMould<?> mould ) {
        this.host = host;
        this.mould = mould; }



    /** The delay in milliseconds before each successive timestamp query.
      */
    static final int msQueryInterval = 1000; /* Cf. `Crawl-delay`.
      https://en.wikipedia.org/wiki/Robots_exclusion_standard#Crawl-delay_directive */



   // ━━━  R u n n a b l e  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    /** {@inheritDoc} Call once only.  This method is thread safe for a single call.
      */
    public @Async @Override void run() {
        final var rr = mould.formalResources.remote.entrySet().iterator();
        forEachRemaining( rr, (resource, dependants) -> {
            probe( resource, dependants );
            if( rr.hasNext() ) {
                try { sleep( msQueryInterval ); }
                catch( final InterruptedException x ) {
                    Thread.currentThread().interrupt(); // Avoid hiding the fact of interruption.
                    throw new UnsourcedInterrupt( x ); }}});}



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
        System.err.println( " ——— TEST, probe remote resource " + resource ); }}



                                                   // Copyright © 2020-2021  Michael Allan.  Licence MIT.
