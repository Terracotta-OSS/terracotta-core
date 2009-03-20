/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.lang.StringUtils;
import org.terracotta.modules.tool.config.Config;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public final class BasicModuleTest extends TestCase {
  protected Config testConfig;

  public void setUp() {
    testConfig = TestConfig.createTestConfig();
  }

  public void testInvalidConfiguration() {
    BasicModule module;

    try {
      module = createModule("foo.bar", "baz", "0.0.0");
      module.groupId();
      module.artifactId();
      module.version();

      module.filename();
      module.installPath();
      module.repoUrl();
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

    try {
      module = createModule("foo.bar", "baz", "0.0.0", "filename");
      module.filename();
      fail("Should've thrown an NPE when filename is null");
    } catch (NullPointerException e) {
      //
    }

    try {
      module = createModule("foo.bar", "baz", "0.0.0", "installPath");
      module.installPath();
      fail("Should've thrown an NPE when installPath is null");
    } catch (NullPointerException e) {
      //
    }

    try {
      module = createModule("foo.bar", "baz", "0.0.0", "repoURL");
      module.repoUrl();
      fail("Should've thrown an IllegalStateException when repoURL is null or malformed");
    } catch (IllegalStateException e) {
      //
    }
  }

  private BasicModule createModule(String groupId, String artifactId, String version, String... excludes) {
    Map<String, Object> attributes = new HashMap();
    String filename = artifactId + "-" + version + ".jar";
    String installPath = groupId.replace('.', File.separatorChar) + File.separatorChar + artifactId
                         + File.separatorChar + version;
    attributes.put("groupId", groupId);
    attributes.put("artifactId", artifactId);
    attributes.put("version", version);
    attributes.put("filename", filename);
    attributes.put("installPath", installPath);
    attributes.put("repoURL", "http://127.0.0.1/repo" + installPath.replace(File.separatorChar, '/'));
    for (String key : Arrays.asList(excludes)) {
      attributes.remove(key);
    }
    assertNotNull(testConfig.getRelativeUrlBase());
    BasicModule basicModule = new BasicModule(null, attributes, testConfig.getRelativeUrlBase());
    assertNotNull(basicModule.repoUrl());
    return basicModule;
  }
}
