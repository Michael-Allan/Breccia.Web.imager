package Breccia.Web.imager;

import java.io.IOException;
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



    /** Executes a `breccia-web-image` shell command.
      *
      *     @param mould The image mould to use.
      *     @return True on success, false on failure.
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec'>
      *       The `breccia-web-image` command of the Breccia Web imager</a>
      *     @see <a href='http://reluk.ca/project/wayic/Web/imager/bin/breccia-web-image.brec'>
      *       The `image` command of the waycast Web imager</a>
      */
    public static boolean image( final ImageMould mould ) {
        boolean hasFailed = false;
        try { mould.formImage(); }
        catch( final UserError x ) {
            System.err.println( "breccia-web-image: " + x.getMessage() );
            hasFailed = true; }
        try { placeImageFiles( /*from*/mould.outDirectory, /*to*/mould.boundaryPathDirectory ); }
        catch( IOException x ) { throw new Unhandled( x ); } /* Failure might occur owing to an
          unwritable directory, but this is unlikely; the mould images only writeable directories. */
        return !hasFailed; }



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
                return CONTINUE; }});}}



                                                   // Copyright © 2020-2021  Michael Allan.  Licence MIT.
