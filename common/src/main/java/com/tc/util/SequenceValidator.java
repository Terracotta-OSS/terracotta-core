/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class SequenceValidator {

  private static final TCLogger logger    = TCLogging.getLogger(SequenceValidator.class);

  private final Map             sequences = new HashMap();
  private final long            start;

  public SequenceValidator(long start) {
    this.start = start;
  }

  // Used in tests
  public synchronized boolean isNext(Object key, SequenceID candidate) {
    if (candidate.isNull()) { return true; }
    Sequencer sequencer = getOrCreate(key);
    return sequencer.isNext(candidate);
  }

  public synchronized void setCurrent(Object key, SequenceID next) throws InvalidSequenceIDException {
    if (key == null || SequenceID.NULL_ID.equals(next)) { return; }
    Sequencer s = getOrCreate(key);
    s.setCurrent(next);
  }

  // Used in tests
  public synchronized SequenceID getCurrent(Object key) {
    Sequencer s = (Sequencer) this.sequences.get(key);
    Assert.assertNotNull(s);
    return s.getCurrent();

  }

  public synchronized void initSequence(Object key, Collection sequenceIDs) {
    Assert.assertFalse(this.sequences.containsKey(key));
    this.sequences.put(key, new Sequencer(key, this.start, sequenceIDs));
  }

  public synchronized void remove(Object key) {
    this.sequences.remove(key);
  }

  public synchronized int size() {
    return this.sequences.size();
  }

  private Sequencer getOrCreate(Object key) {
    Sequencer sequencer = (Sequencer) this.sequences.get(key);
    if (sequencer == null) {
      sequencer = new Sequencer(key, this.start);
      this.sequences.put(key, sequencer);
    }
    return sequencer;
  }

  private static class Sequencer {

    SortedSet  sequenceIDs;
    SequenceID current;

    Sequencer(Object key, long start, Collection sequenceIDs) {
      if (sequenceIDs.size() > 0) {
        this.sequenceIDs = new TreeSet(SequenceID.COMPARATOR);
        this.sequenceIDs.addAll(sequenceIDs);
        // There shouldn't be any duplicates
        if(this.sequenceIDs.size() != sequenceIDs.size()) {
          throw new AssertionError("Size not equal " + this.sequenceIDs.size() + " != " + sequenceIDs.size());
        } 
        this.current = new SequenceID(start);
      } else {
        throw new AssertionError("Sequencer should be set to a valid SequenceID Sequence !!!");
      }
      logger.info("Setting initial Sequence IDs for " + key + " current = " + this.current + " next = "
                  + this.sequenceIDs.first() + " next.total = " + this.sequenceIDs.size());
    }

    Sequencer(Object key, long start) {
      this.current = new SequenceID(start);
      logger.debug("Setting initial Sequence IDs for " + key + " current = " + this.current);
    }

    public boolean isNext(SequenceID candidate) {
      if (candidate.toLong() <= this.current.toLong()) {
        logger.warn("Sequence IDs = " + this.sequenceIDs + " current = " + this.current + " but candidate = "
                    + candidate);
        return false;
      }
      if (this.sequenceIDs == null) {
        return this.current.toLong() + 1 == candidate.toLong();
      } else {
        return (((SequenceID) this.sequenceIDs.first()).toLong() == candidate.toLong());
      }
    }

    public void setCurrent(SequenceID next) throws InvalidSequenceIDException {
      if (!isNext(next)) { throw new InvalidSequenceIDException("Trying to set to " + next + " but current = "
                                                                + this.current); }
      if (this.sequenceIDs != null) {
        if ((this.current.toLong() + 1) != next.toLong()) {
          logger.info("Current Sequence jumping from current = " + this.current + " to next = " + next);
        }
        this.sequenceIDs.headSet(next.next()).clear();
        if (this.sequenceIDs.size() == 0) {
          this.sequenceIDs = null;
        }
      }
      this.current = next;
    }

    public SequenceID getCurrent() {
      return this.current;
    }
  }
}
