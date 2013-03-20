/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;

import java.util.Map;

public interface EvictableMap {

  int getMaxTotalCount();

  int getSize();

  int getTTLSeconds();

  int getTTISeconds();

  Map<Object, EvictableEntry> getRandomSamples(int count, ClientObjectReferenceSet serverMapEvictionClientObjectRefSet);

  boolean startEviction();

  boolean isEvicting();

  void evictionCompleted();

  String getCacheName();

  boolean isEvictionEnabled();

  boolean isBroadcastEvictions();
}
