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
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.testing.api;


/**
 * A test configuration for a single-stripe cluster.
 */
public class BasicTestClusterConfiguration implements ITestClusterConfiguration {
  private final String name;
  public final int serversInStripe;
  
  public BasicTestClusterConfiguration(String name, int serversInStripe) {
    this.name = name;
    this.serversInStripe = serversInStripe;
  }

  @Override
  public String getName() {
    return this.name;
  }
}
