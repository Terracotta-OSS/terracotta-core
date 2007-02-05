/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.process;

/**
 * A <tt>StartupAppender</tt> implementation, if defined, will be invoked by the container subprocess just before the
 * container itself is started. This will allow control over last minute changes such as hardcoded port numbers in
 * config files.
 */
public interface StartupAppender {

  public static final String FILE_NAME              = "startup-appender.jar";
  public static final String APPENDER_TYPE_FILENAME = "appender-type";

  void append() throws Exception;
}
