package Breccia.Web.imager;

import Breccia.parser.*;
import java.nio.file.Path;


/** A translator of Breccian source files into HTML image files.
  *
  *     @param <C> The type of source cursor used by this translator.
  */
public interface FileTranslator<C extends ReusableCursor> {


    /** Finishes an image file that was newly translated from source.  This method is to be called
      * only after all image files have been translated from source.
      *
      *     @see #translate(Path,Path)
      */
    public void finish( Path imageFile ) throws ErrorAtFile;



    /** From the present position of the given source cursor, this method returns any nominal
      * URI reference to an external imaging resource that would be formal were it obtained by
      * this translator.  ‘Nominal’ here means that what is returned ought to be a URI reference
      * and so forth, e.g. based on where it occurs in the markup, though actually it might not be.
      *
      *     @see ImageMould#formalResources
      *     @return The formal reference, or null if there is none.
      */
    public Markup formalReferenceAt( C sourceCursor ) throws ParseError;



    /** A source cursor of the type used by this translator.
      * Between calls to the translator, it may be used for other purposes.
      */
    public C sourceCursor();



    /** Translates a Breccian source file into its namesake image file, forming or reforming
      * part of a Web image.  If the source file is empty, then an empty image file results.
      *
      *     @param imageDirectory The directory in which to write the image file.
      *       If no such directory exists, then one is formed.
      */
    public void translate( Path sourceFile, Path imageDirectory ) throws ParseError, ErrorAtFile;



   // ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀


    /** @param <C> The type of source cursor used by each newly made translator.
      */
    public static interface Maker<C extends ReusableCursor> {


        public FileTranslator<C> newTranslator( ImageMould<?> mould ); }}



                                                   // Copyright © 2020-2022  Michael Allan.  Licence MIT.
