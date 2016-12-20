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

import org.junit.Assert;
import org.terracotta.passthrough.IClientTestEnvironment;
import org.terracotta.passthrough.IClusterControl;


/**
 * This test tries to expose a failure case which we sometimes see in actual usage:
 * -clients A and B are running a test
 * -client A blocks on client B performing some action
 * -client B crashes before this action is taken
 * 
 * The Galvan framework needs to detect that a single client crashed and bring down the rest of the processes, in response.
 */
public class CrashAndHangClientsIT extends MultiProcessGalvanTest {
  @Override
  public int getClientsToStart() {
    return 2;
  }

  @Override
  public void interpretResult(Throwable error) throws Throwable {
    // We are expecting an error, so throw if it is missing.
    if (null == error) {
      throw new Exception("Test should have failed");
    }
  }

  @Override
  public void runTest(IClientTestEnvironment env, IClusterControl control) throws Throwable {
    int clientIndex = env.getThisClientIndex();
    System.out.println("Running client: " + clientIndex);
    if (0 == clientIndex) {
      // We will get index 0 to hang, since any sort of "walk the clients" solutions will get stuck on the earlier clients.
      synchronized(this) {
        wait();
      }
    } else {
      // Crash the other client (the framework should see this and bring down the test).
      Assert.fail();
    }
  }
}
