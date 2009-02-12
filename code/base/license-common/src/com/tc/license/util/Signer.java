/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.license.util;

import java.io.File;

public interface Signer {
  public String sign(byte[] content, File privateKeyFile);

  public boolean verify(byte[] content, String signatureString);
}