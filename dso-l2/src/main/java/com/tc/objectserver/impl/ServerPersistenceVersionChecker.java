package com.tc.objectserver.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.util.ProductInfo;
import com.tc.util.version.Version;

/**
 * @author tim
 */
public class ServerPersistenceVersionChecker {
  private static final TCLogger LOGGER = TCLogging.getLogger(ServerPersistenceVersionChecker.class);
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
      LOGGER.error("Incompatible data format detected. Found data for version " + persistedVersion +" expecting data for version " + currentVersion + ".");
      LOGGER.error("Verify that the correct data is in the server data path, and try again.");
      throw new IllegalStateException("Incompatible data format version. Got " + persistedVersion + " expected " + currentVersion);
    }
  }
}
