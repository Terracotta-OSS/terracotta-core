/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
