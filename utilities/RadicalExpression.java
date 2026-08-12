package com.AtomCAS.utilities;
public class RadicalExpression extends ScalarAtom{
    public LargeMap<LargePowerNode, LargeFraction> sum;
    public LargeFraction coefficient;
    public RadicalExpression(){
        sum = new LargeMap<>();
        coefficient = LargeFraction.ZERO;
    }
    public RadicalExpression(LargeMap<LargePowerNode, LargeFraction> sum, LargeFraction coefficient){
        this.sum = sum;
        this.coefficient = coefficient;
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
    public int compareMags(ScalarAtom other) {
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

    @Override
    public boolean isAlgebraic() {
        return false;
    }

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
    public LargeDecimal approximateToDecimal(long precision) {
        return null;
    }

    @Override
    public Atom copy() {
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
    public int hashCode() {
        return 0;
    }
}
