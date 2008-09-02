/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.thread.BoundedThreadPool;
import org.terracotta.modules.tool.DocumentToAttributes.DependencyType;
import org.terracotta.modules.tool.util.ChecksumUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public final class ModuleTest extends TestCase {

  public void testInstall() throws Exception {
    File basedir = new File(System.getProperty("java.io.tmpdir"), "repo");
    int port = 8888;

    String datafile = "/testData03.xml";
    String tcVersion = "0.0.0";

    initRepository(basedir, datafile);
    Modules modules = loadModules(datafile, tcVersion);

    startRepository(port, basedir);

    Module module = modules.get("foo.bar", "baz", "0.0.0");
    assertNotNull(module);
    List<String> installedList = new ArrayList<String>();
    module.install(new Listener(installedList));
    assertTrue(module.isInstalled());
    assertEquals(1, installedList.size());
    assertTrue(installedList.contains(createModule("foo.bar", "baz", "0.0.0").toString()));

    module = modules.get("foo.bar", "baz", "0.0.1");
    assertNotNull(module);
    installedList = new ArrayList<String>();
    module.install(new Listener(installedList));
    assertTrue(module.isInstalled());
    assertEquals(4, installedList.size());
    assertTrue(installedList.contains(createModule("foo.bar", "baz", "0.0.1").toString()));
    assertTrue(installedList.contains(createModule("foo.bar", "quux", "0.0.0").toString()));
    assertTrue(installedList.contains(createModule("foo.zoo", "tuus", "0.0.0").toString()));
    assertTrue(installedList.contains(createModule("foo.bar", "zoo", "0.0.0").toString()));

    module = modules.get("foo.bar", "quux", "0.0.0");
    assertNotNull(module);
    installedList = new ArrayList<String>();
    module.install(new Listener(installedList));
    assertTrue(module.isInstalled());
    assertEquals(3, installedList.size());
    assertTrue(installedList.contains(createModule("foo.bar", "quux", "0.0.0").toString()));
    assertTrue(installedList.contains(createModule("foo.zoo", "tuus", "0.0.0").toString()));
    assertTrue(installedList.contains(createModule("foo.bar", "zoo", "0.0.0").toString()));

    tcVersion = "0.0.1";
    modules = loadModules(datafile, tcVersion);

    module = modules.get("foo.bar", "baz", "0.0.2");
    assertNotNull(module);
    installedList = new ArrayList<String>();
    module.install(new Listener(installedList));
    assertTrue(module.isInstalled());
    assertEquals(1, installedList.size());
    assertTrue(installedList.contains(createModule("foo.bar", "baz", "0.0.2").toString()));
  }

  public void testIsInstalled() throws IOException {
    String tcVersion = "0.0.2";
    Modules modules = loadModules("/testData02.xml", tcVersion);
    Module module = modules.get("foo.bar", "zoo", "0.0.0");
    assertNotNull(module);

    FileUtils.deleteDirectory(modules.repository());
    File directory = new File(modules.repository(), module.installPath().toString());
    FileUtils.forceMkdir(directory);
    File file = new File(directory, module.filename());
    FileUtils.touch(file);
    assertTrue(file.exists());
    assertTrue(module.isInstalled());

    FileUtils.deleteDirectory(modules.repository());
    FileUtils.forceMkdir(modules.repository());
    file = new File(modules.repository(), module.filename());
    FileUtils.touch(file);
    assertTrue(file.exists());
    assertTrue(module.isInstalled());

    FileUtils.deleteDirectory(modules.repository());
    FileUtils.forceMkdir(modules.repository());
    assertFalse(file.exists());
    assertFalse(module.isInstalled());
  }

  public void testManifest() {
    Modules modules;
    String tcVersion = "0.0.0";
    modules = loadModules("/testData02.xml", tcVersion);

    List<Module> list = modules.list();
    assertFalse(list.isEmpty());

    String version = "0.0.0";
    Module module = modules.get("foo.bar", "baz", version);
    assertNotNull(module);
    List<AbstractModule> manifest = module.manifest();
    assertFalse(manifest.isEmpty());
    assertEquals(1, manifest.size());

    version = "0.0.1";
    module = modules.get("foo.bar", "baz", version);
    assertNotNull(module);
    manifest = module.manifest();
    assertFalse(manifest.isEmpty());
    assertEquals(4, manifest.size());

    List<String> entries = new ArrayList<String>();
    for (AbstractModule entry : manifest) {
      entries.add(entry.groupId() + ":" + entry.artifactId() + ":" + entry.version());
    }
    assertTrue(entries.contains("foo.bar:baz:0.0.1"));
    assertTrue(entries.contains("foo.bar:baz:0.0.3"));
    assertTrue(entries.contains("foo.bar:baz:0.0.5"));
    assertTrue(entries.contains("foo.bar:baz:0.0.7"));
  }

  public void testBadManifest() {
    Modules modules;
    String tcVersion = "0.0.0";
    modules = loadModules("/testData02.xml", tcVersion);

    List<Module> list = modules.list();
    assertFalse(list.isEmpty());

    Module module = modules.get("foo.bar", "baz", "0.0.4");
    assertNotNull(module);
    try {
      module.manifest();
      fail("Should have thrown an IllegalStateException when computing the manifest for an entry with a bad reference.");
    } catch (IllegalStateException e) {
      //
    }
  }

  public void testVersions() {
    Modules modules;
    String tcVersion = "0.0.0";

    modules = loadModules("/testData02.xml", tcVersion);
    List<Module> list = modules.list();
    assertFalse(list.isEmpty());

    String version = "0.0.0";
    Module module = modules.get("foo.bar", "baz", version);
    assertNotNull(module);

    List<String> versions = module.versions();
    assertFalse(versions.isEmpty());
    assertEquals(4, versions.size());
    assertFalse(versions.contains(version));

    assertEquals("0.0.1", versions.get(0));
    assertEquals("0.0.3", versions.get(1));
    assertEquals("0.0.4", versions.get(2));

    tcVersion = "0.0.1";
    modules = loadModules("/testData02.xml", tcVersion);
    list = modules.list();
    assertFalse(list.isEmpty());

    module = modules.get("foo.bar", "baz", "0.0.6");
    assertNotNull(module);
    assertTrue(module.versions().isEmpty());
  }

  public void testIsLatest() {
    Modules modules;
    String tcVersion = "0.0.0";
    modules = loadModules("/testData02.xml", tcVersion);

    List<Module> list = modules.list();
    assertFalse(list.isEmpty());

    Module module = modules.get("foo.bar", "baz", "0.0.0");
    assertNotNull(module);
    assertFalse(module.isLatest());

    module = modules.get("foo.bar", "baz", "0.0.7");
    assertNotNull(module);
    assertTrue(module.isLatest());

    module = modules.get("foo.bar", "quux", "0.0.3");
    assertNotNull(module);
    assertFalse(module.isLatest());

    module = modules.get("foo.bar", "quux", "0.0.5");
    assertNotNull(module);
    assertTrue(module.isLatest());

    tcVersion = "0.0.1";
    modules = loadModules("/testData02.xml", tcVersion);
    list = modules.list();
    assertFalse(list.isEmpty());

    module = modules.get("foo.bar", "baz", "0.0.6");
    assertNotNull(module);
    assertTrue(module.isLatest());
  }

  public void testSiblings() {
    Modules modules;
    String tcVersion = "0.0.0";

    modules = loadModules("/testData02.xml", tcVersion);
    List<Module> list = modules.list();
    assertFalse(list.isEmpty());

    Module module = modules.get("foo.bar", "baz", "0.0.0");
    assertNotNull(module);

    List<Module> siblings = module.siblings();
    assertFalse(siblings.isEmpty());
    assertEquals(4, siblings.size());
    assertFalse(siblings.contains(module));

    assertEquals(createModule("foo.bar", "baz", "0.0.1"), siblings.get(0));
    assertEquals(createModule("foo.bar", "baz", "0.0.3"), siblings.get(1));
    assertEquals(createModule("foo.bar", "baz", "0.0.4"), siblings.get(2));
  }

  public void testLoadFromDocument() throws Exception {
    InputStream data = null;
    try {
      data = getClass().getResourceAsStream("/testData01.xml");
      Document document = new SAXBuilder().build(data);
      List<Element> modules = document.getRootElement().getChildren();
      assertEquals(2, modules.size());

      // tim-ehache-1.3
      Module module = new Module(null, modules.get(0));
      assertEquals("tim-ehcache-1.3", module.artifactId());
      assertEquals("1.0.2", module.version());
      assertEquals("org.terracotta.modules", module.groupId());
      assertEquals("2.5.2", module.tcVersion());
      assertEquals("http://forge-dev.terracotta.lan/releases/projects/tim-ehcache/", module.website().toString());
      assertEquals("Terracotta, Inc.", module.vendor());
      assertEquals("Copyright (c) 2007 Terracotta, Inc.", module.copyright());
      assertEquals("Terracotta Integration Module", module.category());
      assertEquals("Terracotta Integration Module for clustering Ehcache", module.description());
      assertEquals(
                   "http://forge-dev.terracotta.lan/repo/org/terracotta/modules/tim-ehcache-1.3/1.0.2/tim-ehcache-1.3-1.0.2.jar",
                   module.repoUrl().toString());
      assertEquals("org/terracotta/modules/tim-ehcache-1.3/1.0.2", module.installPath().toString());
      assertEquals("tim-ehcache-1.3-1.0.2.jar", module.filename());

      assertFalse(module.docUrl().toURI().equals(module.website().toURI()));
      assertEquals("http://terracotta.org/web/display/orgsite/EHCache+Integration", module.docUrl().toString());

      List<AbstractModule> dependencies = module.dependencies();
      assertEquals(2, dependencies.size());
      assertTrue(dependencies.get(0) instanceof BasicModule);
      assertTrue(dependencies.get(1) instanceof Reference);

      BasicModule dependency = (BasicModule) dependencies.get(0);
      assertEquals(module, dependency.owner());
      assertEquals("modules-common", dependency.artifactId());
      assertEquals("2.5.2", dependency.version());
      assertEquals("org.terracotta.modules", dependency.groupId());
      assertEquals(
                   "http://forge-dev.terracotta.lan/repo/org/terracotta/modules/modules-common/2.5.2/modules-common-2.5.2.jar",
                   dependency.repoUrl().toString());
      assertEquals("org/terracotta/modules/modules-common/2.5.2", dependency.installPath().toString());
      assertEquals("modules-common-2.5.2.jar", dependency.filename());

      Reference reference = (Reference) dependencies.get(1);
      assertEquals(module, reference.owner());
      assertEquals("tim-ehcache-commons", reference.artifactId());
      assertEquals("1.0.2", reference.version());
      assertEquals("org.terracotta.modules", reference.groupId());

      // tim-ehache-commons
      module = new Module(null, modules.get(1));
      assertEquals("tim-ehcache-commons", module.artifactId());
      assertEquals("1.0.2", module.version());
      assertEquals("org.terracotta.modules", module.groupId());
      assertEquals("2.5.2", module.tcVersion());
      assertEquals("http://forge-dev.terracotta.lan/releases/projects/tim-ehcache/", module.website().toString());
      assertEquals("Terracotta, Inc.", module.vendor());
      assertEquals("Copyright (c) 2007 Terracotta, Inc.", module.copyright());
      assertEquals("Terracotta Integration Module", module.category());
      assertEquals("Terracotta Integration Module for clustering Ehcache", module.description());
      assertEquals(
                   "http://forge-dev.terracotta.lan/repo/org/terracotta/modules/tim-ehcache-commons/1.0.2/tim-ehcache-commons-1.0.2.jar",
                   module.repoUrl().toString());
      assertEquals("org/terracotta/modules/tim-ehcache-commons/1.0.2", module.installPath().toString());
      assertEquals("tim-ehcache-commons-1.0.2.jar", module.filename());
      assertEquals(module.docUrl(), module.website());

      dependencies = module.dependencies();
      assertEquals(1, dependencies.size());
      assertTrue(dependencies.get(0) instanceof BasicModule);

      dependency = (BasicModule) dependencies.get(0);
      assertEquals(module, dependency.owner());
      assertEquals("modules-common", dependency.artifactId());
      assertEquals("2.5.2", dependency.version());
      assertEquals("org.terracotta.modules", dependency.groupId());
      assertEquals(
                   "http://forge-dev.terracotta.lan/repo/org/terracotta/modules/modules-common/2.5.2/modules-common-2.5.2.jar",
                   dependency.repoUrl().toString());
      assertEquals("org/terracotta/modules/modules-common/2.5.2", dependency.installPath().toString());
      assertEquals("modules-common-2.5.2.jar", dependency.filename());
    } finally {
      IOUtils.closeQuietly(data);
    }
  }

  public void testOptionalValues() {
    Module module;
    module = createModule("foo.bar", "baz", "0.0.0");
    assertEquals(StringUtils.EMPTY, module.category());
    assertEquals(StringUtils.EMPTY, module.contactAddress());
    assertEquals(StringUtils.EMPTY, module.copyright());
    assertEquals(StringUtils.EMPTY, module.description());
    assertEquals(StringUtils.EMPTY, module.tcProjectStatus());
    assertEquals(StringUtils.EMPTY, module.vendor());
    assertEquals(module.website(), module.docUrl());
  }

  public void testInvalidConfiguration() {
    Module module;

    try {
      module = createModule("foo.bar", "baz", "0.0.0");
      // required attributes
      module.groupId();
      module.artifactId();
      module.version();

      module.tcVersion();

      module.filename();
      module.installPath();
      module.repoUrl();

      // optional attributes
      module.category();
      module.contactAddress();
      module.copyright();

      module.description();
      module.docUrl();

      module.tcProjectStatus();
      module.vendor();

      module.website();

      List<AbstractModule> dependencies = module.dependencies();

      assertEquals(2, dependencies.size());
      assertTrue(dependencies.get(0) instanceof BasicModule);
      assertTrue(dependencies.get(1) instanceof Reference);
    } catch (NullPointerException e) {
      e.printStackTrace();
      fail("Should not have thrown an NPE, all of the attributes values are valid");
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
      module = createModule("foo.bar", "baz", "0.0.0", "tc-version");
      module.tcVersion();
      fail("Should've thrown an NPE when tc-version is null");
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

    module = createModule("foo.bar", "baz", "0.0.0", "dependencies");
    List<AbstractModule> dependencies = module.dependencies();
    assertNotNull(dependencies);
    assertTrue(dependencies.isEmpty());
  }

  private Module createModule(String groupId, String artifactId, String version, String... excludes) {
    Map<String, Object> attributes = new HashMap<String, Object>();
    String filename = artifactId + "-" + version + ".jar";
    String installPath = groupId.replace('.', File.separatorChar) + File.separatorChar + artifactId
                         + File.separatorChar + version;
    attributes.put("groupId", groupId);
    attributes.put("artifactId", artifactId);
    attributes.put("version", version);
    attributes.put("tc-version", "0.0.0");
    attributes.put("filename", filename);
    attributes.put("installPath", installPath);
    attributes.put("repoURL", "http://127.0.0.1/" + installPath.replace(File.separatorChar, '/'));

    Map<String, Object> dependency = new HashMap<String, Object>();
    groupId = "dependency." + groupId;
    installPath = groupId.replace('.', File.separatorChar) + File.separatorChar + artifactId + File.separatorChar
                  + version;
    dependency.put("groupId", groupId);
    dependency.put("artifactId", artifactId);
    dependency.put("version", version);
    dependency.put("filename", filename);
    dependency.put("installPath", installPath);
    dependency.put("repoURL", "http://127.0.0.1/" + installPath.replace(File.separatorChar, '/'));
    dependency.put("_dependencyType", DependencyType.INSTANCE);

    Map<String, Object> reference = new HashMap<String, Object>();
    groupId = "reference." + groupId;
    reference.put("groupId", groupId);
    reference.put("artifactId", artifactId);
    reference.put("version", version);
    reference.put("_dependencyType", DependencyType.REFERENCE);

    List<Map<String, Object>> dependencies = new ArrayList<Map<String, Object>>();
    dependencies.add(dependency);
    dependencies.add(reference);
    attributes.put("dependencies", dependencies);

    for (String key : Arrays.asList(excludes)) {
      attributes.remove(key);
    }
    return new Module(null, attributes);
  }

  private Modules loadModules(String testData, String tcVersion) {
    File tmpdir = new File(System.getProperty("java.io.tmpdir"));
    File repodir = new File(tmpdir, "modules");
    try {
      return new CachedModules(tcVersion, repodir, getClass().getResourceAsStream(testData));
    } catch (Exception e) {
      fail("Unable to load test data: " + testData);
    }
    return null;
  }

  private static void touch(File basedir, BasicAttributes module) throws Exception {
    File path = new File(basedir, module.installPath().toString());
    FileUtils.forceMkdir(path);
    File jarfile = new File(path, module.filename());
    FileUtils.writeStringToFile(jarfile, module.toString(), null);
    BigInteger md5sum = ChecksumUtil.md5Sum(jarfile);
    File md5file = new File(path, module.filename() + ".md5");
    FileUtils.writeStringToFile(md5file, md5sum.toString(16), null);
    FileUtils.touch(md5file);
  }

  private void initRepository(File basedir, String testData) throws Exception {
    if (basedir.exists()) {
      FileUtils.forceDelete(basedir);
      FileUtils.forceMkdir(basedir);
    }

    Modules modules = loadModules(testData, "");
    for (Module module : modules.listAll()) {
      touch(basedir, module);
      for (AbstractModule dependency : module.dependencies()) {
        if (dependency instanceof BasicModule) touch(basedir, (BasicAttributes) dependency);
      }
    }
  }

  private void startRepository(final int port, final File basedir) throws Exception {
    Thread thread = new Thread(new Runnable() {
      public void run() {
        try {
          new FileServer(port, basedir);
        } catch (Exception e) {
          // XXX need a better way to handle this ---
        }
      }
    });

    thread.setPriority(Thread.MAX_PRIORITY);
    thread.setName(getClass().getName());
    thread.setDaemon(true);
    thread.start();

    Thread.sleep(500);
  }

  private static class FileServer {
    public FileServer(int port, File basedir) throws Exception {
      Server server = new Server();

      BoundedThreadPool threadPool = new BoundedThreadPool();
      threadPool.setMaxThreads(100);
      server.setThreadPool(threadPool);
      Connector connector = new SelectChannelConnector();

      connector.setPort(port);
      connector.setMaxIdleTime(30000);
      server.setConnectors(new Connector[] { connector });

      ResourceHandler resource_handler = new ResourceHandler();
      resource_handler.setResourceBase(basedir.toString());
      HandlerList handlers = new HandlerList();
      handlers.setHandlers(new Handler[] { resource_handler, new DefaultHandler() });

      server.setHandler(handlers);
      server.start();
      server.join();
    }
  }

  private static class Listener implements InstallListener {
    private final List<String> installedList;

    public Listener(List<String> installedList) {
      this.installedList = installedList;
    }

    public void notify(Object source, InstallNotification type, String message) {
      if (!InstallNotification.INSTALLED.equals(type) && !InstallNotification.SKIPPED.equals(type)) return;
      installedList.add(source.toString());
    }
  }

}
