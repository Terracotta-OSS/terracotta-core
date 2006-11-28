/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
