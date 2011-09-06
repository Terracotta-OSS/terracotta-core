/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats.statistics;

public class DoubleStatisticImpl extends StatisticImpl implements DoubleStatistic {

  private double doubleValue;

  public DoubleStatisticImpl(long lastSampleTime) {
    super(lastSampleTime);
  }

  public double getDoubleValue() {
    return this.doubleValue;
  }

  public void setDoubleValue(double doubleValue) {
    this.doubleValue = doubleValue;
  }
}
