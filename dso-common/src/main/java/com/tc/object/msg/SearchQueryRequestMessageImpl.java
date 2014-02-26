/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.dna.impl.NullObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.metadata.NVPairSerializer;
import com.tc.object.session.SessionID;
import com.tc.search.SearchRequestID;
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

  private SearchRequestID                     requestID;
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
  private int                                 resultPageSize;

  public SearchQueryRequestMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                       MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public SearchQueryRequestMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                       TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  public void initializeSearchRequestMessage(SearchRequestID searchRequestID, String cache,
                                             List stack, boolean keys, boolean values, Set<String> attributeSet,
                                             Set<String> groupByAttrs, List<NVPair> sortAttributesMap,
                                             List<NVPair> attributeAggregators, int max, int batch,
 boolean prefetchFirst, int resultPrefetchSize) {
    this.requestID = searchRequestID;
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
    this.resultPageSize = resultPrefetchSize;
  }

  @Override
  protected void dehydrateValues() {
    final TCByteBufferOutputStream outStream = getOutputStream();

    putNVPair(SEARCH_REQUEST_ID, this.requestID.toLong());
    putNVPair(CACHE_NAME, this.cacheName);
    putNVPair(INCLUDE_KEYS, this.includeKeys);
    putNVPair(INCLUDE_VALUES, this.includeValues);
    putNVPair(MAX_RESULTS, this.maxResults);
    putNVPair(VALUE_PREFETCH_SIZE, this.batchSize);
    putNVPair(PREFETCH_VALUES, this.prefetchFirstBatch);
    putNVPair(RESULT_PAGE_SIZE, this.resultPageSize);

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

      case VALUE_PREFETCH_SIZE:
        this.batchSize = getIntValue();
        return true;

      case PREFETCH_VALUES:
        this.prefetchFirstBatch = getBooleanValue();
        return true;

      case RESULT_PAGE_SIZE:
        this.resultPageSize = getIntValue();
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
  @Override
  public String getCacheName() {
    return this.cacheName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List getQueryStack() {
    return this.queryStack;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SearchRequestID getRequestID() {
    return requestID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ClientID getClientID() {
    return (ClientID) getSourceNodeID();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getAttributes() {
    return this.attributes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getValuePrefetchSize() {
    return batchSize;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPrefetchFirstBatch() {
    return prefetchFirstBatch;
  }

  /**
   * {@inheritDoc}
   */
  @Override
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
  @Override
  public List<NVPair> getAggregators() {
    return aggregators;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean includeKeys() {
    return this.includeKeys;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean includeValues() {
    return this.includeValues;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getMaxResults() {
    return this.maxResults;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getResultPageSize() {
    return this.resultPageSize;
  }

}
