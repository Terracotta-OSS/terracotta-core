/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

/**
 * Options used to control behavior of the {@link Module#dinstall(PrintWriter out, InstallOption...)} method.
 */
public enum InstallOption {
  /** Should install check the md5 sum of the download file before actuall installation? */
  SKIP_VERIFY,

  /** Should existing installations be overwritten? */
  OVERWRITE,

  /** Synonym to OVERWRITE */
  FORCE,

  /**
   * Download and perform all other checks except actual installation.
   */
  PRETEND
}
