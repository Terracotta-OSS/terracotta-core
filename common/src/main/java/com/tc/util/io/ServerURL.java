/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.io;

import sun.misc.BASE64Encoder;

import com.tc.exception.TCRuntimeException;
import com.tc.net.core.SecurityInfo;
import com.tc.security.PwProvider;
import com.tc.security.TCAuthenticationException;
import com.tc.security.TCAuthorizationException;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@SuppressWarnings("restriction")
public class ServerURL {

  private final URL          theURL;
  private final int          timeout;
  private final SecurityInfo securityInfo;

  public ServerURL(String host, int port, String file, SecurityInfo securityInfo) throws MalformedURLException {
    this(host, port, file, -1, securityInfo);
  }

  public ServerURL(String host, int port, String file, int timeout, SecurityInfo securityInfo) throws MalformedURLException {
    this.timeout = timeout;
    this.securityInfo = securityInfo;
    this.theURL = new URL(securityInfo.isSecure() ? "https" : "http", host, port, file);
  }

  public InputStream openStream() throws IOException {
    return this.openStream(null);
  }

  public InputStream openStream(PwProvider pwProvider) throws IOException {
    URLConnection urlConnection;
    if(securityInfo.isSecure()) {
      Assert.assertNotNull("Secured URL '" + theURL + "', yet PwProvider instance", pwProvider);
    }
    String uri = null;

    if (securityInfo.isSecure()) {
      HttpsURLConnection sslUrlConnection = (HttpsURLConnection) theURL.openConnection();
      if (securityInfo.getUsername() != null) {
        String encodedUsername = URLEncoder.encode(securityInfo.getUsername(), "UTF-8").replace("+", "%20");
        uri = "tc://" + encodedUsername + "@" + theURL.getHost() + ":" + theURL.getPort();
        final char[] passwordTo;
        try {
          final URI theURI = new URI(uri);
          passwordTo = pwProvider.getPasswordFor(theURI);
        } catch (URISyntaxException e) {
          throw new TCRuntimeException("Couldn't create URI to connect to " + uri, e);
        }
        Assert.assertNotNull("No password for " + theURL + " found!", passwordTo);
        sslUrlConnection.addRequestProperty("Authorization", "Basic " + new BASE64Encoder().encode((securityInfo.getUsername() + ":" + new String(passwordTo))
            .getBytes()));
      }

      if (Boolean.getBoolean("tc.ssl.disableHostnameVerifier")) {
        // don't verify hostname
        sslUrlConnection.setHostnameVerifier(new HostnameVerifier() {
          @Override
          public boolean verify(String hostname, SSLSession session) {
            return true;
          }
        });
      }

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

      try {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, null);
        sslUrlConnection.setSSLSocketFactory(sslContext.getSocketFactory());
      } catch (Exception e) {
        throw new RuntimeException("unable to create SSL connection from " + theURL, e);
      }

      urlConnection = sslUrlConnection;
    } else {
      urlConnection = theURL.openConnection();
    }

    if (timeout > -1) {
      urlConnection.setConnectTimeout(timeout);
      urlConnection.setReadTimeout(timeout);
    }

    try {
      return urlConnection.getInputStream();
    } catch (IOException e) {
      if (urlConnection instanceof HttpURLConnection) {
        int responseCode = ((HttpURLConnection)urlConnection).getResponseCode();
        switch (responseCode) {
          case 401:
            throw new TCAuthenticationException("Authentication error connecting to " +
                                                (uri != null ? uri : urlConnection.getURL()) +
                                                " - invalid credentials (tried user " +
                                                securityInfo.getUsername() + ")", e);
          case 403:
            throw new TCAuthorizationException("Authorization error connecting to " +
                                                (uri != null ? uri : urlConnection.getURL()) +
                                                " - does the user '" + securityInfo.getUsername() +
                                                "' have the required roles?", e);
          default:
        }
      }
      throw e;
    }
  }

  @Override
  public String toString() {
    return theURL.toString();
  }
}