/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.beans.tx;

import java.util.Map;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

import com.tc.management.TerracottaMBean;

public interface ClientTxMonitorMBean extends TerracottaMBean {

  /**
   * The total number of read transactions in this L1 client.
   * 
   * @return the total number of read transactions
   */
  int getReadTransactionCount();

  int getReadTransactionRatePerSecond();

  int getWriteTransactionCount();

  int getWriteTransactionRatePerSecond();

  int getMinWritesPerWriteTransaction();

  int getMaxWritesPerWriteTransaction();

  int getMaxModifiedObjectsPerTransaction();

  int getAvgModifiedObjectsPerTransaction();

  int getObjectModificationRatePerSecond();

  int getMaxNewObjectsPerTransaction();

  int getAvgNewObjectsPerTransaction();

  int getObjectCreationRatePerSecond();

  int getMaxNotificationsPerTransaction();

  int getAvgNotificationsPerTransaction();

  int getMaxWritesPerObject();

  int getAvgWritesPerObject();

  TabularData getObjectCreationCountByClass() throws OpenDataException;

  void reset();

  void committedReadTransaction();

  /**
   * @param notifyCount the number of {@link Object#notify()} invocations for this transaction
   * @param modifiedObjectCount the total number of objects that were modified (NOT including creations)
   * @param writeCountPerObject an array of individual write counts for each object in the transaction
   * @param newObjectCountByClass a {@link Map}[{@link Class}, {@link Integer}] of all new objects created
   */
  void committedWriteTransaction(final int notifyCount, final int modifiedObjectCount, final int[] writeCountPerObject,
                                 final Map newObjectCountByClass);

}
