module Postscripts
  def postscripts(name, directory, spec)
    load FilePath.new(@static_resources.kit_builder_scripts, "#{name}.rb").to_s
    method(:postscript).call(ant, @build_environment, product_directory, *spec[:args])
  end
end