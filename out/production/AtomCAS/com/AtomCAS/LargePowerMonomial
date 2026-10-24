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
        LargeList<PrimeExponent> pr = num.factor();
        for(LargeInteger i = LargeInteger.zero; i.lessThan(pr.size()); i = i.add(1))
            pr.set(i, new PrimeExponent(pr.get(i).base, (LargeFraction) pr.get(i).exponent.multiply(dem)));
        if(pr.getFirst().base.equalsTo(LargeInteger.negativeOne)){
            exponent2 = (LargeFraction) pr.getFirst().exponent.mod(LargeInteger.two);
            pr.removeFirst();
            if(pr.size().equalsTo(LargeInteger.zero)){
                base = LargeFraction.one;
                exponent = LargeFraction.one;
                return;
            }
        }else exponent2 = LargeFraction.zero;
        LargeInteger lc = pr.getFirst().exponent.denominator;
        for(PrimeExponent p: pr) lc = lc.lcm(p.exponent.denominator);
        if(lc.equalsTo(LargeInteger.one)){
            base = LargeFraction.buildFromFactors(pr);
            exponent = LargeFraction.one;
            return;
        }else if(pr.size().equalsTo(LargeInteger.one)){
            if(pr.getFirst().exponent.sign().equalsTo(LargeInteger.negativeOne)){
                base = pr.getFirst().base.reciprocal();
                exponent = pr.getFirst().exponent.magnitude();
            }else{
                base = (LargeFraction) pr.getFirst().base.divide(LargeInteger.one);
                exponent = pr.getFirst().exponent;
            }
            return;
        }
        for(LargeInteger i = LargeInteger.zero; i.lessThan(pr.size()); i = i.add(1)){
            PrimeExponent p = pr.get(i);
            //make the exponents all have common denominator
            LargeInteger multiplier = lc.longDivision(p.exponent.denominator);
            pr.set(i, new PrimeExponent(p.base, (LargeFraction) p.exponent.divide(multiplier)));
            p = pr.get(i);
            LargeInteger basTemp = p.base.copy();
            LargeInteger I = LargeInteger.one;
            while(multiplier.greaterThan(LargeInteger.zero)){
                if(multiplier.isOdd()) I = (LargeInteger) I.multiply(basTemp);
                basTemp = basTemp.square();
                multiplier = multiplier.longDivision(LargeInteger.two);
            }
            pr.set(i, new PrimeExponent(I, p.exponent));
        }
        //extract gcd of numerators
        LargeInteger gc = pr.getFirst().exponent.numerator;
        for(PrimeExponent p: pr) gc = gc.gcd(p.exponent.numerator);
        LargeFraction rebase = LargeFraction.one;
        for(PrimeExponent p: pr){
            LargeInteger expTemp = (LargeInteger) p.exponent.numerator.longDivision(gc).multiply(lc.longDivision(p.exponent.denominator));
            LargeInteger basTemp = p.base.copy();
            while(expTemp.notEqualsTo(LargeInteger.zero)){
                if(expTemp.isOdd()){
                    if(expTemp.sign().equalsTo(LargeInteger.negativeOne)) rebase = (LargeFraction) rebase.divide(basTemp);
                    else rebase = (LargeFraction) rebase.multiply(basTemp);
                    if(expTemp.magnitude().equalsTo(LargeInteger.one)) break;
                }
                basTemp = basTemp.square();
                expTemp = expTemp.longDivision(LargeInteger.two);
            }
        }
        base = rebase;
        exponent = new LargeFraction(gc, lc);
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
    public LargePowerMonomial sign(){return new LargePowerMonomial(LargeFraction.negativeOne, exponent2);}
    @Override
    public LargePowerMonomial magnitude(){return new LargePowerMonomial(base, exponent);}
    @Override
    public long compareMags(ScalarAtom other) {return this.magnitude().compareTo(other.magnitude());}
    @Override
    public long compareTo(ScalarAtom other) {
        if(this.isNotReal()) throw new IllegalStateException("Error: Complex numbers cannot be compared");
        if(other instanceof LargeFraction || other instanceof LargeInteger){
            LargePowerMonomial copy = new LargePowerMonomial(base, exponent);
            LargePowerMonomial copy2 = new LargePowerMonomial((LargeFraction) other.divide(LargeInteger.one), LargeFraction.one);
            LargeFraction pow = copy.exponent.denominator.power(LargeInteger.one);
            copy = copy.power(pow);
            copy2 = copy2.power(pow);
            return copy.base.compareTo(copy2.base);
        }
        if(other instanceof LargePowerMonomial){
            if(other.isNotReal()) throw new IllegalArgumentException("Error: Complex numbers cannot be compared");
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
            if(other.isNotReal()) throw new IllegalArgumentException("Error: Complex numbers cannot be compared");
            //I present to you...ERROR BOUNDS
            if(((RadicalTerm) other).coefficient.equalsTo(LargeInteger.zero)) return this.compareTo(((RadicalTerm) other).rationalPart);
            LargeFraction l1 = (LargeFraction) this.floor().divide(LargeInteger.one);
            LargeFraction u1 = (LargeFraction) this.ceil().divide(LargeInteger.one);
            LargeFraction l2 = (LargeFraction) other.floor().divide(LargeInteger.one);
            LargeFraction u2 = (LargeFraction) other.ceil().divide(LargeInteger.one);
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
    public ScalarAtom negate() {return new RadicalTerm(LargeFraction.zero, LargeFraction.negativeOne, this);}
    @Override
    public boolean isReal() {return !isNotReal();}
    @Override
    public boolean isRational(){return exponent2.denominator.equalsTo(LargeFraction.one) && exponent.equalsTo(LargeFraction.one);}
    @Override
    public boolean isNotReal(){return exponent2.notEqualsTo(LargeFraction.one) && exponent2.notEqualsTo(LargeFraction.zero);}
    public boolean isNotInteger() {return exponent.notEqualsTo(LargeInteger.one) || base.denominator.notEqualsTo(LargeInteger.one) || this.isNotReal();}
    public LargePowerMonomial conjugate(){return new LargePowerMonomial(base, exponent, (LargeFraction) exponent2.add(LargeInteger.one));}
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
        if(other instanceof LargeInteger || other instanceof LargeFraction || other instanceof LargePowerMonomial){
            LargeList<PrimeExponent> pr = this.factor();
            LargeList<PrimeExponent> pr2 = ((LargePowerMonomial)other.divide(LargeInteger.one).power(LargeFraction.one)).factor();
            for(PrimeExponent p: pr2)
                pr.add(p.copy());
            PrimeExponent.sortBases(pr);
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
    @Override
    public RadicalTerm percent(){return new RadicalTerm(LargeFraction.zero, (LargeFraction) Parser.parse("0.01"), this);}
    @Override
    public ScalarAtom permille(){return new RadicalTerm(LargeFraction.zero, (LargeFraction) Parser.parse("0.001"), this);}
    public LargePowerMonomial power(LargeFraction f){return new LargePowerMonomial(base, (LargeFraction) exponent.multiply(f), (LargeFraction) exponent2.multiply(f));}
    public LargeList<PrimeExponent> factor(){
        LargeList<PrimeExponent> pr = base.factor();
        for(LargeInteger i = LargeInteger.zero; i.lessThan(pr.size()); i = i.add(1))
            pr.set(i, new PrimeExponent(pr.get(i).base, (LargeFraction) pr.get(i).exponent.multiply(exponent)));
        if(base.notEqualsTo(LargeInteger.zero) && exponent2.notEqualsTo(LargeInteger.zero)) pr.addFirst(new PrimeExponent(LargeInteger.negativeOne, exponent2));
        while(pr.size().greaterThan(LargeInteger.one) && pr.getLast().base.equalsTo(LargeInteger.one)) pr.removeLast();
        return pr;
    }
    public static LargePowerMonomial buildFromFactors(LargeList<PrimeExponent> pr){
        LargeFraction base = LargeFraction.one;
        LargeFraction exponent2 = LargeFraction.zero;
        if(pr.size().equalsTo(LargeInteger.zero)) return new LargePowerMonomial(base, LargeFraction.one);
        else if(pr.size().equalsTo(LargeInteger.one)) return new LargePowerMonomial((LargeFraction) pr.getFirst().base.divide(LargeInteger.one), pr.getFirst().exponent);
        LargeInteger lc = pr.getFirst().exponent.denominator;
        for(PrimeExponent p: pr){
            if(p.base.notEqualsTo(LargeInteger.negativeOne))
                lc = lc.lcm(p.exponent.denominator);
        }
        for(PrimeExponent p: pr){
            if(p.base.equalsTo(LargeInteger.negativeOne)){
                exponent2 = (LargeFraction) exponent2.add(p.exponent);
                continue;
            }
            LargeInteger expTemp = (LargeInteger) p.exponent.numerator.multiply(lc.longDivision(p.exponent.denominator));
            LargeInteger basTemp = p.base.copy();
            while(expTemp.notEqualsTo(LargeInteger.zero)){
                if(expTemp.isOdd()){
                    if(expTemp.sign().equalsTo(LargeInteger.negativeOne))
                        base = (LargeFraction) base.divide(basTemp);
                    else base = (LargeFraction) base.multiply(basTemp);
                    if(expTemp.magnitude().equalsTo(LargeInteger.one)) break;
                }
                basTemp = basTemp.square();
                expTemp = expTemp.longDivision(LargeInteger.two);
            }
        }
        pr.addFirst(new PrimeExponent(LargeInteger.negativeOne, exponent2));
        return new LargePowerMonomial(base, lc.reciprocal(), exponent2);
    }
    @Override
    public ScalarAtom divide(ScalarAtom other){return other.reciprocal().multiply(this);}
    @Override
    public LargePowerMonomial reciprocal(){return new LargePowerMonomial(base, exponent.negate(), exponent2.negate());}
    public ScalarAtom mod(ScalarAtom other) {return this.subtract(other.multiply(this.divide(other).floor()));}
}
