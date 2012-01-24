/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles;

import java.util.ArrayList;
import java.util.List;

public class Modules {
  private final List<Module> modules      = new ArrayList<Module>();
  private final List<String> repositories = new ArrayList<String>();

  public List<String> getRepositories() {
    return repositories;
  }

  public List<Module> getModules() {
    return modules;
  }

  public void addModule(Module m) {
    modules.add(m);
  }

  public void addRepository(String repo) {
    repositories.add(repo);
  }
}
