/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;

import java.util.Date;

public interface EventManager {

  int size();

  void publishEvent(Object source, String message);

  void publishLocalEvent(Object source, String message);

  void clear();

  Date getLastEventTime();

}
