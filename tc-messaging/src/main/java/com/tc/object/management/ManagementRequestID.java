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
 *
 */
package com.tc.object.management;

import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public final class ManagementRequestID {

  private static final AtomicLong ID_GENERATOR = new AtomicLong();

  private final long id;

  public ManagementRequestID() {
    this.id = ID_GENERATOR.incrementAndGet();
  }

  public ManagementRequestID(long longValue) {
    this.id = longValue;
  }

  public long getId() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ManagementRequestID) {
      ManagementRequestID other = (ManagementRequestID)obj;
      return other.id == id;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return new Long(id).hashCode();
  }

  @Override
  public String toString() {
    return "ManagementRequestID[" + id + "]";
  }
}
