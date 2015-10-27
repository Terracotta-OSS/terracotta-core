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
package com.tc.net.core.security;

import java.security.Principal;

/**
 * @author Alex Snaps
 */
public interface Realm {

  /**
   * Initialize the realm. This method is called after the whole security
   * mechanism is initialized, so the realm has a chance to read its password(s)
   * from the key chain using {@link com.tc.security.PwProviderUtil} for instance.
   */
  void initialize();

  Principal authenticate(String username, char[] passwd);
}
