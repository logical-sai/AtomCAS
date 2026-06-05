package com.AtomCAS.utilities;
public class Exp extends ScalarAtom {
    ScalarAtom addend;
    ScalarAtom coefficient;
    ScalarAtom power;
    public Exp(ScalarAtom addend, ScalarAtom coefficient, ScalarAtom power){
        if(power.equalsTo(LargeInteger.zero)){
            this.addend = addend.add(coefficient);
            this.coefficient = LargeInteger.zero;
            this.power = LargeInteger.zero;
            return;
        }else if(power instanceof Logarithm){
            this.addend = addend.add(coefficient.multiply(((Logarithm) power).argument));
            this.coefficient = LargeInteger.zero;
            this.power = LargeInteger.zero;
            return;
        }
        this.addend = addend;
        this.coefficient = coefficient;
        this.power = power;
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
    public ScalarAtom sign() {
        return LargeInteger.zero;
    }

    @Override
    public ScalarAtom magnitude() {
        return null;
    }

    @Override
    public long compareMags(ScalarAtom other) {
        return 0;
    }

    @Override
    public long compareTo(ScalarAtom other) {
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
    public boolean isNotReal() {
        return false;
    }

    @Override
    public ScalarAtom add(ScalarAtom other) {
        return null;
    }

    @Override
    public ScalarAtom subtract(ScalarAtom other) {
        return null;
    }

    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        return null;
    }

    @Override
    public ScalarAtom divide(ScalarAtom other) {
        return null;
    }

    @Override
    public ScalarAtom reciprocal() {
        return null;
    }

    @Override
    public ScalarAtom mod(ScalarAtom other) {
        return null;
    }

    @Override
    public ScalarAtom square() {
        return null;
    }

    @Override
    public ScalarAtom cube() {
        return null;
    }

    @Override
    public ScalarAtom percent() {
        return null;
    }

    @Override
    public ScalarAtom permille() {
        return null;
    }
    @Override
    public ScalarAtom power(LargeFraction f) {return new Exp(this.addend.power(f), this.coefficient.power(f), this.power.multiply(f));}
    @Override
    public Atom copy() {return new Exp(this.addend, this.coefficient, this.power);}
    @Override
    public String toString() {
        StringBuilder str = new  StringBuilder();
        if(addend.notEqualsTo(LargeInteger.zero)) str.append(addend);
        if(coefficient.equalsTo(LargeInteger.zero)){
            if(str.isEmpty()) str.append("0");
            return str.toString();
        }
        str.append(" + ").append(coefficient).append("*e^(").append(power).append(")");
        return str.toString();
    }

    @Override
    public boolean equalsTo(Atom other) {
        return false;
    }

    @Override
    public boolean notEqualsTo(Atom other) {
        return false;
    }
}
