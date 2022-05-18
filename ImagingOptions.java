package Breccia.Web.imager;

import java.util.List;

import static java.lang.System.err;
import static java.lang.System.exit;
import static Java.Paths.enslash;
import static Java.URI_References.isRemote;


public class ImagingOptions {


    /** Partly makes an instance for `initialize` to finish.
      *
      *     @see #commandName
      */
    public ImagingOptions( String commandName ) { this.commandName = commandName; } // [SLA]



    /** @param args Nominal arguments, aka options, from the command line.
      */
    public final void initialize( List<String> args ) { for( String a: args ) initialize( a ); }



    /** The columnar offset on which to centre the text.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec'>
      *         Command option `--centre-column`</a>
      */
    public final String centreColumn() { return centreColumn; }



    /** The enslashed name of the directory containing the auxiliary files of the Web image.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec'>
      *         Command option `--co-service-directory`</a>
      *     @see Java.Path.#enslash(String)
      */
    public final String coServiceDirectory() { return coServiceDirectory; }



    /** The path of the font file relative to the co-service directory.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec'>
      *         Command option `--font`</a>
      */
    public final String font() { return font; }



    /** The font file for glyph tests.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec'>
      *         Command option `--glyph-test-font`</a>
      */
    public final String glyphTestFont() {
        if( glyphTestFont != null ) return glyphTestFont;
        if( isRemote( coServiceDirectory )) return "none";
        return coServiceDirectory + font; }



    /** Whether to forcefully remake the Web image.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec'>
      *         Command option `--force`</a>
      */
    public final boolean toForce() { return toForce; }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private String centreColumn = "52.5";



    /** The name of the shell command that gave these options.
      */
    protected final String commandName;



    private String coServiceDirectory = "http://reluk.ca/_/Web_service/";



    private String font = "font/FairfaxHD.ttf";



    private String glyphTestFont;



    protected void initialize( final String arg ) {
        String s;
        if( arg.startsWith( s = "--centre-column" )) centreColumn = value( arg, s );
        else if( arg.startsWith( s = "--co-service-directory=" )) {
            coServiceDirectory = enslash( value( arg, s )); }
        else if( arg.startsWith( s = "--font" )) font = value( arg, s );
        else if( arg.startsWith( "--force" )) toForce = true;
        else if( arg.startsWith( s = "--glyph-test-font" )) glyphTestFont = value( arg, s );
        else {
            err.println( commandName + ": Unrecognized argument: " + arg );
            exit( 1 ); }}



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
