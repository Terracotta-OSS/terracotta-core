/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;

/**
 * @author steve
 */
public class UnlockedSharedObjectException extends RuntimeException {
  
  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();
  
  private UnlockedSharedObjectException() {
    super();
  }
  
  private UnlockedSharedObjectException(String message) {
    super(wrapper.wrap(message));
  }

  public UnlockedSharedObjectException(String message, String threadName, long vmId) {
    this(UnlockedSharedObjectException.createDisplayableString(message, threadName, vmId));
  }
  
  public UnlockedSharedObjectException(String message, String threadName, long vmId, String details) {
    this(UnlockedSharedObjectException.createDisplayableString(message, threadName, vmId) + "\n    " + details);
  }
  
  private static String createDisplayableString(String message, String threadName, long vmId) {
    return message + "\n\n    Caused by Thread: " + threadName + "  in  VM(" + vmId + ")";
  }
}