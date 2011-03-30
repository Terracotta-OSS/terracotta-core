/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import org.mockito.Mockito;

import com.tc.object.ObjectID;
import com.tc.objectserver.storage.api.TCMapsDatabase;
import com.tc.util.Assert;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class TCPersistableMapTest extends TestCase {

  TCPersistableMap pmap;
  HashMap          backingMap;
  HashMap          delta;
  TCMapsDatabase   db;

  @Override
  public void setUp() {
    pmap = new TCPersistableMap(new ObjectID(5), backingMap = new HashMap(), delta = new HashMap());
    db = Mockito.mock(TCMapsDatabase.class);
  }

  public void testSize() throws Exception {
    pmap.put("k1", "v1");
    assertEquals(1, pmap.size());
    pmap.commit(null, null, db);
    assertEquals(1, pmap.size());
    pmap.put("k2", "v2");
    assertEquals(2, pmap.size());

    pmap.put("k1", "v2");
    assertEquals(2, pmap.size());
    pmap.remove("k1");
    assertEquals(1, pmap.size());
    pmap.put("k1", "v2");
    assertEquals(2, pmap.size());
  }

  public void testIterator() throws Exception {
    HashMap m = new HashMap();
    loadMap(pmap);
    loadMap(m);
    assertMapEquals(m, pmap);

    pmap.commit(null, null, db);

    loadMap(pmap);
    loadMap(m);
    assertMapEquals(m, pmap);

  }

  private void assertMapEquals(Map m1, Map m2) {
    assertEquals(m1.size(), m2.size());
    assertEquals(m1, m2);
    equals(m1.keySet(), m2.keySet());
    equals(m1.values(), m2.values());
    Assert.assertEquals(m1.entrySet(), m2.entrySet());
  }

  // This implementation does not care about the order
  private void equals(final Collection c1, final Collection c2) {
    Assert.assertEquals(c1.size(), c2.size());
    Assert.assertTrue(c1.containsAll(c2));
    Assert.assertTrue(c2.containsAll(c1));
  }

  private void equals(final Set s1, final Set s2) {
    Assert.assertEquals(s1, s2);
    equals(Arrays.asList(s1.toArray()), Arrays.asList(s2.toArray()));
  }

  private void loadMap(Map m) {
    for (int i = 0; i < 200; i++) {
      m.put("ik=" + i, "iv=" + i);
    }
  }

  public void testRandom() {
    //
  }

  public void testRemoves() {

    for (int i = 0; i < 1000000; i++) {
      if (i % 10000 == 0) {
        System.out.println("Put : " + i + " size : " + pmap.size());
      }
      String k = new String(
                            i
                                + " - This is a long string with a lot of charaters to take up a lot of space.;ah ba;jfiirehirhughrughruhgurehgurgrjkegbrjegrgrgurgujrbgjrbjfbjdsk");
      pmap.put(k, "v-" + i);
      pmap.remove(k);
      assertEquals(pmap.size(), 0);
    }
  }
}
