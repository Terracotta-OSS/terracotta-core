/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.exception;

public class TCNonPortableObjectError extends TCError {

  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();
  
  private TCNonPortableObjectError() {
    super();
  }

  public TCNonPortableObjectError(String message) {
    super(wrapper.wrap(message));
  }

}
