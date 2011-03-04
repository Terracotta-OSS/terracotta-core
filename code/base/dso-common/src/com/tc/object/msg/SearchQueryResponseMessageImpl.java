/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.GroupID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.SearchRequestID;
import com.tc.object.session.SessionID;
import com.tc.search.IndexQueryResult;
import com.tc.search.aggregator.AbstractAggregator;
import com.tc.search.aggregator.Aggregator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 
 */
public class SearchQueryResponseMessageImpl extends DSOMessageBase implements SearchQueryResponseMessage {

  private final static byte      SEARCH_REQUEST_ID       = 0;
  private final static byte      GROUP_ID_FROM           = 1;
  private final static byte      RESULTS_SIZE            = 2;
  private final static byte      AGGREGATOR_RESULTS_SIZE = 3;
  private final static byte      ERROR_MESSAGE           = 4;
  private final static byte      IS_ERROR                = 5;
  private final static byte      ANY_CRITERIA_MATCHED    = 6;

  private SearchRequestID        requestID;
  private GroupID                groupIDFrom;
  private List<IndexQueryResult> results;
  private List<Aggregator>       aggregators;
  private String                 errorMessage;
  private boolean                isError;
  private boolean                anyCriteriaMatched;

  public SearchQueryResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                        MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public SearchQueryResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                        TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  /**
   * {@inheritDoc}
   */
  public void initSearchResponseMessage(SearchRequestID searchRequestID, GroupID groupID,
                                        List<IndexQueryResult> searchResults, List<Aggregator> aggregatorList,
                                        boolean criteriaMatched) {
    this.isError = false;
    this.requestID = searchRequestID;
    this.groupIDFrom = groupID;
    this.results = searchResults;
    this.aggregators = aggregatorList;
    this.anyCriteriaMatched = criteriaMatched;
  }

  /**
   * {@inheritDoc}
   */
  public void initSearchResponseMessage(SearchRequestID searchRequestID, GroupID groupID, String errMsg) {
    this.isError = true;
    this.requestID = searchRequestID;
    this.groupIDFrom = groupID;
    this.errorMessage = errMsg;
  }

  /**
   * {@inheritDoc}
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  public boolean isError() {
    return isError;
  }

  public boolean isAnyCriteriaMatched() {
    return anyCriteriaMatched;
  }

  /**
   * {@inheritDoc}
   */
  public SearchRequestID getRequestID() {
    return this.requestID;
  }

  /**
   * {@inheritDoc}
   */
  public GroupID getGroupIDFrom() {
    return this.groupIDFrom;
  }

  /**
   * {@inheritDoc}
   */
  public List<IndexQueryResult> getResults() {
    return this.results;
  }

  /**
   * {@inheritDoc}
   */
  public List<Aggregator> getAggregators() {
    return aggregators;
  }

  @Override
  protected void dehydrateValues() {
    final TCByteBufferOutputStream outStream = getOutputStream();

    putNVPair(SEARCH_REQUEST_ID, this.requestID.toLong());
    putNVPair(GROUP_ID_FROM, this.groupIDFrom.toInt());

    if (results != null) {
      putNVPair(RESULTS_SIZE, this.results.size());

      for (IndexQueryResult result : this.results) {
        result.serializeTo(outStream);
      }
    }

    if (aggregators != null) {
      putNVPair(AGGREGATOR_RESULTS_SIZE, this.aggregators.size());

      for (Aggregator result : this.aggregators) {
        result.serializeTo(outStream);
      }
    }

    if (errorMessage != null) {
      putNVPair(ERROR_MESSAGE, errorMessage);
    }

    putNVPair(IS_ERROR, isError);
    putNVPair(ANY_CRITERIA_MATCHED, anyCriteriaMatched);
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    TCByteBufferInput input = getInputStream();

    switch (name) {
      case SEARCH_REQUEST_ID:
        this.requestID = new SearchRequestID(getLongValue());
        return true;

      case GROUP_ID_FROM:
        this.groupIDFrom = new GroupID(getIntValue());
        return true;

      case RESULTS_SIZE:
        int size = getIntValue();
        this.results = new ArrayList(size);
        while (size-- > 0) {
          IndexQueryResult result = new IndexQueryResultImpl();
          result.deserializeFrom(input);
          this.results.add(result);
        }
        return true;

      case AGGREGATOR_RESULTS_SIZE:
        int aggregatorSize = getIntValue();
        this.aggregators = new ArrayList(aggregatorSize);
        while (aggregatorSize-- > 0) {
          Aggregator aggregator = AbstractAggregator.deserializeInstance(input);
          this.aggregators.add(aggregator);
        }
        return true;
      case ERROR_MESSAGE:
        this.errorMessage = input.readString();
        return true;
      case IS_ERROR:
        this.isError = input.readBoolean();
        return true;
      case ANY_CRITERIA_MATCHED:
        this.anyCriteriaMatched = input.readBoolean();
        return true;
      default:
        return false;
    }
  }

}
