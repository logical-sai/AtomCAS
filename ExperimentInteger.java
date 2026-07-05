package com.AtomCAS;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * This is sort of supposed to be a throwaway class to see how trying out base 2^64 will go. Negative limbs are not actually negative; they are simply words in base 2^64.
 */
public final class ExperimentInteger{
    final long[] limbs; //little-endian. easier for indexing
    final boolean sign; //allows negative numbers

    public static final ExperimentInteger NEGATIVE_ONE = new ExperimentInteger(-1);
    public static final ExperimentInteger ZERO = new ExperimentInteger(0L);
    public static final ExperimentInteger ONE = new ExperimentInteger(1L);
    public static final ExperimentInteger TWO = new ExperimentInteger(2L);
    public static final ExperimentInteger THREE = new ExperimentInteger(3L);
    public static final ExperimentInteger FOUR = new ExperimentInteger(4L);
    public static final ExperimentInteger FIVE = new ExperimentInteger(5L);

    public ExperimentInteger(long[] limbs, boolean sign) {
        this.limbs = trim(limbs);
        this.sign = limbs[this.limbs.length - 1] != 0 && sign;
    }
    public ExperimentInteger(long limb){
        limbs = new long[1];
        if(limb < 0){
            limbs[0] = -limb;
            sign = true;
        }else{
            limbs[0] = limb;
            sign = false;
        }
    }
    public ExperimentInteger(String s) {
        if (s == null)
            throw new NumberFormatException("null");
        s = s.trim();
        if (s.isEmpty())
            throw new NumberFormatException("empty string");
        boolean neg = false;
        int pos = 0;
        if (s.charAt(0) == '+' || s.charAt(0) == '-') {
            neg = s.charAt(0) == '-';
            pos = 1;
        }
        if (pos == s.length())
            throw new NumberFormatException("sign only");
        while (pos < s.length() && s.charAt(pos) == '0')
            pos++;
        if (pos == s.length()) {
            this.limbs = new long[]{0L};
            this.sign = false;
            return;
        }
        long[] acc = {0L};
        int firstLen = (s.length() - pos) % 9;
        if (firstLen == 0) firstLen = 9;
        int end = pos + firstLen;
        acc = multiply1000000000AddSmall(acc, parseChunk(s, pos, end));
        pos = end;
        while (pos < s.length()) {
            end = pos + 9;
            acc = multiply1000000000AddSmall(acc, parseChunk(s, pos, end));
            pos = end;
        }
        this.limbs = trim(acc);
        this.sign = neg && !isZeroArray(this.limbs);
    }
    public BigInteger toBigInteger() {
        if (isZeroArray(limbs)) return BigInteger.ZERO;
        byte[] bytes = new byte[limbs.length * 8 + 1]; // leading 0 keeps it positive
        int p = bytes.length - 1;
        for (long l : limbs) {
            long limb = l;
            for (int j = 0; j < 8; j++) {
                bytes[p--] = (byte) limb;
                limb >>>= 8;
            }
        }

        BigInteger out = new BigInteger(bytes);
        return sign ? out.negate() : out;
    }
    private static long parseChunk(String s, int start, int end) {
        long x = 0L;
        for (int i = start; i < end; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9')
                throw new NumberFormatException("Invalid digit: " + c);
            x = x * 10 + (c - '0');
        }
        return x;
    }
    private static long[] trim(long[] a) {
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
        x = trim(x);
        if (isZeroArray(x))
            return 0;
        int top = x.length - 1;
        return top * 64 + (64 - Long.numberOfLeadingZeros(x[top]));
    }
    public int bitLength() {return bitLength(limbs);}
    private static boolean testBit(long[] x, int bit) {
        int limb = bit >>> 6;
        int offset = bit & 63;

        if (limb >= x.length)
            return false;

        return ((x[limb] >>> offset) & 1L) != 0L;
    }
    private static boolean isZeroArray(long[] a) {
        for (long l : a)
            if (l != 0L)
                return false;
        return true;
    }
    public ExperimentInteger magnitude(){return new ExperimentInteger(this.limbs, false);}
    public ExperimentInteger copy(){return new ExperimentInteger(this.limbs, sign);}
    @Override
    public String toString() {
        long[] x = this.limbs;
        if (isZeroArray(x)) return "0";

        ArrayList<Long> chunks = new ArrayList<>();
        while (!isZeroArray(x)) {
            DivRemSmall d = divRem1e9(x);
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
    private static DivRemSmall divRem1e9(long[] mag) {
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
        return new DivRemSmall(trim(q), rem);
    }
    private record DivRemSmall(long[] quotient, long remainder) {}
    private static final SplittableRandom MASTER_RNG = new SplittableRandom();
    public static ExperimentInteger random(ExperimentInteger start, ExperimentInteger end) {
        SplittableRandom rng = MASTER_RNG.split();
        ExperimentInteger range = end.subtract(start);
        if (range.sign || isZeroArray(range.limbs))
            throw new IllegalArgumentException("Error: Invalid range: [" + start + ", " + end + ")");
        long[] bound = trim(range.limbs);
        long[] candidate = new long[bound.length];
        int top = bound.length - 1;
        do {
            for (int i = 0; i < top; i++)
                candidate[i] = rng.nextLong();
            candidate[top] = randomUnsignedAtMost(bound[top], rng);
        } while (compareArray(candidate, bound) >= 0);
        return new ExperimentInteger(candidate, false).add(start);
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
    public static ExperimentInteger randomChunks(int chunks) {
        if (chunks < 0)
            throw new IllegalArgumentException("chunks must be nonnegative");
        if (chunks == 0)
            return ZERO;
        SplittableRandom rng = MASTER_RNG.split();
        long[] mag = new long[chunks];
        for (int i = 0; i < chunks; i++)
            mag[i] = rng.nextLong();
        // Force actual chunk length to be exactly `chunks`
        while(mag[chunks - 1] == 0L)
            mag[chunks - 1] = rng.nextLong();
        return new ExperimentInteger(mag, false);
    }
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
    private static ExperimentInteger fromTwosComplement(long[] tc) {
        tc = Arrays.copyOf(tc, tc.length);
        boolean negative = tc[tc.length - 1] < 0;
        if (!negative)
            return new ExperimentInteger(trim(tc), false);
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
        return new ExperimentInteger(trim(tc), true);
    }
    public ExperimentInteger NOT(){return this.negate().subtract(ONE);}
    public ExperimentInteger AND(ExperimentInteger other) {
        int len = Math.max(this.limbs.length, other.limbs.length) + 1;
        long[] a = this.toTwosComplement(len);
        long[] b = other.toTwosComplement(len);
        long[] out = new long[len];
        for (int i = 0; i < len; i++)
            out[i] = a[i] & b[i];
        return fromTwosComplement(out);
    }
    public ExperimentInteger OR(ExperimentInteger other) {
        int len = Math.max(this.limbs.length, other.limbs.length) + 1;
        long[] a = this.toTwosComplement(len);
        long[] b = other.toTwosComplement(len);
        long[] out = new long[len];
        for (int i = 0; i < len; i++)
            out[i] = a[i] | b[i];
        return fromTwosComplement(out);
    }
    public ExperimentInteger XOR(ExperimentInteger other) {
        int len = Math.max(this.limbs.length, other.limbs.length) + 1;
        long[] a = this.toTwosComplement(len);
        long[] b = other.toTwosComplement(len);
        long[] out = new long[len];
        for (int i = 0; i < len; i++)
            out[i] = a[i] ^ b[i];
        return fromTwosComplement(out);
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
    public int compareMags(ExperimentInteger other){return compareArray(this.limbs, other.limbs);}
    public int compareTo(ExperimentInteger other){
        int cmp = this.compareMags(other);
        if(cmp == 1){
            if(sign) return -1;
            return 1;
        }else if(cmp == -1){
            if(other.sign) return 1;
            return -1;
        }
        if(sign && !other.sign) return -1;
        else if(!sign && other.sign) return 1;
        return 0;
    }
    public boolean greaterThan(ExperimentInteger other){return compareTo(other) > 0;}
    public boolean lessThan(ExperimentInteger other){return compareTo(other) < 0;}
    public boolean greaterThanOrEqualTo(ExperimentInteger other){return compareTo(other) >= 0;}
    public boolean lesserThanOrEqualTo(ExperimentInteger other){return compareTo(other) <= 0;}
    public boolean equalsTo(ExperimentInteger other){return compareTo(other) == 0;}
    public boolean notEqualsTo(ExperimentInteger other){return compareTo(other) != 0;}
    public ExperimentInteger negate(){return new ExperimentInteger(this.limbs, !sign);}
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
    private record DivRemLongPair(LongPair quotient, long remainder) {}
    private static long[] multiply1000000000AddSmall(long[] mag, long add) {
        if (add < 0)
            throw new IllegalArgumentException("small values must be nonnegative signed longs");
        mag = trim(mag);
        long[] out = new long[mag.length + 2];
        long carry = add;
        for (int i = 0; i < mag.length; i++) {
            LongPair p = LongPair.multiply64(mag[i], 1000000000L).addLow(carry);
            out[i] = p.low;
            carry = p.high;
        }
        out[mag.length] = carry;
        return trim(out);
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
        return trim(res);
    }
    private static long[] subtractMagnitudes(long[] a, long[] b) {
        a = trim(a);
        b = trim(b);
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
        return trim(out);
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
    private static long[] shiftLeftLimbs(long[] x, int limbs) {
        x = trim(x);
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
        x = trim(x);

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

        return trim(out);
    }
    private static long[] shiftRightLimbs(long[] x, int limbs) {
        x = trim(x);
        if (isZeroArray(x) || limbs >= x.length)
            return new long[]{0L};
        if (limbs <= 0)
            return x;
        long[] out = new long[x.length - limbs];
        System.arraycopy(x, limbs, out, 0, x.length - limbs);
        return trim(out);
    }
    private static long[] shiftRightBits(long[] x, int bits) {
        x = trim(x);
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

        return trim(out);
    }
    public ExperimentInteger shiftRightBits(int bits){return new ExperimentInteger(shiftRightBits(limbs, bits), sign);}
    private static long[] slice(long[] a, int low, int high) {
        if (low < 0)
            throw new IllegalArgumentException("negative slice index");
        if (high <= low || low >= a.length)
            return new long[]{0L};
        high = Math.min(high, a.length);
        return trim(Arrays.copyOfRange(a, low, high));
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
        return trim(out);
    }
    private static SignedMag multiplyMagnitudeBySignedLong(long[] a, long m) {
        if(m < 0) return new SignedMag(multiplySmallMag(a, -m), true);
        return new SignedMag(multiplySmallMag(a, m), false);
    }
    public ExperimentInteger max(ExperimentInteger other){return this.greaterThanOrEqualTo(other) ? this : other;}
    public ExperimentInteger min(ExperimentInteger other){return this.lessThan(other) ? this : other;}
    private static long[] multiplyMag(long[] a, long[] b, int KST, int T3T) {
        a = trim(a);
        b = trim(b);
        if (isZeroArray(a) || isZeroArray(b))
            return new long[]{0L};
        long[] t = a.length > b.length ? a : b;
        long[] u = Arrays.equals(t, a) ? b : a;
        int max = t.length;
        int min = u.length;
        if(min == 1)
            return multiplySmallMag(t, u[0]);
        if (min < KST) {
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
            return trim(out);
        } else if (min < T3T) {
            /*
             * Karatsuba multiplication is a specialized algorithm that uses recursion to "split"
             * large numbers into two smaller parts and operate on the parts. Unlike standard grade-school
             * multiply(which uses 4 multiplications), Karatsuba only needs to do 3 multiplications(and several additions)
             * Karatsuba runs in time complexity O(n^(log_2(3)) ≈ O(n^1.585)
             */
            int pivot = max/2;

            long[] lower1 = slice(t, 0, pivot);
            long[] upper1 = slice(t, pivot, t.length);
            long[] lower2 = slice(u, 0, pivot);
            long[] upper2 = slice(u, pivot, u.length);

            long[] z0 = multiplyMag(lower1, lower2, KST, T3T);
            long[] z2 = multiplyMag(upper1, upper2, KST, T3T);
            long[] z1 = subtractMagnitudes(multiplyMag(addMagnitudes(lower1, upper1), addMagnitudes(lower2, upper2), KST, T3T), z0);
            z1 = subtractMagnitudes(z1, z2);
            return trim(addMagnitudes(addMagnitudes(z0, shiftLeftLimbs(z1, pivot)), shiftLeftLimbs(z2, 2 * pivot)));
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

        long[] v0 = multiplyMag(a0, b0, KST, T3T);
        long[] v1 = multiplyMag(p1, q1, KST, T3T);
        SignedMag vm1 = pm1.multiply(qm1);
        long[] v2 = multiplyMag(p2, q2, KST, T3T);
        long[] vInf = multiplyMag(a2, b2, KST, T3T);

        SignedMag r2 = SignedMag.positive(v1).add(vm1).divideExactSmall(2).subtract(SignedMag.positive(addMagnitudes(v0, vInf)));

        SignedMag s = SignedMag.positive(v1).subtract(vm1).divideExactSmall(2);

        SignedMag T = SignedMag.positive(v2).subtract(SignedMag.positive(v0)).subtract(SignedMag.positive(multiplySmallMag(vInf, 16))).divideExactSmall(2);

        SignedMag r3 = T.subtract(s).subtract(SignedMag.positive(multiplySmallMag(r2.mag, 2))).divideExactSmall(3);

        SignedMag r1 = s.subtract(r3);
        return SignedMag.positive(v0).add(r1.shiftLimbs(m)).add(r2.shiftLimbs(2 * m)).add(r3.shiftLimbs(3 * m)).add(SignedMag.positive(shiftLeftLimbs(vInf, 4 * m))).toPositiveMagnitudeChecked();
    }
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
    private static long[] squareMag(long[] a, int GST, int KST){
        if(isZeroArray(a))
            return a;
        int n = a.length;
        if(n == 1){
            LongPair PL = LongPair.multiply64(a[0], a[0]);
            return trim(new long[]{PL.low, PL.high});
        }
        if(n <= GST){
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

            return trim(out);
        }else if(n <= KST){
            int m = n >>> 1;
            long[] x0 = slice(a, 0, m);
            long[] x1 = slice(a, m, n);

            long[] z0 = squareMag(x0, GST, KST);
            long[] z2 = squareMag(x1, GST, KST);
            long[] z1 = squareMag(addMagnitudes(x0, x1), GST, KST);
            z1 = subtractMagnitudes(z1, z0);
            z1 = subtractMagnitudes(z1, z2);

            long[] out = new long[2 * n + 4];

            addAt(out, z0, 0);
            addAt(out, z1, m);
            addAt(out, z2, 2 * m);

            return trim(out);
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
        long[] v0 = squareMag(a0, GST, KST);
        long[] v1 = squareMag(p1, GST, KST);
        long[] vm1 = squareMag(pm1.mag, GST, KST);
        long[] v2 = squareMag(p2, GST, KST);
        long[] vInf = squareMag(a2, GST, KST);

        //interpolate
        long[] r2 = subtractMagnitudes(divideMagExactSmall(addMagnitudes(v1, vm1),2), addMagnitudes(v0, vInf));
        long[] s = divideMagExactSmall(subtractMagnitudes(v1, vm1), 2);
        long[] t = divideMagExactSmall(subtractMagnitudes(subtractMagnitudes(v2, v0), multiplySmallMag(vInf, 16)), 2);
        long[] r3 = divideMagExactSmall(subtractMagnitudes(subtractMagnitudes(t, s), multiplySmallMag(r2, 2)), 3);
        long[] r1 = subtractMagnitudes(s, r3);
        return addMagnitudes(addMagnitudes(addMagnitudes(addMagnitudes(v0, shiftLeftLimbs(r1, m)), shiftLeftLimbs(r2, 2 * m)), shiftLeftLimbs(r3, 3 * m)), shiftLeftLimbs(vInf, 4 * m));
    }
    private static long[] divideMagExactSmall(long[] a, long d) {
        if (d == 0L)
            throw new ArithmeticException("divide by zero");

        a = trim(a);

        long[] q = new long[a.length];
        long rem = 0L;

        for (int i = a.length - 1; i >= 0; i--) {
            long[] qr = LongPair.divRem128By64To64(rem, a[i], d);
            q[i] = qr[0];
            rem = qr[1];
        }

        if (rem != 0L)
            throw new ArithmeticException("not exactly divisible");

        return trim(q);
    }
    private static int modSmall(long[] a, int p) {
        if (p <= 0)
            throw new IllegalArgumentException("Modulus must be positive");
        long rem = 0L;
        // Compute 2^64 mod p.
        long baseMod = BigInteger.ONE.shiftLeft(64).mod(BigInteger.valueOf(p)).longValue();
        // Do not use this inside the hot loop long-term.
        for (int i = a.length - 1; i >= 0; i--) {
            long limbMod = Long.remainderUnsigned(a[i], p);
            rem = (rem * baseMod + limbMod) % p;
        }
        return (int) rem;
    }
    private static long modUnsignedLong(long[] a, long d) {
        if (d == 0L)
            throw new ArithmeticException("divide by zero");

        a = trim(a);

        if (a.length == 1)
            return Long.remainderUnsigned(a[0], d);
        long rem = 0L;
        for (int i = a.length - 1; i >= 0; i--) {
            long[] qr = LongPair.divRem128By64To64(rem, a[i], d);
            rem = qr[1];
        }

        return rem;
    }
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
    /**
     * @param mag      unsigned little-endian magnitude
     * @param negative false if zero
     */
    private record SignedMag(long[] mag, boolean negative) {
        static final SignedMag ZERO = new SignedMag(new long[]{0L}, false);
        SignedMag {
            mag = trim(mag);
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
        SignedMag multiply(SignedMag other) {return new SignedMag(multiplyMag(this.mag, other.mag, 1024, 16384), this.negative ^ other.negative);}
        SignedMag shiftLimbs(int limbs) {return new SignedMag(ExperimentInteger.shiftLeftLimbs(this.mag, limbs), negative);}
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
    private static ExperimentInteger productRange(long a, long b) {
        if (a > b)
            return ONE;
        if (a == b)
            return new ExperimentInteger(a);
        if (b - a == 1) return new ExperimentInteger(a).multiply(new ExperimentInteger(b));
        long middle = (a + b) >>> 1;
        return productRange(a, middle).multiply(productRange(middle + 1, b));
    }
    public static ExperimentInteger factorial(int n){
        if(n < 0) throw new ArithmeticException("Error: Factorial undefined for negative integers");
        return productRange(1L, n);
    }
    private static long[][] divideMagnitude(long[] a, long[] b) {
        if (isZeroArray(b))
            throw new ArithmeticException("divide by zero");
        a = trim(a);
        b = trim(b);
        int cmp = compareArray(a, b);
        if (cmp < 0) return new long[][]{new long[]{0L}, a};
        if (cmp == 0) return new long[][]{new long[]{1L}, new long[]{0L}};
        if (b.length == 1){
            if (b[0] == 1L) return new long[][]{a, new long[]{0L}};
            if ((b[0] & (b[0] - 1L)) == 0L) {
                int shift = Long.numberOfTrailingZeros(b[0]);
                long[] q = shiftRightBits(a, shift);
                long[] r = {a[0] & (b[0] - 1L)};
                return new long[][]{trim(q), trim(r)};
            }
            long[] result = new long[a.length];
            long rem = 0;
            for(int i = result.length - 1; i >= 0; i--){
                LongPair cur = new LongPair(rem, a[i]);
                long[] DR = LongPair.divRem128By64To64(cur.high, cur.low, b[0]);
                result[i] = DR[0];
                rem = DR[1];
            }
            return new long[][]{trim(result), new long[]{rem}};
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
                DivRemLongPair dr = new LongPair(ujn, ujn1)
                        .divideUnsignedByLong(v[n - 1]);

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
        q = trim(q);
        r = trim(r);
        return new long[][]{q, r};
    }
    private static final class BarrettReducer{
        private final long[] modulus;
        private final long[] mu;
        private final int k;
        private final long[] bToKPlusOne;

        BarrettReducer(long[] modulus) {
            modulus = trim(modulus);
            if (isZeroArray(modulus))
                throw new ArithmeticException("modulus is zero");

            this.modulus = modulus;
            this.k = modulus.length;
            this.mu = divideMagnitude(basePower(2 * k).limbs, modulus)[0];
            this.bToKPlusOne = basePower(k + 1).limbs;
        }
        long[] reduce(long[] x) {
            x = trim(x);
            if (compareArray(x, modulus) < 0)
                return x;
            if (x.length <= 2 * k)
                return reduceProduct(x);
            return divideMagnitude(x, modulus)[1];
        }
        long[] reduceProduct(long[] x) {
            x = trim(x);
            if (compareArray(x, modulus) < 0)
                return x;
            long[] q1 = shiftRightLimbs(x, k - 1);
            long[] q2 = multiplyMag(q1, mu, 48, 16_384);
            long[] q3 = shiftRightLimbs(q2, k + 1);
            long[] q3m = multiplyMag(q3, modulus, 48, 16_384);

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
        private long[] multiply(long[] a, long[] b) {return reduce(multiplyMag(a, b, 48, 16_384));}
        public long[] square(long[] a) {return reduce(multiplyMag(a, a, 48, 16_384));}
        private static long[] powModBarrett(long[] base, long[] exponent, long[] modulus) {
            BarrettReducer reducer = new BarrettReducer(modulus);
            if (isZeroArray(exponent))
                return reducer.reduce(ONE.limbs);

            base = reducer.reduce(base);
            int window = windowSizeForBits(bitLength(exponent));
            int tableSize = 1 << (window - 1);
            long[][] oddPowers = new long[tableSize][];

            // oddPowers[i] = base^(2*i + 1)
            oddPowers[0] = base;

            long[] base2 = reducer.square(base);

            for (int i = 1; i < tableSize; i++)
                oddPowers[i] = reducer.multiply(oddPowers[i - 1], base2);
            long[] result = reducer.reduce(ONE.limbs);
            int bit = bitLength(exponent) - 1;

            while (bit >= 0) {
                if (!testBit(exponent, bit)) {
                    result = reducer.square(result);
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
                    result = reducer.square(result);

                result = reducer.multiply(result, oddPowers[value >>> 1]);

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
            modulus = trim(modulus);

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
            t = trim(t);

            long[] work = new long[Math.max(t.length, 2 * k + 2) + 2];
            System.arraycopy(t, 0, work, 0, t.length);

            for (int i = 0; i < k; i++) {
                long u = work[i] * mPrime;
                addMulLimbAt(work, modulus, u, i);
            }

            int resultLen = Math.min(k + 2, work.length - k);
            long[] result = new long[resultLen];
            System.arraycopy(work, k, result, 0, resultLen);
            result = trim(result);
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
            return trim(result);
        }
        public long[] multiply(long[] a, long[] b) {return reduce(multiplyMag(a, b, 48, 16_384));}
        public long[] square(long[] a) {return reduce(multiplyMag(a, a, 48, 16_384));}
        private static long[] powModMontgomery(long[] base, long[] exponent, long[] modulus) {
            MontgomeryReducer reducer = new MontgomeryReducer(modulus);
            if (isZeroArray(exponent))
                return reducer.fromMontgomery(reducer.rModM);

            base = reducer.toMontgomery(base);
            int window = windowSizeForBits(bitLength(exponent));
            int tableSize = 1 << (window - 1);
            long[][] oddPowers = new long[tableSize][];

            oddPowers[0] = base;

            long[] base2 = reducer.square(base);

            for (int i = 1; i < tableSize; i++)
                oddPowers[i] = reducer.multiply(oddPowers[i - 1], base2);

            long[] result = reducer.rModM;

            int bit = bitLength(exponent) - 1;

            while (bit >= 0) {
                if (!testBit(exponent, bit)) {
                    result = reducer.square(result);
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
                    result = reducer.square(result);

                result = reducer.multiply(result, oddPowers[value >>> 1]);

                bit -= width;
            }

            return reducer.fromMontgomery(result);
        }
        long[] toMontgomery(long[] x) {return multiply(divideMagnitude(x, modulus)[1], r2ModM);}// x * R^2 * R^-1 = xR mod m
        long[] fromMontgomery(long[] x) {return reduce(x);} // xR * R^-1 = x
    }
    //public API layer
    public ExperimentInteger add(ExperimentInteger other){
        if(this.sign == other.sign)
            return new ExperimentInteger(addMagnitudes(limbs, other.limbs), sign);
        long cmp = this.compareMags(other);
        if(cmp == 1)
            return new ExperimentInteger(subtractMagnitudes(limbs, other.limbs), sign);
        else if(cmp == -1)
            return new ExperimentInteger(subtractMagnitudes(other.limbs, limbs), other.sign);
        return ZERO;
    }
    public ExperimentInteger subtract(ExperimentInteger other){return this.add(other.negate());}
    public ExperimentInteger multiply(ExperimentInteger other){return new ExperimentInteger(multiplyMag(this.limbs, other.limbs, 49, 16385), sign ^ other.sign);}
    public ExperimentInteger multiply(long other){
        if(other < 0) return new ExperimentInteger(limbs, !sign).multiplyUnsigned(-other);
        return multiplyUnsigned(other);
    }
    public ExperimentInteger multiplyUnsigned(long other){
        if(other == 0) return ZERO;
        return new ExperimentInteger(multiplySmallMag(limbs, other), sign);
    }
    public ExperimentInteger square(){return new ExperimentInteger(squareMag(limbs, 49, 16385), false);}
    public boolean isPerfectSquare(){return sqrtFloor().square().equalsTo(this);}
    public ExperimentInteger cube(){return this.square().multiply(this);}
    public ExperimentInteger[] quotientAndRemainder(ExperimentInteger other) {
        if (isZeroArray(other.limbs))
            throw new ArithmeticException("divide by zero");
        long[][] qr = divideMagnitude(this.limbs, other.limbs);
        ExperimentInteger q = new ExperimentInteger(qr[0], this.sign != other.sign);
        ExperimentInteger r = new ExperimentInteger(qr[1], this.sign);
        return new ExperimentInteger[]{q, r};
    }
    public ExperimentInteger longDivision(ExperimentInteger other){return quotientAndRemainder(other)[0];}
    public ExperimentInteger floorDivision(ExperimentInteger other){
        ExperimentInteger[] DR = quotientAndRemainder(other);
        if(DR[1].sign) return DR[0].subtract(ONE);
        return DR[0];
    }
    public ExperimentInteger remainder(ExperimentInteger other){return quotientAndRemainder(other)[1];}
    public ExperimentInteger mod(ExperimentInteger modulus) {
        if (isZeroArray(modulus.limbs))
            throw new ArithmeticException("mod by zero");
        ExperimentInteger r = this.remainder(modulus);
        if(isZeroArray(r.limbs)) return ZERO;
        else if(r.sign != modulus.sign)
            return r.add(modulus);
        return r;
    }
    public int mod(int modulus){
        int mag = modSmall(limbs, modulus);
        if(sign && mag != 0) mag = modulus - mag;
        return mag;
    }
    public ExperimentInteger modInverse(ExperimentInteger modulus) {
        if (isZeroArray(modulus.limbs))
            throw new ArithmeticException("modulus is zero");
        ExperimentInteger[] eg = this.extendedGCD(modulus);
        ExperimentInteger g = eg[0];
        ExperimentInteger x = eg[1];
        if (!g.equalsTo(ONE))
            throw new ArithmeticException("Error: Modular inverse for " + this + " and " + modulus + " does not exist");
        return x.mod(modulus);
    }
    public boolean isOdd(){return (limbs[0] & 1L) == 1L;}
    public boolean isEven(){return (limbs[0] & 1L) == 0L;}
    public ExperimentInteger power(long l){
        if(this.equalsTo(ONE))
            return ONE;
        if(l < 0) throw new ArithmeticException("Error: Fractions not supported for now");
        if(l == 0){
            if(this.equalsTo(ZERO)) throw new ArithmeticException("Error: Indeterminate form: 0^0");
            return ONE;
        }
        if(this.equalsTo(ZERO))
            return ZERO;
        if(this.equalsTo(NEGATIVE_ONE))
            return (l & 1L) == 0L ? ONE : NEGATIVE_ONE;
        long expTemp = l;
        ExperimentInteger basTemp = this.copy();
        ExperimentInteger result = ONE;
        while(true){
            if(expTemp % 2 == 1){
                result = result.multiply(basTemp);
                if(expTemp == 1) break;
            }
            expTemp >>>= 1;
            basTemp = basTemp.square();
        }
        return result;
    }
    public static ExperimentInteger basePower(int l){
        if(l < 0) throw new ArithmeticException("Error: Fractions not supported for now");
        if(l == 0) return ONE;
        long[] out = new long[l+1];
        out[out.length-1] = 1L;
        return new ExperimentInteger(out,false);
    }
    public static ExperimentInteger powerOfTwo(int bit) {
        if (bit < 0)
            throw new IllegalArgumentException();
        long[] out = new long[(bit >>> 6) + 1];
        out[bit >>> 6] = 1L << (bit & 63);
        return new ExperimentInteger(out, false);
    }
    public ExperimentInteger sqrtFloor() {
        if (this.sign)
            throw new ArithmeticException("sqrt of negative");

        if (isZeroArray(this.limbs) || this.equalsTo(ONE))
            return this;
        int bits = bitLength(this.limbs);
        ExperimentInteger x = powerOfTwo((bits + 1) >>> 1);

        while (true) {
            ExperimentInteger q = this.longDivision(x);
            ExperimentInteger next = new ExperimentInteger(shiftRightBits(x.add(q).limbs, 1), false);

            if (next.greaterThanOrEqualTo(x)) {
                while (x.multiply(x).greaterThan(this))
                    x = x.subtract(ONE);
                return x;
            }
            x = next;
        }
    }
    public ExperimentInteger sqrtCeil() {
        ExperimentInteger floor = sqrtFloor();
        return floor.square().equalsTo(this) ? floor : floor.add(ONE);
    }
    public ExperimentInteger powMod(ExperimentInteger exponent, ExperimentInteger modulus) {
        if (isZeroArray(modulus.limbs))
            throw new ArithmeticException("modulus is zero");
        if (modulus.sign){
            ExperimentInteger tmp = powMod(exponent, modulus.negate());
            if(tmp.equalsTo(ZERO)) return tmp;
            return tmp.add(modulus);
        }
        if (modulus.equals(ONE))
            return ZERO;

        if (exponent.sign)
            return this.modInverse(modulus).powMod(exponent.negate(), modulus);
        ExperimentInteger base = this.mod(modulus);

        long[] result;

        if ((modulus.limbs[0] & 1L) != 0L)
            result = MontgomeryReducer.powModMontgomery(base.limbs, exponent.limbs, modulus.limbs);
        else{
            if(modulus.equalsTo(TWO)) return isOdd() ? ONE : ZERO;
            result = BarrettReducer.powModBarrett(base.limbs, exponent.limbs, modulus.limbs);
        }
        return new ExperimentInteger(result, false);
    }
    private static long gcdSmallUnsigned(long[] a, long b) {
        a = trim(a);
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
        a = trim(a);
        b = trim(b);
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
        mag = trim(mag);

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
        x = trim(x);
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
        a = trim(a);
        b = trim(b);

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
                        long[] na = trim(next[0]);
                        long[] nb = trim(next[1]);
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
    public ExperimentInteger gcd(ExperimentInteger other) {return new ExperimentInteger(gcdMagnitude(limbs, other.limbs), false);}
    public ExperimentInteger[] extendedGCD(ExperimentInteger other) {
        ExperimentInteger oldR = this;
        ExperimentInteger r = other;

        ExperimentInteger oldS = ONE;
        ExperimentInteger s = ZERO;

        ExperimentInteger oldT = ZERO;
        ExperimentInteger t = ONE;
        while (!isZeroArray(r.limbs)) {
            ExperimentInteger[] qr = oldR.quotientAndRemainder(r);
            ExperimentInteger q = qr[0];

            ExperimentInteger nextR = oldR.subtract(q.multiply(r));
            oldR = r;
            r = nextR;

            ExperimentInteger nextS = oldS.subtract(q.multiply(s));
            oldS = s;
            s = nextS;

            ExperimentInteger nextT = oldT.subtract(q.multiply(t));
            oldT = t;
            t = nextT;
        }
        // Make gcd nonnegative.
        if (oldR.sign) {
            oldR = oldR.negate();
            oldS = oldS.negate();
            oldT = oldT.negate();
        }
        return new ExperimentInteger[]{oldR, oldS, oldT};
    }
    public ExperimentInteger lcm(ExperimentInteger other){return longDivision(gcd(other)).multiply(other).magnitude();}
    public boolean isProbablyPrime() {return isProbablyPrime(defaultPrimeCertaintyBits(bitLength()));}
    public boolean isProbablyPrime(int certaintyBits) {
        if (this.sign)
            return false;

        if (this.equalsTo(TWO))
            return true;

        if (this.lessThan(TWO))
            return false;

        if (!this.isOdd())
            return false;

        // Fast small-prime screen first.
        for (SmallPrimeInfo info : SMALL_PRIME_INFOS) {
            int p = info.p();
            if (this.equalsTo(new ExperimentInteger(p)))
                return true; // it is the small prime itself, not composite
            if (modSmall(this.limbs, info) == 0)
                return false;
        }
        //miller-rabin test
        ExperimentInteger nMinusOne = this.subtract(ONE);

        ExperimentInteger d = nMinusOne;
        int s = 0;
        while (!d.isOdd()) {
            d = d.longDivision(TWO);
            s++;
        }
        int rounds = Math.max(1, (certaintyBits + 1) >>> 1);
        for (int i = 0; i < rounds; i++) {
            ExperimentInteger a = random(TWO, nMinusOne); // 2 <= a < n-1
            ExperimentInteger x = a.powMod(d, this);
            if (x.equalsTo(ONE) || x.equalsTo(nMinusOne))
                continue;
            boolean witnessPassed = false;
            for (int r = 1; r < s; r++) {
                x = x.powMod(TWO, this);
                // or faster:
                // x = x.powMod(TWO, n);
                if (x.equalsTo(nMinusOne)) {
                    witnessPassed = true;
                    break;
                }
                if (x.equalsTo(ONE))
                    return false;
            }
            if (!witnessPassed)
                return false;
        }
        return true;
    }
    private static final int[] WHEEL_30_RESIDUES = {1, 7, 11, 13, 17, 19, 23, 29};
    private static final int[] WHEEL_30_STEPS = {6, 4, 2, 4, 2, 4, 6, 2};
    public ExperimentInteger nextProbablePrime() {
        if (this.sign || this.lessThan(TWO))
            return TWO;

        if (this.lessThan(THREE))
            return THREE;

        if (this.lessThan(FIVE))
            return FIVE;

        ExperimentInteger candidate = this.add(ONE);

        int rem30 = candidate.mod(30);
        int wheelIndex = firstWheelIndexAtOrAbove(rem30);

        int targetResidue = WHEEL_30_RESIDUES[wheelIndex];
        int delta = targetResidue - rem30;

        if (delta < 0)
            delta += 30;

        candidate = candidate.add(new ExperimentInteger(delta));

        while (true) {
            if (candidate.isProbablyPrime())
                return candidate;

            candidate = candidate.add(new ExperimentInteger(WHEEL_30_STEPS[wheelIndex]));
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
    public static ExperimentInteger randomPrime(int chunks){
        ExperimentInteger E = ExperimentInteger.randomChunks(chunks).OR(ONE);
        while(!E.isProbablyPrime()) E = ExperimentInteger.randomChunks(chunks).OR(ONE);
        return E;
    }
    public static ExperimentInteger randomPrimeBits(int bits) {
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

            ExperimentInteger e = new ExperimentInteger(limbs, false);

            if (e.isProbablyPrime())
                return e;
        }
    }
    public static int[] sieve(int bound) {
        if (bound < 2)
            return new int[0];

        if (bound == 2)
            return new int[]{2};

        // composite[i] represents odd number 2*i + 3.
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
    private static final int[] SMALL_PRIMES = sieve(1000000);
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
}