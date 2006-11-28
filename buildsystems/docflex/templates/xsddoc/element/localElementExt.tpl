<DOCFLEX_TEMPLATE VER='1.7'>
CREATED='2004-06-21 01:50:00'
LAST_UPDATE='2006-10-09 06:34:50'
DESIGNER_TOOL='DocFlex SDK 1.0'
TEMPLATE_TYPE='DocumentTemplate'
DSM_TYPE_ID='xsddoc'
ROOT_ET='xs:%localElement'
DESCR='prints extension for the local element name which is allows it to be distinguished it from others'
<TEMPLATE_PARAMS>
	PARAM={
		param.name='targetFrame';
		param.type='string';
	}
</TEMPLATE_PARAMS>
FMT={
	doc.lengthUnits='pt';
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
	CHAR_STYLE={
		style.name='Name Modifier';
		style.id='cs3';
		text.font.name='Verdana';
		text.font.size='7';
		text.color.foreground='#B2B2B2';
	}
	PAR_STYLE={
		style.name='Normal';
		style.id='s1';
		style.default='true';
	}
</STYLES>
<ROOT>
	<AREA_SEC>
		DESCR='when the \'type\' attribute is specified'
		COND='getAttrValue("type") != ""'
		FMT={
			sec.outputStyle='text-par';
			text.style='cs3';
			txtfl.delimiter.type='none';
		}
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<LABEL>
						TEXT=' (type '
					</LABEL>
					<DATA_CTRL>
						FMT={
							text.font.style.italic='true';
							text.decor.underline='true';
						}
						<DOC_HLINK>
							TARGET_FRAME_EXPR='getStringParam("targetFrame")'
							TARGET_KEYS={
								'findElementByKey ("types", getAttrQNameValue("type")).id';
								'"detail"';
							}
						</DOC_HLINK>
						ATTR='type'
					</DATA_CTRL>
					<LABEL>
						TEXT=')'
					</LABEL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<AREA_SEC>
		DESCR='otherwise, when the element may be contained in the only another element'
		COND='sectionBlock.execSecNone'
		CONTEXT_ELEMENT_EXPR='key = contextElement.id;\n\ncountElementsByKey ("containing-elements", key) == 1 ? \n  findElementByKey ("containing-elements", key) : null'
		MATCHING_ET='xs:%element'
		FMT={
			sec.outputStyle='text-par';
			text.style='cs3';
			txtfl.delimiter.type='none';
		}
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<LABEL>
						TEXT=' (within '
					</LABEL>
					<SS_CALL_CTRL>
						FMT={
							text.font.style.italic='true';
							text.decor.underline='true';
						}
						SS_NAME='QName'
					</SS_CALL_CTRL>
					<LABEL>
						TEXT=')'
					</LABEL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<FOLDER>
		DESCR='otherwise, the element has embedded type and may be contained\nin multiple other elements'
		COND='sectionBlock.execSecNone'
		FMT={
			sec.outputStyle='text-par';
			text.style='cs3';
			txtfl.delimiter.type='none';
		}
		<BODY>
			<FOLDER>
				DESCR='when the element has embedded type'
				COND='hasChild ("xs:simpleType | xs:complexType")'
				<BODY>
					<AREA_SEC>
						DESCR='the extension of a global type'
						CONTEXT_ELEMENT_EXPR='findElementByLPath("\n  xs:complexType/xs:simpleContent/xs:extension |\n  xs:complexType/xs:complexContent/xs:extension\n")'
						MATCHING_ET='xs:%extensionType'
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<LABEL>
										TEXT='extension of '
									</LABEL>
									<DATA_CTRL>
										FMT={
											text.font.style.italic='true';
											text.decor.underline='true';
										}
										<DOC_HLINK>
											TARGET_FRAME_EXPR='getStringParam("targetFrame")'
											TARGET_KEYS={
												'findElementByKey ("types", getAttrQNameValue("base")).id';
												'"detail"';
											}
										</DOC_HLINK>
										ATTR='base'
									</DATA_CTRL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</AREA_SEC>
					<AREA_SEC>
						DESCR='the restriction of a global type'
						CONTEXT_ELEMENT_EXPR='findElementByLPath("\n  xs:simpleType/xs:restriction |\n  xs:complexType/xs:simpleContent/xs:restriction |\n  xs:complexType/xs:complexContent/xs:restriction\n")'
						MATCHING_ETS={'xs:%restrictionType';'xs:restriction'}
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<LABEL>
										TEXT='restriction of '
									</LABEL>
									<DATA_CTRL>
										FMT={
											text.font.style.italic='true';
											text.decor.underline='true';
										}
										<DOC_HLINK>
											TARGET_FRAME_EXPR='getStringParam("targetFrame")'
											TARGET_KEYS={
												'findElementByKey ("types", getAttrQNameValue("base")).id';
												'"detail"';
											}
										</DOC_HLINK>
										ATTR='base'
									</DATA_CTRL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</AREA_SEC>
					<AREA_SEC>
						DESCR='just embedded type -- no extension or restriction'
						COND='sectionBlock.execSecNone'
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<LABEL>
										TEXT='embedded'
									</LABEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</AREA_SEC>
				</BODY>
			</FOLDER>
			<AREA_SEC>
				DESCR='otherwise, no type information specified; assume \'anyType\''
				COND='sectionBlock.execSecNone'
				<AREA>
					<CTRL_GROUP>
						<CTRLS>
							<LABEL>
								FMT={
									text.font.style.italic='true';
								}
								TEXT='anyType'
							</LABEL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</BODY>
		<HEADER>
			<AREA_SEC>
				<AREA>
					<CTRL_GROUP>
						<CTRLS>
							<LABEL>
								TEXT=' (type '
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
</ROOT>
<STOCK_SECTIONS>
	<AREA_SEC>
		FMT={
			par.option.nowrap='true';
		}
		SS_NAME='QName'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<DATA_CTRL>
						<DOC_HLINK>
							TARGET_FRAME_EXPR='getStringParam("targetFrame")'
							TARGET_KEYS={
								'contextElement.id';
								'"detail"';
							}
						</DOC_HLINK>
						FORMULA='name = getAttrStringValue("name");\n\nschema = getXMLDocument().findChild ("xs:schema");\nnsURI = schema.getAttrStringValue("targetNamespace");\n\ninstanceOf ("xs:%localElement") ? \n{\n  ((form = getAttrStringValue("form")) == "") ? {\n    form = schema.getAttrStringValue ("elementFormDefault");\n  };\n\n  (form != "qualified") ? name : QName (nsURI, name)\n} \n: QName (nsURI, name, Enum (rootElement, contextElement))'
					</DATA_CTRL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
</STOCK_SECTIONS>
CHECKSUM='1fOfiLKC8mJbVxM8h3gz0w'
</DOCFLEX_TEMPLATE>