/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.appevent;


public abstract class AbstractApplicationEvent implements ApplicationEvent {

  private static final long             serialVersionUID = 1323477247234324L;

  private final ApplicationEventContext applicationEventContext;

  public AbstractApplicationEvent(ApplicationEventContext context) {
    this.applicationEventContext = context;
  }

  public ApplicationEventContext getApplicationEventContext() {
    return applicationEventContext;
  }

  public abstract String getMessage();
}
