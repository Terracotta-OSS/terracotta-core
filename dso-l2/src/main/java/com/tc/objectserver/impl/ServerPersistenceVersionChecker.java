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

package com.tc.objectserver.impl;

import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.util.ProductInfo;
import com.tc.util.version.Version;

/**
 * @author tim
 */
public class ServerPersistenceVersionChecker {
  private final ProductInfo productInfo;

  ServerPersistenceVersionChecker() {
    this(ProductInfo.getInstance());
  }

  ServerPersistenceVersionChecker(ProductInfo productInfo) {
    this.productInfo = productInfo;
  }

  void checkAndBumpPersistedVersion(ClusterStatePersistor clusterStatePersistor) {
    Version currentVersion = new Version(productInfo.version());
    Version persistedVersion = clusterStatePersistor.getVersion();
    // only move persisted version forward
    if (persistedVersion == null || currentVersion.compareTo(persistedVersion) > 0) {
      clusterStatePersistor.setVersion(currentVersion);
    }
  }
}
