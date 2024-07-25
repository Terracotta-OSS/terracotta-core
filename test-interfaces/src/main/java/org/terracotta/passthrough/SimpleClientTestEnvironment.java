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
 * The simplest implementation of IClientTestEnvironment:  all data is passed in as read-only, at construction time.
 */
public class SimpleClientTestEnvironment implements IClientTestEnvironment {
  private final String clusterUri;
  private final int totalClientCount;
  private final int thisClientIndex;
  private final IClusterInfo clusterInfo;
  private final int numberOfStripes;
  private final int numberOfServersPerStripe;

  public SimpleClientTestEnvironment(String clusterUri, int totalClientCount, int thisClientIndex, IClusterInfo clusterInfo, int numberOfStripes, int numberOfServersPerStripe) {
    this.clusterUri = clusterUri;
    this.totalClientCount = totalClientCount;
    this.thisClientIndex = thisClientIndex;
    this.clusterInfo = clusterInfo;
    this.numberOfStripes = numberOfStripes;
    this.numberOfServersPerStripe = numberOfServersPerStripe;
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

  @Override
  public IClusterInfo getClusterInfo() {
    return this.clusterInfo;
  }

  @Override
  public int getNumberOfStripes() {
    return this.numberOfStripes;
  }

  @Override
  public int getNumberOfServersPerStripe() {
    return this.numberOfServersPerStripe;
  }
}
