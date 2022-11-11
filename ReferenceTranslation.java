package Breccia.Web.imager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** @param isBounded Whether ‘${boundary}’ occured at the start of the replacement string.
  * @param replacement The replacement string, less any leading ‘${boundary}’.
  * @throws IllegalArgumentException If `isBounded` and `replacement` starts with ‘${boundary}’.
  * @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#reference-ma,reference-ma,translation'>
  *     Command option `--reference-mapping`</a>
  */
record ReferenceTranslation( Matcher matcher, String replacement, boolean isBounded ) {


    ReferenceTranslation {
        if( isBounded && replacement.startsWith( "${boundary}" )) throw new IllegalArgumentException(); }



    static ReferenceTranslation newTranslation( Pattern pattern, String replacement ) {
        final boolean isBounded; {
            if( replacement.startsWith( "${boundary}" )) {
                replacement = replacement.substring( "${boundary}".length() );
                isBounded = true; }
            else isBounded = false; }
        return new ReferenceTranslation( pattern.matcher(""), replacement, isBounded ); }}



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
