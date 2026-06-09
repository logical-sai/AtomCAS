package com.AtomCAS.utilities;
public class RadicalTerm extends ScalarAtom{
    public final LargeFraction rationalPart;
    public final LargeFraction coefficient;
    public final LargePowerMonomial exponent;
    public RadicalTerm(LargeFraction r, LargeFraction c, LargePowerMonomial E){
        if(c.equalsTo(LargeFraction.zero) || E.equalsTo(LargeFraction.zero)){
            rationalPart = r.copy();
            coefficient = LargeFraction.zero;
            exponent = LargePowerMonomial.zero;
            return;
        }else if (E.isRational()) {
            LargeFraction signedBase = E.base;
            if (E.exponent2.equalsTo(LargeInteger.one))
                signedBase = signedBase.negate();
            rationalPart = (LargeFraction) r.add(c.multiply(signedBase));
            coefficient = LargeFraction.zero;
            exponent = LargePowerMonomial.zero;
            return;
        }
        LargeFraction tempR = r.copy();
        LargeFraction tempC = c.copy();
        //make real radicand an integer
        LargePowerMonomial Enum = new LargePowerMonomial((LargeFraction) E.base.numerator.divide(LargeInteger.one), E.exponent, E.exponent2);
        LargePowerMonomial Edem = new LargePowerMonomial((LargeFraction) E.base.denominator.divide(LargeInteger.one), E.exponent);
        LargePowerMonomial Edem2 = new LargePowerMonomial((LargeFraction) E.base.denominator.divide(LargeInteger.one), (LargeFraction) LargeInteger.one.subtract(E.exponent.mod(LargeInteger.one)));
        Enum = (LargePowerMonomial) Enum.multiply(Edem2);
        Edem = (LargePowerMonomial) Edem.multiply(Edem2);
        tempC = (LargeFraction) tempC.divide(Edem.base);
        LargeMap<LargeInteger, LargeFraction> pr = Enum.factor();
        for(LargeInteger I: pr){
            LargeInteger expTemp = pr.get(I).intPart();
            LargeInteger basTemp = I.copy();
            while(expTemp.notEqualsTo(LargeInteger.zero)){
                if(expTemp.isOdd()){
                    if(expTemp.sign().equalsTo(LargeInteger.negativeOne))
                        tempC = (LargeFraction) tempC.divide(basTemp);
                    else tempC = (LargeFraction) tempC.multiply(basTemp);
                    if(expTemp.magnitude().equalsTo(LargeInteger.one)) break;
                }
                basTemp = basTemp.square();
                expTemp = expTemp.longDivision(LargeInteger.two);
            }
            pr.put(I, pr.get(I).fracPart());
        }
        Enum = LargePowerMonomial.buildFromFactors(pr);
        if(Enum.equalsTo(LargeFraction.one)){
            rationalPart = (LargeFraction) tempR.add(tempC);
            coefficient = LargeFraction.zero;
            exponent = LargePowerMonomial.zero;
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
        if(rationalPart.notEqualsTo(LargeInteger.zero))
            str.append(rationalPart);
        if(coefficient.equalsTo(LargeInteger.zero)){
            if(str.isEmpty()) str.append(rationalPart);
            return str.toString();
        }else if(coefficient.sign().equalsTo(LargeInteger.negativeOne)){
            if(rationalPart.notEqualsTo(LargeInteger.zero)) str.append(" - ");
            else str.append("-");
            if(coefficient.notEqualsTo(LargeInteger.negativeOne)) str.append(coefficient.magnitude()).append("*");
        }else{
            if(rationalPart.notEqualsTo(LargeInteger.zero)) str.append(" + ");
            if(coefficient.notEqualsTo(LargeInteger.one)) str.append(coefficient.magnitude()).append("*");
        }
        str.append(exponent);
        return str.toString();
    }
    @Override
    public boolean equalsTo(Atom other) {
        if(!(other instanceof ScalarAtom)) return false;
        else if(other instanceof LargeInteger || other instanceof LargeFraction) return this.rationalPart.equalsTo(other) && this.coefficient.equalsTo(LargeFraction.zero);
        else if(other instanceof LargePowerMonomial){
            RadicalTerm newOther = new RadicalTerm(LargeFraction.zero, LargeFraction.one, (LargePowerMonomial) other);
            return this.rationalPart.equalsTo(newOther.rationalPart) && this.coefficient.equalsTo(newOther.coefficient) && this.exponent.equalsTo(newOther.exponent);
        }else if(other instanceof RadicalTerm) return this.rationalPart.equalsTo(((RadicalTerm) other).rationalPart) && this.coefficient.equalsTo(((RadicalTerm) other).coefficient) && this.exponent.equalsTo(((RadicalTerm) other).exponent);
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public boolean notEqualsTo(Atom other) {return !this.equalsTo(other);}
    @Override
    public LargeInteger floor() {
        if(this.isNotReal()) throw new IllegalStateException("Error: Cannot floor or ceil a non-real atom");
        if (coefficient.equalsTo(LargeInteger.zero))
            return rationalPart.floor();
        LargePowerMonomial K = exponent.copy();
        LargeFraction lower = (LargeFraction) K.floor().divide(LargeInteger.one);
        LargeFraction upper = (LargeFraction) K.ceil().divide(LargeInteger.one);
        while (true) {
            LargeFraction xLow;
            LargeFraction xHigh;
            if(coefficient.sign().equalsTo(LargeInteger.negativeOne)){
                xLow = (LargeFraction) rationalPart.add(coefficient.multiply(upper));
                xHigh = (LargeFraction) rationalPart.add(coefficient.multiply(lower));
            }else{
                xLow = (LargeFraction) rationalPart.add(coefficient.multiply(lower));
                xHigh = (LargeFraction) rationalPart.add(coefficient.multiply(upper));
            }
            if (xLow.floor().equalsTo(xHigh.floor()))
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
        if(coefficient.equalsTo(LargeInteger.zero)) return rationalPart.ceil();
        return (LargeInteger) this.floor().add(LargeInteger.one);
    }
    public ScalarAtom sign(){
        if(this.equalsTo(LargeInteger.zero)) return LargeInteger.zero;
        return this.divide(this.magnitude());
    }

    public boolean isNotReal(){return exponent.isNotReal();}
    public boolean isReal(){return exponent.isReal();}
    @Override
    public boolean isAlgebraic() {return true;}
    @Override
    public ScalarAtom magnitude(){
        if(this.isReal()){
            if(this.floor().sign().equalsTo(LargeInteger.negativeOne))
                return new RadicalTerm(rationalPart.negate(), coefficient.negate(), exponent.copy());
            return this.copy();
        }
        if(rationalPart.equalsTo(LargeInteger.zero))
            return new RadicalTerm(LargeFraction.zero, coefficient.magnitude(), exponent.magnitude());
        ScalarAtom normSquared = this.multiply(this.conjugate());
        if(normSquared.isRational())
            return new RadicalTerm(LargeFraction.zero, LargeFraction.one, new LargePowerMonomial(((RadicalTermSum)normSquared).constant, LargeFraction.half));
        return new LargePowerNode(normSquared, LargeFraction.half, LargeFraction.one);
    }
    public RadicalTerm conjugate(){return new RadicalTerm(rationalPart, coefficient.negate(), exponent);}
    @Override
    public long compareMags(ScalarAtom other){
        if(other instanceof LargeInteger || other instanceof LargeFraction){
            if(coefficient.equalsTo(LargeInteger.zero)) return rationalPart.compareMags(other);
            LargeFraction f = (LargeFraction) other.subtract(rationalPart).divide(coefficient);
            f = f.power(exponent.exponent.denominator.lcm(exponent.exponent2.denominator));
            LargeFraction o = exponent.power(new LargeFraction(exponent.exponent.denominator.lcm(exponent.exponent2.denominator), LargeInteger.one)).base;
            return o.compareMags(f);
        }
        return this.magnitude().compareTo(other.magnitude());
    }
    @Override
    public long compareTo(ScalarAtom other) {
        if(this.isNotReal() || other.isNotReal()) throw new IllegalStateException("Error: Complex numbers cannot be compared");
        if(other instanceof LargeInteger || other instanceof LargeFraction){
            ScalarAtom r = this.subtract(other).sign();
            if(r.equalsTo(LargeInteger.negativeOne)) return -1;
            else if(r.equalsTo(LargeInteger.one)) return 1;
            return 0;
        }else if(other instanceof LargePowerMonomial){
            LargeFraction a1 = (LargeFraction) this.floor().divide(LargeInteger.one);
            LargeFraction b1 = (LargeFraction) this.ceil().divide(LargeInteger.one);
            LargeFraction a2 = (LargeFraction) other.floor().divide(LargeInteger.one);
            LargeFraction b2 = (LargeFraction) other.ceil().divide(LargeInteger.one);
            if(this.equalsTo(other)) return 0;
            while(true){
                if (b1.lesserThanOrEqualTo(a2)) return -1;
                else if (b2.lesserThanOrEqualTo(a1)) return 1;
                LargeFraction mediant = new LargeFraction((LargeInteger) a1.numerator.add(b1.numerator), (LargeInteger) a1.denominator.add(b1.denominator));
                if (mediant.lessThan(this)) a1 = mediant;
                else b1 = mediant;
                LargeFraction mediant2 = new LargeFraction((LargeInteger) a2.numerator.add(b2.numerator), (LargeInteger) a2.denominator.add(b2.denominator));
                if (mediant2.lessThan(other)) a2 = mediant2;
                else b2 = mediant2;
            }
        }else if(other instanceof RadicalTerm){
            LargeFraction a1 = (LargeFraction) this.floor().divide(LargeInteger.one);
            LargeFraction b1 = (LargeFraction) this.ceil().divide(LargeInteger.one);
            LargeFraction a2 = (LargeFraction) other.floor().divide(LargeInteger.one);
            LargeFraction b2 = (LargeFraction) other.ceil().divide(LargeInteger.one);
            if(this.equalsTo(other)) return 0;
            while(true){
                if(a2.greaterThan(b1)) return -1;
                if (a1.greaterThan(b2)) return 1;
                LargeFraction mediant = new LargeFraction((LargeInteger) a1.numerator.add(b1.numerator), (LargeInteger) a1.denominator.add(b1.denominator));
                if (mediant.lessThan(this)) a1 = mediant;
                else b1 = mediant;
                LargeFraction mediant2 = new LargeFraction((LargeInteger) a2.numerator.add(b2.numerator), (LargeInteger) a2.denominator.add(b2.denominator));
                if (mediant2.lessThan(other)) a2 = mediant2;
                else b2 = mediant2;
            }
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public boolean greaterThan(ScalarAtom other) {return this.compareTo(other) > 0;}
    @Override
    public boolean lessThan(ScalarAtom other) {return this.compareTo(other) < 0;}
    @Override
    public boolean greaterThanOrEqualTo(ScalarAtom other) {return this.compareTo(other) >= 0;}
    @Override
    public boolean lesserThanOrEqualTo(ScalarAtom other){return this.compareTo(other) <= 0;}
    @Override
    public RadicalTerm negate(){return new RadicalTerm(rationalPart.negate(), coefficient.negate(), exponent);}
    @Override
    public boolean isRational() {return coefficient.equalsTo(LargeInteger.zero);}
    @Override
    public ScalarAtom add(ScalarAtom other){
        if(other instanceof LargeInteger || other instanceof LargeFraction)
            return new RadicalTerm((LargeFraction) this.rationalPart.add(other), this.coefficient, exponent);
        else if(other instanceof LargePowerMonomial){
            LargeList<RadicalTerm> rt = new LargeList<>();
            rt.add(this);
            rt.add(new RadicalTerm(LargeFraction.zero, LargeFraction.one, (LargePowerMonomial) other));
            return new RadicalTermSum(rt, LargeFraction.zero);
        }else if(other instanceof RadicalTerm){
            LargeList<RadicalTerm> rt = new LargeList<>();
            rt.add(this);
            rt.add((RadicalTerm) other);
            return new RadicalTermSum(rt, LargeFraction.zero);
        }else if(other instanceof RadicalTermSum R){
            LargeList<RadicalTerm> rt = new LargeList<>();
            rt.add(this);
            for(LargePowerMonomial L: R.sum)
                rt.add(new RadicalTerm(LargeFraction.zero, R.sum.get(L), L));
            return new RadicalTermSum(rt, R.constant);
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public ScalarAtom multiply(ScalarAtom f) {
        if(f instanceof LargeFraction || f instanceof LargeInteger) return new RadicalTerm((LargeFraction) this.rationalPart.multiply(f), (LargeFraction) this.coefficient.multiply(f), this.exponent);
        else if(f instanceof LargePowerMonomial) {
            LargeList<RadicalTerm> rt = new LargeList<>();
            rt.add(new RadicalTerm(LargeFraction.zero, this.rationalPart, (LargePowerMonomial) f));
            rt.add(new RadicalTerm(LargeFraction.zero, this.coefficient, (LargePowerMonomial) exponent.multiply(f)));
            return new RadicalTermSum(rt, LargeFraction.zero);
        }else if(f instanceof RadicalTerm){
            LargeList<RadicalTerm> rt = new LargeList<>();
            LargeFraction cons = (LargeFraction) rationalPart.multiply(((RadicalTerm) f).rationalPart);
            LargePowerMonomial L = ((RadicalTerm) f).exponent.copy();
            rt.add(new RadicalTerm(LargeFraction.zero, (LargeFraction) this.rationalPart.multiply(((RadicalTerm) f).coefficient), L));
            L = exponent.copy();
            rt.add(new RadicalTerm(LargeFraction.zero, (LargeFraction) this.coefficient.multiply(((RadicalTerm) f).rationalPart), L));
            L = (LargePowerMonomial) exponent.multiply(((RadicalTerm) f).exponent);
            rt.add(new RadicalTerm(LargeFraction.zero, (LargeFraction) this.coefficient.multiply(((RadicalTerm) f).coefficient), L));
            return new RadicalTermSum(rt, cons);
        }else if(f instanceof RadicalTermSum)
            return f.multiply(this);
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public RadicalTermSum square(){
        LargeList<RadicalTerm> rt = new LargeList<>();
        rt.add(new RadicalTerm(LargeFraction.zero, (LargeFraction) coefficient.multiply(LargeInteger.two).multiply(rationalPart), exponent));
        rt.add(new RadicalTerm(LargeFraction.zero, coefficient.square(), exponent.square()));
        return new RadicalTermSum(rt, rationalPart.square());
    }
    @Override
    public RadicalTermSum cube() {
        LargeList<RadicalTerm> rt = new LargeList<>();
        rt.add(new RadicalTerm(LargeFraction.zero, (LargeFraction) coefficient.square().multiply(LargeInteger.three).multiply(rationalPart), exponent.square()));
        rt.add(new RadicalTerm(LargeFraction.zero, (LargeFraction) coefficient.multiply(rationalPart.square()), exponent));
        rt.add(new RadicalTerm(LargeFraction.zero, coefficient.cube(), exponent.cube()));
        return new RadicalTermSum(rt, rationalPart.cube());
    }
    @Override
    public ScalarAtom power(LargeFraction f){return new LargePowerNode(this, f, LargeFraction.one);}

    @Override
    public LargeDecimal approximateToDecimal(LargeInteger precision) {return null;}

    @Override
    public ScalarAtom reciprocal(){
        if(coefficient.equalsTo(LargeInteger.zero))
            return rationalPart.reciprocal();
        else if(rationalPart.equalsTo(LargeInteger.zero))
            return new RadicalTerm(LargeFraction.zero, coefficient.reciprocal(), exponent.reciprocal());
        else if(this.equalsTo(LargeInteger.zero)) throw new ArithmeticException("Error: Cannot divide by 0");
        else if(this.exponent.exponent.equalsTo(LargeFraction.half) || this.exponent.exponent2.equalsTo(LargeFraction.half))
            return this.conjugate().divide(this.multiply(this.conjugate()));
        // reciprocal of a + b*x, where x^n is rational
        LargeFraction a = rationalPart.copy();
        LargeFraction b = coefficient.copy();
        LargeInteger n = exponent.exponent.denominator.lcm(exponent.exponent2.denominator);
        LargePowerMonomial qAtom = exponent.power(new LargeFraction(n, LargeInteger.one));
        if (!qAtom.isRational())
            throw new ArithmeticException("Error: x^n is not rational");
        LargeFraction q = qAtom.base;
        if(qAtom.exponent2.equalsTo(LargeInteger.one)) q = q.negate();
        LargeList<LargeFraction> coeff = new LargeList<>();
        LargeFraction aPow = a.power(n.subtract(1));
        LargeFraction bPow = LargeFraction.one;
        for (LargeInteger k = LargeInteger.zero; k.lessThan(n); k = k.add(1)) {
            LargeFraction c = (LargeFraction) aPow.multiply(bPow);
            if (k.isOdd()) c = c.negate();
            coeff.add(c);
            aPow = (LargeFraction) aPow.divide(a);
            bPow = (LargeFraction) bPow.multiply(b);
        }

        LargeFraction denom = (LargeFraction) a.power(n).subtract(b.negate().power(n).multiply(q));

        LargeMap<LargePowerMonomial, LargeFraction> map = new LargeMap<>();
        for (LargeInteger i = LargeInteger.zero; i.lessThan(coeff.size()); i = i.add(1))
            map.put(exponent.power(new LargeFraction(i, LargeInteger.one)),(LargeFraction) coeff.get(i).divide(denom));
        return new RadicalTermSum(map, LargeFraction.zero);
    }
}
