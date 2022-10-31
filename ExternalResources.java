package Breccia.Web.imager;

import Java.Async;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

import static Java.Hashing.initialCapacity;


/** A record of external imaging resources, mapping each resource to the source files
  * whose images depend on it.  ‘External’ here means located outside of the source file.
  * The record comprises two maps, namely `{@linkplain #local local}` for the resources
  * that are file-system accessible, and `{@linkplain #remote remote}` for the resources
  * that are network accessible.  Map entries each comprise a resource reference (key)
  * mapped to the set (value) of source files whose images depend on that resource.
  *
  * <p>Each of the two maps is thread safe on condition of no concurrent structural modification,
  * structural modification being defined as for `{@linkplain HashMap HashMap}`
  * and `{@linkplain HashSet HashSet}`.</p>
  */
final @Async class ExternalResources {


    /** Ensures the given resource is mapped to its dependant, mapping it if necessary.
      *
      *     @param <R> The type of resource reference.
      *     @param map A resource map, either {@linkplain #local local} or {@linkplain #remote remote}.
      *     @param resource A resource reference.
      *     @param dependant A source file whose image depends on that resource.
      */
    static <R> void map( final HashMap<R,HashSet<Path>> map, final R resource, final Path dependant ) {
        HashSet<Path> dd = map.get( resource ); // Dependants of the resource.
        if( dd == null ) {
            dd = new HashSet<Path>();
            dd.add( dependant );
            map.put( resource, dd); }
        else dd.add( dependant ); }



    /** Map of external imaging resources that are file-system accessible.
      * Each entry comprises a normalized file path to an existent resource (key)
      * mapped to the set (value) of source files whose images depend on that resource. *//*
      *
      * Resource existence is required up front for sake of clarity of user feedback.  It allows broken
      * references (to non-existent resources) to be reported in the context of their source text
      * while that text is still easily accessible.
      */
    final HashMap<Path,HashSet<Path>> local = new HashMap<>( initialCapacity( 8192/*resources*/ ));



    /** Map of external imaging resources that are network accessible.  Each entry comprises
      * a normalized, unfragmented and apparently probeable URI reference to the resource (key)
      * mapped to the set (value) of source files whose images depend on that resource.
      *
      *     @see RemoteChangeProbe#looksProbeable(ava.net.URI)
      *     @see <a href='https://www.rfc-editor.org/rfc/rfc3986#section-4.1'>URI reference</a> *//*
      *
      * Apparent probeability is required up front for sake of clarity of user feedback.  It allows
      * improbeable references to be reported in the context of their source text while that text
      * is still easily accessible.
      */
    final HashMap<URI,HashSet<Path>> remote = new HashMap<>( initialCapacity( 8192/*resources*/ )); }



                                                   // Copyright © 2021-2022  Michael Allan.  Licence MIT.
