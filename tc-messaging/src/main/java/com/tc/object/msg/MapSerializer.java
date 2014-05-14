package com.tc.object.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * @author tim
 */
public abstract class MapSerializer<K, V> implements TCSerializable {
  private final Map<K, V> map;

  protected MapSerializer(final Map<K, V> map) {
    this.map = map;
  }

  protected abstract void serializeKey(K key, TCByteBufferOutput serialOutput);

  protected abstract void serializeValue(V value, TCByteBufferOutput serialOutput);

  protected abstract K deserializeKey(TCByteBufferInput serialInput) throws IOException;

  protected abstract V deserializeValue(TCByteBufferInput serialInput) throws IOException;

  @Override
  public void serializeTo(final TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(map.size());
    for (Map.Entry<K, V> kvEntry : map.entrySet()) {
      serializeKey(kvEntry.getKey(), serialOutput);
      serializeValue(kvEntry.getValue(), serialOutput);
    }
  }

  @Override
  public Map<K, V> deserializeFrom(final TCByteBufferInput serialInput) throws IOException {
    int size = serialInput.readInt();
    for (int i = 0; i < size; i++) {
      map.put(deserializeKey(serialInput), deserializeValue(serialInput));
    }
    return map;
  }
}
