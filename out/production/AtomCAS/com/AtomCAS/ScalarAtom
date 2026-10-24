package com.AtomCAS.utilities;
public abstract class ScalarAtom extends Atom{
    public abstract LargeInteger floor();
    public abstract LargeInteger ceil();
    public abstract ScalarAtom magnitude();
    public abstract long compareMags(ScalarAtom other);
    public abstract long compareTo(ScalarAtom other);
    public boolean greaterThan(ScalarAtom other){return this.compareTo(other) > 0;}
    public boolean lessThan(ScalarAtom other){return this.compareTo(other) < 0;}
    public boolean greaterThanOrEqualTo(ScalarAtom other){return this.compareTo(other) >= 0;}
    public boolean lesserThanOrEqualTo(ScalarAtom other){return this.compareTo(other) <= 0;}
    public abstract ScalarAtom negate();
    public abstract ScalarAtom sign();
    public abstract boolean isRational();
    public abstract boolean isReal();
    public abstract boolean isNotReal();
    public abstract ScalarAtom add(ScalarAtom other);
    public abstract ScalarAtom subtract(ScalarAtom other);
    public abstract ScalarAtom multiply(ScalarAtom other);
    public abstract ScalarAtom divide(ScalarAtom other);
    public abstract ScalarAtom reciprocal();
    public abstract ScalarAtom mod(ScalarAtom other);
    public abstract ScalarAtom square();
    public abstract ScalarAtom cube();
    public abstract ScalarAtom percent();
    public abstract ScalarAtom permille();
    public abstract ScalarAtom power(LargeFraction f);
}
