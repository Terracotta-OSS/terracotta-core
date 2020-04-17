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
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.entity;

import com.tc.net.ClientID;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ClientSourceId;

public class ClientSourceIdImpl implements ClientSourceId {
  public static final ClientSourceId NULL_ID = new ClientSourceIdImpl(ClientID.NULL_ID.toLong());

  final long id;

  public ClientSourceIdImpl(long id) {
    this.id = id;
  }

  public ClientSourceIdImpl() {
    this(NULL_ID.toLong());
  }

  @Override
  public long toLong() {
    return id;
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
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ClientSourceIdImpl id1 = (ClientSourceIdImpl) o;

    return id == id1.id;
  }

  @Override
  public int hashCode() {
    return (int) (id ^ (id >>> 32));
  }
}
