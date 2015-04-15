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
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author mscott
 */
public class DeleteReference implements ManagedObjectReference {
  
  private final ObjectID id;
  private final AtomicBoolean marked = new AtomicBoolean(true);

  public DeleteReference(ObjectID id) {
    this.id = id;
  }
  
  @Override
  public ObjectID getObjectID() {
    return id;
  }

  @Override
  public void setRemoveOnRelease(boolean removeOnRelease) {

  }

  @Override
  public boolean isRemoveOnRelease() {
    return true;
  }

  @Override
  public boolean markReference() {
    return false;
  }

  @Override
  public boolean unmarkReference() {
    return marked.compareAndSet(true, false);
  }

  @Override
  public boolean isReferenced() {
    return true;
  }

  @Override
  public boolean isNew() {
    return false;
  }

  @Override
  public ManagedObject getObject() {
    return null;
  }
  
}
