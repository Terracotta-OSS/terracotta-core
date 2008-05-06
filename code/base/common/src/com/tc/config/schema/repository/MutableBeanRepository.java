/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
