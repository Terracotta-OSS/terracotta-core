/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.SearchRequestID;
import com.tc.object.dna.impl.NullObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.metadata.AbstractNVPair;
import com.tc.object.metadata.NVPair;
import com.tc.object.session.SessionID;
import com.tc.search.StackOperations;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * @author Nabib El-Rahman
 */
public class SearchQueryRequestMessageImpl extends DSOMessageBase implements SearchQueryRequestMessage {

  private static final ObjectStringSerializer NULL_SERIALIZER        = new NullObjectStringSerializer();

  private final static byte                   SEARCH_REQUEST_ID      = 0;
  private final static byte                   GROUP_ID_FROM          = 1;
  private final static byte                   CACHE_NAME             = 2;
  private final static byte                   INCLUDE_KEYS           = 3;
  private final static byte                   ATTRIBUTES             = 4;
  private final static byte                   SORT_ATTRIBUTES        = 5;
  private final static byte                   AGGREGATORS            = 6;
  private final static byte                   STACK_OPERATION_MARKER = 7;
  private final static byte                   STACK_NVPAIR_MARKER    = 8;
  private final static byte                   MAX_RESULTS            = 9;
  private final static byte                   INCLUDE_VALUES         = 10;
  private final static byte                   BATCH_SIZE             = 11;
  private final static byte                   PREFETCH_FIRST_BATCH   = 12;

  private SearchRequestID                     requestID;
  private GroupID                             groupIDFrom;
  private String                              cacheName;
  private LinkedList                          queryStack;
  private boolean                             includeKeys;
  private boolean                             includeValues;
  private Set<String>                         attributes;
  private List<NVPair>                        sortAttributes;
  private List<NVPair>                        aggregators;
  private int                                 maxResults;
  private int                                 batchSize;
  private boolean                             prefetchFirstBatch;

  public SearchQueryRequestMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                       MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public SearchQueryRequestMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                       TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void initializeSearchRequestMessage(SearchRequestID searchRequestID, GroupID groupID, String cache,
                                             LinkedList stack, boolean keys, boolean values, Set<String> attributeSet,
                                             List<NVPair> sortAttributesMap, List<NVPair> attributeAggregators,
                                             int max, int batch, boolean prefetchFirst) {
    this.requestID = searchRequestID;
    this.groupIDFrom = groupID;
    this.cacheName = cache;
    this.queryStack = stack;
    this.includeKeys = keys;
    this.includeValues = values;
    this.attributes = attributeSet;
    this.sortAttributes = sortAttributesMap;
    this.aggregators = attributeAggregators;
    this.maxResults = max;
    this.batchSize = batch;
    this.prefetchFirstBatch = prefetchFirst;
  }

  @Override
  protected void dehydrateValues() {
    final TCByteBufferOutputStream outStream = getOutputStream();

    putNVPair(SEARCH_REQUEST_ID, this.requestID.toLong());
    putNVPair(GROUP_ID_FROM, this.groupIDFrom.toInt());
    putNVPair(CACHE_NAME, this.cacheName);
    putNVPair(INCLUDE_KEYS, this.includeKeys);
    putNVPair(INCLUDE_VALUES, this.includeValues);
    putNVPair(MAX_RESULTS, this.maxResults);
    putNVPair(BATCH_SIZE, this.batchSize);
    putNVPair(PREFETCH_FIRST_BATCH, this.prefetchFirstBatch);

    putNVPair(ATTRIBUTES, this.attributes.size());
    for (final String attribute : this.attributes) {
      outStream.writeString(attribute);
    }

    putNVPair(SORT_ATTRIBUTES, this.sortAttributes.size());
    for (final NVPair sortedAttributes : this.sortAttributes) {
      sortedAttributes.serializeTo(outStream, NULL_SERIALIZER);
    }

    putNVPair(AGGREGATORS, this.aggregators.size());
    for (final NVPair attributeAggregator : this.aggregators) {
      attributeAggregator.serializeTo(outStream, NULL_SERIALIZER);
    }

    if (!queryStack.isEmpty()) {
      for (ListIterator iter = queryStack.listIterator(queryStack.size()); iter.hasPrevious();) {
        Object obj = iter.previous();
        if (obj instanceof StackOperations) {
          StackOperations operation = (StackOperations) obj;
          putNVPair(STACK_OPERATION_MARKER, operation.name());
        } else if (obj instanceof NVPair) {
          AbstractNVPair pair = (AbstractNVPair) obj;
          putNVPair(STACK_NVPAIR_MARKER, pair, NULL_SERIALIZER);
        } else {
          throw new AssertionError("Unexpected object: " + obj);
        }
      }
    }
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    if (queryStack == null) {
      queryStack = new LinkedList();
    }
    final TCByteBufferInputStream inputStream = getInputStream();
    switch (name) {
      case SEARCH_REQUEST_ID:
        this.requestID = new SearchRequestID(getLongValue());
        return true;

      case GROUP_ID_FROM:
        this.groupIDFrom = new GroupID(getIntValue());
        return true;

      case CACHE_NAME:
        this.cacheName = getStringValue();
        return true;

      case INCLUDE_KEYS:
        this.includeKeys = getBooleanValue();
        return true;

      case INCLUDE_VALUES:
        this.includeValues = getBooleanValue();
        return true;

      case MAX_RESULTS:
        this.maxResults = getIntValue();
        return true;

      case BATCH_SIZE:
        this.batchSize = getIntValue();
        return true;

      case PREFETCH_FIRST_BATCH:
        this.prefetchFirstBatch = getBooleanValue();
        return true;

      case ATTRIBUTES:
        this.attributes = new HashSet<String>();
        int count = getIntValue();
        // Directly decode the key
        while (count-- > 0) {
          String attribute = getStringValue();
          this.attributes.add(attribute);
        }
        return true;

      case SORT_ATTRIBUTES:
        this.sortAttributes = new LinkedList();
        int sortCount = getIntValue();

        while (sortCount-- > 0) {
          NVPair pair = AbstractNVPair.deserializeInstance(inputStream, NULL_SERIALIZER);
          this.sortAttributes.add(pair);
        }
        return true;

      case AGGREGATORS:
        this.aggregators = new LinkedList();
        int attributeAggregatorCount = getIntValue();

        while (attributeAggregatorCount-- > 0) {
          NVPair pair = AbstractNVPair.deserializeInstance(inputStream, NULL_SERIALIZER);
          this.aggregators.add(pair);
        }
        return true;

      case STACK_OPERATION_MARKER:
        StackOperations operation = StackOperations.valueOf(getStringValue());
        queryStack.addFirst(operation);
        return true;

      case STACK_NVPAIR_MARKER:
        NVPair pair = AbstractNVPair.deserializeInstance(inputStream, NULL_SERIALIZER);
        queryStack.addFirst(pair);
        return true;

      default:
        return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  public String getCacheName() {
    return this.cacheName;
  }

  /**
   * {@inheritDoc}
   */
  public LinkedList getQueryStack() {
    return this.queryStack;
  }

  /**
   * {@inheritDoc}
   */
  public SearchRequestID getRequestID() {
    return requestID;
  }

  /**
   * {@inheritDoc}
   */
  public GroupID getGroupIDFrom() {
    return groupIDFrom;
  }

  /**
   * {@inheritDoc}
   */
  public Object getKey() {
    return getSourceNodeID();
  }

  /**
   * {@inheritDoc}
   */
  public ClientID getClientID() {
    return (ClientID) getSourceNodeID();
  }

  /**
   * {@inheritDoc}
   */
  public Set<String> getAttributes() {
    return this.attributes;
  }

  /**
   * {@inheritDoc}
   */
  public int getBatchSize() {
    return batchSize;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isPrefetchFirstBatch() {
    return prefetchFirstBatch;
  }

  /**
   * {@inheritDoc}
   */
  public List<NVPair> getSortAttributes() {
    return sortAttributes;
  }

  /**
   * {@inheritDoc}
   */
  public List<NVPair> getAggregators() {
    return aggregators;
  }

  /**
   * {@inheritDoc}
   */
  public boolean includeKeys() {
    return this.includeKeys;
  }

  /**
   * {@inheritDoc}
   */
  public boolean includeValues() {
    return this.includeValues;
  }

  /**
   * {@inheritDoc}
   */
  public int getMaxResults() {
    return this.maxResults;
  }

}
