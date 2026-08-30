package com.AtomCAS.utilities;
public class AlgebraicNumber extends ScalarAtom{
    public final LargePolynomial minimalPolynomial;
    public final RationalBox rectangle;
    public AlgebraicNumber(ScalarAtom s) throws CloneNotSupportedException {
        minimalPolynomial = LargePolynomial.minimalPolynomial(s);
        rectangle = new RationalBox(s);
    }
    @Override
    public Atom clone() {
        return null;
    }

    @Override
    public String toString() {
        return "";
    }

    @Override
    public String toLatex() {
        return "";
    }

    @Override
    public boolean equals(Atom other) {
        return false;
    }

    @Override
    public LargeInteger floor() {
        return null;
    }

    @Override
    public LargeInteger ceil() {
        return null;
    }

    @Override
    public int compareMagnitudes(ScalarAtom other) {
        return 0;
    }

    @Override
    public int compareTo(ScalarAtom other) {
        return 0;
    }

    @Override
    public ScalarAtom negate() {
        return null;
    }

    @Override
    public ScalarAtom abs() {
        return null;
    }

    @Override
    public boolean isRational() {
        return false;
    }

    @Override
    public boolean isReal() {
        return false;
    }

    public boolean isAlgebraic(){return true;}

    @Override
    public ScalarAtom realPart() {
        return null;
    }

    @Override
    public ScalarAtom imagPart() {
        return null;
    }

    @Override
    public ScalarAtom add(ScalarAtom other) {
        return null;
    }

    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        return null;
    }

    @Override
    public ScalarAtom reciprocal() {
        return null;
    }

    @Override
    public ScalarAtom power(LargeFraction f) {
        return null;
    }

    @Override
    public LargeDecimal approximateToDecimal(LargeInteger precision) {return null;}
}
class RationalInterval{
    LargeFraction lower;
    LargeFraction upper;
    RationalInterval(LargeFraction lower, LargeFraction upper){
        this.lower = lower;
        this.upper = upper;
    }
}
class RationalBox{
    RationalInterval realPart;
    RationalInterval imagPart;
    RationalBox(ScalarAtom s){
        //Strum sequence

    }
}