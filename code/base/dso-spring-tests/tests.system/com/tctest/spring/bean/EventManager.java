/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import java.util.Date;

public interface EventManager {

  int size();

  void publishEvents(Object source, String message, int count);

  void publishLocalEvent(Object source, String message);

  void clear();

  Date getLastEventTime();

}
