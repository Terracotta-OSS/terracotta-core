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
 * This interface exposes the abstract ability for different parts of the harness to register for shutdown treatment but
 * also set the state of entire test run to pass or fail.
 * Note that the test will be considered a failure if any single part fails, no matter whether or not the pass was set.
 * This means that an unexpected server crash, after the test passed, would still count as a failure.
 */
public interface ITestStateManager {
  public void testDidPass();

  public void testDidFail();

  /**
   * Registers a component which needs to be shut down when the test is complete.
   * 
   * @param componentManager The component to shut down.
   * @param shouldPrepend True if this component should be shutdown "first", false if it can be shut down "last"
   */
  public void addComponentToShutDown(IComponentManager componentManager, boolean shouldPrepend);
}
