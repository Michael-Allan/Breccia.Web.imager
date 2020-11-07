package Breccia.Web.imager;

// Changes to this file immediately affect the next build.  Treat it as a build script.

import java.util.List;


/** The software builder proper to the present project.
  */
public final class Builder extends building.Makeshift.BuilderDefault<BuildTarget> {


    public Builder() { super( BuildTarget.class, "Breccia.Web.imager" ); }



    protected @Override List<String> javacArguments() { return List.of( "--enable-preview" ); }}
      // --enable-preview  Q.v. in `java_arguments`.  For sake of records.



                                                        // Copyright © 2020  Michael Allan.  Licence MIT.
