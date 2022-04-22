package Breccia.Web.imager;

import java.nio.file.Path;
import java.util.logging.Logger;

import static java.lang.System.getProperty;


/** The present project.
  */
public final class Project {


    private Project() {}



    /** The logger proper to the present project.
      */
    static final Logger logger = Logger.getLogger( "Breccia.Web.imager" );



    /** The output directory of the present project.
      */
    public static final Path outDirectory = Path.of( getProperty("java.io.tmpdir"),
      "Breccia.Web.imager_" + getProperty("user.name") ); }



                                                  // Copyright © 2020, 2022  Michael Allan.  Licence MIT.
