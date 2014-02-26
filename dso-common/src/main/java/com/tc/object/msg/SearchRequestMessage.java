/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.dna.impl.NullObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.metadata.NVPairSerializer;
import com.tc.search.SearchRequestID;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.SearchBuilder;

import java.util.List;
import java.util.Set;

public interface SearchRequestMessage extends TCMessage {
  static final byte SEARCH_REQUEST_ID      = 0;
  static final byte CACHE_NAME             = 1;
  static final byte INCLUDE_KEYS           = 2;
  static final byte ATTRIBUTES             = 3;
  static final byte GROUP_BY_ATTRIBUTES    = 4;
  static final byte SORT_ATTRIBUTES        = 5;
  static final byte AGGREGATORS            = 6;
  static final byte STACK_OPERATION_MARKER = 7;
  static final byte STACK_NVPAIR_MARKER    = 8;
  static final byte MAX_RESULTS            = 9;
  static final byte INCLUDE_VALUES         = 10;
  static final byte VALUE_PREFETCH_SIZE    = 11;
  static final byte PREFETCH_VALUES        = 12;
  static final byte RESULT_PAGE_SIZE       = 13;
  static final byte START_LOAD_OFFSET      = 14;

  static final NVPairSerializer       NVPAIR_SERIALIZER      = new NVPairSerializer();
  static final ObjectStringSerializer NULL_SERIALIZER        = new NullObjectStringSerializer();

  /**
   * ClientID
   */
  public ClientID getClientID();

  /**
   * Search Identifier. return SearchRequestID requestID
   */
  public SearchRequestID getRequestID();

  /**
   * Name of cache to query against.
   * 
   * @return String string.
   */
  public String getCacheName();

  /**
   * Query stack to search
   * 
   * @return List linkedlist
   */
  public List getQueryStack();

  /**
   * Return set of attributes ask for.
   * 
   * @return Set<String>
   */
  public Set<String> getAttributes();

  /**
   * Return a map of sort attributes
   */
  public List<NVPair> getSortAttributes();

  /**
   * Return a map of attribute aggregators
   * 
   * @return List<NVPair>
   */
  public List<NVPair> getAggregators();

  /**
   * Result should include keys
   * 
   * @return boolean
   */
  public boolean includeKeys();

  /**
   * Result should include values
   * 
   * @return boolean
   */
  public boolean includeValues();

  /**
   * Return maximum results size. return integer
   */
  public int getMaxResults();

  /**
   * How many results to return with query response. If actual returned result count exceeds this value, the rest must
   * be fetched using result paging. Set to {@link SearchBuilder.Search#BATCH_SIZE_UNLIMITED} to get all results in one
   * shot in response message regardless of actual hit count.
   */
  public int getResultPageSize();

}
