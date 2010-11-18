/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.metadata.NVPair;
import com.tc.object.metadata.ValueType;
import com.tc.search.SortOperations;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IndexManager {

  Index getIndex(String name);

  boolean createIndex(String name, Map<String, ValueType> schema) throws IndexException;

  boolean deleteIndex(String name) throws IndexException;

  public IndexContext searchIndex(String name, LinkedList queryStack, boolean includeKeys, Set<String> attributeSet,
                                  Map<String, SortOperations> sortAttributes, List<NVPair> aggregators, int maxResults)
      throws IndexException;

  void shutdown();
}
