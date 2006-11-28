<DOCFLEX_TEMPLATE VER='1.7'>
CREATED='2005-04-26 03:31:00'
LAST_UPDATE='2006-10-09 06:34:49'
DESIGNER_TOOL='DocFlex SDK 1.0'
TEMPLATE_TYPE='DocumentTemplate'
DSM_TYPE_ID='xsddoc'
ROOT_ET='<ANY>'
<TEMPLATE_PARAMS>
	PARAM={
		param.name='key';
		param.description='The key by which the content elements are obtained from the "content-elements" element map';
		param.type='object';
		param.defaultExpr='contextElement.id';
	}
	PARAM={
		param.name='caption';
		param.type='string';
		param.string.default='Content Elements';
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
		style.name='List Heading 1';
		style.id='s1';
		text.font.name='Arial';
		text.font.size='10';
		text.font.style.bold='true';
		par.margin.top='12';
		par.margin.bottom='8';
		par.page.keepWithNext='true';
	}
	PAR_STYLE={
		style.name='Normal';
		style.id='s2';
		style.default='true';
	}
	CHAR_STYLE={
		style.name='Page Number Small';
		style.id='cs3';
		text.font.name='Courier New';
		text.font.size='8';
	}
</STYLES>
<ROOT>
	<FOLDER>
		COND='v = toVector (findElementsByKey ("content-elements", getParam("key")));\nv.size() > 0 ? \n{\n  v.sortVector (\n     @el,\n     FlexQuery (callStockSection (el.toElement(), "List Item")), \n     true\n  );\n\n  setVar ("contentElements", v); \n  true \n\n} : false'
		<BODY>
			<FOLDER>
				DESCR='if there are local elements, print everything as two column list, so the modifers will look more readable'
				COND='count (filterElements (\n  getVar ("contentElements").toEnum(),\n  BooleanQuery (\n    getAttrStringValue("ref") == ""\n  )\n)) != 0'
				FMT={
					sec.indent.block='true';
				}
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
											ctrl.size.width='209.3';
											ctrl.size.height='17.3';
										}
										SS_NAME='List Column'
										PARAMS_EXPR='v = getVar ("contentElements").toVector();\nN = v.size();\n\nArray (\n  v.subVector (0, (N + 1) / 2),\n  N == 1\n)'
									</SS_CALL_CTRL>
									<SS_CALL_CTRL>
										FMT={
											ctrl.size.width='272.3';
											ctrl.size.height='17.3';
											tcell.padding.extra.left='12';
										}
										SS_NAME='List Column'
										PARAMS_EXPR='v = getVar ("contentElements").toVector();\n\nArray (\n  v.subVector ((v.size() + 1) / 2),\n  true\n)'
									</SS_CALL_CTRL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</AREA_SEC>
				</BODY>
			</FOLDER>
			<ELEMENT_ITER>
				DESCR='otherwise, print the list as comma-delimited text flow'
				COND='sectionBlock.execSecNone'
				FMT={
					sec.outputStyle='text-par';
					sec.indent.block='true';
					txtfl.delimiter.type='text';
					txtfl.delimiter.text=', ';
				}
				TARGET_ET='xs:%element'
				SCOPE='custom'
				ELEMENT_ENUM_EXPR='getVar ("contentElements").toEnum()'
				SORTING='by-expr'
				SORTING_KEY={expr='callStockSection("QName")',ascending}
				<BODY>
					<AREA_SEC>
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<SS_CALL_CTRL>
										SS_NAME='List Item'
									</SS_CALL_CTRL>
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
					par.style='s1';
				}
				<AREA>
					<CTRL_GROUP>
						<CTRLS>
							<DATA_CTRL>
								FORMULA='getStringParam("caption")'
							</DATA_CTRL>
							<DATA_CTRL>
								FORMULA='"(" + getVar ("contentElements").toVector().size() + ")"'
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
</ROOT>
<STOCK_SECTIONS>
	<ELEMENT_ITER>
		DESCR='params[0]: vector of column elements; \nparams[1]: true if this is the last part of the whole list (to avoid printing comma after last item)'
		FMT={
			sec.outputStyle='pars';
			txtfl.delimiter.type='text';
			txtfl.delimiter.text=', ';
			par.option.nowrap='true';
			list.style.type='none';
		}
		TARGET_ET='xs:%element'
		SCOPE='custom'
		ELEMENT_ENUM_EXPR='toEnum (stockSection.param)'
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
								SS_NAME='List Item'
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
	<FOLDER>
		MATCHING_ET='xs:%element'
		FMT={
			sec.outputStyle='text-par';
		}
		SS_NAME='List Item'
		<BODY>
			<FOLDER>
				DESCR='case of a reference to a global element'
				COND='getAttrStringValue("ref") != ""'
				<BODY>
					<AREA_SEC>
						DESCR='when the reference is resolved into a documented element'
						CONTEXT_ELEMENT_EXPR='findElementByKey ("global-elements", getAttrQNameValue("ref"))'
						MATCHING_ET='xs:element'
						FMT={
							txtfl.delimiter.type='none';
						}
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<SS_CALL_CTRL>
										SS_NAME='QName'
									</SS_CALL_CTRL>
									<PANEL>
										COND='output.format.supportsPagination &&\ngetBooleanParam("page.refs") &&\nhyperTargetExists (Array (contextElement.id,  "detail"))'
										FMT={
											ctrl.size.width='192';
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
															text.style='cs3';
														}
														TEXT='['
													</LABEL>
													<DATA_CTRL>
														FMT={
															ctrl.option.noHLinkFmt='true';
															text.style='cs3';
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
															text.style='cs3';
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
						DESCR='otherwise, the referenced global element is not within documentation scope'
						COND='sectionBlock.execSecNone'
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<DATA_CTRL>
										ATTR='ref'
									</DATA_CTRL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</AREA_SEC>
				</BODY>
			</FOLDER>
			<AREA_SEC>
				DESCR='case of a local element'
				COND='getAttrStringValue("ref") == ""'
				MATCHING_ET='xs:%element'
				FMT={
					txtfl.delimiter.type='none';
				}
				<AREA>
					<CTRL_GROUP>
						<CTRLS>
							<SS_CALL_CTRL>
								SS_NAME='QName'
							</SS_CALL_CTRL>
							<TEMPLATE_CALL_CTRL>
								TEMPLATE_FILE='localElementExt.tpl'
								OUTPUT_TYPE='included'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL_CTRL>
							<PANEL>
								COND='output.format.supportsPagination &&\ngetBooleanParam("page.refs") &&\nhyperTargetExists (Array (contextElement.id,  "detail"))'
								FMT={
									ctrl.size.width='192';
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
													text.style='cs3';
												}
												TEXT='['
											</LABEL>
											<DATA_CTRL>
												FMT={
													ctrl.option.noHLinkFmt='true';
													text.style='cs3';
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
													text.style='cs3';
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
		</BODY>
	</FOLDER>
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
</STOCK_SECTIONS>
CHECKSUM='Rf95dJoD4t9QSFtzbmxudA'
</DOCFLEX_TEMPLATE>