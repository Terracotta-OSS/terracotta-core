/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.metadata.NVPair;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public interface Index {

  void remove(Object key) throws IndexException;

  void upsert(Object key, List<NVPair> attributes) throws IndexException;

  SearchResult search(LinkedList queryStack, boolean includeKeys, Set<String> attributeSet,
                      List<NVPair> sortAttributes, List<NVPair> aggregators, int maxResults) throws IOException,
      IndexException;

  void close();

}
