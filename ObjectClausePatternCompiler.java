package Breccia.Web.imager;

import Breccia.parser.plain.Language;
import Java.WhitespaceCollapser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.UNICODE_CASE;


/** Compiler of patterns from the object clause of an afterlinker.
  * Before each call to `compile`, ensure that `mSubject` is correctly set.
  *
  *     @see #mSubject
  */
final class ObjectClausePatternCompiler extends PatternCompiler {


    ObjectClausePatternCompiler( ImageMould<?> mould ) { super( mould ); }



    /** Returns the Java compilation of a default pattern matcher to be used where an object clause
      * is absent or comprises a fractal context locant.
      *
      *     @param cR The image of a referential command from an afterlinker.
      */
    final Pattern compileDefaultPattern( final Element cR ) throws FailedInterpolation {
        final StringBuilder bP = clear( stringBuilder );
        bP.append( anchoredPrefix_either );
        appendVariable_same( cR, 0, bP, /*toExpandWhitespace*/true );
        return Pattern.compile( bP.toString(), CASE_INSENSITIVE | UNICODE_CASE | MULTILINE/*[MLM]*/ ); }


    /** The Java pattern matcher of the subject clause successfully matched to the first subject.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/language_definition.brec.xht#subjects,afterlinker,-'>
      *       Breccia language definition, match procedure for a subject clause</a>
      */
    Matcher mSubject;



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    /** @see FailedInterpolation#interpolator
      * @see FailedInterpolation#index *//*
      * @paramImplied #stringBuilder2
      * @paramImplied #stringBuilder3
      */
    private void appendVariable_same( final Element interpolator, final int index,
          final StringBuilder bP, final boolean toExpandWhitespace ) throws FailedInterpolation {

      // Subject clause, in the presence of
      // ───────────────
        if( mSubject != null ) {
            final StringBuilder b = clear( stringBuilder2 );
            final int gN = mSubject.groupCount();

          // captures of its matcher
          // ┈┈┈┈┈┈┈┈
            if( gN > 0 ) {
                for( int g = 1;; ++g ) {
                    final String capture = mSubject.group( g );
                    assert capture != null && capture.length() != 0; // Implied by `mSubject` API.
                    quote( capture, b );
                    if( g == gN ) break;
                    b.append( ' ' ); }
                append( b, bP, toExpandWhitespace );
                return; }

          // whole match
          // ┈┈┈┈┈┈┈┈┈┈┈
            b.append( mSubject.group() );
            assert b.length() != 0; // Implied by `mSubject` API.
            BreccianCollapser.collapseWhitespace( b ); // This may empty `b`, wherefore:
            if( b.length() > 0 ) {
                final StringBuilder c = clear( stringBuilder3 );
                quote( b, c );
                append( c, bP, toExpandWhitespace ); }
            else append( " ", bP, toExpandWhitespace );
            return; }
        Node n = ownerFractum( interpolator );
        assert hasName( "Afterlinker", n );
        n = n.getParentNode(); // Parent of the present afterlinker.
        Node head = head( n ); // Wherein lies the subject.
        if( head == null ) {
            throw new FailedInterpolation( interpolator, index,
              "Misplaced back reference, no parent head to refer to" ); }
        final StringBuilder b = clear( stringBuilder2 );
        final StringBuilder c = clear( stringBuilder3 );
        if( hasName( "Division", n )) {
            final Element firstTitlingLabel = successorTitlingLabel( head, successorAfter(head) );

          // Titled division, the parent of the present afterlinker is
          // ───────────────
            if( firstTitlingLabel != null ) {
                b.append( textChildFlat( firstTitlingLabel ));
                BreccianCollapser.collapseWhitespace( b );
                quote( b, c );
                append( c, bP, toExpandWhitespace );
                return; }}

      // File fractum or point, the parent of the present afterlinker is
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
        BreccianCollapser.collapseWhitespace( b );
        quote( b, c );
        append( c, bP, toExpandWhitespace ); }



    private static final WhitespaceCollapser BreccianCollapser = new WhitespaceCollapser() {
        public @Override boolean isWhitespace( char ch ) { return Language.isWhitespace( ch ); }};



    private final StringBuilder stringBuilder = new StringBuilder( /*initial capacity*/0x800 ); // = 2048



    private final StringBuilder stringBuilder2 = new StringBuilder( /*initial capacity*/0x800 );



    private final StringBuilder stringBuilder3 = new StringBuilder( /*initial capacity*/0x800 );



   // ━━━  P a t t e r n   C o m p i l e r  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    /* @paramImplied #stringBuilder2
     * @paramImplied #stringBuilder3
     */
    protected @Override void append( final Element variable, final StringBuilder bP,
          final boolean toExpandWhitespace ) throws FailedInterpolation {
        final String tF = textChildFlat( variable );

      // ${same}
      // ───────
        if( "${same}".equals( tF )) {
            appendVariable_same( variable, variableName, bP, toExpandWhitespace );
            return; }

      // ${1}, ${2}, ${3}, … ${9}
      // ─────
        if( tF.length() == /*e.g.*/"${1}".length() ) {
            final int g = tF.charAt(variableName) - '0';
            if( 1 <= g  &&  g <= 9 ) {
                if( mSubject == null ) {
                    throw new FailedInterpolation( variable, 0,
                      "Back reference to a subject-clause capture, but no subject clause" ); }
                final int gN = mSubject.groupCount();
                if( g > gN ) {
                    throw new FailedInterpolation( variable, variableName,
                      "No such capture group (" + g + ") in the subject clause" ); }
                final String capture = mSubject.group( g );
                assert capture != null && capture.length() != 0; // Implied by `mSubject` API.
                final StringBuilder b = clear( stringBuilder2 );
                quote( capture, b );
                append( b, bP, toExpandWhitespace );
                return; }}
        super.append( variable, bP, toExpandWhitespace ); }}



// NOTE
// ─────
//   MLM  Multi-line mode operation of Breccian pattern matchers.
//        http://reluk.ca/project/Breccia/language_definition.brec.xht#consistent,perl-s,multi-line
//        http://reluk.ca/project/Breccia/language_definition.brec.xht#consistent,perl-s,multi-line:2



                                                   // Copyright © 2022-2024  Michael Allan.  Licence MIT.
