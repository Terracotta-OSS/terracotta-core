<DOCFLEX_TEMPLATE VER='1.7'>
CREATED='2005-10-08 05:00:00'
LAST_UPDATE='2006-10-09 06:34:49'
DESIGNER_TOOL='DocFlex SDK 1.0'
TEMPLATE_TYPE='DocumentTemplate'
DSM_TYPE_ID='xsddoc'
ROOT_ET='<ANY>'
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
		style.name='Normal Smallest';
		style.id='cs3';
		text.font.name='Arial';
		text.font.size='8';
	}
</STYLES>
<ROOT>
	<AREA_SEC>
		FMT={
			sec.outputStyle='table';
			text.style='cs3';
			text.option.renderNLs='false';
			table.sizing='Relative';
			table.cellpadding.both='4';
			table.bkgr.color='#E7EDF6';
			table.border.style='solid';
			table.border.color='#999999';
			table.page.keepTogether='true';
			table.option.borderStylesInHTML='true';
		}
		<AREA>
			<HR>
				FMT={
					rule.style='dashed';
					rule.color='#B2B2B2';
					par.margin.top='12';
					par.margin.bottom='6';
					par.page.keepWithNext='true';
				}
			</HR>
			<CTRL_GROUP>
				<CTRLS>
					<PANEL>
						FMT={
							ctrl.size.width='499.5';
							ctrl.size.height='201';
							txtfl.delimiter.type='none';
						}
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<LABEL>
										TEXT='XML Schema documentation generated with '
									</LABEL>
									<DATA_CTRL>
										<URL_HLINK>
											TARGET_FRAME_EXPR='"_blank"'
											TARGET_FRAME_ALWAYS
											URL_EXPR='"http://www.filigris.com/products/docflex_xml/'
										</URL_HLINK>
										FORMULA='output.generator.name'
									</DATA_CTRL>
									<LABEL>
										TEXT=' v'
									</LABEL>
									<DATA_CTRL>
										FORMULA='output.generator.version'
									</DATA_CTRL>
								</CTRLS>
							</CTRL_GROUP>
							<CTRL_GROUP>
								FMT={
									par.margin.top='8';
								}
								<CTRLS>
									<LABEL>
										<URL_HLINK>
											TARGET_FRAME_EXPR='"_blank"'
											TARGET_FRAME_ALWAYS
											URL_EXPR='"http://www.filigris.com/products/docflex_xml/'
										</URL_HLINK>
										TEXT='DocFlex/XML'
									</LABEL>
									<LABEL>
										TEXT=' is a powerful template-driven documentation and report generator from any data stored in XML files. '
									</LABEL>
									<LABEL>
										TEXT='Based on an innovative technology developed by '
									</LABEL>
									<LABEL>
										<URL_HLINK>
											TARGET_FRAME_EXPR='"_blank"'
											TARGET_FRAME_ALWAYS
											URL_EXPR='"http://www.filigris.com'
										</URL_HLINK>
										TEXT='FILIGRIS WORKS'
									</LABEL>
									<LABEL>
										TEXT=', this new tool offers virtuoso data querying and formatting capabilities not found in anything else!'
									</LABEL>
								</CTRLS>
							</CTRL_GROUP>
							<CTRL_GROUP>
								FMT={
									par.margin.top='8';
								}
								<CTRLS>
									<LABEL>
										TEXT='Need to convert your XML data into a clear nice-looking documentation or reports? '
									</LABEL>
									<LABEL>
										TEXT='Web-ready hypertext HTML or printable MS Word / OpenOffice.org friendly RTF? '
									</LABEL>
									<LABEL>
										<URL_HLINK>
											TARGET_FRAME_EXPR='"_blank"'
											TARGET_FRAME_ALWAYS
											URL_EXPR='"http://www.filigris.com/products/docflex_xml/'
										</URL_HLINK>
										TEXT='DocFlex/XML'
									</LABEL>
									<LABEL>
										TEXT=' may be a cheap, quick and effective solution exactly for this task!'
									</LABEL>
								</CTRLS>
							</CTRL_GROUP>
							<CTRL_GROUP>
								FMT={
									par.margin.top='8';
								}
								<CTRLS>
									<LABEL>
										TEXT='Have questions? Not sure how to use it? Just send us e-mail to '
									</LABEL>
									<LABEL>
										<URL_HLINK>
											URL_EXPR='"mailto:contact@filigris.com"'
										</URL_HLINK>
										TEXT='contact@filigris.com'
									</LABEL>
									<LABEL>
										TEXT=' and we are always happy to help you! '
									</LABEL>
									<LABEL>
										TEXT='See also our '
									</LABEL>
									<LABEL>
										<URL_HLINK>
											TARGET_FRAME_EXPR='"_blank"'
											TARGET_FRAME_ALWAYS
											URL_EXPR='"http://www.filigris.com/company/services.php"'
										</URL_HLINK>
										TEXT='services'
									</LABEL>
									<LABEL>
										TEXT=' at '
									</LABEL>
									<LABEL>
										<URL_HLINK>
											TARGET_FRAME_EXPR='"_blank"'
											TARGET_FRAME_ALWAYS
											URL_EXPR='"http://www.filigris.com"'
										</URL_HLINK>
										TEXT='www.filigris.com'
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
CHECKSUM='oY6zKzSUK2ybDIz3jOGsOA'
</DOCFLEX_TEMPLATE>