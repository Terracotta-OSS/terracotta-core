/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
