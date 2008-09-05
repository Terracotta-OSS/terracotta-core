/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.terracotta.modules.tool.DocumentToAttributes.DependencyType;
import org.terracotta.modules.tool.InstallListener.InstallNotification;
import org.terracotta.modules.tool.util.ChecksumUtil;
import org.terracotta.modules.tool.util.DownloadUtil;
import org.terracotta.modules.tool.util.DownloadUtil.DownloadOption;

import com.google.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Module extends AbstractModule implements BasicAttributes {

  private final Modules             modules;
  private final Map<String, Object> attributes;
  private final AttributesHelper    attributesHelper;

  @Inject
  public Module(Modules modules, Element module) {
    this(modules, DocumentToAttributes.transform(module));
  }

  Module(Modules modules, Map<String, Object> attributes) {
    this.modules = modules;
    this.attributes = attributes;
    this.attributesHelper = new AttributesHelper(this.attributes);

    groupId = attributesHelper.getAttrValueAsString("groupId", StringUtils.EMPTY);
    artifactId = attributesHelper.getAttrValueAsString("artifactId", StringUtils.EMPTY);
    version = attributesHelper.getAttrValueAsString("version", StringUtils.EMPTY);
  }

  protected Modules owner() {
    return modules;
  }

  public String filename() {
    return attributesHelper.filename();
  }

  public File installPath() {
    return attributesHelper.installPath();
  }

  public URL repoUrl() {
    return attributesHelper.repoUrl();
  }

  public String category() {
    return attributesHelper.getAttrValueAsString("category", StringUtils.EMPTY);
  }

  public String contactAddress() {
    return attributesHelper.getAttrValueAsString("contactAddress", StringUtils.EMPTY);
  }

  public String copyright() {
    return attributesHelper.getAttrValueAsString("copyright", StringUtils.EMPTY);
  }

  public List<AbstractModule> dependencies() {
    List<AbstractModule> list = new ArrayList<AbstractModule>();
    if (attributes.containsKey("dependencies")) {
      List<Map<String, Object>> dependencies = (List<Map<String, Object>>) attributes.get("dependencies");
      for (Map<String, Object> dependencyAttributes : dependencies) {
        DependencyType type = (DependencyType) dependencyAttributes.get("_dependencyType");
        if (DependencyType.INSTANCE.equals(type)) {
          list.add(new BasicModule(this, dependencyAttributes));
          continue;
        } else if (DependencyType.REFERENCE.equals(type)) {
          list.add(new Reference(this, dependencyAttributes));
          continue;
        }
        // XXX dependencyType eval'd as DependencyType.UNKNOWN
        // it means bad data if we should ever get here - we should just have schema to make
        // sure that the data being read is valid - instead of trying to catch a bad state here
        throw new IllegalStateException();
      }
    }
    Collections.sort(list);
    return list;
  }

  /**
   * Descriptive text about the module.
   * 
   * @return A String
   */
  public String description() {
    return attributesHelper.getAttrValueAsString("description", StringUtils.EMPTY);
  }

  /**
   * The URL pointing to the documentation for this module
   * 
   * @return An URL. Will return this module's website URL if none was defined.
   */
  public URL docUrl() {
    return attributesHelper.getAttrValueAsUrl("docURL", website());
  }

  public boolean isInstalled() {
    return isInstalled(modules.repository());
  }

  public boolean isInstalled(File repository) {
    return attributesHelper.isInstalled(repository);
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
    return attributesHelper.getAttrValueAsString("tc-projectStatus", StringUtils.EMPTY);
  }

  public String tcVersion() {
    return attributesHelper.getAttrValueAsString("tc-version", null);
  }

  public String vendor() {
    return attributesHelper.getAttrValueAsString("vendor", StringUtils.EMPTY);
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
    return attributesHelper.getAttrValueAsUrl("website", alturl);
  }

  protected List<AbstractModule> manifest() {
    List<AbstractModule> manifest = new ArrayList<AbstractModule>();
    // manifest, at a minimum, includes this module
    manifest.add(this);
    for (AbstractModule dependency : dependencies()) {
      if (dependency instanceof Reference) {
        Module module = modules.get(dependency.groupId(), dependency.artifactId(), dependency.version());
        // XXX bad data happened - maybe we should be more forgiving here
        if (module == null) throw new IllegalStateException("No listing found for dependency: " + dependency);

        // instance of reference located, include entries from its install manifest into the install manifest
        for (AbstractModule entry : module.manifest()) {
          if (!manifest.contains(entry)) manifest.add(entry);
        }
        continue;
      }
      manifest.add(dependency);
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
  public void install(InstallListener listener, InstallOption... options) {
    install(listener, Arrays.asList(options));
  }

  public void install(InstallListener listener, Collection<InstallOption> options) {
    InstallOptionsHelper installOptions = new InstallOptionsHelper(options);
    List<AbstractModule> manifest = null;

    notifyListener(listener, this, InstallNotification.STARTING, StringUtils.EMPTY);
    try {
      manifest = manifest();
    } catch (IllegalStateException e) {
      String message = "Unable to compute manifest for installation: " + e.getMessage();
      notifyListener(listener, this, InstallNotification.ABORTED, message);
      return;
    }

    for (AbstractModule module : manifest) {
      BasicAttributes basicAttr = (BasicAttributes) module;

      File destdir = new File(modules.repository(), basicAttr.installPath().toString());
      File destfile = new File(destdir, basicAttr.filename());
      if (basicAttr.isInstalled(modules.repository()) && !installOptions.overwrite()) {
        notifyListener(listener, module, InstallNotification.SKIPPED, "Already installed");
        continue;
      }

      if (!installOptions.pretend()) {
        File srcfile = null;
        try {
          srcfile = File.createTempFile("tim-", null);
          download(basicAttr.repoUrl(), srcfile);
        } catch (IOException e) {
          String message = "Attempt to download TIM file at " + basicAttr.repoUrl() + " failed - " + e.getMessage();
          notifyListener(listener, module, InstallNotification.DOWNLOAD_FAILED, message);
          continue;
        }

        if (installOptions.verify()) {
          File md5file = null;
          URL md5url = null;
          try {
            md5file = File.createTempFile("tim-md5-", null);
            md5url = new URL(basicAttr.repoUrl().toExternalForm() + ".md5");
            download(md5url, md5file);
          } catch (IOException e) {
            String message = "Attempt to download checksum file at " + md5url + " failed - " + e.getMessage();
            notifyListener(listener, module, InstallNotification.DOWNLOAD_FAILED, message);
            continue;
          }

          if (!downloadVerified(srcfile, md5file)) {
            String message = "Download might have been corrupted.";
            notifyListener(listener, module, InstallNotification.CHECKSUM_FAILED, message);
            continue;
          }
        }

        try {
          FileUtils.forceMkdir(destdir);
          FileUtils.copyFile(srcfile, destfile);
        } catch (IOException e) {
          String message = destfile + " (" + e.getMessage() + ")";
          notifyListener(listener, module, InstallNotification.INSTALL_FAILED, message);
          continue;
        }
      }

      notifyListener(listener, module, InstallNotification.INSTALLED, "Ok");
    }
  }

  private static boolean downloadVerified(File srcfile, File md5file) {
    try {
      return ChecksumUtil.verifyMD5Sum(srcfile, md5file);
    } catch (Exception e) {
      return false;
    }
  }

  private static void download(URL address, File localfile) throws IOException {
    DownloadUtil downloader = new DownloadUtil();
    downloader.download(address, localfile, DownloadOption.CREATE_INTERVENING_DIRECTORIES,
                        DownloadOption.OVERWRITE_EXISTING);
  }

}
