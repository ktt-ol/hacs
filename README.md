# Hackspace Access Control System


# Create keystore with cert for the mqtt connection

```sh
echo -n | openssl s_client -connect mainframe.io:8883 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > mqtt_mainframe.io.crt
keytool -import -trustcacerts -keystore hacs_keystore.bks -storepass keystorepw -noprompt -alias mqtt_mainframe -file mqtt_mainframe.io.crt -storetype BKS -providerClass org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath bcprov-ext-jdk14-1.53.jar
```

# ssh keys

Must start with `mf-door.` and end with `.key` and must have a password.

## TODO

* [X] Berechtigungen erfragen
* [X] Launcher icon
* [ ] Make better progressbar/waiting
* [ ] Save passwords more secure, e.g. https://developer.android.com/samples/BasicAndroidKeyStore/index.html
* [X] Scan passwords with QR-Code
* [X] About dialog
    * [X] List libs with their Licenses

# Libs

* https://www.eclipse.org/paho/clients/android/
* https://github.com/googlesamples/easypermissions
* https://github.com/ACRA/acra.git
* http://www.jcraft.com/jsch/

# Helpful!

* http://www.hivemq.com/blog/mqtt-client-library-enyclopedia-paho-android-service


# E-Mail:

```
Den private Key von der E-Mail bitte irgendwo auf das Handy kopieren.

In den Settings eintragen:
* Passwort für deinen Private-Key:
* Passwort für den MQTT Server:
```