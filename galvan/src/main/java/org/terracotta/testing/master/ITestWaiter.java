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
 * This interface allows read-only interaction with the test state.
 * 
 * This interface is used by both external consumers waiting on the test completion but also used by other parts of
 *  the state interlock mechanism in order to check if the pass/fail state has been set, yet.
 */
public interface ITestWaiter {
  /**
   * Waits until the test completes, as either a pass or a fail, blocking the calling thread.
   * 
   * Returns after the run, throwing an exception on failure.
   * 
   * @throws GalvanFailureException A description of the failure, if the test has failed
   */
  public void waitForFinish() throws GalvanFailureException;

  /**
   * Like waitForFinish() but it returns immediately:  true if we passed, false if still running, and throws on
   *  failure.
   * 
   * @return true if we passed, false if still running
   * @throws GalvanFailureException A description of the failure, if the test has failed
   */
  public boolean checkDidPass() throws GalvanFailureException;
}
