/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.repository;

import com.tc.config.schema.validate.ConfigurationValidator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A mock {@link ApplicationsRepository}, for use in tests.
 */
public class MockApplicationsRepository implements ApplicationsRepository {

  private int    numRepositoryFors;
  private String lastApplicationName;

  private int    numApplicationNames;

  private Map    returnedRepositories;

  public MockApplicationsRepository() {
    this.returnedRepositories = new HashMap();

    reset();
  }

  public void reset() {
    this.numRepositoryFors = 0;
    this.lastApplicationName = null;

    this.numApplicationNames = 0;
  }
  
  public void addRepositoryValidator(ConfigurationValidator validator) {
    // nothing here yet
  }

  public MutableBeanRepository repositoryFor(String applicationName) {
    ++this.numRepositoryFors;
    this.lastApplicationName = applicationName;
    return (MutableBeanRepository) this.returnedRepositories.get(applicationName);
  }

  public Iterator applicationNames() {
    ++this.numApplicationNames;
    return this.returnedRepositories.keySet().iterator();
  }

  public String getLastApplicationName() {
    return lastApplicationName;
  }

  public int getNumApplicationNames() {
    return numApplicationNames;
  }

  public int getNumRepositoryFors() {
    return numRepositoryFors;
  }

  public void setReturnedRepositories(Map returnedRepositories) {
    this.returnedRepositories = returnedRepositories;
  }

}
