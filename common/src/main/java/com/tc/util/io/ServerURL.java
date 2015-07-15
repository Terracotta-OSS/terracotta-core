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
package com.tc.util.io;

import org.apache.commons.io.IOUtils;
import sun.misc.BASE64Encoder;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.SecurityInfo;
import com.tc.security.PwProvider;
import com.tc.security.TCAuthenticationException;
import com.tc.security.TCAuthorizationException;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

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
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@SuppressWarnings("restriction")
public class ServerURL {

  private static final TCLogger logger                    = TCLogging.getLogger(ServerURL.class);

  private static final String   VERSION_HEADER            = "Version";

  private static final boolean  DISABLE_HOSTNAME_VERIFIER = Boolean.getBoolean("tc.ssl.disableHostnameVerifier");
  private static final boolean  TRUST_ALL_CERTS           = Boolean.getBoolean("tc.ssl.trustAllCerts");

  private final URL             theURL;
  private final int             timeout;
  private final SecurityInfo    securityInfo;

  public ServerURL(String host, int port, String file, SecurityInfo securityInfo) throws MalformedURLException {
    this(host, port, file, -1, securityInfo);
  }

  public ServerURL(String host, int port, String file, int timeout, SecurityInfo securityInfo)
      throws MalformedURLException {
    this.timeout = timeout;
    this.securityInfo = securityInfo;
    this.theURL = new URL(securityInfo.isSecure() ? "https" : "http", host, port, file);
  }

  public InputStream openStream() throws IOException {
    return this.openStream(null);
  }

  public String getServerVersion(PwProvider pwProvider) throws IOException {
    for (int i = 0; i < 3; i++) {
      HttpURLConnection urlConnection = createSecureConnection(pwProvider);
      try {
        urlConnection.connect();
        int responseCode = urlConnection.getResponseCode();
        if (responseCode == 200) {
          return urlConnection.getHeaderField(VERSION_HEADER);
        } else {
          logger.info("Failed to retrieve header field, response code : " + responseCode + ", headers :\n" + readHeaderFields(urlConnection) + " body : \n[" + readLines(urlConnection) + "]");
        }
      } finally {
        urlConnection.disconnect();
      }

      logger.info("Retrying connection since response code != 200");
      ThreadUtil.reallySleep(50);
    }

    throw new IOException("Cannot retrieve " + VERSION_HEADER + " header from server url after 3 tries : " + theURL);
  }

  private static String readHeaderFields(HttpURLConnection urlConnection) {
    StringBuilder sb = new StringBuilder();
    Map<String, List<String>> headerFields = urlConnection.getHeaderFields();
    for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
      sb.append(entry.getKey()).append("=").append(entry.getValue()).append(System.getProperty("line.separator"));
    }
    return sb.toString();
  }

  private static String readLines(HttpURLConnection urlConnection) throws IOException {
    try {
      List<String> lines = IOUtils.readLines(urlConnection.getInputStream());
      StringBuilder sb = new StringBuilder();
      for (String line : lines) {
        sb.append(line).append(System.getProperty("line.separator"));
      }
      return sb.toString();
    } catch (IOException e) {
      return "";
    }
  }

  public InputStream openStream(PwProvider pwProvider) throws IOException {
    URLConnection urlConnection = createSecureConnection(pwProvider);

    try {
      return urlConnection.getInputStream();
    } catch (IOException e) {
      if (urlConnection instanceof HttpURLConnection) {
        int responseCode = ((HttpURLConnection) urlConnection).getResponseCode();
        switch (responseCode) {
          case 401:
            throw new TCAuthenticationException("Authentication error connecting to " + urlConnection.getURL()
                                                + " - invalid credentials (tried user " + securityInfo.getUsername()
                                                + ")", e);
          case 403:
            throw new TCAuthorizationException("Authorization error connecting to " + urlConnection.getURL()
                                               + " - does the user '" + securityInfo.getUsername()
                                               + "' have the required roles?", e);
          default:
        }
      }
      throw e;
    }
  }

  private HttpURLConnection createSecureConnection(PwProvider pwProvider) {
    if (securityInfo.isSecure()) {
      Assert.assertNotNull("Secured URL '" + theURL + "', yet PwProvider instance", pwProvider);
    }

    URLConnection urlConnection;
    try {
      urlConnection = theURL.openConnection();
      String uri = null;

      if (securityInfo.isSecure()) {
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
          urlConnection
              .addRequestProperty("Authorization",
                                  "Basic "
                                      + new BASE64Encoder().encode((securityInfo.getUsername() + ":" + new String(
                                                                                                                  passwordTo))
                                          .getBytes()));
        }

        if (DISABLE_HOSTNAME_VERIFIER || TRUST_ALL_CERTS) {
          tweakSecureConnectionSettings(urlConnection);
        }
      }
    } catch (IOException e1) {
      throw new IllegalStateException(e1);
    }

    if (timeout > -1) {
      urlConnection.setConnectTimeout(timeout);
      urlConnection.setReadTimeout(timeout);
    }
    return (HttpURLConnection) urlConnection;
  }

  @Override
  public String toString() {
    return theURL.toString();
  }

  public String getUsername() {
    return securityInfo.isSecure() ? securityInfo.getUsername() : null;
  }

  private static void tweakSecureConnectionSettings(URLConnection urlConnection) {
    HttpsURLConnection sslUrlConnection;

    try {
      sslUrlConnection = (HttpsURLConnection) urlConnection;
    } catch (ClassCastException e) {
      throw new IllegalStateException(
                                      "Unable to cast "
                                          + urlConnection
                                          + " to javax.net.ssl.HttpsURLConnection. "
                                          + "Options tc.ssl.trustAllCerts and tc.ssl.disableHostnameVerifier are causing this issue.",
                                      e);
    }

    if (DISABLE_HOSTNAME_VERIFIER) {
      // don't verify hostname
      sslUrlConnection.setHostnameVerifier(new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      });
    }

    TrustManager[] trustManagers = null;
    if (TRUST_ALL_CERTS) {
      // trust all certs
      trustManagers = new TrustManager[] { new X509TrustManager() {
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
      } };
    }

    try {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, trustManagers, null);
      sslUrlConnection.setSSLSocketFactory(sslContext.getSocketFactory());
    } catch (Exception e) {
      throw new RuntimeException("unable to create SSL connection from " + urlConnection.getURL(), e);
    }
  }
}