/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import org.apache.xmlbeans.XmlException;
import org.terracotta.license.LicenseConstants;

import com.google.common.collect.MapMaker;
import com.tc.config.schema.beanfactory.TerracottaDomainConfigurationDocumentBeanFactory;
import com.tc.config.schema.repository.StandardBeanRepository;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.ConfigurationSpec;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory.ConfigMode;
import com.tc.config.schema.setup.StandardXMLFileConfigurationCreator;
import com.tc.license.LicenseManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.SecurityInfo;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.object.bytecode.hook.DSOContext;
import com.tc.object.bytecode.hook.impl.DSOContextImpl;
import com.tc.util.ProductInfo;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

public class StandaloneL1Boot implements Callable<Object> {

  static {
    /*
     * Make sure the google-collections finalizer thread is not in the TC thread group otherwise we will pin the L1
     * loader in memory since the TCThreadGroup will have been loaded by the L1Loader and hence will provide a strong
     * ref from the finalizer thread to the loader.
     */
    Map<String, Object> dummy = new MapMaker().weakValues().makeMap();
    dummy.put("dummy", new Object());
  }

  private static TCLogger           logger = TCLogging.getLogger(StandaloneL1Boot.class);

  private final String              embeddedTcConfig;
  private final boolean             isURLConfig;
  private final ClassLoader         appLevelTimLoader;
  private final boolean             rejoin;

  private final SecurityInfo        securityInfo;
  private final Map<String, Object> env;

  public StandaloneL1Boot(String embeddedTcConfig, boolean isURLConfig, ClassLoader appLevelTimLoader, boolean rejoin,
                          Map<String, Object> env) {
    this.embeddedTcConfig = embeddedTcConfig;
    this.isURLConfig = isURLConfig;
    String username = null;
    if (isURLConfig) {
      username = URLConfigUtil.getUsername(embeddedTcConfig);
    }
    this.securityInfo = new SecurityInfo(username != null, username);
    this.appLevelTimLoader = appLevelTimLoader;
    this.rejoin = rejoin;
    this.env = env;
  }

  private File createConfigFile(final TCSecurityManager securityManager) {
    String config = resolveConfig(securityManager);

    File tmp;
    try {
      tmp = File.createTempFile("tc-config", ".xml");
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
    tmp.deleteOnExit();

    String fName = tmp.getAbsolutePath();
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(fName, false);
      fos.write(config.getBytes());
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeQuietly(fos);
    }

    return tmp;
  }

  private static void closeQuietly(Closeable c) {
    if (c != null) {
      try {
        c.close();
      } catch (IOException ioe) {
        // ignore
      }
    }
  }

  private String resolveConfig(TCSecurityManager securityManager) {
    TcConfig embedded;
    try {
      embedded = resolveEmbedded(securityManager);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    TcConfigDocument doc = TcConfigDocument.Factory.newInstance();
    TcConfig newTcConfig = doc.addNewTcConfig();

    // XXX: when/if we refactor or add new major sections, this set of things that we "copy" might
    // be wrong or incomplete!
    if (embedded.getServers() != null) {
      newTcConfig.setServers(embedded.getServers());
    }
    if (embedded.getSystem() != null) {
      newTcConfig.setSystem(embedded.getSystem());
    }
    if (embedded.getClients() != null) {
      newTcConfig.setClients(embedded.getClients());
    }
    if (embedded.getTcProperties() != null) {
      newTcConfig.setTcProperties(embedded.getTcProperties());
    }

    return doc.toString();
  }

  private TcConfig resolveEmbedded(TCSecurityManager securityManager) throws ConfigurationSetupException, XmlException {
    final String configText;

    if (isURLConfig) {
      String embeddedTcConfigURL = URLConfigUtil.translateSystemProperties(this.embeddedTcConfig);
      ConfigurationSpec configurationSpec = new ConfigurationSpec(embeddedTcConfigURL, ConfigMode.EXPRESS_L1,
                                                                  new File("."));
      StandardXMLFileConfigurationCreator creator = new StandardXMLFileConfigurationCreator(
                                                                                            configurationSpec,
                                                                                            new TerracottaDomainConfigurationDocumentBeanFactory(),
                                                                                            securityManager);
      StandardBeanRepository beanRepo = new StandardBeanRepository(Object.class);
      creator.createConfigurationIntoRepositories(beanRepo, beanRepo, beanRepo, beanRepo, true);
      configText = creator.rawConfigText();
    } else {
      configText = embeddedTcConfig;
    }

    return TcConfigDocument.Factory.parse(configText).getTcConfig();
  }

  @Override
  public Object call() throws Exception {
    // load license via normal methods before attempt to load it from application resource
    if (LicenseManager.getLicense() == null) {
      String licenseLocation = LicenseConstants.LICENSE_KEY_FILENAME;
      LicenseManager.loadLicenseFromStream(appLevelTimLoader.getResourceAsStream(licenseLocation), "resource "
                                                                                                   + licenseLocation);

    }

    TCSecurityManager securityManager = null;

    if (securityInfo.isSecure()) {
      if (!ProductInfo.getInstance().isEnterprise()) { throw new RuntimeException(
                                                                                  "You're trying to setup a secured environment, which requires a EE version of Terracotta"); }
      logger.info("Secured environment! Enabling SSL & will be authenticating as user '" + securityInfo.getUsername()
                  + "'");
      securityManager = DSOContextImpl.createSecurityManager(env);
    }

    File configFile = createConfigFile(securityManager);
    DSOContext context = DSOContextImpl.createStandaloneContext(configFile.getAbsolutePath(), appLevelTimLoader,
                                                                this.rejoin, securityManager, securityInfo);

    // run a class through transformer -- avoids ClassCirculartiryError
    String name = getClass().getName();
    ClassLoader loader = getClass().getClassLoader();
    byte[] bytes = com.terracotta.toolkit.express.loader.Util.extract(loader.getResourceAsStream(name.replace('.', '/')
        .concat(".class")));
    context.transform(loader, name, null, null, bytes);

    return context;
  }
}
