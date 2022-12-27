package Breccia.Web.imager;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static Java.Nodes.hasName;
import static Java.Nodes.isElement;
import static Java.Nodes.textChildFlat;
import static Java.Patterns.metacharacters;
import static Java.StringBuilding.clear;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.MULTILINE;


/** A translator of regular-expression patterns from Breccian to compiled Javanese form.
  */
class PatternCompiler {


    /** @see #baseFlags
      */
    PatternCompiler( final int baseFlags, final ImageMould<?> mould ) {
        this.baseFlags = baseFlags;
        this.mould = mould; }



    /** Base match flags to apply by default, or zero if there are none.
      */
    final int baseFlags;



    /** Returns the Java compilation of `eP`, with {@linkplain Pattern#flags() match flags}
      * derived from `{@linkplain #baseFlags baseFlags}` and `matchModifiers`.
      *
      *     @param eP The image of a regular-expression pattern within a pattern matcher.
      *     @param matchModifiers The match modifiers, or an empty string if there are none.
      *     @throws PatternSyntaxException
      *       As for {@linkplain Pattern#compile(String,int) Pattern.compile}.
      */
    final Pattern compile( final Node eP, final String matchModifiers, final Path sourceFile )
          throws FailedInterpolation {

      // Match flags
      // ───────────
        int flags = baseFlags;
        final boolean toExpandSpaces; { // Whether expansive space mode is enabled.
            boolean pIsGiven = false;
            final int mN = matchModifiers.length();
            for( int m = 0; m < mN; ++m ) switch( matchModifiers.charAt( m )) {
                case 'i' -> flags |= CASE_INSENSITIVE;
                case 'm' -> flags |= MULTILINE;
                case 's' -> flags |= DOTALL;
                case 'p' -> pIsGiven = true;
                default -> {
                    throw new IllegalArgumentException( // Unexpected, because the Breccia parser
                      "Match modifiers `" + matchModifiers + '`' ); }} // should have caught it.
            toExpandSpaces = pIsGiven; }

      // Pattern
      // ───────
        final StringBuilder bP = clear( stringBuilder ); // The Java translation of `eP`.
        for( Node n = eP.getFirstChild();  n != null;  n = n.getNextSibling() ) {
            assert isElement( n ); // ↘ for reason
            switch( n.getLocalName()/* ≠ null, given the assertion above */) { // [NSC]
                case "AnchoredPrefix" -> {
                    final String tF = textChildFlat( n );
                    assert tF.length() == 2 && tF.charAt(0) == '^';
                    bP.append( switch( tF.charAt( 1 )) {
                        case '*' -> "^(?:    )*";
                        case '+' -> "^(?:    )*"+"[\u2500-\u259F].*?\\R(?:    )* {1,3}";
                        case '^' -> "^(?:    )*(?:[\u2500-\u259F].*?\\R(?:    )* {1,3})?";
                        default -> throw new IllegalStateException(); }); }
                case "Granum" -> {
                    final String tF = textChildFlat( n );
                    assert hasNoMetacharacter( tF, 0 );
                    append( tF, bP, toExpandSpaces ); }
                case "BackslashedSpecial" -> {
                    final String tF = textChildFlat( n );
                    final Matcher m = numberedCharacterBackslashMatcher.reset( tF );
                    if( m.matches() ) {
                        bP.append( "\\x{" );
                        bP.append( m.group( 1 ));
                        bP.append( '}' ); }
                    else bP.append( tF ); }
                case "Literalizer" -> {
                    bP.append( '\\' );      // The backslash part,
                    n = n.getNextSibling(); // and skipping past it.
                    assert hasName( "Granum", n ); /* Always that backslash is followed
                      directly by a `Granum` that starts with the literalized character. */
                    final String tF = textChildFlat( n );
                    bP.append( tF.charAt( 0 )); // The literalized character, plus
                    if( tF.length() > 1 ) {     // any remainder of the `Granum`.
                        assert hasNoMetacharacter( tF, 1 );
                        append( tF, 1, bP, toExpandSpaces ); }}
                case "Variable" -> append( (Element)n, bP, toExpandSpaces );
                default -> bP.append( textChildFlat( n )); }}
        return Pattern.compile( bP.toString(), flags ); }



    /** Offset within a variable interpolator of the first character of the variable name.
      */
    static final int variableName = 2;



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    /** @param c The offset in `seq` at which to start appending.
      */
    protected final void append( final CharSequence seq, int c, final StringBuilder b,
          final boolean toExpandSpaces ) {
        final int cN = seq.length();
        if( !toExpandSpaces ) {
            b.append( seq, c, cN );
            return; }
        final Matcher m = plainSpaceMatcher.reset( seq );
        if( m.lookingAt() ) {
            b.append( "(?: |\n|\r\n)+" );
            m.region( c = m.end(), cN ); }
        while( m.find() ) {
            b.append( seq, c, m.start() );
            b.append( "(?: |\n|\r\n)+" );
            c = m.end(); }
        if( c < cN ) b.append( seq, c, cN ); }



    protected final void append( CharSequence seq, StringBuilder b, boolean toExpandSpaces ) {
        append( seq, 0, b, toExpandSpaces ); }



    /** Appends to `b` the value of `variable`, or throws `FailedInterpolation`.
      * implementation recognizes no variables and simply throws `FailedInterpolation`.
      *
      *     @param variable The image of a variable interpolator.
      */
    protected void append( final Element variable, final StringBuilder b, final boolean toExpandSpaces )
          throws FailedInterpolation {
        throw new FailedInterpolation( variable, variableName, "No such variable in this context" ); }



    /** @param tF Flat text from the image of a regular-expression pattern.
      * @param c The offset in `tF` at which to start vetting.
      */
    private boolean hasNoMetacharacter( final String tF, int c ) {
        final int cEnd = tF.length();
        while( c < cEnd ) if( metacharacters.indexOf(tF.charAt(c++)) >= 0 ) return false;
        return true; }



    private final ImageMould<?> mould;



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



    /** A pattern to `find` a sequence of plain space characters.
      *
      *     @see java.util.regex.Matcher#find()
      */
    private static final Pattern plainSpacePattern = Pattern.compile( " +" );



    private final Matcher plainSpaceMatcher = plainSpacePattern.matcher( "" );



    private final StringBuilder stringBuilder = new StringBuilder(
      /*initial capacity*/0x800 ); } // = 2048



// NOTE
// ────
//   NSC  Presently ‘null in switch cases is a preview feature and is disabled by default’ (JDK 18),
//        else this code could be simplified.



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
