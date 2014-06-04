/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import java.io.Serializable;
import java.util.Map;

/**
 *
 */
public interface ManagementEventListener {

  static String CONTEXT_SOURCE_NODE_NAME = "CONTEXT_SOURCE_NODE_NAME";

  /**
   * @return the class loader that is going to be used to deserialize the event.
   */
  ClassLoader getClassLoader();

  /**
   * Called when an event was sent by a L1.
   *
   * @param event the event object.
   * @param context the event context.
   */
  void onEvent(Serializable event, Map<String, Object> context);

}
