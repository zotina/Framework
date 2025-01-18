@echo off
for /F "tokens=*" %%A in (config.conf) do set "%%A"

set "destinationFolder=%root%\build\framework"
cd "%root%\framework\src"

javac -d "%destinationFolder%" -cp "D:\etude\S5\Mr Naina\Framework\local\lib\*" *.java


cd "%root%\build\framework"
jar -cvf "%root%\lib\framework.jar" .

pause