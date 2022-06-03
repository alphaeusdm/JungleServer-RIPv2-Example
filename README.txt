Alphaeus Dmonte
ad3289

The implementation is designed to work on local machine. Each camera’s ip is created using a general template: “10.0.-.0”, where “-“ is substituted for id of the camera.
The jungle server also works on localhost: 127.0.0.1

To run the program:
- Compile Camera.java and JungleServer.java using javac.
- To run start a camera use :- java Camera <id> <jungle cloud ip> <jungle cloud port>.
