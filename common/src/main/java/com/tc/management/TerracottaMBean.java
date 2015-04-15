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


public interface TerracottaMBean {

  /**
   * @return the full name of the interface that this bean implements.
   */
  String getInterfaceClassName();

  /**
   * @return true if this bean emits notifications.
   */
  boolean isNotificationBroadcaster();
  
  /**
   * A bean can be enabled to collect stats, or disabled to decrease overhead
   */
  void enable();
  
  /**
   * A bean can be enabled to collect stats, or disabled to decrease overhead
   */
  void disable();
  
  boolean isEnabled();

  /**
   * This method will be called each time the bean is disabled when it was
   * enabled beforehand
   */
  void reset();

}
