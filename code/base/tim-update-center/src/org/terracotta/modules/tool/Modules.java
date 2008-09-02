/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import java.io.File;
import java.util.List;

public interface Modules {

  /**
   * Retrieve the list of qualified modules, ie: modules whose tcVersion() attribute matches or is compatible with the
   * tcVersion() attribute of the instance of this class.
   * 
   * @return A list of modules. The list returned is sorted in the ascending-order.
   */
  List<Module> list();

  /**
   * Similar to Modules.list() but includes only the latest version of each module.
   * 
   * @return A list of modules. The list returned is sorted in the ascending order.
   */
  List<Module> listLatest();

  /**
   * List of all modules regardless of the value of its tcVersion() attribute.
   * 
   * @return A list of modules. The list returned is sorted in the ascending order.
   */
  List<Module> listAll();

  /**
   * The TC version used to qualify or bound the list returned by Modules.list() and Modules.listLatest()
   * 
   * @return A String
   */
  String tcVersion();

  /**
   * The absolute path to the location of the modules' repository.
   * 
   * @return An instance of File
   */
  File repository();

  /**
   * Given a module, locate all of its siblings. The search-space is limited to the list returned by Modules.list()
   * 
   * @return A list of modules. The list returned DOES NOT include the module itself and is sorted in ascending-order.
   */
  List<Module> getSiblings(Module module);

  /**
   * Given a symbolicName, locate all modules with matching symbolicName. The search-space is limited to the list
   * returned by Modules.list()
   * 
   * @return A list of modules. The list returned includes the module itself and is sorted in the ascending-order.
   */
  List<Module> getSiblings(String symbolicName);

  /**
   * Given the groupId, artifactId, and version, locate a module with the same attribute values. The search-space is the
   * list returned by Modules.list()
   * 
   * @return A module. Null if no module matches the search fields.
   */
  Module get(String groupId, String artifactId, String version);

  /**
   * Given a list of fields and values to search, locate all modules with the same attribute values. The search-space is
   * the list returned by Modules.list() - the fields supported is implementation dependent.
   * 
   * @return A list of modules. The list returned includes the module itself and is sorted in the ascending-order.
   */
  List<Module> find(List<String> args);
}
