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
package com.tc.util.version;

import com.tc.productinfo.VersionCompatibility;
import java.util.Collection;
import java.util.function.BiFunction;

/**
 * Version compatibility check is currently disabled.
 */
public class CollectionVersionCompatibility implements VersionCompatibility {
  
  private final Collection<VersionCompatibility> versionChecks;

  public CollectionVersionCompatibility(Collection<VersionCompatibility> versionChecks) {
    this.versionChecks = versionChecks;
  }

  @Override
  public boolean isCompatibleClientServer(String clientVersion, String serverVersion) {
    for (VersionCompatibility v : versionChecks) {
      if (!v.isCompatibleClientServer(clientVersion, serverVersion)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isCompatibleServerServer(String v1, String v2) {
    for (VersionCompatibility v : versionChecks) {
      if (!v.isCompatibleClientServer(v1, v2)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isCompatibleServerPersistence(String persisted, String current) {
    for (VersionCompatibility v : versionChecks) {
      if (!v.isCompatibleServerPersistence(persisted, current)) {
        return false;
      }
    }
    return true;
  }
}
