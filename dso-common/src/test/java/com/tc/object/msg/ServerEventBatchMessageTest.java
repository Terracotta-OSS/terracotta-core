package com.tc.object.msg;

import org.junit.Before;
import org.junit.Test;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.TestMessageChannel;
import com.tc.object.session.SessionID;
import com.tc.server.BasicServerEvent;
import com.tc.server.ServerEvent;

import java.util.Arrays;
import java.util.List;

import static com.tc.server.ServerEventType.EVICT;
import static com.tc.server.ServerEventType.PUT;
import static com.tc.server.ServerEventType.REMOVE;
import static org.junit.Assert.assertEquals;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventBatchMessageTest {

  private ServerEventBatchMessageImpl msg;
  private TCByteBufferOutputStream out;
  private MessageMonitor monitor;
  private MessageChannel channel;

  @Before
  public void setUp() throws Exception {
    this.monitor = new NullMessageMonitor();
    this.channel = new TestMessageChannel();
    this.out = new TCByteBufferOutputStream(4, 4096, false);
    this.msg = new ServerEventBatchMessageImpl(new SessionID(0), this.monitor, this.out, this.channel,
        TCMessageType.SERVER_EVENT_BATCH_MESSAGE);
  }

  @Test
  public void testEncodeDecodeMustProduceInitialResult() throws Exception {
    final ServerEvent event1 = new BasicServerEvent(EVICT, "key-1", "cache1");
    final ServerEvent event2 = new BasicServerEvent(PUT, "key-2", "cache3");
    final ServerEvent event3 = new BasicServerEvent(REMOVE, "key-3", "cache2");
    final List<ServerEvent> events = Arrays.asList(event1, event2, event3);

    this.msg.setEvents(events);
    this.msg.dehydrate();

    final TCByteBuffer[] data = this.out.toArray();
    final TCMessageHeader header = (TCMessageHeader) this.msg.getHeader();
    this.msg = new ServerEventBatchMessageImpl(SessionID.NULL_ID, this.monitor, this.channel, header, data);
    this.msg.hydrate();

    assertEquals(events, this.msg.getEvents());
  }

}
