/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public interface Modules {

  /**
   * Retrieve the list of qualified modules, ie: modules whose tcVersion() attribute matches or is compatible with the
   * tcVersion() attribute of the instance of this class.
   * 
   * @return A list of modules. The list returned is sorted in the ascending-order.
   */
  List<Module> listQualified();

  /**
   * Retrieve the list of available modules, ie: modules whose tcVersion() attribute matches or is compatible with the
   * tcVersion() attribute of the instance of this class, and whose transitive dependencies are all available.
   * 
   * @return A list of modules. The list returned is sorted in the ascending-order.
   */
  List<Module> listAvailable();

  /**
   * Similar to Modules.listAvailable() but includes only the latest version of each module.
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
   * The base URL against which to resolve relative URLs for modules.
   */
  URI relativeUrlBase();

  /**
   * The absolute path to the location of the modules' repository.
   * 
   * @return An instance of File
   */
  File repository();

  /**
   * Given a module, locate all of its siblings. The search-space is limited to the list returned by Modules.list()
   * 
   * @param module The module whose siblings are returned.
   * @return A list of modules. The list returned DOES NOT include the module itself and is sorted in ascending-order.
   */
  List<Module> getSiblings(Module module);

  /**
   * Given a symbolicName, locate all modules with matching symbolicName. The search-space is limited to the list
   * returned by Modules.list()
   * 
   * @param symbolicName This is: groupId + '.' + artifactId - in a collection of modules, those that have the same
   *        symbolicName are considered siblings.
   * @return A list of modules. The list returned includes the module itself and is sorted in the ascending-order.
   */
  List<Module> getSiblings(String symbolicName);

  /**
   * Given the groupId, artifactId, and version, locate all modules with the matching attribute values. The search-space
   * is the list returned by Modules.listQualified()
   * 
   * @param groupId The groupId of the module to get.
   * @param artifactId The artifactId of the module to get
   * @param version The version of the module to get
   * @return A module. Null if no module matches the search fields.
   */
  List<Module> getMatchingQualified(String groupId, String artifactId, String version);

  /**
   * Given the groupId, artifactId, and version, locate a module with the same attribute values. The search-space is the
   * list returned by Modules.listQualified()
   * 
   * @param groupId The groupId of the module to get.
   * @param artifactId The artifactId of the module to get
   * @param version The version of the module to get
   * @return A module. Null if no module matches the search fields.
   */
  Module getQualified(String groupId, String artifactId, String version);

  /**
   * Given the groupId, artifactId, and version, locate a module with the same attribute values. The search-space is the
   * list returned by Modules.listAvailable()
   * 
   * @param groupId The groupId of the module to get.
   * @param artifactId The artifactId of the module to get
   * @param version The version of the module to get
   * @return A module. Null if no module matches the search fields.
   */
  Module getAvailable(String groupId, String artifactId, String version);

  /**
   * Given a list of fields and values to search, locate all modules with the same attribute values. The search-space is
   * the list returned by Modules.listAvailable() - the fields supported is implementation dependent.
   * 
   * @param args The list of fields and values used as arguments for the search.
   * @return A list of modules. The list returned includes the module itself and is sorted in the ascending-order.
   */
  List<Module> find(List<String> args);

  /**
   * Find last version of a module that matches artifactId and groupId Return null if not found
   */
  Module findLatest(String artifactId, String groupId);

  /**
   * Download a module.
   * 
   * @param module The module to download
   * @param verify Flag to indicate if checksum verification is performed after a successful download.
   * @param inspect Flag to indicate if manifest verification is performed after a successful download.
   * @return A File instance representing the just downloaded module.
   * @throws IOException if unable to download or checksum verification failed.
   */
  File download(Installable module, boolean verify, boolean inspect) throws IOException;

  /**
   * The timestamp of the index file (when it was created). Used for debugging purpose
   */
  String indexTimeStamp();
}
