package com.tc.objectserver.impl;

import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.util.ProductInfo;
import com.tc.util.version.Version;

/**
 * @author tim
 */
public class ServerPersistenceVersionChecker {
  private final ClusterStatePersistor clusterStatePersistor;
  private final ProductInfo productInfo;

  public ServerPersistenceVersionChecker(final ClusterStatePersistor clusterStatePersistor) {
    this(clusterStatePersistor, ProductInfo.getInstance());
  }

  ServerPersistenceVersionChecker(final ClusterStatePersistor clusterStatePersistor, final ProductInfo productInfo) {
    this.clusterStatePersistor = clusterStatePersistor;
    this.productInfo = productInfo;
  }

  private boolean checkVersion(Version persisted, Version current) {
    // TODO: increase the range of allowed versions when they exist
    if (persisted != null && (
        current.major() != persisted.major() ||
        current.minor() < persisted.minor() ||
        current.micro() < persisted.micro())) {
      return false;
    }
    return true;
  }

  public void checkAndSetVersion() {
    Version currentVersion = new Version(productInfo.version());
    Version persistedVersion = clusterStatePersistor.getVersion();
    if (checkVersion(clusterStatePersistor.getVersion(), currentVersion)) {
      clusterStatePersistor.setVersion(currentVersion);
    } else {
      throw new IllegalStateException("Incompatible data format version. Got " + persistedVersion + " expected " + currentVersion);
    }
  }
}
