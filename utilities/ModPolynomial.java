package com.AtomCAS.utilities;
import java.util.Arrays;

/**
 * Special modular polynomial class in which all coefficients are reduced modulo a specified modulus
 */
public final class ModPolynomial implements Cloneable{
    private final LargeInteger[] coefficients; //little-endian
    private final LargeInteger modulus;
    private static int effectiveLength(LargeInteger[] coefficients) {
        int len = coefficients.length;
        while(len > 1 && coefficients[len-1].isZero()) len--;
        return len;
    }
    public ModPolynomial(LargeInteger[] coefficients, LargeInteger modulus) {this(coefficients, modulus, false);}
    private ModPolynomial(LargeInteger[] coefficients, LargeInteger modulus, boolean reduced) {
        if (modulus.lesserThanOrEqualTo(LargeInteger.ONE))
            throw new IllegalArgumentException("Modulus must be greater than 1");
        if (coefficients == null || coefficients.length == 0)
            throw new IllegalArgumentException("Coefficient array cannot be empty");
        LargeInteger[] reducer = new LargeInteger[coefficients.length];
        for (int i = 0; i < coefficients.length; i++)
            reducer[i] = reduced ? coefficients[i] : coefficients[i].mod(modulus);
        int len = effectiveLength(reducer);
        this.coefficients = len == reducer.length ? reducer : Arrays.copyOf(reducer, len);
        this.modulus = modulus;
    }
    public boolean isZero() { return isZeroArray(coefficients); }
    public int degree() { return coefficients.length-1; }
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ModPolynomial other)) return false;
        if (this == obj) return true;
        return modulus.equals(other.modulus) && Arrays.equals(coefficients, other.coefficients);
    }
    @Override
    public int hashCode() {return 31 * modulus.hashCode() + Arrays.hashCode(coefficients);}
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int k = coefficients.length - 1; k >= 0; k--) {
            LargeInteger c = coefficients[k];
            if (c.isZero()) continue;
            if (!sb.isEmpty()) sb.append(" + ");
            if (k == 0 || !c.isOne()) sb.append(c);
            if (k > 0) sb.append('x');
            if (k > 1) sb.append('^').append(k);
        }
        if (sb.isEmpty()) sb.append('0');
        return sb.append(" (mod ").append(modulus).append(")").toString();
    }
    public ModPolynomial clone() {
        try {
            ModPolynomial clone = (ModPolynomial) super.clone();
            return new ModPolynomial(clone.coefficients.clone(), clone.modulus.clone());
        } catch (CloneNotSupportedException e) {throw new AssertionError(e);}
    }
    public ModPolynomial negate() {
        LargeInteger[] out = new LargeInteger[coefficients.length];
        for (int i = 0; i < out.length; i++)
            out[i] = coefficients[i].isZero() ? LargeInteger.ZERO : modulus.subtract(coefficients[i]);
        return new ModPolynomial(out, modulus);
    }
    public ModPolynomial add(ModPolynomial other) {
        if(modulus.doesntEqual(other.modulus)) throw new IllegalArgumentException();
        LargeInteger[] out = new LargeInteger[Math.max(coefficients.length, other.coefficients.length)];
        for(int i = 0; i < out.length; i++) {
            out[i] = (i < coefficients.length ? coefficients[i] : LargeInteger.ZERO)
                    .add(i < other.coefficients.length ? other.coefficients[i] : LargeInteger.ZERO);
            if(out[i].greaterThanOrEqualTo(modulus))
                out[i] = out[i].subtract(modulus);
        }
        return new ModPolynomial(out, modulus, true);
    }
    public ModPolynomial subtract(ModPolynomial other) {
        if(modulus.doesntEqual(other.modulus)) throw new IllegalArgumentException();
        LargeInteger[] out = new LargeInteger[Math.max(coefficients.length, other.coefficients.length)];
        for(int i = 0; i < out.length; i++) {
            out[i] = (i < coefficients.length ? coefficients[i] : LargeInteger.ZERO)
                    .subtract(i < other.coefficients.length ? other.coefficients[i] : LargeInteger.ZERO);
            if(out[i].isNegative())
                out[i] = out[i].add(modulus);
        }
        return new ModPolynomial(out, modulus, true);
    }
    private static final int KARATSUBA_THRESHOLD = 24;
    private static final int TOOM3_THRESHOLD = 96;
    private static final int TOOM4_THRESHOLD = 256;
    private static final LargeInteger[] ZERO_POLY = {LargeInteger.ZERO};
    public ModPolynomial multiply(ModPolynomial other) {
        if (modulus.doesntEqual(other.modulus))
            throw new IllegalArgumentException("Moduli must match");
        return new ModPolynomial(multiplyRaw(coefficients, other.coefficients), modulus);
    }
    // All methods below operate over Z, without modular reduction.
    private static LargeInteger[] multiplyRaw(LargeInteger[] a, LargeInteger[] b) {
        if (isZeroArray(a) || isZeroArray(b)) return ZERO_POLY;
        int max = Math.max(a.length, b.length);
        int min = Math.min(a.length, b.length);
        // Avoid recursive splitting for highly unbalanced operands.
        if (min < KARATSUBA_THRESHOLD || max > 2L * min)
            return schoolbook(a, b);
        if (min < TOOM3_THRESHOLD)
            return karatsuba(a, b);
        if (min < TOOM4_THRESHOLD)
            return toom(a, b, 3);
        return toom(a, b, 4);
    }
    private static boolean isZeroArray(LargeInteger[] a) {return a.length == 1 && a[0].isZero();}
    private static LargeInteger[] trim(LargeInteger[] a) {
        int n = a.length;
        while (n > 1 && a[n - 1].isZero()) n--;
        return n == a.length ? a : Arrays.copyOf(a, n);
    }
    private static LargeInteger[] slice(LargeInteger[] a, int from, int size) {
        if (from >= a.length) return ZERO_POLY;
        return trim(Arrays.copyOfRange(a, from, Math.min(a.length, from + size)));
    }
    private static LargeInteger[] addRaw(LargeInteger[] a, LargeInteger[] b) {
        int n = Math.max(a.length, b.length);
        LargeInteger[] out = new LargeInteger[n];
        for (int i = 0; i < n; i++)
            out[i] = (i < a.length ? a[i] : LargeInteger.ZERO).add(i < b.length ? b[i] : LargeInteger.ZERO);
        return trim(out);
    }
    private static LargeInteger[] subtractRaw(LargeInteger[] a, LargeInteger[] b) {
        int n = Math.max(a.length, b.length);
        LargeInteger[] out = new LargeInteger[n];
        for (int i = 0; i < n; i++)
            out[i] = (i < a.length ? a[i] : LargeInteger.ZERO).subtract(i < b.length ? b[i] : LargeInteger.ZERO);
        return trim(out);
    }
    private static LargeInteger[] scaleRaw(LargeInteger[] a, int k) {
        if (k == 0) return ZERO_POLY;
        LargeInteger multiplier = LargeInteger.valueOf(k);
        LargeInteger[] out = new LargeInteger[a.length];
        for (int i = 0; i < a.length; i++)
            out[i] = a[i].multiply(multiplier);
        return trim(out);
    }
    private static LargeInteger[] exactDivideRaw(LargeInteger[] a, int k) {
        LargeInteger divisor = LargeInteger.valueOf(k);
        LargeInteger[] out = new LargeInteger[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = a[i].divide(divisor);
            if (!out[i].multiply(divisor).equals(a[i]))
                throw new ArithmeticException("Inexact Toom interpolation");
        }
        return trim(out);
    }
    private static void addShifted(LargeInteger[] dst, LargeInteger[] src, int shift) {
        for (int i = 0; i < src.length; i++)
            dst[i + shift] = dst[i + shift].add(src[i]);
    }
    // O(n*m)
    private static LargeInteger[] schoolbook(LargeInteger[] a, LargeInteger[] b) {
        LargeInteger[] out = new LargeInteger[a.length + b.length - 1];
        Arrays.fill(out, LargeInteger.ZERO);
        for (int i = 0; i < a.length; i++) {
            if (a[i].isZero()) continue;
            for (int j = 0; j < b.length; j++) {
                if (!b[j].isZero())
                    out[i + j] = out[i + j].add(a[i].multiply(b[j]));
            }
        }
        return trim(out);
    }
    // Three recursive multiplications.
    private static LargeInteger[] karatsuba(LargeInteger[] a, LargeInteger[] b) {
        int n = Math.max(a.length, b.length);
        int half = (n + 1) / 2;
        LargeInteger[] a0 = slice(a, 0, half);
        LargeInteger[] a1 = slice(a, half, half);
        LargeInteger[] b0 = slice(b, 0, half);
        LargeInteger[] b1 = slice(b, half, half);
        LargeInteger[] z0 = multiplyRaw(a0, b0);
        LargeInteger[] z2 = multiplyRaw(a1, b1);
        LargeInteger[] z1 = subtractRaw(subtractRaw(multiplyRaw(addRaw(a0, a1), addRaw(b0, b1)), z0), z2);
        LargeInteger[] out = new LargeInteger[2 * n + 2];
        Arrays.fill(out, LargeInteger.ZERO);
        addShifted(out, z0, 0);
        addShifted(out, z1, half);
        addShifted(out, z2, 2 * half);
        return trim(out);
    }
    // Evaluate a split polynomial at a small integer point.
    private static LargeInteger[] evaluate(LargeInteger[][] blocks, int point) {
        LargeInteger[] value = ZERO_POLY;
        for (int i = blocks.length - 1; i >= 0; i--)
            value = addRaw(scaleRaw(value, point), blocks[i]);
        return value;
    }
    // Generic Toom-Cook 3 / Toom-Cook 4.
    private static LargeInteger[] toom(LargeInteger[] a, LargeInteger[] b, int parts) {
        int pivot = (Math.max(a.length, b.length) + parts - 1) / parts;
        LargeInteger[][] aa = new LargeInteger[parts][];
        LargeInteger[][] bb = new LargeInteger[parts][];
        for (int i = 0; i < parts; i++) {
            aa[i] = slice(a, i * pivot, pivot);
            bb[i] = slice(b, i * pivot, pivot);
        }
        // Toom-3: 0, 1, -1, 2, infinity
        // Toom-4: 0, 1, -1, 2, -2, 3, infinity
        int[] points = parts == 3 ? new int[]{0, 1, -1, 2} : new int[]{0, 1, -1, 2, -2, 3};
        int degree = 2 * parts - 2;
        LargeInteger[] infinity = multiplyRaw(aa[parts - 1], bb[parts - 1]);
        LargeInteger[][] diffs = new LargeInteger[points.length][];
        // Evaluate, multiply, and remove the leading term.
        for (int i = 0; i < points.length; i++) {
            int t = points[i];
            int power = 1;
            for (int j = 0; j < degree; j++)
                power *= t;
            diffs[i] = subtractRaw(multiplyRaw(evaluate(aa, t), evaluate(bb, t)), scaleRaw(infinity, power));
        }
        // Newton divided differences.
        // All divisions are exact over Z.
        for (int order = 1; order < points.length; order++) {
            for (int j = points.length - 1; j >= order; j--)
                diffs[j] = exactDivideRaw(subtractRaw(diffs[j], diffs[j - 1]), points[j] - points[j - order]);
        }
        // Convert Newton form to ordinary power coefficients.
        LargeInteger[][] blocks = {diffs[points.length - 1]};
        for (int i = points.length - 2; i >= 0; i--) {
            LargeInteger[][] next = new LargeInteger[blocks.length + 1][];
            Arrays.fill(next, ZERO_POLY);
            for (int j = 0; j < blocks.length; j++) {
                next[j] = subtractRaw(next[j], scaleRaw(blocks[j], points[i]));
                next[j + 1] = addRaw(next[j + 1], blocks[j]);
            }
            next[0] = addRaw(next[0], diffs[i]);
            blocks = next;
        }
        // Recombine the polynomial blocks.
        LargeInteger[] out = new LargeInteger[2 * parts * pivot + 1];
        Arrays.fill(out, LargeInteger.ZERO);
        for (int i = 0; i < blocks.length; i++)
            addShifted(out, blocks[i], i * pivot);
        addShifted(out, infinity, degree * pivot);
        return trim(out);
    }
    record DivisionResult(ModPolynomial quotient, ModPolynomial remainder) {}
    DivisionResult divideAndRemainder(ModPolynomial divisor) {
        if (modulus.doesntEqual(divisor.modulus))
            throw new IllegalArgumentException("Moduli must match");
        if (divisor.isZero())
            throw new ArithmeticException("Division by zero polynomial");
        int n = degree();
        int m = divisor.degree();
        if (n < m)
            return new DivisionResult(new ModPolynomial(ZERO_POLY, modulus), this);
        LargeInteger inverse = divisor.coefficients[m].modInverse(modulus);
        LargeInteger[] q = new LargeInteger[n - m + 1];
        Arrays.fill(q, LargeInteger.ZERO);
        LargeInteger[] r = Arrays.copyOf(coefficients, coefficients.length);
        for (int k = n - m; k >= 0; k--) {
            LargeInteger c = r[m + k].multiply(inverse).mod(modulus);
            q[k] = c;
            if (c.isZero()) continue;
            for (int j = 0; j <= m; j++)
                r[j + k] = r[j + k].subtract(c.multiply(divisor.coefficients[j])).mod(modulus);
        }
        return new DivisionResult(new ModPolynomial(q, modulus, true),
                new ModPolynomial(r, modulus, true));
    }
    public ModPolynomial divide(ModPolynomial divisor) {return divideAndRemainder(divisor).quotient();}
    public ModPolynomial remainder(ModPolynomial divisor) {
        return remainderSchoolbook(divisor);
    }
    private ModPolynomial remainderSchoolbook(ModPolynomial divisor) {
        if (modulus.doesntEqual(divisor.modulus))
            throw new IllegalArgumentException("Moduli must match");
        if (divisor.isZero())
            throw new ArithmeticException("Division by zero polynomial");
        int n = degree();
        int m = divisor.degree();
        if (n < m) return this;
        boolean monic = divisor.coefficients[m].isOne();
        LargeInteger inverse = monic ? LargeInteger.ONE : divisor.coefficients[m].modInverse(modulus);
        LargeInteger[] r = Arrays.copyOf(coefficients, coefficients.length);
        for (int k = n - m; k >= 0; k--) {
            int top = m + k;
            LargeInteger c = monic ? r[top] : r[top].multiply(inverse).mod(modulus);
            r[top] = LargeInteger.ZERO;
            if (c.isZero()) continue;
            for (int j = 0; j < m; j++) {
                if (divisor.coefficients[j].isZero()) continue;
                r[j + k] = r[j + k].subtract(c.multiply(divisor.coefficients[j])).mod(modulus);
            }
        }
        return new ModPolynomial(Arrays.copyOf(r, Math.max(1, m)), modulus, true);
    }

    public ModPolynomial gcd(ModPolynomial other) {
        if (modulus.doesntEqual(other.modulus))
            throw new IllegalArgumentException("Moduli must match");
        ModPolynomial a = this;
        ModPolynomial b = other;
        while (!b.isZero()) {
            ModPolynomial r = a.remainder(b);
            a = b;
            b = r;
        }
        return a.monic();
    }
    public ModPolynomial monic() {
        if (isZero()) return this;
        LargeInteger inverse = coefficients[degree()].modInverse(modulus);
        LargeInteger[] out = new LargeInteger[coefficients.length];
        for (int i = 0; i < out.length; i++)
            out[i] = coefficients[i].multiply(inverse).mod(modulus);
        return new ModPolynomial(out, modulus);
    }
    private static ModPolynomial linearProduct(LargeInteger modulus, int from, int to, boolean negativeRoots) {
        if (from == to)
            return new ModPolynomial(new LargeInteger[]{LargeInteger.ONE}, modulus);
        if (to - from == 1) {
            long p = LargeInteger.SMALL_PRIMES[from];
            return new ModPolynomial(new LargeInteger[]{LargeInteger.valueOf(negativeRoots ? p : -p), LargeInteger.ONE}, modulus);
        }
        int mid = from + (to - from) / 2;
        return linearProduct(modulus, from, mid, negativeRoots).multiply(linearProduct(modulus, mid, to, negativeRoots));
    }
    public ModPolynomial derivative() {
        LargeInteger[] out = new LargeInteger[degree()];
        if(out.length == 0) return new ModPolynomial(ZERO_POLY, modulus);
        System.arraycopy(coefficients, 1, out, 0, degree());
        for(int j = 0; j < degree(); j++)
            out[j] = out[j].multiply(j+1);
        return new ModPolynomial(out, modulus);
    }
    /*private ModPolynomial multiplyModXPower(ModPolynomial other, int power) {

    }
    public ModPolynomial modXPower(int power) {
        LargeInteger[] out = new LargeInteger[power+1];
        System.arraycopy(coefficients, 1, out, 0, power);
        return new ModPolynomial(out, modulus);
    }
    private ModPolynomial inverseSeries(int precision) {
        if (precision < 1)
            throw new IllegalArgumentException("Precision must be positive");
        LargeInteger inverse = coefficients[0].modInverse(modulus);
        ModPolynomial g = new ModPolynomial(new LargeInteger[]{inverse}, modulus, true);
        ModPolynomial two = new ModPolynomial(new LargeInteger[]{LargeInteger.TWO}, modulus);
        int p = 1;
        while (p < precision) {
            int next = (int) Math.min(2L * p, precision);
            ModPolynomial fg = this.multiplyModXPower(g, next);
            ModPolynomial correction = two.subtract(fg);
            g = g.multiplyModXPower(correction, next);
            p = next;
        }
        return g.modXPower(precision);
    }*/
    public ModPolynomial powMod(LargeInteger exponent, ModPolynomial polynomialModulus) {
        if (modulus.doesntEqual(polynomialModulus.modulus))
            throw new IllegalArgumentException("Moduli must match");
        if (polynomialModulus.isZero())
            throw new ArithmeticException("Division by zero polynomial");
        if (exponent.isNegative())
            throw new IllegalArgumentException("Negative exponent");
        ModPolynomial result = new ModPolynomial(new LargeInteger[]{LargeInteger.ONE}, modulus);
        ModPolynomial base = this.remainder(polynomialModulus);
        LargeInteger e = exponent;
        while (!e.isZero()) {
            if (e.isOdd())
                result = result.multiply(base).remainder(polynomialModulus);
            e = e.shiftRight(1);
            if (!e.isZero())
                base = base.multiply(base).remainder(polynomialModulus);
        }
        return result.remainder(polynomialModulus);
    }
    static void main(String[] args) {
        LargeInteger modulus = LargeInteger.randomPrimeBits(27);
        // 500 shared linear factors
        ModPolynomial common = linearProduct(modulus, 0, 1000, false);
        // 500 additional factors each
        ModPolynomial positive = linearProduct(modulus, 1000, 2000, false);

        ModPolynomial negative = linearProduct(modulus, 1000, 3000, true);
        // Both degree 1000
        ModPolynomial A = common.multiply(positive);
        ModPolynomial B = common.multiply(negative);

        long start = System.nanoTime();
        ModPolynomial G = A.gcd(B);
        long elapsed = System.nanoTime() - start;

        System.out.println("A degree: " + A.degree());
        System.out.println("B degree: " + B.degree());
        System.out.println("GCD degree: " + G.degree());

        System.out.println("Correct: " + G.equals(common));

        System.out.printf("GCD time: %.6f seconds%n", elapsed / 1e9);
        if (!G.equals(common))
            throw new AssertionError("Incorrect polynomial GCD");
    }
}