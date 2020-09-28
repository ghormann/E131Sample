CXX = g++
CXXFLAGS = -std=c++0x -Wall

OBJECTS = main.o DDPOutput.o

test: $(OBJECTS)
	$(CXX) $(CXXFLAGS) -o $@ $^

$(OBJECTS): DDPOutput.h main.cpp DDPOutput.cpp

clean:
	rm test *.o