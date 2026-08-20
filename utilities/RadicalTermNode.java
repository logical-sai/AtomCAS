package com.AtomCAS.utilities;
public class RadicalTermNode extends ScalarAtom{
    public final LargeFraction rationalPart;
    public final LargeFraction coefficient;
    public final LargePowerNode exponent;
    public RadicalTermNode(LargeFraction rationalPart, LargeFraction coefficient, LargePowerNode base){
        LargeFraction R = rationalPart.copy();
        LargeFraction C = coefficient.copy();
        LargePowerNode K = base.copy();
        if(K.equals(LargeInteger.ONE)){
            this.rationalPart = (LargeFraction) R.add(C);
            this.coefficient = LargeFraction.ZERO;
            this.exponent = new LargePowerNode(LargeFraction.ZERO, LargeFraction.ONE, LargeFraction.ONE);
            return;
        }else if(K.equals(LargeInteger.ZERO) || C.equals(LargeInteger.ZERO)){
            this.rationalPart = R;
            this.coefficient = LargeFraction.ZERO;
            this.exponent = new LargePowerNode(K.base, LargeFraction.ONE, LargeInteger.ONE);
            return;
        }
        this.rationalPart = R.copy();
        this.coefficient = C.copy();
        this.exponent = K.copy();
    }
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        if(rationalPart.doesntEqual(LargeInteger.ZERO))
            str.append(rationalPart);
        if(coefficient.equals(LargeInteger.ZERO)){
            if(str.isEmpty()) str.append(rationalPart);
            return str.toString();
        }else if(coefficient.greaterThan(LargeInteger.ZERO)){
            if(rationalPart.doesntEqual(LargeInteger.ZERO)) str.append(" + ");
            if(coefficient.doesntEqual(LargeInteger.ONE)) str.append(coefficient);
        }else if(coefficient.lessThan(LargeInteger.ZERO)){
            if(rationalPart.doesntEqual(LargeInteger.ZERO)) str.append(" - ");
            if(coefficient.doesntEqual(LargeInteger.ONE)) str.append(coefficient.abs());
        }
        if(coefficient.doesntEqual(LargeInteger.ONE))
            str.append("*");
        str.append(exponent);
        return str.toString();
    }

    @Override
    public String toLatex() {return "";}
    @Override
    public RadicalTermNode copy() {return new RadicalTermNode(rationalPart, coefficient, exponent);}
    @Override
    public boolean isRational(){return coefficient.equals(LargeInteger.ZERO);}
    @Override
    public boolean isReal() {return !isNotReal();}
    @Override
    public boolean isAlgebraic() {return exponent.isAlgebraic();}
    @Override
    public ScalarAtom realPart(){return this.sign().realPart().multiply(this.abs());}
    @Override
    public ScalarAtom imagPart(){return this.sign().imagPart().multiply(this.abs());}
    @Override
    public ScalarAtom abs() {throw new UnsupportedOperationException("Error: Operation with this type together not supported yet");}
    @Override
    public LargeInteger floor() {throw new UnsupportedOperationException("Error: Operation with this type not supported yet");}
    @Override
    public LargeInteger ceil() {throw new UnsupportedOperationException("Error: Operation with this type together not supported yet");}
    @Override
    public int compareMags(ScalarAtom other) {throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");}
    @Override
    public int compareTo(ScalarAtom other) {throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");}
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
    public ScalarAtom reciprocal(){return new LargePowerNode(this, LargeFraction.NEGATIVE_ONE, LargeFraction.ZERO);}
    @Override
    public ScalarAtom power(LargeFraction f) {return new LargePowerNode(this, f, LargeFraction.ZERO);}
    @Override
    public LargeDecimal approximateToDecimal(LargeInteger precision) {return null;}
    @Override
    public boolean equals(Atom other) {throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");}
}
