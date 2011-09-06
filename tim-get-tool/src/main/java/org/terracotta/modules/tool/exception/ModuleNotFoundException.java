/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.tool.exception;

public class ModuleNotFoundException extends RuntimeException {

  public ModuleNotFoundException(String moduleName) {
    super("Module not found: " + moduleName);
  }

}
