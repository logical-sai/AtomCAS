package com.AtomCAS.utilities;
public class LargePowerNode extends ScalarAtom{
    public final ScalarAtom signFactor; //MUST have abs 1
    public final ScalarAtom base;
    public final LargeFraction exponent;
    public LargePowerNode(ScalarAtom base, LargeFraction exponent, ScalarAtom signFactor){
        if(base instanceof LargeInteger || base instanceof LargeFraction){
            LargePowerMonomial L = new LargePowerMonomial((LargeFraction) base.divide(LargeInteger.ONE), exponent);
            this.base = L.base;
            this.exponent = L.exponent;
            this.signFactor = signFactor.multiply(new LargePowerMonomial(LargeFraction.NEGATIVE_ONE, L.exponent2));
            return;
        }else if(base instanceof LargePowerMonomial){
            LargePowerMonomial L = (LargePowerMonomial) base.power(exponent);
            this.base = L.base;
            this.exponent = L.exponent;
            this.signFactor = signFactor.multiply(new LargePowerMonomial(LargeFraction.NEGATIVE_ONE, L.exponent2));
            return;
        }else if(base instanceof RadicalTerm R){
            if(R.rationalPart.equals(LargeInteger.ZERO)){
                LargePowerMonomial L = (LargePowerMonomial) R.exponent.power(exponent).multiply(new LargePowerMonomial(R.coefficient, exponent));
                this.base = L.base;
                this.exponent = L.exponent;
                this.signFactor = signFactor.multiply(new LargePowerMonomial(LargeFraction.NEGATIVE_ONE, L.exponent2));
                return;
            }else if(R.coefficient.equals(LargeInteger.ZERO)){
                LargePowerMonomial L = R.rationalPart.power(exponent);
                this.base = L.base;
                this.exponent = L.exponent;
                this.signFactor = signFactor.multiply(new LargePowerMonomial(LargeFraction.NEGATIVE_ONE, L.exponent2));
                return;
            }
            //radical denesting attempt
            if(R.exponent.exponent.equals(LargeFraction.HALF) && R.exponent.exponent2.equals(LargeFraction.ZERO) && exponent.denominator.equals(LargeInteger.TWO)){
                //√(A+B√C) = √x + √y -> A+B√C = x + y + 2√xy. x+y=A, B^2*C/4=xy
                //y = A-x -> B^2*C/4 = Ax-x^2 -> x^2 + Ax + B^2*C/4 = 0
                //x = (A + √(A^2 - B^2*C))/2. y = (A - √(A^2 - B^2*C))/2
                LargeFraction A = R.rationalPart.copy();
                LargeFraction BCoefficient = R.coefficient.copy();
                LargeFraction C = R.exponent.base.copy();
                if(R.lessThan(LargeInteger.ZERO)){
                    signFactor = signFactor.multiply(LargePowerMonomial.I);
                    A = A.negate();
                    BCoefficient = BCoefficient.negate();
                    base = R.negate();
                }

                ScalarAtom discriminant = A.square().subtract(BCoefficient.square().multiply(C));
                ScalarAtom sqrtDisc = discriminant.power(LargeFraction.HALF);

                RadicalTerm xAtom = (RadicalTerm) A.add(sqrtDisc).divide(LargeInteger.TWO);
                RadicalTerm yAtom = (RadicalTerm) A.subtract(sqrtDisc).divide(LargeInteger.TWO);
                if (xAtom.sign().equals(LargeInteger.NEGATIVE_ONE) || yAtom.sign().equals(LargeInteger.NEGATIVE_ONE)){
                    this.base = base;
                    this.exponent = exponent;
                    this.signFactor = signFactor;
                    return;
                }
                if (xAtom.isRational() && yAtom.isRational()) {
                    RadicalTermSum result = new RadicalTermSum();
                    ScalarAtom sqrtX = xAtom.rationalPart.power(LargeFraction.HALF);
                    ScalarAtom sqrtY = yAtom.rationalPart.power(LargeFraction.HALF);
                    result = (RadicalTermSum) result.add(sqrtX);

                    if (BCoefficient.sign().equals(LargeInteger.NEGATIVE_ONE))
                        result = (RadicalTermSum) result.subtract(sqrtY);
                    else
                        result = (RadicalTermSum) result.add(sqrtY);
                    result = result.power(exponent.numerator);
                    this.base = result;
                    this.exponent = LargeFraction.ONE;
                    this.signFactor = signFactor;
                    return;
                }
            }
        }else if(base instanceof RadicalTermSum R){
            if(R.sum.size().equals(LargeInteger.ZERO)){
                LargePowerNode G = new LargePowerNode(R.constant, exponent, signFactor);
                this.base = G.base;
                this.exponent = G.exponent;
                this.signFactor = G.signFactor;
                return;
            }else if(R.sum.size().equals(LargeInteger.ONE)){
                LargePowerNode G = new LargePowerNode(R.constant.add(R.sum.keyAtIndex(LargeInteger.ZERO).multiply(R.sum.get(R.sum.keyAtIndex(LargeInteger.ZERO)))), exponent, signFactor);
                this.base = G.base;
                this.exponent = G.exponent;
                this.signFactor = G.signFactor;
                return;
            }
        }
        ScalarAtom basTemp = (ScalarAtom) base.copy();
        LargeInteger expTemp = exponent.numerator.copy();
        ScalarAtom res = LargeFraction.ONE;
        while(true){
            if(expTemp.isOdd()){
                res = res.multiply(basTemp);
                if(expTemp.equals(LargeInteger.ONE)) break;
            }
            basTemp = basTemp.square();
            expTemp = expTemp.longDivision(LargeInteger.TWO);
        }
        this.base = res;
        this.exponent = exponent.denominator.reciprocal();
        this.signFactor = signFactor;
    }
    @Override
    public LargePowerNode copy() {return new LargePowerNode(base, exponent, signFactor);}
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        if(base.equals(LargeInteger.ZERO)){
            str.append(base);
            return str.toString();
        }
        if(signFactor.equals(LargeFraction.NEGATIVE_ONE)){
            if(exponent.equals(LargeInteger.ONE)){
                str.append(base.negate());
                return str.toString();
            }else str.append("-");
        }
        else if(signFactor.doesntEqual(LargeFraction.ONE)) str.append(signFactor);
        if(base.equals(LargeInteger.ONE)){
            if(str.isEmpty() || str.toString().equals("-")) str.append("1");
            return str.toString();
        }
        if (exponent.doesntEqual(LargeInteger.ONE)) {
            switch (base) {
                case LargeInteger _ -> str.append(base);
                case LargeFraction largeFraction -> {
                    if (largeFraction.denominator.equals(LargeInteger.ONE) || exponent.denominator.equals(LargeInteger.ONE) && signFactor.equals(LargeFraction.ONE))
                        str.append(base);
                    else str.append("(").append(base).append(")");
                }
                case RadicalTerm radicalTerm -> {
                    if (radicalTerm.coefficient.equals(LargeFraction.ZERO) && radicalTerm.rationalPart.denominator.equals(LargeInteger.ONE) && signFactor.equals(LargeFraction.ONE))
                        str.append(base);
                    else str.append("(").append(base).append(")");
                }
                case RadicalTermSum rt -> {
                    if (rt.sum.size().equals(LargeInteger.ZERO) && rt.constant.denominator.equals(LargeInteger.ONE) && signFactor.equals(LargeFraction.ONE))
                        str.append(base);
                    else str.append("(").append(base).append(")");
                }
                default -> str.append("(").append(base).append(")");
            }
        }else{
            if(signFactor.equals(LargeFraction.NEGATIVE_ONE) || signFactor.equals(LargeFraction.ONE))
                str.append(base);
            else str.append("(").append(base).append(")");
        }
        if(exponent.doesntEqual(LargeFraction.ONE))
            str.append("^(").append(exponent).append(")");
        return str.toString();
    }

    @Override
    public String toLatex() {return "";}
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + base.hashCode();
        hash = 31 * hash + exponent.hashCode();
        hash = 31 * hash + signFactor.hashCode();
        return hash;
    }
    @Override
    public boolean equals(Atom other) {
        if(!(other instanceof ScalarAtom)) return false;
        if(other.equals(LargeInteger.ZERO)) return base.equals(LargeInteger.ZERO);
        if (other instanceof LargeInteger || other instanceof LargeFraction)
            return base.equals(((ScalarAtom) other).abs()) && exponent.equals(LargeInteger.ONE) && signFactor.equals(((ScalarAtom) other).sign());
        else if (other instanceof LargePowerNode h)
            return this.base.equals(h.base) && this.exponent.equals(h.exponent) && this.signFactor.equals(h.signFactor);
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public ScalarAtom negate() {return new LargePowerNode(base, exponent, signFactor.negate());}
    @Override
    public ScalarAtom abs() {return new LargePowerNode(base, exponent, LargeInteger.ONE);}
    @Override
    public int compareMags(ScalarAtom other){return 0;}
    @Override
    public int compareTo(ScalarAtom other){return 0;}
    @Override
    public LargeInteger floor() {
        return null;
    }
    @Override
    public LargeInteger ceil() {
        return null;
    }
    @Override
    public boolean isRational() {return base.isRational() && exponent.equals(LargeInteger.ONE) && (signFactor.equals(LargeInteger.ONE) || signFactor.equals(LargeInteger.ZERO));}
    @Override
    public boolean isReal() {return signFactor.equals(LargeInteger.ONE) || signFactor.equals(LargeInteger.NEGATIVE_ONE);}
    @Override
    public boolean isAlgebraic() {return base.isAlgebraic() && signFactor.isAlgebraic();}
    @Override
    public ScalarAtom realPart(){
        return signFactor.realPart().multiply(this.abs());
    }
    public ScalarAtom imagPart(){
        return signFactor.imagPart().multiply(this.abs());
    }
    @Override
    public ScalarAtom add(ScalarAtom other) {
        if(other instanceof LargeFraction || other instanceof LargeInteger)
            return new RadicalTermNode((LargeFraction) other.divide(LargeInteger.ONE), LargeFraction.ONE, this);
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        if(other.equals(LargeInteger.ZERO)) return LargeInteger.ZERO;
        else if(other.equals(LargeInteger.ONE)) return this.copy();
        if (other instanceof LargePowerNode h){
            if (this.base.equals(h.base))
                return new LargePowerNode(this.base, (LargeFraction) this.exponent.add(h.exponent), this.signFactor.multiply(h.signFactor));
            else if(this.exponent.equals(h.exponent))
                return new LargePowerNode(this.base.multiply(h.base), this.exponent, this.signFactor.multiply(h.signFactor));
            LargeFraction GC = exponent.gcd(h.exponent);
            ScalarAtom result = LargeInteger.ONE;
            LargeInteger expTemp = ((LargeFraction)exponent.divide(GC)).numerator;
            ScalarAtom basTemp = (ScalarAtom) base.copy();
            while(true){
                if(expTemp.isOdd()){
                    if(expTemp.lessThan(LargeInteger.ZERO))
                        result = result.divide(basTemp);
                    else result = result.multiply(basTemp);
                    if(expTemp.abs().equals(LargeInteger.ONE)) break;
                }
                basTemp = basTemp.square();
                expTemp = expTemp.longDivision(LargeInteger.TWO);
            }
            expTemp = ((LargeFraction)h.exponent.divide(GC)).numerator;
            basTemp = (ScalarAtom) h.base.copy();
            while(true){
                if(expTemp.isOdd()){
                    if(expTemp.lessThan(LargeInteger.ZERO))
                        result = result.divide(basTemp);
                    else result = result.multiply(basTemp);
                    if(expTemp.abs().equals(LargeInteger.ONE)) break;
                }
                basTemp = basTemp.square();
                expTemp = expTemp.longDivision(LargeInteger.TWO);
            }
            result = Parser.parse(result.toString());
            return new LargePowerNode(result, GC, signFactor.multiply(h.signFactor));
        }
        return this.multiply(new LargePowerNode(other.abs(), LargeFraction.ONE, other.sign()));
    }
    @Override
    public LargePowerNode reciprocal() {return new LargePowerNode(base.reciprocal(), exponent, signFactor.reciprocal());}
    public LargePowerNode power(LargeFraction other){return new LargePowerNode(base, (LargeFraction) exponent.multiply(other), signFactor.power(other));}

    @Override
    public LargeDecimal approximateToDecimal(long precision) {return null;}
    @Override
    public ScalarAtom square() {return this.multiply(this);}
    @Override
    public ScalarAtom cube() {return this.multiply(this).multiply(this);}
}
