/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.beans.tx;

import java.util.Map;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

import com.tc.management.TerracottaMBean;

/**
 * MBean for monitoring transactions and objects in a particular L1 client.
 */
public interface ClientTxMonitorMBean extends TerracottaMBean {

  /**
   * The total number of read transactions in this L1 client.
   * @return the total number of read transactions
   */
  int getReadTransactionCount();

  /**
   * The read transaction rate in this client
   * @return Read transacion rate
   */
  int getReadTransactionRatePerSecond();

  /**
   * The total number of write transactions in this L1 client.
   * @return the total number of write transactions
   */
  int getWriteTransactionCount();

  /**
   * The write transaction rate in this client
   * @return Write transacion rate
   */
  int getWriteTransactionRatePerSecond();

  /**
   * Get the smallest number of writes in a write transaction
   * @return Min writes per write txn
   */
  int getMinWritesPerWriteTransaction();

  /**
   * Get the biggest number of writes in a write transaction
   * @return Max writes per write txn
   */
  int getMaxWritesPerWriteTransaction();

  /**
   * Get most objects modified in a transaction
   * @return Max modified objects per txn
   */
  int getMaxModifiedObjectsPerTransaction();

  /**
   * Get average number of objects modified in a transaction
   * @return Avg modified objects per txn
   */  
  int getAvgModifiedObjectsPerTransaction();

  /**
   * Get object modification rate
   * @return Object modification rate
   */
  int getObjectModificationRatePerSecond();

  /**
   * Get max new objects per transaction
   * @return Max new objects per txn
   */
  int getMaxNewObjectsPerTransaction();

  /**
   * Get average number of new objects per transaction
   * @return Avg new objects per txn
   */
  int getAvgNewObjectsPerTransaction();

  /**
   * Get object creation rate
   * @return Object creation rate
   */
  int getObjectCreationRatePerSecond();

  /**
   * Get max notifications per transaction
   * @return Max notifications per txn
   */
  int getMaxNotificationsPerTransaction();

  /**
   * Get avg notifications per transaction
   * @return Average notifications per txn
   */
  int getAvgNotificationsPerTransaction();

  /**
   * Get max writes per object
   * @return Max writes per object
   */
  int getMaxWritesPerObject();

  /**
   * Get average writes per object
   * @return Average writes per object
   */
  int getAvgWritesPerObject();

  /**
   * Get objection creation by class
   * @return Table of create count per class
   * @throws OpenDataException Thrown when an open data instance can't be constructed due to validity errors
   */
  TabularData getObjectCreationCountByClass() throws OpenDataException;

  /**
   * Reset sampling
   */
  void reset();

  /**
   * Event for a committed read transaction
   */
  void committedReadTransaction();

  /**
   * Event for a committed write transaction
   * @param notifyCount the number of {@link Object#notify()} invocations for this transaction
   * @param modifiedObjectCount the total number of objects that were modified (NOT including creations)
   * @param writeCountPerObject an array of individual write counts for each object in the transaction
   * @param newObjectCountByClass a {@link Map}[{@link Class}, {@link Integer}] of all new objects created
   */
  void committedWriteTransaction(final int notifyCount, final int modifiedObjectCount, final int[] writeCountPerObject,
                                 final Map newObjectCountByClass);

}
