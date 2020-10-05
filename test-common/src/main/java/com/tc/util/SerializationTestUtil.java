/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;

/**
 * Utilities for use in testing serialization.
 */
public class SerializationTestUtil {

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
