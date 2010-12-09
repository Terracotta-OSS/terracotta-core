/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.l2.state.StateChangeListener;
import com.tc.object.metadata.NVPair;
import com.tc.object.metadata.ValueType;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IndexManager extends StateChangeListener {

  Index getIndex(String name);

  Index createIndex(String name, Map<String, ValueType> schema) throws IndexException;

  boolean deleteIndex(String name) throws IndexException;

  public SearchResult searchIndex(String name, LinkedList queryStack, boolean includeKeys, Set<String> attributeSet,
                                  List<NVPair> sortAttributes, List<NVPair> aggregators, int maxResults)
      throws IndexException;

  void shutdown();
}
