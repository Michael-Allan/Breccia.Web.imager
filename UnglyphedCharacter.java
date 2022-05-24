package Breccia.Web.imager;

import Java.CharacterPointer;

import static java.lang.Integer.toHexString;


/** The record of a character that has failed its glyph test.
  *
  *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht'>
  *         Command option `--glyph-test-font`</a>
  */
class UnglyphedCharacter {


    /** @see #codePoint()
      * @see #fontName()
      */
    UnglyphedCharacter( String fontName, int codePoint, CharacterPointer pointer ) {
        this.fontName = fontName;
        this.codePoint = codePoint;
        this.pointer = pointer; }



    /** The code point of the character.
      */
    final int codePoint;



    /** The number of occurences of the character in the source file, initially zero.
      */
    public int count;



    /** The name of the font that has no glyph for the character.
      */
    final String fontName;



    /** A pointer to the first occurence of the character in the source file.
      */
    final CharacterPointer pointer;



   // ━━━  O b j e c t  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override String toString() {
        final var b = new StringBuilder();
        b.append( fontName );
        b.append( " has no glyph for ‘" );
        b.appendCodePoint( codePoint );
        b.append( "’, code point " );
        b.append( toHexString( codePoint ));
        b.append( '\n' );
        b.append( pointer.markedLine() );
        final int c = count - 1;
        if( c > 0 ) {
            b.append( "    (+" );
            b.append( c );
            b.append( " more)" ); }
        return b.toString(); }}



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
