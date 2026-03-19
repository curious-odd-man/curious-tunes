Set oWS = WScript.CreateObject("WScript.Shell")
sLinkFile = "${desktopDir}\\cTunes.lnk"
Set oLink = oWS.CreateShortcut(sLinkFile)
oLink.TargetPath = "javaw"
oLink.Arguments = "-D" & Chr(34) & "spring.profiles.active=prod" & Chr(34) & " -jar " & Chr(34) & "${deployDir}\\\\${jarName}" & Chr(34)
oLink.WorkingDirectory = "${deployDir}"
oLink.IconLocation = "${deployDir}\\cTunes.ico, 0"   ' <-- add this line
oLink.Description = "Launch cTunes"
oLink.Save