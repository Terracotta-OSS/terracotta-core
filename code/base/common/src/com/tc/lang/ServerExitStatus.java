/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.lang;

public interface ServerExitStatus {
  /**
   * RMP-309 : Error code to convey auto-restart of TC server needed
   */
  public static final short EXITCODE_RESTART_REQUEST = 11;

  /**
   * Error codes for all other server problem
   */
  public static final short EXITCODE_STARTUP_ERROR   = 2;
}
