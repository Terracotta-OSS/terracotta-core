/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import com.meterware.httpunit.HeadMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.tc.util.runtime.Vm;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

/*
 * Test reflector links INT-404
 */
public class ReflectorTest extends TestCase {
  private static final String reflectorUrl  = "http://svn.terracotta.org/svn/tc/reflector/reflector.properties";
  private Properties          reflector     = new Properties();
  private Set                 brokenLinks   = new HashSet();
  private Set                 excludedLinks = new HashSet();

  protected void setUp() throws Exception {
    URL url = new URL(reflectorUrl);

    Date disabledUntilDate = new SimpleDateFormat("yyyy-MM-dd").parse("2008-02-05");

    if (disabledUntilDate.after(new Date())) {
      System.out.println("Test disabled till: " + disabledUntilDate);
    } else {
      reflector.load(url.openStream());
    }

    // excluded links - no need to check
    excludedLinks.add("iqcs.depguide");
    excludedLinks.add("default.update.properties");
  }

  public void testBrokenLinks() throws Exception {
    // only run this test with jdk1.4 to reduce the amount
    // of unnecessary runs
    if (!Vm.isJDK14()) return;

    for (Iterator it = reflector.entrySet().iterator(); it.hasNext();) {
      Map.Entry e = (Map.Entry) it.next();
      if (excludedLinks.contains(e.getKey())) continue;
      System.out.println("Checking link: " + e.getKey() + " = " + e.getValue());
      assertNotBroken((String) e.getKey(), (String) e.getValue());
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

  private void assertNotBroken(String desc, String link) throws Exception {
    WebRequest request = new HeadMethodWebRequest(link);
    WebConversation wc = new WebConversation();
    WebResponse response = wc.getResource(request);
    if (response.getResponseCode() != 200) {
      brokenLinks.add(desc + " = " + link);
    }
  }
}
