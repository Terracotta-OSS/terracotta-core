/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.event;

public class UpdateEvent {

  public Object              data;
  public UpdateEventListener source; // may be null

  public UpdateEvent(Object data) {
    this.data = data;
  }
}
