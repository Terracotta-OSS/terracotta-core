/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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