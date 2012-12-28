/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.util;

public interface KeyValueGenerator<K, V> {
  K getKey(int i);

  V getValue(int i);

  public abstract static class Factory {
    public static KeyValueGenerator<Integer, Integer> createIntInt() {
      return new IntIntKeyValueGenerator();
    }

    public static KeyValueGenerator<Integer, TCInt> createIntTCInt() {
      return new IntKeyNonLiteralValueGenerator();
    }

    public static KeyValueGenerator<String, String> createStringString() {
      return new LiteralKeyLiteralValueGenerator();
    }

    public static KeyValueGenerator<String, TCInt> createStringTCInt() {
      return new LiteralKeyNonLiteralValueGenerator();
    }

    public static KeyValueGenerator<TCInt, String> createTCIntString() {
      return new NonLiteralKeyLiteralValueGenerator();
    }

    public static KeyValueGenerator<TCInt, TCInt> createTCIntTCInt() {
      return new NonLiteralKeyNonLiteralValueGenerator();
    }
  }
}
