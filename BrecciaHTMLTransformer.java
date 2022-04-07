package Breccia.Web.imager;

import Breccia.parser.*;
import Breccia.XML.translator.BrecciaXCursor;
import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import Java.Unhandled;
import javax.xml.stream.*;

import static Breccia.parser.AssociativeReference.ReferentClause;
import static Breccia.parser.plain.Project.newSourceReader;
import static Breccia.Web.imager.Imaging.imageSimpleName;
import static Breccia.Web.imager.Project.logger;
import static Breccia.XML.translator.BrecciaXCursor.EMPTY;
import static javax.xml.stream.XMLStreamConstants.*;


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
        try( final Reader sourceReader = newSourceReader​( sourceFile );
             final Writer imageWriter = newImageWriter​( imageFile )) {
            sourceCursor.markupSource( sourceReader );
            sourceTranslator.markupSource( sourceCursor );
            final XMLStreamWriter out = xmlOutputFactory.createXMLStreamWriter( imageWriter );
            try {
                for( final BrecciaXCursor in = sourceTranslator;; ) {
                    switch( in.getEventType() ) {
                        case CHARACTERS -> {
                            final int bN = buffer.length;
                            for( int i = 0;; i += bN ) {
                                final int iN = in.getTextCharacters( i, buffer, 0, bN );
                                if( iN == 0 ) break; // End of input and nothing to write.
                                assert iN > 0 && iN <= bN;
                                out.writeCharacters( buffer, 0, iN );
                                if( iN < bN ) break; }} // End of input.
                        case EMPTY -> logger.fine( () -> "Imaging empty source file: " + sourceFile );
                        case END_DOCUMENT -> out.writeEndDocument();
                        case END_ELEMENT -> out.writeEndElement();
                        case START_DOCUMENT -> out.writeStartDocument();
                        case START_ELEMENT -> out.writeStartElement( in.getLocalName() );
                        default -> throw new IllegalStateException(); }
                    if( !in.hasNext() ) break;
                    try { in.next(); }
                    catch( final XMLStreamException x ) { throw (ParseError)(x.getCause()); }}}
            finally { out.close(); }}
        catch( IOException|XMLStreamException x ) { throw new Unhandled( x ); }}



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    final char[] buffer = new char[0x2000]; // or 8192



    /** Opens an image file for writing, returning a writer suited to the purpose.
      */
    private static Writer newImageWriter( final Path imageFile ) throws IOException { /* Little point
           in dealing with the `IOException` at this level, because anyway the caller must deal with it
           on closing the writer, usually by appending a `catch` to a try-with-resources block. */
        return java.nio.file.Files.newBuffered​Writer( imageFile ); }



    private final ReusableCursor sourceCursor;



    private static final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newDefaultFactory(); }



                                                   // Copyright © 2020-2022  Michael Allan.  Licence MIT.
