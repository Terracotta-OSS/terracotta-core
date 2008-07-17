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
    this.id = ModuleId.create(element);
    this.isReference = element.getName().equals("moduleRef");
    if (this.isReference) {
      this.repoUrl = null;
      this.installPath = null;
      this.filename = null;
      return;
    }
    this.repoUrl = element.getChildText("repoUrl");
    this.installPath = element.getChildText("installPath");
    this.filename = element.getChildText("filename");
    //this.isReference = (this.repoUrl == null) || (this.installPath == null) || (this.filename == null);
  }

  Dependency(Module module) {
    this.id = module.getId();
    this.repoUrl = module.getRepoUrl();
    this.installPath = module.getInstallPath();
    this.filename = module.getFilename();
    this.isReference = false;
  }

  public String toString() {
    return getClass().getSimpleName() + (isReference ? "Ref" : "") + ": " + id.getSymbolicName() + " [" + id.getVersion() + "]";
  }
}
