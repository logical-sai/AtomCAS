package com.AtomCAS.utilities;

public class LargeFraction extends ScalarAtom {
    final LargeInteger numerator;
    final LargeInteger denominator;
    public static final LargeFraction NEGATIVE_ONE = new LargeFraction(-1L);
    public static final LargeFraction ZERO = new LargeFraction(0L);
    public static final LargeFraction HALF = new LargeFraction(LargeInteger.ONE, LargeInteger.TWO);
    public static final LargeFraction ONE = new LargeFraction(1L);
    public LargeFraction(LargeInteger numerator, LargeInteger denominator) {
        if (denominator.negative) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        LargeInteger g = numerator.gcd(denominator);
        this.numerator = numerator.divide(g);
        this.denominator = denominator.divide(g);
        if (LargeInteger.isZeroArray(this.denominator.limbs))
            throw new ArithmeticException("Cannot divide by 0");
    }
    public LargeFraction(String str){
        ScalarAtom R = Parser.parse(str);
        if(R instanceof LargeInteger I){
            numerator = I;
            denominator = LargeInteger.ONE;
            return;
        }else if(R instanceof LargeFraction F){
            numerator = F.numerator;
            denominator = F.denominator;
            return;
        }
        R = Parser.parse(R.toString());
        if(R instanceof LargeInteger I){
            numerator = I;
            denominator = LargeInteger.ONE;
            return;
        }else if(R instanceof LargeFraction F){
            numerator = F.numerator;
            denominator = F.denominator;
            return;
        }
        throw new IllegalArgumentException("Inputted expression does not evaluate to a rational number");
    }
    public LargeFraction(long num) {
        numerator = LargeInteger.valueOf(num);
        denominator = LargeInteger.ONE;
    }
    @Override
    public LargeFraction clone() {return new LargeFraction(numerator, denominator);}
    @Override
    public String toString() {
        if(LargeInteger.isOneArray(denominator.limbs))
            return numerator.toString();
        return numerator + "/" + denominator;
    }
    public int hashCode(){return CASHash.rational(numerator, denominator);}
    @Override
    public String toLatex(){
        if (LargeInteger.isOneArray(denominator.limbs))
            return numerator.toLatex();
        return "\\frac{" + numerator.toLatex() + "}{" + denominator.toLatex() + "}";
    }
    @Override
    public boolean equals(Atom other) {
        if (!(other instanceof ScalarAtom)) return false;
        if(other instanceof LargeInteger I)
            return numerator.equals(I) && LargeInteger.isOneArray(denominator.limbs);
        else if(other instanceof LargeFraction F)
            return numerator.equals(F.numerator) && denominator.equals(F.denominator);
        return this.compareTo((ScalarAtom) other) == 0L;
    }
    @Override
    public LargeFraction abs() {return new LargeFraction(numerator.abs(), denominator);}
    @Override
    public int compareMagnitudes(ScalarAtom other) {
        return switch (other) {
            case LargeInteger I -> numerator.compareMagnitudes(I.multiply(denominator));
            case LargeFraction F -> numerator.multiply(F.denominator).compareMagnitudes(
                    F.numerator.multiply(denominator));
            case LargePowerMonomial LPM -> {
                LargeFraction f = this.power(LPM.exponent.denominator);
                LargeFraction o = LPM.power(LPM.exponent.denominator).base;
                yield f.compareTo(o);
            }case RadicalTerm RT -> this.subtract(RT.rationalPart).divide(RT.coefficient)
                    .compareMagnitudes(RT.exponent);
            default -> throw new UnsupportedOperationException("Operation with these types together not supported yet");
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
                    if (this.numerator.negative) yield -C;
                    yield C;
                } else if (C == -1) {
                    if (other.lessThan(ZERO)) yield C;
                    yield C;
                }
                yield C;
            }case RadicalTerm RT -> {
                if (RT.coefficient.equals(LargeInteger.ZERO)) yield compareTo(RT.rationalPart);
                LargeFraction x = (LargeFraction) this.subtract(RT.rationalPart).divide(RT.coefficient);
                // compare x ? other.exponent
                int cmp = x.compareTo(RT.exponent);
                // because x = (F - A) / B
                if (RT.coefficient.sign().equals(LargeInteger.NEGATIVE_ONE))
                    cmp = -cmp;
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
        if (pair[1].negative) return pair[0].subtract(1);
        return pair[0];
    }
    public LargeInteger intPart() {return numerator.divide(denominator);}
    public LargeFraction fracPart() {return new LargeFraction(numerator.remainder(denominator), denominator);}
    @Override
    public LargeInteger ceil() {
        LargeInteger[] DR = numerator.quotientAndRemainder(denominator);
        LargeInteger result = DR[0];
        if (this.greaterThan(ZERO) && !LargeInteger.isZeroArray(DR[1].limbs))
            result = result.add(1);
        return result;
    }
    public LargeMap<LargeInteger, LargeFraction> factor() {
        LargeMap<LargeInteger, LargeFraction> N = numerator.factor();
        if(LargeInteger.isOneArray(denominator.limbs)) return N;
        if(LargeInteger.isOneArray(numerator.limbs)) N.remove(LargeInteger.ONE);
        LargeMap<LargeInteger, LargeFraction> D = denominator.factor();
        for(LargeInteger I: D.keys()) N.put(I, D.get(I).negate());
        return N;
    }
    public static LargeFraction buildFromFactors(LargeMap<LargeInteger, LargeFraction> pr) {
        LargeMap<LargeInteger, LargeFraction> N = new LargeMap<>();
        LargeMap<LargeInteger, LargeFraction> D = new LargeMap<>();
        for(LargeInteger I: pr.keys()){
            if(!pr.get(I).numerator.negative)
                N.put(I, pr.get(I).clone());
            else D.put(I, pr.get(I).negate());
        }
        return new LargeFraction(LargeInteger.buildFromFactors(N), LargeInteger.buildFromFactors(D));
    }
    @Override
    public ScalarAtom add(ScalarAtom other) {
        return switch (other) {
            case LargeInteger I -> new LargeFraction(numerator.add(I.multiply(denominator)),
                    denominator);
            case LargeFraction F -> {
                LargeInteger l = denominator.lcm(F.denominator);
                yield new LargeFraction(this.numerator.multiply(l.divide(denominator))
                        .add(F.numerator.multiply(l.divide(F.denominator))), l);
            }
            case LargePowerMonomial LPM -> new RadicalTerm(this, ONE, LPM);
            case RadicalTerm RT -> new RadicalTerm((LargeFraction) this.add(RT.rationalPart), RT.coefficient,
                    RT.exponent);
            case RadicalTermSum RTS -> new RadicalTermSum(RTS.sum, (LargeFraction) RTS.constant.add(this));
            default -> throw new UnsupportedOperationException(
                    "Operation with these types together not supported yet");
        };
    }
    public LargeFraction add(long other) {return new LargeFraction(numerator.add(denominator.multiply(other)),
            denominator);}
    public ScalarAtom add(ScalarAtom... other) {
        ScalarAtom result = this.clone();
        for (ScalarAtom atom : other) result = result.add(atom);
        return result;
    }
    public ScalarAtom subtract(ScalarAtom... atoms) {
        ScalarAtom result = this.clone();
        for(ScalarAtom atom: atoms) result = result.subtract(atom);
        return result;
    }
    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        return switch (other) {
            case LargeInteger integer -> new LargeFraction(numerator.multiply(integer), denominator);
            case LargeFraction F -> new LargeFraction(numerator.multiply(F.numerator),
                    denominator.multiply(F.denominator));
            case RadicalTerm RT -> new RadicalTerm((LargeFraction) RT.rationalPart.multiply(this),
                    (LargeFraction) RT.coefficient.multiply(this), RT.exponent);
            default -> other.multiply(this);
        };
    }
    public LargeFraction multiply(long other) {return new LargeFraction(numerator.multiply(other), denominator);}
    public ScalarAtom multiply(LargeList<ScalarAtom> other) {
        ScalarAtom result = clone();
        for (ScalarAtom fr : other) result = result.multiply(fr);
        return result;
    }
    @Override
    public LargeFraction square(){return new LargeFraction(numerator.square(), denominator.square());}
    @Override
    public LargeFraction cube(){return new LargeFraction(numerator.cube(), denominator.cube());}
    public LargePowerMonomial power(LargeFraction exp) {return new LargePowerMonomial(this, exp);}
    @Override
    public LargeDecimal approximateToDecimal(LargeInteger precision) {return null;}
    public LargeFraction power(LargeInteger exp) {
        LargeFraction n = numerator.power(exp);
        LargeFraction d = denominator.power(exp);
        return (LargeFraction) n.divide(d);
    }
    @Override
    public LargeFraction reciprocal(){return new LargeFraction(denominator, numerator);}
    public LargeFraction gcd(LargeFraction other) {
        LargeFraction copy = this.abs();
        LargeFraction copy2 = other.abs();
        while (!LargeInteger.isZeroArray(copy2.numerator.limbs)){
            LargeFraction temp = new LargeFraction(copy.numerator, copy.denominator);
            copy = new LargeFraction(copy2.numerator, copy2.denominator);
            copy2 = (LargeFraction) temp.mod(copy2);
        }
        return copy;
    }
}
