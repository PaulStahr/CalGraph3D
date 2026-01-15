import numpy as np
import inspect
from calgraph3d.data.raytrace.OpticalObject import OpticalObject
from calgraph3d.data.raytrace.Intersection import Intersection
from jsymmath.geometry.Geometry import Geometry
from jsymmath.util import ArrayUtil
from jsymmath.geometry.AffineMatrix import AffineMatrix

class MeshObject(OpticalObject):
    def __init__(self):
        super().__init__()
        self.meshToGlobal = np.identity(4)
        self.globalToMesh = np.identity(4)
        self.texture = None
        self.faces = None
        self.faceNormals = None
        self.faceNormalsInversedLength = None
        self.vertexNormals = None
        self.textureCoordinates = None
        self.vertices = None
        self.weightPoint = None
        self.active = True
        self.smoothNormals = False
        self.radiusQ = np.nan

    def getIntersection(self,
                         position:np.ndarray,
                         direction:np.ndarray,
                         intersection:Intersection,
                         lower_bound:np.ndarray,
                         upper_bound:np.ndarray,
                         xp=None):
        if xp is None:
            xp = inspect.getmodule(type(position))
        globalToMesh = ArrayUtil.convert(self.globalToMesh, xp)
        # Transform position and direction into mesh space
        position = position @ globalToMesh[:3, :3].T + globalToMesh[:3, 3]
        direction = direction @ globalToMesh[:3, :3].T

        dir_len = xp.linalg.norm(direction, axis=-1)
        lower_bound = lower_bound * dir_len
        upper_bound = upper_bound * dir_len
        inv_dir_len = 1 / dir_len
        direction *= inv_dir_len[..., np.newaxis]

        # Early bounding sphere check
        weightRelative = position - ArrayUtil.convert(self.weightPoint[np.newaxis, :], xp)
        distance = xp.sum(xp.square(weightRelative), axis=-1) - self.radiusQ
        b_a = xp.sum(weightRelative * direction, axis=-1)
        sqrt_a = xp.square(b_a) - distance
        mask_b2a = xp.nonzero(sqrt_a >= 0)[0]
        if len(mask_b2a) == 0:
            return xp.zeros(shape=(0,), dtype=int)
        sqrt_b = xp.sqrt(sqrt_a[mask_b2a])
        b_b = b_a[mask_b2a]
        mask_c2b = xp.nonzero((sqrt_b - b_b >= lower_bound[mask_b2a]) & (-sqrt_b - b_b <= upper_bound[mask_b2a]))[0]
        if len(mask_c2b) == 0:
            return xp.zeros(shape=[0], dtype=int)
        mask_c2a = mask_b2a[mask_c2b]
        assignment_mask  = xp.zeros(shape=len(position), dtype=bool)
        vertices = ArrayUtil.convert(self.vertices, xp)
        vertexNormals = ArrayUtil.convert(self.vertexNormals, xp)
        faceNormals = ArrayUtil.convert(self.faceNormals, xp)
        textureCoordinates = ArrayUtil.convert(self.textureCoordinates, xp)
        faces = ArrayUtil.convert(self.faces, xp)
        v0_all = vertices[faces[:, 0]]
        v1_rel_v0_all = vertices[faces[:, 1]] - v0_all
        v2_rel_v0_all = vertices[faces[:, 2]] - v0_all
        d00_all = xp.sum(xp.square(v1_rel_v0_all), axis=-1)
        d01_all = xp.sum(v1_rel_v0_all * v2_rel_v0_all, axis=-1)
        d11_all = xp.sum(xp.square(v2_rel_v0_all), axis=-1)
        inv_denom_all = 1 / (d00_all * d11_all - d01_all * d01_all)
        direction_c = direction[mask_c2a]
        position_c = position[mask_c2a]
        lower_bound_c = lower_bound[mask_c2a]

        for i in range(len(self.faces)):
            face_normal = faceNormals[i]
            v0 = v0_all[i]

            pr_c = v0[np.newaxis, ...] - position_c
            dist_c = (pr_c @ face_normal) / (direction_c @ face_normal)

            mask_d2c = xp.nonzero((lower_bound_c <= dist_c) & (dist_c <= upper_bound[mask_c2a]))[0]
            if len(mask_d2c) == 0:
                continue
            mask_d2a = mask_c2a[mask_d2c]
            dist_d = dist_c[mask_d2c]

            location_rel_v0_d = dist_d[...,xp.newaxis] * direction[mask_d2a] - pr_c[mask_d2c]

            v1_rel_v0 = v1_rel_v0_all[i]
            v2_rel_v0 = v2_rel_v0_all[i]
            d00 = d00_all[i]
            d01 = d01_all[i]
            d11 = d11_all[i]
            d20 = xp.sum(location_rel_v0_d * v1_rel_v0, axis=-1)
            d21 = xp.sum(location_rel_v0_d * v2_rel_v0, axis=-1)
            inv_denom = inv_denom_all[i]
            u1_d = (d11 * d20 - d01 * d21) * inv_denom
            u2_d = (d00 * d21 - d01 * d20) * inv_denom

            u0_d = 1 - (u1_d + u2_d)
            mask_e2d = xp.nonzero((u1_d >= 0) & (u2_d >= 0) & (u0_d >= 0))[0]
            if len(mask_e2d) == 0:
                continue
            mask_e2a = mask_d2a[mask_e2d]

            # Hit found
            intersection.position[mask_e2a] = location_rel_v0_d[mask_e2d] + position[mask_e2a]
            u0_e = u0_d[mask_e2d]
            u1_e = u1_d[mask_e2d]
            u2_e = u2_d[mask_e2d]
            vertex_indices = faces[i, :]
            vertex_normals = vertexNormals[vertex_indices]
            if self.smoothNormals:
                intersection.normal[mask_e2a] = (u0_e[:,xp.newaxis] * vertex_normals[0, xp.newaxis, :]
                                             + u1_e[:,xp.newaxis] * vertex_normals[1, xp.newaxis, :]
                                             + u2_e[:,xp.newaxis] * vertex_normals[2, xp.newaxis, :])
            else:
                intersection.normal[mask_e2a] = face_normal[np.newaxis, :]

                if textureCoordinates is not None:
                    intersection.textureCoords[mask_e2a] = (
                            u2_e * textureCoordinates[vertex_indices[2]]
                            + u1_e * textureCoordinates[vertex_indices[1]]
                            + u0_e * textureCoordinates[vertex_indices[0]])


            upper_bound[mask_e2a] = dist_d[mask_e2d]
            intersection.object[mask_e2a] = self.id
            intersection.faceIndex[mask_e2a] = i
            assignment_mask[mask_e2a] = True

        assignment_mask = xp.nonzero(assignment_mask)[0]
        meshToGlobal = ArrayUtil.convert(self.meshToGlobal, xp)
        if len(assignment_mask) > 0:
            intersection.position[assignment_mask] = intersection.position[assignment_mask] @ meshToGlobal[:3, :3].T + meshToGlobal[:3, 3]
            intersection.normal[assignment_mask] = intersection.normal[assignment_mask] @ meshToGlobal[:3, :3].T
            intersection.distance[assignment_mask] = upper_bound[assignment_mask] * inv_dir_len[assignment_mask]
        return assignment_mask

    def loadFile(self, fileName:str):
        import pymeshlab
        ms = pymeshlab.MeshSet()
        ms.load_new_mesh(fileName)
        vertices, faces = ms.current_mesh().vertex_matrix(), ms.current_mesh().face_matrix()
        textureCoordinates = None
        if ms.current_mesh().has_vertex_tex_coord():
            textureCoordinates = ms.current_mesh().texture_coordinate_matrix()

        self.setData(vertices, faces, textureCoordinates)


    def update(self):
        self.weightPoint = np.mean(self.vertices, axis=0)
        self.vertexNormals, self.faceNormals = Geometry.calcTriangleMeshVertexFaceNormals(self.vertices, self.faces)
        self.faceNormalsInversedLength = self.faceNormals / np.sum(np.square(self.faceNormals), axis=-1, keepdims=True)
        self.faceNormals /= np.linalg.norm(self.faceNormals, axis=-1, keepdims=True)
        self.radiusQ = np.sum(np.square(self.weightPoint - self.vertices), axis=-1).max()


    def setData(self, vertices, faces, textureCoordinates):
        self.textureCoordinates = textureCoordinates
        self.vertices = vertices
        self.faces = faces
        self.update()

    def getVertexPositions(self, xp=np):
        return ArrayUtil.convert(self.vertices @ self.meshToGlobal[:3, :3].T + self.meshToGlobal[:3, 3], xp)

    def getMesh(self):
        return self.getVertexPositions(), self.faces