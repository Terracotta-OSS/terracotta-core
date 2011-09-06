/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.appevent;

/**
 * Abstract base event class to handle storage of common event state
 */
public abstract class AbstractApplicationEvent implements ApplicationEvent {

  private static final long             serialVersionUID = 1323477247234324L;

  private final ApplicationEventContext applicationEventContext;

  /**
   * Construct new event given a context
   * @context The context
   */
  public AbstractApplicationEvent(ApplicationEventContext context) {
    this.applicationEventContext = context;
  }

  public ApplicationEventContext getApplicationEventContext() {
    return applicationEventContext;
  }

  public abstract String getMessage();
}
