/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
