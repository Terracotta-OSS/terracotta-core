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
package org.terracotta.config.provider;

import org.terracotta.config.Configuration;
import org.terracotta.config.ConfigurationException;
import org.terracotta.config.ConfigurationProvider;

import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.terracotta.config.TcConfig;

public class TestConfigurationProvider implements ConfigurationProvider {

  @Override
  public void initialize(List<String> configurationParams) throws ConfigurationException {
    try {
      JAXBContext cxt = JAXBContext.newInstance(TcConfig.class);
      System.out.println(cxt.getClass().getClassLoader());
    }catch (JAXBException e) {
      
    }
    try {
      System.out.println(this.getClass().getClassLoader());
      Class<?> cxt = this.getClass().getClassLoader().loadClass(JAXBContext.class.getName());
      System.out.println(cxt.getClassLoader());
    }catch (ClassNotFoundException e) {
      
    }
  }

  @Override
  public Configuration getConfiguration() {
    return null;
  }

  @Override
  public String getConfigurationParamsDescription() {
    return null;
  }

  @Override
  public void close() {

  }
}
