package com.AtomCAS.utilities;
public class GeneralPolynomial{
    LargeList<ScalarAtom> coefficients;
    public GeneralPolynomial(){
        coefficients = new LargeList<>();
        coefficients.add(LargeInteger.zero);
    }
    public GeneralPolynomial(LargeList<ScalarAtom> coefficients){
        //trim leading zeros
        while(coefficients.getFirst().equalsTo(LargeInteger.zero) && coefficients.size().greaterThan(LargeInteger.one))
            coefficients.removeFirst();
        this.coefficients = new LargeList<>();
        for(ScalarAtom coefficient : coefficients){
            if(coefficient.isAlgebraic())
                this.coefficients.add((ScalarAtom) coefficient.copy());
            else throw new IllegalArgumentException("Error: At least one coefficient is not algebraic");
        }
    }
    public static GeneralPolynomial fromLargePolynomial(LargePolynomial p){
        LargeList<ScalarAtom> list = new LargeList<>();
        for(LargeFraction f : p.coefficients)
            list.add(f.copy());
        return new GeneralPolynomial(list);
    }
    public GeneralPolynomial copy(){return new GeneralPolynomial(coefficients);}
    public static GeneralPolynomial monomial(ScalarAtom coeff, LargeInteger deg){
        LargeList<ScalarAtom> coefficients = new LargeList<>();
        coefficients.add(coeff);
        for(LargeInteger i = LargeInteger.zero; i.lessThan(deg); i = i.add(1)) coefficients.add(LargeFraction.zero);
        return new GeneralPolynomial(coefficients);
    }
    @Override
    public String toString(){
        if(isZero()) return "0";

        StringBuilder str = new StringBuilder();
        LargeInteger deg = degree();

        for(LargeInteger i = LargeInteger.zero; i.lessThan(coefficients.size()); i = i.add(1)){
            ScalarAtom c = coefficients.get(i);
            if(c.equalsTo(LargeInteger.zero)) continue;
            LargeInteger power = (LargeInteger) deg.subtract(i);
            if(!str.isEmpty()) str.append(" + ");
            if(power.equalsTo(LargeInteger.zero))
                str.append(c);
            else if(power.equalsTo(LargeInteger.one)){
                str.append(c).append("*x");
            }else
                str.append(c).append("*x^").append(power);
        }

        return str.toString();
    }
    public boolean isZero(){
        for(ScalarAtom f: coefficients){
            if(f.notEqualsTo(LargeInteger.zero))
                return false;
        }
        return true;
    }
    public static GeneralPolynomial one(){
        GeneralPolynomial P = new GeneralPolynomial();
        P = P.add(LargeInteger.one);
        return P;
    }
    public static GeneralPolynomial x(){return new GeneralPolynomial().add(LargeInteger.one).increaseDegree(LargeInteger.one);}
    public static GeneralPolynomial xMinus(ScalarAtom n){
        LargeList<ScalarAtom> list = new LargeList<>();
        list.add(LargeFraction.one);
        list.add(n.negate());
        return new GeneralPolynomial(list);
    }
    public GeneralPolynomial negate(){
        GeneralPolynomial P = new GeneralPolynomial();
        for(ScalarAtom f: coefficients)
            P = P.add(f.negate());
        return P;
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
    public LargeInteger degree(){return (LargeInteger) coefficients.size().subtract(LargeInteger.one);}
    public GeneralPolynomial increaseDegree(LargeInteger i){
        if(i.lessThan(LargeInteger.zero)) throw new IllegalArgumentException("Error: Input means decreasing the degree");
        if(this.isZero()) return new GeneralPolynomial();
        LargeList<ScalarAtom> cands = new LargeList<>();
        for(ScalarAtom I: coefficients) cands.add(I);
        for(LargeInteger j = LargeInteger.zero; j.lessThan(i); j = j.add(1)) cands.add(LargeFraction.zero);
        return new GeneralPolynomial(cands);
    }
    public GeneralPolynomial composeNegX() {
        LargeList<ScalarAtom> result = new LargeList<>();
        LargeInteger deg = this.degree();
        for (LargeInteger i = LargeInteger.zero; i.lessThan(coefficients.size()); i = i.add(1)) {
            LargeInteger power = (LargeInteger) deg.subtract(i);
            ScalarAtom c = coefficients.get(i);
            if (power.isOdd())
                result.add(c.negate());
            else
                result.add(c);
        }
        return new GeneralPolynomial(result);
    }
    public GeneralPolynomial add(ScalarAtom other){
        LargeList<ScalarAtom> cands = new LargeList<>();
        for(ScalarAtom I: coefficients) cands.add(I);
        cands.set(cands.size().subtract(1), cands.get(cands.size().subtract(1)).add(other));
        return new GeneralPolynomial(cands);
    }
    public GeneralPolynomial add(GeneralPolynomial other){
        LargeList<ScalarAtom> cands = new LargeList<>();
        if(this.coefficients.size().greaterThan(other.coefficients.size())){
            LargeInteger diff = (LargeInteger) this.coefficients.size().subtract(other.coefficients.size());
            for(LargeInteger i = LargeInteger.zero; i.lessThan(this.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    cands.add(this.coefficients.get(i).add(other.coefficients.get((LargeInteger) i.subtract(diff))));
                else cands.add(this.coefficients.get(i));
            }
            return new GeneralPolynomial(cands);
        }else if(other.coefficients.size().greaterThan(this.coefficients.size())){
            LargeInteger diff = (LargeInteger) other.coefficients.size().subtract(this.coefficients.size());
            for(LargeInteger i = LargeInteger.zero; i.lessThan(other.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    cands.add(other.coefficients.get(i).add(this.coefficients.get((LargeInteger) i.subtract(diff))));
                else cands.add(other.coefficients.get(i));
            }
            return new GeneralPolynomial(cands);
        }
        for(LargeInteger i = LargeInteger.zero; i.lessThan(this.coefficients.size()); i = i.add(1))
            cands.add(this.coefficients.get(i).add(other.coefficients.get(i)));
        return new GeneralPolynomial(cands);
    }
    public GeneralPolynomial subtract(ScalarAtom other){
        LargeList<ScalarAtom> cands = new LargeList<>();
        for(ScalarAtom I: coefficients) cands.add(I);
        cands.set(cands.size().subtract(1), cands.get(cands.size().subtract(1)).subtract(other));
        return new GeneralPolynomial(cands);
    }
    public GeneralPolynomial subtract(GeneralPolynomial other){
        LargeList<ScalarAtom> cands = new LargeList<>();
        if(this.coefficients.size().greaterThan(other.coefficients.size())){
            LargeInteger diff = (LargeInteger) this.coefficients.size().subtract(other.coefficients.size());
            for(LargeInteger i = LargeInteger.zero; i.lessThan(this.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    cands.add(this.coefficients.get(i).subtract(other.coefficients.get((LargeInteger) i.subtract(diff))));
                else cands.add(this.coefficients.get(i));
            }
            return new GeneralPolynomial(cands);
        }else if(other.coefficients.size().greaterThan(this.coefficients.size())){
            LargeInteger diff = (LargeInteger) other.coefficients.size().subtract(this.coefficients.size());
            for(LargeInteger i = LargeInteger.zero; i.lessThan(other.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    cands.add(other.coefficients.get(i).subtract(this.coefficients.get((LargeInteger) i.subtract(diff))));
                else cands.add(other.coefficients.get(i).negate());
            }
            return new GeneralPolynomial(cands);
        }
        for(LargeInteger i = LargeInteger.zero; i.lessThan(this.coefficients.size()); i = i.add(1))
            cands.add(this.coefficients.get(i).subtract(other.coefficients.get(i)));
        return new GeneralPolynomial(cands);
    }
    public GeneralPolynomial multiply(ScalarAtom other){
        if(other.equalsTo(LargeInteger.zero) || isZero()) return new GeneralPolynomial();
        LargeList<ScalarAtom> cands = new LargeList<>();
        for(ScalarAtom I: coefficients)
            cands.add(I.multiply(other));
        return new GeneralPolynomial(cands);
    }
    public GeneralPolynomial multiply(GeneralPolynomial other){
        LargeList<GeneralPolynomial> polys = new LargeList<>();
        for(LargeInteger i = LargeInteger.zero; i.lessThan(this.coefficients.size()); i = i.add(1))
            polys.add(other.multiply(coefficients.get(i)).increaseDegree((LargeInteger) this.degree().subtract(i)));
        GeneralPolynomial result = new GeneralPolynomial();
        for(GeneralPolynomial P: polys) result = result.add(P);
        return result;
    }
    public GeneralPolynomial square(){return this.multiply(this);}
    public GeneralPolynomial monomialPower(LargeInteger I){
        if(I.lessThan(LargeInteger.zero)) throw new ArithmeticException("Error: Rational functions not implemented yet");
        if(I.equalsTo(LargeInteger.zero)){
            return new GeneralPolynomial().add(LargeInteger.one);
        }
        GeneralPolynomial P = new GeneralPolynomial();
        P = P.add(LargeFraction.one);
        LargeInteger expTemp = I.copy();
        GeneralPolynomial basTemp = new GeneralPolynomial(this.coefficients);
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
    public GeneralPolynomial longDivision(GeneralPolynomial divisor){
        //long division
        if (divisor.isZero()) throw new ArithmeticException("Error: Cannot divide by 0");
        GeneralPolynomial remainder = new GeneralPolynomial(this.coefficients);
        GeneralPolynomial quotient = new GeneralPolynomial();
        LargeInteger divisorDegree = divisor.degree();
        ScalarAtom divisorLead = divisor.coefficients.getFirst();
        while (!remainder.isZero() && remainder.degree().greaterThanOrEqualTo(divisorDegree)){
            LargeInteger degreeDiff = (LargeInteger) remainder.degree().subtract(divisorDegree);
            ScalarAtom coeff = remainder.coefficients.getFirst().divide(divisorLead);
            GeneralPolynomial term = GeneralPolynomial.monomial(coeff, degreeDiff);
            quotient = quotient.add(term);
            remainder = remainder.subtract(term.multiply(divisor));
        }
        return quotient;
    }
    public GeneralPolynomial compose(GeneralPolynomial inner){
        GeneralPolynomial result = new GeneralPolynomial();
        for(LargeInteger i = LargeInteger.zero; i.lessThan(coefficients.size()); i = i.add(1))
            result = result.multiply(inner).add(coefficients.get(i));
        return result;
    }
}

