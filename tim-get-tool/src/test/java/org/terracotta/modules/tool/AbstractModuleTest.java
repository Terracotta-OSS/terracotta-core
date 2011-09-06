/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

/**
 * Test cases for abstract class Module's methods and other assumptions
 */
public final class AbstractModuleTest extends TestCase {

  /**
   * Tests the compareTo(..) method by way of sort
   */
  public void testCompareTo() {
    List<AbstractModule> modules = new ArrayList<AbstractModule>();
    modules.add(new TestModule("a", "0.0.0-SNAPSHOT"));
    modules.add(new TestModule("a", "0.0.0"));
    modules.add(new TestModule("a", "0.0.1-SNAPSHOT"));
    modules.add(new TestModule("a", "0.0.1"));
    modules.add(new TestModule("a", "0.1.0-SNAPSHOT"));
    modules.add(new TestModule("a", "0.1.0"));
    modules.add(new TestModule("a", "1.0.0-SNAPSHOT"));
    modules.add(new TestModule("a", "1.0.0"));
    modules.add(new TestModule("a", "a", "0.0.0-SNAPSHOT"));
    modules.add(new TestModule("a", "a", "0.0.0"));
    modules.add(new TestModule("a", "a", "0.0.1-SNAPSHOT"));
    modules.add(new TestModule("a", "a", "0.0.1"));
    modules.add(new TestModule("a", "a", "0.1.0-SNAPSHOT"));
    modules.add(new TestModule("a", "a", "0.1.0"));
    modules.add(new TestModule("a", "a", "1.0.0-SNAPSHOT"));
    modules.add(new TestModule("a", "a", "1.0.0"));
    modules.add(new TestModule("b", "a", "0.0.0-SNAPSHOT"));
    modules.add(new TestModule("b", "a", "0.0.0"));
    modules.add(new TestModule("b", "a", "0.0.1-SNAPSHOT"));
    modules.add(new TestModule("b", "a", "0.0.1"));
    modules.add(new TestModule("b", "a", "0.1.0-SNAPSHOT"));
    modules.add(new TestModule("b", "a", "0.1.0"));
    modules.add(new TestModule("b", "a", "1.0.0-SNAPSHOT"));
    modules.add(new TestModule("b", "a", "1.0.0"));
    modules.add(new TestModule("bb", "a", "0.0.0-SNAPSHOT"));
    modules.add(new TestModule("bb", "aa", "0.0.0"));
    modules.add(new TestModule("bb", "aaa", "0.0.1-SNAPSHOT"));
    modules.add(new TestModule("bb", "aaaa", "0.0.1"));
    modules.add(new TestModule("bb", "aaaaa", "0.1.0-SNAPSHOT"));
    modules.add(new TestModule("bb", "aaaaaa", "0.1.0"));
    modules.add(new TestModule("bb", "aaaaaaa", "1.0.0-SNAPSHOT"));
    modules.add(new TestModule("bb", "aaaaaaaa", "1.0.0"));
    List<String> sorted = new ArrayList<String>();
    for (AbstractModule module : modules) {
      sorted.add(module.toString());
    }
    Collections.shuffle(modules);
    Collections.sort(modules);
    for (AbstractModule module : modules) {
      int n0 = sorted.indexOf(module.toString());
      int n1 = modules.indexOf(module);
      assertEquals(n0, n1);
    }
    Collections.reverse(modules);
    for (AbstractModule module : modules) {
      int n0 = modules.size() - (sorted.indexOf(module.toString()) + 1);
      int n1 = modules.indexOf(module);
      assertEquals(n0, n1);
    }
  }

  public void testEquals() {
    AbstractModule module = new TestModule("foo.bar", "baz", "0.0.0");
    AbstractModule other = new TestModule("foo.bar", "baz", "0.0.0");
    assertTrue(module.equals(other));

    other = new TestModule("foo.bar", "baz", "0.0.1");
    assertFalse(module.equals(other));

    other = new TestModule("foo.bar", "quux", "0.0.0");
    assertFalse(module.equals(other));

    other = new TestModule("baz", "0.0.0");
    assertFalse(module.equals(other));
  }

  public void testIsOlder() {
    AbstractModule module = new TestModule("foo.bar", "baz", "0.0.0");
    AbstractModule other = new TestModule("foo.bar", "baz", "0.0.0");
    assertFalse(module.isOlder(other));

    other = new TestModule("foo.bar", "baz", "0.0.0-BETA");
    assertFalse(module.isOlder(other));
    assertTrue(other.isOlder(module));

    other = new TestModule("foo.bar", "baz", "0.0.0-SNAPSHOT");
    assertFalse(module.isOlder(other));
    assertTrue(other.isOlder(module));

    other = new TestModule("foo.bar", "baz", "0.0.1");
    assertTrue(module.isOlder(other));
    assertFalse(other.isOlder(module));

    module = new TestModule("baz", "1.0.1");
    other = new TestModule("baz", "1.0.10");
    assertTrue(module.isOlder(other));

    other = new TestModule("baz", "10.0.0");
    assertTrue(module.isOlder(other));

    module = new TestModule("baz", "1.0.1");
    other = new TestModule("baz", "2.0.0");
    assertTrue(module.isOlder(other));

    module = new TestModule("foo.bar", "baz", "0.0.0");
    other = new TestModule("baz.foo", "baz", "0.0.0");
    try {
      module.isOlder(other);
      fail("Should've thrown UnsupportedOperationException when symbolicNames don't match on call to Module.isOlder(..)");
    } catch (UnsupportedOperationException e) {
      //
    }
  }

  // public void testIsDefaultGroupId() {
  // AbstractModule module = new TestModule("baz", "0.0.0");
  // assertFalse(module.isDefaultGroupId());
  //
  // module = new TestModule("foo.bar", "baz", "0.0.0");
  // assertFalse(module.isDefaultGroupId());
  //
  // module = new TestModule("org.terracotta.modules", "baz", "0.0.0");
  // assertTrue(module.isDefaultGroupId());
  // }

  public void testIsSibling() {
    AbstractModule module = new TestModule("baz", "0.0.0");
    AbstractModule other = new TestModule("baz", "0.0.1");
    assertTrue(module.isSibling(other));

    module = new TestModule("foo.bar", "baz", "0.0.0");
    other = new TestModule("foo.bar", "baz", "0.0.1");
    assertTrue(module.isSibling(other));

    other = new TestModule("foo.bar", "baz", "0.0.0");
    assertFalse(module.isSibling(other));

    other = new TestModule("foo.bar", "quux", "0.0.0");
    assertFalse(module.isSibling(other));

    other = new TestModule("foo.baz", "quux", "0.0.0");
    assertFalse(module.isSibling(other));
  }

  public void testInvalidConfiguration() {
    AbstractModule module;
    try {
      module = new TestModule(null, "0.0.0");
      module.artifactId();
      fail("Should've thrown an NPE when artifactId is null");
    } catch (NullPointerException e) {
      //
    }

    try {
      module = new TestModule("baz", null);
      module.version();
      fail("Should've thrown an NPE when version is null");
    } catch (NullPointerException e) {
      //
    }

    try {
      module = new TestModule("baz", "0.0.0");
      assertTrue(StringUtils.isEmpty(module.groupId()));
    } catch (NullPointerException e) {
      fail("Should've allowed null or empty groupId");
    }

    try {
      module = new TestModule("foo.bar", "baz", "0.0.0");
      module.groupId();
      module.artifactId();
      module.version();
    } catch (NullPointerException e) {
      fail("Should not have thrown an NPE, all of the attributes are valid");
    }
  }

  /**
   * Trivial implementation of Module to fill in the groupId, artifactId, and version field values.
   */
  private static class TestModule extends AbstractModule {

    public TestModule(String artifactId, String version) {
      this.artifactId = artifactId;
      this.version = version;
    }

    public TestModule(String groupId, String artifactId, String version) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
    }
  }
}
