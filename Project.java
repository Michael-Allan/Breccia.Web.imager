package Breccia.Web.imager;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import Java.Unhandled;
import java.util.logging.Logger;

import static java.lang.Math.max;
import static java.lang.System.getProperty;
import static Java.Paths.hasExtension;
import static Java.URI_References.hasExtension;


/** The present project.  Included is a medley of resources,
  * residual odds and ends that properly fit nowhere else.
  */
public final class Project {


    private Project() {}



    /** Returns for the given source path its image sibling: a namesake with a `.xht` extension.
      * Assuming a path {@linkplain java.nio.file.FileSystem#getSeparator name separator} of ‘/’,
      * the image sibling of `dir/foo.brec`, for example, is `dir/foo.brec.xht`.
      *
      *     @param s A path to a source file.
      */
    public static Path imageSibling( final Path s ) {
        return s.resolveSibling( imageSibling( s.getFileName().toString() )); }



    /** Returns for the given source reference its image sibling: a namesake with a `.xht` extension.
      * The image sibling of `dir/foo.brec`, for example, is `dir/foo.brec.xht`.
      *
      *     @param s A URI reference to a source file.
      */
    public static URI imageSibling( final URI s ) { return repath( s, imageSibling( s.getPath() )); }



    /** The output directory of the present project.
      */
    public static final Path projectOutputDirectory = Path.of( getProperty("java.io.tmpdir"),
      "Breccia.Web.imager_" + getProperty("user.name") );



    /** Returns for the given image path its source sibling: a namesake without a `.xht` extension.
      * Assuming a path {@linkplain java.nio.file.FileSystem#getSeparator name separator} of ‘/’,
      * the source sibling of `dir/foo.brec.xht`, for example, is `dir/foo.brec`.
      *
      *     @param i A path to an image file.
      *     @throws IllegalArgumentException If the image path itself has no `.xht` extension.
      */
    public static Path sourceSibling( final Path i ) {
        return i.resolveSibling( sourceSibling( i.getFileName().toString() )); }



    /** Returns for the given image reference its source sibling: a namesake without a `.xht` extension.
      * The source sibling of `dir/foo.brec.xht`, for example, is `dir/foo.brec`.
      *
      *     @param i A URI reference to an image file.
      *     @throws IllegalArgumentException If the image reference itself has no `.xht` extension.
      */
    public static URI sourceSibling( final URI i ) { return repath( i, sourceSibling( i.getPath() )); }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    /** Returns the image sibling of `s`, a namesake with a `.xht` extension.
      * The image sibling of `dir/foo.brec`, for example, is `dir/foo.brec.xht`.
      *
      *     @param s A URI reference or local file path to a source file. *//*
      *
      * Unreliable for general exposure because `s` might be given in the form of a URI reference
      * that has a query or fragment component.
      */
    private static String imageSibling( final String s ) { return s + ".xht"; }



    /** The logger proper to the present project.
      */
    static final Logger logger = Logger.getLogger( "Breccia.Web.imager" );



    /** Whether the given file path appears to refer to a Breccian source file.
      *
      *     @param file The path of a file.
      *     @throws AssertionError If assertions are enabled and `f` is a directory.
      */
    static boolean looksBrecciaLike( final Path file ) {
     // assert !isDirectory( file );
    //// invalid: path `file` may be relative
        return hasExtension( ".brec", file ); }



    /** Whether the given URI reference appears to refer to a Breccian source file.
      *
      *     @see <a href='https://www.rfc-editor.org/rfc/rfc3986#section-4.1'>
      *       URI generic syntax §4.1, URI reference</a>
      */
    static boolean looksBrecciaLike( final URI ref ) { return hasExtension( ".brec", ref ); }



    /** Whether the given file path appears to refer to a Breccian Web image file.
      *
      *     @param file The path of a file.
      *     @throws AssertionError If assertions are enabled and `file` is a directory.
      */
    static boolean looksImageLike( final Path file ) {
     // assert !isDirectory( file );
    //// invalid: path `file` may be relative
        return hasExtension( ".brec.xht", file ); }



    /** Whether the given URI reference appears to refer to a Breccian Web image file.
      *
      *     @see <a href='https://www.rfc-editor.org/rfc/rfc3986#section-4.1'>
      *       URI generic syntax §4.1, URI reference</a>
      */
    static boolean looksImageLike( final URI ref ) { return hasExtension( ".brec.xht", ref ); }



    /** The delimiter for mathematics to be rendered in block (aka display) as opposed to in-line form.
      */
    final static char mathBlockDelimiter = '･'; // Halfwidth katakana middle dot (FF65).



    /** Returns the source sibling of `i`, a namesake without a `.xht` extension.
      * The source sibling of `dir/foo.brec.xht`, for example, is `dir/foo.brec`.
      *
      *     @param i A URI reference or local file path to an image file. *//*
      *     @throws IllegalArgumentException If `i` itself has no `.xht` extension.
      *
      * Unreliable for general exposure because `i` might be given in the form of a URI reference
      * that has a query or fragment component.
      */
    private static String sourceSibling( final String i ) {
        if( !i.endsWith( ".xht" )) throw new IllegalArgumentException();
        return i.substring( 0, i.length() - ".xht".length() ); }



    /** Swaps into `u` the given path and returns the result.
      */
    private static URI repath( final URI u, final String path ) {
        try {
            return new URI( u.getScheme(), u.getAuthority(), path, u.getQuery(), u.getFragment() ); } /*
              With decoding (as opposed to raw) getters, as stipulated in (and above) § Identities:
              `https://docs.oracle.com/en/java/javase/18/docs/api/java.base/java/net/URI.html` */
        catch( URISyntaxException x ) { throw new Unhandled( x ); }}
          // Unexpected with a reconstruction of this sort.



    /** Returns `max( index, 0 )`, so translating to zero any index of -1.
      */
    static int zeroBased( final int index ) { return max( index, 0 ); }}



                                            // Copyright © 2020, 2022, 2024  Michael Allan.  Licence MIT.
