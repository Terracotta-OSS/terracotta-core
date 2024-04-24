/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.l2.dup;

import com.tc.io.InputWrapper;
import com.tc.io.OutputWrapper;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.l2.msg.IBatchableGroupMessage;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class RelayMessage extends AbstractGroupMessage implements IBatchableGroupMessage<ReplicationMessage> {

  public static final int        START_SYNC       = 0x01;
  public static final int        RELAY_BATCH       = 0x02;
  public static final int        RELAY_RESUME      = 0x04;
  public static final int        RELAY_INVALID      = 0x08;
  public static final int        RELAY_SUCCESS      = 0x10;

  private Collection<ReplicationMessage> payloadMessages;
  private long          lastSeen;

  // To make serialization happy
  public RelayMessage() {
    super(-1);
  }

  RelayMessage(int type) {
    super(type);
    if (type == RELAY_BATCH) {
      payloadMessages = new ArrayList<>();
    }
  }
  
  RelayMessage(long lastSeen) {
    super(RELAY_RESUME);
    this.lastSeen = lastSeen;
  }
  
  public long getLastSeen() {
    return lastSeen;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    switch (getType()) {
      case START_SYNC:
      case RELAY_INVALID:
        break;
      case RELAY_BATCH:
        loadReplicationBatch(in);
        break;
      case RELAY_RESUME:
        lastSeen = in.readLong();
        break;
    }
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    switch (getType()) {
      case START_SYNC:
      case RELAY_INVALID:
        break;
      case RELAY_BATCH:
        createReplicationBatch(out);
        break;
      case RELAY_RESUME:
        out.writeLong(lastSeen);
        break;
    }
  }
  
  public static AbstractGroupMessage createStartSync() {
    return new RelayMessage(START_SYNC);
  }
  
  public static RelayMessage createRelayBatch() {
    return new RelayMessage(RELAY_BATCH);
  }
  
  public static RelayMessage createInvalid() {
    return new RelayMessage(RELAY_INVALID);
  }
  
  public static RelayMessage createSuccess() {
    return new RelayMessage(RELAY_SUCCESS);
  }
  
  public static AbstractGroupMessage createResumeMessage(long lastSeen) {
    return new RelayMessage(lastSeen);
  }
  
  private void createReplicationBatch(TCByteBufferOutput output) {
    try (GZIPOutputStream compress = new GZIPOutputStream(new OutputWrapper(output));) {
      try (TCByteBufferOutputStream out = new TCByteBufferOutputStream()) {
        for (ReplicationMessage added : payloadMessages) {
          added.serializeTo(out);
        }
        try (TCByteBufferInputStream in = new TCByteBufferInputStream(out.accessBuffers())) {
          transfer(in, compress);
        }
      }
    } catch (IOException io) {
      throw new RuntimeException(io);
    }
  }
  
  public long unwindBatch(Consumer<ReplicationMessage> next) {
    return payloadMessages.stream().peek(next).map(ReplicationMessage::getSequenceID).reduce(Long::max).orElse(Long.MIN_VALUE);
  }
  
  private void loadReplicationBatch(TCByteBufferInput source) {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();
    try (GZIPInputStream decompress = new GZIPInputStream(new InputWrapper(source))) {
      transfer(decompress, output);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
    output.close();
    payloadMessages = new ArrayList<>();
    try (TCByteBufferInputStream input = new TCByteBufferInputStream(output.accessBuffers())) {
      while (input.available() > 0) {
        ReplicationMessage msg = new ReplicationMessage();
        payloadMessages.add(msg);
        try {
          msg.deserializeFrom(input);
          msg.setMessageOrginator(ServerID.NULL_ID);
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }
      }
    }
  }
  
  private static long transfer(InputStream in, OutputStream out) throws IOException {
    long transferred = 0;
    byte[] buffer = new byte[1024];
    int read;
    while ((read = in.read(buffer, 0, buffer.length)) >= 0) {
        out.write(buffer, 0, read);
        transferred += read;
    }
    return transferred;
  }
  
  @Override
  public void addToBatch(ReplicationMessage element) {
    payloadMessages.add(element);
  }

  @Override
  public int getBatchSize() {
    return payloadMessages.size();
  }

  @Override
  public long getPayloadSize() {
    return payloadMessages.stream().map(ReplicationMessage::getPayloadSize).reduce(Long::sum).orElse(0L);
  }

  @Override
  public void setSequenceID(long rid) {

  }

  @Override
  public long getSequenceID() {
    return payloadMessages.stream().findFirst().map(ReplicationMessage::getSequenceID).orElse(0L);
  }

  @Override
  public AbstractGroupMessage asAbstractGroupMessage() {
    return this;
  }
}