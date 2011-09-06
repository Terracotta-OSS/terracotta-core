/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.object.ObjectID;
import com.tc.test.TCTestCase;

public class OidBitsArrayMapSimpleTest extends TCTestCase {
  public OidBitsArrayMapSimpleTest() {
    //
  }

  public void testNegativeOids() {
    OidBitsArrayMap oids = new OidBitsArrayMapImpl(8);
    ObjectID[] testOids = new ObjectID[] { ObjectID.NULL_ID, new ObjectID(-2), new ObjectID(-511), new ObjectID(-512),
        new ObjectID(-1023), new ObjectID(0), new ObjectID(Integer.MIN_VALUE), new ObjectID(Integer.MAX_VALUE),
        new ObjectID(Long.MIN_VALUE), new ObjectID(Long.MAX_VALUE) };

    for (ObjectID oid : testOids) {
      System.out.println("XXX Testing ObjectID " + oid.toLong());
      Assert.assertFalse(oids.contains(oid));
      oids.getAndSet(oid, null);
      Assert.assertTrue(oids.contains(oid));
      oids.getAndClr(oid, null);
      Assert.assertFalse(oids.contains(oid));
    }
  }

  public void testMixedOids() {
    OidBitsArrayMap oids = new OidBitsArrayMapImpl(8);

    ObjectID[] positiveOids = new ObjectID[] { new ObjectID(0), new ObjectID(1), new ObjectID(2), new ObjectID(3),
        new ObjectID(4), new ObjectID(511), new ObjectID(512), new ObjectID(513), new ObjectID(1023),
        new ObjectID(1024), new ObjectID(Integer.MAX_VALUE), new ObjectID(Long.MAX_VALUE) };
    ObjectID[] negativeOids = new ObjectID[] { ObjectID.NULL_ID, new ObjectID(-2), new ObjectID(-3), new ObjectID(-4),
        new ObjectID(-511), new ObjectID(-512), new ObjectID(-513), new ObjectID(-1023), new ObjectID(-1024),
        new ObjectID(Integer.MIN_VALUE), new ObjectID(Long.MIN_VALUE) };

    for (ObjectID oid : positiveOids) {
      System.out.println("XXX Add ObjectID " + oid.toLong());
      oids.getAndSet(oid, null);
    }

    for (ObjectID oid : negativeOids) {
      Assert.assertFalse("Shall not contain " + oid.toLong(), oids.contains(oid));
      System.out.println("XXX Add ObjectID " + oid.toLong());
      oids.getAndSet(oid, null);
    }

    for (ObjectID oid : negativeOids) {
      Assert.assertTrue("Shall contain " + oid.toLong(), oids.contains(oid));
      System.out.println("XXX Remove ObjectID " + oid.toLong());
      oids.getAndClr(oid, null);
      Assert.assertFalse("Shall not contain " + oid.toLong(), oids.contains(oid));
    }

    for (ObjectID oid : positiveOids) {
      Assert.assertTrue("Shall contain " + oid.toLong(), oids.contains(oid));
    }
  }

  public void testExtremeValues() {
    OidBitsArrayMapImpl oids = new OidBitsArrayMapImpl(8);

    System.out.println("XXX Test Long.MIN_VALUE");
    ObjectID oid = new ObjectID(Long.MIN_VALUE);
    Long base = oids.oidIndex(oid.toLong());
    Assert
        .assertTrue("Long.MIN_VALUE failed, expected " + oid.toLong() + " but got " + base, base.equals(oid.toLong()));

    System.out.println("XXX Test Long.MAX_VALUE");
    oid = new ObjectID(Long.MAX_VALUE);
    base = oids.oidIndex(oid.toLong());
    Assert.assertTrue("Long.MAX_VALUE failed, expected " + oid.toLong() + " but got " + base, base
        .equals(oid.toLong() - 511));

    System.out.println("XXX Test Integer.MIN_VALUE");
    oid = new ObjectID(Integer.MIN_VALUE);
    base = oids.oidIndex(oid.toLong());
    Assert.assertTrue("Inetger.MIN_VALUE failed, expected " + oid.toLong() + " but got " + base, base.equals(oid
        .toLong()));

    System.out.println("XXX Test Integer.MAX_VALUE");
    oid = new ObjectID(Integer.MAX_VALUE);
    base = oids.oidIndex(oid.toLong());
    Assert.assertTrue("Inetger.MAX_VALUE failed, expected " + oid.toLong() + " but got " + base, base.equals(oid
        .toLong() - 511));

    System.out.println("XXX Test 1");
    oid = new ObjectID(1);
    base = oids.oidIndex(oid.toLong());
    Assert.assertTrue("Expected " + oid.toLong() + " but got " + base, base.equals(0L));

    System.out.println("XXX Test -1");
    oid = new ObjectID(-1);
    base = oids.oidIndex(oid.toLong());
    Assert.assertTrue("Expected " + oid.toLong() + " but got " + base, base.equals(-512L));

    System.out.println("XXX Test -512");
    oid = new ObjectID(-512);
    base = oids.oidIndex(oid.toLong());
    Assert.assertTrue("Expected " + oid.toLong() + " but got " + base, base.equals(-512L));

    System.out.println("XXX Test -513");
    oid = new ObjectID(-513);
    base = oids.oidIndex(oid.toLong());
    Assert.assertTrue("Expected " + oid.toLong() + " but got " + base, base.equals(-1024L));

    System.out.println("XXX Test -1024");
    oid = new ObjectID(-1024);
    base = oids.oidIndex(oid.toLong());
    Assert.assertTrue("Expected " + oid.toLong() + " but got " + base, base.equals(-1024L));
  }

}