# BCD-01 — Workspace Overview

This workspace contains sample CORBA examples, a small banking Maven project, a local Payara Server distribution, and a practical Java webapp used for teaching Business Component Development.

**Top-level projects**

- `Corba/` — CORBA IDL and sample Java client/server stubs and implementations
- `corba-jiat-bank/` — Maven-based banking-related project (IDL + Java sources)
- `payara-6.2025.11/` — Payara Server distribution for local deployment
- `prac_01/` — Practical Java web application (Maven + JSP)

Below are expandable (folded) summaries for each project with key files and usage notes.

<details>
<summary>Corba/ (CORBA examples)</summary>

Key contents:

```
Corba/
├─ TestApp.idl
├─ example_1/
│  ├─ HelloClient.java
│  ├─ HelloServer.java
│  ├─ HelloWorld.idl
│  └─ HelloWorldImpl.java
└─ TestApp/
	├─ _TestStub.java
	└─ TestPOA.java
```

About: A set of IDL and generated Java bindings demonstrating CORBA client/server patterns. Use an appropriate ORB and generated stubs to compile and run the examples.

Run instructions (tnameserv + server + client):

```
REM Start the CORBA naming service on port 1050 (foreground)
"%JAVA_HOME%\bin\tnameserv" -ORBInitialPort 1050

REM Or start in a new window on Windows
start cmd /k "%JAVA_HOME%\bin\tnameserv" -ORBInitialPort 1050

REM Compile generated stubs and example sources (if needed)
javac -classpath . path\to\generated\stubs\*.java example\HelloServer.java example\HelloClient.java

REM Run the server
java -classpath . HelloServer -ORBInitialPort 1050 -ORBInitialHost localhost

REM Run the client (in a separate terminal)
java -classpath . HelloClient -ORBInitialPort 1050 -ORBInitialHost localhost
```

Notes:

- Use the flag `-ORBInitialPort` (not `-ORBInitialProt`).
- Ensure generated stub classes from IDL are on the classpath.
- Replace `HelloServer` / `HelloClient` with your actual class names or use `-jar` for runnable JARs.

</details>

<details>
<summary>corba-jiat-bank/ (Maven project)</summary>

Key contents:

```
corba-jiat-bank/
├─ pom.xml
└─ src/main/java/
	└─ Banking.idl
```

About: A Maven project containing banking-related IDL and Java sources. Open with IntelliJ or run with Maven (`mvn clean package`). Confirm the `pom.xml` for build and dependency details.

</details>

<details>
<summary>payara-6.2025.11/ (Payara Server)</summary>

Key contents (excerpt):

```
payara-6.2025.11/
├─ payara6/
│  ├─ bin/
│  │  ├─ asadmin.bat
│  │  └─ startserv.bat
│  └─ glassfish/
└─ h2db/
```

About: A local Payara Server distribution included for deploying webapps and testing Java EE / Jakarta EE components. Start the server with `payara6/bin/asadmin.bat` or use the `startserv.bat` scripts inside `glassfish/bin`.

</details>

<details>
<summary>prac_01/ (Practical webapp)</summary>

Key contents:

```
prac_01/
├─ pom.xml
├─ src/main/webapp/
│  ├─ profile.jsp
│  ├─ signin.jsp
│  └─ signup.jsp
└─ target/ (build output)
```

About: A Maven web application with JSP pages used in coursework. Build with `mvn package` and deploy the generated WAR to the included Payara Server for testing.

</details>

**Quick start**

1. Install JDK 8 (required for running the CORBA examples) and Maven. Other projects in this workspace can use Java 11+.
   - Download a JDK 8 distribution (Adoptium/Temurin 8, Azul Zulu 8, or Oracle JDK 8) and run the Windows installer.
   - Set `JAVA_HOME` and add `bin` to `PATH` (temporary in current CMD):

   ```bat
   set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_xxx
   set PATH=%JAVA_HOME%\bin;%PATH%
   ```

   - Verify:

   ```bat
   java -version
   javac -version
   ```

2. Open the workspace in IntelliJ or your IDE of choice.
3. For `prac_01` and `corba-jiat-bank`: run `mvn clean package` in each project directory.
4. Start Payara to deploy `prac_01` (use `payara6/bin/asadmin.bat` or `glassfish/bin/startserv.bat`).

**Notes**

- Check each project's `pom.xml` and `README` (if present) for project-specific instructions.
- The CORBA examples may require a compatible ORB and generated stubs; consult the source files in `Corba/`.

If you want, I can also add per-project README files, run builds, or create short run scripts. Let me know which next step you prefer.
