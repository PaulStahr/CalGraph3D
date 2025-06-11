import numpy as np

from calgraph3d.data.raytrace.Intersection import Intersection
from calgraph3d.data.raytrace.MaterialType import MaterialType
from calgraph3d.data.raytrace.OpticalSurfaceObject import OpticalSurfaceObject
from calgraph3d.data.raytrace.OpticalVolumeObject import OpticalVolumeObject
import logging

logger = logging.getLogger(__name__)


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
            upper_bound:np.ndarray=None,
            xp=np):
        optical_objects = self.optical_surface_objects + self.optical_volume_objects
        optical_objects = [obj for obj in optical_objects if obj.active]
        successor_matrix = xp.zeros(shape=(len(optical_objects), len(optical_objects)), dtype=bool)
        for source_object, destination_object  in self.successor_set:
            if source_object not in optical_objects or destination_object not in optical_objects:
                continue
            source_index = optical_objects.index(source_object)
            destination_index = optical_objects.index(destination_object)
            successor_matrix[source_index, destination_index] = True

        intersection = Intersection(position.shape[:-1], xp=xp)
        allowed_successors = xp.zeros(shape=(*position.shape[:-1], len(optical_objects)), dtype=bool)
        for start_object in start_objects:
            if start_object not in optical_objects:
                logger.log(logging.WARNING, f"Start object {start_object} not in optical objects")
                continue
            allowed_successors[..., optical_objects.index(start_object)] = True
        for ibounce in range(20):
            if trajectory is not None:
                trajectory.append(position.copy())
            intersection_id = xp.full(shape=position.shape[:-1], dtype=int, fill_value=-1)
            direction /= xp.linalg.norm(direction, axis=-1, keepdims=True)
            lower_bound[:] = 1e-6
            upper_bound[:] = xp.inf
            # get the closest intersection
            for i, optical_object in enumerate(optical_objects):
                mask = xp.nonzero(allowed_successors[..., i])[0]
                if len(mask) == 0:
                    continue
                current_intersection = intersection[mask]
                update_mask = optical_object.getIntersection(position[mask], direction[mask], current_intersection, lower_bound[mask], upper_bound[mask], xp=xp)
                mask = mask[update_mask]
                intersection[mask] = current_intersection[update_mask]
                intersection_id[mask] = i
                upper_bound[mask] = current_intersection.distance[update_mask]
            # apply the closest optical object
            allowed_successors[...] = False
            for i, optical_object in enumerate(optical_objects):
                mask = xp.nonzero(intersection_id == i)
                if len(mask[0]) == 0:
                    continue
                if isinstance(optical_object, OpticalSurfaceObject):
                    active_intersection = intersection[mask]
                    match optical_object.materialType:
                        case MaterialType.ABSORPTION:
                            position[mask] = active_intersection.position
                            assert ~xp.all(successor_matrix[i])
                        case MaterialType.DELETION:
                            position[mask] = xp.nan
                            direction[mask] = xp.nan
                        case MaterialType.REFRACTION:
                            c = xp.sum(direction[mask] * active_intersection.normal, axis=-1)
                            normaldot = xp.sum(xp.square(active_intersection.normal), axis=-1)
                            tmp = xp.where(c > 0, optical_object.iorq, optical_object.inviorq) * normaldot / xp.square(c) + 1
                            position[mask] = active_intersection.position
                            direction[mask] += active_intersection.normal * (xp.where(tmp > 0, xp.sqrt(tmp) - 1, -2) * c / normaldot)[..., xp.newaxis]
                        case MaterialType.REFLECTION:
                            position[mask] = active_intersection.position
                            direction[mask] -= 2 * active_intersection.normal * xp.sum(direction[mask] * active_intersection.normal, axis=-1, keepdims=True)
                        case _:
                            raise NotImplementedError(f"Material type {optical_object.materialType} not implemented")
                if isinstance(optical_object, OpticalVolumeObject):
                    active_intersection = intersection[mask]
                    next_position, next_direction = optical_object.calculateRays(active_intersection.position, direction[mask], xp=xp)
                    position[mask] = next_position
                    direction[mask] = next_direction


                allowed_successors[mask] = successor_matrix[i]

        if trajectory is not None:
            trajectory.append(position.copy())
        return position, direction