package com.AtomCAS.utilities;
public final class PrimeExponent{
    public final LargeInteger base;
    public final LargeFraction exponent;
    public PrimeExponent(LargeInteger b, LargeFraction e){
        base = b.copy();
        if(base.notEqualsTo(LargeInteger.one)) exponent = e.copy();
        else exponent = LargeFraction.one;
    }
    public String toString(){
        if(exponent.equalsTo(LargeInteger.one)) return base.toString();
        return base + "^(" + exponent + ")";}
    public PrimeExponent copy(){return new PrimeExponent(base.copy(), exponent.copy());}
    public static void sortBases(LargeList<PrimeExponent> l) {
        for (LargeInteger i = LargeInteger.zero; i.lessThan(l.size().subtract(LargeInteger.one)); i= (LargeInteger) i.add(LargeInteger.one)) {
            for (LargeInteger j = (LargeInteger) i.add(LargeInteger.one); j.lessThan(l.size()); j= (LargeInteger) j.add(LargeInteger.one)) {
                if (l.get(i).base.greaterThan(l.get(j).base)) {
                    PrimeExponent temp = l.get(i);
                    l.set(i, l.get(j));
                    l.set(j, temp);
                }
            }
        }
    }
}
