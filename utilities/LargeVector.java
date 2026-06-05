package com.AtomCAS.utilities;
public class LargeVector extends TensorAtom {
    private LargeList<LargeInteger> list;
    LargeFraction factor;
    public LargeVector(LargeList<LargeInteger> list, LargeFraction factor){
        this.list = new LargeList<>();
        if(list.size().equalsTo(LargeInteger.zero)){
            this.factor = LargeFraction.one;
            return;
        }
        this.factor = factor.copy();
        for(LargeInteger I: list)
            this.list.add(I.copy());
        LargeInteger lc = list.getFirst();
        for(LargeInteger I: list) lc = lc.gcd(I);
        if(lc.equalsTo(LargeInteger.zero)){
            this.factor = LargeFraction.one;
            return;
        }
        this.factor = (LargeFraction) this.factor.multiply(lc);
        for(LargeInteger i = LargeInteger.zero; i.lessThan(this.list.size()); i = i.add(1))
            this.list.set(i, this.list.get(i).longDivision(lc));
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
        factor = LargeFraction.one;
        for(LargeInteger i = LargeInteger.zero; i.lessThan(dimension); i = i.add(1))
            list.add(LargeInteger.zero);
    }
    public LargeVector() {
        list = new LargeList<>();
        factor = LargeFraction.one;
    }
    public LargeVector copy(){return new LargeVector(list,factor);}
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for(LargeInteger i = LargeInteger.zero; i.lessThan(this.list.size()); i = i.add(1)){
            sb.append(this.list.get(i).multiply(factor));
            if(i.lessThan(this.list.size().subtract(LargeInteger.one))) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
    public LargeList<LargeInteger> rawList(){
        LargeList<LargeInteger> bf = new LargeList<>();
        for(LargeInteger I: list)
            bf.add(I.copy());
        return bf;
    }
    public LargeList<LargeFraction> daList(){
        LargeList<LargeFraction> bf = new LargeList<>();
        for(LargeInteger I: list)
            bf.add((LargeFraction) I.multiply(factor));
        return bf;
    }
    @Override
    public LargeVector negate() {return new LargeVector(list, factor.negate());}
    @Override
    public LargeInteger[] shape() {
        return new LargeInteger[]{this.dimension()};
    }
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
        LargeList<LargeFraction> lk = new  LargeList<>();
        for(LargeInteger I: this.list)
            lk.add((LargeFraction) I.multiply(factor));
        lk.set(index, f);
        LargeVector vK = new LargeVector(lk);
        list = vK.list;
        factor = vK.factor;
    }
    public LargeFraction sum(){
        LargeFraction res = LargeFraction.zero;
        for(LargeInteger I: this.list) res = (LargeFraction) res.add(I.multiply(factor));
        return res;
    }
    public LargeFraction product(){
        LargeFraction res = LargeFraction.one;
        for(LargeInteger I: this.list) res = (LargeFraction) res.multiply(I.multiply(factor));
        return res;
    }
    public void remove(LargeInteger index){list.remove(index);}
    public LargeInteger dimension(){return list.size();}
    public boolean isDifferentDimension(LargeVector other){return !this.dimension().equalsTo(other.dimension());}
    public boolean isZero() {
        for (LargeInteger f: list) if (f.notEqualsTo(LargeInteger.zero)) return false;
        return true;
    }
    public boolean isUnit(){return this.magnitude().equalsTo(LargeInteger.one);}
    @Override
    public TensorAtom add(TensorAtom other) {
        if(other instanceof LargeVector){
            if(((LargeVector) other).isDifferentDimension(this)) throw new IllegalStateException("Error: Dimension mismatch");
            LargeList<LargeFraction> liste = new LargeList<>();
            for(LargeInteger i = LargeInteger.zero; i.lessThan(this.dimension()); i = i.add(1))
                liste.add((LargeFraction) list.get(i).multiply(factor).add(((LargeVector) other).list.get(i).multiply(((LargeVector) other).factor)));
            return new LargeVector(liste);
        }
        throw new IllegalArgumentException("Error: Operation with these types together not supported yet");
    }
    @Override
    public TensorAtom subtract(TensorAtom other) {
        if(other instanceof LargeVector){
            if(((LargeVector) other).isDifferentDimension(this)) throw new IllegalStateException("Error: Dimension mismatch");
            LargeList<LargeFraction> liste = new LargeList<>();
            for(LargeInteger i = LargeInteger.zero; i.lessThan(this.dimension()); i = i.add(1))
                liste.add((LargeFraction) list.get(i).multiply(factor).subtract(((LargeVector) other).list.get(i).multiply(((LargeVector) other).factor)));
            return new LargeVector(liste);
        }
        throw new IllegalArgumentException("Error: Operation with these types together not supported yet");
    }
    @Override
    public TensorAtom multiply(TensorAtom other) {
        if(other instanceof LargeVector){
            if (((LargeVector) other).isDifferentDimension(this))
                throw new IllegalArgumentException("Error: Dimension mismatch");
            LargeList<LargeInteger> result = new LargeList<>();
            for (LargeInteger i = LargeInteger.zero; i.lessThan(this.dimension()); i = (LargeInteger) i.add(LargeInteger.one))
                result.add((LargeInteger) this.get(i).multiply(((LargeVector) other).get(i)));
            return new LargeVector(result, (LargeFraction) this.factor.multiply(((LargeVector) other).factor));
        }
        throw new IllegalArgumentException("Error: Operation with these types together not supported yet");
    }
    public LargeFraction dotProduct(LargeVector other){
        if (other.isDifferentDimension(this))
            throw new IllegalArgumentException("Error: Dimension mismatch");
        LargeFraction result = LargeFraction.zero;
        for (LargeInteger i = LargeInteger.zero; i.lessThan(this.dimension()); i = (LargeInteger) i.add(LargeInteger.one))
            result = (LargeFraction) result.add(this.get(i).multiply(other.get(i)));
        result = (LargeFraction) result.multiply(this.factor).multiply(other.factor);
        return result;
    }
    public LargeVector crossProduct(LargeVector v) {
        if (this.dimension().notEqualsTo(LargeInteger.three) || v.dimension().notEqualsTo(LargeInteger.three))
            throw new IllegalArgumentException("Error: Cross product only defined for 3D vectors");
        LargeFraction a1 = (LargeFraction) list.getFirst().multiply(factor), a2 = (LargeFraction) list.get(LargeInteger.one).multiply(factor), a3 = (LargeFraction) list.get(LargeInteger.two).multiply(factor);
        LargeFraction b1 = (LargeFraction) v.list.getFirst().multiply(v.factor), b2 = (LargeFraction) v.list.get(LargeInteger.one).multiply(v.factor), b3 = (LargeFraction) v.list.get(LargeInteger.two).multiply(v.factor);
        LargeList<LargeFraction> result = new LargeList<>();
        result.add((LargeFraction) a2.multiply(b3).subtract(a3.multiply(b2)));
        result.add((LargeFraction) a3.multiply(b1).subtract(a1.multiply(b3)));
        result.add((LargeFraction) a1.multiply(b2).subtract(a2.multiply(b1)));
        return new LargeVector(result);
    }
    public LargeVector projectionOnto(LargeVector v) {
        LargeFraction denom = v.dotProduct(v);
        if (denom.equalsTo(LargeInteger.zero))
            throw new ArithmeticException("Error: Cannot project onto zero vector");
        LargeFraction factor = (LargeFraction) this.dotProduct(v).divide(denom);
        return v.scale(factor);
    }
    @Override
    public LargeVector scale(LargeFraction scalar) {return new LargeVector(this.list, (LargeFraction) scalar.multiply(factor));}
    @Override
    public boolean equalsTo(Atom other) {
        if(other instanceof  LargeVector){
            if (((LargeVector) other).isDifferentDimension(this))
                return false;
            for(LargeInteger i = LargeInteger.zero; i.lessThan(this.dimension()); i = i.add(1))
                if(this.list.get(i).notEqualsTo(((LargeVector) other).list.get(i))) return false;
            return this.factor.equalsTo(((LargeVector) other).factor);
        }
        return false;
    }
    @Override
    public boolean notEqualsTo(Atom other) {return !this.equalsTo(other);}
    public boolean isPerpendicularTo(LargeVector other){return this.dotProduct(other).equalsTo(LargeFraction.zero);}
    public boolean isParallelTo(LargeVector other){
        if (this.dimension() != other.dimension()) return false;
        for(LargeInteger i = LargeInteger.zero; i.lessThan(this.dimension()); i = i.add(1))
            if(this.list.get(i).mod(other.list.get(i)).notEqualsTo(LargeInteger.zero) && other.list.get(i).mod(this.list.get(i)).notEqualsTo(LargeInteger.zero)) return false;
        return true;
    }
    public boolean isOrthonormalTo(LargeVector other){return this.dotProduct(other).equalsTo(LargeFraction.zero) && this.magnitude().equalsTo(LargeInteger.one) && other.magnitude().equalsTo(LargeInteger.one);}
    public RadicalTerm magnitude(){return new RadicalTerm(LargeFraction.zero, LargeFraction.one, this.dotProduct(this).power(LargeFraction.half));}
    public RadicalTerm distanceTo(LargeVector other){return ((LargeVector)this.subtract(other)).magnitude();}
    public static LargeVector standardBasis(LargeInteger dimension, LargeInteger index) {
        if (dimension.sign().equalsTo(LargeInteger.negativeOne))
            throw new IllegalArgumentException("Error: Dimension cannot be negative");
        if (index.sign().equalsTo(LargeInteger.negativeOne) || index.greaterThanOrEqualTo(dimension))
            throw new IndexOutOfBoundsException("Error: Basis index out of range");
        LargeList<LargeFraction> values = new LargeList<>();
        for (LargeInteger i = LargeInteger.zero; i.lessThan(dimension); i = (LargeInteger) i.add(LargeInteger.one)){
            if (i.equalsTo(index)) values.add(LargeFraction.one);
            else values.add(LargeFraction.zero);
        }
        return new LargeVector(values);
    }
    public LargeMatrix outerProduct(LargeVector other) {
        LargeList<LargeList<LargeFraction>> rows = new LargeList<>();
        for (LargeInteger i = LargeInteger.zero; i.lessThan(this.dimension()); i = (LargeInteger) i.add(LargeInteger.one)) {
            LargeList<LargeFraction> row = new LargeList<>();
            for (LargeInteger j = LargeInteger.zero; j.lessThan(other.dimension()); j = (LargeInteger) j.add(LargeInteger.one))
                row.add((LargeFraction) this.get(i).multiply(other.get(j)));
            rows.add(row);
        }
        return new LargeMatrix(rows);
    }
}
