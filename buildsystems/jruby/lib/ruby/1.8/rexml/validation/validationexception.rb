#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

module REXML
  module Validation
    class ValidationException < RuntimeError
      def initialize msg
        super
      end
    end
  end
end
