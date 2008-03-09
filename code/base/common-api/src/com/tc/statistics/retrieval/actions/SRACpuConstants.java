/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

public interface SRACpuConstants {
  public final static String ACTION_NAME = "cpu";

  public final static String DATA_NAME_COMBINED = ACTION_NAME + " combined";
  public final static String DATA_NAME_IDLE = ACTION_NAME + " idle";
  public final static String DATA_NAME_NICE = ACTION_NAME + " nice";
  public final static String DATA_NAME_SYS = ACTION_NAME + " sys";
  public final static String DATA_NAME_USER = ACTION_NAME + " user";
  public final static String DATA_NAME_WAIT = ACTION_NAME + " wait";
}
