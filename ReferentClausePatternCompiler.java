package Breccia.Web.imager;

import java.util.regex.Matcher;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static Breccia.Web.imager.ImageNodes.head;
import static Breccia.Web.imager.ImageNodes.ownerFractum;
import static Breccia.Web.imager.ImageNodes.sourceText;
import static Breccia.Web.imager.ImageNodes.successorTitlingLabel;
import static Java.Nodes.hasName;
import static Java.Nodes.successor;
import static Java.Nodes.successorAfter;
import static Java.Nodes.successorElement;
import static Java.Nodes.textChildFlat;
import static Java.Patterns.quote; // Yields more readable patterns than does `Pattern.quote`.
import static Java.StringBuilding.clear;
import static Java.StringBuilding.collapseWhitespace;
import static java.util.regex.Pattern.MULTILINE;


/** Compiler of patterns from the referent clause of an associative reference.
  * Before each call to `compile`, ensure that `mReferrer` is correctly set.
  *
  *     @see #mReferrer
  */
final class ReferentClausePatternCompiler extends PatternCompiler {


    ReferentClausePatternCompiler( ImageMould<?> mould ) { super( MULTILINE/*pattern matchers
      in this context operate in ‘multiple-line mode’ [RFI]*/, mould ); }



    /** The pattern matcher of the referrer clause successfully matched to the referrer.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/language_definition.brec.xht#referrers,associative,reference:2'>
      *       Breccia language definition, match procedure for a referrer clause</a>
      */
    Matcher mReferrer;



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private final StringBuilder stringBuilder = new StringBuilder( /*initial capacity*/0x800 ); // = 2048



    private final StringBuilder stringBuilder2 = new StringBuilder( /*initial capacity*/0x800 );



   // ━━━  P a t t e r n   C o m p i l e r  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    protected @Override void append( final Element variable, final StringBuilder bP,
          final boolean toExpandSpaces ) throws FailedInterpolation {
        final String tF = textChildFlat( variable );

      // ═══════
      // ${same}
      // ═══════
        if( "${same}".equals( tF )) {

          // Referrer clause, in the presence of
          // ───────────────
            if( mReferrer != null ) {
                final StringBuilder b = clear( stringBuilder );
                final int gN = mReferrer.groupCount();

              // captures of its matcher
              // ┈┈┈┈┈┈┈┈
                if( gN > 0 ) {
                    for( int g = 1;; ++g ) {
                        final String capture = mReferrer.group( g );
                        assert capture != null && capture.length() != 0; // Implied by `mReferrer` API.
                        quote( capture, b );
                        if( g == gN ) break;
                        b.append( ' ' ); }
                    append( b.toString(), bP, toExpandSpaces );
                    return; }

              // whole match
              // ┈┈┈┈┈┈┈┈┈┈┈
                b.append( mReferrer.group() );
                assert b.length() != 0; // Implied by `mReferrer` API.
                collapseWhitespace( b ); // This may empty `b`, wherefore:
                if( b.length() > 0 ) {
                    final StringBuilder c = clear( stringBuilder2 );
                    quote( b, c );
                    append( c.toString(), bP, toExpandSpaces ); }
                else append( " ", bP, toExpandSpaces );
                return; }
            Node n = ownerFractum( variable );
            assert hasName( "AssociativeReference", n );
            n = n.getParentNode(); // Parent of the present associative reference.
            Node head = head( n ); // Wherein lies the referrer.
            if( head == null ) {
                throw new FailedInterpolation( variable, variableName,
                  "Misplaced back reference, no parent head to refer to" ); }
            final StringBuilder b = clear( stringBuilder );
            final StringBuilder c = clear( stringBuilder2 );
            if( hasName( "Division", n )) {
                final Element firstTitlingLabel = successorTitlingLabel( head, successorAfter(head) );

              // Titled division, the parent of the present associative reference is
              // ───────────────
                if( firstTitlingLabel != null ) {
                    b.append( textChildFlat( firstTitlingLabel ));
                    collapseWhitespace( b );
                    quote( b, c );
                    append( c.toString(), bP, toExpandSpaces );
                    return; }}

          // File fractum or point, the parent of the present associative reference is
          // ─────────────────────
            head = head.cloneNode( /*deeply*/true ); /* So both preserving the original parent head,
              and keeping the nodal scan that follows within the bounds of the isolated clone. */
            strip: for( n = successorElement(head);  n != null;  n = successorElement(n) ) {
                final String name = n.getLocalName();
                if( "CommentAppender".equals( name )
                 || "CommentBlock"   .equals( name )
                 || "IndentBlind"    .equals( name )) {
                    for( ;; ) { // Remove it and all successors.
                        final Node s = successorAfter( n );
                        n.getParentNode().removeChild( n );
                        if( s == null ) break strip;
                        n = s; }}}
            b.append( sourceText( head ));
            collapseWhitespace( b );
            quote( b, c );
            append( c.toString(), bP, toExpandSpaces );
            return; }


      // ═════
      // ${1}, ${2}, ${3}, … ${9}
      // ═════
        if( tF.length() == /*e.g.*/"${1}".length() ) {
            final int g = tF.charAt(variableName) - '0';
            if( 1 <= g  &&  g <= 9 ) {
                if( mReferrer == null ) {
                    throw new FailedInterpolation( variable, 0,
                      "Back reference to a referrer-clause capture, but no referrer clause" ); }
                final int gN = mReferrer.groupCount();
                if( g > gN ) {
                    throw new FailedInterpolation( variable, variableName,
                      "No such capture group (" + g + ") in the referrer clause" ); }
                final String capture = mReferrer.group( g );
                assert capture != null && capture.length() != 0; // Implied by `mReferrer` API.
                final StringBuilder b = clear( stringBuilder );
                quote( capture, b );
                append( b.toString(), bP, toExpandSpaces );
                return; }}
        super.append( variable, bP, toExpandSpaces ); }}



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
