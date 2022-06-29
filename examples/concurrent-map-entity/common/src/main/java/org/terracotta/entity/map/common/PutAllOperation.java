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
import java.util.Map;

public class PutAllOperation implements MapOperation {
  private final Map<Object, Object> map;

  public PutAllOperation(Map<Object, Object> map) {
    this.map = map;
  }

  public Object getMap() {
    return map;
  }

  @Override
  public Type operationType() {
    return Type.PUT_ALL;
  }

  @Override
  public void writeTo(DataOutput output) throws IOException {
    PrimitiveCodec.writeTo(output, map);
  }

  @SuppressWarnings("unchecked")
  static PutAllOperation readFrom(DataInput input) throws IOException {
    return new PutAllOperation((Map<Object, Object>) PrimitiveCodec.readFrom(input));
  }
}
