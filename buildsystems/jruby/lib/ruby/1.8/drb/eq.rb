#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

require 'drb/drb'

module DRb
  class DRbObject
    def ==(other)
      return false unless DRbObject === other
     (@ref == other.__drbref) && (@uri == other.__drburi)
    end

    def hash
      [@uri, @ref].hash
    end

    alias eql? ==
  end
end
