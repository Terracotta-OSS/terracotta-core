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
