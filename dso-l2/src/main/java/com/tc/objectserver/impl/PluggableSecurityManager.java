/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.impl;

import com.tc.net.core.BufferManagerFactory;
import com.tc.net.core.ClearTextBufferManagerFactory;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.util.Assert;

import java.net.URI;
import java.security.Principal;
import javax.net.ssl.SSLContext;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;

/**
 *
 */
public class PluggableSecurityManager implements TCSecurityManager {
  
  private final BufferManagerFactory buffers;

  public PluggableSecurityManager(ServiceRegistry registry) {
    BufferManagerFactory factory = null;
    try {
      factory = registry.getService(new BasicServiceConfiguration<>(BufferManagerFactory.class));
    } catch (ServiceException e) {
      Assert.fail("Multiple BufferManagerFactory implementations found!");
    }
    if (factory == null) {
      factory = new ClearTextBufferManagerFactory();
    }
    buffers = factory;
  }

  @Override
  public Principal authenticate(String username, char[] chars) {
    throw new UnsupportedOperationException("Not supported yet."); 
  }

  @Override
  public boolean isUserInRole(Principal principal, String roleName) {
    throw new UnsupportedOperationException("Not supported yet."); 
  }

  @Override
  public BufferManagerFactory getBufferManagerFactory() {
    return buffers;
  }

  @Override
  public SSLContext getSslContext() {
    throw new UnsupportedOperationException("Not supported yet."); 
  }

  @Override
  public String getIntraL2Username() {
    return null; 
  }

  @Override
  public char[] getPasswordFor(URI uri) {
    throw new UnsupportedOperationException("Not supported yet."); 
  }

  @Override
  public char[] getPasswordForTC(String user, String host, int port) {
    throw new UnsupportedOperationException("Not supported yet."); 
  }
  
}
