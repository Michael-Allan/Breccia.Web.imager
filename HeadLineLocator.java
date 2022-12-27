package Breccia.Web.imager;

import java.util.StringTokenizer;
import org.w3c.dom.Element;

import static java.lang.Integer.parseUnsignedInt;
import static Java.Nodes.parentAsElement;


final class HeadLineLocator extends Java.TextLineLocator {


    HeadLineLocator() { super( new Java.IntArrayExtensor( new int[0x100] )); } // = 256



    /** Locates the line in which the given granum falls.
      */
    void locateLine( final Element granum ) {
      locateLine( parseUnsignedInt( granum.getAttribute( "xunc" )), offsetRegional, numberRegional ); }



    /** Locates the line in which the given offset falls.
      *
      *     @param xunc An offset in UTF-16 code units from the start of a source text.
      *       Normally it lies in the present region.  If rather it lies before `offsetRegional`,
      *       then instead this method uses `offsetRegional`; or if it lies after the region,
      *       then this method yields the last line of the region.
      */
    void locateLine( int offset ) { locateLine( offset, offsetRegional, numberRegional ); }



    /** The ordinal number in the source text of the first line of the present region.
      *
      *     @see #region(Element)
      */
    int numberRegional() { return numberRegional; }



    /** The offset from the start of the source text of the present region in UTF-16 code units.
      *
      *     @see #region(Element)
      */
    int offsetRegional() { return offsetRegional; }



    /** Sets from the given head `endsRegional`, `numberRegional` and `offsetRegional`.
      */
    void region( final Element head ) {
        endsRegional.clear();
        final StringTokenizer tt = new StringTokenizer( head.getAttribute( "xuncLineEnds" ), " " );
        while( tt.hasMoreTokens() ) endsRegional.add( parseUnsignedInt( tt.nextToken() ));
        final Element fractum = parentAsElement( head );
        offsetRegional = parseUnsignedInt( fractum.getAttribute( "xunc" ));
        numberRegional = parseUnsignedInt( fractum.getAttribute( "lineNumber" )); }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private int numberRegional;



    private int offsetRegional; }



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
