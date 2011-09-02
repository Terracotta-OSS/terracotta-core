/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import java.io.File;

import junit.framework.TestCase;

public class OSGiToMavenTest extends TestCase {

  private void artifactIdFromSymbolicName(String symbolicName, String expected) {
    String actual = OSGiToMaven.artifactIdFromSymbolicName(symbolicName);
    assertEquals(expected, actual);
  }

  public void testArtifactIdFromSymbolicName() {
    artifactIdFromSymbolicName("foobar", "foobar");
    artifactIdFromSymbolicName("org.foobar", "foobar");
    artifactIdFromSymbolicName("org.foo.foobar", "foobar");
    artifactIdFromSymbolicName("org.foo.bar.foobar", "foobar");
    artifactIdFromSymbolicName("org.foo.bar.baz.foobar", "foobar");

    artifactIdFromSymbolicName("foo-bar", "foo-bar");
    artifactIdFromSymbolicName("org.foo-bar", "foo-bar");
    artifactIdFromSymbolicName("org.foo.foo-bar", "foo-bar");
    artifactIdFromSymbolicName("org.foo.bar.foo-bar", "foo-bar");
    artifactIdFromSymbolicName("org.foo.bar.baz.foo-bar", "foo-bar");

    artifactIdFromSymbolicName("foo-bar-2.0", "foo-bar-2.0");
    artifactIdFromSymbolicName("org.foo-bar-2.0", "foo-bar-2.0");
    artifactIdFromSymbolicName("org.foo.foo-bar-2.0", "foo-bar-2.0");
    artifactIdFromSymbolicName("org.foo.bar.foo-bar-2.0", "foo-bar-2.0");
    artifactIdFromSymbolicName("org.foo.bar.baz.foo-bar-2.0", "foo-bar-2.0");

    artifactIdFromSymbolicName("org.foo.bar.baz.9foo-bar-2.0", "baz.9foo-bar-2.0");
    artifactIdFromSymbolicName("org.foo.bar.baz.9-foo-bar-2.0", "baz.9-foo-bar-2.0");

    artifactIdFromSymbolicName("foo-bar-2.0", "foo-bar-2.0");
    artifactIdFromSymbolicName("org1.foo-bar-2.0", "foo-bar-2.0");
    artifactIdFromSymbolicName("org1.foo-2.foo-bar-2.0", "foo-bar-2.0");
    artifactIdFromSymbolicName("org1.foo-2.bar_3.foo-bar-2.0", "foo-bar-2.0");
    artifactIdFromSymbolicName("org1.foo-2.bar_3.baz_4-5.foo-bar-2.0", "foo-bar-2.0");

    artifactIdFromSymbolicName("org1.foo-2.bar_3.baz_4-5.0foo-bar-2.0", "baz_4-5.0foo-bar-2.0");
    artifactIdFromSymbolicName("org-foo-bar.b_az.x.q.f_o-obar-1", "f_o-obar-1");
  }

  private void groupIdFromSymbolicName(String symbolicName, String expected) {
    String actual = OSGiToMaven.groupIdFromSymbolicName(symbolicName);
    assertEquals(expected, actual);
  }

  public void testGroupIdFromSymbolicName() {
    groupIdFromSymbolicName("foobar", "");
    groupIdFromSymbolicName("org.foobar", "org");
    groupIdFromSymbolicName("org.foo.foobar", "org.foo");
    groupIdFromSymbolicName("org.foo.bar.foobar", "org.foo.bar");
    groupIdFromSymbolicName("org.foo.bar.baz.foobar", "org.foo.bar.baz");

    groupIdFromSymbolicName("foo-bar", "");
    groupIdFromSymbolicName("org.foo-bar", "org");
    groupIdFromSymbolicName("org.foo.foo-bar", "org.foo");
    groupIdFromSymbolicName("org.foo.bar.foo-bar", "org.foo.bar");
    groupIdFromSymbolicName("org.foo.bar.baz.foo-bar", "org.foo.bar.baz");

    groupIdFromSymbolicName("foo-bar-2.0", "");
    groupIdFromSymbolicName("org.foo-bar-2.0", "org");
    groupIdFromSymbolicName("org.foo.foo-bar-2.0", "org.foo");
    groupIdFromSymbolicName("org.foo.bar.foo-bar-2.0", "org.foo.bar");
    groupIdFromSymbolicName("org.foo.bar.baz.foo-bar-2.0", "org.foo.bar.baz");

    groupIdFromSymbolicName("org.foo.bar.baz.9foo-bar-2.0", "org.foo.bar");
    groupIdFromSymbolicName("org.foo.bar.baz.9-foo-bar-2.0", "org.foo.bar");

    groupIdFromSymbolicName("foo-bar-2.0", "");
    groupIdFromSymbolicName("org1.foo-bar-2.0", "org1");
    groupIdFromSymbolicName("org1.foo-2.foo-bar-2.0", "org1.foo-2");
    groupIdFromSymbolicName("org1.foo-2.bar_3.foo-bar-2.0", "org1.foo-2.bar_3");
    groupIdFromSymbolicName("org1.foo-2.bar_3.baz_4-5.foo-bar-2.0", "org1.foo-2.bar_3.baz_4-5");

    groupIdFromSymbolicName("org1.foo-2.bar_3.baz_4-5.0foo-bar-2.0", "org1.foo-2.bar_3");
    groupIdFromSymbolicName("org-foo-bar.b_az.x.q.f_o-obar-1", "org-foo-bar.b_az.x.q");
  }

  private void bundleVersionToProjectVersion(String bundleVersion, String expected) {
    String actual = OSGiToMaven.bundleVersionToProjectVersion(bundleVersion);
    assertEquals(expected, actual);
  }

  public void testBundleVersionToProjectVersion() {
    bundleVersionToProjectVersion("1", "1");
    bundleVersionToProjectVersion("12", "12");
    bundleVersionToProjectVersion("123", "123");

    bundleVersionToProjectVersion("1.0", "1.0");
    bundleVersionToProjectVersion("12.0", "12.0");
    bundleVersionToProjectVersion("123.0", "123.0");

    bundleVersionToProjectVersion("1.0.0", "1.0.0");
    bundleVersionToProjectVersion("12.0.0", "12.0.0");
    bundleVersionToProjectVersion("123.0.0", "123.0.0");
    bundleVersionToProjectVersion("0.0.0", "0.0.0");
    bundleVersionToProjectVersion("123.456.789", "123.456.789");

    bundleVersionToProjectVersion("1.SNAPSHOT", "1-SNAPSHOT");
    bundleVersionToProjectVersion("12.SNAPSHOT", "12-SNAPSHOT");
    bundleVersionToProjectVersion("123.SNAPSHOT", "123-SNAPSHOT");

    bundleVersionToProjectVersion("1-SNAPSHOT", "1-SNAPSHOT");
    bundleVersionToProjectVersion("12-SNAPSHOT", "12-SNAPSHOT");
    bundleVersionToProjectVersion("123-SNAPSHOT", "123-SNAPSHOT");

    bundleVersionToProjectVersion("1.0.SNAPSHOT", "1.0-SNAPSHOT");
    bundleVersionToProjectVersion("12.0.SNAPSHOT", "12.0-SNAPSHOT");
    bundleVersionToProjectVersion("123.0.SNAPSHOT", "123.0-SNAPSHOT");

    bundleVersionToProjectVersion("1.0-SNAPSHOT", "1.0-SNAPSHOT");
    bundleVersionToProjectVersion("12.0-SNAPSHOT", "12.0-SNAPSHOT");
    bundleVersionToProjectVersion("123.0-SNAPSHOT", "123.0-SNAPSHOT");

    bundleVersionToProjectVersion("1.0.0.SNAPSHOT", "1.0.0-SNAPSHOT");
    bundleVersionToProjectVersion("12.0.0.SNAPSHOT", "12.0.0-SNAPSHOT");
    bundleVersionToProjectVersion("123.6.0.SNAPSHOT", "123.6.0-SNAPSHOT");
    bundleVersionToProjectVersion("0.0.0.SNAPSHOT", "0.0.0-SNAPSHOT");
    bundleVersionToProjectVersion("123.456.789.SNAPSHOT", "123.456.789-SNAPSHOT");

    bundleVersionToProjectVersion("1.0.0-SNAPSHOT", "1.0.0-SNAPSHOT");
    bundleVersionToProjectVersion("12.0.0-SNAPSHOT", "12.0.0-SNAPSHOT");
    bundleVersionToProjectVersion("123.6.0-SNAPSHOT", "123.6.0-SNAPSHOT");
    bundleVersionToProjectVersion("0.0.0-SNAPSHOT", "0.0.0-SNAPSHOT");
    bundleVersionToProjectVersion("123.456.789.SNAPSHOT", "123.456.789-SNAPSHOT");
    bundleVersionToProjectVersion("123.456.789-SNAPSHOT", "123.456.789-SNAPSHOT");

    bundleVersionToProjectVersion("0.0.0.sNapShOt", "0.0.0-sNapShOt");
    bundleVersionToProjectVersion("0.0.0-sNapShOt", "0.0.0-sNapShOt");
    bundleVersionToProjectVersion("0.0.0.alpha", "0.0.0-alpha");
    bundleVersionToProjectVersion("0.0.0.beta1", "0.0.0-beta1");
    bundleVersionToProjectVersion("0.0.0.gamma2-delta", "0.0.0-gamma2-delta");
    bundleVersionToProjectVersion("0.0.0.gamma2.delta", "0.0.0-gamma2.delta");
    bundleVersionToProjectVersion("0.0.0-gamma2.delta", "0.0.0-gamma2.delta");

    bundleVersionToProjectVersion("0.0.0.a2.1", "0.0.0-a2.1");
    bundleVersionToProjectVersion("0.0.0-a2.1", "0.0.0-a2.1");

    bundleVersionToProjectVersion("0.0.0-2.1", "0.0.0-2.1");
    bundleVersionToProjectVersion("0.0.0.2.1", "0.0.0.2.1");

    bundleVersionToProjectVersion("0.0.0.DELTA", "0.0.0-DELTA");
    bundleVersionToProjectVersion("0.0.0-alpha", "0.0.0-alpha");
    bundleVersionToProjectVersion("0.0.0-beta1", "0.0.0-beta1");
    bundleVersionToProjectVersion("0.0.0-DELTA", "0.0.0-DELTA");
    bundleVersionToProjectVersion("0.0.0.A1-C", "0.0.0-A1-C");
    bundleVersionToProjectVersion("0.0.0-A1-C", "0.0.0-A1-C");
  }

  private void makeBundleFilename(String symbolicName, String version, String expected) {
    String actual = OSGiToMaven.makeBundleFilename(symbolicName, version);
    assertEquals(expected, actual);
  }

  public void testMakeBundleFilename() {
    makeBundleFilename("foobar", "0", "foobar-0.jar");
    makeBundleFilename("foobar-1", "0", "foobar-1-0.jar");
    makeBundleFilename("foobar-1", "0.0.0", "foobar-1-0.0.0.jar");
    makeBundleFilename("foobar-1", "0.0.0.SNAPSHOT", "foobar-1-0.0.0-SNAPSHOT.jar");
    makeBundleFilename("foobar-1", "0.0.0-SNAPSHOT", "foobar-1-0.0.0-SNAPSHOT.jar");

    makeBundleFilename("org.foobar", "0", "foobar-0.jar");
    makeBundleFilename("org.foo.bar.foobar-1", "0", "foobar-1-0.jar");
    makeBundleFilename("org.foo.bar.baz.foobar-1", "0.0.0", "foobar-1-0.0.0.jar");

    makeBundleFilename("org1.foo-2.bar_3.baz_4-5.0foo-bar-2.0", "0.0.0.SNAPSHOT",
                       "baz_4-5.0foo-bar-2.0-0.0.0-SNAPSHOT.jar");

    makeBundleFilename("org1.foo_2.bar-3.baz.foobar-1", "0.0.0.SNAPSHOT", "foobar-1-0.0.0-SNAPSHOT.jar");
    makeBundleFilename("f_o-obar-1", "0.0.0-SNAPSHOT", "f_o-obar-1-0.0.0-SNAPSHOT.jar");
  }

  private void makeBundlePathname(String root, String symbolicName, String version, String expected) {
    String actual = OSGiToMaven.makeBundlePathname(root, symbolicName, version);
    System.out.println("xxx - expected: " + expected.replace('/', File.separatorChar));
    System.out.println("xxx - actual  : " + actual);
    assertEquals(expected.replace('/', File.separatorChar), actual);
  }

  public void testMakeBundlePathname() {
    makeBundlePathname("/tmp", "foobar", "0", "/tmp/foobar/0/foobar-0.jar");
    makeBundlePathname("/tmp", "foobar-1", "0", "/tmp/foobar-1/0/foobar-1-0.jar");
    makeBundlePathname("/tmp", "foobar-1", "0.0.0", "/tmp/foobar-1/0.0.0/foobar-1-0.0.0.jar");
    makeBundlePathname("/tmp", "foobar-1", "0.0.0.SNAPSHOT", "/tmp/foobar-1/0.0.0-SNAPSHOT/foobar-1-0.0.0-SNAPSHOT.jar");
    makeBundlePathname("/tmp", "foobar-1", "0.0.0-SNAPSHOT", "/tmp/foobar-1/0.0.0-SNAPSHOT/foobar-1-0.0.0-SNAPSHOT.jar");

    makeBundlePathname("/tmp", "org.foobar", "0", "/tmp/org/foobar/0/foobar-0.jar");
    makeBundlePathname("/tmp", "org.foo.bar.foobar-1", "0", "/tmp/org/foo/bar/foobar-1/0/foobar-1-0.jar");
    makeBundlePathname("/tmp", "org.foo.bar.baz.foobar-1", "0.0.0",
                       "/tmp/org/foo/bar/baz/foobar-1/0.0.0/foobar-1-0.0.0.jar");

    makeBundlePathname("/tmp", "org1.foo-2.bar_3.baz_4-5.0foo-bar-2.0", "0.0.0.SNAPSHOT",
                       "/tmp/org1/foo-2/bar_3/baz_4-5.0foo-bar-2.0/0.0.0-SNAPSHOT/baz_4-5.0foo-bar-2.0-0.0.0-SNAPSHOT.jar");

    makeBundlePathname("/tmp", "org1.foo_2.bar-3.baz.foobar-1", "0.0.0.SNAPSHOT",
                       "/tmp/org1/foo_2/bar-3/baz/foobar-1/0.0.0-SNAPSHOT/foobar-1-0.0.0-SNAPSHOT.jar");
    makeBundlePathname("/tmp", "f_o-obar-1", "0.0.0-SNAPSHOT",
                       "/tmp/f_o-obar-1/0.0.0-SNAPSHOT/f_o-obar-1-0.0.0-SNAPSHOT.jar");

    makeBundlePathname("/tmp", "org-foo-bar.f_o-obar-1", "0.0.0-SNAPSHOT",
                       "/tmp/org-foo-bar/f_o-obar-1/0.0.0-SNAPSHOT/f_o-obar-1-0.0.0-SNAPSHOT.jar");
    makeBundlePathname("/tmp", "org-foo-bar.f_o-obar-1", "0.0.0",
                       "/tmp/org-foo-bar/f_o-obar-1/0.0.0/f_o-obar-1-0.0.0.jar");

    makeBundlePathname("/tmp", "org-foo-bar.b_az.x.q.f_o-obar-1", "0.0.0.ALPHA1.9",
                       "/tmp/org-foo-bar/b_az/x/q/f_o-obar-1/0.0.0-ALPHA1.9/f_o-obar-1-0.0.0-ALPHA1.9.jar");

    makeBundlePathname("/tmp", "org-foo-bar.b_az.q.f_o-obar-1", "0.0.0.ALPHA1",
                       "/tmp/org-foo-bar/b_az/q/f_o-obar-1/0.0.0-ALPHA1/f_o-obar-1-0.0.0-ALPHA1.jar");

    makeBundlePathname("/tmp", "org-foo-bar.b_az.q.f_o-obar-1", "0.0.0.ALPHA1.9.0-xxx",
                       "/tmp/org-foo-bar/b_az/q/f_o-obar-1/0.0.0-ALPHA1.9.0-xxx/f_o-obar-1-0.0.0-ALPHA1.9.0-xxx.jar");

    makeBundlePathname("/tmp", "org-foo-bar.b_az.f_o-obar-1", "0.0.0",
                       "/tmp/org-foo-bar/b_az/f_o-obar-1/0.0.0/f_o-obar-1-0.0.0.jar");
    makeBundlePathname("/tmp", "org-foo-bar.b_az.q.f_o-obar-1", "0.0.0",
                       "/tmp/org-foo-bar/b_az/q/f_o-obar-1/0.0.0/f_o-obar-1-0.0.0.jar");
    makeBundlePathname("/tmp", "org-foo-bar.b_az.q.f_o-obar-1", "0.0.0.ALPHA1.0",
                       "/tmp/org-foo-bar/b_az/q/f_o-obar-1/0.0.0-ALPHA1.0/f_o-obar-1-0.0.0-ALPHA1.0.jar");

    makeBundlePathname("/tmp", "org-foo-bar.b_az.q.f_o-obar-1", "5.2.3.4.ALPHA1.0-beta",
                       "/tmp/org-foo-bar/b_az/q/f_o-obar-1/5.2.3.4-ALPHA1.0-beta/f_o-obar-1-5.2.3.4-ALPHA1.0-beta.jar");
  }

  private void makeFlatBundlePathname(String root, String symbolicName, String version, String expected) {
    String actual = OSGiToMaven.makeFlatBundlePathname(root, symbolicName, version);
    System.out.println("xxx - expected: " + expected.replace('/', File.separatorChar));
    System.out.println("xxx - actual  : " + actual);
    assertEquals(expected.replace('/', File.separatorChar), actual);
  }

  public void testMakeFlatBundlePathname() {
    makeFlatBundlePathname("/tmp", "foobar", "0", "/tmp/foobar-0.jar");
    makeFlatBundlePathname("/tmp", "foobar-1", "0", "/tmp/foobar-1-0.jar");
    makeFlatBundlePathname("/tmp", "foobar-1", "0.0.0", "/tmp/foobar-1-0.0.0.jar");
    makeFlatBundlePathname("/tmp", "foobar-1", "0.0.0.SNAPSHOT", "/tmp/foobar-1-0.0.0-SNAPSHOT.jar");
    makeFlatBundlePathname("/tmp", "foobar-1", "0.0.0-SNAPSHOT", "/tmp/foobar-1-0.0.0-SNAPSHOT.jar");

    makeFlatBundlePathname("/tmp", "org.foobar", "0", "/tmp/foobar-0.jar");
    makeFlatBundlePathname("/tmp", "org.foo.bar.foobar-1", "0", "/tmp/foobar-1-0.jar");
    makeFlatBundlePathname("/tmp", "org.foo.bar.baz.foobar-1", "0.0.0", "/tmp/foobar-1-0.0.0.jar");

    makeFlatBundlePathname("/tmp", "org1.foo-2.bar_3.baz_4-5.0foo-bar-2.0", "0.0.0.SNAPSHOT",
                           "/tmp/baz_4-5.0foo-bar-2.0-0.0.0-SNAPSHOT.jar");

    makeFlatBundlePathname("/tmp", "org1.foo_2.bar-3.baz.foobar-1", "0.0.0.SNAPSHOT",
                           "/tmp/foobar-1-0.0.0-SNAPSHOT.jar");
    makeFlatBundlePathname("/tmp", "f_o-obar-1", "0.0.0-SNAPSHOT", "/tmp/f_o-obar-1-0.0.0-SNAPSHOT.jar");

    makeFlatBundlePathname("/tmp", "org-foo-bar.f_o-obar-1", "0.0.0-SNAPSHOT", "/tmp/f_o-obar-1-0.0.0-SNAPSHOT.jar");
    makeFlatBundlePathname("/tmp", "org-foo-bar.f_o-obar-1", "0.0.0", "/tmp/f_o-obar-1-0.0.0.jar");

    makeFlatBundlePathname("/tmp", "org-foo-bar.b_az.x.q.f_o-obar-1", "0.0.0.ALPHA1.9",
                           "/tmp/f_o-obar-1-0.0.0-ALPHA1.9.jar");

    makeFlatBundlePathname("/tmp", "org-foo-bar.b_az.q.f_o-obar-1", "0.0.0.ALPHA1", "/tmp/f_o-obar-1-0.0.0-ALPHA1.jar");

    makeFlatBundlePathname("/tmp", "org-foo-bar.b_az.q.f_o-obar-1", "0.0.0.ALPHA1.9.0-xxx",
                           "/tmp/f_o-obar-1-0.0.0-ALPHA1.9.0-xxx.jar");

    makeFlatBundlePathname("/tmp", "org-foo-bar.b_az.f_o-obar-1", "0.0.0", "/tmp/f_o-obar-1-0.0.0.jar");
    makeFlatBundlePathname("/tmp", "org-foo-bar.b_az.q.f_o-obar-1", "0.0.0", "/tmp/f_o-obar-1-0.0.0.jar");
    makeFlatBundlePathname("/tmp", "org-foo-bar.b_az.q.f_o-obar-1", "0.0.0.ALPHA1.0",
                           "/tmp/f_o-obar-1-0.0.0-ALPHA1.0.jar");

    makeFlatBundlePathname("/tmp", "org-foo-bar.b_az.q.f_o-obar-1", "5.2.3.4.ALPHA1.0-beta",
                           "/tmp/f_o-obar-1-5.2.3.4-ALPHA1.0-beta.jar");
  }

  public void testMakeBundlePathnamePrefix() {
    String expect = "/tmp/tc/modules/org/terracotta/modules/tim-yogurt/";
    expect = expect.replace('/', File.separatorChar);

    assertEquals(expect, OSGiToMaven
        .makeBundlePathnamePrefix("/tmp/tc/modules", "org.terracotta.modules", "tim-yogurt"));
  }
}
