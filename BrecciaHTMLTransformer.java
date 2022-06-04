package Breccia.Web.imager;

import Breccia.parser.*;
import Breccia.XML.translator.BrecciaXCursor;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.*;
import java.nio.file.Path;
import Java.*;
import java.util.*;
import java.util.regex.Matcher;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.*;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

import static Breccia.parser.AssociativeReference.ReferentClause;
import static Breccia.parser.Typestamp.empty;
import static Breccia.parser.plain.Language.completesNewline;
import static Breccia.parser.plain.Project.newSourceReader;
import static Breccia.Web.imager.Imaging.imageSimpleName;
import static Breccia.Web.imager.Project.logger;
import static Breccia.Web.imager.TransformError.wrnHead;
import static Breccia.XML.translator.XStreamConstants.EMPTY;
import static java.awt.Font.createFont;
import static java.awt.Font.TRUETYPE_FONT;
import static java.lang.Character.charCount;
import static java.lang.Character.isAlphabetic;
import static java.lang.Character.isDigit;
import static java.lang.Integer.parseInt;
import static java.lang.Integer.parseUnsignedInt;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.newOutputStream;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static Java.Nodes.asText;
import static Java.Nodes.parentElement;
import static Java.Nodes.successor;
import static Java.Nodes.successorAfter;
import static Java.Nodes.successorElement;
import static Java.StringBuilding.clear;
import static Java.StringBuilding.collapseWhitespace;
import static Java.Unicode.graphemePattern;
import static java.util.Arrays.sort;
import static javax.xml.transform.OutputKeys.*;


/** @param <C> The type of source cursor used by this transformer.
  */
public class BrecciaHTMLTransformer<C extends ReusableCursor> implements FileTransformer<C> {


    /** @see #sourceCursor()
      * @see #sourceTranslator
      */
    public BrecciaHTMLTransformer( C sourceCursor, BrecciaXCursor sourceTranslator,
          final ImageMould<?> mould ) {
        this.sourceCursor = sourceCursor;
        this.sourceTranslator = sourceTranslator;
        this.mould = mould;
        opt = mould.opt;
        final String f = opt.glyphTestFont();
        if( !f.equals( "none" )) {
            try( final var in = new FileInputStream( f )) {
                glyphTestFont = createFont( TRUETYPE_FONT/*includes all of OpenType*/,
                  /*buffered by callee in JDK 17*/in); }
            catch( FontFormatException|IOException x ) { throw new Unhandled( x ); }}}



    /** The source translator to use during calls to this transformer.
      * Between calls, it may be used for other purposes.
      */
    public final BrecciaXCursor sourceTranslator;



   // ━━━  F i l e   T r a n s f o r m e r  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override Markup formalReferenceAt( final C in ) throws ParseError {
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



    public final @Override C sourceCursor() { return sourceCursor; }



    public @Override void transform( final Path sourceFile, final Path imageDirectory )
          throws ParseError, TransformError {
        final Path imageFile = imageDirectory.resolve( imageSimpleName( sourceFile ));
        try {
            createDirectories( imageFile.getParent() ); // Ensure the parent exists.
            try( final Reader sourceReader = newSourceReader​( sourceFile )) {
                sourceCursor.markupSource( sourceReader );
                if( sourceCursor.state().typestamp() == empty ) {
                    logger.fine( () -> "Imaging empty source file: " + sourceFile );
                    createFile( imageFile ); // Special case, no content to transform.
                    return; }

              // X-Breccia DOM ← X-Breccia parse events ← Breccia source file
              // ─────────────
                sourceTranslator.markupSource( sourceCursor );
                toDOM.setNode( null/*make a new document*/ );
                try { identityTransformer.transform( new StAXSource(sourceTranslator), toDOM ); }
                  // `StAXSource` is ‘not reusable’ according to its API.  How that could be is puzzling
                  // given that it’s a pure wrapper, but let’s humour it.
                catch( final TransformerException xT ) {
                    if( xT.getCause() instanceof XMLStreamException ) {
                        final XMLStreamException xS = (XMLStreamException)(xT.getCause());
                        throw (ParseError)(xS.getCause()); } /* So advertising that the location data
                          of `ParseError` is available for the exception, in case the caller wants it. */
                    throw xT; }}
            final Document d = (Document)(toDOM.getNode());

          // Glyph testing
          // ─────────────
            if( glyphTestFont != null ) {
                unsMap.clear(); // Map of unglyphed characters.
                Node n = d.getFirstChild();
                do {
                    final Text nText = asText( n );
                    if( nText == null ) continue;
                    assert !nText.isElementContentWhitespace(); /* The `sourceTranslator` has produced
                      ‘X-Breccia with no ignorable whitespace’. */
                    final String text = nText.getData();
                    for( int ch, c = 0, cN = text.length(); c < cN; c += charCount(ch) ) {
                        ch = text.codePointAt( c );
                        if( glyphTestFont.canDisplay( ch )) continue;
                        UnglyphedCharacter un = unsMap.get( ch );
                        if( un == null ) {
                            un = new UnglyphedCharacter( glyphTestFont.getFontName(), ch,
                              characterPointer( nText, c ));
                            unsMap.put( ch, un ); }
                        ++un.count; }}
                    while( (n = successor(n)) != null );
                if( !unsMap.isEmpty() ) {
                    final var uns = unsMap.values().toArray( unArrayType );
                    sort( uns, unsComparator );
                    for( var un: uns ) {
                        mould.wrn().println( wrnHead(sourceFile,un.pointer.lineNumber) + un ); }}}

          // XHTML DOM ← X-Breccia DOM
          // ─────────
            transform( d );

          // XHTML image file ← XHTML DOM
          // ────────────────
            fromDOM.setNode( d );
            try( final OutputStream imageWriter = newOutputStream​( imageFile, CREATE_NEW )) {
                toImageFile.setOutputStream( imageWriter );
                identityTransformer.transform( fromDOM, toImageFile ); }}
        catch( IOException|TransformerException x ) {
            throw new TransformError( imageFile, "Unable to make image file", x ); }}



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    /** Removes to the bullet `b` any content of `bP`, forming it as a punctuation element.
      */
    private void appendAnyP( final Element b, final StringBuilder bP ) {
        final int cN = bP.length();
        if( cN > 0 ) {
            final Document d = b.getOwnerDocument();
            final Element punctuation = d.createElementNS( nsImager, "img:punctuation" );
            b.appendChild( punctuation );
            punctuation.appendChild( d.createTextNode( bP.toString() ));
            clear( bP ); }}



    /** Removes to the bullet `b` any content of `bQ`, forming it as flat text.
      */
    private void appendAnyQ( final Element b, final StringBuilder bQ ) {
        final int cN = bQ.length();
        if( cN > 0 ) {
            b.appendChild( b.getOwnerDocument().createTextNode( bQ.toString() ));
            clear( bQ ); }}



    /** @param markup An element of Breccian markup.
      * @param c The offset in `markup` context of the character to point to.
      */
    private CharacterPointer characterPointer( final Element markup, final int c ) {
        final String textRegional;
        final IntArrayExtensor endsRegional = lineLocator.endsRegional;
        final int offsetRegional;
        final int numberRegional;
        for( Element h = markup;; ) {
            if( "Head".equals( h.getLocalName() )) {
                textRegional = sourceText( h );
                endsRegional.clear();
                final StringTokenizer tt = new StringTokenizer( h.getAttribute( "xuncLineEnds" ));
                while( tt.hasMoreTokens() ) endsRegional.add( parseUnsignedInt( tt.nextToken() ));
                offsetRegional = parseUnsignedInt( h.getAttribute( "xunc" ));
                numberRegional = parseUnsignedInt( h.getAttribute( "lineNumber" ));
                break; }
            h = parentElement( h );
            if( h == null ) throw new IllegalArgumentException( markup.toString() ); }

      // Locate the line
      // ───────────────
        int offset = c + parseUnsignedInt( markup.getAttribute( "xunc" )); // `markup` → whole text
        lineLocator.locateLine( offset, offsetRegional, numberRegional );

      // Resolve its content
      // ───────────────────
        final int lineStart = lineLocator.start() - offsetRegional; // whole text → `textRegional`
        final String line = textRegional.substring( lineStart,
          endsRegional.array[lineLocator.index()] - offsetRegional ); // whole text → `textRegional`

      // Form the pointer
      // ────────────────
        offset -= offsetRegional; // whole text → `textRegional`
        final int column = columnarSpan( textRegional, lineStart, offset );
        return new CharacterPointer( line, lineLocator.number(), column ); }



    /** @param markup The text of an element of Breccian markup.
      * @param c The offset in `markup` context of the character to point to.
      */
    private CharacterPointer characterPointer( final Text markup, int c ) {
        final Element p = parentElement( markup );
        if( p == null ) throw new IllegalArgumentException( markup.toString() );
        return characterPointer( p, c ); }



    /** Returns the number of grapheme clusters within `text` between positions `start` and `end`.
      *
      *     @see <a href='https://unicode.org/reports/tr29/#Grapheme_Cluster_Boundaries'>
      *       Grapheme cluster boundaries in Unicode text segmentation</a>
      */
    private int columnarSpan( final String text, final int start, final int end ) {
        graphemeMatcher.reset( text ).region( start, end );
        int count = 0;
        while( graphemeMatcher.find() ) ++count;
        return count; }



    /** @param head A `Head` element representing a fractal head.
      * @return The file title as derived from the head, or null if it yields none.
      */
    private String fileTitle( Node head ) {
        final String titlingExtract; // The relevant text extracted from the fractal head.
        if( "Division".equals( head.getParentNode().getLocalName() )) { // Then `head` is a divider.
            for( Node n = successor(head);;  n = successor(n) ) {
                if( n == null ) return null;
                if( "DivisionLabel".equals( n.getLocalName() )) {
                    titlingExtract = n.getTextContent();
                    break; }}}
        else { // Presumeably `head` is a file head or point head.
            head = head.cloneNode( /*deeply*/true ); /* So both preserving the original,
              and keeping the nodal scan that follows within the bounds of the isolated copy. */
            strip: for( Node p, n = successor(p = head);  n != null;  n = successor(p = n) ) {
                final String localName = n.getLocalName();
                if( "IndentBlind".equals( localName )) for( ;; ) { // Then remove `n` and all successors.
                    final Node s = successorAfter( n );
                    n.getParentNode().removeChild( n );
                    if( s == null ) break strip;
                    n = s; }
                if( "CommentAppender".equals( localName )
                 || "CommentBlock"   .equals( localName )) { // Then `n` is a comment carrier, remove it.
                    final Node c = n;
                    c.getParentNode().removeChild( c );
                    n = p; }} // Resuming from the predecessor of comment carrier `n`, now removed.
            titlingExtract = head.getTextContent(); }
        final StringBuilder b = clear(stringBuilder).append( titlingExtract );
        collapseWhitespace( b );
        return b.isEmpty() ? null : b.toString(); }



    private final DOMSource fromDOM = new DOMSource();



    private Font glyphTestFont;



    private final Matcher graphemeMatcher = graphemePattern.matcher( "" );



    private final Transformer identityTransformer; {
        Transformer t;
        try { t = TransformerFactory.newInstance().newTransformer(); }
        catch( TransformerConfigurationException x ) { throw new Unhandled( x ); }
        t.setOutputProperty( DOCTYPE_SYSTEM, "about:legacy-compat" ); // [DI]
        t.setOutputProperty( ENCODING, "UTF-8" );
        t.setOutputProperty( METHOD, "XML" );
        t.setOutputProperty( OMIT_XML_DECLARATION, "yes" );
        identityTransformer = t; }



    private final TextLineLocator lineLocator = new TextLineLocator(
      new IntArrayExtensor( new int[0x100] )); // = 256



    private final ImageMould<?> mould;



    private static final String nsHTML = "http://www.w3.org/1999/xhtml";



    private static final String nsImager = "data:,Breccia/Web/imager";



    private static final String nsXMLNS = "http://www.w3.org/2000/xmlns/";



    private final ImagingOptions opt;



    /** The original text content of the given node and its descendants prior to any transformation.
      */
    private String sourceText( final Node node ) { return node.getTextContent(); }
      // Should the transform ever introduce text of its own, then it must be marked as non-original,
      // e.g. by some attribute defined for that purpose.  The present method would then be modified
      // to remove all such text from the return value, e.g. by cloning `node`, filtering the clone,
      // then calling `getTextContent` on it.
      //     Non-original elements that merely wrap original content would neither be marked
      // nor removed, as their presence would have no effect on the return value.



    private final StringBuilder stringBuilder = new StringBuilder(
      /*initial capacity*/0x2000/*or 8192*/ );



    private final StringBuilder stringBuilder2 = new StringBuilder(
      /*initial capacity*/0x2000/*or 8192*/ );



    private final C sourceCursor;



    private final DOMResult toDOM = new DOMResult();



    private final StreamResult toImageFile = new StreamResult();



    protected void transform( final Document d ) {

      // HTML form
      // ─────────
        final Node fileFractum = d.removeChild( d.getFirstChild() ); // To be reintroduced
        assert "FileFractum".equals( fileFractum.getLocalName() );  // further below.
        if( d.hasChildNodes() ) throw new IllegalStateException(); // One alone was present.
        final Element html = d.createElementNS( nsHTML, "html" );
        d.appendChild( html );
        html.setAttributeNS( nsXMLNS, "xmlns:img", nsImager );
        html.setAttribute( "style", "--centre-column:" + Float.toString(opt.centreColumn()) + "ch" );

      // `head`
      // ┈┈┈┈┈┈
        final Element documentHead = d.createElementNS( nsHTML, "head" );
        html.appendChild( documentHead );
        Element e;
        for( Node n = successor(fileFractum);  n != null;  n = successor(n) ) {
            if( !"Head".equals( n.getLocalName() )) continue;
            final String tF = fileTitle( n );
            if( tF != null ) {
                documentHead.appendChild( e = d.createElementNS( nsHTML, "title" ));
                e.appendChild( d.createTextNode( tF ));
                break; }}
        documentHead.appendChild( e = d.createElementNS( nsHTML, "link" ));
        e.setAttribute( "rel", "stylesheet" );
        e.setAttribute( "href", opt.coServiceDirectory() + "Breccia/Web/imager/image.css" );

      // `body`
      // ┈┈┈┈┈┈
        final Element documentBody = d.createElementNS( nsHTML, "body" );
        html.appendChild( documentBody );
        documentBody.appendChild( fileFractum );


      // ════════════════
      // Division titling
      // ════════════════
        for( Element dL = successorElement(fileFractum);  dL != null;  dL = successorElement(dL) ) {
            if( !"DivisionLabel".equals( dL.getLocalName() )) continue;
            final String p = asText(dL.getPreviousSibling().getFirstChild()).getData();
              // All `dL` have a `Markup` predecessor comprising flat text.
            int c = p.length();
            do --c; while( p.charAt(c) == ' ' );    // Scan leftward past any plain space characters,
            if( completesNewline( p.charAt( c ))) { // and there test for the presence of a newline.
                assert "".equals( dL.getAttribute( "class" ));
                dL.setAttribute( "class", "titling" ); }}


      // ═════════════════
      // Free-form bullets
      // ═════════════════
        for( Element b = successorElement(fileFractum);  b != null;  b = successorElement(b) ) {
            if( !"Bullet".equals( b.getLocalName() )) continue;
            final int pointType = parseInt( parentElement(parentElement(b)).getAttribute( "typestamp" ));
            final String typeMark; switch( pointType ) {
                case Typestamp.alarmPoint  -> typeMark = "!!";
                case Typestamp.plainPoint  -> typeMark =  ""; // None.
                case Typestamp.taskPoint   -> typeMark =  "+";
                default -> { continue; }}; // No free-form content in bullets of this type.
            final String text;
            final int freeEnd; { // End boundary of free-form part, start of type-mark terminator.
                final Text t = (Text)b.getFirstChild();
                text = t.getData();
                assert text.endsWith( typeMark );
                freeEnd = text.length() - typeMark.length();
                if( freeEnd <= 0 ) continue; // No free-form content in bullet `b`.
                b.removeChild( t ); }

          // Free-form part
          // ──────────────
            final StringBuilder bP = clear( stringBuilder ); // Punctuation characters.
            final StringBuilder bQ = clear( stringBuilder2 ); // Other characters.
            for( int ch, c = 0; c < freeEnd; c += charCount(ch) ) {
                ch = text.codePointAt( c );
                if( isAlphabetic(ch) || isDigit(ch) || ch == ' ' || ch == '\u00A0'/*no-break space*/ ) {
                    appendAnyP( b, bP );
                    bQ.appendCodePoint( ch ); }
                else { // `ch` is punctuation
                    appendAnyQ( b, bQ );
                    bP.appendCodePoint( ch ); }}
            appendAnyP( b, bP );
            appendAnyQ( b, bQ );

          // Terminator, if any
          // ──────────
            if( typeMark.length() == 0 ) continue;
            final Element terminator = d.createElementNS( nsImager, "img:terminator" );
            b.appendChild( terminator );
            terminator.appendChild( d.createTextNode( typeMark )); }}



    private static final UnglyphedCharacter[] unArrayType = new UnglyphedCharacter[0];



    /** A comparator based on linear order of occurrence in the Breccian source file.
      */
    public static final Comparator<UnglyphedCharacter> unsComparator = new Comparator<>() {
        public @Override int compare( final UnglyphedCharacter c, final UnglyphedCharacter d ) {
            final CharacterPointer p = c.pointer;
            final CharacterPointer q = d.pointer;
            int result = Integer.compare( p.lineNumber, q.lineNumber );
            if( result == 0 ) result = Integer.compare( p.column, q.column );
            if( result == 0 ) result = Integer.compareUnsigned( c.codePoint, d.codePoint );
            return result; }};



    private final Map<Integer,UnglyphedCharacter> unsMap = new HashMap<>(); }
      // Code points (keys) each mapped to an unglyphed-character record (value).



// NOTE
// ────
//   DI  `DOCTYPE` inclusion.  The would-be alternative of using, at an earlier stage, the likes of
//       `d.appendChild( d.getImplementation().createDocumentType( "html", null, "about:legacy-compat" ))`
//        in order to give the initial DOM document (here `d`) a `DOCTYPE` turns out not to suffice
//        because it has no effect on the output.



                                                   // Copyright © 2020-2022  Michael Allan.  Licence MIT.
