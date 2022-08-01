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
    GET {
      @Override
      public boolean replicate() { return false; }
    },
    PUT,
    REMOVE,
    SIZE {
      @Override
      public boolean replicate() { return false; }
    },
    CONTAINS_KEY {
      @Override
      public boolean replicate() { return false; }
    },
    CONTAINS_VALUE {
      @Override
      public boolean replicate() { return false; }
    },
    CLEAR,
    PUT_ALL,
    KEY_SET {
      @Override
      public boolean replicate() { return false; }
    },
    VALUES {
      @Override
      public boolean replicate() { return false; }
    },
    ENTRY_SET {
      @Override
      public boolean replicate() { return false; }
    },
    SYNC_OP {
      @Override
      public boolean replicate() { return false; }
    },
    PUT_IF_ABSENT,
    PUT_IF_PRESENT,
    CONDITIONAL_REMOVE,
    CONDITIONAL_REPLACE;

    public boolean replicate() {
      return true;
    }
  }

  Type operationType();

  void writeTo(DataOutput output) throws IOException;
}
