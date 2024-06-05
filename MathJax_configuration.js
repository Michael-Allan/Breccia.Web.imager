/** MathJax configuration for Breccian Web images
  *
  * https://docs.mathjax.org/en/latest/web/start.html#configuring-mathjax
  * https://docs.mathjax.org/en/latest/options/
  */
'use strict'; // [SM]
console.assert( (eval('var _v = null'), typeof _v === 'undefined'), 'That strict mode is in effect' );
window.MathJax = {

  // input processor, https://docs.mathjax.org/en/latest/options/input/index.html
  // ┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈
    tex: { // https://docs.mathjax.org/en/latest/options/input/tex.html
        displayMath: [['･',      '･']], // Halfwidth katakana middle dot (FF65).
          // Changing?  Sync → `mathBlockDelimiter` in `Project.java`.
        inlineMath: [['\u2060', '\u2060']] }, // Word joiner (2060).

  // output processor, https://docs.mathjax.org/en/latest/options/output/index.html
  // ┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈
    chtml: { // https://docs.mathjax.org/en/latest/options/output/chtml.html
        displayAlign: 'left' }}; // So sharing the indent of parent element `MathDisplayBlock`.



// NOTE
// ────
//   SM · Strict mode.  https://262.ecma-international.org/6.0/#sec-strict-mode-code
//        The subsequent test is from Noseratio.  https://stackoverflow.com/a/18916788/2402790



                                                        // Copyright © 2024  Michael Allan.  Licence MIT.
