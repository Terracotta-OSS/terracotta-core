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
		style.name='Normal Smaller';
		style.id='cs3';
		text.font.name='Arial';
		text.font.size='9';
	}
	PAR_STYLE={
		style.name='Profile Subheading';
		style.id='s2';
		text.font.name='Arial';
		text.font.size='9';
		text.font.style.bold='true';
		par.margin.top='6';
	}
</STYLES>
<ROOT>
	<ELEMENT_ITER>
		FMT={
			sec.outputStyle='list';
			text.style='cs3';
			list.type='delimited';
			list.margin.block='true';
		}
		TARGET_ET='xs:schema'
		SCOPE='advanced-location-rules'
		RULES={
			{'*','#DOCUMENT/xs:schema'};
		}
		FILTER='getAttrValue("targetNamespace") == getStringParam("nsURI")'
		SORTING='by-expr'
		SORTING_KEY={expr='getXMLDocument().getAttrStringValue("xmlName")',ascending}
		<BODY>
			<AREA_SEC>
				<AREA>
					<CTRL_GROUP>
						<CTRLS>
							<DATA_CTRL>
								<DOC_HLINK>
									TARGET_KEYS={
										'contextElement.id';
										'"detail"';
									}
								</DOC_HLINK>
								FORMULA='getXMLDocument().getAttrStringValue("xmlName")'
							</DATA_CTRL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</BODY>
		<HEADER>
			<AREA_SEC>
				FMT={
					par.style='s2';
					par.margin.top='0';
				}
				<AREA>
					<CTRL_GROUP>
						FMT={
							txtfl.delimiter.type='none';
						}
						<CTRLS>
							<DATA_CTRL>
								FORMULA='"Targeting Schemas (" + iterator.numItems + "):"'
							</DATA_CTRL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</HEADER>
	</ELEMENT_ITER>
	<FOLDER>
		DESCR='all components defined in this namesapace'
		<BODY>
			<AREA_SEC>
				FMT={
					sec.outputStyle='text-par';
					sec.indent.block='true';
					text.style='cs3';
					txtfl.delimiter.type='text';
					txtfl.delimiter.text=', ';
				}
				<AREA>
					<CTRL_GROUP>
						<CTRLS>
							<PANEL>
								COND='nsURI = getStringParam("nsURI");\n\ncount = countElementsByLPath(\n  \'#DOCUMENT / xs:schema [\n      getAttrValue("targetNamespace") == nsURI\n  ] / xs:element\'\n);\n\ncount > 0 ? { setVar ("count", count); true } : false'
								FMT={
									ctrl.size.width='197.3';
									text.option.nbsps='true';
									txtfl.delimiter.type='nbsp';
								}
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<DATA_CTRL>
												FORMULA='getVar("count")'
											</DATA_CTRL>
											<LABEL>
												COND='getVar("count").toInt() == 1'
												<DOC_HLINK>
													TARGET_KEYS={
														'getStringParam("nsURI")';
														'"global-element-summary"';
													}
												</DOC_HLINK>
												TEXT='global element'
											</LABEL>
											<LABEL>
												COND='getVar("count").toInt() > 1'
												<DOC_HLINK>
													TARGET_KEYS={
														'getStringParam("nsURI")';
														'"global-element-summary"';
													}
												</DOC_HLINK>
												TEXT='global elements'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</PANEL>
							<PANEL>
								COND='nsURI = getStringParam("nsURI");\n\ne = findElementsByLPath(\n  \'#DOCUMENT / xs:schema [\n      getAttrValue("targetNamespace") == nsURI\n  ] / descendant::xs:%localElement [getAttrValue("ref") == ""]\'\n);\n\ne = filterElementsByKey (e, FlexQuery ({\n  (type = getAttrStringValue("type")) != "" ? \n     HashKey (getAttrValue("name"), QName(type)) : contextElement.id\n}));\n\ncount = count (e);\n\ncount > 0 ? { setVar ("count", count); true } : false'
								FMT={
									ctrl.size.width='189';
									text.option.nbsps='true';
									txtfl.delimiter.type='nbsp';
								}
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<DATA_CTRL>
												FORMULA='getVar("count")'
											</DATA_CTRL>
											<LABEL>
												COND='getVar("count").toInt() == 1'
												<DOC_HLINK>
													TARGET_KEYS={
														'getStringParam("nsURI")';
														'"local-element-summary"';
													}
												</DOC_HLINK>
												TEXT='local element'
											</LABEL>
											<LABEL>
												COND='getVar("count").toInt() > 1'
												<DOC_HLINK>
													TARGET_KEYS={
														'getStringParam("nsURI")';
														'"local-element-summary"';
													}
												</DOC_HLINK>
												TEXT='local elements'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</PANEL>
							<PANEL>
								COND='nsURI = getStringParam("nsURI");\n\ncount = countElementsByLPath(\n  \'#DOCUMENT / xs:schema [\n      getAttrValue("targetNamespace") == nsURI\n  ] / descendant::xs:complexType\'\n);\n\ncount > 0 ? { setVar ("count", count); true } : false'
								FMT={
									ctrl.size.width='190.5';
									text.option.nbsps='true';
									txtfl.delimiter.type='nbsp';
								}
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<DATA_CTRL>
												FORMULA='getVar("count")'
											</DATA_CTRL>
											<LABEL>
												COND='getVar("count").toInt() == 1'
												<DOC_HLINK>
													TARGET_KEYS={
														'getStringParam("nsURI")';
														'"complexType-summary"';
													}
												</DOC_HLINK>
												TEXT='complexType'
											</LABEL>
											<LABEL>
												COND='getVar("count").toInt() > 1'
												<DOC_HLINK>
													TARGET_KEYS={
														'getStringParam("nsURI")';
														'"complexType-summary"';
													}
												</DOC_HLINK>
												TEXT='complexTypes'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</PANEL>
							<PANEL>
								COND='nsURI = getStringParam("nsURI");\n\ncount = countElementsByLPath(\n  \'#DOCUMENT / xs:schema [\n      getAttrValue("targetNamespace") == nsURI\n  ] / descendant::xs:simpleType\'\n);\n\ncount > 0 ? { setVar ("count", count); true } : false'
								FMT={
									ctrl.size.width='177.8';
									text.option.nbsps='true';
									txtfl.delimiter.type='nbsp';
								}
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<DATA_CTRL>
												FORMULA='getVar("count")'
											</DATA_CTRL>
											<LABEL>
												COND='getVar("count").toInt() == 1'
												<DOC_HLINK>
													TARGET_KEYS={
														'getStringParam("nsURI")';
														'"simpleType-summary"';
													}
												</DOC_HLINK>
												TEXT='simpleType'
											</LABEL>
											<LABEL>
												COND='getVar("count").toInt() > 1'
												<DOC_HLINK>
													TARGET_KEYS={
														'getStringParam("nsURI")';
														'"simpleType-summary"';
													}
												</DOC_HLINK>
												TEXT='simpleTypes'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</PANEL>
							<PANEL>
								COND='nsURI = getStringParam("nsURI");\n\ncount = countElementsByLPath(\n  \'#DOCUMENT / xs:schema [\n      getAttrValue("targetNamespace") == nsURI\n  ] / descendant::xs:group\'\n);\n\ncount > 0 ? { setVar ("count", count); true } : false'
								FMT={
									ctrl.size.width='194.3';
									text.option.nbsps='true';
									txtfl.delimiter.type='nbsp';
								}
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<DATA_CTRL>
												FORMULA='getVar("count")'
											</DATA_CTRL>
											<LABEL>
												COND='getVar("count").toInt() == 1'
												<DOC_HLINK>
													TARGET_KEYS={
														'getStringParam("nsURI")';
														'"group-summary"';
													}
												</DOC_HLINK>
												TEXT='element group'
											</LABEL>
											<LABEL>
												COND='getVar("count").toInt() > 1'
												<DOC_HLINK>
													TARGET_KEYS={
														'getStringParam("nsURI")';
														'"group-summary"';
													}
												</DOC_HLINK>
												TEXT='element groups'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</PANEL>
							<PANEL>
								COND='nsURI = getStringParam("nsURI");\n\ncount = countElementsByLPath(\n  \'#DOCUMENT / xs:schema [\n     getAttrValue("targetNamespace") == nsURI\n  ] / xs:attribute\'\n);\n\ncount > 0 ? { setVar ("count", count); true } : false'
								FMT={
									ctrl.size.width='198';
									text.option.nbsps='true';
									txtfl.delimiter.type='nbsp';
								}
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<DATA_CTRL>
												FORMULA='getVar("count")'
											</DATA_CTRL>
											<LABEL>
												COND='getVar("count").toInt() == 1'
												<DOC_HLINK>
													TARGET_KEYS={
														'getStringParam("nsURI")';
														'"attribute-summary"';
													}
												</DOC_HLINK>
												TEXT='global attribute'
											</LABEL>
											<LABEL>
												COND='getVar("count").toInt() > 1'
												<DOC_HLINK>
													TARGET_KEYS={
														'getStringParam("nsURI")';
														'"attribute-summary"';
													}
												</DOC_HLINK>
												TEXT='global attributes'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</PANEL>
							<PANEL>
								COND='nsURI = getStringParam("nsURI");\n\ncount = countElementsByLPath(\n  \'#DOCUMENT / xs:schema [\n      getAttrStringValue("targetNamespace") == nsURI\n  ] / descendant::xs:attributeGroup\'\n);\n\ncount > 0 ? { setVar ("count", count); true } : false'
								FMT={
									ctrl.size.width='196.5';
									text.option.nbsps='true';
									txtfl.delimiter.type='nbsp';
								}
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<DATA_CTRL>
												FORMULA='getVar("count")'
											</DATA_CTRL>
											<LABEL>
												COND='getVar("count").toInt() == 1'
												<DOC_HLINK>
													TARGET_KEYS={
														'getStringParam("nsURI")';
														'"attributeGroup-summary"';
													}
												</DOC_HLINK>
												TEXT='attribute group'
											</LABEL>
											<LABEL>
												COND='getVar("count").toInt() > 1'
												<DOC_HLINK>
													TARGET_KEYS={
														'getStringParam("nsURI")';
														'"attributeGroup-summary"';
													}
												</DOC_HLINK>
												TEXT='attribute groups'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</PANEL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</BODY>
		<HEADER>
			<AREA_SEC>
				FMT={
					par.style='s2';
				}
				<AREA>
					<CTRL_GROUP>
						<CTRLS>
							<LABEL>
								TEXT='Components:'
							</LABEL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</HEADER>
	</FOLDER>
</ROOT>
CHECKSUM='LJT2+YOHoQFU?U+5U7FusA'
</DOCFLEX_TEMPLATE>