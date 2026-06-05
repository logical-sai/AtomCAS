package com.AtomCAS.utilities;
public class LargePowerNode extends ScalarAtom{
    public final ScalarAtom signFactor; //MUST have magnitude 1
    public final ScalarAtom base;
    public final LargeFraction exponent;
    public LargePowerNode(ScalarAtom base, LargeFraction exponent, ScalarAtom signFactor){
        if(base instanceof LargeInteger || base instanceof LargeFraction){
            LargePowerMonomial L = new LargePowerMonomial((LargeFraction) base.divide(LargeInteger.one), exponent);
            this.base = L.base;
            this.exponent = L.exponent;
            this.signFactor = signFactor.multiply(new LargePowerMonomial(LargeFraction.negativeOne, L.exponent2));
            return;
        }else if(base instanceof LargePowerMonomial){
            LargePowerMonomial L = (LargePowerMonomial) base.power(exponent);
            this.base = L.base;
            this.exponent = L.exponent;
            this.signFactor = signFactor.multiply(new LargePowerMonomial(LargeFraction.negativeOne, L.exponent2));
            return;
        }else if(base instanceof RadicalTerm){
            if(((RadicalTerm) base).rationalPart.equalsTo(LargeInteger.zero)){
                LargePowerMonomial L = (LargePowerMonomial) ((RadicalTerm) base).exponent.power(exponent).multiply(new LargePowerMonomial(((RadicalTerm) base).coefficient, exponent));
                this.base = L.base;
                this.exponent = L.exponent;
                this.signFactor = signFactor.multiply(new LargePowerMonomial(LargeFraction.negativeOne, L.exponent2));
                return;
            }
        }
        this.base = base.magnitude();
        this.exponent = exponent;
        this.signFactor = signFactor.multiply(base.sign().power(exponent));
    }
    @Override
    public LargePowerNode copy() {return new LargePowerNode(base, exponent, signFactor);}
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        if(base.equalsTo(LargeInteger.zero)){
            str.append(base);
            return str.toString();
        }
        if(signFactor.equalsTo(LargeFraction.negativeOne)) str.append("-");
        else if(signFactor.notEqualsTo(LargeFraction.one)) str.append(signFactor);
        if(base.equalsTo(LargeInteger.one)){
            if(str.isEmpty() || str.toString().equals("-")) str.append("1");
            return str.toString();
        }
        if (exponent.notEqualsTo(LargeInteger.one)) {
            switch (base) {
                case LargeInteger _ -> str.append(base);
                case LargeFraction largeFraction -> {
                    if (largeFraction.denominator.equalsTo(LargeInteger.one) || exponent.denominator.equalsTo(LargeInteger.one))
                        str.append(base);
                    else str.append("(").append(base).append(")");
                }
                case RadicalTerm radicalTerm -> {
                    if (radicalTerm.coefficient.equalsTo(LargeFraction.zero)) {
                        if (radicalTerm.rationalPart.denominator.equalsTo(LargeInteger.one))
                            str.append(base);
                    } else str.append("(").append(base).append(")");
                }
                case RadicalTermSum rt -> {
                    if (rt.sum.size().equalsTo(LargeInteger.zero)) {
                        if (rt.constant.denominator.equalsTo(LargeInteger.one))
                            str.append(base);
                    } else str.append("(").append(base).append(")");
                }
                default -> str.append("(").append(base).append(")");
            }
        }else str.append(base);
        if(exponent.notEqualsTo(LargeFraction.one))
            str.append("^(").append(exponent).append(")");
        return str.toString();
    }
    @Override
    public boolean equalsTo(Atom other) {
        if(!(other instanceof ScalarAtom)) return false;
        if(other.equalsTo(LargeInteger.zero)) return base.equalsTo(LargeInteger.zero);
        if (other instanceof LargeInteger || other instanceof LargeFraction)
            return base.equalsTo(((ScalarAtom) other).magnitude()) && exponent.equalsTo(LargeInteger.one) && signFactor.equalsTo(((ScalarAtom) other).sign());
        else if (other instanceof LargePowerNode h)
            return this.base.equalsTo(h.base) && this.exponent.equalsTo(h.exponent) && this.signFactor.equalsTo(h.signFactor);
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public boolean notEqualsTo(Atom other) {return !equalsTo(other);}
    @Override
    public ScalarAtom negate() {return new LargePowerNode(base, exponent, signFactor.negate());}
    @Override
    public ScalarAtom sign() {return signFactor;}
    @Override
    public ScalarAtom magnitude() {return new LargePowerNode(base, exponent, LargeFraction.zero);}
    @Override
    public long compareMags(ScalarAtom other){
        return 0;
    }
    @Override
    public long compareTo(ScalarAtom other){
        return 0;
    }
    @Override
    public boolean greaterThan(ScalarAtom other) {return this.compareTo(other) > 0;}
    @Override
    public boolean lessThan(ScalarAtom other) {return this.compareTo(other) < 0;}
    @Override
    public boolean greaterThanOrEqualTo(ScalarAtom other) {return this.compareTo(other) >= 0;}
    public boolean lesserThanOrEqualTo(ScalarAtom other) {return this.compareTo(other) <= 0;}

    @Override
    public LargeInteger floor() {
        return null;
    }
    @Override
    public LargeInteger ceil() {
        return null;
    }
    @Override
    public boolean isRational() {return base.isRational() && exponent.equalsTo(LargeInteger.one) && (signFactor.equalsTo(LargeInteger.one) || signFactor.equalsTo(LargeInteger.zero));}
    @Override
    public boolean isReal() {return signFactor.equalsTo(LargeInteger.one) || signFactor.equalsTo(LargeInteger.negativeOne);}
    @Override
    public boolean isNotReal() {return !isReal();}
    @Override
    public ScalarAtom add(ScalarAtom other) {
        if(other instanceof LargeFraction || other instanceof LargeInteger)
            return new RadicalTermNode((LargeFraction) other.divide(LargeInteger.one), LargeFraction.one, this);
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public ScalarAtom subtract(ScalarAtom other) {return this.add(other.negate());}
    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        if(other.equalsTo(LargeInteger.zero)) return LargeInteger.zero;
        else if(other.equalsTo(LargeInteger.one)) return this.copy();
        if (other instanceof LargePowerNode h){
            if (this.base.equalsTo(h.base))
                return new LargePowerNode(this.base, (LargeFraction) this.exponent.add(h.exponent), this.signFactor.multiply(h.signFactor));
            else if(this.exponent.equalsTo(h.exponent))
                return new LargePowerNode(this.base.multiply(h.base), this.exponent, this.signFactor.multiply(h.signFactor));
            LargeInteger lk = this.exponent.denominator.lcm(h.exponent.denominator);
            LargePowerNode M = (LargePowerNode) this.magnitude().power((LargeFraction) lk.divide(exponent));
            LargePowerNode N = (LargePowerNode) h.magnitude().power((LargeFraction) lk.divide(h.exponent));
            return new LargePowerNode(M.base.multiply(N.base), lk.reciprocal(), signFactor.multiply(N.signFactor));
        }else if (other instanceof LargeInteger || other instanceof LargeFraction || other instanceof LargePowerMonomial || other instanceof RadicalTerm || other instanceof RadicalTermSum)
            return this.multiply(new LargePowerNode(other, LargeFraction.one, LargeFraction.zero));
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }

    @Override
    public ScalarAtom divide(ScalarAtom other) {return this.multiply(other.reciprocal());}
    @Override
    public LargePowerNode reciprocal() {return new LargePowerNode(base, exponent.negate(), signFactor);}
    @Override
    public ScalarAtom mod(ScalarAtom other) {return null;}
    public LargePowerNode power(LargeFraction other){return new LargePowerNode(base, (LargeFraction) exponent.multiply(other), signFactor.power(other));}
    @Override
    public ScalarAtom square() {return this.multiply(this);}
    @Override
    public ScalarAtom cube() {return this.multiply(this).multiply(this);}
    @Override
    public ScalarAtom percent() {return this.multiply(Parser.parse("0.01"));}
    @Override
    public ScalarAtom permille() {return this.multiply(Parser.parse("0.001"));}
}
