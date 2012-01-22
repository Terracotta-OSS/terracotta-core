/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.TCRuntimeException;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.test.TCTestCase;
import com.tc.util.ObjectIDSet.ObjectIDSetType;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class ObjectIDSetTest extends TCTestCase {

  public void testContain() {
    final ObjectIDSet bitSetBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    final ObjectIDSet rangeBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final HashSet<ObjectID> hashSet = new HashSet<ObjectID>();

    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("testContain : Seed for Random is " + seed);
    final Random r = new Random(seed);

    for (int i = 0; i < 100000; i++) {
      final ObjectID oid = new ObjectID(r.nextLong());
      bitSetBasedObjectIDSet.add(oid);
      rangeBasedObjectIDSet.add(oid);
      hashSet.add(oid);
    }

    for (final ObjectID oid : hashSet) {
      Assert.assertTrue(bitSetBasedObjectIDSet.contains(oid));
      Assert.assertTrue(rangeBasedObjectIDSet.contains(oid));
    }
  }

  public void testFailingAddAll() {
    ObjectIDSet oidSet1 = new ObjectIDSet();
    HashSet hashSet = new HashSet();
    long failedIDs[] = new long[] { 1884, 1371, 595, 440, 730, 1382, 1781, 217, 1449, 1043, 1556, 1679, 347, 860, 1020,
        1619, 1801, 1146, 769, 19, 532, 655, 692, 1268, 1793, 1533, 1616, 1702, 1241, 1754, 633, 1192, 166, 1312, 179,
        945, 44, 755, 1390, 1070, 431, 293, 1319, 339, 852, 103, 141, 874, 1643, 592, 1477, 242, 1165, 777, 953, 1580,
        554, 866, 1441, 1520, 507, 807, 301, 1327, 609, 1098, 369, 364, 1182, 1062, 36, 714, 74, 1228, 453, 399, 1275,
        581, 1684, 325, 897, 1929, 1566, 1324, 1401, 475, 1846, 298, 1423, 660, 564, 977, 1503, 408, 1625, 1569, 148,
        1054, 1334, 1646, 605, 1198, 336, 249, 13, 392, 1135, 1235, 231, 131, 1037, 76, 1586, 809, 1858, 985, 1345,
        186, 526, 784, 838, 1253, 1542, 485, 998, 620, 1096, 910, 1416, 5, 1839, 958, 557, 239, 164, 241, 1898, 1728,
        113, 1029, 1772, 383, 675, 518, 698, 1261, 1764, 1143, 105, 1379, 68, 639, 918, 801, 1206, 1511, 1594, 1149,
        792, 1917, 493, 1006, 54, 1080, 172, 1791, 320, 1267, 757, 746, 1316, 1854, 290, 572, 683, 467, 1298, 1535,
        631, 738, 690, 341, 1342, 1649, 1495, 1361, 123, 832, 1216, 1432, 1705, 446, 891, 1640, 808, 1812, 851, 1530,
        650, 1537, 218, 367, 1088, 636, 1244, 1452, 1893, 266, 1023, 1782, 22, 178, 1166, 535, 1794, 296, 1697, 1370,
        575, 437, 1297, 612, 1189, 1128, 510, 422, 786, 1063, 1179, 677, 816, 950, 1292, 668, 1442, 1885, 596, 83,
        1522, 432, 1579, 1721, 1802, 357, 865, 1992, 966, 43, 551, 942, 1480, 454, 108, 1668, 153, 794, 1254, 1353,
        1909, 1284, 833, 311, 1694, 1387, 1439, 193, 841, 1867, 1219, 1553, 132, 1488, 462, 661, 35, 1040, 707, 824,
        1733, 974, 1550, 376, 1615, 733, 1820, 589, 1759, 100, 1676, 258, 1015, 1457, 1109, 1622, 859, 1101, 413, 1048,
        140, 30, 607, 1561, 201, 352, 543, 928, 1227, 715, 287, 1174, 1687, 502, 1305, 881, 1741, 400, 1331, 621, 819,
        1855, 328, 1171, 645, 91, 1093, 1053, 1545, 389, 1404, 776, 990, 1278, 1916, 1396, 1606, 902, 1504, 478, 1587,
        51, 210, 185, 1236, 722, 1262, 1671, 1748, 1775, 335, 250, 1859, 1415, 1388, 461, 75, 1487, 519, 628, 1323,
        1836, 1908, 1710, 762, 1158, 848, 14, 1136, 588, 1197, 1032, 999, 486, 1118, 1144, 1656, 384, 1071, 1584, 1465,
        1380, 67, 234, 701, 747, 913, 674, 161, 344, 1713, 873, 1315, 1828, 1079, 754, 405, 1007, 494, 580, 116, 934,
        421, 1641, 6, 279, 856, 1205, 1664, 1339, 1152, 1901, 319, 693, 1362, 226, 1790, 739, 567, 445, 1514, 1985,
        1087, 682, 1431, 169, 982, 1270, 1874, 59, 1496, 470, 896, 124, 827, 1213, 1595 };

    for (long failedID : failedIDs) {
      ObjectID oid = new ObjectID(failedID, 1);
      oidSet1.add(oid);
      hashSet.add(oid);
    }

    Assert.assertEquals(435, oidSet1.size());
    Assert.assertEquals(hashSet.size(), oidSet1.size());
    Assert.assertEquals(failedIDs.length, oidSet1.size());

    ObjectIDSet oidSet2 = new ObjectIDSet();
    oidSet2.add(new ObjectID(410, 1));

    boolean added = oidSet1.addAll(oidSet2);
    Assert.assertTrue(added);

    hashSet.addAll(oidSet2);

    Assert.assertEquals(failedIDs.length + 1, oidSet1.size());
    Assert.assertEquals(hashSet, oidSet1);
  }

  public void testFailingAddAll2() {
    long thisIDs[] = new long[] { 72057594037928192l,
        parseLong("10000000000000000000000000000000000000000000000000000000", 2), 72057594037928768l,
        parseLong("10", 2), 72057594037929216l, Long.parseLong("10000", 2), 72057594037929600l,
        parseLong("1000000000000000000000000000000", 2) };
    long otherIDs[] = new long[] { 72057594037927936l,
        parseLong("1111000011110000101101001111001011110000111100101101001011011", 2), 72057594037928000l,
        parseLong("1111000010110100101001011011010010101101100001111000111100001111", 2), 72057594037928064l,
        parseLong("101101101111001010010110110101101001000111010011111000011110000", 2), 72057594037928128l,
        parseLong("1010010110100100000111100001111001011010000111100111101001011010", 2), 72057594037928192l,
        parseLong("1111000001110001011000000110000111100001111010011110000111100101", 2), 72057594037928256l,
        parseLong("111100011110010110100000111101011011010110101111000101011000", 2), 72057594037928320l,
        parseLong("1010011110000110100001011010011010110110101101010001111010110101", 2), 72057594037928384l,
        parseLong("1111001011110010110100101101111100001111000011110010110100001", 2), 72057594037928448l,
        parseLong("10010110100100000011010010110010111100001111010011010010110100", 2), 72057594037928512l,
        parseLong("100101001011011111000010100101110001111000011110111101000011111", 2), 72057594037928576l,
        parseLong("1111000011110010101101001101010011000000111000101101001111110000", 2), 72057594037928640l,
        parseLong("111100001111010010110100101100101111001001001011110010111100", 2), 72057594037928704l,
        parseLong("101101111111010111000001110000110000010110000101101001111010011", 2), 72057594037928768l,
        parseLong("1010010000001110001111100011110001111001101101011010011010010100", 2), 72057594037928832l,
        parseLong("111100001011000010110100111100101101000011110000110101101001", 2), 72057594037928896l,
        parseLong("11110010111100101101001011010011010000111100001111010010010011", 2), 72057594037928960l,
        parseLong("1111000011110000111110000111100000111011010110110100101101001010", 2), 72057594037929024l,
        parseLong("1000111100001111000111100001111111000010101101001011011010110100", 2), 72057594037929088l,
        parseLong("111100100100101101001011000011111100001111010011010010111100001", 2), 72057594037929152l,
        parseLong("1101001001010110010110100001101110000110100101101001111000011110", 2), 72057594037929216l,
        parseLong("10110110101101100011111000111111100011110000011010010111100100", 2), 72057594037929280l,
        parseLong("111100101111001111110010111000001111000001111010100100000111001", 2), 72057594037929344l,
        parseLong("1000011110100111100001101101101010010110100101100001011010010110", 2), 72057594037929408l,
        parseLong("110100100101001111011010010100101101001011010011111100001101001", 2), 72057594037929472l,
        parseLong("1111100001111001111100001111000010010110101101100111011010010101", 2), 72057594037929536l,
        parseLong("11110100101101000111100011110100101001011010011000111101101000", 2), 72057594037929600l,
        parseLong("1101000011010010111100001111000010110111110000111101001111000011", 2), 72057594037929664l,
        parseLong("111100000111101101001000010010110101101001111010010111100101100", 2), 72057594037929728l,
        parseLong("1000011100000111010110110101101101011011110010011100001110000010", 2), 72057594037929792l,
        parseLong("1101001011010000111100001101001011010010101101001011100001111001", 2), 72057594037929856l,
        parseLong("10110101001111010010110100101101001011010010110100100100101100", 2), 72057594037929920l,
        parseLong("1111000011010011", 2) };

    ObjectIDSet oidSet = createBitSetObjectFrom(thisIDs);
    ObjectIDSet other = createBitSetObjectFrom(otherIDs);

    HashSet oidHashSet = new HashSet(oidSet);
    HashSet otherHashSet = new HashSet(other);

    oidSet.addAll(other);
    oidHashSet.addAll(otherHashSet);

    Assert.assertEquals(oidSet.size(), oidHashSet.size());
    Assert.assertEquals(oidSet, oidHashSet);
  }

  private long parseLong(String longStr, int base) {
    if (longStr.length() < 64) {
      return Long.parseLong(longStr, base);
    } else {
      long l = Long.parseLong(longStr.substring(1), base);
      long highBit = 1L << 63;
      l |= highBit;
      Assert.assertEquals(longStr, Long.toBinaryString(l));
      return l;
    }
  }

  private ObjectIDSet createBitSetObjectFrom(long[] ids) {
    ObjectIDSet oidSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    assertEquals(0, ids.length % 2);
    int size = getSizeFromBitSetIDs(ids);
    TCByteBufferOutputStream stream = new TCByteBufferOutputStream();
    stream.writeInt(ObjectIDSetType.BITSET_BASED_SET.ordinal());
    stream.writeInt(size);
    for (long id : ids) {
      stream.writeLong(id);
    }
    stream.close();
    TCByteBuffer[] data = stream.toArray();
    TCByteBufferInputStream input = new TCByteBufferInputStream(data);
    try {
      oidSet.deserializeFrom(input);
    } catch (IOException e) {
      throw new TCRuntimeException(e);
    }
    return oidSet;
  }

  private int getSizeFromBitSetIDs(long[] ids) {
    int size = 0;
    for (int i = 1; i < ids.length; i += 2) {
      size += Long.bitCount(ids[i]);
    }
    return size;
  }

  public void testIterator() {
    final ObjectIDSet bitSetBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    final ObjectIDSet rangeBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final TreeSet<ObjectID> treeSet = new TreeSet<ObjectID>();

    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("testIterator : Seed for Random is " + seed);
    final Random r = new Random(seed);

    for (int i = 0; i < 100000; i++) {
      final ObjectID oid = new ObjectID(r.nextLong());
      bitSetBasedObjectIDSet.add(oid);
      rangeBasedObjectIDSet.add(oid);
      treeSet.add(oid);
    }

    Assert.assertEquals(treeSet.size(), bitSetBasedObjectIDSet.size());
    Assert.assertEquals(treeSet.size(), rangeBasedObjectIDSet.size());

    final Iterator<ObjectID> tsIterator = treeSet.iterator();
    final Iterator<ObjectID> bitSetIterator = bitSetBasedObjectIDSet.iterator();
    final Iterator<ObjectID> rangeIterator = rangeBasedObjectIDSet.iterator();

    while (tsIterator.hasNext()) {
      final ObjectID oid = tsIterator.next();
      Assert.assertEquals(oid.toLong(), bitSetIterator.next().toLong());
      Assert.assertEquals(oid.toLong(), rangeIterator.next().toLong());
    }

    Assert.assertFalse(bitSetIterator.hasNext());
    Assert.assertFalse(rangeIterator.hasNext());
  }

  public void testNegativeIds() {
    final ObjectIDSet bitSetBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    final ObjectIDSet rangeBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final TreeSet<ObjectID> treeSet = new TreeSet<ObjectID>();

    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("testNegativeIds : Seed for Random is " + seed);
    final Random r = new Random(seed);

    for (int i = 0; i < 100000; i++) {
      final ObjectID oid = new ObjectID(r.nextLong());
      bitSetBasedObjectIDSet.add(oid);
      rangeBasedObjectIDSet.add(oid);
      treeSet.add(oid);
    }

    Assert.assertEquals(treeSet, rangeBasedObjectIDSet);
    Assert.assertEquals(treeSet, bitSetBasedObjectIDSet);

    for (int i = 0; i < 100000; i++) {
      final ObjectID oid = new ObjectID(r.nextLong());
      bitSetBasedObjectIDSet.remove(oid);
      rangeBasedObjectIDSet.remove(oid);
      treeSet.remove(oid);
    }

    Assert.assertEquals(treeSet, bitSetBasedObjectIDSet);
    Assert.assertEquals(treeSet, rangeBasedObjectIDSet);

    for (int i = 0; i < 1000000; i++) {
      final ObjectID oid = new ObjectID(r.nextLong());
      Assert.assertEquals(treeSet.contains(oid), bitSetBasedObjectIDSet.contains(oid));
    }
  }

  public void testFirstAndLast() {
    final ObjectIDSet bitSetBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    final ObjectIDSet rangeBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final TreeSet<ObjectID> treeSet = new TreeSet<ObjectID>();

    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("testFirstAndLast : Seed for Random is " + seed);
    final Random r = new Random(seed);

    for (int i = 0; i < 10000; i++) {
      final ObjectID oid = new ObjectID(r.nextLong());
      bitSetBasedObjectIDSet.add(oid);
      rangeBasedObjectIDSet.add(oid);
      treeSet.add(oid);
    }

    Assert.assertEquals(treeSet.first(), bitSetBasedObjectIDSet.first());
    Assert.assertEquals(treeSet.first(), rangeBasedObjectIDSet.first());

    Assert.assertEquals(treeSet.last(), bitSetBasedObjectIDSet.last());
    Assert.assertEquals(treeSet.last(), rangeBasedObjectIDSet.last());
  }

  public void testRemove() {
    ObjectIDSet bitSetBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    bitSetBasedObjectIDSet.add(new ObjectID(10));
    bitSetBasedObjectIDSet.add(new ObjectID(14));
    bitSetBasedObjectIDSet.add(new ObjectID(1));
    bitSetBasedObjectIDSet.add(new ObjectID(18));
    bitSetBasedObjectIDSet.add(new ObjectID(75));
    bitSetBasedObjectIDSet.add(new ObjectID(68));
    bitSetBasedObjectIDSet.add(new ObjectID(175));
    bitSetBasedObjectIDSet.add(new ObjectID(205));

    // data : [ Range(0,1000100010000000010) Range(64,100000010000)
    // Range(128,100000000000000000000000000000000000000000000000) Range(192,10000000000000)]
    // ids: 1, 10, 14, 18, 68, 75, 175. 205

    final Iterator<ObjectID> iterator = bitSetBasedObjectIDSet.iterator();
    iterateElements(iterator, 4);
    iterator.remove();
    Assert.assertEquals(68, iterator.next().toLong());

    iterateElements(iterator, 1);
    iterator.remove();
    Assert.assertEquals(175, iterator.next().toLong());
    iterator.remove();
    Assert.assertEquals(205, iterator.next().toLong());
    Assert.assertFalse(iterator.hasNext());

    // testing random removes

    bitSetBasedObjectIDSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    final ObjectIDSet rangBasedOidSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final HashSet<ObjectID> hashSet = new HashSet<ObjectID>();

    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("testRemove : Seed for Random is " + seed);
    final Random r = new Random(seed);

    for (int i = 0; i < 10000; i++) {
      final ObjectID oid = new ObjectID(r.nextLong());
      bitSetBasedObjectIDSet.add(oid);
      rangBasedOidSet.add(oid);
      hashSet.add(oid);
    }

    Assert.assertEquals(hashSet, bitSetBasedObjectIDSet);
    Assert.assertEquals(hashSet, rangBasedOidSet);

    for (int i = 0; i < 10000; i++) {
      final ObjectID oid = new ObjectID(r.nextLong());
      bitSetBasedObjectIDSet.remove(oid);
      rangBasedOidSet.remove(oid);
      hashSet.remove(oid);
    }

    Assert.assertEquals(hashSet, bitSetBasedObjectIDSet);
    Assert.assertEquals(hashSet, rangBasedOidSet);
  }

  public void testPerformance() {
    ObjectIDSet bitSetBasedOidSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    ObjectIDSet rangeBasedOidSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final HashSet<ObjectID> hashSet = new HashSet<ObjectID>();

    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("Seed for Random is " + seed);
    final Random r = new Random(seed);

    for (int i = 0; i < 800000; i++) {
      final long l = r.nextLong();
      final ObjectID id = new ObjectID(l);
      hashSet.add(id);
    }

    final long t1 = System.currentTimeMillis();
    for (final ObjectID objectID : hashSet) {
      bitSetBasedOidSet.add(objectID);
    }
    final long t2 = System.currentTimeMillis();

    for (final ObjectID objectID : hashSet) {
      rangeBasedOidSet.add(objectID);
    }
    final long t3 = System.currentTimeMillis();

    for (final ObjectID objectID : hashSet) {
      bitSetBasedOidSet.contains(objectID);
    }
    final long t4 = System.currentTimeMillis();

    for (final ObjectID objectID : hashSet) {
      rangeBasedOidSet.contains(objectID);
    }
    final long t5 = System.currentTimeMillis();

    for (final ObjectID objectID : hashSet) {
      bitSetBasedOidSet.remove(objectID);
    }
    final long t6 = System.currentTimeMillis();

    for (final ObjectID objectID : hashSet) {
      rangeBasedOidSet.remove(objectID);
    }
    final long t7 = System.currentTimeMillis();

    bitSetBasedOidSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    rangeBasedOidSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);

    final long t8 = System.currentTimeMillis();
    bitSetBasedOidSet.addAll(hashSet);
    final long t9 = System.currentTimeMillis();
    rangeBasedOidSet.addAll(hashSet);
    final long t10 = System.currentTimeMillis();
    bitSetBasedOidSet.removeAll(hashSet);
    final long t11 = System.currentTimeMillis();
    rangeBasedOidSet.removeAll(hashSet);
    final long t12 = System.currentTimeMillis();

    System.out.println("comaprision, bitSetBased:rangeBased, add-> " + (t2 - t1) + ":" + (t3 - t2) + " contains->"
                       + (t4 - t3) + ":" + (t5 - t4) + " remove->" + (t6 - t5) + ":" + (t7 - t6));
    System.out.println("comaprision, bitSetBased:rangeBased, addAll-> " + (t9 - t8) + ":" + (t10 - t9) + " removeAll->"
                       + (t11 - t10) + ":" + (t12 - t11));
  }

  public Set createContinuousRangeBasedSet() {
    return new ObjectIDSet();
  }

  public Set create(final Collection c, final ObjectIDSetType objectIDSetType) {
    return new ObjectIDSet(c, objectIDSetType);
  }

  public void basicTest() {
    basicTest(100000, 100000, ObjectIDSetType.RANGE_BASED_SET);
    basicTest(500000, 100000, ObjectIDSetType.RANGE_BASED_SET);
    basicTest(100000, 1000000, ObjectIDSetType.RANGE_BASED_SET);

    basicTest(100000, 100000, ObjectIDSetType.BITSET_BASED_SET);
    basicTest(500000, 100000, ObjectIDSetType.BITSET_BASED_SET);
    basicTest(100000, 1000000, ObjectIDSetType.BITSET_BASED_SET);
  }

  public void testRemoveAll() {
    for (int i = 0; i < 10; i++) {
      timeAndTestRemoveAll();
    }

  }

  private void timeAndTestRemoveAll() {
    // HashSet expected = new HashSet();
    // HashSet big = new HashSet();
    // HashSet small = new HashSet();
    final TreeSet expected = new TreeSet();
    final TreeSet big = new TreeSet();
    final TreeSet small = new TreeSet();
    final ObjectIDSet rangeOidSet = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final ObjectIDSet bitSetOidSet = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);

    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("RemoveALL TEST : Seed for Random is " + seed);
    final Random r = new Random(seed);

    for (int i = 0; i < 1000000; i++) {
      // long l = r.nextLong();
      final long l = r.nextInt(55555555);
      final ObjectID id = new ObjectID(l);
      if (i % 2 == 0) {
        // 500,0000
        big.add(id);
      }
      if (i % 3 == 0) {
        // 333,000
        rangeOidSet.add(id);
        bitSetOidSet.add(id);
        expected.add(id);
      }
      if (i % 100 == 0) {
        small.add(id);
      }
    }

    final long t1 = System.currentTimeMillis();
    rangeOidSet.removeAll(small);
    final long t2 = System.currentTimeMillis();
    bitSetOidSet.removeAll(small);
    final long t3 = System.currentTimeMillis();
    expected.removeAll(small);
    final long t4 = System.currentTimeMillis();
    assertEquals(expected, rangeOidSet);
    assertEquals(expected, bitSetOidSet);

    final long t5 = System.currentTimeMillis();
    rangeOidSet.removeAll(big);
    final long t6 = System.currentTimeMillis();
    bitSetOidSet.removeAll(big);
    final long t7 = System.currentTimeMillis();
    expected.removeAll(big);
    final long t8 = System.currentTimeMillis();
    assertEquals(expected, rangeOidSet);
    assertEquals(expected, bitSetOidSet);

    System.err.println("Time taken for removeAll RangeObjectIDSet : BitSetObjectIDSet : HashSet : " + (t2 - t1) + " : "
                       + (t3 - t2) + " : " + (t4 - t3) + " millis  for small collection, " + (t6 - t5) + " : "
                       + (t7 - t6) + " : " + (t8 - t7) + " millis for large collection");
  }

  public void testSortedSetObjectIDSet() throws Exception {
    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("SORTED TEST : Seed for Random is " + seed);
    final Random r = new Random(seed);
    final TreeSet ts = new TreeSet();
    final SortedSet oidsRangeBased = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final SortedSet oidsBitSetBased = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    for (int i = 0; i < 10000; i++) {
      final long l = r.nextLong();
      // if (l < 0) {
      // l = -l;
      // }
      final ObjectID id = new ObjectID(l);
      final boolean b1 = ts.add(id);
      final boolean b2 = oidsRangeBased.add(id);
      final boolean b3 = oidsBitSetBased.add(id);
      assertEquals(b1, b2);
      assertEquals(b1, b3);
      assertEquals(ts.size(), oidsRangeBased.size());
      assertEquals(ts.size(), oidsBitSetBased.size());
    }

    // verify sorted
    Iterator i = ts.iterator();
    for (final Iterator j = oidsRangeBased.iterator(); j.hasNext();) {
      final ObjectID oid1 = (ObjectID) i.next();
      final ObjectID oid2 = (ObjectID) j.next();
      assertEquals(oid1, oid2);
    }

    i = ts.iterator();
    for (final Iterator j = oidsBitSetBased.iterator(); j.hasNext();) {
      final ObjectID oid1 = (ObjectID) i.next();
      final ObjectID oid2 = (ObjectID) j.next();
      assertEquals(oid1, oid2);
    }
  }

  public void basicTest(final int distRange, final int iterationCount, final ObjectIDSetType objectIDSetType) {
    final long test_start = System.currentTimeMillis();
    final Set s = new HashSet();
    final Set small = new ObjectIDSet(objectIDSetType);
    final String cname = small.getClass().getName();
    System.err.println("Running tests for " + cname + " distRange = " + distRange + " iterationCount = "
                       + iterationCount);
    assertTrue(small.isEmpty());
    assertTrue(small.size() == 0);
    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("Seed for Random is " + seed);
    final Random r = new Random(seed);
    for (int i = 0; i < iterationCount; i++) {
      final long l = r.nextInt(distRange);
      final ObjectID id = new ObjectID(l);
      s.add(id);
      small.add(id);
      assertEquals(s.size(), small.size());
    }
    final Iterator sit = small.iterator();
    final List all = new ArrayList();
    all.addAll(s);
    while (sit.hasNext()) {
      final ObjectID i = (ObjectID) sit.next();
      Assert.eval("FAILED:" + i.toString(), s.remove(i));
    }
    Assert.eval(s.size() == 0);

    // test retain all
    final Set odds = new HashSet();
    final Set evens = new HashSet();
    for (int i = 0; i < all.size(); i++) {
      if (i % 2 == 0) {
        evens.add(all.get(i));
      } else {
        odds.add(all.get(i));
      }
    }

    boolean b = small.retainAll(odds);
    assertTrue(b);
    Assert.assertEquals(odds.size(), small.size());
    assertEquals(odds, small);
    b = small.retainAll(evens);
    assertTrue(b);
    assertEquals(0, small.size());
    small.addAll(all); // back to original state

    // test new set creation (which uses cloning
    long start = System.currentTimeMillis();
    final Set copy = create(all, objectIDSetType);
    System.err.println("Time to add all IDs from a collection to a new " + cname + " = "
                       + (System.currentTimeMillis() - start) + " ms");
    start = System.currentTimeMillis();
    final Set clone = create(small, objectIDSetType);
    System.err.println("Time to add all IDs from an ObjectIDSet to a new " + cname + " = "
                       + (System.currentTimeMillis() - start) + " ms");

    Collections.shuffle(all);
    for (final Iterator i = all.iterator(); i.hasNext();) {
      final ObjectID rid = (ObjectID) i.next();
      Assert.eval(small.contains(rid));
      Assert.eval(clone.contains(rid));
      Assert.eval(copy.contains(rid));
      if (!small.remove(rid)) { throw new AssertionError("couldn't remove:" + rid); }
      if (small.contains(rid)) { throw new AssertionError(rid); }
      if (!clone.remove(rid)) { throw new AssertionError("couldn't remove:" + rid); }
      if (clone.contains(rid)) { throw new AssertionError(rid); }
      if (!copy.remove(rid)) { throw new AssertionError("couldn't remove:" + rid); }
      if (copy.contains(rid)) { throw new AssertionError(rid); }
    }
    for (final Iterator i = all.iterator(); i.hasNext();) {
      final ObjectID rid = (ObjectID) i.next();
      Assert.eval(!small.contains(rid));
      if (small.remove(rid)) { throw new AssertionError("shouldn't have removed:" + rid); }
      if (small.contains(rid)) { throw new AssertionError(rid); }
      if (clone.remove(rid)) { throw new AssertionError("shouldn't have removed:" + rid); }
      if (clone.contains(rid)) { throw new AssertionError(rid); }
      if (copy.remove(rid)) { throw new AssertionError("shouldn't have removed:" + rid); }
      if (copy.contains(rid)) { throw new AssertionError(rid); }
    }
    Assert.eval(s.size() == 0);
    Assert.eval(small.size() == 0);
    Assert.eval(copy.size() == 0);
    Assert.eval(clone.size() == 0);
    System.err.println("Time taken to run basic Test for " + small.getClass().getName() + " is "
                       + (System.currentTimeMillis() - test_start) + " ms");
  }

  public void testSerializationObjectIDSet2() throws Exception {
    for (int i = 0; i < 20; i++) {
      final Set s = createRandomSetOfObjectIDs();
      serializeAndVerify(s, ObjectIDSetType.RANGE_BASED_SET);
      serializeAndVerify(s, ObjectIDSetType.BITSET_BASED_SET);
    }
  }

  private void serializeAndVerify(final Set s, final ObjectIDSetType objectIDSetType) throws Exception {
    final ObjectIDSet org = new ObjectIDSet(s, objectIDSetType);
    assertEquals(s, org);

    final ObjectIDSet ser = serializeAndRead(org);
    assertEquals(s, ser);
    assertEquals(org, ser);
  }

  private ObjectIDSet serializeAndRead(final ObjectIDSet org) throws Exception {
    final TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    org.serializeTo(out);
    System.err.println("Written ObjectIDSet2 size : " + org.size());
    final TCByteBufferInputStream in = new TCByteBufferInputStream(out.toArray());
    final ObjectIDSet oids = new ObjectIDSet();
    oids.deserializeFrom(in);
    System.err.println("Read  ObjectIDSet2 size : " + oids.size());
    return oids;
  }

  private Set createRandomSetOfObjectIDs() {
    final Set s = new HashSet();
    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("Random Set creation : Seed for Random is " + seed);
    final Random r = new Random(seed);
    for (int i = 0; i < r.nextLong(); i++) {
      s.add(new ObjectID(r.nextLong()));
    }
    System.err.println("Created a set of size : " + s.size());
    return s;
  }

  public void testObjectIDSet() {
    basicTest();
  }

  public void testObjectIDSetDump() {
    final ObjectIDSet s1 = new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET);
    final ObjectIDSet s2 = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);
    System.err.println(" toString() : " + s1);

    for (int i = 0; i < 100; i++) {
      s1.add(new ObjectID(i));
    }
    System.err.println(" toString() : " + s1);

    for (int i = 0; i < 100; i += 2) {
      s1.remove(new ObjectID(i));
    }
    System.err.println(" toString() : " + s1);

    System.err.println(" toString() : " + s2);

    for (int i = 0; i < 100; i++) {
      s2.add(new ObjectID(i));
    }
    System.err.println(" toString() : " + s2);

    for (int i = 0; i < 100; i += 2) {
      s2.remove(new ObjectID(i));
    }
    System.err.println(" toString() : " + s2);

  }

  public void testObjectIdSetConcurrentModification() {
    concurrentModificationTest(new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET));
    concurrentModificationTest(new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET));
  }

  private void concurrentModificationTest(final ObjectIDSet objIdSet) throws AssertionError {
    int num = 0;
    for (num = 0; num < 50; num++) {
      objIdSet.add(new ObjectID(num));
    }

    Iterator iterator = objIdSet.iterator();
    objIdSet.add(new ObjectID(num));
    try {
      iterateElements(iterator);
      throw new AssertionError("We should have got the ConcurrentModificationException");
    } catch (final ConcurrentModificationException cme) {
      System.out.println("Caught Expected Exception " + cme.getClass().getName());
    }

    iterator = objIdSet.iterator();
    objIdSet.remove(new ObjectID(0));
    try {
      iterateElements(iterator);
      throw new AssertionError("We should have got the ConcurrentModificationException");
    } catch (final ConcurrentModificationException cme) {
      System.out.println("Caught Expected Exception " + cme.getClass().getName());
    }

    iterator = objIdSet.iterator();
    objIdSet.clear();
    try {
      iterateElements(iterator);
      throw new AssertionError("We should have got the ConcurrentModificationException");
    } catch (final ConcurrentModificationException cme) {
      System.out.println("Caught Expected Exception " + cme.getClass().getName());
    }
  }

  private long iterateElements(final Iterator iterator) throws ConcurrentModificationException {
    return iterateElements(iterator, -1);
  }

  private long iterateElements(final Iterator iterator, final long count) throws ConcurrentModificationException {
    long itrCount = 0;
    while ((iterator.hasNext()) && (count < 0 || itrCount < count)) {
      itrCount++;
      System.out.print(((ObjectID) iterator.next()).toLong() + ", ");
    }
    System.out.print("\n\n");
    return itrCount;
  }

  public void testObjectIDSetIteratorFullRemove() {
    oidSetIteratorFullRemoveTest(new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET));
    oidSetIteratorFullRemoveTest(new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET));
  }

  private void oidSetIteratorFullRemoveTest(final Set oidSet) {
    final Set all = new TreeSet();
    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("Running iteratorRemoveTest for " + oidSet.getClass().getName() + " and seed is " + seed);
    final Random r = new Random(seed);
    for (int i = 0; i < 5000; i++) {
      final long l = r.nextLong();
      final ObjectID id = new ObjectID(l);
      all.add(id);
      oidSet.add(id);
    }

    Assert.assertEquals(all.size(), oidSet.size());
    for (final Iterator i = all.iterator(); i.hasNext();) {
      final ObjectID rid = (ObjectID) i.next();
      Assert.eval(oidSet.contains(rid));
      for (final Iterator j = oidSet.iterator(); j.hasNext();) {
        final ObjectID crid = (ObjectID) j.next();
        if (crid.equals(rid)) {
          j.remove();
          break;
        }
      }
    }
    Assert.assertEquals(oidSet.size(), 0);
  }

  public void testObjectIDSetIteratorSparseRemove() {
    oidSetIteratorSparseRemoveTest(new ObjectIDSet(ObjectIDSetType.RANGE_BASED_SET));
    oidSetIteratorSparseRemoveTest(new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET));
  }

  private void oidSetIteratorSparseRemoveTest(final Set oidSet) {
    // TreeSet<ObjectID> ts = new TreeSet<ObjectID>();
    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("Running iteratorRemoveTest for " + oidSet.getClass().getName() + " and seed is " + seed);
    final Random r = new Random(seed);
    for (int i = 0; i < 1000; i++) {
      ObjectID id;
      do {
        final long l = r.nextLong();
        id = new ObjectID(l);
      } while (oidSet.contains(id));
      // ts.add(id);
      oidSet.add(id);
    }

    System.out.println(oidSet + "\n\n");
    // check if ObjectIDSet has been inited with 1000 elements
    Iterator oidSetIterator = oidSet.iterator();
    assertEquals(1000, iterateElements(oidSetIterator));

    long visitedCount = 0;
    long removedCount = 0;
    oidSetIterator = oidSet.iterator();

    // visit first 100 elements
    visitedCount += iterateElements(oidSetIterator, 100);
    assertEquals(100, visitedCount);

    // remove the 100th element
    oidSetIterator.remove();
    removedCount += 1;

    // visit next 100 elements
    visitedCount += iterateElements(oidSetIterator, 100);
    assertEquals(100 + 100, visitedCount);

    // remove the 200th element
    oidSetIterator.remove();
    removedCount += 1;

    // visit next 100 elements
    visitedCount += iterateElements(oidSetIterator, 100);
    assertEquals(100 + 100 + 100, visitedCount);

    // visit rest of the elements
    visitedCount += iterateElements(oidSetIterator);
    assertEquals(1000, visitedCount);

    // check the backing Set for removed elements
    oidSetIterator = oidSet.iterator();
    final long totalElements = iterateElements(oidSetIterator);
    assertEquals((visitedCount - removedCount), totalElements);
  }

  public void testObjectIDSetIteratorRemoveSpecailCases() {
    final List longList = new ArrayList();
    longList.add(new ObjectID(25));
    longList.add(new ObjectID(26));
    longList.add(new ObjectID(27));
    longList.add(new ObjectID(28));
    longList.add(new ObjectID(9));
    longList.add(new ObjectID(13));
    longList.add(new ObjectID(12));
    longList.add(new ObjectID(14));
    longList.add(new ObjectID(18));
    longList.add(new ObjectID(2));
    longList.add(new ObjectID(23));
    longList.add(new ObjectID(47));
    longList.add(new ObjectID(35));
    longList.add(new ObjectID(10));
    longList.add(new ObjectID(1));
    longList.add(new ObjectID(4));
    longList.add(new ObjectID(15));
    longList.add(new ObjectID(8));
    longList.add(new ObjectID(56));
    longList.add(new ObjectID(11));
    longList.add(new ObjectID(10));
    longList.add(new ObjectID(33));
    longList.add(new ObjectID(17));
    longList.add(new ObjectID(29));
    longList.add(new ObjectID(19));
    // Data : 1 2 4 8 9 10 11 12 13 14 15 17 18 19 23 25 26 27 28 29 33 35 47 56

    /**
     * ObjectIDSet { (oids:ranges) = 24:10 , compression ratio = 1.0 } [ Range(1,2) Range(4,4) Range(8,15) Range(17,19)
     * Range(23,23) Range(25,29) Range(33,33) Range(35,35) Range(47,47) Range(56,56)]
     */

    final int totalElements = longList.size() - 1;

    oidSetIteratorRemoveSpecialCasesTest(totalElements, new ObjectIDSet(longList, ObjectIDSetType.RANGE_BASED_SET));
    oidSetIteratorRemoveSpecialCasesTest(totalElements, new ObjectIDSet(longList, ObjectIDSetType.BITSET_BASED_SET));
  }

  private void oidSetIteratorRemoveSpecialCasesTest(final int totalElements, final Set objectIDSet)
      throws AssertionError {
    Iterator i = objectIDSet.iterator();
    assertEquals(totalElements, iterateElements(i));

    final List longSortList = new ArrayList();
    i = objectIDSet.iterator();
    while (i.hasNext()) {
      longSortList.add(i.next());
    }

    // remove first element in a range. eg: 8 from (8,15)
    removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(8)) + 1, 9);
    objectIDSet.add(new ObjectID(8)); // get back to original state

    // remove last element in a range. eg: 19 from (17,19)
    removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(19)) + 1, 23);
    objectIDSet.add(new ObjectID(19));

    // remove the only element in the range. eg: 33 from (33,33)
    removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(33)) + 1, 35);
    objectIDSet.add(new ObjectID(33));

    // remove the least element
    removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(1)) + 1, 2);
    objectIDSet.add(new ObjectID(1));

    // remove the max element; element will be removed, but while going to next element, exception expected
    try {
      removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(56)) + 1, -99);
      throw new AssertionError("Expected to throw an exception");
    } catch (final NoSuchElementException noSE) {
      // expected
    } finally {
      objectIDSet.add(new ObjectID(56));
    }

    // remove the non existing element; exception expected
    try {
      removeElementFromIterator(objectIDSet.iterator(), totalElements, longSortList.indexOf(new ObjectID(16)) + 1, -99);
      throw new AssertionError("Expected to throw an exception");
    } catch (final IllegalStateException ise) {
      // expected
    }

    i = objectIDSet.iterator();
    assertEquals(5, iterateElements(i, 5));
    objectIDSet.add(new ObjectID(99));
    try {
      assertEquals(5, iterateElements(i, 1));
      throw new AssertionError("Expected to throw an exception");
    } catch (final ConcurrentModificationException cme) {
      // expected
    } finally {
      objectIDSet.remove(new ObjectID(99));
    }
  }

  public void testAddAll() {
    internalTestAddAll(ObjectIDSetType.BITSET_BASED_SET);
    internalTestAddAll(ObjectIDSetType.RANGE_BASED_SET);
  }

  private void internalTestAddAll(ObjectIDSetType type) {
    final int SIZE_MILLION = 1000000;
    final ObjectIDSet set = new ObjectIDSet(type);

    // validate addAll
    addToReferencesRandom(set, SIZE_MILLION);
    int randomSize = set.size();

    final ObjectIDSet set2 = new ObjectIDSet(type);

    long startTime = System.currentTimeMillis();
    set2.addAll(set);
    long addAllTime = System.currentTimeMillis() - startTime;

    System.out.println(type + "Set.addAll random total time took: " + addAllTime + " ms. ");

    // validate addAll
    assertEquals(randomSize, set2.size());

    for (final ObjectID id : set) {
      assertTrue(set2.contains(id));
    }

    // /do serial
    final ObjectIDSet setSerial = new ObjectIDSet(type);
    addToReferencesSerial(setSerial, SIZE_MILLION);

    assertEquals(SIZE_MILLION, setSerial.size());

    startTime = System.currentTimeMillis();
    set2.addAll(setSerial);
    addAllTime = System.currentTimeMillis() - startTime;

    System.out.println(type + "Set.addAll serial total time took: " + addAllTime + " ms. ");

    // validate addAll
    assertEquals(randomSize + SIZE_MILLION, set2.size());

    for (final ObjectID id : setSerial) {
      assertTrue(set2.contains(id));
    }

    // now lets add to serial, and see if the random set exist in it
    setSerial.addAll(set);

    for (ObjectID id : set) {
      assertTrue(setSerial.contains(id));
    }

  }

  public void testAddAllPerformance() {
    internalAddAllPerformance(ObjectIDSetType.BITSET_BASED_SET);
    internalAddAllPerformance(ObjectIDSetType.RANGE_BASED_SET);
  }

  private void internalAddAllPerformance(ObjectIDSetType type) {
    final int SIZE_10_MILLION = 10000000;
    final ObjectIDSet set = new ObjectIDSet(type);
    final ObjectIDSet set2 = new ObjectIDSet(type);
    addToReferencesRandom(set, SIZE_10_MILLION);
    addToReferencesSerial(set2, SIZE_10_MILLION);
    int bitSize = set.size();
    int bitSize2 = set2.size();

    long startTime = System.currentTimeMillis();
    set.addAll(set2);
    long addAllTime = System.currentTimeMillis() - startTime;

    System.out.println(type + "Set.addAll performance random total time took: " + addAllTime + " ms. ");
    assertEquals(bitSize + bitSize2, set.size());
  }

  private void addToReferencesSerial(ObjectIDSet set, int size) {
    for (int i = 2 * size; i < size + (2 * size); i++) {
      set.add(new ObjectID(i));
    }
  }

  private void addToReferencesRandom(final ObjectIDSet set, final int size) {
    final SecureRandom sr = new SecureRandom();
    final long seed = sr.nextLong();
    System.err.println("testContain : Seed for Random is " + seed);
    final Random r = new Random(seed);

    for (int i = 0; i < size; i++) {
      set.add(new ObjectID(r.nextInt(size)));
    }
  }

  private void removeElementFromIterator(final Iterator i, final int totalElements, final long indexOfRemoveElement,
                                         final int nextExpectedElement) {
    long visitedElements = 0;
    visitedElements += iterateElements(i, indexOfRemoveElement);
    i.remove();
    assertEquals(nextExpectedElement, ((ObjectID) i.next()).toLong());
    visitedElements += iterateElements(i);
    assertEquals(visitedElements, totalElements - 1);
  }
}
