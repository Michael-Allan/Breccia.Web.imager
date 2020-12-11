package Breccia.Web.imager;

import java.nio.file.Path;


public interface FileTransformer {


    /** Transforms a single Breccian source file into an HTML sibling, forming (or reforming)
      * part of a Web image.
      */
    public void transform( Path sourceFile ); }


                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
