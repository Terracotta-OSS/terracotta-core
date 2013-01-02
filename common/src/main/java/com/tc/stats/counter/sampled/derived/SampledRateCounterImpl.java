/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter.sampled.derived;

import com.tc.stats.counter.sampled.SampledCounterImpl;

public class SampledRateCounterImpl extends SampledCounterImpl implements SampledRateCounter {

  private static final String OPERATION_NOT_SUPPORTED_MSG = "This operation is not supported. Use SampledCounter Or Counter instead";

  private long                numeratorValue;
  private long                denominatorValue;

  public SampledRateCounterImpl(SampledRateCounterConfig config) {
    super(config);
  }

  @Override
  public synchronized void setValue(long numerator, long denominator) {
    this.numeratorValue = numerator;
    this.denominatorValue = denominator;
  }

  @Override
  public synchronized void increment(long numerator, long denominator) {
    this.numeratorValue += numerator;
    this.denominatorValue += denominator;
  }

  @Override
  public synchronized void decrement(long numerator, long denominator) {
    this.numeratorValue -= numerator;
    this.denominatorValue -= denominator;
  }

  @Override
  public synchronized void setDenominatorValue(long newValue) {
    this.denominatorValue = newValue;
  }

  @Override
  public synchronized void setNumeratorValue(long newValue) {
    this.numeratorValue = newValue;
  }

  @Override
  public synchronized long getValue() {
    return denominatorValue == 0 ? 0 : (numeratorValue / denominatorValue);
  }

  @Override
  public synchronized long getAndReset() {
    long prevVal = getValue();
    setValue(0, 0);
    return prevVal;
  }

  // ====== unsupported operations. These operations need multiple params for this class

  @Override
  public long getAndSet(long newValue) {
    throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED_MSG);
  }

  @Override
  public synchronized void setValue(long newValue) {
    throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED_MSG);
  }

  @Override
  public long decrement() {
    throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED_MSG);
  }

  @Override
  public long decrement(long amount) {
    throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED_MSG);
  }

  public long getMaxValue() {
    throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED_MSG);
  }

  public long getMinValue() {
    throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED_MSG);
  }

  @Override
  public long increment() {
    throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED_MSG);
  }

  @Override
  public long increment(long amount) {
    throw new UnsupportedOperationException(OPERATION_NOT_SUPPORTED_MSG);
  }

}
