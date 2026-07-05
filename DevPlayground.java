package com.AtomCAS;
import com.AtomCAS.utilities.*;

import java.math.BigInteger;
import java.util.Random;

public class DevPlayground {
    private static void testReciprocal(String input) {
        ScalarAtom x = Parser.parse(input);
        System.out.println("1/(" + x + ") = " + x.reciprocal());
    }
    static volatile Object sink;
    public static void main(String[] args){
/*
        let us do demoIntegers();
        demoFractions();
        demoRadicals();
        demoRadicalReciprocals();
        demoVectorsMatrices();
        demoParser();
        demoBaseConversion();
*/
        System.out.println(new LargeInteger("3141592653589793238462643383279502884197169399375105820974944592").multiply(new LargeInteger("2718281828459045235360287471352662497757247093699959574966967627")));
    }
    private static void demoIntegers() {
        System.out.println("=== LargeInteger Demo ===");

        LargeInteger a = new LargeInteger(1234567890L);
        LargeInteger b = new LargeInteger(9876543210L);
        LargeInteger c = new LargeInteger(1000L);

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
        System.out.println("lcm(a, b) = " + a.lcm(a, b));
        System.out.println("factor(-a*b+c) = " + ((LargeInteger)a.negate().multiply(b).add(c)).factor());
        System.out.println("c! = " + c.factorial());
        System.out.println();
    }

    private static void demoFractions() {
        System.out.println("=== LargeFraction Demo ===");

        LargeFraction f = new LargeFraction(new LargeInteger(6), new LargeInteger(8));
        LargeFraction g = new LargeFraction(new LargeInteger(5), new LargeInteger(120));

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
        System.out.println("=== More advanced radical / power Demo ===");
        System.out.println("This section in particular intends to show exact simplification of radical expressions, without decimal approximation.");
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
        testReciprocal("-2^(9/100) + 2");

        // Powers / fractional exponents
        testReciprocal("2^(1/3)+1");
        testReciprocal("3^(1/2)+5^(1/2)");
        testReciprocal("5-7*10^(1/7)");
        testReciprocal("12 - 12*12^(1/12)");
        testReciprocal("2/47 + 97*83^(1/13)");
        testReciprocal("7/443 + 53*(89)^(1/17)");

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
        System.out.println("23/61 in base 10 = " + new Base(new LargeFraction(new LargeInteger(23), new LargeInteger(61)), new LargeInteger(10)));
        System.out.println("17495/17496 in base 3 = " + new Base((LargeFraction) Parser.parse("17495/17496"), new LargeInteger(3)));
        System.out.println("15/97 in base 2 = " + new Base(new LargeFraction(new LargeInteger(15), new LargeInteger(97)), new LargeInteger(2)));
        System.out.println("1/131 in base 16 = " + new Base(new LargeFraction(new LargeInteger(1), new LargeInteger(131)), new LargeInteger(16)));
        System.out.println("51/37 in base 26 = " + new Base(new LargeFraction(new LargeInteger(51), new LargeInteger(37)), new LargeInteger(26)));
        System.out.println("19/64 in base 53 = " + new Base(new LargeFraction(new LargeInteger(19), new LargeInteger(64)), new LargeInteger(53)));
        System.out.println("-213781471834746879182637586423 in base 7 = " + new Base(new LargeFraction("-213781471834746879182637586423"), new LargeInteger(7)));
        System.out.println("117/107 in base 89 = " + new Base(new LargeFraction(new LargeInteger(117), new LargeInteger(107)), new LargeInteger(89)));
        System.out.println("759/432 in base 19 = " + new Base(new LargeFraction(new LargeInteger(759), new LargeInteger(432)), new LargeInteger(19)));
        System.out.println("61/19 in base 41 = " + new Base(new LargeFraction(new LargeInteger(61), new LargeInteger(19)), new LargeInteger(41)));
        System.out.println("71/109 in base 38 = " + new Base(new LargeFraction(new LargeInteger(71), new LargeInteger(109)), new LargeInteger(38)));
    }
}