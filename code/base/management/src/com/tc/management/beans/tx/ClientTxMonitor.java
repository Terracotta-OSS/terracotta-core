/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.beans.tx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.opentypes.adapters.ClassCreationCount;
import com.tc.management.stats.AggregateInteger;

public final class ClientTxMonitor extends AbstractTerracottaMBean implements ClientTxMonitorMBean {

  final AggregateInteger readTransactions;
  final AggregateInteger writeTransactions;
  final AggregateInteger writesPerTransaction;
  final AggregateInteger modifiedObjectsPerTransaction;
  final AggregateInteger newObjectsPerTransaction;

  final AggregateInteger notifiesPerTransaction;
  final AggregateInteger writesPerObject;
  final Map              objectCreationCountByClass;

  public ClientTxMonitor() throws NotCompliantMBeanException {
    super(ClientTxMonitorMBean.class, false);
    readTransactions = new AggregateInteger("Client transactions (read)", 100);
    writeTransactions = new AggregateInteger("Client transactions (write)", 100);
    writesPerTransaction = new AggregateInteger("Writes per transaction (includes Object.notify() calls)", 100);
    modifiedObjectsPerTransaction = new AggregateInteger("Objects modified per transaction", 100);
    newObjectsPerTransaction = new AggregateInteger("New objects created per transaction", 100);
    notifiesPerTransaction = new AggregateInteger("Object.notify() invocations per transaction");
    writesPerObject = new AggregateInteger("Modifications per object");
    objectCreationCountByClass = new HashMap();
  }

  public int getReadTransactionCount() {
    return readTransactions.getN();
  }

  public int getReadTransactionRatePerSecond() {
    return readTransactions.getSampleRate(1000);
  }

  public int getWriteTransactionCount() {
    return writeTransactions.getN();
  }

  public int getWriteTransactionRatePerSecond() {
    return writeTransactions.getSampleRate(1000);
  }

  public int getMinWritesPerWriteTransaction() {
    return writesPerTransaction.getMinimum();
  }

  public int getMaxWritesPerWriteTransaction() {
    return writesPerTransaction.getMaximum();
  }

  public int getMaxModifiedObjectsPerTransaction() {
    return modifiedObjectsPerTransaction.getMaximum();
  }

  public int getAvgModifiedObjectsPerTransaction() {
    return (int) modifiedObjectsPerTransaction.getAverage();
  }

  public int getObjectModificationRatePerSecond() {
    return modifiedObjectsPerTransaction.getSampleRate(1000);
  }

  public int getMaxNewObjectsPerTransaction() {
    return newObjectsPerTransaction.getMaximum();
  }

  public int getAvgNewObjectsPerTransaction() {
    return (int) newObjectsPerTransaction.getAverage();
  }

  public int getObjectCreationRatePerSecond() {
    return newObjectsPerTransaction.getSampleRate(1000);
  }

  public int getMaxNotificationsPerTransaction() {
    return notifiesPerTransaction.getMaximum();
  }

  public int getAvgNotificationsPerTransaction() {
    return (int) notifiesPerTransaction.getAverage();
  }

  public int getMaxWritesPerObject() {
    return writesPerObject.getMaximum();
  }

  public int getAvgWritesPerObject() {
    return (int) writesPerObject.getAverage();
  }

  public TabularData getObjectCreationCountByClass() throws OpenDataException {
    TabularData tabularData = ClassCreationCount.newTabularDataInstance();
    CompositeData compositeData;

    synchronized (objectCreationCountByClass) {
      for (Iterator iter = objectCreationCountByClass.keySet().iterator(); iter.hasNext();) {
        Class classNameItemValue = (Class) iter.next();
        AggregateInteger objectCreationCountItemValue = (AggregateInteger) objectCreationCountByClass
            .get(classNameItemValue);
        compositeData = new ClassCreationCount(classNameItemValue.getName(), new Integer(objectCreationCountItemValue
            .getSum())).toCompositeData();
        tabularData.put(compositeData);
      }
    }
    return tabularData;
  }

  public synchronized void reset() {
    readTransactions.reset();
    writeTransactions.reset();
    writesPerTransaction.reset();
    modifiedObjectsPerTransaction.reset();
    newObjectsPerTransaction.reset();
    notifiesPerTransaction.reset();
    writesPerObject.reset();
    synchronized (objectCreationCountByClass) {
      objectCreationCountByClass.clear();
    }
  }

  public synchronized void committedReadTransaction() {
    if (isEnabled()) readTransactions.addSample(1);
  }

  public synchronized void committedWriteTransaction(final int notifyCount, final int modifiedObjectCount,
                                                     final int[] writeCountPerObject, final Map newObjectCountByClass) {
    if (isEnabled()) {
      writeTransactions.addSample(1);
      modifiedObjectsPerTransaction.addSample(modifiedObjectCount);
      notifiesPerTransaction.addSample(notifyCount);
      int totalWriteCount = 0;
      for (int i = 0; i < writeCountPerObject.length; i++) {
        totalWriteCount += writeCountPerObject[i];
        writesPerObject.addSample(writeCountPerObject[i]);
      }
      writesPerTransaction.addSample(totalWriteCount + notifyCount);
      if (newObjectCountByClass != null && !newObjectCountByClass.isEmpty()) {
        int totalNewObjectCount = 0;
        for (Iterator iter = newObjectCountByClass.keySet().iterator(); iter.hasNext();) {
          final Class createdObjectClass = (Class) iter.next();
          final Integer classCreationCount = (Integer) newObjectCountByClass.get(createdObjectClass);
          synchronized (objectCreationCountByClass) {
            AggregateInteger instanceCounter = (AggregateInteger) objectCreationCountByClass.get(createdObjectClass);
            if (instanceCounter == null) {
              instanceCounter = new AggregateInteger("Object creation count for class[" + createdObjectClass.getName()
                                                     + "]");
              objectCreationCountByClass.put(createdObjectClass, instanceCounter);
            }
            instanceCounter.addSample(classCreationCount.intValue());
            totalNewObjectCount += classCreationCount.intValue();
          }
        }
        newObjectsPerTransaction.addSample(totalNewObjectCount);
      } else {
        newObjectsPerTransaction.addSample(0);
      }
    }
  }
}
