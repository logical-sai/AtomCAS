package com.AtomCAS.utilities;
public class GeneralPolynomial{
    LargeList<ScalarAtom> coefficients;
    public GeneralPolynomial(){
        coefficients = new LargeList<>();
        coefficients.add(LargeInteger.ZERO);
    }
    public GeneralPolynomial(LargeList<ScalarAtom> coefficients){
        //trim leading zeros
        while(coefficients.getFirst().equals(LargeInteger.ZERO) && coefficients.size().greaterThan(LargeInteger.ONE))
            coefficients.removeFirst();
        this.coefficients = new LargeList<>();
        for(ScalarAtom coefficient : coefficients){
            if(coefficient.isAlgebraic())
                this.coefficients.add((ScalarAtom) coefficient.clone());
            else throw new IllegalArgumentException("Error: At least ONE coefficient is not algebraic");
        }
    }
    public static GeneralPolynomial fromLargePolynomial(LargePolynomial p){
        LargeList<ScalarAtom> list = new LargeList<>();
        for(LargeFraction f : p.coefficients)
            list.add(f.clone());
        return new GeneralPolynomial(list);
    }
    public GeneralPolynomial copy(){return new GeneralPolynomial(coefficients);}
    public static GeneralPolynomial monomial(ScalarAtom coeff, LargeInteger deg){
        LargeList<ScalarAtom> coefficients = new LargeList<>();
        coefficients.add(coeff);
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(deg); i = i.add(1)) coefficients.add(LargeFraction.ZERO);
        return new GeneralPolynomial(coefficients);
    }
    @Override
    public String toString(){
        if(isZero()) return "0";

        StringBuilder str = new StringBuilder();
        LargeInteger deg = degree();

        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(coefficients.size()); i = i.add(1)){
            ScalarAtom c = coefficients.get(i);
            if(c.equals(LargeInteger.ZERO)) continue;
            LargeInteger power = (LargeInteger) deg.subtract(i);
            if(!str.isEmpty()) str.append(" + ");
            if(power.equals(LargeInteger.ZERO))
                str.append(c);
            else if(power.equals(LargeInteger.ONE)){
                str.append(c).append("*x");
            }else
                str.append(c).append("*x^").append(power);
        }

        return str.toString();
    }
    public boolean isZero(){
        for(ScalarAtom f: coefficients){
            if(f.doesntEqual(LargeInteger.ZERO))
                return false;
        }
        return true;
    }
    public static GeneralPolynomial one(){
        GeneralPolynomial P = new GeneralPolynomial();
        P = P.add(LargeInteger.ONE);
        return P;
    }
    public static GeneralPolynomial x(){return new GeneralPolynomial().add(LargeInteger.ONE).increaseDegree(LargeInteger.ONE);}
    public static GeneralPolynomial xMinus(ScalarAtom n){
        LargeList<ScalarAtom> list = new LargeList<>();
        list.add(LargeFraction.ONE);
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
        if(degree().doesntEqual(p.degree()))
            return false;
        for(LargeInteger i = LargeInteger.ZERO; i.lesserThanOrEqualTo(degree()); i = i.add(1)){
            if(coefficients.get(i).doesntEqual(p.coefficients.get(i)))
                return false;
        }
        return true;
    }
    public LargeInteger degree(){return (LargeInteger) coefficients.size().subtract(LargeInteger.ONE);}
    public GeneralPolynomial increaseDegree(LargeInteger i){
        if(i.lessThan(LargeInteger.ZERO)) throw new IllegalArgumentException("Error: Input means decreasing the degree");
        if(this.isZero()) return new GeneralPolynomial();
        LargeList<ScalarAtom> cands = new LargeList<>();
        for(ScalarAtom I: coefficients) cands.add(I);
        for(LargeInteger j = LargeInteger.ZERO; j.lessThan(i); j = j.add(1)) cands.add(LargeFraction.ZERO);
        return new GeneralPolynomial(cands);
    }
    public GeneralPolynomial composeNegX() {
        LargeList<ScalarAtom> result = new LargeList<>();
        LargeInteger deg = this.degree();
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(coefficients.size()); i = i.add(1)) {
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
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    cands.add(this.coefficients.get(i).add(other.coefficients.get((LargeInteger) i.subtract(diff))));
                else cands.add(this.coefficients.get(i));
            }
            return new GeneralPolynomial(cands);
        }else if(other.coefficients.size().greaterThan(this.coefficients.size())){
            LargeInteger diff = (LargeInteger) other.coefficients.size().subtract(this.coefficients.size());
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(other.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    cands.add(other.coefficients.get(i).add(this.coefficients.get((LargeInteger) i.subtract(diff))));
                else cands.add(other.coefficients.get(i));
            }
            return new GeneralPolynomial(cands);
        }
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1))
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
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    cands.add(this.coefficients.get(i).subtract(other.coefficients.get((LargeInteger) i.subtract(diff))));
                else cands.add(this.coefficients.get(i));
            }
            return new GeneralPolynomial(cands);
        }else if(other.coefficients.size().greaterThan(this.coefficients.size())){
            LargeInteger diff = (LargeInteger) other.coefficients.size().subtract(this.coefficients.size());
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(other.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    cands.add(other.coefficients.get(i).subtract(this.coefficients.get((LargeInteger) i.subtract(diff))));
                else cands.add(other.coefficients.get(i).negate());
            }
            return new GeneralPolynomial(cands);
        }
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1))
            cands.add(this.coefficients.get(i).subtract(other.coefficients.get(i)));
        return new GeneralPolynomial(cands);
    }
    public GeneralPolynomial multiply(ScalarAtom other){
        if(other.equals(LargeInteger.ZERO) || isZero()) return new GeneralPolynomial();
        LargeList<ScalarAtom> cands = new LargeList<>();
        for(ScalarAtom I: coefficients)
            cands.add(I.multiply(other));
        return new GeneralPolynomial(cands);
    }
    public GeneralPolynomial multiply(GeneralPolynomial other){
        LargeList<GeneralPolynomial> polys = new LargeList<>();
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1))
            polys.add(other.multiply(coefficients.get(i)).increaseDegree((LargeInteger) this.degree().subtract(i)));
        GeneralPolynomial result = new GeneralPolynomial();
        for(GeneralPolynomial P: polys) result = result.add(P);
        return result;
    }
    public GeneralPolynomial square(){return this.multiply(this);}
    public GeneralPolynomial monomialPower(LargeInteger I){
        if(I.lessThan(LargeInteger.ZERO)) throw new ArithmeticException("Error: Rational functions not implemented yet");
        if(I.equals(LargeInteger.ZERO)){
            return new GeneralPolynomial().add(LargeInteger.ONE);
        }
        GeneralPolynomial P = new GeneralPolynomial();
        P = P.add(LargeFraction.ONE);
        LargeInteger expTemp = I.clone();
        GeneralPolynomial basTemp = new GeneralPolynomial(this.coefficients);
        while(true){
            if(expTemp.isOdd()){
                P = P.multiply(basTemp);
                if(expTemp.equals(LargeInteger.ONE)) break;
            }
            basTemp = basTemp.square();
            expTemp = expTemp.longDivision(LargeInteger.TWO);
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
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(coefficients.size()); i = i.add(1))
            result = result.multiply(inner).add(coefficients.get(i));
        return result;
    }
}

