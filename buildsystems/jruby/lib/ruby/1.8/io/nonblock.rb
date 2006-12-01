#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

require "fcntl"
class IO
  def nonblock?
    (fcntl(Fcntl::F_GETFL) & File::NONBLOCK) != 0
  end

  def nonblock=(nb)
    f = fcntl(Fcntl::F_GETFL)
    if nb
      f |= File::NONBLOCK
    else
      f &= ~File::NONBLOCK
    end
    fcntl(Fcntl::F_SETFL, f)
  end

  def nonblock(nb = true)
    nb, self.nonblock = nonblock?, nb
    yield
  ensure
    self.nonblock = nb
  end
end if defined?(Fcntl::F_GETFL)
