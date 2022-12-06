package Breccia.Web.imager;

import Java.GraphemeClusterCounter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import Java.Unhandled;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static Breccia.Web.imager.Project.zeroBased;
import static Breccia.Web.imager.ReferenceTranslation.newTranslation;
import static Java.IntralineCharacterPointer.markedLine;
import static java.lang.Float.parseFloat;
import static java.lang.System.err;
import static java.lang.System.getProperty;
import static java.nio.file.Files.readString;
import static Java.Paths.toPath;
import static Java.URI_References.enslash;
import static Java.URI_References.isRemote;
import static java.util.Collections.unmodifiableList;


/** @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#positional,argument,arguments'>
  *   Options for the `breccia-web-image` command</a>
  */
public class ImagingOptions extends Options {


    public ImagingOptions( String commandName ) { super( commandName ); } // [SLA]



    /** The home directory of the source author.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#author-home-,author-home-,path'>
      *         Command option `-author-home-directory`</a>
      */
    public final Path authorHomeDirectory() { return authorHomeDirectory; }



    /** The columnar offset on which to centre the text.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#centre-colum,centre-colum'>
      *         Command option `-centre-column`</a>
      */
    public final float centreColumn() { return centreColumn; }



    /** The enslashed name of the directory containing the auxiliary files of the Web image.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#co-service-d,co-service-d,reference'>
      *         Command option `-co-service-directory`</a>
      *     @see Java.Path#enslash(String)
      */
    public final String coServiceDirectory() { return coServiceDirectory; }



    /** The font file for glyph tests.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#glyph-test-f,glyph-test-f,path'>
      *         Command option `-glyph-test-font`</a>
      */
    public final String glyphTestFont() { return glyphTestFont; }



    /** List of occurences of the `-reference-mapping` option, each itself a list
      * of reference translations.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#reference-ma,reference-ma,translation'>
      *         Command option `-reference-mapping`</a>
      */
    public final List<List<ReferenceTranslation>> referenceMappings() { return referenceMappings; }



    /** Whether to run without effect.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#fakely'>
      *         Command option `-fakely`</a>
      */
    public final boolean toFake() { return toFake; }



    /** Whether to forcefully remake the Web image.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#forcefully'>
      *         Command option `-forcefully`</a>
      */
    public final boolean toForce() { return toForce; }



   // ━━━  O p t i o n s  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override void initialize( final List<String> args ) {
        super.initialize( args );
        if( authorHomeDirectory == null ) authorHomeDirectory = Path.of( getProperty( "user.home" ));
        if( glyphTestFont == null ) {
            if( !isRemote( coServiceDirectory )) {
                final Path styleSheet; {
                    final URI uCSD; { /* Expose any syntax error in the value of `--co-service-directory`
                          before complicating the error message by involving the style sheet. */
                        try { uCSD = new URI( coServiceDirectory ); }
                        catch( URISyntaxException x ) { throw new Unhandled( x ); }
                        toPath( uCSD ); } // May throw `IllegalArgumentException`.
                    styleSheet = toPath( uCSD.resolve( "Breccia/Web/imager/image.css" )); }
                glyphTestFont = glyphTestFont( styleSheet );
                if( glyphTestFont == null ) glyphTestFont = "none"; }
            else glyphTestFont = "none";
            out(2).println( "Glyph-test font: " + glyphTestFont ); }
        assert referenceMappings instanceof ArrayList; // Yet to be initialized, that is.
        referenceMappings = unmodifiableList( referenceMappings ); }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private Path authorHomeDirectory;



    private float centreColumn = 52.5f;



    private String coServiceDirectory = "http://reluk.ca/_/Web_service/";



    private String font = "FairfaxHD.ttf";



    private String glyphTestFont;



    /** @return The path of the designated glyph-test font, or null if none was found.
      */
    private static String glyphTestFont( final Path styleSheet ) {
        final Path directory = styleSheet.getParent();
        final String content; {
            try { content = readString( styleSheet ); }
            catch( IOException x ) { throw new Unhandled( x ); }}
        Matcher m;

      // Imported sheets, recursively searching these first
      // ───────────────
        m = importedStyleSheetPattern.matcher( content );
        while( m.find() ) {
            final String importedSheet = m.group( 2 );
            if( !isRemote( importedSheet )) {
                final String f = glyphTestFont( directory.resolve( importedSheet ));
                if( f != null ) return f; }}

      // Present sheet
      // ─────────────
        m = glyphTestFontSrcPattern.matcher( content );
        if( m.find() ) {
            final String src = m.group( 2 );
            if( !isRemote( src )) return directory.resolve(src).toString(); }
        return null; }



    /** A pattern to `find` in a style sheet the designated glyph-test font.  It captures as group (2)
      * the font reference, formally a URI reference.
      *
      *     @see java.util.regex.Matcher#find()
      *     @see <a href='https://www.w3.org/TR/css-fonts/#src-desc'>Font reference</a>
      *     @see <a href='https://www.rfc-editor.org/rfc/rfc3986#section-4.1'>
      *       URI generic syntax §4.1, URI reference</a>
      */
    private static final Pattern glyphTestFontSrcPattern = Pattern.compile(
      "(['\"])(\\S+?)\\1 */\\* *\\[GTF\\]" ); // As per note GTF in `image.css`.



    /** A pattern to `find` in a style sheet the import of another style sheet.  It captures as group (2)
      * the ‘URL of the style sheet to be imported’, formally a URI reference.
      *
      *     @see java.util.regex.Matcher#find()
      *     @see <a href='https://www.w3.org/TR/css-cascade/#at-import'>Importing style sheets</a>
      *     @see <a href='https://www.rfc-editor.org/rfc/rfc3986#section-4.1'>
      *       URI generic syntax §4.1, URI reference</a>
      */
    private static final Pattern importedStyleSheetPattern = Pattern.compile(
      "@import +(?:(?:url|src)\\()?(['\"])(.+?)\\1" );



    private List<List<ReferenceTranslation>> referenceMappings = new ArrayList<>(
      /*initial capacity*/4 );



    /** A pattern for `lookingAt` a reference translation within a `-reference-mapping` option.
      * It captures as group (2) the translation pattern, and group (3) the replacement string.
      *
      *     @see java.util.regex.Matcher#find()
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#reference-ma,reference-ma,translation'>
      *         Command option `-reference-mapping`</a>
      */
    private static final Pattern referenceTranslationPattern = Pattern.compile( "(.)(.+?)\\1(.+?)\\1" );



    private boolean toFake;



    private boolean toForce;



   // ━━━  O p t i o n s  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    protected @Override boolean initialize( final String arg ) {
        boolean isGo = true;
        String s;
        arg:if( arg.startsWith( s = "-author-home-directory=" )) {
            authorHomeDirectory = Path.of( value( arg, s )); }
        else if( arg.startsWith( s = "-centre-column=" )) centreColumn = parseFloat( value( arg, s ));
        else if( arg.startsWith( s = "-co-service-directory=" )) {
            coServiceDirectory = enslash( value( arg, s )); }
        else if( arg.equals( "-fakely" )) toFake = true;
        else if( arg.equals( "-forcefully" )) toForce = true;
        else if( arg.startsWith( s = "-glyph-test-font=" )) glyphTestFont = value( arg, s );
        else if( arg.startsWith( s = "-reference-mapping=" )) {
            final List<ReferenceTranslation> tt = new ArrayList<>( /*initial capacity*/8 );
            tt: {
                final String value = value( arg, s );
                final Matcher m = referenceTranslationPattern.matcher( value );
                final int vN = value.length();
                int v = 0;
                while( m.lookingAt() ) {
                    final Pattern pattern; {
                        try { pattern = Pattern.compile( m.group( 2 )); }
                        catch( final PatternSyntaxException x ) {
                            final int a = s.length() + m.start( 2 ) + zeroBased( x.getIndex() );
                            err.println( commandName + ": Malformed pattern: " + x.getDescription()
                              + '\n' + markedLine( "  ", arg, a, new GraphemeClusterCounter() ));
                            isGo = false;
                            break arg; }}
                    tt.add( newTranslation( pattern, /*replacement*/m.group(3) ));
                    v = m.end();
                    if( value.startsWith( "||"/*separator*/, v )) {
                        v += 2;
                        if( v == vN ) break tt; } // Trailing separator, a trivial violation.  Allow it.
                    else break;
                    m.region( v, vN ); }
                if( v < vN ) {
                    err.println( commandName + ": Malformed translation: \n"
                      + markedLine( "  ", arg, s.length() + v, new GraphemeClusterCounter() ));
                    isGo = false;
                    break arg; }
                else assert v == vN; }
            referenceMappings.add( unmodifiableList( tt )); }
        else isGo = super.initialize( arg );
        return isGo; }}



// NOTE
// ────
//   SLA  Source-launch access.  This member would have `protected` access were it not needed by
//        class `BrecciaWebImageCommand`.  Source launched and loaded by a separate class loader,
//        that class is treated at runtime as residing in a separate package.



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
