/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.l2.state.StateChangeListener;
import com.tc.object.metadata.NVPair;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public interface IndexManager extends StateChangeListener {

  void remove(String indexName, Object key) throws IndexException;

  void upsert(String indexName, Object key, List<NVPair> attributes) throws IndexException;

  void clear(String indexName) throws IndexException;

  public SearchResult searchIndex(String indexName, LinkedList queryStack, boolean includeKeys,
                                  Set<String> attributeSet, List<NVPair> sortAttributes, List<NVPair> aggregators,
                                  int maxResults) throws IndexException;

  void shutdown();
}
