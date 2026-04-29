from tensorflow.python.ops.gen_array_ops import lower_bound

from calgraph3d.data.raytrace.OpticalVolumeObject import OpticalVolumeObject
from calgraph3d.data.raytrace.Intersection import Intersection
from matplotlib import pyplot as plt
import argparse
import logging
import numpy as np

logger = logging.getLogger(__name__)

if __name__ == '__main__':
    #This script creates an example plot of a lynnenberg lens, to illustrate the implementation of the volume raytracer
    parser = argparse.ArgumentParser(description='Plot a lynnenberg lens')
    parser.add_argument("--components", type=str,
                        choices=("rays", "lattice", "steps", "ior"),
                        default=("rays", "lattice", "steps", "ior"),
                        )
    parser.add_argument("--iterations-per-step", type=int, default=1, help="Number of iterations per step for the raytracer")
    parser.add_argument("--num-iterations", type=int, default=100, help="Number of iterations to run the raytracer for")
    parser.add_argument("--num-rays", type=int, default=11, help="Number of rays to trace")
    parser.add_argument("--output", type=str, default=None, help="Path to save the plot")
    parser.add_argument("--loglevel", type=str, default="warning", help="Logging level (debug, info, warning, error, critical)")
    parser.add_argument("--plotsize", default=(5,5), help="Size of the plot in inches")
    args = parser.parse_args()

    logging.basicConfig(level=args.loglevel.upper())

    volume = OpticalVolumeObject()
    volume_resolution = 32
    background_resolution = 512
    shape = (volume_resolution, volume_resolution, volume_resolution)
    scale = 0.9
    transformation = np.asarray([[scale, 0, 0, 0],
                                 [0, scale, 0, 0],
                                 [0, 0, scale, 0],
                                 [0, 0, 0, 1]], dtype=np.float32)

    volume.setTransformation(transformation, kind='globalToUnit')
    volume.setSize(shape)

    lattice = volume.getVertexPositions()
    #lattice is an array of shape (10, 10, 10, 3) containing the positions of the vertices in the volume

    #calculate lynnenberg lens ior (assume ball goes from [-1, -1, -1] to [1, 1, 1])
    # use lattice coordinates to calculate the ior
    radius = 1
    volume.ior = np.sqrt(2 - np.square(np.linalg.norm(lattice, axis=-1) / radius))
    #volume.ior = 1 + 0.5 * (1 - np.linalg.norm(lattice, axis=-1) / np.sqrt(3))
    #set translucency to be exactly 0 on the boundary
    volume.translucency = 1 - np.linalg.norm(lattice, axis=-1)
    volume.update()
    volume.updateIOR()
    start_positions = np.asarray([[-1, 0, 0]] * args.num_rays, dtype=np.float32)
    start_directions = np.asarray([[1, 0.0001, 0]] * args.num_rays, dtype=np.float32)
    start_positions[..., 1] += np.linspace(-1, 1, args.num_rays, endpoint=False) + (1 / args.num_rays)
    trajectory = []
    trajectory.append(start_positions.copy())
    positions = start_positions
    directions = start_directions
    intersection = Intersection(shape=args.num_rays)
    lowerBound = np.full(shape=args.num_rays, fill_value=0, dtype=np.float32)
    upperBound = np.full(shape=args.num_rays, fill_value=np.inf, dtype=np.float32)
    volume.checkInnerIntersection = True
    mask = volume.getIntersection(
        positions, directions, intersection=intersection, lowerBound=lowerBound, upperBound=upperBound)
    positions[mask] = intersection.position[mask]
    positions += 0.001 * directions
    directions = 2 * directions / volume_resolution
    trajectory.append(positions.copy())
    for i in range(args.num_iterations):
        positions, directions, iterations = volume.calculateRays(positions, directions, maxIterations=args.iterations_per_step)
        trajectory.append(positions.copy())

    trajectory = np.asarray(trajectory)
    fig, ax = plt.subplots()
    #only plot 2d representation of the rays
    if "ior" in args.components:
        #build lattice with 2d positions, use getRefractiveIndex(positions) to get the ior then use imshow to plot it
        imshow_lattice = np.meshgrid(np.linspace(-1, 1, background_resolution), np.linspace(-1, 1, background_resolution), [0], indexing='ij')
        imshow_lattice = np.stack(imshow_lattice, axis=-1)
        ior = volume.getRefractiveIndex(imshow_lattice)[..., 0]
        ior = np.maximum(1, ior) #clip ior to be at least air
        alpha = (volume.evaluate_inner_outer(imshow_lattice)[..., 0] > 0).astype(np.float32) * 0.5
        handle = ax.imshow(ior[:, :].T, extent=(-1, 1, -1, 1), origin='lower', cmap='viridis', alpha = alpha)
        #add colorbar for ior
        cbar = plt.colorbar(handle, ax=ax)
        cbar.set_label('Refractive Index')
    if "rays" in args.components:
        ax.plot(trajectory[..., 0], trajectory[..., 1], color='blue', label='Rays')
    if "lattice" in args.components:
        #draw lattice lines in gray
        for i in range(shape[0]):
            ax.plot(lattice[i, :, 0, 0], lattice[i, :, 0, 1], color='gray', alpha=0.5)
        for i in range(shape[1]):
            ax.plot(lattice[:, i, 0, 0], lattice[:, i, 0, 1], color='gray', alpha=0.5)
    if "steps" in args.components:
        ax.scatter(trajectory[:, :, 0], trajectory[:,:, 1], color='green', alpha=0.5)
    ax.set_aspect('equal')
    ax.set_xlim(-1.1, 1.1)
    ax.set_ylim(-1.1, 1.1)

    #remove upper and right line
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)

    if args.output is not None:
        with plt.rc_context({'svg.fonttype': 'none'}):
            fig.savefig(args.output, bbox_inches='tight', pad_inches=0, transparent=True)
    else:
        plt.show()