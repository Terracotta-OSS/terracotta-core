/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCSerializable;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.dna.impl.DNAImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.dna.impl.VersionizedDNAWrapper;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnType;
import com.tc.server.ServerEvent;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author steve
 */
public class BroadcastTransactionMessageImpl extends DSOMessageBase implements BroadcastTransactionMessage {

  private final static byte DNA_ID = 1;
  private final static byte LOCK_ID = 2;
  private final static byte CHANGE_ID = 3;
  private final static byte TRANSACTION_ID = 4;
  private final static byte COMMITTER_ID = 5;
  private final static byte TRANSACTION_TYPE_ID = 6;
  private final static byte GLOBAL_TRANSACTION_ID = 7;
  private final static byte LOW_WATERMARK = 8;
  private final static byte SERIALIZER_ID = 9;
  private final static byte NOTIFIED = 10;
  private final static byte ROOT_NAME_ID_PAIR = 11;
  private final static byte LOGICAL_CHANGE_RESULT = 13;
  private final static byte SERVER_EVENT = 14;

  private long changeID;
  private TransactionID transactionID;
  private NodeID committerID;
  private TxnType transactionType;
  private GlobalTransactionID globalTransactionID;
  private GlobalTransactionID lowWatermark;
  private ObjectStringSerializer serializer;

  private final List<DNA>                                 changes               = new ArrayList<DNA>();
  private final Collection<ClientServerExchangeLockContext> notifies            = new ArrayList<ClientServerExchangeLockContext>();
  private final Map<String, ObjectID>                     newRoots              = new HashMap<String, ObjectID>();
  private final List<LockID>                              lockIDs               = new ArrayList<LockID>();
  private final Map<LogicalChangeID, LogicalChangeResult> logicalChangeResults  = new HashMap<LogicalChangeID, LogicalChangeResult>();
  private final List<ServerEvent>                         serverEvents                = new ArrayList<ServerEvent>();

  public BroadcastTransactionMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                         final TCByteBufferOutputStream out, final MessageChannel channel,
                                         final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public BroadcastTransactionMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                         final MessageChannel channel, final TCMessageHeader header,
                                         final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(TRANSACTION_TYPE_ID, this.transactionType.getType());
    for (LockID lockID : this.lockIDs) {
      putNVPair(LOCK_ID, lockID);
    }
    for (ClientServerExchangeLockContext notified : this.notifies) {
      putNVPair(NOTIFIED, notified);
    }

    putNVPair(SERIALIZER_ID, this.serializer);
    putNVPair(CHANGE_ID, this.changeID);
    putNVPair(TRANSACTION_ID, this.transactionID.toLong());
    putNVPair(COMMITTER_ID, this.committerID);
    putNVPair(GLOBAL_TRANSACTION_ID, this.globalTransactionID.toLong());
    putNVPair(LOW_WATERMARK, this.lowWatermark.toLong());

    for (DNA change : this.changes) {
      DNAImpl dna = (DNAImpl) change; // XXX: this cast isn't great. DNA should probably be TCSerializable!
      putNVPair(DNA_ID, dna);
    }
    
    for (Map.Entry<String, ObjectID> root : this.newRoots.entrySet()) {
      putNVPair(ROOT_NAME_ID_PAIR, new RootIDPair(root.getKey(), root.getValue()));
    }
    
    for (final Entry<LogicalChangeID, LogicalChangeResult> entry : logicalChangeResults.entrySet()) {
      putNVPair(LOGICAL_CHANGE_RESULT, new LogicalChangeResultPair(entry.getKey(), entry.getValue()));
    }
    
    for (final ServerEvent event : serverEvents) {
      putNVPair(SERVER_EVENT, new ServerEventSerializableContext(event));
    }
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case TRANSACTION_TYPE_ID:
        this.transactionType = TxnType.typeFor(getByteValue());
        return true;
      case DNA_ID:
        DNA dna = getObject(new DNAImpl(this.serializer, false));
        this.changes.add(dna);
        return true;
      case SERIALIZER_ID:
        this.serializer = getObject(new ObjectStringSerializerImpl());
        return true;
      case LOCK_ID:
        this.lockIDs.add(getLockIDValue());
        return true;
      case NOTIFIED:
        ClientServerExchangeLockContext cselc = getObject(new ClientServerExchangeLockContext());
        this.notifies.add(cselc);
        return true;
      case CHANGE_ID:
        this.changeID = getLongValue();
        return true;
      case TRANSACTION_ID:
        this.transactionID = new TransactionID(getLongValue());
        return true;
      case COMMITTER_ID:
        this.committerID = getNodeIDValue();
        return true;
      case GLOBAL_TRANSACTION_ID:
        this.globalTransactionID = new GlobalTransactionID(getLongValue());
        return true;
      case LOW_WATERMARK:
        this.lowWatermark = new GlobalTransactionID(getLongValue());
        return true;
      case ROOT_NAME_ID_PAIR:
        RootIDPair rootIDPair = getObject(new RootIDPair());
        this.newRoots.put(rootIDPair.getRootName(), rootIDPair.getRootID());
        return true;
      case LOGICAL_CHANGE_RESULT:
        LogicalChangeResultPair resultPair = getObject(new LogicalChangeResultPair());
        this.logicalChangeResults.put(resultPair.getId(), resultPair.getResult());
        return true;
      case SERVER_EVENT:
        final ServerEventSerializableContext ctx = getObject(new ServerEventSerializableContext());
        serverEvents.add(ctx.getEvent());
        return true;
      default:
        return false;
    }
  }

  @Override
  public void initialize(final List<? extends DNA> chges, final ObjectStringSerializer aSerializer, final LockID[] lids,
                         final long cid, final TransactionID txID, final NodeID client, final GlobalTransactionID gtx,
                         final TxnType txnType, final GlobalTransactionID lowGlobalTransactionIDWatermark,
                         final Collection<ClientServerExchangeLockContext> theNotifies, final Map<String, ObjectID> roots,
                         final Map<LogicalChangeID, LogicalChangeResult> logicalInvokeResults,
                         final Collection<ServerEvent> events) {
    Assert.assertNotNull(txnType);

    this.changes.addAll(chges);
    Collections.addAll(this.lockIDs, lids);
    this.changeID = cid;
    this.transactionID = txID;
    this.committerID = client;
    this.transactionType = txnType;
    this.globalTransactionID = gtx;
    this.lowWatermark = lowGlobalTransactionIDWatermark;
    this.serializer = aSerializer;
    this.notifies.addAll(theNotifies);
    this.newRoots.putAll(roots);
    this.logicalChangeResults.putAll(logicalInvokeResults);
    this.serverEvents.addAll(events);
  }

  @Override
  public List<LockID> getLockIDs() {
    return this.lockIDs;
  }

  @Override
  public TxnType getTransactionType() {
    return this.transactionType;
  }

  @Override
  public Collection<DNA> getObjectChanges() {
    final Collection<DNA> versionizedChanges = new ArrayList<DNA>(this.changes.size());
    for (final Object change : this.changes) {
      versionizedChanges.add(new VersionizedDNAWrapper((DNA) change, this.globalTransactionID.toLong()));
    }
    return versionizedChanges;
  }

  @Override
  public long getChangeID() {
    return this.changeID;
  }

  @Override
  public TransactionID getTransactionID() {
    return this.transactionID;
  }

  @Override
  public NodeID getCommitterID() {
    return this.committerID;
  }

  @Override
  public GlobalTransactionID getGlobalTransactionID() {
    return this.globalTransactionID;
  }

  @Override
  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    Assert.assertNotNull(this.lowWatermark);
    return this.lowWatermark;
  }

  @Override
  public Collection<ClientServerExchangeLockContext> getNotifies() {
    return new ArrayList<ClientServerExchangeLockContext>(this.notifies);
  }

  @Override
  public void doRecycleOnRead() {
    // dont recycle yet
  }

  @Override
  protected boolean isOutputStreamRecycled() {
    return true;
  }

  @Override
  public void doRecycleOnWrite() {
    // recycle only those buffers created for this message
    recycleOutputStream();
  }

  @Override
  public Map<String, ObjectID> getNewRoots() {
    return this.newRoots;
  }

  private static class RootIDPair implements TCSerializable<RootIDPair> {
    private String rootName;
    private ObjectID rootID;

    public RootIDPair() {
      
    }

    public RootIDPair(final String rootName, final ObjectID rootID) {
      this.rootName = rootName;
      this.rootID = rootID;
    }

    @Override
    public void serializeTo(final TCByteBufferOutput serialOutput) {
      serialOutput.writeString(this.rootName);
      serialOutput.writeLong(this.rootID.toLong());

    }

    @Override
    public RootIDPair deserializeFrom(final TCByteBufferInput serialInput) throws IOException {
      this.rootName = serialInput.readString();
      this.rootID = new ObjectID(serialInput.readLong());
      return this;
    }

    public ObjectID getRootID() {
      return this.rootID;
    }

    public String getRootName() {
      return this.rootName;
    }
  }

  private static class LogicalChangeResultPair implements TCSerializable<LogicalChangeResultPair> {
    private LogicalChangeID id;
    private LogicalChangeResult result;


    public LogicalChangeResultPair(LogicalChangeID id, LogicalChangeResult result) {
      this.id = id;
      this.result = result;
    }

    public LogicalChangeResultPair() {
    }

    @Override
    public void serializeTo(TCByteBufferOutput serialOutput) {
      serialOutput.writeLong(id.toLong());
      result.serializeTo(serialOutput);
    }

    @Override
    public LogicalChangeResultPair deserializeFrom(TCByteBufferInput serialInput) throws IOException {
      this.id = new LogicalChangeID(serialInput.readLong());
      this.result = new LogicalChangeResult().deserializeFrom(serialInput);
      return this;
    }

    public LogicalChangeID getId() {
      return id;
    }

    public LogicalChangeResult getResult() {
      return result;
    }

  }

  @Override
  public Map<LogicalChangeID, LogicalChangeResult> getLogicalChangeResults() {
    return logicalChangeResults;
  }

  @Override
  public List<ServerEvent> getEvents() {
    return serverEvents;
  }
}
