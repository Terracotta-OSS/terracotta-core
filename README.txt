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
%> mvn verify -Dtest=ArrayCopyTest -P system-tests

You can skip the 'cd' step if you specify the pom of the sub module via '-f'

%> mvn verify -Dtest=ArrayCopyTest -P system-tests -f dso-system-tests/pom.xml

EXTRA TEST PROFILES:
* berkeleydb: run tests with bdb
* derby:      run tests with derby
* normal,active-passive:  see dso-crash-tests/README.txt

TO DEBUG A TEST:

%> mvn verify -Dtest=ArrayCopyTest -P system-tests -Dmaven.surefire.debug

The option -Dmaven.surefire.debug will run the test in debug mode where you 
connect Eclipse or other IDE to the test JVM via remote debug option.


******************************************************************************
3. To pound a test:
******************************************************************************

%> cd dso-system-tests
%> mvn verify -Dtest=ArrayCopyTest -P system-tests -DpoundTimes=5

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

%> mvn install -DskipTests -Pkit,enterprise,os -am -pl terracotta-kit

Note that for any dso-l1 changes, you will a need minimum of 
terracotta-toolkit-ee to be present in the devwork folder (as described by 
#7). 

******************************************************************************
6. To start/stop TC server from the build (not using kit)
******************************************************************************
under "deploy" module, run:

to start:
%> mvn exec:exec -P start-server &

to stop:
%> mvn exec:exec -P stop-server

******************************************************************************
7. To include other Forge projects in the build
******************************************************************************
For convenience, you can include other Forge projects into the main build to 
have them all compile in one go. To be able to do that, svn checkout those
projects under "community/devwork". List of projects currently recognize are:

- terracotta-toolkit(-ee)
- terracotta-ehcache(-ee)
- terracotta-quartz(-ee)
- tim-ehcache(-ee)
- tim-quartz(-ee)
- ehcache-core(-ee)
- quartz

Let say you have terracotta-toolkit-ee under devwork. This step will compile
core, terracotta-toolkit and terracotta-toolkit-ee all in one command:

%> mvn clean install -P os -DskipTests

The profile 'os' is recognized by terracotta-toolkit-ee to include terracotta-toolkit

******************************************************************************
8. Smart build: making the build a bit faster by specify what module was changed.
******************************************************************************

If you only change dso-l1, this is how you would make Maven only compile that module
and its dependents:

%> mvn install -amd -pl dso-l1

Notice the absence of "clean" step. Usually, you don't need to clean every time.

You can also build projects along with it's dependencies (reverse of the above):

%> mvn install -am -pl devwork/terracotta-toolkit-ee/toolkit-runtime-ee

This will build toolkit-runtime-ee and all it's direct and indirect dependencies.
Note that you need to specify the actual file path to toolkit-runtime-ee in order
for maven to find it.

******************************************************************************
9. Using Maven Shell
******************************************************************************

The Maven Shell, created by Sonatype, can also speed up builds to some degree,
since it preloads the entire Maven environment--including plugins--and keeps it
all loaded in memory. To use the Maven Shell, download it from here:

http://shell.sonatype.org/

You can then add the mvnsh command to your path and fire up the Maven Shell in
the root of the project. Thereafter you can issue standard Maven commands and
everything will work as before, except faster and with colorized output.

NOTE: Maven Shell is still somewhat buggy in the 1.0.1 release, so you should
experiment with it to see if it works for you.

