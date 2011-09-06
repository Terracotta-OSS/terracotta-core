/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class LockStatisticsResponseMessageImpl extends DSOMessageBase implements LockStatisticsResponseMessage {

  private final static byte TYPE                                  = 1;
  private final static byte NUMBER_OF_LOCK_STAT_ELEMENTS          = 2;
  private final static byte TC_STACK_TRACE_ELEMENT                = 3;

  // message types
  private final static byte LOCK_STATISTICS_RESPONSE_MESSAGE_TYPE = 1;

  private int               type;
  private Collection        allTCStackTraceElements;

  public LockStatisticsResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                           MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public LockStatisticsResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                           TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(TYPE, this.type);
    put(allTCStackTraceElements);
  }

  private void put(Collection allTCStackTraceElementsCollection) {
    super.putNVPair(NUMBER_OF_LOCK_STAT_ELEMENTS, allTCStackTraceElementsCollection.size());
    for (Iterator i = allTCStackTraceElementsCollection.iterator(); i.hasNext();) {
      TCStackTraceElement lse = (TCStackTraceElement) i.next();
      putNVPair(TC_STACK_TRACE_ELEMENT, lse);
    }
  }

  private boolean isLockStatisticsResponseMessage() {
    return type == LOCK_STATISTICS_RESPONSE_MESSAGE_TYPE;
  }

  protected String describePayload() {
    StringBuffer rv = new StringBuffer();
    rv.append("Type : ");

    if (isLockStatisticsResponseMessage()) {
      rv.append("LOCK STATISTICS RESPONSE \n");
    } else {
      rv.append("UNKNOWN \n");
    }

    return rv.toString() + super.describePayload();
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case TYPE:
        this.type = getIntValue();
        return true;
      case NUMBER_OF_LOCK_STAT_ELEMENTS:
        int numOfStackTraces = getIntValue();
        this.allTCStackTraceElements = new ArrayList(numOfStackTraces);
        return true;
      case TC_STACK_TRACE_ELEMENT:
        TCStackTraceElement lse = new TCStackTraceElement();
        getObject(lse);
        this.allTCStackTraceElements.add(lse);
        return true;
      default:
        return false;
    }
  }

  public Collection getStackTraceElements() {
    return this.allTCStackTraceElements;
  }

  public void initialize(Collection allTCStackTraceElementsCollection) {
    this.allTCStackTraceElements = allTCStackTraceElementsCollection;
    this.type = LOCK_STATISTICS_RESPONSE_MESSAGE_TYPE;
  }

}
