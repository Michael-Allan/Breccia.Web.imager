package Breccia.Web.imager;

import Breccia.parser.ParseError;
import java.nio.file.Path;


public class TransformError extends Exception {


    /** @see #file
      * @see #getMessage()
      */
    public TransformError( Path file, String message, Throwable cause ) {
        super( message, cause );
        this.file = file; }



    /** Makes a message head for reporting an error in `file`.
      *
      *     @see #wrnHead(Path)
      */
    public static String errHead( final Path file ) { return file + ": error: "; }



    /** Makes a message head for reporting an error in `file` at the given line number.
      *
      *     @see #wrnHead(Path,int)
      */
    public static String errHead( final Path file, final int lineNumber ) {
        return file + ":" + lineNumber + ": error: "; }



    /** Makes a message for reporting an error in `file` at the given line number,
      * taking for the body of the message `t.getMessage`.
      *
      *     @see #wrnMsg(Path,int,Throwable)
      */
    public static String errMsg( final Path file, final int lineNumber, final Throwable t ) {
        return  errHead(file,lineNumber) + t.getMessage(); }



    /** Makes a message for reporting a parse error associated with `file`.
      *
      *     @see #wrnMsg(Path,ParseError)
      */
    public static String errMsg( final Path file, final ParseError x ) {
        return errHead(file,x.lineNumber) + x.getMessage(); }



    /** Makes a message for reporting a transform error.
      *
      *     @see #wrnMsg()
      */
    public static String errMsg( final TransformError x ) { return errHead(x.file) + x.getMessage(); }



    /** The file associated with this error.
      */
    public final Path file;



    /** Makes a message head for warning about something in `file`.
      *
      *     @see #errHead(Path)
      */
    public static String wrnHead( final Path file ) { return file + ": warning: "; }



    /** Makes a message head for warning about something in `file` at the given line number.
      *
      *     @see #errHead(Path,int)
      */
    public static String wrnHead( final Path file, final int lineNumber ) {
        return file + ":" + lineNumber + ": warning: "; }



    /** Makes a message for warning about something in `file` at the given line number,
      * taking for the body of the message `t.getMessage`.
      *
      *     @see #errMsg(Path,int,Throwable)
      */
    public static String wrnMsg( final Path file, final int lineNumber, final Throwable t ) {
        return wrnHead(file,lineNumber) + t.getMessage(); }



    /** Makes a message for warning about a parse error associated with `file`.
      *
      *     @see #errMsg(Path,ParseError)
      */
    public static String wrnMsg( final Path file, final ParseError x ) {
        return wrnHead(file,x.lineNumber) + x.getMessage(); }



    /** Makes a message for warning about a transform error.
      *
      *     @see #errMsg()
      */
    public static String wrnMsg( final TransformError x ) { return wrnHead(x.file) + x.getMessage(); }



   // ━━━  T h r o w a b l e  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    /** {@inheritDoc} If the `{@linkplain #getCause cause}` is non-null, then
      * `cause.{@linkplain Throwable#toString() toString}()` is appended to the message.
      *
      *     @return The message, which is never null.
      */
    public String getMessage() {
        final StringBuilder b = new StringBuilder();
        b.append( super.getMessage() );
        final Throwable cause = getCause();
        if( cause != null ) {
            b.append( ':' );
            b.append( cause ); }
        return b.toString(); }}


                                                        // Copyright © 2021  Michael Allan.  Licence MIT.
