/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;


/**
 * Encapsulates a time interval for Object.wait(...) and tryLock.
 */
public final class TimerSpec {

  private final static int UNITIALIZED_TIME_INTERVAL = -1;

  private final Signature signature;

  private long            millis;
  private final int             nanos;
  private long             mark                      = UNITIALIZED_TIME_INTERVAL;

  /**
   * Untimed interval
   */
  public TimerSpec() {
    this(NO_ARGS, UNITIALIZED_TIME_INTERVAL, UNITIALIZED_TIME_INTERVAL);
  }

  /**
   * Time interval specified in millis
   * @param millis Milliseconds to wait
   */
  public TimerSpec(long millis) {
    this(LONG, millis, UNITIALIZED_TIME_INTERVAL);
  }

  /**
   * Time interval specified in millis and nanos
   * @param millis Milliseconds
   * @param nanos Nanoseconds
   */
  public TimerSpec(long millis, int nanos) {
    this(LONG_INT, millis, nanos);
  }

  /**
   * Time interval specified by a signature
   * @param signature Method signature
   * @param millis Milliseconds
   * @param nanos Nanoseconds
   */
  private TimerSpec(Signature signature, long millis, int nanos) {
    this.signature = signature;

    if (signature == LONG) {
      if (millis < 0) { throw new IllegalArgumentException("Invalid milliseconds argument to wait(long): " + millis); }
    } else if (signature == LONG_INT) {
      if (millis < 0) { throw new IllegalArgumentException("Invalid milliseconds argument to wait(long, int): "
                                                           + millis); }
      if (nanos < 0) { throw new IllegalArgumentException("Invalid nanoseconds argument to wait(long, int): " + nanos); }
    }

    this.millis = millis;
    this.nanos = nanos;
  }

  /**
   * @return True if has timeout
   */
  public boolean hasTimeout() {
    return getSignature() != NO_ARGS;
  }

  /**
   * @return True if timeouts are > 0
   */
  public boolean needsToWait() {
    return millis > 0 || nanos > 0;
  }

  /**
   * @return Get millis timeout
   */
  public long getMillis() {
    return millis;
  }

  /**
   * @return Get nanos timeout
   */
  public int getNanos() {
    return nanos;
  }

  /**
   * @return Get method signature
   */
  public Signature getSignature() {
    return this.signature;
  }

  /**
   * Mark seen at current time
   */
  public void mark() {
    mark = System.currentTimeMillis();
  }

  /**
   * Adjust by removing time to wait by now-last mark.
   */
  public void adjust() {
    if (mark <= UNITIALIZED_TIME_INTERVAL || signature == NO_ARGS) return;
    long now = System.currentTimeMillis();
    millis -= (now - mark);

    if (millis <= 0) {
      millis = 1;
    }
  }

  @Override
  public String toString() {
    if (this.signature == NO_ARGS) { return this.signature.toString(); }

    StringBuffer rv = new StringBuffer("wait(");

    if (this.signature == LONG) {
      rv.append(getMillis());
    } else if (this.signature == LONG_INT) {
      rv.append(getMillis()).append(", ").append(getNanos());
    }

    rv.append(")");

    return rv.toString();
  }

  /** Signature for untimed wait */
  public static final Signature NO_ARGS  = new Signature("wait()", 0);
  /** Signature for 1 arg wait() */
  public static final Signature LONG     = new Signature("wait(long)", 1);
  /** Signature for 2 arg wait() */
  public static final Signature LONG_INT = new Signature("wait(long, int)", 2);

  public final static class Signature {
    private final String desc;
    private final int    argCount;

    private Signature(String desc, int numArgs) {
      this.desc = desc;
      this.argCount = numArgs;
    }

    public int getArgCount() {
      return this.argCount;
    }

    @Override
    public String toString() {
      return desc;
    }

  }

}
