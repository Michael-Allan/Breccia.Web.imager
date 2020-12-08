package Breccia.Web.imager;

import java.io.IOException;
import javax.xml.stream.XMLStreamWriter;


public interface FileTransform extends AutoCloseable {


    public BrecciaReader input();



    public XMLStreamWriter output();



    public MarkupTransformer transformer();



   // ━━━  A u t o   C l o s e a b l e  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public void close() throws IOException; }



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
