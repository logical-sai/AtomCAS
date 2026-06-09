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
        }else if(base instanceof RadicalTerm R){
            if(R.rationalPart.equalsTo(LargeInteger.zero)){
                LargePowerMonomial L = (LargePowerMonomial) R.exponent.power(exponent).multiply(new LargePowerMonomial(R.coefficient, exponent));
                this.base = L.base;
                this.exponent = L.exponent;
                this.signFactor = signFactor.multiply(new LargePowerMonomial(LargeFraction.negativeOne, L.exponent2));
                return;
            }else if(R.coefficient.equalsTo(LargeInteger.zero)){
                LargePowerMonomial L = R.rationalPart.power(exponent);
                this.base = L.base;
                this.exponent = L.exponent;
                this.signFactor = signFactor.multiply(new LargePowerMonomial(LargeFraction.negativeOne, L.exponent2));
                return;
            }
            //radical denesting attempt
            if(R.exponent.exponent.equalsTo(LargeFraction.half) && R.exponent.exponent2.equalsTo(LargeFraction.zero) && exponent.equalsTo(LargeFraction.half)){
                //√(A+B√C) = √x + √y -> A+B√C = x + y + 2√xy. x+y=A, B^2*C/4=xy
                //y = A-x -> B^2*C/4 = Ax-x^2 -> x^2 + Ax + B^2*C/4 = 0
                //x = (-A + √(A^2 - B^2*C))/2. y = (-A - √(A^2 - B^2*C))/2
                LargeFraction A = R.rationalPart.copy();
                LargeFraction Bcoeff = R.coefficient.copy();
                LargeFraction C = R.exponent.base.copy();
                if(R.lessThan(LargeInteger.zero)){
                    signFactor = signFactor.multiply(LargePowerMonomial.i);
                    A = A.negate();
                    Bcoeff = Bcoeff.negate();
                    base = R.negate();
                }

                ScalarAtom discriminant = A.square().subtract(Bcoeff.square().multiply(C));
                ScalarAtom sqrtDisc = discriminant.power(LargeFraction.half);

                RadicalTerm xAtom = (RadicalTerm) A.add(sqrtDisc).divide(LargeInteger.two);
                RadicalTerm yAtom = (RadicalTerm) A.subtract(sqrtDisc).divide(LargeInteger.two);
                if (xAtom.sign().equalsTo(LargeInteger.negativeOne) || yAtom.sign().equalsTo(LargeInteger.negativeOne)){
                    this.base = base;
                    this.exponent = exponent;
                    this.signFactor = signFactor;
                    return;
                }
                if (xAtom.isRational() && yAtom.isRational()) {
                    RadicalTermSum result = new RadicalTermSum();
                    ScalarAtom sqrtX = xAtom.rationalPart.power(LargeFraction.half);
                    ScalarAtom sqrtY = yAtom.rationalPart.power(LargeFraction.half);
                    result = (RadicalTermSum) result.add(sqrtX);

                    if (Bcoeff.sign().equalsTo(LargeInteger.negativeOne))
                        result = (RadicalTermSum) result.subtract(sqrtY);
                    else
                        result = (RadicalTermSum) result.add(sqrtY);
                    this.base = result;
                    this.exponent = LargeFraction.one;
                    this.signFactor = signFactor;
                    return;
                }
            }
        }else if(base instanceof RadicalTermSum R){
            if(R.sum.size().equalsTo(LargeInteger.zero)){
                LargePowerNode G = new LargePowerNode(R.constant, exponent, signFactor);
                this.base = G.base;
                this.exponent = G.exponent;
                this.signFactor = signFactor.multiply(G.signFactor);
                return;
            }else if(R.sum.size().equalsTo(LargeInteger.one)){
                LargePowerNode G = new LargePowerNode(R.constant.add(R.sum.keyAtIndex(LargeInteger.zero).multiply(R.sum.get(R.sum.keyAtIndex(LargeInteger.zero)))), exponent, signFactor);
                this.base = G.base;
                this.exponent = G.exponent;
                this.signFactor = G.signFactor.multiply(signFactor);
                return;
            }
        }
        this.base = base;
        this.exponent = exponent;
        this.signFactor = signFactor;
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
                    if (largeFraction.denominator.equalsTo(LargeInteger.one) || exponent.denominator.equalsTo(LargeInteger.one) && signFactor.equalsTo(LargeFraction.one))
                        str.append(base);
                    else str.append("(").append(base).append(")");
                }
                case RadicalTerm radicalTerm -> {
                    if (radicalTerm.coefficient.equalsTo(LargeFraction.zero) && radicalTerm.rationalPart.denominator.equalsTo(LargeInteger.one) && signFactor.equalsTo(LargeFraction.one))
                        str.append(base);
                    else str.append("(").append(base).append(")");
                }
                case RadicalTermSum rt -> {
                    if (rt.sum.size().equalsTo(LargeInteger.zero) && rt.constant.denominator.equalsTo(LargeInteger.one) && signFactor.equalsTo(LargeFraction.one))
                        str.append(base);
                    else str.append("(").append(base).append(")");
                }
                default -> str.append("(").append(base).append(")");
            }
        }else{
            if(signFactor.equalsTo(LargeFraction.negativeOne) || signFactor.equalsTo(LargeFraction.one))
                str.append(base);
            else str.append("(").append(base).append(")");
        }
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
    public ScalarAtom magnitude() {return new LargePowerNode(base, exponent, LargeFraction.one);}
    @Override
    public long compareMags(ScalarAtom other){return 0;}
    @Override
    public long compareTo(ScalarAtom other){return 0;}
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
    public boolean isAlgebraic() {return base.isAlgebraic() && signFactor.isAlgebraic();}
    @Override
    public ScalarAtom add(ScalarAtom other) {
        if(other instanceof LargeFraction || other instanceof LargeInteger)
            return new RadicalTermNode((LargeFraction) other.divide(LargeInteger.one), LargeFraction.one, this);
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
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
        }
        return this.multiply(new LargePowerNode(other, LargeFraction.one, LargeFraction.zero));
    }
    @Override
    public LargePowerNode reciprocal() {return new LargePowerNode(base.reciprocal(), exponent, signFactor.reciprocal());}
    public LargePowerNode power(LargeFraction other){return new LargePowerNode(base, (LargeFraction) exponent.multiply(other), signFactor.power(other));}

    @Override
    public LargeDecimal approximateToDecimal(LargeInteger precision) {
        return null;
    }

    @Override
    public ScalarAtom square() {return this.multiply(this);}
    @Override
    public ScalarAtom cube() {return this.multiply(this).multiply(this);}
}
