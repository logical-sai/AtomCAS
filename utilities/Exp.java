package com.AtomCAS.utilities;
public class Exp extends ScalarAtom {
    ScalarAtom addend;
    ScalarAtom coefficient;
    ScalarAtom power;
    public Exp(ScalarAtom addend, ScalarAtom coefficient, ScalarAtom power){
        if(power.equals(LargeInteger.ZERO)){
            this.addend = addend.add(coefficient);
            this.coefficient = LargeInteger.ZERO;
            this.power = LargeInteger.ZERO;
            return;
        }else if(power instanceof Logarithm){
            this.addend = addend.add(coefficient.multiply(((Logarithm) power).argument));
            this.coefficient = LargeInteger.ZERO;
            this.power = LargeInteger.ZERO;
            return;
        }
        this.addend = addend;
        this.coefficient = coefficient;
        this.power = power;
    }
    @Override
    public Atom copy() {return new Exp(this.addend, this.coefficient, this.power);}
    @Override
    public String toString() {
        StringBuilder str = new  StringBuilder();
        if(addend.doesntEqual(LargeInteger.ZERO)) str.append(addend);
        if(coefficient.equals(LargeInteger.ZERO)){
            if(str.isEmpty()) str.append("0");
            return str.toString();
        }
        str.append(" + ").append(coefficient).append("*e^(").append(power).append(")");
        return str.toString();
    }

    @Override
    public String toLatex() {return "";}
    public int hashCode(){
        return 0;
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
    public ScalarAtom abs() {
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
    public boolean isRational() {
        return false;
    }

    @Override
    public boolean isReal() {
        return false;
    }

    @Override
    public boolean isAlgebraic(){return coefficient.equals(LargeInteger.ZERO) && addend.isAlgebraic();}

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
    public ScalarAtom power(LargeFraction f) {return new Exp(this.addend.power(f), this.coefficient.power(f), this.power.multiply(f));}
    @Override
    public LargeDecimal approximateToDecimal(long precision) {
        return null;
    }
}
