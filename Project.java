package Breccia.Web.imager;

import java.nio.file.Path;
import java.util.logging.Logger;

import static java.lang.System.getProperty;


/** The present project.  Included is a medley of resources,
  * residual odds and ends that properly fit nowhere else.
  */
public final class Project {


    private Project() {}



    /** The output directory of the present project.
      */
    public static final Path outDirectory = Path.of( getProperty("java.io.tmpdir"),
      "Breccia.Web.imager_" + getProperty("user.name") );



    /** Returns for the given image file its source file: a sibling namesake without a `.xht` extension.
      * The source file of `dir/foo.brec.xht`, for example, is `dir/foo.brec`.
      */
    public static Path sourceFile( final Path imageFile ) {
        return imageFile.resolveSibling( sourceSimpleName( imageFile )); }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    /** Returns for the given source file its image file: a sibling namesake with a `.xht` extension.
      * The image file of `dir/foo.brec`, for example, is `dir/foo.brec.xht`.
      */
    static Path imageFile( final Path sourceFile ) {
        return sourceFile.resolveSibling( imageSimpleName( sourceFile )); }



    /** Returns the result of `sourceFile.{@linkplain Path#getFileName() getFileName}() + ".xht"`.
      */
    static String imageSimpleName( final Path sourceFile ) {
        return sourceFile.getFileName() + ".xht"; }



    /** The logger proper to the present project.
      */
    static final Logger logger = Logger.getLogger( "Breccia.Web.imager" );



    /** Returns `imageFile.{@linkplain Path#getFileName() getFileName}`
      * bereft of its last four characters.
      *
      *     @throws AssertionError If assertions are enabled and the last
      *       four characters of `imageFile.getFileName` are not ‘.xht’.
      */
    static String sourceSimpleName( final Path imageFile ) {
        final String imageSimpleName = imageFile.getFileName().toString();
        assert imageSimpleName.endsWith( ".xht" );
        return imageSimpleName.substring( 0, imageSimpleName.length() - ".xht".length() ); }}



                                                  // Copyright © 2020, 2022  Michael Allan.  Licence MIT.
