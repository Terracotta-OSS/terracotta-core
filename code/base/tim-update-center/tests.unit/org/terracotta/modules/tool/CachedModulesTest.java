/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

public final class CachedModulesTest extends TestCase {

  public void testGetSiblinsWithSymbolicName() {
    Modules modules;
    String testData = "/testData02.xml";
    String tcVersion = "0.0.0";

    modules = load(testData, tcVersion);
    Module module = modules.get("foo.bar", "baz", "0.0.0");
    assertNotNull(module);
    List<Module> siblings = modules.getSiblings(module.symbolicName());
    assertFalse(siblings.isEmpty());
    assertEquals(5, siblings.size());
    assertTrue(siblings.contains(module));
    for (Module other : siblings) {
      assertEquals(module.symbolicName(), other.symbolicName());
    }

    module = modules.get("foo.bar", "quux", "0.0.5");
    assertNotNull(module);
    siblings = modules.getSiblings(module.symbolicName());
    assertFalse(siblings.isEmpty());
    assertEquals(5, siblings.size());
    assertTrue(siblings.contains(module));
    for (Module other : siblings) {
      assertEquals(module.symbolicName(), other.symbolicName());
    }

    tcVersion = "0.0.1";
    modules = load(testData, tcVersion);
    module = modules.get("foo.bar", "baz", "0.0.6");
    assertNotNull(module);
    assertFalse(modules.getSiblings(module.symbolicName()).isEmpty());

    assertTrue(modules.getSiblings(StringUtils.EMPTY).isEmpty());
  }

  public void testGetSiblings() {
    Modules modules;
    String testData = "/testData02.xml";
    String tcVersion = "0.0.0";

    modules = load(testData, tcVersion);
    Module module = modules.get("foo.bar", "baz", "0.0.0");
    assertNotNull(module);
    List<Module> siblings = modules.getSiblings(module);
    assertFalse(siblings.isEmpty());
    assertEquals(4, siblings.size());
    for (Module other : siblings) {
      assertFalse(module.equals(other));
      assertTrue(module.isSibling(other));
    }

    module = modules.get("foo.bar", "quux", "0.0.5");
    assertNotNull(module);
    siblings = modules.getSiblings(module);
    assertFalse(siblings.isEmpty());
    assertEquals(4, siblings.size());
    for (Module other : siblings) {
      assertFalse(module.equals(other));
      assertTrue(module.isSibling(other));
    }

    tcVersion = "0.0.1";
    modules = load(testData, tcVersion);
    module = modules.get("foo.bar", "baz", "0.0.6");
    assertNotNull(module);
    assertTrue(modules.getSiblings(module).isEmpty());

    module = null;
    assertTrue(modules.getSiblings(module).isEmpty());
  }

  public void testGet() {
    Modules modules;
    String testData = "/testData02.xml";
    String tcVersion = "0.0.0";

    modules = load(testData, tcVersion);
    Module module = modules.get("xxx.xxx", "xxx", "x.x.x");
    assertNull(module);

    module = modules.get("foo.bar", "baz", "0.0.0");
    assertNotNull(module);
    assertEquals("foo.bar.baz", module.symbolicName());
    assertEquals("0.0.0", module.version());

    module = modules.get("foo.bar", "quux", "0.0.5");
    assertNotNull(module);
    assertEquals("foo.bar.quux", module.symbolicName());
    assertEquals("0.0.5", module.version());

    tcVersion = "0.0.1";
    modules = load(testData, tcVersion);
    module = modules.get("foo.bar", "baz", "0.0.0");
    assertNull(module);

    module = modules.get("foo.bar", "quux", "0.0.5");
    assertNull(module);

    module = modules.get("foo.bar", "baz", "0.0.6");
    assertNotNull(module);
    assertEquals("foo.bar.baz", module.symbolicName());
    assertEquals("0.0.6", module.version());
  }

  public void testList() {
    Modules modules;
    String testData = "/testData02.xml";
    String tcVersion = "x.x.x";

    modules = load(testData, tcVersion);
    assertTrue(modules.list().isEmpty());

    tcVersion = "0.0.0";
    modules = load(testData, tcVersion);
    List<Module> list = modules.list();
    assertFalse(list.isEmpty());
    assertEquals(10, list.size());
    for (Module module : list) {
      assertEquals(tcVersion, module.tcVersion());
    }

    tcVersion = "0.0.1";
    modules = load(testData, tcVersion);
    list = modules.list();
    assertFalse(list.isEmpty());
    assertEquals(1, list.size());
    for (Module module : list) {
      assertEquals(tcVersion, module.tcVersion());
    }
  }

  public void testListLatest() {
    Modules modules;
    String testData = "/testData02.xml";

    modules = load(testData, "x.x.x");
    assertTrue(modules.listLatest().isEmpty());

    modules = load(testData, "0.0.0");
    List<Module> list = modules.listLatest();
    assertFalse(list.isEmpty());
    assertEquals(2, list.size());
    Module module = list.get(0);
    assertEquals("foo.bar.baz", module.symbolicName());
    assertEquals("0.0.7", module.version());
    module = list.get(1);
    assertEquals("foo.bar.quux", module.symbolicName());
    assertEquals("0.0.5", module.version());

    modules = load(testData, "0.0.1");
    list = modules.listLatest();
    assertFalse(list.isEmpty());
    assertEquals(1, list.size());
    module = list.get(0);
    assertEquals("foo.bar.baz", module.symbolicName());
    assertEquals("0.0.6", module.version());
  }

  private Modules load(String testData, String tcVersion) {
    File tmpdir = new File(System.getProperty("java.io.tmpdir"));
    try {
      return new CachedModules(tcVersion, tmpdir, getClass().getResourceAsStream(testData));
    } catch (Exception e) {
      fail("Unable to load test data: " + testData);
    }
    return null;
  }
}
