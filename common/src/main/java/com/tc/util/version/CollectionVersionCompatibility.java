/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.util.version;

import com.tc.productinfo.VersionCompatibility;
import java.util.Collection;

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
