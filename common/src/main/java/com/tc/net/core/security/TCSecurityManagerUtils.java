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

import com.tc.exception.TCRuntimeException;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

/**
 * TCSecurityManagerUtils
 */
public final class TCSecurityManagerUtils {

  private TCSecurityManagerUtils() {
    // Private constructor on static utility class
  }

  public static URI createTcURI(String user, String host, int port) {
    try {
      String userInfo = user != null ? URLEncoder.encode(user, "UTF-8").replace("+", "%20") : null;
      return new URI("tc", userInfo, host, port, null, null, null);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Can't create an URI from the provided arguments!", e);
    } catch (UnsupportedEncodingException uee) {
      throw new IllegalArgumentException("Can't create an URI from the provided arguments!", uee);
    }
  }

  @SuppressWarnings("unchecked")
  public static Realm createRealm(String realmImplClass, String realmUrl) {
    try {
      final Class<? extends Realm> aClass = (Class<? extends Realm>) Class.forName(realmImplClass);
      return aClass.getConstructor(String.class).newInstance(realmUrl);
    } catch (Exception e) {
      throw new TCRuntimeException("Couldn't create Realm instance of type " + realmImplClass + " with " + realmUrl, e);
    }
  }
}
