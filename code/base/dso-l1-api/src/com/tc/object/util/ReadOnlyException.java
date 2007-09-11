/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.util;

import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;

public class ReadOnlyException extends RuntimeException {
  
  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();
  public static final long INVALID_VMID = -1;
  
  public ReadOnlyException() {
    super();
  }
  
  public ReadOnlyException(String message) {
    super(wrapper.wrap(message));
  }
  
  public ReadOnlyException(String message, String threadName, long vmId) {
    this(ReadOnlyException.createDisplayableString(message, threadName, vmId));
  }
  
  public ReadOnlyException(String message, String threadName, long vmId, String details) {
    this(ReadOnlyException.createDisplayableString(message, threadName, vmId) + "\n    " + details);
  }
  
  private static String createDisplayableString(String message, String threadName, long vmId) {
    if (vmId == INVALID_VMID) {
      return message + "\n\n    Caused by Thread: " + threadName;
    }
    return message + "\n\n    Caused by Thread: " + threadName + "  in  VM(" + vmId + ")";
  }
}