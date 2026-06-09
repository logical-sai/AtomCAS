package com.AtomCAS.utilities;
//this class is intended to represent numbers of the form (-1)^x * b^e. x can range within [0, 2) due to modular reductions.
//as a consequence, later classes treat complex numbers as strictly principal; so one gets expressions like (-1)^(1/3) ≠ -1
//my original plan was base^exponent. but I realized that was too limited, so I switched to the current format.
//if the whole thing is rational, collapsing can vary. the b^e part collapses to rational^1. if the result is negative, exponent2 = 1, otherwise it is 0
//this class is used in simplification in RadicalTerm and later classes.
public class LargePowerMonomial extends ScalarAtom{
    public final LargeFraction base;
    public final LargeFraction exponent;
    public final LargeFraction exponent2;
    public static final LargePowerMonomial zero = new LargePowerMonomial(LargeFraction.zero, LargeFraction.one);
    public static final LargePowerMonomial one = new LargePowerMonomial(LargeFraction.one, LargeFraction.one);
    public static final LargePowerMonomial sqrt2 = new LargePowerMonomial(new LargeFraction(2),LargeFraction.half);
    public static final LargePowerMonomial sqrt3 = new LargePowerMonomial(new LargeFraction(3), LargeFraction.half);
    public static final LargePowerMonomial zeta1 = new LargePowerMonomial(new LargeFraction(-1), new LargeFraction(LargeInteger.one, LargeInteger.six));
    public static final LargePowerMonomial i = new LargePowerMonomial(new LargeFraction(-1), LargeFraction.half);
    public static final LargePowerMonomial zeta5 = new LargePowerMonomial(new LargeFraction(-1), new LargeFraction(LargeInteger.five, LargeInteger.six));
    public static final LargePowerMonomial omegaSqrt3 = new LargePowerMonomial(new LargeFraction(3), LargeFraction.half, LargeFraction.third);
    public LargePowerMonomial(LargeFraction num, LargeFraction dem){
        if(num.notEqualsTo(LargeInteger.zero) && dem.equalsTo(LargeInteger.zero)){
            base = LargeFraction.one;
            exponent = LargeFraction.one;
            exponent2 = LargeFraction.zero;
            return;
        }else if(num.equalsTo(LargeFraction.zero) || num.equalsTo(LargeFraction.one)){
            if(num.equalsTo(LargeFraction.zero) && dem.lessThan(LargeFraction.zero)) throw new ArithmeticException("Error: Cannot divide by 0");
            else if(num.equalsTo(LargeFraction.zero) && dem.equalsTo(LargeFraction.zero)) throw new ArithmeticException("Indeterminate form: 0^0");
            base = new LargeFraction(num.numerator, num.denominator);
            exponent = LargeFraction.one;
            exponent2 = LargeFraction.zero;
            return;
        }else if(num.magnitude().equalsTo(LargeInteger.one)){
            if(num.equalsTo(LargeInteger.one)){
                exponent2 = LargeFraction.zero;
                exponent = LargeFraction.one;
                base = LargeFraction.one;
                return;
            }
            if(dem.denominator.equalsTo(LargeInteger.one)){
                if(dem.numerator.isOdd()){
                    base = LargeFraction.one;
                    exponent2 = LargeFraction.one;
                    exponent = LargeFraction.one;
                    return;
                }
                base = LargeFraction.one;
                exponent2 = LargeFraction.zero;
                exponent = LargeFraction.one;
                return;
            }
            exponent2 = (LargeFraction) dem.copy().mod(LargeInteger.two);
            exponent = LargeFraction.one;
            base = LargeFraction.one;
            return;
        }else if(dem.magnitude().equalsTo(LargeInteger.one)){
            if(dem.sign().equalsTo(LargeInteger.negativeOne)) base = num.reciprocal().magnitude();
            else base = num.magnitude();
            if(num.sign().equalsTo(LargeInteger.negativeOne))
                exponent2 = LargeFraction.one;
            else exponent2 = LargeFraction.zero;
            exponent = LargeFraction.one;
            return;
        }
        LargeMap<LargeInteger, LargeFraction> pr = num.factor();
        for(LargeInteger I: pr)
            pr.put(I, (LargeFraction) pr.get(I).multiply(dem));
        if(pr.containsKey(LargeInteger.negativeOne)){
            exponent2 = (LargeFraction) pr.get(LargeInteger.negativeOne).mod(LargeInteger.two);
            pr.remove(LargeInteger.negativeOne);
            if (pr.size().equalsTo(LargeInteger.zero)){
                base = LargeFraction.one;
                exponent = LargeFraction.one;
                return;
            }
        }else
            exponent2 = LargeFraction.zero;
        LargeInteger lc = pr.get(pr.keyAtIndex(LargeInteger.zero)).denominator;
        for(LargeInteger I: pr) lc = lc.lcm(pr.get(I).denominator);
        if(lc.equalsTo(LargeInteger.one)){
            base = LargeFraction.buildFromFactors(pr);
            exponent = LargeFraction.one;
            return;
        }else if(pr.size().equalsTo(LargeInteger.one)){
            if(pr.get(pr.keyAtIndex(LargeInteger.zero)).lessThan(LargeInteger.zero)){
                base = pr.keyAtIndex(LargeInteger.zero).reciprocal();
                exponent = pr.get(pr.keyAtIndex(LargeInteger.zero)).negate();
            }else{
                base = new LargeFraction(pr.keyAtIndex(LargeInteger.zero), LargeInteger.one);
                exponent = pr.get(pr.keyAtIndex(LargeInteger.zero));
            }
            return;
        }
        LargeFraction G = pr.get(pr.keyAtIndex(LargeInteger.zero));
        for(LargeInteger I: pr) G = G.gcd(pr.get(I));
        LargeFraction B = LargeFraction.one;
        for(LargeInteger I: pr){
            LargeInteger expTemp = ((LargeFraction)pr.get(I).divide(G)).numerator;
            LargeInteger basTemp = I.copy();
            while(expTemp.notEqualsTo(LargeInteger.zero)){
                if(expTemp.isOdd()){
                    if(expTemp.lessThan(LargeInteger.zero))
                        B = (LargeFraction) B.divide(basTemp);
                    else B = (LargeFraction) B.multiply(basTemp);
                    if(expTemp.magnitude().equalsTo(LargeInteger.one)) break;
                }
                basTemp = basTemp.square();
                expTemp = expTemp.longDivision(LargeInteger.two);
            }
        }
        base = B.copy();
        exponent = G.copy();
    }
    public LargePowerMonomial(LargeFraction base, LargeFraction exponent, LargeFraction exponent2){
        LargePowerMonomial obj = new LargePowerMonomial(base, exponent);
        this.base = obj.base;
        this.exponent = obj.exponent;
        this.exponent2 = (LargeFraction) exponent2.add(obj.exponent2).mod(LargeInteger.two);
    }
    @Override
    public LargePowerMonomial copy() {return new LargePowerMonomial(base, exponent, exponent2);}
    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        if(exponent2.notEqualsTo(LargeInteger.zero)){
            if(exponent2.notEqualsTo(LargeInteger.one))
                str.append("(-1)^(").append(exponent2).append(")");
            else
                str.append("-");
        }
        if(base.equalsTo(LargeInteger.one)){
            if(str.isEmpty() || str.toString().equals("-")) str.append("1");
            return str.toString();
        }
        if(!str.toString().equals("-") && !str.isEmpty()) str.append("*");
        if(base.denominator.notEqualsTo(LargeInteger.one) && exponent.notEqualsTo(LargeInteger.one))
            str.append("(").append(base).append(")");
        else
            str.append(base.magnitude());
        if(exponent.notEqualsTo(LargeFraction.one))
            str.append("^(").append(exponent).append(")");
        return str.toString();
    }
    @Override
    public String toLatex() {
        StringBuilder str = new StringBuilder();
        if (exponent2.notEqualsTo(LargeInteger.zero)) {
            if (exponent2.equalsTo(LargeInteger.one))
                str.append("-");
            else
                str.append("(-1)^{").append(exponent2.toLatex()).append("}");
        }
        if (base.equalsTo(LargeInteger.one)) {
            if (str.isEmpty() || str.toString().equals("-")) str.append("1");
            return str.toString();
        }
        if (!str.isEmpty() && !str.toString().equals("-"))
            str.append("\\cdot ");
        String baseLatex = base.toLatex();
        if (base.denominator.notEqualsTo(LargeInteger.one) || exponent.notEqualsTo(LargeFraction.one))
            str.append("\\left(").append(baseLatex).append("\\right)");
        else
            str.append(baseLatex);
        if (exponent.notEqualsTo(LargeFraction.one))
            str.append("^{").append(exponent.toLatex()).append("}");
        return str.toString();
    }
    @Override
    public boolean equalsTo(Atom other) {
        if(!(other instanceof ScalarAtom)) return false;
        if(other instanceof LargeInteger || other instanceof LargeFraction){
            boolean B = this.base.equalsTo(((ScalarAtom) other).magnitude()) && this.exponent.equalsTo(LargeFraction.one);
            if(((ScalarAtom) other).sign().equalsTo(LargeInteger.negativeOne))
                return B && this.exponent2.equalsTo(LargeFraction.one);
            else if(((ScalarAtom) other).sign().equalsTo(LargeInteger.one))
                return B && this.exponent2.equalsTo(LargeFraction.zero);
            return B;
        }
        if(other instanceof LargePowerMonomial)
            return ((LargePowerMonomial) other).base.equalsTo(this.base) && this.exponent.equalsTo(((LargePowerMonomial) other).exponent) && this.exponent2.equalsTo(((LargePowerMonomial) other).exponent2);
        if(other instanceof RadicalTerm)
            return new RadicalTerm(LargeFraction.zero, LargeFraction.one, this).equalsTo(other);
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public boolean notEqualsTo(Atom other) {return !equalsTo(other);}
    @Override
    public LargePowerMonomial magnitude(){return new LargePowerMonomial(base, exponent);}
    @Override
    public long compareMags(ScalarAtom other){
        if(other instanceof LargeInteger || other instanceof LargeFraction){
            LargeInteger pow = exponent.denominator.lcm(exponent2.denominator);
            LargeFraction T = this.power(new LargeFraction(pow, LargeInteger.one)).base;
            LargeFraction O = ((LargeFraction)other.divide(LargeInteger.one)).power(pow);
            return T.compareMags(O);
        }else if(other instanceof LargePowerMonomial L){
            LargeInteger pow = exponent.denominator.lcm(exponent2.denominator, L.exponent.denominator, L.exponent2.denominator);
            LargeFraction T = this.power(new LargeFraction(pow, LargeInteger.one)).base;
            LargeFraction O = L.power(new LargeFraction(pow, LargeInteger.one)).base;
            return T.compareMags(O);
        }else if(other instanceof RadicalTerm R){
            if(R.coefficient.equals(LargeFraction.zero)) return this.compareMags(R.rationalPart);
            else if(R.rationalPart.equalsTo(LargeFraction.zero)){
                LargeInteger pow = exponent.denominator.lcm(exponent2.denominator, R.exponent.exponent.denominator, R.exponent.exponent2.denominator);
                LargeFraction T = this.power(new LargeFraction(pow, LargeInteger.one)).base;
                LargeFraction O = R.exponent.power(new LargeFraction(pow, LargeInteger.one)).base;
                O = (LargeFraction) O.multiply(R.coefficient.power(pow));
                return T.compareMags(O);
            }
            LargeFraction F1 = new LargeFraction(this.floor(), LargeInteger.one);
            LargeFraction C1 = new LargeFraction(this.ceil(), LargeInteger.one);
            LargeFraction F2 = new LargeFraction(R.floor(), LargeInteger.one);
            LargeFraction C2 = new LargeFraction(R.ceil(), LargeInteger.one);
            if(F1.greaterThan(C2)) return 1;
            else if(F2.greaterThan(C1)) return -1;

        }
        return this.magnitude().compareTo(other.magnitude());
    }
    @Override
    public long compareTo(ScalarAtom other) {
        if(this.isNotReal() || other.isNotReal()) throw new IllegalStateException("Error: Complex numbers cannot be compared");
        if(other instanceof LargeFraction || other instanceof LargeInteger){
            LargePowerMonomial copy = new LargePowerMonomial(base, exponent);
            LargePowerMonomial copy2 = new LargePowerMonomial((LargeFraction) other.divide(LargeInteger.one), LargeFraction.one);
            LargeFraction pow = copy.exponent.denominator.power(LargeInteger.one);
            copy = copy.power(pow);
            copy2 = copy2.power(pow);
            return copy.base.compareTo(copy2.base);
        }
        if(other instanceof LargePowerMonomial){
            LargePowerMonomial copy = new LargePowerMonomial(this.base, this.exponent);
            LargePowerMonomial copy2 = new LargePowerMonomial(((LargePowerMonomial) other).base, ((LargePowerMonomial) other).exponent);
            LargeInteger lc = this.exponent.denominator.lcm(((LargePowerMonomial) other).exponent.denominator);
            copy = copy.power((LargeFraction) lc.divide(LargeInteger.one));
            copy2 = copy2.power((LargeFraction) lc.divide(LargeInteger.one));
            long C = new LargePowerMonomial(copy.base, copy.exponent).base.compareMags(new LargePowerMonomial(copy2.base, copy2.exponent).base);
            if(C == 1L){
                if(this.sign().equalsTo(LargeInteger.one)) return 1L;
                return -1L;
            }else if(C == -1L){
                if(other.sign().equalsTo(LargeInteger.one)) return -1L;
                return 1L;
            }
            return C;
        }else if(other instanceof RadicalTerm){
            //I present to you...ERROR BOUNDS
            if(((RadicalTerm) other).coefficient.equalsTo(LargeInteger.zero)) return this.compareTo(((RadicalTerm) other).rationalPart);
            LargeFraction l1 = new LargeFraction(this.floor(), LargeInteger.one);
            LargeFraction u1 = new LargeFraction(this.ceil(), LargeInteger.one);
            LargeFraction l2 = new LargeFraction(other.floor(), LargeInteger.one);
            LargeFraction u2 = new LargeFraction(other.ceil(), LargeInteger.one);
            while(l1.lessThan(u2) && l2.lessThan(u1)){
                LargeFraction mediant = new LargeFraction((LargeInteger) l1.numerator.add(u1.numerator), (LargeInteger) l1.denominator.add(u1.denominator));
                if(mediant.lessThan(this))
                    l1 = mediant.copy();
                else u1 = mediant.copy();
                LargeFraction mediant2 = new LargeFraction((LargeInteger) l2.numerator.add(u2.numerator), (LargeInteger) l2.denominator.add(u2.denominator));
                if(mediant2.lessThan(other))
                    l2 = mediant2.copy();
                else u2 = mediant2.copy();
            }
            return l1.compareTo(u2);
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public boolean greaterThan(ScalarAtom other){return this.compareTo(other) == 1L;}
    @Override
    public boolean lessThan(ScalarAtom l){return this.compareTo(l) == -1L;}
    @Override
    public boolean greaterThanOrEqualTo(ScalarAtom l){return this.compareTo(l) >= 0L;}
    @Override
    public boolean lesserThanOrEqualTo(ScalarAtom other) {return this.compareTo(other) <= 0L;}
    @Override
    public LargePowerMonomial negate() {return new LargePowerMonomial(this.base, this.exponent, (LargeFraction) this.exponent2.add(LargeInteger.one));}
    @Override
    public boolean isReal() {return !isNotReal();}
    @Override
    public boolean isRational(){return exponent2.denominator.equalsTo(LargeFraction.one) && exponent.equalsTo(LargeFraction.one);}
    @Override
    public boolean isNotReal(){return exponent2.notEqualsTo(LargeFraction.one) && exponent2.notEqualsTo(LargeFraction.zero);}
    public boolean isNotInteger() {return exponent.notEqualsTo(LargeInteger.one) || base.denominator.notEqualsTo(LargeInteger.one) || this.isNotReal();}
    @Override
    public boolean isAlgebraic() {return true;}
    public LargePowerMonomial conjugate(){return new LargePowerMonomial(base, exponent, exponent2.negate());}
    @Override
    public LargeInteger floor(){
        if(this.isNotReal()) throw new IllegalStateException("Error: Cannot floor a non-real atom");
        else if(this.isRational())
            return base.floor();
        boolean negativeResult = exponent2.equalsTo(LargeInteger.one);
        LargeFraction begun = base.numerator.magnitude().power(exponent.numerator);
        LargeFraction helper = base.denominator.magnitude().power(exponent.numerator);
        LargeInteger d = exponent.denominator;
        LargeInteger low;
        LargeInteger high;
        if(LargeInteger.one.power(d).multiply(helper).lesserThanOrEqualTo(begun)){
            low = LargeInteger.one;
            high = LargeInteger.two;
            while(high.power(d).multiply(helper).lesserThanOrEqualTo(begun)){
                low = high;
                high = (LargeInteger) high.multiply(LargeInteger.two);
            }
            while(high.subtract(low).greaterThan(LargeInteger.one)){
                LargeInteger mid = ((LargeInteger) low.add(high)).longDivision(LargeInteger.two);
                if(mid.power(d).multiply(helper).lesserThanOrEqualTo(begun))
                    low = mid;
                else
                    high = mid;
            }
        }
        else low = LargeInteger.zero;
        if(!negativeResult) return low;
        return (LargeInteger) low.add(LargeInteger.one).negate();
    }
    @Override
    public LargeInteger ceil(){
        if(this.isRational()) return base.ceil();
        return (LargeInteger) this.floor().add(LargeInteger.one);
    }
    @Override
    public ScalarAtom add(ScalarAtom other) {
        if(other instanceof LargeInteger || other instanceof LargeFraction)
            return new RadicalTerm((LargeFraction) other.divide(LargeInteger.one), LargeFraction.one, this);
        else if(other instanceof LargePowerMonomial){
            LargeList<RadicalTerm> rt = new LargeList<>();
            rt.add(new RadicalTerm(LargeFraction.zero, LargeFraction.one, this));
            rt.add(new RadicalTerm(LargeFraction.zero, LargeFraction.one, (LargePowerMonomial) other));
            return new RadicalTermSum(rt, LargeFraction.zero);
        }else if(other instanceof RadicalTerm){
            LargeList<RadicalTerm> rt = new LargeList<>();
            rt.add(new RadicalTerm(LargeFraction.zero, LargeFraction.one, this));
            rt.add((RadicalTerm) other);
            return new RadicalTermSum(rt, LargeFraction.zero);
        }else if(other instanceof RadicalTermSum R){
            LargeList<RadicalTerm> rt = new LargeList<>();
            rt.add(new RadicalTerm(LargeFraction.zero, LargeFraction.one, this));
            for(LargePowerMonomial L: R.sum)
                rt.add(new RadicalTerm(LargeFraction.zero, R.sum.get(L), L));
            return new RadicalTermSum(rt, R.constant);
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public ScalarAtom subtract(ScalarAtom other) {return this.add(other.negate());}
    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        if(other instanceof LargeInteger || other instanceof LargeFraction){
            LargeMap<LargeInteger, LargeFraction> pr = this.factor();
            LargeMap<LargeInteger, LargeFraction> pr2 = ((LargeFraction)other.divide(LargeInteger.one)).factor();
            for(LargeInteger I: pr2){
                if(pr.containsKey(I))
                    pr.put(I, (LargeFraction) pr.get(I).add(pr2.get(I)));
                else pr.put(I, pr2.get(I));
            }
            return buildFromFactors(pr);
        }else if(other instanceof LargePowerMonomial L){
            LargeMap<LargeInteger, LargeFraction> pr = this.factor();
            LargeMap<LargeInteger, LargeFraction> pr2 = L.factor();
            for(LargeInteger I: pr2){
                if(pr.containsKey(I))
                    pr.put(I, (LargeFraction) pr.get(I).add(pr2.get(I)));
                else pr.put(I, pr2.get(I));
            }
            return buildFromFactors(pr);
        }else if(other instanceof RadicalTerm){
            LargeList<RadicalTerm> rt = new LargeList<>();
            rt.add(new RadicalTerm(LargeFraction.zero, ((RadicalTerm) other).rationalPart, this));
            rt.add(new RadicalTerm(LargeFraction.zero, ((RadicalTerm) other).coefficient, (LargePowerMonomial) ((RadicalTerm) other).exponent.multiply(this)));
            return new RadicalTermSum(rt, LargeFraction.zero);
        }else if(other instanceof RadicalTermSum R){
            LargeList<RadicalTerm> rt = new LargeList<>();
            for(LargePowerMonomial L: R.sum)
                rt.add(new RadicalTerm(LargeFraction.zero, R.sum.get(L), (LargePowerMonomial)L.multiply(this)));
            rt.add(new RadicalTerm(LargeFraction.zero, R.constant, this));
            return new RadicalTermSum(rt, LargeFraction.zero);
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public LargePowerMonomial square(){return new LargePowerMonomial(this.base, (LargeFraction) this.exponent.multiply(LargeInteger.two));}
    @Override
    public LargePowerMonomial cube(){return new LargePowerMonomial(this.base, (LargeFraction) this.exponent.multiply(LargeInteger.three));}
    public LargePowerMonomial power(LargeFraction f){return new LargePowerMonomial(base, (LargeFraction) exponent.multiply(f), (LargeFraction) exponent2.multiply(f));}

    @Override
    public LargeDecimal approximateToDecimal(LargeInteger precision) {
        return null;
    }
    public LargeMap<LargeInteger, LargeFraction> factor(){
        LargeMap<LargeInteger, LargeFraction> pr = new LargeMap<>();
        if(exponent2.notEqualsTo(LargeInteger.zero)) pr.put(LargeInteger.negativeOne, exponent2);
        if(base.equalsTo(LargeFraction.one)){
            if(pr.isEmpty())
                pr.put(LargeInteger.one, LargeFraction.one);
            return pr;
        }
        LargeMap<LargeInteger, LargeFraction> pt = base.factor();
        for(LargeInteger I: pt)
            pr.put(I, (LargeFraction) pt.get(I).multiply(exponent));
        return pr;
    }
    public static LargePowerMonomial buildFromFactors(LargeMap<LargeInteger, LargeFraction> pr){
        LargeFraction base = LargeFraction.one;
        LargeFraction exponent2 = LargeFraction.zero;
        if(pr.size().equalsTo(LargeInteger.zero)) return new LargePowerMonomial(base, LargeFraction.one);
        else if(pr.size().equalsTo(LargeInteger.one)) return new LargePowerMonomial(new LargeFraction(pr.keyAtIndex(LargeInteger.zero), LargeInteger.one), pr.get(pr.keyAtIndex(LargeInteger.zero)));
        LargeFraction G = LargeFraction.one;
        for(LargeInteger i = LargeInteger.zero; i.lessThan(pr.size()); i = i.add(1)){
            LargeInteger I = pr.keyAtIndex(i);
            if(I.equalsTo(LargeInteger.negativeOne)){
                exponent2 = (LargeFraction) exponent2.add(pr.get(I));
                pr.remove(I);
                i = i.subtract(1);
                continue;
            }
            G = G.gcd(pr.get(I));
        }
        for(LargeInteger I: pr){
            LargeInteger expTemp = ((LargeFraction) pr.get(I).divide(G)).numerator;
            LargeInteger basTemp = I.copy();
            while(expTemp.notEqualsTo(LargeInteger.zero)){
                if(expTemp.isOdd()){
                    if(expTemp.lessThan(LargeInteger.zero))
                        base = (LargeFraction) base.divide(basTemp);
                    else base = (LargeFraction) base.multiply(basTemp);
                    if(expTemp.magnitude().equalsTo(LargeInteger.one)) break;
                }
                basTemp = basTemp.square();
                expTemp = expTemp.longDivision(LargeInteger.two);
            }
        }
        return new LargePowerMonomial(base, G, exponent2);
    }
    @Override
    public LargePowerMonomial reciprocal(){return new LargePowerMonomial(base, exponent.negate(), exponent2.negate());}
}
