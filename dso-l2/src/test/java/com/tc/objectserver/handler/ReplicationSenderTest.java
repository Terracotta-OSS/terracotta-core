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
import com.tc.l2.msg.PassiveSyncMessage;
import com.tc.l2.msg.ReplicationEnvelope;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.msg.ReplicationMessage.ReplicationType;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupManager;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;
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

/**
 *
 */
public class ReplicationSenderTest {
  
  NodeID node = mock(NodeID.class);
  GroupManager groupMgr = mock(GroupManager.class);
  List<ReplicationEnvelope> collector = new LinkedList<>();
  ReplicationSender testSender = new ReplicationSender(groupMgr);
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
    doAnswer((invoke)-> {
      Object[] args = invoke.getArguments();
      collector.add(((ReplicationMessage)args[1]).target((NodeID)args[0]));
      return null;
    }).when(groupMgr).sendTo(Matchers.any(NodeID.class), Matchers.any(ReplicationMessage.class));
  }
  
  private void makeAndSendSequence(Collection<ReplicationType> list) throws Exception {
    list.stream().forEach(msg->{
      ReplicationMessage rep = makeMessage(msg);
      try {
        testSender.handleEvent(rep.target(node));
      } catch (EventHandlerException exp) {
        throw new RuntimeException(exp);
      }
    });
  }
  
  private ReplicationMessage makeMessage(ReplicationType type) {
    switch (type) {
      case CREATE_ENTITY:
      case DESTROY_ENTITY:
      case INVOKE_ACTION:
      case NOOP:
      case RECONFIGURE_ENTITY:
      case RELEASE_ENTITY:
        return ReplicationMessage.createReplicatedMessage(new EntityDescriptor(entity, ClientInstanceID.NULL_ID, 1), ClientID.NULL_ID, TransactionID.NULL_ID, TransactionID.NULL_ID, type, new byte[0], 0);
      case SYNC_BEGIN:
        return PassiveSyncMessage.createStartSyncMessage();
      case SYNC_END:
        return PassiveSyncMessage.createEndSyncMessage();
      case SYNC_ENTITY_BEGIN:
        return PassiveSyncMessage.createStartEntityMessage(entity, 1, new byte[0], true);
      case SYNC_ENTITY_CONCURRENCY_BEGIN:
        return PassiveSyncMessage.createStartEntityKeyMessage(entity, 1, concurrency);
      case SYNC_ENTITY_CONCURRENCY_END:
        return PassiveSyncMessage.createEndEntityKeyMessage(entity, 1, concurrency++);
      case SYNC_ENTITY_CONCURRENCY_PAYLOAD:
        return PassiveSyncMessage.createPayloadMessage(entity, 1, concurrency, new byte[0]);
      case SYNC_ENTITY_END:
        return PassiveSyncMessage.createEndEntityMessage(entity, 1);
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
    List<ReplicationMessage> origin = new LinkedList<>();
    List<ReplicationMessage> validation = new LinkedList<>();
    buildTest(origin, validation, ReplicationMessage.createStartMessage(), true);
    buildTest(origin, validation, makeMessage(ReplicationType.NOOP), true);
    buildTest(origin, validation, makeMessage(ReplicationType.NOOP), true);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_BEGIN), false);
    buildTest(origin, validation, makeMessage(ReplicationType.INVOKE_ACTION), true);  
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_ENTITY_BEGIN), false);
    buildTest(origin, validation, makeMessage(ReplicationType.CREATE_ENTITY), true);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_ENTITY_CONCURRENCY_BEGIN), false);
    buildTest(origin, validation, makeMessage(ReplicationType.INVOKE_ACTION), true);
    buildTest(origin, validation, makeMessage(ReplicationType.NOOP), true);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_ENTITY_CONCURRENCY_PAYLOAD), false);
    buildTest(origin, validation, makeMessage(ReplicationType.INVOKE_ACTION), true);
    buildTest(origin, validation, makeMessage(ReplicationType.NOOP), true);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_ENTITY_CONCURRENCY_END), false);
    buildTest(origin, validation, makeMessage(ReplicationType.DESTROY_ENTITY), false);
    buildTest(origin, validation, makeMessage(ReplicationType.CREATE_ENTITY), false);
    buildTest(origin, validation, makeMessage(ReplicationType.INVOKE_ACTION), false);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_ENTITY_END), false);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_END), false);
    buildTest(origin, validation, makeMessage(ReplicationType.INVOKE_ACTION), false);

    origin.stream().forEach(msg-> {
      try {
        testSender.handleEvent(msg.target(node));
      } catch (EventHandlerException h) {
        throw new RuntimeException(h);
      }
    });
    
    validateCollector(validation);
  }  
  
  @Test
  public void filterCDC() throws Exception {  // Create-Delete-Create
    entity = new EntityID("TEST", "test");
    List<ReplicationMessage> origin = new LinkedList<>();
    List<ReplicationMessage> validation = new LinkedList<>();
    buildTest(origin, validation, ReplicationMessage.createStartMessage(), true);
    buildTest(origin, validation, makeMessage(ReplicationType.NOOP), true);
    buildTest(origin, validation, makeMessage(ReplicationType.NOOP), true);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_BEGIN), false);
    buildTest(origin, validation, makeMessage(ReplicationType.CREATE_ENTITY), false);
    buildTest(origin, validation, makeMessage(ReplicationType.INVOKE_ACTION), false);  // invoke actions are valid since the stream is working off the create
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_ENTITY_BEGIN), true);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_ENTITY_CONCURRENCY_BEGIN), true);
    buildTest(origin, validation, makeMessage(ReplicationType.INVOKE_ACTION), false);
    buildTest(origin, validation, makeMessage(ReplicationType.NOOP), true);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_ENTITY_CONCURRENCY_PAYLOAD), true);
    buildTest(origin, validation, makeMessage(ReplicationType.INVOKE_ACTION), false);
    buildTest(origin, validation, makeMessage(ReplicationType.NOOP), true);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_ENTITY_CONCURRENCY_END), true);
    buildTest(origin, validation, makeMessage(ReplicationType.DESTROY_ENTITY), false);
    buildTest(origin, validation, makeMessage(ReplicationType.CREATE_ENTITY), false);
    buildTest(origin, validation, makeMessage(ReplicationType.INVOKE_ACTION), false);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_ENTITY_END), true);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_END), false);
    buildTest(origin, validation, makeMessage(ReplicationType.INVOKE_ACTION), false);

    origin.stream().forEach(msg-> {
      try {
        testSender.handleEvent(msg.target(node));
      } catch (EventHandlerException h) {
        throw new RuntimeException(h);
      }
    });
    
    validateCollector(validation);
  }  

  @Test
  public void filterValidation() throws Exception {
    entity = new EntityID("TEST", "test");
    List<ReplicationMessage> origin = new LinkedList<>();
    List<ReplicationMessage> validation = new LinkedList<>();
    buildTest(origin, validation, ReplicationMessage.createStartMessage(), true);
    buildTest(origin, validation, makeMessage(ReplicationType.NOOP), true);
    buildTest(origin, validation, makeMessage(ReplicationType.NOOP), true);
    buildTest(origin, validation, makeMessage(ReplicationType.CREATE_ENTITY), false);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_BEGIN), false);
    buildTest(origin, validation, makeMessage(ReplicationType.INVOKE_ACTION), true);   // invoke actions are valid since the stream is working off the create
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_ENTITY_BEGIN), false);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_ENTITY_CONCURRENCY_BEGIN), false);
    buildTest(origin, validation, makeMessage(ReplicationType.INVOKE_ACTION), true);
    buildTest(origin, validation, makeMessage(ReplicationType.NOOP), true);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_ENTITY_CONCURRENCY_PAYLOAD), false);
    buildTest(origin, validation, makeMessage(ReplicationType.INVOKE_ACTION), true);
    buildTest(origin, validation, makeMessage(ReplicationType.NOOP), true);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_ENTITY_CONCURRENCY_END), false);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_ENTITY_END), false);
    buildTest(origin, validation, makeMessage(ReplicationType.SYNC_END), false);
    buildTest(origin, validation, makeMessage(ReplicationType.INVOKE_ACTION), false);

    origin.stream().forEach(msg-> {
      try {
        testSender.handleEvent(msg.target(node));
      } catch (EventHandlerException h) {
        throw new RuntimeException(h);
      }
    });
    
    validateCollector(validation);
  }
  
  private void validateCollector(Collection<ReplicationMessage> valid) {
    Iterator<ReplicationMessage> next = valid.iterator();
    collector.stream().forEach(cmsg->{
      ReplicationMessage vmsg = next.next();
      if (vmsg.getReplicationType() != ReplicationType.SYNC_BEGIN &&
          vmsg.getReplicationType() != ReplicationType.SYNC_END) {
        Assert.assertEquals(vmsg + "!=" + cmsg.getMessage(), vmsg.getEntityID(), cmsg.getMessage().getEntityID());
      }
      Assert.assertEquals(vmsg + "!=" + cmsg.getMessage(), vmsg.getReplicationType(), cmsg.getMessage().getReplicationType());
      Assert.assertEquals(vmsg + "!=" + cmsg.getMessage(), vmsg.getConcurrency(), cmsg.getMessage().getConcurrency());
      System.err.println(vmsg.getReplicationType() + " on " + vmsg.getEntityID());
    });
  }
  
  private void buildTest(List<ReplicationMessage> origin, List<ReplicationMessage> validation, ReplicationMessage msg, boolean filtered) {
    origin.add(msg);
    if (!filtered) validation.add(msg);
  }
}
