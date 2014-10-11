package com.tc.util;

import org.junit.Test;

import com.tc.object.ObjectID;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BitSetObjectIDSetTest extends ObjectIDSetTestBase {
  @Override
  protected ObjectIDSet create() {
    return new BitSetObjectIDSet();
  }

  @Override
  protected ObjectIDSet create(final Collection<ObjectID> copy) {
    return new BitSetObjectIDSet(copy);
  }

  @Test
  public void testCloneExpanding() throws Exception {
    ExpandingBitSetObjectIDSet expanding = new ExpandingBitSetObjectIDSet();
    for (int i = -1000000; i < 1000000; i++) {
      expanding.add(new ObjectID(i));
    }

    BitSetObjectIDSet bitSetOIDSet = new BitSetObjectIDSet(expanding);
    Iterator<ObjectID> iter = bitSetOIDSet.iterator();
    for (int i = -1000000; i < 1000000; i++) {
      assertTrue(iter.hasNext());
      assertEquals(i, iter.next().toLong());
    }
    assertFalse(iter.hasNext());
  }

  @Test
  public void testPerformance() {
    long seed = new SecureRandom().nextLong();
    final Random r = new Random(seed);
    ObjectIDSet set = create();
    Set<ObjectID> hashSet = new HashSet<ObjectID>();

    for (int i = 0; i < 800000; i++) {
      final long l = r.nextLong();
      final ObjectID id = new ObjectID(l);
      hashSet.add(id);
    }

    final long t1 = System.currentTimeMillis();
    for (final ObjectID objectID : hashSet) {
      set.add(objectID);
    }
    final long t2 = System.currentTimeMillis();

    for (final ObjectID objectID : hashSet) {
      set.contains(objectID);
    }
    final long t3 = System.currentTimeMillis();

    for (final ObjectID objectID : hashSet) {
      set.remove(objectID);
    }
    final long t4 = System.currentTimeMillis();

    final Set<ObjectID> hashSet2 = new HashSet<ObjectID>();
    for (int i = 0; i < 800000; i++) {
      hashSet2.add(new ObjectID(r.nextLong()));
    }

    final long t5 = System.currentTimeMillis();
    set.addAll(hashSet);
    final long t6 = System.currentTimeMillis();
    set.removeAll(hashSet);
    final long t7 = System.currentTimeMillis();

    for (int i = 0; i < 800000; i++) {
      set.add(new ObjectID(r.nextLong()));
    }

    final long t8 = System.currentTimeMillis();
    for (Iterator<ObjectID> i = set.iterator(); i.hasNext(); ) {
      i.next();
    }
    final long t9 = System.currentTimeMillis();
    int j = 0;
    for (Iterator<ObjectID> i = set.iterator(); i.hasNext(); ) {
      i.next();
      if (j++ % 2 == 0) {
        i.remove();
      }
    }
    final long t10 = System.currentTimeMillis();

    System.out.println("Times for ObjectIDSet type " + set.getClass().getSimpleName());
    System.out.println("add-> " + (t2 - t1) + " contains->"
                       + (t3 - t2) + " remove->" + (t4 - t3));
    System.out.println("addAll-> " + (t6 - t5) + " removeAll->"
                       + (t7 - t6));
    System.out.println("iteration->" + (t9 - t8) + " iterator remove->" + (t10 - t9));
  }

}