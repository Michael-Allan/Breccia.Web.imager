package Breccia.Web.imager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import Java.UserError;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamWriter;

import static java.util.logging.Level.FINE;


/** A mould to form or reform a Web image.
  */
public final class ImageMould {


    /** @throws UserError If any of the given source paths denotes an unwritable or unreadable directory,
      *   or an unreadable Breccian file.
      */
    public ImageMould( final Set<Path> sourcePaths, final FileTransformMaker transformMaker )
          throws UserError {
        for( final Path p: sourcePaths ) { // Test their accessibility up front.
            if( wouldRead(p) && !Files.isReadable(p)) throw new UserError( "Path is unreadable: " + p );
            if( Files.isDirectory(p) && !Files.isWritable(p) ) {
                throw new UserError( "Directory is unwritable: " + p ); }}
        this.sourcePaths = sourcePaths;
        this.transformMaker = transformMaker; }



    /** Forms or reforms the image.  Logs a record at level `FINE` for any unreadable
      * Breccian source file, and for any unreadable or unwritable directory.
      *
      *     @see java.util.logging.Level#FINE
      */
    public void formImage() {
        for( final Path p: sourcePaths ) { /* Herein a streamlined process versus that of `pullPath`
              whose added testing and logging would be redundant for these top paths. */
            if( Files.isDirectory( p )) pullDirectory( p );
            else pullFile( p ); }}



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private final Logger logger = Logger.getLogger( "Breccia.Web.imager" );
      // The logger proper to the Breccia Web imager.



    /** @param f The path of a file.
      */
    private boolean looksBreccian( final Path f ) {
        return f.getFileName().toString().endsWith( ".brec" ); }



    /** @param d The path of a source directory to pull into the mould.
      */
    private void pullDirectory( final Path d ) {
        try( final Stream<Path> pp = Files.list( d )) {
            for( final Path p: (Iterable<Path>)pp::iterator ) pullPath( p ); }
        catch( IOException x ) { throw new RuntimeException( x ); }}



    /** @param f The path of a source file to pull into the mould.
      */
    private void pullFile( final Path f ) { if( looksBreccian( f )) transformFile( f ); }



    /** @param p A path to pull into the mould.
      */
    private void pullPath( final Path p ) {
        if( Files.isReadable( p )) { // Herein cf. `formImage`.
            if( Files.isDirectory( p )) {
                if( Files.isWritable( p )) pullDirectory( p );
                else logger.log( FINE, "Skipping unwritable directory: {0}/", p ); }
            else pullFile( p ); }
        else if( logger.isLoggable(FINE) && wouldRead(p) ) {
            logger.log( FINE, "Skipping unreadable path: {0}", p ); }}



    private final Set<Path> sourcePaths;



    private final FileTransformMaker transformMaker;



    /** Transforms a single Breccian source file into an HTML file, forming (or reforming)
      * a part of the image.
      *
      *     @param f The path of a Breccian source file.
      */
    private void transformFile( final Path f ) {
        System.out.println( "   ← " + f ); // TEST
        try( final FileTransform transform = transformMaker.makeFileTransform( f )) {
            final BrecciaReader in = transform.input();
            final MarkupTransformer t = transform.transformer();
            final XMLStreamWriter out = transform.output();
            for( ;; ) {
                t.transform( in, out );
                if( in.hasNext() ) in.next();
                else break; }}
        catch( IOException x ) { throw new RuntimeException( x ); }}



    /** Answers whether path `p` would be read during image formation if it were readable.
      */
    private boolean wouldRead( final Path p ) { return Files.isDirectory(p) || looksBreccian(p); }}



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
