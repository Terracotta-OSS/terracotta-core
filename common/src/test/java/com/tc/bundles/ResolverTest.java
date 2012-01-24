/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.apache.commons.io.FileUtils;
import org.osgi.framework.BundleException;

import com.tc.bundles.exception.MissingDefaultRepositoryException;
import com.tc.test.TCTestCase;
import com.tc.util.ProductInfo;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResolverTest extends TCTestCase {
  private static final Pattern ARTIFACT_PATTERN = Pattern.compile("(.*)-(\\d+.*)");
  private static boolean       PASS             = true;
  private static boolean       FAIL             = false;

  private final String         tcVersion;
  private final String         testRepo;

  public ResolverTest() {
    try {
      testRepo = getTempDirectory().getAbsolutePath() + File.separator + "testRepo";
      new File(testRepo).mkdir();
      System.setProperty("com.tc.l1.modules.repositories", testRepo);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    ProductInfo info = ProductInfo.getInstance();
    tcVersion = info.mavenArtifactsVersion();
  }

  public void testResolveBundle() throws IOException {
    resolveBundles(new String[] { testRepo }, jarFiles(), PASS);
  }

  public void testResolveBundleInFlatRepo() throws IOException {
    String flatRepoUrl = makeFlatRepo("modules.1");
    resolveBundles(new String[] { flatRepoUrl }, jarFiles(), PASS);
  }

  public void testResolveBundleInFlatRepoWithSpaces() throws IOException {
    String flatRepoUrl = makeFlatRepo("modules 1");
    resolveBundles(new String[] { flatRepoUrl }, jarFiles(), PASS);
  }

  public void testResolveBundleWithFlatAndMavenLikeRepos() throws IOException {
    resolveBundles(splitRepo("modules.2", "modules.3"), jarFiles(), PASS);
  }

  public void testFindJar() {
    resolveJars(new String[] { testRepo }, jarFiles(), PASS);
  }

  public void testFindJarInFlatRepo() throws IOException {
    String flatRepoUrl = makeFlatRepo("modules.1");
    resolveJars(new String[] { flatRepoUrl }, jarFiles(), PASS);
  }

  public void testNotFindJar() {
    String[] repos = { testRepo };
    resolve(repos, "foobar", "0.0.0-SNAPSHOT", FAIL);
  }

  public void testFindJarWithFlatAndMavenLikeRepos() throws IOException {
    resolveJars(splitRepo("modules.2", "modules.3"), jarFiles(), PASS);
  }

  // CDV-691
  public void testModuleWithNoVersion() throws Exception {
    resolve(new String[] { testRepo }, "foo", null, false);
  }

  public void testModuleWithNoName() throws Exception {
    resolve(new String[] { testRepo }, null, "1.0.0", false);
  }

  public void testAcceptGoodRepositories() throws IOException {
    String[] repo = { "modules", "modules-repo", "modules-repo.1", "modules repo with space" };
    for (int i = 0; i < repo.length; i++)
      repo[i] = makeRepoDir(repo[i]);

    for (String element : repo)
      assertNotNull("repository location '" + element + "' ignored.", Resolver.resolveRepositoryLocation(element));

    for (int i = 0; i < repo.length; i++) {
      repo[i] = "file://" + repo[i];
      assertNotNull("repository URL '" + repo[i] + "' ignored.", Resolver.resolveRepositoryLocation(repo[i]));
    }
  }

  public void testIgnoreBadRepositories() throws IOException {
    String[] repo = { "modules-repo", "modules-repo.1", "modules repo with space" };

    for (int i = 0; i < repo.length; i++)
      repo[i] = deleteRepoDir(repo[i]);

    for (String element : repo) {
      File repoDir = new File(element);
      assertFalse("repository location '" + element + "' should've been deleted.", repoDir.exists());
      assertNull("non-existent repository location '" + element + "' was not ignored.",
                 Resolver.resolveRepositoryLocation(element));
    }

    for (int i = 0; i < repo.length; i++) {
      repo[i] = "file://" + repo[i];
      File repoDir = new File(repo[i]);
      assertFalse("repository URL '" + repo[i] + "' should've been deleted.", repoDir.exists());
      assertNull("non-existent repository URL '" + repo[i] + "' was not ignored.",
                 Resolver.resolveRepositoryLocation(repo[i]));
    }

    for (int i = 0; i < repo.length; i++) {
      repo[i] = "http://" + repo[i];
      assertNull("bad protocol used to specify repository URL '" + repo[i] + "' but was not ignored.",
                 Resolver.resolveRepositoryLocation(repo[i]));
    }
  }

  public void testAllModulesCanBeLoadedWithoutVersion() throws Exception {
    String[] repoLocation = new String[] { testRepo };
    Collection<File> files = jarFiles();
    for (File file : files) {
      Manifest manifest = Resolver.getManifest(file.toURI().toURL());
      if (manifest != null) {
        Attributes attrs = manifest.getMainAttributes();
        String symbolicName = attrs.getValue(Resolver.BUNDLE_SYMBOLICNAME);
        if (symbolicName != null) {
          String groupId = OSGiToMaven.groupIdFromSymbolicName(symbolicName);
          String artifactId = OSGiToMaven.artifactIdFromSymbolicName(symbolicName);
          File resolvedFile = resolve(repoLocation, groupId, artifactId, null, true);

          assertNotNull(resolvedFile);
          assertEquals(file, resolvedFile);
        }
      }
    }
  }

  private String makeRepoDir(String repoName) throws IOException {
    String repoUrl = getTempDirectory().getAbsolutePath() + File.separator + repoName;
    File repoDir = new File(repoUrl);
    repoDir.mkdir();
    repoDir.deleteOnExit();
    return repoUrl;
  }

  private String deleteRepoDir(String repoName) throws IOException {
    String repoUrl = getTempDirectory().getAbsolutePath() + File.separator + repoName;
    File repoDir = new File(repoUrl);
    if (repoDir.exists()) {
      FileUtils.deleteDirectory(repoDir);
    }
    return repoUrl;
  }

  private String repoPropToFile() {
    String prop = System.getProperty("com.tc.l1.modules.repositories");
    if (prop.startsWith("file:")) {
      try {
        return FileUtils.toFile(new URL(prop)).getAbsolutePath();
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    } else {
      return prop;
    }
  }

  private String[] splitRepo(String name1, String name2) throws IOException {
    String repoUrl = repoPropToFile();
    String repoUrl1 = makeRepoDir(name1);
    String repoUrl2 = makeRepoDir(name2);
    FileUtils.copyDirectory(new File(repoUrl), new File(repoUrl2));
    Object[] jars = jarFiles(new File(repoUrl2)).toArray();
    for (int i = 0; i < jars.length / 2; i++) {
      File srcfile = (File) jars[i];
      FileUtils.copyFileToDirectory(srcfile, new File(repoUrl1));
      srcfile.delete();
      srcfile.getParentFile().delete();
      srcfile.getParentFile().getParentFile().delete();
    }
    return new String[] { repoUrl1, repoUrl2 };
  }

  private String makeFlatRepo(String name) throws IOException {
    String flatRepoUrl = makeRepoDir(name);
    Collection<File> jars = jarFiles();
    for (File file : jars) {
      FileUtils.copyFileToDirectory(file, new File(flatRepoUrl));
    }
    return flatRepoUrl;
  }

  private Collection<File> jarFiles() {
    String jarFileDir = repoPropToFile();
    return jarFiles(new File(jarFileDir));
  }

  private Collection<File> jarFiles(File directory) {
    return FileUtils.listFiles(directory, new String[] { "jar" }, true);
  }

  private void resolveJars(String[] repos, Collection<File> jars, boolean expected) {
    for (File jar : jars) {
      String jarName = jar.getName();
      jarName = jarName.substring(0, jarName.lastIndexOf(".jar"));
      Matcher m = ARTIFACT_PATTERN.matcher(jarName);
      if (m.matches()) {
        String name = m.group(1);
        String version = m.group(2);
        resolve(repos, name, version, expected);
      } else {
        throw new RuntimeException("Can't parse name and veresion from jar name: " + jarName);
      }
    }
  }

  private void resolveBundles(String[] repos, Collection jars, boolean expected) throws IOException {
    for (Iterator i = jars.iterator(); i.hasNext();) {
      JarFile jar = new JarFile((File) i.next());
      Manifest manifest = jar.getManifest();
      String[] reqmts = BundleSpec.getRequirements(manifest);
      for (String reqmt : reqmts) {
        BundleSpec spec = BundleSpec.newInstance(reqmt);
        resolveBundle(repos, spec, expected);
      }
    }
  }

  private void resolveBundle(String[] repos, BundleSpec spec, boolean expected) throws IOException {
    try {
      Resolver resolver = new Resolver(repos, false, tcVersion);
      URL location = resolver.resolveBundle(spec);
      File file = FileUtils.toFile(location);

      if (expected) {
        assertNotNull(spec.getSymbolicName(), file);
        assertEquals(file.getAbsolutePath().endsWith(".jar"), expected);
        JarFile jar = new JarFile(file);
        Manifest manifest = jar.getManifest();
        String symbolicName = BundleSpec.getSymbolicName(manifest);
        String version = BundleSpec.getVersion(manifest);
        assertEquals(symbolicName, spec.getSymbolicName());
        assertEquals(version, spec.getVersion());
      } else {
        assertNull(file);
      }
    } catch (MissingDefaultRepositoryException e) {
      fail(e.getMessage());
    }
  }

  private void resolve(String[] repos, String name, String version, boolean expected) {
    resolve(repos, "org.terracotta.modules", name, version, expected);
  }

  private File resolve(String[] repos, String groupId, String name, String version, boolean expected) {
    try {
      Resolver resolver = new Resolver(repos, false, tcVersion);
      Module module = new Module(groupId, name, version);
      File file = FileUtils.toFile(resolver.resolve(module));
      if (version != null) {
        assertEquals(expected, file.getAbsolutePath().endsWith(name + "-" + version + ".jar"));
      } else {
        assertEquals(expected, file.getAbsolutePath().indexOf(name) >= 0);
      }
      return file;
    } catch (BundleException e) {
      if (PASS == expected) fail(e.getMessage());
      else assertTrue(FAIL == expected);
      return null;
    }
  }

}
