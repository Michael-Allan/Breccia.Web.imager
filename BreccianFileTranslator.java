package Breccia.Web.imager;

import Breccia.parser.*;
import Breccia.XML.translator.BrecciaXCursor;
import Java.*;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import static Breccia.parser.AssociativeReference.ReferentClause;
import static Breccia.parser.Typestamp.empty;
import static Breccia.parser.plain.Language.impliesNewline;
import static Breccia.parser.plain.Language.completesNewline;
import static Breccia.parser.plain.Project.newSourceReader;
import static Breccia.Web.imager.ErrorAtFile.wrnHead;
import static Breccia.Web.imager.Project.imageSibling;
import static Breccia.Web.imager.Project.imageSimpleName;
import static Breccia.Web.imager.Project.logger;
import static Breccia.Web.imager.Project.looksBreccian;
import static Breccia.Web.imager.Project.zeroBased;
import static java.awt.Font.createFont;
import static java.awt.Font.TRUETYPE_FONT;
import static java.lang.Character.charCount;
import static java.lang.Character.isAlphabetic;
import static java.lang.Character.isDigit;
import static java.lang.Character.toLowerCase;
import static java.lang.Integer.parseInt;
import static java.lang.Integer.parseUnsignedInt;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.newBufferedReader;
import static java.nio.file.Files.newOutputStream;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static Java.Nodes.hasName;
import static Java.Nodes.isText;
import static Java.Nodes.parentAsElement;
import static Java.Nodes.parentElement;
import static Java.Nodes.successor;
import static Java.Nodes.successorAfter;
import static Java.Nodes.successorElement;
import static Java.Paths.toPath;
import static Java.StringBuilding.clear;
import static Java.StringBuilding.collapseWhitespace;
import static Java.URI_References.isRemote;
import static java.util.Arrays.sort;
import static javax.xml.transform.OutputKeys.*;


/** @param <C> The type of source cursor used by this translator.
  */
public class BreccianFileTranslator<C extends ReusableCursor> implements FileTranslator<C> {


    /** @see #sourceCursor()
      * @see #sourceXCursor
      */
    public BreccianFileTranslator( C sourceCursor, BrecciaXCursor sourceXCursor,
          final ImageMould<?> mould ) {
        this.sourceCursor = sourceCursor;
        this.sourceXCursor = sourceXCursor;
        this.mould = mould;
        opt = mould.opt;
        final String f = opt.glyphTestFont();
        if( !f.equals( "none" )) {
            try( final var in = new FileInputStream( f )) {
                glyphTestFont = createFont( TRUETYPE_FONT/*includes all of OpenType*/,
                  /*buffered by callee in JDK 17*/in); }
            catch( FontFormatException|IOException x ) { throw new Unhandled( x ); }}}



    /** The XML source cursor (Breccia to X-Breccia translator) to use during calls
      * to this file translator.  Between calls, it may be used for other purposes.
      */
    public final BrecciaXCursor sourceXCursor;



   // ━━━  F i l e   T r a n s l a t o r  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override void finish( Path sourceFile, final Path imageFile ) throws ErrorAtFile {
        try {

          // XHTML DOM ← XHTML image file
          // ─────────
            toDOM.setNode( null/*make a new `Document`*/ );
            try( final Reader imageReader = newBufferedReader​( imageFile )) {
                fromStream.setReader( imageReader );
             // identityTransformer.transform( fromStream, toDOM ); }
            ///  ↑ Transformation direct from an image file fails with ‘unknown protocol: about’. [UPA]
           ////  ↓ Transformation through an intermediate StAX parser does not.
                final XMLStreamReader imageParser = xmlInputFactory.createXMLStreamReader( fromStream );
                try { identityTransformer.transform( new StAXSource(imageParser), toDOM ); } // [SNR]
                finally { imageParser.close(); }}
            final Document d = (Document)(toDOM.getNode());

          // XHTML DOM ← XHTML DOM
          // ─────────
            final Element fileFractum = (Element)(
              d.getDocumentElement()./*body*/getLastChild().getFirstChild() );
            assert hasName( "FileFractum", fileFractum );
            finish( sourceFile, fileFractum );

          // XHTML image file ← XHTML DOM
          // ────────────────
            write( d, imageFile ); }
        catch( IOException|TransformerException|XMLStreamException x ) {
            throw new ErrorAtFile( imageFile, "Unable to finish image file", x ); }}



    public @Override Granum formalReferenceAt( final C in ) throws ParseError {
        final ResourceIndicant iR; {
            FractumIndicant iF; {
                final ReferentClause cR; {
                    final AssociativeReference rA; {
                        rA = in.asAssociativeReference();
                        if( rA == null ) return null; } /* Adding the return of a formal reference other
                          than an associative one?  Then sync with `finish(Path.Document)` below. */
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
              is the containing file, which is not an external resource as required by the method API. */
        if( iR.qualifiers().contains( "non-fractal" )) return null; /* Fractal alone implies formal,
          non-fractal implying a resource whose content is opaque to this translator and therefore
          indeterminate of image form. */
        return iR.reference(); } /* The resource of `iR` is formal ∵ the associative reference containing
          `iR` refers to a pattern of text *in* the resource and ∴ will be imaged as a hyperlink whose
          form depends on the content of that resource.  So it informs the image and ∴ is formal. */



    public final @Override C sourceCursor() { return sourceCursor; }



    public @Override void translate( final Path sourceFile, final Path imageDirectory )
          throws ParseError, ErrorAtFile {
        final Path imageFile = imageDirectory.resolve( imageSimpleName( sourceFile ));
        try {
            createDirectories( imageFile.getParent() ); // Ensure the parent exists.
            try( final Reader sourceReader = newSourceReader​( sourceFile )) {
                sourceCursor.source( sourceReader );
                if( sourceCursor.state().typestamp() == empty ) {
                    throw new ErrorAtFile( sourceFile, "Empty source file" ); } /* Explicitly Breccia
                      neither allows nor forbids an empty file.  Translating it to an empty image file
                      would produce malformed XML, however, as necessarily a well formed XML document
                      ‘contains one or more elements.’  https://www.w3.org/TR/xml/#sec-well-formed
                      Maintaining compliance with XML (and HTML) would therefore require
                      either complicating the code in order to deal with this edge case,
                      or (as here) rejecting the edge case. */

              // X-Breccia DOM ← X-Breccia parse events ← Breccia source file
              // ─────────────
                sourceXCursor.source( sourceCursor );
                toDOM.setNode( null/*make a new `Document`*/ );
                try { identityTransformer.transform( new StAXSource(sourceXCursor), toDOM ); }
                  // [SNR]
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
                    if( !isText( n )) continue;
                    final Text nText = (Text)n;
                    assert !nText.isElementContentWhitespace(); /* The `sourceXCursor` has produced
                      ‘X-Breccia with no ignorable whitespace’. */
                    final String text = nText.getData();
                    for( int ch, c = 0, cN = text.length(); c < cN; c += charCount(ch) ) {
                        ch = text.codePointAt( c );
                        if( glyphTestFont.canDisplay( ch )) continue;
                        UnglyphedCharacter un = unsMap.get( ch );
                        if( un == null ) {
                            un = new UnglyphedCharacter( glyphTestFont.getFontName(), ch,
                              characterPointer( parentAsElement(nText), c ));
                            unsMap.put( ch, un ); }
                        ++un.count; }}
                    while( (n = successor(n)) != null );
                if( !unsMap.isEmpty() ) {
                    final UnglyphedCharacter[] uns = unsMap.values().toArray( unArrayType );
                    sort( uns, unsComparator );
                    for( final var un: uns ) mould.warn( sourceFile, un.pointer, un.toString() ); }}

          // XHTML DOM ← X-Breccia DOM
          // ─────────
            translate( d );

          // XHTML image file ← XHTML DOM
          // ────────────────
            write( d, imageFile, CREATE_NEW ); }
        catch( IOException|TransformerException x ) {
            throw new ErrorAtFile( imageFile, "Unable to make image file", x ); }}



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



    private CharacterPointer characterPointer( Element granum ) { return characterPointer( granum, 0 ); }



    /** @param granum A granal element other than `FileFractum`.
      * @param c The offset in `granum` context of the character to point to.
      */
    private CharacterPointer characterPointer( final Element granum, final int c ) {
        final String textRegional;
        final IntArrayExtensor endsRegional = lineLocator.endsRegional;
        final int offsetRegional;
        final int numberRegional; {
            final Element fH = contextHead( granum );
            assert fH != null; // Caller obeys the API.
            textRegional = sourceText( fH );
            endsRegional.clear();
            final StringTokenizer tt = new StringTokenizer( fH.getAttribute( "xuncLineEnds" ));
            while( tt.hasMoreTokens() ) endsRegional.add( parseUnsignedInt( tt.nextToken() ));
            final Element f = parentAsElement( fH );
            offsetRegional = parseUnsignedInt( f.getAttribute( "xunc" ));
            numberRegional = parseUnsignedInt( f.getAttribute( "lineNumber" )); }

      // Locate the line
      // ───────────────
        int offset = c + parseUnsignedInt( granum.getAttribute( "xunc" )); // `granum` → whole text
        lineLocator.locateLine( offset, offsetRegional, numberRegional );

      // Resolve its content
      // ───────────────────
        final int lineStart = lineLocator.start() - offsetRegional; // whole text → `textRegional`
        final String line = textRegional.substring( lineStart,
          endsRegional.array[lineLocator.index()] - offsetRegional ); // whole text → `textRegional`

      // Form the pointer
      // ────────────────
        offset -= offsetRegional; // whole text → `textRegional`
        final int column = mould.gcc.clusterCount( textRegional, lineStart, offset );
        return new CharacterPointer( line, column, lineLocator.number() ); }



    /** Returns the fractal context of `e`, or null if there is none.
      *
      *     @return The same `e` if it is a fractum, otherwise `ownerFractum(e)`.
      *     @see #ownerFractum(Node)
      */
    private static Element contextFractum( final Element e ) {
        return isFractum(e) ? e : ownerFractum(e); }



    /** Returns the fractal head context of `e`, or null if there is none.
      *
      *     @return The same `e` if it is a fractal head, otherwise `ownerHead(e)`.
      *     @see #ownerHead(Node)
      */
    private static Element contextHead( final Element e ) {
        return hasName("Head",e) ? e : ownerHead(e); }



    /** @param head A `Head` element representing a fractal head.
      * @return The file title as derived from the head, or null if it yields none.
      */
    private String fileTitle( Element head ) {
        final String titlingExtract; // The relevant text extracted from the fractal head.
        if( hasName( "Division", head.getParentNode() )) { // Then `head` is a divider.
            for( Element e = successorElement(head);;  e = successorElement(e) ) {
                if( e == null ) return null;
                if( hasName( "DivisionLabel", e )) {
                    titlingExtract = sourceText( e );
                    break; }}}
        else { // Presumeably `head` is a file head or point head.
            head = (Element)head.cloneNode( /*deeply*/true ); /* So both preserving the original,
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
            titlingExtract = sourceText( head ); }
        final StringBuilder b = clear(stringBuilder).append( titlingExtract );
        collapseWhitespace( b );
        return b.isEmpty() ? null : b.toString(); }



    /** @param sourceFile The source of the file fractum.
      * @param fileFractum The unfinished image of the file fractum.
      */
    protected void finish( final Path sourceFile, final Element fileFractum ) {
        final Document d = fileFractum.getOwnerDocument();

      // URI references each formed as a hyperlink
      // ──────────────
        for( Element e = successorElement(fileFractum);  e != null;  e = successorElement(e) ) {
            if( !hasName( "Reference", e )) continue;
            assert hasName( "AssociativeReference", ownerFractum(e) ); /* Adding a hyperlink to
              other than an associative reference?  Then sync with `formalReferenceAt` above. */
            final Element eRef = e; // The reference encapsulated as an `Element`.
            final Text tRef = (Text)eRef.getFirstChild(); // The reference encapsulated as `Text`.
            final String hRef; { /*
                For what follows, cf. `ImageMould.formalResources_recordFrom`. */
                final String sRefOriginal = tRef.getData(); // The reference in string form.
                final String sRef = mould.translate( sRefOriginal, sourceFile );
                  // Applying any `--reference-mapping` translations.
                final boolean isAlteredRef = !sRef.equals( sRefOriginal );
                hRef = hRef( sourceFile, eRef, sRef, isAlteredRef );
                if( hRef == null ) { // Then `sRef` is not to be hyperlinked.
                    if( isAlteredRef ) hRef( sourceFile, eRef, sRefOriginal, /*isAlteredRef*/false ); /*
                      Falling back to `sRefOriginal`; so verifying that at least *it* would have
                      been hyperlinked, else warning the user. */
                    continue; }}
            final Element a = d.createElementNS( nsHTML, "html:a" );
            eRef.insertBefore( a, tRef );
            a.setAttribute( "href", hRef );
            a.appendChild( tRef ); }}



    private final DOMSource fromDOM = new DOMSource();



    private final StreamSource fromStream = new StreamSource();



    private Font glyphTestFont;



    /** @param f The path of a source file.
      * @param eRef The unfinished image from `f` of a URI reference.
      * @param sRef The reference itself in string form,
      *   after any applicable `--reference-mapping` translations.
      * @param isAlteredRef Whether `sRef` was actually changed by such translation.
      * @return The target reference for the hyperlink `a` element, or null to omit hyperlinking.
      * @see ImageMould#translate(String,Path)
      */
    private String hRef( final Path f, final Element eRef, final String sRef,
          final boolean isAlteredRef ) { /* For what follows,
        cf. the comparably structured code of `ImageMould.formalResources_record`. [RC] */

        final URI uRef; { // The reference in parsed `URI` form.
            try { uRef = new URI( sRef ); }
            catch( final URISyntaxException x ) {
                final int c = isAlteredRef ? 0/*guaranteed within bounds of the unaltered `eRef`*/
                  : zeroBased( x.getIndex() );
                final CharacterPointer p = characterPointer( eRef, c );
                mould.warnOnce( f, p, mould.message( sRef, x, p, isAlteredRef ));
                return null; }} // Without a hyperlink ∵ `x` leaves the intended referent unclear.

      // remote  [RC]
      // ┈┈┈┈┈┈
        if( isRemote( uRef )) { // Then the referent would be reachable through a network.
            return looksBreccian(sRef) ? imageSibling(sRef) : sRef; } // TEST

      // local  [RC]
      // ┈┈┈┈┈
        else { /* The referent would be reachable through a file system, the reference
              being an absolute-path reference or relative-path reference [RR]. */
            final Path pRef; { // The reference parsed and resolved as a local file path.
                try { pRef = f.resolveSibling( toPath( uRef, f )); }
                catch( final IllegalArgumentException x ) {
                    final CharacterPointer p = characterPointer( eRef );
                    mould.warnOnce( f, p, x.getMessage() + '\n'
                      + mould.markedLine( sRef, p, isAlteredRef ));
                    return null; }} // Without a hyperlink ∵ `x` leaves the intended referent unclear.
            if( exists( pRef )) {
                if( !isDirectory(pRef) && looksBreccian(sRef) ) {
                    final boolean sRefImageExists; { /* Whether this (Breccian) referent
                          has an image file either (a) pre-existing or (b) newly formed. */
                        final Path pRefImageSib = imageSibling( pRef );
                        sRefImageExists = /*(a)*/isRegularFile( pRefImageSib )
                          || /*(b)*/pRef.startsWith( mould.boundaryPathDirectory )
                               && isRegularFile( mould.outDirectory.resolve(
                                    mould.boundaryPathDirectory.relativize( pRefImageSib ))); }
                    return sRefImageExists ? imageSibling(sRef) : sRef; }
                else return sRef; }
            else {
                final StringBuilder bMessage = clear( stringBuilder );
                final boolean isTransX = mould.isTransX( pRef, bMessage );
                final boolean wouldPrivatizationSuppress = isAlteredRef && isTransX;
                final CharacterPointer p = characterPointer( eRef );
                final String markedLine = mould.markedLine( sRef, p, isAlteredRef );
                if( wouldPrivatizationSuppress && isPrivatized(contextFractum(eRef)) ) {
                    logger.info( () -> wrnHead(f,p.lineNumber) + bMessage
                      + ": Omitting a hyperlink for this private reference:\n" + markedLine );
                    return null; } /* With neither hyperlink nor warning, because this type
                      of inaccessibility is common when a private reference is altered
                      by a `--reference-mapping` translation. */
                if( wouldPrivatizationSuppress ) {
                    bMessage.append( "; consider marking this reference as private" ); }
                bMessage.append( ":\n" ).append( markedLine );
                mould.warnOnce( f, p, bMessage.toString() ); /* Yet carry on and form the hyperlink,
                  for the cause of inaccessibility could be a misplacement or misconfiguration
                  of the referent as opposed to a malformation of the reference. */
                return sRef; }}}



    private final Transformer identityTransformer; {
        Transformer t;
        try { t = TransformerFactory.newInstance().newTransformer(); }
        catch( TransformerConfigurationException x ) { throw new Unhandled( x ); }
        t.setOutputProperty( DOCTYPE_SYSTEM, systemID_HTML ); /* A DTD is mandatory. [DTR]
          A system identifier for the DTD is not mandatory.  One is given here only as a workaround in
          order to make `identityTransformer` generate the DTD.  It fails to do so unless an identifier
          of some kind (system or public) is given.
              The would-be alternative of inserting a DTD into the DOM before file output, as with
          `Document.appendChild( Document.getImplementation().createDocumentType( "html", null, null ))`,
          fails without effect. */
        t.setOutputProperty( ENCODING, "UTF-8" );
        t.setOutputProperty( METHOD, "XML" );
        t.setOutputProperty( OMIT_XML_DECLARATION, "yes" );
        identityTransformer = t; }



    private final Map<String,Integer> idMap = new HashMap<>();
      // Fractum base identifiers (keys) each mapped to the count of occurences (value).
      // Base identifiers omit any ordinal suffix.



    private static boolean isFractum( final Element e ) { return e.hasAttribute( "typestamp" ); }



    /** Returns true if `fractum` is marked as private (whether directly or indirectly)
      * by the use of a privatizer; false otherwise.
      *
      *     @see Cursor#isPrivatized(int[])
      */
    private static boolean isPrivatized( Element fractum ) {
        assert isFractum( fractum ); // Else the call is needlessly slower.
        if( isPrivatizedDirectly( fractum )) return true;
        if( hasName( "FileFractum", fractum )) return false; // End of ancestral line.
        return isPrivatized( parentAsElement( fractum )); }



    private static boolean isPrivatizedDirectly( Element fractum ) {
        if( fractum.getAttribute("modifiers").contains( "privately" )) return true;
        for( Node child = fractum.getFirstChild(); child != null; child = child.getNextSibling() ) {
            if( hasName( "Privatizer", child )) return true; }
        return false; }



    /** @param token A word or other sequence of characters extracted from a fractal head.
      * @return The token transformed as necessary to serve as a keyword in a fractum `id` attribute.
      */
    private String keyword( final String token ) {
        final StringBuilder b = clear( stringBuilder );
        boolean wasLastMasked = false;
        int c = 0;
        for( final int cN = token.length(); c < cN; ++c ) {
            final char ch = token.charAt( c );
            if( 'a' <= ch && ch <= 'z'  ||  'A' <= ch && ch <= 'Z'  ||  '0' <= ch && ch <= '9' ) {
                b.append( ch );
                wasLastMasked = false; }
            else if( wasLastMasked ) continue; // Omit, so collapsing to a single mask character.
            else {
                b.append( '-' ); // Masking it for sake of pretty URLs, uncomplicated by encoding.
                wasLastMasked = true; }}
        c = 0; // Trim any mask characters at the leading or trailing edges. [MT]
        if( b.length() > 1  &&  b.charAt(c) == '-' ) b.deleteCharAt( c );
        c = b.length() - 1;
        if( b.length() > 1  &&  b.charAt(c) == '-' ) b.deleteCharAt( c );
        final char ch = b.charAt( 0 );
        if( 'A' <= ch && ch <= 'Z' ) b.setCharAt( 0, toLowerCase(ch) ); /* Lower-casing the first letter
          for sake of ID stability, as the keyword might lead a sentence now, then move under editing. */
        return b.toString(); }



    private final TextLineLocator lineLocator = new TextLineLocator(
      new IntArrayExtensor( new int[0x100] )); // = 256



    private final ImageMould<?> mould;



    private static final String nsHTML = "http://www.w3.org/1999/xhtml";



    private static final String nsImager = "data:,Breccia/Web/imager";



    private static final String nsXMLNS = "http://www.w3.org/2000/xmlns/";



    private final ImagingOptions opt;



    /** Returns the nearest fractal ancestor of `node`, or null if there is none.
      *
      *     @return The nearest ancestor of `node` that is a fractum, or null if there is none.
      *     @see #contextFractum(Element)
      */
    private static Element ownerFractum( final Node node ) {
        Element a = parentElement( node );
        while( a != null && !isFractum(a) ) a = parentElement( a );
        return a; }



    /** Returns the nearest ancestor of `node` that is a fractal head, or null if there is none.
      *
      *     @see #contextHead(Node)
      */
    private static Element ownerHead( Node node ) {
        do node = node.getParentNode(); while( node != null && !hasName("Head",node) );
        return (Element)node; }



    /** The original text content of the given node and its descendants prior to any translation.
      */
    private String sourceText( final Node node ) { return node.getTextContent(); }
      // Should the translation ever introduce text of its own, then it must be marked as non-original,
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



    private static final String systemID_HTML = "about:legacy-compat";
      // https://html.spec.whatwg.org/multipage/syntax.html#the-doctype



    private final DOMResult toDOM = new DOMResult();



    private final StreamResult toImageFile = new StreamResult();



    protected void translate( final Document d ) {

      // HTML form
      // ─────────
        final Element fileFractum = (Element)d.removeChild( d.getFirstChild() ); // To be reintroduced
        assert hasName( "FileFractum", fileFractum );                           // further below.
        if( d.hasChildNodes() ) throw new IllegalStateException();             // One alone was present.
        final Element html = d.createElementNS( nsHTML, "html" );
        d.appendChild( html );
        html.setAttributeNS( nsXMLNS, "xmlns:img", nsImager );
        html.setAttribute( "style", "--centre-column:" + Float.toString(opt.centreColumn()) + "ch" ); {
            Element e;

          // `head`
          // ┈┈┈┈┈┈
            final Element documentHead = d.createElementNS( nsHTML, "head" );
            html.appendChild( documentHead );
            String fileTitle = null; // Unless one can be derived from the text:
            for( e = successorElement(fileFractum);  e != null;  e = successorElement(e) ) {
                if( !hasName( "Head", e )) continue;
                if( (fileTitle = fileTitle(e)) != null ) break; }
            documentHead.appendChild( e = d.createElementNS( nsHTML, "title" ));
            e.appendChild( d.createTextNode( fileTitle == null ? "Untitled" : fileTitle )); /* A title
              *is* mandatory.  https://html.spec.whatwg.org/multipage/semantics.html#the-head-element */
            documentHead.appendChild( e = d.createElementNS( nsHTML, "link" ));
            e.setAttribute( "rel", "stylesheet" );
            e.setAttribute( "href", opt.coServiceDirectory() + "Breccia/Web/imager/image.css" );

          // `body`
          // ┈┈┈┈┈┈
            final Element documentBody = d.createElementNS( nsHTML, "body" );
            html.appendChild( documentBody );
            documentBody.appendChild( fileFractum );
            fileFractum.setAttributeNS( nsXMLNS, "xmlns:html", nsHTML );
            documentBody.appendChild( e = d.createElementNS( nsHTML, "script" ));
            e.setAttribute( "src", opt.coServiceDirectory() + "Breccia/Web/imager/image.js" ); }


      // ════════════════
      // Division titling
      // ════════════════
        for( Element dL = successorElement(fileFractum);  dL != null;  dL = successorElement(dL) ) {
            if( !hasName( "DivisionLabel", dL )) continue;
            final String p = ((Text)dL.getPreviousSibling().getFirstChild()).getData();
              // All `dL` have a `Granum` predecessor comprising flat text.
            int c = p.length();
            do --c; while( p.charAt(c) == ' ' );    // Scan leftward past any plain space characters,
            if( completesNewline( p.charAt( c ))) { // and there test for the presence of a newline.
                assert "".equals( dL.getAttribute( "class" ));
                dL.setAttribute( "class", "titling" ); }}


      // ═════════════════
      // Free-form bullets  [BF↓]
      // ═════════════════
        for( Element b = successorElement(fileFractum);  b != null;  b = successorElement(b) ) {
            if( !hasName( "Bullet", b )) continue;
            final int pointType = parseInt(
              parentAsElement(parentAsElement(b)).getAttribute( "typestamp" ));
            final String typeMark; switch( pointType ) {
                case Typestamp.alarmPoint  -> typeMark = "!!";
                case Typestamp.plainPoint  -> typeMark =  ""; // None.
                case Typestamp.taskPoint   -> typeMark =  "+";
                default -> { continue; }}; // No free-form content in bullets of this type.
            final String text;
            final int freeEnd; { // End boundary of free-form part, start of type-mark terminator.
                final Text t = (Text)b.getFirstChild(); /* This must run before the *Body fracta* code,
                  which might here insert an `a` element and split the text.  [BF↓] */
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
            terminator.appendChild( d.createTextNode( typeMark )); }


      // ═══════════
      // Body fracta  [BF]
      // ═══════════
        idMap.clear();
        for( Element bF = successorElement(fileFractum);  bF != null;  bF = successorElement(bF) ) {
            if( !isFractum( bF )) continue;

          // Identification by `id` attribution
          // ──────────────────────────────────
            final int kMax = 3; // Maximum number of keywords to include in the identifier.
            final ArrayList<String> keywords = new ArrayList<>( /*initial capacity*/kMax );

          // gather the longest keywords from the fractal head
          // ┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈
            final Element head = (Element)bF.getFirstChild();
            skim: {
                final StringTokenizer tt = new StringTokenizer( sourceText(head), " \n\r\u00A0" );
                  // Parsing into tokens the text of the fractal head broken on Breccian whitespace.
                do { // Fill `keywords` with the first tokens in linear order.
                    if( !tt.hasMoreTokens() ) break skim;
                    keywords.add( keyword( tt.nextToken() )); }
                    while( keywords.size() < kMax );
                boolean keywordsHaveChanged = true;
                int shortest = -1, shortestLength = -1; // Index and length of the shortest keyword.
                while( tt.hasMoreTokens() ) { // Parse the remainder, ensuring the longest are chosen.
                    if( keywordsHaveChanged ) { // Then find the `shortest`.
                        int k = kMax - 1;
                        shortest = k;
                        shortestLength = keywords.get(k).length();
                        do {
                            --k;
                            final int kLength = keywords.get(k).length();
                            if( kLength < shortestLength ) {
                                shortestLength = kLength;
                                shortest = k; }}
                            while( k > 0 ); }
                    final String keyword = keyword( tt.nextToken() );
                    if( keyword.length() > shortestLength ) {
                        keywords.remove( shortest );
                        keywords.add( keyword );
                        keywordsHaveChanged = true; }}}

          // compose the identifier from the keywords
          // ┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈
            final String id; {
                final StringBuilder ib = clear( stringBuilder );
                for( int k = 0;; ) {
                    final StringBuilder kb = clear(stringBuilder2).append( keywords.get( k ));
                    if( kb.length() > 12 ) kb.setLength( 12 ); // Putting a limit on keyword length.
                    ib.append( kb.toString() );
                    if( ++k == keywords.size() ) break;
                    ib.append( /*keyword separator*/',' ); }
                idMap.compute( /*base identifier*/ib.toString(), (ib_, count) -> {
                    if( count != null ) {
                        ++count;
                        ib.append( ':' ).append( count ); } // Appending to the base an ordinal suffix.
                    else count = 1;
                    return count; });
                bF.setAttribute( "id", id = ib.toString() ); }

          // Self hyperlink
          // ──────────────
            for( Node n = successor(head);  n != null;  n = successor(n) ) {
                if( !isText( n )) continue;
                final Text nText = (Text)n;
                final String text = nText.getData();
                final int textLength = text.length();
                if( textLength == 0 ) continue;
                final int hyperlinkLength; {
                    if( hasName( "PerfectIndent", nText.getParentNode() )) {
                        assert textLength >= 4;
                        hyperlinkLength = textLength - 1; } // All but the final character of the indent.
                    else hyperlinkLength = textLength > 1 && !impliesNewline(text.charAt(1)) ? 2 : 1; }
                      // Taking if possible two characters in order to ease clicking.
                final Text nTextRemainder = nText.splitText( hyperlinkLength );
                final Element a = d.createElementNS( nsHTML, "html:a" );
                nText.getParentNode().insertBefore( a, nTextRemainder );
                a.setAttribute( "class", "self" );
                a.setAttribute( "href", '#' + id );
                a.setAttribute( "onclick",
                  "Breccia_Web_imager.fractumSelfHyperlink_hearClick( event )" );
                a.appendChild( nText );
                break; }}}



    private static final UnglyphedCharacter[] unArrayType = new UnglyphedCharacter[0];



    /** A comparator based on linear order of occurrence in the Breccian source file.
      */
    private static final Comparator<UnglyphedCharacter> unsComparator = new Comparator<>() {
        public @Override int compare( final UnglyphedCharacter c, final UnglyphedCharacter d ) {
            final CharacterPointer p = c.pointer;
            final CharacterPointer q = d.pointer;
            int result = Integer.compare( p.lineNumber, q.lineNumber );
            if( result == 0 ) result = Integer.compare( p.column, q.column );
            if( result == 0 ) result = Integer.compareUnsigned( c.codePoint, d.codePoint );
            return result; }};



    private final Map<Integer,UnglyphedCharacter> unsMap = new HashMap<>();
      // Code points (keys) each mapped to an unglyphed-character record (value).



    private void write( final Document document, final Path imageFile,
          final OpenOption... outputOptions ) throws IOException, TransformerException {
        fromDOM.setNode( document );
        try( final OutputStream imageWriter = newOutputStream​( imageFile, outputOptions )) {
            toImageFile.setOutputStream( imageWriter );
            identityTransformer.transform( fromDOM, toImageFile ); }}



    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    static {  /* ↖ Re `static`: source code (`javax.xml.streamFactoryFinder`, JDK 18)
          suggests that `XMLInputFactory` is thread safe. */
        xmlInputFactory.setProperty( "javax.xml.stream.isCoalescing", true );
          // Consistent with the other input sources here relied on, such as `BrecciaXCursor`.
        xmlInputFactory.setProperty( "javax.xml.stream.isSupportingExternalEntities", false );
        xmlInputFactory.setProperty( "javax.xml.stream.supportDTD", false ); }}
          // While a DTD is present in each image file (a requirement of HTML) it is empty. [DTR]



// NOTES
// ─────
//   BF↓  Code that must execute before section *Body fracta*`.
//
//   BF · Section *Body fracta* itself, or code that must execute in unison with it.
//
//   DTR  ‘A `DOCTYPE` is a required preamble’ in HTML.
//        https://html.spec.whatwg.org/multipage/syntax.html#the-doctype
//
//   MT · Mask trimming for ID stability.  The purpose is to omit any punctuation marks such as quote
//        characters, commas or periods that might destabilize the ID as the source text is edited.
//
//   RC · Cf. the comparably structured code of `ImageMould.formalResources_record`.
//
//   RR · Relative reference.  https://www.rfc-editor.org/rfc/rfc3986#section-4.2
//
//   SNR  `StAXSource` is ‘not reusable’ according to its API.  This is puzzling, however,
//        given that it’s a pure wrapper.
//
//   UPA  `javax.xml.transform.TransformerException: MalformedURLException: unknown protocol: about`.
//        Thrown by `identityTransformer` when it reads the workaround system identifier `systemID_HTML`
//        present in the DTD of each image file. (JDK 18)
//            Attempting to override that identifier via `StreamSource.setSystemId` fails without effect.



                                                   // Copyright © 2020-2022  Michael Allan.  Licence MIT.
