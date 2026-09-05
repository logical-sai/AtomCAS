package com.AtomCAS.utilities;
public class Logarithm extends ScalarAtom {
    public ScalarAtom argument;
    public Logarithm(ScalarAtom a){argument = (ScalarAtom) a.clone();}
    @Override
    public ScalarAtom clone() {throw new UnsupportedOperationException("Operation with these TWO types together not supported yet");}
    @Override
    public LargeInteger floor() {throw new UnsupportedOperationException("Operation with these TWO types together not supported yet");}
    @Override
    public LargeInteger ceil() {throw new UnsupportedOperationException("Operation with these TWO types together not supported yet");}
    @Override
    public ScalarAtom abs() {throw new UnsupportedOperationException("Operation with these TWO types together not supported yet");}
    @Override
    public int compareMagnitudes(ScalarAtom other) {return 0;}
    @Override
    public int compareTo(ScalarAtom other) {return 0;}
    @Override
    public ScalarAtom negate() {return new Logarithm(argument.reciprocal());}
    @Override
    public boolean isRational() {return false;}
    @Override
    public boolean isReal() {return false;}
    @Override
    public boolean isAlgebraic() {return argument instanceof Exp;}
    @Override
    public ScalarAtom realPart() {return null;}
    @Override
    public ScalarAtom imagPart() {return null;}
    @Override
    public ScalarAtom add(ScalarAtom other) {throw new UnsupportedOperationException("Operation with these TWO types together not supported yet");}
    @Override
    public ScalarAtom multiply(ScalarAtom other) {throw new UnsupportedOperationException("Operation with these TWO types together not supported yet");}
    @Override
    public ScalarAtom reciprocal() {throw new UnsupportedOperationException("Operation with these TWO types together not supported yet");}
    @Override
    public ScalarAtom power(LargeFraction f) {return null;}
    @Override
    public LargeDecimal approximateToDecimal(LargeInteger precision) {
        return null;
    }
    @Override
    public String toString() {return "";}
    @Override
    public String toLatex() {return "";}
    @Override
    public boolean equals(Atom other) {return false;}
}
