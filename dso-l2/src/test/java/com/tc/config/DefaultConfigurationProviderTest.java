/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Configuration.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 *
 */
package com.tc.config;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcConfiguration;
import org.terracotta.entity.ServiceProviderConfiguration;

import com.terracotta.config.Configuration;
import com.terracotta.config.ConfigurationException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static com.tc.config.DefaultConfigurationProvider.CONFIG_FILE_PROPERTY_NAME;
import static com.tc.config.DefaultConfigurationProvider.DEFAULT_CONFIG_NAME;
import static com.tc.config.DefaultConfigurationProvider.Opt.CONFIG_PATH;
import java.nio.file.FileAlreadyExistsException;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultConfigurationProviderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  private DefaultConfigurationProvider provider;

  private List<ServiceProviderConfiguration> serviceProviderConfigurations =
      Collections.singletonList(mock(ServiceProviderConfiguration.class));

  private TcConfig tcConfig = mock(TcConfig.class);

  private List<Object> extendedConfigurations = Collections.singletonList(new Object());

  private String rawConfiguration = "<tc-config xmlns=\"http://www.terracotta.org/config\">\n" +
                                    "    <servers>\n" +
                                    "        <server>\n" +
                                    "            <logs>%(user.home)/terracotta/server-logs</logs>\n" +
                                    "        </server>\n" +
                                    "    </servers>\n" +
                                    "</tc-config>";

  @Before
  public void setUp() throws Exception {
    TcConfiguration tcConfiguration = mock(TcConfiguration.class);
    when(tcConfiguration.getServiceConfigurations()).thenReturn(serviceProviderConfigurations);
    when(tcConfiguration.getPlatformConfiguration()).thenReturn(tcConfig);
    when(tcConfiguration.getExtendedConfiguration(any())).thenReturn(extendedConfigurations);
    when(tcConfiguration.toString()).thenReturn(rawConfiguration);

    provider = new DefaultConfigurationProvider() {
      @Override
      protected TcConfiguration getTcConfiguration(Path configurationPath, ClassLoader serviceClassLoader) {
        return tcConfiguration;
      }
    };
  }

  @Test
  public void testExplicitConfigWithRealProvider() throws ConfigurationException {
    String configurationPath = this.getClass().getResource("/simple-tc-config.xml").getPath();
    String[] args = {CONFIG_PATH.getShortOption(), configurationPath };

    systemOutRule.clearLog();
    new DefaultConfigurationProvider().initialize(asList(args));

    assertThat(systemOutRule.getLog(),
               containsString("Attempting to load configuration from the file at '" + configurationPath + "'"));
    assertThat(systemOutRule.getLog(),
               containsString("Successfully loaded configuration from the file at '" + configurationPath + "'"));
    assertThat(systemOutRule.getLog(),
               containsString("The configuration specified by the configuration file at '" + configurationPath + "'"));
  }

  @Test
  public void testExplicitConfigWithShortOption() throws ConfigurationException {
    String[] args = {CONFIG_PATH.getShortOption(), this.getClass().getResource("/simple-tc-config.xml").getPath()};
    provider.initialize(asList(args));

    validateConfiguration(provider.getConfiguration());
  }

  @Test
  public void testExplicitConfigWithLongOption() throws ConfigurationException {
    String[] args = {CONFIG_PATH.getLongOption(), this.getClass().getResource("/simple-tc-config.xml").getPath()};
    provider.initialize(asList(args));

    validateConfiguration(provider.getConfiguration());
  }

  @Test
  public void testExplicitConfigWithNonExistentFile() throws Exception {
    Path temporaryPath = temporaryFolder.newFolder().toPath();
    Path configurationFile = temporaryPath.resolve("tc-config.xml").toAbsolutePath();
    Files.deleteIfExists(configurationFile);
    String[] args = {CONFIG_PATH.getShortOption(), configurationFile.toString()};

    String errorMessage = ".*using the .* option is not found";
    expectedException.expect(new ThrowableCauseMatcher(RuntimeException.class, errorMessage));
    provider.initialize(asList(args));
  }

  @Test
  public void testExplicitConfigWithSystemProperty() throws ConfigurationException {
    System.setProperty(CONFIG_FILE_PROPERTY_NAME,
                       this.getClass().getResource("/simple-tc-config.xml").getPath());
    try {
      provider.initialize(Collections.emptyList());

      validateConfiguration(provider.getConfiguration());
    } finally {
      System.clearProperty(CONFIG_FILE_PROPERTY_NAME);
    }
  }

  @Test
  public void testExplicitConfigWithSystemPropertyWithNonExistentFile() throws Exception {
    Path temporaryPath = temporaryFolder.newFolder().toPath();
    Path configurationFile = temporaryPath.resolve("tc-config.xml").toAbsolutePath();
    Files.deleteIfExists(configurationFile);
    System.setProperty(CONFIG_FILE_PROPERTY_NAME, configurationFile.toString());

    try {
      String errorMessage = ".*using the system property.*not found";
      expectedException.expect(new ThrowableCauseMatcher(RuntimeException.class, errorMessage));
      provider.initialize(Collections.emptyList());
    } finally {
      System.clearProperty(CONFIG_FILE_PROPERTY_NAME);
    }
  }

  @Test
  public void testWithDefaultConfigurationInWorkingDirectory() throws Exception {
    Path configurationFile = Paths.get(System.getProperty("user.dir")).resolve(DEFAULT_CONFIG_NAME);
    Files.copy(this.getClass().getResourceAsStream("/simple-tc-config.xml"), configurationFile);

    try {
      provider.initialize(Collections.emptyList());

      validateConfiguration(provider.getConfiguration());
    } finally {
      Files.delete(configurationFile);
    }
  }

  @Test
  public void testWithDefaultConfigurationInUserDirectory() throws Exception {
    Path configurationFile = Paths.get(System.getProperty("user.home")).resolve(DEFAULT_CONFIG_NAME);
    try {
      Files.copy(this.getClass().getResourceAsStream("/simple-tc-config.xml"), configurationFile);

      try {
        provider.initialize(Collections.emptyList());

        validateConfiguration(provider.getConfiguration());
      } finally {
        Files.delete(configurationFile);
      }
    } catch (FileAlreadyExistsException exists) {
      //  if the file exists, just skip this test
    }
  }

  @Test
  public void testWithDefaultConfigurationInInstallationDirectory() throws Exception {
    Path temporaryDirectory = temporaryFolder.newFolder("conf").toPath();
    System.setProperty(Directories.TC_INSTALL_ROOT_PROPERTY_NAME, temporaryDirectory.getParent().toString());
    Files.copy(this.getClass().getResourceAsStream("/simple-tc-config.xml"), Directories.getDefaultConfigFile().toPath());

    provider.initialize(Collections.emptyList());

    validateConfiguration(provider.getConfiguration());
  }

  @Test
  public void testGetConfigurationParamsDescription() throws Exception {
    String[] args = {CONFIG_PATH.getShortOption(), this.getClass().getResource("/simple-tc-config.xml").getPath()};

    provider.initialize(asList(args));

    assertThat(provider.getConfigurationParamsDescription(), containsString(CONFIG_PATH.getShortOption()));
  }

  private void validateConfiguration(Configuration configuration) {
    assertThat(configuration.getPlatformConfiguration(), is(tcConfig));
    assertThat(configuration.getServiceConfigurations(), is(serviceProviderConfigurations));
    assertThat(configuration.getRawConfiguration(), is(rawConfiguration));
    assertThat(configuration.getExtendedConfiguration(Configuration.class), is(extendedConfigurations));
  }

  private static class ThrowableCauseMatcher extends BaseMatcher<Throwable> {

    private final Class<? extends Throwable> exceptionType;
    private final String errorMessage;

    private ThrowableCauseMatcher(Class<? extends Throwable> exceptionType, String errorMessage) {
      this.exceptionType = exceptionType;
      this.errorMessage = errorMessage;
    }

    @Override
    public void describeTo(Description description) {
    }

    @Override
    public boolean matches(Object o) {
      if (!(o instanceof Throwable)) return false;
      Throwable other = (Throwable)o;
      Throwable cause = other.getCause();
      if (cause == null) return false;
      if (cause.getMessage() == null) return false;

      return cause.getClass().equals(exceptionType) && Pattern.compile(cause.getMessage()).matcher(cause.getMessage()).matches();
    }
  }
}