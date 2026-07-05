package com.AtomCAS.utilities;

import java.util.ArrayList;

public class ScalarList{
    public LargeList<ScalarAtom> list;
    boolean sorted;
    public ScalarList(LargeList<ScalarAtom> list) {
        this.list = new LargeList<>();
        this.list.addAll(list);
        for(LargeInteger i = LargeInteger.one; i.lessThan(this.list.size()); i=i.add(1)){
            if(this.list.get(i).lessThan(this.list.get(i.subtract(1)))){
                sorted = false;
                return;
            }
        }
        sorted = true;
    }
    public ScalarAtom get(LargeInteger i){return list.get(i);}
    public LargeInteger size(){return list.size();}
    private static final LargeInteger MAX_ARRAYLIST_SORT_SIZE = new LargeInteger(100000);
    private void requireReal(String operation) {
        for (ScalarAtom atom : list) {
            if (atom.isNotReal())
                throw new IllegalStateException("Error: " + operation + " is only defined for real-valued ScalarLists");
        }
    }
    public void sort() {
        requireReal("sort");
        if (sorted) return;

        if (list.intSized())
            sortRangeWithArrayList(LargeInteger.zero, list.size());
        else
            mergeSort(LargeInteger.zero, list.size());

        sorted = true;
    }
    private void mergeSort(LargeInteger bottom, LargeInteger top) {
        LargeInteger length = (LargeInteger) top.subtract(bottom);
        if (length.lesserThanOrEqualTo(MAX_ARRAYLIST_SORT_SIZE)) {
            sortRangeWithArrayList(bottom, top);
            return;
        }
        LargeInteger mid = ((LargeInteger) bottom.add(top)).longDivision(LargeInteger.two);
        mergeSort(bottom, mid);
        mergeSort(mid, top);
        merge(bottom, mid, top);
    }
    private void sortRangeWithArrayList(LargeInteger bottom, LargeInteger top) {
        int n = Integer.parseInt(top.subtract(bottom).toString());
        ArrayList<ScalarAtom> copy = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            copy.add(list.get(bottom.add(i)));
        copy.sort(ScalarList::compareScalars);
        for (int i = 0; i < n; i++)
            list.set(bottom.add(i), copy.get(i));
    }
    private void merge(LargeInteger bottom, LargeInteger mid, LargeInteger top) {
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
        for (LargeInteger k = LargeInteger.zero; k.lessThan(temp.size()); k = k.add(1))
            list.set((LargeInteger) bottom.add(k), temp.get(k));
    }
    private static int compareScalars(ScalarAtom a, ScalarAtom b) {return Math.toIntExact(a.compareTo(b));}
    public ScalarAtom range(){
        requireReal("range");
        return max().subtract(min());
    }
    public ScalarAtom min() {
        requireReal("min");
        if(!sorted) sort();
        return list.getFirst();
    }
    public ScalarAtom median() {
        requireReal("median");
        if(!sorted) sort();
        if (list.intSize() == 0) throw new IllegalArgumentException("Error: Cannot calculate median of empty list");
        if (list.size().isOdd()) return list.get(list.size().longDivision(LargeInteger.two));
        else return list.get(list.size().longDivision(LargeInteger.two)).add(list.get((LargeInteger) list.size().longDivision(LargeInteger.two).subtract(LargeInteger.one))).divide(LargeInteger.two);
    }
    public ScalarAtom mean() {
        ScalarAtom result = LargeInteger.zero;
        for (ScalarAtom atom : list) result = result.add(atom);
        return result.divide(list.size());
    }
    public ScalarAtom max() {
        requireReal("max");
        if(!sorted) sort();
        return list.getLast();
    }
    public ScalarAtom IQR() {
        requireReal("IQR");
        if (list.size().lessThan(LargeInteger.two))
            throw new IllegalArgumentException("Error: Need at least 2 values in list");
        if(!sorted) sort();
        LargeList<ScalarAtom> copy = new LargeList<>();
        for (LargeInteger i = LargeInteger.zero; i.lessThan(list.size().longDivision(LargeInteger.two)); i = (LargeInteger) i.add(LargeInteger.one))
            copy.add(list.get(i));
        LargeList<ScalarAtom> copy2 = new LargeList<>();
        for (LargeInteger i = (LargeInteger) list.size().longDivision(LargeInteger.two).add(LargeInteger.one); i.lessThan(list.size()); i = (LargeInteger) i.add(LargeInteger.one))
            copy2.add(list.get(i));
        return new ScalarList(copy2).median().subtract(new ScalarList(copy).median());
    }
    public ScalarAtom mad() {
        requireReal("mad");
        LargeList<ScalarAtom> copy = new LargeList<>();
        ScalarAtom m = this.median();
        for (ScalarAtom l : list)
            copy.add(l.subtract(m).magnitude());
        return new ScalarList(copy).median();
    }
    public ScalarAtom mode() {
        ScalarList copy = new ScalarList(list);
        LargeInteger frequency = LargeInteger.one;
        LargeList<LargeInteger> frequencies = new LargeList<>();
        for (LargeInteger i = LargeInteger.one; i.lessThan(copy.list.size()); i = (LargeInteger) i.add(LargeInteger.one)){
            if(copy.list.get(i).greaterThan(copy.list.get((LargeInteger) i.subtract(LargeInteger.one)))){
                frequency = (LargeInteger) frequency.add(LargeInteger.one);
                copy.list.remove(i);
                i = (LargeInteger) i.subtract(LargeInteger.one);
            }else{
                frequencies.add(frequency);
                frequency = LargeInteger.one;
            }
        }
        LargeInteger maxIndex = LargeInteger.zero;
        LargeInteger maxValue = frequencies.getFirst();
        for (LargeInteger i = LargeInteger.one; i.lessThan(copy.list.size()); i = (LargeInteger) i.add(LargeInteger.one)){
            if (frequencies.get(i).greaterThan(maxValue)) {
                maxIndex = i;
                maxValue = frequencies.get(i);
            }
        }
        return copy.get(maxIndex);
    }
    public ScalarAtom variancePopulation() {
        if (list.size().equalsTo(LargeInteger.zero))
            throw new ArithmeticException("Error: Cannot compute variance of empty list");
        ScalarAtom mean = mean();
        ScalarAtom sum = LargeInteger.zero;
        for (ScalarAtom atom : list) {
            ScalarAtom diff = atom.subtract(mean);
            sum = sum.add(diff.magnitude().square());
        }
        return sum.divide(list.size());
    }

    public ScalarAtom varianceSample() {
        if (list.size().lesserThanOrEqualTo(LargeInteger.one))
            throw new ArithmeticException("Error: Sample variance requires at least two values");
        ScalarAtom mean = mean();
        ScalarAtom sum = LargeInteger.zero;
        for (ScalarAtom atom : list) {
            ScalarAtom diff = atom.subtract(mean);
            sum = sum.add(diff.magnitude().square());
        }
        return sum.divide(list.size().subtract(LargeInteger.one));
    }
    public LargePowerNode standardDeviation() {return (LargePowerNode) varianceSample().power(LargeFraction.half);}
    public LargePowerNode standardDeviationPopulation() {return (LargePowerNode) variancePopulation().power(LargeFraction.half);}
}
