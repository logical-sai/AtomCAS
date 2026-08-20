package com.AtomCAS.utilities;
public class LargeDecimal extends ScalarAtom{
    boolean sign;
    LargeList<Long> intPart; //little-endian; base 2^64
    LargeList<Long> decPart; //big-endian; base 10^18
    public LargeDecimal(LargeList<Long> intPart, LargeList<Long> decPart, boolean sign) {
        this.intPart = intPart;
        this.decPart = decPart;
        this.sign = isNonZeroList(intPart) && isNonZeroList(decPart) && sign;
    }
    private static boolean isNonZeroList(LargeList<Long> list) {
        while(list.size().greaterThan(LargeInteger.ONE) && list.getLast() == 0) list.removeLast();
        if(list.intSized()) return list.intSize() != 1 || list.getFirst() != 0;
        return true;
    }
    @Override
    public Atom copy() {return new LargeDecimal(intPart, decPart, sign);}
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (sign) sb.append('-');
        appendIntegerDecimal(sb, intPart);
        if (!decPart.size().doesntEqual(LargeInteger.ZERO)) {
            sb.append('.');
            for (Long chunk : decPart){
                String s = Long.toString(chunk);
                sb.repeat("0", 18 - s.length());
                sb.append(s);
            }
        }
        return sb.toString();
    }
    private static void appendIntegerDecimal(StringBuilder sb, LargeList<Long> intPart) {

    }
    public String toLatex(){return toString();}
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
        if(!sign) return this;
        return new LargeDecimal(intPart, decPart, false);
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
    public LargeDecimal approximateToDecimal(LargeInteger precision) {
        return this;
    }
}
