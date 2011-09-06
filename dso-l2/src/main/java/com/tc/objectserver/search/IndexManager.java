/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.object.metadata.NVPair;
import com.tc.objectserver.metadata.MetaDataProcessingContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IndexManager {

  void removeIfValueEqual(String indexName, Map<Object, Object> toRemove, ObjectID segmentOid,
                          MetaDataProcessingContext metaDataContext) throws IndexException;

  void remove(String indexName, Object key, ObjectID segmentOid, MetaDataProcessingContext metaDataContext)
      throws IndexException;

  void upsert(String indexName, Object key, Object value, List<NVPair> attributes, boolean onlyIfAbsent,
              ObjectID segmentOid, MetaDataProcessingContext metaDataContext) throws IndexException;

  void clear(String indexName, ObjectID segmentOid, MetaDataProcessingContext metaDataContext) throws IndexException;

  void replace(String indexName, Object key, Object value, Object previousValue, List<NVPair> attributes,
               ObjectID segmentOid, MetaDataProcessingContext metaDataContext) throws IndexException;

  public SearchResult searchIndex(String indexName, List queryStack, boolean includeKeys, boolean includeValues,
                                  Set<String> attributeSet, List<NVPair> sortAttributes, List<NVPair> aggregators,
                                  int maxResults) throws IndexException;

  public SyncSnapshot snapshot() throws IndexException;

  void shutdown();

  void optimizeSearchIndex(String indexName);

  String[] getSearchIndexNames();

  InputStream getIndexFile(String indexName, String fileName) throws IOException;
}
