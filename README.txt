Note: 

a) always do step (1) at least once after a check out to get all the needed
artifacts from Maven repos.

b) Terracotta maven artifacts are created as part of "mvn install"
There is no separate step like "tcbuild dist_maven"


******************************************************************************
1. To compile the project
******************************************************************************
Under root or "community" folder (if it's EE checkout), run

%> mvn install    (tests are skipped by default)


******************************************************************************
2. To run tests:
******************************************************************************
You need to 'cd' into the sub module where the test resides:

%> cd dso-system-tests
%> mvn verify -Dtest=SomeTest -P system-tests

You can skip the 'cd' step if you specify the pom of the sub module via '-f'

%> mvn verify -Dtest=SomeTest -P system-tests -f dso-system-tests/pom.xml

TO DEBUG A TEST:

%> mvn verify -Dtest=SomeTest -P system-tests -Dmaven.surefire.debug

The option -Dmaven.surefire.debug will run the test in debug mode where you 
connect Eclipse or other IDE to the test JVM via remote debug option.


******************************************************************************
3. To pound a test:
******************************************************************************

%> cd dso-system-tests
%> mvn verify -Dtest=SomeTest -P system-tests -DpoundTimes=5

Pound that test 5 times.


******************************************************************************
4. To run check-short:
******************************************************************************
Under root or 'community' folder:

%> mvn verify -P check-short


******************************************************************************
5. To build kits:
******************************************************************************
Under "terracotta-kit" there's a README showing you 
how to build different types of Terracotta kits

You can also just initiate a kit build from the community folder. This will
also take the projects in the devwork folder into account. Simply run this
from the community folder:

%> mvn install -DskipTests -Pkit,enterprise,os,dist -am -pl terracotta-kit

******************************************************************************
6. To start/stop TC server and Dev Console from the build (not using kit)
******************************************************************************

  IMPORTANT: 
  
  - For Opensource TC server and Dev Console, go into "deploy" module
  - For Enterprise TC server and Dev Console, go into "ent-deploy" module

to start:
%> mvn exec:exec -P start-server &

to stop:
%> mvn exec:exec -P stop-server

run dev-console:
%> mvn exec:exec -P dev-console

******************************************************************************
7. To include other Forge projects in the build
******************************************************************************
For convenience, you can include other Forge projects into the main build to 
have them all compile in one go. To be able to do that, svn checkout those
projects under "community/devwork". List of projects currently recognize are:

- terracotta-toolkit-api
- ehcache(-ee)
- quartz(-ee)

Let say you have ehcache-ee under devwork. This step will compile
core, ehcache and ehcache-ee all in one command:

%> mvn clean install -P os -DskipTests

The profile 'os' is recognized by ehcache-ee to include ehcache

******************************************************************************
8. Smart build: making the build a bit faster by specify what module was changed.
******************************************************************************

If you only change dso-l1, this is how you would make Maven only compile that module
and its dependents:

%> mvn install -amd -pl dso-l1

Notice the absence of "clean" step. Usually, you don't need to clean every time.

You can also build projects along with it's dependencies (reverse of the above):

%> mvn install -am -pl toolkit-runtime

This will build toolkit-runtime and all it's direct and indirect dependencies.
Note that you need to specify the actual file path to toolkit-runtime in order
for maven to find it.

******************************************************************************
9. Working with 'devmode' and toolkit
******************************************************************************

Toolkit runtime jars take a long time to package due to processing of internal
jars. There is a "devmode" profile that would allow classes to be loaded directly
from target/classes of these internal modules instead of loading from runtime jars.

To use, do this at least once under "community"

%> mvn install -P devmode

Then if you change something in toolkit-express-impl

%> mvn install -P devmode -pl toolkit-express-impl -pl toolkit-runtime

Then you can run a system test under toolkit-system-tests

%> mvn verify -P system-test,devmode  -Dtest=SomeTest

If you change a class that goes into embbeded jars (ie. common or dso-l1 or ehcache)
you don't need to recompile toolkit-runtime project at all. Those classes are loaded
directly from there target/classes folder

NOTE: if you create a ${user.home}/.devmode file, you can then have devmode
turn on permamently and don't have to specify devmode profile every time

******************************************************************************
10. Working with 'devmode' and L2
******************************************************************************

Do this at least once. Compile with devmode on

%> mvn install -P devmode

Then run system test with devmode on

%> mvn verify -P system-test,devmode  -Dtest=SomeTest

You could change any classes that use by L2 in Eclipse and not having to recompile again.
Just rerun the test and your new class will be picked up from its target/classes folder.


