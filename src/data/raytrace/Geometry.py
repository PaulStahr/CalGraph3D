import numpy as np

class Geometry:
    @staticmethod
    def getOrthorgonalZMatrix(in_vec):
        dirlength = np.linalg.norm(in_vec)
        qx,qy,qz = np.square(in_vec)

        lqx = qy + qz
        lqy = qx + qz
        lqz = qx + qy

        if lqx >= lqy and lqx >= lqz:
            n0 = np.asarray((0, -in_vec[2], in_vec[1]))
        elif lqy >= lqx and lqy >= lqz:
            n0 = np.asarray((-in_vec[2], 0, in_vec[0]))
        else:
            n0 = np.asarray((-in_vec[1], in_vec[0], 0))

        n0 *= dirlength / np.linalg.norm(n0)
        n1 = np.cross(in_vec, n0)
        n1 *= dirlength / np.linalg.norm(n1)
        return np.asarray((n0,n1,in_vec))