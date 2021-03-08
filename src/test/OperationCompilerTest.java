package test;

import static org.junit.Assert.assertTrue;

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
            testCompileToStringInverse(OperationCompiler.compile(str));
        } catch (OperationParseException e) {
            throw new AssertionError(str, e);
        }
    }
    
    private void testCompileToStringInverse(Operation op) {
        Operation cpop = null;
        try {
            cpop = OperationCompiler.compile(op.toString());
            assertTrue(cpop.equals(op));
        }catch(AssertionError e){
            throw new AssertionError(cpop.toString());
        } catch (OperationParseException e) {
            throw new AssertionError(op.toString(), e);
        }
    }   
}
