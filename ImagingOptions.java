package Breccia.Web.imager;

import java.io.IOException;
import java.nio.file.Path;
import Java.Unhandled;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.err;
import static java.lang.System.exit;
import static java.nio.file.Files.readString;
import static Java.Paths.enslash;
import static Java.URI_References.isRemote;


public class ImagingOptions {


    /** Partly makes an instance for `initialize` to finish.
      *
      *     @see #commandName
      */
    public ImagingOptions( String commandName ) { this.commandName = commandName; } // [SLA]



    /** Finishes making this instance.  If instead a fatal error is detected, then this method
      * prints an error message and exits the runtime with a non-zero status code.
      *
      *     @param args Nominal arguments, aka options, from the command line.
      */
    public final void initialize( List<String> args ) {
        boolean isGo = true;
        for( String a: args ) isGo &= initialize( a );
        if( !isGo ) exit( 1 ); }



    /** The columnar offset on which to centre the text.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht'>
      *         Command option `--centre-column`</a>
      */
    public final String centreColumn() { return centreColumn; }



    /** The enslashed name of the directory containing the auxiliary files of the Web image.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht'>
      *         Command option `--co-service-directory`</a>
      *     @see Java.Path.#enslash(String)
      */
    public final String coServiceDirectory() { return coServiceDirectory; }



    /** The font file for glyph tests.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht'>
      *         Command option `--glyph-test-font`</a>
      */
    public final String glyphTestFont() {
        if( glyphTestFont == null ) {
            final String styleSheet = coServiceDirectory + "Breccia/Web/imager/image.css";
            if( !isRemote( styleSheet )) {
                glyphTestFont = glyphTestFont( Path.of( styleSheet ));
                if( glyphTestFont == null ) glyphTestFont = "none"; }
            else glyphTestFont = "none"; }
        return glyphTestFont; }



    /** Whether to forcefully remake the Web image.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht'>
      *         Command option `--force`</a>
      */
    public final boolean toForce() { return toForce; }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private String centreColumn = "52.5";



    /** The name of the shell command that gave these options.
      */
    protected final String commandName;



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
      "src: +(?:(?:url|src)\\()?(['\"])(.+?)\\1 */\\* *\\[GTF\\]" );



    /** A pattern to `find` in a style sheet the import of another style sheet.  It captures as group (2)
      * the ‘URL of the style sheet to be imported’, formally a URI reference.
      *
      *     @see java.util.regex.Matcher#find()
      *     @see <a href='https://www.w3.org/TR/css-cascade/#at-import'>Importing style sheets</a>
      *     @see <a href='https://datatracker.ietf.org/doc/html/rfc3986#section-4.1'>URI reference</a>
      */
    private static final Pattern importedStyleSheetPattern = Pattern.compile(
      "@import +(?:(?:url|src)\\()?(['\"])(.+?)\\1" );



    /** Parses and incorporates the given argument, or prints an error message and returns false.
      *
      *     @param arg A nominal argument from the command line.
      *     @return True if the argument was incorporated, false otherwise.
      */
    protected boolean initialize( final String arg ) {
        boolean isGo = true;
        String s;
        if( arg.startsWith( s = "--centre-column=" )) centreColumn = value( arg, s );
        else if( arg.startsWith( s = "--co-service-directory=" )) {
            coServiceDirectory = enslash( value( arg, s )); }
        else if( arg.equals( "--force" )) toForce = true;
        else if( arg.startsWith( s = "--glyph-test-font=" )) glyphTestFont = value( arg, s );
        else {
            err.println( commandName + ": Unrecognized argument: " + arg );
            isGo = false; }
        return isGo; }



    private boolean toForce;



    /** @param arg A nominal argument, aka option.
      * @param prefix The leading name and equals sign, e.g. "foo=".
      */
    protected static String value( final String arg, final String prefix ) {
        return arg.substring( prefix.length() ); }}



// NOTE
// ────
//   SLA  Source-launch access.  This member would have `protected` access if access were not needed by
//        the `BrecciaWebImageCommand` class.  Source launched and loaded by a separate class loader,
//        that class is treated at runtime as residing in a separate package.



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
