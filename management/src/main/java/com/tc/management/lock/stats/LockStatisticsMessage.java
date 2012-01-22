/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

public class LockStatisticsMessage extends DSOMessageBase {

  private final static byte TYPE                                   = 1;
  private final static byte TRACE_DEPTH                            = 2;
  private final static byte GATHER_INTERVAL                        = 3;

  // message types
  private final static byte LOCK_STATISTICS_ENABLE_MESSAGE_TYPE    = 1;
  private final static byte LOCK_STATISTICS_DISABLE_MESSAGE_TYPE   = 2;
  private final static byte LOCK_STATISTICS_GATHERING_MESSAGE_TYPE = 3;

  private int               type;
  private int               traceDepth;
  private int               gatherInterval;

  public LockStatisticsMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                               MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public LockStatisticsMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                               TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(TYPE, this.type);
    if (isLockStatsEnableDisable()) {
      putNVPair(TRACE_DEPTH, traceDepth);
      putNVPair(GATHER_INTERVAL, gatherInterval);
    }
  }

  public boolean isEnableLockStats() {
    return type == LOCK_STATISTICS_ENABLE_MESSAGE_TYPE;
  }

  public boolean isDisableLockStats() {
    return type == LOCK_STATISTICS_DISABLE_MESSAGE_TYPE;
  }
  
  public boolean isLockStatsEnableDisable() {
    return type == LOCK_STATISTICS_ENABLE_MESSAGE_TYPE || type == LOCK_STATISTICS_DISABLE_MESSAGE_TYPE;
  }
  
  public boolean isLockStatsEnable() {
    return type == LOCK_STATISTICS_ENABLE_MESSAGE_TYPE;
  }
  
  public boolean isLockStatsDisable() {
    return type == LOCK_STATISTICS_DISABLE_MESSAGE_TYPE;
  }
  
  public boolean isGatherLockStatistics() {
    return type == LOCK_STATISTICS_GATHERING_MESSAGE_TYPE;
  }

  protected String describePayload() {
    StringBuffer rv = new StringBuffer();
    rv.append("Type : ");

    if (isEnableLockStats()) {
      rv.append("LOCK STATISTICS ENABLED \n");
    } else if (isDisableLockStats()) {
      rv.append("LOCK STATISTICS DISABLED \n");
    } else if (isGatherLockStatistics()) {
      rv.append("LOCK STATISTICS GATHERING \n");
    } else {
      rv.append("UNKNOWN \n");
    }

    rv.append(traceDepth).append(' ').append(gatherInterval).append(' ').append("Lock Type: ").append('\n');

    return rv.toString();
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case TYPE:
        this.type = getIntValue();
        return true;
      case TRACE_DEPTH:
        this.traceDepth = getIntValue();
        return true;
      case GATHER_INTERVAL:
        this.gatherInterval = getIntValue();
        return true;
      default:
        return false;
    }
  }

  public int getTraceDepth() {
    return this.traceDepth;
  }

  public int getGatherInterval() {
    return this.gatherInterval;
  }

  public void initializeEnableStat(int traceDepthArg, int gatherIntervalArg) {
    this.traceDepth = traceDepthArg;
    this.gatherInterval = gatherIntervalArg;
    this.type = LOCK_STATISTICS_ENABLE_MESSAGE_TYPE;
  }

  public void initializeDisableStat() {
    this.traceDepth = 0;
    this.gatherInterval = 0;
    this.type = LOCK_STATISTICS_DISABLE_MESSAGE_TYPE;
  }
  
  public void initializeLockStatisticsGathering() {
    this.type = LOCK_STATISTICS_GATHERING_MESSAGE_TYPE;
  }

}
