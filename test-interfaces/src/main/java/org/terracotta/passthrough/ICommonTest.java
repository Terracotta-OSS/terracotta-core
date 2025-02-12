/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.passthrough;


/**
 * This interface describes the common shape the user code within a single system test.
 * It exists to allow test implementations to be passed around, abstractly, by test harnesses.
 * NOTE:  The tests, themselves, are expected to be state-less as they may be accessed concurrently by different threads or
 * processes.
 */
public interface ICommonTest {
  /**
   * Called at the beginning of a test run, by a single thread or process, to prepare the server state for the test before
   * multiple threads or processes are started to run it.
   */
  public void runSetup(IClientTestEnvironment env, IClusterControl control) throws Throwable;

  /**
   * Called at the end of a test run, by a single thread or process, to clean up the server state now that the test has
   * completed.
   */
  public void runDestroy(IClientTestEnvironment env, IClusterControl control) throws Throwable;

  /**
   * Runs the actual test.  Note that this call is expected to have no side-effects within the receiver, as it may be called
   * by multiple threads or processes, concurrently.
   * The control is the only way side-effects should be realized (as the test obviously needs to interact with the server
   * and may way to control the cluster).
   */
  public void runTest(IClientTestEnvironment env, IClusterControl control) throws Throwable;
}
