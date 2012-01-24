/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
