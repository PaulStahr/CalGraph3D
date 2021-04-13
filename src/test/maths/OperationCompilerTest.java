package test.maths;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import maths.Operation;
import maths.OperationCompiler;
import maths.exception.OperationParseException;

public class OperationCompilerTest {
    @Test
    public void testCompile()
    {
        testCompileToStringInverse("2*(4+5)");
        testCompileToStringInverse("2^(4+5)/42");
    }

    private void testCompileToStringInverse(String str) {
        try {
            Operation op = OperationCompiler.compile(str);
            assertEquals(OperationCompiler.compile(op.toString()), op);
        } catch (OperationParseException e) {
            throw new AssertionError(str, e);
        }
    }   
}
