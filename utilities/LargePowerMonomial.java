package com.AtomCAS.utilities;
public class LargePowerMonomial extends ScalarAtom{
    private final LargeMap<LargeInteger, LargeFraction> factors;
    final LargeFraction base;
    final LargeFraction exponent;
    final LargeFraction exponent2;

    public static final LargePowerMonomial ZERO = new LargePowerMonomial(LargeFraction.ZERO, LargeFraction.ONE);
    public static final LargePowerMonomial ONE = new LargePowerMonomial(LargeFraction.ONE, LargeFraction.ONE);
    public static final LargePowerMonomial I = new LargePowerMonomial(LargeFraction.NEGATIVE_ONE, LargeFraction.HALF);
    private LargePowerMonomial(LargeFraction base, LargeFraction exponent, LargeFraction exponent2,
            LargeMap<LargeInteger, LargeFraction> factors) {
        this.base = base;
        this.exponent = exponent;
        this.exponent2 = (LargeFraction) exponent2.mod(LargeInteger.TWO);
        this.factors = factors.copy();
    }
    public LargePowerMonomial(LargeFraction num, LargeFraction dem){this(num, dem, LargeFraction.ZERO);}
    public LargePowerMonomial(LargeFraction base, LargeFraction exponent, LargeFraction exponent2){
        exponent2 = (LargeFraction) exponent2.mod(LargeInteger.TWO);

        // 0^e
        if (base.equals(LargeInteger.ZERO)) {
            if (exponent.equals(LargeInteger.ZERO))
                throw new IllegalArgumentException("Indeterminate form: 0^0");
            if (exponent.lessThan(LargeInteger.ZERO))
                throw new ArithmeticException("Cannot divide by 0");
            this.base = LargeFraction.ZERO;
            this.exponent = LargeFraction.ONE;
            this.exponent2 = LargeFraction.ZERO;
            this.factors = new LargeMap<>();
            factors.put(LargeInteger.ZERO, LargeFraction.ONE);
            return;
        }
        // (-1)^e2 * b^0
        if (exponent.equals(LargeInteger.ZERO)) {
            this.base = LargeFraction.ONE;
            this.exponent = LargeFraction.ONE;
            this.exponent2 = (LargeFraction) exponent2.mod(LargeInteger.TWO);
            this.factors = new LargeMap<>();
            if (exponent2.doesntEqual(LargeFraction.ZERO))
                this.factors.put(LargeInteger.NEGATIVE_ONE, this.exponent2);
            return;
        }
        LargeMap<LargeInteger, LargeFraction> pr = base.factor();
        if(pr.isEmpty()){
            this.base = LargeFraction.ONE;
            this.exponent = LargeFraction.ONE;
            this.exponent2 = (LargeFraction) exponent2.mod(LargeInteger.TWO);
            factors = new LargeMap<>();
            if (this.exponent2.doesntEqual(LargeFraction.ZERO))
                factors.put(LargeInteger.NEGATIVE_ONE, this.exponent2);
            return;
        }
        for(LargeInteger I: pr.keys())
            pr.put(I, (LargeFraction) pr.get(I).multiply(exponent));
        if(pr.containsKey(LargeInteger.NEGATIVE_ONE))
            this.exponent2 = (LargeFraction) pr.get(LargeInteger.NEGATIVE_ONE)
                    .add(exponent2).mod(LargeInteger.TWO);
        else this.exponent2 = (LargeFraction) exponent2.mod(LargeInteger.TWO);
        LargeInteger lc = pr.get(pr.keyAtIndex(LargeInteger.ZERO)).denominator;
        for(LargeFraction I: pr.values()) lc = lc.lcm(I.denominator);
        if(lc.equals(LargeInteger.ONE)){
            this.base = LargeFraction.buildFromFactors(pr);
            this.exponent = LargeFraction.ONE;
            factors = pr;
            return;
        }else if(pr.size().equals(LargeInteger.ONE)){
            if(pr.get(pr.keyAtIndex(LargeInteger.ZERO)).lessThan(LargeInteger.ZERO)){
                this.base = pr.keyAtIndex(LargeInteger.ZERO).reciprocal();
                this.exponent = pr.get(pr.keyAtIndex(LargeInteger.ZERO)).negate();
            }else{
                this.base = new LargeFraction(pr.keyAtIndex(LargeInteger.ZERO), LargeInteger.ONE);
                this.exponent = pr.get(pr.keyAtIndex(LargeInteger.ZERO));
            }
            factors = pr;
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
                expTemp = expTemp.shiftRight(1);
            }
        }
        this.base = B.clone();
        this.exponent = G.clone();
        factors = pr;
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
        return switch (other) {
            case LargeInteger largeInteger -> {
                boolean B = this.base.equals(largeInteger.abs()) && this.exponent.equals(LargeFraction.ONE);
                if (largeInteger.lessThan(LargeInteger.ZERO))
                    yield B && this.exponent2.equals(LargeFraction.ONE);
                else if (largeInteger.greaterThan(LargeInteger.ZERO))
                    yield B && this.exponent2.equals(LargeFraction.ZERO);
                yield B;
            }
            case LargeFraction largeFraction -> {
                boolean B = this.base.equals(largeFraction.abs()) && this.exponent.equals(LargeFraction.ONE);
                if (largeFraction.lessThan(LargeInteger.ZERO))
                    yield B && this.exponent2.equals(LargeFraction.ONE);
                else if (largeFraction.greaterThan(LargeInteger.ZERO))
                    yield B && this.exponent2.equals(LargeFraction.ZERO);
                yield B;
            }
            case LargePowerMonomial largePowerMonomial ->
                    largePowerMonomial.base.equals(this.base) && this.exponent.equals(largePowerMonomial.exponent)
                            && this.exponent2.equals(largePowerMonomial.exponent2);
            case RadicalTerm radicalTerm ->
                    new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, this).equals(radicalTerm);
            default -> throw new UnsupportedOperationException(
                    "Operation with these types together not supported yet");
        };
    }
    @Override
    public LargePowerMonomial abs(){return new LargePowerMonomial(base, exponent);}
    @Override
    public int compareMagnitudes(ScalarAtom other){
        return switch (other) {
            case LargeInteger LI -> {
                LargeInteger pow = exponent.denominator.lcm(exponent2.denominator);
                LargeFraction T = this.power(pow).base;
                LargeFraction O = LI.power(pow);
                yield T.compareMagnitudes(O);
            }
            case LargeFraction F -> {
                LargeInteger pow = exponent.denominator.lcm(exponent2.denominator);
                LargeFraction T = this.power(pow).base;
                LargeFraction O = F.power(pow);
                yield T.compareMagnitudes(O);
            }case LargePowerMonomial LPM -> {
                LargeInteger pow = exponent.denominator.lcm(exponent2.denominator, LPM.exponent.denominator,
                        LPM.exponent2.denominator);
                LargeFraction T = this.power(pow).base;
                LargeFraction O = LPM.power(pow).base;
                yield T.compareMagnitudes(O);
            }case RadicalTerm R -> {
                if (R.coefficient.equals(LargeFraction.ZERO)) yield this.compareMagnitudes(R.rationalPart);
                else if (R.rationalPart.equals(LargeFraction.ZERO)) {
                    LargeInteger pow = exponent.denominator.lcm(exponent2.denominator,
                            R.exponent.exponent.denominator, R.exponent.exponent2.denominator);
                    LargeFraction T = this.power(pow).base;
                    LargeFraction O = R.exponent.power(pow).base;
                    O = (LargeFraction) O.multiply(R.coefficient.power(pow));
                    yield T.compareMagnitudes(O);
                }
                LargeFraction F1 = new LargeFraction(floor(), LargeInteger.ONE);
                LargeFraction C1 = new LargeFraction(ceil(), LargeInteger.ONE);
                LargeFraction F2 = new LargeFraction(R.floor(), LargeInteger.ONE);
                LargeFraction C2 = new LargeFraction(R.ceil(), LargeInteger.ONE);
                if (F1.greaterThan(C2)) yield 1;
                else if (F2.greaterThan(C1)) yield -1;
                yield 0;
            }default -> this.abs().compareTo(other.abs());
        };
    }
    @Override
    public int compareTo(ScalarAtom other) {
        if(isNotReal() || other.isNotReal())
            throw new IllegalStateException("Complex numbers cannot be compared");
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
                LargePowerMonomial copy2 = new LargePowerMonomial(
                        largePowerMonomial.base, largePowerMonomial.exponent);
                LargeInteger lc = this.exponent.denominator.lcm(largePowerMonomial.exponent.denominator);
                copy = copy.power(lc);
                copy2 = copy2.power(lc);
                int C = new LargePowerMonomial(copy.base, copy.exponent).base
                        .compareMagnitudes(new LargePowerMonomial(copy2.base, copy2.exponent).base);
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
                if (radicalTerm.coefficient.equals(LargeInteger.ZERO))
                    yield this.compareTo(radicalTerm.rationalPart);
                LargeFraction l1 = new LargeFraction(this.floor(), LargeInteger.ONE);
                LargeFraction u1 = new LargeFraction(this.ceil(), LargeInteger.ONE);
                LargeFraction l2 = new LargeFraction(other.floor(), LargeInteger.ONE);
                LargeFraction u2 = new LargeFraction(other.ceil(), LargeInteger.ONE);
                while (l1.lessThan(u2) && l2.lessThan(u1)) {
                    LargeFraction mediant = new LargeFraction(l1.numerator.add(u1.numerator),
                            l1.denominator.add(u1.denominator));
                    if (mediant.lessThan(this))
                        l1 = mediant.clone();
                    else u1 = mediant.clone();
                    LargeFraction mediant2 = new LargeFraction(l2.numerator.add(u2.numerator),
                            l2.denominator.add(u2.denominator));
                    if (mediant2.lessThan(other))
                        l2 = mediant2.clone();
                    else u2 = mediant2.clone();
                }
                yield l1.compareTo(u2);
            }
            default -> throw new UnsupportedOperationException(
                    "Operation with these types together not supported yet");
        };
    }
    @Override
    public LargePowerMonomial negate() {
        return new LargePowerMonomial(this.base, this.exponent,
                (LargeFraction) this.exponent2.add(LargeFraction.ONE));}
    @Override
    public boolean isReal() {return exponent2.equals(LargeInteger.ONE) || exponent2.equals(LargeInteger.ZERO);}
    @Override
    public boolean isRational(){return exponent2.denominator.equals(LargeFraction.ONE)
            && exponent.equals(LargeFraction.ONE);}
    public boolean isNotInteger() {
        return exponent.doesntEqual(LargeInteger.ONE) || base.denominator.doesntEqual(LargeInteger.ONE)
                || this.isNotReal();}
    @Override
    public boolean isAlgebraic() {return true;}
    public LargePowerMonomial complexConjugate(){return new LargePowerMonomial(base, exponent, exponent2.negate());}
    @Override
    public ScalarAtom realPart(){
        RadicalTermSum RT = (RadicalTermSum) new LargePowerMonomial(LargeFraction.NEGATIVE_ONE, exponent2)
                .add(new LargePowerMonomial(LargeFraction.NEGATIVE_ONE, exponent2.negate()))
                .divide(LargeInteger.TWO);
        return RT.multiply(new LargePowerMonomial(base, exponent));
    }
    @Override
    public ScalarAtom imagPart(){return this.subtract(this.realPart()).divide(I);}
    @Override
    public LargeInteger floor(){
        if(this.isNotReal()) throw new IllegalStateException("Cannot floor a non-real atom");
        else if(this.isRational()) return base.floor();
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
                high = high.shiftLeft(1);
            }
            while(high.subtract(low).greaterThan(LargeInteger.ONE)){
                LargeInteger mid = low.add(high).shiftRight(1);
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
        return switch (other) {
            case LargeInteger integer ->
                    new RadicalTerm(new LargeFraction(integer, LargeInteger.ONE), LargeFraction.ONE, this);
            case LargeFraction largeFraction -> new RadicalTerm(largeFraction, LargeFraction.ONE, this);
            case LargePowerMonomial largePowerMonomial -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                rt.add(new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, this));
                rt.add(new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, largePowerMonomial));
                yield new RadicalTermSum(rt, LargeFraction.ZERO);
            }
            case RadicalTerm radicalTerm -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                rt.add(new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, this));
                rt.add(radicalTerm);
                yield new RadicalTermSum(rt, LargeFraction.ZERO);
            }
            case RadicalTermSum R -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                rt.add(new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, this));
                for (LargePowerMonomial L : R.sum.keys())
                    rt.add(new RadicalTerm(LargeFraction.ZERO, R.sum.get(L), L));
                yield new RadicalTermSum(rt, R.constant);
            }
            default -> throw new UnsupportedOperationException(
                    "Operation with these types together not supported yet");
        };
    }
    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        return switch (other) {
            case LargeInteger largeInteger -> {
                LargeMap<LargeInteger, LargeFraction> pr = this.factors;
                LargeMap<LargeInteger, LargeFraction> pr2 = largeInteger.factor();
                for (LargeInteger I : pr2.keys()) {
                    if (pr.containsKey(I))
                        pr.put(I, (LargeFraction) pr.get(I).add(pr2.get(I)));
                    else pr.put(I, pr2.get(I));
                }
                yield buildFromFactors(pr);
            }case LargeFraction largeFraction -> {
                LargeMap<LargeInteger, LargeFraction> pr = this.factors;
                LargeMap<LargeInteger, LargeFraction> pr2 = largeFraction.factor();
                for (LargeInteger I : pr2.keys()) {
                    if (pr.containsKey(I))
                        pr.put(I, (LargeFraction) pr.get(I).add(pr2.get(I)));
                    else pr.put(I, pr2.get(I));
                }
                yield buildFromFactors(pr);
            }case LargePowerMonomial L -> {
                LargeMap<LargeInteger, LargeFraction> pr = factors;
                LargeMap<LargeInteger, LargeFraction> pr2 = L.factors;
                for (LargeInteger I : pr2.keys()) {
                    if (pr.containsKey(I))
                        pr.put(I, (LargeFraction) pr.get(I).add(pr2.get(I)));
                    else pr.put(I, pr2.get(I));
                }
                yield buildFromFactors(pr);
            }case RadicalTerm radicalTerm -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                rt.add(new RadicalTerm(LargeFraction.ZERO, radicalTerm.rationalPart, this));
                rt.add(new RadicalTerm(LargeFraction.ZERO, radicalTerm.coefficient,
                        (LargePowerMonomial) radicalTerm.exponent.multiply(this)));
                yield new RadicalTermSum(rt, LargeFraction.ZERO);
            }case RadicalTermSum R -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                for (LargePowerMonomial L : R.sum.keys())
                    rt.add(new RadicalTerm(LargeFraction.ZERO, R.sum.get(L),
                            (LargePowerMonomial) L.multiply(this)));
                rt.add(new RadicalTerm(LargeFraction.ZERO, R.constant, this));
                yield new RadicalTermSum(rt, LargeFraction.ZERO);
            }
            default -> throw new UnsupportedOperationException(
                    "Operation with these types together not supported yet");
        };
    }
    @Override
    public LargePowerMonomial square(){return new LargePowerMonomial(this.base, (LargeFraction) this.exponent.multiply(LargeInteger.TWO), (LargeFraction) this.exponent2.multiply(LargeInteger.TWO));}
    @Override
    public LargePowerMonomial cube(){return new LargePowerMonomial(this.base, (LargeFraction) this.exponent.multiply(LargeInteger.THREE), (LargeFraction) this.exponent2.multiply(LargeInteger.THREE));}
    public LargePowerMonomial power(LargeInteger I){return new LargePowerMonomial(base, (LargeFraction) exponent.multiply(I), (LargeFraction) exponent2.multiply(I));}
    public LargePowerMonomial power(LargeFraction f){return new LargePowerMonomial(base, (LargeFraction) exponent.multiply(f), (LargeFraction) exponent2.multiply(f));}
    public LargeDecimal approximateToDecimal(LargeInteger precision){return null;}
    public LargeMap<LargeInteger, LargeFraction> factor(){return factors.copy();}
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
                expTemp = expTemp.shiftRight(1);
            }
        }
        return new LargePowerMonomial(base, G, exponent2, pr);
    }
    @Override
    public LargePowerMonomial reciprocal(){return new LargePowerMonomial(base, exponent.negate(), exponent2.negate());}
}
