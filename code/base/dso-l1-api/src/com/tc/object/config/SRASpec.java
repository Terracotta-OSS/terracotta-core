/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import com.tc.statistics.StatisticRetrievalAction;

import java.util.Collection;

public interface SRASpec extends OsgiServiceSpec {
  public Collection<StatisticRetrievalAction> getSRAs();
}
