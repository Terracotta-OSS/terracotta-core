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
import com.tc.object.metadata.NVPairSerializer;
import com.tc.object.session.SessionID;
import com.terracottatech.search.AbstractNVPair;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.StackOperations;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Nabib El-Rahman
 */
public class SearchQueryRequestMessageImpl extends DSOMessageBase implements SearchQueryRequestMessage {

  private static final NVPairSerializer       NVPAIR_SERIALIZER      = new NVPairSerializer();
  private static final ObjectStringSerializer NULL_SERIALIZER        = new NullObjectStringSerializer();

  private static final byte                   SEARCH_REQUEST_ID      = 0;
  private static final byte                   GROUP_ID_FROM          = 1;
  private static final byte                   CACHE_NAME             = 2;
  private static final byte                   INCLUDE_KEYS           = 3;
  private static final byte                   ATTRIBUTES             = 4;
  private static final byte                   GROUP_BY_ATTRIBUTES    = 5;
  private static final byte                   SORT_ATTRIBUTES        = 6;
  private static final byte                   AGGREGATORS            = 7;
  private static final byte                   STACK_OPERATION_MARKER = 8;
  private static final byte                   STACK_NVPAIR_MARKER    = 9;
  private static final byte                   MAX_RESULTS            = 10;
  private static final byte                   INCLUDE_VALUES         = 11;
  private static final byte                   BATCH_SIZE             = 12;
  private static final byte                   PREFETCH_FIRST_BATCH   = 13;

  private SearchRequestID                     requestID;
  private GroupID                             groupIDFrom;
  private String                              cacheName;
  private List                                queryStack;
  private boolean                             includeKeys;
  private boolean                             includeValues;
  private Set<String>                         attributes;
  private Set<String>                         groupByAttributes;
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
                                             List stack, boolean keys, boolean values, Set<String> attributeSet,
                                             Set<String> groupByAttrs, List<NVPair> sortAttributesMap,
                                             List<NVPair> attributeAggregators, int max, int batch,
                                             boolean prefetchFirst) {
    this.requestID = searchRequestID;
    this.groupIDFrom = groupID;
    this.cacheName = cache;
    this.queryStack = stack;
    this.includeKeys = keys;
    this.includeValues = values;
    this.attributes = attributeSet;
    this.groupByAttributes = groupByAttrs;
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

    putNVPair(GROUP_BY_ATTRIBUTES, this.groupByAttributes.size());
    for (final String attribute : this.groupByAttributes) {
      outStream.writeString(attribute);
    }

    putNVPair(SORT_ATTRIBUTES, this.sortAttributes.size());
    for (final NVPair sortedAttributes : this.sortAttributes) {
      NVPAIR_SERIALIZER.serialize(sortedAttributes, outStream, NULL_SERIALIZER);
    }

    putNVPair(AGGREGATORS, this.aggregators.size());
    for (final NVPair attributeAggregator : this.aggregators) {
      NVPAIR_SERIALIZER.serialize(attributeAggregator, outStream, NULL_SERIALIZER);
    }

    if (!queryStack.isEmpty()) {
      for (Object obj : queryStack) {
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

      case GROUP_BY_ATTRIBUTES:
        this.groupByAttributes = new HashSet<String>();
        count = getIntValue();
        // Directly decode the key
        while (count-- > 0) {
          String attribute = getStringValue();
          this.groupByAttributes.add(attribute);
        }
        return true;

      case SORT_ATTRIBUTES:
        this.sortAttributes = new LinkedList();
        int sortCount = getIntValue();

        while (sortCount-- > 0) {
          NVPair pair = NVPAIR_SERIALIZER.deserialize(inputStream, NULL_SERIALIZER);
          this.sortAttributes.add(pair);
        }
        return true;

      case AGGREGATORS:
        this.aggregators = new LinkedList();
        int attributeAggregatorCount = getIntValue();

        while (attributeAggregatorCount-- > 0) {
          NVPair pair = NVPAIR_SERIALIZER.deserialize(inputStream, NULL_SERIALIZER);
          this.aggregators.add(pair);
        }
        return true;

      case STACK_OPERATION_MARKER:
        StackOperations operation = StackOperations.valueOf(getStringValue());
        queryStack.add(operation);
        return true;

      case STACK_NVPAIR_MARKER:
        NVPair pair = NVPAIR_SERIALIZER.deserialize(inputStream, NULL_SERIALIZER);
        queryStack.add(pair);
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
  public List getQueryStack() {
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

  @Override
  public Set<String> getGroupByAttributes() {
    return groupByAttributes;
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
