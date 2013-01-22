package com.terracotta.management.web.utils;

import com.tc.security.PwProviderUtil;
import com.terracotta.management.keychain.KeyName;
import com.terracotta.management.keychain.URIKeyName;
import com.terracotta.management.security.KeyChainAccessor;
import com.terracotta.management.security.SecretUtils;

import java.net.URI;

/**
 * @author Ludovic Orban
 */
public class L2KeyChainAccessor implements KeyChainAccessor {

  @Override
  public byte[] retrieveSecret(KeyName alias) {
    URIKeyName uriKeyName = (URIKeyName)alias;
    URI uri = uriKeyName.getURI();
    try {
      char[] passwordTo = PwProviderUtil.getPasswordTo(uri);
      return SecretUtils.toBytesAndWipe(passwordTo);
    } catch (NullPointerException npe) {
      // PwProviderUtil.getPasswordTo throws NPE when there is no such secret
      return null;
    }
  }

}
