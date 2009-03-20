#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

module Postscripts
  def postscripts(name, directory, spec)
    puts "calling postscript #{name}"
    load FilePath.new(@static_resources.kit_builder_scripts, "#{name}.rb").to_s
    method(:postscript).call(ant, @build_environment, product_directory, *spec[:args])
  end
end