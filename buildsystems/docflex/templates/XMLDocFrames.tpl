<DOCFLEX_TEMPLATE VER='1.7'>
CREATED='2005-03-04 07:50:00'
LAST_UPDATE='2006-10-09 06:34:47'
DESIGNER_TOOL='DocFlex SDK 1.0'
EXECUTION_POLICY='free'
TEMPLATE_TYPE='FramesetTemplate'
DSM_TYPE_ID='xmldoc'
ROOT_ET='#DOCUMENTS'
DESCR='<b>XML File Documentor Template (framed version)</b> -- allows to compile any number of XML files of any possible types into a framed documentation with the fancy formatting and table of contents frame.'
TITLE_EXPR='getStringParam("docTitle")'
<TEMPLATE_PARAMS>
	PARAM={
		param.name='docTitle';
		param.displayName='Documentation Title';
		param.description='Specify the title for the documentation.';
		param.type='string';
	}
	PARAM={
		param.name='sorting';
		param.displayName='Sorting';
		param.description='Specify the order in which XML files are sorted in table of contents frame.';
		param.type='enum';
		param.enum.values='none\nby name';
		param.enum.default='by name';
	}
	PARAM={
		param.name='include';
		param.displayName='Include';
		param.type='grouping';
	}
	PARAM={
		param.name='include.nsb';
		param.displayName='Namespace Bindings';
		param.description='Include Namespace Bindings report';
		param.type='boolean';
		param.boolean.default='true';
	}
</TEMPLATE_PARAMS>
<FRAMESET>
	LAYOUT='columns'
	<FRAME>
		PERCENT_SIZE=20
		NAME='listFrame'
		SOURCE_EXPR='documentByTemplate("TOC")'
	</FRAME>
	<FRAME>
		PERCENT_SIZE=80
		NAME='detailFrame'
		SOURCE_EXPR='documentByTemplate("Document")'
	</FRAME>
</FRAMESET>
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
		DESCR='iterates by all XML documents'
		TARGET_ET='#DOCUMENT'
		SCOPE='simple-location-rules'
		RULES={
			{'*','child-or-self::#DOCUMENT'};
		}
		SORTING='by-expr'
		SORTING_KEY={expr='getStringParam("sorting") == "by name" ? getAttrStringValue("xmlName")  :  ""',ascending}
		<BODY>
			<TEMPLATE_CALL>
				DESCR='generates a single-file doc for each XML document'
				TEMPLATE_FILE='xmldoc/Document.tpl'
				OUTPUT_TYPE='document'
				OUTPUT_DIR_EXPR='output.filesDir'
				FILE_NAME_EXPR='getAttrStringValue("xmlName").replace(".", "_")'
				DSM_MODE='pass-current-model'
			</TEMPLATE_CALL>
		</BODY>
	</ELEMENT_ITER>
	<TEMPLATE_CALL>
		COND='getBooleanParam("include.nsb")'
		TEMPLATE_FILE='xmldoc/xmlns-bindings.tpl'
		OUTPUT_TYPE='document'
		OUTPUT_DIR_EXPR='output.filesDir'
		DSM_MODE='pass-current-model'
	</TEMPLATE_CALL>
	<TEMPLATE_CALL>
		DESCR='generates index of XML files'
		TEMPLATE_FILE='xmldoc/TOC.tpl'
		OUTPUT_TYPE='document'
		OUTPUT_DIR_EXPR='output.filesDir'
		DSM_MODE='pass-current-model'
	</TEMPLATE_CALL>
</ROOT>
CHECKSUM='80XNAOOU+PDMfiDMSJ?GnQ'
</DOCFLEX_TEMPLATE>