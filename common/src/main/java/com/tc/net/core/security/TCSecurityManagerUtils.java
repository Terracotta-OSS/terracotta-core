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
