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

public class VersionCompatibility {
  private static final Version MINIMUM_COMPATIBLE_PERSISTENCE = new Version("4.1.2");

  public boolean isCompatibleClientServer(Version clientVersion, Version serverVersion) {
    return isCompatible(clientVersion, serverVersion);
  }

  public boolean isCompatibleServerServer(Version v1, Version v2) {
    return isCompatible(v1, v2);
  }

  public boolean isCompatibleServerPersistence(Version persisted, Version current) {
    if (persisted.compareTo(getMinimumCompatiblePersistence()) < 0) {
      return false;
    } else if (persisted.major() == current.major() && persisted.minor() == current.minor()) {
      return true;
    } else {
      return current.compareTo(persisted) >= 0;
    }
  }

  private static boolean isCompatible(Version v1, Version v2) {
    if (v1 == null || v2 == null) { throw new NullPointerException(); }
    return ((v1.major() == v2.major()) && (v1.minor() == v2.minor()));
  }

  public Version getMinimumCompatiblePersistence() {
    return MINIMUM_COMPATIBLE_PERSISTENCE;
  }
}
