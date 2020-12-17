package Breccia.Web.imager;

import java.io.IOException;
import java.nio.file.Path;
import Java.UserError;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static Breccia.Web.imager.Project.logger;
import static java.nio.file.Files.*;
import static java.util.logging.Level.FINE;


/** A mould to form or reform a Web image.
  */
public final class ImageMould {


    /** @throws UserError If any of the given source paths denotes an unwritable or unreadable directory,
      *   or an unreadable Breccian file.
      */
    public ImageMould( final Set<Path> sourcePaths, final FileTransformer transformer )
          throws UserError {
        for( final Path p: sourcePaths ) { // Test their accessibility up front.
            if( wouldRead(p) && !isReadable(p) ) throw new UserError( "Path is unreadable: " + p );
            if( isDirectory(p) && !isWritable(p) ) {
                throw new UserError( "Directory is unwritable: " + p ); }}
        this.sourcePaths = sourcePaths;
        this.transformer = transformer; }



    /** Forms or reforms the image.
      */
    public void formImage() {

      // Pull source files into the mould, sorting them as imageable or indeterminate
      // ────────────────────────────────
        for( final Path p: sourcePaths ) { /* Herein a streamlined process versus that of `pullPath`
              whose added testing and logging would be redundant for these top paths. */
            if( isDirectory( p )) pullDirectory( p );
            else pullFile( p ); }

      // Transform each imageable source file, forming or reforming part of the image
      // ────────────────────────────────────
        imageabilityDetermination.forEach( (f, isImageable) -> {
            if( isImageable != null && isImageable.get() == false ) return;
            try { transformer.transform( f ); }
            catch( IOException x ) { throw new RuntimeException( x ); }});}



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    /** Source files (keys) mapped each to the present determination of the file’s
      * imageability (value).  Initially the mapped value is either null or false.
      * A false value may, in the process of determination, change to true.
      * Null and true values never change and alone indicate an imageable file.
      */
    final Map<Path,AtomicBoolean> imageabilityDetermination = new HashMap<>( /*initial capacity*/8192 );



    /** @param f The path of a file.
      */
    private boolean looksBreccian( final Path f ) {
        return f.getFileName().toString().endsWith( ".brec" ); }



    /** @param d The path of a source directory to pull into the mould.
      */
    private void pullDirectory( final Path d ) {
        try( final Stream<Path> pp = list( d )) {
            for( final Path p: (Iterable<Path>)pp::iterator ) pullPath( p ); }
        catch( IOException x ) { throw new RuntimeException( x ); }}



    /** @param f The path of a source file to pull into the mould.
      */
    private void pullFile( final Path f ) {
        if( !looksBreccian( f )) return;
        if( imageabilityDetermination.containsKey( f )) return;
        final Path fImage = f.resolveSibling( f.getFileName() + ".xht" );
        try {
            if( !exists(fImage) || getLastModifiedTime(f).compareTo(getLastModifiedTime(fImage)) >= 0 ) {
                System.out.println( "   ← " + f ); // TEST
                imageabilityDetermination.put​( f, null ); } // The source file is to be imaged.
            else imageabilityDetermination.put​( f, new AtomicBoolean() ); }
              // The imageability of the source file is to be determined.
        catch( IOException x ) { throw new RuntimeException( x ); }}



    /** @param p A path to pull into the mould.
      */
    private void pullPath( final Path p ) {
        if( isReadable( p )) { // Herein cf. `formImage`.
            if( isDirectory( p )) {
                if( isWritable( p )) pullDirectory( p );
                else logger.log( FINE, "Skipping unwritable directory: {0}/", p ); }
            else pullFile( p ); }
        else if( logger.isLoggable(FINE) && wouldRead(p) ) {
            logger.log( FINE, "Skipping unreadable path: {0}", p ); }}



    private final Set<Path> sourcePaths;



    private final FileTransformer transformer;



    /** Answers whether path `p` would be read during image formation if it were readable.
      */
    private boolean wouldRead( final Path p ) { return isDirectory(p) || looksBreccian(p); }}



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
