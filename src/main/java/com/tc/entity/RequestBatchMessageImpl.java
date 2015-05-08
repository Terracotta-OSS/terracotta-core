package com.tc.entity;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.EntityDescriptor;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * @author twu
 */
public class RequestBatchMessageImpl extends DSOMessageBase implements RequestBatchMessage {
  private static final byte LIST_NAME = 0;
  
  private List<Request> requests;
  
  public RequestBatchMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public RequestBatchMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(LIST_NAME, requests.size());
    TCByteBufferOutputStream outputStream = getOutputStream();
    for (Request request : requests) {
      outputStream.writeLong(((ClientID) request.getSource()).toLong()); // TODO: what if it's not a client?
      outputStream.writeLong(request.getTransactionID().toLong());
      request.getEntityDescriptor().serializeTo(outputStream);

      long acks = 0;
      for (Request.Acks ack : request.getAcks()) {
        acks |= 1L << ack.ordinal();
      }
      outputStream.writeLong(acks);
      
      outputStream.writeInt(request.getType().ordinal());
      // TODO: should there be an equivalent for getBytesValue() here?
      outputStream.writeInt(request.getPayload().length);
      outputStream.write(request.getPayload());
    }
  }

  @Override
  public void setRequestBatch(List<Request> requestBatch) {
    requests = requestBatch;
  }

  @Override
  public List<Request> getRequests() {
    return requests;
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    if (name != LIST_NAME) {
      return false;
    } else {
      int batchSize = getIntValue();
      requests = new ArrayList<>(batchSize);

      for (int i = 0; i < batchSize; i++) {
        ClientID clientID = new ClientID(getLongValue());
        TransactionID transactionID = new TransactionID(getLongValue());
        EntityDescriptor entityDescriptor = EntityDescriptor.readFrom(getInputStream());
        
        Set<Request.Acks> acks = EnumSet.noneOf(Request.Acks.class);
        long requestedAcks = getLongValue();
        for (int j = 0; j < Request.Acks.values().length; j++) {
          if ((requestedAcks & (1L << j)) != 0) {
            acks.add(Request.Acks.values()[j]);
          }
        }

        Request.Type type = Request.Type.values()[getIntValue()];
        byte[] payload = getBytesArray();

        requests.add(new SimpleRequest(clientID, transactionID, entityDescriptor, acks, type, payload));
      }
      return true;
    }
  }
  
  private static class SimpleRequest implements Request {
    private final ClientID clientID;
    private final TransactionID transactionID;
    private final EntityDescriptor entityDescriptor;
    private final Set<Acks> acks;
    private final Type type;
    private final byte[] payload;

    private SimpleRequest(ClientID clientID, TransactionID transactionID, EntityDescriptor entityDescriptor, Set<Acks> acks, Type type, byte[] payload) {
      this.clientID = clientID;
      this.transactionID = transactionID;
      this.entityDescriptor = entityDescriptor;
      this.acks = acks;
      this.type = type;
      this.payload = payload;
    }

    @Override
    public NodeID getSource() {
      return clientID;
    }

    @Override
    public TransactionID getTransactionID() {
      return transactionID;
    }

    @Override
    public EntityDescriptor getEntityDescriptor() {
      return this.entityDescriptor;
    }

    @Override
    public Set<Acks> getAcks() {
      return acks;
    }

    @Override
    public Type getType() {
      return type;
    }

    @Override
    public byte[] getPayload() {
      return payload;
    }
  }
}
