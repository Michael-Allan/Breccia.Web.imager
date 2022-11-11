package Breccia.Web.imager;

import java.io.PrintStream;
import java.util.List;

import static java.io.OutputStream.nullOutputStream;
import static java.lang.Integer.parseUnsignedInt;
import static java.lang.System.err;
import static java.lang.System.exit;


public abstract class Options {


    /** Partly makes an instance for `initialize` to finish.
      *
      *     @see #commandName
      */
    protected Options( String commandName ) { this.commandName = commandName; }



    /** Finishes making this instance.  If instead a fatal error is detected, then this method
      * prints an error message and exits the runtime with a non-zero status code.  Call once only.
      *
      *     @param args Nominal arguments, aka options, from the command line.
      */
    public void initialize( final List<String> args ) {
        boolean isGo = true;
        for( String a: args ) isGo &= initialize( a );
        if( !isGo ) exit( 1 ); }



    /** @see ImageMould#out(int)
      */
    public final PrintStream out( final int v ) { // [SLA]
        if( v != 1 && v != 2 ) throw new IllegalArgumentException();
        return v > verbosity() ? outNull : System.out; }



    /** The allowed amount of user feedback on the standard output stream.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#verbosity,verbosity-0-'>
      *         Command option `--verbosity`</a>
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#quietly'>
      *         Command option `--quietly`</a>
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#verbosely'>
      *         Command option `--verbosely`</a>
      */
    public final int verbosity() { return verbosity; }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    /** The name of the shell command that gave these options.
      */
    protected final String commandName;



    /** Parses and incorporates the given argument, or prints an error message and returns false.
      *
      *     @param arg A nominal argument from the command line.
      *     @return True if the argument was incorporated, false otherwise.
      */
    protected boolean initialize( final String arg ) {
        boolean isGo = true;
        String s;
        if( arg.equals( "--quietly" )) verbosity = 0;
        else if( arg.equals( "--verbosely" )) verbosity = 2;
        else if( arg.startsWith( s = "--verbosity=" )) {
            verbosity = parseUnsignedInt( value( arg, s ));
            if( verbosity < 0 || verbosity > 2 ) {
                err.println( commandName + ": Unrecognized verbosity level: " + verbosity );
                isGo = false; }}
        else {
            err.println( commandName + ": Unrecognized argument: " + arg );
            isGo = false; }
        return isGo; }



    private static final PrintStream outNull = new PrintStream( nullOutputStream(), /*autoFlush*/false,
      System.out.charset() ); // Re `static`: source code (JDK 17) suggests `PrintStream` is thread safe.



    /** @param arg A nominal argument, aka option.
      * @param prefix The leading name and equals sign, e.g. "foo=".
      */
    protected static String value( final String arg, final String prefix ) {
        return arg.substring( prefix.length() ); }



    private int verbosity = 1; }



// NOTE
// ────
//   SLA  Source-launch access.  This member would have `protected` access were it not needed by one
//        or more of the shell-command classes.  Source launched and loaded by separate class loaders,
//        those classes are treated at runtime as residing in a separate package.



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
