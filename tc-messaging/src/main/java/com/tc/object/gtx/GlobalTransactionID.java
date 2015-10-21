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
package com.tc.object.gtx;

import com.tc.util.AbstractIdentifier;

public class GlobalTransactionID extends AbstractIdentifier {

  public static final GlobalTransactionID NULL_ID = new GlobalTransactionID();

  public GlobalTransactionID(long id) {
    super(id);
  }

  private GlobalTransactionID() {
    super();
  }

  @Override
  public String getIdentifierType() {
    return "GlobalTransactionID";
  }

  public boolean lessThan(GlobalTransactionID compare) {
    return isNull() ? true : toLong() < compare.toLong();
  }

  public GlobalTransactionID next() {
    return new GlobalTransactionID(toLong() + 1);
  }

}
