package com.AtomCAS.utilities;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Who are you
 */
public final class LargeInteger extends ScalarAtom {
    private static final long MAX_BITS = 1L << 37;
    private static final boolean PRINTING = true;
    private static void reportOverflow() {
        throw new ArithmeticException("Result exceeds supported range: |X| < 2^(2^37)");}
    /**
     * Little-endian limbs where one "digit" is in base 2^64. Any long primitive,
     * negative or positive, is a valid limb. Empty arrays are invalid
     */
    private final long[] limbs;
    /**
     * Sign of the integer. "True" means that the number is negative, "false"
     * otherwise. 0 is nonnegative, so negative is set to false for that number
     */
    private final boolean negative;
    /**
     * A special object that is used in all calculations that involve randomness
     */
    private static final SecureRandom SECURE_RNG = new SecureRandom();
    /**
     * Internal magnitude constants + zero and one checkers
     */
    private static final long[] ZERO_MAG = {0L};
    private static boolean isZeroMagnitude(long[] a) {return effectiveLength(a) == 1 && a[0] == 0L;}
    private static boolean isZeroArray(long[] a) {
        for(long e: a)
            if (e != 0)
                return false;
        return true;
    }
    private static final long[] ONE_MAG = {1L};
    private static boolean isOneMagnitude(long[] x) {return x.length == 1 && x[0] == 1;}
    private static final long[] TWO_MAG = {2L};
    private static final long[] THREE_MAG = {3L};
    private static final long[] FOUR_MAG = {4L};
    private static final long[] FIVE_MAG = {5L};
    private static final long[] SIX_MAG = {6L};
    private static final long[] SEVEN_MAG = {7L};
    private static final long[] EIGHT_MAG = {8L};
    private static final long[] NINE_MAG = {9L};
    private static final long[] TEN_MAG = {10L};
    private static final long[] HUNDRED_MAG = {100L};
    private static final long[] THOUSAND_MAG = {1000L};
    /**
     * LargeInteger constant -1
     */
    public static final LargeInteger NEGATIVE_ONE;
    /**
     * LargeInteger constant 0
     */
    public static final LargeInteger ZERO;
    /**
     * LargeInteger constant 1
     */
    public static final LargeInteger ONE;
    /**
     * LargeInteger constant 2
     */
    public static final LargeInteger TWO = new LargeInteger(TWO_MAG, false, true);
    /**
     * LargeInteger constant 3
     */
    public static final LargeInteger THREE = new LargeInteger(THREE_MAG, false, true);
    /**
     * LargeInteger constant 4
     */
    public static final LargeInteger FOUR = new LargeInteger(FOUR_MAG, false, true);
    /**
     * LargeInteger constant 5
     */
    public static final LargeInteger FIVE = new LargeInteger(FIVE_MAG, false, true);
    /**
     * LargeInteger constant 6
     */
    @SuppressWarnings("unused")
    public static final LargeInteger SIX = new LargeInteger(SIX_MAG, false, true);
    /**
     * LargeInteger constant 7
     */
    @SuppressWarnings("unused")
    public static final LargeInteger SEVEN = new LargeInteger(SEVEN_MAG, false, true);
    /**
     * LargeInteger constant 8
     */
    public static final LargeInteger EIGHT = new LargeInteger(EIGHT_MAG, false, true);
    /**
     * LargeInteger constant 9
     */
    @SuppressWarnings("unused")
    public static final LargeInteger NINE = new LargeInteger(NINE_MAG, false, true);
    /**
     * LargeInteger constant 10
     */
    public static final LargeInteger TEN = new LargeInteger(TEN_MAG, false, true);
    /**
     * LargeInteger constant 100
     */
    public static final LargeInteger HUNDRED = new LargeInteger(HUNDRED_MAG, false, true);
    /**
     * LargeInteger constant 1000
     */
    public static final LargeInteger THOUSAND = new LargeInteger(THOUSAND_MAG, false, true);
    /**
     * The default constructor for this class
     * @param limbs The array representing the limbs
     * @param sign  The negative of the number
     */
    public LargeInteger(long[] limbs, boolean sign) {this(limbs, sign, false);}
    private LargeInteger(long[] magArray, boolean sign, boolean trimmed) {
        if (magArray == null) throw new NullPointerException("Null inputs not permitted");
        if (magArray.length == 0)
            throw new IllegalArgumentException("limbs must be nonempty");
        if (!trimmed) limbs = trimLeadingZeros(magArray);
        else limbs = magArray;
        negative = sign && !isZeroMagnitude(magArray);
    }
    private static final int CACHE_COUNT = 31;
    private static final LargeInteger[] POS_SMALL = new LargeInteger[CACHE_COUNT];
    private static final LargeInteger[] NEG_SMALL = new LargeInteger[CACHE_COUNT];

    private static final int D = 2310;
    private static final int HALF_D = 1155;
    static {
        ZERO = new LargeInteger(ZERO_MAG, false, true);
        POS_SMALL[1] = new LargeInteger(ONE_MAG, false, true);
        ONE = POS_SMALL[1];
        NEG_SMALL[1] = new LargeInteger(ONE_MAG, true, true);
        NEGATIVE_ONE = NEG_SMALL[1];
        for (int x = 2; x < CACHE_COUNT; x++) {
            POS_SMALL[x] = new LargeInteger(new long[]{x}, false, true);
            NEG_SMALL[x] = new LargeInteger(new long[]{x}, true, true);
        }
    }
    public static LargeInteger valueOf(long x) {
        if (x == 0) return ZERO;
        else if (x > 0 && x < CACHE_COUNT) return POS_SMALL[(int)x];
        else if (x < 0 && x > -CACHE_COUNT) return NEG_SMALL[(int)-x];
        else if (x == 100) return HUNDRED;
        else if (x == 1000) return THOUSAND;
        boolean negative = x < 0;
        return new LargeInteger(new long[]{negative ? -x : x}, negative, true);
    }
    @SuppressWarnings("unused")
    public byte byteValue() {
        long v = longValue();
        if (v < Byte.MIN_VALUE || v > Byte.MAX_VALUE)
            throw new ArithmeticException("Does not fit in byte");
        return (byte) v;
    }
    @SuppressWarnings("unused")
    public short shortValue() {
        long v = longValue();
        if (v < Short.MIN_VALUE || v > Short.MAX_VALUE)
            throw new ArithmeticException("Does not fit in short");
        return (short) v;
    }
    public int intValue() {
        long v = longValue();
        if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE)
            throw new ArithmeticException("Does not fit in int");
        return (int) v;
    }
    public long longValue() {
        if (limbs.length > 1)
            throw new ArithmeticException("Does not fit in long");
        long value = limbs[0];
        if (value < 0) {
            if (negative && value == Long.MIN_VALUE)
                return Long.MIN_VALUE;
            throw new ArithmeticException("Does not fit in long");
        }
        return negative ? -value : value;
    }
    /**
     * @param s The string inputted into the constructor
     * @throws IllegalArgumentException if the inputted string is an expression that does not evaluate to an integer
     */
    public LargeInteger(String s) {
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
            }
            throw new IllegalArgumentException("Inputted expression does not evaluate to an integer");
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
    private static long[] trimLeadingZeros(long[] a) {
        int n = effectiveLength(a);
        if (n == a.length) return a;
        long[] out = new long[n];
        System.arraycopy(a, 0, out, 0, n);
        return out;
    }
    private static long bitLength(long[] x) {
        if (isZeroMagnitude(x)) return 0;
        int top = effectiveLength(x)-1;
        return top * 64L + 64L - Long.numberOfLeadingZeros(x[top]);
    }
    public long bitLength() {return bitLength(limbs);}
    private static int compareArray(long[] a, long[] b) {
        int aLen = effectiveLength(a), bLen = effectiveLength(b);
        if (aLen != bLen) return Integer.compare(aLen, bLen);
        for (int i = aLen - 1; i >= 0; i--) {
            int cmp = Long.compareUnsigned(a[i], b[i]);
            if (cmp != 0) return cmp;
        }
        return 0;
    }
    private static int compareSmall(long[] a, long b) {
        if (effectiveLength(a) > 1) return 1;
        return Long.compareUnsigned(a[0], b);
    }
    private static boolean equalMagnitudes(long[] a, long[] b) {
        if (a == b) return true;
        int aLen = effectiveLength(a), bLen = effectiveLength(b);
        if (aLen != bLen) return false;
        for (int i = 0; i < aLen; i++)
            if (a[i] != b[i]) return false;
        return true;
    }
    @Override
    public int hashCode() {
        int h = 0;
        for (int i = limbs.length - 1; i >= 0; i--)
            h = 31 * h + Long.hashCode(limbs[i]);
        return isNegative() ? -h : h;
    }
    @Override
    public LargeInteger clone() {return new LargeInteger(limbs.clone(), negative, true);}
    private static String magnitudeToString(long[] mag) {
        if (mag.length == 1) return Long.toUnsignedString(mag[0]);
        long m = (bitLength(mag) * 1233L >>> 12) >>> 1;
        long[][] DR = divideMagnitudes(mag, power(TEN_MAG, m));
        StringBuilder sb = new StringBuilder();
        sb.append(magnitudeToString(DR[0]));
        String right = magnitudeToString(DR[1]);
        return sb.repeat('0', Math.toIntExact(Math.max(m-right.length(), 0))).append(right).toString();
    }
    @Override
    public String toString() {
        int chars = Math.toIntExact((bitLength() * 1233L >>> 12) + 1 + (negative ? 1 : 0));
        StringBuilder sb = new StringBuilder(chars);
        if (negative) sb.append('-');
        return sb.append(magnitudeToString(limbs)).toString();
    }
    public String toLatex() {return toString();}
    public BigInteger toBigInteger() {
        if (bitLength() > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Value exceeds BigInteger's supported range");
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
        return negative ? new BigInteger(-1, bytes) : new BigInteger(1, bytes);
    }
    @Override
    public boolean equals(Atom other) {
        if (this == other) return true;
        else if (!(other instanceof ScalarAtom)) return false;
        return switch (other) {
            case LargeInteger I -> equalMagnitudes(limbs, I.limbs) && negative == I.negative;
            case LargeFraction F -> equals(F.numerator) && F.denominator.isOne();
            case LargePowerMonomial LPM -> LPM.exponent2.isOne() == negative &&
                    LPM.base.equals(this) && LPM.exponent.isOne();
            case RadicalTerm RT -> RT.rationalPart.equals(this) && RT.exponent.isZero();
            default -> compareTo((ScalarAtom) other) == 0;
        };
    }
    @Override
    public boolean isZero() {return isZeroMagnitude(limbs);}
    @Override
    public boolean isOne() {return isOneMagnitude(limbs) && !negative;}
    public LargeInteger sign() {return isNegative() ? NEGATIVE_ONE : (isZero() ? ZERO : ONE);}
    public boolean isNegative() {return negative;}
    public LargeInteger negate() {return new LargeInteger(limbs, !negative, true);}
    @Override
    public LargeInteger realPart() {return this;}
    @Override
    public LargeInteger imagPart() {return ZERO;}
    @Override
    public LargeInteger abs() {
        if (!negative) return this;
        return new LargeInteger(limbs, false, true);
    }
    @Override
    public LargeInteger floor() {return this;}
    @Override
    public LargeInteger ceil() {return this;}
    private long[] toTwosComplement(int len) {
        long[] out = new long[len];
        System.arraycopy(limbs, 0, out, 0, Math.min(limbs.length, len));
        if (!negative) return out;
        for (int i = 0; i < len; i++) out[i] = ~out[i];
        long carry = 1L;
        for (int i = 0; i < len && carry != 0L; i++) {
            long old = out[i];
            out[i] = old + carry;
            carry = Long.compareUnsigned(out[i], old) < 0 ? 1L : 0L;
        }
        return out;
    }
    private static LargeInteger fromTwosComplement(long[] tc) {
        boolean negative = tc[tc.length - 1] < 0;
        if (!negative)
            return new LargeInteger(tc, false);
        // Negative: abs = -(TWO's complement value)
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
    public LargeInteger not() {return negate().subtract(1);}
    @SuppressWarnings("unused")
    public LargeInteger and(LargeInteger other) {
        int len = Math.max(limbs.length, other.limbs.length) + 1;
        long[] a = toTwosComplement(len);
        long[] b = other.toTwosComplement(len);
        long[] out = new long[len];
        for (int i = 0; i < len; i++)
            out[i] = a[i] & b[i];
        return fromTwosComplement(out);
    }
    @SuppressWarnings("unused")
    public LargeInteger or(LargeInteger other) {
        int len = Math.max(limbs.length, other.limbs.length) + 1;
        long[] a = toTwosComplement(len);
        long[] b = other.toTwosComplement(len);
        long[] out = new long[len];
        for (int i = 0; i < len; i++)
            out[i] = a[i] | b[i];
        return fromTwosComplement(out);
    }
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
    private static long[] randomMagnitudeBetween(long[] startInclusive, long[] endExclusive) {
        startInclusive = trimLeadingZeros(startInclusive);
        endExclusive = trimLeadingZeros(endExclusive);

        if (compareArray(startInclusive, endExclusive) >= 0)
            throw new IllegalArgumentException("Invalid random magnitude range");

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
     * Generates a random integer within a specified range [start, end)
     */
    @SuppressWarnings("unused")
    public static LargeInteger random(LargeInteger start, LargeInteger end) {
        if (start.negative) return random(ZERO, end.subtract(start)).add(start);
        return new LargeInteger(randomMagnitudeBetween(start.limbs, end.limbs), false, true);
    }
    public static LargeInteger randomChunks(int chunks) {
        if (chunks < 0)
            throw new IllegalArgumentException("Chunks must be nonnegative");
        else if (chunks == 0) return ZERO;
        long[] res = new long[chunks];
        for (int i = 0; i < chunks; i++) res[i] = SECURE_RNG.nextLong();
        while (res[res.length - 1] == 0) res[res.length - 1] = SECURE_RNG.nextLong();
        return new LargeInteger(res, false, true);
    }
    public static LargeInteger randomBits(long bits) {
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
        return new LargeInteger(value, false, true);
    }
    private static int effectiveLength(long[] mag) {
        int len = mag.length;
        while (len > 1 && mag[len-1] == 0) len--;
        return len;
    }
    @Override
    public int compareMagnitudes(ScalarAtom other) {
        return switch (other) {
            case LargeInteger I -> compareArray(limbs, I.limbs);
            case LargeFraction F -> multiply(F.denominator).compareMagnitudes(F.numerator);
            case LargePowerMonomial L -> {
                LargeFraction f = power(L.exponent.denominator);
                LargeFraction o = L.power(L.exponent.denominator).base;
                yield f.compareMagnitudes(o);
            } case RadicalTerm R -> {
                if (other.isNotReal()) {
                    RadicalTermSum RS = (RadicalTermSum) other.multiply(R.complexConjugate());
                    yield square().compareMagnitudes(RS);
                }
                if (equals(R.rationalPart) && R.coefficient.equals(LargeInteger.ZERO)) yield 0;
                LargeFraction f = (LargeFraction) subtract(R.rationalPart).divide(R.coefficient);
                f = f.power(R.exponent.exponent.denominator);
                LargeFraction o = R.exponent.base;
                yield f.compareMagnitudes(o);
            } default -> -other.compareMagnitudes(this);
        };
    }
    @Override
    public int compareTo(ScalarAtom other) {
        if (other.isNotReal()) throw new IllegalStateException("Complex numbers cannot be compared");
        return switch (other) {
            case LargeInteger I -> {
                if (negative == I.negative)
                    yield switch (negative) {
                        case false -> compareArray(limbs, I.limbs);
                        case true -> compareArray(I.limbs, limbs);
                    };
                else yield !negative ? 1 : -1;
            } case LargeFraction F -> multiply(F.denominator).compareTo(F.numerator);
            case LargePowerMonomial LPM -> {
                LargeFraction f = power(LPM.exponent.denominator);
                LargeFraction o = LPM.power(LPM.exponent.denominator).base;
                int res = f.compareMagnitudes(o);
                if (res == 1) {
                    if (negative) yield -1;
                    yield res;
                } else if (res == -1) {
                    if (other.lessThan(ZERO)) yield 1;
                    yield res;
                }
                yield res;
            } default -> {
                int B = compareMagnitudes(other);
                if (B == 1) {
                    if (negative) yield -1;
                    yield B;
                } else if (B == -1) {
                    if (other.lessThan(ZERO)) yield 1;
                    yield B;
                }
                yield B;
            }
        };
    }
    @Override
    public boolean isRational() {return true;}
    @Override
    public boolean isReal() {return true;}
    @Override
    public boolean isAlgebraic() {return true;}
    private static long divide128ByNormalized64Quotient(long high, long low, long divisor) {
        // divisor normalized, high < divisor unsigned
        if (Long.compareUnsigned(high, divisor) >= 0)
            throw new IllegalArgumentException("divide128ByNormalized64 requires high < divisor");
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
        long firstLow = ((u2 & U32_MASK) << 32) |(u1 & U32_MASK);
        long rem = firstLow - qHigh * divisor;

        /*
         * Bring down u0 and get second quotient word.
         */
        long qLow = divideThreeByTwo((int) (rem >>> 32), (int) rem, u0, v1, v0);
        return (qHigh << 32) | qLow;
    }
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
        if (borrow != 0)
            throw new ArithmeticException("subtractMagnitudes underflow");
        return trimLeadingZeros(out);
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
                    return trimLeadingZeros(out);
                return out;
            }
            out[i] = -1L;
        }
        return trimLeadingZeros(out);
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
            throw new AssertionError("subtractMagnitudeInPlace underflow");
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
            case LargePowerMonomial LPM -> new RadicalTerm(new LargeFraction(this),
                    LargeFraction.ONE, LPM);
            case RadicalTerm RT -> new RadicalTerm((LargeFraction) add(RT.rationalPart), RT.coefficient,
                    RT.exponent);
            case RadicalTermSum RTS -> new RadicalTermSum(RTS.sum, (LargeFraction) RTS.constant.add(this));
            case LargePowerNode LPN -> new RadicalTermNode(new LargeFraction(this),
                    LargeFraction.ONE, LPN);
            default -> throw new UnsupportedOperationException(
                    "Operation with these types together not supported yet");
        };
    }
    LargeInteger add(long other) {
        if (other == 0L) return this;
        boolean otherSign = other < 0;
        // Caveat: Long.MIN_VALUE needs special handling because -Long.MIN_VALUE overflows.
        if (other == Long.MIN_VALUE)
            return add(valueOf(other));
        long otherAbs = Math.abs(other);
        if (negative == otherSign)
            return new LargeInteger(addSmall(limbs, otherAbs), negative, true);
        int cmp = compareSmall(limbs, otherAbs);
        if (cmp == 0) return ZERO;
        else if (cmp > 0)
            return new LargeInteger(subtractSmall(limbs, otherAbs), negative, true);
        return new LargeInteger(subtractMagnitudes(new long[]{otherAbs}, limbs), otherSign, true);
    }
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
    LargeInteger subtract(long other) {return add(-other);}
    private static long[] shiftLeftLimbs(long[] x, int limbs) {
        if (isZeroMagnitude(x)) return ZERO_MAG;
        if (limbs == 0) return x;
        if (limbs < 0) return shiftRightLimbs(x, -limbs);
        long[] out = new long[x.length + limbs];
        // pad zeros at the beginning
        System.arraycopy(x, 0, out, limbs, x.length);
        return trimLeadingZeros(out);
    }
    private static long[] shiftLeftBits(long[] n, long shift) {
        if (isZeroMagnitude(n)) return ZERO_MAG;
        if (shift < 0) return shiftRightBits(n, -shift);
        if (shift == 0) return trimLeadingZeros(n);
        if (bitLength(n) + shift > MAX_BITS) reportOverflow();
        int limbShift = (int) shift >>> 6;
        int bitShift = (int) shift & 63;
        long[] out = new long[n.length + limbShift + 1];
        if (bitShift == 0) {
            System.arraycopy(n, 0, out, limbShift, n.length);
            return trimLeadingZeros(out);
        }
        long carry = 0;
        for (int i = 0; i < n.length; i++) {
            long cur = n[i];
            out[i + limbShift] = (cur << bitShift) | carry;
            carry = cur >>> (64 - bitShift);
        }
        out[n.length + limbShift] = carry;
        return trimLeadingZeros(out);
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
        return trimLeadingZeros(out);
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
        return trimLeadingZeros(out);
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
                    trimLeadingZeros(a).length + ", bLen=" + bt.length + ", shiftedLen=" + shifted.length);
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
    private static long[] slice(long[] a, int from, int to) {
        if (to >= a.length) to = a.length;
        if (from < 0) from = 0;
        if (from > to) throw new IllegalArgumentException("Illegal slice range " + from + " to " + to);
        long[] out = new long[to - from];
        System.arraycopy(a, from, out, 0, to - from);
        return out;
    }
    private static long[] sliceFixed(long[] a, int from, int length) {
        if (from < 0 || length < 0)
            throw new IllegalArgumentException("Negative slice");
        long[] out = new long[length];
        if (from >= a.length) return out;
        int amount = Math.min(length, a.length - from);
        System.arraycopy(a, from, out, 0, amount);
        return out;
    }
    /**
     * Returns a LargeInteger whose value is {@code this << shift}
     */
    public LargeInteger shiftLeft(long shift) {
        if (shift == 0) return this;
        return new LargeInteger(shiftLeftBits(limbs, shift), negative, true);
    }
    /**
     * Returns a LargeInteger whose value is {@code this >> shift}
     */
    public LargeInteger shiftRight(long shift) {
        if (shift == 0) return this;
        return new LargeInteger(shiftRightBits(limbs, shift), negative, true);
    }
    private static boolean testBit(long[] x, long bit) {
        long limb = bit >>> 6;
        int offset = (int)(bit & 63);
        if (limb >= x.length) return false;
        return ((x[(int)limb] >>> offset) & 1L) != 0L;
    }
    /**
     * Tests a specified bit to see if it is set
     * @param bit The specified bit tested
     * @return A boolean indicated if the bit is set
     */
    public boolean testBit(long bit) {return testBit(limbs, bit);}
    public LargeInteger setBit(long bit) {
        if (bit < 0)
            throw new ArithmeticException("Negative bit address");
        if (testBit(bit))
            return this;

        long limbIndex = bit >>> 6;
        if (limbIndex >= Integer.MAX_VALUE)
            reportOverflow();

        int index = (int) limbIndex;
        long[] out = Arrays.copyOf(limbs, Math.max(limbs.length, index + 1));
        out[index] |= 1L << (bit & 63);
        return new LargeInteger(out, negative, true);
    }
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
    @SuppressWarnings("unused")
    public LargeInteger flipBit(long bit) {
        if (bit < 0)
            throw new ArithmeticException("Negative bit address");

        long limbIndex = bit >>> 6;
        if (limbIndex >= Integer.MAX_VALUE)
            reportOverflow();

        int index = (int) limbIndex;
        long[] out = Arrays.copyOf(limbs, Math.max(limbs.length, index + 1));
        out[index] ^= 1L << (bit & 63);
        return new LargeInteger(out, negative);
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
    private static long[] multiplySmall(long[] a, long m) {
        if (m == 1L)
            return a;
        if (m == 0L || isZeroMagnitude(a))
            return ZERO_MAG;
        long[] out = new long[a.length + 1];
        multiplySmallInto(a, m, out);
        return trimLeadingZeros(out);
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
    private static long[] multiplyMagnitudes(long[] a, long[] b) {
        return multiplyMagnitudesWithThresholds(a, b, KARATSUBA_THRESHOLD, TOOM_COOK_3_THRESHOLD,
                TOOM_COOK_4_THRESHOLD);
    }
    private static void multiplyInto(long[] a, long[] b, long[] out) {
        Arrays.fill(out, 0);
        if (out.length == 0 || isZeroMagnitude(a) || isZeroMagnitude(b)) return;
        // Put the longer operand in A.
        if (a.length < b.length) {
            long[] t = a;
            a = b;
            b = t;
        }
        // Any b[k] with k >= out.length contributes only beyond the buffer.
        int bLimit = Math.min(b.length, out.length);
        for (int k = 0; k < bLimit; k++) {
            long scalar = b[k];
            if (scalar == 0L) continue;
            long carry = 0L;
            // Only terms satisfying k + i < out.length matter.
            int aLimit = Math.min(a.length, out.length - k);
            int oi = k;
            for (int i = 0; i < aLimit; i++) {
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
    }
    private static long[] multiplyMagnitudesWithThresholds(long[] a, long[] b, int KT, int T3T, int T4T) {
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);
        if (isZeroMagnitude(a) || isZeroMagnitude(b)) return ZERO_MAG;
        if (a.length < b.length) {
            long[] t = a;
            a = b;
            b = t;
        }
        int max = a.length;
        int min = b.length;
        long[] out = new long[max + min];
        if (min == 1) return multiplySmall(a, b[0]);
        else if (min < KT) {
            multiplyInto(a, b, out);
            return trimLeadingZeros(out);
        } else if (min < T3T) {
            /*
              Karatsuba multiplication

              With B = 2^64 and m = pivot:

                a = a1 * B^(m) + a0
                b = b1 * B^(m) + b0
              Compute:
                z0 = a0*b0
                z2 = a1*b1
                z1 = (a0+a1)(b0+b1) - z0 - z2
              Then:
                a*b = z0 + z1 * B^(m) + z2*B^(2m)
              This replaces four half-size products with three,
              giving O(n^log2(3)) ≈ O(n^1.585) asymptotically.
             */
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
            return trimLeadingZeros(out);
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

            v0 = trimLeadingZeros(v0);
            s = trimLeadingZeros(s);
            r2 = trimLeadingZeros(r2);
            v2 = trimLeadingZeros(v2);
            vInf = trimLeadingZeros(vInf);
            addMagnitudeInPlace(out, v0, 0);
            addMagnitudeInPlace(out, s, pivot);
            addMagnitudeInPlace(out, r2, 2 * pivot);
            addMagnitudeInPlace(out, v2, 3 * pivot);
            addMagnitudeInPlace(out, vInf, 4 * pivot);
            return trimLeadingZeros(out);
        }
        //split into four parts
        int pivot = (max + 3) >>> 2;
        long[] a0 = sliceFixed(a, 0, pivot);
        long[] a1 = sliceFixed(a, pivot, pivot);
        long[] a2 = sliceFixed(a, 2*pivot, pivot);
        long[] a3 = sliceFixed(a, 3*pivot, pivot);
        long[] b0 = sliceFixed(b, 0, pivot);
        long[] b1 = sliceFixed(b, pivot, pivot);
        long[] b2 = sliceFixed(b, 2*pivot, pivot);
        long[] b3 = sliceFixed(b, 3*pivot, pivot);
        //point evaluation. points 0, -1, 1, -2, 2, 3, ∞
        long[] p1 = addFourMagnitudes(a0, a1, a2, a3);
        long[] p2 = addFourMagnitudes(a0, shiftLeftBits(a1, 1), shiftLeftBits(a2, 2),
                shiftLeftBits(a3, 3));
        long[] p3 = addFourMagnitudes(a0, multiplySmall(a1, 3), multiplySmall(a2, 9),
                multiplySmall(a3, 27));
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
        long[] q2 = addFourMagnitudes(b0, shiftLeftBits(b1, 1), shiftLeftBits(b2, 2),
                shiftLeftBits(b3, 3));
        long[] q3 = addFourMagnitudes(b0, multiplySmall(b1, 3), multiplySmall(b2, 9),
                multiplySmall(b3, 27));
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
        addMagnitudeInPlace(out, v0,     0);
        addMagnitudeInPlace(out, vM1Magnitude, pivot);
        addMagnitudeInPlace(out, v1,     2*pivot);
        addMagnitudeInPlace(out, vM2Magnitude, 3*pivot);
        addMagnitudeInPlace(out, v2,     4*pivot);
        addMagnitudeInPlace(out, v3,     5*pivot);
        addMagnitudeInPlace(out, vInf,   6*pivot);
        return trimLeadingZeros(out);
    }
    public LargeInteger multiply(LargeInteger other) {
        //if (equals(other)) return square();
        return new LargeInteger(
                multiplyMagnitudes(limbs, other.limbs), negative ^ other.negative, true);
    }
    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        return switch (other) {
            case LargeInteger I -> multiply(I);
            case LargeFraction F -> new LargeFraction(multiply(F.numerator), F.denominator);
            case LargePowerMonomial largePowerMonomial -> new LargePowerMonomial(
                    new LargeFraction(this), LargeFraction.ONE).multiply(largePowerMonomial);
            case RadicalTermSum RTS -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                for (LargePowerMonomial L : RTS.sum.keys())
                    rt.add(new RadicalTerm(LargeFraction.ZERO, (LargeFraction) multiply(RTS.sum.get(L)), L));
                yield new RadicalTermSum(rt, (LargeFraction) multiply(RTS.constant));
            } case LargePowerNode LPN -> new LargePowerNode(LPN.base.multiply(power(LPN.exponent.reciprocal())),
                    LPN.exponent, LPN.signFactor);
            default -> other.multiply(this);
        };
    }
    LargeInteger multiply(long greg) {
        return new LargeInteger(
                multiplySmall(limbs, Math.abs(greg)), negative ^ (greg < 0), true);}

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
     * Threshold for switching from grade-school to Karatsuba in squaring. If the number of limbs
     * in the smaller operand is greater than or equal to this, then the Toom-Cook 4 algorithm will
     * be chosen. This value was found via threshold benchmarks to work well.
     */
    private static final int TOOM_4_SQUARE_THRESHOLD = 520;
    private static long[] squareMagnitude(long[] a) {
        return squareMagnitudeWithThresholds(a, KARATSUBA_SQUARE_THRESHOLD, TOOM_3_SQUARE_THRESHOLD,
                TOOM_4_SQUARE_THRESHOLD);
    }
    private static void squareInto(long[] a, long[] out) {
        int n = a.length;
        int m = Math.min(out.length, n << 1);
        if (m == 0) return;
        // If out is larger than the full product, clear the unused tail.
        if (out.length > (n << 1))
            Arrays.fill(out, n << 1, out.length, 0L);
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
            // Any carry beyond out.length is deliberately discarded.
            j += i;
            while (carry != 0L && j < m) {
                long old = out[j];
                long sum = old + carry;
                out[j] = sum;
                carry = Long.compareUnsigned(sum, old) < 0 ? 1L : 0L;
                j++;
            }
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
    }
    private static long[] squareMagnitudeWithThresholds(long[] a, int KT, int T3T, int T4T) {
        if (isZeroMagnitude(a)) return a;
        int n = a.length;
        if (n == 1) {
            long a0 = a[0];
            long low = a0*a0;
            long high = Math.unsignedMultiplyHigh(a0, a0);
            return high == 0 ? new long[]{low} : new long[]{low, high};
        }
        long[] out = new long[n << 1];
        if (n < KT) {
            //Grade-school
            squareInto(a, out);
            return trimLeadingZeros(out);
        } else if (n < T3T) {
            //Karatsuba
            int m = (n + 1) >>> 1;
            long[] x0 = sliceFixed(a, 0, m);
            long[] x1 = slice(a, m, n);
            long[] z0 = squareMagnitudeWithThresholds(x0, KT, T3T, T4T);
            long[] z2 = squareMagnitudeWithThresholds(x1, KT, T3T, T4T);
            long[] z1 = squareMagnitudeWithThresholds(addMagnitudes(x0, x1), KT, T3T, T4T);
            subtractMagnitudeInPlace(z1, z0);
            subtractMagnitudeInPlace(z1, z2);
            addMagnitudeInPlace(out, z0, 0);
            addMagnitudeInPlace(out, z1, m);
            addMagnitudeInPlace(out, z2, 2 * m);
            return trimLeadingZeros(out);
        } else if (n < T4T) {
            int m = (n + 2) / 3;
            //a = a2*B^2 + a1*B + a0
            long[] a0 = sliceFixed(a, 0, m);
            long[] a1 = sliceFixed(a, m, m);
            long[] a2 = slice(a, m * 2, n);
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
            subtractShiftedLeftBitsInPlace(v2, vInf, 4, "");
            shiftRightBitsInPlace(v2, 1);

            // t -> r3
            subtractMagnitudeInPlace(v2, s);
            subtractShiftedLeftBitsInPlace(v2, v1, 1, "");
            divideMagnitudeExactInPlace(v2, 3);
            // s -> r1
            subtractMagnitudeInPlace(s, v2);
            //recombine
            addMagnitudeInPlace(out, v0, 0);
            addMagnitudeInPlace(out, s, m);
            addMagnitudeInPlace(out, v1, 2 * m);
            addMagnitudeInPlace(out, v2, 3 * m);
            addMagnitudeInPlace(out, vInf, 4 * m);
            return trimLeadingZeros(out);
        }
        //initialize
        int pivot = (n + 3) >>> 2;
        long[] a0 = sliceFixed(a, 0, pivot);
        long[] a1 = sliceFixed(a, pivot, pivot);
        long[] a2 = sliceFixed(a, 2*pivot, pivot);
        long[] a3 = sliceFixed(a, 3*pivot, pivot);
        //define points
        long[] p1 = addFourMagnitudes(a0, a1, a2, a3);
        long[] p2 = addFourMagnitudes(
                shiftLeftBits(a3, 3), shiftLeftBits(a2, 2), shiftLeftBits(a1, 1), a0);
        long[] p3 = addFourMagnitudes(
                multiplySmall(a3, 27),  multiplySmall(a2, 9), multiplySmall(a1, 3), a0);
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
        //multiply
        long[] v0 = squareMagnitudeWithThresholds(a0, KT, T3T, T4T);
        long[] v1 = squareMagnitudeWithThresholds(p1, KT, T3T, T4T);
        long[] v2 = squareMagnitudeWithThresholds(p2, KT, T3T, T4T);
        long[] v3 = squareMagnitudeWithThresholds(p3, KT, T3T, T4T);
        long[] vM1 = squareMagnitudeWithThresholds(pM1, KT, T3T, T4T);
        long[] vM2 = squareMagnitudeWithThresholds(pM2, KT, T3T, T4T);
        long[] vInf = squareMagnitudeWithThresholds(a3, KT, T3T, T4T);
        //interpolate. start with E1 = r2 + r4
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
        addMagnitudeInPlace(out, E1, 2*pivot);
        addMagnitudeInPlace(out, v2, 3*pivot);
        addMagnitudeInPlace(out, E2, 4*pivot);
        addMagnitudeInPlace(out, v3, 5*pivot);
        addMagnitudeInPlace(out, vInf, 6*pivot);
        return trimLeadingZeros(out);
    }
    private static long[] cubeMagnitude(long[] x) {return multiplyMagnitudes(squareMagnitude(x), x);}
    @Override
    public LargeInteger square() {
        return new LargeInteger(squareMagnitude(limbs), false, true);}
    private static SqrtResult sqrtFloorMagnitude(long[] n) {
        n = trimLeadingZeros(n);
        if (isZeroMagnitude(n)) return new SqrtResult(ZERO_MAG, ZERO_MAG);
        long bits = bitLength(n);
        int pairs = (int) (bits + 1) >>> 1;
        long[] root = ZERO_MAG;
        long[] rem = ZERO_MAG;
        for (int pair = pairs - 1; pair >= 0; pair--) {
            // rem = rem * 4 + next two bits of n
            rem = shiftLeftBits(rem, 2);
            int lowBit = pair << 1;
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
        return new SqrtResult(trimLeadingZeros(root), trimLeadingZeros(rem));
    }
    private record SqrtResult(long[] sqrt, long[] remainder) {}
    private static long[] sqrtCeilMagnitude(long[] mag) {
        SqrtResult result = sqrtFloorMagnitude(mag);
        if (isZeroMagnitude(result.remainder)) return result.sqrt;
        return addSmall(result.sqrt, 1);
    }
    private static long[] nthRootFloorMagnitude(long[] n, long exp) {
        if (exp <= 0) throw new IllegalArgumentException("exp must be positive");
        n = trimLeadingZeros(n);
        if (isZeroMagnitude(n) || isOneMagnitude(n) || exp == 1) return n;
        if (exp == 2) return sqrtFloorMagnitude(n).sqrt;
        long bits = bitLength(n);
        long rootBits = (bits + exp - 1) / exp;
        // If exp is already too large, root must be 1.
        if (exp >= bits) return ONE_MAG;
        long[] x = shiftLeftBits(ONE_MAG, rootBits);
        while (true) {
            long[] xPow = powerLimited(x, exp - 1, n);
            /*
             * x is an upper estimate, so normally x^(exp-1) <= n
             * once we're reasonably close. If the limited-power routine
             * reports overflow relative to n, quotient is simply 0.
             */
            long[] quotient;
            if (xPow == null) quotient = ZERO_MAG;
            else quotient = divideMagnitudes(n, xPow)[0];
            long[] lhs = multiplySmall(x, exp - 1);
            long[] next = divideByLong(addMagnitudes(lhs, quotient), exp).quotient;
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
    public LargeInteger cube() {return new LargeInteger(cubeMagnitude(limbs), negative, true);}
    public static LargeInteger productRange(LargeInteger left, LargeInteger right) {
        if (left.equals(right)) return left;
        if (right.subtract(left).isOne()) return left.multiply(right);
        LargeInteger mid = left.add(right).shiftRight(1);
        return productRange(left, mid).multiply(productRange(mid.add(1), right));
    }
    public LargeInteger factorial() {
        if (negative) throw new ArithmeticException("Factorial undefined for negative integers");
        if (isZero() || isOne()) return ONE;
        return productRange(ONE, this);
    }
    public LargeInteger nCr(LargeInteger l) {
        if (lessThan(l)) return ZERO;
        else if (l.isZero()) return ONE;
        return productRange(subtract(l.add(1)), this).divide(l.factorial());
    }
    @SuppressWarnings("unused")
    public LargeFraction nPr(LargeInteger l) {
        LargeInteger temp = subtract(l);
        if (temp.negative) return LargeFraction.ZERO;
        return new LargeFraction(factorial(), temp.factorial());
    }
    @SuppressWarnings("unused")
    public LargeInteger triangular() {return multiply(add(1)).shiftRight(1);}
    @SuppressWarnings("unused")
    public LargeInteger tetrahedral() {return add(2).nCr(THREE);}
    @SuppressWarnings("unused")
    public static LargeInteger figurate(LargeInteger l, LargeInteger k) {
        return k.subtract(1).add(l).nCr(k);}
    private static final long U32_MASK = 0xFFFF_FFFFL;
    private static final long U32_BASE = 1L << 32;
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
            rHat = Long.remainderUnsigned(top, B1);
        }
        long qTimesB2 = q * B2;
        while (rHat < U32_BASE) {
            long right = (rHat << 32) | A3;
            if (Long.compareUnsigned(qTimesB2, right) <= 0) break;
            q--;
            rHat += B1;
            qTimesB2 -= B2;
        }
        return q;
    }
    private static DivRem divideByLong(long[] a, long divisor) {
        if (divisor == 0)
            throw new ArithmeticException("Division by zero");
        long[] q = new long[a.length];
        long rem = 0L;
        // These depend only on divisor: do them once.
        int shift = Long.numberOfLeadingZeros(divisor);
        long d = shift == 0 ? divisor : divisor << shift;
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
             *
             * [u3 u2 u1] / [v1 v0]
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
            long low64 = ((u2 & U32_MASK) << 32) | (u1 & U32_MASK);
            long r1 = low64 - qHigh * d;
            /*
             * Second:
             *
             * [rHigh32 rLow32 u0] / [v1 v0]
             */
            int rHigh32 = (int) (r1 >>> 32);
            int rLow32 = (int) r1;
            long qLow = divideThreeByTwo(rHigh32, rLow32, u0, v1, v0);
            /*
             * Exact second remainder.
             *
             * Low 64 bits of the dividend are [rLow32 u0].
             */
            long low64Second = ((rLow32 & U32_MASK) << 32) | (u0 & U32_MASK);
            long normalizedRem = low64Second - qLow * d;
            q[i] = (qHigh << 32) | qLow;
            rem = normalizedRem >>> shift;
        }
        return new DivRem(trimLeadingZeros(q), rem);
    }
    private record DivRem(long[] quotient, long remainder) {}
    private static void divideMagnitudeExactInPlace(long[] a, long divisor) {
        if (divisor == 0L)
            throw new ArithmeticException("divide by zero");
        // Treat divisor as unsigned.
        // divisor = 2^shift * odd
        int shift = Long.numberOfTrailingZeros(divisor);
        if (shift != 0) {
            // Exact divisibility by 2^shift requires these bits to be zero.
            long mask = (1L << shift) - 1L;
            if ((a[0] & mask) != 0L)
                throw new ArithmeticException(new LargeInteger(a, false, true) +
                        " is not exactly divisible by " + Long.toUnsignedString(divisor));
            // Divide magnitude by 2^shift in place.
            long carry = 0L;
            for (int i = a.length - 1; i >= 0; i--) {
                long x = a[i];
                a[i] = (x >>> shift) | carry;
                carry = x << (64 - shift);
            }
            divisor >>>= shift;
            // Original divisor was a power of two.
            if (divisor == 1L) return;
        }
        // divisor is now odd.
        // Starting with x = d is actually already correct modulo 8,
        // because every odd d satisfies d*d ≡ 1 (mod 8).
        long inverse = divisor;
        // Newton/Hensel iteration:
        // x <- x(2 - d*x)
        // Correct bits roughly double each iteration:
        // 3 -> 6 -> 12 -> 24 -> 48 -> 96
        for (int k = 0; k < 5; k++)
            inverse *= 2L - divisor * inverse;
        long carry = 0L;
        for (int i = 0; i < a.length; i++) {
            long x = a[i];
            // divisor*q + carry ≡ x (mod 2^64)
            long q = (x - carry) * inverse;
            long low = q * divisor;
            long high = Math.unsignedMultiplyHigh(q, divisor);
            long sum = low + carry;
            if (Long.compareUnsigned(sum, low) < 0) high++;
            a[i] = q;
            carry = high;
        }
        if (carry != 0L)
            throw new ArithmeticException("not exactly divisible by " + Long.toUnsignedString(divisor));
    }
    private static long[][] divideMagnitudes(long[] dividend, long[] divisor) {
        dividend = trimLeadingZeros(dividend);
        divisor  = trimLeadingZeros(divisor);
        if (isZeroMagnitude(divisor))
            throw new ArithmeticException("Division by zero");
        int cmp = compareArray(dividend, divisor);
        if (cmp < 0)
            return new long[][]{ZERO_MAG, dividend};
        else if (cmp == 0)
            return new long[][]{ONE_MAG, ZERO_MAG};
        if (divisor.length == 1) {
            DivRem DR = divideByLong(dividend, divisor[0]);
            return new long[][]{DR.quotient, new long[]{DR.remainder}};
        }
        long[][] results;
        if (divisor.length < BURNIKEL_ZIEGLER_BLOCK_THRESHOLD || dividend.length-divisor.length
                < BURNIKEL_ZIEGLER_OFFSET)
            results = divideMagnitudesKnuth(dividend, divisor);
        else results = divideMagnitudesBurnikelZiegler(dividend, divisor);
        if (VERIFY_DIVISION) {
            if (compareArray(results[1], divisor) >= 0)
                throw new AssertionError("BZ division: remainder >= divisor");
            long[] reconstructed = addMagnitudes(multiplyMagnitudes(results[0], divisor), results[1]);
            if (!equalMagnitudes(reconstructed, dividend))
                throw new AssertionError("BZ division: q*d+r != n");
        }
        return results;
    }
    private static long[][] divideMagnitudesKnuth(long[] dividend, long[] divisor) {
        if (isZeroMagnitude(divisor))
            throw new ArithmeticException("Division by zero");
        int cmp = compareArray(dividend, divisor);
        if (cmp < 0)
            return new long[][]{ZERO_MAG, dividend};
        if (cmp == 0)
            return new long[][]{ONE_MAG, ZERO_MAG};
        int n = effectiveLength(divisor);
        if (n == 1) {
            DivRem DR = divideByLong(dividend, divisor[0]);
            return new long[][]{DR.quotient, new long[]{DR.remainder}};
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
                long leftLow  = qhat * x;
                long leftHigh = Math.unsignedMultiplyHigh(qhat, x);
                int c = Long.compareUnsigned(leftHigh, rHat);
                if (c < 0 || (c == 0 && Long.compareUnsigned(leftLow, u[j + n - 2]) <= 0)) break;
                qhat--;
                long old = rHat;
                rHat += v[n - 1];
                if (Long.compareUnsigned(rHat, old) < 0)
                    rHatOverflow = true;
            }
            boolean negative = subtractMulLimbAt(u, v, qhat, j);
            if (negative) {
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
        long[] r = Arrays.copyOf(u, n);
        if (shift != 0)
            shiftRightBitsInPlace(r, shift);
        return new long[][]{trimLeadingZeros(q), trimLeadingZeros(r)};
    }
    private static final int BURNIKEL_ZIEGLER_BLOCK_THRESHOLD = 128;
    private static final int BURNIKEL_ZIEGLER_LEAF_THRESHOLD = 64;
    private static final int BURNIKEL_ZIEGLER_OFFSET = 80;
    private static final boolean VERIFY_DIVISION = false;
    /**
     * Burnikel-Ziegler division of arbitrary-size positive magnitudes.
     * Returns {quotient, remainder}.
     * The divisor is normalized to exactly n limbs, where n is chosen so that
     * repeated halving eventually reaches <= BURNIKEL_ZIEGLER_THRESHOLD.
     * The normalized dividend is then consumed n limbs at a time, using
     * divide2nBy1n() as the block divider.
     */
    private static long[][] divideMagnitudesBurnikelZiegler(long[] dividend, long[] divisor) {
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
         * B should now have exactly n significant limbs.
         */
        if (B.length != n)
            throw new AssertionError("BZ normalization produced " + B.length +
                    " divisor limbs; expected " + n);
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
            long[][] qr;
            if (compareArray(current, B) < 0)
                qr = new long[][]{ZERO_MAG, trimLeadingZeros(current)};
            else qr = divide2nBy1n(current, B);
            long[] qBlock = qr[0];
            remainder = qr[1];
            if (VERIFY_DIVISION) {
                // Fundamental block invariant:
                // current == qBlock * B + newR
                long[] rebuild = addMagnitudes(multiplyMagnitudes(qBlock, B), remainder);
                if (!equalMagnitudes(rebuild, current))
                    throw new AssertionError("BZ block reconstruction failed, " +
                            "block=" + block + ", qLen=" + qBlock.length + ", rLen=" + remainder.length);
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
        shiftRightBitsInPlace(remainder, sigma);
        return new long[][]{trimLeadingZeros(quotient), trimLeadingZeros(remainder)};
    }
    private static long[][] divide2nBy1n(long[] A, long[] B) {
        int cmp = compareArray(A, B);
        if (cmp < 0)
            return new long[][]{ZERO_MAG, trimLeadingZeros(A)};
        if (cmp == 0)
            return new long[][]{ONE_MAG, ZERO_MAG};
        int n = B.length;
        if (n <= BURNIKEL_ZIEGLER_LEAF_THRESHOLD || (n & 1) != 0)
            return divideMagnitudesKnuth(A, B);
        int m = n >>> 1;
        // A = A3 β^(3m) + A2 β^(2m) + A1 β^m + A0
        long[] work = new long[3 * m];
        System.arraycopy(A, m, work, 0, 3 * m);
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
        return new long[][]{trimLeadingZeros(Q), trimLeadingZeros(qr0[1])};
    }
    private static long[] mod2nBy1n(long[] current, long[] B) {
        int n = B.length;
        if (n <= BURNIKEL_ZIEGLER_LEAF_THRESHOLD || (n & 1) != 0)
            return knuthRemainder(current, B);
        int m = n >>> 1;
        // A = A3 β^(3m) + A2 β^(2m) + A1 β^m + A0
        long[] upper = new long[3 * m];
        System.arraycopy(current, m, upper, 0, 3 * m);
        long[][] qr1 = divide3nBy2n(upper, B);
        long[] R1 = qr1[1];
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
        if (cmp < 0)
            return new long[][]{ZERO_MAG, trimLeadingZeros(dividend)};
        if (cmp == 0)
            return new long[][]{ONE_MAG, ZERO_MAG};
        int n = divisor.length;
        if (n <= BURNIKEL_ZIEGLER_LEAF_THRESHOLD)
            return divideMagnitudesKnuth(dividend, divisor);
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
        return new long[][]{trimLeadingZeros(q), subtractMagnitudes(rFull, d)};
    }
    private static long[] divideMagnitudesExact(long[] n, long[] d) {
        long[][] DR = divideMagnitudes(n, d);
        if (!isZeroMagnitude(DR[1]))
            throw new ArithmeticException("Division was not exact");
        return DR[0];
    }
    public LargeInteger[] quotientAndRemainder(LargeInteger other) {
        long[][] qr = divideMagnitudes(limbs, other.limbs);
        LargeInteger q = new LargeInteger(qr[0], negative ^ other.negative, true);
        LargeInteger r = new LargeInteger(qr[1], negative, true);
        return new LargeInteger[]{q, r};
    }
    public LargeInteger divide(LargeInteger other) {return quotientAndRemainder(other)[0];}
    @Override
    public LargeFraction reciprocal() {return new LargeFraction(ONE, this);}
    public LargeInteger remainder(LargeInteger other) {
        long[] r = modMagnitudes(limbs, other.limbs);
        return new LargeInteger(r, negative, true);
    }
    private static long remainder128By64(long high, long low, long divisor) {
        if (divisor == 0L)
            throw new ArithmeticException("divide by zero");
        assert Long.compareUnsigned(high, divisor) < 0;
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
        int u3 = (int) (nHigh >>> 32);
        int u2 = (int) nHigh;
        int u1 = (int) (nLow >>> 32);
        int u0 = (int) nLow;
        int v1 = (int) (d >>> 32);
        int v0 = (int) d;

        /*
         * [u3 u2 u1] / [v1 v0]
         */
        long q1 = divideThreeByTwo(u3, u2, u1, v1, v0);
        /*
         * Exact remainder of the first 96/64 division.
         *
         * The actual remainder is < d, therefore it fits in 64 bits.
         * We only need the low 64 bits of:
         *
         *      N - q1*d
         */
        long firstLow64 = ((u2 & U32_MASK) << 32) | (u1 & U32_MASK);
        long r1 = firstLow64 - q1 * d;

        /*
         * Bring down u0:
         *
         * [r1.high r1.low u0] / [v1 v0]
         */
        long q2 = divideThreeByTwo((int) (r1 >>> 32), (int) r1, u0, v1, v0);
        /*
         * Low 64 bits of [r1.high r1.low u0]
         * are [r1.low u0].
         */
        long secondLow64 = ((r1 & U32_MASK) << 32) | (u0 & U32_MASK);
        long r2 = secondLow64 - q2 * d;
        return r2 >>> shift;
    }
    private static long modSmall(long[] a, long d) {
        if (d == 0L) throw new ArithmeticException("Cannot divide by 0");
        int len = effectiveLength(a);
        if (len == 1) return Long.remainderUnsigned(a[0], d);
        if ((d & (d - 1)) == 0) return a[0] & (d - 1);
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
        dividend = trimLeadingZeros(dividend);
        divisor  = trimLeadingZeros(divisor);
        if (isZeroMagnitude(divisor)) throw new ArithmeticException("Division by zero");
        int cmp = compareArray(dividend, divisor);
        if (cmp < 0) return dividend;
        if (cmp == 0) return ZERO_MAG;
        if (divisor.length == 1) return new long[]{modSmall(dividend, divisor[0])};
        long[] remainder;
        if (divisor.length < LargeInteger.MOD_BZ_BLOCK_THRESHOLD || dividend.length >= divisor.length * 2)
            remainder = knuthRemainder(dividend, divisor);
        else remainder = burnikelZieglerRemainder(dividend, divisor);
        if (VERIFY_DIVISION) {
            if (compareArray(divisor, remainder) >= 0)
                throw new AssertionError("Remainder greater than divisor");
        }
        return remainder;
    }
    private static long[] knuthRemainder(long[] dividend, long[] divisor) {
        dividend = trimLeadingZeros(dividend);
        divisor = trimLeadingZeros(divisor);
        if (isZeroMagnitude(divisor)) throw new ArithmeticException("Division by zero");
        int cmp = compareArray(dividend, divisor);
        if (cmp < 0) return dividend;
        if (cmp == 0) return ZERO_MAG;
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
                long leftLow  = qhat * x;
                long leftHigh = Math.unsignedMultiplyHigh(qhat, x);
                int c = Long.compareUnsigned(leftHigh, rHat);
                if (c < 0 || (c == 0 && Long.compareUnsigned(leftLow, u[j + n - 2]) <= 0)) break;
                qhat--;
                long old = rHat;
                rHat += v[n - 1];
                if (Long.compareUnsigned(rHat, old) < 0) rHatOverflow = true;
            }
            boolean negative = subtractMulLimbAt(u, v, qhat, j);
            if (negative) {
                long carry = 0L;
                for (int i = 0; i < v.length; i++)
                    carry = addWithCarry(u, j + i, v[i], carry);
                int k = j + v.length; // j + n
                u[k] += carry;
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
            if (compareArray(current, B) < 0) remainder = trimLeadingZeros(current);
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
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);
        m = trimLeadingZeros(m);
        long[] subtrahend = compareArray(a, m) < 0 ? a : modMagnitudes(a, m);
        long[] subtractor = compareArray(b, m) < 0 ? b : modMagnitudes(b, m);
        if (compareArray(subtrahend, subtractor) >= 0)
            return subtractMagnitudes(subtrahend, subtractor);
        return subtractMagnitudes(m, subtractMagnitudes(subtractor, subtrahend));
    }
    private static long[] multiplyMod(long[] a, long[] b, long[] m) {
        return modMagnitudes(multiplyMagnitudes(a, b), m);}
    public LargeInteger mod(LargeInteger modulus) {
        LargeInteger r = remainder(modulus);
        if (isZeroMagnitude(r.limbs)) return ZERO;
        else if (r.negative != modulus.negative)
            return r.add(modulus);
        return r;
    }
    long mod(long modulus) {
        if (modulus < 0) {
            long mod = mod(-modulus);
            return mod == 0 ? 0 : mod + modulus;
        }
        long R = modSmall(limbs, modulus);
        if (R == 0) return 0;
        if (negative) return modulus-R;
        return R;
    }
    private static long[] modInverseMagnitudes(long[] a, long[] mod) {
        if (isZeroMagnitude(mod))
            throw new ArithmeticException("Modulus cannot be 0");
        GcdInverseMagnitude eg = gcdInverseMagnitudes(a, mod);
        if (eg.inverse == null)
            throw new ArithmeticException("Modular inverse does not exist");
        return eg.inverse;
    }
    public LargeInteger modInverse(LargeInteger mod) {
        long[] inverse = modInverseMagnitudes(limbs, mod.limbs);
        // (-a)^-1 ≡ -(a^-1) mod |mod|.
        if (negative && !isZeroMagnitude(inverse))
            inverse = subtractMagnitudes(mod.limbs, inverse);
        // Preserve the existing negative-modulus behavior:
        // positive modulus -> [0, mod)
        // negative modulus -> (mod, 0]
        if (mod.negative && !isZeroMagnitude(inverse))
            return new LargeInteger(subtractMagnitudes(mod.limbs, inverse), true, true);
        return new LargeInteger(inverse, false, true);
    }
    private static boolean isOddMagnitude(long[] x) {return (x[0] & 1L) == 1L;}
    private static boolean isEvenMagnitude(long[] x) {return (x[0] & 1L) == 0L;}
    public boolean isEven() {return isEvenMagnitude(limbs);}
    public boolean isOdd() {return isOddMagnitude(limbs);}
    public LargeFraction power(long p) {
        if (p == 0) return LargeFraction.ONE;
        if (p == 1) return new LargeFraction(this);
        if (p < 0) return new LargeInteger(power(limbs, -p), (p & 1) == 1 && negative, true)
                .reciprocal();
        return new LargeFraction(new LargeInteger(power(limbs, p), (p & 1) == 1 && negative, true));
    }
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
                throw new IllegalArgumentException("Indeterminate form: 0^0");
            return ONE_MAG;
        } else if (power == 1) return mag.clone();
        else if (power > MAX_BITS)
            reportOverflow();
        long[] base = mag;
        long exponent = 0;
        if (isEvenMagnitude(base)) {
            base = base.clone();
            while (isEvenMagnitude(base)) {
                exponent++;
                shiftRightBitsInPlace(base, 1);
            }
        }
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
    public LargeFraction power(LargeInteger other) {
        if (other.isZero()) {
            if (isZero())
                throw new ArithmeticException("Indeterminate form: 0^0");
            return LargeFraction.ONE;
        }
        // ±1 never grows, regardless of exponent size
        if (isOne()) {
            LargeInteger result = negative && other.isOdd() ? NEGATIVE_ONE : ONE;
            return other.negative ? result.reciprocal() : new LargeFraction(result);
        }
        if (isZero()) {
            if (other.negative)
                throw new ArithmeticException("Cannot divide by 0");
            return LargeFraction.ZERO;
        }
        // a^n has at least n * (bitlength(base) - 1) + 1 bits.
        long maxExponent = (MAX_BITS - 1)/(bitLength() - 1);
        if (other.abs().greaterThan(valueOf(maxExponent)))
            reportOverflow();
        /*
         * exp is now guaranteed <= MAX_BITS - 1,
         * and MAX_BITS = 2^37, so it necessarily fits in long.
         */
        return power(other.longValue());
    }
    @Override
    public LargePowerMonomial power(LargeFraction other) {
        return new LargePowerMonomial(new LargeFraction(this), other);}
    private static final class MontgomeryReducer {
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
        final long mPrime;
        final long[] rModM;
        final long[] rTimesModulus;
        final long[] r2ModM;
        final long[] scratch;
        MontgomeryReducer copy() {
            return new MontgomeryReducer(modulus, k, mPrime,  rModM, rTimesModulus, r2ModM);}
        private MontgomeryReducer(long[] modulus, int k, long mPrime, long[] rModM, long[] rTimesModulus, long[] r2ModM) {
            this.modulus = modulus;
            this.k = k;
            this.mPrime = mPrime;
            this.rModM = rModM;
            this.rTimesModulus = rTimesModulus;
            this.r2ModM = r2ModM;
            this.scratch = new long[2*k+2];
            USE_CIOS = k < 8;
        }
        MontgomeryReducer(long[] inputModulus) {
            if (inputModulus == null)
                throw new NullPointerException("modulus is null");
            if (inputModulus.length == 0)
                throw new IllegalArgumentException("modulus has an empty abs");
            modulus = trimLeadingZeros(inputModulus);
            if (isEvenMagnitude(modulus))
                throw new ArithmeticException("Montgomery modulus must be odd");
            k = modulus.length;
            USE_CIOS = k < 8;
            long x = modulus[0];
            long y = 2L * x - x * x * x;
            for (int i = 0; i < 4; i++) y *= 2L - x * y;
            mPrime = -y;
            scratch = new long[2 * k + 2];
            long[] power = new long[2 * k + 1];
            power[k] = 1L;
            rModM = Arrays.copyOf(modMagnitudes(power, modulus), k);
            power[k] = 0L;
            power[2 * k] = 1L;
            r2ModM = Arrays.copyOf(modMagnitudes(power, modulus), k);
            rTimesModulus = shiftLeftLimbs(modulus, k);
        }
        void reduceInto(long[] product, long[] out) {
            product = trimLeadingZeros(product);
            // REDC fast path requires product < R * modulus.
            if (product.length > 2 * k || compareArray(product, rTimesModulus) >= 0)
                product = modMagnitudes(product, rTimesModulus);
            Arrays.fill(scratch, 0L);
            System.arraycopy(product, 0, scratch, 0, product.length);
            reduceScratchInto(out);
        }
        long[] toMontgomery(long[] x) {return multiply(x, r2ModM);}
        long[] toMontgomery(long x) {
            return fromMontgomery(multiplySmall(r2ModM, x));}
        long[] fromMontgomery(long[] product) {
            long[] result = new long[k];
            reduceInto(product, result);
            return trimLeadingZeros(result);
        }
        private void addCarryIntoScratch(int index, long carry) {
            if (carry == 0) return;
            carryNonzero++;
            long old = scratch[index];
            long sum = old + carry;
            scratch[index] = sum;
            if (Long.compareUnsigned(sum, old) >= 0) return;
            while (++index < scratch.length){
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
                long c1 = Long.compareUnsigned(s1, lo) < 0 ? 1L : 0L;
                long s2 = s1 + carry;
                long c2 = Long.compareUnsigned(s2, s1) < 0 ? 1L : 0L;
                scratch[index++] = s2;
                carry = hi + c1 + c2;
            }
            addCarryIntoScratch(index, carry);
        }
        private void lastSubtract(long[] out) {
            if (scratch[k << 1] == 0L) {
                for (int i = k - 1; i >= 0; --i) {
                    long a = scratch[k + i];
                    long b = modulus[i];
                    if (a != b) {
                        if (Long.compareUnsigned(a, b) < 0) {
                            canonicalCopy++;
                            System.arraycopy(scratch, k, out, 0, k);
                            return;
                        }
                        break;
                    }
                }
            }
            canonicalSubtract++;
            long borrow = 0L;
            for (int i = 0; i < k; ++i) {
                long a = scratch[k + i];
                long b = modulus[i];
                long d = a - b;
                long b1 = Long.compareUnsigned(a, b) < 0 ? 1L : 0L;
                long r = d - borrow;
                long b2 = Long.compareUnsigned(d, borrow) < 0 ? 1L : 0L;
                out[i] = r;
                borrow = b1 | b2;
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
            return trimLeadingZeros(out);
        }
        void addInto(long[] a, long[] b, long[] out) {
            // a, b < modulus; out has length k.
            long carry = 0;

            for (int i = 0; i < k; ++i) {
                long ai = i < a.length ? a[i] : 0;
                long bi = i < b.length ? b[i] : 0;

                long s1 = ai + bi;
                long c1 = Long.compareUnsigned(s1, ai) < 0 ? 1 : 0;

                long s2 = s1 + carry;
                long c2 = Long.compareUnsigned(s2, s1) < 0 ? 1 : 0;

                out[i] = s2;
                carry = c1 | c2;
            }
            // Since a, b < n, a+b < 2n, so at most one subtraction is needed.
            // If carry != 0, the true sum is 2^(64k) + out.
            // Subtracting modulus modulo 2^(64k) gives exactly the desired reduced result.
            if (carry != 0 || compareFixedToModulus(out) >= 0) {
                long borrow = 0;
                for (int i = 0; i < k; ++i) {
                    long ai = out[i];
                    long bi = modulus[i];

                    long d1 = ai - bi;
                    long b1 = Long.compareUnsigned(ai, bi) < 0 ? 1 : 0;

                    long d2 = d1 - borrow;
                    long b2 = Long.compareUnsigned(d1, borrow) < 0 ? 1 : 0;

                    out[i] = d2;
                    borrow = b1 | b2;
                }
                // Deliberately discard final borrow.
            }
        }
        long[] subtract(long[] a, long[] b) {
            requireReducedMagnitude(a, "a");
            requireReducedMagnitude(b, "b");
            long[] out = new long[k];
            subtractInto(a, b, out);
            return trimLeadingZeros(out);
        }
        void subtractInto(long[] a, long[] b, long[] out) {
            long borrow = 0;
            for (int i = 0; i < k; ++i) {
                long ai = i < a.length ? a[i] : 0;
                long bi = i < b.length ? b[i] : 0;
                long d1 = ai - bi;
                long b1 = Long.compareUnsigned(ai, bi) < 0 ? 1 : 0;
                long d2 = d1 - borrow;
                long b2 = Long.compareUnsigned(d1, borrow) < 0 ? 1 : 0;
                out[i] = d2;
                borrow = b1 | b2;
            }
            // If a < b, out currently represents 2^(64k) + a - b.
            // Adding modulus and discarding overflow gives modulus + a - b.
            if (borrow != 0) {
                long carry = 0;
                for (int i = 0; i < k; ++i) {
                    long mi = i < modulus.length ? modulus[i] : 0;
                    long s1 = out[i] + mi;
                    long c1 = Long.compareUnsigned(s1, out[i]) < 0 ? 1 : 0;
                    long s2 = s1 + carry;
                    long c2 = Long.compareUnsigned(s2, s1) < 0 ? 1 : 0;
                    out[i] = s2;
                    carry = c1 | c2;
                }
            }
        }
        private int compareFixedToModulus(long[] a) {
            for (int i = k - 1; i >= 0; i--) {
                int cmp = Long.compareUnsigned(a[i], modulus[i]);
                if (cmp != 0) return cmp;
            }
            return 0;
        }
        long[] multiply(long[] a, long[] b) {
            long[] out = new long[k];
            multiplyInto(a, b, out);
            return out;
        }
        private void multiplyCIOS(long[] A, long[] B, long[] out) {
            reduceCalls++;
            Arrays.fill(scratch, 0L);
            if (A.length < B.length) {
                long[] temp = A;
                A = B;
                B = temp;
            }
            for (int i = 0; i < B.length; ++i) {
                long scalar = B[i];
                long carry = 0L;
                int index = i;
                for (long a : A) {
                    long lo = a * scalar;
                    long hi = Math.unsignedMultiplyHigh(a, scalar);
                    long s1 = lo + scratch[index];
                    long c1 = Long.compareUnsigned(s1, lo) < 0 ? 1L : 0L;
                    long s2 = s1 + carry;
                    long c2 = Long.compareUnsigned(s2, s1) < 0 ? 1L : 0L;
                    scratch[index++] = s2;
                    carry = hi + c1 + c2;
                }
                addCarryIntoScratch(index, carry);
                handleMontgomeryLimb(i);
            }
            // Remaining implicit B limbs are zero, but Montgomery reduction
            // still needs exactly k cancellation rounds.
            long m0 = modulus[0];
            for (int i = B.length; i < k; ++i){
                long si = scratch[i];
                long u = si * mPrime;
                long carry = Math.unsignedMultiplyHigh(m0, u);
                if (si != 0L) carry++;
                int index = i + 1;
                for (int j = 1; j < k; ++j, ++index) {
                    long mj = modulus[j];
                    long lo = mj * u;
                    long hi = Math.unsignedMultiplyHigh(mj, u);
                    long s1 = lo + scratch[index];
                    long c1 = Long.compareUnsigned(s1, lo) < 0 ? 1L : 0L;
                    long s2 = s1 + carry;
                    long c2 = Long.compareUnsigned(s2, s1) < 0 ? 1L : 0L;
                    scratch[index] = s2;
                    carry = hi + c1 + c2;
                }
                addCarryIntoScratch(index, carry);
            }
            lastSubtract(out);
        }
        private final boolean USE_CIOS;
        void multiplyInto(long[] a, long[] b, long[] out) {
            if (USE_CIOS) {
                multiplyCIOS(a, b, out);
            } else if (Math.min(a.length, b.length) < KARATSUBA_THRESHOLD) {
                LargeInteger.multiplyInto(a, b, scratch);
                reduceScratchInto(out);
            } else reduceInto(multiplyMagnitudes(a, b), out);
        }
        long[] square(long[] a) {
            long[] out = new long[k];
            squareInto(a, out);
            return out;
        }
        void squareInto(long[] a, long[] out) {
            if (a.length < KARATSUBA_SQUARE_THRESHOLD) {
                LargeInteger.squareInto(a, scratch);
                reduceScratchInto(out);
            } else reduceInto(squareMagnitude(a), out);
        }
        long[] cube(long[] a) {
            requireReducedMagnitude(a, "a");
            return multiply(square(a), a);
        }
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
            if (exponent == 0){
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
        void powMontgomery(long[] baseMontgomery, long[] exponent, long[] result) {
            if (isZeroMagnitude(exponent)) {
                System.arraycopy(rModM, 0, result, 0, result.length);
                return;
            }
            int window = bestWindowSize(exponent);
            if (window == 1) {
                System.arraycopy(baseMontgomery, 0, result, 0, baseMontgomery.length);
                for (long bit = bitLength(exponent) - 2; bit >= 0; bit--) {
                    squareInto(result, result);
                    if (testBit(exponent, bit))
                        multiplyInto(result, baseMontgomery, result);
                }
                return;
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
            System.arraycopy(oddPowers[value >> 1], 0, result, 0, result.length);
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
        }
        long[] powMod(long[] base, long[] exponent) {
            long[] result = new long[k];
            powMontgomery(toMontgomery(base), exponent, result);
            return fromMontgomery(result);
        }
    }
    private static int bestWindowSize(long[] exponent) {
        long popCount = 0;
        for (long limb : exponent) popCount += Long.bitCount(limb);
        if (popCount <= 2) return 1;
        double MUL_COST = 1.13;
        // Window 1: no precomputation.
        double bestCost = (popCount - 1) * MUL_COST;
        int bestWindow = 1;
        long topBit = bitLength(exponent) - 1;
        for (int window = 2; window <= 7; window++) {
            long windowMultiplies = 0;
            long windowSquares = 0;
            long bit = topBit;
            while (bit >= 0) {
                if (!testBit(exponent, bit)) {
                    bit--;
                    continue;
                }
                long width = Math.min(window, bit + 1);
                while (width > 1 && !testBit(exponent, bit - width + 1)) width--;
                windowSquares += width;
                windowMultiplies++;
                bit -= width;
            }
            long tableMultiplies = (1L << (window - 1)) - 1;
            double cost = (windowMultiplies + tableMultiplies) * MUL_COST + windowSquares;
            if (cost < bestCost) {
                bestCost = cost;
                bestWindow = window;
            }
        }
        return bestWindow;
    }
    /**
     * Returns a LargeInteger whose value is <code>(this<sup>exponent</sup>) mod modulus</code>.
     * Unlike regular <code>power</code>, this method strictly returns an integer.
     * @param exponent the power which the base is exponentiated to
     * @param modulus the modulus in which each step is always reduced with respect to
     */
    public LargeInteger powMod(LargeInteger exponent, LargeInteger modulus) {
        if (isZeroMagnitude(modulus.limbs))
            throw new ArithmeticException("Cannot mod by 0");
        if (modulus.negative) {
            LargeInteger tmp = powMod(exponent, modulus.negate());
            if (isZeroMagnitude(tmp.limbs)) return tmp;
            return tmp.add(modulus);
        }
        if (isOneMagnitude(modulus.limbs)) return ZERO;
        if (isZeroMagnitude(exponent.limbs)) {
            if (isZero()) throw new IllegalArgumentException("Indeterminate form: 0^0");
            return ONE;
        }
        if (exponent.negative)
            return modInverse(modulus).powMod(exponent.negate(), modulus);
        if (isZero()) return ZERO;
        else if (isOneMagnitude(limbs)) return ONE;
        LargeInteger base = mod(modulus);
        // Odd modulus
        if (modulus.isOdd()) {
            if (modulus.limbs.length == 1)
                return new LargeInteger(new long[]{powMod64(base.limbs, exponent.limbs, modulus.limbs[0])},
                        false, true);
            else return new LargeInteger(new MontgomeryReducer(modulus.limbs).powMod(base.limbs, exponent.limbs),
                    false, true);
        }
        // Even modulus: m = 2^k * q
        long trailingBits = trailingZeroBits(modulus.limbs);
        long[] oddPart = shiftRightBits(modulus.limbs, trailingBits);
        long[] evenPow = powModPowerOfTwo(base.limbs, exponent.limbs, trailingBits);
        // Pure power of two
        if (isOneMagnitude(oddPart))
            return new LargeInteger(evenPow, false, true);
        // Odd component
        long[] oddBase = modMagnitudes(base.limbs, oddPart);
        long[] oddPow;
        if (oddPart.length == 1) oddPow = new long[]{powMod64(oddBase, exponent.limbs, oddPart[0])};
        else oddPow = new MontgomeryReducer(oddPart).powMod(oddBase, exponent.limbs);
        // CRT
        long[] pow2 = shiftLeftBits(ONE_MAG, trailingBits);
        long[] t = modSubtract(evenPow, oddPow, pow2);
        long[] inv = modInverseMagnitudes(oddPart, pow2);
        if (!isOneMagnitude(modMagnitudes(multiplyMagnitudes(oddPart, inv), pow2)))
            throw new AssertionError("bad oddPart inverse mod 2^k");
        t = modMagnitudes(multiplyMagnitudes(t, inv), pow2);
        return new LargeInteger(addMagnitudes(oddPow, multiplyMagnitudes(oddPart, t)), false, true);
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
    private static void maskToBits(long[] a, long bits) {
        int rem = (int) (bits & 63);
        if (rem != 0) a[a.length - 1] &= (1L << rem) - 1;
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
    private static long powModLong(long base, long exp, long mod) {
        long result = 1;
        base %= mod;
        while (exp != 0) {
            if ((exp & 1L) != 0)
                result = remainder128By64(Math.unsignedMultiplyHigh(result, base), result * base, mod);
            exp >>>= 1;
            if (exp != 0)
                base = remainder128By64(Math.unsignedMultiplyHigh(base, base), base * base, mod);
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
        exponent = trimLeadingZeros(exponent);
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
                    maskToBits(scratch, bits);
                    long[] swap = result;
                    result = scratch;
                    scratch = swap;
                }
                shiftRightBitsInPlace(expTemp, 1);
                if (!isZeroMagnitude(expTemp)) {
                    squareInto(base, scratch);
                    maskToBits(scratch, bits);
                    long[] swap = base;
                    base = scratch;
                    scratch = swap;
                }
            }
            return trimLeadingZeros(result);
        }
        //Sliding window
        int tableSize = 1 << (window - 1);
        long[][] oddPowers = new long[tableSize][];
        oddPowers[0] = base;
        long[] base2 = new long[len];
        squareInto(base, base2);
        maskToBits(base2, bits);
        for (int i = 1; i < tableSize; i++) {
            oddPowers[i] = new long[len];
            multiplyInto(oddPowers[i - 1], base2, oddPowers[i]);
            maskToBits(oddPowers[i], bits);
        }
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
                squareInto(result, scratch);
                maskToBits(scratch, bits);
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
                squareInto(result, scratch);
                maskToBits(scratch, bits);
                long[] swap = result;
                result = scratch;
                scratch = swap;
            }
            multiplyInto(result, oddPowers[value >> 1], scratch);
            maskToBits(scratch, bits);
            long[] swap = result;
            result = scratch;
            scratch = swap;
            bit -= width;
        }
        return trimLeadingZeros(result);
    }
    @Override
    public LargeDecimal approximateToDecimal(LargeInteger precision) {return null;}
    public static LargeInteger powerOfTwo(long bit) {
        if (bit < 0) throw new IllegalArgumentException("Negative Exponent");
        long[] out = new long[(int) (bit >>> 6) + 1];
        out[(int) (bit >>> 6)] = 1L << (bit & 63);
        return new LargeInteger(out, false, true);
    }
    /**
     * Returns floor(mag / 2^shift).
     * Caller chooses shift so the result occupies at most 63 bits,
     * therefore the returned long is always nonnegative.
     * Crucially, BOTH Lehmer operands must use the SAME shift.
     */
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

        /*
         * The common shift should guarantee this.
         * Keep the assertion while debugging Lehmer.
         */
        if (out < 0)
            throw new AssertionError("Lehmer approximation exceeded 63 bits");

        return out;
    }
    private static LehmerStep lehmerStep(long[] a, long[] b) {
        long aBits = bitLength(a);
        long bBits = bitLength(b);

        if (bBits == 0)
            return new LehmerStep(1, 0, 0, 1, 0);
        /*
         * Extract both approximations at the SAME binary scale.
         * The value representing "a" is expected to be >= b.
         */
        long shift = Math.max(0L, aBits - 63L);
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
     *
     * q is normally positive, but this works for arbitrary signed operands.
     */
    private static boolean signedMulSubWouldOverflow(long a, long q, long c) {
        long product = q * c;
        /*
         * Math.multiplyHigh is SIGNED high multiplication.
         *
         * A signed 128-bit product fits in a signed long iff the upper
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
    private static long[] linearCombinationNonnegative(
            long[] a, long ca,
            long[] b, long cb) {
        if (ca == 0) return signedScaleNonnegative(b, cb);
        if (cb == 0) return signedScaleNonnegative(a, ca);
        boolean aNeg = ca < 0;
        boolean bNeg = cb < 0;
        // Lehmer coefficients are small enough that Long.MIN_VALUE
        // should never be possible.
        long[] x = multiplySmall(a, Math.abs(ca));
        long[] y = multiplySmall(b, Math.abs(cb));
        if (aNeg == bNeg) {
            // Both terms have the same sign.
            if (aNeg) return null;
            return addMagnitudes(x, y);
        }
        // Opposite signs: magnitude comparison determines result sign.
        int cmp = compareArray(x, y);
        if (cmp == 0) return ZERO_MAG;
        if (cmp > 0) return aNeg ? null : subtractMagnitudes(x, y);
        return bNeg ? null : subtractMagnitudes(y, x);
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
        long[] a = trimLeadingZeros(g1);
        if (a == g1) a = g1.clone();
        long[] b = trimLeadingZeros(g2);
        if (b == g2) b = g2.clone();
        if (isZeroMagnitude(a)) return b;
        if (isZeroMagnitude(b)) return a;
        while (!isZeroMagnitude(b)) {
            if (compareArray(a, b) < 0) {
                long[] t = a;
                a = b;
                b = t;
            }
            int n = b.length;
            if (n == 1) {
                long B = b[0];
                if (a.length == 1) return new long[]{gcdLong(a[0], B)};
                return new long[]{gcdLong(modSmall(a, B), B)};
            }
            /*
             * If a and b are too different in size, leading63AtShift()
             * naturally gives bTop == 0 and Lehmer returns zero steps.
             */
            if (n > 64) {
                LehmerStep lm = lehmerStep(a, b);
                if (lm.steps >= 5) {
                    long[] na = linearCombinationNonnegative(a, lm.A, b, lm.B);
                    if (na != null) {
                        long[] nb = linearCombinationNonnegative(a, lm.C, b, lm.D);
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
            if (a.length - n <= 2) {
                long aShift = trailingZeroBits(a);
                long bShift = trailingZeroBits(b);
                long commonShift = Math.min(aShift, bShift);
                shiftRightBitsInPlace(a, aShift);
                shiftRightBitsInPlace(b, bShift);
                while (true) {
                    int cmp = compareArray(a, b);
                    if (cmp == 0)
                        return shiftLeftBits(a, commonShift);
                    if (cmp > 0) {
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
        int commonShift = Math.min(aZeros, bZeros);
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
        return a << commonShift;
    }
    public LargeInteger gcd(LargeInteger other) {
        return new LargeInteger(gcdMagnitudes(limbs, other.limbs), false, true);}
    public LargeInteger lcm(LargeInteger other) {
        if (isZero() || other.isZero()) return ZERO;
        return abs().divide(gcd(other)).multiply(other.abs());
    }
    public LargeInteger lcm(LargeInteger... other) {
        LargeInteger copy = abs();
        for (LargeInteger I : other) copy = copy.lcm(I);
        return copy;
    }
    public static final int[] VERY_SMALL_PRIMES = sieve(1000);
    public static final int[] SMALL_PRIMES = sieve(1_000_000);
    public boolean isProbablyPrime() {
        if (negative) return false;
        int cmp = compareSmall(limbs, 2);
        if (cmp <= 0) return cmp == 0;
        if (isEvenMagnitude(limbs)) return false;
        //small test
        for (int k: VERY_SMALL_PRIMES)
            if (modSmall(limbs, k) == 0)
                return limbs.length == 1 && limbs[0] == k;
        if (compareSmall(limbs, 1000000) < 0) return true;
        if (isZeroMagnitude(sqrtFloorMagnitude(limbs).remainder)) return false;
        return isAProbablyPrimeMagnitude(limbs);
    }
    private static boolean isAProbablyPrimeMagnitude(long[] n) {
        long[] nMinusOne = subtractSmall(n, 1);
        long s = trailingZeroBits(nMinusOne);
        long[] d = shiftRightBits(nMinusOne, s);
        MontgomeryReducer reducer = new MontgomeryReducer(n);
        // Montgomery representations, computed once.
        long[] oneMont = reducer.rModM;
        long[] minusOneMont = subtractMagnitudes(reducer.modulus, oneMont);
        // randomMagnitudeBetween uses an exclusive upper bound:
        // 2 <= witness < n - 1, therefore witness <= n - 2.
        long bits = bitLength(n);
        int rounds;
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
        } else if (bits < 100)
            return passesMillerRabin(50, reducer, oneMont, minusOneMont, d, s, nMinusOne);
        else if (bits < 256) rounds = 25;
        else if (bits < 512) rounds = 16;
        else if (bits < 768) rounds = 9;
        else if (bits < 1024) rounds = 4;
        else rounds = 2;
        return passesMillerRabin(rounds, reducer, oneMont, minusOneMont, d, s, nMinusOne)
                && passesLucasSelfridge(reducer, oneMont);
    }
    private static boolean passesMillerRabin(int rounds, MontgomeryReducer reducer, long[] oneMont,
                                             long[] minusOneMont, long[] d, long s, long[] nMinusOne) {
        for (int round = 0; round < rounds; round++) {
            long[] witness = reducer.toMontgomery(randomMagnitudeBetween(TWO_MAG, nMinusOne));
            long[] x = new long[reducer.k];
            reducer.powMontgomery(witness, d, x);
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
    private static boolean passesLucasSelfridge(MontgomeryReducer reducer, long[] oneMont) {
        long D = chooseSelfridgeD(reducer.modulus);
        if (D == 0L) return false;
        long[] nPlusOne = addSmall(reducer.modulus, 1);
        long s = trailingZeroBits(nPlusOne);
        long[] d = shiftRightBits(nPlusOne, s);
        if (isOneMagnitude(d)) {
            long[] current = reducer.toMontgomery(4);
            long[] twoMont = reducer.toMontgomery(2);
            for (long k = 0; k < s - 2; k++)
                current = reducer.subtract(reducer.square(current), twoMont);
            return isZeroMagnitude(current);
        }
        // Selfridge parameters:
        // P = 1
        // Q = (1 - D) / 4
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
        for (long bit = bitLength(d); bit-- > 0; ) {
            // Double: k -> 2k
            long[] doubledU = reducer.multiply(U, V);
            long[] doubledV = reducer.subtract(reducer.square(V), reducer.add(Qk, Qk));
            long[] doubledQ = reducer.square(Qk);
            U = doubledU;
            V = doubledV;
            Qk = doubledQ;
            if (testBit(d, bit)) {
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
        for (int r = 1; r < s; r++) {
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
    private static long chooseSelfridgeD(long[] n) {
        long absD = 5L;
        boolean positive = true;
        while (true) {
            long D = positive ? absD : -absD;
            int j = jacobiSmallNumerator(D, n);
            if (j == -1) return D;
            if (j == 0)
                // gcd(|D|, n) > 1.
                // Assuming small inputs were already handled, n is composite.
                return 0L;
            absD += 2L;
            positive = !positive;
        }
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
        // Remove powers of TWO from A until A becomes odd.
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
        long nModA = modSmall(n, a);
        return result * jacobiPositiveLong(nModA, a);
    }
    private static int jacobiPositiveLong(long a, long n) {
        // Assumes 0 <= a, n > 0, and n is odd.
        if (a < 0 || n <= 0 || (n & 1L) == 0L)
            throw new IllegalArgumentException("Jacobi requirements: 0<=a, n>0, and n is odd");
        a %= n;
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
        LargeInteger candidate = add(1);
        int rem30 = (int) candidate.mod(30);
        int wheelIndex = 0;
        for (int i = 1; i < WHEEL_30_RESIDUES.length; i++)
            if (WHEEL_30_RESIDUES[i] >= rem30) {
                wheelIndex = i;
                break;
            }
        int targetResidue = WHEEL_30_RESIDUES[wheelIndex];
        int delta = targetResidue - rem30;
        if (delta < 0) delta += 30;
        candidate = candidate.add(delta);
        while (true) {
            if (candidate.isProbablyPrime()) return candidate;
            candidate = candidate.add(WHEEL_30_STEPS[wheelIndex]);
            wheelIndex++;
            if (wheelIndex == WHEEL_30_STEPS.length)
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
    @SuppressWarnings("unused")
    public static LargeInteger randomPrimeChunks(int chunks) {
        while (true) {
            LargeInteger p = randomChunks(chunks);
            if (p.isProbablyPrime()) return p;
            p = p.nextProbablyPrimeInteger(150);
            if (p != null && p.limbs.length == chunks) return p;
        }
    }
    @SuppressWarnings("unused")
    public static LargeInteger randomPrimeBits(long bits) {
        while (true) {
            LargeInteger p = randomBits(bits);
            if (p.isProbablyPrime()) return p;
            p = p.nextProbablyPrimeInteger(270);
            if (p != null && p.bitLength() == bits) return p;
        }
    }
    @SuppressWarnings("unused")
    public int legendreSymbol(LargeInteger other) {
        if (other.negative || other.isEven() || !other.isProbablyPrime())
            throw new IllegalArgumentException("Legendre symbol requires an odd prime modulus");
        int result = 1;
        if (negative)
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
        if (other.negative || other.isEven())
            throw new IllegalArgumentException("Jacobi denominator must be positive and odd");
        LargeInteger a = mod(other);
        int result = 1;
        while (!isZeroMagnitude(a.limbs)) {
            while (a.isEven()) {
                a = a.shiftRight(1);
                int nMod8 = other.mod(EIGHT).intValue();
                if (nMod8 == 3 || nMod8 == 5)
                    result = -result;
            }
            LargeInteger temp = a;
            a = other;
            other = temp;
            if (a.mod(4) == 3 && other.mod(4) == 3) result = -result;
            a = a.mod(other);
        }
        return isOneMagnitude(other.limbs) ? result : 0;
    }
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
    public static long[] sievePrimeBlock(long lowerInclusive, long upperExclusive, int[] basePrimes) {
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
        long oddCountLong = ((upperExclusive - oddLower) + 1L) >>> 1;
        if (oddCountLong > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Sieve block is too large: "
                    + oddCountLong + " odd candidates");
        int oddCount = (int) oddCountLong;
        boolean[] composite = new boolean[oddCount];
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
            for (long multiple = first;
                 multiple < upperExclusive; ) {
                int index = (int) ((multiple - oddLower) >>> 1);
                composite[index] = true;
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
    private record MagnitudePowerForm(long[] base, long exponent) {}
    private static MagnitudePowerForm detectPerfectPower(long[] n) {
        n = trimLeadingZeros(n);
        if (compareSmall(n, 4) < 0) return null;
        long[] current = n;
        long exponent = 1;
        int[] primes = sieve(Math.toIntExact(bitLength(current) - 1));
        for (int p : primes) {
            if (p > bitLength(current) - 1) break;
            while (true) {
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
        return powModLong(r, k, q) == 1;
    }
    PowerFormInteger asPowerForm() {
        if(negative) throw new ArithmeticException();
        MagnitudePowerForm form = detectPerfectPower(limbs);
        return form == null
                ? new PowerFormInteger(this, 1)
                : new PowerFormInteger(new LargeInteger(form.base, false, true), form.exponent);
    }
    private static final class MagnitudeKey {
        final long[] key;
        final int hashCode;
        MagnitudeKey(long[] magKey) {
            key = magKey;
            hashCode = Arrays.hashCode(key);
        }
        @Override
        public boolean equals(Object o) {
            if (o instanceof MagnitudeKey other)
                return equalMagnitudes(key, other.key);
            return false;
        }
        @Override
        public int hashCode() {return hashCode;}
    }
    enum AlgorithmChoice{
        POLLARD_P_MINUS_ONE,
        BRENT_RHOS,
        FERMAT,
        ECM,
        QUADRATIC_SIEVE
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
        long B = -1;
        ECMModulusContext context() {
            if (context == null)
                context = new ECMModulusContext(n);
            return context;
        }
        long qsBound() {
            if (B < 0) {
                double lnN = lnMagnitude(n);
                B = Math.max(100, Math.round(Math.exp(QS_FACTOR_BASE_C
                        * Math.sqrt(lnN * StrictMath.log(lnN)))));
            }
            return B;
        }
        FactorJob(long[] n, int ecmPlanIndex, int nextECMCurve) {
            this.n = trimLeadingZeros(n);
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
        } else if (task.nextECMCurve > plan.curves()) {
            throw new AssertionError("ECM batch advanced beyond plan's curve allocation");
        }
    }
    private static boolean isNontrivialFactor(long[] d, long[] n) {
        return !isOneMagnitude(d) && compareArray(d, n) < 0 && isZeroMagnitude(modMagnitudes(n, d));}
    private static void addFactor(LargeMap<MagnitudeKey, Long> p, MagnitudeKey base, long exponent) {
        if (!p.containsKey(base)) p.put(base, exponent);
        else p.put(base, p.get(base) + exponent);
    }
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
    @FunctionalInterface
    private interface FactorCall { long[] run();}
    private record TimedFactorResult(long[] factor, long elapsedNanos) {}
    private static TimedFactorResult timeFactorCall(FactorCall call) {
        long start = System.nanoTime();
        long[] factor = call.run();
        return new TimedFactorResult(factor, System.nanoTime() - start);
    }
    /**
     * Decomposes this integer into known multiplicative factors.
     * <p>Factors are prime whenever practical, but complete prime factorization is
     * not guaranteed. If the remaining composite cofactor exceeds the configured
     * factoring effort, it is retained as a single atomic factor.</p>
     * The returned factors always multiply exactly to this integer.
     */
    public LargeMap<LargeInteger, LargeFraction> factor() {
        LargeMap<LargeInteger, LargeFraction> result = new LargeMap<>();
        if (negative)
            result.put(NEGATIVE_ONE, LargeFraction.ONE);
        if (isOneMagnitude(limbs)) return result;
        LargeMap<MagnitudeKey, Long> bag = factorMagnitude(limbs);
        for (MagnitudeKey factor : bag.keys())
            result.put(new LargeInteger(factor.key, false, true),
                    LargeFraction.valueOf(bag.get(factor)));
        return result;
    }
    private static LargeMap<MagnitudeKey, Long> factorMagnitude(long[] limbs) {
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
                LargeMap<MagnitudeKey, Long> smallerFactors = factorMagnitude(powerForm.base);
                for (MagnitudeKey K : smallerFactors.keys())
                    addFactor(factorMap, K, smallerFactors.get(K) * powerForm.exponent);
                verifyAndSortFactors(factorMap, limbs);
                return factorMap;
            }
            //detect a^2 - b^2 = (a - b)(a + b)
            long[] temp = fermatSearch(copy);
            if (!isOneMagnitude(temp)) {
                LargeMap<MagnitudeKey, Long> tempMap = factorMagnitude(temp);
                for (MagnitudeKey MK: tempMap.keys())
                    addFactor(factorMap, MK, tempMap.get(MK));
                tempMap = factorMagnitude(divideMagnitudesExact(copy, temp));
                for (MagnitudeKey MK: tempMap.keys())
                    addFactor(factorMap, MK, tempMap.get(MK));
                verifyAndSortFactors(factorMap, limbs);
                return factorMap;
            }
            //detect 1 + 4b^4 = (1 - 2b + 2b^2)(1 + 2b + 2b^2)
            long[] nMinusOne = subtractSmall(copy, 1);
            SqrtResult sqrtOfNMinusOne = sqrtFloorMagnitude(nMinusOne);
            if (isZeroMagnitude(sqrtOfNMinusOne.remainder) && isEvenMagnitude(sqrtOfNMinusOne.sqrt)) {
                long[] sqrtNMinusOne = sqrtOfNMinusOne.sqrt;
                divideMagnitudeExactInPlace(sqrtNMinusOne, 2);
                SqrtResult sqrtDR = sqrtFloorMagnitude(sqrtNMinusOne);
                if (isZeroMagnitude(sqrtDR.remainder)) {
                    long[] m1 = shiftLeftBits(sqrtNMinusOne, 1);
                    long[] n1 = shiftLeftBits(sqrtDR.sqrt, 1);
                    long[] factor1 = addSmall(subtractMagnitudes(m1, n1), 1);
                    long[] factor2 = addSmall(addMagnitudes(m1, n1), 1);
                    if (isNontrivialFactor(factor1, copy) && isNontrivialFactor(factor2, copy)) {
                        long[] remainder = divideMagnitudesExact(copy, factor1);
                        remainder = divideMagnitudesExact(remainder, factor2);
                        LargeMap<MagnitudeKey, Long> F1 = factorMagnitude(factor1);
                        for (MagnitudeKey K : F1.keys())
                            addFactor(factorMap, K, F1.get(K));
                        LargeMap<MagnitudeKey, Long> F2 = factorMagnitude(factor2);
                        for (MagnitudeKey K : F2.keys())
                            addFactor(factorMap, K, F2.get(K));
                        if (!isOneMagnitude(remainder)) {
                            LargeMap<MagnitudeKey, Long> F3 = factorMagnitude(remainder);
                            for (MagnitudeKey K : F3.keys())
                                addFactor(factorMap, K, F3.get(K));
                        }
                        verifyAndSortFactors(factorMap, limbs);
                        return factorMap;
                    }
                }
            }
            // detect x^n - 1
            MagnitudePowerForm minusForm = detectPerfectPower(addSmall(copy, 1));
            if (minusForm != null) {
                LargeList<LargeInteger> divisors = valueOf(minusForm.exponent).divisors();
                for (LargeInteger d : divisors) {
                    long[] value = ((LargeInteger) LargePolynomial.cyclotomic(d)
                            .evaluateAt(new LargeInteger(minusForm.base, false, true))).limbs;
                    if (isOneMagnitude(value)) continue;
                    if (equalMagnitudes(value, copy)) break;
                    copy = divideMagnitudesExact(copy, value);
                    LargeMap<MagnitudeKey, Long> subFactors = factorMagnitude(value);
                    for (MagnitudeKey base : subFactors.keys())
                        addFactor(factorMap, base, subFactors.get(base));
                }
            }
            // detect x^n + 1
            MagnitudePowerForm plusForm = detectPerfectPower(subtractSmall(copy, 1));
            if (plusForm != null) {
                if (equalMagnitudes(plusForm.base, TEN_MAG) && plusForm.exponent >= 10
                        && (plusForm.exponent - 10) % 20 == 0) {
                    long[] x = power(TEN_MAG, plusForm.exponent / 10);
                    long[] s = power(TEN_MAG, plusForm.exponent / 20 + 1);
                    // A = x^4 + 5x^3 + 7x^2 + 5x + 1
                    long[] A = addSmall(x, 5);
                    A = addSmall(multiplyMagnitudes(A, x), 7);
                    A = addSmall(multiplyMagnitudes(A, x), 5);
                    A = addSmall(multiplyMagnitudes(A, x), 1);
                    // C = x^3 + 2x^2 + 2x + 1
                    long[] C = addSmall(x, 2);
                    C = addSmall(multiplyMagnitudes(C, x), 2);
                    C = addSmall(multiplyMagnitudes(C, x), 1);
                    long[] F = addSmall(squareMagnitude(x), 1);
                    long[] Cs = multiplyMagnitudes(C, s);
                    long[] L = subtractMagnitudes(A, Cs);
                    long[] M = addMagnitudes(A, Cs);
                    LargeMap<MagnitudeKey, Long> F1 = factorMagnitude(F);
                    for (MagnitudeKey K : F1.keys())
                        addFactor(factorMap, K, F1.get(K));
                    LargeMap<MagnitudeKey, Long> M1 = factorMagnitude(M);
                    for (MagnitudeKey K : M1.keys())
                        addFactor(factorMap, K, M1.get(K));
                    LargeMap<MagnitudeKey, Long> L1 = factorMagnitude(L);
                    for (MagnitudeKey K : L1.keys())
                        addFactor(factorMap, K, L1.get(K));
                    verifyAndSortFactors(factorMap, limbs);
                    return factorMap;
                }
                long exponent = plusForm.exponent;
                long[] base = plusForm.base;
                while ((exponent & 1L) == 0L){
                    base = squareMagnitude(base);
                    exponent >>>= 1;
                }
                if (exponent != 1) {
                    LargeInteger n = valueOf(exponent);
                    LargeList<LargeInteger> divisors = n.divisors();
                    for (LargeInteger d : divisors) {
                        long[] value = ((LargeInteger) LargePolynomial.cyclotomic(d.shiftLeft(1))
                                .evaluateAt(new LargeInteger(base, false, true))).limbs;
                        if (isOneMagnitude(value)) continue;
                        if (equalMagnitudes(value, copy)) break;
                        copy = divideMagnitudesExact(copy, value);
                        LargeMap<MagnitudeKey, Long> subFactors = factorMagnitude(value);
                        for (MagnitudeKey K : subFactors.keys())
                            addFactor(factorMap, K, subFactors.get(K));
                    }
                }
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
            if (remLength == 1 && copy[remLength-1] == 1) {
                verifyAndSortFactors(factorMap, limbs);
                return factorMap;
            }
        }
        copy = trimLeadingZeros(copy);
        if (compareSmall(copy, 1_000_000_000_000L) < 0) {
            addFactor(factorMap, new MagnitudeKey(copy), 1L);
            verifyAndSortFactors(factorMap, limbs);
            return factorMap;
        }
        FactorCacheDatabase cache = FactorCacheDatabase.instance();
        ArrayDeque<FactorJob> work = new ArrayDeque<>();
        FactorJob task = new FactorJob(copy);
        LargeInteger number = new LargeInteger(copy, false);
        int digits = magnitudeToString(copy).length();
        work.push(task);
        while (!work.isEmpty()) {
            task = work.pop();
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
            if (!task.cacheTest) {
                number = new LargeInteger(n, false, true);
                digits = magnitudeToString(n).length();
                LargeInteger cached = cache.findFactor(number);
                if (cached != null) {
                    splitAndQueue(work, task, cached.limbs);
                    continue;
                }
                task.cacheTest = true;
            }
            long[] factor;
            if (!task.fermatDone) {
                task.fermatDone = true;
                TimedFactorResult fermat = timeFactorCall(() -> fermatSearch(n));
                //System.out.println("Fermat check time for " + number + ":  " + formatSeconds(fermat.elapsedNanos));
                factor = fermat.factor;
                if (isNontrivialFactor(factor, n)) {
                    cache.save(number, new LargeInteger(factor, false), AlgorithmChoice.FERMAT);
                    splitAndQueue(work, task, factor);
                    continue;
                }
            }
            if (!task.pm1Done) {
                task.pm1Done = true;
                TimedFactorResult pm1 = timeFactorCall(() -> pollardPMinusOne(n));
                //System.out.println("Pollard PMinusOne time for " + number + ":  " + formatSeconds(pm1.elapsedNanos));
                factor = pm1.factor;
                if (isNontrivialFactor(factor, n)) {
                    cache.save(number, new LargeInteger(factor, false), AlgorithmChoice.POLLARD_P_MINUS_ONE);
                    splitAndQueue(work, task, factor);
                    continue;
                }
            }
            if (!task.brentDone) {
                task.brentDone = true;
                TimedFactorResult brent = timeFactorCall(() -> brentRhos(n));
                //System.out.println("Brent rhos time for " + number + ":  " + formatSeconds(Brent.elapsedNanos));
                factor = brent.factor;
                if (isNontrivialFactor(factor, n)) {
                    cache.save(number, new LargeInteger(factor, false), AlgorithmChoice.BRENT_RHOS);
                    splitAndQueue(work, task, factor);
                    continue;
                }
            }
            if (task.ecmPlanIndex >= ECM_EXTREME_PLANS.length) {
                if (PRINTING)
                    System.out.println(number + " is too difficult to factor; treated as composite atomic");
                addFactor(factorMap, new MagnitudeKey(n), 1L);
                continue;
            }
            ECMPlan plan = ECM_EXTREME_PLANS[task.ecmPlanIndex];
            if (digits < 95 && task.ecmPlanIndex > 2) {
                int assumedFactorDigits = (digits + 1) >>> 1;
                int targetPlan = planForFactorDigits(assumedFactorDigits);
                double ecmSeconds = ECM_RUNTIME.estimateSecondsThrough(task, targetPlan);
                if (task.qsSeconds < ecmSeconds) {
                    factor = quadraticSieve(task, ecmSeconds);
                    if (isNontrivialFactor(factor, n)) {
                        cache.save(number, new LargeInteger(factor, false),
                                AlgorithmChoice.QUADRATIC_SIEVE);
                        splitAndQueue(work, task, factor);
                        continue;
                    }
                }
            }
            factor = runSixtyFourECMCurves(task, plan);
            if (isNontrivialFactor(factor, n)) {
                cache.save(number, new LargeInteger(factor, false), AlgorithmChoice.ECM);
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
    public LargeInteger totient() {
        if (isZero()) throw new ArithmeticException("Totient undefined for 0");
        else if (isOneMagnitude(limbs)) return ONE;
        LargeInteger n = abs();
        LargeMap<LargeInteger, LargeFraction> factors = n.factor();
        LargeInteger result = n.clone();
        for (LargeInteger p : factors.keys()) {
            if (!p.isProbablyPrime())
                throw new ArithmeticException("Totient requires full prime decomposition; " +
                        "composite atomic factor found: " + p);
            result = result.divide(p).multiply(p.subtract(1));
        }
        return result;
    }
    public LargeList<LargeInteger> divisors() {
        LargeList<LargeInteger> result = new LargeList<>();
        if (isZero() || isOne()) return result;
        LargeMap<LargeInteger, LargeFraction> factorMap = abs().factor();
        result.add(ONE);
        if (isOneMagnitude(factorMap.keyAtIndex(ZERO).limbs)) return result;
        for (LargeInteger p : factorMap.keys()) {
            if (p.negative) continue;
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
    @SuppressWarnings("unused")
    public boolean isPerfect() {
        if (lesserThanOrEqualTo(ONE)) return false;
        LargeInteger result = ZERO;
        for (LargeInteger I : divisors())
            result = result.add(I);
        return shiftLeft(1).equals(result);
    }
    //the inverse of the factor method
    public static LargeInteger buildFromFactors(LargeMap<LargeInteger, LargeFraction> pr) {
        LargeInteger result = ONE;
        for (LargeInteger p : pr.keys()) {
            if (!isOneMagnitude(pr.get(p).denominator.limbs)) throw new ArithmeticException("Result is not rational");
            if (pr.get(p).numerator.negative) throw new ArithmeticException("Result is not an integer");
            LargeInteger expTemp = pr.get(p).numerator.abs();
            LargeInteger basTemp = p.clone();
            while (!expTemp.isZero()) {
                if (expTemp.isOdd())
                    result = result.multiply(basTemp);
                if (!expTemp.isOne())
                    basTemp = basTemp.square();
                expTemp = expTemp.shiftRight(1);
            }
        }
        return result;
    }
    private record PM1Plan(int B1, int B2) {}
    private record PM1Stage1Result(long[] state, long[] factor, boolean saturated) {}
    private static long[] pollardPMinusOne(long[] n) {
        if (isEvenMagnitude(n)) return TWO_MAG;
        PM1Plan[] plans = {new PM1Plan(10_000, 100_000), new PM1Plan(20000, 200000)};
        // Sieve once through the largest B2.
        int[] primes = sieve(plans[plans.length - 1].B2);
        MontgomeryReducer reducer = new MontgomeryReducer(n);
        long[] state = reducer.toMontgomery(2);
        int previousB1 = 1;
        for (PM1Plan plan : plans) {
            PM1Stage1Result stage1 =
                    pollardPMinusOneStage1(n, state, previousB1, plan.B1, primes, reducer);
            if (isNontrivialFactor(stage1.factor, n)) return stage1.factor;
            if (stage1.saturated) return ONE_MAG;
            state = stage1.state;
            long[] factor = pollardPMinusOneStage2(n, state, primes, plan.B1, plan.B2, reducer);
            if (isNontrivialFactor(factor, n)) return factor;
            previousB1 = plan.B1;
        }
        return ONE_MAG;
    }
    private static PM1Stage1Result pollardPMinusOneStage1(long[] n, long[] initialState, int oldBound,
                                                          int newBound, int[] primes,
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
        /*
         * Brent recovery when the accumulated q caused gcd(q, n) == n.
         *
         * Walk forward from the saved ys one step at a time until an
         * individual difference exposes the factor.
         */
        if (equalMagnitudes(g, n)) {
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
        if (isZeroMagnitude(sr.remainder))
            return sr.sqrt;
        long[] a = addSmall(sr.sqrt, 1);
        // d = a^2 - n
        long[] d = subtractMagnitudes(squareMagnitude(a), n);
        // Anything beyond 500^2 cannot qualify.
        if (compareSmall(d, 250_000) > 0)
            return ONE_MAG;
        sr = sqrtFloorMagnitude(d);
        if (!isZeroMagnitude(sr.remainder)) return ONE_MAG;
        return subtractMagnitudes(a, sr.sqrt);
    }
    private record MontgomeryPoint(long[] X, long[] Z) {}
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
        private void resetScratch() {
            Arrays.fill(s0, 0);
            Arrays.fill(s1, 0);
            Arrays.fill(s2, 0);
            Arrays.fill(s3, 0);
            Arrays.fill(s4, 0);
            Arrays.fill(s5, 0);
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
        }
        void requireCanonicalPoint(MontgomeryPoint P) {
            reducer.requireReducedMagnitude(P.X, "P.X");
            reducer.requireReducedMagnitude(P.Z, "P.Z");
        }
        MontgomeryPoint newPoint() {
            return new MontgomeryPoint(new long[reducer.k], new long[reducer.k]);}
        void copyPointInto(MontgomeryPoint P, MontgomeryPoint out) {
            System.arraycopy(P.X, 0, out.X, 0, reducer.k);
            System.arraycopy(P.Z, 0, out.Z, 0, reducer.k);
        }
        /*
         * ------------------------------------------------------------
         * xDBL
         * ------------------------------------------------------------
         */

        MontgomeryPoint doublePoint(MontgomeryPoint P, ECMStats stats) {
            requireCanonicalPoint(P);
            MontgomeryPoint out = newPoint();
            resetScratch();
            doublePointCanonical(P, out, stats);
            return out;
        }
        void doublePointCanonical(MontgomeryPoint P, MontgomeryPoint out, ECMStats stats) {
            // A = X + Z
            reducer.addInto(P.X, P.Z, s0);
            // B = X - Z
            reducer.subtractInto(P.X, P.Z, s1);
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
        }
        /*
         * ------------------------------------------------------------
         * Differential addition
         * ------------------------------------------------------------
         */
        MontgomeryPoint diffAdd(MontgomeryPoint P, MontgomeryPoint Q, MontgomeryPoint PMinusQ, ECMStats stats) {
            requireCanonicalPoint(P);
            requireCanonicalPoint(Q);
            requireCanonicalPoint(PMinusQ);
            MontgomeryPoint out = newPoint();
            resetScratch();
            diffAddCanonical(P, Q, PMinusQ, out, stats);
            return out;
        }
        private void diffAddCanonical(MontgomeryPoint P, MontgomeryPoint Q, MontgomeryPoint PMinusQ,
                                      MontgomeryPoint out, ECMStats stats) {
            // A = Xp + Zp
            reducer.addInto(P.X, P.Z, s0);

            // B = Xp - Zp
            reducer.subtractInto(P.X, P.Z, s1);

            // C = Xq + Zq
            reducer.addInto(Q.X, Q.Z, s2);

            // D = Xq - Zq
            reducer.subtractInto(Q.X, Q.Z, s3);

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
            reducer.multiplyInto(PMinusQ.Z, s0, out.X);

            // Z = Xd * (DA - CB)^2
            reducer.multiplyInto(PMinusQ.X, s1, out.Z);
            stats.diffAddCounts++;
            stats.montMultiplications += 4;
            stats.montSquares += 2;
        }
        private final class DblAddResult {
            MontgomeryPoint doubled;
            MontgomeryPoint added;
            DblAddResult() {
                doubled = newPoint();
                added = newPoint();
            }
        }
        /**
         * ------------------------------------------------------------
         * <p>Combined xDBL + xADD</p>
         * ------------------------------------------------------------
         */
        private void doubleAndDiffAddCanonical(
                MontgomeryPoint P,
                MontgomeryPoint Q,
                MontgomeryPoint PMinusQ,
                DblAddResult out,
                ECMStats stats) {

            MontgomeryPoint doubled = out.doubled;
            MontgomeryPoint added = out.added;

            if (doubled == null)
                doubled = out.doubled = newPoint();

            if (added == null)
                added = out.added = newPoint();

            // A = Xp + Zp
            reducer.addInto(P.X, P.Z, s0);

            // B = Xp - Zp
            reducer.subtractInto(P.X, P.Z, s1);

            // C = Xq + Zq
            reducer.addInto(Q.X, Q.Z, s2);

            // D = Xq - Zq
            reducer.subtractInto(Q.X, Q.Z, s3);

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
            reducer.multiplyInto(PMinusQ.Z, s4, added.X);
            reducer.multiplyInto(PMinusQ.X, s5, added.Z);

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
            /*
             * Pure power of two.
             */
            if (oddK == 1L) {
                MontgomeryPoint result = newPoint();
                copyPointInto(P, result);
                MontgomeryPoint temp = newPoint();
                for (int i = 0; i < shifts; i++) {
                    doublePointCanonical(result, temp, stats);
                    copyPointInto(temp, result);
                }
                return result;
            }
            long r = choosePracSplit(oddK);
            long d = oddK - r;
            long e = r - d;
            ByteArrayOutputStream operations = new ByteArrayOutputStream();
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
                    throw new IllegalStateException("Invalid multiply PRAC reduction: oddK=" + oddK +
                            ", r=" + r + ", d=" + d + ", e=" + e);
            }
            if (d != 1L)
                throw new IllegalStateException("PRAC pair was not coprime");
            byte[] ops = operations.toByteArray();
            MontgomeryPoint result = newPoint();
            multiplyPrac(P, ops, 0, ops.length, shifts, result, stats);
            return result;
        }
        /**
         * ------------------------------------------------------------
         * <p>PRAC</p>
         * ------------------------------------------------------------
         */
        void multiplyPrac(MontgomeryPoint P, byte[] operations, int from, int toExclusive,
                          int trailingDoubling, MontgomeryPoint out, ECMStats stats) {
            if (from < 0 || toExclusive < from || toExclusive > operations.length || trailingDoubling < 0)
                throw new IllegalArgumentException("Invalid PRAC slice");
            stats.multiplyPRACCounts++;
            requireCanonicalPoint(P);
            resetScratch();
            MontgomeryPoint A = newPoint();
            MontgomeryPoint B = newPoint();
            MontgomeryPoint C = newPoint();
            MontgomeryPoint temp0 = newPoint();
            MontgomeryPoint temp1 = newPoint();
            DblAddResult pair = new DblAddResult();
            doublePointCanonical(P, A, stats);
            copyPointInto(P, B);
            copyPointInto(P, C);
            for (int index = from; index < toExclusive; index++)
                switch (operations[index]) {
                    /*
                     * ------------------------------------------------
                     * Swap
                     * ------------------------------------------------
                     */
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
                        doubleAndDiffAddCanonical(A, B, C, pair, stats);
                        A = pair.doubled;
                        B = pair.added;
                        pair.doubled = oldA;
                        pair.added = oldB;
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
                        doubleAndDiffAddCanonical(A, C, B, pair, stats);
                        A = pair.doubled;
                        C = pair.added;
                        pair.doubled = oldA;
                        pair.added = oldC;
                    } case PRAC_CASE_6 -> {
                        stats.case6Counts++;
                        MontgomeryPoint oldA = A;
                        MontgomeryPoint oldB = B;
                        MontgomeryPoint oldC = C;
                        doubleAndDiffAddCanonical(oldA, oldB, oldC, pair, stats);
                        diffAddCanonical(pair.doubled, pair.added, oldC, temp0, stats);
                        diffAddCanonical(pair.doubled, oldA, oldA, temp1, stats);
                        A = temp1;
                        B = temp0;
                        C = oldB;
                        temp1 = oldA;
                        temp0 = oldC;
                    } case PRAC_CASE_7 -> {
                        stats.case7Counts++;
                        MontgomeryPoint oldA = A;
                        MontgomeryPoint oldB = B;
                        doubleAndDiffAddCanonical(oldA, oldB, C, pair, stats);
                        diffAddCanonical(pair.added, oldA, oldB, temp0, stats);
                        diffAddCanonical(pair.doubled, oldA, oldA, temp1, stats);
                        A = temp1;
                        B = temp0;
                        temp1 = oldA;
                        temp0 = oldB;
                    } case PRAC_CASE_8 -> {
                        stats.case8Counts++;
                        MontgomeryPoint oldA = A;
                        MontgomeryPoint oldB = B;
                        MontgomeryPoint oldC = C;
                        doubleAndDiffAddCanonical(oldA, oldB, oldC, pair, stats);
                        diffAddCanonical(pair.doubled, oldA, oldA, temp0, stats);
                        diffAddCanonical(oldA, oldC, oldB, temp1, stats);
                        MontgomeryPoint newB = pair.added;
                        A = temp0;
                        B = newB;
                        C = temp1;
                        temp0 = oldA;
                        temp1 = oldC;
                        pair.added = oldB;
                        // pair.doubled never became live, so it remains scratch.
                    } case PRAC_CASE_9 -> {
                        stats.case9Counts++;
                        MontgomeryPoint oldB = B;
                        MontgomeryPoint oldC = C;
                        doubleAndDiffAddCanonical(B, C, A, pair, stats);
                        B = pair.doubled;
                        C = pair.added;
                        pair.doubled = oldB;
                        pair.added = oldC;
                    } default -> throw new IllegalStateException("Unknown PRAC operation: " + operations[index]);
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
                copyPointInto(out, originalOut);
        }
    }
    private static long goldenSplit(long k) {
        if (k <= 0L)
            throw new IllegalArgumentException("k must be positive");
        final long numerator = 987L;
        final long denominator = 1597L;
        long quotient = k / denominator;
        long remainder = k % denominator;
        // round(k * 987 / 1597), without forming k * 987
        return quotient * numerator + (remainder * numerator + (denominator >>> 1)) / denominator;
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
    private static long choosePracSplit(long oddK) {
        long center = goldenSplit(oddK);
        for (long offset = 0L; ; offset++) {
            long lower = center - offset;
            if (lower > oddK >>> 1 && gcdLong(oddK, lower) == 1L)
                return lower;
            if (offset != 0L) {
                long upper = center + offset;
                if (upper < oddK && gcdLong(oddK, upper) == 1L)
                    return upper;
            }
        }
    }
    private static final class ECMCurveWork {
        final MontgomeryCurve curve;
        MontgomeryPoint point;
        MontgomeryPoint scratch;
        boolean usable = true;

        ECMCurveWork(MontgomeryCurve curve, MontgomeryPoint point) {
            this.curve = curve;
            this.point = point;
            this.scratch = curve.newPoint();
        }
    }
    private static final class FactorFoundException extends RuntimeException {
        final long[] factor;
        FactorFoundException(long[] factor) {
            super(null, null, false, false); // no stack trace overhead
            this.factor = factor;
        }
    }
    private static final class UnusableECMCurveException extends RuntimeException {
        UnusableECMCurveException(String message) {super(message, null, false, false);}}
    private static final class ECMBatchCancelledException extends RuntimeException {
        ECMBatchCancelledException() {super(null, null, false, false);}}
    private static void checkECMBatch(AtomicReference<long[]> foundFactor) {
        if (foundFactor.get() != null || Thread.currentThread().isInterrupted())
            throw new ECMBatchCancelledException();
    }
    private static boolean claimECMFactor(AtomicReference<long[]> foundFactor, long[] factor) {
        return foundFactor.compareAndSet(null, factor);}
    /**
     * @param coefficients Lowest degree first: coefficients[i] = coefficient of x^i
     */
    private record ModularPolynomial(long[][] coefficients, MontgomeryReducer reducer) {
        ModularPolynomial(long[][] coefficients, MontgomeryReducer reducer) {
            this.coefficients = trim(coefficients);
            this.reducer = reducer;
        }
        static ModularPolynomial zero(MontgomeryReducer reducer) {
            return new ModularPolynomial(new long[][]{new long[reducer.k]}, reducer);}
        private static long[][] trim(long[][] c) {
            int n = c.length;
            if (n == 0)
                throw new IllegalArgumentException("Invalid coefficient array");
            while (n > 1 && isZeroMagnitude(c[n - 1])) n--;
            return Arrays.copyOf(c, n);
        }
        int degree() {return coefficients.length-1;}
        /**
         * Reverse the highest 'count' coefficients. For:
         * <p>
         *     a0 + a1*x + ... + an*x^n
         * <p>
         * and count = k, returns:
         * <p>
         *     an + a(n-1)*x + ... + a(n - k + 1)*x^(k-1)
         */
        private ModularPolynomial reverseHighestCoefficients(int count) {
            if (count < 1 || count > coefficients.length)
                throw new IllegalArgumentException();
            long[][] out = new long[count][];
            int n = coefficients.length - 1;
            for (int i = 0; i < count; i++)
                out[i] = coefficients[n - i];
            return new ModularPolynomial(out, reducer);
        }
        private ModularPolynomial reverseAtLength(int length) {
            if (length < coefficients.length)
                throw new IllegalArgumentException();
            long[][] out = new long[length][];
            for (int i = 0; i < length; i++)
                out[i] = new long[reducer.k];
            for (int i = 0; i < coefficients.length; i++)
                out[length - 1 - i] = coefficients[i];
            return new ModularPolynomial(out, reducer);
        }
        private ModularPolynomial reverse() {
            long[][] out = new long[coefficients.length][];
            for (int i = 0; i < coefficients.length; i++)
                out[coefficients.length-1-i] = coefficients[i];
            return new ModularPolynomial(out, reducer);
        }
        static ModularPolynomial one(MontgomeryReducer reducer) {
            return new ModularPolynomial(new long[][]{reducer.rModM.clone()}, reducer);}
        static ModularPolynomial xMinus(long[] root, MontgomeryReducer reducer) {
            return new ModularPolynomial(new long[][]{reducer.negate(root), reducer.rModM.clone()}, reducer);}
        private ModularPolynomial modXPower(int precision) {
            if (precision < 0)
                throw new IllegalArgumentException("Negative precision");
            if (precision == 0)
                return zero(reducer);
            if (precision >= coefficients.length)
                return this;
            return new ModularPolynomial(Arrays.copyOf(coefficients, precision), reducer);
        }
        private void addShifted(long[][] result, long[][] source, int shift) {
            for (int i = 0; i < source.length; i++) {
                int index = shift + i;
                if (result[index] == null) {
                    result[index] = source[i];       // shallow transfer
                } else {
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
        ModularPolynomial subtract(ModularPolynomial other) {
            if (reducer != other.reducer)
                throw new IllegalArgumentException("Moduli mismatch");
            int n = Math.max(coefficients.length, other.coefficients.length);
            long[][] out = new long[n][];
            for (int i = 0; i < n; i++) {
                if (i >= coefficients.length) out[i] = reducer.negate(other.coefficients[i]);
                else if (i >= other.coefficients.length) out[i] = coefficients[i];
                else out[i] = reducer.subtract(coefficients[i], other.coefficients[i]);
            }
            return new ModularPolynomial(out, reducer);
        }
        private static final int POLY_KARATSUBA_THRESHOLD = 4;
        ModularPolynomial multiply(ModularPolynomial other, ECMStats stats) {
            if (reducer != other.reducer)
                throw new IllegalArgumentException("Moduli mismatch");
            return new ModularPolynomial(multiplyCoefficients(coefficients, other.coefficients, stats), reducer);
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
        private ModularPolynomial multiplyModXPower(ModularPolynomial other, int precision, ECMStats stats) {
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
            return new ModularPolynomial(result, reducer);
        }
        private ModularPolynomial multiplyByTwo() {
            long[][] out = new long[coefficients.length][];
            for (int i = 0; i < out.length; i++)
                out[i] = reducer.add(coefficients[i], coefficients[i]);
            return new ModularPolynomial(out, reducer);
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
        static ModularPolynomial productOfLinearRoots(long[][] roots, MontgomeryReducer reducer, ECMStats stats) {
            ArrayList<ModularPolynomial> level = new ArrayList<>();
            for (long[] root : roots) {
                if (root != null)
                    level.add(xMinus(root, reducer));
            }
            if (level.isEmpty())
                return one(reducer);
            while (level.size() > 1) {
                ArrayList<ModularPolynomial> next = new ArrayList<>((level.size() + 1) >>> 1);
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
        private static ModularPolynomial remainderForNode(ModularPolynomial value, ProductTreeNode node,
                                                          ECMStats stats) {
            if (value.degree() < node.product.degree())
                return value;
            int divisorDegree = node.product.degree();
            int quotientLength =
                    value.degree() - divisorDegree + 1;
            ModularPolynomial result;
            if (quotientLength <= FAST_REMAINDER_THRESHOLD
                    || divisorDegree < FAST_REMAINDER_THRESHOLD)
                result = value.remainderMonicSchoolBook(node.product, stats);
            else {
                ModularPolynomial inverse = node.product.reverse().inverseSeries(quotientLength, stats);
                result = value.remainderMonicFast(node.product, inverse, stats);
            }
            return result;
        }
        ModularPolynomial remainderMonicSchoolBook(ModularPolynomial divisor, ECMStats stats) {
            if (reducer != divisor.reducer)
                throw new IllegalArgumentException("Moduli mismatch");
            int d = divisor.degree();
            if (!equalMagnitudes(divisor.coefficients[d], reducer.rModM))
                throw new IllegalArgumentException("Divisor must be monic");
            if (degree() < d)
                return this;
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
            return new ModularPolynomial(result, reducer);
        }
        /**
         * Fast remainder of this polynomial by a monic divisor.
         * <p>
         * reversedDivisorInverse must satisfy
         * <p>
         *     reverse(divisor) * reversedDivisorInverse == 1 (mod x^k)
         * <p>
         * for at least the required precision k = degree(this) - degree(divisor) + 1.
         */
        private ModularPolynomial remainderMonicFast(ModularPolynomial divisor,
                                                     ModularPolynomial reversedDivisorInverse, ECMStats stats) {
            if (reducer != divisor.reducer ||
                    reducer != reversedDivisorInverse.reducer)
                throw new IllegalArgumentException("Moduli mismatch");
            int m = divisor.degree();
            if (!equalMagnitudes(divisor.coefficients[m], reducer.rModM))
                throw new IllegalArgumentException("Divisor must be monic");
            int n = degree();
            if (n < m) return this;
            int k = degree() - divisor.degree() + 1;
            ModularPolynomial aRev = reverseHighestCoefficients(k);
            ModularPolynomial qRev = aRev.multiplyModXPower(reversedDivisorInverse, k, stats);
            ModularPolynomial quotient = qRev.reverseAtLength(k);
            /*
             * remainder = A - Q*B.
             *
             * We only care about degrees < m.
             */
            ModularPolynomial qb = quotient.multiply(divisor, stats);
            long[][] out = new long[m][];
            for (int i = 0; i < m; i++) {
                long[] a = coefficients[i];
                long[] b = i < qb.coefficients.length ? qb.coefficients[i] : new long[reducer.k];
                out[i] = reducer.subtract(a, b);
            }
            return new ModularPolynomial(out, reducer);
        }
        ModularPolynomial inverseSeries(int precision, ECMStats stats) {
            if (!equalMagnitudes(coefficients[0], reducer.rModM))
                throw new IllegalArgumentException("Constant coefficient must be Montgomery 1");
            ModularPolynomial g = one(reducer);
            int p = 1;
            while (p < precision) {
                int next = Math.min(p << 1, precision);
                ModularPolynomial fg = multiplyModXPower(g, next, stats);
                ModularPolynomial correction = one(reducer).multiplyByTwo().subtract(fg).modXPower(next);
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
            ModularPolynomial rootValue = degree() < root.product.degree() ? this
                    : remainderForNode(this, root, stats);
            long[][] out = new long[count][];
            evaluateRemainderTree(root, rootValue, out, stats);
            return out;
        }
        private static final class ProductTreeNode {
            final ModularPolynomial product;
            final ProductTreeNode left;
            final ProductTreeNode right;
            final int leafIndex;
            final long[] evaluationPoint;

            ModularPolynomial reversedInverse;
            int inversePrecision;

            ProductTreeNode(ModularPolynomial product, ProductTreeNode left, ProductTreeNode right, int leafIndex,
                            long[] evaluationPoint) {

                this.product = product;
                this.left = left;
                this.right = right;
                this.leafIndex = leafIndex;
                this.evaluationPoint = evaluationPoint;

                this.reversedInverse = null;
                this.inversePrecision = 0;
            }
            boolean isLeaf() {return left == null;}
        }
        private ProductTreeNode buildProductTree(long[][] xs, int from, int to, ECMStats stats) {
            if (to - from == 1)
                return new ProductTreeNode(xMinus(xs[from], reducer), null, null, from, xs[from]);
            int mid = (from + to) >>> 1;
            ProductTreeNode left = buildProductTree(xs, from, mid, stats);
            ProductTreeNode right = buildProductTree(xs, mid, to, stats);
            ModularPolynomial product = left.product.multiply(right.product, stats);
            return new ProductTreeNode(product, left, right, -1, null);
        }
        private static void evaluateRemainderTree(ProductTreeNode node, ModularPolynomial value,
                                                  long[][] out, ECMStats stats) {
            if (node.isLeaf()) {
                out[node.leafIndex] = value.evaluate(node.evaluationPoint, stats);
                return;
            }
            ModularPolynomial leftValue = remainderForNode(value, node.left, stats);
            evaluateRemainderTree(node.left, leftValue, out, stats);
            ModularPolynomial rightValue = remainderForNode(value, node.right, stats);
            evaluateRemainderTree(node.right, rightValue, out, stats);
        }
    }
    private static final class Stage2State {
        final MontgomeryCurve curve;
        final long[][] affineBabyX;
        final MontgomeryPoint difference;
        final ModularPolynomial polynomial;
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
            STAGE2_SLOTS = new int[480];
            int index = 0;
            for (int r = 1; r < HALF_D; r++)
                if (gcdLong(r, D) == 1)
                    STAGE2_SLOTS[index++] = r-1;
        }
        Stage2State(MontgomeryCurve curve, MontgomeryPoint Q, long firstStage2Prime, ECMStats stats,
                    int k, long[][] sigmas, AtomicReference<long[]> foundFactor) {
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
            babyX[0] = Q.X;
            babyZ[0] = Q.Z;
            MontgomeryPoint previous = Q;                    // [1]Q
            MontgomeryPoint current = curve.doublePoint(Q, stats); // [2]Q
            for (int r = 2; r < HALF_D; r++) {
                if ((r & 63) == 0) checkECMBatch(foundFactor);
                if (gcdLong(r, D) == 1) {
                    babyX[r - 1] = current.X;
                    babyZ[r - 1] = current.Z;
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
            prefix[0] = curve.reducer.rModM.clone(); // Montgomery 1
            for (int i = 0; i < count; i++) {
                if ((i & 63) == 0) checkECMBatch(foundFactor);
                prefix[i + 1] = curve.reducer.multiply(prefix[i], zs[i]);
            }
            stats.montMultiplications += count;
            long[] productNormal = curve.reducer.fromMontgomery(prefix[count]);
            GcdInverseMagnitude GI = gcdInverseMagnitudes(productNormal, curve.n);
            if (isNontrivialFactor(GI.gcd, curve.n)) {
                stats.stage2Construction += System.nanoTime() - t0;
                if (claimECMFactor(foundFactor, GI.gcd))
                    stats.foundFromStage2(GI.gcd, "Stage 2 baby initialization", k, sigmas);
                throw new FactorFoundException(GI.gcd);
            }
            if (equalMagnitudes(GI.gcd, curve.n)) {
                for (int i = 0; i < count; i++) {
                    long[] zNormal = curve.reducer.fromMontgomery(zs[i]);
                    long[] g = gcdMagnitudes(zNormal, curve.n);
                    if (isNontrivialFactor(g, curve.n)) {
                        stats.stage2Construction += System.nanoTime() - t0;
                        if (claimECMFactor(foundFactor, g))
                            stats.foundFromStage2(g, "Stage 2 baby initialization", k, sigmas);
                        throw new FactorFoundException(g);
                    }
                }
                throw new UnusableECMCurveException("Curve " + k + " is degenerate");
            }
            long[] inverseProduct = curve.reducer.toMontgomery(GI.inverse);
            for (int i = count - 1; i >= 0; i--) {
                if ((i & 63) == 0) checkECMBatch(foundFactor);
                // x_i = X_i / Z_i
                affineBabyX[STAGE2_SLOTS[i]] =
                        curve.reducer.multiply(xs[i], curve.reducer.multiply(inverseProduct, prefix[i]));
                /*
                 Remove Z_i from the inverse product:

                 (Z_0 ... Z_i)^-1 * Z_i
                     = (Z_0 ... Z_{i-1})^-1
                */
                curve.reducer.multiplyInto(inverseProduct, zs[i], inverseProduct);
                stats.montMultiplications += 3;
            }
            checkECMBatch(foundFactor);
            polynomial = ModularPolynomial.productOfLinearRoots(affineBabyX, curve.reducer, stats);
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
        void doStage2(long B1, long B2, ECMStats stats, long[][] sigmas,
                      AtomicReference<long[]> foundFactor) {
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
                    xs[count] = current.X;
                    zs[count++] = current.Z;
                    m++;
                }
                long[][] affineGiantX = batchAffineX(xs, zs, count, stats, sigmas, foundFactor);
                for (int i = 0; i < count; i++) {
                    polynomial.evaluateInto(affineGiantX[i], evaluated, stats);
                    curve.reducer.multiplyInto(blockProduct, evaluated, blockProduct);
                }
                checkECMBatch(foundFactor);
                stats.montMultiplications += count;
                blockTermCount += count;
            }
            stats.stage2Application += System.nanoTime() - start;
        }
        private long[][] batchAffineX(long[][] xs, long[][] zs, int count, ECMStats stats,
                                      long[][] sigmas, AtomicReference<long[]> foundFactor) {
            long[][] prefix = new long[count + 1][];
            prefix[0] = curve.reducer.rModM.clone();
            for (int i = 0; i < count; i++) {
                if ((i & 63) == 0) checkECMBatch(foundFactor);
                prefix[i + 1] = curve.reducer.multiply(prefix[i], zs[i]);
            }
            stats.montMultiplications += count;
            long[] productNormal = curve.reducer.fromMontgomery(prefix[count]);
            GcdInverseMagnitude GI = gcdInverseMagnitudes(productNormal, curve.n);
            if (isNontrivialFactor(GI.gcd, curve.n)) {
                if (claimECMFactor(foundFactor, GI.gcd))
                    stats.foundFromStage2(GI.gcd, "Stage 2 giant normalization", k, sigmas);
                throw new FactorFoundException(GI.gcd);
            }
            if (equalMagnitudes(GI.gcd, curve.n))
                throw new UnusableECMCurveException("Curve " + k + " is degenerate");
            long[] inverseProduct = curve.reducer.toMontgomery(GI.inverse);
            long[][] affineX = new long[count][];
            for (int i = count - 1; i >= 0; i--) {
                long[] inverseZ = curve.reducer.multiply(inverseProduct, prefix[i]);
                affineX[i] = curve.reducer.multiply(xs[i], inverseZ);
                curve.reducer.multiplyInto(inverseProduct, zs[i], inverseProduct);
                stats.montMultiplications += 3;
            }
            return affineX;
        }
        static long nearestCenterIndex(long q) {
            long m = q / D;
            if (q % D > HALF_D)
                m++;
            return Math.max(1L, m);
        }
        private void advanceGiant(ECMStats stats) {
            MontgomeryPoint next;
            if (currentM == 1L) next = curve.doublePoint(difference, stats);
            else next = curve.diffAdd(current, difference, previous, stats);
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
        void foundFromStage2(long[] factor, String location, int curve, long[][] sigmas) {
            if (!PRINTING) return;
            System.out.println("ECM FACTOR FOUND: " + magnitudeToString(factor));
            System.out.println("Location of factor: " + location);
            System.out.println("Curve where factor was found: " + curve);
            System.out.println("Curve sigmas: [");
            for (int index = 0; index < sigmas.length; index++) {
                System.out.print(sigmas[index] == null ? "(not generated)"
                        : magnitudeToString(sigmas[index]));
                if (index < sigmas.length - 1)
                    System.out.println(", ");
            }
            System.out.println("]");
        }
        void found(long[] factor, String location, int curve, long blockLower, long blockUpper, long[][] sigmas) {
            if (!PRINTING) return;
            System.out.println("ECM FACTOR FOUND: " + magnitudeToString(factor));
            System.out.println("Location of factor: " + location);
            System.out.println("Curve where factor was found: " + curve);
            if (blockLower != -1)
                System.out.println("Factor found in prime block between " + blockLower + " and " + blockUpper);
            System.out.println("Time for this batch: " + formatSeconds(totalPlanNanos()));
            System.out.print("Curve sigmas: [");
            for (int index = 0; index < sigmas.length; index++) {
                System.out.print(sigmas[index] == null ? "(not generated)"
                        : magnitudeToString(sigmas[index]));
                if (index < sigmas.length-1)
                    System.out.println(", ");
            }
            System.out.println("]");
            System.out.println();
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
            new ECMPlan(1_000_000L, 100_000_000L, 1920),
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
        for (int i = 0; i < ECM_FACTOR_DIGITS.length; i++) {
            if (factorDigits <= ECM_FACTOR_DIGITS[i])
                return i;
        }
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
    private static double estimatePrimesUpTo(long bound) {return bound/Math.log(bound);}
    private static final class ECMRuntimeModel {
        private final HashMap<ECMRuntimeKey, ECMRuntimeAggregate> samples = new HashMap<>();
        private record ECMEstimate(double creation, double stage1PerB1, double stage2PerPrime) {}
        private ECMEstimate estimateCosts(int limbs) {
            double weightSum = 0.0;
            double creation = 0.0;
            double stage1 = 0.0;
            double stage2 = 0.0;
            for (var entry : samples.entrySet()) {
                ECMRuntimeKey key = entry.getKey();
                ECMRuntimeAggregate agg = entry.getValue();
                if (agg.totalCurves == 0) continue;
                double distance = Math.abs(key.limbCount - limbs);
                double limbWeight = 1.0 / ((1.0 + distance) * (1.0 + distance));
                double weight = limbWeight * agg.totalCurves;
                double createPerCurve = agg.curveCreationNanos / (double) agg.totalCurves;
                double s1PerCurve = agg.stage1Nanos / (double) agg.totalCurves;
                double s2PerCurve = agg.stage2Nanos / (double) agg.totalCurves;
                double stage2Work = estimatePrimesUpTo(key.B2) - estimatePrimesUpTo(key.B1);
                creation += weight * createPerCurve;
                stage1 += weight * (s1PerCurve / key.B1);
                stage2 += weight * (s2PerCurve / stage2Work);
                weightSum += weight;
            }
            if (weightSum == 0.0) return null;
            return new ECMEstimate(creation / weightSum, stage1 / weightSum,
                    stage2 / weightSum);
        }
        synchronized double estimateSecondsThrough(FactorJob task, int targetPlan) {
            if (targetPlan < task.ecmPlanIndex)
                targetPlan = task.ecmPlanIndex;
            ECMEstimate cost = estimateCosts(task.n.length);
            if (cost == null) return Double.POSITIVE_INFINITY;
            double total = 0.0;
            for (int i = task.ecmPlanIndex; i <= targetPlan && i < ECM_EXTREME_PLANS.length; i++) {
                ECMPlan plan = ECM_EXTREME_PLANS[i];
                int curves = i == task.ecmPlanIndex ? plan.curves - task.nextECMCurve : plan.curves;
                double stage2Work = estimatePrimesUpTo(plan.B2) - estimatePrimesUpTo(plan.B1);
                double nanosPerCurve = cost.creation + cost.stage1PerB1 * plan.B1
                        + cost.stage2PerPrime * stage2Work;
                if (!Double.isFinite(nanosPerCurve) || nanosPerCurve < 0.0) return Double.POSITIVE_INFINITY;
                total += nanosPerCurve * curves / 1e9;
            }
            return total;
        }
    }
    private static final ECMRuntimeModel ECM_RUNTIME = new ECMRuntimeModel();
    private static final class ECMModulusContext {
        final long[] inv4;
        final MontgomeryReducer reducer;
        ECMModulusContext(long[] N) {
            GcdInverseMagnitude eg = gcdInverseMagnitudes(FOUR_MAG, N);
            if (eg.inverse == null) throw new FactorFoundException(eg.gcd);
            inv4 = eg.inverse;
            reducer = new MontgomeryReducer(N);
        }
    }
    private record GcdInverseMagnitude(long[] gcd, long[] inverse) {}
    private static GcdInverseMagnitude gcdInverseMagnitudes(long[] a, long[] n) {
        a = trimLeadingZeros(a);
        n = trimLeadingZeros(n);
        long[] oldR = a.clone();
        long[] r = n.clone();
        // Coefficient of 'a'.
        long[] oldX = ONE_MAG;
        boolean oldXNegative = false;
        long[] x = ZERO_MAG;
        boolean xNegative = false;

        while (!isZeroMagnitude(r)) {
            long[][] qr = divideMagnitudes(oldR, r);
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
                int cmp = compareArray(oldX, qx);
                nextXNegative = cmp < 0;
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
    private static long[] runSixtyFourECMCurves(FactorJob job, ECMPlan plan) {
        if (job.context == null && PRINTING){
            String s = magnitudeToString(job.n);
            System.out.println("Number entering ECM: " + s + "(" + s.length() + " digits)");
        }
        int remainingCurves = plan.curves() - job.nextECMCurve;
        if (remainingCurves <= 0) return ONE_MAG;
        ECMStats stats = new ECMStats();
        ECMCurveWork[] works = new ECMCurveWork[64];
        long[][] sigmas = new long[64][];
        AtomicReference<long[]> foundFactor = new AtomicReference<>();
        try {
            ecmCurveCreation(job, sigmas, works, stats, foundFactor);
            ecmStage1(job, sigmas, works, plan, stats, foundFactor);
            ecmStage2(job, sigmas, works, plan, stats, foundFactor);
        } catch (FactorFoundException err) {return err.factor;}
        catch (ExecutionException | InterruptedException e) {throw new RuntimeException(e);}
        ECM_RUNTIME.samples.computeIfAbsent(new ECMRuntimeKey(effectiveLength(job.n), plan.B1, plan.B2),
                        _ -> new ECMRuntimeAggregate())
                .add(stats.curveCreateNanos, stats.stage1Nanos, stats.stage2Nanos);
        if (PRINTING) {
            System.out.println("64 curves tried with bounds " + plan.B1 + " and " + plan.B2 + "; "
                    + stats.usableCurves + " being useful");
            System.out.println("Time for creating all the curves: " + formatSeconds(stats.curveCreateNanos));
            System.out.println("Stage 1 sieve time: " + formatSeconds(stats.stage1SieveNanos));
            System.out.println("Stage 1 chain build time: " + formatSeconds(stats.stage1ChainBuildNanos));
            System.out.println("Stage 1 chain application time: " + formatSeconds(stats.stage1ApplyNanos));
            System.out.println("Total Stage 1 time: " + formatSeconds(stats.stage1Nanos));
            System.out.println("Stage 2 block count: " + stats.stage2BlockCount);
            System.out.println("Stage2State construction: " + formatSeconds(stats.stage2Construction));
            System.out.println("Stage2State application on blocks: " + formatSeconds(stats.stage2Application));
            System.out.println("Total stage 2 time: " + formatSeconds(stats.stage2Nanos));
            System.out.println("Montgomery multiplications: " + stats.montMultiplications);
            System.out.println("Montgomery squares: " + stats.montSquares);
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
            System.out.println("Reduction calls: " + reduceCalls);
            carryNonzero += job.context.reducer.carryNonzero;
            System.out.println("Nonzero carries: " + carryNonzero);
            propagatedLimbs += job.context.reducer.propagatedLimbs;
            System.out.println("Propagated limbs: " + propagatedLimbs);
            canonicalSubtract += job.context.reducer.canonicalSubtract;
            System.out.println("Canonical subtractions: " + canonicalSubtract);
            canonicalCopy += job.context.reducer.canonicalCopy;
            System.out.println("Canonical copies: " + canonicalCopy);
            System.out.println("Point doubling counts: " + stats.pointDoubleCounts);
            System.out.println("Point differential additions: " + stats.diffAddCounts);
            System.out.println("Point doubling + differential addition fusions: " + stats.doubleAndDiffAddCounts);
            System.out.println("PRAC multiplications: " + stats.multiplyPRACCounts);
            System.out.println("PRAC case SWAP: " + stats.swapCounts);
            System.out.println("PRAC case 1: " + stats.case1Counts);
            System.out.println("PRAC case 2/4: " + stats.case2Or4Counts);
            System.out.println("PRAC case 3: " + stats.case3Counts);
            System.out.println("PRAC case 5: " + stats.case5Counts);
            System.out.println("PRAC case 6: " + stats.case6Counts);
            System.out.println("PRAC case 7: " + stats.case7Counts);
            System.out.println("PRAC case 8: " + stats.case8Counts);
            System.out.println("PRAC case 9: " + stats.case9Counts);
            System.out.println("Total time for this batch: " + formatSeconds(stats.totalPlanNanos()));
            System.out.println();
        }
        return ONE_MAG;
    }
    private static void ecmCurveCreation(FactorJob task, long[][] sigmas, ECMCurveWork[] works,
                                         ECMStats stats, AtomicReference<long[]> foundFactor) {
        long[] n = task.n;
        ECMModulusContext context = task.context();
        context.reducer.resetDiagnostics();
        long t0 = System.nanoTime();
        int curveCount = 0;
        while (curveCount < works.length) {
            long[] sigma = randomMagnitudeBetween(SIX_MAG, n);
            // Normal residue
            long[] sigmaSquared = modMagnitudes(squareMagnitude(sigma), n);
            long[] u = modSubtract(sigmaSquared, FIVE_MAG, n);
            long[] v = modMagnitudes(shiftLeftBits(sigma, 2), n);
            int cmp = compareArray(v, u);
            boolean negative = cmp < 0;
            long[] diff = cmp >= 0 ? subtractMagnitudes(v, u) : subtractMagnitudes(u, v);
            long[] numerator = multiplyMod(cubeMagnitude(diff), addMagnitudes(v, multiplySmall(u, 3L)), n);
            if (negative && !isZeroMagnitude(numerator))
                numerator = subtractMagnitudes(n, numerator);
            long[] denominator = multiplyMod(cubeMagnitude(u), shiftLeftBits(v, 2), n);
            GcdInverseMagnitude eg = gcdInverseMagnitudes(denominator, n);
            if (isNontrivialFactor(eg.gcd, n)) {
                stats.curveCreateNanos = System.nanoTime() - t0;
                if (claimECMFactor(foundFactor, eg.gcd))
                    stats.found(eg.gcd, "Curve construction denominator GCD", curveCount + 1,
                            -1, -1, sigmas);
                throw new FactorFoundException(eg.gcd);
            }
            if (equalMagnitudes(eg.gcd, n)) continue;
            long[] APlus2 = multiplyMod(eg.inverse, numerator, n);
            long[] A24 = multiplyMod(APlus2, context.inv4, n);
            MontgomeryCurve curve = new MontgomeryCurve(context.reducer.copy(), A24);

            long[] X = context.reducer.cube(context.reducer.toMontgomery(u));
            long[] Z = context.reducer.cube(context.reducer.toMontgomery(v));
            MontgomeryPoint point = new MontgomeryPoint(X, Z);
            sigmas[curveCount] = sigma;
            works[curveCount++] = new ECMCurveWork(curve, point);
        }
        stats.curveCreateNanos = System.nanoTime() - t0;
    }
    private static void ecmStage1(FactorJob job, long[][] sigmas, ECMCurveWork[] works, ECMPlan plan,
                                  ECMStats stats, AtomicReference<long[]> foundFactor)
            throws ExecutionException, InterruptedException {
        long t0;
        long t1 = System.nanoTime();
        long lower = 2L;
        long upperExclusive = plan.B1 + 1L;
        while (lower < upperExclusive) {
            if (foundFactor.get() != null) return;
            long upper = upperExclusive - lower <= 1048576 ? upperExclusive : lower + 1048576;
            t0 = System.nanoTime();
            long[] primes = sievePrimeBlock(lower, upper, SMALL_PRIMES);
            stats.stage1SieveNanos += System.nanoTime() - t0;
            t0 = System.nanoTime();
            ByteArrayOutputStream operations = new ByteArrayOutputStream();

            int[] offsets = new int[primes.length + 1];
            byte[] trailingDoubling = new byte[primes.length];

            int chainCount = 0;
            int powerOfTwoDoubling = 0;

            for (long prime : primes) {
                if (prime > plan.B1) continue;
                long scalar = largestPrimePowerAtMost(prime, plan.B1);
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
                        throw new IllegalStateException(
                                "Invalid Stage1 PRAC reduction: oddPart=" + oddPart +
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
            applyECMStage1Chains(works, powerOfTwoDoubling, ops, offsets, trailingDoubling,
                    stats, foundFactor);
            lower = upper;
        }
        for (int k = 0; k < works.length; k++) {
            ECMCurveWork state = works[k];
            long[] g = gcdMagnitudes(state.point.Z, job.n);
            if (isNontrivialFactor(g, job.n)) {
                stats.stage1Nanos = System.nanoTime() - t1;
                if (claimECMFactor(foundFactor, g))
                    stats.found(g, "Stage 1 GCD", job.nextECMCurve + k + 1, -1, -1, sigmas);
                throw new FactorFoundException(g);
            }
            if (equalMagnitudes(g, job.n))
                state.usable = false;
        }
        stats.stage1Nanos += System.nanoTime() - t1;
    }
    private static final int threads = 4;
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
                    for (int chainIndex = 0;
                         chainIndex < trailingDoubling.length;
                         chainIndex++) {
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
    private record Stage2ConstructionResult(Stage2State state, ECMStats stats) {}
    private static void ecmStage2(FactorJob job, long[][] sigmas, ECMCurveWork[] works, ECMPlan plan,
                                  ECMStats stats, AtomicReference<long[]> foundFactor)
            throws ExecutionException, InterruptedException {
        long t2 = System.nanoTime();

        long lower = plan.B1 + 1L;
        long upperExclusive = plan.B2 + 1L;

        long firstStage2Prime = -1L;
        long searchLower = lower;

        while (searchLower < upperExclusive) {
            long searchUpper = upperExclusive - searchLower <= 1_048_576L ? upperExclusive
                    : searchLower + 1_048_576L;
            long[] primes = sievePrimeBlock(searchLower, searchUpper, VERY_SMALL_PRIMES);
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
                        localStats, curveIndex, sigmas, foundFactor);
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
                stage2States[k] = result.state;
                stats.mergeCounters(result.stats);
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
            newFutures[k] = ECM_EXECUTOR.submit(
                    () -> runStage2Curve(state, lower, upperExclusive, curveIndex, sigmas,
                            job, foundFactor));
        }
        for (Future<Stage2CurveResult> future : newFutures) {
            if (future == null) continue;
            try {
                Stage2CurveResult result = future.get();
                if (result.usable) stats.usableCurves++;
                stats.mergeCounters(result.stats());
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                switch (cause) {
                    case FactorFoundException f -> foundFactor.compareAndSet(null, f.factor);
                    case ECMBatchCancelledException ignored -> {
                    }
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
                                                    AtomicReference<long[]> foundFactor) {
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
                s2.doStage2(localB1, localB2, local, sigmas, foundFactor);
                long[] g = gcdMagnitudes(s2.curve.reducer.fromMontgomery(s2.blockProduct), s2.curve.n);
                if (isNontrivialFactor(g, s2.curve.n)) {
                    s2.clearStage2Block();
                    local.stage2Nanos = local.totalPlanNanos();
                    if (claimECMFactor(foundFactor, g))
                        local.found(g, "Stage 2 GCD", job.nextECMCurve + curveIndex + 1,
                                blockLower, blockUpper, sigmas);
                    throw new FactorFoundException(g);
                }
                if (equalMagnitudes(g, s2.curve.n)) {
                    s2.previous = s2.blockStartPrevious;
                    s2.current = s2.blockStartCurrent;
                    s2.currentM = s2.blockStartM;
                    s2.blockProduct = s2.curve.reducer.rModM.clone();
                    s2.blockTermCount = 0;
                    long firstM = Stage2State.nearestCenterIndex(localB1 + 1);
                    long lastM = Stage2State.nearestCenterIndex(localB2);
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
                        long[] zNormal = s2.curve.reducer.fromMontgomery(s2.current.Z);
                        GcdInverseMagnitude GI = gcdInverseMagnitudes(zNormal, s2.curve.n);
                        if (isNontrivialFactor(GI.gcd, s2.curve.n)) {
                            local.stage2Nanos = local.totalPlanNanos();
                            if (claimECMFactor(foundFactor, GI.gcd))
                                local.found(GI.gcd, "Stage 2 final GCD",
                                        job.nextECMCurve + curveIndex + 1, blockLower, blockUpper, sigmas);
                            throw new FactorFoundException(GI.gcd);
                        }
                        if (equalMagnitudes(GI.gcd, s2.curve.n)) continue;
                        long[] inverseZ = s2.curve.reducer.toMontgomery(GI.inverse);
                        long[] x = s2.curve.reducer.multiply(s2.current.X, inverseZ);
                        long[] term = s2.polynomial.evaluate(x, local);
                        g = gcdMagnitudes(s2.curve.reducer.fromMontgomery(term), s2.curve.n);
                        if (isNontrivialFactor(g, s2.curve.n)) {
                            local.stage2Nanos = local.totalPlanNanos();
                            if (claimECMFactor(foundFactor, g))
                                local.found(g, "Stage 2 giant replay",
                                        job.nextECMCurve + curveIndex + 1, blockLower, blockUpper, sigmas);
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
    //  Self-Initializing Quadratic Sieve  //

    private static final boolean ENABLE_DOUBLE_PARTIALS = false;
    private static final class QSFactorBase {
        final int size;
        // Permanent data
        final int[] primes;
        final int[] logPrimes;
        final int columnCount;
        final int globalPMax;
        final long[] pMaxSquared;
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
        QSFactorBase(int[] primes, int[] logPrimes, int[] root1, int[] root2, long[] reciprocals, int size) {
            this.size = size;
            this.primes = primes.length == size ? primes : Arrays.copyOf(primes, size);
            this.reciprocals = reciprocals.length == size ? reciprocals : Arrays.copyOf(reciprocals, size);
            this.logPrimes = logPrimes.length == size ? logPrimes : Arrays.copyOf(logPrimes, size);
            this.root1 = root1.length == size ? root1 : Arrays.copyOf(root1, size);
            this.root2 = root2.length == size ? root2 : Arrays.copyOf(root2, size);
            columnCount = size + 1; // negative column + real factor-base primes
            globalPMax = primes[size - 1];
            logB = Math.log(globalPMax);
            singleLargePrimeBound = 100L * globalPMax;
            logLP = StrictMath.log(singleLargePrimeBound);
            doubleLargePrimeBound = 2L*globalPMax;
            double singleAllowance = Math.log(singleLargePrimeBound) * 27.0 / 32.0;
            if (!ENABLE_DOUBLE_PARTIALS)
                filterAllowance = singleAllowance;
            else {
                double doubleAllowance = Math.log(doubleLargePrimeBound) * 27.0 / 16.0;
                filterAllowance = Math.max(singleAllowance, doubleAllowance);
            }
            long lpLow = singleLargePrimeBound * singleLargePrimeBound;
            long lpHigh = Math.unsignedMultiplyHigh(singleLargePrimeBound, singleLargePrimeBound);
            doubleLargePrimeBoundSquared = lpHigh != 0 ? new long[]{lpLow, lpHigh} : new long[]{lpLow};
            pMaxSquared = new long[]{(long) globalPMax * globalPMax};
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
    }
    private static final double LOG_SCALE = 64.0;
    private static QSFactorBase buildFactorBase(long[] n, int bound) {
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
                long logRound = Math.round(StrictMath.log(p) * LOG_SCALE);
                // Fast path: p ≡ 3 mod 4
                if ((p & 3) == 3) {
                    int r = powModInt(nModP, (p + 1) >>> 2, p, reciprocal);
                    primes[out] = p;
                    logPrimes[out] = (int) logRound;
                    root1[out] = r;
                    root2[out] = p - r;
                    reciprocals[out++] = reciprocal;
                    continue;
                }
                if ((p & 7) == 5) {
                    int v = getV(p, nModP, reciprocal);
                    primes[out] = p;
                    logPrimes[out] = (int) logRound;
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
                logPrimes[out] = (int) logRound;
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
            int sqrtMinusOne = powModInt(2, (p - 1) >>> 2, p, reciprocal);
            long product = (long) v * sqrtMinusOne;
            v = (int) (product - Math.multiplyHigh(product, reciprocal) * p);
            if (v >= p) v -= p;
        }
        return v;
    }

    private static int[] buildFactorBaseIndexMap(QSFactorBase factorBase) {
        int max = factorBase.globalPMax;
        int[] index = new int[max + 1];
        Arrays.fill(index, -1);
        for (int i = 0; i < factorBase.size; i++)
            index[factorBase.primes[i]] = i;
        return index;
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
    private record ACandidateResult(ACoefficient coefficient, String failure) {
        static ACandidateResult fail(String reason) {return new ACandidateResult(null, reason);}
    }
    private static ACoefficient chooseAByScoring(long[] sqrt2N, QSFactorBase factorBase, SplittableRandom rng) {
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
            if (result.coefficient == null) {
                genFail++;
                if (trial == 0)
                    firstGenFailure = result.failure;
                continue;
            }
            ACoefficient candidate = result.coefficient;
            if (candidate.half == 0) {
                zeroHalf++;
                continue;
            }
            long half = candidate.half;
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
            double yield = 2.0 * half * roughDickman((lnMagnitude(candidate.A) + 2.0 * StrictMath.log(half))
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
            if (PRINTING)
                System.out.println(
                        "SIQS A warning: using fallback A because strict scoring rejected all candidates."
                                + " aFactorCount=" + factorBase.aPrimeCount + " attempts=" + attempts + " genFail=" + genFail +
                                " zeroHalf=" + zeroHalf + " halfOverInt" + halfOverInt + " halfTooLarge" + halfTooLarge
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
            half = divideByLong(half, p).quotient;
            boolean secondRoot = rng.nextBoolean();
            chosen[chosenCount++] = (idx << 1) | (secondRoot ? 1 : 0);
            logA += factorBase.logPrimes[idx] / LOG_SCALE;
        }
        if (half.length > 1) return ACandidateResult.fail("half too large");
        return new ACandidateResult(new ACoefficient(trimLeadingZeros(A), chosen, half[0]), null);
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
            default -> StrictMath.exp(-u * (StrictMath.log(u) + Math.log(StrictMath.log(u)) - 1.0));
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

        RelationYieldEstimate estimate;
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

            this.estimate = estimate;
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
            double fullHitsPerPoly = weightedAverage(initialEstimate.fullPerPoly, priorWeight,
                    measuredFullPerPoly(), observedWeight);
            double singleHitsPerPoly = weightedAverage(initialEstimate.singlePartialHitsPerPoly, priorWeight,
                    measuredSinglePartialPerPoly(), observedWeight);
            double doubleHitsPerPoly = weightedAverage(initialEstimate.doublePartialHitsPerPoly, priorWeight,
                    measuredDoublePartialPerPoly(), observedWeight);
            double singleMergesPerPoly = weightedAverage(initialEstimate.singlePartialMergesPerPoly, priorWeight,
                    singlePartialMerged / (double) polys, observedWeight);
            double doubleMergesPerPoly = weightedAverage(initialEstimate.doublePartialMergesPerPoly, priorWeight,
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
        long collectionNs() {return precomputation+polyNs+sieveNs+attemptNs;}
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
                                            PartialGraph partialGraph, SplittableRandom rng, int requiredRelations) {
        long precomputation = System.nanoTime();
        ACoefficient AR = chooseAByScoring(sqrt2N, factorBase, rng);
        long[] A = AR.A;
        Arrays.sort(AR.factors);
        int half = (int)AR.half;
        int startT = -half;
        int interval = 2 * half;
        double logA = lnMagnitude(A);
        double logQ = logA + 2.0 * Math.log(half);
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
        long[] B = BR.B;
        long rootCalcTime = System.nanoTime();
        computeInitialRoots(factorBase, B, startT);
        plan.rootCalcTime = System.nanoTime() - rootCalcTime;
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
                    if (delta >= p) delta -= p;
                    deltaRow[j] = delta;
                }
                lo = hi + 1;
            }
            for (int j = lo; j < factorBase.size; j++) {
                int p = factorBase.primes[j];
                long reciprocal = factorBase.reciprocals[j];
                long z = (long) modSmallInt(step, stepLength, p, reciprocal) * factorBase.invAModP[j];
                int delta = (int) (z - Math.multiplyHigh(z, reciprocal) * p);
                if (delta >= p) delta -= p;
                deltaRow[j] = delta;
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
                if (compareArray(B, A) >= 0) {
                    subtractMagnitudeInPlace(B, A);
                    wrapped = true;
                } else wrapped = false;
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
        if (PRINTING)
            System.out.printf(
                    "%50s %10d %12d %16d %12d %10d %14d %8d %14d %13d"
                            + "%20.5f %14.5f %11.5f %16.5f %16.5f %16.5f %16.5f %16.5f %14.5f %13.5f %14.5f " +
                            "%14.5f %12.5f %12.5f %12.5f %15d %25.5f %n",
                    magnitudeToString(A),
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
        if (totalWeight == 0.0) return 0.0;
        return (first * firstWeight + second * secondWeight) / totalWeight;
    }
    private record BResult(long[] B, int[] inactiveLarge, int inactiveLargeCount) {}
    private static BResult computeB(ACoefficient AR, QSFactorBase factorBase, int largeStart) {
        long[] A = AR.A;
        int aLen = effectiveLength(A);
        long[] B = new long[aLen + 1];
        int inactiveLargeCount = 0;
        int[] inactiveLarge = new int[AR.factors.length + 1];
        int lo = 1;
        for (int i = 0; i < AR.factors.length; i++) {
            int packed = AR.factors[i];
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
            long[] twoAi = multiplySmall(Ai, 2);
            if (compareArray(twoAi, A) >= 0)
                subtractMagnitudeInPlace(twoAi, A);
            factorBase.twoTerms[i] = twoAi;
            addMagnitudeInPlace(B, Ai, 0);
            if (compareArray(B, A) >= 0)
                subtractMagnitudeInPlace(B, A);
            lo = idx + 1;
        }
        // Active tail after the final A-factor.
        for (int j = lo; j < factorBase.primes.length; j++) {
            int p = factorBase.primes[j];
            factorBase.invAModP[j] =
                    inverseModSmall(modSmallInt(A, aLen, p, factorBase.reciprocals[j]), p);
        }
        return new BResult(trimLeadingZeros(B), inactiveLarge, inactiveLargeCount);
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
                // active j
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
                if (x1 < 0) x1 += p;
                if (x2 < 0) x2 += p;
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
            if (x1 < 0) x1 += p;
            if (x2 < 0) x2 += p;
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
                if (x1 < 0) x1 += p;
                if (x2 < 0) x2 += p;
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
            if (x1 < 0) x1 += p;
            if (x2 < 0) x2 += p;
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
                score = factorBase.score,
                hitIndices = factorBase.hitIndices, hitPrimes = factorBase.hitPrimes;
        for (int k = 0, inactiveLargeCount = BR.inactiveLargeCount; k < inactiveLargeCount; k++) {
            int hi = BR.inactiveLarge[k];
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
            if (factorBase.score[idx] >= scoreThreshold) {
                factorBase.candidateIndices[candidateCount++] = idx;
                factorBase.hitHead[idx] = -1;
            }
        }
        plan.candidateScanNs += System.nanoTime() - candidateScanStart;
        long hitLinkStart = System.nanoTime();
        int keptHitCount = 0;
        for (int h = 0; h < hitCount; h++) {
            int index = factorBase.hitIndices[h];
            if (factorBase.score[index] < scoreThreshold) continue;
            int j = factorBase.hitPrimes[h];
            factorBase.hitIndices[keptHitCount] = factorBase.hitHead[index];
            factorBase.hitPrimes[keptHitCount] = j;
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
        for (int j = 0; j < largeStart; j++) {
            tested++;
            int p = primes[j];
            long reciprocal = reciprocals[j];
            int column = j + 1;
            while (modSmallInt(remaining, remLength, p, reciprocal) == 0) {
                if (dense[column] == 0) used[usedCount++] = column;
                incExp(dense, column);
                divideMagnitudeExactInPlace(remaining, p);
                while (remLength > 1 && remaining[remLength - 1] == 0) remLength--;
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
        for (int k = 0; k < inactiveLargeCount; k++) {
            int j = inactiveLarge[k];
            tested++;
            int p = primes[j];
            long reciprocal = reciprocals[j];
            int column = j + 1;
            while (modSmallInt(remaining, remLength, p, reciprocal) == 0) {
                if (dense[column] == 0)
                    used[usedCount++] = column;
                incExp(dense, column);
                divideMagnitudeExactInPlace(remaining, p);
                while (remLength > 1 && remaining[remLength - 1] == 0) remLength--;
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
            performInactiveLargeDivisions(qMagnitude, remLength, BR.inactiveLarge, BR.inactiveLargeCount, factorBase.primes,
                    factorBase.reciprocals, factorBase.dense, factorBase.used, usedCount,
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
            qMagnitude = trimLeadingZeros(qMagnitude);
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
                || compareArray(remaining, factorBase.pMaxSquared) <= 0)
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
        if (x >= 0) return (double) x;
        else return (double) (x & Long.MAX_VALUE) + 0x1.0p63;
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
    private static boolean withinLargePrimeRange(long value, long globalPMax, long largePrimeBound) {
        return Long.compareUnsigned(value, globalPMax) > 0 && Long.compareUnsigned(value, largePrimeBound) <= 0;}
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
                    reducer.multiplyInto(rhs, temp, rhs);
                }
            }
            for (long[] sf : relations.squareFactors[r])
                reducer.multiplyInto(rhs, reducer.square(reducer.toMontgomery(sf)), rhs);
            lhs = reducer.fromMontgomery(lhs);
            rhs = reducer.fromMontgomery(rhs);
            if (!equalMagnitudes(lhs, rhs))
                throw new AssertionError("BAD QS RELATION " + r + "\nxValues="
                        + relations.xValues[r].length + "\nsquareFactors=" + relations.squareFactors[r].length
                        + "\nlhs=" + magnitudeToString(lhs) + "\nrhs="
                        + magnitudeToString(rhs));
        }
    }

    // --- Matrix steps from here --- //

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
    private static int findOneAliveRowContaining(ArrayList<SparseMatrixRow> rows, IntList[] incidence,
                                                 int column) {
        IntList list = incidence[column];
        if (list == null) return -1;
        int found = -1;
        for (int i = 0; i < list.size; i++) {
            int r = list.data[i];
            if (!rows.get(r).alive) continue;
            if (found != -1) return -1;
            found = r;
        }
        return found;
    }
    private static int[] findTwoAliveRowsContaining(ArrayList<SparseMatrixRow> rows, IntList[] incidence,
                                                    int column) {
        IntList list = incidence[column];
        if (list == null) return null;
        int first = -1;
        int second = -1;
        int out = 0;
        for (int i = 0; i < list.size; i++) {
            int r = list.data[i];
            if (!rows.get(r).alive) continue;
            list.data[out++] = r;
            if (first < 0) first = r;
            else if (second < 0) second = r;
        }
        // Dead rows can never become alive again.
        list.size = out;
        return second < 0 ? null : new int[]{first, second};
    }
    private static final class IntList {
        int[] data = new int[4];
        int size;
        void add(int x) {
            if (size == data.length)
                data = Arrays.copyOf(data, data.length << 1);
            data[size++] = x;
        }
    }
    private static ArrayList<SparseMatrixRow> degreeTwoMergeFilter(ArrayList<SparseMatrixRow> rows,
                                                                   int columnCount, QSFullRelationPool relations,
                                                                   QSFactorBase factorBase, long[] N) {
        IntList[] incidence = new IntList[columnCount];
        int[] weight = new int[columnCount];
        for (int r = 0; r < rows.size(); r++) {
            SparseMatrixRow row = rows.get(r);
            if (!row.alive) continue;
            for (int c : row.cols) {
                weight[c]++;
                if (incidence[c] == null) incidence[c] = new IntList();
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
                        incidence[c2] = new IntList();
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
            this.alive = true;
        }
        SparseMatrixRow(int[] cols, int[] deps) {
            this.cols = cols;
            this.deps = deps;
            alive = true;
        }
    }
    private static SparseMatrixSetup buildQSMatrix(QSFullRelationPool relations, QSFactorBase factorBase,
                                                   long[] N) {
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
        GF2SparseMatrix matrix = new GF2SparseMatrix(rowCount, factorBase.columnCount, rowOffsets, columns);
        if (PRINTING) {
            System.out.println("Time for building sparse rows: " + formatSeconds(sparseRowNanos));
            System.out.println("Time for first singleton filter: " + formatSeconds(singleton1Nanos));
            System.out.println("Time for doubleton filter: " + formatSeconds(doubletonNanos));
            System.out.println("Time for second singleton filter: " + formatSeconds(singleton2Nanos));
            System.out.println("Matrix dimensions: " + rowCount + " rows by " + factorBase.columnCount + " columns");
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
        void multiplyMMtBlock64(long[] rowBlock, long[] colScratch, long[] out) {
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
                if(lanes == 0) continue;
                for (int k = rowOffsets[r]; k < rowOffsets[r + 1]; k++)
                    colScratch[columns[k]] ^= lanes;
            }
        }
        private void multiplyBlock(long[] colScratch, long[] out) {
            for (int r = 0; r < rowCount; r++) {
                long lanes = 0L;
                int end = rowOffsets[r + 1];
                for (int k = rowOffsets[r]; k < end; k++)
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
        static long rightMultiplyLookup(long x, long[] table) {
            return    table[(int) x & 255]
                    ^ table[256  | ((int) (x >>>  8) & 255)]
                    ^ table[512  | ((int) (x >>> 16) & 255)]
                    ^ table[768  | ((int) (x >>> 24) & 255)]
                    ^ table[1024 | ((int) (x >>> 32) & 255)]
                    ^ table[1280 | ((int) (x >>> 40) & 255)]
                    ^ table[1536 | ((int) (x >>> 48) & 255)]
                    ^ table[1792 | ((int) (x >>> 56) & 255)];
        }
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
            for (int iter = 0; true; iter++) {
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
                GF2Matrix64.multiplyInto(elimination.embeddedInverse, overlap, coefficients);
                buildRightMulTable64(coefficients, rightMulTable);
                long t0 = System.nanoTime();
                for (int i = 0; i < state.v.length; i++)
                    LEFT[i] ^= rightMultiplyLookup(state.v[i], rightMulTable);
                tableApplyTime += System.nanoTime() - t0;
                updateLanczosState(elimination.selectedMask, av, gram, elimination.embeddedInverse, state, dTable,
                        eTable, fTable, bucket);
                if (iter > rowCount + 64)
                    throw new IllegalStateException("Block Lanczos did not terminate");
            }
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
                while (true) {
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
                    combinationLow ^= old.low;
                    combinationHigh ^= old.high;
                }
            }
            ArrayList<long[]> dependencies = new ArrayList<>();
            for (Combination128 combination : nullspace) {
                long[] dependency = new long[(d.length + 63) >>> 6];
                for (int row = 0; row < d.length; row++) {
                    int parity = (Long.bitCount(d[row] & combination.low)
                            ^ Long.bitCount(terminalV[row] & combination.high));
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
        private GF2Matrix64() {}
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
                    throw new AssertionError("Could not include required Lanczos lanes: " +
                            "required=" + Long.toHexString(requiredMask) + ", selected=" +
                            Long.toHexString(selectedMask));
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
                                                  int originalRelationCount) {
        GF2SparseMatrix matrix = setup.matrix;
        for (int attempt = 0; attempt < 8; attempt++) {
            SplittableRandom attemptRng = rng.split();
            long t0 = System.nanoTime();
            BlockLanczosResult lanczos = matrix.blockLanczos(attemptRng);
            if (PRINTING) {
                System.out.println("Block Lanczos iterations: " + lanczos.iterations);
                System.out.println("Lanczos core: " + formatSeconds(System.nanoTime() - t0));
                System.out.println("Time spent on blockDot: " + formatSeconds(blockDotTime));
                System.out.println("Time spent on eliminate64: " + formatSeconds(eliminateTime));
                System.out.println("Time spent on buildRightMultiplyTable64: " + formatSeconds(tableBuildTime));
                System.out.println("Time spent on rightMultiplyLookup: " + formatSeconds(tableApplyTime));
                System.out.println("Time spent on A^T * block: " + formatSeconds(transposeBlockTime));
                System.out.println("Time spent on A * block: " + formatSeconds(blockTime));
            }
            long[] d = new long[Math.max(lanczos.LEFT.length, lanczos.RIGHT.length)];
            xorInto(d, lanczos.LEFT);
            xorInto(d, lanczos.RIGHT);
            t0 = System.nanoTime();
            long[][] filteredDependencies = matrix.extractLanczosDependencies(d, lanczos.terminalV);
            if (filteredDependencies.length == 0) continue;
            if (PRINTING)
                System.out.println(
                        "Extracting dependencies: " + formatSeconds(System.nanoTime() - t0));
            t0 = System.nanoTime();
            long[][] expanded = new long[filteredDependencies.length][];
            int expectedWords = (setup.rowDeps.length + 63) >>> 6;
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
                        if (filteredRow >= setup.rowDeps.length) break;
                        for (int originalRelation : setup.rowDeps[filteredRow])
                            originalRelations[originalRelation >>> 6] ^= 1L << (originalRelation & 63);
                    }
                }
                expanded[i] = originalRelations;
            }
            if (PRINTING) {
                System.out.println("Expanding dependencies: " + formatSeconds(System.nanoTime() - t0));
                System.out.println();
            }
            return expanded;
        }
        throw new IllegalStateException("Block Lanczos repeatedly failed to find dependencies");
    }
    private static long[] reconstructFromSparseDependency(int[] deps, QSFullRelationPool relations,
                                                          QSFactorBase factorBase, long[] N) {
        MontgomeryReducer reducer = new MontgomeryReducer(N);
        int columnCount = factorBase.size + 1;
        long[] X = reducer.rModM.clone();
        long[] Y = reducer.rModM.clone();
        long[] expSums = new long[columnCount];
        for (int relIndex : deps) {
            for (long[] x : relations.xValues[relIndex]) reducer.multiplyInto(X, x, X);
            for (int k = 0; k < relations.exponentIndex[relIndex].length; k++)
                expSums[relations.exponentIndex[relIndex][k]] += relations.exponentCount[relIndex][k];
            for (long[] rf: relations.squareFactors[relIndex])
                reducer.multiplyInto(Y, rf, Y);
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
                reducer.multiplyInto(Y, temp, Y);
            }
        }
        if (!equalMagnitudes(reducer.square(X), reducer.square(Y)))
            throw new AssertionError("X^2 !≡ Y^2 mod N");
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
                reducer.multiplyInto(Y, temp, Y);
            }
        }
        // These can be checked while still in Montgomery representation.
        if (!equalMagnitudes(reducer.square(X), reducer.square(Y))) {
            System.err.println("X^2 = " + magnitudeToString(squareMagnitude(reducer.fromMontgomery(X))));
            System.err.println("Y^2 = " + magnitudeToString(squareMagnitude(reducer.fromMontgomery(Y))));
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
    private static final double QS_FACTOR_BASE_C = 0.55;
    @SuppressWarnings("unused")
    public LargeInteger quadraticSieveWithoutConstraints() {
        if (isProbablyPrime()) return this;
        double lnN = lnMagnitude(limbs);
        double lnLnN = StrictMath.log(lnN);
        long B = Math.round(Math.exp(QS_FACTOR_BASE_C * Math.sqrt(lnN * lnLnN)));
        B = Math.max(B, 100L);
        if (B > Integer.MAX_VALUE)
            throw new ArithmeticException("Unconstrained QS bound " + B + " exceeds supported int range");
        try {
            long t0 = System.nanoTime();
            QSFactorBase factorBase = buildFactorBase(limbs, (int) B);
            int[] fbIndex = buildFactorBaseIndexMap(factorBase);
            long factorBaseBuildTime = System.nanoTime() - t0;
            int requiredRelations = factorBase.primes.length + 30;
            if (PRINTING) {
                String S = toString();
                System.out.println("Number entered unconstrained QS: " + S + "(" + S.length() + " digits)");
                System.out.println("Factor-base bound: " + B);
                System.out.println("Factor base build time: " + formatSeconds(factorBaseBuildTime) + " ("
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
            if (PRINTING) {
                System.out.println("A RNG seed: " + Long.toUnsignedString(A_RNG_SEED));
                System.out.println();
            }
            SplittableRandom A_RNG = new SplittableRandom(A_RNG_SEED);
            long relationStart = System.nanoTime();
            if (PRINTING)
                System.out.printf(
                        "%3s %50s %10s %12s %16s %12s %10s %14s %8s %14s %13s %19s %14s %11s %16s %16s %16s %16s %16s %14s %13s %14s %14s %12s %12s %12s %15s %25s%n",
                        "#", "A", "int", "candidates", "rejected", "full", "sing", "sing merge", "double", "double merge", "mix merge", "precomputation",
                        "root", "sieve", "scanning", "candidateScan", "hitLink", "smallDiv", "factoring", "class", "processing", "total", "rel/s",
                        "div/poly", "rel/poly", "merge/poly", "rel total", "est. sec remaining");
            long[] sqrt2N = sqrtCeilMagnitude(shiftLeftBits(limbs, 1));
            for (long blocks = 0; relations.size < requiredRelations; blocks++) {
                if (PRINTING)
                    System.out.printf("%3d ", blocks + 1);
                sieve128Polynomials(limbs, sqrt2N, factorBase, fbIndex, relations, partials, partialGraph,
                        A_RNG, requiredRelations);
            }
            long relationNanos = System.nanoTime() - relationStart;
            if (PRINTING) {
                System.out.println();
                System.out.println("Total QS relation time: " + formatSeconds(relationNanos));
                System.out.println("Overall relation collection rate: " + relations.size * 1e9 / relationNanos
                        + " relations/second");
                System.out.println();
            }
            validateQSRelations(relations, factorBase, limbs);
            int originalRelationCount = relations.size;
            long buildMatrixNanos = System.nanoTime();
            SparseMatrixSetup setup = buildQSMatrix(relations, factorBase, limbs);
            buildMatrixNanos = System.nanoTime() - buildMatrixNanos;
            if (PRINTING) {
                System.out.println("Matrix build time: " + formatSeconds(buildMatrixNanos));
                System.out.println();
            }
            long matrixSeed = 6722688380560470180L;
            long[][] dependencies = nullspaceDependencies(setup, new SplittableRandom(matrixSeed),
                    originalRelationCount);
            for (long[] dependency : dependencies) {
                long[] factor = reconstructFactor(dependency, relations, factorBase, limbs);
                if (isNontrivialFactor(factor, limbs))
                    return new LargeInteger(factor, false, true);
            }
        } catch (FactorFoundException e) {return new LargeInteger(e.factor, false, true);}
        return ONE;
    }
    //given a number N, find a relation X^2 ≡ Y^2 (mod N). then (X-Y) and (X+Y) reveal factors of N.
    private static long[] quadraticSieve(FactorJob task, double remainingECMSeconds) {
        if (!Double.isFinite(remainingECMSeconds) || remainingECMSeconds <= 0.0) return ONE_MAG;
        /*
         * Spend at most 5% of the known remaining ECM time testing QS.
         */
        double probeSeconds = Math.max(remainingECMSeconds * 0.05, 20.0);
        long probeBudgetNanos = Math.round(probeSeconds * 1e9);
        long totalStart = System.nanoTime();
        String numberText = magnitudeToString(task.n);
        if (PRINTING)
            System.out.println("Number entering QS: " + numberText + "(" + numberText.length() + " digits)");
        /*
         * Factor-base construction time counts against the QS probe.
         */
        try {
            if (task.factorBase == null) {
                long t0 = System.nanoTime();
                task.factorBase = buildFactorBase(task.n, Math.toIntExact(task.qsBound()));
                task.fbIndex = buildFactorBaseIndexMap(task.factorBase);
                if (PRINTING)
                    System.out.print("Factor base build time: "
                            + formatSeconds(System.nanoTime() - t0));
            }
            int requiredRelations = task.factorBase.size + 30;
            if (PRINTING)
                System.out.println("(" + task.factorBase.size + " primes; "
                        + requiredRelations + " relations required)");
            if (task.relations == null)
                task.relations = new QSFullRelationPool(requiredRelations*9/8);
            if (task.partials == null)
                task.partials = new QSPartialRelationPool(requiredRelations*9/8);
            ArrayList<Double> blockRates = new ArrayList<>();
            int blocks = 0;
            boolean qsAccepted;
            SplittableRandom rng = new SplittableRandom();
            if (PRINTING)
                System.out.printf(
                        "%3s %50s %10s %12s %16s %12s %10s %14s %8s %14s %13s %19s %14s %11s %16s %16s %16s %16s %16s %14s %13s %14s %14s %12s %12s %12s %15s %25s%n",
                        "#", "A", "interval", "candidates", "rejected", "full", "sing", "sing merge", "double", "double merge", "mix merge", "precomputation",
                        "root", "sieve", "scanning", "candidateScan", "hitLink", "smallDiv", "factoring", "class", "processing", "total", "rel/s",
                        "div/poly", "rel/poly", "merge/poly", "rel total", "est. sec remaining");
            long[] sqrt2N = sqrtCeilMagnitude(shiftLeftBits(task.n, 1));
            while (task.relations.size < requiredRelations) {
                int relationsBefore = task.relations.size;
                if (PRINTING)
                    System.out.printf("%4d", blocks + 1);
                long blockStart = System.nanoTime();
                sieve128Polynomials(task.n, sqrt2N, task.factorBase, task.fbIndex, task.relations, task.partials,
                        task.partialGraph, rng, requiredRelations);
                long blockNanos = System.nanoTime() - blockStart;
                int relationsAdded = task.relations.size - relationsBefore;
                double rate = blockNanos == 0L ? 0.0 : relationsAdded * 1e9 / blockNanos;
                blockRates.add(rate);
                blocks++;
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
                    if (PRINTING) {
                        System.out.println("QS rejected: mean verified-relation rate is zero");
                        System.out.println();
                    }
                    return ONE_MAG;
                }
                int remainingRelations = requiredRelations - task.relations.size;
                double projectedQSSeconds = 1.15*remainingRelations / medianRate;
                task.qsSeconds = projectedQSSeconds;
                if (PRINTING)
                    System.out.println("QS probe: blocks=" + blocks + ", relations total=" + task.relations.size
                            + "/" + requiredRelations + ", mean rate=" + medianRate + "/s, projected remaining="
                            + projectedQSSeconds + " seconds");
                qsAccepted = projectedQSSeconds*0.75 < remainingECMSeconds;
                if (!qsAccepted) {
                    if (PRINTING) {
                        System.out.println("QS rejected in favor of ECM");
                        System.out.println();
                    }
                    return ONE_MAG;
                }
                if (PRINTING)
                    System.out.println("QS accepted over remaining ECM work");
                break;
            }
            /*
             * Continue from the relations already collected by the probe.
             * Do not restart QS.
             */
            while (task.relations.size < requiredRelations) {
                if (PRINTING)
                    System.out.printf("%4d", blocks + 1);
                sieve128Polynomials(task.n, sqrt2N, task.factorBase, task.fbIndex, task.relations, task.partials,
                        task.partialGraph, rng, requiredRelations);
                blocks++;
            }
            if (PRINTING)
                System.out.println("Total QS relation time: "
                        + formatSeconds(System.nanoTime() - totalStart));
            validateQSRelations(task.relations, task.factorBase, task.n);
            int originalRelationCount = task.relations.size;
            SparseMatrixSetup setup = buildQSMatrix(task.relations, task.factorBase, task.n);
            long[][] dependencies = nullspaceDependencies(setup, rng, originalRelationCount);
            for (long[] dependency : dependencies) {
                long[] factor = reconstructFactor(dependency, task.relations, task.factorBase, task.n);
                if (isNontrivialFactor(factor, task.n)) {
                    if (PRINTING) {
                        System.out.println("QS FACTOR FOUND: " + magnitudeToString(factor));
                        System.out.println();
                    }
                    return factor;
                }
            }
        } catch (FactorFoundException e) {return e.factor;}
        return ONE_MAG;
    }

    //--- General Number Field Sieve ---//

    private record NFSFactorBaseEntry(int prime, int root, double logPrime) {}
    private static final class NFSFactorBase {
        NFSFactorBaseEntry[] rational;
        NFSFactorBaseEntry[] algebraic;
    }
}