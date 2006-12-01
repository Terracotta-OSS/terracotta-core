#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

require 'rexml/encodings/US-ASCII'

module REXML
  module Encoding
    register("ISO-8859-1", &encoding_method("US-ASCII"))
  end
end
