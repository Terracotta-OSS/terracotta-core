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

/**
 * Version compatibility check is currently disabled.
 */
public class DefaultVersionCompatibility implements VersionCompatibility {

  @Override
  public boolean isCompatibleClientServer(String clientVersion, String serverVersion) {
    return isCompatibleClientServer(new Version(clientVersion), new Version(serverVersion));
  }

  @Override
  public boolean isCompatibleServerServer(String v1, String v2) {
    return isCompatibleServerServer(new Version(v1), new Version(v2));
  }

  @Override
  public boolean isCompatibleServerPersistence(String persisted, String current) {
    return isCompatibleServerServer(new Version(persisted), new Version(current));
  }

  private boolean isCompatibleClientServer(Version clientVersion, Version serverVersion) {
    return isCompatible(clientVersion, serverVersion) && !clientVersion.isNewer(serverVersion, 3);
  }

  private boolean isCompatibleServerServer(Version v1, Version v2) {
    return isCompatible(v1, v2);
  }

  private boolean isCompatibleServerPersistence(Version persisted, Version current) {
    if (persisted.major() == current.major() && persisted.minor() == current.minor()) {
      return true;
    } else {
      return current.compareTo(persisted) >= 0;
    }
  }

  private static boolean isCompatible(Version v1, Version v2) {
    if (v1 == null || v2 == null) { throw new NullPointerException(); }
    return ((v1.major() == v2.major()) && (v1.minor() == v2.minor()));
  }

  public static boolean isNewer(Version v1, Version v2, int depth) {
    if (v1 == null || v2 == null) { throw new NullPointerException(); }
    return v1.isNewer(v2, depth);
  }
}
