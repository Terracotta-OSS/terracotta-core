/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.support;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.terracotta.testing.api.ITestClusterConfiguration;
import org.terracotta.testing.api.ITestMaster;
import org.terracotta.testing.common.Assert;
import org.terracotta.testing.master.DebugOptions;
import org.terracotta.testing.master.EnvironmentOptions;


public abstract class AbstractHarnessRunner<C extends ITestClusterConfiguration> extends Runner {
  private final AbstractHarnessTest<C> testCase;

  @SuppressWarnings("unchecked")
  public AbstractHarnessRunner(Class<?> testClass) throws InstantiationException, IllegalAccessException {
    // We will eagerly instantiate the test class in order to assume that it is the right type, later.
    this.testCase = AbstractHarnessTest.class.cast(testClass.newInstance());
  }


  @Override
  public Description getDescription() {
    return Description.createTestDescription(this.testCase.getClass(), this.testCase.getName());
  }

  @Override
  public void run(RunNotifier notifier) {
    Description testDescription = getDescription();
    notifier.fireTestStarted(testDescription);
    
    // Get the system properties we require.
    EnvironmentOptions environmentOptions = new EnvironmentOptions();
    environmentOptions.clientClassPath = System.getProperty("java.class.path");
    environmentOptions.serverInstallDirectory = System.getProperty("kitInstallationPath");
    environmentOptions.testParentDirectory = System.getProperty("kitTestDirectory");
    Assert.assertTrue(environmentOptions.isValid());
    
    // Get the test master implementation.
    ITestMaster<C> masterClass = this.testCase.getTestMaster();
    
    DebugOptions debugOptions = new DebugOptions();
    debugOptions.setupClientDebugPort = readIntProperty("setupClientDebugPort");
    debugOptions.destroyClientDebugPort = readIntProperty("destroyClientDebugPort");
    debugOptions.testClientDebugPortStart = readIntProperty("testClientDebugPortStart");
    debugOptions.serverDebugPortStart = readIntProperty("serverDebugPortStart");
    boolean enableVerbose = true;
    
    // We will only succeed or fail.
    Throwable error = null;
    try {
      boolean wasCompleteSuccess = runTest(environmentOptions, masterClass, debugOptions, enableVerbose);
      if (wasCompleteSuccess) {
        error = null;
      } else {
        // This was a failure without any other information so just create a generic exception.
        error = new Exception("Test failed without exception");
      }
    } catch (FileNotFoundException e) {
      error = e;
    } catch (IOException e) {
      error = e;
    } catch (InterruptedException e) {
      error = e;
    }
    // Determine how to handle the result.
    if (null == error) {
      // Success.
      notifier.fireTestFinished(testDescription);
    } else {
      // Failure.
      notifier.fireTestFailure(new Failure(testDescription, error));
    }
  }

  private int readIntProperty(String propertyName) {
    int result = 0;
    String value = System.getProperty(propertyName);
    if (null != value) {
      try {
        result = Integer.parseInt(value);
      } catch (NumberFormatException e) {
        // Means it wasn't a number so we will treat that like it wasn't there.
      }
    }
    return result;
  }

  protected abstract boolean runTest(EnvironmentOptions environmentOptions, ITestMaster<C> masterClass, DebugOptions debugOptions, boolean enableVerbose) throws IOException, FileNotFoundException, InterruptedException;
}
