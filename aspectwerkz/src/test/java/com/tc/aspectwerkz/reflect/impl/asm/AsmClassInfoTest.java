/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.reflect.impl.asm;

import com.tc.aspectwerkz.reflect.NullClassInfo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

public class AsmClassInfoTest extends TestCase {

  public void testIgnoreError() {
    assertEquals(0, AsmClassInfo.getIgnoreErrors(null).size());
    assertEquals(0, AsmClassInfo.getIgnoreErrors("").size());
    assertEquals(0, AsmClassInfo.getIgnoreErrors(" , ").size());
    assertEquals(1, AsmClassInfo.getIgnoreErrors("org.terracotta.").size());
    assertEquals(1, AsmClassInfo.getIgnoreErrors("org.terracotta. ").size());
    assertEquals(2, AsmClassInfo.getIgnoreErrors("org.terracotta. , tim.eck").size());
    assertEquals(3, AsmClassInfo.getIgnoreErrors("org.terracotta.,,tim.eck,bob").size());

    List<String> ignore = AsmClassInfo.getIgnoreErrors("org.terracotta.,org.apache.");
    assertTrue(AsmClassInfo.isIgnoreError(ignore, "org.terracotta.Foo"));
    assertTrue(AsmClassInfo.isIgnoreError(ignore, "org.terracotta.foo.Foo"));
    assertTrue(AsmClassInfo.isIgnoreError(ignore, "org.apache.Foo"));
    assertTrue(AsmClassInfo.isIgnoreError(ignore, "org.apache.foo.Foo"));
    assertFalse(AsmClassInfo.isIgnoreError(ignore, "org.terracottatech.Foo"));

    assertEquals(NullClassInfo.class, AsmClassInfo.getClassInfo("com.jinspired.Foo", newLoader()).getClass());
    assertEquals(NullClassInfo.class, AsmClassInfo.getClassInfo("com.jinspired.Foo", new byte[] {}, newLoader())
        .getClass());
    assertEquals(NullClassInfo.class,
                 AsmClassInfo.getClassInfo("com.jinspired.Foo", new ByteArrayInputStream(new byte[] {}), newLoader())
                     .getClass());
    assertEquals(NullClassInfo.class, AsmClassInfo.newClassInfo("com.jinspired.Foo", new byte[] {}, newLoader())
        .getClass());
  }

  private ClassLoader newLoader() {
    return new ClassLoader() {
      @Override
      public InputStream getResourceAsStream(String name) {
        return new ByteArrayInputStream(new byte[] {});
      }

    };
  }

}
