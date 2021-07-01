ifeq ($(origin JAVA_HOME),undefined)
ifneq ($(wildcard /usr/lib/jvm/java-11-openjdk-amd64/include/.*),)
JAVA_HOME:=/usr/lib/jvm/java-11-openjdk-amd64/include/
else
JAVA_HOME:=/usr/lib/jvm/java-14-openjdk-amd64/include/
endif
endif
armadillo_java.so: armadillo.cpp util.cpp
	g++ -shared -fPIC  -Wall -Wextra -pedantic -g -O2 -fopenmp -std=c++14 -I"${JAVA_HOME}/include" -I"${JAVA_HOME}/include/linux" armadillo.cpp util.cpp -larmadillo -o armadillo_java.so
