/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.runner;

/**
 * 
 */
public class TransparentAppCoordinator {
  private final String globalId;
  private final int    participantCount;

  public TransparentAppCoordinator(String globalId, int participantCount) {
    this.globalId = globalId;
    this.participantCount = participantCount;
  }

  public String getGlobalId() {
    return this.globalId;
  }

  public int getParticipantCount() {
    return this.participantCount;
  }
}