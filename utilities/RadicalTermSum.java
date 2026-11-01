package com.AtomCAS.utilities;
public final class RadicalTermSum extends ScalarAtom{
    final LargeMap<LargePowerMonomial, LargeFraction> sum;
    final LargeFraction constant;
    public RadicalTermSum(){sum = new LargeMap<>(); constant = LargeFraction.ZERO;}

    private static void addCoefficient(LargeMap<LargePowerMonomial, LargeFraction> map, LargePowerMonomial key,
            LargeFraction value) {
        if (value.isZero()) return;
        if (!map.put(key, value)) {
            LargeFraction combined = map.get(key).add(value);
            if (combined.isZero()) map.remove(key);
            else map.set(key, combined);
        }
    }

    public RadicalTermSum(LargeList<RadicalTerm> r, LargeFraction cons) {
        LargeMap<LargePowerMonomial, LargeFraction> rt = new LargeMap<>();
        LargeFraction CON = cons.clone();
        if(r.isEmpty()){
            constant = CON.clone();
            sum = new LargeMap<>();
            return;
        }
        for(RadicalTerm rx: r) {
            addCoefficient(rt, rx.exponent, rx.coefficient);
            CON = CON.add(rx.rationalPart);
        }
        //handle roots of unity
        LargeInteger lc = LargeInteger.ONE;
        for (LargePowerMonomial L: rt.keys()) {
            if (L.isComplex())
                lc = lc.lcm(L.exponent2.denominator);
        }
        LargeInteger lt = lc.shiftLeft(1).totient();
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(rt.size()); i=i.add(1)) {
            LargePowerMonomial L = rt.keyAtIndex(i);
            if (L.isComplex() && L.exponent2.multiply(lc).greaterThanOrEqualTo(lt)) {
                LargePolynomial P = LargePolynomial.cyclotomic(lc.shiftLeft(1));
                P.coefficients.removeFirst();
                P = P.negate();
                ScalarAtom S = P.evaluateAt(new LargePowerMonomial(LargeFraction.NEGATIVE_ONE, lc.reciprocal()));
                S = S.multiply(new LargePowerMonomial(L.base, L.exponent, L.exponent2.multiply(LargeFraction.valueOf(lc))
                                .subtract(LargeFraction.valueOf(lt)).divide(LargeFraction.valueOf(lc))));
                S = S.multiply(rt.get(L));
                rt.remove(L);
                i = i.subtract(1);
                switch (S) {
                    case LargeInteger largeInteger -> CON = CON.add(LargeFraction.valueOf(largeInteger));
                    case LargeFraction largeFraction -> CON = CON.add(largeFraction);
                    case LargePowerMonomial largePowerMonomial -> rt.put(largePowerMonomial, LargeFraction.ONE);
                    case RadicalTerm R -> {
                        rt.put(R.exponent, R.coefficient);
                        CON = CON.add(R.rationalPart);
                    } case RadicalTermSum RTS -> {
                        for (LargePowerMonomial l : RTS.sum.keys())
                            addCoefficient(rt, l, RTS.sum.get(l));
                        CON = CON.add(RTS.constant);
                    } default -> throw new AssertionError("This should not happen for any inputs");
                }
            }
        }
        //eliminate zeros
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(rt.size()); i = i.add(1)) {
            if(rt.get(rt.keyAtIndex(i)).isZero()){
                rt.remove(rt.keyAtIndex(i));
                i = i.subtract(1);
            }
        }
        //sort radicands
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(rt.size().subtract(LargeInteger.ONE)); i = i.add(1)){
            for(LargeInteger j = i.add(1); j.lessThan(rt.size()); j = j.add(1)){
                if(rt.keyAtIndex(j).compareMagnitudes(rt.keyAtIndex(i)) < 0)
                    rt.swapIndices(i, j);
            }
        }
        sum = rt;
        this.constant = CON;
    }
    public RadicalTermSum(LargeMap<LargePowerMonomial, LargeFraction> sum, LargeFraction constant){
        LargeMap<LargePowerMonomial, LargeFraction> rt = new LargeMap<>();
        LargeFraction CON = constant.clone();
        if(sum.isEmpty()){
            this.constant = CON.clone();
            this.sum = new LargeMap<>();
            return;
        }
        for (LargePowerMonomial L : sum.keys()) {
            RadicalTerm normalized = new RadicalTerm(LargeFraction.ZERO, sum.get(L), L);
            CON = CON.add(normalized.rationalPart);
            addCoefficient(rt, normalized.exponent, normalized.coefficient);
        }
        //handle roots of unity
        LargeInteger lc = LargeInteger.ONE;
        for (LargePowerMonomial L: rt.keys()) {
            if (L.isComplex())
                lc = lc.lcm(L.exponent2.denominator);
        }
        LargeInteger lt = lc.shiftLeft(1).totient();
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(rt.size()); i=i.add(1)) {
            LargePowerMonomial L = rt.keyAtIndex(i);
            if (L.isComplex() && L.exponent2.multiply(lc).greaterThanOrEqualTo(lt)) {
                LargePolynomial P = LargePolynomial.cyclotomic(lc.shiftLeft(1));
                P.coefficients.removeFirst();
                P = P.multiply(LargeFraction.NEGATIVE_ONE);
                ScalarAtom S = P.evaluateAt(new LargePowerMonomial(LargeFraction.NEGATIVE_ONE, lc.reciprocal()));
                for (LargeInteger j = LargeInteger.ZERO; j.lessThan(L.exponent2.multiply(lc).subtract(lt));
                     j = j.add(1))
                    S = S.multiply(new LargePowerMonomial(LargeFraction.NEGATIVE_ONE, lc.reciprocal()));
                S = S.multiply(new LargePowerMonomial(L.base, L.exponent));
                S = S.multiply(rt.get(L));
                rt.remove(L);
                i = i.subtract(1);
                if(S instanceof LargeInteger || S instanceof LargeFraction)
                    CON = (LargeFraction) CON.add(S);
                else if(S instanceof LargePowerMonomial)
                    rt.put((LargePowerMonomial) S, LargeFraction.ONE);
                else if(S instanceof RadicalTerm){
                    rt.put(((RadicalTerm) S).exponent, ((RadicalTerm) S).coefficient);
                    CON = CON.add(((RadicalTerm) S).rationalPart);
                }else if(S instanceof RadicalTermSum R){
                    for(LargePowerMonomial l: R.sum.keys())
                        addCoefficient(rt, l, R.sum.get(l));
                    CON = CON.add(R.constant);
                }
            }
        }
        //eliminate ZEROs
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(rt.size()); i = i.add(1)){
            if(rt.get(rt.keyAtIndex(i)).isZero()){
                rt.remove(rt.keyAtIndex(i));
                i = i.subtract(1);
            }
        }
        //sort radicands
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(rt.size().subtract(1)); i = i.add(1)){
            for(LargeInteger j = i.add(1); j.lessThan(rt.size()); j = j.add(1)){
                if(rt.keyAtIndex(j).compareMagnitudes(rt.keyAtIndex(i)) < 0)
                    rt.swapIndices(i, j);
            }
        }
        this.sum = rt;
        this.constant = CON;
    }
    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        if(!constant.isZero())
            str.append(constant);
        if(sum.isEmpty() && constant.isZero()){
            str.append("0");
            return str.toString();
        }
        for (LargePowerMonomial L : sum.keys()){
            if(!constant.isZero() || !sum.indexOfKey(L).isZero()){
                if (sum.get(L).isNegative())
                    str.append(" - ");
                else if (sum.get(L).greaterThan(LargeInteger.ZERO))
                    str.append(" + ");
            }else{
                if (sum.get(L).isNegative())
                    str.append("-");
            }
            if (!sum.get(L).isOne() && sum.get(L).doesntEqual(LargeInteger.NEGATIVE_ONE))
                str.append(sum.get(L).abs()).append("*");
            str.append(L);
        }
        return str.toString();
    }

    @Override
    public String toLatex() {return "";}
    @Override
    public RadicalTermSum clone() {
        return new RadicalTermSum(sum.clone(), constant.clone());
    }
    @Override
    public boolean equals(Atom other) {
        if(other == this) return true;
        else if(!(other instanceof ScalarAtom)) return false;
        return switch (other) {
            case LargeInteger integer -> constant.equals(integer) && sum.isEmpty();
            case LargeFraction largeFraction -> constant.equals(largeFraction)
                    && sum.isEmpty();
            case LargePowerMonomial L -> {
                if (sum.size().greaterThan(LargeInteger.ONE)) yield false;
                if (L.isRational()) {
                    boolean B = constant.abs().equals(L.base) && sum.isEmpty();
                    if (!B) yield false;
                    if (L.exponent2.isOne()) yield constant.isNegative();
                    yield true;
                }
                yield sum.keyAtIndex(LargeInteger.ZERO).multiply(sum.get(sum.keyAtIndex(LargeInteger.ZERO)))
                        .equals(L) && constant.isZero();
            }
            case RadicalTerm radicalTerm -> subtract(radicalTerm).isZero();
            case RadicalTermSum R -> {
                if (this.constant.doesntEqual(R.constant))
                    yield false;
                if (this.sum.size().doesntEqual(R.sum.size()))
                    yield false;
                for (LargeInteger i = LargeInteger.ZERO; i.lessThan(sum.size()); i = i.add(1))
                    if (sum.keyAtIndex(i).doesntEqual(R.sum.keyAtIndex(i)) ||
                            sum.get(sum.keyAtIndex(i)).doesntEqual(R.sum.get(sum.keyAtIndex(i))))
                        yield false;
                yield true;
            } default -> throw new UnsupportedOperationException(
                    "Operation with these types together not supported yet");
        };
    }
    @Override
    public boolean isZero() {return constant.isZero() && sum.isEmpty();}
    public boolean isReal() {
        for (LargePowerMonomial L: sum.keys())
            if (L.isComplex()) return false;
        return true;
    }
    public boolean isRational() {return sum.isEmpty();}
    public boolean isAlgebraic() {return true;}
    @Override
    public ScalarAtom realPart(){return sign().realPart().multiply(abs());}
    @Override
    public ScalarAtom imagPart(){return sign().imagPart().multiply(abs());}
    @Override
    public LargeInteger floor() {
        if(this.isComplex()) throw new IllegalStateException("Cannot floor or apply ceiling a non-real atom");
        else if(this.isRational()) return constant.floor();
        LargeList<LargeFraction> floors = new LargeList<>();
        LargeList<LargeFraction> ceilings = new LargeList<>();
        for(LargePowerMonomial r: this.sum.keys()){
            LargeFraction F = LargeFraction.valueOf(r.multiply(sum.get(r)).floor());
            floors.add(F);
            if(!r.exponent.isOne() || !r.base.denominator.isOne() || r.isComplex()) F = F.add(1);
            ceilings.add(F);
        }
        for(;;) {
            LargeFraction floorSum = LargeFraction.ZERO;
            LargeFraction ceilSum = LargeFraction.ZERO;
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(floors.size()); i = i.add(1)){
                LargeFraction mediant = new LargeFraction(floors.get(i).numerator.add(ceilings.get(i).numerator),
                        floors.get(i).denominator.add(ceilings.get(i).denominator));
                if(mediant.lessThan(sum.keyAtIndex(i).multiply(sum.get(sum.keyAtIndex(i)))))
                    floors.set(i, mediant);
                else ceilings.set(i, mediant);
                floorSum = floorSum.add(floors.get(i));
                ceilSum = ceilSum.add(ceilings.get(i));
            }
            floorSum = floorSum.add(constant);
            ceilSum = ceilSum.add(constant);
            if(ceilSum.ceil().subtract(1).equals(floorSum.floor()) || ceilSum.equals(floorSum))
                return floorSum.floor();
        }
    }
    @Override
    public LargeInteger ceil() {return sum.isEmpty() ? constant.ceil() : floor().add(1);}
    public RadicalTermSum complexConjugate() {
        LargeList<RadicalTerm> rt = new LargeList<>();
        for(LargePowerMonomial L: sum.keys())
            rt.add(new RadicalTerm(LargeFraction.ZERO, sum.get(L), L.complexConjugate()));
        return new RadicalTermSum(rt, this.constant);
    }
    @Override
    public ScalarAtom abs() {
        if (isComplex()){
            RadicalTermSum R = multiply(complexConjugate());
            if(R.isRational()) return new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE,
                    new LargePowerMonomial(R.constant, LargeFraction.HALF));
            return new LargePowerNode(R, LargeFraction.HALF, LargeFraction.ONE);
        }
        if (isZero())
            return LargeInteger.ZERO;
        LargeInteger f = this.floor();
        LargeInteger c = f.add(1);
        if (f.greaterThanOrEqualTo(LargeInteger.ZERO) && c.greaterThanOrEqualTo(LargeInteger.ONE))
            return this;
        return this.negate();
    }
    @Override
    public int compareTo(ScalarAtom other){
        if(isComplex() || other.isComplex())
            throw new IllegalStateException("Complex numbers cannot be compared");
        return ((LargeInteger)subtract(other).sign()).intValue();
    }
    @Override
    public RadicalTermSum negate() {
        LargeMap<LargePowerMonomial, LargeFraction> rt = new LargeMap<>();
        for(LargePowerMonomial m : sum.keys())
            rt.put(m, sum.get(m).negate());
        return new RadicalTermSum(rt, constant.negate());
    }
    @Override
    public ScalarAtom add(ScalarAtom other) {
        return switch (other) {
            case LargeInteger integer -> new RadicalTermSum(sum, constant.add(LargeFraction.valueOf(integer)));
            case LargeFraction largeFraction ->
                    new RadicalTermSum(sum, constant.add(largeFraction));
            case LargePowerMonomial largePowerMonomial -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                for (LargePowerMonomial m : this.sum.keys())
                    rt.add(new RadicalTerm(LargeFraction.ZERO, this.sum.get(m), m));
                rt.add(new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE, largePowerMonomial));
                yield new RadicalTermSum(rt, constant);
            }
            case RadicalTerm radicalTerm -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                for (LargePowerMonomial m : this.sum.keys())
                    rt.add(new RadicalTerm(LargeFraction.ZERO, this.sum.get(m), m));
                rt.add(radicalTerm);
                yield new RadicalTermSum(rt, constant);
            }
            case RadicalTermSum o -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                for (LargePowerMonomial m : this.sum.keys())
                    rt.add(new RadicalTerm(LargeFraction.ZERO, this.sum.get(m), m));
                for (LargePowerMonomial m : o.sum.keys())
                    rt.add(new RadicalTerm(LargeFraction.ZERO, o.sum.get(m), m));
                yield new RadicalTermSum(rt, constant.add(o.constant));
            }
            default -> throw new UnsupportedOperationException(
                    "Operation with these types together not supported yet");
        };
    }


    public RadicalTermSum multiply(RadicalTermSum o) {
        if (this.isZero() || o.isZero())
            return new RadicalTermSum();
        LargeList<RadicalTerm> products = new LargeList<>();
        // Nonconstant terms × other constant
        if (!o.constant.isZero())
            for (LargePowerMonomial a : this.sum.keys())
                products.add(new RadicalTerm(LargeFraction.ZERO, this.sum.get(a).multiply(o.constant), a));
        // This constant × other nonconstant terms
        if (!this.constant.isZero())
            for (LargePowerMonomial b : o.sum.keys())
                products.add(new RadicalTerm(LargeFraction.ZERO, this.constant.multiply(o.sum.get(b)), b));
        // Nonconstant terms × nonconstant terms
        for (LargePowerMonomial a : this.sum.keys()) {
            LargeFraction ca = this.sum.get(a);
            for (LargePowerMonomial b : o.sum.keys())
                products.add(new RadicalTerm(LargeFraction.ZERO, ca.multiply(o.sum.get(b)), a.multiply(b)));
        }
        // Normalize and combine everything once.
        return new RadicalTermSum(products, this.constant.multiply(o.constant));
    }

    @Override
    public ScalarAtom multiply(ScalarAtom other){
        return switch (other) {
            case LargeInteger integer -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                for (LargePowerMonomial m : this.sum.keys())
                    rt.add(new RadicalTerm(LargeFraction.ZERO, (LargeFraction) sum.get(m).multiply(integer), m));
                yield new RadicalTermSum(rt, (LargeFraction) constant.multiply(integer));
            }case LargeFraction largeFraction -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                for (LargePowerMonomial m : this.sum.keys())
                    rt.add(new RadicalTerm(LargeFraction.ZERO, sum.get(m).multiply(largeFraction), m));
                yield new RadicalTermSum(rt, constant.multiply(largeFraction));
            }case LargePowerMonomial largePowerMonomial -> {
                LargeList<RadicalTerm> rt = new LargeList<>();
                for (LargePowerMonomial m : this.sum.keys())
                    rt.add(new RadicalTerm(LargeFraction.ZERO, this.sum.get(m), (LargePowerMonomial) m.multiply(other)));
                rt.add(new RadicalTerm(LargeFraction.ZERO, this.constant, largePowerMonomial));
                yield new RadicalTermSum(rt, LargeFraction.ZERO);
            }case RadicalTerm o -> {
                RadicalTermSum R = (RadicalTermSum) this.multiply(o.rationalPart);
                RadicalTermSum S = (RadicalTermSum) this.multiply(o.exponent).multiply(o.coefficient);
                yield R.add(S);
            }case RadicalTermSum o -> multiply(o);
            default -> other.multiply(this);
        };
    }
    @Override
    public RadicalTermSum square() {return this.multiply(this);}
    @Override
    public RadicalTermSum cube() {return square().multiply(this);}
    public RadicalTermSum power(LargeInteger exponent){
        LargeInteger expTemp = exponent.abs();
        RadicalTermSum basTemp = this.clone();
        RadicalTermSum res = (RadicalTermSum) new RadicalTermSum().add(LargeFraction.ONE);
        while(!expTemp.isZero()){
            if(expTemp.isOdd())
                res = res.multiply(basTemp);
            if(!expTemp.isOne())
                basTemp = basTemp.square();
            expTemp = expTemp.shiftRight(1);
        }
        return res;
    }
    @Override
    public ScalarAtom power(LargeFraction f) {return new LargePowerNode(this, f, LargeFraction.ONE);}
    private static LargeFraction rationalRatio(LargePowerMonomial numerator, LargePowerMonomial denominator) {
        LargePowerMonomial ratio = numerator.multiply(denominator.reciprocal());
        if (!ratio.isRational())
            return null;
        if (ratio.exponent2.isOne())
            return ratio.base.negate();
        if (!ratio.exponent2.isZero())
            return null;
        return ratio.base;
    }
    @Override
    public ScalarAtom reciprocal() {
        if (isZero())
            throw new ArithmeticException("Cannot divide by 0");
        if (sum.isEmpty())
            return constant.reciprocal();
        // Easy monomial-only case: 1 / (c*m) = c^-1 * m^-1
        if (constant.isZero() && sum.size().isOne()) {
            LargePowerMonomial m = sum.keys().getFirst();
            LargeFraction c = sum.get(m);
            return new RadicalTerm(LargeFraction.ZERO, c, m).reciprocal();
        }
        LargeList<LargePowerMonomial> basis = new LargeList<>();
        basis.add(LargePowerMonomial.ONE);

        /*
         * Roots of unity basis:
         * If any term contains (-1)^(p/q), include the cyclotomic basis.
         */
        LargeList<LargeInteger> unityDenoms = new LargeList<>();

        for (LargePowerMonomial m : this.sum.keys())
            if (!m.exponent2.isZero() && !m.exponent2.isOne())
                unityDenoms.add(m.exponent2.denominator);
        if (unityDenoms.size().greaterThan(LargeInteger.ZERO)) {
            LargeInteger q = LargeInteger.ONE;
            for(LargeInteger I: unityDenoms) q = q.lcm(I);
            LargeInteger degree = q.multiply(2).totient();
            LargeList<LargePowerMonomial> next = new LargeList<>();
            for (LargeInteger i = LargeInteger.ZERO; i.lessThan(degree); i = i.add(1)) {
                LargePowerMonomial zetaPower = new LargePowerMonomial(LargeFraction.ONE, LargeFraction.ONE, new LargeFraction(i, q));
                if (!next.contains(zetaPower)) next.add(zetaPower);
            }
            basis = next;
        }
        /*
         * Radical generator basis:
         * For base^(a/d), include powers of base^(1/d).
         */
        LargeList<LargePowerMonomial> gens = new LargeList<>();
        for (LargePowerMonomial m : sum.keys()) {
            LargeInteger order = m.exponent.denominator;
            if (order.equals(LargeInteger.ONE)) continue;
            LargePowerMonomial g = new LargePowerMonomial(m.base, order.reciprocal());
            if (!gens.contains(g)) gens.add(g);
        }
        //extract the prime factors
        LargeList<LargePowerMonomial> newGens = new LargeList<>();
        for (LargePowerMonomial g : gens) {
            LargeMap<LargeInteger, LargeFraction> pr = g.factor();
            for (LargeInteger I : pr.keys()) {
                if (I.isNegative()) continue;
                if (pr.get(I).denominator.isOne()) continue; // rational contribution; not a radical generator
                LargePowerMonomial gen = new LargePowerMonomial(LargeFraction.valueOf(I), pr.get(I).denominator.reciprocal());
                if (!newGens.contains(gen)) newGens.add(gen);
            }
        }
        //merge same bases

        LargeMap<LargeFraction, LargeInteger> baseToDenom = new LargeMap<>();
        for (LargePowerMonomial g : newGens) {
            if (g.exponent.denominator.isOne()) continue;
            LargeInteger index = baseToDenom.indexOfKey(g.base);
            if (index.isNegative()) baseToDenom.put(g.base, g.exponent.denominator);
            else baseToDenom.set(g.base, baseToDenom.get(g.base).lcm(g.exponent.denominator));
        }
        LargeList<LargePowerMonomial> merged = new LargeList<>();
        for (LargeFraction base : baseToDenom.keys()) {
            LargeInteger denom = baseToDenom.get(base);
            merged.add(new LargePowerMonomial(base, denom.reciprocal()));
        }
        gens = merged;
        for (LargePowerMonomial g : gens) {
            LargeList<LargePowerMonomial> next = new LargeList<>();
            LargeInteger order = g.exponent.denominator;
            for (LargePowerMonomial oldBasis : basis)
                for (LargeInteger i = LargeInteger.ZERO; i.lessThan(order); i = i.add(1)) {
                    LargePowerMonomial product =
                            oldBasis.multiply(new LargePowerMonomial(g.base, new LargeFraction(i, order)));
                    if (!next.contains(product)) next.add(product);
                }
            LargeList<LargePowerMonomial> reordered = new LargeList<>();
            reordered.add(LargePowerMonomial.ONE);
            for (LargePowerMonomial b : next)
                if (!b.isOne()) reordered.add(b);
            basis = reordered;
        }
        /*
         * Solve:
         * this * (x0*b0 + x1*b1 + ... + xn*bn) = 1
         */
        LargeList<LargeList<LargeFraction>> lists = new LargeList<>();
        for (LargePowerMonomial b : basis) {
            RadicalTermSum product = (RadicalTermSum) this.multiply(b);
            LargeList<LargeFraction> colAsRow = new LargeList<>();
            for (LargeInteger i = LargeInteger.ZERO; i.lessThan(basis.size()); i = i.add(1))
                colAsRow.add(LargeFraction.ZERO);
            if (!product.constant.isZero()) {
                LargeFraction old = colAsRow.getFirst();
                colAsRow.set(LargeInteger.ZERO, old.add(product.constant));
            }

            for (LargePowerMonomial m : product.sum.keys()) {
                boolean found = false;
                for (LargeInteger i = LargeInteger.ZERO; i.lessThan(basis.size()); i = i.add(1)) {
                    LargeFraction ratio = rationalRatio(m, basis.get(i));
                    if (ratio == null) continue;
                    colAsRow.set(i, colAsRow.get(i).add(product.sum.get(m).multiply(ratio)));
                    found = true;
                    break;
                }
                if (!found)
                    throw new IllegalStateException("Product escaped reciprocal basis: " + m + "\nbasis = " + basis);
            }
            lists.add(colAsRow);
        }
        LargeList<LargeFraction> rhs = new LargeList<>();
        rhs.add(LargeFraction.ONE);
        for (LargeInteger i = LargeInteger.ONE; i.lessThan(basis.size()); i = i.add(1))
            rhs.add(LargeFraction.ZERO);
        lists.add(rhs);
        LargeMatrix M = new LargeMatrix(lists).transpose().reducedRowEchelonForm();
        LargeFraction cons = M.get(LargeInteger.ZERO, M.cols().subtract(1));
        LargeMap<LargePowerMonomial, LargeFraction> resultMap = new LargeMap<>();
        for (LargeInteger i = LargeInteger.ONE; i.lessThan(M.rows()); i = i.add(1)) {
            LargeFraction coeff = M.get(i, M.cols().subtract(1));
            if (!coeff.isZero())
                resultMap.put(basis.get(i), coeff);
        }
        return new RadicalTermSum(resultMap, cons);
    }
}
