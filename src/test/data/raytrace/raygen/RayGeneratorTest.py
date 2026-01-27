import unittest
import numpy as np
import matplotlib.pyplot as plt
from calgraph3d.data.raytrace.raygen.RayGenerator import RayGenerator
from jsymmath.geometry.Geometry import Geometry

class TestDiffuseSampling(unittest.TestCase):

    def setUp(self):
        self.rng = np.random.default_rng(42)
        self.num_rays = 100000
        self.diffuse = 1
        self.tol = 0.15
        self.axes = np.eye(3)

        # Define some diagonal axes (normalized)
        self.diagonal_axes = [
            np.array([1, 1, 0]) / np.sqrt(2),
            np.array([1, 0, 1]) / np.sqrt(2),
            np.array([0, 1, 1]) / np.sqrt(2),
            np.array([1, 1, 1]) / np.sqrt(3),
            np.array([-1, 1, 1]) / np.sqrt(3)
        ]

    def check_diffuse_axis(self, axis, show_plot=True, axis_name=None):
        direction = np.tile(axis, (self.num_rays, 1))
        new_dirs = RayGenerator.apply_diffuse_to_directions(
            direction.copy(),
            diffuse=self.diffuse,
            rng=self.rng,
            three_dimensional=True,
            xp=np
        )

        # ---------- Polar angle (theta) test ----------
        cos_angles = new_dirs @ axis
        cos_angles = np.clip(cos_angles, 0, 1)
        angles = np.arccos(cos_angles)

        num_bins_theta = 64
        hist_theta, bin_edges_theta = np.histogram(
            angles,
            bins=num_bins_theta,
            range=(0, np.pi / 2),
            density=True
        )
        bin_centers_theta = 0.5 * (bin_edges_theta[:-1] + bin_edges_theta[1:])
        pdf_theta = 2 * np.cos(bin_centers_theta) * np.sin(bin_centers_theta)

        max_diff_theta = np.abs(hist_theta - pdf_theta).max()

        # ---------- Azimuth angle (phi) test ----------
        # Build local orthonormal frame
        M = Geometry.getOrthorgonalZMatrix(axis)

        # Transform directions into local frame
        local_dirs = new_dirs @ M

        phi = np.arctan2(local_dirs[:, 1], local_dirs[:, 0])
        phi = np.mod(phi, 2 * np.pi)

        num_bins_phi = 64
        hist_phi, bin_edges_phi = np.histogram(
            phi,
            bins=num_bins_phi,
            range=(0, 2 * np.pi),
            density=True
        )
        bin_centers_phi = 0.5 * (bin_edges_phi[:-1] + bin_edges_phi[1:])
        pdf_phi = np.ones_like(bin_centers_phi) / (2 * np.pi)

        max_diff_phi = np.abs(hist_phi - pdf_phi).max()

        # ---------- Optional plots ----------
        if show_plot and (max_diff_theta > self.tol or max_diff_phi > self.tol):
            fig, axes = plt.subplots(1, 2, figsize=(12, 4))

            # Theta plot
            axes[0].bar(
                bin_centers_theta,
                hist_theta,
                width=bin_edges_theta[1] - bin_edges_theta[0],
                alpha=0.5,
                label="Simulated"
            )
            axes[0].plot(
                bin_centers_theta,
                pdf_theta,
                "r-",
                linewidth=2,
                label="Theoretical"
            )
            axes[0].set_xlabel("Polar angle θ (rad)")
            axes[0].set_ylabel("Density")
            axes[0].set_title(f"Theta distribution ({axis_name})")
            axes[0].legend()

            # Phi plot
            axes[1].bar(
                bin_centers_phi,
                hist_phi,
                width=bin_edges_phi[1] - bin_edges_phi[0],
                alpha=0.5,
                label="Simulated"
            )
            axes[1].plot(
                bin_centers_phi,
                pdf_phi,
                "r-",
                linewidth=2,
                label="Uniform"
            )
            axes[1].set_xlabel("Azimuth φ (rad)")
            axes[1].set_ylabel("Density")
            axes[1].set_title(f"Azimuth distribution ({axis_name})")
            axes[1].legend()

            plt.tight_layout()
            plt.show()

        # ---------- Assertions ----------
        self.assertLess(
            max_diff_theta,
            self.tol,
            f"Theta distribution deviates too much for axis {axis_name}"
        )
        self.assertLess(
            max_diff_phi,
            self.tol,
            f"Azimuth distribution deviates too much for axis {axis_name}"
        )

    # Tests for main axes
    def test_diffuse_x_axis(self):
        self.check_diffuse_axis(self.axes[0], axis_name='x')

    def test_diffuse_y_axis(self):
        self.check_diffuse_axis(self.axes[1], axis_name='y')

    def test_diffuse_z_axis(self):
        self.check_diffuse_axis(self.axes[2], axis_name='z')

    # Test for diagonal axes
    def test_diffuse_diagonal_axes(self):
        for i, axis in enumerate(self.diagonal_axes):
            self.check_diffuse_axis(axis, axis_name=f'diagonal_{i+1}')
