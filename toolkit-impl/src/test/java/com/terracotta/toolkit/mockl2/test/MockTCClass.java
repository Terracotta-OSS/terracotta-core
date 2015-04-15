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
package com.terracotta.toolkit.mockl2.test;
/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */


import com.tc.exception.ImplementMe;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.platform.PlatformService;

import java.lang.reflect.Constructor;

public class MockTCClass implements TCClass {
  private final String  name = MockTCClass.class.getName();

  public MockTCClass() {
    //
  }

  @Override
  public TraversedReferences getPortableObjects(final Object pojo, final TraversedReferences addTo) {
    throw new ImplementMe();
  }

  @Override
  public Constructor getConstructor() {
    throw new ImplementMe();
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public void hydrate(final TCObject tcObject, final DNA dna, final Object pojo, final boolean force) {
    throw new ImplementMe();
  }

  @Override
  public void dehydrate(final TCObject tcObject, final DNAWriter writer, final Object pojo) {
    throw new ImplementMe();
  }

  @Override
  public TCObject createTCObject(final ObjectID id, final Object peer, final boolean isNew) {
    throw new ImplementMe();
  }

  @Override
  public boolean isUseNonDefaultConstructor() {
    return false;
  }

  @Override
  public Object getNewInstanceFromNonDefaultConstructor(final DNA dna, PlatformService platformService) {
    throw new ImplementMe();
  }

  @Override
  public Class getPeerClass() {
    return getClass();
  }

  @Override
  public ClientObjectManager getObjectManager() {
    throw new ImplementMe();
  }

}
