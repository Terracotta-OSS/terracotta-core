package org.terracotta.passthrough;


/**
 * Defines the callback invoked on successful resolution of a fetch message.  The reason why we need a callback and not a
 * simple call-return structure is that the fetch may block on lock acquisition.
 */
public interface IFetchResult {
  public void onFetchComplete(byte[] config, Exception error);
}
