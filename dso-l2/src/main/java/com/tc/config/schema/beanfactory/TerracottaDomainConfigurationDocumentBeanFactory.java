/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.config.schema.beanfactory;

import com.tc.util.Assert;
import org.terracotta.config.TCConfigurationParser;
import org.terracotta.config.TcConfiguration;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A {@link ConfigBeanFactory} that creates {@link TerracottaDomainConfigurationDocument} beans.
 */
public class TerracottaDomainConfigurationDocumentBeanFactory implements ConfigBeanFactory {

  public TerracottaDomainConfigurationDocumentBeanFactory() {
    // Nothing here yet.
  }

  @Override
  public BeanWithErrors createBean(InputStream in, String sourceDescription, String source) throws IOException, SAXException {
    Assert.assertNotBlank(sourceDescription);

    Collection<SAXParseException> errors = new ArrayList<>();
    TcConfiguration document = TCConfigurationParser.parse(in, errors, source);
    
    return new BeanWithErrors(document, errors.toArray(new SAXParseException[] {}));
  }
}
