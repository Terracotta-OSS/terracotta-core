/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.testing.master;

import java.io.IOException;

import org.terracotta.testing.api.ITestMaster;
import org.terracotta.testing.api.BasicTestClusterConfiguration;
import org.terracotta.testing.logging.VerboseManager;


/**
 * The main class for tests running as a basic stand-alone test (running the harness directly, not part of JUnit).
 * 
 * While not typically used, it does persist as an example of how to run a single test on its own.
 */
public class BasicHarnessMain {
  public static void main(String[] args) throws InterruptedException, IOException {
    // Parse required command-line args.
    EnvironmentOptions environmentOptions = CommandLineSupport.parseEnvironmentOptions(args);
    String parsedMasterClass = CommandLineSupport.parseTestMasterClass(args);
    
    if (environmentOptions.isValid() && (null != parsedMasterClass)) {
      // Determine if any debug options were given (these are all optional).
      DebugOptions debugOptions = CommandLineSupport.parseDebugOptions(args);
      VerboseManager verboseManager = CommandLineSupport.parseVerbose(args);
      // Get the master given by the user.
      ITestMaster<BasicTestClusterConfiguration> master = null;
      try {
        master = CommandLineSupport.loadMaster(parsedMasterClass);
      } catch (Exception e) {
        System.err.println("FATAL: ITestMaster \"" + parsedMasterClass + "\" could not be used: " + e.getLocalizedMessage());
        e.printStackTrace();
        System.exit(1);
      }
      try {
        BasicHarnessEntry harness = new BasicHarnessEntry();
        harness.runTestHarness(environmentOptions, master, debugOptions, verboseManager);
        System.out.println("TEST RUN SUCCESSFUL!");
      } catch (GalvanFailureException e) {
        System.out.println("TEST FAILED! " + e.getLocalizedMessage());
        e.printStackTrace();
        System.exit(2);
      }
    } else {
      System.err.println(CommandLineSupport.getUsageString());
      System.exit(1);
    }
  }
}
