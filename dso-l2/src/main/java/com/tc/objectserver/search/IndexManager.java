/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;
import com.terracottatech.search.IndexException;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.SearchResult;
import com.terracottatech.search.SyncSnapshot;
import com.terracottatech.search.ValueID;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IndexManager {

  void removeIfValueEqual(String indexName, Map<String, ValueID> toRemove, ObjectID segmentOid,
                          MetaDataProcessingContext metaDataContext) throws IndexException;

  void remove(String indexName, String key, ObjectID segmentOid, MetaDataProcessingContext metaDataContext)
      throws IndexException;

  void update(String indexName, String key, ValueID value, List<NVPair> attributes, ObjectID segmentOid,
              MetaDataProcessingContext metaDataContext) throws IndexException;

  void insert(String cacheName, String key, ValueID cacheValue, List<NVPair> attributes, ObjectID segmentOid,
              MetaDataProcessingContext MetaDataProcessingContext) throws IndexException;

  void clear(String indexName, ObjectID segmentOid, MetaDataProcessingContext metaDataContext) throws IndexException;

  void replace(String indexName, String key, ValueID value, ValueID previousValue, List<NVPair> attributes,
               ObjectID segmentOid, MetaDataProcessingContext metaDataContext) throws IndexException;

  public SearchResult searchIndex(String indexName, List queryStack, boolean includeKeys, boolean includeValues,
                                  Set<String> attributeSet, List<NVPair> sortAttributes, List<NVPair> aggregators,
                                  int maxResults) throws IndexException;

  public SyncSnapshot snapshot() throws IndexException;

  void shutdown();

  void optimizeSearchIndex(String indexName);

  String[] getSearchIndexNames();

  InputStream getIndexFile(String cacheName, String indexId, String fileName) throws IOException;

}
