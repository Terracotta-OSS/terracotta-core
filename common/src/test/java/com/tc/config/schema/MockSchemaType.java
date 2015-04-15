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
package com.tc.config.schema;

import org.apache.xmlbeans.QNameSet;
import org.apache.xmlbeans.SchemaAnnotation;
import org.apache.xmlbeans.SchemaAttributeModel;
import org.apache.xmlbeans.SchemaComponent;
import org.apache.xmlbeans.SchemaField;
import org.apache.xmlbeans.SchemaParticle;
import org.apache.xmlbeans.SchemaProperty;
import org.apache.xmlbeans.SchemaStringEnumEntry;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.SchemaTypeElementSequencer;
import org.apache.xmlbeans.SchemaTypeLoader;
import org.apache.xmlbeans.SchemaTypeSystem;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlAnySimpleType;

import com.tc.exception.ImplementMe;

import javax.xml.namespace.QName;

public class MockSchemaType implements SchemaType {

  @Override
  public SchemaComponent.Ref getComponentRef() {
    throw new ImplementMe();
  }

  @Override
  public int getComponentType() {
    return 0;
  }

  @Override
  public String getSourceName() {
    return null;
  }

  @Override
  public boolean blockExtension() {
    return false;
  }

  @Override
  public boolean blockRestriction() {
    return false;
  }

  @Override
  public SchemaStringEnumEntry enumEntryForString(String arg0) {
    return null;
  }

  @Override
  public StringEnumAbstractBase enumForInt(int arg0) {
    return null;
  }

  @Override
  public StringEnumAbstractBase enumForString(String arg0) {
    return null;
  }

  @Override
  public boolean finalExtension() {
    return false;
  }

  @Override
  public boolean finalList() {
    return false;
  }

  @Override
  public boolean finalRestriction() {
    return false;
  }

  @Override
  public boolean finalUnion() {
    return false;
  }

  @Override
  public SchemaType[] getAnonymousTypes() {
    return null;
  }

  @Override
  public int getAnonymousUnionMemberOrdinal() {
    return 0;
  }

  @Override
  public SchemaAttributeModel getAttributeModel() {
    return null;
  }

  @Override
  public SchemaProperty[] getAttributeProperties() {
    return null;
  }

  @Override
  public SchemaProperty getAttributeProperty(QName arg0) {
    return null;
  }

  @Override
  public SchemaType getAttributeType(QName arg0, SchemaTypeLoader arg1) {
    return null;
  }

  @Override
  public QName getAttributeTypeAttributeName() {
    return null;
  }

  @Override
  public SchemaType getBaseEnumType() {
    return null;
  }

  @Override
  public SchemaType getBaseType() {
    return null;
  }

  @Override
  public int getBuiltinTypeCode() {
    return 0;
  }

  @Override
  public SchemaType getCommonBaseType(SchemaType arg0) {
    return null;
  }

  @Override
  public SchemaField getContainerField() {
    return null;
  }

  @Override
  public SchemaType getContentBasedOnType() {
    return null;
  }

  @Override
  public SchemaParticle getContentModel() {
    return null;
  }

  @Override
  public int getContentType() {
    return 0;
  }

  @Override
  public int getDecimalSize() {
    return 0;
  }

  @Override
  public int getDerivationType() {
    return 0;
  }

  @Override
  public SchemaProperty[] getDerivedProperties() {
    return null;
  }

  @Override
  public QName getDocumentElementName() {
    return null;
  }

  @Override
  public SchemaProperty[] getElementProperties() {
    return null;
  }

  @Override
  public SchemaProperty getElementProperty(QName arg0) {
    return null;
  }

  @Override
  public SchemaTypeElementSequencer getElementSequencer() {
    return null;
  }

  @Override
  public SchemaType getElementType(QName arg0, QName arg1, SchemaTypeLoader arg2) {
    return null;
  }

  @Override
  public XmlAnySimpleType[] getEnumerationValues() {
    return null;
  }

  @Override
  public Class getEnumJavaClass() {
    return null;
  }

  @Override
  public XmlAnySimpleType getFacet(int arg0) {
    return null;
  }

  @Override
  public String getFullJavaImplName() {
    return null;
  }

  @Override
  public String getFullJavaName() {
    return null;
  }

  @Override
  public Class getJavaClass() {
    return null;
  }

  @Override
  public SchemaType getListItemType() {
    return null;
  }

  @Override
  public QName getName() {
    return null;
  }

  @Override
  public SchemaType getOuterType() {
    return null;
  }

  @Override
  public String[] getPatterns() {
    return null;
  }

  @Override
  public SchemaType getPrimitiveType() {
    return null;
  }

  @Override
  public SchemaProperty[] getProperties() {
    return null;
  }

  @Override
  public org.apache.xmlbeans.SchemaType.Ref getRef() {
    return null;
  }

  @Override
  public String getShortJavaImplName() {
    return null;
  }

  @Override
  public String getShortJavaName() {
    return null;
  }

  @Override
  public int getSimpleVariety() {
    return 0;
  }

  @Override
  public SchemaStringEnumEntry[] getStringEnumEntries() {
    return null;
  }

  @Override
  public SchemaTypeSystem getTypeSystem() {
    return null;
  }

  @Override
  public SchemaType getUnionCommonBaseType() {
    return null;
  }

  @Override
  public SchemaType[] getUnionConstituentTypes() {
    return null;
  }

  @Override
  public SchemaType[] getUnionMemberTypes() {
    return null;
  }

  @Override
  public SchemaType[] getUnionSubTypes() {
    return null;
  }

  @Override
  public Object getUserData() {
    return null;
  }

  @Override
  public int getWhiteSpaceRule() {
    return 0;
  }

  @Override
  public boolean hasAllContent() {
    return false;
  }

  @Override
  public boolean hasAttributeWildcards() {
    return false;
  }

  @Override
  public boolean hasElementWildcards() {
    return false;
  }

  @Override
  public boolean hasPatternFacet() {
    return false;
  }

  @Override
  public boolean hasStringEnumValues() {
    return false;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public boolean isAnonymousType() {
    return false;
  }

  @Override
  public boolean isAssignableFrom(SchemaType arg0) {
    return false;
  }

  @Override
  public boolean isAttributeType() {
    return false;
  }

  @Override
  public boolean isBounded() {
    return false;
  }

  @Override
  public boolean isBuiltinType() {
    return false;
  }

  @Override
  public boolean isCompiled() {
    return false;
  }

  @Override
  public boolean isDocumentType() {
    return false;
  }

  @Override
  public boolean isFacetFixed(int arg0) {
    return false;
  }

  @Override
  public boolean isFinite() {
    return false;
  }

  @Override
  public boolean isNoType() {
    return false;
  }

  @Override
  public boolean isNumeric() {
    return false;
  }

  @Override
  public boolean isOrderSensitive() {
    return false;
  }

  @Override
  public boolean isPrimitiveType() {
    return false;
  }

  @Override
  public boolean isSimpleType() {
    return false;
  }

  @Override
  public boolean isSkippedAnonymousType() {
    return false;
  }

  @Override
  public boolean isURType() {
    return false;
  }

  @Override
  public boolean isValidSubstitution(QName arg0) {
    return false;
  }

  @Override
  public boolean matchPatternFacet(String arg0) {
    return false;
  }

  @Override
  public XmlAnySimpleType newValue(Object arg0) {
    return null;
  }

  @Override
  public int ordered() {
    return 0;
  }

  @Override
  public QNameSet qnameSetForWildcardAttributes() {
    return null;
  }

  @Override
  public QNameSet qnameSetForWildcardElements() {
    return null;
  }

  @Override
  public SchemaAnnotation getAnnotation() {
    return null;
  }

}
