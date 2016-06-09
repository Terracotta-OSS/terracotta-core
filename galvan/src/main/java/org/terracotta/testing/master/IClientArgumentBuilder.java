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

import java.util.List;


/**
 * An interface to describe the details of how to invoke the client process.
 * This exists to further decouple the core galvan framework from the details of how it is being used.
 */
public interface IClientArgumentBuilder {
  /**
   * @return The fully-qualified name of the main class to call in the client process
   */
  public String getMainClassName();

  /**
   * Creates the argument list for a "setup" client run.
   * 
   * @param connectUri The URI of the test stripe
   * @return The list of arguments to pass to the client main
   */
  public List<String> getArgumentsForSetupRun(String connectUri);

  /**
   * Creates the argument list for a regular "test" client run.
   * 
   * @param connectUri The URI of the test stripe
   * @param totalClientCount The total number of test clients being run, concurrently
   * @param thisClientIndex The index number of this client, within the set of concurrent test clients
   * @return The list of arguments to pass to the client main
   */
  public List<String> getArgumentsForTestRun(String connectUri, int totalClientCount, int thisClientIndex);

  /**
   * Creates the argument list for a "destroy" client run.
   * 
   * @param connectUri The URI of the test stripe
   * @return The list of arguments to pass to the client main
   */
  public List<String> getArgumentsForDestroyRun(String connectUri);
}