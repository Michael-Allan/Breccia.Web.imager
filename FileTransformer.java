package Breccia.Web.imager;

import Breccia.parser.ParseError;
import java.io.IOException;
import java.nio.file.Path;


public interface FileTransformer {


    /** Transforms a single Breccian source file into a namesake image file, forming or reforming
      * part of a Web image.  If the source file is empty, then an empty image file results.
      *
      *     @param imageDirectory The directory in which to write the image file.
      *       If no such directory exists, then one is formed.
      */
    public void transform( Path sourceFile, Path imageDirectory ) throws IOException, ParseError; }


                                                   // Copyright © 2020-2021  Michael Allan.  Licence MIT.
