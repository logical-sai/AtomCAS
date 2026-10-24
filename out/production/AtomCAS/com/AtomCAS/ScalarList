package com.AtomCAS.utilities;
public class ScalarList{
    public LargeList<ScalarAtom> list;
    public ScalarList(LargeList<ScalarAtom> list) {
        this.list = new LargeList<>();
        this.list.addAll(list);
        sort();
    }
    public ScalarAtom get(LargeInteger i){return list.get(i);}
    public LargeInteger size(){return list.size();}
    public void sort() {
        for (LargeInteger i = LargeInteger.zero; i.lessThan(list.size().subtract(LargeInteger.one)); i = (LargeInteger) i.add(LargeInteger.one)) {
            for (LargeInteger j = (LargeInteger) i.add(LargeInteger.one); j.lessThan(list.size()); j = (LargeInteger) j.add(LargeInteger.one)) {
                if (list.get(i).greaterThan(list.get(j))) {
                    ScalarAtom temp = list.get(i);
                    list.set(i, list.get(j));
                    list.set(j, temp);
                }
            }
        }
    }
    public ScalarAtom min() {return list.getFirst();}
    public ScalarAtom median() {
        if (list.size().equalsTo(LargeInteger.zero)) throw new IllegalArgumentException("Error: Cannot calculate median of empty list");
        if (list.size().isOdd()) return list.get(list.size().longDivision(LargeInteger.two));
        else return list.get(list.size().longDivision(LargeInteger.two)).add(list.get((LargeInteger) list.size().longDivision(LargeInteger.two).subtract(LargeInteger.one))).divide(LargeInteger.two);
    }
    public ScalarAtom mean() {
        ScalarAtom result = LargeInteger.zero;
        for (ScalarAtom atom : list) result = result.add(atom);
        return result.divide(list.size());
    }
    public ScalarAtom max() {return list.getLast();}
    public ScalarAtom IQR() {
        if (list.size().lessThan(LargeInteger.two))
            throw new IllegalArgumentException("Error: Need at least 2 values in list");
        this.sort();
        LargeList<ScalarAtom> copy = new LargeList<>();
        for (LargeInteger i = LargeInteger.zero; i.lessThan(list.size().longDivision(LargeInteger.two)); i = (LargeInteger) i.add(LargeInteger.one))
            copy.add(list.get(i));
        LargeList<ScalarAtom> copy2 = new LargeList<>();
        for (LargeInteger i = (LargeInteger) list.size().longDivision(LargeInteger.two).add(LargeInteger.one); i.lessThan(list.size()); i = (LargeInteger) i.add(LargeInteger.one))
            copy2.add(list.get(i));
        return new ScalarList(copy2).median().subtract(new ScalarList(copy).median());
    }
    public ScalarAtom mad() {
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
    public LargePowerNode standardDeviation() {return (LargePowerNode) varianceSample().power((LargeFraction) Parser.parse("1/2"));}
    public LargePowerNode standardDeviationPopulation() {return (LargePowerNode) variancePopulation().power((LargeFraction) Parser.parse("1/2"));}
}
