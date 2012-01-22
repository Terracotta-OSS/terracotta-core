/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
