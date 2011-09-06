/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.terracotta.modules.tool.config.Config;

import com.tc.test.TCTestCase;
import com.tc.test.TempDirectoryHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract base class for command test cases.
 */
public abstract class AbstractCommandTestCase extends TCTestCase {

  private Properties props;
  private File       tempDir;
  private File       indexFile;
  private File       repoDir;
  private File       modulesDir;

  protected Properties setupConfig(String tcVersion, String timApiVersion) throws Exception {
    props = new Properties();

    props.setProperty(getKey(Config.TC_VERSION), tcVersion);
    props.setProperty(getKey(Config.TIM_API_VERSION), timApiVersion);

    tempDir = new TempDirectoryHelper(getClass(), true).getDirectory();
    tempDir.mkdir();
    indexFile = new File(tempDir, "index.xml");
    repoDir = new File(tempDir, "repo");
    repoDir.mkdir();
    File cacheDir = new File(tempDir, "cache");
    cacheDir.mkdir();
    modulesDir = new File(tempDir, "modules");
    modulesDir.mkdir();

    props.setProperty(getKey(Config.DATA_FILE_URL), indexFile.toURI().toString());
    props.setProperty(getKey(Config.RELATIVE_URL_BASE), repoDir.toURI().toString());
    props.setProperty(getKey(Config.CACHE), cacheDir.getAbsolutePath());
    props.setProperty(getKey(Config.MODULES_DIR), modulesDir.getAbsolutePath());
    props.setProperty(getKey(Config.DATA_CACHE_EXPIRATION), "86400");
    props.setProperty(getKey(Config.INCLUDE_SNAPSHOTS), "true");

    System.out.println("Using tim-get.properties: " + props);

    return props;
  }

  protected Properties getConfigProps() {
    return this.props;
  }

  protected File getTempDir() {
    return this.tempDir;
  }

  protected File getIndexFile() {
    return this.indexFile;
  }

  protected File getRepoDir() {
    return this.repoDir;
  }

  protected File getModulesDir() {
    return this.modulesDir;
  }

  protected void copyResourceToFile(String sourceResourcePath, File file) throws Exception {
    if (file.exists()) {
      file.delete();
    }
    InputStream sourceStream = getClass().getResourceAsStream(sourceResourcePath);
    List lines = IOUtils.readLines(sourceStream);
    FileUtils.writeLines(file, "UTF-8", lines);
  }

  protected String getKey(String keyPart) {
    return Config.KEYSPACE + keyPart;
  }

  protected void assertMatches(StringWriter out, String regex) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(out.toString());
    assertTrue(matcher.find());
  }

  /**
   * Create jar file in the modules directory based on these coordinates (at the appropriate path and with the
   * appropriate manifest attributes. The jar won't have anything else in it.
   */
  protected void createDummyModule(String groupId, String artifactId, String version, File rootDir) throws Exception {
    Manifest manifest = new Manifest();
    Attributes attributes = manifest.getMainAttributes();
    attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    attributes.putValue(ManifestAttributes.OSGI_SYMBOLIC_NAME.attribute(), groupId + "." + artifactId);
    attributes.putValue(ManifestAttributes.OSGI_VERSION.attribute(), version);

    File moduleDirPath = new File(rootDir, getModulePath(groupId, artifactId, version));
    moduleDirPath.mkdirs();
    File modulePath = new File(moduleDirPath, getArtifactName(artifactId, version));

    FileOutputStream fstream = new FileOutputStream(modulePath);
    JarOutputStream stream = new JarOutputStream(fstream, manifest);
    stream.flush();
    stream.close();
  }

  protected String getModulePath(String groupId, String artifactId, String version) {
    return groupId.replace(".", File.separator) + File.separatorChar + artifactId + File.separatorChar + version;
  }

  protected String getArtifactName(String artifactId, String version) {
    return artifactId + "-" + version + ".jar";
  }

}
