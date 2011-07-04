/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.ObjectID;

import java.util.Map;
import java.util.SortedSet;

public interface EvictableMap {

  public int getMaxTotalCount();

  public int getSize();

  public int getTTLSeconds();

  public int getTTISeconds();

  public Map getRandomSamples(int count, SortedSet<ObjectID> ignoreList);

  public void evictionCompleted();

  public String getCacheName();

}
