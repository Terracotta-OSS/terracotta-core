/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;
import com.terracottatech.search.IndexFile;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.SearchResult;
import com.terracottatech.search.SyncSnapshot;
import com.terracottatech.search.ValueID;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NullIndexManager implements IndexManager {

  public SearchResult searchIndex(String name, List queryStack, boolean includeKeys, boolean includeValues,
                                  Set<String> attributeSet, Set<String> groupByAttributes, List<NVPair> sortAttributes,
                                  List<NVPair> aggregators, int maxResults) {
    return null;
  }

  public void shutdown() {
    //
  }

  public void remove(String indexName, String key, ObjectID segmentOid, MetaDataProcessingContext metaDataContext) {
    //
  }

  public void update(String indexName, String key, ValueID value, List<NVPair> attributes, ObjectID segmentOid,
                     MetaDataProcessingContext metaDataContex) {
    //
  }

  @Override
  public void insert(String indexName, String key, ValueID value, List<NVPair> attributes, ObjectID segmentOid,
                     MetaDataProcessingContext metaDataContext) {
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

  public void removeIfValueEqual(String indexName, Map<String, ValueID> toRemove, ObjectID segmentOid,
                                 MetaDataProcessingContext metaDataContext) {
    //
  }

  public void replace(String indexName, String key, ValueID value, ValueID previousValue, List<NVPair> attributes,
                      ObjectID segmentOid, MetaDataProcessingContext metaDataContext) {
    //
  }

  public void optimizeSearchIndex(String indexName) {
    //
  }

  public String[] getSearchIndexNames() {
    return new String[] {};
  }

  public InputStream getIndexFile(String cacheName, String indexId, String fileName) {
    throw new AssertionError();
  }
}
