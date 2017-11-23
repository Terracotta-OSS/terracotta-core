/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.util.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.net.core.SecurityInfo;
import com.tc.security.TCAuthenticationException;
import com.tc.security.TCAuthorizationException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

@SuppressWarnings("restriction")
public class ServerURL {

  private static final Logger logger = LoggerFactory.getLogger(ServerURL.class);

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
    URLConnection urlConnection = createConnection();

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

  private URLConnection createConnection() {
    URLConnection urlConnection;
    try {
      urlConnection = theURL.openConnection();
    } catch (IOException e1) {
      throw new IllegalStateException(e1);
    }

    if (timeout > -1) {
      urlConnection.setConnectTimeout(timeout);
      urlConnection.setReadTimeout(timeout);
    }
    return urlConnection;
  }

  @Override
  public String toString() {
    return theURL.toString();
  }

  public String getUsername() {
    return securityInfo.isSecure() ? securityInfo.getUsername() : null;
  }

}