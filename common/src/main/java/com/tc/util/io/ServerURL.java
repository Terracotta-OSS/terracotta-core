/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.util.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  public ServerURL(String host, int port, String file) throws MalformedURLException {
    this(host, port, file, -1);
  }

  public ServerURL(String host, int port, String file, int timeout)
      throws MalformedURLException {
    this.timeout = timeout;
    this.theURL = new URL("http", host, port, file);
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
            throw new TCAuthenticationException("Authentication error connecting to " + urlConnection.getURL(), e);
          case 403:
            throw new TCAuthorizationException("Authorization error connecting to " + urlConnection.getURL(), e);
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

}