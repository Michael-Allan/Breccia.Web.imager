package Breccia.Web.imager;

import Breccia.parser.ReusableCursor;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import Java.Unhandled;
import Java.UserError;

import static Java.Files.verifyDirectoryArgument;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


public final class Imaging {


    private Imaging() {}



    /** Executes an imaging shell command.
      *
      *     @param <C> The type of source cursor used by the image mould.
      *     @param name The name of the shell command.
      *     @see ImageMould#boundaryPath
      *     @see ImageMould#transformer
      *     @see ImageMould#outDirectory
      *     @return True on success; false on failure.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec'>
      *       The `breccia-web-image` command of the Breccia Web imager</a>
      *     @see <a href='http://reluk.ca/project/wayic/Web/imager/bin/breccia-web-image.brec'>
      *       The `image` command of the waycast Web imager</a>
      */
    public static <C extends ReusableCursor> boolean image( final String name,  final Path boundaryPath,
          final FileTransformer<C> transformer, final Path outDirectory ) {
        boolean hasFailed;
        final StringWriter errHolder = new StringWriter();
        final ImageMould<C> mould;
        try( final PrintWriter err = new PrintWriter( errHolder )) {
            mould = new ImageMould<>( boundaryPath, transformer, outDirectory, err );
            try { hasFailed = mould.formImage(); }
            catch( final UserError x ) {
                System.err.println( name + ": " + x.getMessage() );
                hasFailed = true; }
            err.flush(); }
        try { placeImageFiles( /*from*/mould.outDirectory, /*to*/mould.boundaryPathDirectory ); }
        catch( IOException x ) { throw new Unhandled( x ); } /* Failure might occur owing to an
          unwritable directory, but this is unlikely; the mould images only writeable directories. */
        System.err.print( errHolder.toString() );
        System.err.flush();
        return !hasFailed; }



    /** Returns the image file for the given source file: a sibling namesake with a `.xht` extension.
      * The image file of `dir/foo.brec`, for example, is `dir/foo.brec.xht`.
      */
    public static Path imageFile( final Path sourceFile ) {
        return sourceFile.resolveSibling( imageSimpleName( sourceFile )); }



    /** Returns the result of `sourceFile.{@linkplain Path#getFileName() getFileName}() + ".xht"`.
      */
    public static String imageSimpleName( final Path sourceFile ) {
        return sourceFile.getFileName() + ".xht"; }



    /** Whether the given reference is formally recognized, such that a Web imager
      * might try to obtain its referent.
      *
      *     @param ref A <a href='https://tools.ietf.org/html/rfc3986#section-4.1'>
      *       URI reference</a>.
      */
    static boolean looksReachable( final URI ref ) { /* Note that whether an imager
          would go ahead and image `ref` as a hyperlink is a separate question. */
        boolean answer = true;
        if( ref.isOpaque() ) answer = false; // No known use case.
        else {
            final String scheme = ref.getScheme();
            if( scheme == null ) {
                if( ref.getHost() != null ) answer = false; } // Too weird to trouble over.
            else {
                if( ref.getHost() == null ) answer = false; /* Too weird to trouble over.
                  Moreover such a hostless URI is allowed a rootless path, making it hard to
                  resolve from outside the network context (e.g. HTTP) implied by the scheme. */
                else if( !isHTTP( scheme )) answer = false; }} // No known use case.
        return answer; }



    /** Moves all simple files of directory `dFrom` to the same relative path of `dTo`,
      * replacing any that are already present.
      *
      *     @throws IllegalArgumentException Unless `dFrom` and `dTo` are directories.
      */
    public static void placeImageFiles​( final Path dFrom, final Path dTo ) throws IOException {
        verifyDirectoryArgument( dFrom );
        verifyDirectoryArgument( dTo );
        walkFileTree( dFrom, new SimpleFileVisitor<Path>() {
            public @Override FileVisitResult visitFile( final Path f, BasicFileAttributes _a )
                  throws IOException {
                Files.move( f, dTo.resolve(dFrom.relativize(f)), REPLACE_EXISTING );
                return CONTINUE; }});}



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private static final boolean isHTTP( final String scheme ) {
        if( scheme.startsWith( "http" )) {
            final int sN = scheme.length();
            if( sN == 4  ) return true;
            if( sN == 5 && scheme.endsWith("s") ) return true; }
        return false; }}



                                                   // Copyright © 2020-2021  Michael Allan.  Licence MIT.
