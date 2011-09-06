/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public final class ReferenceTest extends TestCase {

  public void testInvalidConfiguration() {
    Reference module;

    try {
      module = createModule("foo.bar", "baz", "0.0.0");
      module.groupId();
      module.artifactId();
      module.version();
    } catch (NullPointerException e) {
      fail("Should not have thrown an NPE, all of the attributes are valid");
    }

    try {
      module = createModule("foo.bar", null, "0.0.0");
      module.artifactId();
      fail("Should've thrown an NPE when artifactId is null");
    } catch (NullPointerException e) {
      //
    }

    try {
      module = createModule("foo.bar", "baz", null);
      module.version();
      fail("Should've thrown an NPE when version is null");
    } catch (NullPointerException e) {
      //
    }

    try {
      module = createModule(StringUtils.EMPTY, "baz", null);
      assertTrue(StringUtils.isEmpty(module.groupId()));
    } catch (NullPointerException e) {
      fail("Should've allowed null or empty groupId");
    }

  }

  private Reference createModule(String groupId, String artifactId, String version) {
    Map<String, Object> attributes = new HashMap();
    attributes.put("groupId", groupId);
    attributes.put("artifactId", artifactId);
    attributes.put("version", version);
    return new Reference(null, attributes);
  }
}
