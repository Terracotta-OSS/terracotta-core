/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import java.util.List;

/**
 * A collection of modules. Implementations of this interface may query a remote
 * service for each request or cache information for a period of time.
 */
public interface Modules {

  /**
   * Retrieve a module whose id matches that of the argument.
   * 
   * @param id The id of the module to get.
   */
  public Module get(ModuleId id);

  /**
   * Return a list of all modules. The list returned is sorted by symbolicName + version in ascending order.
   */
  public List<Module> list();

  /**
   * Return a list of all modules but include only the latest version of each module. sorted by symbolicName + version
   * in ascending order.
   */
  public List<Module> listLatest();
  
  /**
   * Return a list of modules matching the groupId and artifactId.
   */
  public List<Module> get(String groupId, String artifactId);

  /**
   * Return a the latest module matching the groupId and artifactId.
   */
  public Module getLatest(String groupId, String artifactId);
  
  /**
   * The TC version that binds this list of modules.
   */
  public String tcVersion();

  /**
   * 
   */
  public List<Module> find(String artifactId, String version, String groupId);
}
