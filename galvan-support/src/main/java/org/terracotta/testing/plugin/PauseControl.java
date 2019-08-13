/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.plugin;

import com.tc.classloader.BuiltinService;
import com.tc.server.TCServerMain;
import com.tc.spi.Pauseable;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;

/**
 *
 */
@BuiltinService
public class PauseControl implements ServiceProvider {

  @Override
  public boolean initialize(ServiceProviderConfiguration spc, PlatformConfiguration pc) {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    try {
      ObjectName name = new ObjectName("org.terracotta:name=PauseControl");
      
      server.registerMBean(new PauseController(), name);
    } catch (InstanceAlreadyExistsException | MBeanRegistrationException | MalformedObjectNameException | NotCompliantMBeanException e) {
      
    }
    return false;
  }

  @Override
  public <T> T getService(long l, ServiceConfiguration<T> sc) {
    return null;
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.emptySet();
  }

  @Override
  public void prepareForSynchronization() throws ServiceProviderCleanupException {

  }
  
  private static class PauseController extends StandardMBean implements PauseControlInterface {
    private final Pauseable pause = TCServerMain.getServer();
    public PauseController() throws NotCompliantMBeanException {
      super(PauseControlInterface.class);
    }

    @Override
    public void pause(String path) {
      pause.pause(path);
    }

    @Override
    public void unpause(String path) {
      pause.unpause(path);
    }
    
  }
  
  private interface PauseControlInterface {
    void pause(String path);
    void unpause(String path);
  }
}
