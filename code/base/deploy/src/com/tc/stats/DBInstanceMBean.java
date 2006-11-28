/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.stats;

public interface DBInstanceMBean {
  String  getDescription();
  boolean isActive();
  void    refresh();
}
