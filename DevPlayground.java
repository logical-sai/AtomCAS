package com.AtomCAS;
import com.AtomCAS.utilities.*;
import java.math.BigInteger;
import java.util.*;

class DevPlayground {
    public static void testReciprocal(String input) {
        ScalarAtom x = Parser.parse(input);
        ScalarAtom y = x.reciprocal();
        if(x.multiply(y).doesntEqual(LargeInteger.ONE))
            throw new AssertionError("Reciprocal of " + x + " is wrong");
        System.out.println("1/(" + x + ") = " + y);
    }
    public static volatile Object sink = new Object();
    public static void testMultiply() {
        int BI_Wins = 0;
        int LI_Wins = 0;
        int[] sizes = {1, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256, 384, 512, 768, 1024, 1536,
                2048, 3072, 4096, 6144, 8192, 12288, 16384, 24576, 32768, 49152, 65536, 98304, 131072, 196608,
                262144, 393216, 524288, 786432, 1048576};
        int pairs = 10;
        double[] ratios = new double[sizes.length*pairs];
        for (int k = 0; k < sizes.length; k++) {
            int trials = (3 << 22)/sizes[k];
            System.out.println("Bits tried: " + sizes[k] + " with " + pairs + " pairs and " + trials + " trials per pair");
            double[] sizeRatios = new double[pairs];
            for (int l = 0; l < pairs; l++) {
                LargeInteger A = LargeInteger.randomBits(sizes[k]);
                LargeInteger B = LargeInteger.randomBits(sizes[k]);
                BigInteger BA = A.toBigInteger();
                BigInteger BB = B.toBigInteger();
                LargeInteger LResult = A.multiply(B);
                BigInteger BResult = BA.multiply(BB);
                if (!LResult.toBigInteger().equals(BResult))
                    throw new AssertionError("Product is wrong");
                long liTime;
                long biTime;
                long t0;
                long t1;
                if (((k * pairs + l) & 1) == 1){
                    for (int i = 0; i < trials; i++) {
                        sink = A.multiply(B);
                        sink = BA.multiply(BB);
                    }
                    t0 = System.nanoTime();
                    for (int i = 0; i < trials; i++)
                        sink = A.multiply(B);
                    t1 = System.nanoTime();
                    liTime = t1 - t0;

                    t0 = System.nanoTime();
                    for (int i = 0; i < trials; i++)
                        sink = BA.multiply(BB);
                    t1 = System.nanoTime();
                    biTime = t1 - t0;

                } else {
                    for (int i = 0; i < trials; i++) {
                        sink = BA.multiply(BB);
                        sink = A.multiply(B);
                    }
                    t0 = System.nanoTime();
                    for (int i = 0; i < trials; i++)
                        sink = BA.multiply(BB);
                    t1 = System.nanoTime();
                    biTime = t1 - t0;
                    t0 = System.nanoTime();
                    for (int i = 0; i < trials; i++)
                        sink = A.multiply(B);
                    t1 = System.nanoTime();
                    liTime = t1 - t0;
                }
                double ratio = (double) liTime / biTime;
                if (ratio < 1.0) LI_Wins++;
                else BI_Wins++;
                System.out.println("LargeInteger time: " + liTime / (1e9 * trials) + " seconds/trial");
                System.out.println("BigInteger time: " + biTime / (1e9 * trials) + " seconds/trial");
                System.out.println("Ratio: " + ratio);
                sizeRatios[l] = ratio;
                ratios[k * pairs + l] = ratio;
            }
            Arrays.sort(sizeRatios);
            System.out.println("Median for size: " + (sizeRatios[sizeRatios.length/2-1] + sizeRatios[sizeRatios.length/2])/2.0);
            System.out.println();
        }
        System.out.println("Times LargeInteger won: " + LI_Wins);
        System.out.println("Times BigInteger won: " + BI_Wins);
        Arrays.sort(ratios);
        System.out.println("Median ratio: " + (ratios[ratios.length/2-1] + ratios[ratios.length/2])/2.0);
    }
    public static void testPrimeExponents(){
        long startTime = System.nanoTime();
        for (int i: LargeInteger.sieve(10000)) {
            LargeInteger n = LargeInteger.powerOfTwo(i).subtract(LargeInteger.ONE);
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

            System.out.printf("p=%d limbs=%d LI=%.6fms BI=%.6fms ", i, i/64, (t2 - t1) / 1e6, (t3 - t2) / 1e6);
            if (li != bi)
                throw new AssertionError("Mismatch on " + n + "\nLI = " + li + " \nBI = " + bi);
            System.out.println("Prime? " + li);
        }
        long elapsedTime = System.nanoTime() - startTime;
        System.out.println("Elapsed time: " + elapsedTime/1e9 + " seconds");
    }
    public static void testPowMod(){
        for(int k: new int[]{32, 48, 64, 80, 96, 112, 128, 144, 160, 176, 192, 208, 224}){
            long LI_time = 0;
            long BI_time = 0;
            int trials = 49152/k;
            for(int j = 0; j < 100; j++){
                LargeInteger base = LargeInteger.randomBits(k);
                LargeInteger exponent = LargeInteger.powerOfTwo(k).subtract(LargeInteger.ONE);
                LargeInteger modulus = LargeInteger.randomBits(k).or(LargeInteger.ONE);
                BigInteger bigBase = base.toBigInteger();
                BigInteger bigExponent = exponent.toBigInteger();
                BigInteger bigModulus = modulus.toBigInteger();
                if(!base.powMod(exponent, modulus).toBigInteger().equals(bigBase.modPow(bigExponent, bigModulus)))
                    throw new AssertionError("PowMod for base=" + base + ", exponent=" + exponent
                            + ", and modulus=" + modulus + " is wrong");
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
            System.out.println("Bits tested: " + k);
            System.out.println("Average LI time: " + LI_time/1e9/(100*trials) + " seconds/trial");
            System.out.println("Average BI time: " + BI_time/1e9/(100*trials) + " seconds/trial");
            System.out.println("LI/BI = " + LI_time*1.0/BI_time);
            System.out.println();
        }
    }
    public static void testToString() {
        for (int size : new int[]{1, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256, 384, 512, 768, 1024, 1536, 2048}) {
            long LI_time = 0;
            long BI_time = 0;
            int trials;
            if (size <= 16)        trials = 5000;
            else if (size <= 64)   trials = 2000;
            else if (size <= 256)  trials = 500;
            else if (size <= 512)  trials = 200;
            else if (size <= 1024) trials = 50;
            else                   trials = 20;
            System.out.println("Chunk size: " + size);
            for (int k = 0; k < 100; k++) {
                LargeInteger L5 = LargeInteger.randomChunks(size);
                LargeInteger L6 = LargeInteger.randomChunks(size);
                if(L5.lessThan(L6)){
                    LargeInteger temp =  L5;
                    L5 = L6;
                    L6 = temp;
                }
                BigInteger B5 = L5.toBigInteger();
                BigInteger B6 = L6.toBigInteger();
                if(!L5.remainder(L6).toBigInteger().equals(B5.remainder(B6)))
                    throw new AssertionError("toString is wrong");
                if ((k & 1) == 1) {
                    for (int i = 0; i < trials / 2; i++) {
                        sink = L5.toString();
                        sink = B5.toString();
                    }
                    long t0 = System.nanoTime();
                    for (int i = 0; i < trials; i++)
                        sink = L5.toString();
                    long t1 = System.nanoTime();
                    for (int i = 0; i < trials; i++)
                        sink = B5.toString();
                    long t2 = System.nanoTime();
                    LI_time += t1 - t0;
                    BI_time += t2 - t1;
                } else {
                    for (int i = 0; i < trials / 2; i++) {
                        sink = B5.toString();
                        sink = L5.toString();
                    }
                    long t0 = System.nanoTime();
                    for (int i = 0; i < trials; i++)
                        sink = B5.toString();
                    long t1 = System.nanoTime();
                    for (int i = 0; i < trials; i++)
                        sink = L5.toString();
                    long t2 = System.nanoTime();
                    LI_time += t2 - t1;
                    BI_time += t1 - t0;
                }
            }
            System.out.println("LI time: " + LI_time / 1e9 / (trials * 100) + " seconds/trial");
            System.out.println("BI time: " + BI_time / 1e9 / (trials * 100) + " seconds/trial");
            System.out.println("Ratio: " + LI_time * 1.0 / BI_time);
            System.out.println();
        }
    }
    public static void testGCD(){
        int trials = 150;
        int total = 101;
        long[] LI_times = new long[total];
        long[] BI_times = new long[total];
        for (int j = 0; j < total; j++) {
            LargeInteger A = LargeInteger.randomChunks(150);
            LargeInteger B = LargeInteger.randomChunks(75);
            BigInteger BA = A.toBigInteger();
            BigInteger BB = B.toBigInteger();
            if(!A.gcd(B).toBigInteger().equals(BA.gcd(BB)))
                throw new AssertionError("gcd is wrong");
            //warmup
            for (int k = 0; k < trials/2; k++) {
                sink = A.gcd(B);
                sink = BA.gcd(BB);
            }
            long t0 = System.nanoTime();
            if ((j & 1) == 0) {
                for (int i = 0; i < trials; i++)
                    sink = A.gcd(B);
                long t1 = System.nanoTime();
                for (int i = 0; i < trials; i++)
                    sink = BA.gcd(BB);
                long t2 = System.nanoTime();
                LI_times[j] = t1 - t0;
                BI_times[j] = t2 - t1;
            } else {
                for (int i = 0; i < trials; i++)
                    sink = BA.gcd(BB);
                long t1 = System.nanoTime();
                for (int i = 0; i < trials; i++)
                    sink = A.gcd(B);
                long t2 = System.nanoTime();
                BI_times[j] = t1 - t0;
                LI_times[j] = t2 - t1;
            }
        }
        double[] ratios = new double[total];
        for (int j = 0; j < total; j++)
            ratios[j] = (double) LI_times[j] / BI_times[j];
        Arrays.sort(ratios);
        Arrays.sort(LI_times);
        Arrays.sort(BI_times);
        System.out.println("LI median: " + LI_times[LI_times.length / 2]/1e9/trials + " seconds/trial");
        System.out.println("BI median: " + BI_times[BI_times.length / 2]/1e9/trials + " seconds/trial");
        System.out.println("Median LI/BI ratio: " + ratios[total / 2]);
    }

    record FactorBoss(String value, String origin, String notes) {}
    @SuppressWarnings("unused")
    static final List<FactorBoss> ECMFactorBosses = List.of(
            new FactorBoss(
                    "17329683219061543002422421228779516108730216118351629631525988433124595828881651243776644793221812900676299259681",
                    "113-digit factor of 10^280+1",
                    "It has P50 ;-;"),
            new FactorBoss(
                    "95494137605894520631622200650595911876626195592470868465557173785682624796482816755939080254946071115981326539067",
                    "113-digit factor of 10^151-1",
                    "IT'S P56 * P58 GODDAMMIT"),
            new FactorBoss(
                    "2545241670696632645269668355010308228766321362213342156837791748326503601516964035735193056580722339586143704344727531879151925475323882002596146504110565298175061722110514393341647789457609",
                    "190-digit factor of 10^194+1",
                    "IT HAS A P71...*despair*"),
            new FactorBoss(
                    "453006805906714884411502853326668790794086913203640988280372355951493593240466603283669916463694317597801206598870997639207",
                    "123-digit factor of 10^149+1",
                    "A P48..."),
            new FactorBoss(
                    "65501941944313206950705851760705669113582631270934567573786629910626991261428869184178218403813518055772508847597858150001",
                    "122-digit factor of 10^300+1",
                    "P49 now. not going for that"),
            new FactorBoss(
                    "300932863707222380064047767986506993480747134074482327493193906987568752931846264493277211892465666521176460891605373427957891512801",
                    "132-digit factor of 10^370+1",
                    "\"insanity is doing the same thing over and over and expecting different results\" -this BALANCED P66*P67"),
            new FactorBoss(
                    "1542640683424132399232471221044907051052572151832476120359456130667480485184999280119584639349554309571691938515526407159478857314501",
                    "133-digit factor of 10^182+1",
                    "A P64 * P69..."),
            new FactorBoss(
                    "192765138747618664658708619609149325554035237425929935887315879260862972540025154612659873734596956586713945640750799208135282412056415351",
                    "138-digit factor of 10^163-1",
                    "P63*P75. These numbers are WAY too hard"),
            new FactorBoss(
                    "4507841767620154777691376885052292431233465072658754301801002791829306068995480121896964468514459503528632140714078303",
                    "118-digit factor of 10^167+1",
                    "If I see another P57 I am going to say NO"),
            new FactorBoss(
                    "45032210066506069256826272734972918887334048441151951782829437603679673895864323327927303771353309461952652208440573593921168571395063494292741911733731775383",
                    "158-digit factor of 10^181+1",
                    "do NOT even attempt this P74*P84 you will heavily regret it"),
            new FactorBoss(
                    "99999999000000009999999900000000999999990000000099999999000000009999999900000000999999990000000099999999000000009999999900000000999999990000000099999999000000009999999900000001",
                    "176-digit factor of 10^184+1",
                    "A P66 in a raw cyclotomic result. I do not want to be the next B. Dodson"),
            new FactorBoss(
                    "84523708900354999577381456343504352971092891556081489307750824107007015467838813287127039142929591750486856563265996196433099484413828078776097540360071",
                    "152-digit factor of 10^169-1",
                    "A perfect-square exponent does not guarantee ease. Sometimes it can yield P49"),
            new FactorBoss(
                    "1496558280854452426177935255386940584258573648331944343019881625583325531570112963563951422005189461786234963804668662694845539823312397215285049",
                    "145-digit factor of 10^169+1",
                    "This guy and his twin say the same thing. Except this guy says P55"),
            new FactorBoss(
                    "342594358689357800535632642961144086146356092203500784425797707669595872827197291891165896611202348682216686730018255401",
                    "120-digit factor of 10^158+1",
                    "A P51 after all this time"),
            new FactorBoss(
                    "115803748393662249221904430519040355618527394268823242649175806830408102188348254509315050886227655112116740491407543143760599096075587645629094643688352623",
                    "156-digit factor of 10^191+1",
                    "IT HAS A P76...AAAAAAAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGGGGGGGHHHHHHHHHHHHHH"),
            new FactorBoss(
                    "18946045266864079447749952129297405359583272502326386935207566955946398049786420518369090889686059502391624781043612268906774013174683868338923394524031201737",
                    "158-digit factor of 10^196+1",
                    "These damn guys with their P64s and whatnot are saying 'you will not complete this triangle with your frugal methods'"),
            new FactorBoss(
                    "257151137227669163459078372107635123438302660448129776040255217899013443107781377045296816007267173658585082212816158474903682305884153564617",
                    "141-digit factor of 10^212+1",
                    "I'm just going through the triangle because ye. P65s can wait"),
            new FactorBoss(
                    "497589584370643651833411052136403857469855512021216681367094246340846264306841359259739399116532272051367234127695381400591250742929694843555904520509603702962028539798720834254409924410330623440892183",
                    "201-digit factor of 10^223+1",
                    "P. seventy. three."),
            new FactorBoss(
                    "14188188836968705250117711741499944698487714862142939263488190699603257680334448968531423820453615812952863324879504234349910697001471647658310156271402955153602671554331811547256147592874775035781",
                    "197-digit factor of 10^226+1",
                    "STOP. YOUR. DAMN. WASTE. OF. TIME. especially when one target has a F##KING P80"),
            new FactorBoss(
                    "392341376282551597853225488244320429407492675293824468308234708890386715143464851563136496549187240385763178553846210338740834833642810743816248430253285047235830732149562248523725576650081305575723632459",
                    "204-digit factor of 10^229+1",
                    "A P41. Then a P59"),
            new FactorBoss(
                    "1098901098901098901098901098901098901098901098901098901098901098901098901098901098901098901098900989010989010989010989010989010989010989010989010989010989010989010989010989010989010989010989011",
                    "193-digit factor of 10^291+1",
                    "P42 this time. My next target is obvious"),
            new FactorBoss(
                    "23745522635916420509212937025036341721288549626793287444171791964097702703756675293617858214186414108961",
                    "104-digit factor 3^370+1",
                    "BALANCED: is P52*P52. Wait until GNFS is complete")
            );

    static void main() {
        LargeInteger N = new LargeInteger("10^258+1");
        System.out.println(N + " = " + N.factor());
    }
    public static void demoIntegers() {
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
        System.out.println("factor(-a*b+c) = " + a.negate().multiply(b).add(c).factor());
        System.out.println("totient(a+b+c) = " + a.add(b).add(c).totient());
        System.out.println();
    }
    public static void demoFractions() {
        System.out.println("=== LargeFraction Demo ===");

        LargeFraction f = new LargeFraction(LargeInteger.valueOf(6), LargeInteger.valueOf(8));
        LargeFraction g = new LargeFraction(LargeInteger.valueOf(5), LargeInteger.valueOf(120));

        System.out.println("6/8 simplifies to " + f);
        System.out.println("0.041{6} = " + g);
        System.out.println("6/8 + 5/120 = " + f.add(g));
        System.out.println("6/8 * 5/120 = " + f.multiply(g));
        System.out.println("6/8 / 5/120 = " + f.divide(g));
        System.out.println("1/5 + 1/2 = " + new LargeFraction("1/5 + 1/2"));
        System.out.println();
    }
    public static void demoRadicals() {
        System.out.println("=== Radical / Power Demo ===");

        System.out.println("√72 = " + Parser.parse("√72"));
        System.out.println("√8 + √18 = " + Parser.parse("√8 + √18"));
        System.out.println("(√2 + 1)^2 = " + Parser.parse("(√2+1)^2"));
        System.out.println("(√8 - √98)/(√3 + √5) = " + Parser.parse("(√8 - √98)/(√3 + √5)"));
        System.out.println("1/(√3 + 6^(1/3)) = " + Parser.parse("1/(√3 + 6^(1/3))"));
        System.out.println("47*(2+√3+√5)/(√6+√7+√2) = " + Parser.parse("47*(2+√3+√5)/(√6+√7+√2)"));
        System.out.println();
    }
    public static void demoRadicalReciprocals(){
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
        testReciprocal("(√2+√3)(√5-7)");

        //Hidden generators
        testReciprocal("√10 - √2 + √15 - √3");
        testReciprocal("√6 + √10 + √15");
        testReciprocal("√14 - √2 + √21 - √3");
        testReciprocal("√30 + √6 - √10 - √2");

        //Basis size possibly too large
        testReciprocal("2^(1/3) + 4^(1/3)");
        testReciprocal("8^(1/3) + 2^(1/3)");
        testReciprocal("16^(1/4) - 2^(1/2)");
        testReciprocal("27^(1/7) + 3^(1/3)");
        testReciprocal("-5^(99/50) + 5");

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
    public static void demoVectorsMatrices() {
        System.out.println("=== Vector / Matrix Demo ===");

        LargeList<LargeFraction> entries = new LargeList<>();
        entries.add(LargeFraction.ONE);
        entries.add(LargeFraction.TWO);
        entries.add(LargeFraction.valueOf(LargeInteger.THREE));
        entries.add(LargeFraction.valueOf(LargeInteger.FOUR));
        entries.add(LargeFraction.valueOf(LargeInteger.FIVE));

        LargeVector v = new LargeVector(entries);

        System.out.println("v = " + v);
        System.out.println("v · v = " + v.dotProduct(v));
        System.out.println("|v| = " + v.magnitude());
        System.out.println();
    }
    public static void demoParser() {
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
        for (String input : inputs)
            try {System.out.println(input + " -> " + Parser.parse(input));}
            catch (Exception e) {System.out.println(input + " -> ERROR: " + e.getMessage());}
        System.out.println();
    }
    public static void demoBaseConversion() {
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