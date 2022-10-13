package Breccia.Web.imager;

import Breccia.parser.ReusableCursor;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import Java.Unhandled;
import Java.UserError;

import static Java.Files.emptyDirectory;
import static Java.Files.verifyDirectoryArgument;
import static java.lang.System.err;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static Java.URIs.isHTTP;


public final class Imaging {


    private Imaging() {}



    /** Makes a Web image on behalf of a shell command.
      *
      *     @param <C> The type of source cursor to use.
      *     @param name The name of the shell command.
      *     @see ImageMould#boundaryPath
      *     @param outProject The output directory of the project that owns the shell command.
      *     @return True on success; false on failure.
      */
    public static <C extends ReusableCursor> boolean image( final String name,  final Path boundaryPath,
          final ImagingOptions opt, final FileTranslator.Maker<C> tMaker, final Path outProject ) {
        if( !exists( boundaryPath )) {
            err.println( name + ": No such file or directory: " + boundaryPath );
            return false; }
        final Path out;
        try { out = emptyDirectory( createDirectories( outProject.resolve( Path.of( "mould" )))); }
        catch( IOException x ) { throw new Unhandled( x ); } // Unexpected for `outDirectory`.
        boolean hasFailed;
        final StringWriter errHolder = new StringWriter();
        final ImageMould<C> mould;
        try( final PrintWriter errWriter = new PrintWriter( errHolder )) {
            mould = new ImageMould<>( boundaryPath, opt, out, errWriter );
            mould.initialize( tMaker.newTranslator( mould ));
            try { hasFailed = mould.formImage(); }
            catch( final UserError x ) {
                err.println( name + ": " + x.getMessage() );
                hasFailed = true; }
            errWriter.flush(); }
        try { placeImageFiles( /*from*/out, /*to*/mould.boundaryPathDirectory ); }
        catch( IOException x ) { throw new Unhandled( x ); } /* Failure might occur owing to an
          unwritable directory, but this is unlikely; the mould images only writeable directories. */
        err.print( errHolder.toString() );
        err.flush();
        return !hasFailed; }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    /** Whether the given reference is formally recognized, such that a Web imager
      * might try to obtain its referent.
      *
      *     @param ref A <a href='https://datatracker.ietf.org/doc/html/rfc3986#section-4.1'>
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
    private static void placeImageFiles​( final Path dFrom, final Path dTo ) throws IOException {
        verifyDirectoryArgument( dFrom );
        verifyDirectoryArgument( dTo );
        walkFileTree( dFrom, new SimpleFileVisitor<Path>() {
            public @Override FileVisitResult visitFile( final Path f, BasicFileAttributes _a )
                  throws IOException {
                Files.move( f, dTo.resolve(dFrom.relativize(f)), REPLACE_EXISTING );
                return CONTINUE; }}); }}



                                                   // Copyright © 2020-2022  Michael Allan.  Licence MIT.
