package com.AtomCAS.utilities;
public class LargePowerMonomial extends ScalarAtom{
    public final LargeFraction base;
    public final LargeFraction exponent;
    public final LargeFraction exponent2;
    public static final LargePowerMonomial ZERO = new LargePowerMonomial(LargeFraction.ZERO, LargeFraction.ONE);
    public static final LargePowerMonomial ONE = new LargePowerMonomial(LargeFraction.ONE, LargeFraction.ONE);
    public static final LargePowerMonomial I = new LargePowerMonomial(LargeFraction.NEGATIVE_ONE, LargeFraction.HALF);
    public LargePowerMonomial(LargeFraction num, LargeFraction dem){
        if(num.doesntEqual(LargeInteger.ZERO) && dem.equals(LargeInteger.ZERO)){
            base = LargeFraction.ONE;
            exponent = LargeFraction.ONE;
            exponent2 = LargeFraction.ZERO;
            return;
        }else if(num.equals(LargeFraction.ZERO) || num.equals(LargeFraction.ONE)){
            if(num.equals(LargeFraction.ZERO) && dem.lessThan(LargeFraction.ZERO)) throw new ArithmeticException("Cannot divide by 0");
            else if(num.equals(LargeFraction.ZERO) && dem.equals(LargeFraction.ZERO)) throw new ArithmeticException("Indeterminate form: 0^0");
            base = new LargeFraction(num.numerator, num.denominator);
            exponent = LargeFraction.ONE;
            exponent2 = LargeFraction.ZERO;
            return;
        }else if(num.abs().equals(LargeInteger.ONE)){
            if(num.equals(LargeInteger.ONE)){
                exponent2 = LargeFraction.ZERO;
                exponent = LargeFraction.ONE;
                base = LargeFraction.ONE;
                return;
            }
            if(LargeInteger.isOneArray(dem.denominator.limbs)){
                if(dem.numerator.isOdd()){
                    base = LargeFraction.ONE;
                    exponent2 = LargeFraction.ONE;
                    exponent = LargeFraction.ONE;
                    return;
                }
                base = LargeFraction.ONE;
                exponent2 = LargeFraction.ZERO;
                exponent = LargeFraction.ONE;
                return;
            }
            exponent2 = (LargeFraction) dem.clone().mod(LargeInteger.TWO);
            exponent = LargeFraction.ONE;
            base = LargeFraction.ONE;
            return;
        }else if(dem.abs().equals(LargeInteger.ONE)){
            if(dem.lessThan(LargeInteger.ZERO)) base = num.reciprocal().abs();
            else base = num.abs();
            if(num.lessThan(LargeInteger.ZERO))
                exponent2 = LargeFraction.ONE;
            else exponent2 = LargeFraction.ZERO;
            exponent = LargeFraction.ONE;
            return;
        }
        LargeMap<LargeInteger, LargeFraction> pr = num.factor();
        for(LargeInteger I: pr.keys())
            pr.put(I, (LargeFraction) pr.get(I).multiply(dem));
        if(pr.containsKey(LargeInteger.NEGATIVE_ONE)){
            exponent2 = (LargeFraction) pr.get(LargeInteger.NEGATIVE_ONE).mod(LargeInteger.TWO);
            pr.remove(LargeInteger.NEGATIVE_ONE);
            if (pr.size().equals(LargeInteger.ZERO)){
                base = LargeFraction.ONE;
                exponent = LargeFraction.ONE;
                return;
            }
        }else exponent2 = LargeFraction.ZERO;
        LargeInteger lc = pr.get(pr.keyAtIndex(LargeInteger.ZERO)).denominator;
        for(LargeFraction I: pr.values()) lc = lc.lcm(I.denominator);
        if(lc.equals(LargeInteger.ONE)){
            base = LargeFraction.buildFromFactors(pr);
            exponent = LargeFraction.ONE;
            return;
        }else if(pr.size().equals(LargeInteger.ONE)){
            if(pr.get(pr.keyAtIndex(LargeInteger.ZERO)).lessThan(LargeInteger.ZERO)){
                base = pr.keyAtIndex(LargeInteger.ZERO).reciprocal();
                exponent = pr.get(pr.keyAtIndex(LargeInteger.ZERO)).negate();
            }else{
                base = new LargeFraction(pr.keyAtIndex(LargeInteger.ZERO), LargeInteger.ONE);
                exponent = pr.get(pr.keyAtIndex(LargeInteger.ZERO));
            }
            return;
        }
        LargeFraction G = pr.get(pr.keyAtIndex(LargeInteger.ZERO));
        for(LargeFraction I: pr.values()) G = G.gcd(I);
        LargeFraction B = LargeFraction.ONE;
        for(LargeInteger I: pr.keys()){
            LargeInteger expTemp = ((LargeFraction)pr.get(I).divide(G)).numerator;
            LargeInteger basTemp = I.clone();
            while(expTemp.doesntEqual(LargeInteger.ZERO)){
                if(expTemp.isOdd()){
                    if(expTemp.lessThan(LargeInteger.ZERO))
                        B = (LargeFraction) B.divide(basTemp);
                    else B = (LargeFraction) B.multiply(basTemp);
                    if(expTemp.abs().equals(LargeInteger.ONE)) break;
                }
                basTemp = basTemp.square();
                expTemp = expTemp.longDivision(LargeInteger.TWO);
            }
        }
        base = B.clone();
        exponent = G.clone();
    }
    public LargePowerMonomial(LargeFraction base, LargeFraction exponent, LargeFraction exponent2){
        LargePowerMonomial obj = new LargePowerMonomial(base, exponent);
        this.base = obj.base;
        this.exponent = obj.exponent;
        this.exponent2 = (LargeFraction) exponent2.add(obj.exponent2).mod(LargeInteger.TWO);
    }
    @Override
    public LargePowerMonomial clone() {return new LargePowerMonomial(base, exponent, exponent2);}
    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        if(exponent2.doesntEqual(LargeInteger.ZERO)){
            if(exponent2.doesntEqual(LargeInteger.ONE))
                str.append("(-1)^(").append(exponent2).append(")");
            else
                str.append("-");
        }
        if(base.equals(LargeInteger.ONE)){
            if(str.isEmpty() || str.toString().equals("-")) str.append("1");
            return str.toString();
        }
        if(!str.toString().equals("-") && !str.isEmpty()) str.append("*");
        if(base.denominator.doesntEqual(LargeInteger.ONE) && exponent.doesntEqual(LargeInteger.ONE))
            str.append("(").append(base).append(")");
        else
            str.append(base.abs());
        if(exponent.doesntEqual(LargeFraction.ONE))
            str.append("^(").append(exponent).append(")");
        return str.toString();
    }
    @Override
    public String toLatex() {
        StringBuilder str = new StringBuilder();
        if (exponent2.doesntEqual(LargeInteger.ZERO)) {
            if (exponent2.equals(LargeInteger.ONE))
                str.append("-");
            else
                str.append("(-1)^{").append(exponent2.toLatex()).append("}");
        }
        if (base.equals(LargeInteger.ONE)) {
            if (str.isEmpty() || str.toString().equals("-")) str.append("1");
            return str.toString();
        }
        if (!str.isEmpty() && !str.toString().equals("-"))
            str.append("\\cdot ");
        String baseLatex = base.toLatex();
        if (base.denominator.doesntEqual(LargeInteger.ONE) || exponent.doesntEqual(LargeFraction.ONE))
            str.append("\\left(").append(baseLatex).append("\\right)");
        else
            str.append(baseLatex);
        if (exponent.doesntEqual(LargeFraction.ONE))
            str.append("^{").append(exponent.toLatex()).append("}");
        return str.toString();
    }
    @Override
    public boolean equals(Atom other) {
        if(!(other instanceof ScalarAtom)) return false;
        switch (other) {
            case LargeInteger largeInteger -> {
                boolean B = this.base.equals(largeInteger.abs()) && this.exponent.equals(LargeFraction.ONE);
                if (largeInteger.lessThan(LargeInteger.ZERO))
                    return B && this.exponent2.equals(LargeFraction.ONE);
                else if (largeInteger.greaterThan(LargeInteger.ZERO))
                    return B && this.exponent2.equals(LargeFraction.ZERO);
                return B;
            }
            case LargeFraction largeFraction -> {
                boolean B = this.base.equals(largeFraction.abs()) && this.exponent.equals(LargeFraction.ONE);
                if (largeFraction.lessThan(LargeInteger.ZERO))
                    return B && this.exponent2.equals(LargeFraction.ONE);
                else if (largeFraction.greaterThan(LargeInteger.ZERO))
                    return B && this.exponent2.equals(LargeFraction.ZERO);
                return B;
            }
            case LargePowerMonomial largePowerMonomial -> {return largePowerMonomial.base.equals(this.base) && this.exponent.equals(largePowerMonomial.exponent) && this.exponent2.equals(largePowerMonomial.exponent2);}
            case RadicalTerm radicalTerm -> {return new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, this).equals(radicalTerm);}
            default -> throw new UnsupportedOperationException("Operation with these types together not supported yet");
        }
    }
    @Override
    public LargePowerMonomial abs(){return new LargePowerMonomial(base, exponent);}
    @Override
    public int compareMagnitudes(ScalarAtom other){
        if(other instanceof LargeInteger LI){
            LargeInteger pow = exponent.denominator.lcm(exponent2.denominator);
            LargeFraction T = this.power(new LargeFraction(pow, LargeInteger.ONE)).base;
            LargeFraction O = LI.power(pow);
            return T.compareMagnitudes(O);
        }else if(other instanceof LargeFraction F){
            LargeInteger pow = exponent.denominator.lcm(exponent2.denominator);
            LargeFraction T = this.power(new LargeFraction(pow, LargeInteger.ONE)).base;
            LargeFraction O = F.power(pow);
            return T.compareMagnitudes(O);
        }else if(other instanceof LargePowerMonomial LPM){
            LargeInteger pow = exponent.denominator.lcm(exponent2.denominator, LPM.exponent.denominator, LPM.exponent2.denominator);
            LargeFraction T = this.power(new LargeFraction(pow, LargeInteger.ONE)).base;
            LargeFraction O = LPM.power(new LargeFraction(pow, LargeInteger.ONE)).base;
            return T.compareMagnitudes(O);
        }else if(other instanceof RadicalTerm R){
            if(R.coefficient.equals(LargeFraction.ZERO)) return this.compareMagnitudes(R.rationalPart);
            else if(R.rationalPart.equals(LargeFraction.ZERO)){
                LargeInteger pow = exponent.denominator.lcm(exponent2.denominator, R.exponent.exponent.denominator, R.exponent.exponent2.denominator);
                LargeFraction T = this.power(new LargeFraction(pow, LargeInteger.ONE)).base;
                LargeFraction O = R.exponent.power(new LargeFraction(pow, LargeInteger.ONE)).base;
                O = (LargeFraction) O.multiply(R.coefficient.power(pow));
                return T.compareMagnitudes(O);
            }
            LargeFraction F1 = new LargeFraction(this.floor(), LargeInteger.ONE);
            LargeFraction C1 = new LargeFraction(this.ceil(), LargeInteger.ONE);
            LargeFraction F2 = new LargeFraction(R.floor(), LargeInteger.ONE);
            LargeFraction C2 = new LargeFraction(R.ceil(), LargeInteger.ONE);
            if(F1.greaterThan(C2)) return 1;
            else if(F2.greaterThan(C1)) return -1;

        }
        return this.abs().compareTo(other.abs());
    }
    @Override
    public int compareTo(ScalarAtom other) {
        if(this.isNotReal() || other.isNotReal()) throw new IllegalStateException("Complex numbers cannot be compared");
        return switch (other) {
            case LargeFraction largeFraction -> compareTo(new LargePowerMonomial(largeFraction, LargeFraction.ONE));
            case LargeInteger largeInteger -> {
                LargePowerMonomial copy = new LargePowerMonomial(base, exponent);
                LargePowerMonomial copy2 = new LargePowerMonomial(new LargeFraction(largeInteger, LargeInteger.ONE), LargeFraction.ONE);
                LargeFraction pow = copy.exponent.denominator.power(LargeInteger.ONE);
                copy = copy.power(pow);
                copy2 = copy2.power(pow);
                yield copy.base.compareTo(copy2.base);
            }case LargePowerMonomial largePowerMonomial -> {
                LargePowerMonomial copy = new LargePowerMonomial(this.base, this.exponent);
                LargePowerMonomial copy2 = new LargePowerMonomial(largePowerMonomial.base, largePowerMonomial.exponent);
                LargeInteger lc = this.exponent.denominator.lcm(largePowerMonomial.exponent.denominator);
                copy = copy.power((LargeFraction) lc.divide(LargeInteger.ONE));
                copy2 = copy2.power((LargeFraction) lc.divide(LargeInteger.ONE));
                int C = new LargePowerMonomial(copy.base, copy.exponent).base.compareMagnitudes(new LargePowerMonomial(copy2.base, copy2.exponent).base);
                if (C == 1) {
                    if (this.sign().equals(LargeInteger.ONE)) yield 1;
                    yield -1;
                } else if (C == -1) {
                    if (other.sign().equals(LargeInteger.ONE)) yield -1;
                    yield 1;
                }
                yield C;
            }case RadicalTerm radicalTerm -> {
                //I present to you...ERROR BOUNDS
                if (radicalTerm.coefficient.equals(LargeInteger.ZERO)) yield this.compareTo(radicalTerm.rationalPart);
                LargeFraction l1 = new LargeFraction(this.floor(), LargeInteger.ONE);
                LargeFraction u1 = new LargeFraction(this.ceil(), LargeInteger.ONE);
                LargeFraction l2 = new LargeFraction(other.floor(), LargeInteger.ONE);
                LargeFraction u2 = new LargeFraction(other.ceil(), LargeInteger.ONE);
                while (l1.lessThan(u2) && l2.lessThan(u1)) {
                    LargeFraction mediant = new LargeFraction((LargeInteger) l1.numerator.add(u1.numerator), (LargeInteger) l1.denominator.add(u1.denominator));
                    if (mediant.lessThan(this))
                        l1 = mediant.clone();
                    else u1 = mediant.clone();
                    LargeFraction mediant2 = new LargeFraction((LargeInteger) l2.numerator.add(u2.numerator), (LargeInteger) l2.denominator.add(u2.denominator));
                    if (mediant2.lessThan(other))
                        l2 = mediant2.clone();
                    else u2 = mediant2.clone();
                }
                yield l1.compareTo(u2);
            }
            default -> throw new UnsupportedOperationException("Operation with these types together not supported yet");
        };
    }
    @Override
    public LargePowerMonomial negate() {return new LargePowerMonomial(this.base, this.exponent, (LargeFraction) this.exponent2.add(LargeFraction.ONE));}
    @Override
    public boolean isReal() {return exponent2.equals(LargeInteger.ONE) || exponent2.equals(LargeInteger.ZERO);}
    @Override
    public boolean isRational(){return exponent2.denominator.equals(LargeFraction.ONE) && exponent.equals(LargeFraction.ONE);}
    public boolean isNotInteger() {return exponent.doesntEqual(LargeInteger.ONE) || base.denominator.doesntEqual(LargeInteger.ONE) || this.isNotReal();}
    @Override
    public boolean isAlgebraic() {return true;}
    public LargePowerMonomial complexConjugate(){return new LargePowerMonomial(base, exponent, exponent2.negate());}
    @Override
    public ScalarAtom realPart(){
        RadicalTermSum RT = (RadicalTermSum) new LargePowerMonomial(LargeFraction.NEGATIVE_ONE, exponent2).add(new LargePowerMonomial(LargeFraction.NEGATIVE_ONE, exponent2.negate())).divide(LargeInteger.TWO);
        return RT.multiply(new LargePowerMonomial(base, exponent));
    }
    @Override
    public ScalarAtom imagPart(){return this.subtract(this.realPart()).divide(I);}
    @Override
    public LargeInteger floor(){
        if(this.isNotReal()) throw new IllegalStateException("Cannot floor a non-real atom");
        else if(this.isRational())
            return base.floor();
        boolean negativeResult = exponent2.equals(LargeInteger.ONE);
        LargeFraction begun = base.numerator.abs().power(exponent.numerator);
        LargeFraction helper = base.denominator.abs().power(exponent.numerator);
        LargeInteger d = exponent.denominator;
        LargeInteger low;
        LargeInteger high;
        if(LargeInteger.ONE.power(d).multiply(helper).lesserThanOrEqualTo(begun)){
            low = LargeInteger.ONE;
            high = LargeInteger.TWO;
            while(high.power(d).multiply(helper).lesserThanOrEqualTo(begun)){
                low = high;
                high = (LargeInteger) high.multiply(LargeInteger.TWO);
            }
            while(high.subtract(low).greaterThan(LargeInteger.ONE)){
                LargeInteger mid = ((LargeInteger) low.add(high)).longDivision(LargeInteger.TWO);
                if(mid.power(d).multiply(helper).lesserThanOrEqualTo(begun))
                    low = mid;
                else
                    high = mid;
            }
        }
        else low = LargeInteger.ZERO;
        if(!negativeResult) return low;
        return low.add(1).negate();
    }
    @Override
    public LargeInteger ceil(){
        if(this.isRational()) return base.ceil();
        return this.floor().add(1);
    }
    @Override
    public ScalarAtom add(ScalarAtom other) {
        if(other instanceof LargeInteger || other instanceof LargeFraction)
            return new RadicalTerm((LargeFraction) other.divide(LargeInteger.ONE), LargeFraction.ONE, this);
        else if(other instanceof LargePowerMonomial){
            LargeList<RadicalTerm> rt = new LargeList<>();
            rt.add(new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, this));
            rt.add(new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, (LargePowerMonomial) other));
            return new RadicalTermSum(rt, LargeFraction.ZERO);
        }else if(other instanceof RadicalTerm){
            LargeList<RadicalTerm> rt = new LargeList<>();
            rt.add(new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, this));
            rt.add((RadicalTerm) other);
            return new RadicalTermSum(rt, LargeFraction.ZERO);
        }else if(other instanceof RadicalTermSum R){
            LargeList<RadicalTerm> rt = new LargeList<>();
            rt.add(new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, this));
            for(LargePowerMonomial L: R.sum.keys())
                rt.add(new RadicalTerm(LargeFraction.ZERO, R.sum.get(L), L));
            return new RadicalTermSum(rt, R.constant);
        }
        throw new UnsupportedOperationException("Operation with these types together not supported yet");
    }
    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        if(other.equals(LargeInteger.ONE)) return this;
        switch (other) {
            case LargeInteger largeInteger -> {
                LargeMap<LargeInteger, LargeFraction> pr = this.factor();
                LargeMap<LargeInteger, LargeFraction> pr2 = largeInteger.factor();
                for (LargeInteger I : pr2.keys()) {
                    if (pr.containsKey(I))
                        pr.put(I, (LargeFraction) pr.get(I).add(pr2.get(I)));
                    else pr.put(I, pr2.get(I));
                }
                return buildFromFactors(pr);
            }
            case LargeFraction largeFraction -> {
                LargeMap<LargeInteger, LargeFraction> pr = this.factor();
                LargeMap<LargeInteger, LargeFraction> pr2 = largeFraction.factor();
                for (LargeInteger I : pr2.keys()) {
                    if (pr.containsKey(I))
                        pr.put(I, (LargeFraction) pr.get(I).add(pr2.get(I)));
                    else pr.put(I, pr2.get(I));
                }
                return buildFromFactors(pr);
            }
            case LargePowerMonomial L -> {
                LargeMap<LargeInteger, LargeFraction> pr = this.factor();
                LargeMap<LargeInteger, LargeFraction> pr2 = L.factor();
                for (LargeInteger I : pr2.keys()) {
                    if (pr.containsKey(I))
                        pr.put(I, (LargeFraction) pr.get(I).add(pr2.get(I)));
                    else pr.put(I, pr2.get(I));
                }
                return buildFromFactors(pr);
            }
            case RadicalTerm radicalTerm -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                rt.add(new RadicalTerm(LargeFraction.ZERO, radicalTerm.rationalPart, this));
                rt.add(new RadicalTerm(LargeFraction.ZERO, radicalTerm.coefficient, (LargePowerMonomial) radicalTerm.exponent.multiply(this)));
                return new RadicalTermSum(rt, LargeFraction.ZERO);
            }
            case RadicalTermSum R -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                for (LargePowerMonomial L : R.sum.keys())
                    rt.add(new RadicalTerm(LargeFraction.ZERO, R.sum.get(L), (LargePowerMonomial) L.multiply(this)));
                rt.add(new RadicalTerm(LargeFraction.ZERO, R.constant, this));
                return new RadicalTermSum(rt, LargeFraction.ZERO);
            }
            default -> {
            }
        }
        throw new UnsupportedOperationException("Operation with these types together not supported yet");
    }
    @Override
    public LargePowerMonomial square(){return new LargePowerMonomial(this.base, (LargeFraction) this.exponent.multiply(LargeInteger.TWO), (LargeFraction) this.exponent2.multiply(LargeInteger.TWO));}
    @Override
    public LargePowerMonomial cube(){return new LargePowerMonomial(this.base, (LargeFraction) this.exponent.multiply(LargeInteger.THREE), (LargeFraction) this.exponent2.multiply(LargeInteger.THREE));}
    public LargePowerMonomial power(LargeInteger I){return new LargePowerMonomial(base, (LargeFraction) exponent.multiply(I), (LargeFraction) exponent2.multiply(I));}
    public LargePowerMonomial power(LargeFraction f){return new LargePowerMonomial(base, (LargeFraction) exponent.multiply(f), (LargeFraction) exponent2.multiply(f));}
    public LargeDecimal approximateToDecimal(LargeInteger precision){return null;}
    public LargeMap<LargeInteger, LargeFraction> factor(){
        LargeMap<LargeInteger, LargeFraction> pr = new LargeMap<>();
        if(exponent2.doesntEqual(LargeInteger.ZERO)) pr.put(LargeInteger.NEGATIVE_ONE, exponent2);
        if(base.equals(LargeFraction.ONE)){
            if(pr.isEmpty())
                pr.put(LargeInteger.ONE, LargeFraction.ONE);
            return pr;
        }
        LargeMap<LargeInteger, LargeFraction> pt = base.factor();
        for(LargeInteger I: pt.keys())
            pr.put(I, (LargeFraction) pt.get(I).multiply(exponent));
        return pr;
    }
    public static LargePowerMonomial buildFromFactors(LargeMap<LargeInteger, LargeFraction> pr){
        LargeFraction base = LargeFraction.ONE;
        LargeFraction exponent2 = LargeFraction.ZERO;
        if(pr.size().equals(LargeInteger.ZERO)) return ONE;
        else if(pr.size().equals(LargeInteger.ONE))
            return new LargePowerMonomial(new LargeFraction(pr.keyAtIndex(LargeInteger.ZERO), LargeInteger.ONE), pr.get(pr.keyAtIndex(LargeInteger.ZERO)));
        LargeFraction G = LargeFraction.ONE;
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(pr.size()); i = i.add(1)){
            LargeInteger I = pr.keyAtIndex(i);
            if(I.equals(LargeInteger.NEGATIVE_ONE)){
                exponent2 = (LargeFraction) exponent2.add(pr.get(I));
                pr.remove(I);
                i = i.subtract(1);
                continue;
            }
            G = G.gcd(pr.get(I));
        }
        for(LargeInteger I: pr.keys()){
            LargeInteger expTemp = ((LargeFraction) pr.get(I).divide(G)).numerator;
            LargeInteger basTemp = I.clone();
            while(expTemp.doesntEqual(LargeInteger.ZERO)){
                if(expTemp.isOdd()){
                    if(expTemp.lessThan(LargeInteger.ZERO))
                        base = (LargeFraction) base.divide(basTemp);
                    else base = (LargeFraction) base.multiply(basTemp);
                    if(expTemp.abs().equals(LargeInteger.ONE)) break;
                }
                basTemp = basTemp.square();
                expTemp = expTemp.longDivision(LargeInteger.TWO);
            }
        }
        return new LargePowerMonomial(base, G, exponent2);
    }
    @Override
    public LargePowerMonomial reciprocal(){return new LargePowerMonomial(base, exponent.negate(), exponent2.negate());}
}
