/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.stats;

import java.io.Serializable;
import java.text.MessageFormat;

public final class AggregateInteger implements Serializable {

  /**
   * @param {0} name
   * @param {1} n
   * @param {2} sum
   * @param {3} minimum
   * @param {4} maximum
   * @param {5} average
   */
  private static final String TO_STRING_FORMAT = "<{0}(integer): [samples/{1}], [sum/{2}], [minimum/{3}], [maximum/{4}], [average/{5}]>";

  private static final class Sample {

    final long timestamp;

    Sample() {
      timestamp = System.currentTimeMillis();
    }

  }

  private final String   name;

  // Even though it is unnecessary to make ints volatile, we say so here to indicate that readers of these variables do
  // not necessarily need synchronization; if they are a little behind in reading values it's ok so long as they are not
  // corrupted (hence making them volatile), only the writers need actual synchronization
  private volatile int   n;
  private volatile int   sum;
  private volatile int   minimum;
  private volatile int   maximum;

  // This used to be a variable-length linked list based on time rather than sample count, but the performance was
  // miserable so now it's a fixed size circular buffer :(
  private final Sample[] sampleHistory;
  private int            nextHistoryPosition;
  private final Sample[] sampleHistorySnapshot;

  /**
   * Creates a new aggregate integer statistic without maintaining a history of samples.
   * 
   * @param name the name of this statistic
   */
  public AggregateInteger(final String name) {
    this(name, 0);
  }

  /**
   * Creates a new aggregate integer statistic, maintaining a rolling history of samples for the last
   * {@link historyLengthInSamples} samples. Sample rates are extrapolated based on how many samples are maintained.
   */
  public AggregateInteger(final String name, final int historyLengthInSamples) {
    this.name = name;
    sampleHistory = historyLengthInSamples > 0 ? new Sample[historyLengthInSamples] : null;
    sampleHistorySnapshot = historyLengthInSamples > 0 ? new Sample[historyLengthInSamples] : null;
    nextHistoryPosition = 0;
    reset();
  }

  public synchronized void addSample(final int sample) {
    if (sample < minimum) minimum = sample;
    if (sample > maximum) maximum = sample;
    ++n;
    sum += sample;
    // If we are keeping track of history, add our sample to the tail of the list and trim the front so it's within our
    // timing boundary
    if (sampleHistory != null) {
      sampleHistory[nextHistoryPosition++] = new Sample();
      nextHistoryPosition %= sampleHistory.length;
    }
  }

  /**
   * Resets this statistic, all counters/averages/etc. go to 0; any history is cleared as well.
   */
  public synchronized void reset() {
    n = sum = 0;
    minimum = Integer.MAX_VALUE;
    maximum = Integer.MIN_VALUE;
    if (sampleHistory != null) {
      for (int pos = 0; pos < sampleHistory.length; ++pos) {
        sampleHistory[pos] = null;
      }
      nextHistoryPosition = 0;
    }
  }

  public String getName() {
    return name;
  }

  /**
   * @return the maximum value of all samples
   */
  public int getMaximum() {
    return maximum;
  }

  /**
   * @return the minimum value of all samples
   */
  public int getMinimum() {
    return minimum;
  }

  /**
   * @return the number of samples (so far)
   */
  public int getN() {
    return n;
  }

  /**
   * @return the running sum of samples (so far)
   */
  public int getSum() {
    return sum;
  }

  /**
   * @return the running average of the samples (so far)
   */
  public double getAverage() {
    return n > 0 ? ((double) sum / (double) n) : 0.0;
  }

  /**
   * Returns an average rate at which samples were added, if you want this rate per second then pass in
   * <strong>1000</strong>, if you want it per minute then pass in <strong>1000 * 60</strong>, etc. This rate is
   * extrapolated from the entire available history as defined in the constructor. For finer and more accurate rates,
   * the history length should be lengthened.
   * 
   * @return the rate at which samples were added per {@link periodInMillis}, averaged over the (rolling) history
   *         length, or -1 if history is not being kept
   */
  public int getSampleRate(final long periodInMillis) {
    // XXX:
    // NOTE:
    // IMPORTANT:
    // If you mess with this method, please run the AggregateIntegerTest manually and un-disable the
    // testGetSampleRate() method there. It does not pass reliably because it is timing dependent, but it should
    // be run manually if this method is modified.
    final int sampleRate;
    if (sampleHistorySnapshot != null) {
      // We synchronize on and use our history snapshot (thus keeping with our fixed-memory requirements) for each
      // calculation
      synchronized (sampleHistorySnapshot) {
        final int snapshotPosition;
        final int localN;
        synchronized (this) {
          for (int pos = 0; pos < sampleHistory.length; ++pos) {
            sampleHistorySnapshot[pos] = sampleHistory[pos];
          }
          snapshotPosition = nextHistoryPosition;
          localN = n;
        }
        if (localN > 0) {
          // Now with our snapshot data we need to extrapolate our rate information
          final Sample oldestSample;
          final int existingSampleCount;
          if (localN > sampleHistorySnapshot.length) {
            oldestSample = sampleHistorySnapshot[snapshotPosition];
            existingSampleCount = sampleHistorySnapshot.length;
          } else {
            oldestSample = sampleHistorySnapshot[0];
            existingSampleCount = localN;
          }
          final double elapsedSampleTimeInMillis = System.currentTimeMillis() - oldestSample.timestamp;
          if (elapsedSampleTimeInMillis > 0) {
            sampleRate = (int) ((periodInMillis / elapsedSampleTimeInMillis) * existingSampleCount);
          } else {
            sampleRate = 0;
          }
        } else {
          sampleRate = 0;
        }
      }
    } else {
      sampleRate = -1;
    }
    return sampleRate;
  }

  @Override
  public String toString() {
    return MessageFormat.format(TO_STRING_FORMAT, new Object[] { name, Integer.valueOf(n), Integer.valueOf(sum),
        Integer.valueOf(minimum), Integer.valueOf(maximum), Integer.valueOf(sum / n) });
  }

}
