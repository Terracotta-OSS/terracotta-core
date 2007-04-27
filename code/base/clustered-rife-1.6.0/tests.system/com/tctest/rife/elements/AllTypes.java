package com.tctest.rife.elements;

import com.uwyn.rife.engine.Element;
import com.uwyn.rife.engine.annotations.Elem;
import com.uwyn.rife.tools.StringUtils;

@Elem
public class AllTypes extends Element {
	public final static String BEFORE = "before pause";

	private static int			sInt1 = 1;
	private static String		sString1 = "static ok";
	private static Long[]		sLongs1 = new Long[] {new Long(9111L), new Long(9333L)};
	private static int[][]	sMultiInts1 = new int[][] {{1, 2, 3, 4}, {5, 6, 7, 8}, {9, 10, 11, 12}};
	private static long			sLong1 = 3L;

	private static int			sInt2 = 0;
	private static String		sString2 = null;
	private static Long[]		sLongs2 = null;
	private static int[][]	sMultiInts2 = null;
	private static long			sLong2 = 0L;

	private int			mInt1 = 2;
	private String	mString1 = "member ok";
	private Long[]	mLongs1 = new Long[] {new Long(8111L), new Long(8333L)};
	private int[][]	mMultiInts1 = new int[][] {{31, 32, 33, 34}, {35, 36, 37, 38}};
	private long		mLong1 = 4L;

	private int			mInt2 = 0;
	private String	mString2 = null;
	private Long[]	mLongs2 = null;
	private int[][]	mMultiInts2 = null;
	private long		mLong2 = 0L;

	public int getInt1() {
		int int1 = 9;
		long long2;
		long2 = int1 + 1;
		return (int) long2 - 2;
	}

	public String createString(String first, String second, long number) {
		return first + " " + second + " " + number;
	}

	public String[] createArrayString(int size) {
		return new String[size];
	}

	public String[][] createMultiArrayString(int size1, int size2) {
		String[][] result = new String[size1][size2];

		return fillMultiArrayString(result);
	}

	public String[][] fillMultiArrayString(String[][] array) {
		int size1 = array.length;
		int size2 = array[0].length;

		for (int i = 0; i < size1; i++) {
			for (int j = 0; j < size2; j++) {
				array[i][j] = "str " + i + " " + j;
			}
		}

		return array;
	}

	public long[] createArrayLong(int size) {
		return new long[size];
	}

	public long[][] createMultiArrayLong(int size1, int size2) {
		long[][] result = new long[size1][size2];

		return fillMultiArrayLong(result);
	}

	public long[][] fillMultiArrayLong(long[][] result) {
		int size1 = result.length;
		int size2 = result[0].length;

		for (int i = 0; i < size1; i++) {
			for (int j = 0; j < size2; j++) {
				result[i][j] = i * 100 + j;
			}
		}

		return result;
	}

	public int[][][] createMultiArrayInt(int size1, int size2, int size3) {
		int[][][] result = new int[size1][size2][size3];

		return fillMultiArrayInt(result);
	}

	public int[][][] fillMultiArrayInt(int[][][] result) {
		int size1 = result.length;
		int size2 = result[0].length;
		int size3 = result[0][0].length;

		for (int i = 0; i < size1; i++) {
			for (int j = 0; j < size2; j++) {
				for (int k = 0; k < size3; k++) {
					result[i][j][k] = i * 100 + j * 10 + k;
				}
			}
		}

		return result;
	}

	public Integer[][][] createMultiArrayInteger(int size1, int size2, int size3) {
		Integer[][][] result = new Integer[size1][size2][size3];

		return fillMultiArrayInteger(result);
	}

	public Integer[][][] fillMultiArrayInteger(Integer[][][] result) {
		int size1 = result.length;
		int size2 = result[0].length;
		int size3 = result[0][0].length;

		for (int i = 0; i < size1; i++) {
			for (int j = 0; j < size2; j++) {
				for (int k = 0; k < size3; k++) {
					result[i][j][k] = new Integer(i * 100 + j * 10 + k);
				}
			}
		}

		return result;
	}

	public void processElement() {
		int int1 = 0;
		int1 = 1209967;
		int1 += 10;
		int1++;
		int1--;
		int int2 = int1;
		int2 -= 977;
		int1 = getInt1();
		long long1 = Long.MAX_VALUE;
		int int3 = 33 / 3;
		String ref1 = new String("local ok");
		int int4 = int1 * 2;
		int int5 = ref1.indexOf('k');
		int int6 = -1;
		int6 += 9;
		{
			long long2 = 1L;
			assert long2 != 0;
			long long3 = 2L;
			assert long3 != 0;
			long long4 = 3L;
			assert long4 != 0;
			long long5 = 4L;
			assert long5 != 0;
			long long6 = 5L;
			assert long6 != 0;
		}
		long long2 = 0L;
		long long3 = long2 + long1 - 98L;
		long long4 = long3 / 10L;
		long long5 = int1;
		long long6 = -1L;
		{
			float float2 = 1f;
			assert float2 != 0;
			float float3 = 2f;
			assert float3 != 0;
			float float4 = 3f;
			assert float4 != 0;
			float float5 = 4f;
			assert float5 != 0;
			float float6 = 5f;
			assert float6 != 0;
		}
		float float1 = 0.4f;
		float float2 = float1 + int1;
		{
			double double2 = 1d;
			assert double2 != 0;
			double double4 = 1d;
			assert double4 != 0;
			double double5 = 1d;
			assert double5 != 0;
			double double6 = 1d;
			assert double6 != 0;
			double double7 = 1d;
			assert double7 != 0;
		}
		double double1 = 2389.98d;
		double double2 = double1 + 9.4f + int6;
		float float3 = long6 * 80 - float1;
		float float4 = float1 + float3;
		float float5 = 0f;
		float float6 = -1f;
		double double3 = float4 / 8;
		double double4 = -1d;
		double double5 = -0d;
		double double6 = double3 + double2;

		StringBuffer stringbuffer1 = new StringBuffer(createString("some", "value", 6899L));

		while (int1 < 40) {
			int int7 = 2;
			assert int7 != 0;
			Long long_obj1 = new Long(43L);
			assert long_obj1 != null;
			print(BEFORE + " while " + int1 + "\n" + getContinuationId());
			pause();
			int1++;
		}

		Object[] longs1 = new Long[] { new Long(111111L), new Long(555555L), null, new Long(999999) };
		Long[] longs2 = (Long[]) longs1;
		longs2[1] = new Long(444444L);
		longs2[2] = new Long(666666L);

		long[] longs3 = new long[] { 333L, 8888L, 99L };
		long[] longs4 = new long[longs3.length];
		System.arraycopy(longs3, 0, longs4, 0, longs3.length);
		longs4[1] = 66L;
		long long7 = longs3[2];

		Object[] strings1 = createArrayString(4);
		strings1[0] = "zero";
		strings1[2] = "two";
		String[] strings2 = (String[]) strings1;
		strings2[1] = "one";
		String[] strings3 = new String[] { "ini", "mini", "moo" };

		Object[][] strings4 = createMultiArrayString(3, 4);
		String[][] strings5 = (String[][]) strings4;
		strings5[0][1] = "replaced";
		String[][] strings6 = new String[2][2];
		fillMultiArrayString(strings6);

		long[] longs5 = createArrayLong(2);
		longs5[0] = -98;
		long[] longs6 = (long[]) longs5;
		longs6[1] = 97;
		long[] longs7 = new long[] { 98L, 23L, 11L };
		longs7[0] = -longs5[0];

		long[][] longs8 = createMultiArrayLong(2, 5);
		long[][] longs9 = (long[][]) longs8;
		longs9[1][3] = -89L;
		long[][] longs10 = new long[3][3];
		fillMultiArrayLong(longs10);

		int field_int = mInt1;
		long field_long = mLong1;
		String field_string = mString1;
		Long[] field_longs = mLongs1;
		int[][] field_multiints = mMultiInts1;

		int static_int = sInt1;
		long static_long = sLong1;
		String static_string = sString1;
		Long[] static_longs = sLongs1;
		int[][] static_multiints = sMultiInts1;

		mInt2 = field_int * 50;
		mLong2 = field_long * 100;
		mString2 = field_string + " two";
		mLongs2 = new Long[] { field_longs[1], field_longs[0], new Long(23687L) };
		mMultiInts2 = new int[][] { field_multiints[1] };

		sInt2 = static_int * 60;
		sString2 = static_string + " two";
		sLongs2 = new Long[] { new Long(23476L), static_longs[1], static_longs[0], new Long(8334L) };
		sMultiInts2 = new int[][] { static_multiints[2], static_multiints[0] };
		sLong2 = static_long * 200;

		print(BEFORE + " a\n" + getContinuationId());
		pause();

		Object[][][] ints1 = createMultiArrayInteger(2, 3, 8);
		Integer[][][] ints2 = (Integer[][][]) ints1;
		ints2[1][2][4] = new Integer(-99);
		ints2[1][2][5] = null;

		int[][] ints3 = new int[][] { { 1, 3 }, { 5, 7 }, { 11, 13 }, { 17, 19 } };
		int[][] ints4 = (int[][]) ints3;
		ints3[2][1] = -199;

		boolean[] booleans1 = new boolean[] { true, false, false };
		char[] chars1 = new char[] { 'K', 'O' };
		float[] floats1 = new float[] { 54.7f, 9.8f };
		double[] doubles1 = new double[] { 82324.45d, 997823.23d, 87.8998d };
		byte[] bytes1 = new byte[] { (byte) 98, (byte) 12 };
		short[] shorts1 = new short[] { (short) 8, (short) 11 };

		String string1 = stringbuffer1.substring(2);

		print(BEFORE + " b\n" + getContinuationId());
		pause();

		int int7 = Integer.MAX_VALUE;
		int int8 = (89 / 8) + 2 * 7;
		int int9 = "KHKJ".length();
		int int10 = (int1 + int2) / int3;
		int int11 = 134 - int4;
		int int12 = Integer.MIN_VALUE;

		print(BEFORE + " c\n" + getContinuationId());
		pause();

		print(int1 + "," + int2 + "," + int3 + "," + int4 + "," + int5 + "," + int6 + ",\n" + long1 + "," + long2 + ","
				+ long3 + "," + long4 + "," + long5 + "," + long6 + "," + long7 + ",\n" + float1 + "," + float2 + "," + float3
				+ "," + float4 + "," + float5 + "," + float6 + ",\n" + double1 + "," + double2 + "," + double3 + "," + double4
				+ "," + double5 + "," + double6 + ",\n" + ref1 + "," + stringbuffer1 + ",\n" + StringUtils.join(booleans1, "|")
				+ "," + StringUtils.join(chars1, "|") + "," + StringUtils.join(floats1, "|") + ","
				+ StringUtils.join(doubles1, "|") + "," + StringUtils.join(bytes1, "|") + "," + StringUtils.join(shorts1, "|")
				+ ",\n" + StringUtils.join(longs1, "|") + "," + StringUtils.join(longs2, "|") + ","
				+ StringUtils.join(longs3, "|") + "," + StringUtils.join(longs4, "|") + ",\n" + StringUtils.join(strings1, "|")
				+ "," + StringUtils.join(strings2, "|") + "," + StringUtils.join(strings3, "|") + ",\n" + strings4.length + ":"
				+ StringUtils.join(strings4[0], "|") + "||" + StringUtils.join(strings4[1], "|") + "||"
				+ StringUtils.join(strings4[2], "|") + ",\n" + strings5.length + ":" + StringUtils.join(strings5[0], "|")
				+ "||" + StringUtils.join(strings5[1], "|") + "||" + StringUtils.join(strings5[2], "|") + ",\n"
				+ strings6.length + ":" + StringUtils.join(strings6[0], "|") + "||" + StringUtils.join(strings6[1], "|")
				+ ",\n" + StringUtils.join(longs5, "|") + "," + StringUtils.join(longs6, "|") + ","
				+ StringUtils.join(longs7, "|") + ",\n" + longs8.length + ":" + StringUtils.join(longs8[0], "|") + "||"
				+ StringUtils.join(longs8[1], "|") + ",\n" + longs9.length + ":" + StringUtils.join(longs9[0], "|") + "||"
				+ StringUtils.join(longs9[1], "|") + ",\n" + longs10.length + ":" + StringUtils.join(longs10[0], "|") + "||"
				+ StringUtils.join(longs10[1], "|") + "||" + StringUtils.join(longs10[2], "|") + ",\n" + field_int + ","
				+ field_long + "," + field_string + "," + StringUtils.join(field_longs, "|") + "," + field_multiints.length
				+ ":" + StringUtils.join(field_multiints[0], "|") + "||" + StringUtils.join(field_multiints[1], "|") + ",\n"
				+ static_int + "," + static_long + "," + static_string + "," + StringUtils.join(static_longs, "|") + ","
				+ static_multiints.length + ":" + StringUtils.join(static_multiints[0], "|") + "||"
				+ StringUtils.join(static_multiints[1], "|") + "||" + StringUtils.join(static_multiints[2], "|") + ",\n"
				+ mInt1 + "," + mLong1 + "," + mString1 + "," + StringUtils.join(mLongs1, "|") + "," + mMultiInts1.length + ":"
				+ StringUtils.join(mMultiInts1[0], "|") + "||" + StringUtils.join(mMultiInts1[1], "|") + ",\n" + sInt1 + ","
				+ sLong1 + "," + sString1 + "," + StringUtils.join(sLongs1, "|") + "," + sMultiInts1.length + ":"
				+ StringUtils.join(sMultiInts1[0], "|") + "||" + StringUtils.join(sMultiInts1[1], "|") + "||"
				+ StringUtils.join(sMultiInts1[2], "|") + ",\n" + mInt2 + "," + mLong2 + "," + mString2 + ","
				+ StringUtils.join(mLongs2, "|") + "," + mMultiInts2.length + ":" + StringUtils.join(mMultiInts2[0], "|")
				+ ",\n" + sInt2 + "," + sLong2 + "," + sString2 + "," + StringUtils.join(sLongs2, "|") + ","
				+ sMultiInts2.length + ":" + StringUtils.join(sMultiInts2[0], "|") + "||"
				+ StringUtils.join(sMultiInts2[1], "|") + ",\n" + ints1.length + ":" + ints1[0].length + ":" + ints1[1].length
				+ ":" + StringUtils.join(ints1[0][0], "|") + "||" + StringUtils.join(ints1[0][1], "|") + "||"
				+ StringUtils.join(ints1[0][2], "|") + "|||" + StringUtils.join(ints1[1][0], "|") + "||"
				+ StringUtils.join(ints1[1][1], "|") + "||" + StringUtils.join(ints1[1][2], "|") + ",\n" + ints2.length + ":"
				+ ints2[0].length + ":" + ints2[1].length + ":" + StringUtils.join(ints2[0][0], "|") + "||"
				+ StringUtils.join(ints2[0][1], "|") + "||" + StringUtils.join(ints2[0][2], "|") + "|||"
				+ StringUtils.join(ints2[1][0], "|") + "||" + StringUtils.join(ints2[1][1], "|") + "||"
				+ StringUtils.join(ints2[1][2], "|") + ",\n" + ints3.length + ":" + StringUtils.join(ints3[0], "|") + "||"
				+ StringUtils.join(ints3[1], "|") + "||" + StringUtils.join(ints3[2], "|") + "||"
				+ StringUtils.join(ints3[3], "|") + ",\n" + ints4.length + ":" + StringUtils.join(ints4[0], "|") + "||"
				+ StringUtils.join(ints4[1], "|") + "||" + StringUtils.join(ints4[2], "|") + "||"
				+ StringUtils.join(ints4[3], "|") + ",\n" + string1 + ",\n" + int7 + "," + int8 + "," + int9 + "," + int10
				+ "," + int11 + "," + int12);
	}
}
