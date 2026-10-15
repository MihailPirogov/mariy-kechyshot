param([string]$Modes = "light,dark,tablet")
$a='C:\sdk1\platform-tools\adb.exe'; $p='D:\android\mari-kechyshot'; $pkg='ru.pirogovlab.mariykechyshot.shots'; $act="$pkg/ru.mari.kechyshot.MainActivity"
& $a shell settings put global sysui_demo_allowed 1
function demo { & $a shell am broadcast -a com.android.systemui.demo -e command @args | Out-Null }
demo enter; demo clock -e hhmm 1000; demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4 -e mobile show -e level 4 -e datatype none; demo notifications -e visible false
function cap($dir,$n){ Start-Sleep 7; & $a shell screencap -p /sdcard/x.png; New-Item -ItemType Directory -Force $dir | Out-Null; & $a pull /sdcard/x.png "$dir\$n.png" | Out-Null }
function st { & $a shell am start -S -n $act --es date 2026-07-14 @args | Out-Null }
function run($dir){
 st --ei tab 0; cap $dir '01_tache'
 st --ei tab 1; cap $dir '02_tylze'
 st --es shot_open hol-501c4c5b65; cap $dir '03_holiday'
 st --es shot_open rite-ffcd365a31; cap $dir '04_rite'
 st --ei tab 2; cap $dir '05_kychalash'
 st --ei tab 3; cap $dir '06_ushestarymash'
 st --es shot_share hol-501c4c5b65; cap $dir '07_share'
 st --ez shot_notify true; Start-Sleep 3; & $a shell cmd statusbar expand-notifications; cap $dir '08_notification'; & $a shell cmd statusbar collapse
}
$s="$p\store\screenshots"
& $a shell wm size reset; & $a shell wm density reset; st --ei tab 0; Start-Sleep 10
foreach ($m in $Modes.Split(',')) {
 if ($m -eq 'dark') { & $a shell cmd uimode night yes } else { & $a shell cmd uimode night no }
 if ($m -eq 'tablet') { & $a shell wm size 1600x2560; & $a shell wm density 320; Start-Sleep 3 }
 run "$s\$m"
 if ($m -eq 'tablet') { & $a shell wm size reset; & $a shell wm density reset }
}
Compress-Archive "$s\*" "$env:TEMP\store_raw.zip" -Force; "ok"
