/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;

/**
 * Thrown when there is an attempt to access a shared object outside the scope of a shared lock.
 */
public class UnlockedSharedObjectException extends RuntimeException {

  public static final String            TROUBLE_SHOOTING_GUIDE = "http://www.terracotta.org/kit/reflector?kitID=default&pageID=usoe";

  private static final ExceptionWrapper wrapper                = new ExceptionWrapperImpl();

  private UnlockedSharedObjectException(final String message) {
    super(wrapper.wrap(message));
  }

  public UnlockedSharedObjectException(final String message, final String threadName, final String vmId) {
    this(UnlockedSharedObjectException.createDisplayableString(message, threadName, vmId));
  }

  public UnlockedSharedObjectException(final String message, final String threadName, final long vmId) {
    this(UnlockedSharedObjectException.createDisplayableString(message, threadName, Long.toString(vmId)));
  }

  public UnlockedSharedObjectException(final String message, final String threadName, final long vmId,
                                       final String details) {
    this(UnlockedSharedObjectException.createDisplayableString(message, threadName, Long.toString(vmId)) + "\n"
         + details);
  }

  private static String createDisplayableString(final String message, final String threadName, final String vmId) {
    return message + "\n\nCaused by Thread: " + threadName + " in VM(" + vmId + ")";
  }
}