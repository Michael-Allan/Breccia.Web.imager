package Breccia.Web.imager;

import Breccia.parser.BrecciaReader;
import Breccia.parser.BrecciaXTranslator;
import java.nio.file.Path;


public final class BrecciaHTMLTransformer implements FileTransformer {


   // ━━━  F i l e   T r a n s f o r m e r  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override void transform( Path sourceFile ) {
        try( final BrecciaReader source = new BrecciaReader( sourceFile );
             final BrecciaXTranslator in = new BrecciaXTranslator( source ); ) {
            for( ;; ) {
                // TODO, the actual transform.
                if( in.hasNext() ) in.next();
                else break; }}}}



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
