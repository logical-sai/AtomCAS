package com.AtomCAS.utilities;
public class RadicalTerm extends ScalarAtom{
    public final LargeFraction rationalPart;
    public final LargeFraction coefficient;
    public final LargePowerMonomial exponent;
    public RadicalTerm(LargeFraction r, LargeFraction c, LargePowerMonomial E){
        if(c.equals(LargeFraction.ZERO) || E.equals(LargeFraction.ZERO)){
            rationalPart = r.clone();
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
        LargeFraction tempR = r.clone();
        LargeFraction tempC = c.clone();
        //make real radicand an integer
        LargePowerMonomial Enum = new LargePowerMonomial((LargeFraction) E.base.numerator.divide(LargeInteger.ONE), E.exponent, E.exponent2);
        LargePowerMonomial Edem = new LargePowerMonomial(new LargeFraction(E.base.denominator, LargeInteger.ONE), E.exponent);
        LargePowerMonomial Edem2 = new LargePowerMonomial(new LargeFraction(E.base.denominator, LargeInteger.ONE), (LargeFraction) LargeInteger.ONE.subtract(E.exponent.mod(LargeInteger.ONE)));
        Enum = (LargePowerMonomial) Enum.multiply(Edem2);
        Edem = (LargePowerMonomial) Edem.multiply(Edem2);
        tempC = (LargeFraction) tempC.divide(Edem.base);
        LargeMap<LargeInteger, LargeFraction> pr = Enum.factor();
        for(LargeInteger I: pr.keys()){
            LargeInteger expTemp = pr.get(I).intPart();
            LargeInteger basTemp = I.clone();
            while(expTemp.doesntEqual(LargeInteger.ZERO)){
                if(expTemp.isOdd()){
                    if(expTemp.lessThan(LargeInteger.ZERO))
                        tempC = (LargeFraction) tempC.divide(basTemp);
                    else tempC = (LargeFraction) tempC.multiply(basTemp);
                    if(expTemp.abs().equals(LargeInteger.ONE)) break;
                }
                basTemp = basTemp.square();
                expTemp = expTemp.shiftRight(1);
            }
            pr.put(I, pr.get(I).fracPart());
        }
        Enum = LargePowerMonomial.buildFromFactors(pr);
        if(Enum.equals(LargeInteger.ONE)){
            rationalPart = (LargeFraction) tempR.add(tempC);
            coefficient = LargeFraction.ZERO;
            exponent = LargePowerMonomial.ZERO;
            return;
        }
        rationalPart = tempR.clone();
        coefficient = tempC.clone();
        exponent = Enum.clone();
    }
    @Override
    public RadicalTerm clone(){return new RadicalTerm(rationalPart.clone(), coefficient.clone(), exponent.clone());}
    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        if(rationalPart.doesntEqual(LargeInteger.ZERO))
            str.append(rationalPart);
        if(coefficient.equals(LargeInteger.ZERO)){
            if(str.isEmpty()) str.append(rationalPart);
            return str.toString();
        }else if(coefficient.lessThan(LargeInteger.ZERO)){
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
    public boolean equals(Atom other) {
        if(!(other instanceof ScalarAtom)) return false;
        else if(other instanceof LargeInteger || other instanceof LargeFraction) return rationalPart.equals(other) && coefficient.equals(LargeFraction.ZERO);
        else if(other instanceof LargePowerMonomial){
            RadicalTerm newOther = new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, (LargePowerMonomial) other);
            return rationalPart.equals(newOther.rationalPart) && coefficient.equals(newOther.coefficient) && exponent.equals(newOther.exponent);
        }else if(other instanceof RadicalTerm) return rationalPart.equals(((RadicalTerm) other).rationalPart) && coefficient.equals(((RadicalTerm) other).coefficient) && exponent.equals(((RadicalTerm) other).exponent);
        throw new UnsupportedOperationException("Operation with these types together not supported yet");
    }
    @Override
    public LargeInteger floor() {
        if(isNotReal()) throw new IllegalStateException("Cannot floor or apply ceiling a non-real atom");
        if (coefficient.equals(LargeInteger.ZERO))
            return rationalPart.floor();
        LargePowerMonomial K = exponent.clone();
        LargeFraction lower = new LargeFraction(K.floor(), LargeInteger.ONE);
        LargeFraction upper = new LargeFraction(K.ceil(), LargeInteger.ONE);
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
        return floor().add(1);
    }
    public boolean isReal(){return exponent.isReal();}
    @Override
    public boolean isAlgebraic() {return true;}
    @Override
    public ScalarAtom realPart(){return sign().realPart().multiply(abs());}
    @Override
    public ScalarAtom imagPart(){return sign().imagPart().multiply(abs());}
    @Override
    public ScalarAtom abs(){
        if(isReal()){
            if(lessThan(LargeInteger.ZERO))
                return negate();
            return this;
        }
        if(rationalPart.equals(LargeInteger.ZERO))
            return new RadicalTerm(LargeFraction.ZERO, coefficient.abs(), exponent.abs());
        ScalarAtom normSquared = multiply(complexConjugate());
        if(normSquared.isRational())
            return new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, new LargePowerMonomial(((RadicalTermSum)normSquared).constant, LargeFraction.HALF));
        return new LargePowerNode(normSquared, LargeFraction.HALF, LargeFraction.ONE);
    }
    public RadicalTerm complexConjugate(){return new RadicalTerm(rationalPart, coefficient, exponent.complexConjugate());}
    @Override
    public int compareMagnitudes(ScalarAtom other){
        if(other instanceof LargeInteger || other instanceof LargeFraction){
            if(coefficient.equals(LargeInteger.ZERO)) return rationalPart.compareMagnitudes(other);
            LargeFraction f = (LargeFraction) other.subtract(rationalPart).divide(coefficient);
            f = f.power(exponent.exponent.denominator.lcm(exponent.exponent2.denominator));
            LargeFraction o = exponent.power(new LargeFraction(exponent.exponent.denominator.lcm(exponent.exponent2.denominator), LargeInteger.ONE)).base;
            return o.compareMagnitudes(f);
        }
        return abs().compareTo(other.abs());
    }
    @Override
    public int compareTo(ScalarAtom other) {
        if(isNotReal() || other.isNotReal()) throw new IllegalStateException("Complex numbers cannot be compared");
        return switch (other) {
            case LargeInteger _, LargeFraction _ ->{
                if(coefficient.equals(LargeInteger.ZERO))
                    yield rationalPart.compareTo(other);
                LargeFraction f = (LargeFraction) other.subtract(rationalPart).divide(coefficient);
                int cmp = exponent.compareTo(f);
                if (coefficient.lessThan(LargeInteger.ZERO))
                    cmp = -cmp;
                yield cmp;
            }case LargePowerMonomial _, RadicalTerm _ -> {
                LargeFraction a1 = (LargeFraction) floor().divide(LargeInteger.ONE);
                LargeFraction b1 = (LargeFraction) ceil().divide(LargeInteger.ONE);
                LargeFraction a2 = (LargeFraction) other.floor().divide(LargeInteger.ONE);
                LargeFraction b2 = (LargeFraction) other.ceil().divide(LargeInteger.ONE);
                if (equals(other)) yield 0;
                while (true) {
                    if (b1.lesserThanOrEqualTo(a2)) yield -1;
                    else if (b2.lesserThanOrEqualTo(a1)) yield 1;
                    LargeFraction mediant = new LargeFraction((LargeInteger) a1.numerator.add(b1.numerator), (LargeInteger) a1.denominator.add(b1.denominator));
                    if (mediant.lessThan(this)) a1 = mediant;
                    else b1 = mediant;
                    LargeFraction mediant2 = new LargeFraction((LargeInteger) a2.numerator.add(b2.numerator), (LargeInteger) a2.denominator.add(b2.denominator));
                    if (mediant2.lessThan(other)) a2 = mediant2;
                    else b2 = mediant2;
                }
            }
            default -> -other.compareTo(this);
        };
    }
    @Override
    public RadicalTerm negate(){return new RadicalTerm(rationalPart.negate(), coefficient.negate(), exponent);}
    @Override
    public boolean isRational() {return coefficient.equals(LargeInteger.ZERO);}
    @Override
    public ScalarAtom add(ScalarAtom other){
        if(other instanceof LargeInteger || other instanceof LargeFraction)
            return new RadicalTerm((LargeFraction) rationalPart.add(other), coefficient, exponent);
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
        throw new UnsupportedOperationException("Operation with these types together not supported yet");
    }
    @Override
    public ScalarAtom multiply(ScalarAtom f) {
        if(f instanceof LargeFraction || f instanceof LargeInteger) return new RadicalTerm((LargeFraction) rationalPart.multiply(f), (LargeFraction) coefficient.multiply(f), exponent);
        else if(f instanceof LargePowerMonomial) {
            LargeList<RadicalTerm> rt = new LargeList<>();
            rt.add(new RadicalTerm(LargeFraction.ZERO, rationalPart, (LargePowerMonomial) f));
            rt.add(new RadicalTerm(LargeFraction.ZERO, coefficient, (LargePowerMonomial) exponent.multiply(f)));
            return new RadicalTermSum(rt, LargeFraction.ZERO);
        }else if(f instanceof RadicalTerm){
            LargeList<RadicalTerm> rt = new LargeList<>();
            LargeFraction cons = (LargeFraction) rationalPart.multiply(((RadicalTerm) f).rationalPart);
            LargePowerMonomial L = ((RadicalTerm) f).exponent.clone();
            rt.add(new RadicalTerm(LargeFraction.ZERO, (LargeFraction) rationalPart.multiply(((RadicalTerm) f).coefficient), L));
            L = exponent.clone();
            rt.add(new RadicalTerm(LargeFraction.ZERO, (LargeFraction) coefficient.multiply(((RadicalTerm) f).rationalPart), L));
            L = (LargePowerMonomial) exponent.multiply(((RadicalTerm) f).exponent);
            rt.add(new RadicalTerm(LargeFraction.ZERO, (LargeFraction) coefficient.multiply(((RadicalTerm) f).coefficient), L));
            return new RadicalTermSum(rt, cons);
        }
        return f.multiply(this);
    }
    @Override
    public LargePowerNode power(LargeFraction f){return new LargePowerNode(this, f, LargeFraction.ONE);}
    @Override
    public LargeDecimal approximateToDecimal(LargeInteger precision) {return null;}
    @Override
    public ScalarAtom reciprocal(){
        if(coefficient.equals(LargeInteger.ZERO))
            return rationalPart.reciprocal();
        else if(rationalPart.equals(LargeInteger.ZERO))
            return new RadicalTerm(LargeFraction.ZERO, coefficient.reciprocal(), exponent.reciprocal());
        else if(equals(LargeInteger.ZERO)) throw new ArithmeticException("Cannot divide by 0");
        // reciprocal of a + b*x, where x^n is rational
        LargeFraction a = rationalPart.clone();
        LargeFraction b = coefficient.clone();
        LargeInteger n = exponent.exponent.denominator.lcm(exponent.exponent2.denominator);
        LargePowerMonomial qAtom = exponent.power(n);
        if (!qAtom.isRational())
            throw new ArithmeticException("x^n is not rational");
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
            map.add(new RadicalTerm(LargeFraction.ZERO,(LargeFraction) coefficient.get(i).divide(denominator),
                    exponent.power(i)));
        return new RadicalTermSum(map, LargeFraction.ZERO);
    }
}
