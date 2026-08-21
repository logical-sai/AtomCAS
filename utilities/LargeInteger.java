package com.AtomCAS.utilities;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
public class LargeInteger extends ScalarAtom {
    /**
     * Little-endian limbs where one "digit" is in base 2^64. Any long primitive,
     * negative or positive, is a valid limb. Empty arrays are invalid
     */
    public final long[] limbs;
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
    static boolean isZeroArray(long[] a){return a.length == 1 && a[0] == 0;}
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
    public static final LargeInteger FOUR = new LargeInteger(FOUR_MAG, false, true);
    /**
     * LargeInteger constant 5
     */
    public static final LargeInteger FIVE = new LargeInteger(FIVE_MAG, false, true);
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
        this.negative = !isZeroArray(this.limbs) && sign;
    }
    private static final LargeInteger[] POS_SMALL = new LargeInteger[31];
    private static final LargeInteger[] NEG_SMALL = new LargeInteger[31];
    static {
        for (int x = 1; x <= 30; x++) {
            POS_SMALL[x] = new LargeInteger(new long[]{x}, false);
            NEG_SMALL[x] = new LargeInteger(new long[]{x}, true);
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
            throw new IllegalArgumentException("Error: Inputted expression does not evaluate to an integer");
        }
        s = s.trim();
        if (s.isEmpty())
            throw new NumberFormatException("Error: Empty string");
        boolean neg = false;
        int pos = 0;
        if (s.charAt(0) == '-') {
            neg = true;
            pos = 1;
        }
        if (pos == s.length())
            throw new NumberFormatException("Error: Only negative is present");
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
        acc = addSmallMag(multiplySmallMag(acc, 1_000_000_000_000_000_000L), parseDecimalChunk(s, pos, end));
        pos = end;
        while (pos < s.length()) {
            end = pos + 18;
            acc = addSmallMag(multiplySmallMag(acc, 1_000_000_000_000_000_000L), parseDecimalChunk(s, pos, end));
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
        if (n == a.length) return a;
        long[] out = new long[n];
        System.arraycopy(a, 0, out, 0, n);
        return out;
    }
    private static long bitLength(long[] x) {
        x = trimLeadingZeros(x);
        if (isZeroArray(x))
            return 0;
        int top = x.length - 1;
        return top * 64L + 64L - Long.numberOfLeadingZeros(x[top]);
    }
    public long bitLength() {return bitLength(limbs);}
    @Override
    public int hashCode() {return CASHash.rational(this, ONE);}
    public static LargeInteger basePower(int chunks) {
        if (chunks < 0)
            throw new IllegalArgumentException("Negative chunk power");
        long[] arr = new long[chunks + 1];
        arr[arr.length - 1] = 1L;
        return new LargeInteger(arr, false);
    }
    public static LargeFraction pow10(long power) {return TEN.power(power);}
    @Override
    public LargeInteger copy() {return new LargeInteger(limbs, negative);}
    //returns a String version that can be printed.
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (negative) sb.append("-");
        if (limbs.length == 1)
            return sb.append(Long.toUnsignedString(limbs[0])).toString();
        //1233/4096 is an approximation of log_10(2)
        long m = (bitLength(limbs) * 1233L >>> 12) / 2;
        LargeInteger power = pow10(m).numerator;
        LargeInteger[] DR = abs().quotientAndRemainder(power);
        sb.append(DR[0]);
        String s = DR[1].toString();
        sb.repeat("0", Math.toIntExact(Math.max(m - s.length(), 0)));
        sb.append(s);
        return sb.toString();
    }
    public String toLatex() {return toString();}
    public BigInteger toBigInteger() {
        if(bitLength(limbs) > Integer.MAX_VALUE) return null;
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
        else if (other instanceof LargeInteger I)
            return compareArray(limbs, I.limbs) == 0 && negative == I.negative;
        return compareTo((ScalarAtom) other) == 0L;
    }
    public LargeInteger sign() {
        if (negative) return NEGATIVE_ONE;
        return isZeroArray(limbs) ? ZERO : ONE;
    }
    public LargeInteger negate() {return new LargeInteger(limbs, !negative);}
    @Override
    public LargeInteger realPart() {return this;}
    @Override
    public LargeInteger imagPart() {return ZERO;}
    @Override
    public LargeInteger abs() {
        if (!negative) return this;
        return new LargeInteger(limbs, false);
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
        tc = Arrays.copyOf(tc, tc.length);
        boolean negative = tc[tc.length - 1] < 0;
        if (!negative)
            return new LargeInteger(trimLeadingZeros(tc), false);
        // Negative: abs = -(TWO's complement value)
        // invert all limbs, then add 1.
        for (int i = 0; i < tc.length; i++) tc[i] = ~tc[i];
        long carry = 1L;
        for (int i = 0; i < tc.length && carry != 0L; i++) {
            long old = tc[i];
            tc[i] = old + carry;
            carry = Long.compareUnsigned(tc[i], old) < 0 ? 1L : 0L;
        }
        return new LargeInteger(trimLeadingZeros(tc), true);
    }
    public LargeInteger not() {return negate().subtract(1);}
    public LargeInteger and(LargeInteger other) {
        int len = Math.max(limbs.length, other.limbs.length) + 1;
        long[] a = toTwosComplement(len);
        long[] b = other.toTwosComplement(len);
        long[] out = new long[len];
        for (int i = 0; i < len; i++)
            out[i] = a[i] & b[i];
        return fromTwosComplement(out);
    }
    public LargeInteger or(LargeInteger other) {
        int len = Math.max(limbs.length, other.limbs.length) + 1;
        long[] a = toTwosComplement(len);
        long[] b = other.toTwosComplement(len);
        long[] out = new long[len];
        for (int i = 0; i < len; i++)
            out[i] = a[i] | b[i];
        return fromTwosComplement(out);
    }
    public LargeInteger xor(LargeInteger other) {
        int len = Math.max(limbs.length, other.limbs.length) + 1;
        long[] a = toTwosComplement(len);
        long[] b = other.toTwosComplement(len);
        long[] out = new long[len];
        for (int i = 0; i < len; i++)
            out[i] = a[i] ^ b[i];
        return fromTwosComplement(out);
    }
    private static long[] randomMagBetween(long[] startInclusive, long[] endExclusive) {
        startInclusive = trimLeadingZeros(startInclusive);
        endExclusive = trimLeadingZeros(endExclusive);
        int cmp = compareArray(startInclusive, endExclusive);
        if (cmp >= 0)
            throw new IllegalArgumentException("Invalid random magnitude range");
        long[] range = trimLeadingZeros(subtractMagnitudes(endExclusive, startInclusive));
        long[] candidate;
        do {
            candidate = new long[range.length];
            boolean free = false;
            for (int i = range.length - 1; i >= 0; i--) {
                if (free) {
                    candidate[i] = SECURE_RNG.nextLong();
                    continue;
                }
                long upperBound = range[i];
                if (upperBound == -1L) {
                    candidate[i] = SECURE_RNG.nextLong();
                    if (candidate[i] != -1L) free = true;
                    continue;
                }
                long exclusive = upperBound + 1L;
                free = Long.compareUnsigned(candidate[i], upperBound) < 0;
                if (exclusive > 0L) {
                    candidate[i] = SECURE_RNG.nextLong(exclusive);
                    continue;
                }
                // unsigned upperBound lies in [2^63, 2^64 - 2]
                do candidate[i] = SECURE_RNG.nextLong();
                while (Long.compareUnsigned(candidate[i], upperBound) > 0);
            }
        }while (compareArray(candidate, range) >= 0);
        return addMagnitudes(startInclusive, candidate);
    }
    //generates a random integer within a specified range [start, end)
    public static LargeInteger random(LargeInteger start, LargeInteger end) {
        if(start.negative) return (LargeInteger) random(ZERO, (LargeInteger) end.subtract(start)).add(start);
        return new LargeInteger(randomMagBetween(start.limbs, end.limbs), false);
    }
    public static LargeInteger randomChunks(int chunks){return randomChunks(chunks, SECURE_RNG);}
    public static LargeInteger randomChunks(int chunks, SecureRandom rng) {
        if (chunks < 0)
            throw new IllegalArgumentException("Error: Chunks must be nonnegative");
        else if (chunks == 0) return ZERO;
        long[] res = new long[chunks];
        for (int i = 0; i < chunks; i++) res[i] = rng.nextLong();
        while (res[res.length - 1] == 0) res[res.length - 1] = rng.nextLong();
        return new LargeInteger(res, false);
    }
    static int compareArray(long[] a, long[] b) {
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);
        return compareArrayUntrimmed(a, b);
    }
    private static int compareArrayUntrimmed(long[] a, long[] b) {
        int alen = a.length;
        while (alen > 1 && a[alen - 1] == 0L) alen--;
        int bLen = b.length;
        while (bLen > 1 && b[bLen - 1] == 0L) bLen--;
        if (alen != bLen)
            return Integer.compare(alen, bLen);
        for (int i = alen - 1; i >= 0; i--) {
            int cmp = Long.compareUnsigned(a[i], b[i]);
            if (cmp != 0)
                return cmp;
        }
        return 0;
    }
    static boolean equalsArray(long[] a, long[] b) {
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++)
            if (a[i] != b[i]) return false;
        return true;
    }
    @Override
    public int compareMags(ScalarAtom other) {
        return switch(other){
            case LargeInteger I -> compareArray(limbs, I.limbs);
            case LargeFraction F -> multiply(F.denominator).compareMags(F.numerator);
            case LargePowerMonomial L -> {
                LargeFraction f = power(L.exponent.denominator);
                LargeFraction o = L.power(new LargeFraction(L.exponent.denominator, ONE)).base;
                yield f.compareMags(o);
            }case RadicalTerm R -> {
                if (other.isNotReal()) {
                    RadicalTermSum RS = (RadicalTermSum) other.multiply(R.complexConjugate());
                    yield square().compareMags(RS);
                }
                if (equals(R.rationalPart) && R.coefficient.equals(LargeInteger.ZERO)) yield 0;
                LargeFraction f = (LargeFraction) subtract(R.rationalPart).divide(R.coefficient);
                f = f.power(R.exponent.exponent.denominator);
                LargeFraction o = R.exponent.base;
                yield f.compareMags(o);
            }default -> throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
        };
    }
    @Override
    public int compareTo(ScalarAtom other) {
        if (other.isNotReal()) throw new IllegalStateException("Error: Complex numbers cannot be compared");
        return switch (other) {
            case LargeInteger I -> {
                if(negative == I.negative){
                    yield switch(negative){
                        case false -> compareMags(I);
                        case true -> I.compareMags(this);
                    };
                }
                yield !negative ? 1 : -1;
            }case LargeFraction F -> multiply(F.denominator).compareTo(F.numerator);
            case LargePowerMonomial LPM -> {
                LargeFraction f = power(LPM.exponent.denominator);
                LargeFraction o = LPM.power((LargeFraction) LPM.exponent.denominator.divide(ONE)).base;
                int res = f.compareMags(o);
                if (res == 1) {
                    if (negative) yield -1;
                    yield 1;
                } else if (res == -1) {
                    if (other.lessThan(ZERO)) yield 1;
                    yield -1;
                }
                yield res;
            }
            default -> {
                long B = compareMags(other);
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
    private record LongPair(long high, long low) {
        LongPair addLow(long x) {
            long newLow = low + x;
            long carry = Long.compareUnsigned(newLow, low) < 0 ? 1L : 0L;
            return new LongPair(high + carry, newLow);
        }
        static LongPair multiply64(long a, long b) {return new LongPair(Math.unsignedMultiplyHigh(a, b), a * b);}
        static LongPair add64(long a, long b) {
            long low = a + b;
            long carry = Long.compareUnsigned(low, a) < 0 ? 1L : 0L;
            return new LongPair(carry, low);
        }
        int compareTo(LongPair other) {
            int c = Long.compareUnsigned(high, other.high);
            if (c != 0) return c;
            return Long.compareUnsigned(low, other.low);
        }
        DivRemLongPair divideByLong(long divisor) {
            if (divisor == 0L)
                throw new ArithmeticException("Error: divide by zero");
            /*
             * Grade-school division in base 2^64:
             * high / divisor = qHigh R rHigh
             * then bring down low:
             * (rHigh * 2^64 + low) / divisor = qLow R remainder
             * Since rHigh < divisor, qLow fits in one 64-bit limb.
             */
            long qHigh = Long.divideUnsigned(high, divisor);
            long rHigh = Long.remainderUnsigned(high, divisor);

            if (rHigh == 0L && low == 0L)
                return new DivRemLongPair(new LongPair(qHigh, 0L), 0L);
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
                nLow = low;
            } else {
                d = divisor << shift;
                /*
                 * This cannot overflow beyond 128 bits:
                 *
                 * rHigh < divisor
                 *
                 * so after normalization rHigh << shift < 2^64.
                 */
                nHigh = (rHigh << shift) | (low >>> (64 - shift));
                nLow = low << shift;
            }

            /*
             * Split the normalized 128-bit dividend:
             *
             * [u3 u2 u1 u0]
             *
             * and 64-bit divisor:
             *
             * [v1 v0]
             *
             * into unsigned 32-bit words.
             */
            int u3 = (int) (nHigh >>> 32);
            int u2 = (int) nHigh;
            int u1 = (int) (nLow >>> 32);
            int u0 = (int) nLow;
            int v1 = (int) (d >>> 32);
            int v0 = (int) d;
            /*
             * First quotient word:
             *
             * [u3 u2 u1] / [v1 v0]
             */
            DivRem3By2 first = divideThreeByTwo(u3, u2, u1, v1, v0);
            /*
             * Bring down u0:
             *
             * [r1 r0 u0] / [v1 v0]
             */
            DivRem3By2 second = divideThreeByTwo(first.remHigh(), first.remLow(), u0, v1, v0);
            /*
             * Combine the two unsigned 32-bit quotient words.
             */
            long qLow = (Integer.toUnsignedLong(first.quotient()) << 32) | Integer.toUnsignedLong(second.quotient());
            /*
             * Combine normalized remainder.
             */
            long rNormalized = (Integer.toUnsignedLong(second.remHigh()) << 32) | Integer.toUnsignedLong(second.remLow());
            long remainder = shift == 0 ? rNormalized : rNormalized >>> shift;
            return new DivRemLongPair(new LongPair(qHigh, qLow), remainder);
        }
        //OtherDivRemLongPair divide(LongPair other) {return divideTwoByTwo(high, low, other.high, other.low);}
        boolean lessThan(LongPair other) {return compareTo(other) < 0;}
        boolean lesserThanOrEqualTo(LongPair other) {return compareTo(other) <= 0;}
    }
    private record DivRemLongPair(LongPair quotient, long remainder) {}
    private record SignedMag(long[] mag, boolean negative) {
        static SignedMag ZERO = new SignedMag(ZERO_MAG, false);
        SignedMag {
            mag = trimLeadingZeros(mag);
            negative = !isZeroArray(mag) && negative;
        }
        static SignedMag positive(long[] mag) {return new SignedMag(mag, false);}
        static SignedMag negative(long[] mag) {return new SignedMag(mag, true);}
        SignedMag negate() {return new SignedMag(mag, !negative);}
        SignedMag add(SignedMag other) {
            if (isZeroArray(mag)) return other;
            if (isZeroArray(other.mag)) return this;
            // Same negative: add magnitudes, keep negative.
            if (negative == other.negative)
                return new SignedMag(addMagnitudes(mag, other.mag), negative);
            // Different signs: subtract smaller abs from larger abs.
            int cmp = compareArray(mag, other.mag);
            if (cmp == 0)
                return new SignedMag(ZERO_MAG, false);
            if (cmp > 0)
                return new SignedMag(subtractMagnitudes(mag, other.mag), negative);
            return new SignedMag(subtractMagnitudes(other.mag, mag), other.negative);
        }
        SignedMag multiply(SignedMag other) {return new SignedMag(multiplyMagnitudes(mag, other.mag), negative ^ other.negative);}
        SignedMag multiplyMagnitude(long[] other) {return new SignedMag(multiplyMagnitudes(mag, other), negative);}
        SignedMag multiplySignedLong(long other) {return new SignedMag(multiplySmallMag(mag, Math.abs(other)), negative ^ (other < 0));}
        long[] toPositiveMagnitudeChecked() {
            if (negative) throw new ArithmeticException("Error: Some interpolation produced negative final values");
            return mag;
        }
        SignedMag shiftRightOneBit(){return new SignedMag(LargeInteger.shiftRightBits(mag, 1), negative);}
        long[] modMag(long[] n) {
            long[] r = modMagnitudes(mag, n);
            if (!negative)
                return r;
            if (isZeroArray(r))
                return ZERO_MAG;
            return subtractMagnitudes(n, r);
        }
        int compareTo(SignedMag other) {
            int cmp = compareArray(mag, other.mag);
            if (cmp == 1) {
                if (negative) return -1;
                return 1;
            } else if (cmp == -1) {
                if (other.negative) return 1;
                return -1;
            }
            if (negative && !other.negative) return -1;
            else if (!negative && other.negative) return 1;
            return 0;
        }
        SignedMag cube() {return new SignedMag(cubeMagnitude(mag), negative);}
    }
    private static long[] addSmallMag(long[] addend, long adder) {
        addend = trimLeadingZeros(addend);
        long[] out = new long[addend.length + 1];
        out[0] = addend[0] + adder;
        long carry = Long.compareUnsigned(out[0], addend[0]) < 0 ? 1 : 0;
        for (int i = 1; i < addend.length; i++) {
            out[i] = addend[i] + carry;
            carry = out[i] < addend[i] ? 1 : 0L;
        }
        out[addend.length] = carry;
        return trimLeadingZeros(out);
    }
    private static long[] addMagnitudes(long[] a, long[] b) {
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);
        if (a.length == 1)
            return addSmallMag(b, a[0]);
        if (b.length == 1)
            return addSmallMag(a, b[0]);
        int max = Math.max(a.length, b.length);
        long[] res = new long[max + 1];
        long carry = 0L;
        for (int i = 0; i < max; i++) {
            long a_i = i < a.length ? a[i] : 0L;
            long b_i = i < b.length ? b[i] : 0L;
            long s = a_i + b_i;
            long c1 = Long.compareUnsigned(s, a_i) < 0 ? 1L : 0L;
            long r = s + carry;
            long c2 = Long.compareUnsigned(r, s) < 0 ? 1L : 0L;
            res[i] = r;
            carry = c1 | c2;
        }
        res[max] = carry;
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
    private static long[] subtractMagnitudes(long[] a, long[] b) {
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);
        if (compareArray(a, b) < 0)
            throw new ArithmeticException("Error: subtractMagnitudes requires a >= b");
        if (b.length == 1)
            return subtractSmallMag(a, b[0]);
        long[] out = new long[a.length];
        long borrow = 0L;
        for (int i = 0; i < a.length; i++) {
            long ai = a[i];
            long bi = i < b.length ? b[i] : 0L;

            long t = ai - bi;
            long b1 = Long.compareUnsigned(ai, bi) < 0 ? 1L : 0L;

            long u = t - borrow;
            long b2 = Long.compareUnsigned(t, borrow) < 0 ? 1L : 0L;

            out[i] = u;
            borrow = b1 | b2;
        }
        if (borrow != 0L)
            throw new ArithmeticException("Error: subtractMagnitudes underflow");
        return trimLeadingZeros(out);
    }
    private static long[] subtractSmallMag(long[] a, long small) {
        a = trimLeadingZeros(a);
        if (small == 0L)
            return a;
        long[] out = Arrays.copyOf(a, a.length);
        long old = out[0];
        out[0] = old - small;
        long borrow = Long.compareUnsigned(old, small) < 0 ? 1L : 0L;
        int i = 1;
        while (borrow != 0L && i < out.length) {
            old = out[i];
            out[i] = old - 1L;
            borrow = old == 0L ? 1L : 0L;
            i++;
        }
        if (borrow != 0L)
            throw new ArithmeticException("subtractSmallMag underflow");
        return trimLeadingZeros(out);
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
    @Override
    public ScalarAtom add(ScalarAtom other) {
        //This method adds two integers via base-2^64 chunking. Like every other method, it assumes both are normalized to the best they can be. Runs in time complexity O(n), n being max limbs of chunks
        if (other instanceof LargeInteger I) {
            if (negative == I.negative)
                return new LargeInteger(addMagnitudes(limbs, I.limbs), negative, true);
            long cmp = compareMags(I);
            if (cmp == 0) return ZERO;
            else if (cmp == 1)
                return new LargeInteger(subtractMagnitudes(limbs, I.limbs), negative, true);
            return new LargeInteger(subtractMagnitudes(I.limbs, limbs), I.negative, true);
        }else if (other instanceof LargeFraction F)
            return new LargeFraction((LargeInteger) F.denominator.multiply(this).add(F.numerator), F.denominator);
        else if (other instanceof LargePowerMonomial LPM)
            return new RadicalTerm(new LargeFraction(this, ONE), LargeFraction.ONE, LPM);
        else if (other instanceof RadicalTerm RT)
            return new RadicalTerm((LargeFraction) add(RT.rationalPart), RT.coefficient, RT.exponent);
        else if (other instanceof RadicalTermSum RTS)
            return new RadicalTermSum(RTS.sum, (LargeFraction) RTS.constant.add(this));
        else if (other instanceof LargePowerNode LPN)
            return new RadicalTermNode(new LargeFraction(this, ONE), LargeFraction.ONE, LPN);
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    public ScalarAtom add(ScalarAtom... atoms) {
        ScalarAtom result = this;
        for (ScalarAtom atom : atoms) result = result.add(atom);
        return result;
    }
    public LargeInteger add(long other) {
        if (other == 0L) return this;
        boolean otherSign = other < 0;
        long otherMag = Math.abs(other);
        // Caveat: Long.MIN_VALUE needs special handling because -Long.MIN_VALUE overflows.
        if (other == Long.MIN_VALUE)
            return (LargeInteger) add(new LargeInteger(Long.toString(other)));
        long[] o = {otherMag};
        if (negative == otherSign)
            return new LargeInteger(addMagnitudes(limbs, o), negative, true);
        int cmp = compareArray(limbs, o);
        if (cmp == 0) return ZERO;
        else if (cmp > 0)
            return new LargeInteger(subtractMagnitudes(limbs, o), negative, true);
        return new LargeInteger(subtractMagnitudes(o, limbs), otherSign, true);
    }
    public ScalarAtom subtract(ScalarAtom... atoms) {
        ScalarAtom result = this;
        for (ScalarAtom atom : atoms) result = result.subtract(atom);
        return result;
    }
    public LargeInteger subtract(long other) {return add(-other);}
    private static long[] shiftLeftLimbs(long[] x, int limbs) {
        if (isZeroArray(x)) return ZERO_MAG;
        if (limbs <= 0) return x;
        long[] out = new long[x.length + limbs];
        // pad zeros at the beginning
        System.arraycopy(x, 0, out, limbs, x.length);
        return trimLeadingZeros(out);
    }
    private static long[] shiftLeftBits(long[] n, long shift) {
        if (isZeroArray(n)) return ZERO_MAG;
        if (shift < 0) return shiftRightBits(n, -shift);
        if (shift == 0) return n;
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
        if (bits >= 64) return shiftRightBits(shiftRightLimbs(x, (int) (bits / 64)), bits & 63);
        if (bits == 0) return x;
        long[] out = new long[x.length];
        long carry = 0L;
        for (int i = x.length - 1; i >= 0; i--) {
            long limb = x[i];
            out[i] = (limb >>> bits) | carry;
            carry = limb << (64 - bits);
        }
        return trimLeadingZeros(out);
    }
    private static void shiftRightOneBitInPlace(long[] a) {
        long carry = 0L;
        for (int i = a.length - 1; i >= 0; i--) {
            long x = a[i];
            a[i] = (x >>> 1) | carry;
            carry = x << 63;
        }
    }
    private static void subtractShiftedLeftBitsInPlace(long[] a, long[] b, int shift) {
        if (shift <= 0 || shift >= 64)
            throw new IllegalArgumentException();
        long borrow = 0L;
        long spill = 0L;
        int i = 0;
        // Subtract ordinary shifted limbs.
        for (; i < b.length; i++) {
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
    private static boolean testBit(long[] x, long bit) {
        long limb = bit >>> 6;
        int offset = (int)(bit & 63);
        if (limb >= x.length) return false;
        return ((x[(int)limb] >>> offset) & 1L) != 0L;
    }
    public boolean testBit(long bit){return testBit(limbs, bit);}
    private static long[] slice(long[] a, int from, int to) {
        a = trimLeadingZeros(a);
        return sliceFixed(a, from, to-from);
    }
    private static long[] multiplySmallMag(long[] a, long m) {
        if (isZeroArray(a) || m == 0L) return ZERO_MAG;
        else if (a.length == 1) {
            long low = a[0] * m;
            long high = Math.unsignedMultiplyHigh(a[0], m);
            return high == 0L ? new long[]{low} : new long[]{low, high};
        }
        long[] out = new long[a.length + 1];
        long carry = 0L;
        for (int i = 0; i < a.length; i++) {
            long low = a[i]*m;
            long high = Math.unsignedMultiplyHigh(a[i], m);
            long low2 = low+carry;
            high += Long.compareUnsigned(low2, low) < 0 ? 1L : 0L;
            out[i] = low2;
            carry = high;
        }
        out[a.length] = carry;
        return trimLeadingZeros(out);
    }
    private static SignedMag multiplyMagnitudeBySignedLong(long[] a, long m) {
        if (m < 0) return new SignedMag(multiplySmallMag(a, -m), true);
        return new SignedMag(multiplySmallMag(a, m), false);
    }
    /**
     * Multiplication dispatch thresholds.
     * These constants are intentionally implementation-specific. They are chosen
     * experimentally from benchmark heatmaps for the current limb representation
     * and multiplication implementations.
     * In this build:
     * <l>below KARATSUBA_THRESHOLD: grade-school multiplication is fastest</l>
     * below TOOM_COOK_3_THRESHOLD: Karatsuba is fastest
     * at or above TOOM_COOK_3_THRESHOLD: Toom-Cook 3 is fastest
     * Do not change these based on asymptotic complexity alone; re-benchmark first.
     */
    private static final int KARATSUBA_THRESHOLD = 32;
    private static final int TOOM_COOK_3_THRESHOLD = 1024;
    static long[] multiplyMagnitudes(long[] a, long[] b){return multiplyMagnitudesWithThresholds(a, b, KARATSUBA_THRESHOLD, TOOM_COOK_3_THRESHOLD);}
    static long[] multiplyMagnitudesWithThresholds(long[] a, long[] b, int KT, int T3T) {
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);
        if (isZeroArray(a) || isZeroArray(b))
            return ZERO_MAG;
        int cmp = compareArrayUntrimmed(a, b);
        if (cmp < 0) {
            long[] t = a;
            a = b;
            b = t;
        }else if (cmp == 0) return squareMagnitude(a);
        int max = a.length;
        int min = b.length;
        if (min == 1) return multiplySmallMag(a, b[0]);
        else if (min < KT) {
            long[] out = new long[max + min];
            for (int k = 0; k < max; k++) {
                long scalar = a[k];
                if (scalar == 0L) continue;
                long carry = 0L;
                int currentIndex = k;
                for (long l : b) {
                    long low = l * scalar;
                    long high = Math.unsignedMultiplyHigh(l, scalar);
                    long t = low + out[currentIndex];
                    if (Long.compareUnsigned(t, low) < 0) high++;
                    long result = t + carry;
                    if (Long.compareUnsigned(result, t) < 0) high++;
                    out[currentIndex] = result;
                    carry = high;
                    currentIndex++;
                }
                while (carry != 0L) {
                    long old = out[currentIndex];
                    long sum = old + carry;
                    out[currentIndex] = sum;
                    carry = Long.compareUnsigned(sum, old) < 0 ? 1L : 0L;
                    currentIndex++;
                }
            }
            return trimLeadingZeros(out);
        }else if (min < T3T){
            /*
             * Karatsuba multiplication is a specialized algorithm that uses recursion to "split"
             * large limbs into two smaller parts and operate on the parts. Unlike standard grade-school
             * multiply(which uses 4 multiplications), Karatsuba only needs to do 3 multiplications(and several additions)
             * Karatsuba runs in time complexity O(n^(log_2(3)) ≈ O(n^1.585)
             */
            int pivot = (max+1)/2;
            long[] lower1 = sliceFixed(a, 0, pivot);
            long[] upper1 = sliceFixed(a, pivot, pivot);
            long[] lower2 = sliceFixed(b, 0, pivot);
            long[] upper2 = sliceFixed(b, pivot, pivot);
            long[] z0 = multiplyMagnitudesWithThresholds(lower1, lower2, KT, T3T);
            long[] z2 = multiplyMagnitudesWithThresholds(upper1, upper2, KT, T3T);
            long[] z1 = multiplyMagnitudesWithThresholds(addMagnitudes(lower1, upper1), addMagnitudes(lower2, upper2), KT, T3T);
            subtractMagnitudeInPlace(z1, z0);
            subtractMagnitudeInPlace(z1, z2);
            long[] out = new long[max+min+1];
            addMagnitudeInPlace(out, z0, 0);
            addMagnitudeInPlace(out, z1, pivot);
            addMagnitudeInPlace(out, z2, pivot*2);
            return trimLeadingZeros(out);
        }
        //Toom-Cook 3 algorithm. a SignedMag helper will be needed because negative intermediate values can occur
        int pivot = (max + 2) / 3;
        //define constants from both the polynomials
        long[] a0 = sliceFixed(a, 0, pivot);
        long[] a1 = sliceFixed(a, pivot, pivot);
        long[] a2 = sliceFixed(a, 2 * pivot, pivot);
        long[] b0 = sliceFixed(b, 0, pivot);
        long[] b1 = sliceFixed(b, pivot, pivot);
        long[] b2 = sliceFixed(b, 2 * pivot, pivot);
        //evaluate each point
        long[] p1 = addThreeMagnitudes(a0, a1, a2);
        SignedMag pm1 = SignedMag.positive(addMagnitudes(a0, a2)).add(SignedMag.negative(a1));
        long[] p2 = addThreeMagnitudes(a0, shiftLeftBits(a1, 1), shiftLeftBits(a2, 2));
        long[] q1 = addThreeMagnitudes(b0, b1, b2);
        SignedMag qm1 = SignedMag.positive(addMagnitudes(b0, b2)).add(SignedMag.negative(b1));
        long[] q2 = addThreeMagnitudes(b0, shiftLeftBits(b1, 1), shiftLeftBits(b2, 2));
        //multiply
        long[] v0 = multiplyMagnitudesWithThresholds(a0, b0, KT, T3T);
        long[] v1 = multiplyMagnitudesWithThresholds(p1, q1, KT, T3T);
        SignedMag vm1 = new SignedMag(multiplyMagnitudesWithThresholds(pm1.mag, qm1.mag, KT, T3T), pm1.negative ^ qm1.negative);
        long[] v2 = multiplyMagnitudesWithThresholds(p2, q2, KT, T3T);
        long[] vInf = multiplyMagnitudesWithThresholds(a2, b2, KT, T3T);
        //interpolate
        SignedMag r2 = SignedMag.positive(v1).add(vm1).shiftRightOneBit().add(SignedMag.negative(addMagnitudes(v0, vInf)));
        SignedMag s = SignedMag.positive(v1).add(vm1.negate()).shiftRightOneBit();
        SignedMag T = SignedMag.positive(v2).add(SignedMag.negative(addMagnitudes(shiftLeftBits(vInf, 4), v0))).shiftRightOneBit();
        SignedMag r3 = T.add(s.negate()).add(SignedMag.negative(shiftLeftBits(r2.mag, 1)));
        divideMagExactInPlace(r3.mag, 3);
        SignedMag r1 = s.add(r3.negate());
        //final coefficients
        long[] R1 = r1.toPositiveMagnitudeChecked();
        long[] R2 = r2.toPositiveMagnitudeChecked();
        long[] R3 = r3.toPositiveMagnitudeChecked();
        long[] product = new long[max + min + 1];
        addMagnitudeInPlace(product, v0,   0);
        addMagnitudeInPlace(product, R1,   pivot);
        addMagnitudeInPlace(product, R2,   2 * pivot);
        addMagnitudeInPlace(product, R3,   3 * pivot);
        addMagnitudeInPlace(product, vInf, 4 * pivot);
        return trimLeadingZeros(product);
    }
    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        if (other instanceof LargeInteger I)
            return new LargeInteger(multiplyMagnitudes(limbs, I.limbs), negative != I.negative);
        else if (other instanceof LargeFraction F)
            return new LargeFraction((LargeInteger) multiply(F.numerator), F.denominator);
        else if (other instanceof LargePowerMonomial)
            return new LargePowerMonomial(new LargeFraction(this, ONE), LargeFraction.ONE).multiply(other);
        else if (other instanceof RadicalTermSum RT) {
            LargeList<RadicalTerm> rt = new LargeList<>();
            for (LargePowerMonomial L : RT.sum.keys())
                rt.add(new RadicalTerm(LargeFraction.ZERO, (LargeFraction) multiply(RT.sum.get(L)), L));
            return new RadicalTermSum(rt, (LargeFraction) multiply(RT.constant));
        } else if (other instanceof LargePowerNode LPN)
            return new LargePowerNode(LPN.base.multiply(power(LPN.exponent.reciprocal())), LPN.exponent, LPN.signFactor);
        return other.multiply(this);
    }

    public ScalarAtom multiply(ScalarAtom... atoms) {
        ScalarAtom result = this;
        for (ScalarAtom atom : atoms) result = result.multiply(atom);
        return result;
    }
    public LargeInteger multiply(long greg) {return new LargeInteger(multiplySmallMag(limbs, Math.abs(greg)), negative != (greg < 0));}
    private static void addMagnitudeInPlace(long[] out, long[] x, int offset) {
        long carry = 0L;
        for (int i = 0; i < x.length; i++) {
            LongPair s = LongPair.add64(out[offset + i], x[i]).addLow(carry);
            out[offset + i] = s.low;
            carry = s.high;
        }
        int k = offset + x.length;
        while (carry != 0L) {
            LongPair s = LongPair.add64(out[k], carry);
            out[k] = s.low;
            carry = s.high;
            k++;
        }
    }
    private static void subtractMagnitudeInPlace(long[] a, long[] b) {
        if (compareArrayUntrimmed(a, b) < 0)
            throw new IllegalArgumentException("subtractMagnitudeInPlace requires a >= b");
        int bLen = b.length;
        while (bLen > 0 && b[bLen - 1] == 0L) bLen--;
        long borrow = 0L;
        int i = 0;
        for (; i < bLen; i++) {
            long ai = a[i];
            long bi = b[i];

            long t = ai - bi;
            long b1 = Long.compareUnsigned(ai, bi) < 0 ? 1L : 0L;

            long u = t - borrow;
            long b2 = Long.compareUnsigned(t, borrow) < 0 ? 1L : 0L;

            a[i] = u;
            borrow = b1 | b2;
        }
        while (borrow != 0L) {
            long ai = a[i];
            a[i] = ai - 1L;
            borrow = ai == 0L ? 1L : 0L;
            i++;
        }
    }
    private static final int KARATSUBA_SQUARE_THRESHOLD = 48;
    private static final int TOOM_SQUARE_THRESHOLD = 128;
    private static long[] squareMagnitude(long[] a){return squareMagnitudeWithThresholds(a, KARATSUBA_SQUARE_THRESHOLD, TOOM_SQUARE_THRESHOLD);}
    public static long[] squareMagnitudeWithThresholds(long[] a, int KT, int T3T) {
        if (isZeroArray(a)) return a;
        int n = a.length;
        if (n == 1) {
            long a0 = a[0];
            long low = a0*a0;
            long high = Math.unsignedMultiplyHigh(a0, a0);
            if(high == 0) return new long[]{low};
            return new long[]{low, high};
        }
        if (n < KT) {
            long[] out = new long[n << 1];
            // Store floor(D / 2), where
            // D = Σ a[i]^2 * B^(2i)
            long shiftCarry = 0L;
            for (int i = n - 1; i >= 0; i--) {
                long ai = a[i];
                long low = ai * ai;
                long high = Math.unsignedMultiplyHigh(ai, ai);
                int k = i << 1;
                out[k + 1] = (high >>> 1) | shiftCarry;
                shiftCarry = high << 63;
                out[k] = (low >>> 1) | shiftCarry;
                shiftCarry = low << 63;
            }
            // Add O = Σ(i<j) a_i*a_j
            for (int i = 0; i < n; i++) {
                long ai = a[i];
                long carry = 0L;
                for (int j = i + 1; j < n; j++) {
                    int k = i + j;
                    long aj = a[j];
                    long low = ai * aj;
                    long high = Math.unsignedMultiplyHigh(ai, aj);
                    long t = low + out[k];
                    if (Long.compareUnsigned(t, low) < 0) high++;
                    long lo = t + carry;
                    if (Long.compareUnsigned(lo, t) < 0) high++;
                    out[k] = lo;
                    carry = high;
                }
                // Flush row carry into the next limb.
                int k = i + n;
                while (carry != 0L) {
                    long old = out[k];
                    long sum = old + carry;
                    out[k] = sum;
                    carry = Long.compareUnsigned(sum, old) < 0 ? 1L : 0L;
                    k++;
                }
            }
            // Multiply everything by 2.
            long bitCarry = 0L;
            for (int i = 0; i < out.length; i++) {
                long x = out[i];
                out[i] = (x << 1) | bitCarry;
                bitCarry = x >>> 63;
            }
            // Restore the bit lost by floor(D/2).
            out[0] |= a[0] & 1L;
            return trimLeadingZeros(out);
        } else if (n < T3T){
            int m = (n+1) >>> 1;
            long[] x0 = slice(a, 0, m);
            long[] x1 = slice(a, m, n);
            long[] z0 = squareMagnitudeWithThresholds(x0, KT, T3T);
            long[] z2 = squareMagnitudeWithThresholds(x1, KT, T3T);
            long[] z1 = squareMagnitudeWithThresholds(addMagnitudes(x0, x1), KT, T3T);
            subtractMagnitudeInPlace(z1, z0);
            subtractMagnitudeInPlace(z1, z2);
            long[] out = new long[2 * n];
            addMagnitudeInPlace(out, z0, 0);
            addMagnitudeInPlace(out, z1, m);
            addMagnitudeInPlace(out, z2, 2 * m);
            return trimLeadingZeros(out);
        }
        int m = (n + 2) / 3;
        //a = a2*B^2 + a1*B + a0
        long[] a0 = slice(a, 0, m);
        long[] a1 = slice(a, m, m * 2);
        long[] a2 = slice(a, m * 2, n);
        //evaluate points
        long[] p1 = addThreeMagnitudes(a2, a1, a0);
        long[] pm1 = addMagnitudes(a2, a0);
        if(compareArray(pm1, a1) >= 0)
            subtractMagnitudeInPlace(pm1, a1);
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
        shiftRightOneBitInPlace(s);
        // v1 -> r2
        subtractMagnitudeInPlace(v1, s);
        subtractMagnitudeInPlace(v1, v0);
        subtractMagnitudeInPlace(v1, vInf);
        // v2 -> t
        subtractMagnitudeInPlace(v2, v0);
        subtractShiftedLeftBitsInPlace(v2, vInf, 4);
        shiftRightOneBitInPlace(v2);

        // t -> r3
        subtractMagnitudeInPlace(v2, s);
        subtractShiftedLeftBitsInPlace(v2, v1, 1);
        divideMagExactInPlace(v2, 3);
        // s -> r1
        subtractMagnitudeInPlace(s, v2);
        long[] out = new long[n * 2];
        addMagnitudeInPlace(out, v0,   0);
        addMagnitudeInPlace(out, s,   m);
        addMagnitudeInPlace(out, v1, 2*m);
        addMagnitudeInPlace(out, v2, 3*m);
        addMagnitudeInPlace(out, vInf,4*m);
        return trimLeadingZeros(out);
    }
    private static long[] cubeMagnitude(long[] x) {return multiplyMagnitudes(squareMagnitude(x), x);}
    @Override
    public LargeInteger square() {return new LargeInteger(squareMagnitude(limbs), false);}
    private static long[] sqrtFloorMag(long[] n) {
        n = trimLeadingZeros(n);
        if (isZeroArray(n)) return ZERO_MAG;
        long bits = bitLength(n);
        int pairs = (int) (bits + 1) >>> 1;
        long[] root = ZERO_MAG;
        long[] rem = ZERO_MAG;

        for (int pair = pairs - 1; pair >= 0; pair--) {
            // rem = rem * 4 + next TWO bits of n
            rem = shiftLeftBits(rem, 2);
            int lowBit = pair << 1;
            int value = 0;
            if (testBit(n, lowBit)) value |= 1;
            if (testBit(n, lowBit + 1)) value |= 2;
            if (value != 0) rem = addSmallMag(rem, value);
            // candidate = 4 * root + 1
            long[] candidate = addSmallMag(shiftLeftBits(root, 2), 1);
            if (compareArray(rem, candidate) >= 0) {
                rem = subtractMagnitudes(rem, candidate);
                root = addSmallMag(shiftLeftBits(root, 1), 1);
            }else root = shiftLeftBits(root, 1);
        }
        return trimLeadingZeros(root);
    }
    private static long[] sqrtCeilMag(long[] mag) {
        long[] C = sqrtFloorMag(mag);
        if (equalsArray(squareMagnitude(C), mag)) return C;
        return addSmallMag(C, 1);
    }
    private static long[] nthRootFloor(long[] n, long exp) {
        if (exp <= 0)
            throw new IllegalArgumentException("exp must be positive");
        if (isZeroArray(n) || isOneArray(n) || exp == 1)
            return trimLeadingZeros(n);
        long rootBits = (bitLength(n) + exp - 1) / exp;
        long[] low = ONE_MAG;
        long[] high = shiftLeftBits(ONE_MAG, rootBits); // exclusive-ish upper bound
        while (compareArray(low, high) < 0) {
            long[] mid = shiftRightBits(addMagnitudes(low, high), 1);
            if (isZeroArray(mid)) mid = ONE_MAG;
            int cmp = comparePowTo(mid, exp, n);
            if (cmp <= 0) low = addSmallMag(mid, 1);
            else high = mid;
        }
        return subtractSmallMag(low, 1);
    }
    private static int comparePowTo(long[] base, long exp, long[] limit) {
        long[] result = ONE_MAG;
        for (int i = 0; i < exp; i++) {
            result = multiplyMagnitudes(result, base);
            if (compareArray(result, limit) > 0)
                return 1;
        }
        return compareArray(result, limit);
    }
    private static boolean isPerfectSquare(long[] limbs) {return equalsArray(limbs, squareMagnitude(sqrtFloorMag(limbs)));}
    public LargeInteger cube() {return new LargeInteger(cubeMagnitude(limbs), negative);}
    public static LargeInteger productRange(LargeInteger left, LargeInteger right) {
        if (left.equals(right)) return left;
        if (right.subtract(left).equals(ONE)) return (LargeInteger) left.multiply(right);
        LargeInteger mid = ((LargeInteger) left.add(right)).longDivision(TWO);
        return (LargeInteger) productRange(left, mid).multiply(productRange(mid.add(1), right));
    }
    public LargeInteger factorial() {
        if (negative) throw new ArithmeticException("Error: Factorial undefined for negative integers");
        if (isZeroArray(limbs) || isOneArray(limbs)) return ONE;
        return productRange(ONE, this);
    }
    public LargeInteger nCr(LargeInteger l) {
        if (lessThan(l)) return ZERO;
        else if (isZeroArray(l.limbs)) return ONE;
        return productRange((LargeInteger) subtract(l).add(ONE), this).longDivision(l.factorial());
    }
    public LargeFraction nPr(LargeInteger l) {
        LargeInteger temp = (LargeInteger) subtract(l);
        if (temp.negative) return LargeFraction.ZERO;
        return (LargeFraction) factorial().divide(temp.factorial());
    }
    public LargeInteger triangular() {return (LargeInteger) (isEven() ? longDivision(TWO).multiply(add(1)) : multiply(add(1).longDivision(TWO)));}
    public LargeInteger tetrahedral() {return add(2).nCr(THREE);}
    public static LargeInteger figurate(LargeInteger l, LargeInteger k) {return ((LargeInteger) k.subtract(1).add(l)).nCr(k);}
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

            if (Long.compareUnsigned(qTimesB2, right) <= 0)
                break;

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
        a = trimLeadingZeros(a);
        long[] q = new long[a.length];
        long rem = 0L;
        for (int i = a.length - 1; i >= 0; i--) {
            DivRemLongPair dr = new LongPair(rem, a[i]).divideByLong(divisor);
            if (dr.quotient.high != 0L)
                throw new ArithmeticException("quotient overflow");
            q[i] = dr.quotient.low;
            rem = dr.remainder;
        }
        return new DivRem(trimLeadingZeros(q), rem);
    }
    private static void divideMagExactInPlace(long[] a, long divisor) {
        if (divisor == 0L)
            throw new ArithmeticException("divide by zero");
        // Treat divisor as unsigned.
        // divisor = 2^shift * odd
        int shift = Long.numberOfTrailingZeros(divisor);
        if (shift != 0) {
            // Exact divisibility by 2^shift requires these bits to be zero.
            long mask = (1L << shift) - 1L;
            if ((a[0] & mask) != 0L)
                throw new ArithmeticException("not exactly divisible");
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
            throw new ArithmeticException("not exactly divisible");
    }
    private record DivRem(long[] quotient, long remainder) {}
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
        if (divisor.length <= BURNIKEL_ZIEGLER_THRESHOLD || (long)divisor.length*(long)BURNIKEL_ZIEGLER_RATIO < dividend.length || dividend.length-divisor.length < BURNIKEL_ZIEGLER_OFFSET)
            results = divideMagnitudesKnuth(dividend, divisor);
        else results = divideMagnitudesBurnikelZiegler(dividend, divisor);
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
            DivRem dr = divideByLong(dividend, divisor[0]);
            return new long[][]{dr.quotient, new long[]{dr.remainder}};
        }
        int n = divisor.length;
        int m = dividend.length - n;
        int shift = Long.numberOfLeadingZeros(divisor[n - 1]);
        long[] v = shiftLeftBits(divisor, shift);
        long[] u0 = shiftLeftBits(dividend, shift);
        if (v.length != n)
            throw new AssertionError("Knuth normalization changed divisor width: " + n + " -> " + v.length);
        // Knuth needs one extra top limb.
        long[] u = new long[u0.length + 1];
        System.arraycopy(u0, 0, u, 0, u0.length);
        long[] q = new long[m + 1];
        for (int j = m; j >= 0; j--) {
            long ujn = u[j + n];
            long ujn1 = u[j + n - 1];
            long qhat;
            long rHat;
            boolean rHatOverflow = false;
            int topCmp = Long.compareUnsigned(ujn, v[n - 1]);
            if (topCmp > 0)
                throw new AssertionError("Knuth invariant broken: u[j+n] > v[n-1], j=" + j);
            // estimate
            if (Long.compareUnsigned(ujn, v[n - 1]) == 0) {
                qhat = -1L;
                LongPair s = LongPair.add64(ujn1, v[n - 1]);
                rHat = s.low;
                rHatOverflow = s.high != 0L;
            } else {
                DivRemLongPair dr = new LongPair(ujn, ujn1).divideByLong(v[n - 1]);
                if (dr.quotient().high != 0L)
                    throw new ArithmeticException("qhat overflow");
                qhat = dr.quotient.low;
                rHat = dr.remainder;
            }
            // second-limb correction
            while (!rHatOverflow) {
                LongPair left = LongPair.multiply64(qhat, v[n - 2]);
                LongPair right = new LongPair(rHat, u[j + n - 2]);
                if (left.compareTo(right) <= 0) break;
                qhat--;
                LongPair newR = LongPair.add64(rHat, v[n - 1]);
                rHat = newR.low;
                rHatOverflow = newR.high != 0L;
            }
            boolean negative = subtractMulLimbAt(u, v, qhat, j);
            if (negative) {
                qhat--;
                long carry = 0L;
                for (int i = 0; i < v.length; i++) {
                    LongPair s = LongPair.add64(u[j + i], v[i]).addLow(carry);
                    u[j + i] = s.low;
                    carry = s.high;
                }
                int k = j + v.length;
                LongPair s = LongPair.add64(u[k], carry);
                u[k] = s.low;
            }
            q[j] = qhat;
        }
        long[] r = new long[n];
        System.arraycopy(u, 0, r, 0, n);
        r = shiftRightBits(r, shift);
        q = trimLeadingZeros(q);
        return new long[][]{q, r};
    }
    private static final int BURNIKEL_ZIEGLER_THRESHOLD = 128;
    private static final int BURNIKEL_ZIEGLER_RATIO = 2;
    private static final int BURNIKEL_ZIEGLER_OFFSET = 80;
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
        int groups = (int) (((long) divisor.length + BURNIKEL_ZIEGLER_THRESHOLD - 1) / BURNIKEL_ZIEGLER_THRESHOLD);
        int granularity = 1;
        while (granularity < groups) granularity <<= 1;
        int n = (int) ((((long) divisor.length + granularity - 1) / granularity) * granularity);
        /*
         * Normalize divisor to exactly n limbs with its highest bit set.
         *
         * beta = 2^64
         *
         * sigma = 64*n - bitLength(B)
         *
         * Multiplying both operands by 2^sigma leaves the quotient unchanged:
         *
         *     (A << sigma) / (B << sigma) = A / B
         *
         * while the remainder is shifted by sigma.
         */
        long sigma = ((long) n << 6) - bitLength(divisor);
        long[] B = shiftLeftBits(divisor, sigma);
        long[] A = shiftLeftBits(dividend, sigma);
        /*
         * B should now have exactly n significant limbs.
         */
        if (B.length != n)
            throw new AssertionError("BZ normalization produced " + B.length + " divisor limbs; expected " + n);
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
            long[] newR = qr[1];
            // Fundamental block invariant:
            // current == qBlock * B + newR
            long[] rebuild = addMagnitudes(multiplyMagnitudes(qBlock, B), newR);
            if (!equalsArray(rebuild, current))
                throw new AssertionError("BZ block reconstruction failed, block=" + block + ", qLen=" + qBlock.length + ", rLen=" + newR.length);
            if (compareArray(newR, B) >= 0)
                throw new AssertionError("BZ remainder >= divisor at block=" + block);
            if (qBlock.length > n)
                throw new AssertionError("BZ quotient block overflow at block=" + block + ", qLen=" + qBlock.length + ", n=" + n);
            remainder = newR;
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
        remainder = shiftRightBits(remainder, sigma);
        return new long[][]{trimLeadingZeros(quotient), trimLeadingZeros(remainder)};
    }
    private static long[] sliceFixed(long[] a, int from, int length) {
        if (from < 0 || length < 0)
            throw new IllegalArgumentException("Negative slice");
        long[] out = new long[length];

        if (from >= a.length)
            return out;

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
        if (n <= BURNIKEL_ZIEGLER_THRESHOLD || (n & 1) != 0)
            return divideMagnitudesKnuth(A, B);
        int m = n >>> 1;
        // A = A3 β^(3m) + A2 β^(2m) + A1 β^m + A0
        long[] A0 = sliceFixed(A, 0,     m);
        long[] A1 = sliceFixed(A, m,     m);
        long[] A2 = sliceFixed(A, 2 * m, m);
        long[] A3 = sliceFixed(A, 3 * m, m);
        /*
         * First: [A3 A2 A1] / B
         * Little endian storage: [A1, A2, A3]
         */
        long[] upper = new long[3 * m];
        System.arraycopy(A1, 0, upper, 0,     m);
        System.arraycopy(A2, 0, upper, m,     m);
        System.arraycopy(A3, 0, upper, 2 * m, m);
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
        System.arraycopy(A0, 0, lower, 0, m);
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
    private static long[][] divide3nBy2n(long[] dividend, long[] divisor) {
        int cmp = compareArray(dividend, divisor);
        if (cmp < 0)
            return new long[][]{ZERO_MAG, trimLeadingZeros(dividend)};
        if (cmp == 0)
            return new long[][]{ONE_MAG, ZERO_MAG};
        int n = divisor.length;
        if (n <= BURNIKEL_ZIEGLER_THRESHOLD)
            return divideMagnitudesKnuth(dividend, divisor);

        int m = n >>> 1;

        long[] A0 = sliceFixed(dividend, 0,     m);
        long[] A1 = sliceFixed(dividend, m,     m);
        long[] A2 = sliceFixed(dividend, 2 * m, m);

        long[] B0 = sliceFixed(divisor, 0, m);
        long[] B1 = sliceFixed(divisor, m, m);

        long[] A12 = new long[2 * m];
        System.arraycopy(A1, 0, A12, 0, m);
        System.arraycopy(A2, 0, A12, m, m);

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
            r = subtractMagnitudes(r, shiftLeftLimbs(B1, m));
            if (isZeroArray(B0)) d = ZERO_MAG;
            else d = subtractMagnitudes(shiftLeftLimbs(B0, m), B0);
        }
        long[] rFull = addMagnitudes(shiftLeftLimbs(r, m), A0);
        int corrections = 0;
        while (compareArray(rFull, d) < 0) {
            rFull = addMagnitudes(rFull, divisor);
            q = subtractMagnitudes(q, ONE_MAG);
            if (++corrections > 2)
                throw new AssertionError("BZ divide3nBy2n required too many corrections");
        }
        rFull = subtractMagnitudes(rFull, d);
        return new long[][]{trimLeadingZeros(q), trimLeadingZeros(rFull)};
    }
    private static long[] divideExactMag(long[] n, long[] d) {
        long[][] qr = divideMagnitudes(n, d);
        if (!isZeroArray(qr[1]))
            throw new ArithmeticException("d did not divide exactly");
        return qr[0];
    }
    public LargeInteger[] quotientAndRemainder(LargeInteger other) {
        long[][] qr = divideMagnitudes(limbs, other.limbs);
        LargeInteger q = new LargeInteger(qr[0], negative != other.negative);
        LargeInteger r = new LargeInteger(qr[1], negative);
        return new LargeInteger[]{q, r};
    }
    public LargeInteger longDivision(LargeInteger other) {return quotientAndRemainder(other)[0];}
    @Override
    public LargeFraction reciprocal() {return new LargeFraction(ONE, this);}
    public LargeInteger remainder(LargeInteger other) {return quotientAndRemainder(other)[1];}
    private static long modSmall(long[] a, long d) {
        if (d == 0L)
            throw new ArithmeticException("Error: Cannot divide by 0");
        a = trimLeadingZeros(a);
        if (a.length == 1)
            return Long.remainderUnsigned(a[0], d);
        if ((d & (d - 1)) == 0) return a[0] & (d - 1);
        long r = 0L;
        for (int i = a.length - 1; i >= 0; i--)
            r = new LongPair(r, a[i]).divideByLong(d).remainder;
        return r;
    }
    private static long[] modMagnitudes(long[] dividend, long[] divisor) {
        dividend = trimLeadingZeros(dividend);
        divisor  = trimLeadingZeros(divisor);
        if(isZeroArray(divisor))
            throw new ArithmeticException("Division by zero");
        int cmp = compareArray(dividend, divisor);
        if (cmp < 0) return dividend;
        if (cmp == 0) return ZERO_MAG;
        if (divisor.length == 1)
            return new long[]{modSmall(dividend, divisor[0])};
        // Normalize divisor so its top bit is set.
        int shift = Long.numberOfLeadingZeros(divisor[divisor.length - 1]);
        long[] v = shift == 0 ? divisor : shiftLeftBits(divisor, shift);
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
            if (Long.compareUnsigned(ujn, v[n - 1]) >= 0) {
                qhat = -1L; // unsigned B - 1
                LongPair s = LongPair.add64(ujn1, v[n - 1]);
                rHat = s.low;
                rHatOverflow = s.high != 0L;
            }else{
                DivRemLongPair dr = new LongPair(ujn, ujn1).divideByLong(v[n - 1]);
                if (dr.quotient.high != 0L)
                    throw new ArithmeticException("qhat overflow");
                qhat = dr.quotient.low;
                rHat = dr.remainder;
            }
            while (!rHatOverflow) {
                LongPair left = LongPair.multiply64(qhat, v[n - 2]);
                LongPair right = new LongPair(rHat, u[j + n - 2]);
                if (left.lesserThanOrEqualTo(right)) break;
                qhat--;
                LongPair newR = LongPair.add64(rHat, v[n - 1]);
                rHat = newR.low;
                rHatOverflow = newR.high != 0L;
            }
            boolean negative = subtractMulLimbAt(u, v, qhat, j);
            if (negative) {
                long carry = 0L;
                for (int i = 0; i < v.length; i++) {
                    LongPair s = LongPair.add64(u[j + i], v[i]).addLow(carry);
                    u[j + i] = s.low;
                    carry = s.high;
                }
                int k = j + v.length; // j + n
                LongPair s = LongPair.add64(u[k], carry);
                u[k] = s.low;
            }
        }
        // Low n limbs now contain normalized remainder.
        long[] r = Arrays.copyOf(u, n);
        if (shift != 0)
            r = shiftRightBits(r, shift);
        return trimLeadingZeros(r);
    }
    private static long[] modAdd(long[] a, long[] b, long[] m) {
        if (compareArray(a, m) >= 0)
            a = modMagnitudes(a, m);
        if (compareArray(b, m) >= 0)
            b = modMagnitudes(b, m);
        long[] sum = addMagnitudes(a, b);
        if(compareArray(sum, m) >= 0)
            subtractMagnitudeInPlace(sum, m);
        return sum;
    }
    private static long[] modSubtract(long[] a, long[] b, long[] m) {
        boolean aReduced = compareArray(a, m) < 0;
        boolean bReduced = compareArray(b, m) < 0;
        if (aReduced && bReduced) {
            if (compareArray(a, b) >= 0)
                return subtractMagnitudes(a, b);
            return subtractMagnitudes(m, subtractMagnitudes(b, a));
        }
        a = aReduced ? a : modMagnitudes(a, m);
        b = bReduced ? b : modMagnitudes(b, m);
        if (compareArray(a, b) >= 0)
            return subtractMagnitudes(a, b);
        return subtractMagnitudes(m, subtractMagnitudes(b, a));
    }
    private static long[] multiplyMod(long[] a, long[] b, long[] m) {return modMagnitudes(multiplyMagnitudes(a, b), m);}
    @Override
    public ScalarAtom mod(ScalarAtom other) {
        if (other instanceof LargeInteger modulus) {
            LargeInteger r = remainder(modulus);
            if (isZeroArray(r.limbs)) return ZERO;
            else if (r.negative != modulus.negative)
                return r.add(modulus);
            return r;
        }
        return mod(other);
    }
    public long mod(long modulus){
        if(modulus < 0){
            long mod = mod(-modulus);
            return mod == 0 ? 0 : mod + modulus;
        }
        long R = modSmall(limbs, modulus);
        if(R==0) return 0;
        if(negative) return modulus-R;
        return R;
    }
    public LargeInteger modInverse(LargeInteger mod) {
        if (isZeroArray(mod.limbs))
            throw new ArithmeticException("Error: Modulus cannot be 0");
        // Computes |this|^-1 mod |mod| in [0, |mod|).
        ExtGcdMag EX = extendedGcdMagnitudes(limbs, mod.limbs);
        if (!isOneArray(EX.gcd))
            throw new ArithmeticException("Error: Modular inverse does not exist");
        long[] inverse = EX.x.modMag(mod.limbs);
        // (-a)^-1 ≡ -(a^-1) mod |mod|.
        if (negative && !isZeroArray(inverse))
            inverse = subtractMagnitudes(mod.limbs, inverse);
        // Preserve the existing negative-modulus behavior:
        // positive modulus -> [0, mod)
        // negative modulus -> (mod, 0]
        if (mod.negative && !isZeroArray(inverse))
            return new LargeInteger(subtractMagnitudes(mod.limbs, inverse), true);
        return new LargeInteger(inverse, false);
    }
    private static boolean isEvenMag(long[] x) {return (x[0] & 1L) == 0L;}
    public boolean isEven() {return isEvenMag(limbs);}
    public boolean isOdd() {return (limbs[0] & 1L) == 1L;}
    public LargeFraction power(long p) {
        if (p < 0) return new LargeInteger(power(limbs, -p), p % 2 != 0 && negative).reciprocal();
        return new LargeFraction(new LargeInteger(power(limbs, p), p % 2 != 0 && negative), ONE);
    }
    private static long[] power(long[] mag, long power) {
        if (power < 0) throw new IllegalArgumentException("Error: Positive power is required");
        if (power == 0) return ONE_MAG;
        long expTemp = power;
        long[] basTemp = mag;
        long[] result = ONE_MAG;
        while (expTemp > 0) {
            if ((expTemp & 1L) == 1L)
                result = multiplyMagnitudes(result, basTemp);
            basTemp = squareMagnitude(basTemp);
            expTemp >>>= 1;
        }
        return result;
    }
    public LargeFraction power(LargeInteger other) {
        if (equals(ONE)) return LargeFraction.ONE;
        else if (isZeroArray(other.limbs)) {
            if (isZeroArray(limbs))
                throw new ArithmeticException("Error: Indeterminate form: 0^0");
            return LargeFraction.ONE;
        }
        LargeFraction result = LargeFraction.ONE;
        LargeInteger expTemp = other.abs();
        LargeInteger basTemp = copy();
        while (true) {
            if (expTemp.isOdd()) {
                result = (LargeFraction) result.multiply(basTemp);
                if (expTemp.equals(ONE)) break;
            }
            basTemp = basTemp.square();
            expTemp = expTemp.longDivision(TWO);
        }
        if (other.negative) return result.reciprocal();
        return result;
    }
    private static final class BarrettReducer {
        private final long[] modulus;
        private final long[] mu;
        private final int k;
        private final long[] bToKPlusOne;
        BarrettReducer(long[] modulus) {
            modulus = trimLeadingZeros(modulus);
            if (isZeroArray(modulus))
                throw new ArithmeticException("modulus is zero");
            this.modulus = modulus;
            k = modulus.length;
            mu = divideMagnitudes(basePower(2 * k).limbs, modulus)[0];
            bToKPlusOne = basePower(k + 1).limbs;
        }
        long[] reduce(long[] x) {
            x = trimLeadingZeros(x);
            if (compareArray(x, modulus) < 0)
                return x;
            if (x.length <= 2 * k){
                long[] q1 = shiftRightLimbs(x, k - 1);
                long[] q2 = multiplyMagnitudes(q1, mu);
                long[] q3 = shiftRightLimbs(q2, k + 1);
                long[] q3m = multiplyMagnitudes(q3, modulus);

                long[] r1 = slice(x, 0, k + 1);
                long[] r2 = slice(q3m, 0, k + 1);

                SignedMag m = SignedMag.positive(modulus);
                SignedMag r = SignedMag.positive(r1).add(SignedMag.negative(r2));
                if (r.negative) {
                    r = r.add(SignedMag.positive(bToKPlusOne));
                    if (r.negative) throw new AssertionError("Reduction failed");
                }
                while (r.compareTo(m) >= 0) r = r.add(m.negate());
                return r.toPositiveMagnitudeChecked();
            }
            return modMagnitudes(x, modulus);
        }
        private long[] multiply(long[] a, long[] b) {return reduce(multiplyMagnitudes(a, b));}
        public long[] square(long[] a) {return reduce(squareMagnitude(a));}
        private long[] powModBarrett(long[] base, long[] exponent) {
            if (isZeroArray(exponent)) return reduce(ONE_MAG);
            base = reduce(base);
            int window = MontgomeryReducer.bestWindowSize(exponent);
            int tableSize = 1 << (window - 1);
            long[][] oddPowers = new long[tableSize][];
            // oddPowers[I] = base^(2*I + 1)
            oddPowers[0] = base;
            long[] base2 = square(base);
            for (int i = 1; i < tableSize; i++)
                oddPowers[i] = multiply(oddPowers[i - 1], base2);
            long[] result = reduce(ONE_MAG);
            long bit = bitLength(exponent) - 1;
            while (bit >= 0) {
                if (!testBit(exponent, bit)) {
                    result = square(result);
                    bit--;
                    continue;
                }
                int width = Math.toIntExact(Math.min(window, bit + 1));
                // shrink width until the low bit of the window is 1
                while (width > 1 && !testBit(exponent, bit - width + 1)) width--;
                int value = 0;
                for (int i = 0; i < width; i++) {
                    value <<= 1;
                    if (testBit(exponent, bit - i))
                        value |= 1;
                }
                for (int i = 0; i < width; i++) result = square(result);
                result = multiply(result, oddPowers[value >>> 1]);
                bit -= width;
            }
            return result;
        }
    }
    private static final class MontgomeryReducer {
        private final long[] modulus;
        private final int k;
        private final long mPrime;
        private final long[] rModM;
        private final long[] rTimesModulus;
        private final long[] r2ModM;
        private final boolean useCIOS;
        private final long[] scratch;

        MontgomeryReducer(long[] inputModulus) {
            if (inputModulus == null)
                throw new NullPointerException("modulus is null");
            if (inputModulus.length == 0)
                throw new IllegalArgumentException("modulus has an empty abs");
            long[] modulus = trimLeadingZeros(inputModulus).clone();
            if (isZeroArray(modulus))
                throw new ArithmeticException("modulus is zero");
            if (isEvenMag(modulus))
                throw new ArithmeticException("Montgomery modulus must be odd");
            this.modulus = modulus;
            this.k = modulus.length;
            long x = modulus[0];
            long y = 2L * x - x * x * x;
            for (int i = 0; i < 4; i++) y *= 2L - x * y;
            this.mPrime = -y;
            this.useCIOS = k <= 4;
            this.scratch = new long[2 * k + 2];
            long[] power = new long[2 * k + 1];
            power[k] = 1L;
            this.rModM = modMagnitudes(power, modulus);
            power[k] = 0L;
            power[2 * k] = 1L;
            this.r2ModM = modMagnitudes(power, modulus);
            this.rTimesModulus = shiftLeftLimbs(modulus, k);
        }
        long[] reduce(long[] product) {
            product = trimLeadingZeros(product);
            // REDC's bounded fast path requires product < R * modulus.
            if (product.length > 2 * k || compareArray(product, rTimesModulus) >= 0)
                product = modMagnitudes(product, rTimesModulus);
            Arrays.fill(scratch, 0L);
            System.arraycopy(product, 0, scratch, 0, product.length);
            reduceScratchInPlace();
            return copyReducedResult();
        }
        private void reduceScratchInPlace() {
            for (int i = 0; i < k; i++) {
                long u = scratch[i] * mPrime;
                long carry = 0L;
                for (int j = 0; j < modulus.length; j++) {
                    LongPair p = LongPair.multiply64(modulus[j], u).addLow(scratch[i + j]).addLow(carry);
                    scratch[i + j] = p.low;
                    carry = p.high;
                }
                int k = i + modulus.length;
                while (carry != 0L) {
                    LongPair s = LongPair.add64(scratch[k], carry);
                    scratch[k] = s.low;
                    carry = s.high;
                    k++;
                }
            }
        }
        private long[] copyReducedResult() {
            long[] result = new long[k + 1];
            System.arraycopy(scratch, k, result, 0, k + 1);
            result = trimLeadingZeros(result);
            if (compareArray(result, modulus) >= 0)
                result = subtractMagnitudes(result, modulus);
            return result;
        }
        private void requireReducedMagnitude(long[] x, String argumentName) {
            if (x == null)
                throw new NullPointerException(argumentName + " is null");
            if (x.length == 0)
                throw new IllegalArgumentException(argumentName + " has an empty abs");
            if (x.length > 1 && x[x.length - 1] == 0L)
                throw new IllegalArgumentException(argumentName + " is not canonically normalized");
            if (compareArray(x, modulus) >= 0)
                throw new IllegalArgumentException(argumentName + " is not reduced modulo the supplied modulus");
        }
        long[] add(long[] a, long[] b) {
            requireReducedMagnitude(a, "a");
            requireReducedMagnitude(b, "b");
            return addCanonical(a, b);
        }
        long[] addCanonical(long[] a, long[] b) {
            long[] sum = addMagnitudes(a, b);
            return compareArray(sum, modulus) >= 0 ? subtractMagnitudes(sum, modulus) : sum;
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

        long[] multiply(long[] a, long[] b) {
            if (useCIOS) return multiplyCIOS(a, b);
            if (Math.min(a.length, b.length) < KARATSUBA_THRESHOLD) {
                Arrays.fill(scratch, 0L);
                schoolbookMultiplyInto(a, b, scratch);
                reduceScratchInPlace();
                return copyReducedResult();
            }
            return reduce(multiplyMagnitudes(a, b));
        }
        private static void schoolbookMultiplyInto(long[] a, long[] b, long[] out) {
            if (a.length < b.length) {
                long[] temp = a;
                a = b;
                b = temp;
            }
            for (int i = 0; i < a.length; i++) {
                long carry = 0L;
                for (int j = 0; j < b.length; j++) {
                    int index = i + j;
                    LongPair product = LongPair.multiply64(a[i], b[j]).addLow(out[index]).addLow(carry);
                    out[index] = product.low;
                    carry = product.high;
                }
                int index = i + b.length;
                while (carry != 0L) {
                    LongPair sum = LongPair.add64(out[index], carry);
                    out[index] = sum.low;
                    carry = sum.high;
                    index++;
                }
            }
        }
        /**
         * CIOS Montgomery multiplication.
         * Preconditions:
         * 0 <= a < modulus
         * 0 <= b < modulus
         * @return a * b * R^-1 mod modulus
         * where R = 2^(64*k).
         */
        long[] multiplyCIOS(long[] a, long[] b) {
            a = trimLeadingZeros(a);
            b = trimLeadingZeros(b);
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
            long[] t = new long[k + 2];

            for (int i = 0; i < k; i++) {
                long bi = i < b.length ? b[i] : 0L;

                //t += a * bi
                long carry = 0L;

                for (int j = 0; j < a.length; j++) {
                    long low = a[j] * bi;
                    long high = Math.unsignedMultiplyHigh(a[j], bi);

                    long s1 = t[j] + low;
                    long c1 = Long.compareUnsigned(s1, t[j]) < 0 ? 1L : 0L;

                    long s2 = s1 + carry;
                    long c2 = Long.compareUnsigned(s2, s1) < 0 ? 1L : 0L;

                    t[j] = s2;

                    /*
                     * Under the multiplication invariant, this unsigned
                     * addition cannot overflow past ONE limb.
                     */
                    carry = high + c1 + c2;
                }

                /*
                 * Add the product carry above a's highest stored limb.
                 * a may contain fewer than k limbs.
                 */
                int carryIndex = a.length;
                while (carry != 0L) {
                    long old = t[carryIndex];
                    long sum = old + carry;
                    t[carryIndex] = sum;
                    carry = Long.compareUnsigned(sum, old) < 0 ? 1L : 0L;
                    carryIndex++;

                    if (carryIndex >= t.length && carry != 0L)
                        throw new AssertionError("CIOS product accumulation overflow");
                }

                //q is chosen so that t[0] + q * modulus[0] == 0 mod 2^6
                long q = t[0] * mPrime;

                /*
                 * t += q * modulus
                 * Simultaneously divide by 2^64. The value written for
                 * limb j is placed at j-1, so the canceled low limb is
                 * never copied or retained.
                 */

                for (int j = 0; j < k; j++) {
                    long low = modulus[j] * q;
                    long high = Math.unsignedMultiplyHigh(modulus[j], q);

                    long s1 = t[j] + low;
                    long c1 = Long.compareUnsigned(s1, t[j]) < 0 ? 1L : 0L;

                    long s2 = s1 + carry;
                    long c2 = Long.compareUnsigned(s2, s1) < 0 ? 1L : 0L;

                    carry = high + c1 + c2;

                    if (j == 0) {
                        //This is the fundamental Montgomery invariant.
                        if (s2 != 0L) throw new AssertionError("Montgomery cancellation failed");
                    } else t[j - 1] = s2; //Store S / 2^64 directly.
                }

                //Finish the shifted top limbs.
                long oldTop = t[k];
                long shiftedTop = oldTop + carry;
                long topCarry =
                        Long.compareUnsigned(shiftedTop, oldTop) < 0 ? 1L : 0L;

                t[k - 1] = shiftedTop;

                long oldExtra = t[k + 1];
                long newExtra = oldExtra + topCarry;

                if (Long.compareUnsigned(newExtra, oldExtra) < 0)
                    throw new AssertionError("CIOS top-limb overflow");

                t[k] = newExtra;
                t[k + 1] = 0L;
            }

            /*
             * With reduced operands, the CIOS result is in [0, 2m).
             * Therefore, at most 1 subtraction is required.
             */
            if (compareArray(t, modulus) >= 0) subtractModulusOnce(t);
            if (t[k] != 0L || compareArray(t, modulus) >= 0)
                throw new AssertionError("CIOS result was not fully reduced");
            return trimLeadingZeros(Arrays.copyOf(t, k));
        }
        private void subtractModulusOnce(long[] t) { //t - modulus
            long borrow = 0L;
            for (int i = 0; i < k; i++) {
                long ai = t[i];
                long bi = modulus[i];

                long s1 = ai - bi;
                long b1 = Long.compareUnsigned(ai, bi) < 0 ? 1L : 0L;

                long s2 = s1 - borrow;
                long b2 = Long.compareUnsigned(s1, borrow) < 0 ? 1L : 0L;
                t[i] = s2;
                borrow = b1 | b2;
            }
            long top = t[k];
            long newTop = top - borrow;
            long topBorrow = Long.compareUnsigned(top, borrow) < 0 ? 1L : 0L;
            t[k] = newTop;
            if (topBorrow != 0L)
                throw new AssertionError("Montgomery final subtraction underflow");
        }
        long[] square(long[] a) {
            if (useCIOS) return multiplyCIOS(a, a);
            if (k < KARATSUBA_THRESHOLD) {
                Arrays.fill(scratch, 0L);
                schoolbookMultiplyInto(a, a, scratch);
                reduceScratchInPlace();
                return copyReducedResult();
            }
            return reduce(squareMagnitude(a));
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
        static int bestWindowSize(long[] exponent) {
            long bestCost = 0;
            for (long l : exponent) bestCost += Long.bitCount(l);
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
        long[] powMontgomery(long[] baseMontgomery, long[] exponent) {
            if (isZeroArray(exponent)) return rModM;
            int window = bestWindowSize(exponent);
            if (window == 1) {
                long[] result = rModM;
                for (long bit = bitLength(exponent); bit-- > 0; ) {
                    result = square(result);
                    if (testBit(exponent, bit))
                        result = multiply(result, baseMontgomery);
                }
                return result;
            }
            int tableSize = 1 << (window - 1);
            long[][] oddPowers = new long[tableSize][];
            oddPowers[0] = baseMontgomery;
            long[] base2 = square(baseMontgomery);
            for (int i = 1; i < tableSize; i++) oddPowers[i] = multiply(oddPowers[i - 1], base2);
            long[] result = rModM;
            long bit = bitLength(exponent) - 1L;
            while (bit >= 0) {
                if (!testBit(exponent, bit)) {
                    result = square(result);
                    bit--;
                    continue;
                }
                long width = Math.min(window, bit + 1);
                while (width > 1 && !testBit(exponent, bit - width + 1)) width--;
                int value = 0;
                for (int i = 0; i < width; i++) {
                    value <<= 1;
                    if (testBit(exponent, bit - i)) value |= 1;
                }
                for (int i = 0; i < width; i++) result = square(result);
                result = multiply(result, oddPowers[value >> 1]);
                bit -= width;
            }
            return result;
        }
        long[] powMod(long[] base, long[] exponent) {return reduce(powMontgomery(toMontgomery(base), exponent));}
        long[] toMontgomery(long[] x) {return multiply(x, r2ModM);}
    }
    public LargeInteger powMod(LargeInteger exponent, LargeInteger modulus) {
        if (isZeroArray(modulus.limbs))
            throw new ArithmeticException("Error: Cannot mod by 0");
        if(isZeroArray(limbs) && isZeroArray(exponent.limbs))
            throw new ArithmeticException("Error: Indeterminate form: 0^0");
        if (modulus.negative) {
            LargeInteger tmp = powMod(exponent, modulus.negate());
            if (tmp.equals(ZERO)) return tmp;
            return (LargeInteger) tmp.add(modulus);
        }
        if (modulus.equals(ONE)) return ZERO;
        if (exponent.negative)
            return modInverse(modulus).powMod(exponent.negate(), modulus);
        LargeInteger base = (LargeInteger) mod(modulus);
        if (modulus.isOdd())
            return new LargeInteger(new MontgomeryReducer(modulus.limbs).powMod(base.limbs, exponent.limbs), false);
        if (modulus.equals(TWO)) return base.isOdd() ? ONE : ZERO;
        return new LargeInteger(new BarrettReducer(modulus.limbs).powModBarrett(base.limbs, exponent.limbs), false);
    }
    private static long powMod(long base, long exp, long mod) {
        long res = 1;
        while (exp > 0) {
            if ((exp & 1L) == 1L) res = res * base % mod;
            base = base * base % mod;
            exp >>= 1;
        }
        return res;
    }
    @Override
    public LargePowerMonomial power(LargeFraction f) {return new LargePowerMonomial(new LargeFraction(this, ONE), f);}
    @Override
    public LargeDecimal approximateToDecimal(LargeInteger precision) {return null;}
    public static LargeInteger powerOfTwo(long bit) {
        if (bit < 0) throw new IllegalArgumentException("Error: Negative Exponent");
        long[] out = new long[(int) (bit >>> 6) + 1];
        out[(int) (bit >>> 6)] = 1L << (bit & 63);
        return new LargeInteger(out, false);
    }
    private static long leading63(long[] mag) {
        mag = trimLeadingZeros(mag);
        if (isZeroArray(mag)) return 0L;
        int top = mag.length - 1;
        long hi = mag[top];
        int bitsInHi = 64 - Long.numberOfLeadingZeros(hi);
        if (bitsInHi >= 63) return hi >>> (bitsInHi - 63);
        int need = 63 - bitsInHi;
        long out = hi << need;
        if (top > 0) out |= mag[top - 1] >>> (64 - need);
        return out;
    }
    private static LehmerStep lehmerStep(long[] a, long[] b) {
        long aTop = leading63(a);
        long bTop = leading63(b);
        long A = 1, B = 0, C = 0, D = 1;
        long lastA = A, lastB = B, lastC = C, lastD = D;
        boolean moved = false;
        long steps = 0;
        while (bTop != 0) {
            long den1 = bTop + C;
            long den2 = bTop + D;
            if (den1 <= 0 || den2 <= 0) break;
            long q1 = (aTop + A) / den1;
            long q2 = (aTop + B) / den2;
            if (q1 != q2) break;
            if (q1 <= 0) break;
            if (mulSubWouldOverflow(A, q1, C)) break;
            else if(mulSubWouldOverflow(B, q1, D)) break;
            else if(mulSubWouldOverflow(aTop, q1, bTop)) break;
            long nextA = C;
            long nextB = D;
            long nextC = A - q1 * C;
            long nextD = B - q1 * D;
            long nextATop = bTop;
            long nextBTop = aTop - q1 * bTop;
            if (nextBTop < 0) break;
            // Validate approximate transformed pair is nonnegative.
            if (linearWouldBeNegative(nextA, aTop, nextB, bTop)) break;
            else if(linearWouldBeNegative(nextC, aTop, nextD, bTop)) break;
            A = nextA;
            B = nextB;
            C = nextC;
            D = nextD;
            aTop = nextATop;
            bTop = nextBTop;
            lastA = A;
            lastB = B;
            lastC = C;
            lastD = D;
            moved = true;
            steps++;
        }
        if (!moved) return new LehmerStep(1, 0, 0, 1, 0);
        return new LehmerStep(lastA, lastB, lastC, lastD, steps);
    }
    private static boolean linearWouldBeNegative(long a, long b, long x, long y) {return a * x + b * y < 0;}
    private static boolean mulSubWouldOverflow(long a, long q, long c) {
        LongPair qc = LongPair.multiply64(q, c);
        return new LongPair(a, 0).lessThan(qc);
    }
    private record LehmerStep(long A, long B, long C, long D, long steps){}
    private static long trailingZeroBits(long[] x) {
        x = trimLeadingZeros(x);
        if (isZeroArray(x)) return 0;
        int limbs = 0;
        while (limbs < x.length && x[limbs] == 0L) limbs++;
        return limbs * 64L + Long.numberOfTrailingZeros(x[limbs]);
    }
    private static SignedMag linCombSmall(long[] a, long ca, long[] b, long cb) {
        if (ca == 0L)
            return multiplyMagnitudeBySignedLong(b, cb);
        if (cb == 0L)
            return multiplyMagnitudeBySignedLong(a, ca);
        boolean negA = ca < 0;
        boolean negB = cb < 0;
        long[] partA = multiplySmallMag(a, Math.abs(ca));
        long[] partB = multiplySmallMag(b, Math.abs(cb));
        return new SignedMag(partA, negA).add(new SignedMag(partB, negB));
    }
    private static boolean acceptLehmerPair(long[] oldA, long[] oldB, long[] NA, long[] NB) {
        if (isZeroArray(NA)) return false;
        if (isZeroArray(NB)) return true;
        // Both outputs must be smaller than the old max operand.
        if (compareArray(NA, oldA) >= 0) return false;
        if (compareArray(NB, oldA) >= 0) return false;
        // Avoid accepting identical/no-progress transforms.
        return !equalsArray(NA, oldA) || !equalsArray(NB, oldB);
    }
    private static long[] gcdMagnitudes(long[] a, long[] b) {
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);
        if(isZeroArray(a)) return b;
        else if(isZeroArray(b)) return a;
        while (!isZeroArray(b)) {
            if (compareArray(a, b) < 0) {
                long[] t = a;
                a = b;
                b = t;
            }
            int n = b.length; // smaller operand
            if (n == 1) {
                long B = b[0];
                if (a.length == 1)
                    return new long[]{gcdLong(a[0], B)};
                long remainder = modSmall(a, B);
                return new long[]{gcdLong(remainder, B)};
            }
            else if (a.length - n <= 2){
                long commonShift = Math.min(trailingZeroBits(a), trailingZeroBits(b));
                a = shiftRightBits(a, trailingZeroBits(a));
                b = shiftRightBits(b, trailingZeroBits(b));
                while (!isZeroArray(b)) {
                    int cmp = compareArray(a, b);
                    if (cmp == 0) return shiftLeftBits(a, commonShift);
                    else if (cmp > 0) {
                        a = subtractMagnitudes(a, b);
                        a = shiftRightBits(a, trailingZeroBits(a));
                    } else {
                        b = subtractMagnitudes(b, a);
                        b = shiftRightBits(b, trailingZeroBits(b));
                    }
                    if (a.length == 1 && b.length == 1)
                        return new long[]{gcdLong(a[0], b[0])};
                }
                return shiftLeftBits(a, commonShift);
            }
            else if (a.length - n > 64 && n > 64) {
                LehmerStep lm = lehmerStep(a, b);
                if (lm.steps >= 5) {
                    SignedMag na = linCombSmall(a, lm.A, b, lm.B);
                    SignedMag nb = linCombSmall(a, lm.C, b, lm.D);
                    if (!na.negative && !nb.negative) {
                        long[] NA = na.mag;
                        long[] NB = nb.mag;
                        if (acceptLehmerPair(a, b, NA, NB)) {
                            a = NA;
                            b = NB;
                            continue;
                        }
                    }
                }
            }
            long[] div = modMagnitudes(a, b);
            a = b;
            b = div;
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
    private static ExtGcdMag extendedGcdMagnitudes(long[] a, long[] b){
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);

        long[] oldR = a;
        long[] r = b;

        SignedMag oldS = SignedMag.positive(ONE_MAG);
        SignedMag s = SignedMag.ZERO;
        SignedMag oldT = SignedMag.ZERO;
        SignedMag t = SignedMag.positive(ONE_MAG);

        while (!isZeroArray(r)) {
            long[][] qr = divideMagnitudes(oldR, r);
            long[] q = qr[0];
            long[] nextR = qr[1]; // oldR - q*r, already computed by division

            oldR = r;
            r = nextR;
            SignedMag nextS = oldS.add(s.multiplyMagnitude(q).negate());
            oldS = s;
            s = nextS;

            SignedMag nextT = oldT.add(t.multiplyMagnitude(q).negate());
            oldT = t;
            t = nextT;
        }
        return new ExtGcdMag(oldR, oldS, oldT);
    }
    public LargeInteger gcd(LargeInteger other) {return new LargeInteger(gcdMagnitudes(limbs, other.limbs), false);}
    private record ExtGcdMag(long[] gcd, SignedMag x, SignedMag y) {
        ExtGcdMag(long[] gcd, SignedMag x, SignedMag y) {
            this.gcd = trimLeadingZeros(gcd);
            this.x = x;
            this.y = y;
        }
    }
    public LargeInteger lcm(LargeInteger other) {
        LargeInteger G = gcd(other);
        return (LargeInteger) longDivision(G).multiply(other);
    }
    public LargeInteger lcm(LargeInteger... other) {
        LargeInteger copy = abs();
        for (LargeInteger I : other) copy = copy.lcm(I);
        return copy;
    }
    public static final int[] SMALL_PRIMES = sieve(1000000);
    public boolean isProbablyPrime() {return !negative && isProbablyPrimeMag(limbs);}
    private static boolean isProbablyPrimeMag(long[] n) {
        int cmp = compareArray(n, TWO_MAG);
        if (cmp <= 0) return cmp == 0;
        if (isEvenMag(n) || isPerfectSquare(n)) return false;
        for(int k: sieve(1000)) {
            if(modSmall(n, k) == 0)
                return n.length == 1 && n[0] == k;
        }
        return isProbablyPrimeSpecialMag(n);
    }
    private static boolean isProbablyPrimeSpecialMag(long[] n) {
        long[] nMinusOne = subtractSmallMag(n, 1);
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
        if (bits < 100)
            return passesMillerRabin(50, reducer, oneMont, minusOneMont, d, s, nMinusOne);
        else if (bits < 256) rounds = 27;
        else if (bits < 512) rounds = 15;
        else if (bits < 768) rounds = 8;
        else if (bits < 1024) rounds = 4;
        else rounds = 2;
        return passesMillerRabin(rounds, reducer, oneMont, minusOneMont, d, s, nMinusOne) && passesLucasSelfridge(n, reducer, oneMont);
    }
    private static boolean passesMillerRabin(int rounds, MontgomeryReducer reducer, long[] oneMont, long[] minusOneMont, long[] d, long s, long[] nMinusOne) {
        for (int round = 0; round < rounds; round++) {
            long[] witnessMont = reducer.toMontgomery(randomMagBetween(TWO_MAG, nMinusOne));
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
        long[] nPlusOne = addSmallMag(n, 1);
        long s = trailingZeroBits(nPlusOne);
        long[] d = shiftRightBits(nPlusOne, s);
        if(isOneArray(d)){
            //Lucas-Lehmer test
            long[] current = FOUR_MAG;
            for(long k = 0; k < s-2; k++)
                current = modMagnitudes(subtractSmallMag(squareMagnitude(current), 2), n);
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
    public LargeInteger nextProbablePrime() {
        if (lessThan(TWO)) return TWO;
        else if (lessThan(THREE)) return THREE;
        else if (lessThan(FIVE)) return FIVE;
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
    public static LargeInteger randomPrimeChunks(int chunks) {
        if (chunks <= 0)
            throw new IllegalArgumentException("chunks must be positive");
        int wheel = 30;
        while (true) {
            LargeInteger candidate = randomChunks(chunks, SECURE_RNG);
            int residue = WHEEL_30_RESIDUES[SECURE_RNG.nextInt(WHEEL_30_RESIDUES.length)];
            long rem = candidate.mod(wheel);
            candidate = candidate.add(residue).subtract(rem);
            if (candidate.isProbablyPrime()) return candidate;
        }
    }
    public static LargeInteger randomPrimeBits(long bits) {
        if (bits <= 1)
            throw new IllegalArgumentException("Bits <= 1 cannot produce a prime number");
        while (true) {
            long[] limbs = new long[Math.toIntExact((bits + 63) >>> 6)];
            for (int i = 0; i < limbs.length; i++)
                limbs[i] = SECURE_RNG.nextLong();
            int topBit = Math.toIntExact((bits - 1) & 63);
            if (topBit != 63) {
                long mask = (1L << (topBit + 1)) - 1L;
                limbs[limbs.length - 1] &= mask;
            }
            limbs[limbs.length - 1] |= 1L << topBit;
            limbs[0] |= 1L;
            LargeInteger candidate = new LargeInteger(limbs, false);
            if (candidate.isProbablyPrime())
                return candidate;
        }
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
    private record PowerPlusOneForm(long[] base, long exponent) {}
    private static PowerPlusOneForm detectPowerPlusOne(long[] n) {
        if (compareArray(n, TWO_MAG) <= 0)
            return null;
        long[] powerCandidate = subtractSmallMag(n, 1); // n = base^exponent + 1
        PowerPlusOneForm bestEven = null;
        for (long exp = bitLength(powerCandidate); exp >= 2; exp--) {
            long[] root = nthRootFloor(powerCandidate, exp);
            if (comparePowTo(root, exp, powerCandidate) == 0) {
                if ((exp & 1) == 1)
                    return new PowerPlusOneForm(root, exp);
                if (bestEven == null)
                    bestEven = new PowerPlusOneForm(root, exp);
            }
        }
        return bestEven;
    }
    private record PowerMinusOneForm(long[] base, long exponent) {}
    private static PowerMinusOneForm detectPowerMinusOne(long[] n) {
        long[] nPlusOne = addSmallMag(n, 1);
        long maxExp = bitLength(nPlusOne);
        for (long i = maxExp; i >= 2; i--) {
            long[] root = nthRootFloor(nPlusOne, i);
            if (comparePowTo(root, i, nPlusOne) == 0)
                return new PowerMinusOneForm(root, i);
        }
        return null;
    }
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

        boolean pm1Done;
        boolean brentDone;
        boolean fermatDone;

        int ecmPlanIndex;
        int lastQSProbePlan;
        int nextECMCurve;
        ECMModulusContext context;
        double qsNanos;

        ArrayList<QSRelationFull> relations = new ArrayList<>();
        PartialGraph partialGraph = new PartialGraph();

        FactorJob(long[] n) {
            this.n = trimLeadingZeros(n.clone());
            this.context = new ECMModulusContext(this.n);
            double lnN = logMag(n);
            double lnLnN = Math.log(lnN);
            long B = Math.round(Math.exp(0.55 * Math.sqrt(lnN * lnLnN)));
            this.B = Math.max(B, 100);
        }
        private FactorJob(long[] n, int ecmPlanIndex, int nextECMCurve){
            this.n = trimLeadingZeros(n.clone());
            this.ecmPlanIndex = ecmPlanIndex;
            this.nextECMCurve = nextECMCurve;
            this.context = new ECMModulusContext(this.n);
            double lnN = logMag(n);
            double lnLnN = Math.log(lnN);
            long B = Math.round(Math.exp(0.55 * Math.sqrt(lnN * lnLnN)));
            this.B = Math.max(B, 100);
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
        long[] h = divideExactMag(parent.n, candidate);
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
    private static boolean isNontrivialFactor(long[] d, long[] n) {return isZeroArray(modMagnitudes(n, d)) && !isOneArray(d) && compareArray(d, n) < 0;}
    private static void addFactor(LargeMap<MagKey, Long> p, MagKey base, long exponent) {
        if (!p.containsKey(base))
            p.put(base, exponent);
        else p.put(base, p.get(base) + exponent);
    }
    @FunctionalInterface
    private interface FactorCall { long[] run();}
    private record TimedFactorResult(long[] factor, long elapsedNanos){}
    private static TimedFactorResult timeFactorCall(FactorCall call) {
        long start = System.nanoTime();
        long[] factor = call.run();
        return new TimedFactorResult(factor, System.nanoTime() - start);
    }
    /**
     * Factors a limbs into multiplicative components. Usually this means prime factors. USUALLY.
     * In strict mathematics, 1 has an empty factorization, 0 has no prime factorization, and negatives are handled by a separate negative.
     * However, programming prefers total functions over philosophical purity.
     * To account for this, 1 returns {1 -> 1}, 0 returns {0 -> 1}, and -1 is treated as another factor.
     * Also, if the limbs decides to become a cryptography final boss, the remaining chunk may be treated as ONE atomic factor. So definitely not full prime factorization as one might expect.
     */
    public LargeMap<LargeInteger, LargeFraction> factor() {
        LargeMap<LargeInteger, LargeFraction> result = new LargeMap<>();
        if (negative) {
            result.put(NEGATIVE_ONE, LargeFraction.ONE);
            if (equalsArray(limbs, ONE_MAG)) return result;
        }
        LargeMap<MagKey, Long> bag = factorMag(limbs);
        for (MagKey factor : bag.keys())
            result.put(new LargeInteger(factor.key, false), new LargeFraction(bag.get(factor)));
        return result;
    }
    private static LargeMap<MagKey, Long> factorMag(long[] limbs) {
        LargeMap<MagKey, Long> p = new LargeMap<>();
        if (isZeroArray(limbs)) {
            p.put(new MagKey(ZERO_MAG), 1L);
            return p;
        }else if (isOneArray(limbs)) return p;
        long[] copy = limbs.clone();
        if(compareArray(copy, new long[]{Integer.MAX_VALUE}) > 0) {
            //detect 1 + 4b^4 = (1 - 2b + 2b^2)(1 + 2b + 2b^2)
            long[] nMinusOne = subtractSmallMag(copy, 1);
            if (isPerfectSquare(nMinusOne)) {
                long[] x = sqrtFloorMag(nMinusOne);
                DivRem DR = divideByLong(x, 2);
                if (DR.remainder == 0 && isPerfectSquare(DR.quotient)) {
                    long[] sqrtM = sqrtFloorMag(DR.quotient);
                    long[] factor1 = addSmallMag(subtractMagnitudes(shiftLeftBits(DR.quotient, 1), shiftLeftBits(sqrtM, 1)), 1);
                    long[] factor2 = addSmallMag(addMagnitudes(shiftLeftBits(DR.quotient, 1), shiftLeftBits(sqrtM, 1)), 1);
                    if (isNontrivialFactor(factor1, copy) && isNontrivialFactor(factor2, copy)) {
                        copy = divideExactMag(copy, factor1);
                        LargeMap<MagKey, Long> F1 = factorMag(factor1);
                        for (MagKey K : F1.keys())
                            addFactor(p, K, F1.get(K));
                        copy = divideExactMag(copy, factor2);
                        LargeMap<MagKey, Long> F2 = factorMag(factor2);
                        for (MagKey K : F2.keys())
                            addFactor(p, K, F2.get(K));
                    }
                }
            }
            // detect x^n - 1
            PowerMinusOneForm minusForm = detectPowerMinusOne(copy);
            if (minusForm != null) {
                LargeInteger n = valueOf(minusForm.exponent);
                LargeList<LargeInteger> divisors = n.divisorsProper();
                if (divisors.intSize() != 1) {
                    divisors.add(n);
                    for (LargeInteger d : divisors) {
                        long[] value = ((LargeInteger) LargePolynomial.cyclotomic(d).evaluateAt(new LargeInteger(minusForm.base, false))).limbs;
                        if (isOneArray(value)) continue;
                        if (equalsArray(value, limbs)) break;
                        copy = divideExactMag(copy, value);
                        LargeMap<MagKey, Long> subFactors = factorMag(value);
                        for (MagKey base : subFactors.keys())
                            addFactor(p, base, subFactors.get(base));
                    }
                }
            }
            // detect x^n + 1
            PowerPlusOneForm plusForm = detectPowerPlusOne(copy);
            if (plusForm != null && (plusForm.exponent & 1L) == 1) {
                LargeInteger n = valueOf(plusForm.exponent);
                LargeList<LargeInteger> divisors = n.divisorsProper();
                if (divisors.intSize() != 1) {
                    divisors.add(n);
                    for (LargeInteger d : divisors) {
                        long[] value = ((LargeInteger) LargePolynomial.cyclotomic(d.multiply(2)).evaluateAt(new LargeInteger(plusForm.base, false))).limbs;
                        if (isOneArray(value)) continue;
                        if (equalsArray(value, limbs)) break;
                        copy = divideExactMag(copy, value);
                        LargeMap<MagKey, Long> subFactors = factorMag(value);
                        for (MagKey base : subFactors.keys())
                            addFactor(p, base, subFactors.get(base));
                    }
                }
            }
        }
        for (int k : SMALL_PRIMES) {
            int exponent = 0;
            while (modSmall(copy, k) == 0) {
                exponent++;
                divideMagExactInPlace(copy, k);
            }
            if(exponent != 0)
                addFactor(p, new MagKey(new long[]{k}), exponent);
            if (isOneArray(copy)) {
                //sort factors
                for (LargeInteger i = ZERO; i.lessThan(p.size()); i = i.add(1)) {
                    for (LargeInteger j = i.add(1); j.lessThan(p.size()); j = j.add(1))
                        if (compareArray(p.keyAtIndex(i).key, p.keyAtIndex(j).key) > 0)
                            p.swap(p.keyAtIndex(i), p.keyAtIndex(j));
                }
                return p;
            }
        }
        if (isProbablyPrimeSpecialMag(limbs)) {
            p.put(new MagKey(limbs), 1L);
            return p;
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
            if (isProbablyPrimeSpecialMag(n)) {
                // Add prime to result.
                addFactor(p, new MagKey(n), 1);
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
                TimedFactorResult Brent = timeFactorCall(() -> brentRhos(n));
                //System.out.println("Brent rhos time for " + number + ":  " + formatSeconds(Brent.elapsedNanos));
                factor = Brent.factor();
                if (isNontrivialFactor(factor, copy)) {
//                    cache.save(number, new LargeInteger(factor, false), AlgorithmChoice.BRENT_RHOS);
                    splitAndQueue(work, task, factor);
                    continue;
                }
            }
            if (task.ecmPlanIndex >= EXTREME_PLANS.length) {
                System.out.println(number + " is too difficult to factor; treated as composite atomic");
                addFactor(p, new MagKey(n), 1L);
                continue;
            }
            ECMPlan plan = EXTREME_PLANS[task.ecmPlanIndex];
            if (task.B <= 15_000_000) {
                double nextECMSeconds = ECM_RUNTIME.estimateSeconds(n, plan);
                if (task.qsNanos < nextECMSeconds) {
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
        for (LargeInteger i = ZERO; i.lessThan(p.size()); i = i.add(1)) {
            for (LargeInteger j = i.add(1); j.lessThan(p.size()); j = j.add(1))
                if (compareArray(p.keyAtIndex(i).key, p.keyAtIndex(j).key) > 0)
                    p.swap(p.keyAtIndex(i), p.keyAtIndex(j));
        }
        return p;
    }
    public LargeInteger totient() {
        if (isZeroArray(limbs)) throw new ArithmeticException("Error: Totient undefined for 0");
        else if (equals(ONE)) return ONE;
        LargeInteger n = abs();
        LargeMap<LargeInteger, LargeFraction> factors = n.factor();
        LargeInteger result = n.copy();
        for (LargeInteger p : factors.keys()) {
            if (!p.isProbablyPrime())
                throw new ArithmeticException("Error: Totient requires full prime decomposition; composite atomic factor found: " + p);
            result = (LargeInteger) result.longDivision(p).multiply(p.subtract(1));
        }
        return result;
    }
    public LargeList<LargeInteger> divisorsProper() {
        LargeList<LargeInteger> result = new LargeList<>();
        if (isZeroArray(limbs) || equals(ONE)) return result;
        LargeMap<LargeInteger, LargeFraction> factorMap = abs().factor();
        result.add(ONE);
        if (factorMap.keyAtIndex(ZERO).equals(ONE)) return result;
        for (LargeInteger p : factorMap.keys()) {
            if (p.equals(NEGATIVE_ONE)) continue;
            LargeInteger exp = factorMap.get(p).numerator;
            LargeInteger currentSize = result.size();
            LargeInteger pPower = ONE;
            for (LargeInteger e = ONE; e.lesserThanOrEqualTo(exp); e = e.add(1)) {
                pPower = (LargeInteger) pPower.multiply(p);
                for (LargeInteger i = ZERO; i.lessThan(currentSize); i = i.add(1))
                    result.add((LargeInteger) result.get(i).multiply(pPower));
            }
        }
        result.removeLast();
        return result;
    }
    public boolean isPerfect() {
        if (lesserThanOrEqualTo(ONE)) return false;
        LargeInteger result = ZERO;
        for (LargeInteger I : divisorsProper())
            result = (LargeInteger) result.add(I);
        return equals(result);
    }
    //the inverse of the factor method
    public static LargeInteger buildFromFactors(LargeMap<LargeInteger, LargeFraction> pr) {
        LargeInteger result = ONE;
        for (LargeInteger p : pr.keys()) {
            if (!isOneArray(pr.get(p).denominator.limbs)) throw new ArithmeticException("Error: Result is not rational");
            if (pr.get(p).numerator.negative) throw new ArithmeticException("Error: Result is not an integer");
            LargeInteger expTemp = pr.get(p).numerator.abs();
            LargeInteger basTemp = p.copy();
            while (true) {
                if (expTemp.isOdd()) {
                    result = (LargeInteger) result.multiply(basTemp);
                    if (isOneArray(expTemp.limbs)) break;
                }
                basTemp = basTemp.square();
                expTemp = expTemp.longDivision(TWO);
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
    private static PM1Stage1Result pollardPMinusOneStage1(long[] n, long[] initialState, int oldBound, int newBound, int[] primes, MontgomeryReducer reducer) {
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

            batchExponent = multiplySmallMag(batchExponent, multiplier);
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
    private static long[] pollardPMinusOneStage2(long[] n, long[] stage1Montgomery, int[] primes, int B1, int B2, MontgomeryReducer reducer) {
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
            while (currentM < targetM) {
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
        long[] y = randomMagBetween(TWO_MAG, subtractSmallMag(n, 2));
        long[] c = randomMagBetween(ONE_MAG, subtractSmallMag(n, 1));
        long m = 1024;
        long r = 1;
        long times = 0;

        long[] g = ONE_MAG;
        long[] x = ZERO_MAG;
        long[] ys = ZERO_MAG;

        while (isOneArray(g) && times < steps) {
            x = y.clone();
            for (long i = 0; i < r && times < steps; i++) {
                y = modAdd(squareMagnitude(y), c, n);
                times++;
            }
            long k = 0;
            while (k < r && isOneArray(g) && times < steps) {
                ys = y;
                long[] q = ONE_MAG;
                long limit = Math.min(m, r - k);
                for (long i = 0; i < limit; i++) {
                    y = modAdd(squareMagnitude(y), c, n);
                    long[] diff = SignedMag.positive(x).add(SignedMag.negative(y)).mag;
                    if (isZeroArray(diff)) return ONE_MAG;
                    q = reducer.multiply(q, diff);
                    times++;
                }
                g = gcdMagnitudes(q, n);
                k += m;
            }
            if (r > (Long.MAX_VALUE >>> 1)) return ONE_MAG;
            r <<= 1;
        }
        if (isNontrivialFactor(g, n)) return g;
        if (equalsArray(g, n)) {
            g = ONE_MAG;
            for (int i = 0; i < 10_000 && isOneArray(g); i++) {
                ys = modAdd(squareMagnitude(ys), c, n);
                long[] diff = SignedMag.positive(x).add(SignedMag.negative(y)).mag;
                if (isZeroArray(diff)) return ONE_MAG;
                g = gcdMagnitudes(diff, n);
            }
            if (isNontrivialFactor(g, n)) return g;
        }
        return ONE_MAG;
    }
    public static long[] fermatCheck(long[] n) {
        long[] sqrtN = sqrtCeilMag(n);
        long[] diff = subtractMagnitudes(squareMagnitude(sqrtN), n);
        if (isPerfectSquare(diff))
            return addMagnitudes(sqrtN, sqrtCeilMag(diff));
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
        MontgomeryCurve(MontgomeryReducer reducer, long[] A24Normal) {
            this.reducer = reducer;
            A24 = reducer.toMontgomery(A24Normal);
            n = reducer.modulus;
        }
        MontgomeryPoint doublePoint(MontgomeryPoint P, ECMStats stats) {
            requireCanonicalPoint(P);
            return doublePointCanonical(P, stats);
        }

        private MontgomeryPoint doublePointCanonical(MontgomeryPoint P, ECMStats stats) {
            long t0 = 0;
            if (stage2) t0 = System.nanoTime();
            long[] xpz = reducer.addCanonical(P.X, P.Z);
            long[] xmz = reducer.subtractCanonical(P.X, P.Z);
            long[] xpz2 = reducer.square(xpz);
            long[] xmz2 = reducer.square(xmz);
            long[] diff = reducer.subtractCanonical(xpz2, xmz2);
            long[] X2 = reducer.multiply(xpz2, xmz2);
            long[] inner = reducer.addCanonical(xmz2, reducer.multiply(A24, diff));
            long[] Z2 = reducer.multiply(diff, inner);
            if (stage2) stats.stage2DoublingTime += System.nanoTime() - t0;
            return new MontgomeryPoint(X2, Z2);
        }

        MontgomeryPoint diffAdd(MontgomeryPoint P, MontgomeryPoint Q, MontgomeryPoint PMinusQ, ECMStats stats) {
            requireCanonicalPoint(P);
            requireCanonicalPoint(Q);
            requireCanonicalPoint(PMinusQ);
            return diffAddCanonical(P, Q, PMinusQ, stats);
        }

        private MontgomeryPoint diffAddCanonical(MontgomeryPoint P, MontgomeryPoint Q, MontgomeryPoint PMinusQ, ECMStats stats) {
            long t0 = 0;
            if (stage2) t0 = System.nanoTime();
            long[] a = reducer.addCanonical(P.X, P.Z);
            long[] b = reducer.subtractCanonical(P.X, P.Z);
            long[] c = reducer.addCanonical(Q.X, Q.Z);
            long[] d = reducer.subtractCanonical(Q.X, Q.Z);

            long[] da = reducer.multiply(d, a);
            long[] cb = reducer.multiply(c, b);

            long[] plus = reducer.addCanonical(da, cb);
            long[] minus = reducer.subtractCanonical(da, cb);
            if (stage2) stats.stage2DifferentialAdditionTime += System.nanoTime() - t0;
            return new MontgomeryPoint(reducer.multiply(PMinusQ.Z, reducer.square(plus)), reducer.multiply(PMinusQ.X, reducer.square(minus)));
        }

        private record DblAddResult(MontgomeryPoint doubled, MontgomeryPoint added) {}
        private DblAddResult doubleAndDiffAddCanonical(MontgomeryPoint P, MontgomeryPoint Q, MontgomeryPoint PMinusQ, ECMStats stats) {
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
            MontgomeryPoint doubled = new MontgomeryPoint(reducer.multiply(aa, bb), reducer.multiply(e, reducer.addCanonical(bb, reducer.multiply(A24, e))));
            MontgomeryPoint added = new MontgomeryPoint(reducer.multiply(PMinusQ.Z, reducer.square(plus)), reducer.multiply(PMinusQ.X, reducer.square(minus)));
            stats.stage2MontMultiplications += 7;
            return new DblAddResult(doubled, added);
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

        MontgomeryPoint multiplyPrac(MontgomeryPoint P, byte[] operations, int from, int toExclusive, int trailingDoubling, ECMStats stats) {
            if (from < 0 || toExclusive < from || toExclusive > operations.length || trailingDoubling < 0)
                throw new IllegalArgumentException("Invalid PRAC slice");
            requireCanonicalPoint(P);
            MontgomeryPoint A = doublePointCanonical(P, stats);
            MontgomeryPoint B = P;
            MontgomeryPoint C = P;
            for (int index = from; index < toExclusive; index++) {
                switch (operations[index]) {
                    case PracBlock.SWAP -> {
                        MontgomeryPoint temp = A;
                        A = B;
                        B = temp;
                    }
                    case PracBlock.CASE_1 -> {
                        MontgomeryPoint sum = diffAddCanonical(A, B, C, stats);
                        MontgomeryPoint newA = diffAddCanonical(sum, A, B, stats);
                        MontgomeryPoint newB = diffAddCanonical(B, sum, A, stats);
                        A = newA;
                        B = newB;
                    }
                    case PracBlock.CASE_2, PracBlock.CASE_4 -> {
                        DblAddResult pair = doubleAndDiffAddCanonical(A, B, C, stats);
                        A = pair.doubled;
                        B = pair.added;
                    }
                    case PracBlock.CASE_3 -> {
                        MontgomeryPoint oldB = B;
                        B = diffAddCanonical(B, A, C, stats);
                        C = oldB;
                    }
                    case PracBlock.CASE_5 -> {
                        // Computes 2A and A+C, where |A-C| = B.
                        DblAddResult pair = doubleAndDiffAddCanonical(A, C, B, stats);
                        A = pair.doubled;
                        C = pair.added;
                    }
                    case PracBlock.CASE_6 -> {
                        MontgomeryPoint oldA = A;
                        MontgomeryPoint oldB = B;
                        MontgomeryPoint oldC = C;

                        // Gives 2A and A+B.
                        DblAddResult pair = doubleAndDiffAddCanonical(oldA, oldB, oldC, stats);
                        A = diffAddCanonical(pair.doubled, oldA, oldA, stats);
                        B = diffAddCanonical(pair.doubled, pair.added, oldC, stats);
                        C = oldB;
                    }
                    case PracBlock.CASE_7 -> {
                        MontgomeryPoint oldA = A;
                        MontgomeryPoint oldB = B;

                        DblAddResult pair = doubleAndDiffAddCanonical(oldA, oldB, C, stats);
                        B = diffAddCanonical(pair.added, oldA, oldB, stats);
                        A = diffAddCanonical(pair.doubled, oldA, oldA, stats);
                    }
                    case PracBlock.CASE_8 -> {
                        MontgomeryPoint oldA = A;
                        MontgomeryPoint oldB = B;
                        MontgomeryPoint oldC = C;

                        DblAddResult pair = doubleAndDiffAddCanonical(oldA, oldB, oldC, stats);
                        A = diffAddCanonical(pair.doubled, oldA, oldA, stats);
                        B = pair.added;
                        C = diffAddCanonical(oldA, oldC, oldB, stats);
                    }
                    case PracBlock.CASE_9 -> {
                        // Computes 2B and B+C, where |B-C| = A.
                        DblAddResult pair = doubleAndDiffAddCanonical(B, C, A, stats);
                        B = pair.doubled;
                        C = pair.added;
                    }
                    default -> throw new IllegalStateException("Unknown PRAC operation: " + operations[index]);
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
                    operations.write(PracBlock.SWAP);
                }
                // Apply the first qualifying PRAC condition.
                if (d - e <= e / 4L && (d + e) % 3L == 0L) {
                    operations.write(PracBlock.CASE_1);
                    d = (2L * d - e) / 3L;
                    e = (e - d) / 2L;
                } else if (d - e <= e / 4L && (d - e) % 6L == 0L) {
                    operations.write(PracBlock.CASE_2);
                    d = (d - e) / 2L;
                } else if ((d + 3L) / 4L <= e) {
                    operations.write(PracBlock.CASE_3);
                    d -= e;
                } else if ((d + e) % 2L == 0L) {
                    operations.write(PracBlock.CASE_4);
                    d = (d - e) / 2L;
                } else if ((d & 1L) == 0L) {
                    operations.write(PracBlock.CASE_5);
                    d /= 2L;
                } else if (d % 3L == 0L) {
                    operations.write(PracBlock.CASE_6);
                    d = d / 3L - e;
                } else if ((d + e) % 3L == 0L) {
                    operations.write(PracBlock.CASE_7);
                    d = (d - 2L * e) / 3L;
                } else if ((d - e) % 3L == 0L) {
                    operations.write(PracBlock.CASE_8);
                    d = (d - e) / 3L;
                } else {
                    operations.write(PracBlock.CASE_9);
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

    /**
     * @param offsets          Chain k occupies operations[offsets[k] ... offsets[k + 1]).
     * @param trailingDoubling A long scalar has at most 63 trailing zero bits.
     */
    private record PracBlock(byte[] operations, int[] offsets, byte[] trailingDoubling, int powerOfTwoDoubling) {
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

        PracBlock {
            if (offsets.length != trailingDoubling.length + 1)
                throw new IllegalArgumentException("Offsets must contain ONE terminal entry");
            if (offsets[0] != 0 || offsets[offsets.length - 1] != operations.length)
                throw new IllegalArgumentException("Invalid PRAC operation offsets");
        }

        private static PracBlock buildPracBlock(long[] primes, long bound) {
            ByteArrayOutputStream operations = new ByteArrayOutputStream();

            int[] offsets = new int[primes.length + 1];
            byte[] trailingDoubling = new byte[primes.length];

            int chainCount = 0;
            int powerOfTwoDoubling = 0;

            for (long prime : primes) {
                if (prime < 2L || prime > bound)
                    continue;

                long scalar = prime;

                while (scalar <= bound / prime)
                    scalar *= prime;

                int shifts = Long.numberOfTrailingZeros(scalar);
                long oddPart = scalar >>> shifts;

                if (oddPart == 1L) {
                    // This can only be the largest power of 2 <= bound.
                    powerOfTwoDoubling = shifts;
                    continue;
                }
                offsets[chainCount] = operations.size();
                appendPracOperations(oddPart, operations);
                trailingDoubling[chainCount] = (byte) shifts;
                chainCount++;
            }
            offsets[chainCount] = operations.size();
            return new PracBlock(operations.toByteArray(), Arrays.copyOf(offsets, chainCount + 1), Arrays.copyOf(trailingDoubling, chainCount), powerOfTwoDoubling);
        }
        private static void appendPracOperations(long oddScalar, ByteArrayOutputStream operations) {
            long r = goldenSplit(oddScalar);
            while (gcdLong(oddScalar, r) != 1L) r--;
            long d = oddScalar - r;
            long e = r - d;
            while (d != e) {
                if (d < e) {
                    long temporary = d;
                    d = e;
                    e = temporary;
                    operations.write(PracBlock.SWAP);
                }
                // Apply the first qualifying PRAC condition.
                if (d - e <= e / 4L && (d + e) % 3L == 0L) {
                    operations.write(PracBlock.CASE_1);
                    d = (2L * d - e) / 3L;
                    e = (e - d) / 2L;
                } else if (d - e <= e / 4L && (d - e) % 6L == 0L) {
                    operations.write(PracBlock.CASE_2);
                    d = (d - e) / 2L;
                } else if ((d + 3L) / 4L <= e) {
                    operations.write(PracBlock.CASE_3);
                    d -= e;
                } else if ((d + e) % 2L == 0L) {
                    operations.write(PracBlock.CASE_4);
                    d = (d - e) / 2L;
                } else if ((d & 1L) == 0L) {
                    operations.write(PracBlock.CASE_5);
                    d /= 2L;
                } else if (d % 3L == 0L) {
                    operations.write(PracBlock.CASE_6);
                    d = d / 3L - e;
                } else if ((d + e) % 3L == 0L) {
                    operations.write(PracBlock.CASE_7);
                    d = (d - 2L * e) / 3L;
                } else if ((d - e) % 3L == 0L) {
                    operations.write(PracBlock.CASE_8);
                    d = (d - e) / 3L;
                } else {
                    operations.write(PracBlock.CASE_9);
                    e /= 2L;
                }
                if (d <= 0L || e <= 0L)
                    throw new IllegalStateException("Invalid PRAC reduction");
            }
            if (d != 1L)
                throw new IllegalStateException("PRAC pair was not coprime");
        }
    }

    //    private static int firstGreaterThan(int[] values, int target) {
//        int low = 0;
//        int high = values.length;
//        while (low < high) {
//            int mid = (low + high) >>> 1;
//            if (values[mid] <= target)
//                low = mid + 1;
//            else high = mid;
//        }
//        return low;
//    }
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
            this.Q = P;
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
            blockProduct = curve.reducer.rModM;
        }

        static long nearestCenterIndex(long q) {
            long m = q / D;
            if (q % D > HALF_D)
                m++;
            return Math.max(1L, m);
        }

        long[] stage2Term(long q, ECMStats stats) {
            stats.stage2TermCount++;
            long targetM = nearestCenterIndex(q);
            while (currentM < targetM) {
                MontgomeryPoint next;
                if (currentM == 1L) next = curve.doublePoint(difference, stats);
                else next = curve.diffAdd(current, difference, previous, stats);
                previous = current;
                current = next;
                currentM++;
            }
            long center = Math.multiplyExact(currentM, D);
            long distance = Math.abs(center - q);
            if (distance == 0L || distance > HALF_D) return null;
            MontgomeryPoint B = baby[(int) distance - 1];
            long[] left = curve.reducer.multiply(current.X, B.Z);
            long[] right = curve.reducer.multiply(B.X, current.Z);
            return curve.reducer.subtract(left, right);
        }

        void clearStage2Block() {
            blockProduct = curve.reducer.rModM;
            blockTermCount = 0;
            blockStartPrevious = null;
            blockStartCurrent = null;
            blockStartM = 0L;
        }
    }
    private static final class ECMStats {
        final ECMPlan plan;
        final long planStartNanos = System.nanoTime();

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
        long stage2DifferentialAdditionTime = 0;
        long blockProcessTime = 0;
        long blockTermCount = 0;
        long stage2MontMultiplications = 0;
        long replayPrime = -1;

        Throwable failure;
        ECMStats(ECMPlan plan) {this.plan = plan;}
        void found(long[] factor, String location, int curve, long blockLower, long blockUpper) {
            System.out.println("ECM FACTOR FOUND: " + new LargeInteger(factor, false));
            System.out.println("Location of factor: " + location);
            System.out.println("Curve where factor was found: " + curve);
            if (blockLower != -1)
                System.out.println("Factor found in prime block between " + blockLower + " and " + blockUpper);
            if (location.equals("Stage 2 individual-term replay"))
                System.out.println("During replay, found found due to prime " + replayPrime);
            System.out.println();
        }
        long totalPlanNanos() {return System.nanoTime() - planStartNanos;}
    }
    private static final ECMPlan[] EXTREME_PLANS = {
            new ECMPlan(2_000L, 200_000L, 32),
            new ECMPlan(11_000L, 1_900_000L, 96),
            new ECMPlan(50_000L, 13_000_000L, 256),
            new ECMPlan(250_000L, 130_000_000L, 448),
            new ECMPlan(1_000_000L, 1_000_000_000L, 960),
            new ECMPlan(3_000_000L, 5_700_000_000L, 2400),
            new ECMPlan(11_000_000L, 35_000_000_000L, 4800),
            new ECMPlan(43_000_000L, 240_000_000_000L, 8000)
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
            ECMRuntimeAggregate aggregate = samples.computeIfAbsent(key, _ -> new ECMRuntimeAggregate());
            aggregate.add(elapsedNanos);
        }
        synchronized double estimateSeconds(long[] n, ECMPlan plan) {
            int limbs = trimLeadingZeros(n).length;
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
            return nanosPerCurve * plan.curves / 1e9;
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
            ExtGcdMag eg = extendedGcdMagnitudes(FOUR_MAG, n);
            if (!equalsArray(eg.gcd, ONE_MAG))
                throw new FactorFoundException(eg.gcd);
            this.inv4 = eg.x.modMag(n);
            this.reducer = new MontgomeryReducer(n);
        }
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
            long t0 = System.nanoTime();
            //Create the curves
            int curveCount = 0;
            while (curveCount < works.length) {
                long[] sigma = randomMagBetween(SIX_MAG, n);
                // Normal residue
                long[] sigmaSquared = modMagnitudes(squareMagnitude(sigma), n);

                long[] u = modMagnitudes(subtractSmallMag(sigmaSquared, 5), n);
                long[] v = modMagnitudes(shiftLeftBits(sigma, 2), n);

                long[] numerator = SignedMag.positive(v).add(SignedMag.negative(u)).cube().multiplyMagnitude(addMagnitudes(v, multiplySmallMag(u, 3L))).modMag(n);
                long[] denominator = modMagnitudes(shiftLeftBits(multiplyMagnitudes(cubeMagnitude(u), v), 2L), n);

                ExtGcdMag eg = extendedGcdMagnitudes(denominator, n);
                if (isNontrivialFactor(eg.gcd, n)) {
                    stats.curveCreateNanos = System.nanoTime() - t0;
                    stats.found(eg.gcd, "Curve construction denominator GCD", curveCount+1, -1, -1);
                    return eg.gcd;
                }
                if (equalsArray(eg.gcd, n)) continue; //Degenerate Suyama parameter; try another sigma
                long[] A = SignedMag.positive(numerator).multiply(eg.x).add(SignedMag.negative(TWO_MAG)).modMag(n);
                long[] A24 = modMagnitudes(multiplyMagnitudes(addSmallMag(A, 2L), context.inv4), n);
                MontgomeryCurve curve = new MontgomeryCurve(reducer, A24);

                // Convert u and v before Montgomery multiplication.
                long[] uMont = reducer.toMontgomery(u);
                long[] vMont = reducer.toMontgomery(v);

                long[] X = reducer.cube(uMont);
                long[] Z = reducer.cube(vMont);
                MontgomeryPoint point = new MontgomeryPoint(X, Z);
                works[curveCount] = new ECMCurveWork(curve, point);
                curveCount++;
            }
            stats.curveCreateNanos = System.nanoTime() - t0;
            //Stage 1 in ECM
            long t1 = System.nanoTime();
            stage2 = false;
            long lower = 2L;
            long upperExclusive = plan.B1 + 1L;
            while (lower < upperExclusive) {
                long upper = upperExclusive - lower <= 1048576 ? upperExclusive : lower + 1048576;
                t0 = System.nanoTime();
                long[] primes = sievePrimeBlock(lower, upper, SMALL_PRIMES);
                stats.stage1SieveNanos += System.nanoTime() - t0;
                t0 = System.nanoTime();
                PracBlock block = PracBlock.buildPracBlock(primes, plan.B1);
                stats.stage1ChainBuildNanos += System.nanoTime() - t0;
                //apply the Prac block
                t0 = System.nanoTime();
                for (ECMCurveWork state : works) {
                    for (int i = 0; i < block.powerOfTwoDoubling; i++)
                        state.point = state.curve.doublePoint(state.point, stats);
                    byte[] operations = block.operations;
                    int[] offsets = block.offsets;
                    byte[] doubling = block.trailingDoubling;
                    for (int chain = 0; chain < doubling.length; chain++)
                        state.point = state.curve.multiplyPrac(state.point, operations, offsets[chain], offsets[chain + 1], doubling[chain], stats);
                }
                stats.stage1ApplyNanos += System.nanoTime() - t0;
                lower = upper;
            }
            t0 = System.nanoTime();
            for (int k = 0; k < works.length; k++) {
                ECMCurveWork state = works[k];
                long[] g = gcdMagnitudes(state.point.Z, n);
                if (isNontrivialFactor(g, n)) {
                    stats.stage1Nanos = System.nanoTime() - t1;
                    stats.found(g, "Stage 1 final GCD", job.nextECMCurve + k + 1, -1, -1);
                    return g;
                }
                if (equalsArray(g, n)) state.usable = false;
                else stats.usableCurves++;
            }
            stats.gcdStage1Nanos += System.nanoTime() - t0;
            stats.stage1Nanos = System.nanoTime() - t1;
            // Stage 2 in ECM
            long t2 = System.nanoTime();
            stage2 = true;
            lower = plan.B1 + 1L;
            upperExclusive = plan.B2 + 1L;
            Stage2State[] stage2States = new Stage2State[works.length];
            long blockLower = lower;

            while (blockLower < upperExclusive) {
                long blockUpper = upperExclusive - blockLower <= 1_048_576L ? upperExclusive : blockLower + 1_048_576L;

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
                        stage2States[k] = new Stage2State(work.curve, work.point, primes[0], stats);

                    Stage2State s2 = stage2States[k];
                    stats.stage2BlockCount++;
                    s2.blockStartPrevious = s2.previous;
                    s2.blockStartCurrent = s2.current;
                    s2.blockStartM = s2.currentM;
                    long blockStart = System.nanoTime();
                    for (long prime : primes) {
                        long[] term = s2.stage2Term(prime, stats);
                        if (term == null) {
                            /*
                             * Keep this only if it accurately represents what
                             * stage2Term performed in the null case.
                             */
                            stats.stage2MontMultiplications += 2;
                            continue;
                        }
                        s2.blockProduct = s2.curve.reducer.multiply(s2.blockProduct, term);
                        s2.blockTermCount++;
                        stats.blockTermCount++;
                        stats.stage2MontMultiplications += 3;
                    }
                    stats.blockProcessTime += System.nanoTime() - blockStart;
                    if (s2.blockTermCount == 0) {
                        s2.clearStage2Block();
                        continue;
                    }
                    t0 = System.nanoTime();
                    /*
                     * Do not overwrite blockProduct with the normal residue.
                     */
                    long[] g = gcdMagnitudes(s2.curve.reducer.reduce(s2.blockProduct), s2.curve.n);
                    stats.stage2GcdTime += System.nanoTime() - t0;
                    if (isNontrivialFactor(g, s2.curve.n)) {
                        s2.clearStage2Block();
                        stats.stage2Nanos = System.nanoTime() - t2;
                        stats.found(g, "Stage 2 GCD", job.nextECMCurve + k + 1, blockLower, blockUpper);
                        return g;
                    }
                    if (equalsArray(g, s2.curve.n)) {
                        /*
                         * Restore the giant-step state to the beginning of this
                         * prime block and find the individual factor-producing
                         * term.
                         */
                        s2.previous = s2.blockStartPrevious;
                        s2.current = s2.blockStartCurrent;
                        s2.currentM = s2.blockStartM;
                        s2.blockProduct = s2.curve.reducer.rModM;
                        s2.blockTermCount = 0;
                        for (long q : primes) {
                            long[] term = s2.stage2Term(q, stats);
                            if (term == null) continue;
                            t0 = System.nanoTime();
                            g = gcdMagnitudes(term, s2.curve.n);
                            stats.stage2GcdTime += System.nanoTime() - t0;
                            if (isNontrivialFactor(g, s2.curve.n)) {
                                stats.replayPrime = q;
                                s2.clearStage2Block();
                                stats.stage2Nanos = System.nanoTime() - t2;
                                stats.found(g, "Stage 2 individual-term replay", job.nextECMCurve + k + 1, blockLower, blockUpper);
                                return g;
                            }
                        }
                    }
                    /*
                     * Required for both gcd == 1 and gcd == n.
                     * previous/current/currentM remain intact for the next block.
                     */
                    s2.clearStage2Block();
                }
                /*
                 * Advance the outer block only after every curve has processed it.
                 */
                blockLower = blockUpper;
            }
            stats.stage2Nanos = System.nanoTime() - t2;
            System.out.println("32 curves tried with bounds " + plan.B1 + " and " + plan.B2 + "; " + stats.usableCurves + " being useful");
            System.out.println("Time for creating all the curves: " + stats.curveCreateNanos / 1e9 + " seconds");
            System.out.println("Stage 1 sieve time: " + stats.stage1SieveNanos / 1e9 + " seconds");
            System.out.println("Stage 1 chain build time: " + stats.stage1ChainBuildNanos / 1e9 + " seconds");
            System.out.println("Stage 1 chain application time: " + stats.stage1ApplyNanos / 1e9 + " seconds");
            System.out.println("Stage 1 gcd time: " + stats.gcdStage1Nanos / 1e9 + " seconds");
            System.out.println("Total Stage 1 time: " + stats.stage1Nanos / 1e9 + " seconds");
            System.out.println("Stage 2 terms: " + stats.stage2TermCount);
            System.out.println("Stage 2 blocks: " + stats.stage2BlockCount);
            System.out.println("Stage 2 point doubling time: " + stats.stage2DoublingTime / 1e9 + " seconds");
            System.out.println("Stage 2 point differential addition time: " + stats.stage2DifferentialAdditionTime / 1e9 + " seconds");
            System.out.println("Stage 2 block process time: " + stats.blockProcessTime / 1e9 + " seconds over " + stats.blockTermCount + " terms");
            System.out.println("Stage 2 Montgomery multiplications: " + stats.stage2MontMultiplications);
            System.out.println("Stage 2 gcd time: " + stats.stage2GcdTime / 1e9 + " seconds");
            System.out.println("Total stage 2 time: " + stats.stage2Nanos / 1e9 + " seconds");
            System.out.println("Total time for this batch: " + stats.totalPlanNanos() / 1e9 + " seconds");
            System.out.println();
        } catch (Throwable t) {
            stats.failure = t;
            throw t;
        }
        long completedPlanNanos = stats.totalPlanNanos();
        ECM_RUNTIME.observe(n, plan, completedPlanNanos);
        return ONE_MAG;
    }
    private static final class QSFactorBase {
        final int size;
        // Permanent data
        final int[] primes;
        final int[] logPrimes;

        // Current polynomial-family data
        final int[] root1;
        final int[] root2;

        QSFactorBase(int[] primes, int[] logPrimes, int[] root1, int[] root2) {
            this.size = primes.length;
            this.primes = primes;
            this.logPrimes = logPrimes;
            this.root1 = root1;
            this.root2 = root2;
        }
    }
    private static abstract class QSRelation {
        final long[][] xValues;
        final SparseExponents exponents;
        QSRelation(long[][] xValues, SparseExponents exponents) {
            if (xValues == null || xValues.length == 0)
                throw new IllegalArgumentException("Relation must contain an X value");
            if (exponents == null)
                throw new IllegalArgumentException("Exponent vector cannot be null");
            this.xValues = xValues;
            this.exponents = exponents;
        }
    }
    /**
     * @param index column index: 0 = negative, j+1 = factorBase[j]
     * @param count exponent count
     */
    private record SparseExponents(int[] index, short[] count) {}
    private static SparseExponents compress(short[] dense) {
        int nonzero = 0;
        for (short v : dense) {
            if (v != 0)
                nonzero++;
        }
        int[] index = new int[nonzero];
        short[] count = new short[nonzero];
        int k = 0;
        for (int i = 0; i < dense.length; i++) {
            if (dense[i] != 0) {
                index[k] = i;
                count[k] = dense[i];
                k++;
            }
        }
        return new SparseExponents(index, count);
    }
    private static final class QSRelationFull extends QSRelation {
        final long[] parity;
        final long[][] squareFactors;
        QSRelationFull(long[][] xValues, SparseExponents exponents, int columnCount, long[][] squareFactors) {
            super(xValues, exponents);
            this.parity = new long[(columnCount + 63) >>> 6];
            for (int k = 0; k < exponents.index.length; k++) {
                if ((exponents.count[k] & 1) != 0) {
                    int column = exponents.index[k];
                    parity[column >>> 6] ^= 1L << (column & 63);
                }
            }
            this.squareFactors = squareFactors;
        }
    }
    private static final long PARTIAL_ROOT = 0L;
    private static final class QSRelationPartial extends QSRelation {
        final long vertex1;
        final long vertex2;
        QSRelationPartial(long[][] xValues, SparseExponents exponents, long vertex1, long vertex2) {
            super(xValues, exponents);
            if (vertex1 == PARTIAL_ROOT && vertex2 == PARTIAL_ROOT)
                throw new IllegalArgumentException("A partial relation must contain at least one large prime");
            if (vertex1 != PARTIAL_ROOT && Long.compareUnsigned(vertex1, 1L) <= 0)
                throw new IllegalArgumentException("Large-prime vertices must be greater than one");
            if (vertex2 != PARTIAL_ROOT && Long.compareUnsigned(vertex2, 1L) <= 0)
                throw new IllegalArgumentException("Large-prime vertices must be greater than one");
            // Keep ROOT first for single partials, and otherwise sort unsigned.
            if (vertex1 == PARTIAL_ROOT) {
                this.vertex1 = PARTIAL_ROOT;
                this.vertex2 = vertex2;
            } else if (vertex2 == PARTIAL_ROOT) {
                this.vertex1 = PARTIAL_ROOT;
                this.vertex2 = vertex1;
            } else if (Long.compareUnsigned(vertex1, vertex2) <= 0) {
                this.vertex1 = vertex1;
                this.vertex2 = vertex2;
            } else {
                this.vertex1 = vertex2;
                this.vertex2 = vertex1;
            }
        }
        static QSRelationPartial single(long[][] xValues, SparseExponents exponents, long largePrime) {return new QSRelationPartial(xValues, exponents, PARTIAL_ROOT, largePrime);}
        static QSRelationPartial pair(long[][] xValues, SparseExponents exponents, long largePrime1, long largePrime2) {return new QSRelationPartial(xValues, exponents, largePrime1, largePrime2);}
        boolean isSinglePrime(){return vertex1 == PARTIAL_ROOT;}
    }
    private static long inverseModSmall(long a, long p) {
        long oldR = p;
        long r = Math.floorMod(a, p);
        long oldS = 0;
        long s = 1;
        while (r != 0) {
            long q = oldR / r;
            long tmpR = oldR - q * r;
            oldR = r;
            r = tmpR;
            long tmpS = oldS - q * s;
            oldS = s;
            s = tmpS;
        }
        if (oldR != 1)
            throw new ArithmeticException("No inverse mod " + p);
        return Math.floorMod(oldS, p);
    }

    private record AFactor(int fbIndex, int p, int root) {}
    private static int maxFactorBaseIndexAtMost(QSFactorBase factorBase, long[] maxNextPrimeMag) {
        if (isZeroArray(maxNextPrimeMag)) return -1;
        int last = factorBase.size - 1;
        if (maxNextPrimeMag.length > 1) return last;
        long max = maxNextPrimeMag[0];
        if (Long.compareUnsigned(max, factorBase.primes[last]) >= 0) return last;
        int idx = Arrays.binarySearch(factorBase.primes, (int)max);
        if(idx < 0) return -idx-2;
        return idx;
    }
    private record ACoefficient(long[] A, AFactor[] primes, long[] aExponents) {}
    private record ACandidateResult(ACoefficient coefficient, String failure) {
        static ACandidateResult ok(ACoefficient c) {return new ACandidateResult(c, null);}
        static ACandidateResult fail(String reason) {return new ACandidateResult(null, reason);}
        boolean ok() {return coefficient != null;}
    }
    private static ACoefficient chooseAByScoring(long[] sqrt2N, QSFactorBase factorBase, int aFactorCount, SplittableRandom rng) {
        ACoefficient best = null;
        double bestYield = -1.0;

        ACoefficient fallback = null;
        long fallbackHalf = 0L;

        int pMax = factorBase.primes[factorBase.size - 1];
        int attempts = 4096;

        int genFail = 0;
        int zeroHalf = 0;
        int hugeHalf = 0;
        int badYield = 0;
        int accepted = 0;
        int halfOverInt = 0;
        int halfTooSmall = 0;
        int halfTooLarge = 0;

        String firstGenFailure = null;
        long targetHalf = targetHalfForA(factorBase, aFactorCount);
        for (int trial = 0; trial < attempts; trial++) {
            ACandidateResult result = randomSquarefreeAResult(sqrt2N, factorBase, aFactorCount, rng);
            if (!result.ok()) {
                genFail++;
                if (firstGenFailure == null)
                    firstGenFailure = result.failure;
                continue;
            }
            ACoefficient candidate = result.coefficient;
            long[] halfMag = divideMagnitudes(sqrt2N, candidate.A)[0];
            if (isZeroArray(halfMag)) {
                zeroHalf++;
                continue;
            }

            if (halfMag.length > 1) {
                hugeHalf++;
                continue;
            }
            long half = halfMag[0];
            if (Long.compareUnsigned(half, Integer.MAX_VALUE) > 0) {
                halfOverInt++;
                continue;
            }
            long lower = Math.max(1L, targetHalf / 4L);
            long upper = Math.multiplyExact(targetHalf, 4L);

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
            double yield = estimatedRelationsPerPolynomial(candidate.A, halfMag, pMax);
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
            System.out.println("SIQS A warning: using fallback A because strict scoring rejected all candidates. " + "aFactorCount=" + aFactorCount + " attempts=" + attempts + " genFail=" + genFail + " zeroHalf=" + zeroHalf + " hugeHalf=" + hugeHalf + " halfOverInt" + halfOverInt + " halfTooLarge" + halfTooLarge + "halfTooSmall" + halfTooSmall + " badYield=" + badYield + " accepted=" + accepted + " fallbackHalf=" + Long.toUnsignedString(fallbackHalf));
            return fallback;
        }
        throw new IllegalStateException("Could not find usable SIQS A: " + "aFactorCount=" + aFactorCount + " attempts=" + attempts + " genFail=" + genFail + " zeroHalf=" + zeroHalf + " hugeHalf=" + hugeHalf + " halfOverInt=" + halfOverInt + " halfTooLarge=" + halfTooLarge + "halfTooSmall=" + halfTooSmall + " badYield=" + badYield + " accepted=" + accepted + " firstGenFailure=[" + firstGenFailure + "]");
    }
    private static long targetHalfForA(QSFactorBase factorBase, int aFactorCount) {
        /*
         * Total positions per A, across all B values.
         * Tune this multiplier empirically.
         */
        long half = 16L*factorBase.size/(1L << aFactorCount);
        return Math.max(half, 1L);
    }
    private static ACandidateResult randomSquarefreeAResult(long[] sqrt2N, QSFactorBase factorBase, int aFactorCount, SplittableRandom rng) {
        boolean[] used = new boolean[factorBase.size];
        ArrayList<AFactor> chosen = new ArrayList<>();
        long[] A = ONE_MAG;
        long targetHalf = targetHalfForA(factorBase, aFactorCount);
        double logTargetA = logMag(sqrt2N) - Math.log(targetHalf);
        if (!Double.isFinite(logTargetA) || logTargetA <= 0.0)
            return ACandidateResult.fail("badLogTargetA");
        double logA = 0.0;

        while (chosen.size() < aFactorCount) {
            int remaining = aFactorCount - chosen.size();
            double remainingLogBudget = logTargetA - logA;
            if (remainingLogBudget <= 0.0)
                return ACandidateResult.fail("A already above target");
            double desiredLogP = remainingLogBudget / remaining;
            /*
             * Also enforce A * p < sqrt2N, but do not use sqrt2N as the actual target.
             * sqrt2N is only the absolute upper bound.
             */
            long[] maxNextPrimeMag = divideMagnitudes(sqrt2N, A)[0];
            int maxIdx = maxFactorBaseIndexAtMost(factorBase, maxNextPrimeMag);
            if (maxIdx < 1)
                return ACandidateResult.fail("maxIdx<1");
            int idx = chooseUnusedPrimeNearLogBounded(factorBase, used, desiredLogP, maxIdx, rng);
            if (idx < 0)
                return ACandidateResult.fail("noUnusedPrime");
            long[] newA = multiplySmallMag(A, factorBase.primes[idx]);
            if (compareArray(newA, sqrt2N) > 0)
                return ACandidateResult.fail("A exceeded sqrt2N");
            used[idx] = true;
            int root = rng.nextBoolean() ? factorBase.root1[idx] : factorBase.root2[idx];
            chosen.add(new AFactor(idx, factorBase.primes[idx], root));
            A = newA;
            logA += factorBase.logPrimes[idx]/LOG_SCALE;
        }
        AFactor[] factors = chosen.toArray(new AFactor[0]);
        long[] aExponents = new long[factorBase.size];
        for (AFactor f : factors) {
            if (aExponents[f.fbIndex] != 0)
                return ACandidateResult.fail("notSquarefree");
            aExponents[f.fbIndex] = 1;
        }
        return ACandidateResult.ok(new ACoefficient(A, factors, aExponents));
    }

    private static int chooseUnusedPrimeNearLogBounded(QSFactorBase factorBase, boolean[] used, double desiredLogP, int maxIdx, SplittableRandom rng) {
        if (maxIdx < 1) return -1;
        int center = getCenter(factorBase, desiredLogP, maxIdx);
        // Tiny random jitter, not maxIdx/64.
        int radius = Math.max(8, maxIdx/64);
        for (int tries = 0; tries < 32; tries++) {
            int offset = rng.nextInt(-radius, radius + 1);
            int idx = center + offset;
            if (idx >= 1 && idx <= maxIdx && !used[idx] && factorBase.primes[idx] > 2)
                return idx;
        }
        // nearest unused prime
        for (int d = 0; center - d >= 1 || center + d <= maxIdx; d++) {
            int a = center - d;
            int b = center + d;
            if (a >= 1 && !used[a] && factorBase.primes[a] > 2)
                return a;
            if (b <= maxIdx && !used[b] && factorBase.primes[b] > 2)
                return b;
        }
        return -1;
    }

    private static int getCenter(QSFactorBase factorBase, double desiredLogP, int maxIdx) {
        double desiredPDouble = Math.exp(desiredLogP);
        if (!Double.isFinite(desiredPDouble))
            desiredPDouble = factorBase.primes[maxIdx];
        int desiredP = (int) Math.clamp(desiredPDouble, 3, factorBase.primes[maxIdx]);
        int lo = 1;
        int hi = maxIdx;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (factorBase.primes[mid] < desiredP)
                lo = mid + 1;
            else hi = mid - 1;
        }
        return Math.clamp(lo, 1, maxIdx);
    }

    private static double roughDickman(double u) {
        return switch (u) {
            case double v when v < 0.0 -> 0.0;
            case double v when v <= 1.0 -> 1.0;
            case double v when v <= 2.0 -> 1.0 - Math.log(u);
            default -> Math.exp(-u * (Math.log(u) + Math.log(Math.log(u)) - 1.0)); // Crude but useful ranking approximation.
        };
    }

    private static double estimatedRelationsPerPolynomial(long[] A, long[] halfMag, int pMax) {
        if (isZeroArray(halfMag))
            return 0.0;

        if (halfMag.length > 1)
            return 0.0;

        long half = halfMag[0];

        if (half == 0)
            return 0.0;

        double logA = logMag(A);
        double logM = Math.log((double) half);

        double logQEdge = logA + 2.0 * logM;

        double u = logQEdge / Math.log(pMax);

        double smoothProb = roughDickman(u);

        return 2.0 * half * smoothProb;
    }

    private static void verifyACoefficient(ACoefficient AR, QSFactorBase factorBase) {
        long[] rebuiltA = ONE_MAG;
        long[] rebuiltExp = new long[factorBase.size];
        for (AFactor f : AR.primes) {
            if (rebuiltExp[f.fbIndex()] != 0)
                throw new AssertionError("Repeated A prime");
            if (factorBase.primes[f.fbIndex] != f.p)
                throw new AssertionError("AFactor fbIndex does not match p");
            rebuiltExp[f.fbIndex()] = 1;
            rebuiltA = multiplySmallMag(rebuiltA, f.p);
        }
        if (!equalsArray(rebuiltA, AR.A))
            throw new AssertionError("A != product of A factors");
        for (int i = 0; i < rebuiltExp.length; i++)
            if (rebuiltExp[i] != AR.aExponents[i])
                throw new AssertionError("Bad A exponent vector at index " + i);
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
        long logSieveNs;
        long sieveNs;
        long attemptNs;
        long polyNs;

        AStats(long[] A, long[] halfMag, QSFactorBase factorBase, int aFactorCount, long largePrimeBound, RelationYieldEstimate estimate) {
            this.aFactorCount = aFactorCount;
            this.bCount = 1L << (aFactorCount - 1);
            this.half = halfMag[0];
            this.interval = 2L * half;
            this.positionsPerA = Math.multiplyExact(interval, bCount);

            this.fbSize = factorBase.size;
            this.pMax = factorBase.primes[factorBase.size - 1];
            this.largePrimeBound = largePrimeBound;

            this.logA = logMag(A);
            this.logQEdge = logA + 2.0 * Math.log((double) half);

            this.uB = logQEdge / Math.log(pMax);

            this.uLP = logQEdge / Math.log((double) largePrimeBound);

            this.estimate = estimate;
            this.initialEstimate = estimate;
        }
        void finishPolynomial(long sieveElapsedNs, long attemptElapsedNs, long generationElapsedNs){
            polys++;
            sieveNs += sieveElapsedNs;
            attemptNs += attemptElapsedNs;
            polyNs += generationElapsedNs;
            estimate = adaptiveEstimate();
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
        long relationCount;
        long factorTestsTotal;
        long factorTestsMin = Long.MAX_VALUE;
        long factorTestsMax;
        void observeFactorTests(long tested) {
            relationCount++;
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
            double fullHitsPerPoly = weightedAverage(initialEstimate.fullPerPoly(), priorWeight, measuredFullPerPoly(), observedWeight);
            double singleHitsPerPoly = weightedAverage(initialEstimate.singlePartialHitsPerPoly(), priorWeight, measuredSinglePartialPerPoly(), observedWeight);
            double doubleHitsPerPoly = weightedAverage(initialEstimate.doublePartialHitsPerPoly(), priorWeight, measuredDoublePartialPerPoly(), observedWeight);
            double singleMergesPerPoly = weightedAverage(initialEstimate.singlePartialMergesPerPoly(), priorWeight, singlePartialMerged / (double) polys, observedWeight);
            double doubleMergesPerPoly = weightedAverage(initialEstimate.doublePartialMergesPerPoly(), priorWeight, (doublePartialMerged + mixedPartialMerged) / (double) polys, observedWeight);
            return RelationYieldEstimate.of(fullHitsPerPoly, singleHitsPerPoly, singleMergesPerPoly, doubleHitsPerPoly, doubleMergesPerPoly);
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
        long collectionNs(){return polyNs+sieveNs+attemptNs;}
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
            double avgTests = factorTestsTotal / (double) relationCount;
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
            System.out.println("  Factor-base size: " + fbSize);
            System.out.println("  Factor-base maximum: " + pMax);
            System.out.println("  Large-prime bound: " + largePrimeBound);
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
            System.out.println("  Total log sieve time: " + formatSeconds(logSieveNs));
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
    private static AStats makeAPlan(long[] A, long[] sqrt2N, QSFactorBase factorBase, int aFactorCount, long largePrimeBound) {
        long[] halfMag = divideMagnitudes(sqrt2N, A)[0];
        if (isZeroArray(halfMag) || halfMag.length > 1)
            throw new AssertionError("Bad A HALF=" + new LargeInteger(halfMag, false));
        int pMax = factorBase.primes[factorBase.size - 1];
        RelationYieldEstimate estimate = RelationYieldEstimate.estimateRelationYieldPerPolynomial(A, halfMag, pMax, largePrimeBound);
        return new AStats(A, halfMag, factorBase, aFactorCount, largePrimeBound, estimate);
    }

    private static final class MPQSPolynomial {
        final long[] A;
        final long[] B;
        final SignedMag C;
        final long[] twoB;
        final AFactor[] aPrimes;
        final long[] aExponents;

        MPQSPolynomial(long[] A, long[] B, SignedMag C, AFactor[] aPrimes, long[] aExponents) {
            this.A = A;
            this.B = B;
            this.C = C;
            this.twoB = shiftLeftBits(B, 1);
            this.aPrimes = aPrimes;
            this.aExponents = aExponents;
        }
        SignedMag evaluateAt(long t) {
            SignedMag result = SignedMag.positive(A);
            result = result.multiplySignedLong(t).add(SignedMag.positive(twoB));
            result = result.multiplySignedLong(t).add(C);
            return result;
        }
    }
    private static final double LOG_SCALE = 64.0;
    private static QSFactorBase buildFactorBase(long[] n, int bound) {
        int[] candidates = sieve(bound);
        boolean[] acceptances = new boolean[candidates.length];
        int accepted = 0;
        for (int i = 0; i < candidates.length; i++) {
            int p = candidates[i];
            if (p == 2) {
                if(isEvenMag(n)) throw new FactorFoundException(TWO_MAG);
                acceptances[i] = true;
                accepted++;
                continue;
            }
            int nModP = (int) modSmall(n, p);
            if (nModP == 0)
                throw new FactorFoundException(new long[]{p});
            if (legendreSymbol(nModP, p) == 1){
                acceptances[i] = true;
                accepted++;
            }
        }
        int[] primes = new int[accepted];
        int[] logPrimes = new int[accepted];
        int[] root1 = new int[accepted];
        int[] root2 = new int[accepted];
        int out = 0;
        for (int k = 0; k < acceptances.length; k++) {
            if(!acceptances[k]) continue;
            int p = candidates[k];
            int nModP = (int) modSmall(n, p);
            if (p == 2) {
                primes[out] = 2;
                logPrimes[out] = (int)Math.round(Math.log(2.0)*LOG_SCALE);
                // Since N is odd after trial division, N ≡ 1 mod 2.
                root1[out] = 1;
                root2[out] = 1;
                out++;
                continue;
            }
            int r = tonelliShanks(nModP, p);
            primes[out] = p;
            logPrimes[out] = (int)Math.round(Math.log(p)*LOG_SCALE);
            root1[out] = r;
            root2[out] = p - r;
            out++;
        }
        return new QSFactorBase(primes, logPrimes, root1, root2);
    }
    private static int[] buildFactorBaseIndexMap(QSFactorBase factorBase) {
        int max = factorBase.primes[factorBase.size - 1];
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
    private static int legendreSymbol(long a, long p) {
        a %= p;
        if (a < 0) a += p;
        if (a == 0) return 0;
        long r = powMod(a, (p - 1) >>> 1, p);
        if (r == 1) return 1;
        if (r == p - 1) return -1;
        throw new ArithmeticException("Invalid Legendre result");
    }
    private static int legendreSymbol(long[] a, long[] b) {
        if (isEvenMag(b) || !isProbablyPrimeSpecialMag(b))
            throw new IllegalArgumentException("Legendre symbol requires an odd prime modulus");
        long[] exponent = shiftRightBits(subtractSmallMag(b, 1), 1);
        long[] m = new MontgomeryReducer(b).powMod(modMagnitudes(a, b), exponent);
        if (isZeroArray(m))
            return 0;
        if (equalsArray(m, ONE_MAG))
            return 1;
        if (equalsArray(m, subtractSmallMag(b, 1)))
            return -1;
        throw new IllegalStateException("Invalid Legendre result; " + new LargeInteger(b, false) + " may not be prime");
    }
    public int legendreSymbol(LargeInteger other) {
        if (other.negative)
            throw new IllegalArgumentException("Error: Legendre symbol only defined for positive odd denominators");
        int result = 1;
        if (negative)
            if (other.mod(THREE).equals(ONE))
                result = -1;
        result *= legendreSymbol(limbs, other.limbs);
        return result;
    }
    public int jacobiSymbol(LargeInteger n) {
        if (n.negative || n.isEven())
            throw new IllegalArgumentException("Jacobi denominator must be positive and odd");
        LargeInteger a = (LargeInteger) mod(n);
        int result = 1;
        while (a.doesntEqual(ZERO)) {
            while (a.isEven()) {
                a = a.longDivision(TWO);
                long nMod8 = modSmall(n.limbs, 8);
                if (nMod8 == 3 || nMod8 == 5)
                    result = -result;
            }
            LargeInteger temp = a;
            a = n;
            n = temp;
            if (a.mod(FOUR).equals(THREE) && n.mod(FOUR).equals(THREE))
                result = -result;
            a = (LargeInteger) a.mod(n);
        }
        return n.equals(ONE) ? result : 0;
    }
    private static int tonelliShanks(int n, int p) {
        if (p == 2) return n & 1;
        // Fast path: p ≡ 3 mod 4
        if ((p & 3) == 3)
            return (int)powMod(n, (p + 1) >>> 2, p);
        // Full Tonelli-Shanks
        long q = p - 1;
        int s = 0;
        while ((q & 1) == 0) {
            s++;
            q >>>= 1;
        }
        int z = 2;
        while (legendreSymbol(z, p) != -1) z++;
        long c = powMod(z, q, p);
        long x = powMod(n, (q + 1) >>> 1, p);
        long t = powMod(n, q, p);
        int m = s;

        while (t != 1) {
            int i = 1;
            long temp = (t * t) % p;
            while (temp != 1) {
                temp = (temp * temp) % p;
                i++;
                if (i == m)
                    throw new ArithmeticException("Tonelli-Shanks failed");
            }
            long b = c;
            for (int j = 0; j < m - i - 1; j++)
                b = (b * b) % p;
            x = (x * b) % p;
            t = (t * b % p) * b % p;
            c = (b * b) % p;
            m = i;
        }
        return (int)x;
    }
    private static void verifyBForA(long[] B, long[] A, long[] N, int mask) {
        long[] lhs = modMagnitudes(squareMagnitude(B), A);
        long[] rhs = modMagnitudes(N, A);
        if (!equalsArray(lhs, rhs))
            throw new AssertionError("Invalid SIQS B at mask=" + mask + " B^2 mod A=" + new LargeInteger(lhs, false) + " N mod A=" + new LargeInteger(rhs, false));
    }
    private static void sieve128Polynomials(long[] N, QSFactorBase factorBase, int[] fbIndex, ArrayList<QSRelationFull> relations, PartialGraph partials) {
        int fbSize = factorBase.size;
        int columnCount = fbSize + 1; // negative column + real factor-base primes
        int globalPMax = factorBase.primes[fbSize - 1];
        long largePrimeBound = Math.multiplyExact(100L, globalPMax);
        double filterAllowance = (Math.log(largePrimeBound)) * 27.0/32.0;
        long[] sqrt2N = sqrtCeilMag(shiftLeftBits(N, 1));
        int count = 1;
        int bPolys = 1;
        while (bPolys < fbSize/ bPolys) {
            count++;
            bPolys <<= 1;
        }
        count = Math.clamp(count, 4, 8);
        ACoefficient AR = chooseAByScoring(sqrt2N, factorBase, count, FAST_RNG.split());
        verifyACoefficient(AR, factorBase);
        long[] A = AR.A;
        AFactor[] arPrimes = AR.primes;
        long[] halfMag = divideMagnitudes(sqrt2N, A)[0];
        if (halfMag.length > 1) throw new AssertionError("A is too small");
        long half = halfMag[0];
        long startT = -half;
        int interval = Math.toIntExact(2 * half);
        AStats plan = makeAPlan(A, sqrt2N, factorBase, count, largePrimeBound);
        // Precompute values depending only on A.
        int[] invAModP = new int[fbSize];
        boolean[] active = new boolean[fbSize];
        for (int j = 0; j < fbSize; j++) {
            int p = factorBase.primes[j];
            if (p <= 2) {
                active[j] = false;
                continue;
            }
            long aModP = modSmall(A, p);
            if (aModP == 0) {
                active[j] = false;
                continue;
            }
            active[j] = true;
            invAModP[j] = (int)inverseModSmall((int) aModP, p);
        }
        double[] invPDouble = new double[fbSize];
        for (int j = 0; j < fbSize; j++)
            invPDouble[j] = 1.0 / factorBase.primes[j];
        long[][] bTerms = new long[count][];
        for (int i = 0; i < count; i++) {
            int q = arPrimes[i].p;
            int root = arPrimes[i].root;
            long[] Ai = A.clone();
            divideMagExactInPlace(Ai, q);
            long aiModQ = modSmall(Ai, q);
            long inv = inverseModSmall((int) aiModQ, q);
            long coefficient = (inv * root) % q;
            // coefficient < q and Ai = A/q, so this is already < A.
            bTerms[i] = multiplySmallMag(Ai, coefficient);
        }
        long[] B = ZERO_MAG;
        for (long[] term : bTerms) {
            B = addMagnitudes(B, term);
            if (compareArray(B, A) >= 0)
                B = subtractMagnitudes(B, A);
        }
        int[] score = new int[interval];
        int[] startTModP = new int[fbSize];
        for (int j = 0; j < fbSize; j++)
            startTModP[j] = Math.floorMod(startT, factorBase.primes[j]);
        int[] pos1 = new int[fbSize];
        int[] pos2 = new int[fbSize];
        long t0 = System.nanoTime();
        for(int j = 0; j < fbSize; j++){
            if(!active[j]) continue;
            int p = factorBase.primes[j];
            int bMod = (int)modSmall(B, p);
            int invA = invAModP[j];
            double invP = invPDouble[j];
            int d1 = factorBase.root1[j] - bMod;
            if (d1 < 0) d1 += p;
            int d2 = factorBase.root2[j] - bMod;
            if (d2 < 0) d2 += p;
            long x1 = (long)d1 * invA;
            long q1 = (long)(x1 * invP);
            long r1 = x1 - q1 * p;
            long x2 = (long)d2 * invA;
            long q2 = (long)(x2 * invP);
            long r2 = x2 - q2 * p;
            if (r1 >= p) r1 -= p;
            else if (r1 < 0) r1 += p;
            if (r2 >= p) r2 -= p;
            else if (r2 < 0) r2 += p;
            x1 = r1 - startTModP[j];
            if (x1 < 0) x1 += p;
            x2 = r2 - startTModP[j];
            if (x2 < 0) x2 += p;
            pos1[j] = (int)x1;
            pos2[j] = (int)x2;
        }
        long rootCalcTime = System.nanoTime() - t0;
        plan.rootCalcTime += rootCalcTime;
        plan.sieveNs += rootCalcTime;
        short[] dense = new short[columnCount];
        long[][] twoTerms = new long[bTerms.length][];
        for (int i = 0; i < bTerms.length; i++)
            twoTerms[i] = modAdd(bTerms[i], bTerms[i], A);
        long[] aExponents = AR.aExponents;
        int nonzero = 0;
        for (long e : aExponents)
            if (e != 0) nonzero++;
        SparseExponents aSparse = new SparseExponents(new int[nonzero], new short[nonzero]);
        int id = 0;
        for (int j = 0; j < aExponents.length; j++) {
            long e = aExponents[j];
            if (e == 0) continue;
            if (e > Short.MAX_VALUE)
                throw new ArithmeticException("A exponent too large");
            aSparse.index[id] = j + 1;
            aSparse.count[id] = (short)e;
            id++;
        }
        int cachedBits = arPrimes.length-1;
        int[] grayDelta = new int[cachedBits * fbSize];
        for (int bit = 0; bit < cachedBits; bit++) {
            long[] step = twoTerms[bit+1];
            int off = bit * fbSize;
            for (int j = 0; j < fbSize; j++) {
                if (!active[j]) continue;
                int p = factorBase.primes[j];
                int stepMod = (int) modSmall(step, p);
                long z = (long) stepMod * invAModP[j];
                long q = (long) (z * invPDouble[j]);
                int delta = (int) (z - q * p);
                if (delta >= p) delta -= p;
                else if (delta < 0) delta += p;
                grayDelta[off + j] = delta;
            }
        }
        int prevGray = 0;
        for (int mask = 0; mask < 1 << (count-1); mask++) {
            int gray = mask ^ (mask >>> 1);
            long[] oldB = B;
            if (mask != 0) {
                int changed = gray ^ prevGray;
                int bit = Integer.numberOfTrailingZeros(changed);
                long[] step = twoTerms[bit+1];
                boolean wrapped;
                boolean nowNegative = ((gray >>> bit) & 1) != 0;
                if (nowNegative){ // +T_i -> -T_i
                    wrapped = compareArray(oldB, step) < 0;
                    B = modSubtract(oldB, step, A);
                }else{ // -T_i -> +T_i
                    B = modAdd(oldB, step, A);
                    wrapped = compareArray(B, oldB) < 0;
                }
                t0 = System.nanoTime();
                for(int j = 0; j < fbSize; j++){
                    if(!active[j]) continue;
                    int p = factorBase.primes[j];
                    int oldR1 = pos1[j];
                    int oldR2 = pos2[j];
                    int delta;
                    if (bit < cachedBits) delta = grayDelta[bit * fbSize + j];
                    else {
                        int stepMod = (int) modSmall(step, p);
                        long z = (long) stepMod * invAModP[j];
                        long q = (long) (z * invPDouble[j]);
                        delta = (int)(z - q * p);
                        if (delta >= p) delta -= p;
                        else if (delta < 0) delta += p;
                    }
                    int predicted1;
                    int predicted2;
                    if (nowNegative) {
                        predicted1 = oldR1 + delta;
                        predicted2 = oldR2 + delta;
                        if (predicted1 >= p) predicted1 -= p;
                        if (predicted2 >= p) predicted2 -= p;
                        if (wrapped) {
                            if (--predicted1 < 0) predicted1 += p;
                            if (--predicted2 < 0) predicted2 += p;
                        }
                    }else{
                        predicted1 = oldR1 - delta;
                        predicted2 = oldR2 - delta;
                        if (predicted1 < 0) predicted1 += p;
                        if (predicted2 < 0) predicted2 += p;
                        if (wrapped) {
                            if (++predicted1 >= p) predicted1 -= p;
                            if (++predicted2 >= p) predicted2 -= p;
                        }
                    }
                    pos1[j] = predicted1;
                    pos2[j] = predicted2;
                }
                rootCalcTime = System.nanoTime() - t0;
                plan.rootCalcTime += rootCalcTime;
                plan.sieveNs += rootCalcTime;
            }
            prevGray = gray;
            verifyBForA(B, A, N, gray);
            t0 = System.nanoTime();
            SignedMag numeratorC = SignedMag.positive(squareMagnitude(B)).add(SignedMag.negative(N));
            SignedMag C = new SignedMag(divideExactMag(numeratorC.mag, A), numeratorC.negative);
            MPQSPolynomial poly = new MPQSPolynomial(A, B, C, arPrimes, aExponents);
            long polyGenTime = System.nanoTime() - t0;
            //log sieve
            long sieveStart = System.nanoTime();
            Arrays.fill(score, 0);
            t0 = System.nanoTime();
            long smallSieve = System.nanoTime();
            int sieveIndex = 0;
            while (sieveIndex < fbSize && factorBase.primes[sieveIndex] < interval) {
                if (!active[sieveIndex]){
                    sieveIndex++;
                    continue;
                }
                int p = factorBase.primes[sieveIndex];
                int logP = factorBase.logPrimes[sieveIndex];
                int x1 = pos1[sieveIndex];
                int x2 = pos2[sieveIndex];
                while(x1 < score.length && x2 < score.length){
                    score[x1] += logP;
                    score[x2] += logP;
                    x1 += p;
                    x2 += p;
                }
                if (x1 < score.length)
                    score[x1] += logP;
                if (x2 < score.length)
                    score[x2] += logP;
                sieveIndex++;
            }
            smallSieve = System.nanoTime()-smallSieve;
            long largeSieve = System.nanoTime();
            while(sieveIndex < fbSize){
                if (!active[sieveIndex]){
                    sieveIndex++;
                    continue;
                }
                int logP = factorBase.logPrimes[sieveIndex];
                int x1 = pos1[sieveIndex];
                int x2 = pos2[sieveIndex];
                if(x1 < interval)
                    score[x1] += logP;
                if(x2 < interval)
                    score[x2] += logP;
                sieveIndex++;
            }
            largeSieve = System.nanoTime()-largeSieve;
            plan.logSieveNs += System.nanoTime() - t0;
            plan.smallLogSieveNs += smallSieve;
            plan.largeLogSieveNs += largeSieve;
            long totalSieveTime = System.nanoTime() - sieveStart;
            //attempt after sieve
            double logQThreshold = plan.logQEdge;
            plan.scanned += interval;
            long attemptStart = System.nanoTime();
            Arrays.fill(dense, (short) 0);
            for (int idx = 0; idx < interval; idx++) {
                double value = score[idx]/LOG_SCALE + filterAllowance;
                if (value < logQThreshold) continue;
                plan.firstFilterHits++;
                long t = startT + idx;
                SignedMag currentQ = poly.evaluateAt(t);
                double actualLogQ = logMag(currentQ.mag);
                if (value < actualLogQ) continue;
                plan.secondFilterHits++;
                if(isZeroArray(currentQ.mag)){
                    plan.observeRelationAdd(RelationAddKind.NONE);
                    continue;
                }
                // A contribution: column 0 is negative, factorBase[j] lives at column j + 1.
                Arrays.fill(dense, (short) 0);
                if (currentQ.negative)
                    incExp(dense, 0);
                long[] remaining = currentQ.mag;
                long[] currentX = multiplyMagnitudeBySignedLong(A, t).add(SignedMag.positive(poly.B)).modMag(N);
                if (isOneArray(remaining)){
                    relations.add(new QSRelationFull(new long[][]{currentX}, addSparse(compress(dense), aSparse), columnCount, new long[0][]));
                    plan.observeRelationAdd(RelationAddKind.FULL);
                    continue;
                }
                long tested = 0;
                for (int j = 0; j < fbSize; j++) {
                    tested++;
                    int p = factorBase.primes[j];
                    while(modSmall(remaining, p) == 0){
                        incExp(dense, j+1);
                        divideMagExactInPlace(remaining, p);
                    }
                    remaining = trimLeadingZeros(remaining);
                    if (remaining.length == 1) {
                        long r = remaining[0];
                        long pp = (long) p * p;
                        if (Long.compareUnsigned(pp, r) > 0) break;
                    }
                }
                plan.observeFactorTests(tested);
                /*
                 * If the residual is itself a factor-base prime,
                 * incorporate it before compressing the exponent vector.
                 */
                if (remaining.length == 1) {
                    int index = factorBaseIndexOf(fbIndex, remaining[0]);
                    if (index >= 0) {
                        incExp(dense, index + 1);
                        remaining = ONE_MAG;
                    }
                }
                SparseExponents sparse = addSparse(compress(dense), aSparse);
                long[][] xValues = {currentX};
                /*
                 * Fully factor-base smooth.
                 */
                if (isOneArray(remaining)){
                    relations.add(new QSRelationFull(xValues, sparse, columnCount, new long[0][]));
                    plan.observeRelationAdd(RelationAddKind.FULL);
                    continue;
                }
                /*
                 * A single-limb residual can be:
                 * 1. A factor-base prime that was not consumed for some reason.
                 * 2. A single large prime.
                 * 3. A product of two large primes.
                 */
                if (remaining.length == 1) {
                    long residual = remaining[0];
                    int factorBaseIndex = factorBaseIndexOf(fbIndex, residual);
                    if (factorBaseIndex >= 0) {
                        incExp(sparse.count(), factorBaseIndex + 1);
                        relations.add(new QSRelationFull(xValues, sparse, columnCount, new long[0][]));
                        plan.observeRelationAdd(RelationAddKind.FULL);
                        continue;
                    }
                    /*
                     * Single-large-prime relation:
                     * globalPMax < residual <= largePrimeBound
                     */
                    if (unsignedInLargePrimeRange(residual, globalPMax, largePrimeBound)){
                        plan.observeRelationAdd(addQSPartialRelation(QSRelationPartial.single(xValues, sparse, residual),  relations, partials, columnCount));
                        continue;
                    }
                }
                /*
                 * A valid double-large-prime residual cannot exceed L².
                 * Do this test for multi-limb residuals as well, because L² may require
                 * two limbs even when each individual large prime fits in one limb.
                 */
                long[] largePrimeBoundSquared = squareMagnitude(new long[]{largePrimeBound});
                long[] pMaxSquared = squareMagnitude(new long[] { globalPMax });
                if (compareArray(remaining, largePrimeBoundSquared) > 0 || compareArray(pMaxSquared, remaining) >= 0){
                    plan.observeRelationAdd(RelationAddKind.NONE);
                    continue;
                }
                /*
                 * Avoid trying to split a residual that is itself prime.
                 * A prime residual larger than largePrimeBound cannot be accepted.
                 */
                if (isProbablyPrimeSpecialMag(remaining)){
                    plan.observeRelationAdd(RelationAddKind.NONE);
                    continue;
                }
                long[] factor = findEasyResidualFactor(remaining);
                if (!isNontrivialFactor(factor, remaining)){
                    plan.observeRelationAdd(RelationAddKind.NONE);
                    continue;
                }
                long[] otherFactor = divideExactMag(remaining, factor);
                /*
                 * The unified partial graph currently stores vertices as unsigned longs,
                 * so both factors must be single-limb.
                 */
                if (factor.length != 1 || otherFactor.length != 1){
                    plan.observeRelationAdd(RelationAddKind.NONE);
                    continue;
                }
                long q1 = factor[0];
                long q2 = otherFactor[0];
                if (!unsignedInLargePrimeRange(q1, globalPMax, largePrimeBound) || !unsignedInLargePrimeRange(q2, globalPMax, largePrimeBound)){
                    plan.observeRelationAdd(RelationAddKind.NONE);
                    continue;
                }
                /*
                 * The factor finder may return a composite divisor.
                 */
                if (!isProbablyPrimeSpecialMag(factor) || !isProbablyPrimeSpecialMag(otherFactor)){
                    plan.observeRelationAdd(RelationAddKind.NONE);
                    continue;
                }
                /*
                 * residual = q² already has even large-prime parity, so this is a full
                 * relation rather than a graph edge.
                 */
                if (q1 == q2){
                    relations.add(new QSRelationFull(xValues, sparse, columnCount, new long[][]{new long[]{q1}}));
                    plan.observeRelationAdd(RelationAddKind.FULL);
                }else plan.observeRelationAdd(addQSPartialRelation(QSRelationPartial.pair(xValues, sparse, q1, q2), relations, partials, columnCount));
            }
            long relationTime = System.nanoTime() - attemptStart;
            plan.finishPolynomial(totalSieveTime, relationTime, polyGenTime);
        }
        System.out.println("A used: " + new LargeInteger(A, false));
        plan.printAStats(factorBase.primes.length+30, relations.size());
    }
    private static double weightedAverage(double first, double firstWeight, double second, double secondWeight) {
        double totalWeight = firstWeight + secondWeight;
        if (totalWeight == 0.0) return 0.0;
        return (first * firstWeight + second * secondWeight) / totalWeight;
    }
    private record RelationYieldEstimate(double fullPerPoly, double singlePartialHitsPerPoly, double singlePartialMergesPerPoly, double doublePartialHitsPerPoly, double doublePartialMergesPerPoly, double usefulRowsPerPoly) {
        static RelationYieldEstimate of(double fullHitsPerPoly, double singlePartialHitsPerPoly, double singlePartialMergesPerPoly, double doublePartialHitsPerPoly, double doublePartialMergesPerPoly) {
            double usableRowsPerPoly = fullHitsPerPoly + singlePartialMergesPerPoly + doublePartialMergesPerPoly;
            return new RelationYieldEstimate(fullHitsPerPoly, singlePartialHitsPerPoly, singlePartialMergesPerPoly, doublePartialHitsPerPoly, doublePartialMergesPerPoly, usableRowsPerPoly);
        }
        private static RelationYieldEstimate estimateRelationYieldPerPolynomial(long[] A, long[] halfMag, int factorBasePMax, long largePrimeBound) {
            if (isZeroArray(halfMag) || halfMag.length != 1 || halfMag[0] <= 0 || factorBasePMax <= 1 || largePrimeBound <= factorBasePMax)
                return new RelationYieldEstimate(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            long half = halfMag[0];
            double interval = 2.0 * half;
            double logA = logMag(A);
            double logM = Math.log(half);
            double logQ = logA + 2.0 * logM;
            double logB = Math.log(factorBasePMax);
            double logLP = Math.log(largePrimeBound);
            double fullProbability = roughDickman(logQ / logB);
            double lpSmoothProbability = roughDickman(logQ / logLP);
            double rawFullHits = interval * fullProbability;
            double rawAcceptedPartialPool = interval * Math.max(0.0, lpSmoothProbability - fullProbability);
            double fullCalibration = 0.06;
            double partialCalibration = 0.02;
            double fullHits = fullCalibration * rawFullHits;
            double calibratedPartialPool = partialCalibration * rawAcceptedPartialPool;
            /*
             * Use the passed bounds to estimate how the accepted partial pool
             * splits between one-large-prime and two-large-prime residuals.
             *
             * This predicts hits only, not graph merges.
             */
            double singleShare = estimateSinglePartialShare(logQ, logB,logLP);
            double singlePartialHits = calibratedPartialPool * singleShare;
            double doublePartialHits = calibratedPartialPool * (1.0 - singleShare);
            return RelationYieldEstimate.of(fullHits, singlePartialHits, 0.0, doublePartialHits,0.0);
        }
        private static double estimateSinglePartialShare(double logQ, double logB, double logLP) {
            double lpSpan = logLP - logB;
            if (!(lpSpan > 0.0)) return 1.0;
            /*
             * Wider LP ranges make accepted two-prime residuals more likely.
             * Scale the span against the typical logarithmic Q magnitude.
             */
            double normalizedSpan = lpSpan / Math.max(logQ - logB, 1e-12);
            return Math.clamp(1.0 - normalizedSpan, 0.10, 0.90);
        }
    }
    private static double logMag(long[] mag) {
        mag = trimLeadingZeros(mag);
        if (isZeroArray(mag))
            return Double.NEGATIVE_INFINITY;
        else if(isOneArray(mag)) return 0.0;
        return bitLength(mag)*Math.log(2.0);
    }
    private static void incExp(short[] dense, int column) {
        if (dense[column] == Short.MAX_VALUE)
            throw new ArithmeticException("Exponent overflow at column " + column);
        dense[column]++;
    }
    private record PartialEdge(long from, long to, QSRelationPartial relation) {
        long other(long vertex) {
            if (vertex == from) return to;
            if (vertex == to) return from;
            throw new IllegalArgumentException("Vertex is not on this edge");
        }
    }
    private static final class PartialGraph {
        /*
         * Only forest edges are stored here. A cycle-closing edge is immediately
         * converted into a full relation and need not be retained.
         */
        final HashMap<Long, ArrayList<PartialEdge>> adjacency = new HashMap<>();
        final HashMap<Long, Long> parent = new HashMap<>();
        final HashMap<Long, Integer> rank = new HashMap<>();
        void ensureVertex(long vertex) {
            parent.putIfAbsent(vertex, vertex);
            rank.putIfAbsent(vertex, 0);
            adjacency.computeIfAbsent(vertex, ignored -> new ArrayList<>());
        }
        long find(long vertex) {
            ensureVertex(vertex);
            long p = parent.get(vertex);
            if (p != vertex) {
                p = find(p);
                parent.put(vertex, p);
            }
            return p;
        }
        boolean connected(long a, long b) {return find(a) == find(b);}
        void union(long a, long b) {
            long rootA = find(a);
            long rootB = find(b);

            if (rootA == rootB)
                return;

            int rankA = rank.get(rootA);
            int rankB = rank.get(rootB);

            if (rankA < rankB) parent.put(rootA, rootB);
            else if(rankA > rankB) parent.put(rootB, rootA);
            else {
                parent.put(rootB, rootA);
                rank.put(rootA, rankA + 1);
            }
        }
        void addForestEdge(QSRelationPartial relation) {
            long a = relation.vertex1;
            long b = relation.vertex2;
            ensureVertex(a);
            ensureVertex(b);
            PartialEdge edge = new PartialEdge(a, b, relation);
            adjacency.get(a).add(edge);
            adjacency.get(b).add(edge);
            union(a, b);
        }
        ArrayList<QSRelationPartial> findPartialPath(long start, long target) {
            ArrayDeque<Long> queue = new ArrayDeque<>();
            HashSet<Long> visited = new HashSet<>();
            HashMap<Long, Long> previousVertex = new HashMap<>();
            HashMap<Long, PartialEdge> previousEdge = new HashMap<>();
            queue.add(start);
            visited.add(start);
            while (!queue.isEmpty()) {
                long vertex = queue.removeFirst();
                if (vertex == target) break;
                ArrayList<PartialEdge> edges = adjacency.get(vertex);
                if (edges == null) continue;
                for (PartialEdge edge : edges) {
                    long next = edge.other(vertex);
                    if (!visited.add(next)) continue;
                    previousVertex.put(next, vertex);
                    previousEdge.put(next, edge);
                    queue.addLast(next);
                }
            }
            if (!visited.contains(target)) return null;
            ArrayList<QSRelationPartial> reversed = new ArrayList<>();
            long current = target;
            while (current != start) {
                PartialEdge edge = previousEdge.get(current);
                if (edge == null)
                    throw new IllegalStateException("Broken predecessor chain in partial graph");
                reversed.add(edge.relation);
                current = previousVertex.get(current);
            }
            Collections.reverse(reversed);
            return reversed;
        }
    }
    private static boolean unsignedInLargePrimeRange(long value, long globalPMax, long largePrimeBound) {return Long.compareUnsigned(value, globalPMax) > 0 && Long.compareUnsigned(value, largePrimeBound) <= 0;}
    private static long[] findEasyResidualFactor(long[] residual) {
        long[] factor = pollardPMinusOneEasy(residual);
        return isNontrivialFactor(factor, residual) ? factor : brentRhos(residual);
    }
    private static SparseExponents addSparse(SparseExponents a, SparseExponents b) {
        int i = 0, j = 0, k = 0;
        int[] tmpIndex = new int[a.index.length + b.index.length];
        short[] tmpCount = new short[tmpIndex.length];
        while (i < a.index.length || j < b.index.length) {
            if (j >= b.index.length || (i < a.index.length && a.index[i] < b.index[j])) {
                tmpIndex[k] = a.index[i];
                tmpCount[k] = a.count[i];
                i++;
            } else if (i >= a.index.length || b.index[j] < a.index[i]) {
                tmpIndex[k] = b.index[j];
                tmpCount[k] = b.count[j];
                j++;
            } else {
                int sum = a.count[i] + b.count[j];
                if (sum != 0) {
                    tmpIndex[k] = a.index[i];
                    tmpCount[k] = (short) sum;
                    k++;
                }
                i++;
                j++;
                continue;
            }
            k++;
        }
        return new SparseExponents(Arrays.copyOf(tmpIndex, k), Arrays.copyOf(tmpCount, k));
    }
    private static void incrementLargePrimeCount(HashMap<Long, Integer> counts, long vertex) {
        if (vertex == PARTIAL_ROOT) return;
        counts.merge(vertex, 1, Integer::sum);
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
    private static QSRelationFull combinePartialCycle(ArrayList<QSRelationPartial> cycle, int columnCount) {
        if (cycle.isEmpty())
            throw new IllegalArgumentException("Partial cycle cannot be empty");
        SparseExponents mergedExponents = new SparseExponents(new int[0], new short[0]);
        int xCount = 0;
        /*
         * Count how many times each large prime occurs among all edge endpoints.
         */
        HashMap<Long, Integer> largePrimeCounts = new HashMap<>();
        for (QSRelationPartial partial : cycle) {
            mergedExponents = addSparse(mergedExponents, partial.exponents);
            xCount += partial.xValues.length;
            incrementLargePrimeCount(largePrimeCounts, partial.vertex1);
            incrementLargePrimeCount(largePrimeCounts, partial.vertex2);
        }
        long[][] mergedXValues = new long[xCount][];
        int xIndex = 0;
        for (QSRelationPartial partial : cycle) {
            for (long[] x : partial.xValues)
                mergedXValues[xIndex++] = x;
        }
        /*
         * In a valid cycle every non-root vertex occurs an even number of times.
         * For each q^(2k), q^k belongs on the square-root side.
         */
        ArrayList<long[]> squareFactorList = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : largePrimeCounts.entrySet()) {
            long prime = entry.getKey();
            int count = entry.getValue();
            if (prime == PARTIAL_ROOT) continue;
            if ((count & 1) != 0)
                throw new IllegalStateException("Large prime has odd degree in reconstructed cycle: " + Long.toUnsignedString(prime) + " occurs " + count + " times");
            for (int i = 0; i < count / 2; i++)
                squareFactorList.add(new long[] { prime });
        }
        long[][] squareFactors = squareFactorList.toArray(new long[0][]);
        return new QSRelationFull(mergedXValues, mergedExponents, columnCount, squareFactors);
    }
    private static RelationAddKind addQSPartialRelation(QSRelationPartial partial, ArrayList<QSRelationFull> fullRelations, PartialGraph partialGraph, int columnCount) {
        long a = partial.vertex1;
        long b = partial.vertex2;
        /*
         * Self-loop: the same large prime appears twice in this one relation.
         * That is already a complete cycle of length one.
         */
        if (a == b) {
            ArrayList<QSRelationPartial> cycle = new ArrayList<>(1);
            cycle.add(partial);
            fullRelations.add(combinePartialCycle(cycle, columnCount));
            return RelationAddKind.SINGLE_PARTIAL_CYCLE;
        }
        if (!partialGraph.connected(a, b)) {
            partialGraph.addForestEdge(partial);
            return partial.isSinglePrime() ? RelationAddKind.SINGLE_PRIME_PARTIAL : RelationAddKind.DOUBLE_PRIME_PARTIAL;
        }
        ArrayList<QSRelationPartial> cycle = partialGraph.findPartialPath(a, b);
        if (cycle == null) throw new IllegalStateException("Partial graph reports connected vertices, but no forest path exists");
        cycle.add(partial);

        QSRelationFull full = combinePartialCycle(cycle, columnCount);
        fullRelations.add(full);
        boolean hasSingle = false;
        boolean hasDouble = false;
        for (QSRelationPartial edge : cycle) {
            if (edge.isSinglePrime()) hasSingle = true;
            else hasDouble = true;
        }
        if (hasSingle && hasDouble)
            return RelationAddKind.MIXED_PARTIAL_CYCLE;
        if (hasSingle)
            return RelationAddKind.SINGLE_PARTIAL_CYCLE;
        return RelationAddKind.DOUBLE_PARTIAL_CYCLE;
    }
    //Matrix steps from here
    private static ArrayList<SparseMatrixRow> singletonFilter(ArrayList<SparseMatrixRow> rows, int columnCount, ArrayList<QSRelationFull> relations, QSFactorBase factorBase, long[] N) {
        int[] weight = new int[columnCount];
        // First count incidence sizes
        int[] incidenceCount = new int[columnCount];

        for (SparseMatrixRow row : rows) {
            if (!row.alive) continue;
            for (int c : row.cols) {
                weight[c]++;
                incidenceCount[c]++;
            }
        }
        // Compact column -> row adjacency
        int[] offsets = new int[columnCount + 1];
        for (int c = 0; c < columnCount; c++)
            offsets[c + 1] = offsets[c] + incidenceCount[c];
        int[] incidentRows = new int[offsets[columnCount]];
        int[] cursor = offsets.clone();
        for (int r = 0; r < rows.size(); r++) {
            SparseMatrixRow row = rows.get(r);
            if (!row.alive) continue;
            for (int c : row.cols)
                incidentRows[cursor[c]++] = r;
        }
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int c = 0; c < columnCount; c++) {
            if (weight[c] == 1)
                queue.addLast(c);
        }
        while (!queue.isEmpty()) {
            int c = queue.removeFirst();
            if (weight[c] != 1) continue;
            int r = -1;
            for (int k = offsets[c]; k < offsets[c + 1]; k++) {
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
                if (w == 1) queue.addLast(c2);
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
    private static int findOneAliveRowContaining(ArrayList<SparseMatrixRow> rows, IntList[] incidence, int column) {
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
    private static int[] findTwoAliveRowsContaining(
            ArrayList<SparseMatrixRow> rows,
            IntList[] incidence,
            int column
    ) {
        IntList list = incidence[column];
        if (list == null)
            return null;
        int first = -1;
        int second = -1;
        for (int i = 0; i < list.size; i++) {
            int r = list.data[i];
            if (!rows.get(r).alive) continue;
            if (first < 0) first = r;
            else if (second < 0) second = r;
            else return null; // current live degree is > 2
        }
        return second < 0 ? null : new int[] {first, second};
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
    private static ArrayList<SparseMatrixRow> degreeTwoMergeFilter(ArrayList<SparseMatrixRow> rows, int columnCount, ArrayList<QSRelationFull> relations, QSFactorBase factorBase, long[] N) {
        IntList[] incidence = new IntList[columnCount];
        long aliveRows = 0;
        long incidences = 0;

        for (int r = 0; r < rows.size(); r++) {
            SparseMatrixRow row = rows.get(r);
            if (!row.alive) continue;
            aliveRows++;
            for (int c : row.cols) {
                incidences++;
                if (incidence[c] == null)
                    incidence[c] = new IntList();
                incidence[c].add(r);
            }
        }
        int nonNullColumns = 0;
        for (IntList list : incidence)
            if (list != null)
                nonNullColumns++;
        System.out.println("aliveRows=" + aliveRows + " incidences=" + incidences + " nonNullColumns=" + nonNullColumns);
        boolean changed;
        do {
            changed = false;
            int[] weight = new int[columnCount];
            for (SparseMatrixRow row : rows) {
                if (!row.alive) continue;
                for (int c : row.cols)
                    weight[c]++;
            }
            // Same singleton phase.
            for (int c = 0; c < columnCount; c++) {
                if (weight[c] != 1) continue;
                int r = findOneAliveRowContaining(rows, incidence, c);
                if (r >= 0) {
                    rows.get(r).alive = false;
                    changed = true;
                }
            }
            // Same recomputation.
            weight = new int[columnCount];
            for (SparseMatrixRow row : rows) {
                if (!row.alive) continue;
                for (int c : row.cols)
                    weight[c]++;
            }
            // Same degree-2 pass.
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
                int newRowIndex = rows.size();
                rows.add(new SparseMatrixRow(mergedCols, mergedDeps));
                // CRITICAL: newly created row must immediately
                // participate in subsequent findTwo calls.
                for (int c2 : mergedCols) {
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
        } while (changed);
        ArrayList<SparseMatrixRow> survivors = new ArrayList<>();
        for (SparseMatrixRow row : rows) {
            if (row.alive)
                survivors.add(row);
        }
        return survivors;
    }
    private static final class SparseMatrixRow {
        int[] cols; // sorted odd columns
        int[] deps; // sorted original relation indices xor-combination
        boolean alive;

        SparseMatrixRow(int[] cols, int relationIndex) {
            this.cols = cols;
            this.deps = new int[]{relationIndex};
            this.alive = true;
        }

        SparseMatrixRow(int[] cols, int[] deps) {
            this.cols = cols;
            this.deps = deps;
            this.alive = true;
        }
    }

    private static ArrayList<SparseMatrixRow> buildSparseRows(ArrayList<QSRelationFull> relations) {
        ArrayList<SparseMatrixRow> rows = new ArrayList<>(relations.size());
        for (int i = 0; i < relations.size(); i++) {
            int[] cols = oddColumnsFromPacked(relations.get(i).parity);
            rows.add(new SparseMatrixRow(cols, i));
        }
        return rows;
    }
    private static int[] oddColumnsFromPacked(long[] parity) {
        int count = 0;
        for (long word : parity)
            count += Long.bitCount(word);
        int[] cols = new int[count];
        int out = 0;
        for (int w = 0; w < parity.length; w++) {
            long word = parity[w];
            while (word != 0L) {
                int bit = Long.numberOfTrailingZeros(word);
                cols[out++] = (w << 6) + bit;
                word &= word - 1;
            }
        }
        return cols;
    }
    private static SparseMatrixSetup buildQSMatrix(ArrayList<QSRelationFull> relations, QSFactorBase factorBase, long[] N) {
        int columnCount = factorBase.size + 1;
        long t0 = System.nanoTime();
        ArrayList<SparseMatrixRow> sparseRows = buildSparseRows(relations);
        long sparseRowNanos = System.nanoTime()-t0;

        t0 = System.nanoTime();
        sparseRows = singletonFilter(sparseRows, columnCount, relations, factorBase, N);
        long singleton1Nanos = System.nanoTime()-t0;
        t0 = System.nanoTime();
        sparseRows = degreeTwoMergeFilter(sparseRows, columnCount, relations, factorBase, N);
        long doubletonNanos = System.nanoTime()-t0;
        t0 = System.nanoTime();
        sparseRows = singletonFilter(sparseRows, columnCount, relations, factorBase, N);
        long singleton2Nanos = System.nanoTime()-t0;
        int rowCount = sparseRows.size();
        int[][] rowColumns = new int[rowCount][];
        int[][] rowDeps = new int[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            SparseMatrixRow row = sparseRows.get(i);
            rowColumns[i] = row.cols;
            rowDeps[i] = row.deps;
        }
        GF2SparseMatrix matrix = new GF2SparseMatrix(rowCount, columnCount, rowColumns);
        System.out.println("Time for building sparse rows: " + formatSeconds(sparseRowNanos));
        System.out.println("Time for first singleton filter: " + formatSeconds(singleton1Nanos));
        System.out.println("Time for doubleton filter: " + formatSeconds(doubletonNanos));
        System.out.println("Time for second singleton filter: " + formatSeconds(singleton2Nanos));
        return new SparseMatrixSetup(matrix, rowDeps);
    }
    private record SparseMatrixSetup(GF2SparseMatrix matrix, int[][] rowDeps) {}
    private record GF2SparseMatrix(int rowCount, int columnCount, int[][] rowColumns) {
        long[] multiplyTransposeVector(long[] packedRows) {
            int expectedLength = (rowCount + 63) >>> 6;
            if (packedRows.length != expectedLength)
                throw new IllegalArgumentException("Expected packed row vector length " + expectedLength + ", got " + packedRows.length);
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
                    for (int column : rowColumns[row])
                        packedColumns[column >>> 6] ^= 1L << (column & 63);
                }
            }
            return packedColumns;
        }
        long[] multiplyMMtBlock64(long[] rowBlock) {return multiplyBlock64(multiplyTransposeBlock64(rowBlock));}
        // out = A^T * x
        // x.length == rows
        // out.length == cols
        long[] multiplyTransposeBlock64(long[] rowBlock) {
            if (rowBlock.length != rowCount)
                throw new IllegalArgumentException("Expected block length " + rowCount + ", got " + rowBlock.length);
            long[] colBlock = new long[columnCount];
            for (int r = 0; r < rowCount; r++) {
                long lanes = rowBlock[r];
                for (int c : rowColumns[r])
                    colBlock[c] ^= lanes;
            }
            return colBlock;
        }

        long[] multiplyBlock64(long[] colBlock) {
            if (colBlock.length != columnCount)
                throw new IllegalArgumentException("Expected block length " + columnCount + ", got " + colBlock.length);
            long[] rowBlock = new long[rowCount];
            for (int r = 0; r < rowCount; r++) {
                long laneParities = 0L;
                for (int c : rowColumns[r]) laneParities ^= colBlock[c];
                rowBlock[r] = laneParities;
            }
            return rowBlock;
        }

        static long[] randomRowBlock(int rowCount, SplittableRandom rng) {
            long[] block = new long[rowCount];
            for (int r = 0; r < rowCount; r++)
                block[r] = rng.nextLong();
            return block;
        }
        static final class RightMulTable64 {
            final long[][] table = new long[8][256];
            RightMulTable64(GF2Matrix64 matrix) {
                for (int byteIndex = 0; byteIndex < 8; byteIndex++) {
                    int base = byteIndex << 3;
                    for (int value = 1; value < 256; value++) {
                        int lowBit = Integer.numberOfTrailingZeros(value);
                        int without = value & (value - 1);
                        table[byteIndex][value] = table[byteIndex][without] ^ matrix.rows[base + lowBit];
                    }
                }
            }
        }
        static long[] rightMultiplyBlock64(long[] block, GF2Matrix64 matrix) {
            RightMulTable64 t = new RightMulTable64(matrix);
            long[] result = new long[block.length];
            for (int i = 0; i < block.length; i++) {
                long x = block[i];
                result[i] = t.table[0][(int)(x & 0xff)]
                                ^ t.table[1][(int)((x >>>  8) & 0xff)]
                                ^ t.table[2][(int)((x >>> 16) & 0xff)]
                                ^ t.table[3][(int)((x >>> 24) & 0xff)]
                                ^ t.table[4][(int)((x >>> 32) & 0xff)]
                                ^ t.table[5][(int)((x >>> 40) & 0xff)]
                                ^ t.table[6][(int)((x >>> 48) & 0xff)]
                                ^ t.table[7][(int)((x >>> 56) & 0xff)];
            }
            return result;
        }
        static void rightMultiplyBlock64XorInto(long[] dst, long[] block, GF2Matrix64 matrix) {
            RightMulTable64 t = new RightMulTable64(matrix);
            for (int i = 0; i < block.length; i++) {
                long x = block[i];
                dst[i] ^= t.table[0][(int)(x&0xff)]
                                ^ t.table[1][(int)((x >>>  8) & 0xff)]
                                ^ t.table[2][(int)((x >>> 16) & 0xff)]
                                ^ t.table[3][(int)((x >>> 24) & 0xff)]
                                ^ t.table[4][(int)((x >>> 32) & 0xff)]
                                ^ t.table[5][(int)((x >>> 40) & 0xff)]
                                ^ t.table[6][(int)((x >>> 48) & 0xff)]
                                ^ t.table[7][(int)((x >>> 56) & 0xff)];
            }
        }
        BlockLanczosResult blockLanczos(SplittableRandom rng) {
            long[] RIGHT = randomRowBlock(rowCount, rng);
            long[] initialV = multiplyMMtBlock64(RIGHT);
            long[] v0 = initialV.clone();
            long[] LEFT = new long[rowCount];
            BlockLanczosState state = new BlockLanczosState(initialV);
            for (int iter = 0; true; iter++) {
                boolean VERIFY_BLOCK_LANCZOS = iter == 0;
                long[] av = multiplyMMtBlock64(state.v);

                GF2Matrix64 gram = blockDot(state.v, av);
                if (gram.isZero())
                    return new BlockLanczosResult(LEFT, RIGHT, state.v, iter);
                GF2Elim64 elimination = getGf2Elim64(gram, state);
                GF2Matrix64 wInv = elimination.embeddedInverse;
                GF2Matrix64 overlap = blockDot(state.v, v0);
                GF2Matrix64 coefficients = wInv.multiply(overlap);
                xorInto(LEFT, rightMultiplyBlock64(state.v, coefficients));
                GF2Matrix64 identity = GF2Matrix64.identity();
                long selectedMask = elimination.selectedMask;
                GF2Matrix64 p = GF2Matrix64.diagonal(selectedMask);
                GF2Matrix64 h = blockDot(av, av);
                /*
                 * P is supposed to mean right-multiplication by the
                 * selected-lane mask. Verify that the specialized
                 * projection and general right multiplication agree.
                 */
                if (VERIFY_BLOCK_LANCZOS) {
                    long[] projectedByMask = BlockLanczosState.projectBlock(av, selectedMask);
                    long[] projectedByMatrix = rightMultiplyBlock64(av, p);
                    if(!equalsArray(projectedByMask, projectedByMatrix))
                        throw new AssertionError("projectBlock/rightMultiply mismatch on iteration " + iter);
                }

                GF2Matrix64 q = h.multiply(p).xor(gram);
                GF2Matrix64 d = identity.xor(wInv.multiply(q));
                GF2Matrix64 e = state.wInvPrev.multiply(gram).multiply(p);
                GF2Matrix64 f = state.wInvPrev2.multiply(identity.xor(state.gPrev.multiply(state.wInvPrev))).multiply(state.qPrev).multiply(p);
                /*
                 * Check the fundamental blockDot/right-multiplication
                 * convention using quantities we already need here.
                 */
                if (VERIFY_BLOCK_LANCZOS) {
                    GF2Matrix64 actualD = blockDot(state.v, rightMultiplyBlock64(av, d));
                    GF2Matrix64 expectedD = gram.multiply(d);
                    if(actualD.doesntEqual(expectedD))
                        throw new AssertionError("blockDot/rightMultiply convention mismatch on iteration " + iter);
                    GF2Matrix64 actualP = blockDot(av, BlockLanczosState.projectBlock(av, selectedMask));
                    GF2Matrix64 expectedP = h.multiply(p);
                    if(actualP.doesntEqual(expectedP))
                        throw new AssertionError("projection algebra mismatch on iteration " + iter);
                }
                /*
                 * Build the complete V_{i+1}.
                 */
                long[] nextV = BlockLanczosState.projectBlock(av, selectedMask);
                rightMultiplyBlock64XorInto(nextV, state.v, d);
                rightMultiplyBlock64XorInto(nextV, state.vPrev, e);
                rightMultiplyBlock64XorInto(nextV, state.vPrev2, f);
                /*
                 * Now compute A V_{i+1}.
                 */
                long[] aNextV = multiplyMMtBlock64(nextV);
                /*
                 * Verify the complete recurrence after nextV has actually
                 * been assembled, while state still refers to iteration i.
                 */
                if (VERIFY_BLOCK_LANCZOS) {
                    GF2Matrix64 actual = blockDot(state.v, aNextV);
                    GF2Matrix64 viAvPrev = blockDot(state.v, multiplyMMtBlock64(state.vPrev));
                    GF2Matrix64 viAvPrev2 = blockDot(state.v, multiplyMMtBlock64(state.vPrev2));
                    GF2Matrix64 predicted = h.multiply(p).xor(gram.multiply(d)).xor(viAvPrev.multiply(e)).xor(viAvPrev2.multiply(f));
                    if(actual.doesntEqual(predicted))
                        throw new AssertionError("Complete recurrence algebra mismatch on iteration " + iter);
                    if(actual.rowsAreNonzero(selectedMask))
                        throw new AssertionError("V_i selected subspace is not A-orthogonal to V_{i+1} on iteration " + iter);
                }

                state.vPrev2 = state.vPrev;
                state.vPrev = state.v;
                state.v = nextV;

                state.wInvPrev2 = state.wInvPrev;
                state.wInvPrev = wInv;

                state.gPrev = gram;
                state.qPrev = q;
                state.selectedMaskPrev = selectedMask;
                if (iter > rowCount + 64)
                    throw new IllegalStateException("Block Lanczos did not terminate");
            }
        }
        private static GF2Elim64 getGf2Elim64(GF2Matrix64 gram, BlockLanczosState state) {
            if (gram.isNotSymmetric())
                throw new AssertionError("V^T*A*V is not symmetric; blockDot or A multiplication is wrong");
            long requiredMask = activeLaneMask(state.v) & ~state.selectedMaskPrev;
            GF2Elim64 elimination = gram.eliminate64(requiredMask);
            if (elimination.rank == 0)
                throw new AssertionError("Elimination returned rank zero for nonzero Gram matrix");
            return elimination;
        }
        private Block128 multiplyTransposeBlock128(long[] lowBlock, long[] highBlock) {
            if (lowBlock.length != rowCount || highBlock.length != rowCount)
                throw new IllegalArgumentException("Row-block dimension mismatch");
            Block128 result = new Block128(columnCount);
            for (int row = 0; row < rowCount; row++) {
                long low = lowBlock[row];
                long high = highBlock[row];
                for (int col : rowColumns[row]) {
                    result.low[col] ^= low;
                    result.high[col] ^= high;
                }
            }

            return result;
        }
        long[][] extractLanczosDependencies(long[] d, long[] terminalV) {
            Block128 mtz = multiplyTransposeBlock128(d, terminalV);
            ArrayList<long[]> dependencies = new ArrayList<>();
            Combination128[] combinations = Combination128.eliminate128Columns(mtz.low, mtz.high);
            for (Combination128 combination : combinations) {
                long[] dependency = combineCandidateBlocksPacked(d, terminalV, combination.low, combination.high);
                long[] check = multiplyTransposeVector(dependency);
                for (long word : check)
                    if (word != 0L) throw new AssertionError("Invalid Block Lanczos dependency");
                dependencies.add(dependency);
            }
            return dependencies.toArray(long[][]::new);
        }
    }
    private static long activeLaneMask(long[] block) {
        long mask = 0L;
        for (long row : block)
            mask |= row;
        return mask;
    }
    private static final class BlockLanczosState {
        long[] v;
        long[] vPrev;
        long[] vPrev2;

        GF2Matrix64 wInvPrev;
        GF2Matrix64 wInvPrev2;

        GF2Matrix64 gPrev;
        GF2Matrix64 qPrev;

        long selectedMaskPrev;

        BlockLanczosState(long[] initialV) {
            int dimension = initialV.length;

            this.v = initialV;
            this.vPrev = new long[dimension];
            this.vPrev2 = new long[dimension];

            this.wInvPrev = GF2Matrix64.zero();
            this.wInvPrev2 = GF2Matrix64.zero();

            this.gPrev = GF2Matrix64.zero();
            this.qPrev = GF2Matrix64.zero();

            this.selectedMaskPrev = -1L;
        }
        static long[] projectBlock(long[] block, long selectedMask) {
            long[] result = new long[block.length];
            for (int i = 0; i < block.length; i++)
                result[i] = block[i] & selectedMask;
            return result;
        }
    }
    private record BlockLanczosResult(long[] LEFT, long[] RIGHT, long[] terminalV, int iterations) {}
    /**
     * @param selectedMask    Indices selected for S_i. Bit j is set when Gram column/row j is active.
     * @param inverse         Ordinary 64×64 inverse. Null unless rank == 64.
     * @param embeddedInverse S_i * (S_i^T Q S_i)^-1 * S_i^T. Always defined; zero matrix when rank == 0.
     * @param reduced         Optional but useful for debugging.
     */
    private record GF2Elim64(int rank, long selectedMask, GF2Matrix64 inverse, GF2Matrix64 embeddedInverse, GF2Matrix64 reduced) {}
    private record GF2Matrix64(long[] rows) {
        public static GF2Matrix64 zero(){return new GF2Matrix64(new long[64]);}
        static GF2Matrix64 diagonal(long mask) {
            GF2Matrix64 result = zero();
            for (long bits = mask; bits != 0L; bits &= bits - 1) {
                int lane = Long.numberOfTrailingZeros(bits);
                result.rows[lane] = 1L << lane;
            }
            return result;
        }
        boolean isZero(){
            for(long L: rows) if(L != 0) return false;
            return true;
        }
        static GF2Matrix64 identity() {
            long[] rows = new long[64];
            for (int i = 0; i < 64; i++)
                rows[i] = 1L << i;   // Must be 1L, not 1
            return new GF2Matrix64(rows);
        }
        public GF2Matrix64 {if (rows.length != 64) throw new IllegalArgumentException("Expected 64 rows");}
        GF2Matrix64 xor(GF2Matrix64 other) {
            long[] result = new long[64];
            for (int row = 0; row < 64; row++)
                result[row] = rows[row] ^ other.rows[row];
            return new GF2Matrix64(result);
        }
        GF2Elim64 eliminate64(long requiredMask) {
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
            if ((selectedMask & requiredMask) != requiredMask)
                throw new BlockLanczosBreakdownException("Could not include required Lanczos lanes: required=" + Long.toHexString(requiredMask) + ", selected=" + Long.toHexString(selectedMask));
            GF2Matrix64 embeddedInverse = embeddedPrincipalInverse(selectedMask);
            verifyEmbeddedInverse(selectedMask, embeddedInverse);
            GF2Matrix64 inverse = rank == 64 ? embeddedInverse : null;
            return new GF2Elim64(rank, selectedMask, inverse, embeddedInverse, new GF2Matrix64(reducedRows));
        }
        private GF2Matrix64 inverseFullRankDirect() {
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
            return new GF2Matrix64(right);
        }
        GF2Matrix64 embeddedPrincipalInverse(long selectedMask) {
            int rank = Long.bitCount(selectedMask);
            if (rank == 0) return zero();
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
            GF2Matrix64 compact = new GF2Matrix64(compactRows);
            GF2Matrix64 compactInverse = compact.inverseFullRankDirect();
            if (compactInverse == null)
                throw new AssertionError("Selected principal submatrix is singular: " + Long.toUnsignedString(selectedMask, 16));
            long[] embeddedRows = new long[64];
            for (int compactRow = 0; compactRow < rank; compactRow++) {
                int originalRow = selected[compactRow];
                long compactInverseRow = compactInverse.rows[compactRow];
                long destination = 0L;
                for (int compactColumn = 0; compactColumn < rank; compactColumn++) {
                    if ((compactInverseRow & (1L << compactColumn)) != 0L)
                        destination |= 1L << selected[compactColumn];
                }
                embeddedRows[originalRow] = destination;
            }
            return new GF2Matrix64(embeddedRows);
        }
        boolean equals(GF2Matrix64 other) {
            for(int i = 0; i < 64; i++) if(rows[i] != other.rows[i]) return false;
            return true;
        }
        boolean doesntEqual(GF2Matrix64 other){return !equals(other);}
        void verifyEmbeddedInverse(long selectedMask, GF2Matrix64 embeddedInverse) {
            GF2Matrix64 projector = GF2Matrix64.diagonal(selectedMask);
            GF2Matrix64 left = embeddedInverse.multiply(this).multiply(projector);
            GF2Matrix64 right = projector.multiply(this).multiply(embeddedInverse);
            if (left.doesntEqual(projector) || right.doesntEqual(projector))
                throw new AssertionError("Invalid embedded inverse");
            if (embeddedInverse.isNotSymmetric())
                throw new AssertionError("Embedded inverse of symmetric principal minor " + "is not symmetric");
        }
        GF2Matrix64 multiply(GF2Matrix64 other) {
            long[] out = new long[64];
            for (int i = 0; i < 64; i++) {
                long bits = rows[i];
                long r = 0L;
                while (bits != 0L) {
                    int j = Long.numberOfTrailingZeros(bits);
                    r ^= other.rows[j];
                    bits &= bits - 1;
                }
                out[i] = r;
            }
            return new GF2Matrix64(out);
        }
        GF2Matrix64 transpose() {
            long[] transposedRows = new long[64];
            for (int row = 0; row < 64; row++) {
                long bits = rows[row];
                while (bits != 0L) {
                    int column = Long.numberOfTrailingZeros(bits);
                    transposedRows[column] |= 1L << row;
                    bits &= bits - 1;
                }
            }
            return new GF2Matrix64(transposedRows);
        }
        boolean isNotSymmetric() {return doesntEqual(transpose());}
        boolean rowsAreNonzero(long mask) {
            while (mask != 0L) {
                int row = Long.numberOfTrailingZeros(mask);
                if (rows[row] != 0L)
                    return true;
                mask &= mask - 1;
            }
            return false;
        }
    }
    private static final class BlockLanczosBreakdownException extends RuntimeException{BlockLanczosBreakdownException(String message){super(message);}}
    private static GF2Matrix64 blockDot(long[] left, long[] right) {
        if (left.length != right.length)
            throw new IllegalArgumentException("Block dimension mismatch");

        // 8 byte positions × 256 possible masks.
        long[][] bucket = new long[8][256];

        for (int coordinate = 0; coordinate < left.length; coordinate++) {
            long a = left[coordinate];
            long b = right[coordinate];
            bucket[0][(int)( a         & 0xff)] ^= b;
            bucket[1][(int)((a >>>  8) & 0xff)] ^= b;
            bucket[2][(int)((a >>> 16) & 0xff)] ^= b;
            bucket[3][(int)((a >>> 24) & 0xff)] ^= b;
            bucket[4][(int)((a >>> 32) & 0xff)] ^= b;
            bucket[5][(int)((a >>> 40) & 0xff)] ^= b;
            bucket[6][(int)((a >>> 48) & 0xff)] ^= b;
            bucket[7][(int)((a >>> 56) & 0xff)] ^= b;
        }
        long[] rows = new long[64];
        for (int byteIndex = 0; byteIndex < 8; byteIndex++) {
            long[] t = bucket[byteIndex];
            int base = byteIndex << 3;
            for (int value = 1; value < 256; value++) {
                long x = t[value];
                if (x == 0L) continue;
                int mask = value;
                while (mask != 0) {
                    int bit = Integer.numberOfTrailingZeros(mask);
                    rows[base + bit] ^= x;
                    mask &= mask - 1;
                }
            }
        }
        return new GF2Matrix64(rows);
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

    private static long[] xorBlocks(long[] left, long[] right) {
        if (left.length > right.length) {
            long[] temp = left;
            left = right;
            right = temp;
        }
        long[] out = new long[right.length];
        for (int i = 0; i < right.length; i++)
            out[i] = i < left.length ? left[i] ^ right[i] : right[i];
        return out;
    }
    private static final class Block128 {
        final long[] low;
        final long[] high;
        Block128(int dimension) {
            low = new long[dimension];
            high = new long[dimension];
        }
    }
    private record Combination128(long low, long high) {
        static Combination128[] eliminate128Columns(long[] lowRows, long[] highRows) {
            if (lowRows.length != highRows.length)
                throw new IllegalArgumentException("Low/high row-count mismatch");
            int rowCount = lowRows.length;
            int packedRowWords = (rowCount + 63) >>> 6;

            // Convert the 128 candidate columns into packed column vectors.
            long[][] columns = new long[128][packedRowWords];

            for (int row = 0; row < rowCount; row++) {
                long low = lowRows[row];
                while (low != 0L) {
                    int bit = Long.numberOfTrailingZeros(low);
                    columns[bit][row >>> 6] |= 1L << (row & 63);
                    low &= low - 1;
                }
                long high = highRows[row];
                while (high != 0L) {
                    int bit = Long.numberOfTrailingZeros(high);
                    columns[64 + bit][row >>> 6] |= 1L << (row & 63);
                    high &= high - 1;
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
            return nullspace.toArray(Combination128[]::new);
        }
    }
    private static long[] combineCandidateBlocksPacked(long[] d, long[] terminalV, long dCombination, long terminalCombination) {
        if (d.length != terminalV.length)
            throw new IllegalArgumentException("Block dimension mismatch");
        int rowCount = d.length;
        long[] packed = new long[(rowCount + 63) >>> 6];
        for (int row = 0; row < rowCount; row++) {
            int parity = (Long.bitCount(d[row] & dCombination) ^ Long.bitCount(terminalV[row] & terminalCombination)) & 1;
            if (parity != 0)
                packed[row >>> 6] |= 1L << (row & 63);
        }
        return packed;
    }
    private static long[][] nullspaceDependencies(
            SparseMatrixSetup setup,
            SplittableRandom rng,
            int originalRelationCount
    ) {
        GF2SparseMatrix matrix = setup.matrix;

        for (int attempt = 0; attempt < 8; attempt++) {
            SplittableRandom attemptRng = rng.split();
            try {
                BlockLanczosResult lanczos = matrix.blockLanczos(attemptRng);
                long[] d = xorBlocks(lanczos.LEFT, lanczos.RIGHT);
                long[][] filteredDependencies =
                        matrix.extractLanczosDependencies(d, lanczos.terminalV);
                if (filteredDependencies.length != 0)
                    return expandFilteredDependencies(filteredDependencies, setup.rowDeps, originalRelationCount);
            } catch (BlockLanczosBreakdownException _) {
                // Random starting block was unsuitable.
            }
        }
        throw new IllegalStateException("Block Lanczos repeatedly failed to find dependencies");
    }
    private static long[] expandFilteredDependency(long[] packedFilteredRows, int[][] rowDeps, int originalRelationCount) {
        int expectedWords = (rowDeps.length + 63) >>> 6;
        if (packedFilteredRows.length != expectedWords)
            throw new IllegalArgumentException("Filtered dependency length mismatch");
        long[] originalRelations = new long[(originalRelationCount + 63) >>> 6];
        for (int wordIndex = 0;
             wordIndex < packedFilteredRows.length;
             wordIndex++) {
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
        return originalRelations;
    }
    private static long[][] expandFilteredDependencies(long[][] filteredDependencies, int[][] rowDeps, int originalRelationCount) {
        long[][] expanded = new long[filteredDependencies.length][];
        for (int i = 0; i < filteredDependencies.length; i++)
            expanded[i] = expandFilteredDependency(filteredDependencies[i], rowDeps, originalRelationCount);
        return expanded;
    }
    private static long[] reconstructFromSparseDependency(int[] deps, ArrayList<QSRelationFull> relations, QSFactorBase factorBase, long[] N) {
        int columnCount = factorBase.size + 1;
        long[] X = ONE_MAG;
        long[] Y = ONE_MAG;
        long[] expSums = new long[columnCount];
        ArrayList<long[]> squareFactors = new ArrayList<>();
        for (int relIndex : deps) {
            QSRelationFull rel = relations.get(relIndex);
            for (long[] x : rel.xValues) X = multiplyMod(X, x, N);
            SparseExponents e = rel.exponents;
            for (int k = 0; k < e.index.length; k++) expSums[e.index[k]] += e.count[k];
            squareFactors.addAll(Arrays.asList(rel.squareFactors));
        }
        MontgomeryReducer reducer = new MontgomeryReducer(N);
        for (int col = 0; col < expSums.length; col++) {
            long e = expSums[col];
            if ((e & 1L) == 1)
                throw new AssertionError("Dependency exponent sum is odd at column " + col);
            long half = e >>> 1;
            if (half == 0) continue;
            long[] factor = col == 0 ? subtractSmallMag(N, 1) : new long[]{factorBase.primes[col - 1]};
            reducer.multiply(Y, reducer.powMod(factor, new long[]{half}));
        }
        for (long[] sf : squareFactors) Y = multiplyMod(Y, sf, N);
        if(!equalsArray(multiplyMod(X, X, N), multiplyMod(Y, Y, N)))
            throw new AssertionError("X^2 !≡ Y^2 mod N");
        long[] diff = compareArray(X, Y) >= 0 ? subtractMagnitudes(X, Y) : subtractMagnitudes(Y, X);
        long[] g = gcdMagnitudes(diff, N);
        if (isNontrivialFactor(g, N)) return g;
        g = gcdMagnitudes(addMagnitudes(X, Y), N);
        if (isNontrivialFactor(g, N)) return g;
        return ONE_MAG;
    }
    private static long[] reconstructFactor(long[] dependency, ArrayList<QSRelationFull> relations, QSFactorBase factorBase, long[] n) {
        int expectedWords = (relations.size() + 63) >>> 6;
        if (dependency.length != expectedWords)
            throw new IllegalArgumentException("Dependency length mismatch");
        long[] x = ONE_MAG;
        long[] y = ONE_MAG;
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
                if (relationIndex >= relations.size())
                    continue;
                selectedAny = true;
                QSRelationFull relation = relations.get(relationIndex);
                for (long[] xValue : relation.xValues)
                    x = multiplyMod(x, xValue, n);
                /*
                 * A merged partial relation has the common
                 * large prime stored as a square-side factor.
                 */
                for (long[] squareFactor : relation.squareFactors)
                    y = multiplyMod(y, squareFactor, n);
                SparseExponents exponents = relation.exponents;
                for (int j = 0; j < exponents.index.length; j++) {
                    int column = exponents.index[j];
                    int exponent = exponents.count[j];
                    if (column < 0 || column >= columnCount)
                        throw new AssertionError("Relation exponent column out of range: " + column + " of " + columnCount);
                    exponentSums[column] += exponent;
                }
            }
        }
        if (!selectedAny)
            return ONE_MAG;
        MontgomeryReducer reducer = new MontgomeryReducer(n);
        for (int column = 0; column < columnCount; column++) {
            long exponent = exponentSums[column];
            if ((exponent & 1L) != 0L)
                throw new IllegalStateException("Invalid dependency: odd exponent " + exponent + " at column " + column);
            long halfExponent = exponent >>> 1;
            if (halfExponent == 0L)
                continue;
            if (column == 0){
                /*
                 * Sign-column base is -1 mod n.
                 */
                if ((halfExponent & 1L) != 0L)
                    y = modSubtract(n, y, n);
            }else{
                long[] power = reducer.powMod(new long[]{factorBase.primes[column - 1]}, new long[]{halfExponent});
                y = multiplyMod(y, power, n);
            }
        }
        if(!equalsArray(multiplyMod(x, x, n), multiplyMod(y, y, n)))
            throw new AssertionError("X^2 !≡ Y^2 mod N");
        long[] factor = gcdMagnitudes(modSubtract(x, y, n), n);
        if (isNontrivialFactor(factor, n))
            return factor;
        factor = gcdMagnitudes(modAdd(x, y, n), n);
        if (isNontrivialFactor(factor, n))
            return factor;
        return ONE_MAG;
    }
    private static double mean(ArrayList<Double> values) {
        double K = 0;
        for(double value: values) K += value;
        return K/values.size();
    }
    private static long qsProbeBudgetNanos(double remainingECMSeconds) {
        if (!Double.isFinite(remainingECMSeconds) || remainingECMSeconds <= 0.0) return 0L;
        /*
         * Spend at most 5% of the known remaining ECM time testing QS.
         * Keep the probe between 0.5 and 10 seconds.
         */
        double probeSeconds = Math.max(remainingECMSeconds * 0.05, 10.0);
        return Math.round(probeSeconds * 1e9);
    }
    public LargeInteger quadraticSieveWithoutConstraints() {
        if(isProbablyPrime()) return this;
        double lnN = logMag(limbs);
        double lnLnN = Math.log(lnN);
        long B = Math.round(Math.exp(0.55 * Math.sqrt(lnN * lnLnN)));
        B = Math.max(B, 100L);
        if (B > Integer.MAX_VALUE)
            throw new ArithmeticException("Unconstrained QS bound " + B + " exceeds supported int range");
        String S = toString();
        System.out.println("Number entered unconstrained QS: " + S + "(" + S.length() + " digits)");
        System.out.println("Factor-base bound: " + B);
        try {
            long t0 = System.nanoTime();
            QSFactorBase factorBase = buildFactorBase(limbs, (int) B);
            int[] fbIndex = buildFactorBaseIndexMap(factorBase);
            long factorBaseBuildTime = System.nanoTime() - t0;
            System.out.println("Factor base build time: " + formatSeconds(factorBaseBuildTime));
            int requiredRelations = factorBase.primes.length + 30;
            ArrayList<QSRelationFull> relations = new ArrayList<>();
            PartialGraph partials = new PartialGraph();
            System.out.println();
            long relationStart = System.nanoTime();
            long blocks = 0;
            while(relations.size() < requiredRelations) {
                blocks++;
                sieve128Polynomials(limbs, factorBase, fbIndex, relations, partials);
//                if(blocks % 16 == 0){
//                    System.out.println("Total usable relations up to now: " + relations.size());
//                    System.out.println("Total blocks up to now: " + blocks);
//                    System.out.println("Time up to now: " + formatSeconds(System.nanoTime() - relationStart));
//                    System.out.println();
//                }
            }
            long relationNanos = System.nanoTime() - relationStart;
            System.out.println("Total QS relation time: " + formatSeconds(relationNanos));
            System.out.println("Overall relation collection rate: " + relations.size() * 1e9 / relationNanos);
            System.out.println("Blocks used: " + blocks);
            System.out.println();
            int originalRelationCount = relations.size();
            long buildMatrixNanos = System.nanoTime();
            SparseMatrixSetup setup = buildQSMatrix(relations, factorBase, limbs);
            buildMatrixNanos = System.nanoTime() - buildMatrixNanos;
            System.out.println("Matrix build time: " + formatSeconds(buildMatrixNanos));
            long matrixSeed = FAST_RNG.split().nextLong();
            System.out.println("Block Lanczos RNG seed: " + Long.toUnsignedString(matrixSeed));
            System.out.println();
            long matrixSolveNanos = System.nanoTime();
            long[][] dependencies = nullspaceDependencies(setup, new SplittableRandom(matrixSeed), originalRelationCount);
            matrixSolveNanos = System.nanoTime() - matrixSolveNanos;
            System.out.println("Matrix solve time: " + formatSeconds(matrixSolveNanos));
            System.out.println();
            for (long[] dependency : dependencies) {
                long[] factor = reconstructFactor(dependency, relations, factorBase, limbs);
                if (isNontrivialFactor(factor, limbs))
                    return new LargeInteger(factor, false);
            }
        }catch (FactorFoundException e) {return new LargeInteger(e.factor, false);}
        return ONE;
    }
    //given a number N, find a relation X^2 ≡ Y^2 (mod N). then (X-Y)(X+Y) reveals factors of N.
    private static long[] quadraticSieve(FactorJob task, double remainingECMSeconds) {
        long probeBudgetNanos = qsProbeBudgetNanos(remainingECMSeconds);
        if (probeBudgetNanos == 0L || task.B > 15000000) return ONE_MAG;
        long totalStart = System.nanoTime();
        String numberText = new LargeInteger(task.n, false).toString();
        System.out.println("Number entering QS: " + numberText + "(" + numberText.length() + " digits)");
        /*
         * Factor-base construction time counts against the QS probe.
         */
        try {
            QSFactorBase factorBase = buildFactorBase(task.n, Math.toIntExact(task.B));
            int[] fbIndex = buildFactorBaseIndexMap(factorBase);
            int requiredRelations = factorBase.size + 30;
            System.out.println("Factor-base size: " + factorBase.size);
            System.out.println("Required relations: " + requiredRelations);
            ArrayList<Double> blockRates = new ArrayList<>();
            int blocks = 0;
            boolean qsAccepted;
            while (task.relations.size() < requiredRelations) {
                int relationsBefore = task.relations.size();
                long blockStart = System.nanoTime();
                sieve128Polynomials(task.n, factorBase, fbIndex, task.relations, task.partialGraph);
                long blockNanos = System.nanoTime() - blockStart;
                int relationsAdded = task.relations.size() - relationsBefore;
                double rate = blockNanos == 0L ? 0.0 : relationsAdded * 1e9 / blockNanos;
                blockRates.add(rate);
                blocks++;
                long elapsedNanos = System.nanoTime() - totalStart;
                boolean budgetExpired = elapsedNanos >= probeBudgetNanos;
                if (!budgetExpired) continue;
                /*
                 * A very short sample cannot reliably distinguish a lucky
                 * block from a genuinely good QS input.
                 */
                double medianRate = mean(blockRates);
                if (!(medianRate > 0.0) || !Double.isFinite(medianRate)) {
                    System.out.println("QS rejected: mean verified-relation rate is zero");
                    System.out.println();
                    return ONE_MAG;
                }
                int remainingRelations = requiredRelations - task.relations.size();
                double projectedQSSeconds = 1.05*remainingRelations / medianRate;
                task.qsNanos = projectedQSSeconds;
                System.out.println("QS probe: blocks=" + blocks + ", relations total=" + task.relations.size() + "/" + requiredRelations + ", mean rate=" + medianRate + "/s, projected remaining=" + projectedQSSeconds + " seconds, ECM estimate=" + remainingECMSeconds + " seconds");
                qsAccepted = projectedQSSeconds < remainingECMSeconds;
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
            while (task.relations.size() < requiredRelations) {
                sieve128Polynomials(task.n, factorBase, fbIndex, task.relations, task.partialGraph);
                blocks++;
            }
            System.out.println("Total QS relation time: " + (System.nanoTime() - totalStart) / 1e9 + " seconds");
            int originalRelationCount = task.relations.size();
            long matrixTime = System.nanoTime();
            SparseMatrixSetup setup = buildQSMatrix(task.relations, factorBase, task.n);
            long[][] dependencies = nullspaceDependencies(setup, FAST_RNG.split(), originalRelationCount);
            matrixTime = System.nanoTime() - matrixTime;
            System.out.println("Matrix build+solve time: " + matrixTime / 1e9 + " seconds");
            System.out.println();
            for (long[] dependency : dependencies) {
                long[] factor = reconstructFactor(dependency, task.relations, factorBase, task.n);
                if (isNontrivialFactor(factor, task.n))
                    return factor;
            }
        } catch (FactorFoundException e) {return e.factor;}
        return ONE_MAG;
    }
}