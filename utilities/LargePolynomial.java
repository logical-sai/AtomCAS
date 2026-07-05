package com.AtomCAS.utilities;
public class LargePolynomial{
    LargeList<LargeFraction> coefficients; //higher degrees come first
    public LargePolynomial(){coefficients = new LargeList<>(); coefficients.add(LargeFraction.zero);}
    public LargePolynomial(LargeList<LargeFraction> coefficients){
        this.coefficients = new LargeList<>();
        for(LargeFraction I: coefficients) this.coefficients.add(I.copy());
        while(this.coefficients.size().greaterThan(LargeInteger.one) && this.coefficients.getFirst().equalsTo(LargeInteger.zero))
            this.coefficients.removeFirst();
    }
    public LargePolynomial copy(){return new LargePolynomial(coefficients);}
    public static LargePolynomial monomial(LargeFraction coeff, LargeInteger deg){
        LargeList<LargeFraction> coefficients = new LargeList<>();
        coefficients.add(coeff);
        for(LargeInteger i = LargeInteger.zero; i.lessThan(deg); i = i.add(1)) coefficients.add(LargeFraction.zero);
        return new LargePolynomial(coefficients);
    }
    public LargePolynomial monic(){
        LargeList<LargeFraction> coefficients = new LargeList<>();
        LargeFraction coeff = this.coefficients.getFirst();
        for(LargeFraction f: this.coefficients)
            coefficients.add((LargeFraction) f.divide(coeff));
        return new LargePolynomial(coefficients);
    }
    public String toString(){
        if(isZero()) return "0";

        StringBuilder str = new StringBuilder();
        for(LargeInteger i = LargeInteger.zero; i.lessThan(coefficients.size()); i = i.add(1)){
            LargeFraction c = coefficients.get(i);
            if(c.equalsTo(LargeInteger.zero)) continue;
            LargeInteger power = (LargeInteger) coefficients.size().subtract(1).subtract(i);
            if(i.equalsTo(LargeInteger.zero)){
                if(power.equalsTo(LargeInteger.zero)){
                    str.append(c);
                    break;
                }
                if(c.lessThan(LargeInteger.zero)) str.append("-");
                if(c.magnitude().notEqualsTo(LargeInteger.one)) str.append(c.magnitude()).append("*x");
                else str.append("x");
                if(power.notEqualsTo(LargeInteger.one)) str.append("^").append(power);
                continue;
            }
            if(c.lessThan(LargeInteger.zero)) str.append(" - ");
            else str.append(" + ");
            if(i.equalsTo(coefficients.size().subtract(1))){
                str.append(c.magnitude());
                break;
            }
            if(c.magnitude().notEqualsTo(LargeInteger.one)) str.append(c.magnitude()).append("*x");
            else str.append("x");
            if(power.notEqualsTo(LargeInteger.one)) str.append("^").append(power);
        }
        return str.toString();
    }
    public boolean isZero(){
        for(LargeFraction f: coefficients){
            if(f.notEqualsTo(LargeInteger.zero))
                return false;
        }
        return true;
    }
    public boolean isMonomial() {
        for(LargeInteger i = LargeInteger.one; i.lessThan(coefficients.size()); i = i.add(1))
            if(coefficients.get(i).notEqualsTo(LargeInteger.zero))
                return false;
        return true;
    }
    public LargeInteger monomialDegree() {
        LargeInteger deg = degree();
        for (LargeInteger i = LargeInteger.zero; i.lessThan(coefficients.size()); i = i.add(1)) {
            if (coefficients.get(i).notEqualsTo(LargeInteger.zero))
                return (LargeInteger) deg.subtract(i);
        }
        return LargeInteger.zero;
    }
    public LargeFraction monomialCoefficient() {
        for (LargeFraction c : coefficients)
            if (c.notEqualsTo(LargeInteger.zero)) return c;
        return LargeFraction.zero;
    }
    public boolean equalsTo(LargePolynomial p){
        if(degree().notEqualsTo(p.degree()))
            return false;
        for(LargeInteger i = LargeInteger.zero; i.lesserThanOrEqualTo(degree()); i = i.add(1)){
            if(coefficients.get(i).notEqualsTo(p.coefficients.get(i)))
                return false;
        }
        return true;
    }
    public static LargePolynomial one(){
        LargePolynomial P = new LargePolynomial();
        P = P.add(LargeFraction.one);
        return P;
    }
    public static LargePolynomial x(){return new LargePolynomial().add(LargeFraction.one).increaseDegree(LargeInteger.one);}
    public static LargePolynomial xMinus(LargeFraction n){
        LargeList<LargeFraction> list = new LargeList<>();
        list.add(LargeFraction.one);
        list.add(n.negate());
        return new LargePolynomial(list);
    }
    public LargePolynomial negate(){
        LargeList<LargeFraction> P = new LargeList<>();
        for(LargeFraction f: coefficients)
            P.add(f.negate());
        return new LargePolynomial(P);
    }
    public LargeInteger degree(){return (LargeInteger) coefficients.size().subtract(LargeInteger.one);}
    public LargePolynomial increaseDegree(LargeInteger i){
        if(i.lessThan(LargeInteger.zero)) throw new IllegalArgumentException("Error: Input means decreasing the degree");
        if(this.isZero()) return new LargePolynomial();
        LargeList<LargeFraction> cands = new LargeList<>();
        for(LargeFraction I: coefficients) cands.add(I);
        for(LargeInteger j = LargeInteger.zero; j.lessThan(i); j = j.add(1)) cands.add(LargeFraction.zero);
        return new LargePolynomial(cands);
    }
    public LargePolynomial add(LargeFraction other){
        LargeList<LargeFraction> cands = new LargeList<>();
        for(LargeFraction I: coefficients) cands.add(I);
        cands.set(cands.size().subtract(1), (LargeFraction) cands.get(cands.size().subtract(1)).add(other));
        return new LargePolynomial(cands);
    }
    public LargePolynomial add(LargePolynomial other){
        LargeList<LargeFraction> cands = new LargeList<>();
        if(this.coefficients.size().greaterThan(other.coefficients.size())){
            LargeInteger diff = (LargeInteger) this.coefficients.size().subtract(other.coefficients.size());
            for(LargeInteger i = LargeInteger.zero; i.lessThan(this.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    cands.add((LargeFraction) this.coefficients.get(i).add(other.coefficients.get((LargeInteger) i.subtract(diff))));
                else cands.add(this.coefficients.get(i));
            }
            return new LargePolynomial(cands);
        }else if(other.coefficients.size().greaterThan(this.coefficients.size())){
            LargeInteger diff = (LargeInteger) other.coefficients.size().subtract(this.coefficients.size());
            for(LargeInteger i = LargeInteger.zero; i.lessThan(other.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    cands.add((LargeFraction) other.coefficients.get(i).add(this.coefficients.get((LargeInteger) i.subtract(diff))));
                else cands.add(other.coefficients.get(i));
            }
            return new LargePolynomial(cands);
        }
        for(LargeInteger i = LargeInteger.zero; i.lessThan(this.coefficients.size()); i = i.add(1))
            cands.add((LargeFraction) this.coefficients.get(i).add(other.coefficients.get(i)));
        return new LargePolynomial(cands);
    }
    public LargePolynomial subtract(LargeFraction other){
        LargeList<LargeFraction> cands = new LargeList<>();
        for(LargeFraction I: coefficients) cands.add(I);
        cands.set(cands.size().subtract(1), (LargeFraction) cands.get(cands.size().subtract(1)).subtract(other));
        return new LargePolynomial(cands);
    }
    public LargePolynomial subtract(LargePolynomial other){
        LargeList<LargeFraction> cands = new LargeList<>();

        if(this.coefficients.size().greaterThan(other.coefficients.size())){
            LargeInteger diff = (LargeInteger) this.coefficients.size().subtract(other.coefficients.size());

            for(LargeInteger i = LargeInteger.zero; i.lessThan(this.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff)){
                    cands.add((LargeFraction)
                            this.coefficients.get(i).subtract(
                                    other.coefficients.get((LargeInteger) i.subtract(diff))
                            )
                    );
                }else{
                    cands.add(this.coefficients.get(i));
                }
            }

            return new LargePolynomial(cands);

        }else if(other.coefficients.size().greaterThan(this.coefficients.size())){
            LargeInteger diff = (LargeInteger) other.coefficients.size().subtract(this.coefficients.size());
            for(LargeInteger i = LargeInteger.zero; i.lessThan(other.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    cands.add((LargeFraction) this.coefficients.get((LargeInteger) i.subtract(diff)).subtract(other.coefficients.get(i)));
                else
                    cands.add(other.coefficients.get(i).negate());
            }
            return new LargePolynomial(cands);
        }
        for(LargeInteger i = LargeInteger.zero; i.lessThan(this.coefficients.size()); i = i.add(1))
            cands.add((LargeFraction) this.coefficients.get(i).subtract(other.coefficients.get(i)));
        return new LargePolynomial(cands);
    }
    public LargePolynomial multiply(LargeFraction other){
        if(other.equalsTo(LargeInteger.zero) || isZero()) return new LargePolynomial();
        LargeList<LargeFraction> cands = new LargeList<>();
        for(LargeFraction I: coefficients)
            cands.add((LargeFraction) I.multiply(other));
        return new LargePolynomial(cands);
    }
    public LargePolynomial multiply(LargePolynomial other) {
        if (this.isZero() || other.isZero()) return new LargePolynomial();
        if (this.isMonomial())
            return other.multiply(this.monomialCoefficient()).increaseDegree(this.monomialDegree());
        if (other.isMonomial())
            return this.multiply(other.monomialCoefficient()).increaseDegree(other.monomialDegree());
        // existing generic multiply
        LargeList<LargePolynomial> polys = new LargeList<>();
        for (LargeInteger i = LargeInteger.zero; i.lessThan(this.coefficients.size()); i = i.add(1))
            polys.add(other.multiply(coefficients.get(i)).increaseDegree((LargeInteger) this.degree().subtract(i)));
        LargePolynomial result = new LargePolynomial();
        for (LargePolynomial P : polys) result = result.add(P);
        return result;
    }
    public LargePolynomial square(){return this.multiply(this);}
    public LargePolynomial monomialPower(LargeInteger I){
        if(I.sign().equalsTo(LargeInteger.negativeOne)) throw new ArithmeticException("Error: Rational functions not implemented yet");
        if(I.equalsTo(LargeInteger.zero))
            return LargePolynomial.one();
        LargePolynomial P = new LargePolynomial();
        P = P.add(LargeFraction.one);
        LargeInteger expTemp = I.copy();
        LargePolynomial basTemp = new LargePolynomial(this.coefficients);
        while(true){
            if(expTemp.isOdd()){
                P = P.multiply(basTemp);
                if(expTemp.equalsTo(LargeInteger.one)) break;
            }
            basTemp = basTemp.square();
            expTemp = expTemp.longDivision(LargeInteger.two);
        }
        return P;
    }
    public LargePolynomial longDivision(LargePolynomial divisor){return divideAndRemainder(divisor)[0];}
    public LargePolynomial remainder(LargePolynomial divisor){return divideAndRemainder(divisor)[1];}
    private LargePolynomial[] divideAndRemainder(LargePolynomial divisor) {
        if (divisor.isZero()) throw new ArithmeticException("Error: Cannot divide by 0");
        LargePolynomial remainder = this.copy();
        LargePolynomial quotient = new LargePolynomial();
        LargeInteger divisorDegree = divisor.degree();
        LargeFraction divisorLead = divisor.coefficients.getFirst();
        while (!remainder.isZero() && remainder.degree().greaterThanOrEqualTo(divisorDegree)) {
            LargeInteger degreeDiff = (LargeInteger) remainder.degree().subtract(divisorDegree);
            LargeFraction coeff = (LargeFraction) remainder.coefficients.getFirst().divide(divisorLead);
            LargePolynomial term = LargePolynomial.monomial(coeff, degreeDiff);
            quotient = quotient.add(term);
            remainder = remainder.subtract(term.multiply(divisor));
        }
        return new LargePolynomial[]{quotient, remainder};
    }
    public LargePolynomial gcd(LargePolynomial other) {
        LargePolynomial a = this.copy();
        LargePolynomial b = other.copy();
        if (a.isZero()) return b.monic();
        if (b.isZero()) return a.monic();
        while (!b.isZero()) {
            LargePolynomial r = a.remainder(b);
            a = b;
            b = r;
        }
        return a;
    }
    public static LargePolynomial cyclotomic(LargeInteger n){
        if (n.lesserThanOrEqualTo(LargeInteger.zero))
            throw new IllegalArgumentException("Error: Cyclotomic index must be positive");

        if (n.equalsTo(LargeInteger.one))
            return LargePolynomial.xMinus(LargeFraction.one);

        if (n.isProbablyPrime()){
            LargeList<LargeFraction> L = new LargeList<>();
            for(LargeInteger i = LargeInteger.zero; i.lessThan(n); i=i.add(1))
                L.add(LargeFraction.one);
            return new LargePolynomial(L);
        }
        // Φ_2m(x) = Φ_m(-x), m odd
        if (n.isEven()) {
            LargeInteger m = n.longDivision(LargeInteger.two);
            if (m.isOdd())
                return cyclotomic(m).compose(x().negate());
        }

        LargeMap<LargeInteger, LargeFraction> factors = n.factor();
        LargePolynomial repeatedPrimeCase = tryRepeatedPrimeShortcut(n, factors);
        if (repeatedPrimeCase != null)
            return repeatedPrimeCase;
        LargePolynomial P = monomial(LargeFraction.one, n).subtract(LargeFraction.one);
        LargeList<LargeInteger> N = n.divisorsProper();
        for(LargeInteger I: N)
            P = P.longDivision(cyclotomic(I));
        return P;
    }
    private static LargePolynomial tryRepeatedPrimeShortcut(LargeInteger n, LargeMap<LargeInteger, LargeFraction> factors) {
        LargeInteger rad = LargeInteger.one;
        for (LargeInteger p : factors) {
            if (p.equalsTo(LargeInteger.negativeOne)) continue;
            rad = (LargeInteger) rad.multiply(p);
        }
        if (rad.equalsTo(n)) return null; // squarefree, no help
        LargeInteger step = n.longDivision(rad);
        return cyclotomic(rad).composeXPower(step);
    }
    //this method derives a polynomial from (x + arg)^degree
    public static LargePolynomial binomial(LargeInteger degree, LargeFraction scale, LargeFraction arg){
        if(degree.lessThan(LargeInteger.zero)) throw new IllegalArgumentException("Error: Result not a polynomial for negative degrees");
        LargePolynomial P = new LargePolynomial();
        for(LargeInteger i = LargeInteger.zero; i.lesserThanOrEqualTo(degree); i=i.add(1))
            P = P.increaseDegree(LargeInteger.one).multiply(scale).add((LargeFraction) degree.nCr(i).multiply(arg.power(i)));
        return P;
    }
    public ScalarAtom plugIn(ScalarAtom s){
        ScalarAtom result = LargeInteger.zero;
        for(LargeInteger i = LargeInteger.zero; i.lessThan(coefficients.size()); i = i.add(1))
            result = Parser.parse(result.multiply(s).add(coefficients.get(i)).toString());
        return result;
    }
    public LargePolynomial composeXPower(LargeInteger k) {
        if (k.lesserThanOrEqualTo(LargeInteger.zero))
            throw new IllegalArgumentException("Error: exponent must be positive");
        LargeList<LargeFraction> out = new LargeList<>();
        LargeInteger newDegree = (LargeInteger) this.degree().multiply(k);
        for (LargeInteger e = newDegree; e.greaterThanOrEqualTo(LargeInteger.zero); e = e.subtract(1)) {
            if (e.mod(k).equalsTo(LargeInteger.zero)) {
                LargeInteger oldPower = e.longDivision(k);
                LargeInteger index = (LargeInteger) this.degree().subtract(oldPower);
                out.add(this.coefficients.get(index));
            }else out.add(LargeFraction.zero);
            if (e.equalsTo(LargeInteger.zero)) break;
        }

        return new LargePolynomial(out);
    }
    public LargePolynomial compose(LargePolynomial inner){
        LargePolynomial result = new LargePolynomial();
        for(LargeInteger i = LargeInteger.zero; i.lessThan(coefficients.size()); i = i.add(1))
            result = result.multiply(inner).add(coefficients.get(i));
        return result;
    }
    public static LargePolynomial annihilatingPolynomial(ScalarAtom s){
        if(!s.isAlgebraic()) throw new IllegalArgumentException("Error: Transcendental numbers do not satisfy any polynomials");
        LargePolynomial P = x();
        switch (s) {
            case LargeInteger I -> {return P.subtract(new LargeFraction(I, LargeInteger.one));}
            case LargeFraction F -> {
                P = P.multiply(new LargeFraction(F.denominator, LargeInteger.one));
                P = P.add(new LargeFraction(F.numerator, LargeInteger.negativeOne));
                return P.primitivePart();
            }case LargePowerMonomial L -> {
                //x = (-1)^(p/q) * b^(r/s)
                LargeInteger I = L.exponent2.denominator.lcm(L.exponent.denominator);
                P = P.monomialPower(I);
                LargePowerMonomial M = L.power(new LargeFraction(I, LargeInteger.one));
                if (M.exponent2.equalsTo(LargeInteger.one)) P = P.add(M.base);
                else P = P.add(M.base.negate());
                return P.primitivePart();
            }case RadicalTerm R -> {return annihilatingPolynomial(new RadicalTermSum().add(R));}
            case RadicalTermSum R -> {
                if(R.sum.isEmpty()) return annihilatingPolynomial(R.constant);
                LargeList<LargePowerMonomial> basis = new LargeList<>();
                basis.add(LargePowerMonomial.one);
                /*
                 * Roots of unity basis:
                 * If any term contains (-1)^(p/q), include the cyclotomic basis.
                 */
                LargeList<LargeInteger> unityDenoms = new LargeList<>();

                for (LargePowerMonomial m : R.sum)
                    if (m.exponent2.notEqualsTo(LargeInteger.zero) && m.exponent2.notEqualsTo(LargeInteger.one))
                        unityDenoms.add(m.exponent2.denominator);
                if (unityDenoms.size().greaterThan(LargeInteger.zero)) {
                    LargeInteger q = LargeInteger.one;
                    for(LargeInteger I: unityDenoms) q = q.lcm(I);
                    LargeInteger degree = q.multiply(2).totient();
                    LargeList<LargePowerMonomial> next = new LargeList<>();
                    for (LargeInteger i = LargeInteger.zero; i.lessThan(degree); i = i.add(1)) {
                        LargePowerMonomial zetaPower = new LargePowerMonomial(LargeFraction.one, LargeFraction.one, (LargeFraction) i.divide(q));
                        boolean contains = false;
                        for(LargePowerMonomial m: next){
                            if(m.equalsTo(zetaPower)){
                                contains = true;
                                break;
                            }
                        }
                        if(!contains) next.add(zetaPower);
                    }
                    basis = next;
                }
                /*
                 * Radical generator basis:
                 * For base^(a/d), include powers of base^(1/d).
                 */
                LargeList<LargePowerMonomial> gens = new LargeList<>();
                for (LargePowerMonomial m : R.sum) {
                    LargeInteger order = m.exponent.denominator;
                    if (order.equalsTo(LargeInteger.one)) continue;
                    LargePowerMonomial g = new LargePowerMonomial(m.base, new LargeFraction(LargeInteger.one, order));
                    boolean contains = false;
                    for (LargePowerMonomial b : gens)
                        if (b.equalsTo(g)){
                            contains = true;
                            break;
                        }
                    if(!contains) gens.add(g);
                }
                //extract the prime factors
                LargeList<LargePowerMonomial> newGens = new LargeList<>();
                for (LargePowerMonomial g : gens) {
                    LargeMap<LargeInteger, LargeFraction> pr = g.factor();
                    for (LargeInteger I : pr) {
                        if (I.equalsTo(LargeInteger.negativeOne))
                            continue;
                        if (pr.get(I).denominator.equalsTo(LargeInteger.one))
                            continue; // rational contribution; not a radical generator
                        LargePowerMonomial gen = new LargePowerMonomial(new LargeFraction(I, LargeInteger.one), new LargeFraction(LargeInteger.one, pr.get(I).denominator));
                        boolean contains = false;
                        for (LargePowerMonomial m : newGens)
                            if(m.equalsTo(gen)){
                                contains = true;
                                break;
                            }
                        if(!contains) newGens.add(gen);
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
                            boolean contains = false;
                            for(LargePowerMonomial m: next){
                                if(m.equalsTo(product)){
                                    contains = true;
                                    break;
                                }
                            }
                            if(!contains) next.add(product);
                        }
                    }
                    LargeList<LargePowerMonomial> reordered = new LargeList<>();
                    reordered.add(LargePowerMonomial.one);
                    for (LargePowerMonomial b : next) {
                        if (b.notEqualsTo(LargeInteger.one))
                            reordered.add(b);
                    }
                    basis = reordered;
                }
                LargeMatrix M = new LargeMatrix();
                for (LargePowerMonomial b : basis) {
                    RadicalTermSum product = (RadicalTermSum) s.multiply(b);
                    LargeVector colAsRow = new LargeVector(basis.size());
                    if (product.constant.notEqualsTo(LargeInteger.zero)) {
                        LargeFraction old = colAsRow.get(LargeInteger.zero);
                        colAsRow.set(LargeInteger.zero, (LargeFraction) old.add(product.constant));
                    }
                    for (LargePowerMonomial m : product.sum) {
                        LargeInteger index = LargeInteger.negativeOne;
                        for(LargeInteger i = LargeInteger.zero; i.lessThan(basis.size()); i = i.add(1)){
                            if(basis.get(i).equalsTo(m)){
                                index = i;
                                break;
                            }
                        }
                        if (index.equalsTo(LargeInteger.negativeOne))
                            throw new IllegalStateException("Product escaped reciprocal basis: " + m + "\nbasis = " + basis);
                        LargeFraction old = colAsRow.get(index);
                        colAsRow.set(index, (LargeFraction) old.add(product.sum.get(m)));
                    }
                    // append as row for now, then transpose later
                    M.appendRow(colAsRow);
                }
                return M.transpose().characteristicPolynomial().primitivePart();
            }case LargePowerNode n -> {
                LargePolynomial PS = minimalPolynomial(n.signFactor);
                LargePolynomial PM = minimalPolynomial(n.base).composeXPower(n.exponent.denominator);
                if(PS.equalsTo(xMinus(LargeFraction.one))) return PM;
                return null;
            }
            default -> throw new UnsupportedOperationException("Error: Annihilating polynomial does not support this type yet");
        }
    }
    public static LargePolynomial minimalPolynomial(ScalarAtom s) {
        if (!s.isAlgebraic())
            throw new IllegalArgumentException("Error: Transcendental numbers do not satisfy any polynomials");
        LargePolynomial ann = annihilatingPolynomial(s);
        assert ann != null;
        LargeList<LargePolynomial> LQ = ann.factorOverRationals();
        for(LargePolynomial p: LQ){
            if(p.plugIn(s).equalsTo(LargeInteger.zero))
                return p.primitivePart();
        }
        throw new UnsupportedOperationException("Error: Minimal polynomial does not support this type yet");
    }
    private LargePolynomial extractLinearFactor(){
        LargePolynomial P = this.primitivePart();
        LargeList<LargeFraction> rationals = new LargeList<>();
        LargeList<LargeInteger> top = new LargeList<>();
        for(LargeInteger i = P.coefficients.size().subtract(1); i.greaterThan(LargeInteger.zero); i=i.subtract(1)){
            if(P.coefficients.get(i).equalsTo(LargeInteger.zero)) continue;
            top = P.coefficients.get(i).numerator.divisorsProper();
            top.add(P.coefficients.get(i).numerator);
            break;
        }
        LargeList<LargeInteger> bottom = P.coefficients.getFirst().numerator.divisorsProper();
        bottom.add(P.coefficients.getFirst().numerator);
        for(LargeInteger i: top){
            for(LargeInteger j: bottom){
                rationals.add(new LargeFraction(i, j));
                rationals.add(new LargeFraction(i.negate(), j));
            }
        }
        rationals.add(LargeFraction.zero);
        for (LargeFraction f : rationals) {
            LargePolynomial[] DR = P.divideAndRemainder(x().subtract(f));
            if (DR[1].isZero())
                return x().subtract(f);
        }
        return null;
    }
    private boolean irreducibleByEisenstein(){
        LargePolynomial P = this.primitivePart();
        LargeMap<LargeInteger, LargeFraction> BF = new LargeMap<>();
        for (LargeInteger i = LargeInteger.one; i.lessThan(P.coefficients.size()); i = i.add(1)) {
            if (P.coefficients.get(i).equalsTo(LargeInteger.zero)) continue;
            // Since primitivePart guarantees integer coefficients:
            BF = P.coefficients.get(i).numerator.magnitude().factor();
            break;
        }
        for (LargeInteger p : BF) {
            if (p.lesserThanOrEqualTo(LargeInteger.one)) continue;
            // p must not divide leading coefficient
            if (P.coefficients.getFirst().numerator.mod(p).equalsTo(LargeInteger.zero)) continue;
            boolean insufficient = false;
            // p must divide every coefficient except the leading one
            for (LargeInteger i = LargeInteger.one;
                 i.lessThan(P.coefficients.size());
                 i = i.add(1)) {
                if (P.coefficients.get(i).numerator.mod(p).notEqualsTo(LargeInteger.zero)) {
                    insufficient = true;
                    break;
                }
            }
            if (insufficient) continue;
            // p^2 must not divide the constant term
            if (P.coefficients.getLast().numerator.mod(p.square()).notEqualsTo(LargeInteger.zero))
                return true;
        }
        return false; //does not necessarily mean reducible
    }
    public LargeList<LargePolynomial> factorOverRationals() {
        LargeList<LargePolynomial> factors = new LargeList<>();
        LargeList<LargePolynomial> work = new LargeList<>();
        LargePolynomial P = this.primitivePart();
        LargePolynomial GC = P.gcd(P.derivative());
        LargePolynomial factor = GC.longDivision(GC.primitivePart());
        LargePolynomial factor2 = P.longDivision(GC).longDivision(P.longDivision(GC).primitivePart());
        if(!GC.equalsTo(one())){
            LargePolynomial product = factor.multiply(factor2);
            if(!product.equalsTo(one())) factors.add(product);
            work.add(GC.primitivePart());
            work.add(P.longDivision(GC.primitivePart()));
        }else work.add(P);
        while (work.size().greaterThan(LargeInteger.zero)) {
            LargePolynomial F = work.getLast();
            work.removeLast();

            if (F.degree().lesserThanOrEqualTo(LargeInteger.one)) {
                factors.add(F);
                continue;
            }
            // 1. Rational root theorem
            LargePolynomial linear = F.extractLinearFactor();
            if (linear != null){
                factors.add(linear);
                work.add(F.longDivision(linear));
                continue;
            }
            // 2. Eisenstein irreducibility proof
            if (F.irreducibleByEisenstein()) {
                factors.add(F);
                continue;
            }
            // 3. Mod-prime irreducibility proof
            if (F.irreducibleBySomeModPrimeTest()) {
                factors.add(F);
                continue;
            }

            // 4. Mod-prime factor discovery
            LargePolynomial candidate = F.tryFactorFromModPrime();
            if (candidate != null && candidate.degree().greaterThan(LargeInteger.zero) && candidate.degree().lessThan(F.degree()) && F.remainder(candidate).isZero()) {
                work.add(candidate.monic());
                work.add(F.longDivision(candidate).monic());
                continue;
            }

            // 5. If all methods fail, do not pretend it is irreducible.
            // Either throw, or add as unresolved.
            factors.add(F); // unresolved factor, not necessarily irreducible
        }
        return factors;
    }
    private boolean irreducibleBySomeModPrimeTest() {
        LargePolynomial P = this.primitivePart();
        for (int p0 : LargeInteger.SMALL_PRIMES) {
            LargeInteger p = new LargeInteger(p0);
            // skip primes dividing leading coefficient
            if (P.coefficients.getFirst().numerator.mod(p).equalsTo(LargeInteger.zero))
                continue;
            ModPolynomial MP = ModPolynomial.fromLargePolynomial(P, p);
            // skip inseparable/bad reductions
            if (MP.gcd(MP.derivative()).notEqualsTo(ModPolynomial.one(p)))
                continue;

            if (MP.isIrreducible())
                return true;
        }
        return false;
    }
    private LargePolynomial tryFactorFromModPrime(){
        for(int i: LargeInteger.SMALL_PRIMES){
            LargeInteger I = new LargeInteger(i);
            ModPolynomial MP = ModPolynomial.fromLargePolynomial(this, I);
            //ensure MP is squareFree
            ModPolynomial GP = MP.gcd(MP.derivative());
            if(GP.notEqualsTo(ModPolynomial.one(I))) continue;
            //test all numbers up to I
            ModPolynomial product = ModPolynomial.one(I);
            for(LargeInteger k = LargeInteger.zero; k.lessThan(I); k = k.add(1)){
                LargeInteger J = MP.plugIn(k);
                if(J.equalsTo(LargeInteger.zero)){
                    LargeList<LargeInteger> lt = new LargeList<>();
                    lt.add(LargeInteger.one);
                    lt.add(k.negate());
                    product = product.multiply(new ModPolynomial(lt, I));
                }
            }
            if(product.notEqualsTo(ModPolynomial.one(I))){
                LargeList<LargeFraction> lt = new LargeList<>();
                for(LargeInteger C: product.coeffs) lt.add(new LargeFraction(C, LargeInteger.one));
                return new LargePolynomial(lt);
            }
        }
        return null;
    }
    private LargePolynomial primitivePart() {
        LargeFraction L = LargeFraction.zero;
        for(LargeFraction f: coefficients) L = L.gcd(f);
        return this.multiply(L.reciprocal()).multiply((LargeFraction) coefficients.getFirst().sign());
    }

    public LargePolynomial derivative(){
        LargeList<LargeFraction> coeffs = new LargeList<>();
        for(LargeInteger i = LargeInteger.zero; i.lessThan(degree()); i=i.add(1))
            coeffs.add((LargeFraction) coefficients.get(i).multiply(degree().subtract(i)));
        return new LargePolynomial(coeffs);
    }
}
class ModPolynomial {
    // coefficients high degree first, matching LargePolynomial
    LargeList<LargeInteger> coeffs;
    LargeInteger p;
    public ModPolynomial(LargeList<LargeInteger> coefficients, LargeInteger p) {
        if (!p.isProbablyPrime())
            throw new IllegalArgumentException("Modulus must be prime for field arithmetic");
        this.p = p;
        this.coeffs = new LargeList<>();
        for (LargeInteger c : coefficients)
            this.coeffs.add((LargeInteger) c.mod(p));
        trimLeadingZeros();
    }
    private void trimLeadingZeros(){
        while(coeffs.size().greaterThan(LargeInteger.one) && coeffs.getFirst().equalsTo(LargeInteger.zero))
            coeffs.removeFirst();
    }
    ModPolynomial copy(){
        return new ModPolynomial(coeffs.copy(), p.copy());
    }
    private static LargeInteger mod(LargeInteger a, LargeInteger p) {
        LargeInteger r = (LargeInteger) a.mod(p);
        return r.lessThan(LargeInteger.zero) ? (LargeInteger) r.add(p) : r;
    }
    private static LargeList<LargeInteger> trimNormalize(LargeList<LargeInteger> a, LargeInteger p) {
        LargeInteger start = LargeInteger.zero;
        while (start.lessThan(a.size().subtract(1)) && mod(a.get(start), p).equalsTo(LargeInteger.zero))
            start=start.add(1);
        LargeList<LargeInteger> out = new LargeList<>();
        for (LargeInteger i = start; i.lessThan(a.size()); i=i.add(1))
            out.add(mod(a.get(i), p));
        return out;
    }
    public ModPolynomial monic() {
        if (isZero()) return this;
        LargeInteger inv = invMod(lead(), p);
        LargeList<LargeInteger> out = new LargeList<>();
        for (LargeInteger i = LargeInteger.zero; i.lessThan(coeffs.size()); i=i.add(1))
            out.add(mod((LargeInteger) coeffs.get(i).multiply(inv), p));
        return new ModPolynomial(out, p);
    }
    private LargeInteger norm(LargeInteger a) {
        LargeInteger r = (LargeInteger) a.mod(p);
        if (r.lessThan(LargeInteger.zero))
            r = (LargeInteger) r.add(p);
        return r;
    }
    public LargeInteger degree() {return coeffs.size().subtract(1);}
    public boolean isZero() {return coeffs.size().equalsTo(LargeInteger.one) && coeffs.getFirst().equalsTo(LargeInteger.zero);}
    public boolean equalsTo(ModPolynomial other) {
        if (this.p.notEqualsTo(other.p)) return false;
        if (this.coeffs.size().notEqualsTo(other.coeffs.size())) return false;
        for (LargeInteger i = LargeInteger.zero; i.lessThan(coeffs.size()); i=i.add(1))
            if (this.coeffs.get(i).notEqualsTo(other.coeffs.get(i)))
                return false;
        return true;
    }
    public boolean notEqualsTo(ModPolynomial other) {return !equalsTo(other);}
    public LargeInteger lead() {return coeffs.getFirst();}
    public static ModPolynomial one(LargeInteger p) {
        LargeList<LargeInteger> coefficients = new LargeList<>();
        coefficients.add(LargeInteger.one);
        return new ModPolynomial(coefficients, p);
    }
    public static ModPolynomial x(LargeInteger p) {
        LargeList<LargeInteger> coefficients = new LargeList<>();
        coefficients.add(LargeInteger.one);
        coefficients.add(LargeInteger.zero);
        return new ModPolynomial(coefficients, p);
    }
    static ModPolynomial fromLargePolynomial(LargePolynomial f, LargeInteger p) {
        LargeInteger n = new LargeInteger(f.coefficients.size().toString());
        LargeList<LargeInteger> a = new LargeList<>();
        for (LargeInteger i = LargeInteger.zero; i.lessThan(n); i=i.add(1)){
            LargeFraction c = f.coefficients.get(i);
            if (c.denominator.notEqualsTo(LargeInteger.one))
                throw new IllegalArgumentException("Expected integer coefficients");
            a.add(new LargeInteger(c.numerator.mod(p).toString()));
        }
        return new ModPolynomial(a, p);
    }
    static LargeInteger invMod(LargeInteger a, LargeInteger p) {
        a = mod(a, p);
        if (a.equalsTo(LargeInteger.zero)) throw new ArithmeticException("No inverse for 0");
        LargeInteger t = LargeInteger.zero, newT = LargeInteger.one;
        LargeInteger r = p, newR = a;

        while (newR.notEqualsTo(LargeInteger.zero)) {
            LargeInteger q = r.longDivision(newR);
            LargeInteger tempT = (LargeInteger) t.subtract(q.multiply(newT));
            t = newT;
            newT = tempT;
            LargeInteger tempR = (LargeInteger) r.subtract(q.multiply(newR));
            r = newR;
            newR = tempR;
        }

        if (r.greaterThan(LargeInteger.one)) throw new ArithmeticException("Not invertible");
        return mod(t, p);
    }
    ModPolynomial gcd(ModPolynomial other) {
        ModPolynomial a = this;
        ModPolynomial b = other;
        while (!b.isZero()) {
            ModPolynomial r = a.remainder(b);
            a = b;
            b = r;
        }
        return a.monic();
    }
    ModPolynomial add(ModPolynomial other) {
        if (this.p.notEqualsTo(other.p))
            throw new IllegalArgumentException("Different moduli");
        LargeInteger max = this.coeffs.size().greaterThan(other.coeffs.size()) ? this.coeffs.size() : other.coeffs.size();
        LargeList<LargeInteger> a = padLeft(this.coeffs, max);
        LargeList<LargeInteger> b = padLeft(other.coeffs, max);
        LargeList<LargeInteger> out = new LargeList<>();
        for (LargeInteger i = LargeInteger.zero; i.lessThan(max); i=i.add(1))
            out.add(mod((LargeInteger) a.get(i).add(b.get(i)), p));
        return new ModPolynomial(out, p);
    }
    ModPolynomial subtract(ModPolynomial other) {
        if (this.p.notEqualsTo(other.p))
            throw new IllegalArgumentException("Different moduli");
        LargeInteger max = this.coeffs.size().greaterThan(other.coeffs.size()) ? this.coeffs.size() : other.coeffs.size();
        LargeList<LargeInteger> a = padLeft(this.coeffs, max);
        LargeList<LargeInteger> b = padLeft(other.coeffs, max);
        LargeList<LargeInteger> out = new LargeList<>();
        for (LargeInteger i = LargeInteger.zero; i.lessThan(max); i=i.add(1))
            out.add(mod((LargeInteger) a.get(i).subtract(b.get(i)), p));
        return new ModPolynomial(out, p);
    }
    static LargeList<LargeInteger> padLeft(LargeList<LargeInteger> a, LargeInteger targetSize) {
        LargeList<LargeInteger> out = a.copy();
        while (out.size().lessThan(targetSize))
            out.addFirst(LargeInteger.zero);
        return out;
    }
    ModPolynomial multiply(ModPolynomial other) {
        if (this.p.notEqualsTo(other.p))
            throw new IllegalArgumentException("Different moduli");
        LargeList<LargeInteger> out = new LargeList<>();
        for(LargeInteger i = LargeInteger.zero; i.lesserThanOrEqualTo(this.coeffs.size().add(other.coeffs.size()).subtract(LargeInteger.two)); i=i.add(1))
            out.add(LargeInteger.zero);
        for (LargeInteger i = LargeInteger.zero; i.lessThan(this.coeffs.size()); i=i.add(1))
            for (LargeInteger j = LargeInteger.zero; j.lessThan(other.coeffs.size()); j=j.add(1))
                out.set((LargeInteger) i.add(j), mod((LargeInteger) out.get((LargeInteger) i.add(j)).add(this.coeffs.get(i).multiply(other.coeffs.get(j))), p));
        return new ModPolynomial(out, p);
    }
    ModPolynomial multiplyMod(ModPolynomial other, ModPolynomial modulus) {return this.multiply(other).remainder(modulus);}
    ModPolynomial longDivision(LargeInteger other){
        ModPolynomial P = this.copy();
        for(LargeInteger i = LargeInteger.zero; i.lessThan(P.coeffs.size()); i=i.add(1))
            P.coeffs.set(i, (LargeInteger) P.coeffs.get(i).multiply(other.modInverse(p)).mod(p));
        return P;
    }
    private ModPolynomial[] divideAndRemainder(ModPolynomial divisor) {
        if (this.p.notEqualsTo(divisor.p))
            throw new IllegalArgumentException("Different moduli");
        if (divisor.isZero())
            throw new ArithmeticException("Cannot divide by zero polynomial");

        LargeList<LargeInteger> rem = this.coeffs.copy();
        LargeList<LargeInteger> quo = new LargeList<>();

        LargeInteger qSize = (LargeInteger) this.degree().subtract(divisor.degree()).add(LargeInteger.one);
        if (qSize.lesserThanOrEqualTo(LargeInteger.zero)) {
            quo.add(LargeInteger.zero);
            return new ModPolynomial[]{
                    new ModPolynomial(quo, p),
                    new ModPolynomial(rem, p)
            };
        }

        for (LargeInteger i = LargeInteger.zero; i.lessThan(qSize); i = i.add(1))
            quo.add(LargeInteger.zero);

        LargeInteger divisorDeg = divisor.degree();
        LargeInteger divisorLeadInv = invMod(divisor.lead(), p);

        while (rem.size().subtract(1).greaterThanOrEqualTo(divisorDeg)
                && !(rem.size().equalsTo(LargeInteger.one) && rem.getFirst().equalsTo(LargeInteger.zero))) {

            LargeInteger remDeg = rem.size().subtract(1);
            LargeInteger degreeDiff = (LargeInteger) remDeg.subtract(divisorDeg);

            LargeInteger scale = mod((LargeInteger) rem.getFirst().multiply(divisorLeadInv), p);

            LargeInteger qIndex = (LargeInteger) qSize.subtract(1).subtract(degreeDiff);
            quo.set(qIndex, scale);

            for (LargeInteger i = LargeInteger.zero; i.lessThan(divisor.coeffs.size()); i = i.add(1)) {
                rem.set(i, mod((LargeInteger) rem.get(i).subtract(scale.multiply(divisor.coeffs.get(i))), p));
            }
            rem = trimNormalize(rem, p);
        }
        return new ModPolynomial[]{
                new ModPolynomial(quo, p),
                new ModPolynomial(rem, p)
        };
    }
    public ModPolynomial longDivision(ModPolynomial divisor) {
        return divideAndRemainder(divisor)[0];
    }
    public ModPolynomial remainder(ModPolynomial divisor) {
        return divideAndRemainder(divisor)[1];
    }
    ModPolynomial powMod(LargeInteger exp, ModPolynomial modulus){
        ModPolynomial result = one(p);
        ModPolynomial base = this.remainder(modulus);
        while(true){
            if (exp.isOdd()){
                result = result.multiplyMod(base, modulus);
                if(exp.equalsTo(LargeInteger.one)) break;
            }
            base = base.multiplyMod(base, modulus);
            exp = exp.longDivision(LargeInteger.two);
        }
        return result;
    }
    private LargeInteger addMod(LargeInteger a, LargeInteger b) {
        return norm((LargeInteger) a.add(b));
    }
    private LargeInteger subMod(LargeInteger a, LargeInteger b) {
        return norm((LargeInteger) a.subtract(b));
    }
    private LargeInteger mulMod(LargeInteger a, LargeInteger b) {
        return norm((LargeInteger) a.multiply(b));
    }
    private LargeInteger divMod(LargeInteger a, LargeInteger b) {
        if (norm(b).equalsTo(LargeInteger.zero))
            throw new ArithmeticException("Error: Cannot divide by 0 mod " + p);
        return mulMod(a, norm(b).modInverse(p));
    }
    ModPolynomial derivative() {
        LargeList<LargeInteger> out = new LargeList<>();
        LargeInteger n = degree();
        if (n.equalsTo(LargeInteger.zero)){
            out.add(LargeInteger.zero);
            return new ModPolynomial(out, p);
        }
        for(LargeInteger i = LargeInteger.zero; i.lessThan(n); i=i.add(1)){
            LargeInteger power = (LargeInteger) n.subtract(i);
            out.add(mod((LargeInteger) coeffs.get(i).multiply(power), p));
        }
        return new ModPolynomial(out, p);
    }
    boolean isIrreducible() {
        if (degree().lesserThanOrEqualTo(LargeInteger.zero)) return false;
        if (degree().equalsTo(LargeInteger.one)) return true;

        ModPolynomial f = this.monic();

        // First check square-free
        ModPolynomial d = f.gcd(f.derivative());
        if (d.degree().greaterThan(LargeInteger.zero)) return false;
        //try different plugIns
        for(LargeInteger k = LargeInteger.zero; k.lessThan(p); k=k.add(1))
            if(f.plugIn(k).equalsTo(LargeInteger.zero)) return false;
        return true;
    }
    LargeInteger plugIn(LargeInteger I){
        LargeInteger result = LargeInteger.zero;
        for(LargeInteger i: coeffs)
            result = (LargeInteger) result.multiply(I).add(i);
        return (LargeInteger) result.mod(p);
    }

    public LargePolynomial toLargePolynomialSymmetric() {
        LargeList<LargeFraction> out = new LargeList<>();
        for(LargeInteger i: coeffs) out.add(new LargeFraction(i, LargeInteger.one));
        return new LargePolynomial(out);
    }
}