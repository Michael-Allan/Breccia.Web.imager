package Breccia.Web.imager;

import org.w3c.dom.Element;


/** The failure of a variable interpolation in a regular-expression pattern.
  */
class FailedInterpolation extends Exception {


    /** @see #interpolation
      * @see #index
      * @see #getMessage
      */
    FailedInterpolation( final Element interpolation, final int index, final String message ) {
        super( message );
        this.interpolation = interpolation;
        this.index = index; }



    /** Index to the fault in the `interpolation` text, or zero if the interpolation as a whole
      * is at fault.
      */
    final int index;



    /** The image of the interpolation that failed.
      */
    final Element interpolation; }



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
