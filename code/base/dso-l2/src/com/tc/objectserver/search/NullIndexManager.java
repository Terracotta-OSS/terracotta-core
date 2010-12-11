/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.l2.context.StateChangedEvent;
import com.tc.object.metadata.NVPair;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class NullIndexManager implements IndexManager {

  public boolean deleteIndex(String name) {
    return false;
  }

  public SearchResult searchIndex(String name, LinkedList queryStack, boolean includeKeys, Set<String> attributeSet,
                                  List<NVPair> sortAttributes, List<NVPair> aggregators, int maxResults) {
    return null;
  }

  public void shutdown() {
    // no nothing
  }

  public void remove(String indexName, Object key) {
    //
  }

  public void upsert(String indexName, Object key, List<NVPair> attributes) {
    //
  }

  public void l2StateChanged(StateChangedEvent sce) {
    //
  }

}
