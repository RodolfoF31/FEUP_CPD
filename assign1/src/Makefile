CC = g++
CFLAGS = -O2
LIBS = -lpapi
OPENMP = -fopenmp
JAVA = javac
PYTHON = python3
JAVA_RUN = java
CPP_SRC = multiplyMatrix.cpp
CPP_SRC_OPENMP = multiplyMatrix_openmp.cpp
JAVA_SRC = multiplyMatrix.java
PYTHON_SRC = multiplyMatrix.py
CPP_EXE = multiplyMatrix
CPP_EXE_OPENMP = multiplyMatrix_openmp
JAVA_CLASS = multiplyMatrix
PYTHON_SCRIPT = multiplyMatrix.py

.PHONY: run-cpp run-parallel run-java run-py clean-ex clean-txt

run-cpp: $(CPP_SRC)
	$(CC) $(CFLAGS) $(CPP_SRC) -o $(CPP_EXE) $(LIBS)
	./$(CPP_EXE)

run-parallel: $(CPP_SRC_OPENMP)
	$(CC) $(CFLAGS) $(CPP_SRC_OPENMP) -o $(CPP_EXE_OPENMP) $(LIBS) $(OPENMP)
	./$(CPP_EXE_OPENMP)

run-java: $(JAVA_SRC)
	$(JAVA) $(JAVA_SRC)
	$(JAVA_RUN) $(JAVA_CLASS)

run-py: $(PYTHON_SRC)
	$(PYTHON) $(PYTHON_SCRIPT)

clean-ex:
	rm -f $(CPP_EXE) *.class *.exe *.o *.out

clean-txt:
	rm -f *.txt

run: run-cpp run-java run-py

clean: clean-ex clean-txt