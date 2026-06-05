package com.AtomCAS.utilities;
public class LargeMatrix extends TensorAtom {
    private LargeList<LargeList<LargeInteger>> list;
    LargeFraction factor;
    public LargeMatrix(){
        factor = LargeFraction.one;
        list = new LargeList<>();
    }
    public LargeMatrix(LargeList<LargeList<LargeInteger>> list, LargeFraction factor){
        this.list = new  LargeList<>();
        this.list.add(list.getFirst());
        for(LargeInteger i = LargeInteger.one; i.lessThan(list.size()); i = i.add(1)){
            this.list.add(list.get(i));
            if(list.get(i).size().notEqualsTo(list.get((LargeInteger) i.subtract(LargeInteger.one)).size()))
                throw new IllegalStateException("Error: Rows must have same sizes");
        }
        this.factor = factor;
        LargeInteger gc = LargeInteger.one;
        for(LargeList<LargeInteger> I: this.list){
            for(LargeInteger U: I)
                gc = gc.gcd(U);
        }
        this.factor = (LargeFraction) this.factor.multiply(gc);
        for(LargeList<LargeInteger> I: this.list)
            for(LargeInteger j = LargeInteger.zero; j.lessThan(I.size()); j = (LargeInteger) j.add(LargeInteger.one))
                I.set(j, I.get(j).longDivision(gc));
        if(this.factor.equalsTo(LargeInteger.zero)) this.factor =  LargeFraction.one;
    }
    public LargeMatrix(LargeList<LargeList<LargeFraction>> list){
        this.list = new  LargeList<>();
        LargeList<LargeInteger> dems = new LargeList<>();
        if(list.size().equalsTo(LargeInteger.zero)) return;
        LargeInteger lc = list.getFirst().getFirst().denominator;
        for(LargeList<LargeFraction> I: list){
            for(LargeFraction J: I)
                lc = lc.lcm(J.denominator);
        }
        this.factor = lc.reciprocal();
        for(LargeList<LargeFraction> I: list){
            LargeList<LargeInteger> lk = new LargeList<>();
            for(LargeFraction J: I) lk.add(((LargeFraction)J.multiply(lc)).numerator);
            this.list.add(lk);
        }
        LargeMatrix M = new LargeMatrix(this.list, this.factor);
        this.list = M.list;
        this.factor = M.factor;
    }
    public static LargeMatrix identity(LargeInteger n){
        LargeList<LargeList<LargeFraction>> rows = new LargeList<>();
        for(LargeInteger i = LargeInteger.zero; i.lessThan(n); i = i.add(1)){
            LargeList<LargeFraction> row = new LargeList<>();
            for(LargeInteger j = LargeInteger.zero; j.lessThan(n); j = j.add(1)){
                if(i.equalsTo(j)) row.add(LargeFraction.one);
                else row.add(LargeFraction.zero);
            }
            rows.add(row);
        }
        return new LargeMatrix(rows);
    }
    @Override
    public String toString() {
        if(rows().equalsTo(LargeInteger.zero)) return "[]";
        StringBuilder sb = new StringBuilder();
        //collect entry lengths
        LargeList<Integer> strs = new LargeList<>();
        for(LargeList<LargeInteger> I: list)
            for(LargeInteger J: I)
                strs.add(J.multiply(factor).toString().length());
        int st = strs.getFirst();
        for(int sf: strs)
            if(sf > st) st = sf;
        for(LargeInteger i = LargeInteger.zero; i.lessThan(rows()); i = i.add(1)){
            if(list.size().equalsTo(LargeInteger.one)){
                sb.append("[");
                for(LargeInteger j = LargeInteger.zero; j.lessThan(list.get(i).size());  j = (LargeInteger) j.add(LargeInteger.one)){
                    sb.append(list.get(i).get(j).multiply(factor));
                    if(j.lessThan(list.get(i).size().subtract(LargeInteger.one))) sb.append(" ");
                    sb.repeat(" ", Math.max(0, st - list.get(i).get(j).multiply(factor).toString().length()));
                }
                sb.append("]");
                break;
            }
            if(i.equalsTo(LargeInteger.zero)){
                sb.append("⌈");
                for(LargeInteger j = LargeInteger.zero; j.lessThan(list.get(i).size());  j = (LargeInteger) j.add(LargeInteger.one)){
                    sb.append(list.get(i).get(j).multiply(factor));
                    if(j.lessThan(list.get(i).size().subtract(LargeInteger.one))) sb.append(" ");
                    sb.repeat(" ", Math.max(0, st - list.get(i).get(j).multiply(factor).toString().length()));
                }
                sb.append("⌉\n");
            }else if(i.equalsTo(list.size().subtract(LargeInteger.one))){
                sb.append("⌊");
                for(LargeInteger j = LargeInteger.zero; j.lessThan(list.get(i).size());  j = (LargeInteger) j.add(LargeInteger.one)){
                    sb.append(list.get(i).get(j).multiply(factor));
                    if(j.lessThan(list.get(i).size().subtract(LargeInteger.one))) sb.append(" ");
                    sb.repeat(" ", Math.max(0, st - list.get(i).get(j).multiply(factor).toString().length()));
                }
                sb.append("⌋");
            }else{
                sb.append("|");
                for(LargeInteger j = LargeInteger.zero; j.lessThan(list.get(i).size());  j = (LargeInteger) j.add(LargeInteger.one)){
                    sb.append(list.get(i).get(j).multiply(factor));
                    if(j.lessThan(list.get(i).size().subtract(LargeInteger.one))) sb.append(" ");
                    sb.repeat(" ", Math.max(0, st - list.get(i).get(j).multiply(factor).toString().length()));
                }
                sb.append("|\n");
            }
        }
        return sb.toString();
    }
    @Override
    public LargeMatrix copy() {return new LargeMatrix(this.list, this.factor);}
    public LargeInteger rows(){return list.size();}
    public LargeInteger cols(){return list.getFirst().size();}
    private LargeInteger getRaw(LargeInteger i, LargeInteger j){return list.get(i).get(j);}
    public LargeFraction get(LargeInteger i, LargeInteger j){return (LargeFraction) getRaw(i, j).multiply(factor);}
    private void setRaw(LargeInteger i, LargeInteger j, LargeInteger k){list.get(i).set(j, k);}
    public void set(LargeInteger i, LargeInteger j, LargeFraction l){
        if(l.equalsTo(LargeInteger.zero)){
            setRaw(i, j, LargeInteger.zero);
            return;
        }
        LargeFraction k = ((LargeFraction) l.divide(factor));
        factor = (LargeFraction) factor.divide(k.denominator);
        for(LargeList<LargeInteger> B: list){
            for(LargeInteger h = LargeInteger.zero; h.lessThan(B.size()); h = (LargeInteger) h.add(LargeInteger.one))
                B.set(h, (LargeInteger) B.get(h).multiply(k.denominator));
        }
        setRaw(i, j, k.numerator);
    }
    private void appendRaw(LargeList<LargeInteger> B){
        LargeList<LargeInteger> cop = new LargeList<>();
        for(LargeInteger I: B)
            cop.add(I.copy());
        if(rows().greaterThan(LargeInteger.zero) && cop.size().notEqualsTo(cols())) throw new IllegalStateException("Error: Rows must be same sizes");
        list.add(cop);
    }
    public void appendRow(LargeVector b){
        if(factor.equalsTo(b.factor)){
            appendRaw(b.rawList());
            return;
        }
        LargeFraction g = factor.gcd(b.factor);
        for(LargeList<LargeInteger> B: list){
            for(LargeInteger i = LargeInteger.zero; i.lessThan(B.size()); i = i.add(1))
                B.set(i, ((LargeFraction) B.get(i).multiply(factor.divide(g))).numerator);
        }
        factor = g.copy();
        LargeList<LargeInteger> k = new LargeList<>();
        for(LargeInteger I: b.rawList())
            k.add(((LargeFraction) I.multiply(b.factor.divide(g))).numerator);
        appendRaw(k);
    }
    @Override
    public TensorAtom negate(){return new LargeMatrix(this.list, this.factor.negate());}
    @Override
    public LargeInteger[] shape(){return new LargeInteger[]{rows(), cols()};}
    public boolean isNotSquare(){return !this.rows().equalsTo(this.cols());}
    public void swapRows(LargeInteger r1, LargeInteger r2) {
        LargeList<LargeInteger> temp = list.get(r1);
        list.set(r1, list.get(r2));
        list.set(r2, temp);
    }
    public LargeMatrix transpose(){
        LargeList<LargeList<LargeInteger>> bk = new LargeList<>();
        for(LargeInteger i = LargeInteger.zero; i.lessThan(this.cols()); i = i.add(1)){
            LargeList<LargeInteger> bs = new LargeList<>();
            for(LargeInteger j = LargeInteger.zero; j.lessThan(this.rows()); j = j.add(1))
                bs.add(getRaw(j, i));
            bk.add(bs);
        }
        return new LargeMatrix(bk, factor);
    }
    @Override
    public TensorAtom add(TensorAtom other) {
        if(other instanceof LargeMatrix){
            if(((LargeMatrix) other).cols().notEqualsTo(this.cols()) || this.rows().notEqualsTo(((LargeMatrix) other).rows()))
                throw new IllegalStateException("Error: Dimension mismatch");
            LargeList<LargeList<LargeFraction>> bk = new LargeList<>();
            for(LargeInteger i = LargeInteger.zero; i.lessThan(this.rows()); i = i.add(1)){
                LargeList<LargeFraction> bs = new LargeList<>();
                for(LargeInteger j = LargeInteger.zero; j.lessThan(this.cols()); j = j.add(1))
                    bs.add((LargeFraction) this.get(i, j).add(((LargeMatrix) other).get(i, j)));
                bk.add(bs);
            }
            return new LargeMatrix(bk);
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public TensorAtom subtract(TensorAtom other){return this.add(other.negate());}
    @Override
    public TensorAtom multiply(TensorAtom other){
        if(other instanceof LargeVector){
            if(this.cols().notEqualsTo(((LargeVector) other).dimension()))
                throw new ArithmeticException("Error: Dimension mismatch");
            LargeList<LargeFraction> bk = new LargeList<>();
            for(LargeInteger i = LargeInteger.zero; i.lessThan(this.rows()); i = i.add(1)){
                LargeFraction f = LargeFraction.zero;
                for(LargeInteger j = LargeInteger.zero; j.lessThan(this.cols()); j = j.add(1))
                    f = (LargeFraction) f.add(this.get(i, j).multiply(((LargeVector) other).get(j)));
                bk.add(f);
            }
            return new LargeVector(bk);
        }else if(other instanceof LargeMatrix){
            if(this.cols().notEqualsTo(((LargeMatrix) other).rows()))
                throw new ArithmeticException("Error: Dimension mismatch");
            LargeList<LargeList<LargeInteger>> result = new LargeList<>();
            for (LargeInteger i = LargeInteger.zero; i.lessThan(this.rows()); i = (LargeInteger) i.add(LargeInteger.one)) {
                LargeList<LargeInteger> row = new LargeList<>();
                for (LargeInteger j = LargeInteger.zero; j.lessThan(((LargeMatrix) other).cols()); j = (LargeInteger) j.add(LargeInteger.one)) {
                    LargeInteger sum = LargeInteger.zero;
                    for (LargeInteger k = LargeInteger.zero; k.lessThan(this.cols()); k = (LargeInteger) k.add(LargeInteger.one))
                        sum = (LargeInteger) sum.add(this.getRaw(i, k).multiply(((LargeMatrix) other).getRaw(k, j)));
                    row.add(sum);
                }
                result.add(row);
            }
            return new LargeMatrix(result, (LargeFraction) factor.multiply(((LargeMatrix) other).factor));
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public TensorAtom scale(LargeFraction scalar) {return new LargeMatrix(list, (LargeFraction) factor.multiply(scalar));}
    @Override
    public boolean equalsTo(Atom other){
        if(!(other instanceof TensorAtom)) return false;
        else if(other instanceof LargeVector)
            return this.rows().equalsTo(LargeInteger.one) && new LargeVector(list.getFirst(), factor).equalsTo(other);
        else if(other instanceof LargeMatrix){
            if(this.rows().notEqualsTo(((LargeMatrix) other).rows()) || this.cols().notEqualsTo(((LargeMatrix) other).cols())) return false;
            for(LargeInteger i = LargeInteger.zero; i.lessThan(this.rows()); i = i.add(1))
                for(LargeInteger j = LargeInteger.zero; j.lessThan(this.cols()); j = (LargeInteger) j.add(LargeInteger.one))
                    if(this.get(i, j).notEqualsTo(((LargeMatrix) other).get(i, j))) return false;
            return true;
        }
        throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
    }
    @Override
    public boolean notEqualsTo(Atom other) {return !equalsTo(other);}
    public LargeFraction determinant() {
        if(this.isNotSquare())
            throw new ArithmeticException("Error: Determinant is only defined for square matrices");
        LargeInteger n = this.rows();
        if(n.equalsTo(LargeInteger.zero))
            return LargeFraction.one;
        if(n.equalsTo(LargeInteger.one))
            return (LargeFraction) this.getRaw(LargeInteger.zero, LargeInteger.zero).multiply(factor);
        // copy raw matrix
        LargeList<LargeList<LargeInteger>> m = new LargeList<>();
        for(LargeInteger i = LargeInteger.zero; i.lessThan(n); i = i.add(1)){
            LargeList<LargeInteger> row = new LargeList<>();
            for(LargeInteger j = LargeInteger.zero; j.lessThan(n); j = (LargeInteger) j.add(LargeInteger.one))
                row.add(this.getRaw(i, j).copy());
            m.add(row);
        }
        LargeInteger previousPivot = LargeInteger.one;
        long sign = 1L;
        for (LargeInteger k = LargeInteger.zero;
             k.lessThan(n.subtract(LargeInteger.one));
             k = (LargeInteger) k.add(LargeInteger.one)) {
            // find nonzero pivot
            LargeInteger pivotRow = k;
            while (pivotRow.lessThan(n) && m.get(pivotRow).get(k).equalsTo(LargeInteger.zero))
                pivotRow = (LargeInteger) pivotRow.add(LargeInteger.one);
            if (pivotRow.equalsTo(n))
                return LargeFraction.zero;
            // swap rows if needed
            if (pivotRow.notEqualsTo(k)) {
                LargeList<LargeInteger> temp = m.get(k);
                m.set(k, m.get(pivotRow));
                m.set(pivotRow, temp);
                sign *= -1L;
            }
            LargeInteger pivot = m.get(k).get(k);
            for (LargeInteger i = (LargeInteger) k.add(LargeInteger.one);
                 i.lessThan(n);
                 i = (LargeInteger) i.add(LargeInteger.one)) {
                for (LargeInteger j = (LargeInteger) k.add(LargeInteger.one);
                     j.lessThan(n);
                     j = (LargeInteger) j.add(LargeInteger.one)) {
                    LargeInteger aij = m.get(i).get(j);
                    LargeInteger aik = m.get(i).get(k);
                    LargeInteger akj = m.get(k).get(j);
                    LargeInteger numerator = (LargeInteger) pivot.multiply(aij).subtract(aik.multiply(akj));
                    LargeInteger updated = numerator.longDivision(previousPivot);
                    m.get(i).set(j, updated);
                }
            }
            // Optional cleanup: clear below/right stale values
            for (LargeInteger i = (LargeInteger) k.add(LargeInteger.one);
                 i.lessThan(n);
                 i = (LargeInteger) i.add(LargeInteger.one)) {
                m.get(i).set(k, LargeInteger.zero);
            }
            previousPivot = pivot;
        }
        LargeInteger rawDet = m.get((LargeInteger) n.subtract(LargeInteger.one)).get((LargeInteger) n.subtract(LargeInteger.one));
        if(sign == -1L)
            rawDet = (LargeInteger) rawDet.negate();
        return (LargeFraction) rawDet.multiply(factor.power(n));
    }
    public LargeMatrix rowEchelonFrom(){
        LargeMatrix M = this.copy();
        LargeInteger pivotRow = LargeInteger.zero;
        for(LargeInteger pivotCol = LargeInteger.zero; pivotCol.lessThan(M.cols()) && pivotRow.lessThan(M.rows()); pivotCol = (LargeInteger) pivotCol.add(LargeInteger.one)){
            //determine the first row in which pivot is nonzero, in the current column
            LargeInteger nextRow = LargeInteger.negativeOne;
            for(LargeInteger i = pivotRow; i.lessThan(M.rows()); i = (LargeInteger) i.add(LargeInteger.one))
                if(M.get(i, pivotCol).notEqualsTo(LargeInteger.zero)){
                    nextRow = i;
                    break;
                }
            if(nextRow.equalsTo(LargeInteger.negativeOne)) continue;
            M.swapRows(pivotRow, nextRow);
            LargeFraction pivot = M.get(pivotRow, pivotCol);
            // eliminate entries below pivot
            for(LargeInteger r = (LargeInteger) pivotRow.add(LargeInteger.one); r.lessThan(M.rows()); r = (LargeInteger) r.add(LargeInteger.one)){
                LargeFraction entry = M.get(r, pivotCol);
                if(entry.equalsTo(LargeInteger.zero)) continue;
                LargeFraction factor = (LargeFraction) entry.divide(pivot);
                for(LargeInteger c = pivotCol; c.lessThan(M.cols()); c = (LargeInteger) c.add(LargeInteger.one)){
                    LargeFraction updated = (LargeFraction) M.get(r, c).subtract(factor.multiply(M.get(pivotRow, c)));
                    M.set(r, c, updated);
                }
            }
            pivotRow = (LargeInteger) pivotRow.add(LargeInteger.one);
        }
        return M;
    }
    public LargeMatrix reducedRowEchelonForm(){
        LargeMatrix M = this.copy();
        LargeInteger pivotRow = LargeInteger.zero;
        for(LargeInteger pivotCol = LargeInteger.zero; pivotCol.lessThan(M.cols()) && pivotRow.lessThan(M.rows()); pivotCol = (LargeInteger) pivotCol.add(LargeInteger.one)){
            //determine the first row in which pivot is nonzero, in the current column
            LargeInteger nextRow = LargeInteger.negativeOne;
            for(LargeInteger i = pivotRow; i.lessThan(M.rows()); i = (LargeInteger) i.add(LargeInteger.one))
                if(M.get(i, pivotCol).notEqualsTo(LargeInteger.zero)){
                    nextRow = i;
                    break;
                }
            if(nextRow.equalsTo(LargeInteger.negativeOne)) continue;
            M.swapRows(pivotRow, nextRow);
            LargeFraction pivot = M.get(pivotRow, pivotCol);
            //scale pivot row so that pivot is 1
            for(LargeInteger c = pivotCol; c.lessThan(M.cols()); c = (LargeInteger) c.add(LargeInteger.one))
                M.set(pivotRow, c, (LargeFraction) M.get(pivotRow, c).divide(pivot));
            // eliminate entries below and above pivot
            for(LargeInteger r = LargeInteger.zero; r.lessThan(M.rows()); r = (LargeInteger) r.add(LargeInteger.one)){
                if(r.equalsTo(pivotRow)) continue;
                LargeFraction factor = M.get(r, pivotCol);
                if(factor.equalsTo(LargeInteger.zero)) continue;
                for(LargeInteger c = pivotCol; c.lessThan(M.cols()); c = (LargeInteger) c.add(LargeInteger.one))
                    M.set(r, c, (LargeFraction) M.get(r, c).subtract(factor.multiply(M.get(pivotRow, c))));
            }
            pivotRow = (LargeInteger) pivotRow.add(LargeInteger.one);
        }
        return M;
    }
    public LargeMatrix inverse(){
        if(this.isNotSquare())
            throw new ArithmeticException("Error: Inverse only defined for square matrices");
        LargeInteger n = this.rows();
        // Build augmented matrix [A | I]
        LargeMatrix M = new LargeMatrix();
        for(LargeInteger i = LargeInteger.zero; i.lessThan(n); i = i.add(1)){
            LargeList<LargeFraction> row = new LargeList<>();
            // left side: A
            for(LargeInteger j = LargeInteger.zero; j.lessThan(n); j = j.add(1))
                row.add(this.get(i, j));
            // right side: I
            for(LargeInteger j = LargeInteger.zero; j.lessThan(n); j = j.add(1)){
                if(i.equalsTo(j)) row.add(LargeFraction.one);
                else row.add(LargeFraction.zero);
            }
            M.appendRow(new LargeVector(row));
        }
        M = M.reducedRowEchelonForm();
        //check if left side is identity
        LargeMatrix L = new LargeMatrix();
        for(LargeInteger i = LargeInteger.zero; i.lessThan(n); i = i.add(1)){
            LargeVector v = new LargeVector();
            for(LargeInteger j = LargeInteger.zero; j.lessThan(M.cols().longDivision(LargeInteger.two)); j=j.add(1))
                v.append(M.get(i, j));
            L.appendRow(v);
        }
        if(L.notEqualsTo(identity(n)))
            throw new ArithmeticException("Error: This matrix does not have an inverse");
        LargeMatrix R = new LargeMatrix();
        for(LargeInteger i = LargeInteger.zero; i.lessThan(n); i = i.add(1)){
            LargeVector v = new LargeVector();
            for(LargeInteger j = M.cols().longDivision(LargeInteger.two); j.lessThan(M.cols()); j=j.add(1))
                v.append(M.get(i, j));
            R.appendRow(v);
        }
        return R;
    }
    public LargeInteger rank(){
        LargeInteger rank = LargeInteger.zero;
        LargeMatrix M = this.reducedRowEchelonForm();
        for(LargeList<LargeInteger> L: M.list){
            LargeInteger count = LargeInteger.zero;
            for(LargeInteger I: L)
                if(I.notEqualsTo(LargeInteger.zero)) count = (LargeInteger) count.add(LargeInteger.one);
            if(count.notEqualsTo(LargeInteger.zero)) rank = (LargeInteger) rank.add(LargeInteger.one);
        }
        return rank;
    }
}
