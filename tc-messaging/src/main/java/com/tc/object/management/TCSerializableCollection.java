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

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 */
public abstract class TCSerializableCollection<T extends TCSerializable<T>> extends AbstractCollection<T> implements TCSerializable<TCSerializableCollection<T>> {
  private final Set<T> authority = new HashSet<T>();

  public Set<T> getAuthority() {
    return Collections.unmodifiableSet(authority);
  }

  @Override
  public boolean add(T t) {
    return authority.add(t);
  }

  @Override
  public Iterator<T> iterator() {
    return authority.iterator();
  }

  @Override
  public int size() {
    return authority.size();
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(authority.size());
    for (T t : authority) {
      t.serializeTo(serialOutput);
    }
  }
  
  @Override
  public TCSerializableCollection<T> deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    clear();
    int count = serialInput.readInt();
    for (int i=0;i<count;i++) {
      add(newObject().deserializeFrom(serialInput));
    }
    
    return this;
  }

  protected abstract T newObject();

}