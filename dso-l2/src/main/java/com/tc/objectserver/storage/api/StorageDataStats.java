/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import java.io.Serializable;
import java.util.Map;

public interface StorageDataStats extends Serializable {
  public Map<String, Map<String, Long>> getStorageStats();
}
