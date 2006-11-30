/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
  
  void enable();
  
  void disable();
  
  boolean isEnabled();

  void reset();

}
