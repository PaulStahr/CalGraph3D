import numpy as np

from calgraph3d.data.raytrace.Intersection import Intersection
from calgraph3d.data.raytrace.MaterialType import MaterialType
from calgraph3d.data.raytrace.OpticalSurfaceObject import OpticalSurfaceObject


"""    public final SurfaceObject apply_surface_to_ray(
            RaySimulationObject ray, double[] trajectory, int trajectoryWriteIndex,
            float[] color, int colorWriteIndex, Intersection nearest,
            Vector3d direction) {
        SurfaceObject obj = ((SurfaceObject)nearest.object);
        nearest.c = direction.dot(nearest.normal);
        if (obj.materialType == null)
        {
        	throw new NullPointerException("Object has no material " + obj.id);
        }
        switch (obj.materialType)
        {
        	case ABSORBATION:
        		if (ray.readColorMiddle && obj.color.getAlpha() != 255 && obj.alphaCalculation != AlphaCalculation.IGNORE)
        		{
        			switch(obj.alphaCalculation)
        			{
        				case MULT:
        					readColor(ray, obj, ray.color);
        					multColor(color, colorWriteIndex, ray.color);
        					break;
        				case MIX:
        					readColor(ray, obj, ray.color);
        					mixColor(color, colorWriteIndex, ray.color);
        					break;
        				default:
        					break;
        			}
        			break;
        		}
        	case DELETION:
        		ray.invalidated = true;
        		break;
        	case REFRACTION:
        		double normaldot = nearest.normal.dot();
        		if (Double.isNaN(obj.iorq))
        		{
        			VariableStack vs = new VariableStack(this.vs);
        			Variable x = new Variable("x", nearest.position.x);
        			Variable y = new Variable("y", nearest.position.y);
        			Variable z = new Variable("z", nearest.position.z);
        			vs.addLocal(x);
        			vs.addLocal(y);
        			vs.addLocal(z);
        			Controller control = new Controller();
        			double ior0 = obj.ior0.calculate(vs, control).doubleValue();
        			double ior1 = obj.ior1.calculate(vs, control).doubleValue();
        			double ior = obj.invertNormal == nearest.c > 0 ? ior1 / ior0 : ior0 / ior1;
        			double iorq = ior * ior - 1;
        			double tmp = iorq * normaldot / (nearest.c * nearest.c) + 1;
        			direction.add(nearest.normal,(tmp > 0 ? (Math.sqrt(tmp) - 1) : -2.) * nearest.c / normaldot);
        		}
        		else
        		{
        			double tmp = (nearest.c > 0 ? obj.iorq : obj.inviorq) * normaldot / (nearest.c * nearest.c) + 1;
        			direction.add(nearest.normal,(tmp > 0 ? (Math.sqrt(tmp) - 1) : -2.) * nearest.c / normaldot);
        		}
        		break;
        	case REFLECTION:direction.add(nearest.normal, -2 * nearest.c/nearest.normal.dot());break;
        	case RANDOM:	direction.setAdd(nearest.normal, Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5);break;
        	default:
        		throw new IllegalArgumentException("Object with illegal material: " + obj.id);
        }
        return obj;
    }"""


class RaytraceScene:

    def __init__(self):
        self.optical_surface_objects = []
        self.optical_volume_objects = []
        self.successor_set = {}



    def calculateRays(
            self,
            position:np.ndarray,
            direction:np.ndarray,
            start_objects:list,
            trajectory:list=None,
            lower_bound:np.ndarray=None,
            upper_bound:np.ndarray=None):
        optical_objects = self.optical_surface_objects + self.optical_volume_objects
        successor_matrix = np.zeros(shape=(len(optical_objects), len(optical_objects)), dtype=bool)
        for source_object, destination_object  in self.successor_set:
            source_index = optical_objects.index(source_object)
            destination_index = optical_objects.index(destination_object)
            successor_matrix[source_index, destination_index] = True

        intersection = Intersection(position.shape[:-1])
        allowed_successors = np.zeros(shape=(*position.shape[:-1], len(optical_objects)), dtype=bool)
        for start_obect in start_objects:
            allowed_successors[..., optical_objects.index(start_obect)] = True
        for i in range(10):
            if trajectory is not None:
                trajectory.append(position.copy())
            intersection_id = np.full(shape=position.shape[:-1], dtype=int, fill_value=-1)
            direction /= np.linalg.norm(direction, axis=-1, keepdims=True)
            for i, optical_object in enumerate(optical_objects):
                mask = np.nonzero(allowed_successors[..., i])[0]
                current_intersection = intersection[mask]
                update_mask = optical_object.getIntersection(position[mask], direction[mask], current_intersection, lower_bound[mask], upper_bound[mask])
                intersection[mask[update_mask]] = current_intersection[update_mask]
                intersection_id[mask[update_mask]] = i
                lower_bound[mask] = intersection.distance[mask]
            for i, optical_object in enumerate(optical_objects):
                mask = np.nonzero(intersection_id == i)
                if len(mask[0]) == 0:
                    continue
                if isinstance(optical_object, OpticalSurfaceObject):
                    active_intersection = intersection[mask]
                    match optical_object.materialType:
                        case MaterialType.REFRACTION:
                            c = np.sum(direction[mask] * active_intersection.normal, axis=-1)
                            normaldot = np.sum(np.square(active_intersection.normal), axis=-1)
                            tmp = np.where(c > 0, optical_object.iorq, optical_object.inviorq) * normaldot / np.square(c) + 1
                            position[mask] = active_intersection.position
                            direction[mask] += active_intersection.normal * (np.where(tmp > 0, np.sqrt(tmp) - 1, -2) * c / normaldot)[..., np.newaxis]
                        case MaterialType.REFLECTION:
                            position[mask] = active_intersection.position
                            direction[mask] -= 2 * active_intersection.normal * np.sum(direction[mask] * active_intersection.normal, axis=-1, keepdims=True)
                        case _:
                            raise NotImplementedError(f"Material type {optical_object.materialType} not implemented")
                allowed_successors[mask] = successor_matrix[i]
        if trajectory is not None:
            trajectory.append(position.copy())