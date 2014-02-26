/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;
import com.tc.search.SearchRequestID;
import com.terracottatech.search.IndexException;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.QueryID;
import com.terracottatech.search.SearchResult;
import com.terracottatech.search.SyncSnapshot;
import com.terracottatech.search.ValueID;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IndexManager {

  void deleteIndex(String indexName, final MetaDataProcessingContext processingContext) throws IndexException;

  void removeIfValueEqual(String indexName, Map<String, ValueID> toRemove, ObjectID segmentOid,
                          MetaDataProcessingContext metaDataContext, boolean fromEviction) throws IndexException;

  void remove(String indexName, String key, ObjectID segmentOid, MetaDataProcessingContext metaDataContext)
      throws IndexException;

  void update(String indexName, String key, ValueID value, List<NVPair> attributes, ObjectID segmentOid,
              MetaDataProcessingContext metaDataContext) throws IndexException;

  void insert(String cacheName, String key, ValueID cacheValue, List<NVPair> attributes, ObjectID segmentOid,
              MetaDataProcessingContext metaDataContext) throws IndexException;

  public void putIfAbsent(String indexName, String key, ValueID value, List<NVPair> attributes, ObjectID segmentOid,
                          MetaDataProcessingContext metaDataContext) throws IndexException;

  void clear(String indexName, ObjectID segmentOid, MetaDataProcessingContext metaDataContext) throws IndexException;

  void replace(String indexName, String key, ValueID value, ValueID previousValue, List<NVPair> attributes,
               ObjectID segmentOid, MetaDataProcessingContext metaDataContext) throws IndexException;

  public SearchResult searchIndex(String indexName, ClientID clientId, SearchRequestID reqId, List queryStack,
                                  boolean includeKeys, boolean includeValues,
                                  Set<String> attributeSet, Set<String> groupByAttributes, List<NVPair> sortAttributes,
                                  List<NVPair> aggregators, int maxResults, int fetchSize) throws IndexException;

  public SearchResult getSearchResults(String name, ClientID clientId, SearchRequestID reqId, final List queryStack,
                                       final boolean includeKeys, final boolean includeValues,
                                       final Set<String> attributeSet, final List<NVPair> sortAttributes,
                                       final List<NVPair> aggregators, final int maxResults, int start, int pageSize)
      throws IndexException;

  public SyncSnapshot snapshot(String id) throws IndexException;

  void backup(File destDir, SyncSnapshot syncSnapshot) throws IndexException;

  void shutdown() throws IndexException;

  void optimizeSearchIndex(String indexName);

  String[] getSearchIndexNames();

  InputStream getIndexFile(String cacheName, String indexId, String fileName) throws IOException;

  void releaseAllResultsFor(ClientID clientId) throws IndexException;

  void pruneSearchResults(Set<ClientID> clientFilter) throws IndexException;

  void releaseSearchResults(String indexName, QueryID query,
                            MetaDataProcessingContext context) throws IndexException;

  void snapshotForQuery(String indexName, QueryID query, MetaDataProcessingContext context) throws IndexException;
}
