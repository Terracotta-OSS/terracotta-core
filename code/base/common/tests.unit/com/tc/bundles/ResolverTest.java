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
    URL repoUrl = new URL(System.getProperty("com.tc.l1.modules.repositories"));
    resolveBundles(new URL[] { repoUrl }, jarFiles(), PASS);
  }
  
  public void testResolveBundleInFlatRepo() throws IOException {
    URL flatRepoUrl = makeFlatRepo("modules.1");
    resolveBundles(new URL[] { flatRepoUrl }, jarFiles(), PASS);
  }
  
  public void testResolveBundleWithFlatAndMavenLikeRepos() throws IOException {
    resolveBundles(splitRepo("modules.2", "modules.3"), jarFiles(), PASS);
  }
  
  public void testFindJar() throws IOException {
    URL repoUrl = new URL(System.getProperty("com.tc.l1.modules.repositories"));
    resolveJars(new URL[] { repoUrl }, jarFiles(), PASS);
  }

  public void testFindJarInFlatRepo() throws IOException {
    URL flatRepoUrl = makeFlatRepo("modules.1");
    resolveJars(new URL[] { flatRepoUrl }, jarFiles(), PASS);
  }
  
  public void testNotFindJar() throws IOException {
    URL[] repos = { new URL(System.getProperty("com.tc.l1.modules.repositories")) };
    resolve(repos, "foobar", "0.0.0-SNAPSHOT", FAIL);
  }

  public void testFindJarWithFlatAndMavenLikeRepos() throws IOException {
    resolveJars(splitRepo("modules.2", "modules.3"), jarFiles(), PASS);
  }
 
  // CDV-691
  public void testModuleWithNoVersion() throws Exception {
    resolve(new URL[] {}, "foo", null, false);
  }

  public void testModuleWithNoName() throws Exception {
    resolve(new URL[] {}, null, "1.0.0", false);
  }

  private URL makeRepoDir(String repoName) throws IOException {
    URL flatRepoUrl = new URL("file://" + System.getProperty(TestConfigObject.TC_BASE_DIR) + File.separator + "build"
                              + File.separator + repoName);
    File repoDir = FileUtils.toFile(flatRepoUrl);
    repoDir.mkdir();
    repoDir.deleteOnExit();
    return flatRepoUrl;
  }

  private URL[] splitRepo(String name1, String name2) throws IOException {
    URL repoUrl = new URL(System.getProperty("com.tc.l1.modules.repositories"));
    URL repoUrl1 = makeRepoDir(name1);
    URL repoUrl2 = makeRepoDir(name2);
    FileUtils.copyDirectory(FileUtils.toFile(repoUrl), FileUtils.toFile(repoUrl2));
    Object[] jars = jarFiles(FileUtils.toFile(repoUrl2)).toArray();
    for (int i = 0; i < jars.length; i++) {
      File srcfile = (File) jars[i];
      if (i % 2 != 0) FileUtils.copyFileToDirectory(srcfile, FileUtils.toFile(repoUrl1));
      else {
        srcfile.delete();
        srcfile.getParentFile().delete();
        srcfile.getParentFile().getParentFile().delete();
      }
    }
    return new URL[] { repoUrl1, repoUrl2 };
  }

  private URL makeFlatRepo(String name) throws IOException {
    URL flatRepoUrl = makeRepoDir(name);
    Collection jars = jarFiles();
    for (Iterator i = jars.iterator(); i.hasNext();) {
      FileUtils.copyFileToDirectory((File) i.next(), FileUtils.toFile(flatRepoUrl));
    }
    return flatRepoUrl;
  }

  private Collection jarFiles() throws IOException {
    URL url = new URL(System.getProperty("com.tc.l1.modules.repositories"));
    return jarFiles(FileUtils.toFile(url));
  }
  
  private Collection jarFiles(File directory) {
    return FileUtils.listFiles(directory, new String[] { "jar" }, true);
  }

  private void resolveJars(URL[] repos, Collection jars, boolean expected) {
    for (Iterator i = jars.iterator(); i.hasNext();) {
      File jar = new File(i.next().toString());
      String version = "2.7.0-SNAPSHOT";
      String name = jar.getName().replaceAll("-" + version + ".jar", "");
      resolve(repos, name, version, expected);
    }
  }

  private void resolveBundles(URL[] repos, Collection jars, boolean expected) throws IOException {
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

  private void resolveBundle(URL[] repos, BundleSpec spec, boolean expected) throws IOException {
    try {
      Resolver resolver = new Resolver(repos);
      URL url = resolver.resolveBundle(spec);
      assertEquals(url.toString().endsWith(".jar"), expected);
      JarFile jar = new JarFile(FileUtils.toFile(url));
      Manifest manifest = jar.getManifest();
      String symbolicName = BundleSpec.getSymbolicName(manifest);
      String version = BundleSpec.getVersion(manifest);
      assertEquals(symbolicName, spec.getSymbolicName());
      assertEquals(version, spec.getVersion());
    } catch (BundleException e) {
      if (PASS == expected) fail(e.getMessage());
      else assertTrue(FAIL == expected);
    }
  }
  
  private void resolve(URL[] repos, String name, String version, boolean expected) {
    try {
      Resolver resolver = new Resolver(repos);
      Module module = Module.Factory.newInstance();
      module.setName(name);
      module.setVersion(version);
      module.setGroupId("org.terracotta.modules");
      URL url = resolver.resolve(module);
      assertEquals(url.getPath().endsWith(name + "-" + version + ".jar"), expected);
    } catch (BundleException e) {
      if (PASS == expected) fail(e.getMessage());
      else assertTrue(FAIL == expected);
    }
  }
}
