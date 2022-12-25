package Breccia.Web.imager;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static Breccia.parser.plain.Language.completesNewline;
import static Java.Nodes.hasName;
import static Java.Nodes.parentElement;
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



    /** Returns the nearest fractal ancestor of `node`, or null if there is none.
      *
      *     @return The nearest ancestor of `node` that is a fractum, or null if there is none.
      */
    public static Element ownerFractum( final Node node ) {
        Element a = parentElement( node );
        while( a != null && !isFractum(a) ) a = parentElement( a );
        return a; }



    /** Returns the same `e` if it is a fractum, otherwise `ownerFractum(e)`.
      */
    public static Element ownerFractumOrSelf( final Element e ) {
        return isFractum(e) ? e : ownerFractum(e); }



    /** Returns the nearest ancestor of `node` that is a fractal head, or null if there is none.
      */
    public static Element ownerHead( Node node ) {
        do node = node.getParentNode(); while( node != null && !hasName("Head",node) );
        return (Element)node; }



    /** Returns the same `e` if it is a fractal head, otherwise `ownerHead(e)`.
      */
    public static Element ownerHeadOrSelf( final Element e ) {
        return hasName("Head",e) ? e : ownerHead(e); }



    /** The original text content of the given node and its descendants, prior to any translation.
      */
    public static String sourceText( final Node node ) { return node.getTextContent(); }
      // Should the translation ever introduce text of its own, then it must be marked as non-original,
      // e.g. by some attribute defined for that purpose.  The present method would then be modified
      // to remove all such text from the return value, e.g. by cloning `node`, filtering the clone,
      // then calling `getTextContent` on it.
      //     Non-original elements that merely wrap original content would neither be marked nor removed,
      // as their presence would have no effect on the return value.



    /** Returns the next titling label that succeeds `n` and precedes `nBoundary` in document order,
      * inclusive of any descendants of `n`, or null if there is none.
      *
      *     @param nBoundary The exclusive end boundary of the search beyond `n`, or null to search
      *       the remainder of the document.
      *     @see <a href='https://www.w3.org/TR/DOM-Level-3-Core/glossary.html#dt-document-order'>
      *       Definition of ‘document order’</a>
      */
    public static Element successorTitlingLabel( Node n, final Node nBoundary ) {
        for( Element e = successorElement( n );; ) {
            if( hasName( "DivisionLabel", e )) {
                n = e.getPreviousSibling();
                assert hasName( "Granum", n ); // A granum of flat text precedes all division labels.
                final String s = textChildFlat( n );
                for( int c = s.length();; ) {
                    final char ch = s.charAt( --c );
                    if( ch != ' ' ) {
                        if( completesNewline( ch )) return e; /* For it leads the line on which
                          it occurs, so preceding any divider drawing character of the same line,
                          which by definition makes it a titling label. */
                        break; } // Label `e` is no titling label, keep searching.
                    assert c != 0; }} // A division label cannot be preceded by plain space alone.
            e = successorElement( e );
            if( e == nBoundary ) return null; }}}



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
