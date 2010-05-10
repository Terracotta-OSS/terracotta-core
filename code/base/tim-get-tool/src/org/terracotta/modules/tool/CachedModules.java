/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.terracotta.modules.tool.commands.KitTypes;
import org.terracotta.modules.tool.commands.ManifestAttributes;
import org.terracotta.modules.tool.config.Config;
import org.terracotta.modules.tool.config.ConfigAnnotation;
import org.terracotta.modules.tool.config.InvalidConfigurationException;
import org.terracotta.modules.tool.exception.RemoteIndexIOException;
import org.terracotta.modules.tool.util.ChecksumUtil;
import org.terracotta.modules.tool.util.DataLoader;
import org.terracotta.modules.tool.util.DownloadUtil;
import org.terracotta.modules.tool.util.DownloadUtil.DownloadOption;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.tc.bundles.OSGiToMaven;
import com.tc.util.version.VersionMatcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class CachedModules implements Modules {
  static final String        FORMAT_VERSION = "2";

  private List<Module>       modules;
  private List<Module>       qualifiedModules;
  private List<Module>       latestModules;

  @Inject
  @Named(ConfigAnnotation.TERRACOTTA_VERSION)
  private String             tcVersion;

  @Inject
  @Named(ConfigAnnotation.TIM_API_VERSION)
  private String             timApiVersion;

  @Inject
  @Named(ConfigAnnotation.CONFIG_INSTANCE)
  private Config             config;

  @Inject
  @Named(ConfigAnnotation.INCLUDE_SNAPSHOTS)
  private boolean            includeSnapshots;

  private final File         repository;
  private final DataLoader   dataLoader;
  private String             timeStamp;

  @Inject
  @Named(ConfigAnnotation.DOWNLOADUTIL_INSTANCE)
  private final DownloadUtil downloader;

  @Inject
  public CachedModules(@Named(ConfigAnnotation.MODULES_DIRECTORY) String repository, DataLoader dataLoader) {
    this.repository = new File(repository);
    this.dataLoader = dataLoader;
    this.downloader = new DownloadUtil();
  }

  public File download(Installable module, boolean verify, boolean inspect) throws IOException {
    File srcfile = File.createTempFile("tim.", null);
    srcfile.deleteOnExit();
    download(module.repoUrl(), srcfile);

    if (verify) {
      File md5file = File.createTempFile("tim.md5.", null);
      URL md5url = new URL(module.repoUrl().toExternalForm() + ".md5");
      md5file.deleteOnExit();
      download(md5url, md5file);

      if (!downloadVerified(srcfile, md5file)) {
        String msg = "The file might be corrupt, the expected hash value does not jive.";
        throw new IOException(msg);
      }
    }

    if (inspect && !attributesVerified(srcfile, (AbstractModule) module)) {
      String msg = "The file might be corrupt - the name and/or version retrieved does not match what was requested.";
      throw new IOException(msg);
    }
    return srcfile;
  }

  private static boolean attributesVerified(File srcfile, AbstractModule module) throws IOException {
    String moduleName = null;
    String moduleVersion = null;

    Manifest mf = (new JarFile(srcfile)).getManifest();
    Attributes attrs = mf.getMainAttributes();
    moduleName = attrs.getValue(ManifestAttributes.OSGI_SYMBOLIC_NAME.attribute());
    String osgiVersion = attrs.getValue(ManifestAttributes.OSGI_VERSION.attribute());

    if (moduleName == null || osgiVersion == null) {
      // Try to use Terracotta-ArtifactCoordinates instead
      String coords = attrs.getValue(ManifestAttributes.TERRACOTTA_COORDINATES.attribute());
      if (coords == null || coords.length() == 0) { return false; }

      String[] parts = coords.split(":");
      if (parts.length != 3) { return false; }
      moduleName = parts[0] + "." + parts[1];
      moduleVersion = parts[2];
    } else {
      // Normal bundle
      moduleVersion = OSGiToMaven.bundleVersionToProjectVersion(osgiVersion);
    }

    return moduleName.equals(module.symbolicName()) && moduleVersion.equals(module.version());
  }

  private void download(URL url, File dest) throws IOException {
    downloader.download(url, dest, DownloadOption.CREATE_INTERVENING_DIRECTORIES, DownloadOption.OVERWRITE_EXISTING);
  }

  private static boolean downloadVerified(File srcfile, File md5file) {
    try {
      return ChecksumUtil.verifyMD5Sum(srcfile, md5file);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * XXX: This constructor is used for tests only
   */
  CachedModules(Config config, File repository, InputStream inputStream) throws ParserConfigurationException,
      SAXException, IOException {
    this.config = config;
    this.tcVersion = config.getTcVersion();
    this.timApiVersion = config.getTimApiVersion();
    this.includeSnapshots = config.getIncludeSnapshots();
    this.repository = repository;
    this.dataLoader = null;
    this.downloader = new DownloadUtil();
    loadData(inputStream);
  }

  private void loadData() {
    if ((modules == null) || modules.isEmpty()) {
      InputStream dataStream = null;
      try {
        dataStream = dataLoader.openDataStream();

        try {
          loadData(dataStream);
        } catch (Exception e) {
          throw new RuntimeException("Error parsing index file: " + e.getMessage(), e);
        }
      } catch (FileNotFoundException e) {
        throw new RemoteIndexIOException("Remote index file not found: " + e.getMessage(), e, dataLoader
            .getLocalDataFile(), dataLoader.getRemoteDataUrl());
      } catch (IOException e) {
        throw new RemoteIndexIOException("Error reading remote index file: " + e.getMessage(), e, dataLoader
            .getLocalDataFile(), dataLoader.getRemoteDataUrl());
      } finally {
        IOUtils.closeQuietly(dataStream);
      }
    }
  }

  private void loadData(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
    if (modules != null) return;

    Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
    validateFormatVersion(document.getDocumentElement().getAttribute("format-version"));
    timeStamp = document.getDocumentElement().getAttribute("timestamp");
    modules = new ArrayList<Module>();
    NodeList children = document.getDocumentElement().getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element child = (Element) node;
        Module module = new Module(this, DocumentToAttributes.transform(child), relativeUrlBase());
        modules.add(module);
      }
    }
  }

  private void validateFormatVersion(String formatVersion) {
    if (!FORMAT_VERSION.equals(formatVersion)) {
      String message = "Format version '" + formatVersion + "' does not match expected format version '"
                       + FORMAT_VERSION + "'";
      throw new InvalidConfigurationException(message);
    }
  }

  // TODO NEED TEST
  public List<Module> find(List<String> argList) {
    List<Module> list = new ArrayList<Module>();
    List<String> args = new ArrayList<String>(argList);

    String artifactId = args.remove(0);
    String version = args.isEmpty() ? null : args.remove(0);
    String groupId = args.isEmpty() ? null : args.remove(0);

    for (Module module : list()) {
      // boolean m0 = (artifactId == null) ? true : module.artifactId().equals(artifactId);
      boolean m0 = module.artifactId().equals(artifactId);
      boolean m1 = (version == null) ? true : module.version().equals(version);
      boolean m2 = (groupId == null) ? true : module.groupId().equals(groupId);
      if (!m0 || !m1 || !m2) continue;
      list.add(module);
    }

    Collections.sort(list);
    return list;
  }

  public Module findLatest(String artifactId, String groupId) {
    for (Module module : listLatest()) {
      boolean foundArtifactId = module.artifactId().equals(artifactId);
      boolean foundGroupId = (groupId == null) ? true : module.groupId().equals(groupId);
      if (!foundArtifactId || !foundGroupId) continue;
      return module;
    }
    return null;
  }

  public Module get(String groupId, String artifactId, String version) {
    Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("groupId", groupId);
    attributes.put("artifactId", artifactId);
    attributes.put("version", version);
    Module module = new Module(null, attributes, relativeUrlBase());
    int index = list().indexOf(module);
    return (index == -1) ? null : list().get(index);
  }

  public List<Module> getSiblings(Module module) {
    List<Module> list = new ArrayList<Module>();
    if (module == null) return list;

    for (Module other : list()) {
      if (module.isSibling(other)) list.add(other);
    }
    Collections.sort(list);
    return list;
  }

  public List<Module> getSiblings(String symbolicName) {
    List<Module> list = new ArrayList<Module>();
    if (StringUtils.isEmpty(symbolicName)) return list;

    for (Module module : list()) {
      if (module.symbolicName().equals(symbolicName)) list.add(module);
    }
    Collections.sort(list);
    return list;
  }

  boolean qualify(Module module) {
    return isKitEditionMatch(module)
           && new VersionMatcher(tcVersion, timApiVersion).matches(module.tcVersion(), module.timApiVersion());
  }

  boolean isKitEditionMatch(Module module) {
    if (module.kit().equals(KitTypes.ALL.type())) {
      return true;
    } else if (module.kit().equals(KitTypes.ENTERPRISE.type())) {
      return config.isEnterpriseKit();
    } else if (module.kit().equals(KitTypes.OPEN_SOURCE.type())) {
      return config.isOpenSourceKit();
    } else {
      throw new IllegalArgumentException("Unknown <tc-kit> value for module " + module.groupId + ":"
                                         + module.artifactId + ":" + module.version() + ": " + module.kit());
    }
  }

  public List<Module> list() {
    if (qualifiedModules != null) return qualifiedModules;
    loadData();

    List<Module> list = new ArrayList<Module>();
    for (Module module : modules) {
      if (!includeSnapshots && module.version().endsWith("-SNAPSHOT")) continue;
      if (qualify(module)) list.add(module);
    }
    Collections.sort(list);
    qualifiedModules = Collections.unmodifiableList(list);
    return qualifiedModules;
  }

  public List<Module> listLatest() {
    if (latestModules != null) return latestModules;

    Map<String, Module> group = new HashMap<String, Module>();
    for (Module module : list()) {
      Module other = group.get(module.symbolicName());
      if (other == null) {
        group.put(module.symbolicName(), module);
        continue;
      }
      if (module.isOlder(other)) continue;
      group.put(module.symbolicName(), module);
    }

    List<Module> list = new ArrayList<Module>(group.values());
    Collections.sort(list);
    latestModules = Collections.unmodifiableList(list);
    return latestModules;
  }

  public List<Module> listAll() {
    loadData();
    return Collections.unmodifiableList(modules);
  }

  public File repository() {
    return repository;
  }

  public String tcVersion() {
    return tcVersion;
  }

  public String timApiVersion() {
    return timApiVersion;
  }

  public String indexTimeStamp() {
    return timeStamp;
  }

  public URI relativeUrlBase() {
    return config.getRelativeUrlBase();
  }
}
