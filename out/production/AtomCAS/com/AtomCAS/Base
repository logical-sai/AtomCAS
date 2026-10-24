package com.AtomCAS.utilities;
public class Base{
    LargeList<LargeInteger> intPart;
    LargeList<LargeInteger> terminatingPart;
    LargeList<LargeInteger> repeatingPart;
    boolean sign;
    LargeInteger base;
    public Base(LargeFraction N, LargeInteger b) {
        if (b.lesserThanOrEqualTo(LargeInteger.one)) throw new IllegalArgumentException("Error: Invalid base");
        //integer part
        intPart = new LargeList<>();
        LargeInteger copy = N.intPart().magnitude();
        sign = N.sign().equalsTo(LargeInteger.negativeOne);
        do{
            intPart.addFirst((LargeInteger) copy.mod(b));
            copy = copy.longDivision(b);
        }while(copy.greaterThan(LargeInteger.zero));
        //terminating part
        terminatingPart = new LargeList<>();
        LargeFraction copy2 = N.fracPart().magnitude();
        LargeInteger iterCount = LargeInteger.zero;
        while(copy2.denominator.gcd(b).notEqualsTo(LargeInteger.one) && copy2.notEqualsTo(LargeInteger.zero)){
            copy2 = (LargeFraction) copy2.multiply(b);
            iterCount = (LargeInteger) iterCount.add(LargeInteger.one);
        }
        LargeInteger ter = copy2.intPart();
        copy2 = copy2.fracPart();
        for(LargeInteger i = LargeInteger.zero; i.lessThan(iterCount); i = i.add(1)){
            terminatingPart.addFirst((LargeInteger) ter.mod(b));
            ter = ter.longDivision(b);
        }
        //repeating part
        repeatingPart = new LargeList<>();
        LargeInteger rem = copy2.numerator;
        if(rem.notEqualsTo(LargeInteger.zero)){
            do{
                repeatingPart.add(((LargeInteger) rem.multiply(b)).longDivision(copy2.denominator));
                rem = (LargeInteger) rem.multiply(b).mod(copy2.denominator);
            }while(rem.notEqualsTo(copy2.numerator));
        }
        base = b;
    }
    public String toString(){
        StringBuilder sb = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        if (sign) sb.append('-');
        //integer part
        for (LargeInteger i = LargeInteger.zero; i.lessThan(intPart.size()); i= (LargeInteger) i.add(LargeInteger.one)){
            LargeInteger bit = intPart.get(i);
            if(base.lessThan(LargeInteger.ten)) sb.append(bit);
            else if(base.greaterThanOrEqualTo(LargeInteger.ten) && base.lesserThanOrEqualTo(new LargeInteger(chars.length()+10)))
                if(bit.greaterThanOrEqualTo(LargeInteger.ten)) sb.append(chars.charAt(Integer.parseInt(bit.toString())-10));
                else sb.append(bit);
            else{
                sb.append(bit);
                if(i.lessThan(intPart.size().subtract(LargeInteger.one))) sb.append(",");
            }
        }
        if(intPart.size().equalsTo(LargeInteger.zero)) sb.append("0");
        //terminating part
        if(terminatingPart.size().greaterThan(LargeInteger.zero) || repeatingPart.size().greaterThan(LargeInteger.zero)) sb.append(".");
        for (LargeInteger i = LargeInteger.zero; i.lessThan(terminatingPart.size()); i= (LargeInteger) i.add(LargeInteger.one)){
            LargeInteger bit = terminatingPart.get(i);
            if(base.lessThan(LargeInteger.ten)) sb.append(bit);
            else if(base.greaterThanOrEqualTo(LargeInteger.ten) && base.lesserThanOrEqualTo(new LargeInteger(chars.length()+10)))
                if(bit.greaterThanOrEqualTo(LargeInteger.ten)) sb.append(chars.charAt(Integer.parseInt(bit.toString())-10));
                else sb.append(bit);
            else{
                sb.append(bit);
                if(i.lessThan(terminatingPart.size().subtract(LargeInteger.one)) || repeatingPart.size().greaterThan(LargeInteger.zero)) sb.append(",");
            }
        }
        //repeating part
        if(repeatingPart.size().greaterThan(LargeInteger.zero)){
            sb.append("{");
            for (LargeInteger i = LargeInteger.zero; i.lessThan(repeatingPart.size()); i= (LargeInteger) i.add(LargeInteger.one)){
                LargeInteger bit = repeatingPart.get(i);
                if(base.lessThan(LargeInteger.ten)) sb.append(bit);
                else if(base.greaterThanOrEqualTo(LargeInteger.ten) && base.lesserThanOrEqualTo(new LargeInteger(chars.length()+10)))
                    if(bit.greaterThanOrEqualTo(LargeInteger.ten)) sb.append(chars.charAt(Integer.parseInt(bit.toString())-10));
                    else sb.append(bit);
                else{
                    sb.append(bit);
                    if(i.lessThan(repeatingPart.size().subtract(LargeInteger.one))) sb.append(",");
                }
            }
            sb.append("}");
        }
        if(base.notEqualsTo(LargeInteger.ten)) sb.append(":").append(base);
        return sb.toString();
    }
    public Base convertTo(LargeInteger b){
        if(b.lesserThanOrEqualTo(LargeInteger.one))
            throw new IllegalArgumentException("Error: Invalid base");
        LargeFraction result = LargeFraction.zero;
        //integer part
        for (LargeInteger digit : intPart)
            result = (LargeFraction) result.multiply(base).add(digit);
        // terminating fractional part
        LargeInteger termValue = LargeInteger.zero;
        for (LargeInteger digit : terminatingPart)
            termValue = (LargeInteger) termValue.multiply(base).add(digit);
        LargeInteger basePowT = LargeInteger.one;
        for (LargeInteger i = LargeInteger.zero; i.lessThan(terminatingPart.size()); i = (LargeInteger) i.add(LargeInteger.one))
            basePowT = (LargeInteger) basePowT.multiply(base);
        result = (LargeFraction) result.add(new LargeFraction(termValue, basePowT));
        // repeating fractional part
        if (repeatingPart.size().greaterThan(LargeInteger.zero)) {
            LargeInteger repeatValue = LargeInteger.zero;
            for (LargeInteger digit : repeatingPart)
                repeatValue = (LargeInteger) repeatValue.multiply(base).add(digit);
            LargeInteger basePowR = LargeInteger.one;
            for (LargeInteger i = LargeInteger.zero; i.lessThan(repeatingPart.size()); i = (LargeInteger) i.add(LargeInteger.one))
                basePowR = (LargeInteger) basePowR.multiply(base);
            LargeInteger repeatDenom = (LargeInteger) basePowT.multiply(basePowR.subtract(LargeInteger.one));
            result = (LargeFraction) result.add(new LargeFraction(repeatValue, repeatDenom));
        }
        if (sign)
            result = result.negate();
        return new Base(result, b);
    }
}
