/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
