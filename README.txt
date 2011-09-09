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


******************************************************************************
6. To start/stop TC server from the build (not using kit)
******************************************************************************
under "deploy" module, run:

to start:
%> mvn exec:exec -P start-server &

to stop:
%> mvn exec:exec -P stop-server

******************************************************************************
7. To include terracotta-toolkit(-ee) in the build
******************************************************************************
Check out either or both "terracotta-toolkit" and "terracotta-toolkit-ee" under
"community/devwork". You have to create "devwork" folder manually since it's set
to be ignored in SVN. 





