package com.AtomCAS.utilities;
/**
 * This special class is intended to dictate all the possible scalar values that can enter the system.
 * It has operations like floor, ceil, basic arithmetic, exponentiation, sign and magnitude, and so on.
 * approximateToDecimal is still a stub. I'll implement that later.
 */
public abstract class ScalarAtom extends Atom{
    public abstract LargeInteger floor();
    public abstract LargeInteger ceil();
    public abstract long compareMags(ScalarAtom other);
    public abstract long compareTo(ScalarAtom other);
    public boolean greaterThan(ScalarAtom other){return this.compareTo(other) > 0;}
    public boolean lessThan(ScalarAtom other){return this.compareTo(other) < 0;}
    public boolean greaterThanOrEqualTo(ScalarAtom other){return this.compareTo(other) >= 0;}
    public boolean lesserThanOrEqualTo(ScalarAtom other){return this.compareTo(other) <= 0;}
    public abstract ScalarAtom negate();
    public ScalarAtom sign(){
        if(this.equalsTo(LargeInteger.zero)) return LargeInteger.zero;
        return this.divide(this.magnitude());
    }
    public abstract ScalarAtom magnitude();
    public abstract boolean isRational();
    public abstract boolean isReal();
    public abstract boolean isNotReal();
    public abstract boolean isAlgebraic();
    public abstract ScalarAtom add(ScalarAtom other);
    public ScalarAtom subtract(ScalarAtom other){return this.add(other.negate());}
    public abstract ScalarAtom multiply(ScalarAtom other);
    public ScalarAtom divide(ScalarAtom other){return this.multiply(other.reciprocal());}
    public abstract ScalarAtom reciprocal();
    public ScalarAtom mod(ScalarAtom other){return this.subtract(this.divide(other).floor().multiply(other));}
    public abstract ScalarAtom square();
    public abstract ScalarAtom cube();
    public ScalarAtom percent(){return this.multiply(SI.centi);}
    public ScalarAtom permille(){return this.multiply(SI.milli);}
    public abstract ScalarAtom power(LargeFraction f);
    public abstract LargeDecimal approximateToDecimal(LargeInteger precision);
}
