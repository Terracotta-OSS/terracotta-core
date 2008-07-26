/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.terracotta.modules.tool.util.DownloadUtil;
import org.terracotta.modules.tool.util.DownloadUtil.DownloadOption;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A single Terracotta Integration Module (TIM) artifact. A TIM has a composite unique identifier consisting of groupId,
 * artifactId, and version, which is represented by the {@link ModuleId} class. Note that TIMs that are packaged
 * together into an archive are still represented as separate Tim objects.
 */

public class Module implements Comparable {
  private enum SymbolStyle {
    BINARY, TERNARY
  }

  public static final String     LEGEND              = "legend: [+] already installed  [!] installed but newer version exists  [-] not installed";

  private static final String    SYMBOL_OUTOFDATE    = "!";
  private static final String    SYMBOL_NOTINSTALLED = "-";
  private static final String    SYMBOL_INSTALLED    = "+";

  private final ModuleId         id;
  private final String           repoUrl;
  private final String           installPath;
  private final String           filename;

  private final String           tcVersion;
  private final String           tcProjectStatus;
  private final String           website;
  private final String           vendor;
  private final String           copyright;
  private final String           category;
  private final String           docUrl;
  private final String           contactAddress;
  private final String           description;
  private final List<Dependency> dependencies;

  private final Modules          modules;

  private static File            repositoryPath      = null;

  public ModuleId getId() {
    return id;
  }

  public String getRepoUrl() {
    return repoUrl;
  }

  public String getInstallPath() {
    return installPath;
  }

  public String getFilename() {
    return filename;
  }

  public List<ModuleId> dependencies() {
    List<ModuleId> list = new ArrayList<ModuleId>();
    list.addAll(computeManifest().keySet());
    list.remove(id);
    Collections.sort(list);
    return list;
  }

  public String getTcProjectStatus() {
    return tcProjectStatus;
  }

  public String getTcVersion() {
    return tcVersion;
  }

  public String getWebsite() {
    return website;
  }

  public String getVendor() {
    return vendor;
  }

  public String getCopyright() {
    return copyright;
  }

  public String getCategory() {
    return category;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Return the list of direct dependencies of this module.
   */
  public List<Dependency> getDependencies() {
    return Collections.unmodifiableList(dependencies);
  }

  public static Module create(Modules modules, Element module) {
    return new Module(modules, module);
  }

  Module(Modules modules, Element root) {
    this.modules = modules;
    id = ModuleId.create(root);
    tcVersion = getChildText(root, "tc-version");
    tcProjectStatus = getChildText(root, "tc-projectStatus");
    website = getChildText(root, "website");
    vendor = getChildText(root, "vendor");
    copyright = getChildText(root, "copyright");
    category = getChildText(root, "category");
    description = getChildText(root, "description");
    repoUrl = getChildText(root, "repoURL");
    docUrl = getChildText(root, "docURL");
    contactAddress = getChildText(root, "contactAddress");
    installPath = getChildText(root, "installPath");
    filename = getChildText(root, "filename");
    dependencies = new ArrayList<Dependency>();
    if (root.getChild("dependencies") != null) {
      List<Element> children = root.getChild("dependencies").getChildren();
      for (Element child : children) {
        dependencies.add(new Dependency(child));
      }
    }

    assert repoUrl.length() > 0 : "repoUrl field was empty";
    assert installPath.length() > 0 : "installPath field was empty";
    assert filename.length() > 0 : "filename field was empty";
  }

  private String getChildText(Element element, String name) {
    return getChildText(element, name, "");
  }

  private String getChildText(Element element, String name, String defaultValue) {
    return (element.getChildText(name) == null ? defaultValue : element.getChildText(name)).trim();
  }

  public int compareTo(Object obj) {
    assert obj instanceof Module : "must be instanceof Module";
    Module other = (Module) obj;
    return id.compareTo(other.getId());
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;
    if (getClass() != obj.getClass()) return false;
    Module other = (Module) obj;
    return toString().equals(other.toString());
  }

  public String getSymbolicName() {
    return ModuleId.computeSymbolicName(id.getGroupId(), id.getArtifactId());
  }

  public boolean isOlder(Module other) {
    assert getSymbolicName().equals(other.getSymbolicName()) : "symbolicNames do not match.";
    // return id.sortableVersion().compareTo(o.getId().sortableVersion()) < 0;
    return this.compareTo(other) < 0;
  }

  public String toString() {
    return getSymbolicName() + " " + id.getVersion();
  }

  /**
   * Returns a list of all available version for this module. The list returned does not include the version of this
   * module.
   */
  public List<String> getVersions() {
    return getVersions(false);
  }

  public List<String> getAllVersions() {
    return getVersions(true);
  }

  private List<String> getVersions(boolean inclusive) {
    List<ModuleId> idlist = new ArrayList<ModuleId>();
    for (Module module : this.modules.list()) {
      if (!module.isSibling(this)) continue;
      if (!inclusive && (module == this)) continue;
      idlist.add(module.getId());
    }
    Collections.sort(idlist);
    List<String> versions = new ArrayList<String>();
    for (ModuleId mid : idlist) {
      versions.add(mid.getVersion());
    }
    return versions;
  }

  public boolean isSibling(Module other) {
    return this.getSymbolicName().equals(other.getSymbolicName());
  }

  /**
   * Returns the siblings of this module. A sibling is another module with the matching symbolicName but a different
   * version. The list returned will not include this module.
   */
  public List<Module> getSiblings() {
    List<Module> siblings = new ArrayList<Module>();
    List<String> versions = getVersions();
    for (String version : versions) {
      Module sibling = this.modules.get(ModuleId.create(id.getGroupId(), id.getArtifactId(), version));
      siblings.add(sibling);
    }
    Collections.sort(siblings);
    Collections.reverse(siblings);
    return siblings;
  }

  /**
   * Indicates if this module is the latest version among its siblings.
   */
  public boolean isLatest() {
    List<String> versions = getVersions(true);
    return versions.indexOf(this.getId().getVersion()) == (versions.size() - 1);
  }

  /**
   * Install this module.
   * 
   * @param verify TODO
   * @throws IOException
   */
  public void install(boolean verify, boolean overwrite, boolean pretend, PrintWriter out) {
    Map<ModuleId, Dependency> manifest = computeManifest();
    List<ModuleId> list = new ArrayList<ModuleId>(manifest.keySet());

    out.println("Installing " + id.toDigestString() + " and dependencies...");
    for (ModuleId key : list) {
      Dependency dependency = manifest.get(key);
      String dependencyId = dependency.getId().toDigestString();

      File destdir = new File(repositoryPath(), dependency.getInstallPath());
      File destfile = new File(destdir, dependency.getFilename());
      if (isInstalled(dependency) && !overwrite) {
        out.println("   Skipped: " + dependencyId);
        continue;
      }

      if (!pretend) {
        File srcfile = null;
        try {
          srcfile = File.createTempFile("tim-", null);
          download(dependency.getRepoUrl(), srcfile);
        } catch (IOException e) {
          out.println("   Unable to download: " + dependencyId);
          out.println("             from URL: " + dependency.getRepoUrl());
          continue;
        }

        if (verify) {
          File md5file = null;
          try {
            md5file = File.createTempFile("tim-md5-", null);
            download(dependency.getRepoUrl() + ".md5", md5file);
          } catch (IOException e) {
            out.println("   Unable to verify: " + dependencyId);
            continue;
          }
          if (!downloadVerified(srcfile, md5file)) {
            out.println("   Checksum failed: " + dependencyId);
            continue;
          }
        }

        try {
          FileUtils.forceMkdir(destdir);
          FileUtils.copyFile(srcfile, destfile);
        } catch (IOException e) {
          out.println("   Unable to install: " + dependencyId);
          continue;
        }
      }
      out.println("   Installed: " + dependencyId);
    }
  }

  private Map<ModuleId, Dependency> computeManifest() {
    Map<ModuleId, Dependency> manifest = new HashMap<ModuleId, Dependency>();
    manifest.put(id, new Dependency(this));
    assert dependencies != null : "dependencies field must not be null";
    for (Dependency dependency : dependencies) {
      if (dependency.isReference()) {
        Module module = modules.get(dependency.getId());
        assert module != null : "ID yields null: " + dependency.toString();
        for (Entry<ModuleId, Dependency> entry : module.computeManifest().entrySet()) {
          if (manifest.containsKey(entry.getKey())) continue;
          manifest.put(entry.getKey(), entry.getValue());
        }
        continue;
      }
      manifest.put(dependency.getId(), dependency);
    }
    return manifest;
  }

  private static boolean downloadVerified(File srcfile, File md5file) {
    BufferedReader reader = null;
    InputStream src = null;
    try {
      reader = new BufferedReader(new FileReader(md5file));
      String expected = reader.readLine();
      reader.close();

      MessageDigest md = MessageDigest.getInstance("MD5");
      md.reset();
      src = new BufferedInputStream(new FileInputStream(srcfile));
      byte[] buffer = new byte[4096];
      int read = 0;
      while ((read = src.read(buffer)) > 0) {
        md.update(buffer, 0, read);
      }

      byte[] md5sum = md.digest();
      BigInteger bigInt = new BigInteger(1, md5sum);
      String actual = bigInt.toString(16);
      src.close();
      return actual.equals(expected);
    } catch (IOException e) {
      return false;
    } catch (NoSuchAlgorithmException e) {
      return false;
    } finally {
      try {
        if (reader != null) reader.close();
        if (src != null) src.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static void download(String address, File localfile) throws IOException {
    DownloadUtil downloader = new DownloadUtil();
    downloader.download(new URL(address), localfile, DownloadOption.CREATE_INTERVENING_DIRECTORIES,
                        DownloadOption.OVERWRITE_EXISTING);
  }

  private File installPath(String path, String name) {
    return new File(new File(repositoryPath(), path), name);
  }

  private File installPath() {
    return installPath(installPath, filename);
  }

  /**
   * Returns the canonical file path used as root directory when installing modules.
   */
  public static File repositoryPath() {
    if (repositoryPath != null) return repositoryPath;
    String rootdir = System.getProperty("tc.install-root", System.getProperty("java.io.tmpdir"));
    repositoryPath = new File(rootdir, "modules");
    try {
      repositoryPath = repositoryPath.getCanonicalFile();
    } catch (IOException e) {
      //
    }
    return repositoryPath;
  }

  /**
   * Checks if a module described by Dependency has been installed.
   */
  public boolean isInstalled(Dependency dependency) {
    return installPath(dependency.getInstallPath(), dependency.getFilename()).exists();
  }

  /**
   * Checks if this module has been installed.
   */
  public boolean isInstalled() {
    return installPath().exists();
  }

  private void printInstallationInfo(PrintWriter out) {
    if (isInstalled()) out.println("Installed at " + installPath().getParent());
    if (getVersions().isEmpty()) {
      out.println("There are no other versions of this TIM that is compatible with TC " + modules.tcVersion());
    } else {
      out.println("The following versions are also available for TC " + modules.tcVersion() + ":\n");
      List<Module> siblings = this.getSiblings();
      for (Module sibling : siblings) {
        out.print("\t" + sibling.installStateSymbol(SymbolStyle.BINARY));
        out.print(" " + sibling.getId().getVersion());
        if (sibling.isInstalled()) out.println("\tinstalled at " + sibling.installPath().getParent());
        else out.println();
      }
    }
  }

  private void printBasicInfo(PrintWriter out) {
    out.println("Installed: " + (isInstalled() ? "YES" : "NO"));
    out.println();
    if (vendor.length() > 0) out.println("Author   : " + vendor);
    if (copyright.length() > 0) out.println("Copyright: " + copyright);
    if (website.length() > 0) out.println("Homepage : " + website);
    if (contactAddress.length() > 0) out.println("Contact  : " + contactAddress);
    if (docUrl.length() > 0) out.println("Docs     : " + docUrl);
    out.println("Download : " + repoUrl);
    out.println("Status   : " + tcProjectStatus); // CERTIFIED, EXPERIMENTAL, NONE, etc.
    out.println();
    if (description.length() > 0) {
      out.println(description.replaceAll("\n[ ]+", "\n"));
      out.println();
    }
    out.println("Compatible with " + (tcVersion.equals("*") ? "any Terracotta version." : "TC " + tcVersion));
  }

  private void printDependenciesInfo(PrintWriter out) {
    List<ModuleId> requires = dependencies();
    out.println("Dependencies:\n");
    if (requires.isEmpty()) {
      out.println("\tNone.");
      return;
    }
    Collections.sort(requires);
    Map<ModuleId, Dependency> manifest = computeManifest();
    for (ModuleId m : requires) {
      Dependency d = manifest.get(m);
      out.println("\t" + installStateSymbol(isInstalled(d)) + " " + m.toDigestString());
    }
  }

  private void printMavenInfo(PrintWriter out) {
    out.println("Maven Coordinates:\n");
    out.println("\tgroupId   : " + id.getGroupId());
    out.println("\tartifactId: " + id.getArtifactId());
    out.println("\tversion   : " + id.getVersion());
  }

  private void printConfigInfo(PrintWriter out) {
    Element parent = new Element("modules");
    Element child = new Element("module");
    parent.addContent(child);
    child.setAttribute("name", id.getArtifactId());
    child.setAttribute("version", id.getVersion());
    if (!id.isUsingDefaultGroupId()) child.setAttribute("group-id", id.getGroupId());

    out.println("Configuration:\n");
    StringWriter sw = new StringWriter();
    Format formatter = Format.getPrettyFormat();
    formatter.setIndent("\t");
    XMLOutputter xmlout = new XMLOutputter(formatter);
    try {
      xmlout.output(parent, new PrintWriter(sw));
      out.println(sw.toString().replaceAll("\\<", "\t\\<"));
    } catch (IOException e) {
      out.println(e.getMessage());
    }
  }

  public void printDetails(PrintWriter out) {
    StringBuffer text = new StringBuffer();

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    pw.println(id.toDigestString());
    text.append(sw.toString());

    sw = new StringWriter();
    pw = new PrintWriter(sw);
    printBasicInfo(pw);
    pw.println();

    printDependenciesInfo(pw);
    pw.println();

    printMavenInfo(pw);
    pw.println();

    printConfigInfo(pw);
    pw.println();

    printInstallationInfo(pw);

    text.append("\n\t").append(sw.toString().replaceAll("\n", "\n\t"));
    out.println(StringUtils.chomp(text.toString().trim().replaceAll("\t", "   ")));

    out.println();
    out.println(LEGEND);
  }

  public void printSummary(PrintWriter out) {
    StringBuffer text = new StringBuffer();

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    pw.println(id.toDigestString());
    text.append(sw.toString());

    sw = new StringWriter();
    pw = new PrintWriter(sw);
    printBasicInfo(pw);

    text.append("\n\t").append(sw.toString().replaceAll("\n", "\n\t"));
    out.println(StringUtils.chomp(text.toString().trim().replaceAll("\t", "   ")));
  }

  public void printDigest(PrintWriter out) {
    out.println(installStateSymbol(SymbolStyle.TERNARY) + " " + id.toDigestString());
  }

  private String installStateSymbol(boolean state) {
    return state ? SYMBOL_INSTALLED : SYMBOL_NOTINSTALLED;
  }

  private String installStateSymbol(SymbolStyle style) {
    String marker = isInstalled() ? SYMBOL_INSTALLED : SYMBOL_NOTINSTALLED;
    if ((style == SymbolStyle.TERNARY) && marker.equals(SYMBOL_NOTINSTALLED)) {
      List<Module> siblings = getSiblings();
      for (Module sibling : siblings) {
        if (!sibling.isInstalled()) continue;
        marker = SYMBOL_OUTOFDATE;
        break;
      }
    }
    return marker;
  }
}
