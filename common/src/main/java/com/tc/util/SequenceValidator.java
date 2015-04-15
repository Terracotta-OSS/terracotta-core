/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.util;

import com.tc.exception.InvalidSequenceIDException;
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
  
  public synchronized SequenceID advanceCurrent(Object key, SequenceID next) throws InvalidSequenceIDException {
    Sequencer s = getOrCreate(key);
    return s.advance(next);
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
    
    public SequenceID advance(SequenceID next) {
      if (this.sequenceIDs != null) {
        this.sequenceIDs.headSet(next.next()).clear();
        if (this.sequenceIDs.size() == 0) {
          this.sequenceIDs = null;
        }
      }
      this.current = next;
      return this.current;
    }

    public SequenceID getCurrent() {
      return this.current;
    }
  }
}
