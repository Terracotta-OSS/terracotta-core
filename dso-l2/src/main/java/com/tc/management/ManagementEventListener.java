/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import java.io.Serializable;

/**
 *
 */
public interface ManagementEventListener {

  /**
   * @return the class loader that is going to be used to deserialize the event.
   */
  ClassLoader getClassLoader();

  /**
   * Called when an event was sent by a L1.
   *
   * @param event the event object.
   */
  void onEvent(Serializable event);

}
