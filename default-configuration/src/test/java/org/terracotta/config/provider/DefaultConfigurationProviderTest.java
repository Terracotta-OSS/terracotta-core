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

import com.tc.classloader.ServiceLocator;
import com.tc.logging.TCLogging;
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

import org.terracotta.configuration.Configuration;
import org.terracotta.configuration.ConfigurationException;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;


import java.nio.file.FileAlreadyExistsException;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.terracotta.config.Consistency;
import org.terracotta.config.FailoverPriority;
import org.terracotta.configuration.ConfigurationProvider;
import com.tc.server.Directories;
import static org.terracotta.config.provider.DefaultConfigurationProvider.CONFIG_FILE_PROPERTY_NAME;
import static org.terracotta.config.provider.DefaultConfigurationProvider.DEFAULT_CONFIG_NAME;
import static org.terracotta.config.provider.DefaultConfigurationProvider.Opt.CONFIG_PATH;
import static com.tc.server.Directories.TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME;
import java.util.Arrays;
import static org.mockito.Mockito.doAnswer;
import org.terracotta.config.Servers;
import static org.terracotta.config.provider.DefaultConfigurationProvider.Opt.HELP;
import org.terracotta.server.Server;
import org.terracotta.server.ServerEnv;

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
    final TcConfiguration tcConfiguration = mock(TcConfiguration.class);
    when(tcConfig.getServers()).thenReturn(mock(Servers.class));
    when(tcConfiguration.getServiceConfigurations()).thenReturn(serviceProviderConfigurations);
    when(tcConfiguration.getPlatformConfiguration()).thenReturn(tcConfig);
    when(tcConfiguration.getExtendedConfiguration(any())).thenReturn(extendedConfigurations);
    when(tcConfiguration.toString()).thenReturn(rawConfiguration);

    Server server = mock(Server.class);
    doAnswer(a->{
      TCLogging.getConsoleLogger().info(a.getArgument(0).toString(), Arrays.copyOfRange(a.getArguments(), 1, a.getArguments().length));
      return null;
    }).when(server).console(org.mockito.ArgumentMatchers.any(String.class), any());
    ServerEnv.setServer(server);

    provider = new DefaultConfigurationProvider() {
      @Override
      protected TcConfiguration getTcConfiguration(Path configurationPath, ClassLoader serviceClassLoader) {
        return tcConfiguration;
      }
    };
  }

  @Test
  public void testNoVoterCount() throws Exception {
    final TcConfiguration tcConfiguration = mock(TcConfiguration.class);
    TcConfig config = mock(TcConfig.class);
    when(tcConfiguration.getPlatformConfiguration()).thenReturn(config);
    when(config.getServers()).thenReturn(mock(Servers.class));
    FailoverPriority priority = mock(FailoverPriority.class);
    when(config.getFailoverPriority()).thenReturn(priority);
    Consistency consistency = mock(Consistency.class);
    when(priority.getConsistency()).thenReturn(consistency);
    when(consistency.getVoter()).thenReturn(null);
    DefaultConfigurationProvider dp = new DefaultConfigurationProvider() {
      @Override
      protected TcConfiguration getTcConfiguration(Path configurationPath, ClassLoader serviceClassLoader) {
        return tcConfiguration;
      }
    };
    String old = System.setProperty(TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME, "true");
    try {
      dp.initialize(Collections.emptyList());

      int count = dp.getConfiguration().getFailoverPriority().getExternalVoters();
      assertEquals(0, count);
    } finally {
      if (old == null) {
        System.getProperties().remove(TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME);
      } else {
        System.setProperty(TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME, old);
      }
    }
  }
  
  @Test
  public void testServiceLoad() throws Exception {
    List<Class<? extends ConfigurationProvider>> loader = new ServiceLocator(this.getClass().getClassLoader()).getImplementations(ConfigurationProvider.class);
    Assert.assertTrue(loader.size() > 0);
  }
  
  @Test
  public void testJaxbLoad() throws Exception {
    ServiceLocator locate = new ServiceLocator(this.getClass().getClassLoader());
    List<Class<? extends ConfigurationProvider>> loader = locate.getImplementations(ConfigurationProvider.class);
    for (Class<? extends ConfigurationProvider> check : loader) {
      if (check.getName().contains("Test")) {
        check.newInstance().initialize(Collections.emptyList());
      }
    }
    Assert.assertTrue(loader.size() > 0);
  }  

  @Test
  public void testExplicitConfigWithRealProvider() throws ConfigurationException {
    String configurationPath = getFilePath(this.getClass().getResource("/simple-tc-config.xml"));
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
    String[] args = {CONFIG_PATH.getShortOption(), getFilePath(this.getClass().getResource("/simple-tc-config.xml")) };
    provider.initialize(asList(args));

    validateConfiguration(provider.getConfiguration());
  }

  @Test
  public void testExplicitConfigWithLongOption() throws ConfigurationException {
    String[] args = {CONFIG_PATH.getLongOption(), getFilePath(this.getClass().getResource("/simple-tc-config.xml"))};
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
    expectedException.expect(new ExceptionMatcher(ConfigurationException.class, errorMessage));
    provider.initialize(asList(args));
  }

  @Test
  public void testExplicitConfigWithSystemProperty() throws ConfigurationException {
    System.setProperty(CONFIG_FILE_PROPERTY_NAME,
                       getFilePath(this.getClass().getResource("/simple-tc-config.xml")));
    try {
      provider.initialize(Collections.<String>emptyList());

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
      expectedException.expect(new ExceptionMatcher(ConfigurationException.class, errorMessage));
      provider.initialize(Collections.<String>emptyList());
    } finally {
      System.clearProperty(CONFIG_FILE_PROPERTY_NAME);
    }
  }

  @Test
  public void testWithDefaultConfigurationInWorkingDirectory() throws Exception {
    Path configurationFile = Paths.get(System.getProperty("user.dir")).resolve(DEFAULT_CONFIG_NAME);
    Files.copy(this.getClass().getResourceAsStream("/simple-tc-config.xml"), configurationFile);

    try {
      provider.initialize(Collections.<String>emptyList());

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
        provider.initialize(Collections.<String>emptyList());

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
    Files.copy(this.getClass().getResourceAsStream("/simple-tc-config.xml"), Directories.getInstallationRoot().resolve("conf/tc-config.xml"));

    provider.initialize(Collections.<String>emptyList());

    validateConfiguration(provider.getConfiguration());
  }

  @Test
  public void testGetConfigurationParamsDescription() throws Exception {
    String[] args = {CONFIG_PATH.getShortOption(), getFilePath(this.getClass().getResource("/simple-tc-config.xml"))};

    provider.initialize(asList(args));

    assertThat(provider.getConfigurationParamsDescription(), containsString(CONFIG_PATH.getShortOption()));
  }

  @Test
  public void testHelp() throws Exception {
    String[] args = {HELP.getShortOption()};

    expectedException.expect(ConfigurationException.class);
    provider.initialize(asList(args));
  }

  private String getFilePath(URL resource) {
    try {
      return Paths.get(resource.toURI()).toString();
    } catch (URISyntaxException e) {
      throw new AssertionError("Unexpected URL parsing error", e);
    }
  }

  private void validateConfiguration(Configuration configuration) {
    assertThat(configuration.getServiceConfigurations(), is(serviceProviderConfigurations));
    assertThat(configuration.getRawConfiguration(), is(rawConfiguration));
    assertThat(configuration.getExtendedConfiguration(Configuration.class), is(extendedConfigurations));
  }

  private static class ExceptionMatcher extends BaseMatcher<Throwable> {

    private final Class<? extends Throwable> exceptionType;
    private final Pattern errorMessage;

    private ExceptionMatcher(Class<? extends Throwable> exceptionType, String errorMessage) {
      this.exceptionType = exceptionType;
      this.errorMessage = Pattern.compile(errorMessage);
    }

    @Override
    public void describeTo(Description description) {
    }

    @Override
    public boolean matches(Object o) {
      if (!(o instanceof Throwable)) return false;
      Throwable other = (Throwable)o;
      if (other.getMessage() == null) return false;

      return other.getClass().equals(exceptionType) && errorMessage.matcher(other.getMessage()).matches();
    }
  }

  private static class ThrowableCauseMatcher extends BaseMatcher<Throwable> {

    private final Class<? extends Throwable> exceptionType;
    private final Pattern errorMessage;

    private ThrowableCauseMatcher(Class<? extends Throwable> exceptionType, String errorMessage) {
      this.exceptionType = exceptionType;
      this.errorMessage = Pattern.compile(errorMessage);
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

      return cause.getClass().equals(exceptionType) && errorMessage.matcher(cause.getMessage()).matches();
    }
  }
}