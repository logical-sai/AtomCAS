package com.AtomCAS.utilities;
import java.util.*;

/*
Ignore everything above this point
 */
public class ArrayIntList implements Cloneable, Iterable<Integer> {
    int[] elementData;
    int size;
    public ArrayIntList() {this(10);}
    public ArrayIntList(int capacity) {
        elementData = new int[capacity];
        size = 0;
    }
    public boolean isEmpty() {return size == 0;}
    public int size() {return size;}
    public int get(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("index: " + index);
        return elementData[index];
    }
    public ArrayIntList set(int index, int value) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("index: " + index);
        elementData[index] = value;
        return this;
    }
    public ArrayIntList add(int value) {
        if(size == elementData.length)
            ensureCapacity(elementData.length*2);
        elementData[size++] = value;
        return this;
    }
    public ArrayIntList add(int index, int value) {
        if(index < 0 || index > size)
            throw new IndexOutOfBoundsException("index: " + index);
        if(size == elementData.length)
            ensureCapacity(Math.max(elementData.length*2, index));
        System.arraycopy(elementData, index, elementData, index + 1, size++ - index);
        elementData[index] = value;
        return this;
    }
    private void ensureCapacity(int capacity) {
        if (capacity <= elementData.length) return;
        int newCapacity = Math.max(1, elementData.length);
        while (newCapacity < capacity) {
            if (newCapacity > Integer.MAX_VALUE / 2) {
                newCapacity = capacity;
                break;
            }
            newCapacity *= 2;
        }

        elementData = Arrays.copyOf(elementData, newCapacity);
    }
    public int remove(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("index: " + index);
        int oldValue = elementData[index];
        System.arraycopy(elementData, index + 1, elementData, index, --size - index);
        return oldValue;
    }
    public ArrayIntList removeAll(int value) {
        int write = 0;
        for (int read = 0; read < size; read++) {
            if (elementData[read] != value)
                elementData[write++] = elementData[read];
        }
        size = write;
        return this;
    }
    public ArrayIntList removeAll(int[] values) {
        for (int value : values)
            removeAll(value);
        return this;
    }
    public ArrayIntList replaceAll(int oldValue, int newValue) {
        for(int i = 0; i < size; i++)
            if (elementData[i] == oldValue)
                elementData[i] = newValue;
        return this;
    }
    public ArrayIntList retainAll(int value) {
        for(int i = 0; i < size; i++)
            if (elementData[i] != value)
                remove(i--);
        return this;
    }
    public ArrayIntList retainAll(int[] values) {
        for (int i = 0; i < size; i++) {
            boolean inList = false;
            for(int value : values)
                if (value == elementData[i]) {
                    inList = true;
                    break;
                }
            if(!inList) remove(i--);
        }
        return this;
    }
    public ArrayIntList addAll(int[] values) {
        for (int value : values)
            add(value);
        return this;
    }
    public ArrayIntList clear() {
        size = 0;
        Arrays.fill(elementData, 0);
        return this;
    }
    public boolean contains(int value) {
        for(int i = 0; i < size; i++)
            if(elementData[i] == value)
                return true;
        return false;
    }
    public boolean containsAll(int[] values) {
        for (int value : values)
            if (!contains(value))
                return false;
        return true;
    }
    public int indexOf(int value) {
        for(int i = 0; i < size; i++)
            if(elementData[i] == value)
                return i;
        return -1;
    }
    public int lastIndexOf(int value) {
        for(int i = size-1; i >= 0; i--)
            if(elementData[i] == value)
                return i;
        return -1;
    }
    public int sum() {
        int sum = 0;
        for(int i = 0; i < size; i++)
            sum += elementData[i];
        return sum;
    }
    public double avg() {return (double) sum() / size;}
    public int max() {
        if (size == 0) return -1;
        int max = Integer.MIN_VALUE;
        for(int i = 0; i < size; i++)
            if(elementData[i] > max)
                max = elementData[i];
        return max;
    }
    public int min() {
        if (size == 0) return -1;
        int min = Integer.MAX_VALUE;
        for(int i = 0; i < size; i++)
            if(elementData[i] < min)
                min = elementData[i];
        return min;
    }
    public int[] toArray() {return Arrays.copyOf(elementData, size);}
    public boolean equals(Object o) {
        if(!(o instanceof ArrayIntList list)) return false;
        if(size != list.size)
            return false;
        for(int i = 0; i < size; i++)
            if(elementData[i] != list.elementData[i])
                return false;
        return true;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(int i = 0; i < size; i++) {
            sb.append(elementData[i]);
            if(i < size - 1)
                sb.append(", ");
        }
        return sb.append("]").toString();
    }
    @Override
    public ArrayIntList clone() {
        try {
            ArrayIntList clone = (ArrayIntList) super.clone();
            clone.elementData = elementData.clone();
            clone.size = size;
            return clone;
        } catch (CloneNotSupportedException e) {throw new AssertionError(e);}
    }
    @Override
    public int hashCode() {
        int hash = 1;
        for (int i = 0; i < size; i++)
            hash = 31 * hash + Integer.hashCode(elementData[i]);
        return hash;
    }
    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<>() {
            int index = 0;
            public boolean hasNext() {return index < size;}
            public Integer next() {return elementData[index++];}
            public void remove() {ArrayIntList.this.remove(--index);}
        };
    }
}

