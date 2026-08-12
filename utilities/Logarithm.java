package com.AtomCAS.utilities;
public class Logarithm extends ScalarAtom {
    public ScalarAtom argument;
    public Logarithm(ScalarAtom a){argument = (ScalarAtom) a.copy();}
    @Override
    public ScalarAtom copy() {throw new UnsupportedOperationException("Error: Operation with these TWO types together not supported yet");}
    @Override
    public LargeInteger floor() {throw new UnsupportedOperationException("Error: Operation with these TWO types together not supported yet");}
    @Override
    public LargeInteger ceil() {throw new UnsupportedOperationException("Error: Operation with these TWO types together not supported yet");}
    @Override
    public ScalarAtom abs() {throw new UnsupportedOperationException("Error: Operation with these TWO types together not supported yet");}
    @Override
    public int compareMags(ScalarAtom other) {return 0;}
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
    public ScalarAtom realPart() {
        return null;
    }
    @Override
    public ScalarAtom imagPart() {
        return null;
    }
    @Override
    public ScalarAtom add(ScalarAtom other) {throw new UnsupportedOperationException("Error: Operation with these TWO types together not supported yet");}
    @Override
    public ScalarAtom multiply(ScalarAtom other) {throw new UnsupportedOperationException("Error: Operation with these TWO types together not supported yet");}
    @Override
    public ScalarAtom reciprocal() {throw new UnsupportedOperationException("Error: Operation with these TWO types together not supported yet");}
    @Override
    public ScalarAtom power(LargeFraction f) {return null;}
    @Override
    public LargeDecimal approximateToDecimal(long precision) {
        return null;
    }
    @Override
    public String toString() {return "";}
    @Override
    public String toLatex() {return "";}
    public int hashCode(){
        return 0;
    }
    @Override
    public boolean equals(Atom other) {return false;}
}
