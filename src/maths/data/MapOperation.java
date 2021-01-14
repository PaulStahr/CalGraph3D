package maths.data;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import maths.MengenOperation;
import maths.Operation;

public class MapOperation extends MengenOperation{
	private final HashMap<Operation, Operation> operations = new HashMap<>();

	public final Operation get(Operation op)
	{
		return operations.get(op);
	}
	
	@Override
	public int isElementOf(Operation element) {
		return 0;
	}

	@Override
	public StringBuilder toString(Print type, StringBuilder stringBuilder) {
		stringBuilder.append('{');
		boolean first = true;
		for (Entry<Operation, Operation> entry : operations.entrySet()) {
			if (first)
			{
				first = false;
			}
			else
			{
				stringBuilder.append(',');
			}
		    entry.getValue().toString(type, entry.getKey().toString(type, stringBuilder).append(':'));
		}
		return stringBuilder.append('}');
	}

	public Operation set(Operation key, Operation value) {
		if (!key.isPrimitive())
		{
			throw new IllegalArgumentException("Object has to be primitive");
		}
		operations.put(key, value);
		return this;
	}

	public final Set<Entry<Operation, Operation>> entrySet() {
		return operations.entrySet();
	}
}
