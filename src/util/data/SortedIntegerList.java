package util.data;

public interface SortedIntegerList extends IntegerList{
    boolean hasMatch(int[] data, int begin, int end);

    boolean contains(int e1);

    boolean add(int e0);
}