import pathlib
from setuptools import find_packages, setup
# The directory containing this file
HERE = pathlib.Path(__file__).parent

# The text of the README file
README = (HERE/ "README.md").read_text()

# This call to setup() does all the work
setup(
    name="calgraph3d",
    version="2.1.0",
    description="Program to display 3-dimensional functions and perform raytracing",
    long_description=README,
    long_description_content_type="text/markdown",
    url="https://github.com/PaulStahr/CalGraph3D",
    author="Paul Stahr",
    author_email="paul.stahr@posteo.de",
    license="MIT",
    classifiers=[
        "License :: OSI Approved :: BSD License",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.6",
        "Programming Language :: Python :: 3.7",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
    ],
    packages=['calgraph3d'],
    include_package_data=False,
    install_requires=["numpy"],
)
