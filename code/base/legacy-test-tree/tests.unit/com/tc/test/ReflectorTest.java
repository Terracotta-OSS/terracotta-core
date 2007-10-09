/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import com.meterware.httpunit.HeadMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

/*
 * Test reflector links INT-404
 */
public class ReflectorTest extends TestCase {
  private static final String reflectorUrl = "http://svn.terracotta.org/svn/tc/reflector/reflector.properties";
  private Properties          reflector    = new Properties();
  private Map                 brokenLinks  = new HashMap();

  protected void setUp() throws Exception {
    URL url = new URL(reflectorUrl);
    reflector.load(url.openStream());
  }

  public void testBrokenLinks() throws Exception {
    for (Iterator it = reflector.entrySet().iterator(); it.hasNext();) {
      Map.Entry e = (Map.Entry) it.next();
      if (e.getValue().toString().indexOf("svn.terracotta.org") > 0) continue;
      assertNotBroken((String) e.getKey(), (String) e.getValue());
    }

    if (brokenLinks.size() > 0) {
      StringBuffer keys = new StringBuffer();
      System.err.println("Broken links:");
      for (Iterator it = brokenLinks.entrySet().iterator(); it.hasNext();) {
        Map.Entry e = (Map.Entry) it.next();
        System.err.println(e.getKey() + " = " + e.getValue());
        keys.append((String)e.getKey()).append("\n");
      }
      fail("Broken links found on reflector. Please let Orion or Fiona know.\n" + keys.toString());
    }
  }

  private void assertNotBroken(String desc, String link) throws Exception {
    WebRequest request = new HeadMethodWebRequest(link);
    WebConversation wc = new WebConversation();
    WebResponse response = wc.getResource(request);
    if (response.getResponseCode() != 200) {
      brokenLinks.put(desc, link);
    }
  }
}
