package com.AtomCAS.utilities;
public class RadicalTermSum extends ScalarAtom{
    public final LargeMap<LargePowerMonomial, LargeFraction> sum;
    public final LargeFraction constant;
    public RadicalTermSum(){sum = new LargeMap<>(); constant = LargeFraction.zero;}
    private static void addCoeff(LargeMap<LargePowerMonomial, LargeFraction> map, LargePowerMonomial key, LargeFraction value) {
        if (value.equalsTo(LargeInteger.zero)) return;
        if (map.containsKey(key)) {
            LargeFraction updated = (LargeFraction) map.get(key).add(value);
            if (updated.equalsTo(LargeInteger.zero))
                map.remove(key);
            else
                map.put(key, updated);
        } else
            map.put(key, value);
    }
    public RadicalTermSum(LargeList<RadicalTerm> r, LargeFraction cons){
        LargeMap<LargePowerMonomial, LargeFraction> rt = new LargeMap<>();
        LargeFraction CON = cons.copy();
        if(r.size().equalsTo(LargeInteger.zero)){
            constant = CON.copy();
            sum = new LargeMap<>();
            return;
        }
        for(RadicalTerm rx: r){
            addCoeff(rt, rx.exponent, rx.coefficient);
            CON = (LargeFraction) CON.add(rx.rationalPart);
        }
        //handle roots of unity
        LargeInteger lc = LargeInteger.one;
        for (LargePowerMonomial L: rt) {
            if (L.isNotReal())
                lc = lc.lcm(L.exponent2.denominator);
        }
        LargeInteger lt = lc.multiply(2).totient();
        for (LargeInteger i = LargeInteger.zero; i.lessThan(rt.size()); i=i.add(1)) {
            LargePowerMonomial L = rt.keyAtIndex(i);
            if (L.isNotReal() && L.exponent2.multiply(lc).greaterThanOrEqualTo(lt)) {
                LargePolynomial P = LargePolynomial.cyclotomic(lc.multiply(2));
                P.coefficients.removeFirst();
                P = P.multiply(LargeFraction.negativeOne);
                RadicalTermSum S = (RadicalTermSum) P.plugIn(new LargePowerMonomial(LargeFraction.negativeOne, lc.reciprocal()));
                for (LargeInteger j = LargeInteger.zero; j.lessThan(L.exponent2.multiply(lc).subtract(lt)); j = j.add(1))
                    S = (RadicalTermSum) S.multiply(new LargePowerMonomial(LargeFraction.negativeOne, lc.reciprocal()));
                S = (RadicalTermSum) S.multiply(new LargePowerMonomial(L.base, L.exponent));
                S = (RadicalTermSum) S.multiply(rt.get(L));
                rt.remove(L);
                i = i.subtract(1);
                for(LargePowerMonomial l: S.sum)
                    addCoeff(rt, l, S.sum.get(l));
                CON = (LargeFraction) CON.add(S.constant);
            }
        }
        //eliminate zeros
        for(LargeInteger i = LargeInteger.zero; i.lessThan(rt.size()); i = i.add(1)){
            if(rt.get(rt.keyAtIndex(i)).equalsTo(LargeInteger.zero)){
                rt.remove(rt.keyAtIndex(i));
                i = i.subtract(1);
            }
        }
        //sort radicands
        for(LargeInteger i = LargeInteger.zero; i.lessThan(rt.size().subtract(LargeInteger.one)); i = i.add(1)){
            for(LargeInteger j = i.add(1); j.lessThan(rt.size()); j = j.add(1)){
                if(rt.keyAtIndex(j).isReal() && rt.keyAtIndex(i).isReal() && rt.keyAtIndex(j).exponent.lessThan(rt.keyAtIndex(i).exponent))
                    rt.swap(rt.keyAtIndex(i), rt.keyAtIndex(j));
            }
        }
        sum = rt;
        this.constant = CON;
    }
    public RadicalTermSum(LargeMap<LargePowerMonomial, LargeFraction> sum, LargeFraction constant){
        LargeMap<LargePowerMonomial, LargeFraction> rt = new LargeMap<>();
        LargeFraction CON = constant.copy();
        if(sum.size().equalsTo(LargeInteger.zero)){
            this.constant = CON.copy();
            this.sum = new LargeMap<>();
            return;
        }
        for(LargePowerMonomial L: sum)
            rt.put(L, sum.get(L));
        //handle roots of unity
        LargeInteger lc = LargeInteger.one;
        for (LargePowerMonomial L: rt) {
            if (L.isNotReal())
                lc = lc.lcm(L.exponent2.denominator);
        }
        LargeInteger lt = lc.multiply(2).totient();
        for (LargeInteger i = LargeInteger.zero; i.lessThan(rt.size()); i=i.add(1)) {
            LargePowerMonomial L = rt.keyAtIndex(i);
            if (L.isNotReal() && L.exponent2.multiply(lc).greaterThanOrEqualTo(lt)) {
                LargePolynomial P = LargePolynomial.cyclotomic(lc.multiply(2));
                P.coefficients.removeFirst();
                P = P.multiply(LargeFraction.negativeOne);
                RadicalTermSum S = (RadicalTermSum) P.plugIn(new LargePowerMonomial(LargeFraction.negativeOne, lc.reciprocal()));
                for (LargeInteger j = LargeInteger.zero; j.lessThan(L.exponent2.multiply(lc).subtract(lt)); j = j.add(1))
                    S = (RadicalTermSum) S.multiply(new LargePowerMonomial(LargeFraction.negativeOne, lc.reciprocal()));
                S = (RadicalTermSum) S.multiply(new LargePowerMonomial(L.base, L.exponent));
                S = (RadicalTermSum) S.multiply(rt.get(L));
                rt.remove(L);
                i = i.subtract(1);
                for(LargePowerMonomial l: S.sum)
                    addCoeff(rt, l, S.sum.get(l));
                CON = (LargeFraction) CON.add(S.constant);
            }
        }
        //eliminate zeros
        for(LargeInteger i = LargeInteger.zero; i.lessThan(rt.size()); i = i.add(1)){
            if(rt.get(rt.keyAtIndex(i)).equalsTo(LargeInteger.zero)){
                rt.remove(rt.keyAtIndex(i));
                i = i.subtract(1);
            }
        }
        //sort radicands
        for(LargeInteger i = LargeInteger.zero; i.lessThan(rt.size().subtract(LargeInteger.one)); i = i.add(1)){
            for(LargeInteger j = i.add(1); j.lessThan(rt.size()); j = j.add(1)){
                if(rt.keyAtIndex(j).compareMags(rt.keyAtIndex(j)) < 0)
                    rt.swap(rt.keyAtIndex(i), rt.keyAtIndex(j));
            }
        }
        this.sum = rt;
        this.constant = CON;
    }
    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        if(constant.notEqualsTo(LargeInteger.zero))
            str.append(constant);
        if(sum.size().equalsTo(LargeInteger.zero) && constant.equalsTo(LargeInteger.zero)){
            str.append("0");
            return str.toString();
        }
        for (LargePowerMonomial L : sum){
            if(constant.notEqualsTo(LargeInteger.zero) || sum.indexOfKey(L).notEqualsTo(LargeInteger.zero)){
                if (sum.get(L).lessThan(LargeInteger.zero))
                    str.append(" - ");
                else if (sum.get(L).greaterThan(LargeInteger.zero))
                    str.append(" + ");
                if (sum.get(L).notEqualsTo(LargeInteger.one) && sum.get(L).notEqualsTo(LargeInteger.negativeOne))
                    str.append(sum.get(L).magnitude()).append("*");
            }else{
                if (sum.get(L).lessThan(LargeInteger.zero))
                    str.append("-");
                if (sum.get(L).notEqualsTo(LargeInteger.one) && sum.get(L).notEqualsTo(LargeInteger.negativeOne))
                    str.append(sum.get(L).magnitude()).append("*");
            }
            str.append(L);
        }
        return str.toString();
    }
    @Override
    public RadicalTermSum copy() {return new RadicalTermSum(sum, constant);}
    @Override
    public boolean equalsTo(Atom other) {
        if(!(other instanceof ScalarAtom)) return false;
        else if(other instanceof LargeFraction || other instanceof LargeInteger) return constant.equalsTo(other) && sum.size().equalsTo(LargeInteger.zero);
        else if(other instanceof LargePowerMonomial L){
            if(sum.size().greaterThan(LargeInteger.one)) return false;
            if(L.isRational()){
                boolean B = constant.magnitude().equalsTo(L.base) && sum.size().equalsTo(LargeInteger.zero);
                if(!B) return false;
                if(L.exponent2.equalsTo(LargeInteger.one)) return constant.sign().equalsTo(LargeInteger.negativeOne);
                return true;
            }
            return sum.keyAtIndex(LargeInteger.zero).multiply(sum.get(sum.keyAtIndex(LargeInteger.zero))).equalsTo(L) && constant.equalsTo(LargeInteger.zero);
        }else if(other instanceof RadicalTerm)
            return sum.size().equalsTo(LargeInteger.one) && constant.add(sum.keyAtIndex(LargeInteger.zero).multiply(sum.get(sum.keyAtIndex(LargeInteger.zero)))).equalsTo(other);
        else if(other instanceof RadicalTermSum R){
            if (this.constant.notEqualsTo(R.constant))
                return false;
            if (this.sum.size() != R.sum.size())
                return false;
            for (LargeInteger i = LargeInteger.zero; i.lessThan(sum.size()); i = i.add(1))
                if(sum.keyAtIndex(i).notEqualsTo(R.sum.keyAtIndex(i)) || sum.get(sum.keyAtIndex(i)).notEqualsTo(R.sum.get(sum.keyAtIndex(i))))
                    return false;
            return true;
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public boolean notEqualsTo(Atom other) {return !equalsTo(other);}
    public boolean isInteger(){return sum.size().equalsTo(LargeInteger.zero) && constant.denominator.equalsTo(LargeInteger.one);}
    public boolean isNotInteger(){return !isInteger();}
    public boolean isReal(){
        for (LargePowerMonomial L: sum) if (L.isNotReal()) return false;
        return true;
    }
    public boolean isNotReal(){return !isReal();}
    public boolean isRational(){return sum.size().equalsTo(LargeInteger.zero);}
    @Override
    public LargeInteger floor() {
        if(this.isNotReal()) throw new IllegalStateException("Error: Cannot floor or apply ceiling a non-real atom");
        else if(this.isRational()) return constant.floor();
        LargeList<LargeFraction> floors = new LargeList<>();
        LargeList<LargeFraction> ceilings = new LargeList<>();
        for(LargePowerMonomial r: this.sum){
            LargeFraction F = (LargeFraction) r.multiply(sum.get(r)).floor().divide(LargeInteger.one);
            floors.add(F);
            if(r.isNotInteger()) F = (LargeFraction) F.add(LargeInteger.one);
            ceilings.add(F);
        }
        LargeInteger result;
        while(true){
            LargeFraction floorSum = LargeFraction.zero;
            LargeFraction ceilSum = LargeFraction.zero;
            for(LargeInteger i = LargeInteger.zero; i.lessThan(floors.size()); i = i.add(1)){
                LargeFraction mediant = new LargeFraction((LargeInteger) floors.get(i).numerator.add(ceilings.get(i).numerator), (LargeInteger) floors.get(i).denominator.add(ceilings.get(i).denominator));
                if(mediant.compareTo(sum.keyAtIndex(i).multiply(sum.get(sum.keyAtIndex(i)))) == -1) floors.set(i, mediant);
                else ceilings.set(i, mediant);
                floorSum = (LargeFraction) floorSum.add(floors.get(i));
                ceilSum = (LargeFraction) ceilSum.add(ceilings.get(i));
            }
            floorSum = (LargeFraction) floorSum.add(constant);
            ceilSum = (LargeFraction) ceilSum.add(constant);
            if(ceilSum.ceil().subtract(LargeInteger.one).equalsTo(floorSum.floor()) || ceilSum.equalsTo(floorSum)){
                result = floorSum.floor();
                break;
            }
        }
        return result;
    }
    @Override
    public LargeInteger ceil() {
        LargeInteger I = this.floor();
        if(this.isNotInteger()) I = (LargeInteger) I.add(LargeInteger.one);
        return I;
    }
    public RadicalTermSum conjugate() {
        LargeMap<LargePowerMonomial, LargeFraction> rt = new LargeMap<>();
        for (LargePowerMonomial r : this.sum){
            if(r.isNotReal()) rt.put(r, sum.get(r).negate());
            else rt.put(r, sum.get(r));
        }
        return new RadicalTermSum(rt, this.constant);
    }
    @Override
    public ScalarAtom sign() {
        if(this.equalsTo(LargeInteger.zero)) return LargeInteger.zero;
        return this.divide(this.magnitude());
    }
    @Override
    public ScalarAtom magnitude() {
        if (this.isNotReal()){
            RadicalTermSum R = (RadicalTermSum) this.multiply(this.conjugate());
            if(R.isRational()) return new RadicalTerm(LargeFraction.zero, LargeFraction.one, new LargePowerMonomial(R.constant, LargeFraction.half));
            return new LargePowerNode(R, LargeFraction.half, LargeFraction.zero);
        }
        if (this.equalsTo(LargeInteger.zero))
            return LargeInteger.zero;
        LargeInteger f = this.floor();
        LargeInteger c = f.add(1);
        if (f.greaterThanOrEqualTo(LargeInteger.zero) && c.greaterThanOrEqualTo(LargeInteger.one))
            return this;
        return this.negate();
    }
    @Override
    public long compareMags(ScalarAtom other){return this.magnitude().compareTo(other.magnitude());}
    @Override
    public long compareTo(ScalarAtom other){
        if(this.isNotReal() || other.isNotReal()) throw new IllegalStateException("Error: Complex numbers cannot be compared");
        ScalarAtom s = this.subtract(other).sign();
        if(s.equalsTo(LargeInteger.one)) return 1L;
        if(s.equalsTo(LargeInteger.negativeOne)) return -1L;
        return 0L;
    }
    public boolean greaterThan(ScalarAtom other) {return this.compareTo(other) > 0;}
    public boolean lessThan(ScalarAtom other) {return this.compareTo(other) < 0;}
    public boolean greaterThanOrEqualTo(ScalarAtom other) {return this.compareTo(other) >= 0;}
    public boolean lesserThanOrEqualTo(ScalarAtom other) {return this.compareTo(other) <= 0;}
    @Override
    public RadicalTermSum negate() {
        LargeMap<LargePowerMonomial, LargeFraction> rt = new LargeMap<>();
        for(LargePowerMonomial m : this.sum)
            rt.put(m, sum.get(m).negate());
        return new RadicalTermSum(rt, constant.negate());
    }
    @Override
    public ScalarAtom add(ScalarAtom other) {
        if (other instanceof LargeInteger || other instanceof LargeFraction)
            return new RadicalTermSum(this.sum, (LargeFraction) this.constant.add(other));
        else if (other instanceof LargePowerMonomial){
            LargeList<RadicalTerm> rt = new LargeList<>();
            for(LargePowerMonomial m : this.sum)
                rt.add(new RadicalTerm(LargeFraction.zero, this.sum.get(m), m));
            rt.add(new RadicalTerm(LargeFraction.zero, LargeFraction.one, (LargePowerMonomial) other));
            return new RadicalTermSum(rt, constant);
        }else if (other instanceof RadicalTerm){
            LargeList<RadicalTerm> rt = new LargeList<>();
            for(LargePowerMonomial m : this.sum)
                rt.add(new RadicalTerm(LargeFraction.zero, this.sum.get(m), m));
            rt.add((RadicalTerm) other);
            return new RadicalTermSum(rt, constant);
        }else if (other instanceof RadicalTermSum o){
            LargeList<RadicalTerm> rt = new LargeList<>();
            for(LargePowerMonomial m : this.sum)
                rt.add(new RadicalTerm(LargeFraction.zero, this.sum.get(m), m));
            for(LargePowerMonomial m : o.sum)
                rt.add(new RadicalTerm(LargeFraction.zero, o.sum.get(m), m));
            return new RadicalTermSum(rt, (LargeFraction) constant.add(o.constant));
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public ScalarAtom subtract(ScalarAtom other){return this.add(other.negate());}
    @Override
    public ScalarAtom multiply(ScalarAtom other){
        if(other instanceof LargeInteger || other instanceof LargeFraction){
            LargeMap<LargePowerMonomial, LargeFraction> rt = new LargeMap<>();
            for(LargePowerMonomial m : this.sum)
                rt.put(m, (LargeFraction) sum.get(m).multiply(other));
            return new RadicalTermSum(rt, (LargeFraction) constant.multiply(other));
        }else if(other instanceof LargePowerMonomial){
            LargeList<RadicalTerm> rt = new LargeList<>();
            for(LargePowerMonomial m : this.sum)
                rt.add(new RadicalTerm(LargeFraction.zero, this.sum.get(m), (LargePowerMonomial) m.multiply(other)));
            rt.add(new RadicalTerm(LargeFraction.zero, this.constant, (LargePowerMonomial) other));
            return new RadicalTermSum(rt, LargeFraction.zero);
        }else if(other instanceof RadicalTerm o){
            RadicalTermSum R = (RadicalTermSum) this.multiply(o.rationalPart);
            RadicalTermSum S = (RadicalTermSum) this.multiply(o.exponent).multiply(o.coefficient);
            return R.add(S);
        }
        else if(other instanceof RadicalTermSum o){
            RadicalTermSum rt = new RadicalTermSum();
            rt = (RadicalTermSum) rt.add(this.multiply(o.constant));
            for(LargePowerMonomial m : o.sum)
                rt = (RadicalTermSum) rt.add(this.multiply(o.sum.get(m)).multiply(m));
            return rt;
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    public ScalarAtom multiply(LargeList<ScalarAtom> other){
        ScalarAtom result = this;
        for(ScalarAtom s: other) result = result.multiply(s);
        return result;
    }
    @Override
    public ScalarAtom square() {return this.multiply(this);}
    @Override
    public ScalarAtom cube() {return this.square().multiply(this);}
    @Override
    public ScalarAtom percent() {return this.multiply(Parser.parse("0.01"));}
    @Override
    public ScalarAtom permille() {return this.multiply(Parser.parse("0.001"));}
    @Override
    public ScalarAtom power(LargeFraction f) {return new LargePowerNode(this, f, LargeFraction.one);}
    private static RadicalTermSum asRadicalTermSum(ScalarAtom x) {
        if (x instanceof RadicalTermSum) return (RadicalTermSum) x;
        if (x instanceof RadicalTerm){
            LargeList<RadicalTerm> list = new LargeList<>();
            list.add((RadicalTerm) x);
            return new RadicalTermSum(list, LargeFraction.zero);
        }
        if (x instanceof LargeInteger || x instanceof LargeFraction)
            return new RadicalTermSum(new LargeList<>(), (LargeFraction) x.divide(LargeInteger.one));
        if (x instanceof LargePowerMonomial) {
            LargeList<RadicalTerm> list = new LargeList<>();
            list.add(new RadicalTerm(LargeFraction.zero, LargeFraction.one, (LargePowerMonomial) x));
            return new RadicalTermSum(list, LargeFraction.zero);
        }
        throw new UnsupportedOperationException("Error: Cannot convert the input to RadicalTermSum");
    }
    private static void addBasisIfMissing(LargeList<LargePowerMonomial> basis, LargePowerMonomial x){
        for (LargePowerMonomial b : basis)
            if (b.equalsTo(x)) return;
        basis.add(x);
    }
    private static void addGenIfMissing(LargeList<LargePowerMonomial> gens, LargePowerMonomial candidate) {
        for (LargePowerMonomial g : gens) {
            if (g.base.equalsTo(candidate.base) && g.exponent.equalsTo(candidate.exponent) && g.exponent2.equalsTo(candidate.exponent2))
                return;
        }
        gens.add(candidate);
    }
    private static LargeInteger indexOfBasis(LargeList<LargePowerMonomial> basis, LargePowerMonomial m) {
        for (LargeInteger i = LargeInteger.zero; i.lessThan(basis.size()); i = i.add(1)) {
            if (basis.get(i).equalsTo(m))
                return i;
        }
        return LargeInteger.negativeOne;
    }
    private static LargeVector coefficientVector(RadicalTermSum product, LargeList<LargePowerMonomial> basis) {
        LargeVector v = new LargeVector(LargeInteger.zero);
        for (LargeInteger i = LargeInteger.zero; i.lessThan(basis.size()); i = i.add(1))
            v.append(LargeFraction.zero);
        if (product.constant.notEqualsTo(LargeInteger.zero)) {
            LargeFraction old = v.get(LargeInteger.zero);
            v.set(LargeInteger.zero, (LargeFraction) old.add(product.constant));
        }
        for (LargePowerMonomial m : product.sum) {
            LargeInteger index = indexOfBasis(basis, m);
            if (index.equalsTo(LargeInteger.negativeOne))
                throw new IllegalStateException("Product escaped reciprocal basis: " + m + "\nbasis = " + basis);
            LargeFraction old = v.get(index);
            v.set(index, (LargeFraction) old.add(product.sum.get(m)));
        }
        return v;
    }
    @Override
    public ScalarAtom reciprocal(){
        if (this.equalsTo(LargeInteger.zero))
            throw new ArithmeticException("Error: Cannot divide by 0");

        if (sum.size().equalsTo(LargeInteger.zero))
            return constant.reciprocal();

        // Easy monomial-only case: 1 / (c*m) = c^-1 * m^-1
        if (constant.equalsTo(LargeInteger.zero) && sum.size().equalsTo(LargeInteger.one)) {
            LargePowerMonomial m = sum.keyAtIndex(LargeInteger.zero);
            LargeFraction c = sum.get(m);
            return m.reciprocal().multiply(c.reciprocal());
        }

        LargeList<LargePowerMonomial> basis = new LargeList<>();
        LargePowerMonomial one = new LargePowerMonomial(LargeFraction.one, LargeFraction.one);
        basis.add(one);

        /*
         * Roots of unity basis:
         * If any term contains (-1)^(p/q), include the cyclotomic basis.
         */
        LargeList<LargeInteger> unityDenoms = new LargeList<>();

        for (LargePowerMonomial m : this.sum)
            if (m.exponent2.notEqualsTo(LargeInteger.zero) && m.exponent2.notEqualsTo(LargeInteger.one))
                unityDenoms.add(m.exponent2.denominator);
        if (unityDenoms.size().greaterThan(LargeInteger.zero)) {
            LargeInteger q = LargeInteger.one;
            for(LargeInteger I: unityDenoms) q = q.lcm(I);
            LargeInteger degree = q.multiply(2).totient();
            LargeList<LargePowerMonomial> next = new LargeList<>();
            for (LargeInteger i = LargeInteger.zero; i.lessThan(degree); i = i.add(1)) {
                LargePowerMonomial zetaPower = new LargePowerMonomial(LargeFraction.one, LargeFraction.one, (LargeFraction) i.divide(q));
                addBasisIfMissing(next, zetaPower);
            }
            basis = next;
        }
        /*
         * Radical generator basis:
         * For base^(a/d), include powers of base^(1/d).
         */
        LargeList<LargePowerMonomial> gens = new LargeList<>();
        for (LargePowerMonomial m : this.sum) {
            LargeInteger order = m.exponent.denominator;
            if (order.equalsTo(LargeInteger.one)) continue;
            LargePowerMonomial g = new LargePowerMonomial(m.base, new LargeFraction(LargeInteger.one, order));
            addBasisIfMissing(gens, g);
        }
        //extract the prime factors
        LargeList<LargePowerMonomial> newGens = new LargeList<>();
        for (LargePowerMonomial g : gens) {
            LargeList<PrimeExponent> pr = g.factor();
            for (PrimeExponent p : pr) {
                if (p.base.equalsTo(LargeInteger.negativeOne))
                    continue;
                if (p.exponent.denominator.equalsTo(LargeInteger.one))
                    continue; // rational contribution; not a radical generator
                LargePowerMonomial gen = new LargePowerMonomial(new LargeFraction(p.base, LargeInteger.one), new LargeFraction(LargeInteger.one, p.exponent.denominator));
                addGenIfMissing(newGens, gen);
            }
        }
        //merge same bases
        LargeMap<LargeFraction, LargeInteger> baseToDenom = new LargeMap<>();
        for (LargePowerMonomial g : newGens) {
            if (g.exponent.denominator.equalsTo(LargeInteger.one))
                continue;
            if (baseToDenom.containsKey(g.base)) {
                LargeInteger oldDenom = baseToDenom.get(g.base);
                baseToDenom.put(g.base, oldDenom.lcm(g.exponent.denominator));
            } else
                baseToDenom.put(g.base, g.exponent.denominator);
        }
        LargeList<LargePowerMonomial> merged = new LargeList<>();
        for (LargeFraction base : baseToDenom) {
            LargeInteger denom = baseToDenom.get(base);
            merged.add(new LargePowerMonomial(base, new LargeFraction(LargeInteger.one, denom)));
        }
        gens = merged;
        for (LargePowerMonomial g : gens) {
            LargeList<LargePowerMonomial> next = new LargeList<>();
            LargeInteger order = g.exponent.denominator;
            for (LargePowerMonomial oldBasis : basis) {
                for (LargeInteger i = LargeInteger.zero; i.lessThan(order); i = i.add(1)) {
                    LargePowerMonomial powerOfG = new LargePowerMonomial(g.base, (LargeFraction) i.divide(order));
                    LargePowerMonomial product = (LargePowerMonomial) oldBasis.multiply(powerOfG);
                    addBasisIfMissing(next, product);
                }
            }
            LargeList<LargePowerMonomial> reordered = new LargeList<>();
            reordered.add(one);
            for (LargePowerMonomial b : next) {
                if (b.notEqualsTo(LargeInteger.one))
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
            LargeVector colAsRow = coefficientVector(product, basis);
            M.appendRow(colAsRow);
        }
        LargeVector rhs = new LargeVector();
        rhs.append(LargeFraction.one);
        for (LargeInteger i = LargeInteger.one; i.lessThan(basis.size()); i = i.add(1))
            rhs.append(LargeFraction.zero);
        M.appendRow(rhs);
        M = M.transpose().reducedRowEchelonForm();
        LargeFraction cons = M.get(LargeInteger.zero, (LargeInteger) M.cols().subtract(LargeInteger.one));
        LargeMap<LargePowerMonomial, LargeFraction> resultMap = new LargeMap<>();
        for (LargeInteger i = LargeInteger.one; i.lessThan(M.rows()); i = i.add(1)) {
            LargeFraction coeff = M.get(i, (LargeInteger) M.cols().subtract(LargeInteger.one));
            if (coeff.notEqualsTo(LargeInteger.zero))
                resultMap.put(basis.get(i), coeff);
        }
        RadicalTermSum result = new RadicalTermSum(resultMap, cons);
        if (result.multiply(this).notEqualsTo(LargeInteger.one))
            throw new ArithmeticException("Error: Could not find the proper reciprocal. The used basis was " + basis);
        return result;
    }
    @Override
    public ScalarAtom divide(ScalarAtom other) {return other.reciprocal().multiply(this);}
    @Override
    public ScalarAtom mod(ScalarAtom other) {return this.subtract(this.divide(other).floor().multiply(other));}
}
