/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.lang.StringUtils;
import org.terracotta.modules.tool.config.Config;
import org.terracotta.modules.tool.exception.UnsatisfiedDependencyException;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

public final class CachedModulesTest extends TestCase {

  protected Config testConfig;

  @Override
  public void setUp() {
    testConfig = TestConfig.createTestConfig();
  }

  public void testGetSiblinsWithSymbolicName() {
    Modules modules;
    String testData = "/testData02.xml";
    String tcVersion = "0.0.0";

    modules = load(testData, tcVersion);
    Module module = modules.getQualified("foo.bar", "baz", "0.0.0");
    assertNotNull(module);
    List<Module> siblings = modules.getSiblings(module.symbolicName());
    assertFalse(siblings.isEmpty());
    assertEquals(4, siblings.size());
    assertTrue(siblings.contains(module));
    for (Module other : siblings) {
      assertEquals(module.symbolicName(), other.symbolicName());
    }

    module = modules.getQualified("foo.bar", "quux", "0.0.5");
    assertNotNull(module);
    siblings = modules.getSiblings(module.symbolicName());
    assertFalse(siblings.isEmpty());
    assertEquals(2, siblings.size());
    assertTrue(siblings.contains(module));
    for (Module other : siblings) {
      assertEquals(module.symbolicName(), other.symbolicName());
    }

    tcVersion = "0.0.1";
    testConfig.setTcVersion(tcVersion);
    modules = load(testData, tcVersion);
    module = modules.getQualified("foo.bar", "baz", "0.0.6");
    assertNotNull(module);
    assertFalse(modules.getSiblings(module.symbolicName()).isEmpty());

    assertTrue(modules.getSiblings(StringUtils.EMPTY).isEmpty());
  }

  public void testGetSiblings() {
    Modules modules;
    String testData = "/testData02.xml";
    String tcVersion = "0.0.0";

    modules = load(testData, tcVersion);
    Module module = modules.getQualified("foo.bar", "baz", "0.0.0");
    assertNotNull(module);
    List<Module> siblings = modules.getSiblings(module);
    assertFalse(siblings.isEmpty());
    assertEquals(3, siblings.size());
    for (Module other : siblings) {
      assertFalse(module.equals(other));
      assertTrue(module.isSibling(other));
    }

    module = modules.getQualified("foo.bar", "quux", "0.0.5");
    assertNotNull(module);
    siblings = modules.getSiblings(module);
    assertFalse(siblings.isEmpty());
    assertEquals(1, siblings.size());
    for (Module other : siblings) {
      assertFalse(module.equals(other));
      assertTrue(module.isSibling(other));
    }

    tcVersion = "0.0.1";
    testConfig.setTcVersion(tcVersion);
    modules = load(testData, tcVersion);
    module = modules.getQualified("foo.bar", "baz", "0.0.6");
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
    Module module = modules.getQualified("xxx.xxx", "xxx", "1.2.3");
    assertNull(module);

    module = modules.getQualified("foo.bar", "baz", "0.0.0");
    assertNotNull(module);
    assertEquals("foo.bar.baz", module.symbolicName());
    assertEquals("0.0.0", module.version());

    module = modules.getQualified("foo.bar", "quux", "0.0.5");
    assertNotNull(module);
    assertEquals("foo.bar.quux", module.symbolicName());
    assertEquals("0.0.5", module.version());

    tcVersion = "0.0.1";
    testConfig.setTcVersion(tcVersion);
    modules = load(testData, tcVersion);
    module = modules.getQualified("foo.bar", "baz", "0.0.0");
    assertNull(module);

    module = modules.getQualified("foo.bar", "quux", "0.0.5");
    assertNull(module);

    module = modules.getQualified("foo.bar", "baz", "0.0.6");
    assertNotNull(module);
    assertEquals("foo.bar.baz", module.symbolicName());
    assertEquals("0.0.6", module.version());
  }

  public void testList() {
    Modules modules;
    String testData = "/testData02.xml";
    String tcVersion = "x.x.x";
    testConfig.setTcVersion(tcVersion);

    modules = load(testData, tcVersion);
    assertTrue(modules.listQualified().isEmpty());

    tcVersion = "0.0.0";
    testConfig.setTcVersion(tcVersion);
    modules = load(testData, tcVersion);
    List<Module> list = modules.listQualified();
    assertFalse(list.isEmpty());
    assertEquals(10, list.size());
    for (Module module : list) {
      assertEquals(tcVersion, module.tcVersion());
    }

    tcVersion = "0.0.1";
    testConfig.setTcVersion(tcVersion);
    modules = load(testData, tcVersion);
    list = modules.listQualified();
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

  public void testApiVersion() {
    Modules modules;
    String testData = "/testData04.xml";

    modules = load(testData, "0.0.0", "0.0.0");
    assertTrue(modules.listLatest().isEmpty());

    modules = load(testData, "3.0.0", "1.0.0");
    List<Module> list = modules.listLatest();
    assertFalse(list.isEmpty());
    assertEquals(2, list.size());
    Module module = list.get(0);
    assertEquals("foo.bar.abc", module.symbolicName());
    assertEquals("2.0.0", module.version());
    module = list.get(1);
    assertEquals("foo.bar.def", module.symbolicName());
    assertEquals("2.0.0", module.version());
    assertEquals(2, modules.listQualified().size());

    modules = load(testData, "3.0.0", "1.0.1");
    list = modules.listLatest();
    assertFalse(list.isEmpty());
    assertEquals(2, list.size());
    module = list.get(0);
    assertEquals("foo.bar.abc", module.symbolicName());
    assertEquals("2.0.1", module.version());
    module = list.get(1);
    assertEquals("foo.bar.def", module.symbolicName());
    assertEquals("2.0.1", module.version());
    assertEquals(4, modules.listQualified().size());
  }

  public void testSidewaysInstallPath() throws Exception {
    Modules modules;
    String testData = "/testData05.xml";

    modules = load(testData, "0.0.0", "0.0.0");
    assertTrue(modules.listLatest().isEmpty());

    modules = load(testData, "3.0.0", "1.0.0");
    List<Module> list = modules.listLatest();
    assertFalse(list.isEmpty());
    assertEquals(1, list.size());
    Module module = list.get(0);
    assertEquals("org.the.tc.library", module.symbolicName());
    assertEquals("2.0.1", module.version());
    assertTrue(module.installPath().getCanonicalPath().endsWith("test"));

    modules = load(testData, "3.0.0", "1.1.0");
    list = modules.listLatest();
    assertFalse(list.isEmpty());
    assertEquals(1, list.size());
    module = list.get(0);
    assertEquals("org.the.tc.library", module.symbolicName());
    assertEquals("2.1.0", module.version());
    assertTrue(module.installPath().getCanonicalPath().endsWith("test"));
  }

  public void testMissingTransitiveDependency() throws UnsatisfiedDependencyException {
    testConfig.setTcVersion("1.0.0");
    Modules modules = load("/transitive-deps.xml", "1.0.0");

    assertEquals(5, modules.listAvailable().size());

    Module m = modules.getAvailable("org.terracotta.modules", "tim-ehcache-2.x", "1.6.0");
    assertTrue(m.isLatest());

    List<AbstractModule> manifest = m.manifest();
    assertEquals(2, manifest.size());

    AbstractModule toolkit = manifest.get(1);
    assertEquals("terracotta-toolkit-1.1", toolkit.artifactId());
    assertEquals("1.0.1", toolkit.version());

    assertNull(modules.getAvailable("org.terracotta.modules", "tim-ehcache-2.x", "1.6.1"));
  }

  private Modules load(String testData, String tcVersion) {
    return load(testData, tcVersion, "1.0.0");
  }

  private Modules load(String testData, String tcVersion, String apiVersion) {
    testConfig.setTcVersion(tcVersion);
    if (apiVersion != null) {
      testConfig.setTimApiVersion(apiVersion);
    }
    File tmpdir = new File(System.getProperty("java.io.tmpdir"));
    try {
      return new CachedModules(testConfig, tmpdir, getClass().getResourceAsStream(testData));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
