INCLUDEDIRS=-I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux 

all:
	gcc $(INCLUDEDIRS) -c GetPid.c
	gcc -shared -o libGetPid.so GetPid.o

64bit:        
	gcc -m64 -fPIC $(INCLUDEDIRS) -c GetPid.c
	gcc -m64 -fPIC -shared -o libGetPid.so GetPid.o

clean:
	rm -f *.so *.o
