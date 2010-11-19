/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.metadata.NVPair;
import com.tc.search.SortOperations;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Index {

  void remove(Object key) throws IndexException;

  void upsert(Object key, List<NVPair> attributes) throws IndexException;

  IndexContext search(LinkedList queryStack, boolean includeKeys, Set<String> attributeSet,
                      Map<String, SortOperations> sortAttributes, List<NVPair> aggregators, int maxResults)
      throws IOException;

  void close();

}
