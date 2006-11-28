<DOCFLEX_TEMPLATE VER='1.7'>
CREATED='2005-10-13 03:37:00'
LAST_UPDATE='2006-10-09 06:34:52'
DESIGNER_TOOL='DocFlex SDK 1.0'
TEMPLATE_TYPE='DocumentTemplate'
DSM_TYPE_ID='xsddoc'
ROOT_ETS={'xs:complexType';'xs:simpleType'}
<TEMPLATE_PARAMS>
	PARAM={
		param.name='nsURI';
		param.displayName='type namespace URI';
		param.description='The namespace to which this type belongs';
		param.type='string';
	}
	PARAM={
		param.name='usageCount';
		param.description='number of locations where this type is used';
		param.type='int';
		param.defaultExpr='countElementsByKey (\n  "type-usage",\n  QName (getStringParam("nsURI"), getAttrStringValue("name"))\n)';
		param.hidden='true';
	}
	PARAM={
		param.name='contentMapKey';
		param.displayName='"content-attributes", "content-elements" map key';
		param.description='The key for "content-attributes" and "content-elements" maps to find items associated with this component';
		param.type='object';
		param.defaultExpr='contextElement.id';
		param.hidden='true';
	}
	PARAM={
		param.name='attributeCount';
		param.displayName='number of all attributes';
		param.description='total number of attributes declared for this component';
		param.type='int';
		param.defaultExpr='countElementsByKey (\n  "content-attributes", \n  getParam("contentMapKey"),\n  BooleanQuery (! instanceOf ("xs:anyAttribute"))\n)';
		param.hidden='true';
	}
	PARAM={
		param.name='elementCount';
		param.displayName='number of all content elements';
		param.description='total number of content elements declared for this component';
		param.type='int';
		param.defaultExpr='countElementsByKey (\n  "content-elements", \n  getParam("contentMapKey"),\n  BooleanQuery (! instanceOf ("xs:any"))\n)';
		param.hidden='true';
	}
	PARAM={
		param.name='ownAttributeCount';
		param.displayName='number of component\'s own attributes';
		param.description='number of attributes defined within this component';
		param.type='int';
		param.defaultExpr='countElementsByKey (\n  "content-attributes", \n  getParam("contentMapKey"),\n  BooleanQuery (\n    ! instanceOf ("xs:anyAttribute") &&\n    findPredecessorByType("xs:%element;xs:complexType;xs:attributeGroup").id \n    == rootElement.id\n  )\n)';
		param.hidden='true';
	}
	PARAM={
		param.name='ownElementCount';
		param.displayName='number of component\'s own content elements';
		param.description='number of content elements defined within this component';
		param.type='int';
		param.defaultExpr='countElementsByKey (\n  "content-elements", \n  getParam("contentMapKey"),\n  BooleanQuery (\n    ! instanceOf ("xs:any") &&\n    findPredecessorByType("xs:%element;xs:complexType;xs:group").id \n    == rootElement.id\n  )\n)';
		param.hidden='true';
	}
	PARAM={
		param.name='anyAttribute';
		param.displayName='component has any-attribute';
		param.description='indicates that the component allows any attributes';
		param.type='boolean';
		param.defaultExpr='checkElementsByKey (\n  "content-attributes", \n  getParam("contentMapKey"),\n  BooleanQuery (instanceOf ("xs:anyAttribute"))\n)';
		param.hidden='true';
	}
	PARAM={
		param.name='anyElement';
		param.displayName='component has any-content-element';
		param.description='indicates that the component allows any content elements';
		param.type='boolean';
		param.defaultExpr='checkElementsByKey (\n  "content-elements", \n  getParam("contentMapKey"),\n  BooleanQuery (instanceOf ("xs:any"))\n)';
		param.hidden='true';
	}
	PARAM={
		param.name='showNS';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='showSchema';
		param.type='boolean';
		param.boolean.default='true';
	}
</TEMPLATE_PARAMS>
FMT={
	doc.lengthUnits='pt';
	doc.hlink.style.link='cs4';
}
<STYLES>
	CHAR_STYLE={
		style.name='Code';
		style.id='cs1';
		text.font.name='Courier New';
		text.font.size='9';
	}
	CHAR_STYLE={
		style.name='Code Smaller';
		style.id='cs2';
		text.font.name='Courier New';
		text.font.size='8';
	}
	CHAR_STYLE={
		style.name='Default Paragraph Font';
		style.id='cs3';
		style.default='true';
	}
	CHAR_STYLE={
		style.name='Hyperlink';
		style.id='cs4';
		text.decor.underline='true';
		text.color.foreground='#0000FF';
	}
	PAR_STYLE={
		style.name='Normal';
		style.id='s1';
		style.default='true';
	}
	CHAR_STYLE={
		style.name='Property Title Font';
		style.id='cs5';
		text.font.size='8';
		text.font.style.bold='true';
		par.lineHeight='11';
		par.margin.right='7';
	}
	CHAR_STYLE={
		style.name='Property Value Font';
		style.id='cs6';
		text.font.name='Verdana';
		text.font.size='8';
		par.lineHeight='11';
	}
</STYLES>
<ROOT>
	<AREA_SEC>
		COND='getBooleanParam("showNS")'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<LABEL>
						FMT={
							text.style='cs5';
						}
						TEXT='Namespace:'
					</LABEL>
					<DELIMITER>
						FMT={
							text.style='cs1';
						}
					</DELIMITER>
					<DATA_CTRL>
						FMT={
							text.style='cs2';
						}
						<DOC_HLINK>
							TARGET_KEYS={
								'getStringParam("nsURI")';
								'"detail"';
							}
						</DOC_HLINK>
						FORMULA='(ns = getParam("nsURI")) != "" ? ns : "<global>"'
					</DATA_CTRL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<AREA_SEC>
		MATCHING_ET='xs:complexType'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<LABEL>
						FMT={
							text.style='cs5';
						}
						TEXT='Content:'
					</LABEL>
					<DELIMITER>
						FMT={
							text.style='cs1';
						}
					</DELIMITER>
					<PANEL>
						FMT={
							ctrl.size.width='413.3';
							text.style='cs6';
							txtfl.delimiter.type='text';
							txtfl.delimiter.text=', ';
						}
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<TEMPLATE_CALL_CTRL>
										TEMPLATE_FILE='contentType.tpl'
										OUTPUT_TYPE='included'
										DSM_MODE='pass-current-model'
									</TEMPLATE_CALL_CTRL>
									<PANEL>
										COND='count = getIntParam("attributeCount") + \n        getBooleanParam("anyAttribute").toInt();\n\ncount > 0 ? { setVar ("count", count); true } : false'
										FMT={
											ctrl.size.width='277.5';
										}
										<AREA>
											<CTRL_GROUP>
												<CTRLS>
													<DATA_CTRL>
														COND='getIntParam("attributeCount") > 0'
														FORMULA='getIntParam("attributeCount")'
													</DATA_CTRL>
													<DELIMITER>
														COND='getIntParam("attributeCount") > 0'
														FMT={
															txtfl.delimiter.type='text';
															txtfl.delimiter.text='+';
														}
													</DELIMITER>
													<LABEL>
														COND='getBooleanParam("anyAttribute")'
														TEXT='any'
													</LABEL>
													<DELIMITER>
														FMT={
															txtfl.delimiter.type='nbsp';
														}
													</DELIMITER>
													<LABEL>
														COND='getVar("count").toInt() == 1'
														<DOC_HLINK>
															TARGET_KEYS={
																'contextElement.id';
																'"attribute-detail"';
															}
														</DOC_HLINK>
														TEXT='attribute'
													</LABEL>
													<LABEL>
														COND='getVar("count").toInt() > 1'
														<DOC_HLINK>
															TARGET_KEYS={
																'contextElement.id';
																'"attribute-detail"';
															}
														</DOC_HLINK>
														TEXT='attributes'
													</LABEL>
												</CTRLS>
											</CTRL_GROUP>
										</AREA>
									</PANEL>
									<PANEL>
										COND='count = getIntParam("elementCount") + \n        getBooleanParam("anyElement").toInt();\n\ncount > 0 ? { setVar ("count", count); true } : false'
										FMT={
											ctrl.size.width='277.5';
											ctrl.size.height='38.3';
										}
										<AREA>
											<CTRL_GROUP>
												<CTRLS>
													<DATA_CTRL>
														COND='getIntParam("elementCount") > 0'
														FORMULA='getIntParam("elementCount")'
													</DATA_CTRL>
													<DELIMITER>
														COND='getIntParam("elementCount") > 0'
														FMT={
															txtfl.delimiter.type='text';
															txtfl.delimiter.text='+';
														}
													</DELIMITER>
													<LABEL>
														COND='getBooleanParam("anyElement")'
														TEXT='any'
													</LABEL>
													<DELIMITER>
														FMT={
															txtfl.delimiter.type='nbsp';
														}
													</DELIMITER>
													<LABEL>
														COND='getVar("count").toInt() == 1'
														<DOC_HLINK>
															TARGET_KEYS={
																'contextElement.id';
																'"content-element-detail"';
															}
														</DOC_HLINK>
														TEXT='element'
													</LABEL>
													<LABEL>
														COND='getVar("count").toInt() > 1'
														<DOC_HLINK>
															TARGET_KEYS={
																'contextElement.id';
																'"content-element-detail"';
															}
														</DOC_HLINK>
														TEXT='elements'
													</LABEL>
												</CTRLS>
											</CTRL_GROUP>
										</AREA>
									</PANEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</PANEL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<AREA_SEC>
		COND='getBooleanParam("showSchema")'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<LABEL>
						FMT={
							text.style='cs5';
						}
						TEXT='Defined:'
					</LABEL>
					<DELIMITER>
						FMT={
							text.style='cs1';
						}
					</DELIMITER>
					<PANEL>
						FMT={
							ctrl.size.width='357';
							text.style='cs6';
						}
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<LABEL>
										TEXT='globally in'
									</LABEL>
									<DATA_CTRL>
										<DOC_HLINK>
											TARGET_KEYS={
												'getXMLDocument().id';
												'"detail"';
											}
										</DOC_HLINK>
										FORMULA='getXMLDocument().getAttrStringValue("xmlName")'
									</DATA_CTRL>
									<PANEL>
										COND='hyperTargetExists (Array (contextElement.id, "xml-source"))'
										FMT={
											ctrl.size.width='108.8';
										}
										<AREA>
											<CTRL_GROUP>
												<CTRLS>
													<DELIMITER>
														FMT={
															txtfl.delimiter.type='text';
															txtfl.delimiter.text=', ';
														}
													</DELIMITER>
													<LABEL>
														TEXT='see'
													</LABEL>
													<LABEL>
														<DOC_HLINK>
															TARGET_KEYS={
																'contextElement.id';
																'"xml-source"';
															}
														</DOC_HLINK>
														TEXT='XML source'
													</LABEL>
												</CTRLS>
											</CTRL_GROUP>
										</AREA>
									</PANEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</PANEL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<AREA_SEC>
		CONTEXT_ELEMENT_EXPR='findElementByKey ("redefined-component", contextElement.id)'
		MATCHING_ET='<ANY>'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<LABEL>
						FMT={
							text.style='cs5';
						}
						TEXT='Redefines:'
					</LABEL>
					<DELIMITER>
						FMT={
							text.style='cs1';
						}
					</DELIMITER>
					<PANEL>
						FMT={
							ctrl.size.width='414.8';
							ctrl.size.height='38.3';
							text.style='cs6';
						}
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
										FORMULA='QName (getStringParam("nsURI"), getAttrStringValue("name"))'
									</DATA_CTRL>
									<LABEL>
										TEXT='in'
									</LABEL>
									<DATA_CTRL>
										<DOC_HLINK>
											TARGET_KEYS={
												'getXMLDocument().id';
												'"detail"';
											}
										</DOC_HLINK>
										FORMULA='getXMLDocument().getAttrStringValue("xmlName")'
									</DATA_CTRL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</PANEL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<AREA_SEC>
		COND='checkElementsByKey ("redefining-components", contextElement.id)'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<LABEL>
						FMT={
							text.style='cs5';
						}
						TEXT='Redefined:'
					</LABEL>
					<DELIMITER>
						FMT={
							text.style='cs1';
						}
					</DELIMITER>
					<SS_CALL_CTRL>
						FMT={
							text.style='cs6';
						}
						SS_NAME='Redefining Components'
					</SS_CALL_CTRL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<AREA_SEC>
		COND='count = getIntParam("ownAttributeCount") + getIntParam("ownElementCount");\ncount > 0 ? { setVar ("count", count); true } : false'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<LABEL>
						FMT={
							text.style='cs5';
						}
						TEXT='Includes:'
					</LABEL>
					<DELIMITER>
						FMT={
							text.style='cs1';
						}
					</DELIMITER>
					<PANEL>
						FMT={
							ctrl.size.width='372';
							ctrl.size.height='98.3';
							text.style='cs6';
						}
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<LABEL>
										COND='getVar("count").toInt() == 1'
										TEXT='definition'
									</LABEL>
									<LABEL>
										COND='getVar("count").toInt() > 1'
										TEXT='definitions'
									</LABEL>
									<LABEL>
										TEXT='of'
									</LABEL>
									<PANEL>
										COND='getIntParam("ownAttributeCount") > 0'
										FMT={
											ctrl.size.width='243';
											ctrl.size.height='38.3';
										}
										<AREA>
											<CTRL_GROUP>
												<CTRLS>
													<DATA_CTRL>
														FORMULA='getIntParam("ownAttributeCount")'
													</DATA_CTRL>
													<LABEL>
														COND='getIntParam("ownAttributeCount") == 1'
														<DOC_HLINK>
															TARGET_KEYS={
																'contextElement.id';
																'"attribute-defs"';
															}
														</DOC_HLINK>
														TEXT='attribute'
													</LABEL>
													<LABEL>
														COND='getIntParam("ownAttributeCount") > 1'
														<DOC_HLINK>
															TARGET_KEYS={
																'contextElement.id';
																'"attribute-defs"';
															}
														</DOC_HLINK>
														TEXT='attributes'
													</LABEL>
													<DELIMITER>
														FMT={
															txtfl.delimiter.type='text';
															txtfl.delimiter.text=' and ';
														}
													</DELIMITER>
												</CTRLS>
											</CTRL_GROUP>
										</AREA>
									</PANEL>
									<PANEL>
										COND='getIntParam("ownElementCount") > 0'
										FMT={
											ctrl.size.width='222.8';
											ctrl.size.height='38.3';
										}
										<AREA>
											<CTRL_GROUP>
												<CTRLS>
													<DATA_CTRL>
														FORMULA='getIntParam("ownElementCount")'
													</DATA_CTRL>
													<LABEL>
														COND='getIntParam("ownElementCount") == 1'
														<DOC_HLINK>
															TARGET_KEYS={
																'contextElement.id';
																'"content-element-defs"';
															}
														</DOC_HLINK>
														TEXT='element'
													</LABEL>
													<LABEL>
														COND='getIntParam("ownElementCount") > 1'
														<DOC_HLINK>
															TARGET_KEYS={
																'contextElement.id';
																'"content-element-defs"';
															}
														</DOC_HLINK>
														TEXT='elements'
													</LABEL>
												</CTRLS>
											</CTRL_GROUP>
										</AREA>
									</PANEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</PANEL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<AREA_SEC>
		COND='getIntParam("usageCount") == 0'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<LABEL>
						FMT={
							text.style='cs5';
						}
						TEXT='Used:'
					</LABEL>
					<DELIMITER>
						FMT={
							text.style='cs1';
						}
					</DELIMITER>
					<LABEL>
						FMT={
							text.style='cs6';
						}
						TEXT='never'
					</LABEL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<AREA_SEC>
		COND='getIntParam("usageCount") > 0'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<LABEL>
						FMT={
							text.style='cs5';
						}
						TEXT='Used:'
					</LABEL>
					<DELIMITER>
						FMT={
							text.style='cs1';
						}
					</DELIMITER>
					<PANEL>
						FMT={
							ctrl.size.width='215.3';
							text.style='cs6';
						}
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<LABEL>
										TEXT='at'
									</LABEL>
									<DATA_CTRL>
										FORMULA='getIntParam("usageCount")'
									</DATA_CTRL>
									<LABEL>
										COND='getIntParam("usageCount") == 1'
										<DOC_HLINK>
											TARGET_KEYS={
												'contextElement.id';
												'"usage-locations"';
											}
										</DOC_HLINK>
										TEXT='location'
									</LABEL>
									<LABEL>
										COND='getIntParam("usageCount") > 1'
										<DOC_HLINK>
											TARGET_KEYS={
												'contextElement.id';
												'"usage-locations"';
											}
										</DOC_HLINK>
										TEXT='locations'
									</LABEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</PANEL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
</ROOT>
<STOCK_SECTIONS>
	<ELEMENT_ITER>
		FMT={
			sec.outputStyle='text-par';
			txtfl.delimiter.type='text';
			txtfl.delimiter.text=', ';
		}
		TARGET_ET='<ANY>'
		SCOPE='custom'
		ELEMENT_ENUM_EXPR='findElementsByKey ("redefining-components", contextElement.id)'
		SS_NAME='Redefining Components'
		<BODY>
			<AREA_SEC>
				<AREA>
					<CTRL_GROUP>
						FMT={
							txtfl.delimiter.type='space';
						}
						<CTRLS>
							<LABEL>
								TEXT='with'
							</LABEL>
							<DATA_CTRL>
								<DOC_HLINK>
									TARGET_KEYS={
										'contextElement.id';
										'"detail"';
									}
								</DOC_HLINK>
								FORMULA='QName (getStringParam("nsURI"), getAttrStringValue("name"))'
							</DATA_CTRL>
							<LABEL>
								TEXT='in'
							</LABEL>
							<DATA_CTRL>
								<DOC_HLINK>
									TARGET_KEYS={
										'getXMLDocument().id';
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
	</ELEMENT_ITER>
</STOCK_SECTIONS>
CHECKSUM='l47KaFbkSfid6Il17ZmzCA'
</DOCFLEX_TEMPLATE>