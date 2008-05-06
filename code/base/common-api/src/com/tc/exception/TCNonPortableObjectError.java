/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.exception;

/**
 * Indicates that an object cannot be made portable.  
 */
public class TCNonPortableObjectError extends TCError {

  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();
  
  public TCNonPortableObjectError(String message) {
    super(wrapper.wrap(message));
  }

}
