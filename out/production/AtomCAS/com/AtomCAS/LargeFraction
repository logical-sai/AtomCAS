package com.AtomCAS.utilities;

import java.util.Random;

public class LargeFraction extends ScalarAtom {
    public final LargeInteger numerator;
    public final LargeInteger denominator;
    public static final LargeFraction negativeOne = new LargeFraction(-1);
    public static final LargeFraction zero = new LargeFraction(0L);
    public static final LargeFraction half = (LargeFraction) LargeInteger.one.divide(LargeInteger.two);
    public static final LargeFraction one = new LargeFraction(1L);
    public LargeFraction(LargeInteger num, LargeInteger dem) {
        if (dem.sign().equalsTo(LargeInteger.negativeOne)) {
            num = num.negate();
            dem = dem.negate();
        }
        LargeInteger g = num.gcd(dem);
        numerator = num.longDivision(g);
        denominator = dem.longDivision(g);
        if (denominator.sign().equalsTo(LargeInteger.zero)) throw new ArithmeticException("Error: Cannot divide by 0");
    }
    public LargeFraction(long num) {
        numerator = new LargeInteger(num);
        denominator = LargeInteger.one;
    }
    @Override
    public String toString() {
        if(denominator.equalsTo(LargeInteger.one))
            return numerator.toString();
        return numerator + "/" + denominator;
    }
    @Override
    public String toLatex(){
        if (denominator.equalsTo(LargeInteger.one))
            return numerator.toLatex();
        return "\\frac{" + numerator.toLatex() + "}{" + denominator.toLatex() + "}";
    }
    @Override
    public boolean equalsTo(Atom other) {
        if (!(other instanceof ScalarAtom)) return false;
        return this.compareTo((ScalarAtom) other) == 0L;
    }
    @Override
    public boolean notEqualsTo(Atom other) {return !this.equalsTo(other);}
    @Override
    public LargeFraction magnitude() {return new LargeFraction(numerator.magnitude(), denominator);}
    @Override
    public long compareMags(ScalarAtom other) {
        if (other instanceof LargeInteger)
            return numerator.magnitude().compareTo(other.multiply(denominator).magnitude());
        else if (other instanceof LargeFraction)
            return numerator.multiply(((LargeFraction) other).denominator).compareMags(((LargeFraction) other).numerator.multiply(denominator));
        else if(other instanceof LargePowerMonomial){
            if(other.isNotReal()) throw new IllegalStateException("Error: Complex numbers cannot be compared");
            LargeFraction f = this.power(((LargePowerMonomial)other).exponent.denominator);
            LargeFraction o = ((LargePowerMonomial) other).power((LargeFraction) ((LargePowerMonomial) other).exponent.denominator.divide(LargeInteger.one)).base;
            return f.compareTo(o);
        }else if(other instanceof RadicalTerm)
            return this.subtract(((RadicalTerm) other).rationalPart).divide(((RadicalTerm) other).coefficient).compareMags(((RadicalTerm) other).exponent);
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    public long compareTo(ScalarAtom other) {
        if (other instanceof LargeInteger)
            return numerator.compareTo(other.multiply(denominator));
        else if(other instanceof LargeFraction){
            return numerator.multiply(((LargeFraction) other).denominator).compareTo(((LargeFraction) other).numerator.multiply(denominator));
        }else if (other instanceof LargePowerMonomial) {
            long C = this.compareMags(other);
            if(C == 1){
                if(this.sign().equalsTo(LargeInteger.negativeOne)) return -1L;
                return 1L;
            }else if(C == -1L){
                if(other.sign().equalsTo(LargeInteger.negativeOne)) return 1L;
                return -1L;
            }
            return C;
        }else if (other instanceof RadicalTerm){
            if(((RadicalTerm) other).coefficient.equalsTo(LargeInteger.zero)) return this.compareTo(((RadicalTerm) other).rationalPart);
            LargeFraction x = (LargeFraction) this.subtract(((RadicalTerm) other).rationalPart).divide(((RadicalTerm) other).coefficient);
            // compare x ? other.exponent
            long cmp = x.compareTo(((RadicalTerm) other).exponent);
            // because x = (F - A) / B
            if (((RadicalTerm) other).coefficient.sign().equalsTo(LargeInteger.negativeOne))
                cmp = -cmp;
            return cmp;
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    public boolean greaterThan(ScalarAtom other) {return this.compareTo(other) > 0L;}
    public boolean lessThan(ScalarAtom other) {return this.compareTo(other) < 0L;}
    public boolean greaterThanOrEqualTo(ScalarAtom other) {return this.compareTo(other) >= 0L;}
    public boolean lesserThanOrEqualTo(ScalarAtom other) {return this.compareTo(other) <= 0L;}
    @Override
    public LargeFraction negate() {return new LargeFraction(numerator.negate(), denominator);}
    @Override
    public boolean isRational() {return true;}
    @Override
    public boolean isReal() {return true;}
    @Override
    public boolean isNotReal() {return false;}
    @Override
    public LargeInteger sign() {return numerator.sign();}
    @Override
    public LargeFraction copy() {return new LargeFraction(numerator, denominator);}
    @Override
    public LargeInteger floor() {return numerator.floorDivision(denominator);}
    public LargeInteger intPart() {return numerator.longDivision(denominator);}
    public LargeFraction fracPart() {return (LargeFraction) this.subtract(this.intPart());}
    @Override
    public LargeInteger ceil() {
        LargeInteger result = numerator.longDivision(denominator);
        if (this.sign().equalsTo(LargeInteger.one) && numerator.mod(denominator).notEqualsTo(LargeInteger.zero))
            result = (LargeInteger) result.add(LargeInteger.one);
        return result;
    }
    public static LargeFraction random() {
        Random RNG = new Random();
        StringBuilder str = new StringBuilder();
        //whole part
        long range = RNG.nextLong(50L) + 1;
        boolean sign = RNG.nextBoolean();
        if (sign) str.append("-");
        for (long i = 0; i < range; i++) str.append(RNG.nextLong(10L));
        boolean point = RNG.nextBoolean();
        if (point) {
            str.append(".");
            //nonrepeating part
            long range2 = RNG.nextLong(50L) + 1;
            for (long i = 0; i < range2; i++) str.append(RNG.nextLong(10L));
            boolean repeat = RNG.nextBoolean();
            if (repeat) {
                //repeating part
                str.append("(");
                long range3 = RNG.nextLong(20L) + 1;
                for (long i = 0; i < range3; i++) str.append(RNG.nextLong(10L));
                str.append(")");
            }
        }
        return (LargeFraction) Parser.parse(str.toString());
    }
    public static LargeFraction smallRandom() {
        int num = (int) (Math.random() * 11) - 5;
        int den = (int) (Math.random() * 9) + 1;
        return (LargeFraction) Parser.parse(num + "/" + den);
    }
    public LargeList<PrimeExponent> factor() {
        LargeList<PrimeExponent> n = numerator.factor();
        LargeList<PrimeExponent> d = denominator.factor();
        for (PrimeExponent p: d)
            n.add(new PrimeExponent(p.base, (LargeFraction) p.exponent.multiply(LargeFraction.negativeOne)));
        while (n.size().greaterThan(LargeInteger.one) && n.getFirst().base.equalsTo(LargeInteger.one)) n.removeFirst();
        while (n.size().greaterThan(LargeInteger.one) && n.getLast().base.equalsTo(LargeInteger.one)) n.removeLast();
        PrimeExponent.sortBases(n);
        return n;
    }
    public static LargeFraction buildFromFactors(LargeList<PrimeExponent> pr) {
        LargeList<PrimeExponent> N = new LargeList<>();
        LargeList<PrimeExponent> D = new LargeList<>();
        for (PrimeExponent p : pr){
            if(p.exponent.sign().equalsTo(LargeInteger.one)) N.add(p);
            else D.add(new PrimeExponent(p.base, p.exponent.magnitude()));
        }
        return (LargeFraction) LargeInteger.buildFromFactors(N).divide(LargeInteger.buildFromFactors(D));
    }
    @Override
    public ScalarAtom add(ScalarAtom other) {
        if (other instanceof LargeInteger) return this.add(new LargeFraction((LargeInteger) other, LargeInteger.one));
        else if (other instanceof LargeFraction){
            LargeInteger l = this.denominator.lcm(((LargeFraction) other).denominator);
            return new LargeFraction((LargeInteger) this.numerator.multiply(l.longDivision(this.denominator)).add(((LargeFraction) other).numerator.multiply(l.longDivision(((LargeFraction) other).denominator))), l);
        }else if (other instanceof LargePowerMonomial) return new RadicalTerm(this, LargeFraction.one, (LargePowerMonomial) other);
        else if (other instanceof RadicalTerm)
            return new RadicalTerm((LargeFraction) this.add(((RadicalTerm) other).rationalPart), ((RadicalTerm) other).coefficient, ((RadicalTerm) other).exponent);
        else if (other instanceof RadicalTermSum)
            return new RadicalTermSum(((RadicalTermSum) other).sum, (LargeFraction) ((RadicalTermSum) other).constant.add(this));
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    public ScalarAtom add(ScalarAtom... other) {
        ScalarAtom result = this.copy();
        for (ScalarAtom atom : other) result = result.add(atom);
        return result;
    }
    @Override
    public ScalarAtom subtract(ScalarAtom other) {return this.add(other.negate());}
    public ScalarAtom subtract(ScalarAtom... atoms) {
        ScalarAtom result = this.copy();
        for(ScalarAtom atom: atoms) result = result.subtract(atom);
        return result;
    }
    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        if (other instanceof LargeInteger)
            return new LargeFraction((LargeInteger) numerator.multiply(other), denominator);
        else if (other instanceof LargeFraction)
            return new LargeFraction((LargeInteger) this.numerator.multiply(((LargeFraction) other).numerator), (LargeInteger) this.denominator.multiply(((LargeFraction) other).denominator));
        else if (other instanceof LargePowerMonomial) return other.multiply(this);
        else if (other instanceof RadicalTerm)
            return new RadicalTerm((LargeFraction) ((RadicalTerm) other).rationalPart.multiply(this), (LargeFraction) ((RadicalTerm) other).coefficient.multiply(this), ((RadicalTerm) other).exponent);
        return other.multiply(this);
    }
    public LargeFraction multiply(long other) {return (LargeFraction) this.multiply(new LargeFraction(other));}
    public ScalarAtom multiply(LargeList<ScalarAtom> other) {
        ScalarAtom result = this.copy();
        for (ScalarAtom fr : other) result = result.multiply(fr);
        return result;
    }
    @Override
    public LargeFraction square() {return new LargeFraction(numerator.square(), denominator.square());}
    @Override
    public LargeFraction cube() {return new LargeFraction(numerator.cube(), denominator.cube());}
    @Override
    public LargeFraction percent() {return new LargeFraction(numerator, denominator.multiply(100));}
    @Override
    public LargeFraction permille() {return new LargeFraction(numerator, denominator.multiply(1000));}
    public LargePowerMonomial power(LargeFraction exp) {return new LargePowerMonomial(this, exp);}
    public LargeFraction power(LargeInteger exp) {
        LargeFraction n = numerator.power(exp);
        LargeFraction d = denominator.power(exp);
        return (LargeFraction) n.divide(d);
    }
    public boolean isPerfectNthPower(LargeFraction N){return new LargePowerMonomial(this, N.reciprocal()).exponent.equalsTo(LargeInteger.one);}
    @Override
    public ScalarAtom divide(ScalarAtom other){return this.multiply(other.reciprocal());}
    @Override
    public LargeFraction reciprocal(){return new LargeFraction(this.denominator, this.numerator);}
    @Override
    public ScalarAtom mod(ScalarAtom other){
        if (other instanceof LargeFraction || other instanceof LargeInteger) {
            if (other.equalsTo(LargeInteger.zero)) throw new ArithmeticException("Error: Cannot divide by 0");
            LargeFraction result = (LargeFraction) this.subtract(this.divide(other).floor().multiply(other));
            if (result.sign() != other.sign() && result.notEqualsTo(LargeInteger.zero))
                result = (LargeFraction) result.add(other.magnitude());
            return result;
        }
        return this.subtract(this.divide(other).floor().multiply(other));
    }
    public LargeFraction gcd(LargeFraction other) {
        LargeFraction copy = this.magnitude();
        LargeFraction copy2 = other.magnitude();
        while (copy2.notEqualsTo(LargeInteger.zero)){
            LargeFraction temp = new LargeFraction(copy.numerator, copy.denominator);
            copy = new LargeFraction(copy2.numerator, copy2.denominator);
            copy2 = (LargeFraction) temp.mod(copy2);
        }
        return copy;
    }
    public LargeFraction gcd(LargeFraction... other){
        LargeFraction r = this.copy();
        for(LargeFraction f: other) r = r.gcd(f);
        return r;
    }
    public LargeFraction lcm(LargeFraction other){return (LargeFraction) this.multiply(other).divide(this.gcd(other));}
    public LargeFraction lcm(LargeFraction... other){
        LargeFraction r = this.copy();
        for(LargeFraction f: other) r = r.lcm(f);
        return r;
    }
    public LargeList<LargeFraction> egyptianFractionSum() {
        LargeList<LargeFraction> result = new LargeList<>();
        result.add((LargeFraction) this.floor().divide(LargeInteger.one));
        if(this.equalsTo(LargeInteger.zero)) return result;
        LargeFraction F = (LargeFraction) this.mod(LargeInteger.one);
        while (F.notEqualsTo(LargeInteger.zero)) {
            LargeFraction unit = new LargeFraction(LargeInteger.one, F.reciprocal().ceil());
            result.add(unit);
            F = (LargeFraction) F.subtract(unit);
        }
        if(result.getFirst().equalsTo(LargeInteger.zero)) result.removeFirst();
        return result;
    }
}
