# Changelog

# 2.24.0
* Key change of main area backdoor.
* Update `targetSdkVersion` to 35.

# 2.23.2
* Internal version bump to overwrite Playstore version.

# 2.23.0
* Support for main area backdoor.

# 2.22.0
* Support for woodworking backdoor.

# 2.21.0
* New trash calendar 2025.

# 2.20.1
* Fixes Cashbox update.

# 2.20.0
* Removes EasyPermissions dependency. The app should work with less permissions now.

# 2.19.0
* Sets the correct wifi ids for wood working.

## 2.18.2
* Fixes Mainframe wifi detection in Cashbox view.

## 2.18.1
* Internal refactorings.
* Removes trash notifications (but the trash reminder still exists)

## 2.17.0
* Adds Woodworking status.

## 2.16.0
* Supports Backdoor status and machining keyholder again. 

## 2.15.0
* Removes the mqtt implementation and replaces it with http server send events to our Status page. Not all previous features are supported anymore.

## 2.14.0
* Requests permission SCHEDULE_EXACT_ALARM to fix crashes on Android 12+ 

## 2.13.3
* Fixes some possible NPE.

## 2.13.2
* Android permission fixes.

## 2.13.1
* Rollback of the ssh lib to hopefully fix a regression

## 2.13.0
* Trash calendar update (2024)
* Fix for CVE-2023-48795 (SSH)

## 2.12.0
* New places, new places colors

## 2.11.0
* Refactoring of the Cashbox part. Fixes session handling.
* Adds "Holz" as location.

## 2.10.2
* Reduces minSdkVersion to 26 (Android 8.0)

## 2.10.1
* Fixes cashbox crash

## 2.10.0
* Updates cashbox api

## 2.9.9
* Fixes Trash calendar for Android 12

## 2.9.8
* Trash calendar date parse fix

## 2.9.7
* Bugfix for Android 12

## 2.9.6
* Removes any filename restrictions for selected the private key

## 2.9.5
* Trash calendar update (2023)
* Fix machining 5GHz BSSID

## 2.9.4
* Fix machining 5GHz BSSID

## 2.9.3
* SSID list update

## 2.9.2
* SSID list update
* Trash calendar update (2022)

## 2.9.1
* Changes the cert handling for the cashbox
* Always show all status buttons

## 2.9.0
* Permission refactoring for Android 11. Removed the "scan for your private key" function and uses the internal picker.
* Logging fixed and removed the log to file function. Added an mail and clipboard export instead.
* Fixes SSID detection for newer Android versions.

## 2.8.6
* SSID list update
* Trash calendar update (2021)

## 2.8.5
* Shows all events if there multiple per day

## 2.8.4
* UI fix for wrong private key password

## 2.8.3
* Supports cashbox update

## 2.8.2
* Logging improved (and fixed)
* ssh lib updated
* Cashbox bugfix

## 2.8.1
* Disables Cashbox page if not im Mainframe wifi

## 2.8.0
* Cashbox page

## 2.7.3
* Trash calendar 2020

## 2.7.2
* Enables the trash notification feature as a beta setting.

## 2.7.1
* Fixes a crash for a wrong mqtt password.

## 2.7.0
* Shows on the overview an info text about garbage events. 
* Shows a warning on closing the space if the garbage bin should be moved to the street.  

## 2.6.7
* Increases version in meta files.

## 2.6.6
* Bugfix: Keystore file was missing in the FDroid build.

## 2.6.5
* Bugfix: The notification time should be at 20:03 before the next event. 

## 2.6.4
* Bugfix: Adds several null checks. 

## 2.6.3
* Fixes for fdroid (repo cleanup and lint config)

## 2.6.2
* Changes the logging when the settings change
* Bugfix: NPE in LED status

## 2.6.1
* Crash bugfix.

## 2.6.0
* Shows warnings about the garbage 
* New permission Location to get the ssid on Android 8.1
* Bugfix for the "require Mainframe wifi" option

## 2.5.2
* Bugfix: Fixes "become keyholder" button enable state

## 2.5.1
* Bugfix: Initial state of back door was wrong and leads to a crash.

## 2.5.0
* Shows the status of the back-door
* Asks the user if he really wants to close the space with an open back door
* Disables 'Keyholder' button and all buttons on status if the wifi has a machining bssid

## 2.4.2
* More Bugfixes (fixes broken extended door status)

## 2.4.1
* Bugfixes (#2)

## 2.4.0
* Machining Seite

## 2.3.1
* Bugfixes for door buttons

## 2.3.0
* Adds a new door buzzer button (inner metal door)
* Shows the device locationsstatus

## 2.2.1
* support new device list format

## 2.2.0
* Adds outer door buzzer

## 2.1.0
* Adds back button for fragment changes

## 2.0.0
* Complete redesign of the UI
* Feature: Show current keyholder
* Feature: "Become keyholder"
* mqtt connection handling changed

## 1.4.0
* Door buzzer button added

## 1.3.0
* About dialog added
* Can read passwords from qr-code
* Mqtt lib update to 1.1.0
* New launcher icon

## 1.2.0
* Support for Android >= 6 permission request system

## 1.1.0
* A file browser for private key
