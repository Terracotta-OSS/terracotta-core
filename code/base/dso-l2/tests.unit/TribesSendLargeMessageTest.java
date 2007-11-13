import com.tc.l2.msg.GCResultMessage;
import com.tc.l2.msg.GCResultMessageFactory;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupMessageListener;
import com.tc.net.groups.Node;
import com.tc.net.groups.NodeID;
import com.tc.net.groups.TribesGroupManager;
import com.tc.object.ObjectID;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.ObjectIDSet2;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Random;

public class TribesSendLargeMessageTest extends TCTestCase {

  private static final TCLogger logger      = TCLogging.getLogger(TribesGroupManager.class);
  private static short          portnum     = 0;
  private static final long     millionOids = 1024 * 1024;

  public TribesSendLargeMessageTest() {
    // use random mcast port for testing purpose.
    useRandomMcastPort();
  }

  /*
   * Choose a random mcast port number to avoid conflict with other LAN machines. Must be called before joinMcast.
   */
  public void useRandomMcastPort() {
    if (portnum == 0) {
      // generate a random port number
      Random r = new Random();
      r.setSeed(System.currentTimeMillis());
      portnum = (short) (r.nextInt(Short.MAX_VALUE - 1025) + 1024);
    }

    TCPropertiesImpl.setProperty("l2.nha.tribes.mcast.mcastPort", String.valueOf(portnum));
    logger.info("McastService uses random mcast port: " + portnum);
  }

  public void baseTestSendingReceivingMessagesStatic(long oidsCount) throws Exception {
    logger.info("Test with ObjectIDs size " + oidsCount);
    TCPropertiesImpl.setProperty("l2.nha.mcast.enabled", "false");
    PortChooser pc = new PortChooser();
    final int p1 = pc.chooseRandomPort();
    final int p2 = pc.chooseRandomPort();
    final Node[] allNodes = new Node[] { new Node("localhost", p1), new Node("localhost", p2) };

    TribesGroupManager gm1 = new TribesGroupManager();
    MyListener l1 = new MyListener();
    gm1.registerForMessages(GCResultMessage.class, l1);
    NodeID n1 = gm1.join(allNodes[0], allNodes);

    TribesGroupManager gm2 = new TribesGroupManager();
    MyListener l2 = new MyListener();
    gm2.registerForMessages(GCResultMessage.class, l2);
    NodeID n2 = gm2.join(allNodes[1], allNodes);
    assertNotEquals(n1, n2);
    checkSendingReceivingMessages(gm1, l1, gm2, l2, oidsCount);
    gm1.stop();
    gm2.stop();
  }

  public void testSendingReceivingMessagesStatic4M() throws Exception {
    baseTestSendingReceivingMessagesStatic(millionOids * 4);
  }

  public void testSendingReceivingMessagesStatic5M() throws Exception {
    baseTestSendingReceivingMessagesStatic(millionOids * 5);
  }

  public void testSendingReceivingMessagesStatic10M() throws Exception {
    baseTestSendingReceivingMessagesStatic(millionOids * 10);
  }

  public void testSendingReceivingMessagesStatic20M() throws Exception {
    baseTestSendingReceivingMessagesStatic(millionOids * 20);
  }

  public void testSendingReceivingMessagesStatic40M() throws Exception {
    baseTestSendingReceivingMessagesStatic(millionOids * 40);
  }

  // public void testSendingReceivingMessagesMcast() throws Exception {
  // long oidsCount = millionOids * 5;
  // TCPropertiesImpl.setProperty("l2.nha.mcast.enabled", "true");
  // TribesGroupManager gm1 = new TribesGroupManager();
  // MyListener l1 = new MyListener();
  // gm1.registerForMessages(GCResultMessage.class, l1);
  // NodeID n1 = gm1.join(null, null);
  // TribesGroupManager gm2 = new TribesGroupManager();
  // MyListener l2 = new MyListener();
  // gm2.registerForMessages(GCResultMessage.class, l2);
  // NodeID n2 = gm2.join(null, null);
  // assertNotEquals(n1, n2);
  // checkSendingReceivingMessages(gm1, l1, gm2, l2, oidsCount);
  // gm1.stop();
  // gm2.stop();
  // }

  private void checkSendingReceivingMessages(TribesGroupManager gm1, MyListener l1, TribesGroupManager gm2,
                                             MyListener l2, long oidsCount) throws GroupException {
    ThreadUtil.reallySleep(5 * 1000);

    ObjectIDSet2 oidSet = new ObjectIDSet2();
    for (long i = 1; i <= oidsCount; ++i) {
      oidSet.add(new ObjectID(i));
    }
    final GCResultMessage msg1 = GCResultMessageFactory.createGCResultMessage(oidSet);
    gm1.sendAll(msg1);

    GCResultMessage msg2 = (GCResultMessage) l2.take();

    assertEquals(msg1.getGCedObjectIDs(), msg2.getGCedObjectIDs());

    oidSet = new ObjectIDSet2();
    for (long i = (oidsCount + 1); i <= (oidsCount * 2); ++i) {
      oidSet.add(new ObjectID(i));
    }
    final GCResultMessage msg3 = GCResultMessageFactory.createGCResultMessage(oidSet);
    gm2.sendAll(msg3);

    GCResultMessage msg4 = (GCResultMessage) l1.take();

    assertEquals(msg3.getGCedObjectIDs(), msg4.getGCedObjectIDs());

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
}
