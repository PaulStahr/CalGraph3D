"""Tests for near-axis (almost-parallel) parabolic surface intersection.

The parabolic solve reduces to a quadratic in the ray parameter; for rays
nearly parallel to the axis the terms cancel and the result is only as
accurate as the working dtype allows.  These tests document and quantify
that behaviour in float32.  They intentionally assert the current (broken)
behaviour of the source for some cases and must be revisited if
``OpticalSurfaceObject.getIntersection`` is fixed.
"""
import numpy as np

from calgraph3d.data.raytrace.OpticalSurfaceObject import OpticalSurfaceObject
from calgraph3d.data.raytrace.SurfaceType import SurfaceType
from calgraph3d.data.raytrace.Intersection import Intersection
import logging

logger = logging.getLogger(__name__)

L = 10.0
RAY_POS = [0.5, 0.0, 20.0]
F32_EPS = np.finfo(np.float32).eps


def make_paraboloid(dtype):
    obj = OpticalSurfaceObject()
    obj.surf = SurfaceType.PARABOLIC
    obj.midpoint = np.zeros(3, dtype=dtype)
    obj.direction = np.array([0, 0, L], dtype=dtype)
    obj.maxRadiusGeometric = 5.0
    obj.minRadiusGeometric = 0.0
    obj.update()
    return obj


def intersect_near_axis(obj, theta, dtype):
    ray_pos = np.array([RAY_POS], dtype=dtype)
    ray_dir = np.array([[np.sin(theta), 0.0, -np.cos(theta)]], dtype=dtype)
    ray_dir = ray_dir / np.linalg.norm(ray_dir, axis=-1, keepdims=True)
    inter = Intersection(shape=(1,))
    obj.getIntersection(
        ray_pos, ray_dir, inter,
        ray_tmin=np.array([0.0], dtype=dtype),
        ray_tmax=np.array([np.inf], dtype=dtype))
    return inter


def on_paraboloid(pos):
    rho2 = pos[0] ** 2 + pos[1] ** 2
    w = pos[2]
    return np.isclose(rho2, 2 * L * (L - w), rtol=1e-6, atol=1e-4)


def test_float64_near_axis_hits_surface():
    theta = 1e-2
    inter = intersect_near_axis(make_paraboloid(np.float64), theta, np.float64)
    d = float(inter.distance[0])
    assert np.isfinite(d) and d > 0
    pos = np.asarray(inter.position[0])
    assert np.all(np.isfinite(pos)), f"position not finite: {pos}"
    assert on_paraboloid(pos), f"point {pos} not on paraboloid"
    assert np.isclose(d, 10.0185, atol=1e-3), f"expected ~10.0185, got {d}"


def test_float32_near_axis_cancellation_error():
    # float32 result deviates from the float64 reference far beyond
    # float32 machine precision: catastrophic cancellation.
    # Known-bug documentation: expected to fail once the source is fixed.
    theta = 5e-3
    ref = float(intersect_near_axis(make_paraboloid(np.float64), theta, np.float64).distance[0])
    assert np.isfinite(ref) and ref > 0
    got = float(intersect_near_axis(make_paraboloid(np.float32), theta, np.float32).distance[0])
    assert np.isfinite(got) and got > 0, f"float32 lost the hit at theta={theta}: got {got}"
    err_rel = abs(got - ref) / ref
    logger.log(logging.INFO, f"float32 catastrophic cancellation at theta={theta}: got {got}, ref {ref}, rel error {err_rel:.3e}")
    assert err_rel > 100 * F32_EPS, (
        f"float32 error {err_rel:.3e} is at machine precision; "
        f"cancellation not demonstrated (ref={ref}, got={got})")


def test_float32_near_axis_catastrophic_failure():
    # Below this angle the float32 solve yields b^2 - c < 0 and the ray
    # is reported as a complete miss, while float64 still hits.
    # Known-bug documentation: expected to fail once the source is fixed.
    theta = 1e-3
    ref = float(intersect_near_axis(make_paraboloid(np.float64), theta, np.float64).distance[0])
    assert np.isfinite(ref) and ref > 0
    inter = intersect_near_axis(make_paraboloid(np.float32), theta, np.float32)
    logger.log(logging.INFO, f"float32 catastrophic failure at theta={theta}: distance={inter.distance[0]}, position={inter.position[0]}")
    assert inter.distance[0] == 0.0, f"expected complete miss, got distance={inter.distance[0]}"
    assert not np.any(np.isfinite(inter.position[0])), (
        f"expected NaN position for missed ray, got {inter.position[0]}")


def test_moderate_angle_sanity():
    theta = np.deg2rad(10.0)
    inter = intersect_near_axis(make_paraboloid(np.float64), theta, np.float64)
    d0 = float(inter.distance[0])
    assert np.isfinite(d0) and d0 > 0
    pos = np.asarray(inter.position[0])
    assert np.all(np.isfinite(pos)), f"position not finite: {pos}"
    assert on_paraboloid(pos), f"point {pos} not on paraboloid"
    # float32 must agree with float64 to working precision here
    got32 = float(intersect_near_axis(make_paraboloid(np.float32), theta, np.float32).distance[0])
    logger.log(logging.INFO, f"float32/float64 comparison at moderate angle: {got32} vs {d0}")
    assert np.isfinite(got32) and got32 > 0
    assert abs(got32 - d0) / d0 < 1e-3, (
        f"float32/float64 disagree at moderate angle: {got32} vs {d0}")
