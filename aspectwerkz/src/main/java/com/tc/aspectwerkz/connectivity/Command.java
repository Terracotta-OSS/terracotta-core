/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.connectivity;

/**
 * Enum with all the commands for remote access.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class Command {
  public static final int CREATE = 0;

  public static final int INVOKE = 1;

  public static final int CLOSE = 2;
}