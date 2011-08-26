/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import com.tc.object.appevent.ApplicationEventContext;
import com.tc.object.appevent.ReadOnlyObjectEvent;
import com.tc.object.appevent.ReadOnlyObjectEventContext;

public class ReadOnlyWorkState extends AbstractWorkState {
  private ReadOnlyObjectEvent fEvent;

  public ReadOnlyWorkState(ReadOnlyObjectEvent event) {
    fEvent = event;
  }

  public ReadOnlyObjectEvent getEvent() {
    return fEvent;
  }

  public ReadOnlyObjectEventContext getEventContext() {
    return fEvent.getReadOnlyObjectEventContext();
  }

  public String descriptionFor(ApplicationEventContext context) {
    if (!(context instanceof ReadOnlyObjectEventContext)) return "";
    return null;
  }

  public String summary() {
    return "Read-only shared object modification";
  }
}