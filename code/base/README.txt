usage: tcbuild <target>

Available targets are:

BASICS

clean
    Removes everything the buildsystem can build

clean_tests
    Removes all testruns (results of tests, temp files, etc.)

resolve_dependencies
    Invokes Ivy to download external libraries and install them into the
    dependencies project.  The compile target depends on this target, and
    therefore so do any targets that depend on compile.  To bypass the
    dependency resolution process, pass the --no-ivy option anywhere on the
    tcbuild command-line.

compile
    Compiles all the code.  To override the JDK used for compiling, see the
    jdk= option described in JDK SELECTION below.

smart_compile <project-name>
    Compiles the project and all of its dependencies

show_modules
    Prints a list of all the modules the system knows about.

show_module_dependencies <module-name>
    Prints the names of all modules that the named module depends on.

show_dependent_modules <module-name>
    Prints the names of all modules that depend on the named module.

USEFUL OPTIONS

compile_only=dso-container-tests,dso-spring-tests
    Only compile specified projects

--no-ivy
    Bypass Ivy dependency resolution

--no-compile
    Don't compile

TESTING

NOTE: for all of the check targets described below, the debug=<port> option can
be used to run the test(s) using the JPDA/JDWP JVM debugging facilities.  The
<port> argument specifies the port on which the debug server will listen for
connections.  For example:

  tcbuild check_one AssertTest debug=8000

The test.mode=<mode> option can be used to set the transparent tests mode to
one of 'crash', 'active-passive', or 'normal' (the default).  For example:

  tcbuild check_one TransparentSetTest test.mode=active-passive

NOTE: To override the JDK used for testing, see the JDK SELECTION section below.

check
    Runs all tests

check check_modules=dso-container-tests
    Run all tests under module(s) (comma separated list)

check_one <test_name>
    Runs a single named test where <test_name> is the class
    name of the Java class containing the test to run.  This
    target will scan all modules to find the test.

    Example: tcbuild check_one AssertTest --no-ivy compile_only=common

pound_this <test_name>
    Runs the test up to 1000 times. It will stop at the first failure and email
    out the result if you pass in 'email=myemail@terracottatech.com'. Email option
    only works if you have 'mail' command in the PATH


check_<module-group>
check_<test-type>
check_<module-group>_<test-type>
    Runs all tests that match the given <module-group> and <test-type>.
    Module groups are defined in modules.def.yml.  <test-type> may be either
    'unit' or 'system'.  If <test-type> is not specified, both unit and system
    tests are included.

    Examples:
      Run all unit tests in all modules:
          tcbuild check_unit

      Run all tests for all modules that are part of the spring group:
          tcbuild check_spring

      Run system tests for all modules that are part of the framework group:
          tcbuild check_framework_system

show_classpath <project> <subtree>
    Show the classpath of the subtree in this project
    Ex: tcbuild show_classpath dso-container-tets tests.system

check_prep <module> <type>
    Prepare to run tests from an external tool (e.g. Eclipse).
    <module> may be the name of a module as specified in modules.def.yml, or
    it may be the keyword 'all'.
    <type> may be either 'unit', 'system', or 'all'.
    Both <module> and <type> default to 'all', so simply running check_prep
    with no arguments will prepare all test subtrees in all modules.

    Examples:

      Prepare all subtress in all modules:
          tcbuild check_prep

      Prepare unit and system tests in common module:
          tcbuild check_prep common

      Prepare unit tests for all modules, but not system tests:
          tcbuild check_prep all unit

check_file <file>
    Runs set of tests specified in <file>.  Each line of the <file>
    can be blank, a comment (starting with '#'), or the fully
    qualified class name of the Java class containing a test.

check_list
    Runs a set of tests for each test subtree, as specified in a
    file called '<modulename</tests.(unit|system).lists.<listname>'
    for each module.

check_short
    Runs 'check_list short' - in other words, runs all tests in
    files called '<modulename>/tests.unit.lists.short' and
    '<modulename>/tests.system.lists.short'.

recheck
    Reruns whichever tests failed on last test run.

show_test_results <test_run_name>
    Prints analysis of exactly what failed in the named test run.
    <test_run_name> is the name of a test run (e.g., 'testrun-0007')
    or just 'latest'.

    To extend the test timeout (default 15 minutes) use option
test_timeout=time_out_value_in_seconds

PACKAGING & DISTRIBUTION

NOTE: Output of binaries are placed under code/base/build/dist

--no-demo
    don't include demos while building kits

--no-external-resources
    don't include external resources (ehcache-core, quartz, etc) when building kits

--no-tims
    don't include TIMs (including express jars) when building kits

--no-extra
    don't include demos, TIMs and express products

--no-schema
    Skipping schema compilation

--no-no
    only build a bare kit and skip ivy also

maven.useLocalRepo=[true|false]
    If true, tcbuild will first look in the local Maven repository for Maven
    artifacts that are to be included in the kit.  The dist_dev target sets
    this variable to true automatically.

dist <product_code> <flavor> [maven.repo=URL] [kit.version=number]
    Create distribution binaries.
    <product_code> may be one of dso (the default), web, or api
    <flavor> may be either OPENSOURCE (the default) or ENTERPRISE.
    If provided, the maven.repo parameter specifies the URL of the Maven
    repository to which the generated binaries will be deployed.  The
    keyword 'local' can be used in place of a URL to specify the local
    Maven repository.  Maven must be installed to use this option.
    If the kit.version parameter is given, it will be appended to the
    kit name.  This parameter can also be specified in build-config.global.

dist_jars <product_code> <distribution_type>
    Acts like the dist target but will only build the jar files that will be found
    in a kit.

dist_dev <product_code> <flavor>
    Create a development kit using local versions of artifacts and external
    resources to populate the kit.  Specifically, this target will set
    maven.useLocalRepo=true and build all external projects before constructing
    the kit.  See build_external for further details about building external
    projects.

create_package <product_code> [kit.version=number]
    Assembles and packages the kit. Product codes: dso, web
    Default product_code is dso.
    If the kit.version parameter is given, it will be appended to the
    kit name.  This parameter can also be specified in build-config.global.

create_all_packages
    Assembles, packages, and publishes all possible kits, based on the
    configuration files found under the code/base/buildconfig/distribution
    directory.

patch [level=IDENTIFIER] [maven.repo=URL]
    Creates a patch tar file containing the files specified in patch.def.yml.
    The patch.def.yml file is a YAML file containing two keys: level and files.
    The files key must refer to an array of file names relative to the product
    directory.  The releasenotes.txt file is always auto-included into the patch.

    If specified, the 'level' command-line option overrides the level attribute
    from patch.def.yml.

    If the maven.repo parameter is given, then patched versions of the Terracotta
    Maven artifacts will be deployed to the specified repository URL.  As with
    other Maven commands, the maven.repositoryId parameter can also be used to
    link to a server/id element in the Maven settings.xml.

HOW TO CREATE A DEV PATCH

  A "dev" patch is usually built by engineers and has not been verified by QA

./tcbuild patch dso enterprise level=dev

    
DEPLOYING MAVEN ARTIFACTS

dist_maven [maven.repo=URL args...]
    Deploys all Terracotta Maven artifacts to the Maven repository.
    If the maven.repo option is specified, it must be a valid URL with a scheme
    that is understood by the Maven deploy plugin.  If maven.repo is not specified,
    it defaults to the local Maven repository.
    Other optional arguments include:
        maven.repositoryId: Link to a server/id element in the Maven settings.xml
        maven.version: Specify the version to apply to deployed artifacts (config
                modules excluded)
        maven.snapshot: If true, force all deployed artifacts to -SNAPSHOT versions

Config modules are deployed as part of the tcbuild 'compile' phase if and only
if the maven.repo argument is given.  Therefore, to deploy only config modules
without going through the full dist_maven process, just add maven.repo=local to
the tcbuild command line.  For example:

    # Deploy all config modules to the repo on 'myrepo' server
    tcbuild compile maven.repo=scp://myrepo/maven2

    # Deploy only the clustered-iBatis-2.2.0 module to the local repo
    tcbuild compile compile_only=clustered-iBatis-2.2.0 maven.repo=local

TIP: To quickly deploy the core artifacts without deploying all of the config
modules, first make sure that everything is fully compiled, and then run
dist_maven with the --no-ivy and --no-compile options:

    tcbuild compile  # skip this step if everything is already compiled
    tcbuild dist_maven --no-ivy --no-compile
    
dist_maven_ee
  Builds and installs EE artifacts. This only work if you have a EE branch checked out
  
dist_maven_all
  Builds and installs EE + OSS artifacts. This only work if you have a EE branch checked out

  
RUNNING SERVERS, CLASSES, ETC.

run_class <class_name> [args...]
    Runs the named class.  <class_name> must be a fully-qualified
    class name.  Any further arguments are passed to that class.

run_server
    Runs the Terracotta server. The 'jvmargs' parameter can be
    used to set JVM arguments for it, if you need any extra ones.

NOTE: The jdk= option, described in the JDK SELECTION section below, can be
used to specify the JDK used by these targets.

MISCELLANEOUS

build_external
    Build all external projects found in the external directory.
    See README-EXTERNAL.txt for details.

javadoc
    Generates Javadoc API documentation in build/doc/api.
    The set of modules to include is specified by the javadoc option in
    modules.def.yml

boot_jar_path=/path/to/your/boot/jar
   Specify this option if you want to run your test with your own bootjar

create_boot_jar <jvm_type>
    Creates a boot JAR file for <jvm_type>
    <jvm_type> can be '1.5', '1.6', or the fully-qualified path
    to a Java home.

show_config
    Prints the configuration parameters that the build system
    itself is building with.

generate_config_classes
    Generates XMLBeans against the Terracotta schema

findbugs
    Runs FindBugs analysis. The result find is saved to build/findbugs.xml.
    If you run 'tcbuild findbugs gui' it will run findbugs analysis first then start the GUI

findbugs_gui
    Starts FindBugs GUI. You can use this GUI to read the build/findbugs.xml
    
    
JDK SELECTION

You can override the default JDK used for compiling and/or testing by
specifying either the jdk= option or the tests-jdk= option.  The jdk= option
will force tcbuild to use the specified JDK for all tasks during the build,
including compiling, testing, building the bootjar, and running classes.
The tests-jdk= option tells tcbuild to use the specified JDK for executing
tests, while using the default JDK for everything else.

Both jdk= and tests-jdk= can be given either the name of a JDK specified in
jdk.def.yml, or the path to a Java installation.  For example:

  tests-jdk=1.5               # run tests using J2SE_15
  tests-jdk=1.6               # run tests using JAVASE_16
  jdk=/usr/local/jdk1.6.0     # do everything with JAVASE_16

You could skip JDK enforcement (using JDK16 in place of JDK15) by passing
skip.jdk.check=true to tcbuild, or put that option in file build-config.local  
  
SEE ALSO

Further documentation is available on the following wiki pages:

http://docs.terracotta.org/confluence/display/devdocs/Building+Terracotta
http://docs.terracotta.org/confluence/display/devdocs/Configuring+JDK+Versions
http://docs.terracotta.org/confluence/display/devdocs/tcbuild+Targets
http://docs.terracotta.org/confluence/display/devdocs/Source+Modules
