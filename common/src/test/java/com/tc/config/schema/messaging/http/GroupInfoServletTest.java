/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.messaging.http;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.tc.config.schema.beanfactory.TerracottaDomainConfigurationDocumentBeanFactory;
import com.tc.config.schema.defaults.SchemaDefaultValueProvider;
import com.tc.config.schema.setup.ConfigurationCreator;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.ConfigurationSpec;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.L2ConfigurationSetupManagerImpl;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.StandardXMLFileConfigurationCreator;
import com.tc.config.schema.utils.StandardXmlObjectComparator;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

public class GroupInfoServletTest extends TCTestCase {
  L2ConfigurationSetupManager configSetupMgr;
  FileOutputStream            out;

  private class GroupInfoServletForTest extends GroupInfoServlet {
    @Override
    protected L2ConfigurationSetupManager getConfigurationManager() {
      return configSetupMgr;
    }

    @Override
    protected OutputStream getOutPutStream(HttpServletResponse response1) throws IOException {
      out = new FileOutputStream(getTempFile("temp.txt"));
      return out;
    }
  }

  private File           tcConfig = null;
  @Mock
  HttpServletResponse    response;
  private BufferedReader bufferedReader;

  @Override
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Override
  protected File getTempFile(String fileName) throws IOException {
    return getTempDirectoryHelper().getFile(fileName);
  }

  private synchronized void writeConfigFile(String fileContents) {
    try {
      FileOutputStream out1 = new FileOutputStream(tcConfig);
      IOUtils.write(fileContents, out1);
      out1.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  public void testCreateServerNameToTsaPortAndHostname() throws IOException, ConfigurationSetupException {
    tcConfig = getTempFile("bind-address.xml");
    String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                    + "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\"\n"
                    + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                    + "xsi:schemaLocation=\"http://www.terracotta.org/schema/terracotta-6.xsd\">\n\n"
                    + "<servers>\n<mirror-group group-name=\"DevGroup\">\n"
                    + "<server host=\"10.60.52.156\" name=\"S119D90\" bind=\"10.60.54.69\">\n\n"
                    + "<tsa-port bind=\"10.60.54.69\">9610</tsa-port>\n"
                    + "<jmx-port bind=\"10.60.52.156\">9620</jmx-port>\n"
                    + "<tsa-group-port bind=\"10.60.54.69\">9630</tsa-group-port>\n" + "</server>\n"
                    + "<server host=\"10.60.52.156\" name=\"S119D91\" >\n\n" + "<tsa-port>9640</tsa-port>\n"
                    + "<jmx-port>9650</jmx-port>\n" + "<tsa-group-port>9660</tsa-group-port>\n"
                    + " \n</server>\n</mirror-group>\n" + "\n<restartable enabled=\"false\" />\n"
                    + "<client-reconnect-window>120</client-reconnect-window>\n</servers>\n"
                    + "<clients>\n<logs>terracotta-logs-%i</logs>\n</clients>\n</tc:tc-config>\n";
    writeConfigFile(config);
    configSetupMgr = initializeAndGetL2ConfigurationSetupManager();
    GroupInfoServletForTest groupInfoServlet = new GroupInfoServletForTest();
    groupInfoServlet.doGet(null, response);
    String str;
    bufferedReader = new BufferedReader(new FileReader(getTempFile("temp.txt")));
    bufferedReader.readLine();
    str = bufferedReader.readLine();
    System.out.println(str);
    CharSequence s = "<name>10.60.54.69</name><tsa-port>9610</tsa-port>";
    Assert.assertTrue(str.contains(s));
    s = "<name>10.60.52.156</name><tsa-port>9640</tsa-port>";
    Assert.assertTrue(str.contains(s));

  }

  private L2ConfigurationSetupManager initializeAndGetL2ConfigurationSetupManager() throws ConfigurationSetupException {
    String cwdAsString = System.getProperty("user.dir");
    ConfigurationSpec configurationSpec = new ConfigurationSpec(tcConfig.getAbsolutePath(), null,
                                                                StandardConfigurationSetupManagerFactory.ConfigMode.L2,
                                                                new File(cwdAsString));

    ConfigurationCreator configurationCreator = new StandardXMLFileConfigurationCreator(
                                                                                        configurationSpec,
                                                                                        new TerracottaDomainConfigurationDocumentBeanFactory());

    configSetupMgr = new L2ConfigurationSetupManagerImpl(configurationCreator, "S119D90",
                                                         new SchemaDefaultValueProvider(),
                                                         new StandardXmlObjectComparator(),
                                                         new FatalIllegalConfigurationChangeHandler());

    return configSetupMgr;
  }
}
