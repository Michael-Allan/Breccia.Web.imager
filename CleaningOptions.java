package Breccia.Web.imager;


/** @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/web-image-clean.brec.xht#positional,argument,arguments'>
  *   Options for the `web-image-clean` command</a>
  */
public final class CleaningOptions extends Options {


    public CleaningOptions( String commandName ) { super( commandName ); } // [SLA]



    /** Whether to forcefully clean the Web image.
      *
      *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#forcefully'>
      *         Command option `--forcefully`</a>
      */
    public boolean toForce() { return toForce; }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    private boolean toForce;



   // ━━━  O p t i o n s  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    protected @Override boolean initialize( final String arg ) {
        boolean isGo = true;
        String s;
        if( arg.equals( "--forcefully" )) toForce = true;
        else isGo = super.initialize( arg );
        return isGo; }}



// NOTE
// ────
//   SLA  Source-launch access.  This member would have `protected` access were it not needed by
//        class `WebImageCleanCommand`.  Source launched and loaded by a separate class loader,
//        that class is treated at runtime as residing in a separate package.



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
