/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.apache.commons.io.FileUtils;
import org.osgi.framework.BundleException;

import com.tc.test.TestConfigObject;
import com.terracottatech.config.Module;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import junit.framework.TestCase;

public class ResolverTest extends TestCase {
  private static boolean PASS = true;
  private static boolean FAIL = false;

  public void testResolveBundle() throws IOException {
    resolveBundles(new String[] { System.getProperty("com.tc.l1.modules.repositories") }, jarFiles(), PASS);
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
    resolveJars(new String[] { System.getProperty("com.tc.l1.modules.repositories") }, jarFiles(), PASS);
  }

  public void testFindJarInFlatRepo() throws IOException {
    String flatRepoUrl = makeFlatRepo("modules.1");
    resolveJars(new String[] { flatRepoUrl }, jarFiles(), PASS);
  }

  public void testNotFindJar() {
    String[] repos = { System.getProperty("com.tc.l1.modules.repositories") };
    resolve(repos, "foobar", "0.0.0-SNAPSHOT", FAIL);
  }

  public void testFindJarWithFlatAndMavenLikeRepos() throws IOException {
    resolveJars(splitRepo("modules.2", "modules.3"), jarFiles(), PASS);
  }

  // CDV-691
  public void testModuleWithNoVersion() throws Exception {
    resolve(new String[] { System.getProperty("com.tc.l1.modules.repositories") }, "foo", null, false);
  }

  public void testModuleWithNoName() throws Exception {
    resolve(new String[] { System.getProperty("com.tc.l1.modules.repositories") }, null, "1.0.0", false);
  }

  public void testAcceptGoodRepositories() {
    String[] repo = { "modules", "modules-repo", "modules-repo.1", "modules repo with space" };
    for (int i = 0; i < repo.length; i++)
      repo[i] = makeRepoDir(repo[i]);

    for (int i = 0; i < repo.length; i++)
      assertNotNull("repository location '" + repo[i] + "' ignored.", Resolver.resolveRepositoryLocation(repo[i]));

    for (int i = 0; i < repo.length; i++) {
      repo[i] = "file://" + repo[i];
      assertNotNull("repository URL '" + repo[i] + "' ignored.", Resolver.resolveRepositoryLocation(repo[i]));
    }
  }

  public void testIgnoreBadRepositories() {
    String[] repo = { "modules-repo", "modules-repo.1", "modules repo with space" };

    for (int i = 0; i < repo.length; i++)
      repo[i] = deleteRepoDir(repo[i]);

    for (int i = 0; i < repo.length; i++) {
      File repoDir = new File(repo[i]);
      assertFalse("repository location '" + repo[i] + "' should've been deleted.", repoDir.exists());
      assertNull("non-existent repository location '" + repo[i] + "' was not ignored.", Resolver
          .resolveRepositoryLocation(repo[i]));
    }

    for (int i = 0; i < repo.length; i++) {
      repo[i] = "file://" + repo[i];
      File repoDir = new File(repo[i]);
      assertFalse("repository URL '" + repo[i] + "' should've been deleted.", repoDir.exists());
      assertNull("non-existent repository URL '" + repo[i] + "' was not ignored.", Resolver
          .resolveRepositoryLocation(repo[i]));
    }

    for (int i = 0; i < repo.length; i++) {
      repo[i] = "http://" + repo[i];
      assertNull("bad protocol used to specify repository URL '" + repo[i] + "' but was not ignored.", Resolver
          .resolveRepositoryLocation(repo[i]));
    }
  }

  private String makeRepoDir(String repoName) {
    String repoUrl = System.getProperty(TestConfigObject.TC_BASE_DIR) + File.separator + "build" + File.separator
                     + repoName;
    File repoDir = new File(repoUrl);
    repoDir.mkdir();
    repoDir.deleteOnExit();
    return repoUrl;
  }

  private String deleteRepoDir(String repoName) {
    String repoUrl = System.getProperty(TestConfigObject.TC_BASE_DIR) + File.separator + "build" + File.separator
                     + repoName;
    File repoDir = new File(repoUrl);
    if (repoDir.exists()) try {
      FileUtils.deleteDirectory(repoDir);
    } catch (IOException e) {
      // 
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
    Collection jars = jarFiles();
    for (Iterator i = jars.iterator(); i.hasNext();) {
      FileUtils.copyFileToDirectory((File) i.next(), new File(flatRepoUrl));
    }
    return flatRepoUrl;
  }

  private Collection jarFiles() {
    String jarFileDir = repoPropToFile();
    return jarFiles(new File(jarFileDir));
  }

  private Collection jarFiles(File directory) {
    return FileUtils.listFiles(directory, new String[] { "jar" }, true);
  }

  private void resolveJars(String[] repos, Collection jars, boolean expected) {
    for (Iterator i = jars.iterator(); i.hasNext();) {
      File jar = new File(i.next().toString());
      String version = "2.7.0-SNAPSHOT";
      String name = jar.getName().replaceAll("-" + version + ".jar", "");
      resolve(repos, name, version, expected);
    }
  }

  private void resolveBundles(String[] repos, Collection jars, boolean expected) throws IOException {
    for (Iterator i = jars.iterator(); i.hasNext();) {
      JarFile jar = new JarFile((File) i.next());
      Manifest manifest = jar.getManifest();
      String[] reqmts = BundleSpec.getRequirements(manifest);
      for (int j = 0; j < reqmts.length; j++) {
        BundleSpec spec = BundleSpec.newInstance(reqmts[j]);
        resolveBundle(repos, spec, expected);
      }
    }
  }

  private void resolveBundle(String[] repos, BundleSpec spec, boolean expected) throws IOException {
    //try {
    Resolver resolver = new Resolver(repos, false);
    File file = resolver.resolveBundle(spec);

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
    //} catch (BundleException e) {
    //  if (PASS == expected) fail(e.getMessage());
    //  else assertTrue(FAIL == expected);
    //}
  }

  private void resolve(String[] repos, String name, String version, boolean expected) {
    try {
      Resolver resolver = new Resolver(repos, false);
      Module module = Module.Factory.newInstance();
      module.setName(name);
      module.setVersion(version);
      module.setGroupId("org.terracotta.modules");
      File file = resolver.resolve(module);
      assertEquals(file.getAbsolutePath().endsWith(name + "-" + version + ".jar"), expected);
    } catch (BundleException e) {
      if (PASS == expected) fail(e.getMessage());
      else assertTrue(FAIL == expected);
    }
  }
}
