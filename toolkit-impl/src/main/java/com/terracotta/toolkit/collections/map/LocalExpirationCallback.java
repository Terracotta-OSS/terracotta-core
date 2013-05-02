/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.terracotta.toolkit.collections.map;

/**
 * Used to notify about local L1 expirations.
 *
 * @author Eugene Shelestovich
 */
interface LocalExpirationCallback {
  void expiredLocally(Object key);
}
