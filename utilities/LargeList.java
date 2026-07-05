package com.AtomCAS.utilities;
import java.util.Iterator;
import java.util.NoSuchElementException;
public class LargeList<T> implements Iterable<T>{
    private static final int CHUNK_SIZE = 1000;

    private LargeInteger size = LargeInteger.zero;
    private int sizeInt = 0;
    private boolean intSized = true;

    private int height = 0; // 0 means root is a leaf Object[1000]
    private Object[] root = new Object[CHUNK_SIZE];
    public LargeList<T> copy() {
        LargeList<T> lt = new LargeList<>();
        if (intSized())
            for (int i = 0; i < sizeInt; i++)
                lt.add(get(i));
        else
            for (LargeInteger i = LargeInteger.zero; i.lessThan(size); i = i.add(1))
                lt.add(get(i));
        return lt;
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < sizeInt; i++) {
            sb.append(this.get(i));
            if (i < sizeInt-1) sb.append(", ");
        }
        if(!intSized()){
            sb.append(", ");
            for(LargeInteger i = new LargeInteger(2147483648L); i.lessThan(size); i=i.add(1)){
                sb.append(this.get(i));
                if(i.lessThan(size.subtract(1))) sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }
    public void add(T value) {
        if (intSized())
            setRaw(sizeInt, value);
        else
            setRaw(size, value);
        size = size.add(1);
        if(intSized()){
            sizeInt++;
            if(sizeInt == 2147483647)
                intSized = false;
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
        for (LargeInteger i = size.subtract(1); i.greaterThan(LargeInteger.zero); i = i.subtract(1))
            set(i, get(i.subtract(1)));
        set(LargeInteger.zero, value);
    }
    public void set(LargeInteger index, T value){
        if (index.lessThan(LargeInteger.zero) || index.greaterThanOrEqualTo(size))
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for range [0, " + size + ")");
        setRaw(index, value);
    }
    public void set(int index, T value){
        if(index < 0 || index >= sizeInt)
            throw new IndexOutOfBoundsException("Error: Index " + index + " out of bounds for range [0, " + size + ")");
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
        ensureCapacity(new LargeInteger(index));
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
    public boolean intSized() {return intSized;}
    public int intSize() {
        if (intSized())
            return sizeInt;
        throw new ArithmeticException("Error: List too large to fit in int");
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
        LargeInteger result = LargeInteger.one;
        for (int i = 0; i < height + 1; i++)
            result = (LargeInteger) result.multiply(new LargeInteger(CHUNK_SIZE));
        return result;
    }
    @SuppressWarnings("unchecked")
    public T get(LargeInteger index) {
        if (index.lessThan(LargeInteger.zero) || index.greaterThanOrEqualTo(size))
            throw new IndexOutOfBoundsException("Error: Index " + index + " out of bounds for range [0, " + size + ")");
        if(intSized()) return get(Integer.parseInt(index.toString()));
        int[] path = base1000Digits(index);
        Object[] node = root;
        for (int level = 0; level < height; level++) {
            int slot = path[level];
            if (node[slot] == null)
                throw new UnsupportedOperationException("Error: Operation with these types together not supported yet");
            node = (Object[]) node[slot];
        }
        return (T) node[path[height]];
    }
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= sizeInt)
            throw new IndexOutOfBoundsException("Error: Index " + index + " out of bounds for range [0, " + sizeInt + ")");
        int[] digits = base1000Digits(index);
        Object[] node = root;
        int leadingZeros = height + 1 - digits.length;
        for (int level = 0; level < height; level++) {
            int slot = level < leadingZeros ? 0 : digits[level - leadingZeros];
            if (node[slot] == null)
                throw new UnsupportedOperationException("Error: Missing LargeList node");
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
        if(index.lessThan(LargeInteger.zero) || index.greaterThanOrEqualTo(size))
            throw new IndexOutOfBoundsException("Error:  Index " + index + " out of bounds for range [0, " + size + ")");
        for(LargeInteger i = index; i.lessThan(this.size().subtract(LargeInteger.one)); i = i.add(1))
            this.set(i, this.get((LargeInteger) i.add(LargeInteger.one)));
        this.set((LargeInteger) this.size().subtract(LargeInteger.one), null);
        size = (LargeInteger) size.subtract(LargeInteger.one);
        if(intSized())
            sizeInt--;
        else
            if(size.equalsTo(new LargeInteger(2147483647))){
                sizeInt = 2147483647;
                intSized = true;
            }
    }
    public void remove(int index){
        if(index < 0 || index >= sizeInt)
            throw new IndexOutOfBoundsException("Error:  Index " + index + " out of bounds for range [0, " + sizeInt + ")");
        if(intSized()) {
            for (int i = index; i < this.sizeInt - 1; i++)
                this.set(i, this.get(i + 1));
            set(this.sizeInt - 1, null);
        }else{
            for(LargeInteger i = new LargeInteger(index); i.lessThan(size.subtract(1)); i = i.add(1))
                this.set(i, this.get(i.add(1)));
            this.set((LargeInteger) this.size().subtract(LargeInteger.one), null);
        }
        size = (LargeInteger) size.subtract(LargeInteger.one);
        if(intSized())
            sizeInt--;
        else
        if(size.equalsTo(new LargeInteger(2147483647))){
            sizeInt = 2147483647;
            intSized = true;
        }
    }
    public void removeFirst(){remove(0);}
    public void removeLast(){remove((LargeInteger) size.subtract(LargeInteger.one));}
    public void clear(){
        size = LargeInteger.zero;
        sizeInt = 0;
        intSized = true;
        height = 0;
        root = new Object[CHUNK_SIZE];
    }
    public String pathOf(LargeInteger index) {
        int[] path = base1000Digits(index);
        StringBuilder sb = new StringBuilder("LargeList");
        for (int slot : path)
            sb.append("[").append(slot).append("]");
        return sb.toString();
    }
    private int slotAt(int[] digits, int level) {
        int leadingZeros = height + 1 - digits.length;
        return level < leadingZeros ? 0 : digits[level - leadingZeros];
    }
    private int[] base1000Digits(LargeInteger index) {
        if (index.equalsTo(LargeInteger.zero))
            return new int[]{0};
        int[] temp = new int[height + 1];
        LargeInteger copy = index;
        int count = 0;
        LargeInteger base = new LargeInteger(CHUNK_SIZE);
        while (copy.greaterThan(LargeInteger.zero)) {
            temp[count++] = Integer.parseInt(copy.mod(base).toString());
            copy = copy.longDivision(base);
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
    public boolean equalsTo(LargeList<T> other){
        if (this.intSized() && other.intSized()) {
            if (this.sizeInt != other.sizeInt) return false;
            for (int i = 0; i < sizeInt; i++)
                if (!this.get(i).equals(other.get(i)))
                    return false;
            return true;
        }
        if(other.size().notEqualsTo(size))
            return false;
        for(LargeInteger i = LargeInteger.zero; i.lessThan(size); i = i.add(1))
            if(!this.get(i).equals(other.get(i)))
                return false;
        return true;
    }
    @Override
    public Iterator<T> iterator() {
        if (intSized()) {
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
        }
        return new Iterator<>() {
            private LargeInteger index = LargeInteger.zero;
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
