#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

require "rexml/child"
module REXML
	module DTD
		class AttlistDecl < Child
			START = "<!ATTLIST"
			START_RE = /^\s*#{START}/um
			PATTERN_RE = /\s*(#{START}.*?>)/um
		end
	end
end
