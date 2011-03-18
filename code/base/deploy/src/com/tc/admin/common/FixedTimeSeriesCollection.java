/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.common;

import org.jfree.data.DomainInfo;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.SeriesChangeEvent;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimePeriodAnchor;
import org.jfree.data.xy.AbstractIntervalXYDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * An XYDataset implementation that represents a collection of samples gathered at the same moment. This was created
 * because TimeSeriesCollection was inefficient with a large number of series.
 * 
 * @author gkeim
 */

public class FixedTimeSeriesCollection extends AbstractIntervalXYDataset implements XYDataset, IntervalXYDataset,
    DomainInfo, Serializable {

  private int                      maximumItemCount;

  private final Comparable[]       seriesKeys;

  private final List<MomentSample> data;

  private final Calendar           workingCalendar;

  /**
   * The point within each time period that is used for the X value when this collection is used as an
   * {@link org.jfree.data.xy.XYDataset}. This can be the start, middle or end of the time period.
   */
  private TimePeriodAnchor         xPosition;

  public static class MomentSample {
    final RegularTimePeriod timePeriod;
    final float[]           samples;

    public MomentSample(RegularTimePeriod timePeriod, float[] samples) {
      this.timePeriod = timePeriod;
      this.samples = samples;
    }

    public RegularTimePeriod getTimePeriod() {
      return timePeriod;
    }

    public float[] getSamples() {
      return samples;
    }

    public float getSample(int series) {
      return samples[series];
    }
  }

  public FixedTimeSeriesCollection(Comparable[] seriesKeys, int maximumItemCount) {
    this(seriesKeys, maximumItemCount, TimeZone.getDefault());
  }

  public FixedTimeSeriesCollection(Comparable[] seriesKeys, int maximumItemCount, TimeZone timeZone) {
    this.seriesKeys = seriesKeys;
    this.maximumItemCount = maximumItemCount;
    this.workingCalendar = Calendar.getInstance(timeZone);
    this.data = new ArrayList<MomentSample>();
    this.xPosition = TimePeriodAnchor.START;
  }

  /**
   * Returns the maximum number of items that will be retained in the series. The default value is
   * <code>Integer.MAX_VALUE</code>.
   * 
   * @return The maximum item count.
   * @see #setMaximumItemCount(int)
   */
  public int getMaximumItemCount() {
    return this.maximumItemCount;
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
    int count = this.data.size();
    if (count > maximum) {
      delete(0, count - maximum - 1);
    }
  }

  /**
   * Deletes data from start until end index (end inclusive).
   * 
   * @param start the index of the first period to delete.
   * @param end the index of the last period to delete.
   */
  public void delete(int start, int end) {
    if (end < start) { throw new IllegalArgumentException("Requires start <= end."); }
    for (int i = 0; i <= (end - start); i++) {
      this.data.remove(start);
    }
    fireDatasetChanged();
  }

  /**
   * Returns the position within each time period that is used for the X value when the collection is used as an
   * {@link org.jfree.data.xy.XYDataset}.
   * 
   * @return The anchor position (never <code>null</code>).
   */
  public TimePeriodAnchor getXPosition() {
    return this.xPosition;
  }

  /**
   * Sets the position within each time period that is used for the X values when the collection is used as an
   * {@link XYDataset}, then sends a {@link DatasetChangeEvent} is sent to all registered listeners.
   * 
   * @param anchor the anchor position (<code>null</code> not permitted).
   */
  public void setXPosition(TimePeriodAnchor anchor) {
    if (anchor == null) { throw new IllegalArgumentException("Null 'anchor' argument."); }
    this.xPosition = anchor;
    notifyListeners(new DatasetChangeEvent(this, this));
  }

  /**
   * Returns the range of the values in this dataset's domain.
   * 
   * @param includeInterval a flag that determines whether or not the x-interval is taken into account.
   * @return The range.
   */
  public Range getDomainBounds(boolean includeInterval) {
    Range result = null;
    int count = data.size();
    if (count > 0) {
      RegularTimePeriod start = getTimePeriod(0);
      RegularTimePeriod end = getTimePeriod(count - 1);
      Range temp;
      if (!includeInterval) {
        temp = new Range(getX(start), getX(end));
      } else {
        temp = new Range(start.getFirstMillisecond(this.workingCalendar), end.getLastMillisecond(this.workingCalendar));
      }
      result = Range.combine(result, temp);
    }

    return result;
  }

  public RegularTimePeriod getTimePeriod(int i) {
    return data.get(i).getTimePeriod();
  }

  /**
   * Returns the x-value for a time period.
   * 
   * @param period the time period (<code>null</code> not permitted).
   * @return The x-value.
   */
  protected synchronized long getX(RegularTimePeriod period) {
    long result = 0L;
    if (this.xPosition == TimePeriodAnchor.START) {
      result = period.getFirstMillisecond(this.workingCalendar);
    } else if (this.xPosition == TimePeriodAnchor.MIDDLE) {
      result = period.getMiddleMillisecond(this.workingCalendar);
    } else if (this.xPosition == TimePeriodAnchor.END) {
      result = period.getLastMillisecond(this.workingCalendar);
    }
    return result;
  }

  /**
   * Returns the minimum x-value in the dataset.
   * 
   * @param includeInterval a flag that determines whether or not the x-interval is taken into account.
   * @return The minimum value.
   */
  public double getDomainLowerBound(boolean includeInterval) {
    double result = Double.NaN;
    Range r = getDomainBounds(includeInterval);
    if (r != null) {
      result = r.getLowerBound();
    }
    return result;
  }

  /**
   * Returns the maximum x-value in the dataset.
   * 
   * @param includeInterval a flag that determines whether or not the x-interval is taken into account.
   * @return The maximum value.
   */
  public double getDomainUpperBound(boolean includeInterval) {
    double result = Double.NaN;
    Range r = getDomainBounds(includeInterval);
    if (r != null) {
      result = r.getUpperBound();
    }
    return result;
  }

  /**
   * Returns the ending X value for the specified series and item.
   * 
   * @param series The series (zero-based index).
   * @param item The item (zero-based index).
   * @return The value.
   */
  public synchronized Number getEndX(int series, int item) {
    return new Long(getTimePeriod(item).getLastMillisecond(this.workingCalendar));
  }

  /**
   * Returns the ending Y value for the specified series and item.
   * 
   * @param series the series (zero-based index).
   * @param item the item (zero-based index).
   * @return The value (possibly <code>null</code>).
   */
  public Number getEndY(int series, int item) {
    return getY(series, item);
  }

  /**
   * Returns the starting X value for the specified series and item.
   * 
   * @param series the series (zero-based index).
   * @param item the item (zero-based index).
   * @return The value.
   */
  public synchronized Number getStartX(int series, int item) {
    return new Long(getTimePeriod(item).getFirstMillisecond(this.workingCalendar));
  }

  /**
   * Returns the starting Y value for the specified series and item.
   * 
   * @param series the series (zero-based index).
   * @param item the item (zero-based index).
   * @return The value (possibly <code>null</code>).
   */
  public Number getStartY(int series, int item) {
    return getY(series, item);
  }

  /**
   * Returns the number of items in the specified series which, in this implementation, are all equals to the number of
   * moment samples.
   * 
   * @param series the series index (zero-based).
   * @return The item count.
   */
  public int getItemCount(int series) {
    return getItemCount();
  }

  /**
   * Returns the number of moment samples.
   * 
   * @return The item count.
   */
  public int getItemCount() {
    return data.size();
  }

  /**
   * Returns the x-value for the specified series and item.
   * 
   * @param series the series (zero-based index).
   * @param item the item (zero-based index).
   * @return The value.
   */
  public Number getX(int series, int item) {
    return new Long(getX(getTimePeriod(item)));
  }

  /**
   * Returns the y-value for the specified series and item.
   * 
   * @param series the series (zero-based index).
   * @param item the item (zero-based index).
   * @return The value (possibly <code>null</code>).
   */
  public Number getY(int series, int item) {
    MomentSample ms = data.get(item);
    return Float.valueOf(ms.getSample(series));
  }

  /**
   * Returns the number of series in the collection.
   * 
   * @return The series count.
   */
  @Override
  public int getSeriesCount() {
    return seriesKeys.length;
  }

  /**
   * Returns the key for a series.
   * 
   * @param series the index of the series (zero-based).
   * @return The key for a series.
   */
  @Override
  public Comparable getSeriesKey(int i) {
    return seriesKeys[i];
  }

  public void appendData(RegularTimePeriod timePeriod, float[] newData) {
    if (newData == null) {
      int itemCount = getItemCount();
      if (itemCount > 0) {
        newData = data.get(itemCount - 1).getSamples();
      } else {
        newData = new float[getSeriesCount()];
      }
    }
    if (newData.length != getSeriesCount()) { throw new IllegalArgumentException(
                                                                                 "Sample set doesn't match number of series"); }
    data.add(new MomentSample(timePeriod, newData));

    // check if this addition will exceed the maximum item count...
    if (getItemCount() > this.maximumItemCount) {
      this.data.remove(0);
    }

    fireDatasetChanged();
  }

  /**
   * Removes all data items from the series and sends a {@link SeriesChangeEvent} to all registered listeners.
   */
  public void clear() {
    if (data.size() > 0) {
      data.clear();
      fireDatasetChanged();
    }
  }
}
