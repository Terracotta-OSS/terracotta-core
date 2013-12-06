package com.terracotta.toolkit.bulkload;

/**
 * @author tim
 */
public interface BufferedOperation<T> {
  Type getType();

  T getValue();

  boolean isVersioned();

  int getCreateTimeInSecs();

  int getCustomMaxTTISeconds();

  int getCustomMaxTTLSeconds();

  long getVersion();

  public static enum Type {
    PUT, PUT_IF_ABSENT, REMOVE
  }
}
