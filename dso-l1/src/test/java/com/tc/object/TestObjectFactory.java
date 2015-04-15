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

import com.tc.exception.ImplementMe;
import com.tc.object.dna.api.DNA;
import com.tc.platform.PlatformService;

public class TestObjectFactory implements TCObjectFactory {

  public TCObject tcObject;
  public Object   peerObject;

  @Override
  public void setObjectManager(ClientObjectManager objectManager) {
    return;
  }

  @Override
  public TCObject getNewInstance(ObjectID id, Object peer, Class clazz, boolean isNew) {
    return tcObject;
  }

  public TCObject getNewInstance(ObjectID id, Class clazz, boolean isNew) {
    return tcObject;
  }

  @Override
  public Object getNewPeerObject(TCClass type) throws IllegalArgumentException, SecurityException {
    return peerObject;
  }

  @Override
  public Object getNewPeerObject(TCClass type, DNA dna, PlatformService platformService) {
    return peerObject;
  }

  @Override
  public void initClazzIfRequired(Class clazz, TCObjectSelf tcObjectSelf) {
    throw new ImplementMe();

  }

}
