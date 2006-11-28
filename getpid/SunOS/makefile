INCLUDEDIRS=-I${JAVA_HOME}/include -I${JAVA_HOME}/include/solaris

all:
	cc $(INCLUDEDIRS) -c GetPid.c
	cc -G -o libGetPid.so GetPid.o

clean:
	rm -f *.so *.o
