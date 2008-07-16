/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import com.tc.object.appevent.ApplicationEventContext;
import com.tc.object.appevent.UnlockedSharedObjectEvent;
import com.tc.object.appevent.UnlockedSharedObjectEventContext;

public class UnlockedSharedWorkState extends AbstractWorkState {
  private UnlockedSharedObjectEvent fEvent;

  public UnlockedSharedWorkState(UnlockedSharedObjectEvent event) {
    fEvent = event;
  }

  public UnlockedSharedObjectEvent getEvent() {
    return fEvent;
  }

  public UnlockedSharedObjectEventContext getEventContext() {
    return fEvent.getUnlockedSharedObjectEventContext();
  }

  public String descriptionFor(ApplicationEventContext context) {
    if (!(context instanceof UnlockedSharedObjectEventContext)) return "";
    return null;
  }

  public String summary() {
    return "Unlocked shared object exception";
  }
}