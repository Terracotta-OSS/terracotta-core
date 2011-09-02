/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.appevent;

/**
 * Abstract lock event context, builds on AbstractApplicationEventContext
 */
public abstract class AbstractLockEventContext extends AbstractApplicationEventContext {

  private static final long serialVersionUID = 4788562594133534828L;

  private final String      className;
  private final String      fieldName;
  private final Exception   exception;

  /**
   * Construct new lock event context
   * @param pojo Object of interest
   * @param threadName Thread name
   * @param clientID JVM client ID
   * @param exception Exception that occurred
   */
  public AbstractLockEventContext(Object pojo, String threadName, String clientId, Exception exception) {
    this(pojo, null, null, threadName, clientId, exception);
  }

  /**
   * Construct new lock event context
   * @param pojo Object of interest
   * @param className Class that was being updated 
   * @param fieldName Field that was being updated
   * @param threadName Thread name
   * @param clientID JVM client ID
   * @param exception Exception that occurred
   */
  public AbstractLockEventContext(Object pojo, String className, String fieldName, String threadName, String clientId,
                                  Exception exception) {
    super(pojo, threadName, clientId);

    this.className = className;
    this.fieldName = fieldName;
    this.exception = exception;
  }

  /**
   * @return Class that was being updated, may be null
   */
  public String getClassName() {
    return className;
  }

  /**
   * @return Field that was being updated, may be null
   */
  public String getFieldName() {
    return fieldName;
  }

  /**
   * @return Exception that occurred
   */
  public Exception getException() {
    return exception;
  }
  
  /**
   * @return The stack elements from the {@link #getException()}
   */
  public StackTraceElement[] getStackElements() {
    return exception.getStackTrace();
  }
}
