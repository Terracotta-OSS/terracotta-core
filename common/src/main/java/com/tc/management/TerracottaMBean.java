/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
