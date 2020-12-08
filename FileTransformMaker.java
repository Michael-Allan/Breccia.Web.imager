package Breccia.Web.imager;

import java.nio.file.Path;


/** A maker of file transforms.
  */
public interface FileTransformMaker {


    /** Makes a file transform.
      *
      *     @param f The path of the Breccian source file to transform.
      */
    public FileTransform makeFileTransform( Path f ); }



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
