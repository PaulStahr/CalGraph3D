import numpy as np

def fillWithRadialIndexData(latitudes, longitudes):
    num_faces = latitudes * (longitudes * 2 - 3) // 2
    faces = np.empty((num_faces, 4), dtype=np.int32)

    # First section: center point to outer ring, every 2 steps
    j_vals = np.arange(0, latitudes, 2)
    faces[:len(j_vals), 0] = 0
    faces[:len(j_vals), 1] = (j_vals + 2) % latitudes + 1
    faces[:len(j_vals), 2] = j_vals + 2
    faces[:len(j_vals), 3] = j_vals + 1

    # Vectorized second section
    i_vals = np.arange(longitudes - 2)
    j_vals = np.arange(latitudes)

    # Create 2D grid of shape (longitudes-2, latitudes)
    index0 = i_vals[:, None] * latitudes + 1
    index1 = index0 + latitudes

    j = j_vals[None, :]
    jp1 = (j + 1) % latitudes

    v0 = index0 + j
    v1 = index0 + jp1
    v2 = index1 + jp1
    v3 = index1 + j

    faces[latitudes // 2:] = np.stack([v0, v1, v2, v3], axis=-1).reshape(-1,4)
    return faces


def fillWithCylinderIndexData(latitudes, longitudes, faces=None):
    num_faces = latitudes * (4 * longitudes - 4)

    # i ranges over (longitudes - 1), j over latitudes
    i_vals = np.arange(longitudes - 1)[:, None]  # shape (longitudes - 1, 1)
    j_vals = np.arange(latitudes)[None, :]  # shape (1, latitudes)

    index0 = i_vals * latitudes
    index1 = index0 + latitudes

    jj = (j_vals + 1) % latitudes

    v0 = index0 + j_vals
    v1 = index0 + jj
    v2 = index1 + jj
    v3 = index1 + j_vals

    return np.stack([v0, v1, v2, v3], axis=-1)