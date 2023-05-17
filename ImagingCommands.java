package Breccia.Web.imager;

import Breccia.parser.ReusableCursor;
import java.io.*;
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


public final class ImagingCommands {


    private ImagingCommands() {}



    /** Makes a Web image on behalf of a shell command.
      *
      *     @param <C> The type of source cursor to use.
      *     @param name The name of the shell command.
      *     @see ImageMould#boundaryPath
      *     @param projectOutputDirectory The output directory of the project that owns
      *       the shell command.
      *     @return True on success; false on failure.
      */
    public static <C extends ReusableCursor> boolean image( final String name,  final Path boundaryPath,
          final ImagingOptions opt, final FileTranslator.Maker<C> tMaker,
          final Path projectOutputDirectory ) {
        if( !exists( boundaryPath )) {
            err.println( name + ": No such file or directory: " + boundaryPath );
            return false; }
        final Path mouldOutputDirectory; {
            try { mouldOutputDirectory = emptyDirectory( createDirectories(
              projectOutputDirectory.resolve( "mould" ))); }
            catch( IOException x ) { throw new Unhandled( x ); }} // Unexpected here.
        boolean hasFailed;
        final StringWriter errHolder = new StringWriter();
        final ImageMould<C> mould;
        try( final PrintWriter errWriter = new PrintWriter( errHolder )) {
            mould = new ImageMould<>( boundaryPath, opt, mouldOutputDirectory, errWriter );
            mould.initialize( tMaker.newTranslator( mould ));
            try { hasFailed = !mould.formImage(); }
            catch( final UserError x ) {
                err.println( name + ": " + x.getMessage() );
                hasFailed = true; }
            errWriter.flush(); }
        try { placeImageFiles( /*from*/mouldOutputDirectory, /*to*/mould.boundaryPathDirectory, opt ); }
        catch( IOException x ) { throw new Unhandled( x ); } /* Failure might occur owing to an
          unwritable directory, but this is unlikely; the mould images only writeable directories. */
        err.print( errHolder.toString() );
        err.flush();
        return !hasFailed; }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    /** Moves all simple files of directory `dFrom` to the same relative path of `dTo`,
      * replacing any that are already present.
      *
      *     @throws IllegalArgumentException Unless `dFrom` and `dTo` are directories.
      */
    private static void placeImageFiles​( final Path dFrom, final Path dTo, final ImagingOptions opt )
          throws IOException {
        verifyDirectoryArgument( dFrom );
        verifyDirectoryArgument( dTo );
        final boolean toDo = !opt.toFake();
        walkFileTree( dFrom, new SimpleFileVisitor<Path>() {
            public @Override FileVisitResult visitFile( final Path f, BasicFileAttributes _a )
                  throws IOException {
                if( toDo ) Files.move( f, dTo.resolve(dFrom.relativize(f)), REPLACE_EXISTING );
                return CONTINUE; }}); }}



                                                   // Copyright © 2020-2023  Michael Allan.  Licence MIT.
