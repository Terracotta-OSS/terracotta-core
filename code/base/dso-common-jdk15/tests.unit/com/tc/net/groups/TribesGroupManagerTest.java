/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.test.TCTestCase;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class TribesGroupManagerTest extends TCTestCase {

  public TribesGroupManagerTest() {
//    disableTestUntil("testIfTribesGroupManagerLoads", "2007-04-27");
  }

  public void testIfTribesGroupManagerLoads() throws Exception {
    GroupManager gm = GroupManagerFactory.createGroupManager();
    assertNotNull(gm);
    assertEquals(TribesGroupManager.class.getName(), gm.getClass().getName());
  }

  public void testSendingReceivingMessages() throws Exception {
    PortChooser pc = new PortChooser();
    final int p1 = pc.chooseRandomPort();
    final int p2 = pc.chooseRandomPort();
    final Node[] allNodes = new Node[] { new Node("localhost", p1), new Node("localhost", p2) };

    TribesGroupManager gm1 = new TribesGroupManager();
    MyListener l1 = new MyListener();
    gm1.registerForMessages(TestMessage.class, l1);
    NodeID n1 = gm1.join(allNodes[0], allNodes);

    TribesGroupManager gm2 = new TribesGroupManager();
    MyListener l2 = new MyListener();
    gm2.registerForMessages(TestMessage.class, l2);
    NodeID n2 = gm2.join(allNodes[1], allNodes);

    assertNotEquals(n1, n2);

    TestMessage m1 = new TestMessage("Hello there");
    gm1.sendAll(m1);

    TestMessage m2 = (TestMessage) l2.take();
    System.err.println(m2);

    assertEquals(m1, m2);

    TestMessage m3 = new TestMessage("Hello back");
    gm2.sendAll(m3);

    TestMessage m4 = (TestMessage) l1.take();
    System.err.println(m4);

    assertEquals(m3, m4);

  }

  private static final class MyListener implements GroupMessageListener {

    NoExceptionLinkedQueue queue = new NoExceptionLinkedQueue();

    public void messageReceived(NodeID fromNode, GroupMessage msg) {
      queue.put(msg);
    }

    public GroupMessage take() {
      return (GroupMessage) queue.take();
    }

  }

  private static final class TestMessage extends AbstractGroupMessage {

    // to make serialization sane
    public TestMessage() {
      super(0);
    }

    public TestMessage(String message) {
      super(0);
      this.msg = message;
    }

    String msg;

    @Override
    protected void basicReadExternal(int msgType, ObjectInput in) throws IOException {
      msg = in.readUTF();

    }

    @Override
    protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
      out.writeUTF(msg);

    }

    public int hashCode() {
      return msg.hashCode();
    }

    public boolean equals(Object o) {
      if (o instanceof TestMessage) {
        TestMessage other = (TestMessage) o;
        return this.msg.equals(other.msg);
      }
      return false;
    }

    public String toString() {
      return "TestMessage [ " + msg + "]";
    }
  }
}
