/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.context.StandardConfigContext;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.repository.ApplicationsRepository;
import com.tc.config.schema.repository.BeanRepository;
import com.tc.config.schema.repository.ChildBeanFetcher;
import com.tc.config.schema.repository.ChildBeanRepository;
import com.tc.config.schema.repository.MutableBeanRepository;
import com.tc.config.schema.repository.StandardApplicationsRepository;
import com.tc.config.schema.repository.StandardBeanRepository;
import com.tc.config.schema.utils.XmlObjectComparator;
import com.tc.object.config.schema.DSOApplicationConfig;
import com.tc.object.config.schema.DSOApplicationConfigObject;
import com.tc.util.Assert;
import com.terracottatech.config.Application;
import com.terracottatech.config.Client;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Servers;
import com.terracottatech.config.System;
import com.terracottatech.config.TcProperties;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A base class for all TVS configuration setup managers.
 */
public class BaseConfigurationSetupManager {
  private final String[]                          args;
  private final ConfigurationCreator              configurationCreator;
  private final MutableBeanRepository             clientBeanRepository;
  private final MutableBeanRepository             serversBeanRepository;
  private final MutableBeanRepository             systemBeanRepository;
  private final MutableBeanRepository             tcPropertiesRepository;
  private final ApplicationsRepository            applicationsRepository;

  protected final DefaultValueProvider            defaultValueProvider;
  private final XmlObjectComparator               xmlObjectComparator;
  private final IllegalConfigurationChangeHandler illegalConfigurationChangeHandler;

  private final Map                               dsoApplicationConfigs;
  private final Map                               springApplicationConfigs;

  public BaseConfigurationSetupManager(ConfigurationCreator configurationCreator,
                                       DefaultValueProvider defaultValueProvider,
                                       XmlObjectComparator xmlObjectComparator,
                                       IllegalConfigurationChangeHandler illegalConfigurationChangeHandler) {
    this((String[]) null, configurationCreator, defaultValueProvider, xmlObjectComparator,
         illegalConfigurationChangeHandler);
  }

  public BaseConfigurationSetupManager(String[] args, ConfigurationCreator configurationCreator,
                                       DefaultValueProvider defaultValueProvider,
                                       XmlObjectComparator xmlObjectComparator,
                                       IllegalConfigurationChangeHandler illegalConfigurationChangeHandler) {
    Assert.assertNotNull(configurationCreator);
    Assert.assertNotNull(defaultValueProvider);
    Assert.assertNotNull(xmlObjectComparator);
    Assert.assertNotNull(illegalConfigurationChangeHandler);

    this.args = args;
    this.configurationCreator = configurationCreator;
    this.systemBeanRepository = new StandardBeanRepository(System.class);
    this.clientBeanRepository = new StandardBeanRepository(Client.class);
    this.serversBeanRepository = new StandardBeanRepository(Servers.class);
    this.tcPropertiesRepository = new StandardBeanRepository(TcProperties.class);
    this.applicationsRepository = new StandardApplicationsRepository();

    this.defaultValueProvider = defaultValueProvider;
    this.xmlObjectComparator = xmlObjectComparator;
    this.illegalConfigurationChangeHandler = illegalConfigurationChangeHandler;

    this.dsoApplicationConfigs = new HashMap();
    this.springApplicationConfigs = new HashMap();
  }

  public String[] processArguments() {
    return args;
  }

  protected final MutableBeanRepository clientBeanRepository() {
    return this.clientBeanRepository;
  }

  public final MutableBeanRepository serversBeanRepository() {
    return this.serversBeanRepository;
  }

  protected final MutableBeanRepository systemBeanRepository() {
    return this.systemBeanRepository;
  }

  protected final MutableBeanRepository tcPropertiesRepository() {
    return this.tcPropertiesRepository;
  }

  protected final ApplicationsRepository applicationsRepository() {
    return this.applicationsRepository;
  }

  protected final XmlObjectComparator xmlObjectComparator() {
    return this.xmlObjectComparator;
  }

  protected final ConfigurationCreator configurationCreator() {
    return this.configurationCreator;
  }

  protected final void runConfigurationCreator(boolean isClient) throws ConfigurationSetupException {
    this.configurationCreator.createConfigurationIntoRepositories(clientBeanRepository, serversBeanRepository,
                                                                  systemBeanRepository, tcPropertiesRepository,
                                                                  applicationsRepository, isClient);
  }

  public String[] applicationNames() {
    Set names = new HashSet();

    names.addAll(this.dsoApplicationConfigs.keySet());
    names.addAll(this.springApplicationConfigs.keySet());

    return (String[]) names.toArray(new String[names.size()]);
  }

  public final ConfigContext createContext(BeanRepository beanRepository, File configFilePath) {
    Assert.assertNotNull(beanRepository);
    return new StandardConfigContext(beanRepository, this.defaultValueProvider, this.illegalConfigurationChangeHandler);
  }

  public synchronized DSOApplicationConfig dsoApplicationConfigFor(String applicationName) {
    // When we support multiple applications, just take this assertion out.
    Assert.eval(applicationName.equals(ConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME));

    DSOApplicationConfig out = (DSOApplicationConfig) this.dsoApplicationConfigs.get(applicationName);
    if (out == null) {
      out = createNewDSOApplicationConfig(applicationName);
      this.dsoApplicationConfigs.put(applicationName, out);
    }

    return out;
  }

  protected DSOApplicationConfig createNewDSOApplicationConfig(String applicationName) {
    return new DSOApplicationConfigObject(
                                          createContext(new ChildBeanRepository(this.applicationsRepository
                                                            .repositoryFor(applicationName), DsoApplication.class,
                                                                                new ChildBeanFetcher() {
                                                                                  public XmlObject getChild(XmlObject parent) {
                                                                                    return ((Application) parent)
                                                                                        .getDso();
                                                                                  }
                                                                                }), null));
  }

}
