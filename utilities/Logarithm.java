package com.AtomCAS.utilities;
public class Logarithm extends ScalarAtom {
    public ScalarAtom argument;
    public Logarithm(ScalarAtom a){argument = (ScalarAtom) a.copy();}
    @Override
    public ScalarAtom copy() {throw new UnsupportedOperationException("Error: Operation with these two types together not supported yet");}
    @Override
    public LargeInteger floor() {throw new UnsupportedOperationException("Error: Operation with these two types together not supported yet");}
    @Override
    public LargeInteger ceil() {throw new UnsupportedOperationException("Error: Operation with these two types together not supported yet");}
    @Override
    public ScalarAtom sign() {return LargeInteger.zero;}
    @Override
    public ScalarAtom magnitude() {throw new UnsupportedOperationException("Error: Operation with these two types together not supported yet");}
    @Override
    public long compareMags(ScalarAtom other) {return 0;}
    @Override
    public long compareTo(ScalarAtom other) {return 0;}
    @Override
    public boolean greaterThan(ScalarAtom other) {return false;}
    @Override
    public boolean lessThan(ScalarAtom other) {return false;}
    @Override
    public boolean greaterThanOrEqualTo(ScalarAtom other) {return false;}
    @Override
    public boolean lesserThanOrEqualTo(ScalarAtom other) {return false;}
    @Override
    public ScalarAtom negate() {return new Logarithm(argument.reciprocal());}
    @Override
    public boolean isRational() {return false;}
    @Override
    public boolean isReal() {return false;}
    @Override
    public boolean isNotReal() {return false;}
    @Override
    public boolean isAlgebraic() {return argument instanceof Exp;}
    @Override
    public ScalarAtom add(ScalarAtom other) {throw new UnsupportedOperationException("Error: Operation with these two types together not supported yet");}
    @Override
    public ScalarAtom multiply(ScalarAtom other) {throw new UnsupportedOperationException("Error: Operation with these two types together not supported yet");}
    @Override
    public ScalarAtom reciprocal() {throw new UnsupportedOperationException("Error: Operation with these two types together not supported yet");}
    @Override
    public ScalarAtom square() {throw new UnsupportedOperationException("Error: Operation with these two types together not supported yet");}
    @Override
    public ScalarAtom cube() {throw new UnsupportedOperationException("Error: Operation with these two types together not supported yet");}
    @Override
    public ScalarAtom power(LargeFraction f) {return null;}

    @Override
    public LargeDecimal approximateToDecimal(LargeInteger precision) {
        return null;
    }

    @Override
    public String toString() {return "";}
    @Override
    public boolean equalsTo(Atom other) {return false;}
    @Override
    public boolean notEqualsTo(Atom other) {return false;}
}
