/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.repository;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.validate.ConfigurationValidator;

/**
 * A {@link BeanRepository} that lets clients change the bean in it.
 */
public interface MutableBeanRepository extends BeanRepository {

  void setBean(XmlObject bean, String sourceDescription) throws XmlException;

  void addValidator(ConfigurationValidator validator);

  /**
   * For <strong>TESTS ONLY</strong>.
   */
  void saveCopyOfBeanInAnticipationOfFutureMutation();

  /**
   * For <strong>TESTS ONLY</strong>.
   */
  void didMutateBean();

}
