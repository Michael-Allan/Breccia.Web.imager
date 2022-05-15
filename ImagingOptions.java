package Breccia.Web.imager;

import static Java.URI_References.isRemote;


public class ImagingOptions {


    /** The columnar offset on which to centre the text.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec'>
      *         Command option `--centre-column`</a>
      */
    public String centreColumn = "52.5";



    /** The enslashed name of the directory containing the auxiliary files of the Web image.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec'>
      *         Command option `--co-service-directory`</a>
      *     @see Java.Path.#enslash(String)
      */
    public String coServiceDirectory = "http://reluk.ca/_/Web_service/";



    /** The path of the font file relative to the co-service directory.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec'>
      *         Command option `--font`</a>
      */
    public String font = "font/FairfaxHD.ttf";



    /** The font file for glyph tests.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec'>
      *         Command option `--glyph-test-font`</a>
      */
    public final String glyphTestFont() {
        if( glyphTestFont != null ) return glyphTestFont;
        if( isRemote( coServiceDirectory )) return "none";
        return coServiceDirectory + font; }



    /** The font file for glyph tests, or null for the default.
      *
      *     @see #glyphTestFont()
      */
    public String glyphTestFont = null;



    /** Whether to forcefully remake the Web image.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec'>
      *         Command option `--force`</a>
      */
    public boolean toForce; }



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
