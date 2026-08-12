package com.AtomCAS.utilities;
public class RadicalTerm extends ScalarAtom{
    public final LargeFraction rationalPart;
    public final LargeFraction coefficient;
    public final LargePowerMonomial exponent;
    public RadicalTerm(LargeFraction r, LargeFraction c, LargePowerMonomial E){
        if(c.equals(LargeFraction.ZERO) || E.equals(LargeFraction.ZERO)){
            rationalPart = r.copy();
            coefficient = LargeFraction.ZERO;
            exponent = LargePowerMonomial.ZERO;
            return;
        }else if (E.isRational()) {
            LargeFraction signedBase = E.base;
            if (E.exponent2.equals(LargeInteger.ONE))
                signedBase = signedBase.negate();
            rationalPart = (LargeFraction) r.add(c.multiply(signedBase));
            coefficient = LargeFraction.ZERO;
            exponent = LargePowerMonomial.ZERO;
            return;
        }
        LargeFraction tempR = r.copy();
        LargeFraction tempC = c.copy();
        //make real radicand an integer
        LargePowerMonomial Enum = new LargePowerMonomial((LargeFraction) E.base.numerator.divide(LargeInteger.ONE), E.exponent, E.exponent2);
        LargePowerMonomial Edem = new LargePowerMonomial((LargeFraction) E.base.denominator.divide(LargeInteger.ONE), E.exponent);
        LargePowerMonomial Edem2 = new LargePowerMonomial((LargeFraction) E.base.denominator.divide(LargeInteger.ONE), (LargeFraction) LargeInteger.ONE.subtract(E.exponent.mod(LargeInteger.ONE)));
        Enum = (LargePowerMonomial) Enum.multiply(Edem2);
        Edem = (LargePowerMonomial) Edem.multiply(Edem2);
        tempC = (LargeFraction) tempC.divide(Edem.base);
        LargeMap<LargeInteger, LargeFraction> pr = Enum.factor();
        for(LargeInteger I: pr.keys()){
            LargeInteger expTemp = pr.get(I).intPart();
            LargeInteger basTemp = I.copy();
            while(expTemp.doesntEqual(LargeInteger.ZERO)){
                if(expTemp.isOdd()){
                    if(expTemp.lessThan(LargeInteger.ZERO))
                        tempC = (LargeFraction) tempC.divide(basTemp);
                    else tempC = (LargeFraction) tempC.multiply(basTemp);
                    if(expTemp.abs().equals(LargeInteger.ONE)) break;
                }
                basTemp = basTemp.square();
                expTemp = expTemp.longDivision(LargeInteger.TWO);
            }
            pr.put(I, pr.get(I).fracPart());
        }
        Enum = LargePowerMonomial.buildFromFactors(pr);
        if(Enum.equals(LargeFraction.ONE)){
            rationalPart = (LargeFraction) tempR.add(tempC);
            coefficient = LargeFraction.ZERO;
            exponent = LargePowerMonomial.ZERO;
            return;
        }
        rationalPart = tempR.copy();
        coefficient = tempC.copy();
        exponent = Enum.copy();
    }
    @Override
    public RadicalTerm copy(){return new RadicalTerm(rationalPart.copy(), coefficient.copy(), exponent.copy());}
    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        if(rationalPart.doesntEqual(LargeInteger.ZERO))
            str.append(rationalPart);
        if(coefficient.equals(LargeInteger.ZERO)){
            if(str.isEmpty()) str.append(rationalPart);
            return str.toString();
        }else if(coefficient.sign().equals(LargeInteger.NEGATIVE_ONE)){
            if(rationalPart.doesntEqual(LargeInteger.ZERO)) str.append(" - ");
            else str.append("-");
            if(coefficient.doesntEqual(LargeInteger.NEGATIVE_ONE)) str.append(coefficient.abs()).append("*");
        }else{
            if(rationalPart.doesntEqual(LargeInteger.ZERO)) str.append(" + ");
            if(coefficient.doesntEqual(LargeInteger.ONE)) str.append(coefficient.abs()).append("*");
        }
        str.append(exponent);
        return str.toString();
    }
    @Override
    public String toLatex() {
        return "";
    }
    @Override
    public int hashCode() {
        if (coefficient.equals(LargeFraction.ZERO))
            return rationalPart.hashCode();
        // General RadicalTerm structural hash
        int hash = 0;
        hash = hash * 31 + coefficient.hashCode();
        hash = hash * 31 + rationalPart.hashCode();
        hash = hash * 31 + exponent.hashCode();
        return hash;
    }
    @Override
    public boolean equals(Atom other) {
        if(!(other instanceof ScalarAtom)) return false;
        else if(other instanceof LargeInteger || other instanceof LargeFraction) return this.rationalPart.equals(other) && this.coefficient.equals(LargeFraction.ZERO);
        else if(other instanceof LargePowerMonomial){
            RadicalTerm newOther = new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, (LargePowerMonomial) other);
            return this.rationalPart.equals(newOther.rationalPart) && this.coefficient.equals(newOther.coefficient) && this.exponent.equals(newOther.exponent);
        }else if(other instanceof RadicalTerm) return this.rationalPart.equals(((RadicalTerm) other).rationalPart) && this.coefficient.equals(((RadicalTerm) other).coefficient) && this.exponent.equals(((RadicalTerm) other).exponent);
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public LargeInteger floor() {
        if(this.isNotReal()) throw new IllegalStateException("Error: Cannot floor or apply ceiling a non-real atom");
        if (coefficient.equals(LargeInteger.ZERO))
            return rationalPart.floor();
        LargePowerMonomial K = exponent.copy();
        LargeFraction lower = (LargeFraction) K.floor().divide(LargeInteger.ONE);
        LargeFraction upper = (LargeFraction) K.ceil().divide(LargeInteger.ONE);
        while (true) {
            LargeFraction xLow;
            LargeFraction xHigh;
            if(coefficient.sign().equals(LargeInteger.NEGATIVE_ONE)){
                xLow = (LargeFraction) rationalPart.add(coefficient.multiply(upper));
                xHigh = (LargeFraction) rationalPart.add(coefficient.multiply(lower));
            }else{
                xLow = (LargeFraction) rationalPart.add(coefficient.multiply(lower));
                xHigh = (LargeFraction) rationalPart.add(coefficient.multiply(upper));
            }
            if (xLow.floor().equals(xHigh.floor()))
                return xLow.floor();
            LargeFraction mediant = new LargeFraction((LargeInteger) lower.numerator.add(upper.numerator), (LargeInteger) lower.denominator.add(upper.denominator));
            if (mediant.greaterThan(K))
                upper = mediant;
            else
                lower = mediant;
        }
    }
    @Override
    public LargeInteger ceil() {
        if(coefficient.equals(LargeInteger.ZERO)) return rationalPart.ceil();
        return this.floor().add(1);
    }
    public boolean isReal(){return exponent.isReal();}
    @Override
    public boolean isAlgebraic() {return true;}
    @Override
    public ScalarAtom realPart(){return this.sign().realPart().multiply(this.abs());}
    @Override
    public ScalarAtom imagPart(){return this.sign().imagPart().multiply(this.abs());}
    @Override
    public ScalarAtom abs(){
        if(this.isReal()){
            if(this.floor().sign().equals(LargeInteger.NEGATIVE_ONE))
                return new RadicalTerm(rationalPart.negate(), coefficient.negate(), exponent.copy());
            return this.copy();
        }
        if(rationalPart.equals(LargeInteger.ZERO))
            return new RadicalTerm(LargeFraction.ZERO, coefficient.abs(), exponent.abs());
        ScalarAtom normSquared = this.multiply(this.complexConjugate());
        if(normSquared.isRational())
            return new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, new LargePowerMonomial(((RadicalTermSum)normSquared).constant, LargeFraction.HALF));
        return new LargePowerNode(normSquared, LargeFraction.HALF, LargeFraction.ONE);
    }
    public RadicalTerm complexConjugate(){return new RadicalTerm(rationalPart, coefficient, exponent.complexConjugate());}
    @Override
    public int compareMags(ScalarAtom other){
        if(other instanceof LargeInteger || other instanceof LargeFraction){
            if(coefficient.equals(LargeInteger.ZERO)) return rationalPart.compareMags(other);
            LargeFraction f = (LargeFraction) other.subtract(rationalPart).divide(coefficient);
            f = f.power(exponent.exponent.denominator.lcm(exponent.exponent2.denominator));
            LargeFraction o = exponent.power(new LargeFraction(exponent.exponent.denominator.lcm(exponent.exponent2.denominator), LargeInteger.ONE)).base;
            return o.compareMags(f);
        }
        return this.abs().compareTo(other.abs());
    }
    @Override
    public int compareTo(ScalarAtom other) {
        if(this.isNotReal() || other.isNotReal()) throw new IllegalStateException("Error: Complex numbers cannot be compared");
        switch (other) {
            case LargeInteger _, LargeFraction _ -> {
                ScalarAtom r = this.subtract(other).sign();
                if (r.equals(LargeInteger.NEGATIVE_ONE)) return -1;
                else if (r.equals(LargeInteger.ONE)) return 1;
                return 0;
            }case LargePowerMonomial _ -> {
                LargeFraction a1 = (LargeFraction) this.floor().divide(LargeInteger.ONE);
                LargeFraction b1 = (LargeFraction) this.ceil().divide(LargeInteger.ONE);
                LargeFraction a2 = (LargeFraction) other.floor().divide(LargeInteger.ONE);
                LargeFraction b2 = (LargeFraction) other.ceil().divide(LargeInteger.ONE);
                if (this.equals(other)) return 0;
                while (true) {
                    if (b1.lesserThanOrEqualTo(a2)) return -1;
                    else if (b2.lesserThanOrEqualTo(a1)) return 1;
                    LargeFraction mediant = new LargeFraction((LargeInteger) a1.numerator.add(b1.numerator), (LargeInteger) a1.denominator.add(b1.denominator));
                    if (mediant.lessThan(this)) a1 = mediant;
                    else b1 = mediant;
                    LargeFraction mediant2 = new LargeFraction((LargeInteger) a2.numerator.add(b2.numerator), (LargeInteger) a2.denominator.add(b2.denominator));
                    if (mediant2.lessThan(other)) a2 = mediant2;
                    else b2 = mediant2;
                }
            }case RadicalTerm _ -> {
                LargeFraction a1 = (LargeFraction) this.floor().divide(LargeInteger.ONE);
                LargeFraction b1 = (LargeFraction) this.ceil().divide(LargeInteger.ONE);
                LargeFraction a2 = (LargeFraction) other.floor().divide(LargeInteger.ONE);
                LargeFraction b2 = (LargeFraction) other.ceil().divide(LargeInteger.ONE);
                if (this.equals(other)) return 0;
                while (true) {
                    if (a2.greaterThan(b1)) return -1;
                    if (a1.greaterThan(b2)) return 1;
                    LargeFraction mediant = new LargeFraction((LargeInteger) a1.numerator.add(b1.numerator), (LargeInteger) a1.denominator.add(b1.denominator));
                    if (mediant.lessThan(this)) a1 = mediant;
                    else b1 = mediant;
                    LargeFraction mediant2 = new LargeFraction((LargeInteger) a2.numerator.add(b2.numerator), (LargeInteger) a2.denominator.add(b2.denominator));
                    if (mediant2.lessThan(other)) a2 = mediant2;
                    else b2 = mediant2;
                }
            }
            default -> throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
        }
    }
    @Override
    public RadicalTerm negate(){return new RadicalTerm(rationalPart.negate(), coefficient.negate(), exponent);}
    @Override
    public boolean isRational() {return coefficient.equals(LargeInteger.ZERO);}
    @Override
    public ScalarAtom add(ScalarAtom other){
        if(other instanceof LargeInteger || other instanceof LargeFraction)
            return new RadicalTerm((LargeFraction) this.rationalPart.add(other), this.coefficient, exponent);
        else if(other instanceof LargePowerMonomial){
            LargeList<RadicalTerm> rt = new LargeList<>();
            rt.add(this);
            rt.add(new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, (LargePowerMonomial) other));
            return new RadicalTermSum(rt, LargeFraction.ZERO);
        }else if(other instanceof RadicalTerm){
            LargeList<RadicalTerm> rt = new LargeList<>();
            rt.add(this);
            rt.add((RadicalTerm) other);
            return new RadicalTermSum(rt, LargeFraction.ZERO);
        }else if(other instanceof RadicalTermSum R){
            LargeList<RadicalTerm> rt = new LargeList<>();
            rt.add(this);
            for(LargePowerMonomial L: R.sum.keys())
                rt.add(new RadicalTerm(LargeFraction.ZERO, R.sum.get(L), L));
            return new RadicalTermSum(rt, R.constant);
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public ScalarAtom multiply(ScalarAtom f) {
        if(f instanceof LargeFraction || f instanceof LargeInteger) return new RadicalTerm((LargeFraction) this.rationalPart.multiply(f), (LargeFraction) this.coefficient.multiply(f), this.exponent);
        else if(f instanceof LargePowerMonomial) {
            LargeList<RadicalTerm> rt = new LargeList<>();
            rt.add(new RadicalTerm(LargeFraction.ZERO, this.rationalPart, (LargePowerMonomial) f));
            rt.add(new RadicalTerm(LargeFraction.ZERO, this.coefficient, (LargePowerMonomial) exponent.multiply(f)));
            return new RadicalTermSum(rt, LargeFraction.ZERO);
        }else if(f instanceof RadicalTerm){
            LargeList<RadicalTerm> rt = new LargeList<>();
            LargeFraction cons = (LargeFraction) rationalPart.multiply(((RadicalTerm) f).rationalPart);
            LargePowerMonomial L = ((RadicalTerm) f).exponent.copy();
            rt.add(new RadicalTerm(LargeFraction.ZERO, (LargeFraction) this.rationalPart.multiply(((RadicalTerm) f).coefficient), L));
            L = exponent.copy();
            rt.add(new RadicalTerm(LargeFraction.ZERO, (LargeFraction) this.coefficient.multiply(((RadicalTerm) f).rationalPart), L));
            L = (LargePowerMonomial) exponent.multiply(((RadicalTerm) f).exponent);
            rt.add(new RadicalTerm(LargeFraction.ZERO, (LargeFraction) this.coefficient.multiply(((RadicalTerm) f).coefficient), L));
            return new RadicalTermSum(rt, cons);
        }
        return f.multiply(this);
    }
    @Override
    public LargePowerNode power(LargeFraction f){return new LargePowerNode(this, f, LargeFraction.ONE);}
    @Override
    public LargeDecimal approximateToDecimal(long precision) {return null;}
    @Override
    public ScalarAtom reciprocal(){
        if(this.coefficient.equals(LargeInteger.ZERO))
            return rationalPart.reciprocal();
        else if(rationalPart.equals(LargeInteger.ZERO))
            return new RadicalTerm(LargeFraction.ZERO, this.coefficient.reciprocal(), exponent.reciprocal());
        else if(this.equals(LargeInteger.ZERO)) throw new ArithmeticException("Error: Cannot divide by 0");
        // reciprocal of a + b*x, where x^n is rational
        LargeFraction a = rationalPart.copy();
        LargeFraction b = this.coefficient.copy();
        LargeInteger n = exponent.exponent.denominator.lcm(exponent.exponent2.denominator);
        LargePowerMonomial qAtom = exponent.power(new LargeFraction(n, LargeInteger.ONE));
        if (!qAtom.isRational())
            throw new ArithmeticException("Error: x^n is not rational");
        LargeFraction q = qAtom.base;
        if(qAtom.exponent2.equals(LargeInteger.ONE)) q = q.negate();
        LargeList<LargeFraction> coefficient = new LargeList<>();
        LargeFraction aPow = a.power(n.subtract(1));
        LargeFraction bPow = LargeFraction.ONE;
        for (LargeInteger k = LargeInteger.ZERO; k.lessThan(n); k = k.add(1)) {
            LargeFraction c = (LargeFraction) aPow.multiply(bPow);
            if (k.isOdd()) c = c.negate();
            coefficient.add(c);
            aPow = (LargeFraction) aPow.divide(a);
            bPow = (LargeFraction) bPow.multiply(b);
        }

        LargeFraction denominator = (LargeFraction) a.power(n).subtract(b.negate().power(n).multiply(q));

        LargeList<RadicalTerm> map = new LargeList<>();
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(coefficient.size()); i = i.add(1))
            map.add(new RadicalTerm(LargeFraction.ZERO,(LargeFraction) coefficient.get(i).divide(denominator), exponent.power(new LargeFraction(i, LargeInteger.ONE))));
        return new RadicalTermSum(map, LargeFraction.ZERO);
    }
}
