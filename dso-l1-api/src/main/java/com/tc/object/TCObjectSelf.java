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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public interface TCObjectSelf extends TCObject {

  public void initializeTCObject(final ObjectID id, final TCClass clazz, final boolean isNew);

  public void serialize(ObjectOutput out) throws IOException;

  public void deserialize(ObjectInput in) throws IOException;

  public void initClazzIfRequired(TCClass tcc);

  public boolean isInitialized();

}
