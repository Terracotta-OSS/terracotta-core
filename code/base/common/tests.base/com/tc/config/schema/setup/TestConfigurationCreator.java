/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.xmlbeans.XmlException;

import com.tc.config.schema.repository.ApplicationsRepository;
import com.tc.config.schema.repository.MutableBeanRepository;
import com.tc.util.Assert;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link ConfigurationCreator} that creates config appropriate for tests only.
 */
public class TestConfigurationCreator implements ConfigurationCreator {

  private final TestConfigBeanSet beanSet;
  private boolean                 loadedFromTrustedSource;
  private final Set               allRepositoriesStoredInto;

  public TestConfigurationCreator(TestConfigBeanSet beanSet, boolean trustedSource) {
    Assert.assertNotNull(beanSet);
    this.beanSet = beanSet;
    this.loadedFromTrustedSource = trustedSource;
    this.allRepositoriesStoredInto = new HashSet();
  }

  public synchronized MutableBeanRepository[] allRepositoriesStoredInto() {
    return (MutableBeanRepository[]) this.allRepositoriesStoredInto
        .toArray(new MutableBeanRepository[this.allRepositoriesStoredInto.size()]);
  }

  public boolean loadedFromTrustedSource() {
    return this.loadedFromTrustedSource;
  }
  
  public String describeSources() {
    return "Dynamically-generated configuration for tests";
  }

  public void createConfigurationIntoRepositories(MutableBeanRepository l1BeanRepository,
                                                  MutableBeanRepository l2sBeanRepository,
                                                  MutableBeanRepository systemBeanRepository,
                                                  ApplicationsRepository applicationsRepository)
      throws ConfigurationSetupException {
    try {
      l1BeanRepository.setBean(this.beanSet.clientBean(), "from test config");
      addRepository(l1BeanRepository);
      l1BeanRepository.saveCopyOfBeanInAnticipationOfFutureMutation();

      l2sBeanRepository.setBean(this.beanSet.serversBean(), "from test config");
      addRepository(l2sBeanRepository);
      l2sBeanRepository.saveCopyOfBeanInAnticipationOfFutureMutation();

      systemBeanRepository.setBean(this.beanSet.systemBean(), "from test config");
      addRepository(systemBeanRepository);
      systemBeanRepository.saveCopyOfBeanInAnticipationOfFutureMutation();

      String[] allNames = this.beanSet.applicationNames();
      for (int i = 0; i < allNames.length; ++i) {
        MutableBeanRepository repository = applicationsRepository.repositoryFor(allNames[i]);
        addRepository(repository);
        repository.setBean(this.beanSet.applicationBeanFor(allNames[i]), "from test config");
        repository.saveCopyOfBeanInAnticipationOfFutureMutation();
      }
    } catch (XmlException xmle) {
      throw new ConfigurationSetupException("Unable to set bean", xmle);
    }
  }

  public File directoryConfigurationLoadedFrom() {
    return null;
  }

  private synchronized void addRepository(MutableBeanRepository theRepository) {
    this.allRepositoriesStoredInto.add(theRepository);
  }

}
