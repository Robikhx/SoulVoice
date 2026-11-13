# Clear jSerialComm cache folders (powershell)
Remove-Item -Recurse -Force "$env:LOCALAPPDATA\Temp\jSerialComm" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "$env:USERPROFILE\.jSerialComm" -ErrorAction SilentlyContinue
Write-Host "jSerialComm cache cleared."