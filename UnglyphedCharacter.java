package Breccia.Web.imager;

import Java.CharacterPointer;

import static java.lang.Integer.toHexString;


/** The record of a character that has failed its glyph test.
  *
  *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec'>
  *         Command option `--glyph-test-font`</a>
  */
public class UnglyphedCharacter {


    /** @see #codePoint()
      * @see #fontName()
      */
    public UnglyphedCharacter( String fontName, final int codePoint, final CharacterPointer pointer ) {
        this.fontName = fontName;
        this.codePoint = codePoint;
        this.pointer = pointer; }



    /** The code point of the character.
      */
    public final int codePoint;



    /** The name of the font that has no glyph for the character.
      */
    public final String fontName;



    /** Indicant of where precisely the character occurs in the source file.
      */
    public final CharacterPointer pointer;



   // ━━━  O b j e c t  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override String toString() {
        final var b = new StringBuilder();
        b.append( fontName );
        b.append( " has no glyph for ‘" );
        b.appendCodePoint( codePoint );
        b.append( "’, code point " );
        b.append( toHexString( codePoint ));
        return b.toString(); }}



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
