package com.AtomCAS.utilities;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
public class LargeList<T> implements Iterable<T>{
    private static final int CHUNK_SIZE = 1000;
    private LargeInteger size = LargeInteger.zero;
    private int height = 0; // 0 means root is a leaf Object[1000]
    private Object[] root = new Object[CHUNK_SIZE];
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (LargeInteger i = LargeInteger.zero; i.lessThan(size); i = i.add(1)) {
            sb.append(this.get(i));
            if (i.notEqualsTo(size.subtract(LargeInteger.one))) sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
    }
    public void add(T value) {
        setRaw(size, value);
        size = (LargeInteger) size.add(LargeInteger.one);
    }
    public void addFirst(T value){
        this.add(null);
        for(LargeInteger i = (LargeInteger) size.subtract(LargeInteger.one); i.greaterThan(LargeInteger.zero); i = i.subtract(1))
            this.set(i, this.get((LargeInteger) i.subtract(LargeInteger.one)));
        this.set(LargeInteger.zero, value);
    }
    public void set(LargeInteger index, T value){
        if (index.sign().equalsTo(LargeInteger.negativeOne) || index.greaterThanOrEqualTo(size))
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for range [0, " + size + ")");
        setRaw(index, value);
    }
    private void setRaw(LargeInteger index, T value) {
        ensureCapacity(index);
        ArrayList<Integer> digits = base1000Digits(index);
        while (digits.size() < height + 1)
            digits.addFirst(0);
        Object[] node = root;
        for (int level = 0; level < height; level++) {
            int slot = digits.get(level);
            if (node[slot] == null)
                node[slot] = new Object[CHUNK_SIZE];
            node = (Object[]) node[slot];
        }
        node[digits.get(height)] = value;
    }
    public LargeInteger size(){return size;}
    private void ensureCapacity(LargeInteger index) {
        while (index.greaterThanOrEqualTo(capacity())) {
            Object[] newRadicalTerm = new Object[CHUNK_SIZE];
            newRadicalTerm[0] = root;
            root = newRadicalTerm;
            height++;
        }
    }
    private LargeInteger capacity() {
        LargeInteger result = LargeInteger.one;
        for (int i = 0; i < height + 1; i++)
            result = (LargeInteger) result.multiply(new LargeInteger(CHUNK_SIZE));
        return result;
    }
    @SuppressWarnings("unchecked")
    public T get(LargeInteger index) {
        if (index.sign().equalsTo(LargeInteger.negativeOne) || index.greaterThanOrEqualTo(size))
            throw new IndexOutOfBoundsException("Error: Index " + index + " out of bounds for range [0, " + size + ")");
        ArrayList<Integer> path = base1000Digits(index);
        while (path.size() < height + 1)
            path.addFirst(0);
        Object[] node = root;
        for (int level = 0; level < height; level++) {
            int slot = path.get(level);
            if (node[slot] == null)
                throw new UnsupportedOperationException("Error: Operation with these two types together not supported yet");
            node = (Object[]) node[slot];
        }
        return (T) node[path.get(height)];
    }
    public void swap(LargeInteger index1, LargeInteger index2){
        T temp = get(index1);
        set(index1, get(index2));
        set(index2, temp);
    }
    public T getFirst(){return this.get(LargeInteger.zero);}
    public T getLast(){return this.get((LargeInteger) size.subtract(LargeInteger.one));}
    public void addAll(LargeList<T> other){for(T I: other) this.add(I);}
    public void remove(LargeInteger index){
        if(index.sign().equalsTo(LargeInteger.negativeOne) || index.greaterThanOrEqualTo(size))
            throw new IndexOutOfBoundsException("Error:  Index " + index + " out of bounds for range [0, " + size + ")");
        for(LargeInteger i = index.magnitude(); i.lessThan(this.size().subtract(LargeInteger.one)); i = i.add(1))
            this.set(i, this.get((LargeInteger) i.add(LargeInteger.one)));
        this.set((LargeInteger) this.size().subtract(LargeInteger.one), null);
        size = (LargeInteger) size.subtract(LargeInteger.one);
    }
    public void removeFirst(){remove(LargeInteger.zero);}
    public void removeLast(){remove((LargeInteger) size.subtract(LargeInteger.one));}
    public void clear(){
        size = LargeInteger.zero;
        height = 0;
        root = new Object[CHUNK_SIZE];
    }
    public String pathOf(LargeInteger index) {
        ArrayList<Integer> path = base1000Digits(index);
        while (path.size() < height + 1)
            path.addFirst(0);
        StringBuilder sb = new StringBuilder("LargeList");
        for (int slot : path)
            sb.append("[").append(slot).append("]");
        return sb.toString();
    }
    private ArrayList<Integer> base1000Digits(LargeInteger index) {
        ArrayList<Integer> digits = new ArrayList<>();
        LargeInteger copy = index;
        if (copy.equalsTo(LargeInteger.zero)) {
            digits.add(0);
            return digits;
        }
        LargeInteger base = new LargeInteger(CHUNK_SIZE);
        while (copy.greaterThan(LargeInteger.zero)) {
            LargeInteger digit = (LargeInteger) copy.mod(base);
            digits.addFirst(Integer.parseInt(digit.toString()));
            copy = copy.longDivision(base);
        }
        return digits;
    }
    public boolean equalsTo(LargeList<T> other){
        if(other.size() != size)
            return false;
        for(LargeInteger i = LargeInteger.zero; i.lessThan(size); i = i.add(1))
            if(!this.get(i).equals(other.get(i)))
                return false;
        return true;
    }
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private LargeInteger index = LargeInteger.zero;
            @Override
            public boolean hasNext() {return index.lessThan(size);}
            @Override
            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                T value = get(index);
                index = (LargeInteger) index.add(LargeInteger.one);
                return value;
            }
        };
    }
    public interface LargeComparator<T> { long compare(T a, T b);}
    public void sort(LargeComparator<T> comparator) {
        if (size.lesserThanOrEqualTo(LargeInteger.one)) return;
        for (LargeInteger i = LargeInteger.zero; i.lessThan(size.subtract(LargeInteger.one)); i = i.add(1)) {
            for (LargeInteger j = (LargeInteger) i.add(LargeInteger.one); j.lessThan(size); j = j.add(1)) {
                if (comparator.compare(this.get(j), this.get(i)) < 0L){
                    T temp = this.get(i);
                    this.set(i, this.get(j));
                    this.set(j, temp);
                }
            }
        }
    }
}
