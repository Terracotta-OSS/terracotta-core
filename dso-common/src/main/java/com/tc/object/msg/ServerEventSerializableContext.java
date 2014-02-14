package com.tc.object.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.SerializerDNAEncodingImpl;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.server.BasicServerEvent;
import com.tc.server.CustomLifespanVersionedServerEvent;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;
import com.tc.server.VersionedServerEvent;

import java.io.IOException;

/**
 * @author Eugene Shelestovich
 */
class ServerEventSerializableContext implements TCSerializable {

  private static final DNAEncoding serializer = new SerializerDNAEncodingImpl();

  private ServerEvent event;

  public ServerEventSerializableContext() {
  }

  public ServerEventSerializableContext(final ServerEvent event) {
    this.event = event;
  }

  @Override
  public void serializeTo(final TCByteBufferOutput out) {
    serializer.encode(event.getType().ordinal(), out);
    serializer.encode(event.getCacheName(), out);
    serializer.encode(event.getKey(), out);
    serializer.encode(event.getValue(), out);
    // Note: This is an ugly hack, but it will work for now. Should fix it soon.
    // Currently every event is a VersionedServerEvent, there is no implementation for ServerEvent
    serializer.encode(((VersionedServerEvent) event).getVersion(), out);

    boolean customLifespanEvent = (event instanceof CustomLifespanVersionedServerEvent);
    serializer.encode(customLifespanEvent, out);
    if (customLifespanEvent) {
      CustomLifespanVersionedServerEvent customLifespanVersionedServerEvent = (CustomLifespanVersionedServerEvent) event;
      serializer.encode(customLifespanVersionedServerEvent.getCreationTimeInSeconds(), out);
      serializer.encode(customLifespanVersionedServerEvent.getTimeToIdle(), out);
      serializer.encode(customLifespanVersionedServerEvent.getTimeToLive(), out);
    }
  }

  @Override
  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    try {
      int index = (Integer) serializer.decode(in);
      final ServerEventType type = ServerEventType.values()[index];
      final String destination = (String) serializer.decode(in);
      final Object key = serializer.decode(in);
      final byte[] value = (byte[]) serializer.decode(in);
      final long version = (Long) serializer.decode(in);
      final VersionedServerEvent versionedEvent = new BasicServerEvent(type, extractStringIfNecessary(key),
          value, version, destination);

      boolean customLifespanEvent = (Boolean) serializer.decode(in);
      if (customLifespanEvent) {
        final int creationTime = (Integer) serializer.decode(in);
        final int timeToIdle = (Integer) serializer.decode(in);
        final int timeToLive = (Integer) serializer.decode(in);
        event = new CustomLifespanVersionedServerEvent(versionedEvent, creationTime, timeToIdle, timeToLive);
      } else {
        event = versionedEvent;
      }
    } catch (ClassNotFoundException e) {
      throw new AssertionError(e);
    }
    return this;
  }

  /**
   * Transform a key from internal representation to a string, if necessary.
   */
  private static Object extractStringIfNecessary(final Object key) {
    final Object normalizedKey;
    if (key instanceof UTF8ByteDataHolder) {
      normalizedKey = ((UTF8ByteDataHolder) key).asString();
    } else {
      normalizedKey = key;
    }
    return normalizedKey;
  }

  public ServerEvent getEvent() {
    return event;
  }
}
