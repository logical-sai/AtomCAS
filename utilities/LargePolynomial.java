package com.AtomCAS.utilities;
public class LargePolynomial{
    LargeList<LargeFraction> coefficients; //higher degrees come first
    public LargePolynomial(){coefficients = new LargeList<>(); coefficients.add(LargeFraction.ZERO);}
    public LargePolynomial(LargeList<LargeFraction> coefficients){
        this.coefficients = new LargeList<>();
        for(LargeFraction I: coefficients) this.coefficients.add(I.clone());
        while(this.coefficients.size().greaterThan(LargeInteger.ONE) && this.coefficients.getFirst().equals(LargeInteger.ZERO))
            this.coefficients.removeFirst();
    }
    public LargePolynomial copy(){return new LargePolynomial(coefficients);}
    public static LargePolynomial monomial(LargeFraction coefficient, LargeInteger deg){
        LargeList<LargeFraction> coefficients = new LargeList<>();
        coefficients.add(coefficient);
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(deg); i = i.add(1)) coefficients.add(LargeFraction.ZERO);
        return new LargePolynomial(coefficients);
    }
    public LargePolynomial monic(){
        LargeList<LargeFraction> coefficients = new LargeList<>();
        LargeFraction coefficient = this.coefficients.getFirst();
        for(LargeFraction f: this.coefficients)
            coefficients.add((LargeFraction) f.divide(coefficient));
        return new LargePolynomial(coefficients);
    }
    public String toString(){
        if(isZero()) return "0";

        StringBuilder str = new StringBuilder();
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(coefficients.size()); i = i.add(1)){
            LargeFraction c = coefficients.get(i);
            if(c.equals(LargeInteger.ZERO)) continue;
            LargeInteger power = coefficients.size().subtract(1).subtract(i);
            if(i.equals(LargeInteger.ZERO)){
                if(power.equals(LargeInteger.ZERO)){
                    str.append(c);
                    break;
                }
                if(c.lessThan(LargeInteger.ZERO)) str.append("-");
                if(c.abs().doesntEqual(LargeInteger.ONE)) str.append(c.abs()).append("*x");
                else str.append("x");
                if(power.doesntEqual(LargeInteger.ONE)) str.append("^").append(power);
                continue;
            }
            if(c.lessThan(LargeInteger.ZERO)) str.append(" - ");
            else str.append(" + ");
            if(i.equals(coefficients.size().subtract(1))){
                str.append(c.abs());
                break;
            }
            if(c.abs().doesntEqual(LargeInteger.ONE)) str.append(c.abs()).append("*x");
            else str.append("x");
            if(power.doesntEqual(LargeInteger.ONE)) str.append("^").append(power);
        }
        return str.toString();
    }
    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("LargePolynomial does not support hashing");
//        int h = POLYNOMIAL_TAG;
//        for (LargeFraction coefficient : coefficients)
//            h = CASHash.mix(h, coefficient.hashCode());
//        return CASHash.finish(h);
    }
    public boolean isZero(){
        for(LargeFraction f: coefficients)
            if(f.doesntEqual(LargeInteger.ZERO))
                return false;
        return true;
    }
    public boolean isMonomial() {
        for(LargeInteger i = LargeInteger.ONE; i.lessThan(coefficients.size()); i = i.add(1))
            if(coefficients.get(i).doesntEqual(LargeInteger.ZERO))
                return false;
        return true;
    }
    public LargeInteger monomialDegree() {
        LargeInteger deg = degree();
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(coefficients.size()); i = i.add(1)) {
            if (coefficients.get(i).doesntEqual(LargeInteger.ZERO))
                return deg.subtract(i);
        }
        return LargeInteger.ZERO;
    }
    public LargeFraction monomialCoefficient() {
        for (LargeFraction c : coefficients)
            if (c.doesntEqual(LargeInteger.ZERO)) return c;
        return LargeFraction.ZERO;
    }
    public boolean equals(LargePolynomial p){
        if(degree().doesntEqual(p.degree()))
            return false;
        for(LargeInteger i = LargeInteger.ZERO; i.lesserThanOrEqualTo(degree()); i = i.add(1)){
            if(coefficients.get(i).doesntEqual(p.coefficients.get(i)))
                return false;
        }
        return true;
    }
    public static LargePolynomial ONE(){
        LargePolynomial P = new LargePolynomial();
        P = P.add(LargeFraction.ONE);
        return P;
    }
    public static LargePolynomial x(){return new LargePolynomial().add(LargeFraction.ONE).increaseDegree(LargeInteger.ONE);}
    public static LargePolynomial xMinus(LargeFraction n){
        LargeList<LargeFraction> list = new LargeList<>();
        list.add(LargeFraction.ONE);
        list.add(n.negate());
        return new LargePolynomial(list);
    }
    public LargePolynomial negate(){
        LargeList<LargeFraction> P = new LargeList<>();
        for(LargeFraction f: coefficients)
            P.add(f.negate());
        return new LargePolynomial(P);
    }
    public LargeInteger degree(){return coefficients.size().subtract(1);}
    public LargePolynomial increaseDegree(LargeInteger i){
        if(i.lessThan(LargeInteger.ZERO)) throw new IllegalArgumentException("Input means decreasing the degree");
        if(this.isZero()) return new LargePolynomial();
        LargeList<LargeFraction> out = new LargeList<>();
        for(LargeFraction I: coefficients) out.add(I);
        for(LargeInteger j = LargeInteger.ZERO; j.lessThan(i); j = j.add(1)) out.add(LargeFraction.ZERO);
        return new LargePolynomial(out);
    }
    public LargePolynomial add(LargeFraction other){
        LargeList<LargeFraction> out = new LargeList<>();
        for(LargeFraction I: coefficients) out.add(I);
        out.set(out.size().subtract(1), (LargeFraction) out.get(out.size().subtract(1)).add(other));
        return new LargePolynomial(out);
    }
    public LargePolynomial add(LargePolynomial other){
        LargeList<LargeFraction> out = new LargeList<>();
        if(this.coefficients.size().greaterThan(other.coefficients.size())){
            LargeInteger diff = this.coefficients.size().subtract(other.coefficients.size());
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    out.add((LargeFraction) this.coefficients.get(i).add(other.coefficients.get(i.subtract(diff))));
                else out.add(this.coefficients.get(i));
            }
            return new LargePolynomial(out);
        }else if(other.coefficients.size().greaterThan(this.coefficients.size())){
            LargeInteger diff = other.coefficients.size().subtract(this.coefficients.size());
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(other.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    out.add((LargeFraction) other.coefficients.get(i).add(this.coefficients.get(i.subtract(diff))));
                else out.add(other.coefficients.get(i));
            }
            return new LargePolynomial(out);
        }
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1))
            out.add((LargeFraction) this.coefficients.get(i).add(other.coefficients.get(i)));
        return new LargePolynomial(out);
    }
    public LargePolynomial subtract(LargeFraction other){
        LargeList<LargeFraction> out = new LargeList<>();
        for(LargeFraction I: coefficients) out.add(I);
        out.set(out.size().subtract(1), (LargeFraction) out.get(out.size().subtract(1)).subtract(other));
        return new LargePolynomial(out);
    }
    public LargePolynomial subtract(LargePolynomial other){
        LargeList<LargeFraction> out = new LargeList<>();
        if(this.coefficients.size().greaterThan(other.coefficients.size())){
            LargeInteger diff = this.coefficients.size().subtract(other.coefficients.size());
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    out.add((LargeFraction) this.coefficients.get(i).subtract(other.coefficients.get(i.subtract(diff))));
                else out.add(this.coefficients.get(i));
            }
            return new LargePolynomial(out);
        }else if(other.coefficients.size().greaterThan(this.coefficients.size())){
            LargeInteger diff = other.coefficients.size().subtract(this.coefficients.size());
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(other.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    out.add((LargeFraction) this.coefficients.get(i.subtract(diff)).subtract(other.coefficients.get(i)));
                else out.add(other.coefficients.get(i).negate());
            }
            return new LargePolynomial(out);
        }
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1))
            out.add((LargeFraction) this.coefficients.get(i).subtract(other.coefficients.get(i)));
        return new LargePolynomial(out);
    }
    public LargePolynomial multiply(LargeFraction other){
        if(other.equals(LargeInteger.ZERO) || isZero()) return new LargePolynomial();
        LargeList<LargeFraction> out = new LargeList<>();
        for(LargeFraction I: coefficients)
            out.add((LargeFraction) I.multiply(other));
        return new LargePolynomial(out);
    }
    public LargePolynomial multiply(LargePolynomial other) {
        if (this.isZero() || other.isZero()) return new LargePolynomial();
        if (this.isMonomial())
            return other.multiply(this.monomialCoefficient()).increaseDegree(this.monomialDegree());
        if (other.isMonomial())
            return this.multiply(other.monomialCoefficient()).increaseDegree(other.monomialDegree());
        // existing generic multiply
        LargeList<LargePolynomial> polys = new LargeList<>();
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1))
            polys.add(other.multiply(coefficients.get(i)).increaseDegree(this.degree().subtract(i)));
        LargePolynomial result = new LargePolynomial();
        for (LargePolynomial P : polys) result = result.add(P);
        return result;
    }
    public LargePolynomial square(){return this.multiply(this);}
    public LargePolynomial monomialPower(LargeInteger I){
        if(I.sign().equals(LargeInteger.NEGATIVE_ONE)) throw new ArithmeticException("Rational functions not implemented yet");
        if(I.equals(LargeInteger.ZERO))
            return LargePolynomial.ONE();
        LargePolynomial P = new LargePolynomial();
        P = P.add(LargeFraction.ONE);
        LargeInteger expTemp = I.clone();
        LargePolynomial basTemp = new LargePolynomial(this.coefficients);
        while(true){
            if(expTemp.isOdd()){
                P = P.multiply(basTemp);
                if(expTemp.equals(LargeInteger.ONE)) break;
            }
            basTemp = basTemp.square();
            expTemp = expTemp.shiftRight(1);
        }
        return P;
    }
    public LargePolynomial longDivision(LargePolynomial divisor){return divideAndRemainder(divisor)[0];}
    public LargePolynomial remainder(LargePolynomial divisor){return divideAndRemainder(divisor)[1];}
    private LargePolynomial[] divideAndRemainder(LargePolynomial divisor) {
        if (divisor.isZero()) throw new ArithmeticException("Cannot divide by 0");
        LargePolynomial remainder = this.copy();
        LargePolynomial quotient = new LargePolynomial();
        LargeInteger divisorDegree = divisor.degree();
        LargeFraction divisorLead = divisor.coefficients.getFirst();
        while (!remainder.isZero() && remainder.degree().greaterThanOrEqualTo(divisorDegree)) {
            LargeInteger degreeDiff = remainder.degree().subtract(divisorDegree);
            LargeFraction coefficient = (LargeFraction) remainder.coefficients.getFirst().divide(divisorLead);
            LargePolynomial term = LargePolynomial.monomial(coefficient, degreeDiff);
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
        if (n.equals(LargeInteger.ZERO))
            throw new IllegalArgumentException("Cyclotomic index must be positive");
        if (n.equals(LargeInteger.ONE))
            return LargePolynomial.xMinus(LargeFraction.ONE);
        if(n.equals(LargeInteger.TWO))
            return LargePolynomial.xMinus(LargeFraction.NEGATIVE_ONE);
        if(n.lessThan(LargeInteger.ZERO))
            return cyclotomic(n.negate().multiply(2));
        if (n.isProbablyPrime()){
            LargeList<LargeFraction> L = new LargeList<>();
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(n); i=i.add(1))
                L.add(LargeFraction.ONE);
            return new LargePolynomial(L);
        }
        // Φ_2m(x) = Φ_m(-x), m odd
        if (n.isEven()) {
            LargeInteger m = n.shiftRight(1);
            if (m.isOdd())
                return cyclotomic(m).compose(x().negate());
        }
        LargeMap<LargeInteger, LargeFraction> factors = n.factor();
        LargePolynomial repeatedPrimeCase = tryRepeatedPrimeShortcut(n, factors);
        if (repeatedPrimeCase != null)
            return repeatedPrimeCase;
        LargePolynomial P = monomial(LargeFraction.ONE, n).subtract(LargeFraction.ONE);
        LargeList<LargeInteger> N = n.divisors();
        N.removeLast();
        for(LargeInteger I: N)
            P = P.longDivision(cyclotomic(I));
        return P;
    }
    private static LargePolynomial tryRepeatedPrimeShortcut(LargeInteger n,
                                                            LargeMap<LargeInteger, LargeFraction> factors) {
        LargeInteger rad = LargeInteger.ONE;
        for (LargeInteger p : factors.keys()) {
            if (p.equals(LargeInteger.NEGATIVE_ONE)) continue;
            rad = rad.multiply(p);
        }
        if (rad.equals(n)) return null; // squarefree, no help
        LargeInteger step = n.divide(rad);
        return cyclotomic(rad).composeXPower(step);
    }
    public ScalarAtom evaluateAt(ScalarAtom s){
        ScalarAtom result = LargeInteger.ZERO;
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(coefficients.size()); i = i.add(1))
            result = result.multiply(s).add(coefficients.get(i));
        return Parser.parse(result.toString());
    }
    public LargePolynomial composeXPower(LargeInteger k) {
        if (k.lesserThanOrEqualTo(LargeInteger.ZERO))
            throw new IllegalArgumentException("exponent must be positive");
        LargeList<LargeFraction> out = new LargeList<>();
        LargeInteger newDegree = this.degree().multiply(k);
        for (LargeInteger e = newDegree; e.greaterThanOrEqualTo(LargeInteger.ZERO); e = e.subtract(1)) {
            if (e.mod(k).equals(LargeInteger.ZERO)) {
                LargeInteger oldPower = e.divide(k);
                LargeInteger index = this.degree().subtract(oldPower);
                out.add(this.coefficients.get(index));
            }else out.add(LargeFraction.ZERO);
            if (e.equals(LargeInteger.ZERO)) break;
        }

        return new LargePolynomial(out);
    }
    public LargePolynomial compose(LargePolynomial inner){
        LargePolynomial result = new LargePolynomial();
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(coefficients.size()); i = i.add(1))
            result = result.multiply(inner).add(coefficients.get(i));
        return result;
    }
    public static LargePolynomial annihilatingPolynomial(ScalarAtom s) throws CloneNotSupportedException {
        if(!s.isAlgebraic()) throw new IllegalArgumentException("Transcendental numbers do not satisfy any polynomials");
        LargePolynomial P = x();
        switch (s) {
            case LargeInteger I -> {return P.subtract(new LargeFraction(I, LargeInteger.ONE));}
            case LargeFraction F -> {
                P = P.multiply(new LargeFraction(F.denominator, LargeInteger.ONE));
                P = P.add(new LargeFraction(F.numerator, LargeInteger.NEGATIVE_ONE));
                return P.primitivePart();
            }case LargePowerMonomial L -> {
                //x = (-1)^(p/q) * b^(r/s)
                LargeInteger I = L.exponent2.denominator.lcm(L.exponent.denominator);
                P = P.monomialPower(I);
                LargePowerMonomial M = L.power(I);
                if (M.exponent2.equals(LargeInteger.ONE)) P = P.add(M.base);
                else P = P.add(M.base.negate());
                return P.primitivePart();
            }case RadicalTerm R -> {return annihilatingPolynomial(new RadicalTermSum().add(R));}
            case RadicalTermSum R -> {
                if(R.sum.isEmpty()) return annihilatingPolynomial(R.constant);
                LargeList<LargePowerMonomial> basis = new LargeList<>();
                basis.add(LargePowerMonomial.ONE);
                /*
                 * Roots of unity basis:
                 * If any term contains (-1)^(p/q), include the cyclotomic basis.
                 */
                LargeList<LargeInteger> unityDenominators = new LargeList<>();

                for (LargePowerMonomial m : R.sum.keys())
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
                        boolean contains = false;
                        for(LargePowerMonomial m: next){
                            if(m.equals(zetaPower)){
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
                for (LargePowerMonomial m : R.sum.keys()) {
                    LargeInteger order = m.exponent.denominator;
                    if (order.equals(LargeInteger.ONE)) continue;
                    LargePowerMonomial g = new LargePowerMonomial(m.base, new LargeFraction(LargeInteger.ONE, order));
                    boolean contains = false;
                    for (LargePowerMonomial b : gens)
                        if (b.equals(g)){
                            contains = true;
                            break;
                        }
                    if(!contains) gens.add(g);
                }
                //extract the prime factors
                LargeList<LargePowerMonomial> newGens = new LargeList<>();
                for (LargePowerMonomial g : gens) {
                    LargeMap<LargeInteger, LargeFraction> pr = g.factor();
                    for (LargeInteger I : pr.keys()) {
                        if (I.equals(LargeInteger.NEGATIVE_ONE))
                            continue;
                        if (pr.get(I).denominator.equals(LargeInteger.ONE))
                            continue; // rational contribution; not a radical generator
                        LargePowerMonomial gen = new LargePowerMonomial(new LargeFraction(I, LargeInteger.ONE), new LargeFraction(LargeInteger.ONE, pr.get(I).denominator));
                        boolean contains = false;
                        for (LargePowerMonomial m : newGens)
                            if(m.equals(gen)){
                                contains = true;
                                break;
                            }
                        if(!contains) newGens.add(gen);
                    }
                }
                //merge same bases
                LargeMap<LargeFraction, LargeInteger> baseToDenominator = new LargeMap<>();
                for (LargePowerMonomial g : newGens) {
                    if (g.exponent.denominator.equals(LargeInteger.ONE))
                        continue;
                    if (baseToDenominator.containsKey(g.base)) {
                        LargeInteger oldDenominator = baseToDenominator.get(g.base);
                        baseToDenominator.put(g.base, oldDenominator.lcm(g.exponent.denominator));
                    } else
                        baseToDenominator.put(g.base, g.exponent.denominator);
                }
                LargeList<LargePowerMonomial> merged = new LargeList<>();
                for (LargeFraction base : baseToDenominator.keys()) {
                    LargeInteger denominator = baseToDenominator.get(base);
                    merged.add(new LargePowerMonomial(base, new LargeFraction(LargeInteger.ONE, denominator)));
                }
                gens = merged;
                for (LargePowerMonomial g : gens) {
                    LargeList<LargePowerMonomial> next = new LargeList<>();
                    LargeInteger order = g.exponent.denominator;
                    for (LargePowerMonomial oldBasis : basis) {
                        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(order); i = i.add(1)) {
                            LargePowerMonomial powerOfG = new LargePowerMonomial(g.base, new LargeFraction(i, order));
                            LargePowerMonomial product = (LargePowerMonomial) oldBasis.multiply(powerOfG);
                            boolean contains = false;
                            for(LargePowerMonomial m: next){
                                if(m.equals(product)){
                                    contains = true;
                                    break;
                                }
                            }
                            if(!contains) next.add(product);
                        }
                    }
                    LargeList<LargePowerMonomial> reordered = new LargeList<>();
                    reordered.add(LargePowerMonomial.ONE);
                    for (LargePowerMonomial b : next) {
                        if (b.doesntEqual(LargeInteger.ONE))
                            reordered.add(b);
                    }
                    basis = reordered;
                }
                LargeMatrix M = new LargeMatrix();
                for (LargePowerMonomial b : basis) {
                    RadicalTermSum product = (RadicalTermSum) s.multiply(b);
                    LargeVector colAsRow = new LargeVector(basis.size());
                    if (product.constant.doesntEqual(LargeInteger.ZERO)) {
                        LargeFraction old = colAsRow.get(LargeInteger.ZERO);
                        colAsRow.set(LargeInteger.ZERO, (LargeFraction) old.add(product.constant));
                    }
                    for (LargePowerMonomial m : product.sum.keys()) {
                        LargeInteger index = LargeInteger.NEGATIVE_ONE;
                        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(basis.size()); i = i.add(1)){
                            if(basis.get(i).equals(m)){
                                index = i;
                                break;
                            }
                        }
                        if (index.equals(LargeInteger.NEGATIVE_ONE))
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
                if(PS.equals(xMinus(LargeFraction.ONE))) return PM;
                return null;
            }
            default -> throw new UnsupportedOperationException("Annihilating polynomial does not support this type yet");
        }
    }
    public static LargePolynomial minimalPolynomial(ScalarAtom s) throws CloneNotSupportedException {
        if (!s.isAlgebraic())
            throw new IllegalArgumentException("Transcendental numbers do not satisfy any polynomials");
        LargePolynomial ann = annihilatingPolynomial(s);
        assert ann != null;
        LargeList<LargePolynomial> LQ = ann.factorOverRationals();
        for(LargePolynomial p: LQ){
            if(p.evaluateAt(s).equals(LargeInteger.ZERO))
                return p.primitivePart();
        }
        throw new UnsupportedOperationException("Minimal polynomial does not support this type yet");
    }
    private LargePolynomial extractLinearFactor(){
        LargePolynomial P = this.primitivePart();
        LargeList<LargeFraction> rationals = new LargeList<>();
        LargeList<LargeInteger> top = new LargeList<>();
        for(LargeInteger i = P.coefficients.size().subtract(1); i.greaterThan(LargeInteger.ZERO); i=i.subtract(1)){
            if(P.coefficients.get(i).equals(LargeInteger.ZERO)) continue;
            top = P.coefficients.get(i).numerator.divisors();
            break;
        }
        LargeList<LargeInteger> bottom = P.coefficients.getFirst().numerator.divisors();
        for(LargeInteger i: top){
            for(LargeInteger j: bottom){
                rationals.add(new LargeFraction(i, j));
                rationals.add(new LargeFraction(i.negate(), j));
            }
        }
        rationals.add(LargeFraction.ZERO);
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
        for (LargeInteger i = LargeInteger.ONE; i.lessThan(P.coefficients.size()); i = i.add(1)) {
            if (P.coefficients.get(i).equals(LargeInteger.ZERO)) continue;
            // Since primitivePart guarantees integer coefficients:
            BF = P.coefficients.get(i).numerator.abs().factor();
            break;
        }
        for (LargeInteger p : BF.keys()) {
            if (p.lesserThanOrEqualTo(LargeInteger.ONE)) continue;
            // p must not divide leading coefficient
            if (P.coefficients.getFirst().numerator.mod(p).equals(LargeInteger.ZERO)) continue;
            boolean insufficient = false;
            // p must divide every coefficient except the leading ONE
            for (LargeInteger i = LargeInteger.ONE;
                 i.lessThan(P.coefficients.size());
                 i = i.add(1)) {
                if (P.coefficients.get(i).numerator.mod(p).doesntEqual(LargeInteger.ZERO)) {
                    insufficient = true;
                    break;
                }
            }
            if (insufficient) continue;
            // p^2 must not divide the constant term
            if (P.coefficients.getLast().numerator.mod(p.square()).doesntEqual(LargeInteger.ZERO))
                return true;
        }
        return false; //does not necessarily mean reducible
    }
    public LargeList<LargePolynomial> factorOverRationals() throws CloneNotSupportedException {
        LargeList<LargePolynomial> factors = new LargeList<>();
        LargeList<LargePolynomial> work = new LargeList<>();
        LargePolynomial P = this.primitivePart();
        LargePolynomial GC = P.gcd(P.derivative());
        LargePolynomial factor = GC.longDivision(GC.primitivePart());
        LargePolynomial factor2 = P.longDivision(GC).longDivision(P.longDivision(GC).primitivePart());
        if(!GC.equals(ONE())){
            LargePolynomial product = factor.multiply(factor2);
            if(!product.equals(ONE())) factors.add(product);
            work.add(GC.primitivePart());
            work.add(P.longDivision(GC.primitivePart()));
        }else work.add(P);
        while (work.size().greaterThan(LargeInteger.ZERO)) {
            LargePolynomial F = work.getLast();
            work.removeLast();

            if (F.degree().lesserThanOrEqualTo(LargeInteger.ONE)) {
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
            if (candidate != null && candidate.degree().greaterThan(LargeInteger.ZERO) && candidate.degree().lessThan(F.degree()) && F.remainder(candidate).isZero()) {
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
    private boolean irreducibleBySomeModPrimeTest() throws CloneNotSupportedException {
        LargePolynomial P = this.primitivePart();
        for (int p0 : LargeInteger.SMALL_PRIMES) {
            LargeInteger p = LargeInteger.valueOf(p0);
            // skip primes dividing leading coefficient
            if (P.coefficients.getFirst().numerator.mod(p).equals(LargeInteger.ZERO))
                continue;
            ModPolynomial MP = ModPolynomial.fromLargePolynomial(P, p);
            // skip inseparable/bad reductions
            if (MP.gcd(MP.derivative()).doesntEqual(ModPolynomial.one(p)))
                continue;

            if (MP.isIrreducible())
                return true;
        }
        return false;
    }
    private LargePolynomial tryFactorFromModPrime() throws CloneNotSupportedException {
        for(int i: LargeInteger.SMALL_PRIMES){
            LargeInteger I = LargeInteger.valueOf(i);
            ModPolynomial MP = ModPolynomial.fromLargePolynomial(this, I);
            //ensure MP is squareFree
            ModPolynomial GP = MP.gcd(MP.derivative());
            if(GP.doesntEqual(ModPolynomial.one(I))) continue;
            //test all numbers up to I
            ModPolynomial product = ModPolynomial.one(I);
            for(LargeInteger k = LargeInteger.ZERO; k.lessThan(I); k = k.add(1)){
                LargeInteger J = MP.evaluateAt(k);
                if(J.equals(LargeInteger.ZERO)){
                    LargeList<LargeInteger> lt = new LargeList<>();
                    lt.add(LargeInteger.ONE);
                    lt.add(k.negate());
                    product = product.multiply(new ModPolynomial(lt, I));
                }
            }
            if(product.doesntEqual(ModPolynomial.one(I))){
                LargeList<LargeFraction> lt = new LargeList<>();
                for(LargeInteger C: product.coefficients) lt.add(new LargeFraction(C, LargeInteger.ONE));
                return new LargePolynomial(lt);
            }
        }
        return null;
    }
    private LargePolynomial primitivePart() {
        LargeFraction L = LargeFraction.ZERO;
        for(LargeFraction f: coefficients) L = L.gcd(f);
        return this.multiply(L.reciprocal()).multiply((LargeFraction) coefficients.getFirst().sign());
    }

    public LargePolynomial derivative(){
        LargeList<LargeFraction> coefficients = new LargeList<>();
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(degree()); i=i.add(1))
            coefficients.add((LargeFraction) this.coefficients.get(i).multiply(degree().subtract(i)));
        return new LargePolynomial(coefficients);
    }
}
class ModPolynomial {
    // coefficients high degree first, matching LargePolynomial
    LargeList<LargeInteger> coefficients;
    LargeInteger p;
    public ModPolynomial(LargeList<LargeInteger> coefficients, LargeInteger p) {
        if (!p.isProbablyPrime())
            throw new IllegalArgumentException("Modulus must be prime for field arithmetic");
        this.p = p;
        this.coefficients = new LargeList<>();
        for (LargeInteger c : coefficients)
            this.coefficients.add(c.mod(p));
        trimLeadingZeros();
    }
    private void trimLeadingZeros(){
        while(coefficients.size().greaterThan(LargeInteger.ONE) && coefficients.getFirst().equals(LargeInteger.ZERO))
            coefficients.removeFirst();
    }
    private static LargeInteger mod(LargeInteger a, LargeInteger p) {
        LargeInteger r = a.mod(p);
        return r.lessThan(LargeInteger.ZERO) ? r.add(p) : r;
    }
    private static LargeList<LargeInteger> trimNormalize(LargeList<LargeInteger> a, LargeInteger p) {
        LargeInteger start = LargeInteger.ZERO;
        while (start.lessThan(a.size().subtract(1)) && mod(a.get(start), p).equals(LargeInteger.ZERO))
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
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(coefficients.size()); i=i.add(1))
            out.add(mod(coefficients.get(i).multiply(inv), p));
        return new ModPolynomial(out, p);
    }
    public LargeInteger degree() {return coefficients.size().subtract(1);}
    public boolean isZero() {return coefficients.size().equals(LargeInteger.ONE) && coefficients.getFirst().equals(LargeInteger.ZERO);}
    public boolean equals(ModPolynomial other) {
        if (this.p.doesntEqual(other.p)) return false;
        if (this.coefficients.size().doesntEqual(other.coefficients.size())) return false;
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(coefficients.size()); i=i.add(1))
            if (this.coefficients.get(i).doesntEqual(other.coefficients.get(i)))
                return false;
        return true;
    }
    public boolean doesntEqual(ModPolynomial other) {return !equals(other);}
    public LargeInteger lead() {return coefficients.getFirst();}
    public static ModPolynomial one(LargeInteger p) {
        LargeList<LargeInteger> coefficients = new LargeList<>();
        coefficients.add(LargeInteger.ONE);
        return new ModPolynomial(coefficients, p);
    }
    static ModPolynomial fromLargePolynomial(LargePolynomial f, LargeInteger p) {
        LargeInteger n = new LargeInteger(f.coefficients.size().toString());
        LargeList<LargeInteger> a = new LargeList<>();
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(n); i=i.add(1)){
            LargeFraction c = f.coefficients.get(i);
            if (c.denominator.doesntEqual(LargeInteger.ONE))
                throw new IllegalArgumentException("Expected integer coefficients");
            a.add(new LargeInteger(c.numerator.mod(p).toString()));
        }
        return new ModPolynomial(a, p);
    }
    static LargeInteger invMod(LargeInteger a, LargeInteger p) {
        a = mod(a, p);
        if (a.equals(LargeInteger.ZERO)) throw new ArithmeticException("No inverse for 0");
        LargeInteger t = LargeInteger.ZERO, newT = LargeInteger.ONE;
        LargeInteger r = p, newR = a;

        while (newR.doesntEqual(LargeInteger.ZERO)) {
            LargeInteger q = r.divide(newR);
            LargeInteger tempT = t.subtract(q.multiply(newT));
            t = newT;
            newT = tempT;
            LargeInteger tempR = r.subtract(q.multiply(newR));
            r = newR;
            newR = tempR;
        }

        if (r.greaterThan(LargeInteger.ONE)) throw new ArithmeticException("Not invertible");
        return mod(t, p);
    }
    ModPolynomial gcd(ModPolynomial other) throws CloneNotSupportedException {
        ModPolynomial a = this;
        ModPolynomial b = other;
        while (!b.isZero()) {
            ModPolynomial r = a.remainder(b);
            a = b;
            b = r;
        }
        return a.monic();
    }
    ModPolynomial multiply(ModPolynomial other) {
        if (this.p.doesntEqual(other.p))
            throw new IllegalArgumentException("Different moduli");
        LargeList<LargeInteger> out = new LargeList<>();
        for(LargeInteger i = LargeInteger.ZERO; i.lesserThanOrEqualTo(this.coefficients.size().add(other.coefficients.size()).subtract(LargeInteger.TWO)); i=i.add(1))
            out.add(LargeInteger.ZERO);
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i=i.add(1))
            for (LargeInteger j = LargeInteger.ZERO; j.lessThan(other.coefficients.size()); j=j.add(1))
                out.set(i.add(j), mod(out.get(i.add(j)).add(this.coefficients.get(i).multiply(other.coefficients.get(j))), p));
        return new ModPolynomial(out, p);
    }
    private ModPolynomial[] divideAndRemainder(ModPolynomial divisor) throws CloneNotSupportedException {
        if (this.p.doesntEqual(divisor.p))
            throw new IllegalArgumentException("Different moduli");
        if (divisor.isZero())
            throw new ArithmeticException("Cannot divide by ZERO polynomial");

        LargeList<LargeInteger> rem = this.coefficients.copy();
        LargeList<LargeInteger> quo = new LargeList<>();

        LargeInteger qSize = this.degree().subtract(divisor.degree()).add(1);
        if (qSize.lesserThanOrEqualTo(LargeInteger.ZERO)) {
            quo.add(LargeInteger.ZERO);
            return new ModPolynomial[]{
                    new ModPolynomial(quo, p),
                    new ModPolynomial(rem, p)
            };
        }

        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(qSize); i = i.add(1))
            quo.add(LargeInteger.ZERO);

        LargeInteger divisorDeg = divisor.degree();
        LargeInteger divisorLeadInv = invMod(divisor.lead(), p);

        while (rem.size().subtract(1).greaterThanOrEqualTo(divisorDeg)
                && !(rem.size().equals(LargeInteger.ONE) && rem.getFirst().equals(LargeInteger.ZERO))) {

            LargeInteger remDeg = rem.size().subtract(1);
            LargeInteger degreeDiff = remDeg.subtract(divisorDeg);

            LargeInteger scale = mod(rem.getFirst().multiply(divisorLeadInv), p);

            LargeInteger qIndex = qSize.subtract(1).subtract(degreeDiff);
            quo.set(qIndex, scale);

            for (LargeInteger i = LargeInteger.ZERO; i.lessThan(divisor.coefficients.size()); i = i.add(1)) {
                rem.set(i, mod(rem.get(i).subtract(scale.multiply(divisor.coefficients.get(i))), p));
            }
            rem = trimNormalize(rem, p);
        }
        return new ModPolynomial[]{
                new ModPolynomial(quo, p),
                new ModPolynomial(rem, p)
        };
    }
    public ModPolynomial remainder(ModPolynomial divisor) throws CloneNotSupportedException {return divideAndRemainder(divisor)[1];}
    ModPolynomial derivative() {
        LargeList<LargeInteger> out = new LargeList<>();
        LargeInteger n = degree();
        if (n.equals(LargeInteger.ZERO)){
            out.add(LargeInteger.ZERO);
            return new ModPolynomial(out, p);
        }
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(n); i=i.add(1)){
            LargeInteger power = n.subtract(i);
            out.add(mod(coefficients.get(i).multiply(power), p));
        }
        return new ModPolynomial(out, p);
    }
    boolean isIrreducible() throws CloneNotSupportedException {
        if (degree().lesserThanOrEqualTo(LargeInteger.ZERO)) return false;
        if (degree().equals(LargeInteger.ONE)) return true;

        ModPolynomial f = this.monic();

        // First check square-free
        ModPolynomial d = f.gcd(f.derivative());
        if (d.degree().greaterThan(LargeInteger.ZERO)) return false;
        //try different plugIns
        for(LargeInteger k = LargeInteger.ZERO; k.lessThan(p); k=k.add(1))
            if(f.evaluateAt(k).equals(LargeInteger.ZERO)) return false;
        return true;
    }
    LargeInteger evaluateAt(LargeInteger I){
        LargeInteger result = LargeInteger.ZERO;
        for(LargeInteger i: coefficients)
            result = result.multiply(I).add(i);
        return result.mod(p);
    }
}