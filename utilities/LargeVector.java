package com.AtomCAS.utilities;
public class LargeVector extends TensorAtom {
    private LargeList<LargeInteger> list;
    public LargeFraction factor;
    public LargeVector(LargeList<LargeInteger> list, LargeFraction factor){
        this.list = new LargeList<>();
        if(list.size().equals(LargeInteger.ZERO)){
            this.factor = LargeFraction.ONE;
            return;
        }
        this.factor = factor;
        for(LargeInteger I: list)
            this.list.add(I);
        LargeInteger lc = list.getFirst();
        for(LargeInteger I: list) lc = lc.gcd(I);
        if(lc.equals(LargeInteger.ZERO)){
            this.factor = LargeFraction.ONE;
            return;
        }
        this.factor = (LargeFraction) this.factor.multiply(lc);
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.list.size()); i = i.add(1))
            this.list.set(i, this.list.get(i).divide(lc));
    }
    public LargeVector(LargeList<LargeFraction> liste){
        this.list = new LargeList<>();
        LargeInteger lc = liste.getFirst().denominator;
        for(LargeFraction f: liste) lc = lc.lcm(f.denominator);
        for(LargeFraction f: liste) this.list.add(((LargeFraction)f.multiply(lc)).numerator);
        this.factor = lc.reciprocal();
    }
    public LargeVector(LargeInteger dimension){
        list = new LargeList<>();
        factor = LargeFraction.ONE;
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(dimension); i = i.add(1))
            list.add(LargeInteger.ZERO);
    }
    public LargeVector() {
        list = new LargeList<>();
        factor = LargeFraction.ONE;
    }
    public LargeVector clone(){return new LargeVector(list,factor);}
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append((char) 0x3008);
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(list.size()); i = i.add(1)){
            sb.append(list.get(i).multiply(factor));
            if(i.lessThan(list.size().subtract(1))) sb.append(", ");
        }
        sb.append((char) 0x3009);
        return sb.toString();
    }
    @Override
    public String toLatex() {
        return "";
    }
    public LargeList<LargeInteger> rawList(){
        LargeList<LargeInteger> bf = new LargeList<>();
        for(LargeInteger I: list)
            bf.add(I.clone());
        return bf;
    }
    public LargeList<LargeFraction> list(){
        LargeList<LargeFraction> bf = new LargeList<>();
        for(LargeInteger I: list)
            bf.add((LargeFraction) I.multiply(factor));
        return bf;
    }
    @Override
    public LargeVector negate() {return new LargeVector(list, factor.negate());}
    @Override
    public LargeInteger[] shape() {return new LargeInteger[]{this.dimension()};}
    public LargeFraction get(LargeInteger index){return (LargeFraction) list.get(index).multiply(factor);}
    public void append(LargeFraction f){
        LargeList<LargeFraction> lk = new  LargeList<>();
        for(LargeInteger I: this.list)
            lk.add((LargeFraction) I.multiply(factor));
        lk.add(f);
        LargeVector vK = new LargeVector(lk);
        list = vK.list;
        factor = vK.factor;
    }
    public void set(LargeInteger index, LargeFraction f){
        LargeList<LargeFraction> lk = new LargeList<>();
        for(LargeInteger I: this.list)
            lk.add((LargeFraction) I.multiply(factor));
        lk.set(index, f);
        LargeVector vK = new LargeVector(lk);
        list = vK.list;
        factor = vK.factor;
    }
    public LargeFraction sum(){
        LargeFraction res = LargeFraction.ZERO;
        for(LargeInteger I: this.list) res = (LargeFraction) res.add(I.multiply(factor));
        return res;
    }
    public LargeFraction product(){
        LargeFraction res = LargeFraction.ONE;
        for(LargeInteger I: this.list) res = (LargeFraction) res.multiply(I.multiply(factor));
        return res;
    }
    public void remove(LargeInteger index){list.remove(index);}
    public LargeInteger dimension(){return list.size();}
    public boolean isDifferentDimension(LargeVector other){return !this.dimension().equals(other.dimension());}
    public boolean isZero() {
        for (LargeInteger f: list) if (f.doesntEqual(LargeInteger.ZERO)) return false;
        return true;
    }
    @SuppressWarnings("unused")
    public boolean isUnit(){return this.magnitude().equals(LargeInteger.ONE);}
    public LargeVector add(LargeVector other){
        if(other.isDifferentDimension(this)) throw new IllegalStateException("Dimension mismatch");
        LargeList<LargeFraction> liste = new LargeList<>();
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.dimension()); i = i.add(1))
            liste.add((LargeFraction) list.get(i).multiply(factor).add(other.list.get(i).multiply(other.factor)));
        return new LargeVector(liste);
    }
    @Override
    public TensorAtom add(TensorAtom other) {
        if(other instanceof LargeVector LV)
            return add(LV);
        throw new UnsupportedOperationException("Operation with these types together not supported yet");
    }
    public LargeVector subtract(LargeVector other){
        if(other.isDifferentDimension(this)) throw new IllegalStateException("Dimension mismatch");
        LargeList<LargeFraction> liste = new LargeList<>();
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.dimension()); i = i.add(1))
            liste.add((LargeFraction) list.get(i).multiply(factor).subtract(
                    other.list.get(i).multiply(other.factor)));
        return new LargeVector(liste);
    }
    @Override
    public TensorAtom subtract(TensorAtom other) {
        if(other instanceof LargeVector lv) return subtract(lv);
        else throw new UnsupportedOperationException("Operation with these types together not supported yet");
    }
    public LargeVector hadamardProduct(LargeVector LV){
        if (LV.isDifferentDimension(this))
            throw new IllegalArgumentException("Dimension mismatch");
        LargeList<LargeInteger> result = new LargeList<>();
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(this.dimension()); i = i.add(1))
            result.add((LargeInteger) this.get(i).multiply(LV.get(i)));
        return new LargeVector(result, (LargeFraction) this.factor.multiply(LV.factor));
    }
    @Override
    public TensorAtom multiply(TensorAtom other) {
        if(other instanceof LargeVector LV) return hadamardProduct(LV);
        throw new UnsupportedOperationException("Operation with these types together not supported yet");
    }
    public LargeFraction dotProduct(LargeVector other){
        if (other.isDifferentDimension(this))
            throw new IllegalArgumentException("Dimension mismatch");
        LargeFraction result = LargeFraction.ZERO;
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(dimension()); i = i.add(1))
            result = (LargeFraction) result.add(get(i).multiply(other.get(i)));
        return result;
    }
    public LargeVector crossProduct(LargeVector v) {
        if (dimension().doesntEqual(LargeInteger.THREE) || v.dimension().doesntEqual(LargeInteger.THREE))
            throw new IllegalArgumentException("Cross product only defined for 3D vectors");
        LargeFraction a1 = (LargeFraction) list.getFirst().multiply(factor), a2 = (LargeFraction) list.get(LargeInteger.ONE).multiply(factor), a3 = (LargeFraction) list.get(LargeInteger.TWO).multiply(factor);
        LargeFraction b1 = (LargeFraction) v.list.getFirst().multiply(v.factor), b2 = (LargeFraction) v.list.get(LargeInteger.ONE).multiply(v.factor), b3 = (LargeFraction) v.list.get(LargeInteger.TWO).multiply(v.factor);
        LargeList<LargeFraction> result = new LargeList<>();
        result.add((LargeFraction) a2.multiply(b3).subtract(a3.multiply(b2)));
        result.add((LargeFraction) a3.multiply(b1).subtract(a1.multiply(b3)));
        result.add((LargeFraction) a1.multiply(b2).subtract(a2.multiply(b1)));
        return new LargeVector(result);
    }
    public LargeVector projectionOnto(LargeVector v) {
        LargeFraction denom = v.dotProduct(v);
        if (denom.equals(LargeInteger.ZERO))
            throw new ArithmeticException("Cannot project onto ZERO vector");
        LargeFraction factor = (LargeFraction) this.dotProduct(v).divide(denom);
        return v.scale(factor);
    }
    @Override
    public LargeVector scale(LargeFraction scalar) {return new LargeVector(this.list, (LargeFraction) scalar.multiply(factor));}
    @Override
    public boolean equals(Atom other) {
        if(other instanceof  LargeVector){
            if (((LargeVector) other).isDifferentDimension(this))
                return false;
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.dimension()); i = i.add(1))
                if(this.list.get(i).doesntEqual(((LargeVector) other).list.get(i))) return false;
            return this.factor.equals(((LargeVector) other).factor);
        }
        return false;
    }
    @SuppressWarnings("unused")
    public boolean isPerpendicularTo(LargeVector other){return dotProduct(other).equals(LargeFraction.ZERO);}
    @SuppressWarnings("unused")
    public boolean isParallelTo(LargeVector other){
        if (this.dimension() != other.dimension()) return false;
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.dimension()); i = i.add(1))
            if(this.list.get(i).mod(other.list.get(i)).doesntEqual(LargeInteger.ZERO)
                    && other.list.get(i).mod(list.get(i)).doesntEqual(LargeInteger.ZERO))
                return false;
        return true;
    }
    public boolean isOrthonormalTo(LargeVector other){
        return this.dotProduct(other).equals(LargeFraction.ZERO)
                && this.magnitude().equals(LargeInteger.ONE) && other.magnitude().equals(LargeInteger.ONE);}
    public RadicalTerm magnitude(){
        return new RadicalTerm(LargeFraction.ZERO, LargeFraction.ONE,
                this.dotProduct(this).power(LargeFraction.HALF));}
    @SuppressWarnings("unused")
    public RadicalTerm distanceTo(LargeVector other){return subtract(other).magnitude();}
    @SuppressWarnings("unused")
    public static LargeVector standardBasis(LargeInteger dimension, LargeInteger index) {
        if (dimension.lessThan(LargeInteger.ZERO))
            throw new IllegalArgumentException("Dimension cannot be negative");
        if (index.lessThan(LargeInteger.ZERO) || index.greaterThanOrEqualTo(dimension))
            throw new IndexOutOfBoundsException("Basis index out of range");
        LargeList<LargeFraction> values = new LargeList<>();
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(dimension); i = i.add(1)){
            if (i.equals(index)) values.add(LargeFraction.ONE);
            else values.add(LargeFraction.ZERO);
        }
        return new LargeVector(values);
    }
    @SuppressWarnings("unused")
    public LargeMatrix outerProduct(LargeVector other) {
        LargeList<LargeList<LargeFraction>> rows = new LargeList<>();
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(this.dimension()); i = i.add(1)) {
            LargeList<LargeFraction> row = new LargeList<>();
            for (LargeInteger j = LargeInteger.ZERO; j.lessThan(other.dimension()); j = j.add(1))
                row.add((LargeFraction) this.get(i).multiply(other.get(j)));
            rows.add(row);
        }
        return new LargeMatrix(rows);
    }
}
