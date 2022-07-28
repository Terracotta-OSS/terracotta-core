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

import org.terracotta.entity.EntityMessage;

import java.io.DataOutput;
import java.io.IOException;


public interface MapOperation extends EntityMessage {
  enum Type {
    GET,
    PUT,
    REMOVE,
    SIZE,
    CONTAINS_KEY,
    CONTAINS_VALUE,
    CLEAR,
    PUT_ALL,
    KEY_SET,
    VALUES,
    ENTRY_SET,
    SYNC_OP,
    PUT_IF_ABSENT,
    PUT_IF_PRESENT,
    CONDITIONAL_REMOVE,
    CONDITIONAL_REPLACE;
  }

  Type operationType();

  void writeTo(DataOutput output) throws IOException;
}
