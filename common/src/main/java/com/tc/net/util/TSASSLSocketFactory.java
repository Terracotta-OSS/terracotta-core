/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author Ludovic Orban
 */
public class TSASSLSocketFactory extends SSLSocketFactory implements RMIClientSocketFactory {
  private final SSLSocketFactory socketFactory;

  public static SocketFactory getDefault() {
    try {
      return new TSASSLSocketFactory();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public TSASSLSocketFactory() throws Exception {
    SSLContext ctx = SSLContext.getInstance("TLS");

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

    ctx.init(null, trustManagers, null);
    socketFactory = ctx.getSocketFactory();
  }


  @Override
  public String[] getDefaultCipherSuites() {
    return socketFactory.getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return socketFactory.getSupportedCipherSuites();
  }

  @Override
  public Socket createSocket(Socket socket, String string, int i, boolean bln) throws IOException {
    return socketFactory.createSocket(socket, string, i, bln);
  }

  @Override
  public Socket createSocket(String string, int i) throws IOException {
    return socketFactory.createSocket(string, i);
  }

  @Override
  public Socket createSocket(String string, int i, InetAddress ia, int i1) throws IOException {
    return socketFactory.createSocket(string, i, ia, i1);
  }

  @Override
  public Socket createSocket(InetAddress ia, int i) throws IOException {
    return socketFactory.createSocket(ia, i);
  }

  @Override
  public Socket createSocket(InetAddress ia, int i, InetAddress ia1, int i1) throws IOException {
    return socketFactory.createSocket(ia, i, ia1, i1);
  }

}
