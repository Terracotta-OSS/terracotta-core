/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.exposed;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

import com.tc.management.TerracottaMBean;

public interface SessionsProductMBean extends TerracottaMBean {
  
  int getRequestCount();
  
  int getRequestCountPerSecond();
  
  int getSessionsCreatedPerMinute();
  
  int getSessionsExpiredPerMinute();
  
  int getSessionWritePercentage();

  TabularData getTop10ClassesByObjectCreationCount() throws OpenDataException;
  
  void expireSession(String sessionId);
  
}
