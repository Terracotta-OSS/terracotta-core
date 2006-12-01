#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

module REXML
	# Defines a number of tokens used for parsing XML.  Not for general
	# consumption.
	module XMLTokens
		NCNAME_STR= '[\w:][\-\w\d.]*'
		NAME_STR= "(?:#{NCNAME_STR}:)?#{NCNAME_STR}"

		NAMECHAR = '[\-\w\d\.:]'
		NAME = "([\\w:]#{NAMECHAR}*)"
		NMTOKEN = "(?:#{NAMECHAR})+"
		NMTOKENS = "#{NMTOKEN}(\\s+#{NMTOKEN})*"
		REFERENCE = "(?:&#{NAME};|&#\\d+;|&#x[0-9a-fA-F]+;)"

		#REFERENCE = "(?:#{ENTITYREF}|#{CHARREF})"
		#ENTITYREF = "&#{NAME};"
		#CHARREF = "&#\\d+;|&#x[0-9a-fA-F]+;"
	end
end
