package Breccia.Web.imager;

import org.w3c.dom.Element;


/** The failure of a variable interpolation in a regular-expression pattern.
  */
class FailedInterpolation extends Exception {


    /** @see #interpolator
      * @see #index
      * @see #getMessage
      */
    FailedInterpolation( final Element interpolator, final int index, final String message ) {
        super( message );
        this.interpolator = interpolator;
        this.index = index; }



    /** Index to the fault in the `interpolator` text, or zero if the interpolator as a whole
      * is at fault.
      */
    final int index;



    /** The image of the interpolator that failed, or its representative in the case
      * of an implied interpolator.
      */
    final Element interpolator; }



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
