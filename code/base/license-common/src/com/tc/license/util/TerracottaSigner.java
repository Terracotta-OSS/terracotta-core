/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.license.util;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class TerracottaSigner implements Signer {
  private static final String LICENSE_PUBLIC_KEY_RESOURCE_NAME = "/com/tc/license/license-public-key.x509";
  private File                privateKeyFile;

  public TerracottaSigner() {
    //
  }

  public TerracottaSigner(File privateKeyFile) {
    this.privateKeyFile = privateKeyFile;
  }

  public void setPrivateKeyFile(File privateKeyFile) {
    this.privateKeyFile = privateKeyFile;
  }

  public String sign(byte[] content) {
    if (privateKeyFile == null) { throw new IllegalStateException("Private key file is needed to sign"); }
    Signature signature = prepareSignSignature(privateKeyFile);
    try {
      signature.update(content);
      byte[] signatureData = signature.sign();
      return Base64.encodeBytes(signatureData);
    } catch (SignatureException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean verify(byte[] content, String signatureString) {
    Signature signature = prepareVerifySignature();
    try {
      signature.update(content);
      return signature.verify(Base64.decode(signatureString));
    } catch (SignatureException e) {
      throw new RuntimeException(e);
    }
  }

  private static Signature prepareVerifySignature() {
    InputStream in = TerracottaSigner.class.getResourceAsStream(LICENSE_PUBLIC_KEY_RESOURCE_NAME);
    if (in == null) { throw new RuntimeException("Can't find public key: " + LICENSE_PUBLIC_KEY_RESOURCE_NAME); }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Signature signature;
    try {
      IOUtils.copy(in, baos);
      X509EncodedKeySpec keySpec = new X509EncodedKeySpec(baos.toByteArray());
      KeyFactory keyFactory = KeyFactory.getInstance("DSA");
      PublicKey publicKey = keyFactory.generatePublic(keySpec);

      signature = Signature.getInstance("SHA1withDSA");
      signature.initVerify(publicKey);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
    return signature;
  }

  private static Signature prepareSignSignature(File keyFile) {
    if (keyFile == null || !keyFile.exists()) {
      //
      throw new IllegalArgumentException("Private key file doesn't exist or unknown");
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Signature signature;
    try {
      IOUtils.copy(new FileInputStream(keyFile), baos);
      KeySpec privateKeySpec = new PKCS8EncodedKeySpec(baos.toByteArray());
      KeyFactory factory = KeyFactory.getInstance("DSA");
      PrivateKey privateKey = factory.generatePrivate(privateKeySpec);

      signature = Signature.getInstance("SHA1withDSA");
      signature.initSign(privateKey);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
    return signature;
  }
}
