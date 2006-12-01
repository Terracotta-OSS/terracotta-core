/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.exposed;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.beans.sessions.SessionMonitorMBean;
import com.tc.management.beans.tx.ClientTxMonitorMBean;
import com.tc.management.opentypes.adapters.ClassCreationCount;
import com.tc.management.stats.TopN;

import java.util.Iterator;

import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

public class SessionsProduct extends AbstractTerracottaMBean implements SessionsProductMBean {

  private final SessionMonitorMBean  sessionsMonitor;
  private final ClientTxMonitorMBean clientTxMonitor;

  public SessionsProduct(final SessionMonitorMBean sessionsMonitor, final ClientTxMonitorMBean clientTxMonitor)
      throws NotCompliantMBeanException {
    super(SessionsProductMBean.class, false);
    this.sessionsMonitor = sessionsMonitor;
    this.clientTxMonitor = clientTxMonitor;
  }
  
  public void reset() {
    // nothing to reset
  }

  public int getRequestCount() {
    return sessionsMonitor.getRequestCount();
  }

  public int getRequestCountPerSecond() {
    return sessionsMonitor.getRequestRatePerSecond();
  }

  public int getSessionsCreatedPerMinute() {
    return sessionsMonitor.getSessionCreationRatePerMinute();
  }

  public int getSessionsExpiredPerMinute() {
    return sessionsMonitor.getSessionDestructionRatePerMinute();
  }

  public int getSessionWritePercentage() {
    int percentage = -1;
    int reads = clientTxMonitor.getReadTransactionCount();
    int writes = clientTxMonitor.getWriteTransactionCount();
    percentage = (int) (((double) writes / (reads + writes)) * 100);
    return percentage;
  }

  public TabularData getTop10ClassesByObjectCreationCount() throws OpenDataException {
    TabularData tData = clientTxMonitor.getObjectCreationCountByClass();
    if (tData == null || tData.isEmpty()) { return null; }

    TopN topTen = new TopN(10);
    for (Iterator iter = tData.values().iterator(); iter.hasNext();) {
      topTen.evaluate(new ClassCreationCount((CompositeData) iter.next()));
    }

    TabularData tabularData = ClassCreationCount.newTabularDataInstance();
    for (Iterator iter = topTen.iterator(); iter.hasNext();) {
      tabularData.put(((ClassCreationCount) iter.next()).toCompositeData());
    }
    return tabularData;
  }

  public void expireSession(String sessionId) {
    sessionsMonitor.expireSession(sessionId);
  }

}
