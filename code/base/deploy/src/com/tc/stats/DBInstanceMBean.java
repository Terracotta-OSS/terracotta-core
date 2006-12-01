/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats;

public interface DBInstanceMBean {
  String  getDescription();
  boolean isActive();
  void    refresh();
}
