package Breccia.Web.imager;

import Breccia.parser.BrecciaCursor;
import Breccia.parser.BrecciaXCursor;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;


public final class BrecciaHTMLTransformer implements FileTransformer {


   // ━━━  F i l e   T r a n s f o r m e r  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override void transform( Path sourceFile ) throws IOException {
        try( final InputStream byteSource = Files.newInputStream​( sourceFile );
             final InputStreamReader charSource = new InputStreamReader( byteSource, UTF_8 )) {
               // Cursor `in` does the buffering of `charSource` recommended by `InputStreamReader`.
               // The underlying `byteSource` needs none.  https://stackoverflow.com/a/27347262/2402790
            for( in.setMarkupSource( charSource );; ) {
                // TODO, the actual transform.
                if( in.hasNext() ) in.next();
                else break; }}}



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private final BrecciaXCursor in = new BrecciaXCursor( new BrecciaCursor() ); }



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
