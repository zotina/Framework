@echo off
for /F "tokens=*" %%A in (config.conf) do set "%%A"

set "destinationFolder=%root%\bin"
cd "%root%\src"

javac -d "%destinationFolder%" -cp "C:\Users\user\Desktop\S5\Mr Naina\Framework\local\lib\*" *.java


cd "%root%\bin"
jar -cvf "../jar/framework.jar" .

pause