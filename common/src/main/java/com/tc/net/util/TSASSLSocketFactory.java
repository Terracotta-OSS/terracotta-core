/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
