package com.terracotta.management.web.utils;

import com.tc.security.PwProviderUtil;
import com.terracotta.management.keychain.KeyName;
import com.terracotta.management.keychain.URIKeyName;
import com.terracotta.management.security.KeyChainAccessor;
import com.terracotta.management.security.SecretUtils;

/**
 * @author Ludovic Orban
 */
public class L2KeyChainAccessor implements KeyChainAccessor {

  @Override
  public byte[] retrieveSecret(KeyName alias) {
    URIKeyName uriKeyName = (URIKeyName)alias;
    return SecretUtils.toBytesAndWipe(PwProviderUtil.getPasswordTo(uriKeyName.getURI()));
  }

}
