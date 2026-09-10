package com.AtomCAS.utilities;
import java.util.*;
public class LargeList<T> implements Iterable<T>{
    private static final int CHUNK_SIZE = 1000;

    private LargeInteger size = LargeInteger.ZERO;
    private int sizeInt = 0;

    private int height = 0; // 0 means root is a leaf Object[1000]
    private Object[] root = new Object[CHUNK_SIZE];
    @SuppressWarnings("unchecked")
    public LargeList<T> copy() throws CloneNotSupportedException {
        LargeList<T> clone = (LargeList<T>) super.clone();
        clone.sizeInt = this.sizeInt;
        clone.height = this.height;
        clone.size = this.size;
        clone.root = this.root;
        return clone;
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if(sizeInt == 0) return "{}";
        else if(sizeInt > 100 || sizeInt < 0){
            for(int i = 0; i < 100; i++){
                sb.append(get(i));
                if(i < 99) sb.append(", ");
            }
            sb.append(" +").append(size.subtract(100)).append(" more");
            sb.append("}");
            return sb.toString();
        }
        for(int i = 0; i < sizeInt; i++){
            sb.append(get(i));
            if(i < sizeInt-1) sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
    }
    @Override
    public int hashCode() {
        int h = 1;
        for (T x : this)
            h = CASHash.mix(h, x == null ? 0 : x.hashCode());
        return CASHash.finish(h);
    }
    public void add(T value) {
        if (intSized())
            setRaw(sizeInt, value);
        else setRaw(size, value);
        size = size.add(1);
        if(intSized()){
            sizeInt++;
            if(sizeInt == -2147483648)
                sizeInt = -1;
        }
    }
    public void addFirst(T value) {
        add(null);
        if (intSized()) {
            for (int i = sizeInt - 1; i > 0; i--)
                set(i, get(i - 1));
            set(0, value);
            return;
        }
        for (LargeInteger i = size.subtract(1); i.greaterThan(LargeInteger.ZERO); i = i.subtract(1))
            set(i, get(i.subtract(1)));
        set(LargeInteger.ZERO, value);
    }
    public void set(LargeInteger index, T value){
        if (index.lessThan(LargeInteger.ZERO) || index.greaterThanOrEqualTo(size))
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for range [0, " + size + ")");
        setRaw(index, value);
    }
    public void set(int index, T value){
        if(index < 0 || index >= sizeInt)
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for range [0, " + size + ")");
        setRaw(index, value);
    }
    private void setRaw(LargeInteger index, T value){
        ensureCapacity(index);
        int[] digits = base1000Digits(index);
        Object[] node = root;
        for (int level = 0; level < height; level++) {
            int slot = digits[level];
            if (node[slot] == null)
                node[slot] = new Object[CHUNK_SIZE];
            node = (Object[]) node[slot];
        }
        node[digits[height]] = value;
    }
    private void setRaw(int index, T value) {
        ensureCapacity(LargeInteger.valueOf(index));
        int[] digits = base1000Digits(index);
        Object[] node = root;
        int leadingZeros = height + 1 - digits.length;
        for (int level = 0; level < height; level++) {
            int slot = level < leadingZeros ? 0 : digits[level - leadingZeros];
            if (node[slot] == null)
                node[slot] = new Object[CHUNK_SIZE];
            node = (Object[]) node[slot];
        }
        int leafSlot = height < leadingZeros ? 0 : digits[height - leadingZeros];
        node[leafSlot] = value;
    }
    public LargeInteger size() {return size;}
    public boolean intSized() {return sizeInt != -1;}
    public int intSize() {
        if (intSized())
            return sizeInt;
        throw new ArithmeticException("List too large to fit in int");
    }
    private void ensureCapacity(LargeInteger index) {
        while (index.greaterThanOrEqualTo(capacity())) {
            Object[] newRadicalTerm = new Object[CHUNK_SIZE];
            newRadicalTerm[0] = root;
            root = newRadicalTerm;
            height++;
        }
    }
    private LargeInteger capacity() {
        LargeInteger result = LargeInteger.ONE;
        for (int i = 0; i < height + 1; i++)
            result = result.multiply(CHUNK_SIZE);
        return result;
    }
    @SuppressWarnings("unchecked")
    public T get(LargeInteger index) {
        if (index.lessThan(LargeInteger.ZERO) || index.greaterThanOrEqualTo(size))
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for range [0, " + size + ")");
        if(intSized()) return get(Integer.parseInt(index.toString()));
        int[] path = base1000Digits(index);
        Object[] node = root;
        for (int level = 0; level < height; level++) {
            int slot = path[level];
            if (node[slot] == null)
                throw new UnsupportedOperationException("Operation with these types together not supported yet");
            node = (Object[]) node[slot];
        }
        return (T) node[path[height]];
    }
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= sizeInt)
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for range [0, " + sizeInt + ")");
        int[] digits = base1000Digits(index);
        Object[] node = root;
        int leadingZeros = height + 1 - digits.length;
        for (int level = 0; level < height; level++) {
            int slot = level < leadingZeros ? 0 : digits[level - leadingZeros];
            if (node[slot] == null)
                throw new UnsupportedOperationException("Missing LargeList node");
            node = (Object[]) node[slot];
        }
        int leafSlot = height < leadingZeros ? 0 : digits[height - leadingZeros];
        return (T) node[leafSlot];
    }
    public void swap(LargeInteger index1, LargeInteger index2){
        T temp = get(index1);
        set(index1, get(index2));
        set(index2, temp);
    }
    public void swap(int index1, int index2){
        T temp = get(index1);
        set(index1, get(index2));
        set(index2, temp);
    }
    public T getFirst(){return this.get(0);}
    public T getLast(){
        if(intSized()) return get(sizeInt-1);
        return get(size.subtract(1));
    }
    public void addAll(LargeList<T> other){for(T I: other) this.add(I);}
    public void remove(LargeInteger index){
        if(index.lessThan(LargeInteger.ZERO) || index.greaterThanOrEqualTo(size))
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for range [0, " + size + ")");
        for(LargeInteger i = index; i.lessThan(this.size().subtract(LargeInteger.ONE)); i = i.add(1))
            this.set(i, this.get(i.add(1)));
        this.set(this.size().subtract(1), null);
        size = size.subtract(1);
        if(intSized())
            sizeInt--;
        else
            if(size.equals(LargeInteger.valueOf(2147483647)))
                sizeInt = 2147483647;
    }
    public void remove(int index){
        if(index < 0 || index >= sizeInt)
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for range [0, " + sizeInt + ")");
        if(intSized()) {
            for (int i = index; i < this.sizeInt - 1; i++)
                this.set(i, this.get(i + 1));
            set(this.sizeInt - 1, null);
        }else{
            for(LargeInteger i = LargeInteger.valueOf(index); i.lessThan(size.subtract(1)); i = i.add(1))
                this.set(i, this.get(i.add(1)));
            this.set(this.size().subtract(LargeInteger.ONE), null);
        }
        size = size.subtract(1);
        if(intSized())
            sizeInt--;
        else
        if(size.equals(LargeInteger.valueOf(2147483647)))
            sizeInt = 2147483647;
    }
    public void removeFirst(){remove(0);}
    public void removeLast(){remove(size.subtract(1));}
    public void clear(){
        size = LargeInteger.ZERO;
        sizeInt = 0;
        height = 0;
        root = new Object[CHUNK_SIZE];
    }
    public boolean isEmpty(){return sizeInt==0;}
    private int[] base1000Digits(LargeInteger index) {
        if (index.equals(LargeInteger.ZERO))
            return new int[]{0};
        int[] temp = new int[height + 1];
        LargeInteger copy = index;
        int count = 0;
        long base = 1000;
        while (copy.greaterThan(LargeInteger.ZERO)) {
            temp[count++] = Math.toIntExact(copy.mod(base));
            copy = copy.divide(LargeInteger.THOUSAND);
        }
        int[] result = new int[count];
        for (int i = 0; i < count; i++)
            result[i] = temp[count - 1 - i];
        return result;
    }
    private static int[] base1000Digits(int index) {
        if (index == 0)
            return new int[]{0};
        int len = 0;
        int temp = index;
        while (temp > 0) {
            len++;
            temp /= CHUNK_SIZE;
        }
        int[] digits = new int[len];
        for (int i = len - 1; i >= 0; i--) {
            digits[i] = index % CHUNK_SIZE;
            index /= CHUNK_SIZE;
        }
        return digits;
    }
    public boolean equals(LargeList<T> other){
        if (this.intSized() && other.intSized()) {
            if (this.sizeInt != other.sizeInt) return false;
            for (int i = 0; i < sizeInt; i++)
                if (!this.get(i).equals(other.get(i)))
                    return false;
            return true;
        }
        if(other.size().doesntEqual(size))
            return false;
        for(LargeInteger i = LargeInteger.ZERO; i.lessThan(size); i = i.add(1))
            if(!this.get(i).equals(other.get(i)))
                return false;
        return true;
    }
    public ArrayList<T> toArrayList(){
        if(!intSized()) return null;
        ArrayList<T> list = new ArrayList<>();
        for(T item: this)
            list.add(item);
        return list;
    }
    public Object[] toArray() {
        if (!intSized()) return null;
        return Arrays.copyOf(root, sizeInt);
    }
    public void reverse(){
        if(intSized()){
            for(int i = 0; i < this.sizeInt/2; i++)
                swap(i, this.sizeInt-i-1);
        }else{
            for(LargeInteger i = LargeInteger.ZERO; i.lessThan(size.shiftRight(1)); i = i.add(1))
                swap(i, size().subtract(i));
        }
    }
    @Override
    public Iterator<T> iterator() {
        if (intSized())
            return new Iterator<>() {
                private int index = 0;
                @Override
                public boolean hasNext() {return index < sizeInt;}
                @Override
                public T next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return get(index++);
                }
            };
        return new Iterator<>() {
            private LargeInteger index = LargeInteger.ZERO;
            @Override
            public boolean hasNext() {return index.lessThan(size);}
            @Override
            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                T value = get(index);
                index = index.add(1);
                return value;
            }
        };
    }
}
