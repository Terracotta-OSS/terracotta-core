/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.management.web.utils;

import com.terracotta.management.security.SSLContextFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author Ludovic Orban
 */
public class TSASslContextFactory implements SSLContextFactory {

  @Override
  public SSLContext create() throws NoSuchAlgorithmException, IOException, KeyStoreException, CertificateException, UnrecoverableKeyException, KeyManagementException, URISyntaxException {
    SSLContext sslCtxt = SSLContext.getInstance("TLS");

    TrustManager[] trustManagers = null;
    if (Boolean.getBoolean("tc.ssl.trustAllCerts")) {
      // trust all certs
      trustManagers = new TrustManager[] {
          new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
              //
            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
              //
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return null;
            }
          }
      };
    }

    sslCtxt.init(null, trustManagers, null);
    return sslCtxt;

  }

  @Override
  public boolean isUsingClientAuth() {
    return false;
  }
}
