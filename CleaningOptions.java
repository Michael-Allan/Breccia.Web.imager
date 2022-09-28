package Breccia.Web.imager;

//import java.io.IOException;
//import java.io.PrintStream;
//import java.nio.file.Path;
//import Java.Unhandled;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;

//import static java.io.OutputStream.nullOutputStream;
//import static java.lang.Float.parseFloat;
//import static java.lang.Integer.parseUnsignedInt;
//import static java.lang.System.err;
//import static java.nio.file.Files.readString;
//import static Java.Paths.enslash;
//import static Java.URI_References.isRemote;


public final class CleaningOptions extends Options {


    public CleaningOptions( String commandName ) { super( commandName ); } // [SLA]



    /** Whether to forcefully clean the Web image.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/web-image-clean.brec.xht'>
      *         Command option `--force`</a>
      */
    public final boolean toForce() { return toForce; }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    /** Parses and incorporates the given argument, or prints an error message and returns false.
      *
      *     @param arg A nominal argument from the command line.
      *     @return True if the argument was incorporated, false otherwise.
      */
    protected boolean initialize( final String arg ) {
        boolean isGo = true;
        String s;
        if( arg.equals( "--force" )) toForce = true;
        else isGo = super.initialize( arg );
        return isGo; }



    private boolean toForce; }



// NOTE
// ────
//   SLA  Source-launch access.  This member would have `protected` access if access were not needed by
//        the `BrecciaWebImageCommand` class.  Source launched and loaded by a separate class loader,
//        that class is treated at runtime as residing in a separate package.



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
