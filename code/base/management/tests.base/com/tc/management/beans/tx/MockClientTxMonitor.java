/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.beans.tx;

import java.util.Map;

import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

import com.tc.management.AbstractTerracottaMBean;

public final class MockClientTxMonitor extends AbstractTerracottaMBean implements ClientTxMonitorMBean {

  public MockClientTxMonitor() throws NotCompliantMBeanException {
    super(ClientTxMonitorMBean.class, false);
  }

  public void committedReadTransaction() {
  }

  public void committedWriteTransaction(int notifyCount, int modifiedObjectCount, int[] writeCountPerObject,
                                        Map newObjectCountByClass) {
  }

  public int getAvgModifiedObjectsPerTransaction() {
    return 0;
  }

  public int getAvgNewObjectsPerTransaction() {
    return 0;
  }

  public int getAvgNotificationsPerTransaction() {
    return 0;
  }

  public int getAvgWritesPerObject() {
    return 0;
  }

  public int getAvgWritesPerWriteTransactionPerSecond() {
    return 0;
  }

  public int getMaxModifiedObjectsPerTransaction() {
    return 0;
  }

  public int getMaxNewObjectsPerTransaction() {
    return 0;
  }

  public int getMaxNotificationsPerTransaction() {
    return 0;
  }

  public int getMaxWritesPerObject() {
    return 0;
  }

  public int getMaxWritesPerWriteTransaction() {
    return 0;
  }

  public int getMinWritesPerWriteTransaction() {
    return 0;
  }

  public TabularData getObjectCreationCountByClass() throws OpenDataException {
    return null;
  }

  public int getObjectCreationRatePerSecond() {
    return 0;
  }

  public int getObjectModificationRatePerSecond() {
    return 0;
  }

  public int getReadTransactionCount() {
    return 0;
  }

  public int getReadTransactionRatePerSecond() {
    return 0;
  }

  public int getWriteTransactionCount() {
    return 0;
  }

  public int getWriteTransactionRatePerSecond() {
    return 0;
  }

  public void reset() {
    // nothing to reset
  }

}
