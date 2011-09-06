/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.runner;

import com.tc.util.concurrent.ThreadUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 
 */
public class TransparentAppCoordinator {
  private final String  globalId;
  private final int     participantCount;
  private final Map     participants = new HashMap();
  private final boolean print        = false;

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

  public void moveToStageAndWait(int stage) {
    try {
      moveToStage(stage);
      while (true) {
        if (allNodesInOrBeyondStage(stage)) return;
        ThreadUtil.reallySleep(1000);
        println("Waiting");
      }
    } finally {
      synchronized (participants) {
        println("Done waiting:" + participants + " stage: " + stage);
      }
    }
  }

  public void moveToStage(int stage) {
    synchronized (participants) {
      participants.put("" + globalId, new Integer(stage));
    }
  }

  public boolean allNodesInOrBeyondStage(int stage) {
    synchronized (participants) {
      println("participants: " + participants + ", participantCount: " + this.participantCount);
      if (participants.size() != this.participantCount) return false;
      for (Iterator i = participants.keySet().iterator(); i.hasNext();) {
        int testStage = ((Integer) participants.get(i.next())).intValue();
        if (testStage < stage) return false;
      }
    }
    return true;
  }

  private void println(String line) {
    if (print) {
      System.out.println(Thread.currentThread() + ": " + line);
    }
  }
}