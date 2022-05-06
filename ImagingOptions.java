package Breccia.Web.imager;


public class ImagingOptions {


    /** The columnar offset on which to centre the text.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec'>
      *         command option `--centre-column`</a>
      */
    public String centreColumn = "52.5";



    /** The enslashed name of the directory containing the auxiliary files of the Web image.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec'>
      *         command option `--co-service-directory`</a>
      *     @see Java.Path.#enslash(String)
      */
    public String coServiceDirectory = "http://reluk.ca/_/Web_service/";



    /** Whether to forcefully remake the Web image.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec'>
      *         command option `--force`</a>
      */
    public boolean toForce; }



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
