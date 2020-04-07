package util;
import geometry.Matrixd;
import maths.Operation;

public class OperationGeometry {
    public static final boolean parseMat (final Operation op, Matrixd mat){
    	int cols = mat.cols(), rows = mat.rows();
    	if (!(op instanceof maths.data.ArrayOperation)||op.size() != rows)
    		return false;
        for (int i=0;i<rows;i++){
        	Operation tmp = op.get(i);
        	if (!(tmp.isArray()) || tmp.size() !=cols)
            	return false;
            for (int j=0;j<cols;j++)
            	mat.set(i, j, tmp.get(j).isRealFloatingNumber() ? tmp.get(j).doubleValue() : Double.NaN);
        }
        return true;
    }
    

}
