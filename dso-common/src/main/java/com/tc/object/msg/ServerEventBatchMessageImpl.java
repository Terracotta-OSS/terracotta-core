package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.SerializerDNAEncodingImpl;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.object.session.SessionID;
import com.tc.server.BasicServerEvent;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;
import com.tc.server.VersionedServerEvent;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventBatchMessageImpl extends DSOMessageBase implements ServerEventBatchMessage {

  private final static DNAEncoding encoder = new SerializerDNAEncodingImpl();
  private final static DNAEncoding decoder = new SerializerDNAEncodingImpl();

  private static final byte EVENTS_COUNT = 0;

  private List<ServerEvent> events = new ArrayList<ServerEvent>();

  public ServerEventBatchMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                     final TCByteBufferOutputStream out, final MessageChannel channel,
                                     final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ServerEventBatchMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                     final MessageChannel channel, final TCMessageHeader header,
                                     final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(EVENTS_COUNT, events.size());

    int count = 0;
    final TCByteBufferOutputStream outStream = getOutputStream();
    for (ServerEvent event : events) {
      encoder.encode(event.getType().ordinal(), outStream);
      encoder.encode(event.getCacheName(), outStream);
      encoder.encode(event.getKey(), outStream);
      encoder.encode(event.getValue(), outStream);
      if (event instanceof VersionedServerEvent) {
        encoder.encode(((VersionedServerEvent)event).getVersion(), outStream);
      }
      count++;
    }
    Assert.assertEquals(events.size(), count);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case EVENTS_COUNT:
        try {
          int count = getIntValue();
          events = new ArrayList<ServerEvent>(count);
          final TCByteBufferInputStream inputStream = getInputStream();
          while (count-- > 0) {
            int index = (Integer)decoder.decode(inputStream);
            final ServerEventType type = ServerEventType.values()[index];
            final String destination = (String)decoder.decode(inputStream);
            final Object key = decoder.decode(inputStream);
            final byte[] value = (byte[])decoder.decode(inputStream);
            final long version = (inputStream.available() > 0) ? (Long)decoder.decode(inputStream)
                : VersionedServerEvent.DEFAULT_VERSION;
            events.add(new BasicServerEvent(type, extractStringIfNecessary(key), value, version, destination));
          }
        } catch (ClassNotFoundException e) {
          throw new AssertionError(e);
        }
        return true;
      default:
        return false;
    }
  }

  /**
   * Transform a key from internal representation to string if necessary.
   */
  private static Object extractStringIfNecessary(final Object key) {
    final Object normalizedKey;
    if (key instanceof UTF8ByteDataHolder) {
      normalizedKey = ((UTF8ByteDataHolder)key).asString();
    } else {
      normalizedKey = key;
    }
    return normalizedKey;
  }

  @Override
  public void setEvents(final List<ServerEvent> events) {
    this.events = events;
  }

  @Override
  public List<ServerEvent> getEvents() {
    return events;
  }
}
