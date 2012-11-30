package com.terracotta.management.web.utils;

import com.tc.security.PwProviderUtil;
import com.terracotta.management.keychain.KeyName;
import com.terracotta.management.keychain.URIKeyName;
import com.terracotta.management.security.KeyChainAccessor;

/**
 * @author Ludovic Orban
 */
public class L2KeyChainAccessor implements KeyChainAccessor {

  @Override
  public byte[] retrieveSecret(KeyName alias) {
    URIKeyName uriKeyName = (URIKeyName)alias;
    return toBytes(PwProviderUtil.getPasswordTo(uriKeyName.getURI()));
  }

  private byte[] toBytes(char[] chars) {
    byte[] result = new byte[chars.length];
    for (int i = 0; i < chars.length; i++) {
      result[i] = (byte)chars[i];
    }
    return result;
  }

}
