/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.exception;


/**
 * Thrown when someone tries to call an unimplemented feature.
 */
public class TCLogicalSubclassNotPortableException extends TCRuntimeException {
  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();

  private String className;
  private String superClassName;

  public TCLogicalSubclassNotPortableException(String className, String superClassName) {
    super(wrapper.wrap(className + " is a subclass of the logically managed superclass " + superClassName + ". It has contained structure that is currently not supported. Perhaps it has overridden a protected method." ));
    this.className = className;
    this.superClassName = superClassName;
  }

  public String getClassName() {
    return className;
  }

  public String getSuperClassName() {
    return superClassName;
  }
  
}