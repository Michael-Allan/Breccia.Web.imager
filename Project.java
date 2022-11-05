package Breccia.Web.imager;

import Java.CharacterPointer;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.logging.Logger;

import static java.lang.Math.max;
import static java.lang.System.getProperty;


/** The present project.  Included is a medley of resources,
  * residual odds and ends that properly fit nowhere else.
  */
public final class Project {


    private Project() {}



    /** Returns for the given source file its image file: a sibling namesake with a `.xht` extension.
      * The image file of `dir/foo.brec`, for example, is `dir/foo.brec.xht`.
      */
    public static Path imageSibling( final Path sourceFile ) {
        return sourceFile.resolveSibling( imageSimpleName( sourceFile )); }



    /** Returns for the given source file its image file: a sibling namesake with a `.xht` extension.
      * The image file of `dir/foo.brec`, for example, is `dir/foo.brec.xht`.
      */
    public static String imageSibling( final String sourceFile ) { return sourceFile + ".xht"; }



    /** Returns the result of `sourceFile.{@linkplain Path#getFileName() getFileName}() + ".xht"`.
      */
    public static String imageSimpleName( final Path sourceFile ) {
        return imageSibling( sourceFile.getFileName().toString() ); }



    /** The output directory of the present project.
      */
    public static final Path outDirectory = Path.of( getProperty("java.io.tmpdir"),
      "Breccia.Web.imager_" + getProperty("user.name") );



    /** Returns for the given image file its source file: a sibling namesake without a `.xht` extension.
      * The source file of `dir/foo.brec.xht`, for example, is `dir/foo.brec`.
      *
      *     @throws IllegalArgumentException If the last four characters
      *       of `imageFile.getFileName` are not ‘.xht’.
      */
    public static Path sourceSibling( final Path imageFile ) {
        return imageFile.resolveSibling( sourceSimpleName( imageFile )); }



    /** Returns for the given image file its source file: a sibling namesake without a `.xht` extension.
      * The source file of `dir/foo.brec.xht`, for example, is `dir/foo.brec`.
      *
      *     @throws IllegalArgumentException If the last four characters
      *       of `imageFile.getFileName` are not ‘.xht’.
      */
    public static String sourceSibling( final String imageFile ) {
        if( !imageFile.endsWith( ".xht" )) throw new IllegalArgumentException();
        return imageFile.substring( 0, imageFile.length() - ".xht".length() ); }



    /** Returns `imageFile.{@linkplain Path#getFileName() getFileName}`
      * bereft of its last four characters.
      *
      *     @throws IllegalArgumentException If the last four characters
      *       of `imageFile.getFileName` are not ‘.xht’.
      */
    public static String sourceSimpleName( final Path imageFile ) {
        return sourceSibling( imageFile.getFileName().toString() ); }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    /** The logger proper to the present project.
      */
    static final Logger logger = Logger.getLogger( "Breccia.Web.imager" );



    /** Whether `f` appears to be the path of a Breccian file.
      *
      *     @param f The path of a file.
      */
    static boolean looksBreccian( final Path f ) { return looksBreccian( f.getFileName().toString() ); }



    /** Whether `ref` appears to refer to a Breccian file.
      *
      *     @param ref A <a href='https://www.rfc-editor.org/rfc/rfc3986#section-4.1'>
      *       URI reference</a>.
      */
    static boolean looksBreccian( final String ref ) { return ref.endsWith( ".brec" ); }



    /** Returns `max( x.getIndex(), 0 )`, so translating as zero any index of -1.
      */
    static int malformationIndex( final URISyntaxException x ) { return max( x.getIndex(), 0 ); }



    /** Makes a message to describe a malformed URI reference.
      */
    static String malformationMessage( final URISyntaxException x, final CharacterPointer p ) {
      return "Malformed URI reference: " + x.getReason() + '\n' + p.markedLine(); }}



                                                  // Copyright © 2020, 2022  Michael Allan.  Licence MIT.
