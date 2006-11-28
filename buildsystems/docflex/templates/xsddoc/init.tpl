<DOCFLEX_TEMPLATE VER='1.7'>
CREATED='2005-06-12 08:57:00'
LAST_UPDATE='2006-10-09 06:34:49'
DESIGNER_TOOL='DocFlex SDK 1.0'
TEMPLATE_TYPE='ProcedureTemplate'
DSM_TYPE_ID='xsddoc'
ROOT_ET='#DOCUMENTS'
<STYLES>
	CHAR_STYLE={
		style.name='Default Paragraph Font';
		style.id='cs1';
		style.default='true';
	}
	CHAR_STYLE={
		style.name='Hyperlink';
		style.id='cs2';
		text.decor.underline='true';
		text.color.foreground='#0000FF';
	}
	PAR_STYLE={
		style.name='Normal';
		style.id='s1';
		style.default='true';
	}
</STYLES>
<ROOT>
	<ELEMENT_ITER>
		DESCR='load all subschemas;\n\nAdditionally, the following hash-maps are created (see tab: Processing | Init/Step/Finish | Init Expression):\n\n(1) "loaded-schema" \n\nMaps each xs:import/xs:include/xs:redefine element to the schema it loads. The hash-key is the ID of the importing element. The result element is xs:schema.\n\n(2) "schema-loading-elements"\n\nMaps each schema to all xs:import/xs:include/xs:redefine elements referring to it. The hash-key is the xs:schema element\'s id; the result is the enumeration of all importing elements referring to that schema.\n\n(3) "redefined-component"\n\nMaps each redefining component (the one specified within <xs:redefine> element) to the original component it redefines. The hash-key is the redefining component\'s element id; the result is the element of the redefined component.\n\n(4) "redefining-components"\n\nMaps each original redefined component to all the components redefining it (those specified within <xs:redefine> elements). The hash-key is the original component\'s element id; the result is the enumeration of redefining component elements.\n\nNote: Actually, the presence of multiple components redefining the same original one would points to the erroneous situation. That would mean that the same subschema has been redefined in several locations with the same target namespace. This may happen when two different main schemas are being documented in a single documentation and both of them redefine the same subschema however in different ways. Since the current XSDDoc implementation treats all open schemas as if they are imported from a single root one, such a multiple redefinition may cause wrong processing and produce incorrect documentation.'
		TARGET_ET='xs:schema'
		SCOPE='advanced-location-rules'
		RULES={
			{'*','#DOCUMENT/xs:schema'};
		}
		INIT_EXPR='createElementMap("loaded-schema");\ncreateElementMap("schema-loading-elements");\ncreateElementMap("redefined-component");\ncreateElementMap("redefining-components");\n'
		<BODY>
			<SS_CALL>
				SS_NAME='Load Subschemas'
			</SS_CALL>
		</BODY>
	</ELEMENT_ITER>
	<FOLDER>
		DESCR='prepare hash-maps'
		<BODY>
			<FOLDER>
				DESCR='\'global-elements\', \'types\', \'groups\', \'attributeGroups\', \'global-attributes\''
				INIT_EXPR='keyQuery = FlexQuery (QName (\n  findPredecessorByType("xs:schema").getAttrStringValue("targetNamespace"),\n  getAttrStringValue("name")\n));\n\nprepareElementMap (\n  "global-elements",\n  findElementsByLPath("#DOCUMENT / xs:schema / xs:element"), \n  keyQuery\n);\n\nprepareElementMap (\n  "types",\n  findElementsByLPath (\n    "#DOCUMENT / xs:schema / descendant::(xs:simpleType | xs:complexType)"\n  ),\n  keyQuery\n);\n\nprepareElementMap (\n  "groups",\n  findElementsByLPath ("#DOCUMENT / xs:schema / descendant::xs:group"),\n  keyQuery\n);\n\nprepareElementMap (\n  "attributeGroups",\n  findElementsByLPath (\n    "#DOCUMENT / xs:schema / descendant::xs:attributeGroup"\n  ),\n  keyQuery\n);\n\nprepareElementMap (\n  "global-attributes",\n  findElementsByLPath("#DOCUMENT / xs:schema / xs:attribute"),\n  keyQuery\n);'
				<BODY>
				</BODY>
			</FOLDER>
			<FOLDER>
				DESCR='\'element-usage\''
				INIT_EXPR='// prepare generation of the element\'s key for "element-usage" map\nkeyQuery = FlexQuery ({\n\n // the element for which the key should be generated is currently \n // the generator\'s context element\n\n // in case there\'s a reference to a global element\n\n (ref = getAttrStringValue("ref")) != "" ?  { \n   findElementByKey ("global-elements", QName(ref)).id \n } : {\n\n // in case the element type specified\n\n (type = getAttrStringValue("type")) != "" ? {\n\n   schema = getXMLDocument().findChild ("xs:schema");\n   nsURI = schema.getAttrStringValue("targetNamespace");\n   name = getAttrStringValue("name");\n\n   HashKey (nsURI, name, QName (type));\n } : { \n\n // in any other cases\n contextElement.id\n\n}}});\n\nprepareElementMap ("element-usage",\n  findElementsByLPath ("#DOCUMENT / xs:schema / descendant::xs:%localElement"), \n  keyQuery\n)'
				<BODY>
				</BODY>
			</FOLDER>
			<FOLDER>
				DESCR='\'type-usage\''
				INIT_EXPR='// prepare generation of the type keys for a particular location\nkeysQuery = FlexQuery ({\n\n  // direct type of an element or attribute\n  instanceOf ("xs:%element | xs:%attribute") ?\n    getAttrQNameValue("type") \n:\n  // extension or sestriction base\n  instanceOf ("xs:%extensionType | xs:%restrictionType | xs:restriction") ?\n    getAttrQNameValue("base") \n:\n  // list item\n  instanceOf ("xs:list") ? getAttrQNameValue("itemType") \n:\n  // union members\n  instanceOf ("xs:union") ? getAttrQNameValues("memberTypes") \n:\n  // neither of the above\n  null\n\n});\n\n// collect XML elements where types may be involved\n\ne = findElementsByLPath (\n  "#DOCUMENT / xs:schema / descendant::(xs:%element | xs:%attribute | \n     xs:%extensionType | xs:%restrictionType | xs:restriction | xs:list | xs:union)"\n);\n\n// eliminate repeating definition of local elements with \n// the same name and the same type\n\ne = e.filterElementsByKey (FlexQuery ({\n  instanceOf("xs:%localElement") && (type = getAttrStringValue ("type")) != "" ?\n     HashKey (getAttrStringValue("name"), QName(type)) : contextElement.id\n}));\n\n// other filter: element attributes with prohibited use\n\ne = e.filterElements (BooleanQuery ({\n  ! (instanceOf ("xs:%attribute") && getAttrValue ("use") == "prohibited")\n}));\n\nprepareElementMap ("type-usage", e, keysQuery)'
				<BODY>
				</BODY>
			</FOLDER>
			<FOLDER>
				DESCR='\'group-usage\', \'attributeGroup-usage\', \'attribute-usage\''
				INIT_EXPR='prepareElementMap ("group-usage",\n  findElementsByLPath (\'#DOCUMENT / xs:schema / descendant::xs:%groupRef\'), \n  FlexQuery (getAttrQNameValue("ref")),\n  FlexQuery (findPredecessorByType ("xs:%element;xs:complexType;xs:group"))\n);\n\nprepareElementMap ("attributeGroup-usage",\n  findElementsByLPath (\'#DOCUMENT / xs:schema / descendant::xs:%attributeGroupRef\'), \n  FlexQuery (getAttrQNameValue("ref")),\n  FlexQuery (findPredecessorByType ("xs:%element;xs:complexType;xs:attributeGroup"))\n);\n\nprepareElementMap ("attribute-usage",\n  findElementsByLPath (\'#DOCUMENT / xs:schema / descendant::xs:%attribute\'), \n  FlexQuery (getAttrQNameValue("ref"))\n)'
				<BODY>
				</BODY>
			</FOLDER>
			<FOLDER>
				DESCR='\'direct-subtypes\', \'indirect-subtypes\''
				INIT_EXPR='rule1 = \'xs:complexType -> {\n  findElementsByKey ("types", QName(\n    getValueByLPath ("(xs:simpleContent | xs:complexContent) / (xs:extension|xs:restriction)/@base").toString()\n  ))\n}::(xs:complexType | xs:simpleType)\';\n\nrule2 = \'xs:simpleType -> descendant::xs:restriction / {\n  findElementsByKey ("types", getAttrQNameValue("base"))\n}::xs:simpleType\';\n\nrule3 = \'xs:simpleType -> descendant::xs:list / {\n  findElementsByKey ("types", getAttrQNameValue("itemType"))\n}::xs:simpleType\';\n\nrule4 = \'xs:simpleType -> descendant::xs:union / {\n  generateVector (\n    getAttrValues("memberTypes"),\n    FlexQuery (findElementsByKey ("types", QName (toString (_element ))))\n  ).toEnum()\n}::xs:simpleType\';\n\nprepareElementMap ("direct-subtypes",\n  findElementsByLPath("#DOCUMENT / xs:schema / (xs:simpleType | xs:complexType)"),\n  FlexQuery (getElementIds (findElementsByLRules (\n    Array (\n      LocationRule (rule1, false), \n      LocationRule (rule2, false),\n      LocationRule (rule3, false),\n      LocationRule (rule4, false)\n    )\n  )))\n);\n\nprepareElementMap ("indirect-subtypes",\n  findElementsByLPath("#DOCUMENT / xs:schema / (xs:simpleType | xs:complexType)"),\n  FlexQuery ({\n    id = contextElement.id;\n    getElementIds (findElementsByLRules (\n      Array (\n        LocationRule (rule1, true), \n        LocationRule (rule2, true),\n        LocationRule (rule3, true),\n        LocationRule (rule4, true)\n      ),\n      "",\n      BooleanQuery (findPredecessorByType("xs:complexType;xs:simpleType").id != id)             \n    ))\n  })\n);'
				<BODY>
				</BODY>
			</FOLDER>
			<FOLDER>
				DESCR='\'derived-elements\', \'derived-attributes\''
				INIT_EXPR='createElementMap("derived-elements");\ncreateElementMap("derived-attributes");\n\niterate (\n  findElementsByLPath("#DOCUMENT/xs:schema/(xs:simpleType|xs:complexType)"),\n  @typeVar,\n  FlexQuery ({\n    type = typeVar.toElement();\n    typeId = type.id;\n\n    iterate (\n      Enum (\n        type,\n        findElementsByKey ("direct-subtypes", typeId),\n        findElementsByKey ("indirect-subtypes", typeId)\n      ),\n      @subtypeVar,\n      FlexQuery ({\n        subtype = subtypeVar.toElement();\n        schema = subtype.getXMLDocument().findChild("xs:schema");\n\n        subtypeQName = QName (\n          schema.getAttrStringValue("targetNamespace"), \n          subtype.getAttrStringValue("name")\n        );\n\n        locations = findElementsByKey (\n          "type-usage", \n          subtypeQName,\n          BooleanQuery (instanceOf (\n            "xs:%element | xs:%attribute |\n             xs:%extensionType | xs:%restrictionType |\n             xs:list | xs:restriction | xs:union"\n          ))\n        );\n\n        iterate (\n          locations, \n          @loc,\n          FlexQuery ({\n            el = loc.toElement();\n\n            (! el.instanceOf ("xs:%element | xs:%attribute")) ?\n              el = el.findPredecessorByType("xs:%element;xs:%attribute");\n\n            el.instanceOf ("xs:%element") ?\n               putElementByKey ("derived-elements", typeId, el) :\n            el.instanceOf ("xs:%attribute") ?\n               putElementByKey ("derived-attributes", typeId, el);\n          })\n        )\n      })\n    )\n  })\n)'
				<BODY>
				</BODY>
			</FOLDER>
			<FOLDER>
				DESCR='\'containing-elements\'  -- a hash-map that allows to quickly find all those schema elements which may contain a given element (that is, the given element belongs to the content model of those elements we want to find).\n\nThe hash-key represents the element component for which we want finds the elements containing it. The hash-key value may be of two different types:\n\n(1) if the element component is specified with \'type\' attribute, the hash-key is \nHashKey (elementName, QName (type))\n\n(2) Otherwise, the hash-key is the GOMElement.id of the element component.\n\nThe hash-map does not include entries for the element components specified by reference. To find the containg elements for them, the referenced global element should be used.\n\nThe set of the contaning elements stored in each hash-map entry is filtered by the same hash-key. That is, different element components with the same name and non-empty \'type\' attribute are reduced to only one of them.'
				INIT_EXPR='rules = Array (\n  LocationRule (\n    \'xs:%element -> {\n       findElementsByKey ("types", getAttrQNameValue("type"))\n    }::xs:complexType\',\n    false\n  ),\n  LocationRule (\n    \'xs:%element -> xs:complexType\',\n    false\n  ),\n  LocationRule (\n    \'xs:%element -> { \n       findElementsByKey ("global-elements", getAttrQNameValue("ref"))\n    }::xs:%element\',\n    true\n  ),\n  LocationRule (\n    \'xs:%complexType -> xs:complexContent / xs:extension / {\n       findElementsByKey ("types", getAttrQNameValue("base"))\n    }::xs:complexType\',\n    true\n  ),\n  LocationRule (\n    \'xs:%complexType -> xs:complexContent / (xs:extension | xs:restriction) / xs:%group\',\n    true\n  ),\n  LocationRule (\'xs:%complexType -> xs:%group\', true),\n  LocationRule (\'xs:%group -> (xs:%element | xs:%group)\', true),\n  LocationRule (\n    \'xs:%groupRef -> { \n       findElementsByKey ("groups", getAttrQNameValue("ref"))\n    }::xs:group\',\n    true\n  )\n);\n\nkeysQuery = FlexQuery (generateVector (\n  findElementsByLRules (rules, "xs:%element", BooleanQuery (getAttrStringValue("ref") == "")),\n  FlexQuery ({\n    el = toElement (_element);\n    type = el.getAttrStringValue ("type");\n    type != "" ? HashKey (el.getAttrStringValue("name"), QName(type)) : el.id\n  })\n));\n\nprepareElementMap ("containing-elements",  \n  filterElementsByKey (\n    findElementsByLPath (\n      \'#DOCUMENT / xs:schema / descendant::xs:%element [getAttrStringValue("ref") == ""]\'\n    ),\n    FlexQuery ({\n      (type = getAttrStringValue ("type")) != "" ? \n       HashKey (getAttrStringValue("name"), QName(type)) : contextElement.id\n    })\n  ),\n  keysQuery\n)'
				<BODY>
				</BODY>
			</FOLDER>
			<FOLDER>
				DESCR='\'content-elements\' -- a hash-map that allows to quickly find all content model elements for a given schema element, complexType or element group.\n\nThe hash-key is the GOMElement.id of the schema component for which we want to find the content model elements. \n\nTo reduce the hash-map size, it does not include the direct entries for all those <xs:element> components which are defined by reference or have a specified \'type\' attribute. To find content model for them, the referenced global element or type component should be used instead.\n\nThe content model elements associated with a hash-key includes all \'xs:%element\' components plus all \'xs:any\' wildcard components, which may also belong to a model.'
				INIT_EXPR='/** Prepare Location Rules to obtain the content model elements \nfor a given: element, complexType or group **/\n\nrules = Array (\n  LocationRule (\n    \'xs:%element -> {\n       findElementsByKey ("types", getAttrQNameValue("type"))\n    }::xs:complexType\',\n    false\n  ),\n  LocationRule (\n    \'xs:%element -> xs:complexType\', \n    false\n  ),\n  LocationRule (\n    \'xs:%complexType -> xs:complexContent / xs:extension / {\n        findElementsByKey ("types", getAttrQNameValue("base"))\n    }::xs:complexType\',\n    true\n  ),\n  LocationRule(\n    \'xs:%complexType -> xs:complexContent / (xs:extension | xs:restriction) / xs:%group\',\n    true\n  ),\n  LocationRule(\n    \'xs:%complexType -> xs:%group\',\n    true\n  ),\n  LocationRule(\n    \'xs:%group -> (xs:%element | xs:any | xs:%group)\',\n    true\n  ),\n  LocationRule(\n    \'xs:%groupRef -> { \n       findElementsByKey ("groups", getAttrQNameValue("ref"))\n    }::xs:group\',\n    true\n  )\n);\n\n/** The subquery which produces an enumeration of the content model\nelements for a given: element, complexType or group **/\n\nelemQuery = FlexQuery (findElementsByLRules (rules, "xs:%element | xs:any"));\n\n/** Collect all schema components for which we want to hash the content model \nelements in the hash-map. To reduce the hash-map size, exclude from this \nenumeration all those <xs:element> components which are defined by reference \nor have a specified \'type\' attribute **/\n\ncomponents = findElementsByLPath (\n  \'#DOCUMENT / xs:schema / descendant::xs:%element [\n      getAttrValue("ref") == "" && getAttrValue("type") == ""] |\n   #DOCUMENT / xs:schema / descendant::xs:%complexType |\n   #DOCUMENT / xs:schema / xs:group | \n   #DOCUMENT / xs:schema / xs:redefine / xs:group\'\n);\n\n/** Create the hash-map. The hash key is the component\'s unique ID **/ \n\nprepareElementMap (\n  "content-elements",  \n  components, \n  FlexQuery (contextElement.id),  \n  elemQuery\n)'
				<BODY>
				</BODY>
			</FOLDER>
			<FOLDER>
				DESCR='\'content-attributes\' -- a hash-map that allows to quickly find all attribute components associated with a given schema element, complexType or attributeGroup.\n\nThe hash-key is the GOMElement.id of the schema component for which we want to find the attributes. \n\nTo reduce the hash-map size, it does not include direct entries for all those <xs:element> components which are defined by reference or have a specified \'type\' attribute. To find content model for them, the referenced global element or type component should be used instead.\n\nThe attribute components associated with a hash-key includes all \'xs:%attribute\' components plus a \'xs:anyAttribute\' wildcard component (if its presence implied from the schema).'
				INIT_EXPR='/** Prepare Location Rules to obtain possible attributes \nfor a given: element, complexType or attributeGroup **/\n\nrules = Array (\n  LocationRule (\n    \'xs:%element -> {findElementsByKey ("types", getAttrQNameValue("type"))}::xs:complexType\',\n    false\n  ),\n  LocationRule (\n    \'xs:%element -> xs:complexType\',\n    false\n  ),\n  LocationRule (\n    \'xs:%complexType -> descendant::(xs:attribute%xs:attribute | xs:anyAttribute)\',\n    true\n  ),\n  LocationRule (\n    \'xs:%complexType -> descendant::xs:%attributeGroupRef / {\n       findElementsByKey ("attributeGroups", getAttrQNameValue("ref"))\n    }::xs:attributeGroup\',\n    true\n  ),\n  LocationRule (\n    \'xs:attributeGroup -> (xs:attribute | xs:anyAttribute)\',\n    true\n  ),\n  LocationRule (\n    \'xs:attributeGroup -> xs:attributeGroup / {\n       findElementsByKey ("attributeGroups", getAttrQNameValue("ref"))\n    }::xs:attributeGroup\',\n    true\n  ),\n  LocationRule (\n    \'xs:%complexType -> xs:complexContent / (xs:extension | xs:restriction) / {\n       findElementsByKey ("types", getAttrQNameValue("base"))\n    }::xs:complexType\',\n    true\n  ),\n  LocationRule (\n    \'xs:%complexType -> xs:simpleContent / (xs:extension | xs:restriction) / {\n       findElementsByKey ("types", getAttrQNameValue("base"))\n    }::xs:complexType\',\n    true\n  )\n);\n\n/** The subquery which produces an enumeration of the possible attributes\nfor a given: element, complexType or attributeGroup **/\n\nattrQuery = FlexQuery ({\n\n  /** First, produce the enumeration of all atribute declarations. \n  The location rules are designed and intepreted so that the further an attribute \n  is declared in the ancestor types chain the later it appears in the enumeration **/\n\n  e = findElementsByLRules (rules, "xs:%attribute | xs:anyAttribute");\n\n  /** Now, remove from the produced enumeration those declarations that refer\n  to the same attribute. The remaining ones will be those which appear first. \n  This is what you need, because an attribute declaration taken from the last \n  descendant type may override settings specified for the same attribute in \n  the ancestor types **/\n\n  e = filterElementsByKey (e, FlexQuery ({\n\n    // in case of anyAttribute the null key ensures that only first declaration will remain\n\n    instanceOf ("xs:anyAttribute") ? null : \n      ((ref = getAttrStringValue("ref")) != "") ? QName (ref) : \n        QName (\n          getXMLDocument().getValueByLPath ("xs:schema/@targetNamespace").toString(),\n          getAttrStringValue("name")\n        )\n  }));\n\n  /** Now, what\'s left is to remove the prohibited attributes **/\n\n  filterElements (e, BooleanQuery (getAttrValue("use") != "prohibited"));\n});\n\n/** Collect all schema components for which we want to hash the possible \ncontained attributes. To reduce the hash-map size, exclude from this enumeration \nall those <xs:element> components which are defined by reference or have \na specified \'type\' attribute **/\n\ncomponents = findElementsByLPath (\n  \'#DOCUMENT / xs:schema / descendant::xs:%element [\n      getAttrValue("ref") == "" && getAttrValue("type") == ""] |\n   #DOCUMENT / xs:schema / xs:complexType |\n   #DOCUMENT / xs:schema / xs:redefine / xs:complexType |\n   #DOCUMENT / xs:schema / xs:attributeGroup | \n   #DOCUMENT / xs:schema / xs:redefine / xs:attributeGroup\'\n);\n\n/** Create the hash-map. The hash key is the component\'s unique ID **/ \n\nprepareElementMap (\n  "content-attributes",  \n  components, \n  FlexQuery (contextElement.id),\n  attrQuery\n)'
				<BODY>
				</BODY>
			</FOLDER>
		</BODY>
	</FOLDER>
</ROOT>
<STOCK_SECTIONS>
	<ELEMENT_ITER>
		MATCHING_ET='xs:schema'
		TARGET_ETS={'xs:import';'xs:include';'xs:redefine'}
		SCOPE='simple-location-rules'
		RULES={
			{'*','(xs:import|xs:include|xs:redefine)'};
		}
		SS_NAME='Load Subschemas'
		<BODY>
			<SS_CALL>
				MATCHING_ET='xs:import'
				SS_NAME='Load Subschemas'
				PASSED_ELEMENT_EXPR='schemaLocation = getAttrStringValue("schemaLocation");\n(schemaLocation == "") ? \n  schemaLocation = getAttrStringValue("namespace");\n\nurl = resolveURL (\n  schemaLocation,\n  getXMLDocument().getAttrStringValue("xmlURI")\n);\n\n((xsdDocument = findXMLDocument (url)) == null) ?\n{\n  schema = loadXMLDocument (url)->findChild ("xs:schema");\n  (schema != null) ?\n  {\n     putElementByKey ("loaded-schema", contextElement.id, schema);\n     putElementByKey ("schema-loading-elements", schema.id, contextElement);\n\n     (hasXMLAttribute("namespace")) ? \n        schema.setXMLAttribute ("targetNamespace", getXMLAttribute("namespace"));\n\n     schema\n  }\n} : {\n  ((schema = xsdDocument.findChild ("xs:schema")) != null) ? \n  {\n     putElementByKey ("loaded-schema", contextElement.id, schema);\n     putElementByKey ("schema-loading-elements", schema.id, contextElement);\n  };\n\n  null\n}'
				PASSED_ELEMENT_MATCHING_ET='xs:schema'
			</SS_CALL>
			<SS_CALL>
				MATCHING_ET='xs:include'
				SS_NAME='Load Subschemas'
				PASSED_ELEMENT_EXPR='url = resolveURL (\n  getAttrStringValue("schemaLocation"),\n  getXMLDocument().getAttrStringValue("xmlURI")\n);\n\ntargetNS = stockSection.contextElement.getXMLAttribute("targetNamespace");\n\n((xsdDocument = findXMLDocument (url, targetNS)) == null) ?\n{\n  xsdDocument = loadXMLDocument (url, targetNS);\n  ((schema = xsdDocument->findChild ("xs:schema")) != null) ?\n  {\n     putElementByKey ("loaded-schema", contextElement.id, schema);\n     putElementByKey ("schema-loading-elements", schema.id, contextElement);\n\n     (targetNS != "") ? schema.setXMLAttribute ("targetNamespace", targetNS)\n                                 : schema.removeXMLAttribute ("targetNamespace");\n     schema\n  }\n} : {\n  ((schema = xsdDocument.findChild ("xs:schema")) != null) ?\n  {\n     putElementByKey ("loaded-schema", contextElement.id, schema);\n     putElementByKey ("schema-loading-elements", schema.id, contextElement);\n  };\n\n  null\n}'
				PASSED_ELEMENT_MATCHING_ET='xs:schema'
			</SS_CALL>
			<SS_CALL>
				MATCHING_ET='xs:redefine'
				SS_NAME='Redefine Schema'
				PARAMS_EXPR='url = resolveURL (\n  getAttrStringValue("schemaLocation"),\n  getXMLDocument().getAttrStringValue("xmlURI")\n);\n\ntargetNS = stockSection.contextElement.getXMLAttribute("targetNamespace");\n\n((xsdDocument = findXMLDocument (url, targetNS)) == null) ?\n{\n  xsdDocument = loadXMLDocument (url, targetNS);\n  ((schema = xsdDocument->findChild ("xs:schema")) != null) ? \n  {\n    putElementByKey ("loaded-schema", contextElement.id, schema);\n    putElementByKey ("schema-loading-elements", schema.id, contextElement);\n\n    (targetNS != "") ? schema.setXMLAttribute ("targetNamespace", targetNS)\n                                : schema.removeXMLAttribute ("targetNamespace");\n  }\n} : {\n  ((schema = xsdDocument->findChild ("xs:schema")) != null) ?\n  {\n     putElementByKey ("loaded-schema", contextElement.id, schema);\n     putElementByKey ("schema-loading-elements", schema.id, contextElement);\n  }\n};\n\nArray (schema)'
			</SS_CALL>
		</BODY>
	</ELEMENT_ITER>
	<FOLDER>
		DESCR='parameter: <schema> element of the redefined schema'
		COND='stockSection.param != null'
		MATCHING_ET='xs:redefine'
		SS_NAME='Redefine Schema'
		<BODY>
			<ELEMENT_ITER>
				DESCR='iterates by simpleTypes specified within <redefine> element'
				TARGET_ET='xs:simpleType'
				SCOPE='simple-location-rules'
				RULES={
					{'*','xs:simpleType'};
				}
				<BODY>
					<ELEMENT_ITER>
						DESCR='those types within the redefined schema are renamed by adding \'$ORIGINAL\' suffix\n(see tab: Processing | Init/Step/Finish | Step Expression)'
						CONTEXT_ELEMENT_EXPR='stockSection.param.toElement()'
						MATCHING_ET='xs:schema'
						TARGET_ET='xs:simpleType'
						SCOPE='simple-location-rules'
						RULES={
							{'*','xs:simpleType'};
						}
						STEP_EXPR='redefiningComp = sectionBlock.contextElement;\nredefiningName = redefiningComp.getAttrStringValue("name");\n\n((name = getAttrStringValue("name")) == redefiningName) ?\n{\n  name = name + "$ORIGINAL";\n  setXMLAttribute("name", name);\n};\n\n(name == redefiningName + "$ORIGINAL") ?\n{\n  putElementByKey ("redefining-components", contextElement.id, redefiningComp);\n  putElementByKey ("redefined-component", redefiningComp.id, contextElement);\n}'
						<BODY>
						</BODY>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='self-references in <restriction> bases within a redefining type are also renamed'
						TARGET_ET='xs:restriction'
						SCOPE='simple-location-rules'
						RULES={
							{'*','descendant::xs:restriction'};
						}
						STEP_EXPR='base = getAttrStringValue("base");\n\n(base == sectionBlock.contextElement.getAttrStringValue("base")) ?\n  setXMLAttribute("base", base + "$ORIGINAL");\n'
						<BODY>
						</BODY>
					</ELEMENT_ITER>
				</BODY>
			</ELEMENT_ITER>
			<ELEMENT_ITER>
				DESCR='iterates by complexTypes specified within <redefine> element'
				TARGET_ET='xs:complexType'
				SCOPE='simple-location-rules'
				RULES={
					{'*','xs:complexType'};
				}
				<BODY>
					<ELEMENT_ITER>
						DESCR='those types within the redefined schema are renamed by adding \'$ORIGINAL\' suffix\n(see tab: Processing | Init/Step/Finish | Step Expression)'
						CONTEXT_ELEMENT_EXPR='stockSection.param.toElement()'
						MATCHING_ET='xs:schema'
						TARGET_ET='xs:complexType'
						SCOPE='simple-location-rules'
						RULES={
							{'*','xs:complexType'};
						}
						STEP_EXPR='redefiningComp = sectionBlock.contextElement;\nredefiningName = redefiningComp.getAttrStringValue("name");\n\n((name = getAttrStringValue("name")) == redefiningName) ?\n{\n  name = name + "$ORIGINAL";\n  setXMLAttribute("name", name);\n};\n\n(name == redefiningName + "$ORIGINAL") ?\n{\n  putElementByKey ("redefining-components", contextElement.id, redefiningComp);\n  putElementByKey ("redefined-component", redefiningComp.id, contextElement);\n}'
						<BODY>
						</BODY>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='self-references in <restriction>/<extension> bases within a redefining type are also renamed'
						TARGET_ETS={'xs:extension%xs:extensionType';'xs:restriction%xs:complexRestrictionType'}
						SCOPE='simple-location-rules'
						RULES={
							{'*','descendant::(xs:extension%xs:extensionType|xs:restriction%xs:complexRestrictionType)'};
						}
						STEP_EXPR='base = getAttrStringValue("base");\n\n(base == sectionBlock.contextElement.getAttrStringValue("base")) ?\n  setXMLAttribute("base", base + "$ORIGINAL");\n'
						<BODY>
						</BODY>
					</ELEMENT_ITER>
				</BODY>
			</ELEMENT_ITER>
			<ELEMENT_ITER>
				DESCR='iterates by groups specified within <redefine> element'
				TARGET_ET='xs:group'
				SCOPE='simple-location-rules'
				RULES={
					{'*','xs:group'};
				}
				<BODY>
					<ELEMENT_ITER>
						DESCR='those groups within the redefined schema are renamed by adding \'$ORIGINAL\' suffix\n(see tab: Processing | Init/Step/Finish | Step Expression)'
						CONTEXT_ELEMENT_EXPR='stockSection.param.toElement()'
						MATCHING_ET='xs:schema'
						TARGET_ET='xs:group'
						SCOPE='simple-location-rules'
						RULES={
							{'*','xs:group'};
						}
						STEP_EXPR='redefiningComp = sectionBlock.contextElement;\nredefiningName = redefiningComp.getAttrStringValue("name");\n\n((name = getAttrStringValue("name")) == redefiningName) ?\n{\n  name = name + "$ORIGINAL";\n  setXMLAttribute("name", name);\n};\n\n(name == redefiningName + "$ORIGINAL") ?\n{\n  putElementByKey ("redefining-components", contextElement.id, redefiningComp);\n  putElementByKey ("redefined-component", redefiningComp.id, contextElement);\n}'
						<BODY>
						</BODY>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='self-references within a redefining group are also renamed'
						TARGET_ET='xs:%groupRef'
						SCOPE='simple-location-rules'
						RULES={
							{'*','descendant::xs:%groupRef'};
						}
						STEP_EXPR='ref = getAttrStringValue("ref");\n\n(ref == sectionBlock.contextElement.getAttrStringValue("name")) ?\n  setXMLAttribute("ref", ref + "$ORIGINAL");\n'
						<BODY>
						</BODY>
					</ELEMENT_ITER>
				</BODY>
			</ELEMENT_ITER>
			<ELEMENT_ITER>
				DESCR='iterates by attributeGroups specified within <redefine> element'
				TARGET_ET='xs:attributeGroup'
				SCOPE='simple-location-rules'
				RULES={
					{'*','xs:attributeGroup'};
				}
				<BODY>
					<ELEMENT_ITER>
						DESCR='those attributeGroups within the redefined schema are renamed by adding \'$ORIGINAL\' suffix (see tab: Processing | Init/Step/Finish | Step Expression)'
						CONTEXT_ELEMENT_EXPR='stockSection.param.toElement()'
						MATCHING_ET='xs:schema'
						TARGET_ET='xs:attributeGroup'
						SCOPE='simple-location-rules'
						RULES={
							{'*','xs:attributeGroup'};
						}
						STEP_EXPR='redefiningComp = sectionBlock.contextElement;\nredefiningName = redefiningComp.getAttrStringValue("name");\n\n((name = getAttrStringValue("name")) == redefiningName) ?\n{\n  name = name + "$ORIGINAL";\n  setXMLAttribute("name", name);\n};\n\n(name == redefiningName + "$ORIGINAL") ?\n{\n  putElementByKey ("redefining-components", contextElement.id, redefiningComp);\n  putElementByKey ("redefined-component", redefiningComp.id, contextElement);\n}'
						<BODY>
						</BODY>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='self-references within the redefining attributeGroup are also renamed'
						TARGET_ET='xs:%attributeGroupRef'
						SCOPE='simple-location-rules'
						RULES={
							{'*','descendant::xs:%attributeGroupRef'};
						}
						STEP_EXPR='ref = getAttrStringValue("ref");\n\n(ref == sectionBlock.contextElement.getAttrStringValue("name")) ?\n  setXMLAttribute("ref", ref + "$ORIGINAL");\n'
						<BODY>
						</BODY>
					</ELEMENT_ITER>
				</BODY>
			</ELEMENT_ITER>
		</BODY>
	</FOLDER>
</STOCK_SECTIONS>
CHECKSUM='UWtByUbWe0aC?2CtAFob7g'
</DOCFLEX_TEMPLATE>