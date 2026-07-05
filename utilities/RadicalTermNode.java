package com.AtomCAS.utilities;
public class RadicalTermNode extends ScalarAtom{
    public final LargeFraction rationalPart;
    public final LargeFraction coefficient;
    public final LargePowerNode exponent;
    public RadicalTermNode(LargeFraction rationalPart, LargeFraction coefficient, LargePowerNode base){
        LargeFraction R = rationalPart.copy();
        LargeFraction C = coefficient.copy();
        LargePowerNode K = base.copy();
        if(K.equalsTo(LargeInteger.one)){
            this.rationalPart = (LargeFraction) R.add(C);
            this.coefficient = LargeFraction.zero;
            this.exponent = new LargePowerNode(LargeFraction.zero, LargeFraction.one, LargeFraction.one);
            return;
        }else if(K.equalsTo(LargeInteger.zero) || C.equalsTo(LargeInteger.zero)){
            this.rationalPart = R;
            this.coefficient = LargeFraction.zero;
            this.exponent = new LargePowerNode(K.base, LargeFraction.one, LargeInteger.one);
            return;
        }
        this.rationalPart = R.copy();
        this.coefficient = C.copy();
        this.exponent = K.copy();
    }
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        if(rationalPart.notEqualsTo(LargeInteger.zero))
            str.append(rationalPart);
        if(coefficient.equalsTo(LargeInteger.zero)){
            if(str.isEmpty()) str.append(rationalPart);
            return str.toString();
        }else if(coefficient.greaterThan(LargeInteger.zero)){
            if(rationalPart.notEqualsTo(LargeInteger.zero)) str.append(" + ");
            if(coefficient.notEqualsTo(LargeInteger.one)) str.append(coefficient);
        }else if(coefficient.lessThan(LargeInteger.zero)){
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
    public boolean isAlgebraic() {return exponent.isAlgebraic();}
    @Override
    public ScalarAtom realPart(){
        return this.sign().realPart().multiply(this.magnitude());
    }
    @Override
    public ScalarAtom imagPart(){
        return this.sign().imagPart().multiply(this.magnitude());
    }
    @Override
    public ScalarAtom magnitude() {
        throw new UnsupportedOperationException("Error: Operation with this type together not supported yet");
    }
    @Override
    public LargeInteger floor() {
        throw new UnsupportedOperationException("Error: Operation with this type not supported yet");
    }
    @Override
    public LargeInteger ceil() {
        throw new UnsupportedOperationException("Error: Operation with this type together not supported yet");
    }
    @Override
    public long compareMags(ScalarAtom other) {
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public long compareTo(ScalarAtom other) {throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");}
    @Override
    public ScalarAtom negate() {throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");}
    @Override
    public ScalarAtom add(ScalarAtom other) {throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");}
    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        if(other instanceof LargeInteger || other instanceof LargeFraction)
            return new RadicalTermNode((LargeFraction) rationalPart.multiply(other), (LargeFraction) coefficient.multiply(other), exponent);
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public ScalarAtom reciprocal(){return new LargePowerNode(this, LargeFraction.negativeOne, LargeFraction.zero);}
    @Override
    public ScalarAtom power(LargeFraction f) {return new LargePowerNode(this, f, LargeFraction.zero);}
    @Override
    public LargeDecimal approximateToDecimal(LargeInteger precision) {return null;}
    @Override
    public boolean equalsTo(Atom other) {throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");}
}
