/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.exception.ImplementMe;
import com.tc.l2.api.L2Coordinator;
import com.tc.logging.TCLogger;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.StringLockID;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.msg.BroadcastTransactionMessage;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.object.session.SessionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.api.ServerMapRequestManager;
import com.tc.objectserver.api.ObjectManagerTest.TestDateDNA;
import com.tc.objectserver.clustermetadata.ServerClusterMetaDataManager;
import com.tc.objectserver.context.BroadcastChangeContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.locks.LockManager;
import com.tc.objectserver.locks.NotifiedWaiters;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionListener;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.objectserver.tx.TransactionBatchReaderFactory;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionLister;
import com.tc.stats.Stats;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.SampledCounterImpl;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.stats.counter.sampled.derived.SampledRateCounterImpl;
import com.tc.test.TCTestCase;
import com.tc.util.ObjectIDSet;
import com.tc.util.SequenceID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

public class BroadcastChangeHandlerTest extends TCTestCase {

  private BroadcastChangeHandler     handler;
  private ServerConfigurationContext serverCfgCxt;

  private final static int           SRC_CLIENT_ID       = 1;
  private final static int           NO_OF_CLIENTS       = 10;
  private final static int           DISCONNECTED_CLIENT = 5;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    SampledCounterImpl sci = new SampledCounterImpl(new SampledCounterConfig(5, 10, true, 0));
    SampledRateCounterImpl srci = new SampledRateCounterImpl(new SampledRateCounterConfig(5, 10, true));
    this.handler = new BroadcastChangeHandler(sci, new ObjectStatsRecorder(), srci);
    this.serverCfgCxt = new TestServerConfigurationContext(NO_OF_CLIENTS, DISCONNECTED_CLIENT);
    this.handler.initialize(this.serverCfgCxt);
  }

  public void testBasic() throws Exception {
    System.out.println("Client 5 gets disonnected ");

    ServerTransaction serverTX = new TestServerTransaction(SRC_CLIENT_ID);
    BroadcastChangeContext context = new BroadcastChangeContext(serverTX, new GlobalTransactionID(1),
                                                                new NotifiedWaiters(), new BackReferences());
    this.handler.handleEvent(context);

    TestServerTransactionManager serverTxManager = (TestServerTransactionManager) this.serverCfgCxt
        .getTransactionManager();
    List<NodeID> clients = serverTxManager.acknowledgedBack;

    Assert.assertEquals(NO_OF_CLIENTS - 1, clients.size());
  }

  private class TestServerTransaction implements ServerTransaction {
    private final int srcID;

    public TestServerTransaction(int srcID) {
      this.srcID = srcID;
    }

    public TxnBatchID getBatchID() {
      throw new ImplementMe();
    }

    public List getChanges() {
      return Collections.EMPTY_LIST;
    }

    public DmiDescriptor[] getDmiDescriptors() {
      return new DmiDescriptor[0];
    }

    public long[] getHighWaterMarks() {
      return null;
    }

    public LockID[] getLockIDs() {
      return new LockID[] { new StringLockID("1") };
    }

    public ObjectIDSet getNewObjectIDs() {
      return null;
    }

    public Map getNewRoots() {
      return Collections.EMPTY_MAP;
    }

    public Collection getNotifies() {
      return Collections.EMPTY_LIST;
    }

    public int getNumApplicationTxn() {
      return 0;
    }

    public ObjectIDSet getObjectIDs() {
      return null;
    }

    public ObjectStringSerializer getSerializer() {
      return null;
    }

    public ServerTransactionID getServerTransactionID() {
      return null;
    }

    public NodeID getSourceID() {
      return new ClientID(this.srcID);
    }

    public TransactionID getTransactionID() {
      return null;
    }

    public TxnType getTransactionType() {
      return TxnType.CONCURRENT;
    }

    public boolean needsBroadcast() {
      return false;
    }

    public SequenceID getClientSequenceID() {
      return null;
    }

    public GlobalTransactionID getGlobalTransactionID() {
      return null;
    }

    public void setGlobalTransactionID(GlobalTransactionID gid) {
      throw new ImplementMe();
    }

  }

  private class TestServerConfigurationContext implements ServerConfigurationContext {
    private final int                          noOfClients;
    private final int                          clientDisconnectNo;
    private final TestServerTransactionManager testServerTxManager;

    public TestServerConfigurationContext(int noOfClients, int clientDisconnectNo) {
      this.clientDisconnectNo = clientDisconnectNo;
      this.noOfClients = noOfClients;
      this.testServerTxManager = new TestServerTransactionManager(new ClientID(clientDisconnectNo));
    }

    public DSOChannelManager getChannelManager() {
      return new TestDSOChannelManager(this.noOfClients, this.clientDisconnectNo);
    }

    public ChannelStats getChannelStats() {
      throw new ImplementMe();
    }

    public ServerClientHandshakeManager getClientHandshakeManager() {
      throw new ImplementMe();
    }

    public ClientStateManager getClientStateManager() {
      return new TestClientStateManagerImpl();
    }

    public ServerClusterMetaDataManager getClusterMetaDataManager() {
      return null;
    }

    public L2Coordinator getL2Coordinator() {
      throw new ImplementMe();
    }

    public LockManager getLockManager() {
      throw new ImplementMe();
    }

    public ObjectManager getObjectManager() {
      throw new ImplementMe();
    }

    public ObjectRequestManager getObjectRequestManager() {
      throw new ImplementMe();
    }

    public ManagedObjectStore getObjectStore() {
      throw new ImplementMe();
    }

    public ServerGlobalTransactionManager getServerGlobalTransactionManager() {
      throw new ImplementMe();
    }

    public TransactionBatchManager getTransactionBatchManager() {
      throw new ImplementMe();
    }

    public TransactionBatchReaderFactory getTransactionBatchReaderFactory() {
      throw new ImplementMe();
    }

    public ServerTransactionManager getTransactionManager() {
      return this.testServerTxManager;
    }

    public TransactionalObjectManager getTransactionalObjectManager() {
      throw new ImplementMe();
    }

    public TCLogger getLogger(Class clazz) {
      throw new ImplementMe();
    }

    public Stage getStage(String name) {
      return new TestStage();
    }

    public ServerMapRequestManager getServerMapRequestManager() {
      throw new ImplementMe();
    }

  }

  private class TestStage implements Stage {
    private final Sink sink = new TestSink();

    public void destroy() {
      throw new ImplementMe();
    }

    public Sink getSink() {
      return this.sink;
    }

    public void start(ConfigurationContext context) {
      throw new ImplementMe();
    }

  }

  private class TestSink implements Sink {

    public void add(EventContext context) {
      //
    }

    public boolean addLossy(EventContext context) {
      throw new ImplementMe();
    }

    public void addMany(Collection contexts) {
      throw new ImplementMe();
    }

    public void clear() {
      throw new ImplementMe();
    }

    public AddPredicate getPredicate() {
      throw new ImplementMe();
    }

    public void setAddPredicate(AddPredicate predicate) {
      throw new ImplementMe();
    }

    public int size() {
      throw new ImplementMe();
    }

    public void enableStatsCollection(boolean enable) {
      throw new ImplementMe();
    }

    public Stats getStats(long frequency) {
      throw new ImplementMe();
    }

    public Stats getStatsAndReset(long frequency) {
      throw new ImplementMe();
    }

    public boolean isStatsCollectionEnabled() {
      throw new ImplementMe();
    }

    public void resetStats() {
      throw new ImplementMe();
    }

  }

  private class TestDSOChannelManager implements DSOChannelManager {
    private final int noOfChannels;
    private final int deadChannelID;

    public TestDSOChannelManager(int noOfChannels, int deadChannelID) {
      this.noOfChannels = noOfChannels;
      this.deadChannelID = deadChannelID;
    }

    public void addEventListener(DSOChannelManagerEventListener listener) {
      throw new ImplementMe();
    }

    public void closeAll(Collection clientIDs) {
      throw new ImplementMe();
    }

    public MessageChannel getActiveChannel(NodeID id) {
      throw new ImplementMe();
    }

    public MessageChannel[] getActiveChannels() {
      MessageChannel[] channels = new MessageChannel[this.noOfChannels];
      for (int i = 1; i <= this.noOfChannels; i++) {
        boolean isClosed = i == this.deadChannelID ? true : false;
        channels[i - 1] = new TestMessageChannel(i, isClosed);
      }

      return channels;
    }

    public TCConnection[] getAllActiveClientConnections() {
      throw new ImplementMe();
    }

    public Set getAllClientIDs() {
      throw new ImplementMe();
    }

    public String getChannelAddress(NodeID nid) {
      throw new ImplementMe();
    }

    public ClientID getClientIDFor(ChannelID channelID) {
      return new ClientID(channelID.toLong());
    }

    public boolean isActiveID(NodeID nodeID) {
      throw new ImplementMe();
    }

    public void makeChannelActive(ClientID clientID, boolean persistent) {
      throw new ImplementMe();
    }

    public void makeChannelActiveNoAck(MessageChannel channel) {
      throw new ImplementMe();
    }

    public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(NodeID nid) {
      throw new ImplementMe();
    }
  }

  private class TestMessageChannel implements MessageChannel {
    private final long    id;
    private final boolean isClosed;

    public TestMessageChannel(int id, boolean isClosed) {
      this.isClosed = isClosed;
      this.id = id;
    }

    public void addAttachment(String key, Object value, boolean replace) {
      throw new ImplementMe();
    }

    public void addListener(ChannelEventListener listener) {
      throw new ImplementMe();
    }

    public void close() {
      throw new ImplementMe();
    }

    public TCMessage createMessage(TCMessageType type) {
      return new TestBroadcastMessage();
    }

    public Object getAttachment(String key) {
      throw new ImplementMe();
    }

    public ChannelID getChannelID() {
      return new ChannelID(this.id);
    }

    public TCSocketAddress getLocalAddress() {
      throw new ImplementMe();
    }

    public NodeID getLocalNodeID() {
      throw new ImplementMe();
    }

    public TCSocketAddress getRemoteAddress() {
      throw new ImplementMe();
    }

    public NodeID getRemoteNodeID() {
      throw new ImplementMe();
    }

    public boolean isClosed() {
      return this.isClosed;
    }

    public boolean isConnected() {
      throw new ImplementMe();
    }

    public boolean isOpen() {
      throw new ImplementMe();
    }

    public NetworkStackID open() {
      throw new ImplementMe();
    }

    public Object removeAttachment(String key) {
      throw new ImplementMe();
    }

    public void send(TCNetworkMessage message) {
      //
    }

    public void setLocalNodeID(NodeID source) {
      throw new ImplementMe();
    }
  }

  private class TestBroadcastMessage implements TCMessage, BroadcastTransactionMessage {

    public void dehydrate() {
      throw new ImplementMe();
    }

    public MessageChannel getChannel() {
      throw new ImplementMe();
    }

    public NodeID getDestinationNodeID() {
      throw new ImplementMe();
    }

    public SessionID getLocalSessionID() {
      throw new ImplementMe();
    }

    public TCMessageType getMessageType() {
      throw new ImplementMe();
    }

    public NodeID getSourceNodeID() {
      throw new ImplementMe();
    }

    public int getTotalLength() {
      throw new ImplementMe();
    }

    public void hydrate() {
      throw new ImplementMe();
    }

    public void send() {
      //
    }

    public Collection addNotifiesTo(List c) {
      throw new ImplementMe();
    }

    public long getChangeID() {
      throw new ImplementMe();
    }

    public NodeID getCommitterID() {
      throw new ImplementMe();
    }

    public List getDmiDescriptors() {
      throw new ImplementMe();
    }

    public GlobalTransactionID getGlobalTransactionID() {
      throw new ImplementMe();
    }

    public List getLockIDs() {
      throw new ImplementMe();
    }

    public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
      throw new ImplementMe();
    }

    public Map getNewRoots() {
      throw new ImplementMe();
    }

    public Collection getObjectChanges() {
      throw new ImplementMe();
    }

    public TransactionID getTransactionID() {
      throw new ImplementMe();
    }

    public TxnType getTransactionType() {
      throw new ImplementMe();
    }

    public void initialize(List chges, ObjectStringSerializer serializer, LockID[] lids, long cid, TransactionID txID,
                           NodeID commitID, GlobalTransactionID gtx, TxnType txnType,
                           GlobalTransactionID lowGlobalTransactionIDWatermark, Collection notifies, Map newRoots,
                           DmiDescriptor[] dmis) {
      //
    }

  }

  private class TestClientStateManagerImpl implements ClientStateManager {

    public void addAllReferencedIdsTo(Set<ObjectID> rescueIds) {
      throw new ImplementMe();

    }

    public void addReference(NodeID nodeID, ObjectID objectID) {
      throw new ImplementMe();

    }

    public Set<ObjectID> addReferences(NodeID nodeID, Set<ObjectID> oids) {
      return null;
    }

    public List<DNA> createPrunedChangesAndAddObjectIDTo(Collection<DNA> changes, BackReferences references,
                                                         NodeID clientID, Set<ObjectID> objectIDs) {
      ArrayList<DNA> list = new ArrayList<DNA>();
      ObjectID dateID = new ObjectID(1);
      list.add(new TestDateDNA("java.util.Date", dateID));
      return list;
    }

    public Set<NodeID> getConnectedClientIDs() {
      throw new ImplementMe();
    }

    public int getReferenceCount(NodeID nodeID) {
      throw new ImplementMe();
    }

    public boolean hasReference(NodeID nodeID, ObjectID objectID) {
      throw new ImplementMe();
    }

    public void removeReferencedFrom(NodeID nodeID, Set<ObjectID> secondPass) {
      throw new ImplementMe();
    }

    public void removeReferences(NodeID nodeID, Set<ObjectID> removed) {
      throw new ImplementMe();
    }

    public void shutdownNode(NodeID deadNode) {
      throw new ImplementMe();
    }

    public void startupNode(NodeID nodeID) {
      throw new ImplementMe();
    }

  }

  private class TestServerTransactionManager implements ServerTransactionManager {

    private final NodeID       deadNodeID;
    private final List<NodeID> acknowledgedBack = new ArrayList<NodeID>();

    public TestServerTransactionManager(NodeID dead) {
      this.deadNodeID = dead;
    }

    public void acknowledgement(NodeID waiter, TransactionID requestID, NodeID waitee) {
      this.acknowledgedBack.add(waitee);
    }

    public void addWaitingForAcknowledgement(NodeID waiter, TransactionID requestID, NodeID waitee) {
      if (this.deadNodeID.equals(waitee)) { return; }
      acknowledgement(waiter, requestID, waitee);
    }

    public void addTransactionListener(ServerTransactionListener listener) {
      throw new ImplementMe();
    }

    public void apply(ServerTransaction txn, Map objects, BackReferences includeIDs,
                      ObjectInstanceMonitor instanceMonitor) {
      throw new ImplementMe();
    }

    public void broadcasted(NodeID waiter, TransactionID requestID) {
      //
    }

    public void callBackOnResentTxnsInSystemCompletion(TxnsInSystemCompletionLister l) {
      throw new ImplementMe();
    }

    public void callBackOnTxnsInSystemCompletion(TxnsInSystemCompletionLister l) {
      throw new ImplementMe();
    }

    public void commit(PersistenceTransactionProvider ptxp, Collection objects, Map newRoots,
                       Collection appliedServerTransactionIDs) {
      throw new ImplementMe();
    }

    public int getTotalPendingTransactionsCount() {
      throw new ImplementMe();
    }

    public void goToActiveMode() {
      throw new ImplementMe();
    }

    public void incomingTransactions(NodeID nodeID, Set txnIDs, Collection txns, boolean relayed) {
      throw new ImplementMe();
    }

    public boolean isWaiting(NodeID waiter, TransactionID requestID) {
      throw new ImplementMe();
    }

    public void nodeConnected(NodeID nodeID) {
      throw new ImplementMe();
    }

    public void objectsSynched(NodeID node, ServerTransactionID tid) {
      throw new ImplementMe();
    }

    public void removeTransactionListener(ServerTransactionListener listener) {
      throw new ImplementMe();
    }

    public void setResentTransactionIDs(NodeID source, Collection transactionIDs) {
      throw new ImplementMe();
    }

    public void shutdownNode(NodeID nodeID) {
      throw new ImplementMe();
    }

    public void skipApplyAndCommit(ServerTransaction txn) {
      throw new ImplementMe();
    }

    public void start(Set cids) {
      throw new ImplementMe();
    }

    public void transactionsRelayed(NodeID node, Set serverTxnIDs) {
      throw new ImplementMe();
    }

    public long getTotalNumOfActiveTransactions() {
      throw new ImplementMe();
    }
  }
}
