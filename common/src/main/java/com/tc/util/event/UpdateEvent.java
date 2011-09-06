/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.event;

public class UpdateEvent {

  public Object              data;
  public UpdateEventListener source; // may be null

  public UpdateEvent(Object data) {
    this.data = data;
  }
}
