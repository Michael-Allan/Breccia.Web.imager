package Breccia.Web.imager;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static Breccia.parser.plain.Language.completesNewline;
import static Breccia.parser.plain.Language.isDividerDrawing;
import static Java.Nodes.hasName;
import static Java.Nodes.isElement;
import static Java.Nodes.parentElement;
import static Java.Nodes.successor;
import static Java.Nodes.successorElement;
import static Java.Nodes.textChildFlat;


public final class ImageNodes {


    private ImageNodes() {}



    /** @return The head of the given fractum, or null if there is none.
      * @see BreccianFileTranslator#finalHead(Element)
      */
    public static Element head( final Node fractum ) {
        Element h = (Element)fractum.getFirstChild();
        assert h != null; // No fractum is both headless and bodiless.
        if( !hasName( "Head", h )) {
            assert hasName( "FileFractum", fractum ); // Which alone may be headless.
            h = null; }
        return h; }



    /** Whether `e` is the image of a fractum.
      */
    public static boolean isFractum( final Element e ) { return e.hasAttribute( "typestamp" ); }



    /** The namespace name for HTML.
      */
    public static final String nsHTML = "http://www.w3.org/1999/xhtml";



    /** The namespace name for Breccia Web Imager.
      */
    public static final String nsImager = "data:,Breccia/Web/imager";



    /** The namespace name for XML namespacing.
      */
    public static final String nsXMLNS = "http://www.w3.org/2000/xmlns/";



    /** Returns the nearest fractal ancestor of `node`, or null if there is none.
      *
      *     @return The nearest ancestor of `node` that is a fractum, or null if there is none.
      *     @see <a href='http://reluk.ca/project/Breccia/glossary.brec.xht#owning,fractum'>
      *       Definition of ‘owning fractum’</a>
      */
    public static Element ownerFractum( final Node node ) {
        Element a = parentElement( node );
        while( a != null && !isFractum(a) ) a = parentElement( a );
        return a; }



    /** Returns the same `e` if it is a fractum, otherwise `ownerFractum(e)`.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/glossary.brec.xht#owning,fractum'>
      *       Definition of ‘owning fractum’</a>
      */
    public static Element ownerFractumOrSelf( final Element e ) {
        return isFractum(e) ? e : ownerFractum(e); }



    /** Returns the nearest ancestor of `node` that is a fractal head, or null if there is none.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/glossary.brec.xht#owning,fractum'>
      *       Definition of ‘owning fractum’</a>
      */
    public static Element ownerHead( Node node ) {
        do node = node.getParentNode(); while( node != null && !hasName("Head",node) );
        return (Element)node; }



    /** Returns the same `e` if it is a fractal head, otherwise `ownerHead(e)`.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/glossary.brec.xht#owning,fractum'>
      *       Definition of ‘owning fractum’</a>
      */
    public static Element ownerHeadOrSelf( final Element e ) {
        return hasName("Head",e) ? e : ownerHead(e); }



    /** The original text content of the given node prior to any translation.  The original text
      * is recovered as the text content of the node and its descendants exclusive of any contained
      * within an element marked by an `{@linkplain #nsImager img}:nonOriginalText` attribute.
      *
      *     @return The original text content, or the empty string if there is none.
      */
    public static String sourceText( Node node ) {
        if( hasAttribute_nonOriginalText( node )) return "";
        node = node.cloneNode( /*deeply*/true );
        for( Node p, n = successor(p = node);  n != null;  n = successor(p = n) ) {
            if( hasAttribute_nonOriginalText( n )) {
                n.getParentNode().removeChild( n );
                n = p; }} // Resuming from the predecessor of element `n`, now removed.
        return node.getTextContent(); }



    /** Returns the next titling label subsequent to `n` in document order,
      * inclusive of any descendants of `n`, or null if there is none.
      *
      *     @see <a href='https://www.w3.org/TR/DOM-Level-3-Core/glossary.html#dt-document-order'>
      *       Definition of ‘document order’</a>
      */
    public static Element successorTitlingLabel( Node n ) { return successorTitlingLabel( n, null ); }



    /** Returns the next titling label that succeeds `n` and precedes `nBoundary` in document order,
      * inclusive of any descendants of `n`, or null if there is none.
      *
      *     @param nBoundary The exclusive end boundary of the search beyond `n`, or null to search
      *       the remainder of the document.
      *     @see <a href='https://www.w3.org/TR/DOM-Level-3-Core/glossary.html#dt-document-order'>
      *       Definition of ‘document order’</a>
      */
    public static Element successorTitlingLabel( Node n, final Node nBoundary ) {
        Element e = successorElement( n );
        do {
            if( !hasName( "DivisionLabel", e )) continue;
            n = e.getPreviousSibling();
            assert hasName( "Granum", n ); // A granum of flat text `t` precedes all division labels.
         // final String t = textChildFlat( n );
         /// but the imager may have wrapped that text child, e.g. with a self-hyperlink `a` element
            final String t = sourceText( n );
            char ch;
            int c = t.length();
            do --c; while( (ch = t.charAt(c)) == ' ' ); // Scan leftward past any plain space characters,
            if( completesNewline( ch )) return e; /*       and there test for the presence of a newline.
              That preceding newline proves that the label leads the line on which it occurs, which by
              definition makes it a titling label. */
            assert isDividerDrawing( ch ); } // The only alternative is a divider drawing character.
            while( (e = successorElement(e)) != nBoundary );
        return null; }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private static boolean hasAttribute_nonOriginalText( final Node n ) {
        return isElement(n) && ((Element)n).hasAttributeNS(nsImager,"nonOriginalText"); }}



                                                  // Copyright © 2022, 2024  Michael Allan.  Licence MIT.
