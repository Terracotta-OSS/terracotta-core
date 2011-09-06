/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.terracotta.modules.tool.DocumentToAttributes.DependencyType;
import org.terracotta.modules.tool.InstallListener.InstallNotification;
import org.terracotta.modules.tool.commands.ActionLog;
import org.terracotta.modules.tool.commands.KitTypes;
import org.terracotta.modules.tool.exception.UnsatisfiedDependencyException;
import org.w3c.dom.Element;

import com.google.inject.Inject;
import com.tc.util.version.VersionMatcher;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class Module extends AttributesModule implements Installable {

  private final Modules modules;

  public Module(Modules modules, Element module, URI relativeUrlBase, int toolkitRange) {
    this(modules, DocumentToAttributes.transform(module), relativeUrlBase);
  }

  @Inject
  Module(Modules modules, Map<String, Object> attributes, URI relativeUrlBase) {
    super(attributes, relativeUrlBase);
    this.modules = modules;
  }

  protected Modules owner() {
    return modules;
  }

  public String category() {
    return getAttributesHelper().getAttrValueAsString("category", StringUtils.EMPTY);
  }

  public String contactAddress() {
    return getAttributesHelper().getAttrValueAsString("contactAddress", StringUtils.EMPTY);
  }

  public String copyright() {
    return getAttributesHelper().getAttrValueAsString("copyright", StringUtils.EMPTY);
  }

  public List<AbstractModule> dependencies() {
    List<AbstractModule> list = new ArrayList<AbstractModule>();
    if (getAttributes().containsKey("dependencies")) {
      List<Map<String, Object>> dependencies = (List<Map<String, Object>>) getAttributes().get("dependencies");
      for (Map<String, Object> dependencyAttributes : dependencies) {
        DependencyType type = (DependencyType) dependencyAttributes.get("_dependencyType");
        if (DependencyType.INSTANCE.equals(type)) {
          list.add(new BasicModule(this, dependencyAttributes, getRelativeURlBase()));
          continue;
        } else if (DependencyType.REFERENCE.equals(type)) {
          list.add(new Reference(this, dependencyAttributes));
          continue;
        }
        // XXX dependencyType eval'd as DependencyType.UNKNOWN
        // it means bad data if we should ever get here - we should just have schema to make
        // sure that the data being read is valid - instead of trying to catch a bad state here
        throw new IllegalStateException("Unknown dependency type in " + this);
      }
    }

    return list;
  }

  /**
   * Descriptive text about the module.
   * 
   * @return A String
   */
  public String description() {
    return getAttributesHelper().getAttrValueAsString("description", StringUtils.EMPTY);
  }

  /**
   * Check what kits this module is applicable for
   * 
   * @see org.terracotta.modules.tool.commands.KitTypes
   */
  public String kit() {
    return getAttributesHelper().getAttrValueAsString("tc-kit", KitTypes.ALL.type());
  }

  /**
   * The URL pointing to the documentation for this module
   * 
   * @return An URL. Will return this module's website URL if none was defined.
   */
  public URL docUrl() {
    return getAttributesHelper().getAttrValueAsUrl("docURL", website());
  }

  public boolean isInstalled() {
    return isInstalled(modules.repository());
  }

  /**
   * Retrieve the latest version available for this module. If this module has no siblings, then it returns itself.
   */
  public Module latest() {
    List<Module> siblings = siblings();
    return siblings.isEmpty() ? this : siblings.get(siblings.size() - 1);
  }

  public boolean isLatest() {
    List<Module> siblings = siblings();
    if (siblings.isEmpty()) return true;

    Module youngest = siblings.get(siblings.size() - 1);
    return youngest.isOlder(this);
  }

  /**
   * Retrieve the siblings of this module. The list returned is sorted in ascending-order, ie: oldest version first. The
   * listed returned DOES NOT include the module itself.
   * 
   * @return a List of Module.
   */
  public List<Module> siblings() {
    return modules.getSiblings(this);
  }

  public String tcProjectStatus() {
    return getAttributesHelper().getAttrValueAsString("tc-projectStatus", StringUtils.EMPTY);
  }

  public String tcVersion() {
    return getAttributesHelper().getAttrValueAsString("tc-version", null);
  }

  public String timApiVersion() {
    if (getAttributes().containsKey("tim-api-version")) {
      return getAttributesHelper().getAttrValueAsString("tim-api-version", null);
    } else {
      return VersionMatcher.ANY_VERSION;
    }
  }

  public String vendor() {
    return getAttributesHelper().getAttrValueAsString("vendor", StringUtils.EMPTY);
  }

  public boolean tcInternalTIM() {
    return Boolean.valueOf(getAttributesHelper().getAttrValueAsString("tc-internalTIM", "false"));
  }

  /**
   * Retrieve the list of versions of this module. The list returned is sorted in ascending-order, ie: oldest version
   * first. The list returned DOES NOT include the version of this module.
   */
  public List<String> versions() {
    List<String> list = new ArrayList<String>();
    for (Module module : siblings()) {
      list.add(module.version());
    }
    return list;
  }

  /**
   * The website URL of this module.
   * 
   * @return An URL. Will return a the URL point to the TC Forge if none was defined.
   */
  public URL website() {
    URL alturl = null;
    try {
      alturl = new URL("http://forge.terracotta.org/");
    } catch (MalformedURLException e) {
      //
    }
    return getAttributesHelper().getAttrValueAsUrl("website", alturl);
  }

  /**
   * Currently we are determining this based on whether the tc-downloadPath exists. This value is usually set to install
   * a download outside the modules directory.
   */
  public boolean installsAsModule() {
    String downloadPath = getAttributesHelper().getAttrValueAsString("tc-downloadPath", null);
    return downloadPath == null || downloadPath.length() == 0;
  }

  protected List<AbstractModule> manifest() throws UnsatisfiedDependencyException {
    List<AbstractModule> manifest = new ArrayList<AbstractModule>();
    // manifest, at a minimum, includes this module
    manifest.add(this);
    for (AbstractModule dependency : dependencies()) {
      if (dependency instanceof Reference) {
        List<Module> possibles = modules.getMatchingQualified(dependency.groupId(), dependency.artifactId(),
                                                              dependency.version());

        if (possibles.isEmpty()) { throw new UnsatisfiedDependencyException("No qualified listing found",
                                                                            (Reference) dependency); }

        List<AbstractModule> depManifest = null;
        for (ListIterator<Module> it = possibles.listIterator(possibles.size()); it.hasPrevious();) {
          Module module = it.previous();
          try {
            depManifest = module.manifest();
            break;
          } catch (IllegalStateException e) {
            // ignore - try next possible
          }
        }

        if (depManifest == null) { throw new UnsatisfiedDependencyException("No installable listing found",
                                                                            (Reference) dependency); }

        for (AbstractModule entry : depManifest) {
          if (!manifest.contains(entry)) manifest.add(entry);
        }
      } else {
        manifest.add(dependency);
      }
    }
    return manifest;
  }

  private void notifyListener(InstallListener listener, AbstractModule source, InstallNotification type, String message) {
    if (listener == null) return;
    listener.notify(source, type, message);
  }

  /**
   * Install this module.
   */
  public void install(InstallListener listener, ActionLog actionLog, InstallOption... options) {
    install(listener, actionLog, Arrays.asList(options));
  }

  public void install(InstallListener listener, ActionLog actionLog, Collection<InstallOption> options) {
    InstallOptionsHelper installOptions = new InstallOptionsHelper(options);
    List<AbstractModule> manifest = null;

    notifyListener(listener, this, InstallNotification.STARTING, StringUtils.EMPTY);
    try {
      manifest = manifest();
    } catch (UnsatisfiedDependencyException e) {
      String message = "Unable to compute manifest for installation: " + e.getMessage();
      handleInstallFailure(e, installOptions, listener, this, InstallNotification.ABORTED, message);
      return;
    }

    for (AbstractModule entry : manifest) {
      Installable module = (Installable) entry;
      File destFile = module.installLocationInRepository(modules.repository());
      if (module.isInstalled(modules.repository()) && !installOptions.overwrite()) {
        notifyListener(listener, entry, InstallNotification.SKIPPED, "Already installed");
        continue;
      }

      if (!installOptions.pretend()) {
        File srcfile = null;

        try {
          srcfile = modules.download(module, installOptions.verify(), installOptions.inspect());
        } catch (IOException e) {
          String message = "Attempt to download TIM file at " + module.repoUrl() + " failed - " + e.getMessage();
          handleInstallFailure(e, installOptions, listener, entry, InstallNotification.DOWNLOAD_FAILED, message);
          continue;
        }

        try {
          FileUtils.forceMkdir(destFile.getParentFile());
          FileUtils.copyFile(srcfile, destFile);
        } catch (IOException e) {
          String message = destFile + " (" + e.getMessage() + ")";
          handleInstallFailure(e, installOptions, listener, entry, InstallNotification.INSTALL_FAILED, message);
          continue;
        }
      }

      actionLog.addInstallAction(groupId, artifactId, version, destFile.getAbsolutePath());
      notifyListener(listener, entry, InstallNotification.INSTALLED, "Ok");
    }
  }

  private void handleInstallFailure(Throwable e, InstallOptionsHelper installOptions, InstallListener listener,
                                    AbstractModule source, InstallNotification type, String message) {
    if (installOptions.failFast()) {
      throw new RuntimeException(e);
    } else {
      notifyListener(listener, source, type, message);
    }
  }
}
