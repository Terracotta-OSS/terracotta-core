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
package com.tc.object.locks;

import com.tc.object.ClientObjectManager;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.bytecode.Manageable;

public class LockIdFactory {

  private final ClientObjectManager objectManager;

  public LockIdFactory(ClientObjectManager objectManager) {
    this.objectManager = objectManager;
  }

  public LockID generateLockIdentifier(final Object obj) {
    if (obj instanceof Long) {
      return generateLockIdentifier(((Long) obj).longValue());
    } else if (obj instanceof String) {
      return generateLockIdentifier((String) obj);
    } else {
      final TCObject tco = lookupExistingOrNull(obj);
      if (tco != null) {
        if (tco.autoLockingDisabled()) {
          return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
        } else {
          return new DsoLockID(tco.getObjectID());
        }
      } else if (isLiteralAutolock(obj)) {
        try {
          return new DsoLiteralLockID(obj);
        } catch (final IllegalArgumentException e) {
          return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
        }
      } else {
        return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
      }
    }
  }

  private TCObject lookupExistingOrNull(Object obj) {
    return objectManager.lookupExistingOrNull(obj);
  }

  public LockID generateLockIdentifier(final long l) {
    return new LongLockID(l);
  }

  public LockID generateLockIdentifier(final String str) {
    return new StringLockID(str);
  }

  public boolean isLiteralAutolock(final Object o) {
    if (o instanceof Manageable) { return false; }
    return (!(o instanceof Class)) && (!(o instanceof ObjectID)) && LiteralValues.isLiteralInstance(o);
  }
}
