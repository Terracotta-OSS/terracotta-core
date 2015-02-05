package com.tc.server;

import java.util.Arrays;

/**
 * Base class for all typed events.
 *
 * @author Eugene Shelestovich
 */
public final class BasicServerEvent implements VersionedServerEvent {

  private static final byte[] EMPTY_BYTE_ARRAY = new byte[] {};

  private final String cacheName;
  private final Object key;
  private final long version;

  private ServerEventType type;
  private byte[] value;

  public BasicServerEvent(ServerEventType type, Object key, String cacheName) {
    this(type, key, EMPTY_BYTE_ARRAY, DEFAULT_VERSION, cacheName);
  }

  public BasicServerEvent(ServerEventType type, Object key, byte[] value, String cacheName) {
    this(type, key, value, DEFAULT_VERSION, cacheName);
  }

  public BasicServerEvent(ServerEventType type, Object key, long version, String cacheName) {
    this(type, key, EMPTY_BYTE_ARRAY, version, cacheName);
  }

  public BasicServerEvent(ServerEventType type, Object key, byte[] value, long version, String cacheName) {
    this.type = type;
    this.key = key;
    this.value = value;
    this.cacheName = cacheName;
    this.version = version;
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
  public void setType(ServerEventType type) {
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
  public void setValue(byte[] value) {
    this.value = value;
  }

  @Override
  public long getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final BasicServerEvent that = (BasicServerEvent) o;

    if (version != that.version) return false;
    if (!cacheName.equals(that.cacheName)) return false;
    if (!key.equals(that.key)) return false;
    if (type != that.type) return false;
    if (!Arrays.equals(value, that.value)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = cacheName.hashCode();
    result = 31 * result + key.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + (value != null ? Arrays.hashCode(value) : 0);
    result = 31 * result + (int) (version ^ (version >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "ServerEvent" +
           "{type=" + type +
           ", key=" + key +
           ", value size=" + value.length +
           ", version=" + version +
           ", cacheName='" + cacheName + '\'' +
           '}';
  }
}
