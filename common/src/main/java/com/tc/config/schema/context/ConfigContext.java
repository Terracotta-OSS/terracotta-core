/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.config.schema.context;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.repository.BeanRepository;

/**
 * Binds together a {@link BeanRepository} and a {@link DefaultValueProvider}, and provides convenience methods for
 * creating various items.
 */
public interface ConfigContext {

  void ensureRepositoryProvides(Class theClass);

  boolean hasDefaultFor(String xpath) throws XmlException;

  XmlObject defaultFor(String xpath) throws XmlException;

  boolean isOptional(String xpath) throws XmlException;

  IllegalConfigurationChangeHandler illegalConfigurationChangeHandler();

  XmlObject bean();

  Object syncLockForBean();

  void itemCreated(ConfigItem item);

}
