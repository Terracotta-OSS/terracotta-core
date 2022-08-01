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
package org.terracotta.entity.map;

import java.nio.ByteBuffer;

/**
 *
 */
public class MapConfig {
  
  private final int concurrency;
  private final String name;

  public MapConfig(byte[] configuration) {
    concurrency = ByteBuffer.wrap(configuration).getInt();
    name = new String(configuration, 4, configuration.length - 4);
  }
  
  public MapConfig(int concurrency, String name) {
    this.concurrency = concurrency;
    this.name = name;
  }
  
  public byte[] getBytes() {
    ByteBuffer buf = ByteBuffer.allocate(4 + name.getBytes().length);
    buf.putInt(concurrency);
    buf.put(name.getBytes());
    return buf.array();
  }

  public int getConcurrency() {
    return concurrency;
  }

  public String getName() {
    return name;
  }
}
