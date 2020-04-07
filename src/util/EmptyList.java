package util;

import java.util.AbstractList;

public final class EmptyList<E> extends AbstractList<E> {

	@Override
	public E get(int arg0) {
		throw new ArrayIndexOutOfBoundsException();
	}

	@Override
	public int size() {
		return 0;
	}

}
