/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import com.meterware.httpunit.HeadMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/*
 * Test reflector links INT-404
 */
public class ReflectorTest extends TCTestCase {
  private static final String reflectorUrl  = "http://svn.terracotta.org/svn/tc/reflector/reflector.properties";
  private Properties          reflector     = new Properties();
  private Set                 brokenLinks   = new HashSet();
  private Set                 excludedLinks = new HashSet();

  protected void setUp() throws Exception {
    URL url = new URL(reflectorUrl);
    reflector.load(url.openStream());

    // excluded links - no need to check
    excludedLinks.add("iqcs.depguide");
    excludedLinks.add("default.update.properties");
  }

  public void testBrokenLinks() throws Exception {
    for (Iterator it = reflector.entrySet().iterator(); it.hasNext();) {
      Map.Entry e = (Map.Entry) it.next();
      String desc = (String) e.getKey();
      String link = ((String) e.getValue()).trim();
      if (excludedLinks.contains(desc) || !link.startsWith("http:")) continue;
      System.out.println("Checking link: " + desc + " = " + link);
      if (!isValid(link)) {
        brokenLinks.add(desc + " = " + link);
      }
    }

    // report broken links
    if (brokenLinks.size() > 0) {
      StringBuffer keys = new StringBuffer();
      for (Iterator it = brokenLinks.iterator(); it.hasNext();) {
        keys.append((String) it.next()).append("\n");
      }
      fail("Broken links found on reflector. Please let Orion or Fiona know.\n" + keys.toString());
    }
  }

  private static boolean isValid(String url) {
    try {
      url = url.trim();
      WebRequest request = new HeadMethodWebRequest(url);
      WebConversation wc = new WebConversation();
      WebResponse response = wc.getResource(request);
      String redirectLink = response.getHeaderField("Location");
      if (redirectLink != null) {
        return isValid(redirectLink);
      } else {
        return response.getResponseCode() == 200;
      }
    } catch (Exception e) {
      return false;
    }
  }
}
