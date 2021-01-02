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
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.newInputStream;


public final class BrecciaHTMLTransformer implements FileTransformer {


   // ━━━  F i l e   T r a n s f o r m e r  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override void transform( final Path sourceFile, final Path imageDirectory )
          throws IOException {
        try( final InputStream byteSource = newInputStream​( sourceFile );
             final InputStreamReader charSource = new InputStreamReader( byteSource, UTF_8 )) {
               // Cursor `in` does the buffering of `charSource` recommended by `InputStreamReader`.
               // The underlying `byteSource` needs none.  https://stackoverflow.com/a/27347262/2402790
            in.markupSource( charSource );
            final int t = in.getEventType();
            if( t == EMPTY ) {
                logger.fine( () -> "Imaging empty source file: " + sourceFile );
                final Path imageFile = imageDirectory.resolve( sourceFile.getFileName() + ".xht" );
                createFile( imageFile );
                return; }
            assert t == START_DOCUMENT;
            for( ;; ) {
                // TODO, the actual transform.
                if( in.hasNext() ) in.next();
                else break; }}}



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private final BrecciaXCursor in = new BrecciaXCursor( new BrecciaCursor() ); }



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.