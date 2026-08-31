package com.AtomCAS;
import com.AtomCAS.utilities.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.SplittableRandom;

public class DevPlayground {
    private static void testReciprocal(String input) {
        ScalarAtom x = Parser.parse(input);
        System.out.println("1/(" + x + ") = " + x.reciprocal());
    }
    static volatile Object sink = new Object();
    static void benchmarkModMagnitudes() {
        SplittableRandom rng = new SplittableRandom(123456789L);

        int[] sizes = {64, 96, 128, 160, 192, 256, 384, 512, 768, 1024};
        System.out.printf("%6s %6s %8s %12s %12s %9s%n", "div", "num", "reps", "Knuth(ns)", "BZ(ns)", "BZ/Knuth");
        long sink = 0;
        for (int dLen : sizes) {
            int[] dividendLengths = {dLen + 32, dLen * 2, dLen * 4};
            for (int nLen : dividendLengths) {
                long[] divisor = randomBenchmarkMag(dLen, rng);
                long[] dividend = randomBenchmarkMag(nLen, rng);

                // Make sure dividend >= divisor.
                dividend[nLen - 1] |= 1L << 63;
                divisor[dLen - 1] |= 1L << 63;

                // Roughly constant amount of Knuth work per test point.
                int reps = Math.clamp(
                        4_000_000L / ((long)nLen * dLen),
                        2,
                        2000
                );

                // warmup
                for (int i = 0; i < Math.min(50, reps); i++) {
                    sink ^= LargeInteger.modMagnitudesKnuth(dividend, divisor)[0];
                    sink ^= LargeInteger.modMagnitudesBurnikelZiegler(dividend, divisor)[0];
                }

                // correctness once, outside timing
                long[] rk = LargeInteger.modMagnitudesKnuth(dividend, divisor);
                long[] rb = LargeInteger.modMagnitudesBurnikelZiegler(dividend, divisor);

                if (!Arrays.equals(rk, rb))
                    throw new AssertionError(
                            "Remainder disagreement at " +
                                    nLen + " / " + dLen
                    );

                long t0 = System.nanoTime();
                for (int i = 0; i < reps; i++)
                    sink ^= LargeInteger.modMagnitudesKnuth(dividend, divisor)[0];
                long knuth = System.nanoTime() - t0;

                t0 = System.nanoTime();
                for (int i = 0; i < reps; i++)
                    sink ^= LargeInteger.modMagnitudesBurnikelZiegler(dividend, divisor)[0];
                long bz = System.nanoTime() - t0;

                double kNs = (double)knuth / reps;
                double bNs = (double)bz / reps;

                System.out.printf(
                        "%6d %6d %8d %12.0f %12.0f %9.3f%n",
                        dLen,
                        nLen,
                        reps,
                        kNs,
                        bNs,
                        bNs / kNs
                );
            }
        }
        // Keep HotSpot from deciding results don't matter.
        if (sink == 0x123456789ABCDEFL)
            System.out.println(sink);
    }

    private static long[] randomBenchmarkMag(
            int limbs,
            SplittableRandom rng
    ) {
        long[] x = new long[limbs];

        for (int i = 0; i < limbs; i++)
            x[i] = rng.nextLong();

        // Guarantee exact requested limb length.
        x[limbs - 1] |= 1L << 63;

        return x;
    }
    static void testMultiply(){
        int BI_Wins = 0;
        int LI_Wins = 0;
        double[] ratios = new double[15001];
        for (int k = 1; k < 15001; k ++) {
            int trials = 15001-k;
            LargeInteger A = LargeInteger.randomChunks(k);
            LargeInteger B = LargeInteger.randomChunks(k);
            BigInteger BA = A.toBigInteger();
            BigInteger BB = B.toBigInteger();

            LargeInteger LResult = A.square();
            BigInteger BResult = BA.multiply(BA);

            if (!LResult.toBigInteger().equals(BResult))
                throw new AssertionError("Product is wrong");
            // Warmup
            for (int i = 0; i < trials; i++) {
                sink = A.multiply(B);
                sink = BA.multiply(BB);
            }
            // Measure LI
            long t0 = System.nanoTime();
            for (int i = 0; i < trials; i++)
                sink = A.multiply(A);
            long t1 = System.nanoTime();
            // Measure BI
            for (int i = 0; i < trials; i++)
                sink = BA.multiply(BB);
            long t2 = System.nanoTime();
            System.out.println("Chunks tried: " + k);
            System.out.println("Avg. LargeInteger time: " + (t1 - t0) / (1e9 * trials) + " seconds/trial");
            System.out.println("Avg. BigInteger time: " + (t2 - t1) / (1e9 * trials) + " seconds/trial");
            double ratio = (t1 - t0) * 1.0 / (t2 - t1);
            if(ratio < 1.0) LI_Wins++;
            else BI_Wins++;
            System.out.println("Ratio: " + ratio);
            System.out.println();
            ratios[k/5] = ratio;
        }
        System.out.println("Times LargeInteger won: " + LI_Wins);
        System.out.println("Times BigInteger won: " + BI_Wins);
        Arrays.sort(ratios);
        System.out.println("Median ratio: " + ratios[ratios.length/2]);
    }
    static void testPrimeExponents(){
        long startTime = System.nanoTime();
        for (int i: LargeInteger.sieve(10000)) {
            LargeInteger n = LargeInteger.TWO.power(i).numerator.subtract(1);
            BigInteger b = n.toBigInteger();
            for(int k = 0; k < 10000-i; k++){
                sink = n.isProbablyPrime();
                sink = b.isProbablePrime(100);
            }

            long t1 = System.nanoTime();
            boolean li = n.isProbablyPrime();
            long t2 = System.nanoTime();
            boolean bi = b.isProbablePrime(100);
            long t3 = System.nanoTime();

            System.out.printf(
                    "p=%d limbs=%d LI=%.6fms BI=%.6fms ",
                    i,
                    i/64,
                    (t2 - t1) / 1e6,
                    (t3 - t2) / 1e6
            );

            if (li != bi) {
                System.out.println("Mismatch:");
                System.out.println(n);
                System.out.println("LI = " + li);
                System.out.println("BI = " + bi);
                throw new AssertionError();
            }
            System.out.println("Prime? " + li);
        }
        long elapsedTime = System.nanoTime() - startTime;
        System.out.println("Elapsed time: " + elapsedTime/1e9 + " seconds");
    }
    static void benchmarkPowMod(){
        for(int k: new int[]{1, 2, 3, 4, 5, 6, 8, 10, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256, 384, 512, 768, 1024, 1536, 2000}){
            long LI_time = 0;
            long BI_time = 0;
            int trials = 4000/k;
            for(int j = 0; j < 100; j++){
                LargeInteger base = LargeInteger.randomChunks(10);
                LargeInteger exponent = LargeInteger.basePower(k);
                LargeInteger modulus = LargeInteger.randomChunks(10).or(LargeInteger.ONE);
                BigInteger bigBase = base.toBigInteger();
                BigInteger bigExponent = exponent.toBigInteger();
                BigInteger bigModulus = modulus.toBigInteger();
                if(!base.powMod(exponent, modulus).toBigInteger().equals(bigBase.modPow(bigExponent, bigModulus)))
                    throw new AssertionError("PowMod is wrong");
                //warmup
                for(int i = 0; i < 20; i++){
                    sink = base.powMod(exponent, modulus);
                    sink = bigBase.modPow(bigExponent, bigModulus);
                }
                //benchmark
                long t0 = System.nanoTime();
                for(int i = 0; i < trials; i++)
                    sink = base.powMod(exponent, modulus);
                long t1 = System.nanoTime();
                for(int i = 0; i < trials; i++)
                    sink = bigBase.modPow(bigExponent, bigModulus);
                long t2 = System.nanoTime();
                LI_time += t1 - t0;
                BI_time += t2 - t1;
            }
            System.out.println("Chunks tried: " + k);
            System.out.println("Average LI time: " + LI_time/1e9/(100.0*trials) + " seconds/trial");
            System.out.println("Average BI time: " + BI_time/1e9/(100.0*trials) + " seconds/trial");
            System.out.println("LI/BI = " + LI_time*1.0/BI_time);
            System.out.println();
        }
    }
    static void main() {
        long startTime = System.nanoTime();
        System.out.println(new LargeInteger("1056789109669864471763593655329859336296915214623458801219226050139").quadraticSieveWithoutConstraints());
        long elapsedTime = System.nanoTime() - startTime;
        System.out.println("Elapsed time: " + elapsedTime/1e9 + " seconds");
    }
    private static void demoIntegers() {
        System.out.println("=== LargeInteger Demo ===");

        LargeInteger a = LargeInteger.valueOf(1234567890L);
        LargeInteger b = LargeInteger.valueOf(9876543210L);
        LargeInteger c = LargeInteger.valueOf(1000L);

        System.out.println("a = " + a);
        System.out.println("b = " + b);
        System.out.println("c = " + c);
        System.out.println("a + b = " + a.add(b));
        System.out.println("a * b = " + a.multiply(b));
        System.out.println("a - c = " + a.subtract(c));
        System.out.println("a^2 = " + a.square());
        System.out.println("b^3 = " + b.cube());
        System.out.println("c^3 = " + c.cube());
        System.out.println("gcd(a, b) = " + a.gcd(b));
        System.out.println("lcm(a, b) = " + a.lcm(b));
        System.out.println("factor(-a*b+c) = " + ((LargeInteger)a.negate().multiply(b).add(c)).factor());
        System.out.println("totient(a+b+c) = " + ((LargeInteger)a.add(b).add(c)).totient());
        System.out.println();
    }

    private static void demoFractions() {
        System.out.println("=== LargeFraction Demo ===");

        LargeFraction f = new LargeFraction(LargeInteger.valueOf(6), LargeInteger.valueOf(8));
        LargeFraction g = new LargeFraction(LargeInteger.valueOf(5), LargeInteger.valueOf(120));

        System.out.println("6/8 simplifies to " + f);
        System.out.println("0.041{6} = " + g);
        System.out.println("6/8 + 5/120 = " + f.add(g));
        System.out.println("6/8 * 5/120 = " + f.multiply(g));
        System.out.println("6/8 / 5/120 = " + f.divide(g));
        System.out.println("1/5 + 1/2 = " + Parser.parse("1/5 + 1/2"));
        System.out.println();
    }

    private static void demoRadicals() {
        System.out.println("=== Radical / Power Demo ===");

        System.out.println("√72 = " + Parser.parse("√72"));
        System.out.println("√8 + √18 = " + Parser.parse("√8 + √18"));
        System.out.println("(√2 + 1)^2 = " + Parser.parse("(√2+1)^2"));
        System.out.println("(√8 - √98)/(√3 + √5) = " + Parser.parse("(√8 - √98)/(√3 + √5)"));
        System.out.println("1/(√3 + 6^(1/3)) = " + Parser.parse("1/(√3 + 6^(1/3))"));
        System.out.println("47*(2+√3+√5)/(√6+√7+√2) = " + Parser.parse("47*(2+√3+√5)/(√6+√7+√2)"));
        System.out.println();
    }
    private static void demoRadicalReciprocals(){
        System.out.println("=== Advanced reciprocal Demo ===");
        // Rational-only
        testReciprocal("2");
        testReciprocal("-3/7");
        testReciprocal("0.125");

        // Single radical term
        testReciprocal("√2");
        testReciprocal("3√5");
        testReciprocal("-7√11");
        testReciprocal("4*108^(-1/3)");

        // Binomial radical sums
        testReciprocal("1+√2");
        testReciprocal("3-√5");
        testReciprocal("2+3√7");
        testReciprocal("4-3*625^(-1/3)");

        // Multi-radical sums
        testReciprocal("1+√2+√3");
        testReciprocal("5+2√2-3√3");
        testReciprocal("√2+√3+√5");
        testReciprocal("3^(1/3) - 17^(1/3) + 11^(1/3)");

        // Products that should simplify after reciprocal
        testReciprocal("(1+√2)(3-√2)");
        testReciprocal("(√2+√3)(√5-1)");

        //Hidden generators
        testReciprocal("√10 - √2 + √15 - √3");
        testReciprocal("√6 + √10 + √15");
        testReciprocal("√14 - √2 + √21 - √3");
        testReciprocal("√30 + √6 - √10 - √2");

        //Basis size possibly too large
        testReciprocal("2^(1/3) + 4^(1/3)");
        testReciprocal("8^(1/3) + 2^(1/3)");
        testReciprocal("16^(1/4) - 2^(1/2)");
        testReciprocal("27^(1/6) + 3^(1/3)");
        testReciprocal("-5^(99/100) + 5");

        // Powers / fractional exponents
        testReciprocal("2^(1/3)+1");
        testReciprocal("3^(1/2)+5^(1/2)");
        testReciprocal("5-7*10^(1/7)");
        testReciprocal("60 - 60*60^(1/60)");
        testReciprocal("2/47 + 97*83^(12/13)");
        testReciprocal("7/443 + 53*(89)^(16/17)");

        // Complex-principal-root cases
        testReciprocal("√-1");
        testReciprocal("1+√-1");
        testReciprocal("(-1)^(1/3) - (-1)^(1/4)");
        testReciprocal("(-1)^(1/5) - (-1)^(4/5)");
        System.out.println();
    }
    private static void demoVectorsMatrices() {
        System.out.println("=== Vector / Matrix Demo ===");

        LargeList<LargeFraction> entries = new LargeList<>();
        entries.add(new LargeFraction(1));
        entries.add(new LargeFraction(2));
        entries.add(new LargeFraction(3));
        entries.add(new LargeFraction(4));
        entries.add(new LargeFraction(5));

        LargeVector v = new LargeVector(entries);

        System.out.println("v = " + v);
        System.out.println("v · v = " + v.dotProduct(v));
        System.out.println("|v| = " + v.magnitude());
        System.out.println();
    }
    private static void demoParser() {
        System.out.println("=== Parser Demo ===");

        String[] inputs = {
                "2+3*4",
                "(2+3)^4",
                "0.{142857}",
                "50%",
                "√50",
                "3√12 + 2√27",
                "71!",
                "√(5+2√6)",
                "(1+√7)^8"
        };
        for (String input : inputs) {
            try {
                System.out.println(input + " -> " + Parser.parse(input));
            } catch (Exception e) {
                System.out.println(input + " -> ERROR: " + e.getMessage());
            }
        }
        System.out.println();
    }

    private static void demoBaseConversion() {
        System.out.println("=== Base Conversion Demo ===");
        System.out.println("23/61 in base 10 = " + new Base(new LargeFraction(LargeInteger.valueOf(23), LargeInteger.valueOf(61)), 10));
        System.out.println("17495/17496 in base 3 = " + new Base((LargeFraction) Parser.parse("17495/17496"), 3));
        System.out.println("15/97 in base 2 = " + new Base(new LargeFraction(LargeInteger.valueOf(15), LargeInteger.valueOf(97)), 2));
        System.out.println("1/131 in base 16 = " + new Base(new LargeFraction(LargeInteger.valueOf(1), LargeInteger.valueOf(131)), 16));
        System.out.println("51/37 in base 26 = " + new Base(new LargeFraction(LargeInteger.valueOf(51), LargeInteger.valueOf(37)), 26));
        System.out.println("19/64 in base 53 = " + new Base(new LargeFraction(LargeInteger.valueOf(19), LargeInteger.valueOf(64)), 53));
        System.out.println("-213781471834746879182637586423 in base 7 = " + new Base(new LargeFraction("-213781471834746879182637586423"), 7));
        Base B = new Base(new LargeFraction(LargeInteger.valueOf(117), LargeInteger.valueOf(107)), 89);
        System.out.println("117/107 in base 89 = " + B);
        System.out.println("117/107 in base 43 = " + B.convertToBase(43));
        System.out.println("759/432 in base 19 = " + new Base(new LargeFraction(LargeInteger.valueOf(759), LargeInteger.valueOf(432)), 19));
        System.out.println("61/19 in base 41 = " + new Base(new LargeFraction(LargeInteger.valueOf(61), LargeInteger.valueOf(19)), 41));
        System.out.println("71/109 in base 38 = " + new Base(new LargeFraction(LargeInteger.valueOf(71), LargeInteger.valueOf(109)), 38));
    }
}