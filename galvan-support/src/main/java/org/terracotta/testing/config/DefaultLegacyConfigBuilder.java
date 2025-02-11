/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.testing.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.nio.file.Path;
import org.terracotta.testing.api.LegacyConfigBuilder;

/**
 *
 */
public class DefaultLegacyConfigBuilder implements LegacyConfigBuilder {
  private String namespaceFragment = "";
  private String serviceFragment = "";
  private StripeConfiguration stripeConfiguration;
  
  @Override
  public void withNamespaceFragment(final String namespaceFragment) {
    if (namespaceFragment == null) {
      throw new NullPointerException("Namespace fragment must be non-null");
    }
    this.namespaceFragment = namespaceFragment;
  }
  
  @Override
  public void withServiceFragment(final String serviceFragment) {
    if (serviceFragment == null) {
      throw new NullPointerException("Service fragment must be non-null");
    }
    this.serviceFragment = serviceFragment;
  }
  
  @Override
  public void withStripeConfiguration(StripeConfiguration config) {
    this.stripeConfiguration = config;
  }

  
  @Override
  public Path createConfig(Path stripeInstallationDir) throws IOException {
    Files.createDirectories(stripeInstallationDir);
    Path config = stripeInstallationDir.resolve("tc-config.xml");
    if (!Files.exists(config)) {
      TcConfigBuilder configBuilder = new TcConfigBuilder(stripeInstallationDir, stripeConfiguration.getServerNames(), stripeConfiguration.getServerPorts(), 
          stripeConfiguration.getServerGroupPorts(), stripeConfiguration.getTcProperties(),
          namespaceFragment, serviceFragment, stripeConfiguration.getReconnectWindow(), stripeConfiguration.getVoters());

      String tcConfig = configBuilder.build();
      try {
        Path tcConfigPath = Files.createFile(config);
        Files.write(tcConfigPath, tcConfig.getBytes(UTF_8));
        return tcConfigPath;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return config;
  }
  
}
