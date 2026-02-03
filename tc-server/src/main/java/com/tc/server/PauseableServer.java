/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
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


import java.util.List;
import java.util.Map;


import org.terracotta.monitoring.PlatformStopException;
import org.terracotta.server.ServerJMX;
import org.terracotta.server.StopAction;

import com.tc.classloader.ServiceLocator;
import com.tc.config.ServerConfigurationManager;
import com.tc.l2.logging.TCLogbackLogging;
import com.tc.logging.TCLogging;
import com.tc.objectserver.core.impl.GuardianContext;
import com.tc.spi.Pauseable;
import com.tc.spi.SPIServer;
import java.util.Properties;

public class PauseableServer implements Pauseable, SPIServer {
    private final TCServerImpl impl;
    private final ServerConfigurationManager config;
    private final List<String> args;
    private final ServiceLocator loader;

    public PauseableServer(ServerConfigurationManager config, List<String> args, ServiceLocator loader, TCServerImpl impl) {
        this.impl = impl;
        this.config = config;
        this.args = args;
        this.loader = loader;
    }
 
    @Override
    public Map<String, ?> getStateMap() {
        return impl.getStateMap();
    }

    @Override
    public void pause(String path) {
        impl.pause(path);
    }

    @Override
    public void unpause(String path) {
        impl.unpause(path);
    }

    @Override
    public int getServerCount() {
        return config.getNumberOfServers();
    }

    @Override
    public String[] processArguments() {
        return args.toArray(new String[args.size()]);
    }

    @Override
    public void stop(StopAction... modes) {
        impl.stop(modes);
    }

    @Override
    public boolean stopIfPassive(StopAction... modes) {
        try {
            impl.stopIfPassive(modes);
        } catch (PlatformStopException stop) {
            warn("unable to stop server", stop);
            return false;
        }
        return true;
    }

    @Override
    public boolean stopIfActive(StopAction... modes) {
        try {
            impl.stopIfActive(modes);
        } catch (PlatformStopException stop) {
            warn("unable to stop server", stop);
            return false;
        }
        return true;
    }

    @Override
    public boolean isActive() {
        return impl.isActive();
    }

    @Override
    public boolean isStopped() {
        return impl.isStopped();
    }

    @Override
    public boolean isPassiveUnitialized() {
        return impl.isPassiveUnitialized();
    }

    @Override
    public boolean isPassiveStandby() {
        return impl.isPassiveStandby();
    }

    @Override
    public boolean isReconnectWindow() {
        return impl.isReconnectWindow();
    }

    @Override
    public String getState() {
        return impl.getState().toString();
    }

    @Override
    public long getStartTime() {
        return impl.getStartTime();
    }

    @Override
    public long getActivateTime() {
        return impl.getActivateTime();
    }

    @Override
    public String getIdentifier() {
        return impl.getL2Identifier();
    }

    @Override
    public int getClientPort() {
        return config.getServerConfiguration().getTsaPort().getPort();
    }

    @Override
    public int getServerPort() {
        return config.getServerConfiguration().getGroupPort().getPort();
    }

    @Override
    public String getServerHostName() {
        return config.getServerConfiguration().getHost();
    }

    @Override
    public int getReconnectWindowTimeout() {
        return config.getServerConfiguration().getClientReconnectWindow();
    }

    @Override
    public boolean waitUntilShutdown() {
        try {
            return impl.waitUntilShutdown();
        } finally {
            try {
            TCLogbackLogging.resetLogging();
            } catch (Exception e) {
            // Ignore
            }
        }
    }

    @Override
    public void dump() {
        impl.dump();
    }

    @Override
    public String getClusterState() {
        return impl.getClusterState(null);
    }

    @Override
    public String getConfiguration() {
        return config.rawConfigString();
    }

    @Override
    public ClassLoader getServiceClassLoader(ClassLoader parent, Class<?>... serviceClasses) {
        return new ServiceClassLoader(parent, serviceClasses);
    }

    @Override
    public <T> List<Class<? extends T>> getImplementations(Class<T> serviceClasses) {
        return loader.getImplementations(serviceClasses);
    }

    @Override
    public ServerJMX getManagement() {
      return new JMXRouter(impl.getJMX());
    }
    
    @Override
    public Properties getCurrentChannelProperties() {
    return GuardianContext.getCurrentChannelProperties();
    }

    @Override
    public void warn(String warning, Object...event) {
    TCLogging.getConsoleLogger().warn(warning, event);
    }

    @Override
    public void console(String message, Object... sub) {
    TCLogging.getConsoleLogger().info(message, sub);
    }

    @Override
    public void audit(String msg, Properties additional) {
    impl.audit(msg, additional);
    }

    @Override
    public void security(String msg, Properties additional) {
    impl.security(msg, additional);
    }
};