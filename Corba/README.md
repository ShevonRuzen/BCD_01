# CORBA Examples — Run Instructions

This folder contains CORBA IDL, generated stubs, and example Java client/server code.

Prerequisite: JDK 8 is required for these examples. Other projects in the workspace can use Java 11+.

## Install JDK 8 (Windows)

1. Download a JDK 8 distribution (Adoptium/Temurin 8, Azul Zulu 8, or Oracle JDK 8) and run the installer.
2. Set `JAVA_HOME` and add `bin` to `PATH` (temporary in current CMD):

```bat
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_xxx
set PATH=%JAVA_HOME%\bin;%PATH%
```

3. Verify:

```bat
java -version
javac -version
```

## Start the CORBA naming service (tnameserv)

Run in a terminal (foreground):

```bat
"%JAVA_HOME%\bin\tnameserv" -ORBInitialPort 1050
```

To run in a new Windows terminal window:

```bat
start cmd /k "%JAVA_HOME%\bin\tnameserv" -ORBInitialPort 1050
```

## Compile (if needed)

Make sure generated stub classes are on the classpath and compile any example sources:

```bat
javac -classpath . path\to\generated\stubs\*.java example\HelloServer.java example\HelloClient.java
```

## Run server and client

Run the server in a terminal (ensure `tnameserv` is running):

```bat
java -cp C:\path\to\classes;C:\path\to\stubs com.example.HelloServer -ORBInitialPort 1050 -ORBInitialHost localhost
```

Run the client in a separate terminal:

```bat
java -cp C:\path\to\classes;C:\path\to\stubs com.example.HelloClient -ORBInitialPort 1050 -ORBInitialHost localhost
```

To run a runnable JAR:

```bat
java -jar C:\path\to\myapp.jar -ORBInitialPort 1050 -ORBInitialHost localhost
```

Notes:

- The correct flag is `-ORBInitialPort` (not `-ORBInitialProt`).
- Replace class names and paths with your actual package-qualified class names and directories.
- Ensure generated stub classes from IDL are present and included on the classpath.

If you want, I can also add a small `run.bat` in this folder to start `tnameserv` and launch examples.
