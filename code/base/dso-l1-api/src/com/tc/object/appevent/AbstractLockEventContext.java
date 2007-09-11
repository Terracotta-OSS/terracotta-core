/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.appevent;

public abstract class AbstractLockEventContext extends AbstractApplicationEventContext {

  private static final long serialVersionUID = 4788562594133534828L;

  private final String      className;
  private final String      fieldName;
  private final Exception   exception;

  public AbstractLockEventContext(Object pojo, String threadName, String clientId, Exception exception) {
    this(pojo, null, null, threadName, clientId, exception);
  }

  public AbstractLockEventContext(Object pojo, String className, String fieldName, String threadName, String clientId,
                                  Exception exception) {
    super(pojo, threadName, clientId);

    this.className = className;
    this.fieldName = fieldName;
    this.exception = exception;
  }

  public String getClassName() {
    return className;
  }

  public String getFieldName() {
    return fieldName;
  }

  public Exception getException() {
    return exception;
  }
  
  public StackTraceElement[] getStackElements() {
    return exception.getStackTrace();
  }
}
