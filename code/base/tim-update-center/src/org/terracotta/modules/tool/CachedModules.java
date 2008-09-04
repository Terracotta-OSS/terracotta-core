/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.terracotta.modules.tool.config.ConfigAnnotation;
import org.terracotta.modules.tool.util.DataLoader;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CachedModules implements Modules {

  private List<Module> modules;
  private List<Module> qualifiedModules;
  private List<Module> latestModules;

  private final String tcVersion;
  private final File   repository;

  @Inject
  public CachedModules(@Named(ConfigAnnotation.TERRACOTTA_VERSION) String tcVersion,
                       @Named(ConfigAnnotation.MODULES_DIRECTORY) String repository, DataLoader dataLoader)
      throws JDOMException, IOException {
    this(tcVersion, new File(repository), new FileInputStream(dataLoader.getDataFile()));
  }

  CachedModules(String tcVersion, File repository, InputStream inputStream) throws JDOMException, IOException {
    this.tcVersion = tcVersion;
    this.repository = repository;
    loadData(inputStream);
  }

  private void loadData(InputStream inputStream) throws JDOMException, IOException {
    if (modules != null) return;

    Document document = new SAXBuilder().build(inputStream);
    modules = new ArrayList<Module>();
    List<Element> children = document.getRootElement().getChildren();
    for (Element child : children) {
      Module module = new Module(this, DocumentToAttributes.transform(child));
      modules.add(module);
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

  public Module get(String groupId, String artifactId, String version) {
    Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("groupId", groupId);
    attributes.put("artifactId", artifactId);
    attributes.put("version", version);
    Module module = new Module(null, attributes);
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

  private boolean qualify(Module module) {
    return "*".equals(module.tcVersion()) || tcVersion().equals(module.tcVersion());
  }

  public List<Module> list() {
    if (qualifiedModules != null) return qualifiedModules;

    List<Module> list = new ArrayList<Module>();
    for (Module module : modules) {
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
    return Collections.unmodifiableList(modules);
  }

  public File repository() {
    return repository;
  }

  public String tcVersion() {
    return tcVersion;
  }

}
