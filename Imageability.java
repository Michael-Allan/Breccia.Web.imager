package Breccia.Web.imager;


/** A state in the process of determining and acting on whether a source file is to be imaged.
  * It begins as either `indeterminate` or `imageable`, and finishes as either `indeterminate`,
  * `unimageable` or `imaged`.  Any indeterminates that remain at the end of the determination
  * process are taken to need no imaging.
  */
enum Imageability {


    /** The imageability of the source file has yet to be determined.
      */
    indeterminate,



    /** The source file must not be imaged.  This is a final state.
      */
    unimageable,



    /** The source file needs imaging.
      */
    imageable,



    /** The source file needed imaging and is now imaged.  This is a final state.
      */
    imaged }


                                                   // Copyright © 2020-2021  Michael Allan.  Licence MIT.
