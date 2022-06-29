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


public class SizeResponse implements MapResponse {
  private final long size;

  public SizeResponse(long size) {
    this.size = size;
  }

  public long getSize() {
    return this.size;
  }

  @Override
  public Type responseType() {
    return Type.SIZE;
  }

  @Override
  public void writeTo(DataOutput output) throws IOException {
    PrimitiveCodec.writeTo(output, this.size);
  }

  static SizeResponse readFrom(DataInput input) throws IOException {
    Long size = (Long) PrimitiveCodec.readFrom(input);
    return new SizeResponse(size.longValue());
  }
}
