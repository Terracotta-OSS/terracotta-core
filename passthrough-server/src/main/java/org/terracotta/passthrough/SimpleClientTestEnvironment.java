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
 * The simplest implementation of IClientTestEnvironment:  all data is passed in as read-only, at construction time.
 */
public class SimpleClientTestEnvironment implements IClientTestEnvironment {
  private final String clusterUri;
  private final int totalClientCount;
  private final int thisClientIndex;
  private final IClusterInfo clusterInfo;

  public SimpleClientTestEnvironment(String clusterUri, int totalClientCount, int thisClientIndex) {
    this(clusterUri, totalClientCount, thisClientIndex, null);
  }

  public SimpleClientTestEnvironment(String clusterUri, int totalClientCount, int thisClientIndex, IClusterInfo clusterInfo) {
    this.clusterUri = clusterUri;
    this.totalClientCount = totalClientCount;
    this.thisClientIndex = thisClientIndex;
    this.clusterInfo = clusterInfo;
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
}
