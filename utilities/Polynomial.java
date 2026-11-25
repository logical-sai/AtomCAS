package com.AtomCAS.utilities;
public class Polynomial implements Cloneable {
    LargeList<ScalarAtom> coefficients;
    public Polynomial(){
        coefficients = new LargeList<>();
        coefficients.add(LargeInteger.ZERO);
    }
    public Polynomial(LargeList<ScalarAtom> coefficients){
        //trim leading zeros
        while(coefficients.getFirst().isZero() && coefficients.size().greaterThan(LargeInteger.ONE))
            coefficients.removeFirst();
        this.coefficients = new LargeList<>();
        for(ScalarAtom coefficient : coefficients){
            if(coefficient.isAlgebraic())
                this.coefficients.add(coefficient.clone());
            else throw new IllegalArgumentException("At least 1 coefficient is not algebraic");
        }
    }
    public Polynomial clone() {
        try {
            Polynomial p = (Polynomial) super.clone();
            return new Polynomial(p.coefficients.clone());
        } catch (CloneNotSupportedException e) {throw new AssertionError(e);}
    }
    public static Polynomial monomial(ScalarAtom coeff, LargeInteger deg){
        LargeList<ScalarAtom> coefficients = new LargeList<>();
        coefficients.add(coeff);
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(deg); i = i.add(1)) coefficients.add(LargeFraction.ZERO);
        return new Polynomial(coefficients);
    }
    @Override
    public String toString(){
        if(isZero()) return "0";

        StringBuilder str = new StringBuilder();
        LargeInteger deg = degree();

        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(coefficients.size()); i = i.add(1)){
            ScalarAtom c = coefficients.get(i);
            if(c.isZero()) continue;
            LargeInteger power = deg.subtract(i);
            if(!str.isEmpty()) str.append(" + ");
            if(power.isZero())
                str.append(c);
            else if(power.isOne()){
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
    public static Polynomial one(){
        Polynomial P = new Polynomial();
        P = P.add(LargeInteger.ONE);
        return P;
    }
    public static Polynomial x(){return new Polynomial().add(LargeInteger.ONE).increaseDegree(LargeInteger.ONE);}
    public Polynomial negate(){
        Polynomial P = new Polynomial();
        for(ScalarAtom f: coefficients)
            P = P.add(f.negate());
        return P;
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
    public LargeInteger degree(){return coefficients.size().subtract(1);}
    public Polynomial increaseDegree(LargeInteger i){
        if(i.isNegative()) throw new IllegalArgumentException("Input means decreasing the degree");
        if(this.isZero()) return new Polynomial();
        LargeList<ScalarAtom> cands = new LargeList<>();
        for(ScalarAtom I: coefficients) cands.add(I);
        for(LargeInteger j = LargeInteger.ZERO; j.lessThan(i); j = j.add(1)) cands.add(LargeFraction.ZERO);
        return new Polynomial(cands);
    }
    public Polynomial add(ScalarAtom other){
        LargeList<ScalarAtom> cands = new LargeList<>();
        for(ScalarAtom I: coefficients) cands.add(I);
        cands.set(cands.size().subtract(1), cands.get(cands.size().subtract(1)).add(other));
        return new Polynomial(cands);
    }
    public Polynomial add(Polynomial other){
        LargeList<ScalarAtom> cands = new LargeList<>();
        if(this.coefficients.size().greaterThan(other.coefficients.size())){
            LargeInteger diff = this.coefficients.size().subtract(other.coefficients.size());
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    cands.add(this.coefficients.get(i).add(other.coefficients.get(i.subtract(diff))));
                else cands.add(this.coefficients.get(i));
            }
            return new Polynomial(cands);
        }else if(other.coefficients.size().greaterThan(this.coefficients.size())){
            LargeInteger diff = other.coefficients.size().subtract(this.coefficients.size());
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(other.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    cands.add(other.coefficients.get(i).add(this.coefficients.get(i.subtract(diff))));
                else cands.add(other.coefficients.get(i));
            }
            return new Polynomial(cands);
        }
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1))
            cands.add(this.coefficients.get(i).add(other.coefficients.get(i)));
        return new Polynomial(cands);
    }
    public Polynomial subtract(ScalarAtom other){
        LargeList<ScalarAtom> cands = new LargeList<>();
        for(ScalarAtom I: coefficients) cands.add(I);
        cands.set(cands.size().subtract(1), cands.get(cands.size().subtract(1)).subtract(other));
        return new Polynomial(cands);
    }
    public Polynomial subtract(Polynomial other){
        LargeList<ScalarAtom> cands = new LargeList<>();
        if(this.coefficients.size().greaterThan(other.coefficients.size())){
            LargeInteger diff = this.coefficients.size().subtract(other.coefficients.size());
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    cands.add(this.coefficients.get(i).subtract(other.coefficients.get(i.subtract(diff))));
                else cands.add(this.coefficients.get(i));
            }
            return new Polynomial(cands);
        }else if(other.coefficients.size().greaterThan(this.coefficients.size())){
            LargeInteger diff = other.coefficients.size().subtract(this.coefficients.size());
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(other.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    cands.add(other.coefficients.get(i).subtract(this.coefficients.get(i.subtract(diff))));
                else cands.add(other.coefficients.get(i).negate());
            }
            return new Polynomial(cands);
        }
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1))
            cands.add(this.coefficients.get(i).subtract(other.coefficients.get(i)));
        return new Polynomial(cands);
    }
    public Polynomial multiply(ScalarAtom other){
        if(other.isZero() || isZero()) return new Polynomial();
        LargeList<ScalarAtom> cands = new LargeList<>();
        for(ScalarAtom I: coefficients)
            cands.add(I.multiply(other));
        return new Polynomial(cands);
    }
    public Polynomial multiply(Polynomial other){
        LargeList<Polynomial> polys = new LargeList<>();
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.coefficients.size()); i = i.add(1))
            polys.add(other.multiply(coefficients.get(i)).increaseDegree(this.degree().subtract(i)));
        Polynomial result = new Polynomial();
        for(Polynomial P: polys) result = result.add(P);
        return result;
    }
    public Polynomial square(){return this.multiply(this);}
    public Polynomial divide(Polynomial divisor){
        //long division
        if (divisor.isZero()) throw new ArithmeticException("Cannot divide by 0");
        Polynomial remainder = new Polynomial(this.coefficients);
        Polynomial quotient = new Polynomial();
        LargeInteger divisorDegree = divisor.degree();
        ScalarAtom divisorLead = divisor.coefficients.getFirst();
        while (!remainder.isZero() && remainder.degree().greaterThanOrEqualTo(divisorDegree)){
            LargeInteger degreeDiff = remainder.degree().subtract(divisorDegree);
            ScalarAtom coeff = remainder.coefficients.getFirst().divide(divisorLead);
            Polynomial term = Polynomial.monomial(coeff, degreeDiff);
            quotient = quotient.add(term);
            remainder = remainder.subtract(term.multiply(divisor));
        }
        return quotient;
    }
    public ScalarAtom evaluateAt(ScalarAtom s) {
        ScalarAtom result = LargeInteger.ZERO;
        for(ScalarAtom coefficient: this.coefficients)
            result = result.multiply(s).add(coefficient);
        return Parser.parse(result.toString());
    }
    public Polynomial compose(Polynomial inner){
        Polynomial result = new Polynomial();
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(coefficients.size()); i = i.add(1))
            result = result.multiply(inner).add(coefficients.get(i));
        return result;
    }
}

