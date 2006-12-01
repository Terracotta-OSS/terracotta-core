#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# -*- ruby -*-

require 'optparse'
require 'uri'

OptionParser.accept(URI) {|s,| URI.parse(s) if s}
