package com.AtomCAS.utilities;
public final class LargeMatrix extends TensorAtom {
    private final LargeList<LargeList<LargeInteger>> list;
    private final LargeFraction factor;
    LargeMatrix(LargeList<LargeList<LargeInteger>> list, LargeFraction factor){
        this.list = new LargeList<>();
        LargeInteger gc = LargeInteger.ZERO;
        for(LargeList<LargeInteger> I: list){
            for(LargeInteger J: I)
                gc = gc.gcd(J);
        }
        for(LargeList<LargeInteger> I: list){
            LargeList<LargeInteger> lt = new LargeList<>();
            for(LargeInteger J: I)
                lt.add(J.divide(gc));
            this.list.add(lt);
        }
        this.factor = (LargeFraction) factor.multiply(gc);
    }
    public LargeMatrix(LargeList<LargeList<LargeFraction>> list){
        this.list = new  LargeList<>();
        LargeFraction gc = LargeFraction.ZERO;
        for(LargeList<LargeFraction> LF: list){
            for(LargeFraction F: LF)
                gc = gc.gcd(F);
        }
        LargeInteger size = list.getFirst().size();
        for(LargeList<LargeFraction> LF: list) {
            LargeList<LargeInteger> IF = new LargeList<>();
            for(LargeFraction F: LF)
                IF.add(F.divide(gc).numerator);
            if (IF.size().doesntEqual(size))
                throw new IllegalArgumentException();
            this.list.add(IF);
        }
        this.factor = gc;
    }
    public static LargeMatrix identity(LargeInteger n) {
        LargeList<LargeList<LargeFraction>> rows = new LargeList<>();
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(n); i = i.add(1)){
            LargeList<LargeFraction> row = new LargeList<>();
            for(LargeInteger j = LargeInteger.ZERO; j.lessThan(n); j = j.add(1)){
                if(i.equals(j)) row.add(LargeFraction.ONE);
                else row.add(LargeFraction.ZERO);
            }
            rows.add(row);
        }
        return new LargeMatrix(rows);
    }
    @Override
    public String toString() {
        if(rows().isZero()) return "[]";
        StringBuilder sb = new StringBuilder();
        //collect entry lengths
        LargeList<Integer> strings = new LargeList<>();
        for(LargeList<LargeInteger> I: list)
            for(LargeInteger J: I)
                strings.add(J.multiply(factor).toString().length());
        int st = strings.getFirst();
        for(int sf: strings)
            if(sf > st) st = sf;
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(rows()); i = i.add(1)){
            if(list.size().isOne()){
                sb.append(']');
                for(LargeInteger j = LargeInteger.ZERO; j.lessThan(list.get(i).size()); j = j.add(1)){
                    sb.append(list.get(i).get(j).multiply(factor));
                    if(j.lessThan(list.get(i).size().subtract(1))) sb.append(' ');
                    sb.repeat(' ', Math.max(0, st - list.get(i).get(j).multiply(factor).toString().length()));
                }
                sb.append(']');
                break;
            }
            if(i.isZero()){
                sb.append('⌈');
                for(LargeInteger j = LargeInteger.ZERO; j.lessThan(list.get(i).size()); j = j.add(1)){
                    sb.append(list.get(i).get(j).multiply(factor));
                    if(j.lessThan(list.get(i).size().subtract(1))) sb.append(' ');
                    sb.repeat(' ', Math.max(0, st - list.get(i).get(j).multiply(factor).toString().length()));
                }
                sb.append("⌉\n");
            } else if(i.equals(list.size().subtract(1))) {
                sb.append('⌊');
                for(LargeInteger j = LargeInteger.ZERO; j.lessThan(list.get(i).size()); j = j.add(1)){
                    sb.append(list.get(i).get(j).multiply(factor));
                    if(j.lessThan(list.get(i).size().subtract(1))) sb.append(' ');
                    sb.repeat(' ', Math.max(0, st - list.get(i).get(j).multiply(factor).toString().length()));
                }
                sb.append('⌋');
            } else {
                sb.append('|');
                for(LargeInteger j = LargeInteger.ZERO; j.lessThan(list.get(i).size()); j = j.add(1)){
                    sb.append(list.get(i).get(j).multiply(factor));
                    if(j.lessThan(list.get(i).size().subtract(1))) sb.append(' ');
                    sb.repeat(' ', Math.max(0, st - list.get(i).get(j).multiply(factor).toString().length()));
                }
                sb.append("|\n");
            }
        }
        return sb.toString();
    }
    @Override
    public String toLatex() {return "";}
    private LargeList<LargeList<LargeFraction>> toFractionMatrix() {
        LargeList<LargeList<LargeFraction>> M = new LargeList<>();
        LargeInteger rowCount = rows(), colCount = cols();
        for (LargeInteger r = LargeInteger.ZERO; r.lessThan(rowCount); r = r.add(1)) {
            LargeList<LargeFraction> row = new LargeList<>();
            for (LargeInteger c = LargeInteger.ZERO; c.lessThan(colCount); c = c.add(1))
                row.add(get(r, c));
            M.add(row);
        }
        return M;
    }
    @Override
    public LargeMatrix clone() {return new LargeMatrix(this.list.clone(), this.factor.clone());}
    public LargeInteger rows() {return list.size();}
    public LargeInteger cols() {return list.getFirst().size();}
    private LargeInteger getRaw(LargeInteger i, LargeInteger j) {return list.get(i).get(j);}
    public LargeFraction get(LargeInteger i, LargeInteger j) {return (LargeFraction) getRaw(i, j).multiply(factor);}
    public LargeFraction get(int i, int j) {return (LargeFraction) list.get(i).get(j).multiply(factor);}
    @Override
    public TensorAtom negate() {return new LargeMatrix(this.list, this.factor.negate());}
    @Override
    public LargeInteger[] shape() {return new LargeInteger[]{rows(), cols()};}
    boolean isNotSquare() {return !this.rows().equals(this.cols());}
    public LargeMatrix transpose() {
        LargeList<LargeList<LargeInteger>> bk = new LargeList<>();
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.cols()); i = i.add(1)){
            LargeList<LargeInteger> bs = new LargeList<>();
            for(LargeInteger j = LargeInteger.ZERO; j.lessThan(this.rows()); j = j.add(1))
                bs.add(getRaw(j, i));
            bk.add(bs);
        }
        return new LargeMatrix(bk, factor);
    }
    public LargeMatrix add(LargeMatrix LM) {
        if(LM.cols().doesntEqual(this.cols()) || this.rows().doesntEqual(LM.rows()))
            throw new IllegalStateException("Dimension mismatch");
        LargeList<LargeList<LargeFraction>> bk = new LargeList<>();
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.rows()); i = i.add(1)){
            LargeList<LargeFraction> bs = new LargeList<>();
            for(LargeInteger j = LargeInteger.ZERO; j.lessThan(this.cols()); j = j.add(1))
                bs.add(this.get(i, j).add(LM.get(i, j)));
            bk.add(bs);
        }
        return new LargeMatrix(bk);
    }
    @Override
    public TensorAtom add(TensorAtom other) {
        if(other instanceof LargeMatrix LM)
            return add(LM);
        else throw new UnsupportedOperationException("Operation with these types together not supported yet");
    }
    @Override
    public TensorAtom subtract(TensorAtom other) {return this.add(other.negate());}
    public LargeMatrix multiply(LargeMatrix LM) {
        if(this.cols().doesntEqual(LM.rows()))
            throw new ArithmeticException("Dimension mismatch");
        LargeList<LargeList<LargeInteger>> result = new LargeList<>();
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(this.rows()); i = i.add(1)) {
            LargeList<LargeInteger> row = new LargeList<>();
            for (LargeInteger j = LargeInteger.ZERO; j.lessThan(LM.cols()); j = j.add(1)) {
                LargeInteger sum = LargeInteger.ZERO;
                for (LargeInteger k = LargeInteger.ZERO; k.lessThan(this.cols()); k = k.add(1))
                    sum = sum.add(this.getRaw(i, k).multiply(LM.getRaw(k, j)));
                row.add(sum);
            }
            result.add(row);
        }
        return new LargeMatrix(result, factor.multiply(LM.factor));
    }
    @Override
    public TensorAtom multiply(TensorAtom other) {
        if(other instanceof LargeVector LV) {
            if(cols().doesntEqual(LV.dimension()))
                throw new ArithmeticException("Dimension mismatch");
            LargeList<LargeFraction> bk = new LargeList<>();
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.rows()); i = i.add(1)){
                LargeFraction f = LargeFraction.ZERO;
                for(LargeInteger j = LargeInteger.ZERO; j.lessThan(this.cols()); j = j.add(1))
                    f = f.add(this.get(i, j).multiply(LV.get(j)));
                bk.add(f);
            }
            return new LargeVector(bk);
        } else if(other instanceof LargeMatrix LM) return multiply(LM);
        else throw new UnsupportedOperationException("Operation with these types together not supported yet");
    }
    @Override
    public TensorAtom scale(LargeFraction scalar) {
        return new LargeMatrix(list, factor.multiply(scalar));}
    @Override
    public boolean equals(Atom other){
        if(other == this) return true;
        else if(!(other instanceof TensorAtom)) return false;
        if(other instanceof LargeVector LV)
            return this.rows().isOne() && LV.factor.equals(factor) && list.getFirst().equals(LV.list);
        else if(other instanceof LargeMatrix LM){
            if(this.rows().doesntEqual(LM.rows()) || this.cols().doesntEqual(LM.cols())) return false;
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(this.rows()); i = i.add(1))
                for(LargeInteger j = LargeInteger.ZERO; j.lessThan(this.cols()); j = j.add(1))
                    if(this.get(i, j).doesntEqual(LM.get(i, j))) return false;
            return true;
        } else return false;
    }
    @SuppressWarnings("unused")
    public LargeFraction determinant() {
        if(isNotSquare())
            throw new ArithmeticException("Determinant only defined for square matrices");
        LargeInteger n = this.rows();
        if(n.isZero())
            return LargeFraction.ONE;
        if(n.isOne())
            return (LargeFraction) this.getRaw(LargeInteger.ZERO, LargeInteger.ZERO).multiply(factor);
        // clone raw matrix
        LargeList<LargeList<LargeInteger>> m = new LargeList<>();
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(n); i = i.add(1)){
            LargeList<LargeInteger> row = new LargeList<>();
            for(LargeInteger j = LargeInteger.ZERO; j.lessThan(n); j = j.add(1))
                row.add(getRaw(i, j));
            m.add(row);
        }
        LargeInteger previousPivot = LargeInteger.ONE;
        boolean negative = false;
        for (LargeInteger k = LargeInteger.ZERO; k.lessThan(n.subtract(1)); k = k.add(1)) {
            // find nonzero pivot
            LargeInteger pivotRow = k;
            while (pivotRow.lessThan(n) && m.get(pivotRow).get(k).isZero())
                pivotRow = pivotRow.add(1);
            if (pivotRow.equals(n))
                return LargeFraction.ZERO;
            // swap rows if needed
            if (pivotRow.doesntEqual(k)) {
                LargeList<LargeInteger> temp = m.get(k);
                m.set(k, m.get(pivotRow));
                m.set(pivotRow, temp);
                negative = !negative;
            }
            LargeInteger pivot = m.get(k).get(k);
            for (LargeInteger i = k.add(1); i.lessThan(n); i = i.add(1)) {
                for (LargeInteger j = k.add(1); j.lessThan(n); j = j.add(1)) {
                    LargeInteger aij = m.get(i).get(j);
                    LargeInteger aik = m.get(i).get(k);
                    LargeInteger akj = m.get(k).get(j);
                    LargeInteger numerator = pivot.multiply(aij).subtract(aik.multiply(akj));
                    LargeInteger updated = numerator.divide(previousPivot);
                    m.get(i).set(j, updated);
                }
            }
            // Optional cleanup: clear below/right stale values
            for (LargeInteger i = k.add(1); i.lessThan(n); i = i.add(1))
                m.get(i).set(k, LargeInteger.ZERO);
            previousPivot = pivot;
        }
        LargeInteger rawDet = m.get(n.subtract(1)).get(n.subtract(1));
        if(negative)
            rawDet = rawDet.negate();
        return (LargeFraction) rawDet.multiply(factor.power(n));
    }
    @SuppressWarnings("unused")
    public LargeMatrix rowEchelonForm(){
        LargeList<LargeList<LargeFraction>> M = toFractionMatrix();
        LargeInteger rowCount = rows();
        LargeInteger colCount = cols();
        for(LargeInteger pivotCol = LargeInteger.ZERO, pivotRow = LargeInteger.ZERO; pivotCol.lessThan(colCount)
                && pivotRow.lessThan(rowCount); pivotRow = pivotRow.add(1)){
            //determine the first row in which pivot is nonzero, in the current column
            LargeInteger nextRow = LargeInteger.NEGATIVE_ONE;
            for(LargeInteger i = pivotRow; i.lessThan(rowCount); i = i.add(1))
                if(!M.get(i).get(pivotCol).isZero()){
                    nextRow = i;
                    break;
                }
            if(nextRow.isNegative()) continue;
            M.swap(pivotRow, nextRow);
            LargeFraction pivot = M.get(pivotRow).get(pivotCol);
            // eliminate entries below pivot
            for(LargeInteger r = pivotRow.add(1); r.lessThan(rowCount); r = r.add(1)){
                LargeFraction entry = M.get(r).get(pivotCol);
                if(entry.isZero()) continue;
                LargeFraction factor = entry.divide(pivot);
                for(LargeInteger c = pivotCol; c.lessThan(colCount); c = c.add(1)){
                    LargeFraction updated = M.get(r).get(c).subtract(factor.multiply(M.get(pivotRow).get(c)));
                    M.get(r).set(c, updated);
                }
            }
            pivotRow = pivotRow.add(1);
        }
        return new LargeMatrix(M);
    }
    public LargeMatrix reducedRowEchelonForm() {
        LargeList<LargeList<LargeFraction>> M = toFractionMatrix();
        LargeInteger colCount = M.getFirst().size();
        LargeInteger rowCount = M.size();
        for (LargeInteger pivotCol = LargeInteger.ZERO, pivotRow = LargeInteger.ZERO;
             pivotCol.lessThan(colCount) && pivotRow.lessThan(rowCount);
             pivotCol = pivotCol.add(1)) {
            LargeInteger nextRow = LargeInteger.NEGATIVE_ONE;
            for (LargeInteger r = pivotRow; r.lessThan(M.size()); r = r.add(1))
                if (!M.get(r).get(pivotCol).isZero()) {
                    nextRow = r;
                    break;
                }
            if (nextRow.isNegative()) continue;
            M.swap(pivotRow, nextRow);
            LargeList<LargeFraction> pivotData = M.get(pivotRow);
            LargeFraction pivot = pivotData.get(pivotCol);
            LargeInteger nextCol = pivotCol.add(1);
            // Normalize pivot row
            for (LargeInteger c = nextCol; c.lessThan(colCount); c = c.add(1))
                pivotData.set(c, pivotData.get(c).divide(pivot));
            pivotData.set(pivotCol, LargeFraction.ONE);
            // Eliminate above and below
            for (LargeInteger r = LargeInteger.ZERO; r.lessThan(rowCount); r = r.add(1)) {
                if (r.equals(pivotRow)) continue;
                LargeList<LargeFraction> row = M.get(r);
                LargeFraction factor = row.get(pivotCol);
                if (factor.isZero()) continue;
                for (LargeInteger c = nextCol; c.lessThan(M.getFirst().size()); c = c.add(1))
                    row.set(c, row.get(c).subtract(factor.multiply(pivotData.get(c))));
                row.set(pivotCol, LargeFraction.ZERO);
            }
            pivotRow = pivotRow.add(1);
        }
        return new LargeMatrix(M);
    }
    public LargeMatrix inverse() {
        if(isNotSquare())
            throw new ArithmeticException("Inverse only defined for square matrices");
        LargeInteger n = rows();
        // Build augmented matrix [A | I]
        LargeList<LargeList<LargeFraction>> lists = toFractionMatrix();
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(n); i = i.add(1)){
            LargeList<LargeFraction> row = new LargeList<>();
            // left side: A
            for(LargeInteger j = LargeInteger.ZERO; j.lessThan(n); j = j.add(1))
                row.add(get(i, j));
            // right side: I
            for(LargeInteger j = LargeInteger.ZERO; j.lessThan(n); j = j.add(1)){
                if(i.equals(j)) row.add(LargeFraction.ONE);
                else row.add(LargeFraction.ZERO);
            }
            lists.add(row);
        }
        LargeMatrix M = new LargeMatrix(lists).reducedRowEchelonForm();
        //check if left side is identity
        LargeList<LargeList<LargeInteger>> L = new LargeList<>();
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(n); i = i.add(1)){
            LargeList<LargeInteger> v = new LargeList<>();
            for(LargeInteger j = LargeInteger.ZERO; j.lessThan(M.cols().shiftRight(1)); j=j.add(1))
                v.add(M.getRaw(i, j));
            L.add(v);
        }
        if(!L.equals(identity(n).list))
            throw new ArithmeticException("This matrix does not have an inverse");
        LargeList<LargeList<LargeFraction>> R = new LargeList<>();
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(n); i = i.add(1)){
            LargeList<LargeFraction> row = new LargeList<>();
            for(LargeInteger j = M.cols().shiftRight(1); j.lessThan(M.cols()); j=j.add(1))
                row.add(M.get(i, j));
            R.add(row);
        }
        return new LargeMatrix(R);
    }
    public LargeMatrix divide(LargeMatrix other) {return this.multiply(other.inverse());}
    @SuppressWarnings("unused")
    public LargeInteger rank() {
        LargeInteger rank = LargeInteger.ZERO;
        LargeMatrix M = reducedRowEchelonForm();
        for(LargeList<LargeInteger> L: M.list){
            boolean nonZero = false;
            for(LargeInteger I: L)
                if(!I.isZero()){
                    nonZero = true;
                    break;
                }
            if(nonZero) rank = rank.add(1);
        }
        return rank;
    }
    public LargePolynomial characteristicPolynomial() {
        if(isNotSquare())
            throw new IllegalArgumentException("Characteristic polynomial defined only for square matrices");
        LargeInteger n = rows();
        LargeList<LargeList<LargePolynomial>> M = new LargeList<>();
        for (LargeInteger i = LargeInteger.ZERO; i.lessThan(n); i = i.add(1)) {
            LargeList<LargePolynomial> L = new LargeList<>();
            for (LargeInteger j = LargeInteger.ZERO; j.lessThan(n); j = j.add(1)){
                LargePolynomial entry = LargePolynomial.ONE().multiply(get(i, j).negate());
                if (i.equals(j)) entry = LargePolynomial.x().add(entry);
                L.add(entry);
            }
            M.add(L);
        }
        return determinantOfPolynomialMatrix(M);
    }
    public static LargePolynomial determinantOfPolynomialMatrix(LargeList<LargeList<LargePolynomial>> input) {
        if(input.size().doesntEqual(input.getFirst().size()))
            throw new IllegalArgumentException("Determinant only defined for square matrices");
        LargeInteger n = input.size();

        if (n.isZero()) return LargePolynomial.ONE();
        if (n.isOne()) return input.getFirst().getFirst();

        LargeList<LargeList<LargePolynomial>> m = new LargeList<>();
        for(LargeList<LargePolynomial> L: input){
            LargeList<LargePolynomial> R = new LargeList<>();
            for(LargePolynomial I: L) R.add(I);
            m.add(R);
        }

        LargePolynomial previousPivot = LargePolynomial.ONE();
        int sign = 1;

        for (LargeInteger k = LargeInteger.ZERO; k.lessThan(n.subtract(1)); k = k.add(1)) {
            LargeInteger pivotRow = k;

            while (pivotRow.lessThan(n) && m.get(pivotRow).get(k).isZero())
                pivotRow=pivotRow.add(1);
            if (pivotRow.equals(n))
                return new LargePolynomial();
            if (pivotRow.doesntEqual(k)) {
                LargeList<LargePolynomial> temp = m.get(k);
                m.set(k, m.get(pivotRow));
                m.set(pivotRow, temp);
                sign = -sign;
            }
            LargePolynomial pivot = m.get(k).get(k);
            for (LargeInteger i = k.add(1); i.lessThan(n); i=i.add(1)) {
                for (LargeInteger j = k.add(1); j.lessThan(n); j=j.add(1)) {
                    LargePolynomial numerator = m.get(i).get(j).multiply(pivot)
                            .subtract(m.get(i).get(k).multiply(m.get(k).get(j)));
                    if (k.isZero()) m.get(i).set(j, numerator);
                    else m.get(i).set(j, numerator.longDivision(previousPivot));
                }
            }
            previousPivot = pivot;
            // optional cleanup: zero out unused row/column
            for (LargeInteger i = k.add(1); i.lessThan(n); i=i.add(1))
                m.get(i).set(k, new LargePolynomial());

            for (LargeInteger i = k.add(1); i.lessThan(n); i=i.add(1))
                m.get(k).set(i, new LargePolynomial());
        }
        LargePolynomial det = m.get(n.subtract(1)).get(n.subtract(1));
        if (sign < 0) det = det.negate();
        return det;
    }
}
