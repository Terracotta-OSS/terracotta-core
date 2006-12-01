#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

require 'observer'

module DRb
  module DRbObservable
    include Observable

    def notify_observers(*arg)
      if defined? @observer_state and @observer_state
	if defined? @observer_peers
	  for i in @observer_peers.dup
	    begin
	      i.update(*arg)
	    rescue
	      delete_observer(i)
	    end
	  end
	end
	@observer_state = false
      end
    end
  end
end
