/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.util;

import com.tc.net.NodeID;

/**
 * Static utility methods to access event instances.
 *
 * @author Eugene Shelestovich
 */
public final class Events {

  private Events() {}

  /**
   * Constructs an event what notifies subscribers about new map mutation operation.
   */
  public static WriteOperationCountChangeEvent writeOperationCountIncrementEvent(NodeID source) {
    return writeOperationCountChangeEvent(source, 1);
  }

  /**
   * Constructs an event what notifies subscribers about new map mutation operations.
   */
  public static WriteOperationCountChangeEvent writeOperationCountChangeEvent(NodeID source, int delta) {
    return new WriteOperationCountChangeEvent(source, delta);
  }

  public static final class WriteOperationCountChangeEvent {
    private final NodeID source;
    private final int delta;

    WriteOperationCountChangeEvent(NodeID source, int delta) {
      this.source = source;
      this.delta = delta;
    }

    public int getDelta() {
      return delta;
    }

    public NodeID getSource() {
      return source;
    }
  }
}
