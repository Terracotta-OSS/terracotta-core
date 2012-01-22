package com.tc.test.config.model;

/**
 * Configuration for each mirror group in the test <br>
 * Default : <br>
 * members in each group : 1 <br>
 * election time : 5 secs <br>
 * @author rsingh
 * 
 */
public class GroupConfig {
  private int memberCount = 1;
  private int electionTime = 5;

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
   * @return elcection time in seconds
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
