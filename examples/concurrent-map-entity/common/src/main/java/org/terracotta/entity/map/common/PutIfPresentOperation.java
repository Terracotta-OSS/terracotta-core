/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.entity.map.common;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PutIfPresentOperation implements KeyedOperation {
  private final Object key;
  private final Object value;

  public PutIfPresentOperation(Object key, Object value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public Object getKey() {
    return key;
  }

  public Object getValue() {
    return value;
  }

  @Override
  public Type operationType() {
    return Type.PUT_IF_PRESENT;
  }

  @Override
  public void writeTo(DataOutput output) throws IOException {
    PrimitiveCodec.writeTo(output, key);
    PrimitiveCodec.writeTo(output, value);
  }

  static PutIfPresentOperation readFrom(DataInput dataInput) throws IOException {
    return new PutIfPresentOperation(PrimitiveCodec.readFrom(dataInput), PrimitiveCodec.readFrom(dataInput));
  }
}
