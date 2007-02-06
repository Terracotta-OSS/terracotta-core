/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
import com.tc.object.config.schema.NewDSOApplicationConfig;
import com.tc.object.config.schema.NewDSOApplicationConfigObject;
import com.tc.object.config.schema.NewSpringApplicationConfig;
import com.tc.object.config.schema.NewSpringApplicationConfigObject;
import com.tc.util.Assert;
import com.terracottatech.config.Application;
import com.terracottatech.config.Client;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Servers;
import com.terracottatech.config.SpringApplication;
import com.terracottatech.config.System;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A base class for all TVS configuration setup managers.
 */
public class BaseTVSConfigurationSetupManager {

  private final MutableBeanRepository             clientBeanRepository;
  private final MutableBeanRepository             serversBeanRepository;
  private final MutableBeanRepository             systemBeanRepository;
  private final ApplicationsRepository            applicationsRepository;

  private final DefaultValueProvider              defaultValueProvider;
  private final XmlObjectComparator               xmlObjectComparator;
  private final IllegalConfigurationChangeHandler illegalConfigurationChangeHandler;

  private final Map                               dsoApplicationConfigs;
  private final Map                               springApplicationConfigs;

  public BaseTVSConfigurationSetupManager(DefaultValueProvider defaultValueProvider,
                                          XmlObjectComparator xmlObjectComparator,
                                          IllegalConfigurationChangeHandler illegalConfigurationChangeHandler) {
    Assert.assertNotNull(defaultValueProvider);
    Assert.assertNotNull(xmlObjectComparator);
    Assert.assertNotNull(illegalConfigurationChangeHandler);

    this.systemBeanRepository = new StandardBeanRepository(System.class);
    this.clientBeanRepository = new StandardBeanRepository(Client.class);
    this.serversBeanRepository = new StandardBeanRepository(Servers.class);
    this.applicationsRepository = new StandardApplicationsRepository();

    this.defaultValueProvider = defaultValueProvider;
    this.xmlObjectComparator = xmlObjectComparator;
    this.illegalConfigurationChangeHandler = illegalConfigurationChangeHandler;

    this.dsoApplicationConfigs = new HashMap();
    this.springApplicationConfigs = new HashMap();
  }

  protected final MutableBeanRepository clientBeanRepository() {
    return this.clientBeanRepository;
  }

  protected final MutableBeanRepository serversBeanRepository() {
    return this.serversBeanRepository;
  }

  protected final MutableBeanRepository systemBeanRepository() {
    return this.systemBeanRepository;
  }

  protected final ApplicationsRepository applicationsRepository() {
    return this.applicationsRepository;
  }

  protected final XmlObjectComparator xmlObjectComparator() {
    return this.xmlObjectComparator;
  }

  protected final void runConfigurationCreator(ConfigurationCreator configurationCreator)
    throws ConfigurationSetupException
  {
    configurationCreator.createConfigurationIntoRepositories(clientBeanRepository,
                                                             serversBeanRepository,
                                                             systemBeanRepository,
                                                             applicationsRepository);
  }

  public String[] applicationNames() {
    Set names = new HashSet();

    names.addAll(this.dsoApplicationConfigs.keySet());
    names.addAll(this.springApplicationConfigs.keySet());

    return (String[]) names.toArray(new String[names.size()]);
  }

  protected final ConfigContext createContext(BeanRepository beanRepository, File configFilePath) {
    Assert.assertNotNull(beanRepository);
    return new StandardConfigContext(beanRepository, this.defaultValueProvider, this.illegalConfigurationChangeHandler,
                                     configFilePath);
  }

  public synchronized NewDSOApplicationConfig dsoApplicationConfigFor(String applicationName) {
    // When we support multiple applications, just take this assertion out.
    Assert.eval(applicationName.equals(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME));

    NewDSOApplicationConfig out = (NewDSOApplicationConfig) this.dsoApplicationConfigs.get(applicationName);
    if (out == null) {
      out = createNewDSOApplicationConfig(applicationName);
      this.dsoApplicationConfigs.put(applicationName, out);
    }

    return out;
  }

  public synchronized NewSpringApplicationConfig springApplicationConfigFor(String applicationName) {
    // When we support multiple applications, just take this assertion out.
    Assert.eval(applicationName.equals(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME));

    NewSpringApplicationConfig out = (NewSpringApplicationConfig) this.springApplicationConfigs.get(applicationName);
    if (out == null) {
      out = createNewSpringApplicationConfig(applicationName);
      this.springApplicationConfigs.put(applicationName, out);
    }

    return out;
  }

  protected NewDSOApplicationConfig createNewDSOApplicationConfig(String applicationName) {
    return new NewDSOApplicationConfigObject(createContext(new ChildBeanRepository(this.applicationsRepository
        .repositoryFor(applicationName), DsoApplication.class, new ChildBeanFetcher() {
      public XmlObject getChild(XmlObject parent) {
        return ((Application) parent).getDso();
      }
    }), null));
  }

  protected NewSpringApplicationConfig createNewSpringApplicationConfig(String applicationName) {
    return new NewSpringApplicationConfigObject(createContext(new ChildBeanRepository(this.applicationsRepository
        .repositoryFor(applicationName), SpringApplication.class, new ChildBeanFetcher() {
      public XmlObject getChild(XmlObject parent) {
        return ((Application) parent).getSpring();
      }
    }), null));
  }

}
