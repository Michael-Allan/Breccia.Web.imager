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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
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
import static Breccia.Web.imager.Project.logger;
import static Breccia.Web.imager.Project.looksBrecciaLike;
import static Breccia.Web.imager.Project.looksImageLike;
import static Breccia.Web.imager.Project.sourceSibling;
import static Breccia.Web.imager.Project.zeroBased;
import static java.awt.Font.createFont;
import static java.awt.Font.TRUETYPE_FONT;
import static Java.Hashing.initialCapacity;
import static Java.IntralineCharacterPointer.markedLine;
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
import static Java.Nodes.isElement;
import static Java.Nodes.isText;
import static Java.Nodes.parentAsElement;
import static Java.Nodes.parentElement;
import static Java.Nodes.successor;
import static Java.Nodes.successorAfter;
import static Java.Nodes.successorElement;
import static Java.Nodes.successorElementAfter;
import static Java.Paths.toPath;
import static Java.Paths.to_URI_relativeReference;
import static Java.StringBuilding.clear;
import static Java.StringBuilding.collapseWhitespace;
import static Java.URI_References.isRemote;
import static java.util.Arrays.sort;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.MULTILINE;
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


    public @Override void finish( Path sourceFile, final Path imageFile ) throws ErrorAtFile { // [F]
        final Element fileFractum = fileFractum( imageFile );
        finish( sourceFile, fileFractum );
        try { write( fileFractum.getOwnerDocument(), imageFile ); }
        catch( IOException|TransformerException x ) {
            throw new ErrorAtFile( imageFile, "Unable to write image file", x ); }}



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
                    if( iF.patternMatchers() == null ) return null; } /* The pattern matchers
                     of a fractum indicant alone make it formal (not the resource reference),
                     because alone their hyperlink forms depend on the content of the resource. */
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
        final Path imageFile = imageDirectory.resolve( imageSibling( sourceFile.getFileName() ));
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
            translate( sourceFile, d );

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



    /** @param granum A granal element other than `FileFractum`.
      */
    protected final CharacterPointer characterPointer( Element granum ) {
        return characterPointer( granum, 0 ); }



    /** @param granum A granal element other than a fractum.
      * @param c The offset in `granum` context of the character to point to.
      */
    protected final CharacterPointer characterPointer( final Element granum, final int c ) {
        final String textRegional;
        final IntArrayExtensor endsRegional = lineLocator.endsRegional;
        final int offsetRegional;
        final int numberRegional; {
            final Element fH = ownerHeadOrSelf( granum );
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



    /** @param imageFile The absolute path of an image file.
      * @return The image of that path’s file fractum.
      */
    private Element fileFractum( final Path imageFile ) throws ErrorAtFile {
        toDOM.setNode( null/*make a new `Document`*/ );
        try( final Reader fileReader = newBufferedReader​( imageFile )) {
            fromStream.setReader( fileReader );
         // identityTransformer.transform( fromStream, toDOM ); }
        ///  ↑ Transformation direct from an image file fails with ‘unknown protocol: about’. [UPA]
       ////  ↓ Transformation through an intermediate StAX parser does not.
            final XMLStreamReader imageReader = xmlInputFactory.createXMLStreamReader( fromStream );
            try { identityTransformer.transform( new StAXSource(imageReader), toDOM ); } // [SNR]
            finally { imageReader.close(); }}
        catch( IOException|TransformerException|XMLStreamException x ) {
            throw new ErrorAtFile( imageFile, "Unable to read image file", x ); }
        final Document d = (Document)(toDOM.getNode());
        final Element e = (Element)( d.getDocumentElement()./*body*/getLastChild().getFirstChild() );
        assert hasName( "FileFractum", e );
        return e; }



    private static final String fileFractumIdentifier = "file-fractum"; // Its `id` attribute.



    /** @param head A `Head` element representing a fractal head.
      * @return The file title as derived from the head, or null if it yields none. *//*
      * @paramImplied #stringBuilder
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
              and keeping the nodal scan that follows within the bounds of the isolated clone. */
            strip: for( Node p, n = successor(p = head);  n != null;  n = successor(p = n) ) {
                final String name = n.getLocalName();
                if( "IndentBlind".equals( name )) for( ;; ) { // Remove indent blind `n`
                    final Node s = successorAfter( n );      // and all successors.
                    n.getParentNode().removeChild( n );
                    if( s == null ) break strip;
                    n = s; }
                if( "CommentAppender".equals( name )
                 || "CommentBlock"   .equals( name )) { // Remove comment carrier `n`.
                    final Node c = n;
                    c.getParentNode().removeChild( c );
                    n = p; }} // Resuming from the predecessor of comment carrier `n`, now removed.
            titlingExtract = sourceText( head ); }
        final StringBuilder b = clear(stringBuilder).append( titlingExtract );
        collapseWhitespace( b );
        return b.isEmpty() ? null : b.toString(); }



    /** @return The last fractal head within the given fractum.
      * @see #head(Element)
      */
    private static Element finalHead( final Element fractum ) {
        Element h = null; {
            Node n = fractum.getLastChild();
            do if( hasName( "Head", n )) h = (Element)n;
                while( (n = n.getLastChild()) != null ); }
        return h; }



    /** @param sourceFile The absolute path of a source file.
      * @param fileFractum The unfinished image of its file fractum.
      */
    protected void finish( final Path sourceFile, final Element fileFractum ) {
        final Document d = fileFractum.getOwnerDocument();

      // ══════════════
      // URI references each formed as a hyperlink  [F, HF, ◦↓◦]
      // ══════════════
        for( Element e = successorElement(fileFractum);  e != null;  e = successorElement(e) ) {
            if( !hasName( "Reference", e )) continue;
            final Element eRef = e; // The reference encapsulated as an `Element`.
            final Text tRef = (Text)eRef.getFirstChild(); // The reference encapsulated as `Text`.
            final String hRef; { /*
                For what follows, cf. `ImageMould.formalResources_recordFrom`. */
                final String sRefOriginal = tRef.getData(); // The reference in string form.
                final String sRef = mould.translate( sRefOriginal, sourceFile );
                  // Applying any `-reference-mapping` translations.
                final boolean isAlteredRef = !sRef.equals( sRefOriginal );
                if( isAlteredRef ) {
                    final String test = hRef( sourceFile, eRef, sRefOriginal, /*isAlteredRef*/false ); /*
                      Always testing `sRefOriginal`, so verifying that it would have been hyperlinked,
                      else warning the user.  For there are types of warning that are issued only
                      against `sRefOriginal`, which might otherwise be lost.  See for instance,
                      `wayic.Web.imager.WaybreccianFileTranslator.hRefLocal` and `hRefRemote`. */
                    if( test == null ) continue; } /* Let the user correct the source first,
                      as this would likely reduce noise on the console. */
                hRef = hRef( sourceFile, eRef, sRef, isAlteredRef );
                if( hRef == null ) continue; } // For then `sRef` is not to be hyperlinked.
            final Element a = d.createElementNS( nsHTML, "html:a" );
            eRef.insertBefore( a, tRef );
            a.setAttribute( "href", hRef ); // [◦↓◦]
            a.appendChild( tRef ); }


      // ══════════════════
      // Patterns of fracta within fractum indicants, each formed as a hyperlink  [◦↑◦]
      // ══════════════════
        iF: for( Element iF = successorElement(fileFractum);  iF != null;  iF = successorElement(iF) ) {
            if( !hasName( "FractumIndicant", iF )) continue;
            if( !hasName( "PatternMatcher", iF.getFirstChild() )
             && !hasName( "InferentialReferentIndicant", iF.getParentNode().getParentNode() )) {
                continue; } // No patterns (explicit or implicit) to hyperlink.
            assert hasName( "AssociativeReference", ownerFractum(iF) ); /* Hyperlinking a formal
              reference other than an associative one?  Then sync with `formalReferenceAt` above. */

          // Referent file
          // ─────────────
            final ImageFile iRef;
            final int rSelf; // Index in `iRef.fracta` of `iF` owner, as per `seek(m,fracta,fSelf)`.
            final String hRef_filePart; // The pre-fragment part of each hyperlink’s `href` attribute.
            Node n;
            Node iFc; { // Initialized herein to the last child of `iF` before any resource indicant:
                iFc = iF.getLastChild();
                if( hasName( "ResourceIndicant", iFc )) {
                    if( ((Element)iFc).getAttribute("qualifiers").contains( "non-fractal" )) continue;
                       // No patterns of *fracta* to hyperlink.
                    n = iFc.getLastChild();
                    assert hasName( "Reference", n );
                    n = n.getFirstChild();
                    if( !hasName( "a", n )) continue; /* No hyperlink having been formed
                      for the referent file, none will be formed for its fracta. */
                    final String hRef = ((Element)n).getAttribute( "href" ); // [◦↑◦]
                    final URI uRef; {
                        try { uRef = new URI( hRef ); }
                        catch( URISyntaxException x ) { throw new Unhandled( x ); }}
                          // Unexpected because this is effectively a reconstruction.
                    if( !looksImageLike( uRef )) continue; /* The hyperlink for the referent file
                      does not target its Web image, which means either that the referent file is
                      non-Breccian, or it had no corresponding image file earlier, when one was sought.
                      Without an image file, there can be no `iRef` against which to resolve the patterns
                      of `iF`, nor any way to form hyperlinks to the matching fracta. */
                    if( isRemote( uRef )) {
                        continue; // No HTTP access, no `iRef`. [NH]
                     /* hRef_filePart = unfragmented( uRef ).toASCIIString(); /* To be correct,
                          though no fragment is expected on Breccian referent `uRef`. */ }
                    else {
                        final Path referentPath = // Absolute path of the referent image file,
                          sourceFile.resolveSibling( toPath( uRef, sourceFile )); /*
                            No `IllegalArgumentException` expected, ∵ a reference so malformed
                            would not have been hyperlinked. [◦↑◦] */
                        iRef = recorded( referentPath.normalize() );
                        if( iRef == null ) continue;
                        rSelf = -2;
                        hRef_filePart = hRef; } // Already without a fragment, given `toPath` above.
                    iFc = iFc.getPreviousSibling(); }
                else { // The referent file is the containing file, the present image file.
                    iRef = recorded( imageSibling(sourceFile).normalize() );
                    assert iRef != null; // It was formed earlier, during the `translate` cycle.
                    rSelf = seek( parseUnsignedInt( ownerFractum(iF).getAttribute( "xunc" )),
                      iRef.fracta() );
                    hRef_filePart = ""; }}

          // Referring patterns taken from right to left in the pattern series
          // ──────────────────
            int region = 0, regionEnd = iRef.sourceText().length(); // Search region in referent source.
            for(; iFc != null; iFc = iFc.getPreviousSibling() ) { // Leftward through `iF` children.
                if( !hasName( "PatternMatcher", iFc )) continue;
                final Element eP = (Element)iFc.getFirstChild().getNextSibling(); /* The image
                  of a Breccian regular-expression pattern from a pattern matcher. */
                assert hasName( "Pattern", eP );
                final Pattern jP; { /* The Java translation of `eP` compiled with
                      its associated match modifiers. */
                    n = iFc.getLastChild();
                    final String mm = hasName("MatchModifiers",n) ? textChildFlat(n) : "";
                    try { jP = pattern( eP, mm, MULTILINE ); } // Re MULTILINE: these patterns must be
                    catch( final PatternSyntaxException x ) { // matched in ‘multiple-line mode’. [RFI]
                        final CharacterPointer p = characterPointer( eP );
                        mould.warn( sourceFile, p, "Malformed pattern: " + x.getDescription() + '\n'
                          + markedLine( "    ", x.getPattern(), zeroBased(x.getIndex()), mould.gcc )
                          + "\n    Source line, with original pattern:  (before translation to Java)\n"
                          + p.markedLine() );
                        continue iF; }}
                final String hRef; { // Hyperlink `href` attribute referring to matched fractum.
                    final ImagedBodyFractum[] referentFracta = iRef.fracta();
                    final int r; { // Index in `referentFracta` of the matched body fractum, or -1.
                        final Matcher m = jP.matcher( iRef.sourceText() ).region( region, regionEnd );
                        r = seek( m, referentFracta, rSelf );
                        if( r == -2 ) {
                            final CharacterPointer p = characterPointer( eP );
                            mould.warn( sourceFile, p, "No such fractal head\n" + p.markedLine() );
                            continue iF; }
                        final int s = r + 1;
                        if( s < referentFracta.length ) {
                            if( seek_advance( m )) { /* A further match may exist that would indicate
                                an ambigous pattern.  Test for it: */
                                final int r2 = seek( m, referentFracta, rSelf, /*ignoring*/r/* because
                                  any ‘further match in the same head … will be ignored.’  [RFI] */ );
                                if( r2 != -2 ) { // Then a further fractum is matched.
                                    final CharacterPointer p = characterPointer( eP );
                                    final int rLineNumber = r < 0 ? 1 : referentFracta[r].lineNumber();
                                    mould.warn( sourceFile, p, "Ambiguous pattern: fracta at lines "
                                      + rLineNumber + " and " + referentFracta[r2].lineNumber()
                                      + " both match\n" + p.markedLine() ); // This is disallowed. [RFI]
                                    continue iF; }}
                            else assert false; /* That `seek_advance` cannot fail given the prior guard
                              `s < referentFracta.length`. */
                            region = referentFracta[s].xunc(); } /* Seek any next pattern in `r` body,
                              which, if `r` has a body (see `regionEnd` below), begins with `s` head. */
                        else region = regionEnd; } // No more fracta, no more search region.
                    if( r < 0 ) { // Then the referent is the file fractum.
                        assert r == -1;
                        hRef = hRef_filePart.length() > 0 ? hRef_filePart // Either to that file,
                          : '#' +  fileFractumIdentifier; } // or to the top of the present file.
                    else { // The referent is a body fractum.
                        final ImagedBodyFractum referent = referentFracta[r];
                        hRef = hRef_filePart + '#' +  referent.identifier();
                        regionEnd = referent.xuncEnd(); }
                    assert region <= regionEnd; } // Ready for the next pattern in the series, if any.
                final Element a = d.createElementNS( nsHTML, "html:a" );
                a.setAttribute( "href", hRef );
                while( (n = eP.getFirstChild()) != null ) a.appendChild( n ); // All `eP` children wrap-
                eP.appendChild( a ); }}}                                     // ped to form a hyperlink.



    private final DOMSource fromDOM = new DOMSource();



    private final StreamSource fromStream = new StreamSource();



    private Font glyphTestFont;



    /** @return The head of the given fractum, or null if there is none.
      * @see #finalHead(Element)
      */
    private static Element head( final Element fractum ) {
        final Element h = (Element)fractum.getFirstChild();
        assert h == null && hasName( "FileFractum", fractum )  ||  hasName( "Head", h );
        return h; }



    /** Returns the hyperlink target reference (`href` attribute of `a` element) to use for `eRef`,
      * or null to omit hyperlinking.
      *
      *     @param f The absolute path of a source file.
      *     @param eRef The unfinished image from `f` of a URI reference.
      *     @param sRef The reference itself in string form, after any applicable
      *       {@linkplain ImageMould#translate(String,Path) `-reference-mapping` translations}.
      *     @param isAlteredRef Whether `sRef` was actually changed by such translation. *//*
      *     @paramImplied #stringBuilder
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
        if( isRemote( uRef )) return hRefRemote( f, eRef, sRef, isAlteredRef, uRef );  /*
          The referent would be reachable through a network, the reference
          being a URI or network-path reference. [RR] */

      // local  [RC]
      /* ┈┈┈┈┈
          The referent would be reachable through a file system, the reference
          being an absolute-path reference or relative-path reference. [RR] */
        final Path pRef; // The reference parsed as a local file path.
        final Path pRefAbsolute; { // `pRef` resolved against the parent directory of `f`.
            try { pRef = mould.toPath( uRef, f ); }
            catch( final IllegalArgumentException x ) {
                final CharacterPointer p = characterPointer( eRef );
                mould.warnOnce( f, p, x.getMessage() + '\n' + mould.markedLine( sRef, p, isAlteredRef ));
                return null; } // Without a hyperlink ∵ `x` leaves the intended referent unclear.
            pRefAbsolute = f.resolveSibling( pRef ); }
        if( !exists( pRefAbsolute )) {
            final StringBuilder bMessage = clear( stringBuilder );
            final boolean isTransX = mould.isTransX( pRefAbsolute, bMessage );
            final boolean wouldPrivatizationSuppress = isAlteredRef && isTransX;
            final CharacterPointer p = characterPointer( eRef );
            final String markedLine = mould.markedLine( sRef, p, isAlteredRef );
            if( wouldPrivatizationSuppress && isPrivatized(ownerFractum(eRef)) ) {
                logger.fine( () -> wrnHead(f,p.lineNumber) + bMessage
                  + ": Omitting a hyperlink for this private reference:\n" + markedLine );
                return null; } /* With neither hyperlink nor warning, because this type
                  of inaccessibility is common when a private reference is altered
                  by a `-reference-mapping` translation. */
            if( wouldPrivatizationSuppress ) {
                bMessage.append( "; consider marking this reference as private" ); }
            bMessage.append( ":\n" ).append( markedLine );
            mould.warnOnce( f, p, bMessage.toString() ); } /* Yet carry on and form the hyperlink,
              for the cause of inaccessibility could be a misplacement or misconfiguration
              of the referent as opposed to a malformation of the reference. */
        return hRefLocal( f, eRef, sRef, isAlteredRef, uRef, pRef, pRefAbsolute ); }



    /** Returns the hyperlink target reference (`href` attribute of `a` element) to use for `eRef`,
      * the referent of which is known to exist locally at `pRefAbsolute`, or null to omit hyperlinking.
      *
      * <p>Where {@linkplain ImageMould#translate(String,Path) `-reference-mapping`}
      * alters a reference, this method is called twice: first with the original reference,
      * then with the altered reference.</p>
      *
      *     @param f The absolute path of a source file.
      *     @param eRef The unfinished image from `f` of an absolute-path reference
      *       or relative-path reference.
      *     @param sRef The reference itself in string form, after any applicable
      *       {@linkplain ImageMould#translate(String,Path) `-reference-mapping` translations}.
      *     @param isAlteredRef Whether `sRef` was actually changed by such translation.
      *     @param uRef The reference in parsed `URI` form.
      *     @param pRef The reference translated to a local file path by way of
      *       `{@linkplain ImageMould#toPath(URI,Path) ImageMould.toPath}`.
      *     @param pRefAbsolute `pRef`resolved against the parent directory of `f`.
      *     @see <a href='https://www.rfc-editor.org/rfc/rfc3986#section-4.2'>
      *       URI generic syntax §4.2, ‘absolute-path reference’ and ‘relative-path reference’</a>
      */
    protected String hRefLocal( final Path f, final Element eRef, final String sRef,
          final boolean isAlteredRef, final URI uRef, final Path pRef, final Path pRefAbsolute ) {
        Path p = pRef; // Either `pRef` or its image sibling.
        if( !isDirectory( pRefAbsolute )) {
            if( looksBrecciaLike( pRef )) {
                final boolean imageExists; { /* Whether this referent (a Breccian source file
                      it appears) has an image file either (a) pre-existing or (b) newly formed. */
                    final Path pRefImageSib = imageSibling( pRefAbsolute );
                    imageExists = /*(a)*/isRegularFile( pRefImageSib )
                      || /*(b)*/pRefAbsolute.startsWith( mould.boundaryPathDirectory )
                           && isRegularFile( mould.outputDirectory.resolve(
                                mould.boundaryPathDirectory.relativize( pRefImageSib ))); }
                if( imageExists ) p = imageSibling( p ); }
            else if( !isAlteredRef/*[LC]*/  &&  looksImageLike(pRef)  &&  !isNonFractal(eRef) ) {
                warn_imageFileReference( f, eRef, sRef, isAlteredRef ); }} /* Yet carry on and form
                  the hyperlink, for the purpose here is satified by flagging the fault in the source. */
        return to_URI_relativeReference( p ); /* Effectively `sRef` (or its image sibling)
          after tilde expansion as per `ImageMould.toPath`. */ }



    /** Returns the hyperlink target reference (`href` attribute of `a` element) to use for `eRef`,
      * or null to omit hyperlinking.
      *
      * <p>Where {@linkplain ImageMould#translate(String,Path) `-reference-mapping`}
      * alters a reference, this method is called twice: first with the original reference,
      * then with the altered reference.</p>
      *
      *     @param f The absolute path of a source file.
      *     @param eRef The unfinished image from `f` of a URI reference in the form of a URI
      *       or network-path reference.
      *     @param sRef The reference itself in string form, after any applicable
      *       {@linkplain ImageMould#translate(String,Path) `-reference-mapping` translations}.
      *     @param isAlteredRef Whether `sRef` was actually changed by such translation.
      *     @param uRef The reference in parsed `URI` form.
      *     @see <a href='https://www.rfc-editor.org/rfc/rfc3986#section-4.1'>
      *       URI generic syntax §4.1, URI reference</a>
      *     @see <a href='https://www.rfc-editor.org/rfc/rfc3986#section-3'>
      *       URI generic syntax §3, ‘URI’</a>
      *     @see <a href='https://www.rfc-editor.org/rfc/rfc3986#section-4.2'>
      *       URI generic syntax §4.2, ‘network-path reference’</a>
      */
    protected String hRefRemote( final Path f, final Element eRef, final String sRef,
          final boolean isAlteredRef, final URI uRef ) {
        URI u = uRef; // Either `uRef` or its image sibling.
        if( looksBrecciaLike( uRef )) {
         /* u = imageSibling( uRef );  Only if `uRef` actually has an image sibling,
            which cannot be tested at present, for HTTP access is deferred. [NH] */ }
        else if( !isAlteredRef/*[LC]*/  &&  looksImageLike(uRef)  &&  !isNonFractal(eRef) ) {
            warn_imageFileReference( f, eRef, sRef, isAlteredRef ); } /* Yet carry on and form
              the hyperlink, for the purpose here is satified by flagging the fault in the source. */
        return u.toASCIIString(); }



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



    private final Map<String,Integer> idMap;
      // Fractum base identifiers (keys) for the present image file of `translate`, each mapped
      // to the count of its occurences (value).  Base identifiers omit any ordinal suffix.



    private final ArrayList<ImagedBodyFractum> imagedBodyFracta; { // For use by `translate`, or
        final int c = 0x2000; // = 8192                               elsewhere as `paramImplied`.
        idMap = new HashMap<>( initialCapacity( c ));
        imagedBodyFracta = new ArrayList<>( c ); }



    private static final ImagedBodyFractum[] imagedBodyFractaType = new ImagedBodyFractum[0];



    private static boolean isFractum( final Element e ) { return e.hasAttribute( "typestamp" ); }



    /** Whether the given image of a URI reference is marked `non-fractal`.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/language_definition.brec.xht#-,resource,indicant'>
      *       Breccia language definition § Resource indicant, `non-fractal` qualifier</a>
      */
    protected boolean isNonFractal( final Element eRef ) {
        final Element iR = parentElement( eRef );
        if( !hasName( "ResourceIndicant", iR )) {
            assert false : "URI references occur as the direct content of resource indicants alone";
            return false; }
        return iR.getAttribute("qualifiers").contains( "non-fractal" ); }



    /** Returns true if `fractum` is marked as private (whether directly or indirectly)
      * by the use of a privatizer; false otherwise.
      *
      *     @see Cursor#isPrivatized(int[])
      */
    protected static boolean isPrivatized( Element fractum ) {
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
      * @return The token transformed as necessary to serve as a keyword in a fractum `id` attribute. *//*
      * @paramImplied #stringBuilder
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



    protected final ImageMould<?> mould;



    private static ImageFile newImageFile( final Path imageFile, final Element fileFractum,
          final ImagedBodyFractum[] fracta ) {
        final String text = sourceText( fileFractum );
        assert text.length() == xuncEnd( fileFractum, fracta );
        return new ImageFile( text, fracta ); }



    private static final String nsHTML = "http://www.w3.org/1999/xhtml";



    private static final String nsImager = "data:,Breccia/Web/imager";



    private static final String nsXMLNS = "http://www.w3.org/2000/xmlns/";



    /** A pattern that `matches` in a regular-expression pattern a `\N{⋯}` element designating
      * a character by its numeric code point.  It captures as group (1) the code point.
      *
      *     @see java.util.regex.Matcher#match()
      *     @see <a href='http://reluk.ca/project/Breccia/language_definition.brec.xht#n'>
      *       Breccia language definition § Pattern language, `\N{⋯}` element</a>
      */
    private static final Pattern numberedCharacterBackslashPattern = Pattern.compile(
      "\\\\N\\{ *U\\+(\\p{XDigit}+) *\\}" );



    private final Matcher numberedCharacterBackslashMatcher =
      numberedCharacterBackslashPattern.matcher( "" );



    private final ImagingOptions opt;



    /** Returns the nearest fractal ancestor of `node`, or null if there is none.
      *
      *     @return The nearest ancestor of `node` that is a fractum, or null if there is none.
      */
    protected static Element ownerFractum( final Node node ) {
        Element a = parentElement( node );
        while( a != null && !isFractum(a) ) a = parentElement( a );
        return a; }



    /** Returns the same `e` if it is a fractum, otherwise `ownerFractum(e)`.
      */
    protected static Element ownerFractumOrSelf( final Element e ) {
        return isFractum(e) ? e : ownerFractum(e); }



    /** Returns the nearest ancestor of `node` that is a fractal head, or null if there is none.
      */
    protected static Element ownerHead( Node node ) {
        do node = node.getParentNode(); while( node != null && !hasName("Head",node) );
        return (Element)node; }



    /** Returns the same `e` if it is a fractal head, otherwise `ownerHead(e)`.
      */
    protected static Element ownerHeadOrSelf( final Element e ) {
        return hasName("Head",e) ? e : ownerHead(e); }



    /** Returns the Java translation of `eP` compiled with `mm`.
      *
      *     @param eP The image of a regular-expression pattern within a pattern matcher.
      *     @param mm The match modifiers, or an empty string if there are none.
      *     @param flags Default {@linkplain Pattern#flags() match flags}, or zero if there are none.
      *       The match flags for the resulting pattern will comprise this default
      *       together with the translatation of any `mm`.
      *     @throws PatternSyntaxException
      *       As for {@linkplain Pattern#compile(String,int) Pattern.compile}. *//*
      *     @paramImplied #stringBuilder
      */
    private Pattern pattern( final Node eP, final String mm, int flags ) {

      // Pattern
      // ───────
        final StringBuilder bP = clear( stringBuilder ); // The Java translation of `eP`.
        for( Node n = eP.getFirstChild();  n != null;  n = n.getNextSibling() ) {
            assert isElement( n ); // ↘ for reason
            bP.append( switch( n.getLocalName()/* ≠ null, given the assertion above */) { // [NSC]
                case "Granum" -> Pattern.quote( textChildFlat( n ));
                case "BackslashedSpecial" -> {
                    final String tF = textChildFlat( n );
                    if( tF.equals( "\\t" )) yield "(?:    )";
                    final Matcher m = numberedCharacterBackslashMatcher.reset( tF );
                    if( m.matches() ) yield "\\x{" + m.group(1) + "}";
                    yield tF; }
                case "Literalizer" -> {
                    n = n.getNextSibling(); // Skipping past the literalizer `\`.
                    assert hasName( "Granum", n ); /* Always a literalizer is followed
                      directly by a `Granum` that starts with the character to literalize. */
                    yield Pattern.quote( textChildFlat( n )); } /* Yielding the `Granum`
                      quoted as usual, for that suffices to literalize the character. */
                case "PerfectIndentMarker" -> "^(?:    )*";
                default -> textChildFlat( n ); }); }

      // Flags, aka match modifiers
      // ─────
        for( int c = 0, cN = mm.length(); c < cN; ++c ) flags |= switch( mm.charAt( c )) {
            case 'm' -> MULTILINE;
            case 's' -> DOTALL;
            default -> throw new IllegalArgumentException( "Match modifiers `" + mm + '`' ); };
              // Unexpected, the Breccia parser should have caught and reported it to the user.
        return Pattern.compile( bP.toString(), flags ); }



    /** Returns a record of the given image file, or null if none could be formed.
      * Caches return values in `imageFilesLocal`.
      *
      *     @param imageFile The absolute, normalized path of an existing image file.
      *     @see ImageMould#imageFilesLocal *//*
      *
      *     @paramImplied #imagedBodyFracta
      */
    private ImageFile recorded( final Path imageFile ) {
        assert imageFile.isAbsolute();
        ImageFile rec = mould.imageFilesLocal.get( imageFile );
        if( rec == null ) {
            assert exists( imageFile );
            final Element fileFractum; {
                try { fileFractum = fileFractum( imageFile ); }
                catch( ErrorAtFile x ) { throw new Unhandled( x ); }}
            imagedBodyFracta.clear();
            for( Element bF = successorFractum(fileFractum);  bF != null;  bF = successorFractum(bF) ) {
                imagedBodyFracta.add( new ImagedBodyFractum(
                  parseUnsignedInt( bF.getAttribute( "xunc" )),
                  parseUnsignedInt( bF.getAttribute( "lineNumber" )),
                  bF.getAttribute( "id" ), xuncEnd( bF ))); }
            rec = newImageFile( imageFile, fileFractum, imagedBodyFracta.toArray(imagedBodyFractaType) );
            mould.imageFilesLocal.put( imageFile, rec ); }
        return rec; }



    /** Seeks the next match of a fractum-indicant pattern in a referent source text.
      *
      *     @param m A matcher preconfigured for the purpose, preset to the search region
      *       of the referent source text and ready for immediate use.
      *     @param fracta The imaged body fracta of the referent source text.
      *     @param fSelf The index in `fracta` of the nearest fractal ancestor of the fractum indicant,
      *        or -1 if that ancestor is the file fractum of the referent source text,
      *        or -2 if the fractum indicant lies outside of the referent source text.
      *        The value is other than -2 only in the case of a same-document reference.
      *     @return The index in `fracta` of the matched body fractum, or -1 if instead the file fractum
      *       is matched, or -2 if no fractum is matched.
      */
    private static int seek( final Matcher m, final ImagedBodyFractum[] fracta, final int fSelf ) {
        while( m.find() ) {
            final int f = seek( m.start(), fracta ); // Index in `fracta` of the matched fractum, or -1.
            if( f == fSelf ) { /* Then ignore this match, for matches of a fractum-indicant pattern
                  are ‘excluded from the head in which the fractum indicant is contained.’ [RFI] */
                if( seek_advance( m )) continue;
                break; }
            final int g = f + 1;
            if( g < fracta.length ) {
                final int fEnd = fracta[g].xunc(); // End boundary of the head of the matched fractum.
                if( m.end() >= fEnd ) { /* Then this match extends across multiple heads.
                      Ignore this match, for matches are ‘confined to a single head.’ [RFI] */
                    if( seek_advance( m )) continue;
                    break; }}
            return f; }
        return -2; }



    /** @param fIgnore The index in `fracta` of a body fractum whose matches to ignore,
      *    or -1 to ignore matches of the file fractum.
      */
    private static int seek( final Matcher m, final ImagedBodyFractum[] fracta, final int fSelf,
          final int fIgnore ) {
        do {
            final int f = seek( m, fracta, fSelf );
            if( f != fIgnore ) return f; }
            while( seek_advance( m ));
        return -2; }



    /** Seeks the fractum in whose head the given offset lies.
      *
      *     @param xunc An offset in UTF-16 code units from the start of a source text.
      *     @param fracta A linear-order array of the source text’s imaged body fracta.
      *     @return The index in `fracta` of the body fractum in whose head the offset lies,
      *       or -1 if instead the offset lies in the head of the file fractum.
      */
    private static int seek( final int xunc, final ImagedBodyFractum[] fracta ) {
        for( int f = fracta.length - 1;; --f ) {
            if( f < 0 ) return -1;
            if( fracta[f].xunc() <= xunc ) return f; }}



    private static boolean seek_advance( final Matcher m ) {
        final int matchStart = m.start();
        final int regionEnd = m.regionEnd();
        if( matchStart == regionEnd ) return false; // Maybe possible given a zero-width assertion.
        m.region( matchStart + 1, regionEnd ); /* Far enough to avoid a rematch.
          Whether `matchStart + 1` is too timid, too aggressive, or neither is uncertain.
          It might depend on the richness of the pattern language. */
        return true; }



    private final C sourceCursor;



    /** The original text content of the given node and its descendants, prior to any translation.
      */
    private static String sourceText( final Element node ) { return node.getTextContent(); }
      // Should the translation ever introduce text of its own, then it must be marked as non-original,
      // e.g. by some attribute defined for that purpose.  The present method would then be modified
      // to remove all such text from the return value, e.g. by cloning `node`, filtering the clone,
      // then calling `getTextContent` on it.
      //     Non-original elements that merely wrap original content would neither be markednor removed,
      // as their presence would have no effect on the return value.



    private final StringBuilder stringBuilder = new StringBuilder(
      /*initial capacity*/0x2000 ); // = 8192



    private final StringBuilder stringBuilder2 = new StringBuilder(
      /*initial capacity*/0x2000 ); // = 8192



    /** Returns the fractal successor of `n` in document order, including any first child,
      * or null if `n` has no fractal successor.
      *
      *     @see <a href='https://www.w3.org/TR/DOM-Level-3-Core/glossary.html#dt-document-order'>
      *       Definition of ‘document order’</a>
      */
    public static Element successorFractum( final Node n ) {
        Element e = successorElement( n );
        while( e != null && !isFractum(e) ) e = successorElement( e );
        return e; }



    /** Returns the exclusive fractal successor of `n` in document order, or null if there is none.
      *
      *     @see <a href='https://www.w3.org/TR/DOM-Level-3-Core/glossary.html#dt-document-order'>
      *       Definition of ‘document order’</a>
      *     @return The first fractal successor of `e` outside of its descendants,
      *       or null if none exists.
      */
    public static Element successorFractumAfter( final Node n ) {
        Element e = successorElementAfter( n );
        while( e != null && !isFractum(e) ) e = successorElementAfter( e );
        return e; }



    private static final String systemID_HTML = "about:legacy-compat";
      // https://html.spec.whatwg.org/multipage/syntax.html#the-doctype



    /** Returns the flat text (aka `data`) of the first child of the given node,
      * which child must be a text node.
      */
    private static String textChildFlat( final Node n ) { return ((Text)n.getFirstChild()).getData(); }



    private final DOMResult toDOM = new DOMResult();



    private final StreamResult toImageFile = new StreamResult();



    /** @param sourceFile The absolute path of a source file.
      * @param d Its unfinished image. *//*
        @paramImplied #stringBuilder
      * @paramImplied #stringBuilder2
      */
    protected void translate( final Path sourceFile, final Document d ) {

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
            final String p = textChildFlat( dL.getPreviousSibling() );
              // All `dL` have a `Granum` predecessor comprising flat text.
            int c = p.length();
            do --c; while( p.charAt(c) == ' ' );    // Scan leftward past any plain space characters,
            if( completesNewline( p.charAt( c ))) { // and there test for the presence of a newline.
                assert "".equals( dL.getAttribute( "class" ));
                dL.setAttribute( "class", "titling" ); }}


      // ════════════
      // File fractum
      // ════════════
        idMap.clear();
        idMap.put( fileFractumIdentifier, 1 );
        fileFractum.setAttribute( "id", fileFractumIdentifier );


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
        imagedBodyFracta.clear();
        for( Element bF = successorFractum(fileFractum);  bF != null;  bF = successorFractum(bF) ) {

          // Identification by `id` attribution
          // ──────────────────────────────────
            final int kMax = 3; // Maximum number of keywords to include in the identifier.
            final ArrayList<String> keywords = new ArrayList<>( /*initial capacity*/kMax );

          // gather the longest keywords from the fractal head
          // ┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈
            final Element head = head( bF );
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
                break; }

          // Record of image
          // ───────────────
            imagedBodyFracta.add( new ImagedBodyFractum(
              parseUnsignedInt( bF.getAttribute( "xunc" )),
              parseUnsignedInt( bF.getAttribute( "lineNumber" )), id, xuncEnd(bF) )); }
        final Path imageFile = imageSibling(sourceFile).normalize();
        mould.imageFilesLocal.put( imageFile, newImageFile(
          imageFile, fileFractum, imagedBodyFracta.toArray(imagedBodyFractaType) )); }



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



    /* Note that Breccia Mode for Emacs plans a remedy for this type of malformed reference.
     * http://reluk.ca/project/Breccia/Emacs/working_notes.brec.xht#substitution,source-file,references
     */
    private void warn_imageFileReference( final Path f, final Element eRef, final String sRef,
          final boolean isAlteredRef ) {
        final CharacterPointer p = characterPointer( eRef );
        mould.warn( f, p, "Reference to an image file; consider qualifying the reference "
          + "as `non-fractal` or referring instead to the source file (`.brec`):\n"
          + mould.markedLine( sRef, p, isAlteredRef )); }



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
        xmlInputFactory.setProperty( "javax.xml.stream.supportDTD", false ); }
          // While a DTD is present in each image file (a requirement of HTML) it is empty. [DTR]



    /** @return The offset in the source text of the end boundary of the given fractum.
      */
    private static int xuncEnd( final Element fractum ) {
        Element e = successorFractumAfter( fractum );
        return e == null ? xuncHeadEnd( finalHead( fractum )) :
          parseUnsignedInt( e.getAttribute( "xunc" )); }



    /** @return The offset in the source text of the end boundary of the file fractum;
      *   in other words, the length of the source text.
      */
    private static int xuncEnd( final Element fileFractum, final ImagedBodyFractum[] fracta ) {
        final int fN = fracta.length;
        return fN == 0 ? xuncHeadEnd( head( fileFractum )) : fracta[fN-1].xuncEnd(); }



    /** @return The offset in the source text of the end boundary of `head`.
      */
    private static int xuncHeadEnd( final Element head ) {
        final String ends = head.getAttribute( "xuncLineEnds" );
        return parseUnsignedInt( ends.substring( ends.lastIndexOf(' ') + 1 )); }}



// NOTES
// ─────
//   ◦↓◦  Code that is order dependent with like-marked code (◦↕◦, ◦↑◦) that comes after.
//
//   ◦↕◦  Code that is order dependent with like-marked code (◦↓◦, ◦↕◦, ◦↑◦) that comes before and after.
//
//   ◦↑◦  Code that is order dependent with like-marked code (◦↓◦, ◦↕◦) that comes before.
//
//   BF↓  Code that must execute before section *Body fracta*`.
//
//   BF · Section *Body fracta* itself, or code that must execute in unison with it.
//
//   RFI  Resolving a fractum indicant.
//        http://reluk.ca/project/Breccia/language_definition.brec.xht#indicated,fractum,indicant
//
//   DTR  ‘A `DOCTYPE` is a required preamble’ in HTML.
//        https://html.spec.whatwg.org/multipage/syntax.html#the-doctype
//
//   F ·· Method `finish` itself, or code that must execute in unison with it.
//
//   HF · Hyperlink formation.  Done late (in the `finish` cycle) that hyperlink `href` attributes may
//        incorporate any body-fractum identifiers newly formed earlier (in the `translate` cycle).
//
//   LC · A lint check on the original source only.
//
//   MT · Mask trimming for ID stability.  The purpose is to omit any punctuation marks such as quote
//        characters, commas or periods that might destabilize the ID as the source text is edited.
//
//   NH · HTTP access has yet to be implemented here.
//        http://reluk.ca/project/Breccia/Web/imager/working_notes.brec.xht#deferral,hTTP,fetches
//
//   NSC  Presently ‘null in switch cases is a preview feature and is disabled by default’ (JDK 18),
//        else this code could be simplified.
//
//   RC · Referencing code.  Cf. the comparably structured code of `ImageMould.formalResources_record`.
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
