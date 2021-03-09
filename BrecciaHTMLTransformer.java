package Breccia.Web.imager;

import Breccia.parser.*;
import Breccia.XML.translator.BrecciaXCursor;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Path;
import Java.Unhandled;
import javax.xml.stream.XMLStreamException;

import static Breccia.parser.AssociativeReference.ReferentClause;
import static Breccia.parser.Project.newSourceReader;
import static Breccia.Web.imager.Imaging.imageSimpleName;
import static Breccia.Web.imager.Project.logger;
import static Breccia.XML.translator.BrecciaXCursor.EMPTY;
import static java.nio.file.Files.createFile;


public final class BrecciaHTMLTransformer implements FileTransformer<BrecciaCursor> {


    /** @see #sourceCursor
      * @see #sourceTranslator
      */
    public BrecciaHTMLTransformer( BrecciaCursor sourceCursor, BrecciaXCursor sourceTranslator ) {
        this.sourceCursor = sourceCursor;
        this.sourceTranslator = sourceTranslator; }



    /** The source translator to use during calls to this transformer.
      * Between calls, it may be used for other purposes.
      */
    public final BrecciaXCursor sourceTranslator;



   // ━━━  F i l e   T r a n s f o r m e r  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override FlatMarkup formalReferenceAt( final BrecciaCursor in ) {
        // Only two ‘formal’ cases exist, each contained in (iR) a resource indicator.
        ResourceIndicator iR; iR: {      // Each `iR`, in turn, is contained in (cR) the
            ReferentClause cR = null; { // referent clause of an associative reference.
                var __ =              in.asAssociativeReference(); // ↓ Drill down.
                if( __ != null ) cR = __.imperativeClause.referentClause(); }
            if( cR == null ) return null;

          // inferential referent indicator with (iF) a fractum indicator, in turn
          // ┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈ with (iR) a resource indicator
            var __ =                 cR.inferentialReferentIndicator(); // ↓ Drill down.
            if( __ != null ) {
                var ___ =            __.containmentClause();
                if( ___ != null ) {
                    final var iF =  ___.fractumIndicator;
                    iR =             iF.resourceIndicator();
                    if( iR != null ) break iR; }}

          // bare fractum indicator (iF) with both a pattern and (iR) resource indicator
          // ┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈
            final var iF = cR.fractumIndicator();
            if( iF == null ) return null;
            if( iF.patterns.size() == 0 || (iR = iF.resourceIndicator()) == null ) return null; }

        if( !iR.isFractal() ) return null; /* Fractal alone implies formal, non-fractal implying a
          resource whose content is opaque to this transformer and ∴ indeterminate of image form. */
        return iR.reference; } /* The indicated resource of `iR` is formal ∵ the associative reference
          (the markup in which indicator `iR` is contained) will be imaged as a hyperlink whose form
          depends on the content of the resource.  In short, it is formal ∵ it informs the image. */



    public @Override BrecciaCursor sourceCursor() { return sourceCursor; }



    public @Override void transform( final Path sourceFile, final Path imageDirectory )
          throws ParseError, TransformError {
        try( final Reader source = newSourceReader​( sourceFile )) {
            final BrecciaXCursor inX = sourceTranslator;
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
        catch( IOException x ) { throw new Unhandled( x ); }}



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private final BrecciaCursor sourceCursor; }



                                                   // Copyright © 2020-2021  Michael Allan.  Licence MIT.
