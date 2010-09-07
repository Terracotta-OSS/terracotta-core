This README describes tcbuild's support for building external projects.

The foundation for external projects support is the "external" directory in
the code/base directory.  tcbuild will attempt to automatically build and
package any external projects that it finds in this directory.  However,
because the external projects do not follow the tcbuild directory structure,
and because they are not registered in the modules.def.yml file, some
conventions must be adhered to in order for tcbuild to be able to work its
magic.

To have tcbuild automatically build and package an external project, simply
checkout the project as a subdirectory of the external directory.  For each
subdirectory in external, tcbuild will look for a build file that it can use
to build the project.  In order of preference, tcbuild will look for one of
the following build files:

 1) build.sh
 2) build.rb
 3) pom.xml
 4) build.xml

tcbuild will stop the search at the first build file that it finds.

If there are multiple subdirectories in external, then tcbuild will look for a
build file in each subdirectory.  However, in this case the order in which the
projects are built is somewhat arbitrary: it is based on the sort order of the
subdirectory names.  To gain some control over the build order, there are two
options.  First, you can use a numeric prefix on the subdirectory names such
that they sort in the correct order.  For example, you could have the
following subdirectories checked out into external:

 01_ehcache-core
 02_terracotta-toolkit
 03_tim-ehcache
 04_terracotta-ehcache

With this set of subdirectories, tcbuild would build the external projects in
the correct order according to the numeric prefix.

The second approach is more intricate, but also more powerful.  To gain full
control over how the external projects are built, you can place a build file
directly into the external directory itself.  This build file could be any one
of the four mentioned above, and serves as an aggregate build file for all of
the external projects.  If tcbuild finds an aggregate build file in the
external directory it will use that build file to build all of the sub
projects in one pass, and will not invoke the build files of the individual
projects. An aggregate build file could, for example, be a build.sh or
build.rb file that invokes the build file for each individual project, or
could be a pom.xml file that declares the individual projects as modules.
(Note that tcbuild will not pass any arguments or define any properties when
it builds external projects, so if anything like this is required then you
will need to use an aggregate build file that does so as appropriate.)

Because external projects will typically rely on the Maven artifacts produced
by tcbuild, they are not built until after said Maven artifacts have been
constructed and deployed.  tcbuild invokes the build files with no arguments,
except in the case of pom.xml files, for which tcbuild passes 'install' as the
sole argument (as in "mvn install").

To allow multiple developers to share a set of external projects, it is
possible to use svn:externals to aggregate a group of projects together in a
single Subversion directory.  This aggregate directory can then be checked out
directly into the external directory to enable tcbuild integration.
For example:

  svn co $MY_AGGREGATE external

This command would populate the external directory with all of the projects
aggregated by the $MY_AGGREGATE directory.

