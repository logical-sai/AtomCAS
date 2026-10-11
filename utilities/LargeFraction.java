package com.AtomCAS.utilities;

public final class LargeFraction extends ScalarAtom {
    final LargeInteger numerator;
    final LargeInteger denominator;
    public static final LargeFraction NEGATIVE_ONE = new LargeFraction(LargeInteger.NEGATIVE_ONE);
    public static final LargeFraction ZERO = new LargeFraction(LargeInteger.ZERO);
    public static final LargeFraction HALF = LargeInteger.TWO.reciprocal();
    public static final LargeFraction ONE = new LargeFraction(LargeInteger.ONE);
    public static final LargeFraction TWO = new LargeFraction(LargeInteger.TWO);
    public static final LargeFraction THREE = new LargeFraction(LargeInteger.THREE);
    public static final LargeFraction FOUR = new LargeFraction(LargeInteger.FOUR);
    public static final LargeFraction FIVE = new LargeFraction(LargeInteger.FIVE);
    public static final LargeFraction SIX = new LargeFraction(LargeInteger.SIX);
    public static final LargeFraction SEVEN = new LargeFraction(LargeInteger.SEVEN);
    public static final LargeFraction EIGHT = new LargeFraction(LargeInteger.EIGHT);
    public static final LargeFraction NINE = new LargeFraction(LargeInteger.NINE);
    public static final LargeFraction TEN = new LargeFraction(LargeInteger.TEN);
    public LargeFraction(LargeInteger in) {
        numerator = in;
        denominator = LargeInteger.ONE;
    }
    public LargeFraction(LargeInteger numerator, LargeInteger denominator) {
        if (denominator.isNegative()) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        if (denominator.isZero())
            throw new ArithmeticException("Cannot divide by 0");
        LargeInteger g = numerator.gcd(denominator);
        this.numerator = numerator.divide(g);
        this.denominator = denominator.divide(g);
    }
    public LargeFraction(String str) {
        ScalarAtom R = Parser.parse(str);
        if(R instanceof LargeInteger I) {
            numerator = I;
            denominator = LargeInteger.ONE;
            return;
        }else if(R instanceof LargeFraction F) {
            numerator = F.numerator;
            denominator = F.denominator;
            return;
        }
        R = Parser.parse(R.toString());
        if(R instanceof LargeInteger I) {
            numerator = I;
            denominator = LargeInteger.ONE;
            return;
        }else if(R instanceof LargeFraction F) {
            numerator = F.numerator;
            denominator = F.denominator;
            return;
        }
        throw new IllegalArgumentException("Inputted expression does not evaluate to a rational number");
    }
    public static LargeFraction valueOf(long n) {return new LargeFraction(LargeInteger.valueOf(n));}
    @Override
    public LargeFraction clone() {return new LargeFraction(numerator.clone(), denominator.clone());}
    @Override
    public String toString() {
        if(denominator.isOne())
            return numerator.toString();
        return numerator + "/" + denominator;
    }
    public int hashCode() {return CASHash.rational(numerator, denominator);}
    @Override
    public String toLatex() {
        if (denominator.isOne())
            return numerator.toLatex();
        return "\\frac{" + numerator.toLatex() + "}{" + denominator.toLatex() + "}";
    }
    @Override
    public boolean equals(Atom other) {
        if (!(other instanceof ScalarAtom)) return false;
        if(other instanceof LargeInteger I)
            return numerator.equals(I) && denominator.isOne();
        else if(other instanceof LargeFraction F)
            return numerator.equals(F.numerator) && denominator.equals(F.denominator);
        return this.compareTo((ScalarAtom) other) == 0L;
    }
    @Override
    public boolean isZero() {return numerator.isZero();}
    @Override
    public boolean isOne() {return numerator.isOne();}
    @Override
    public boolean isNegative() {return numerator.isNegative();}
    @Override
    public LargeFraction abs() {return new LargeFraction(numerator.abs(), denominator);}
    @Override
    public LargeInteger sign() {return numerator.sign();}
    @Override
    public int compareMagnitudes(ScalarAtom other) {
        return switch (other) {
            case LargeInteger I -> numerator.compareMagnitudes(I.multiply(denominator));
            case LargeFraction F -> numerator.multiply(F.denominator).compareMagnitudes(
                    F.numerator.multiply(denominator));
            case LargePowerMonomial LPM ->
                    power(LPM.exponent.denominator).compareTo(LPM.power(LPM.exponent.denominator).base);
            case RadicalTerm RT -> this.subtract(RT.rationalPart).divide(RT.coefficient)
                    .compareMagnitudes(RT.exponent);
            default -> throw new UnsupportedOperationException(
                    "Operation with these types together not supported yet");
        };
    }
    public int compareTo(ScalarAtom other) {
        if(other.isNotReal()) throw new ArithmeticException("Complex numbers cannot be compared");
        return switch (other) {
            case LargeInteger I -> numerator.compareTo(I.multiply(denominator));
            case LargeFraction F -> numerator.multiply(F.denominator).compareTo(F.numerator.multiply(denominator));
            case LargePowerMonomial largePowerMonomial -> {
                int C = this.compareMagnitudes(largePowerMonomial);
                if (C == 1) {
                    if (this.numerator.isNegative()) yield -C;
                    yield C;
                } else if (C == -1) {
                    if (other.isNegative()) yield C;
                    yield C;
                }
                yield C;
            }case RadicalTerm RT -> {
                if (RT.coefficient.isZero()) yield compareTo(RT.rationalPart);
                LargeFraction x = this.subtract(RT.rationalPart).divide(RT.coefficient);
                // compare x ? other.exponent
                int cmp = x.compareTo(RT.exponent);
                // because x = (F - A) / B
                if (RT.coefficient.isNegative()) cmp = -cmp;
                yield cmp;
            }default -> throw new UnsupportedOperationException(
                    "Operation with these types together not supported yet");
        };
    }
    @Override
    public LargeFraction negate() {return new LargeFraction(numerator.negate(), denominator);}
    @Override
    public boolean isRational() {return true;}
    @Override
    public boolean isReal() {return true;}
    @Override
    public boolean isAlgebraic() {return true;}
    @Override
    public ScalarAtom realPart() {return this;}
    @Override
    public ScalarAtom imagPart() {return LargeInteger.ZERO;}
    @Override
    public LargeInteger floor() {
        LargeInteger[] pair = numerator.quotientAndRemainder(denominator);
        if (pair[1].isNegative()) return pair[0].subtract(1);
        return pair[0];
    }
    public LargeInteger intPart() {return numerator.divide(denominator);}
    public LargeFraction fracPart() {return new LargeFraction(numerator.remainder(denominator), denominator);}
    @Override
    public LargeInteger ceil() {
        LargeInteger[] DR = numerator.quotientAndRemainder(denominator);
        LargeInteger result = DR[0];
        if (greaterThan(ZERO) && !DR[1].isZero())
            result = result.add(1);
        return result;
    }
    PowerFormFraction asPowerForm() {
        PowerFormInteger num = numerator.asPowerForm();
        PowerFormInteger den = denominator.asPowerForm();
        long exponent = LargeInteger.gcdLong(num.exponent(), den.exponent());
        if (exponent == 1) return new PowerFormFraction(this, 1);
        LargeInteger numBase = num.base().power(num.exponent() / exponent).numerator;
        LargeInteger denBase = den.base().power(den.exponent() / exponent).numerator;
        return new PowerFormFraction(new LargeFraction(numBase, denBase), exponent);
    }
    public LargeMap<LargeInteger, LargeFraction> factor() {
        LargeMap<LargeInteger, LargeFraction> N = numerator.factor();
        if(denominator.isOne()) return N;
        if(numerator.isOne()) N.remove(LargeInteger.ONE);
        LargeMap<LargeInteger, LargeFraction> D = denominator.factor();
        for(LargeInteger I: D.keys()) N.put(I, D.get(I).negate());
        return N;
    }
    public static LargeFraction buildFromFactors(LargeMap<LargeInteger, LargeFraction> pr) {
        LargeMap<LargeInteger, LargeFraction> N = new LargeMap<>();
        LargeMap<LargeInteger, LargeFraction> D = new LargeMap<>();
        for(LargeInteger I: pr.keys()) {
            if(!pr.get(I).numerator.isNegative())
                N.put(I, pr.get(I).clone());
            else D.put(I, pr.get(I).negate());
        }
        return new LargeFraction(LargeInteger.buildFromFactors(N), LargeInteger.buildFromFactors(D));
    }
    public LargeFraction add(LargeFraction F) {
        LargeInteger l = denominator.lcm(F.denominator);
        return new LargeFraction(this.numerator.multiply(l.divide(denominator))
                .add(F.numerator.multiply(l.divide(F.denominator))), l);
    }
    @Override
    public ScalarAtom add(ScalarAtom other) {
        return switch (other) {
            case LargeInteger I -> new LargeFraction(numerator.add(I.multiply(denominator)), denominator);
            case LargeFraction F -> add(F);
            case LargePowerMonomial LPM -> new RadicalTerm(this, ONE, LPM);
            case RadicalTerm RT -> new RadicalTerm(add(RT.rationalPart), RT.coefficient, RT.exponent);
            case RadicalTermSum RTS -> new RadicalTermSum(RTS.sum, RTS.constant.add(this));
            default -> throw new UnsupportedOperationException(
                    "Operation with these types together not supported yet");
        };
    }
    LargeFraction add(long other) {
        return new LargeFraction(numerator.add(denominator.multiply(other)), denominator);}
    public ScalarAtom add(ScalarAtom... other) {
        ScalarAtom result = this.clone();
        for (ScalarAtom atom : other) result = result.add(atom);
        return result;
    }
    public LargeFraction subtract(LargeFraction F) {
        LargeInteger l = denominator.lcm(F.denominator);
        return new LargeFraction(this.numerator.multiply(l.divide(denominator))
                .subtract(F.numerator.multiply(l.divide(F.denominator))), l);
    }
    public LargeFraction multiply(LargeFraction other) {
        return new LargeFraction(numerator.multiply(other.numerator), denominator.multiply(other.denominator));}
    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        return switch (other) {
            case LargeInteger integer -> new LargeFraction(numerator.multiply(integer), denominator);
            case LargeFraction F -> multiply(F);
            case RadicalTerm RT -> new RadicalTerm(RT.rationalPart.multiply(this),
                    RT.coefficient.multiply(this), RT.exponent);
            default -> other.multiply(this);
        };
    }
    LargeFraction multiply(long other) {return new LargeFraction(numerator.multiply(other), denominator);}
    public ScalarAtom multiply(LargeList<ScalarAtom> other) {
        ScalarAtom result = clone();
        for (ScalarAtom fr : other) result = result.multiply(fr);
        return result;
    }
    @Override
    public LargeFraction square() {return new LargeFraction(numerator.square(), denominator.square());}
    @Override
    public LargeFraction cube() {return new LargeFraction(numerator.cube(), denominator.cube());}
    public LargeFraction power(LargeInteger exp) {
        LargeFraction n = numerator.power(exp);
        LargeFraction d = denominator.power(exp);
        return n.divide(d);
    }
    public LargePowerMonomial power(LargeFraction exp) {return new LargePowerMonomial(this, exp);}
    @Override
    public LargeDecimal approximateToDecimal(LargeInteger precision) {return null;}
    @Override
    public LargeFraction reciprocal() {return new LargeFraction(denominator, numerator);}
    public LargeFraction divide(LargeFraction other) {
        return new LargeFraction(numerator.multiply(other.denominator), denominator.multiply(other.numerator));}
    public LargeFraction gcd(LargeFraction other) {
        LargeFraction copy = this.abs();
        LargeFraction copy2 = other.abs();
        while (!copy2.isZero()) {
            LargeFraction temp = new LargeFraction(copy.numerator, copy.denominator);
            copy = new LargeFraction(copy2.numerator, copy2.denominator);
            copy2 = temp.mod(copy2);
        }
        return copy;
    }
    public LargeFraction lcm(LargeFraction other) {return divide(gcd(other)).multiply(abs());}
    public LargeFraction mod(LargeFraction other) {
        return (LargeFraction) subtract(divide(other).floor().multiply(other));}
}
