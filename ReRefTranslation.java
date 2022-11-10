package Breccia.Web.imager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** @param isBounded Whether ‘${boundary}’ occured at the start of the original replacement string,
  *   in which case ensure that `replacement` has been transformed accordingly.
  * @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht'>
  *     Command option `--re-ref`</a>
  */
record ReRefTranslation( Matcher matcher, String replacement, boolean isBounded ) {


    static ReRefTranslation newTranslation( Pattern pattern, String replacement ) {
        final boolean isBounded; {
            if( replacement.startsWith( "${boundary}" )) {
                replacement = replacement.substring( "${boundary}".length() );
                isBounded = true; }
            else isBounded = false; }
        return new ReRefTranslation( pattern.matcher(""), replacement, isBounded ); }}



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
