package Breccia.Web.imager;

import Java.Async;
import Java.UnsourcedInterrupt;
import java.net.URI;
import java.nio.file.Path;
import java.util.Set;

import static Java.Collections.forEachRemaining;
import static java.lang.Thread.sleep;


/** A Web crawler to get the timestamps of referents (ping them) and so determine the imageability
  * of their referrers.  Assigned a particular Web host among `documentReferences`, it determines
  * as imageable any referrer to that host whose image file does not postdate the referent,
  * and updates the referrer’s `imageabilityDetermination` accordingly.
  *
  *     @see ImageMould#documentReferences
  *     @see ImageMould#imageabilityDetermination
  */
final class Pinger implements Runnable {


    /** @param host The identifier of the Web host whose referents to ping,
      *   as per `{@linkplain ImageMould#host(java.net.URI) ImageMould.host}`.
      * @param mould The mould to use.
      */
    Pinger( final String host, final ImageMould mould ) {
        this.host = host;
        this.mould = mould; }



    /** The delay in milliseconds before each successive ping.
      */
    static final int msPingInterval = 1000; /* Cf. `Crawl-delay`.
      https://en.wikipedia.org/wiki/Robots_exclusion_standard#Crawl-delay_directive */



   // ━━━  R u n n a b l e  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    /** {@inheritDoc} Call once only.  This method is thread safe for a single call.
      */
    public @Async @Override void run() {
        final var refSequence = mould.documentReferences.entrySet().iterator();
        forEachRemaining( refSequence, (referent, referrers) -> {
            ping( referent, referrers );
            if( refSequence.hasNext() ) {
                try { sleep( msPingInterval ); }
                catch( final InterruptedException x ) {
                    Thread.currentThread().interrupt(); // Avoid hiding the fact of interruption.
                    throw new UnsourcedInterrupt( x ); }}});}



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////



    private final String host;



    private final ImageMould mould;



    private void ping( final URI referent, final Set<Path> referrers ) {
        System.err.println( " ——— TEST, ping referent " + referent ); }}



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
