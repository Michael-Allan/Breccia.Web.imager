/* Javascript program for Breccian Web images
 */
'use strict'; // [SM]
console.assert( (eval('var _v = null'), typeof _v === 'undefined'), 'That strict mode is in effect' );
window.Breccia_Web_imager = ( function()
{

    const εP = {}; // Exports to the public interface of this program.



    /** Event handler for clicks on the self hyperlink of a body fractum.  It makes the hyperlink operate
      * as a toggle.  On each click, if the fractum is not the present target of the window location,
      * then this handler passes the click through to the hyperlink and so targets the fractum.
      * Otherwise it steals the click from the hyperlink and instead untargets the fractum.
      *
      *     @param click (Event)
      */
    εP.fractumSelfHyperlink_hearClick = function( click ) {
        let targetedID = location.hash;
        steal: if( targetedID.length > 1 ) {
            targetedID = targetedID.slice( 1 ); // Omitting the prefixed fragment symbol ‘#’.
            const fractum = ( ()=> { // The body fractum in which the self hyperlink is contained.
                let e = click.currentTarget; // The self hyperlink `a` element.
                for( ;; ) { // Find its containing fractum.
                    e = e.parentElement;
                    if( e === null || e.hasAttribute( 'typestamp'/*exclusive to fracta*/ )) {
                        return e; }}})();
            if( fractum === null ) {
                console.assert( false ); // There must be a containing fractum.
                break steal; }
            if( fractum === document.getElementById(targetedID) ) {
             // location.hash = ''; // Defragment the window location …
             //// (leaves the fragment delimiter hanging there uselessly, with nothing to delimit)
             // location = location.pathname + location.search; // Defragment the window location …
             //// (scrolls to page top)
                history.pushState( /*state*/null, ''/*[DP]*/, location.pathname + location.search );
                  // Defragment the window location [WDL], thus untargeting the body fractum.
                location.reload(); /* Sync the page styles with the new window location,
                  which `pushState` alone fails to do. */
                click.preventDefault(); }}}; // Steal the click.



    window.MathJax = { /* In case of imaging option `-math`.
          https://docs.mathjax.org/en/latest/options/index.html */

      // input processor, https://docs.mathjax.org/en/latest/options/input/index.html
      // ┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈
       tex: { // https://docs.mathjax.org/en/latest/options/input/tex.html
           displayMath: [['$$', '$$']], // The default includes also `['\\[', '\\]']`.
           inlineMath: [['\u2060', '\u2060']] }, // Word joiner (2060).

      // output processor, https://docs.mathjax.org/en/latest/options/output/index.html
      // ┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈
       chtml: { // https://docs.mathjax.org/en/latest/options/output/chtml.html
           displayAlign: 'left' }}; // So sharing the indent of parent element `MathDisplayBlock`.

    Object.freeze( εP );
    return εP;

}() );



// NOTES
// ─────
//   DP · Defunct parameter.
//
//   SM · Strict mode.  https://262.ecma-international.org/6.0/#sec-strict-mode-code
//        The subsequent test is from Noseratio.  https://stackoverflow.com/a/18916788/2402790
//
//   WDL  One may use either `window.location` or `window.document.location`, the two are identical.
//        https://html.spec.whatwg.org/multipage/history.html#the-location-interface



                                                   // Copyright © 2022-2024  Michael Allan.  Licence MIT.
