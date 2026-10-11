package com.AtomCAS.utilities;
public class LargePowerMonomial extends ScalarAtom{
    final LargeFraction base;
    final LargeFraction exponent;
    final LargeFraction exponent2;

    public static final LargePowerMonomial ZERO = new LargePowerMonomial(LargeFraction.ZERO, LargeFraction.ONE);
    public static final LargePowerMonomial ONE = new LargePowerMonomial(LargeFraction.ONE, LargeFraction.ONE);
    public static final LargePowerMonomial I = new LargePowerMonomial(LargeFraction.NEGATIVE_ONE, LargeFraction.HALF);
    public LargePowerMonomial(LargeFraction num, LargeFraction dem) {this(num, dem, LargeFraction.ZERO);}
    public LargePowerMonomial(LargeFraction base, LargeFraction exponent, LargeFraction exponent2) {
        // Handle exponent first, because 0^0 must not become 0.
        if (exponent.isZero()) {
            if (base.isZero())
                throw new IllegalArgumentException("Indeterminate form 0^0");
            this.base = LargeFraction.ONE;
            this.exponent = LargeFraction.ONE;
            this.exponent2 = exponent2.mod(LargeFraction.TWO);
            return;
        }
        // 0^negative is undefined; 0^positive = 0.
        if (base.isZero()) {
            if (exponent.isNegative())
                throw new IllegalArgumentException("Cannot divide by 0");
            this.base = LargeFraction.ZERO;
            this.exponent = LargeFraction.ONE;
            this.exponent2 = LargeFraction.ZERO;
            return;
        }
        // (-b)^e = (-1)^e * b^e
        if (base.isNegative()) {
            exponent2 = exponent2.add(exponent);
            base = base.negate();
        }
        exponent2 = exponent2.mod(LargeFraction.TWO);
        PowerFormFraction form = base.asPowerForm();
        exponent = exponent.multiply(form.exponent());
        base = form.base();
        if (exponent.isNegative()) {
            base = base.reciprocal();
            exponent = exponent.negate();
        }
        if (exponent.denominator.isOne()) {
            this.base = base.power(exponent.numerator);
            this.exponent = LargeFraction.ONE;
            this.exponent2 = exponent2;
            return;
        }
        this.base = base;
        this.exponent = exponent;
        this.exponent2 = exponent2;
    }
    @Override
    public LargePowerMonomial clone() {
        return new LargePowerMonomial(base.clone(), exponent.clone(), exponent2.clone());}
    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        if(exponent2.doesntEqual(LargeFraction.ZERO)){
            if(exponent2.doesntEqual(LargeFraction.ONE))
                str.append("(-1)^(").append(exponent2).append(")");
            else str.append("-");
        }
        if(base.equals(LargeFraction.ONE)){
            if(str.isEmpty() || str.toString().equals("-")) str.append("1");
            return str.toString();
        }
        if(!str.toString().equals("-") && !str.isEmpty()) str.append("*");
        if(base.denominator.doesntEqual(LargeInteger.ONE) && exponent.doesntEqual(LargeFraction.ONE))
            str.append("(").append(base).append(")");
        else str.append(base.abs());
        if(exponent.doesntEqual(LargeFraction.ONE)) str.append("^(").append(exponent).append(")");
        return str.toString();
    }
    @Override
    public String toLatex() {
        StringBuilder str = new StringBuilder();
        if (exponent2.doesntEqual(LargeFraction.ZERO)) {
            if (exponent2.equals(LargeFraction.ONE)) str.append("-");
            else str.append("(-1)^{").append(exponent2.toLatex()).append("}");
        }
        if (base.equals(LargeFraction.ONE)) {
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
                if (largeInteger.isNegative())
                    yield B && this.exponent2.equals(LargeFraction.ONE);
                else if (largeInteger.greaterThan(LargeInteger.ZERO))
                    yield B && this.exponent2.equals(LargeFraction.ZERO);
                yield B;
            }
            case LargeFraction largeFraction -> {
                boolean B = this.base.equals(largeFraction.abs()) && this.exponent.equals(LargeFraction.ONE);
                if (largeFraction.numerator.isNegative())
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
    public boolean isZero() {return base.isZero();}
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
            }case LargeFraction F -> {
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
                    O = O.multiply(R.coefficient.power(pow));
                    yield T.compareMagnitudes(O);
                }
                LargeFraction F1 = new LargeFraction(floor());
                LargeFraction C1 = new LargeFraction(ceil());
                LargeFraction F2 = new LargeFraction(R.floor());
                LargeFraction C2 = new LargeFraction(R.ceil());
                if (F1.greaterThan(C2)) yield 1;
                else if (F2.greaterThan(C1)) yield -1;
                yield 0;
            }default -> super.compareMagnitudes(other);
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
                LargePowerMonomial copy2 = new LargePowerMonomial(new LargeFraction(largeInteger), LargeFraction.ONE);
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
                    if (this.sign().isOne()) yield 1;
                    yield -1;
                } else if (C == -1) {
                    if (other.sign().isOne()) yield -1;
                    yield 1;
                }
                yield C;
            }case RadicalTerm radicalTerm -> {
                //I present to you...ERROR BOUNDS
                if (radicalTerm.coefficient.isZero())
                    yield this.compareTo(radicalTerm.rationalPart);
                LargeFraction l1 = new LargeFraction(this.floor());
                LargeFraction u1 = new LargeFraction(this.ceil());
                LargeFraction l2 = new LargeFraction(other.floor());
                LargeFraction u2 = new LargeFraction(other.ceil());
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
        return new LargePowerMonomial(this.base, this.exponent, this.exponent2.add(LargeFraction.ONE));}
    @Override
    public boolean isReal() {return exponent2.isOne() || exponent2.isZero();}
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
        boolean negativeResult = exponent2.isOne();
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
                    new RadicalTerm(new LargeFraction(integer), LargeFraction.ONE, this);
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
    public LargePowerMonomial multiply(LargePowerMonomial other) {
        if (base.isZero() || other.base.isZero())
            return ZERO;
        // Cheap important fast path.
        if (base.equals(other.base))
            return new LargePowerMonomial(base, exponent.add(other.exponent), exponent2.add(other.exponent2));
        LargeFraction common = exponent.gcd(other.exponent);
        LargeFraction exp1 = exponent.divide(common);
        LargeFraction exp2 = other.exponent.divide(common);
        // By definition of rational gcd, these must be integers.
        LargeInteger power1 = exp1.numerator;
        LargeInteger power2 = exp2.numerator;
        LargeFraction combinedBase = base.power(power1).multiply(other.base.power(power2));
        return new LargePowerMonomial(combinedBase, common, exponent2.add(other.exponent2));
    }
    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        return switch (other) {
            case LargeInteger _, LargeFraction _ -> {
                LargeFraction f = (LargeFraction) other.divide(LargeInteger.ONE);
                if (f.isZero()) yield ZERO;
                LargeFraction signExponent = f.isNegative() ? LargeFraction.ONE : LargeFraction.ZERO;
                yield multiply( new LargePowerMonomial(f.abs(), LargeFraction.ONE, signExponent));
            } case LargePowerMonomial L -> multiply(L);
            case RadicalTerm radicalTerm -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                rt.add(new RadicalTerm(LargeFraction.ZERO, radicalTerm.rationalPart, this));
                rt.add(new RadicalTerm(LargeFraction.ZERO, radicalTerm.coefficient,
                        radicalTerm.exponent.multiply(this)));
                yield new RadicalTermSum(rt, LargeFraction.ZERO);
            } case RadicalTermSum R -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                for (LargePowerMonomial L : R.sum.keys())
                    rt.add(new RadicalTerm(LargeFraction.ZERO, R.sum.get(L), L.multiply(this)));
                rt.add(new RadicalTerm(LargeFraction.ZERO, R.constant, this));
                yield new RadicalTermSum(rt, LargeFraction.ZERO);
            } default -> throw new UnsupportedOperationException(
                    "Operation with these types together not supported yet");
        };
    }
    @Override
    public LargePowerMonomial square() {
        return new LargePowerMonomial(this.base, (LargeFraction) this.exponent.multiply(LargeInteger.TWO),
                (LargeFraction) this.exponent2.multiply(LargeInteger.TWO));}
    @Override
    public LargePowerMonomial cube() {
        return new LargePowerMonomial(this.base, (LargeFraction) this.exponent.multiply(LargeInteger.THREE),
                (LargeFraction) this.exponent2.multiply(LargeInteger.THREE));}
    public LargePowerMonomial power(LargeInteger I) {
        return new LargePowerMonomial(base, (LargeFraction) exponent.multiply(I),
                (LargeFraction) exponent2.multiply(I));}
    public LargePowerMonomial power(LargeFraction f){ return new LargePowerMonomial(base, exponent.multiply(f),
            exponent2.multiply(f));}
    public LargeDecimal approximateToDecimal(LargeInteger precision) {return null;}
    public LargeMap<LargeInteger, LargeFraction> factor() {
        LargeMap<LargeInteger, LargeFraction> map = new LargeMap<>();
        if(!exponent2.isZero()) map.put(LargeInteger.NEGATIVE_ONE, exponent2);
        LargeMap<LargeInteger, LargeFraction> map2 = base.factor();
        for(LargeInteger I : map2.keys()) map.put(I, map2.get(I).multiply(exponent));
        return map;
    }
    public static LargePowerMonomial buildFromFactors(LargeMap<LargeInteger, LargeFraction> pr) {
        LargeFraction base = LargeFraction.ONE;
        LargeFraction exponent2 = LargeFraction.ZERO;
        if(pr.size().isZero()) return ONE;
        else if(pr.size().isOne())
            return new LargePowerMonomial(new LargeFraction(pr.keyAtIndex(LargeInteger.ZERO)),
                    pr.get(pr.keyAtIndex(LargeInteger.ZERO)));
        LargeFraction G = LargeFraction.ONE;
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(pr.size()); i = i.add(1)){
            LargeInteger I = pr.keyAtIndex(i);
            if(I.equals(LargeInteger.NEGATIVE_ONE)){
                exponent2 = exponent2.add(pr.get(I));
                pr.remove(I);
                i = i.subtract(1);
                continue;
            }
            G = G.gcd(pr.get(I));
        }
        for(LargeInteger I: pr.keys()){
            LargeInteger expTemp = pr.get(I).divide(G).numerator;
            LargeInteger basTemp = I.clone();
            while(expTemp.doesntEqual(LargeInteger.ZERO)){
                if(expTemp.isOdd()){
                    if(expTemp.isNegative())
                        base = (LargeFraction) base.divide(basTemp);
                    else base = (LargeFraction) base.multiply(basTemp);
                    if(expTemp.abs().isOne()) break;
                }
                basTemp = basTemp.square();
                expTemp = expTemp.shiftRight(1);
            }
        }
        return new LargePowerMonomial(base, G, exponent2);
    }
    @Override
    public LargePowerMonomial reciprocal(){return new LargePowerMonomial(base, exponent.negate(), exponent2.negate());}
}
