/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.common;

import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.SeriesChangeEvent;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimePeriod;
import org.jfree.data.time.TimeTableXYDataset;

public class FixedTimeTableXYDataset extends TimeTableXYDataset {
  private int  maximumItemCount = Integer.MAX_VALUE;
  private long maximumItemAge   = Long.MAX_VALUE;

  public FixedTimeTableXYDataset() {
    super();
  }

  /**
   * Returns the maximum number of items that will be retained in the series. The default value is
   * <code>Integer.MAX_VALUE</code>.
   * 
   * @return The maximum item count.
   * @see #setMaximumItemCount(int)
   */
  public int getMaximumItemCount() {
    return maximumItemCount;
  }

  /**
   * Sets the maximum number of items that will be retained in the series. If you add a new item to the series such that
   * the number of items will exceed the maximum item count, then the FIRST element in the series is automatically
   * removed, ensuring that the maximum item count is not exceeded.
   * 
   * @param maximum the maximum (requires >= 0).
   * @see #getMaximumItemCount()
   */
  public void setMaximumItemCount(int maximum) {
    if (maximum < 0) { throw new IllegalArgumentException("Negative 'maximum' argument."); }
    this.maximumItemCount = maximum;
    int count = getItemCount();
    if (count > maximum) {
      int diff = count - maximum;
      for (int j = 0; j < diff; j++) {
        removeFirstItem();
      }
    }
  }

  private void removeFirstItem() {
    TimePeriod period = getTimePeriod(0);
    for (int i = 0; i < getSeriesCount(); i++) {
      String seriesName = getSeriesKey(i).toString();
      remove(period, seriesName, false);
    }
  }

  /**
   * Returns the maximum item age (in time periods) for the series.
   * 
   * @return The maximum item age.
   * @see #setMaximumItemAge(long)
   */
  public long getMaximumItemAge() {
    return this.maximumItemAge;
  }

  /**
   * Sets the number of time units in the 'history' for the series. This provides one mechanism for automatically
   * dropping old data from the time series. For example, if a series contains daily data, you might set the history
   * count to 30. Then, when you add a new data item, all data items more than 30 days older than the latest value are
   * automatically dropped from the series.
   * 
   * @param periods the number of time periods.
   * @see #getMaximumItemAge()
   */
  public void setMaximumItemAge(long periods) {
    if (periods < 0) { throw new IllegalArgumentException("Negative 'periods' argument."); }
    this.maximumItemAge = periods;
    removeAgedItems(true); // remove old items and notify if necessary
  }

  /**
   * Adds a new data item to the dataset and, if requested, sends a {@link DatasetChangeEvent} to all registered
   * listeners.
   * 
   * @param period the time period (<code>null</code> not permitted).
   * @param y the value for this period (<code>null</code> permitted).
   * @param seriesName the name of the series to add the value (<code>null</code> not permitted).
   * @param notify whether dataset listener are notified or not.
   * @see #remove(TimePeriod, String, boolean)
   */
  @Override
  public void add(TimePeriod period, Number y, String seriesName, boolean notify) {
    super.add(period, y, seriesName, false);
    removeAgedItems(false);
    if (notify) {
      fireDatasetChanged();
    }
  }

  /**
   * Age items in the series. Ensure that the timespan from the youngest to the oldest record in the series does not
   * exceed maximumItemAge time periods. Oldest items will be removed if required.
   * 
   * @param notify controls whether or not a {@link SeriesChangeEvent} is sent to registered listeners IF any items are
   *        removed.
   */
  public void removeAgedItems(boolean notify) {
    // check if there are any values earlier than specified by the history
    // count...
    if (getItemCount() > 1) {
      long latest = getRegularTimePeriod(getItemCount() - 1).getSerialIndex();
      boolean removed = false;
      while ((latest - getRegularTimePeriod(0).getSerialIndex()) > this.maximumItemAge) {
        removeFirstItem();
        removed = true;
      }
      if (removed && notify) {
        fireDatasetChanged();
      }
    }
  }

  public RegularTimePeriod getRegularTimePeriod(int index) {
    return (RegularTimePeriod) getTimePeriod(index);
  }
}
