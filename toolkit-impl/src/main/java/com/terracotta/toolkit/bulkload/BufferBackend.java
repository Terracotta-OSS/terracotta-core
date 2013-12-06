package com.terracotta.toolkit.bulkload;

import java.util.Map;

/**
 * @author tim
 */
public interface BufferBackend<K, V> {

  /**
   * Dump out the local buffered changes into the target.
   *
   * @param buffer locally buffered up changes
   */
  void drain(Map<K, BufferedOperation<V>> buffer);
}
