package Breccia.Web.imager;

import Breccia.parser.ParseError;
import java.nio.file.Path;


public class ErrorAtFile extends Exception {


    /** @see #file
      * @see #getMessage()
      */
    public ErrorAtFile( Path file, String message ) { this( file, message, /*cause*/null ); }



    /** @see #file
      * @see #getMessage()
      * @see #getCause()
      */
    public ErrorAtFile( Path file, String message, Throwable cause ) {
        super( message, cause );
        this.file = file; }



    /** Makes a report head for an error in `file`.
      *
      *     @see #wrnHead(Path)
      */
    public static String errHead( final Path file ) { return file + ": error: "; }



    /** Makes a report head for an error in `file` at the given line number.
      *
      *     @see #wrnHead(Path,int)
      */
    public static String errHead( final Path file, final int lineNumber ) {
        return file + ":" + lineNumber + ": error: "; }



    /** Makes a report of an error in `file` at the given line number,
      * using `t.getMessage` as the message.
      *
      *     @see #wrnReport(Path,int,Throwable)
      */
    public static String errReport( final Path file, final int lineNumber, final Throwable t ) {
        return  errHead(file,lineNumber) + t.getMessage(); }



    /** Makes a report of a parse error associated with `file`.
      *
      *     @see #wrnReport(Path,ParseError)
      */
    public static String errReport( final Path file, final ParseError x ) {
        return errHead(file,x.lineNumber) + x.getMessage(); }



    /** Makes a report of an error at a file.
      *
      *     @see #wrnReport()
      */
    public static String errReport( final ErrorAtFile x ) { return errHead(x.file) + x.getMessage(); }



    /** The file associated with this error.
      */
    public final Path file;



    /** Makes a report head for warning of something in `file`.
      *
      *     @see #errHead(Path)
      */
    public static String wrnHead( final Path file ) { return file + ": warning: "; }



    /** Makes a report head for warning of something in `file` at the given line number.
      *
      *     @see #errHead(Path,int)
      */
    public static String wrnHead( final Path file, final int lineNumber ) {
        return file + ":" + lineNumber + ": warning: "; }



    /** Makes a report to warn of something in `file` at the given line number,
      * using `t.getMessage` as the message.
      *
      *     @see #errReport(Path,int,Throwable)
      */
    public static String wrnReport( final Path file, final int lineNumber, final Throwable t ) {
        return wrnHead(file,lineNumber) + t.getMessage(); }



    /** Makes a report to warn of a parse error associated with `file`.
      *
      *     @see #errReport(Path,ParseError)
      */
    public static String wrnReport( final Path file, final ParseError x ) {
        return wrnHead(file,x.lineNumber) + x.getMessage(); }



    /** Makes a report to warn of an error at a file.
      *
      *     @see #errReport()
      */
    public static String wrnReport( final ErrorAtFile x ) { return wrnHead(x.file) + x.getMessage(); }



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
            b.append( ": " );
            b.append( cause ); }
        return b.toString(); }}


                                                   // Copyright © 2021-2022  Michael Allan.  Licence MIT.
