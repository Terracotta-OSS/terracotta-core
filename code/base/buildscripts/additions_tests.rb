#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

require 'test/unit'
require 'additions'

# Unit tests for the code in 'additions.rb'. Needs lots and lots of additions.
class StringTests < Test::Unit::TestCase
    def test_starts_with
        assert(! "foo".starts_with?("x"))
        assert(! "foo".starts_with?("o"))
        assert(! "foo".starts_with?("oo"))
        assert(! "foo".starts_with?("foof"))
        
        assert("foo".starts_with?(""))
        assert("foo".starts_with?("f"))
        assert("foo".starts_with?("fo"))
        assert("foo".starts_with?("foo"))
    end
    
    def test_ends_with
        assert(! "bar".ends_with?("x"))
        assert(! "bar".ends_with?("a"))
        assert(! "bar".ends_with?("ba"))
        assert(! "bar".ends_with?("bbar"))
        assert(! "bar".ends_with?("ra"))
        
        assert("bar".ends_with?(""))
        assert("bar".ends_with?("r"))
        assert("bar".ends_with?("ar"))
        assert("bar".ends_with?("bar"))
    end
    
    def test_blank
        assert("".blank?)
        assert("   ".blank?)
        assert(nil.blank?)
        
        assert(! "x".blank?)
        assert(! "   x   ".blank?)
    end
    
    def test_empty
        assert("".empty?)
        assert(nil.empty?)
        
        assert(! "x".empty?)
        assert(! "    ".empty?)
        assert(! "    x     ".empty?)
    end
    
    def test_trim
        assert_equal("bar", "fbar".trim("f"))
        assert_equal("bar", "fbar".trim("f", :both))
        assert_equal("bar", "fbar".trim("f", :start))
        assert_equal("fbar", "fbar".trim("f", :end))

        assert_equal("bar", "fbarf".trim("f"))
        assert_equal("bar", "fbarf".trim("f", :both))
        assert_equal("barf", "fbarf".trim("f", :start))
        assert_equal("fbar", "fbarf".trim("f", :end))

        assert_equal("fbarf", "fbarf".trim(""))
        assert_equal("fbarf", "fbarf".trim("", :both))
        assert_equal("fbarf", "fbarf".trim("", :start))
        assert_equal("fbarf", "fbarf".trim("", :end))
        
        assert_equal("fobarfo", "foofoofobarfofoofoo".trim("foo"))
        assert_equal("fobarfo", "foofoofobarfofoofoo".trim("foo", :both))
        assert_equal("fobarfofoofoo", "foofoofobarfofoofoo".trim("foo", :start))
        assert_equal("foofoofobarfo", "foofoofobarfofoofoo".trim("foo", :end))
        
        assert_raise(RuntimeError) { "foo".trim("f", :funk) }
    end
end
