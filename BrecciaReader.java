package Breccia.Web.imager;

import java.nio.file.Path;


public class BrecciaReader { // Cf. `javax.xml.stream.XMLStreamReader`.


    /** @param _f The path of the Breccian file to read.
      */
    public BrecciaReader( Path _f ) {}



    /** Answers whether the read cursor can advance to the next parse state.
      *
      *     @see #next()
      */
    public boolean hasNext() { return false; }



    /** Advances the read cursor to the next parse state.
      */
    public void next() { throw new UnsupportedOperationException(); }} // Yet uncoded.



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
