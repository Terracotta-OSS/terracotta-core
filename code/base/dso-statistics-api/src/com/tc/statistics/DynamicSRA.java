/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

/**
 * Interface that has to be implemented by SRAs that need to dynamically
 * enable or disable the measurement and sampling of the statistic data in the
 * system. Typically, this is used when this data imposes a non-neglectable
 * overhead to the system when being measured.
 */
public interface DynamicSRA extends StatisticRetrievalAction {

  void enableStatisticCollection();

  void disableStatisticCollection();
}
