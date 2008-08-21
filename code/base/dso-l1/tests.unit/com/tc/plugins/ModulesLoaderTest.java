/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.plugins;

import org.osgi.framework.BundleException;

import com.tc.bundles.EmbeddedOSGiRuntime;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.loaders.ClassProvider;
import com.tc.util.Assert;
import com.terracottatech.config.Modules;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

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
    Assert.assertTrue("Expected error message to contain '" + expectedText + "' but was '" + message + "'", message
        .indexOf(expectedText) >= 0);
  }

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
      ModulesLoader.initModules(osgiRuntime, configHelper, classProvider, modules.getModuleArray(), false);
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
    File tempDir = new File(System.getProperty("java.io.tmpdir"));
    File generatedJar1 = createBundle(tempDir, badGroupId, badArtifactId, badVersion, badSymbolicName,
                                      badSymbolicVersion, null, null);
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
        ModulesLoader.initModules(osgiRuntime, configHelper, classProvider, modules.getModuleArray(), false);
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
                            String bundleName, String bundleVersion, String requiredBundles, String tcXml)
      throws IOException {
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

    createBundleWithManifest(bundleFile.getAbsolutePath(), manifestAttributes, tcXml);

    return bundleFile;
  }

  private void createBundleWithManifest(String jarLocation, Map manifestProps, String tcXml) throws IOException {
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

    if (tcXml != null) {
      stream.putNextEntry(new JarEntry("terracotta.xml"));
      OutputStreamWriter writer = new OutputStreamWriter(stream, "utf-8");
      writer.write(tcXml);
      writer.flush();
      stream.closeEntry();
    }

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
      ModulesLoader.initModules(osgiRuntime, configHelper, classProvider, modules.getModuleArray(), false);
    } finally {
      shutdownAndCleanUpJars(osgiRuntime, null);
    }
  }

  /**
   * CDV-553 - when a module with a bad terracotta.xml is loaded, throw an error and identify the module
   */
  public void testBadModuleXmlThrowsError() throws Exception {
    String badGroupId = "org.terracotta.modules";
    String badArtifactId = "badxml";
    String badVersion = "1.0.0";
    String badSymbolicName = badGroupId + "." + badArtifactId;

    // Create bundle jar based on these attributes
    File tempDir = getTempDirectory();
    File generatedJar1 = createBundle(tempDir, badGroupId, badArtifactId, badVersion, badSymbolicName, badVersion,
                                      null, TC_CONFIG_MISSING_FIRST_CHAR);

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
        ModulesLoader.initModules(osgiRuntime, configHelper, classProvider, modules.getModuleArray(), false);
        Assert.fail("Should get exception on invalid bundle");

      } catch (BundleException e) {
        checkErrorMessageContainsText(e, badGroupId);
        checkErrorMessageContainsText(e, badArtifactId);
        checkErrorMessageContainsText(e, badVersion);
      }

    } finally {
      shutdownAndCleanUpJars(osgiRuntime, new File[] { generatedJar1 });
    }
  }

  // XXX This test is now obsolete, the contraint that this test is checking is now enforeced by the XML Schema
  // - will have to create an equivalent test in ConfigLoaderTest
  /**
   * Test catch and throw of ConfigSetupException due to root with no field or expression public void
   * testBadModuleConfig_rootNoField() throws Exception { String badGroupId = "org.terracotta.modules"; String
   * badArtifactId = "badconfig"; String badVersion = "1.0.0"; String badSymbolicName = badGroupId + "." +
   * badArtifactId; // Create bundle jar based on these attributes File tempDir = getTempDirectory(); File generatedJar1
   * = createBundle(tempDir, badGroupId, badArtifactId, badVersion, badSymbolicName, badVersion, null,
   * TC_CONFIG_NO_ROOT_FIELD_OR_EXPR); EmbeddedOSGiRuntime osgiRuntime = null; try { DSOClientConfigHelper configHelper
   * = configHelper(); // Add temp dir to list of repository locations to pick up bundle above
   * configHelper.addRepository(tempDir.getAbsolutePath()); configHelper.addModule(badArtifactId, badVersion);
   * ClassProvider classProvider = new MockClassProvider(); try { Modules modules =
   * configHelper.getModulesForInitialization(); osgiRuntime = EmbeddedOSGiRuntime.Factory.createOSGiRuntime(modules);
   * ModulesLoader.initModules(osgiRuntime, configHelper, classProvider, modules.getModuleArray(), false);
   * Assert.fail("Should get exception on invalid config"); } catch (BundleException e) {
   * checkErrorMessageContainsText(e, badGroupId); checkErrorMessageContainsText(e, badArtifactId);
   * checkErrorMessageContainsText(e, badVersion); checkErrorMessageContainsText(e, "no_expr"); // check root name is in
   * message } } finally { shutdownAndCleanUpJars(osgiRuntime, new File[] { generatedJar1 }); } } private static final
   * String TC_CONFIG_NO_ROOT_FIELD_OR_EXPR = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + "<xml-fragment>" +
   * "<roots>" + "<root><root-name>no_expr</root-name></root>" + "</roots>" + "</xml-fragment>";
   */

  private static final String TC_CONFIG_MISSING_FIRST_CHAR = "?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                                                             + "<xml-fragment>" + "</xml-fragment>";

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
      for (int i = 0; i < jars.length; i++) {
        jars[i].delete();
      }
    }
  }

}
