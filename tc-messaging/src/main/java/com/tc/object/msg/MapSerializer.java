/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */

package com.tc.object.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import java.io.IOException;
import java.util.Map;

/**
 * @author tim
 */
public abstract class MapSerializer<K, V> implements TCSerializable<MapSerializer<K, V>> {
  private final Map<K, V> map;

  protected MapSerializer(Map<K, V> map) {
    this.map = map;
  }

  protected abstract void serializeKey(K key, TCByteBufferOutput serialOutput);

  protected abstract void serializeValue(V value, TCByteBufferOutput serialOutput);

  protected abstract K deserializeKey(TCByteBufferInput serialInput) throws IOException;

  protected abstract V deserializeValue(TCByteBufferInput serialInput) throws IOException;

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(map.size());
    for (Map.Entry<K, V> kvEntry : map.entrySet()) {
      serializeKey(kvEntry.getKey(), serialOutput);
      serializeValue(kvEntry.getValue(), serialOutput);
    }
  }

  @Override
  public MapSerializer<K, V> deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    int size = serialInput.readInt();
    for (int i = 0; i < size; i++) {
      map.put(deserializeKey(serialInput), deserializeValue(serialInput));
    }
    return this;
  }
  
  public Map<K, V> getMappings() {
    return map;
  }
}
