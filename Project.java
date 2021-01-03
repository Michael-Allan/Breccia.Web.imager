package Breccia.Web.imager;

import java.nio.file.Path;
import java.util.logging.Logger;


/** The present project.
  */
public final class Project {


    private Project() {}



    /** The logger proper to the present project.
      */
    static final Logger logger = Logger.getLogger( "Breccia.Web.imager" );



    /** The output directory of the present project.
      */
    public static final Path outDirectory = Path.of(
      System.getProperty("java.io.tmpdir"), "Breccia.Web.imager" ); }



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
