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
package com.tc.object.tx;

import com.tc.util.AbstractIdentifier;
import static com.tc.util.Assert.assertTrue;

/**
 * @author steve
 */
public class TransactionID extends AbstractIdentifier {
  public final static TransactionID NULL_ID = new TransactionID();

  public TransactionID(long id) {
    super(id);
    assertTrue(id != 0L);
  }

  private TransactionID() {
    super();
  }

  @Override
  public String getIdentifierType() {
    return "TransactionID";
  }
}
