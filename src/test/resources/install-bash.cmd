@ECHO ON

ECHO %PATH%

::CD .\target\tmp
::curl -L -o .\PortableGit.7z.exe https://github.com/git-for-windows/git/releases/download/v2.37.3.windows.1/PortableGit-2.37.3-64-bit.7z.exe
::.\PortableGit.7z.exe -y

::%cd%\PortableGit\bin\bash.exe --version

bash.exe --version
