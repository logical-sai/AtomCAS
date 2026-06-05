package com.AtomCAS.utilities;
public class RadicalTermNode extends ScalarAtom{
    public final LargeFraction rationalPart;
    public final LargeFraction coefficient;
    public final LargePowerNode exponent;
    public RadicalTermNode(LargeFraction rationalPart, LargeFraction coefficient, LargePowerNode base){
        LargeFraction R = rationalPart.copy();
        LargeFraction C = coefficient.copy();
        ScalarAtom K = base.copy();
        if(K.equalsTo(LargeInteger.one)){
            this.rationalPart = (LargeFraction) R.add(C);
            this.coefficient = LargeFraction.zero;
            this.exponent = new LargePowerNode(LargeFraction.zero, LargeFraction.one, LargeFraction.zero);
            return;
        }
        this.rationalPart = R.copy();
        this.coefficient = C.copy();
        this.exponent = (LargePowerNode) K.copy();
    }
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        if(rationalPart.notEqualsTo(LargeInteger.zero))
            str.append(rationalPart);
        if(coefficient.equalsTo(LargeInteger.zero)){
            if(str.isEmpty()) str.append(rationalPart);
            return str.toString();
        }else if(coefficient.sign().equalsTo(LargeInteger.one)){
            if(rationalPart.notEqualsTo(LargeInteger.zero)) str.append(" + ");
            if(coefficient.notEqualsTo(LargeInteger.one)) str.append(coefficient);
        }else if(coefficient.sign().equalsTo(LargeInteger.negativeOne)){
            if(rationalPart.notEqualsTo(LargeInteger.zero)) str.append(" - ");
            if(coefficient.notEqualsTo(LargeInteger.one)) str.append(coefficient.magnitude());
        }
        if(coefficient.notEqualsTo(LargeInteger.one))
            str.append("*");
        str.append(exponent);
        return str.toString();
    }
    @Override
    public RadicalTermNode copy() {return new RadicalTermNode(rationalPart, coefficient, exponent);}
    @Override
    public boolean isRational(){return coefficient.equalsTo(LargeInteger.zero);}
    @Override
    public boolean isReal() {return !isNotReal();}
    @Override
    public boolean isNotReal(){return exponent.isNotReal();}
    @Override
    public ScalarAtom sign() {
        if(this.equalsTo(LargeInteger.zero)) return LargeInteger.zero;
        return this.divide(this.magnitude());
    }
    @Override
    public ScalarAtom magnitude() {
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public LargeInteger floor() {
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public LargeInteger ceil() {
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public long compareMags(ScalarAtom other) {
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public long compareTo(ScalarAtom other) {throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");}

    @Override
    public boolean greaterThan(ScalarAtom other) {
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }

    @Override
    public boolean lessThan(ScalarAtom other) {
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }

    @Override
    public boolean greaterThanOrEqualTo(ScalarAtom other) {
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }

    @Override
    public boolean lesserThanOrEqualTo(ScalarAtom other) {
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }

    @Override
    public ScalarAtom negate() {throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");}
    @Override
    public ScalarAtom add(ScalarAtom other) {throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");}

    @Override
    public ScalarAtom subtract(ScalarAtom other) {
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }

    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        if(other instanceof LargeInteger || other instanceof LargeFraction)
            return new RadicalTermNode((LargeFraction) rationalPart.multiply(other), (LargeFraction) coefficient.multiply(other), exponent);
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }

    @Override
    public ScalarAtom divide(ScalarAtom other) {
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }

    @Override
    public ScalarAtom reciprocal(){return new LargePowerNode(this, LargeFraction.negativeOne, LargeFraction.zero);}

    @Override
    public ScalarAtom mod(ScalarAtom other) {return this.subtract(this.divide(other).floor().multiply(other));}

    @Override
    public ScalarAtom square() {
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }

    @Override
    public ScalarAtom cube() {
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }

    @Override
    public ScalarAtom percent() {
        return this.multiply(Parser.parse("1/100"));
    }

    @Override
    public ScalarAtom permille() {
        return this.multiply(Parser.parse("1/100"));
    }
    @Override
    public ScalarAtom power(LargeFraction f) {return new LargePowerNode(this, f, LargeFraction.zero);}
    @Override
    public boolean equalsTo(Atom other) {
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }

    @Override
    public boolean notEqualsTo(Atom other) {
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
}
