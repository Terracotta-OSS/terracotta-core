/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.impl.DNAImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.VersionizedDNAWrapper;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockRequest;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnType;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author steve
 */
public class BroadcastTransactionMessageImpl extends DSOMessageBase implements BroadcastTransactionMessage {
  private final static byte      DNA_ID                = 1;
  private final static byte      LOCK_ID               = 2;
  private final static byte      CHANGE_ID             = 3;
  private final static byte      TRANSACTION_ID        = 4;
  private final static byte      COMMITTER_ID          = 5;
  private final static byte      TRANSACTION_TYPE_ID   = 6;
  private final static byte      GLOBAL_TRANSACTION_ID = 7;
  private final static byte      LOW_WATERMARK         = 8;
  private final static byte      SERIALIZER_ID         = 9;
  private final static byte      NOTIFIED              = 10;
  private final static byte      LOOKUP_OBJECT_IDS     = 11;
  private final static byte      ROOT_NAME_ID_PAIR     = 12;

  private List                   changes               = new LinkedList();
  private List                   lockIDs               = new LinkedList();
  private Set                    lookupObjectIDs       = new HashSet();
  private Collection             notifies              = new LinkedList();
  private Map                    newRoots              = new HashMap();

  private long                   changeID;
  private TransactionID          transactionID;
  private ChannelID              committerID;
  private TxnType                transactionType;
  private GlobalTransactionID    globalTransactionID;
  private GlobalTransactionID    lowWatermark;
  private ObjectStringSerializer serializer;

  public BroadcastTransactionMessageImpl(MessageMonitor monitor, TCByteBufferOutput out, MessageChannel channel,
                                         TCMessageType type) {
    super(monitor, out, channel, type);
  }

  public BroadcastTransactionMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                         TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(TRANSACTION_TYPE_ID, transactionType.getType());
    for (Iterator i = lockIDs.iterator(); i.hasNext();) {
      LockID lockID = (LockID) i.next();
      putNVPair(LOCK_ID, lockID.asString());
    }

    for (Iterator i = notifies.iterator(); i.hasNext();) {
      LockRequest notified = (LockRequest) i.next();
      putNVPair(NOTIFIED, notified);
    }

    putNVPair(SERIALIZER_ID, serializer);

    putNVPair(CHANGE_ID, changeID);
    putNVPair(TRANSACTION_ID, transactionID.toLong());
    putNVPair(COMMITTER_ID, committerID.toLong());
    putNVPair(GLOBAL_TRANSACTION_ID, globalTransactionID.toLong());
    putNVPair(LOW_WATERMARK, lowWatermark.toLong());

    for (Iterator i = changes.iterator(); i.hasNext();) {
      DNAImpl dna = (DNAImpl) i.next();
      putNVPair(DNA_ID, dna);
    }
    for (Iterator i = lookupObjectIDs.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      putNVPair(LOOKUP_OBJECT_IDS, oid.toLong());
    }
    for (Iterator i = newRoots.keySet().iterator(); i.hasNext();) {
      String key = (String) i.next();
      ObjectID value = (ObjectID) newRoots.get(key);
      putNVPair(ROOT_NAME_ID_PAIR, new RootIDPair(key, value));
    }
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case TRANSACTION_TYPE_ID:
        this.transactionType = TxnType.typeFor(getByteValue());
        return true;
      case DNA_ID:
        this.changes.add(getObject(new DNAImpl(serializer, false)));
        return true;
      case SERIALIZER_ID:
        this.serializer = (ObjectStringSerializer) getObject(new ObjectStringSerializer());
        return true;
      case LOCK_ID:
        this.lockIDs.add(new LockID(getStringValue()));
        return true;
      case NOTIFIED:
        this.notifies.add(this.getObject(new LockRequest()));
        return true;
      case CHANGE_ID:
        this.changeID = getLongValue();
        return true;
      case TRANSACTION_ID:
        this.transactionID = new TransactionID(getLongValue());
        return true;
      case COMMITTER_ID:
        this.committerID = new ChannelID(getLongValue());
        return true;
      case GLOBAL_TRANSACTION_ID:
        this.globalTransactionID = new GlobalTransactionID(getLongValue());
        return true;
      case LOW_WATERMARK:
        this.lowWatermark = new GlobalTransactionID(getLongValue());
        return true;
      case LOOKUP_OBJECT_IDS:
        this.lookupObjectIDs.add(new ObjectID(getLongValue()));
        return true;
      case ROOT_NAME_ID_PAIR:
        RootIDPair rootIDPair = (RootIDPair) getObject(new RootIDPair());
        this.newRoots.put(rootIDPair.getRootName(), rootIDPair.getRootID());
        return true;
      default:
        return false;
    }
  }

  public void initialize(List chges, Set objectIDs, ObjectStringSerializer aSerializer, LockID[] lids, long cid,
                         TransactionID txID, ChannelID commitID, GlobalTransactionID gtx, TxnType txnType,
                         GlobalTransactionID lowGlobalTransactionIDWatermark, Collection theNotifies, Map roots) {
    Assert.eval(lids.length > 0);
    Assert.assertNotNull(txnType);

    this.changes = chges;
    this.lockIDs = new LinkedList(Arrays.asList(lids));
    this.changeID = cid;
    this.transactionID = txID;
    this.committerID = commitID;
    this.transactionType = txnType;
    this.globalTransactionID = gtx;
    this.lowWatermark = lowGlobalTransactionIDWatermark;
    this.serializer = aSerializer;
    this.notifies.addAll(theNotifies);
    this.lookupObjectIDs.addAll(objectIDs);
    this.newRoots.putAll(roots);
  }

  public LockID[] getLockIDs() {
    return (LockID[]) lockIDs.toArray(new LockID[lockIDs.size()]);
  }

  public TxnType getTransactionType() {
    return transactionType;
  }

  public Collection getObjectChanges() {
    Collection versionizedChanges = new ArrayList(changes.size());
    for (Iterator iter = changes.iterator(); iter.hasNext();) {
      versionizedChanges.add(new VersionizedDNAWrapper((DNA) iter.next(), globalTransactionID.toLong()));

    }
    return versionizedChanges;
  }

  public Set getLookupObjectIDs() {
    return lookupObjectIDs;
  }

  public long getChangeID() {
    return changeID;
  }

  public TransactionID getTransactionID() {
    return transactionID;
  }

  public ChannelID getCommitterID() {
    return committerID;
  }

  public GlobalTransactionID getGlobalTransactionID() {
    return this.globalTransactionID;
  }

  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    Assert.assertNotNull(this.lowWatermark);
    return this.lowWatermark;
  }

  public Collection addNotifiesTo(List c) {
    c.addAll(notifies);
    return c;
  }

  public void doRecycleOnRead() {
    // dont recycle yet
  }

  protected boolean isOutputStreamRecycled() {
    return true;
  }

  public void doRecycleOnWrite() {
    // recycle only those buffers created for this message
    recycleOutputStream();
  }

  public Map getNewRoots() {
    return this.newRoots;
  }

  private static class RootIDPair implements TCSerializable {
    private String   rootName;
    private ObjectID rootID;

    public RootIDPair() {
      super();
    }

    public RootIDPair(String rootName, ObjectID rootID) {
      this.rootName = rootName;
      this.rootID = rootID;
    }

    public void serializeTo(TCByteBufferOutput serialOutput) {
      serialOutput.writeString(rootName);
      serialOutput.writeLong(rootID.toLong());

    }

    public Object deserializeFrom(TCByteBufferInputStream serialInput) throws IOException {
      this.rootName = serialInput.readString();
      this.rootID = new ObjectID(serialInput.readLong());
      return this;
    }

    public ObjectID getRootID() {
      return rootID;
    }

    public String getRootName() {
      return rootName;
    }
  }
}
