package com.AtomCAS;
import com.AtomCAS.utilities.*;
import java.math.BigInteger;
import java.util.Arrays;

public class DevPlayground {
    private static void testReciprocal(String input) {
        ScalarAtom x = Parser.parse(input);
        System.out.println("1/(" + x + ") = " + x.reciprocal());
    }
    static volatile Object sink = new Object();
    static void benchmarkSquare(){
        int[] karatsubaThresholds = {32, 48, 64, 96, 128, 192, 256, 384, 512, 768, 1024, 1536, 2048, 3072, 4096};
        int[] toomThresholds = {128, 192, 256, 384, 512, 768, 1024, 1536, 2048, 3072, 4096, 8192, 16384, 32768, 65536, 131072, Integer.MAX_VALUE};
        int[] chunkSizes = {32, 48, 64, 96, 128, 192, 256, 384, 512, 768, 1024, 1536, 2048, 3072, 4096, 6144, 8192, 12288,
                16384, 24576, 32768, 49152, 65536};
        for (int chunkSize : chunkSizes) {
            System.out.println("Chunk size tried: " + chunkSize);
            for(int karatsubaThreshold : karatsubaThresholds)
                System.out.printf("%10d", karatsubaThreshold);
            System.out.println();
            long[] A = LargeInteger.randomChunks(chunkSize).limbs;
            int trials = 131072/chunkSize;
            for (int toom : toomThresholds) {
                if(toom > 2*chunkSize) continue;
                System.out.printf("%-8d", toom);
                for (int karatsuba : karatsubaThresholds) {
                    if (karatsuba >= toom) continue;
                    for(int i = 0; i < trials; i++)
                        LargeInteger.squareMagnitudeWithThresholds(A, karatsuba, toom);
                    long start = System.nanoTime();
                    for (int i = 0; i < trials; i++)
                        LargeInteger.squareMagnitudeWithThresholds(A, karatsuba, toom);
                    long end = System.nanoTime();
                    long duration = end - start;
                    System.out.printf("%-10.3f",  duration/1e9);
                }
                System.out.println();
            }
            System.out.println();
        }
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
    static void main() {
//        long start = System.nanoTime();
//        demoIntegers();
//        demoFractions();
//        demoRadicals();
//        demoRadicalReciprocals();
//        demoVectorsMatrices();
//        demoParser();
//        demoBaseConversion();
//        long end = System.nanoTime();
//        System.out.println("Elapsed time: " + (end - start)/1e9 + " seconds");
        long t0 = System.nanoTime();
        System.out.println(new LargeInteger("2451955667358449766574244930742726059038594502086245442706362842197225471").quadraticSieveWithoutConstraints());
        long t1 = System.nanoTime();
        System.out.println((t1-t0)/1e9 + " seconds");
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
        System.out.println("This section in particular intends to show exact simplification of reciprocals of several expressions");
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
            try {System.out.println(input + " -> " + Parser.parse(input));}
            catch (Exception e) {System.out.println(input + " -> ERROR: " + e.getMessage());}
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