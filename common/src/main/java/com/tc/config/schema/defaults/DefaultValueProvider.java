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
package com.tc.config.schema.defaults;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

/**
 * Knows how to tell you the default value for an element in the config.
 */
public interface DefaultValueProvider {

  boolean possibleForXPathToHaveDefault(String xpath);
  
  XmlObject defaultFor(SchemaType baseType, String xpath) throws XmlException;
  
  boolean hasDefault(SchemaType baseType, String xpath) throws XmlException;
  
  boolean isOptional(SchemaType baseType, String xpath) throws XmlException;

}
