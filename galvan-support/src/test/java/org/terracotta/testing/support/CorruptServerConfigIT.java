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

import org.terracotta.passthrough.IClientTestEnvironment;
import org.terracotta.passthrough.IClusterControl;


/**
 * This test overrides the normal config options of the server, resulting in a corrupt config.  The server will fail to even
 * start.
 * 1 Client is started, who does nothing.
 */
public class CorruptServerConfigIT extends MultiProcessGalvanTest {
  @Override
  public int getClientsToStart() {
    return 1;
  }

  @Override
  public String getEntityConfigXMLSnippet() {
    return "Bogus<String<";
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
    System.out.println("Running in client: " + clientIndex);
  }
}
