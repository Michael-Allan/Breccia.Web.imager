package Breccia.Web.imager;

import Breccia.parser.*;
import java.nio.file.Path;


/** A transformer of Breccian source files into namesake image files.
  *
  *     @param <C> The type of source cursor used by this transformer.
  */
public interface FileTransformer<C extends ReusableCursor> {


    /** From the present position of the given source cursor, this method returns any nominal,
      * URI reference to an external imaging resource that would be formal were it obtained by
      * this transformer.  ‘Nominal’ here means that what is returned ought to be these things,
      * e.g. based on where it occurs in the markup, though actually it might not be.
      *
      *     @see ImageMould#formalResources
      *     @return The formal reference, or null if there is none.
      */
    public Markup formalReferenceAt( C sourceCursor ) throws ParseError;



    /** A source cursor of the type used by this transformer.
      * Between calls to the transformer, it may be used for other purposes.
      */
    public C sourceCursor();



    /** Transforms a single Breccian source file into a namesake image file, forming or reforming
      * part of a Web image.  If the source file is empty, then an empty image file results.
      *
      *     @param imageDirectory The directory in which to write the image file.
      *       If no such directory exists, then one is formed.
      */
    public void transform( Path sourceFile, Path imageDirectory ) throws ParseError, TransformError; }



                                                   // Copyright © 2020-2021  Michael Allan.  Licence MIT.
