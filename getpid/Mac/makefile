INCLUDEDIRS=-I${JAVA_HOME}/include
ARCH += -arch ppc -arch i386

all:
	gcc $(INCLUDEDIRS) $(ARCH) -c GetPid.c
	gcc -dynamiclib $(ARCH) -o libGetPid.jnilib GetPid.o

clean:
	rm -f *.jnilib *.o
