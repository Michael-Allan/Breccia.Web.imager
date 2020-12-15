package Breccia.Web.imager;

import java.io.IOException;
import java.nio.file.Path;


public interface FileTransformer {


    /** Transforms a single Breccian source file into an HTML sibling, forming (or reforming)
      * part of a Web image.  If the source file is empty, then an empty image file results.
      */
    public void transform( Path sourceFile ) throws IOException; }


                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
