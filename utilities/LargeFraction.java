package com.AtomCAS.utilities;

public class LargeFraction extends ScalarAtom {
    public final LargeInteger numerator;
    public final LargeInteger denominator;
    public static final LargeFraction NEGATIVE_ONE = new LargeFraction(-1);
    public static final LargeFraction ZERO = new LargeFraction(0L);
    public static final LargeFraction HALF = new LargeFraction(LargeInteger.ONE, LargeInteger.TWO);
    public static final LargeFraction ONE = new LargeFraction(1L);
    public LargeFraction(LargeInteger num, LargeInteger dem) {
        if (dem.lessThan(LargeInteger.ZERO)) {
            num = num.negate();
            dem = dem.negate();
        }
        LargeInteger g = num.gcd(dem);
        numerator = num.longDivision(g);
        denominator = dem.longDivision(g);
        if (denominator.equals(LargeInteger.ZERO))
            throw new ArithmeticException("Error: Cannot divide by 0");
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
        throw new IllegalArgumentException("Error: Inputted expression does not evaluate to a rational number");
    }
    public LargeFraction(long num) {
        numerator = LargeInteger.valueOf(num);
        denominator = LargeInteger.ONE;
    }
    @Override
    public String toString() {
        if(denominator.equals(LargeInteger.ONE))
            return numerator.toString();
        return numerator + "/" + denominator;
    }
    public int hashCode(){return AtomHash.rational(numerator, denominator);}
    @Override
    public String toLatex(){
        if (denominator.equals(LargeInteger.ONE))
            return numerator.toLatex();
        return "\\frac{" + numerator.toLatex() + "}{" + denominator.toLatex() + "}";
    }
    @Override
    public boolean equals(Atom other) {
        if (!(other instanceof ScalarAtom)) return false;
        if(other instanceof LargeInteger)
            return numerator.equals(other) && denominator.equals(LargeInteger.ONE);
        else if(other instanceof LargeFraction f)
            return numerator.equals(f.numerator) && denominator.equals(f.denominator);
        return this.compareTo((ScalarAtom) other) == 0L;
    }
    @Override
    public LargeFraction abs() {return new LargeFraction(numerator.abs(), denominator);}
    @Override
    public int compareMags(ScalarAtom other) {
        if (other instanceof LargeInteger)
            return numerator.abs().compareTo(other.multiply(denominator).abs());
        else if (other instanceof LargeFraction)
            return numerator.multiply(((LargeFraction) other).denominator).compareMags(((LargeFraction) other).numerator.multiply(denominator));
        else if(other instanceof LargePowerMonomial){
            if(other.isNotReal()) throw new IllegalStateException("Error: Complex numbers cannot be compared");
            LargeFraction f = this.power(((LargePowerMonomial)other).exponent.denominator);
            LargeFraction o = ((LargePowerMonomial) other).power((LargeFraction) ((LargePowerMonomial) other).exponent.denominator.divide(LargeInteger.ONE)).base;
            return f.compareTo(o);
        }else if(other instanceof RadicalTerm)
            return this.subtract(((RadicalTerm) other).rationalPart).divide(((RadicalTerm) other).coefficient).compareMags(((RadicalTerm) other).exponent);
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    public int compareTo(ScalarAtom other) {
        if (other instanceof LargeInteger)
            return numerator.compareTo(other.multiply(denominator));
        else if(other instanceof LargeFraction){
            return numerator.multiply(((LargeFraction) other).denominator).compareTo(((LargeFraction) other).numerator.multiply(denominator));
        }else if (other instanceof LargePowerMonomial) {
            int C = this.compareMags(other);
            if(C == 1){
                if(this.sign().equals(LargeInteger.NEGATIVE_ONE)) return -1;
                return 1;
            }else if(C == -1){
                if(other.sign().equals(LargeInteger.NEGATIVE_ONE)) return 1;
                return -1;
            }
            return C;
        }else if (other instanceof RadicalTerm){
            if(((RadicalTerm) other).coefficient.equals(LargeInteger.ZERO)) return this.compareTo(((RadicalTerm) other).rationalPart);
            LargeFraction x = (LargeFraction) this.subtract(((RadicalTerm) other).rationalPart).divide(((RadicalTerm) other).coefficient);
            // compare x ? other.exponent
            int cmp = x.compareTo(((RadicalTerm) other).exponent);
            // because x = (F - A) / B
            if (((RadicalTerm) other).coefficient.sign().equals(LargeInteger.NEGATIVE_ONE))
                cmp = -cmp;
            return cmp;
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
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
    public LargeFraction copy() {return new LargeFraction(numerator, denominator);}
    @Override
    public LargeInteger floor() {return numerator.floorDivision(denominator);}
    public LargeInteger intPart() {return numerator.longDivision(denominator);}
    public LargeFraction fracPart() {return new LargeFraction(numerator.remainder(denominator), denominator);}
    @Override
    public LargeInteger ceil() {
        LargeInteger result = numerator.longDivision(denominator);
        if (this.greaterThan(ZERO) && numerator.remainder(denominator).doesntEqual(ZERO))
            result = result.add(1);
        return result;
    }
    public LargeMap<LargeInteger, LargeFraction> factor() {
        LargeMap<LargeInteger, LargeFraction> N = numerator.factor();
        if(denominator.equals(ONE)) return N;
        if(numerator.equals(ONE)) N.remove(LargeInteger.ONE);
        LargeMap<LargeInteger, LargeFraction> D = denominator.factor();
        for(LargeInteger I: D.keys()) N.put(I, D.get(I).negate());
        return N;
    }
    public static LargeFraction buildFromFactors(LargeMap<LargeInteger, LargeFraction> pr) {
        LargeMap<LargeInteger, LargeFraction> N = new LargeMap<>();
        LargeMap<LargeInteger, LargeFraction> D = new LargeMap<>();
        for(LargeInteger I: pr.keys()){
            if(pr.get(I).greaterThan(ZERO)) N.put(I, pr.get(I).copy());
            else D.put(I, pr.get(I).negate());
        }
        return new LargeFraction(LargeInteger.buildFromFactors(N), LargeInteger.buildFromFactors(D));
    }
    @Override
    public ScalarAtom add(ScalarAtom other) {
        if (other instanceof LargeInteger I) return new LargeFraction((LargeInteger) numerator.add(I.multiply(denominator)), denominator);
        else if (other instanceof LargeFraction F){
            LargeInteger l = this.denominator.lcm(F.denominator);
            return new LargeFraction((LargeInteger) this.numerator.multiply(l.longDivision(this.denominator)).add(F.numerator.multiply(l.longDivision(F.denominator))), l);
        }else if (other instanceof LargePowerMonomial LPM) return new RadicalTerm(this, ONE, LPM);
        else if (other instanceof RadicalTerm RT)
            return new RadicalTerm((LargeFraction) this.add(RT.rationalPart), RT.coefficient, RT.exponent);
        else if (other instanceof RadicalTermSum RTS)
            return new RadicalTermSum(RTS.sum, (LargeFraction) RTS.constant.add(this));
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    public LargeFraction add(long other) {return new LargeFraction((LargeInteger) numerator.add(denominator.multiply(other)), denominator);}
    public ScalarAtom add(ScalarAtom... other) {
        ScalarAtom result = this.copy();
        for (ScalarAtom atom : other) result = result.add(atom);
        return result;
    }
    public ScalarAtom subtract(ScalarAtom... atoms) {
        ScalarAtom result = this.copy();
        for(ScalarAtom atom: atoms) result = result.subtract(atom);
        return result;
    }
    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        if (other instanceof LargeInteger)
            return new LargeFraction((LargeInteger) numerator.multiply(other), denominator);
        else if (other instanceof LargeFraction F)
            return new LargeFraction((LargeInteger) this.numerator.multiply(F.numerator), (LargeInteger) this.denominator.multiply(F.denominator));
        else if (other instanceof RadicalTerm RT)
            return new RadicalTerm((LargeFraction) RT.rationalPart.multiply(this), (LargeFraction) RT.coefficient.multiply(this), RT.exponent);
        return other.multiply(this);
    }
    public LargeFraction multiply(long other) {return (LargeFraction) this.multiply(new LargeFraction(other));}
    public ScalarAtom multiply(LargeList<ScalarAtom> other) {
        ScalarAtom result = this.copy();
        for (ScalarAtom fr : other) result = result.multiply(fr);
        return result;
    }
    @Override
    public LargeFraction square(){return new LargeFraction(numerator.square(), denominator.square());}
    @Override
    public LargeFraction cube(){return new LargeFraction(numerator.cube(), denominator.cube());}
    public LargePowerMonomial power(LargeFraction exp) {return new LargePowerMonomial(this, exp);}
    @Override
    public LargeDecimal approximateToDecimal(long precision) {
        LargeInteger factor = LargeInteger.pow10(precision).numerator;
        if (precision > 0) {
            LargeInteger scaled = ((LargeInteger) numerator.multiply(factor)).longDivision(denominator);
            return new LargeDecimal(scaled, precision);
        } else {
            LargeInteger divisor = factor.copy();
            LargeInteger scaled = numerator.longDivision((LargeInteger) denominator.multiply(divisor));
            return new LargeDecimal(scaled, precision);
        }
    }
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
        while (copy2.doesntEqual(LargeInteger.ZERO)){
            LargeFraction temp = new LargeFraction(copy.numerator, copy.denominator);
            copy = new LargeFraction(copy2.numerator, copy2.denominator);
            copy2 = (LargeFraction) temp.mod(copy2);
        }
        return copy;
    }
}
