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
import com.tc.object.session.SessionID;
import com.tc.search.SearchRequestID;
import com.terracottatech.search.IndexQueryResult;
import com.terracottatech.search.aggregator.Aggregator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SearchResultsResponseMessageImpl extends DSOMessageBase implements SearchResponseMessage {
  private static final byte      SEARCH_REQUEST_ID = 0;
  private static final byte      GROUP_ID_FROM     = 1;
  private static final byte      ERROR_MESSAGE     = 2;
  private static final byte      RESULTS_SIZE            = 3;

  private SearchRequestID        requestID;
  private GroupID                groupIDFrom;
  private List<IndexQueryResult> results;
  private String                 errorMessage;

  public SearchResultsResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                         MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public SearchResultsResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                         TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  public SearchRequestID getRequestID() {
    return requestID;
  }

  @Override
  public GroupID getGroupIDFrom() {
    return groupIDFrom;
  }

  @Override
  public void initSearchResponseMessage(SearchRequestID searchRequestID, GroupID groupID, List<IndexQueryResult> res,
                                        List<Aggregator> aggregators, boolean anyCriteriaMatched, boolean isGroupBy,
                                        long totalCount) {
    this.requestID = searchRequestID;
    this.groupIDFrom = groupID;
    this.results = res;
  }

  @Override
  public void initSearchResponseMessage(SearchRequestID searchRequestID, GroupID groupID, String error) {
    this.requestID = searchRequestID;
    this.groupIDFrom = groupID;
    this.errorMessage = error;
  }

  @Override
  public List<IndexQueryResult> getResults() {
    return results;
  }

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public boolean isError() {
    return errorMessage != null;
  }

  @Override
  protected void dehydrateValues() {
    final TCByteBufferOutputStream outStream = getOutputStream();

    putNVPair(SEARCH_REQUEST_ID, this.requestID.toLong());
    putNVPair(GROUP_ID_FROM, this.groupIDFrom.toInt());

    if (results != null) {
      putNVPair(RESULTS_SIZE, this.results.size());
      IndexQueryResultSerializer<IndexQueryResult> writer = IndexQueryResultSerializer.getInstance(false); // grouped results can't be used here

      for (IndexQueryResult result : this.results) {
        writer.serialize(result, outStream);
      }
    }

    if (errorMessage != null) {
      putNVPair(ERROR_MESSAGE, errorMessage);
    }

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
        this.results = new ArrayList<IndexQueryResult>(size);
        IndexQueryResultSerializer<IndexQueryResult> reader = IndexQueryResultSerializer.getInstance(false);

        while (size-- > 0) {

          IndexQueryResult result = reader.deserializeFrom(input);
          this.results.add(result);
        }
        return true;

      case ERROR_MESSAGE:
        this.errorMessage = input.readString();
        return true;
      default:
        return false;
    }
  }

}
