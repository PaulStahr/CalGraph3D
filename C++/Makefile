ifeq ($(origin JAVA_HOME),undefined)
JAVA_HOME:=/usr/lib/jvm/java-11-openjdk-amd64
endif
armadillo_java.so: armadillo.cpp util.cpp
	g++ -shared -fPIC  -Wall -Wextra -pedantic -g -O2 -fopenmp -std=c++14 -I"${JAVA_HOME}/include" -I"${JAVA_HOME}/include/linux" armadillo.cpp util.cpp -larmadillo -o armadillo_java.so
