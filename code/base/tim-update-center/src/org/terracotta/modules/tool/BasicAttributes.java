/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import java.io.File;
import java.net.URL;

interface BasicAttributes {

  /**
   * The full relative path where a TIM may be installed.
   */
  File installPath();

  /**
   * The URL where the TIM may be retrieved.
   */
  URL repoUrl();

  /**
   * The name of the jar file representing a TIM
   */
  String filename();

  /**
   * Check if the module is installed relative to the path described by the argument.
   * 
   * @param repository The path to the local repository.
   */
  boolean isInstalled(File repository);

}
