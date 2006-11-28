<DOCFLEX_TEMPLATE VER='1.7'>
CREATED='2005-12-02 02:47:00'
LAST_UPDATE='2006-10-09 06:34:48'
DESIGNER_TOOL='DocFlex SDK 1.0'
TEMPLATE_TYPE='DocumentTemplate'
DSM_TYPE_ID='xsddoc'
ROOT_ET='#DOCUMENTS'
DESCR='This template generates a single file XML Schema documentation in any of the supported output formats. It may be particularly useful when the documentation is intended for printing.'
INIT_EXPR='callStockSection("Init")'
TITLE_EXPR='getStringParam("docTitle")'
<TEMPLATE_PARAMS>
	PARAM={
		param.name='docTitle';
		param.displayName='Documentation Title';
		param.description='Specifies the title to be placed at the top of the documentation overview.';
		param.type='string';
		param.trimSpaces='true';
		param.noEmptyString='true';
		param.string.default='XML Schema Documentation';
	}
	PARAM={
		param.name='include.detail';
		param.displayName='Include Details';
		param.description='This group of parameters controls the overall content of the generated\nXML schema documentation.\n<p>\n<b>Note:</b> The detailed content of each particular piece of the documentation can be specified in the <i>"Details"</i> parameter group.';
		param.type='grouping';
	}
	PARAM={
		param.name='include.detail.overview';
		param.displayName='Overview Summary';
		param.description='Specifies whether to generate the documentation <i>Overview Summary</i>.';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='include.detail.namespace';
		param.displayName='Namespace Overviews';
		param.description='Specifies if the <i>Namespace Overview</i> documentation should be generated for each namespace.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>Details | Namespace Overview</i>,<br>\n<i>Pagination | Start from new page | Namespace Overview</i>\n</blockquote>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='include.detail.schema';
		param.displayName='Schema Overviews';
		param.description='Specifies if the <i>Schema Overview</i> documentation should be generated for each XML schema.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>Details | Schema Overview,<br>\n<i>Pagination | Start from new page | Schema Overview</i>\n</blockquote>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='include.detail.element.global';
		param.displayName='Global Elements';
		param.description='Specifies if the detailed <i>Element Documentation</i> should be generated for each <i>global</i> element.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>Details | Element Documentation</i>,<br>\n<i>Pagination | Start from new page | Component Documentation</i>\n</blockquote>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='include.detail.element.local';
		param.displayName='Local Elements';
		param.description='Specifies if the detailed <i>Element Documentation</i> should be generated for each <i>local</i> element.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>Details | Element Documentation</i>,<br>\n<i>Pagination | Start from new page | Component Documentation</i>\n</blockquote>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='include.detail.complexType';
		param.displayName='Complex Types';
		param.description='Specifies if the detailed <i>Complex Type Documentation</i> should be generated for each global complex type.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>Details | Complex Type Documentation</i>,<br>\n<i>Pagination | Start from new page | Component Documentation</i>\n</blockquote>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='include.detail.simpleType';
		param.displayName='Simple Types';
		param.description='Specifies if the detailed <i>Simple Type Documentation</i> should be generated for each global simple type.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>Details | Simple Type Documentation</i>,<br>\n<i>Pagination | Start from new page | Component Documentation</i>\n</blockquote>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='include.detail.group';
		param.displayName='Element Groups';
		param.description='Specifies if the detailed <i>Element Group Documentation</i> should be generated for each global element group.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>Details | Element Group Documentation</i>,<br>\n<i>Pagination | Start from new page | Component Documentation</i>\n</blockquote>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='include.detail.attribute';
		param.displayName='Global Attributes';
		param.description='Specifies if the detailed <i>Global Attribute Documentation</i> should be generated for each global attribute.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>Details | Global Attribute Documentation</i>,<br>\n<i>Pagination | Start from new page | Component Documentation</i>\n</blockquote>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='include.detail.attributeGroup';
		param.displayName='Attribute Groups';
		param.description='Specify if the detailed <i>Attribute Group Documentation</i> should be generated for each global attribute group.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>Details | Attribute Group Documentation</i>,<br>\n<i>Pagination | Start from new page | Component Documentation</i>\n</blockquote>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc';
		param.displayName='Details';
		param.description='This group of parameters controls the content of the documentation main blocks.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>"Include Details"</i> parameter group\n</blockquote>';
		param.type='grouping';
	}
	PARAM={
		param.name='doc.namespace';
		param.displayName='Namespace Overview';
		param.description='This group of parameters controls the content of the  <i>Namespace Overview</i> documentation generated for each namespace.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>Include Details | Namespace Overviews</i>,<br>\n<i>Pagination | Start from new page | Namespace Overview</i>\n</blockquote>';
		param.type='grouping';
	}
	PARAM={
		param.name='doc.namespace.profile';
		param.displayName='Namespace Profile';
		param.description='Specifies whether to generate the <i>Namespace Profile</i> section, which contains a brief information about the namespace.';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.namespace.cs.element';
		param.displayName='Element Summary';
		param.description='Specifies whether and how to generate the summary of all <i>Element Components</i> that belong to the given namespace. There are following possibilities:\n<p>\n<i>"all"</i>\n<blockquote>\nBoth <i>global</i> and <i>local</i> element summary is generated as a single table sorted alphabetically.\n</blockquote>\n\n<i>"global only"</i>\n<blockquote>\nOnly summary of <i>global</i> elements is generated.\n</blockquote>\n\n<i>"local only"</i>\n<blockquote>\nOnly summary of <i>local</i> elements is generated.\n</blockquote>\n\n<i>"global & local separately"</i>\n<blockquote>\nThe summaries of both <i>global</i> and <i>local</i> elements are generated as two separate tables.\n</blockquote>\n\n<i>"none"</i>\n<blockquote>\nNo element summary is generated\n</blockquote>';
		param.type='enum';
		param.enum.values='all\nglobal\nlocal\nglobal_local\nnone';
		param.enum.displayValues='all\nglobal only\nlocal only\nglobal & local separately\nnone';
	}
	PARAM={
		param.name='doc.namespace.cs.complexType';
		param.displayName='Complex Type Summary';
		param.description='Specifies whether to generate the summary of all <i>Complex Type Components</i> that belong to the given namespace.';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.namespace.cs.simpleType';
		param.displayName='Simple Type Summary';
		param.description='Specifies whether to generate the summary of all <i>Simple Type Components</i> that belong to the given namespace.';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.namespace.cs.group';
		param.displayName='Element Group Summary';
		param.description='Specifies whether to generate the summary of all <i>Element Group Components</i> that belong to the given namespace.';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.namespace.cs.attribute';
		param.displayName='Global Attribute Summary';
		param.description='Specifies whether to generate the summary of all <i>Global Attribute Components</i> that belong to the given namespace.';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.namespace.cs.attributeGroup';
		param.displayName='Attribute Group Summary';
		param.description='Specifies whether to generate the summary of all <i>Attribute Group Components</i> that belong to the given namespace.';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.schema';
		param.displayName='Schema Overview';
		param.description='This group of parameters controls the detailed content of the <i>Schema Overview</i> documentation generated for each XML schema.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>Include Details | Schema Overviews</i>,<br>\n<i>Pagination | Start from new page | Schema Overview</i>\n</blockquote>';
		param.type='grouping';
	}
	PARAM={
		param.name='doc.schema.profile';
		param.displayName='Schema Profile';
		param.description='Specifies whether to generate the <i>Schema Profile</i> section, which contains a brief information about the schema.';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.schema.annotation';
		param.displayName='Annotation';
		param.description='Specifies whether to include the XML schema annotation.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>"Sections | Annotation"</i> parameter group, which controls the processing of annotations.\n</blockquote>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.schema.cs.element';
		param.displayName='Element Summary';
		param.description='Specifies whether and how to generate the summary of <i>Element Components</i> defined in the schema. There are following possibilities:\n<p>\n<i>"all"</i>\n<blockquote>\nBoth <i>global</i> and <i>local</i> element summary is generated as a single table sorted alphabetically.\n</blockquote>\n\n<i>"global only"</i>\n<blockquote>\nOnly summary of <i>global</i> elements is generated.\n</blockquote>\n\n<i>"local only"</i>\n<blockquote>\nOnly summary of <i>local</i> elements is generated.\n</blockquote>\n\n<i>"global & local separately"</i>\n<blockquote>\nThe summaries of both <i>global</i> and <i>local</i> elements are generated as two separate tables.\n</blockquote>\n\n<i>"none"</i>\n<blockquote>\nNo element summary is generated\n</blockquote>';
		param.type='enum';
		param.enum.values='all\nglobal\nlocal\nglobal_local\nnone';
		param.enum.displayValues='all\nglobal only\nlocal only\nglobal & local separately\nnone';
		param.enum.default='none';
	}
	PARAM={
		param.name='doc.schema.cs.complexType';
		param.displayName='Complex Type Summary';
		param.description='Specifies whether to generate the summary of <i>Complex Type Components</i> defined in the schema.';
		param.type='boolean';
	}
	PARAM={
		param.name='doc.schema.cs.simpleType';
		param.displayName='Simple Type Summary';
		param.description='Specifies whether to generate the summary of <i>Simple Type Components</i> defined in the schema.';
		param.type='boolean';
	}
	PARAM={
		param.name='doc.schema.cs.group';
		param.displayName='Element Group Summary';
		param.description='Specifies whether to generate the summary of <i>Element Group Components</i> defined in the schema.';
		param.type='boolean';
	}
	PARAM={
		param.name='doc.schema.cs.attribute';
		param.displayName='Global Attribute Summary';
		param.description='Specifies whether to generate the summary of <i>Global Attribute Components</i> defined in the schema.';
		param.type='boolean';
	}
	PARAM={
		param.name='doc.schema.cs.attributeGroup';
		param.displayName='Attribute Group Summary';
		param.description='Specifies whether to generate the summary of <i>Attribute Group Components</i> defined in the schema.';
		param.type='boolean';
	}
	PARAM={
		param.name='doc.schema.xmlSource';
		param.displayName='XML Source';
		param.description='Specifies if the XML source of the whole XML schema should be reproduced in the <i>Schema Overview</i> documentation.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>"Sections | XML Source"</i> parameter group that controls the appearance of the reproduced XML source.\n</blockquote>';
		param.type='boolean';
	}
	PARAM={
		param.name='doc.component';
		param.displayName='Component Documentation';
		param.description='This group of parameters allows to specify the default settings for all <i>Component Documentation</i> blocks at once:\n<ul>\n<li>Element Documentation</li>\n<li>Complex Type Documentation</li>\n<li>Simple Type Documentation</li>\n<li>Element Group Documentation</li>\n<li>Global Attribute Documentation</li>\n<li>Attribute Group Documentation</li>\n</ul>\n\n<b>Note:</b> Any settings specified in this parameter group can be overridden by the corresponding parameters of the particular component documentation.\n\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>"Pagination | Start from new page | Component Documentation"</i> parameter\n</blockquote>';
		param.type='grouping';
	}
	PARAM={
		param.name='doc.component.profile';
		param.displayName='Component Profile';
		param.description='Specifies whether to generate the <i>Component Profile</i> section, which contains a brief information about the component (such as to which namespace it belongs, where it is declared, type and content summary, etc.)';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.component.xmlRep';
		param.displayName='XML Representation Summary';
		param.description='Specifies whether to generate the <i>XML Representation Summary</i> section.';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.component.list.contentElements';
		param.displayName='List of Content Elements';
		param.description='Specifies whether to generate the <i>List of Content Elements</i>.\n<p>\nThis list, which in the Element Documentation also appears under the heading <i>"May contain elements"</i>, shows all elements declared in the element content model of a given component. These are the same elements as shown in the Complex Content Model of the component\'s XML Representation Summary. However, unlike the model representation, the elements in this list are ordered alphabetically, never repeat and hyperlinked directly to the corresponding Element Documentations.\n<p>\n<b>Applies To:</b>\n<ul>\n<li>Element Documentation</li>\n<li>Complex Type Documentation</li>\n<li>Element Group Documentation</li>\n</ul>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.component.list.containingElements';
		param.displayName='List of Containing Elements';
		param.description='Specifies whether to generate the <i>List of Containing Elements</i>.\n<p>\nThis list is generated only for Element Components and appears in the Element Documentation under the heading <i>"May be included in elements"</i>. It shows all elements that may contain the given element as a child (more precisely, the given element has been explicitly declared within the content models of the elements in the list).\n<p>\n<b>Applies To:</b>\n<ul>\n<li>Element Documentation</li>\n</ul>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.component.list.directSubtypes';
		param.displayName='List of Direct Subtypes';
		param.description='Specifies whether to generate the <i>List of Direct Subtypes</i>.\n<p>\nThis list is generated for each Simple/Complex Type Component. It shows all other type components that are directly derived from the given type component. \n<p>\n(A type is considered to be <i>directly derived</i> from the given type, when its definition contains an explicit reference to the given type).\n<p>\n<b>Applies To:</b>\n<ul>\n<li>Simple Type Documentation</li>\n<li>Complex Type Documentation</li>\n</ul>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.component.list.indirectSubtypes';
		param.displayName='List of Indirect Subtypes';
		param.description='Specifies whether to generate the <i>List of Indirect Subtypes</i>.\n<p>\nThis list is generated for each Simple/Complex Type Component. It shows all other type components that are indirectly derived from the given type component. \n<p>\n(A type is considered to be <i>indirectly derived</i> from the given type, when its definition contains no explicit references to the given type but a reference to a certain third type that is directly or indirectly derived from the given type).\n<p>\n<b>Applies To:</b>\n<ul>\n<li>Simple Type Documentation</li>\n<li>Complex Type Documentation</li>\n</ul>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.component.list.basedElements';
		param.displayName='List of All Based Elements';
		param.description='Specifies whether to generate the <i>List of All Based Elements</i>.\n<p>\nThis list is generated for each Simple/Complex Type Component. It shows all elements whose type is either the given type itself or directly/indirectly derived from the given type.\n<p>\n<b>Applies To:</b>\n<ul>\n<li>Simple Type Documentation</li>\n<li>Complex Type Documentation</li>\n</ul>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.component.list.basedAttributes';
		param.displayName='List of All Based Attributes';
		param.description='Specifies whether to generate the <i>List of All Based Attributes</i>.\n<p>\nThis list is generated for each Simple Type Component. It shows all attributes (defined both globally and locally) whose type is either the given type itself or directly/indirectly derived from the given type.\n<p>\n<b>Applies To:</b>\n<ul>\n<li>Simple Type Documentation</li>\n</ul>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.component.list.usage';
		param.displayName='Usage / Definition Locations';
		param.description='Specifies whether to generate the <i>Usage / Definition Locations</i> report.\n<p>\nThis section may be generated for each component. It shows where and how the given component is used within this and other XML Schemas included in the documentation. (For a local element, this report shows all definition locations associated with that element).';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.component.annotation';
		param.displayName='Annotation';
		param.description='Specifies whether to include the annotation of the component.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>"Sections | Annotation"</i> group of parameters, which control the processing of annotations.\n</blockquote>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.component.typeDef';
		param.displayName='Type Definition Detail';
		param.description='Specifies whether to generate the <i>Type Definition Detail</i> section of the component documentation.\n<p>\n<b>Applies To:</b>\n<ul>\n<li>Simple Type Documentation</li>\n<li>Complex Type Documentation</li>\n</ul>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.component.embeddedType';
		param.displayName='Embedded Type Detail';
		param.description='Specifies whether to generate the <i>Embedded Type Detail</i> section of the component documentation.\n<p>\nThe <i>embedded type</i> is one which is defined directly within the definition of the component to which that type is assigned (an element or attribute).\n<p>\n<b>Applies To:</b>\n<ul>\n<li>Element Documentation</li>\n<li>Global Attribute Documentation</li>\n</ul>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.component.xmlSource';
		param.displayName='XML Source';
		param.description='Specifies whether to reproduce within the component documentation the XML source defining the given component.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>"Sections | XML Source"</i> parameter group that controls the appearance of the reproduced XML source.\n</blockquote>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.component.attributes';
		param.displayName='Attribute Detail';
		param.description='Specifies whether to generate the details of the attributes declared for the given component.\n<p>\n<b>Applies To:</b>\n<ul>\n<li>Element Documentation</li>\n<li>Complex Type Documentation</li>\n<li>Attribute Group Documentation</li>\n</ul>\n\n<b>See Also:</b>\n<blockquote>\n<i>"Sections | Attribute Detail"</i> group of parameters, which control what is included in this section of the component documentation.\n</blockquote>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.component.contentElements';
		param.displayName='Content Element Detail';
		param.description='Specifies whether to generate the details of the elements declared in the element content model of the given component.\n<p>\n<b>Applies To:</b>\n<ul>\n<li>Element Documentation</li>\n<li>Complex Type Documentation</li>\n<li>Element Group Documentation</li>\n</ul>\n\n<b>See Also:</b>\n<blockquote>\n<i>"Sections | Content Element Detail"</i> group of parameters, which control what is included in this section of the component documentation.\n</blockquote>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='doc.element';
		param.displayName='Element Documentation';
		param.description='This group of parameters controls the detailed content of the <i>Element Documentation</i> generated for each <i>element</i> component.\n<p>\n<b>Note:</b> The default settings of these parameters are specified in the <i>"Component Documentation"</i> parameter group.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>"Include Details | Global Elements"</i> and <i>"Include Details | Local Elements"</i> parameters\n</blockquote>';
		param.type='grouping';
	}
	PARAM={
		param.name='doc.element.profile';
		param.displayName='Element Profile';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Component Profile"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.profile")';
	}
	PARAM={
		param.name='doc.element.xmlRep';
		param.displayName='XML Representation Summary';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | XML Representation Summary"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.xmlRep")';
	}
	PARAM={
		param.name='doc.element.list.contentElements';
		param.displayName='List of Content Elements';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | List of Content Elements"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.list.contentElements")';
	}
	PARAM={
		param.name='doc.element.list.containingElements';
		param.displayName='List of Containing Elements';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | List of Containing Elements"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.list.containingElements")';
	}
	PARAM={
		param.name='doc.element.list.usage';
		param.displayName='Usage / Definition Locations';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Usage / Definition Locations"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.list.usage")';
	}
	PARAM={
		param.name='doc.element.annotation';
		param.displayName='Annotation';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Annotation"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.annotation")';
	}
	PARAM={
		param.name='doc.element.embeddedType';
		param.displayName='Embedded Type Detail';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Embedded Type Detail"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.embeddedType")';
	}
	PARAM={
		param.name='doc.element.xmlSource';
		param.displayName='XML Source';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | XML Source"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.xmlSource")';
	}
	PARAM={
		param.name='doc.element.attributes';
		param.displayName='Attribute Detail';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Attribute Detail"</i> </blockquote>';
		param.type='boolean';
		param.defaultExpr='getBooleanParam("doc.component.attributes")';
	}
	PARAM={
		param.name='doc.element.contentElements';
		param.displayName='Content Element Detail';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Content Element Detail"</i> </blockquote>';
		param.type='boolean';
		param.defaultExpr='getBooleanParam("doc.component.contentElements")';
	}
	PARAM={
		param.name='doc.complexType';
		param.displayName='Complex Type Documentation';
		param.description='This group of parameters controls the detailed content of the <i>Complex Type Documentation</i> generated for each <i>complexType</i> component.\n<p>\n<b>Note:</b> The default settings of these parameters are specified in the <i>"Component Documentation"</i> parameter group.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>"Include Details | Complex Types"</i> parameter\n</blockquote>';
		param.type='grouping';
	}
	PARAM={
		param.name='doc.complexType.profile';
		param.displayName='Complex Type Profile';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Component Profile"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.profile")';
	}
	PARAM={
		param.name='doc.complexType.xmlRep';
		param.displayName='XML Representation Summary';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | XML Representation Summary"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.xmlRep")';
	}
	PARAM={
		param.name='doc.complexType.list.contentElements';
		param.displayName='List of Content Elements';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | List of Content Elements"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.list.contentElements")';
	}
	PARAM={
		param.name='doc.complexType.list.directSubtypes';
		param.displayName='List of Direct Subtypes';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | List of Direct Subtypes"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.list.directSubtypes")';
	}
	PARAM={
		param.name='doc.complexType.list.indirectSubtypes';
		param.displayName='List of Indirect Subtypes';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | List of Indirect Subtypes"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.list.indirectSubtypes")';
	}
	PARAM={
		param.name='doc.complexType.list.basedElements';
		param.displayName='List of All Based Elements';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | List of All Based Elements"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.list.basedElements")';
	}
	PARAM={
		param.name='doc.complexType.list.usage';
		param.displayName='Usage Locations';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Usage / Definition Locations"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.list.usage")';
	}
	PARAM={
		param.name='doc.complexType.annotation';
		param.displayName='Annotation';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Annotation"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.annotation")';
	}
	PARAM={
		param.name='doc.complexType.typeDef';
		param.displayName='Type Definition Detail';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Type Definition Detail"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.typeDef")';
	}
	PARAM={
		param.name='doc.complexType.xmlSource';
		param.displayName='XML Source';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | XML Source"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.xmlSource")';
	}
	PARAM={
		param.name='doc.complexType.attributes';
		param.displayName='Attribute Detail';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Attribute Detail"</i> </blockquote>';
		param.type='boolean';
		param.defaultExpr='getBooleanParam("doc.component.attributes")';
	}
	PARAM={
		param.name='doc.complexType.contentElements';
		param.displayName='Content Element Detail';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Content Element Detail"</i> </blockquote>';
		param.type='boolean';
		param.defaultExpr='getBooleanParam("doc.component.contentElements")';
	}
	PARAM={
		param.name='doc.simpleType';
		param.displayName='Simple Type Documentation';
		param.description='This group of parameters controls the detailed content of the <i>Simple Type Documentation</i> generated for each <i>simpleType</i> component.\n<p>\n<b>Note:</b> The default settings of these parameters are specified in the <i>"Component Documentation"</i> parameter group.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>"Include Details | Simple Types"</i> parameter\n</blockquote>';
		param.type='grouping';
	}
	PARAM={
		param.name='doc.simpleType.profile';
		param.displayName='Simple Type Profile';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Component Profile"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.profile")';
	}
	PARAM={
		param.name='doc.simpleType.xmlRep';
		param.displayName='XML Representation Summary';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | XML Representation Summary"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.xmlRep")';
	}
	PARAM={
		param.name='doc.simpleType.list.directSubtypes';
		param.displayName='List of Direct Subtypes';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | List of Direct Subtypes"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.list.directSubtypes")';
	}
	PARAM={
		param.name='doc.simpleType.list.indirectSubtypes';
		param.displayName='List of Indirect Subtypes';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | List of Indirect Subtypes"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.list.indirectSubtypes")';
	}
	PARAM={
		param.name='doc.simpleType.list.basedElements';
		param.displayName='List of All Based Elements';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | List of All Based Elements"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.list.basedElements")';
	}
	PARAM={
		param.name='doc.simpleType.list.basedAttributes';
		param.displayName='List of All Based Attributes';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | List of All Based Attributes"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.list.basedAttributes")';
	}
	PARAM={
		param.name='doc.simpleType.list.usage';
		param.displayName='Usage Locations';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Usage / Definition Locations"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.list.usage")';
	}
	PARAM={
		param.name='doc.simpleType.annotation';
		param.displayName='Annotation';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Annotation"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.annotation")';
	}
	PARAM={
		param.name='doc.simpleType.typeDef';
		param.displayName='Type Definition Detail';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Type Definition Detail"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.typeDef")';
	}
	PARAM={
		param.name='doc.simpleType.xmlSource';
		param.displayName='XML Source';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | XML Source"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.xmlSource")';
	}
	PARAM={
		param.name='doc.group';
		param.displayName='Element Group Documentation';
		param.description='This group of parameters controls the detailed content of the <i>Element Group Documentation</i> generated for each <i>group</i> component.\n<p>\n<b>Note:</b> The default settings of these parameters are specified in the <i>"Component Documentation"</i> parameter group.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>"Include Details | Element Groups"</i> parameter\n</blockquote>';
		param.type='grouping';
	}
	PARAM={
		param.name='doc.group.profile';
		param.displayName='Element Group Profile';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.profile")';
	}
	PARAM={
		param.name='doc.group.xmlRep';
		param.displayName='XML Representation Summary';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | XML Representation Summary"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.xmlRep")';
	}
	PARAM={
		param.name='doc.group.list.contentElements';
		param.displayName='List of Content Elements';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | List of Content Elements"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.list.contentElements")';
	}
	PARAM={
		param.name='doc.group.list.usage';
		param.displayName='Usage Locations';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Usage / Definition Locations"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.list.usage")';
	}
	PARAM={
		param.name='doc.group.annotation';
		param.displayName='Annotation';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Annotation"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.annotation")';
	}
	PARAM={
		param.name='doc.group.xmlSource';
		param.displayName='XML Source';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | XML Source"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.xmlSource")';
	}
	PARAM={
		param.name='doc.group.contentElements';
		param.displayName='Content Element Detail';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Content Element Detail"</i> </blockquote>';
		param.type='boolean';
		param.defaultExpr='getBooleanParam("doc.component.contentElements")';
	}
	PARAM={
		param.name='doc.attribute';
		param.displayName='Global Attribute Documentation';
		param.description='This group of parameters controls the detailed content of the <i>Global Attribute Documentation</i> generated for each <i>attribute</i> component.\n<p>\n<b>Note:</b> The default settings of these parameters are specified in the <i>"Component Documentation"</i> parameter group.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>"Include Details | Global Attributes"</i> parameter\n</blockquote>';
		param.type='grouping';
	}
	PARAM={
		param.name='doc.attribute.profile';
		param.displayName='Attribute Profile';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Component Profile"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.profile")';
	}
	PARAM={
		param.name='doc.attribute.xmlRep';
		param.displayName='XML Representation Summary';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | XML Representation Summary"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.xmlRep")';
	}
	PARAM={
		param.name='doc.attribute.list.usage';
		param.displayName='Usage Locations';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Usage / Definition Locations"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.list.usage")';
	}
	PARAM={
		param.name='doc.attribute.annotation';
		param.displayName='Annotation';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Annotation"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.annotation")';
	}
	PARAM={
		param.name='doc.attribute.embeddedType';
		param.displayName='Embedded Type Detail';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Embedded Type Detail"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.embeddedType")';
	}
	PARAM={
		param.name='doc.attribute.xmlSource';
		param.displayName='XML Source';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | XML Source"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.xmlSource")';
	}
	PARAM={
		param.name='doc.attributeGroup';
		param.displayName='Attribute Group Documentation';
		param.description='This group of parameters controls the detailed content of the <i>Attribute Group Documentation</i> generated for each <i>attributeGroup</i> component.\n<p>\n<b>Note:</b> The default settings of these parameters are specified in the <i>"Component Documentation"</i> parameter group.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>"Include Details | Attribute Groups"</i> parameter\n</blockquote>';
		param.type='grouping';
	}
	PARAM={
		param.name='doc.attributeGroup.profile';
		param.displayName='Attribute Group Profile';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Component Profile"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.profile")';
	}
	PARAM={
		param.name='doc.attributeGroup.xmlRep';
		param.displayName='XML Representation Summary';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | XML Representation Summary"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.xmlRep")';
	}
	PARAM={
		param.name='doc.attributeGroup.list.usage';
		param.displayName='Usage Locations';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Usage / Definition Locations"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.list.usage")';
	}
	PARAM={
		param.name='doc.attributeGroup.annotation';
		param.displayName='Annotation';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Annotation"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.annotation")';
	}
	PARAM={
		param.name='doc.attributeGroup.xmlSource';
		param.displayName='XML Source';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | XML Source"</i> </blockquote>';
		param.type='boolean';
		param.boolean.default='true';
		param.defaultExpr='getBooleanParam("doc.component.xmlSource")';
	}
	PARAM={
		param.name='doc.attributeGroup.attributes';
		param.displayName='Attribute Detail';
		param.description='<b>Overrides:</b>\n<blockquote>\n<i>"Details | Component Documentation | Attribute Detail"</i> </blockquote>';
		param.type='boolean';
		param.defaultExpr='getBooleanParam("doc.component.attributes")';
	}
	PARAM={
		param.name='sec';
		param.displayName='Sections';
		param.description='This group of parameters controls the content and appearance of certain sections included in the main documentation blocks.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>"Details"</i> parameter group\n</blockquote>';
		param.type='grouping';
	}
	PARAM={
		param.name='sec.annotation';
		param.displayName='Annotation';
		param.description='This group of parameters controls processing and formatting of annotations (the content of <b><code>&lt;xs:annotation&gt;</code></b> elements specified in schemas).\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>"Details | Schema Overview | Annotation"</i>,<br>\n<i>"Details | Component Documentation | Annotation"</i>\n</blockquote>';
		param.type='grouping';
	}
	PARAM={
		param.name='sec.annotation.proc.xhtml';
		param.displayName='Process XHTML tags';
		param.description='Specifies whether to process the XHTML tags embedded in the annotation text.\n<p>\nThe XHTML tags are considered any XML elements that belong to XHTML namespace associated with the following URI:\n<blockquote><code>\nhttp://www.w3.org/1999/xhtml\n</code></blockquote>\n(Specifically, this particular URI, which is used by the templates to identify the XHTML elements, is specified in <i>xmltypes.config</i> file, where you can change it when you need.)\n<p>\nWhen this parameter is <code><b>true</b></code> (checked), any XHTML elements will be converted to normal HTML tags (that is, the namespace prefix will be removed from each tag\'s name and everything else rewritten as it was). That will make the annotation text look as a fragment of normal HTML, which will be further inserted directly into the documentation output (in case of HTML) or rendered (in case of RTF). \n<p>\nTo have it work (for both HTML and RTF), when generating documentation, check also that <i>"Render embedded HTML"</i> option of the destination output format (specified in the generator dialog) is selected!\n<p>\nWhen this parameter is <code><b>false</b></code> (unchecked), the XHTML elements will not be specifically processed in any way. The element tags will be simply removed or printed as normal text, which is controlled by <i>"Show other tags"</i> parameter.';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='sec.annotation.show.otherTags';
		param.displayName='Show other tags';
		param.description='Controls the appearance of unprocessed XML tags in the documentations.\n<p>\nWhen this parameter is <code><b>true</b></code> (checked), all XML tags (contained in the annotation) that have not been specifically processed (see <i>"Process XHTML tags"</i> parameter) will be shown in the generated documentation.\n<p>\nIf the parameter is <code><b>false</b></code> (unchecked), any unprocessed tags will be removed.';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='sec.annotation.copy.images';
		param.displayName='Copy images';
		param.description='Specify copying images embedded in the annotation text.\n<p>\nThis parameter works in conjunction with <i>"Process XHTML"</i> parameter, when processing of embedded XHTML tags is enabled, and only when generating HTML documentation.\n<p>\nIf the parameter is <code><b>true</b></code> (checked), all images referred from <code>&lt;img&gt;</code> tags will be automatically copied to the associated files directory (e.g. "doc-files") of the schema documentation. The <code>src</code> attribute of each <code>&lt;img&gt;</code> tag will be altered to point to the new image location.\n<p>\nWhen the parameter is <code><b>false</b></code> (unchecked), no images are copied and the original images source URLs in <code>&lt;img&gt;</code> tags are preserved.';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='sec.xmlSource';
		param.displayName='XML Source';
		param.description='This group of parameters controls the appearance the reproduced XML source included in the documentation.';
		param.type='grouping';
	}
	PARAM={
		param.name='sec.xmlSource.box';
		param.displayName='Enclose in Box';
		param.type='grouping';
	}
	PARAM={
		param.name='sec.xmlSource.box.schema';
		param.displayName='Schemas';
		param.description='Specifies if the entire XML source of each <i>schema</i> should be enclosed in a box.\n<p>\n<b>See Also:</b>\n<blockquote>\nParameter: <i>"Details | Schema Overview | XML Source"</i>\n</blockquote>';
		param.type='boolean';
	}
	PARAM={
		param.name='sec.xmlSource.box.component';
		param.displayName='Components';
		param.description='Specifies if  the XML source of each XML Schema <i>component</i> should be enclosed in a box.\n<p>\n<b>See Also:</b>\n<blockquote>\nParameter: <i>"Details | Component Documentation | XML Source"</i>\n</blockquote>';
		param.type='boolean';
	}
	PARAM={
		param.name='sec.xmlSource.box.attributes';
		param.displayName='Attribute Detail';
		param.description='Specifies if the XML source of each <i>attribute definition</i> should be enclosed in a box.\n<p>\n<b>See Also:</b>\n<blockquote>\nParameter: <i>"Sections | Attribute Detail | XML Source"</i>\n</blockquote>';
		param.type='boolean';
	}
	PARAM={
		param.name='sec.xmlSource.box.contentElements';
		param.displayName='Content Element Detail';
		param.description='Specifies if the XML source of each <i>content element definition</i> should be enclosed in a box.\n<p>\n<b>See Also:</b>\n<blockquote>\nParameter: <i>"Sections | Content Element Detail | XML Source"</i>\n</blockquote>';
		param.type='boolean';
	}
	PARAM={
		param.name='sec.attributes';
		param.displayName='Attribute Detail';
		param.description='This group of parameters controls the generation of the <i>"Attribute Detail"</i> section that may be included in the <i>Component Documentation</i> to provide the details about the attributes associated with the given component.\n<p>\n<b>See Also:</b>\n<blockquote>\nParameter: <i>"Details | Component Documentation | Attribute Detail"</i>\n</blockquote>';
		param.type='grouping';
	}
	PARAM={
		param.name='sec.attributes.ownOnly';
		param.displayName='Component\'s Only';
		param.description='This parameter allows to avoid repeating the details of the same attributes in different locations of the entire XML schema documentation.\n<p>\nWhen this parameter is <code><b>true</b></code> (selected), only those attributes are documented in the <i>"Attribute Detail"</i> section of each <i>Component Documentation</i> that are defined within the definition of that component. Any other attributes, which the component inherits from other ones, are documented only together with those components where they are defined. Only hyperlinks to the corresponding attribute details will lead from the documentation of the given component (e.g. from its <i>XML Representation Summary</i>).\n<p>\nWhen this parameter is <code><b>false</b></code> (unselected), all attributes associated with the given component are documented together with it (both those defined within this component and those inherited from others). This will result in the repeating of the details of the same attributes defined in a certain component across the documentations of all its descendant components. However, this possibility may be useful when the generated documentation does not include all XML schema components, or it is intended for printing (RTF), in which case the ability of walking by links will be limited.';
		param.type='boolean';
	}
	PARAM={
		param.name='sec.attributes.xmlSource';
		param.displayName='XML Source';
		param.description='Specifies if the XML source defining each attribute should be reproduced within the attribute details.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>"Sections | XML Source"</i> parameter group that controls the appearance of the reproduced XML source.\n</blockquote>';
		param.type='boolean';
	}
	PARAM={
		param.name='sec.contentElements';
		param.displayName='Content Element Detail';
		param.description='This group of parameters controls the generation of <i>"Content Element Detail"</i> section, which may be included in the <i>Component Documentation</i> to provide the details about the elements declared in the complex content model of the given component.\n<p>\n<b>See Also:</b>\n<blockquote>\nParameter: <i>"Details | Component Documentation | Content Element Detail"</i>\n</blockquote>';
		param.type='grouping';
	}
	PARAM={
		param.name='sec.contentElements.ownOnly';
		param.displayName='Component\'s Only';
		param.description='This parameter allows to avoid repeating the details of the same content elements in different locations of the entire XML schema documentation.\n<p>\nWhen this parameter is <code><b>true</b></code> (selected), only those content elements are documented in the <i>"Content Element Detail"</i> section of each <i>Component Documentation</i> which are defined within the definition of that component. Any other content elements, which this component inherits from other ones, are documented only together with those components where they are defined. Only hyperlinks to the corresponding content element details will lead from the documentation of the given component (e.g. from its <i>XML Representation Summary</i>).\n<p>\nWhen this parameter is <code><b>false</b></code> (unselected), all content elements declated in the content model of the given component are documented together with it (both those defined within this component and those inherited from others). This will result in the repeating of the details of the same content elements defined in a certain component across the documentations of all its descendant components. However, that possibility may be useful when the generated documentation does not include all XML schema  components, or it is intended for printing (RTF), in which case the ability of walking by links will be limited.';
		param.type='boolean';
	}
	PARAM={
		param.name='sec.contentElements.xmlSource';
		param.displayName='XML Source';
		param.description='Specifies if the XML source defining each content element should be reproduced within the content element details.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>"Sections | XML Source"</i> parameter group that controls the appearance of the reproduced XML source.\n</blockquote>';
		param.type='boolean';
	}
	PARAM={
		param.name='fmt';
		param.displayName='Formatting';
		param.description='The group of parameters to control some formatting aspects of the generated documentation.';
		param.type='grouping';
	}
	PARAM={
		param.name='fmt.allowNestedTables';
		param.displayName='Allow nested tables';
		param.description='Specify if the generated output may contain nested tables.\n<p>\nXSDDoc templates frequently use tables as layouts in order to fit data better on a page. In certain cases, however, this leads to the situation when one table is nested into a cell of another table.\nFor some RTF reader, this may cause a problem.\n<p>\nAlthough RTF specification since v1.6 does support nested tables (as well as MS Word 2000 and the later), some modern and important RTF readers (such as OpenOffice.org 2.0) break on them.\n<p>\nTo work around this problem, all XSDDoc templates that use nested tables also contain alternative sections without them. Such sections will be activated instead of the primary ones, when this parameter is unchecked. Although, without nested tables some pieces of the documentation may look less compact and sleek, this still allows to generate a quite decently looking output.\n<p>\nPlease, uncheck this parameter when your destination RTF reader has problems with nested tables!';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='page';
		param.displayName='Pagination';
		param.description='The pagination related group of parameters';
		param.type='grouping';
	}
	PARAM={
		param.name='page.refs';
		param.displayName='Generate page references';
		param.description='Specify whether to generate the cross-reference page numbers,\nwhich look as the following:\n<blockquote>\n<code>xs:explicitGroup <b>[125]</b></code>\n</blockquote>\nSuch page numbers are substitution for hyperlinks in the printed documentation and help to navigate it.';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='page.start';
		param.displayName='Start from new page';
		param.description='This group of parameters specifies which documentation blocks should start from a new page.';
		param.type='grouping';
	}
	PARAM={
		param.name='page.start.namespace';
		param.displayName='Namespace Overview';
		param.description='Specify whether each <i>Namespace Overview</i> documentation should start from a new page.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>Details | Namespace Overview</i>\n</blockquote>';
		param.type='boolean';
		param.boolean.default='true';
	}
	PARAM={
		param.name='page.start.schema';
		param.displayName='Schema Overview';
		param.description='Specify whether each <i>Schema Overview</i> documentation should start from a new page.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>Details | Schema Overview</i>\n</blockquote>';
		param.type='boolean';
	}
	PARAM={
		param.name='page.start.component';
		param.displayName='Component Documentation';
		param.description='Specify if each <i>Component Documentation</i> should start from a new page.\n<p>\n<b>See Also:</b>\n<blockquote>\n<i>Details | Component Documentation</i>\n</blockquote>';
		param.type='boolean';
	}
</TEMPLATE_PARAMS>
FMT={
	doc.lengthUnits='pt';
	doc.hlink.style.link='cs11';
}
<STYLES>
	CHAR_STYLE={
		style.name='Annotation';
		style.id='cs1';
		text.font.name='Arial';
		text.font.size='9';
	}
	CHAR_STYLE={
		style.name='Attribute Name';
		style.id='cs2';
		text.font.name='Courier New';
		text.font.size='8';
		text.color.foreground='#990000';
	}
	CHAR_STYLE={
		style.name='Code';
		style.id='cs3';
		text.font.name='Courier New';
		text.font.size='9';
	}
	CHAR_STYLE={
		style.name='Code Smaller';
		style.id='cs4';
		text.font.name='Courier New';
		text.font.size='8';
	}
	CHAR_STYLE={
		style.name='Code Smallest';
		style.id='cs5';
		text.font.name='Courier New';
		text.font.size='7.5';
	}
	CHAR_STYLE={
		style.name='Comment';
		style.id='cs6';
		text.font.name='Courier New';
		text.color.foreground='#4D4D4D';
	}
	CHAR_STYLE={
		style.name='Content Model';
		style.id='cs7';
		text.font.name='Verdana';
		text.font.size='8.5';
	}
	CHAR_STYLE={
		style.name='Default Paragraph Font';
		style.id='cs8';
		style.default='true';
	}
	CHAR_STYLE={
		style.name='Derivation Type';
		style.id='cs9';
		text.font.name='Verdana';
		text.font.size='8';
		text.color.foreground='#FF9900';
	}
	CHAR_STYLE={
		style.name='Derivation Type Small';
		style.id='cs10';
		text.font.name='Verdana';
		text.font.size='7';
		text.color.foreground='#F59200';
	}
	PAR_STYLE={
		style.name='Detail Heading 1';
		style.id='s1';
		text.font.size='12';
		text.font.style.bold='true';
		par.bkgr.opaque='true';
		par.bkgr.color='#CCCCFF';
		par.border.style='solid';
		par.border.color='#666666';
		par.margin.top='14';
		par.margin.bottom='10';
		par.padding.left='2.5';
		par.padding.right='2.5';
		par.padding.top='2';
		par.padding.bottom='2';
		par.page.keepWithNext='true';
	}
	PAR_STYLE={
		style.name='Detail Heading 2';
		style.id='s2';
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
	PAR_STYLE={
		style.name='Detail Heading 3';
		style.id='s3';
		text.font.size='9';
		text.font.style.bold='true';
		text.font.style.italic='true';
		text.color.background='#CCCCFF';
		text.color.opaque='true';
		par.margin.top='10';
		par.margin.bottom='6';
		par.page.keepWithNext='true';
	}
	CHAR_STYLE={
		style.name='Hyperlink';
		style.id='cs11';
		text.decor.underline='true';
		text.color.foreground='#0000FF';
	}
	PAR_STYLE={
		style.name='Inset Heading';
		style.id='s4';
		text.font.style.bold='true';
		text.color.foreground='#990000';
		par.margin.bottom='6.8';
	}
	PAR_STYLE={
		style.name='Inset Heading 2';
		style.id='s5';
		text.font.size='9';
		text.font.style.bold='true';
		text.color.foreground='#990000';
		par.margin.bottom='6.8';
	}
	CHAR_STYLE={
		style.name='Internal Subset';
		style.id='cs12';
		text.font.name='Courier New';
		text.color.foreground='#0000FF';
	}
	PAR_STYLE={
		style.name='List Heading 1';
		style.id='s6';
		text.font.name='Arial';
		text.font.size='10';
		text.font.style.bold='true';
		par.margin.top='12';
		par.margin.bottom='8';
		par.page.keepWithNext='true';
	}
	PAR_STYLE={
		style.name='List Heading 2';
		style.id='s7';
		text.font.name='Arial';
		text.font.size='9';
		text.font.style.bold='true';
		par.margin.bottom='8';
		par.page.keepWithNext='true';
	}
	PAR_STYLE={
		style.name='Main Heading';
		style.id='s8';
		text.font.name='Verdana';
		text.font.size='13';
		text.font.style.bold='true';
		text.color.foreground='#4477AA';
		par.bkgr.opaque='true';
		par.bkgr.color='#EEEEEE';
		par.border.style='solid';
		par.border.color='#4477AA';
		par.margin.top='0';
		par.margin.bottom='8';
		par.padding.left='5';
		par.padding.right='5';
		par.padding.top='3';
		par.padding.bottom='3';
		par.page.keepWithNext='true';
	}
	CHAR_STYLE={
		style.name='Name Highlight';
		style.id='cs13';
		text.color.foreground='#FF0000';
	}
	CHAR_STYLE={
		style.name='Name Modifier';
		style.id='cs14';
		text.font.name='Verdana';
		text.font.size='7';
		text.color.foreground='#777777';
	}
	PAR_STYLE={
		style.name='Normal';
		style.id='s9';
		style.default='true';
	}
	CHAR_STYLE={
		style.name='Normal Smaller';
		style.id='cs15';
		text.font.name='Arial';
		text.font.size='9';
	}
	CHAR_STYLE={
		style.name='Normal Smallest';
		style.id='cs16';
		text.font.name='Arial';
		text.font.size='8';
	}
	PAR_STYLE={
		style.name='Overview Heading';
		style.id='s10';
		text.font.name='Verdana';
		text.font.size='13';
		text.font.style.bold='true';
		text.color.foreground='#4477AA';
		par.bkgr.opaque='true';
		par.bkgr.color='#EEEEEE';
		par.border.style='solid';
		par.border.color='#4477AA';
		par.margin.top='0';
		par.margin.bottom='1.7';
		par.padding.left='4';
		par.padding.right='4';
		par.padding.top='3';
		par.padding.bottom='3';
	}
	CHAR_STYLE={
		style.name='Page Footer Font';
		style.id='cs17';
		text.font.name='Arial';
		text.font.style.italic='true';
	}
	CHAR_STYLE={
		style.name='Page Header Font';
		style.id='cs18';
		text.font.name='Arial';
		text.font.style.italic='true';
	}
	CHAR_STYLE={
		style.name='Page Number';
		style.id='cs19';
		text.font.size='9';
		text.font.style.italic='true';
	}
	CHAR_STYLE={
		style.name='Page Number Small';
		style.id='cs20';
		text.font.name='Courier New';
		text.font.size='8';
	}
	PAR_STYLE={
		style.name='Profile Subheading';
		style.id='s11';
		text.font.name='Arial';
		text.font.size='9';
		text.font.style.bold='true';
		par.margin.top='6';
	}
	PAR_STYLE={
		style.name='Properties Heading';
		style.id='s12';
		text.font.name='Arial';
		text.font.size='8';
		text.font.style.bold='true';
		text.font.style.italic='true';
		text.color.background='#CCCCFF';
		text.color.opaque='true';
		par.margin.top='6';
		par.margin.bottom='6';
		par.page.keepWithNext='true';
	}
	CHAR_STYLE={
		style.name='Property Text';
		style.id='cs21';
		text.font.name='Verdana';
		text.font.size='8';
		par.lineHeight='11';
	}
	PAR_STYLE={
		style.name='Property Title';
		style.id='s13';
		text.font.name='Arial';
		text.font.size='8';
		text.font.style.bold='true';
		par.lineHeight='11';
		par.margin.right='7';
	}
	CHAR_STYLE={
		style.name='Property Title Font';
		style.id='cs22';
		text.font.size='8';
		text.font.style.bold='true';
		par.lineHeight='11';
		par.margin.right='7';
	}
	PAR_STYLE={
		style.name='Property Value';
		style.id='s14';
		text.font.name='Verdana';
		text.font.size='8';
		par.lineHeight='11';
	}
	CHAR_STYLE={
		style.name='Property Value Font';
		style.id='cs23';
		text.font.name='Verdana';
		text.font.size='8';
		par.lineHeight='11';
	}
	CHAR_STYLE={
		style.name='Summary Heading Font';
		style.id='cs24';
		text.font.size='12';
		text.font.style.bold='true';
	}
	CHAR_STYLE={
		style.name='Underline';
		style.id='cs25';
		text.decor.underline='true';
	}
	CHAR_STYLE={
		style.name='Value';
		style.id='cs26';
		text.font.size='7';
		text.font.style.bold='true';
		text.color.foreground='#000000';
	}
	CHAR_STYLE={
		style.name='Value Highlight';
		style.id='cs27';
		text.font.size='7';
		text.font.style.bold='true';
		text.color.foreground='#FF0000';
	}
	CHAR_STYLE={
		style.name='XML Markup';
		style.id='cs28';
		text.color.foreground='#0000FF';
		text.option.nbsps='true';
	}
	CHAR_STYLE={
		style.name='XML Name';
		style.id='cs29';
		text.color.foreground='#990000';
	}
	CHAR_STYLE={
		style.name='XML Source';
		style.id='cs30';
		text.font.name='Verdana';
		text.font.size='8';
	}
</STYLES>
<PAGE_FOOTER>
	FMT={
		text.style='cs17';
	}
	<AREA_SEC>
		FMT={
			sec.outputStyle='table';
			table.sizing='Relative';
			table.cellpadding.horz='1';
			table.border.style='none';
			table.border.top.style='solid';
		}
		<AREA>
			<CTRL_GROUP>
				<CTRLS>
					<DATA_CTRL>
						FMT={
							ctrl.size.width='293.3';
							ctrl.size.height='39.8';
						}
						FORMULA='getStringParam("docTitle")'
					</DATA_CTRL>
					<PANEL>
						FMT={
							content.outputStyle='text-par';
							ctrl.size.width='206.3';
							ctrl.size.height='39.8';
							tcell.align.horz='Right';
							tcell.option.nowrap='true';
							table.border.style='none';
						}
						<AREA>
							<CTRL_GROUP>
								<CTRLS>
									<LABEL>
										TEXT='Page'
									</LABEL>
									<DATA_CTRL>
										DOCFIELD='page'
									</DATA_CTRL>
									<LABEL>
										TEXT='of'
									</LABEL>
									<DATA_CTRL>
										DOCFIELD='num-pages'
									</DATA_CTRL>
								</CTRLS>
							</CTRL_GROUP>
						</AREA>
					</PANEL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
</PAGE_FOOTER>
<ROOT>
	<TEMPLATE_CALL>
		COND='getBooleanParam("include.detail.overview")'
		TEMPLATE_FILE='xsddoc/overview-summary.tpl'
		OUTPUT_TYPE='included'
		OUTPUT_DIR_EXPR='output.filesDir'
		DSM_MODE='pass-current-model'
	</TEMPLATE_CALL>
	<ELEMENT_ITER>
		DESCR='iterate by all namespaces (see sorting);\nnamespace URI is saved in \'nsURI\' variable'
		TARGET_ET='xs:schema'
		SCOPE='advanced-location-rules'
		RULES={
			{'*','#DOCUMENT/xs:schema'};
		}
		SORTING='by-attr'
		SORTING_KEY={lpath='@targetNamespace',ascending,unique}
		STEP_EXPR='setVar("nsURI", getAttrStringValue("targetNamespace"))'
		<BODY>
			<FOLDER>
				DESCR='NAMESPACE SUMMARY'
				COND='getBooleanParam("include.detail.namespace")'
				FMT={
					sec.spacing.before='20';
				}
				<BODY>
					<TEMPLATE_CALL>
						DESCR='(start from a new page)'
						COND='getBooleanParam("page.start.namespace")'
						FMT={
							sec.page.breakBefore='true';
						}
						TEMPLATE_FILE='xsddoc/namespace/namespace-summary.tpl'
						PASSED_PARAMS={
							'nsURI','getVar("nsURI")';
						}
						OUTPUT_TYPE='included'
						OUTPUT_DIR_EXPR='getVar("nsFolder").toString()'
						DSM_MODE='pass-current-model'
						PASSED_ROOT_ELEMENT_EXPR='rootElement'
					</TEMPLATE_CALL>
					<TEMPLATE_CALL>
						DESCR='(continue on the same page)'
						COND='! getBooleanParam("page.start.namespace")'
						FMT={
							sec.page.breakBefore='true';
						}
						TEMPLATE_FILE='xsddoc/namespace/namespace-summary.tpl'
						PASSED_PARAMS={
							'nsURI','getVar("nsURI")';
						}
						OUTPUT_TYPE='included'
						OUTPUT_DIR_EXPR='getVar("nsFolder").toString()'
						DSM_MODE='pass-current-model'
						PASSED_ROOT_ELEMENT_EXPR='rootElement'
					</TEMPLATE_CALL>
				</BODY>
			</FOLDER>
			<FOLDER>
				DESCR='SCHEMAS & COMPONENTS WITHIN THIS NAMESPACE; switch context element to the template\'s root element'
				CONTEXT_ELEMENT_EXPR='rootElement'
				MATCHING_ET='#DOCUMENTS'
				<BODY>
					<ELEMENT_ITER>
						DESCR='schema summaries'
						COND='getBooleanParam("include.detail.schema")'
						TARGET_ET='xs:schema'
						SCOPE='advanced-location-rules'
						RULES={
							{'*','#DOCUMENT/xs:schema'};
						}
						FILTER='getAttrStringValue("targetNamespace") == getVar("nsURI")'
						SORTING='by-expr'
						SORTING_KEY={expr='getXMLDocument().getAttrStringValue("xmlName")',ascending,case_sensitive}
						COLLAPSED
						<BODY>
							<TEMPLATE_CALL>
								DESCR='(start from a new page)'
								COND='getBooleanParam("page.start.schema")'
								FMT={
									sec.spacing.before='20';
									sec.page.breakBefore='true';
								}
								TEMPLATE_FILE='xsddoc/schema/schema-summary.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
							<TEMPLATE_CALL>
								DESCR='(continue on the same page)'
								COND='! getBooleanParam("page.start.schema")'
								FMT={
									sec.spacing.before='20';
								}
								TEMPLATE_FILE='xsddoc/schema/schema-summary.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
						</BODY>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='all elements'
						COND='getBooleanParam("include.detail.element.global") &&\ngetBooleanParam("include.detail.element.local")'
						FMT={
							table.sizing='Relative';
							table.cellpadding.both='3';
						}
						TARGET_ET='xs:%element'
						SCOPE='advanced-location-rules'
						RULES={
							{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getVar("nsURI")]/descendant::xs:%element'};
						}
						FILTER='getAttrStringValue("ref") == ""\n'
						SORTING='by-compound-key'
						SORTING_KEY={
							{expr='instanceOf("xs:element") ? \n QName (getVar("nsURI").toString(), getAttrStringValue("name")\n ) : callStockSection("Local Element Name")',ascending,case_sensitive};
							{expr='getAttrValue("type") == "" ? contextElement.id\n\n/* if the element has an embedded type, there must be a separate \n doc for it. Since the first subkey may be repeating, this one ensures \n the whole compound key is always unique for such an element. */',ascending};
							unique
						}
						COLLAPSED
						<BODY>
							<TEMPLATE_CALL>
								DESCR='(start from a new page)'
								COND='getBooleanParam("page.start.component")'
								FMT={
									sec.spacing.before='20';
									sec.page.breakBefore='true';
								}
								TEMPLATE_FILE='xsddoc/element/element.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
							<TEMPLATE_CALL>
								DESCR='(continue on the same page)'
								COND='! getBooleanParam("page.start.component")'
								FMT={
									sec.spacing.before='20';
								}
								TEMPLATE_FILE='xsddoc/element/element.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
						</BODY>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='global elements only'
						COND='getBooleanParam("include.detail.element.global") &&\n! getBooleanParam("include.detail.element.local")'
						FMT={
							table.sizing='Relative';
							table.cellpadding.both='3';
						}
						TARGET_ET='xs:element'
						SCOPE='advanced-location-rules'
						RULES={
							{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getVar("nsURI")]/xs:element'};
						}
						SORTING='by-attr'
						SORTING_KEY={lpath='@name',ascending}
						COLLAPSED
						<BODY>
							<TEMPLATE_CALL>
								DESCR='(start from a new page)'
								COND='getBooleanParam("page.start.component")'
								FMT={
									sec.spacing.before='20';
									sec.page.breakBefore='true';
								}
								TEMPLATE_FILE='xsddoc/element/element.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
							<TEMPLATE_CALL>
								DESCR='(continue on the same page)'
								COND='! getBooleanParam("page.start.component")'
								FMT={
									sec.spacing.before='20';
								}
								TEMPLATE_FILE='xsddoc/element/element.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
						</BODY>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='local elements only'
						COND='getBooleanParam("include.detail.element.local") &&\n! getBooleanParam("include.detail.element.global")'
						FMT={
							table.sizing='Relative';
							table.cellpadding.both='3';
						}
						TARGET_ET='xs:%localElement'
						SCOPE='advanced-location-rules'
						RULES={
							{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getVar("nsURI")]/descendant::xs:%element'};
						}
						FILTER='getAttrStringValue("ref") == ""\n'
						SORTING='by-compound-key'
						SORTING_KEY={
							{expr='callStockSection("Local Element Name")',ascending,case_sensitive};
							{expr='getAttrValue("type") == "" ? contextElement.id\n\n/* if the element has an embedded type, there must be a separate \n doc for it. Since the first subkey may be repeating, this one ensures \n the whole compound key is always unique for such an element. */',ascending};
							unique
						}
						COLLAPSED
						<BODY>
							<TEMPLATE_CALL>
								DESCR='(start from a new page)'
								COND='getBooleanParam("page.start.component")'
								FMT={
									sec.spacing.before='20';
									sec.page.breakBefore='true';
								}
								TEMPLATE_FILE='xsddoc/element/element.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
							<TEMPLATE_CALL>
								DESCR='(continue on the same page)'
								COND='! getBooleanParam("page.start.component")'
								FMT={
									sec.spacing.before='20';
								}
								TEMPLATE_FILE='xsddoc/element/element.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
						</BODY>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='complexTypes'
						COND='getBooleanParam("include.detail.complexType")'
						FMT={
							table.sizing='Relative';
							table.cellpadding.both='3';
						}
						TARGET_ET='xs:complexType'
						SCOPE='advanced-location-rules'
						RULES={
							{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getVar("nsURI")]/descendant::xs:complexType'};
						}
						SORTING='by-attr'
						SORTING_KEY={lpath='@name',ascending}
						COLLAPSED
						<BODY>
							<TEMPLATE_CALL>
								DESCR='(start from a new page)'
								COND='getBooleanParam("page.start.component")'
								FMT={
									sec.spacing.before='20';
									sec.page.breakBefore='true';
								}
								TEMPLATE_FILE='xsddoc/type/complexType.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
							<TEMPLATE_CALL>
								DESCR='(continue on the same page)'
								COND='! getBooleanParam("page.start.component")'
								FMT={
									sec.spacing.before='20';
								}
								TEMPLATE_FILE='xsddoc/type/complexType.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
						</BODY>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='simpleTypes'
						COND='getBooleanParam("include.detail.simpleType")'
						FMT={
							table.sizing='Relative';
							table.cellpadding.both='3';
						}
						TARGET_ET='xs:simpleType'
						SCOPE='advanced-location-rules'
						RULES={
							{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getVar("nsURI")]/descendant::xs:simpleType'};
						}
						SORTING='by-attr'
						SORTING_KEY={lpath='@name',ascending}
						COLLAPSED
						<BODY>
							<TEMPLATE_CALL>
								DESCR='(start from a new page)'
								COND='getBooleanParam("page.start.component")'
								FMT={
									sec.spacing.before='20';
									sec.page.breakBefore='true';
								}
								TEMPLATE_FILE='xsddoc/type/simpleType.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
							<TEMPLATE_CALL>
								DESCR='(continue on the same page)'
								COND='! getBooleanParam("page.start.component")'
								FMT={
									sec.spacing.before='20';
								}
								TEMPLATE_FILE='xsddoc/type/simpleType.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
						</BODY>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='element groups'
						COND='getBooleanParam("include.detail.group")'
						FMT={
							table.sizing='Relative';
							table.cellpadding.both='3';
						}
						TARGET_ET='xs:group'
						SCOPE='advanced-location-rules'
						RULES={
							{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getVar("nsURI")]/descendant::xs:group'};
						}
						SORTING='by-attr'
						SORTING_KEY={lpath='@name',ascending}
						COLLAPSED
						<BODY>
							<TEMPLATE_CALL>
								DESCR='(start from a new page)'
								COND='getBooleanParam("page.start.component")'
								FMT={
									sec.spacing.before='20';
									sec.page.breakBefore='true';
								}
								TEMPLATE_FILE='xsddoc/groups/group.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
							<TEMPLATE_CALL>
								DESCR='(continue on the same page)'
								COND='! getBooleanParam("page.start.component")'
								FMT={
									sec.spacing.before='20';
								}
								TEMPLATE_FILE='xsddoc/groups/group.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
						</BODY>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='global attributes'
						COND='getBooleanParam("include.detail.attribute")'
						FMT={
							table.sizing='Relative';
							table.cellpadding.both='3';
						}
						TARGET_ET='xs:attribute'
						SCOPE='advanced-location-rules'
						RULES={
							{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getVar("nsURI")]/xs:attribute'};
						}
						SORTING='by-attr'
						SORTING_KEY={lpath='@name',ascending}
						COLLAPSED
						<BODY>
							<TEMPLATE_CALL>
								DESCR='(start from a new page)'
								COND='getBooleanParam("page.start.component")'
								FMT={
									sec.spacing.before='20';
									sec.page.breakBefore='true';
								}
								TEMPLATE_FILE='xsddoc/attribute/attribute.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
							<TEMPLATE_CALL>
								DESCR='(continue on the same page)'
								COND='! getBooleanParam("page.start.component")'
								FMT={
									sec.spacing.before='20';
								}
								TEMPLATE_FILE='xsddoc/attribute/attribute.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
						</BODY>
					</ELEMENT_ITER>
					<ELEMENT_ITER>
						DESCR='attribute groups'
						COND='getBooleanParam("include.detail.attributeGroup")'
						FMT={
							table.sizing='Relative';
							table.cellpadding.both='3';
						}
						TARGET_ET='xs:attributeGroup'
						SCOPE='advanced-location-rules'
						RULES={
							{'*','#DOCUMENT/xs:schema[getAttrStringValue("targetNamespace") == getVar("nsURI")]/descendant::xs:attributeGroup'};
						}
						SORTING='by-attr'
						SORTING_KEY={lpath='@name',ascending}
						COLLAPSED
						<BODY>
							<TEMPLATE_CALL>
								DESCR='(start from a new page)'
								COND='getBooleanParam("page.start.component")'
								FMT={
									sec.spacing.before='20';
									sec.page.breakBefore='true';
								}
								TEMPLATE_FILE='xsddoc/groups/attributeGroup.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
							<TEMPLATE_CALL>
								DESCR='(continue on the same page)'
								COND='! getBooleanParam("page.start.component")'
								FMT={
									sec.spacing.before='20';
								}
								TEMPLATE_FILE='xsddoc/groups/attributeGroup.tpl'
								OUTPUT_TYPE='included'
								OUTPUT_DIR_EXPR='getVar("schemaFolder").toString()'
								DSM_MODE='pass-current-model'
							</TEMPLATE_CALL>
						</BODY>
					</ELEMENT_ITER>
				</BODY>
			</FOLDER>
		</BODY>
	</ELEMENT_ITER>
	<TEMPLATE_CALL>
		DESCR='Bottom Message'
		TEMPLATE_FILE='xsddoc/about.tpl'
		OUTPUT_TYPE='included'
		DSM_MODE='pass-current-model'
	</TEMPLATE_CALL>
</ROOT>
<STOCK_SECTIONS>
	<FOLDER>
		MATCHING_ET='#DOCUMENTS'
		SS_NAME='Init'
		<BODY>
			<TEMPLATE_CALL>
				TEMPLATE_FILE='xsddoc/init.tpl'
				OUTPUT_TYPE='document'
				DSM_MODE='pass-current-model'
			</TEMPLATE_CALL>
		</BODY>
	</FOLDER>
	<AREA_SEC>
		MATCHING_ET='xs:%localElement'
		FMT={
			sec.outputStyle='text-par';
			txtfl.delimiter.type='none';
		}
		SS_NAME='Local Element Name'
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
						FORMULA='localName = getAttrStringValue("name");\n\n((form = getAttrStringValue("form")) == "") ?  { \n  schema = getXMLDocument().findChild ("xs:schema");\n  form = schema.getAttrStringValue ("elementFormDefault")\n};\n\nform == "qualified" ? \n  QName (getVar("nsURI").toString(), localName) :  localName;'
					</DATA_CTRL>
					<TEMPLATE_CALL_CTRL>
						FMT={
							text.font.style.bold='false';
						}
						TEMPLATE_FILE='xsddoc/element/localElementExt.tpl'
						OUTPUT_TYPE='included'
						DSM_MODE='pass-current-model'
					</TEMPLATE_CALL_CTRL>
				</CTRLS>
			</CTRL_GROUP>
		</AREA>
	</AREA_SEC>
</STOCK_SECTIONS>
CHECKSUM='zz4+dBqk6QoFe8kJc9Yo?A'
</DOCFLEX_TEMPLATE>