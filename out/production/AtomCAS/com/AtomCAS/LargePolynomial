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
    public static LargePolynomial monomial(LargeFraction coeff, LargeInteger deg){
        LargeList<LargeFraction> coefficients = new LargeList<>();
        coefficients.add(coeff);
        for(LargeInteger i = LargeInteger.zero; i.lessThan(deg); i = i.add(1)) coefficients.add(LargeFraction.zero);
        return new LargePolynomial(coefficients);
    }
    public String toString(){
        StringBuilder str = new StringBuilder();
        for(LargeInteger i = LargeInteger.zero; i.lessThan(coefficients.size()); i = i.add(1)){
            if(coefficients.get(i).sign().equalsTo(LargeInteger.negativeOne) && i.equalsTo(LargeInteger.zero))
                str.append("-");
            if(i.notEqualsTo(LargeInteger.zero)){
                if(coefficients.get(i).sign().equalsTo(LargeInteger.negativeOne)){
                    str.append(" - ");
                }else if(coefficients.get(i).sign().equalsTo(LargeInteger.one)){
                    str.append(" + ");
                }else continue;
            }
            if(i.lessThan(coefficients.size().subtract(1))){
                if(coefficients.get(i).magnitude().notEqualsTo(LargeInteger.one))
                    str.append(coefficients.get(i).magnitude()).append("*");
                str.append("x");
                if(i.lessThan(coefficients.size().subtract(2)))
                    str.append("^").append(coefficients.size().subtract(1).subtract(i));
            }else str.append(coefficients.get(i).magnitude());
        }
        return str.toString();
    }
    public boolean isZero(){return coefficients.getFirst().equalsTo(LargeInteger.zero);}
    public static LargePolynomial one(){
        LargePolynomial P = new LargePolynomial();
        P = P.add(LargeFraction.one);
        return P;
    }
    public static LargePolynomial xMinusOne(){
        LargeList<LargeFraction> list = new LargeList<>();
        list.add(LargeFraction.one);
        list.add(LargeFraction.negativeOne);
        return new LargePolynomial(list);
    }
    public LargeInteger degree(){return (LargeInteger) coefficients.size().subtract(LargeInteger.one);}
    public LargePolynomial increaseDegree(LargeInteger i){
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
                if(i.greaterThanOrEqualTo(diff))
                    cands.add((LargeFraction) this.coefficients.get(i).subtract(other.coefficients.get((LargeInteger) i.subtract(diff))));
                else cands.add(this.coefficients.get(i));
            }
            return new LargePolynomial(cands);
        }else if(other.coefficients.size().greaterThan(this.coefficients.size())){
            LargeInteger diff = (LargeInteger) other.coefficients.size().subtract(this.coefficients.size());
            for(LargeInteger i = LargeInteger.zero; i.lessThan(other.coefficients.size()); i = i.add(1)){
                if(i.greaterThanOrEqualTo(diff))
                    cands.add((LargeFraction) other.coefficients.get(i).subtract(this.coefficients.get((LargeInteger) i.subtract(diff))));
                else cands.add(other.coefficients.get(i));
            }
            return new LargePolynomial(cands);
        }
        for(LargeInteger i = LargeInteger.zero; i.lessThan(this.coefficients.size()); i = i.add(1))
            cands.add((LargeFraction) this.coefficients.get(i).subtract(other.coefficients.get(i)));
        return new LargePolynomial(cands);
    }
    public LargePolynomial multiply(LargeFraction other){
        LargeList<LargeFraction> cands = new LargeList<>();
        for(LargeFraction I: coefficients)
            cands.add((LargeFraction) I.multiply(other));
        return new LargePolynomial(cands);
    }
    public LargePolynomial multiply(LargePolynomial other){
        LargeList<LargePolynomial> polys = new LargeList<>();
        for(LargeInteger i = LargeInteger.zero; i.lessThan(this.coefficients.size()); i = i.add(1))
            polys.add(other.multiply(coefficients.get(i)).increaseDegree((LargeInteger) this.degree().subtract(i)));
        LargePolynomial result = new LargePolynomial();
        for(LargePolynomial P: polys) result = result.add(P);
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
        while(expTemp.notEqualsTo(LargeInteger.zero)){
            if(expTemp.isOdd()){
                P = P.multiply(basTemp);
                if(expTemp.equals(LargeInteger.one)) break;
            }
            basTemp = basTemp.square();
            expTemp = expTemp.longDivision(LargeInteger.two);
        }
        return P;
    }
    public LargePolynomial longDivision(LargePolynomial divisor){
        //long division
        if (divisor.isZero()) throw new ArithmeticException("Error: Cannot divide by 0");
        LargePolynomial remainder = new LargePolynomial(this.coefficients);
        LargePolynomial quotient = new LargePolynomial();
        LargeInteger divisorDegree = divisor.degree();
        LargeFraction divisorLead = divisor.coefficients.getFirst();
        while (!remainder.isZero() && remainder.degree().greaterThanOrEqualTo(divisorDegree)){
            LargeInteger degreeDiff = (LargeInteger) remainder.degree().subtract(divisorDegree);
            LargeFraction coeff = (LargeFraction) remainder.coefficients.getFirst().divide(divisorLead);
            LargePolynomial term = LargePolynomial.monomial(coeff, degreeDiff);
            quotient = quotient.add(term);
            remainder = remainder.subtract(term.multiply(divisor));
        }
        return quotient;
    }
    public static LargePolynomial cyclotomic(LargeInteger n){
        if(n.sign().equalsTo(LargeInteger.negativeOne)) throw new IllegalArgumentException("Error: Cannot allow negative cyclotomics");
        if (n.equalsTo(LargeInteger.one))
            return LargePolynomial.xMinusOne();
        LargePolynomial result = LargePolynomial.monomial(LargeFraction.one, n).subtract(LargePolynomial.one());
        for (LargeInteger d: n.divisorsProper())
            result = result.longDivision(cyclotomic(d));
        return result;
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
            result = result.multiply(s).add(coefficients.get(i));
        return result;
    }
    public LargePolynomial plugIn(LargePolynomial x) {
        LargePolynomial result = new LargePolynomial();
        for (LargeInteger i = LargeInteger.zero; i.lessThan(coefficients.size()); i = i.add(1))
            result = result.multiply(x).add(coefficients.get(i));
        return result;
    }
    public static LargePolynomial minimalPolynomial(ScalarAtom s){
        if(s instanceof Logarithm || s instanceof Exp) throw new IllegalArgumentException("Error: Transcendental numbers do not have minimal polynomials");
        return null;
    }
}
