/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.stats.statistics;

import javax.management.j2ee.statistics.Statistic;

public interface DoubleStatistic extends Statistic {

  double getDoubleValue();

}
