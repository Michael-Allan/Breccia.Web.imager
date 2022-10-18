package Breccia.Web.imager;

import java.io.IOException;
import java.nio.file.Path;
import Java.Unhandled;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Float.parseFloat;
import static java.lang.System.err;
import static java.nio.file.Files.readString;
import static Java.Paths.enslash;
import static Java.URI_References.isRemote;


/** @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#positional,argument,arguments'>
  *   Options for the `breccia-web-image` command</a>
  */
public class ImagingOptions extends Options {


    public ImagingOptions( String commandName ) { super( commandName ); } // [SLA]



    /** The columnar offset on which to centre the text.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#centre-colum,centre-colum'>
      *         Command option `--centre-column`</a>
      */
    public final float centreColumn() { return centreColumn; }



    /** The enslashed name of the directory containing the auxiliary files of the Web image.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#co-service-d,co-service-d,reference'>
      *         Command option `--co-service-directory`</a>
      *     @see Java.Path.#enslash(String)
      */
    public final String coServiceDirectory() { return coServiceDirectory; }



    /** The font file for glyph tests.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#glyph-test-f,glyph-test-f,path'>
      *         Command option `--glyph-test-font`</a>
      */
    public final String glyphTestFont() {
        if( glyphTestFont == null ) {
            if( !isRemote( coServiceDirectory )) {
                glyphTestFont = glyphTestFont( Path.of(
                  coServiceDirectory + "Breccia/Web/imager/image.css" ));
                if( glyphTestFont == null ) glyphTestFont = "none"; }
            else glyphTestFont = "none";
            out(2).println( "Glyph-test font: " + glyphTestFont ); }
        return glyphTestFont; }



    /** Whether to forcefully remake the Web image.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#force'>
      *         Command option `--force`</a>
      */
    public final boolean toForce() { return toForce; }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


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
      *     @see <a href='https://datatracker.ietf.org/doc/html/rfc3986#section-4.1'>URI reference</a>
      */
    private static final Pattern glyphTestFontSrcPattern = Pattern.compile(
      "(['\"])(\\S+?)\\1 */\\* *\\[GTF\\]" ); // As per note GTF in `image.css`.



    /** A pattern to `find` in a style sheet the import of another style sheet.  It captures as group (2)
      * the ‘URL of the style sheet to be imported’, formally a URI reference.
      *
      *     @see java.util.regex.Matcher#find()
      *     @see <a href='https://www.w3.org/TR/css-cascade/#at-import'>Importing style sheets</a>
      *     @see <a href='https://datatracker.ietf.org/doc/html/rfc3986#section-4.1'>URI reference</a>
      */
    private static final Pattern importedStyleSheetPattern = Pattern.compile(
      "@import +(?:(?:url|src)\\()?(['\"])(.+?)\\1" );



    private boolean toForce;



   // ━━━  O p t i o n s  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    protected @Override boolean initialize( final String arg ) {
        boolean isGo = true;
        String s;
        if( arg.startsWith( s = "--centre-column=" )) centreColumn = parseFloat( value( arg, s ));
        else if( arg.startsWith( s = "--co-service-directory=" )) {
            coServiceDirectory = enslash( value( arg, s )); }
        else if( arg.equals( "--force" )) toForce = true;
        else if( arg.startsWith( s = "--glyph-test-font=" )) glyphTestFont = value( arg, s );
        else isGo = super.initialize( arg );
        return isGo; }}



// NOTE
// ────
//   SLA  Source-launch access.  This member would have `protected` access were it not needed by
//        class `BrecciaWebImageCommand`.  Source launched and loaded by a separate class loader,
//        that class is treated at runtime as residing in a separate package.



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
