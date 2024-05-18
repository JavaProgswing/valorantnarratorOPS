@echo off

REM Check if input file, artifact ID, and main class path arguments are provided
if "%1"=="" (
    echo Usage: %0 ^<output_file^>
    exit /b 1
)

REM Input file, artifact ID, and main class path
set output_file=%1

REM Overwrite the content in the input file
(
    echo set args = WScript.Arguments
    echo num = args.Count
    echo if num = 0 then
    echo     WScript.Quit 1
    echo end if
    echo sargs = ""
    echo if num ^> 1 then
    echo     sargs = " "
    echo     for k = 1 to num - 1
    echo         anArg = args.Item^(k^)
    echo         sargs = sargs ^& anArg ^& " "
    echo     next
    echo end if
    echo Set WshShell = WScript.CreateObject^("WScript.Shell"^)
    echo WshShell.Run """" ^& WScript.Arguments^(0^) ^& """" ^& sargs, 0, False
) > "%output_file%"