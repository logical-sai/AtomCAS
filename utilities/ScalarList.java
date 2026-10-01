package com.AtomCAS.utilities;
import java.util.ArrayList;
public class ScalarList{
    public LargeList<ScalarAtom> list;
    public ScalarList(LargeList<ScalarAtom> list) {this.list = list.copy();}
    public ScalarAtom get(int i) {return list.get(i);}
    public ScalarAtom get(LargeInteger i){return list.get(i);}
    public LargeInteger size(){return list.size();}
    private void requireReal(String operation) {
        for (ScalarAtom atom : list) {
            if (atom.isNotReal())
                throw new IllegalStateException(operation + " is only defined for real-valued ScalarLists");
        }
    }
    public boolean sorted(){
        requireReal("checking if sorted list");
        ScalarAtom previous = get(0);
        for(ScalarAtom atom : list){
            if(atom.lessThan(previous))
                return false;
            previous = atom;
        }
        return true;
    }
    public void sort() {
        requireReal("sort");
        if (sorted()) return;
        if (list.intSized())
            sortRangeWithArrayList(LargeInteger.ZERO, list.size());
        else
            mergeSort(LargeInteger.ZERO, list.size());
    }
    private void mergeSort(LargeInteger bottom, LargeInteger top) {
        if (list.intSized()){
            sortRangeWithArrayList(bottom, top);
            return;
        }
        LargeInteger mid = bottom.add(top).shiftRight(1);
        mergeSort(bottom, mid);
        mergeSort(mid, top);
        //merge step
        LargeInteger i = bottom;
        LargeInteger j = mid;
        LargeList<ScalarAtom> temp = new LargeList<>();
        while (i.lessThan(mid) && j.lessThan(top)) {
            ScalarAtom a = list.get(i);
            ScalarAtom b = list.get(j);
            if (compareScalars(a, b) <= 0) {
                temp.add(a);
                i = i.add(1);
            } else {
                temp.add(b);
                j = j.add(1);
            }
        }
        while (i.lessThan(mid)) {
            temp.add(list.get(i));
            i = i.add(1);
        }
        while (j.lessThan(top)) {
            temp.add(list.get(j));
            j = j.add(1);
        }
        for (LargeInteger k = LargeInteger.ZERO; k.lessThan(temp.size()); k = k.add(1))
            list.set(bottom.add(k), temp.get(k));
    }
    private void sortRangeWithArrayList(LargeInteger bottom, LargeInteger top) {
        int n = top.subtract(bottom).intValue();
        ArrayList<ScalarAtom> copy = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            copy.add(list.get(bottom.add(i)));
        copy.sort(ScalarList::compareScalars);
        for (int i = 0; i < n; i++)
            list.set(bottom.add(i), copy.get(i));
    }
    private static int compareScalars(ScalarAtom a, ScalarAtom b) {return a.compareTo(b);}
    public ScalarAtom range(){
        requireReal("range");
        return max().subtract(min());
    }
    public ScalarAtom min() {
        requireReal("min");
        sort();
        return list.getFirst();
    }
    public ScalarAtom median() {
        requireReal("median");
        sort();
        if (list.intSize() == 0) throw new IllegalArgumentException("Cannot calculate median of empty list");
        if (list.size().isOdd()) return list.get(list.size().shiftRight(1));
        else return list.get(list.size().shiftRight(1)).add(list.get(list.size().shiftRight(1).subtract(1))).divide(LargeInteger.TWO);
    }
    public ScalarAtom mean() {
        ScalarAtom result = LargeInteger.ZERO;
        for (ScalarAtom atom : list) result = result.add(atom);
        return result.divide(list.size());
    }
    public ScalarAtom max() {
        requireReal("max");
        sort();
        return list.getLast();
    }
    public ScalarAtom IQR() {
        requireReal("IQR");
        if (list.size().lessThan(LargeInteger.TWO))
            throw new IllegalArgumentException("Need at least 2 values in list");
        sort();
        LargeList<ScalarAtom> copy = new LargeList<>();
        for (ScalarAtom atom : list)
            copy.add(atom);
        LargeList<ScalarAtom> copy2 = new LargeList<>();
        for (LargeInteger i = list.size().shiftRight(1).add(1); i.lessThan(list.size()); i = i.add(1))
            copy2.add(list.get(i));
        return new ScalarList(copy2).median().subtract(new ScalarList(copy).median());
    }
    public ScalarAtom mad() {
        requireReal("mad");
        LargeList<ScalarAtom> copy = new LargeList<>();
        ScalarAtom m = this.median();
        for (ScalarAtom l : list)
            copy.add(l.subtract(m).abs());
        return new ScalarList(copy).median();
    }
    public ScalarAtom mode() throws CloneNotSupportedException {
        ScalarList copy = new ScalarList(list);
        LargeInteger frequency = LargeInteger.ONE;
        LargeList<LargeInteger> frequencies = new LargeList<>();
        for (LargeInteger i = LargeInteger.ONE; i.lessThan(copy.list.size()); i = i.add(1)){
            if(copy.list.get(i).greaterThan(copy.list.get(i.subtract(1)))){
                frequency = frequency.add(1);
                copy.list.remove(i);
                i = i.subtract(1);
            }else{
                frequencies.add(frequency);
                frequency = LargeInteger.ONE;
            }
        }
        LargeInteger maxIndex = LargeInteger.ZERO;
        LargeInteger maxValue = frequencies.getFirst();
        for (LargeInteger i = LargeInteger.ONE; i.lessThan(copy.list.size()); i = i.add(1)){
            if (frequencies.get(i).greaterThan(maxValue)) {
                maxIndex = i;
                maxValue = frequencies.get(i);
            }
        }
        return copy.get(maxIndex);
    }
    public ScalarAtom variancePopulation() {
        if (list.size().equals(LargeInteger.ZERO))
            throw new ArithmeticException("Cannot compute variance of empty list");
        ScalarAtom mean = mean();
        ScalarAtom sum = LargeInteger.ZERO;
        for (ScalarAtom atom : list) {
            ScalarAtom diff = atom.subtract(mean);
            sum = sum.add(diff.abs().square());
        }
        return sum.divide(list.size());
    }

    public ScalarAtom varianceSample() {
        if (list.size().lesserThanOrEqualTo(LargeInteger.ONE))
            throw new ArithmeticException("Sample variance requires at least TWO values");
        ScalarAtom mean = mean();
        ScalarAtom sum = LargeInteger.ZERO;
        for (ScalarAtom atom : list) {
            ScalarAtom diff = atom.subtract(mean);
            sum = sum.add(diff.abs().square());
        }
        return sum.divide(list.size().subtract(LargeInteger.ONE));
    }
    public LargePowerNode standardDeviation() {return (LargePowerNode) varianceSample().power(LargeFraction.HALF);}
    public LargePowerNode standardDeviationPopulation() {return (LargePowerNode) variancePopulation().power(LargeFraction.HALF);}
}
