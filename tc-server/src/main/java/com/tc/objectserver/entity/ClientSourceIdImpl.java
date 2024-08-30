/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
