package Breccia.Web.imager;

import Breccia.parser.*;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Path;
import Java.Unhandled;
import javax.xml.stream.XMLStreamException;

import static Breccia.parser.BrecciaXCursor.EMPTY;
import static Breccia.parser.Project.newSourceReader;
import static Breccia.Web.imager.Imaging.imageSimpleName;
import static Breccia.Web.imager.Project.logger;
import static java.nio.file.Files.createFile;


public final class BrecciaHTMLTransformer implements FileTransformer<BrecciaCursor> {


    /** @see #inX
      */
    public BrecciaHTMLTransformer( BrecciaXCursor inX ) { this.inX = inX; }



    /** The source translator to use during calls to this transformer.
      * Between calls, it may be used for other purposes.
      */
    public final BrecciaXCursor inX;



   // ━━━  F i l e   T r a n s f o r m e r  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override Markup formalReferenceAt( final BrecciaCursor sourceCursor ) {
        return null; } // TODO



    public @Override BrecciaCursor sourceCursor() { return inX.sourceCursor(); }



    public @Override void transform( final Path sourceFile, final Path imageDirectory )
          throws ParseError, TransformError {
        try( final Reader source = newSourceReader​( sourceFile )) {
            inX.markupSource( source ); /* Better not to parse functionally using `inX.perState`
              and mess with shipping a checked `TransformError` out of the lambda function. */
            for( ;; ) {
                switch( inX.getEventType() ) {
                    case EMPTY -> {
                        logger.fine( () -> "Imaging empty source file: " + sourceFile );
                        final Path imageFile = imageDirectory.resolve( imageSimpleName( sourceFile ));
                        createFile( imageFile ); }}
                    // TODO, the actual transform for other states.
                if( !inX.hasNext() ) break;
                try { inX.next(); }
                catch( final XMLStreamException x ) { throw (ParseError)(x.getCause()); }}}
        catch( IOException x ) { throw new Unhandled( x ); }}}



                                                   // Copyright © 2020-2021  Michael Allan.  Licence MIT.
