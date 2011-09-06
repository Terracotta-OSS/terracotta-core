/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

  public SchemaComponent.Ref getComponentRef() {
    throw new ImplementMe();
  }

  public int getComponentType() {
    return 0;
  }

  public String getSourceName() {
    return null;
  }

  public boolean blockExtension() {
    return false;
  }

  public boolean blockRestriction() {
    return false;
  }

  public SchemaStringEnumEntry enumEntryForString(String arg0) {
    return null;
  }

  public StringEnumAbstractBase enumForInt(int arg0) {
    return null;
  }

  public StringEnumAbstractBase enumForString(String arg0) {
    return null;
  }

  public boolean finalExtension() {
    return false;
  }

  public boolean finalList() {
    return false;
  }

  public boolean finalRestriction() {
    return false;
  }

  public boolean finalUnion() {
    return false;
  }

  public SchemaType[] getAnonymousTypes() {
    return null;
  }

  public int getAnonymousUnionMemberOrdinal() {
    return 0;
  }

  public SchemaAttributeModel getAttributeModel() {
    return null;
  }

  public SchemaProperty[] getAttributeProperties() {
    return null;
  }

  public SchemaProperty getAttributeProperty(QName arg0) {
    return null;
  }

  public SchemaType getAttributeType(QName arg0, SchemaTypeLoader arg1) {
    return null;
  }

  public QName getAttributeTypeAttributeName() {
    return null;
  }

  public SchemaType getBaseEnumType() {
    return null;
  }

  public SchemaType getBaseType() {
    return null;
  }

  public int getBuiltinTypeCode() {
    return 0;
  }

  public SchemaType getCommonBaseType(SchemaType arg0) {
    return null;
  }

  public SchemaField getContainerField() {
    return null;
  }

  public SchemaType getContentBasedOnType() {
    return null;
  }

  public SchemaParticle getContentModel() {
    return null;
  }

  public int getContentType() {
    return 0;
  }

  public int getDecimalSize() {
    return 0;
  }

  public int getDerivationType() {
    return 0;
  }

  public SchemaProperty[] getDerivedProperties() {
    return null;
  }

  public QName getDocumentElementName() {
    return null;
  }

  public SchemaProperty[] getElementProperties() {
    return null;
  }

  public SchemaProperty getElementProperty(QName arg0) {
    return null;
  }

  public SchemaTypeElementSequencer getElementSequencer() {
    return null;
  }

  public SchemaType getElementType(QName arg0, QName arg1, SchemaTypeLoader arg2) {
    return null;
  }

  public XmlAnySimpleType[] getEnumerationValues() {
    return null;
  }

  public Class getEnumJavaClass() {
    return null;
  }

  public XmlAnySimpleType getFacet(int arg0) {
    return null;
  }

  public String getFullJavaImplName() {
    return null;
  }

  public String getFullJavaName() {
    return null;
  }

  public Class getJavaClass() {
    return null;
  }

  public SchemaType getListItemType() {
    return null;
  }

  public QName getName() {
    return null;
  }

  public SchemaType getOuterType() {
    return null;
  }

  public String[] getPatterns() {
    return null;
  }

  public SchemaType getPrimitiveType() {
    return null;
  }

  public SchemaProperty[] getProperties() {
    return null;
  }

  public org.apache.xmlbeans.SchemaType.Ref getRef() {
    return null;
  }

  public String getShortJavaImplName() {
    return null;
  }

  public String getShortJavaName() {
    return null;
  }

  public int getSimpleVariety() {
    return 0;
  }

  public SchemaStringEnumEntry[] getStringEnumEntries() {
    return null;
  }

  public SchemaTypeSystem getTypeSystem() {
    return null;
  }

  public SchemaType getUnionCommonBaseType() {
    return null;
  }

  public SchemaType[] getUnionConstituentTypes() {
    return null;
  }

  public SchemaType[] getUnionMemberTypes() {
    return null;
  }

  public SchemaType[] getUnionSubTypes() {
    return null;
  }

  public Object getUserData() {
    return null;
  }

  public int getWhiteSpaceRule() {
    return 0;
  }

  public boolean hasAllContent() {
    return false;
  }

  public boolean hasAttributeWildcards() {
    return false;
  }

  public boolean hasElementWildcards() {
    return false;
  }

  public boolean hasPatternFacet() {
    return false;
  }

  public boolean hasStringEnumValues() {
    return false;
  }

  public boolean isAbstract() {
    return false;
  }

  public boolean isAnonymousType() {
    return false;
  }

  public boolean isAssignableFrom(SchemaType arg0) {
    return false;
  }

  public boolean isAttributeType() {
    return false;
  }

  public boolean isBounded() {
    return false;
  }

  public boolean isBuiltinType() {
    return false;
  }

  public boolean isCompiled() {
    return false;
  }

  public boolean isDocumentType() {
    return false;
  }

  public boolean isFacetFixed(int arg0) {
    return false;
  }

  public boolean isFinite() {
    return false;
  }

  public boolean isNoType() {
    return false;
  }

  public boolean isNumeric() {
    return false;
  }

  public boolean isOrderSensitive() {
    return false;
  }

  public boolean isPrimitiveType() {
    return false;
  }

  public boolean isSimpleType() {
    return false;
  }

  public boolean isSkippedAnonymousType() {
    return false;
  }

  public boolean isURType() {
    return false;
  }

  public boolean isValidSubstitution(QName arg0) {
    return false;
  }

  public boolean matchPatternFacet(String arg0) {
    return false;
  }

  public XmlAnySimpleType newValue(Object arg0) {
    return null;
  }

  public int ordered() {
    return 0;
  }

  public QNameSet qnameSetForWildcardAttributes() {
    return null;
  }

  public QNameSet qnameSetForWildcardElements() {
    return null;
  }

  public SchemaAnnotation getAnnotation() {
    return null;
  }

}
