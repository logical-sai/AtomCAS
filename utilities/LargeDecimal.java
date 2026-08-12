package com.AtomCAS.utilities;
public class LargeDecimal extends ScalarAtom{
    LargeInteger mantissa;
    long scale;
    public LargeDecimal(LargeInteger mantissa, long scale){
        this.mantissa = mantissa;
        this.scale = scale;
    }
    @Override
    public Atom copy() {return new LargeDecimal(mantissa, scale);}
    @Override
    public String toString() {
        if (mantissa.equals(LargeInteger.ZERO))
            return "0";
        boolean negative = mantissa.sign().equals(LargeInteger.NEGATIVE_ONE);
        String digits = mantissa.abs().toString();
        StringBuilder sb = new StringBuilder();
        if (negative) sb.append("-");
        // scale == 0: ordinary integer
        if (scale == 0) {
            sb.append(digits);
            return sb.toString();
        }
        // scale < 0: append zeros to the right
        if (scale < 0) {
            sb.append(digits);
            long zeros = -scale;
            for (long i = 0; i <= zeros; i++)
                sb.append("0");
            return sb.toString();
        }

        // scale > 0: decimal point goes scale digits from the right
        int digitCount = digits.length();

        if (scale < digitCount) {
            int pointIndex = Math.toIntExact(digitCount - scale);
            sb.append(digits, 0, pointIndex);
            sb.append(".");
            sb.append(digits.substring(pointIndex));
        } else {
            sb.append("0.");
            long leadingZeros = scale - digitCount;
            for (long i = 0; i <= leadingZeros; i++)
                sb.append("0");
            sb.append(digits);
        }
        return sb.toString();
    }
    public int hashCode(){
        int hash = 0;
        hash = 31 * hash + mantissa.hashCode();
        return hash;
    }
    public String toLatex(){return "";}
    @Override
    public boolean equals(Atom other) {
        return false;
    }
    @Override
    public LargeInteger floor() {
        return null;
    }
    @Override
    public LargeInteger ceil() {
        return null;
    }

    @Override
    public int compareMags(ScalarAtom other) {
        return 0;
    }

    @Override
    public int compareTo(ScalarAtom other) {
        return 0;
    }

    @Override
    public ScalarAtom negate() {
        return null;
    }

    @Override
    public ScalarAtom abs() {
        return null;
    }

    @Override
    public boolean isRational() {
        return false;
    }

    @Override
    public boolean isReal() {
        return false;
    }

    @Override
    public boolean isAlgebraic() {
        return false;
    }

    @Override
    public ScalarAtom realPart() {
        return null;
    }

    @Override
    public ScalarAtom imagPart() {
        return null;
    }

    @Override
    public ScalarAtom add(ScalarAtom other) {
        return null;
    }

    @Override
    public ScalarAtom multiply(ScalarAtom other) {
        return null;
    }

    @Override
    public ScalarAtom reciprocal() {
        return null;
    }

    @Override
    public ScalarAtom square() {
        return null;
    }

    @Override
    public ScalarAtom cube() {
        return null;
    }

    @Override
    public ScalarAtom power(LargeFraction f) {
        return null;
    }

    @Override
    public LargeDecimal approximateToDecimal(long precision) {
        return this;
    }
}
