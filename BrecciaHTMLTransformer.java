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
import static Breccia.parser.plain.Project.newSourceReader;
import static Breccia.Web.imager.Imaging.imageSimpleName;
import static Breccia.Web.imager.Project.logger;
import static Breccia.XML.translator.BrecciaXCursor.EMPTY;
import static java.nio.file.Files.createFile;


public final class BrecciaHTMLTransformer implements FileTransformer<ReusableCursor> {


    /** @see #sourceCursor
      * @see #sourceTranslator
      */
    public BrecciaHTMLTransformer( ReusableCursor sourceCursor, BrecciaXCursor sourceTranslator ) {
        this.sourceCursor = sourceCursor;
        this.sourceTranslator = sourceTranslator; }



    /** The source translator to use during calls to this transformer.
      * Between calls, it may be used for other purposes.
      */
    public final BrecciaXCursor sourceTranslator;



   // ━━━  F i l e   T r a n s f o r m e r  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override Markup formalReferenceAt( final ReusableCursor in ) throws ParseError {
        final ResourceIndicant iR; {
            FractumIndicant iF; {
                final ReferentClause cR; {
                    final AssociativeReference rA; {
                        rA = in.asAssociativeReference();
                        if( rA == null ) return null; }
                    cR = rA.referentClause();
                    if( cR == null ) return null; }
                final var iIR = cR.inferentialReferentIndicant();
                if( iIR == null ) { // Then `cR` itself directly contains any `iF`.
                    iF = cR.fractumIndicant();
                    if( iF.patternMatchers() == null ) return null; } /* Without a matcher, `iF`
                      indicates the resource as a whole, making it informal within the image. */
                else iF = iIR.fractumIndicant(); /* The `iIR` of `cR` alone contains any `iF`.  Whether
                  this `iF` includes a matcher is immaterial ∵ already `iIR` itself infers one. */
                if( iF == null ) return null; }
            iR = iF.resourceIndicant();
            if( iR == null ) return null; } /* The absence of `iR` implies that the indicated resource
              is the containing file, which is not an external resource as required by the API. */
        if( iR.qualifiers().contains( "non-fractal" )) return null; /* Fractal alone implies formal,
          non-fractal implying a resource whose content is opaque to this transformer and therefore
          indeterminate of image form. */
        return iR.reference(); } /* The resource of `iR` is formal ∵ the associative reference containing
          `iR`refers to a matcher of markup *in* the resource and ∴ will be imaged as a hyperlink whose
          form depends on the content of the resource.  In short, it is formal ∵ it informs the image. */



    public @Override ReusableCursor sourceCursor() { return sourceCursor; }



    public @Override void transform( final Path sourceFile, final Path imageDirectory )
          throws ParseError, TransformError {
        try( final Reader sourceReader = newSourceReader​( sourceFile )) {
            sourceCursor.markupSource( sourceReader );
            sourceTranslator.markupSource( sourceCursor ); /* Better not to parse functionally using its
              `perState` and mess with shipping a checked `TransformError` out of the lambda function. */
            for( final BrecciaXCursor inX = sourceTranslator;; ) {
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


    private final ReusableCursor sourceCursor; }



                                                   // Copyright © 2020-2022  Michael Allan.  Licence MIT.
