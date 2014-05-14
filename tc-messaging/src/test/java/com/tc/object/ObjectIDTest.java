/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ObjectIDTest {

  @Test
  public void testIdentity() {
    int idValue = 1001;
    ObjectID id = new ObjectID(idValue);
    ObjectID clone = new ObjectID(idValue);

    assertIdentity(id, clone);
  }

  public void assertIdentity(ObjectID id, ObjectID clone) {
    assertNotSame(id, clone);
    assertEquals(id, clone);

    Set set = new HashSet();
    set.add(clone);

    assertTrue(set.contains(id));
    set.remove(id);
    assertEquals(0, set.size());

    Map map = new HashMap();
    Object o = new Object();
    map.put(id, o);
    assertEquals(o, map.get(clone));
  }

  @Test
  public void testGroupIDObjectID() {
    SecureRandom sr = new SecureRandom();
    long seed = sr.nextLong();
    Random r = new Random(seed);
    for (int gid = 0; gid < 255; gid++) {
      for (long oid = 0; oid < 600; oid++) {
        groupIDObjectIDTest(gid, oid);
      }
      for (int i = 0; i < 300; i++) {
        groupIDObjectIDTest(gid, nextRandomObjectID(r));
      }
      for (long oid = ObjectID.MAX_ID - 600; oid <= ObjectID.MAX_ID; oid++) {
        groupIDObjectIDTest(gid, oid);
      }
    }

    try {
      new ObjectID(ObjectID.MAX_ID + 1, 254);
      fail("Expected AssertionError: [seed = " + seed + "]");
    } catch (AssertionError a) {
      // expected
    }

    try {
      new ObjectID(ObjectID.MAX_ID, 255);
      fail("Expected AssertionError: [seed = " + seed + "]");
    } catch (AssertionError a) {
      // expected
    }
  }

  private long nextRandomObjectID(Random r) {
    long oid = -1;
    while (oid < 0 || oid > ObjectID.MAX_ID) {
      oid = r.nextLong();
    }
    return oid;
  }

  private void groupIDObjectIDTest(int gid, long oid) {
    ObjectID id = new ObjectID(oid, gid);
    assertEquals(gid, id.getGroupID());
    assertEquals(oid, id.getMaskedObjectID());
    ObjectID clone = new ObjectID(id.toLong());
    assertIdentity(id, clone);
    assertFalse(ObjectID.NULL_ID.equals(id));
  }
}
