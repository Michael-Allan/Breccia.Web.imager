package Breccia.Web.imager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import Java.UserError;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.logging.Level.FINE;


/** An image production set.
  */
public final class ProductionSet {


    /** @throws UserError If any of the given paths is unreadable or denotes an unwritable directory.
      */
    public ProductionSet( final Set<Path> paths, final ProductionFlow flow ) throws UserError {
        for( final Path p: paths ) { // Test all the given paths up front
              // as the user is likely to want a complete production set.
            if( !Files.isReadable( p )) throw new UserError( "Path is unreadable: " + p );
            if( Files.isDirectory(p) && !Files.isWritable(p) ) {
                throw new UserError( "Directory is unwritable: " + p ); }}
        this.paths = paths;
        this.flow = flow; }



    /** Creates or updates the image of this production set.  Logs a record at level `FINE`
      * for any unreadable Breccian file, and for any unreadable or unwritable directory.
      *
      *     @see java.util.logging.Level#FINE
      */
    public void produce() {
        for( final Path p: paths ) { /* Herein a streamlined process versus that of `processSubpath`
              whose added testing and logging would be redundant for these top paths. */
            if( Files.isDirectory( p )) processDirectory( p );
            else image( p ); }} // Regardless of whether it `looksBreccian`, the user has asked for it.



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private final ProductionFlow flow;



    /** @param f The path of the file to image.
      */
    private void image( final Path f ) {
        System.out.println( "   would image " + f ); } // TEST



    private final Logger logger = Logger.getLogger( "Breccia.Web.imager" );
      // The logger proper to the Breccia Web imager.



    /** @param f The path of a file.
      */
    private boolean looksBreccian( final Path f ) {
        return f.getFileName().toString().endsWith( ".brec" ); }



    private final Set<Path> paths;



    /** @param d The path of the directory to process.
      */
    private void processDirectory( final Path d ) {
        try( final Stream<Path> pp = Files.list( d )) {
            for( final Path p: (Iterable<Path>)pp::iterator ) processSubpath( p ); }
        catch( IOException x ) { throw new RuntimeException( x ); }}



    private void processSubpath( final Path p ) {
        if( Files.isReadable( p )) {
            if( Files.isDirectory( p )) {
                if( Files.isWritable( p )) processDirectory( p );
                else logger.log( FINE, "Skipping unwritable directory: {0}/", p ); }
            else if( looksBreccian( p )) image( p ); }
        else if( logger.isLoggable(FINE) && wouldRead(p) ) {
            logger.log( FINE, "Skipping unreadable path: {0}", p ); }}



    /** Answers whether `p` would be read during image production if it were readable.
      */
    private boolean wouldRead( final Path p ) { return Files.isDirectory(p) || looksBreccian(p); }}



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
