package com.AtomCAS.utilities;

public final class CASHash {
    private CASHash(){}
    static int mix(int h, int... xs) {
        for (int x : xs) {
            h ^= x;
            h *= 0x85ebca6b;
            h ^= h >>> 13;
        }
        return h;
    }
    static int finish(int h) {
        h ^= h >>> 16;
        h *= 0x7feb352d;
        h ^= h >>> 15;
        h *= 0x846ca68b;
        h ^= h >>> 16;
        return h;
    }
    static int hash(Atom a) {
        return switch (a) {
            case LargeInteger x       -> integer(x);
            case LargeFraction x      -> rational(x.numerator, x.denominator);
            case LargeDecimal x       -> decimal(x);
            case LargePowerMonomial x -> powerMonomial(x);
            case RadicalTerm x        -> radicalTerm(x);
            case RadicalTermSum x     -> radicalTermSum(x);
            case LargePowerNode x     -> powerNode(x);
            case Exp x                -> exp(x);
            case Logarithm x          -> logarithm(x);
            case LargeVector x        -> vector(x);
            case LargeMatrix x        -> matrix(x);
            case RadicalExpression x  -> radicalExpression(x);
            case AlgebraicNumber x    -> algebraicNumber(x);

            default -> throw new UnsupportedOperationException("Hashing not implemented for " + a.getClass());
        };
    }
    static int integer(LargeInteger x) {
        int h = 0;
        for (int i = x.limbs.length - 1; i >= 0; i--)
            h = 31 * h + Long.hashCode(x.limbs[i]);
        return x.negative ? -h : h;
    }
    static int rational(LargeInteger numerator, LargeInteger denominator){
        if (denominator.equals(LargeInteger.ONE))
            return integer(numerator);
        return mix(integer(numerator), integer(denominator));
    }
    static int powerMonomial(LargePowerMonomial x) {
        throw new UnsupportedOperationException("LargePowerMonomial");
//        if (x.isRational()) {
//            LargeFraction frac = x.exponent2.equals(LargeInteger.ONE) ? x.base.negate() : x.base;
//            return rational(frac.numerator, frac.denominator);
//        }
//        return mix(TAG_POWER_MONOMIAL, x.base.hashCode(), x.exponent.hashCode(), x.exponent2.hashCode());
    }
    static int decimal(LargeDecimal x) {
        throw new UnsupportedOperationException("LargeDecimal");
//        if(x.decPart.isEmpty())
//            return x.intPart.hashCode();
    }
    static int powerNode(LargePowerNode x) {
        throw new UnsupportedOperationException("LargePowerNode");
    }
    static int radicalTerm(RadicalTerm x){
        throw new UnsupportedOperationException("RadicalTerm");
    }
    static int radicalTermSum(RadicalTermSum x){
        throw new UnsupportedOperationException("RadicalTermSum");
    }
    static int logarithm(Logarithm x) {
        throw new UnsupportedOperationException("Logarithm");
//        return mix(TAG_LOG, x.argument.hashCode());
    }
    static int vector(LargeVector x) {
        throw new UnsupportedOperationException("LargeVector");
//        int h = TAG_VECTOR;
//        h = mix(h, x.dimension().hashCode());
//        for (LargeInteger i = LargeInteger.ZERO;
//             i.lessThan(x.dimension());
//             i = i.add(1)) {
//            h = mix(h, x.get(i).hashCode());
//        }
//        return finish(h);
    }
    static int matrix(LargeMatrix x) {
        throw new UnsupportedOperationException("LargeMatrix");
//        int h = TAG_MATRIX;
//        h = mix(h, x.rows().hashCode());
//        h = mix(h, x.cols().hashCode());
//        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(x.rows()); i = i.add(1))
//            for (LargeInteger j = LargeInteger.ZERO; j.lessThan(x.cols()); j = j.add(1))
//                h = mix(h, x.get(i, j).hashCode());
//        return finish(h);
    }
    static int radicalExpression(RadicalExpression x){
        throw new UnsupportedOperationException("RadicalExpression");
    }
    static int algebraicNumber(AlgebraicNumber x) {
        throw new UnsupportedOperationException("AlgebraicNumber");
    }
    static int exp(Exp x) {
        throw new UnsupportedOperationException("Exp");
//        if (x.coefficient.equals(LargeInteger.ZERO))
//            return x.addend.hashCode();
//        return mix(TAG_EXP, x.addend.hashCode(), x.coefficient.hashCode(), x.power.hashCode());
    }
}
