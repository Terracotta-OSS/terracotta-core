/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.util.runtime;

import java.util.Locale;
import java.util.Properties;

/**
 * Stores parsed version information
 */
public final class VmVersion {

  private final boolean        isIBM;
  private final boolean        isJRockit;

  /**
   * Construct with system properties, which will be parsed to determine version. Looks at properties like java.version,
   * java.runtime.version, jrockit.version, java.vm.name, and java.vendor.
   * 
   * @param props Typically System.getProperties()
   */
  public VmVersion(Properties props) {
    this(isJRockit(props), isIBM(props));
  }

  /**
   * Construct with specific version information
   * 
   * @param isJRockit True if BEA JRockit JVM
   * @param isIBM True if IBM JVM
   */
  private VmVersion(boolean isJRockit, boolean isIBM) {
    this.isIBM = isIBM;
    this.isJRockit = isJRockit;
  }

  /**
   * @return True if IBM JVM
   */
  public boolean isIBM() {
    return isIBM;
  }

  /**
   * @return True if BEA JRockit
   */
  public boolean isJRockit() {
    return isJRockit;
  }

  private static boolean isIBM(Properties props) {
    return props.getProperty("java.vm.name", "").toLowerCase(Locale.ENGLISH).contains("ibm");
  }

  private static boolean isJRockit(Properties props) {
    return props.getProperty("jrockit.version") != null
           || props.getProperty("java.vm.name", "").toLowerCase(Locale.ENGLISH).indexOf("jrockit") >= 0;
  }
}
