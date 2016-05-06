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

import org.terracotta.testing.api.ITestClusterConfiguration;
import org.terracotta.testing.api.ITestMaster;

import junit.framework.TestCase;


public abstract class AbstractHarnessTest<C extends ITestClusterConfiguration> extends TestCase {
  public abstract ITestMaster<C> getTestMaster();

  /**
   * Called when a test is finished, passing in an error, if there was one.
   * The implementation can return (implying this was a "pass"), or throw something (implying this was a "failure").
   * 
   * The default implementation merely re-throws the given error, assuming that most tests are looking for successful runs.
   * 
   * @param error An error describing the failure, or null if the test passed
   * @throws Throwable An exception thrown to describe the failure, not thrown on a pass
   */
  public void interpretResult(Throwable error) throws Throwable {
    if (null != error) {
      throw error;
    }
  }
}
