#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Additions of methods to standard Ruby classes that make our life a lot easier.
class Object
  def escape(style = :properties)
    self.to_s.escape(style)
  end
end


# Adds methods to class String that are useful for our purposes.
class String
  # Does this string start with exactly the given string?
  def starts_with?(str)
    self.length >= str.length && (str.empty? || self[0..str.length - 1] == str)
  end

  # Does this string end with exactly the given string?
  def ends_with?(str)
    self.length >= str.length && (str.empty? || self[-str.length..-1] == str)
  end

  # Is this string either the empty string, or contain only whitespace? This method
  # is also implemented on NilClass, so you can safely say things like
  #
  #    if my_string.blank?
  #
  # and have it work (i.e., the test will pass) even if my_string is nil.
  def blank?
    self.strip.empty?
  end

  # Is this string the empty string? This method
  # is also implemented on NilClass, so you can safely say things like
  #
  #    if my_string.blank?
  #
  # and have it work (i.e., the test will pass) even if my_string is nil.
  def empty?
    self.length == 0
  end

  # Remove any number of copies of the given string from either just the beginning
  # (if ends is :start), just the end (if ends is :end), or both of this string.
  def trim(str, ends=:both)
    assert { [:start, :end, :both].include?(ends) }

    out = self.dup

    unless str.empty?
      if [:both, :start].include?(ends)
        while out.starts_with?(str)
          out = out[str.length..-1]
        end
      end

      if [:both, :end].include?(ends)
        while out.ends_with?(str)
          out = out[0..-(str.length + 1)]
        end
      end
    end

    out
  end

  def escape(style = :properties)
    case style
    when :properties
      self.to_propertyfile_escaped_s
    when :xml
      self.xml_escape(false)
    when :xmlattr, :xml_attr, :xml_attribute
      self.xml_escape(true)
    when :shell
      self.to_shell_escaped_s
    else
      raise "Unrecognized escape style: #{style}"
    end
  end

  # Returns a version of this string that's escaped properly for inclusion in
  # a Java properties file (i.e., the kind used by java.util.Properties.load()).
  # This basically just escapes backslashes with a second backslash. (I *think*
  # that's all we need to do here...right?)
  def to_propertyfile_escaped_s
    out = ""
    each_byte do |byte|
      if byte == 92 # backslash; don't do '\\' -- JRuby really doesn't like it, and you won't have fun deciphering the resulting error messages
        out << 92.chr << 92.chr
      else
        out << byte.chr
      end
    end

    out
  end

  # Returns a version of this string that's escaped properly for inclusion in
  # XML; escapes '&' with '&amp;', '<' with '&lt;', and '>' with '&gt;'. Also
  # escapes '"' with '&quot;' and ''' (single straight quote) with '&apos;' if
  # for_attribute is true, so that you can embed the string in an attribute, not
  # just an element.
  def xml_escape(for_attribute=false)
    out = gsub("&", "&amp;")
    out = out.gsub('"', '&quot;').gsub("'", "&apos;") if for_attribute
    out = out.gsub("<", "&lt;").gsub(">", "&gt;")
    out
  end

  # Returns a version of this string that's escaped properly for processing with
  # the Unix shell. This basically puts backslashes before single and double quotes,
  # and then, _if_ there are spaces or backslashes anywhere in the source string,
  # surrounds the entire thing with single quotes _unless_ it already appears
  # to have single quotes around it.
  #
  # Phew. Madness. I hate this kind of shit.
  def to_shell_escaped_s
    last_byte = nil
    out = ""
    needs_quoting = false
    is_quoted = self.length >= 2 && ((self[0] == '\'' && self[-1] == '\'') || (self[0] == '\"' && self[-1] == '\"'))

    self.each_byte do |byte|
      if (byte == '\'' || byte == '\"') && (last_byte.nil? || last_byte != '\\')
        out += '\\'
      end

      if byte == ' ' && (last_byte.nil? || last_byte != '\\')
        needs_quoting = true
      end

      out += byte.chr
    end

    if (! is_quoted) && needs_quoting
      out = "'" + out + "'"
    end

    out
  end
end

# Make 'nil' respond to the String methods blank? and empty? properly, so that
# you don't have to check (x == nil || x.blank?) but can instead just say (x.blank?).
class NilClass
  def blank?
    true
  end

  def empty?
    true
  end
end

# Gives Hash a new method.
class Hash
  # Iterates over the supplied block, passing it a copy of the hash you've called
  # this method on, except that each time through the block the given key is set,
  # sequentially, to one of the given values. That is, if you do:
  #
  #     { :a => :b, :c => :d }.vary('c', [ 'e', 'f', 'g' ]) { |hash| puts hash }
  #
  # You'll get something like:
  #
  #       a => b, c => e
  #       a => b, c => f
  #       a => b, c => g
  #
  # (Note that there is no requirement to have the key you're varying over already
  # bound in the source hash; that's just an artifact of the above example to show
  # you that it will, indeed, replace any existing value.)
  #
  # The source hash (the receiver of this message) will be unaltered in all cases.
  # We're generating new copies of the source hash, not mutating it.
  def vary(key, values)
    values.each do |value|
      new_hash = merge({ key => value })
      yield new_hash
    end
  end
end

