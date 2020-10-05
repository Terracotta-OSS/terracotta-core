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
package org.terracotta.testing.support;

import java.io.IOException;

import org.terracotta.testing.api.ITestMaster;
import org.terracotta.testing.api.BasicTestClusterConfiguration;
import org.terracotta.testing.logging.VerboseManager;
import org.terracotta.testing.master.BasicHarnessEntry;
import org.terracotta.testing.master.DebugOptions;
import org.terracotta.testing.master.EnvironmentOptions;
import org.terracotta.testing.master.GalvanFailureException;


/**
 * The JUnit harness runner class for all {@link BasicHarnessTest} tests.
 */
public class BasicHarnessRunner extends AbstractHarnessRunner<BasicTestClusterConfiguration> {
  public BasicHarnessRunner(Class<?> testClass) throws InstantiationException, IllegalAccessException {
    super(testClass);
  }

  @Override
  protected void runTest(EnvironmentOptions environmentOptions, ITestMaster<BasicTestClusterConfiguration> masterClass, DebugOptions debugOptions, VerboseManager verboseManager) throws IOException, GalvanFailureException {
    BasicHarnessEntry harness = new BasicHarnessEntry();
    harness.runTestHarness(environmentOptions, masterClass, debugOptions, verboseManager);
  }
}
