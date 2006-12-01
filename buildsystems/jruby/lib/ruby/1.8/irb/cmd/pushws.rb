#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#
#   change-ws.rb - 
#   	$Release Version: 0.9.5$
#   	$Revision$
#   	$Date$
#   	by Keiju ISHITSUKA(keiju@ruby-lang.org)
#
# --
#
#   
#

require "irb/cmd/nop.rb"
require "irb/ext/workspaces.rb"

module IRB
  module ExtendCommand
    class Workspaces<Nop
      def execute(*obj)
	irb_context.workspaces.collect{|ws| ws.main}
      end
    end

    class PushWorkspace<Workspaces
      def execute(*obj)
	irb_context.push_workspace(*obj)
	super
      end
    end

    class PopWorkspace<Workspaces
      def execute(*obj)
	irb_context.pop_workspace(*obj)
	super
      end
    end
  end
end

