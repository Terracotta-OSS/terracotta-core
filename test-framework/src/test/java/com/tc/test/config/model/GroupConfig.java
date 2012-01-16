package com.tc.test.config.model;

public class GroupConfig {
  private int memberCount  = 1;
  private int electionTime = 5;

  public int getMemberCount() {
    return memberCount;
  }

  public void setMemberCount(int memberCount) {
    this.memberCount = memberCount;
  }

  public int getElectionTime() {
    return electionTime;
  }

  public void setElectionTime(int electionTime) {
    this.electionTime = electionTime;
  }

}
