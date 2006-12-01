#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#
#   irb/help.rb - print usase module
#   	$Release Version: 0.9.5$
#   	$Revision$
#   	$Date$
#   	by Keiju ISHITSUKA(keiju@ishitsuka.com)
#
# --
#
#   
#

module IRB
  def IRB.print_usage
    lc = IRB.conf[:LC_MESSAGES]
    path = lc.find("irb/help-message")
    space_line = false
    File.foreach(path) do
      |l|
      if /^\s*$/ =~ l
	lc.puts l unless space_line
	space_line = true
	next
      end
      space_line = false
      
      l.sub!(/#.*$/, "")
      next if /^\s*$/ =~ l
      lc.puts l
    end
  end
end

