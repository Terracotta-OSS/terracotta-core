/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
}
