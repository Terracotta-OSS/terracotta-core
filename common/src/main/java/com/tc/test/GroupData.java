/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.test;

public class GroupData {
  private final String   groupName;
  private int[]          tsaPorts;
  private final int[]    jmxPorts;
  private final int[]    tsaGroupPorts;
  private final String[] serverNames;

  public GroupData(int[] tsaPorts, int[] jmxPorts, int[] tsaGroupPorts, String[] serverNames) {
    this(null, tsaPorts, jmxPorts, tsaGroupPorts, serverNames);
  }

  public GroupData(String groupName, int[] tsaPorts, int[] jmxPorts, int[] tsaGroupPorts, String[] serverNames) {
    this.groupName = groupName;
    this.serverNames = serverNames;
    this.tsaPorts = tsaPorts;
    this.jmxPorts = jmxPorts;
    this.tsaGroupPorts = tsaGroupPorts;
  }

  public void setTsaPorts(int[] tsaPorts) {
    this.tsaPorts = tsaPorts;
  }

  public String getGroupName() {
    return this.groupName;
  }

  public int[] getTsaPorts() {
    return tsaPorts;
  }

  public int[] getJmxPorts() {
    return jmxPorts;
  }

  public int[] getTsaGroupPorts() {
    return tsaGroupPorts;
  }

  public String[] getServerNames() {
    return serverNames;
  }

  public int getServerCount() {
    return tsaPorts.length;
  }

}
