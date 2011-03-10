/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.object.metadata.NVPair;
import com.tc.objectserver.metadata.MetaDataProcessingContext;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NullIndexManager implements IndexManager {

  public SearchResult searchIndex(String name, LinkedList queryStack, boolean includeKeys, boolean includeValues,
                                  Set<String> attributeSet, List<NVPair> sortAttributes, List<NVPair> aggregators,
                                  int maxResults) {
    return null;
  }

  public void shutdown() {
    //
  }

  public void remove(String indexName, Object key, ObjectID segmentOid, MetaDataProcessingContext metaDataContext) {
    //
  }

  public void upsert(String indexName, Object key, Object value, List<NVPair> attributes, boolean onlyIfAbsent,
                     ObjectID segmentOid, MetaDataProcessingContext metaDataContext) {
    //
  }

  public void clear(String indexName, ObjectID segmentOid, MetaDataProcessingContext metaDataContext) {
    //
  }

  public SyncSnapshot snapshot() {
    return new SyncSnapshot() {
      public void release() {
        //
      }

      public Map<String, List<IndexFile>> getFilesToSync() {
        return Collections.EMPTY_MAP;
      }
    };
  }

  public void removeIfValueEqual(String indexName, Map<Object, Object> toRemove, ObjectID segmentOid,
                                 MetaDataProcessingContext metaDataContext) {
    //
  }

  public void replace(String indexName, Object key, Object value, Object previousValue, List<NVPair> attributes,
                      ObjectID segmentOid, MetaDataProcessingContext metaDataContext) {
    //
  }

  public void optimizeSearchIndex(String indexName) {
    //
  }

  public String[] getSearchIndexNames() {
    return new String[] {};
  }
}
