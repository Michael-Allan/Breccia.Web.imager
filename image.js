/* Javascript program for Breccian Web images
 */
'use strict'; // [SM]
console.assert( (eval('var _v = null'), typeof _v === 'undefined'), 'That strict mode is in effect' );
window.Breccia_Web_imager = ( function() {


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



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////


    /** @param e (ClipboardEvent)
      */
    function copy/*event listener*/( e ) {
        const selection = getSelection();
        let sourceText = '';
        identifyMath( document.body );                                                    // [◦↓◦]
        for( let r = 0, rN = selection.rangeCount; r < rN; ++r ) {
            sourceText += sourceTextOfClone( selection.getRangeAt(r).cloneContents() ); } // [◦↑◦]
        if( sourceText ) e.clipboardData.setData( 'text/plain', sourceText );
        e.preventDefault(); }



    /** @param n (Node)
      */
    function hasAttribute_nonOriginalText( n ) { // Changing?  Sync → `ImageNodes.java`.
        return isElement(n) && n.hasAttributeNS(nsImager,'nonOriginalText'); }



    /** Ensures `id` attribution of all mathematic images, namely `mjx-container` elements.
      *
      *     @param node (Node)
      */
    function identifyMath( node ) {
        for( let n = successor(node);  n !== null;  n = successor(n) ) {
            if( n.nodeName !== 'mjx-container' || n.id ) continue;
            n.id = nsImager + '/math.' + identifyMath_counter++; }}



    let identifyMath_counter = 0;



    /** @param n (Node)
      */
    function isElement( n ) { return n.nodeType === Node.ELEMENT_NODE; }



    /** The namespace name for Breccia Web Imager.
      */
    const nsImager = 'data:,Breccia/Web/imager';



    function run() { addEventListener( 'copy', copy ); }



    /** The original text content of the given node prior to Web imaging.
      *
      *     @param node (Node)
      */
    function sourceText( node ) { // Changing?  Sync → `ImageNodes.java`.
        if( hasAttribute_nonOriginalText( node )) return '';
        identifyMath( node );                                          // [◦↓◦]
        return sourceTextOfClone( node.cloneNode( /*deeply*/true )); } // [◦↑◦]



    /** The original text content of the given nodal clone prior to Web imaging.
      *
      *     @param node (Node) A clone of a node in whose original all instances of mathematics
      *       were (before cloning) identified.
      *      @see #identifyMath
      */
    function sourceTextOfClone( node ) { // Changing?  Sync → `ImageNodes.java`.
        for( let p, n = successor(p = node);  n !== null;  n = successor(p = n) ) {
            if( n.nodeName === 'mjx-container' ) { /* Then `n` is an math image swapped in by MathJax.
                  Replace it with its original source text, e.g. Tex expression. */
                const mm = MathJax.startup.document.getMathItemsWithin( document.getElementById( n.id ));
                  // https://docs.mathjax.org/en/latest/web/typeset.html#looking-up-the-math-on-the-page
                if( mm.length === 1 ) {
                    const m = mm[0];
                    n.parentNode.insertBefore(
                      document.createTextNode( m.start.delim + m.math/*source text*/ + m.end.delim ),
                      n );
                    n.parentNode.removeChild( n ); }
                else console.assert( false, 'Record of math image is retrievable' ); }
            else if( hasAttribute_nonOriginalText( n )) {
                n.parentNode.removeChild( n );
                n = p; }} // Resume from the predecessor of element `n`, now removed.
        return node.textContent; }



    /** Returns the successor of `node` in document order, including any first child,
      * or null if `node` has no successor.
      *
      *     @param node (Node)
      */
    function successor( node ) { // Changing?  Sync → `Java/Nodes.java`.
        let s = node.firstChild;
        if( !s ) s = successorAfter( node );
        return s; }



    /** Returns the exclusive successor of `node` in document order, or null if there is none.
      *
      *     @param node (Node)
      *     @return The first successor of `node` outside of its descendants, or null if none exists.
      */
    function successorAfter( node ) { // Changing?  Sync → `Java/Nodes.java`.
        let s = node.nextSibling;
        if( !s ) {
            const p = node.parentNode;
            if( p ) s = successorAfter( p ); }
        return s; }



////////////////////

    Object.freeze( εP );
    run();
    return εP; }() );



// NOTES
// ─────
//   ◦↓◦  Code that is order dependent with like-marked code (◦↕◦, ◦↑◦) that comes after.
//
//   ◦↑◦  Code that is order dependent with like-marked code (◦↓◦, ◦↕◦) that comes before.
//
//   DP · Defunct parameter.
//
//   SM · Strict mode.  https://262.ecma-international.org/6.0/#sec-strict-mode-code
//        The subsequent test is from Noseratio.  https://stackoverflow.com/a/18916788/2402790
//
//   WDL  One may use either `window.location` or `window.document.location`, the two are identical.
//        https://html.spec.whatwg.org/multipage/history.html#the-location-interface



                                                   // Copyright © 2022-2024  Michael Allan.  Licence MIT.
