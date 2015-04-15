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

import com.tc.abortable.AbortedOperationException;

public class TCObjectLogical extends TCObjectImpl {

  public TCObjectLogical(final ObjectID id, final Object peer, final TCClass tcc, final boolean isNew) {
    super(id, peer, tcc, isNew);
  }

  @Override
  public void logicalInvoke(final LogicalOperation method, final Object[] parameters) {
    getObjectManager().getTransactionManager().logicalInvoke(this, method, parameters);
  }

  public boolean logicalInvokeWithResult(final LogicalOperation method, final Object[] parameters) throws AbortedOperationException {
    return getObjectManager().getTransactionManager().logicalInvokeWithResult(this, method, parameters);
  }

  @Override
  public void unresolveReference(final String fieldName) {
    throw new AssertionError();
  }
}
