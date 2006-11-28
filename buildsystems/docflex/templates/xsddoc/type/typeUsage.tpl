<DOCFLEX_TEMPLATE VER='1.7'>
CREATED='2005-09-01 11:12:00'
LAST_UPDATE='2006-10-09 06:34:52'
DESIGNER_TOOL='DocFlex SDK 1.0'
TEMPLATE_TYPE='DocumentTemplate'
DSM_TYPE_ID='xsddoc'
ROOT_ETS={'xs:complexType';'xs:simpleType'}
<TEMPLATE_PARAMS>
	PARAM={
		param.name='qName';
		param.description='The type\'s <code>QName</code>; used as a key for <i>"type-usage"</i> map; should be passed by the calling template';
		param.type='object';
	}
	PARAM={
		param.name='page.refs';
		param.displayName='Generate page references';
		param.type='boolean';
		param.boolean.default='true';
	}
</TEMPLATE_PARAMS>
FMT={
	doc.lengthUnits='pt';
	doc.hlink.style.link='cs3';
}
<STYLES>
	CHAR_STYLE={
		style.name='Default Paragraph Font';
		style.id='cs1';
		style.default='true';
	}
	CHAR_STYLE={
		style.name='Derivation Type';
		style.id='cs2';
		text.font.name='Verdana';
		text.font.size='8';
		text.color.foreground='#FF9900';
	}
	PAR_STYLE={
		style.name='Detail Heading 2';
		style.id='s1';
		text.font.size='10';
		text.font.style.bold='true';
		par.bkgr.opaque='true';
		par.bkgr.color='#EEEEFF';
		par.border.style='solid';
		par.border.color='#666666';
		par.margin.top='12';
		par.margin.bottom='8';
		par.padding.left='2';
		par.padding.right='2';
		par.padding.top='2';
		par.padding.bottom='2';
		par.page.keepWithNext='true';
	}
	CHAR_STYLE={
		style.name='Hyperlink';
		style.id='cs3';
		text.decor.underline='true';
		text.color.foreground='#0000FF';
	}
	PAR_STYLE={
		style.name='List Heading 2';
		style.id='s2';
		text.font.name='Arial';
		text.font.size='9';
		text.font.style.bold='true';
		par.margin.bottom='8';
		par.page.keepWithNext='true';
	}
	CHAR_STYLE={
		style.name='Name Modifier';
		style.id='cs4';
		text.font.name='Verdana';
		text.font.size='7';
		text.color.foreground='#B2B2B2';
	}
	PAR_STYLE={
		style.name='Normal';
		style.id='s3';
		style.default='true';
	}
	CHAR_STYLE={
		style.name='Page Number Small';
		style.id='cs5';
		text.font.name='Courier New';
		text.font.size='8';
	}
	CHAR_STYLE={
		style.name='Property Text';
		style.id='cs6';
		text.font.name='Verdana';
		text.font.size='8';
		par.lineHeight='11';
	}
</STYLES>
<ROOT>
	<FOLDER>
		DESCR='All KNOWN USAGE LOCATIONS'
		FMT={
			sec.outputStyle='list';
			list.item.margin.top='10';
			list.item.margin.bottom='10';
		}
		<HTARGET>
			TARGET_KEYS={
				'contextElement.id';
				'"usage-locations"';
			}
		</HTARGET>
		INIT_EXPR='setVar (\n  "allLocations", \n  toVector (findElementsByKey ("type-usage", getParam("qName")))\n)'
		<BODY>
			<FOLDER>
				DESCR='Usage in derivations of other global types'
				COND='v = filterElements (\n  getVar ("allLocations").toEnum(), \n  BooleanQuery (\n    instanceOf ("xs:%extensionType | xs:%restrictionType | xs:list | xs:restriction | xs:union") \n    &&\n    findPredecessorByType(\n       "xs:%element;xs:%attribute;xs:simpleType;xs:complexType"\n    ).instanceOf ("xs:simpleType | xs:complexType")\n  )\n).toVector();\n\nv.size() > 0 ? { setVar ("locations", v); true; } : false'
				COLLAPSED
				<BODY>
					<SS_CALL>
						SS_NAME='Two Column List'
						PARAMS_EXPR='v = getVar("locations").toVector();\n\n// sort locations according to the containing type qualified name\n\nv.sortVector (\n  @el,\n  FlexQuery ({\n    el = el.toElement();\n    callStockSection(\n       el.findPredecessorByType ("xs:simpleType;xs:complexType"),\n       "QName"\n    )})\n);\n\nArray (v, "Usage in global type")'
					</SS_CALL>
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
										TEXT='In derivations of other global types'
									</LABEL>
									<DATA_CTRL>
										FORMULA='"(" + getVar ("locations").toVector().size() + ")"'
									</DATA_CTRL>
									<DELIMITER>
										FMT={
											txtfl.delimiter.type='none';
										}
									</DELIMITER>
									<LABEL>
										TEXT=':'
									</LABEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</AREA_SEC>
				</HEADER>
			</FOLDER>
			<FOLDER>
				DESCR='Usage as direct type of elements'
				COND='v = filterElements (\n  getVar ("allLocations").toEnum(), \n  "xs:%element"\n).toVector();\n\nv.size() > 0 ? { setVar ("locations", v); true; } : false'
				COLLAPSED
				<BODY>
					<SS_CALL>
						DESCR='in case there are local elements, print everything in 2-column list'
						COND='findElementByType (\n  getVar("locations").toVector(), \n  "xs:%localElement"\n) != null'
						SS_NAME='Two Column List'
						PARAMS_EXPR='v = getVar("locations").toVector();\n\n// sort locations according to the containing element representation\n\nv.sortVector (\n  @el,\n  FlexQuery (\n    callStockSection (el.toElement(), "Element Location")\n  )\n);\n\nArray (v, "Element Location 2")'
					</SS_CALL>
					<ELEMENT_ITER>
						DESCR='otherwise, print as comma-delimited list'
						COND='sectionBlock.execSecNone'
						FMT={
							sec.outputStyle='text-par';
							txtfl.delimiter.type='text';
							txtfl.delimiter.text=', ';
						}
						TARGET_ET='xs:%element'
						SCOPE='custom'
						ELEMENT_ENUM_EXPR='getVar("locations").toEnum()'
						SORTING='by-expr'
						SORTING_KEY={expr='callStockSection("QName")',ascending,case_sensitive}
						<BODY>
							<AREA_SEC>
								<AREA>
									<CTRL_GROUP>
										FMT={
											txtfl.delimiter.type='nbsp';
										}
										<CTRLS>
											<SS_CALL_CTRL>
												SS_NAME='QName'
											</SS_CALL_CTRL>
											<PANEL>
												COND='output.format.supportsPagination &&\ngetBooleanParam("page.refs") &&\nhyperTargetExists (Array (contextElement.id, "detail"))'
												FMT={
													ctrl.size.width='153.8';
													text.style='cs5';
													txtfl.delimiter.type='none';
												}
												<AREA>
													<CTRL_GROUP>
														<CTRLS>
															<LABEL>
																TEXT='['
															</LABEL>
															<DATA_CTRL>
																FMT={
																	ctrl.option.noHLinkFmt='true';
																	text.hlink.fmt='none';
																}
																<DOC_HLINK>
																	TARGET_KEYS={
																		'contextElement.id';
																		'"detail"';
																	}
																</DOC_HLINK>
																DOCFIELD='page-htarget'
															</DATA_CTRL>
															<LABEL>
																TEXT=']'
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
					</ELEMENT_ITER>
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
										TEXT='As direct type of elements'
									</LABEL>
									<DATA_CTRL>
										FORMULA='"(" + getVar ("locations").toVector().size() + ")"'
									</DATA_CTRL>
									<DELIMITER>
										FMT={
											txtfl.delimiter.type='none';
										}
									</DELIMITER>
									<LABEL>
										TEXT=':'
									</LABEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</AREA_SEC>
				</HEADER>
			</FOLDER>
			<FOLDER>
				DESCR='Usage in deriviations of embedded types of elements'
				COND='v = filterElements (\n  getVar ("allLocations").toEnum(), \n  BooleanQuery (\n    instanceOf ("xs:%extensionType | xs:%restrictionType | xs:list | xs:restriction | xs:union") \n    &&\n    findPredecessorByType("xs:%attribute;xs:%element").instanceOf ("xs:%element")\n  )\n).toVector();\n\nv.size() > 0 ? { setVar ("locations", v); true; } : false'
				COLLAPSED
				<BODY>
					<SS_CALL>
						SS_NAME='Two Column List'
						PARAMS_EXPR='v = getVar("locations").toVector();\n\n// sort locations according to the containing element representation\n\nv.sortVector (\n  @el,\n  FlexQuery (callStockSection (\n       findPredecessorByType (el.toElement(), "xs:%element"),\n      "Element Location"\n  ))\n);\n\nArray (v, "Usage in embedded type of element")'
					</SS_CALL>
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
										TEXT='In derivations of embedded types of elements'
									</LABEL>
									<DATA_CTRL>
										FORMULA='"(" + getVar ("locations").toVector().size() + ")"'
									</DATA_CTRL>
									<DELIMITER>
										FMT={
											txtfl.delimiter.type='none';
										}
									</DELIMITER>
									<LABEL>
										TEXT=':'
									</LABEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</AREA_SEC>
				</HEADER>
			</FOLDER>
			<FOLDER>
				DESCR='Usage as direct type of attributes'
				COND='v = filterElements (\n  getVar ("allLocations").toEnum(), \n  "xs:%attribute"\n).toVector();\n\nv.size() > 0 ? { setVar ("locations", v); true; } : false'
				FMT={
					sec.outputStyle='list-items';
				}
				COLLAPSED
				<BODY>
					<ELEMENT_ITER>
						DESCR='global attributes'
						TARGET_ET='xs:%topLevelAttribute'
						SCOPE='custom'
						ELEMENT_ENUM_EXPR='getVar("locations").toEnum()'
						SORTING='by-expr'
						SORTING_KEY={expr='callStockSection("Attribute Name")',ascending,case_sensitive}
						<BODY>
							<AREA_SEC>
								DESCR='case of global attribute'
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<SS_CALL_CTRL>
												SS_NAME='Attribute Location'
											</SS_CALL_CTRL>
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
												TEXT='As direct type of global attributes'
											</LABEL>
											<DATA_CTRL>
												FORMULA='"(" + iterator.numItems + ")"'
											</DATA_CTRL>
											<DELIMITER>
												FMT={
													txtfl.delimiter.type='none';
												}
											</DELIMITER>
											<LABEL>
												TEXT=':'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</AREA_SEC>
						</HEADER>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='in elements'
						TARGET_ET='xs:%attribute'
						SCOPE='custom'
						ELEMENT_ENUM_EXPR='getVar("locations").toEnum()'
						FILTER='findPredecessorByType("xs:%element") != null'
						SORTING='by-expr'
						SORTING_KEY={expr='callStockSection("Attribute Location")',ascending,case_sensitive}
						<BODY>
							<AREA_SEC>
								DESCR='case of global attribute'
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<SS_CALL_CTRL>
												SS_NAME='Attribute Location'
											</SS_CALL_CTRL>
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
												TEXT='As direct type of attributes within elements'
											</LABEL>
											<DATA_CTRL>
												FORMULA='"(" + iterator.numItems + ")"'
											</DATA_CTRL>
											<DELIMITER>
												FMT={
													txtfl.delimiter.type='none';
												}
											</DELIMITER>
											<LABEL>
												TEXT=':'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</AREA_SEC>
						</HEADER>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='in complexTypes'
						TARGET_ET='xs:%attribute'
						SCOPE='custom'
						ELEMENT_ENUM_EXPR='getVar("locations").toEnum()'
						FILTER='findPredecessorByType("xs:%element;xs:complexType").instanceOf ("xs:complexType")\n\n// this condition ensures the attribute has been declared directly in a global complexType,\n// but not within a local element belonging to a complexType'
						SORTING='by-expr'
						SORTING_KEY={expr='callStockSection("Attribute Location")',ascending,case_sensitive}
						<BODY>
							<AREA_SEC>
								DESCR='case of global attribute'
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<SS_CALL_CTRL>
												SS_NAME='Attribute Location'
											</SS_CALL_CTRL>
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
												TEXT='As direct type of attributes within complexTypes'
											</LABEL>
											<DATA_CTRL>
												FORMULA='"(" + iterator.numItems + ")"'
											</DATA_CTRL>
											<DELIMITER>
												FMT={
													txtfl.delimiter.type='none';
												}
											</DELIMITER>
											<LABEL>
												TEXT=':'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</AREA_SEC>
						</HEADER>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='in attributeGroups'
						TARGET_ET='xs:%attribute'
						SCOPE='custom'
						ELEMENT_ENUM_EXPR='getVar("locations").toEnum()'
						FILTER='findPredecessorByType("xs:attributeGroup") != null'
						SORTING='by-expr'
						SORTING_KEY={expr='callStockSection("Attribute Location")',ascending,case_sensitive}
						<BODY>
							<AREA_SEC>
								DESCR='case of global attribute'
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<SS_CALL_CTRL>
												SS_NAME='Attribute Location'
											</SS_CALL_CTRL>
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
												TEXT='As direct type of attributes within attributeGroups'
											</LABEL>
											<DATA_CTRL>
												FORMULA='"(" + iterator.numItems + ")"'
											</DATA_CTRL>
											<DELIMITER>
												FMT={
													txtfl.delimiter.type='none';
												}
											</DELIMITER>
											<LABEL>
												TEXT=':'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</AREA_SEC>
						</HEADER>
					</ELEMENT_ITER>
				</BODY>
			</FOLDER>
			<FOLDER>
				DESCR='Usage in derivations of embedded types of attributes'
				COND='v = filterElements (\n  getVar ("allLocations").toEnum(), \n  BooleanQuery (\n    findPredecessorByType("xs:%attribute") != null\n  )\n).toVector();\n\nv.size() > 0 ? { setVar ("locations", v); true; } : false'
				FMT={
					sec.outputStyle='list-items';
				}
				COLLAPSED
				<BODY>
					<ELEMENT_ITER>
						DESCR='global attributes'
						TARGET_ETS={'xs:%extensionType';'xs:%restrictionType';'xs:list';'xs:restriction';'xs:union'}
						SCOPE='custom'
						ELEMENT_ENUM_EXPR='getVar("locations").toEnum()'
						FILTER='findPredecessorByType ("xs:%topLevelAttribute") != null'
						SORTING='by-expr'
						SORTING_KEY={expr='findPredecessorByType ("xs:%attribute").callStockSection("Attribute Name")',ascending,case_sensitive}
						<BODY>
							<AREA_SEC>
								DESCR='case of global attribute'
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<SS_CALL_CTRL>
												SS_NAME='Attribute Location'
												PASSED_ELEMENT_EXPR='findPredecessorByType ("xs:%attribute")'
												PASSED_ELEMENT_MATCHING_ET='xs:%attribute'
											</SS_CALL_CTRL>
											<SS_CALL_CTRL>
												SS_NAME='Used As'
												PASSED_ELEMENT_EXPR='iterator.element'
												PASSED_ELEMENT_MATCHING_ETS={'xs:%extensionType';'xs:%restrictionType';'xs:list';'xs:restriction';'xs:union'}
											</SS_CALL_CTRL>
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
												TEXT='In derivations of embedded types of global attributes'
											</LABEL>
											<DATA_CTRL>
												FORMULA='"(" + iterator.numItems + ")"'
											</DATA_CTRL>
											<DELIMITER>
												FMT={
													txtfl.delimiter.type='none';
												}
											</DELIMITER>
											<LABEL>
												TEXT=':'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</AREA_SEC>
						</HEADER>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='in elements'
						TARGET_ETS={'xs:%extensionType';'xs:%restrictionType';'xs:list';'xs:restriction';'xs:union'}
						SCOPE='custom'
						ELEMENT_ENUM_EXPR='getVar("locations").toEnum()'
						FILTER='findPredecessorByType("xs:%element;xs:complexType").instanceOf ("xs:%element")\n'
						SORTING='by-expr'
						SORTING_KEY={expr='callStockSection("Attribute Location")',ascending,case_sensitive}
						<BODY>
							<AREA_SEC>
								DESCR='case of global attribute'
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<SS_CALL_CTRL>
												SS_NAME='Attribute Location'
												PASSED_ELEMENT_EXPR='findPredecessorByType ("xs:%attribute")'
												PASSED_ELEMENT_MATCHING_ET='xs:%attribute'
											</SS_CALL_CTRL>
											<SS_CALL_CTRL>
												SS_NAME='Used As'
												PASSED_ELEMENT_EXPR='iterator.element'
												PASSED_ELEMENT_MATCHING_ETS={'xs:%extensionType';'xs:%restrictionType';'xs:list';'xs:restriction';'xs:union'}
											</SS_CALL_CTRL>
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
												TEXT='In derivations of embedded types of attributes within elements'
											</LABEL>
											<DATA_CTRL>
												FORMULA='"(" + iterator.numItems + ")"'
											</DATA_CTRL>
											<DELIMITER>
												FMT={
													txtfl.delimiter.type='none';
												}
											</DELIMITER>
											<LABEL>
												TEXT=':'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</AREA_SEC>
						</HEADER>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='in complexTypes'
						TARGET_ETS={'xs:%extensionType';'xs:%restrictionType';'xs:list';'xs:restriction';'xs:union'}
						SCOPE='custom'
						ELEMENT_ENUM_EXPR='getVar("locations").toEnum()'
						FILTER='findPredecessorByType("xs:%element;xs:complexType").instanceOf ("xs:complexType")\n\n// this condition ensures the attribute has been declared directly in a global complexType,\n// but not within a local element belonging to a complexType'
						SORTING='by-expr'
						SORTING_KEY={expr='callStockSection("Attribute Location")',ascending,case_sensitive}
						<BODY>
							<AREA_SEC>
								DESCR='case of global attribute'
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<SS_CALL_CTRL>
												SS_NAME='Attribute Location'
												PASSED_ELEMENT_EXPR='findPredecessorByType ("xs:%attribute")'
												PASSED_ELEMENT_MATCHING_ET='xs:%attribute'
											</SS_CALL_CTRL>
											<SS_CALL_CTRL>
												SS_NAME='Used As'
												PASSED_ELEMENT_EXPR='iterator.element'
												PASSED_ELEMENT_MATCHING_ETS={'xs:%extensionType';'xs:%restrictionType';'xs:list';'xs:restriction';'xs:union'}
											</SS_CALL_CTRL>
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
												TEXT='In derivations of embedded types of attributes within complexTypes'
											</LABEL>
											<DATA_CTRL>
												FORMULA='"(" + iterator.numItems + ")"'
											</DATA_CTRL>
											<DELIMITER>
												FMT={
													txtfl.delimiter.type='none';
												}
											</DELIMITER>
											<LABEL>
												TEXT=':'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</AREA_SEC>
						</HEADER>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='in attributeGroups'
						TARGET_ETS={'xs:%extensionType';'xs:%restrictionType';'xs:list';'xs:restriction';'xs:union'}
						SCOPE='custom'
						ELEMENT_ENUM_EXPR='getVar("locations").toEnum()'
						FILTER='findPredecessorByType("xs:attributeGroup") != null'
						SORTING='by-expr'
						SORTING_KEY={expr='callStockSection("Attribute Location")',ascending,case_sensitive}
						<BODY>
							<AREA_SEC>
								DESCR='case of global attribute'
								<AREA>
									<CTRL_GROUP>
										<CTRLS>
											<SS_CALL_CTRL>
												SS_NAME='Attribute Location'
												PASSED_ELEMENT_EXPR='findPredecessorByType ("xs:%attribute")'
												PASSED_ELEMENT_MATCHING_ET='xs:%attribute'
											</SS_CALL_CTRL>
											<SS_CALL_CTRL>
												SS_NAME='Used As'
												PASSED_ELEMENT_EXPR='iterator.element'
												PASSED_ELEMENT_MATCHING_ETS={'xs:%extensionType';'xs:%restrictionType';'xs:list';'xs:restriction';'xs:union'}
											</SS_CALL_CTRL>
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
												TEXT='In derivations of embedded types of attributes within attributeGroups'
											</LABEL>
											<DATA_CTRL>
												FORMULA='"(" + iterator.numItems + ")"'
											</DATA_CTRL>
											<DELIMITER>
												FMT={
													txtfl.delimiter.type='none';
												}
											</DELIMITER>
											<LABEL>
												TEXT=':'
											</LABEL>
										</CTRLS>
									</CTRL_GROUP>
								</AREA>
							</AREA_SEC>
						</HEADER>
					</ELEMENT_ITER>
				</BODY>
			</FOLDER>
		</BODY>
		<HEADER>
			<AREA_SEC>
				FMT={
					par.style='s1';
				}
				<AREA>
					<CTRL_GROUP>
						FMT={
							trow.bkgr.color='#CCCCFF';
						}
						<CTRLS>
							<LABEL>
								TEXT='Known Usage Locations'
							</LABEL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</HEADER>
	</FOLDER>
</ROOT>
<STOCK_SECTIONS>
	<AREA_SEC>
		DESCR='case of global attribute'
		MATCHING_ET='xs:%attribute'
		FMT={
			sec.outputStyle='text-par';
			txtfl.delimiter.type='none';
			par.option.nowrap='true';
		}
		SS_NAME='Attribute Location'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<PANEL>
						DESCR='in case of local attribute'
						CONTEXT_ELEMENT_EXPR='findPredecessorByType("xs:%element;xs:complexType;xs:attributeGroup")'
						MATCHING_ETS={'xs:%element';'xs:attributeGroup';'xs:complexType'}
						FMT={
							ctrl.size.width='297';
						}
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<SS_CALL_CTRL>
										MATCHING_ET='xs:%element'
										SS_NAME='Element Location'
									</SS_CALL_CTRL>
									<SS_CALL_CTRL>
										MATCHING_ETS={'xs:attributeGroup';'xs:complexType'}
										SS_NAME='QName'
									</SS_CALL_CTRL>
									<LABEL>
										TEXT='/@'
									</LABEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</PANEL>
					<SS_CALL_CTRL>
						SS_NAME='Attribute Name'
					</SS_CALL_CTRL>
					<PANEL>
						COND='output.format.supportsPagination &&\ngetBooleanParam("page.refs") &&\nhyperTargetExists (Array (contextElement.id, "def"))'
						FMT={
							ctrl.size.width='193.5';
							txtfl.delimiter.type='none';
						}
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<DELIMITER>
										FMT={
											txtfl.delimiter.type='nbsp';
										}
									</DELIMITER>
									<LABEL>
										FMT={
											text.style='cs5';
										}
										TEXT='['
									</LABEL>
									<DATA_CTRL>
										FMT={
											ctrl.option.noHLinkFmt='true';
											text.style='cs5';
											text.hlink.fmt='none';
										}
										<DOC_HLINK>
											TARGET_KEYS={
												'contextElement.id';
												'"def"';
											}
										</DOC_HLINK>
										DOCFIELD='page-htarget'
									</DATA_CTRL>
									<LABEL>
										FMT={
											text.style='cs5';
										}
										TEXT=']'
									</LABEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</PANEL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<AREA_SEC>
		MATCHING_ET='xs:%attribute'
		FMT={
			par.option.nowrap='true';
		}
		SS_NAME='Attribute Name'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<DATA_CTRL>
						<DOC_HLINK>
							TARGET_KEYS={
								'contextElement.id';
								'"def"';
							}
						</DOC_HLINK>
						<DOC_HLINK>
							TARGET_KEYS={
								'contextElement.id';
								'"detail"';
							}
						</DOC_HLINK>
						FORMULA='name = getAttrStringValue("name");\n\nschema = getXMLDocument().findChild ("xs:schema");\nnsURI = schema.getAttrStringValue("targetNamespace");\n\nform = instanceOf("xs:%topLevelAttribute") ? "qualified" :\n{\n  ((form = getAttrStringValue("form")) != "") ? form :\n     schema.getAttrStringValue ("attributeFormDefault");\n};\n\n(form == "qualified") ?\n  QName (nsURI, name, Enum (rootElement, contextElement)) : name;\n'
					</DATA_CTRL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
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
						OUTPUT_TYPE='included'
						DSM_MODE='pass-current-model'
					</TEMPLATE_CALL_CTRL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<AREA_SEC>
		MATCHING_ET='xs:%element'
		FMT={
			sec.outputStyle='text-par';
			txtfl.delimiter.type='none';
		}
		SS_NAME='Element Location 2'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<SS_CALL_CTRL>
						SS_NAME='QName'
					</SS_CALL_CTRL>
					<TEMPLATE_CALL_CTRL>
						MATCHING_ET='xs:%localElement'
						TEMPLATE_FILE='../element/localElementExt.tpl'
						OUTPUT_TYPE='included'
						DSM_MODE='pass-current-model'
					</TEMPLATE_CALL_CTRL>
					<PANEL>
						COND='output.format.supportsPagination &&\n  hyperTargetExists (Array (contextElement.id, "detail"))'
						FMT={
							ctrl.size.width='193.5';
							txtfl.delimiter.type='none';
						}
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<DELIMITER>
										FMT={
											txtfl.delimiter.type='nbsp';
										}
									</DELIMITER>
									<LABEL>
										FMT={
											text.style='cs5';
										}
										TEXT='['
									</LABEL>
									<DATA_CTRL>
										FMT={
											ctrl.option.noHLinkFmt='true';
											text.style='cs5';
											text.hlink.fmt='none';
										}
										<DOC_HLINK>
											TARGET_KEYS={
												'contextElement.id';
												'"detail"';
											}
										</DOC_HLINK>
										DOCFIELD='page-htarget'
									</DATA_CTRL>
									<LABEL>
										FMT={
											text.style='cs5';
										}
										TEXT=']'
									</LABEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</PANEL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<ELEMENT_ITER>
		DESCR='params[0]: the vector of column items\nparams[1]: true if this is the last part of the whole list (to avoid printing comma after last item)\nparams[2]: the name of the stock-section to print each item'
		FMT={
			sec.outputStyle='pars';
			txtfl.delimiter.type='text';
			txtfl.delimiter.text=', ';
			par.option.nowrap='true';
			list.style.type='none';
		}
		TARGET_ET='<ANY>'
		SCOPE='custom'
		ELEMENT_ENUM_EXPR='toEnum (stockSection.params[0])'
		SS_NAME='List Column'
		<BODY>
			<AREA_SEC>
				<AREA>
					<CTRL_GROUP>
						FMT={
							txtfl.delimiter.type='none';
						}
						<CTRLS>
							<SS_CALL_CTRL>
								SS_NAME_EXPR='stockSection.params[2].toString()'
							</SS_CALL_CTRL>
							<LABEL>
								COND='! iterator.isLastItem ||\n! stockSection.params[1].toBoolean()'
								TEXT=','
							</LABEL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</BODY>
	</ELEMENT_ITER>
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
	<FOLDER>
		DESCR='params[0]: vector of elements to be printed in the list; \nparams[1]: the name of the stock-section to print each list item'
		SS_NAME='Two Column List'
		INIT_EXPR='// sort vector according to the list item string representations\n\nssName = stockSection.params[1].toString();\n\nsortVector (\n  toVector (stockSection.param), \n  @el,\n  FlexQuery (callStockSection (el.toElement(), ssName))\n)\n'
		<BODY>
			<AREA_SEC>
				FMT={
					sec.outputStyle='table';
					table.cellpadding.both='0';
					table.border.style='none';
				}
				<AREA>
					<CTRL_GROUP>
						FMT={
							trow.align.vert='Top';
						}
						<CTRLS>
							<SS_CALL_CTRL>
								FMT={
									ctrl.size.width='217.5';
									ctrl.size.height='17.3';
								}
								SS_NAME='List Column'
								PARAMS_EXPR='v = stockSection.param.toVector();\nN = v.size();\n\nArray (\n  v.subVector (0, (N + 1) / 2), \n  N == 1,\n  stockSection.params[1]\n)'
							</SS_CALL_CTRL>
							<SS_CALL_CTRL>
								FMT={
									ctrl.size.width='282';
									ctrl.size.height='17.3';
									tcell.padding.extra.left='12';
								}
								SS_NAME='List Column'
								PARAMS_EXPR='v = stockSection.param.toVector();\n\nArray (\n  v.subVector ((v.size() + 1) / 2), \n  true,\n  stockSection.params[1]\n)'
							</SS_CALL_CTRL>
						</CTRLS>
					</CTRL_GROUP>
				</AREA>
			</AREA_SEC>
		</BODY>
	</FOLDER>
	<AREA_SEC>
		DESCR='context element is a usage location within definition of embedded type of element'
		MATCHING_ETS={'xs:%extensionType';'xs:%restrictionType';'xs:list';'xs:restriction';'xs:union'}
		SS_NAME='Usage in embedded type of element'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<PANEL>
						CONTEXT_ELEMENT_EXPR='findPredecessorByType ("xs:%element")'
						MATCHING_ET='xs:%element'
						FMT={
							ctrl.size.width='329.3';
						}
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<SS_CALL_CTRL>
										SS_NAME='Element Location'
									</SS_CALL_CTRL>
									<PANEL>
										COND='output.format.supportsPagination &&\n  hyperTargetExists (Array (contextElement.id, "detail"))'
										FMT={
											ctrl.size.width='157.5';
											txtfl.delimiter.type='none';
										}
										<AREA>
											<CTRL_GROUP>
												<CTRLS>
													<LABEL>
														FMT={
															text.style='cs5';
														}
														TEXT='['
													</LABEL>
													<DATA_CTRL>
														FMT={
															ctrl.option.noHLinkFmt='true';
															text.style='cs5';
															text.hlink.fmt='none';
														}
														<DOC_HLINK>
															TARGET_KEYS={
																'contextElement.id';
																'"detail"';
															}
														</DOC_HLINK>
														DOCFIELD='page-htarget'
													</DATA_CTRL>
													<LABEL>
														FMT={
															text.style='cs5';
														}
														TEXT=']'
													</LABEL>
												</CTRLS>
											</CTRL_GROUP>
										</AREA>
									</PANEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</PANEL>
					<SS_CALL_CTRL>
						SS_NAME='Used As'
					</SS_CALL_CTRL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<AREA_SEC>
		DESCR='context element is a usage location within definition of a global type'
		MATCHING_ETS={'xs:%extensionType';'xs:%restrictionType';'xs:list';'xs:restriction';'xs:union'}
		SS_NAME='Usage in global type'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<PANEL>
						CONTEXT_ELEMENT_EXPR='findPredecessorByType ("xs:simpleType;xs:complexType")'
						MATCHING_ETS={'xs:complexType';'xs:simpleType'}
						FMT={
							ctrl.size.width='292.5';
						}
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<SS_CALL_CTRL>
										SS_NAME='QName'
									</SS_CALL_CTRL>
									<PANEL>
										COND='output.format.supportsPagination &&\ngetBooleanParam("page.refs") &&\nhyperTargetExists (Array (contextElement.id, "detail"))'
										FMT={
											ctrl.size.width='157.5';
											text.style='cs5';
											txtfl.delimiter.type='none';
										}
										<AREA>
											<CTRL_GROUP>
												<CTRLS>
													<LABEL>
														TEXT='['
													</LABEL>
													<DATA_CTRL>
														FMT={
															ctrl.option.noHLinkFmt='true';
															text.hlink.fmt='none';
														}
														<DOC_HLINK>
															TARGET_KEYS={
																'contextElement.id';
																'"detail"';
															}
														</DOC_HLINK>
														DOCFIELD='page-htarget'
													</DATA_CTRL>
													<LABEL>
														TEXT=']'
													</LABEL>
												</CTRLS>
											</CTRL_GROUP>
										</AREA>
									</PANEL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</PANEL>
					<SS_CALL_CTRL>
						SS_NAME='Used As'
					</SS_CALL_CTRL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
	<AREA_SEC>
		MATCHING_ETS={'xs:%extensionType';'xs:%restrictionType';'xs:list';'xs:restriction';'xs:union'}
		FMT={
			sec.outputStyle='text-par';
			txtfl.delimiter.type='none';
		}
		SS_NAME='Used As'
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<DELIMITER>
					</DELIMITER>
					<LABEL>
						MATCHING_ET='xs:%extensionType'
						FMT={
							text.style='cs2';
						}
						TEXT='(as extension base)'
					</LABEL>
					<LABEL>
						MATCHING_ETS={'xs:%restrictionType';'xs:restriction'}
						FMT={
							text.style='cs2';
						}
						TEXT='(as restriction base)'
					</LABEL>
					<LABEL>
						MATCHING_ET='xs:list'
						FMT={
							text.style='cs2';
						}
						TEXT='(as list item type)'
					</LABEL>
					<LABEL>
						MATCHING_ET='xs:union'
						FMT={
							text.style='cs2';
						}
						TEXT='(as union member)'
					</LABEL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
</STOCK_SECTIONS>
CHECKSUM='ZzM88sCA1?qWFt54azH0kw'
</DOCFLEX_TEMPLATE>