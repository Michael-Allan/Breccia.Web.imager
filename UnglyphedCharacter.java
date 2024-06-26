package Breccia.Web.imager;

import Java.CharacterPointer;

import static java.lang.Integer.toHexString;
import static Breccia.Web.imager.Project.mathBlockDelimiter;


/** The record of a character that has failed its glyph test.
  *
  *     @see <a href='http://reluk.ca/project/Breccia/Web/imager/bin/breccia-web-image.brec.xht#glyph-test-f,glyph-test-f,file'>
  *         Command option `-glyph-test-font`</a>
  */
class UnglyphedCharacter {


    /** @see #codePoint()
      * @see #fontName()
      */
    UnglyphedCharacter( String fontName, int codePoint, CharacterPointer pointer ) {
        this.fontName = fontName;
        this.codePoint = codePoint;
        this.pointer = pointer;
        isMathDelimiter = codePoint == mathBlockDelimiter; }



    /** The code point of the character.
      */
    final int codePoint;



    /** The number of occurrences of the character in the source file, initially zero.
      */
    public int count;



    /** The name of the font that has no glyph for the character.
      */
    final String fontName;



    /** A pointer to the first occurrence of the character in the source file.
      */
    final CharacterPointer pointer;



   // ━━━  O b j e c t  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━


    public @Override String toString() {
        final var b = new StringBuilder();
        if( isMathDelimiter ) b.append( "No `-math` option was given and " );
        b.append( fontName );
        b.append( " has no glyph for " );
        if( isMathDelimiter ) b.append( "math delimiter " );
        b.append( '‘' );
        b.appendCodePoint( codePoint );
        b.append( "’, code point " );
        b.append( toHexString(codePoint).toUpperCase() );
        b.append( '\n' );
        b.append( pointer.markedLine() );
        final int c = count - 1;
        if( c > 0 ) {
            b.append( "    (+" );
            b.append( c );
            b.append( " more)" ); }
        return b.toString(); }



////  P r i v a t e  ////////////////////////////////////////////////////////////////////////////////////



    private final boolean isMathDelimiter; }



                                                  // Copyright © 2022, 2024  Michael Allan.  Licence MIT.
