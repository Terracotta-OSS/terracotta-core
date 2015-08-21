/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import java.util.Map;

/**
 * Management events listener interface.
 *
 * @author Ludovic Orban
 */
public interface ManagementEventListener {

  static String CONTEXT_SOURCE_NODE_NAME = "CONTEXT_SOURCE_NODE_NAME";
  static String CONTEXT_SOURCE_JMX_ID = "CONTEXT_SOURCE_JMX_ID";

  /**
   * Get the classloader from which to load classes of deserialized objects.
   *
   * @return the class loader that is going to be used to deserialize the event.
   */
  ClassLoader getClassLoader();

  /**
   * Called when an event is sent by a L1.
   *
   * @param event the event object.
   * @param context the event context.
   */
  void onEvent(TCManagementEvent event, Map<String, Object> context);

}
