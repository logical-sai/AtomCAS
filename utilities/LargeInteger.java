package com.AtomCAS.utilities;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
public class LargeInteger extends ScalarAtom{
    private static final ThreadMXBean TMX = ManagementFactory.getThreadMXBean();
    private static final long MAX_BITS = 1L << 37;
    private static void reportOverflow() {
        throw new ArithmeticException("Result exceeds supported range: |X| < 2^(2^37)");}
    /**
     * Little-endian limbs where one "digit" is in base 2^64. Any long primitive,
     * negative or positive, is a valid limb. Empty arrays are invalid
     */
    final long[] limbs;
    /**
     * Sign of the integer. "True" means that the number is negative, "false"
     * otherwise. 0 is nonnegative, so negative is set to false for that number
     */
    final boolean negative;
    /**
     * A special object that is used in all calculations that involve randomness
     */
    private static final SecureRandom SECURE_RNG = new SecureRandom();
    private static final SplittableRandom FAST_RNG = new SplittableRandom();
    /**
     * Internal magnitude constants + zero and one checkers
     */
    private static final long[] ZERO_MAG = {0L};
    static boolean isZeroArray(long[] a){
        for (long limb : a)
            if (limb != 0L)
                return false;
        return true;
    }
    private static final long[] ONE_MAG = {1L};
    static boolean isOneArray(long[] x) {return x.length == 1 && x[0] == 1;}
    private static final long[] TWO_MAG = {2L};
    private static final long[] THREE_MAG = {3L};
    private static final long[] FOUR_MAG = {4L};
    private static final long[] FIVE_MAG = {5L};
    private static final long[] SIX_MAG = {6L};
    private static final long[] TEN_MAG = {10L};
    private static final long[] HUNDRED_MAG = {100L};
    private static final long[] THOUSAND_MAG = {1000L};
    /**
     * LargeInteger constant -1
     */
    public static final LargeInteger NEGATIVE_ONE = new LargeInteger(ONE_MAG, true, true);
    /**
     * LargeInteger constant 0
     */
    public static final LargeInteger ZERO = new LargeInteger(ZERO_MAG, false, true);
    /**
     * LargeInteger constant 1
     */
    public static final LargeInteger ONE = new LargeInteger(ONE_MAG, false, true);
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
    @SuppressWarnings("unused")
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
    private LargeInteger(long[] limbs, boolean sign, boolean trimmed){
        if (limbs == null || limbs.length == 0)
            throw new IllegalArgumentException("limbs must be nonempty");
        if(!trimmed) limbs = trimLeadingZeros(limbs);
        this.limbs = limbs;
        negative = !isZeroArray(limbs) && sign;
    }
    private static final LargeInteger[] POS_SMALL = new LargeInteger[31];
    private static final LargeInteger[] NEG_SMALL = new LargeInteger[31];
    static {
        for (int x = 1; x <= 30; x++) {
            POS_SMALL[x] = new LargeInteger(new long[]{x}, false, true);
            NEG_SMALL[x] = new LargeInteger(new long[]{x}, true, true);
        }
    }
    public static LargeInteger valueOf(long x) {
        if (x == 0) return ZERO;
        else if (x > 0 && x <= 30) return POS_SMALL[(int)x];
        else if (x < 0 && x >= -30) return NEG_SMALL[(int)-x];
        else if (x == 100) return HUNDRED;
        else if (x == 1000) return THOUSAND;
        boolean negative = x < 0;
        return new LargeInteger(new long[]{negative ? -x : x}, negative, true);
    }
    @SuppressWarnings("unused")
    public byte byteValueExact() {
        long v = longValueExact();
        if (v < Byte.MIN_VALUE || v > Byte.MAX_VALUE)
            throw new ArithmeticException("Does not fit in byte");
        return (byte) v;
    }
    @SuppressWarnings("unused")
    public short shortValueExact() {
        long v = longValueExact();
        if (v < Short.MIN_VALUE || v > Short.MAX_VALUE)
            throw new ArithmeticException("Does not fit in short");
        return (short) v;
    }
    @SuppressWarnings("unused")
    public int intValueExact() {
        long v = longValueExact();
        if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE)
            throw new ArithmeticException("Does not fit in int");
        return (int) v;
    }
    public long longValueExact() {
        if (limbs.length > 1)
            throw new ArithmeticException("Does not fit in long");
        long mag = limbs[0];
        if (mag < 0) {
            if (negative && mag == Long.MIN_VALUE)
                return Long.MIN_VALUE;
            throw new ArithmeticException("Does not fit in long");
        }
        return negative ? -mag : mag;
    }
    /**
     * @param s The string inputted into the constructor
     * @throws IllegalArgumentException if the inputted string is an expression that does not evaluate to an integer
     */
    public LargeInteger(String s) {
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
        s = s.trim();
        if (s.isEmpty())
            throw new NumberFormatException("Empty string");
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
        limbs = trimLeadingZeros(acc);
        negative = neg && !isZeroArray(limbs);
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
        int n = a.length;
        while (n > 1 && a[n - 1] == 0L) n--;
        return n == a.length ? a : Arrays.copyOf(a, n);
    }
    private static long bitLength(long[] x) {
        if (isZeroArray(x)) return 0;
        int top = x.length - 1;
        while(top > 0 && x[top] == 0L) top--;
        return top * 64L + 64L - Long.numberOfLeadingZeros(x[top]);
    }
    public long bitLength() {return bitLength(limbs);}
    @Override
    public int hashCode() {return CASHash.rational(this, ONE);}
    @Override
    public LargeInteger clone() {return new LargeInteger(limbs.clone(), negative, true);}
    private static String magToString(long[] mag){
        if (mag.length == 1) return Long.toUnsignedString(mag[0]);
        long m = (bitLength(mag) * 1233L >>> 12) / 2;
        long[] power = power(TEN_MAG, m);
        long[][] DR = divideMagnitudes(mag, power);
        String left = magToString(DR[0]);
        String right = magToString(DR[1]);
        StringBuilder sb = new StringBuilder();
        sb.append(left);
        sb.repeat('0', Math.toIntExact(Math.max(m-right.length(), 0)));
        return sb.append(right).toString();
    }
    @Override
    public String toString() {
        int chars = Math.toIntExact((bitLength() * 1233L >>> 12) + 1 + (negative ? 1 : 0));
        StringBuilder sb = new StringBuilder(chars);
        if (negative) sb.append('-');
        return sb.append(magToString(limbs)).toString();
    }
    public String toLatex() {return toString();}
    public BigInteger toBigInteger() {
        if(bitLength() > Integer.MAX_VALUE) return null;
        if (isZeroArray(limbs)) return BigInteger.ZERO;
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
        if (!(other instanceof ScalarAtom)) return false;
        return switch (other) {
            case LargeInteger I -> equalsArray(limbs, I.limbs) && negative == I.negative;
            case LargeFraction F -> equals(F.numerator) && isOneArray(F.denominator.limbs);
            case LargePowerMonomial LPM -> LPM.exponent2.equals(ONE) == negative &&
                    LPM.base.equals(this) && LPM.exponent.equals(ONE);
            case RadicalTerm RT -> RT.rationalPart.equals(this) && RT.exponent.equals(ZERO);
            default -> compareTo((ScalarAtom) other) == 0;
        };
    }
    public LargeInteger sign() {
        if (negative) return NEGATIVE_ONE;
        return isZeroArray(limbs) ? ZERO : ONE;
    }
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
        if (isZeroArray(maxOffset)) return startInclusive;
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
        if(start.negative) return random(ZERO, end.subtract(start)).add(start);
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
    static int compareArray(long[] a, long[] b) {
        int alen = a.length;
        while (alen > 1 && a[alen - 1] == 0L) alen--;
        int bLen = b.length;
        while (bLen > 1 && b[bLen - 1] == 0L) bLen--;
        if (alen != bLen)
            return Integer.compare(alen, bLen);
        for (int i = alen - 1; i >= 0; i--) {
            int cmp = Long.compareUnsigned(a[i], b[i]);
            if (cmp != 0) return cmp;
        }
        return 0;
    }
    private static int compareSmall(long[] a, long b){
        int alen = a.length;
        while (alen > 1 && a[alen - 1] == 0L) alen--;
        if(alen > 1) return 1;
        return Long.compareUnsigned(a[0], b);
    }
    static boolean equalsArray(long[] a, long[] b) {
        if(a == b) return true;
        int alen = a.length;
        while (alen > 1 && a[alen - 1] == 0L) alen--;
        int bLen = b.length;
        while (bLen > 1 && b[bLen - 1] == 0L) bLen--;
        if (alen != bLen)
            return false;
        for (int i = 0; i < alen; i++)
            if (a[i] != b[i]) return false;
        return true;
    }
    @Override
    public int compareMagnitudes(ScalarAtom other) {
        return switch(other){
            case LargeInteger I -> compareArray(limbs, I.limbs);
            case LargeFraction F -> multiply(F.denominator).compareMagnitudes(F.numerator);
            case LargePowerMonomial L -> {
                LargeFraction f = power(L.exponent.denominator);
                LargeFraction o = L.power(L.exponent.denominator).base;
                yield f.compareMagnitudes(o);
            }case RadicalTerm R -> {
                if (other.isNotReal()) {
                    RadicalTermSum RS = (RadicalTermSum) other.multiply(R.complexConjugate());
                    yield square().compareMagnitudes(RS);
                }
                if (equals(R.rationalPart) && R.coefficient.equals(LargeInteger.ZERO)) yield 0;
                LargeFraction f = (LargeFraction) subtract(R.rationalPart).divide(R.coefficient);
                f = f.power(R.exponent.exponent.denominator);
                LargeFraction o = R.exponent.base;
                yield f.compareMagnitudes(o);
            }default -> -other.compareMagnitudes(this);
        };
    }
    @Override
    public int compareTo(ScalarAtom other) {
        if (other.isNotReal()) throw new IllegalStateException("Complex numbers cannot be compared");
        return switch (other) {
            case LargeInteger I -> {
                if(negative == I.negative){
                    yield switch(negative){
                        case false -> compareArray(limbs, I.limbs);
                        case true -> compareArray(I.limbs, limbs);
                    };
                }
                yield !negative ? 1 : -1;
            }case LargeFraction F -> multiply(F.denominator).compareTo(F.numerator);
            case LargePowerMonomial LPM -> {
                LargeFraction f = power(LPM.exponent.denominator);
                LargeFraction o = LPM.power(LPM.exponent.denominator).base;
                int res = f.compareMagnitudes(o);
                if (res == 1) {
                    if (negative) yield -1;
                    yield 1;
                } else if (res == -1) {
                    if (other.lessThan(ZERO)) yield 1;
                    yield -1;
                }
                yield res;
            }default -> {
                long B = compareMagnitudes(other);
                if (B == 1) {
                    if (negative) yield -1;
                    yield 1;
                } else if (B == -1) {
                    if (other.lessThan(ZERO)) yield 1;
                    yield -1;
                }
                yield 0;
            }
        };
    }
    @Override
    public boolean isRational() {return true;}
    @Override
    public boolean isReal() {return true;}
    @Override
    public boolean isAlgebraic() {return true;}
    private static long[] divide128ByNormalized64(long high, long low, long divisor) {
        // Preconditions:
        // divisor has bit 63 set
        // high < divisor unsigned
        if(Long.compareUnsigned(high, divisor) >= 0)
            throw new ArithmeticException("divide128ByNormalized64 requires high < divisor");

        int u3 = (int) (high >>> 32);
        int u2 = (int) high;
        int u1 = (int) (low >>> 32);
        int u0 = (int) low;

        int v1 = (int) (divisor >>> 32);
        int v0 = (int) divisor;

        DivRem3By2 first = divideThreeByTwo(u3, u2, u1, v1, v0);
        DivRem3By2 second = divideThreeByTwo(first.remHigh, first.remLow, u0, v1, v0);
        long quotient = (Integer.toUnsignedLong(first.quotient) << 32) | Integer.toUnsignedLong(second.quotient);

        long remainder = (Integer.toUnsignedLong(second.remHigh) << 32) | Integer.toUnsignedLong(second.remLow);
        return new long[]{quotient, remainder};
    }
    private static long[] addSmall(long[] addend, long adder) {
        addend = trimLeadingZeros(addend);
        if (addend.length == 1) {
            long low = addend[0] + adder;
            return Long.compareUnsigned(low, addend[0]) < 0 ? new long[]{low, 1L} : new long[]{low};
        }
        int n = addend.length;
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
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);
        if (a.length == 1)
            return addSmall(b, a[0]);
        if (b.length == 1)
            return addSmall(a, b[0]);
        if (a.length < b.length) {
            long[] t = a;
            a = b;
            b = t;
        }
        long[] res = new long[a.length + 1];
        System.arraycopy(a, 0, res, 0, a.length);
        addMagnitudeInPlace(res, b, 0);
        return trimLeadingZeros(res);
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
    private static void addMagnitudeInPlace(long[] out, long[] x, int offset) {
        int xLen = x.length;
        while (xLen > 1 && x[xLen - 1] == 0L) xLen--;
        if (xLen == 1 && x[0] == 0L) return;
        if (offset + xLen > out.length)
            throw new AssertionError("term does not fit: " + "offset=" + offset + ", xLen=" + xLen +
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
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);
        int cmp = compareArray(a, b);
        if (cmp < 0)
            throw new IllegalArgumentException("subtractMagnitudes requires a >= b");
        if (cmp == 0)
            return ZERO_MAG;
        int n = a.length;
        int m = b.length;
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
        if(borrow != 0)
            throw new ArithmeticException("subtractMagnitudes underflow");
        return trimLeadingZeros(out);
    }
    private static long[] subtractSmall(long[] a, long small) {
        a = trimLeadingZeros(a);
        if (small == 0L) return a;
        if (a.length == 1) {
            int cmp = Long.compareUnsigned(a[0], small);
            if (cmp < 0)
                throw new ArithmeticException("subtractSmall requires a >= small");
            if (cmp == 0) return ZERO_MAG;
            return new long[]{a[0] - small};
        }
        if (a.length == 2) {
            long low = a[0] - small;
            if (Long.compareUnsigned(a[0], small) >= 0)
                return new long[]{low, a[1]};
            long high = a[1] - 1L;
            if (high != 0L) return new long[]{low, high};
            // Result contracted from 2 limbs to 1.
            return new long[]{low};
        }
        long[] out = new long[a.length];
        long low = a[0] - small;
        out[0] = low;
        if (Long.compareUnsigned(a[0], small) >= 0) {
            System.arraycopy(a, 1, out, 1, a.length - 1);
            return out;
        }
        for (int i = 1; i < a.length; i++) {
            long x = a[i];
            if (x != 0L) {
                out[i] = x - 1;
                System.arraycopy(a, i + 1, out, i + 1, a.length - i - 1);
                if (i == a.length - 1 && out[i] == 0L)
                    return trimLeadingZeros(out);
                return out;
            }
            out[i] = -1L;
        }
        return trimLeadingZeros(out);
    }
    private static void subtractMagnitudeInPlace(long[] a, long[] b) {
        if (compareArray(a, b) < 0)
            throw new IllegalArgumentException("subtractMagnitudeInPlace requires a >= b\n" +
                    "a = " + new LargeInteger(trimLeadingZeros(a), false, true) + "\n" +
                    "b = " + new LargeInteger(trimLeadingZeros(b), false, true));

        int bLen = b.length;
        while (bLen > 0 && b[bLen - 1] == 0L)
            bLen--;

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
            a[i] = ai - 1L;
            borrow = ai == 0L ? 1L : 0L;
            i++;
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
        if (isZeroArray(limbs)) return other;
        if (isZeroArray(other.limbs)) return this;
        if (negative == other.negative)
            return new LargeInteger(addMagnitudes(limbs, other.limbs), negative, true);
        int cmp = compareMagnitudes(other);
        if (cmp == 0) return ZERO;
        if (cmp > 0)
            return new LargeInteger(subtractMagnitudes(limbs, other.limbs), negative, true);
        return new LargeInteger(subtractMagnitudes(other.limbs, limbs), other.negative, true);
    }
    @Override
    public ScalarAtom add(ScalarAtom other) {
        return switch (other) {
            case LargeInteger I -> add(I);
            case LargeFraction F -> new LargeFraction(F.denominator.multiply(this)
                    .add(F.numerator), F.denominator);
            case LargePowerMonomial LPM -> new RadicalTerm(new LargeFraction(this, ONE),
                    LargeFraction.ONE, LPM);
            case RadicalTerm RT -> new RadicalTerm((LargeFraction) add(RT.rationalPart), RT.coefficient,
                    RT.exponent);
            case RadicalTermSum RTS -> new RadicalTermSum(RTS.sum, (LargeFraction) RTS.constant.add(this));
            case LargePowerNode LPN -> new RadicalTermNode(new LargeFraction(this, ONE),
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
            return add(new LargeInteger(Long.toString(other)));
        long[] otherMag = {Math.abs(other)};
        if (negative == otherSign)
            return new LargeInteger(addMagnitudes(limbs, otherMag), negative, true);
        int cmp = compareArray(limbs, otherMag);
        if (cmp == 0) return ZERO;
        else if (cmp > 0)
            return new LargeInteger(subtractMagnitudes(limbs, otherMag), negative, true);
        return new LargeInteger(subtractMagnitudes(otherMag, limbs), otherSign, true);
    }
    public LargeInteger subtract(LargeInteger other) {
        if (isZeroArray(limbs))
            return other.negate();
        if (isZeroArray(other.limbs))
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
        if (isZeroArray(x)) return ZERO_MAG;
        if (limbs == 0) return x;
        if(limbs < 0) return shiftRightLimbs(x, -limbs);
        long[] out = new long[x.length + limbs];
        // pad zeros at the beginning
        System.arraycopy(x, 0, out, limbs, x.length);
        return trimLeadingZeros(out);
    }
    private static long[] shiftLeftBits(long[] n, long shift) {
        if (isZeroArray(n)) return ZERO_MAG;
        if (shift < 0) return shiftRightBits(n, -shift);
        if (shift == 0) return trimLeadingZeros(n);
        if(bitLength(n) + shift > MAX_BITS)
            reportOverflow();
        int limbShift = (int) shift >>> 6;
        int bitShift = (int) shift & 63;
        long[] out = new long[n.length + limbShift + 1];
        if (bitShift == 0){
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
        if (isZeroArray(x) || limbs >= x.length) return ZERO_MAG;
        if (limbs < 0) return shiftLeftLimbs(x, -limbs);
        long[] out = new long[x.length - limbs];
        System.arraycopy(x, limbs, out, 0, x.length - limbs);
        return trimLeadingZeros(out);
    }
    private static long[] shiftRightBits(long[] x, long bits) {
        if (isZeroArray(x)) return ZERO_MAG;
        if (bits < 0) return shiftLeftBits(x, -bits);
        else if (bits >= 64) return shiftRightBits(
                shiftRightLimbs(x, Math.toIntExact(bits / 64)), bits & 63);
        else if (bits == 0) return x;
        long[] out = new long[x.length];
        long carry = 0L;
        for (int i = x.length - 1; i >= 0; i--) {
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
        int bLen = b.length;
        while (bLen > 1 && b[bLen - 1] == 0L)
            bLen--;
        long[] bt = trimLeadingZeros(Arrays.copyOf(b, bLen));
        long[] shifted = shiftLeftBits(bt, shift);

        if (compareArray(a, shifted) < 0)
            throw new AssertionError(where +
                    " : a < (b << " + shift + "), a_Len=" +
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
    /**
     * Returns a LargeInteger whose value is {@code this << shift}
     */
    public LargeInteger shiftLeft(long shift){
        if (shift == 0) return this;
        return new LargeInteger(shiftLeftBits(limbs, shift), negative, true);
    }
    /**
     * Returns a LargeInteger whose value is {@code this >> shift}
     */
    public LargeInteger shiftRight(long shift){
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
    @SuppressWarnings("unused")
    public boolean testBit(long bit){return testBit(limbs, bit);}
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
        if (m == 0L || isZeroArray(a))
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
     * smaller operand is in the range from this to the Toom-Cook threshold, then the Karatsuba algorithm will
     * be chosen. This value was found via experimental heatmaps to work well.
     */
    private static final int KARATSUBA_THRESHOLD = 28;
    /**
     * Threshold for switching from Karatsuba to the Toom-Cook 3 algorithm in regular multiply.If the
     * number of limbs in the smaller operand is greater than or equal to this constant,hen the Toom
     * algorithm will be chosen. This value was found via experimental heatmaps to work well.
     */
    private static final int TOOM_COOK_3_THRESHOLD = 120;
    static long[] multiplyMagnitudes(long[] a, long[] b){
        if(Integer.compareUnsigned(a.length+b.length, Integer.MAX_VALUE) > 0) reportOverflow();
        return multiplyMagnitudesWithThresholds(a, b, KARATSUBA_THRESHOLD, TOOM_COOK_3_THRESHOLD);
    }
    private static void multiplyInto(long[] a, long[] b, long[] out){
        Arrays.fill(out, 0);
        if (out.length == 0 || isZeroArray(a) || isZeroArray(b)) return;
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
            for (int i = 0; i < aLimit; i++) {
                int oi = k + i;
                long x = a[i];
                long low = x * scalar;
                long high = Math.unsignedMultiplyHigh(x, scalar);
                long t = low + out[oi];
                if (Long.compareUnsigned(t, low) < 0) high++;
                long r = t + carry;
                if (Long.compareUnsigned(r, t) < 0) high++;
                out[oi] = r;
                carry = high;
            }
            /*
             * If A was exhausted before the output buffer was exhausted,
             * the carry belongs in the next limb. Otherwise, that carry is
             * beyond out[] and is intentionally discarded.
             */
            int oi = k + aLimit;
            while (carry != 0L && oi < out.length) {
                long old = out[oi];
                long r = old + carry;
                out[oi] = r;
                carry = Long.compareUnsigned(r, old) < 0 ? 1L : 0L;
                oi++;
            }
        }
    }
    public static long[] multiplyMagnitudesWithThresholds(long[] a, long[] b, int KT, int T3T) {
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);
        if (isZeroArray(a) || isZeroArray(b)) return ZERO_MAG;
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
        }else if (min < T3T){
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
            long[] z0 = multiplyMagnitudesWithThresholds(lower1, lower2, KT, T3T);
            long[] z2 = multiplyMagnitudesWithThresholds(upper1, upper2, KT, T3T);
            long[] z1 = multiplyMagnitudesWithThresholds(addMagnitudes(lower1, upper1),
                    addMagnitudes(lower2, upper2), KT, T3T);
            subtractMagnitudeInPlace(z1, z0);
            subtractMagnitudeInPlace(z1, z2);
            addMagnitudeInPlace(out, z0, 0);
            addMagnitudeInPlace(out, z1, pivot);
            addMagnitudeInPlace(out, z2, pivot * 2);
            return trimLeadingZeros(out);
        }
/*
Toom-Cook 3 Algorithm

---INITIALIZATION---

Let beta = 2^64 be the limb base and let k = ceil(n/3), where n is
the number of limbs in the larger operand. Then define X = beta^k.
Split each operand into three approximately equal pieces:

A = a2*X^2 + a1*X + a0
B = b2*X^2 + b1*X + b0

We can treat these as the polynomials

p(x) = a2*x^2 + a1*x + a0
q(x) = b2*x^2 + b1*x + b0

The original integers are recovered by evaluating them at x = X:

A = p(X)
B = q(X)

Therefore

A*B = p(X)q(X)

and it is sufficient to determine the product polynomial

r(x) = p(x)q(x) = r4*x^4 + r3*x^3 + r2*x^2 + r1*x + r0.

---POINT EVALUATION---

Karatsuba uses the points 0, 1, and infinity.
Toom-Cook 3 needs five points because the product polynomial has
degree 4 and therefore has five coefficients. The point values define
the interpolation strategy, but they do not depend on the inputs and can be hardocded.
For this implementation, points 0, 1, 2, -1, and infinity will be used

p0   = p(0)  = a0
p1   = p(1)  = a2 + a1 + a0
p2   = p(2)  = 4*a2 + 2*a1 + a0
pM1  = p(-1) = a2 - a1 + a0
pInf = p(infinity) = a2

q0   = q(0)  = b0
q1   = q(1)  = b2 + b1 + b0
q2   = q(2)  = 4*b2 + 2*b1 + b0
qM1  = q(-1) = b2 - b1 + b0
qInf = q(infinity) = b2

---MULTIPLICATION---

Standard grade-school multiplication would compute 9 products:

a0*b0, a0*b1, a0*b2,
a1*b0, a1*b1, a1*b2,
a2*b0, a2*b1, a2*b2

Toom-Cook 3 instead needs only 5 recursive products:

v0   = p0*q0
v1   = p1*q1
v2   = p2*q2
vM1  = pM1*qM1
vInf = pInf*qInf

These are evaluations of r(x):

v0   = r(0)
v1   = r(1)
v2   = r(2)
vM1  = r(-1)
vInf = r(infinity)


---INTERPOLATION---

This is probably the hardest step.
We know

r(x) = r0 + r1*x + r2*x^2 + r3*x^3 + r4*x^4

so our five evaluations give

[ v0   ]   [ 1   0   0   0   0 ] [ r0 ]
[ v1   ]   [ 1   1   1   1   1 ] [ r1 ]
[ v2   ] = [ 1   2   4   8  16 ] [ r2 ]
[ vM1  ]   [ 1  -1   1  -1   1 ] [ r3 ]
[ vInf ]   [ 0   0   0   0   1 ] [ r4 ]

The first and last coefficients are immediate:

r0 = v0
r4 = vInf

Remove their contributions from the x = 1 and x = -1 evaluations:

s1  = v1  - r0 - r4
    = r1 + r2 + r3

sM1 = vM1 - r0 - r4
    = -r1 + r2 - r3

Adding these equations eliminates r1 and r3:

s1 + sM1 = 2*r2

so

r2 = (s1 + sM1)/2

Subtracting them gives

s1 - sM1 = 2*(r1 + r3)

so define

t = (s1 - sM1)/2
  = r1 + r3

Now use the x = 2 evaluation:

v2 = r0 + 2*r1 + 4*r2 + 8*r3 + 16*r4

Remove r0 and 16*r4, then divide by 2:

u = (v2 - r0 - 16*r4)/2
  = r1 + 2*r2 + 4*r3

Since

t = r1 + r3,

subtract t and 2*r2:

u - t - 2*r2 = 3*r3

therefore

r3 = (u - t - 2*r2)/3

and finally

r1 = t - r3

Thus, the interpolation can be computed as:
r0 = v0
r4 = vInf
s1  = v1  - r0 - r4
sM1 = vM1 - r0 - r4
r2 = (s1 + sM1)/2
t  = (s1 - sM1)/2
u  = (v2 - r0 - 16*r4)/2
r3 = (u - t - 2*r2)/3
r1 = t - r3
All divisions above are exact. No rounding is involved.

---RECOMPOSITION---

Recall that X = beta^k = 2^(64k).
The final integer product is obtained by evaluating r at X:
A*B = r(X)
    = r0
    + r1*X
    + r2*X^2
    + r3*X^3
    + r4*X^4

Because X is an exact power of the limb base, multiplication by X^j
is simply a shift by j*k limbs.

The coefficients r1, r2, and r3 may temporarily be negative during
interpolation, so the implementation must either support signed
intermediate values or carefully handle subtraction before the final
recomposition.

---COMPLEXITY---

Each recursive multiplication operates on approximately n/3 limbs,
and there are 5 recursive multiplications.

Therefore, the recurrence is

T(n) = 5*T(n/3) + O(n)

where the O(n) term consists of point evaluation, interpolation,
addition/subtraction, exact small divisions, and recomposition.

By the Master Theorem,

T(n) = O(n^(log_3(5)))

and

log_3(5) ≈ 1.465

so the asymptotic runtime is approximately

O(n^1.465).

This improves on Karatsuba's

O(n^(log_2(3))) ≈ O(n^1.585),

at the cost of substantially more linear-time addition,
subtraction, and interpolation overhead. Because of that overhead,
Toom-Cook 3 is normally only used above a larger operand-size
threshold, with smaller recursive products falling back to
Karatsuba or grade-school multiplication.
*/
        int pivot = (max + 2) / 3;

        long[] a0 = sliceFixed(a, 0, pivot);
        long[] a1 = sliceFixed(a, pivot, pivot);
        long[] a2 = sliceFixed(a, 2 * pivot, pivot);
        long[] b0 = sliceFixed(b, 0, pivot);
        long[] b1 = sliceFixed(b, pivot, pivot);
        long[] b2 = sliceFixed(b, 2 * pivot, pivot);

        long[] p1 = addThreeMagnitudes(a0, a1, a2);
        long[] pm1Mag = addMagnitudes(a0, a2);
        boolean pm1Negative = compareArray(pm1Mag, a1) < 0;
        if (!pm1Negative) subtractMagnitudeInPlace(pm1Mag, a1);
        else pm1Mag = subtractMagnitudes(a1,  pm1Mag);
        long[] p2 = addThreeMagnitudes(a0, shiftLeftBits(a1, 1), shiftLeftBits(a2, 2));
        long[] q1 = addThreeMagnitudes(b0, b1, b2);
        long[] qm1Mag = addMagnitudes(b0, b2);
        boolean qm1Negative = compareArray(qm1Mag, b1) < 0;
        if(!qm1Negative) subtractMagnitudeInPlace(qm1Mag, b1);
        else qm1Mag = subtractMagnitudes(b1,  qm1Mag);
        long[] q2 = addThreeMagnitudes(b0, shiftLeftBits(b1, 1), shiftLeftBits(b2, 2));

        long[] v0 = multiplyMagnitudesWithThresholds(a0, b0, KT, T3T);
        long[] v1 = multiplyMagnitudesWithThresholds(p1, q1, KT, T3T);
        long[] vm1Mag = multiplyMagnitudesWithThresholds(pm1Mag, qm1Mag, KT, T3T);
        boolean vm1Negative = pm1Negative ^ qm1Negative;
        long[] v2 = multiplyMagnitudesWithThresholds(p2, q2, KT, T3T);
        long[] vInf = multiplyMagnitudesWithThresholds(a2, b2, KT, T3T);

        long[] r2;
        long[] s;
        if (vm1Negative) {
            // v1 + vm1 = v1 - |vm1|
            r2  = subtractMagnitudes(v1, vm1Mag);
            // v1 - vm1 = v1 + |vm1|
            s = addMagnitudes(v1, vm1Mag);
        } else {
            r2  = addMagnitudes(v1, vm1Mag);
            s = subtractMagnitudes(v1, vm1Mag);
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

        v0   = trimLeadingZeros(v0);
        s    = trimLeadingZeros(s);
        r2   = trimLeadingZeros(r2);
        v2   = trimLeadingZeros(v2);
        vInf = trimLeadingZeros(vInf);
        addMagnitudeInPlace(out, v0, 0);
        addMagnitudeInPlace(out, s, pivot);
        addMagnitudeInPlace(out, r2, 2*pivot);
        addMagnitudeInPlace(out, v2, 3*pivot);
        addMagnitudeInPlace(out, vInf, 4*pivot);
        return trimLeadingZeros(out);
    }
    public LargeInteger multiply(LargeInteger other){
        if(equals(other)) return square();
        return new LargeInteger(
                multiplyMagnitudes(limbs, other.limbs), negative ^ other.negative, true);
    }
    @Override
    public ScalarAtom multiply(ScalarAtom other){
        return switch (other) {
            case LargeInteger I -> multiply(I);
            case LargeFraction F -> new LargeFraction(multiply(F.numerator), F.denominator);
            case LargePowerMonomial largePowerMonomial -> new LargePowerMonomial(
                    new LargeFraction(this, ONE), LargeFraction.ONE).multiply(largePowerMonomial);
            case RadicalTermSum RTS -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                for (LargePowerMonomial L : RTS.sum.keys())
                    rt.add(new RadicalTerm(LargeFraction.ZERO, (LargeFraction) multiply(RTS.sum.get(L)), L));
                yield new RadicalTermSum(rt, (LargeFraction) multiply(RTS.constant));
            }case LargePowerNode LPN -> new LargePowerNode(LPN.base.multiply(power(LPN.exponent.reciprocal())),
                    LPN.exponent, LPN.signFactor);
            default -> other.multiply(this);
        };
    }
    LargeInteger multiply(long greg) {
        return new LargeInteger(multiplySmall(limbs, Math.abs(greg)), negative ^ (greg < 0), true);}
    private static final int KARATSUBA_SQUARE_THRESHOLD = 48;
    private static final int TOOM_SQUARE_THRESHOLD = 128;
    private static long[] squareMagnitude(long[] a){
        if(Integer.compareUnsigned(2*a.length, Integer.MAX_VALUE) > 0) reportOverflow();
        return squareMagnitudeWithThresholds(a, KARATSUBA_SQUARE_THRESHOLD, TOOM_SQUARE_THRESHOLD);
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

        int topI = Math.min(n - 1, (m - 1) >>> 1);

        for (int i = topI; i >= 0; i--) {
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
            int firstJ = i + 1;
            if (firstJ >= n || i + firstJ >= m) break;
            long ai = a[i];
            long carry = 0L;
            int jLimit = Math.min(n, m - i);
            for (int j = firstJ; j < jLimit; j++) {
                int k = i + j;
                long aj = a[j];
                long low  = ai * aj;
                long high = Math.unsignedMultiplyHigh(ai, aj);
                long t = low + out[k];
                if (Long.compareUnsigned(t, low) < 0) high++;
                long lo = t + carry;
                if (Long.compareUnsigned(lo, t) < 0) high++;
                out[k] = lo;
                carry = high;
            }
            // Any carry beyond out.length is deliberately discarded.
            int k = i + jLimit;
            while (carry != 0L && k < m) {
                long old = out[k];
                long sum = old + carry;
                out[k] = sum;
                carry = Long.compareUnsigned(sum, old) < 0 ? 1L : 0L;
                k++;
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
    private static long[] squareMagnitudeWithThresholds(long[] a, int KT, int T3T) {
        if (isZeroArray(a)) return a;
        int n = a.length;
        if (n == 1) {
            long a0 = a[0];
            long low = a0*a0;
            long high = Math.unsignedMultiplyHigh(a0, a0);
            return high == 0 ? new long[]{low} : new long[]{low, high};
        }
        long[] out = new long[n << 1];
        if (n < KT) {
            squareInto(a, out);
            return trimLeadingZeros(out);
        }else if (n < T3T){
            int m = (n+1) >>> 1;
            long[] x0 = sliceFixed(a, 0, m);
            long[] x1 = slice(a, m, n);
            long[] z0 = squareMagnitudeWithThresholds(x0, KT, T3T);
            long[] z2 = squareMagnitudeWithThresholds(x1, KT, T3T);
            long[] z1 = squareMagnitudeWithThresholds(addMagnitudes(x0, x1), KT, T3T);
            subtractMagnitudeInPlace(z1, z0);
            subtractMagnitudeInPlace(z1, z2);
            addMagnitudeInPlace(out, z0, 0);
            addMagnitudeInPlace(out, z1, m);
            addMagnitudeInPlace(out, z2, 2 * m);
            return trimLeadingZeros(out);
        }
        /*
Toom-Cook 3 Squaring

This is the squaring-specific version of Toom-Cook 3. It uses the
same five evaluation points as general Toom-3 multiplication, but
because both operands are identical, only five recursive squares are
required rather than five general multiplications.

---INITIALIZATION---

Let beta = 2^64 be the limb base and let

m = ceil(n/3)

where n is the number of limbs in the input.

Define

X = beta^(m).

Split the input into three approximately equal pieces:

a = a2*X^2 + a1*X + a0

and interpret it as the polynomial

p(x) = a2*x^2 + a1*x + a0.

Then

a^2 = p(X)^2.

Define the degree-4 product polynomial

r(x) = p(x)^2
     = r0 + r1*x + r2*x^2 + r3*x^3 + r4*x^4.

Expanding directly shows that

r0 = a0^2
r1 = 2*a0*a1
r2 = a1^2 + 2*a0*a2
r3 = 2*a1*a2
r4 = a2^2.

All five final coefficients are therefore nonnegative magnitudes.


---POINT EVALUATION---

Evaluate p(x) at the Toom-3 points 0, 1, 2, -1, and infinity:

p0   = p(0)  = a0
p1   = p(1)  = a0 + a1 + a2
p2   = p(2)  = a0 + 2*a1 + 4*a2
pM1  = p(-1) = a0 - a1 + a2
pInf = p(infinity) = a2

The -1 evaluation may itself be negative. However, unlike general
multiplication, its sign does not need to be stored:

vM1 = p(-1)^2 = |p(-1)|^2.

Therefore the implementation may calculate

pm1 = |a0 + a2 - a1|

and square only its magnitude.


---RECURSIVE SQUARING---

Square the five evaluated values:

v0   = p0^2
v1   = p1^2
v2   = p2^2
vM1  = pM1^2
vInf = pInf^2

These are exactly

v0   = r(0)
v1   = r(1)
v2   = r(2)
vM1  = r(-1)
vInf = r(infinity).

Thus five recursive squares replace the nine chunk products that
would arise from directly expanding a three-part square.


---INTERPOLATION---

We know

r(x) = r0 + r1*x + r2*x^2 + r3*x^3 + r4*x^4.

Therefore

v0   = r0

v1   = r0 + r1 + r2 + r3 + r4

vM1  = r0 - r1 + r2 - r3 + r4

v2   = r0 + 2*r1 + 4*r2 + 8*r3 + 16*r4

vInf = r4.


The outer coefficients are immediate:

r0 = v0
r4 = vInf.


First recover the combined odd-degree contribution.

Subtract the -1 evaluation from the +1 evaluation:

v1 - vM1
    = 2*r1 + 2*r3.

Therefore

s = (v1 - vM1)/2
  = r1 + r3.


Now recover r2.

From

v1 = r0 + r1 + r2 + r3 + r4

and

s = r1 + r3,

we obtain

r2 = v1 - s - r0 - r4.


Next use the x = 2 evaluation.

Remove r0 and 16*r4 and divide by 2:

t = (v2 - r0 - 16*r4)/2

  = r1 + 2*r2 + 4*r3.


Since

s = r1 + r3,

then

t - s - 2*r2
    = 3*r3.

Therefore

r3 = (t - s - 2*r2)/3.

Finally,

r1 = s - r3.


Thus, interpolation is:

r0 = v0
r4 = vInf

s  = (v1 - vM1)/2
r2 = v1 - s - r0 - r4

t  = (v2 - r0 - 16*r4)/2
r3 = (t - s - 2*r2)/3
r1 = s - r3

All divisions by 2 and 3 are exact.

---WHY MAGNITUDE-ONLY INTERPOLATION WORKS---

The temporary evaluation p(-1) may be negative, but squaring removes
its sign:

p(-1)^2 = |p(-1)|^2.

Furthermore, the recovered coefficients are

r1 = 2*a0*a1
r2 = a1^2 + 2*a0*a2
r3 = 2*a1*a2,

which are all nonnegative because a0, a1, and a2 are magnitudes.

Also,

p(1)^2 >= p(-1)^2,

so

v1 - vM1 >= 0.

Consequently, the entire interpolation after evaluating |-1| can be
performed using unsigned magnitude arrays without a separate sign
representation.


---VARIABLE REUSE IN THIS IMPLEMENTATION---

To reduce temporary allocations, several evaluation arrays are
overwritten during interpolation.

Initially:

v0   = r(0)
v1   = r(1)
vm1  = r(-1)
v2   = r(2)
vInf = r(infinity)

Then:

s  = (v1 - vm1)/2
   = r1 + r3

v1 is transformed into:

v1 = v1 - s - v0 - vInf
   = r2

v2 is transformed first into:

v2 = (v2 - v0 - 16*vInf)/2
   = t

and then into:

v2 = (v2 - s - 2*v1)/3
   = r3

Finally:

s = s - v2
  = r1.

Therefore, immediately before recomposition:

v0   = r0
s    = r1
v1   = r2
v2   = r3
vInf = r4.

---RECOMPOSITION---

Recall that

X = beta^(m) = 2^(64m).

Then

a^2 = r(X)  = r0 + r1*X + r2*X^2 + r3*X^3+ r4*X^4.

Because X is exactly one block of m limbs, multiplication by X^j
corresponds to shifting the coefficient by j*m limbs.
Thus, the five coefficients are accumulated at offsets 0, m, 2*m, 3*m, 4*m.

---COMPLEXITY---

Each recursive square operates on approximately n/3 limbs and there
are five recursive calls.

Therefore

T(n) = 5*T(n/3) + O(n),

giving

T(n) = O(n^(log_3(5)))  ≈ O(n^1.465).
As with general Toom-Cook 3 multiplication, the larger linear-time
evaluation and interpolation overhead means this method is useful
only above a sufficiently large threshold. Smaller recursive squares
fall back to Karatsuba squaring or ordinary quadratic squaring.
*/
        int m = (n + 2) / 3;
        //a = a2*B^2 + a1*B + a0
        long[] a0 = sliceFixed(a, 0, m);
        long[] a1 = sliceFixed(a, m, m);
        long[] a2 = slice(a, m * 2, n);
        //evaluate points
        long[] p1 = addThreeMagnitudes(a2, a1, a0);
        long[] pm1 = addMagnitudes(a2, a0);
        if(compareArray(pm1, a1) >= 0) subtractMagnitudeInPlace(pm1, a1);
        else pm1 = subtractMagnitudes(a1, pm1);
        long[] p2 = addThreeMagnitudes(shiftLeftBits(a2, 2), shiftLeftBits(a1, 1), a0);
        //square each point
        long[] v0 = squareMagnitudeWithThresholds(a0, KT, T3T);
        long[] v1 = squareMagnitudeWithThresholds(p1,  KT, T3T);
        long[] vm1 = squareMagnitudeWithThresholds(pm1, KT, T3T);
        long[] v2 = squareMagnitudeWithThresholds(p2, KT, T3T);
        long[] vInf = squareMagnitudeWithThresholds(a2, KT, T3T);
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
        addMagnitudeInPlace(out, v0,   0);
        addMagnitudeInPlace(out, s,   m);
        addMagnitudeInPlace(out, v1, 2*m);
        addMagnitudeInPlace(out, v2, 3*m);
        addMagnitudeInPlace(out, vInf,4*m);
        return trimLeadingZeros(out);
    }
    private static long[] cubeMagnitude(long[] x) {return multiplyMagnitudes(squareMagnitude(x), x);}
    @Override
    public LargeInteger square() {return new LargeInteger(squareMagnitude(limbs), false, true);}
    private static long[] sqrtFloorMagnitude(long[] n) {
        n = trimLeadingZeros(n);
        if (isZeroArray(n)) return ZERO_MAG;
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
            }else root = shiftLeftBits(root, 1);
        }
        return trimLeadingZeros(root);
    }
    private static long[] sqrtCeilMagnitude(long[] mag) {
        long[] C = sqrtFloorMagnitude(mag);
        if (equalsArray(squareMagnitude(C), mag)) return C;
        return addSmall(C, 1);
    }
    @SuppressWarnings("unused")
    public LargeInteger sqrt(){
        if(negative) throw new IllegalArgumentException(
                "Sqrt complex for negative numbers; this method returns an integer");
        return new LargeInteger(sqrtFloorMagnitude(limbs), false, true);
    }
    private static long[] nthRootFloorMagnitude(long[] n, long exp) {
        if (exp <= 0) throw new IllegalArgumentException("exp must be positive");
        n = trimLeadingZeros(n);
        if (isZeroArray(n) || isOneArray(n) || exp == 1) return n;
        if (exp == 2) return sqrtFloorMagnitude(n);
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
        long[] result = ONE_MAG;
        for (int i = 0; i < exp; i++) {
            result = multiplyMagnitudes(result, base);
            if (compareArray(result, limit) > 0) return 1;
        }
        return compareArray(result, limit);
    }
    public LargeInteger cube() {return new LargeInteger(cubeMagnitude(limbs), negative, true);}
    public static LargeInteger productRange(LargeInteger left, LargeInteger right) {
        if (left.equals(right)) return left;
        if (right.subtract(left).equals(ONE)) return left.multiply(right);
        LargeInteger mid = left.add(right).shiftRight(1);
        return productRange(left, mid).multiply(productRange(mid.add(1), right));
    }
    public LargeInteger factorial() {
        if (negative) throw new ArithmeticException("Factorial undefined for negative integers");
        if (isZeroArray(limbs) || isOneArray(limbs)) return ONE;
        return productRange(ONE, this);
    }
    public LargeInteger nCr(LargeInteger l) {
        if (lessThan(l)) return ZERO;
        else if (isZeroArray(l.limbs)) return ONE;
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
    private record DivRem3By2(int quotient, int remHigh, int remLow) {}
    private static DivRem3By2 divideThreeByTwo(int a1, int a2, int a3, int b1, int b2) {
        long A1 = Integer.toUnsignedLong(a1);
        long A2 = Integer.toUnsignedLong(a2);
        long A3 = Integer.toUnsignedLong(a3);
        long B1 = Integer.toUnsignedLong(b1);
        long B2 = Integer.toUnsignedLong(b2);
        if (B1 == 0)
            throw new ArithmeticException("Division by zero");

        if (A1 > B1)
            throw new IllegalArgumentException(
                    "Three-by-two quotient does not fit one word");

        // Expected for the Knuth/BZ use case.
        if ((b1 & 0x8000_0000) == 0)
            throw new IllegalArgumentException(
                    "Divisor must be normalized");

        long q;
        long rHat;

        if (A1 == B1) {
            // qhat would be beta, so saturate at beta - 1.
            q = U32_MASK;
            rHat = A2 + B1;
        } else {
            long top = (A1 << 32) | A2;

            q = Long.divideUnsigned(top, B1);
            rHat = Long.remainderUnsigned(top, B1);
        }

        long qTimesB2 = q * B2;

        /*
         * Correct q while
         *
         * q*b2 > rHat*beta + a3.
         *
         * Once rHat >= beta, the RHS exceeds anything q*b2 can be,
         * so correction is finished.
         */
        while (rHat < U32_BASE) {
            long right = (rHat << 32) | A3;
            if (Long.compareUnsigned(qTimesB2, right) <= 0) break;
            q--;
            rHat += B1;
            qTimesB2 -= B2;
        }
        /*
         * Form q * (b1*beta + b2).
         */
        long p0 = q * B2;
        long p0Low = p0 & U32_MASK;
        long carry = p0 >>> 32;

        long p1 = q * B1 + carry;
        long p1Low = p1 & U32_MASK;
        long p2 = p1 >>> 32;
        /*
         * [a1,a2,a3] - [p2,p1Low,p0Low].
         */
        long t = A3 - p0Low;
        long borrow = t < 0 ? 1L : 0L;
        long r0 = t & U32_MASK;
        t = A2 - p1Low - borrow;
        borrow = t < 0 ? 1L : 0L;
        long r1 = t & U32_MASK;
        t = A1 - p2 - borrow;
        if (t != 0)
            throw new ArithmeticException("Internal three-by-two division error");
        return new DivRem3By2((int) q, (int) r1, (int) r0);
    }
    private static DivRem divideByLong(long[] a, long divisor) {
        if (divisor == 0)
            throw new ArithmeticException("Division by zero");
        long[] q = new long[a.length];
        long rem = 0L;
        for (int i = a.length - 1; i >= 0; i--) {
            /*
              Grade-school division in base 2^64:
              high / divisor = qHigh R rHigh
              then bring down low:
              (rHigh * 2^64 + low) / divisor = qLow R remainder
              Since rHigh < divisor, qLow fits in one 64-bit limb.
             */
            long qHigh = Long.divideUnsigned(rem, divisor);
            if(qHigh != 0)
                throw new ArithmeticException("Quotient overflow");
            long rHigh = Long.remainderUnsigned(rem, divisor);
            /*
             * Normalize divisor so bit 63 is set.
             */
            int shift = Long.numberOfLeadingZeros(divisor);
            long d;
            long nHigh;
            long nLow;
            if (shift == 0) {
                d = divisor;
                nHigh = rHigh;
                nLow = a[i];
            } else {
                d = divisor << shift;
                /*
                  This cannot overflow beyond 128 bits:
                  rHigh < divisor
                  so after normalization rHigh << shift < 2^64.
                 */
                nHigh = (rHigh << shift) | (a[i] >>> (64 - shift));
                nLow = a[i] << shift;
            }
            /*
              Split the normalized 128-bit dividend:
              [u3 u2 u1 u0]
              and 64-bit divisor:
              [v1 v0]
              into unsigned 32-bit words.
             */
            int u3 = (int) (nHigh >>> 32);
            int u2 = (int) nHigh;
            int u1 = (int) (nLow >>> 32);
            int u0 = (int) nLow;
            int v1 = (int) (d >>> 32);
            int v0 = (int) d;
            /*
              First quotient word:

              [u3 u2 u1] / [v1 v0]
             */
            DivRem3By2 first = divideThreeByTwo(u3, u2, u1, v1, v0);
            /*
              Bring down u0:

              [r1 r0 u0] / [v1 v0]
             */
            DivRem3By2 second = divideThreeByTwo(first.remHigh, first.remLow, u0, v1, v0);
            /*
              Combine the two unsigned 32-bit quotient words.
             */
            long qLow = (Integer.toUnsignedLong(first.quotient) << 32)
                    | Integer.toUnsignedLong(second.quotient);
            /*
              Combine normalized remainder.
             */
            long rNormalized = (Integer.toUnsignedLong(second.remHigh()) << 32)
                    | Integer.toUnsignedLong(second.remLow);
            long remainder = shift == 0 ? rNormalized : rNormalized >>> shift;
            q[i] = qLow;
            rem = remainder;
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
        // Starting with x=d is actually already correct modulo 8,
        // because every odd d satisfies d*d ≡ 1 (mod 8).
        long inverse = divisor;
        // Newton/Hensel iteration:
        // x <- x(2 - d*x)
        // Correct bits roughly double each iteration:
        // 3 -> 6 -> 12 -> 24 -> 48 -> 96
        for(int k = 0; k < 5; k++)
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
    private static long[][] divideMagnitudes(long[] dividend, long[] divisor){
        dividend = trimLeadingZeros(dividend);
        divisor  = trimLeadingZeros(divisor);
        if (isZeroArray(divisor))
            throw new ArithmeticException("Division by zero");
        int cmp = compareArray(dividend, divisor);
        if (cmp < 0)
            return new long[][]{ZERO_MAG, dividend};
        if (cmp == 0)
            return new long[][]{ONE_MAG, ZERO_MAG};
        if(divisor.length == 1){
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
            if (!equalsArray(reconstructed, dividend))
                throw new AssertionError("BZ division: q*d+r != n");
        }
        return results;
    }
    private static long[][] divideMagnitudesKnuth(long[] dividend, long[] divisor){
        dividend = trimLeadingZeros(dividend);
        divisor  = trimLeadingZeros(divisor);
        if (isZeroArray(divisor))
            throw new ArithmeticException("Division by zero");
        int cmp = compareArray(dividend, divisor);
        if (cmp < 0)
            return new long[][]{ZERO_MAG, dividend};
        if (cmp == 0)
            return new long[][]{ONE_MAG, ZERO_MAG};
        if (divisor.length == 1) {
            DivRem DR = divideByLong(dividend, divisor[0]);
            return new long[][]{DR.quotient, new long[]{DR.remainder}};
        }
        int n = divisor.length;
        int m = dividend.length - n;
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
            }else{
                long[] dr = divide128ByNormalized64(ujn, ujn1, v[n - 1]);
                qhat = dr[0];
                rHat = dr[1];
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
                if (!equalsArray(rebuild, current))
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
    private static long[] slice(long[] a, int from, int to){
        if(to >= a.length) to = a.length;
        if(from < 0) from = 0;
        if(from > to) throw new IllegalArgumentException("Illegal slice range " + from + " to " + to);
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
        long[] upper = new long[3 * m];
        System.arraycopy(A, m, upper, 0, 3 * m);
        long[][] qr1 = divide3nBy2n(upper, B);
        long[] Q1 = qr1[0];
        long[] R1 = qr1[1];
        /*
          Second:
               [R1 A0] / B
          R1 may occupy up to 2m = n limbs, so this is
          another 3m / 2m division.
         */
        long[] lower = new long[3 * m];
        System.arraycopy(A, 0, lower, 0, m);
        System.arraycopy(R1, 0, lower, m, Math.min(R1.length, 2 * m));
        long[][] qr0 = divide3nBy2n(lower, B);
        long[] Q0 = qr0[0];
        long[] R  = qr0[1];
        /*
         * Q = Q1 β^m + Q0
         */
        long[] Q = new long[2 * m];
        System.arraycopy(Q0, 0, Q, 0, Math.min(Q0.length, m));
        System.arraycopy(Q1, 0, Q, m, Math.min(Q1.length, m));
        return new long[][]{trimLeadingZeros(Q), trimLeadingZeros(R)};
    }
    private static long[] mod2nBy1n(long[] A, long[] B) {
        int cmp = compareArray(A, B);
        if (cmp < 0)
            return trimLeadingZeros(A);
        if (cmp == 0)
            return ZERO_MAG;
        int n = B.length;
        if (n <= BURNIKEL_ZIEGLER_LEAF_THRESHOLD || (n & 1) != 0)
            return knuthRemainder(A, B);
        int m = n >>> 1;
        // A = A3 β^(3m) + A2 β^(2m) + A1 β^m + A0
        long[] upper = new long[3 * m];
        System.arraycopy(A, m, upper, 0, 3 * m);
        long[][] qr1 = divide3nBy2n(upper, B);
        long[] R1 = qr1[1];
        /*
         * [R1 A0] / B
         */
        long[] lower = new long[3 * m];
        System.arraycopy(A, 0, lower, 0, m);
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
            if (isZeroArray(B0)) d = ZERO_MAG;
            else d = subtractMagnitudes(shiftLeftLimbs(B0, m), B0);
        }
        long[] rFull = new long[2 * m + 1];
        System.arraycopy(dividend, 0, rFull, 0, m+1);
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
        if(!isZeroArray(DR[1]))
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
        // Required by this specialized routine.
        // modSmall maintains this invariant.
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
        DivRem3By2 first = divideThreeByTwo(u3, u2, u1, v1, v0);
        DivRem3By2 second = divideThreeByTwo(first.remHigh, first.remLow, u0, v1, v0);
        long rNormalized = (Integer.toUnsignedLong(second.remHigh) << 32) | Integer.toUnsignedLong(second.remLow);
        return shift == 0 ? rNormalized : rNormalized >>> shift;
    }
    private static long modSmall(long[] a, long d) {
        if (d == 0L) throw new ArithmeticException("Cannot divide by 0");
        if (a.length == 1) return Long.remainderUnsigned(a[0], d);
        if ((d & (d - 1)) == 0) return a[0] & (d - 1);
        if(Long.compareUnsigned(d, Integer.MAX_VALUE) < 0)
            return modSmallInt(a, a.length, (int) d, Long.divideUnsigned(-1L, d));
        long r = 0L;
        for (int i = a.length - 1; i >= 0; i--) r = remainder128By64(r, a[i], d);
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
        if (isZeroArray(divisor)) throw new ArithmeticException("Division by zero");
        int cmp = compareArray(dividend, divisor);
        if (cmp < 0) return dividend;
        if (cmp == 0) return ZERO_MAG;
        if (divisor.length == 1) return new long[]{modSmall(dividend, divisor[0])};
        long[] remainder;
        if (divisor.length < LargeInteger.MOD_BZ_BLOCK_THRESHOLD || dividend.length >= divisor.length * 2)
            remainder = knuthRemainder(dividend, divisor);
        else remainder = burnikelZieglerRemainder(dividend, divisor);
        if(VERIFY_DIVISION){
            if(compareArray(divisor, remainder) >= 0)
                throw new AssertionError("Remainder greater than divisor");
        }
        return remainder;
    }
    public static long[] knuthRemainder(long[] dividend, long[] divisor) {
        dividend = trimLeadingZeros(dividend);
        divisor = trimLeadingZeros(divisor);
        if(isZeroArray(divisor)) throw new ArithmeticException("Division by zero");
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
                long[] dr = divide128ByNormalized64(ujn, ujn1, v[n - 1]);
                qhat = dr[0];
                rHat = dr[1];
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
    public static long[] burnikelZieglerRemainder(long[] dividend, long[] divisor) {
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
        if(VERIFY_DIVISION)
            if (compareArray(remainder, divisor) >= 0)
                throw new AssertionError("Knuth remainder became >= divisor after denormalization");
        return remainder;
    }
    private static long[] modAdd(long[] a, long[] b, long[] m) {
        long[] addend = compareArray(a, m) < 0 ? a : modMagnitudes(a, m);
        long[] adder = compareArray(b, m) < 0 ? b : modMagnitudes(b, m);
        long[] sum = addMagnitudes(addend, adder);
        if(compareArray(sum, m) >= 0)
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
    public LargeInteger mod(LargeInteger modulus){
        LargeInteger r = remainder(modulus);
        if (isZeroArray(r.limbs)) return ZERO;
        else if (r.negative != modulus.negative)
            return r.add(modulus);
        return r;
    }
    @Override
    public ScalarAtom mod(ScalarAtom other) {
        if (other instanceof LargeInteger modulus)
            return mod(modulus);
        return super.mod(other);
    }
    long mod(long modulus){
        if(modulus < 0){
            long mod = mod(-modulus);
            return mod == 0 ? 0 : mod + modulus;
        }
        long R = modSmall(limbs, modulus);
        if(R==0) return 0;
        if(negative) return modulus-R;
        return R;
    }
    private static long[] modInverseMagnitudes(long[] a, long[] mod) {
        if (isZeroArray(mod))
            throw new ArithmeticException("Modulus cannot be 0");
        ExtGcdMag eg = extendedGcdMagnitudes(a, mod);
        if (!isOneArray(eg.gcd))
            throw new ArithmeticException("Modular inverse does not exist");
        long[] x = modMagnitudes(eg.xMag, mod);
        if (!eg.xNeg) return x;
        if (isZeroArray(x)) return ZERO_MAG;
        return subtractMagnitudes(mod, x);
    }
    public LargeInteger modInverse(LargeInteger mod) {
        long[] inverse = modInverseMagnitudes(limbs, mod.limbs);
        // (-a)^-1 ≡ -(a^-1) mod |mod|.
        if (negative && !isZeroArray(inverse))
            inverse = subtractMagnitudes(mod.limbs, inverse);
        // Preserve the existing negative-modulus behavior:
        // positive modulus -> [0, mod)
        // negative modulus -> (mod, 0]
        if (mod.negative && !isZeroArray(inverse))
            return new LargeInteger(subtractMagnitudes(mod.limbs, inverse), true, true);
        return new LargeInteger(inverse, false, true);
    }
    private static boolean isOddMag(long[] x){return (x[0] & 1L) == 1L;}
    private static boolean isEvenMag(long[] x) {return (x[0] & 1L) == 0L;}
    public boolean isEven() {return isEvenMag(limbs);}
    public boolean isOdd() {return isOddMag(limbs);}
    public LargeFraction power(long p) {
        if (p == 0) return LargeFraction.ONE;
        if (p == 1) return new LargeFraction(this, ONE);
        if (p < 0) return new LargeInteger(power(limbs, -p), p % 2 != 0 && negative, true)
                .reciprocal();
        return new LargeFraction(new LargeInteger(power(limbs, p), p % 2 != 0 && negative, true),
                ONE);
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
            if (isZeroArray(mag))
                throw new IllegalArgumentException("Indeterminate form: 0^0");
            return ONE_MAG;
        }else if(power == 1) return mag.clone();
        else if(power > MAX_BITS)
            reportOverflow();
        long[] base = mag;
        long exponent = 0;

        if (isEvenMag(base)) {
            base = base.clone();
            while (isEvenMag(base)) {
                exponent++;
                shiftRightBitsInPlace(base, 1);
            }
        }
        long shift = Math.multiplyExact(power, exponent);
        if (isOneArray(base))
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
        return shift == 0 ? result : shiftLeftBits(result, shift);
    }
    public LargeFraction power(LargeInteger other) {
        if (isZeroArray(other.limbs)) {
            if (isZeroArray(limbs))
                throw new ArithmeticException("Indeterminate form: 0^0");
            return LargeFraction.ONE;
        }
        // ±1 never grows, regardless of exponent size
        if (isOneArray(limbs)) {
            LargeInteger result = negative && other.isOdd() ? NEGATIVE_ONE : ONE;
            return other.negative ? result.reciprocal() : new LargeFraction(result, ONE);
        }
        if (isZeroArray(limbs)) {
            if (other.negative)
                throw new ArithmeticException("Cannot divide by 0");
            return LargeFraction.ZERO;
        }
        LargeInteger exp = other.abs();
        long baseBits = bitLength();
        // a^n has at least n * (baseBits - 1) + 1 bits.
        LargeInteger maxExponent = valueOf(MAX_BITS - 1).divide(valueOf(baseBits - 1));
        if (exp.greaterThan(maxExponent))
            reportOverflow();
        /*
         * exp is now guaranteed <= MAX_BITS - 1,
         * and MAX_BITS = 2^37, so it necessarily fits in long.
         */
        long p = exp.longValueExact();
        return power(other.negative ? -p : p);
    }
    @Override
    public LargePowerMonomial power(LargeFraction f) {
        return new LargePowerMonomial(new LargeFraction(this, ONE), f);}
    private static final class MontgomeryReducer {
        private final long[] modulus;
        private final int k;
        private final long mPrime;
        private final long[] rModM;
        private final long[] rTimesModulus;
        private final long[] r2ModM;
        private final boolean useCIOS;
        private final long[] scratch;
        private final long[] ciosScratch;

        MontgomeryReducer(long[] inputModulus) {
            if (inputModulus == null)
                throw new NullPointerException("modulus is null");
            if (inputModulus.length == 0)
                throw new IllegalArgumentException("modulus has an empty abs");
            long[] modulus = trimLeadingZeros(inputModulus);
            if (isZeroArray(modulus))
                throw new ArithmeticException("modulus is zero");
            if (isEvenMag(modulus))
                throw new ArithmeticException("Montgomery modulus must be odd");
            this.modulus = modulus;
            k = modulus.length;
            long x = modulus[0];
            long y = 2L * x - x * x * x;
            for (int i = 0; i < 4; i++) y *= 2L - x * y;
            mPrime = -y;
            useCIOS = k <= 0;
            scratch = new long[2 * k + 2];
            ciosScratch = new long[2 * k + 2];
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
            reduceScratchInPlace();
            copyReducedInto(out);
        }
        long[] reduce(long[] product) {
            long[] result = new long[k];
            reduceInto(product, result);
            return trimLeadingZeros(result);
        }
        private void reduceScratchInPlace() {
            for (int i = 0; i < k; i++) {
                long u = scratch[i] * mPrime;
                // j = 0 special case
                long lo = modulus[0] * u;
                long hi = Math.unsignedMultiplyHigh(modulus[0], u);

                long sum = lo + scratch[i];
                long c = Long.compareUnsigned(sum, lo) < 0 ? 1L : 0L;// sum == 0 modulo 2^64 by construction
                long carry = hi + c;
                // start generic loop at j = 1
                for (int j = 1; j < k; j++) {
                    lo = modulus[j] * u;
                    hi = Math.unsignedMultiplyHigh(modulus[j], u);

                    long s1 = lo + scratch[i + j];
                    long c1 = Long.compareUnsigned(s1, lo) < 0 ? 1L : 0L;

                    long s2 = s1 + carry;
                    long c2 = Long.compareUnsigned(s2, s1) < 0 ? 1L : 0L;

                    scratch[i + j] = s2;
                    carry = hi + c1 + c2;
                }
                int pos = i + k;
                if (carry != 0L) {
                    long old = scratch[pos];
                    sum = old + carry;
                    scratch[pos] = sum;
                    if (Long.compareUnsigned(sum, old) < 0)
                        do pos++;
                        while (++scratch[pos] == 0L);
                }
            }
        }
        private void copyReducedInto(long[] out) {
            if (out.length != k)
                throw new IllegalArgumentException();
            boolean subtract = scratch[2 * k] != 0L;
            if (!subtract) {
                // Compare low k limbs against modulus.
                for (int i = k - 1; i >= 0; i--) {
                    int cmp = Long.compareUnsigned(scratch[k + i], modulus[i]);
                    if (cmp > 0) {
                        subtract = true;
                        break;
                    }
                    if (cmp < 0) break;
                }
            }
            if (!subtract) {
                System.arraycopy(scratch, k, out, 0, k);
                return;
            }
            long borrow = 0L;
            for (int i = 0; i < k; i++) {
                long a = scratch[k + i];
                long b = modulus[i];
                long x = a - b;
                long b1 = Long.compareUnsigned(a, b) < 0 ? 1L : 0L;
                long y = x - borrow;
                long b2 = Long.compareUnsigned(x, borrow) < 0 ? 1L : 0L;
                out[i] = y;
                borrow = b1 | b2;
            }
        }
        void requireReducedMagnitude(long[] x, String name) {
            if (x == null)
                throw new NullPointerException(name + " is null");
            if (x.length == 0)
                throw new IllegalArgumentException(name + " is empty");
            if (x.length > k)
                throw new IllegalArgumentException(name + " exceeds Montgomery width");
            if (compareArray(x, modulus) >= 0)
                throw new IllegalArgumentException(name + " is not reduced");
        }
        long[] add(long[] a, long[] b) {
            requireReducedMagnitude(a, "a");
            requireReducedMagnitude(b, "b");
            return addCanonical(a, b);
        }
        long[] addCanonical(long[] a, long[] b) {
            long[] sum = addMagnitudes(a, b);
            if(compareArray(sum, modulus) >= 0)
                subtractMagnitudeInPlace(sum, modulus);
            return trimLeadingZeros(sum);
        }
        void addInto(long[] a, long[] b, long[] out) {
            // a,b < modulus; out has length k.
            long carry = 0;

            for (int i = 0; i < k; i++) {
                long ai = i < a.length ? a[i] : 0;
                long bi = i < b.length ? b[i] : 0;

                long s1 = ai + bi;
                long c1 = Long.compareUnsigned(s1, ai) < 0 ? 1 : 0;

                long s2 = s1 + carry;
                long c2 = Long.compareUnsigned(s2, s1) < 0 ? 1 : 0;

                out[i] = s2;
                carry = c1 | c2;
            }
            // Since a,b < n, a+b < 2n, so at most one subtraction is needed.
            // If carry != 0, the true sum is 2^(64k) + out.
            // Subtracting modulus modulo 2^(64k) gives exactly the desired reduced result.
            if (carry != 0 || compareFixedToModulus(out) >= 0)
                subtractModulusFixed(out);
        }
        long[] subtract(long[] a, long[] b) {
            requireReducedMagnitude(a, "a");
            requireReducedMagnitude(b, "b");
            return subtractCanonical(a, b);
        }
        long[] subtractCanonical(long[] a, long[] b) {
            int cmp = compareArray(a, b);
            if (cmp == 0) return ZERO_MAG;
            else if (cmp > 0) return subtractMagnitudes(a, b);
            return subtractMagnitudes(modulus, subtractMagnitudes(b, a));
        }
        void subtractInto(long[] a, long[] b, long[] out) {
            long borrow = 0;
            for (int i = 0; i < k; i++) {
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
            if (borrow != 0)
                addModulusFixed(out);
        }
        private int compareFixedToModulus(long[] a) {
            for (int i = k - 1; i >= 0; i--) {
                long ai = a[i];
                long mi = i < modulus.length ? modulus[i] : 0;
                int cmp = Long.compareUnsigned(ai, mi);
                if (cmp != 0) return cmp;
            }
            return 0;
        }
        private void addModulusFixed(long[] a) {
            long carry = 0;
            for (int i = 0; i < k; i++) {
                long mi = i < modulus.length ? modulus[i] : 0;

                long s1 = a[i] + mi;
                long c1 = Long.compareUnsigned(s1, a[i]) < 0 ? 1 : 0;

                long s2 = s1 + carry;
                long c2 = Long.compareUnsigned(s2, s1) < 0 ? 1 : 0;

                a[i] = s2;
                carry = c1 | c2;
            }
        }
        private void subtractModulusFixed(long[] a) {
            long borrow = 0;

            for (int i = 0; i < k; i++) {
                long ai = a[i];
                long bi = modulus[i];

                long d1 = ai - bi;
                long b1 = Long.compareUnsigned(ai, bi) < 0 ? 1 : 0;

                long d2 = d1 - borrow;
                long b2 = Long.compareUnsigned(d1, borrow) < 0 ? 1 : 0;

                a[i] = d2;
                borrow = b1 | b2;
            }

            // Deliberately discard final borrow.
        }
        long[] multiply(long[] a, long[] b) {
            long[] out = new long[k];
            multiplyInto(a, b, out);
            return out;
        }
        void multiplyInto(long[] a, long[] b, long[] out){
            if (useCIOS){
                multiplyCIOS(a, b, out);
                return;
            }
            if (Math.min(a.length, b.length) < KARATSUBA_THRESHOLD) {
                Arrays.fill(scratch, 0L);
                schoolbookMultiplyInto(a, b, scratch);
                reduceScratchInPlace();
                copyReducedInto(out);
                return;
            }
            reduceInto(multiplyMagnitudes(a, b), out);
        }
        static void schoolbookMultiplyInto(long[] a, long[] b, long[] out) {
            if (a.length < b.length) {
                long[] t = a;
                a = b;
                b = t;
            }
            int al = a.length;
            int bl = b.length;
            // First row: there is nothing already in out[] to accumulate.
            long ai = a[0]; long carry = 0L;
            for (int j = 0; j < bl; j++) {
                long bj = b[j];

                long low = ai * bj;
                long high = Math.unsignedMultiplyHigh(ai, bj);

                long sum = low + carry;
                long c = Long.compareUnsigned(sum, low) < 0 ? 1L : 0L;

                out[j] = sum;
                carry = high + c;
            }
            out[bl] = carry;
            // Remaining rows accumulate into already initialized limbs.
            for (int i = 1; i < al; i++) {
                ai = a[i];
                carry = 0L;
                for (int j = 0; j < bl; j++) {
                    int index = i + j;
                    long bj = b[j];
                    long low = ai * bj;
                    long high = Math.unsignedMultiplyHigh(ai, bj);
                    long s1 = low + out[index];
                    long c1 = Long.compareUnsigned(s1, low) < 0 ? 1L : 0L;
                    long s2 = s1 + carry;
                    long c2 = Long.compareUnsigned(s2, s1) < 0 ? 1L : 0L;
                    out[index] = s2;
                    carry = high + c1 + c2;
                }
                // This limb has never previously been touched.
                out[i + bl] = carry;
            }
        }
        static void schoolbookSquareInto(long[] a, long[] out) {
            int n = a.length;
            // Diagonal terms: a[i]^2
            for (int i = 0; i < n; i++) {
                long x = a[i];
                long low = x * x;
                long high = Math.unsignedMultiplyHigh(x, x);
                int index = i << 1;
                add128Into(out, index, low, high);
            }
            // Cross terms: 2 * a[i] * a[j], i < j
            for (int i = 0; i < n; i++) {
                long ai = a[i];
                for (int j = i + 1; j < n; j++) {
                    long bj = a[j];
                    long productLow = ai * bj;
                    long productHigh = Math.unsignedMultiplyHigh(ai, bj);
                    // Double the unsigned 128-bit product.
                    long low  = productLow << 1;
                    long high = (productHigh << 1) | (productLow >>> 63);
                    long top  = productHigh >>> 63;
                    add192Into(out, i + j, low, high, top);
                }
            }
        }
        private static void add128Into(long[] out, int index, long low, long high) {
            long old = out[index];
            long sum = old + low;
            long carry = Long.compareUnsigned(sum, old) < 0 ? 1L : 0L;
            out[index] = sum;
            old = out[index + 1];
            sum = old + high;
            long c1 = Long.compareUnsigned(sum, old) < 0 ? 1L : 0L;
            long sum2 = sum + carry;
            long c2 = Long.compareUnsigned(sum2, sum) < 0 ? 1L : 0L;
            out[index + 1] = sum2;
            carry = c1 | c2;
            index += 2;
            while (carry != 0L) {
                old = out[index];
                sum = old + carry;
                out[index] = sum;
                carry = Long.compareUnsigned(sum, old) < 0 ? 1L : 0L;
                index++;
            }
        }
        private static void add192Into(long[] out, int index, long low, long high, long top) {
            long carry = addWithCarry(out, index, low, 0L);
            carry = addWithCarry(out, index + 1, high, carry);
            long upper = top + carry;
            index += 2;
            while (upper != 0L) {
                long old = out[index];
                long sum = old + upper;
                out[index] = sum;
                upper = Long.compareUnsigned(sum, old) < 0 ? 1L : 0L;
                index++;
            }
        }
        /**
         * CIOS Montgomery multiplication.
         * Preconditions:
         * 0 <= a < modulus
         * 0 <= b < modulus
         * where R = 2^(64*k).
         */
        void multiplyCIOS(long[] a, long[] b, long[] out) {
            requireReducedMagnitude(a, "a");
            requireReducedMagnitude(b, "b");
            /*
             * One extra top limb is required during accumulation.
             *
             * The old implementation allocated:
             *     product: approximately 2*k limbs
             *     REDC work: approximately 2*k limbs
             *     result: approximately k limbs
             *
             * This implementation needs only this k+2-limb accumulator
             * plus the returned result.
             */
            Arrays.fill(ciosScratch, 0L);
            for (int i = 0; i < k; i++) {
                long bi = i < b.length ? b[i] : 0L;
                //t += a * bi
                long carry = 0L;
                for (int j = 0; j < a.length; j++) {
                    long low = a[j] * bi;
                    long high = Math.unsignedMultiplyHigh(a[j], bi);
                    long s1 = ciosScratch[j] + low;
                    long c1 = Long.compareUnsigned(s1, ciosScratch[j]) < 0 ? 1L : 0L;
                    long s2 = s1 + carry;
                    long c2 = Long.compareUnsigned(s2, s1) < 0 ? 1L : 0L;
                    ciosScratch[j] = s2;
                    /*
                      Under the multiplication invariant, this unsigned
                      addition cannot overflow past 1 limb.
                     */
                    carry = high + c1 + c2;
                }
                /*
                  Add the product carry above a's highest stored limb.
                  a may contain fewer than k limbs.
                 */
                int carryIndex = a.length;
                while (carry != 0L) {
                    long old = ciosScratch[carryIndex];
                    long sum = old + carry;
                    ciosScratch[carryIndex] = sum;
                    carry = Long.compareUnsigned(sum, old) < 0 ? 1L : 0L;
                    carryIndex++;
                    if (carryIndex >= ciosScratch.length && carry != 0L)
                        throw new AssertionError("CIOS product accumulation overflow");
                }
                //q is chosen so that t[0] + q * modulus[0] == 0 mod 2^6
                long q = ciosScratch[0] * mPrime;
                /*
                  t += q * modulus
                  Simultaneously divide by 2^64. The value written for
                  limb j is placed at j-1, so the canceled low limb is
                  never copied or retained.
                 */
                for (int j = 0; j < k; j++) {
                    long low = modulus[j] * q;
                    long high = Math.unsignedMultiplyHigh(modulus[j], q);
                    long s1 = ciosScratch[j] + low;
                    if(Long.compareUnsigned(s1, ciosScratch[j]) < 0) high++;
                    long s2 = s1 + carry;
                    if(Long.compareUnsigned(s2, s1) < 0) high++;
                    carry = high;
                    if (j == 0) {
                        //This is the fundamental Montgomery invariant.
                        if (s2 != 0L) throw new AssertionError("Montgomery cancellation failed");
                    } else ciosScratch[j - 1] = s2; //Store S / 2^64 directly.
                }
                //Finish the shifted top limbs.
                long oldTop = ciosScratch[k];
                long shiftedTop = oldTop + carry;
                long topCarry = Long.compareUnsigned(shiftedTop, oldTop) < 0 ? 1L : 0L;
                ciosScratch[k - 1] = shiftedTop;
                long oldExtra = ciosScratch[k + 1];
                long newExtra = oldExtra + topCarry;
                if (Long.compareUnsigned(newExtra, oldExtra) < 0)
                    throw new AssertionError("CIOS top-limb overflow");
                ciosScratch[k] = newExtra;
                ciosScratch[k + 1] = 0L;
            }
            /*
              With reduced operands, the CIOS result is in [0, 2m).
              Therefore, at most 1 subtraction is required.
             */
            if (compareArray(ciosScratch, modulus) >= 0)
                subtractMagnitudeInPlace(ciosScratch, modulus);
            if (ciosScratch[k] != 0L || compareArray(ciosScratch, modulus) >= 0)
                throw new AssertionError("CIOS result was not fully reduced");
            System.arraycopy(ciosScratch, 0, out, 0, k);
        }
        long[] square(long[] a) {
            long[] out = new long[k];
            squareInto(a, out);
            return out;
        }
        void squareInto(long[] a, long[] out) {
            if (useCIOS) {
                multiplyCIOS(a, a, out);
                return;
            }
            if (k < KARATSUBA_THRESHOLD) {
                Arrays.fill(scratch, 0L);
                schoolbookSquareInto(a, scratch);
                reduceScratchInPlace();
                copyReducedInto(out);
                return;
            }
            reduceInto(squareMagnitude(a), out);
        }
        long[] cube(long[] a) {
            requireReducedMagnitude(a, "a");
            return multiply(square(a), a);
        }
        long[] half(long[] a) {
            requireReducedMagnitude(a, "a");
            if ((a[0] & 1L) == 0L)
                return shiftRightBits(a, 1);
            return shiftRightBits(addMagnitudes(a, modulus), 1);
        }
        long[] powMontgomery(long[] baseMontgomery, long[] exponent) {
            if (isZeroArray(exponent)) return rModM;
            int window = bestWindowSize(exponent);
            if (window == 1) {
                long[] result = new long[k];
                System.arraycopy(baseMontgomery, 0, result, 0, baseMontgomery.length);
                for (long bit = bitLength(exponent) - 2; bit >= 0; bit--) {
                    squareInto(result, result);
                    if (testBit(exponent, bit))
                        multiplyInto(result, baseMontgomery, result);
                }
                return trimLeadingZeros(result);
            }
            int tableSize = 1 << (window - 1);
            long[][] oddPowers = new long[tableSize][];
            oddPowers[0] = baseMontgomery;
            long[] base2 = square(baseMontgomery);
            for (int i = 1; i < tableSize; i++)
                oddPowers[i] = multiply(oddPowers[i - 1], base2);
            long[] result;
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
            result = Arrays.copyOf(oddPowers[value >> 1], k);
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
                multiplyInto(result, oddPowers[value >> 1], result);
                bit -= width;
            }
            return result;
        }
        long[] powMod(long[] base, long[] exponent) {return reduce(powMontgomery(toMontgomery(base), exponent));}
        long[] toMontgomery(long[] x) {return multiply(x, r2ModM);}
    }
    private static int bestWindowSize(long[] exponent) {
        long popCount = 0;
        for (long limb : exponent)
            popCount += Long.bitCount(limb);
        // No window >= 2 can possibly pay for its precomputation.
        if (popCount <= 2) return 1;
        long bestCost = popCount - 1; // leading 1 already consumed
        int bestWindow = 1;
        for (int window = 2; window <= 7; window++) {
            long tableMultiplies = (1L << (window - 1)) - 1;
            long count = 0;
            long bit = bitLength(exponent) - 1L;
            while (bit >= 0) {
                if (!testBit(exponent, bit)) {
                    bit--;
                    continue;
                }
                long width = Math.min(window, bit + 1);
                // Must match powMontgomery's window selection exactly.
                while (width > 1 && !testBit(exponent, bit - width + 1)) width--;
                count++;
                bit -= width;
            }
            count += tableMultiplies + 1; // base² square
            if (count < bestCost) {
                bestCost = count;
                bestWindow = window;
            }
        }
        return bestWindow;
    }
    /**
     * Returns a LargeInteger whose value is <code>(this<sup>exponent</sup>) mod modulus</code>.
     * Unlike generic <code>power</code>, this method strictly returns an integer.
     * @param exponent the power which the base is exponentiated to
     * @param modulus the modulus in which each step is always reduced with respect to
     */
    public LargeInteger powMod(LargeInteger exponent, LargeInteger modulus) {
        if (isZeroArray(modulus.limbs))
            throw new ArithmeticException("Cannot mod by 0");
        if (modulus.negative) {
            LargeInteger tmp = powMod(exponent, modulus.negate());
            if (isZeroArray(tmp.limbs)) return tmp;
            return tmp.add(modulus);
        }
        if (isOneArray(modulus.limbs)) return ZERO;
        if (isZeroArray(exponent.limbs)) {
            if (isZeroArray(limbs)) throw new IllegalArgumentException("Indeterminate form: 0^0");
            return ONE;
        }
        if (exponent.negative)
            return modInverse(modulus).powMod(exponent.negate(), modulus);
        if (isZeroArray(limbs)) return ZERO;
        if(isOneArray(limbs)) return ONE;
        LargeInteger base = mod(modulus);
        // Odd modulus
        if (modulus.isOdd()) {
            if (modulus.limbs.length == 1) {
                long r = powMod64(base.limbs, exponent.limbs, modulus.limbs[0]);
                return new LargeInteger(new long[]{r}, false, true);
            }
            return new LargeInteger(new MontgomeryReducer(modulus.limbs).powMod(base.limbs, exponent.limbs),
                    false, true);
        }
        // Even modulus: m = 2^k * q
        long trailingBits = trailingZeroBits(modulus.limbs);
        long[] oddPart = shiftRightBits(modulus.limbs, trailingBits);
        long[] evenPow = powModPowerOfTwo(base.limbs, exponent.limbs, trailingBits);
        // Pure power of two
        if (isOneArray(oddPart))
            return new LargeInteger(evenPow, false, true);
        // Odd component
        long[] oddBase = modMagnitudes(base.limbs, oddPart);
        long[] oddPow;
        if (oddPart.length == 1) oddPow = new long[]{powMod64(oddBase, exponent.limbs, oddPart[0])};
        else oddPow = new MontgomeryReducer(oddPart).powMod(oddBase, exponent.limbs);
        // CRT
        long[] pow2 = shiftLeftBits(ONE_MAG, trailingBits);
        long[] combined = CRT(oddPow, evenPow, oddPart, pow2);
        return new LargeInteger(combined, false, true);
    }
    private static long[] CRT(long[] oddPow, long[] evenPow, long[] oddPart, long[] pow2){
        long[] t = modSubtract(evenPow, oddPow, pow2);
        long[] inv = modInverseMagnitudes(oddPart, pow2);
        if (!isOneArray(modMagnitudes(multiplyMagnitudes(oddPart, inv), pow2)))
            throw new AssertionError("bad oddPart inverse mod 2^k");
        t = modMagnitudes(multiplyMagnitudes(t, inv), pow2);
        return addMagnitudes(oddPow, multiplyMagnitudes(oddPart, t));
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
    private static long powMod64(long[] base, long[] exponent, long modulus){
        if (modulus == 1) return 0;
        if (isZeroArray(exponent)) return 1L;
        if (modulus == 2)
            return isEvenMag(base) ? 0L : 1L;
        long baseTemp = modSmall(base, modulus);
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
                result = remainder128By64(Math.unsignedMultiplyHigh(result, result), result * result, modulus);
                if (((limb >>> bit) & 1L) != 0)
                    result = remainder128By64(Math.unsignedMultiplyHigh(result, baseTemp), result * baseTemp,
                            modulus);
            }
        }
        return result;
    }
    private static int powMod(long base, long exp, int mod, long reciprocal) {
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
    private static void maskToBits(long[] a, long bits) {
        int rem = (int) (bits & 63);
        if (rem != 0) a[a.length - 1] &= (1L << rem) - 1;
    }
    private static void multiplyModPowerOfTwoInto(long[] a, long[] b, long[] out, long bits) {
        multiplyInto(a, b, out);
        maskToBits(out, bits);
    }
    private static void squareModPowerOfTwoInto(long[] a, long[] out, long bits) {
        squareInto(a, out);
        maskToBits(out, bits);
    }
    private static long[] powModPowerOfTwo(long[] in, long[] exponent, long bits) {
        if (bits <= 0)
            throw new IllegalArgumentException("bits must be positive");
        else if(bits == 1)
            return isOddMag(in) ? ONE_MAG : ZERO_MAG;
        exponent = trimLeadingZeros(exponent);
        if (isZeroArray(exponent)) {
            if (isZeroArray(in))
                throw new ArithmeticException("Indeterminate form: 0^0");
            return ONE_MAG;
        }
        int len = Math.toIntExact(Math.ceilDiv(bits, 64L));
        // Fixed-width base residue.
        long[] base = new long[len];
        long[] reduced = lowestKBits(in, bits);
        System.arraycopy(reduced, 0, base, 0, Math.min(reduced.length, len));
        maskToBits(base, bits);
        if (isZeroArray(base))
            return ZERO_MAG;
        long[] expTemp = exponent.clone();
        long t = trailingZeroBits(base);
        if (t > 0) {
            // v2(base^e) = t*e. Once >= bits, residue is zero.
            if (compareSmall(expTemp, Math.ceilDiv(bits, t)) >= 0)
                return ZERO_MAG;
        } else if (bits >= 3)
            // lambda(2^bits) = 2^(bits-2) for odd base.
            expTemp = lowestKBits(expTemp, bits - 2);
        if (isZeroArray(expTemp))
            return ONE_MAG;
        long[] result = new long[len];
        result[0] = 1L;
        long[] scratch = new long[len];
        int window = bestWindowSize(expTemp);
        if (window == 1) {
            //Exponentiation by squaring
            while (!isZeroArray(expTemp)) {
                if ((expTemp[0] & 1L) != 0) {
                    multiplyInto(result, base, scratch);
                    maskToBits(scratch, bits);
                    long[] swap = result;
                    result = scratch;
                    scratch = swap;
                }
                shiftRightBitsInPlace(expTemp, 1);
                if (!isZeroArray(expTemp)) {
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
        squareModPowerOfTwoInto(base, base2, bits);
        for (int i = 1; i < tableSize; i++) {
            oddPowers[i] = new long[len];
            multiplyModPowerOfTwoInto(oddPowers[i - 1], base2, oddPowers[i], bits);
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
                squareModPowerOfTwoInto(result, scratch, bits);
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
                squareModPowerOfTwoInto(result, scratch, bits);
                long[] swap = result;
                result = scratch;
                scratch = swap;
            }
            multiplyModPowerOfTwoInto(result, oddPowers[value >> 1], scratch, bits);
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
    /*
     * Returns floor(mag / 2^shift).
     *
     * Caller chooses shift so the result occupies at most 63 bits,
     * therefore the returned long is always nonnegative.
     *
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
             * (aTop,bTop) <- (bTop,aTop-q*bTop)
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
    private record LehmerStep(long A, long B, long C, long D, long steps){}
    private static long trailingZeroBits(long[] x) {
        int limbs = 0;
        while (limbs < x.length && x[limbs] == 0L) limbs++;
        if(limbs == x.length) return 0L;
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
        if (c == 0 || isZeroArray(a))
            return ZERO_MAG;

        if (c < 0)
            return null;

        return multiplySmall(a, c);
    }
    private static boolean acceptLehmerPair(long[] oldA, long[] oldB, long[] newA, long[] newB) {
        if (isZeroArray(newA) && isZeroArray(newB)) return false;
        /*
         * oldA is the old maximum because gcdMagnitudes ensures oldA >= oldB.
         * The transformed pair need not preserve ordering; the next outer
         * iteration can swap them.
         */
        if (!isZeroArray(newA) && compareArray(newA, oldA) >= 0) return false;
        if (!isZeroArray(newB) && compareArray(newB, oldA) >= 0) return false;
        /*
         * Avoid accepting the identity/no-progress transform.
         */
        return !equalsArray(newA, oldA) || !equalsArray(newB, oldB);
    }
    private static long[] gcdMagnitudes(long[] g1, long[] g2) {
        long[] a = trimLeadingZeros(g1);
        if(a == g1) a = g1.clone();
        long[] b = trimLeadingZeros(g2);
        if(b == g2) b = g2.clone();
        if(isZeroArray(a)) return b;
        if(isZeroArray(b)) return a;
        while (!isZeroArray(b)) {
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
    private static long gcdLong(long a, long b) {
        // a and b are interpreted as unsigned 64-bit integers.
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
            }else{
                b -= a;
                b >>>= Long.numberOfTrailingZeros(b);
            }
        }
        return a << commonShift;
    }
    private static ExtGcdMag extendedGcdMagnitudes(long[] a, long[] b) {
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);
        long[] oldR = a;
        long[] r = b;
        long[] oldSMag = ONE_MAG;
        boolean oldSNeg = false;
        long[] sMag = ZERO_MAG;
        boolean sNeg = false;
        long[] oldTMag = ZERO_MAG;
        boolean oldTNeg = false;
        long[] tMag = ONE_MAG;
        boolean tNeg = false;
        while (!isZeroArray(r)) {
            long[][] qr = divideMagnitudes(oldR, r);
            long[] q = qr[0];
            long[] nextR = qr[1];
            oldR = r;
            r = nextR;
            // nextS = oldS - q*s
            long[] qs = multiplyMagnitudes(sMag, q);
            long[] nextSMag;
            boolean nextSNeg;
            if (oldSNeg != sNeg) {
                // oldS and -q*s have the same sign.
                nextSMag = addMagnitudes(oldSMag, qs);
                nextSNeg = oldSNeg;
            } else {
                // Opposite signs: subtract magnitudes.
                int cmp = compareArray(oldSMag, qs);
                if (cmp >= 0) {
                    nextSMag = subtractMagnitudes(oldSMag, qs);
                    nextSNeg = false;
                } else {
                    nextSMag = subtractMagnitudes(qs, oldSMag);
                    nextSNeg = true;
                }
                if (isZeroArray(nextSMag))
                    nextSNeg = false;
            }
            oldSMag = sMag;
            oldSNeg = sNeg;
            sMag = nextSMag;
            sNeg = nextSNeg;
            // nextT = oldT - q*t
            long[] qt = multiplyMagnitudes(tMag, q);
            long[] nextTMag;
            boolean nextTNeg;
            if (oldTNeg != tNeg) {
                nextTMag = addMagnitudes(oldTMag, qt);
                nextTNeg = oldTNeg;
            } else {
                int cmp = compareArray(oldTMag, qt);
                if (cmp >= 0) {
                    nextTMag = subtractMagnitudes(oldTMag, qt);
                    nextTNeg = false;
                } else {
                    nextTMag = subtractMagnitudes(qt, oldTMag);
                    nextTNeg = true;
                }
                if (isZeroArray(nextTMag))
                    nextTNeg = false;
            }
            oldTMag = tMag;
            oldTNeg = tNeg;
            tMag = nextTMag;
            tNeg = nextTNeg;
        }
        return new ExtGcdMag(trimLeadingZeros(oldR), oldSMag, oldSNeg, oldTMag, oldTNeg);
    }
    private long[] binaryGcd(long[] g1, long[] g2){
        if (isZeroArray(g1)) return g2;
        if (isZeroArray(g2)) return g1;
        long[] a = trimLeadingZeros(g1);
        if(a == g1) a = g1.clone();
        long[] b = trimLeadingZeros(g2);
        if(b == g2) b = g2.clone();
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
    private long[] euclideanGcd(long[] g1, long[] g2){
        long[] a = g1.clone();
        long[] b = g2.clone();
        while(!isZeroArray(b)){
            long[] mid = modMagnitudes(a, b);
            a = b;
            b = mid;
        }
        return a;
    }
    public LargeInteger gcd(LargeInteger other) {
        return new LargeInteger(gcdMagnitudes(limbs, other.limbs), false, true);}
    private record ExtGcdMag(long[] gcd, long[] xMag, boolean xNeg, long[] yMag, boolean yNeg) {}
    public LargeInteger lcm(LargeInteger other) {
        if (isZeroArray(limbs) || isZeroArray(other.limbs)) return ZERO;
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
        if(negative) return false;
        int cmp = compareSmall(limbs, 2);
        if (cmp <= 0) return cmp == 0;
        if (isEvenMag(limbs)) return false;
        //small test
        for(int k: VERY_SMALL_PRIMES){
            if(modSmall(limbs, k) == 0)
                return limbs.length == 1 && limbs[0] == k;
        }
        if(compareSmall(limbs, 1000000) < 0) return true;
        if(equalsArray(limbs, squareMagnitude(sqrtFloorMagnitude(limbs)))) return false;
        return isProbablyPrimeMag(limbs);
    }
    private static boolean isProbablyPrimeMag(long[] n) {
        n = trimLeadingZeros(n);
        long[] nMinusOne = subtractSmall(n, 1);
        long s = trailingZeroBits(nMinusOne);
        long[] d = shiftRightBits(nMinusOne, s);
        MontgomeryReducer reducer = new MontgomeryReducer(n);
        // Montgomery representations, computed once.
        long[] oneMont = reducer.rModM;
        long[] minusOneMont = subtractMagnitudes(reducer.modulus, oneMont);
        // randomMagBetween uses an exclusive upper bound:
        // 2 <= witness < n - 1, therefore witness <= n - 2.
        long bits = bitLength(n);
        int rounds;
        if (bits <= 64) {
            long modulus = n[0];
            long minusOne = modulus - 1;
            long[] bases = {2, 325, 9375, 28178, 450775, 9780504, 1795265022};
            for (long base : bases) {
                long witness = modSmall(new long[]{base}, modulus);
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
        }else if (bits < 100)
            return passesMillerRabin(49, reducer, oneMont, minusOneMont, d, s, nMinusOne);
        else if (bits < 256) rounds = 25;
        else if (bits < 512) rounds = 16;
        else if (bits < 768) rounds = 9;
        else if (bits < 1024) rounds = 4;
        else rounds = 2;
        return passesMillerRabin(rounds, reducer, oneMont, minusOneMont, d, s, nMinusOne)
                && passesLucasSelfridge(n, reducer, oneMont);
    }
    private static boolean passesMillerRabin(int rounds, MontgomeryReducer reducer, long[] oneMont,
                                             long[] minusOneMont, long[] d, long s, long[] nMinusOne) {
        for (int round = 0; round < rounds; round++) {
            long[] witnessMont = reducer.toMontgomery(randomMagnitudeBetween(TWO_MAG, nMinusOne));
            long[] x = reducer.powMontgomery(witnessMont, d);
            if (equalsArray(x, oneMont) || equalsArray(x, minusOneMont)) continue;
            boolean passed = false;
            for (long r = 1; r < s; r++) {
                x = reducer.square(x);
                if (equalsArray(x, minusOneMont)) {
                    passed = true;
                    break;
                }
                if (equalsArray(x, oneMont)) return false;
            }
            if (!passed) return false;
        }
        return true;
    }
    private static boolean passesLucasSelfridge(long[] n, MontgomeryReducer reducer, long[] oneMont){
        //Lucas-Selfridge test
        long D = chooseSelfridgeD(n);
        if (D == 0L) return false;
        long[] nPlusOne = addSmall(n, 1);
        long s = trailingZeroBits(nPlusOne);
        long[] d = shiftRightBits(nPlusOne, s);
        if(isOneArray(d)){
            long[] current = reducer.toMontgomery(FOUR_MAG);
            long[] twoMont = reducer.toMontgomery(TWO_MAG);
            for (long k = 0; k < s - 2; k++)
                current = reducer.subtract(reducer.square(current), twoMont);
            return isZeroArray(current);
        }
        // Selfridge parameters:
        // P = 1
        // Q = (1 - D) / 4
        long Q = (1L - D) / 4L;
        long[] dMont = reducer.toMontgomery(signedSmallMod(D, n));
        long[] qMont = reducer.toMontgomery(signedSmallMod(Q, n));
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
                // U_(2k+1) = (U_(2k) + V_(2k)) / 2
                U = reducer.half(reducer.add(oldU, oldV));
                // V_(2k+1) = (D*U_(2k) + V_(2k)) / 2
                V = reducer.half(reducer.add(reducer.multiply(dMont, oldU), oldV));
                Qk = reducer.multiply(Qk, qMont);
            }
        }
        if (isZeroArray(U) || isZeroArray(V)) return true;
        for (int r = 1; r < s; r++) {
            V = reducer.subtract(reducer.square(V), reducer.add(Qk, Qk));
            Qk = reducer.square(Qk);
            if (isZeroArray(V)) return true;
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
        if (!negative || isZeroArray(reduced)) return reduced;
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
        if ((n[0] & 1L) == 0L)
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
    public LargeInteger nextProbablyPrimeInteger() {
        if (lessThan(TWO)) return TWO;
        else if (lessThan(THREE)) return THREE;
        else if(lessThan(FIVE)) return FIVE;
        LargeInteger candidate = add(1);
        int rem30 = (int) candidate.mod(30);
        int wheelIndex = 0;
        for (int i = 1; i < WHEEL_30_RESIDUES.length; i++)
            if (WHEEL_30_RESIDUES[i] >= rem30){
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
    @SuppressWarnings("unused")
    public static LargeInteger randomPrimeChunks(int chunks) {
        while (true) {
            LargeInteger p = randomChunks(chunks).nextProbablyPrimeInteger();
            if (p.limbs.length == chunks)
                return p;
        }
    }
    @SuppressWarnings("unused")
    public static LargeInteger randomPrimeBits(long bits) {
        while (true) {
            LargeInteger p = randomBits(bits).nextProbablyPrimeInteger();
            if (p.bitLength() == bits)
                return p;
        }
    }
    @SuppressWarnings("unused")
    public int legendreSymbol(LargeInteger other) {
        if (other.negative || other.isEven() || !other.isProbablyPrime())
            throw new IllegalArgumentException("Legendre symbol requires an odd prime modulus");
        int result = 1;
        if (negative)
            if (other.mod(3) == 1)
                result = -1;
        long[] exponent = shiftRightBits(subtractSmall(other.limbs, 1), 1);
        long[] m = new MontgomeryReducer(other.limbs).powMod(modMagnitudes(limbs, other.limbs), exponent);
        if (isZeroArray(m)) return 0;
        if (isOneArray(m)) return result;
        if (equalsArray(m, subtractSmall(other.limbs, 1))) return -result;
        throw new IllegalStateException("Invalid Legendre result; " + other + " may not be prime");
    }
    @SuppressWarnings("unused")
    public int jacobiSymbol(LargeInteger n) {
        if (n.negative || n.isEven())
            throw new IllegalArgumentException("Jacobi denominator must be positive and odd");
        LargeInteger a = mod(n);
        int result = 1;
        while (!isZeroArray(a.limbs)) {
            while (a.isEven()) {
                a = a.shiftRight(1);
                int nMod8 = (int)n.mod(8);
                if (nMod8 == 3 || nMod8 == 5)
                    result = -result;
            }
            LargeInteger temp = a;
            a = n;
            n = temp;
            if (a.mod(4) == 3 && n.mod(4) == 3) result = -result;
            a = a.mod(n);
        }
        return isOneArray(n.limbs) ? result : 0;
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
            return containsTwo ? new long[]{2L} : new long[0];
        // Number of odd integers in [oddLower, upperExclusive).
        long oddCountLong = ((upperExclusive - oddLower) + 1L) >>> 1;
        if (oddCountLong > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Sieve block is too large: " + oddCountLong + " odd candidates");
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
    private record PowerForm(long[] base, long exponent) {}
    private static PowerForm detectPerfectPower(long[] n) {
        n = trimLeadingZeros(n);
        if (compareSmall(n, 4) < 0) return null;
        long[] current = n;
        long exponent = 1;
        long maxPrime = bitLength(current) - 1;
        int[] primes = sieve(Math.toIntExact(maxPrime));
        for (int prime : primes) {
            // If current = a^p with a >= 2, then current >= 2^p.
            // Therefore p <= floor(log2(current)) = bitLength(current) - 1.
            if (prime > bitLength(current) - 1) break;
            while (true) {
                long[] root = nthRootFloorMagnitude(current, prime);
                if (comparePowTo(root, prime, current) != 0) break;
                current = root;
                exponent = Math.multiplyExact(exponent, prime);
                // Can't possibly extract p again.
                if (prime > bitLength(current) - 1) break;
            }
        }
        return exponent == 1 ? null : new PowerForm(current, exponent);
    }
    private static PowerForm detectPowerPlusOne(long[] n) {
        if (compareSmall(n,2) <= 0) return null;
        PowerForm form = detectPerfectPower(subtractSmall(n, 1));
        if (form == null) return null;
        long exponent = form.exponent;
        long[] base = form.base;
        while ((exponent & 1L) == 0) {
            base = squareMagnitude(base);
            exponent >>>= 1;
        }
        if (exponent == 1) return null;
        return new PowerForm(base, exponent);
    }
    private static PowerForm detectPowerMinusOne(long[] n) {return detectPerfectPower(addSmall(n, 1));}
    private static final class MagKey {
        long[] key;
        int hashCode;
        MagKey(long[] key) {
            this.key = key;
            hashCode = Arrays.hashCode(key);
        }
        @Override
        public boolean equals(Object o) {
            if (o instanceof MagKey other)
                return equalsArray(key, other.key);
            return false;
        }
        @Override
        public int hashCode() {return hashCode;}
    }
    public enum AlgorithmChoice{
        POLLARD_P_MINUS_ONE,
        BRENT_RHOS,
        FERMAT,
        ECM,
        QUADRATIC_SIEVE
    }
    private static final class FactorJob {
        final long[] n;
        final long B;
        QSFactorBase factorBase;
        int[] fbIndex;

        boolean pm1Done;
        boolean brentDone;
        boolean fermatDone;

        int ecmPlanIndex;
        int lastQSProbePlan;
        int nextECMCurve;
        ECMModulusContext context;
        double qsSeconds;

        QSFullRelationPool relations;
        QSPartialRelationPool partials;
        PartialGraph partialGraph = new PartialGraph();

        FactorJob(long[] n) {
            this.n = trimLeadingZeros(n);
            this.context = new ECMModulusContext(this.n);
            double lnN = logMag(this.n);
            double lnLnN = StrictMath.log(lnN);
            this.B = Math.max(100, Math.round(Math.exp(0.55 * Math.sqrt(lnN * lnLnN))));
            this.factorBase = null;
        }
        private FactorJob(long[] n, int ecmPlanIndex, int nextECMCurve){
            this.n = trimLeadingZeros(n);
            this.ecmPlanIndex = ecmPlanIndex;
            this.nextECMCurve = nextECMCurve;
            this.context = new ECMModulusContext(this.n);
            double lnN = logMag(n);
            double lnLnN = StrictMath.log(lnN);
            long B = Math.round(Math.exp(0.55 * Math.sqrt(lnN * lnLnN)));
            this.B = Math.max(B, 100);
            this.factorBase = null;
        }
        /*
         * A child starts its own cheap-method checks, but inherits the
         * parent's ECM position.
         *
         * QS state cannot be inherited because its relations belonged
         * to the old modulus.
         */
        FactorJob child(long[] childN) {return new FactorJob(childN, ecmPlanIndex, nextECMCurve);}
    }
    private static void splitAndQueue(ArrayDeque<FactorJob> work, FactorJob parent, long[] candidate) {
        if (!isNontrivialFactor(candidate, parent.n))
            throw new IllegalArgumentException("Invalid factor returned by factoring algorithm");
        long[] h = divideMagnitudesExact(parent.n, candidate);
        if (!equalsArray(multiplyMagnitudes(candidate, h), parent.n))
            throw new AssertionError("Factor split does not reconstruct parent");
        FactorJob gJob = parent.child(candidate);
        FactorJob hJob = parent.child(h);
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
    private static boolean isNontrivialFactor(long[] d, long[] n) {
        return isZeroArray(modMagnitudes(n, d)) && !isOneArray(d) && compareArray(d, n) < 0;}
    private static void addFactor(LargeMap<MagKey, Long> p, MagKey base, long exponent) {
        if (!p.containsKey(base)) p.put(base, exponent);
        else p.put(base, p.get(base) + exponent);
    }
    @FunctionalInterface
    private interface FactorCall { long[] run();}
    private record TimedFactorResult(long[] factor, long elapsedNanos){}
    private static TimedFactorResult timeFactorCall(FactorCall call) {
        long start = TMX.getCurrentThreadCpuTime();
        long[] factor = call.run();
        return new TimedFactorResult(factor, TMX.getCurrentThreadCpuTime() - start);
    }
    /**
     * Factors a limbs into multiplicative components. Usually this means prime factors.
     * In strict mathematics, 1 has an empty factorization, 0 has no prime factorization, and negatives are handled by a separate negative.
     * However, programming prefers total functions over philosophical purity.
     * To account for this, 1 returns {1 -> 1}, 0 returns {0 -> 1}, and -1 is treated as another factor.
     * Also, if the limbs decides to become a cryptography final boss, the remaining chunk may be treated as ONE atomic factor. So definitely not full prime factorization as one might expect.
     */
    public LargeMap<LargeInteger, LargeFraction> factor() {
        LargeMap<LargeInteger, LargeFraction> result = new LargeMap<>();
        if (negative)
            result.put(NEGATIVE_ONE, LargeFraction.ONE);
        if (isOneArray(limbs)) return result;
        LargeMap<MagKey, Long> bag = factorMag(limbs);
        for (MagKey factor : bag.keys())
            result.put(new LargeInteger(factor.key, false), new LargeFraction(bag.get(factor)));
        return result;
    }
    private static LargeMap<MagKey, Long> factorMag(long[] limbs) {
        LargeMap<MagKey, Long> factorMap = new LargeMap<>();
        if (isZeroArray(limbs)) {
            factorMap.put(new MagKey(ZERO_MAG), 1L);
            return factorMap;
        }else if (isOneArray(limbs)) return factorMap;
        long[] copy = limbs.clone();
        if(compareSmall(copy, 1_000_000_000_000L) > 0) {
            //detect 1 + 4b^4 = (1 - 2b + 2b^2)(1 + 2b + 2b^2)
            long[] nMinusOne = subtractSmall(copy, 1);
            long[] sqrtNMinusOne = sqrtFloorMagnitude(nMinusOne);
            if (equalsArray(squareMagnitude(sqrtNMinusOne), nMinusOne) && isEvenMag(sqrtNMinusOne)) {
                divideMagnitudeExactInPlace(sqrtNMinusOne, 2);
                long[] sqrtDR = sqrtFloorMagnitude(sqrtNMinusOne);
                if (equalsArray(squareMagnitude(sqrtDR), sqrtDR)) {
                    long[] factor1 = addSmall(subtractMagnitudes(
                            shiftLeftBits(sqrtNMinusOne, 1), shiftLeftBits(sqrtDR, 1)), 1);
                    long[] factor2 = addSmall(addMagnitudes(
                            shiftLeftBits(sqrtNMinusOne, 1), shiftLeftBits(sqrtDR, 1)), 1);
                    if (isNontrivialFactor(factor1, copy) && isNontrivialFactor(factor2, copy)) {
                        LargeMap<MagKey, Long> F1 = factorMag(factor1);
                        for (MagKey K : F1.keys())
                            addFactor(factorMap, K, F1.get(K));
                        LargeMap<MagKey, Long> F2 = factorMag(factor2);
                        for (MagKey K : F2.keys())
                            addFactor(factorMap, K, F2.get(K));
                        //sort factors
                        for (LargeInteger i = ZERO; i.lessThan(factorMap.size()); i = i.add(1)) {
                            for (LargeInteger j = i.add(1); j.lessThan(factorMap.size()); j = j.add(1))
                                if (compareArray(factorMap.keyAtIndex(i).key, factorMap.keyAtIndex(j).key) > 0)
                                    factorMap.swap(factorMap.keyAtIndex(i), factorMap.keyAtIndex(j));
                        }
                        return factorMap;
                    }
                }
            }
            // detect x^n - 1
            PowerForm minusForm = detectPowerMinusOne(copy);
            if (minusForm != null) {
                LargeInteger n = valueOf(minusForm.exponent);
                LargeList<LargeInteger> divisors = n.divisors();
                for (LargeInteger d : divisors) {
                    long[] value = ((LargeInteger) LargePolynomial.cyclotomic(d)
                                    .evaluateAt(new LargeInteger(minusForm.base, false))).limbs;
                    if (isOneArray(value)) continue;
                    if (equalsArray(value, copy)) break;
                    copy = divideMagnitudesExact(copy, value);
                    LargeMap<MagKey, Long> subFactors = factorMag(value);
                    for (MagKey base : subFactors.keys())
                        addFactor(factorMap, base, subFactors.get(base));
                }
            }
            // detect x^n + 1
            PowerForm plusForm = detectPowerPlusOne(copy);
            if (plusForm != null && (plusForm.exponent & 1L) == 1) {
                LargeInteger n = valueOf(plusForm.exponent);
                LargeList<LargeInteger> divisors = n.divisors();
                for (LargeInteger d : divisors) {
                    long[] value = ((LargeInteger) LargePolynomial.cyclotomic(d.shiftLeft(1))
                                    .evaluateAt(new LargeInteger(plusForm.base, false))).limbs;
                    if (isOneArray(value)) continue;
                    if (equalsArray(value, copy)) break;
                    copy = divideMagnitudesExact(copy, value);
                    LargeMap<MagKey, Long> subFactors = factorMag(value);
                    for (MagKey base : subFactors.keys())
                        addFactor(factorMap, base, subFactors.get(base));
                }
            }
        }
        int remLength = copy.length;
        for (int k : SMALL_PRIMES) {
            int exponent = 0;
            long rec = Long.divideUnsigned(-1, k);
            while (modSmallInt(copy, remLength, k, rec) == 0) {
                exponent++;
                divideMagnitudeExactInPlace(copy, k);
                while(remLength > 1 && copy[remLength-1] == 0) remLength--;
            }
            if(exponent != 0) addFactor(factorMap, new MagKey(new long[]{k}), exponent);
            if(remLength == 1 && copy[remLength-1] == 1){
                for (LargeInteger i = ZERO; i.lessThan(factorMap.size()); i = i.add(1)) {
                    for (LargeInteger j = i.add(1); j.lessThan(factorMap.size()); j = j.add(1))
                        if (compareArray(factorMap.keyAtIndex(i).key, factorMap.keyAtIndex(j).key) > 0)
                            factorMap.swap(factorMap.keyAtIndex(i), factorMap.keyAtIndex(j));
                }
                return factorMap;
            }
        }
        copy = trimLeadingZeros(copy);
        if (compareSmall(copy, 1000000000000L) < 0) {
            factorMap.put(new MagKey(copy), 1L);
            for (LargeInteger i = ZERO; i.lessThan(factorMap.size()); i = i.add(1)) {
                for (LargeInteger j = i.add(1); j.lessThan(factorMap.size()); j = j.add(1))
                    if (compareArray(factorMap.keyAtIndex(i).key, factorMap.keyAtIndex(j).key) > 0)
                        factorMap.swap(factorMap.keyAtIndex(i), factorMap.keyAtIndex(j));
            }
            return factorMap;
        }
//        FactorCacheDatabase cache = FactorCacheDatabase.instance();
        ArrayDeque<FactorJob> work = new ArrayDeque<>();
        FactorJob task = new FactorJob(copy);
        task.lastQSProbePlan = -1;
        work.push(task);
        while (!work.isEmpty()) {
            task = work.pop();
            final long[] n = task.n;
            if (isOneArray(n))
                continue;
            if (isProbablyPrimeMag(n)) {
                // Add prime to result.
                addFactor(factorMap, new MagKey(n), 1);
                continue;
            }
            LargeInteger number = new LargeInteger(n, false);
//            LargeInteger cached = cache.findFactor(number);
//            if (cached != null) {
//                splitAndQueue(work, task, cached.limbs);
//                continue;
//            }
            long[] factor;
            if (!task.fermatDone) {
                task.fermatDone = true;
                TimedFactorResult fermat = timeFactorCall(() -> fermatCheck(n));
                //System.out.println("Fermat check time for " + number + ":  " + formatSeconds(fermat.elapsedNanos));
                factor = fermat.factor;
                if (isNontrivialFactor(factor, n)) {
//                    cache.save(number, new LargeInteger(factor, false), AlgorithmChoice.FERMAT);
                    splitAndQueue(work, task, factor);
                    continue;
                }
            }
            if (!task.pm1Done) {
                task.pm1Done = true;
                TimedFactorResult pm1 = timeFactorCall(() -> pollardPMinusOneEasy(n));
                //System.out.println("Pollard PMinusOne time for " + number + ":  " + formatSeconds(pm1.elapsedNanos));
                factor = pm1.factor;
                if (isNontrivialFactor(factor, n)) {
//                    cache.save(number, new LargeInteger(factor, false), AlgorithmChoice.POLLARD_P_MINUS_ONE);
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
//                    cache.save(number, new LargeInteger(factor, false), AlgorithmChoice.BRENT_RHOS);
                    splitAndQueue(work, task, factor);
                    continue;
                }
            }
            if (task.ecmPlanIndex >= ECM_EXTREME_PLANS.length) {
                System.out.println(number + " is too difficult to factor; treated as composite atomic");
                addFactor(factorMap, new MagKey(n), 1L);
                continue;
            }
            ECMPlan plan = ECM_EXTREME_PLANS[task.ecmPlanIndex];
            if (task.B <= 15_000_000 && task.ecmPlanIndex > 0) {
                double nextECMSeconds = ECM_RUNTIME.estimateSeconds(task, plan);
                if (task.qsSeconds < nextECMSeconds) {
                    factor = quadraticSieve(task, nextECMSeconds);
                    if (isNontrivialFactor(factor, n)) {
                        splitAndQueue(work, task, factor);
                        continue;
                    }
                }
            }
            factor = runThirtyTwoCurves(task, plan);
            if (isNontrivialFactor(factor, n)) {
                splitAndQueue(work, task, factor);
                continue;
            }
            task.nextECMCurve += 32;
            if(task.nextECMCurve == plan.curves){
                task.nextECMCurve = 0;
                task.ecmPlanIndex++;
            }
            work.push(task);
        }
        //sort factors
        for (LargeInteger i = ZERO; i.lessThan(factorMap.size()); i = i.add(1)) {
            for (LargeInteger j = i.add(1); j.lessThan(factorMap.size()); j = j.add(1))
                if (compareArray(factorMap.keyAtIndex(i).key, factorMap.keyAtIndex(j).key) > 0)
                    factorMap.swap(factorMap.keyAtIndex(i), factorMap.keyAtIndex(j));
        }
        return factorMap;
    }
    public LargeInteger totient() {
        if (isZeroArray(limbs)) throw new ArithmeticException("Totient undefined for 0");
        else if (isOneArray(limbs)) return ONE;
        LargeInteger n = abs();
        LargeMap<LargeInteger, LargeFraction> factors = n.factor();
        LargeInteger result = n.clone();
        for (LargeInteger p : factors.keys()) {
            if (!p.isProbablyPrime())
                throw new ArithmeticException("Totient requires full prime decomposition; composite atomic factor found: " + p);
            result = result.divide(p).multiply(p.subtract(1));
        }
        return result;
    }
    public LargeList<LargeInteger> divisors() {
        LargeList<LargeInteger> result = new LargeList<>();
        if (isZeroArray(limbs) || isOneArray(limbs)) return result;
        LargeMap<LargeInteger, LargeFraction> factorMap = abs().factor();
        result.add(ONE);
        if (isOneArray(factorMap.keyAtIndex(ZERO).limbs)) return result;
        for (LargeInteger p : factorMap.keys()) {
            if (p.negative) continue;
            if(!p.isProbablyPrime())
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
            if (!isOneArray(pr.get(p).denominator.limbs)) throw new ArithmeticException("Result is not rational");
            if (pr.get(p).numerator.negative) throw new ArithmeticException("Result is not an integer");
            LargeInteger expTemp = pr.get(p).numerator.abs();
            LargeInteger basTemp = p.clone();
            while (true) {
                if (expTemp.isOdd()) {
                    result = result.multiply(basTemp);
                    if (isOneArray(expTemp.limbs)) break;
                }
                basTemp = basTemp.square();
                expTemp = expTemp.shiftRight(1);
            }
        }
        return result;
    }
    private record PM1Plan(int B1, int B2) {}
    private record PM1Stage1Result(long[] state, long[] factor, boolean saturated) {}
    private static long[] pollardPMinusOneEasy(long[] n) {
        if (isEvenMag(n)) return TWO_MAG;
        PM1Plan[] plans = {new PM1Plan(10_000, 100_000), new PM1Plan(20000, 200000)};
        // Sieve once through the largest B2.
        int[] primes = sieve(plans[plans.length - 1].B2);
        MontgomeryReducer reducer = new MontgomeryReducer(n);
        long[] state = reducer.toMontgomery(TWO_MAG);
        int previousB1 = 1;
        for (PM1Plan plan : plans) {
            PM1Stage1Result stage1 = pollardPMinusOneStage1(n, state, previousB1, plan.B1, primes, reducer);
            if (isNontrivialFactor(stage1.factor, n))
                return stage1.factor;
            if (stage1.saturated)
                return ONE_MAG;
            state = stage1.state;
            long[] factor = pollardPMinusOneStage2(n, state, primes, plan.B1, plan.B2, reducer);
            if (isNontrivialFactor(factor, n))
                return factor;
            previousB1 = plan.B1;
        }
        return ONE_MAG;
    }
    private static PM1Stage1Result pollardPMinusOneStage1(long[] n, long[] initialState,
                                                          int oldBound, int newBound, int[] primes,
                                                          MontgomeryReducer reducer) {
        long[] a = initialState;
        long[] batchStartState = initialState;
        long[] batchExponent = ONE_MAG;

        int batchStartIndex = 0;
        int batchCount = 0;

        for (int i = 0; i < primes.length; i++) {
            int q = primes[i];
            if (q > newBound)
                break;

            long oldPower = largestPrimePowerAtMost(q, oldBound);
            long newPower = largestPrimePowerAtMost(q, newBound);
            long multiplier = newPower / oldPower;

            if (multiplier == 1)
                continue;

            batchExponent = multiplySmall(batchExponent, multiplier);
            batchCount++;

            if (batchCount == 256) {
                a = reducer.powMontgomery(a, batchExponent);
                long[] g = gcdMagnitudes(modSubtract(a, reducer.rModM, n), n);
                if (isNontrivialFactor(g, n))
                    return new PM1Stage1Result(a, g, false);
                if (equalsArray(g, n)) {
                    long[] replay = batchStartState;

                    for (int j = batchStartIndex; j <= i; j++) {
                        int p = primes[j];
                        long oldP = largestPrimePowerAtMost(p, oldBound);
                        long newP = largestPrimePowerAtMost(p, newBound);
                        long step = newP / oldP;
                        if (step == 1) continue;
                        replay = reducer.powMontgomery(replay, new long[]{step});
                        long[] replayGcd = gcdMagnitudes(modSubtract(replay, reducer.rModM, n), n);
                        if (isNontrivialFactor(replayGcd, n))
                            return new PM1Stage1Result(replay, replayGcd, false);
                        if (equalsArray(replayGcd, n))
                            break;
                    }
                    return new PM1Stage1Result(a, ONE_MAG, true);
                }
                batchStartState = a;
                batchStartIndex = i + 1;
                batchExponent = ONE_MAG;
                batchCount = 0;
            }
        }
        if (batchCount != 0) {
            a = reducer.powMontgomery(a, batchExponent);
            long[] g = gcdMagnitudes(modSubtract(a, reducer.rModM, n), n);
            if (isNontrivialFactor(g, n))
                return new PM1Stage1Result(a, g, false);
            if (equalsArray(g, n))
                return new PM1Stage1Result(a, ONE_MAG, true);
        }
        return new PM1Stage1Result(a, ONE_MAG, false);
    }
    private static long largestPrimePowerAtMost(int q, int bound) {
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
        int D = 2310;
        long[][] baby = new long[D][];
        long[] power = reducer.rModM;
        for (int r = 1; r < D; r++) {
            power = reducer.multiply(power, stage1Montgomery);
            if (gcdLong(r, D) == 1)
                baby[r] = power;
        }
        // power currently equals x^(D - 1), so this gives x exponentiated to D.
        long[] xToD = reducer.multiply(power, stage1Montgomery);
        int firstPrime = primes[start];
        int currentM = (firstPrime + D - 1) / D;
        long[] giant = reducer.powMontgomery(xToD, new long[]{currentM});
        long[] product = reducer.rModM;
        long[][] batchTerms = new long[1024][];
        int terms = 0;
        for (int i = start; i < primes.length; i++) {
            int q = primes[i];
            if (q > B2) break;
            int targetM = (q + D - 1) / D;
            while(currentM < targetM){
                giant = reducer.multiply(giant, xToD);
                currentM++;
            }
            int r = targetM * D - q;
            // q > B1, so q cannot be ONE of D's small prime factors.
            if (r == 0 || baby[r] == null) continue;
            long[] term = reducer.subtract(giant, baby[r]);
            product = reducer.multiply(product, term);
            batchTerms[terms++] = term;
            if (terms == 1024) {
                long[] factor = checkPM1Stage2Batch(product, batchTerms, terms, n);
                if (isNontrivialFactor(factor, n))
                    return factor;
                product = reducer.rModM;
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
        if (equalsArray(g, n)) {
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
        long[] y = Arrays.copyOf(
                reducer.toMontgomery(
                        randomMagnitudeBetween(TWO_MAG, subtractSmall(n, 2))
                ),
                len
        );

        long[] c = Arrays.copyOf(
                reducer.toMontgomery(
                        randomMagnitudeBetween(ONE_MAG, subtractSmall(n, 1))
                ),
                len
        );

        long m = 1024;
        long r = 1;
        long times = 0;

        long[] g = ONE_MAG;

        // Fixed-width Montgomery work buffers.
        long[] x    = new long[len];
        long[] ys   = new long[len];
        long[] q    = new long[len];
        long[] diff = new long[len];

        while (isOneArray(g) && times < steps) {

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

            while (k < r && isOneArray(g) && times < steps) {

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
                Arrays.fill(q, 0L);
                System.arraycopy(
                        reducer.rModM,
                        0,
                        q,
                        0,
                        reducer.rModM.length
                );

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

                    if (isZeroArray(diff))
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
        if (equalsArray(g, n)) {
            g = ONE_MAG;
            for (int i = 0; i < 10_000 && isOneArray(g); i++) {
                // ys = ys^2 + c mod n
                reducer.squareInto(ys, ys);
                reducer.addInto(ys, c, ys);
                // diff = x - ys mod n
                reducer.subtractInto(x, ys, diff);
                if (isZeroArray(diff))
                    return ONE_MAG;
                g = gcdMagnitudes(diff, n);
            }
            if (isNontrivialFactor(g, n))
                return g;
        }
        return ONE_MAG;
    }
    public static long[] fermatCheck(long[] n) {
        long[] sqrtN = sqrtCeilMagnitude(n);
        long[] diff = subtractMagnitudes(squareMagnitude(sqrtN), n);
        long[] sqrtDiff = sqrtFloorMagnitude(diff);
        if (equalsArray(squareMagnitude(sqrtDiff), diff))
            return addMagnitudes(sqrtN, sqrtDiff);
        return ONE_MAG;
    }

    private record MontgomeryPoint(long[] X, long[] Z) {}
    private static final class MontgomeryCurve {
        final long[] A24; //(A+2)/4 mod n
        final MontgomeryReducer reducer;
        final long[] n;
        void requireCanonicalPoint(MontgomeryPoint P) {
            reducer.requireReducedMagnitude(P.X, "P.X");
            reducer.requireReducedMagnitude(P.Z, "P.Z");
        }
        final long[] s0;
        final long[] s1;
        final long[] s2;
        final long[] s3;
        MontgomeryCurve(MontgomeryReducer reducer, long[] A24Normal) {
            this.reducer = reducer;
            A24 = reducer.toMontgomery(A24Normal);
            n = reducer.modulus;

            int k = reducer.k;
            s0 = new long[k];
            s1 = new long[k];
            s2 = new long[k];
            s3 = new long[k];
        }
        MontgomeryPoint doublePoint(MontgomeryPoint P, ECMStats stats) {
            requireCanonicalPoint(P);
            return doublePointCanonical(P, stats);
        }
        private MontgomeryPoint doublePointCanonical(MontgomeryPoint P, ECMStats stats) {
            long t0 = 0;
            if (stage2)
                t0 = TMX.getCurrentThreadCpuTime();

            // s0 = X + Z
            reducer.addInto(P.X, P.Z, s0);

            // s1 = X - Z
            reducer.subtractInto(P.X, P.Z, s1);

            // s0 = (X + Z)^2 = AA
            reducer.squareInto(s0, s0);

            // s1 = (X - Z)^2 = BB
            reducer.squareInto(s1, s1);

            // s2 = AA - BB = E
            reducer.subtractInto(s0, s1, s2);

            long[] X2 = new long[reducer.k];
            long[] Z2 = new long[reducer.k];

            // X2 = AA * BB
            reducer.multiplyInto(s0, s1, X2);

            // s3 = A24 * E
            reducer.multiplyInto(A24, s2, s3);

            // s3 = BB + A24*E
            reducer.addInto(s1, s3, s3);

            // Z2 = E * (...)
            reducer.multiplyInto(s2, s3, Z2);

            if (stage2){
                stats.stage2DoublingTime += TMX.getCurrentThreadCpuTime() - t0;
                stats.stage2MontMultiplications += 2;
            }

            return new MontgomeryPoint(X2, Z2);
        }
        MontgomeryPoint diffAdd(MontgomeryPoint P, MontgomeryPoint Q, MontgomeryPoint PMinusQ, ECMStats stats) {
            requireCanonicalPoint(P);
            requireCanonicalPoint(Q);
            requireCanonicalPoint(PMinusQ);
            return diffAddCanonical(P, Q, PMinusQ, stats);
        }
        private MontgomeryPoint diffAddCanonical(MontgomeryPoint P, MontgomeryPoint Q, MontgomeryPoint PMinusQ,
                ECMStats stats) {
            long t0 = 0;
            if (stage2)
                t0 = TMX.getCurrentThreadCpuTime();
            // s0 = Xp + Zp
            reducer.addInto(P.X, P.Z, s0);
            // s1 = Xp - Zp
            reducer.subtractInto(P.X, P.Z, s1);
            // s2 = Xq + Zq
            reducer.addInto(Q.X, Q.Z, s2);
            // s3 = Xq - Zq
            reducer.subtractInto(Q.X, Q.Z, s3);
            // s3 = (Xq-Zq)(Xp+Zp) = DA
            reducer.multiplyInto(s3, s0, s3);
            // s2 = (Xq+Zq)(Xp-Zp) = CB
            reducer.multiplyInto(s2, s1, s2);
            // s0 no longer needed:
            // s0 = DA + CB
            reducer.addInto(s3, s2, s0);
            // s1 no longer needed:
            // s1 = DA - CB
            reducer.subtractInto(s3, s2, s1);
            reducer.squareInto(s0, s0);
            reducer.squareInto(s1, s1);

            long[] X = new long[reducer.k];
            long[] Z = new long[reducer.k];

            reducer.multiplyInto(PMinusQ.Z, s0, X);
            reducer.multiplyInto(PMinusQ.X, s1, Z);
            if (stage2){
                stats.stage2DiffAdditionTime += TMX.getCurrentThreadCpuTime() - t0;
                stats.stage2MontMultiplications += 4;
            }
            return new MontgomeryPoint(X, Z);
        }
        private static final class DblAddResult {
            MontgomeryPoint doubled;
            MontgomeryPoint added;
        }
        private void doubleAndDiffAddCanonical(MontgomeryPoint P, MontgomeryPoint Q,
                                                       MontgomeryPoint PMinusQ, DblAddResult out, ECMStats stats) {
            long[] a = reducer.addCanonical(P.X, P.Z);
            long[] b = reducer.subtractCanonical(P.X, P.Z);
            long[] aa = reducer.square(a);
            long[] bb = reducer.square(b);
            long[] e = reducer.subtractCanonical(aa, bb);
            long[] c = reducer.addCanonical(Q.X, Q.Z);
            long[] d = reducer.subtractCanonical(Q.X, Q.Z);
            long[] da = reducer.multiply(d, a);
            long[] cb = reducer.multiply(c, b);
            long[] plus = reducer.addCanonical(da, cb);
            long[] minus = reducer.subtractCanonical(da, cb);
            MontgomeryPoint doubled = new MontgomeryPoint(reducer.multiply(aa, bb),
                    reducer.multiply(e, reducer.addCanonical(bb, reducer.multiply(A24, e))));
            MontgomeryPoint added = new MontgomeryPoint(reducer.multiply(PMinusQ.Z, reducer.square(plus)),
                    reducer.multiply(PMinusQ.X, reducer.square(minus)));
            if(stage2)
                stats.stage2MontMultiplications += 7;
            out.doubled = doubled;
            out.added = added;
        }
        MontgomeryPoint multiply(MontgomeryPoint P, long k, ECMStats stats) {
            if (k <= 0L) throw new IllegalArgumentException("Scalar must be positive");
            int shifts = Long.numberOfTrailingZeros(k);
            long oddPart = k >>> shifts;
            MontgomeryPoint result;
            if (oddPart == 1L) {
                result = P;
                for (int i = 0; i < shifts; i++)
                    result = doublePointCanonical(result, stats);
                return result;
            }
            PracChain chain = PracChain.buildPracChain(k);
            return multiplyPrac(P, chain.operations, 0, chain.operations.length, chain.trailingDoubling, stats);
        }
        MontgomeryPoint multiplyPrac(MontgomeryPoint P, byte[] operations, int from, int toExclusive,
                                     int trailingDoubling, ECMStats stats) {
            if (from < 0 || toExclusive < from || toExclusive > operations.length || trailingDoubling < 0)
                throw new IllegalArgumentException("Invalid PRAC slice");
            requireCanonicalPoint(P);
            MontgomeryPoint A = doublePointCanonical(P, stats);
            MontgomeryPoint B = P;
            MontgomeryPoint C = P;
            DblAddResult pair = new DblAddResult();
            for (int index = from; index < toExclusive; index++) {
                switch (operations[index]) {
                    case PracChain.SWAP -> {
                        MontgomeryPoint temp = A;
                        A = B;
                        B = temp;
                    }case PracChain.CASE_1 -> {
                        MontgomeryPoint sum = diffAddCanonical(A, B, C, stats);
                        MontgomeryPoint newA = diffAddCanonical(sum, A, B, stats);
                        MontgomeryPoint newB = diffAddCanonical(B, sum, A, stats);
                        A = newA;
                        B = newB;
                    }case PracChain.CASE_2, PracChain.CASE_4 -> {
                        doubleAndDiffAddCanonical(A, B, C, pair, stats);
                        A = pair.doubled;
                        B = pair.added;
                    }case PracChain.CASE_3 -> {
                        MontgomeryPoint oldB = B;
                        B = diffAddCanonical(B, A, C, stats);
                        C = oldB;
                    }case PracChain.CASE_5 -> {
                        // Computes 2A and A+C, where |A-C| = B.
                        doubleAndDiffAddCanonical(A, C, B, pair, stats);
                        A = pair.doubled;
                        C = pair.added;
                    }case PracChain.CASE_6 -> {
                        MontgomeryPoint oldB = B;
                        doubleAndDiffAddCanonical(A, B, C, pair, stats);
                        MontgomeryPoint doubled = pair.doubled;
                        MontgomeryPoint added = pair.added;

                        B = diffAddCanonical(doubled, added, C, stats);
                        A = diffAddCanonical(doubled, A, A, stats);
                        C = oldB;
                    }case PracChain.CASE_7 -> {
                        MontgomeryPoint oldA = A;
                        MontgomeryPoint oldB = B;
                        doubleAndDiffAddCanonical(oldA, oldB, C, pair, stats);
                        B = diffAddCanonical(pair.added, oldA, oldB, stats);
                        A = diffAddCanonical(pair.doubled, oldA, oldA, stats);
                    }case PracChain.CASE_8 -> {
                        MontgomeryPoint oldA = A;
                        MontgomeryPoint oldB = B;
                        MontgomeryPoint oldC = C;
                        doubleAndDiffAddCanonical(oldA, oldB, oldC, pair, stats);
                        A = diffAddCanonical(pair.doubled, oldA, oldA, stats);
                        B = pair.added;
                        C = diffAddCanonical(oldA, oldC, oldB, stats);
                    }case PracChain.CASE_9 -> {
                        // Computes 2B and B+C, where |B-C| = A.
                        doubleAndDiffAddCanonical(B, C, A, pair, stats);
                        B = pair.doubled;
                        C = pair.added;
                    }default -> throw new IllegalStateException("Unknown PRAC operation: " + operations[index]);
                }
            }
            MontgomeryPoint result = diffAddCanonical(A, B, C, stats);
            for (int i = 0; i < trailingDoubling; i++)
                result = doublePointCanonical(result, stats);
            return result;
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
        return quotient * numerator
                + (remainder * numerator + denominator / 2L)
                / denominator;
    }

    private static long choosePracSplit(long oddK) {
        long center = goldenSplit(oddK);
        for (long offset = 0L; true; offset++) {
            long lower = center - offset;
            if (lower > oddK / 2L && gcdLong(oddK, lower) == 1L)
                return lower;
            if (offset != 0L) {
                long upper = center + offset;
                if (upper < oddK && gcdLong(oddK, upper) == 1L)
                    return upper;
            }
        }
    }

    private record PracChain(byte[] operations, int trailingDoubling, long oddPart) {
        static final byte SWAP = 0;
        static final byte CASE_1 = 1;
        static final byte CASE_2 = 2;
        static final byte CASE_3 = 3;
        static final byte CASE_4 = 4;
        static final byte CASE_5 = 5;
        static final byte CASE_6 = 6;
        static final byte CASE_7 = 7;
        static final byte CASE_8 = 8;
        static final byte CASE_9 = 9;
        static PracChain buildPracChain(long k) {
            if (k <= 0L)
                throw new IllegalArgumentException("k must be positive");
            int trailingDoubling = Long.numberOfTrailingZeros(k);
            long oddK = k >>> trailingDoubling;
            if (oddK == 1L)
                return new PracChain(new byte[0], trailingDoubling, oddK);
            long r = choosePracSplit(oddK);
            long d = oddK - r;
            long e = r - d;
            ByteArrayOutputStream operations = new ByteArrayOutputStream();
            while (d != e) {
                if (d < e) {
                    long temporary = d;
                    d = e;
                    e = temporary;
                    operations.write(SWAP);
                }
                // Apply the first qualifying PRAC condition.
                if (d - e <= e / 4L && (d + e) % 3L == 0L) {
                    operations.write(CASE_1);
                    d = (2L * d - e) / 3L;
                    e = (e - d) / 2L;
                } else if (d - e <= e / 4L && (d - e) % 6L == 0L) {
                    operations.write(CASE_2);
                    d = (d - e) / 2L;
                } else if ((d + 3L) / 4L <= e) {
                    operations.write(CASE_3);
                    d -= e;
                } else if ((d + e) % 2L == 0L) {
                    operations.write(CASE_4);
                    d = (d - e) / 2L;
                } else if ((d & 1L) == 0L) {
                    operations.write(CASE_5);
                    d /= 2L;
                } else if (d % 3L == 0L) {
                    operations.write(CASE_6);
                    d = d / 3L - e;
                } else if ((d + e) % 3L == 0L) {
                    operations.write(CASE_7);
                    d = (d - 2L * e) / 3L;
                } else if ((d - e) % 3L == 0L) {
                    operations.write(CASE_8);
                    d = (d - e) / 3L;
                } else {
                    operations.write(CASE_9);
                    e /= 2L;
                }
                if (d <= 0L || e <= 0L)
                    throw new IllegalStateException("Invalid PRAC reduction");
            }
            if (d != 1L)
                throw new IllegalStateException("PRAC pair was not coprime");
            return new PracChain(operations.toByteArray(), trailingDoubling, k);
        }
    }
    private static final class ECMCurveWork {
        final MontgomeryCurve curve;
        MontgomeryPoint point;
        boolean usable = true;

        ECMCurveWork(MontgomeryCurve curve, MontgomeryPoint point) {
            this.curve = curve;
            this.point = point;
        }
    }
    private static final class FactorFoundException extends RuntimeException {
        final long[] factor;
        FactorFoundException(long[] factor) {
            super(null, null, false, false); // no stack trace overhead
            this.factor = factor;
        }
    }
    private static boolean stage2;
    private static final class Stage2State {
        private static final int D = 2310;
        private static final int HALF_D = D >>> 1;
        final MontgomeryCurve curve;
        final MontgomeryPoint Q;
        final MontgomeryPoint[] baby;
        final MontgomeryPoint difference;

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

        Stage2State(MontgomeryCurve curve, MontgomeryPoint P, long firstStage2Prime, ECMStats stats) {
            this.curve = curve;
            Q = P;
            baby = new MontgomeryPoint[HALF_D];
            baby[0] = Q;
            MontgomeryPoint previousMultiple = Q;
            MontgomeryPoint currentMultiple = curve.doublePoint(Q, stats);

            for (int r = 2; r < HALF_D; r++) {
                if (gcdLong(r, D) == 1)
                    baby[r - 1] = currentMultiple;
                MontgomeryPoint nextMultiple = curve.diffAdd(currentMultiple, Q, previousMultiple, stats);
                previousMultiple = currentMultiple;
                currentMultiple = nextMultiple;
            }
            difference = curve.multiply(Q, D, stats);
            currentM = nearestCenterIndex(firstStage2Prime);
            if (currentM == 1L) {
                previous = null;
                current = difference;
            } else {
                previous = curve.multiply(Q, Math.multiplyExact(currentM - 1L, D), stats);
                current = curve.multiply(Q, Math.multiplyExact(currentM, D), stats);
            }
            blockProduct = new long[curve.reducer.k];
            System.arraycopy(curve.reducer.rModM, 0, blockProduct, 0, curve.reducer.k);
        }
        static long nearestCenterIndex(long q) {
            long m = q / D;
            if (q % D > HALF_D) m++;
            return Math.max(1L, m);
        }
        long[] stage2Term(long q, ECMStats stats, long[] left, long[] right, long[] out) {
            long targetM = nearestCenterIndex(q);
            while (currentM < targetM) {
                MontgomeryPoint next;
                if (currentM == 1L) next = curve.doublePoint(difference, stats);
                else next = curve.diffAdd(current, difference, previous, stats);
                previous = current;
                current = next;
                currentM++;
            }
            long center = currentM*D;
            long distance = Math.abs(center - q);
            if (distance == 0L) return null;
            MontgomeryPoint B = baby[(int) distance - 1];
            curve.reducer.multiplyInto(current.X, B.Z, left);
            curve.reducer.multiplyInto(B.X, current.Z, right);
            stats.stage2MontMultiplications += 2;
            curve.reducer.subtractInto(left, right, out);
            return out;
        }
        void clearStage2Block() {
            blockProduct = new long[curve.reducer.k];
            System.arraycopy(curve.reducer.rModM, 0, blockProduct, 0, curve.reducer.k);
            blockTermCount = 0;
            blockStartPrevious = null;
            blockStartCurrent = null;
            blockStartM = 0L;
        }
    }
    private static final class ECMStats {
        final ECMPlan plan;
        final long planStartNanos = TMX.getCurrentThreadCpuTime();

        int usableCurves;
        long curveCreateNanos;

        long stage1Nanos;
        long stage1SieveNanos = 0;
        long stage1ChainBuildNanos = 0;
        long stage1ApplyNanos = 0;
        long gcdStage1Nanos = 0;

        long stage2Nanos;
        long stage2GcdTime = 0;
        long stage2TermCount = 0;
        long stage2BlockCount = 0;
        long stage2DoublingTime = 0;
        long stage2DiffAdditionTime = 0;
        long blockProcessTime = 0;
        long blockTermCount = 0;
        long stage2MontMultiplications = 0;
        long replayPrime = -1;
        ECMStats(ECMPlan plan) {this.plan = plan;}
        void found(long[] factor, String location, int curve, long blockLower, long blockUpper) {
            System.out.println("ECM FACTOR FOUND: " + new LargeInteger(factor, false, true));
            System.out.println("Location of factor: " + location);
            System.out.println("Curve where factor was found: " + curve);
            if (blockLower != -1)
                System.out.println("Factor found in prime block between " + blockLower + " and " + blockUpper);
            if (location.equals("Stage 2 individual-term replay"))
                System.out.println("During replay, found found due to prime " + replayPrime);
            System.out.println();
        }
        long totalPlanNanos() {return TMX.getCurrentThreadCpuTime() - planStartNanos;}
    }
    private static final ECMPlan[] ECM_EXTREME_PLANS = {
            new ECMPlan(2_000L, 200_000L, 64),
            new ECMPlan(11_000L, 1_900_000L, 128),
            new ECMPlan(50_000L, 13_000_000L, 384),
            new ECMPlan(250_000L, 130_000_000L, 768),
            new ECMPlan(1_000_000L, 1_000_000_000L, 1824),
            new ECMPlan(3_000_000L, 5_700_000_000L, 5152),
            new ECMPlan(11_000_000L, 35_000_000_000L, 12288),
            new ECMPlan(43_000_000L, 240_000_000_000L, 32768),
            new ECMPlan(110_000_000, 1_200_000_000_000L, 65536),
            new ECMPlan(260_000_000, 13_700_000_000_000L, 131072),
            new ECMPlan(850_000_000, 89_600_000_000_000L, 393216),
            new ECMPlan(2_900_000_000L, 331_700_000_000_000L, 524288)
    };
    private record ECMRuntimeKey(int limbCount, long B1, long B2) {}
    private static final class ECMRuntimeAggregate {
        long totalNanos;
        long totalCurves;
        void add(long nanos) {
            totalCurves += 32;
            totalNanos += nanos;
        }
        double nanosPerCurve() {
            if (totalCurves == 0)
                return Double.NaN;
            return totalNanos / (double) totalCurves;
        }
    }
    private static final class ECMRuntimeModel {
        private final HashMap<ECMRuntimeKey, ECMRuntimeAggregate> samples = new HashMap<>();

        synchronized void observe(long[] n, ECMPlan plan, long elapsedNanos) {
            if (elapsedNanos <= 0) return;
            ECMRuntimeKey key = new ECMRuntimeKey(trimLeadingZeros(n).length, plan.B1, plan.B2);
            ECMRuntimeAggregate aggregate = samples.computeIfAbsent(key, _ ->
                    new ECMRuntimeAggregate());
            aggregate.add(elapsedNanos);
        }
        synchronized double estimateSeconds(FactorJob task, ECMPlan plan) {
            int limbs = task.n.length;
            ECMRuntimeAggregate best = null;
            int bestDistance = Integer.MAX_VALUE;
            for (var entry : samples.entrySet()) {
                ECMRuntimeKey key = entry.getKey();
                if (key.B1 != plan.B1 || key.B2 != plan.B2) continue;
                int distance = Math.abs(key.limbCount - limbs);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = entry.getValue();
                }
            }
            if (best == null) return Double.POSITIVE_INFINITY;
            double nanosPerCurve = best.nanosPerCurve();
            if (!Double.isFinite(nanosPerCurve))
                return Double.POSITIVE_INFINITY;
            return nanosPerCurve * (plan.curves - task.nextECMCurve) / 1e9;
        }
    }
    private static final ECMRuntimeModel ECM_RUNTIME = new ECMRuntimeModel();
    private record ECMPlan(long B1, long B2, int curves) {}
    private static final class ECMModulusContext {
        final long[] n;
        final long[] inv4;
        final MontgomeryReducer reducer;
        ECMModulusContext(long[] n) {
            this.n = n;
            GcdInverseMag eg = gcdInverseMagnitudes(FOUR_MAG, n);
            if (!isOneArray(eg.gcd)) throw new FactorFoundException(eg.gcd);
            inv4 = eg.inverse;
            reducer = new MontgomeryReducer(n);
        }
    }
    private record GcdInverseMag(long[] gcd, long[] inverse) {}
    private static GcdInverseMag gcdInverseMagnitudes(long[] a, long[] n) {
        a = trimLeadingZeros(a);
        n = trimLeadingZeros(n);

        long[] oldR = a;
        long[] r = n;

        // Coefficient of 'a'.
        long[] oldX = ONE_MAG;
        boolean oldXNegative = false;

        long[] x = ZERO_MAG;
        boolean xNegative = false;

        while (!isZeroArray(r)) {
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

                if (cmp >= 0) {
                    nextX = subtractMagnitudes(oldX, qx);
                    nextXNegative = false;
                } else {
                    nextX = subtractMagnitudes(qx, oldX);
                    nextXNegative = true;
                }

                if (isZeroArray(nextX))
                    nextXNegative = false;
            }
            oldX = x;
            oldXNegative = xNegative;
            x = nextX;
            xNegative = nextXNegative;
        }
        if (!isOneArray(oldR))
            return new GcdInverseMag(oldR, null);
        long[] inverse = modMagnitudes(oldX, n);
        if (oldXNegative && !isZeroArray(inverse))
            inverse = subtractMagnitudes(n, inverse);
        return new GcdInverseMag(oldR, inverse);
    }
    private static long[] runThirtyTwoCurves(FactorJob job, ECMPlan plan) {
        ECMModulusContext context = job.context;
        long[] n = context.n;
        MontgomeryReducer reducer = context.reducer;
        int remainingCurves = plan.curves() - job.nextECMCurve;
        if (remainingCurves <= 0) return ONE_MAG;
        ECMStats stats = new ECMStats(plan);
        ECMCurveWork[] works = new ECMCurveWork[32];
        try {
            ecmCurveCreation(n, context, works, stats);
            ecmStage1(job, works, plan, stats);
            ecmStage2(job, reducer, plan, works, stats);
        } catch(FactorFoundException e){return e.factor;}
        System.out.println("32 curves tried with bounds " + plan.B1 + " and " + plan.B2 + "; "
                + stats.usableCurves + " being useful");
        System.out.println("Time for creating all the curves: " + formatSeconds(stats.curveCreateNanos));
        System.out.println("Stage 1 sieve time: " + formatSeconds(stats.stage1SieveNanos));
        System.out.println("Stage 1 chain build time: " + formatSeconds(stats.stage1ChainBuildNanos));
        System.out.println("Stage 1 chain application time: " + formatSeconds(stats.stage1ApplyNanos));
        System.out.println("Stage 1 gcd time: " + formatSeconds(stats.gcdStage1Nanos));
        System.out.println("Total Stage 1 time: " + formatSeconds(stats.stage1Nanos));
        System.out.println("Stage 2 terms: " + stats.stage2TermCount);
        System.out.println("Stage 2 blocks: " + stats.stage2BlockCount);
        System.out.println("Stage 2 point doubling time: " + formatSeconds(stats.stage2DoublingTime));
        System.out.println("Stage 2 point differential addition time: " +
                formatSeconds(stats.stage2DiffAdditionTime));
        System.out.println("Stage 2 block process time: " + stats.blockProcessTime / 1e9 + " seconds over "
                + stats.blockTermCount + " terms");
        System.out.println("Stage 2 Montgomery multiplications: " + stats.stage2MontMultiplications);
        System.out.println("Stage 2 gcd time: " + formatSeconds(stats.stage2GcdTime));
        System.out.println("Total stage 2 time: " + formatSeconds(stats.stage2Nanos));
        long completedPlanNanos = stats.totalPlanNanos();
        System.out.println("Total time for this batch: " + formatSeconds(completedPlanNanos));
        System.out.println();
        ECM_RUNTIME.observe(n, plan, completedPlanNanos);
        return ONE_MAG;
    }
    private static void ecmCurveCreation(long[] n, ECMModulusContext context,
                                         ECMCurveWork[] works, ECMStats stats){
        long t0 = TMX.getCurrentThreadCpuTime();
        int curveCount = 0;
        while (curveCount < works.length) {
            long[] sigma = randomMagnitudeBetween(SIX_MAG, n);
            // Normal residue
            long[] sigmaSquared = modMagnitudes(squareMagnitude(sigma), n);
            long[] u = modMagnitudes(subtractSmall(sigmaSquared, 5), n);
            long[] v = modMagnitudes(shiftLeftBits(sigma, 2), n);
            int cmp = compareArray(v, u);
            boolean negative = cmp < 0;
            long[] diff = cmp >= 0 ? subtractMagnitudes(v, u) : subtractMagnitudes(u, v);
            long[] numerator = multiplyMod(cubeMagnitude(diff), addMagnitudes(v, multiplySmall(u, 3L)), n);
            if (negative && !isZeroArray(numerator))
                numerator = subtractMagnitudes(n, numerator);
            long[] denominator = multiplyMod(cubeMagnitude(u), shiftLeftBits(v, 2), n);
            GcdInverseMag eg = gcdInverseMagnitudes(denominator, n);
            if (isNontrivialFactor(eg.gcd, n)) {
                stats.curveCreateNanos = TMX.getCurrentThreadCpuTime() - t0;
                stats.found(eg.gcd, "Curve construction denominator GCD", curveCount + 1,
                        -1, -1);
                throw new FactorFoundException(eg.gcd);
            }
            if (equalsArray(eg.gcd, n)) continue;
            long[] A = multiplyMod(eg.inverse, numerator, n);
            long[] A24 = multiplyMod(addSmall(A, 2L), context.inv4, n);
            MontgomeryCurve curve = new MontgomeryCurve(context.reducer, A24);

            long[] X = context.reducer.cube(context.reducer.toMontgomery(u));
            long[] Z = context.reducer.cube(context.reducer.toMontgomery(v));
            MontgomeryPoint point = new MontgomeryPoint(X, Z);
            works[curveCount] = new ECMCurveWork(curve, point);
            curveCount++;
        }
        stats.curveCreateNanos = TMX.getCurrentThreadCpuTime() - t0;
    }
    private static void ecmStage1(FactorJob job, ECMCurveWork[] works, ECMPlan plan, ECMStats stats){
        long t0;
        long t1 = TMX.getCurrentThreadCpuTime();
        stage2 = false;
        long lower = 2L;
        long upperExclusive = plan.B1 + 1L;
        while (lower < upperExclusive) {
            long upper = upperExclusive - lower <= 1048576 ? upperExclusive : lower + 1048576;
            t0 = TMX.getCurrentThreadCpuTime();
            long[] primes = sievePrimeBlock(lower, upper, SMALL_PRIMES);
            stats.stage1SieveNanos += TMX.getCurrentThreadCpuTime() - t0;
            t0 = TMX.getCurrentThreadCpuTime();
            ByteArrayOutputStream operations = new ByteArrayOutputStream();

            int[] offsets = new int[primes.length + 1];
            byte[] trailingDoubling = new byte[primes.length];

            int chainCount = 0;
            int powerOfTwoDoubling = 0;

            for (long prime : primes) {
                if (prime > plan.B1) continue;
                long scalar = prime;
                while (scalar <= plan.B1 / prime) scalar *= prime;
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
                long d = oddPart - r;
                long e = r - d;
                while (d != e) {
                    if (d < e) {
                        long temporary = d;
                        d = e;
                        e = temporary;
                        operations.write(PracChain.SWAP);
                    }
                    // Apply the first qualifying PRAC condition.
                    if (d - e <= e / 4L && (d + e) % 3L == 0L) {
                        operations.write(PracChain.CASE_1);
                        d = (2L * d - e) / 3L;
                        e = (e - d) / 2L;
                    } else if (d - e <= e / 4L && (d - e) % 6L == 0L) {
                        operations.write(PracChain.CASE_2);
                        d = (d - e) / 2L;
                    } else if ((d + 3L) / 4L <= e) {
                        operations.write(PracChain.CASE_3);
                        d -= e;
                    } else if ((d + e) % 2L == 0L) {
                        operations.write(PracChain.CASE_4);
                        d = (d - e) / 2L;
                    } else if ((d & 1L) == 0L) {
                        operations.write(PracChain.CASE_5);
                        d /= 2L;
                    } else if (d % 3L == 0L) {
                        operations.write(PracChain.CASE_6);
                        d = d / 3L - e;
                    } else if ((d + e) % 3L == 0L) {
                        operations.write(PracChain.CASE_7);
                        d = (d - 2L * e) / 3L;
                    } else if ((d - e) % 3L == 0L) {
                        operations.write(PracChain.CASE_8);
                        d = (d - e) / 3L;
                    } else {
                        operations.write(PracChain.CASE_9);
                        e /= 2L;
                    }
                    if (d <= 0L || e <= 0L)
                        throw new IllegalStateException("Invalid PRAC reduction");
                }
                if (d != 1L)
                    throw new IllegalStateException("PRAC pair was not coprime");
                trailingDoubling[chainCount] = (byte) shifts;
                chainCount++;
            }
            offsets[chainCount] = operations.size();
            offsets = Arrays.copyOf(offsets, chainCount + 1);
            trailingDoubling = Arrays.copyOf(trailingDoubling, chainCount);
            byte[] ops = operations.toByteArray();
            stats.stage1ChainBuildNanos += TMX.getCurrentThreadCpuTime() - t0;
            //apply the Prac block
            applyECMStage1Chains(works, powerOfTwoDoubling, ops, offsets, trailingDoubling, stats);
            lower = upper;
        }
        t0 = TMX.getCurrentThreadCpuTime();
        for (int k = 0; k < works.length; k++) {
            ECMCurveWork state = works[k];
            long[] g = gcdMagnitudes(state.point.Z, job.n);
            if (isNontrivialFactor(g, job.n)) {
                stats.stage1Nanos = TMX.getCurrentThreadCpuTime() - t1;
                stats.found(g, "Stage 1 final GCD", job.nextECMCurve + k + 1, -1, -1);
                throw new FactorFoundException(g);
            }
            if (equalsArray(g, job.n)) state.usable = false;
            else stats.usableCurves++;
        }
        stats.gcdStage1Nanos += TMX.getCurrentThreadCpuTime() - t0;
        stats.stage1Nanos += TMX.getCurrentThreadCpuTime() - t1;
    }
    private static void applyECMStage1Chains(ECMCurveWork[] works, int powerOfTwoDoubling, byte[] ops,
            int[] offsets, byte[] trailingDoubling, ECMStats stats) {
        long t0 = TMX.getCurrentThreadCpuTime();
        for (ECMCurveWork state : works) {
            for (int i = 0; i < powerOfTwoDoubling; i++)
                state.point = state.curve.doublePoint(state.point, stats);
        }
        for (int chain = 0; chain < trailingDoubling.length; chain++) {
            int from = offsets[chain];
            int to = offsets[chain + 1];
            int doubling = trailingDoubling[chain];
            for (ECMCurveWork state : works)
                state.point = state.curve.multiplyPrac(
                        state.point, ops, from, to, doubling, stats);
        }
        stats.stage1ApplyNanos = TMX.getCurrentThreadCpuTime() - t0;
    }
    private static void ecmStage2(FactorJob job, MontgomeryReducer reducer,
                                  ECMPlan plan, ECMCurveWork[] works, ECMStats stats){
        long t2 = TMX.getCurrentThreadCpuTime();
        stage2 = true;
        long lower = plan.B1 + 1L;
        long upperExclusive = plan.B2 + 1L;
        Stage2State[] stage2States = new Stage2State[works.length];
        long blockLower = lower;
        long[] left = new long[reducer.k];
        long[] right = new long[reducer.k];
        long[] out = new long[reducer.k];
        while (blockLower < upperExclusive) {
            long blockUpper = upperExclusive - blockLower <= 1_048_576L
                    ? upperExclusive : blockLower + 1_048_576L;
            long[] primes = sievePrimeBlock(blockLower, blockUpper, SMALL_PRIMES);
            if (primes.length == 0) {
                blockLower = blockUpper;
                continue;
            }
            for (int k = 0; k < works.length; k++) {
                ECMCurveWork work = works[k];
                if (!work.usable) continue;
                /*
                 * Each curve owns its own Stage2State. The state persists
                 * across all prime blocks.
                 */
                if (stage2States[k] == null)
                    stage2States[k] = new Stage2State(work.curve, work.point, primes[0],
                            stats);
                Stage2State s2 = stage2States[k];
                stats.stage2BlockCount++;
                s2.blockStartPrevious = s2.previous;
                s2.blockStartCurrent = s2.current;
                s2.blockStartM = s2.currentM;
                long blockStart = TMX.getCurrentThreadCpuTime();
                processStage2Terms(s2, primes, stats, left, right, out);
                stats.blockProcessTime += TMX.getCurrentThreadCpuTime() - blockStart;
                if (s2.blockTermCount == 0) {
                    s2.clearStage2Block();
                    continue;
                }
                long t0 = TMX.getCurrentThreadCpuTime();
                /*
                 * Do not overwrite blockProduct with the normal residue.
                 */
                long[] g = gcdMagnitudes(s2.curve.reducer.reduce(s2.blockProduct),
                        s2.curve.n);
                stats.stage2GcdTime += TMX.getCurrentThreadCpuTime() - t0;
                if (isNontrivialFactor(g, s2.curve.n)) {
                    s2.clearStage2Block();
                    stats.stage2Nanos = TMX.getCurrentThreadCpuTime() - t2;
                    stats.found(g, "Stage 2 GCD", job.nextECMCurve + k + 1,
                            blockLower, blockUpper);
                    throw new FactorFoundException(g);
                }
                if (equalsArray(g, s2.curve.n)) {
                        /*
                          Restore the giant-step state to the beginning of this
                          prime block and find the individual factor-producing
                          term.
                         */
                    s2.previous = s2.blockStartPrevious;
                    s2.current = s2.blockStartCurrent;
                    s2.currentM = s2.blockStartM;
                    s2.blockProduct = s2.curve.reducer.rModM;
                    s2.blockTermCount = 0;
                    Arrays.fill(left, 0);
                    Arrays.fill(right, 0);
                    Arrays.fill(out, 0);
                    for (long q : primes) {
                        long[] term = s2.stage2Term(q, stats, left, right, out);
                        if (term == null) continue;
                        t0 = TMX.getCurrentThreadCpuTime();
                        g = gcdMagnitudes(term, s2.curve.n);
                        stats.stage2GcdTime += TMX.getCurrentThreadCpuTime() - t0;
                        if (isNontrivialFactor(g, s2.curve.n)) {
                            stats.replayPrime = q;
                            s2.clearStage2Block();
                            stats.stage2Nanos = TMX.getCurrentThreadCpuTime() - t2;
                            stats.found(g, "Stage 2 individual-term replay",
                                    job.nextECMCurve + k + 1, blockLower, blockUpper);
                            throw new FactorFoundException(g);
                        }
                    }
                }
                    /*
                      Required for both gcd == 1 and gcd == n.
                      previous/current/currentM remain intact for the next block.
                     */
                s2.clearStage2Block();
            }
                /*
                  Advance the outer block only after every curve has processed it.
                 */
            blockLower = blockUpper;
        }
        stats.stage2Nanos = TMX.getCurrentThreadCpuTime() - t2;
    }
    private static void processStage2Terms(Stage2State s2, long[] primes, ECMStats stats,
                                           long[] left, long[] right,
            long[] out) {
        stats.stage2TermCount += primes.length;
        for (long prime : primes) {
            long[] term = s2.stage2Term(prime, stats, left, right, out);
            if (term == null) continue;
            s2.curve.reducer.multiplyInto(s2.blockProduct, term, s2.blockProduct);
            s2.blockTermCount++;
            stats.blockTermCount++;
            stats.stage2MontMultiplications += 3;
        }
    }

    /*
    This final section is dedicated to the QS implementation
     */
    private static final class QSFactorBase {
        final int size;
        // Permanent data
        final int[] primes;
        final int[] logPrimes;
        final int columnCount;
        final int globalPMax;
        final double logB;
        final long largePrimeBound;
        final double logLP;
        final double filterAllowance;
        final long[] largePrimeBoundSquared;
        final long[] pMaxSquared;
        final long[] reciprocals;
        final int aPrimeCount;
        final int[] inactive;
        final int[] root1;
        final int[] root2;

        // Reusable QS scratch
        final int[] aIndex;
        final short[] aCount;
        final long[][] twoTerms;
        final int[] invAModP;
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
            this.primes = Arrays.copyOf(primes, size);
            this.reciprocals = Arrays.copyOf(reciprocals, size);
            this.logPrimes = Arrays.copyOf(logPrimes, size);
            this.root1 = Arrays.copyOf(root1, size);
            this.root2 = Arrays.copyOf(root2, size);
            columnCount = size + 1; // negative column + real factor-base primes
            globalPMax = primes[size - 1];
            logB = Math.log(globalPMax);
            largePrimeBound = Math.multiplyExact(100L, globalPMax);
            logLP = Math.log(largePrimeBound);
            filterAllowance = (StrictMath.log(largePrimeBound)) * 27.0/32.0;
            long lpLow = largePrimeBound*largePrimeBound;
            long lpHigh = Math.unsignedMultiplyHigh(largePrimeBound, largePrimeBound);
            largePrimeBoundSquared = lpHigh > 0 ?
                    new long[]{lpLow, lpHigh} : new long[]{lpLow};
            pMaxSquared = new long[]{(long) globalPMax * globalPMax};
            int aFactorCount = 1;
            int bPolys = 1;
            while (bPolys < size/ bPolys) {
                aFactorCount++;
                bPolys <<= 1;
            }
            aPrimeCount = Math.clamp(aFactorCount, 4, 8);
            inactive = new int[aPrimeCount+1];
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
            if (hitIndices.length < largeHitCapacity){
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
                reciprocals[out] = reciprocal;
                out++;
                continue;
            }
            if (legendreSymbol(nModP, p, reciprocal) == 1) {
                long logRound = Math.round(StrictMath.log(p) * LOG_SCALE);
                // Fast path: p ≡ 3 mod 4
                if ((p & 3) == 3) {
                    int r = powMod(nModP, (p + 1) >>> 2, p, reciprocal);
                    primes[out] = p;
                    logPrimes[out] = (int) logRound;
                    root1[out] = r;
                    root2[out] = p - r;
                    reciprocals[out] = reciprocal;
                    out++;
                    continue;
                }
                if ((p & 7) == 5) {
                    int v = getV(p, nModP, reciprocal);
                    primes[out] = p;
                    logPrimes[out] = (int) logRound;
                    root1[out] = v;
                    root2[out] = p - v;
                    reciprocals[out] = reciprocal;
                    out++;
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
                int c = powMod(z, q, p, reciprocal);
                int r = powMod(nModP, (q + 1) >>> 1, p, reciprocal);
                int t = powMod(nModP, q, p, reciprocal);
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
                reciprocals[out] = reciprocal;
                out++;
            }
        }
        return new QSFactorBase(primes, logPrimes, root1, root2, reciprocals, out);
    }
    private static int getV(int p, int nModP, long reciprocal) {
        int v = powMod(nModP, (p + 3) >>> 3, p, reciprocal);
        long vv = (long) v * v;
        int check = (int) (vv - Math.multiplyHigh(vv, reciprocal) * p);
        if (check >= p) check -= p;
        if (check != nModP) {
            int sqrtMinusOne = powMod(2, (p - 1) >>> 2, p, reciprocal);
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
    private static int factorBaseIndexOf(int[] fbIndex, long L) {
        if (Long.compareUnsigned(L, fbIndex.length) >= 0)
            return -1;
        return fbIndex[(int) L];
    }
    private static int legendreSymbol(long a, int p, long reciprocal) {
        a %= p;
        if (a < 0) a += p;
        if (a == 0) return 0;
        long r = powMod(a, (p - 1) >>> 1, p, reciprocal);
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
            this.xValues[id] = xs;
            this.expIdx[id] = indexes;
            this.expCount[id] = counts;
            this.vertex1[id] = a;
            this.vertex2[id] = b;
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
            if (existing != null)
                return existing;

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
            while (ufParent[root] != root)
                root = ufParent[root];
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
        if(a >= p)
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
        if (oldR != 1)
            throw new ArithmeticException("No inverse mod " + p);
        return Math.floorMod(oldS, p);
    }
    private static int maxFactorBaseIndexAtMost(QSFactorBase factorBase, long[] maxNextPrimeMag) {
        if (isZeroArray(maxNextPrimeMag)) return -1;
        int last = factorBase.size - 1;
        if (maxNextPrimeMag.length > 1) return last;
        long max = maxNextPrimeMag[0];
        if (Long.compareUnsigned(max, factorBase.primes[last]) >= 0) return last;
        int maxIdx = Arrays.binarySearch(factorBase.primes, (int)max);
        if(maxIdx < 0) maxIdx = -maxIdx-2;
        return maxIdx;
    }
    private record ACoefficient(long[] A, int[] factors, long half) {}
    private record ACandidateResult(ACoefficient coefficient, String failure) {
        static ACandidateResult ok(ACoefficient c) {return new ACandidateResult(c, null);}
        static ACandidateResult fail(String reason) {return new ACandidateResult(null, reason);}
    }
    private static ACoefficient chooseAByScoring(long[] sqrt2N, QSFactorBase factorBase, int aFactorCount, SplittableRandom rng) {
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
        long targetHalf = 16L*factorBase.size/(1L << aFactorCount);
        targetHalf = Math.max(targetHalf, 1);
        long lower = Math.max(1L, targetHalf / 4L);
        long upper = Math.multiplyExact(targetHalf, 4L);
        double logTargetA = logMag(sqrt2N) - StrictMath.log(targetHalf);
        if (!Double.isFinite(logTargetA) || logTargetA <= 0.0)
            throw new IllegalStateException("Bad SIQS log target A");
        for (int trial = 0; trial < attempts; trial++) {
            ACandidateResult result = randomSquarefreeAResult(sqrt2N, factorBase, aFactorCount, logTargetA, rng);
            if (result.coefficient == null) {
                genFail++;
                if (firstGenFailure == null)
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
            double yield = estimatedRelationsPerPolynomial(candidate.A, half, factorBase.globalPMax);
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
            System.out.println("SIQS A warning: using fallback A because strict scoring rejected all candidates."
                    + " aFactorCount=" + aFactorCount + " attempts=" + attempts + " genFail=" + genFail +
                    " zeroHalf=" + zeroHalf + " halfOverInt" + halfOverInt + " halfTooLarge" + halfTooLarge
                    + "halfTooSmall" + halfTooSmall + " badYield=" + badYield + " accepted=" + accepted +
                    " fallbackHalf=" + Long.toUnsignedString(fallbackHalf));
            return fallback;
        }
        throw new IllegalStateException("Could not find usable SIQS A: "
                + "aFactorCount=" + aFactorCount + " attempts=" + attempts +
                " genFail=" + genFail + " zeroHalf=" + zeroHalf + " halfOverInt=" + halfOverInt
                + " halfTooLarge=" + halfTooLarge + "halfTooSmall=" + halfTooSmall + " badYield=" + badYield
                + " accepted=" + accepted + " firstGenFailure=[" + firstGenFailure + "]");
    }
    private static ACandidateResult randomSquarefreeAResult(long[] sqrt2N, QSFactorBase factorBase,
            int aFactorCount, double logTargetA, SplittableRandom rng) {
        int[] chosen = new int[aFactorCount];
        int chosenCount = 0;
        long[] A = new long[aFactorCount+1];
        A[0] = 1L;
        // floor(sqrt2N / A), initially A = 1.
        long[] halfMag = sqrt2N;
        double logA = 0.0;
        while (chosenCount < aFactorCount) {
            int remaining = aFactorCount - chosenCount;
            double remainingLogBudget = logTargetA - logA;
            if (remainingLogBudget <= 0.0) return ACandidateResult.fail("A already above target");
            double desiredLogP = remainingLogBudget / remaining;
            // halfMag is already floor(sqrt2N / A).
            int maxIdx = maxFactorBaseIndexAtMost(factorBase, halfMag);
            if (maxIdx < 1) return ACandidateResult.fail("maxIdx < 1");
            int idx = chooseUnusedPrimeNearLogBounded(factorBase, chosen, chosenCount, desiredLogP, maxIdx, rng);
            if (idx < 0) return ACandidateResult.fail("no unused prime");
            int p = factorBase.primes[idx];
            multiplySmallInto(A, p, A);
            // Exact identity:
            // floor(sqrt2N / (oldA * p))
            // = floor(floor(sqrt2N / oldA) / p)
            halfMag = divideByLong(halfMag, p).quotient;
            boolean secondRoot = rng.nextBoolean();
            chosen[chosenCount++] = (idx << 1) | (secondRoot ? 1 : 0);
            logA += factorBase.logPrimes[idx] / LOG_SCALE;
        }
        if(halfMag.length > 1) return ACandidateResult.fail("half too large");
        return ACandidateResult.ok(new ACoefficient(trimLeadingZeros(A), chosen, halfMag[0]));
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
    private static double estimatedRelationsPerPolynomial(long[] A, long half, int pMax) {
        if (half == 0) return 0.0;
        double logA = logMag(A);
        double logM = StrictMath.log((double) half);
        double logQEdge = logA + 2.0 * logM;
        double u = logQEdge / StrictMath.log(pMax);
        double smoothProb = roughDickman(u);
        return 2.0 * half * smoothProb;
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

        long firstFilterHits;
        long secondFilterHits;

        long rootCalcTime;
        long smallLogSieveNs;
        long largeLogSieveNs;
        long sieveNs;
        long attemptNs;
        long polyNs;

        AStats(double logA, long half, QSFactorBase factorBase, RelationYieldEstimate estimate) {
            this.aFactorCount = factorBase.aPrimeCount;
            bCount = 1L << (aFactorCount - 1);
            this.half = half;
            interval = 2L * half;
            positionsPerA = Math.multiplyExact(interval, bCount);

            fbSize = factorBase.size;
            pMax = factorBase.globalPMax;
            this.largePrimeBound = factorBase.largePrimeBound;

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
        void observeFactorTests(long tested){
            smallDivisions++;
            factorTestsTotal += tested;
            factorTestsMin = Math.min(factorTestsMin, tested);
            factorTestsMax = Math.max(factorTestsMax, tested);
        }
        long mergedRows(){return singlePartialMerged+doublePartialMerged+mixedPartialMerged;}
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
        long collectionNs(){return precomputation+polyNs+sieveNs+attemptNs;}
        double empiricalSecondsRemaining(long requiredRelations, long currentRelations) {
            long remaining = Math.max(0L, requiredRelations - currentRelations);
            RelationYieldEstimate adaptive = adaptiveEstimate();
            if (adaptive.usefulRowsPerPoly <= 0.0) return Double.POSITIVE_INFINITY;
            double secPerPoly = measuredSecondsPerSieve();
            if (!Double.isFinite(secPerPoly)) return Double.POSITIVE_INFINITY;
            return remaining / adaptive.usefulRowsPerPoly() * secPerPoly;
        }
        void printAStats(int requiredRelations, int currentRelations) {
            long remainingRelations = Math.max(0L, requiredRelations - currentRelations);
            double elapsedSeconds = collectionNs()/ 1e9;
            double measuredSecondsPerPoly = measuredSecondsPerSieve();
            double measuredRowsPerPoly = measuredUsefulRowsPerPoly();
            double measuredRowsPerSecond = elapsedSeconds <= 0.0 ? 0.0 : usefulRows() / elapsedSeconds;
            double estimatedRowsPerPoly = estimate.usefulRowsPerPoly();
            double avgTests = factorTestsTotal / (double) smallDivisions;
            double fbFraction = avgTests / fbSize;
            /*
             * Prefer measured polynomial time once available. Before the first
             * polynomial, the total-time estimate cannot be formed from AStats.
             */
            double estimatedPolysRemaining = estimatedRowsPerPoly > 0.0 ? remainingRelations / estimatedRowsPerPoly : Double.POSITIVE_INFINITY;
            double estimatedCollectionSeconds = empiricalSecondsRemaining(requiredRelations, currentRelations);
            System.out.println("  A-factor count: " + aFactorCount);
            System.out.println("  B-polynomials per A: " + bCount);
            System.out.println("  Sieve interval: " + interval);
            System.out.println("  Positions per A: " + positionsPerA);
            System.out.println("  log(A): " + logA);
            System.out.println("  log(|Q| at edge): " + logQEdge);
            System.out.println("  u(B): " + uB);
            System.out.println("  u(LP): " + uLP);
            System.out.println("Estimated yield per polynomial");
            System.out.println("  Full: " + estimate.fullPerPoly());
            System.out.println("  Single-prime partial hits: " + estimate.singlePartialHitsPerPoly());
            System.out.println("  Single-prime merged rows: " + estimate.singlePartialMergesPerPoly());
            System.out.println("  Double-prime partial hits: " + estimate.doublePartialHitsPerPoly());
            System.out.println("  Double-prime merged rows: " + estimate.doublePartialMergesPerPoly());
            System.out.println("  Useful rows: " + estimatedRowsPerPoly);
            System.out.println("Measured totals");
            System.out.println("  Polynomials: " + polys);
            System.out.println("  Positions scanned: " + scanned);
            System.out.println("  Candidates attempted: " + candidates);
            System.out.println("  Candidates passed filter 1: " + firstFilterHits);
            System.out.println("  Candidates passed filter 2: " + secondFilterHits);
            System.out.println("  Full relations: " + full);
            System.out.println("  Single-prime partials stored: " + singlePartialStored);
            System.out.println("  Double-prime partials stored: " + doublePartialStored);
            System.out.println("  Single-prime merges: " + singlePartialMerged);
            System.out.println("  Double-prime merges: " + doublePartialMerged);
            System.out.println("  Mixed partial merges: " + mixedPartialMerged);
            System.out.println("  Rejected candidates: " + none);
            System.out.println("  Useful rows: " + usefulRows());
            System.out.println("Measured rates");
            System.out.println("  Candidates/polynomial: " + candidates*1.0/polys);
            System.out.println("  Average trial divisions per candidate: " + avgTests);
            System.out.println("  Fraction of factor base length: " + fbFraction);
            System.out.println("  Full relations/polynomial: " + measuredFullPerPoly());
            System.out.println("  Single partials/polynomial: " + measuredSinglePartialPerPoly());
            System.out.println("  Double partials/polynomial: " + measuredDoublePartialPerPoly());
            System.out.println("  Merged rows/polynomial: " + measuredPartialMergesPerPoly());
            System.out.println("  Useful rows/polynomial: " + measuredRowsPerPoly);
            System.out.println("  Useful rows/second: " + measuredRowsPerSecond);
            System.out.println("Timing");
            System.out.println("  Polynomial generation: " + formatSeconds(polyNs));
            System.out.println("  Small log sieve time: " + formatSeconds(smallLogSieveNs));
            System.out.println("  Large log sieve time: " + formatSeconds(largeLogSieveNs));
            System.out.println("  Root calculation time: " + formatSeconds(rootCalcTime));
            System.out.println("  Total sieve time: " + formatSeconds(sieveNs));
            System.out.println("  Candidate/relation processing: " + formatSeconds(attemptNs));
            System.out.println("  Total relation collection: " + formatSeconds(collectionNs()));
            System.out.println("  Seconds/polynomial usage: " + measuredSecondsPerPoly);
            System.out.println("Relation collection projection");
            System.out.println("  Current relations: " + currentRelations + " / " + requiredRelations);
            System.out.println("  Remaining relations: " + remainingRelations);
            System.out.println("  Estimated polynomials remaining: " + estimatedPolysRemaining);
            System.out.println("  Estimated collection time remaining: " + estimatedCollectionSeconds + " seconds");
            System.out.println();
        }
    }
    private static String formatSeconds(long nanoSeconds) {return nanoSeconds/1e9 + " seconds";}
    /*
    These two values are guesses; do not treat them as Holy Grails of truth
     */
    private static final double fullCalibration = 0.06;
    private static final double partialCalibration = 0.02;
    private static void sieve128Polynomials(long[] N, long[] sqrt2N, QSFactorBase factorBase, int[] fbIndex,
                                            QSFullRelationPool relations, QSPartialRelationPool partials,
                                            PartialGraph partialGraph, SplittableRandom rng) {
        long precomputation = TMX.getCurrentThreadCpuTime();
        ACoefficient AR = chooseAByScoring(sqrt2N, factorBase, factorBase.aPrimeCount, rng);
        long[] A = AR.A;
        Arrays.sort(AR.factors);
        int half = (int)AR.half;
        int startT = -half;
        int interval = 2 * half;
        double logA = logMag(A);
        double logQ = logA + 2.0 * Math.log(half);
        double fullProbability = roughDickman(logQ / factorBase.logB);
        double rawAcceptedPartialPool = interval * Math.max(0.0, roughDickman(logQ / factorBase.logLP) - fullProbability);
        double fullHits = fullCalibration * interval * fullProbability;
        double calibratedPartialPool = partialCalibration * rawAcceptedPartialPool;
        /*
         * Wider LP ranges make accepted two-prime residuals more likely.
         * Scale the span against the typical logarithmic Q magnitude.
         */
        double normalizedSpan = (factorBase.logLP - factorBase.logB)/ Math.max(logQ - factorBase.logB, 1e-12);
        normalizedSpan = Math.clamp(1.0 - normalizedSpan, 0.10, 0.90);
        double singlePartialHits = calibratedPartialPool * normalizedSpan;
        double doublePartialHits = calibratedPartialPool * (1.0 - normalizedSpan);
        AStats plan = new AStats(logA, half, factorBase, RelationYieldEstimate.of(fullHits, singlePartialHits,
                0.0, doublePartialHits,0.0));
        // Precompute values depending only on A.
        int largeStart = Arrays.binarySearch(factorBase.primes, interval);
        if (largeStart < 0) largeStart = -largeStart - 1;
        factorBase.ensureSieveCapacity(interval, 2 * (factorBase.size - largeStart));
        BResult BR = computeB(AR, factorBase, largeStart);
        long[] B = BR.B;
        long rootCalcTime = TMX.getCurrentThreadCpuTime();
        computeInitialRoots(factorBase, B, startT);
        plan.rootCalcTime = TMX.getCurrentThreadCpuTime() - rootCalcTime;
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
                    deltaRow[j] = (int) (z - Math.multiplyHigh(z, reciprocal) * p);
                }
                lo = hi + 1;
            }
            for (int j = lo; j < factorBase.size; j++) {
                int p = factorBase.primes[j];
                long reciprocal = factorBase.reciprocals[j];
                long z = (long) modSmallInt(step, stepLength, p, reciprocal) * factorBase.invAModP[j];
                deltaRow[j] = (int) (z - Math.multiplyHigh(z, reciprocal) * p);
            }
        }
        plan.precomputation = TMX.getCurrentThreadCpuTime() - precomputation;
        long polynomialTime = TMX.getCurrentThreadCpuTime();
        // Construct initial B for polynomial 0.
        // Initial values, computed exactly once.
        long[] twoB = new long[A.length+1];
        shiftLeftBitsInto(B, 1, twoB);
        long[] b2 = squareMagnitude(B);
        long[] deltaB;
        long[] cMag;
        boolean cNegative;
        if (compareArray(b2, N) >= 0) {
            subtractMagnitudeInPlace(b2, N);
            cMag = divideMagnitudesExact(b2, A);
            cNegative = false;
        } else {
            b2 = subtractMagnitudes(N, b2);
            cMag = divideMagnitudesExact(b2, A);
            cNegative = true;
        }
        cMag = Arrays.copyOf(cMag, cMag.length+1);
        plan.polyNs += TMX.getCurrentThreadCpuTime() - polynomialTime;
        //log sieve
        long logSieve = TMX.getCurrentThreadCpuTime();
        Arrays.fill(factorBase.score, 0, interval, 0);
        long smallSieve = TMX.getCurrentThreadCpuTime();
        // ---------------- SMALL SIEVE ---------------- //
        markSmallRoots(largeStart, factorBase, interval);
        plan.smallLogSieveNs += TMX.getCurrentThreadCpuTime() - smallSieve;
        // ---------------- LARGE SIEVE ---------------- //
        long largeSieve = TMX.getCurrentThreadCpuTime();
        int hitCount = markLargeRoots(BR, largeStart, factorBase, interval);
        plan.largeLogSieveNs += TMX.getCurrentThreadCpuTime() - largeSieve;
        long totalSieveTime = TMX.getCurrentThreadCpuTime() - logSieve;
        //attempt after sieve
        int scoreThreshold = (int)Math.ceil((plan.logQEdge - factorBase.filterAllowance) * LOG_SCALE);
        plan.scanned += interval;
        long relationTime = TMX.getCurrentThreadCpuTime();
        int candidateCount = scanCandidates(interval, scoreThreshold, hitCount, factorBase, plan);
        plan.scanTime += TMX.getCurrentThreadCpuTime() - relationTime;
        collectQSRelations(N, A, B, BR, twoB, cMag, cNegative, candidateCount, startT, factorBase, fbIndex,
                largeStart, relations, partials, partialGraph, plan);
        plan.attemptNs += TMX.getCurrentThreadCpuTime() - relationTime;
        plan.polys++;
        plan.sieveNs += totalSieveTime;
        for (int mask = 1; mask < 1 << (factorBase.aPrimeCount-1); mask++) {
            long[] oldB = B.clone();
            int bit = Integer.numberOfTrailingZeros(mask);
            long[] step = factorBase.twoTerms[bit + 1];
            boolean wrapped;
            if (((mask >>> (bit+1)) & 1) == 0) {
                // B -= step (mod A)
                if (compareArray(B, step) < 0) {
                    B = subtractMagnitudes(A, subtractMagnitudes(step, B));
                    wrapped = true;
                } else {
                    subtractMagnitudeInPlace(B, step);
                    wrapped = false;
                }
                long cpuRoot = TMX.getCurrentThreadCpuTime();
                if (wrapped) updateRootsAddWrapped(factorBase, factorBase.grayDelta[bit]);
                else updateRootsAdd(factorBase, factorBase.grayDelta[bit]);
                rootCalcTime = TMX.getCurrentThreadCpuTime() - cpuRoot;
            } else {
                // B += step (mod A)
                B = addMagnitudes(B, step);
                if (compareArray(B, A) >= 0) {
                    subtractMagnitudeInPlace(B, A);
                    wrapped = true;
                } else wrapped = false;
                long cpuRoot = TMX.getCurrentThreadCpuTime();
                if (wrapped) updateRootsSubtractWrapped(factorBase, factorBase.grayDelta[bit]);
                else updateRootsSubtract(factorBase, factorBase.grayDelta[bit]);
                rootCalcTime = TMX.getCurrentThreadCpuTime() - cpuRoot;
            }
            plan.rootCalcTime += rootCalcTime;
            // Incremental C update.
            polynomialTime = TMX.getCurrentThreadCpuTime();
            boolean deltaNegative;
            int cmp = compareArray(B, oldB);
            if (cmp >= 0) {
                deltaB = subtractMagnitudes(B, oldB);
                deltaNegative = false;
            } else {
                deltaB = subtractMagnitudes(oldB, B);
                deltaNegative = true;
            }
            if (!isZeroArray(deltaB)) {
                long[] sumB = addMagnitudes(B, oldB);
                long[] deltaC = multiplyMagnitudes(deltaB, sumB);
                deltaC = divideMagnitudesExact(deltaC, A);
                if (cNegative == deltaNegative)
                    addMagnitudeInPlace(cMag, deltaC, 0);
                else {
                    int cCmp = compareArray(cMag, deltaC);
                    if (cCmp > 0) subtractMagnitudeInPlace(cMag, deltaC);
                    else if (cCmp < 0) {
                        cMag = subtractMagnitudes(deltaC, cMag);
                        cNegative = deltaNegative;
                    } else {
                        cMag = ZERO_MAG;
                        cNegative = false;
                    }
                }
            }
            shiftLeftBitsInto(B, 1, twoB);
            plan.polyNs += TMX.getCurrentThreadCpuTime() - polynomialTime;
            //log sieve
            logSieve = TMX.getCurrentThreadCpuTime();
            Arrays.fill(factorBase.score, 0, interval, 0);
            smallSieve = TMX.getCurrentThreadCpuTime();
            // ---------------- SMALL SIEVE ---------------- //
            markSmallRoots(largeStart, factorBase, interval);
            plan.smallLogSieveNs += TMX.getCurrentThreadCpuTime() - smallSieve;
            // ---------------- LARGE SIEVE ---------------- //
            largeSieve = TMX.getCurrentThreadCpuTime();
            hitCount = markLargeRoots(BR, largeStart, factorBase, interval);
            plan.largeLogSieveNs += TMX.getCurrentThreadCpuTime() - largeSieve;
            totalSieveTime = TMX.getCurrentThreadCpuTime() - logSieve;
            //attempt after sieve
            plan.scanned += interval;
            relationTime = TMX.getCurrentThreadCpuTime();
            candidateCount = scanCandidates(interval, scoreThreshold, hitCount, factorBase, plan);
            plan.scanTime += TMX.getCurrentThreadCpuTime() - relationTime;
            collectQSRelations(N, A, B, BR, twoB, cMag, cNegative, candidateCount, startT, factorBase, fbIndex,
                    largeStart, relations, partials, partialGraph, plan);
            plan.attemptNs += TMX.getCurrentThreadCpuTime() - relationTime;
            plan.polys++;
            plan.sieveNs += totalSieveTime;
        }
        System.out.printf(
                "%50s %10d %12d %12d %10d %8d "
                        + "%20.5f %14.5f %11.5f %11.5f %16.5f %16.5f %16.5f %16.5f %14.5f %13.5f %14.5f " +
                        "%14.5f %12.5f %12.5f %15d %n",
                new LargeInteger(A, false),
                interval,
                plan.candidates,
                plan.full,
                plan.singlePartialStored,
                plan.singlePartialMerged + plan.doublePartialMerged + plan.mixedPartialMerged,

                plan.precomputation/1e6,
                plan.rootCalcTime / 1e6,
                plan.smallLogSieveNs / 1e6,
                plan.largeLogSieveNs / 1e6,
                plan.scanTime / 1e6,
                plan.candidateScanNs / 1e6,
                plan.hitLinkNs / 1e6,
                plan.smallDivideTime / 1e6,
                plan.relationClassifyNs / 1e6,
                plan.attemptNs / 1e6,
                plan.collectionNs() / 1e6,

                plan.usefulRows() * 1e9 / plan.collectionNs(),
                plan.factorTestsTotal / (double) plan.smallDivisions,
                plan.usefulRows() / 128.0,
                relations.size
        );
        if(PRINT_VERBOSE)
            plan.printAStats(factorBase.size+30, relations.size);
    }
    private static final boolean PRINT_VERBOSE = false;
    private static double weightedAverage(double first, double firstWeight, double second, double secondWeight) {
        double totalWeight = firstWeight + secondWeight;
        if (totalWeight == 0.0) return 0.0;
        return (first * firstWeight + second * secondWeight) / totalWeight;
    }
    private record BResult(long[] B, int[] inactiveLarge, int inactiveLargeCount) {}
    private static BResult computeB(ACoefficient AR, QSFactorBase factorBase, int largeStart) {
        long[] A = AR.A;
        long[] B = new long[A.length + 1];
        int inactiveLargeCount = 0;
        int[] inactiveLarge = new int[AR.factors.length+1];
        int lo = 1;
        for (int i = 0; i < AR.factors.length; i++) {
            int packed = AR.factors[i];
            int idx = packed >>> 1;
            // Compute A^-1 mod p for the active range before this A-factor.
            for (int j = lo; j < idx; j++) {
                int p = factorBase.primes[j];
                factorBase.invAModP[j] =
                        inverseModSmall(modSmallInt(A, A.length, p, factorBase.reciprocals[j]), p);
            }
            // idx is inactive because primes[idx] divides A.
            factorBase.inactive[i + 1] = idx;
            factorBase.aIndex[i] = idx + 1;
            factorBase.aCount[i] = 1;
            if (idx >= largeStart) inactiveLarge[inactiveLargeCount++] = idx;
            // Construct the corresponding B term.
            int p = factorBase.primes[idx];
            int root = (packed & 1) == 0 ? factorBase.root1[idx] : factorBase.root2[idx];
            long[] Ai = A.clone();
            divideMagnitudeExactInPlace(Ai, p);
            long coefficient =
                    (long) inverseModSmall(modSmallInt(Ai, A.length, p, factorBase.reciprocals[idx]), p)
                            * root % p;
            multiplySmallInto(Ai, coefficient, Ai);
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
            factorBase.invAModP[j] = inverseModSmall(modSmallInt(A, A.length, p, factorBase.reciprocals[j]), p);
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
                if (d1 < 0) d1 += p;
                int d2 = factorBase.root2[j] - bMod;
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
            if (d1 < 0) d1 += p;
            int d2 = factorBase.root2[j] - bMod;
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
    private static void updateRootsAddWrapped(QSFactorBase fb, int[] deltaRow){
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
    private static void updateRootsSubtract(QSFactorBase fb, int[] deltaRow){
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
    private static void updateRootsSubtractWrapped(QSFactorBase factorBase, int[] deltaRow){
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
    private static void markSmallRoots(int largeStart, QSFactorBase factorBase, int interval){
        int lo = 0;
        for (int hi : factorBase.inactive) {
            if (hi >= largeStart) break;
            for (int j = lo; j < hi; j++) {
                int p = factorBase.primes[j];
                int x1 = factorBase.pos1[j];
                int x2 = factorBase.pos2[j];
                if (x1 >= interval && x2 >= interval) continue;
                int logP = factorBase.logPrimes[j];
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
            int p = factorBase.primes[j];
            int x1 = factorBase.pos1[j];
            int x2 = factorBase.pos2[j];
            if (x1 >= interval && x2 >= interval) continue;
            int logP = factorBase.logPrimes[j];
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
    private static int markLargeRoots(BResult BR, int largeStart,
                                      QSFactorBase factorBase, int interval) {
        int hitCount = 0, lo = largeStart;
        for (int k = 0; k < BR.inactiveLargeCount; k++) {
            int hi = BR.inactiveLarge[k];
            for (int j = lo; j < hi; j++) {
                int x1 = factorBase.pos1[j], x2 = factorBase.pos2[j];
                if (x1 >= interval && x2 >= interval) continue;
                int logP = factorBase.logPrimes[j];
                if (x1 < interval) {
                    factorBase.score[x1] += logP;
                    factorBase.hitIndices[hitCount] = x1;
                    factorBase.hitPrimes[hitCount++] = j;
                }
                if (x2 < interval) {
                    factorBase.score[x2] += logP;
                    factorBase.hitIndices[hitCount] = x2;
                    factorBase.hitPrimes[hitCount++] = j;
                }
            }
            lo = hi + 1;
        }
        for (int j = lo; j < factorBase.size; j++) {
            int x1 = factorBase.pos1[j], x2 = factorBase.pos2[j];
            if (x1 >= interval && x2 >= interval) continue;
            int logP = factorBase.logPrimes[j];
            if (x1 < interval) {
                factorBase.score[x1] += logP;
                factorBase.hitIndices[hitCount] = x1;
                factorBase.hitPrimes[hitCount++] = j;
            }
            if (x2 < interval) {
                factorBase.score[x2] += logP;
                factorBase.hitIndices[hitCount] = x2;
                factorBase.hitPrimes[hitCount++] = j;
            }
        }
        return hitCount;
    }
    private static int scanCandidates(int interval, int scoreThreshold, int hitCount, QSFactorBase factorBase,
                                      AStats plan){
        long candidateScanStart = TMX.getCurrentThreadCpuTime();
        int candidateCount = 0;
        for (int idx = 0; idx < interval; idx++) {
            if (factorBase.score[idx] >= scoreThreshold) {
                factorBase.candidateIndices[candidateCount++] = idx;
                factorBase.hitHead[idx] = -1;
            }
        }
        plan.candidateScanNs += TMX.getCurrentThreadCpuTime() - candidateScanStart;
        long hitLinkStart = TMX.getCurrentThreadCpuTime();
        int keptHitCount = 0;
        for (int h = 0; h < hitCount; h++) {
            int index = factorBase.hitIndices[h];
            if (factorBase.score[index] < scoreThreshold) continue;
            int j = factorBase.hitPrimes[h];
            factorBase.hitIndices[keptHitCount] = factorBase.hitHead[index];
            factorBase.hitPrimes[keptHitCount] = j;
            factorBase.hitHead[index] = keptHitCount++;
        }
        plan.hitLinkNs += TMX.getCurrentThreadCpuTime() - hitLinkStart;
        return candidateCount;
    }
    private static void clearUsed(short[] dense, int[] used, int usedCount) {
        for (int i = 0; i < usedCount; i++)
            dense[used[i]] = 0;
    }
    private static void performSmallDivisions(long[] remaining, int largeStart, int[] primes, long[] reciprocals,
            short[] dense, int[] used, int usedCount, int[] result) {
        int tested = 0;
        int remLength = remaining.length;
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
            if (remLength == 1) {
                long part = remaining[0];
                long pp = (long) p * p;
                if (Long.compareUnsigned(part, pp) <= 0) break;
            }
        }
        result[0] = tested;
        result[1] = usedCount;
        result[2] = remLength;
    }
    private static void performInactiveLargeDivisions(long[] remaining, int remLength, int[] inactiveLarge,
            int inactiveLargeCount, int[] primes, long[] reciprocals, short[] dense, int[] used, int usedCount,
                                                      int[] result) {
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
                while (remLength > 1 && remaining[remLength - 1] == 0)
                    remLength--;
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
            int[] hitIndices, int[] hitPrimes, int[] primes, long[] reciprocals, short[] dense, int[] used, int usedCount,
            int[] result) {
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
    private static void collectQSRelations(long[] N, long[] A, long[] B, BResult BR, long[] twoB, long[] cMag,
                                           boolean cNegative, int candidateCount, long startT,
                                           QSFactorBase factorBase, int[] fbIndex, int largeStart,
                                           QSFullRelationPool relations, QSPartialRelationPool partials,
                                           PartialGraph partialGraph, AStats plan) {
        for (int k = 0; k < candidateCount; k++) {
            int idx = factorBase.candidateIndices[k];
            long t = startT + idx;
            boolean tNegative = t < 0;
            long absT = tNegative ? -t : t;
            long[] atMag = multiplySmall(A, absT);
            // q = At + 2B
            long[] qMag = new long[Math.max(Math.max(atMag.length, twoB.length) + 3, cMag.length + 1)];
            System.arraycopy(atMag, 0, qMag, 0, atMag.length);
            boolean qNegative;
            if (!tNegative) {
                addMagnitudeInPlace(qMag, twoB, 0);
                qNegative = false;
            } else {
                if (compareArray(twoB, atMag) >= 0) {
                    // qMag = twoB - atMag, but retain scratch capacity
                    System.arraycopy(twoB, 0, qMag, 0, twoB.length);
                    Arrays.fill(qMag, twoB.length, qMag.length, 0L);
                    subtractMagnitudeInPlace(qMag, atMag);
                    qNegative = false;
                } else {
                    subtractMagnitudeInPlace(qMag, twoB);
                    qNegative = true;
                }
            }
            multiplySmallInto(qMag, absT, qMag);
            if (tNegative && !isZeroArray(qMag)) qNegative = !qNegative;
            if (qNegative == cNegative) addMagnitudeInPlace(qMag, cMag, 0);
            else {
                int cmp = compareArray(qMag, cMag);
                if (cmp >= 0) subtractMagnitudeInPlace(qMag, cMag);
                else {
                    qMag = subtractMagnitudes(cMag, qMag);
                    qNegative = cNegative;
                }
                if (isZeroArray(qMag)) qNegative = false;
            }
            if (factorBase.score[idx] / LOG_SCALE + factorBase.filterAllowance < logMag(qMag)) continue;
            plan.secondFilterHits++;
            long[] currentX;
            if (!tNegative) currentX = modAdd(B, atMag, N);
            else currentX = modSubtract(B, atMag, N);
            int usedCount = 0;
            // A contribution: column 0 is negative, factorBase[j] lives at column j + 1.
            if (qNegative){
                factorBase.used[usedCount++] = 0;
                incExp(factorBase.dense, 0);
            }
            if (isZeroArray(qMag)) {
                clearUsed(factorBase.dense, factorBase.used, usedCount);
                plan.observeRelationAdd(RelationAddKind.NONE);
                continue;
            }
            if (isOneArray(qMag)) {
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
            long smallDivideTime = TMX.getCurrentThreadCpuTime();
            performSmallDivisions(qMag, largeStart, factorBase.primes, factorBase.reciprocals,
                    factorBase.dense, factorBase.used, usedCount, factorBase.smallDivResult);
            int tested = factorBase.smallDivResult[0];
            usedCount = factorBase.smallDivResult[1];
            int remLength = factorBase.smallDivResult[2];
            plan.smallDivideTime += TMX.getCurrentThreadCpuTime() - smallDivideTime;
            performInactiveLargeDivisions(qMag, remLength, BR.inactiveLarge, BR.inactiveLargeCount, factorBase.primes,
                    factorBase.reciprocals, factorBase.dense, factorBase.used, usedCount,
                    factorBase.smallDivResult);
            tested += factorBase.smallDivResult[0];
            usedCount = factorBase.smallDivResult[1];
            remLength = factorBase.smallDivResult[2];
            performHitLargeDivisions(qMag, remLength, idx, factorBase.hitHead, factorBase.hitIndices,
                    factorBase.hitPrimes, factorBase.primes, factorBase.reciprocals, factorBase.dense,
                    factorBase.used, usedCount, factorBase.smallDivResult);
            tested += factorBase.smallDivResult[0];
            usedCount = factorBase.smallDivResult[1];
            remLength = factorBase.smallDivResult[2];
            plan.observeFactorTests(tested);
            long[][] xValues = {currentX};
            long classifyTime = TMX.getCurrentThreadCpuTime();
            /*
             * If the residual is itself a factor-base prime,
             * incorporate it before compressing the exponent vector.
             */
            qMag = trimLeadingZeros(qMag);
            if (remLength == 1) {
                int index = factorBaseIndexOf(fbIndex, qMag[0]);
                if (index >= 0) {
                    if(factorBase.dense[index+1] == 0) factorBase.used[usedCount++] = index+1;
                    incExp(factorBase.dense, index + 1);
                    qMag = ONE_MAG;
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
            plan.observeRelationAdd(classifyRelation(qMag, index, count, xValues, relations, partials, partialGraph,
                    factorBase));
            plan.relationClassifyNs += TMX.getCurrentThreadCpuTime() - classifyTime;
        }
    }
    private static RelationAddKind classifyRelation(long[] remaining, int[] index, short[] count, long[][] xValues,
                                                    QSFullRelationPool relations, QSPartialRelationPool partials,
                                                    PartialGraph partialGraph, QSFactorBase factorBase){
        /*
         * Fully factor-base smooth.
         */
        if (isOneArray(remaining)){
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
             * globalPMax < residual <= largePrimeBound
             */
            if (withinLargePrimeRange(residual, factorBase.globalPMax,
                    factorBase.largePrimeBound)){
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
        /*
         * A valid double-large-prime residual cannot exceed L².
         * Do this test for multi-limb residuals as well, because L² may require
         * two limbs even when each individual large prime fits in one limb.
         */
        if (remaining.length > 2 || compareArray(remaining, factorBase.largePrimeBoundSquared) > 0
                || compareArray(remaining, factorBase.pMaxSquared) <= 0)
            return RelationAddKind.NONE;
        /*
         * Avoid trying to split a residual that is itself prime.
         * A prime residual larger than largePrimeBound cannot be accepted.
         */
        if (isProbablyPrimeMag(remaining))
            return RelationAddKind.NONE;
        long[] factor = pollardPMinusOneEasy(remaining);
        if (!isNontrivialFactor(factor, remaining)) {
            factor = brentRhos(remaining);
            if (!isNontrivialFactor(factor, remaining))
                return RelationAddKind.NONE;
        }
        long[] otherFactor = divideMagnitudesExact(remaining, factor);
        /*
         * The unified partial graph currently stores vertices as unsigned longs,
         * so both factors must be single-limb.
         */
        if (factor.length != 1 || otherFactor.length != 1)
            return RelationAddKind.NONE;
        long q1 = factor[0];
        long q2 = otherFactor[0];
        if (!withinLargePrimeRange(q1, factorBase.globalPMax, factorBase.largePrimeBound)
                || !withinLargePrimeRange(q2, factorBase.globalPMax, factorBase.largePrimeBound))
            return RelationAddKind.NONE;
        /*
         * The factor finder may return a composite divisor.
         */
        if (!isProbablyPrimeMag(factor) || !isProbablyPrimeMag(otherFactor))
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
            combinePartialCycle(partialGraph.path, pathLength, xValues, index, count, q1, q2, partials, relations);
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
            return new RelationYieldEstimate(fullHitsPerPoly, singlePartialHitsPerPoly, singlePartialMergesPerPoly,
                    doublePartialHitsPerPoly, doublePartialMergesPerPoly, usableRowsPerPoly);
        }
    }
    private static double unsignedLongToDouble(long x) {
        if (x >= 0)
            return (double) x;
        return (double) (x & Long.MAX_VALUE) + 0x1.0p63;
    }
    private static double logMag(long[] mag) {
        mag = trimLeadingZeros(mag);
        if (isZeroArray(mag))
            return Double.NEGATIVE_INFINITY;
        int n = mag.length;
        double hi = unsignedLongToDouble(mag[n - 1]);
        if (n > 1)
            hi += Math.scalb(unsignedLongToDouble(mag[n - 2]), -64);
        return Math.log(hi) + 64.0 * (n - 1) * Math.log(2.0);
    }
    private static void incExp(short[] dense, int column) {
        if (dense[column] == Short.MAX_VALUE)
            throw new ArithmeticException("Exponent overflow at column " + column);
        dense[column]++;
    }
    private static boolean withinLargePrimeRange(long value, long globalPMax, long largePrimeBound) {
        return Long.compareUnsigned(value, globalPMax) > 0 && Long.compareUnsigned(value, largePrimeBound) <= 0;
    }
    private enum RelationAddKind {
        NONE(0),
        FULL(1),
        SINGLE_PRIME_PARTIAL(2),
        DOUBLE_PRIME_PARTIAL(3),
        SINGLE_PARTIAL_CYCLE(4),
        DOUBLE_PARTIAL_CYCLE(5),
        MIXED_PARTIAL_CYCLE(6);
        final int code;
        RelationAddKind(int code) {this.code = code;}
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
        BigInteger n = new LargeInteger(N, false, true).toBigInteger();
        for (int r = 0; r < relations.size; r++) {
            BigInteger x = BigInteger.ONE;
            for (long[] xv : relations.xValues[r]) {
                BigInteger v = new LargeInteger(xv, false, true).toBigInteger();
                x = x.multiply(v).mod(n);
            }
            BigInteger rhs = BigInteger.ONE;
            for (int k = 0; k < relations.exponentIndex[r].length; k++) {
                int col = relations.exponentIndex[r][k];
                int e = relations.exponentCount[r][k];
                if (col == 0) {
                    if ((e & 1) != 0)
                        rhs = rhs.negate().mod(n);
                } else {
                    BigInteger p = BigInteger.valueOf(factorBase.primes[col - 1]);
                    rhs = rhs.multiply(p.modPow(BigInteger.valueOf(e), n)).mod(n);
                }
            }
            // Each stored squareFactor belongs directly on Y,
            // therefore its square belongs on RHS.
            for (long[] sf : relations.squareFactors[r]) {
                BigInteger s = new LargeInteger(sf, false, true).toBigInteger();
                rhs = rhs.multiply(s.multiply(s).mod(n)).mod(n);
            }
            BigInteger lhs = x.multiply(x).mod(n);
            if (!lhs.equals(rhs))
                throw new AssertionError("BAD QS RELATION " + r + "\n" +
                        "xValues=" + relations.xValues[r].length + "\n" +
                        "squareFactors=" + relations.squareFactors[r].length + "\n" +
                        "lhs=" + lhs + "\n" +
                        "rhs=" + rhs);
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
            int columnCount, QSFullRelationPool relations, QSFactorBase factorBase, long[] N) {
        IntList[] incidence = new IntList[columnCount];
        int[] weight = new int[columnCount];
        long aliveRows = 0;
        long incidences = 0;
        for (int r = 0; r < rows.size(); r++) {
            SparseMatrixRow row = rows.get(r);
            if (!row.alive) continue;
            aliveRows++;
            for (int c : row.cols) {
                incidences++;
                weight[c]++;
                IntList list = incidence[c];
                if (list == null) incidence[c] = list = new IntList();
                list.add(r);
            }
        }
        int nonNullColumns = 0;
        for (IntList list : incidence) if (list != null) nonNullColumns++;
        System.out.println("aliveRows=" + aliveRows + " incidences=" + incidences +
                " nonNullColumns=" + nonNullColumns);
        for (boolean changed = true; changed;){
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
                    IntList list = incidence[c2];
                    if (list == null)
                        incidence[c2] = list = new IntList();
                    list.add(newRowIndex);
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
            this.alive = true;
        }
    }
    private static SparseMatrixSetup buildQSMatrix(QSFullRelationPool relations, QSFactorBase factorBase, long[] N){
        long t0 = TMX.getCurrentThreadCpuTime();
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
        long sparseRowNanos = TMX.getCurrentThreadCpuTime()-t0;
        t0 = TMX.getCurrentThreadCpuTime();
        sparseRows = singletonFilter(sparseRows, factorBase.columnCount, relations, factorBase, N);
        long singleton1Nanos = TMX.getCurrentThreadCpuTime()-t0;
        t0 = TMX.getCurrentThreadCpuTime();
        sparseRows = degreeTwoMergeFilter(sparseRows, factorBase.columnCount, relations, factorBase, N);
        long doubletonNanos = TMX.getCurrentThreadCpuTime()-t0;
        t0 = TMX.getCurrentThreadCpuTime();
        sparseRows = singletonFilter(sparseRows, factorBase.columnCount, relations, factorBase, N);
        long singleton2Nanos = TMX.getCurrentThreadCpuTime()-t0;
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
        System.out.println("Time for building sparse rows: " + formatSeconds(sparseRowNanos));
        System.out.println("Time for first singleton filter: " + formatSeconds(singleton1Nanos));
        System.out.println("Time for doubleton filter: " + formatSeconds(doubletonNanos));
        System.out.println("Time for second singleton filter: " + formatSeconds(singleton2Nanos));
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
            if(colScratch.length != columnCount)
                throw new IllegalArgumentException("Expected block width " + columnCount +
                        ", got " + colScratch.length);
            if (out.length != rowCount)
                throw new IllegalArgumentException("Expected output length " + rowCount + ", got " + out.length);
            long t0 = TMX.getCurrentThreadCpuTime();
            Arrays.fill(colScratch, 0L);
            multiplyTransposeBlock(rowBlock, colScratch);
            long t1 = TMX.getCurrentThreadCpuTime();
            multiplyBlock(colScratch, out);
            long t2 = TMX.getCurrentThreadCpuTime();
            transposeBlockTime += t1 - t0;
            blockTime += t2 - t1;
        }
        private void multiplyTransposeBlock(long[] rowBlock, long[] colScratch) {
            for (int r = 0; r < rowCount; r++) {
                long lanes = rowBlock[r];
                int end = rowOffsets[r + 1];
                for (int k = rowOffsets[r]; k < end; k++)
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
            long t0 = TMX.getCurrentThreadCpuTime();
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
            tableBuildTime += TMX.getCurrentThreadCpuTime() - t0;
        }
        static long rightMultiplyLookup(long x, long[] table) {
            return table[(int) x & 255]
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
            for(int k = 0; k < RIGHT.length; k++)
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
                    if (!equalsArray(gram, transposed))
                        throw new AssertionError("Gram matrix is not symmetric");
                }
                GF2Elim64 elimination = GF2Matrix64.eliminate64(gram, activeMask(state.v)
                        & ~state.selectedMaskPrev);
                GF2Matrix64.blockDot(state.v, initialV, bucket, overlap);
                GF2Matrix64.multiplyInto(elimination.embeddedInverse, overlap, coefficients);
                buildRightMulTable64(coefficients, rightMulTable);
                long t0 = TMX.getCurrentThreadCpuTime();
                for (int i = 0; i < state.v.length; i++)
                    LEFT[i] ^= rightMultiplyLookup(state.v[i], rightMulTable);
                tableApplyTime += TMX.getCurrentThreadCpuTime() - t0;
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
            long t0 = TMX.getCurrentThreadCpuTime();
            for (int i = 0; i < nextV.length; i++)
                nextV[i] = (av[i] & selectedMask)
                        ^ rightMultiplyLookup(state.v[i], dTable)
                        ^ rightMultiplyLookup(state.vPrev[i], eTable)
                        ^ rightMultiplyLookup(state.vPrev2[i], fTable);
            tableApplyTime += TMX.getCurrentThreadCpuTime() - t0;
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
            ArrayList<long[]> dependencies = new ArrayList<>();
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
            ArrayList<Combination128> nullspace = new ArrayList<>();
            for (int column = 0; column < 128; column++) {
                long[] vector = columns[column].clone();
                long combinationLow = column < 64 ? 1L << column : 0L;
                long combinationHigh = column >= 64 ? 1L << (column - 64) : 0L;
                while (true) {
                    int pivot = firstSetBit(vector);
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
            Combination128[] combinations = nullspace.toArray(Combination128[]::new);
            for (Combination128 combination : combinations) {
                long[] dependency = new long[(d.length + 63) >>> 6];
                for (int row = 0; row < d.length; row++) {
                    int parity = (Long.bitCount(d[row] & combination.low)
                            ^ Long.bitCount(terminalV[row] & combination.high)) & 1;
                    if (parity != 0)
                        dependency[row >>> 6] |= 1L << (row & 63);
                }
                if(isZeroArray(dependency)) continue;
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
    private static long activeMask(long[] block){
        long mask = 0L;
        for (long row : block)
            mask |= row;
        return mask;
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
        private GF2Matrix64(){}
        static GF2Elim64 eliminate64(long[] rows, long requiredMask) {
            long t0 = TMX.getCurrentThreadCpuTime();
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
            if(VERIFY_BLOCK_LANCZOS)
                if ((selectedMask & requiredMask) != requiredMask)
                    throw new AssertionError("Could not include required Lanczos lanes: " +
                        "required=" + Long.toHexString(requiredMask) + ", selected=" +
                        Long.toHexString(selectedMask));
            long[] embeddedInverse = embeddedPrincipalInverse(rows, selectedMask);
            if(VERIFY_BLOCK_LANCZOS)
                verifyEmbeddedInverse(selectedMask, embeddedInverse, rows);
            long[] inverse = rank == 64 ? embeddedInverse : null;
            eliminateTime += TMX.getCurrentThreadCpuTime() - t0;
            return new GF2Elim64(rank, selectedMask, inverse, embeddedInverse, reducedRows);
        }
        private static long[] inverseFullRankDirect(long[] rows) {
            long[] left = rows.clone();
            long[] right = new long[64];
            for (int row = 0; row < 64; row++)
                right[row] = 1L << row;
            int rank = 0;
            for (int col = 0; col < 64; col++) {
                int pivotRow = rank;
                while (pivotRow < 64 && ((left[pivotRow] >>> col) & 1L) == 0L) pivotRow++;
                if (pivotRow == 64) return null;
                long swap = left[rank];
                left[rank] = left[pivotRow];
                left[pivotRow] = swap;
                swap = right[rank];
                right[rank] = right[pivotRow];
                right[pivotRow] = swap;
                for (int row = 0; row < 64; row++) {
                    if (row != rank && ((left[row] >>> col) & 1L) != 0L) {
                        left[row] ^= left[rank];
                        right[row] ^= right[rank];
                    }
                }
                rank++;
            }
            if (rank != 64) return null;
            return right;
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
            long[] compactInverse = inverseFullRankDirect(compactRows);
            if (compactInverse == null)
                throw new AssertionError("Selected principal submatrix is singular: "
                        + Long.toHexString(selectedMask));
            long[] embeddedRows = new long[64];
            for (int compactRow = 0; compactRow < rank; compactRow++) {
                int originalRow = selected[compactRow];
                long compactInverseRow = compactInverse[compactRow];
                long destination = 0L;
                for (int compactColumn = 0; compactColumn < rank; compactColumn++) {
                    if ((compactInverseRow & (1L << compactColumn)) != 0L)
                        destination |= 1L << selected[compactColumn];
                }
                embeddedRows[originalRow] = destination;
            }
            return embeddedRows;
        }
        static void verifyEmbeddedInverse(long selectedMask, long[] embeddedInverse, long[] rows) {
            long[] projector = new long[64];
            for (long bits = selectedMask; bits != 0L; bits &= bits - 1) {
                int lane = Long.numberOfTrailingZeros(bits);
                projector[lane] = 1L << lane;
            }
            long[] left = multiply(multiply(embeddedInverse, rows), projector);
            long[] right = multiply(multiply(projector, rows), embeddedInverse);
            if (!equalsArray(left, projector) || !equalsArray(right, projector))
                throw new AssertionError("Invalid embedded inverse");
            if (!equalsArray(embeddedInverse, transpose(embeddedInverse)))
                throw new AssertionError("Embedded inverse of symmetric principal minor " + "is not symmetric");
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
            long t0 = TMX.getCurrentThreadCpuTime();
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
            blockDotTime += TMX.getCurrentThreadCpuTime() - t0;
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
    private static int firstSetBit(long[] packed) {
        for (int word = 0; word < packed.length; word++) {
            long value = packed[word];
            if (value != 0L)
                return (word << 6) + Long.numberOfTrailingZeros(value);
        }
        return -1;
    }
    private static void xorInto(long[] destination, long[] source) {
        if (destination.length != source.length)
            throw new IllegalArgumentException("Block dimension mismatch");
        for (int i = 0; i < destination.length; i++)
            destination[i] ^= source[i];
    }
    private record Combination128(long low, long high) { }
    private static long[][] nullspaceDependencies(SparseMatrixSetup setup, SplittableRandom rng,
                                                  int originalRelationCount) {
        GF2SparseMatrix matrix = setup.matrix;
        for (int attempt = 0; attempt < 8; attempt++) {
            SplittableRandom attemptRng = rng.split();
            long t0 = TMX.getCurrentThreadCpuTime();
            BlockLanczosResult lanczos = matrix.blockLanczos(attemptRng);
            System.out.println("Block Lanczos iterations: " + lanczos.iterations);
            System.out.println("Lanczos core: " + formatSeconds(TMX.getCurrentThreadCpuTime()-t0));
            System.out.println("Time spent on blockDot: " + formatSeconds(blockDotTime));
            System.out.println("Time spent on eliminate64: " + formatSeconds(eliminateTime));
            System.out.println("Time spent on buildRightMultiplyTable64: " + formatSeconds(tableBuildTime));
            System.out.println("Time spent on rightMultiplyLookup: " + formatSeconds(tableApplyTime));
            System.out.println("Time spent on A^T * block: " + formatSeconds(transposeBlockTime));
            System.out.println("Time spent on A * block: " + formatSeconds(blockTime));
            long[] d = new long[Math.max(lanczos.LEFT.length, lanczos.RIGHT.length)];
            xorInto(d, lanczos.LEFT);
            xorInto(d, lanczos.RIGHT);
            t0 = TMX.getCurrentThreadCpuTime();
            long[][] filteredDependencies = matrix.extractLanczosDependencies(d, lanczos.terminalV);
            if (filteredDependencies.length == 0) continue;
            System.out.println("Extracting dependencies: " + formatSeconds(TMX.getCurrentThreadCpuTime() - t0));
            t0 = TMX.getCurrentThreadCpuTime();
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
            System.out.println("Expanding dependencies: " + formatSeconds(TMX.getCurrentThreadCpuTime() - t0));
            System.out.println();
            return expanded;
        }
        throw new IllegalStateException("Block Lanczos repeatedly failed to find dependencies");
    }
    private static long[] reconstructFromSparseDependency(int[] deps, QSFullRelationPool relations,
                                                          QSFactorBase factorBase, long[] N) {
        int columnCount = factorBase.size + 1;
        long[] X = ONE_MAG;
        long[] Y = ONE_MAG;
        long[] expSums = new long[columnCount];
        ArrayList<long[]> squareFactors = new ArrayList<>();
        for (int relIndex : deps) {
            for (long[] x : relations.xValues[relIndex]) X = multiplyMod(X, x, N);
            for (int k = 0; k < relations.exponentIndex[relIndex].length; k++)
                expSums[relations.exponentIndex[relIndex][k]] += relations.exponentCount[relIndex][k];
            squareFactors.addAll(Arrays.asList(relations.squareFactors[relIndex]));
        }
        MontgomeryReducer reducer = new MontgomeryReducer(N);
        for (int col = 0; col < expSums.length; col++) {
            long e = expSums[col];

            if ((e & 1L) != 0)
                throw new AssertionError("Dependency exponent sum is odd at column " + col);
            long half = e >>> 1;
            if (half == 0) continue;
            if (col == 0) {
                // (-1)^half
                if ((half & 1L) != 0)
                    Y = modSubtract(N, Y, N);
            } else {
                long[] power = reducer.powMod(new long[]{factorBase.primes[col - 1]}, new long[]{half});
                Y = multiplyMod(Y, power, N);
            }
        }
        for (long[] sf : squareFactors) Y = multiplyMod(Y, sf, N);
        if(!equalsArray(multiplyMod(X, X, N), multiplyMod(Y, Y, N)))
            throw new AssertionError("X^2 !≡ Y^2 mod N");
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
        long[] X = ONE_MAG;
        long[] Y = ONE_MAG;
        /*
         * Column 0 is the negative-sign column.
         * Column j + 1 represents factorBase[j].
         */
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
                    X = multiplyMod(X, xValue, N);
                /*
                 * A merged partial relation has the common
                 * large prime stored as a square-side factor.
                 */
                for (long[] squareFactor : relations.squareFactors[relationIndex])
                    Y = multiplyMod(Y, squareFactor, N);
                for (int j = 0; j < relations.exponentIndex[relationIndex].length; j++) {
                    int column = relations.exponentIndex[relationIndex][j];
                    int exponent = relations.exponentCount[relationIndex][j];
                    if (column < 0 || column >= columnCount)
                        throw new AssertionError("Relation exponent column out of range: "
                                + column + " of " + columnCount);
                    exponentSums[column] += exponent;
                }
            }
        }
        if (!selectedAny) return ONE_MAG;
        MontgomeryReducer reducer = new MontgomeryReducer(N);
        for (int column = 0; column < columnCount; column++) {
            long exponent = exponentSums[column];
            if ((exponent & 1L) != 0L)
                throw new IllegalStateException("Invalid dependency: odd exponent " + exponent +
                        " at column " + column);
            long halfExponent = exponent >>> 1;
            if (halfExponent == 0L) continue;
            if (column == 0){
                /*
                 * Sign-column base is -1 mod n.
                 */
                if ((halfExponent & 1L) != 0L)
                    Y = modSubtract(N, Y, N);
            }else{
                long[] power = reducer.powMod(new long[]{factorBase.primes[column - 1]}, new long[]{halfExponent});
                Y = multiplyMod(Y, power, N);
            }
        }
        long[] x2 = multiplyMod(X, X, N);
        long[] y2 = multiplyMod(Y, Y, N);

        if (!equalsArray(x2, y2)) {
            BigInteger bn = new LargeInteger(N, false, true).toBigInteger();
            BigInteger bx = new LargeInteger(X, false, true).toBigInteger();
            BigInteger by = new LargeInteger(Y, false, true).toBigInteger();
            BigInteger bx2 = bx.multiply(bx).mod(bn);
            BigInteger by2 = by.multiply(by).mod(bn);
            throw new AssertionError(
                    "X^2 !≡ Y^2 mod N\n" +
                            "LargeInteger X2 = " + new LargeInteger(x2, false, true) + "\n" +
                            "LargeInteger Y2 = " + new LargeInteger(y2, false, true) + "\n" +
                            "BigInteger X2   = " + bx2 + "\n" +
                            "BigInteger Y2   = " + by2 + "\n" +
                            "BigInteger congruent = " + bx2.equals(by2)
            );
        }
        long[] factor = gcdMagnitudes(modSubtract(X, Y, N), N);
        if (isNontrivialFactor(factor, N)) return factor;
        factor = gcdMagnitudes(modAdd(X, Y, N), N);
        if (isNontrivialFactor(factor, N)) return factor;
        return ONE_MAG;
    }
    @SuppressWarnings("unused")
    public LargeInteger quadraticSieveWithoutConstraints() {
        if(isProbablyPrime()) return this;
        double lnN = logMag(limbs);
        double lnLnN = StrictMath.log(lnN);
        long B = Math.round(Math.exp(0.55 * Math.sqrt(lnN * lnLnN)));
        B = Math.max(B, 100L);
        if (B > Integer.MAX_VALUE)
            throw new ArithmeticException("Unconstrained QS bound " + B + " exceeds supported int range");
        try {
            long t0 = TMX.getCurrentThreadCpuTime();
            QSFactorBase factorBase = buildFactorBase(limbs, (int) B);
            int[] fbIndex = buildFactorBaseIndexMap(factorBase);
            long factorBaseBuildTime = TMX.getCurrentThreadCpuTime() - t0;
            String S = toString();
            System.out.println("Number entered unconstrained QS: " + S + "(" + S.length() + " digits)");
            System.out.println("Factor-base bound: " + B);
            System.out.println("Factor base build time: " + formatSeconds(factorBaseBuildTime) +
                    " (" + factorBase.size + " primes)");
            int requiredRelations = factorBase.primes.length + 30;
            QSFullRelationPool relations = new QSFullRelationPool(requiredRelations +
                    requiredRelations/8);
            QSPartialRelationPool partials = new QSPartialRelationPool(requiredRelations +
                    requiredRelations/8);
            PartialGraph partialGraph = new PartialGraph();
            //A deterministic seed is used so that A sequences are fixed. This is to make an objective
            //decision on if a code change really made the method faster. Please do not replace with
            //rng.nextLong() or rng.split(). Same case for the block Lanczos seed
            long A_RNG_SEED = 6601994121933596671L;
            System.out.println("A RNG seed: " + Long.toUnsignedString(A_RNG_SEED));
            System.out.println();
            SplittableRandom A_RNG = new SplittableRandom(A_RNG_SEED);
            long relationStart = TMX.getCurrentThreadCpuTime();
            System.out.printf(
                    "%3s %50s %10s %12s %12s %10s %8s %20s %14s %11s %11s %16s %16s %16s %16s %14s %13s %14s %14s %12s %12s %15s%n",
                    "#", "A", "int", "candidates", "full", "sing", "merge", "precomputation",
                    "root", "small", "large", "scanning", "candidateScan", "hitLink", "smallDiv", "class", "processing", "total", "rel/s",
                    "tests", "rel/poly", "rel total"
            );
            long[] sqrt2N = sqrtCeilMagnitude(shiftLeftBits(limbs, 1));
            for(long blocks = 0; relations.size < requiredRelations; blocks++) {
                System.out.printf("%3d ", blocks+1);
                sieve128Polynomials(limbs, sqrt2N, factorBase, fbIndex, relations, partials, partialGraph, A_RNG);
//                if(blocks % 16 == 0){
//                    System.out.println("Total usable relations up to now: " + relations.size());
//                    System.out.println("Total blocks up to now: " + blocks);
//                    System.out.println("Time up to now: " + formatSeconds(TMX.getCurrentThreadCpuTime() - relationStart));
//                    System.out.println();
//                }
            }
            System.out.println();
            long relationNanos = TMX.getCurrentThreadCpuTime() - relationStart;
            System.out.println("Total QS relation time: " + formatSeconds(relationNanos));
            System.out.println("Overall relation collection rate: " + relations.size * 1e9 / relationNanos + " relations/second");
            System.out.println();
            validateQSRelations(relations, factorBase, limbs);
            int originalRelationCount = relations.size;
            long buildMatrixNanos = TMX.getCurrentThreadCpuTime();
            SparseMatrixSetup setup = buildQSMatrix(relations, factorBase, limbs);
            buildMatrixNanos = TMX.getCurrentThreadCpuTime() - buildMatrixNanos;
            System.out.println("Matrix build time: " + formatSeconds(buildMatrixNanos));
            long matrixSeed = 6722688380560470180L;
            System.out.println();
            long[][] dependencies = nullspaceDependencies(setup, new SplittableRandom(matrixSeed),
                    originalRelationCount);
            for (long[] dependency : dependencies) {
                long[] factor = reconstructFactor(dependency, relations, factorBase, limbs);
                if (isNontrivialFactor(factor, limbs))
                    return new LargeInteger(factor, false);
            }
        }catch (FactorFoundException e) {return new LargeInteger(e.factor, false);}
        return ONE;
    }
    //given a number N, find a relation X^2 ≡ Y^2 (mod N). then (X-Y) and (X+Y) reveal factors of N.
    private static long[] quadraticSieve(FactorJob task, double remainingECMSeconds) {
        if (!Double.isFinite(remainingECMSeconds) || remainingECMSeconds <= 0.0) return ONE_MAG;
        /*
         * Spend at most 5% of the known remaining ECM time testing QS.
         * Keep the probe between 0.5 and 10 seconds.
         */
        double probeSeconds = Math.max(remainingECMSeconds * 0.05, 20.0);
        long probeBudgetNanos = Math.round(probeSeconds * 1e9);
        long totalStart = TMX.getCurrentThreadCpuTime();
        String numberText = new LargeInteger(task.n, false).toString();
        System.out.println("Number entering QS: " + numberText + "(" + numberText.length() + " digits)");
        /*
         * Factor-base construction time counts against the QS probe.
         */
        try {
            if (task.factorBase == null) {
                task.factorBase = buildFactorBase(task.n, Math.toIntExact(task.B));
                task.fbIndex = buildFactorBaseIndexMap(task.factorBase);
            }
            int requiredRelations = task.factorBase.size + 30;
            System.out.println("Factor-base size: " + task.factorBase.size);
            System.out.println("Required relations: " + requiredRelations);
            if(task.relations == null)
                task.relations = new QSFullRelationPool(requiredRelations*9/8);
            if(task.partials == null)
                task.partials = new QSPartialRelationPool(requiredRelations*9/8);
            ArrayList<Double> blockRates = new ArrayList<>();
            int blocks = 0;
            boolean qsAccepted;
            SplittableRandom rng = FAST_RNG.split();
            System.out.printf(
                    "%3s %50s %10s %12s %12s %10s %8s %20s %14s %11s %11s %16s %16s %16s %16s %14s %13s %14s %14s %12s %12s %15s%n",
                    "#", "A", "int", "candidates", "full", "sing", "merge", "precomputation",
                    "root", "small", "large", "scanning", "candidateScan", "hitLink", "smallDiv", "class", "processing", "total", "rel/s",
                    "tests", "rel/poly", "rel total"
            );
            long[] sqrt2N = sqrtCeilMagnitude(shiftLeftBits(task.n, 1));
            while (task.relations.size < requiredRelations) {
                int relationsBefore = task.relations.size;
                System.out.printf("%4d", blocks+1);
                long blockStart = TMX.getCurrentThreadCpuTime();
                sieve128Polynomials(task.n, sqrt2N, task.factorBase, task.fbIndex, task.relations, task.partials,
                        task.partialGraph, rng);
                long blockNanos = TMX.getCurrentThreadCpuTime() - blockStart;
                int relationsAdded = task.relations.size - relationsBefore;
                double rate = blockNanos == 0L ? 0.0 : relationsAdded * 1e9 / blockNanos;
                blockRates.add(rate);
                blocks++;
                long elapsedNanos = TMX.getCurrentThreadCpuTime() - totalStart;
                boolean budgetExpired = elapsedNanos >= probeBudgetNanos;
                if (!budgetExpired) continue;
                /*
                 * A very short sample cannot reliably distinguish a lucky
                 * block from a genuinely good QS input.
                 */
                double medianRate = 0;
                for(double blockRate: blockRates) medianRate += blockRate;
                medianRate /= blockRates.size();
                if (!(medianRate > 0.0) || !Double.isFinite(medianRate)) {
                    System.out.println("QS rejected: mean verified-relation rate is zero");
                    System.out.println();
                    return ONE_MAG;
                }
                int remainingRelations = requiredRelations - task.relations.size;
                double projectedQSSeconds = 1.15*remainingRelations / medianRate;
                task.qsSeconds = projectedQSSeconds;
                System.out.println("QS probe: blocks=" + blocks +
                        ", relations total=" + task.relations.size + "/" + requiredRelations +
                        ", mean rate=" + medianRate + "/s, projected remaining=" + projectedQSSeconds + " seconds");
                qsAccepted = projectedQSSeconds*0.75 < remainingECMSeconds;
                if (!qsAccepted) {
                    System.out.println("QS rejected in favor of ECM");
                    System.out.println();
                    return ONE_MAG;
                }
                System.out.println("QS accepted over remaining ECM work");
                break;
            }
            /*
             * Continue from the relations already collected by the probe.
             * Do not restart QS.
             */
            while (task.relations.size < requiredRelations) {
                System.out.printf("%4d", blocks+1);
                sieve128Polynomials(task.n, sqrt2N, task.factorBase, task.fbIndex, task.relations, task.partials,
                        task.partialGraph, rng);
                blocks++;
            }
            System.out.println("Total QS relation time: " + formatSeconds(TMX.getCurrentThreadCpuTime() - totalStart));
            validateQSRelations(task.relations, task.factorBase, task.n);
            int originalRelationCount = task.relations.size;
            long matrixTime = TMX.getCurrentThreadCpuTime();
            SparseMatrixSetup setup = buildQSMatrix(task.relations, task.factorBase, task.n);
            long[][] dependencies = nullspaceDependencies(setup, FAST_RNG.split(), originalRelationCount);
            matrixTime = TMX.getCurrentThreadCpuTime() - matrixTime;
            System.out.println("Matrix build+solve time: " + formatSeconds(matrixTime));
            System.out.println();
            for (long[] dependency : dependencies) {
                long[] factor = reconstructFactor(dependency, task.relations, task.factorBase, task.n);
                if (isNontrivialFactor(factor, task.n)){
                    System.out.println("QS FACTOR FOUND: " + new LargeInteger(factor, false));
                    return factor;
                }
            }
        } catch (FactorFoundException e) {return e.factor;}
        return ONE_MAG;
    }
}