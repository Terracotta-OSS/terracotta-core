/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import com.tc.util.Assert;
import com.terracottatech.config.Application;
import com.terracottatech.config.Client;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.System;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Holds on to a set of beans that can be modified; this is used by the
 * {@link com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory}.
 */
public class TestConfigBeanSet {

  private final Client  rootClientBean;
  private final Servers rootServersBean;
  private final System  rootSystemBean;
  private final Map     rootApplicationBeans;

  public TestConfigBeanSet() {
    this.rootClientBean = Client.Factory.newInstance();

    this.rootServersBean = Servers.Factory.newInstance();
    Server initialServer = this.rootServersBean.addNewServer();
    initialServer.setName("localhost");

    this.rootSystemBean = System.Factory.newInstance();

    this.rootApplicationBeans = new HashMap();
    this.rootApplicationBeans.put(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME, createNewApplication());

    checkValidates(this.rootClientBean);
    checkValidates(this.rootServersBean);
    checkValidates(this.rootSystemBean);

    Iterator iter = this.rootApplicationBeans.values().iterator();
    while (iter.hasNext()) {
      checkValidates((XmlObject) iter.next());
    }
  }

  private void checkValidates(XmlObject object) {
    List errors = new ArrayList();
    XmlOptions options = new XmlOptions().setErrorListener(errors);

    boolean result = object.validate(options);
    if ((!result) || (errors.size() > 0)) {
      // formatting
      throw Assert
          .failure("Object " + object + " of " + object.getClass() + " didn't validate; errors were: " + errors);
    }
  }

  public Client clientBean() {
    return this.rootClientBean;
  }

  public Servers serversBean() {
    return this.rootServersBean;
  }

  public System systemBean() {
    return this.rootSystemBean;
  }

  public String[] applicationNames() {
    return (String[]) this.rootApplicationBeans.keySet().toArray(new String[this.rootApplicationBeans.size()]);
  }

  public Application applicationBeanFor(String applicationName) {
    Application out = (Application) this.rootApplicationBeans.get(applicationName);
    if (out == null) {
      out = createNewApplication();
      this.rootApplicationBeans.put(applicationName, out);
    }
    return out;
  }

  private Application createNewApplication() {
    Application out = Application.Factory.newInstance();
    out.addNewDso().addNewInstrumentedClasses();
    return out;
  }

}
