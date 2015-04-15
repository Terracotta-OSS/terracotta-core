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
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.tx.ServerTransactionID;

/**
 * @author tim
 */
public class ServerTransactionCompleteContext implements EventContext {
  private final ServerTransactionID stxID;

  public ServerTransactionCompleteContext(final ServerTransactionID stxID) {
    this.stxID = stxID;
  }

  public ServerTransactionID getServerTransactionID() {
    return stxID;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ServerTransactionCompleteContext that = (ServerTransactionCompleteContext) o;

    if (!stxID.equals(that.stxID)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return stxID.hashCode();
  }
}
