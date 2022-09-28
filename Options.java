package Breccia.Web.imager;

import java.util.List;

import static java.lang.System.err;
import static java.lang.System.exit;


public abstract class Options {


    /** Partly makes an instance for `initialize` to finish.
      *
      *     @see #commandName
      */
    protected Options( String commandName ) { this.commandName = commandName; }



    /** Finishes making this instance.  If instead a fatal error is detected, then this method
      * prints an error message and exits the runtime with a non-zero status code.
      *
      *     @param args Nominal arguments, aka options, from the command line.
      */
    public final void initialize( List<String> args ) {
        boolean isGo = true;
        for( String a: args ) isGo &= initialize( a );
        if( !isGo ) exit( 1 ); }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    /** The name of the shell command that gave these options.
      */
    protected final String commandName;



    /** Parses and incorporates the given argument, or prints an error message and returns false.
      *
      * <p>The base implementation of `Options` simply prints
      *    the error message ‘Unrecognized argument’.</p>
      *
      *     @param arg A nominal argument from the command line.
      *     @return True if the argument was incorporated, false otherwise.
      */
    protected boolean initialize( final String arg ) {
        err.println( commandName + ": Unrecognized argument: " + arg );
        return false; }



    /** @param arg A nominal argument, aka option.
      * @param prefix The leading name and equals sign, e.g. "foo=".
      */
    protected static String value( final String arg, final String prefix ) {
        return arg.substring( prefix.length() ); } }



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
