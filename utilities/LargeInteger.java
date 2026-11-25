package com.AtomCAS.utilities;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import jdk.internal.util.*;
import jdk.internal.vm.annotation.*;

/**
 * This is an attempt at a big-integer class that supports standard integer operations such as
 * addition, subtraction, multiplication, division/remainder, modulus, powers, modular powers, etc.
 * It also "supports" factorization and divisor calculation; it is recommended <b>not</b> to treat these
 * methods with the same freedom as the rest, unless you want to say goodbye to your computing device.
 */
public final class LargeInteger extends ScalarAtom {
    //--- Fields ---//
    /**
     * An array the represents the magnitude of this integer. It stores little-endian limbs
     * where one "digit" is in base 2^64. Any value that fits in a <code>long</code>,
     * negative or positive, is a valid limb. Empty arrays are invalid, so 0 is represented as {0L}.
     */
    private final long[] limbs;
    /**
     * The sign of the integer. "True" means that the number is negative, "false"
     * otherwise. For 0, since it is nonnegative, this field is set as <code>false</code>.
     */
    private final boolean negative;

    /**
     * One plus the bitlength of this integer.
     */
    @Stable
    private long bitLengthPlusOne;

    /**
     * One plus the bit <i>count</i> of this integer.
     */
    @Stable
    private long bitCountPlusOne;

    //--- Static constants ---//

    /**
     * Magnitude representation of 0, used in {@link #ZERO}.
     */
    private static final long[] ZERO_MAG = {0L};

    /*
    These two are intended to check if an array is "zero," meaning every element is 0 or the array is empty.
     */
    private static boolean isZeroMagnitude(long[] a) {return effectiveLength(a) == 1 && a[0] == 0L;}
    private static boolean isZeroArray(long[] a) {
        for (long e: a)
            if (e != 0)
                return false;
        return true;
    }
    /**
     * Magnitude representation of 1. Although this is not used in {@link #ONE}, it is still used many times
     * elsewhere.
     */
    private static final long[] ONE_MAG = {1L};
    private static boolean isOneMagnitude(long[] x) {return effectiveLength(x) == 1 && x[0] == 1;}
    private static final long[] TWO_MAG = {2L};
    private static final long[] FOUR_MAG = {4L};
    private static final long[] FIVE_MAG = {5L};
    private static final long[] SIX_MAG = {6L};
    private static final long[] TEN_MAG = {10L};
    /**
     * The LargeInteger constant -1.
     */
    public static final LargeInteger NEGATIVE_ONE;
    /**
     * The LargeInteger constant 0.
     */
    public static final LargeInteger ZERO = new LargeInteger(ZERO_MAG);
    /**
     * The LargeInteger constant 1.
     */
    public static final LargeInteger ONE;
    /**
     * The LargeInteger constant 2.
     */
    public static final LargeInteger TWO;
    /**
     * The LargeInteger constant 3.
     */
    public static final LargeInteger THREE;
    /**
     * The LargeInteger constant 4.
     */
    public static final LargeInteger FOUR;
    /**
     * The LargeInteger constant 5.
     */
    public static final LargeInteger FIVE;
    /**
     * The LargeInteger constant 6.
     */
    public static final LargeInteger SIX;
    /**
     * The LargeInteger constant 7.
     */
    public static final LargeInteger SEVEN;
    /**
     * The LargeInteger constant 8.
     */
    public static final LargeInteger EIGHT;
    /**
     * The LargeInteger constant 9.
     */
    public static final LargeInteger NINE;
    /**
     * The LargeInteger constant 10.
     */
    public static final LargeInteger TEN;
    /**
     * The LargeInteger constant 100.
     */
    public static final LargeInteger HUNDRED = new LargeInteger(new long[]{100L});
    /**
     * The LargeInteger constant 1000.
     */
    public static final LargeInteger THOUSAND = new LargeInteger(new long[]{1000L});

    //--- Constructors and static factories ---//

    /**
     * The default constructor for this class; converts a long[] array with a signum into a LargeInteger.
     * @param limbs The array representing the limbs
     * @param sign  The field saying if the number is negative
     */
    public LargeInteger(long[] limbs, boolean sign) {this(limbs, sign, false);}

    /**
     * Private constructor for converting a long[] array into a LargeInteger.
     * Unlike its public cousin, this assumes a normalized magnitude.
     * @param limbs The limb array used for construction.
     */
    private LargeInteger(long[] limbs) {this(limbs, false, true);}

    private LargeInteger(long[] magArray, boolean sign, boolean trimmed) {
        if (magArray == null) throw new NullPointerException("Null inputs not permitted");
        if (magArray.length == 0)
            throw new IllegalArgumentException("limbs must be nonempty");
        if (!trimmed) limbs = trimLeadingZerosUntrusted(magArray);
        else limbs = magArray;
        negative = sign && !isZeroMagnitude(magArray);
    }

    /*
    Constants -30 to -1 and 1 to 30 are cached for usage in valueOf
     */
    private static final int CACHE_COUNT = 31;
    private static final LargeInteger[] POS_SMALL = new LargeInteger[CACHE_COUNT];
    private static final LargeInteger[] NEG_SMALL = new LargeInteger[CACHE_COUNT];

    static {
        for (int x = 1; x < CACHE_COUNT; x++) {
            long[] mag = {x};
            POS_SMALL[x] = new LargeInteger(mag);
            NEG_SMALL[x] = new LargeInteger(mag, true, true);
        }
        NEGATIVE_ONE = NEG_SMALL[1];
        ONE = POS_SMALL[1];
        TWO = POS_SMALL[2];
        THREE = POS_SMALL[3];
        FOUR = POS_SMALL[4];
        FIVE = POS_SMALL[5];
        SIX = POS_SMALL[6];
        SEVEN = POS_SMALL[7];
        EIGHT = POS_SMALL[8];
        NINE = POS_SMALL[9];
        TEN = POS_SMALL[10];
    }

    /**
     * Returns the LargeInteger representation of the input(signed). If x falls within the range
     * (-{@link #CACHE_COUNT}, {@link #CACHE_COUNT}), or x = 100 or x = 1000, then a cached value is returned.
     * Otherwise, a new integer is constructed.
     * @param x The long value converted
     * @return This class's representation of x
     */
    public static LargeInteger valueOf(long x) {
        if (x == 0) return ZERO;
        else if (x > 0 && x < CACHE_COUNT) return POS_SMALL[(int)x];
        else if (x < 0 && x > -CACHE_COUNT) return NEG_SMALL[(int)-x];
        else if (x == 100) return HUNDRED;
        else if (x == 1000) return THOUSAND;
        boolean negative = x < 0;
        return new LargeInteger(new long[]{negative ? -x : x}, negative, true);
    }
    /**
     * Converts this LargeInteger to a <code>byte</code>, assuming the LargeInteger fits into the primitive.
     */
    @SuppressWarnings("unused")
    public byte byteValue() {
        short v = shortValue();
        if (v < Byte.MIN_VALUE || v > Byte.MAX_VALUE)
            throw new ArithmeticException("Does not fit in byte");
        return (byte) v;
    }
    /**
     * Converts this LargeInteger to a <code>short</code>, assuming the LargeInteger fits into the primitive.
     */
    public short shortValue() {
        int v = intValue();
        if (v < Short.MIN_VALUE || v > Short.MAX_VALUE)
            throw new ArithmeticException("Does not fit in short or byte");
        return (short) v;
    }
    /**
     * Converts this LargeInteger to an <code>int</code>, assuming the LargeInteger fits into the primitive.
     */
    public int intValue() {
        long v = longValue();
        if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE)
            throw new ArithmeticException("Does not fit in int, short or byte");
        return (int) v;
    }
    /**
     * Converts this LargeInteger to a <code>long</code>, assuming the LargeInteger fits into the primitive.
     */
    public long longValue() {
        if (limbs.length > 1)
            throw new ArithmeticException("Does not fit in long, int, short or byte");
        long value = limbs[0];
        if (value < 0) {
            if (isNegative() && value == Long.MIN_VALUE)
                return Long.MIN_VALUE;
            throw new ArithmeticException("Does not fit in long, int, short or byte");
        }
        return isNegative() ? -value : value;
    }
    /**
     * Constructor that takes an expression string. If the expression is formatted as an integer, this class decodes
     * the representation into its internal format, otherwise it delegates to the Parser class (which eventually
     * recurses into this constructor).
     * @param s The string inputted into the constructor
     * @throws IllegalArgumentException if the inputted string is an expression that does not evaluate to an integer
     */
    public LargeInteger(String s) {
        if (s == null)
            throw new NullPointerException("Null inputs not permitted");
        s = s.trim();
        if (s.isEmpty())
            throw new NumberFormatException("Empty string");
        if (!s.matches("-?\\d+")) {
            ScalarAtom R = Parser.parse(s);
            if (R instanceof LargeInteger I) {
                limbs = I.limbs;
                negative = I.negative;
                return;
            }
            R = Parser.parse(R.toString());
            if (R instanceof LargeInteger I) {
                limbs = I.limbs;
                negative = I.negative;
                return;
            } else throw new IllegalArgumentException("Inputted expression does not evaluate to an integer");
        }
        boolean neg = false;
        int pos = 0;
        if (s.charAt(0) == '-') {
            neg = true;
            pos = 1;
        }
        if (pos == s.length())
            throw new NumberFormatException("Only negative is present");
        while (pos < s.length() && s.charAt(pos) == '0') pos++;
        if (pos == s.length()) {
            limbs = ZERO_MAG;
            negative = false;
            return;
        }
        long[] acc = ZERO_MAG;
        int firstLen = (s.length() - pos) % 18;
        if (firstLen == 0) firstLen = 18;
        int end = pos + firstLen;
        acc = addSmall(multiplySmall(acc, 1_000_000_000_000_000_000L), parseDecimalChunk(s, pos, end));
        pos = end;
        while (pos < s.length()) {
            end = pos + 18;
            acc = addSmall(multiplySmall(acc, 1_000_000_000_000_000_000L), parseDecimalChunk(s, pos, end));
            pos = end;
        }
        limbs = acc;
        negative = neg && !isZero();
    }
    private static long parseDecimalChunk(String s, int start, int end) {
        long x = 0L;
        for (int i = start; i < end; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9')
                throw new NumberFormatException("Invalid digit: " + c);
            x = x * 10 + (c - '0');
        }
        return x;
    }

    /**
     * Returns the effective length of an unsigned integer magnitude
     * without modifying or copying its underlying limb array.
     *
     * <p>
     * The effective length excludes redundant most-significant zero
     * limbs, while retaining at least one limb to represent zero.
     * This allows operations to treat magnitudes as normalized
     * without physically trimming their arrays.
     * </p>
     *
     * <p>
     * By tracking the significant portion of an existing array,
     * this method avoids the need to create a separate
     * {@code MutableLargeInteger} object solely to represent
     * an adjusted magnitude length.
     * </p>
     *
     * @param mag the magnitude represented as a little-endian
     *            array of unsigned 64-bit limbs
     * @return the number of limbs required to represent the
     *         magnitude, with a minimum of one for nonempty arrays
     */
    private static int effectiveLength(long[] mag) {
        int len = mag.length;
        while (len > 1 && mag[len - 1] == 0) len--;
        return len;
    }
    private static long[] trimLeadingZerosTrusted(long[] a) {
        int n = effectiveLength(a);
        if (n == a.length) return a;
        long[] out = new long[n];
        System.arraycopy(a, 0, out, 0, n);
        return out;
    }
    /**
     * This method differs from {@link #trimLeadingZerosTrusted(long[])} such that it <i>always</i>
     * copies the input.
     * @param a The array copied
     * @return A new array with leading zeros stripped
     */
    private static long[] trimLeadingZerosUntrusted(long[] a) {return Arrays.copyOf(a, effectiveLength(a));}

    //--- Bit counting ---//

    private static long bitLength(long[] x) {
        int top = effectiveLength(x)-1;
        if (top == 0 && x[0] == 0L) return 0;
        return top * 64L + 64L - Long.numberOfLeadingZeros(x[top]);
    }
    /**
     * Computes the bitlength of this integer.
     * @return Number of bits in the integer up to the leading one
     */
    public long bitLength() {
        if (bitLengthPlusOne == 0)
            bitLengthPlusOne = bitLength(limbs) + 1;
        return bitLengthPlusOne - 1;
    }
    /**
     * Computes the number of bits that are set in this integer.
     * @return Total count of set bits
     */
    @SuppressWarnings("unused")
    public long bitCount() {
        if (bitCountPlusOne == 0) {
            for (long limb: limbs)
                bitCountPlusOne += Long.bitCount(limb);
            return bitCountPlusOne++ - 1;
        } else return bitCountPlusOne - 1;
    }

    //--- Comparisons and equality ---//

    /**
     * Compares two nonnegative integer magnitudes represented as
     * little-endian arrays of unsigned 64-bit limbs.
     *
     * <p>
     * This method compares the numerical values of the magnitudes,
     * rather than their raw array lengths or signed Java
     * {@code long} values. It is used by arithmetic operations
     * such as {@link #compareMagnitudes(ScalarAtom)} and
     * {@link #add(LargeInteger)}.
     * </p>
     *
     * <h3>Comparison Procedure</h3>
     * <ol>
     *   <li>
     *     <b>Determine effective lengths:</b>
     *     Uses {@link #effectiveLength(long[])} to ignore redundant
     *     most-significant zero limbs without modifying either array.
     *   </li>
     *   <li>
     *     <b>Compare lengths:</b>
     *     If the effective lengths differ, the magnitude with
     *     more significant limbs is considered larger.
     *   </li>
     *   <li>
     *     <b>Compare individual limbs:</b>
     *     If the effective lengths are equal, compares limbs
     *     from most significant to least significant using
     *     {@code Long.compareUnsigned}.
     *   </li>
     *   <li>
     *     <b>Determine equality:</b>
     *     If every significant limb matches, the magnitudes
     *     are numerically equal, even if their array lengths differ.
     *   </li>
     * </ol>
     *
     * <h3>Complexity</h3>
     * <ul>
     *   <li>
     *     <b>Time:</b> O(max(n, m)) in the worst case, where
     *     n and m are the input array lengths.
     *   </li>
     *   <li>
     *     <b>Auxiliary space:</b> O(1).
     *   </li>
     * </ul>
     *
     * <p>
     * Neither input array is modified nor copied.
     * Both arrays must be nonempty.
     * </p>
     *
     * @param a the first unsigned integer magnitude
     * @param b the second unsigned integer magnitude
     * @return a negative integer if {@code a < b},
     *         zero if {@code a == b}, or a positive integer
     *         if {@code a > b}, comparing numerical magnitudes
     *         rather than signed limb values
     */
    private static int compareArray(long[] a, long[] b) {
        if (a == b) return 0;
        int aLen = effectiveLength(a), bLen = effectiveLength(b);
        if (aLen != bLen) return aLen > bLen ? 1 : -1;
        for (int i = aLen - 1; i >= 0; i--) {
            int cmp = Long.compareUnsigned(a[i], b[i]);
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    /**
     * In some cases, one will need to compare a long[] with a regular unsigned long.
     */
    private static int compareSmall(long[] a, long b)
    {return effectiveLength(a) > 1 ? 1 : Long.compareUnsigned(a[0], b);}
    @Override
    int compareMagnitudes(ScalarAtom other) {
        return switch (other) {
            case LargeInteger I -> compareArray(limbs, I.limbs);
            case LargeFraction F -> multiply(F.denominator).compareMagnitudes(F.numerator);
            case LargePowerMonomial L -> {
                LargeFraction f = power(L.exponent.denominator);
                LargeFraction o = L.power(L.exponent.denominator).base;
                yield f.compareMagnitudes(o);
            } case RadicalTerm R -> {
                if (other.isComplex()) {
                    RadicalTermSum RS = (RadicalTermSum) other.multiply(R.complexConjugate());
                    yield square().compareMagnitudes(RS);
                }
                if (equals(R.rationalPart) && R.coefficient.isZero()) yield 0;
                LargeFraction f = (LargeFraction) subtract(R.rationalPart).divide(R.coefficient);
                f = f.power(R.exponent.exponent.denominator);
                LargeFraction o = R.exponent.base;
                yield f.compareMagnitudes(o);
            } default -> -other.compareMagnitudes(this);
        };
    }
    @Override
    public int compareTo(ScalarAtom other) {
        if (other.isComplex()) throw new IllegalStateException("Complex numbers cannot be compared");
        return switch (other) {
            case LargeInteger I -> negative == I.negative
                    ? (isNegative() ? compareArray(I.limbs, limbs) : compareArray(limbs, I.limbs))
                    : (isNegative() ? -1 : 1);
            case LargeFraction F -> multiply(F.denominator).compareTo(F.numerator);
            case LargePowerMonomial LPM -> {
                LargeFraction f = power(LPM.exponent.denominator);
                LargeFraction o = LPM.power(LPM.exponent.denominator).base;
                yield f.compareTo(o);
            } default -> {
                int res = compareMagnitudes(other);
                yield switch (res) {
                    case 1 -> isNegative() ? -1 : res;
                    case -1 -> other.isNegative() ? 1 : res;
                    default -> res;
                };
            }
        };
    }
    private static boolean equalMagnitudes(long[] a, long[] b) {
        if (a == b) return true;
        int aLen = effectiveLength(a);
        if (aLen != effectiveLength(b)) return false;
        return ArraysSupport.mismatch(a, b, aLen) == -1;
    }
    @Override
    public boolean equals(Atom other) {
        if (this == other) return true;
        else if (other instanceof ScalarAtom A)
            return switch (A) {
                case LargeInteger I -> equalMagnitudes(limbs, I.limbs) && negative == I.negative;
                case LargeFraction F -> equals(F.numerator) && F.denominator.isOne();
                case LargePowerMonomial LPM -> LPM.exponent2.isOne() == negative &&
                        LPM.base.equals(this) && LPM.exponent.isOne();
                case RadicalTerm RT -> RT.rationalPart.equals(this) && RT.exponent.isZero();
                default -> compareTo(A) == 0;
            };
        return false;
    }

    //--- Inherited from Object ---//

    @Override
    public int hashCode() {
        int h = 0;
        for (int i = limbs.length - 1; i >= 0; i--)
            h = 31 * h + Long.hashCode(limbs[i]);
        return isNegative() ? -h : h;
    }
    @Override
    public LargeInteger clone() {return new LargeInteger(limbs.clone(), negative, true);}

    private static void magnitudeToStringLinear(long[] mag, StringBuilder sb, int minDigits) {

        int start = sb.length();
        long[] dividend = mag;
        do {
            DivRem dr = divideByLong(dividend, 1_000_000_000_000_000_000L);
            dividend = dr.quotient();
            long remainder = dr.remainder();
            int digits = 0;
            do {
                sb.append((char) ('0' + remainder % 10));
                remainder /= 10;
                digits++;
            } while (remainder != 0);
            // Pad all non-leading chunks to 18 digits.
            if (!isZeroMagnitude(dividend))
                while (digits++ < 18)
                    sb.append('0');
        } while (!isZeroMagnitude(dividend));
        // These become leading zeros after reversal.
        while (sb.length() - start < minDigits)
            sb.append('0');
        // Reverse only the digits appended by this call.
        for (int i = start, j = sb.length() - 1; i < j; i++, j--) {
            char c = sb.charAt(i);
            sb.setCharAt(i, sb.charAt(j));
            sb.setCharAt(j, c);
        }
    }

    /**
     * Threshold for switching from recursive toString to linear toString. If the chunk count is
     * below this threshold, then {@link #magnitudeToStringLinear(long[], StringBuilder, int)} will
     * be called.
     */
    private static final int SCHONHAGE_BASE_CONVERSION_THRESHOLD = 10;
    /**
     * A static Hashmap used to cache 10<sup>2^n</sup> for reuse in
     * {@link #magnitudeToString(long[], StringBuilder, int)}.
     */
    private static final ConcurrentHashMap<Long, long[]> powerCache = new ConcurrentHashMap<>();
    private static void magnitudeToString(long[] mag, StringBuilder sb, int minDigits) {
        if (mag.length < SCHONHAGE_BASE_CONVERSION_THRESHOLD) {
            magnitudeToStringLinear(mag, sb, minDigits);
            return;
        }
        long m = (bitLength(mag) * 1233L >>> 12) >>> 1;
        long mk = m;
        long[] power = ONE_MAG;
        while (mk != 0) {
            long t = Long.highestOneBit(mk);
            power = multiplyMagnitudes(power, powerCache.computeIfAbsent(t, k -> power(TEN_MAG, k)));
            mk -= t;
        }
        long[][] DR = divideMagnitudes(mag, power, true);
        int width = Math.toIntExact(m);
        magnitudeToString(DR[0], sb, Math.max(0, minDigits - width));
        magnitudeToString(DR[1], sb, width);

    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(Math.toIntExact((bitLength() * 1233L >>> 12)
                + 1 + (isNegative() ? 1 : 0)));
        if (isNegative()) sb.append('-');
        magnitudeToString(limbs, sb, 0);
        return sb.toString();
    }
    /**
     * Converts this LargeInteger into a BigInteger object, provided it fits in the object.
     * @return BigInteger equivalent of this integer
     */
    public BigInteger toBigInteger() {
        if (bitLength() > Integer.MAX_VALUE)
            throw new ArithmeticException("Value exceeds BigInteger's supported range");
        if (isZero()) return BigInteger.ZERO;
        /*
        BigInteger exposes a byte[] constructor. Use it
         */
        byte[] bytes = new byte[limbs.length * 8];
        for (int i = 0; i < limbs.length; i++) {
            long limb = limbs[i];
            int byteIndex = bytes.length - 8 * (i + 1);
            bytes[byteIndex] = (byte) (limb >>> 56);
            bytes[byteIndex + 1] = (byte) (limb >>> 48);
            bytes[byteIndex + 2] = (byte) (limb >>> 40);
            bytes[byteIndex + 3] = (byte) (limb >>> 32);
            bytes[byteIndex + 4] = (byte) (limb >>> 24);
            bytes[byteIndex + 5] = (byte) (limb >>> 16);
            bytes[byteIndex + 6] = (byte) (limb >>> 8);
            bytes[byteIndex + 7] = (byte) limb;
        }
        return isNegative() ? new BigInteger(-1, bytes) : new BigInteger(1, bytes);
    }

    /**
     * The maximum number of bits that a magnitude can have.
     */
    private static final long MAX_BITS = 1L << 37;
    /**
     * Throw an {@link ArithmeticException} indicating that the result exceeds this class's supported range.
     * @throws ArithmeticException always
     */
    private static void reportOverflow()
    {throw new ArithmeticException("Result exceeds supported range: |X| < 2^(2^37)");}

    //--- Logical operators ---//

    /**
     * @param len The length of the resultant array
     * @return This integer converted to an array in twos-complement.
     */
    private long[] toTwosComplement(int len) {
        long[] out = new long[len];
        System.arraycopy(limbs, 0, out, 0, Math.min(limbs.length, len));
        if (!isNegative()) return out;
        for (int i = 0; i < len; i++) out[i] = ~out[i];
        long carry = 1L;
        for (int i = 0; i < len && carry != 0L; i++) {
            long old = out[i];
            out[i] = old + carry;
            carry = Long.compareUnsigned(out[i], old) < 0 ? 1L : 0L;
        }
        return out;
    }

    /**
     * @param tc The twos-complement array to be converted to an integer
     * @return The LargeInteger resulting from
     */
    private static LargeInteger fromTwosComplement(long[] tc) {
        if (tc[tc.length - 1] >= 0)
            return new LargeInteger(tc, false);
        // Negative: abs = -(two's complement value)
        // invert all limbs, then add 1.
        for (int i = 0; i < tc.length; i++) tc[i] = ~tc[i];
        long carry = 1L;
        for (int i = 0; i < tc.length && carry != 0L; i++) {
            long old = tc[i];
            tc[i] = old + carry;
            carry = Long.compareUnsigned(tc[i], old) < 0 ? 1L : 0L;
        }
        return new LargeInteger(tc, true);
    }

    /**
     * @return A LargeInteger whose value is <code>~this</code>.
     */
    public LargeInteger not() {return negate().subtract(1);}
    /**
     * @return A LargeInteger whose value is <code>this & other</code>.
     */
    public LargeInteger and(LargeInteger other) {
        int len = Math.max(limbs.length, other.limbs.length) + 1;
        long[] a = toTwosComplement(len);
        long[] b = other.toTwosComplement(len);
        long[] out = new long[len];
        for (int i = 0; i < len; i++)
            out[i] = a[i] & b[i];
        return fromTwosComplement(out);
    }
    /**
     * @return A LargeInteger whose value is <code>this | other</code>.
     */
    public LargeInteger or(LargeInteger other) {
        int len = Math.max(limbs.length, other.limbs.length) + 1;
        long[] a = toTwosComplement(len);
        long[] b = other.toTwosComplement(len);
        long[] out = new long[len];
        for (int i = 0; i < len; i++)
            out[i] = a[i] | b[i];
        return fromTwosComplement(out);
    }
    /**
     * @return A LargeInteger whose value is <code>this ^ other</code>.
     */
    @SuppressWarnings("unused")
    public LargeInteger xor(LargeInteger other) {
        int len = Math.max(limbs.length, other.limbs.length) + 1;
        long[] a = toTwosComplement(len);
        long[] b = other.toTwosComplement(len);
        long[] out = new long[len];
        for (int i = 0; i < len; i++)
            out[i] = a[i] ^ b[i];
        return fromTwosComplement(out);
    }

    //--- Randomization ---//

    /**
     * This private special object is used in random generation for {@link #randomMagnitudeBetween(long[], long[])}.
     */
    private static final SecureRandom SECURE_RNG = new SecureRandom();
    private static long[] randomMagnitudeBetween(long[] startInclusive, long[] endExclusive) {
        startInclusive = trimLeadingZerosTrusted(startInclusive);
        endExclusive = trimLeadingZerosTrusted(endExclusive);

        int cmp = compareArray(startInclusive, endExclusive);
        if (cmp > 0)
            throw new IllegalArgumentException("Invalid random magnitude range");
        else if (cmp == 0) return startInclusive;

        // Number of possible values: endExclusive - startInclusive
        long[] range = subtractMagnitudes(endExclusive, startInclusive);

        // Offset must lie in [0, range), equivalently [0, range - 1].
        long[] maxOffset = subtractSmall(range, 1);

        // range == 1 -> only possible result is startInclusive
        if (isZeroMagnitude(maxOffset)) return startInclusive;
        long bits = bitLength(maxOffset);
        int topBits = (int) (((bits - 1) & 63L) + 1);
        long[] candidate;
        do {
            candidate = new long[maxOffset.length];
            // Fill all limbs uniformly.
            for (int i = 0; i < candidate.length; i++)
                candidate[i] = SECURE_RNG.nextLong();
            // Remove unused bits from the most-significant limb.
            if (topBits < 64)
                candidate[candidate.length - 1] &= (-1L >>> (64 - topBits));
            // Reject the excess portion of 0..2^bits-1.
        } while (compareArray(candidate, maxOffset) > 0);
        return addMagnitudes(startInclusive, candidate);
    }
    /**
     * Generates a random integer within a specified range {@code [start, end)}.
     */
    public static LargeInteger random(LargeInteger start, LargeInteger end) {
        return start.isNegative() ? random(ZERO, end.subtract(start)).add(start)
                : new LargeInteger(randomMagnitudeBetween(start.limbs, end.limbs));
    }
    /**
     * Generates a random integer with the specified number of chunks.
     */
    public static LargeInteger randomChunks(int chunks) {return randomChunks(chunks, false);}
    /**
     * Generates a random integer with the specified number of chunks and negativity.
     */
    public static LargeInteger randomChunks(int chunks, boolean negative) {
        if (chunks < 0)
            throw new IllegalArgumentException("Chunks must be nonnegative");
        else if (chunks == 0) return ZERO;
        long[] res = new long[chunks];
        for (int i = 0; i < chunks; i++) res[i] = SECURE_RNG.nextLong();
        while (res[res.length - 1] == 0) res[res.length - 1] = SECURE_RNG.nextLong();
        return new LargeInteger(res, negative);
    }
    /**
     * Generates a random integer with the specified number of bits.
     */
    public static LargeInteger randomBits(long bits) {return randomBits(bits, false);}
    /**
     * Generates a random integer with the specified number of bits and negativity.
     */
    public static LargeInteger randomBits(long bits, boolean negative) {
        if (bits < 0)
            throw new IllegalArgumentException("Bits must be nonnegative");
        if (bits == 0) return ZERO;
        if (bits > MAX_BITS) reportOverflow();
        int chunks = (int)((bits + 63) >>> 6);
        long[] value = new long[chunks];
        for (int i = 0; i < chunks; i++)
            value[i] = SECURE_RNG.nextLong();
        int topBit = (int)((bits - 1) & 63);
        // Remove bits above the requested bit length.
        if (topBit != 63)
            value[chunks - 1] &= (1L << (topBit + 1)) - 1;
        // Force exact bit length.
        value[chunks - 1] |= 1L << topBit;
        return new LargeInteger(value, negative);
    }
    /**
     * Generates a random <i>probably prime</i> integer with the specified number of chunks.
     */
    @SuppressWarnings("unused")
    public static LargeInteger randomPrimeChunks(int chunks) {
        for (;;) {
            LargeInteger p = randomChunks(chunks);
            if (p.isProbablyPrime()) return p;
            p = p.nextProbablyPrimeInteger(300);
            if (p != null && p.limbs.length == chunks) return p;
        }
    }
    /**
     * Generates a random <i>probably prime</i> integer with the specified number of chunks.
     */
    @SuppressWarnings("unused")
    public static LargeInteger randomPrimeBits(long bits) {
        for (;;) {
            LargeInteger p = randomBits(bits);
            if (p.isProbablyPrime()) return p;
            p = p.nextProbablyPrimeInteger(301);
            if (p != null && p.bitLength() == bits) return p;
        }
    }

    //--- Inherited from ScalarAtom ---//

    public String toLatex() {return toString();}
    @Override
    public boolean isZero() {return limbs.length == 1 && limbs[0] == 0L;}
    @Override
    public boolean isOne() {return !isNegative() && limbs.length == 1 && limbs[0] == 1;}
    public LargeInteger sign() {return isNegative() ? NEGATIVE_ONE : (isZero() ? ZERO : ONE);}
    public boolean isNegative() {return negative;}
    public LargeInteger negate() {return new LargeInteger(limbs, !negative, true);}
    @Override
    public LargeInteger realPart() {return this;}
    @Override
    public LargeInteger imagPart() {return ZERO;}
    @Override
    public LargeInteger abs() {return isNegative() ? negate() : this;}
    @Override
    public LargeInteger floor() {return this;}
    @Override
    public LargeInteger ceil() {return this;}
    @Override
    public boolean isRational() {return true;}
    @Override
    public boolean isReal() {return true;}
    @Override
    public boolean isAlgebraic() {return true;}
    @Override
    public LargePolynomial annihilatingPolynomial()
    {return new LargePolynomial(LargeList.of(LargeFraction.ONE, LargeFraction.valueOf(negate())));}

    //--- Addition ---//

    private static long[] addSmall(long[] addend, long adder) {
        int n = effectiveLength(addend);
        if (n == 1) {
            long low = addend[0] + adder;
            return Long.compareUnsigned(low, addend[0]) < 0 ? new long[]{low, 1L} : new long[]{low};
        }
        long[] out = new long[n];
        long low = addend[0] + adder;
        out[0] = low;
        long carry = Long.compareUnsigned(low, addend[0]) < 0 ? 1L : 0L;
        if (carry == 0) {
            System.arraycopy(addend, 1, out, 1, n - 1);
            return out;
        }
        for (int i = 1; i < n; i++) {
            long x = addend[i] + 1L;
            out[i] = x;
            if (x != 0L) {
                System.arraycopy(addend, i + 1, out, i + 1, n - i - 1);
                return out;
            }
        }
        // Carry propagated through the entire magnitude.
        long[] expanded = Arrays.copyOf(out, n + 1);
        expanded[n] = 1L;
        return expanded;
    }
    private static long[] addMagnitudes(long[] a, long[] b) {
        int an = effectiveLength(a);
        int bn = effectiveLength(b);
        if (an < bn) {
            long[] t = a;
            a = b;
            b = t;
            int ti = an;
            an = bn;
            bn = ti;
        }
        if (bn == 1) {
            long[] out = new long[an];
            long a0 = a[0];
            long low = a0 + b[0];
            out[0] = low;
            // No carry.
            if (Long.compareUnsigned(low, a0) >= 0) {
                if (an > 1) System.arraycopy(a, 1, out, 1, an - 1);
                return out;
            }
            // Propagate carry.
            for (int i = 1; i < an; i++) {
                long x = a[i] + 1L;
                out[i] = x;
                if (x != 0L) {
                    if (i + 1 < an) System.arraycopy(a, i + 1, out, i + 1, an - i - 1);
                    return out;
                }
            }
            long[] expanded = Arrays.copyOf(out, an + 1);
            expanded[an] = 1L;
            return expanded;
        }
        long[] out = new long[an];
        long carry = 0L;
        int i = 0;
        for (; i < bn; i++) {
            long ai = a[i];
            long s = ai + b[i];
            long c1 = Long.compareUnsigned(s, ai) < 0 ? 1L : 0L;

            long r = s + carry;
            long c2 = Long.compareUnsigned(r, s) < 0 ? 1L : 0L;

            out[i] = r;
            carry = c1 | c2;
        }
        // Propagate carry through the unmatched high part of A.
        while (i < an && carry != 0L) {
            long r = a[i] + 1L;
            out[i++] = r;
            carry = r == 0L ? 1L : 0L;
        }
        // Remaining limbs are unchanged.
        if (i < an)
            System.arraycopy(a, i, out, i, an - i);
        if (carry != 0L) {
            out = Arrays.copyOf(out, an + 1);
            out[an] = 1L;
        }
        return out;
    }
    private static long[] addThreeMagnitudes(long[] a, long[] b, long[] c) {
        int n = Math.max(a.length, Math.max(b.length, c.length));
        long[] out = new long[n + 1];

        long carry = 0; // can be 0, 1, or 2

        for (int i = 0; i < n; i++) {
            long x = i < a.length ? a[i] : 0;
            long y = i < b.length ? b[i] : 0;
            long z = i < c.length ? c[i] : 0;

            long s = x + y;
            long c1 = Long.compareUnsigned(s, x) < 0 ? 1 : 0;

            long t = s + z;
            long c2 = Long.compareUnsigned(t, s) < 0 ? 1 : 0;

            long u = t + carry;
            long c3 = Long.compareUnsigned(u, t) < 0 ? 1 : 0;

            out[i] = u;
            carry = c1 + c2 + c3;
        }
        out[n] = carry;
        return out[n] == 0 ? Arrays.copyOf(out, n) : out;
    }
    private static long[] addFourMagnitudes(long[] a, long[] b, long[] c, long[] d) {
        int n = Math.max(
                Math.max(a.length, b.length),
                Math.max(c.length, d.length)
        );
        long[] out = new long[n + 1];

        long carry = 0; // can be 0, 1, 2, or 3

        for (int i = 0; i < n; i++) {
            long w = i < a.length ? a[i] : 0L;
            long x = i < b.length ? b[i] : 0L;
            long y = i < c.length ? c[i] : 0L;
            long z = i < d.length ? d[i] : 0L;

            long s = w + x;
            long c1 = Long.compareUnsigned(s, w) < 0 ? 1L : 0L;

            long t = s + y;
            long c2 = Long.compareUnsigned(t, s) < 0 ? 1L : 0L;

            long u = t + z;
            long c3 = Long.compareUnsigned(u, t) < 0 ? 1L : 0L;

            long v = u + carry;
            long c4 = Long.compareUnsigned(v, u) < 0 ? 1L : 0L;

            out[i] = v;
            carry = c1 + c2 + c3 + c4;
        }
        out[n] = carry;
        return out[n] == 0L ? Arrays.copyOf(out, n) : out;
    }
    private static void addMagnitudeInPlace(long[] out, long[] x, int offset) {
        int xLen = effectiveLength(x);
        if (xLen == 1 && x[0] == 0) return;
        if (offset + xLen > out.length)
            throw new AssertionError("term does not fit: offset=" + offset + ", xLen=" + xLen +
                    ", outLen=" + out.length);
        long carry = 0L;
        for (int i = 0; i < xLen; i++)
            carry = addWithCarry(out, offset + i, x[i], carry);
        int k = offset + xLen;
        while (carry != 0L) {
            if (k >= out.length) throw new AssertionError("carry overflow");
            long old = out[k];
            long sum = old + carry;
            out[k] = sum;
            carry = Long.compareUnsigned(sum, old) < 0 ? 1L : 0L;
            k++;
        }
    }
    private static long addWithCarry(long[] out, int index, long x, long carry) {
        long old = out[index];
        long sum = old + x;
        long c1 = Long.compareUnsigned(sum, old) < 0 ? 1L : 0L;

        long result = sum + carry;
        long c2 = Long.compareUnsigned(result, sum) < 0 ? 1L : 0L;

        out[index] = result;
        return c1 + c2;
    }
    /**
     * Calculates and returns a LargeInteger whose value is <code>this + other</code>.
     * @param other The addend
     * @return The sum of <code>this</code> and <code>other</code>
     */
    public LargeInteger add(LargeInteger other) {
        if (isZero()) return other;
        if (other.isZero()) return this;
        if (negative == other.negative)
            return new LargeInteger(addMagnitudes(limbs, other.limbs), negative, true);
        int cmp = compareMagnitudes(other);
        if (cmp == 0) return ZERO;
        return cmp > 0
                ? new LargeInteger(subtractMagnitudes(limbs, other.limbs), negative, true)
                : new LargeInteger(subtractMagnitudes(other.limbs, limbs), other.negative, true);
    }
    @Override
    public ScalarAtom add(ScalarAtom other) {
        return switch (other) {
            case LargeInteger I -> add(I);
            case LargeFraction F -> new LargeFraction(F.denominator.multiply(this)
                    .add(F.numerator), F.denominator);
            case LargePowerMonomial LPM -> new RadicalTerm(LargeFraction.valueOf(this),
                    LargeFraction.ONE, LPM);
            case RadicalTerm RT -> new RadicalTerm((LargeFraction) add(RT.rationalPart), RT.coefficient,
                    RT.exponent);
            case RadicalTermSum RTS -> new RadicalTermSum(RTS.sum, (LargeFraction) RTS.constant.add(this));
            case LargePowerNode LPN -> new RadicalTermNode(LargeFraction.valueOf(this),
                    LargeFraction.ONE, LPN);
            default -> throw new UnsupportedOperationException(
                    "Operation with these types together not supported yet");
        };
    }
    /**
     * Package-private method used to add a LargeInteger with a signed long.
     * @param other The addend
     * @return The sum of <code>this</code> and <code>other</code>
     */
    LargeInteger add(long other) {
        if (other == 0L) return this;
        boolean otherSign = other < 0;
        // Caveat: Long.MIN_VALUE needs special handling because -Long.MIN_VALUE overflows.
        if (other == Long.MIN_VALUE)
            return add(valueOf(other));
        long otherAbs = otherSign ? -other : other;
        if (negative == otherSign)
            return new LargeInteger(addSmall(limbs, otherAbs), negative, true);
        int cmp = compareSmall(limbs, otherAbs);
        if (cmp == 0) return ZERO;
        else if (cmp > 0)
            return new LargeInteger(subtractSmall(limbs, otherAbs), negative, true);
        return new LargeInteger(new long[]{otherAbs - limbs[0]}, otherSign, true);
    }

    //--- Subtraction ---//

    private static long[] subtractMagnitudes(long[] a, long[] b) {
        if (equalMagnitudes(a, b)) return ZERO_MAG;
        int n = effectiveLength(a);
        int m = effectiveLength(b);
        long[] out = new long[n];

        long borrow = 0L;

        int i = 0;
        for (; i < m; i++) {
            long ai = a[i];
            long bi = b[i];
            long t = ai - bi;
            long b1 = Long.compareUnsigned(ai, bi) < 0 ? 1L : 0L;

            long r = t - borrow;
            long b2 = Long.compareUnsigned(t, borrow) < 0 ? 1L : 0L;

            out[i] = r;
            borrow = b1 | b2;
        }
        for (; i < n; i++) {
            long ai = a[i];
            long r = ai - borrow;
            out[i] = r;
            if (borrow == 0L) {
                System.arraycopy(a, i + 1, out, i + 1, n - i - 1);
                break;
            }
            borrow = ai == 0L ? 1L : 0L;
        }
        if (borrow != 0L)
            throw new ArithmeticException("underflow in magnitude subtraction");
        return trimLeadingZerosTrusted(out);
    }
    private static long[] subtractSmall(long[] a, long small) {
        int len = effectiveLength(a);
        if (small == 0L) return a;
        long a0 = a[0];
        if (len == 1) {
            int cmp = Long.compareUnsigned(a0, small);
            if (cmp < 0)
                throw new ArithmeticException("subtractSmall requires a >= small");
            if (cmp == 0) return ZERO_MAG;
            return new long[]{a0 - small};
        }
        if (len == 2) {
            long low = a0 - small;
            if (Long.compareUnsigned(a0, small) >= 0)
                return new long[]{low, a[1]};
            long high = a[1] - 1L;
            if (high != 0L) return new long[]{low, high};
            // Result contracted from 2 limbs to 1.
            return new long[]{low};
        }
        long[] out = new long[len];
        long low = a0 - small;
        out[0] = low;
        if (Long.compareUnsigned(a0, small) >= 0) {
            System.arraycopy(a, 1, out, 1, len - 1);
            return out;
        }
        for (int i = 1; i < len; i++) {
            long x = a[i];
            if (x != 0L) {
                out[i] = x - 1;
                System.arraycopy(a, i + 1, out, i + 1, len - i - 1);
                if (i == len - 1 && out[i] == 0L)
                    return trimLeadingZerosTrusted(out);
                return out;
            }
            out[i] = -1L;
        }
        return trimLeadingZerosTrusted(out);
    }
    private static void subtractMagnitudeInPlace(long[] a, long[] b) {
        int bLen = effectiveLength(b);
        long borrow = 0L;
        int i = 0;
        for (; i < bLen; i++) {
            long ai = a[i];
            long bi = b[i];

            long t = ai - bi;
            long b1 = Long.compareUnsigned(ai, bi) < 0 ? 1L : 0L;

            long r = t - borrow;
            long b2 = Long.compareUnsigned(t, borrow) < 0 ? 1L : 0L;

            a[i] = r;
            borrow = b1 | b2;
        }
        while (borrow != 0L && i < a.length) {
            long ai = a[i];
            a[i++] = ai - 1L;
            borrow = ai == 0L ? 1L : 0L;
        }

        if (borrow != 0L)
            throw new ArithmeticException("underflow in magnitude subtraction");
    }
    private static boolean subtractMulLimbAt(long[] u, long[] v, long qhat, int offset) {
        long carry = 0L;
        long borrow = 0L;
        for (int i = 0; i < v.length; i++) {
            long low = v[i] * qhat;
            long high = Math.unsignedMultiplyHigh(v[i], qhat);
            // Add carry from PREVIOUS limb to this product's low limb.
            long subLow = low + carry;
            high += Long.compareUnsigned(subLow, low) < 0 ? 1L : 0L;
            // This product's high limb becomes NEXT iteration's carry.
            carry = high;
            long x = u[offset + i];
            long t = x - subLow;
            long b1 = Long.compareUnsigned(x, subLow) < 0 ? 1L : 0L;
            long t2 = t - borrow;
            long b2 = Long.compareUnsigned(t, borrow) < 0 ? 1L : 0L;
            u[offset + i] = t2;
            borrow = b1 | b2;
        }
        long x = u[offset + v.length];
        long t = x - carry;
        long b1 = Long.compareUnsigned(x, carry) < 0 ? 1L : 0L;
        long t2 = t - borrow;
        long b2 = Long.compareUnsigned(t, borrow) < 0 ? 1L : 0L;
        u[offset + v.length] = t2;
        return (b1 | b2) != 0L;
    }
    /**
     * Calculates and returns a LargeInteger whose value is <code>this - other</code>.
     * @param other The subtrahend
     * @return The difference between <code>this</code> and <code>other</code>
     */
    public LargeInteger subtract(LargeInteger other) {
        if (isZero())
            return other.negate();
        if (other.isZero())
            return this;
        // a - (-b), or (-a) - b
        if (negative != other.negative)
            return new LargeInteger(addMagnitudes(limbs, other.limbs), negative, true);
        // Same signs: subtract magnitudes.
        int cmp = compareMagnitudes(other);
        if (cmp == 0) return ZERO;
        if (cmp > 0)
            return new LargeInteger(subtractMagnitudes(limbs, other.limbs), negative, true);
        return new LargeInteger(subtractMagnitudes(other.limbs, limbs), !negative, true);
    }
    /**
     * Package-private method used to subtract a signed long from a LargeInteger.
     * @param other The subtrahend
     * @return The sum of <code>this</code> and <code>other</code>
     */
    LargeInteger subtract(long other) {
        if (other == Long.MIN_VALUE)
            return subtract(valueOf(other));
        return add(-other);
    }

    //--- Bit shifts and limb shifts ---//

    private static long[] shiftLeftLimbs(long[] x, int limbs) {
        if (isZeroMagnitude(x)) return ZERO_MAG;
        if (limbs == 0) return x;
        if (limbs < 0) return shiftRightLimbs(x, -limbs);
        long[] out = new long[x.length + limbs];
        // pad zeros at the beginning
        System.arraycopy(x, 0, out, limbs, x.length);
        return trimLeadingZerosTrusted(out);
    }
    private static long[] shiftLeftBits(long[] n, long shift) {
        if (isZeroMagnitude(n)) return ZERO_MAG;
        if (shift < 0) return shiftRightBits(n, -shift);
        if (shift == 0) return trimLeadingZerosTrusted(n);
        if (bitLength(n) + shift > MAX_BITS) reportOverflow();
        int limbShift = (int) shift >>> 6;
        int bitShift = (int) shift & 63;
        long[] out = new long[n.length + limbShift + 1];
        if (bitShift == 0) {
            System.arraycopy(n, 0, out, limbShift, n.length);
            return trimLeadingZerosTrusted(out);
        }
        long carry = 0;
        for (int i = 0; i < n.length; i++) {
            long cur = n[i];
            out[i + limbShift] = (cur << bitShift) | carry;
            carry = cur >>> (64 - bitShift);
        }
        out[n.length + limbShift] = carry;
        return trimLeadingZerosTrusted(out);
    }
    private static void shiftLeftBitsInto(long[] src, int shift, long[] dst) {
        if (shift < 0 || shift >= 64)
            throw new IllegalArgumentException("shift must be in [0, 63]");
        if (shift == 0) {
            if (dst.length < src.length)
                throw new IllegalArgumentException("Destination too small");
            System.arraycopy(src, 0, dst, 0, src.length);
            Arrays.fill(dst, src.length, dst.length, 0L);
            return;
        }
        if (dst.length < src.length + 1)
            throw new IllegalArgumentException("Destination too small");
        long carry = 0L;
        int inv = 64 - shift;
        for (int i = 0; i < src.length; i++) {
            long x = src[i];
            dst[i] = (x << shift) | carry;
            carry = x >>> inv;
        }
        dst[src.length] = carry;
        Arrays.fill(dst, src.length + 1, dst.length, 0L);
    }
    private static long[] shiftRightLimbs(long[] x, int limbs) {
        if (isZeroMagnitude(x) || limbs >= x.length) return ZERO_MAG;
        if (limbs < 0) return shiftLeftLimbs(x, -limbs);
        long[] out = new long[x.length - limbs];
        System.arraycopy(x, limbs, out, 0, x.length - limbs);
        return trimLeadingZerosTrusted(out);
    }
    private static long[] shiftRightBits(long[] x, long bits) {
        if (isZeroMagnitude(x)) return ZERO_MAG;
        if (bits < 0) return shiftLeftBits(x, -bits);
        else if (bits >= 64) return shiftRightBits(
                shiftRightLimbs(x, Math.toIntExact(bits / 64)), bits & 63);
        else if (bits == 0) return x;
        int len = effectiveLength(x);
        long[] out = new long[len];
        long carry = 0L;
        for (int i = len - 1; i >= 0; i--) {
            long limb = x[i];
            out[i] = (limb >>> bits) | carry;
            carry = limb << (64 - bits);
        }
        return trimLeadingZerosTrusted(out);
    }
    private static void shiftRightBitsInPlace(long[] a, long bits) {
        if (bits < 0) throw new IllegalArgumentException("Negative shift");
        if (bits == 0) return;
        long limbShiftLong = bits >>> 6;
        if (limbShiftLong >= a.length) {
            Arrays.fill(a, 0L);
            return;
        }
        int limbShift = (int) limbShiftLong;
        int bitShift = (int) (bits & 63L);
        int newLength = a.length - limbShift;
        if (bitShift == 0)
            // System.arraycopy handles overlapping ranges.
            System.arraycopy(a, limbShift, a, 0, newLength);
        else {
            int inverseShift = 64 - bitShift;
            for (int i = 0; i < newLength; i++) {
                int src = i + limbShift;
                long low = a[src] >>> bitShift;
                long high = (src + 1 < a.length) ? a[src + 1] << inverseShift : 0L;
                a[i] = low | high;
            }
        }
        Arrays.fill(a, newLength, a.length, 0L);
    }
    private static void subtractShiftedLeftBitsInPlace(long[] a, long[] b, int shift, String where) {
        if (shift <= 0 || shift >= 64)
            throw new IllegalArgumentException();
        int bLen = effectiveLength(b);
        long[] bt = Arrays.copyOf(b, bLen);
        long[] shifted = shiftLeftBits(bt, shift);
        if (compareArray(a, shifted) < 0)
            throw new AssertionError(where + " : a < (b << " + shift + "), a_Len=" +
                    trimLeadingZerosTrusted(a).length + ", bLen=" + bt.length + ", shiftedLen=" + shifted.length);
        long borrow = 0L;
        long spill = 0L;
        int i = 0;
        // Subtract ordinary shifted limbs.
        for (; i < bLen; i++) {
            long x = b[i];
            long bi = (x << shift) | spill;
            spill = x >>> (64 - shift);
            long ai = a[i];
            long t = ai - bi;
            long b1 = Long.compareUnsigned(ai, bi) < 0 ? 1L : 0L;
            long u = t - borrow;
            long b2 = Long.compareUnsigned(t, borrow) < 0 ? 1L : 0L;
            a[i] = u;
            borrow = b1 | b2;
        }
        // Left shift may have produced one extra high limb.
        if (spill != 0L) {
            if (i >= a.length)
                throw new ArithmeticException("shifted subtraction overflow");
            long ai = a[i];
            long t = ai - spill;
            long b1 = Long.compareUnsigned(ai, spill) < 0 ? 1L : 0L;
            long u = t - borrow;
            long b2 = Long.compareUnsigned(t, borrow) < 0 ? 1L : 0L;
            a[i] = u;
            borrow = b1 | b2;
            i++;
        }
        // If there was no spill, borrow still has to be consumed.
        while (borrow != 0L) {
            if (i >= a.length)
                throw new ArithmeticException("subtract underflow");
            long ai = a[i];
            a[i] = ai - 1L;
            borrow = ai == 0L ? 1L : 0L;
            i++;
        }
    }
    /**
     * Returns a LargeInteger whose value is {@code this << shift}.
     */
    public LargeInteger shiftLeft(long shift)
    {return shift == 0 ? this : new LargeInteger(shiftLeftBits(limbs, shift), negative, true);}
    /**
     * Returns a LargeInteger whose value is {@code this >> shift}.
     * @apiNote The <code>>>></code> function is not provided since such a function only makes sense
     * for a fixed-with primitive.
     */
    public LargeInteger shiftRight(long shift)
    {return shift == 0 ? this : new LargeInteger(shiftRightBits(limbs, shift), negative, true);}

    //--- Individual bit operations ---//

    private static boolean testBit(long[] x, long bit) {
        long limb = bit >>> 6;
        int offset = (int)(bit & 63);
        if (limb >= x.length) return false;
        return ((x[(int)limb] >>> offset) & 1L) != 0L;
    }
    /**
     * Tests a specified bit to determine if it is set.
     * @param bit The specified bit tested
     * @return A boolean indicated if the bit is set
     */
    public boolean testBit(long bit) {return testBit(limbs, bit);}
    /**
     * Sets the specified bit as 1, if it is 0.
     * @param bit The bit requested to be set
     * @return A LargeInteger after the bit is set
     */
    public LargeInteger setBit(long bit) {
        if (bit < 0)
            throw new ArithmeticException("Negative bit address");
        if (testBit(bit))
            return this;
        long limbIndex = bit >>> 6;
        if (limbIndex >= Integer.MAX_VALUE) reportOverflow();
        int index = (int) limbIndex;
        long[] out = Arrays.copyOf(limbs, Math.max(limbs.length, index + 1));
        out[index] |= 1L << (bit & 63);
        return new LargeInteger(out, negative, true);
    }
    /**
     * Sets the specified bit as 0, if it is 1.
     * @param bit The bit requested to be set
     * @return A LargeInteger after the bit is set
     */
    @SuppressWarnings("unused")
    public LargeInteger clearBit(long bit) {
        if (bit < 0)
            throw new ArithmeticException("Negative bit address");
        if (!testBit(bit))
            return this;

        int index = (int) (bit >>> 6);
        long[] out = Arrays.copyOf(limbs, limbs.length);
        out[index] &= ~(1L << (bit & 63));
        return new LargeInteger(out, negative);
    }
    /**
     * Flips the specified bit such that is set to 1 if not set, 0 otherwise.
     * @param bit The bit which is flipped
     * @return A LargeInteger after the bit is flipped
     */
    @SuppressWarnings("unused")
    public LargeInteger flipBit(long bit) {
        if (bit < 0)
            throw new ArithmeticException("Negative bit address");
        if (bit > MAX_BITS) reportOverflow();

        int index = (int) (bit >>> 6);
        long[] out = Arrays.copyOf(limbs, Math.max(limbs.length, index + 1));
        out[index] ^= 1L << (bit & 63);
        return new LargeInteger(out, negative);
    }

    //--- Multiplication and squaring ---//

    /**
     * Slices the operand, retaining the portion specified by <code>from</code> and <code>to</code>.
     * @param a The array sliced
     * @param from The starting index, inclusive
     * @param to The ending index, exclusive
     * @return the result of slicing the input array
     */
    private static long[] slice(long[] a, int from, int to) {
        if (to >= a.length) to = a.length;
        if (from < 0) from = 0;
        if (from > to) throw new IllegalArgumentException("Illegal slice range " + from + " to " + to);
        long[] out = new long[to - from];
        System.arraycopy(a, from, out, 0, to - from);
        return out;
    }
    /**
     * Slices the operand, retaining the portion specified by <code>from</code> and <code>length</code>.
     * @param a The array sliced
     * @param from The starting index, inclusive
     * @param length The length of the retained array
     * @return the result of slicing the input array
     */
    private static long[] sliceFixed(long[] a, int from, int length) {
        if (from < 0 || length < 0)
            throw new IllegalArgumentException("Negative slice");
        long[] out = new long[length];
        if (from >= a.length) return out;
        int amount = Math.min(length, a.length - from);
        System.arraycopy(a, from, out, 0, amount);
        return out;
    }
    private static void multiplySmallInto(long[] a, long m, long[] out) {
        int n = Math.min(a.length, out.length);
        long carry = 0L;

        int i = 0;
        for (; i < n; i++) {
            long low = a[i] * m;
            long high = Math.unsignedMultiplyHigh(a[i], m);
            long sum = low + carry;
            high += Long.compareUnsigned(sum, low) < 0 ? 1L : 0L;
            out[i] = sum;
            carry = high;
        }
        // Source exhausted, but destination has room for the final carry.
        if (i < out.length) {
            out[i++] = carry;
            // Important if 'out' is reused.
            while (i < out.length)
                out[i++] = 0L;
        }
    }
    /**
     * Multiplies a nonnegative integer magnitude by a single unsigned
     * 64-bit limb.
     *
     * <p>
     * The operand {@code a} is represented as a little-endian array
     * of unsigned 64-bit limbs, with each limb corresponding to a
     * digit in base 2^64. The multiplier {@code m} is interpreted
     * as unsigned, regardless of its signed Java {@code long} value.
     * </p>
     *
     * <h3>Special Cases</h3>
     * <ul>
     *   <li>
     *     <b>Multiplication by one:</b>
     *     If {@code m == 1L}, returns {@code a} directly without
     *     allocating another array.
     *   </li>
     *   <li>
     *     <b>Multiplication by zero:</b>
     *     If {@code m == 0L} or {@code a} represents zero,
     *     returns {@link #ZERO_MAG}.
     *   </li>
     * </ul>
     *
     * <h3>General Multiplication</h3>
     * <p>
     * For all other cases, an output array of length
     * <code>a.length + 1</code> is allocated to accommodate
     * a possible final carry.
     * </p>
     * <p>
     * The method delegates the actual multiplication to
     * {@code multiplySmallInto}, which computes the product
     * using unsigned limb arithmetic and writes it into the
     * allocated output array.
     * </p>
     * <p>
     * After multiplication, redundant most-significant zero
     * limbs are removed using {@code trimLeadingZeros}.
     * </p>
     *
     * <h3>Complexity</h3>
     * <ul>
     *   <li><b>Time:</b> O(n), where n is {@code a.length}.</li>
     *   <li><b>Space:</b> O(n) for the output array.</li>
     * </ul>
     *
     * @param a the nonnegative integer magnitude, represented
     *          as a little-endian array of unsigned 64-bit limbs
     * @param m the unsigned 64-bit multiplier
     * @return the product of {@code a} and {@code m} as a
     *         normalized little-endian magnitude array;
     *         may return {@code a} itself when {@code m == 1L}
     */

    private static long[] multiplySmall(long[] a, long m) {
        if (m == 1L)
            return a;
        if (m == 0L || isZeroMagnitude(a))
            return ZERO_MAG;
        long[] out = new long[a.length + 1];
        multiplySmallInto(a, m, out);
        return trimLeadingZerosTrusted(out);
    }

    /*
     * The following constants are multiplication dispatch thresholds.
     * These constants are intentionally implementation-specific. They are chosen
     * experimentally from benchmark heatmaps for the current limb representation
     * and multiplication implementations.
     * In this build:
     * below KARATSUBA_THRESHOLD: grade-school multiplication is fastest
     * below TOOM_COOK_3_THRESHOLD: Karatsuba is fastest
     * below TOOM_COOK_4_THRESHOLD: Toom-Cook 3 is fastest
     * at or above TOOM_COOK_4_THRESHOLD: Toom-Cook 4 is fastest
     * Do not change these based on asymptotic complexity alone; re-benchmark first.
     */

    /**
     * Threshold for switching from grade-school to Karatsuba in regular multiply. If the number of limbs in the
     * smaller operand is in the range from this to the Toom-Cook 3 threshold, then the Karatsuba algorithm will
     * be chosen. This value was found via threshold benchmarks to work well.
     */
    private static final int KARATSUBA_THRESHOLD = 36;
    /**
     * Threshold for switching from Karatsuba to the Toom-Cook 3 algorithm in regular multiply. If the
     * number of limbs in the smaller operand is in the range from this to the Toom-Cook 4 threshold,
     * then the Toom-3 algorithm will be chosen. This value was found via threshold benchmarks to work well.
     */
    private static final int TOOM_COOK_3_THRESHOLD = 104;
    /**
     * Threshold for switching from Toom-Cook 3 to the Toom-Cook 4 algorithm in regular multiply. If the
     * number of limbs in the smaller operand is greater than or equal to this threshold, then the Toom-4
     * algorithm will be chosen. This value was found via threshold benchmarks to work well.
     */
    private static final int TOOM_COOK_4_THRESHOLD = 432;

    /**
     * Multiplies two magnitudes with the specified thresholds.
     * See {@link #multiplyMagnitudesWithThresholds(long[], long[], int, int, int)} for more details.
     */
    private static long[] multiplyMagnitudes(long[] a, long[] b) {
        return multiplyMagnitudesWithThresholds(a, b,
                KARATSUBA_THRESHOLD, TOOM_COOK_3_THRESHOLD, TOOM_COOK_4_THRESHOLD);
    }
    /**
     * Multiplies two nonnegative integer magnitudes using classical
     * schoolbook multiplication, storing the result in a supplied
     * output buffer.
     *
     * <p>
     * The operands are represented as little-endian arrays of unsigned
     * 64-bit limbs. Each limb represents a digit in base 2^64,
     * regardless of its signed Java {@code long} interpretation.
     * </p>
     *
     * <p>
     * The output buffer is cleared before multiplication begins.
     * If the buffer is shorter than the full product, only the
     * least-significant limbs that fit are retained. Thus, the
     * result is computed modulo 2^(64*out.length).
     * </p>
     *
     * <h3>Initialization and Special Cases</h3>
     * <ul>
     *   <li>
     *     All elements of {@code out} are initialized to zero
     *     using {@code Arrays.fill}.
     *   </li>
     *   <li>
     *     If {@code out} is empty or either operand represents
     *     zero, the zero-filled output buffer is returned.
     *   </li>
     *   <li>
     *     If {@code a} is shorter than {@code b}, the local
     *     references are exchanged so that {@code a} is the
     *     longer operand.
     *   </li>
     * </ul>
     *
     * <h3>Schoolbook Multiplication</h3>
     * <p>
     * The multiplication follows the classical digit-by-digit
     * algorithm. For each limb {@code b[k]}, every relevant
     * limb {@code a[i]} is multiplied by it, and the resulting
     * partial product is accumulated at index {@code i + k}
     * in the output buffer.
     * </p>
     *
     * <p>
     * Each individual limb multiplication produces a conceptual
     * 128-bit unsigned product, divided into two 64-bit halves:
     * </p>
     * <ul>
     *   <li>
     *     <code>low = x * scalar</code> gives the lower
     *     64 bits of the product.
     *   </li>
     *   <li>
     *     <code>high = Math.unsignedMultiplyHigh(x, scalar)</code>
     *     gives the upper 64 bits of the product.
     *   </li>
     * </ul>
     *
     * <h3>Accumulation and Carry Propagation</h3>
     * <p>
     * The lower product limb is added to the existing output
     * limb, followed by the carry from the preceding
     * multiplication.
     * </p>
     * <pre>
     * low  = low64(x * scalar)
     * high = high64(x * scalar)
     *
     * t = low + out[i + k]
     * if t overflows unsigned 64-bit arithmetic:
     *     high++
     *
     * r = t + carry
     * if r overflows unsigned 64-bit arithmetic:
     *     high++
     *
     * out[i + k] = r
     * carry = high
     * </pre>
     *
     * <p>
     * Unsigned addition overflow is detected using
     * {@code Long.compareUnsigned}. Each overflow contributes
     * one additional unit to the upper limb of the partial
     * product.
     * </p>
     *
     * <p>
     * After the inner multiplication loop, any remaining carry
     * is propagated through successive output limbs until
     * the carry becomes zero or the buffer is exhausted.
     * </p>
     *
     * <h3>Output Truncation</h3>
     * <ul>
     *   <li>
     *     Outer-loop iterations are limited to
     *     <code>min(b.length, out.length)</code>.
     *   </li>
     *   <li>
     *     Inner-loop iterations are limited to
     *     <code>min(a.length, out.length - k)</code>.
     *   </li>
     *   <li>
     *     Terms whose destination indices exceed the output
     *     buffer are skipped because they cannot affect its
     *     least-significant limbs.
     *   </li>
     *   <li>
     *     Any carry extending beyond the output buffer is
     *     intentionally discarded.
     *   </li>
     *   <li>
     *     If {@code out.length >= a.length + b.length},
     *     the buffer can contain the complete, exact product.
     *   </li>
     * </ul>
     *
     * <h3>Complexity</h3>
     * <p>
     * Let n and m denote the lengths of the two operands,
     * with n being the larger length.
     * </p>
     * <ul>
     *   <li>
     *     <b>Time:</b> O(n*m + L), where L is
     *     {@code out.length}. The multiplication loops perform
     *     at most n*m limb products; output initialization and
     *     carry propagation contribute additional work.
     *   </li>
     *   <li>
     *     <b>Auxiliary space:</b> O(1), excluding the
     *     caller-supplied output buffer.
     *   </li>
     * </ul>
     *
     * <h3>Buffer Requirements</h3>
     * <p>
     * The output buffer must be writable and must not alias either
     * input array, since it is cleared before multiplication.
     * The returned array is the same {@code out} instance;
     * no result array is allocated internally.
     * </p>
     *
     * @param a the first nonnegative integer magnitude,
     *          represented as a little-endian array of
     *          unsigned 64-bit limbs
     * @param b the second nonnegative integer magnitude,
     *          using the same representation as {@code a}
     * @param out the caller-provided output buffer that receives
     *            the product, truncated to its available
     *            number of limbs if necessary
     * @return {@code out}, containing the product modulo
     *         2^(64*out.length), without normalization
     */
    private static long[] multiplyInto(long[] a, long[] b, long[] out) {
        Arrays.fill(out, 0);
        if (out.length == 0 || isZeroMagnitude(a) || isZeroMagnitude(b)) return out;
        // Put the longer operand in A.
        if (a.length < b.length) {
            long[] t = a;
            a = b;
            b = t;
        }
        // Any b[k] with k >= out.length contributes only beyond the buffer.
        for (int k = 0, bLimit = Math.min(b.length, out.length); k < bLimit; k++) {
            long scalar = b[k];
            if (scalar == 0L) continue;
            long carry = 0L;
            int oi = k;
            // Only terms satisfying k + i < out.length matter.
            for (int i = 0, aLimit = Math.min(a.length, out.length - k); i < aLimit; ++i) {
                long x = a[i];
                long low = x * scalar;
                long high = Math.unsignedMultiplyHigh(x, scalar);
                long t = low + out[oi];
                if (Long.compareUnsigned(t, low) < 0) high++;
                long r = t + carry;
                if (Long.compareUnsigned(r, t) < 0) high++;
                out[oi++] = r;
                carry = high;
            }
            /*
             * If A was exhausted before the output buffer was exhausted,
             * the carry belongs in the next limb. Otherwise, that carry is
             * beyond out[] and is intentionally discarded.
             */
            while (carry != 0L && oi < out.length) {
                long old = out[oi];
                long r = old + carry;
                out[oi++] = r;
                carry = Long.compareUnsigned(r, old) < 0 ? 1L : 0L;
            }
        }
        return out;
    }

    /**
     * Computes <code>a * b mod 2<sup>64*limit</sup></code>.
     * @implNote This method, for Karatsuba and Toom-Cook, computes the full product and truncates.
     * This could potentially lead to lots of unnecessary memory allocation.
     */
    private static long[] multiplyLow(long[] a, long[] b, int limit) {
        if (limit == 0)
            return new long[0];
        if (Math.min(a.length, b.length) < KARATSUBA_THRESHOLD)
            return trimLeadingZerosTrusted(multiplyInto(a, b, new long[limit]));
        // Karatsuba / Toom-Cook for large operands
        long[] product = multiplyMagnitudes(a, b);
        // Keep only the lowest 'limit' limbs
        return Arrays.copyOf(product, limit);
    }

    /**
     * Multiplies two nonnegative integer magnitudes using a threshold-based
     * selection of multiplication algorithms.
     *
     * <p>
     * The operands are represented as little-endian arrays of unsigned
     * 64-bit limbs, with each limb stored in a Java {@code long}.
     * The numerical base is 2^64, meaning that limb {@code i} represents
     * a multiple of 2^(64*i).
     * </p>
     *
     * <p>
     * The method removes leading zero limbs, handles zero and single-limb
     * operands separately, and selects a multiplication algorithm based
     * on the length of the smaller operand.
     * Recursive multiplication calls use the same thresholds.
     * </p>
     *
     * <h3>Algorithm Selection</h3>
     * <ul>
     *   <li>
     *     <b>Zero multiplication:</b>
     *     If either magnitude is zero, returns {@link #ZERO_MAG}.
     *   </li>
     *   <li>
     *     <b>Single-limb multiplication:</b>
     *     If the smaller operand contains exactly one limb, uses
     *     {@link #multiplySmall}.
     *   </li>
     *   <li>
     *     <b>Schoolbook multiplication:</b>
     *     When <code>min &lt; KT</code>, uses the classical
     *     O(n*m) multiplication algorithm, where n and m are
     *     the respective operand lengths.
     *   </li>
     *   <li>
     *     <b>Karatsuba multiplication:</b>
     *     When <code>KT &lt;= min &lt; T3T</code>, splits the
     *     operands into two parts and computes three recursive
     *     products instead of four.
     *   </li>
     *   <li>
     *     <b>Toom-Cook 3-way multiplication (Toom-3):</b>
     *     When <code>T3T &lt;= min &lt; T4T</code>, splits
     *     the operands into three parts and computes five
     *     recursive products.
     *   </li>
     *   <li>
     *     <b>Toom-Cook 4-way multiplication (Toom-4):</b>
     *     When <code>min &gt;= T4T</code>, splits the operands
     *     into four parts and computes seven recursive products.
     *   </li>
     * </ul>
     *
     * <h3>Karatsuba Multiplication</h3>
     * <p>
     * Each operand is divided into a lower and upper portion,
     * using <code>pivot = (max + 1) / 2</code>.
     * With B = 2^64:
     * </p>
     * <pre>
     * a = a0 + a1 * B^pivot
     * b = b0 + b1 * B^pivot
     *
     * z0 = a0 * b0
     * z2 = a1 * b1
     * z1 = (a0 + a1)(b0 + b1) - z0 - z2
     *
     * a*b = z0 + z1*B^pivot + z2*B^(2*pivot)
     * </pre>
     * <p>
     * This reduces four half-size multiplications to three,
     * giving O(n<sup> log2(3)</sup>), approximately O(n<sup>1.585</sup>),
     * time for balanced operands.
     * </p>
     *
     * <h3>Toom-Cook 3-Way Multiplication</h3>
     * <p>
     * The operands are divided into three parts using
     * <code>pivot = (max + 2) / 3</code>.
     * Each operand is interpreted as a degree-two polynomial:
     * </p>
     * <pre>
     * A(x) = a0 + a1*x + a2*x^2
     * B(x) = b0 + b1*x + b2*x^2
     * </pre>
     * <p>
     * The product polynomial C(x) = A(x)*B(x) has degree at most four.
     * It is determined by evaluating and multiplying the operand
     * polynomials at five points.
     * </p>
     * <ul>
     *   <li><code>x = 0</code>: constant coefficient product.</li>
     *   <li><code>x = 1</code>: sum of all operand parts.</li>
     *   <li><code>x = -1</code>: alternating sum of operand parts.</li>
     *   <li><code>x = 2</code>: weighted sum using powers of two.</li>
     *   <li><code>x = infinity</code>: highest-degree coefficient product.</li>
     * </ul>
     * <p>
     * Negative evaluations are represented using a separate sign
     * flag and an unsigned magnitude. The sign of the product
     * at -1 is determined from the operand evaluation signs.
     * </p>
     *
     * <p>
     * The five pointwise products are interpolated to recover
     * coefficients r0 through r4:
     * </p>
     * <pre>
     * C(x) = r0 + r1*x + r2*x^2 + r3*x^3 + r4*x^4
     *
     * r0 = v(0)
     * r4 = v(infinity)
     *
     * r2 = (v(1) + v(-1))/2 - r0 - r4
     * s  = (v(1) - v(-1))/2
     * T  = (v(2) - r0 - 16*r4)/2
     *
     * r3 = (T - s - 2*r2)/3
     * r1 = s - r3
     * </pre>
     * <p>
     * The resulting coefficients are recombined at offsets
     * 0, pivot, 2*pivot, 3*pivot, and 4*pivot limbs.
     * For balanced operands, the recursive algorithm has
     * complexity O(n^log3(5)), approximately O(n^1.465).
     * </p>
     *
     * <h3>Toom-Cook 4-Way Multiplication</h3>
     * <p>
     * The operands are divided into four parts using
     * <code>pivot = (max + 3) / 4</code>.
     * Each operand is interpreted as a degree-three polynomial:
     * </p>
     * <pre>
     * A(x) = a0 + a1*x + a2*x^2 + a3*x^3
     * B(x) = b0 + b1*x + b2*x^2 + b3*x^3
     * </pre>
     * <p>
     * Their product has degree at most six and therefore requires
     * seven independent point evaluations for interpolation.
     * The selected evaluation points are:
     * </p>
     * <ul>
     *   <li><code>x = 0</code>: constant coefficient product.</li>
     *   <li><code>x = 1</code>: sum of all four parts.</li>
     *   <li><code>x = -1</code>: alternating sum of the parts.</li>
     *   <li><code>x = 2</code>: evaluation with powers 1, 2, 4, 8.</li>
     *   <li><code>x = -2</code>: alternating evaluation with powers of two.</li>
     *   <li><code>x = 3</code>: evaluation with powers 1, 3, 9, 27.</li>
     *   <li><code>x = infinity</code>: highest-degree coefficient product.</li>
     * </ul>
     * <p>
     * As in Toom-3, negative evaluation results are stored as
     * unsigned magnitudes with separate sign flags.
     * The seven pointwise products are computed recursively.
     * </p>
     *
     * <h4>Interpolation</h4>
     * <p>
     * The product polynomial is expressed as:
     * </p>
     * <pre>
     * C(x) = r0 + r1*x + r2*x^2 + r3*x^3
     *           + r4*x^4 + r5*x^5 + r6*x^6
     * </pre>
     * <p>
     * The constant and highest-degree coefficients are obtained
     * directly:
     * </p>
     * <pre>
     * r0 = v(0)
     * r6 = v(infinity)
     * </pre>
     * <p>
     * The evaluations at 1, -1, 2, and -2 are combined to
     * separate even-degree and odd-degree coefficients.
     * </p>
     * <pre>
     * E1 = (v(1) + v(-1))/2 - r0 - r6
     *    = r2 + r4
     *
     * E2 = ((v(2) + v(-2))/2 - r0 - 64*r6)/4
     *    = r2 + 4*r4
     *
     * r4 = (E2 - E1)/3
     * r2 = E1 - r4
     *
     * O1 = (v(1) - v(-1))/2
     *    = r1 + r3 + r5
     *
     * O2 = (v(2) - v(-2))/4
     *    = r1 + 4*r3 + 16*r5
     * </pre>
     * <p>
     * The remaining odd-degree coefficients are determined using
     * the evaluation at 3:
     * </p>
     * <pre>
     * D = (O2 - O1)/3
     *   = r3 + 5*r5
     *
     * E = ((v(3) - r0 - 9*r2 - 81*r4 - 729*r6)/3
     *      - O1)/8
     *   = r3 + 10*r5
     *
     * r5 = (E - D)/5
     * r3 = D - 5*r5
     * r1 = O1 - r3 - r5
     * </pre>
     * <p>
     * All interpolation operations use exact integer arithmetic.
     * Divisions by 2, 4, and 8 are performed through bit shifts;
     * divisions by 3 and 5 use exact magnitude division.
     * </p>
     * <p>
     * The seven recovered coefficients are recombined at
     * successive multiples of {@code pivot} limbs.
     * For balanced operands, Toom-4 has asymptotic complexity
     * O(n^log4(7)), approximately O(n^1.404).
     * </p>
     *
     * <h3>Normalization and Result Construction</h3>
     * <ul>
     *   <li>
     *     Both operands are normalized by removing redundant
     *     most-significant zero limbs.
     *   </li>
     *   <li>
     *     The larger operand is placed first, ensuring that
     *     {@code max} and {@code min} refer to the larger and
     *     smaller operand lengths, respectively.
     *   </li>
     *   <li>
     *     A result array of length {@code max + min} is allocated
     *     to accommodate the full product.
     *   </li>
     *   <li>
     *     Recursive algorithms reconstruct the result by adding
     *     their interpolated coefficient magnitudes at the
     *     appropriate limb offsets.
     *   </li>
     *   <li>
     *     Redundant most-significant zero limbs are removed
     *     before returning the final product.
     *   </li>
     * </ul>
     *
     * <h3>Threshold Requirements</h3>
     * <p>
     * The three thresholds should satisfy
     * <code>1 &lt; KT &lt;= T3T &lt;= T4T</code>.
     * Threshold comparisons use the number of 64-bit limbs,
     * rather than the number of individual bits.
     * The method does not independently validate these thresholds.
     * </p>
     *
     * @param a the first nonnegative integer magnitude, represented
     *          as a little-endian array of unsigned 64-bit limbs
     * @param b the second nonnegative integer magnitude, using
     *          the same representation as {@code a}
     * @param KT the minimum smaller-operand limb count at which
     *           Karatsuba multiplication is selected
     * @param T3T the minimum smaller-operand limb count at which
     *            Toom-Cook 3-way multiplication is selected
     * @param T4T the minimum smaller-operand limb count at which
     *            Toom-Cook 4-way multiplication is selected
     * @return a normalized little-endian magnitude array containing
     *         the exact unsigned product of {@code a} and {@code b},
     *         or {@code ZERO_MAG} if either operand is zero
     */
    private static long[] multiplyMagnitudesWithThresholds(long[] a, long[] b, int KT, int T3T, int T4T) {
        a = trimLeadingZerosTrusted(a);
        b = trimLeadingZerosTrusted(b);
        if (isZeroMagnitude(a) || isZeroMagnitude(b)) return ZERO_MAG;
        if (a.length < b.length) {
            long[] t = a;
            a = b;
            b = t;
        }
        int max = a.length;
        int min = b.length;
        if (min == 1) return multiplySmall(a, b[0]);
        long[] out = new long[max + min];
        if (min < KT)
            return trimLeadingZerosTrusted(multiplyInto(a, b, out));
        else if (min < T3T) {
            int pivot = (max + 1) >>> 1;
            long[] lower1 = sliceFixed(a, 0, pivot);
            long[] upper1 = slice(a, pivot, max);
            long[] lower2 = sliceFixed(b, 0, Math.min(pivot, b.length));
            long[] upper2 = pivot >= b.length ? ZERO_MAG : slice(b, pivot, b.length);
            long[] z0 = multiplyMagnitudesWithThresholds(lower1, lower2, KT, T3T, T4T);
            long[] z2 = multiplyMagnitudesWithThresholds(upper1, upper2, KT, T3T, T4T);
            long[] z1 = multiplyMagnitudesWithThresholds(addMagnitudes(lower1, upper1),
                    addMagnitudes(lower2, upper2), KT, T3T, T4T);
            subtractMagnitudeInPlace(z1, z0);
            subtractMagnitudeInPlace(z1, z2);
            addMagnitudeInPlace(out, z0, 0);
            addMagnitudeInPlace(out, z1, pivot);
            addMagnitudeInPlace(out, z2, pivot * 2);
        } else if (min < T4T) {
            int pivot = (max + 2) / 3;

            long[] a0 = sliceFixed(a, 0, pivot);
            long[] a1 = sliceFixed(a, pivot, pivot);
            long[] a2 = sliceFixed(a, 2 * pivot, pivot);
            long[] b0 = sliceFixed(b, 0, pivot);
            long[] b1 = sliceFixed(b, pivot, pivot);
            long[] b2 = sliceFixed(b, 2 * pivot, pivot);

            long[] p1 = addThreeMagnitudes(a0, a1, a2);
            long[] pm1Magnitude = addMagnitudes(a0, a2);
            boolean pm1Negative = compareArray(pm1Magnitude, a1) < 0;
            if (!pm1Negative) subtractMagnitudeInPlace(pm1Magnitude, a1);
            else pm1Magnitude = subtractMagnitudes(a1, pm1Magnitude);
            long[] p2 = addThreeMagnitudes(a0, shiftLeftBits(a1, 1), shiftLeftBits(a2, 2));
            long[] q1 = addThreeMagnitudes(b0, b1, b2);
            long[] qm1Magnitude = addMagnitudes(b0, b2);
            boolean qm1Negative = compareArray(qm1Magnitude, b1) < 0;
            if (!qm1Negative) subtractMagnitudeInPlace(qm1Magnitude, b1);
            else qm1Magnitude = subtractMagnitudes(b1, qm1Magnitude);
            long[] q2 = addThreeMagnitudes(b0, shiftLeftBits(b1, 1), shiftLeftBits(b2, 2));

            long[] v0 = multiplyMagnitudesWithThresholds(a0, b0, KT, T3T, T4T);
            long[] v1 = multiplyMagnitudesWithThresholds(p1, q1, KT, T3T, T4T);
            long[] vm1Magnitude = multiplyMagnitudesWithThresholds(pm1Magnitude, qm1Magnitude, KT, T3T, T4T);
            boolean vm1Negative = pm1Negative ^ qm1Negative;
            long[] v2 = multiplyMagnitudesWithThresholds(p2, q2, KT, T3T, T4T);
            long[] vInf = multiplyMagnitudesWithThresholds(a2, b2, KT, T3T, T4T);

            long[] r2;
            long[] s;
            if (vm1Negative) {
                // v1 + vm1 = v1 - |vm1|
                r2 = subtractMagnitudes(v1, vm1Magnitude);
                // v1 - vm1 = v1 + |vm1|
                s = addMagnitudes(v1, vm1Magnitude);
            } else {
                r2 = addMagnitudes(v1, vm1Magnitude);
                s = subtractMagnitudes(v1, vm1Magnitude);
            }
            shiftRightBitsInPlace(r2, 1);
            shiftRightBitsInPlace(s, 1);
            // r2 = (v1 + vm1)/2 - v0 - vInf
            subtractMagnitudeInPlace(r2, v0);
            subtractMagnitudeInPlace(r2, vInf);
            // v2 -> T
            subtractMagnitudeInPlace(v2, v0);
            subtractShiftedLeftBitsInPlace(v2, vInf, 4, "T: v2 -= 16*vInf");
            shiftRightBitsInPlace(v2, 1);
            // v2 -> r3
            subtractMagnitudeInPlace(v2, s);
            subtractShiftedLeftBitsInPlace(v2, r2, 1, "r3: T-s -= 2*r2");
            divideMagnitudeExactInPlace(v2, 3);
            // s -> r1
            subtractMagnitudeInPlace(s, v2);

            addMagnitudeInPlace(out, v0, 0);
            addMagnitudeInPlace(out, s, pivot);
            addMagnitudeInPlace(out, r2, 2 * pivot);
            addMagnitudeInPlace(out, v2, 3 * pivot);
            addMagnitudeInPlace(out, vInf, 4 * pivot);
        } else {
            if (bitLength(a) + bitLength(b) - 1 > MAX_BITS)
                reportOverflow();
            //split into four parts
            int pivot = (max + 3) >>> 2;
            long[] a0 = sliceFixed(a, 0, pivot);
            long[] a1 = sliceFixed(a, pivot, pivot);
            long[] a2 = sliceFixed(a, 2 * pivot, pivot);
            long[] a3 = sliceFixed(a, 3 * pivot, pivot);
            long[] b0 = sliceFixed(b, 0, pivot);
            long[] b1 = sliceFixed(b, pivot, pivot);
            long[] b2 = sliceFixed(b, 2 * pivot, pivot);
            long[] b3 = sliceFixed(b, 3 * pivot, pivot);
            //point evaluation. points 0, -1, 1, -2, 2, 3, ∞
            long[] p1 = addFourMagnitudes(a0, a1, a2, a3);
            long[] p2 = addFourMagnitudes(a0, shiftLeftBits(a1, 1), shiftLeftBits(a2, 2), shiftLeftBits(a3, 3));
            long[] p3 = addFourMagnitudes(a0, multiplySmall(a1, 3), multiplySmall(a2, 9), multiplySmall(a3, 27));
            long[] pM1Magnitude = addMagnitudes(a2, a0);
            long[] pM1Subtract = addMagnitudes(a3, a1);
            boolean pM1Neg = compareArray(pM1Magnitude, pM1Subtract) < 0;
            if (!pM1Neg) subtractMagnitudeInPlace(pM1Magnitude, pM1Subtract);
            else pM1Magnitude = subtractMagnitudes(pM1Subtract, pM1Magnitude);
            long[] pM2Magnitude = addMagnitudes(a0, shiftLeftBits(a2, 2));
            long[] pM2Subtract = addMagnitudes(shiftLeftBits(a1, 1), shiftLeftBits(a3, 3));
            boolean pM2Neg = compareArray(pM2Magnitude, pM2Subtract) < 0;
            if (!pM2Neg) subtractMagnitudeInPlace(pM2Magnitude, pM2Subtract);
            else pM2Magnitude = subtractMagnitudes(pM2Subtract, pM2Magnitude);
            long[] q1 = addFourMagnitudes(b0, b1, b2, b3);
            long[] q2 = addFourMagnitudes(b0, shiftLeftBits(b1, 1), shiftLeftBits(b2, 2), shiftLeftBits(b3, 3));
            long[] q3 = addFourMagnitudes(b0, multiplySmall(b1, 3), multiplySmall(b2, 9), multiplySmall(b3, 27));
            long[] qM1Magnitude = addMagnitudes(b2, b0);
            long[] qM1Subtract = addMagnitudes(b3, b1);
            boolean qM1Neg = compareArray(qM1Magnitude, qM1Subtract) < 0;
            if (!qM1Neg) subtractMagnitudeInPlace(qM1Magnitude, qM1Subtract);
            else qM1Magnitude = subtractMagnitudes(qM1Subtract, qM1Magnitude);
            long[] qM2Magnitude = addMagnitudes(b0, shiftLeftBits(b2, 2));
            long[] qM2Subtract = addMagnitudes(shiftLeftBits(b1, 1), shiftLeftBits(b3, 3));
            boolean qM2Neg = compareArray(qM2Magnitude, qM2Subtract) < 0;
            if (!qM2Neg) subtractMagnitudeInPlace(qM2Magnitude, qM2Subtract);
            else qM2Magnitude = subtractMagnitudes(qM2Subtract, qM2Magnitude);
            //multiply the points
            long[] v0 = multiplyMagnitudesWithThresholds(a0, b0, KT, T3T, T4T);
            long[] v1 = multiplyMagnitudesWithThresholds(p1, q1, KT, T3T, T4T);
            long[] v2 = multiplyMagnitudesWithThresholds(p2, q2, KT, T3T, T4T);
            long[] v3 = multiplyMagnitudesWithThresholds(p3, q3, KT, T3T, T4T);
            long[] vM1Magnitude = multiplyMagnitudesWithThresholds(pM1Magnitude, qM1Magnitude, KT, T3T, T4T);
            boolean vM1Neg = pM1Neg ^ qM1Neg;
            long[] vM2Magnitude = multiplyMagnitudesWithThresholds(pM2Magnitude, qM2Magnitude, KT, T3T, T4T);
            boolean vM2Neg = pM2Neg ^ qM2Neg;
            long[] vInf = multiplyMagnitudesWithThresholds(a3, b3, KT, T3T, T4T);
            //INTERPOLATE
            // diff = (v1 - |v(-1)|) / 2
            subtractMagnitudeInPlace(v1, vM1Magnitude);
            shiftRightBitsInPlace(v1, 1);
            // other half = |v(-1)| + diff
            vM1Magnitude = addMagnitudes(vM1Magnitude, v1);
            if (!vM1Neg) {
                // sign positive:
                // v1 currently odd half, vM1Magnitude even half
                long[] t = v1;
                v1 = vM1Magnitude;
                vM1Magnitude = t;
            }
            // now:
            // v1     = even half = r0+r2+r4+r6
            // vM1Magnitude = O1        = r1+r3+r5
            subtractMagnitudeInPlace(v1, v0);
            subtractMagnitudeInPlace(v1, vInf);
            // v1 = r2+r4

            subtractMagnitudeInPlace(v2, vM2Magnitude);
            shiftRightBitsInPlace(v2, 1);

            vM2Magnitude = addMagnitudes(vM2Magnitude, v2);

            if (!vM2Neg) {
                long[] t = v2;
                v2 = vM2Magnitude;
                vM2Magnitude = t;
            }
            // v2     = (v(2)+v(-2))/2
            // vM2Magnitude = (v(2)-v(-2))/2

            subtractMagnitudeInPlace(v2, v0);
            subtractMagnitudeInPlace(v2, shiftLeftBits(vInf, 6));
            shiftRightBitsInPlace(v2, 2);
            // v2 = r2 + 4*r4
            shiftRightBitsInPlace(vM2Magnitude, 1);
            // vM2Magnitude = O2 = r1 + 4*r3 + 16*r5
            subtractMagnitudeInPlace(v2, v1);
            divideMagnitudeExactInPlace(v2, 3);
            // v2 = r4
            subtractMagnitudeInPlace(v1, v2);
            // v1 = r2

            subtractMagnitudeInPlace(v3, v0);
            subtractMagnitudeInPlace(v3, multiplySmall(v1, 9));
            subtractMagnitudeInPlace(v3, multiplySmall(v2, 81));
            subtractMagnitudeInPlace(v3, multiplySmall(vInf, 729));
            divideMagnitudeExactInPlace(v3, 3);
            // v3 = r1 + 9*r3 + 81*r5, vM2Magnitude -> D = r3 + 5*r5
            subtractMagnitudeInPlace(vM2Magnitude, vM1Magnitude);
            divideMagnitudeExactInPlace(vM2Magnitude, 3);
            // v3 -> E = r3 + 10*r5
            subtractMagnitudeInPlace(v3, vM1Magnitude);
            shiftRightBitsInPlace(v3, 3);
            // v3 -> 5*r5
            subtractMagnitudeInPlace(v3, vM2Magnitude);
            // vM2Magnitude -> r3, while v3 still contains 5*r5
            subtractMagnitudeInPlace(vM2Magnitude, v3);
            // v3 -> r5
            divideMagnitudeExactInPlace(v3, 5);
            // vM1Magnitude -> r1
            subtractMagnitudeInPlace(vM1Magnitude, vM2Magnitude);
            subtractMagnitudeInPlace(vM1Magnitude, v3);
            //recombine
            addMagnitudeInPlace(out, v0, 0);
            addMagnitudeInPlace(out, vM1Magnitude, pivot);
            addMagnitudeInPlace(out, v1, 2 * pivot);
            addMagnitudeInPlace(out, vM2Magnitude, 3 * pivot);
            addMagnitudeInPlace(out, v2, 4 * pivot);
            addMagnitudeInPlace(out, v3, 5 * pivot);
            addMagnitudeInPlace(out, vInf, 6 * pivot);
        }
        return trimLeadingZerosTrusted(out);
    }
    /**
     * Returns a LargeInteger whose value is <code>this * other</code>.
     * @param other The value this integer is multiplied by
     * @return The product of these two integers
     * @apiNote This method dispatches to {@link #square()} if the inputs are equal
     */
    public LargeInteger multiply(LargeInteger other) {
        if (isZero() || other.isZero()) return ZERO;
        return equals(other) ? square() :
                new LargeInteger(multiplyMagnitudes(limbs, other.limbs),
                        negative ^ other.negative, true);
    }
    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        return switch (other) {
            case LargeInteger I -> multiply(I);
            case LargeFraction F -> new LargeFraction(multiply(F.numerator), F.denominator);
            case LargePowerMonomial largePowerMonomial -> new LargePowerMonomial(
                    LargeFraction.valueOf(this), LargeFraction.ONE).multiply(largePowerMonomial);
            case RadicalTermSum RTS -> {
                LargeMap<LargePowerMonomial, LargeFraction> rt = new LargeMap<>();
                for (LargePowerMonomial L : RTS.sum.keys())
                    rt.put(L, (LargeFraction) RTS.sum.get(L).multiply(this));
                yield new RadicalTermSum(rt, (LargeFraction) multiply(RTS.constant));
            } case LargePowerNode LPN -> new LargePowerNode(LPN.base.multiply(power(LPN.exponent.reciprocal())),
                    LPN.exponent, LPN.signFactor);
            default -> other.multiply(this);
        };
    }
    /**
     * Returns a LargeInteger whose value is <code>this * other</code>.
     * @param other The value this integer is multiplied by
     * @return The product of these two integers
     */
    LargeInteger multiply(long other) {
        boolean neg = other < 0;
        return new LargeInteger(multiplySmall(limbs, neg ? -other : other), negative ^ neg, true);
    }

    /*
     * The following constants are more multiplication dispatch thresholds.
     * Once again, these constants are intentionally implementation-specific. They are chosen
     * experimentally from benchmark heatmaps for the current limb representation
     * and multiplication implementations.
     * In this build:
     * below KARATSUBA_SQUARE_THRESHOLD: grade-school squaring is fastest
     * below TOOM_3_SQUARE_THRESHOLD: Karatsuba squaring is fastest
     * below TOOM_4_SQUARE_THRESHOLD: Toom-Cook 3 squaring is fastest
     * at or above TOOM_COOK_4_THRESHOLD: Toom-Cook 4 squaring is fastest
     * Again, do not change these based on asymptotic complexity alone; re-benchmark first.
     */

    /**
     * Threshold for switching from grade-school to Karatsuba in squaring. If the number of limbs
     * in the smaller operand is in the range from this to the Toom-Cook 3 threshold, then the
     * Karatsuba algorithm will be chosen. This value was found via threshold benchmarks to work well.
     */
    private static final int KARATSUBA_SQUARE_THRESHOLD = 48;
    /**
     * Threshold for switching from Karatsuba to Toom-Cook 3 in squaring. If the number of limbs
     * in the smaller operand is in the range from this to the Toom-Cook 4 threshold, then the
     * Toom-Cook 3 algorithm will be chosen. This value was found via threshold benchmarks to work well.
     */
    private static final int TOOM_3_SQUARE_THRESHOLD = 128;
    /**
     * Threshold for switching from Toom-Cook 3 to Toom-Cook 4 in squaring. If the number of limbs
     * in the smaller operand is greater than or equal to this, then the Toom-Cook 4 algorithm will
     * be chosen. This value was found via threshold benchmarks to work well.
     */
    private static final int TOOM_4_SQUARE_THRESHOLD = 520;

    /**
     * Computes <code>a<sup>2</sup></code> and writes the result into the output buffer, truncating if necessary.
     * Exploits the identity (∑<sub>k=0</sub><sup>n</sup>(a<sub>k</sub>*β<sup>k</sup>))<sup>2</sup>
     * = ∑<sub>k=0</sub><sup>n</sup>(a<sub>k</sub><sup>2</sup>*β<sup>2k</sup>)
     * + 2*∑<sub>k=0</sub><sup>n</sup>∑<sub>j=k+1</sub><sup>n</sup>(a<sub>k</sub>*a<sub>j</sub>*β<sup>k+j</sup>).
     */
    private static long[] squareInto(long[] a, long[] out) {
        int n = a.length;
        int m = Math.min(out.length, n << 1);
        if (m == 0) return out;
        // If out is larger than the full product, clear the unused tail.
        if (out.length > (n << 1))
            Arrays.fill(out, n << 1, out.length, 0);
        /*
         * Store the low m limbs of floor(D / 2), where
         *
         * D = Σ a[i]^2 * B^(2i)
         *
         * At the truncation boundary we intentionally ignore the bit
         * coming from D's first discarded limb. After the final << 1
         * that bit would itself be shifted outside out[], so it cannot
         * affect the retained product.
         */
        long shiftCarry = 0L;
        for (int i = Math.min(n - 1, (m - 1) >>> 1); i >= 0; i--) {
            long ai = a[i];
            long low  = ai * ai;
            long high = Math.unsignedMultiplyHigh(ai, ai);
            int k = i << 1;
            if (k + 1 < m)
                out[k + 1] = (high >>> 1) | shiftCarry;
            shiftCarry = high << 63;
            out[k] = (low >>> 1) | shiftCarry;
            shiftCarry = low << 63;
        }
        /*
         * Add
         *
         * O = Σ(i<j) a[i]a[j] B^(i+j)
         *
         * Only terms with i+j < m can affect the retained limbs.
         */
        for (int i = 0; i < n; i++) {
            int j = i + 1;
            if (j >= n || i + j >= m) break;
            long ai = a[i];
            long carry = 0L;
            for (int jLimit = Math.min(n, m - i); j < jLimit; j++) {
                int k = i + j;
                long aj = a[j];
                long low  = ai * aj;
                long high = Math.unsignedMultiplyHigh(ai, aj);
                long t = low + out[k];
                long c1 = Long.compareUnsigned(t, low) < 0 ? 1L : 0L;
                long lo = t + carry;
                long c2 = Long.compareUnsigned(lo, t) < 0 ? 1L : 0L;
                out[k] = lo;
                carry = high + c1 + c2;
            }
            j += i;
            while (carry != 0L && j < m) {
                long old = out[j];
                long sum = old + carry;
                out[j++] = sum;
                carry = Long.compareUnsigned(sum, old) < 0 ? 1L : 0L;
            }
            // Any carry beyond out.length is deliberately discarded.
        }
        // Multiply floor(D/2) + O by 2.
        long bitCarry = 0L;
        for (int i = 0; i < m; i++) {
            long x = out[i];
            out[i] = (x << 1) | bitCarry;
            bitCarry = x >>> 63;
        }
        // Restore the one bit lost by floor(D/2).
        out[0] |= a[0] & 1L;
        return out;
    }

    /**
     * Method that computes and returns <code>A<sup>2</sup></code>.
     * There are several methods used in this hierarchy.
     * <p> Grade School multiplication. See {@link #squareInto(long[], long[])}.
     * <p> Karatsuba multiplication. The input <code>A</code> can be written as <code>a1*β + a0</code>.
     *     Using recursion, compute <code>z0 = a0<sup>2</sup></code>, <code>z2 = a1<sup>2</sup></code>,
     *     and <code>z1 = (a0+a1)<sup>2</sup> - z2 - z0</code>.
     *     Then the result is <code>z0 + z1*β + z2*β<sup>2</sup></code>.
     * <p> Toom-Cook 3 multiplication. This method introduces point evaluation, and involves much more
     *     complex interpolation. Karatsuba (also can be called Toom-2) used points 0, 1 and ∞. This branch
     *     uses points 0, 1 2, -1 and ∞.
     * <p>
     *     Toom-Cook 4 multiplication. Same as previous branch, except with points 0, 1, 2, -1, -2, 3 and ∞.
     * </p>
     * @param A The input to be squared.
     * @param KT See {@link #KARATSUBA_SQUARE_THRESHOLD}.
     * @param T3T See {@link #TOOM_3_SQUARE_THRESHOLD}.
     * @param T4T See {@link #TOOM_4_SQUARE_THRESHOLD}.
     * @return A<sup>2</sup>
     */
    private static long[] squareMagnitudeWithThresholds(long[] A, int KT, int T3T, int T4T) {
        if (isZeroMagnitude(A)) return A;
        int n = A.length;
        if (n == 1) {
            long a0 = A[0];
            long low = a0*a0;
            long high = Math.unsignedMultiplyHigh(a0, a0);
            return high == 0 ? new long[]{low} : new long[]{low, high};
        }
        long[] out = new long[n << 1];
        if (n < KT)
            //Grade-school
            return trimLeadingZerosTrusted(squareInto(A, out));
        else if (n < T3T) {
            //Karatsuba
            int m = (n + 1) >>> 1;
            long[] x0 = sliceFixed(A, 0, m);
            long[] x1 = slice(A, m, n);
            long[] z0 = squareMagnitudeWithThresholds(x0, KT, T3T, T4T);
            long[] z2 = squareMagnitudeWithThresholds(x1, KT, T3T, T4T);
            long[] z1 = squareMagnitudeWithThresholds(addMagnitudes(x0, x1), KT, T3T, T4T);
            subtractMagnitudeInPlace(z1, z0);
            subtractMagnitudeInPlace(z1, z2);
            addMagnitudeInPlace(out, z0, 0);
            addMagnitudeInPlace(out, z1, m);
            addMagnitudeInPlace(out, z2, 2 * m);
        } else if (n < T4T) {
            //Toom-Cook 3
            int m = (n + 2) / 3;
            //A = a2*B^2 + a1*B + a0
            long[] a0 = sliceFixed(A, 0, m);
            long[] a1 = sliceFixed(A, m, m);
            long[] a2 = slice(A, m * 2, n);
            //evaluate points
            long[] p1 = addThreeMagnitudes(a2, a1, a0);
            long[] pm1 = addMagnitudes(a2, a0);
            if (compareArray(pm1, a1) >= 0) subtractMagnitudeInPlace(pm1, a1);
            else pm1 = subtractMagnitudes(a1, pm1);
            long[] p2 = addThreeMagnitudes(shiftLeftBits(a2, 2), shiftLeftBits(a1, 1), a0);
            //square each point
            long[] v0 = squareMagnitudeWithThresholds(a0, KT, T3T, T4T);
            long[] v1 = squareMagnitudeWithThresholds(p1, KT, T3T, T4T);
            long[] vm1 = squareMagnitudeWithThresholds(pm1, KT, T3T, T4T);
            long[] v2 = squareMagnitudeWithThresholds(p2, KT, T3T, T4T);
            long[] vInf = squareMagnitudeWithThresholds(a2, KT, T3T, T4T);
            //interpolate
            long[] s = subtractMagnitudes(v1, vm1);
            shiftRightBitsInPlace(s, 1);
            // v1 -> r2
            subtractMagnitudeInPlace(v1, s);
            subtractMagnitudeInPlace(v1, v0);
            subtractMagnitudeInPlace(v1, vInf);
            // v2 -> t
            subtractMagnitudeInPlace(v2, v0);
            subtractShiftedLeftBitsInPlace(v2, vInf, 4, null);
            shiftRightBitsInPlace(v2, 1);

            // t -> r3
            subtractMagnitudeInPlace(v2, s);
            subtractShiftedLeftBitsInPlace(v2, v1, 1, null);
            divideMagnitudeExactInPlace(v2, 3);
            // s -> r1
            subtractMagnitudeInPlace(s, v2);
            //recombine
            addMagnitudeInPlace(out, v0, 0);
            addMagnitudeInPlace(out, s, m);
            addMagnitudeInPlace(out, v1, 2 * m);
            addMagnitudeInPlace(out, v2, 3 * m);
            addMagnitudeInPlace(out, vInf, 4 * m);
        } else {
            if (bitLength(A)*2-1 > MAX_BITS)
                reportOverflow();
            //Initialize the coefficients
            int pivot = (n + 3) >>> 2;
            long[] a0 = sliceFixed(A, 0, pivot);
            long[] a1 = sliceFixed(A, pivot, pivot);
            long[] a2 = sliceFixed(A, 2 * pivot, pivot);
            long[] a3 = sliceFixed(A, 3 * pivot, pivot);
            //Define and evaluate the points
            long[] p1 = addFourMagnitudes(a0, a1, a2, a3);
            long[] p2 = addFourMagnitudes(
                    shiftLeftBits(a3, 3), shiftLeftBits(a2, 2), shiftLeftBits(a1, 1), a0);
            long[] p3 = addFourMagnitudes(
                    multiplySmall(a3, 27), multiplySmall(a2, 9), multiplySmall(a1, 3), a0);
            long[] pM1 = addMagnitudes(a2, a0);
            long[] pM1Subtract = addMagnitudes(a3, a1);
            if (compareArray(pM1, pM1Subtract) >= 0)
                subtractMagnitudeInPlace(pM1, pM1Subtract);
            else pM1 = subtractMagnitudes(pM1Subtract, pM1);
            long[] pM2 = addMagnitudes(shiftLeftBits(a2, 2), a0);
            long[] pM2Subtract = addMagnitudes(shiftLeftBits(a3, 3), shiftLeftBits(a1, 1));
            if (compareArray(pM2, pM2Subtract) >= 0)
                subtractMagnitudeInPlace(pM2, pM2Subtract);
            else pM2 = subtractMagnitudes(pM2Subtract, pM2);
            //Square the points
            long[] v0 = squareMagnitudeWithThresholds(a0, KT, T3T, T4T);
            long[] v1 = squareMagnitudeWithThresholds(p1, KT, T3T, T4T);
            long[] v2 = squareMagnitudeWithThresholds(p2, KT, T3T, T4T);
            long[] v3 = squareMagnitudeWithThresholds(p3, KT, T3T, T4T);
            long[] vM1 = squareMagnitudeWithThresholds(pM1, KT, T3T, T4T);
            long[] vM2 = squareMagnitudeWithThresholds(pM2, KT, T3T, T4T);
            long[] vInf = squareMagnitudeWithThresholds(a3, KT, T3T, T4T);
            //Interpolate. Start with E1 = r2 + r4
            long[] E1 = addMagnitudes(v1, vM1);
            shiftRightBitsInPlace(E1, 1);
            subtractMagnitudeInPlace(E1, v0);
            subtractMagnitudeInPlace(E1, vInf);
            // E2 = r2 + 4*r4
            long[] E2 = addMagnitudes(v2, vM2);
            shiftRightBitsInPlace(E2, 1);
            subtractMagnitudeInPlace(E2, v0);
            subtractMagnitudeInPlace(E2, shiftLeftBits(vInf, 6));
            shiftRightBitsInPlace(E2, 2);
            // E2 -> r4
            subtractMagnitudeInPlace(E2, E1);
            divideMagnitudeExactInPlace(E2, 3);
            // E1 -> r2
            subtractMagnitudeInPlace(E1, E2);
            // v1 = O1
            subtractMagnitudeInPlace(v1, vM1);
            shiftRightBitsInPlace(v1, 1);
            // v1 = O1 = r1+r3+r5

            // v2 = O2
            subtractMagnitudeInPlace(v2, vM2);
            shiftRightBitsInPlace(v2, 2);

            // v3 = O3
            subtractMagnitudeInPlace(v3, v0);
            subtractMagnitudeInPlace(v3, multiplySmall(E1, 9));
            subtractMagnitudeInPlace(v3, multiplySmall(E2, 81));
            subtractMagnitudeInPlace(v3, multiplySmall(vInf, 729));
            divideMagnitudeExactInPlace(v3, 3);

            // v2 = D = r3 + 5r5
            subtractMagnitudeInPlace(v2, v1);
            divideMagnitudeExactInPlace(v2, 3);

            // v3 = E = r3 + 10r5
            subtractMagnitudeInPlace(v3, v1);
            shiftRightBitsInPlace(v3, 3);

            // v3 = 5r5
            subtractMagnitudeInPlace(v3, v2);

            // v2 = r3
            subtractMagnitudeInPlace(v2, v3);

            // v3 = r5
            divideMagnitudeExactInPlace(v3, 5);
            // v1 = r1
            subtractMagnitudeInPlace(v1, v2);
            subtractMagnitudeInPlace(v1, v3);
            //recombine
            addMagnitudeInPlace(out, v0, 0);
            addMagnitudeInPlace(out, v1, pivot);
            addMagnitudeInPlace(out, E1, 2 * pivot);
            addMagnitudeInPlace(out, v2, 3 * pivot);
            addMagnitudeInPlace(out, E2, 4 * pivot);
            addMagnitudeInPlace(out, v3, 5 * pivot);
            addMagnitudeInPlace(out, vInf, 6 * pivot);
        }
        return trimLeadingZerosTrusted(out);
    }
    /**
     * Squares a magnitude with the specified thresholds. See
     * {@link #squareMagnitudeWithThresholds(long[], int, int, int)} for more details.
     */
    private static long[] squareMagnitude(long[] a) {
        return squareMagnitudeWithThresholds(a, KARATSUBA_SQUARE_THRESHOLD, TOOM_3_SQUARE_THRESHOLD,
                TOOM_4_SQUARE_THRESHOLD);
    }

    /**
     * Computes x<sup>2</sup> * x. Used for curve creation in ECM and {@link #cube()}.
     */
    private static long[] cubeMagnitude(long[] x) {return multiplyMagnitudes(squareMagnitude(x), x);}

    /**
     * Returns a LargeInteger whose value is <code>this<sup>2</sup></code>.
     * @return The square of this integer
     */
    @Override
    public LargeInteger square()
    {return isZero() ? ZERO : isOne() ? ONE : new LargeInteger(squareMagnitude(limbs));}

    /**
     * Returns a LargeInteger whose value is <code>this<sup>3</sup></code>.
     * @return The cube of this integer
     */
    public LargeInteger cube() {return new LargeInteger(cubeMagnitude(limbs), negative, true);}

    private static LargeInteger productRange(LargeInteger bottom, LargeInteger top) {
        if (bottom.equals(top)) return bottom;
        if (bottom.add(1).equals(top)) return bottom.multiply(top);
        LargeInteger mid = bottom.add(top).shiftRight(1);
        return productRange(bottom, mid).multiply(productRange(mid.add(1), top));
    }

    /**
     * Returns a LargeInteger whose value is <code>this!</code>.
     */
    public LargeInteger factorial() {
        if (isNegative()) throw new ArithmeticException("Factorial not defined for negative integers");
        if (isZero() || isOne()) return ONE;
        return productRange(ONE, this);
    }

    //--- Division ---//

    private static final long U32_MASK = 0xFFFF_FFFFL;
    private static final long U32_BASE = 1L << 32;
    private static long divide128ByNormalized64Quotient(long high, long low, long divisor) {
        // assumes divisor normalized, high < divisor unsigned
        int u3 = (int) (high >>> 32);
        int u2 = (int) high;
        int u1 = (int) (low >>> 32);
        int u0 = (int) low;

        int v1 = (int) (divisor >>> 32);
        int v0 = (int) divisor;
        /*
         * First 96/64 division.
         */
        long qHigh = divideThreeByTwo(u3, u2, u1, v1, v0);
        /*
         * Remainder after first division.
         *
         * Low 64 bits of [u3 u2 u1] are [u2 u1].
         */
        long rem = (((u2 & U32_MASK) << 32) | (u1 & U32_MASK)) - qHigh * divisor;

        /*
         * Bring down u0 and get second quotient word.
         */
        return (qHigh << 32) | divideThreeByTwo((int) (rem >>> 32), (int) rem, u0, v1, v0);
    }
    private static long divideThreeByTwo(int a1, int a2, int a3, int b1, int b2) {
        long A1 = a1 & U32_MASK;
        long A2 = a2 & U32_MASK;
        long A3 = a3 & U32_MASK;
        long B1 = b1 & U32_MASK;
        long B2 = b2 & U32_MASK;
        if (B1 == 0) throw new ArithmeticException("Division by zero");
        if (A1 > B1 || (A1 == B1 && A2 >= B2))
            throw new IllegalArgumentException("Three-by-two quotient does not fit one word");
        if ((b1 & 0x8000_0000) == 0) throw new IllegalArgumentException("Divisor must be normalized");
        long q;
        long rHat;
        if (A1 == B1) {
            q = U32_MASK;
            rHat = A2 + B1;
        } else {
            long top = (A1 << 32) | A2;
            q = Long.divideUnsigned(top, B1);
            rHat = top - B1*q;
        }
        long qTimesB2 = q * B2;
        while (rHat < U32_BASE && Long.compareUnsigned(qTimesB2, (rHat << 32) | A3) > 0) {
            q--;
            rHat += B1;
            qTimesB2 -= B2;
        }
        return q;
    }
    /**
     * Divides an unsigned, little-endian magnitude by an unsigned 64-bit divisor,
     * replacing the magnitude with the exact quotient.
     *
     * <p>This method assumes that the dividend is exactly divisible by the divisor.
     * Rather than performing conventional long division, it computes a modular
     * inverse using Newton–Hensel lifting and applies that inverse to every limb
     * in O(n) time, where n is the number of limbs.
     *
     * <h3>Step 1: Remove powers of two</h3>
     *
     * <p>Write the unsigned divisor as d = 2<sup>s</sup> * m, where m is odd and
     * s is the number of trailing zero bits of d.
     * Exact divisibility requires the lowest s bits of the dividend to be zero.
     * After verifying this, the dividend is shifted right by s bits and the
     * divisor is replaced by m.
     *
     * <p>If m = 1, the shift alone completes the division.
     *
     * <h3>Step 2: Newton–Hensel lifting</h3>
     *
     * <p>Because the remaining divisor d is odd, gcd(d, 2^64) = 1.
     * Therefore, d has a unique multiplicative inverse modulo 2^64.
     *
     * <p>We seek an integer x satisfying:
     * <pre>
     *     d*x ≡ 1 (mod 2^64)
     * </pre>
     *
     * <p>Every odd integer satisfies d^2 ≡ 1 (mod 8), so x = d
     * is already a valid inverse modulo 8.
     *
     * <p>Newton–Hensel lifting improves this approximation through:
     * <pre>
     *     x<sub>new</sub> = x * (2 - d*x)
     * </pre>
     *
     * <p>If d*x ≡ 1 (mod 2^k), then:
     * <pre>
     *     1 - d*x<sub>new</sub> = (1 - d*x)^2
     * </pre>
     *
     * <p>Thus, x<sub>new</sub> is correct modulo 2^(2k). Each iteration at least
     * doubles the number of correct low-order bits.
     *
     * <p>Starting with 3 correct bits, five iterations guarantee at least:
     * <pre>
     *     3 -> 6 -> 12 -> 24 -> 48 -> 96 bits
     * </pre>
     *
     * <p>This exceeds the required 64 bits. Java's overflowing long
     * multiplication naturally performs arithmetic modulo 2^64,
     * so no explicit modular reduction is necessary.
     *
     * <h3>Step 3: Linear-time exact division</h3>
     *
     * <p>Let B = 2^64. Process the dividend's limbs from least significant
     * to most significant, maintaining a carry c, initially zero.
     *
     * <p>For each dividend limb a[i], determine the quotient limb q by:
     * <pre>
     *     q = (a[i] - c) * inverse (mod B)
     * </pre>
     *
     * <p>Since inverse*d ≡ 1 (mod B), this guarantees:
     * <pre>
     *     q*d + c ≡ a[i] (mod B)
     * </pre>
     *
     * <p>The next carry is the upper 64 bits of the full unsigned sum:
     * <pre>
     *     c_next = floor((q*d + c) / B)
     * </pre>
     *
     * <p>This is computed using {@link Math#unsignedMultiplyHigh(long, long)}, with an
     * additional increment if adding c to the low product overflows.
     *
     * <p>The quotient limb q overwrites a[i]. Since each iteration accesses
     * only the current dividend limb and a constant number of local variables,
     * the division requires O(n) time and O(1) auxiliary space.
     *
     * <p>After processing all limbs, the quotient Q and original shifted
     * dividend A satisfy:
     * <pre>
     *     d*Q = A + c_final * B^n
     * </pre>
     *
     * <p>Exact divisibility requires c_final = 0. A nonzero final carry
     * indicates that the caller violated the exact-divisibility precondition.
     *
     * @param a the nonempty, little-endian unsigned magnitude; overwritten
     *          with the exact quotient
     * @param divisor the nonzero divisor, interpreted as an unsigned long
     * @throws ArithmeticException if the divisor is zero or the dividend
     *         fails the divisibility check for the power-of-two factor
     * @throws AssertionError if the final carry is nonzero, indicating that
     *         the dividend was not exactly divisible by the odd divisor. Normally
     *         {@link java.lang.ArithmeticException} would be thrown, but the input
     *         has already been corrupted and the original cannot be recovered with
     *         this method alone.
     */
    private static void divideMagnitudeExactInPlace(long[] a, long divisor) {
        if (divisor == 0L)
            throw new ArithmeticException("divide by zero");
        int shift = Long.numberOfTrailingZeros(divisor);
        if (shift != 0) {
            if ((a[0] & ((1L << shift) - 1L)) != 0L)
                throw new ArithmeticException(new LargeInteger(a) + " is not exactly divisible by "
                        + Long.toUnsignedString(divisor));
            shiftRightBitsInPlace(a, shift);
            divisor >>>= shift;
        }
        if (divisor == 1L) return;
        long inverse = divisor;
        for (int k = 0; k < 5; k++)
            inverse *= 2L - divisor * inverse;
        long carry = 0L;
        for (int i = 0; i < a.length; i++) {
            long q = (a[i] - carry) * inverse;
            long low = q * divisor;
            long high = Math.unsignedMultiplyHigh(q, divisor);
            if (Long.compareUnsigned(low + carry, low) < 0) high++;
            a[i] = q;
            carry = high;
        }
        if (carry != 0L)
            throw new AssertionError("divideMagnitudeExactInPlace: result was corrupted; " +
                    Long.toUnsignedString(divisor) + " was not a divisor of the original");
    }
    private record DivRem(long[] quotient, long remainder) {}
    /**
     * Divides a magnitude by an unsigned divisor. Unlike {@link #divideMagnitudeExactInPlace(long[], long)},
     * this method does not assume that the quotient is exact, and returns a record containing the quotient
     * and the remainder.
     */
    private static DivRem divideByLong(long[] a, long divisor) {
        if (divisor == 0)
            throw new ArithmeticException("Division by zero");
        if (divisor == 1L)
            return new DivRem(trimLeadingZerosTrusted(a), 0L);
        long[] q = new long[a.length];
        long rem = 0L;
        // These depend only on divisor: do them once.
        int shift = Long.numberOfLeadingZeros(divisor);
        long d = divisor << shift;
        int v1 = (int) (d >>> 32);
        int v0 = (int) d;
        for (int i = a.length - 1; i >= 0; i--) {
            // Invariant from previous iteration:
            // unsigned rem < unsigned divisor.
            long rHigh = rem;
            long nHigh;
            long nLow;
            if (shift == 0) {
                nHigh = rHigh;
                nLow = a[i];
            } else {
                nHigh = (rHigh << shift) | (a[i] >>> (64 - shift));
                nLow = a[i] << shift;
            }
            int u3 = (int) (nHigh >>> 32);
            int u2 = (int) nHigh;
            int u1 = (int) (nLow >>> 32);
            int u0 = (int) nLow;
            /*
             * First:
             * qHigh = [u3 u2 u1] / [v1 v0]
             */
            long qHigh = divideThreeByTwo(u3, u2, u1, v1, v0);
            /*
             * Exact first remainder:
             *
             * [u2 u1] - qHigh * [v1 v0]
             *
             * modulo 2^64 is sufficient because the actual
             * remainder fits in 64 bits.
             */
            long r1 = (((u2 & U32_MASK) << 32) | (u1 & U32_MASK)) - qHigh * d;
            /*
             * Second:
             * [rHigh32 rLow32 u0] / [v1 v0]
             */
            int rLow32 = (int) r1;
            long qLow = divideThreeByTwo((int) (r1 >>> 32), rLow32, u0, v1, v0);
            /*
             * Exact second remainder.
             * Low 64 bits of the dividend are [rLow32 u0].
             */
            long normalizedRem = (((rLow32 & U32_MASK) << 32) | (u0 & U32_MASK)) - qLow * d;
            q[i] = (qHigh << 32) | qLow;
            rem = normalizedRem >>> shift;
        }
        return new DivRem(trimLeadingZerosTrusted(q), rem);
    }
    /**
     * Divides one magnitude by another and returns a 2D array.
     * @param remainderNeeded The result array should include the remainder.
     * @return An array containing the quotient, remainder if requested.
     */
    private static long[][] divideMagnitudes(long[] dividend, long[] divisor, boolean remainderNeeded) {
        dividend = trimLeadingZerosTrusted(dividend);
        divisor  = trimLeadingZerosTrusted(divisor);
        if (isZeroMagnitude(divisor))
            throw new ArithmeticException("Division by zero");
        int cmp = compareArray(dividend, divisor);
        if (cmp < 0)
            return remainderNeeded ? new long[][]{ZERO_MAG, dividend} : new long[][]{ZERO_MAG};
        if (cmp == 0)
            return remainderNeeded ? new long[][]{ONE_MAG, ZERO_MAG} : new long[][]{ONE_MAG};
        if (divisor.length == 1) {
            DivRem DR = divideByLong(dividend, divisor[0]);
            return remainderNeeded ? new long[][]{DR.quotient(), new long[]{DR.remainder()}}
                    : new long[][]{DR.quotient()};
        }
        long[][] results;
        if (divisor.length < BURNIKEL_ZIEGLER_BLOCK_THRESHOLD
                || dividend.length-divisor.length < BURNIKEL_ZIEGLER_OFFSET)
            results = divideMagnitudesKnuth(dividend, divisor, remainderNeeded);
        else results = divideMagnitudesBurnikelZiegler(dividend, divisor, remainderNeeded);
        if (VERIFY_DIVISION) {
            long[] product = multiplyMagnitudes(results[0], divisor);
            if (remainderNeeded) {
                if (compareArray(results[1], divisor) >= 0)
                    throw new AssertionError("Remainder >= divisor");
                long[] reconstructed = addMagnitudes(product, results[1]);
                if (!equalMagnitudes(reconstructed, dividend))
                    throw new AssertionError("q*d+r != n");
            } else {
                if (compareArray(product, dividend) > 0 ||
                        compareArray(addMagnitudes(product, divisor), dividend) <= 0)
                    throw new AssertionError("Incorrect quotient");
            }
        }
        return results;
    }
    private static long[][] divideMagnitudesKnuth(long[] dividend, long[] divisor, boolean remainderNeeded) {
        if (isZeroMagnitude(divisor))
            throw new ArithmeticException("Division by zero");
        int cmp = compareArray(dividend, divisor);
        if (cmp < 0)
            return remainderNeeded ? new long[][]{ZERO_MAG, dividend} : new long[][]{ZERO_MAG};
        if (cmp == 0)
            return remainderNeeded ? new long[][]{ONE_MAG, ZERO_MAG} : new long[][]{ONE_MAG};
        int n = effectiveLength(divisor);
        if (n == 1) {
            DivRem DR = divideByLong(dividend, divisor[0]);
            return remainderNeeded ? new long[][]{DR.quotient(), new long[]{DR.remainder()}}
                    : new long[][]{DR.quotient()};
        }
        int m = effectiveLength(dividend) - n;
        int shift = Long.numberOfLeadingZeros(divisor[n - 1]);
        long[] v = shiftLeftBits(divisor, shift);
        long[] u = new long[dividend.length + 1];
        shiftLeftBitsInto(dividend, shift, u);
        long[] q = new long[m + 1];
        for (int j = m; j >= 0; j--) {
            long ujn = u[j + n];
            long ujn1 = u[j + n - 1];
            long qhat;
            long rHat;
            boolean rHatOverflow = false;
            if (Long.compareUnsigned(ujn, v[n - 1]) > 0)
                throw new AssertionError("Knuth invariant broken: u[j+n] > v[n-1], j=" + j);
            // estimate
            if (ujn == v[n - 1]) {
                qhat = -1L;
                rHat = ujn1 + v[n - 1];
                rHatOverflow = Long.compareUnsigned(rHat, ujn1) < 0;
            } else {
                qhat = divide128ByNormalized64Quotient(ujn, ujn1, v[n - 1]);
                rHat = ujn1 - qhat * v[n - 1];
            }
            // second-limb correction
            while (!rHatOverflow) {
                long x = v[n - 2];
                int c = Long.compareUnsigned(Math.unsignedMultiplyHigh(qhat, x), rHat);
                if (c < 0 || (c == 0 && Long.compareUnsigned(qhat * x, u[j + n - 2]) <= 0)) break;
                qhat--;
                long old = rHat;
                rHat += v[n - 1];
                rHatOverflow = Long.compareUnsigned(rHat, old) < 0;
            }
            if (subtractMulLimbAt(u, v, qhat, j)) {
                qhat--;
                long carry = 0L;
                for (int i = 0; i < n; i++) {
                    int k = j + i;
                    long old = u[k];
                    long t = old + v[i];
                    long c1 = Long.compareUnsigned(t, old) < 0 ? 1L : 0L;
                    long sum = t + carry;
                    long c2 = Long.compareUnsigned(sum, t) < 0 ? 1L : 0L;
                    u[k] = sum;
                    carry = c1 | c2;
                }
                u[j + n] += carry;
            }
            q[j] = qhat;
        }
        if (!remainderNeeded) return new long[][]{trimLeadingZerosTrusted(q)};
        long[] r = Arrays.copyOf(u, n);
        shiftRightBitsInPlace(r, shift);
        return new long[][]{trimLeadingZerosTrusted(q), trimLeadingZerosTrusted(r)};
    }
    private static final int BURNIKEL_ZIEGLER_BLOCK_THRESHOLD = 128;
    private static final int BURNIKEL_ZIEGLER_LEAF_THRESHOLD = 64;
    private static final int BURNIKEL_ZIEGLER_OFFSET = 80;
    private static final boolean VERIFY_DIVISION = false;
    /**
     * Burnikel-Ziegler division of arbitrary-size positive magnitudes.
     * The divisor is normalized to exactly n limbs, where n is chosen so that
     * repeated halving eventually reaches <code> <= BURNIKEL_ZIEGLER_THRESHOLD</code>.
     * The normalized dividend is then consumed n limbs at a time, using
     * divide2nBy1n() as the block divider.
     * @return An array containing quotient and remainder.
     */
    private static long[][] divideMagnitudesBurnikelZiegler(long[] dividend, long[] divisor, boolean needRemainder) {
        /*
          Choose structural block length n.
          We want:
              n / 2^k <= BZ_THRESHOLD
          after recursive halving.
          granularity is therefore a power of two.
         */
        int groups = (int) (((long) divisor.length + BURNIKEL_ZIEGLER_BLOCK_THRESHOLD - 1)
                / BURNIKEL_ZIEGLER_BLOCK_THRESHOLD);
        int granularity = 1;
        while (granularity < groups) granularity <<= 1;
        int n = (int) ((((long) divisor.length + granularity - 1) / granularity) * granularity);
        /*
          Normalize divisor to exactly n limbs with its highest bit set.

          beta = 2^64

          sigma = 64*n - bitLength(B)

          Multiplying both operands by 2^sigma leaves the quotient unchanged:

              (A << sigma) / (B << sigma) = A / B

          while the remainder is shifted by sigma.
         */
        long sigma = ((long) n << 6) - bitLength(divisor);
        long[] B = shiftLeftBits(divisor, sigma);
        long[] A = shiftLeftBits(dividend, sigma);
        /*
         * Number of n-limb blocks in A.
         */
        int blockCount = (int) (((long) A.length + n - 1) / n);
        /*
         * Quotient has at most blockCount n-limb blocks.
         * Leading zero blocks are trimmed at the end.
         */
        long[] quotient = new long[Math.multiplyExact(blockCount, n)];
        long[] remainder = ZERO_MAG;
        /*
         * Block long division, from most-significant block to least-significant.
         *
         * At every iteration:
         *
         *     current = remainder * beta^n + nextBlock
         *
         * Since remainder < B < beta^n:
         *
         *     current < beta^(2n)
         *
         * so this is exactly a 2n-by-n division.
         */
        for (int block = blockCount - 1; block >= 0; block--) {
            long[] next = sliceFixed(A, block * n, n);
            long[] current = new long[2 * n];
            System.arraycopy(next, 0, current, 0, n);
            System.arraycopy(remainder, 0, current, n, Math.min(remainder.length, n));
            long[][] qr = compareArray(current, B) < 0
                    ? new long[][]{ZERO_MAG, trimLeadingZerosTrusted(current)}
                    : divide2nBy1n(current, B);
            long[] qBlock = qr[0];
            remainder = qr[1];
            if (VERIFY_DIVISION) {
                // Fundamental block invariant:
                // current == qBlock * B + newR
                long[] rebuild = addMagnitudes(multiplyMagnitudes(qBlock, B), remainder);
                if (!equalMagnitudes(rebuild, current))
                    throw new AssertionError("BZ block reconstruction failed, block=" + block
                            + ", qLen=" + qBlock.length + ", rLen=" + remainder.length);
                if (compareArray(remainder, B) >= 0)
                    throw new AssertionError("BZ remainder >= divisor at block=" + block);
                if (qBlock.length > n)
                    throw new AssertionError("BZ quotient block overflow at block=" + block +
                            ", qLen=" + qBlock.length + ", n=" + n);
            }
            System.arraycopy(qBlock, 0, quotient, block * n, qBlock.length);
        }
        /*
         * We divided:
         *
         *     (dividend << sigma) / (divisor << sigma)
         *
         * so the quotient is already correct, but:
         *
         *     normalizedRemainder = originalRemainder << sigma
         */
        if (!needRemainder) return new long[][]{trimLeadingZerosTrusted(quotient)};
        shiftRightBitsInPlace(remainder, sigma);
        return new long[][]{trimLeadingZerosTrusted(quotient), trimLeadingZerosTrusted(remainder)};
    }
    private static long[][] divide2nBy1n(long[] A, long[] B) {
        int cmp = compareArray(A, B);
        if (cmp < 0) return new long[][]{ZERO_MAG, trimLeadingZerosTrusted(A)};
        else if (cmp == 0) return new long[][]{ONE_MAG, ZERO_MAG};
        int n = B.length;
        if (n <= BURNIKEL_ZIEGLER_LEAF_THRESHOLD || (n & 1) != 0)
            return divideMagnitudesKnuth(A, B, true);
        int m = n >>> 1;
        // A = A3*β^(3m) + A2*β^(2m) + A1*β^m + A0
        long[] work = new long[3 * m];
        System.arraycopy(A, m, work, 0, work.length);
        long[][] qr1 = divide3nBy2n(work, B);
        long[] Q1 = qr1[0];
        long[] R1 = qr1[1];
        /*
          Second:
               [R1 A0] / B
          R1 may occupy up to 2m = n limbs, so this is
          another 3m / 2m division.
         */
        Arrays.fill(work, 0L);
        System.arraycopy(A, 0, work, 0, m);
        System.arraycopy(R1, 0, work, m, Math.min(R1.length, 2 * m));
        long[][] qr0 = divide3nBy2n(work, B);
        long[] Q0 = qr0[0];
        /*
         * Q = Q1 β^m + Q0
         */
        long[] Q = new long[2 * m];
        System.arraycopy(Q0, 0, Q, 0, Math.min(Q0.length, m));
        System.arraycopy(Q1, 0, Q, m, Math.min(Q1.length, m));
        return new long[][]{trimLeadingZerosTrusted(Q), trimLeadingZerosTrusted(qr0[1])};
    }
    private static long[] mod2nBy1n(long[] current, long[] B) {
        int n = B.length;
        if (n <= BURNIKEL_ZIEGLER_LEAF_THRESHOLD || (n & 1) != 0)
            return knuthRemainder(current, B);
        int m = n >>> 1;
        // A = A3 β^(3m) + A2 β^(2m) + A1 β^m + A0
        long[] upper = new long[3 * m];
        System.arraycopy(current, m, upper, 0, 3 * m);
        long[] R1 = divide3nBy2n(upper, B)[1];
        /*
         * [R1 A0] / B
         */
        long[] lower = new long[3 * m];
        System.arraycopy(current, 0, lower, 0, m);
        System.arraycopy(R1, 0, lower, m, Math.min(R1.length, 2 * m));
        return divide3nBy2n(lower, B)[1];
    }
    private static long[][] divide3nBy2n(long[] dividend, long[] divisor) {
        int cmp = compareArray(dividend, divisor);
        if (cmp < 0) return new long[][]{ZERO_MAG, trimLeadingZerosTrusted(dividend)};
        if (cmp == 0) return new long[][]{ONE_MAG, ZERO_MAG};
        int n = divisor.length;
        if (n <= BURNIKEL_ZIEGLER_LEAF_THRESHOLD)
            return divideMagnitudesKnuth(dividend, divisor, true);
        int m = n >>> 1;
        long[] A2 = sliceFixed(dividend, 2 * m, m);
        long[] B0 = sliceFixed(divisor, 0, m);
        long[] B1 = sliceFixed(divisor, m, m);
        long[] A12 = new long[2 * m];
        System.arraycopy(dividend, m, A12, 0, 2 * m);
        long[] q;
        long[] r;
        long[] d;
        if (compareArray(A2, B1) < 0) {
            long[][] qr = divide2nBy1n(A12, B1);
            q = qr[0];
            r = qr[1];
            d = multiplyMagnitudes(q, B0);
        } else {
            q = new long[m];
            Arrays.fill(q, -1L);
            r = addMagnitudes(A12, B1);
            subtractMagnitudeInPlace(r, shiftLeftLimbs(B1, m));
            if (isZeroMagnitude(B0)) d = ZERO_MAG;
            else d = subtractMagnitudes(shiftLeftLimbs(B0, m), B0);
        }
        long[] rFull = new long[2 * m + 1];
        System.arraycopy(dividend, 0, rFull, 0, m + 1);
        System.arraycopy(r, 0, rFull, m, Math.min(r.length, m + 1));
        int corrections = 0;
        while (compareArray(rFull, d) < 0) {
            addMagnitudeInPlace(rFull, divisor, 0);
            subtractMagnitudeInPlace(q, ONE_MAG);
            if (++corrections > 2)
                throw new AssertionError("BZ divide3nBy2n required too many corrections");
        }
        return new long[][]{trimLeadingZerosTrusted(q), subtractMagnitudes(rFull, d)};
    }
    private static long[] divideMagnitudesExact(long[] n, long[] d) {
        long[][] DR = divideMagnitudes(n, d, true);
        if (!isZeroMagnitude(DR[1]))
            throw new ArithmeticException("Division was not exact");
        return DR[0];
    }

    /**
     * Returns a LargeInteger whose value is <code>this / other</code>, ignoring remainder.
     */
    public LargeInteger divide(LargeInteger other) {
        return new LargeInteger(divideMagnitudes(limbs, other.limbs, false)[0],
                negative ^ other.negative, true);
    }
    public LargeFraction percent() {return new LargeFraction(this, HUNDRED);}
    public LargeFraction permille() {return new LargeFraction(this, THOUSAND);}

    /**
     * Returns a fraction whose value is <code>1 / this</code>.
     */
    @Override
    public LargeFraction reciprocal() {return new LargeFraction(ONE, this);}

    //--- Remainder and modulus ---//

    private static long remainder128By64(long high, long low, long divisor) {
        if (divisor == 0L)
            throw new ArithmeticException("divide by zero");
        assert Long.compareUnsigned(high, divisor) < 0 : "remainder128By64 requires high < divisor";
        if (high == 0L)
            return Long.remainderUnsigned(low, divisor);
        int shift = Long.numberOfLeadingZeros(divisor);
        long d;
        long nHigh;
        long nLow;
        if (shift == 0) {
            d = divisor;
            nHigh = high;
            nLow = low;
        } else {
            d = divisor << shift;
            nHigh = (high << shift) | (low >>> (64 - shift));
            nLow = low << shift;
        }
        int u2 = (int) nHigh;
        int u1 = (int) (nLow >>> 32);
        int u0 = (int) nLow;
        int v1 = (int) (d >>> 32);
        int v0 = (int) d;

        //[u3 u2 u1] / [v1 v0]
        long q1 = divideThreeByTwo((int) (nHigh >>> 32), u2, u1, v1, v0);
        /*
          Exact remainder of the first 96/64 division.
          The actual remainder is < d, therefore it fits in 64 bits.
          We only need the low 64 bits of N - q1*d
         */
        long r1 = (((u2 & U32_MASK) << 32) | (u1 & U32_MASK)) - (q1 * d);
        /*
          Bring down u0:
          [r1.high r1.low u0] / [v1 v0]
          then low 64 bits of [r1.high r1.low u0]
          are [r1.low u0].
         */
        return (((r1 & U32_MASK) << 32) | (u0 & U32_MASK))
                - (divideThreeByTwo((int) (r1 >>> 32), (int) r1, u0, v1, v0) * d) >>> shift;
    }
    private static long modSmall(long[] a, long d) {
        if (d == 0L) throw new ArithmeticException("Cannot divide by 0");
        if ((d & (d - 1)) == 0) return a[0] & (d - 1);
        int len = effectiveLength(a);
        if (len == 1) return Long.remainderUnsigned(a[0], d);
        long r = 0L;
        for (int i = len - 1; i >= 0; i--) r = remainder128By64(r, a[i], d);
        return r;
    }
    private static int modSmallInt(long[] a, int len, int p, long reciprocal) {
        long r = 0;
        for (int i = len - 1; i >= 0; i--) {
            long limb = a[i];
            long x = (r << 32) + (limb >>> 32);
            long q = Math.multiplyHigh(x, reciprocal);
            r = x - q * p;
            if (r >= p) r -= p;
            x = (r << 32) + (limb & U32_MASK);
            q = Math.multiplyHigh(x, reciprocal);
            r = x - q * p;
            if (r >= p) r -= p;
        }
        return (int) r;
    }
    private static int reduceSignedIntModP(int value, int p, long reciprocal) {
        if (value >= 0) {
            long q = Math.multiplyHigh(value, reciprocal);
            int r = (int) ((long) value - q * p);
            if (r >= p) r -= p;
            return r;
        }
        long x = -(long) value;
        long q = Math.multiplyHigh(x, reciprocal);
        int r = (int) (x - q * p);
        if (r >= p) r -= p;
        return r == 0 ? 0 : p - r;
    }
    private static final int MOD_BZ_BLOCK_THRESHOLD = 192;
    private static long[] modMagnitudes(long[] dividend, long[] divisor) {
        dividend = trimLeadingZerosTrusted(dividend);
        divisor  = trimLeadingZerosTrusted(divisor);
        if (isZeroMagnitude(divisor)) throw new ArithmeticException("Division by zero");
        int cmp = compareArray(dividend, divisor);
        if (cmp < 0) return dividend;
        else if (cmp == 0) return ZERO_MAG;
        if (divisor.length == 1) return new long[]{modSmall(dividend, divisor[0])};
        long[] remainder;
        if (divisor.length < MOD_BZ_BLOCK_THRESHOLD
                || dividend.length - divisor.length < BURNIKEL_ZIEGLER_OFFSET)
            remainder = knuthRemainder(dividend, divisor);
        else remainder = burnikelZieglerRemainder(dividend, divisor);
        if (VERIFY_DIVISION) {
            if (compareArray(divisor, remainder) >= 0)
                throw new AssertionError("Remainder greater than divisor");
        }
        return remainder;
    }
    private static long[] knuthRemainder(long[] dividend, long[] divisor) {
        dividend = trimLeadingZerosTrusted(dividend);
        divisor = trimLeadingZerosTrusted(divisor);
        if (isZeroMagnitude(divisor)) throw new ArithmeticException("Division by zero");
        int cmp = compareArray(dividend, divisor);
        if (cmp < 0) return dividend;
        else if (cmp == 0) return ZERO_MAG;
        if (divisor.length == 1) return new long[]{modSmall(dividend, divisor[0])};
        // Normalize divisor so its top bit is set.
        int shift = Long.numberOfLeadingZeros(divisor[divisor.length - 1]);
        long[] v = shiftLeftBits(divisor, shift);
        long[] u = new long[dividend.length + 1];
        if (shift == 0) System.arraycopy(dividend, 0, u, 0, dividend.length);
        else {
            long[] u0 = shiftLeftBits(dividend, shift);
            System.arraycopy(u0, 0, u, 0, u0.length);
        }
        int n = v.length;
        int m = u.length - n - 1;
        for (int j = m; j >= 0; j--) {
            long ujn = u[j + n];
            long ujn1 = u[j + n - 1];
            long qhat;
            long rHat;
            boolean rHatOverflow = false;
            int topCmp = Long.compareUnsigned(ujn, v[n - 1]);
            if (topCmp > 0)
                throw new AssertionError("Knuth mod invariant broken: u[j+n] > v[n-1], j=" + j);
            if (topCmp == 0) {
                qhat = -1L;
                rHat = ujn1 + v[n - 1];
                rHatOverflow = Long.compareUnsigned(rHat, ujn1) < 0;
            } else {
                qhat = divide128ByNormalized64Quotient(ujn, ujn1, v[n - 1]);
                rHat = ujn1 - qhat * v[n - 1];
            }
            while (!rHatOverflow) {
                long x = v[n - 2];
                int c = Long.compareUnsigned(Math.unsignedMultiplyHigh(qhat, x), rHat);
                if (c < 0 || (c == 0 && Long.compareUnsigned(qhat * x, u[j + n - 2]) <= 0)) break;
                qhat--;
                long old = rHat;
                rHat += v[n - 1];
                if (Long.compareUnsigned(rHat, old) < 0) rHatOverflow = true;
            }
            if (subtractMulLimbAt(u, v, qhat, j)) {
                long carry = 0L;
                for (int i = 0; i < v.length; i++)
                    carry = addWithCarry(u, j + i, v[i], carry);
                u[j + v.length] += carry;
            }
            if (u[j + n] != 0L)
                throw new AssertionError("Knuth step failed to clear top limb; j=" + j +
                        ", top=" + Long.toUnsignedString(u[j + n]));
        }
        // Low n limbs now contain normalized remainder.
        long[] r = Arrays.copyOf(u, n);
        return shiftRightBits(r, shift);
    }
    private static long[] burnikelZieglerRemainder(long[] dividend, long[] divisor) {
        int groups = (int)(((long) divisor.length + BURNIKEL_ZIEGLER_BLOCK_THRESHOLD - 1)
                / BURNIKEL_ZIEGLER_BLOCK_THRESHOLD);
        int granularity = 1;
        while (granularity < groups) granularity <<= 1;
        int n = (int)(((long) divisor.length + granularity - 1) / granularity * granularity);
        long sigma = ((long)n << 6) - bitLength(divisor);
        long[] B = shiftLeftBits(divisor, sigma);
        long[] A = shiftLeftBits(dividend, sigma);
        if (B.length != n) throw new AssertionError("BZ normalization produced " + B.length +
                " divisor limbs; expected " + n);
        int blockCount = (int)(((long) A.length + n - 1) / n);
        long[] remainder = ZERO_MAG;
        for (int block = blockCount - 1; block >= 0; block--) {
            long[] next = sliceFixed(A, block * n, n);
            long[] current = new long[2 * n];
            System.arraycopy(next, 0, current, 0, n);
            System.arraycopy(remainder, 0, current, n, Math.min(remainder.length, n));
            if (compareArray(current, B) < 0) remainder = trimLeadingZerosTrusted(current);
            else remainder = mod2nBy1n(current, B);
        }
        remainder = shiftRightBits(remainder, sigma);
        if (VERIFY_DIVISION)
            if (compareArray(remainder, divisor) >= 0)
                throw new AssertionError("Knuth remainder became >= divisor after denormalization");
        return remainder;
    }
    private static long[] modAdd(long[] a, long[] b, long[] m) {
        long[] addend = compareArray(a, m) < 0 ? a : modMagnitudes(a, m);
        long[] adder = compareArray(b, m) < 0 ? b : modMagnitudes(b, m);
        long[] sum = addMagnitudes(addend, adder);
        if (compareArray(sum, m) >= 0)
            subtractMagnitudeInPlace(sum, m);
        return sum;
    }
    private static long[] modSubtract(long[] a, long[] b, long[] m) {
        a = trimLeadingZerosTrusted(a);
        b = trimLeadingZerosTrusted(b);
        m = trimLeadingZerosTrusted(m);
        long[] subtrahend = compareArray(a, m) < 0 ? a : modMagnitudes(a, m);
        long[] subtractor = compareArray(b, m) < 0 ? b : modMagnitudes(b, m);
        return compareArray(subtrahend, subtractor) >= 0 ? subtractMagnitudes(subtrahend, subtractor)
                : subtractMagnitudes(m, subtractMagnitudes(subtractor, subtrahend));
    }
    private static long[] multiplyMod(long[] a, long[] b, long[] m)
    {return modMagnitudes(multiplyMagnitudes(a, b), m);}

    /**
     * Returns a LargeInteger whose value is <code>this - this.divide(other)*other</code>.
     */
    public LargeInteger remainder(LargeInteger other) {
        long[] r = modMagnitudes(limbs, other.limbs);
        return new LargeInteger(r, negative, true);
    }

    /**
     * Returns an array containing Q and R such that <code>this / other</code> = Q with remainder R.
     */
    public LargeInteger[] quotientAndRemainder(LargeInteger other) {
        long[][] qr = divideMagnitudes(limbs, other.limbs, true);
        return new LargeInteger[]{
                new LargeInteger(qr[0], negative ^ other.negative, true),
                new LargeInteger(qr[1], negative, true)};
    }

    /**
     * Returns this integer's residue modulo other. This treats negative residue differently
     * from {@link #remainder(LargeInteger)}.
     */
    public LargeInteger mod(LargeInteger modulus) {
        LargeInteger r = remainder(modulus);
        if (r.isZero()) return ZERO;
        else if (r.negative != modulus.negative)
            return r.add(modulus);
        else return r;
    }
    long mod(long modulus) {
        if (modulus < 0) {
            long mod = mod(-modulus);
            return mod == 0 ? 0 : mod + modulus;
        }
        long R = modSmall(limbs, modulus);
        if (R == 0) return 0;
        if (isNegative()) return modulus-R;
        return R;
    }
    private static long[] modInverseMagnitudes(long[] a, long[] mod) {
        if (isZeroMagnitude(mod))
            throw new ArithmeticException("Modulus cannot be 0");
        GcdInverseMagnitude eg = gcdInverseMagnitudes(a, mod);
        if (eg.inverse() == null)
            throw new ArithmeticException("Modular inverse does not exist");
        return eg.inverse();
    }

    /**
     * Returns <code>this<sup>-1</sup> (mod other)</code>, provided that <code>this</code> is coprime
     * to <code>other</code>.
     * @param mod The other integer that this is reduced with respect to
     * @return Modular inverse of <code>this</code> mod <code>other</code>
     */
    public LargeInteger modInverse(LargeInteger mod) {
        long[] inverse = modInverseMagnitudes(limbs, mod.limbs);
        // (-a)^-1 ≡ -(a^-1) mod |mod|.
        if (isNegative() && !isZeroMagnitude(inverse))
            inverse = subtractMagnitudes(mod.limbs, inverse);
        // Preserve the existing negative-modulus behavior:
        // positive modulus -> [0, mod)
        // negative modulus -> (mod, 0]
        if (mod.isNegative() && !isZeroMagnitude(inverse))
            return new LargeInteger(subtractMagnitudes(mod.limbs, inverse), true, true);
        return new LargeInteger(inverse);
    }
    private static boolean isOddMagnitude(long[] x) {return (x[0] & 1L) == 1L;}
    private static boolean isEvenMagnitude(long[] x) {return (x[0] & 1L) == 0L;}
    public boolean isEven() {return isEvenMagnitude(limbs);}
    public boolean isOdd() {return isOddMagnitude(limbs);}

    //--- Exponentiation and nth roots ---//

    private static long[] powerOneLimb(long base, long exp) {
        long result = 1L;
        long b = base;
        while (exp != 0) {
            if ((exp & 1L) != 0) {
                if (Math.unsignedMultiplyHigh(result, b) != 0L) return null;
                result *= b;
            }
            exp >>>= 1;
            if (exp == 0) break;
            if (Math.unsignedMultiplyHigh(b, b) != 0L) return null;
            b *= b;
        }
        return new long[]{result};
    }
    private static long[] power(long[] mag, long power) {
        if (power < 0)
            throw new IllegalArgumentException("Positive power is required");
        else if (power == 0) {
            if (isZeroMagnitude(mag))
                throw new ArithmeticException("Indeterminate form: 0^0");
            return ONE_MAG;
        } else if (power == 1) return mag.clone();
        else if (power > MAX_BITS) reportOverflow();
        long[] base = mag.clone();
        long exponent = trailingZeroBits(base);
        shiftRightBitsInPlace(base, exponent);
        long shift = Math.multiplyExact(power, exponent);
        if (isOneMagnitude(base))
            return shiftLeftBits(base, shift);
        // Exact one-limb power fast path.
        if (base.length == 1) {
            long[] small = powerOneLimb(base[0], power);
            if (small != null)
                return shiftLeftBits(small, shift);
        }
        long mask = Long.highestOneBit(power) >>> 1;
        long[] result = base;
        while (mask != 0) {
            result = squareMagnitude(result);
            if ((power & mask) != 0)
                result = multiplyMagnitudes(result, base);
            mask >>>= 1;
        }
        return shiftLeftBits(result, shift);
    }

    private static SqrtResult sqrtFloorMagnitude(long[] n) {
        n = trimLeadingZerosTrusted(n);
        if (isZeroMagnitude(n)) return new SqrtResult(ZERO_MAG, ZERO_MAG);
        long[] root = ZERO_MAG;
        long[] rem = ZERO_MAG;
        for (long pair = ((bitLength(n) + 1) >>> 1) - 1; pair >= 0; pair--) {
            // rem = rem * 4 + next two bits of n
            rem = shiftLeftBits(rem, 2);
            long lowBit = pair << 1;
            int value = 0;
            if (testBit(n, lowBit)) value |= 1;
            if (testBit(n, lowBit + 1)) value |= 2;
            if (value != 0) rem = addSmall(rem, value);
            // candidate = 4 * root + 1
            long[] candidate = addSmall(shiftLeftBits(root, 2), 1);
            if (compareArray(rem, candidate) >= 0) {
                subtractMagnitudeInPlace(rem, candidate);
                root = addSmall(shiftLeftBits(root, 1), 1);
            } else root = shiftLeftBits(root, 1);
        }
        return new SqrtResult(trimLeadingZerosTrusted(root), trimLeadingZerosTrusted(rem));
    }
    private record SqrtResult(long[] sqrt, long[] remainder) {}
    private static long[] sqrtCeilMagnitude(long[] mag) {
        SqrtResult result = sqrtFloorMagnitude(mag);
        return (isZeroMagnitude(result.remainder())) ? result.sqrt() : addSmall(result.sqrt(), 1);
    }
    private static long[] nthRootFloorMagnitude(long[] n, long exp) {
        if (exp <= 0) throw new IllegalArgumentException("exp must be positive");
        n = trimLeadingZerosTrusted(n);
        if (isZeroMagnitude(n) || isOneMagnitude(n) || exp == 1) return n;
        if (exp == 2) return sqrtFloorMagnitude(n).sqrt();
        long bits = bitLength(n);
        long rootBits = (bits + exp - 1) / exp;
        // If exp is already too large, root must be 1.
        if (exp >= bits) return ONE_MAG;
        long[] x = shiftLeftBits(ONE_MAG, rootBits);
        for (;;) {
            long[] xPow = powerLimited(x, exp - 1, n);
            /*
             * x is an upper estimate, so normally x^(exp-1) <= n
             * once we're reasonably close. If the limited-power routine
             * reports overflow relative to n, quotient is simply 0.
             */
            long[] quotient;
            if (xPow == null) quotient = ZERO_MAG;
            else quotient = divideMagnitudes(n, xPow, false)[0];
            long[] lhs = multiplySmall(x, exp - 1);
            long[] next = divideByLong(addMagnitudes(lhs, quotient), exp).quotient();
            if (compareArray(next, x) >= 0) break;
            x = next;
        }
        // Newton can finish at/just above the root.
        while (comparePowTo(x, exp, n) > 0)
            x = subtractSmall(x, 1);
        // Defensive upward correction.
        while (comparePowTo(addSmall(x, 1), exp, n) <= 0)
            x = addSmall(x, 1);
        return x;
    }
    private static long[] powerLimited(long[] base, long exp, long[] limit) {
        long[] result = ONE_MAG;
        long[] power = base;
        while (exp != 0) {
            if ((exp & 1L) != 0) {
                result = multiplyMagnitudes(result, power);
                if (compareArray(result, limit) > 0)
                    return null;
            }
            exp >>>= 1;
            if (exp == 0) break;
            power = squareMagnitude(power);
            if (compareArray(power, limit) > 0)
                return null;
        }
        return result;
    }
    private static int comparePowTo(long[] base, long exp, long[] limit) {
        long[] result = powerLimited(base, exp, limit);
        return result == null ? 1 : compareArray(result, limit);
    }
    private record MagnitudePowerForm(long[] base, long exponent) {}
    private static MagnitudePowerForm detectPerfectPower(long[] n) {
        n = trimLeadingZerosTrusted(n);
        if (compareSmall(n, 4) < 0) return null;
        long[] current = n;
        long exponent = 1;
        int[] primes = sieve(Math.toIntExact(bitLength(current) - 1));
        for (int p : primes) {
            if (p > bitLength(current) - 1) break;
            for (;;) {
                // p = 2 has a specialized sqrt anyway.
                if (p != 2 && !passesPthPowerFilters(current, p)) break;
                long[] root = nthRootFloorMagnitude(current, p);
                if (comparePowTo(root, p, current) != 0) break;
                current = root;
                exponent = Math.multiplyExact(exponent, p);
                if (p > bitLength(current) - 1) break;
            }
        }
        return exponent == 1 ? null : new MagnitudePowerForm(current, exponent);
    }
    private static int findFilterPrime(int p) {
        if (p == 2) return 5;
        // p is odd, so multiplier must be even.
        for (long k = 2; ; k += 2) {
            long q = (long) p * k + 1;
            if (q > Integer.MAX_VALUE) return 0;
            if (isSmallPrime((int) q))
                return (int) q;
        }
    }
    private static boolean isSmallPrime(int n) {
        if (n < 2) return false;
        if ((n & 1) == 0) return n == 2;
        if (n % 3 == 0) return n == 3;
        // Every prime > 3 is 6m ± 1.
        for (int d = 5; (long) d * d <= n; d += 6) {
            if (n % d == 0 || n % (d + 2) == 0)
                return false;
        }
        return true;
    }
    private static boolean passesPthPowerFilters(long[] n, int p) {
        long q1 = findFilterPrime(p);
        if (q1 != 0 && !passesPowerResidueFilter(n, p, q1))
            return false;
        long q2 = findFilterPrime(p);
        return q2 == 0 || passesPowerResidueFilter(n, p, q2);
    }
    private static boolean passesPowerResidueFilter(long[] n, int p, long q) {
        long r = modSmall(n, q);
        if (r == 0) return true;
        long k = (q - 1) / p;
        long result = 1;
        while (k != 0) {
            if ((k & 1L) != 0)
                result = remainder128By64(Math.unsignedMultiplyHigh(result, r), result * r, q);
            k >>>= 1;
            if (k != 0)
                r = remainder128By64(Math.unsignedMultiplyHigh(r, r), r * r, q);
        }
        return result == 1;
    }
    PowerFormInteger asPowerForm() {
        if (isNegative()) throw new ArithmeticException();
        MagnitudePowerForm form = detectPerfectPower(limbs);
        return form == null
                ? new PowerFormInteger(this, 1)
                : new PowerFormInteger(new LargeInteger(form.base()), form.exponent());
    }

    /**
     * Returns a fraction whose value is this<sup> other</sup>.
     */
    public LargeFraction power(LargeInteger other) {
        if (other.isZero()) {
            if (isZero())
                throw new ArithmeticException("Indeterminate form: 0^0");
            return LargeFraction.ONE;
        }
        // 1 never grows, regardless of exponent size
        if (isOne()) return LargeFraction.ONE;
        if (isZero()) {
            if (other.isNegative())
                throw new ArithmeticException("Cannot divide by 0");
            return LargeFraction.ZERO;
        }
        if (equals(NEGATIVE_ONE))
            return other.isOdd() ? LargeFraction.NEGATIVE_ONE : LargeFraction.ONE;
        // a^n has at least n * (bitlength(base) - 1) + 1 bits.
        long maxExponent = (MAX_BITS - 1)/(bitLength() - 1);
        if (other.abs().greaterThan(valueOf(maxExponent))) reportOverflow();
        /*
         * exp is now guaranteed <= MAX_BITS - 1,
         * and MAX_BITS = 2^37, so it necessarily fits in long.
         */
        return power(other.longValue());
    }
    /**
     * Returns a fraction whose value is this<sup> other</sup>.
     */
    public LargeFraction power(long p) {
        if (p == 0) return LargeFraction.ONE;
        if (p == 1) return LargeFraction.valueOf(this);
        if (p < 0)
            return new LargeInteger(power(limbs, -p), (p & 1) == 1 && negative, true).reciprocal();
        else return LargeFraction.valueOf(new LargeInteger(power(limbs, p), (p & 1) == 1 && negative, true));
    }
    /**
     * Returns a LargeInteger representing 2<sup>bit</sup>.
     */
    public static LargeInteger powerOfTwo(long bit) {
        if (bit < 0) throw new IllegalArgumentException("negative exponent");
        if (bit > MAX_BITS) reportOverflow();
        int limbIndex = (int) (bit >>> 6) + 1;
        long[] out = new long[limbIndex];
        out[limbIndex-1] = 1L << (bit & 63);
        return new LargeInteger(out);
    }
    /**
     * Returns this<sup> other</sup>.
     */
    @Override
    public LargePowerMonomial power(LargeFraction other)
    {return new LargePowerMonomial(LargeFraction.valueOf(this), other);}

    //--- Montgomery reduction and modular powers ---//

    /**
     * A special class for reducing with respect to moduli when ordinary division is expensive.
     */
    private static final class MontgomeryReducer implements Cloneable{
        long reduceCalls;
        long carryNonzero;
        long propagatedLimbs;
        long canonicalSubtract;
        long canonicalCopy;
        void resetDiagnostics() {
            reduceCalls = 0;
            carryNonzero = 0;
            propagatedLimbs = 0;
            canonicalSubtract = 0;
            canonicalCopy = 0;
        }
        final long[] modulus;
        final int k;
        private final long mPrime;
        private final long[] negInverse;
        final long[] rModM;
        private final long[] rTimesModulus;
        private final long[] r2ModM;
        private final long[] scratch;
        @Override
        public MontgomeryReducer clone() {
            try {
                MontgomeryReducer clone = (MontgomeryReducer) super.clone();
                return new MontgomeryReducer(clone.modulus, clone.k, clone.mPrime, clone.negInverse,
                        clone.rModM, clone.rTimesModulus, clone.r2ModM);
            } catch (CloneNotSupportedException e) {
                //This normally should not happen
                throw new AssertionError(e);
            }
        }
        private MontgomeryReducer(long[] modulus, int k, long mPrime, long[] negInverse, long[] rModM,
                                  long[] rTimesModulus, long[] r2ModM) {
            this.modulus = modulus;
            this.k = k;
            this.mPrime = mPrime;
            this.negInverse = negInverse;
            this.rModM = rModM;
            this.rTimesModulus = rTimesModulus;
            this.r2ModM = r2ModM;
            scratch = new long[2*k+2];
            USE_CIOS = k < CIOS_THRESHOLD;
            USE_FAST_REDC = k > FAST_REDC_THRESHOLD;
            long[] nPlusOne = addSmall(modulus, 1);
            USE_MERSENNE_REDC = isOneMagnitude(shiftRightBits(rModM, trailingZeroBits(rModM))) &&
                    isOneMagnitude(shiftRightBits(nPlusOne, trailingZeroBits(nPlusOne))) && !USE_CIOS;
        }
        MontgomeryReducer(long[] inputModulus) {
            if (inputModulus == null)
                throw new NullPointerException("modulus is null");
            if (inputModulus.length == 0)
                throw new IllegalArgumentException("modulus is empty");
            modulus = trimLeadingZerosTrusted(inputModulus);
            if (isEvenMagnitude(modulus))
                throw new ArithmeticException("Montgomery modulus must be odd");
            k = modulus.length;
            USE_CIOS = k < CIOS_THRESHOLD;
            USE_FAST_REDC = k > FAST_REDC_THRESHOLD;
            long x = modulus[0];
            long y = 2L * x - x * x * x;
            for (int i = 0; i < 4; i++) y *= 2L - x * y;
            mPrime = -y;
            if (USE_FAST_REDC) {
                long[] negInverse = {mPrime};
                for (int precision = 1; precision < k; precision = precision >= k - precision ? k : precision * 2) {
                    int next = Math.min(precision * 2, k);
                    long[] factor = multiplyLow(modulus, negInverse, next);
                    addTwoMod(factor);
                    negInverse = multiplyLow(negInverse, factor, next);
                }
                this.negInverse = negInverse;
            } else negInverse = null;
            scratch = new long[2 * k + 2];
            long[] power = new long[2 * k + 1];
            power[k] = 1L;
            rModM = Arrays.copyOf(modMagnitudes(power, modulus), k);
            long[] nPlusOne = addSmall(modulus, 1);
            USE_MERSENNE_REDC = isOneMagnitude(shiftRightBits(rModM, trailingZeroBits(rModM))) &&
                    isOneMagnitude(shiftRightBits(nPlusOne, trailingZeroBits(nPlusOne))) && !USE_CIOS;
            power[k] = 0L;
            power[2 * k] = 1L;
            r2ModM = Arrays.copyOf(modMagnitudes(power, modulus), k);
            rTimesModulus = shiftLeftLimbs(modulus, k);
        }
        void reduceInto(long[] product, long[] out) {
            product = trimLeadingZerosTrusted(product);
            // REDC fast path requires product < R * modulus.
            if (compareArray(product, rTimesModulus) >= 0)
                product = modMagnitudes(product, rTimesModulus);
            if (USE_MERSENNE_REDC) {
                reduceCalls++;
                mersenneFoldIntoScratch(product);
                rotateMersenneInto(out);
                return;
            }
            Arrays.fill(scratch, 0L);
            System.arraycopy(product, 0, scratch, 0, product.length);
            reduceScratchInto(out);
        }
        private void reduceFullInverseInto(long[] product, long[] out) {
            reduceCalls++;
            // q = (product * negInverse) mod R
            long[] q = multiplyLow(product, negInverse, k);
            // u = (product + q * modulus) / R
            long[] qN = multiplyMagnitudes(q, modulus);
            long[] sum = addMagnitudes(product, qN);
            long[] u = shiftRightLimbs(sum, k);
            // Canonical reduction
            if (compareArray(u, modulus) >= 0)
                subtractMagnitudeInPlace(u, modulus);
            Arrays.fill(out, 0L);
            System.arraycopy(u, 0, out, 0, u.length);
        }
        long[] toMontgomery(long[] x) {return multiply(x, r2ModM);}
        long[] toMontgomery(long x) {return fromMontgomery(multiplySmall(r2ModM, x));}
        long[] fromMontgomery(long[] product) {
            long[] result = new long[k];
            reduceInto(product, result);
            return trimLeadingZerosTrusted(result);
        }
        private void addCarryIntoScratch(int index, long carry) {
            if (carry == 0) return;
            carryNonzero++;
            long old = scratch[index];
            long sum = old + carry;
            scratch[index] = sum;
            if (Long.compareUnsigned(sum, old) >= 0) return;
            while (++index < scratch.length) {
                if (++scratch[index] != 0L) return;
                propagatedLimbs++;
            }
            throw new ArithmeticException("Carry overflow");
        }
        private void handleMontgomeryLimb(int i) {
            long si = scratch[i];
            long u = si * mPrime;
            long carry = Math.unsignedMultiplyHigh(modulus[0], u);
            if (si != 0L) carry++;
            int index = i + 1;
            for (int j = 1; j < k; ++j) {
                long mj = modulus[j];
                long lo = mj * u;
                long hi = Math.unsignedMultiplyHigh(mj, u);
                long s1 = lo + scratch[index];
                if (Long.compareUnsigned(s1, lo) < 0) hi++;
                long s2 = s1 + carry;
                if (Long.compareUnsigned(s2, s1) < 0) hi++;
                scratch[index++] = s2;
                carry = hi;
            }
            addCarryIntoScratch(index, carry);
        }

        private void lastSubtract(long[] out) {
            if (scratch[k << 1] == 0L)
                for (int i = k - 1; i >= 0; --i) {
                    long a = scratch[k + i];
                    long b = modulus[i];
                    if (a != b) {
                        if (Long.compareUnsigned(a, b) < 0) {
                            canonicalCopy++;
                            System.arraycopy(scratch, k, out, 0, out.length);
                            return;
                        }
                        break;
                    }
                }
            canonicalSubtract++;
            long borrow = 0L;
            for (int i = 0; i < k; ++i) {
                long a = scratch[k + i];
                long b = modulus[i];
                out[i] = a - b - borrow;
                borrow = Long.compareUnsigned(a, b) < borrow ? 1L : 0L;
            }
        }
        private final boolean USE_MERSENNE_REDC;

        private void mersenneFoldIntoScratch(long[] product) {
            // Requires product < modulus^2 and out.length == k.
            // Computes product mod modulus, not yet Montgomery REDC.
            int p = (k << 6) - Long.numberOfLeadingZeros(modulus[k - 1]);
            int word = p >>> 6;
            int bits = p & 63;
            Arrays.fill(scratch, 0L);
            // Copy the low p bits.
            System.arraycopy(product, 0, scratch, 0, Math.min(word, product.length));
            if (bits != 0 && word < product.length)
                scratch[word] = product[word] & ((1L << bits) - 1);
            // Add the high part (product >>> p) into out.
            long carry = 0;
            for (int i = 0; i < k; i++) {
                int j = word + i;
                long hi = j < product.length ? product[j] >>> bits : 0L;
                if (bits != 0 && j + 1 < product.length)
                    hi |= product[j + 1] << (64 - bits);
                long a = scratch[i];
                long s = a + hi;
                long c = Long.compareUnsigned(s, a) < 0 ? 1 : 0;
                long s2 = s + carry;
                carry = c | (Long.compareUnsigned(s2, s) < 0 ? 1 : 0);
                scratch[i] = s2;
            }
            // Fold any overflow past bit p back into bit 0.
            boolean overflow;
            if (bits == 0)
                overflow = carry != 0;
            else {
                overflow = (scratch[word] >>> bits) != 0;
                scratch[word] &= (1L << bits) - 1;
            }
            if (overflow) {
                for (int i = 0; i < k; i++) {
                    if (++scratch[i] != 0)
                        break;
                }
            }
            // The all-ones p-bit value represents zero modulo m.
            if (equalMagnitudes(scratch, modulus))
                Arrays.fill(scratch, 0L);
        }

        private void rotateMersenneInto(long[] out) {
            // folded and out are separate k-limb buffers.
            int p = (k << 6) - Long.numberOfLeadingZeros(modulus[k - 1]);
            int r = (k << 6) % p;
            if (r == 0) {
                System.arraycopy(scratch, 0, out, 0, k);
                return;
            }
            // First component: folded >>> r
            int word = r >>> 6;
            int bits = r & 63;
            for (int i = 0; i < k; i++) {
                int j = i + word;
                long v = j < k ? scratch[j] >>> bits : 0L;
                if (bits != 0 && j + 1 < k)
                    v |= scratch[j + 1] << (64 - bits);
                out[i] = v;
            }
            // Second component: (folded mod 2^r) << (p-r)
            int shift = p - r;
            int whole = shift >>> 6;
            int offset = shift & 63;
            int count = (r + 63) >>> 6;
            for (int i = 0; i < count; i++) {
                long v = scratch[i];
                if (i == count - 1 && (r & 63) != 0)
                    v &= -1L >>> (64 - (r & 63));
                int j = whole + i;
                out[j] |= v << offset;
                if (offset != 0 && j + 1 < k)
                    out[j + 1] |= v >>> (64 - offset);
            }
        }
        private void reduceScratchInto(long[] out) {
            reduceCalls++;
            for (int i = 0; i < k; ++i)
                handleMontgomeryLimb(i);
            lastSubtract(out);
        }
        void requireReducedMagnitude(long[] x, String name) {
            if (x == null)
                throw new NullPointerException(name + " is null");
            if (x.length == 0)
                throw new IllegalArgumentException(name + " is empty");
            if (effectiveLength(x) > k)
                throw new IllegalArgumentException(name + " exceeds Montgomery width");
            if (compareArray(x, modulus) >= 0)
                throw new IllegalArgumentException(name + " is not reduced");
        }
        long[] add(long[] a, long[] b) {
            requireReducedMagnitude(a, "a");
            requireReducedMagnitude(b, "b");
            long[] out = new long[k];
            addInto(a, b, out);
            return trimLeadingZerosTrusted(out);
        }

        void addInto(long[] a, long[] b, long[] out) {
            long carry = 0L;
            for (int i = 0; i < k; ++i) {
                long ai = i < a.length ? a[i] : 0L;
                long sum = ai + (i < b.length ? b[i] : 0L) + carry;
                carry = Long.compareUnsigned(sum, ai) < carry ? 1L : 0L;
                out[i] = sum;
            }
            if (carry != 0 || greaterThanOrEqualToModulus(out)) {
                long borrow = 0L;
                for (int i = 0; i < k; ++i) {
                    long ai = out[i];
                    long bi = modulus[i];
                    out[i] = ai - bi - borrow;
                    borrow = Long.compareUnsigned(ai, bi) < borrow ? 1L : 0L;
                }
            }
        }

        long[] subtract(long[] a, long[] b) {
            requireReducedMagnitude(a, "a");
            requireReducedMagnitude(b, "b");
            long[] out = new long[k];
            subtractInto(a, b, out);
            return trimLeadingZerosTrusted(out);
        }

        void subtractInto(long[] a, long[] b, long[] out) {
            long borrow = 0L;

            for (int i = 0; i < k; ++i) {
                long ai = i < a.length ? a[i] : 0L;
                long bi = i < b.length ? b[i] : 0L;
                out[i] = ai - bi - borrow;
                borrow = Long.compareUnsigned(ai, bi) < borrow ? 1L : 0L;
            }
            if (borrow != 0) {
                long carry = 0L;
                for (int i = 0; i < k; ++i) {
                    long ai = out[i];
                    long sum = ai + modulus[i] + carry;
                    carry = Long.compareUnsigned(sum, ai) < carry ? 1L : 0L;
                    out[i] = sum;
                }
            }
        }
        private boolean greaterThanOrEqualToModulus(long[] a) {
            for (int i = k - 1; i >= 0; i--) {
                int cmp = Long.compareUnsigned(a[i], modulus[i]);
                if (cmp != 0) return cmp > 0;
            }
            return true;
        }
        long[] multiply(long[] a, long[] b) {
            long[] out = new long[k];
            multiplyInto(a, b, out);
            return out;
        }
        private static final int CIOS_THRESHOLD = 4;
        private static final int FAST_REDC_THRESHOLD = 190;
        private final boolean USE_CIOS;
        private final boolean USE_FAST_REDC;
        void multiplyInto(long[] a, long[] b, long[] out) {
            if (USE_MERSENNE_REDC)
                reduceInto(multiplyMagnitudes(a, b), out);
            else if (USE_CIOS) {
                reduceCalls++;
                Arrays.fill(scratch, 0L);
                if (a.length < b.length) {
                    long[] temp = a;
                    a = b;
                    b = temp;
                }
                for (int i = 0; i < b.length; ++i) {
                    long scalar = b[i];
                    long carry = 0L;
                    int index = i;
                    for (long v : a) {
                        long lo = v * scalar;
                        long hi = Math.unsignedMultiplyHigh(v, scalar);
                        long s1 = lo + scratch[index];
                        if (Long.compareUnsigned(s1, lo) < 0) hi++;
                        long s2 = s1 + carry;
                        if (Long.compareUnsigned(s2, s1) < 0) hi++;
                        scratch[index++] = s2;
                        carry = hi;
                    }
                    addCarryIntoScratch(index, carry);
                    handleMontgomeryLimb(i);
                }
                // Remaining implicit B limbs are zero, but Montgomery reduction
                // still needs exactly k cancellation rounds.
                for (int i = b.length; i < k; ++i)
                    handleMontgomeryLimb(i);
                lastSubtract(out);
            } else if (USE_FAST_REDC)
                reduceFullInverseInto(multiplyMagnitudes(a, b), out);
            else if (Math.min(a.length, b.length) < KARATSUBA_THRESHOLD) {
                LargeInteger.multiplyInto(a, b, scratch);
                reduceScratchInto(out);
            } else reduceInto(multiplyMagnitudes(a, b), out);
        }
        private static void addTwoMod(long[] a) {
            long old = a[0];
            a[0] += 2L;
            if (Long.compareUnsigned(a[0], old) >= 0)
                return;
            for (int i = 1; i < a.length; i++) {
                if (++a[i] != 0L)
                    return;
            }
        }
        long[] square(long[] a) {
            long[] out = new long[k];
            squareInto(a, out);
            return out;
        }
        private void addWideIntoScratch(int index, long lo, long hi, long top) {
            long old = scratch[index];
            long s0 = old + lo;
            scratch[index] = s0;
            long carry = Long.compareUnsigned(s0, old) < 0 ? 1L : 0L;

            old = scratch[index + 1];
            long s1 = old + hi;
            long c1 = Long.compareUnsigned(s1, old) < 0 ? 1L : 0L;

            long s2 = s1 + carry;
            long c2 = Long.compareUnsigned(s2, s1) < 0 ? 1L : 0L;

            scratch[index + 1] = s2;
            addCarryIntoScratch(index + 2, top + c1 + c2);
        }
        void squareInto(long[] a, long[] out) {
            if (USE_MERSENNE_REDC)
                reduceInto(squareMagnitude(a), out);
            else if (USE_CIOS) {
                reduceCalls++;
                Arrays.fill(scratch, 0L);
                int n = a.length;
                for (int i = 0; i < n; ++i) {
                    long ai = a[i];
                    // Diagonal contribution: ai^2 at position 2i.
                    long lo = ai * ai;
                    long hi = Math.unsignedMultiplyHigh(ai, ai);
                    addWideIntoScratch(2 * i, lo, hi, 0L);
                    // Off-diagonal contribution: 2 * ai * aj.
                    for (int j = i + 1; j < n; ++j) {
                        long aj = a[j];
                        lo = ai * aj;
                        hi = Math.unsignedMultiplyHigh(ai, aj);
                        addWideIntoScratch(i + j, lo << 1, (hi << 1) | (lo >>> 63), hi >>> 63);
                    }
                    // Cancel the current Montgomery limb.
                    handleMontgomeryLimb(i);
                }
                // Finish the remaining Montgomery reduction rounds.
                for (int i = n; i < k; ++i)
                    handleMontgomeryLimb(i);
                lastSubtract(out);
            } else if (USE_FAST_REDC)
                reduceFullInverseInto(squareMagnitude(a), out);
            else if (a.length < KARATSUBA_SQUARE_THRESHOLD) {
                LargeInteger.squareInto(a, scratch);
                reduceScratchInto(out);
            } else reduceInto(squareMagnitude(a), out);
        }
        long[] cube(long[] a) {return multiply(square(a), a);}
        long[] negate(long[] e) {
            if (isZeroMagnitude(e)) return e;
            return subtractMagnitudes(modulus, e);
        }
        long[] half(long[] a) {
            requireReducedMagnitude(a, "a");
            if (isEvenMagnitude(a))
                return shiftRightBits(a, 1);
            return shiftRightBits(addMagnitudes(a, modulus), 1);
        }
        void powMontgomery(long[] baseMontgomery, long exponent, long[] result) {
            if (exponent == 0) {
                System.arraycopy(rModM, 0, result, 0, result.length);
                return;
            }
            System.arraycopy(baseMontgomery, 0, result, 0, baseMontgomery.length);
            for (int bit = 62 - Long.numberOfLeadingZeros(exponent); bit >= 0; bit--) {
                squareInto(result, result);
                if (((exponent >>> bit) & 1L) != 0L)
                    multiplyInto(result, baseMontgomery, result);
            }
        }
        long[] powMontgomery(long[] baseMontgomery, long[] exponent, long[] result) {
            if (isZeroMagnitude(exponent)) {
                System.arraycopy(rModM, 0, result, 0, result.length);
                return result;
            }
            int window = bestWindowSize(exponent);
            if (window == 1) {
                System.arraycopy(baseMontgomery, 0, result, 0, baseMontgomery.length);
                for (long bit = bitLength(exponent) - 2; bit >= 0; bit--) {
                    squareInto(result, result);
                    if (testBit(exponent, bit))
                        multiplyInto(result, baseMontgomery, result);
                }
                return result;
            }
            int tableSize = 1 << (window - 1);
            long[][] oddPowers = new long[tableSize][];
            oddPowers[0] = baseMontgomery;
            long[] base2 = square(baseMontgomery);
            for (int i = 1; i < tableSize; i++)
                oddPowers[i] = multiply(oddPowers[i - 1], base2);
            long bit = bitLength(exponent) - 1;
            // Select first window
            long width = Math.min(window, bit + 1);
            while (width > 1 && !testBit(exponent, bit - width + 1)) width--;
            int value = 0;
            for (int i = 0; i < width; i++) {
                value <<= 1;
                if (testBit(exponent, bit - i))
                    value |= 1;
            }
            System.arraycopy(oddPowers[value >> 1], 0, result, 0, Math.min(oddPowers[value >> 1].length, result.length));
            bit -= width;
            while (bit >= 0) {
                if (!testBit(exponent, bit)) {
                    squareInto(result, result);
                    bit--;
                    continue;
                }
                width = Math.min(window, bit + 1);
                while (width > 1 && !testBit(exponent, bit - width + 1)) width--;
                value = 0;
                for (int i = 0; i < width; i++) {
                    value <<= 1;
                    if (testBit(exponent, bit - i)) value |= 1;
                }
                for (int i = 0; i < width; i++) squareInto(result, result);
                multiplyInto(result, oddPowers[value >>> 1], result);
                bit -= width;
            }
            return result;
        }
        long[] powMod(long[] base, long[] exponent) {
            return fromMontgomery(powMontgomery(toMontgomery(base), exponent, new long[k]));
        }
    }

    private static int bestWindowSize(long[] exponent) {
        long bits = bitLength(exponent);
        long popCount = 0;
        for (long limb : exponent)
            popCount += Long.bitCount(limb);

        if (popCount <= 2) return 1;

        final double MUL_COST = 1.13;

        // Window 1: initialize result directly from base.
        double bestCost = (bits - 1)
                + (popCount - 1) * MUL_COST;
        int bestWindow = 1;

        for (int window = 2; window <= 7; window++) {
            long windows = 0;
            long firstWidth = 0;
            long bit = bits - 1;

            while (bit >= 0) {
                if (!testBit(exponent, bit)) {
                    bit--;
                    continue;
                }

                long width = Math.min(window, bit + 1);
                while (width > 1 &&
                        !testBit(exponent, bit - width + 1))
                    width--;

                if (windows == 0)
                    firstWidth = width;

                windows++;
                bit -= width;
            }

            // First window is loaded directly.
            long exponentMultiplies = windows - 1;

            // One square and 2^(w-1)-1 multiplications
            // to precompute the odd powers.
            long tableMultiplies = (1L << (window - 1)) - 1;

            // Every exponent bit after the first window
            // causes exactly one squaring.
            long totalSquares = bits - firstWidth + 1;

            double cost = totalSquares
                    + (exponentMultiplies + tableMultiplies)
                    * MUL_COST;

            if (cost < bestCost) {
                bestCost = cost;
                bestWindow = window;
            }
        }

        return bestWindow;
    }

    /**
     * Returns a LargeInteger whose value is <code>(this<sup>exponent</sup>) mod modulus</code>.
     * Unlike <code>power</code>, this method treats negative exponents as modular inverses
     * rather than reciprocals.
     * @param exponent the power which the base is exponentiated to
     * @param modulus the modulus in which each step is always reduced with respect to
     */
    public LargeInteger powMod(LargeInteger exponent, LargeInteger modulus) {
        if (modulus.isZero())
            throw new ArithmeticException("Cannot mod by 0");
        if (modulus.isNegative()) {
            LargeInteger tmp = powMod(exponent, modulus.negate());
            return tmp.isZero() ? ZERO : tmp.add(modulus);
        }
        if (modulus.isOne()) return ZERO;
        if (exponent.isZero()) {
            if (isZero()) throw new ArithmeticException("Indeterminate form: 0^0");
            return ONE;
        }
        if (isZero()) return ZERO;
        else if (isOne()) return ONE;
        LargeInteger base = mod(modulus);
        if (exponent.isNegative())
            return base.modInverse(modulus).powMod(exponent.negate(), modulus);
        // Odd modulus
        if (modulus.isOdd())
            return modulus.limbs.length == 1
                    ? new LargeInteger(new long[]{powMod64(base.limbs, exponent.limbs, modulus.limbs[0])})
                    : new LargeInteger(new MontgomeryReducer(modulus.limbs).powMod(base.limbs, exponent.limbs));
        // Even modulus: m = 2^k * q
        long trailingBits = trailingZeroBits(modulus.limbs);
        long[] oddPart = shiftRightBits(modulus.limbs, trailingBits);
        long[] evenPow = powModPowerOfTwo(base.limbs, exponent.limbs, trailingBits);
        // Pure power of two
        if (isOneMagnitude(oddPart))
            return new LargeInteger(evenPow);
        // Odd component
        long[] oddBase = modMagnitudes(base.limbs, oddPart);
        long[] oddPow = oddPart.length == 1
                ? new long[]{powMod64(oddBase, exponent.limbs, oddPart[0])}
                : new MontgomeryReducer(oddPart).powMod(oddBase, exponent.limbs);
        // CRT
        long[] pow2 = shiftLeftBits(ONE_MAG, trailingBits);
        long[] t = modSubtract(evenPow, oddPow, pow2);
        t = multiplyMod(t, modInverseMagnitudes(oddPart, pow2), pow2);
        return new LargeInteger(addMagnitudes(oddPow, multiplyMagnitudes(oddPart, t)));
    }
    private static long[] lowestKBits(long[] n, long bits) {
        if (bits >= bitLength(n))
            return n;
        int len = Math.toIntExact(Math.ceilDiv(bits, 64L));
        long[] out = Arrays.copyOf(n, len);
        int rem = (int) (bits & 63);
        if (rem != 0)
            out[len - 1] &= (1L << rem) - 1;
        return out;
    }
    private static long[] maskToBits(long[] a, long bits) {
        int rem = (int) (bits & 63);
        if (rem != 0) a[a.length - 1] &= (1L << rem) - 1;
        return a;
    }
    private static long powMod64(long[] base, long[] exponent, long modulus) {
        if (modulus == 1) return 0;
        if (isZeroMagnitude(exponent)) return 1L;
        long baseTemp = modSmall(base, modulus);
        if (baseTemp == 0L) return 0L; // exponent is positive
        if (baseTemp == 1L) return 1L;
        if (modulus == 2) return baseTemp;
        long result = baseTemp;
        int top = exponent.length - 1;
        long limb = exponent[top];
        int firstBit = 63 - Long.numberOfLeadingZeros(limb);
        for (int bit = firstBit - 1; bit >= 0; bit--) {
            result = remainder128By64(Math.unsignedMultiplyHigh(result, result), result * result, modulus);
            if (((limb >>> bit) & 1L) != 0)
                result = remainder128By64(Math.unsignedMultiplyHigh(result, baseTemp), result * baseTemp,
                        modulus);
        }
        for (int i = top - 1; i >= 0; i--) {
            limb = exponent[i];
            for (int bit = 63; bit >= 0; bit--) {
                result = remainder128By64(Math.unsignedMultiplyHigh(result, result), result * result,
                        modulus);
                if (((limb >>> bit) & 1L) != 0)
                    result = remainder128By64(Math.unsignedMultiplyHigh(result, baseTemp), result * baseTemp,
                            modulus);
            }
        }
        return result;
    }
    private static int powModInt(long base, long exp, int mod, long reciprocal) {
        long res = 1;
        while (exp > 0) {
            if ((exp & 1L) != 0) {
                long x = res * base;
                res = x - Math.multiplyHigh(x, reciprocal) * mod;
                if (res >= mod) res -= mod;
            }
            exp >>>= 1;
            if (exp == 0) break;
            long x = base * base;
            base = x - Math.multiplyHigh(x, reciprocal) * mod;
            if (base >= mod) base -= mod;
        }
        return (int)res;
    }
    private static long[] powModPowerOfTwo(long[] in, long[] exponent, long bits) {
        if (isZeroMagnitude(in))
            return ZERO_MAG;
        if (isOneMagnitude(in))
            return ONE_MAG;
        if (bits <= 0)
            throw new IllegalArgumentException("bits must be positive");
        else if (bits == 1)
            return isOddMagnitude(in) ? ONE_MAG : ZERO_MAG;
        exponent = trimLeadingZerosTrusted(exponent);
        if (isZeroMagnitude(exponent)) {
            if (isZeroMagnitude(in))
                throw new ArithmeticException("Indeterminate form: 0^0");
            return ONE_MAG;
        }
        int len = Math.toIntExact(Math.ceilDiv(bits, 64L));
        // Fixed-width base residue.
        long[] base = new long[len];
        long[] reduced = lowestKBits(in, bits);
        System.arraycopy(reduced, 0, base, 0, Math.min(reduced.length, len));
        maskToBits(base, bits);
        if (isZeroMagnitude(base))
            return ZERO_MAG;
        long[] expTemp = exponent.clone();
        long t = trailingZeroBits(base);
        if (t > 0) {
            // v2(base^e) = t*e. Once >= bits, residue is zero.
            if (compareSmall(expTemp, Math.ceilDiv(bits, t)) >= 0)
                return ZERO_MAG;
        } else if (t == 0) {
            if (bits == 2) expTemp = lowestKBits(expTemp, 1);
            else expTemp = lowestKBits(expTemp, bits - 2);
        } else if (bits >= 3)
            // lambda(2^bits) = 2^(bits-2) for odd base.
            expTemp = lowestKBits(expTemp, bits - 2);
        if (isZeroMagnitude(expTemp))
            return ONE_MAG;
        long[] result = new long[len];
        result[0] = 1L;
        long[] scratch = new long[len];
        int window = bestWindowSize(expTemp);
        if (window == 1) {
            //Exponentiation by squaring
            while (!isZeroMagnitude(expTemp)) {
                if (isOddMagnitude(expTemp)) {
                    multiplyInto(result, base, scratch);
                    long[] swap = result;
                    result = maskToBits(scratch, bits);
                    scratch = swap;
                }
                shiftRightBitsInPlace(expTemp, 1);
                if (!isZeroMagnitude(expTemp)) {
                    squareInto(base, scratch);
                    long[] swap = base;
                    base = maskToBits(scratch, bits);
                    scratch = swap;
                }
            }
            return trimLeadingZerosTrusted(result);
        }
        //Sliding window
        int tableSize = 1 << (window - 1);
        long[][] oddPowers = new long[tableSize][];
        oddPowers[0] = base;
        long[] base2 = maskToBits(squareInto(base, new long[len]), bits);
        for (int i = 1; i < tableSize; i++)
            oddPowers[i] = maskToBits(multiplyInto(oddPowers[i - 1], base2, new long[len]), bits);
        long bit = bitLength(expTemp) - 1;
        long width = Math.min(window, bit + 1);
        while (width > 1 && !testBit(expTemp, bit - width + 1)) width--;
        int value = 0;
        for (int i = 0; i < width; i++) {
            value <<= 1;
            if (testBit(expTemp, bit - i))
                value |= 1;
        }
        result = Arrays.copyOf(oddPowers[value >> 1], len);
        bit -= width;
        while (bit >= 0) {
            if (!testBit(expTemp, bit)) {
                maskToBits(squareInto(result, scratch), bits);
                long[] swap = result;
                result = scratch;
                scratch = swap;
                bit--;
                continue;
            }
            width = Math.min(window, bit + 1);
            while (width > 1 && !testBit(expTemp, bit - width + 1)) width--;
            value = 0;
            for (int i = 0; i < width; i++) {
                value <<= 1;
                if (testBit(expTemp, bit - i))
                    value |= 1;
            }
            for (int i = 0; i < width; i++) {
                long[] swap = result;
                result = maskToBits(squareInto(result, scratch), bits);
                scratch = swap;
            }
            long[] swap = result;
            result = maskToBits(multiplyInto(result, oddPowers[value >> 1], scratch), bits);
            scratch = swap;
            bit -= width;
        }
        return trimLeadingZerosTrusted(result);
    }

    //--- GCD and LCM ---//

    private static long leading63AtShift(long[] mag, long shift) {
        if (shift < 0)
            throw new IllegalArgumentException("negative shift");
        long wordIndexLong = shift >>> 6;
        if (wordIndexLong >= mag.length)
            return 0L;

        int word = (int) wordIndexLong;
        int bits = (int) (shift & 63L);

        long out = mag[word] >>> bits;
        if (bits != 0 && word + 1 < mag.length)
            out |= mag[word + 1] << (64 - bits);
        return out;
    }
    private static LehmerStep lehmerStep(long[] a, long[] b) {
        /*
         * Extract both approximations at the same binary scale.
         * The value representing "a" is expected to be >= b.
         */
        long shift = Math.max(0L, bitLength(a) - 63L);
        long aTop = leading63AtShift(a, shift);
        long bTop = leading63AtShift(b, shift);
        /*
         * If b is too much smaller than 'a', its leading approximation
         * disappears completely. Lehmer is not useful here; do a real
         * division instead.
         */
        if (bTop == 0L)
            return new LehmerStep(1, 0, 0, 1, 0);
        /*
         * Signed transformation matrix:
         *
         * [ A B ]
         * [ C D ]
         *
         * Negative coefficients are expected.
         */
        long A = 1L;
        long B = 0L;
        long C = 0L;
        long D = 1L;
        long steps = 0L;
        while (bTop != 0L) {
            /*
             * Lehmer quotient agreement test:
             *
             * q1 = (aTop + A) / (bTop + C)
             * q2 = (aTop + B) / (bTop + D)
             *
             * All four sums are SIGNED operations.
             */
            if (signedAddWouldOverflow(aTop, A) || signedAddWouldOverflow(aTop, B)
                    || signedAddWouldOverflow(bTop, C) || signedAddWouldOverflow(bTop, D)) break;
            long num1 = aTop + A;
            long num2 = aTop + B;
            long den1 = bTop + C;
            long den2 = bTop + D;
            /*
             * Lehmer's bounds are only useful while these quantities
             * remain positive.
             */
            if (num1 <= 0L || num2 <= 0L || den1 <= 0L || den2 <= 0L) break;
            long q1 = num1 / den1;
            long q2 = num2 / den2;
            if (q1 != q2 || q1 == 0L) break;
            /*
             * Matrix update:
             *
             * [A B] <- [C D]
             * [C D]    [A-qC B-qD]
             *
             * C and D are SIGNED. In particular, D becomes -q on
             * the very first Euclidean step.
             */
            if (signedMulSubWouldOverflow(A, q1, C) || signedMulSubWouldOverflow(B, q1, D)) break;
            /*
             * Approximation update:
             *
             * (aTop,bTop) <- (bTop, aTop-q * bTop)
             */
            if (signedMulSubWouldOverflow(aTop, q1, bTop)) break;
            long nextA = C;
            long nextB = D;
            long nextC = A - q1 * C;
            long nextD = B - q1 * D;
            long nextATop = bTop;
            long nextBTop = aTop - q1 * bTop;
            /*
             * A Euclidean quotient must not overshoot.
             */
            if (nextBTop < 0L) break;
            A = nextA;
            B = nextB;
            C = nextC;
            D = nextD;
            aTop = nextATop;
            bTop = nextBTop;
            steps++;
        }
        return new LehmerStep(A, B, C, D, steps);
    }
    private static boolean signedAddWouldOverflow(long a, long b) {
        long r = a + b;
        return ((a ^ r) & (b ^ r)) < 0;
    }
    /*
     * Tests whether a - q*c fits in a signed long.
     * q is normally positive, but this works for arbitrary signed operands.
     */
    private static boolean signedMulSubWouldOverflow(long a, long q, long c) {
        long product = q * c;
        /*
         * A signed 128-bit product fits in a signed long if the upper
         * 64 bits are just the sign-extension of the low 64 bits.
         */
        long high = Math.multiplyHigh(q, c);
        if (high != (product >> 63))
            return true;
        long result = a - product;
        // Signed subtraction overflow.
        return ((a ^ product) & (a ^ result)) < 0;
    }
    private record LehmerStep(long A, long B, long C, long D, long steps) {}
    private static long trailingZeroBits(long[] x) {
        int limbs = 0;
        while (limbs < x.length && x[limbs] == 0L) limbs++;
        if (limbs == x.length) return 0L;
        return limbs * 64L + Long.numberOfTrailingZeros(x[limbs]);
    }
    private static long[] linearCombinationNonnegative(long[] a, long ca, long[] b, long cb) {
        if (ca == 0) return signedScaleNonnegative(b, cb);
        if (cb == 0) return signedScaleNonnegative(a, ca);
        boolean aNeg = ca < 0;
        boolean bNeg = cb < 0;
        // Lehmer coefficients are small enough that Long.MIN_VALUE
        // should never be possible.
        long[] x = multiplySmall(a, aNeg ? -ca : ca);
        long[] y = multiplySmall(b, bNeg ? -cb : cb);
        if (aNeg == bNeg) {
            // Both terms have the same sign.
            if (aNeg) return null;
            return addMagnitudes(x, y);
        }
        // Opposite signs: magnitude comparison determines result sign.
        int cmp = compareArray(x, y);
        if (cmp == 0) return ZERO_MAG;
        else if (cmp > 0) return aNeg ? null : subtractMagnitudes(x, y);
        else return bNeg ? null : subtractMagnitudes(y, x);
    }
    private static long[] signedScaleNonnegative(long[] a, long c) {
        if (c == 0 || isZeroMagnitude(a)) return ZERO_MAG;
        else if (c < 0) return null;
        else return multiplySmall(a, c);
    }
    private static boolean acceptLehmerPair(long[] oldA, long[] oldB, long[] newA, long[] newB) {
        if (isZeroMagnitude(newA) && isZeroMagnitude(newB)) return false;
        /*
         * oldA is the old maximum because gcdMagnitudes ensures oldA >= oldB.
         * The transformed pair need not preserve ordering; the next outer
         * iteration can swap them.
         */
        if (!isZeroMagnitude(newA) && compareArray(newA, oldA) >= 0) return false;
        if (!isZeroMagnitude(newB) && compareArray(newB, oldA) >= 0) return false;
        /*
         * Avoid accepting the identity/no-progress transform.
         */
        return !equalMagnitudes(newA, oldA) || !equalMagnitudes(newB, oldB);
    }
    private static long[] gcdMagnitudes(long[] g1, long[] g2) {
        long[] a = trimLeadingZerosUntrusted(g1);
        long[] b = trimLeadingZerosUntrusted(g2);
        while (!isZeroMagnitude(b)) {
            if (a.length < b.length) {
                long[] t = a;
                a = b;
                b = t;
            }
            int n = b.length;
            if (n == 1) {
                long B = b[0];
                return (a.length == 1) ? new long[]{gcdLong(a[0], B)} : new long[]{gcdLong(modSmall(a, B), B)};
            }
            /*
             * Lehmer GCD branch
             * If a and b are too different in size, leading63AtShift()
             * naturally gives bTop == 0 and Lehmer returns zero steps.
             */
            if (n > 6) {
                LehmerStep lm = lehmerStep(a, b);
                if (lm.steps() >= 5) {
                    long[] na = linearCombinationNonnegative(a, lm.A(), b, lm.B());
                    if (na != null) {
                        long[] nb = linearCombinationNonnegative(a, lm.C(), b, lm.D());
                        if (nb != null && acceptLehmerPair(a, b, na, nb)) {
                            a = na;
                            b = nb;
                            continue;
                        }
                    }
                }
            }
            /*
             * Binary GCD branch
             */
            if (n <= 12 && a.length - n <= 2) {
                long aShift = trailingZeroBits(a);
                long bShift = trailingZeroBits(b);
                shiftRightBitsInPlace(a, aShift);
                shiftRightBitsInPlace(b, bShift);
                for (;;) {
                    int cmp = compareArray(a, b);
                    if (cmp == 0)
                        return shiftLeftBits(a, Math.min(aShift, bShift));
                    else if (cmp > 0) {
                        subtractMagnitudeInPlace(a, b);
                        shiftRightBitsInPlace(a, trailingZeroBits(a));
                    } else {
                        subtractMagnitudeInPlace(b, a);
                        shiftRightBitsInPlace(b, trailingZeroBits(b));
                    }
                }
            }
            long[] r = modMagnitudes(a, b);
            a = b;
            b = r;
        }
        return a;
    }
    static long gcdLong(long a, long b) {
        if (a == 0) return b;
        if (b == 0) return a;
        int aZeros = Long.numberOfTrailingZeros(a);
        int bZeros = Long.numberOfTrailingZeros(b);
        a >>>= aZeros;
        b >>>= bZeros;
        while (a != b) {
            if (Long.compareUnsigned(a, b) > 0) {
                a -= b;
                a >>>= Long.numberOfTrailingZeros(a);
            } else {
                b -= a;
                b >>>= Long.numberOfTrailingZeros(b);
            }
        }
        return a << Math.min(aZeros, bZeros);
    }
    private record GcdInverseMagnitude(long[] gcd, long[] inverse) {}
    private static GcdInverseMagnitude gcdInverseMagnitudes(long[] a, long[] n) {
        a = trimLeadingZerosTrusted(a);
        n = trimLeadingZerosTrusted(n);
        long[] oldR = a.clone();
        long[] r = n.clone();
        // Coefficient of 'a'.
        long[] oldX = ONE_MAG;
        boolean oldXNegative = false;
        long[] x = ZERO_MAG;
        boolean xNegative = false;

        while (!isZeroMagnitude(r)) {
            long[][] qr = divideMagnitudes(oldR, r, true);
            long[] q = qr[0];
            oldR = r;
            r = qr[1];
            // nextX = oldX - q*x
            long[] qx = multiplyMagnitudes(q, x);
            long[] nextX;
            boolean nextXNegative;
            if (oldXNegative != xNegative) {
                // oldX and -q*x have the same sign.
                nextX = addMagnitudes(oldX, qx);
                nextXNegative = oldXNegative;
            } else {
                nextXNegative = compareArray(oldX, qx) < 0;
                if (!nextXNegative) nextX = subtractMagnitudes(oldX, qx);
                else nextX = subtractMagnitudes(qx, oldX);
                if (isZeroMagnitude(nextX))
                    nextXNegative = false;
            }
            oldX = x;
            oldXNegative = xNegative;
            x = nextX;
            xNegative = nextXNegative;
        }
        if (!isOneMagnitude(oldR))
            return new GcdInverseMagnitude(oldR, null);
        long[] inverse = modMagnitudes(oldX, n);
        if (oldXNegative && !isZeroMagnitude(inverse))
            inverse = subtractMagnitudes(n, inverse);
        return new GcdInverseMagnitude(oldR, inverse);
    }
    /**
     * Returns the greatest common denominator of <code>this</code> and <code>other</code>.
     * @param other The other integer that shares the gcd
     * @return GCD(<code>this</code>, <code>other</code>)
     */
    public LargeInteger gcd(LargeInteger other) {
        if (other.isZero()) return this;
        else if (isZero()) return other;
        else return new LargeInteger(gcdMagnitudes(limbs, other.limbs));
    }
    /**
     * Returns the least common multiple of <code>this</code> and <code>other</code>.
     * @param other The other integer that shares the lcm
     * @return LCM(<code>this</code>, <code>other</code>)
     */
    public LargeInteger lcm(LargeInteger other) {
        if (isZero() || other.isZero()) return ZERO;
        return divide(gcd(other)).multiply(other).abs();
    }
    public LargeInteger lcm(LargeInteger... atoms) {
        LargeInteger result = ZERO;
        for (LargeInteger atom : atoms) result = result.lcm(atom);
        return result;
    }

    //--- Primality checking and nextProbablyPrimeInteger ---//

    public static int[] sieve(int bound) {
        if (bound < 2) return new int[0];
        if (bound == 2) return new int[]{2};
        // composite[I] represents odd limbs 2*I + 3.
        // I = 0 -> 3
        // I = 1 -> 5
        // I = 2 -> 7
        int oddCount = (bound - 1) >>> 1;
        boolean[] composite = new boolean[oddCount];
        for (int p = 3; p * p <= bound; p += 2) {
            int pIndex = (p - 3) >>> 1;
            if (!composite[pIndex]) {
                long start = (long) p * p;
                for (long multiple = start; multiple <= bound; multiple += 2L * p)
                    composite[(int) ((multiple - 3) >>> 1)] = true;
            }
        }
        int count = 1; // prime 2
        for (int i = 0; i < oddCount; i++) if (!composite[i]) count++;
        int[] primes = new int[count];
        primes[0] = 2;
        int index = 1;
        for (int i = 0; i < oddCount; i++) {
            if (!composite[i])
                primes[index++] = 2 * i + 3;
        }
        return primes;
    }
    public static long[] sieveBlock(long lowerInclusive, long upperExclusive, int[] basePrimes) {
        if (lowerInclusive < 0)
            throw new IllegalArgumentException("lower bound must be nonnegative");
        if (upperExclusive < lowerInclusive)
            throw new IllegalArgumentException("upper bound must be >= lower bound");
        if (lowerInclusive == upperExclusive) return new long[0];
        boolean containsTwo = lowerInclusive <= 2L && 2L < upperExclusive;
        long oddLower = Math.max(lowerInclusive, 3L);
        if ((oddLower & 1L) == 0L) oddLower++;
        if (oddLower >= upperExclusive)
            return containsTwo ? TWO_MAG : new long[0];
        // Number of odd integers in [oddLower, upperExclusive).
        long oddCount = ((upperExclusive - oddLower) + 1L) >>> 1;
        if (oddCount > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Sieve block is too large: " + oddCount + " odd candidates");
        boolean[] composite = new boolean[(int) oddCount];
        for (int basePrime : basePrimes) {
            if (basePrime == 2) continue;
            long pSquared = (long) basePrime * (long) basePrime;
            // No composite in this block can have this prime as its
            // smallest factor once p² reaches the block's upper bound.
            if (pSquared >= upperExclusive) break;
            long first;
            if (pSquared >= oddLower) first = pSquared;
            else {
                long remainder = oddLower % (long) basePrime;
                long offset = remainder == 0L ? 0L : (long) basePrime - remainder;
                // Avoid overflow and skip if the first multiple lies
                // outside this block.
                if (offset >= upperExclusive - oddLower)
                    continue;
                first = oddLower + offset;
            }
            // Only odd candidates are represented.
            if ((first & 1L) == 0L) {
                if ((long) basePrime >= upperExclusive - first)
                    continue;
                first += basePrime;
            }
            long step = (long) basePrime << 1;
            for (long multiple = first; multiple < upperExclusive; ) {
                composite[(int) ((multiple - oddLower) >>> 1)] = true;
                // Prevent multiple += step from overflowing and avoid
                // advancing past the interval.
                if (multiple > upperExclusive - step) break;
                multiple += step;
            }
        }
        int count = containsTwo ? 1 : 0;
        for (boolean value : composite)
            if (!value) count++;
        long[] primes = new long[count];
        int index = 0;
        if (containsTwo) primes[index++] = 2L;
        for (int i = 0; i < composite.length; i++) {
            if (!composite[i])
                primes[index++] = oddLower + ((long) i << 1);
        }
        return primes;
    }
    public static final int[] VERY_SMALL_PRIMES = sieve(1000);
    public static final int[] SMALL_PRIMES = sieve(1_000_000);
    public boolean isProbablyPrime() {
        if (isNegative() || isZero() || isOne()) return false;
        //small test
        for (int k: VERY_SMALL_PRIMES)
            if (modSmall(limbs, k) == 0)
                return limbs.length == 1 && limbs[0] == k;
        if (compareSmall(limbs, 1000000) < 0) return true;
        if ((limbs[0] & 3L) == 1L &&
                isZeroMagnitude(sqrtFloorMagnitude(limbs).remainder()))
            return false;
        return isAProbablyPrimeMagnitude(limbs);
    }
    private static boolean isAProbablyPrimeMagnitude(long[] n) {
        long[] nMinusOne = subtractSmall(n, 1);
        long s = trailingZeroBits(nMinusOne);
        long[] d = shiftRightBits(nMinusOne, s);
        long bits = bitLength(n);
        if (bits <= 64) {
            long modulus = n[0];
            long minusOne = modulus - 1;
            long[] bases = {2, 325, 9375, 28178, 450775, 9780504, 1795265022};
            for (long base : bases) {
                long witness = Long.remainderUnsigned(base, modulus);
                if (witness == 0) continue;
                long x = powMod64(new long[]{witness}, d, modulus);
                if (x == 1L || x == minusOne) continue;
                boolean passed = false;
                for (long r = 1; r < s; r++) {
                    x = remainder128By64(Math.unsignedMultiplyHigh(x, x), x * x, modulus);
                    if (x == minusOne) {
                        passed = true;
                        break;
                    }
                    if (x == 1L) return false;
                }
                if (!passed) return false;
            }
            return true;
        }
        MontgomeryReducer reducer = new MontgomeryReducer(n);
        //Use Lucas-Lehmer when applicable.
        long[] nPlusOne = addSmall(reducer.modulus, 1);
        long s2 = trailingZeroBits(nPlusOne);
        long[] d2 = shiftRightBits(nPlusOne, s2);
        if (isOneMagnitude(d2)) {
            /*
            Lucas-Lehmer loop:
            For N = 2^n-1, start with current = 4.
            Perform current = (current^2 - 2) mod N. Do this n-2 times.
            N is prime if current at the end comes as 0.
            Unlike with Miller-Rabin and Lucas-Selfridge, the results for this
            method are always fully certain.
             */
            long[] current = reducer.toMontgomery(4);
            long[] twoMont = reducer.toMontgomery(2);
            for (long k = 0; k < s2 - 2; k++)
                current = reducer.subtract(reducer.square(current), twoMont);
            return isZeroMagnitude(current);
        }
        // Montgomery representations, computed once.
        long[] oneMont = reducer.rModM;
        long[] minusOneMont = subtractMagnitudes(reducer.modulus, oneMont);
        // randomMagnitudeBetween uses an exclusive upper bound:
        // 2 <= witness < n - 1, therefore witness <= n - 2.
        int rounds;
        if (bits < 100)
            return passesMillerRabin(50, reducer, oneMont, minusOneMont, d, s);
        else if (bits < 256) rounds = 25;
        else if (bits < 512) rounds = 16;
        else if (bits < 768) rounds = 9;
        else if (bits < 1024) rounds = 4;
        else rounds = 2;
        return passesMillerRabin(rounds, reducer, oneMont, minusOneMont, d, s)
                && passesLucasSelfridge(reducer, d2, s2, oneMont);
    }
    private static boolean passesMillerRabin(int rounds, MontgomeryReducer reducer, long[] oneMont,
                                             long[] minusOneMont, long[] d, long s) {
        for (int round = 0; round < rounds; round++) {
            long[] witness;
            do witness = randomMagnitudeBetween(ONE_MAG, reducer.modulus);
            while (equalMagnitudes(witness, oneMont) || equalMagnitudes(witness, minusOneMont));
            long[] x = reducer.powMontgomery(witness, d, new long[reducer.k]);
            if (equalMagnitudes(x, oneMont) || equalMagnitudes(x, minusOneMont)) continue;
            boolean passed = false;
            for (long r = 1; r < s; r++) {
                reducer.squareInto(x, x);
                if (equalMagnitudes(x, minusOneMont)) {
                    passed = true;
                    break;
                }
                if (equalMagnitudes(x, oneMont))
                    return false;
            }
            if (!passed) return false;
        }
        return true;
    }
    private static boolean passesLucasSelfridge(MontgomeryReducer reducer, long[] d2, long s2, long[] oneMont) {
        // Selfridge parameters:
        // P = 1
        // Q = (1 - D) / 4
        long D = 5L;
        while (jacobiSmallNumerator(D, reducer.modulus) != -1)
            D = D > 0 ? -D - 2 : -D + 2;
        long Q = (1L - D) >> 2;
        long[] dMont = reducer.toMontgomery(signedSmallMod(D, reducer.modulus));
        long[] qMont = reducer.toMontgomery(signedSmallMod(Q, reducer.modulus));
        // U_0 = 0
        // V_0 = 2
        // Q^0 = 1
        long[] U = ZERO_MAG;
        long[] V = reducer.add(oneMont, oneMont);
        long[] Qk = oneMont;
        /*
         * Process index from its most significant bit downward.
         * Current state represents index k.
         * Doubling gives 2k.
         * A set bit then advances to 2k + 1.
         */
        for (long bit = bitLength(d2); bit-- > 0; ) {
            // Double: k -> 2k
            long[] doubledU = reducer.multiply(U, V);
            long[] doubledV = reducer.subtract(reducer.square(V), reducer.add(Qk, Qk));
            long[] doubledQ = reducer.square(Qk);
            U = doubledU;
            V = doubledV;
            Qk = doubledQ;
            if (testBit(d2, bit)) {
                // Save the 2k values because both formulas use them.
                long[] oldU = U;
                long[] oldV = V;
                // P = 1:
                // U_(2k + 1) = (U_(2k) + V_(2k)) / 2
                U = reducer.half(reducer.add(oldU, oldV));
                // V_(2k + 1) = (D*U_(2k) + V_(2k)) / 2
                V = reducer.half(reducer.add(reducer.multiply(dMont, oldU), oldV));
                reducer.multiplyInto(Qk, qMont, Qk);
            }
        }
        if (isZeroMagnitude(U) || isZeroMagnitude(V)) return true;
        for (int r = 1; r < s2; r++) {
            V = reducer.subtract(reducer.square(V), reducer.add(Qk, Qk));
            reducer.squareInto(Qk, Qk);
            if (isZeroMagnitude(V)) return true;
        }
        return false;
    }
    private static long[] signedSmallMod(long value, long[] n) {
        if (value == 0L) return ZERO_MAG;
        if (value == Long.MIN_VALUE)
            throw new IllegalArgumentException("Signed value too small");
        boolean negative = value < 0L;
        long magnitude = negative ? -value : value;
        long[] reduced = modMagnitudes(new long[]{magnitude}, n);
        if (!negative || isZeroMagnitude(reduced)) return reduced;
        return subtractMagnitudes(n, reduced);
    }
    private static int jacobiSmallNumerator(long a, long[] n) {
        // Assumes n is a positive odd integer greater than 1.
        if (isEvenMagnitude(n))
            throw new IllegalArgumentException("Jacobi denominator must be odd");
        if (a == 0L) return 0;
        if (a == Long.MIN_VALUE)
            throw new IllegalArgumentException("Unsupported numerator");
        int result = 1;
        // (-1/n) = -1 exactly when n ≡ 3 mod 4.
        if (a < 0L) {
            a = -a;
            if ((n[0] & 3L) == 3L)
                result = -result;
        }
        // Remove powers of 2 from A until A becomes odd.
        int twos = Long.numberOfTrailingZeros(a);
        a >>>= twos;
        // (2/n) = -1 when n ≡ 3 or 5 mod 8.
        if ((twos & 1) != 0) {
            long nMod8 = n[0] & 7L;
            if (nMod8 == 3L || nMod8 == 5L)
                result = -result;
        }
        if (a == 1L) return result;
        // Quadratic reciprocity:
        // (a/n) = -(n/a) if both a and n are 3 mod 4.
        if ((a & 3L) == 3L && (n[0] & 3L) == 3L)
            result = -result;
        return result * jacobiPositiveLong(modSmall(n, a), a);
    }
    private static int jacobiPositiveLong(long a, long n) {
        // Assumes 0 <= a, n > 0, and n is odd.
        if (a < 0 || n <= 0 || (n & 1L) == 0L)
            throw new IllegalArgumentException("Jacobi requirements: 0<=a, n>0, and n is odd");
        int result = 1;
        while (a != 0L) {
            int twos = Long.numberOfTrailingZeros(a);
            a >>>= twos;
            if ((twos & 1) != 0) {
                long nMod8 = n & 7L;
                if (nMod8 == 3L || nMod8 == 5L)
                    result = -result;
            }
            if ((a & 3L) == 3L && (n & 3L) == 3L)
                result = -result;
            long temp = a;
            a = n % temp;
            n = temp;
        }
        return n == 1L ? result : 0;
    }
    private static final int[] WHEEL_30_RESIDUES = {1, 7, 11, 13, 17, 19, 23, 29};
    private static final int[] WHEEL_30_STEPS = {6, 4, 2, 4, 2, 4, 6, 2};
    @SuppressWarnings("unused")
    public LargeInteger nextProbablyPrimeInteger() {
        if (lessThan(TWO)) return TWO;
        else if (lessThan(THREE)) return THREE;
        else if (lessThan(FIVE)) return FIVE;
        else if (lessThan(SEVEN)) return SEVEN;
        LargeInteger candidate = add(1);
        int rem30 = (int) candidate.mod(30);
        int wheelIndex = 0;
        for (int i = 1; i < WHEEL_30_RESIDUES.length; i++)
            if (WHEEL_30_RESIDUES[i] >= rem30) {
                wheelIndex = i;
                break;
            }
        int delta = WHEEL_30_RESIDUES[wheelIndex] - rem30;
        if (delta < 0) delta += 30;
        candidate = candidate.add(delta);
        for (;;) {
            if (candidate.isProbablyPrime()) return candidate;
            candidate = candidate.add(WHEEL_30_STEPS[wheelIndex]);
            if (++wheelIndex == WHEEL_30_STEPS.length)
                wheelIndex = 0;
        }
    }
    private LargeInteger nextProbablyPrimeInteger(int maxCandidates) {
        if (lessThan(TWO)) return TWO;
        if (lessThan(THREE)) return THREE;
        if (lessThan(FIVE)) return FIVE;

        LargeInteger candidate = add(1);

        int rem30 = (int) candidate.mod(30);
        int wheelIndex = 0;

        for (int i = 0; i < WHEEL_30_RESIDUES.length; i++) {
            if (WHEEL_30_RESIDUES[i] >= rem30) {
                wheelIndex = i;
                break;
            }
        }
        int delta = WHEEL_30_RESIDUES[wheelIndex] - rem30;
        if (delta < 0) delta += 30;
        candidate = candidate.add(delta);
        for (int tested = 0; tested < maxCandidates; tested++) {
            if (candidate.isProbablyPrime())
                return candidate;
            candidate = candidate.add(WHEEL_30_STEPS[wheelIndex]);
            if (++wheelIndex == WHEEL_30_STEPS.length)
                wheelIndex = 0;
        }
        return null;
    }

    //--- Legendre and Jacobi symbol (why) ---//

    @SuppressWarnings("unused")
    public int legendreSymbol(LargeInteger other) {
        if (other.isNegative() || other.isEven() || !other.isProbablyPrime())
            throw new IllegalArgumentException("Legendre symbol requires an odd prime modulus");
        int result = 1;
        if (isNegative())
            if (other.mod(3) == 1) result = -1;
        long[] exponent = shiftRightBits(subtractSmall(other.limbs, 1), 1);
        long[] m = new MontgomeryReducer(other.limbs).powMod(modMagnitudes(limbs, other.limbs), exponent);
        if (isZeroMagnitude(m)) return 0;
        if (isOneMagnitude(m)) return result;
        if (equalMagnitudes(m, subtractSmall(other.limbs, 1))) return -result;
        throw new IllegalStateException("Invalid Legendre result; " + other + " may not be prime");
    }
    @SuppressWarnings("unused")
    public int jacobiSymbol(LargeInteger other) {
        if (other.isNegative() || other.isEven())
            throw new IllegalArgumentException("Jacobi denominator must be positive and odd");
        LargeInteger a = mod(other);
        int result = 1;
        while (!a.isZero()) {
            while (a.isEven()) {
                a = a.shiftRight(1);
                int nMod8 = (int) other.mod(8);
                if (nMod8 == 3 || nMod8 == 5)
                    result = -result;
            }
            LargeInteger temp = a;
            a = other;
            other = temp;
            if (a.mod(4) == 3 && other.mod(4) == 3) result = -result;
            a = a.mod(other);
        }
        return other.isOne() ? result : 0;
    }

    //--- Factorization(oh boy HERE WE GO) ---//

    private static final class MagnitudeKey {
        final long[] key;
        final int hashCode;
        MagnitudeKey(long[] magKey) {
            key = magKey;
            hashCode = Arrays.hashCode(key);
        }
        @Override
        public boolean equals(Object o) {return o instanceof MagnitudeKey other && equalMagnitudes(key, other.key);}
        @Override
        public int hashCode() {return hashCode;}
        @Override
        public String toString() {return new LargeInteger(key).toString();}
    }
    enum AlgorithmChoice{
        POLLARD_P_MINUS_ONE,
        BRENT_RHOS,
        FERMAT,
        ELLIPTIC_CURVE_METHOD,
        QUADRATIC_SIEVE,
        NUMBER_FIELD_SIEVE
    }
    private static final class FactorJob {
        final long[] n;
        QSFactorBase factorBase;
        int[] fbIndex;

        boolean primeTestDone;
        boolean cacheTest;
        boolean pm1Done;
        boolean brentDone;
        boolean fermatDone;

        int ecmPlanIndex;
        int nextECMCurve;
        long totalCurveCount;
        double qsSeconds;

        QSFullRelationPool relations;
        QSPartialRelationPool partials;
        PartialGraph partialGraph = new PartialGraph();

        FactorJob(long[] n) {this(n, 0, 0);}

        ECMModulusContext context;
        ECMModulusContext context() {
            return this.context == null ? this.context = new ECMModulusContext(n) : this.context;
        }

        long B = -1;
        long qsBound() {
            if (B < 0) {
                double lnN = lnMagnitude(n);
                return B = Math.max(100, Math.round(Math.exp(QS_FACTOR_BASE_C
                        * StrictMath.sqrt(lnN * StrictMath.log(lnN)))));
            } else return B;
        }

        private FactorJob(long[] n, int ecmPlanIndex, int nextECMCurve) {
            this.n = trimLeadingZerosTrusted(n);
            this.ecmPlanIndex = ecmPlanIndex;
            this.nextECMCurve = nextECMCurve;
            factorBase = null;
        }
        /*
         * The remaining cofactor has already been subjected to the parent's
         * unsuccessful ECM batches, so resume its effort schedule. The new
         * modulus needs its own Montgomery context and QS relations.
         * Primality and factor-cache tests must run again for this modulus.
         */
        FactorJob remainingCofactor(long[] cofactor) {
            FactorJob next = new FactorJob(cofactor, ecmPlanIndex, nextECMCurve);
            next.totalCurveCount = totalCurveCount;
            next.fermatDone = fermatDone;
            next.pm1Done = pm1Done;
            next.brentDone = brentDone;
            return next;
        }
    }
    private static void splitAndQueue(ArrayDeque<FactorJob> work, FactorJob parent, long[] candidate) {
        long[] h = divideMagnitudesExact(parent.n, candidate);
        // Discovered factor: new branch. Remaining cofactor: continue prior effort.
        FactorJob gJob = new FactorJob(candidate);
        FactorJob hJob = parent.remainingCofactor(h);
        /*
         * process the smaller child first; addFirst is LIFO
         */
        if (compareArray(candidate, h) <= 0) {
            work.addFirst(hJob);
            work.addFirst(gJob);
        } else {
            work.addFirst(gJob);
            work.addFirst(hJob);
        }
    }
    private static void advanceECMBatch(FactorJob task, ECMPlan plan, boolean completed) {
        task.nextECMCurve += 64;
        if (completed) task.totalCurveCount += 64;
        if (task.nextECMCurve == plan.curves()) {
            task.nextECMCurve = 0;
            task.ecmPlanIndex++;
        } else if (task.nextECMCurve > plan.curves())
            throw new AssertionError("ECM batch advanced beyond plan's curve allocation");
    }
    private static boolean isNontrivialFactor(long[] d, long[] n)
    {return !isOneMagnitude(d) && compareArray(d, n) < 0 && isZeroMagnitude(modMagnitudes(n, d));}
    private static void addFactor(LargeMap<MagnitudeKey, Long> map, MagnitudeKey base, long exponent) {
        if (map.containsKey(base)) {
            long exp = map.remove(base);
            map.put(base, exp+exponent);
        } else map.put(base, exponent);
    }

    /**
     * Method used as the final step in {@link #factorMagnitude(long[], boolean, boolean)}.
     * Before returning, the resulting factorization is verified against
     * the original magnitude and its factors are sorted. Factors discovered
     * through different algebraic or numerical paths are accumulated with
     * their corresponding multiplicities.</p>
     * <p>The fundamental invariant of this method is</p>
     * <pre> n = product(f_i ^ e_i) </pre>
     * <p>regardless of whether every returned factor is prime.
     * @param factors The factor map to be verified against N
     * @param N The original integer that should be the parent for the factor map
     */
    private static void verifyAndSortFactors(LargeMap<MagnitudeKey, Long> factors, long[] N) {
        long[] product = ONE_MAG;
        for (MagnitudeKey MK: factors.keys())
            product = multiplyMagnitudes(product, power(MK.key, factors.get(MK)));
        if (!equalMagnitudes(product, N))
            throw new AssertionError("Factor map does not reconstruct parent");
        for (LargeInteger i = ZERO; i.lessThan(factors.size()); i = i.add(1))
            for (LargeInteger j = i.add(1); j.lessThan(factors.size()); j = j.add(1))
                if (compareArray(factors.keyAtIndex(i).key, factors.keyAtIndex(j).key) > 0)
                    factors.swapIndices(i, j);
    }
    /**
     * Decomposes an unsigned integer magnitude into multiplicative factors using
     * a combination of algebraic identities, numerical factorization algorithms,
     * and previously cached factorization results.
     *
     * <p>This method does not guarantee complete prime factorization. Composite
     * cofactors that cannot be decomposed within the configured computational
     * effort are retained as atomic factors. Nevertheless, the product of all
     * returned factors, raised to their respective multiplicities, is guaranteed
     * to equal the original magnitude exactly.</p>
     *
     * <h2>1. Trivial cases</h2>
     *
     * <ul>
     *     <li>Zero is represented by the factor 0 with multiplicity 1.</li>
     *     <li>One is represented by an empty factorization.</li>
     * </ul>
     *
     * <h2>2. Structural and algebraic factorization</h2>
     *
     * <p>For sufficiently large magnitudes, structural properties of the integer
     * are examined before computationally expensive general-purpose factorization
     * algorithms are attempted. Exploiting an algebraic identity can reduce a
     * difficult factorization problem into several substantially smaller ones.</p>
     *
     * <h3>Perfect power detection</h3>
     *
     * <p>If the magnitude can be expressed as {@code n = a^k}, where
     * {@code k > 1}, only the base {@code a} needs to be factored.
     * The multiplicities of its factors are subsequently multiplied by
     * {@code k}.</p>
     *
     * <h3>Difference of squares</h3>
     *
     * <p>Fermat's factorization method attempts to express the magnitude as</p>
     *
     * <pre>
     * n = a^2 - b^2
     *   = (a - b)(a + b).
     * </pre>
     *
     * <p>This method is particularly effective when the magnitude contains
     * two factors that are close in value. The search is bounded to avoid
     * excessive work on integers for which this representation is not
     * computationally useful.</p>
     *
     * <h3>Perfect powers minus one</h3>
     *
     * <p>If {@code n + 1} is a perfect power, the identity</p>
     *
     * <pre>
     * a^k - 1 = product of Phi_d(a), for all d dividing k
     * </pre>
     *
     * <p>is used, where {@code Phi_d(x)} denotes the d-th cyclotomic
     * polynomial. The evaluated cyclotomic components provide an exact
     * algebraic decomposition of the original magnitude.</p>
     *
     * <h3>Perfect powers plus one</h3>
     *
     * <p>If {@code n - 1} is a perfect power, the magnitude has the form
     * {@code a^k + 1}. Writing {@code k = 2^s * m}, where {@code m} is
     * odd, gives</p>
     *
     * <pre>
     * b = a^(2^s)
     * a^k + 1 = b^m + 1
     *         = product of Phi_(2d)(b), for all d dividing m.
     * </pre>
     *
     * <p>This reduction permits cyclotomic decomposition even when the
     * original exponent is even.</p>
     *
     * <h3>Aurifeuillian factorization</h3>
     *
     * <p>Integers of the form {@code a^k - 1} or {@code a^k + 1} are also
     * examined for applicable Aurifeuillian identities.</p>
     *
     * <p>Aurifeuillian factorization exploits additional algebraic structure
     * arising from particular relationships between the base and exponent.
     * Such identities can expose integer factors beyond those obtained
     * directly from ordinary cyclotomic decomposition.</p>
     *
     * <p>When an Aurifeuillian decomposition is available, its components
     * are refined against the evaluated cyclotomic components using
     * the greatest common divisors. Each discovered common factor is removed
     * exactly from both corresponding components, preserving the complete
     * multiplicative decomposition without counting overlapping factors
     * more than once.</p>
     *
     * <p>Numerical factorization of the resulting components is deferred
     * until this algebraic refinement is complete. Consequently, expensive
     * numerical algorithms are applied to the refined components rather
     * than unnecessarily large intermediate cofactors.</p>
     *
     * <p>If no Aurifeuillian decomposition is available, the proper
     * cyclotomic components are factored recursively, while the remaining
     * highest-index cyclotomic component continues through the ordinary
     * factorization pipeline.</p>
     *
     * <h2>3. Trial division</h2>
     *
     * <p>Factors belonging to the precomputed small-prime table are removed
     * by repeated exact division. Their multiplicities are recorded in the
     * factor map.</p>
     *
     * <p>Trial division operates directly on the unsigned limb representation,
     * using specialized small-integer remainder and in-place division
     * routines to minimize arbitrary-precision arithmetic and allocation
     * overhead.</p>
     *
     * <p>After exhausting the small-prime table, sufficiently small
     * remaining cofactors are accepted directly.</p>
     *
     * <h2>4. Numerical factorization</h2>
     *
     * <p>Remaining cofactors are managed using an explicit work stack.
     * Each factorization job maintains its own algorithmic progress,
     * preventing completed stages from being unnecessarily repeated.</p>
     *
     * <p>Whenever a nontrivial factor {@code f} of a cofactor {@code n}
     * is discovered, the problem is replaced by two smaller factorization
     * jobs corresponding to {@code f} and {@code n / f}.</p>
     *
     * <h3>Primality testing</h3>
     *
     * <p>Each previously untested cofactor undergoes probable-prime testing.
     * Cofactors that pass the primality test are added directly to the
     * result without attempting further decomposition.</p>
     *
     * <h3>Persistent factorization cache</h3>
     *
     * <p>Before attempting numerical factorization, the factorization cache
     * is queried for a previously discovered nontrivial divisor. Successful
     * numerical factorizations are stored in the cache together with the
     * algorithm responsible for discovering the factor.</p>
     *
     * <p>This allows subsequent factorization requests to reuse expensive
     * computations performed during earlier requests.</p>
     *
     * <h3>Pollard's p - 1 method</h3>
     *
     * <p>Pollard's p - 1 method attempts to exploit the multiplicative group
     * modulo an unknown prime divisor {@code p} of the cofactor.</p>
     *
     * <p>If {@code p - 1} is sufficiently smooth with respect to the
     * configured bounds, a suitable modular exponentiation followed by
     * the greatest common divisor calculation may reveal {@code p} without
     * determining the other factors of the cofactor.</p>
     *
     * <h3>Brent's variant of Pollard's rho method</h3>
     *
     * <p>Brent's rho method searches for collisions in a pseudorandom
     * modular sequence. A collision modulo an unknown prime divisor may
     * reveal that divisor through the greatest common divisor calculation.</p>
     *
     * <p>This method is particularly useful for discovering relatively
     * small prime factors of much larger composite integers.</p>
     *
     * <h3>Elliptic Curve Method (ECM)</h3>
     *
     * <p>Lenstra's Elliptic Curve Method attempts to discover factors by
     * performing elliptic curve arithmetic modulo the composite cofactor.</p>
     *
     * <p>Unlike Pollard's p - 1 method, ECM can vary the underlying group
     * by selecting different elliptic curves. Its effectiveness depends
     * primarily on the size of the factor being sought rather than the
     * size of the entire composite integer.</p>
     *
     * <p>ECM is executed using progressively more expensive parameter
     * plans. Curves are processed in parallel batches of up to 64,
     * permitting multiple independent factorization attempts to proceed
     * simultaneously.</p>
     *
     * <p>When a worker discovers a nontrivial factor, the current batch
     * is abandoned and the discovered factor and complementary cofactor
     * are scheduled for further factorization. Abandoned curves are not
     * incorrectly counted as completed work.</p>
     *
     * <h3>Self-Initializing Quadratic Sieve (SIQS)</h3>
     *
     * <p>The Self-Initializing Quadratic Sieve attempts to construct a
     * nontrivial congruence of squares</p>
     *
     * <pre>
     * x^2 = y^2 (mod n),
     * </pre>
     *
     * <p>from which a factor may be recovered by computing</p>
     *
     * <pre>
     * gcd(x - y, n) or gcd(x + y, n).
     * </pre>
     *
     * <p>The quadratic sieve pipeline includes polynomial generation,
     * modular root computation, sieving, smooth and partial relation
     * collection, sparse matrix filtering, linear dependency computation
     * over GF(2), and factor reconstruction.</p>
     *
     * <p>Unlike ECM, the expected cost of the quadratic sieve depends
     * primarily on the size of the entire composite cofactor. It is
     * therefore particularly useful for factoring integers whose prime
     * factors are of comparable magnitude.</p>
     *
     * <h2>5. Adaptive ECM/SIQS scheduling</h2>
     *
     * <p>For cofactors containing at most 60 decimal digits, the quadratic
     * sieve is attempted directly with a fixed computational allowance.</p>
     *
     * <p>For larger cofactors within the supported quadratic sieve range,
     * the dispatcher initially favors ECM to search for relatively small
     * factors. Once sufficient ECM effort has been expended, estimated
     * ECM and quadratic sieve runtimes are compared.</p>
     *
     * <p>The runtime comparison uses the cofactor's decimal digit count,
     * the current ECM parameter plan, previously completed ECM work,
     * and the factorization job's quadratic sieve runtime estimate.</p>
     *
     * <p>If continuing ECM is estimated to be more expensive than
     * attempting the quadratic sieve, computational effort is allocated
     * to the quadratic sieve. If that attempt does not discover a factor,
     * ECM processing resumes.</p>
     *
     * <p>This scheduling strategy attempts to balance the advantages of
     * ECM on composites containing relatively small factors against the
     * advantages of SIQS on comparatively balanced composites.</p>
     *
     * <h2>6. Computational effort exhaustion</h2>
     *
     * <p>If the configured sequence of ECM plans is exhausted without
     * discovering a nontrivial factor, the unresolved composite is
     * retained as a single atomic factor.</p>
     *
     * <p>Such a factor is not asserted to be prime. Its presence preserves
     * the mathematical correctness of the decomposition while preventing
     * the factorization process from consuming unbounded computational
     * resources.</p>
     *
     * <h2>7. Verification and normalization</h2>
     *
     * See {@link #verifyAndSortFactors(LargeMap, long[])}.
     *
     * <h2>Performance considerations</h2>
     *
     * <p>Integer factorization is substantially more computationally
     * expensive than ordinary arbitrary-precision arithmetic. Execution
     * time and memory consumption depend on the size, algebraic structure,
     * and distribution of prime factors of the input.</p>
     *
     * <p>This method may perform extensive multithreaded computations,
     * allocate large sieving and sparse linear algebra data structures,
     * and access persistent factorization data. Large or difficult
     * inputs may require considerable computational resources.</p>
     *
     * @param limbs   the unsigned integer magnitude, represented by
     *                little-endian 64-bit limbs
     * @param ecmSave whether to enable ECM diagnostic logging and saving
     *                behavior
     * @param qsSave  whether to enable quadratic sieve diagnostic logging
     *                and saving behavior
     * @return a map from factor magnitudes to their positive integer
     *         multiplicities; factors are not necessarily prime
     * @throws ArithmeticException if an exact decomposition or algebraic
     *         refinement invariant is violated
     */
    private static LargeMap<MagnitudeKey, Long> factorMagnitude(long[] limbs, boolean ecmSave, boolean qsSave) {
        LargeMap<MagnitudeKey, Long> factorMap = new LargeMap<>();
        if (isZeroMagnitude(limbs)) {
            factorMap.put(new MagnitudeKey(ZERO_MAG), 1L);
            return factorMap;
        } else if (isOneMagnitude(limbs)) return factorMap;
        long[] copy = limbs.clone();
        if (compareSmall(copy, 1_000_000_000_000L) > 0) {
            //detect x^n
            MagnitudePowerForm powerForm = detectPerfectPower(copy);
            if (powerForm != null) {
                LargeMap<MagnitudeKey, Long> smallerFactors = factorMagnitude(powerForm.base(), ecmSave, qsSave);
                for (MagnitudeKey K : smallerFactors.keys())
                    addFactor(factorMap, K, smallerFactors.get(K) * powerForm.exponent());
                verifyAndSortFactors(factorMap, limbs);
                return factorMap;
            }
            //detect a^2 - b^2 = (a - b)(a + b)
            long[] temp = fermatSearch(copy);
            if (!isOneMagnitude(temp)) {
                LargeMap<MagnitudeKey, Long> tempMap = factorMagnitude(temp, ecmSave, qsSave);
                for (MagnitudeKey MK: tempMap.keys())
                    addFactor(factorMap, MK, tempMap.get(MK));
                tempMap = factorMagnitude(divideMagnitudesExact(copy, temp), ecmSave, qsSave);
                for (MagnitudeKey MK: tempMap.keys())
                    addFactor(factorMap, MK, tempMap.get(MK));
                verifyAndSortFactors(factorMap, limbs);
                return factorMap;
            }
            // detect x^n - 1
            MagnitudePowerForm minusForm = detectPerfectPower(addSmall(copy, 1));
            if (minusForm != null) {
                long[][] FLM = tryAurifeuillianSplit(minusForm.base(), minusForm.exponent(), true);
                ArrayList<long[]> pieces = new ArrayList<>();
                LargeList<LargeInteger> divisors = valueOf(minusForm.exponent()).divisors();
                divisors.removeLast();
                for (LargeInteger d : divisors) {
                    long[] value = ((LargeInteger) LargePolynomial.cyclotomic(d)
                            .evaluateAt(new LargeInteger(minusForm.base()))).limbs;
                    if (isOneMagnitude(value)) continue;
                    if (FLM == null) {
                        copy = divideMagnitudesExact(copy, value);
                        pieces.add(value);
                    } else {
                        for (int i = 0; i < FLM.length && !isOneMagnitude(value); i++) {
                            long[] g = gcdMagnitudes(FLM[i], value);
                            if (isOneMagnitude(g)) continue;
                            pieces.add(g);
                            FLM[i] = divideMagnitudesExact(FLM[i], g);
                            value = divideMagnitudesExact(value, g);
                        }
                        if (!isOneMagnitude(value))
                            throw new ArithmeticException("Incomplete algebraic refinement");
                    }
                }
                // Process Aurifeuillian remainders
                if (FLM != null) {
                    for (long[] A : FLM)
                        if (!isOneMagnitude(A))
                            pieces.add(A);
                }
                // Numerical factorization begins only after refinement.
                for (long[] piece : pieces) {
                    LargeMap<MagnitudeKey, Long> subFactors = factorMagnitude(piece, ecmSave, qsSave);
                    for (MagnitudeKey K : subFactors.keys())
                        addFactor(factorMap, K, subFactors.get(K));
                }
                if (FLM != null) {
                    verifyAndSortFactors(factorMap, limbs);
                    return factorMap;
                }
                // Otherwise, 'copy' retains the unprocessed
                // largest cyclotomic component.
            }
            // detect x^n + 1
            MagnitudePowerForm plusForm = detectPerfectPower(subtractSmall(copy, 1));
            if (plusForm != null) {
                long exponent = plusForm.exponent();
                long[] base = plusForm.base();
                long[][] FLM = tryAurifeuillianSplit(base, exponent, false);
                ArrayList<long[]> pieces = new ArrayList<>();
                while ((exponent & 1L) == 0L) {
                    base = squareMagnitude(base);
                    exponent >>>= 1;
                }
                if (exponent != 1) {
                    LargeList<LargeInteger> divisors = valueOf(exponent).divisors();
                    divisors.removeLast();
                    for (LargeInteger d : divisors) {
                        long[] value = ((LargeInteger) LargePolynomial.cyclotomic(d.shiftLeft(1))
                                .evaluateAt(new LargeInteger(base))).limbs;
                        if (isOneMagnitude(value)) continue;
                        if (FLM == null) {
                            copy = divideMagnitudesExact(copy, value);
                            pieces.add(value);
                        } else {
                            for (int i = 0; i < FLM.length && !isOneMagnitude(value); i++) {
                                long[] g = gcdMagnitudes(FLM[i], value);
                                if (isOneMagnitude(g)) continue;
                                pieces.add(g);
                                FLM[i] = divideMagnitudesExact(FLM[i], g);
                                value = divideMagnitudesExact(value, g);
                            }
                            if (!isOneMagnitude(value))
                                throw new ArithmeticException("Incomplete algebraic refinement");
                        }
                    }
                }
                // Process Aurifeuillian remainders
                if (FLM != null) {
                    for (long[] A : FLM)
                        if (!isOneMagnitude(A))
                            pieces.add(A);
                }
                // Numerical factorization begins only after refinement.
                for (long[] piece : pieces) {
                    LargeMap<MagnitudeKey, Long> subFactors = factorMagnitude(piece, ecmSave, qsSave);
                    for (MagnitudeKey K : subFactors.keys())
                        addFactor(factorMap, K, subFactors.get(K));
                }
                if (FLM != null) {
                    verifyAndSortFactors(factorMap, limbs);
                    return factorMap;
                }
                // Otherwise, 'copy' retains the unprocessed
                // largest cyclotomic component.
            }

        }
        int remLength = effectiveLength(copy);
        for (int k : SMALL_PRIMES) {
            int exponent = 0;
            long rec = Long.divideUnsigned(-1, k);
            while (modSmallInt(copy, remLength, k, rec) == 0) {
                exponent++;
                divideMagnitudeExactInPlace(copy, k);
                while (remLength > 1 && copy[remLength-1] == 0) remLength--;
            }
            if (exponent != 0) addFactor(factorMap, new MagnitudeKey(new long[]{k}), exponent);
            if (remLength == 1 && copy[0] == 1) {
                verifyAndSortFactors(factorMap, limbs);
                return factorMap;
            }
        }
        copy = trimLeadingZerosTrusted(copy);
        if (compareSmall(copy, 1_000_000_000_000L) < 0) {
            addFactor(factorMap, new MagnitudeKey(copy), 1L);
            verifyAndSortFactors(factorMap, limbs);
            return factorMap;
        }
        FactorCacheDatabase cache = null;
        ArrayDeque<FactorJob> work = new ArrayDeque<>();
        work.push(new FactorJob(copy));
        LargeInteger number = new LargeInteger(copy);
        int digits = number.toString().length();
        while (!work.isEmpty()) {
            FactorJob task = work.pop();
            final long[] n = task.n;
            if (!task.primeTestDone) {
                if (isOneMagnitude(n)) continue;
                if (isAProbablyPrimeMagnitude(n)) {
                    // Add prime to result.
                    addFactor(factorMap, new MagnitudeKey(n), 1L);
                    continue;
                }
                task.primeTestDone = true;
            }
            if (cache == null)
                cache = FactorCacheDatabase.instance();
            if (!task.cacheTest) {
                number = new LargeInteger(n);
                digits = number.toString().length();
                LargeInteger cached = cache.findFactor(number);
                if (cached != null) {
                    splitAndQueue(work, task, cached.limbs);
                    continue;
                }
                task.cacheTest = true;
            }
            long[] factor;
            if (!task.fermatDone) {
                factor = fermatSearch(n);
                if (isNontrivialFactor(factor, n)) {
                    cache.save(number, new LargeInteger(factor ), AlgorithmChoice.FERMAT);
                    splitAndQueue(work, task, factor);
                    continue;
                }
                task.fermatDone = true;
            }
            if (!task.pm1Done) {
                factor = pollardPMinusOne(n);
                if (isNontrivialFactor(factor, n)) {
                    cache.save(number, new LargeInteger(factor ), AlgorithmChoice.POLLARD_P_MINUS_ONE);
                    splitAndQueue(work, task, factor);
                    continue;
                }
                task.pm1Done = true;
            }
            if (!task.brentDone) {
                factor = brentRhos(n);
                if (isNontrivialFactor(factor, n)) {
                    cache.save(number, new LargeInteger(factor ), AlgorithmChoice.BRENT_RHOS);
                    splitAndQueue(work, task, factor);
                    continue;
                }
                task.brentDone = true;
            }
            if (task.ecmPlanIndex >= ECM_EXTREME_PLANS.length) {
                System.out.println(number + " is too difficult to factor; treated as composite atomic");
                addFactor(factorMap, new MagnitudeKey(n), 1L);
                continue;
            }
            if (digits <= 60) {
                //Immediately do QS, do not estimate time
                factor = quadraticSieve(task, 1000, qsSave);
                if (isNontrivialFactor(factor, n)) {
                    cache.save(number, new LargeInteger(factor ), AlgorithmChoice.QUADRATIC_SIEVE);
                    splitAndQueue(work, task, factor);
                    continue;
                }
            }
            ECMPlan plan = ECM_EXTREME_PLANS[task.ecmPlanIndex];
            if (digits <= 100 && task.ecmPlanIndex > 5
                    || digits <= 95 && task.ecmPlanIndex > 4
                    || digits <= 90 && task.ecmPlanIndex > 3
                    || digits <= 80 && task.ecmPlanIndex > 2) {
                double ecmSeconds = ECM_RUNTIME.estimateSecondsThrough(task, planForFactorDigits((digits + 1) >>> 1));
                if (task.qsSeconds < ecmSeconds) {
                    if (ecmSave)
                        try (PrintWriter log = new PrintWriter(Files.newBufferedWriter(Path.of("ECM_factorization.log " + new LargeInteger(task.n)),
                                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
                            log.println("SWITCHED TO QS");
                        } catch (IOException e) {throw new RuntimeException(e);}
                    factor = quadraticSieve(task, ecmSeconds, qsSave);
                    if (isNontrivialFactor(factor, n)) {
                        cache.save(number, new LargeInteger(factor), AlgorithmChoice.QUADRATIC_SIEVE);
                        splitAndQueue(work, task, factor);
                        continue;
                    }
                }
            }
            factor = runSixtyFourECMCurves(task, plan, ecmSave);
            if (isNontrivialFactor(factor, n)) {
                cache.save(number, new LargeInteger(factor), AlgorithmChoice.ELLIPTIC_CURVE_METHOD);
                // This batch is abandoned once a worker wins. Do not run it
                // again on the remaining cofactor, but do not count the other
                // curves as completed either.
                advanceECMBatch(task, plan, false);
                splitAndQueue(work, task, factor);
                continue;
            }
            advanceECMBatch(task, plan, true);
            work.push(task);
        }
        verifyAndSortFactors(factorMap, limbs);
        return factorMap;
    }
    private static int[] mobiusSieve(int limit) {
        int[] mu = new int[limit + 1];
        mu[1] = 1;
        for (int d = 1; d <= limit / 2; d++) {
            for (int m = 2 * d; m <= limit; m += d)
                mu[m] -= mu[d];
        }
        return mu;
    }
    private static int[] totientSieve(int limit) {
        int[] phi = new int[limit + 1];
        for (int i = 0; i <= limit; i++)
            phi[i] = i;
        for (int p = 2; p <= limit; p++) {
            if (phi[p] == p) {
                for (int j = p; j <= limit; j += p)
                    phi[j] -= phi[j] / p;
            }
        }
        return phi;
    }
    private static void addSignedTerm(long[][] values, boolean[] negative,
            int index, long[] term, boolean termNegative) {
        if (isZeroMagnitude(term)) return;
        long[] current = values[index];
        if (negative[index] == termNegative) {
            values[index] = addMagnitudes(current, term);
        } else {
            int cmp = compareArray(current, term);
            if (cmp > 0)
                values[index] = subtractMagnitudes(current, term);
            else if (cmp < 0) {
                values[index] = subtractMagnitudes(term, current);
                negative[index] = termNegative;
            } else {
                values[index] = ZERO_MAG;
                negative[index] = false;
            }
        }

        if (isZeroMagnitude(values[index]))
            negative[index] = false;
    }
    private static long[][] tryAurifeuillianSplit(long[] base, long exponent, boolean minusOne) {
        // The base must fit in a positive long <= exponent.
        if (exponent <= 0 || effectiveLength(base) != 1)
            return null;
        long n = base[0];
        if (n < 2 || n > exponent)
            return null;
        // Detect n^(n*(2k+1)) + 1.
        if (exponent % n != 0 || ((exponent / n) & 1L) == 0)
            return null;
        long mod4 = n & 3L;
        if (minusOne) {
            if (mod4 != 1L) return null;
        } else {
            if (mod4 != 2L && mod4 != 3L) return null;
        }
        if (n > Integer.MAX_VALUE / 2)
            return null;
        int nPrime = 2 * (int)n;
        int[] mu = mobiusSieve(nPrime);
        int[] tot = totientSieve(nPrime);
        if (mu[(int)n] == 0)
            return null;
        int d = tot[nPrime] / 2;
        int[] Q = new int[d];
        for (int k = 1; k <= d; k++)
            if ((k & 1) == 1)
                Q[k-1] = jacobiPositiveLong(n, k);
            else {
                int g = (int) gcdLong(k, nPrime);
                Q[k-1] = mu[nPrime / g] * tot[g];
                long r = (((n - 1) & 7L) * (k & 7L)) & 7L;
                Q[k-1] *= r == 0 ? 1 : r == 4 ? -1 : 0;
            }
        long[][] gamma = new long[d + 1][];
        long[][] delta = new long[d][];

        boolean[] gammaNegative = new boolean[d + 1];
        boolean[] deltaNegative = new boolean[d];

        gamma[0] = ONE_MAG;
        delta[0] = ONE_MAG;
        for (int k = 1; k <= d / 2; k++) {
            gamma[k] = ZERO_MAG;
            // gamma[k]
            for (int j = 0; j < k; j++) {
                int qOdd = Q[2*k - 2*j - 2];
                int qEven = Q[2*k - 2*j - 1];
                // + n*qOdd*delta[j]
                addSignedTerm(gamma, gammaNegative, k,
                        multiplySmall(multiplySmall(delta[j], n), Math.abs(qOdd)),
                        deltaNegative[j] ^ (qOdd < 0));
                // - qEven*gamma[j]
                addSignedTerm(gamma, gammaNegative, k,
                        multiplySmall(gamma[j], Math.abs(qEven)),
                        gammaNegative[j] == qEven < 0);
            }
            divideMagnitudeExactInPlace(gamma[k], 2L * k);
            if (k <= (d - 1) / 2) {
                // Start delta[k] with gamma[k]
                delta[k] = gamma[k];
                deltaNegative[k] = gammaNegative[k];
                for (int j = 0; j < k; j++) {
                    int qOdd = Q[2*k - 2*j];
                    boolean qONeg = qOdd < 0;
                    int qEven = Q[2*k - 2*j - 1];
                    // + qOdd*gamma[j]
                    addSignedTerm(delta, deltaNegative, k,
                            multiplySmall(gamma[j], qONeg ? -qOdd : qOdd),
                            gammaNegative[j] ^ qONeg);
                    // - qEven*delta[j]
                    addSignedTerm(delta, deltaNegative, k,
                            multiplySmall(delta[j], Math.abs(qEven)),
                            deltaNegative[j] == qEven < 0);
                }
                divideMagnitudeExactInPlace(delta[k], 2L * k + 1);
            }
        }
        // Palindromic coefficients
        for (int k = d / 2 + 1; k <= d; k++) {
            gamma[k] = gamma[d - k];
            gammaNegative[k] = gammaNegative[d - k];
        }
        for (int k = (d + 1) / 2; k < d; k++) {
            delta[k] = delta[d - 1 - k];
            deltaNegative[k] = deltaNegative[d - 1 - k];
        }

        //Evaluate polynomials
        long[] x = power(base, exponent/n);
        long[][] evaluated = {ZERO_MAG, ZERO_MAG};
        boolean[] negative = new boolean[2];
        for (int i = 0; i <= d; i++) {
            // C = C*x + gamma[i]
            evaluated[0] = multiplyMagnitudes(evaluated[0], x);
            addSignedTerm(evaluated, negative, 0, gamma[i], gammaNegative[i]);
            if (i < d) {
                // D = D*x + delta[i]
                evaluated[1] = multiplyMagnitudes(evaluated[1], x);
                addSignedTerm(evaluated, negative, 1, delta[i], deltaNegative[i]);
            }
        }
        long[] C = evaluated[0];
        long[] D = evaluated[1];
        //Final products to be factored
        long[] nx = sqrtFloorMagnitude(multiplySmall(x, n)).sqrt();
        long[] L = subtractMagnitudes(C, multiplyMagnitudes(nx, D));
        long[] M = addMagnitudes(C,  multiplyMagnitudes(nx, D));
        long[] whole = power(base, exponent);
        long[] numerator = minusOne ? subtractSmall(whole, 1) : addSmall(whole, 1);
        long[] F = divideMagnitudesExact(numerator, multiplyMagnitudes(L, M));
        return new long[][]{F, L, M};
    }
    /**
     * Decomposes this integer into known multiplicative factors.
     * <p>
     *     Factors are prime whenever practical, but complete prime factorization is
     *     not guaranteed. If the remaining composite cofactor exceeds the configured
     *     factoring effort, it is retained as a single atomic factor.
     * <p>
     *     Zero, unlike other integers, does not have a standard factorization, so this method
     *     returns {0 -> 1}. For one, since it is defined as the empty product, {} is returned.
     *     Negative integers are factored normally, except {-1 -> 1} comes before all other factors
     * </p>
     * The returned factors always multiply exactly to this integer.
     * @implNote See {@link #factorMagnitude(long[], boolean, boolean)} for implementation details.
     */
    public LargeMap<LargeInteger, LargeFraction> factor() {return factor(false, false);}

    /**
     *
     * @param ecmSave The elliptic curve method can generate a LOT of data. This parameter determines if
     *                all that data should be saved to a file. While good for reproducibility, it can take
     *                a lot of storage fast.
     * @param qsSave Same case for quadratic sieve. This field determines if QS should have its data saved
     *               or not.
     * @return Multiplicative decomposition of this integer
     */
    public LargeMap<LargeInteger, LargeFraction> factor(boolean ecmSave, boolean qsSave) {
        LargeMap<LargeInteger, LargeFraction> result = new LargeMap<>();
        if (isNegative())
            result.put(NEGATIVE_ONE, LargeFraction.ONE);
        if (isOne()) return result;
        LargeMap<MagnitudeKey, Long> bag = factorMagnitude(limbs, ecmSave, qsSave);
        for (MagnitudeKey factor : bag.keys())
            result.put(new LargeInteger(factor.key), LargeFraction.valueOf(bag.get(factor)));
        return result;
    }
    /**
     * Calculates the number of integers under <code>this</code> that are coprime to <code>this</code>,
     * denoted as ϕ(<code>this</code>). It uses the result {p1^e1, p2^e2, p3^e3, ...} from {@link #factor}
     * and computes <code>this</code>*(1 - 1/p1)*(1 - 1/p2)*(1 - 1/p3)*...
     * @return ϕ(<code>this</code>)
     */
    public LargeInteger totient() {
        if (isZero()) throw new ArithmeticException("Totient undefined for 0");
        LargeInteger n = abs();
        LargeMap<LargeInteger, LargeFraction> factors = n.factor();
        LargeInteger result = this;
        for (LargeInteger p : factors.keys()) {
            if (!p.isProbablyPrime())
                throw new ArithmeticException("Totient requires full prime decomposition");
            result = result.divide(p).multiply(p.subtract(1));
        }
        return result;
    }

    /**
     * Returns a list containing every integer that <code>this</code> is divisible by, including 1 and the
     * integer itself.
     * @return Divisors of this integer
     */
    public LargeList<LargeInteger> divisors() {
        LargeList<LargeInteger> result = new LargeList<>();
        if (isZero() || isOne()) return result;
        LargeMap<LargeInteger, LargeFraction> factorMap = abs().factor();
        result.add(ONE);
        if (factorMap.keyAtIndex(ZERO).isOne()) return result;
        for (LargeInteger p : factorMap.keys()) {
            if (p.isNegative()) continue;
            if (!p.isProbablyPrime())
                throw new ArithmeticException("divisors() requires full prime decomposition");
            LargeInteger exp = factorMap.get(p).numerator;
            LargeInteger currentSize = result.size();
            LargeInteger pPower = ONE;
            for (LargeInteger e = ONE; e.lesserThanOrEqualTo(exp); e = e.add(1)) {
                pPower = pPower.multiply(p);
                for (LargeInteger i = ZERO; i.lessThan(currentSize); i = i.add(1))
                    result.add(result.get(i).multiply(pPower));
            }
        }
        return result;
    }

    /**
     * Uses the result from {@link #divisors()} and sums every element in the list.
     * @return True if the sum evaluates to this integer, false otherwise.
     */
    public boolean isPerfect() {
        if (lesserThanOrEqualTo(ONE)) return false;
        LargeInteger result = ZERO;
        for (LargeInteger I : divisors())
            result = result.add(I);
        return shiftLeft(1).equals(result);
    }

    //--- Pollard P-1 (deep into the rabbit hole) ---//

    private record PM1Plan(int B1, int B2) {}
    private record PM1Stage1Result(long[] state, long[] factor, boolean saturated) {}
    private static long[] pollardPMinusOne(long[] n) {
        if (isEvenMagnitude(n)) return TWO_MAG;
        PM1Plan[] plans = {new PM1Plan(10_000, 250_000), new PM1Plan(20_000, 500_000)};
        // Sieve once through the largest B2.
        int[] primes = sieve(plans[plans.length - 1].B2());
        MontgomeryReducer reducer = new MontgomeryReducer(n);
        long[] state = reducer.toMontgomery(2);
        int previousB1 = 1;
        for (PM1Plan plan : plans) {
            PM1Stage1Result stage1 =
                    pollardPMinusOneStage1(n, state, previousB1, plan.B1(), primes, reducer);
            if (isNontrivialFactor(stage1.factor(), n)) return stage1.factor();
            if (stage1.saturated()) return ONE_MAG;
            state = stage1.state();
            long[] factor = pollardPMinusOneStage2(n, state, primes, plan.B1(), plan.B2(), reducer);
            if (isNontrivialFactor(factor, n)) return factor;
            previousB1 = plan.B1();
        }
        return ONE_MAG;
    }
    private static PM1Stage1Result pollardPMinusOneStage1(long[] n, long[] initialState,
                                                          int oldBound, int newBound, int[] primes,
                                                          MontgomeryReducer reducer) {
        long[] a = Arrays.copyOf(initialState, reducer.k);
        long[] batchStartState = initialState.clone();
        long[] batchExponent = ONE_MAG;

        int batchStartIndex = 0;
        int batchCount = 0;

        for (int i = 0; i < primes.length; i++) {
            int q = primes[i];
            if (q > newBound) break;
            long oldPower = largestPrimePowerAtMost(q, oldBound);
            long newPower = largestPrimePowerAtMost(q, newBound);
            long multiplier = newPower / oldPower;
            if (multiplier == 1) continue;
            batchExponent = multiplySmall(batchExponent, multiplier);
            batchCount++;
            if (batchCount == 256) {
                reducer.powMontgomery(a, batchExponent, a);
                long[] g = gcdMagnitudes(modSubtract(a, reducer.rModM, n), n);
                if (isNontrivialFactor(g, n))
                    return new PM1Stage1Result(a, g, false);
                if (equalMagnitudes(g, n)) {
                    long[] replay = batchStartState.clone();
                    for (int j = batchStartIndex; j <= i; j++) {
                        int p = primes[j];
                        long oldP = largestPrimePowerAtMost(p, oldBound);
                        long newP = largestPrimePowerAtMost(p, newBound);
                        long step = newP / oldP;
                        if (step == 1) continue;
                        reducer.powMontgomery(replay, step, replay);
                        long[] replayGcd = gcdMagnitudes(modSubtract(replay, reducer.rModM, n), n);
                        if (isNontrivialFactor(replayGcd, n))
                            return new PM1Stage1Result(replay, replayGcd, false);
                        if (equalMagnitudes(replayGcd, n))
                            break;
                    }
                    return new PM1Stage1Result(a, ONE_MAG, true);
                }
                System.arraycopy(a, 0, batchStartState, 0, batchStartState.length);
                batchStartIndex = i + 1;
                batchExponent = ONE_MAG;
                batchCount = 0;
            }
        }
        if (batchCount != 0) {
            reducer.powMontgomery(a, batchExponent, a);
            long[] g = gcdMagnitudes(modSubtract(a, reducer.rModM, n), n);
            if (isNontrivialFactor(g, n))
                return new PM1Stage1Result(a, g, false);
            if (equalMagnitudes(g, n))
                return new PM1Stage1Result(a, ONE_MAG, true);
        }
        return new PM1Stage1Result(a, ONE_MAG, false);
    }
    private static final int D = 2310;
    private static final int HALF_D = 1155;
    private static long nearestCenterIndex(long q) {
        long m = q / D;
        if (q % D > HALF_D)
            m++;
        return Math.max(1L, m);
    }
    private static long largestPrimePowerAtMost(long q, long bound) {
        if (q > bound)
            return 1;
        long power = q;
        while (power <= bound / q)
            power *= q;
        return power;
    }
    private static long[] pollardPMinusOneStage2(long[] n, long[] stage1Montgomery,
                                                 int[] primes, int B1, int B2, MontgomeryReducer reducer) {
        int start = primes.length;
        while (primes[start - 1] > B1) start--;
        if (start == primes.length) start = -1;
        if (start >= primes.length || primes[start] > B2) return ONE_MAG;
        long[][] baby = new long[D][];
        long[] power = reducer.rModM.clone();
        for (int r = 1; r < D; r++) {
            reducer.multiplyInto(power, stage1Montgomery, power);
            if (gcdLong(r, D) == 1)
                baby[r] = power;
        }
        // power currently equals x^(D - 1), so this gives x exponentiated to D.
        long[] xToD = reducer.multiply(power, stage1Montgomery);
        int currentM = (primes[start] + D - 1) / D;
        long[] giant = new long[reducer.k];
        reducer.powMontgomery(xToD, currentM, giant);
        long[] product = reducer.rModM.clone();
        long[][] batchTerms = new long[1024][];
        int terms = 0;
        for (int i = start; i < primes.length; i++) {
            int q = primes[i];
            if (q > B2) break;
            int targetM = (q + D - 1) / D;
            while (currentM < targetM) {
                reducer.multiplyInto(giant, xToD, giant);
                currentM++;
            }
            int r = targetM * D - q;
            // q > B1, so q cannot be one of D's small prime factors.
            if (r == 0 || baby[r] == null) continue;
            long[] term = reducer.subtract(giant, baby[r]);
            reducer.multiplyInto(product, term, product);
            batchTerms[terms++] = term;
            if (terms == 1024) {
                long[] factor = checkPM1Stage2Batch(product, batchTerms, terms, n);
                if (isNontrivialFactor(factor, n)) return factor;
                System.arraycopy(reducer.rModM, 0, product, 0, product.length);
                terms = 0;
            }
        }
        if (terms != 0) {
            long[] factor = checkPM1Stage2Batch(product, batchTerms, terms, n);
            if (isNontrivialFactor(factor, n))
                return factor;
        }
        return ONE_MAG;
    }
    private static long[] checkPM1Stage2Batch(long[] product, long[][] terms, int count, long[] n) {
        long[] g = gcdMagnitudes(product, n);
        if (isNontrivialFactor(g, n))
            return g;
        /*
         * Multiple different terms may have supplied different factors,
         * making the product divisible by n. Check the individual
         * terms rather than recomputing exponentiation.
         */
        if (equalMagnitudes(g, n)) {
            for (int i = 0; i < count; i++) {
                g = gcdMagnitudes(terms[i], n);
                if (isNontrivialFactor(g, n))
                    return g;
            }
        }
        return ONE_MAG;
    }

    //--- Brent rhos and Fermat detection of a^2-b^2 (why are you even here) ---//

    private static long[] brentRhos(long[] n) {
        long bits = bitLength(n);
        long steps;
        if (bits <= 128) steps = 2_000_000L;
        else if (bits <= 192) steps = 750_000L;
        else if (bits <= 320) steps = 250_000L;
        else steps = 100_000L;

        MontgomeryReducer reducer = new MontgomeryReducer(n);
        int len = reducer.k;

        // Convert to Montgomery form once.
        long[] y = reducer.toMontgomery(randomMagnitudeBetween(TWO_MAG, subtractSmall(n, 2)));
        long[] c = reducer.toMontgomery(randomMagnitudeBetween(ONE_MAG, subtractSmall(n, 1)));
        long m = 1024;
        long r = 1;
        long times = 0;
        long[] g = ONE_MAG;
        // Fixed-width Montgomery work buffers.
        long[] x    = new long[len];
        long[] ys   = new long[len];
        long[] q;
        long[] diff = new long[len];
        while (isOneMagnitude(g) && times < steps) {
            // x = y
            System.arraycopy(y, 0, x, 0, len);
            // Advance y by r steps.
            for (long i = 0; i < r && times < steps; i++) {
                // y = y^2 + c mod n
                reducer.squareInto(y, y);
                reducer.addInto(y, c, y);
                times++;
            }
            long k = 0;
            while (k < r && isOneMagnitude(g) && times < steps) {
                /*
                 * Save y for Brent's g == n recovery.
                 *
                 * IMPORTANT: cannot do:
                 *
                 *     ys = y;
                 *
                 * anymore, because y is now mutated in place.
                 */
                System.arraycopy(y, 0, ys, 0, len);
                // q = 1 in Montgomery representation.
                q = reducer.rModM.clone();
                long limit = Math.min(m, r - k);
                for (long i = 0; i < limit && times < steps; i++) {
                    // y = y^2 + c mod n
                    reducer.squareInto(y, y);
                    reducer.addInto(y, c, y);
                    /*
                     * diff = x - y mod n.
                     *
                     * Absolute value is unnecessary:
                     *
                     * gcd((x - y) mod n, n) == gcd(x - y, n)
                     */
                    reducer.subtractInto(x, y, diff);
                    if (isZeroMagnitude(diff))
                        return ONE_MAG;
                    // q = q * diff mod n, still Montgomery form.
                    reducer.multiplyInto(q, diff, q);
                    times++;
                }
                /*
                 * q is a Montgomery residue:
                 *
                 * qMont = q * R mod n
                 *
                 * Since gcd(R, n) = 1:
                 *
                 * gcd(qMont, n) == gcd(q, n)
                 *
                 * Therefore no fromMontgomery() is required here.
                 */
                g = gcdMagnitudes(q, n);
                k += m;
            }
            if (r > (Long.MAX_VALUE >>> 1))
                return ONE_MAG;
            r <<= 1;
        }
        if (isNontrivialFactor(g, n))
            return g;
        if (equalMagnitudes(g, n)) {
            /*
             * Brent recovery when the accumulated q caused gcd(q, n) == n.
             * Walk forward from the saved ys one step at a time until an
             * individual difference exposes the factor.
             */
            g = ONE_MAG;
            for (int i = 0; i < 10_000 && isOneMagnitude(g); i++) {
                // ys = ys^2 + c mod n
                reducer.squareInto(ys, ys);
                reducer.addInto(ys, c, ys);
                // diff = x - ys mod n
                reducer.subtractInto(x, ys, diff);
                if (isZeroMagnitude(diff))
                    return ONE_MAG;
                g = gcdMagnitudes(diff, n);
            }
            if (isNontrivialFactor(g, n))
                return g;
        }
        return ONE_MAG;
    }
    private static long[] fermatSearch(long[] n) {
        SqrtResult sr = sqrtFloorMagnitude(n);
        if (isZeroMagnitude(sr.remainder()))
            return sr.sqrt();
        long[] a = addSmall(sr.sqrt(), 1);
        // d = a^2 - n
        long[] d = subtractMagnitudes(squareMagnitude(a), n);
        // Anything beyond 500^2 cannot qualify.
        if (compareSmall(d, 250_000) > 0)
            return ONE_MAG;
        sr = sqrtFloorMagnitude(d);
        if (!isZeroMagnitude(sr.remainder())) return ONE_MAG;
        return subtractMagnitudes(a, sr.sqrt());
    }

    //--- Lenstra Elliptic Curve Method(the one thing I brag about for factorization) ---//
    private static final boolean ecmSave = true;

    private static final class ECMModulusContext {
        final long[] inv4;
        final MontgomeryReducer reducer;
        ECMModulusContext(long[] N) {
            GcdInverseMagnitude eg = gcdInverseMagnitudes(FOUR_MAG, N);
            if (eg.inverse() == null) throw new FactorFoundException(eg.gcd());
            inv4 = eg.inverse();
            reducer = new MontgomeryReducer(N);
        }
    }
    private static final class FactorFoundException extends RuntimeException {
        final long[] factor;
        FactorFoundException(long[] factor) {this.factor = factor;}
    }
    private static final class UnusableECMCurveException extends RuntimeException
    { UnusableECMCurveException(String message) {super(message);}}
    private static final class ECMBatchCancelledException extends RuntimeException
    { ECMBatchCancelledException() {super();}}
    private static void checkECMBatch(AtomicReference<long[]> foundFactor) {
        if (foundFactor.get() != null || Thread.currentThread().isInterrupted())
            throw new ECMBatchCancelledException();
    }
    private static boolean claimECMFactor(AtomicReference<long[]> foundFactor, long[] factor)
    {return foundFactor.compareAndSet(null, factor);}
    private static long[] runSixtyFourECMCurves(FactorJob job, ECMPlan plan, boolean ecmSave) {
        int remainingCurves = plan.curves() - job.nextECMCurve;
        if (remainingCurves <= 0) return ONE_MAG;
        ECMStats stats = new ECMStats();
        ECMCurveWork[] works = new ECMCurveWork[64];
        long[][] sigmas = new long[64][];
        AtomicReference<long[]> foundFactor = new AtomicReference<>();
        try (PrintWriter log = ecmSave ?
                new PrintWriter(Files.newBufferedWriter(Path.of("ECM_factorization.log " + new LargeInteger(job.n)),
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) : null) {
            long t0 = System.nanoTime();
            if (ecmSave && job.ecmPlanIndex == 0 && job.totalCurveCount == 0) {
                String s = new LargeInteger(job.n).toString();
                log.println("Number entering ECM: " + s + '(' + s.length() + " digits)");
            }
            ecmCurveCreation(job, sigmas, works, stats, foundFactor, log);
            ecmStage1(job, sigmas, works, plan, stats, foundFactor, log);
            ecmStage2(job, sigmas, works, plan, stats, foundFactor, log);
            ECM_RUNTIME.samples.computeIfAbsent(new ECMRuntimeKey(effectiveLength(job.n), plan.B1(), plan.B2()),
                            _ -> new ECMRuntimeAggregate())
                    .add(stats.curveCreateNanos, stats.stage1Nanos, stats.stage2Nanos);
            if (ecmSave) {
                log.println("64 curves tried with bounds " + plan.B1() + " and " + plan.B2() + "; "
                        + stats.usableCurves + " being useful");
                log.println("Time for creating all the curves: " + formatSeconds(stats.curveCreateNanos));
                log.println("Stage 1 sieve time: " + formatSeconds(stats.stage1SieveNanos));
                log.println("Stage 1 chain build time: " + formatSeconds(stats.stage1ChainBuildNanos));
                log.println("Stage 1 chain application time: " + formatSeconds(stats.stage1ApplyNanos));
                log.println("Total Stage 1 time: " + formatSeconds(stats.stage1Nanos));
                log.println("Stage 2 block count: " + stats.stage2BlockCount);
                log.println("Stage2State construction: " + formatSeconds(stats.stage2Construction));
                log.println("Stage2State application on blocks: " + formatSeconds(stats.stage2Application));
                log.println("Total stage 2 time: " + formatSeconds(stats.stage2Nanos));
                log.println("Montgomery multiplications: " + stats.montMultiplications);
                log.println("Montgomery squares: " + stats.montSquares);
                long reduceCalls = 0;
                long carryNonzero = 0;
                long propagatedLimbs = 0;
                long canonicalSubtract = 0;
                long canonicalCopy = 0;
                for (ECMCurveWork work : works) {
                    MontgomeryReducer r = work.curve.reducer;
                    reduceCalls += r.reduceCalls;
                    carryNonzero += r.carryNonzero;
                    propagatedLimbs += r.propagatedLimbs;
                    canonicalSubtract += r.canonicalSubtract;
                    canonicalCopy += r.canonicalCopy;
                }
                reduceCalls += job.context.reducer.reduceCalls;
                log.println("Reduction calls: " + reduceCalls);
                carryNonzero += job.context.reducer.carryNonzero;
                log.println("Nonzero carries: " + carryNonzero);
                propagatedLimbs += job.context.reducer.propagatedLimbs;
                log.println("Propagated limbs: " + propagatedLimbs);
                canonicalSubtract += job.context.reducer.canonicalSubtract;
                log.println("Canonical subtractions: " + canonicalSubtract);
                canonicalCopy += job.context.reducer.canonicalCopy;
                log.println("Canonical copies: " + canonicalCopy);
                log.println("Point doubling counts: " + stats.pointDoubleCounts);
                log.println("Point differential additions: " + stats.diffAddCounts);
                log.println("Point doubling + differential addition fusions: " + stats.doubleAndDiffAddCounts);
                log.println("PRAC multiplications: " + stats.multiplyPRACCounts);
                log.println("PRAC case SWAP: " + stats.swapCounts);
                log.println("PRAC case 1: " + stats.case1Counts);
                log.println("PRAC case 2/4: " + stats.case2Or4Counts);
                log.println("PRAC case 3: " + stats.case3Counts);
                log.println("PRAC case 5: " + stats.case5Counts);
                log.println("PRAC case 6: " + stats.case6Counts);
                log.println("PRAC case 7: " + stats.case7Counts);
                log.println("PRAC case 8: " + stats.case8Counts);
                log.println("PRAC case 9: " + stats.case9Counts);
                log.println("Total time for this batch: " + formatSeconds(System.nanoTime() - t0));
                log.println();
                log.flush();
            }
        } catch (FactorFoundException factorFoundException) {return factorFoundException.factor;}
        catch (ExecutionException | InterruptedException | IOException e) {throw new RuntimeException(e);}
        return ONE_MAG;
    }
    private static void ecmCurveCreation(FactorJob task, long[][] sigmas, ECMCurveWork[] works,
                                         ECMStats stats, AtomicReference<long[]> foundFactor, PrintWriter log) {
        long[] n = task.n;
        ECMModulusContext context = task.context();
        context.reducer.resetDiagnostics();
        long t0 = System.nanoTime();
        int curveCount = 0;
        while (curveCount < works.length) {
            long[] sigma = randomMagnitudeBetween(SIX_MAG, n);
            // Normal residues
            long[] u = modSubtract(modMagnitudes(squareMagnitude(sigma), n), FIVE_MAG, n);
            long[] v = modMagnitudes(shiftLeftBits(sigma, 2), n);
            boolean negative = compareArray(v, u) < 0;
            long[] numerator = multiplyMod(cubeMagnitude(
                    negative ? subtractMagnitudes(u, v) : subtractMagnitudes(v, u)),
                    addMagnitudes(v, multiplySmall(u, 3)), n);
            if (negative && !isZeroMagnitude(numerator))
                numerator = subtractMagnitudes(n, numerator);
            GcdInverseMagnitude eg = gcdInverseMagnitudes(
                    multiplyMod(cubeMagnitude(u), shiftLeftBits(v, 2), n), n);
            if (isNontrivialFactor(eg.gcd(), n)) {
                stats.curveCreateNanos = System.nanoTime() - t0;
                if (claimECMFactor(foundFactor, eg.gcd()))
                    stats.found(eg.gcd(), "Curve construction denominator GCD", curveCount + 1,
                            -1, -1, sigmas, log);
                throw new FactorFoundException(eg.gcd());
            }
            if (equalMagnitudes(eg.gcd(), n)) continue;
            MontgomeryCurve curve = new MontgomeryCurve(context.reducer.clone(),
                    multiplyMod(multiplyMagnitudes(eg.inverse(), numerator), context.inv4, n));
            MontgomeryPoint point = new MontgomeryPoint(curve.reducer.cube(context.reducer.toMontgomery(u)),
                    context.reducer.cube(context.reducer.toMontgomery(v)));
            sigmas[curveCount] = sigma;
            works[curveCount++] = new ECMCurveWork(curve, point);
        }
        stats.curveCreateNanos = System.nanoTime() - t0;
    }

    /**
     * A special point object used for curve multiplication. This cannot be treated like
     * an actual record object; the contents of X and Z must be directly accessed and overwritten during
     * scalar multiplication in <code>MontgomeryCurve</code>.
     * @param X The X value that is overwritten
     * @param Z The Z value that is overwritten
     */
    private record MontgomeryPoint(long[] X, long[] Z) {
        void empty() {
            Arrays.fill(X, 0);
            Arrays.fill(Z, 0);
        }
        void copyPointInto(MontgomeryPoint out) {
            System.arraycopy(X, 0, out.X, 0, X.length);
            System.arraycopy(Z, 0, out.Z, 0, Z.length);
        }
    }
    private static final class MontgomeryCurve {
        final long[] A24; // (A + 2)/4 mod n
        final MontgomeryReducer reducer;
        final long[] n;

        // Shared arithmetic scratch.
        final long[] s0;
        final long[] s1;
        final long[] s2;
        final long[] s3;
        final long[] s4;
        final long[] s5;

        //Points shared for reuse in PRAC
        private MontgomeryPoint A;
        private MontgomeryPoint B;
        private MontgomeryPoint C;
        private MontgomeryPoint temp0;
        private MontgomeryPoint temp1;
        private MontgomeryPoint DBd;
        private MontgomeryPoint DBa;
        private void resetPRACScratch() {
            A.empty();
            B.empty();
            C.empty();
            temp0.empty();
            temp1.empty();
            DBd.empty();
            DBa.empty();
        }
        MontgomeryCurve(MontgomeryReducer reducer, long[] A24Normal) {
            this.reducer = reducer;
            A24 = reducer.toMontgomery(A24Normal);
            n = reducer.modulus;

            int k = reducer.k;
            s0 = new long[k];
            s1 = new long[k];
            s2 = new long[k];
            s3 = new long[k];
            s4 = new long[k];
            s5 = new long[k];
            A = newPoint();
            B = newPoint();
            C = newPoint();
            temp0 = newPoint();
            temp1 = newPoint();
            DBd = newPoint();
            DBa = newPoint();
        }
        void requireCanonicalPoint(MontgomeryPoint P) {
            reducer.requireReducedMagnitude(P.X(), "P.X");
            reducer.requireReducedMagnitude(P.Z(), "P.Z");
        }
        MontgomeryPoint newPoint()
        {return new MontgomeryPoint(new long[reducer.k], new long[reducer.k]);}
        /**
         * ------------------------------------------------------------
         * <p>xDBL</p>
         * ------------------------------------------------------------
         */
        MontgomeryPoint doublePoint(MontgomeryPoint P, ECMStats stats) {
            requireCanonicalPoint(P);
            return doublePointCanonical(P, newPoint(), stats);
        }
        MontgomeryPoint doublePointCanonical(MontgomeryPoint P, MontgomeryPoint out, ECMStats stats) {
            // A = X + Z
            reducer.addInto(P.X(), P.Z(), s0);
            // B = X - Z
            reducer.subtractInto(P.X(), P.Z(), s1);
            // AA = A^2
            reducer.squareInto(s0, s0);
            // BB = B^2
            reducer.squareInto(s1, s1);
            // E = AA - BB
            reducer.subtractInto(s0, s1, s2);
            // X2 = AA * BB
            reducer.multiplyInto(s0, s1, out.X);
            // s3 = A24 * E
            reducer.multiplyInto(A24, s2, s3);
            // s3 = BB + A24*E
            reducer.addInto(s1, s3, s3);
            // Z2 = E * (BB + A24*E)
            reducer.multiplyInto(s2, s3, out.Z);
            stats.pointDoubleCounts++;
            // There are 3 Montgomery multiplications here:
            //
            // AA*BB,
            // A24*E,
            // E*(...)
            stats.montMultiplications += 3;
            stats.montSquares += 2;
            return out;
        }
        /**
         * ------------------------------------------------------------
         * <p>Differential addition</p>
         * ------------------------------------------------------------
         */
        MontgomeryPoint diffAdd(MontgomeryPoint P, MontgomeryPoint Q, MontgomeryPoint PMinusQ, ECMStats stats) {
            requireCanonicalPoint(P);
            requireCanonicalPoint(Q);
            requireCanonicalPoint(PMinusQ);
            return diffAddCanonical(P, Q, PMinusQ, newPoint(), stats);
        }
        private MontgomeryPoint diffAddCanonical(MontgomeryPoint P, MontgomeryPoint Q, MontgomeryPoint PMinusQ,
                                      MontgomeryPoint out, ECMStats stats) {
            // A = Xp + Zp
            reducer.addInto(P.X(), P.Z(), s0);

            // B = Xp - Zp
            reducer.subtractInto(P.X(), P.Z(), s1);

            // C = Xq + Zq
            reducer.addInto(Q.X(), Q.Z(), s2);

            // D = Xq - Zq
            reducer.subtractInto(Q.X(), Q.Z(), s3);

            // DA = D*A
            reducer.multiplyInto(s3, s0, s3);

            // CB = C*B
            reducer.multiplyInto(s2, s1, s2);

            // DA + CB
            reducer.addInto(s3, s2, s0);

            // DA - CB
            reducer.subtractInto(s3, s2, s1);

            // (DA + CB)^2
            reducer.squareInto(s0, s0);

            // (DA - CB)^2
            reducer.squareInto(s1, s1);

            // X = Zd * (DA + CB)^2
            reducer.multiplyInto(PMinusQ.Z(), s0, out.X);

            // Z = Xd * (DA - CB)^2
            reducer.multiplyInto(PMinusQ.X(), s1, out.Z);
            stats.diffAddCounts++;
            stats.montMultiplications += 4;
            stats.montSquares += 2;
            return out;
        }
        /**
         * ------------------------------------------------------------
         * <p>Combined xDBL + xADD</p>
         * ------------------------------------------------------------
         */
        private void doubleAndDiffAddCanonical(MontgomeryPoint P, MontgomeryPoint Q, MontgomeryPoint PMinusQ,
                MontgomeryPoint doubled, MontgomeryPoint added, ECMStats stats) {

            // A = Xp + Zp
            reducer.addInto(P.X(), P.Z(), s0);

            // B = Xp - Zp
            reducer.subtractInto(P.X(), P.Z(), s1);

            // C = Xq + Zq
            reducer.addInto(Q.X(), Q.Z(), s2);

            // D = Xq - Zq
            reducer.subtractInto(Q.X(), Q.Z(), s3);

            // DA, CB
            reducer.multiplyInto(s3, s0, s3);
            reducer.multiplyInto(s2, s1, s2);

            // DA+CB, DA-CB
            reducer.addInto(s3, s2, s4);
            reducer.subtractInto(s3, s2, s5);

            // AA, BB
            reducer.squareInto(s0, s0);
            reducer.squareInto(s1, s1);

            // E
            reducer.subtractInto(s0, s1, s2);

            // xDBL
            reducer.multiplyInto(s0, s1, doubled.X);
            reducer.multiplyInto(A24, s2, s3);
            reducer.addInto(s1, s3, s3);
            reducer.multiplyInto(s2, s3, doubled.Z);

            // xADD
            reducer.squareInto(s4, s4);
            reducer.squareInto(s5, s5);
            reducer.multiplyInto(PMinusQ.Z(), s4, added.X);
            reducer.multiplyInto(PMinusQ.X(), s5, added.Z);

            stats.montMultiplications += 7;
            stats.montSquares += 4;
            stats.doubleAndDiffAddCounts++;
        }
        /**
         * ------------------------------------------------------------
         * <p>Scalar multiplication</p>
         * ------------------------------------------------------------
         */
        MontgomeryPoint multiply(MontgomeryPoint P, long scalar, ECMStats stats) {
            requireCanonicalPoint(P);
            int shifts = Long.numberOfTrailingZeros(scalar);
            long oddK = scalar >>> shifts;
            if (oddK == 1L) {
                //Pure power of two.
                MontgomeryPoint result = newPoint();
                P.copyPointInto(result);
                MontgomeryPoint temp = newPoint();
                for (int i = 0; i < shifts; i++) {
                    doublePointCanonical(result, temp, stats);
                    temp.copyPointInto(result);
                }
                return result;
            }
            stats.multiplyPRACCounts++;
            resetPRACScratch();
            doublePointCanonical(P, A, stats);
            P.copyPointInto(B);
            P.copyPointInto(C);
            long center = goldenSplit(oddK);
            long lower = center;
            long split;
            if (lower > oddK >>> 1 && gcdLong(oddK, lower) == 1L)
                split = lower;
            else for (long offset = 1L; ; offset++) {
                lower = center - offset;
                if (lower > oddK >>> 1 && gcdLong(oddK, lower) == 1L) {
                    split = lower;
                    break;
                }
                long upper = center + offset;
                if (upper < oddK && gcdLong(oddK, upper) == 1L) {
                    split = upper;
                    break;
                }
            }
            long d = oddK - split;
            long e = split - d;
            while (d != e) {
                if (d < e) {
                    long temporary = d;
                    d = e;
                    e = temporary;
                    stats.swapCounts++;
                    MontgomeryPoint T = A;
                    A = B;
                    B = T;
                }
                // Apply the first qualifying PRAC condition.
                if (d - e <= e >> 2 && (d + e) % 3L == 0L) {
                    d = (2L * d - e) / 3L;
                    e = (e - d) >> 1;
                    stats.case1Counts++;
                    diffAddCanonical(A, B, C, temp0, stats);
                    diffAddCanonical(temp0, A, B, temp1, stats);
                    diffAddCanonical(B, temp0, A, B, stats);
                    MontgomeryPoint oldA = A;
                    A = temp1;
                    temp1 = oldA;
                } else if (d - e <= e >> 2 && (d - e) % 6L == 0L) {
                    d = (d - e) >> 1;
                    stats.case2Or4Counts++;
                    MontgomeryPoint oldA = A;
                    MontgomeryPoint oldB = B;
                    doubleAndDiffAddCanonical(A, B, C, DBd, DBa, stats);
                    A = DBd;
                    B = DBa;
                    DBd = oldA;
                    DBa = oldB;
                } else if ((d + 3L) >> 2 <= e) {
                    d -= e;
                    stats.case3Counts++;
                    MontgomeryPoint oldB = B;
                    MontgomeryPoint oldC = C;
                    diffAddCanonical(oldB, A, oldC, temp0, stats);
                    B = temp0;
                    C = oldB;
                    temp0 = oldC;
                } else if (((d + e) & 1L) == 0L) {
                    d = (d - e) >> 1;
                    stats.case2Or4Counts++;
                    MontgomeryPoint oldA = A;
                    MontgomeryPoint oldB = B;
                    doubleAndDiffAddCanonical(A, B, C, DBd, DBa, stats);
                    A = DBd;
                    B = DBa;
                    DBd = oldA;
                    DBa = oldB;
                } else if ((d & 1L) == 0L) {
                    d >>= 1;
                    stats.case5Counts++;
                    MontgomeryPoint oldA = A;
                    MontgomeryPoint oldC = C;
                    doubleAndDiffAddCanonical(A, C, B, DBd, DBa, stats);
                    A = DBd;
                    C = DBa;
                    DBd = oldA;
                    DBa = oldC;
                } else if (d % 3L == 0L) {
                    d = d / 3L - e;
                    stats.case6Counts++;
                    MontgomeryPoint oldA = A;
                    MontgomeryPoint oldB = B;
                    MontgomeryPoint oldC = C;
                    doubleAndDiffAddCanonical(oldA, oldB, oldC, DBd, DBa, stats);
                    diffAddCanonical(DBd, DBa, oldC, temp0, stats);
                    diffAddCanonical(DBd, oldA, oldA, temp1, stats);
                    A = temp1;
                    B = temp0;
                    C = oldB;
                    temp1 = oldA;
                    temp0 = oldC;
                } else if ((d + e) % 3L == 0L) {
                    d = (d - 2L * e) / 3L;
                    stats.case7Counts++;
                    MontgomeryPoint oldA = A;
                    MontgomeryPoint oldB = B;
                    doubleAndDiffAddCanonical(oldA, oldB, C, DBd, DBa, stats);
                    diffAddCanonical(DBa, oldA, oldB, temp0, stats);
                    diffAddCanonical(DBd, oldA, oldA, temp1, stats);
                    A = temp1;
                    B = temp0;
                    temp1 = oldA;
                    temp0 = oldB;
                } else if ((d - e) % 3L == 0L) {
                    d = (d - e) / 3L;
                    stats.case8Counts++;
                    MontgomeryPoint oldA = A;
                    MontgomeryPoint oldB = B;
                    MontgomeryPoint oldC = C;
                    doubleAndDiffAddCanonical(oldA, oldB, oldC, DBd, DBa, stats);
                    diffAddCanonical(DBd, oldA, oldA, temp0, stats);
                    diffAddCanonical(oldA, oldC, oldB, temp1, stats);
                    MontgomeryPoint newB = DBa;
                    A = temp0;
                    B = newB;
                    C = temp1;
                    temp0 = oldA;
                    temp1 = oldC;
                    DBa = oldB;
                    // pair.doubled never became live, so it remains scratch.
                } else {
                    e >>= 1;
                    stats.case9Counts++;
                    MontgomeryPoint oldB = B;
                    MontgomeryPoint oldC = C;
                    doubleAndDiffAddCanonical(B, C, A, DBd, DBa, stats);
                    B = DBd;
                    C = DBa;
                    DBd = oldB;
                    DBa = oldC;
                }
                if (d <= 0L || e <= 0L)
                    throw new IllegalStateException("Invalid multiply PRAC reduction: oddK=" + oddK +
                            ", r=" + split + ", d=" + d + ", e=" + e);
            }
            if (d != 1L)
                throw new IllegalStateException("PRAC pair was not coprime");
            MontgomeryPoint out = newPoint();
            diffAddCanonical(A, B, C, out, stats);
            if (shifts == 0) return out;
            MontgomeryPoint originalOut = out;
            MontgomeryPoint temp = newPoint();
            for (int i = 0; i < shifts; i++) {
                doublePointCanonical(out, temp, stats);
                MontgomeryPoint swap = out;
                out = temp;
                temp = swap;
            }
            if (out != originalOut)
                originalOut.copyPointInto(out);
            return out;
        }
        /**
         * ------------------------------------------------------------
         * <p>PRAC</p>
         * ------------------------------------------------------------
         */
        void multiplyPrac(MontgomeryPoint P, byte[] operations, int from, int toExclusive,
                          int trailingDoubling, MontgomeryPoint out, ECMStats stats) {
            stats.multiplyPRACCounts++;
            requireCanonicalPoint(P);
            resetPRACScratch();
            doublePointCanonical(P, A, stats);
            P.copyPointInto(B);
            P.copyPointInto(C);
            for (int index = from; index < toExclusive; index++)
                switch (operations[index]) {
                    case PRAC_SWAP -> {
                        stats.swapCounts++;
                        MontgomeryPoint T = A;
                        A = B;
                        B = T;
                    } case PRAC_CASE_1 -> {
                        stats.case1Counts++;
                        diffAddCanonical(A, B, C, temp0, stats);
                        diffAddCanonical(temp0, A, B, temp1, stats);
                        diffAddCanonical(B, temp0, A, B, stats);
                        MontgomeryPoint oldA = A;
                        A = temp1;
                        temp1 = oldA;
                    } case PRAC_CASE_2, PRAC_CASE_4 -> {
                        stats.case2Or4Counts++;
                        MontgomeryPoint oldA = A;
                        MontgomeryPoint oldB = B;
                        doubleAndDiffAddCanonical(A, B, C, DBd, DBa, stats);
                        A = DBd;
                        B = DBa;
                        DBd = oldA;
                        DBa = oldB;
                    } case PRAC_CASE_3 -> {
                        stats.case3Counts++;
                        MontgomeryPoint oldB = B;
                        MontgomeryPoint oldC = C;
                        diffAddCanonical(oldB, A, oldC, temp0, stats);
                        B = temp0;
                        C = oldB;
                        temp0 = oldC;
                    } case PRAC_CASE_5 -> {
                        stats.case5Counts++;
                        MontgomeryPoint oldA = A;
                        MontgomeryPoint oldC = C;
                        doubleAndDiffAddCanonical(A, C, B, DBd, DBa, stats);
                        A = DBd;
                        C = DBa;
                        DBd = oldA;
                        DBa = oldC;
                    } case PRAC_CASE_6 -> {
                        stats.case6Counts++;
                        MontgomeryPoint oldA = A;
                        MontgomeryPoint oldB = B;
                        MontgomeryPoint oldC = C;
                        doubleAndDiffAddCanonical(oldA, oldB, oldC, DBd, DBa, stats);
                        diffAddCanonical(DBd, DBa, oldC, temp0, stats);
                        diffAddCanonical(DBd, oldA, oldA, temp1, stats);
                        A = temp1;
                        B = temp0;
                        C = oldB;
                        temp1 = oldA;
                        temp0 = oldC;
                    } case PRAC_CASE_7 -> {
                        stats.case7Counts++;
                        MontgomeryPoint oldA = A;
                        MontgomeryPoint oldB = B;
                        doubleAndDiffAddCanonical(oldA, oldB, C, DBd, DBa, stats);
                        diffAddCanonical(DBa, oldA, oldB, temp0, stats);
                        diffAddCanonical(DBd, oldA, oldA, temp1, stats);
                        A = temp1;
                        B = temp0;
                        temp1 = oldA;
                        temp0 = oldB;
                    } case PRAC_CASE_8 -> {
                        stats.case8Counts++;
                        MontgomeryPoint oldA = A;
                        MontgomeryPoint oldB = B;
                        MontgomeryPoint oldC = C;
                        doubleAndDiffAddCanonical(oldA, oldB, oldC, DBd, DBa, stats);
                        diffAddCanonical(DBd, oldA, oldA, temp0, stats);
                        diffAddCanonical(oldA, oldC, oldB, temp1, stats);
                        MontgomeryPoint newB = DBa;
                        A = temp0;
                        B = newB;
                        C = temp1;
                        temp0 = oldA;
                        temp1 = oldC;
                        DBa = oldB;
                        // pair.doubled never became live, so it remains scratch.
                    } case PRAC_CASE_9 -> {
                        stats.case9Counts++;
                        MontgomeryPoint oldB = B;
                        MontgomeryPoint oldC = C;
                        doubleAndDiffAddCanonical(B, C, A, DBd, DBa, stats);
                        B = DBd;
                        C = DBa;
                        DBd = oldB;
                        DBa = oldC;
                    } default -> throw new IllegalStateException("Unknown PRAC operation");
                }
            diffAddCanonical(A, B, C, out, stats);
            if (trailingDoubling == 0) return;
            MontgomeryPoint originalOut = out;
            MontgomeryPoint temp = newPoint();
            for (int i = 0; i < trailingDoubling; i++) {
                doublePointCanonical(out, temp, stats);
                MontgomeryPoint swap = out;
                out = temp;
                temp = swap;
            }
            if (out != originalOut)
                originalOut.copyPointInto(out);
        }
    }
    private static long goldenSplit(long k) {
        if (k <= 0L)
            throw new IllegalArgumentException("k must be positive");
        long numerator = 987L;
        long denominator = 1597L;
        long quotient = k / denominator;
        // round(k * 987 / 1597), without forming k * 987
        return quotient * numerator
                + ((k - denominator * quotient) * numerator + (denominator >>> 1)) / denominator;
    }
    private static final byte PRAC_SWAP = 0;
    private static final byte PRAC_CASE_1 = 1;
    private static final byte PRAC_CASE_2 = 2;
    private static final byte PRAC_CASE_3 = 3;
    private static final byte PRAC_CASE_4 = 4;
    private static final byte PRAC_CASE_5 = 5;
    private static final byte PRAC_CASE_6 = 6;
    private static final byte PRAC_CASE_7 = 7;
    private static final byte PRAC_CASE_8 = 8;
    private static final byte PRAC_CASE_9 = 9;
    private static final class ECMCurveWork {
        final MontgomeryCurve curve;
        MontgomeryPoint point;
        MontgomeryPoint scratch;
        boolean usable = true;

        ECMCurveWork(MontgomeryCurve curve, MontgomeryPoint point) {
            this.curve = curve;
            this.point = point;
            scratch = curve.newPoint();
        }
    }
    private static void ecmStage1(FactorJob job, long[][] sigmas, ECMCurveWork[] works, ECMPlan plan,
                                  ECMStats stats, AtomicReference<long[]> foundFactor, PrintWriter log)
            throws ExecutionException, InterruptedException {
        long t0;
        long t1 = System.nanoTime();
        long lower = 2L;
        long upperExclusive = plan.B1() + 1L;
        while (lower < upperExclusive) {
            if (foundFactor.get() != null) return;
            long upper = upperExclusive - lower <= 1048576 ? upperExclusive : lower + 1048576;
            t0 = System.nanoTime();
            long[] primes = sieveBlock(lower, upper, VERY_SMALL_PRIMES);
            stats.stage1SieveNanos += System.nanoTime() - t0;
            t0 = System.nanoTime();
            ByteArrayOutputStream operations = new ByteArrayOutputStream();

            int[] offsets = new int[primes.length + 1];
            byte[] trailingDoubling = new byte[primes.length];

            int chainCount = 0;
            int powerOfTwoDoubling = 0;

            for (long prime : primes) {
                if (prime > plan.B1()) continue;
                long scalar = largestPrimePowerAtMost(prime, plan.B1());
                int shifts = Long.numberOfTrailingZeros(scalar);
                long oddPart = scalar >>> shifts;
                if (oddPart == 1L) {
                    // This can only be the largest power of 2 <= bound.
                    powerOfTwoDoubling = shifts;
                    continue;
                }
                offsets[chainCount] = operations.size();
                long r = goldenSplit(oddPart);
                while (gcdLong(oddPart, r) != 1L) r--;
                //d and e are SIGNED, ya idiot.
                long d = oddPart - r;
                long e = r - d;
                while (d != e) {
                    if (d < e) {
                        long temporary = d;
                        d = e;
                        e = temporary;
                        operations.write(PRAC_SWAP);
                    }
                    // Apply the first qualifying PRAC condition.
                    if (d - e <= e >> 2 && (d + e) % 3L == 0L) {
                        operations.write(PRAC_CASE_1);
                        d = (2L * d - e) / 3L;
                        e = (e - d) >> 1;
                    } else if (d - e <= e >> 2 && (d - e) % 6L == 0L) {
                        operations.write(PRAC_CASE_2);
                        d = (d - e) >> 1;
                    } else if ((d + 3L) >> 2 <= e) {
                        operations.write(PRAC_CASE_3);
                        d -= e;
                    } else if (((d + e) & 1L) == 0L) {
                        operations.write(PRAC_CASE_4);
                        d = (d - e) >> 1;
                    } else if ((d & 1L) == 0L) {
                        operations.write(PRAC_CASE_5);
                        d >>= 1;
                    } else if (d % 3L == 0L) {
                        operations.write(PRAC_CASE_6);
                        d = d / 3L - e;
                    } else if ((d + e) % 3L == 0L) {
                        operations.write(PRAC_CASE_7);
                        d = (d - 2L * e) / 3L;
                    } else if ((d - e) % 3L == 0L) {
                        operations.write(PRAC_CASE_8);
                        d = (d - e) / 3L;
                    } else {
                        operations.write(PRAC_CASE_9);
                        e >>= 1;
                    }
                    if (d <= 0L || e <= 0L)
                        // Stage 1 chain builder
                        throw new IllegalStateException("Invalid Stage1 PRAC reduction: oddPart=" + oddPart +
                                ", r=" + r + ", d=" + d + ", e=" + e);
                }
                if (d != 1L)
                    throw new IllegalStateException("PRAC pair was not coprime");
                trailingDoubling[chainCount++] = (byte) shifts;
            }
            offsets[chainCount] = operations.size();
            offsets = Arrays.copyOf(offsets, chainCount + 1);
            trailingDoubling = Arrays.copyOf(trailingDoubling, chainCount);
            byte[] ops = operations.toByteArray();
            stats.stage1ChainBuildNanos += System.nanoTime() - t0;
            //apply the Prac block
            applyECMStage1Chains(works, powerOfTwoDoubling, ops, offsets, trailingDoubling, stats, foundFactor);
            lower = upper;
        }
        for (int k = 0; k < works.length; k++) {
            ECMCurveWork state = works[k];
            long[] g = gcdMagnitudes(state.point.Z(), job.n);
            if (isNontrivialFactor(g, job.n)) {
                stats.stage1Nanos = System.nanoTime() - t1;
                if (claimECMFactor(foundFactor, g))
                    stats.found(g, "Stage 1 GCD", job.nextECMCurve + k + 1, 
                            -1, -1, sigmas, log);
                throw new FactorFoundException(g);
            }
            if (equalMagnitudes(g, job.n))
                state.usable = false;
        }
        stats.stage1Nanos += System.nanoTime() - t1;
    }
    private static final int threads = Math.min(4,  Runtime.getRuntime().availableProcessors());
    private static final ExecutorService ECM_EXECUTOR =
            Executors.newFixedThreadPool(threads, Thread.ofPlatform()
                    .daemon().name("ecm-worker-", 0).factory());
    private static void applyECMStage1Chains(ECMCurveWork[] works, int powerOfTwoDoubling, byte[] ops,
                                             int[] offsets, byte[] trailingDoubling, ECMStats stats,
                                             AtomicReference<long[]> foundFactor)
            throws ExecutionException, InterruptedException {
        long t0 = System.nanoTime();
        ArrayList<Future<ECMStats>> futures = new ArrayList<>();
        int chunk = (works.length + threads - 1) / threads;
        for (int worker = 0; worker < threads; worker++) {
            int from = worker * chunk;
            int to = Math.min(works.length, from + chunk);
            if (from >= to)
                break;
            futures.add(ECM_EXECUTOR.submit(() -> {
                ECMStats localStats = new ECMStats();
                for (int k = from; k < to; k++) {
                    if (foundFactor.get() != null)
                        break;
                    ECMCurveWork state = works[k];
                    // powers of two
                    for (int i = 0; i < powerOfTwoDoubling; i++) {
                        state.curve.doublePointCanonical(state.point, state.scratch, localStats);
                        MontgomeryPoint t = state.point;
                        state.point = state.scratch;
                        state.scratch = t;
                    }
                    // PRAC chains
                    for (int chainIndex = 0; chainIndex < trailingDoubling.length; chainIndex++) {
                        if (foundFactor.get() != null) break;
                        int opFrom = offsets[chainIndex];
                        int opTo = offsets[chainIndex + 1];
                        state.curve.multiplyPrac(state.point, ops, opFrom, opTo,
                                trailingDoubling[chainIndex], state.scratch, localStats);
                        MontgomeryPoint t = state.point;
                        state.point = state.scratch;
                        state.scratch = t;
                    }
                }
                return localStats;
            }));
        }
        for (Future<ECMStats> f : futures) {
            ECMStats workerStats = f.get();
            stats.mergeCounters(workerStats);
        }
        stats.stage1ApplyNanos += System.nanoTime() - t0;
    }
    /**
     * Private polynomial class used for Stage 2 evaluation. Assumes every coefficient is in
     * Montgomery form.
     * @param coefficients Lowest degree first: coefficients[i] = coefficient of x<sup>i</sup>
     */
    private record MontgomeryPolynomial(long[][] coefficients, MontgomeryReducer reducer) {
        MontgomeryPolynomial(long[][] coefficients, MontgomeryReducer reducer) {
            this.coefficients = trim(coefficients);
            this.reducer = reducer;
        }
        static MontgomeryPolynomial zero(MontgomeryReducer reducer)
        {return new MontgomeryPolynomial(new long[][]{new long[reducer.k]}, reducer);}
        private static long[][] trim(long[][] c) {
            int n = c.length;
            if (n == 0)
                throw new IllegalArgumentException("Invalid coefficient array");
            while (n > 1 && isZeroMagnitude(c[n - 1])) n--;
            return Arrays.copyOf(c, n);
        }
        int degree() {return coefficients.length-1;}
        /**
         *Reverse the highest 'count' coefficients. For:
         *<p>
         *a0 + a1*x + ... + an*x^n
         *<p>
         *and count = k, returns:
         * <p>
         *an + a(n-1)*x + ... + a(n - k + 1)*x^(k-1)
         */
        private MontgomeryPolynomial reverseHighestCoefficients(int count) {
            if (count < 1 || count > coefficients.length)
                throw new IllegalArgumentException();
            long[][] out = new long[count][];
            int n = coefficients.length - 1;
            for (int i = 0; i < count; i++)
                out[i] = coefficients[n - i];
            return new MontgomeryPolynomial(out, reducer);
        }
        private MontgomeryPolynomial reverseAtLength(int length) {
            if (length < coefficients.length)
                throw new IllegalArgumentException();
            long[][] out = new long[length][];
            for (int i = 0; i < length; i++)
                out[i] = new long[reducer.k];
            for (int i = 0; i < coefficients.length; i++)
                out[length - 1 - i] = coefficients[i];
            return new MontgomeryPolynomial(out, reducer);
        }
        private MontgomeryPolynomial reverse() {
            long[][] out = new long[coefficients.length][];
            for (int i = 0; i < coefficients.length; i++)
                out[coefficients.length-1-i] = coefficients[i];
            return new MontgomeryPolynomial(out, reducer);
        }
        static MontgomeryPolynomial one(MontgomeryReducer reducer)
        {return new MontgomeryPolynomial(new long[][]{reducer.rModM.clone()}, reducer);}
        static MontgomeryPolynomial xMinus(long[] root, MontgomeryReducer reducer)
        {return new MontgomeryPolynomial(new long[][]{reducer.negate(root), reducer.rModM.clone()}, reducer);}
        private MontgomeryPolynomial modXPower(int precision) {
            if (precision < 0)
                throw new IllegalArgumentException("Negative precision");
            if (precision == 0)
                return zero(reducer);
            if (precision >= coefficients.length)
                return this;
            return new MontgomeryPolynomial(Arrays.copyOf(coefficients, precision), reducer);
        }
        private void addShifted(long[][] result, long[][] source, int shift) {
            for (int i = 0; i < source.length; i++) {
                int index = shift + i;
                if (result[index] == null)
                    result[index] = source[i];       // shallow transfer
                else {
                    long[] sum = new long[reducer.k];
                    reducer.addInto(result[index], source[i], sum);
                    result[index] = sum;
                }
            }
        }
        private static long[][] addCoefficients(long[][] coefficients1, long[][] coefficients2,
                                                MontgomeryReducer reducer) {
            int n = Math.max(coefficients1.length, coefficients2.length);
            long[][] out = new long[n][];
            for (int i = 0; i < n; i++) {
                if (i >= coefficients1.length) out[i] = coefficients2[i];
                else if (i >= coefficients2.length) out[i] = coefficients1[i];
                else out[i] = reducer.add(coefficients1[i], coefficients2[i]);
            }
            return trim(out);
        }
        private static long[][] subtractCoefficients(long[][] co1, long[][] co2, MontgomeryReducer reducer) {
            int n = Math.max(co1.length, co2.length);
            long[][] out = new long[n][];
            for (int i = 0; i < n; i++) {
                if (i >= co1.length) out[i] = reducer.negate(co2[i]);
                else if (i >= co2.length) out[i] = co1[i];
                else out[i] = reducer.subtract(co1[i], co2[i]);
            }
            return trim(out);
        }
        MontgomeryPolynomial subtract(MontgomeryPolynomial other) {
            if (reducer != other.reducer)
                throw new IllegalArgumentException("Moduli mismatch");
            int n = Math.max(coefficients.length, other.coefficients.length);
            long[][] out = new long[n][];
            for (int i = 0; i < n; i++) {
                if (i >= coefficients.length) out[i] = reducer.negate(other.coefficients[i]);
                else if (i >= other.coefficients.length) out[i] = coefficients[i];
                else out[i] = reducer.subtract(coefficients[i], other.coefficients[i]);
            }
            return new MontgomeryPolynomial(out, reducer);
        }
        private static final int POLY_KARATSUBA_THRESHOLD = 4;
        MontgomeryPolynomial multiply(MontgomeryPolynomial other, ECMStats stats) {
            if (reducer != other.reducer)
                throw new IllegalArgumentException("Moduli mismatch");
            return new MontgomeryPolynomial(multiplyCoefficients(coefficients, other.coefficients, stats), reducer);
        }
        private long[][] multiplyCoefficients(long[][] a, long[][] b, ECMStats stats) {
            int minDegree = Math.min(a.length - 1, b.length - 1);
            if (minDegree < POLY_KARATSUBA_THRESHOLD) {
                long[][] out = new long[a.length+b.length][reducer.k];
                long[] temp = new long[reducer.k];
                for (int i = 0; i < a.length; i++) {
                    for (int j = 0; j < b.length; j++) {
                        reducer.multiplyInto(a[i], b[j], temp);
                        reducer.addInto(out[i + j], temp, out[i + j]);
                    }
                    stats.montMultiplications += b.length;
                }
                return trim(out);
            }
            int pivot = (Math.max(a.length, b.length) + 1) >>> 1;
            long[][] a0 = getLowerHalf(a, pivot);
            long[][] a1 = getUpperHalf(a, pivot);
            long[][] b0 = getLowerHalf(b, pivot);
            long[][] b1 = getUpperHalf(b, pivot);
            long[][] z0 = multiplyCoefficients(a0, b0, stats);
            long[][] z2 = multiplyCoefficients(a1, b1, stats);
            long[][] z1 = multiplyCoefficients(
                    addCoefficients(a0, a1, reducer), addCoefficients(b0, b1, reducer), stats);
            z1 = subtractCoefficients(z1, z2, reducer);
            z1 = subtractCoefficients(z1, z0, reducer);
            long[][] result = new long[
                    Math.max(z0.length, Math.max(pivot + z1.length, 2 * pivot + z2.length))][];
            // Shallow-copy coefficient references.
            System.arraycopy(z0, 0, result, 0, z0.length);
            addShifted(result, z1, pivot);
            addShifted(result, z2, 2 * pivot);
            return trim(result);
        }
        private MontgomeryPolynomial multiplyModXPower(MontgomeryPolynomial other, int precision, ECMStats stats) {
            if (reducer != other.reducer)
                throw new IllegalArgumentException("Moduli mismatch");
            int resultLength = Math.min(precision, coefficients.length + other.coefficients.length - 1);
            long[][] result = new long[resultLength][];
            for (int i = 0; i < resultLength; i++)
                result[i] = new long[reducer.k];
            long[] temp = new long[reducer.k];
            for (int i = 0; i < coefficients.length && i < resultLength; i++) {
                int maxJ = Math.min(other.coefficients.length, resultLength - i);
                for (int j = 0; j < maxJ; j++) {
                    reducer.multiplyInto(coefficients[i], other.coefficients[j], temp);
                    reducer.addInto(result[i + j], temp, result[i + j]);
                }
                stats.montMultiplications += maxJ;
            }
            return new MontgomeryPolynomial(result, reducer);
        }
        private MontgomeryPolynomial multiplyByTwo() {
            long[][] out = new long[coefficients.length][];
            for (int i = 0; i < out.length; i++)
                out[i] = reducer.add(coefficients[i], coefficients[i]);
            return new MontgomeryPolynomial(out, reducer);
        }
        private static long[][] getLowerHalf(long[][] coefficients, int pivot) {
            int end = Math.min(pivot, coefficients.length);
            return Arrays.copyOfRange(coefficients, 0, end);
        }
        private static long[][] getUpperHalf(long[][] coefficients, int pivot) {
            if (pivot >= coefficients.length)
                return new long[][]{ZERO_MAG};
            return Arrays.copyOfRange(coefficients, pivot, coefficients.length);
        }
        static MontgomeryPolynomial productOfLinearRoots(long[][] roots, MontgomeryReducer reducer, ECMStats stats) {
            ArrayList<MontgomeryPolynomial> level = new ArrayList<>();
            for (long[] root : roots) {
                if (root != null)
                    level.add(xMinus(root, reducer));
            }
            if (level.isEmpty())
                return one(reducer);
            while (level.size() > 1) {
                ArrayList<MontgomeryPolynomial> next = new ArrayList<>((level.size() + 1) >>> 1);
                for (int i = 0; i < level.size(); i += 2) {
                    if (i + 1 < level.size())
                        next.add(level.get(i).multiply(level.get(i + 1), stats));
                    else next.add(level.get(i));
                }
                level = next;
            }
            return level.getFirst();
        }
        private static final int FAST_REMAINDER_THRESHOLD = 128;
        private static MontgomeryPolynomial remainderForNode(MontgomeryPolynomial value, ProductTreeNode node,
                                                             ECMStats stats) {
            if (value.degree() < node.product.degree()) return value;
            int divisorDegree = node.product.degree();
            int quotientLength = value.degree() - divisorDegree + 1;
            MontgomeryPolynomial result;
            if (quotientLength <= FAST_REMAINDER_THRESHOLD || divisorDegree < FAST_REMAINDER_THRESHOLD)
                result = value.remainderMonicSchoolBook(node.product, stats);
            else {
                MontgomeryPolynomial inverse = node.product.reverse().inverseSeries(quotientLength, stats);
                result = value.remainderMonicFast(node.product, inverse, stats);
            }
            return result;
        }
        MontgomeryPolynomial remainderMonicSchoolBook(MontgomeryPolynomial divisor, ECMStats stats) {
            if (reducer != divisor.reducer)
                throw new IllegalArgumentException("Moduli mismatch");
            int d = divisor.degree();
            if (!equalMagnitudes(divisor.coefficients[d], reducer.rModM))
                throw new IllegalArgumentException("Divisor must be monic");
            if (degree() < d) return this;
            long[][] r = coefficients.clone();
            boolean[] owned = new boolean[r.length];
            long[] temp = new long[reducer.k];
            /*
             * Low-degree-first coefficients.
             * Eliminate x^i from the highest degree downward.
             */
            for (int i = r.length - 1; i >= d; i--) {
                long[] lead = r[i];
                if (isZeroMagnitude(lead)) continue;
                int shift = i - d;

                for (int j = 0; j < d; j++) {
                    int p = shift + j;
                    reducer.multiplyInto(lead, divisor.coefficients[j], temp);
                    if (!owned[p]) {
                        r[p] = r[p].clone();
                        owned[p] = true;
                    }
                    reducer.subtractInto(r[p], temp, r[p]);
                }
                stats.montMultiplications += d;
            }
            long[][] result = new long[d][];
            System.arraycopy(r, 0, result, 0, d);
            return new MontgomeryPolynomial(result, reducer);
        }
        /**
         * Fast remainder of this polynomial by a monic divisor.
         * <p>
         * reversedDivisorInverse must satisfy
         * <p>
         * reverse(divisor) * reversedDivisorInverse == 1 (mod x^k)
         * <p>
         * for at least the required precision k = degree(this) - degree(divisor) + 1.
         */
        private MontgomeryPolynomial remainderMonicFast(MontgomeryPolynomial divisor,
                                                        MontgomeryPolynomial reversedDivisorInverse, ECMStats stats) {
            if (reducer != divisor.reducer ||
                    reducer != reversedDivisorInverse.reducer)
                throw new IllegalArgumentException("Moduli mismatch");
            int m = divisor.degree();
            if (!equalMagnitudes(divisor.coefficients[m], reducer.rModM))
                throw new IllegalArgumentException("Divisor must be monic");
            int n = degree();
            if (n < m) return this;
            int k = degree() - divisor.degree() + 1;
            MontgomeryPolynomial aRev = reverseHighestCoefficients(k);
            MontgomeryPolynomial qRev = aRev.multiplyModXPower(reversedDivisorInverse, k, stats);
            MontgomeryPolynomial quotient = qRev.reverseAtLength(k);
            /*
             * remainder = A - Q*B.
             *
             * We only care about degrees < m.
             */
            MontgomeryPolynomial qb = quotient.multiplyModXPower(divisor, m, stats);
            long[][] out = new long[m][];
            for (int i = 0; i < m; i++) {
                long[] a = coefficients[i];
                long[] b = i < qb.coefficients.length ? qb.coefficients[i] : new long[reducer.k];
                out[i] = reducer.subtract(a, b);
            }
            return new MontgomeryPolynomial(out, reducer);
        }
        MontgomeryPolynomial inverseSeries(int precision, ECMStats stats) {
            if (!equalMagnitudes(coefficients[0], reducer.rModM))
                throw new IllegalArgumentException("Constant coefficient must be Montgomery 1");
            MontgomeryPolynomial g = one(reducer);
            int p = 1;
            while (p < precision) {
                int next = Math.min(p << 1, precision);
                MontgomeryPolynomial fg = multiplyModXPower(g, next, stats);
                MontgomeryPolynomial correction = one(reducer).multiplyByTwo().subtract(fg).modXPower(next);
                g = g.multiplyModXPower(correction, next, stats);
                p = next;
            }
            return g.modXPower(precision);
        }
        long[] evaluate(long[] x, ECMStats stats) {
            long[] out = new long[reducer.k];
            evaluateInto(x, out, stats);
            return out;
        }
        void evaluateInto(long[] x, long[] out, ECMStats stats) {
            int d = degree();
            if (d == 0) {
                Arrays.fill(out, 0L);
                System.arraycopy(coefficients[0], 0, out, 0,
                        Math.min(reducer.k, coefficients[0].length));
                return;
            }
            reducer.addInto(x, coefficients[d - 1], out);
            for (int i = d - 2; i >= 0; i--) {
                reducer.multiplyInto(out, x, out);
                reducer.addInto(out, coefficients[i], out);
            }
            stats.montMultiplications += d - 1;
        }
        long[][] evaluateAtBatchAffineX(long[][] affineGiantX, int count, ECMStats stats) {
            if (count < 0 || count > affineGiantX.length)
                throw new IllegalArgumentException("Invalid count");
            if (count == 0)
                return new long[0][];
            if (count == 1)
                return new long[][]{evaluate(affineGiantX[0], stats)};
            ProductTreeNode root = buildProductTree(affineGiantX, 0, count, stats);
            MontgomeryPolynomial rootValue = degree() < root.product.degree() ? this
                    : remainderForNode(this, root, stats);
            long[][] out = new long[count][];
            evaluateRemainderTree(root, rootValue, out, stats);
            return out;
        }
        private record ProductTreeNode(MontgomeryPolynomial product, ProductTreeNode left, ProductTreeNode right,
                                       int leafIndex, long[] evaluationPoint) {
            boolean isLeaf() {return left == null;}}
        private ProductTreeNode buildProductTree(long[][] xs, int from, int to, ECMStats stats) {
            if (to - from == 1)
                return new ProductTreeNode(xMinus(xs[from], reducer), null, null, from, xs[from]);
            int mid = (from + to) >>> 1;
            ProductTreeNode left = buildProductTree(xs, from, mid, stats);
            ProductTreeNode right = buildProductTree(xs, mid, to, stats);
            MontgomeryPolynomial product = left.product.multiply(right.product, stats);
            return new ProductTreeNode(product, left, right, -1, null);
        }
        private static void evaluateRemainderTree(ProductTreeNode node, MontgomeryPolynomial value,
                                                  long[][] out, ECMStats stats) {
            if (node.isLeaf()) {
                out[node.leafIndex] = value.evaluate(node.evaluationPoint, stats);
                return;
            }
            MontgomeryPolynomial leftValue = remainderForNode(value, node.left, stats);
            evaluateRemainderTree(node.left, leftValue, out, stats);
            MontgomeryPolynomial rightValue = remainderForNode(value, node.right, stats);
            evaluateRemainderTree(node.right, rightValue, out, stats);
        }
    }
    private static final class Stage2State {
        final MontgomeryCurve curve;
        final long[][] affineBabyX;
        final MontgomeryPoint difference;
        final MontgomeryPolynomial polynomial;
        final int k;
        MontgomeryPoint previous;
        MontgomeryPoint current;
        long currentM;

        // State local to the current sieve block.
        long[] blockProduct;
        int blockTermCount;

        // Snapshot needed only if gcd(blockProduct, n) == n.
        MontgomeryPoint blockStartPrevious;
        MontgomeryPoint blockStartCurrent;
        long blockStartM;
        private static final int[] STAGE2_SLOTS;
        static {
            STAGE2_SLOTS = new int[240];
            int index = 0;
            for (int r = 1; r < HALF_D; r++)
                if (gcdLong(r, D) == 1)
                    STAGE2_SLOTS[index++] = r-1;
        }
        Stage2State(MontgomeryCurve curve, MontgomeryPoint Q, long firstStage2Prime, ECMStats stats,
                    int k, long[][] sigmas, AtomicReference<long[]> foundFactor, PrintWriter log) {
            long t0 = System.nanoTime();
            this.curve = curve;
            this.k = k;
            /*
              Baby steps:

              baby[r - 1] = [r]Q

              for the r values needed by the wheel.
             */
            affineBabyX = new long[HALF_D][];
            long[][] babyX = new long[HALF_D][];
            long[][] babyZ = new long[HALF_D][];
            // r = 1
            babyX[0] = Q.X();
            babyZ[0] = Q.Z();
            MontgomeryPoint previous = Q;                    // [1]Q
            MontgomeryPoint current = curve.doublePoint(Q, stats); // [2]Q
            for (int r = 2; r < HALF_D; r++) {
                if ((r & 63) == 0) checkECMBatch(foundFactor);
                if (gcdLong(r, D) == 1) {
                    babyX[r - 1] = current.X();
                    babyZ[r - 1] = current.Z();
                }
                if (r + 1 < HALF_D) {
                    MontgomeryPoint nextMultiple = curve.diffAdd(current, Q, previous, stats);
                    previous = current;
                    current = nextMultiple;
                }
            }
            int count = STAGE2_SLOTS.length;
            long[][] xs = new long[count][];
            long[][] zs = new long[count][];
            for (int j = 0; j < count; j++) {
                int slot = STAGE2_SLOTS[j];
                xs[j] = babyX[slot];
                zs[j] = babyZ[slot];
            }
            long[][] prefix = new long[count + 1][];
            prefix[0] = curve.reducer.rModM;
            for (int i = 0; i < count; i++) {
                if ((i & 63) == 0) checkECMBatch(foundFactor);
                prefix[i + 1] = curve.reducer.multiply(prefix[i], zs[i]);
            }
            stats.montMultiplications += count;
            GcdInverseMagnitude GI = gcdInverseMagnitudes(curve.reducer.fromMontgomery(prefix[count]), curve.n);
            if (isNontrivialFactor(GI.gcd(), curve.n)) {
                stats.stage2Construction += System.nanoTime() - t0;
                if (claimECMFactor(foundFactor, GI.gcd()))
                    stats.foundFromStage2(GI.gcd(), "Stage 2 baby initialization", k, sigmas, log);
                throw new FactorFoundException(GI.gcd());
            }
            if (equalMagnitudes(GI.gcd(), curve.n)) {
                for (long[] z: zs) {
                    long[] g = gcdMagnitudes(curve.reducer.fromMontgomery(z), curve.n);
                    if (isNontrivialFactor(g, curve.n)) {
                        stats.stage2Construction += System.nanoTime() - t0;
                        if (claimECMFactor(foundFactor, g))
                            stats.foundFromStage2(g, "Stage 2 baby initialization", k, sigmas, log);
                        throw new FactorFoundException(g);
                    }
                }
                throw new UnusableECMCurveException("Curve " + k + " is degenerate");
            }
            long[] inverseProduct = curve.reducer.toMontgomery(GI.inverse());
            for (int i = count - 1; i >= 0; i--) {
                if ((i & 63) == 0) checkECMBatch(foundFactor);
                // x_i = X_i / Z_i
                affineBabyX[STAGE2_SLOTS[i]] =
                        curve.reducer.multiply(xs[i], curve.reducer.multiply(inverseProduct, prefix[i]));
                /*
                 Remove Z_i from the inverse product:
                 (Z_0 ... Z_i)^-1 * Z_i = (Z_0 ... Z_{i-1})^-1
                */
                curve.reducer.multiplyInto(inverseProduct, zs[i], inverseProduct);
                stats.montMultiplications += 3;
            }
            checkECMBatch(foundFactor);
            polynomial = MontgomeryPolynomial.productOfLinearRoots(affineBabyX, curve.reducer, stats);
            checkECMBatch(foundFactor);
            difference = curve.multiply(Q, D, stats);
            currentM = nearestCenterIndex(firstStage2Prime);
            if (currentM == 1L) {
                this.previous = null;
                this.current = difference;
            } else {
                this.previous = curve.multiply(difference, currentM - 1L, stats);
                this.current = curve.multiply(difference, currentM, stats);
            }
            blockProduct = curve.reducer.rModM.clone();
            stats.stage2Construction += System.nanoTime() - t0;
        }
        void doStage2(long B1, long B2, ECMStats stats, long[][] sigmas, AtomicReference<long[]> foundFactor,
                      PrintWriter log) {
            long start = System.nanoTime();
            long firstM = nearestCenterIndex(B1 + 1);
            long lastM = nearestCenterIndex(B2);
            while (currentM < firstM) {
                checkECMBatch(foundFactor);
                advanceGiant(stats);
            }
            int capacity = (int) Math.min(16384, lastM - firstM + 1);
            long[][] xs = new long[capacity][];
            long[][] zs = new long[capacity][];
            long[] evaluated = new long[curve.reducer.k];
            long m = firstM;
            while (m <= lastM) {
                checkECMBatch(foundFactor);
                int count = 0;
                while (count < capacity && m <= lastM) {
                    while (currentM < m)
                        advanceGiant(stats);
                    // current = [mD]Q
                    xs[count] = current.X();
                    zs[count++] = current.Z();
                    m++;
                }
                //Polynomial evaluation needs a few batched points
                long[][] prefix = new long[count + 1][];
                prefix[0] = curve.reducer.rModM;
                for (int i = 0; i < count; i++) {
                    if ((i & 63) == 0) checkECMBatch(foundFactor);
                    prefix[i + 1] = curve.reducer.multiply(prefix[i], zs[i]);
                }
                stats.montMultiplications += count;
                GcdInverseMagnitude GI = gcdInverseMagnitudes(curve.reducer.fromMontgomery(prefix[count]), curve.n);
                if (isNontrivialFactor(GI.gcd(), curve.n)) {
                    if (claimECMFactor(foundFactor, GI.gcd()))
                        stats.foundFromStage2(GI.gcd(), "Stage 2 giant normalization", k, sigmas, log);
                    throw new FactorFoundException(GI.gcd());
                }
                if (equalMagnitudes(GI.gcd(), curve.n))
                    throw new UnusableECMCurveException("Curve " + k + " is degenerate");
                long[] inverseProduct = curve.reducer.toMontgomery(GI.inverse());
                long[][] affineGiantX = new long[count][];
                for (int i = count - 1; i >= 0; i--) {
                    affineGiantX[i] = curve.reducer.multiply(xs[i], curve.reducer.multiply(inverseProduct, prefix[i]));
                    curve.reducer.multiplyInto(inverseProduct, zs[i], inverseProduct);
                }
                stats.montMultiplications += 3L * count;
                //Points are set; evaluate them
                for (long[] X: affineGiantX) {
                    polynomial.evaluateInto(X, evaluated, stats);
                    curve.reducer.multiplyInto(blockProduct, evaluated, blockProduct);
                }
                checkECMBatch(foundFactor);
                stats.montMultiplications += count;
                blockTermCount += count;
            }
            stats.stage2Application += System.nanoTime() - start;
        }
        private void advanceGiant(ECMStats stats) {
            MontgomeryPoint next =
                    currentM == 1L ? curve.doublePoint(difference, stats)
                    : curve.diffAdd(current, difference, previous, stats);
            previous = current;
            current = next;
            currentM++;
        }
        void clearStage2Block() {
            blockProduct = curve.reducer.rModM.clone();
            blockTermCount = 0;
            blockStartPrevious = null;
            blockStartCurrent = null;
            blockStartM = 0L;
        }
    }
    private record Stage2ConstructionResult(Stage2State state, ECMStats stats) {}
    private static void ecmStage2(FactorJob job, long[][] sigmas, ECMCurveWork[] works, ECMPlan plan,
                                  ECMStats stats, AtomicReference<long[]> foundFactor, PrintWriter log)
            throws ExecutionException, InterruptedException {
        long t2 = System.nanoTime();

        long lower = plan.B1() + 1L;
        long upperExclusive = plan.B2() + 1L;

        long firstStage2Prime = -1L;
        long searchLower = lower;

        while (searchLower < upperExclusive) {
            long searchUpper = upperExclusive - searchLower <= 1_048_576L ? upperExclusive
                    : searchLower + 1_048_576L;
            long[] primes = sieveBlock(searchLower, searchUpper, SMALL_PRIMES);
            if (primes.length != 0) {
                firstStage2Prime = primes[0];
                break;
            }
            searchLower = searchUpper;
        }
        if (firstStage2Prime < 0L) return;
        Stage2State[] stage2States = new Stage2State[works.length];

        @SuppressWarnings("unchecked")
        Future<Stage2ConstructionResult>[] futures = new Future[works.length];
        long constructionStart = System.nanoTime();
        for (int k = 0; k < works.length; k++) {
            ECMCurveWork work = works[k];
            if (!work.usable) continue;
            final int curveIndex = k;
            final long finalFirstStage2Prime = firstStage2Prime;
            futures[k] = ECM_EXECUTOR.submit(() -> {
                checkECMBatch(foundFactor);
                ECMStats localStats = new ECMStats();
                Stage2State state = new Stage2State(work.curve, work.point, finalFirstStage2Prime,
                        localStats, curveIndex, sigmas, foundFactor, log);
                return new Stage2ConstructionResult(state, localStats);
            });
        }
        for (int k = 0; k < works.length; k++) {
            // Always drain ALL submitted tasks; otherwise workers from the old
            // modulus may keep running while the next factor job starts.
            Future<Stage2ConstructionResult> future = futures[k];
            if (future == null) continue;
            try {
                Stage2ConstructionResult result = future.get();
                stage2States[k] = result.state();
                stats.mergeCounters(result.stats());
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                switch (cause) {
                    case FactorFoundException f -> {
                        foundFactor.compareAndSet(null, f.factor);
                        stage2States[k] = null;
                    }
                    case ECMBatchCancelledException _ -> stage2States[k] = null;
                    case UnusableECMCurveException _ -> {
                        works[k].usable = false;
                        stage2States[k] = null;
                    }
                    case null, default -> throw new RuntimeException(cause);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        stats.stage2Construction += System.nanoTime() - constructionStart;
        if (foundFactor.get() != null)
            throw new FactorFoundException(foundFactor.get());
        @SuppressWarnings("unchecked")
        Future<Stage2CurveResult>[] newFutures = new Future[works.length];
        long doStage2 = System.nanoTime();
        for (int k = 0; k < stage2States.length; k++) {
            Stage2State state = stage2States[k];
            if (state == null) continue;
            final int curveIndex = k;
            newFutures[k] = ECM_EXECUTOR.submit(() -> runStage2Curve(state, lower, upperExclusive, curveIndex,
                    sigmas, job, foundFactor, log));
        }
        for (Future<Stage2CurveResult> future : newFutures) {
            if (future == null) continue;
            try {
                Stage2CurveResult result = future.get();
                if (result.usable()) stats.usableCurves++;
                stats.mergeCounters(result.stats());
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                switch (cause) {
                    case FactorFoundException f -> foundFactor.compareAndSet(null, f.factor);
                    case ECMBatchCancelledException _ -> {}
                    case RuntimeException r -> throw r;
                    case Error error -> throw error;
                    case null, default -> throw new RuntimeException(cause);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        if (foundFactor.get() != null)
            throw new FactorFoundException(foundFactor.get());
        stats.stage2Application += System.nanoTime() - doStage2;
        stats.stage2Nanos = System.nanoTime() - t2;
    }
    private record Stage2CurveResult(ECMStats stats, boolean usable) {}
    private static Stage2CurveResult runStage2Curve(Stage2State s2, long lower, long upperExclusive,
                                                    int curveIndex, long[][] sigmas, FactorJob job,
                                                    AtomicReference<long[]> foundFactor, PrintWriter log) {
        ECMStats local = new ECMStats();
        long blockLower = lower;
        boolean usable = false;
        while (blockLower < upperExclusive) {
            if (foundFactor.get() != null || Thread.currentThread().isInterrupted())
                return new Stage2CurveResult(local, false);
            long blockUpper =
                    upperExclusive - blockLower <= 1_048_576L ? upperExclusive : blockLower + 1_048_576L;
            long localB1 = blockLower - 1L;
            long localB2 = blockUpper - 1L;
            local.stage2BlockCount++;
            s2.blockStartPrevious = s2.previous;
            s2.blockStartCurrent = s2.current;
            s2.blockStartM = s2.currentM;
            try {
                s2.doStage2(localB1, localB2, local, sigmas, foundFactor, log);
                long[] g = gcdMagnitudes(s2.curve.reducer.fromMontgomery(s2.blockProduct), s2.curve.n);
                if (isNontrivialFactor(g, s2.curve.n)) {
                    s2.clearStage2Block();
                    local.stage2Nanos = local.totalPlanNanos();
                    if (claimECMFactor(foundFactor, g))
                        local.found(g, "Stage 2 GCD", job.nextECMCurve + curveIndex + 1,
                                blockLower, blockUpper, sigmas, log);
                    throw new FactorFoundException(g);
                }
                if (equalMagnitudes(g, s2.curve.n)) {
                    s2.previous = s2.blockStartPrevious;
                    s2.current = s2.blockStartCurrent;
                    s2.currentM = s2.blockStartM;
                    s2.blockProduct = s2.curve.reducer.rModM.clone();
                    s2.blockTermCount = 0;
                    long firstM = nearestCenterIndex(localB1 + 1);
                    long lastM = nearestCenterIndex(localB2);
                    while (s2.currentM < firstM) {
                        checkECMBatch(foundFactor);
                        s2.advanceGiant(local);
                    }
                    for (long m = firstM; m <= lastM; m++) {
                        checkECMBatch(foundFactor);
                        while (s2.currentM < m) s2.advanceGiant(local);
                        /*
                         * Replay can afford an individual inversion because
                         * this branch should be extremely rare.
                         */
                        GcdInverseMagnitude GI = gcdInverseMagnitudes(
                                s2.curve.reducer.fromMontgomery(s2.current.Z()), s2.curve.n);
                        if (isNontrivialFactor(GI.gcd(), s2.curve.n)) {
                            local.stage2Nanos = local.totalPlanNanos();
                            if (claimECMFactor(foundFactor, GI.gcd()))
                                local.found(GI.gcd(), "Stage 2 final GCD",
                                        job.nextECMCurve + curveIndex + 1, blockLower, blockUpper, sigmas, log);
                            throw new FactorFoundException(GI.gcd());
                        }
                        if (equalMagnitudes(GI.gcd(), s2.curve.n)) continue;
                        long[] x = s2.curve.reducer.multiply(s2.current.X(),
                                s2.curve.reducer.toMontgomery(GI.inverse()));
                        long[] term = s2.polynomial.evaluate(x, local);
                        g = gcdMagnitudes(s2.curve.reducer.fromMontgomery(term), s2.curve.n);
                        if (isNontrivialFactor(g, s2.curve.n)) {
                            local.stage2Nanos = local.totalPlanNanos();
                            if (claimECMFactor(foundFactor, g))
                                local.found(g, "Stage 2 giant replay", 
                                        job.nextECMCurve + curveIndex + 1, 
                                        blockLower, blockUpper, sigmas, log);
                            throw new FactorFoundException(g);
                        }
                    }
                }
            } catch (UnusableECMCurveException | ECMBatchCancelledException e) {
                s2.clearStage2Block();
                return new Stage2CurveResult(local, false);
            }
            s2.clearStage2Block();
            if (blockLower == lower) usable = true;
            blockLower = blockUpper;
        }
        return new Stage2CurveResult(local, usable);
    }
    private static final class ECMStats {
        final long planStartNanos;
        int usableCurves;
        long curveCreateNanos;

        long pointDoubleCounts = 0;
        long diffAddCounts = 0;
        long doubleAndDiffAddCounts = 0;
        long multiplyPRACCounts = 0;
        long swapCounts;
        long case1Counts;
        long case2Or4Counts;
        long case3Counts;
        long case5Counts;
        long case6Counts;
        long case7Counts;
        long case8Counts;
        long case9Counts;

        long stage1Nanos = 0;
        long stage1SieveNanos = 0;
        long stage1ChainBuildNanos = 0;
        long stage1ApplyNanos = 0;

        long stage2Nanos = 0;
        long stage2Construction;
        long stage2BlockCount = 0;
        long stage2Application = 0;
        long montMultiplications = 0;
        long montSquares = 0;
        ECMStats() {planStartNanos = System.nanoTime();}
        void foundFromStage2(long[] factor, String location, int curve, long[][] sigmas, PrintWriter log) {
            if (!ecmSave) return;
            log.println("ECM FACTOR FOUND: " + new LargeInteger(factor));
            log.println("Location of factor: " + location);
            log.println("Curve where factor was found: " + curve);
            log.println("Curve sigmas: [");
            for (int index = 0; index < sigmas.length; index++) {
                log.print(sigmas[index] == null ? "(not generated)"
                        : new LargeInteger(sigmas[index]));
                if (index < sigmas.length - 1)
                    log.println(", ");
            }
            log.println(']');
        }
        void found(long[] factor, String location, int curve, long blockLower, long blockUpper, long[][] sigmas,
                   PrintWriter log) {
            if (!ecmSave) return;
            log.println("ECM FACTOR FOUND: " + new LargeInteger(factor));
            log.println("Location of factor: " + location);
            log.println("Curve where factor was found: " + curve);
            if (blockLower != -1)
                log.println("Factor found in prime block between " + blockLower + " and " + blockUpper);
            log.print("Curve sigmas: [");
            for (int index = 0; index < sigmas.length; index++) {
                log.print(sigmas[index] == null ? "(not generated)"
                        : new LargeInteger(sigmas[index]));
                if (index < sigmas.length-1)
                    log.println(", ");
            }
            log.println(']');
        }
        long totalPlanNanos() {return System.nanoTime() - planStartNanos;}
        void mergeCounters(ECMStats src) {
            montMultiplications += src.montMultiplications;
            montSquares += src.montSquares;

            pointDoubleCounts += src.pointDoubleCounts;
            diffAddCounts += src.diffAddCounts;
            doubleAndDiffAddCounts += src.doubleAndDiffAddCounts;

            multiplyPRACCounts += src.multiplyPRACCounts;

            swapCounts += src.swapCounts;
            case1Counts += src.case1Counts;
            case2Or4Counts += src.case2Or4Counts;
            case3Counts += src.case3Counts;
            case5Counts += src.case5Counts;
            case6Counts += src.case6Counts;
            case7Counts += src.case7Counts;
            case8Counts += src.case8Counts;
            case9Counts += src.case9Counts;

            stage2BlockCount += src.stage2BlockCount;
        }
    }
    private record ECMPlan(long B1, long B2, int curves) {}
    private static final ECMPlan[] ECM_EXTREME_PLANS = {
            new ECMPlan(2_000L, 200_000L, 64),
            new ECMPlan(11_000L, 1_100_000L, 128),
            new ECMPlan(50_000L, 5_000_000L, 384),
            new ECMPlan(250_000L, 25_000_000L, 768),
            new ECMPlan(1_000_000L, 100_000_000L, 2048),
            new ECMPlan(3_000_000L, 300_000_000L, 6144),
            new ECMPlan(11_000_000L, 1_100_000_000L, 12288),
            new ECMPlan(43_000_000L, 4_300_000_000L, 32768),
            new ECMPlan(110_000_000, 11_000_000_000L, 65536),
            new ECMPlan(260_000_000, 26_000_000_000L, 131072),
            new ECMPlan(850_000_000, 85_000_000_000L, 393216),
            new ECMPlan(3_000_000_000L, 300_000_000_000L, 786432)
    };
    private static final int[] ECM_FACTOR_DIGITS = {15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70};
    private static int planForFactorDigits(int factorDigits) {
        for (int i = 0; i < ECM_FACTOR_DIGITS.length; i++)
            if (factorDigits <= ECM_FACTOR_DIGITS[i])
                return i;
        return ECM_FACTOR_DIGITS.length - 1;
    }
    private record ECMRuntimeKey(int limbCount, long B1, long B2) {}
    private static final class ECMRuntimeAggregate {
        long curveCreationNanos;
        long stage1Nanos;
        long stage2Nanos;
        long totalCurves;
        void add(long curveCreationNanos, long stage1Nanos, long stage2Nanos) {
            totalCurves += 64;
            this.curveCreationNanos += curveCreationNanos;
            this.stage1Nanos += stage1Nanos;
            this.stage2Nanos += stage2Nanos;
        }
    }
    private static double estimatePrimesUpTo(long bound) {return bound/StrictMath.log(bound);}
    private static final class ECMRuntimeModel {
        private final HashMap<ECMRuntimeKey, ECMRuntimeAggregate> samples = new HashMap<>();
        synchronized double estimateSecondsThrough(FactorJob task, int targetPlan) {
            if (targetPlan < task.ecmPlanIndex)
                targetPlan = task.ecmPlanIndex;
            double weightSum = 0.0;
            double creation = 0.0;
            double stage1 = 0.0;
            double stage2 = 0.0;
            for (var entry : samples.entrySet()) {
                ECMRuntimeKey key = entry.getKey();
                ECMRuntimeAggregate agg = entry.getValue();
                if (agg.totalCurves == 0) continue;
                double distance = Math.abs(key.limbCount() - task.n.length);
                double weight = agg.totalCurves / (1.0 + 2.0*distance + distance*distance);
                creation += weight * agg.curveCreationNanos / (double) agg.totalCurves;
                stage1 += weight * agg.stage1Nanos / (double) agg.totalCurves / key.B1();
                stage2 += weight * agg.stage2Nanos / (double) agg.totalCurves
                        / (estimatePrimesUpTo(key.B2()) - estimatePrimesUpTo(key.B1()));
                weightSum += weight;
            }
            if (weightSum == 0) return Double.POSITIVE_INFINITY;
            creation /= weightSum;
            stage1 /= weightSum;
            stage2 /= weightSum;
            double total = 0.0;
            for (int i = task.ecmPlanIndex; i <= targetPlan && i < ECM_EXTREME_PLANS.length; i++) {
                ECMPlan plan = ECM_EXTREME_PLANS[i];
                int curves = i == task.ecmPlanIndex ? plan.curves() - task.nextECMCurve : plan.curves();
                double nanosPerCurve = creation + stage1 * plan.B1()
                        + stage2 * (estimatePrimesUpTo(plan.B2) - estimatePrimesUpTo(plan.B1()));
                if (!Double.isFinite(nanosPerCurve) || nanosPerCurve < 0.0) return Double.POSITIVE_INFINITY;
                total += nanosPerCurve * curves / 1e9;
            }
            return total;
        }
    }
    private static final ECMRuntimeModel ECM_RUNTIME = new ECMRuntimeModel();

    //--- Self-Initializing Quadratic Sieve(scroll up please) ---//

    private static final boolean ENABLE_DOUBLE_PARTIALS = false;
    private static final boolean qsSave = true;
    private static final double LOG_SCALE = 64.0;
    private static final class QSFactorBase {
        // Permanent data
        final int size;
        final int[] primes;
        final int[] logPrimes;
        final int columnCount;
        final int globalPMax;
        final long pMaxSquared;
        final double logB;
        final long singleLargePrimeBound;
        final long doubleLargePrimeBound;
        final double logLP;
        final long[] doubleLargePrimeBoundSquared;
        final double filterAllowance;
        final long[] reciprocals;
        final int aPrimeCount;
        final int[] root1;
        final int[] root2;

        // Reusable QS scratch
        final int[] aIndex;
        final short[] aCount;
        final long[][] twoTerms;
        final int[] invAModP;
        final int[] inactive;
        final int[] pos1;
        final int[] pos2;
        final short[] dense;
        final int[] used;
        final int[] smallDivResult = new int[3];
        final int[][] grayDelta;
        // interval-sized scratch
        int[] hitHead = new int[0];
        int[] hitIndices = new int[0];
        int[] hitPrimes = new int[0];
        int[] candidateIndices = new int[0];
        int[] score = new int[0];

        static QSFactorBase buildFactorBase(long[] n, int bound) {
            int[] candidates = sieve(bound);
            int[] primes = new int[candidates.length];
            int[] logPrimes = new int[candidates.length];
            int[] root1 = new int[candidates.length];
            int[] root2 = new int[candidates.length];
            long[] reciprocals = new long[candidates.length];
            int out = 0;
            for (int p : candidates) {
                long reciprocal = Long.divideUnsigned(-1L, p);
                int nModP = modSmallInt(n, n.length, p, reciprocal);
                if (nModP == 0) throw new FactorFoundException(new long[]{p});
                if (p == 2) {
                    primes[out] = 2;
                    logPrimes[out] = (int) Math.round(StrictMath.log(2.0) * LOG_SCALE);
                    // Since N is odd after trial division, N ≡ 1 mod 2.
                    root1[out] = 1;
                    root2[out] = 1;
                    reciprocals[out++] = reciprocal;
                    continue;
                }
                if (legendreSymbol(nModP, p, reciprocal) == 1) {
                    int logRound = (int) Math.round(StrictMath.log(p) * LOG_SCALE);
                    if ((p & 3) == 3) {
                        int r = powModInt(nModP, (p + 1) >>> 2, p, reciprocal);
                        primes[out] = p;
                        logPrimes[out] = logRound;
                        root1[out] = r;
                        root2[out] = p - r;
                        reciprocals[out++] = reciprocal;
                        continue;
                    }
                    if ((p & 7) == 5) {
                        int v = getV(p, nModP, reciprocal);
                        primes[out] = p;
                        logPrimes[out] = logRound;
                        root1[out] = v;
                        root2[out] = p - v;
                        reciprocals[out++] = reciprocal;
                        continue;
                    }
                    // Full Tonelli-Shanks
                    long q = p - 1;
                    int s = 0;
                    while ((q & 1) == 0) {
                        s++;
                        q >>>= 1;
                    }
                    int z = 2;
                    while (legendreSymbol(z, p, reciprocal) != -1) z++;
                    int c = powModInt(z, q, p, reciprocal);
                    int r = powModInt(nModP, (q + 1) >>> 1, p, reciprocal);
                    int t = powModInt(nModP, q, p, reciprocal);
                    int m = s;
                    while (t != 1) {
                        int i = 1;
                        long temp = ((long) t * t) % p;
                        while (temp != 1) {
                            temp = (temp * temp) % p;
                            i++;
                            if (i == m)
                                throw new ArithmeticException("Tonelli-Shanks failed");
                        }
                        long b = c;
                        for (int j = 0; j < m - i - 1; j++) b = (b * b) % p;
                        r = (int) ((r * b) % p);
                        t = (int) ((t * b % p) * b % p);
                        c = (int) ((b * b) % p);
                        m = i;
                    }
                    primes[out] = p;
                    logPrimes[out] = logRound;
                    root1[out] = r;
                    root2[out] = p - r;
                    reciprocals[out++] = reciprocal;
                }
            }
            return new QSFactorBase(primes, logPrimes, root1, root2, reciprocals, out);
        }
        private static int getV(int p, int nModP, long reciprocal) {
            int v = powModInt(nModP, (p + 3) >>> 3, p, reciprocal);
            long vv = (long) v * v;
            int check = (int) (vv - Math.multiplyHigh(vv, reciprocal) * p);
            if (check >= p) check -= p;
            if (check != nModP) {
                long product = (long) v * powModInt(2, (p - 1) >>> 2, p, reciprocal);
                v = (int) (product - Math.multiplyHigh(product, reciprocal) * p);
                if (v >= p) v -= p;
            }
            return v;
        }
        private QSFactorBase(int[] primes, int[] logPrimes, int[] root1, int[] root2, long[] reciprocals, int size) {
            this.size = size;
            this.primes = Arrays.copyOf(primes, size);
            this.reciprocals = Arrays.copyOf(reciprocals, size);
            this.logPrimes = Arrays.copyOf(logPrimes, size);
            this.root1 = Arrays.copyOf(root1, size);
            this.root2 = Arrays.copyOf(root2, size);
            columnCount = size + 1; // negative column + real factor-base primes
            globalPMax = primes[size - 1];
            logB = StrictMath.log(globalPMax);
            singleLargePrimeBound = 100L * globalPMax;
            logLP = StrictMath.log(singleLargePrimeBound);
            doubleLargePrimeBound = 50L * globalPMax;
            double singleAllowance = StrictMath.log(singleLargePrimeBound) * 31.0 / 32.0;
            filterAllowance = ENABLE_DOUBLE_PARTIALS
                    ? Math.max(singleAllowance, StrictMath.log(doubleLargePrimeBound) * 31.0 / 16.0)
                    : singleAllowance;
            long lpLow = singleLargePrimeBound * singleLargePrimeBound;
            long lpHigh = Math.unsignedMultiplyHigh(singleLargePrimeBound, singleLargePrimeBound);
            doubleLargePrimeBoundSquared = lpHigh != 0 ? new long[]{lpLow, lpHigh} : new long[]{lpLow};
            pMaxSquared = (long) globalPMax * globalPMax;
            int aFactorCount = 1;
            int bPolys = 1;
            while (bPolys < size / bPolys) {
                aFactorCount++;
                bPolys <<= 1;
            }
            aPrimeCount = Math.clamp(aFactorCount, 4, 8);
            inactive = new int[aPrimeCount + 1];
            inactive[0] = 2;
            aIndex = new int[aPrimeCount];
            aCount = new short[aPrimeCount];
            twoTerms = new long[aPrimeCount][];
            invAModP = new int[size];
            pos1 = new int[size];
            pos2 = new int[size];
            dense = new short[columnCount];
            used = new int[columnCount];
            grayDelta = new int[aPrimeCount - 1][size];
        }
        void ensureSieveCapacity(int interval, int largeHitCapacity) {
            if (score.length < interval) {
                score = new int[interval];
                hitHead = new int[interval];
                candidateIndices = new int[interval];
            }
            if (hitIndices.length < largeHitCapacity) {
                hitIndices = new int[largeHitCapacity];
                hitPrimes = new int[largeHitCapacity];
            }
        }
        int[] buildFactorBaseIndexMap() {
            int[] index = new int[globalPMax + 1];
            Arrays.fill(index, -1);
            for (int i = 0; i < size; i++)
                index[primes[i]] = i;
            return index;
        }
    }
    private static int legendreSymbol(long a, int p, long reciprocal) {
        a %= p;
        if (a < 0) a += p;
        if (a == 0) return 0;
        long r = powModInt(a, (p - 1) >>> 1, p, reciprocal);
        if (r == 1) return 1;
        if (r == p - 1) return -1;
        throw new ArithmeticException("Invalid Legendre result");
    }
    private static int mergedExponentLength(
            int[] aIndex, short[] aCount,
            int[] bIndex, short[] bCount) {
        int i = 0, j = 0, n = 0;
        while (i < aIndex.length && j < bIndex.length) {
            int ai = aIndex[i];
            int bj = bIndex[j];
            if (ai < bj) {
                i++;
                n++;
            } else if (bj < ai) {
                j++;
                n++;
            } else {
                if (aCount[i] + bCount[j] != 0) n++;
                i++;
                j++;
            }
        }
        return n + (aIndex.length - i) + (bIndex.length - j);
    }
    private static void mergeExponents(
            int[] aIndex, short[] aCount,
            int[] bIndex, short[] bCount,
            int[] outIndex, short[] outCount) {
        int i = 0, j = 0, k = 0;
        while (i < aIndex.length && j < bIndex.length) {
            int ai = aIndex[i];
            int bj = bIndex[j];
            if (ai < bj) {
                outIndex[k] = ai;
                outCount[k++] = aCount[i++];
            } else if (bj < ai) {
                outIndex[k] = bj;
                outCount[k++] = bCount[j++];
            } else {
                int sum = aCount[i] + bCount[j];
                if (sum != 0) {
                    outIndex[k] = ai;
                    outCount[k++] = (short) sum;
                }
                i++;
                j++;
            }
        }
        while (i < aIndex.length) {
            outIndex[k] = aIndex[i];
            outCount[k++] = aCount[i++];
        }
        while (j < bIndex.length) {
            outIndex[k] = bIndex[j];
            outCount[k++] = bCount[j++];
        }
    }
    private static final class QSFullRelationPool {
        long[][][] xValues;
        int[][] exponentIndex;
        short[][] exponentCount;
        long[][][] squareFactors;
        int size;

        QSFullRelationPool(int initialCapacity) {
            xValues = new long[initialCapacity][][];
            exponentIndex = new int[initialCapacity][];
            exponentCount = new short[initialCapacity][];
            squareFactors = new long[initialCapacity][][];
        }
        void add(long[][] x, int[] expIndex, short[] expCount, long[][] squares) {
            int id = size++;
            xValues[id] = x;
            exponentIndex[id] = expIndex;
            exponentCount[id] = expCount;
            squareFactors[id] = squares;
        }
        private void addMerged(long[][] xValues, int[] aIndex, short[] aCount, int[] bIndex, short[] bCount,
                               long[][] squareFactors) {
            int len = mergedExponentLength(aIndex, aCount, bIndex, bCount);
            int[] index = new int[len];
            short[] count = new short[len];
            mergeExponents(aIndex, aCount, bIndex, bCount, index, count);
            add(xValues, index, count, squareFactors);
        }
    }
    private static final long PARTIAL_ROOT = 0L;
    private static final class QSPartialRelationPool {
        long[][][] xValues;
        int[][] expIdx;
        short[][] expCount;
        long[] vertex1;
        long[] vertex2;
        int size;
        QSPartialRelationPool(int initialCapacity) {
            int cap = Math.max(1, initialCapacity);
            xValues = new long[cap][][];
            expIdx = new int[cap][];
            expCount = new short[cap][];
            vertex1 = new long[cap];
            vertex2 = new long[cap];
        }
        int add(long[][] xs, int[] indexes, short[] counts, long v1, long v2) {
            if (xs == null || xs.length == 0)
                throw new IllegalArgumentException("Relation must contain an X value");
            if (indexes == null || counts == null)
                throw new NullPointerException();
            if (indexes.length != counts.length)
                throw new IllegalArgumentException("Exponent index/count lengths differ");
            if (v1 == PARTIAL_ROOT && v2 == PARTIAL_ROOT)
                throw new IllegalArgumentException("A partial relation must contain at least one large prime");
            if (v1 != PARTIAL_ROOT && Long.compareUnsigned(v1, 1L) <= 0)
                throw new IllegalArgumentException("Large-prime vertices must be greater than one");
            if (v2 != PARTIAL_ROOT && Long.compareUnsigned(v2, 1L) <= 0)
                throw new IllegalArgumentException("Large-prime vertices must be greater than one");
            /*
             * Canonicalize endpoints:
             * single: ROOT first
             * pair: unsigned ascending order
             */
            long a;
            long b;
            if (v1 == PARTIAL_ROOT) {
                a = PARTIAL_ROOT;
                b = v2;
            } else if (v2 == PARTIAL_ROOT) {
                a = PARTIAL_ROOT;
                b = v1;
            } else if (Long.compareUnsigned(v1, v2) <= 0) {
                a = v1;
                b = v2;
            } else {
                a = v2;
                b = v1;
            }
            ensureCapacity(size*10);
            int id = size++;
            xValues[id] = xs;
            expIdx[id] = indexes;
            expCount[id] = counts;
            vertex1[id] = a;
            vertex2[id] = b;
            return id;
        }
        long otherVertex(int id, long vertex) {
            long a = vertex1[id];
            long b = vertex2[id];
            if (vertex == a) return b;
            if (vertex == b) return a;
            throw new IllegalArgumentException("Vertex is not an endpoint of partial relation");
        }
        private void ensureCapacity(int required) {
            if (required <= vertex1.length) return;
            int newCapacity = Math.max(required, vertex1.length << 1);
            xValues = Arrays.copyOf(xValues, newCapacity);
            expIdx = Arrays.copyOf(expIdx, newCapacity);
            expCount = Arrays.copyOf(expCount, newCapacity);
            vertex1 = Arrays.copyOf(vertex1, newCapacity);
            vertex2 = Arrays.copyOf(vertex2, newCapacity);
        }
    }
    private static final class PartialGraph {
        private static final int NONE = -1;
        final HashMap<Long, Integer> vertexIds = new HashMap<>();
        int[] treeParent = new int[1024];
        int[] treeEdge   = new int[1024];
        int[] ufParent = new int[1024];
        int[] ufSize   = new int[1024];
        int[] mark = new int[1024];
        int markStamp = 0;
        /*
         * Ephemeral path buffers. Valid until the next path search.
         */
        int[] path = new int[64];
        int[] reverse = new int[64];
        int vertexCount;
        PartialGraph() {
            Arrays.fill(treeParent, NONE);
            Arrays.fill(treeEdge, NONE);
        }
        private int vertexId(long vertex) {
            Integer existing = vertexIds.get(vertex);
            if (existing != null) return existing;
            int id = vertexCount++;
            ensureVertexCapacity(vertexCount);
            vertexIds.put(vertex, id);
            treeParent[id] = NONE;
            treeEdge[id] = NONE;
            ufParent[id] = id;
            ufSize[id] = 1;
            return id;
        }
        private int ufFind(int x) {
            int root = x;
            while (ufParent[root] != root) root = ufParent[root];
            while (ufParent[x] != x) {
                int next = ufParent[x];
                ufParent[x] = root;
                x = next;
            }
            return root;
        }
        /*
         * Reverse the actual tree-parent chain so v becomes the tree root.
         */
        private void reroot(int v) {
            int previous = NONE;
            int previousEdge = NONE;
            int current = v;
            while (current != NONE) {
                int next = treeParent[current];
                int nextEdge = treeEdge[current];
                treeParent[current] = previous;
                treeEdge[current] = previousEdge;
                previous = current;
                previousEdge = nextEdge;
                current = next;
            }
        }
        /*
         * Must only be called when a and b are currently disconnected.
         * Reroot the smaller component at its endpoint and attach it to
         * the larger component. This keeps rerooting amortized.
         */
        void addForestEdge(long a, long b, int partialId) {
            int va = vertexId(a);
            int vb = vertexId(b);
            int rootA = ufFind(va);
            int rootB = ufFind(vb);
            if (rootA == rootB)
                throw new IllegalStateException("Attempted to add cycle edge to partial forest");
            if (ufSize[rootA] <= ufSize[rootB]) {
                reroot(va);
                treeParent[va] = vb;
                treeEdge[va] = partialId;
                ufParent[rootA] = rootB;
                ufSize[rootB] += ufSize[rootA];
            } else {
                reroot(vb);

                treeParent[vb] = va;
                treeEdge[vb] = partialId;

                ufParent[rootB] = rootA;
                ufSize[rootA] += ufSize[rootB];
            }
        }
        /*
         * Returns:
         *   -1 : vertices are disconnected
         *  >=0 : number of relation IDs in path[]
         * No allocation after buffers have reached sufficient capacity.
         */
        int findPartialPath(long start, long target) {
            int s = vertexId(start);
            int t = vertexId(target);

            if (ufFind(s) != ufFind(t)) return -1;
            if (s == t) return 0;
            int stamp = nextStamp();
            /*
             * Mark ancestors of start.
             */
            for (int v = s; v != NONE; v = treeParent[v])
                mark[v] = stamp;
            /*
             * Walk target upward until the paths meet.
             */
            int lca = t;

            while (mark[lca] != stamp)
                lca = treeParent[lca];

            int length = 0;

            /*
             * start -> LCA is already in correct order.
             */
            for (int v = s; v != lca; v = treeParent[v]) {
                ensurePathCapacity(length*10);
                path[length++] = treeEdge[v];
            }

            /*
             * target -> LCA is backwards.
             */
            int reverseLength = 0;

            for (int v = t; v != lca; v = treeParent[v]) {
                ensureReverseCapacity(reverseLength*10);
                reverse[reverseLength++] = treeEdge[v];
            }
            ensurePathCapacity(length + reverseLength);
            while (reverseLength != 0)
                path[length++] = reverse[--reverseLength];
            return length;
        }
        private int nextStamp() {
            if (++markStamp == 0) {
                Arrays.fill(mark, 0);
                markStamp = 1;
            }
            return markStamp;
        }
        private void ensureVertexCapacity(int needed) {
            if (needed <= treeParent.length) return;
            int oldLength = treeParent.length;
            int n = Math.max(needed, oldLength << 1);
            treeParent = Arrays.copyOf(treeParent, n);
            treeEdge = Arrays.copyOf(treeEdge, n);
            ufParent = Arrays.copyOf(ufParent, n);
            ufSize = Arrays.copyOf(ufSize, n);
            mark = Arrays.copyOf(mark, n);
            Arrays.fill(treeParent, oldLength, n, NONE);
            Arrays.fill(treeEdge, oldLength, n, NONE);
        }
        private void ensurePathCapacity(int needed) {
            if (needed > path.length)
                path = Arrays.copyOf(path, Math.max(needed, path.length << 1));
        }
        private void ensureReverseCapacity(int needed) {
            if (needed > reverse.length)
                reverse = Arrays.copyOf(reverse, Math.max(needed, reverse.length << 1));
        }
    }
    private static int inverseModSmall(int a, int p) {
        if (a >= p)
            throw new AssertionError("Specialized inverseModSmall precondition a < p violated");
        int oldR = p;
        int oldS = 0;
        int s = 1;
        while (a != 0) {
            int q = oldR / a;
            int tmpR = oldR - q * a;
            oldR = a;
            a = tmpR;
            int tmpS = oldS - q * s;
            oldS = s;
            s = tmpS;
        }
        if (oldR != 1) throw new ArithmeticException("No inverse mod " + p);
        return Math.floorMod(oldS, p);
    }
    private static int maxFactorBaseIndexAtMost(QSFactorBase factorBase, long[] maxNextPrimeMag) {
        if (isZeroMagnitude(maxNextPrimeMag)) return -1;
        int last = factorBase.size - 1;
        if (maxNextPrimeMag.length > 1) return last;
        long max = maxNextPrimeMag[0];
        if (Long.compareUnsigned(max, factorBase.primes[last]) >= 0) return last;
        int maxIdx = Arrays.binarySearch(factorBase.primes, (int)max);
        if (maxIdx < 0) maxIdx = -maxIdx-2;
        return maxIdx;
    }
    private record ACoefficient(long[] A, int[] factors, long half) {}
    private record ACandidateResult(ACoefficient coefficient, String failure)
    { static ACandidateResult fail(String reason) {return new ACandidateResult(null, reason);}}
    private static ACoefficient chooseAByScoring(long[] sqrt2N, QSFactorBase factorBase, SplittableRandom rng,
                                                 PrintWriter log) {
        ACoefficient best = null;
        double bestYield = -1.0;
        ACoefficient fallback = null;
        long fallbackHalf = 0L;
        int attempts = 2048;
        int genFail = 0;
        int zeroHalf = 0;
        int badYield = 0;
        int accepted = 0;
        int halfOverInt = 0;
        int halfTooSmall = 0;
        int halfTooLarge = 0;
        String firstGenFailure = null;
        long targetHalf = 16L*factorBase.size/(1L << factorBase.aPrimeCount);
        long lower = Math.max(1L, targetHalf >>> 2);
        long upper = Math.multiplyExact(targetHalf, 4L);
        double logTargetA = lnMagnitude(sqrt2N) - StrictMath.log(targetHalf);
        if (!Double.isFinite(logTargetA) || logTargetA <= 0.0)
            throw new IllegalStateException("Bad SIQS log target A");
        for (int trial = 0; trial < attempts; trial++) {
            ACandidateResult result = randomSquarefreeAResult(sqrt2N, factorBase, logTargetA, rng);
            if (result.coefficient() == null) {
                genFail++;
                if (trial == 0)
                    firstGenFailure = result.failure();
                continue;
            }
            ACoefficient candidate = result.coefficient();
            long half = candidate.half();
            if (half == 0) {
                zeroHalf++;
                continue;
            }
            if (Long.compareUnsigned(half, Integer.MAX_VALUE) > 0) {
                halfOverInt++;
                continue;
            }
            if (Long.compareUnsigned(half, lower) < 0) {
                halfTooSmall++;
                continue;
            }
            if (Long.compareUnsigned(half, upper) > 0) {
                halfTooLarge++;
                continue;
            }
            // Keep a fallback before applying soft quality filters.
            if (fallback == null || Long.compareUnsigned(half, fallbackHalf) > 0) {
                fallback = candidate;
                fallbackHalf = half;
            }
            double yield = 2.0 * half * roughDickman((lnMagnitude(candidate.A()) + 2.0 * StrictMath.log(half))
                    / StrictMath.log(factorBase.globalPMax));
            if (!Double.isFinite(yield) || yield <= 0.0) {
                badYield++;
                continue;
            }
            accepted++;
            if (yield > bestYield) {
                bestYield = yield;
                best = candidate;
            }
        }
        if (best != null) return best;
        /*
         * Important:
         * If every candidate failed only because of the soft positions/yield filter,
         * still return a fallback so you can print stats and learn what happened.
         */
        if (fallback != null) {
            if (qsSave)
                log.println(
                        "SIQS A warning: using fallback A because strict scoring rejected all candidates. aFactorCount=" + factorBase.aPrimeCount
                                + " attempts=" + attempts + " genFail=" + genFail + " zeroHalf=" + zeroHalf
                                + " halfOverInt" + halfOverInt + " halfTooLarge" + halfTooLarge
                                + "halfTooSmall" + halfTooSmall + " badYield=" + badYield + " accepted=" + accepted +
                                " fallbackHalf=" + Long.toUnsignedString(fallbackHalf));
            return fallback;
        }
        throw new IllegalStateException("Could not find usable SIQS A: aFactorCount=" +
                factorBase.aPrimeCount + " attempts=" + attempts + " genFail=" + genFail + " zeroHalf="
                + zeroHalf + " halfOverInt=" + halfOverInt + " halfTooLarge=" + halfTooLarge + " halfTooSmall="
                + halfTooSmall + " badYield=" + badYield + " accepted=" + accepted + " firstGenFailure=["
                + firstGenFailure + "]");
    }
    private static ACandidateResult randomSquarefreeAResult(long[] sqrt2N, QSFactorBase factorBase,
                                                            double logTargetA, SplittableRandom rng) {
        int[] chosen = new int[factorBase.aPrimeCount];
        int chosenCount = 0;
        long[] A = new long[factorBase.aPrimeCount + 1];
        A[0] = 1L;
        // floor(sqrt2N / A), initially A = 1.
        long[] half = sqrt2N;
        double logA = 0.0;
        while (chosenCount < factorBase.aPrimeCount) {
            int remaining = factorBase.aPrimeCount - chosenCount;
            double remainingLogBudget = logTargetA - logA;
            if (remainingLogBudget <= 0.0) return ACandidateResult.fail("A already above target");
            double desiredLogP = remainingLogBudget / remaining;
            // half is already floor(sqrt2N / A).
            int maxIdx = maxFactorBaseIndexAtMost(factorBase, half);
            if (maxIdx < 1) return ACandidateResult.fail("maxIdx < 1");
            int idx = chooseUnusedPrimeNearLogBounded(factorBase, chosen, chosenCount, desiredLogP, maxIdx, rng);
            if (idx < 0) return ACandidateResult.fail("no unused prime");
            int p = factorBase.primes[idx];
            multiplySmallInto(A, p, A);
            // Exact identity:
            // floor(sqrt2N / (oldA * p))
            // = floor(floor(sqrt2N / oldA) / p)
            half = divideByLong(half, p).quotient();
            boolean secondRoot = rng.nextBoolean();
            chosen[chosenCount++] = (idx << 1) | (secondRoot ? 1 : 0);
            logA += factorBase.logPrimes[idx] / LOG_SCALE;
        }
        if (half.length > 1) return ACandidateResult.fail("half too large");
        return new ACandidateResult(new ACoefficient(trimLeadingZerosTrusted(A), chosen, half[0]), null);
    }
    private static int chooseUnusedPrimeNearLogBounded(QSFactorBase factorBase, int[] chosen, int chosenCount,
                                                       double desiredLogP, int maxIdx, SplittableRandom rng) {
        if (maxIdx < 1) return -1;
        double desiredPDouble = Math.exp(desiredLogP);
        if (!Double.isFinite(desiredPDouble))
            desiredPDouble = factorBase.primes[maxIdx];
        int desiredP = (int)Math.clamp(desiredPDouble, 3, factorBase.primes[maxIdx]);
        int lo = 1;
        int hi = maxIdx;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (factorBase.primes[mid] < desiredP) lo = mid + 1;
            else hi = mid - 1;
        }
        int center = Math.clamp(lo, 1, maxIdx);
        int radius = Math.max(8, maxIdx / 64);
        for (int tries = 0; tries < 32; tries++) {
            int offset = rng.nextInt(-radius, radius + 1);
            int idx = center + offset;
            if (idx >= 1 && idx <= maxIdx && factorBase.primes[idx] > 2 &&
                    doesntContainAFactorIndex(chosen, chosenCount, idx))
                return idx;
        }
        // nearest unused prime
        for (int d = 0; center - d >= 1 || center + d <= maxIdx; d++) {
            int a = center - d;
            if (a >= 1 && factorBase.primes[a] > 2 && doesntContainAFactorIndex(chosen, chosenCount, a))
                return a;
            int b = center + d;
            if (b <= maxIdx && factorBase.primes[b] > 2 && doesntContainAFactorIndex(chosen, chosenCount, b))
                return b;
        }
        return -1;
    }
    private static boolean doesntContainAFactorIndex(int[] chosen, int chosenCount, int index) {
        for (int i = 0; i < chosenCount; i++) {
            if ((chosen[i] >>> 1) == index)
                return false;
        }
        return true;
    }
    private static double roughDickman(double u) {
        return switch (u) {
            case double v when v < 0.0 -> 0.0;
            case double v when v <= 1.0 -> 1.0;
            case double v when v <= 2.0 -> 1.0 - StrictMath.log(u);
            // Crude but useful ranking approximation.
            default -> StrictMath.exp(-u * (StrictMath.log(u) + StrictMath.log(StrictMath.log(u)) - 1.0));
        };
    }
    private static final class AStats {
        final int aFactorCount;
        final long bCount;
        final long half;
        final long interval;
        final long positionsPerA;
        final int fbSize;
        final int pMax;
        final long largePrimeBound;

        final double logA;
        final double logQEdge;
        final double uB;
        final double uLP;

        long precomputation;
        long smallDivideTime;
        long relationClassifyNs;
        long scanTime;
        long hitLinkNs;
        long candidateScanNs;
        long factorNs;

        RelationYieldEstimate initialEstimate;

        long polys;
        long scanned;
        long candidates;

        long full;
        long singlePartialStored;
        long doublePartialStored;
        long singlePartialMerged;
        long doublePartialMerged;
        long mixedPartialMerged;
        long none;

        long rootCalcTime;
        long sieveNs;
        long attemptNs;
        long polyNs;

        AStats(double logA, long half, QSFactorBase factorBase, RelationYieldEstimate estimate) {
            aFactorCount = factorBase.aPrimeCount;
            bCount = 1L << (aFactorCount - 1);
            this.half = half;
            interval = 2L * half;
            positionsPerA = Math.multiplyExact(interval, bCount);

            fbSize = factorBase.size;
            pMax = factorBase.globalPMax;
            largePrimeBound = factorBase.singleLargePrimeBound;

            this.logA = logA;
            logQEdge = logA + 2.0 * StrictMath.log(half);

            uB = logQEdge / StrictMath.log(pMax);

            uLP = logQEdge / StrictMath.log(largePrimeBound);
            this.initialEstimate = estimate;
        }
        void observeRelationAdd(RelationAddKind kind) {
            candidates++;
            switch (kind) {
                case NONE -> none++;
                case FULL -> full++;
                case SINGLE_PRIME_PARTIAL -> singlePartialStored++;
                case DOUBLE_PRIME_PARTIAL -> doublePartialStored++;
                case SINGLE_PARTIAL_CYCLE -> singlePartialMerged++;
                case DOUBLE_PARTIAL_CYCLE -> doublePartialMerged++;
                case MIXED_PARTIAL_CYCLE ->  mixedPartialMerged++;
            }
        }
        long smallDivisions;
        long factorTestsTotal;
        long factorTestsMin = Long.MAX_VALUE;
        long factorTestsMax;
        void observeFactorTests(long tested) {
            smallDivisions++;
            factorTestsTotal += tested;
            factorTestsMin = Math.min(factorTestsMin, tested);
            factorTestsMax = Math.max(factorTestsMax, tested);
        }
        long mergedRows() {return singlePartialMerged+doublePartialMerged+mixedPartialMerged;}
        long usefulRows() {return full + mergedRows();}
        private RelationYieldEstimate adaptiveEstimate() {
            if (polys == 0L) return initialEstimate;
            double priorWeight = 8.0;
            double observedWeight = polys;
            double fullHitsPerPoly = weightedAverage(initialEstimate.fullPerPoly(), priorWeight,
                    measuredFullPerPoly(), observedWeight);
            double singleHitsPerPoly = weightedAverage(initialEstimate.singlePartialHitsPerPoly(), priorWeight,
                    measuredSinglePartialPerPoly(), observedWeight);
            double doubleHitsPerPoly = weightedAverage(initialEstimate.doublePartialHitsPerPoly(), priorWeight,
                    measuredDoublePartialPerPoly(), observedWeight);
            double singleMergesPerPoly = weightedAverage(initialEstimate.singlePartialMergesPerPoly(), priorWeight,
                    singlePartialMerged / (double) polys, observedWeight);
            double doubleMergesPerPoly = weightedAverage(initialEstimate.doublePartialMergesPerPoly(), priorWeight,
                    (doublePartialMerged + mixedPartialMerged) / (double) polys, observedWeight);
            return RelationYieldEstimate.of(fullHitsPerPoly, singleHitsPerPoly, singleMergesPerPoly,
                    doubleHitsPerPoly, doubleMergesPerPoly);
        }
        double measuredFullPerPoly() {return polys == 0L ? 0.0 : full / (double) polys;}
        double measuredSinglePartialPerPoly() {return polys == 0L ? 0.0 : singlePartialStored / (double) polys;}
        double measuredDoublePartialPerPoly() {return polys == 0L ? 0.0 : doublePartialStored / (double) polys;}
        double measuredPartialMergesPerPoly() {return polys == 0L ? 0.0 : mergedRows()/ (double) polys;}
        double measuredUsefulRowsPerPoly() {return polys == 0L ? 0.0 : usefulRows()/ (double) polys;}
        double measuredSecondsPerSieve() {
            long sum = collectionNs();
            return sum == 0L ? Double.NaN : sum / 1e9 / polys;
        }
        long collectionNs() {return precomputation+rootCalcTime+polyNs+sieveNs+attemptNs;}
        double empiricalSecondsRemaining(long requiredRelations, long currentRelations) {
            long remaining = Math.max(0L, requiredRelations - currentRelations);
            RelationYieldEstimate adaptive = adaptiveEstimate();
            if (adaptive.usefulRowsPerPoly <= 0.0) return Double.POSITIVE_INFINITY;
            double secPerPoly = measuredSecondsPerSieve();
            if (!Double.isFinite(secPerPoly)) return Double.POSITIVE_INFINITY;
            return remaining / adaptive.usefulRowsPerPoly() * secPerPoly;
        }
    }
    private static String formatSeconds(long nanoSeconds) {return nanoSeconds / 1e9 + " seconds";}
    /*
    These two values are guesses; do not treat them as Holy Grails of truth
     */
    private static final double fullCalibration = 0.06;
    private static final double partialCalibration = 0.02;
    private static void sieve128Polynomials(long[] N, long[] sqrt2N, QSFactorBase factorBase, int[] fbIndex,
                                            QSFullRelationPool relations, QSPartialRelationPool partials,
                                            PartialGraph partialGraph, SplittableRandom rng,
                                            int requiredRelations, PrintWriter log, boolean save) {
        long precomputation = System.nanoTime();
        ACoefficient AR = chooseAByScoring(sqrt2N, factorBase, rng, log);
        long[] A = AR.A();
        Arrays.sort(AR.factors());
        int half = (int)AR.half();
        int startT = -half;
        int interval = 2 * half;
        double logA = lnMagnitude(A);
        double logQ = logA + 2.0 * StrictMath.log(half);
        double fullProbability = roughDickman(logQ / factorBase.logB);
        double rawAcceptedPartialPool = interval *
                Math.max(0.0, roughDickman(logQ / factorBase.logLP) - fullProbability);
        double fullHits = fullCalibration * interval * fullProbability;
        double calibratedPartialPool = partialCalibration * rawAcceptedPartialPool;
        /*
         * Wider LP ranges make accepted two-prime residuals more likely.
         * Scale the span against the typical logarithmic Q magnitude.
         */
        double normalizedSpan = (factorBase.logLP - factorBase.logB) / Math.max(logQ - factorBase.logB, 1e-12);
        normalizedSpan = Math.clamp(1.0 - normalizedSpan, 0.10, 0.90);
        double singlePartialHits = calibratedPartialPool * normalizedSpan;
        double doublePartialHits = calibratedPartialPool * (1.0 - normalizedSpan);
        AStats plan = new AStats(logA, half, factorBase, RelationYieldEstimate.of(fullHits, singlePartialHits,
                0.0, doublePartialHits, 0.0));
        // Precompute values depending only on A.
        int largeStart = Arrays.binarySearch(factorBase.primes, interval);
        if (largeStart < 0) largeStart = -largeStart - 1;
        factorBase.ensureSieveCapacity(interval, 2 * (factorBase.size - largeStart));
        BResult BR = computeB(AR, factorBase, largeStart);
        long[] B = BR.B();
        long rootCalcTime = System.nanoTime();
        computeInitialRoots(factorBase, B, startT);
        plan.rootCalcTime = System.nanoTime() - rootCalcTime;
        precomputation += plan.rootCalcTime;
        for (int bit = 0; bit < factorBase.aPrimeCount-1; bit++) {
            long[] step = factorBase.twoTerms[bit + 1];
            int stepLength = step.length;
            int[] deltaRow = factorBase.grayDelta[bit];
            int lo = 0;
            for (int hi : factorBase.inactive) {
                for (int j = lo; j < hi; j++) {
                    int p = factorBase.primes[j];
                    long reciprocal = factorBase.reciprocals[j];
                    long z = (long) modSmallInt(step, stepLength, p, reciprocal) * factorBase.invAModP[j];
                    int delta = (int) (z - Math.multiplyHigh(z, reciprocal) * p);
                    if ((deltaRow[j] = delta) >= p) deltaRow[j] -= p;
                }
                lo = hi + 1;
            }
            for (int j = lo; j < factorBase.size; j++) {
                int p = factorBase.primes[j];
                long reciprocal = factorBase.reciprocals[j];
                long z = (long) modSmallInt(step, stepLength, p, reciprocal) * factorBase.invAModP[j];
                int delta = (int) (z - Math.multiplyHigh(z, reciprocal) * p);
                if ((deltaRow[j] = delta) >= p) deltaRow[j] -= p;
            }
        }
        plan.precomputation = System.nanoTime() - precomputation;
        long polynomialTime = System.nanoTime();
        // Construct initial B for polynomial 0.
        // Initial values, computed exactly once.
        long[] twoB = new long[A.length + 2];
        shiftLeftBitsInto(B, 1, twoB);
        long[] b2 = squareMagnitude(B);
        long[] deltaB;
        long[] cMagnitude;
        boolean cNegative = compareArray(b2, N) < 0;
        if (!cNegative) subtractMagnitudeInPlace(b2, N);
        else b2 = subtractMagnitudes(N, b2);
        cMagnitude = divideMagnitudesExact(b2, A);
        plan.polyNs += System.nanoTime() - polynomialTime;
        //log sieve
        long logSieve = System.nanoTime();
        Arrays.fill(factorBase.score, 0, interval, 0);
        markSmallRoots(largeStart, factorBase, interval);
        int hitCount = markLargeRoots(BR, largeStart, factorBase, interval);
        long totalSieveTime = System.nanoTime() - logSieve;
        //attempt after sieve
        int scoreThreshold = (int)((plan.logQEdge - factorBase.filterAllowance) * LOG_SCALE) + 1;
        plan.scanned += interval;
        long relationTime = System.nanoTime();
        int candidateCount = scanCandidates(interval, scoreThreshold, hitCount, factorBase, plan);
        plan.scanTime += System.nanoTime() - relationTime;
        collectQSRelations(N, A, B, BR, twoB, cMagnitude, cNegative, candidateCount, startT, factorBase, fbIndex,
                largeStart, relations, partials, partialGraph, plan);
        plan.attemptNs += System.nanoTime() - relationTime;
        plan.polys++;
        plan.sieveNs += totalSieveTime;
        cMagnitude = Arrays.copyOf(cMagnitude, cMagnitude.length + 1);
        for (int mask = 1; mask < 1 << (factorBase.aPrimeCount-1); mask++) {
            long[] oldB = B.clone();
            int bit = Integer.numberOfTrailingZeros(mask);
            long[] step = factorBase.twoTerms[bit + 1];
            boolean wrapped;
            if (((mask >>> (bit + 1)) & 1) == 0) {
                // B -= step (mod A)
                if (compareArray(B, step) < 0) {
                    B = subtractMagnitudes(A, subtractMagnitudes(step, B));
                    wrapped = true;
                } else {
                    subtractMagnitudeInPlace(B, step);
                    wrapped = false;
                }
                long cpuRoot = System.nanoTime();
                if (wrapped) updateRootsAddWrapped(factorBase, factorBase.grayDelta[bit]);
                else updateRootsAdd(factorBase, factorBase.grayDelta[bit]);
                rootCalcTime = System.nanoTime() - cpuRoot;
            } else {
                // B += step (mod A)
                B = addMagnitudes(B, step);
                wrapped = compareArray(B, A) >= 0;
                if (wrapped) subtractMagnitudeInPlace(B, A);
                long cpuRoot = System.nanoTime();
                if (wrapped) updateRootsSubtractWrapped(factorBase, factorBase.grayDelta[bit]);
                else updateRootsSubtract(factorBase, factorBase.grayDelta[bit]);
                rootCalcTime = System.nanoTime() - cpuRoot;
            }
            plan.rootCalcTime += rootCalcTime;
            // Incremental C update.
            polynomialTime = System.nanoTime();
            boolean deltaNegative = compareArray(B, oldB) < 0;
            deltaB = deltaNegative ? subtractMagnitudes(oldB, B) : subtractMagnitudes(B, oldB);
            if (!isZeroMagnitude(deltaB)) {
                long[] sumB = addMagnitudes(B, oldB);
                long[] deltaC = divideMagnitudesExact(multiplyMagnitudes(deltaB, sumB), A);
                if (cNegative == deltaNegative)
                    addMagnitudeInPlace(cMagnitude, deltaC, 0);
                else {
                    int cCmp = compareArray(cMagnitude, deltaC);
                    if (cCmp > 0) subtractMagnitudeInPlace(cMagnitude, deltaC);
                    else if (cCmp < 0) {
                        subtractMagnitudeInPlace(deltaC, cMagnitude);
                        cMagnitude = deltaC;
                        cNegative = deltaNegative;
                    } else {
                        cMagnitude = ZERO_MAG;
                        cNegative = false;
                    }
                }
            }
            shiftLeftBitsInto(B, 1, twoB);
            plan.polyNs += System.nanoTime() - polynomialTime;
            //log sieve
            logSieve = System.nanoTime();
            Arrays.fill(factorBase.score, 0);
            markSmallRoots(largeStart, factorBase, interval);
            hitCount = markLargeRoots(BR, largeStart, factorBase, interval);
            totalSieveTime = System.nanoTime() - logSieve;
            //attempt after sieve
            plan.scanned += interval;
            relationTime = System.nanoTime();
            candidateCount = scanCandidates(interval, scoreThreshold, hitCount, factorBase, plan);
            plan.scanTime += System.nanoTime() - relationTime;
            collectQSRelations(N, A, B, BR, twoB, cMagnitude, cNegative, candidateCount, startT, factorBase,
                    fbIndex, largeStart, relations, partials, partialGraph, plan);
            plan.attemptNs += System.nanoTime() - relationTime;
            plan.polys++;
            plan.sieveNs += totalSieveTime;
        }
        if (save)
            log.printf(
                    "%50s %10d %12d %16d %12d %10d %14d %8d %14d %13d"
                            + "%20.5f %14.5f %11.5f %16.5f %16.5f %16.5f %16.5f %16.5f %14.5f %13.5f %14.5f " +
                            "%14.5f %12.5f %12.5f %12.5f %15d %25.5f %n",
                    new LargeInteger(A),
                    interval,
                    plan.candidates,
                    plan.none,
                    plan.full,
                    plan.singlePartialStored,
                    plan.singlePartialMerged,
                    plan.doublePartialStored,
                    plan.doublePartialMerged,
                    plan.mixedPartialMerged,

                    plan.precomputation/1e6,
                    plan.rootCalcTime / 1e6,
                    plan.sieveNs / 1e6,
                    plan.scanTime / 1e6,
                    plan.candidateScanNs / 1e6,
                    plan.hitLinkNs / 1e6,
                    plan.smallDivideTime / 1e6,
                    plan.factorNs / 1e6,
                    plan.relationClassifyNs / 1e6,
                    plan.attemptNs / 1e6,
                    plan.collectionNs() / 1e6,

                    plan.usefulRows() * 1e9 / plan.collectionNs(),
                    plan.factorTestsTotal / (double) plan.smallDivisions,
                    plan.measuredUsefulRowsPerPoly(),
                    plan.measuredPartialMergesPerPoly(),
                    relations.size,
                    plan.empiricalSecondsRemaining(requiredRelations, relations.size)
            );
    }
    private static double weightedAverage(double first, double firstWeight, double second, double secondWeight) {
        double totalWeight = firstWeight + secondWeight;
        return totalWeight == 0.0 ? 0.0 : (first * firstWeight + second * secondWeight) / totalWeight;
    }
    private record BResult(long[] B, int[] inactiveLarge, int inactiveLargeCount) {}
    private static BResult computeB(ACoefficient AR, QSFactorBase factorBase, int largeStart) {
        long[] A = AR.A();
        int aLen = effectiveLength(A);
        long[] B = new long[aLen + 1];
        int inactiveLargeCount = 0;
        int[] factors = AR.factors();
        int[] inactiveLarge = new int[factors.length + 1];
        int lo = 1;
        for (int i = 0; i < factors.length; i++) {
            int packed = factors[i];
            int idx = packed >>> 1;
            // Compute A^-1 mod p for the active range before this A-factor.
            for (int j = lo; j < idx; j++) {
                int p = factorBase.primes[j];
                factorBase.invAModP[j] =
                        inverseModSmall(modSmallInt(A, aLen, p, factorBase.reciprocals[j]), p);
            }
            // idx is inactive because primes[idx] divides A.
            factorBase.inactive[i + 1] = idx;
            factorBase.aIndex[i] = idx + 1;
            factorBase.aCount[i] = 1;
            if (idx >= largeStart)
                inactiveLarge[inactiveLargeCount++] = idx;
            // Construct the corresponding B term.
            int p = factorBase.primes[idx];
            int root = (packed & 1) == 0 ? factorBase.root1[idx] : factorBase.root2[idx];
            long[] Ai = A.clone();
            divideMagnitudeExactInPlace(Ai, p);
            multiplySmallInto(Ai,(long) inverseModSmall(
                    modSmallInt(Ai, aLen, p, factorBase.reciprocals[idx]), p) * root % p, Ai);
            long[] twoAi = shiftLeftBits(Ai, 1);
            if (compareArray(twoAi, A) >= 0) subtractMagnitudeInPlace(twoAi, A);
            factorBase.twoTerms[i] = twoAi;
            addMagnitudeInPlace(B, Ai, 0);
            if (compareArray(B, A) >= 0) subtractMagnitudeInPlace(B, A);
            lo = idx + 1;
        }
        // Active tail after the final A-factor.
        for (int j = lo; j < factorBase.primes.length; j++) {
            int p = factorBase.primes[j];
            factorBase.invAModP[j] = inverseModSmall(modSmallInt(A, aLen, p, factorBase.reciprocals[j]), p);
        }
        return new BResult(trimLeadingZerosTrusted(B), inactiveLarge, inactiveLargeCount);
    }
    private static void computeInitialRoots(QSFactorBase factorBase, long[] B, int startT) {
        int lo = 0;
        for (int hi : factorBase.inactive) {
            for (int j = lo; j < hi; j++) {
                long reciprocal = factorBase.reciprocals[j];
                int p = factorBase.primes[j];
                int invA = factorBase.invAModP[j];
                int bMod = modSmallInt(B, B.length, p, reciprocal);
                int d1 = factorBase.root1[j] - bMod;
                int d2 = factorBase.root2[j] - bMod;
                if (d1 < 0) d1 += p;
                if (d2 < 0) d2 += p;
                long x1 = (long) d1 * invA;
                int r1 = (int) (x1 - Math.multiplyHigh(x1, reciprocal) * p);
                if (r1 >= p) r1 -= p;
                long x2 = (long) d2 * invA;
                int r2 = (int) (x2 - Math.multiplyHigh(x2, reciprocal) * p);
                if (r2 >= p) r2 -= p;
                int startMod = reduceSignedIntModP(startT, p, reciprocal);
                x1 = r1 - startMod;
                if (x1 < 0) x1 += p;
                x2 = r2 - startMod;
                if (x2 < 0) x2 += p;
                factorBase.pos1[j] = (int) x1;
                factorBase.pos2[j] = (int) x2;
            }
            lo = hi + 1;
        }
        for (int j = lo; j < factorBase.size; j++) {
            long reciprocal = factorBase.reciprocals[j];
            int p = factorBase.primes[j];
            int invA = factorBase.invAModP[j];
            int bMod = modSmallInt(B, B.length, p, reciprocal);
            int d1 = factorBase.root1[j] - bMod;
            int d2 = factorBase.root2[j] - bMod;
            if (d1 < 0) d1 += p;
            if (d2 < 0) d2 += p;
            long x1 = (long) d1 * invA;
            int r1 = (int) (x1 - Math.multiplyHigh(x1, reciprocal) * p);
            if (r1 >= p) r1 -= p;
            long x2 = (long) d2 * invA;
            int r2 = (int) (x2 - Math.multiplyHigh(x2, reciprocal) * p);
            if (r2 >= p) r2 -= p;
            int startMod = reduceSignedIntModP(startT, p, reciprocal);
            x1 = r1 - startMod;
            if (x1 < 0) x1 += p;
            x2 = r2 - startMod;
            if (x2 < 0) x2 += p;
            factorBase.pos1[j] = (int) x1;
            factorBase.pos2[j] = (int) x2;
        }
    }
    private static void updateRootsAdd(QSFactorBase fb, int[] deltaRow) {
        int lo = 0;
        for (int hi : fb.inactive) {
            for (int j = lo; j < hi; j++) {
                int p = fb.primes[j];
                int delta = deltaRow[j];
                int x1 = fb.pos1[j] + delta;
                int x2 = fb.pos2[j] + delta;
                if (x1 >= p) x1 -= p;
                if (x2 >= p) x2 -= p;
                fb.pos1[j] = x1;
                fb.pos2[j] = x2;
            }
            lo = hi + 1;
        }
        for (int j = lo; j < fb.size; j++) {
            int p = fb.primes[j];
            int delta = deltaRow[j];
            int x1 = fb.pos1[j] + delta;
            int x2 = fb.pos2[j] + delta;
            if (x1 >= p) x1 -= p;
            if (x2 >= p) x2 -= p;
            fb.pos1[j] = x1;
            fb.pos2[j] = x2;
        }
    }
    private static void updateRootsAddWrapped(QSFactorBase fb, int[] deltaRow) {
        int lo = 0;
        for (int hi : fb.inactive) {
            for (int j = lo; j < hi; j++) {
                int p = fb.primes[j];
                int delta = deltaRow[j];
                int x1 = fb.pos1[j] + delta;
                int x2 = fb.pos2[j] + delta;
                if (x1 >= p) x1 -= p;
                if (x2 >= p) x2 -= p;
                if (--x1 < 0) x1 += p;
                if (--x2 < 0) x2 += p;
                fb.pos1[j] = x1;
                fb.pos2[j] = x2;
            }
            lo = hi + 1;
        }
        for (int j = lo; j < fb.size; j++) {
            int p = fb.primes[j];
            int delta = deltaRow[j];
            int x1 = fb.pos1[j] + delta;
            int x2 = fb.pos2[j] + delta;
            if (x1 >= p) x1 -= p;
            if (x2 >= p) x2 -= p;
            if (--x1 < 0) x1 += p;
            if (--x2 < 0) x2 += p;
            fb.pos1[j] = x1;
            fb.pos2[j] = x2;
        }
    }
    private static void updateRootsSubtract(QSFactorBase fb, int[] deltaRow) {
        int lo = 0;
        for (int hi : fb.inactive) {
            for (int j = lo; j < hi; j++) {
                int p = fb.primes[j];
                int delta = deltaRow[j];
                int x1 = fb.pos1[j] - delta;
                int x2 = fb.pos2[j] - delta;
                x1 += (p & (x1 >> 31));
                x2 += (p & (x2 >> 31));
                fb.pos1[j] = x1;
                fb.pos2[j] = x2;
            }
            lo = hi + 1;
        }
        for (int j = lo; j < fb.size; j++) {
            int p = fb.primes[j];
            int delta = deltaRow[j];
            int x1 = fb.pos1[j] - delta;
            int x2 = fb.pos2[j] - delta;
            x1 += (p & (x1 >> 31));
            x2 += (p & (x2 >> 31));
            fb.pos1[j] = x1;
            fb.pos2[j] = x2;
        }
    }
    private static void updateRootsSubtractWrapped(QSFactorBase factorBase, int[] deltaRow) {
        int lo = 0;
        for (int hi : factorBase.inactive) {
            for (int j = lo; j < hi; j++) {
                int p = factorBase.primes[j];
                int delta = deltaRow[j];
                int x1 = factorBase.pos1[j] - delta, x2 = factorBase.pos2[j] - delta;
                x1 += (p & (x1 >> 31));
                x2 += (p & (x2 >> 31));
                if (++x1 >= p) x1 -= p;
                if (++x2 >= p) x2 -= p;
                factorBase.pos1[j] = x1;
                factorBase.pos2[j] = x2;
            }
            lo = hi + 1;
        }
        for (int j = lo; j < factorBase.size; j++) {
            int p = factorBase.primes[j];
            int delta = deltaRow[j];
            int x1 = factorBase.pos1[j] - delta, x2 = factorBase.pos2[j] - delta;
            x1 += (p & (x1 >> 31));
            x2 += (p & (x2 >> 31));
            if (++x1 >= p) x1 -= p;
            if (++x2 >= p) x2 -= p;
            factorBase.pos1[j] = x1;
            factorBase.pos2[j] = x2;
        }
    }
    private static void markSmallRoots(int largeStart, QSFactorBase factorBase, int interval) {
        int lo = 0;
        for (int hi : factorBase.inactive) {
            if (hi >= largeStart) break;
            for (int j = lo; j < hi; j++) {
                int p = factorBase.primes[j], x1 = factorBase.pos1[j], x2 = factorBase.pos2[j],
                        logP = factorBase.logPrimes[j];
                while (x1 < interval && x2 < interval) {
                    factorBase.score[x1] += logP;
                    factorBase.score[x2] += logP;
                    x1 += p;
                    x2 += p;
                }
                if (x1 < interval) factorBase.score[x1] += logP;
                if (x2 < interval) factorBase.score[x2] += logP;
            }
            lo = hi + 1;
        }
        for (int j = lo; j < largeStart; j++) {
            int p = factorBase.primes[j], x1 = factorBase.pos1[j], x2 = factorBase.pos2[j],
                    logP = factorBase.logPrimes[j];
            while (x1 < interval && x2 < interval) {
                factorBase.score[x1] += logP;
                factorBase.score[x2] += logP;
                x1 += p;
                x2 += p;
            }
            if (x1 < interval) factorBase.score[x1] += logP;
            if (x2 < interval) factorBase.score[x2] += logP;
        }
    }
    private static int markLargeRoots(BResult BR, int largeStart, QSFactorBase factorBase, int interval) {
        int hitCount = 0, lo = largeStart;
        int[] pos1 = factorBase.pos1, pos2 = factorBase.pos2, logPrimes = factorBase.logPrimes,
                score = factorBase.score, hitIndices = factorBase.hitIndices, hitPrimes = factorBase.hitPrimes;
        int[] inactiveLarge = BR.inactiveLarge();
        for (int k = 0, inactiveLargeCount = BR.inactiveLargeCount(); k < inactiveLargeCount; k++) {
            int hi = inactiveLarge[k];
            for (int j = lo; j < hi; j++) {
                int x1 = pos1[j], x2 = pos2[j], logP = logPrimes[j];
                if (x1 < interval) {
                    score[x1] += logP;
                    hitIndices[hitCount] = x1;
                    hitPrimes[hitCount++] = j;
                }
                if (x2 < interval) {
                    score[x2] += logP;
                    hitIndices[hitCount] = x2;
                    hitPrimes[hitCount++] = j;
                }
            }
            lo = hi + 1;
        }
        for (int j = lo, fbSize = factorBase.size; j < fbSize; j++) {
            int x1 = pos1[j], x2 = pos2[j], logP = logPrimes[j];
            if (x1 < interval) {
                score[x1] += logP;
                hitIndices[hitCount] = x1;
                hitPrimes[hitCount++] = j;
            }
            if (x2 < interval) {
                score[x2] += logP;
                hitIndices[hitCount] = x2;
                hitPrimes[hitCount++] = j;
            }
        }
        return hitCount;
    }
    private static int scanCandidates(int interval, int scoreThreshold, int hitCount, QSFactorBase factorBase,
                                      AStats plan) {
        long candidateScanStart = System.nanoTime();
        int candidateCount = 0;
        for (int idx = 0; idx < interval; idx++) {
            if (factorBase.score[idx] < scoreThreshold) continue;
            factorBase.candidateIndices[candidateCount++] = idx;
            factorBase.hitHead[idx] = -1;
        }
        plan.candidateScanNs += System.nanoTime() - candidateScanStart;
        long hitLinkStart = System.nanoTime();
        int keptHitCount = 0;
        for (int h = 0; h < hitCount; h++) {
            int index = factorBase.hitIndices[h];
            if (factorBase.score[index] < scoreThreshold) continue;
            factorBase.hitIndices[keptHitCount] = factorBase.hitHead[index];
            factorBase.hitPrimes[keptHitCount] = factorBase.hitPrimes[h];
            factorBase.hitHead[index] = keptHitCount++;
        }
        plan.hitLinkNs += System.nanoTime() - hitLinkStart;
        return candidateCount;
    }
    private static void clearUsed(short[] dense, int[] used, int usedCount) {
        for (int i = 0; i < usedCount; i++)
            dense[used[i]] = 0;
    }
    private static void performSmallDivisions(long[] remaining, int largeStart, int[] primes, long[] reciprocals,
                                              short[] dense, int[] used, int usedCount, int[] result) {
        int tested = 0;
        int remLength = effectiveLength(remaining);
        while (tested < largeStart) {
            int p = primes[tested];
            long reciprocal = reciprocals[tested];
            int column = ++tested;
            while (modSmallInt(remaining, remLength, p, reciprocal) == 0) {
                if (dense[column] == 0) used[usedCount++] = column;
                incExp(dense, column);
                divideMagnitudeExactInPlace(remaining, p);
                while (remLength > 1 && remaining[remLength-1] == 0) --remLength;
            }
            if (remLength == 1 && Long.compareUnsigned(remaining[0], (long) p * p) <= 0) break;
        }
        result[0] = tested;
        result[1] = usedCount;
        result[2] = remLength;
    }
    private static void performInactiveLargeDivisions(long[] remaining, int remLength, int[] inactiveLarge,
                                                      int inactiveLargeCount, int[] primes, long[] reciprocals,
                                                      short[] dense, int[] used, int usedCount, int[] result) {
        int tested = 0;
        while (tested < inactiveLargeCount) {
            int j = inactiveLarge[tested++];
            int p = primes[j];
            long reciprocal = reciprocals[j];
            int column = j + 1;
            while (modSmallInt(remaining, remLength, p, reciprocal) == 0) {
                if (dense[column] == 0) used[usedCount++] = column;
                incExp(dense, column);
                divideMagnitudeExactInPlace(remaining, p);
                while (remLength > 1 && remaining[remLength-1] == 0) --remLength;
            }
            if (remLength == 1) {
                long part = remaining[0];
                long pp = (long) p * p;
                if (Long.compareUnsigned(part, pp) <= 0)
                    break;
            }
        }
        result[0] = tested;
        result[1] = usedCount;
        result[2] = remLength;
    }
    private static void performHitLargeDivisions(long[] remaining, int remLength, int idx, int[] hitHead,
                                                 int[] hitIndices, int[] hitPrimes, int[] primes,
                                                 long[] reciprocals, short[] dense, int[] used,
                                                 int usedCount, int[] result) {
        int tested = 0;
        int h = hitHead[idx];
        hitHead[idx] = -1;
        while (h != -1) {
            tested++;
            int j = hitPrimes[h];
            h = hitIndices[h];
            int p = primes[j];
            long reciprocal = reciprocals[j];
            int column = j + 1;
            if (dense[column] == 0)
                used[usedCount++] = column;
            do {
                incExp(dense, column);
                divideMagnitudeExactInPlace(remaining, p);
                while (remLength > 1 && remaining[remLength - 1] == 0) remLength--;
            } while (modSmallInt(remaining, remLength, p, reciprocal) == 0);
        }
        result[0] = tested;
        result[1] = usedCount;
        result[2] = remLength;
    }
    private static void collectQSRelations(long[] N, long[] A, long[] B, BResult BR, long[] twoB,
                                           long[] cMag, boolean cNegative, int candidateCount,
                                           long startT, QSFactorBase factorBase, int[] fbIndex,
                                           int largeStart, QSFullRelationPool relations,
                                           QSPartialRelationPool partials, PartialGraph partialGraph,
                                           AStats plan) {
        for (int k = 0; k < candidateCount; k++) {
            int idx = factorBase.candidateIndices[k];
            long t = startT + idx;
            boolean tNegative = t < 0;
            long absT = tNegative ? -t : t;
            long[] atMagnitude = multiplySmall(A, absT);
            // q = At + 2B
            long[] qMagnitude = new long[Math.max(Math.max(atMagnitude.length, twoB.length) + 3, cMag.length + 1)];
            System.arraycopy(atMagnitude, 0, qMagnitude, 0, atMagnitude.length);
            boolean qNegative;
            if (!tNegative) {
                addMagnitudeInPlace(qMagnitude, twoB, 0);
                qNegative = false;
            } else {
                if (compareArray(twoB, atMagnitude) >= 0) {
                    // qMagnitude = twoB - atMagnitude, but retain scratch capacity
                    System.arraycopy(twoB, 0, qMagnitude, 0, twoB.length);
                    Arrays.fill(qMagnitude, twoB.length, qMagnitude.length, 0L);
                    subtractMagnitudeInPlace(qMagnitude, atMagnitude);
                    qNegative = false;
                } else {
                    subtractMagnitudeInPlace(qMagnitude, twoB);
                    qNegative = true;
                }
            }
            multiplySmallInto(qMagnitude, absT, qMagnitude);
            if (tNegative && !isZeroMagnitude(qMagnitude)) qNegative = !qNegative;
            if (qNegative == cNegative)
                addMagnitudeInPlace(qMagnitude, cMag, 0);
            else {
                if (compareArray(qMagnitude, cMag) >= 0)
                    subtractMagnitudeInPlace(qMagnitude, cMag);
                else {
                    qMagnitude = subtractMagnitudes(cMag, qMagnitude);
                    qNegative = cNegative;
                }
                if (isZeroMagnitude(qMagnitude)) qNegative = false;
            }
            if (factorBase.score[idx] / LOG_SCALE + factorBase.filterAllowance < lnMagnitude(qMagnitude)) continue;
            long[] currentX;
            if (!tNegative) currentX = modAdd(B, atMagnitude, N);
            else currentX = modSubtract(B, atMagnitude, N);
            int usedCount = 0;
            // A contribution: column 0 is negative, factorBase[j] lives at column j + 1.
            if (qNegative) {
                factorBase.used[usedCount++] = 0;
                incExp(factorBase.dense, 0);
            }
            if (isZeroMagnitude(qMagnitude)) {
                clearUsed(factorBase.dense, factorBase.used, usedCount);
                plan.observeRelationAdd(RelationAddKind.NONE);
                continue;
            }
            if (isOneMagnitude(qMagnitude)) {
                Arrays.sort(factorBase.used, 0, usedCount);
                int[] bIndex = Arrays.copyOf(factorBase.used, usedCount);
                short[] bCount = new short[usedCount];
                for (int i = 0; i < usedCount; i++)
                    bCount[i] = factorBase.dense[bIndex[i]];
                relations.addMerged(new long[][]{currentX}, factorBase.aIndex, factorBase.aCount, bIndex, bCount,
                        new long[0][]);
                clearUsed(factorBase.dense, factorBase.used, usedCount);
                plan.observeRelationAdd(RelationAddKind.FULL);
                continue;
            }
            long smallDivideTime = System.nanoTime();
            performSmallDivisions(qMagnitude, largeStart, factorBase.primes, factorBase.reciprocals,
                    factorBase.dense, factorBase.used, usedCount, factorBase.smallDivResult);
            int tested = factorBase.smallDivResult[0];
            usedCount = factorBase.smallDivResult[1];
            int remLength = factorBase.smallDivResult[2];
            plan.smallDivideTime += System.nanoTime() - smallDivideTime;
            performInactiveLargeDivisions(qMagnitude, remLength, BR.inactiveLarge(), BR.inactiveLargeCount(),
                    factorBase.primes, factorBase.reciprocals, factorBase.dense, factorBase.used, usedCount,
                    factorBase.smallDivResult);
            tested += factorBase.smallDivResult[0];
            usedCount = factorBase.smallDivResult[1];
            remLength = factorBase.smallDivResult[2];
            performHitLargeDivisions(qMagnitude, remLength, idx, factorBase.hitHead, factorBase.hitIndices,
                    factorBase.hitPrimes, factorBase.primes, factorBase.reciprocals, factorBase.dense,
                    factorBase.used, usedCount, factorBase.smallDivResult);
            tested += factorBase.smallDivResult[0];
            usedCount = factorBase.smallDivResult[1];
            remLength = factorBase.smallDivResult[2];
            plan.observeFactorTests(tested);
            long[][] xValues = {currentX};
            long classifyTime = System.nanoTime();
            /*
             * If the residual is itself a factor-base prime,
             * incorporate it before compressing the exponent vector.
             */
            qMagnitude = trimLeadingZerosTrusted(qMagnitude);
            if (remLength == 1 && Long.compareUnsigned(qMagnitude[0], fbIndex.length) < 0) {
                int index = fbIndex[(int) qMagnitude[0]] + 1;
                if (index >= 1) {
                    if (factorBase.dense[index] == 0) factorBase.used[usedCount++] = index;
                    incExp(factorBase.dense, index);
                    qMagnitude = ONE_MAG;
                }
            }
            Arrays.sort(factorBase.used, 0, usedCount);
            int[] localIndex = Arrays.copyOf(factorBase.used, usedCount);
            short[] localCount = new short[usedCount];
            for (int i = 0; i < usedCount; i++)
                localCount[i] = factorBase.dense[localIndex[i]];
            int mergedLength = mergedExponentLength(localIndex, localCount, factorBase.aIndex, factorBase.aCount);
            int[] index = new int[mergedLength];
            short[] count = new short[mergedLength];
            mergeExponents(localIndex, localCount, factorBase.aIndex, factorBase.aCount, index, count);
            clearUsed(factorBase.dense, factorBase.used, usedCount);
            plan.observeRelationAdd(classifyRelation(qMagnitude, index, count, xValues, relations, partials,
                    partialGraph, factorBase, plan));
            plan.relationClassifyNs += System.nanoTime() - classifyTime;
        }
    }
    private static RelationAddKind classifyRelation(long[] remaining, int[] index, short[] count, long[][] xValues,
                                                    QSFullRelationPool relations, QSPartialRelationPool partials,
                                                    PartialGraph partialGraph, QSFactorBase factorBase, AStats plan) {
        /*
         * Fully factor-base smooth.
         */
        if (isOneMagnitude(remaining)) {
            relations.add(xValues, index, count, new long[0][]);
            return RelationAddKind.FULL;
        }
        /*
         * A single-limb residual can be:
         * 1. A factor-base prime that was not consumed for some reason.
         * 2. A single large prime.
         * 3. A product of two large primes.
         */
        if (remaining.length == 1) {
            long residual = remaining[0];
            /*
             * Single-large-prime relation:
             * globalPMax < residual <= singleLargePrimeBound
             */
            if (withinLargePrimeRange(residual, factorBase.globalPMax,
                    factorBase.singleLargePrimeBound)) {
                long a = PARTIAL_ROOT;
                int pathLength = partialGraph.findPartialPath(a, residual);
                if (pathLength >= 0) {
                    combinePartialCycle(partialGraph.path, pathLength, xValues, index, count, a, residual,
                            partials, relations);
                    return RelationAddKind.SINGLE_PARTIAL_CYCLE;
                }
                int partialId = partials.add(xValues, index, count, a, residual);
                partialGraph.addForestEdge(a, residual, partialId);
                return RelationAddKind.SINGLE_PRIME_PARTIAL;
            }
        }
        if (!ENABLE_DOUBLE_PARTIALS) return RelationAddKind.NONE;
        /*
         * A valid double-large-prime residual cannot exceed L².
         * Do this test for multi-limb residuals as well, because L² may require
         * two limbs even when each individual large prime fits in one limb.
         */
        if (remaining.length > 2 || compareArray(remaining, factorBase.doubleLargePrimeBoundSquared) > 0
                || compareSmall(remaining, factorBase.pMaxSquared) <= 0)
            return RelationAddKind.NONE;
        /*
         * Avoid trying to split a residual that is itself prime.
         * A prime residual larger than doubleLargePrimeBound cannot be accepted.
         */
        if (isAProbablyPrimeMagnitude(remaining))
            return RelationAddKind.NONE;
        long t0 = System.nanoTime();
        long[] factor = fermatSearch(remaining);
        if (!isNontrivialFactor(factor, remaining)) {
            factor = pollardPMinusOne(remaining);
            if (!isNontrivialFactor(factor, remaining)) {
                factor = brentRhos(remaining);
                if (!isNontrivialFactor(factor, remaining)) {
                    plan.factorNs += System.nanoTime() - t0;
                    return RelationAddKind.NONE;
                }
            }
        }
        plan.factorNs += System.nanoTime() - t0;
        long[] otherFactor = divideMagnitudesExact(remaining, factor);
        /*
         * The unified partial graph currently stores vertices as unsigned longs,
         * so both factors must be single-limb.
         */
        if (factor.length != 1 || otherFactor.length != 1)
            return RelationAddKind.NONE;
        long q1 = factor[0];
        long q2 = otherFactor[0];
        if (!withinLargePrimeRange(q1, factorBase.globalPMax, factorBase.doubleLargePrimeBound)
                || !withinLargePrimeRange(q2, factorBase.globalPMax, factorBase.doubleLargePrimeBound))
            return RelationAddKind.NONE;
        /*
         * The factor finder may return a composite divisor.
         */
        if (!isAProbablyPrimeMagnitude(factor) || !isAProbablyPrimeMagnitude(otherFactor))
            return RelationAddKind.NONE;
        /*
         * residual = q² already has even large-prime parity, so this is a full
         * relation rather than a graph edge.
         */
        if (q1 == q2) {
            relations.add(xValues, index, count, new long[][]{{q1}});
            return RelationAddKind.DOUBLE_PARTIAL_CYCLE;
        }
        int pathLength = partialGraph.findPartialPath(q1, q2);
        if (pathLength >= 0) {
            combinePartialCycle(partialGraph.path, pathLength, xValues, index, count, q1, q2, partials,
                    relations);
            return RelationAddKind.DOUBLE_PARTIAL_CYCLE;
        }
        int partialId = partials.add(xValues, index, count, q1, q2);
        partialGraph.addForestEdge(q1, q2, partialId);
        return RelationAddKind.DOUBLE_PRIME_PARTIAL;
    }
    private record RelationYieldEstimate(double fullPerPoly, double singlePartialHitsPerPoly,
                                         double singlePartialMergesPerPoly, double doublePartialHitsPerPoly,
                                         double doublePartialMergesPerPoly, double usefulRowsPerPoly) {
        static RelationYieldEstimate of(double fullHitsPerPoly, double singlePartialHitsPerPoly,
                                        double singlePartialMergesPerPoly, double doublePartialHitsPerPoly,
                                        double doublePartialMergesPerPoly) {
            double usableRowsPerPoly = fullHitsPerPoly + singlePartialMergesPerPoly + doublePartialMergesPerPoly;
            return new RelationYieldEstimate(fullHitsPerPoly, singlePartialHitsPerPoly,
                    singlePartialMergesPerPoly, doublePartialHitsPerPoly, doublePartialMergesPerPoly,
                    usableRowsPerPoly);
        }
    }
    private static double unsignedLongToDouble(long x) {
        return x >= 0 ? (double) x : (double) (x & Long.MAX_VALUE) + 0x1.0p63;
    }
    private static double lnMagnitude(long[] mag) {
        if (isZeroMagnitude(mag))
            return Double.NEGATIVE_INFINITY;
        int n = effectiveLength(mag);
        double hi = unsignedLongToDouble(mag[n - 1]);
        if (n > 1)
            hi += StrictMath.scalb(unsignedLongToDouble(mag[n - 2]), -64);
        return StrictMath.log(hi) + 64.0 * (n - 1) * StrictMath.log(2.0);
    }
    private static void incExp(short[] dense, int column) {
        if (dense[column] == Short.MAX_VALUE)
            throw new ArithmeticException("Exponent overflow at column " + column);
        dense[column]++;
    }
    private static boolean withinLargePrimeRange(long value, long globalPMax, long largePrimeBound)
    {return Long.compareUnsigned(value, globalPMax) > 0 && Long.compareUnsigned(value, largePrimeBound) <= 0;}
    private enum RelationAddKind {
        NONE,
        FULL,
        SINGLE_PRIME_PARTIAL,
        DOUBLE_PRIME_PARTIAL,
        SINGLE_PARTIAL_CYCLE,
        DOUBLE_PARTIAL_CYCLE,
        MIXED_PARTIAL_CYCLE
    }
    private static void combinePartialCycle(int[] path, int pathLength, long[][] closingX, int[] closingExpIdx,
                                            short[] closingExpCount, long startVertex, long targetVertex, QSPartialRelationPool partials,
                                            QSFullRelationPool fulls) {
        // Count X values.
        int xCount = closingX.length;
        for (int i = 0; i < pathLength; i++)
            xCount += partials.xValues[path[i]].length;
        long[][] mergedX = new long[xCount][];
        int xOut = 0;
        for (int i = 0; i < pathLength; i++) {
            long[][] xs = partials.xValues[path[i]];
            for (long[] x : xs)
                mergedX[xOut++] = x;
        }
        for (long[] x : closingX)
            mergedX[xOut++] = x;
        /*
         * Start with the closing relation's factor-base exponents,
         * then merge in every retained forest edge on the path.
         */
        int[] mergedIndex = closingExpIdx;
        short[] mergedCount = closingExpCount;
        for (int i = 0; i < pathLength; i++) {
            int id = path[i];
            int[] bIndex = partials.expIdx[id];
            short[] bCount = partials.expCount[id];
            int len = mergedExponentLength(mergedIndex, mergedCount, bIndex, bCount);
            int[] nextIndex = new int[len];
            short[] nextCount = new short[len];
            mergeExponents(mergedIndex, mergedCount, bIndex, bCount, nextIndex, nextCount);
            mergedIndex = nextIndex;
            mergedCount = nextCount;
        }
        /*
         * The forest path plus the closing edge forms a simple cycle.
         * Every non-root vertex occurs twice, so one copy of that
         * large prime belongs on the square-root side.
         */
        int squareCount = 0;
        long vertex = startVertex;
        if (vertex != PARTIAL_ROOT)
            squareCount++;
        for (int i = 0; i < pathLength; i++) {
            vertex = partials.otherVertex(path[i], vertex);
            if (vertex != PARTIAL_ROOT)
                squareCount++;
        }
        if (vertex != targetVertex) throw new IllegalStateException(
                "Partial path does not terminate at target vertex");
        long[][] squareFactors = new long[squareCount][];
        int squareOut = 0;
        vertex = startVertex;
        if (vertex != PARTIAL_ROOT)
            squareFactors[squareOut++] = new long[]{vertex};
        for (int i = 0; i < pathLength; i++) {
            vertex = partials.otherVertex(path[i], vertex);
            if (vertex != PARTIAL_ROOT)
                squareFactors[squareOut++] = new long[]{vertex};
        }
        fulls.add(mergedX, mergedIndex, mergedCount, squareFactors);
    }
    private static void validateQSRelations(QSFullRelationPool relations, QSFactorBase factorBase, long[] N) {
        MontgomeryReducer reducer = new MontgomeryReducer(N);
        for (int r = 0; r < relations.size; r++) {
            long[] lhs = reducer.rModM.clone();
            for (long[] xv : relations.xValues[r])
                reducer.multiplyInto(lhs, reducer.toMontgomery(xv), lhs);
            reducer.squareInto(lhs, lhs);
            long[] rhs = reducer.rModM.clone();
            for (int k = 0; k < relations.exponentIndex[r].length; k++) {
                int col = relations.exponentIndex[r][k];
                int e = relations.exponentCount[r][k];
                if (col == 0) {
                    if ((e & 1) != 0)
                        // Montgomery representation of -rhs mod N.
                        rhs = reducer.negate(rhs);
                } else {
                    long[] temp = new long[reducer.k];
                    reducer.powMontgomery(reducer.toMontgomery(factorBase.primes[col - 1]), e, temp);
                    reducer.multiplyInto(rhs, temp, temp);
                    rhs = temp;
                }
            }
            for (long[] sf : relations.squareFactors[r])
                reducer.multiplyInto(rhs, reducer.square(reducer.toMontgomery(sf)), rhs);
            lhs = reducer.fromMontgomery(lhs);
            rhs = reducer.fromMontgomery(rhs);
            if (!equalMagnitudes(lhs, rhs))
                throw new AssertionError("BAD QS RELATION " + r + "\nxValues="
                        + relations.xValues[r].length + "\nsquareFactors=" + relations.squareFactors[r].length
                        + "\nlhs=" + new LargeInteger(lhs) + "\nrhs=" + new LargeInteger(rhs));
        }
    }

    private static ArrayList<SparseMatrixRow> singletonFilter(ArrayList<SparseMatrixRow> rows, int columnCount,
                                                              QSFullRelationPool relations, QSFactorBase factorBase, long[] N) {
        int[] weight = new int[columnCount];
        // Count initial column incidences.
        for (SparseMatrixRow row : rows) {
            if (!row.alive) continue;
            for (int c : row.cols)
                weight[c]++;
        }

        // CSR offsets.
        int[] offsets = new int[columnCount + 1];
        for (int c = 0; c < columnCount; c++)
            offsets[c + 1] = offsets[c] + weight[c];

        int[] incidentRows = new int[offsets[columnCount]];
        int[] cursor = offsets.clone();

        for (int r = 0, size = rows.size(); r < size; r++) {
            SparseMatrixRow row = rows.get(r);
            if (!row.alive) continue;

            for (int c : row.cols)
                incidentRows[cursor[c]++] = r;
        }

        // A column can become singleton only once because weights only decrease.
        int[] queue = new int[columnCount];
        int head = 0;
        int tail = 0;
        for (int c = 0; c < columnCount; c++) {
            if (weight[c] == 1)
                queue[tail++] = c;
        }
        while (head < tail) {
            int c = queue[head++];
            // Mostly defensive; should normally still be 1.
            if (weight[c] != 1) continue;
            int r = -1;
            for (int k = offsets[c], end = offsets[c + 1]; k < end; k++) {
                int candidate = incidentRows[k];
                if (rows.get(candidate).alive) {
                    r = candidate;
                    break;
                }
            }
            if (r < 0) continue;
            SparseMatrixRow dead = rows.get(r);
            dead.alive = false;
            for (int c2 : dead.cols) {
                int w = --weight[c2];
                if (w == 1) queue[tail++] = c2;
            }
        }
        ArrayList<SparseMatrixRow> survivors = new ArrayList<>();
        for (SparseMatrixRow row : rows) {
            if (!row.alive) continue;
            if (row.cols.length == 0) {
                long[] g = reconstructFromSparseDependency(row.deps, relations, factorBase, N);
                if (isNontrivialFactor(g, N))
                    throw new FactorFoundException(g);
                continue;
            }
            survivors.add(row);
        }
        return survivors;
    }
    private static int[] xorSorted(int[] a, int[] b) {
        int[] tmp = new int[a.length + b.length];
        int i = 0;
        int j = 0;
        int out = 0;
        while (i < a.length && j < b.length) {
            int x = a[i];
            int y = b[j];
            if (x == y) {
                i++;
                j++;
            } else if (x < y) {
                tmp[out++] = x;
                i++;
            } else {
                tmp[out++] = y;
                j++;
            }
        }
        while (i < a.length)
            tmp[out++] = a[i++];

        while (j < b.length)
            tmp[out++] = b[j++];
        return Arrays.copyOf(tmp, out);
    }
    private static int findOneAliveRowContaining(ArrayList<SparseMatrixRow> rows, ArrayIntList[] incidence,
                                                 int column) {
        ArrayIntList list = incidence[column];
        if (list == null) return -1;
        int found = -1;
        for (int r: list) {
            if (!rows.get(r).alive) continue;
            if (found != -1) return -1;
            found = r;
        }
        return found;
    }
    private static int[] findTwoAliveRowsContaining(ArrayList<SparseMatrixRow> rows, ArrayIntList[] incidence,
                                                    int column) {
        ArrayIntList list = incidence[column];
        if (list == null) return null;
        int first = -1;
        int second = -1;
        int out = 0;
        for (int r: list) {
            if (!rows.get(r).alive) continue;
            list.set(out++, r);
            if (first < 0) first = r;
            else if (second < 0) second = r;
        }
        // Dead rows can never become alive again.
        list.size = out;
        return second < 0 ? null : new int[]{first, second};
    }
    private static ArrayList<SparseMatrixRow> degreeTwoMergeFilter(ArrayList<SparseMatrixRow> rows,
                                                                   int columnCount, QSFullRelationPool relations,
                                                                   QSFactorBase factorBase, long[] N) {
        ArrayIntList[] incidence = new ArrayIntList[columnCount];
        int[] weight = new int[columnCount];
        for (int r = 0; r < rows.size(); r++) {
            SparseMatrixRow row = rows.get(r);
            if (!row.alive) continue;
            for (int c : row.cols) {
                weight[c]++;
                if (incidence[c] == null) incidence[c] = new ArrayIntList();
                incidence[c].add(r);
            }
        }
        for (boolean changed = true; changed;) {
            changed = false;
            // Singleton elimination.
            for (int c = 0; c < columnCount; c++) {
                if (weight[c] != 1) continue;
                int r = findOneAliveRowContaining(rows, incidence, c);
                if (r < 0) continue;
                SparseMatrixRow row = rows.get(r);
                row.alive = false;
                // Maintain weights instead of recomputing them.
                for (int c2 : row.cols) weight[c2]--;
                changed = true;
            }
            // Degree-2 elimination.
            for (int c = 0; c < columnCount; c++) {
                if (weight[c] != 2) continue;
                int[] pair = findTwoAliveRowsContaining(rows, incidence, c);
                if (pair == null) continue;
                SparseMatrixRow a = rows.get(pair[0]);
                SparseMatrixRow b = rows.get(pair[1]);
                int[] mergedCols = xorSorted(a.cols, b.cols);
                if (mergedCols.length > 256) continue;
                int[] mergedDeps = xorSorted(a.deps, b.deps);
                a.alive = false;
                b.alive = false;
                // Remove both old rows from the live column degrees.
                for (int c2 : a.cols) weight[c2]--;
                for (int c2 : b.cols) weight[c2]--;
                int newRowIndex = rows.size();
                rows.add(new SparseMatrixRow(mergedCols, mergedDeps));
                // Add new row to both incidence structure and live weights.
                for (int c2 : mergedCols) {
                    weight[c2]++;
                    if (incidence[c2] == null)
                        incidence[c2] = new ArrayIntList();
                    incidence[c2].add(newRowIndex);
                }
                changed = true;
                if (mergedCols.length == 0) {
                    long[] g = reconstructFromSparseDependency(mergedDeps, relations, factorBase, N);
                    if (isNontrivialFactor(g, N))
                        throw new FactorFoundException(g);
                }
            }
        }
        ArrayList<SparseMatrixRow> survivors = new ArrayList<>();
        for (SparseMatrixRow row : rows)
            if (row.alive)
                survivors.add(row);
        return survivors;
    }
    private static final class SparseMatrixRow {
        int[] cols; // sorted odd columns
        int[] deps; // sorted original relation indices xor-combination
        boolean alive;
        SparseMatrixRow(int[] cols, int relationIndex) {
            this.cols = cols;
            deps = new int[]{relationIndex};
            alive = true;
        }
        SparseMatrixRow(int[] cols, int[] deps) {
            this.cols = cols;
            this.deps = deps;
            alive = true;
        }
    }
    private static SparseMatrixSetup buildQSMatrix(QSFullRelationPool relations, QSFactorBase factorBase,
                                                   long[] N, PrintWriter log, boolean save) {
        long t0 = System.nanoTime();
        ArrayList<SparseMatrixRow> sparseRows = new ArrayList<>(relations.size);
        for (int i = 0, size = relations.size; i < size; i++) {
            int[] index = relations.exponentIndex[i];
            short[] exp = relations.exponentCount[i];
            int odd = 0;
            for (short e : exp) odd += e & 1;
            int[] cols = new int[odd];
            int out = 0;
            for (int k = 0; k < exp.length; k++)
                if ((exp[k] & 1) != 0)
                    cols[out++] = index[k];
            sparseRows.add(new SparseMatrixRow(cols, i));
        }
        long sparseRowNanos = System.nanoTime()-t0;
        t0 = System.nanoTime();
        sparseRows = singletonFilter(sparseRows, factorBase.columnCount, relations, factorBase, N);
        long singleton1Nanos = System.nanoTime()-t0;
        t0 = System.nanoTime();
        sparseRows = degreeTwoMergeFilter(sparseRows, factorBase.columnCount, relations, factorBase, N);
        long doubletonNanos = System.nanoTime()-t0;
        t0 = System.nanoTime();
        sparseRows = singletonFilter(sparseRows, factorBase.columnCount, relations, factorBase, N);
        long singleton2Nanos = System.nanoTime()-t0;
        // All filtering is finished at this point.
        boolean[] used = new boolean[factorBase.columnCount];
        for (SparseMatrixRow row : sparseRows)
            for (int c : row.cols)
                used[c] = true;
        // Build an order-preserving old-column -> new-column mapping.
        int[] remap = new int[factorBase.columnCount];
        Arrays.fill(remap, -1);
        int activeColumnCount = 0;
        for (int c = 0; c < factorBase.columnCount; c++) {
            if (used[c])
                remap[c] = activeColumnCount++;
        }// Rewrite every surviving row using compact column IDs.
        for (SparseMatrixRow row : sparseRows) {
            for (int i = 0; i < row.cols.length; i++) {
                int mapped = remap[row.cols[i]];
                if (mapped < 0)
                    throw new AssertionError("Unmapped active column " + row.cols[i]);
                row.cols[i] = mapped;
            }
        }
        int rowCount = sparseRows.size();
        int[] rowOffsets = new int[rowCount + 1];
        int[][] rowDeps = new int[rowCount][];
        int nnz = 0;
        for (int r = 0; r < rowCount; r++) {
            SparseMatrixRow row = sparseRows.get(r);
            rowOffsets[r] = nnz;
            nnz += row.cols.length;
            rowDeps[r] = row.deps;
        }
        rowOffsets[rowCount] = nnz;
        int[] columns = new int[nnz];
        int k = 0;
        for (SparseMatrixRow row : sparseRows) {
            System.arraycopy(row.cols, 0, columns, k, row.cols.length);
            k += row.cols.length;
        }
        GF2SparseMatrix matrix = new GF2SparseMatrix(rowCount, activeColumnCount, rowOffsets, columns);
        if (save) {
            log.println("Time for building sparse rows: " + formatSeconds(sparseRowNanos));
            log.println("Time for first singleton filter: " + formatSeconds(singleton1Nanos));
            log.println("Time for doubleton filter: " + formatSeconds(doubletonNanos));
            log.println("Time for second singleton filter: " + formatSeconds(singleton2Nanos));
            log.println("Matrix dimensions: " + rowCount + " rows by " + activeColumnCount + " columns");
            log.println("Final nnz: " + nnz);
            int min = Integer.MAX_VALUE;
            int max = 0;
            long total = 0;
            for (int r = 0; r < rowCount; r++) {
                int degree = rowOffsets[r + 1] - rowOffsets[r];
                min = Math.min(min, degree);
                max = Math.max(max, degree);
                total += degree;
            }
            log.println("avg degree = " + (double) total / rowCount);
            log.println("min = " + min + ", max = " + max);
            log.flush();
        }
        return new SparseMatrixSetup(matrix, rowDeps);
    }
    private record SparseMatrixSetup(GF2SparseMatrix matrix, int[][] rowDeps) {}
    private static final class GF2SparseMatrix{
        int rowCount;
        int columnCount;
        int[] rowOffsets;
        int[] columns;
        GF2SparseMatrix(int rowCount, int columnCount, int[] rowOffsets, int[] columns) {
            this.rowCount = rowCount;
            this.columnCount = columnCount;
            this.rowOffsets = rowOffsets;
            this.columns = columns;
        }
        long[] multiplyTransposeVector(long[] packedRows) {
            int expectedLength = (rowCount + 63) >>> 6;
            if (packedRows.length != expectedLength)
                throw new IllegalArgumentException(
                        "Expected packed row vector length " + expectedLength + ", got " + packedRows.length);
            long[] packedColumns = new long[(columnCount + 63) >>> 6];
            for (int wordIndex = 0;
                 wordIndex < packedRows.length;
                 wordIndex++) {
                long bits = packedRows[wordIndex];
                while (bits != 0L) {
                    int offset = Long.numberOfTrailingZeros(bits);
                    int row = (wordIndex << 6) + offset;
                    bits &= bits - 1;
                    if (row >= rowCount) break;
                    int end = rowOffsets[row + 1];
                    for (int k = rowOffsets[row]; k < end; k++) {
                        int column = columns[k];
                        packedColumns[column >>> 6] ^= 1L << (column & 63);
                    }
                }
            }
            return packedColumns;
        }
        private void multiplyMMtBlock64(long[] rowBlock, long[] colScratch, long[] out) {
            // out = A^T * x
            // x.length == rows
            // out.length == cols
            if (rowBlock.length != rowCount)
                throw new IllegalArgumentException("Expected block length " + rowCount +
                        ", got " + rowBlock.length);
            if (colScratch.length != columnCount)
                throw new IllegalArgumentException("Expected block width " + columnCount +
                        ", got " + colScratch.length);
            if (out.length != rowCount)
                throw new IllegalArgumentException("Expected output length " + rowCount + ", got " + out.length);
            Arrays.fill(colScratch, 0L);
            long t0 = System.nanoTime();
            multiplyTransposeBlock(rowBlock, colScratch);
            long t1 = System.nanoTime();
            multiplyBlock(colScratch, out);
            long t2 = System.nanoTime();
            transposeBlockTime += t1 - t0;
            blockTime += t2 - t1;
        }
        private void multiplyTransposeBlock(long[] rowBlock, long[] colScratch) {
            for (int r = 0; r < rowCount; r++) {
                long lanes = rowBlock[r];
                for (int k = rowOffsets[r], end = rowOffsets[r + 1]; k < end; k++)
                    colScratch[columns[k]] ^= lanes;
            }
        }
        private void multiplyBlock(long[] colScratch, long[] out) {
            for (int r = 0; r < rowCount; r++) {
                long lanes = 0L;
                for (int k = rowOffsets[r], end = rowOffsets[r + 1]; k < end; k++)
                    lanes ^= colScratch[columns[k]];
                out[r] = lanes;
            }
        }
        private static void buildRightMulTable64(long[] matrix, long[] table) {
            long t0 = System.nanoTime();
            for (int byteIndex = 0; byteIndex < 8; byteIndex++) {
                int tableBase = byteIndex << 8;
                int matrixBase = byteIndex << 3;
                table[tableBase] = 0L;
                for (int value = 1; value < 256; value++) {
                    int bit = Integer.numberOfTrailingZeros(value);
                    int without = value & (value - 1);
                    table[tableBase + value] = table[tableBase + without] ^ matrix[matrixBase + bit];
                }
            }
            tableBuildTime += System.nanoTime() - t0;
        }
        static long rightMultiplyLookup(long x, long[] table)
        {return    table[(int) x & 255]
                    ^ table[256  | ((int) (x >>>  8) & 255)]
                    ^ table[512  | ((int) (x >>> 16) & 255)]
                    ^ table[768  | ((int) (x >>> 24) & 255)]
                    ^ table[1024 | ((int) (x >>> 32) & 255)]
                    ^ table[1280 | ((int) (x >>> 40) & 255)]
                    ^ table[1536 | ((int) (x >>> 48) & 255)]
                    ^ table[1792 | ((int) (x >>> 56) & 255)];}
        BlockLanczosResult blockLanczos(SplittableRandom rng) {
            long[] LEFT = new long[rowCount];
            long[] RIGHT = new long[rowCount];
            for (int k = 0; k < RIGHT.length; k++)
                RIGHT[k] = rng.nextLong();
            long[] dTable = new long[2048];
            long[] eTable = new long[2048];
            long[] fTable = new long[2048];
            long[] rightMulTable = new long[2048];
            long[] colScratch = new long[columnCount];
            long[] initialV = new long[rowCount];
            multiplyMMtBlock64(RIGHT, colScratch, initialV);
            BlockLanczosState state = new BlockLanczosState(initialV);
            long[][] bucket = new long[8][256];
            long[] av = new long[rowCount];
            long[] overlap = new long[64];
            long[] coefficients = new long[64];
            for (int iter = 0; iter <= rowCount+64; ++iter) {
                multiplyMMtBlock64(state.v, colScratch, av);
                long[] gram = new long[64];
                GF2Matrix64.blockDot(state.v, av, bucket, gram);
                if (isZeroArray(gram))
                    return new BlockLanczosResult(LEFT, RIGHT, state.v, iter);
                if (VERIFY_BLOCK_LANCZOS) {
                    long[] transposed = GF2Matrix64.transpose(gram);
                    if (!Arrays.equals(gram, transposed))
                        throw new AssertionError("Gram matrix is not symmetric");
                }
                long mask = 0L;
                for (long row : state.v) mask |= row;
                GF2Elim64 elimination = GF2Matrix64.eliminate64(gram, mask & ~state.selectedMaskPrev);
                GF2Matrix64.blockDot(state.v, initialV, bucket, overlap);
                GF2Matrix64.multiplyInto(elimination.embeddedInverse(), overlap, coefficients);
                buildRightMulTable64(coefficients, rightMulTable);
                long t0 = System.nanoTime();
                for (int i = 0; i < state.v.length; i++)
                    LEFT[i] ^= rightMultiplyLookup(state.v[i], rightMulTable);
                tableApplyTime += System.nanoTime() - t0;
                updateLanczosState(elimination.selectedMask(), av, gram, elimination.embeddedInverse(),
                        state, dTable, eTable, fTable, bucket);
            }
            throw new IllegalStateException("Block Lanczos did not terminate");
        }
        private static final long[] scratchA = new long[64];
        private static final long[] scratchB = new long[64];
        private static void updateLanczosState(long selectedMask, long[] av, long[] gram, long[] wInv,
                                               BlockLanczosState state, long[] dTable, long[] eTable,
                                               long[] fTable, long[][] bucket) {
            // qRows = (h * P) XOR gram where h = av^T av
            long[] qRows = new long[64];
            GF2Matrix64.blockDot(av, av, bucket, qRows);
            for (int i = 0; i < 64; i++)
                qRows[i] = (qRows[i] & selectedMask) ^ gram[i];
            // d = I XOR wInv*q
            GF2Matrix64.multiplyInto(wInv, qRows, scratchA);
            for (int i = 0; i < 64; i++)
                scratchA[i] ^= 1L << i;
            buildRightMulTable64(scratchA, dTable);
            // e = (wInvPrev * gram) * P
            GF2Matrix64.multiplyInto(state.wInvPrev, gram, scratchA);
            for (int i = 0; i < 64; i++)
                scratchA[i] &= selectedMask;
            buildRightMulTable64(scratchA, eTable);
            // f
            GF2Matrix64.multiplyInto(state.gPrev, state.wInvPrev, scratchA);
            for (int i = 0; i < 64; i++)
                scratchA[i] ^= 1L << i;
            GF2Matrix64.multiplyInto(state.wInvPrev2, scratchA, scratchB);
            GF2Matrix64.multiplyInto(scratchB, state.qPrev, scratchA);
            for (int i = 0; i < 64; i++)
                scratchA[i] &= selectedMask;
            buildRightMulTable64(scratchA, fTable);
            long[] nextV = new long[av.length];
            long t0 = System.nanoTime();
            for (int i = 0; i < nextV.length; i++)
                nextV[i] = (av[i] & selectedMask)
                        ^ rightMultiplyLookup(state.v[i], dTable)
                        ^ rightMultiplyLookup(state.vPrev[i], eTable)
                        ^ rightMultiplyLookup(state.vPrev2[i], fTable);
            tableApplyTime += System.nanoTime() - t0;
            state.vPrev2 = state.vPrev;
            state.vPrev = state.v;
            state.v = nextV;
            state.wInvPrev2 = state.wInvPrev;
            state.wInvPrev = wInv;
            state.gPrev = gram;
            state.qPrev = qRows;
            state.selectedMaskPrev = selectedMask;
        }
        long[][] extractLanczosDependencies(long[] d, long[] terminalV) {
            long[] low = new long[columnCount];
            long[] high = new long[columnCount];
            for (int row = 0; row < rowCount; row++) {
                long lo = d[row];
                long hi = terminalV[row];
                int end = rowOffsets[row + 1];
                for (int k = rowOffsets[row]; k < end; k++) {
                    int col = columns[k];
                    low[col] ^= lo;
                    high[col] ^= hi;
                }
            }
            int rowCount = low.length;
            int packedRowWords = (rowCount + 63) >>> 6;
            // Convert the 128 candidate columns into packed column vectors.
            long[][] columns = new long[128][packedRowWords];
            for (int row = 0; row < rowCount; row++) {
                long low1 = low[row];
                while (low1 != 0L) {
                    int bit = Long.numberOfTrailingZeros(low1);
                    columns[bit][row >>> 6] |= 1L << (row & 63);
                    low1 &= low1 - 1;
                }
                long high1 = high[row];
                while (high1 != 0L) {
                    int bit = Long.numberOfTrailingZeros(high1);
                    columns[64 + bit][row >>> 6] |= 1L << (row & 63);
                    high1 &= high1 - 1;
                }
            }
            long[][] pivotColumns = new long[rowCount][];
            Combination128[] pivotCombinations = new Combination128[rowCount];
            ArrayList<Combination128> nullspace = new ArrayList<>(128);
            for (int column = 0; column < 128; column++) {
                long[] vector = columns[column].clone();
                long combinationLow = column < 64 ? 1L << column : 0L;
                long combinationHigh = column >= 64 ? 1L << (column - 64) : 0L;
                for (;;) {
                    int pivot = -1;
                    for (int word = 0; word < vector.length; word++) {
                        long value = vector[word];
                        if (value != 0L) {
                            pivot = (word << 6) + Long.numberOfTrailingZeros(value);
                            break;
                        }
                    }
                    if (pivot < 0) {
                        // This combination of candidate columns is zero.
                        nullspace.add(new Combination128(combinationLow, combinationHigh));
                        break;
                    }
                    if (pivotColumns[pivot] == null) {
                        pivotColumns[pivot] = vector;
                        pivotCombinations[pivot] = new Combination128(combinationLow, combinationHigh);
                        break;
                    }
                    xorInto(vector, pivotColumns[pivot]);
                    Combination128 old = pivotCombinations[pivot];
                    combinationLow ^= old.low();
                    combinationHigh ^= old.high();
                }
            }
            ArrayList<long[]> dependencies = new ArrayList<>();
            for (Combination128 combination : nullspace) {
                long[] dependency = new long[(d.length + 63) >>> 6];
                for (int row = 0; row < d.length; row++) {
                    int parity = (Long.bitCount(d[row] & combination.low())
                            ^ Long.bitCount(terminalV[row] & combination.high()));
                    if ((parity & 1) != 0)
                        dependency[row >>> 6] |= 1L << (row & 63);
                }
                if (isZeroArray(dependency)) continue;
                if (VERIFY_BLOCK_LANCZOS) {
                    long[] check = multiplyTransposeVector(dependency);
                    for (long word : check)
                        if (word != 0L)
                            throw new AssertionError("Invalid Block Lanczos dependency");
                }
                dependencies.add(dependency);
            }
            return dependencies.toArray(long[][]::new);
        }
    }
    private static final class BlockLanczosState {
        long[] v;
        long[] vPrev;
        long[] vPrev2;

        long[] wInvPrev;
        long[] wInvPrev2;

        long[] gPrev;
        long[] qPrev;

        long selectedMaskPrev;

        BlockLanczosState(long[] initialV) {
            int dimension = initialV.length;

            v = initialV;
            vPrev = new long[dimension];
            vPrev2 = new long[dimension];

            wInvPrev = new long[64];
            wInvPrev2 = new long[64];

            gPrev = new long[64];
            qPrev = new long[64];

            selectedMaskPrev = -1L;
        }
    }
    private static final boolean VERIFY_BLOCK_LANCZOS = false;
    private record BlockLanczosResult(long[] LEFT, long[] RIGHT, long[] terminalV, int iterations) {}
    /**
     * @param selectedMask    Indices selected for S_i. Bit j is set when Gram column/row j is active.
     * @param inverse         Ordinary 64×64 inverse. Null unless rank == 64.
     * @param embeddedInverse S_i * (S_i^T Q S_i)^-1 * S_i^T. Always defined; zero matrix when rank == 0.
     * @param reduced         Optional but useful for debugging.
     */
    private record GF2Elim64(int rank, long selectedMask, long[] inverse, long[] embeddedInverse, long[] reduced) {}
    private static long blockDotTime;
    private static long eliminateTime;
    private static long tableBuildTime;
    private static long tableApplyTime;
    private static long transposeBlockTime;
    private static long blockTime;
    private static final class GF2Matrix64{
        private GF2Matrix64() {throw new AssertionError("Why are you even doing this?");}
        static GF2Elim64 eliminate64(long[] rows, long requiredMask) {
            long t0 = System.nanoTime();
            long[] reducedRows = rows.clone();
            int[] columnOrder = new int[64];
            int orderLength = 0;
            // Required columns first.
            for (long bits = requiredMask; bits != 0L; bits &= bits - 1)
                columnOrder[orderLength++] = Long.numberOfTrailingZeros(bits);
            // Then every remaining column.
            long optionalMask = ~requiredMask;
            for (long bits = optionalMask;
                 bits != 0L;
                 bits &= bits - 1) {
                columnOrder[orderLength++] = Long.numberOfTrailingZeros(bits);
            }
            int rank = 0;
            long selectedMask = 0L;
            for (int position = 0; position < 64; position++) {
                int col = columnOrder[position];
                int pivotRow = rank;
                while (pivotRow < 64 && ((reducedRows[pivotRow] >>> col) & 1L) == 0L) pivotRow++;
                if (pivotRow == 64) continue;
                long swap = reducedRows[rank];
                reducedRows[rank] = reducedRows[pivotRow];
                reducedRows[pivotRow] = swap;
                for (int row = 0; row < 64; row++)
                    if (row != rank && ((reducedRows[row] >>> col) & 1L) != 0L)
                        reducedRows[row] ^= reducedRows[rank];
                selectedMask |= 1L << col;
                rank++;
            }
            if (VERIFY_BLOCK_LANCZOS)
                if ((selectedMask & requiredMask) != requiredMask)
                    throw new AssertionError("Could not include required Lanczos lanes: required="
                            + Long.toHexString(requiredMask) + ", selected=" + Long.toHexString(selectedMask));
            long[] embeddedInverse = embeddedPrincipalInverse(rows, selectedMask);
            if (VERIFY_BLOCK_LANCZOS) {
                long[] projector = new long[64];
                for (long bits = selectedMask; bits != 0L; bits &= bits - 1) {
                    int lane = Long.numberOfTrailingZeros(bits);
                    projector[lane] = 1L << lane;
                }
                long[] left = multiply(multiply(embeddedInverse, rows), projector);
                long[] right = multiply(multiply(projector, rows), embeddedInverse);
                if (!Arrays.equals(left, projector) || !Arrays.equals(right, projector))
                    throw new AssertionError("Invalid embedded inverse");
                if (!Arrays.equals(embeddedInverse, transpose(embeddedInverse)))
                    throw new AssertionError(
                            "Embedded inverse of symmetric principal minor is not symmetric");
            }
            long[] inverse = rank == 64 ? embeddedInverse : null;
            eliminateTime += System.nanoTime() - t0;
            return new GF2Elim64(rank, selectedMask, inverse, embeddedInverse, reducedRows);
        }
        static long[] embeddedPrincipalInverse(long[] rows, long selectedMask) {
            int rank = Long.bitCount(selectedMask);
            if (rank == 0) return new long[64];
            int[] selected = new int[rank];
            int position = 0;
            for (long bits = selectedMask;
                 bits != 0L;
                 bits &= bits - 1) {
                selected[position++] = Long.numberOfTrailingZeros(bits);
            }
            /*
             * Build:
             *
             * [ G[J,J]  0 ]
             * [   0     I ]
             */
            long[] compactRows = new long[64];
            for (int compactRow = 0; compactRow < rank; compactRow++) {
                int originalRow = selected[compactRow];
                long source = rows[originalRow];
                long destination = 0L;
                for (int compactColumn = 0; compactColumn < rank; compactColumn++) {
                    int originalColumn = selected[compactColumn];
                    if ((source & (1L << originalColumn)) != 0L)
                        destination |= 1L << compactColumn;
                }
                compactRows[compactRow] = destination;
            }
            for (int i = rank; i < 64; i++)
                compactRows[i] = 1L << i;
            long[] left = compactRows.clone();
            long[] compactInverse = new long[64];
            for (int row = 0; row < 64; row++)
                compactInverse[row] = 1L << row;
            int invRank = 0;
            for (int col = 0; col < 64; col++) {
                int pivotRow = invRank;
                while (pivotRow < 64 && ((left[pivotRow] >>> col) & 1L) == 0L) pivotRow++;
                if (pivotRow == 64)
                    throw new AssertionError("Selected principal submatrix is singular at "
                            + Long.toHexString(selectedMask));
                long swap = left[invRank];
                left[invRank] = left[pivotRow];
                left[pivotRow] = swap;
                swap = compactInverse[invRank];
                compactInverse[invRank] = compactInverse[pivotRow];
                compactInverse[pivotRow] = swap;
                for (int row = 0; row < 64; row++) {
                    if (row != invRank && ((left[row] >>> col) & 1L) != 0L) {
                        left[row] ^= left[invRank];
                        compactInverse[row] ^= compactInverse[invRank];
                    }
                }
                invRank++;
            }
            if (invRank != 64)
                throw new AssertionError("Selected principal submatrix is singular at "
                        + Long.toHexString(selectedMask));
            long[] embeddedInverse = new long[64];
            for (int compactRow = 0; compactRow < rank; compactRow++) {
                int originalRow = selected[compactRow];
                long compactInverseRow = compactInverse[compactRow];
                long destination = 0L;
                for (int compactColumn = 0; compactColumn < rank; compactColumn++) {
                    if ((compactInverseRow & (1L << compactColumn)) != 0L)
                        destination |= 1L << selected[compactColumn];
                }
                embeddedInverse[originalRow] = destination;
            }
            return embeddedInverse;
        }
        static long[] multiply(long[] rows, long[] other) {
            long[] out = new long[64];
            multiplyInto(rows, other, out);
            return out;
        }
        static void multiplyInto(long[] a, long[] b, long[] out) {
            for (int i = 0; i < 64; i++) {
                long bits = a[i];
                long r = 0L;
                while (bits != 0L) {
                    int j = Long.numberOfTrailingZeros(bits);
                    r ^= b[j];
                    bits &= bits - 1;
                }
                out[i] = r;
            }
        }
        static void blockDot(long[] left, long[] right, long[][] bucket, long[] out) {
            if (left.length != right.length)
                throw new IllegalArgumentException("Block dimension mismatch");
            long t0 = System.nanoTime();
            for (long[] b : bucket)
                Arrays.fill(b, 0L);
            for (int coordinate = 0; coordinate < left.length; coordinate++) {
                long a = left[coordinate];
                long b = right[coordinate];
                bucket[0][(int)( a         & 0xFF)] ^= b;
                bucket[1][(int)((a >>>  8) & 0xFF)] ^= b;
                bucket[2][(int)((a >>> 16) & 0xFF)] ^= b;
                bucket[3][(int)((a >>> 24) & 0xFF)] ^= b;
                bucket[4][(int)((a >>> 32) & 0xFF)] ^= b;
                bucket[5][(int)((a >>> 40) & 0xFF)] ^= b;
                bucket[6][(int)((a >>> 48) & 0xFF)] ^= b;
                bucket[7][(int)((a >>> 56) & 0xFF)] ^= b;
            }
            for (int byteIndex = 0; byteIndex < 8; byteIndex++) {
                long[] t = bucket[byteIndex];
                int base = byteIndex << 3;
                int length = 256;
                for (int bit = 0; bit < 7; bit++) {
                    long result = 0L;
                    int write = 0;
                    for (int i = 0; i < length; i += 2) {
                        long even = t[i];
                        long odd = t[i + 1];
                        result ^= odd;
                        t[write++] = even ^ odd;
                    }
                    out[base + bit] = result;
                    length >>>= 1;
                }
                // After seven reductions:
                // t[0] = XOR of values 0..127
                // t[1] = XOR of values 128..255
                out[base + 7] = t[1];
            }
            blockDotTime += System.nanoTime() - t0;
        }
        static long[] transpose(long[] rows) {
            long[] transposedRows = new long[64];
            for (int row = 0; row < 64; row++) {
                long bits = rows[row];
                while (bits != 0L) {
                    int column = Long.numberOfTrailingZeros(bits);
                    transposedRows[column] |= 1L << row;
                    bits &= bits - 1;
                }
            }
            return transposedRows;
        }
    }
    private static void xorInto(long[] destination, long[] source) {
        if (destination.length != source.length)
            throw new IllegalArgumentException("Block dimension mismatch");
        for (int i = 0; i < destination.length; i++)
            destination[i] ^= source[i];
    }
    private record Combination128(long low, long high) {}
    private static long[][] nullspaceDependencies(SparseMatrixSetup setup, SplittableRandom rng,
                                                  int originalRelationCount, PrintWriter log, boolean save) {
        GF2SparseMatrix matrix = setup.matrix();
        for (int attempt = 0; attempt < 8; attempt++) {
            SplittableRandom attemptRng = rng.split();
            long t0 = System.nanoTime();
            BlockLanczosResult lanczos = matrix.blockLanczos(attemptRng);
            if (save) {
                log.println("Block Lanczos iterations: " + lanczos.iterations());
                log.println("Lanczos core: " + formatSeconds(System.nanoTime() - t0));
                log.println("Time spent on blockDot: " + formatSeconds(blockDotTime));
                log.println("Time spent on eliminate64: " + formatSeconds(eliminateTime));
                log.println("Time spent on buildRightMultiplyTable64: " + formatSeconds(tableBuildTime));
                log.println("Time spent on rightMultiplyLookup: " + formatSeconds(tableApplyTime));
                log.println("Time spent on A^T * block: " + formatSeconds(transposeBlockTime));
                log.println("Time spent on A * block: " + formatSeconds(blockTime));
            }
            long[] d = new long[Math.max(lanczos.LEFT().length, lanczos.RIGHT().length)];
            xorInto(d, lanczos.LEFT);
            xorInto(d, lanczos.RIGHT);
            t0 = System.nanoTime();
            long[][] filteredDependencies = matrix.extractLanczosDependencies(d, lanczos.terminalV);
            if (filteredDependencies.length == 0) continue;
            if (save)
                log.println(
                        "Extracting dependencies: " + formatSeconds(System.nanoTime() - t0));
            t0 = System.nanoTime();
            long[][] expanded = new long[filteredDependencies.length][];
            int[][] rowDeps = setup.rowDeps();
            int expectedWords = (rowDeps.length + 63) >>> 6;
            for (int i = 0; i < filteredDependencies.length; i++) {
                long[] packedFilteredRows = filteredDependencies[i];
                if (packedFilteredRows.length != expectedWords)
                    throw new IllegalArgumentException("Filtered dependency length mismatch");
                long[] originalRelations = new long[(originalRelationCount + 63) >>> 6];
                for (int wordIndex = 0; wordIndex < packedFilteredRows.length; wordIndex++) {
                    long bits = packedFilteredRows[wordIndex];
                    while (bits != 0L) {
                        int offset = Long.numberOfTrailingZeros(bits);
                        int filteredRow = (wordIndex << 6) + offset;
                        bits &= bits - 1;
                        if (filteredRow >= rowDeps.length) break;
                        for (int originalRelation : rowDeps[filteredRow])
                            originalRelations[originalRelation >>> 6] ^= 1L << (originalRelation & 63);
                    }
                }
                expanded[i] = originalRelations;
            }
            if (save) {
                log.println("Expanding dependencies: " + formatSeconds(System.nanoTime() - t0));
                log.println();
                log.flush();
            }
            return expanded;
        }
        throw new IllegalStateException("Block Lanczos repeatedly failed to find dependencies");
    }
    private static long[] reconstructFromSparseDependency(int[] deps, QSFullRelationPool relations,
                                                          QSFactorBase factorBase, long[] N) {
        MontgomeryReducer reducer = new MontgomeryReducer(N);
        long[] X = reducer.rModM.clone();
        long[] Y = reducer.rModM.clone();
        long[] expSums = new long[factorBase.size+1];
        for (int relIndex : deps) {
            for (long[] x : relations.xValues[relIndex])
                reducer.multiplyInto(X, reducer.toMontgomery(x), X);
            for (int k = 0; k < relations.exponentIndex[relIndex].length; k++)
                expSums[relations.exponentIndex[relIndex][k]] += relations.exponentCount[relIndex][k];
            for (long[] rf : relations.squareFactors[relIndex])
                reducer.multiplyInto(Y, reducer.toMontgomery(rf), Y);
        }
        for (int col = 0; col < expSums.length; col++) {
            long e = expSums[col];
            if ((e & 1L) != 0)
                throw new AssertionError("Dependency exponent sum is odd at column " + col);
            long half = e >>> 1;
            if (half == 0) continue;
            if (col == 0) {
                // (-1)^half
                if ((half & 1L) != 0)
                    Y = reducer.negate(Y);
            } else {
                long[] temp = new long[reducer.k];
                reducer.powMontgomery(reducer.toMontgomery(factorBase.primes[col - 1]), half, temp);
                reducer.multiplyInto(Y, temp, temp);
                Y = temp;
            }
        }
        if (!equalMagnitudes(reducer.square(X), reducer.square(Y))) {
            System.err.println("X^2 = " + new LargeInteger(squareMagnitude(reducer.fromMontgomery(X))));
            System.err.println("Y^2 = " + new LargeInteger(squareMagnitude(reducer.fromMontgomery(Y))));
            throw new AssertionError("X^2 !≡ Y^2 mod N");
        }
        Y = reducer.fromMontgomery(Y);
        X = reducer.fromMontgomery(X);
        long[] g = gcdMagnitudes(modSubtract(X, Y, N), N);
        if (isNontrivialFactor(g, N)) return g;
        g = gcdMagnitudes(addMagnitudes(X, Y), N);
        if (isNontrivialFactor(g, N)) return g;
        return ONE_MAG;
    }
    private static long[] reconstructFactor(long[] dependency, QSFullRelationPool relations,
                                            QSFactorBase factorBase, long[] N) {
        int expectedWords = (relations.size + 63) >>> 6;
        if (dependency.length != expectedWords)
            throw new IllegalArgumentException("Dependency length mismatch");
        MontgomeryReducer reducer = new MontgomeryReducer(N);
        long[] X = reducer.rModM.clone();
        long[] Y = reducer.rModM.clone();
        int columnCount = factorBase.size + 1;
        long[] exponentSums = new long[columnCount];
        boolean selectedAny = false;
        for (int wordIndex = 0; wordIndex < dependency.length; wordIndex++) {
            long bits = dependency[wordIndex];
            while (bits != 0L) {
                int offset = Long.numberOfTrailingZeros(bits);
                int relationIndex = (wordIndex << 6) + offset;
                bits &= bits - 1;
                if (relationIndex >= relations.size) continue;
                selectedAny = true;
                for (long[] xValue : relations.xValues[relationIndex])
                    reducer.multiplyInto(X, reducer.toMontgomery(xValue), X);
                for (long[] squareFactor : relations.squareFactors[relationIndex])
                    reducer.multiplyInto(Y, reducer.toMontgomery(squareFactor), Y);
                int[] indexes = relations.exponentIndex[relationIndex];
                short[] counts = relations.exponentCount[relationIndex];
                for (int j = 0; j < indexes.length; j++) {
                    int column = indexes[j];
                    if (column < 0 || column >= columnCount)
                        throw new AssertionError("Relation exponent column out of range: "
                                + column + " of " + columnCount);
                    exponentSums[column] += counts[j];
                }
            }
        }
        if (!selectedAny)
            return ONE_MAG;
        for (int column = 0; column < columnCount; column++) {
            long exponent = exponentSums[column];
            if ((exponent & 1L) != 0L)
                throw new IllegalStateException("Invalid dependency: odd exponent " + exponent + " at column "
                        + column);
            long halfExponent = exponent >>> 1;
            if (halfExponent == 0L) continue;
            if (column == 0) {
                if ((halfExponent & 1L) != 0L)
                    Y = reducer.negate(Y);
            } else {
                long[] temp = new long[reducer.k];
                reducer.powMontgomery(reducer.toMontgomery(factorBase.primes[column - 1]), halfExponent, temp);
                reducer.multiplyInto(Y, temp, temp);
                Y = temp;
            }
        }
        // These can be checked while still in Montgomery representation.
        if (!equalMagnitudes(reducer.square(X), reducer.square(Y))) {
            System.err.println("X^2 = " + new LargeInteger(squareMagnitude(reducer.fromMontgomery(X))));
            System.err.println("Y^2 = " + new LargeInteger(squareMagnitude(reducer.fromMontgomery(Y))));
            throw new AssertionError("X^2 !≡ Y^2 mod N");
        }
        // Leave Montgomery representation only once.
        X = reducer.fromMontgomery(X);
        Y = reducer.fromMontgomery(Y);
        long[] factor = gcdMagnitudes(modSubtract(X, Y, N), N);
        if (isNontrivialFactor(factor, N)) return factor;
        factor = gcdMagnitudes(modAdd(X, Y, N), N);
        if (isNontrivialFactor(factor, N)) return factor;
        return ONE_MAG;
    }
    /**
     * DO NOT CHANGE THIS CONSTANT.
     * <p>0.55 has already been benchmarked against smaller values.
     * Smaller values produce vastly fewer relations and are slower overall.
     * This is not an invitation to benchmark it again.</p>
     */
    private static final double QS_FACTOR_BASE_C = 0.5;
    @SuppressWarnings("unused")
    public LargeInteger quadraticSieveWithoutConstraints(boolean save) {
        if (isProbablyPrime()) return this;
        double lnN = lnMagnitude(limbs);
        double lnLnN = StrictMath.log(lnN);
        long B = Math.round(Math.exp(QS_FACTOR_BASE_C * StrictMath.sqrt(lnN * lnLnN)));
        B = Math.max(B, 100L);
        if (B > Integer.MAX_VALUE)
            throw new ArithmeticException("Unconstrained QS bound " + B + " exceeds supported int range");
        try (PrintWriter log = save ? new PrintWriter(Files.newBufferedWriter(
                Path.of("QS_factorization.log (benchmark)" + this), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) : null) {
            long t0 = System.nanoTime();
            QSFactorBase factorBase = QSFactorBase.buildFactorBase(limbs, (int) B);
            int[] fbIndex = factorBase.buildFactorBaseIndexMap();
            long factorBaseBuildTime = System.nanoTime() - t0;
            int requiredRelations = factorBase.primes.length + 30;
            if (save) {
                String S = toString();
                log.println("Number entered unconstrained QS: " + S + " (" + S.length() + " digits)");
                log.println("Factor-base bound: " + B);
                log.println("Factor base build time: " + formatSeconds(factorBaseBuildTime) + " ("
                        + factorBase.size + " primes; " + requiredRelations + " relations required)");
            }
            QSFullRelationPool relations = new QSFullRelationPool(requiredRelations +
                    requiredRelations/4);
            QSPartialRelationPool partials = new QSPartialRelationPool(requiredRelations +
                    requiredRelations/4);
            PartialGraph partialGraph = new PartialGraph();
            //A deterministic seed is used so that A sequences are fixed. This is to make an objective
            //decision on if a code change really made the method faster. Please do not replace with
            //rng.nextLong() or rng.split(). Same case for the block Lanczos seed
            long A_RNG_SEED = 6601994121933596671L;
            if (save) {
                log.println("A RNG seed: " + Long.toUnsignedString(A_RNG_SEED));
                log.println();
            }
            SplittableRandom A_RNG = new SplittableRandom(A_RNG_SEED);
            long relationStart = System.nanoTime();
            if (save)
                log.printf(
                        "%4s %50s %10s %12s %16s %12s %10s %14s %8s %14s %13s %19s %14s %11s %16s %16s %16s %16s %16s %14s %13s %14s %14s %12s %12s %12s %15s %25s%n",
                        "#", "A", "int", "candidates", "rejected", "full", "sing", "sing merge", "double", "double merge", "mix merge", "precomputation",
                        "root", "sieve", "scanning", "candidateScan", "hitLink", "smallDiv", "factoring", "class", "processing", "total", "rel/s",
                        "div/poly", "rel/poly", "merge/poly", "rel total", "est. sec remaining");
            long[] sqrt2N = sqrtCeilMagnitude(shiftLeftBits(limbs, 1));
            for (long blocks = 0; relations.size < requiredRelations; blocks++) {
                if (save)
                    log.printf("%4d ", blocks + 1);
                sieve128Polynomials(limbs, sqrt2N, factorBase, fbIndex, relations, partials, partialGraph,
                        A_RNG, requiredRelations, log, save);
                if (save && (blocks & 31) == 31)
                    log.flush();
            }
            long relationNanos = System.nanoTime() - relationStart;
            if (save) {
                log.println();
                log.println("Total QS relation time: " + formatSeconds(relationNanos));
                log.println("Overall relation collection rate: " + relations.size * 1e9 / relationNanos
                        + " relations/second");
                log.println();
            }
            validateQSRelations(relations, factorBase, limbs);
            int originalRelationCount = relations.size;
            long buildMatrixNanos = System.nanoTime();
            SparseMatrixSetup setup = buildQSMatrix(relations, factorBase, limbs, log, save);
            buildMatrixNanos = System.nanoTime() - buildMatrixNanos;
            if (save) {
                log.println("Matrix build time: " + formatSeconds(buildMatrixNanos));
                log.println();
            }
            long matrixSeed = 6722688380560470180L;
            long[][] dependencies = nullspaceDependencies(setup, new SplittableRandom(matrixSeed),
                    originalRelationCount, log, save);
            for (int tested = 1; tested <= dependencies.length; tested++) {
                long[] factor = reconstructFactor(dependencies[tested-1], relations, factorBase, limbs);
                if (isNontrivialFactor(factor, limbs)) {
                    LargeInteger result = new LargeInteger(factor);
                    if (save) {
                        log.printf("Dependencies tested: %d%n", tested);
                        log.printf("Factor found: %s%n", result);
                        log.close();
                    }
                    return result;
                }
            }
        } catch (FactorFoundException e) {return new LargeInteger(e.factor);}
        catch (IOException e) {throw new AssertionError(e);}
        return ONE;
    }

    /**
     * Given an integer N, this method finds a congruence X^2 ≡ Y^2 (mod N).
     * Then (X-Y) and (X+Y) reveal factors of N.
     */
    private static long[] quadraticSieve(FactorJob task, double remainingECMSeconds, boolean qsSave) {
        if (!Double.isFinite(remainingECMSeconds) || remainingECMSeconds <= 0.0) return ONE_MAG;
        /*
         * Spend at most 5% of the known remaining ECM time testing QS.
         */
        double probeSeconds = Math.max(remainingECMSeconds * 0.05, 20.0);
        long probeBudgetNanos = Math.round(probeSeconds * 1e9);
        long totalStart = System.nanoTime();
        /*
         * Factor-base construction time counts against the QS probe.
         */
        try (PrintWriter log = qsSave ?
                     new PrintWriter(Files.newBufferedWriter(Path.of("QS_factorization.log " + new LargeInteger(task.n)),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) : null) {
            if (qsSave) {
                String numberText = new LargeInteger(task.n).toString();
                log.println("Number entering QS: " + numberText + '(' + numberText.length() + " digits)");
            }
            if (task.factorBase == null) {
                long t0 = System.nanoTime();
                task.factorBase = QSFactorBase.buildFactorBase(task.n, Math.toIntExact(task.qsBound()));
                task.fbIndex = task.factorBase.buildFactorBaseIndexMap();
                if (qsSave)
                    log.println("Factor base build time: " + formatSeconds(System.nanoTime() - t0));
            }
            int requiredRelations = task.factorBase.size + 30;
            if (qsSave)
                log.println(task.factorBase.size + " primes; " + requiredRelations + " relations required");
            if (task.relations == null)
                task.relations = new QSFullRelationPool(requiredRelations*9/8);
            if (task.partials == null)
                task.partials = new QSPartialRelationPool(requiredRelations*9/8);
            ArrayList<Double> blockRates = new ArrayList<>();
            int blocks = 0;
            boolean qsAccepted;
            SplittableRandom rng = new SplittableRandom();
            if (qsSave)
                log.printf(
                        "%4s %50s %10s %12s %16s %12s %10s %14s %8s %14s %13s %19s %14s %11s %16s %16s %16s %16s %16s %14s %13s %14s %14s %12s %12s %12s %15s %25s%n",
                        "#", "A", "interval", "candidates", "rejected", "full", "sing", "sing merge", "double", "double merge", "mix merge", "precomputation",
                        "root", "sieve", "scanning", "candidateScan", "hitLink", "smallDiv", "factoring", "class", "processing", "total", "rel/s",
                        "div/poly", "rel/poly", "merge/poly", "rel total", "est. sec remaining");
            long[] sqrt2N = sqrtCeilMagnitude(shiftLeftBits(task.n, 1));
            while (task.relations.size < requiredRelations) {
                int relationsBefore = task.relations.size;
                if (qsSave)
                    log.printf("%4d", blocks + 1);
                long blockStart = System.nanoTime();
                sieve128Polynomials(task.n, sqrt2N, task.factorBase, task.fbIndex, task.relations, task.partials,
                        task.partialGraph, rng, requiredRelations, log, qsSave);
                long blockNanos = System.nanoTime() - blockStart;
                int relationsAdded = task.relations.size - relationsBefore;
                double rate = blockNanos == 0L ? 0.0 : relationsAdded * 1e9 / blockNanos;
                blockRates.add(rate);
                blocks++;
                if (blocks % 10 == 0 && qsSave)
                    log.flush();
                long elapsedNanos = System.nanoTime() - totalStart;
                boolean budgetExpired = elapsedNanos >= probeBudgetNanos;
                if (!budgetExpired) continue;
                /*
                 * A very short sample cannot reliably distinguish a lucky
                 * block from a good QS input.
                 */
                double medianRate = 0;
                for (double blockRate: blockRates) medianRate += blockRate;
                medianRate /= blockRates.size();
                if (!(medianRate > 0.0) || !Double.isFinite(medianRate)) {
                    if (qsSave) {
                        log.println("QS rejected: mean verified-relation rate is zero");
                        log.println();
                        log.close();
                    }
                    return ONE_MAG;
                }
                int remainingRelations = requiredRelations - task.relations.size;
                double projectedQSSeconds = 1.15*remainingRelations / medianRate;
                task.qsSeconds = projectedQSSeconds;
                if (qsSave)
                    log.println("QS probe: blocks=" + blocks + ", relations total=" + task.relations.size
                            + "/" + requiredRelations + ", mean rate=" + medianRate + "/s, projected remaining="
                            + projectedQSSeconds + " seconds");
                qsAccepted = projectedQSSeconds*0.75 < remainingECMSeconds;
                if (!qsAccepted) {
                    if (qsSave) {
                        log.println("QS rejected in favor of ECM");
                        log.println();
                        log.close();
                    }
                    return ONE_MAG;
                }
                if (qsSave)
                    log.println("QS accepted over remaining ECM work");
                break;
            }
            /*
             * Continue from the relations already collected by the probe.
             * Do not restart QS.
             */
            while (task.relations.size < requiredRelations) {
                if (qsSave)
                    log.printf("%4d", blocks + 1);
                sieve128Polynomials(task.n, sqrt2N, task.factorBase, task.fbIndex, task.relations, task.partials,
                        task.partialGraph, rng, requiredRelations, log, qsSave);
                blocks++;
                if (blocks % 10 == 0 && qsSave)
                    log.flush();
            }
            if (qsSave) {
                log.println("Total QS relation time: "
                        + formatSeconds(System.nanoTime() - totalStart));
                log.flush();
            }
            validateQSRelations(task.relations, task.factorBase, task.n);
            int originalRelationCount = task.relations.size;
            long buildMatrixNanos = System.nanoTime();
            SparseMatrixSetup setup = buildQSMatrix(task.relations, task.factorBase, task.n, log, qsSave);
            buildMatrixNanos = System.nanoTime() - buildMatrixNanos;
            if (qsSave) {
                log.println("Matrix build time: " + formatSeconds(buildMatrixNanos));
                log.println();
            }
            long[][] dependencies = nullspaceDependencies(setup, rng, originalRelationCount, log, qsSave);
            for (long[] dependency : dependencies) {
                long[] factor = reconstructFactor(dependency, task.relations, task.factorBase, task.n);
                if (isNontrivialFactor(factor, task.n)) {
                    if (qsSave) {
                        log.println("QS FACTOR FOUND: " + new LargeInteger(factor));
                        log.println();
                        log.close();
                    }
                    return factor;
                }
            }
            if (qsSave)
                log.flush();
        } catch (FactorFoundException e) {return e.factor;}
        catch (IOException e) {throw new AssertionError(e);}
        return ONE_MAG;
    }

    //--- Number Field Sieve(go back up and tell the class name "I'm sorry for seeing your undies") ---//

    private record RationalFBEntry(int prime, int root, long reciprocal, int log) {}
    private static RationalFBEntry[] buildRationalFactorBase(long[] evalPoint, int bound) {
        int[] primes = sieve(bound);
        RationalFBEntry[] base = new RationalFBEntry[primes.length];
        for (int i = 0; i < primes.length; i++) {
            int p = primes[i];
            long rec = Long.divideUnsigned(-1, p);
            int root = modSmallInt(evalPoint, evalPoint.length, p, rec);
            base[i] = new RationalFBEntry(p, root, rec, (int) Math.round(LOG_SCALE*Math.log(p)));
        }
        return base;
    }
    private record AlgebraicFBEntry(int prime, int root, long reciprocal, int log, boolean projective) {}
    private static ModPolynomial polynomialModuloPrime(int[] polynomial, int p) {
        LargeInteger[] integers = new LargeInteger[polynomial.length];
        for (int k = 0; k < polynomial.length; k++)
            integers[k] = valueOf(polynomial[k]);
        return new ModPolynomial(integers, valueOf(p));
    }
    private static AlgebraicFBEntry[] buildAlgebraicFactorBase(int[] polynomial, int bound) {
        ArrayList<AlgebraicFBEntry> base = new ArrayList<>();
        for (int p : sieve(bound)) {
            // Returns ALL distinct affine roots modulo p.
            LargeInteger[] roots = polynomialModuloPrime(polynomial, p).rootsModPrime();
            int round = (int) Math.round(LOG_SCALE * Math.log(p));
            for (LargeInteger root : roots)
                base.add(new AlgebraicFBEntry(p, root.intValue(), Long.divideUnsigned(-1, p), round,false));
            // Leading coefficient vanishes modulo p:
            // the homogenized polynomial has a projective root.
            if (Math.floorMod(polynomial[polynomial.length - 1], p) == 0)
                base.add(new AlgebraicFBEntry(p, 0, Long.divideUnsigned(-1, p), round, true));
        }
        return base.toArray(AlgebraicFBEntry[]::new);
    }
    private static final class SNFSFactorBase {
        // Permanent rational factor base
        final int[] rationalPrimes;
        final int[] rationalRoots;
        final int[] rationalLogs;
        final long[] rationalReciprocals;

        // Permanent algebraic factor base
        final int[] algebraicPrimes;
        final int[] algebraicRoots;
        final int[] algebraicLogs;
        final long[] algebraicReciprocals;
        final boolean[] projective;

        final int rationalSize;
        final int algebraicSize;
        final int rationalBound;
        final int algebraicBound;

        // Derived configuration
        final int columnCount;
        final long rationalLargePrimeBound;
        final long algebraicLargePrimeBound;

        // Reusable sieve workspace
        int[] rationalScore = new int[0];
        int[] algebraicScore = new int[0];
        int[] candidateIndices = new int[0];

        // Sieve offsets for the current b
        final int[] rationalPositions;
        final int[] algebraicPositions;

        SNFSFactorBase(RationalFBEntry[] rationals, AlgebraicFBEntry[] algebraics) {
            rationalPrimes = new int[rationals.length];
            rationalPositions = new int[rationals.length];
            rationalLogs = new int[rationals.length];
            rationalRoots = new int[rationals.length];
            rationalReciprocals = new long[rationals.length];
            rationalSize = rationals.length;
            for (int k = 0; k < rationalSize; k++) {
                RationalFBEntry entry = rationals[k];
                rationalPrimes[k] = entry.prime();
                rationalLogs[k] = entry.log();
                rationalRoots[k] = entry.root();
                rationalReciprocals[k] = entry.reciprocal();
            }
            rationalBound = rationalPrimes[rationalSize-1];
            rationalLargePrimeBound = 100L*rationalBound;

            algebraicPrimes = new int[algebraics.length];
            algebraicRoots = new int[algebraics.length];
            algebraicReciprocals = new long[algebraics.length];
            algebraicLogs = new int[algebraics.length];
            algebraicPositions = new int[algebraics.length];
            projective = new boolean[algebraics.length];
            algebraicSize = algebraics.length;
            for (int k = 0; k < algebraicSize; k++) {
                AlgebraicFBEntry entry = algebraics[k];
                algebraicPrimes[k] = entry.prime();
                algebraicLogs[k] = entry.log();
                algebraicRoots[k] = entry.root();
                algebraicReciprocals[k] = entry.reciprocal();
                projective[k] = entry.projective();
            }
            algebraicBound = algebraicPrimes[algebraicSize-1];
            algebraicLargePrimeBound = 100L*algebraicBound;

            columnCount = 1 + rationalSize + algebraicSize;
        }
        void ensureSieveCapacity(int interval) {
            if (rationalScore.length < interval) {
                rationalScore = new int[interval];
                algebraicScore = new int[interval];
                candidateIndices = new int[interval];
            }
        }
    }
    private static long gcdSigned(long a, long b) {
        long A = Math.abs(a);
        long B = Math.abs(b);
        while(B != 0) {
            long temp = A % B;
            A = B;
            B = temp;
        }
        return A;
    }
    private static void lineSieve(int minA, int maxA, long maxB, SNFSFactorBase factorBase) {
        int interval = minA + maxA + 1;
        for (long b = 1; b <= maxB; b++) {
            Arrays.fill(factorBase.rationalScore, 0, interval, 0);
            Arrays.fill(factorBase.algebraicScore, 0, interval, 0);

            // Rational factor base
            for (int k = 0; k < factorBase.rationalSize; k++) {
                int p = factorBase.rationalPrimes[k];
                int start = Math.floorMod((b % p) * (factorBase.rationalRoots[k] % p) + minA, p);
                for (int i = start; i < interval; i += p)
                    factorBase.rationalScore[i] += factorBase.rationalLogs[k];
            }
            // Algebraic factor base
            int projectiveBonus = 0;
            for (int k = 0; k < factorBase.algebraicSize; k++) {
                int p = factorBase.algebraicPrimes[k];
                if (factorBase.projective[k]) {
                    if (b % p == 0)
                        projectiveBonus += factorBase.algebraicLogs[k];
                    continue;
                }
                int start = (int) Math.floorMod((b % p) * factorBase.algebraicRoots[k] + minA, (long) p);
                for (int i = start; i < interval; i += p)
                    factorBase.algebraicScore[i] += factorBase.algebraicLogs[k];
            }
            //Candidate evaluation
            for (int i = 0; i < interval; i++) {
                int a = i - minA;
                if (gcdSigned(a, b) != 1)
                    continue;
                int algebraicScore = factorBase.algebraicScore[i] + projectiveBonus;
                int rationalScore = factorBase.rationalScore[i];
            }
        }
    }
    private record SNFSCandidate(int a, int b, int rationalScore, int algebraicScore) {}
    private record SNFSRelation(int a, int b,
                                boolean rationalNegative,
                                int[] rationalIndices, int[] rationalExponents,
                                int[] algebraicIndices, int[] algebraicExponents) {}
    private static boolean acceptsSNFSCandidate(int rationalScore, int algebraicScore,
                                                int projectiveBonus,
                                                double rationalLogNorm, double algebraicLogNorm,
                                                double rationalLogLP, double algebraicLogLP,
                                                double rationalLambda, double algebraicLambda) {
        double rationalThreshold = LOG_SCALE * (rationalLogNorm - rationalLambda * rationalLogLP);
        double algebraicThreshold = LOG_SCALE * (algebraicLogNorm - algebraicLambda * algebraicLogLP);
        return rationalScore >= rationalThreshold && (long) algebraicScore + projectiveBonus >= algebraicThreshold;
    }
    private static long[] factorSNFS(long[] N, long[] original, int[] polynomial, long[] evalPoint) {
        if (!isZeroMagnitude(modMagnitudes(original, N)))
            throw new IllegalArgumentException("N must divide original");
        long[] check = ZERO_MAG;
        boolean negative = false;
        for (int k = polynomial.length - 1; k >= 0; k--) {
            check = multiplyMagnitudes(check, evalPoint);
            long c = polynomial[k]; // Widen before negation
            if (c == 0) continue;
            boolean cNegative = c < 0;
            long abs = cNegative ? -c : c;
            if (negative == cNegative)
                check = addSmall(check, abs);
            else {
                int cmp = compareSmall(check, abs);
                if (cmp >= 0)
                    check = subtractSmall(check, abs);
                else {
                    // check < abs, so check fits in one limb.
                    check = new long[]{abs - check[0]};
                    negative = cNegative;
                }
            }
            if (isZeroMagnitude(check))
                negative = false;
        }
        if (!isZeroMagnitude(modMagnitudes(check, N)))
            throw new IllegalArgumentException("Polynomial evaluation is not divisible by N");
        return ONE_MAG;
    }
}
