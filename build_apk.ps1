$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:GRADLE_USER_HOME = 'E:\.gradle'
$env:TEMP = 'E:\scratch\temp'
$env:TMP = 'E:\scratch\temp'
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH

Write-Host "Building VIRTUAL APK..."
Set-Location 'C:\Users\surface\OneDrive\Desktop\VIRTUAL\android'
& 'E:\Gradle\gradle-8.7\bin\gradle.bat' assembleDebug
