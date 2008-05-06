/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.repository;

import com.tc.config.schema.validate.ConfigurationValidator;

import java.util.Iterator;

/**
 * Gives you access to an {@link BeanRepository} for each application.
 */
public interface ApplicationsRepository {

  void addRepositoryValidator(ConfigurationValidator validator);
  
  MutableBeanRepository repositoryFor(String applicationName);

  Iterator applicationNames();

}
