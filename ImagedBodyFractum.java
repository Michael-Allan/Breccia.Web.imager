package Breccia.Web.imager;


/** @param xunc The offset in the source text of the body fractum,
  *   as per `Granum.{@linkplain Breccia.parser.Granum#xunc() xunc}`.
  * @param lineNumber The {@linkplain Breccia.parser.Granum#lineNumber() line number}
  *   of the body fractum.
  * @param identifier The `id` attribute of its image element.
  * @param xuncEnd The offset in the source text of the end of the body fractum.
  */
record ImagedBodyFractum( int xunc, int lineNumber, String identifier, int xuncEnd ) {}



                                                        // Copyright © 2022  Michael Allan.  Licence MIT.
