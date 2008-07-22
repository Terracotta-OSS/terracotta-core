/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.jdom.Element;

public class Dependency {

  private final ModuleId id;
  private final String   repoUrl;
  private final String   installPath;
  private final String   filename;
  private final boolean  isReference;

  public ModuleId getId() {
    return id;
  }

  public boolean isReference() {
    return isReference;
  }

  public String getRepoUrl() {
    return (isReference ? "" : repoUrl).trim();
  }

  public String getInstallPath() {
    return (isReference ? "" : installPath).trim();
  }

  public String getFilename() {
    return (isReference ? "" : filename).trim();
  }

  Dependency(Element element) {
    id = ModuleId.create(element);
    isReference = element.getName().equals("moduleRef");
    if (isReference) {
      repoUrl = null;
      installPath = null;
      filename = null;
      return;
    }
    repoUrl = element.getChildText("repoURL");
    installPath = element.getChildText("installPath");
    filename = element.getChildText("filename");

    assert repoUrl != null : "repoUrl field was null";
    assert installPath != null : "installPath field was null";
    assert filename != null : "filename field was null";
  }

  Dependency(Module module) {
    this.id = module.getId();
    this.repoUrl = module.getRepoUrl();
    this.installPath = module.getInstallPath();
    this.filename = module.getFilename();
    this.isReference = false;

    assert repoUrl.length() > 0 : "repoUrl field was empty";
    assert installPath.length() > 0 : "installPath field was empty";
    assert filename.length() > 0 : "filename field was empty";
  }

  public String toString() {
    return getClass().getSimpleName() + (isReference ? "Ref" : "") + ": " + id.getSymbolicName() + " ["
           + id.getVersion() + "]";
  }

}
