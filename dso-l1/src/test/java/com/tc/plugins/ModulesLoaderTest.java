/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.plugins;

import org.apache.commons.io.FileUtils;
import org.osgi.framework.BundleException;

import com.tc.bundles.EmbeddedOSGiRuntime;
import com.tc.bundles.Modules;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.loaders.ClassProvider;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class ModulesLoaderTest extends BaseDSOTestCase {

  /**
   * Helper method to check that an error message contains some expected text
   * 
   * @param t The error, will verify has non-null message
   * @param expectedText Expected text in message
   */
  private void checkErrorMessageContainsText(Throwable t, String expectedText) {
    String message = t.getMessage();
    Assert.assertNotNull("Expected non-null error message", message);
    Assert.assertTrue("Expected error message to contain '" + expectedText + "' but was '" + message + "'",
                      message.indexOf(expectedText) >= 0);
  }

  @Override
  protected boolean cleanTempDir() {
    return false;
  }

  /**
   * Test that missing bundle will cause ModulesLoader to throw exception and also that the message thrown
   */
  public void testMissingModule() throws Exception {
    String nonexistentBundle = "IDoNotExistThereforeIAmNot";
    String nonexistentVersion = "1.0.0";

    DSOClientConfigHelper configHelper = configHelper();
    configHelper.addModule(nonexistentBundle, nonexistentVersion);
    ClassProvider classProvider = new MockClassProvider();

    try {
      Modules modules = configHelper.getModulesForInitialization();
      EmbeddedOSGiRuntime osgiRuntime = EmbeddedOSGiRuntime.Factory.createOSGiRuntime(modules);
      ModulesLoader.initModules(osgiRuntime, configHelper, classProvider, modules.getModules(), false);
      Assert.fail("Should get exception on missing bundle");

    } catch (BundleException e) {
      checkErrorMessageContainsText(e, nonexistentBundle);
      checkErrorMessageContainsText(e, nonexistentVersion);
    }
  }

  /**
   * CDV-552 - when a module with a bad bundle version is loaded, tell the user which module caused startup to fail.
   */
  public void testBadModuleBundleManifestVersion() throws Exception {
    String badGroupId = "org.terracotta.modules";
    String badArtifactId = "badversion";
    String badVersion = "1.0.0.SNAPSHOT";
    String badSymbolicName = badGroupId + "." + badArtifactId;
    String badSymbolicVersion = "TedNugent";

    // Create bundle jar based on these attributes
    // Note: on windows, when this osgi startup fails, the jar file is left
    // open and prevents deletion. So, for this particular test, we write
    // instead to the java temp dir and mark to delete on exit
    File tempDir = this.getTempDirectory();
    File generatedJar1 = createBundle(tempDir, badGroupId, badArtifactId, badVersion, badSymbolicName,
                                      badSymbolicVersion, null);
    generatedJar1.deleteOnExit();

    EmbeddedOSGiRuntime osgiRuntime = null;
    try {
      DSOClientConfigHelper configHelper = configHelper();

      // Add temp dir to list of repository locations to pick up bundle above
      configHelper.addRepository(tempDir.getAbsolutePath());
      configHelper.addModule(badArtifactId, badVersion);
      ClassProvider classProvider = new MockClassProvider();

      try {
        Modules modules = configHelper.getModulesForInitialization();
        osgiRuntime = EmbeddedOSGiRuntime.Factory.createOSGiRuntime(modules);
        ModulesLoader.initModules(osgiRuntime, configHelper, classProvider, modules.getModules(), false);
        Assert.fail("Should get exception on invalid bundle");

      } catch (BundleException e) {
        checkErrorMessageContainsText(e, badGroupId);
        checkErrorMessageContainsText(e, badArtifactId);
        checkErrorMessageContainsText(e, badVersion);
      }

    } finally {
      shutdownAndCleanUpJars(osgiRuntime, null);
    }
  }

  private File createBundle(File repositoryRootDir, String groupId, String artifactId, String version,
                            String bundleName, String bundleVersion, String requiredBundles) throws IOException {
    File groupLocation = new File(repositoryRootDir, groupId.replace('.', File.separatorChar));
    File nameLocation = new File(groupLocation, artifactId);
    File versionLocation = new File(nameLocation, version);
    versionLocation.mkdirs();
    File bundleFile = new File(versionLocation, artifactId + "-" + version + ".jar");

    Map manifestAttributes = new HashMap();
    manifestAttributes.put("Bundle-SymbolicName", bundleName);
    manifestAttributes.put("Bundle-Version", bundleVersion);
    if (requiredBundles != null) {
      manifestAttributes.put("RequireBundle", requiredBundles);
    }

    createBundleWithManifest(bundleFile.getAbsolutePath(), manifestAttributes);

    return bundleFile;
  }

  private void createBundleWithManifest(String jarLocation, Map manifestProps) throws IOException {
    Manifest manifest = new Manifest();
    Attributes attributes = manifest.getMainAttributes();
    Iterator attrIter = manifestProps.entrySet().iterator();
    attributes.put(Name.MANIFEST_VERSION, "1.0");
    while (attrIter.hasNext()) {
      Map.Entry entry = (Map.Entry) attrIter.next();
      attributes.putValue((String) entry.getKey(), (String) entry.getValue());
    }

    FileOutputStream fstream = new FileOutputStream(jarLocation);
    JarOutputStream stream = new JarOutputStream(fstream, manifest);
    stream.close();
  }

  /**
   * Test that using a windows file path with spaces does not throw an exception
   */
  public void testWindowsFilePathRepository() throws Exception {
    String repo = "c:\\evil windows\\path";

    DSOClientConfigHelper configHelper = configHelper();
    ClassProvider classProvider = new MockClassProvider();

    EmbeddedOSGiRuntime osgiRuntime = null;
    try {
      configHelper.addRepository(repo);
      Modules modules = configHelper.getModulesForInitialization();
      osgiRuntime = EmbeddedOSGiRuntime.Factory.createOSGiRuntime(modules);
      ModulesLoader.initModules(osgiRuntime, configHelper, classProvider, modules.getModules(), false);
    } finally {
      shutdownAndCleanUpJars(osgiRuntime, null);
    }
  }

  public void testModuleInMultipleRepo() throws Exception {
    String multipleRepoGroupId = "org.terracotta.modules";
    String multipleRepoArtifactId = "multiplerepomodule";
    String multRepoVersion = "1.0.0";
    String multRepoSymbolicName = multipleRepoGroupId + "." + multipleRepoArtifactId;

    // Create bundle jar based on these attributes
    File tempDir = getTempDirectory();
    File repo1 = new File(tempDir, "repo1");
    File repo2 = new File(tempDir, "repo2");
    repo1.mkdir();
    repo2.mkdir();

    File generatedJar1 = createBundle(repo1, multipleRepoGroupId, multipleRepoArtifactId, multRepoVersion,
                                      multRepoSymbolicName, multRepoVersion, null);
    File generatedJar2 = createBundle(repo2, multipleRepoGroupId, multipleRepoArtifactId, multRepoVersion,
                                      multRepoSymbolicName, multRepoVersion, null);

    EmbeddedOSGiRuntime osgiRuntime = null;
    try {
      DSOClientConfigHelper configHelper = configHelper();

      // Add both repo's to list of repository locations to pick up bundle above
      configHelper.addRepository(repo1.getAbsolutePath());
      configHelper.addRepository(repo2.getAbsolutePath());
      configHelper.addModule(multipleRepoArtifactId, multRepoVersion);
      ClassProvider classProvider = new MockClassProvider();

      Modules modules = configHelper.getModulesForInitialization();
      osgiRuntime = EmbeddedOSGiRuntime.Factory.createOSGiRuntime(modules);
      ModulesLoader.initModules(osgiRuntime, configHelper, classProvider, modules.getModules(), false);

    } finally {
      shutdownAndCleanUpJars(osgiRuntime, new File[] { generatedJar1, generatedJar2 });
    }
  }

  /**
   * Test add module programmatically with non-default groupId
   */
  public void testNonDefaultGroupId() throws Exception {
    String groupId = "org.foo";
    String artifactId = "bar";
    String version = "1.0.0";
    String symbolicName = groupId + "." + artifactId;

    // Create bundle jar based on these attributes
    File tempDir = getTempDirectory();
    File generatedJar1 = createBundle(tempDir, groupId, artifactId, version, symbolicName, version, null);

    EmbeddedOSGiRuntime osgiRuntime = null;
    try {
      DSOClientConfigHelper configHelper = configHelper();

      // Add temp dir to list of repository locations to pick up bundle above
      configHelper.addRepository(tempDir.getAbsolutePath());
      configHelper.addModule(groupId, artifactId, version);
      ClassProvider classProvider = new MockClassProvider();

      Modules modules = configHelper.getModulesForInitialization();
      osgiRuntime = EmbeddedOSGiRuntime.Factory.createOSGiRuntime(modules);
      ModulesLoader.initModules(osgiRuntime, configHelper, classProvider, modules.getModules(), false);

      // should find and load the module without error

    } finally {
      shutdownAndCleanUpJars(osgiRuntime, new File[] { generatedJar1 });
    }
  }

  public void testDotFileInRepo() throws Exception {
    String groupId = "org.terracotta.modules";
    String artifactId = "somemodule";
    String version = "1.0.0";
    String symbolicName = groupId + "." + artifactId;

    // Create bundle jar based on these attributes
    File repo = new File(getTempDirectory(), "repowithdots");
    ensureDir(repo);

    // create a couples of .svn dirs
    File dotSvnDir = new File(repo, ".svn");
    Assert.assertTrue(dotSvnDir.mkdirs());
    dotSvnDir = new File(repo, "org/.svn");
    Assert.assertTrue(dotSvnDir.mkdirs());
    dotSvnDir = new File(repo, "org/teracotta/.svn");
    Assert.assertTrue(dotSvnDir.mkdirs());
    dotSvnDir = new File(repo, "org/terracotta/modules/.svn");
    Assert.assertTrue(dotSvnDir.mkdirs());
    dotSvnDir = new File(repo, "org/terracotta/modules/somemodule/.svn");
    Assert.assertTrue(dotSvnDir.mkdirs());
    dotSvnDir = new File(repo, "org/terracotta/modules/somemodule/1.0.0/.svn");
    Assert.assertTrue(dotSvnDir.mkdirs());

    File generatedJar1 = createBundle(repo, groupId, artifactId, version, symbolicName, version, null);

    EmbeddedOSGiRuntime osgiRuntime = null;
    try {
      DSOClientConfigHelper configHelper = configHelper();

      configHelper.addRepository(repo.getAbsolutePath());
      configHelper.addModule(artifactId, version);
      ClassProvider classProvider = new MockClassProvider();

      Modules modules = configHelper.getModulesForInitialization();
      osgiRuntime = EmbeddedOSGiRuntime.Factory.createOSGiRuntime(modules);
      ModulesLoader.initModules(osgiRuntime, configHelper, classProvider, modules.getModules(), false);
    } finally {
      shutdownAndCleanUpJars(osgiRuntime, new File[] { generatedJar1 });
    }
  }

  private void ensureDir(File dir) throws IOException {
    if (dir.exists()) FileUtils.deleteDirectory(dir);
    dir.mkdirs();
    Assert.assertTrue(dir.isDirectory());
  }

  private void shutdownAndCleanUpJars(EmbeddedOSGiRuntime osgiRuntime, File[] jars) {
    // Shutdown and wait for open jar references to get cleaned up
    if (osgiRuntime != null) {
      osgiRuntime.shutdown();
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        // ignore
      }
    }

    if (jars != null) {
      for (File jar : jars) {
        jar.delete();
      }
    }
  }

}
