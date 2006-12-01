#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

module Postscripts
  def postscripts(name, directory, spec)
    load FilePath.new(@static_resources.kit_builder_scripts, "#{name}.rb").to_s
    method(:postscript).call(ant, @build_environment, product_directory, *spec[:args])
  end
end