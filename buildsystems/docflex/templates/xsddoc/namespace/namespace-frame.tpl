<DOCFLEX_TEMPLATE VER='1.7'>
CREATED='2004-06-21 01:50:00'
LAST_UPDATE='2006-10-09 06:34:51'
DESIGNER_TOOL='DocFlex SDK 1.0'
TEMPLATE_TYPE='DocumentTemplate'
DSM_TYPE_ID='xsddoc'
ROOT_ET='#DOCUMENTS'
<TEMPLATE_PARAMS>
	PARAM={
		param.name='nsURI';
		param.displayName='Namespace URI';
		param.type='string';
	}
</TEMPLATE_PARAMS>
<HTARGET>
	TARGET_KEYS={
		'getStringParam("nsURI")';
		'"summary"';
	}
</HTARGET>
FMT={
	doc.lengthUnits='pt';
}
<STYLES>
	CHAR_STYLE={
		style.name='Default Paragraph Font';
		style.id='cs1';
		style.default='true';
	}
	PAR_STYLE={
		style.name='Frame Heading';
		style.id='s1';
		text.font.size='11';
		par.option.nowrap='true';
	}
	PAR_STYLE={
		style.name='Frame Item';
		style.id='s2';
		text.font.size='9';
		par.option.nowrap='true';
	}
	PAR_STYLE={
		style.name='Frame Subheading';
		style.id='s3';
		text.font.size='11';
		par.margin.top='7.5';
		par.margin.bottom='3';
		par.option.nowrap='true';
	}
	CHAR_STYLE={
		style.name='Hyperlink';
		style.id='cs2';
		text.decor.underline='true';
		text.color.foreground='#0000FF';
	}
	PAR_STYLE={
		style.name='Normal';
		style.id='s4';
		style.default='true';
	}
</STYLES>
<ROOT>
	<AREA_SEC>
		FMT={
			par.style='s1';
			par.option.nowrap='true';
		}
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<DATA_CTRL>
						<DOC_HLINK>
							TARGET_FRAME_EXPR='"detailFrame"'
							TARGET_KEYS={
								'getStringParam("nsURI")';
								'"detail"';
							}
						</DOC_HLINK>
						FORMULA='(nsURI = getStringParam("nsURI")) != "" ? nsURI : "Global Namespace"'
					</DATA_CTRL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<ELEMENT_ITER>
		TARGET_ET='xs:%element'
		SCOPE='advanced-location-rules'
		RULES={
			{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getStringParam("nsURI")]/descendant::xs:%element'};
		}
		FILTER='getAttrStringValue("ref") == ""'
		SORTING='by-compound-key'
		SORTING_KEY={
			{expr='callStockSection("Element Location")',ascending,case_sensitive};
			{expr='getAttrValue("type") == "" ? contextElement.id\n\n/* if the element has an embedded type, there must be a separate \n doc for it. Since the first subkey may be repeating, this one ensures \n the whole compound key is always unique for such an element. */',ascending};
			unique
		}
		<BODY>
			<SS_CALL>
				FMT={
					par.style='s2';
				}
				SS_NAME='Element Location'
			</SS_CALL>
		</BODY>
		<HEADER>
			<AREA_SEC>
				FMT={
					par.style='s3';
				}
				<AREA>
					<CTRL_GROUP>
						<CTRLS>
							<LABEL>
								TEXT='All Elements'
							</LABEL>
							<DATA_CTRL>
								FORMULA='"(" + iterator.numItems + ")"'
							</DATA_CTRL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</HEADER>
	</ELEMENT_ITER>
	<ELEMENT_ITER>
		TARGET_ET='xs:complexType'
		SCOPE='advanced-location-rules'
		RULES={
			{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getStringParam("nsURI")]/xs:complexType'};
			{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getStringParam("nsURI")]/xs:redefine/xs:complexType'};
		}
		SORTING='by-attr'
		SORTING_KEY={lpath='@name',ascending,case_sensitive}
		<BODY>
			<SS_CALL>
				FMT={
					par.style='s2';
				}
				SS_NAME='QName'
			</SS_CALL>
		</BODY>
		<HEADER>
			<AREA_SEC>
				FMT={
					par.style='s3';
				}
				<AREA>
					<CTRL_GROUP>
						<CTRLS>
							<LABEL>
								TEXT='Complex Types'
							</LABEL>
							<DATA_CTRL>
								FORMULA='"(" + iterator.numItems + ")"'
							</DATA_CTRL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</HEADER>
	</ELEMENT_ITER>
	<ELEMENT_ITER>
		TARGET_ET='xs:simpleType'
		SCOPE='advanced-location-rules'
		RULES={
			{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getStringParam("nsURI")]/xs:simpleType'};
			{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getStringParam("nsURI")]/xs:redefine/xs:complexType'};
		}
		SORTING='by-attr'
		SORTING_KEY={lpath='@name',ascending,case_sensitive}
		<BODY>
			<SS_CALL>
				FMT={
					par.style='s2';
				}
				SS_NAME='QName'
			</SS_CALL>
		</BODY>
		<HEADER>
			<AREA_SEC>
				FMT={
					par.style='s3';
				}
				<AREA>
					<CTRL_GROUP>
						<CTRLS>
							<LABEL>
								TEXT='Simple Types'
							</LABEL>
							<DATA_CTRL>
								FORMULA='"(" + iterator.numItems + ")"'
							</DATA_CTRL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</HEADER>
	</ELEMENT_ITER>
	<ELEMENT_ITER>
		TARGET_ET='xs:group'
		SCOPE='advanced-location-rules'
		RULES={
			{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getStringParam("nsURI")]/xs:group'};
			{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getStringParam("nsURI")]/xs:redefine/xs:group'};
		}
		SORTING='by-attr'
		SORTING_KEY={lpath='@name',ascending,case_sensitive}
		<BODY>
			<SS_CALL>
				FMT={
					par.style='s2';
				}
				SS_NAME='QName'
			</SS_CALL>
		</BODY>
		<HEADER>
			<AREA_SEC>
				FMT={
					par.style='s3';
				}
				<AREA>
					<CTRL_GROUP>
						<CTRLS>
							<LABEL>
								TEXT='Element Groups'
							</LABEL>
							<DATA_CTRL>
								FORMULA='"(" + iterator.numItems + ")"'
							</DATA_CTRL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</HEADER>
	</ELEMENT_ITER>
	<ELEMENT_ITER>
		TARGET_ET='xs:attribute'
		SCOPE='advanced-location-rules'
		RULES={
			{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getStringParam("nsURI")]/xs:attribute'};
		}
		SORTING='by-attr'
		SORTING_KEY={lpath='@name',ascending,case_sensitive}
		<BODY>
			<SS_CALL>
				FMT={
					par.style='s2';
				}
				SS_NAME='QName'
			</SS_CALL>
		</BODY>
		<HEADER>
			<AREA_SEC>
				FMT={
					par.style='s3';
				}
				<AREA>
					<CTRL_GROUP>
						<CTRLS>
							<LABEL>
								TEXT='Attributes'
							</LABEL>
							<DATA_CTRL>
								FORMULA='"(" + iterator.numItems + ")"'
							</DATA_CTRL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</HEADER>
	</ELEMENT_ITER>
	<ELEMENT_ITER>
		TARGET_ET='xs:attributeGroup'
		SCOPE='advanced-location-rules'
		RULES={
			{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getStringParam("nsURI")]/xs:attributeGroup'};
			{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getStringParam("nsURI")]/xs:redefine/xs:attributeGroup'};
		}
		SORTING='by-attr'
		SORTING_KEY={lpath='@name',ascending,case_sensitive}
		<BODY>
			<SS_CALL>
				FMT={
					par.style='s2';
				}
				SS_NAME='QName'
			</SS_CALL>
		</BODY>
		<HEADER>
			<AREA_SEC>
				FMT={
					par.style='s3';
				}
				<AREA>
					<CTRL_GROUP>
						<CTRLS>
							<LABEL>
								TEXT='Attribute Groups'
							</LABEL>
							<DATA_CTRL>
								FORMULA='"(" + iterator.numItems + ")"'
							</DATA_CTRL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</HEADER>
	</ELEMENT_ITER>
</ROOT>
<STOCK_SECTIONS>
	<AREA_SEC>
		MATCHING_ET='xs:%element'
		FMT={
			sec.outputStyle='text-par';
			txtfl.delimiter.type='none';
		}
		SS_NAME='Element Location'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<SS_CALL_CTRL>
						SS_NAME='QName'
					</SS_CALL_CTRL>
					<TEMPLATE_CALL_CTRL>
						MATCHING_ET='xs:%localElement'
						TEMPLATE_FILE='../element/localElementExt.tpl'
						PASSED_PARAMS={
							'targetFrame','"detailFrame"';
						}
						OUTPUT_TYPE='included'
						DSM_MODE='pass-current-model'
					</TEMPLATE_CALL_CTRL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<AREA_SEC>
		SS_NAME='QName'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<DATA_CTRL>
						<DOC_HLINK>
							TARGET_FRAME_EXPR='"detailFrame"'
							TARGET_KEYS={
								'contextElement.id';
								'"detail"';
							}
						</DOC_HLINK>
						FORMULA='name = getAttrStringValue("name");\n\ninstanceOf ("xs:%localElement") ? \n{\n  ((form = getAttrStringValue("form")) == "") ? {\n    schema = getXMLDocument().findChild ("xs:schema");\n    form = schema.getAttrStringValue ("elementFormDefault");\n  };\n\n  (form != "qualified") ? name : QName (getStringParam("nsURI"), name)\n} \n: QName (getStringParam("nsURI"), name)'
					</DATA_CTRL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
</STOCK_SECTIONS>
CHECKSUM='vv3hYoWPh+kB5aPtccI21Q'
</DOCFLEX_TEMPLATE>