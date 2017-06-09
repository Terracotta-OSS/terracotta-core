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

package com.tc.util;

/**
 * @author tim
 */
public enum ProductID {
  DIAGNOSTIC(true, false, false), STRIPE(false, true, true), SERVER(true, false, true);

  private final boolean internal;
  private final boolean reconnect;
  private final boolean redirect;

  ProductID(boolean internal, boolean reconnect, boolean redirect) {
    this.internal = internal;
    this.reconnect = reconnect;
    this.redirect = redirect;
  }

  public boolean isInternal() {
    return internal;
  }
  
  public boolean isReconnectEnabled() {
    return reconnect;
  }

  public boolean isRedirectEnabled() {
    return redirect;
  }
  
  public String toString() {
    return name().charAt(0) + name().substring(1).toLowerCase();
  }
}
