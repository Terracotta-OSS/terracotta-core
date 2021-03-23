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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ClientSourceId;

public class PassthroughClientSourceId implements ClientSourceId {
  final long id;

  public PassthroughClientSourceId(long id) {
    this.id = id;
  }

  @Override
  public long toLong() {
    return id;
  }

  @Override
  public boolean isValidClient() {
    return id >= 0;
  }

  @Override
  public boolean matches(ClientDescriptor descriptor) {
    return descriptor.getSourceId().toLong() == id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o  instanceof ClientSourceId)) {
      return false;
    }

    return id == ((ClientSourceId) o).toLong();
  }

  @Override
  public int hashCode() {
    return (int) (id ^ (id >>> 32));
  }
}
