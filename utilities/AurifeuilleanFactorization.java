package com.AtomCAS.utilities;
public final class AurifeuilleanFactorization {
    private AurifeuilleanFactorization() {}
    public record Factorization(LargeInteger value, LargeInteger factor1, LargeInteger factor2) {
        public boolean isNonTrivial() {return factor1.greaterThan(LargeInteger.ONE) && factor2.greaterThan(LargeInteger.ONE);}
    }
    /**
     * Applies:
     * a^4 + 4b^4
     * = (a^2 - 2ab + 2b^2)(a^2 + 2ab + 2b^2)
     */
    public static Factorization factorFourthPlusFourFourth(LargeInteger a, LargeInteger b) {
        if (a == null || b == null)
            throw new IllegalArgumentException("a and b cannot be null");
        // Fourth powers ignore signs. Normalizing also prevents the TWO
        // factors from swapping when a or b is negative.
        LargeInteger A = a.abs();
        LargeInteger B = b.abs();

        LargeInteger aSquared = A.square();
        LargeInteger bSquared = B.square();

        LargeInteger twoAB = (LargeInteger) A.multiply(B).multiply(LargeInteger.TWO);

        LargeInteger twoBSquared = bSquared.multiply(2);

        // a^2 + 2b^2
        LargeInteger center = (LargeInteger) aSquared.add(twoBSquared);

        LargeInteger factor1 = (LargeInteger) center.subtract(twoAB);

        LargeInteger factor2 = (LargeInteger) center.add(twoAB);

        LargeInteger value = (LargeInteger) aSquared.square().add(bSquared.square().multiply(4));

        // Defensive verification. This should never fail unless an
        // underlying arithmetic operation is incorrect.
        LargeInteger reconstructed = (LargeInteger) factor1.multiply(factor2);
        if (reconstructed.doesntEqual(value))
            throw new IllegalStateException("Sophie Germain factorization verification failed: " + factor1 + " * " + factor2 + " != " + value);
        return new Factorization(value, factor1, factor2);
    }
    private static void testSophieGermain(String aText, String bText) {
        LargeInteger a = new LargeInteger(aText);
        LargeInteger b = new LargeInteger(bText);
        AurifeuilleanFactorization.Factorization result = AurifeuilleanFactorization.factorFourthPlusFourFourth(a, b);
        System.out.println(result.value() + " = " + result.factor1() + " * " + result.factor2() + ", nontrivial = " + result.isNonTrivial());
    }
    static void main() {
        testSophieGermain("3", "2");
        testSophieGermain("5", "3");
        testSophieGermain("10^76", "1");
        testSophieGermain("-12", "7");
        testSophieGermain("123456789012345678901", "109876543210987654321");
        testSophieGermain("63", "7^9");
    }
}
