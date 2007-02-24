/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public final class URLUtil {

  private URLUtil() {
    throw new RuntimeException("Don't try to instantiate this [utility] class");
  }

  /**
   * @return the full URL to the given [relative] path, which could be located at any of the URLs in
   *         <code>baseURLs</code>, or <code>null</code> if the path does not exist at any of them.
   * @throws MalformedURLException
   */
  public static URL resolve(final URL[] baseURLs, final String path) throws MalformedURLException {
    if (baseURLs != null && path != null) {
      for (int pos = 0; pos < baseURLs.length; pos++) {
        final URL testURL = new URL(baseURLs[pos].toString() + (baseURLs[pos].toString().endsWith("/") ? "" : "/")
            + path);
        try {
          final InputStream is = testURL.openStream();
          is.read();
          is.close();
          return testURL;
        } catch (IOException ioe) {
          // Ignore this, the URL is bad
        }
      }
    }
    return null;
  }

}
