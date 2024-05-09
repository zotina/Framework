@echo off
for /F "tokens=*" %%A in (config.conf) do set "%%A"

set "destinationFolder=%root%\bin"
cd "%root%\%sprint%\framework\src"

javac -d "%destinationFolder%" *.java

cd "%root%\bin"
jar -cvf "../jar/framework.jar" .

pause