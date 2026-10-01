package com.AtomCAS.utilities;
public class RadicalTermSum extends ScalarAtom{
    final LargeMap<LargePowerMonomial, LargeFraction> sum;
    final LargeFraction constant;
    public RadicalTermSum(){sum = new LargeMap<>(); constant = LargeFraction.ZERO;}
    private static void addCoefficient(LargeMap<LargePowerMonomial, LargeFraction> map, LargePowerMonomial key, LargeFraction value) {
        if (value.isZero()) return;
        if (map.containsKey(key)){
            LargeFraction updated = (LargeFraction) map.get(key).add(value);
            map.put(key, updated);
        } else
            map.put(key, value);
    }
    public RadicalTermSum(LargeList<RadicalTerm> r, LargeFraction cons){
        LargeMap<LargePowerMonomial, LargeFraction> rt = new LargeMap<>();
        LargeFraction CON = cons.clone();
        if(r.size().isZero()){
            constant = CON.clone();
            sum = new LargeMap<>();
            return;
        }
        for(RadicalTerm rx: r){
            addCoefficient(rt, rx.exponent, rx.coefficient);
            CON = (LargeFraction) CON.add(rx.rationalPart);
        }
        //handle roots of unity
        LargeInteger lc = LargeInteger.ONE;
        for (LargePowerMonomial L: rt.keys()) {
            if (L.isNotReal())
                lc = lc.lcm(L.exponent2.denominator);
        }
        LargeInteger lt = lc.multiply(2).totient();
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(rt.size()); i=i.add(1)) {
            LargePowerMonomial L = rt.keyAtIndex(i);
            if (L.isNotReal() && L.exponent2.multiply(lc).greaterThanOrEqualTo(lt)) {
                LargePolynomial P = LargePolynomial.cyclotomic(lc.multiply(2));
                P.coefficients.removeFirst();
                P = P.negate();
                ScalarAtom S = P.evaluateAt(new LargePowerMonomial(LargeFraction.NEGATIVE_ONE, lc.reciprocal()));
                S = S.multiply(new LargePowerMonomial(L.base, L.exponent, (LargeFraction) L.exponent2.multiply(lc).subtract(lt).divide(lc)));
                S = S.multiply(rt.get(L));
                rt.remove(L);
                i = i.subtract(1);
                if(S instanceof LargeInteger || S instanceof LargeFraction)
                    CON = (LargeFraction) CON.add(S);
                else if(S instanceof LargePowerMonomial)
                    rt.put((LargePowerMonomial) S, LargeFraction.ONE);
                else if(S instanceof RadicalTerm){
                    rt.put(((RadicalTerm) S).exponent, ((RadicalTerm) S).coefficient);
                    CON = (LargeFraction) CON.add(((RadicalTerm) S).rationalPart);
                }else if(S instanceof RadicalTermSum R){
                    for(LargePowerMonomial l: R.sum.keys())
                        addCoefficient(rt, l, R.sum.get(l));
                    CON = (LargeFraction) CON.add(R.constant);
                }
            }
        }
        //eliminate zeros
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(rt.size()); i = i.add(1)){
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
        if(sum.size().isZero()){
            this.constant = CON.clone();
            this.sum = new LargeMap<>();
            return;
        }
        for(LargePowerMonomial L: sum.keys()){
            if(L.isRational()){
                LargeFraction bas = (LargeFraction) L.base.multiply(sum.get(L));
                CON = (LargeFraction) CON.add(bas);
            }else
                rt.put(L, sum.get(L));
        }
        //handle roots of unity
        LargeInteger lc = LargeInteger.ONE;
        for (LargePowerMonomial L: rt.keys()) {
            if (L.isNotReal())
                lc = lc.lcm(L.exponent2.denominator);
        }
        LargeInteger lt = lc.multiply(2).totient();
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(rt.size()); i=i.add(1)) {
            LargePowerMonomial L = rt.keyAtIndex(i);
            if (L.isNotReal() && L.exponent2.multiply(lc).greaterThanOrEqualTo(lt)) {
                LargePolynomial P = LargePolynomial.cyclotomic(lc.multiply(2));
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
                    CON = (LargeFraction) CON.add(((RadicalTerm) S).rationalPart);
                }else if(S instanceof RadicalTermSum R){
                    for(LargePowerMonomial l: R.sum.keys())
                        addCoefficient(rt, l, R.sum.get(l));
                    CON = (LargeFraction) CON.add(R.constant);
                }
            }
        }
        //eliminate zeros
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
        if(constant.doesntEqual(LargeInteger.ZERO))
            str.append(constant);
        if(sum.size().isZero() && constant.isZero()){
            str.append("0");
            return str.toString();
        }
        for (LargePowerMonomial L : sum.keys()){
            if(constant.doesntEqual(LargeInteger.ZERO) || sum.indexOfKey(L).doesntEqual(LargeInteger.ZERO)){
                if (sum.get(L).isNegative())
                    str.append(" - ");
                else if (sum.get(L).greaterThan(LargeInteger.ZERO))
                    str.append(" + ");
            }else{
                if (sum.get(L).isNegative())
                    str.append("-");
            }
            if (sum.get(L).doesntEqual(LargeInteger.ONE) && sum.get(L).doesntEqual(LargeInteger.NEGATIVE_ONE))
                str.append(sum.get(L).abs()).append("*");
            str.append(L);
        }
        return str.toString();
    }

    @Override
    public String toLatex() {return "";}
    @Override
    public RadicalTermSum clone() {return new RadicalTermSum(sum, constant);}
    @Override
    public boolean equals(Atom other) {
        if(!(other instanceof ScalarAtom)) return false;
        return switch (other) {
            case LargeFraction largeFraction -> constant.equals(largeFraction)
                    && sum.isEmpty();
            case LargeInteger integer -> constant.equals(integer) && sum.size().isZero();
            case LargePowerMonomial L -> {
                if (sum.size().greaterThan(LargeInteger.ONE)) yield false;
                if (L.isRational()) {
                    boolean B = constant.abs().equals(L.base) && sum.size().isZero();
                    if (!B) yield false;
                    if (L.exponent2.equals(LargeInteger.ONE)) yield constant.isNegative();
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
            }
            default -> throw new UnsupportedOperationException(
                    "Operation with these types together not supported yet");
        };
    }
    @Override
    public boolean isZero() {return constant.isZero() && sum.isEmpty();}
    public boolean isReal() {
        for (LargePowerMonomial L: sum.keys())
            if (L.isNotReal()) return false;
        return true;
    }
    public boolean isRational(){return sum.isEmpty();}
    public boolean isAlgebraic(){return true;}
    @Override
    public ScalarAtom realPart(){return this.sign().realPart().multiply(this.abs());}
    @Override
    public ScalarAtom imagPart(){return this.sign().imagPart().multiply(this.abs());}
    @Override
    public LargeInteger floor() {
        if(this.isNotReal()) throw new IllegalStateException("Cannot floor or apply ceiling a non-real atom");
        else if(this.isRational()) return constant.floor();
        LargeList<LargeFraction> floors = new LargeList<>();
        LargeList<LargeFraction> ceilings = new LargeList<>();
        for(LargePowerMonomial r: this.sum.keys()){
            LargeFraction F = new LargeFraction(r.multiply(sum.get(r)).floor());
            floors.add(F);
            if(r.isNotInteger()) F = (LargeFraction) F.add(LargeFraction.ONE);
            ceilings.add(F);
        }
        LargeInteger result;
        while(true){
            LargeFraction floorSum = LargeFraction.ZERO;
            LargeFraction ceilSum = LargeFraction.ZERO;
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(floors.size()); i = i.add(1)){
                LargeFraction mediant = new LargeFraction(floors.get(i).numerator.add(ceilings.get(i).numerator),
                        floors.get(i).denominator.add(ceilings.get(i).denominator));
                if(mediant.lessThan(sum.keyAtIndex(i).multiply(sum.get(sum.keyAtIndex(i)))))
                    floors.set(i, mediant);
                else ceilings.set(i, mediant);
                floorSum = (LargeFraction) floorSum.add(floors.get(i));
                ceilSum = (LargeFraction) ceilSum.add(ceilings.get(i));
            }
            floorSum = (LargeFraction) floorSum.add(constant);
            ceilSum = (LargeFraction) ceilSum.add(constant);
            if(ceilSum.ceil().subtract(LargeInteger.ONE).equals(floorSum.floor()) || ceilSum.equals(floorSum)){
                result = floorSum.floor();
                break;
            }
        }
        return result;
    }
    @Override
    public LargeInteger ceil() {
        if(sum.isEmpty()) return constant.ceil();
        return this.floor().add(1);
    }
    public RadicalTermSum complexConjugate() {
        LargeList<RadicalTerm> rt = new LargeList<>();
        for(LargePowerMonomial L: sum.keys())
            rt.add(new RadicalTerm(LargeFraction.ZERO, sum.get(L), L.complexConjugate()));
        return new RadicalTermSum(rt, this.constant);
    }
    @Override
    public ScalarAtom abs() {
        if (this.isNotReal()){
            RadicalTermSum R = (RadicalTermSum) this.multiply(this.complexConjugate());
            if(R.isRational()) return new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE,
                    new LargePowerMonomial(R.constant, LargeFraction.HALF));
            return new LargePowerNode(R, LargeFraction.HALF, LargeFraction.ONE);
        }
        if (this.isZero())
            return LargeInteger.ZERO;
        LargeInteger f = this.floor();
        LargeInteger c = f.add(1);
        if (f.greaterThanOrEqualTo(LargeInteger.ZERO) && c.greaterThanOrEqualTo(LargeInteger.ONE))
            return this;
        return this.negate();
    }
    @Override
    public int compareTo(ScalarAtom other){
        if(this.isNotReal() || other.isNotReal()) throw new IllegalStateException(
                "Complex numbers cannot be compared");
        ScalarAtom s = subtract(other).sign();
        if(s.equals(LargeInteger.ONE)) return 1;
        if(s.equals(LargeInteger.NEGATIVE_ONE)) return -1;
        return 0;
    }
    @Override
    public RadicalTermSum negate() {
        LargeMap<LargePowerMonomial, LargeFraction> rt = new LargeMap<>();
        for(LargePowerMonomial m : this.sum.keys())
            rt.put(m, sum.get(m).negate());
        return new RadicalTermSum(rt, constant.negate());
    }
    @Override
    public ScalarAtom add(ScalarAtom other) {
        return switch (other) {
            case LargeInteger integer -> new RadicalTermSum(this.sum, (LargeFraction) this.constant.add(integer));
            case LargeFraction largeFraction ->
                    new RadicalTermSum(this.sum, (LargeFraction) this.constant.add(largeFraction));
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
                yield new RadicalTermSum(rt, (LargeFraction) constant.add(o.constant));
            }
            default -> throw new UnsupportedOperationException(
                    "Operation with these types together not supported yet");
        };
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
                    rt.add(new RadicalTerm(LargeFraction.ZERO, (LargeFraction) sum.get(m).multiply(largeFraction),
                            m));
                yield new RadicalTermSum(rt, (LargeFraction) constant.multiply(largeFraction));
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
            }case RadicalTermSum o -> {
                RadicalTermSum rt = new RadicalTermSum();
                rt = (RadicalTermSum) rt.add(this.multiply(o.constant));
                for (LargePowerMonomial m : o.sum.keys())
                    rt = (RadicalTermSum) rt.add(this.multiply(o.sum.get(m)).multiply(m));
                yield rt;
            }default -> other.multiply(this);
        };
    }
    public ScalarAtom multiply(ScalarAtom... other){
        ScalarAtom result = this;
        for(ScalarAtom s: other) result = result.multiply(s);
        return result;
    }
    @Override
    public RadicalTermSum square() {return (RadicalTermSum) this.multiply(this);}
    @Override
    public RadicalTermSum cube() {return (RadicalTermSum) square().multiply(this);}
    public RadicalTermSum power(LargeInteger exponent){
        LargeInteger expTemp = exponent.abs();
        RadicalTermSum basTemp = this.clone();
        RadicalTermSum res = (RadicalTermSum) new RadicalTermSum().add(LargeFraction.ONE);
        while(true){
            if(expTemp.isOdd()){
                res = (RadicalTermSum) res.multiply(basTemp);
                if(expTemp.equals(LargeInteger.ONE)) break;
            }
            basTemp = basTemp.square();
            expTemp = expTemp.shiftRight(1);
        }
        return res;
    }
    @Override
    public ScalarAtom power(LargeFraction f) {return new LargePowerNode(this, f, LargeFraction.ONE);}
    @Override
    public LargeDecimal approximateToDecimal(LargeInteger precision) {return null;}
    private static void addBasisIfMissing(LargeList<LargePowerMonomial> basis, LargePowerMonomial x){
        for (LargePowerMonomial b : basis)
            if (b.equals(x)) return;
        basis.add(x);
    }
    private static void addGenIfMissing(LargeList<LargePowerMonomial> gens, LargePowerMonomial candidate) {
        for (LargePowerMonomial g : gens)
            if(g.equals(candidate)) return;
        gens.add(candidate);
    }
    private static LargeInteger indexOfBasis(LargeList<LargePowerMonomial> basis, LargePowerMonomial m) {
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(basis.size()); i = i.add(1)) {
            if (basis.get(i).equals(m))
                return i;
        }
        return LargeInteger.NEGATIVE_ONE;
    }
    @Override
    public ScalarAtom reciprocal(){
        if (this.isZero())
            throw new ArithmeticException("Cannot divide by 0");

        if (sum.size().isZero())
            return constant.reciprocal();

        // Easy monomial-only case: 1 / (c*m) = c^-1 * m^-1
        if (sum.size().equals(LargeInteger.ONE)) {
            LargePowerMonomial m = sum.keyAtIndex(LargeInteger.ZERO);
            LargeFraction c = sum.get(m);
            return new RadicalTerm(constant, c, m).reciprocal();
        }

        LargeList<LargePowerMonomial> basis = new LargeList<>();
        basis.add(LargePowerMonomial.ONE);

        /*
         * Roots of unity basis:
         * If any term contains (-1)^(p/q), include the cyclotomic basis.
         */
        LargeList<LargeInteger> unityDenominators = new LargeList<>();

        for (LargePowerMonomial m : this.sum.keys())
            if (m.exponent2.doesntEqual(LargeInteger.ZERO) && m.exponent2.doesntEqual(LargeInteger.ONE))
                unityDenominators.add(m.exponent2.denominator);
        if (unityDenominators.size().greaterThan(LargeInteger.ZERO)) {
            LargeInteger q = LargeInteger.ONE;
            for(LargeInteger I: unityDenominators) q = q.lcm(I);
            LargeInteger degree = q.multiply(2).totient();
            LargeList<LargePowerMonomial> next = new LargeList<>();
            for (LargeInteger i = LargeInteger.ZERO; i.lessThan(degree); i = i.add(1)) {
                LargePowerMonomial zetaPower = new LargePowerMonomial(LargeFraction.ONE, LargeFraction.ONE,
                        new LargeFraction(i, q));
                addBasisIfMissing(next, zetaPower);
            }
            basis = next;
        }
        /*
         * Radical generator basis:
         * For base^(a/d), include powers of base^(1/d).
         */
        LargeList<LargePowerMonomial> gens = new LargeList<>();
        for (LargePowerMonomial m : this.sum.keys()) {
            LargeInteger order = m.exponent.denominator;
            if (order.isOne()) continue;
            LargePowerMonomial g = new LargePowerMonomial(m.base, order.reciprocal());
            addBasisIfMissing(gens, g);
        }
        //extract the prime factors
        LargeList<LargePowerMonomial> newGens = new LargeList<>();
        for (LargePowerMonomial g : gens) {
            LargeMap<LargeInteger, LargeFraction> pr = g.factor();
            for (LargeInteger I : pr.keys()) {
                if (I.equals(LargeInteger.NEGATIVE_ONE))
                    continue;
                if (pr.get(I).isOne())
                    continue; // rational contribution; not a radical generator
                LargePowerMonomial gen = new LargePowerMonomial(new LargeFraction(I),
                        pr.get(I).denominator.reciprocal());
                addGenIfMissing(newGens, gen);
            }
        }
        //merge same bases
        LargeMap<LargeFraction, LargeInteger> baseToDenominator = new LargeMap<>();
        for (LargePowerMonomial g : newGens) {
            if (g.exponent.denominator.isOne()) continue;
            if (baseToDenominator.containsKey(g.base)) {
                LargeInteger oldDenominator = baseToDenominator.get(g.base);
                baseToDenominator.put(g.base, oldDenominator.lcm(g.exponent.denominator));
            } else baseToDenominator.put(g.base, g.exponent.denominator);
        }
        LargeList<LargePowerMonomial> merged = new LargeList<>();
        for (LargeFraction base : baseToDenominator.keys()) {
            LargeInteger denominator = baseToDenominator.get(base);
            merged.add(new LargePowerMonomial(base, denominator.reciprocal()));
        }
        gens = merged;
        for (LargePowerMonomial g : gens) {
            LargeList<LargePowerMonomial> next = new LargeList<>();
            LargeInteger order = g.exponent.denominator;
            for (LargePowerMonomial oldBasis : basis) {
                for (LargeInteger i = LargeInteger.ZERO; i.lessThan(order); i = i.add(1)) {
                    LargePowerMonomial powerOfG = new LargePowerMonomial(g.base, new LargeFraction(i, order));
                    LargePowerMonomial product = (LargePowerMonomial) oldBasis.multiply(powerOfG);
                    addBasisIfMissing(next, product);
                }
            }
            LargeList<LargePowerMonomial> reordered = new LargeList<>();
            reordered.add(LargePowerMonomial.ONE);
            for (LargePowerMonomial b : next) {
                if (b.doesntEqual(LargePowerMonomial.ONE))
                    reordered.add(b);
            }
            basis = reordered;
        }
        /*
         * Solve:
         * this * (x0*b0 + x1*b1 + ... + xn*bn) = 1
         */
        LargeMatrix M = new LargeMatrix();
        for (LargePowerMonomial b : basis) {
            RadicalTermSum product = (RadicalTermSum) this.multiply(b);
            LargeVector colAsRow = new LargeVector(basis.size());
            if (product.constant.doesntEqual(LargeInteger.ZERO)) {
                LargeFraction old = colAsRow.get(LargeInteger.ZERO);
                colAsRow.set(LargeInteger.ZERO, (LargeFraction) old.add(product.constant));
            }
            for (LargePowerMonomial m : product.sum.keys()) {
                LargeInteger index = indexOfBasis(basis, m);
                if (index.equals(LargeInteger.NEGATIVE_ONE))
                    throw new IllegalStateException("Product escaped reciprocal basis: " + m + "\nbasis = " + basis);
                LargeFraction old = colAsRow.get(index);
                colAsRow.set(index, (LargeFraction) old.add(product.sum.get(m)));
            }
            M.appendRow(colAsRow);
        }
        LargeVector rhs = new LargeVector(basis.size());
        rhs.set(LargeInteger.ZERO, LargeFraction.ONE);
        M.appendRow(rhs);
        M = M.transpose().reducedRowEchelonForm();
        LargeFraction cons = M.get(LargeInteger.ZERO, M.cols().subtract(1));
        LargeMap<LargePowerMonomial, LargeFraction> resultMap = new LargeMap<>();
        for (LargeInteger i = LargeInteger.ONE; i.lessThan(M.rows()); i = i.add(1)) {
            LargeFraction coefficient = M.get(i, M.cols().subtract(1));
            if (coefficient.doesntEqual(LargeInteger.ZERO))
                resultMap.put(basis.get(i), coefficient);
        }
        RadicalTermSum result = new RadicalTermSum(resultMap, cons);
        if (result.multiply(this).doesntEqual(LargeInteger.ONE))
            throw new AssertionError("Could not find the proper reciprocal. The used basis was " + basis + " and has size " + basis.size());
        return result;
    }
}
