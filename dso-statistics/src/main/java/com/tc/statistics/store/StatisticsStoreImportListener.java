/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store;

public interface StatisticsStoreImportListener {
  public void started();

  public void imported(long count);

  public void optimizing();

  public void finished(long total);
}