/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
 * Exposes some basic client-relevant data in an ICommonTest.
 */
public interface IClientTestEnvironment {
  /**
   * @return The URI of the cluster this client is supposed to interact with.
   */
  public String getClusterUri();

  /**
   * @return The number of clients participating in the test (always > 0).
   */
  public int getTotalClientCount();

  /**
   * @return The 0-indexed value uniquely specifying this client (always >= 0, < getTotalClientCount()).
   */
  public int getThisClientIndex();

  /**
   * @return {@link IClusterInfo} which provides information about connecting cluster
   */
  public IClusterInfo getClusterInfo();

  /**
   * @return The number of stripes in the current environment (always > 0).
   */
  public int getNumberOfStripes();

  /**
   * @return The number of servers within a given stripe in the current environment (always > 0).
   */
  public int getNumberOfServersPerStripe();
}
