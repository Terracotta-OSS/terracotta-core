/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.Assert;

/**
 * Utilities for use in testing serialization.
 */
public class SerializationTestUtil {

  private static final TCLogger logger = TCLogging.getLogger(SerializationTestUtil.class);

  public static void testSerialization(Object o) throws Exception {
    testSerialization(o, false, false);
  }

  public static void testSerializationWithRestore(Object o) throws Exception {
    testSerialization(o, true, false);
  }

  public static void testSerializationAndEquals(Object o) throws Exception {
    testSerialization(o, true, true);
  }

  public static void testSerialization(Object o, boolean checkRestore, boolean checkEquals) throws Exception {
    Assert.assertNotNull(o);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);

    oos.writeObject(o);
    oos.flush();

    logger.debug("Object " + o + " of class " + o.getClass() + " serialized to " + baos.toByteArray().length
                 + " bytes.");

    if (checkRestore) {
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      ObjectInputStream ois = new ObjectInputStream(bais);

      Object restored = ois.readObject();

      if (checkEquals) {
        Assert.assertEquals(o, restored);
        Assert.assertEquals(restored, o);

        Assert.assertEquals(o.hashCode(), restored.hashCode());

        Assert.assertNotSame(o, restored);
      }
    }
  }

}