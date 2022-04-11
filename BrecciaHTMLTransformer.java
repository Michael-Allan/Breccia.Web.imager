package Breccia.Web.imager;

import Breccia.parser.*;
import Breccia.XML.translator.BrecciaXCursor;
import java.io.*;
import java.nio.file.Path;
import Java.Unhandled;
import javax.xml.transform.*;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static Breccia.parser.AssociativeReference.ReferentClause;
import static Breccia.parser.Typestamp.empty;
import static Breccia.parser.plain.Project.newSourceReader;
import static Breccia.Web.imager.Imaging.imageSimpleName;
import static Breccia.Web.imager.Project.logger;
import static Breccia.XML.translator.XStreamConstants.EMPTY;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.newOutputStream;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static javax.xml.transform.OutputKeys.ENCODING;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;


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
        final Path imageFile = imageDirectory.resolve( imageSimpleName( sourceFile ));
        try {
          // Breccia text file → X-Breccia parse events → X-Breccia DOM
          // ──────────────────────────────────────────────────────────
            try( final Reader sourceReader = newSourceReader​( sourceFile )) {
                sourceCursor.markupSource( sourceReader );
                if( sourceCursor.state().typestamp() == empty ) {
                    logger.fine( () -> "Imaging empty source file: " + sourceFile );
                    createFile( imageFile ); // Special case, no content to transform.
                    return; }
                sourceTranslator.markupSource( sourceCursor );
                domOutput.setNode( null/*make a new document*/ );
                identityTransformer.transform( new StAXSource(sourceTranslator), domOutput ); }
                  // `StAXSource` is ‘not reusable’ according to its API.  How that could be is puzzling
                  // given that it’s a pure wrapper, but let’s humour it.

          // X-Breccia DOM → X-Breccia text file
          // ───────────────────────────────────
            domInput.setNode( domOutput.getNode() );
            try( final OutputStream imageWriter = newOutputStream​( imageFile, CREATE_NEW )) {
                imageFileOutput.setOutputStream( imageWriter );
                identityTransformer.transform( domInput, imageFileOutput ); }}
        catch( IOException|TransformerException x ) { throw new Unhandled( x ); }}



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private final DOMSource domInput = new DOMSource();



    private final DOMResult domOutput = new DOMResult();



    private final Transformer identityTransformer; {
        Transformer t;
        try { t = TransformerFactory.newInstance().newTransformer(); }
        catch( TransformerConfigurationException x ) { throw new Unhandled( x ); }
        t.setOutputProperty( ENCODING, "UTF-8" );
        t.setOutputProperty( OMIT_XML_DECLARATION, "yes" );
        identityTransformer = t; }



    private final StreamResult imageFileOutput = new StreamResult();



    private final ReusableCursor sourceCursor; }



                                                   // Copyright © 2020-2022  Michael Allan.  Licence MIT.
