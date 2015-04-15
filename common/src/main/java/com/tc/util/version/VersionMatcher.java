/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.util.version;

/**
 * This class encapsulates knowledge about whether a particular module or modules are valid within the current tc/api
 * version.
 */

public class VersionMatcher {

  public static final String ANY_VERSION = "*";

  private final String       tcVersion;

  public VersionMatcher(String tcVersion) {
    if (tcVersion == null || tcVersion.equals(ANY_VERSION)) { throw new IllegalArgumentException("Invalid tcVersion: "
                                                                                                 + tcVersion); }

    this.tcVersion = tcVersion;
  }

  /**
   * Determine whether a module's tc version matches with the current Terracotta installation's tc
   * version.
   * 
   * @param moduleTcVersion is expected to be: * or exact like 3.0.0
   * @return true if module is suitable for this installation
   */
  public boolean matches(String moduleTcVersion) {
    return tcMatches(moduleTcVersion);
  }

  private boolean tcMatches(String moduleTcVersion) {
    return ANY_VERSION.equals(moduleTcVersion) /* || tcVersion.equals("[unknown]") */
           || tcVersion.equals(moduleTcVersion);
  }

}
