package com.tc.server;

import org.apache.commons.lang.ArrayUtils;

/**
 * Base class for all typed events.
 *
 * @author Eugene Shelestovich
 */
public final class BasicServerEvent implements ServerEvent {

  private final String cacheName;
  private final Object key;
  private ServerEventType type;
  private byte[] value;

  public BasicServerEvent(final ServerEventType type, final Object key, final String cacheName) {
    this(type, key, ArrayUtils.EMPTY_BYTE_ARRAY, cacheName);
  }

  public BasicServerEvent(final ServerEventType type, final Object key, byte[] value, final String cacheName) {
    this.type = type;
    this.key = key;
    this.value = value;
    this.cacheName = cacheName;
  }

  @Override
  public String getCacheName() {
    return cacheName;
  }

  @Override
  public ServerEventType getType() {
    return type;
  }

  @Override
  public void setType(final ServerEventType type) {
    this.type = type;
  }

  @Override
  public Object getKey() {
    return key;
  }

  @Override
  public byte[] getValue() {
    return value;
  }

  @Override
  public void setValue(final byte[] value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "ServerEvent" +
           "{type=" + type +
           ", key=" + key +
           ", cacheName='" + cacheName + '\'' +
           '}';
  }
}
