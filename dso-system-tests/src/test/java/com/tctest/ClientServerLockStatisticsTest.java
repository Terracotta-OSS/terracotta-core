/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.TCTestCase;

// import java.lang.reflect.Constructor;
// import java.util.Collection;
// import java.util.Iterator;
// import java.util.concurrent.BrokenBarrierException;
// import java.util.concurrent.CyclicBarrier;

public class ClientServerLockStatisticsTest extends TCTestCase {

  // private ClientLockManagerImpl clientLockManager;
  // private LockManagerImpl serverLockManager;
  // private ClientServerLockStatManagerGlue clientServerGlue;
  // private TestSessionManager sessionManager;
  // private ManualThreadIDManager threadManager;
  // //private ClientLockStatManager clientLockStatManager;
  // private L2LockStatsManager serverLockStatManager;
  // private final ChannelID channelId1 = new ChannelID(1);
  // private ClientMessageChannel channel1;
  // private TestSink sink;
  //
  // public ClientServerLockStatisticsTest() {
  // // MNK-444
  // // disableAllUntil(new Date(Long.MAX_VALUE));
  // }
  //
  // @Override
  // protected void setUp() throws Exception {
  // super.setUp();
  //
  // sink = new TestSink();
  // sessionManager = new TestSessionManager();
  // clientServerGlue = new ClientServerLockStatManagerGlue(sessionManager, sink);
  // //clientLockStatManager = new ClientLockStatisticsManagerImpl();
  // threadManager = new ManualThreadIDManager();
  // clientLockManager = new ClientLockManagerImpl(new NullTCLogger(), sessionManager, clientServerGlue, threadManager,
  // new MockTransactionManager());
  //
  // DSOChannelManager nullChannelManager = new NullChannelManager();
  // //serverLockStatManager = new L2LockStatisticsManagerImpl();
  // serverLockManager = new LockManagerImpl(nullChannelManager, serverLockStatManager);
  // serverLockManager.setLockPolicy(LockManagerImpl.ALTRUISTIC_LOCK_POLICY);
  //
  // channel1 = new TestClientMessageChannel(channelId1, sink);
  // serverLockStatManager.start(new TestChannelManager(channel1), null);
  //
  // clientLockStatManager.start(new TestClientChannel(channel1), sink);
  // clientServerGlue.set(clientLockManager, serverLockManager, clientLockStatManager, serverLockStatManager);
  // }
  //
  // public void testClientDisconnect() {
  // final CyclicBarrier localBarrier = new CyclicBarrier(2);
  // DSOChannelManager nullChannelManager = new NullChannelManager();
  // serverLockStatManager = new MockL2LockStatManagerImpl(localBarrier);
  // serverLockManager = new LockManagerImpl(nullChannelManager, serverLockStatManager);
  //
  // serverLockStatManager.start(new TestChannelManager(channel1), null);
  // clientServerGlue.set(clientLockManager, serverLockManager, clientLockStatManager, serverLockStatManager);
  //
  // final LockID l1 = new StringLockID("1");
  // final ThreadID tx1 = new ThreadID(1);
  // final ThreadID tx2 = new ThreadID(1);
  //
  // Thread t1 = new Thread(new Runnable() {
  // public void run() {
  // serverLockManager.clearAllLocksFor(new ClientID(channelId1.toLong()));
  // try {
  // localBarrier.await();
  // } catch (InterruptedException e) {
  // throw new AssertionError(e);
  // } catch (BrokenBarrierException e) {
  // throw new AssertionError(e);
  // }
  // }
  // });
  // serverLockStatManager.setLockStatisticsConfig(1, 1);
  // threadManager.setThreadID(tx1);
  // clientLockManager.lock(l1, LockLevel.WRITE);
  // t1.start();
  // Collection c = serverLockStatManager.getLockSpecs();
  // Assert.assertEquals(1, c.size());
  // LockSpec lockSpec = (LockSpec) c.iterator().next();
  // Assert.assertEquals(0, lockSpec.children().size());
  //
  // threadManager.setThreadID(tx2);
  // clientLockManager.unlock(l1, LockLevel.WRITE);
  // }
  //
  // public void testCollectLockStackTraces() {
  // final LockID lockID1 = new StringLockID("1");
  // final ThreadID tx1 = new ThreadID(1);
  // final ThreadID tx2 = new ThreadID(1);
  //
  // serverLockStatManager.setLockStatisticsConfig(1, 1);
  // threadManager.setThreadID(tx1);
  // clientLockManager.lock(lockID1, LockLevel.READ);
  // sleep(1000);
  //
  // threadManager.setThreadID(tx2);
  // clientLockManager.lock(lockID1, LockLevel.READ);
  // sleep(2000);
  // assertStackTraces(lockID1, 2, 1);
  // assertStackTraces(lockID1, 2, 1);
  //
  // threadManager.setThreadID(tx2);
  // clientLockManager.unlock(lockID1, LockLevel.READ);
  // sleep(2000);
  // assertStackTraces(lockID1, 3, 1);
  // assertStackTraces(lockID1, 3, 1);
  //
  // threadManager.setThreadID(tx1);
  // clientLockManager.unlock(lockID1, LockLevel.READ);
  // sleep(1000);
  // assertStackTraces(lockID1, 4, 1);
  // assertStackTraces(lockID1, 4, 1);
  //
  // serverLockStatManager.setLockStatisticsConfig(2, 1);
  // sleep(1000);
  // threadManager.setThreadID(tx2);
  // clientLockManager.lock(lockID1, LockLevel.WRITE);
  // sleep(1000);
  // clientLockManager.lock(lockID1, LockLevel.WRITE);
  // sleep(2000);
  // assertStackTraces(lockID1, 2, 2);
  // assertStackTraces(lockID1, 2, 2);
  // clientLockManager.unlock(lockID1, LockLevel.WRITE);
  // assertStackTraces(lockID1, 3, 2);
  // assertStackTraces(lockID1, 3, 2);
  // clientLockManager.unlock(lockID1, LockLevel.WRITE);
  // }
  //
  // private void assertStackTraces(final LockID lockID, final int numOfStackTraces, final int depthOfStackTraces) {
  // Collection lockSpecs = serverLockStatManager.getLockSpecs();
  //
  // Assert.assertEquals(1, lockSpecs.size()); // only one client in this test
  // for (Iterator i = lockSpecs.iterator(); i.hasNext();) {
  // LockSpec s = (LockSpec) i.next();
  // Collection children = s.children();
  // Assert.assertTrue(assertStackTracesDepth(children, depthOfStackTraces));
  // }
  // }
  //
  // private boolean assertStackTracesDepth(final Collection traces, final int expectedDepthOfStackTraces) {
  // if (traces.size() == 0 && expectedDepthOfStackTraces == 0) { return true; }
  // if (traces.size() == 0 || expectedDepthOfStackTraces == 0) { return false; }
  //
  // LockStatElement lse = (LockStatElement) traces.iterator().next();
  // return assertStackTracesDepth(lse.children(), expectedDepthOfStackTraces - 1);
  // }
  //
  // private void sleep(final long l) {
  // try {
  // Thread.sleep(l);
  // } catch (InterruptedException e) {
  // // NOP
  // }
  // }
  //
  // @Override
  // protected void tearDown() throws Exception {
  // clientServerGlue.stop();
  // super.tearDown();
  // }
  //
  // private static class TestChannelManager extends NullChannelManager {
  // private final MessageChannel channel;
  //
  // public TestChannelManager(final MessageChannel channel) {
  // this.channel = channel;
  // }
  //
  // @Override
  // public MessageChannel[] getActiveChannels() {
  // return new MessageChannel[] { channel };
  // }
  // }
  //
  // private static class TestClientMessageChannel extends MockMessageChannel implements ClientMessageChannel {
  // private final Sink sink;
  //
  // public TestClientMessageChannel(final ChannelID channelId, final Sink sink) {
  // super(channelId);
  // super.registerType(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE, LockStatisticsResponseMessageImpl.class);
  // super.registerType(TCMessageType.LOCK_STAT_MESSAGE, LockStatisticsMessage.class);
  // this.sink = sink;
  // }
  //
  // @Override
  // public TCMessage createMessage(final TCMessageType type) {
  // Class theClass = super.getRegisteredMessageClass(type);
  //
  // if (theClass == null) throw new ImplementMe();
  //
  // try {
  // Constructor constructor = theClass.getConstructor(new Class[] { SessionID.class, MessageMonitor.class,
  // TCByteBufferOutputStream.class, MessageChannel.class, TCMessageType.class });
  // TCMessageImpl message = (TCMessageImpl) constructor.newInstance(new Object[] { SessionID.NULL_ID,
  // new NullMessageMonitor(), new TCByteBufferOutputStream(4, 4096, false), this, type });
  // // message.seal();
  // return message;
  // } catch (Exception e) {
  // throw new ImplementMe("Failed", e);
  // }
  // }
  //
  // @Override
  // public void send(final TCNetworkMessage message) {
  // super.send(message);
  // sink.add(message);
  // }
  //
  // public void addClassMapping(final TCMessageType type, final Class msgClass) {
  // throw new ImplementMe();
  //
  // }
  //
  // public ChannelIDProvider getChannelIDProvider() {
  // return null;
  // }
  //
  // public int getConnectAttemptCount() {
  // return 0;
  // }
  //
  // public int getConnectCount() {
  // return 0;
  // }
  //
  // public void routeMessageType(final TCMessageType messageType, final Sink destSink, final Sink hydrateSink) {
  // throw new ImplementMe();
  //
  // }
  //
  // public void routeMessageType(final TCMessageType type, final TCMessageSink sinkArg) {
  // throw new ImplementMe();
  //
  // }
  //
  // public void unrouteMessageType(final TCMessageType type) {
  // throw new ImplementMe();
  //
  // }
  //
  // }
  //
  // private static class TestClientChannel implements DSOClientMessageChannel, LockStatisticsReponseMessageFactory {
  // private final ClientMessageChannel clientMessageChannel;
  //
  // public TestClientChannel(final ClientMessageChannel clientMessageChannel) {
  // this.clientMessageChannel = clientMessageChannel;
  // }
  //
  // public ClientMessageChannel channel() {
  // return clientMessageChannel;
  // }
  //
  // public void addClassMapping(final TCMessageType messageType, final Class messageClass) {
  // throw new ImplementMe();
  //
  // }
  //
  // public void addListener(final ChannelEventListener listener) {
  // throw new ImplementMe();
  //
  // }
  //
  // public void close() {
  // throw new ImplementMe();
  //
  // }
  //
  // public AcknowledgeTransactionMessageFactory getAcknowledgeTransactionMessageFactory() {
  // throw new ImplementMe();
  // }
  //
  // public ClientIDProvider getClientIDProvider() {
  // throw new ImplementMe();
  // }
  //
  // public ClientHandshakeMessageFactory getClientHandshakeMessageFactory() {
  // throw new ImplementMe();
  // }
  //
  // public CommitTransactionMessageFactory getCommitTransactionMessageFactory() {
  // throw new ImplementMe();
  // }
  //
  // public JMXMessage getJMXMessage() {
  // throw new ImplementMe();
  // }
  //
  // public LockRequestMessageFactory getLockRequestMessageFactory() {
  // throw new ImplementMe();
  // }
  //
  // public ObjectIDBatchRequestMessageFactory getObjectIDBatchRequestMessageFactory() {
  // throw new ImplementMe();
  // }
  //
  // public RequestManagedObjectMessageFactory getRequestManagedObjectMessageFactory() {
  // throw new ImplementMe();
  // }
  //
  // public RequestRootMessageFactory getRequestRootMessageFactory() {
  // throw new ImplementMe();
  // }
  //
  // public NodesWithObjectsMessageFactory getNodesWithObjectsMessageFactory() {
  // throw new ImplementMe();
  // }
  //
  // public KeysForOrphanedValuesMessageFactory getKeysForOrphanedValuesMessageFactory() {
  // throw new ImplementMe();
  // }
  //
  // public NodeMetaDataMessageFactory getNodeMetaDataMessageFactory() {
  // throw new ImplementMe();
  // }
  //
  // public boolean isConnected() {
  // throw new ImplementMe();
  // }
  //
  // public void open() {
  // throw new ImplementMe();
  //
  // }
  //
  // public void routeMessageType(final TCMessageType messageType, final Sink destSink, final Sink hydrateSink) {
  // throw new ImplementMe();
  //
  // }
  //
  // public CompletedTransactionLowWaterMarkMessageFactory getCompletedTransactionLowWaterMarkMessageFactory() {
  // throw new ImplementMe();
  // }
  //
  // public GroupID[] getGroupIDs() {
  // throw new ImplementMe();
  // }
  //
  // public LockStatisticsReponseMessageFactory getLockStatisticsReponseMessageFactory() {
  // return this;
  // }
  //
  // public LockStatisticsResponseMessage newLockStatisticsResponseMessage(NodeID remoteID) {
  // return (LockStatisticsResponseMessage) channel().createMessage(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE);
  // }
  // }
  //
  // private static class MockL2LockStatManagerImpl extends L2LockStatisticsManagerImpl {
  // private final CyclicBarrier barrier;
  //
  // public MockL2LockStatManagerImpl(final CyclicBarrier barrier) {
  // super();
  // this.barrier = barrier;
  // }
  //
  // @Override
  // public void recordClientStat(final NodeID nodeID, final Collection<TCStackTraceElement> stackTraceElements) {
  // try {
  // barrier.await();
  // super.recordClientStat(nodeID, stackTraceElements);
  // } catch (InterruptedException e) {
  // throw new AssertionError(e);
  // } catch (BrokenBarrierException e) {
  // throw new AssertionError(e);
  // }
  // }
  // }

  public void testDummy() {
    // please the testing gods
  }
}
