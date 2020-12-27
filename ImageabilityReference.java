package Breccia.Web.imager;

import java.util.concurrent.atomic.AtomicReference;
import static java.util.Objects.requireNonNull​;


final class ImageabilityReference extends AtomicReference<Imageability> { // An alias, for convenience.


    ImageabilityReference( final Imageability i ) { super( requireNonNull​( i )); }}
      // Setters too might be so guarded, and guarded for valid state transitions,
      // but that is not the purpose of this class.


                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
