/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.transactions;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.tc.util.Assert;
import com.tc.util.stringification.OurStringBuilder;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * The standard implementation of {@link TransactionalObject}. See that class for details.
 * </p>
 * <p>
 * This implementation allows reads to also be checked with a slight relaxation of the rules, called <em>slop</em>:
 * if, when you read, you get a value that's invalid, <em>but</em> would have been valid were the read issued at any
 * point up to <code>slop</code> milliseconds ago, it's treated as valid. This is used for our tests that involve the
 * Terracotta database listener, as invalidations take a nonzero amount of time to travel.
 * </p>
 * <p>
 * Be warned: this class's implementation is quite tricky. Make changes with care, and re-run the test whenever you
 * change it.
 * </p>
 * <p>
 * (The set of all values you could possibly read includes all values the object has had from the current time less the
 * slop up to the start of the read, in addition to all values for which writes have at least been started (by calling
 * {@link startWrite(Object)} for them) before the moment you called {@link #endRead(Context, Object)}.)
 * </p>
 * <p>
 * This class works in a bit of a funny way: if <code>slop</code> is exactly zero, we need to maintain order of method
 * calls &mdash; many method calls can happen without {@link System#currentTimeMillis()} changing at all. To do that, we
 * simply use a {@link Timing} object, rather than a raw <code>long</code>, to store our times; there's one
 * implementation (which we use if <code>slop</code> is nonzero) that uses just the raw time from
 * {@link System#currentTimeMillis()}, while there's another (which we use if <code>slop</code> is exactly zero) that
 * uses both the raw time and an internally-generated sequence number, to distinguish calls that happen at the same
 * 'time' (according to {@link System#currentTimeMillis()}).
 */
public class StandardTransactionalObject implements TransactionalObject {

  private static interface Timing {
    boolean isAfter(Timing other);

    boolean isBefore(Timing other);

    boolean isAfterOrEqual(Timing other);

    boolean isBeforeOrEqual(Timing other);

    long rawTime();
  }

  private static class SequencedTiming implements Timing {
    private final long actualTime;
    private final long sequenceNumber;

    public SequencedTiming(long actualTime, long sequenceNumber) {
      this.actualTime = actualTime;
      this.sequenceNumber = sequenceNumber;
    }

    public boolean isAfter(Timing rawOther) {
      SequencedTiming other = (SequencedTiming) rawOther;
      if (other == null) return true;
      return this.actualTime > other.actualTime
             || (this.actualTime == other.actualTime && this.sequenceNumber > other.sequenceNumber);
    }

    public boolean isBefore(Timing rawOther) {
      SequencedTiming other = (SequencedTiming) rawOther;
      if (other == null) return false;
      return this.actualTime < other.actualTime
             || (this.actualTime == other.actualTime && this.sequenceNumber < other.sequenceNumber);
    }

    public boolean isAfterOrEqual(Timing other) {
      return isAfter(other) || equals(other);
    }

    public boolean isBeforeOrEqual(Timing other) {
      return isBefore(other) || equals(other);
    }

    public boolean equals(Object that) {
      if (!(that instanceof SequencedTiming)) return false;
      SequencedTiming thatTiming = (SequencedTiming) that;
      return new EqualsBuilder().append(this.actualTime, thatTiming.actualTime).append(this.sequenceNumber,
                                                                                       thatTiming.sequenceNumber)
          .isEquals();
    }

    public long rawTime() {
      return this.actualTime;
    }

    public String toString() {
      return new OurStringBuilder(this, OurStringBuilder.COMPACT_STYLE).append("actual time", this.actualTime)
          .append("sequence number", this.sequenceNumber).toString();
    }
  }

  private static class BasicTiming implements Timing {
    private final long time;

    public BasicTiming(long time) {
      this.time = time;
    }

    public boolean isAfter(Timing rawOther) {
      if (rawOther == null) return true;
      BasicTiming other = (BasicTiming) rawOther;
      return this.time > other.time;
    }

    public boolean isBefore(Timing rawOther) {
      if (rawOther == null) return false;
      BasicTiming other = (BasicTiming) rawOther;
      return this.time < other.time;
    }

    public boolean isAfterOrEqual(Timing other) {
      return isAfter(other) || equals(other);
    }

    public boolean isBeforeOrEqual(Timing other) {
      return isBefore(other) || equals(other);
    }

    public boolean equals(Object that) {
      if (!(that instanceof BasicTiming)) return false;
      BasicTiming thatTiming = (BasicTiming) that;
      return new EqualsBuilder().append(this.time, thatTiming.time).isEquals();
    }

    public long rawTime() {
      return this.time;
    }

    public String toString() {
      return new OurStringBuilder(this, OurStringBuilder.COMPACT_STYLE).append("time", this.time).toString();
    }
  }

  private static class Write implements Context {
    private final Object value;
    private final Timing startedAt;
    private Timing       committedAt;

    public Write(Object value, Timing startedAt) {
      Assert.assertNotNull(startedAt);
      this.value = value;
      this.startedAt = startedAt;
      this.committedAt = null;
    }

    public void commit(Timing now) {
      Assert.eval(this.committedAt == null);
      this.committedAt = now;
    }

    public Timing startedAt() {
      return this.startedAt;
    }

    public Timing committedAt() {
      return this.committedAt;
    }

    public boolean isCommitted() {
      return this.committedAt != null;
    }

    public Object value() {
      return this.value;
    }

    public String toString() {
      return new OurStringBuilder(this, OurStringBuilder.COMPACT_STYLE).append("value", value).append("started at",
                                                                                                      startedAt)
          .append("committed at", committedAt).toString();
    }
  }

  private static class Read implements Context {
    private final Timing startedAt;

    public Read(Timing startedAt) {
      Assert.eval(startedAt != null);
      this.startedAt = startedAt;
    }

    public Timing startedAt() {
      return this.startedAt;
    }

    public String toString() {
      return new OurStringBuilder(this, OurStringBuilder.COMPACT_STYLE).append("started at", startedAt).toString();
    }
  }

  private static final long DEFAULT_GC_SLOP = 60 * 1000;

  private final String      name;
  private final long        slop;
  private final long        gcSlop;
  private final Set         currentWrites;
  private final Set         currentReads;
  private long              lastSequence;

  public StandardTransactionalObject(String name, long slop, long gcSlop, Object initialValue, long now) {
    Assert.assertNotBlank(name);
    Assert.eval(slop >= 0);
    Assert.eval(gcSlop >= 0);
    this.name = name;
    this.slop = slop;
    this.gcSlop = gcSlop;
    this.currentWrites = new HashSet();
    this.currentReads = new HashSet();
    this.lastSequence = 0;
    endWrite(startWrite(initialValue, now), now);
  }

  public StandardTransactionalObject(String name, long slop, Object initialValue, long now) {
    this(name, slop, DEFAULT_GC_SLOP, initialValue, now);
  }

  public StandardTransactionalObject(String name, long slop, Object initialValue) {
    this(name, slop, initialValue, System.currentTimeMillis());
  }

  public StandardTransactionalObject(String name, Object initialValue, long now) {
    this(name, 0, initialValue, now);
  }

  public StandardTransactionalObject(String name, Object initialValue) {
    this(name, 0, initialValue, System.currentTimeMillis());
  }

  private Timing createTiming(long time) {
    return createTiming(time, 0);
  }

  private Timing createTiming(long time, long offset) {
    if (this.slop == 0) return new SequencedTiming(time + offset, nextSequence());
    else return new BasicTiming(time + offset);
  }

  private Timing createTiming(Timing time, long offset) {
    if (this.slop == 0) return new SequencedTiming(((SequencedTiming) time).actualTime + offset, nextSequence());
    else return new BasicTiming(((BasicTiming) time).time + offset);
  }

  private synchronized long nextSequence() {
    return ++this.lastSequence;
  }

  public synchronized Context startWrite(Object value) {
    return startWrite(value, System.currentTimeMillis());
  }

  public synchronized Context startWrite(Object value, long now) {
    Assert.eval(now >= 0);

    gc(now);

    Write out = new Write(value, createTiming(now));
    this.currentWrites.add(out);
    return out;
  }

  public synchronized void endWrite(Context rawWrite) {
    endWrite(rawWrite, System.currentTimeMillis());
  }

  public synchronized void endWrite(Context rawWrite, long now) {
    Assert.assertNotNull(rawWrite);
    Assert.eval(now >= 0);

    gc(now);

    Write write = (Write) rawWrite;
    Assert.eval("You can't commit a write before you started it, buddy.", createTiming(now)
        .isAfterOrEqual(write.startedAt()));
    write.commit(createTiming(now));
  }

  public synchronized Context startRead() {
    return startRead(System.currentTimeMillis());
  }

  public synchronized Context startRead(long now) {
    Assert.eval(now >= 0);

    gc(now);

    Read out = new Read(createTiming(now));
    this.currentReads.add(out);
    return out;
  }

  public synchronized void endRead(Context rawRead, Object result) {
    endRead(rawRead, result, System.currentTimeMillis());
  }

  public synchronized void endRead(Context rawRead, Object result, long now) {
    Assert.assertNotNull(rawRead);
    Assert.eval(now >= 0);

    gc(now);

    Read read = (Read) rawRead;

    Set withoutTooOld = removeNotInEffectAsOfTime(this.currentWrites, createTiming(read.startedAt(), -this.slop));
    Set potentials = removeTooYoung(withoutTooOld, createTiming(now));

    // System.err.println("endRead(" + rawRead + ", " + result + ", " + now + "),: source " + this.currentWrites
    // + ", potentials " + potentials);

    this.currentReads.remove(read);

    Iterator iter = potentials.iterator();
    while (iter.hasNext()) {
      if (valueEquals(result, ((Write) iter.next()).value())) return;
    }

    throw Assert.failure("Your read on object " + this + " was incorrect. You read the value " + result
                         + " at some point between " + read.startedAt() + " and " + now
                         + ", but the only writes that were in effect during that period are " + potentials);
  }

  private synchronized Set removeNotInEffectAsOfTime(Set source, Timing effectiveTime) {
    Set out = new HashSet();

    // Go find the latest start of all the writes that have committed before our cutoff time.
    Timing latestStart = null;
    Iterator sourceIter = source.iterator();
    while (sourceIter.hasNext()) {
      Write sourceWrite = (Write) sourceIter.next();
      if (!sourceWrite.isCommitted() || sourceWrite.committedAt().isAfterOrEqual(effectiveTime)) continue;
      if (latestStart == null || latestStart.isBefore(sourceWrite.startedAt())) latestStart = sourceWrite.startedAt();
    }

    // Go filter out those that were committed before that start -- they couldn't possibly be affecting us any more.
    sourceIter = source.iterator();
    while (sourceIter.hasNext()) {
      Write sourceWrite = (Write) sourceIter.next();
      if (sourceWrite.isCommitted() && sourceWrite.committedAt().isBefore(latestStart)) continue;
      out.add(sourceWrite);
    }

    return out;
  }

  private synchronized Set removeTooYoung(Set source, Timing notAfter) {
    Set out = new HashSet();

    Iterator sourceIter = source.iterator();
    while (sourceIter.hasNext()) {
      Write sourceWrite = (Write) sourceIter.next();
      if (sourceWrite.startedAt().isAfter(notAfter)) continue;
      out.add(sourceWrite);
    }

    return out;
  }

  private synchronized void gc(long now) {
    Timing gcTime = createTiming(Math.min(now, earliestReadStart()), -Math.max(this.slop, this.gcSlop));
    Set newSet = removeNotInEffectAsOfTime(this.currentWrites, gcTime);

    this.currentWrites.clear();
    this.currentWrites.addAll(newSet);
  }

  private synchronized long earliestReadStart() {
    long out = Long.MAX_VALUE;
    Iterator iter = this.currentReads.iterator();
    while (iter.hasNext()) {
      Read read = (Read) iter.next();
      out = Math.min(out, read.startedAt.rawTime());
    }

    return out;
  }

  private boolean valueEquals(Object one, Object two) {
    if ((one == null) != (two == null)) return false;
    if (one == null) return true;
    return one.equals(two);
  }

  public String toString() {
    return new OurStringBuilder(this, OurStringBuilder.COMPACT_STYLE).append("name", this.name).append("slop",
                                                                                                       this.slop)
        .toString();
  }

}
