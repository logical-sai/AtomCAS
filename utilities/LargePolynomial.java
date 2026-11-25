package com.AtomCAS.utilities;
public final class LargePolynomial extends Polynomial {
    final LargeList<LargeFraction> coefficients; //higher degrees come first
    public static LargePolynomial ZERO = new LargePolynomial(LargeList.of(LargeFraction.ZERO));
    public static LargePolynomial ONE = new LargePolynomial(LargeList.of(LargeFraction.ONE));
    public LargePolynomial(LargeList<LargeFraction> coefficients){
        while(coefficients.size().greaterThan(LargeInteger.ONE)
                && coefficients.getFirst().isZero())
            coefficients.removeFirst();
        this.coefficients = coefficients;
    }
    @Override
    public LargePolynomial clone() {
        LargeList<LargeFraction> copy = new LargeList<>();
        for (LargeFraction coefficient : this.coefficients)
            copy.add(coefficient.clone());
        return new LargePolynomial(copy);
    }
    public static LargePolynomial monomial(LargeFraction coefficient, LargeInteger deg) {
        LargeList<LargeFraction> coefficients = new LargeList<>();
        coefficients.add(coefficient);
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(deg); i = i.add(1))
            coefficients.add(LargeFraction.ZERO);
        return new LargePolynomial(coefficients);
    }
    public LargePolynomial monic(){
        LargeList<LargeFraction> coefficients = new LargeList<>();
        LargeFraction coefficient = this.coefficients.getFirst();
        for(LargeFraction f: this.coefficients)
            coefficients.add(f.divide(coefficient));
        return new LargePolynomial(coefficients);
    }
    public String toString(){
        if(isZero()) return "0";
        StringBuilder str = new StringBuilder();
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(coefficients.size()); i = i.add(1)){
            LargeFraction c = coefficients.get(i);
            if(c.isZero()) continue;
            LargeInteger power = coefficients.size().subtract(1).subtract(i);
            if(i.isZero()){
                if(power.isZero()){
                    str.append(c);
                    break;
                }
                if(c.isNegative()) str.append("-");
                if(c.abs().doesntEqual(LargeInteger.ONE)) str.append(c.abs()).append("*x");
                else str.append("x");
                if(power.doesntEqual(LargeInteger.ONE)) str.append("^").append(power);
                continue;
            }
            if(c.isNegative()) str.append(" - ");
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
    public static LargePolynomial x(){return ONE.increaseDegree(LargeInteger.ONE);}
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
        if(i.isNegative()) throw new IllegalArgumentException("Input means decreasing the degree");
        if(this.isZero()) return ZERO;
        LargeList<LargeFraction> out = new LargeList<>();
        out.addAll(coefficients);
        for(LargeInteger j = LargeInteger.ZERO; j.lessThan(i); j = j.add(1)) out.add(LargeFraction.ZERO);
        return new LargePolynomial(out);
    }
    public LargePolynomial add(LargeFraction other){
        LargeList<LargeFraction> out = new LargeList<>();
        out.addAll(coefficients);
        out.set(out.size().subtract(1), out.get(out.size().subtract(1)).add(other));
        return new LargePolynomial(out);
    }
    public LargePolynomial add(LargePolynomial other){
        LargeList<LargeFraction> out = new LargeList<>();
        if(coefficients.size().greaterThan(other.coefficients.size())){
            LargeInteger diff = this.coefficients.size().subtract(other.coefficients.size());
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    out.add(this.coefficients.get(i).add(other.coefficients.get(i.subtract(diff))));
                else out.add(this.coefficients.get(i));
            }
            return new LargePolynomial(out);
        }else if(other.coefficients.size().greaterThan(this.coefficients.size())){
            LargeInteger diff = other.coefficients.size().subtract(this.coefficients.size());
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(other.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    out.add(other.coefficients.get(i).add(this.coefficients.get(i.subtract(diff))));
                else out.add(other.coefficients.get(i));
            }
            return new LargePolynomial(out);
        }
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1))
            out.add(this.coefficients.get(i).add(other.coefficients.get(i)));
        return new LargePolynomial(out);
    }
    public LargePolynomial subtract(LargeFraction other){
        LargeList<LargeFraction> out = new LargeList<>();
        for(LargeFraction I: coefficients) out.add(I);
        out.set(out.size().subtract(1),
                out.get(out.size().subtract(1)).subtract(other));
        return new LargePolynomial(out);
    }
    public LargePolynomial subtract(LargePolynomial other){
        LargeList<LargeFraction> out = new LargeList<>();
        if(this.coefficients.size().greaterThan(other.coefficients.size())){
            LargeInteger diff = this.coefficients.size().subtract(other.coefficients.size());
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    out.add(this.coefficients.get(i).subtract(other.coefficients.get(i.subtract(diff))));
                else out.add(this.coefficients.get(i));
            }
            return new LargePolynomial(out);
        }else if(other.coefficients.size().greaterThan(this.coefficients.size())){
            LargeInteger diff = other.coefficients.size().subtract(this.coefficients.size());
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(other.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    out.add(this.coefficients.get(i.subtract(diff)).subtract(other.coefficients.get(i)));
                else out.add(other.coefficients.get(i).negate());
            }
            return new LargePolynomial(out);
        }
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1))
            out.add(this.coefficients.get(i).subtract(other.coefficients.get(i)));
        return new LargePolynomial(out);
    }
    public LargePolynomial multiply(LargeFraction other){
        if(other.isZero() || isZero()) return ZERO;
        LargeList<LargeFraction> out = new LargeList<>();
        for(LargeFraction I: coefficients)
            out.add(I.multiply(other));
        return new LargePolynomial(out);
    }
    public LargePolynomial multiply(LargePolynomial other) {
        if (this.isZero() || other.isZero()) return ZERO;
        if (this.isMonomial())
            return other.multiply(this.monomialCoefficient()).increaseDegree(this.monomialDegree());
        if (other.isMonomial())
            return this.multiply(other.monomialCoefficient()).increaseDegree(other.monomialDegree());
        //generic multiply
        LargeList<LargePolynomial> polys = new LargeList<>();
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1))
            polys.add(other.multiply(coefficients.get(i)).increaseDegree(this.degree().subtract(i)));
        LargePolynomial result = ZERO;
        for (LargePolynomial P : polys) result = result.add(P);
        return result;
    }
    public LargePolynomial square() {
        if (isZero())
            return ZERO;
        if (isMonomial())
            return monomial(monomialCoefficient().square(), monomialDegree().shiftLeft(1));
        LargeInteger size = coefficients.size();
        LargeInteger outSize = size.shiftLeft(1).subtract(1);
        LargeList<LargeFraction> out = new LargeList<>(outSize);
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(outSize); i = i.add(1))
            out.add(LargeFraction.ZERO);
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(size); i = i.add(1)) {
            LargeFraction a = coefficients.get(i);
            if (a.isZero()) continue;
            // Diagonal product
            LargeInteger diagonal = i.shiftLeft(1);
            out.set(diagonal, out.get(diagonal).add(a.square()));
            // Off-diagonal products
            for (LargeInteger j = i.add(1); j.lessThan(size); j = j.add(1)) {
                LargeFraction b = coefficients.get(j);
                if (b.isZero()) continue;
                LargeInteger index = i.add(j);
                out.set(index, out.get(index).add(a.multiply(b).multiply(2)));
            }
        }
        return new LargePolynomial(out);
    }
    public LargePolynomial power(LargeInteger I) {
        if(I.isNegative())
            throw new ArithmeticException("Rational functions not implemented yet");
        if(I.isZero()) return ONE;
        LargePolynomial P = ONE;
        LargePolynomial basTemp = new LargePolynomial(this.coefficients);
        for (LargeInteger expTemp = I.clone(); ; expTemp = expTemp.shiftRight(1)) {
            if(expTemp.isOdd()) {
                P = P.multiply(basTemp);
                if(expTemp.isOne()) return P;
            }
            basTemp = basTemp.square();
        }
    }
    public LargePolynomial divide(LargePolynomial divisor){return divideAndRemainder(divisor)[0];}
    public LargePolynomial remainder(LargePolynomial divisor){return divideAndRemainder(divisor)[1];}
    public LargePolynomial[] divideAndRemainder(LargePolynomial divisor) {
        if (divisor.isZero()) throw new ArithmeticException("Cannot divide by 0");
        LargePolynomial remainder = clone();
        LargePolynomial quotient = ZERO;
        LargeInteger divisorDegree = divisor.degree();
        LargeFraction divisorLead = divisor.coefficients.getFirst();
        while (!remainder.isZero() && remainder.degree().greaterThanOrEqualTo(divisorDegree)) {
            LargeInteger degreeDiff = remainder.degree().subtract(divisorDegree);
            LargeFraction coefficient = remainder.coefficients.getFirst().divide(divisorLead);
            LargePolynomial term = LargePolynomial.monomial(coefficient, degreeDiff);
            quotient = quotient.add(term);
            remainder = remainder.subtract(term.multiply(divisor));
        }
        return new LargePolynomial[]{quotient, remainder};
    }
    public LargePolynomial gcd(LargePolynomial other) {
        LargePolynomial a = clone();
        LargePolynomial b = other.clone();
        if (a.isZero()) return b.monic();
        if (b.isZero()) return a.monic();
        while (!b.isZero()) {
            LargePolynomial r = a.remainder(b);
            a = b;
            b = r;
        }
        return a;
    }
    public static LargePolynomial cyclotomic(LargeInteger n) {
        if (n.isZero())
            throw new IllegalArgumentException("Cyclotomic index must be positive");
        if (n.isOne())
            return LargePolynomial.xMinus(LargeFraction.ONE);
        if(n.equals(LargeInteger.TWO))
            return LargePolynomial.xMinus(LargeFraction.NEGATIVE_ONE);
        if(n.isNegative())
            return cyclotomic(n.negate().shiftLeft(1));
        if (n.isProbablyPrime()) {
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
        LargeInteger rad = LargeInteger.ONE;
        for (LargeInteger p : factors.keys()) {
            if (p.equals(LargeInteger.NEGATIVE_ONE)) continue;
            rad = rad.multiply(p);
        }
        if (!rad.equals(n)) {
            LargeInteger step = n.divide(rad);
            return cyclotomic(rad).composeXPower(step);
        }
        LargePolynomial P = monomial(LargeFraction.ONE, n).subtract(LargeFraction.ONE);
        LargeList<LargeInteger> N = n.divisors();
        N.removeLast();
        for(LargeInteger I: N)
            P = P.divide(cyclotomic(I));
        return P;
    }
    public ScalarAtom evaluateAt(ScalarAtom s) {
        ScalarAtom result = LargeInteger.ZERO;
        for(LargeFraction coefficient: this.coefficients)
            result = result.multiply(s).add(coefficient);
        return Parser.parse(result.toString());
    }
    public LargePolynomial composeXPower(LargeInteger k) {
        if (k.lesserThanOrEqualTo(LargeInteger.ZERO))
            throw new IllegalArgumentException("exponent must be positive");
        LargeList<LargeFraction> out = new LargeList<>();
        LargeInteger newDegree = this.degree().multiply(k);
        for (LargeInteger e = newDegree; e.greaterThanOrEqualTo(LargeInteger.ZERO); e = e.subtract(1)) {
            if (e.mod(k).isZero()) {
                LargeInteger oldPower = e.divide(k);
                LargeInteger index = this.degree().subtract(oldPower);
                out.add(this.coefficients.get(index));
            } else out.add(LargeFraction.ZERO);
            if (e.isZero()) break;
        }

        return new LargePolynomial(out);
    }
    public LargePolynomial compose(LargePolynomial inner){
        LargePolynomial result = ZERO;
        for(LargeFraction coefficient: coefficients)
            result = result.multiply(inner).add(coefficient);
        return result;
    }
    public LargePolynomial derivative() {
        LargeList<LargeFraction> coefficients = new LargeList<>();
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(degree()); i=i.add(1))
            coefficients.add((LargeFraction) this.coefficients.get(i).multiply(degree().subtract(i)));
        return new LargePolynomial(coefficients);
    }
}