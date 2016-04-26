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
package org.terracotta.testing.client;

import org.terracotta.passthrough.IClientTestEnvironment;


public class ClientTestEnvironment implements IClientTestEnvironment {
  private final String clusterUri;
  private final int totalClientCount;
  private final int thisClientIndex;

  public ClientTestEnvironment(String clusterUri, int totalClientCount, int thisClientIndex) {
    this.clusterUri = clusterUri;
    this.totalClientCount = totalClientCount;
    this.thisClientIndex = thisClientIndex;
  }

  @Override
  public String getClusterUri() {
    return this.clusterUri;
  }

  @Override
  public int getTotalClientCount() {
    return this.totalClientCount;
  }

  @Override
  public int getThisClientIndex() {
    return this.thisClientIndex;
  }
}
