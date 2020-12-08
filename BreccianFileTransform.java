package Breccia.Web.imager;

import java.io.IOException;
import java.nio.file.Path;
import javax.xml.stream.XMLStreamWriter;


public final class BreccianFileTransform implements FileTransform {


    /** @param f The path of the Breccian source file to transform.
      */
    public BreccianFileTransform( final Path f ) { reader = new BrecciaReader( f ); }



   // ━━━  A u t o   C l o s e a b l e  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public void close() throws IOException {}



   // ━━━  F i l e   T r a n s f o r m  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public BrecciaReader input() { return reader; }



    public XMLStreamWriter output() { return null; }



    public MarkupTransformer transformer() { return transformer; }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private final BrecciaReader reader;



    private final MarkupTransformer transformer = new BrecciaToHTML(); }



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
