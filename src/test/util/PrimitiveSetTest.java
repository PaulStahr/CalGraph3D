package test.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import util.data.SortedIntegerArrayList;
import util.functional.IsEqualPredicate;

public class PrimitiveSetTest {
    public void testSet(SortedIntegerArrayList s, final Integer e0, final Integer e1, final Integer e2) {
        s.add(e0);
        s.add(e1);
        s.add(e0);
        assertEquals(s.size(), 2);
        assert(s.contains(e1));
        assert(s.removeIf(new IsEqualPredicate<Integer>(e1)));
        assertEquals(s.size(), 1);
        assert(!s.contains(e1));
        assert(!s.removeIf(new IsEqualPredicate<Integer>(e1)));
    }

    @Test
    public void testSortedIntegerList() {testSet(new SortedIntegerArrayList(), 7, 12, 64);}
}
