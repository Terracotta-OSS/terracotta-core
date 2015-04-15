/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
