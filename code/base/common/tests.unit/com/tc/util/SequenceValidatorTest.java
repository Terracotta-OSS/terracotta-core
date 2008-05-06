/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.ArrayList;
import java.util.Random;

import junit.framework.TestCase;

public class SequenceValidatorTest extends TestCase {

  private static final int  INITIAL_VALUE = 0;

  private SequenceValidator sv;

  public void setUp() throws Exception {
    this.sv = new SequenceValidator(INITIAL_VALUE);
  }

  public void tests() throws Exception {

    SequenceID one = new SequenceID(1);
    SequenceID two = new SequenceID(2);
    SequenceID three = new SequenceID(3);

    Object key1 = new Object();
    Object key2 = new Object();
    assertEquals(0, sv.size());
    assertTrue(sv.isNext(key1, one));
    assertTrue(sv.isNext(key2, one));
    assertFalse(sv.isNext(key1, two));
    assertFalse(sv.isNext(key2, two));

    sv.setCurrent(key1, one);

    assertTrue(sv.isNext(key2, one));
    assertFalse(sv.isNext(key2, two));
    assertTrue(sv.isNext(key1, two));
    assertFalse(sv.isNext(key1, three));

    sv.remove(key1);
    assertEquals(1, sv.size());
    sv.remove(key2);
    assertEquals(0, sv.size());
    assertTrue(sv.isNext(key1, one));
    assertTrue(sv.isNext(key2, one));
  }

  public void testSetCurrent() throws Exception {

    Object key = new Object();
    SequenceID sid = new SequenceID(1);
    for (int i = 0; i < 50; i++) {
      assertTrue(sv.isNext(key, sid));
      sv.setCurrent(key, sid);
      sid = sid.next();
      try {
        sv.setCurrent(key, sid.next());
        fail();
      } catch (InvalidSequenceIDException er) {
        // expected
      }
    }
  }

  public void testDisjointSequences() throws Exception {

    int start = 100;
    ArrayList sequences = createRandomSequences(start);

    Object key1 = new Object();

    assertEquals(0, sv.size());
    sv.initSequence(key1, sequences);

    try {
      sv.initSequence(key1, new ArrayList());
      fail("Should have thrown an Exception");
    } catch (TCAssertionError er) {
      // expected
    }

    SequenceID current = sv.getCurrent(key1);
    assertTrue(current.toLong() == INITIAL_VALUE);

    while (sequences.size() > 0) {
      SequenceID next = new SequenceID(start);
      if (sequences.contains(next)) {
        assertTrue(sv.isNext(key1, next));
        sequences.remove(next);
        sv.setCurrent(key1, next);
      } else {
        assertFalse(sv.isNext(key1, next));
      }
      start++;
    }
    int i = 100;
    while (i-- > 0) {
      SequenceID next = new SequenceID(start);
      assertTrue(sv.isNext(key1, next));
      sv.setCurrent(key1, next);
      start++;
    }

  }

  private ArrayList createRandomSequences(int inital) {
    Random r = new Random();
    ArrayList sequences = new ArrayList();
    int count = r.nextInt(100) + 1;
    while (count-- > 0) {
      sequences.add(new SequenceID(inital));
      inital += r.nextInt(5) + 1;
    }
    return sequences;
  }
}
