package Breccia.Web.imager;

import Breccia.parser.BrecciaCursor;
import Breccia.parser.BrecciaXCursor;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static Breccia.parser.BrecciaXCursor.EMPTY;
import static Breccia.parser.BrecciaXCursor.START_DOCUMENT;
import static Breccia.Web.imager.Project.logger;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.logging.Level.FINE;


public final class BrecciaHTMLTransformer implements FileTransformer {


   // ━━━  F i l e   T r a n s f o r m e r  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override void transform( Path sourceFile ) throws IOException {
        try( final InputStream byteSource = Files.newInputStream​( sourceFile );
             final InputStreamReader charSource = new InputStreamReader( byteSource, UTF_8 )) {
               // Cursor `in` does the buffering of `charSource` recommended by `InputStreamReader`.
               // The underlying `byteSource` needs none.  https://stackoverflow.com/a/27347262/2402790
            in.markupSource( charSource );
            final int t = in.getEventType();
            if( t == EMPTY ) {
                logger.log( FINE, "Imaging empty source file: {0}", sourceFile );
                final Path imageFile = sourceFile.resolveSibling( sourceFile.getFileName() + ".xht" );
                Files.deleteIfExists( imageFile );
                Files.createFile( imageFile );
                return; }
            assert t == START_DOCUMENT;
            for( ;; ) {
                // TODO, the actual transform.
                if( in.hasNext() ) in.next();
                else break; }}}



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private final BrecciaXCursor in = new BrecciaXCursor( new BrecciaCursor() ); }



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
