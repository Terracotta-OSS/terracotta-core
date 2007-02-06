/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
    if (candidate.isNull()) return true;
    Sequencer sequencer = getOrCreate(key);
    return sequencer.isNext(candidate);
  }

  public synchronized void setCurrent(Object key, SequenceID next) throws InvalidSequenceIDException {
    if (key == null || SequenceID.NULL_ID.equals(next)) return;
    Sequencer s = getOrCreate(key);
    s.setCurrent(next);
  }

  // Used in tests
  public synchronized SequenceID getCurrent(Object key) {
    Sequencer s = (Sequencer) sequences.get(key);
    Assert.assertNotNull(s);
    return s.getCurrent();

  }

  public synchronized void initSequence(Object key, Collection sequenceIDs) {
    Assert.assertFalse(sequences.containsKey(key));
    sequences.put(key, new Sequencer(key, start, sequenceIDs));
  }

  public synchronized void remove(Object key) {
    sequences.remove(key);
  }

  public synchronized int size() {
    return sequences.size();
  }

  private Sequencer getOrCreate(Object key) {
    Sequencer sequencer = (Sequencer) sequences.get(key);
    if (sequencer == null) {
      sequencer = new Sequencer(key, start);
      sequences.put(key, sequencer);
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
        current = new SequenceID(start);
      } else {
        throw new AssertionError("Sequencer should be set to a valid SequenceID Sequence !!!");
      }
      logger.info("Setting initial Sequence IDs for " + key + " current = " + current + " next = " + this.sequenceIDs);
    }

    Sequencer(Object key, long start) {
      current = new SequenceID(start);
      logger.debug("Setting initial Sequence IDs for " + key + " current = " + current);
    }

    public boolean isNext(SequenceID candidate) {
      if (candidate.toLong() <= current.toLong()) {
        logger.warn("Sequence IDs = " + sequenceIDs + " current = " + current + " but candidate = " + candidate);
        return false;
      }
      if (sequenceIDs == null) {
        return current.toLong() + 1 == candidate.toLong();
      } else {
        return (((SequenceID) sequenceIDs.first()).toLong() == candidate.toLong());
      }
    }

    public void setCurrent(SequenceID next) throws InvalidSequenceIDException {
      if (!isNext(next)) { throw new InvalidSequenceIDException("Trying to set to " + next + " but current = "
                                                                + current); }
      if (sequenceIDs != null) {
        logger.info("Setting current Sequence IDs from current = " + current + " to next = " + next);
        sequenceIDs.headSet(next.next()).clear();
        if (sequenceIDs.size() == 0) {
          sequenceIDs = null;
        }
      }
      current = next;
    }

    public SequenceID getCurrent() {
      return current;
    }
  }
}
