#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  protected

  # - Install TIMs from the Forge into the kit using the tim-get tool.
  def postscript(ant, build_environment, product_directory, *args)
    
    kit_type = @config_source['kit-type']
    return unless kit_type
    
    tim_get_properties_file = File.join(product_directory.to_s, args[0]['location'])
    types = args[1]['kit-type']

    new_props = types[kit_type]

    if new_props.nil?
      puts "XXX: kit-type #{kit_type} doen't overwrite any tim-get properties"
      return
    end

    tim_get_original_content = []
    File.open(tim_get_properties_file) do |f|
      tim_get_original_content = f.readlines
    end

    tim_get_updated_content = []
    tim_get_original_content.each do |line|
      updated_line = line
      entry = line.strip.split(/=/)
      
      if entry.size == 2
        key = entry[0].strip
        updated_line = key + " = " + interpolate(new_props[key]) if new_props[key]
      end
      tim_get_updated_content << updated_line
    end

    File.open(tim_get_properties_file, "w") do |f|
      tim_get_updated_content.each do |line|
        f.puts line
      end
    end
    
  end
end
