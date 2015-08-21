/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.lang;

public interface ServerExitStatus {
  /**
   * RMP-309 : Error code to convey auto-restart of TC server needed
   */
  public static final short EXITCODE_RESTART_REQUEST = 11;

  /**
   * Error codes during the Server start
   */
  public static final short EXITCODE_STARTUP_ERROR   = 2;

  /**
   * Error code on other fatal condition
   */
  public static final short EXITCODE_FATAL_ERROR     = 3;
}
