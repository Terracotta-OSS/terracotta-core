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
package com.tc.test.config.model;

/**
 * Configuration for each mirror group in the test <br>
 * Default : <br>
 * members in each group : 1 <br>
 * election time : 10 secs <br>
 * 
 * @author rsingh
 */
public class GroupConfig {
  private int memberCount = 1;
  private int electionTime = 10;

  /**
   * @return number of servers in each mirror group
   */
  public int getMemberCount() {
    return memberCount;
  }

  /**
   * Sets the number of servers in each mirror group
   */
  public void setMemberCount(int memberCount) {
    this.memberCount = memberCount;
  }

  /**
   * @return election time in seconds
   */
  public int getElectionTime() {
    return electionTime;
  }

  /**
   * Sets the election time for each mirror group
   * @param electionTime time in seconds
   */
  public void setElectionTime(int electionTime) {
    this.electionTime = electionTime;
  }

}
