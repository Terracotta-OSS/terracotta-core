/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;
import com.terracottatech.search.IndexException;
import com.terracottatech.search.IndexFile;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.SearchResult;
import com.terracottatech.search.SyncSnapshot;
import com.terracottatech.search.ValueID;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NullIndexManager implements IndexManager {

  @Override
  public SearchResult searchIndex(String name, List queryStack, boolean includeKeys, boolean includeValues,
                                  Set<String> attributeSet, Set<String> groupByAttributes, List<NVPair> sortAttributes,
                                  List<NVPair> aggregators, int maxResults) {
    return null;
  }

  @Override
  public void shutdown() {
    //
  }

  @Override
  public void remove(String indexName, String key, ObjectID segmentOid, MetaDataProcessingContext metaDataContext) {
    //
  }

  @Override
  public void update(String indexName, String key, ValueID value, List<NVPair> attributes, ObjectID segmentOid,
                     MetaDataProcessingContext metaDataContex) {
    //
  }

  @Override
  public void insert(String indexName, String key, ValueID value, List<NVPair> attributes, ObjectID segmentOid,
                     MetaDataProcessingContext metaDataContext) {
    //
  }

  @Override
  public void putIfAbsent(String indexName, String key, ValueID value, List<NVPair> attributes, ObjectID segmentOid,
                          MetaDataProcessingContext metaDataContext) {
    //
  }

  @Override
  public void clear(String indexName, ObjectID segmentOid, MetaDataProcessingContext metaDataContext) {
    //
  }

  @Override
  public SyncSnapshot snapshot(String syncId) {
    return new SyncSnapshot() {
      @Override
      public void release() {
        //
      }

      @Override
      public Map<String, List<IndexFile>> getFilesToSync() {
        return Collections.EMPTY_MAP;
      }
    };
  }

  @Override
  public void backup(File destDir, SyncSnapshot syncSnapshot) throws IndexException {
    //
  }

  @Override
  public void removeIfValueEqual(String indexName, Map<String, ValueID> toRemove, ObjectID segmentOid,
                                 MetaDataProcessingContext metaDataContext, boolean fromEviction) {
    //
  }

  @Override
  public void replace(String indexName, String key, ValueID value, ValueID previousValue, List<NVPair> attributes,
                      ObjectID segmentOid, MetaDataProcessingContext metaDataContext) {
    //
  }

  @Override
  public void optimizeSearchIndex(String indexName) {
    //
  }

  @Override
  public String[] getSearchIndexNames() {
    return new String[] {};
  }

  @Override
  public InputStream getIndexFile(String cacheName, String indexId, String fileName) {
    throw new AssertionError();
  }

  @Override
  public void deleteIndex(final String indexName, final MetaDataProcessingContext processingContext) throws IndexException {
    //
  }
}
