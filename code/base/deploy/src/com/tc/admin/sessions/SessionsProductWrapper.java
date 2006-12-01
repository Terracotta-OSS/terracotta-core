/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.sessions;

import com.tc.management.exposed.SessionsProductMBean;
import com.tc.management.opentypes.adapters.ClassCreationCount;

import javax.management.openmbean.TabularData;

public class SessionsProductWrapper {
  private SessionsProductMBean bean;
  
  private int                  requestCount;
  private int                  requestCountPerSecond;
  private int                  sessionWritePercentage;
  private int                  sessionsCreatedPerMinute;
  private int                  sessionsExpiredPerMinute;
  private TabularData          top10ClassesByObjectCreationCount;
  private ClassCreationCount[] classCreationCount;
  
  public SessionsProductWrapper(SessionsProductMBean bean) {
    this.bean = bean;
    
    requestCount             = bean.getRequestCount();
    requestCountPerSecond    = bean.getRequestCountPerSecond();
    sessionWritePercentage   = bean.getSessionWritePercentage();
    sessionsCreatedPerMinute = bean.getSessionsCreatedPerMinute();
    sessionsExpiredPerMinute = bean.getSessionsExpiredPerMinute();
    
    try {
      top10ClassesByObjectCreationCount = bean.getTop10ClassesByObjectCreationCount();
      if(top10ClassesByObjectCreationCount != null) {
        classCreationCount = ClassCreationCount.fromTabularData(top10ClassesByObjectCreationCount);
      } else {
        classCreationCount = new ClassCreationCount[0];
      }
    } catch(Exception e) {
      classCreationCount = new ClassCreationCount[0];
    }
  }
  
  public int getRequestCount() {
    return requestCount;
  }

  public int getRequestCountPerSecond() {
    return requestCountPerSecond;
  }

  public int getSessionWritePercentage() {
    return sessionWritePercentage;
  }

  public int getSessionsCreatedPerMinute() {
    return sessionsCreatedPerMinute;
  }

  public int getSessionsExpiredPerMinute() {
    return sessionsExpiredPerMinute;
  }

  public TabularData getTop10ClassesByObjectCreationCount() {
    return top10ClassesByObjectCreationCount;
  }
  
  public ClassCreationCount[] getClassCreationCount() {
    return classCreationCount;
  }

  public void expireSession(String sessionId) {
    bean.expireSession(sessionId);
  }
}
