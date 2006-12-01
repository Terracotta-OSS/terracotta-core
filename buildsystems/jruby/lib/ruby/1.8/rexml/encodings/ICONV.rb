#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

require "iconv"
raise LoadError unless defined? Iconv

module REXML
  module Encoding
    def decode_iconv(str)
      Iconv.conv(UTF_8, @encoding, str)
    end

    def encode_iconv(content)
      Iconv.conv(@encoding, UTF_8, content)
    end

    register("ICONV") do |obj|
      Iconv.conv(UTF_8, obj.encoding, nil)
      class << obj
        alias decode decode_iconv
        alias encode encode_iconv
      end
    end
  end
end
