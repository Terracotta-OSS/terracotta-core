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
package org.terracotta.testing.master;


/**
 * Given that any part of the test may need to notify that the harness has failed, and the parts which pass may not be
 * aware of these other components, this interface allows abstract write-only interaction with the state of the running
 * test.
 * Note that the test will be considered a failure if any single part fails, no matter whether or not the pass was set.
 * This means that an unexpected server crash, after the test passed, would still count as a failure.
 */
public interface ITestStateManager {
  /**
   * Notify that the test did complete.  If no error was so far set, this will mark it as passed.
   */
  public void setTestDidPassIfNotFailed();

  /**
   * Notify that the test failed.  This will over-write any previous setting that the test had failed but will not
   *  over-write an existing failure (since we typically want to see the first cause of failure, not the last).
   * @param failureDescription The description of the test failure
   */
  public void testDidFail(GalvanFailureException failureDescription);
}
