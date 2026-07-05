package com.AtomCAS.utilities;

import java.math.BigInteger;
import java.util.*;

public class LargeInteger extends ScalarAtom {
    //instance variables
    private final long[] limbs; //little-endian in base 2^64
    private final boolean sign;
    //special instances and helper variables
    public static final LargeInteger negativeOne = new LargeInteger(-1L);
    public static final LargeInteger zero = new LargeInteger(0L);
    public static final LargeInteger one = new LargeInteger(1L);
    public static final LargeInteger two = new LargeInteger(2L);
    public static final LargeInteger three = new LargeInteger(3L);
    public static final LargeInteger four = new LargeInteger(4L);
    public static final LargeInteger five = new LargeInteger(5L);
    public static final LargeInteger six = new LargeInteger(6L);
    public static final LargeInteger seven = new LargeInteger(7L);
    public static final LargeInteger eight = new LargeInteger(8L);
    public static final LargeInteger nine = new LargeInteger(9L);
    public static final LargeInteger ten = new LargeInteger(10L);
    public static final LargeInteger eleven = new LargeInteger(11L);
    public static final LargeInteger twelve = new LargeInteger(12L);
    public static final LargeInteger thirteen = new LargeInteger(13L);
    public static final LargeInteger fourteen = new LargeInteger(14L);
    public static final LargeInteger fifteen = new LargeInteger(15L);
    public static final LargeInteger sixteen = new LargeInteger(16L);
    public static final LargeInteger seventeen = new LargeInteger(17L);
    public static final LargeInteger eighteen = new LargeInteger(18L);
    public static final LargeInteger nineteen = new LargeInteger(19L);
    public static final LargeInteger twenty = new LargeInteger(20L);
    public static final LargeInteger thirty = new LargeInteger(30L);
    public static final LargeInteger forty = new LargeInteger(40L);
    public static final LargeInteger fifty = new LargeInteger(50L);
    public static final LargeInteger sixty = new LargeInteger(60L);
    public static final LargeInteger sixtyThree = new LargeInteger(63L);
    public static final LargeInteger sixtyFour = new LargeInteger(64L);
    public static final LargeInteger hundred = new LargeInteger(100L);
    public static final LargeInteger thousand = new LargeInteger(1000L);
    private static final SplittableRandom MASTER_RNG = new SplittableRandom();

    public LargeInteger(long[] limbs, boolean sign) {
        if (limbs == null || limbs.length == 0)
            throw new IllegalArgumentException("limbs must be nonempty");
        this.limbs = trimLeadingZeros(limbs);
        this.sign = !isZeroArray(this.limbs) && sign;
    }
    public LargeInteger(long limb){
        limbs = new long[1];
        if(limb < 0){
            limbs[0] = -limb;
            sign = true;
        }else{
            limbs[0] = limb;
            sign = false;
        }
    }
    public LargeInteger(String s){
        if (!s.matches("-?\\d+")) {
            ScalarAtom R = Parser.parse(s);
            if (R instanceof LargeInteger I) {
                this.limbs = I.limbs;
                this.sign = I.sign;
                return;
            }
            R = Parser.parse(R.toString());
            if (R instanceof LargeInteger I) {
                this.limbs = I.limbs;
                this.sign = I.sign;
                return;
            }
            throw new IllegalArgumentException("Error: Inputted expression does not evaluate to an integer");
        }
        s = s.trim();
        if (s.isEmpty())
            throw new NumberFormatException("Error: Empty string");
        boolean neg = false;
        int pos = 0;
        if (s.charAt(0) == '+' || s.charAt(0) == '-') {
            neg = s.charAt(0) == '-';
            pos = 1;
        }
        if (pos == s.length())
            throw new NumberFormatException("Error: Only sign is present");
        while (pos < s.length() && s.charAt(pos) == '0')
            pos++;
        if (pos == s.length()) {
            this.limbs = new long[]{0L};
            this.sign = false;
            return;
        }
        long[] acc = {0L};
        int firstLen = (s.length() - pos) % 18;
        if (firstLen == 0) firstLen = 18;
        int end = pos + firstLen;
        acc = multiply1e18AddSmall(acc, parseDecimalChunk(s, pos, end));
        pos = end;
        while (pos < s.length()) {
            end = pos + 18;
            acc = multiply1e18AddSmall(acc, parseDecimalChunk(s, pos, end));
            pos = end;
        }
        this.limbs = trimLeadingZeros(acc);
        this.sign = neg && !isZeroArray(this.limbs);
    }
    private static boolean isZeroArray(long[] a) {return a.length == 1 && a[a.length-1] == 0;}
    private static long[] multiply1e18AddSmall(long[] mag, long add) {
        if (add < 0)
            throw new IllegalArgumentException("small values must be nonnegative signed longs");
        mag = trimLeadingZeros(mag);
        long[] out = new long[mag.length + 2];
        long carry = add;
        for (int i = 0; i < mag.length; i++) {
            LongPair p = LongPair.multiply64(mag[i], 1_000_000_000_000_000_000L).addLow(carry);
            out[i] = p.low;
            carry = p.high;
        }
        out[mag.length] = carry;
        return trimLeadingZeros(out);
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
        while (n > 1 && a[n - 1] == 0L)
            n--;
        if (n == a.length)
            return a;
        long[] out = new long[n];
        System.arraycopy(a, 0, out, 0, n);
        return out;
    }
    private static int bitLength(long[] x) {
        x = trimLeadingZeros(x);
        if (isZeroArray(x))
            return 0;
        int top = x.length - 1;
        return top * 64 + (64 - Long.numberOfLeadingZeros(x[top]));
    }
    public int bitLength() {return bitLength(limbs);}
    public static LargeInteger basePower(int chunks) {
        if (chunks < 0)
            throw new IllegalArgumentException("Negative chunk power");
        long[] arr = new long[chunks + 1];
        arr[arr.length-1] = 1L;
        return new LargeInteger(arr, false);
    }
    public static LargeInteger pow10(LargeInteger power){return ten.power(power).numerator;}
    @Override
    public LargeInteger copy(){return new LargeInteger(limbs, sign);}
    private static DivRem divRem1e9(long[] mag) {
        long[] q = new long[mag.length];
        long rem = 0L;
        for (int i = mag.length - 1; i >= 0; i--) {
            long limb = mag[i];
            long hi = limb >>> 32;
            long cur = (rem << 32) + hi;
            long qHi = cur / 1_000_000_000L;
            rem = cur % 1_000_000_000L;
            long lo = limb & 0xFFFF_FFFFL;
            cur = (rem << 32) + lo;
            long qLo = cur / 1_000_000_000L;
            rem = cur % 1_000_000_000L;

            q[i] = (qHi << 32) | qLo;
        }
        return new DivRem(trimLeadingZeros(q), rem);
    }
    private record DivRemLongPair(LongPair quotient, long remainder) {}
    private record DivRem(long[] quotient, long remainder) {}
    //returns a String version that can be printed.
    @Override
    public String toString() {
        long[] x = this.limbs;
        if (isZeroArray(x)) return "0";

        ArrayList<Long> chunks = new ArrayList<>();
        while (!isZeroArray(x)) {
            DivRem d = divRem1e9(x);
            chunks.add(d.remainder());
            x = d.quotient();
        }
        StringBuilder sb = new StringBuilder();
        if (sign) sb.append("-");

        int last = chunks.size() - 1;
        sb.append(chunks.get(last)); // most significant chunk: no padding

        for (int i = last - 1; i >= 0; i--) {
            String chunk = Long.toString(chunks.get(i));
            sb.repeat("0", 9 - chunk.length());
            sb.append(chunk);
        }
        return sb.toString();
    }
    public BigInteger toBigInteger() {
        if (isZeroArray(limbs))
            return BigInteger.ZERO;

        byte[] bytes = toUnsignedBigEndianBytes(limbs);

        BigInteger result = new BigInteger(1, bytes);
        return sign ? result.negate() : result;
    }

    private static byte[] toUnsignedBigEndianBytes(long[] limbs) {
        limbs = trimLeadingZeros(limbs);

        byte[] bytes = new byte[limbs.length * 8];

        for (int i = 0; i < limbs.length; i++) {
            long limb = limbs[i];

            int byteIndex = bytes.length - 8 * (i + 1);

            bytes[byteIndex]     = (byte) (limb >>> 56);
            bytes[byteIndex + 1] = (byte) (limb >>> 48);
            bytes[byteIndex + 2] = (byte) (limb >>> 40);
            bytes[byteIndex + 3] = (byte) (limb >>> 32);
            bytes[byteIndex + 4] = (byte) (limb >>> 24);
            bytes[byteIndex + 5] = (byte) (limb >>> 16);
            bytes[byteIndex + 6] = (byte) (limb >>> 8);
            bytes[byteIndex + 7] = (byte) limb;
        }

        return bytes;
    }
    @Override
    public boolean equalsTo(Atom other){
        if(!(other instanceof ScalarAtom)) return false;
        else if(other instanceof LargeInteger I)
            return compareArray(limbs, I.limbs) == 0 && sign == I.sign;
        return this.compareTo((ScalarAtom) other) == 0L;
    }
    public LargeInteger negate(){return new LargeInteger(this.limbs, !sign);}
    @Override
    public ScalarAtom realPart(){return this;}
    @Override
    public ScalarAtom imagPart(){return zero;}
    @Override
    public LargeInteger magnitude(){return new LargeInteger(this.limbs, false);}
    @Override
    public LargeInteger floor(){return this;}
    @Override
    public LargeInteger ceil(){return this;}
    private long[] toTwosComplement(int len) {
        long[] out = new long[len];
        System.arraycopy(limbs, 0, out, 0, Math.min(limbs.length, len));
        if (!sign)
            return out;
        for (int i = 0; i < len; i++)
            out[i] = ~out[i];
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
        // Negative: magnitude = -(two's complement value)
        // invert all limbs, then add 1.
        for (int i = 0; i < tc.length; i++)
            tc[i] = ~tc[i];
        long carry = 1L;
        for (int i = 0; i < tc.length && carry != 0L; i++) {
            long old = tc[i];
            tc[i] = old + carry;
            carry = Long.compareUnsigned(tc[i], old) < 0 ? 1L : 0L;
        }
        return new LargeInteger(trimLeadingZeros(tc), true);
    }
    public LargeInteger NOT(){return this.negate().subtract(1);}
    public LargeInteger AND(LargeInteger other) {
        int len = Math.max(this.limbs.length, other.limbs.length) + 1;
        long[] a = this.toTwosComplement(len);
        long[] b = other.toTwosComplement(len);
        long[] out = new long[len];
        for (int i = 0; i < len; i++)
            out[i] = a[i] & b[i];
        return fromTwosComplement(out);
    }
    public LargeInteger OR(LargeInteger other) {
        int len = Math.max(this.limbs.length, other.limbs.length) + 1;
        long[] a = this.toTwosComplement(len);
        long[] b = other.toTwosComplement(len);
        long[] out = new long[len];
        for (int i = 0; i < len; i++)
            out[i] = a[i] | b[i];
        return fromTwosComplement(out);
    }
    public LargeInteger XOR(LargeInteger other) {
        int len = Math.max(this.limbs.length, other.limbs.length) + 1;
        long[] a = this.toTwosComplement(len);
        long[] b = other.toTwosComplement(len);
        long[] out = new long[len];
        for (int i = 0; i < len; i++)
            out[i] = a[i] ^ b[i];
        return fromTwosComplement(out);
    }
    //generates a random integer within a specified range [start, end)
    public static LargeInteger random(LargeInteger start, LargeInteger end){return random(start, end, MASTER_RNG.split());}
    private static LargeInteger random(LargeInteger start, LargeInteger end, SplittableRandom rng) {
        LargeInteger range = (LargeInteger) end.subtract(start);
        if (range.sign || isZeroArray(range.limbs))
            throw new IllegalArgumentException("Error: Invalid range: [" + start + ", " + end + ")");
        long[] bound = trimLeadingZeros(range.limbs);
        long[] candidate = new long[bound.length];
        int top = bound.length - 1;
        do {
            for (int i = 0; i < top; i++)
                candidate[i] = rng.nextLong();
            candidate[top] = randomUnsignedAtMost(bound[top], rng);
        } while (compareArray(candidate, bound) >= 0);
        return (LargeInteger) new LargeInteger(candidate, false).add(start);
    }
    private static long randomUnsignedAtMost(long bound, SplittableRandom rng) {
        // Uniform unsigned random x with 0 <=_u x <=_u bound
        if (bound == -1L) {
            // unsigned 2^64 - 1, so any 64-bit pattern is allowed
            return rng.nextLong();
        }
        long exclusive = bound + 1L;
        // If exclusive is positive, Java's bounded nextLong works directly.
        if (exclusive > 0L)
            return rng.nextLong(exclusive);
        // Otherwise exclusive wrapped around signed-wise, so do unsigned rejection.
        long x;
        do
            x = rng.nextLong();
        while (Long.compareUnsigned(x, exclusive) >= 0);
        return x;
    }
    public static LargeInteger randomChunks(int chunks){
        if (chunks < 0)
            throw new IllegalArgumentException("Error: Chunks must be nonnegative");
        if (chunks == 0)
            return zero;
        long[] res = new long[chunks];
        for(int i = 0; i < chunks; i++)
            res[i] = MASTER_RNG.nextLong();
        while(res[res.length-1] == 0)
            res[res.length-1] = MASTER_RNG.nextLong();
        return new LargeInteger(res, false);
    }
    private static int compareArray(long[] a, long[] b){
        if(a.length > b.length) return 1;
        else if(b.length > a.length) return -1;
        for(int i = a.length - 1; i >= 0; i--){
            if(Long.compareUnsigned(a[i], b[i]) > 0) return 1;
            else if(Long.compareUnsigned(a[i], b[i]) < 0) return -1;
        }
        return 0;
    }
    @Override
    public long compareMags(ScalarAtom other) {
        if(other instanceof LargeInteger I)
            return compareArray(this.limbs, I.limbs);
        else if(other instanceof LargeFraction f)
            return this.multiply(f.denominator).compareMags(f.numerator);
        else if(other instanceof LargePowerMonomial L){
            LargeFraction f = this.power(L.exponent.denominator);
            LargeFraction o = L.power(new LargeFraction(L.exponent.denominator, one)).base;
            return f.compareMags(o);
        }else if(other instanceof RadicalTerm R){
            if(other.isNotReal()){
                RadicalTermSum RS = (RadicalTermSum) other.multiply(R.complexConjugate());
                return this.square().compareMags(RS);
            }
            if(this.equalsTo(R.rationalPart) && R.coefficient.equalsTo(LargeInteger.zero)) return 0L;
            LargeFraction f = (LargeFraction) this.subtract(R.rationalPart).divide(R.coefficient);
            f = f.power(R.exponent.exponent.denominator);
            LargeFraction o = R.exponent.base;
            return f.compareMags(o);
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    //compares two limbs. for integers and exponents, it uses special techniques.
    //integer comparison uses compareMags, which does most of the hard work.
    @Override
    public long compareTo(ScalarAtom other) {
        if(other instanceof LargeInteger){
            if(this.compareMags(other) > 0L){
                if(this.sign) return -1L;
                return 1L;
            }else if(this.compareMags(other) < 0L){
                if(((LargeInteger) other).sign) return 1L;
                return -1L;
            }
            if(this.sign && !((LargeInteger) other).sign) return -1L;
            else if(!this.sign && ((LargeInteger) other).sign) return 1L;
            return 0L;
        }else if(other instanceof LargeFraction)
            return this.multiply(((LargeFraction) other).denominator).compareTo(((LargeFraction) other).numerator);
        else if(other instanceof LargePowerMonomial){
            if(other.isNotReal()) throw new IllegalStateException("Error: Complex numbers cannot be compared");
            LargeFraction f = this.power(((LargePowerMonomial) other).exponent.denominator);
            LargeFraction o = ((LargePowerMonomial) other).power((LargeFraction) ((LargePowerMonomial) other).exponent.denominator.divide(one)).base;
            long res = f.compareMags(o);
            if(res == 1){
                if(this.sign) return -1L;
                return 1L;
            }else if(res == -1L){
                if(other.lessThan(zero)) return 1L;
                return -1L;
            }
            return res;
        }else if(other instanceof RadicalTerm){
            if(other.isNotReal()) throw new IllegalStateException("Error: Complex numbers cannot be compared");
            long B = this.compareMags(other);
            if(B == 1L){
                if(this.sign) return -1L;
                return 1L;
            }else if(B == -1L){
                if(other.lessThan(zero)) return 1L;
                return -1L;
            }
            if(this.sign && other.greaterThan(zero)) return -1L;
            else if(!this.sign && other.lessThan(zero)) return 1L;
            return 0L;
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    public boolean isRational() {return true;}
    @Override
    public boolean isReal() {return true;}
    @Override
    public boolean isNotReal() {return false;}
    @Override
    public boolean isAlgebraic() {return true;}
    public LargeInteger triangular(){return ((LargeInteger) this.add(one)).nCr(two);}
    public LargeInteger tetrahedral(){return ((LargeInteger)this.add(two)).nCr(three);}
    public static LargeInteger figurate(LargeInteger l, LargeInteger k){return ((LargeInteger)k.add(l).subtract(one)).nCr(k);}
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
        static LongPair add64(long a, long b, long carryIn) {
            long s = a + b;
            long c1 = Long.compareUnsigned(s, a) < 0 ? 1L : 0L;

            long t = s + carryIn;
            long c2 = Long.compareUnsigned(t, s) < 0 ? 1L : 0L;

            return new LongPair(c1 + c2, t);
        }
        private int bitLength() {
            if (high != 0L)
                return 64 + (64 - Long.numberOfLeadingZeros(high));
            if (low != 0L)
                return 64 - Long.numberOfLeadingZeros(low);
            return 0;
        }
        private boolean testBit(int bit) {
            if (bit < 64)
                return ((low >>> bit) & 1L) != 0L;
            return ((high >>> (bit - 64)) & 1L) != 0L;
        }
        private LongPair setBit(int bit) {
            if (bit < 0 || bit >= 128)
                throw new IllegalArgumentException();

            if (bit < 64)
                return new LongPair(high, low | (1L << bit));

            return new LongPair(high | (1L << (bit - 64)), low);
        }

        static int compareUnsigned128(LongPair a, LongPair b) {
            int c = Long.compareUnsigned(a.high, b.high);
            if (c != 0) return c;
            return Long.compareUnsigned(a.low, b.low);
        }

        LongPair shiftLeftOne() {
            long newHigh = (high << 1) | (low >>> 63);
            long newLow = low << 1;
            return new LongPair(newHigh, newLow);
        }

        LongPair addLowBit(long bit) {
            // bit must be 0 or 1
            if (bit == 0L) return this;

            long newLow = low + 1L;
            long carry = Long.compareUnsigned(newLow, low) < 0 ? 1L : 0L;
            return new LongPair(high + carry, newLow);
        }
        LongPair subtract(LongPair other) {
            long newLow = low - other.low;
            long borrow = Long.compareUnsigned(low, other.low) < 0 ? 1L : 0L;
            long newHigh = high - other.high - borrow;
            return new LongPair(newHigh, newLow);
        }
        DivRemLongPair divideUnsignedByLong(long divisor) {
            if (divisor == 0L)
                throw new ArithmeticException("Error: divide by zero");
            LongPair q = new LongPair(0L, 0L);
            LongPair r = new LongPair(0L, 0L);
            LongPair d = new LongPair(0L, divisor);

            int bits = this.bitLength();

            for (int i = bits - 1; i >= 0; i--) {
                r = r.shiftLeftOne().addLowBit(this.testBit(i) ? 1L : 0L);

                if (compareUnsigned128(r, d) >= 0) {
                    r = r.subtract(d);
                    q = q.setBit(i);
                }
            }
            // Since divisor is 64-bit, remainder must fit in one limb.
            return new DivRemLongPair(q, r.low);
        }
        boolean greaterThan(LongPair other){
            int cmp = Long.compareUnsigned(high, other.high);
            if(cmp > 0) return true;
            else if(cmp < 0) return false;

            cmp = Long.compareUnsigned(low, other.low);
            if(cmp > 0) return true;
            else if(cmp < 0) return false;

            return false;
        }
        static long[] divRem128By64To64(long high, long low, long divisor) {
            DivRemLongPair dr = new LongPair(high, low).divideUnsignedByLong(divisor);
            if (dr.quotient.high != 0L)
                throw new ArithmeticException("quotient overflow");
            return new long[]{dr.quotient.low, dr.remainder};
        }
    }
    private record SignedMag(long[] mag, boolean negative) {
        SignedMag {
            mag = trimLeadingZeros(mag);
            negative = !isZeroArray(mag) && negative;
        }
        static SignedMag positive(long[] mag) {return new SignedMag(mag, false);}
        static SignedMag negative(long[] mag) {return new SignedMag(mag, true);}
        SignedMag negate() {return new SignedMag(mag, !negative);}

        SignedMag add(SignedMag other) {
            if (isZeroArray(this.mag)) return other;
            if (isZeroArray(other.mag)) return this;
            // Same sign: add magnitudes, keep sign.
            if (this.negative == other.negative)
                return new SignedMag(addMagnitudes(this.mag, other.mag), this.negative);
            // Different signs: subtract smaller magnitude from larger magnitude.
            int cmp = compareArray(this.mag, other.mag);
            if (cmp == 0)
                return new SignedMag(new long[]{0L}, false);
            if (cmp > 0)
                return new SignedMag(subtractMagnitudes(this.mag, other.mag), this.negative);
            return new SignedMag(subtractMagnitudes(other.mag, this.mag), other.negative);
        }
        SignedMag subtract(SignedMag other) {return this.add(other.negate());}
        SignedMag multiply(SignedMag other) {return new SignedMag(multiplyMagnitudes(this.mag, other.mag), this.negative ^ other.negative);}
        SignedMag shiftLimbs(int limbs) {return new SignedMag(shiftLeftLimbs(this.mag, limbs), negative);}
        long[] toPositiveMagnitudeChecked() {
            if (negative)
                throw new ArithmeticException("Error: Some interpolation produced negative final values");
            return mag;
        }

        SignedMag divideExactSmall(int d) {
            if (d <= 0)
                throw new IllegalArgumentException("divisor must be positive");
            if (isZeroArray(this.mag))
                return this;
            return new SignedMag(divideMagExactSmall(this.mag, d), this.negative);
        }

        int compareTo(SignedMag other) {
            int cmp = compareArray(this.mag, other.mag);
            if (cmp == 1) {
                if (this.negative) return -1;
                return 1;
            } else if (cmp == -1) {
                if (other.negative) return 1;
                return -1;
            }
            if (this.negative && !other.negative) return -1;
            else if (!this.negative && other.negative) return 1;
            return 0;
        }
        boolean greaterThan(SignedMag other) {return compareTo(other) > 0;}
        boolean lessThan(SignedMag other) {return compareTo(other) < 0;}
        boolean greaterThanOrEqualTo(SignedMag other) {return compareTo(other) >= 0;}
        boolean lesserThanOrEqualTo(SignedMag other) {return compareTo(other) <= 0;}
    }
    private static long[] addMagnitudes(long[] a, long[] b){
        long carry = 0;
        int len = Math.max(a.length, b.length);
        long[] res = new long[len+1];
        for(int i = 0; i < len; i++){
            long p1 = i < a.length ? a[i] : 0L;
            long p2 = i < b.length ? b[i] : 0L;
            LongPair part = LongPair.add64(p1, p2, carry);
            res[i] = part.low;
            carry = part.high;
        }
        res[len] = carry;
        return trimLeadingZeros(res);
    }
    private static long[] subtractMagnitudes(long[] a, long[] b) {
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);
        if (compareArray(a, b) < 0)
            throw new ArithmeticException("subtractMagnitudes requires a >= b");
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
            throw new ArithmeticException("subtractMagnitudes underflow");
        return trimLeadingZeros(out);
    }
    private static boolean subtractMulLimbAt(long[] u, long[] v, long qhat, int offset) {
        long carry = 0L;
        long borrow = 0L;

        for (int i = 0; i < v.length; i++) {
            LongPair p = LongPair.multiply64(v[i], qhat).addLow(carry);

            long subLow = p.low;
            carry = p.high;

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
        //This one adds two integers via base-2^64 chunking. Like every other method, it assumes both are normalized to the best they can be. Runs in time complexity O(n), n being max limbs of chunks
        if (other instanceof LargeInteger o) {
            if (this.sign == o.sign)
                return new LargeInteger(addMagnitudes(this.limbs, o.limbs), this.sign);
            long cmp = this.compareMags(o);
            if (cmp == 0) return zero;
            if (cmp > 0) {
                long[] result = subtractMagnitudes(this.limbs, o.limbs);
                return new LargeInteger(result, sign);
            }
            long[] result = subtractMagnitudes(o.limbs, this.limbs);
            return new LargeInteger(result, o.sign);
        } else if(other instanceof LargeFraction) return this.divide(one).add(other);
        else if(other instanceof LargePowerMonomial) return new RadicalTerm(new LargeFraction(this, one), LargeFraction.one, (LargePowerMonomial) other);
        else if(other instanceof RadicalTerm) return new RadicalTerm((LargeFraction) this.add(((RadicalTerm) other).rationalPart), ((RadicalTerm) other).coefficient, ((RadicalTerm) other).exponent);
        else if(other instanceof RadicalTermSum)
            return new RadicalTermSum(((RadicalTermSum) other).sum, (LargeFraction) ((RadicalTermSum) other).constant.add(this));
        else if(other instanceof LargePowerNode)
            return new RadicalTermNode(new LargeFraction(this, one), LargeFraction.one, (LargePowerNode) other);
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    public ScalarAtom add(ScalarAtom... atoms){
        ScalarAtom result = this;
        for(ScalarAtom atom: atoms) result = result.add(atom);
        return result;
    }
    public LargeInteger add(long other) {
        if (other == 0L)
            return this;

        boolean otherSign = other < 0;
        long otherMag = otherSign ? -other : other;

        // Caveat: Long.MIN_VALUE needs special handling because -Long.MIN_VALUE overflows.
        if (other == Long.MIN_VALUE)
            return (LargeInteger) this.add(new LargeInteger(Long.toString(other)));
        long[] o = new long[]{otherMag};
        if (this.sign == otherSign)
            return new LargeInteger(addMagnitudes(this.limbs, o), this.sign);

        int cmp = compareArray(this.limbs, o);

        if (cmp == 0)
            return zero;

        if (cmp > 0)
            return new LargeInteger(subtractMagnitudes(this.limbs, o), this.sign);

        return new LargeInteger(subtractMagnitudes(o, this.limbs), otherSign);
    }
    public ScalarAtom subtract(ScalarAtom... atoms){
        ScalarAtom result = this;
        for(ScalarAtom atom: atoms) result = result.subtract(atom);
        return result;
    }
    public LargeInteger subtract(long other){return this.add(-other);}
    private static long[] shiftLeftLimbs(long[] x, int limbs) {
        x = trimLeadingZeros(x);
        if (isZeroArray(x))
            return new long[]{0L};
        if (limbs <= 0)
            return x;
        long[] out = new long[x.length + limbs];
        // pad zeros at the beginning
        System.arraycopy(x, 0, out, limbs, x.length);
        return out;
    }
    private static long[] shiftLeftBits(long[] x, int bits) {
        x = trimLeadingZeros(x);

        if (isZeroArray(x))
            return new long[]{0L};

        if (bits < 0 || bits >= 64)
            throw new IllegalArgumentException("bits must be in [0, 63]");

        if (bits == 0)
            return x;

        long[] out = new long[x.length + 1];

        long carry = 0L;

        for (int i = 0; i < x.length; i++) {
            long limb = x[i];

            out[i] = (limb << bits) | carry;
            carry = limb >>> (64 - bits);
        }

        out[x.length] = carry;

        return trimLeadingZeros(out);
    }
    private static long[] shiftRightLimbs(long[] x, int limbs) {
        x = trimLeadingZeros(x);
        if (isZeroArray(x) || limbs >= x.length)
            return new long[]{0L};
        if (limbs <= 0)
            return x;
        long[] out = new long[x.length - limbs];
        System.arraycopy(x, limbs, out, 0, x.length - limbs);
        return trimLeadingZeros(out);
    }
    private static long[] shiftRightBits(long[] x, int bits) {
        x = trimLeadingZeros(x);
        if (isZeroArray(x))
            return new long[]{0L};

        if (bits < 0 || bits >= 64)
            throw new IllegalArgumentException("bits must be in [0, 63]");

        if (bits == 0)
            return x;

        long[] out = new long[x.length];

        long carry = 0L;

        for (int i = x.length - 1; i >= 0; i--) {
            long limb = x[i];

            out[i] = (limb >>> bits) | carry;
            carry = limb << (64 - bits);
        }

        return trimLeadingZeros(out);
    }
    private static boolean testBit(long[] x, int bit) {
        int limb = bit >>> 6;
        int offset = bit & 63;

        if (limb >= x.length)
            return false;

        return ((x[limb] >>> offset) & 1L) != 0L;
    }
    public LargeInteger shiftRightBits(int bits){return new LargeInteger(shiftRightBits(limbs, bits), sign);}
    private static long[] slice(long[] a, int from, int to) {
        int len = Math.max(0, to - from);
        if(len == 0) return new long[]{0};
        else if(to > a.length){
            if(from > a.length)
                return new long[]{0};
            long[] out = new long[a.length - from];
            if (a.length - from >= 0)
                System.arraycopy(a, from, out, 0, a.length - from);
            return trimLeadingZeros(out);
        }
        long[] out = new long[len];
        if (to - from >= 0) System.arraycopy(a, from, out, 0, to - from);
        return trimLeadingZeros(out);
    }
    private static long[] multiplySmallMag(long[] a, long m) {
        if (isZeroArray(a) || m == 0L)
            return new long[]{0L};
        long[] out = new long[a.length + 1];
        long carry = 0L;
        for (int i = 0; i < a.length; i++) {
            LongPair p = LongPair.multiply64(a[i], m).addLow(carry);
            out[i] = p.low;
            carry = p.high;
        }
        out[a.length] = carry;
        return trimLeadingZeros(out);
    }
    private static SignedMag multiplyMagnitudeBySignedLong(long[] a, long m) {
        if(m < 0) return new SignedMag(multiplySmallMag(a, -m), true);
        return new SignedMag(multiplySmallMag(a, m), false);
    }
    private static long[] multiplyMagnitudes(long[] a, long[] b) {
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);
        if (isZeroArray(a) || isZeroArray(b))
            return new long[]{0L};
        long[] t = a.length > b.length ? a : b;
        long[] u = Arrays.equals(t, a) ? b : a;
        int max = t.length;
        int min = u.length;
        if(min == 1)
            return multiplySmallMag(t, u[0]);
        if (min < 49) {
            long[] out = new long[t.length + u.length];
            for (int i = 0; i < t.length; i++) {
                long carry = 0L;
                for (int j = 0; j < u.length; j++) {
                    int k = i + j;
                    LongPair p = LongPair.multiply64(t[i], u[j]).addLow(out[k]).addLow(carry);
                    out[k] = p.low;
                    carry = p.high;
                }
                // propagate carry after finishing row i
                int k = i + u.length;
                while (carry != 0L) {
                    LongPair s = LongPair.add64(out[k], carry);
                    out[k] = s.low;
                    carry = s.high;
                    k++;
                }
            }
            return trimLeadingZeros(out);
        } else if (min < 16385) {
            /*
             * Karatsuba multiplication is a specialized algorithm that uses recursion to "split"
             * large limbs into two smaller parts and operate on the parts. Unlike standard grade-school
             * multiply(which uses 4 multiplications), Karatsuba only needs to do 3 multiplications(and several additions)
             * Karatsuba runs in time complexity O(n^(log_2(3)) ≈ O(n^1.585)
             */
            int pivot = max/2;

            long[] lower1 = slice(t, 0, pivot);
            long[] upper1 = slice(t, pivot, t.length);
            long[] lower2 = slice(u, 0, pivot);
            long[] upper2 = slice(u, pivot, u.length);

            long[] z0 = multiplyMagnitudes(lower1, lower2);
            long[] z2 = multiplyMagnitudes(upper1, upper2);
            long[] z1 = subtractMagnitudes(multiplyMagnitudes(addMagnitudes(lower1, upper1), addMagnitudes(lower2, upper2)), z0);
            z1 = subtractMagnitudes(z1, z2);
            return trimLeadingZeros(addMagnitudes(addMagnitudes(z0, shiftLeftLimbs(z1, pivot)), shiftLeftLimbs(z2, 2 * pivot)));
        }
        //Toom-Cook 3 algorithm. a SignedMag helper will be needed because negative intermediate values can occur
        int m = (max + 2) / 3;
        long[] a0 = slice(t, 0, m);
        long[] a1 = slice(t, m, 2 * m);
        long[] a2 = slice(t, 2 * m, t.length);

        long[] b0 = slice(u, 0, m);
        long[] b1 = slice(u, m, 2 * m);
        long[] b2 = slice(u, 2 * m, u.length);

        long[] p1   = addMagnitudes(addMagnitudes(a0, a1), a2);
        SignedMag pm1 = SignedMag.positive(a0).subtract(SignedMag.positive(a1)).add(SignedMag.positive(a2));
        long[] p2   = addMagnitudes(addMagnitudes(a0, multiplySmallMag(a1, 2)), multiplySmallMag(a2, 4));

        long[] q1   = addMagnitudes(addMagnitudes(b0, b1), b2);
        SignedMag qm1 = SignedMag.positive(b0).subtract(SignedMag.positive(b1)).add(SignedMag.positive(b2));
        long[] q2   = addMagnitudes(addMagnitudes(b0, multiplySmallMag(b1, 2)), multiplySmallMag(b2, 4));

        long[] v0 = multiplyMagnitudes(a0, b0);
        long[] v1 = multiplyMagnitudes(p1, q1);
        SignedMag vm1 = pm1.multiply(qm1);
        long[] v2 = multiplyMagnitudes(p2, q2);
        long[] vInf = multiplyMagnitudes(a2, b2);

        SignedMag r2 = SignedMag.positive(v1).add(vm1).divideExactSmall(2).subtract(SignedMag.positive(addMagnitudes(v0, vInf)));

        SignedMag s = SignedMag.positive(v1).subtract(vm1).divideExactSmall(2);

        SignedMag T = SignedMag.positive(v2).subtract(SignedMag.positive(v0)).subtract(SignedMag.positive(multiplySmallMag(vInf, 16))).divideExactSmall(2);

        SignedMag r3 = T.subtract(s).subtract(SignedMag.positive(multiplySmallMag(r2.mag, 2))).divideExactSmall(3);

        SignedMag r1 = s.subtract(r3);
        return SignedMag.positive(v0).add(r1.shiftLimbs(m)).add(r2.shiftLimbs(2 * m)).add(r3.shiftLimbs(3 * m)).add(SignedMag.positive(shiftLeftLimbs(vInf, 4 * m))).toPositiveMagnitudeChecked();
    }
    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        if(other instanceof LargeInteger I)
            return new LargeInteger(multiplyMagnitudes(limbs, I.limbs), this.sign ^ I.sign);
        else if(other instanceof LargeFraction)
            return new LargeFraction((LargeInteger) this.multiply(((LargeFraction) other).numerator), ((LargeFraction) other).denominator);
        else if(other instanceof LargePowerMonomial)
            return new LargePowerMonomial(new LargeFraction(this, one), LargeFraction.one).multiply(other);
        else if(other instanceof RadicalTermSum R){
            LargeList<RadicalTerm> rt = new LargeList<>();
            for(LargePowerMonomial L: R.sum)
                rt.add(new RadicalTerm(LargeFraction.zero, (LargeFraction) this.multiply(R.sum.get(L)), L));
            return new RadicalTermSum(rt, (LargeFraction) this.multiply(R.constant));
        }else if(other instanceof LargePowerNode L)
            return new LargePowerNode(L.base.multiply(this.power(L.exponent.reciprocal())), L.exponent, L.signFactor);
        return other.multiply(this);
    }
    public ScalarAtom multiply(ScalarAtom... atoms){
        ScalarAtom result = this;
        for(ScalarAtom atom: atoms) result = result.multiply(atom);
        return result;
    }
    public LargeInteger multiply(long greg){return new LargeInteger(multiplySmallMag(limbs, Math.abs(greg)), this.sign ^ (greg < 0));}
    private static void addAt(long[] out, long[] x, int offset) {
        long carry = 0L;

        for (int i = 0; i < x.length; i++) {
            LongPair s = LongPair.add64(out[offset + i], x[i], carry);
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
    private static void addLongPairAt(long[] out, int offset, LongPair p) {
        long old = out[offset];
        long sum = old + p.low;
        out[offset] = sum;
        long carry = Long.compareUnsigned(sum, old) < 0 ? 1L : 0L;

        old = out[offset + 1];
        sum = old + p.high;
        long carry2 = Long.compareUnsigned(sum, old) < 0 ? 1L : 0L;

        sum += carry;
        long carry3 = Long.compareUnsigned(sum, carry) < 0 ? 1L : 0L;

        out[offset + 1] = sum;

        carry = carry2 | carry3;

        int i = offset + 2;
        while (carry != 0L) {
            old = out[i];
            out[i] = old + 1L;
            carry = Long.compareUnsigned(out[i], old) < 0 ? 1L : 0L;
            i++;
        }
    }
    private static long[] squareMag(long[] a){
        if(isZeroArray(a))
            return a;
        int n = a.length;
        if(n == 1){
            LongPair PL = LongPair.multiply64(a[0], a[0]);
            return trimLeadingZeros(new long[]{PL.low, PL.high});
        }
        if(n < 49){
            long[] out = new long[2 * n + 2];
            // Diagonal terms: ai^2 at offset 2*i
            for (int i = 0; i < n; i++)
                addLongPairAt(out, i << 1, LongPair.multiply64(a[i], a[i]));

            // Off-diagonal terms: 2*ai*aj, but row-carry style
            for (int i = 0; i < n; i++) {
                long ai = a[i];

                long carry0 = 0L; // carry into current limb
                long carry1 = 0L; // carry one limb beyond that, normally 0 or 1

                for (int j = i + 1; j < n; j++) {
                    int k = i + j;

                    LongPair p = LongPair.multiply64(ai, a[j]);

                    // double the 128-bit product:
                    // d = 2*p = d2:d1:d0 where d2 is 0 or 1
                    long d0 = p.low << 1;
                    long d1 = (p.high << 1) | (p.low >>> 63);
                    long d2 = p.high >>> 63;

                    // out[k] += d0 + carry0
                    LongPair s0 = LongPair.add64(out[k], d0, carry0);
                    out[k] = s0.low;

                    // carry into next limb = d1 + carry1 + s0.high
                    LongPair s1 = LongPair.add64(d1, carry1, s0.high);

                    carry0 = s1.low;
                    carry1 = d2 + s1.high; // d2 is 0/1, s1.high is 0/1/2-ish safe here
                }
                // Flush final carries after the row.
                int k = i + n;
                if (carry0 != 0L) {
                    LongPair s = LongPair.add64(out[k], carry0);
                    out[k] = s.low;
                    carry0 = s.high;
                    k++;
                }
                long carry = carry0 + carry1;
                while (carry != 0L) {
                    LongPair s = LongPair.add64(out[k], carry);
                    out[k] = s.low;
                    carry = s.high;
                    k++;
                }
            }
            return trimLeadingZeros(out);
        }else if(n < 16385){
            int m = n >>> 1;
            long[] x0 = slice(a, 0, m);
            long[] x1 = slice(a, m, n);

            long[] z0 = squareMag(x0);
            long[] z2 = squareMag(x1);
            long[] z1 = squareMag(addMagnitudes(x0, x1));
            z1 = subtractMagnitudes(z1, z0);
            z1 = subtractMagnitudes(z1, z2);

            long[] out = new long[2 * n + 4];

            addAt(out, z0, 0);
            addAt(out, z1, m);
            addAt(out, z2, 2 * m);

            return trimLeadingZeros(out);
        }
        int m = (n+2)/3;
        //a = a2*B^2 + a1*B + a0
        long[] a0 = slice(a, 0, m);
        long[] a1 = slice(a, m, m *2);
        long[] a2 = slice(a, m *2, n);

        //evaluate points
        long[] p1 = addMagnitudes(addMagnitudes(a2, a1), a0);
        SignedMag pm1 = SignedMag.positive(a2).subtract(SignedMag.positive(a1)).add(SignedMag.positive(a0));
        long[] p2 = addMagnitudes(addMagnitudes(multiplySmallMag(a2, 4), multiplySmallMag(a1, 2)), a0);

        //square each point
        long[] v0 = squareMag(a0);
        long[] v1 = squareMag(p1);
        long[] vm1 = squareMag(pm1.mag);
        long[] v2 = squareMag(p2);
        long[] vInf = squareMag(a2);

        //interpolate
        long[] r2 = subtractMagnitudes(divideMagExactSmall(addMagnitudes(v1, vm1),2), addMagnitudes(v0, vInf));
        long[] s = divideMagExactSmall(subtractMagnitudes(v1, vm1), 2);
        long[] t = divideMagExactSmall(subtractMagnitudes(subtractMagnitudes(v2, v0), multiplySmallMag(vInf, 16)), 2);
        long[] r3 = divideMagExactSmall(subtractMagnitudes(subtractMagnitudes(t, s), multiplySmallMag(r2, 2)), 3);
        long[] r1 = subtractMagnitudes(s, r3);
        return addMagnitudes(addMagnitudes(addMagnitudes(addMagnitudes(v0, shiftLeftLimbs(r1, m)), shiftLeftLimbs(r2, 2 * m)), shiftLeftLimbs(r3, 3 * m)), shiftLeftLimbs(vInf, 4 * m));
    }
    @Override
    public LargeInteger square(){return new LargeInteger(squareMag(limbs), false);}
    public LargeInteger sqrtFloor() {return nthRootFloor(2);}
    public LargeInteger nthRootFloor(int n) {
        if(n == 1) return this;
        if (n < 1) throw new IllegalArgumentException("Exponent must be >= 1");
        if (lessThan(zero))
            throw new IllegalArgumentException("This version assumes N >= 0");
        if (isZeroArray(limbs) || equalsTo(one))
            return this;
        LargeInteger low = one;
        LargeInteger high = two;
        // Find an upper bound: high^n > N
        while (powLesserThanOrEqualTo(high, n, this))
            high = (LargeInteger) high.multiply(two);
        if(high.power(new LargeInteger(n)).equalsTo(this)) return high;
        // Binary search [low, high)
        while (high.subtract(low).greaterThan(LargeInteger.one)) {
            LargeInteger mid = ((LargeInteger) low.add(high)).longDivision(two);
            if (powLesserThanOrEqualTo(mid, n, this))
                low = mid;
            else high = mid;
        }
        return low;
    }
    public LargeInteger sqrtCeil() {
        LargeInteger f = sqrtFloor();
        if (f.square().equalsTo(this)) return f;
        return f.add(1);
    }
    public boolean isPerfectSquare() {
        if (this.sign) return false;
        return sqrtFloor().square().equalsTo(this);
    }
    public LargeInteger cube(){return (LargeInteger) this.square().multiply(this);}
    public static LargeInteger productRange(LargeInteger left, LargeInteger right) {
        if (left.equalsTo(right)) return left;
        if (right.subtract(left).equalsTo(one)) return (LargeInteger) left.multiply(right);
        LargeInteger mid = ((LargeInteger)left.add(right)).longDivision(two);
        return (LargeInteger) productRange(left, mid).multiply(productRange(mid.add(1), right));
    }
    public LargeInteger factorial(){
        if(sign) throw new ArithmeticException("Error: Factorial undefined for negative integers");
        if(isZeroArray(limbs) || equalsTo(one)) return one;
        return productRange(one, this);
    }
    private static long[] divideMagExactSmall(long[] a, long d) {
        if (d == 0L)
            throw new ArithmeticException("divide by zero");
        a = trimLeadingZeros(a);

        long[] q = new long[a.length];
        long rem = 0L;

        for (int i = a.length - 1; i >= 0; i--) {
            long[] qr = LongPair.divRem128By64To64(rem, a[i], d);
            q[i] = qr[0];
            rem = qr[1];
        }
        if (rem != 0L)
            throw new ArithmeticException("not exactly divisible");

        return trimLeadingZeros(q);
    }
    private static long[][] divideMagnitude(long[] a, long[] b) {
        if (isZeroArray(b))
            throw new ArithmeticException("divide by zero");
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);
        int cmp = compareArray(a, b);
        if (cmp < 0) return new long[][]{new long[]{0L}, a};
        if (cmp == 0) return new long[][]{new long[]{1L}, new long[]{0L}};
        if (b.length == 1){
            if (b[0] == 1L) return new long[][]{a, new long[]{0L}};
            if ((b[0] & (b[0] - 1L)) == 0L) {
                int shift = Long.numberOfTrailingZeros(b[0]);
                long[] q = shiftRightBits(a, shift);
                long[] r = {a[0] & (b[0] - 1L)};
                return new long[][]{trimLeadingZeros(q), trimLeadingZeros(r)};
            }
            long[] result = new long[a.length];
            long rem = 0;
            for(int i = result.length - 1; i >= 0; i--){
                LongPair cur = new LongPair(rem, a[i]);
                long[] DR = LongPair.divRem128By64To64(cur.high, cur.low, b[0]);
                result[i] = DR[0];
                rem = DR[1];
            }
            return new long[][]{trimLeadingZeros(result), new long[]{rem}};
        }
        int n = b.length;
        int m = a.length - n;

        int shift = Long.numberOfLeadingZeros(b[n - 1]);

        long[] v = shiftLeftBits(b, shift);
        long[] u0 = shiftLeftBits(a, shift);

        // Knuth needs one extra top limb.
        long[] u = new long[u0.length + 1];
        System.arraycopy(u0, 0, u, 0, u0.length);

        long[] q = new long[m + 1];

        for (int j = m; j >= 0; j--) {
            long ujn = u[j + n];
            long ujn1 = u[j + n - 1];

            long qhat;
            long rHat;

            if (Long.compareUnsigned(ujn, v[n - 1]) >= 0) {
                qhat = -1L;
                rHat = u[j + n - 1];
            } else {
                DivRemLongPair dr = new LongPair(ujn, ujn1).divideUnsignedByLong(v[n - 1]);
                if (dr.quotient().high != 0L)
                    throw new ArithmeticException("qhat overflow");

                qhat = dr.quotient().low;
                rHat = dr.remainder();
            }
            while (true) {
                LongPair left = LongPair.multiply64(qhat, v[n - 2]);
                LongPair right = new LongPair(rHat, u[j + n - 2]);

                if (!left.greaterThan(right))
                    break;

                qhat--;

                LongPair newR = LongPair.add64(rHat, v[n - 1]);
                rHat = newR.low;

                if (newR.high != 0L)
                    break;
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

                while (carry != 0L) {
                    LongPair s = LongPair.add64(u[k], carry);
                    u[k] = s.low;
                    carry = s.high;
                    k++;
                }
            }
            q[j] = qhat;
        }
        long[] r = new long[n];
        System.arraycopy(u, 0, r, 0, n);
        r = shiftRightBits(r, shift);
        q = trimLeadingZeros(q);
        r = trimLeadingZeros(r);
        return new long[][]{q, r};
    }
    public LargeInteger[] divideAndRemainder(LargeInteger other){
        if (isZeroArray(other.limbs))
            throw new ArithmeticException("Error: Cannot divide by 0");
        long[][] qr = divideMagnitude(this.limbs, other.limbs);
        LargeInteger q = new LargeInteger(qr[0], this.sign != other.sign);
        LargeInteger r = new LargeInteger(qr[1], this.sign);
        return new LargeInteger[]{q, r};
    }
    public LargeInteger longDivision(LargeInteger other){return this.divideAndRemainder(other)[0];}
    public LargeInteger floorDivision(LargeInteger other){
        LargeInteger[] pair = this.divideAndRemainder(other);
        if(pair[1].sign) return pair[0].subtract(1);
        return pair[0];
    }
    public LargeInteger totient() {
        if (isZeroArray(limbs)) throw new ArithmeticException("Error: Totient undefined for 0");
        else if(this.equalsTo(one)) return one;
        LargeInteger n = this.magnitude();
        LargeMap<LargeInteger, LargeFraction> factors = n.factor();
        LargeInteger result = n.copy();
        for (LargeInteger p : factors) {
            if (!p.isProbablyPrime())
                throw new ArithmeticException("Error: Totient requires full prime decomposition; composite atomic factor found: " + p);
            result = (LargeInteger) result.longDivision(p).multiply(p.subtract(1));
        }
        return result;
    }
    @Override
    public LargeFraction reciprocal(){return new LargeFraction(one, this);}
    public LargeInteger remainder(LargeInteger other){return divideAndRemainder(other)[1];}
    private static int modSmall(long[] a, SmallPrimeInfo info) {
        int p = info.p;
        long baseMod = info.baseMod;
        long rem = 0L;

        for (int i = a.length - 1; i >= 0; i--) {
            long limbMod = Long.remainderUnsigned(a[i], p);
            rem = (rem * baseMod + limbMod) % p;
        }
        return (int) rem;
    }
    private static long modUnsignedLong(long[] a, long d) {
        if (d == 0L)
            throw new ArithmeticException("divide by zero");

        a = trimLeadingZeros(a);
        if (a.length == 1)
            return Long.remainderUnsigned(a[0], d);
        long rem = 0L;
        for (int i = a.length - 1; i >= 0; i--) {
            long[] qr = LongPair.divRem128By64To64(rem, a[i], d);
            rem = qr[1];
        }

        return rem;
    }
    @Override
    public ScalarAtom mod(ScalarAtom other){
        if(other instanceof LargeInteger modulus){
            if (isZeroArray(modulus.limbs))
                throw new ArithmeticException("mod by zero");
            LargeInteger r = this.remainder(modulus);
            if(isZeroArray(r.limbs)) return zero;
            else if(r.sign != modulus.sign)
                return r.add(modulus);
            return r;
        }
        return this.mod(other);
    }
    public LargeInteger mod2(){return isEven() ? zero : one;}
    public LargeInteger modInverse(LargeInteger mod) {
        LargeInteger[] eg = this.extendedGCD(mod);
        if (eg[0].notEqualsTo(one))
            throw new ArithmeticException("Error: Modular inverse does not exist");
        return (LargeInteger) eg[1].mod(mod);
    }
    public boolean isOdd(){return (limbs[0] & 1L) == 1;}
    public boolean isEven(){return (limbs[0] & 1L) == 0;}
    public LargeInteger nCr(LargeInteger l) {
        if (this.lessThan(l)) return zero;
        else if(isZeroArray(l.limbs)) return one;
        return productRange((LargeInteger) this.subtract(l).add(one), this).longDivision(l.factorial());
    }
    public LargeFraction nPr(LargeInteger l){
        LargeInteger temp = (LargeInteger) this.subtract(l);
        if(temp.sign) return new LargeFraction(zero, one);
        return (LargeFraction) factorial().divide(temp.factorial());
    }
    public LargeFraction power(long p){
        if(this.equalsTo(two)){
            if(p < 0) return powerOfTwo(p).reciprocal();
            return new LargeFraction(powerOfTwo(p), one);
        }
        long expTemp = Math.abs(p);
        LargeInteger basTemp = this.copy();
        LargeInteger result = one;
        while(expTemp != 0){
            if(expTemp % 2 == 1){
                result = (LargeInteger) result.multiply(basTemp);
                if(expTemp == 1) break;
            }
            expTemp /= 2;
            basTemp = basTemp.square();
        }
        if(p < 0) return result.reciprocal();
        return new LargeFraction(result, one);
    }
    public LargeFraction power(LargeInteger other){
        if(this.equalsTo(one)) return LargeFraction.one;
        else if(isZeroArray(other.limbs)){
            if(isZeroArray(limbs))
                throw new ArithmeticException("Error: Indeterminate form: 0^0");
            return LargeFraction.one;
        }
        LargeFraction result = LargeFraction.one;
        LargeInteger expTemp = other.magnitude();
        LargeInteger basTemp = this.copy();
        while(true){
            if(expTemp.isOdd()){
                result = (LargeFraction) result.multiply(basTemp);
                if(expTemp.equalsTo(one)) break;
            }
            basTemp = basTemp.square();
            expTemp = expTemp.longDivision(two);
        }
        if(other.sign) return result.reciprocal();
        return result;
    }
    private static final class BarrettReducer{
        private final long[] modulus;
        private final long[] mu;
        private final int k;
        private final long[] bToKPlusOne;

        BarrettReducer(long[] modulus) {
            modulus = trimLeadingZeros(modulus);
            if (isZeroArray(modulus))
                throw new ArithmeticException("modulus is zero");

            this.modulus = modulus;
            this.k = modulus.length;
            this.mu = divideMagnitude(basePower(2 * k).limbs, modulus)[0];
            this.bToKPlusOne = basePower(k + 1).limbs;
        }
        long[] reduce(long[] x) {
            x = trimLeadingZeros(x);
            if (compareArray(x, modulus) < 0)
                return x;
            if (x.length <= 2 * k)
                return reduceProduct(x);
            return divideMagnitude(x, modulus)[1];
        }
        long[] reduceProduct(long[] x) {
            x = trimLeadingZeros(x);
            if (compareArray(x, modulus) < 0)
                return x;
            long[] q1 = shiftRightLimbs(x, k - 1);
            long[] q2 = multiplyMagnitudes(q1, mu);
            long[] q3 = shiftRightLimbs(q2, k + 1);
            long[] q3m = multiplyMagnitudes(q3, modulus);

            long[] r1 = slice(x, 0, k + 1);
            long[] r2 = slice(q3m, 0, k + 1);

            SignedMag m = SignedMag.positive(modulus);
            SignedMag r = SignedMag.positive(r1).subtract(SignedMag.positive(r2));
            if(r.negative){
                r = r.add(SignedMag.positive(bToKPlusOne));
                if(r.negative)
                    throw new AssertionError();
            }
            while (r.greaterThanOrEqualTo(m))
                r = r.subtract(m);
            return r.toPositiveMagnitudeChecked();
        }
        private long[] multiply(long[] a, long[] b) {return reduce(multiplyMagnitudes(a, b));}
        public long[] square(long[] a) {return reduce(multiplyMagnitudes(a, a));}
        private long[] powModBarrett(long[] base, long[] exponent) {
            if (isZeroArray(exponent))
                return reduce(new long[]{1L});

            base = reduce(base);
            int window = windowSizeForBits(bitLength(exponent));
            int tableSize = 1 << (window - 1);
            long[][] oddPowers = new long[tableSize][];

            // oddPowers[i] = base^(2*i + 1)
            oddPowers[0] = base;

            long[] base2 = square(base);

            for (int i = 1; i < tableSize; i++)
                oddPowers[i] = multiply(oddPowers[i - 1], base2);
            long[] result = reduce(new long[]{1L});
            int bit = bitLength(exponent) - 1;

            while (bit >= 0) {
                if (!testBit(exponent, bit)) {
                    result = square(result);
                    bit--;
                    continue;
                }

                int width = Math.min(window, bit + 1);

                // shrink width until the low bit of the window is 1
                while (width > 1 && !testBit(exponent, bit - width + 1))
                    width--;

                int value = 0;
                for (int i = 0; i < width; i++) {
                    value <<= 1;
                    if (testBit(exponent, bit - i))
                        value |= 1;
                }
                for (int i = 0; i < width; i++)
                    result = square(result);

                result = multiply(result, oddPowers[value >>> 1]);

                bit -= width;
            }

            return result;
        }
    }
    private static int windowSizeForBits(int bits) {
        if (bits < 256) return 3;
        if (bits < 1024) return 4;
        if (bits < 4096) return 5;
        if (bits < 16384) return 6;
        return 7;
    }
    private static void addMulLimbAt(long[] out, long[] a, long m, int offset) {
        long carry = 0L;
        for (int i = 0; i < a.length; i++) {
            LongPair p = LongPair.multiply64(a[i], m).addLow(out[offset + i]).addLow(carry);
            out[offset + i] = p.low;
            carry = p.high;
        }
        int k = offset + a.length;
        while (carry != 0L) {
            LongPair s = LongPair.add64(out[k], carry);
            out[k] = s.low;
            carry = s.high;
            k++;
        }
    }
    private static final class MontgomeryReducer{
        private final long[] modulus;
        private final int k;
        private final long mPrime;
        private final long[] rModM;
        private final long[] r2ModM;

        MontgomeryReducer(long[] modulus) {
            modulus = trimLeadingZeros(modulus);

            if (isZeroArray(modulus))
                throw new ArithmeticException("modulus is zero");

            if ((modulus[0] & 1L) == 0L)
                throw new ArithmeticException("Montgomery modulus must be odd");

            this.modulus = modulus;
            this.k = modulus.length;
            this.mPrime = -inverseMod2Pow64(modulus[0]);

            this.rModM = divideMagnitude(basePower(k).limbs, modulus)[1];
            this.r2ModM = divideMagnitude(basePower(2 * k).limbs, modulus)[1];
        }
        private static long inverseMod2Pow64(long x) {
            if ((x & 1L) == 0L)
                throw new ArithmeticException("inverse does not exist");
            long y = x;
            for(int i = 0; i < 5; i++)
                y *= 2L - x * y;
            return y;
        }
        public long[] reduce(long[] t) {
            t = trimLeadingZeros(t);

            long[] work = new long[Math.max(t.length, 2 * k + 2) + 2];
            System.arraycopy(t, 0, work, 0, t.length);

            for (int i = 0; i < k; i++) {
                long u = work[i] * mPrime;
                addMulLimbAt(work, modulus, u, i);
            }

            int resultLen = Math.min(k + 2, work.length - k);
            long[] result = new long[resultLen];
            System.arraycopy(work, k, result, 0, resultLen);
            result = trimLeadingZeros(result);
            if (compareArray(result, modulus) >= 0) {
                long borrow = 0L;
                for (int i = 0; i < result.length; i++) {
                    long ai = result[i];
                    long bi = i < modulus.length ? modulus[i] : 0L;
                    long tk = ai - bi;
                    long b1 = Long.compareUnsigned(ai, bi) < 0 ? 1L : 0L;

                    long u = tk - borrow;
                    long b2 = Long.compareUnsigned(tk, borrow) < 0 ? 1L : 0L;

                    result[i] = u;
                    borrow = b1 | b2;
                }
                if (borrow != 0L)
                    throw new ArithmeticException("subtractMagInPlace underflow");
            }
            return trimLeadingZeros(result);
        }
        public long[] multiply(long[] a, long[] b) {return reduce(multiplyMagnitudes(a, b));}
        public long[] square(long[] a) {return reduce(multiplyMagnitudes(a, a));}
        private long[] powModMontgomery(long[] base, long[] exponent) {
            if (isZeroArray(exponent))
                return fromMontgomery(rModM);

            base = toMontgomery(base);
            int window = windowSizeForBits(bitLength(exponent));
            int tableSize = 1 << (window - 1);
            long[][] oddPowers = new long[tableSize][];

            oddPowers[0] = base;

            long[] base2 = square(base);

            for (int i = 1; i < tableSize; i++)
                oddPowers[i] = multiply(oddPowers[i - 1], base2);

            long[] result = rModM;

            int bit = bitLength(exponent) - 1;

            while (bit >= 0) {
                if (!testBit(exponent, bit)) {
                    result = square(result);
                    bit--;
                    continue;
                }

                int width = Math.min(window, bit + 1);

                while (width > 1 && !testBit(exponent, bit - width + 1))
                    width--;

                int value = 0;
                for (int i = 0; i < width; i++) {
                    value <<= 1;
                    if (testBit(exponent, bit - i))
                        value |= 1;
                }
                for (int i = 0; i < width; i++)
                    result = square(result);

                result = multiply(result, oddPowers[value >>> 1]);

                bit -= width;
            }
            return fromMontgomery(result);
        }
        long[] toMontgomery(long[] x) {
            long[] xMod = divideMagnitude(x, modulus)[1];
            return multiply(xMod, r2ModM);
        }
        long[] fromMontgomery(long[] x) {return reduce(x);} // xR * R^-1 = x
    }
    public LargeInteger powMod(LargeInteger exponent, LargeInteger modulus) {
        if (isZeroArray(modulus.limbs))
            throw new ArithmeticException("modulus is zero");
        if (modulus.sign){
            LargeInteger tmp = powMod(exponent, modulus.negate());
            if(tmp.equalsTo(zero)) return tmp;
            return (LargeInteger) tmp.add(modulus);
        }
        if (modulus.equalsTo(one))
            return zero;

        if (exponent.sign)
            return this.modInverse(modulus).powMod(exponent.negate(), modulus);
        LargeInteger base = (LargeInteger) this.mod(modulus);

        long[] result;

        if (modulus.isOdd())
            result = new MontgomeryReducer(modulus.limbs).powModMontgomery(base.limbs, exponent.limbs);
        else{
            if(modulus.equalsTo(two)) return isOdd() ? one : zero;
            result = new BarrettReducer(modulus.limbs).powModBarrett(base.limbs, exponent.limbs);
        }
        return new LargeInteger(result, false);
    }
    @Override
    public ScalarAtom power(LargeFraction f) {return new LargePowerMonomial(new LargeFraction(this, one), f);}
    @Override
    public LargeDecimal approximateToDecimal(LargeInteger precision) {
        return null;
    }
    public static LargeInteger powerOfTwo(long bit) {
        if (bit < 0)
            throw new IllegalArgumentException("Error: Negative Exponent");
        long[] out = new long[(int)(bit >>> 6) + 1];
        out[(int)(bit >>> 6)] = 1L << (bit & 63);
        return new LargeInteger(out, false);
    }
    private static boolean powLesserThanOrEqualTo(LargeInteger base, int exp, LargeInteger limit) {return base.power(new LargeInteger(exp)).lesserThanOrEqualTo(limit);}
    private static boolean powEqualsTo(LargeInteger base, int exp, LargeInteger target) {
        LargeInteger result = LargeInteger.one;
        for (int i = 0; i < exp; i++) {
            result = (LargeInteger) result.multiply(base);
            if (result.greaterThan(target))
                return false;
        }
        return result.equalsTo(target);
    }private static long gcdSmallUnsigned(long[] a, long b) {
        a = trimLeadingZeros(a);
        if (b == 0L) {
            if (a.length == 1)
                return a[0];
            throw new ArithmeticException("gcd does not fit in one limb");
        }
        if (isZeroArray(a))
            return b;
        long r = modUnsignedLong(a, b);
        while (b != 0L) {
            long k = Long.remainderUnsigned(r, b);
            r = b;
            b = k;
        }
        return r;
    }
    private static long[] binaryGCD(long[] a, long[] b) {
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);
        if (isZeroArray(a)) return b;
        if (isZeroArray(b)) return a;

        int commonShift = Math.min(trailingZeroBits(a), trailingZeroBits(b));
        a = shiftRightBits(a, trailingZeroBits(a));
        b = shiftRightBits(b, trailingZeroBits(b));
        while (!isZeroArray(b)) {
            int cmp = compareArray(a, b);
            if (cmp == 0)
                return shiftLeftBits(a, commonShift);
            if (cmp > 0) {
                a = subtractMagnitudes(a, b);
                a = shiftRightBits(a, trailingZeroBits(a));
            } else {
                b = subtractMagnitudes(b, a);
                b = shiftRightBits(b, trailingZeroBits(b));
            }
        }
        return shiftLeftBits(a, commonShift);
    }
    private static long leading63(long[] mag) {
        mag = trimLeadingZeros(mag);

        if (isZeroArray(mag))
            return 0L;

        int top = mag.length - 1;
        long hi = mag[top];

        int bitsInHi = 64 - Long.numberOfLeadingZeros(hi);
        if (bitsInHi >= 63)
            return hi >>> (bitsInHi - 63);
        int need = 63 - bitsInHi;
        long out = hi << need;
        if (top > 0)
            out |= mag[top - 1] >>> (64 - need);
        return out;
    }
    private static LehmerStep lehmerStep(long[] a, long[] b) {
        long x = leading63(a);
        long y = leading63(b);

        long A = 1, B = 0;
        long C = 0, D = 1;

        long lastA = A, lastB = B, lastC = C, lastD = D;

        boolean moved = false;
        long steps = 0;

        while (y != 0) {
            long den1 = y + C;
            long den2 = y + D;

            if (den1 <= 0 || den2 <= 0)
                break;

            long q1 = (x + A) / den1;
            long q2 = (x + B) / den2;

            if (q1 != q2)
                break;

            if (q1 <= 0)
                break;

            if (mulSubWouldOverflow(A, q1, C) ||
                    mulSubWouldOverflow(B, q1, D) ||
                    mulSubWouldOverflow(x, q1, y))
                break;

            long nextA = C;
            long nextB = D;
            long nextC = A - q1 * C;
            long nextD = B - q1 * D;

            long nextX = y;
            long nextY = x - q1 * y;

            if (nextY < 0)
                break;

            // Validate approximate transformed pair is nonnegative.
            if (linearWouldBeNegative(nextA, x, nextB, y) ||
                    linearWouldBeNegative(nextC, x, nextD, y))
                break;

            A = nextA;
            B = nextB;
            C = nextC;
            D = nextD;

            x = nextX;
            y = nextY;

            lastA = A;
            lastB = B;
            lastC = C;
            lastD = D;
            moved = true;
            steps++;
        }
        if (!moved)
            return LehmerStep.NONE;

        return new LehmerStep(lastA, lastB, lastC, lastD, steps);
    }
    private static boolean linearWouldBeNegative(long a, long b, long x, long y) {return a*x+b*y<0;}
    private static boolean mulSubWouldOverflow(long a, long q, long c) {
        try {
            LongPair qc = LongPair.multiply64(q, c);
            new LongPair(a, 0).subtract(qc);
            return false;
        } catch (ArithmeticException e) {return true;}
    }
    private record LehmerStep(long A, long B, long C, long D, long steps) {
        static final LehmerStep NONE = new LehmerStep(1, 0, 0, 1, 0);
        boolean useful() {return steps > 0;}
    }
    private static int trailingZeroBits(long[] x) {
        x = trimLeadingZeros(x);
        if (isZeroArray(x))
            return 0;
        int limbs = 0;
        while (limbs < x.length && x[limbs] == 0L)
            limbs++;
        return limbs * 64 + Long.numberOfTrailingZeros(x[limbs]);
    }
    private static long signedLongMagnitudeAsUnsigned(long x) {return x >= 0 ? x : -x;}
    private static SignedMag linCombSmall(long[] a, long ca, long[] b, long cb) {
        if (ca == 0L)
            return multiplyMagnitudeBySignedLong(b, cb);
        if (cb == 0L)
            return multiplyMagnitudeBySignedLong(a, ca);
        boolean negA = ca < 0;
        boolean negB = cb < 0;

        long absA = signedLongMagnitudeAsUnsigned(ca);
        long absB = signedLongMagnitudeAsUnsigned(cb);
        long[] partA = multiplySmallMag(a, absA);
        long[] partB = multiplySmallMag(b, absB);
        return new SignedMag(partA, negA).add(new SignedMag(partB, negB));
    }
    private static long[][] applyLehmerStep(long[] a, long[] b, LehmerStep m) {
        SignedMag na = linCombSmall(a, m.A, b, m.B);
        if (na.negative()) return null;
        SignedMag nb = linCombSmall(a, m.C, b, m.D);
        if (nb.negative()) return null;
        return new long[][]{na.mag(), nb.mag()};
    }
    private static boolean acceptLehmerPair(long[] oldA, long[] oldB, long[] na, long[] nb) {
        if (isZeroArray(na))
            return false;
        if (isZeroArray(nb))
            return true;
        // Both outputs must be smaller than the old max operand.
        if (compareArray(na, oldA) >= 0)
            return false;
        if (compareArray(nb, oldA) >= 0)
            return false;
        // Avoid accepting identical/no-progress transforms.
        return compareArray(na, oldA) != 0 || compareArray(nb, oldB) != 0;
    }
    private static long[] gcdMagnitude(long[] a, long[] b) {
        a = trimLeadingZeros(a);
        b = trimLeadingZeros(b);

        if (isZeroArray(a)) return b;
        if (isZeroArray(b)) return a;

        if (compareArray(a, b) < 0) {
            long[] t = a;
            a = b;
            b = t;
        }

        while (!isZeroArray(b)) {
            if (compareArray(a, b) < 0) {
                long[] t = a;
                a = b;
                b = t;
            }

            int n = b.length; // smaller operand
            if(n == 1)
                return new long[]{gcdSmallUnsigned(a, b[0])};
            if (n <= 32)
                return binaryGCD(a, b);
            if (n >= 64) {
                LehmerStep lm = lehmerStep(a, b);
                if (lm.useful() && lm.steps() >= 5) {
                    long[][] next = applyLehmerStep(a, b, lm);
                    if (next != null) {
                        long[] na = trimLeadingZeros(next[0]);
                        long[] nb = trimLeadingZeros(next[1]);
                        if (acceptLehmerPair(a, b, na, nb)) {
                            a = na;
                            b = nb;
                            continue;
                        }
                    }
                }
            }
            long[][] div = divideMagnitude(a, b);
            a = b;
            b = div[1];
        }
        return a;
    }
    //calculating x and y such that ax+by=g. return in the format {g, x, y}
    // returns {g, x, y} such that this*x + other*y = g
    // returns {g, x, y} such that this*x + other*y = g
    public LargeInteger gcd(LargeInteger other){return new LargeInteger(gcdMagnitude(limbs, other.limbs), false);}
    public LargeInteger[] extendedGCD(LargeInteger other) {
        LargeInteger a0 = this.magnitude();
        LargeInteger b0 = other.magnitude();
        LargeInteger oldR = a0;
        LargeInteger r = b0;
        LargeInteger oldS = LargeInteger.one;   // coefficient of a0
        LargeInteger s = LargeInteger.zero;
        LargeInteger oldT = LargeInteger.zero;  // coefficient of b0
        LargeInteger t = LargeInteger.one;
        while (r.notEqualsTo(LargeInteger.zero)) {
            LargeInteger q = oldR.longDivision(r);
            LargeInteger nextR = (LargeInteger) oldR.subtract(q.multiply(r));
            oldR = r;
            r = nextR;
            LargeInteger nextS = (LargeInteger) oldS.subtract(q.multiply(s));
            oldS = s;
            s = nextS;
            LargeInteger nextT = (LargeInteger) oldT.subtract(q.multiply(t));
            oldT = t;
            t = nextT;
        }
        LargeInteger g = oldR;
        LargeInteger x = oldS;
        LargeInteger y = oldT;
        // Correct signs because the loop used magnitudes.
        if (this.sign)
            x = x.negate();
        if (other.sign)
            y = y.negate();
        ScalarAtom check = this.multiply(x).add(other.multiply(y));
        if (check.notEqualsTo(g))
            throw new ArithmeticException("Error: extendedGCD failed: " + this + "*(" + x + ") + " + other + "*(" + y + ") = " + check + ", expected " + g);
        return new LargeInteger[]{g, x, y};
    }
    public LargeInteger gcd(LargeInteger... other){
        LargeInteger copy = this.magnitude();
        for(LargeInteger I: other)
            copy = copy.gcd(I);
        return copy;
    }
    public LargeInteger lcm(LargeInteger other){
        LargeInteger G = this.gcd(other);
        return (LargeInteger) this.longDivision(G).multiply(other);
    }
    public LargeInteger lcm(LargeInteger... other){
        LargeInteger copy = this.magnitude();
        for(LargeInteger I: other)
            copy = copy.lcm(I);
        return copy;
    }
    public static final int[] SMALL_PRIMES = sieve(100000);
    private record SmallPrimeInfo(int p, long baseMod) {}
    private static final SmallPrimeInfo[] SMALL_PRIME_INFOS = makeSmallPrimeInfos();
    private static SmallPrimeInfo[] makeSmallPrimeInfos() {
        SmallPrimeInfo[] out = new SmallPrimeInfo[SMALL_PRIMES.length];
        for (int i = 0; i < SMALL_PRIMES.length; i++) {
            int p = SMALL_PRIMES[i];
            long baseMod = BigInteger.ONE.shiftLeft(64)
                    .mod(BigInteger.valueOf(p))
                    .longValue();
            out[i] = new SmallPrimeInfo(p, baseMod);
        }
        return out;
    }
    private static int defaultPrimeCertaintyBits(int bits) {
        if (bits < 128) return 80;
        if (bits < 512) return 100;
        if (bits < 2048) return 128;
        if (bits < 8192) return 160;
        return 192;
    }
    public boolean isProbablyPrime() {return isProbablyPrime(defaultPrimeCertaintyBits(this.bitLength()));}
    public boolean isProbablyPrime(int certaintyBits) {
        if (this.sign)
            return false;

        if (this.equalsTo(two))
            return true;

        if (this.lessThan(two))
            return false;

        if (!this.isOdd())
            return false;

        // Fast small-prime screen first.
        for (SmallPrimeInfo info : SMALL_PRIME_INFOS) {
            int p = info.p();
            if (modSmall(limbs, info) == 0)
                return this.equalsTo(new LargeInteger(p));
        }
        //miller-rabin test
        LargeInteger nMinusOne = this.subtract(1);
        LargeInteger multiplier = nMinusOne;
        long s = 0;
        while (multiplier.isEven()) {
            multiplier = multiplier.shiftRightBits(1);
            s++;
        }
        int rounds = Math.max(1, (certaintyBits + 1) >>> 1);
        for (int i = 0; i < rounds; i++) {
            LargeInteger witness = random(two, nMinusOne, MASTER_RNG); // 2 <= a < n-1
            LargeInteger x = witness.powMod(multiplier, this);
            if (x.equalsTo(one) || x.equalsTo(nMinusOne))
                continue;
            boolean witnessPassed = false;
            for (long r = 1L; r < s; r++) {
                x = x.powMod(two, this);
                if (x.equalsTo(nMinusOne)) {
                    witnessPassed = true;
                    break;
                }
                if (x.equalsTo(one))
                    return false;
            }
            if (!witnessPassed)
                return false;
        }
        return true;
    }
    private static final int[] WHEEL_30_RESIDUES = {1, 7, 11, 13, 17, 19, 23, 29};
    private static final int[] WHEEL_30_STEPS = {6, 4, 2, 4, 2, 4, 6, 2};
    public LargeInteger nextProbablePrime() {
        if (this.sign || this.lessThan(two))
            return two;

        if (this.lessThan(three))
            return three;

        if (this.lessThan(five))
            return five;

        LargeInteger candidate = this.add(1);

        int rem30 = (int) modUnsignedLong(candidate.limbs, 30);
        int wheelIndex = firstWheelIndexAtOrAbove(rem30);

        int targetResidue = WHEEL_30_RESIDUES[wheelIndex];
        int delta = targetResidue - rem30;

        if (delta < 0)
            delta += 30;

        candidate = (LargeInteger) candidate.add(new LargeInteger(delta));

        while (true) {
            if (candidate.isProbablyPrime())
                return candidate;

            candidate = (LargeInteger) candidate.add(new LargeInteger(WHEEL_30_STEPS[wheelIndex]));
            wheelIndex++;

            if (wheelIndex == WHEEL_30_STEPS.length)
                wheelIndex = 0;
        }
    }
    private static int firstWheelIndexAtOrAbove(int rem) {
        for (int i = 0; i < WHEEL_30_RESIDUES.length; i++) {
            if (WHEEL_30_RESIDUES[i] >= rem)
                return i;
        }
        return 0;
    }
    public static LargeInteger randomPrime(int chunks) {
        if (chunks <= 0)
            throw new IllegalArgumentException("chunks must be positive");
        SplittableRandom rng = MASTER_RNG.split();
        LargeInteger wheel = new LargeInteger(210);
        while (true) {
            LargeInteger candidate = randomChunks(chunks).magnitude();

            int residue = WHEEL_30_RESIDUES[rng.nextInt(WHEEL_30_RESIDUES.length)];
            LargeInteger rem = (LargeInteger) candidate.mod(wheel);

            candidate = (LargeInteger) candidate.subtract(rem).add(new LargeInteger(residue));

            if (candidate.bitLength() < (chunks - 1) * 64)
                continue;
            if (candidate.isProbablyPrime())
                return candidate;
        }
    }
    public static LargeInteger randomPrimeBits(int bits) {
        if (bits <= 1)
            throw new IllegalArgumentException("bits must be > 1");
        while (true) {
            long[] limbs = new long[(bits + 63) >>> 6];

            SplittableRandom rng = MASTER_RNG.split();
            for (int i = 0; i < limbs.length; i++)
                limbs[i] = rng.nextLong();
            int topBit = (bits - 1) & 63;

            // Clear bits above requested bit length.
            if (topBit != 63) {
                long mask = (1L << (topBit + 1)) - 1L;
                limbs[limbs.length - 1] &= mask;
            }

            // Force exact bit length.
            limbs[limbs.length - 1] |= 1L << topBit;

            // Force odd.
            limbs[0] |= 1L;

            LargeInteger e = new LargeInteger(limbs, false);
            if (e.isProbablyPrime())
                return e;
        }
    }
    public static int[] sieve(int bound) {
        if (bound < 2)
            return new int[0];

        if (bound == 2)
            return new int[]{2};

        // composite[i] represents odd limbs 2*i + 3.
        // i = 0 -> 3
        // i = 1 -> 5
        // i = 2 -> 7
        int oddCount = (bound - 1) >>> 1;
        boolean[] composite = new boolean[oddCount];

        int sqrt = (int) Math.sqrt(bound);

        for (int p = 3; p <= sqrt; p += 2) {
            int pIndex = (p - 3) >>> 1;

            if (!composite[pIndex]) {
                long start = (long) p * p;

                for (long multiple = start; multiple <= bound; multiple += 2L * p) {
                    composite[(int) ((multiple - 3) >>> 1)] = true;
                }
            }
        }

        int count = 1; // prime 2

        for (int i = 0; i < oddCount; i++) {
            if (!composite[i])
                count++;
        }

        int[] primes = new int[count];
        primes[0] = 2;

        int index = 1;

        for (int i = 0; i < oddCount; i++) {
            if (!composite[i])
                primes[index++] = 2 * i + 3;
        }
        return primes;
    }
    public static LargeList<LargeInteger> primesUpTo(int limit) {
        LargeList<LargeInteger> result = new LargeList<>();
        if (limit < 2) return result;
        int[] primes = sieve(limit);
        for(int i: primes) result.add(new LargeInteger(i));
        return result;
    }
    private LargeInteger pollardPMinusOneQuick() {
        if (this.isEven()) return two;
        long[] bounds = {10_000L, 100_000L, 1_000_000L};
        for (long B : bounds) {
            LargeInteger d = this.pollardPMinusOne(B);
            if (d.greaterThan(one) && d.lessThan(this))
                return d;
            if (d.equalsTo(this))
                return one; // over-accumulated; stop or try different base later
        }
        return one;
    }
    private LargeInteger pollardPMinusOne(long bound) {
        if (this.isEven()) return two;
        MontgomeryReducer reducer = new MontgomeryReducer(limbs);
        long[] a = {2L};

        int checkpoint = 0;
        int[] PM1_PRIMES = sieve((int)bound);
        for (int q : PM1_PRIMES) {
            if (q > bound) break;
            long qPower = q;
            while (qPower <= bound / q)
                qPower *= q;
            a = reducer.powModMontgomery(a, new long[]{qPower});
            checkpoint++;
            if (checkpoint == 128) {
                checkpoint = 0;
                LargeInteger g = new LargeInteger(a, false).subtract(1).gcd(this);
                if (g.greaterThan(one) && g.lessThan(this))
                    return g;
                else if (g.equalsTo(this))
                    return one; // this seed failed / over-accumulated
            }
        }
        LargeInteger g = new LargeInteger(a, false).subtract(1).gcd(this);
        if (g.greaterThan(one) && g.lessThan(this))
            return g;
        return one;
    }
    private LargeInteger brentRhos() {
        if (this.isEven()) return two;

        int digits = this.toString().length();

        int attempts;
        long steps;

        if (digits <= 25) {
            attempts = 64;
            steps = 50_000L;
        } else if (digits <= 40) {
            attempts = 32;
            steps = 500_000L;
        } else if (digits <= 60) {
            attempts = 16;
            steps = 2_000_000L;
        } else if(digits <= 80) {
            attempts = 8;
            steps = 4_000_000L;
        }else{
            attempts = 4;
            steps = 10_000_000L;
        }

        MontgomeryReducer reducer = new MontgomeryReducer(limbs);

        for (long attempt = 1; attempt <= attempts; attempt++) {
            LargeInteger d = brentRhoAttempt(steps, reducer);
            if (d.greaterThan(one) && d.lessThan(this))
                return d;
        }
        return one;
    }
    private LargeInteger brentRhoAttempt(long steps, MontgomeryReducer reducer) {
        SplittableRandom random = MASTER_RNG.split();
        long[] y = random(two, (LargeInteger) this.subtract(two), random).limbs;
        long[] c = random(one, (LargeInteger) this.subtract(one), random).limbs;

        final long m = 1024;
        long r = 1;
        long times = 0;

        long[] g = {1L};
        long[] q;
        long[] x = {0L};
        long[] ys = {0L};

        while (Arrays.equals(g, new long[]{1L}) && times < steps) {
            x = y.clone();
            for (long i = 0; i < r && times < steps; i++) {
                y = reducer.reduce(addMagnitudes(squareMag(y), c));
                times++;
            }
            long k = 0;
            while (k < r && Arrays.equals(g, new long[]{1L}) && times < steps) {
                ys = y.clone();
                q = new long[]{1L};

                long limit = Math.min(m, r - k);

                for (long i = 0; i < limit && times < steps; i++) {
                    y = reducer.reduce(addMagnitudes(squareMag(y), c));
                    SignedMag diff = SignedMag.positive(x).subtract(SignedMag.positive(y));
                    if (isZeroArray(diff.mag)) return one;
                    q = reducer.reduce(multiplyMagnitudes(q, diff.mag));
                    times++;
                }
                g = gcdMagnitude(q, limbs);
                k += m;
            }
            if (r > (Long.MAX_VALUE >> 1)) return one;
            r <<= 1;
        }
        if (Arrays.equals(g, new long[]{1L})) return one;
        if (Arrays.equals(g, limbs)) {
            g = new long[]{1L};
            for (long i = 0; i < 10_000 && Arrays.equals(g, new long[]{1L}); i++) {
                ys = reducer.reduce(addMagnitudes(squareMag(ys), c));
                g = gcdMagnitude(SignedMag.positive(x).subtract(SignedMag.positive(ys)).mag, limbs);
            }
            if (Arrays.equals(g, new long[]{1L}) || Arrays.equals(g, limbs)) return one;
        }
        return new LargeInteger(g, false);
    }
    private boolean shouldTryFermat() {
        int digits = toString().length();

        if (digits <= 30) return true;       // cheap enough
        if (digits > 80) return false;       // usually bad

        LargeInteger x = sqrtCeil();
        LargeInteger gap = (LargeInteger) x.square().subtract(this);

        // If N is already very near a square, Fermat might hit fast.
        return gap.toString().length() <= Math.max(6, digits / 4);
    }
    private LargeInteger fermatFactor() {
        if (isEven()) return two;
        LargeInteger x = sqrtCeil();
        LargeInteger y2 = (LargeInteger) x.square().subtract(this);
        for (long i = 0; i < 100000L; i++) {
            LargeInteger y = y2.sqrtFloor();
            if (y.square().equalsTo(y2)) {
                LargeInteger a = (LargeInteger) x.subtract(y);
                LargeInteger b = (LargeInteger) x.add(y);
                if (a.greaterThan(LargeInteger.one) && b.lessThan(this))
                    return a;
            }
            // update (x+1)^2 - n from x^2 - n
            // y2_new = y2 + 2x + 1
            y2 = (LargeInteger) y2.add(x.multiply(2)).add(one);
            x = x.add(1);
        }

        return LargeInteger.one;
    }
    private static class ECPoint {
        LargeInteger x;
        LargeInteger y;
        boolean infinity;
        static ECPoint infinity() {
            ECPoint p = new ECPoint();
            p.infinity = true;
            return p;
        }
        ECPoint(LargeInteger x, LargeInteger y) {
            this.x = x;
            this.y = y;
            this.infinity = false;
        }

        private ECPoint() {}
    }
    private static class EllipticCurveModN { //represents curves of the form: y^2 = x^3 + ax + b mod n
        LargeInteger a;
        LargeInteger b;
        LargeInteger n;
        EllipticCurveModN(LargeInteger a, LargeInteger b, LargeInteger n) {
            this.a = (LargeInteger) a.mod(n);
            this.b = (LargeInteger) b.mod(n);
            this.n = n;
        }
        private LargeInteger mod(LargeInteger x) {
            x = (LargeInteger) x.mod(n);
            if (x.lessThan(LargeInteger.zero)) x = (LargeInteger) x.add(n);
            return x;
        }
        ECPoint add(ECPoint P, ECPoint Q) {
            if (P.infinity) return Q;
            if (Q.infinity) return P;
            // If x1 == x2 and y1 == -y2, result is infinity
            if (P.x.equalsTo(Q.x) && mod((LargeInteger) P.y.add(Q.y)).equalsTo(LargeInteger.zero))
                return ECPoint.infinity();
            LargeInteger numerator;
            LargeInteger denominator;

            if (P.x.equalsTo(Q.x) && P.y.equalsTo(Q.y)) {
                // point doubling
                numerator = (LargeInteger) P.x.square().multiply(3).add(a);
                denominator = P.y.multiply(2);
            } else {
                // ordinary addition
                numerator = (LargeInteger) Q.y.subtract(P.y);
                denominator = (LargeInteger) Q.x.subtract(P.x);
            }

            numerator = mod(numerator);
            denominator = mod(denominator);

            LargeInteger inv = inverseOrFactor(denominator, n);
            LargeInteger m = mod((LargeInteger) numerator.multiply(inv));

            LargeInteger x3 = mod((LargeInteger) m.square().subtract(P.x).subtract(Q.x));
            LargeInteger y3 = mod((LargeInteger) m.multiply(P.x.subtract(x3)).subtract(P.y));
            return new ECPoint(x3, y3);
        }
        ECPoint multiply(ECPoint P, long k) {
            ECPoint result = ECPoint.infinity();
            ECPoint base = P;
            while (true) {
                if (k % 2 == 1){
                    result = add(result, base);
                    if(k == 1) break;
                }
                base = add(base, base);
                k /= 2;
            }
            return result;
        }
    }
    private static class FactorFoundException extends RuntimeException {
        LargeInteger factor;
        private FactorFoundException(LargeInteger g){factor = g.copy();}
    }
    private static LargeInteger inverseOrFactor(LargeInteger d, LargeInteger n) {
        d = (LargeInteger) d.mod(n);
        LargeInteger[] eg = d.extendedGCD(n);
        LargeInteger g = eg[0];
        if (g.equalsTo(one)) {
            LargeInteger inv = (LargeInteger) eg[1].mod(n);
            if (inv.lessThan(zero)) inv = (LargeInteger) inv.add(n);
            return inv;
        }
        if (g.notEqualsTo(n))
            throw new FactorFoundException(g);
        throw new ArithmeticException("Point operation failed without revealing factor");
    }
    private LargeInteger ecmStage2Walk(EllipticCurveModN curve, ECPoint Q, int B1, int B2) {
        int previousPrime = 0;
        ECPoint R = null;
        for (int q : sieve(B2)){
            if (q <= B1) continue;
            try {
                if (R == null)
                    R = curve.multiply(Q, q);
                else {
                    int gap = q - previousPrime;
                    ECPoint gapQ = curve.multiply(Q, gap);
                    R = curve.add(R, gapQ);
                }
                previousPrime = q;
            }
            catch (FactorFoundException e) {return e.factor;}
            catch (ArithmeticException ignored) {return one;}
        }
        return one;
    }
    private LargeInteger ecmMedium(){
        int[] bounds = {1000, 2000, 5000, 10_000};
        int curvesPerBound = 32;
        for(int i: bounds){
            LargeInteger d = ecmAttempt(i, curvesPerBound);
            if(d.notEqualsTo(one) && d.notEqualsTo(this)) return d;
        }
        return one;
    }
    private LargeInteger ecmHard(){
        int[] bounds = {1000, 2000, 5000, 10000, 20000, 50000, 100000, 200000, 500000, 1000000, 2000000, 5000000, 10000000, 20000000};
        int curvesPerBound = 128;
        for(int i: bounds){
            LargeInteger d = ecmAttempt(i, curvesPerBound);
            if(d.notEqualsTo(one) && d.notEqualsTo(this)) return d;
        }
        return one;
    }
    private LargeInteger ecmAttempt(int B1, int curvesPerBound) {
        for (int i = 0; i < curvesPerBound; i++) {
            SplittableRandom curveRng = MASTER_RNG.split();

            LargeInteger x = random(two, (LargeInteger) this.subtract(two), curveRng);
            LargeInteger y = random(two, (LargeInteger) this.subtract(two), curveRng);
            LargeInteger a = random(two, (LargeInteger) this.subtract(two), curveRng);
            LargeInteger b = (LargeInteger) y.square().subtract(x.cube()).subtract(a.multiply(x)).mod(this);
            LargeInteger disc = (LargeInteger) a.cube().multiply(4).add(b.square().multiply(27)).mod(this);
            LargeInteger g = disc.gcd(this);
            if (g.greaterThan(one) && g.lessThan(this))
                return g;
            if (g.equalsTo(this))
                continue;

            EllipticCurveModN curve = new EllipticCurveModN(a, b, this);
            ECPoint P = new ECPoint(x, y);

            try {
                ECPoint Q = P;
                for (int p : sieve(B1)) {
                    long power = p;
                    while (power * p <= B1)
                        power = power * p;
                    Q = curve.multiply(Q, power);
                }
                LargeInteger I = ecmStage2Walk(curve, Q, B1, B1 * 100);
                if (I.greaterThan(one) && I.lessThan(this))
                    return I;
            } catch (FactorFoundException e) {return e.factor;}
            catch (ArithmeticException ignored) {}
        }
        return one;
    }
    private static class QSPrime {
        LargeInteger p;
        LargeInteger root1;
        LargeInteger root2;
        QSPrime(LargeInteger p, LargeInteger root1, LargeInteger root2) {
            this.p = p;
            this.root1 = root1;
            this.root2 = root2;
        }
    }
    private static class QSRelation {
        LargeInteger x; //the actual x used
        LargeInteger diff; //|x^2-N|
        LargeList<Long> exponents; //exponents when factoring |x^2-N|
        LargeList<Long> parityBits; //exponents mod 2
        QSRelation(LargeInteger x, LargeInteger diff, LargeList<Long> exponents, LargeList<Long> parities) {
            this.x = x;
            this.diff = diff;
            this.exponents = exponents;
            this.parityBits = parities;
        }
    }
    private static class QSMatrixRow {
        LargeList<Long> parity;      // factor-base parity vector
        LargeList<Long> dependency;  // which relation rows were XORed together
        QSMatrixRow(LargeList<Long> parity, LargeInteger relationIndex, LargeInteger relationCount) {
            this.parity = parity.copy();
            this.dependency = new LargeList<>();
            for(LargeInteger i = zero; i.lessThan(relationCount.add(63).longDivision(sixtyFour)); i=i.add(1))
                this.dependency.add(0L);
            this.dependency.set(relationIndex.longDivision(sixtyFour), Long.parseLong(two.power(relationIndex.AND(sixtyThree)).numerator.toString()));
        }
    }
    private static LargeList<QSPrime> buildQSFactorBase(LargeInteger N, int B){
        LargeList<QSPrime> factors = new LargeList<>();
        for(LargeInteger prime: primesUpTo(B)){
            if(prime.equalsTo(two)){
                factors.add(new QSPrime(prime, N.mod2(), N.mod2()));
                continue;
            }
            if(N.legendreSymbol(prime).notEqualsTo(one)) continue;
            LargeInteger root1;
            LargeInteger root2;
            //special case: p%4 == 3
            if(prime.mod(four).equalsTo(three)){
                root1 = N.powMod(prime.add(1).longDivision(four), prime);
                root2 = (LargeInteger) prime.subtract(root1);
                factors.add(new QSPrime(prime, root1, root2));
                continue;
            }
            //the main algorithm here will be called Tonelli-Shanks, and can help determine root1 and root2.
            //first step: write p-1 as Q*2^S
            LargeInteger multiplier = prime.subtract(1);
            long exponent = 0;
            while(multiplier.isEven()){
                exponent++;
                multiplier = multiplier.longDivision(two);
            }
            //step 2: find the smallest M that is a quadratic nonresidue mod p
            LargeInteger Z = zero;
            for(LargeInteger z = LargeInteger.one; z.lessThan(prime); z=z.add(1)){
                if(z.legendreSymbol(prime).equalsTo(negativeOne)){
                    Z = z;
                    break;
                }
            }
            //step 3: new variables :D
            LargeInteger c = Z.powMod(multiplier, prime);
            LargeInteger t = N.powMod(multiplier, prime);
            LargeInteger R = N.powMod(multiplier.add(1).longDivision(two), prime);
            if(isZeroArray(t.limbs)) {
                root1 = zero;
                root2 = prime;
                factors.add(new QSPrime(prime, root1, root2));
                continue;
            }
            //step 4: looping step
            while (t.notEqualsTo(one)){
                // find smallest i such that t^(2^i) == 1 mod p
                long iterations = 0;
                LargeInteger temp = t;
                while (temp.notEqualsTo(one)) {
                    temp = temp.powMod(two, prime); // t^(2^2), t^(2^3), ...
                    iterations++;
                    if (iterations == exponent)
                        throw new ArithmeticException("Tonelli-Shanks failed");
                }
                LargeInteger b = c.powMod(powerOfTwo(exponent - iterations - 1), prime);
                exponent = iterations;
                c = b.powMod(two, prime);
                t = (LargeInteger) t.multiply(c).mod(prime);
                R = (LargeInteger) R.multiply(c).mod(prime);
            }
            factors.add(new QSPrime(prime, R, (LargeInteger) prime.subtract(R)));
        }
        return factors;
    }
    public LargeInteger legendreSymbol(LargeInteger p) {
        if (p.lesserThanOrEqualTo(two) || !p.isProbablyPrime())
            throw new IllegalArgumentException("p must be an odd prime");
        LargeInteger a = (LargeInteger) this.mod(p);
        if (isZeroArray(a.limbs))
            return a;
        LargeInteger exp = p.subtract(1).longDivision(two);
        LargeInteger r = a.powMod(exp, p);
        if (r.equalsTo(one))
            return one;
        else if (r.equalsTo(p.subtract(one)))
            return negativeOne;
        throw new IllegalStateException("Euler criterion failed: p may not be prime");
    }
    private static void collectRelations(
            LargeInteger N,
            LargeInteger sqrtN,
            LargeInteger interval,
            LargeList<QSPrime> factorBase,
            LargeList<QSRelation> relations
    ) {
        LargeInteger end = (LargeInteger) sqrtN.add(interval);
        for (LargeInteger x = sqrtN; x.lesserThanOrEqualTo(end); x = x.add(1)) {
            LargeInteger q = (LargeInteger) x.square().subtract(N);
            QSRelation relation = tryBuildRelation(x, q, factorBase);
            if (relation != null) {
                relations.add(relation);
                if (relations.size().greaterThanOrEqualTo(factorBase.size().add(20))) return;
            }
        }
    }
    private static QSRelation tryBuildRelation(
            LargeInteger x,
            LargeInteger q,
            LargeList<QSPrime> factorBase
    ) {
        LargeList<Long> exponents = new LargeList<>();

        LargeInteger rem = q;
        if (factorBase.intSized())
            for (int j = 0; j < factorBase.intSize(); j++) {
                LargeInteger p = factorBase.get(j).p;
                if (p.equalsTo(negativeOne))
                    continue;
                long exponent = 0;
                LargeInteger[] DR = rem.divideAndRemainder(p);
                while (isZeroArray(DR[1].limbs)) {
                    rem = DR[0];
                    DR = rem.divideAndRemainder(p);
                    exponent++;
                }
                exponents.add(exponent);
                if (rem.equalsTo(one))
                    break;
            }
        else
            for (LargeInteger j = zero; j.lessThan(factorBase.size()); j = j.add(1)){
                LargeInteger p = factorBase.get(j).p;
                if (p.equalsTo(negativeOne))
                    continue;
                long exponent = 0;
                LargeInteger[] DR = rem.divideAndRemainder(p);
                while (isZeroArray(DR[1].limbs)) {
                    rem = DR[0];
                    DR = rem.divideAndRemainder(p);
                    exponent++;
                }
                exponents.add(exponent);
                if (rem.equalsTo(one))
                    break;
            }
        if (!rem.equalsTo(one))
            return null;
        LargeList<Long> parityBits = new LargeList<>();
        if(exponents.intSized()){
            for(int i = 0; i < (exponents.intSize() + 63) >>> 6; i++)
                parityBits.add(0L);
            for (int j = 0; j < exponents.intSize(); j++)
                if ((exponents.get(j) & 1L) != 0)
                    parityBits.set(j >>> 6, parityBits.get(j>>>6) ^ 1L << (j & 63));
        }else{
            for(LargeInteger i = zero; i.lessThan(exponents.size().add(63).longDivision(sixtyFour)); i=i.add(1))
                parityBits.add(0L);
            for(LargeInteger j = zero; j.lessThan(exponents.size()); j=j.add(1))
                if((exponents.get(j) & 1L) != 0)
                    parityBits.set(j.longDivision(sixtyFour), parityBits.get(j.longDivision(sixtyFour)) ^ 1L << Integer.parseInt(j.AND(sixtyThree).toString()));
        }
        return new QSRelation(x, q, exponents, parityBits);
    }
    public LargeInteger quadraticSieve() { //debug mode
        LargeInteger N = this.magnitude();
        int digits = N.toString().length();
        int B = 15;
        int neededExtra = 50;
        LargeList<QSPrime> factorBase = buildQSFactorBase(N, B);
        LargeList<QSRelation> relations = new LargeList<>();
        LargeInteger sqrtN = N.sqrtCeil();
        LargeInteger interval = zero;
        LargeInteger QS_BLOCK_SIZE = factorBase.size().multiply(20L).greaterThan(new LargeInteger(100000L)) ? factorBase.size().multiply(20L) : new LargeInteger(100000L);
        while (relations.size().lessThan(factorBase.size().add(neededExtra))) {
            collectRelations(N, sqrtN, interval, factorBase, relations);
            interval = (LargeInteger) interval.add(QS_BLOCK_SIZE);
        }
        LargeList<LargeList<Long>> dependencies = new LargeList<>();
        if(factorBase.intSized()){
            while((long) dependencies.intSize() < factorBase.intSize()+30L){
                LargeList<Long> dependency = findDependency(relations);
                if(dependency != null)
                    dependencies.add(dependency);
            }
        }else{
            while(dependencies.size().lessThan(factorBase.size().add(30))){
                LargeList<Long> dependency = findDependency(relations);
                if(dependency != null)
                    dependencies.add(dependency);
            }
        }
        //attempt dependencies
        for(LargeList<Long> L: dependencies){
            LargeInteger K = tryDependency(N, factorBase, relations, L);
            if(K.greaterThan(one) && K.lessThan(N)) return K;
        }
        return one;
    }
    private static void xorInto(QSMatrixRow target, QSMatrixRow source) {
        if(target.parity.size().notEqualsTo(source.parity.size()))
            throw new IllegalArgumentException("Error: Rows must have same sizes");
        if (target.parity.intSized()) {
            for (int i = 0; i < target.parity.intSize(); i++)
                target.parity.set(i, target.parity.get(i) ^ source.parity.get(i));
            for (int i = 0; i < target.dependency.intSize(); i++)
                target.dependency.set(i, target.dependency.get(i) ^ source.dependency.get(i));
        }else{
            for (LargeInteger i = zero; i.lessThan(target.parity.size()); i=i.add(1))
                target.parity.set(i, target.parity.get(i) ^ source.parity.get(i));
            for (LargeInteger i = zero; i.lessThan(target.dependency.size()); i=i.add(1))
                target.dependency.set(i, target.dependency.get(i) ^ source.dependency.get(i));
        }
    }
    private static LargeList<Long> findDependency(LargeList<QSRelation> relations) {
        if (relations.intSized()) {
            int relationCount = relations.intSize();
            LargeList<QSMatrixRow> rows = new LargeList<>();
            for (int i = 0; i < relationCount; i++)
                rows.add(new QSMatrixRow(relations.get(i).parityBits, new LargeInteger(i), new LargeInteger(relationCount)));
            int columnCount = relations.getFirst().exponents.intSize();
            QSMatrixRow[] pivotForColumn = new QSMatrixRow[columnCount];
            for (int r = 0; r < rows.intSize(); r++) {
                QSMatrixRow row = rows.get(r);
                while (true) {
                    //find the pivot, or the first bit for matrix reduction
                    int pivot = -1;
                    for (int word = 0; word < row.parity.intSize(); word++) {
                        long x = row.parity.get(word);
                        if (x != 0L) {
                            pivot = (word << 6) + Long.numberOfTrailingZeros(x);
                            break;
                        }
                    }
                    if (pivot == -1)
                        return row.dependency; // found dependency
                    if (pivotForColumn[pivot] == null) {
                        pivotForColumn[pivot] = row;
                        break;
                    }
                    xorInto(row, pivotForColumn[pivot]);
                }
            }
        }else{
            LargeInteger relationCount = relations.size();
            LargeList<QSMatrixRow> rows = new LargeList<>();
            for (LargeInteger i = zero; i.lessThan(relationCount); i=i.add(1))
                rows.add(new QSMatrixRow(relations.get(i).parityBits, i, relationCount));
            LargeInteger columnCount = relations.getFirst().exponents.size();
            LargeList<QSMatrixRow> pivotForColumn = new LargeList<>();
            for(LargeInteger i = zero; i.lessThan(columnCount); i=i.add(1))
                pivotForColumn.add(null);
            for (int r = 0; r < rows.intSize(); r++) {
                QSMatrixRow row = rows.get(r);
                while (true) {
                    //find the pivot, or the first bit for matrix reduction
                    LargeInteger pivot = negativeOne;
                    for (LargeInteger word = zero; word.lessThan(row.parity.size()); word=word.add(1)) {
                        long x = row.parity.get(word);
                        if (x != 0L) {
                            pivot = word.multiply(64).add(Long.numberOfTrailingZeros(x));
                            break;
                        }
                    }
                    if (pivot.equalsTo(negativeOne))
                        return row.dependency; // found dependency
                    if (pivotForColumn.get(pivot) == null) {
                        pivotForColumn.set(pivot, row);
                        break;
                    }
                    xorInto(row, pivotForColumn.get(pivot));
                }
            }
        }
        return null; // need more relations
    }
    private static boolean dependencyHasBit(LargeList<Long> dependency, int index) {
        return ((dependency.get(index >>> 6) >>> (index & 63)) & 1L) != 0;
    }
    private static boolean dependencyHasBit(LargeList<Long> dependency, LargeInteger index){
        return dependency.get(index.longDivision(sixtyFour).longDivision(two.power(index.AND(sixtyThree)).numerator).AND(one)) != 0L;
    }
    private static LargeInteger tryDependency(
            LargeInteger N,
            LargeList<QSPrime> factorBase,
            LargeList<QSRelation> relations,
            LargeList<Long> dependency
    ) {
        if (factorBase.intSized() && dependency.intSized()){
            int factorBaseSize = factorBase.intSize();
            LargeInteger X = one;
            long[] totalExponents = new long[factorBaseSize];
            for (int i = 0; i < relations.intSize(); i++) {
                if (!dependencyHasBit(dependency, i)) continue;
                QSRelation r = relations.get(i);
                X = (LargeInteger) X.multiply(r.x).mod(N);
                for (int j = 0; j < factorBaseSize; j++)
                    totalExponents[j] += r.exponents.get(j);
            }
            LargeInteger Y = one;
            for (int j = 0; j < factorBaseSize; j++) {
                LargeInteger p = factorBase.get(j).p;
                if (p.equalsTo(negativeOne)) continue;
                long halfExp = totalExponents[j] >>> 1;
                for (long k = 0; k < halfExp; k++)
                    Y = (LargeInteger) Y.multiply(p).mod(N);
            }
            LargeInteger g1 = ((LargeInteger) X.subtract(Y)).magnitude().gcd(N);
            if (g1.greaterThan(one) && g1.lessThan(N))
                return g1;
            LargeInteger g2 = ((LargeInteger) X.add(Y)).magnitude().gcd(N);
            if (g2.greaterThan(one) && g2.lessThan(N))
                return g2;
        }else{
            LargeInteger factorBaseSize = factorBase.size();
            LargeInteger X = one;
            LargeList<Long> totalExponents = new LargeList<>();
            for (LargeInteger i = zero; i.lessThan(relations.size()); i=i.add(1)){
                if (!dependencyHasBit(dependency, i)) continue;
                QSRelation r = relations.get(i);
                X = (LargeInteger) X.multiply(r.x).mod(N);
                for (LargeInteger j = zero; j.lessThan(factorBaseSize); j=j.add(1))
                    totalExponents.set(j, totalExponents.get(j) + r.exponents.get(j));
            }
            LargeInteger Y = one;
            for (LargeInteger j = zero; j.lessThan(factorBaseSize); j=j.add(1)){
                LargeInteger p = factorBase.get(j).p;
                if (p.equalsTo(negativeOne)) continue;
                long halfExp = totalExponents.get(j) >>> 1;
                for (long k = 0; k < halfExp; k++)
                    Y = (LargeInteger) Y.multiply(p).mod(N);
            }
            LargeInteger g1 = ((LargeInteger) X.subtract(Y)).magnitude().gcd(N);
            if (g1.greaterThan(one) && g1.lessThan(N))
                return g1;
            LargeInteger g2 = ((LargeInteger) X.add(Y)).magnitude().gcd(N);
            if (g2.greaterThan(one) && g2.lessThan(N))
                return g2;
        }
        return one;
    }
    public record PowerPlusOneForm(LargeInteger base, int exponent) {}
    public PowerPlusOneForm detectPowerPlusOne() {
        if (lesserThanOrEqualTo(LargeInteger.two)) return null;
        LargeInteger M = subtract(1);
        // n cannot exceed log2(M), but decimalDigits * 4 is a safe rough upper bound.
        int maxExp = M.toString().length() * 4;
        // Usually check large exponents first so 10^303 is found as 10^303,
        // not as 1000^101.
        for (int n = maxExp; n >= 2; n--) {
            LargeInteger root = M.nthRootFloor(n);
            if (powEqualsTo(root, n, M))
                return new PowerPlusOneForm(root, n);
        }
        return null;
    }
    public record PowerMinusOneForm(LargeInteger base, int exponent) {}
    public PowerMinusOneForm detectPowerMinusOne() {
        if (lesserThanOrEqualTo(LargeInteger.zero)) return null;

        LargeInteger M = add(1);

        int maxExp = M.toString().length() * 4;

        for (int n = maxExp; n >= 2; n--) {
            LargeInteger root = M.nthRootFloor(n);
            if (powEqualsTo(root, n, M))
                return new PowerMinusOneForm(root, n);
        }

        return null;
    }
    private void addFactor(LargeMap<LargeInteger, LargeFraction> out, LargeInteger p) {
        if (out.containsKey(p))
            out.put(p, (LargeFraction) out.get(p).add(LargeFraction.one));
        else
            out.put(p, LargeFraction.one);
    }
    /**
     * Factors a limbs into multiplicative components. Usually this means prime factors. USUALLY.
     * In strict mathematics, 1 has an empty factorization, 0 has no prime factorization, and negatives are handled by a separate sign.
     * However, programming prefers total functions over philosophical purity.
     * To account for this, 1 returns {1 -> 1}, 0 returns {0 -> 1}, and -1 is treated as a sign-factor.
     * Also, if the limbs decides to become a cryptography final boss, the remaining chunk may be treated as one atomic factor. So definitely not full prime factorization as one might expect.
     */
    public LargeMap<LargeInteger, LargeFraction> factor(){
        LargeMap<LargeInteger, LargeFraction> p = new LargeMap<>();
        if(isZeroArray(limbs)){
            p.put(zero, LargeFraction.one);
            return p;
        }else if(this.equalsTo(one)){
            p.put(one, LargeFraction.one);
            return p;
        }else if(this.sign)
            p.put(negativeOne, LargeFraction.one);
        LargeInteger copy = this.magnitude();
        if (copy.toString().length() > 25) {
            // detect numbers of the form x^n - 1
            PowerMinusOneForm minusForm = copy.detectPowerMinusOne();
            if (minusForm != null) {
                LargeInteger n = new LargeInteger(minusForm.exponent);

                LargeList<LargeInteger> divisors = n.divisorsProper();
                divisors.add(n);

                for (LargeInteger d : divisors) {
                    ScalarAtom value = LargePolynomial.cyclotomic(d).plugIn(minusForm.base);
                    value = value.magnitude();

                    if (value instanceof RadicalTermSum)
                        value = ((RadicalTermSum) value).constant.numerator;
                    else if (value instanceof LargeFraction)
                        value = ((LargeFraction) value).numerator;

                    LargeInteger T = (LargeInteger) value.copy();
                    if(T.lesserThanOrEqualTo(one) || T.equalsTo(copy)) continue;

                    LargeMap<LargeInteger, LargeFraction> subFactors = T.factor();
                    for (LargeInteger q : subFactors) {
                        LargeInteger exp = subFactors.get(q).numerator;
                        for (LargeInteger i = zero; i.lessThan(exp); i = i.add(1)) {
                            addFactor(p, q);
                            copy = copy.longDivision(q);
                        }
                    }
                }
            }
            // detect numbers of the form x^n + 1
            PowerPlusOneForm form = copy.detectPowerPlusOne();
            if (form != null && form.exponent % 2 == 1) {
                LargeInteger n = new LargeInteger(form.exponent);
                LargeList<LargeInteger> divisors = n.divisorsProper();
                divisors.add(n);
                LargeInteger minusBase = form.base.negate();
                for (LargeInteger d : divisors) {
                    // Φ_d(-x), equivalent to Φ_{2d}(x) when d is odd.
                    ScalarAtom todo = LargePolynomial.cyclotomic(d).plugIn(minusBase);
                    todo = todo.magnitude();
                    if (todo instanceof RadicalTermSum) todo = ((RadicalTermSum) todo).constant.numerator;
                    LargeInteger T = new LargeInteger(todo.toString());
                    LargeMap<LargeInteger, LargeFraction> subFactors = T.factor();
                    for (LargeInteger q : subFactors) {
                        if (p.containsKey(q))
                            p.put(q, (LargeFraction) subFactors.get(q).add(p.get(q)));
                        else p.put(q, subFactors.get(q));
                        copy = ((LargeFraction) copy.divide(q.power(subFactors.get(q).numerator))).numerator;
                    }
                }
            }
        }
        for(int k: SMALL_PRIMES){
            if(k == 2){
                LargeFraction exponent = LargeFraction.zero;
                while(copy.isEven()){
                    copy = copy.shiftRightBits(1);
                    exponent = (LargeFraction) exponent.add(one);
                }
                if(exponent.notEqualsTo(zero))
                    p.put(two, exponent);
                continue;
            }
            LargeInteger I = new LargeInteger(k);
            LargeInteger[] DR = copy.divideAndRemainder(I);
            LargeFraction exponent = LargeFraction.zero;
            while(isZeroArray(DR[1].limbs)){
                copy = DR[0];
                exponent = (LargeFraction) exponent.add(one);
                DR = copy.divideAndRemainder(I);
            }
            if(!isZeroArray(exponent.numerator.limbs)) p.put(I, exponent);
            if(copy.equalsTo(one)){
                //sort factors
                for(LargeInteger i = zero; i.lessThan(p.size()); i=i.add(1)){
                    for(LargeInteger j = i.add(1); j.lessThan(p.size()); j=j.add(1))
                        if(p.keyAtIndex(i).greaterThan(p.keyAtIndex(j)))
                            p.swap(p.keyAtIndex(i), p.keyAtIndex(j));
                }
                return p;
            }
        }
        ArrayDeque<LargeInteger> work = new ArrayDeque<>();
        work.push(copy);
        while (!work.isEmpty()) {
            copy = work.pop();
            if (copy.equalsTo(one)) continue;
            if (copy.isProbablyPrime()) {
                addFactor(p, copy);
                continue;
            }
            LargeInteger d = copy.pollardPMinusOneQuick();
            if (d.notEqualsTo(one) && d.notEqualsTo(copy)){
                work.push(copy.longDivision(d));
                work.push(d);
                continue;
            }
            d = copy.brentRhos();
            if (d.notEqualsTo(one) && d.notEqualsTo(copy)){
                work.push(copy.longDivision(d));
                work.push(d);
                continue;
            }
            if (copy.shouldTryFermat()) {
                d = copy.fermatFactor();
                if (d.notEqualsTo(one) && d.notEqualsTo(copy)) {
                    work.push(copy.longDivision(d));
                    work.push(d);
                    continue;
                }
            }
            d = copy.ecmMedium();
            if (d.notEqualsTo(one) && d.notEqualsTo(copy)){
                work.push(copy.longDivision(d));
                work.push(d);
                continue;
            }
            d = copy.quadraticSieve();
            if (d.notEqualsTo(one) && d.notEqualsTo(copy)){
                work.push(copy.longDivision(d));
                work.push(d);
                continue;
            }
            d = copy.ecmHard();
            if (d.notEqualsTo(one) && d.notEqualsTo(copy)){
                work.push(copy.longDivision(d));
                work.push(d);
                continue;
            }
            System.out.println("limbs " + copy + " too difficult to factor; treated as composite atomic");
            addFactor(p, copy);
        }
        //sort factors
        for(LargeInteger i = zero; i.lessThan(p.size()); i=i.add(1)){
            for(LargeInteger j = i.add(1); j.lessThan(p.size()); j=j.add(1))
                if(p.keyAtIndex(i).greaterThan(p.keyAtIndex(j)))
                    p.swap(p.keyAtIndex(i), p.keyAtIndex(j));
        }
        return p;
    }
    public LargeList<LargeInteger> divisorsProper() {
        LargeList<LargeInteger> result = new LargeList<>();
        if (isZeroArray(limbs) || this.equalsTo(one))
            return result;
        LargeMap<LargeInteger, LargeFraction> f = this.magnitude().factor();
        result.add(one);
        if(f.size().equalsTo(one) && f.keyAtIndex(zero).equalsTo(one))
            return result;
        for (LargeInteger p : f) {
            if (p.equalsTo(LargeInteger.negativeOne)) continue;
            LargeInteger exp = f.get(p).numerator;
            LargeInteger currentSize = result.size();
            LargeInteger pPower = one;
            for (LargeInteger e = one; e.lesserThanOrEqualTo(exp); e = e.add(1)) {
                pPower = (LargeInteger) pPower.multiply(p);
                for (LargeInteger i = zero; i.lessThan(currentSize); i = i.add(1))
                    result.add((LargeInteger) result.get(i).multiply(pPower));
            }
        }
        result.removeLast();
        return result;
    }
    public boolean isPerfect(){
        if(this.lesserThanOrEqualTo(one)) return false;
        LargeInteger result = zero;
        for(LargeInteger I: divisorsProper())
            result = (LargeInteger) result.add(I);
        return result.equalsTo(this);
    }
    //the inverse of the factor method.
    public static LargeInteger buildFromFactors(LargeMap<LargeInteger, LargeFraction> pr){
        LargeInteger result = one;
        for(LargeInteger p: pr){
            if(pr.get(p).denominator.notEqualsTo(one)) throw new ArithmeticException("Error: Result is not rational");
            if(pr.get(p).lessThan(zero)) throw new ArithmeticException("Error: Result is not an integer");
            LargeInteger expTemp = pr.get(p).numerator.magnitude();
            LargeInteger basTemp = p.copy();
            while(true){
                if(expTemp.isOdd()){
                    result = (LargeInteger) result.multiply(basTemp);
                    if(expTemp.equalsTo(one)) break;
                }
                basTemp = basTemp.square();
                expTemp = expTemp.longDivision(two);
            }
        }
        return result;
    }
    private record PM1Plan(int B1, int B2) {}
    private record PM1Stage1Result(long[] state, long[] factor, boolean saturated) {}
    private static long[] pollardPMinusOneEasy(long[] n){
        if (isEvenMag(n)) return TWO_MAG;
        PM1Plan[] plans = {new PM1Plan(10_000, 100_000)};
        long[] g = pollardPMinusOneAttempt(n, plans);
        if(isNontrivialFactor(g, n)) return g;
        return ONE_MAG;
    }
    private static long[] pollardPMinusOneAttempt(long[] n, PM1Plan[] plans) {
        n = trimLeadingZeros(n);
        // Sieve once through the largest B2.
        int[] primes = sieve(plans[plans.length - 1].B2);
        MontgomeryReducer reducer = new MontgomeryReducer(n);

        long[] state = reducer.toMontgomery(TWO_MAG);
        int previousB1 = 1;

        for (PM1Plan plan : plans) {
            PM1Stage1Result stage1 = pollardPMinusOneStage1(n, state, previousB1, plan.B1, primes, reducer);
            if (isNontrivialFactor(stage1.factor, n))
                return stage1.factor;
            if (stage1.saturated())
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
                long[] g = gcdMagnitude(modSubtract(a, reducer.rModM, n), n);
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
                        long[] replayGcd = gcdMagnitude(modSubtract(replay, reducer.rModM, n), n);
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
            long[] g = gcdMagnitude(modSubtract(a, reducer.rModM, n), n);
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
        int start = firstGreaterThan(primes, B1);

        if (start >= primes.length || primes[start] > B2)
            return ONE_MAG;
        int D = 2310;
        long[][] baby = new long[D][];
        long[] power = reducer.rModM;
        for (int r = 1; r < D; r++) {
            power = reducer.multiply(power, stage1Montgomery);
            if (gcdInt(r, D) == 1)
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

            if (q > B2)
                break;
            int targetM = (q + D - 1) / D;
            while (currentM < targetM) {
                giant = reducer.multiply(giant, xToD);
                currentM++;
            }
            int r = targetM * D - q;
            // q > B1, so q cannot be one of D's small prime factors.
            if (r == 0 || baby[r] == null) continue;
            long[] term = modSubtract(giant, baby[r], n);
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
        long[] g = gcdMagnitude(product, n);
        if (isNontrivialFactor(g, n))
            return g;
        /*
         * Multiple different terms may have supplied different factors,
         * making the product divisible by n. Check the individual
         * terms rather than recomputing exponentiation.
         */
        if (equalsArray(g, n)) {
            for (int i = 0; i < count; i++) {
                g = gcdMagnitude(terms[i], n);
                if (isNontrivialFactor(g, n))
                    return g;
            }
        }
        return ONE_MAG;
    }
    private static long chooseRhoBudget(long bits) {
        if (bits <= 128) return 2_000_000L;
        if (bits <= 192) return   750_000L;
        if (bits <= 320) return   250_000L;
        return 100_000L;
    }
    private static long[] brentRhos(long[] n) {
        if (isEvenMag(n)) return TWO_MAG;
        long bits = bitLength(n);
        long steps = chooseRhoBudget(bits);
        MontgomeryReducer reducer = new MontgomeryReducer(n);
        long[] d = brentRhoAttempt(n, steps, reducer);
        if (isNontrivialFactor(d, n)) return d;
        return ONE_MAG;
    }
    private static long[] brentRhoAttempt(
            long[] n,
            long steps,
            MontgomeryReducer reducer
    ) {
        long[] y = randomMagBetween(TWO_MAG, subtractSmallMag(n, 2), MASTER_RNG.split());
        long[] c = randomMagBetween(ONE_MAG, subtractOneMag(n), MASTER_RNG.split());

        final long m = 1024;
        long r = 1;
        long times = 0;

        long[] g = ONE_MAG;
        long[] x = ZERO_MAG;
        long[] ys = ZERO_MAG;

        while (isOneMag(g) && times < steps) {
            x = y.clone();
            for (long i = 0; i < r && times < steps; i++) {
                y = reducer.reduce(addMagnitudes(squareMagnitude(y), c));
                times++;
            }
            long k = 0;
            while (k < r && isOneMag(g) && times < steps) {
                ys = y;
                long[] q = ONE_MAG;

                long limit = Math.min(m, r - k);

                for (long i = 0; i < limit && times < steps; i++) {
                    y = reducer.reduce(addMagnitudes(squareMagnitude(y), c));

                    long[] diff = SignedMag.positive(x).subtract(SignedMag.positive(y)).mag;
                    if(isZeroArray(diff)) return ONE_MAG;

                    q = reducer.reduce(multiplyMagnitudes(q, diff));
                    times++;
                }

                g = gcdMagnitude(q, n);
                k += m;
            }

            if (r > (Long.MAX_VALUE >>> 1)) return ONE_MAG;
            r <<= 1;
        }

        if (isOneMag(g)) return ONE_MAG;

        if (equalsArray(g, n)) {
            g = ONE_MAG;
            for (long i = 0; i < 10_000 && isOneMag(g); i++) {
                ys = reducer.reduce(addMagnitudes(squareMagnitude(ys), c));

                int cmp = compareArray(x, ys);
                if (cmp == 0) return ONE_MAG;

                long[] diff = cmp > 0
                        ? subtractMagnitudes(x, ys)
                        : subtractMagnitudes(ys, x);

                g = gcdMagnitude(diff, n);
            }
            if(isNontrivialFactor(g, n)) return g;
        }
        return ONE_MAG;
    }
    private static long[] fermatSearch(long[] n) {
        long[] a = sqrtCeilMag(n);
        long[] b2 = subtractMagnitudes(squareMagnitude(a), n);
        if (!isPerfectSquare(b2))
            return ONE_MAG;
        long[] b = sqrtFloorMag(b2);
        long[] factor = subtractMagnitudes(a, b);
        return isNontrivialFactor(factor, n) ? factor : ONE_MAG;
    }
    private static long reducedCheckCount = 0;
    private record MontgomeryPointMag(long[] X, long[] Z){}
    private static final class MontgomeryCurveMag {
        final long[] A24; //(A+2)/4 mod n
        final MontgomeryReducer reducer;
        final long[] n;
        void requireCanonicalPoint(MontgomeryPointMag P) {
            reducedCheckCount+=2;
            reducer.requireReducedMagnitude(P.X, "P.X");
            reducer.requireReducedMagnitude(P.Z, "P.Z");
        }
        MontgomeryCurveMag(MontgomeryReducer reducer, long[] A24Normal) {
            this.reducer = reducer;
            A24 = reducer.toMontgomery(A24Normal);
            n = reducer.modulus;
        }
        MontgomeryPointMag doublePoint(MontgomeryPointMag P) {
            requireCanonicalPoint(P);
            return doublePointCanonical(P);
        }
        private MontgomeryPointMag doublePointCanonical(MontgomeryPointMag P) {
            long[] xpz = reducer.addCanonical(P.X, P.Z);
            long[] xmz = reducer.subtractCanonical(P.X, P.Z);

            long[] xpz2 = reducer.square(xpz);
            long[] xmz2 = reducer.square(xmz);

            long[] diff = reducer.subtractCanonical(xpz2, xmz2);

            long[] X2 = reducer.multiply(xpz2, xmz2);

            long[] inner = reducer.addCanonical(xmz2, reducer.multiply(A24, diff));

            long[] Z2 = reducer.multiply(diff, inner);

            return new MontgomeryPointMag(X2, Z2);
        }
        MontgomeryPointMag diffAdd(MontgomeryPointMag P, MontgomeryPointMag Q, MontgomeryPointMag PMinusQ) {
            long[] a = reducer.add(P.X, P.Z);
            long[] b = reducer.subtract(P.X, P.Z);
            long[] c = reducer.add(Q.X, Q.Z);
            long[] d = reducer.subtract(Q.X, Q.Z);

            long[] da = reducer.multiply(d, a);
            long[] cb = reducer.multiply(c, b);

            long[] plus = reducer.add(da, cb);
            long[] minus = reducer.subtract(da, cb);

            long[] X = reducer.multiply(PMinusQ.Z, reducer.square(plus));
            long[] Z = reducer.multiply(PMinusQ.X, reducer.square(minus));
            // X and Z are already Montgomery residues.
            return new MontgomeryPointMag(X, Z);
        }
        MontgomeryPointMag multiply(MontgomeryPointMag P, long k){return multiply(P, PracChain.buildPracChain(k));}
        MontgomeryPointMag multiply(MontgomeryPointMag P, PracChain chain) {
            MontgomeryPointMag result;

            if (chain.oddPart == 1L)
                result = P;
            else
                result = multiplyPrac(P, chain);

            for (int i = 0;
                 i < chain.trailingDoubling;
                 i++) {
                result = doublePoint(result);
            }

            return result;
        }
        MontgomeryPointMag multiplyPrac(MontgomeryPointMag P, PracChain chain) {
            MontgomeryPointMag A = doublePoint(P);
            MontgomeryPointMag B = P;
            MontgomeryPointMag C = P;

            MontgomeryPointMag T;
            MontgomeryPointMag T2;

            for (byte operation : chain.operations) {
                switch (operation) {
                    case PracChain.SWAP -> {
                        MontgomeryPointMag temporary = A;
                        A = B;
                        B = temporary;
                    }case PracChain.CASE_1 -> {
                        T = diffAdd(A, B, C);
                        T2 = diffAdd(T, A, B);
                        B = diffAdd(B, T, A);
                        A = T2;
                    }case PracChain.CASE_2, PracChain.CASE_4 -> {
                        B = diffAdd(A, B, C);
                        A = doublePoint(A);
                        //C unchanged
                    }case PracChain.CASE_3 -> {
                        T = diffAdd(B, A, C);
                        MontgomeryPointMag oldB = B;
                        B = T;
                        C = oldB;
                    }case PracChain.CASE_5 -> {
                        C = diffAdd(C, A, B);
                        A = doublePoint(A);
                        //B unchanged
                    }case PracChain.CASE_6 -> {
                        T = doublePoint(A);
                        T2 = diffAdd(A, B, C);
                        MontgomeryPointMag oldB = B;
                        A = diffAdd(T, A, A);
                        B = diffAdd(T, T2, C);
                        C = oldB;
                    }case PracChain.CASE_7 -> {
                        T = diffAdd(A, B, C);
                        B = diffAdd(T, A, B);
                        T2 = doublePoint(A);
                        A = diffAdd(T2, A, A);
                    }case PracChain.CASE_8 -> {
                        T = diffAdd(A, B, C); // a+b
                        C = diffAdd(A, C, B);// a+(a-b)=2a-b
                        B = T;
                        T2 = doublePoint(A);
                        A = diffAdd(A, T2, A);
                    }case PracChain.CASE_9 -> {
                        C = diffAdd(C, B, A);
                        B = doublePoint(B);
                        //A is unchanged
                    }
                    default -> throw new IllegalStateException("Unknown PRAC operation");
                }
            }
            MontgomeryPointMag result = diffAdd(A, B, C);
            for (int i = 0; i < chain.trailingDoubling; i++)
                result = doublePoint(result);
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
    private record PracChain(byte[] operations, int trailingDoubling, long oddPart){
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

                    operations.write(PracChain.SWAP);
                }

                // Apply the first qualifying PRAC condition.

                if (d - e <= e / 4L &&
                        (d + e) % 3L == 0L) {

                    operations.write(PracChain.CASE_1);

                    d = (2L * d - e) / 3L;
                    e = (e - d) / 2L;

                } else if (d - e <= e / 4L &&
                        (d - e) % 6L == 0L) {

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
            return new PracChain(operations.toByteArray(), trailingDoubling, k);
        }
    }
    private static int firstGreaterThan(int[] values, int target) {
        int low = 0;
        int high = values.length;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (values[mid] <= target)
                low = mid + 1;
            else high = mid;
        }
        return low;
    }
    private static final class ECMCurveState {
        final MontgomeryCurveMag curve;
        MontgomeryPointMag point;
        long[] stage1Product;
        ECMCurveState(MontgomeryCurveMag curve, MontgomeryPointMag initialPoint) {
            this.curve = curve;
            this.point = initialPoint;
            this.stage1Product = curve.reducer.rModM;
        }
    }

    private static ECMCurveState createCurve(
            long[] n,
            MontgomeryReducer reducer,
            long[] inv4
    ) {
        while (true) {
            long[] sigma = randomMagBetween(SIX_MAG, n, MASTER_RNG.split());
            // Normal residues
            long[] sigmaSquared = modMag(squareMagnitude(sigma), n);

            long[] u = modSubtract(sigmaSquared, FIVE_MAG, n);
            long[] v = modMag(multiplySmallMag(sigma, 4L), n);

            long[] numerator = SignedMag.positive(v).add(SignedMag.negative(u)).cube().multiplyUnsigned(addMagnitudes(v, multiplySmallMag(u, 3L))).modMag(n);
            long[] denominator = modMag(multiplySmallMag(multiplyMagnitudes(cubeMagnitude(u), v), 4L), n);

            ExtGcdMag eg = extendedGcdMag(denominator, n);

            if (isNontrivialFactor(eg.gcd, n))
                throw new FactorFoundException(eg.gcd);

            if (equalsArray(eg.gcd, n))
                // Degenerate Suyama parameter. Try another sigma.
                continue;
            long[] A = SignedMag.positive(numerator).multiply(eg.x).add(SignedMag.negative(TWO_MAG)).modMag(n);
            long[] A24 = modMag(multiplyMagnitudes(addSmallMag(A, 2L), inv4), n);
            MontgomeryCurveMag curve = new MontgomeryCurveMag(reducer, A24);

            // Convert u and v before Montgomery multiplication.
            long[] uMont = reducer.toMontgomery(u);
            long[] vMont = reducer.toMontgomery(v);

            long[] X = reducer.cube(uMont);
            long[] Z = reducer.cube(vMont);
            MontgomeryPointMag point = new MontgomeryPointMag(X, Z);
            return new ECMCurveState(curve, point);
        }
    }
    private static final class FactorFoundException extends RuntimeException {
        long[] factor;
        FactorFoundException(long[] factor) {this.factor = factor;}
    }
    private record Stage1Step(PracChain chain) {}
    private static Stage1Step[] stage1PrimePowers(long B1) {
        if (B1 < 2L)
            return new Stage1Step[0];
        if (B1 > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Use segmented stage 1 for this bound");
        int[] primes = sieve((int) B1);
        Stage1Step[] res = new Stage1Step[primes.length];

        for (int i = 0; i < primes.length; i++) {
            long p = primes[i];
            long power = p;

            while (power <= B1 / p)
                power *= p;
            res[i] = new Stage1Step(PracChain.buildPracChain(power));
        }

        return res;
    }
    private static final class Stage2State {
        private static final int D = 2310;
        private static final int HALF_D = D >>> 1;
        private static final PracChain D_CHAIN = PracChain.buildPracChain(D);

        final MontgomeryCurveMag curve;
        final MontgomeryPointMag Q;
        final MontgomeryPointMag[] baby;
        final MontgomeryPointMag difference;

        MontgomeryPointMag previous;
        MontgomeryPointMag current;
        long currentM;

        // State local to the current sieve block.
        long[] blockProduct;
        int blockTermCount;

        // Snapshot needed only if gcd(blockProduct, n) == n.
        MontgomeryPointMag blockStartPrevious;
        MontgomeryPointMag blockStartCurrent;
        long blockStartM;

        Stage2State(MontgomeryCurveMag curve, MontgomeryPointMag Q, long firstStage2Prime) {
            this.curve = curve;
            this.Q = Q;
            baby = new MontgomeryPointMag[HALF_D + 1];

            baby[1] = Q;

            MontgomeryPointMag previousMultiple = Q;
            MontgomeryPointMag currentMultiple = curve.doublePoint(Q);

            for (int r = 2; r < HALF_D; r++) {
                if (gcdInt(r, D) == 1)
                    baby[r] = currentMultiple;

                MontgomeryPointMag nextMultiple = curve.diffAdd(
                        currentMultiple,   // [r]Q
                        Q,                 // [1]Q
                        previousMultiple   // [r-1]Q
                );
                previousMultiple = currentMultiple;
                currentMultiple = nextMultiple;
            }

            if (gcdInt(HALF_D, D) == 1)
                baby[HALF_D] = currentMultiple;

            difference = curve.multiply(Q, D_CHAIN);

            currentM = nearestCenterIndex(firstStage2Prime);

            if (currentM == 1L) {
                previous = null;
                current = difference;
            } else {
                previous = curve.multiply(Q, Math.multiplyExact(currentM - 1L, D));
                current = curve.multiply(Q, Math.multiplyExact(currentM, D));
            }

            blockProduct = curve.reducer.rModM;
        }
        private static long nearestCenterIndex(long q) {
            long m = q / D;
            long remainder = q % D;

            if (remainder > HALF_D)
                m++;

            return Math.max(1L, m);
        }
        private void processStage2Prime(long q) {
            long[] term = stage2Term(q);
            if(term == null) return;
            blockProduct = curve.reducer.multiply(blockProduct, term);
            blockTermCount++;
        }
        private long[] stage2Term(long q) {
            long targetM = Stage2State.nearestCenterIndex(q);
            while (currentM < targetM) {
                MontgomeryPointMag next;

                if (currentM == 1L) next = curve.doublePoint(difference);
                else next = curve.diffAdd(current, difference, previous);

                previous = current;
                current = next;
                currentM++;
            }

            long center = Math.multiplyExact(currentM, Stage2State.D);
            long distance = q >= center ? q - center : center - q;
            if (distance == 0L || distance > Stage2State.HALF_D) return null;

            int r = Math.toIntExact(distance);
            MontgomeryPointMag B = baby[r];
            long[] left = curve.reducer.multiply(current.X, B.Z);
            long[] right = curve.reducer.multiply(B.X, current.Z);
            return curve.reducer.subtract(left, right);
        }
        private long[] finishStage2Block(long[] blockPrimes) {
            if (blockTermCount == 0) {
                clearStage2Block();
                return ONE_MAG;
            }

            long[] g = gcdMagnitude(blockProduct, curve.n);

            if (isNontrivialFactor(g, curve.n)) {
                clearStage2Block();
                return g;
            }
            if (equalsArray(g, curve.n)) {
                g = replayStage2Block(blockPrimes);
                clearStage2Block();
                return g;
            }

            clearStage2Block();
            return ONE_MAG;
        }
        private void clearStage2Block() {
           blockProduct = curve.reducer.rModM;

            blockTermCount = 0;

            blockStartPrevious = null;
            blockStartCurrent = null;
            blockStartM = 0L;
        }
        private long[] replayStage2Block(long[] blockPrimes) {
            previous = blockStartPrevious;
            current = blockStartCurrent;
            currentM = blockStartM;

            blockProduct = curve.reducer.rModM;
            blockTermCount = 0;

            for (long q : blockPrimes) {
                long[] term = stage2Term(q);

                if (term == null)
                    continue;

                long[] g = gcdMagnitude(term, curve.n);

                if (isNontrivialFactor(g, curve.n))
                    return g;
                if (equalsArray(g, curve.n))
                    return curve.n;
            }
            return ONE_MAG;
        }
    }
    private record ECMPlan(long B1, long B2, int curves){}
    public static long[] ecmHard(long[] n){
        if (isEvenMag(n)) return TWO_MAG;
        ECMPlan[] plans = {
                new ECMPlan(1000, 100000, 64),
                new ECMPlan(2000, 200000, 48),
                new ECMPlan(4000, 400000, 32),
                new ECMPlan(8000, 800000, 24),
                new ECMPlan(16000, 1600000, 16),
                new ECMPlan(32000, 3200000, 12),
        };
        ExtGcdMag inv4Eg = extendedGcdMag(FOUR_MAG, n);
        if (isNontrivialFactor(inv4Eg.gcd, n))
            return inv4Eg.gcd;
        long[] inv4 = inv4Eg.x.modMag(n);
        MontgomeryReducer reducer = new MontgomeryReducer(n);
        for(ECMPlan P: plans){
            long t1 = System.nanoTime();
            Stage1Step[] stage1Powers = stage1PrimePowers(P.B1);
            for(int i = 1; i <= P.curves; i++){
                try{
                    ECMCurveState setup = createCurve(n, reducer, inv4);
                    MontgomeryPointMag Q = setup.point;
                    for (Stage1Step step : stage1Powers)
                        Q = setup.curve.multiply(Q, step.chain);
                    long[] factor = gcdMagnitude(Q.Z, n);
                    if (isNontrivialFactor(factor, n))
                        return factor;
                }catch(FactorFoundException e){return e.factor;}
            }
            long t2 = System.nanoTime();
            System.out.println(reducedCheckCount + " checks for bound " + P.B1);
            System.out.println((t2-t1)/1e9 + " seconds for this bound with " + P.curves + " curves");
            System.out.println();
            reducedCheckCount = 0;
        }
        return ONE_MAG;
    }
    public static long[] ecmExtreme(long[] n){
        if (isEvenMag(n)) return TWO_MAG;
        ECMPlan[] EXTREME_PLANS = {
                new ECMPlan(     11_000L,       1_900_000L,   96),
                new ECMPlan(     50_000L,      13_000_000L,  224),
                new ECMPlan(    250_000L,     130_000_000L,  448),
                new ECMPlan(  1_000_000L,   1_000_000_000L,  960),
                new ECMPlan(  3_000_000L,   5_700_000_000L, 2400),
                new ECMPlan( 11_000_000L,  35_000_000_000L, 4800),
                new ECMPlan( 43_000_000L, 240_000_000_000L, 8000)
        };
        for(ECMPlan P: EXTREME_PLANS){
            long[] g = ecmAttempt(n, P.B1, P.B2, P.curves);
            if(isNontrivialFactor(g, n)) return g;
        }
        return ONE_MAG;
    }
    private static long[] ecmAttempt(
            long[] n,
            long B1,
            long B2,
            int curveCount
    ) {
        if (B1 < 2L)
            throw new IllegalArgumentException("B1 must be >= 2");
        if (B2 <= B1)
            throw new IllegalArgumentException("B2 must exceed B1");
        ExtGcdMag inv4Eg = extendedGcdMag(FOUR_MAG, n);
        if (isNontrivialFactor(inv4Eg.gcd, n))
            return inv4Eg.gcd;
        long[] inv4 = inv4Eg.x.modMag(n);
        MontgomeryReducer reducer = new MontgomeryReducer(n);
        int[] crossingPrimes = ensureSievePrimes(Math.addExact(B2, 1L));
        long[] times = new long[curveCount];
        for (int curveIndex = 0; curveIndex < curveCount; curveIndex++) {
            try{
                ECMCurveState setup = createCurve(n, reducer, inv4);
                long t1 = System.nanoTime();
                long[] factor = runCurve(n, B1, B2, crossingPrimes, setup.curve, setup.point);
                long t2 = System.nanoTime();
                if (factor != null && isNontrivialFactor(factor, n)) return factor;
                times[curveIndex] = t2 - t1;
            }catch(FactorFoundException e){return e.factor;}
        }
        Arrays.sort(times);
        double median;
        if((curveCount & 1L) == 1)
            median = times[curveCount/2]/1e9;
        else median = (times[curveCount/2-1] + times[curveCount/2])/2e9;
        System.out.println("Median time for " + curveCount + " curves with bounds " + B1 + " and " + B2 + ": " + median + " seconds");
        System.out.println("Best time: " + times[0]/1e9 + " seconds");
        System.out.println("Worst time: " + times[curveCount-1]/1e9 + " seconds");
        System.out.println("Modular reduction checks: " + reducedCheckCount);
        reducedCheckCount = 0;
        System.out.println();
        return ONE_MAG;
    }
    private static long nextBlockUpper(long lower, long rangeUpper) {
        long remaining = rangeUpper - lower;
        if (remaining <= 1048576)
            return rangeUpper;
        return lower + 1048576;
    }
    private static long[] runCurve(long[] n, long B1, long B2, int[] crossingPrimes, MontgomeryCurveMag curve, MontgomeryPointMag initialPoint) {
        MontgomeryPointMag Q = initialPoint;
        /*
         * Stage 1
         */
        long lower = 2L;
        long upperExclusive = Math.addExact(B1, 1L);
        while (lower < upperExclusive) {
            long upper = nextBlockUpper(lower, upperExclusive);
            long[] primes = sievePrimeBlock(lower, upper, crossingPrimes);
            for (long p : primes) {
                long primePower = p;
                while(primePower <= B1/p)
                    primePower *= p;
                Q = curve.multiply(Q, PracChain.buildPracChain(primePower));
            }
            lower = upper;
        }
        long[] factor = gcdMagnitude(Q.Z, n);
        if (isNontrivialFactor(factor, n))
            return factor;
        if (equalsArray(factor, n))
            return null;
        /*
         * Stage 2
         */
        Stage2State stage2 = new Stage2State(curve, Q, B1);

        lower = Math.addExact(B1, 1L);
        upperExclusive = Math.addExact(B2, 1L);

        while (lower < upperExclusive) {
            long upper = nextBlockUpper(lower, upperExclusive);
            long[] primes = sievePrimeBlock(lower, upper, crossingPrimes);
            for (long q : primes)
                stage2.processStage2Prime(q);
            factor = stage2.finishStage2Block(n);
            if (isNontrivialFactor(factor, n))
                return factor;
            lower = upper;
        }
        factor = stage2.finishStage2Block(n);
        return isNontrivialFactor(factor, n) ? factor : null;
    }
    private static long timeSieve = 0;
    private static long attemptTime = 0;
    private static long timeMatrix = 0;
    private static long timePoly = 0;
    private static long polynomialCount = 0;
    private static long candidates = 0;
    private static long positionsScanned = 0;
    private static long fullHits = 0;
    private static long partialHits = 0;
    private static long partialMerges = 0;
    private static long timeDependency = 0;
    private record QSPrime(int p, double logP, int root1, int root2) {}
    private static abstract class QSRelation {}

    /**
     * @param index column index: 0 = sign, j+1 = factorBase[j]
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
    private static final class QSRelationFull extends  QSRelation{
        final long[][] xValues;
        final SparseExponents exponents;
        final long[] parity;
        final long[][] squareFactors;
        QSRelationFull(long[][] xValues, SparseExponents exponents, int columnCount, long[][] squareFactors) {
            this.xValues = xValues.clone();
            this.exponents = exponents;
            this.parity = buildParity(exponents, columnCount);
            this.squareFactors = squareFactors.clone();
        }
    }
    private static long[] buildParity(SparseExponents e, int columnCount) {
        long[] parity = new long[(columnCount + 63) >>> 6];
        for (int k = 0; k < e.index.length; k++) {
            if ((e.count[k] & 1) != 0) {
                int col = e.index[k];
                parity[col >>> 6] ^= 1L << (col & 63);
            }
        }
        return parity;
    }
    private static final class QSRelationPartial extends QSRelation {
        final long[][] xValues;
        final SparseExponents exponents;
        final long cofactor;
        QSRelationPartial(long[][] xValues, SparseExponents exponents, long cofactor) {
            this.xValues = xValues;
            this.exponents = exponents;
            this.cofactor = cofactor;
        }
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
    private static ArrayList<long[]> generateBValuesForA(
            long[] A,
            AFactor[] factors
    ) {
        int k = factors.length;

        long[][] terms = new long[k][];

        for (int i = 0; i < k; i++) {
            int p = factors[i].p();
            int root = factors[i].root();

            long[] Ai = divideMagExactSmall(A, p);
            long aiModP = modSmall(Ai, p);
            long inv = inverseModSmall((int) aiModP, p);

            long coefficient = (inv * root) % p;
            terms[i] = multiplySmallMag(Ai, coefficient);
        }

        ArrayList<long[]> Bs = new ArrayList<>();

        int masks = 1 << (k - 1); // avoid B/A-B duplication

        for (int mask = 0; mask < masks; mask++) {
            long[] B = ZERO_MAG;
            for (int i = 0; i < k; i++) {
                boolean plus;
                if (i == 0)
                    plus = true; // fix first sign to avoid duplicate negation
                else plus = ((mask >>> (i - 1)) & 1) == 0;
                if (plus) B = addMagnitudes(B, terms[i]);
                else B = modSubtract(B, terms[i], A); // or B -= term mod A
                B = modMag(B, A);
            }
            if (compareArray(multiplySmallMag(B, 2), A) > 0)
                B = subtractMagnitudes(A, B);

            Bs.add(B);
        }

        return Bs;
    }
    private static int chooseAFactorCountFromFB(QSPrime[] factorBase) {
        int fb = factorBase.length;

        int k = 1;
        int bPolys = 1;
        while (bPolys < fb / bPolys) {
            k++;
            bPolys <<= 1;
        }

        return Math.clamp(k, 4, 8);
    }
    private static int maxFactorBaseIndexAtMost(QSPrime[] factorBase, long[] maxMag) {
        maxMag = trimLeadingZeros(maxMag);

        if (isZeroArray(maxMag))
            return -1;

        int last = factorBase.length - 1;

        if (maxMag.length > 1)
            return last;

        long max = maxMag[0];

        if (Long.compareUnsigned(max, factorBase[last].p) >= 0)
            return last;
        int lo = 1;
        int hi = last;
        int ans = -1;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;

            if (Long.compareUnsigned(factorBase[mid].p, max) <= 0) {
                ans = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }

        return ans;
    }
    private record ACandidateResult(
            ACoefficient coefficient,
            String failure
    ) {
        static ACandidateResult ok(ACoefficient c) {
            return new ACandidateResult(c, null);
        }

        static ACandidateResult fail(String reason) {
            return new ACandidateResult(null, reason);
        }

        boolean ok() {
            return coefficient != null;
        }
    }
    private record ACoefficient(long[] A, AFactor[] primes, long[] aExponents){}
    private static boolean halfNearTarget(long half, long targetHalf) {
        return Long.compareUnsigned(half, targetHalf / 4L) >= 0 && Long.compareUnsigned(half, targetHalf * 4L) <= 0;
    }
    private static ACoefficient chooseAByScoring(
            long[] sqrt2N,
            QSPrime[] factorBase,
            int aFactorCount,
            SplittableRandom rng
    ) {
        ACoefficient best = null;
        double bestYield = -1.0;

        ACoefficient fallback = null;
        long fallbackHalf = 0L;

        int pMax = factorBase[factorBase.length - 1].p;
        int attempts = 4096;

        int genFail = 0;
        int zeroHalf = 0;
        int hugeHalf = 0;
        int tinyPositions = 0;
        int badYield = 0;
        int accepted = 0;

        String firstGenFailure = null;

        for (int trial = 0; trial < attempts; trial++) {
            ACandidateResult result = randomSquarefreeAResult(sqrt2N, factorBase, aFactorCount, rng);
            if (!result.ok()) {
                genFail++;

                if (firstGenFailure == null)
                    firstGenFailure = result.failure();

                continue;
            }

            ACoefficient candidate = result.coefficient;

            long targetHalf = targetHalfForA(factorBase, aFactorCount);

            long[] halfMag = divideMagnitudes(sqrt2N, candidate.A)[0];
            if (isZeroArray(halfMag) || halfMag.length > 1 || Long.compareUnsigned(halfMag[0], Integer.MAX_VALUE) > 0)
                continue;
            long half = halfMag[0];
            if (!halfNearTarget(half, targetHalf))
                continue;

            // Keep a fallback before applying soft quality filters.
            if (fallback == null || Long.compareUnsigned(half, fallbackHalf) > 0) {
                fallback = candidate;
                fallbackHalf = half;
            }

//            if (!hasEnoughPositionsPerA(half, aFactorCount, factorBase.length)) {
//                tinyPositions++;
//                continue;
//            }

            double yield = estimatedRelationsPerPolynomial(
                    candidate.A,
                    halfMag,
                    pMax
            );

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

        if (best != null)
            return best;

        /*
         * Important:
         * If every candidate failed only because of the soft positions/yield filter,
         * still return a fallback so you can print stats and learn what happened.
         */
        if (fallback != null) {
            System.out.println(
                    "SIQS A warning: using fallback A because strict scoring rejected all candidates. " +
                            "aFactorCount=" + aFactorCount +
                            " attempts=" + attempts +
                            " genFail=" + genFail +
                            " zeroHalf=" + zeroHalf +
                            " hugeHalf=" + hugeHalf +
                            " tinyPositions=" + tinyPositions +
                            " badYield=" + badYield +
                            " accepted=" + accepted +
                            " fallbackHalf=" + Long.toUnsignedString(fallbackHalf)
            );

            return fallback;
        }

        throw new IllegalStateException(
                "Could not find usable SIQS A: " +
                        "aFactorCount=" + aFactorCount +
                        " attempts=" + attempts +
                        " genFail=" + genFail +
                        " zeroHalf=" + zeroHalf +
                        " hugeHalf=" + hugeHalf +
                        " tinyPositions=" + tinyPositions +
                        " badYield=" + badYield +
                        " accepted=" + accepted +
                        " firstGenFailure=[" + firstGenFailure + "]"
        );
    }
    private static long targetHalfForA(
            QSPrime[] factorBase,
            int aFactorCount
    ) {
        long fb = factorBase.length;

        long bCount = 1L << (aFactorCount - 1);

        /*
         * Total positions per A, across all B values.
         * Tune this multiplier empirically.
         */
        long positionsMultiplier = 16L;

        long targetPositionsPerA = positionsMultiplier * fb;

        long half = targetPositionsPerA /(2L*bCount);

        if (half < 1L)
            half = 1L;

        return half;
    }
    private static ACandidateResult randomSquarefreeAResult(
            long[] sqrt2N,
            QSPrime[] factorBase,
            int aFactorCount,
            SplittableRandom rng
    ) {
        boolean[] used = new boolean[factorBase.length];
        ArrayList<AFactor> chosen = new ArrayList<>();

        long[] A = ONE_MAG;

        long targetHalf = targetHalfForA(factorBase, aFactorCount);
        double logTargetA = logMag(sqrt2N) - Math.log((double) targetHalf);

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

            int idx = chooseUnusedPrimeNearLogBounded(
                    factorBase,
                    used,
                    desiredLogP,
                    maxIdx,
                    rng
            );

            if (idx < 0)
                return ACandidateResult.fail("noUnusedPrime");

            QSPrime q = factorBase[idx];

            long[] newA = multiplySmallMag(A, q.p);

            if (compareArray(newA, sqrt2N) > 0)
                return ACandidateResult.fail("A exceeded sqrt2N");

            used[idx] = true;

            int root = rng.nextBoolean() ? q.root1 : q.root2;
            chosen.add(new AFactor(idx, q.p, root));

            A = newA;
            logA += Math.log(q.p);
        }

        AFactor[] factors = chosen.toArray(new AFactor[0]);

        long[] aExponents = new long[factorBase.length];

        for (AFactor f : factors) {
            if (aExponents[f.fbIndex()] != 0)
                return ACandidateResult.fail("notSquarefree");

            aExponents[f.fbIndex()] = 1;
        }
        return ACandidateResult.ok(new ACoefficient(A, factors, aExponents));
    }
    private static int chooseUnusedPrimeNearLogBounded(QSPrime[] factorBase, boolean[] used, double desiredLogP, int maxIdx, SplittableRandom rng) {
        if (maxIdx < 1)
            return -1;

        int center = getCenter(factorBase, desiredLogP, maxIdx);

        int radius = Math.max(8, maxIdx / 64);

        int leftBound = Math.max(1, center - radius);
        int rightBound = Math.min(maxIdx, center + radius);

        for (int tries = 0; tries < 128; tries++) {
            int idx = rng.nextInt(leftBound, rightBound + 1);
            if (!used[idx] && factorBase[idx].p > 2)
                return idx;
        }
        for (int d = 0; center - d >= 1 || center + d <= maxIdx; d++) {
            int a = center - d;
            int b = center + d;

            if (a >= 1 && !used[a] && factorBase[a].p > 2)
                return a;

            if (b <= maxIdx && !used[b] && factorBase[b].p > 2)
                return b;
        }
        return -1;
    }

    private static int getCenter(QSPrime[] factorBase, double desiredLogP, int maxIdx) {
        double desiredPDouble = Math.exp(desiredLogP);

        if (!Double.isFinite(desiredPDouble))
            desiredPDouble = factorBase[maxIdx].p;

        int desiredP = (int) Math.clamp(desiredPDouble, 3.0, factorBase[maxIdx].p);

        int lo = 1;
        int hi = maxIdx;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;

            if (factorBase[mid].p < desiredP)
                lo = mid + 1;
            else
                hi = mid - 1;
        }

        return Math.clamp(lo, 1, maxIdx);
    }
    private static double roughDickman(double u) {
        if (u < 0.0)
            return 0.0;
        if (u <= 1.0)
            return 1.0;

        if (u <= 2.0)
            return 1.0 - Math.log(u);

        // Crude but useful ranking approximation.
        return Math.exp(-u * (Math.log(u) + Math.log(Math.log(u)) - 1.0));
    }
    private static double estimatedRelationsPerPolynomial(
            long[] A,
            long[] halfMag,
            int pMax
    ) {
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
    private static void verifyACoefficient(ACoefficient AR, QSPrime[] factorBase) {
        long[] rebuiltA = ONE_MAG;
        long[] rebuiltExp = new long[factorBase.length];

        for (AFactor f : AR.primes) {
            if (rebuiltExp[f.fbIndex()] != 0)
                throw new AssertionError("Repeated A prime");
            if (factorBase[f.fbIndex()].p != f.p())
                throw new AssertionError("AFactor fbIndex does not match p");
            rebuiltExp[f.fbIndex()] = 1;
            rebuiltA = multiplySmallMag(rebuiltA, f.p());
        }

        if (!equalsArray(rebuiltA, AR.A))
            throw new AssertionError("A != product of A factors");

        for (int i = 0; i < rebuiltExp.length; i++) {
            if (rebuiltExp[i] != AR.aExponents[i])
                throw new AssertionError("Bad A exponent vector at index " + i);
        }
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

        final RelationYieldEstimate estimate;

        long polys;
        long scanned;
        long candidates;

        long full;
        long partialStored;
        long partialMerged;
        long none;

        long sieveNs;
        long attemptNs;
        long polyNs;

        AStats(
                ACoefficient coefficient,
                long[] halfMag,
                QSPrime[] factorBase,
                int aFactorCount,
                long largePrimeBound,
                RelationYieldEstimate estimate
        ) {
            this.aFactorCount = aFactorCount;
            this.bCount = 1L << (aFactorCount - 1);
            this.half = halfMag[0];
            this.interval = 2L * half;
            this.positionsPerA = interval * bCount;

            this.fbSize = factorBase.length;
            this.pMax = factorBase[factorBase.length - 1].p;
            this.largePrimeBound = largePrimeBound;

            this.logA = logMag(coefficient.A);
            this.logQEdge = logA + 2.0 * Math.log((double) half);

            this.uB = logQEdge / Math.log(pMax);
            this.uLP = logQEdge / Math.log((double) largePrimeBound);

            this.estimate = estimate;
        }

        void startPolynomial() {
            polys++;
            scanned += interval;
        }
        void observeRelationAdd(RelationAddKind kind) {
            candidates++;
            switch (kind) {
                case NONE -> none++;
                case FULL -> full++;
                case PARTIAL_STORED -> partialStored++;
                case PARTIAL_MERGED -> partialMerged++;
            }
        }

        long partialHits() {
            return partialStored + partialMerged;
        }

        long usefulRows() {
            return full + partialMerged;
        }

        double candidatesPerPoly() {
            return candidates / (double) Math.max(1L, polys);
        }

        double fullPerPoly() {
            return full / (double) Math.max(1L, polys);
        }

        double partialHitsPerPoly() {
            return partialHits() / (double) Math.max(1L, polys);
        }

        double usefulPerPoly() {
            return usefulRows() / (double) Math.max(1L, polys);
        }

        double candidateRate() {
            return candidates / (double) Math.max(1L, scanned);
        }

        double candidateSuccessRate() {
            return (full + partialHits()) / (double) Math.max(1L, candidates);
        }

        double partialMergeRate() {
            return partialMerged / (double) Math.max(1L, partialHits());
        }

        double positionsPerFb() {
            return positionsPerA / (double) fbSize;
        }

        double fullPredictionRatio() {
            return fullPerPoly() / Math.max(1e-12, estimate.fullPerPoly());
        }
        double usefulPredictionRatio() {
            return usefulPerPoly() / Math.max(1e-12, estimate.usefulRowsPerPoly());
        }
    }
    private static AStats printAPlanAndMakeStats(
            ACoefficient coefficient,
            long[] sqrt2N,
            QSPrime[] factorBase,
            int aFactorCount,
            long largePrimeBound,
            int requiredRelations
    ) {
        long[] halfMag = divideMagnitudes(sqrt2N, coefficient.A)[0];

        if (isZeroArray(halfMag) || halfMag.length > 1)
            throw new AssertionError("Bad A half=" + new LargeInteger(halfMag, false));

        int pMax = factorBase[factorBase.length - 1].p;

        RelationYieldEstimate estimate = estimateRelationYieldPerPolynomial(
                coefficient.A,
                halfMag,
                pMax,
                largePrimeBound
        );

        AStats stats = new AStats(
                coefficient,
                halfMag,
                factorBase,
                aFactorCount,
                largePrimeBound,
                estimate
        );

        System.out.printf(
                "A plan: fb=%d pMax=%d factors=%d B=%d half=%d interval=%d " +
                        "positions/A=%d positions/A/fb=%.2f required≈%d " +
                        "uB=%.2f uLP=%.2f estFull/poly=%.4g estPartialHits/poly=%.4g " +
                        "estPartialMerges/poly=%.4g estUseful/poly=%.4g%n",
                stats.fbSize,
                stats.pMax,
                stats.aFactorCount,
                stats.bCount,
                stats.half,
                stats.interval,
                stats.positionsPerA,
                stats.positionsPerFb(),
                requiredRelations,
                stats.uB,
                stats.uLP,
                estimate.fullPerPoly(),
                estimate.partialHitsPerPoly(),
                estimate.partialMergesPerPoly(),
                estimate.usefulRowsPerPoly()
        );

        return stats;
    }
    private static void printAStats(AStats s, int requiredRelations) {
        long useful = s.usefulRows();

        double timeSec = (s.sieveNs + s.attemptNs + s.polyNs) / 1e9;
        double aBlocksNeeded = requiredRelations / Math.max(1.0, (double) useful);
        double projectedSeconds = aBlocksNeeded * timeSec;

        System.out.printf(
                "A actual: polys=%d scanned=%d candidates=%d full=%d partialStored=%d partialMerged=%d none=%d " +
                        "cand/poly=%.3f full/poly=%.3f partialHits/poly=%.3f useful/poly=%.3f " +
                        "useful/A=%d required=%d ABlocks≈%.1f projected=%.1fs %.2fh " +
                        "candidateRate=%.8f candidateSuccess=%.5f partialMergeRate=%.5f " +
                        "fullPredRatio=%.4f usefulPredRatio=%.4f " +
                        "time[sieve=%.3f attempt=%.3f poly=%.3f total=%.3f]%n",
                s.polys,
                s.scanned,
                s.candidates,
                s.full,
                s.partialStored,
                s.partialMerged,
                s.none,
                s.candidatesPerPoly(),
                s.fullPerPoly(),
                s.partialHitsPerPoly(),
                s.usefulPerPoly(),
                useful,
                requiredRelations,
                aBlocksNeeded,
                projectedSeconds,
                projectedSeconds / 3600.0,
                s.candidateRate(),
                s.candidateSuccessRate(),
                s.partialMergeRate(),
                s.fullPredictionRatio(),
                s.usefulPredictionRatio(),
                s.sieveNs / 1e9,
                s.attemptNs / 1e9,
                s.polyNs / 1e9,
                timeSec
        );
    }
    private static final class MPQSPolynomial {
        final long[] A;
        final long[] B;
        final SignedMag C;
        final long[] twoB;
        final AFactor[] aPrimes;
        final long[] aExponents;
        MPQSPolynomial(
                long[] A,
                long[] B,
                SignedMag C,
                AFactor[] aPrimes, long[] aExponents) {
            this.A = A;
            this.B = B;
            this.C = C;
            this.twoB = multiplySmallMag(B, 2);
            this.aPrimes = aPrimes;
            this.aExponents = aExponents;
        }
        SignedMag evaluateAt(long t){
            // Q(t) = A*t^2 + 2B*t + C
            SignedMag result = SignedMag.positive(A);
            result = result.multiplySignedLong(t).add(SignedMag.positive(twoB));
            result = result.multiplySignedLong(t).add(C);
            return result;
        }
    }
    private static void verifyPolynomial(
            MPQSPolynomial poly,
            QSPrime[] factorBase,
            long[] N
    ) {
        long[] productA = ONE_MAG;
        boolean[] seen = new boolean[factorBase.length];

        for (AFactor f : poly.aPrimes) {
            int idx = f.fbIndex();
            int p = f.p();

            if (idx < 0 || idx >= factorBase.length)
                throw new AssertionError("A factor index out of range");

            if (seen[idx])
                throw new AssertionError("A is not squarefree");

            seen[idx] = true;

            if (factorBase[idx].p != p)
                throw new AssertionError("A factor index/prime mismatch");

            if (p <= 2)
                throw new AssertionError("This verifier assumes odd A primes");

            if (legendreSymbolPrime(N, p) != 1)
                throw new AssertionError("N is not a quadratic residue mod A prime " + p);

            long bMod = modSmall(poly.B, p);
            long nMod = modSmall(N, p);

            if ((bMod * bMod) % p != nMod)
                throw new AssertionError("B^2 != N mod A prime " + p);

            if (poly.aExponents[idx] != 1)
                throw new AssertionError("A exponent missing for prime " + p);

            productA = multiplySmallMag(productA, p);
        }

        if (!equalsArray(productA, poly.A))
            throw new AssertionError("A does not equal product of selected primes");

        for (int i = 0; i < poly.aExponents.length; i++) {
            long e = poly.aExponents[i];

            if (seen[i]) {
                if (e != 1)
                    throw new AssertionError("Bad A exponent at index " + i);
            } else {
                if (e != 0)
                    throw new AssertionError("Unexpected A exponent at index " + i);
            }
        }

        if (!equalsArray(modMag(squareMagnitude(poly.B), poly.A), modMag(N, poly.A)))
            throw new AssertionError("B^2 != N mod A");

        SignedMag numeratorC = SignedMag.positive(squareMagnitude(poly.B))
                .add(SignedMag.negative(N));

        long[][] div = divideMagnitudes(numeratorC.mag, poly.A);
        if (!isZeroArray(div[1]))
            throw new AssertionError("(B^2 - N) is not divisible by A");

        SignedMag expectedC = new SignedMag(div[0], numeratorC.negative);

        if (!equalsArray(expectedC.mag, poly.C.mag) || expectedC.negative != poly.C.negative)
            throw new AssertionError("Invalid C coefficient");
    }
    private record QSMatrixRow(long[] parity, long[] dependency) {}
    private static QSPrime[] buildFactorBase(long[] n, int bound) {
        ArrayList<QSPrime> fb = new ArrayList<>();
        // p = 2 special case
        fb.add(new QSPrime(2, Math.log(2), 0, 0));
        for (int p : sieve(bound)) {
            if (p == 2) continue;
            long nModP = modSmall(n, p);
            if (nModP == 0)
                // p directly divides N, so QS does not need to continue.
                // This should be handled before buildFactorBase or returned specially.
                continue;

            if (legendreSymbol(nModP, p) != 1)
                continue;
            int r = (int) tonelliShanks(nModP, p);
            int r2 = p - r;
            fb.add(new QSPrime(p, Math.log(p), r, r2));
        }
        return fb.toArray(new QSPrime[0]);
    }
    private static int[] buildFactorBaseIndexMap(QSPrime[] factorBase) {
        int max = factorBase[factorBase.length - 1].p;
        int[] index = new int[max + 1];

        Arrays.fill(index, -1);

        for (int i = 0; i < factorBase.length; i++)
            index[factorBase[i].p] = i;

        return index;
    }
    private static int factorBaseIndexOf(int[] fbIndex, long L) {
        if (Long.compareUnsigned(L, fbIndex.length) >= 0)
            return -1;

        return fbIndex[(int) L];
    }
    private static int legendreSymbolPrime(long[] a, int p) {
        if (p <= 2)
            throw new IllegalArgumentException("Legendre symbol requires an odd prime modulus");

        long am = modSmall(a, p);

        if (am == 0)
            return 0;

        long r = powModSmall(am, (p - 1L) >>> 1, p);

        if (r == 1)
            return 1;

        if (r == p - 1L)
            return -1;

        throw new IllegalStateException("Invalid Legendre result");
    }
    private static long powModSmall(long base, long exp, int mod) {
        long result = 1L;
        long b = base % mod;

        while (exp != 0) {
            if ((exp & 1L) != 0)
                result = (result * b) % mod;

            b = (b * b) % mod;
            exp >>>= 1;
        }

        return result;
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
        if (isEvenMag(b))
            throw new IllegalArgumentException("Legendre symbol requires an odd prime modulus");
        long[] exponent = shiftRightBits(subtractOneMag(b), 1);
        long[] m = new MontgomeryReducer(b).powMod(a, exponent);
        if (isZeroArray(m))
            return 0;

        if (equalsArray(m, ONE_MAG))
            return 1;

        if (equalsArray(m, subtractOneMag(b)))
            return -1;
        throw new IllegalStateException("Invalid Legendre result");
    }
    private static long tonelliShanks(long n, long p) {
        if (p == 2) return n & 1;
        // Fast path: p ≡ 3 mod 4
        if ((p & 3) == 3)
            return powMod(n, (p + 1) >>> 2, p);
        // Full Tonelli-Shanks
        long q = p - 1;
        int s = 0;
        while ((q & 1) == 0) {
            s++;
            q >>>= 1;
        }
        int z = 2;
        while (legendreSymbol(z, p) != -1)
            z++;
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
        return x;
    }
    private static void sieveBlock(long[] N, QSPrime[] factorBase, int[] fbIndex, ArrayList<QSRelationFull> relations, HashMap<Long, QSRelationPartial> partials, int requiredRelations) {
        int columnCount = factorBase.length + 1; // sign column + real factor-base primes
        int globalPMax = factorBase[factorBase.length - 1].p;
        long largePrimeBound = 30L * globalPMax;
        double logLP = Math.log(largePrimeBound);

        long[] sqrt2N = sqrtCeilMag(multiplySmallMag(N, 2));
        ACoefficient AR = chooseAByScoring(sqrt2N, factorBase, chooseAFactorCountFromFB(factorBase), MASTER_RNG.split());
        verifyACoefficient(AR, factorBase);
        long half = divideMagnitudes(sqrt2N, AR.A)[0][0];
        long startT = -half;
        int interval = Math.toIntExact(2*half);
        AStats plan = printAPlanAndMakeStats(AR, sqrt2N, factorBase, AR.primes.length, largePrimeBound, requiredRelations);
        // Precompute values depending only on A.
        int[] invAModP = new int[factorBase.length];
        for (int j = 0; j < factorBase.length; j++) {
            int p = factorBase[j].p;
            if (p <= 2) {
                invAModP[j] = 0;
                continue;
            }
            long aMod = modSmall(AR.A, p);
            if (aMod == 0) {
                invAModP[j] = 0;
                continue;
            }
            invAModP[j] = Math.toIntExact(inverseModSmall(aMod, p));
        }
        double[] score = new double[interval];
        ArrayList<long[]> BValues = generateBValuesForA(AR.A, AR.primes);
        for (long[] B : BValues) {
            long t0 = System.nanoTime();
            SignedMag numeratorC = SignedMag.positive(squareMagnitude(B)).add(SignedMag.negative(N));
            SignedMag C = new SignedMag(divideExactMag(numeratorC.mag, AR.A), numeratorC.negative);
            MPQSPolynomial poly = new MPQSPolynomial(AR.A, B, C, AR.primes, AR.aExponents);
            verifyPolynomial(poly, factorBase, N);
            timePoly += System.nanoTime() - t0;
            plan.polyNs += System.nanoTime() - t0;
            polynomialCount++;
            plan.startPolynomial();

            //log sieve
            t0 = System.nanoTime();
            Arrays.fill(score, 0);
            for (int i = 0; i < factorBase.length; i++) {
                int p = factorBase[i].p;

                long bMod = modSmall(poly.B, p);
                long invA = invAModP[i];

                long d1 = Math.floorMod((long) factorBase[i].root1 - bMod, p);
                long d2 = Math.floorMod((long) factorBase[i].root2 - bMod, p);

                int r1 = (int) ((d1 * invA) % p);
                int r2 = (int) ((d2 * invA) % p);

                double logP = factorBase[i].logP;
                markRoot(score, r1, p, startT, logP);
                if (r2 != r1)
                    markRoot(score, r2, p, startT, logP);
            }
            timeSieve += System.nanoTime() - t0;
            plan.sieveNs += System.nanoTime() - t0;
            t0 = System.nanoTime();
            double logQThreshold = plan.logQEdge;
            for (int idx = 0; idx < interval; idx++) {
                positionsScanned++;
                if (score[idx] + logLP < logQThreshold)
                    continue;
                long t = startT + idx;
                SignedMag currentQ = poly.evaluateAt(t);
                double exactLogQ = logMag(currentQ.mag);

                if (score[idx] + logLP < exactLogQ)
                    continue;
                candidates++;
                plan.candidates++;
                long[] currentX = computeMPQSX(poly, t, N);
                QSRelation relation = tryBuildQSRelation(
                        currentQ,
                        currentX,
                        poly,
                        factorBase,
                        fbIndex,
                        globalPMax,
                        largePrimeBound,
                        columnCount
                );
                RelationAddKind kind = addQSRelation(
                        relation,
                        relations,
                        partials,
                        columnCount
                );
                plan.observeRelationAdd(kind);
            }
            attemptTime += System.nanoTime() - t0;
            plan.attemptNs += System.nanoTime() - t0;
        }
        printAStats(plan, requiredRelations);
    }
    private record RelationYieldEstimate(
            double fullPerPoly,
            double partialHitsPerPoly,
            double partialMergesPerPoly,
            double usefulRowsPerPoly
    ) {}
    private static RelationYieldEstimate estimateRelationYieldPerPolynomial(
            long[] A,
            long[] halfMag,
            int factorBasePMax,
            long largePrimeBound
    ) {
        if (isZeroArray(halfMag) || halfMag.length > 1)
            return new RelationYieldEstimate(0.0, 0.0, 0.0, 0.0);

        long half = halfMag[0];

        if (half == 0)
            return new RelationYieldEstimate(0.0, 0.0, 0.0, 0.0);

        double interval = 2.0 * (double) half;

        double logA = logMag(A);
        double logM = Math.log((double) half);

        double logQ = logA + 2.0 * logM;

        double logB = Math.log(factorBasePMax);
        double logLP = Math.log((double) largePrimeBound);

        double fullProb = roughDickman(logQ / logB);
        double lpSmoothProb = roughDickman(logQ / logLP);

        double rawFulls = interval * fullProb;
        double rawLpSmooth = interval * lpSmoothProb;

        double fullCalibration = 0.06;
        double partialCalibration = 0.02;

        double fulls = fullCalibration * rawFulls;

        /*
         * Do not count LP-smooth - B-smooth directly as usable partials.
         * That overestimates heavily.
         */
        double partialHits = partialCalibration * Math.max(0.0, rawLpSmooth - rawFulls);

        double mergeFraction = estimatePartialMergeFraction(
                partialHits,
                factorBasePMax,
                largePrimeBound
        );

        double partialMerges = partialHits * mergeFraction;

        return new RelationYieldEstimate(
                fulls,
                partialHits,
                partialMerges,
                fulls + partialMerges
        );
    }
    private static double estimatePartialMergeFraction(double partialHits, int factorBasePMax, long largePrimeBound) {
        if (partialHits <= 1.0)
            return 0.0;
        double bucketCount = approximatePrimeCount(largePrimeBound) - approximatePrimeCount(factorBasePMax);
        if (bucketCount <= 1.0)
            return 0.0;
        /*
         * If partial cofactors were uniformly distributed, expected pairs are tiny
         * until partialHits becomes comparable to sqrt(bucketCount).
         *
         * Real QS partial cofactors are not uniform, so actual merges are usually
         * better than this. Still useful as a conservative warning.
         */
        double expectedPairs = partialHits * partialHits / (2.0 * bucketCount);

        return Math.min(0.5, expectedPairs / partialHits);
    }
    private static double approximatePrimeCount(double x) {
        if (x < 17.0)
            return Math.max(0.0, x / 2.0);
        return x / Math.log(x);
    }
    private static void markRoot(
            double[] score,
            int root,
            int p,
            long startT,
            double logP
    ) {
        int start = Math.toIntExact(Math.floorMod(root - Math.floorMod(startT, p), p));
        for (int idx = start; idx < score.length; idx += p)
            score[idx] += logP;
    }
    private static double logMag(long[] mag) {
        mag = trimLeadingZeros(mag);
        if (isZeroArray(mag))
            return Double.NEGATIVE_INFINITY;
        int top = mag.length - 1;
        long hi = mag[top];
        int bits = 64 - Long.numberOfLeadingZeros(hi);
        int bitLength = top * 64 + bits;
        // Good enough for sieve thresholding.
        return bitLength * Math.log(2.0);
    }
    private static long[] computeMPQSX(MPQSPolynomial poly, long t, long[] N) {
        SignedMag at = SignedMag.signedMultiplySmall(poly.A, t);
        SignedMag xSigned = at.add(SignedMag.positive(poly.B));
        return xSigned.modMag(N);
    }
    private static void incExp(short[] dense, int column) {
        if (dense[column] == Short.MAX_VALUE)
            throw new ArithmeticException("Exponent overflow at column " + column);
        dense[column]++;
    }
    private static QSRelation tryBuildQSRelation(
            SignedMag qSigned,
            long[] X,
            MPQSPolynomial poly,
            QSPrime[] factorBase,
            int[] fbIndex,
            long globalPMax,
            long largePrimeBound,
            int columnCount
    ) {
        if (isZeroArray(qSigned.mag))
            return null;

        short[] dense = new short[columnCount];

        // A contribution: column 0 is sign, factorBase[j] lives at column j + 1.
        for (int j = 0; j < poly.aExponents.length; j++) {
            long e = poly.aExponents[j];

            if (e == 0)
                continue;

            if (e > Short.MAX_VALUE)
                throw new ArithmeticException("A exponent too large");

            dense[j + 1] = (short) e;
        }

        SignedMag remainingSigned = qSigned;

        if (remainingSigned.negative) {
            remainingSigned = remainingSigned.negate();
            incExp(dense, 0);
        }

        long[] remaining = remainingSigned.mag;

        for (int j = 0; j < factorBase.length; j++) {
            int p = factorBase[j].p;

            if (remaining.length == 1) {
                long L = remaining[0];

                if (L == 1L)
                    break;

                long pp = (long) p * (long) p;

                if (Long.compareUnsigned(pp, L) > 0) {
                    int idx = factorBaseIndexOf(fbIndex, L);

                    if (idx >= 0) {
                        incExp(dense, idx + 1);
                        remaining = ONE_MAG;
                        break;
                    }

                    if (Long.compareUnsigned(L, globalPMax) > 0 && Long.compareUnsigned(L, largePrimeBound) <= 0) {
                        SparseExponents sparse = compress(dense);
                        return new QSRelationPartial(new long[][]{X}, sparse, L);
                    }

                    return null;
                }
            }
            DivRem DR = divideLong(remaining, p);
            while (DR.remainder == 0) {
                remaining = DR.quotient;
                incExp(dense, j + 1);
                if (isOneMag(remaining))
                    break;
                DR = divideLong(remaining, p);
            }
            if (isOneMag(remaining))
                break;
        }

        if (isOneMag(remaining)) {
            SparseExponents sparse = compress(dense);
            return new QSRelationFull(new long[][]{X}, sparse, columnCount, new long[0][]);
        }

        if (remaining.length == 1) {
            long L = remaining[0];

            if (Long.compareUnsigned(L, globalPMax) > 0 &&
                    Long.compareUnsigned(L, largePrimeBound) <= 0 &&
                    isProbablyPrimeMag(remaining)) {
                SparseExponents sparse = compress(dense);
                return new QSRelationPartial(new long[][]{X}, sparse, L);
            }
        }

        return null;
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
    private enum RelationAddKind {
        NONE,
        FULL,
        PARTIAL_STORED,
        PARTIAL_MERGED
    }
    private static RelationAddKind addQSRelation(
            QSRelation relation,
            ArrayList<QSRelationFull> relations,
            HashMap<Long, QSRelationPartial> partials,
            int columnCount
    ) {
        if (relation == null)
            return RelationAddKind.NONE;

        if (relation instanceof QSRelationFull full) {
            fullHits++;
            relations.add(full);
            return RelationAddKind.FULL;
        }

        QSRelationPartial partial = (QSRelationPartial) relation;
        long L = partial.cofactor;

        QSRelationPartial old = partials.remove(L);

        if (old == null) {
            partials.put(L, partial);
            partialHits++;
            return RelationAddKind.PARTIAL_STORED;
        }

        SparseExponents mergedExponents = addSparse(old.exponents, partial.exponents);

        QSRelationFull merged = new QSRelationFull(
                concat(old.xValues, partial.xValues),
                mergedExponents,
                columnCount,
                new long[][]{new long[]{L}}
        );

        relations.add(merged);
        partialMerges++;
        return RelationAddKind.PARTIAL_MERGED;
    }
    private static QSMatrixRow[] buildQSMatrix(ArrayList<QSRelationFull> relations) {
        int relationCount = relations.size();
        int dependencyWords = (relationCount + 63) >>> 6;
        QSMatrixRow[] rows = new QSMatrixRow[relationCount];
        for (int i = 0; i < relationCount; i++) {
            long[] parity = relations.get(i).parity;
            long[] dependency = new long[dependencyWords];
            dependency[i >>> 6] |= 1L << (i & 63);
            rows[i] = new QSMatrixRow(parity, dependency);
        }
        return rows;
    }
    private static ArrayList<long[]> nullspaceDependencies(QSMatrixRow[] rows, int factorBaseSize) {
        ArrayList<long[]> dependencies = new ArrayList<>();
        int row = 0;
        for (int col = 0; col < factorBaseSize && row < rows.length; col++) {
            int pivot = -1;
            for (int r = row; r < rows.length; r++) {
                if (bitIsSet(rows[r].parity, col)) {
                    pivot = r;
                    break;
                }
            }
            if (pivot == -1) continue;
            swap(rows, row, pivot);
            for (int r = 0; r < rows.length; r++) {
                if (r != row && bitIsSet(rows[r].parity, col))
                    rows[r] = new QSMatrixRow(xorInto(rows[r].parity, rows[row].parity), xorInto(rows[r].dependency, rows[row].dependency));
            }
            row++;
        }
        for (QSMatrixRow r : rows) {
            if (isZeroBits(r.parity))
                dependencies.add(r.dependency.clone());
        }
        return dependencies;
    }
    private static boolean bitIsSet(long[] bits, int index) {return ((bits[index >>> 6] >>> (index & 63)) & 1L) != 0;}
    private static long[] xorInto(long[] target, long[] source) {
        long[] out = new long[target.length];
        for (int i = 0; i < target.length; i++)
            out[i] = target[i] ^ source[i];
        return out;
    }
    private static boolean isZeroBits(long[] bits) {
        for (long word : bits)
            if (word != 0L) return false;
        return true;
    }
    private static void swap(QSMatrixRow[] rows, int i, int j) {
        if (i == j) return;
        QSMatrixRow temp = rows[i];
        rows[i] = rows[j];
        rows[j] = temp;
    }
    private static long[] reconstructFromDependency(
            long[] dependency,
            ArrayList<QSRelationFull> relations,
            QSPrime[] factorBase,
            long[] N
    ) {
        int columnCount = factorBase.length + 1;

        long[] X = ONE_MAG;
        long[] Y = ONE_MAG;

        long[] expSums = new long[columnCount];

        ArrayList<long[]> squareFactors = new ArrayList<>();

        for (int relIndex = 0; relIndex < relations.size(); relIndex++) {
            if (!bitIsSet(dependency, relIndex))
                continue;

            QSRelationFull rel = relations.get(relIndex);

            // X side: multiply all stored X-values.
            for (long[] x : rel.xValues)
                X = multiplyMod(X, x, N);

            // RHS exponent side: sparse accumulation.
            SparseExponents e = rel.exponents;

            for (int k = 0; k < e.index.length; k++) {
                int col = e.index[k];
                int count = e.count[k];

                expSums[col] += count;
            }
            // Large-prime square factors from merged partials.
            squareFactors.addAll(Arrays.asList(rel.squareFactors));
        }

        // Build Y from half exponents.
        for (int col = 0; col < expSums.length; col++) {
            long e = expSums[col];

            if ((e & 1L) != 0)
                throw new AssertionError("Dependency exponent sum is odd at column " + col);

            long half = e >>> 1;

            if (half == 0)
                continue;

            long[] factor;

            if (col == 0) {
                // Sign column: -1 mod N
                factor = subtractOneMag(N);
            } else {
                // Real factor-base prime. Column col corresponds to factorBase[col - 1].
                factor = new long[]{Integer.toUnsignedLong(factorBase[col - 1].p)};
            }

            for (long i = 0; i < half; i++)
                Y = multiplyMod(Y, factor, N);
        }

        // Each merged partial contributes one extra large prime to Y.
        // It is applied ONCE, because the pair created L^2 on RHS.
        for (long[] sf : squareFactors)
            Y = multiplyMod(Y, sf, N);

        long[] diff;
        int cmp = compareArray(X, Y);
        if (cmp >= 0)
            diff = subtractMagnitudes(X, Y);
        else
            diff = subtractMagnitudes(Y, X);
        long[] g = gcdMagnitude(diff, N);
        if(isNontrivialFactor(g, N))
            return g;
        g = gcdMagnitude(addMagnitudes(X, Y), N);
        if(isNontrivialFactor(g, N))
            return g;
        return ONE_MAG;
    }
    //given a number N, find a relation X^2 ≡ Y^2 (mod N). then (X-Y)(X+Y) reveals factors of N.
    public static long[] quadraticSieveMag(long[] x) {
        double lnX = bitLength(x) * Math.log(2.0);
        double LnLnX = Math.log(lnX);
        double c = 0.55; // tune experimentally
        long B = Math.round(Math.exp(c * Math.sqrt(lnX * LnLnX)));
        if(B < 100) B = 100;
        if(B > 10000000) return ONE_MAG;
        QSPrime[] factorBase = buildFactorBase(x, Math.toIntExact(B));
        int[] fbIndex = buildFactorBaseIndexMap(factorBase);

        // next: choose sieve interval and collect relations
        int requiredRelations = factorBase.length+30;
        ArrayList<QSRelationFull> relations = new ArrayList<>();
        HashMap<Long, QSRelationPartial> partials = new HashMap<>();

        while (relations.size() < requiredRelations)
            sieveBlock(x, factorBase, fbIndex, relations, partials, requiredRelations);

        long total0 = System.nanoTime();
        QSMatrixRow[] matrix = buildQSMatrix(relations);
        ArrayList<long[]> dependencies = nullspaceDependencies(matrix, factorBase.length);
        timeMatrix += System.nanoTime() - total0;

        long[] g = ONE_MAG;
        long dep0 = System.nanoTime();
        for (long[] dependency : dependencies) {
            long[] factor = reconstructFromDependency(dependency, relations, factorBase, x);
            if (isNontrivialFactor(factor, x)){
                g = factor;
                break;
            }
        }
        timeDependency += System.nanoTime() - dep0;

        System.out.println("fbSize=" + factorBase.length);
        System.out.printf(
                "polynomialCount=%d relationCount=%d partialMapSize=%d candidates=%d attempts=%d fullHits=%d partialHits=%d partialMerges=%d dependencyCount=%d%n" +
                        "time poly=%.3f%n sieveTime=%.3f%n attemptTime=%.3f%n matrix=%.3f%n dependencyCheck=%.3f%n",
                polynomialCount,
                relations.size(),
                partials.size(),
                candidates,
                positionsScanned,
                fullHits,
                partialHits,
                partialMerges,
                dependencies.size(),
                timePoly / 1e9,
                timeSieve / 1e9,
                attemptTime/1e9,
                timeMatrix / 1e9,
                timeDependency/1e9
        );
        return g;
    }

}