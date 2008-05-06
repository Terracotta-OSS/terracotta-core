/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.repository;

import com.tc.config.schema.validate.ConfigurationValidator;
import com.tc.util.Assert;
import com.terracottatech.config.Application;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The standard implementation of {@link ApplicationsRepository}.
 */
public class StandardApplicationsRepository implements ApplicationsRepository {

  private final Map applications;
  private final Set configurationValidators;

  public StandardApplicationsRepository() {
    this.applications = new HashMap();
    this.configurationValidators = new HashSet();
  }

  public void addRepositoryValidator(ConfigurationValidator validator) {
    Assert.assertNotNull(validator);
    this.configurationValidators.add(validator);
  }

  public synchronized MutableBeanRepository repositoryFor(String applicationName) {
    Assert.assertNotBlank(applicationName);

    MutableBeanRepository out = (MutableBeanRepository) this.applications.get(applicationName);
    if (out == null) {
      out = new StandardBeanRepository(Application.class);

      Iterator iter = this.configurationValidators.iterator();
      while (iter.hasNext()) {
        out.addValidator((ConfigurationValidator) iter.next());
      }

      this.applications.put(applicationName, out);
    }

    return out;
  }

  public synchronized Iterator applicationNames() {
    List names = Arrays.asList(this.applications.keySet().toArray(new String[this.applications.size()]));
    return names.iterator();
  }

}
