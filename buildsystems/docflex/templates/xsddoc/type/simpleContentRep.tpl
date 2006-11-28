<DOCFLEX_TEMPLATE VER='1.7'>
CREATED='2005-04-26 03:31:00'
LAST_UPDATE='2006-10-09 06:34:51'
DESIGNER_TOOL='DocFlex SDK 1.0'
TEMPLATE_TYPE='DocumentTemplate'
DSM_TYPE_ID='xsddoc'
ROOT_ETS={'xs:%complexType';'xs:%simpleType'}
<TEMPLATE_PARAMS>
	PARAM={
		param.name='showMarkup';
		param.type='boolean';
	}
</TEMPLATE_PARAMS>
FMT={
	doc.lengthUnits='pt';
	doc.hlink.style.link='cs2';
}
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
	CHAR_STYLE={
		style.name='XML Markup';
		style.id='cs3';
		text.color.foreground='#0000FF';
		text.option.nbsps='true';
	}
</STYLES>
<ROOT>
	<FOLDER>
		DESCR='in case of a global simpleType, first obtain the type\'s QName (see Processing). If this QName points to an XSD predefined type, print it as is'
		MATCHING_ET='xs:simpleType'
		INIT_EXPR='setVar ("typeQName", QName (\n  getXMLDocument().getValueByLPath ("xs:schema/@targetNamespace").toString(),\n  getAttrStringValue("name")\n));'
		<BODY>
			<AREA_SEC>
				COND='isXSPredefinedType (getVar ("typeQName").toQName())'
				<AREA>
					<CTRL_GROUP>
						<CTRLS>
							<DATA_CTRL>
								FMT={
									ctrl.option.text.noBlankOutput='true';
								}
								<DOC_HLINK>
									TARGET_KEYS={
										'contextElement.id';
										'"detail"';
									}
								</DOC_HLINK>
								FORMULA='getVar ("typeQName")'
							</DATA_CTRL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
			<SS_CALL>
				DESCR='otherwise, look into the type\'s inside'
				COND='sectionBlock.execSecNone'
				SS_NAME='simpleType'
			</SS_CALL>
		</BODY>
	</FOLDER>
	<SS_CALL>
		MATCHING_ET='xs:%localSimpleType'
		SS_NAME='simpleType'
	</SS_CALL>
	<SS_CALL>
		MATCHING_ET='xs:%complexType'
		SS_NAME='complexType'
	</SS_CALL>
</ROOT>
<STOCK_SECTIONS>
	<FOLDER>
		DESCR='this should be a complexType with simple content only'
		MATCHING_ET='xs:%complexType'
		SS_NAME='complexType'
		<BODY>
			<FOLDER>
				DESCR='switch to a restriction or an extension contained in it'
				CONTEXT_ELEMENT_EXPR='findElementByLPath("xs:simpleContent / (xs:extension | xs:restriction)")'
				MATCHING_ETS={'xs:extension%xs:simpleExtensionType';'xs:restriction%xs:simpleRestrictionType'}
				<BODY>
					<SS_CALL>
						DESCR='process the restriction'
						MATCHING_ET='xs:restriction%xs:simpleRestrictionType'
						SS_NAME='restriction'
					</SS_CALL>
					<SS_CALL>
						DESCR='in case of extension, proceed with the base type'
						MATCHING_ET='xs:extension%xs:simpleExtensionType'
						SS_NAME='typeByQName'
						PARAMS_EXPR='Array (getAttrQNameValue("base"))'
					</SS_CALL>
				</BODY>
			</FOLDER>
		</BODY>
	</FOLDER>
	<FOLDER>
		MATCHING_ETS={'xs:%simpleRestrictionType';'xs:restriction'}
		SS_NAME='restriction'
		<BODY>
			<ELEMENT_ITER>
				DESCR='in case, there are enumeration facets, they overwrite any enumerations defined in supertypes, therefore we are not interested in them'
				FMT={
					txtfl.delimiter.type='text';
					txtfl.delimiter.text=' | ';
				}
				TARGET_ET='xs:enumeration'
				SCOPE='simple-location-rules'
				RULES={
					{'*','xs:enumeration'};
				}
				<BODY>
					<AREA_SEC>
						COND='getBooleanParam("showMarkup")'
						<AREA>
							<CTRL_GROUP>
								FMT={
									txtfl.delimiter.type='none';
								}
								<CTRLS>
									<LABEL>
										FMT={
											text.style='cs3';
										}
										TEXT='"'
									</LABEL>
									<DATA_CTRL>
										ATTR='value'
									</DATA_CTRL>
									<LABEL>
										FMT={
											text.style='cs3';
										}
										TEXT='"'
									</LABEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</AREA_SEC>
					<AREA_SEC>
						COND='! getBooleanParam("showMarkup")'
						<AREA>
							<CTRL_GROUP>
								FMT={
									txtfl.delimiter.type='none';
								}
								<CTRLS>
									<LABEL>
										TEXT='"'
									</LABEL>
									<DATA_CTRL>
										ATTR='value'
									</DATA_CTRL>
									<LABEL>
										TEXT='"'
									</LABEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</AREA_SEC>
				</BODY>
				<HEADER>
					<AREA_SEC>
						COND='iterator.numItems > 1'
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<LABEL>
										TEXT='('
									</LABEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</AREA_SEC>
				</HEADER>
				<FOOTER>
					<AREA_SEC>
						COND='iterator.numItems > 1'
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<LABEL>
										TEXT=')'
									</LABEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</AREA_SEC>
				</FOOTER>
			</ELEMENT_ITER>
		</BODY>
		<ELSE>
			DESCR='if no enumeration facets, look what\'s defined in supertypes'
			<SS_CALL>
				COND='getAttrStringValue("base") != ""'
				SS_NAME='typeByQName'
				PARAMS_EXPR='Array (getAttrQNameValue("base"))'
			</SS_CALL>
			<SS_CALL>
				COND='getAttrStringValue("base") == ""'
				SS_NAME='simpleType'
				PASSED_ELEMENT_EXPR='findChild("xs:simpleType")'
				PASSED_ELEMENT_MATCHING_ET='xs:%localSimpleType'
			</SS_CALL>
		</ELSE>
	</FOLDER>
	<ELEMENT_ITER>
		MATCHING_ET='xs:%simpleType'
		FMT={
			sec.outputStyle='text-par';
			txtfl.delimiter.type='space';
		}
		TARGET_ETS={'xs:list';'xs:restriction';'xs:union'}
		SCOPE='simple-location-rules'
		RULES={
			{'*','(xs:list|xs:restriction|xs:union)'};
		}
		SS_NAME='simpleType'
		<BODY>
			<SS_CALL>
				MATCHING_ET='xs:restriction'
				SS_NAME='restriction'
			</SS_CALL>
			<FOLDER>
				MATCHING_ET='xs:list'
				<BODY>
					<AREA_SEC>
						COND='getAttrStringValue("itemType") != ""'
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<LABEL>
										FMT={
											text.font.style.italic='true';
										}
										TEXT='list of'
									</LABEL>
									<SS_CALL_CTRL>
										SS_NAME='typeByQName'
										PARAMS_EXPR='Array (getAttrQNameValue("itemType"))'
									</SS_CALL_CTRL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</AREA_SEC>
					<AREA_SEC>
						COND='getAttrStringValue("itemType") == ""'
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<LABEL>
										FMT={
											text.font.style.italic='true';
										}
										TEXT='list of'
									</LABEL>
									<SS_CALL_CTRL>
										SS_NAME='simpleType'
										PASSED_ELEMENT_EXPR='findChild("xs:simpleType")'
										PASSED_ELEMENT_MATCHING_ET='xs:%simpleType'
									</SS_CALL_CTRL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</AREA_SEC>
				</BODY>
			</FOLDER>
			<FOLDER>
				MATCHING_ET='xs:union'
				FMT={
					txtfl.delimiter.type='text';
					txtfl.delimiter.text=' | ';
				}
				<BODY>
					<ATTR_ITER>
						SCOPE='attr-values'
						ATTR='memberTypes'
						<BODY>
							<AREA_SEC>
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<SS_CALL_CTRL>
												SS_NAME='typeByQName'
												PARAMS_EXPR='Array (toQName (iterator.value))'
											</SS_CALL_CTRL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</AREA_SEC>
						</BODY>
					</ATTR_ITER>
					<ELEMENT_ITER>
						TARGET_ET='xs:%localSimpleType'
						SCOPE='simple-location-rules'
						RULES={
							{'*','xs:simpleType'};
						}
						<BODY>
							<SS_CALL>
								SS_NAME='simpleType'
							</SS_CALL>
						</BODY>
					</ELEMENT_ITER>
				</BODY>
				<HEADER>
					<AREA_SEC>
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<LABEL>
										TEXT='('
									</LABEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</AREA_SEC>
				</HEADER>
				<FOOTER>
					<AREA_SEC>
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<LABEL>
										TEXT=')'
									</LABEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</AREA_SEC>
				</FOOTER>
			</FOLDER>
		</BODY>
	</ELEMENT_ITER>
	<FOLDER>
		DESCR='params[0]: QName of the global type'
		SS_NAME='typeByQName'
		<BODY>
			<FOLDER>
				DESCR='if this is not a predefined XSD type, switch to an element representing it'
				COND='! isXSPredefinedType (toQName (stockSection.param))'
				CONTEXT_ELEMENT_EXPR='findElementByKey ("types", toQName (stockSection.param))'
				MATCHING_ETS={'xs:complexType';'xs:simpleType'}
				<BODY>
					<SS_CALL>
						DESCR='process a simpleType'
						MATCHING_ET='xs:simpleType'
						SS_NAME='simpleType'
					</SS_CALL>
					<SS_CALL>
						DESCR='process a complexType'
						MATCHING_ET='xs:complexType'
						SS_NAME='complexType'
					</SS_CALL>
				</BODY>
			</FOLDER>
		</BODY>
		<ELSE>
			DESCR='if no output has been produced, just print the type\'s QName itself'
			<AREA_SEC>
				<AREA>
					<CTRL_GROUP>
						<CTRLS>
							<DATA_CTRL>
								<DOC_HLINK>
									TARGET_KEYS={
										'findElementByKey ("types", toQName (stockSection.param)).id';
										'"detail"';
									}
								</DOC_HLINK>
								FORMULA='stockSection.param != "" ? stockSection.param : "anySimpleType"'
							</DATA_CTRL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</ELSE>
	</FOLDER>
</STOCK_SECTIONS>
CHECKSUM='jvMvxMGgK9zIlpP?TaJJ0Q'
</DOCFLEX_TEMPLATE>