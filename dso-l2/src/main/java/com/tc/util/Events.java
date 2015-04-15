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

    WriteOperationCountChangeEvent(final NodeID source, final int delta) {
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
