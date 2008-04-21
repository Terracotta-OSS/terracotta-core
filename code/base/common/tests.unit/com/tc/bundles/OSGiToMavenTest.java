/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import java.io.File;

import junit.framework.TestCase;

public class OSGiToMavenTest extends TestCase {

  public void testArtifactIdFromSymbolicName() {
    String symbolicName = "org.foo.bar.foobar";
    String actual = OSGiToMaven.artifactIdFromSymbolicName(symbolicName);
    assertEquals("foobar", actual);

    symbolicName = "org.foo.bar.foobar-2.0";
    actual = OSGiToMaven.artifactIdFromSymbolicName(symbolicName);
    assertEquals("foobar-2.0", actual);

    symbolicName = "org.foo.bar-9.foobar-2.0";
    actual = OSGiToMaven.artifactIdFromSymbolicName(symbolicName);
    assertEquals("foobar-2.0", actual);

    symbolicName = "org.foo.foobar";
    actual = OSGiToMaven.artifactIdFromSymbolicName(symbolicName);
    assertEquals("foobar", actual);

    symbolicName = "org.foo.foobar-2.0";
    actual = OSGiToMaven.artifactIdFromSymbolicName(symbolicName);
    assertEquals("foobar-2.0", actual);

    symbolicName = "org.foo.foobar-2.0";
    actual = OSGiToMaven.artifactIdFromSymbolicName(symbolicName);
    assertEquals("foobar-2.0", actual);

    symbolicName = "org-1.xx0.xxx0.foo-bar";
    actual = OSGiToMaven.artifactIdFromSymbolicName(symbolicName);
    assertEquals("foo-bar", actual);

    symbolicName = "org-1";
    actual = OSGiToMaven.artifactIdFromSymbolicName(symbolicName);
    assertEquals("org-1", actual);

    symbolicName = "foobar-2.0";
    actual = OSGiToMaven.artifactIdFromSymbolicName(symbolicName);
    assertEquals("foobar-2.0", actual);

    symbolicName = "foobar";
    actual = OSGiToMaven.artifactIdFromSymbolicName(symbolicName);
    assertEquals("foobar", actual);
  }

  public void testGroupIdFromSymbolicName() {
    String symbolicName = "org.foo.bar.foobar";
    String actual = OSGiToMaven.groupIdFromSymbolicName(symbolicName);
    assertEquals("org.foo.bar", actual);

    symbolicName = "org.foo.foobar";
    actual = OSGiToMaven.groupIdFromSymbolicName(symbolicName);
    assertEquals("org.foo", actual);

    symbolicName = "org.foobar";
    actual = OSGiToMaven.groupIdFromSymbolicName(symbolicName);
    assertEquals("org", actual);

    symbolicName = "foobar";
    actual = OSGiToMaven.groupIdFromSymbolicName(symbolicName);
    assertEquals("", actual);
  }

  public void testBundleVersionToProjectVersion() {
    String bundleVersion = "1.0.0.SNAPSHOT";
    String actual = OSGiToMaven.bundleVersionToProjectVersion(bundleVersion);
    assertEquals("1.0.0-SNAPSHOT", actual);

    bundleVersion = "1.0.0-SNAPSHOT";
    actual = OSGiToMaven.bundleVersionToProjectVersion(bundleVersion);
    assertEquals("1.0.0-SNAPSHOT", actual);

    bundleVersion = "1.0.0";
    actual = OSGiToMaven.bundleVersionToProjectVersion(bundleVersion);
    assertEquals("1.0.0", actual);

    bundleVersion = "1.0.SNAPSHOT";
    actual = OSGiToMaven.bundleVersionToProjectVersion(bundleVersion);
    assertEquals("1.0-SNAPSHOT", actual);

    bundleVersion = "1.0-SNAPSHOT";
    actual = OSGiToMaven.bundleVersionToProjectVersion(bundleVersion);
    assertEquals("1.0-SNAPSHOT", actual);

    bundleVersion = "1.0";
    actual = OSGiToMaven.bundleVersionToProjectVersion(bundleVersion);
    assertEquals("1.0", actual);

    bundleVersion = "1.SNAPSHOT";
    actual = OSGiToMaven.bundleVersionToProjectVersion(bundleVersion);
    assertEquals("1-SNAPSHOT", actual);

    bundleVersion = "1-SNAPSHOT";
    actual = OSGiToMaven.bundleVersionToProjectVersion(bundleVersion);
    assertEquals("1-SNAPSHOT", actual);

    bundleVersion = "1";
    actual = OSGiToMaven.bundleVersionToProjectVersion(bundleVersion);
    assertEquals("1", actual);
  }

  public void testMakeBundleFilename() {
    String symbolicName = "org.foo.bar.foobar";
    String version = "1.0.0.SNAPSHOT";
    String actual = OSGiToMaven.makeBundleFilename(symbolicName, version);
    assertEquals("foobar-1.0.0-SNAPSHOT.jar", actual);

    symbolicName = "org.foo.bar.foobar";
    version = "1.0.0-SNAPSHOT";
    actual = OSGiToMaven.makeBundleFilename(symbolicName, version);
    assertEquals("foobar-1.0.0-SNAPSHOT.jar", actual);

    symbolicName = "org.foo.bar.foobar-2.0";
    version = "1.0.0.SNAPSHOT";
    actual = OSGiToMaven.makeBundleFilename(symbolicName, version);
    assertEquals("foobar-2.0-1.0.0-SNAPSHOT.jar", actual);

    symbolicName = "org.foo.bar.foo-bar-2.0.0";
    version = "1.0.0-SNAPSHOT";
    actual = OSGiToMaven.makeBundleFilename(symbolicName, version);
    assertEquals("foo-bar-2.0.0-1.0.0-SNAPSHOT.jar", actual);

    symbolicName = "org.foo.bar.foobar";
    version = "1.0.0";
    actual = OSGiToMaven.makeBundleFilename(symbolicName, version);
    assertEquals("foobar-1.0.0.jar", actual);

    symbolicName = "org.foo.bar.foobar-2.0";
    version = "1.0.0";
    actual = OSGiToMaven.makeBundleFilename(symbolicName, version);
    assertEquals("foobar-2.0-1.0.0.jar", actual);

    symbolicName = "foobar";
    version = "1.0.0.SNAPSHOT";
    actual = OSGiToMaven.makeBundleFilename(symbolicName, version);
    assertEquals("foobar-1.0.0-SNAPSHOT.jar", actual);

    symbolicName = "foobar-2.0";
    version = "1.0.0-SNAPSHOT";
    actual = OSGiToMaven.makeBundleFilename(symbolicName, version);
    assertEquals("foobar-2.0-1.0.0-SNAPSHOT.jar", actual);

    symbolicName = "foobar";
    version = "1.0.0";
    actual = OSGiToMaven.makeBundleFilename(symbolicName, version);
    assertEquals("foobar-1.0.0.jar", actual);

    symbolicName = "foobar-2.0";
    version = "1.0.0";
    actual = OSGiToMaven.makeBundleFilename(symbolicName, version);
    assertEquals("foobar-2.0-1.0.0.jar", actual);
  }

  public void testMakeBundlePathname() {
    String root = "/tmp";
    String symbolicName = "org.foo.bar.baz";
    String version = "1.0.0.SNAPSHOT";
    String actual = OSGiToMaven.makeBundlePathname(root, symbolicName, version);
    assertEquals(root + "/org/foo/bar/baz/1.0.0-SNAPSHOT/baz-1.0.0-SNAPSHOT.jar".replace('/', File.separatorChar),
                 actual);
  }
}
