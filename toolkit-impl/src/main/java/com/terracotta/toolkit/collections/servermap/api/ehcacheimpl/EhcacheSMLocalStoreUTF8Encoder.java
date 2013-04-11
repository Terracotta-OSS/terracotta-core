/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.servermap.api.ehcacheimpl;

import com.tc.object.dna.impl.UTF8ByteDataHolder;

class EhcacheSMLocalStoreUTF8Encoder {
  private final boolean useEncoding;

  public EhcacheSMLocalStoreUTF8Encoder(boolean useEncoding) {
    this.useEncoding = useEncoding;
  }

  public Object encodeKey(Object key) {

    if (useEncoding && key instanceof String) {
      return new UTF8ByteDataHolder((String) key);
    } else {
      return key;
    }

  }

  public Object decodeKey(Object key) {
    if (useEncoding && key instanceof UTF8ByteDataHolder) {
      return ((UTF8ByteDataHolder) key).asString();
    } else {
      return key;
    }
  }
}
