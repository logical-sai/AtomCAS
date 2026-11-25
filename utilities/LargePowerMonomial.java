package com.AtomCAS.utilities;
public final class LargePowerMonomial extends ScalarAtom {
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
                throw new ArithmeticException("Indeterminate form 0^0");
            this.base = LargeFraction.ONE;
            this.exponent = LargeFraction.ONE;
            this.exponent2 = exponent2.mod(LargeFraction.TWO);
            return;
        }
        // 0^negative is undefined; 0^positive = 0.
        if (base.isZero()) {
            if (exponent.isNegative())
                throw new ArithmeticException("Cannot divide by 0");
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
        if (base.isOne()) {
            this.base = LargeFraction.ONE;
            this.exponent = LargeFraction.ONE;
            this.exponent2 = exponent2;
            return;
        }
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
    public LargePowerMonomial(String str) {
        ScalarAtom R = Parser.parse(str);
        if (R instanceof LargeInteger integer) {
            base = LargeFraction.valueOf(integer);
            exponent = LargeFraction.ONE;
            exponent2 = LargeFraction.ZERO;
            return;
        } else if (R instanceof LargeFraction F) {
            base = F;
            exponent = LargeFraction.ONE;
            exponent2 = LargeFraction.ZERO;
            return;
        } else if (R instanceof LargePowerMonomial LPM) {
            base = LPM.base;
            exponent = LPM.exponent;
            exponent2 = LPM.exponent2;
            return;
        } else if (R instanceof RadicalTerm RT) {
            if(!RT.rationalPart.isZero())
                throw new IllegalArgumentException("Inputted expression does not evaluate to a power monomial");
            LargePowerMonomial LM = (LargePowerMonomial) RT.exponent.multiply(RT.coefficient);
            base = LM.base;
            exponent = LM.exponent;
            exponent2 = LM.exponent2;
            return;
        }
        R = Parser.parse(R.toString());
        switch (R) {
            case LargeInteger integer -> {
                base = LargeFraction.valueOf(integer);
                exponent = LargeFraction.ONE;
                exponent2 = LargeFraction.ZERO;
            } case LargeFraction F -> {
                base = F;
                exponent = LargeFraction.ONE;
                exponent2 = LargeFraction.ZERO;
            } case LargePowerMonomial LPM -> {
                base = LPM.base;
                exponent = LPM.exponent;
                exponent2 = LPM.exponent2;
            } case RadicalTerm RT -> {
                if (!RT.rationalPart.isZero())
                    throw new IllegalArgumentException("Inputted expression does not evaluate to a power monomial");
                LargePowerMonomial LM = (LargePowerMonomial) RT.exponent.multiply(RT.coefficient);
                base = LM.base;
                exponent = LM.exponent;
                exponent2 = LM.exponent2;
            } case null, default ->
                    throw new IllegalArgumentException("Inputted expression does not evaluate to a power monomial");
        }
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
        if(base.isOne()){
            if(str.isEmpty() || str.toString().equals("-")) str.append("1");
            return str.toString();
        }
        if(!str.toString().equals("-") && !str.isEmpty()) str.append("*");
        if(!base.denominator.isOne() && !exponent.isOne())
            str.append("(").append(base).append(")");
        else str.append(base.abs());
        if(exponent.doesntEqual(LargeFraction.ONE)) str.append("^(").append(exponent).append(")");
        return str.toString();
    }
    @Override
    public String toLatex() {
        StringBuilder str = new StringBuilder();
        if (exponent2.doesntEqual(LargeFraction.ZERO)) {
            if (exponent2.isOne()) str.append("-");
            else str.append("(-1)^{").append(exponent2.toLatex()).append("}");
        }
        if (base.isOne()) {
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
                boolean B = base.equals(largeInteger.abs()) && this.exponent.isOne();
                if (largeInteger.isNegative())
                    yield B && exponent2.isOne();
                else if (largeInteger.greaterThan(LargeInteger.ZERO))
                    yield B && exponent2.isZero();
                yield B;
            }
            case LargeFraction largeFraction -> {
                boolean B = this.base.equals(largeFraction.abs()) && this.exponent.isOne();
                if (largeFraction.numerator.isNegative())
                    yield B && this.exponent2.isOne();
                else if (largeFraction.greaterThan(LargeInteger.ZERO))
                    yield B && this.exponent2.isZero();
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
    int compareMagnitudes(ScalarAtom other){
        return switch (other) {
            case LargeInteger LI -> {
                LargeInteger pow = exponent.denominator.lcm(exponent2.denominator);
                LargeFraction T = this.power(pow).base;
                LargeFraction O = LI.power(pow);
                yield T.compareMagnitudes(O);
            } case LargeFraction F -> {
                LargeInteger pow = exponent.denominator.lcm(exponent2.denominator);
                LargeFraction T = this.power(pow).base;
                LargeFraction O = F.power(pow);
                yield T.compareMagnitudes(O);
            } case LargePowerMonomial LPM -> {
                LargeInteger pow = exponent.denominator.lcm(exponent2.denominator, LPM.exponent.denominator,
                        LPM.exponent2.denominator);
                LargeFraction T = this.power(pow).base;
                LargeFraction O = LPM.power(pow).base;
                yield T.compareMagnitudes(O);
            } case RadicalTerm R -> {
                if (R.coefficient.isZero()) yield this.compareMagnitudes(R.rationalPart);
                else if (R.rationalPart.isZero()) {
                    LargeInteger pow = exponent.denominator.lcm(exponent2.denominator,
                            R.exponent.exponent.denominator, R.exponent.exponent2.denominator);
                    LargeFraction T = this.power(pow).base;
                    LargeFraction O = R.exponent.power(pow).base;
                    O = O.multiply(R.coefficient.power(pow));
                    yield T.compareMagnitudes(O);
                }
                LargeFraction F1 = LargeFraction.valueOf(floor());
                LargeFraction C1 = LargeFraction.valueOf(ceil());
                LargeFraction F2 = LargeFraction.valueOf(R.floor());
                LargeFraction C2 = LargeFraction.valueOf(R.ceil());
                if (F1.greaterThan(C2)) yield 1;
                else if (F2.greaterThan(C1)) yield -1;
                yield 0;
            } default -> super.compareMagnitudes(other);
        };
    }
    @Override
    public int compareTo(ScalarAtom other) {
        if(isComplex() || other.isComplex())
            throw new IllegalStateException("Complex numbers cannot be compared");
        return switch (other) {
            case LargeFraction largeFraction -> compareTo(new LargePowerMonomial(largeFraction, LargeFraction.ONE));
            case LargeInteger largeInteger -> {
                LargePowerMonomial copy = new LargePowerMonomial(base, exponent);
                LargePowerMonomial copy2 = new LargePowerMonomial(LargeFraction.valueOf(largeInteger), LargeFraction.ONE);
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
                LargeFraction l1 = LargeFraction.valueOf(floor());
                LargeFraction u1 = l1.equals(this) ? l1.clone() : (l1.isNegative() ? l1.add(1) : l1.clone());
                LargeFraction l2 = LargeFraction.valueOf(other.floor());
                LargeFraction u2 = l2.equals(other) ? l2.clone() : (l2.isNegative() ? l2.add(1) : l2.clone());
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
    public boolean isRational(){return exponent2.denominator.isOne() && exponent.isOne();}
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
    public LargeInteger floor() {
        if(this.isComplex()) throw new IllegalStateException("Cannot floor a non-real atom");
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
    public LargeInteger ceil() {return this.isRational() ? base.ceil() : floor().add(1);}
    @Override
    public ScalarAtom add(ScalarAtom other) {
        return switch (other) {
            case LargeInteger integer -> new RadicalTerm(LargeFraction.valueOf(integer), LargeFraction.ONE, this);
            case LargeFraction largeFraction -> new RadicalTerm(largeFraction, LargeFraction.ONE, this);
            case LargePowerMonomial largePowerMonomial -> {
                LargeMap<LargePowerMonomial, LargeFraction> rt = new LargeMap<>();
                rt.put(this, LargeFraction.ONE);
                rt.put(largePowerMonomial, LargeFraction.ONE);
                yield new RadicalTermSum(rt, LargeFraction.ONE);
            } case RadicalTerm radicalTerm -> {
                if(radicalTerm.exponent.equals(this))
                    yield new RadicalTerm(radicalTerm.rationalPart, radicalTerm.coefficient.add(1), this);
                LargeList<RadicalTerm> rt = new LargeList<>();
                rt.add(new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, this));
                rt.add(radicalTerm);
                yield new RadicalTermSum(rt, LargeFraction.ZERO);
            } case RadicalTermSum R -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                rt.add(new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, this));
                for (LargePowerMonomial L : R.sum.keys())
                    rt.add(new RadicalTerm(LargeFraction.ZERO, R.sum.get(L), L));
                yield new RadicalTermSum(rt, R.constant);
            } default -> throw new UnsupportedOperationException(
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
            case LargeInteger integer -> {
                if (integer.isZero()) yield ZERO;
                LargeFraction signExponent = integer.isNegative() ? LargeFraction.ONE : LargeFraction.ZERO;
                yield multiply(new LargePowerMonomial(LargeFraction.valueOf(integer.abs()),
                        LargeFraction.ONE, signExponent));
            } case LargeFraction F -> {
                if (F.isZero()) yield ZERO;
                LargeFraction signExponent = F.isNegative() ? LargeFraction.ONE : LargeFraction.ZERO;
                yield multiply( new LargePowerMonomial(F.abs(), LargeFraction.ONE, signExponent));
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
        return new LargePowerMonomial(base, exponent.multiply(2), exponent2.multiply(2));}
    @Override
    public LargePowerMonomial cube() {
        return new LargePowerMonomial(base, exponent.multiply(3), exponent2.multiply(3));}
    public LargePowerMonomial power(LargeInteger I) {
        return new LargePowerMonomial(base, (LargeFraction) exponent.multiply(I),
                (LargeFraction) exponent2.multiply(I));}
    public LargePowerMonomial power(LargeFraction f){ return new LargePowerMonomial(base, exponent.multiply(f),
            exponent2.multiply(f));}
    public LargeMap<LargeInteger, LargeFraction> factor() {
        LargeMap<LargeInteger, LargeFraction> map = new LargeMap<>();
        if(!exponent2.isZero()) map.put(LargeInteger.NEGATIVE_ONE, exponent2);
        LargeMap<LargeInteger, LargeFraction> map2 = base.factor();
        for(LargeInteger I : map2.keys()) map.put(I, map2.get(I).multiply(exponent));
        return map;
    }
    public static LargePowerMonomial buildFromFactors(
            LargeMap<LargeInteger, LargeFraction> pr) {

        LargeFraction base = LargeFraction.ONE;
        LargeFraction exponent2 = LargeFraction.ZERO;
        LargeFraction G = LargeFraction.ZERO;

        // Determine the common exponent and phase.
        for (LargeInteger I : pr.keys()) {
            LargeFraction power = pr.get(I);

            if (I.equals(LargeInteger.NEGATIVE_ONE)) {
                exponent2 = exponent2.add(power);
                continue;
            }

            if (power.isZero()) continue;

            G = G.isZero()
                    ? power.abs()
                    : G.gcd(power.abs());
        }

        // No positive-base factors remain.
        if (G.isZero())
            return new LargePowerMonomial(
                    LargeFraction.ONE,
                    LargeFraction.ONE,
                    exponent2);

        // Reconstruct the common base.
        for (LargeInteger I : pr.keys()) {
            if (I.equals(LargeInteger.NEGATIVE_ONE))
                continue;

            LargeFraction power = pr.get(I).divide(G);

            if (!power.denominator.isOne())
                throw new AssertionError(
                        "Nonintegral reconstruction exponent: " + power);

            base = base.multiply(I.power(power.numerator));
        }

        return new LargePowerMonomial(base, G, exponent2);
    }
    @Override
    public LargePowerMonomial reciprocal(){return new LargePowerMonomial(base, exponent.negate(), exponent2.negate());}
    @Override
    public LargePolynomial annihilatingPolynomial() {
        LargeInteger n = exponent.denominator.lcm(exponent2.denominator);
        LargePowerMonomial raised = power(n);
        if (!raised.isRational())
            throw new AssertionError("Expected rational power");
        LargeFraction value = raised.exponent2.isOne() ? raised.base.negate() : raised.base;
        return LargePolynomial.monomial(LargeFraction.valueOf(value.denominator), n)
                .subtract(LargeFraction.valueOf(value.numerator));
    }
}
