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
package com.tc.object;

import com.google.common.base.Objects;

public class VersionedObject {

  private final Object object;
  private final long   version;

  public VersionedObject(final Object object, final long version) {
    this.object = object;
    this.version = version;
  }

  public Object getObject() {
    return this.object;
  }

  public long getVersion() {
    return this.version;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("object", object)
        .add("version", version)
        .toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final VersionedObject that = (VersionedObject)o;

    return version == that.version && object.equals(that.object);

  }

  @Override
  public int hashCode() {
    int result = object.hashCode();
    result = 31 * result + (int)(version ^ (version >>> 32));
    return result;
  }
}
