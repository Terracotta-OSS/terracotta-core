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
package com.tc.server;

import java.io.IOException;
import java.net.URI;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.terracotta.config.service.ServiceConfigParser;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.w3c.dom.Element;

/**
 *
 */
public class TestServiceConfigParser implements ServiceConfigParser {

  @Override
  public Source getXmlSchema() throws IOException {
    return new StreamSource(getClass().getResource("/test.xsd").openStream());
  }

  @Override
  public URI getNamespace() {
    return URI.create("http://www.terracotta.org/config/test");
  }

  @Override
  public ServiceProviderConfiguration parse(Element fragment, String source) {
    return new TestServiceProviderConfiguration();
  }
  
}
