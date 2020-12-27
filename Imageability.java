package Breccia.Web.imager;


/** A state in the process of determining and acting on whether a source file is to be imaged.
  * It begins as either `indeterminate` or `imageable`, then transits in the order here declared
  * and finishes as either `indeterminate` or `imaged`.  Indeterminates that remain at the end
  * of the determination process are taken to need no imaging.
  */
enum Imageability {


    /** The imageability of the source file has yet to be determined.
      */
    indeterminate,



    /** The source file needs imaging.
      */
    imageable,



    /** The source file needed imaging and is now imaged.
      */
    imaged; }


                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
