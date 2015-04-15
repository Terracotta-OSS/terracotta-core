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
package com.tc.test.setup;

import com.google.common.collect.Sets;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public class TestJMXServerManager {
  private Set<ObjectName> registeredMBeans = Sets.newHashSet();

  private final MBeanServer mBeanServer;
  private final int jmxServerPort;

  private boolean started = false;
  private JMXConnectorServer connectorServer;

  public TestJMXServerManager(final MBeanServer mBeanServer, final int jmxServerPort) {
    this.mBeanServer = mBeanServer;
    this.jmxServerPort = jmxServerPort;
  }

  public TestJMXServerManager(int jmxServerPort) {
    this(ManagementFactory.getPlatformMBeanServer(), jmxServerPort);
  }

  public synchronized void startJMXServer() throws Exception {
    if (started) { return; }
    // Get the platform MBeanServer
    System.out.println("********** Starting test JMX server at port[" + jmxServerPort + "]");
    try {
      // Uniquely identify the MBeans and register them with the platform MBeanServer
      JMXServiceURL url = new JMXServiceURL("service:jmx:jmxmp://" + "localhost" + ":" + this.jmxServerPort);
      connectorServer =  JMXConnectorServerFactory.newJMXConnectorServer(url, null, mBeanServer);
      connectorServer.start();
      started = true;
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public synchronized void registerMBean(Object mBean, ObjectName objectName) throws NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
    checkState(started, "JMX server is not yet started");
    checkArgument(!registeredMBeans.contains(objectName), "MBean by name {} is already registered.", objectName);

    mBeanServer.registerMBean(mBean, objectName);
    registeredMBeans.add(objectName);
  }

  public synchronized void stopJmxServer() throws Exception {
    if (!started) { return; }
    System.out.println("********** stopping test JMX server at port[" + jmxServerPort + "]");
    try {
      connectorServer.stop();
      started = false;

      for (ObjectName registeredMBean : registeredMBeans) {
        mBeanServer.unregisterMBean(registeredMBean);
      }
      registeredMBeans.clear();

    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }
  
  public int getJmxServerPort() {
    return jmxServerPort;
  }
}
