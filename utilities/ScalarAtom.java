package com.AtomCAS.utilities;
/**
 * This special class is intended to dictate all the possible scalar values that can enter the system.
 * It has operations like floor, ceil, basic arithmetic, exponentiation, sign and abs, and so on.
 * approximateToDecimal is still a stub. I'll implement that later.
 */
public abstract class ScalarAtom extends Atom{
    public abstract LargeInteger floor();
    public abstract LargeInteger ceil();
    public abstract int compareMags(ScalarAtom other);
    public abstract int compareTo(ScalarAtom other);
    public boolean greaterThan(ScalarAtom other){return compareTo(other) > 0;}
    public boolean lessThan(ScalarAtom other){return compareTo(other) < 0;}
    public boolean greaterThanOrEqualTo(ScalarAtom other){return compareTo(other) >= 0;}
    public boolean lesserThanOrEqualTo(ScalarAtom other){return compareTo(other) <= 0;}
    public abstract ScalarAtom negate();
    public ScalarAtom sign(){
        if(equals(LargeInteger.ZERO)) return LargeInteger.ZERO;
        return divide(abs());
    }
    public abstract ScalarAtom abs();
    public abstract boolean isRational();
    public abstract boolean isReal();
    public boolean isNotReal(){return !isReal();}
    public abstract boolean isAlgebraic();
    public boolean isTranscendental(){return !isAlgebraic();}
    public abstract ScalarAtom realPart();
    public abstract ScalarAtom imagPart();
    public abstract ScalarAtom add(ScalarAtom other);
    public ScalarAtom subtract(ScalarAtom other){return add(other.negate());}
    public abstract ScalarAtom multiply(ScalarAtom other);
    public ScalarAtom divide(ScalarAtom other){return multiply(other.reciprocal());}
    public abstract ScalarAtom reciprocal();
    public ScalarAtom mod(ScalarAtom other){return subtract(divide(other).floor().multiply(other));}
    public ScalarAtom square(){return multiply(this);}
    public ScalarAtom cube(){return multiply(this.square());}
    public ScalarAtom percent(){return multiply(SI.centi);}
    public ScalarAtom permille(){return multiply(SI.milli);}
    public abstract ScalarAtom power(LargeFraction f);
    public abstract LargeDecimal approximateToDecimal(LargeInteger precision);
}
