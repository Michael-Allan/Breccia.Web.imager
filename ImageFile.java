package Breccia.Web.imager;


/** @param sourceText The original source text from which the image file was formed.
  * @param fracta A linear-order array of the body fracta of the image file.
  */
record ImageFile( String sourceText, ImagedBodyFractum[] fracta ) {}
  // OPT `sourceText`, this will not scale.



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
