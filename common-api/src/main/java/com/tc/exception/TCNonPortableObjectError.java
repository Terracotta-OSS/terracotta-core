/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.exception;

/**
 * Indicates that an object cannot be made portable.  
 */
public class TCNonPortableObjectError extends TCError {
  
  public static final String NPOE_TROUBLE_SHOOTING_GUIDE = "http://www.terracotta.org/kit/reflector?kitID=default&pageID=npoe";

  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();
  
  public TCNonPortableObjectError(String message) {
    super(wrapper.wrap(message));
  }

}
