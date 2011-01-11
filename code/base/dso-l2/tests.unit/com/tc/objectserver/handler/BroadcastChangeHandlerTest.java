/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import org.mockito.Mockito;

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
import com.tc.object.dna.api.MetaDataReader;
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
import com.tc.objectserver.l1.api.InvalidateObjectManager;
import com.tc.objectserver.locks.LockManager;
import com.tc.objectserver.locks.NotifiedWaiters;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.metadata.MetaDataManager;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.search.IndexManager;
import com.tc.objectserver.search.SearchRequestManager;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionListener;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.objectserver.tx.TransactionBatchReaderFactory;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionListener;
import com.tc.stats.Stats;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.SampledCounterImpl;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.stats.counter.sampled.derived.SampledRateCounterImpl;
import com.tc.test.TCTestCase;
import com.tc.text.PrettyPrinter;
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
    final SampledCounterImpl sci = new SampledCounterImpl(new SampledCounterConfig(5, 10, true, 0));
    final SampledRateCounterImpl srci = new SampledRateCounterImpl(new SampledRateCounterConfig(5, 10, true));
    this.handler = new BroadcastChangeHandler(sci, new ObjectStatsRecorder(), srci, Mockito
        .mock(InvalidateObjectManager.class));
    this.serverCfgCxt = new TestServerConfigurationContext(NO_OF_CLIENTS, DISCONNECTED_CLIENT);
    this.handler.initialize(this.serverCfgCxt);
  }

  public void testBasic() throws Exception {
    System.out.println("Client 5 gets disonnected ");

    final ServerTransaction serverTX = new TestServerTransaction(SRC_CLIENT_ID);
    final BroadcastChangeContext context = new BroadcastChangeContext(serverTX, new GlobalTransactionID(1),
                                                                      new NotifiedWaiters(), new ApplyTransactionInfo());
    this.handler.handleEvent(context);

    final TestServerTransactionManager serverTxManager = (TestServerTransactionManager) this.serverCfgCxt
        .getTransactionManager();
    final List<NodeID> clients = serverTxManager.acknowledgedBack;

    Assert.assertEquals(NO_OF_CLIENTS - 1, clients.size());
  }

  private class TestServerTransaction implements ServerTransaction {
    private final int srcID;

    public TestServerTransaction(final int srcID) {
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

    public MetaDataReader[] getMetaDataReaders() {
      return new MetaDataReader[0];
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

    public boolean isActiveTxn() {
      return false;
    }

    public SequenceID getClientSequenceID() {
      return null;
    }

    public GlobalTransactionID getGlobalTransactionID() {
      return null;
    }

    public void setGlobalTransactionID(final GlobalTransactionID gid) {
      throw new ImplementMe();
    }

  }

  private class TestServerConfigurationContext implements ServerConfigurationContext {
    private final int                          noOfClients;
    private final int                          clientDisconnectNo;
    private final TestServerTransactionManager testServerTxManager;

    public TestServerConfigurationContext(final int noOfClients, final int clientDisconnectNo) {
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

    public TCLogger getLogger(final Class clazz) {
      throw new ImplementMe();
    }

    public Stage getStage(final String name) {
      return new TestStage();
    }

    public ServerMapRequestManager getServerMapRequestManager() {
      throw new ImplementMe();
    }

    public IndexManager getIndexManager() {
      throw new ImplementMe();
    }

    public MetaDataManager getMetaDataManager() {
      throw new ImplementMe();
    }

    public SearchRequestManager getSearchRequestManager() {
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

    public void start(final ConfigurationContext context) {
      throw new ImplementMe();
    }

    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      throw new ImplementMe();
    }

  }

  private class TestSink implements Sink {

    public void add(final EventContext context) {
      //
    }

    public boolean addLossy(final EventContext context) {
      throw new ImplementMe();
    }

    public void addMany(final Collection contexts) {
      throw new ImplementMe();
    }

    public void clear() {
      throw new ImplementMe();
    }

    public AddPredicate getPredicate() {
      throw new ImplementMe();
    }

    public void setAddPredicate(final AddPredicate predicate) {
      throw new ImplementMe();
    }

    public int size() {
      throw new ImplementMe();
    }

    public void enableStatsCollection(final boolean enable) {
      throw new ImplementMe();
    }

    public Stats getStats(final long frequency) {
      throw new ImplementMe();
    }

    public Stats getStatsAndReset(final long frequency) {
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

    public TestDSOChannelManager(final int noOfChannels, final int deadChannelID) {
      this.noOfChannels = noOfChannels;
      this.deadChannelID = deadChannelID;
    }

    public void addEventListener(final DSOChannelManagerEventListener listener) {
      throw new ImplementMe();
    }

    public void closeAll(final Collection clientIDs) {
      throw new ImplementMe();
    }

    public MessageChannel getActiveChannel(final NodeID id) {
      throw new ImplementMe();
    }

    public MessageChannel[] getActiveChannels() {
      final MessageChannel[] channels = new MessageChannel[this.noOfChannels];
      for (int i = 1; i <= this.noOfChannels; i++) {
        final boolean isClosed = i == this.deadChannelID ? true : false;
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

    public String getChannelAddress(final NodeID nid) {
      throw new ImplementMe();
    }

    public ClientID getClientIDFor(final ChannelID channelID) {
      return new ClientID(channelID.toLong());
    }

    public boolean isActiveID(final NodeID nodeID) {
      throw new ImplementMe();
    }

    public void makeChannelActive(final ClientID clientID, final boolean persistent) {
      throw new ImplementMe();
    }

    public void makeChannelActiveNoAck(final MessageChannel channel) {
      throw new ImplementMe();
    }

    public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(final NodeID nid) {
      throw new ImplementMe();
    }

    public void makeChannelRefuse(ClientID clientID, String message) {
      throw new ImplementMe();

    }
  }

  private class TestMessageChannel implements MessageChannel {
    private final long    id;
    private final boolean isClosed;

    public TestMessageChannel(final int id, final boolean isClosed) {
      this.isClosed = isClosed;
      this.id = id;
    }

    public void addAttachment(final String key, final Object value, final boolean replace) {
      throw new ImplementMe();
    }

    public void addListener(final ChannelEventListener listener) {
      throw new ImplementMe();
    }

    public void close() {
      throw new ImplementMe();
    }

    public TCMessage createMessage(final TCMessageType type) {
      return new TestBroadcastMessage();
    }

    public Object getAttachment(final String key) {
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

    public Object removeAttachment(final String key) {
      throw new ImplementMe();
    }

    public void send(final TCNetworkMessage message) {
      //
    }

    public void setLocalNodeID(final NodeID source) {
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

    public Collection addNotifiesTo(final List c) {
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

    public void initialize(final List chges, final ObjectStringSerializer serializer, final LockID[] lids,
                           final long cid, final TransactionID txID, final NodeID commitID,
                           final GlobalTransactionID gtx, final TxnType txnType,
                           final GlobalTransactionID lowGlobalTransactionIDWatermark, final Collection notifies,
                           final Map newRoots, final DmiDescriptor[] dmis) {
      //
    }

  }

  private class TestClientStateManagerImpl implements ClientStateManager {

    public Set<ObjectID> addAllReferencedIdsTo(final Set<ObjectID> rescueIds) {
      throw new ImplementMe();
    }

    public void addReference(final NodeID nodeID, final ObjectID objectID) {
      throw new ImplementMe();
    }

    public Set<ObjectID> addReferences(final NodeID nodeID, final Set<ObjectID> oids) {
      return null;
    }

    public List<DNA> createPrunedChangesAndAddObjectIDTo(Collection<DNA> changes, ApplyTransactionInfo references,
                                                         NodeID clientID, Set<ObjectID> objectIDs,
                                                         Set<ObjectID> invalidateObjectIDs) {
      final ArrayList<DNA> list = new ArrayList<DNA>();
      final ObjectID dateID = new ObjectID(1);
      list.add(new TestDateDNA("java.util.Date", dateID));
      return list;
    }

    public Set<NodeID> getConnectedClientIDs() {
      throw new ImplementMe();
    }

    public int getReferenceCount(final NodeID nodeID) {
      throw new ImplementMe();
    }

    public boolean hasReference(final NodeID nodeID, final ObjectID objectID) {
      throw new ImplementMe();
    }

    public void removeReferencedFrom(final NodeID nodeID, final Set<ObjectID> secondPass) {
      throw new ImplementMe();
    }

    public void removeReferences(final NodeID nodeID, final Set<ObjectID> removed) {
      throw new ImplementMe();
    }

    public void shutdownNode(final NodeID deadNode) {
      throw new ImplementMe();
    }

    public void startupNode(final NodeID nodeID) {
      throw new ImplementMe();
    }

  }

  private class TestServerTransactionManager implements ServerTransactionManager {

    private final NodeID       deadNodeID;
    private final List<NodeID> acknowledgedBack = new ArrayList<NodeID>();

    public TestServerTransactionManager(final NodeID dead) {
      this.deadNodeID = dead;
    }

    public void acknowledgement(final NodeID waiter, final TransactionID requestID, final NodeID waitee) {
      this.acknowledgedBack.add(waitee);
    }

    public void addWaitingForAcknowledgement(final NodeID waiter, final TransactionID requestID, final NodeID waitee) {
      if (this.deadNodeID.equals(waitee)) { return; }
      acknowledgement(waiter, requestID, waitee);
    }

    public void addTransactionListener(final ServerTransactionListener listener) {
      throw new ImplementMe();
    }

    public void apply(final ServerTransaction txn, final Map objects, final ApplyTransactionInfo includeIDs,
                      final ObjectInstanceMonitor instanceMonitor) {
      throw new ImplementMe();
    }

    public void broadcasted(final NodeID waiter, final TransactionID requestID) {
      //
    }

    public void callBackOnResentTxnsInSystemCompletion(final TxnsInSystemCompletionListener l) {
      throw new ImplementMe();
    }

    public void callBackOnTxnsInSystemCompletion(final TxnsInSystemCompletionListener l) {
      throw new ImplementMe();
    }

    public void commit(final PersistenceTransactionProvider ptxp, final Collection objects, final Map newRoots,
                       final Collection appliedServerTransactionIDs) {
      throw new ImplementMe();
    }

    public int getTotalPendingTransactionsCount() {
      throw new ImplementMe();
    }

    public void goToActiveMode() {
      throw new ImplementMe();
    }

    public void incomingTransactions(final NodeID nodeID, final Set txnIDs, final Collection txns, final boolean relayed) {
      throw new ImplementMe();
    }

    public boolean isWaiting(final NodeID waiter, final TransactionID requestID) {
      throw new ImplementMe();
    }

    public void nodeConnected(final NodeID nodeID) {
      throw new ImplementMe();
    }

    public void objectsSynched(final NodeID node, final ServerTransactionID tid) {
      throw new ImplementMe();
    }

    public void removeTransactionListener(final ServerTransactionListener listener) {
      throw new ImplementMe();
    }

    public void setResentTransactionIDs(final NodeID source, final Collection transactionIDs) {
      throw new ImplementMe();
    }

    public void shutdownNode(final NodeID nodeID) {
      throw new ImplementMe();
    }

    public void skipApplyAndCommit(final ServerTransaction txn) {
      throw new ImplementMe();
    }

    public void start(final Set cids) {
      throw new ImplementMe();
    }

    public void transactionsRelayed(final NodeID node, final Set serverTxnIDs) {
      throw new ImplementMe();
    }

    public long getTotalNumOfActiveTransactions() {
      throw new ImplementMe();
    }

    public void processingMetaDataCompleted(NodeID sourceID, TransactionID txnID) {
      //
    }
  }
}
