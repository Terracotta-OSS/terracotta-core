#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

require "rexml/child"
module REXML
	module DTD
		class ElementDecl < Child
			START = "<!ELEMENT"
			START_RE = /^\s*#{START}/um
			PATTERN_RE = /^\s*(#{START}.*?)>/um
			PATTERN_RE = /^\s*#{START}\s+((?:[:\w_][-\.\w_]*:)?[-!\*\.\w_]*)(.*?)>/
			#\s*((((["']).*?\5)|[^\/'">]*)*?)(\/)?>/um, true)

			def initialize match
				@name = match[1]
				@rest = match[2]
			end
		end
	end
end
