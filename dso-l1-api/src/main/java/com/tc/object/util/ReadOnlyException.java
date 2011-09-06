/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.util;

import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;

/**
 * Indicates a read-only transaction is trying to access a shared object.  This is most likely 
 * a problem with an incorrect lock configuration.
 */
public class ReadOnlyException extends RuntimeException {
  
  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();
  
  /** Indicates a default invalid VM_ID to use */
  public static final long INVALID_VMID = -1;
  
  /**
   * @param message Message, which will be wrapped
   */
  protected ReadOnlyException(String message) {
    super(wrapper.wrap(message));
  }
  
  /**
   * @param message Message
   * @param threadName Thread name
   * @param vmId VM identifier
   */
  public ReadOnlyException(String message, String threadName, long vmId) {
    this(ReadOnlyException.createDisplayableString(message, threadName, vmId));
  }
  
  /**
   * @param message Message
   * @param threadName Thread name
   * @param vmId VM identifier
   * @param details Additional details
   */
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