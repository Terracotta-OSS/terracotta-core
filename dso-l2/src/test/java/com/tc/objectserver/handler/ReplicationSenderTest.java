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
package com.tc.objectserver.handler;

import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.SpecializedEventContext;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.l2.msg.ReplicationAddPassiveIntent;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.msg.ReplicationReplicateMessageIntent;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupManager;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.stats.Stats;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;


public class ReplicationSenderTest {
  NodeID node = mock(NodeID.class);
  @SuppressWarnings("unchecked")
  GroupManager<AbstractGroupMessage> groupMgr = mock(GroupManager.class);
  List<ReplicationMessage> collector = new LinkedList<>();
  ReplicationSender testSender = new ReplicationSender(groupMgr);
  Sink<NodeID> sink = new Sink<NodeID>() {
    @Override
    public void enableStatsCollection(boolean enable) {
      Assert.fail("Not in test");
    }
    @Override
    public boolean isStatsCollectionEnabled() {
      Assert.fail("Not in test");
      return false;
    }
    @Override
    public Stats getStats(long frequency) {
      Assert.fail("Not in test");
      return null;
    }
    @Override
    public Stats getStatsAndReset(long frequency) {
      Assert.fail("Not in test");
      return null;
    }
    @Override
    public void resetStats() {
      Assert.fail("Not in test");
    }
    @Override
    public void addSingleThreaded(NodeID context) {
      try {
        testSender.handleEvent(context);
      } catch (EventHandlerException e) {
        Assert.fail(e.getLocalizedMessage());
      }
    }
    @Override
    public void addMultiThreaded(NodeID context) {
      Assert.fail("Not in test");
    }
    @Override
    public void addSpecialized(SpecializedEventContext specialized) {
      Assert.fail("Not in test");
    }
    @Override
    public int size() {
      Assert.fail("Not in test");
      return 0;
    }
    @Override
    public void clear() {
      Assert.fail("Not in test");
    }
    @Override
    public void setClosed(boolean closed) {
      Assert.fail("Not in test");
    }};
  EntityID entity = EntityID.NULL_ID;
  int concurrency = 1;
  
  public ReplicationSenderTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() throws Exception {
    this.testSender.setSelfSink(this.sink);
    doAnswer((invoke)-> {
      Object[] args = invoke.getArguments();
      // We need to emulate having sent the message so run it through the serialization mechanism.
      ReplicationMessage sending = (ReplicationMessage)args[1];
      TCByteBufferOutput output = new TCByteBufferOutputStream();
      sending.serializeTo(output);
      TCByteBufferInput input = new TCByteBufferInputStream(output.toArray());
      ReplicationMessage receiving = new ReplicationMessage();
      try {
        receiving.deserializeFrom(input);
      } catch (IOException e) {
        // Not expected in test.
        e.printStackTrace();
        Assert.fail(e.getLocalizedMessage());
      }
      collector.add(receiving);
      return null;
    }).when(groupMgr).sendTo(Matchers.any(NodeID.class), Matchers.any(ReplicationMessage.class));
  }
  
  private SyncReplicationActivity makeMessage(SyncReplicationActivity.ActivityType type) {
    switch (type) {
      case CREATE_ENTITY:
      case DESTROY_ENTITY:
      case INVOKE_ACTION:
      case RECONFIGURE_ENTITY:
        ClientID source = new ClientID(1);
        return SyncReplicationActivity.createReplicatedMessage(new EntityDescriptor(entity, ClientInstanceID.NULL_ID, 1), source, TransactionID.NULL_ID, TransactionID.NULL_ID, type, new byte[0], concurrency, "");
      case ORDERING_PLACEHOLDER:
        return SyncReplicationActivity.createOrderingPlaceholder(new EntityDescriptor(entity, ClientInstanceID.NULL_ID, 1), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, "");
      case FLUSH_LOCAL_PIPELINE:
        return SyncReplicationActivity.createFlushLocalPipelineMessage(entity, 1);
      case SYNC_BEGIN:
        return SyncReplicationActivity.createStartSyncMessage(new SyncReplicationActivity.EntityCreationTuple[0] );
      case SYNC_END:
        return SyncReplicationActivity.createEndSyncMessage(new byte[0]);
      case SYNC_ENTITY_BEGIN:
        return SyncReplicationActivity.createStartEntityMessage(entity, 1, new byte[0], 0);
      case SYNC_ENTITY_CONCURRENCY_BEGIN:
        return SyncReplicationActivity.createStartEntityKeyMessage(entity, 1, concurrency);
      case SYNC_ENTITY_CONCURRENCY_END:
        return SyncReplicationActivity.createEndEntityKeyMessage(entity, 1, concurrency++);
      case SYNC_ENTITY_CONCURRENCY_PAYLOAD:
        return SyncReplicationActivity.createPayloadMessage(entity, 1, concurrency, new byte[0], "");
      case SYNC_ENTITY_END:
        return SyncReplicationActivity.createEndEntityMessage(entity, 1);
      default:
        throw new AssertionError("bad message type");
    }
  }
  
  @After
  public void tearDown() {
    
  }
  
  @Test
  public void filterSCDC() throws Exception {  // Sync-Create-Delete-Create
    entity = new EntityID("TEST", "test");
    List<SyncReplicationActivity> origin = new LinkedList<>();
    List<SyncReplicationActivity> validation = new LinkedList<>();
    buildTest(origin, validation, SyncReplicationActivity.createStartMessage(), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_BEGIN), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), true);  
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_ENTITY_BEGIN), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_BEGIN), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_PAYLOAD), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_END), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_ENTITY_END), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_END), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), false);

    origin.stream().forEach(activity-> {
      testSender.replicateMessage(ReplicationReplicateMessageIntent.createReplicatedMessageEnvelope(node, activity, null));
      });
    System.err.println("filter SDSC");
    validateCollector(validation);
  }  
  
  @Test
  public void filterCDC() throws Exception {  // Create-Delete-Create
    entity = new EntityID("TEST", "test");
 //  creates and sync can no longer intermix
    List<SyncReplicationActivity> origin = new LinkedList<>();
    List<SyncReplicationActivity> validation = new LinkedList<>();
    buildTest(origin, validation, SyncReplicationActivity.createStartMessage(), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_BEGIN), false);
 // this create is not part of the sync set so everything should pass through
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.CREATE_ENTITY), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), false);  // invoke actions are valid since the stream is working off the create

    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER), true);
// create and destroy can no longer happen concurrently with sync
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_END), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), false);

    origin.stream().forEach(msg-> {
      testSender.replicateMessage(ReplicationReplicateMessageIntent.createReplicatedMessageEnvelope(node, msg, null));
      });
    
    System.err.println("filter CDC");
    validateCollector(validation);
  }  

  @Test
  public void filterValidation() throws Exception {
    entity = new EntityID("TEST", "test");
    List<SyncReplicationActivity> origin = new LinkedList<>();
    List<SyncReplicationActivity> validation = new LinkedList<>();
    buildTest(origin, validation, SyncReplicationActivity.createStartMessage(), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.CREATE_ENTITY), true);//  this will be replicated, it's up to the passive to drop it on the floor if it hasn't seen a sync yet
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_BEGIN), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), true);   
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_ENTITY_BEGIN), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_BEGIN), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_PAYLOAD), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_END), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_ENTITY_END), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_END), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), false);

    origin.stream().forEach(msg-> {
      testSender.replicateMessage(ReplicationReplicateMessageIntent.createReplicatedMessageEnvelope(node, msg, null));
      });
    
    validateCollector(validation);
  }
  
  @Test
  public void validateSyncState() {
    entity = new EntityID("TEST", "test");
    List<SyncReplicationActivity> origin = new LinkedList<>();
    List<SyncReplicationActivity> validation = new LinkedList<>();
    buildTest(origin, validation, SyncReplicationActivity.createStartMessage(), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.FLUSH_LOCAL_PIPELINE), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.FLUSH_LOCAL_PIPELINE), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.CREATE_ENTITY), false);//  this will be replicated, it's up to the passive to drop it on the floor if it hasn't seen a sync yet
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_BEGIN), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), true);   
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_ENTITY_BEGIN), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_BEGIN), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.FLUSH_LOCAL_PIPELINE), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_PAYLOAD), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.FLUSH_LOCAL_PIPELINE), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_END), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), true);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_ENTITY_END), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.SYNC_END), false);
    buildTest(origin, validation, makeMessage(SyncReplicationActivity.ActivityType.INVOKE_ACTION), false);

    SetOnceFlag started = new SetOnceFlag();
    SetOnceFlag finished = new SetOnceFlag();
    origin.stream().forEach(activity-> {
        SyncReplicationActivity.ActivityType activityType = activity.getActivityType();
        if (SyncReplicationActivity.ActivityType.SYNC_BEGIN == activityType) {
          started.set();
        } else if (SyncReplicationActivity.ActivityType.SYNC_END == activityType) {
          finished.set();
        }
        // We normally don't send the local flush operations.
        if (SyncReplicationActivity.ActivityType.FLUSH_LOCAL_PIPELINE != activityType) {
          SetOnceFlag sent = new SetOnceFlag();
          SetOnceFlag notsent = new SetOnceFlag();
          if (SyncReplicationActivity.ActivityType.SYNC_START == activityType) {
            this.testSender.addPassive(ReplicationAddPassiveIntent.createAddPassiveEnvelope(node, activity, ()->sent.set(), ()->notsent.set()));
          } else {
            this.testSender.replicateMessage(ReplicationReplicateMessageIntent.createReplicatedMessageDebugEnvelope(node, activity, ()->sent.set(), ()->notsent.set()));
          }
          Assert.assertEquals(started.isSet() + " " + finished.isSet(), started.isSet() && !finished.isSet(), testSender.isSyncOccuring(node));
          if (!testSender.isSyncOccuring(node) && (SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER != activityType)) {
            Assert.assertTrue(activity, sent.isSet());
          }
        }
    });
    
    validateCollector(validation);
  }
  
  private void validateCollector(Collection<SyncReplicationActivity> valid) {
    Iterator<SyncReplicationActivity> next = valid.iterator();
    collector.stream().forEach(msg->{
      for (SyncReplicationActivity activity : msg.getActivities()) {
        SyncReplicationActivity.ActivityType activityType = activity.getActivityType();
        if ((activityType != SyncReplicationActivity.ActivityType.SYNC_START) && (activityType != SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER)) {
          SyncReplicationActivity nextActivity = next.next();
          SyncReplicationActivity.ActivityType nextActivityType = nextActivity.getActivityType();
          if (nextActivityType != SyncReplicationActivity.ActivityType.SYNC_BEGIN &&
              nextActivityType != SyncReplicationActivity.ActivityType.SYNC_END) {
          }
          Assert.assertEquals(activityType, nextActivityType);
          Assert.assertEquals(activity.getConcurrency(), nextActivity.getConcurrency());
          System.err.println(nextActivityType + " on " + nextActivity.getEntityID());
        }
      }
    });
  }
  
  private void buildTest(List<SyncReplicationActivity> origin, List<SyncReplicationActivity> validation, SyncReplicationActivity activity, boolean filtered) {
    origin.add(activity);
    if (!filtered) {
      validation.add(activity);
    }
  }
}
