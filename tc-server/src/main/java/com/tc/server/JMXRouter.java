/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.server;

import com.tc.management.TerracottaManagement;
import com.tc.objectserver.impl.JMXSubsystem;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import org.terracotta.server.ServerJMX;

public class JMXRouter implements ServerJMX {

  private final JMXSubsystem system;

  public JMXRouter(JMXSubsystem system) {
    this.system = system;
  }

  @Override
  public String get(String target, String attr) {
    return system.get(target, attr);
  }

  @Override
  public String set(String target, String attr, String val) {
    return system.set(target, attr, val);
  }

  @Override
  public String call(String target, String cmd, String arg) {
    return system.call(target, cmd, arg);
  }

  @Override
  public void registerMBean(String target, Object object) {
    try {
      system.getServer().registerMBean(object, TerracottaManagement.createObjectName(null, target, TerracottaManagement.MBeanDomain.PUBLIC));
    } catch (InstanceAlreadyExistsException
            | MBeanRegistrationException
            | MalformedObjectNameException
            | NotCompliantMBeanException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public MBeanServer getMBeanServer() {
    return system.getServer();
  }
}

