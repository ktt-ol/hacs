# Hackspace Access Control System


# Create keystore with cert for the mqtt connection

```sh
echo -n | openssl s_client -connect mainframe.io:8883 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > mqtt_mainframe.io.crt
keytool -import -trustcacerts -keystore hacs_keystore.bks -storepass keystorepw -noprompt -alias mqtt_mainframe -file mqtt_mainframe.io.crt -storetype BKS -providerClass org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath bcprov-ext-jdk14-1.53.jar
```

# ssh keys

Must start with `mf-door.` and end with `.private.key` and must have a password.

## TODO

* Launcher icon
* Make better progressbar/waiting
* Save passwords more secure, e.g. https://developer.android.com/samples/BasicAndroidKeyStore/index.html

# Libs

* https://www.eclipse.org/paho/clients/android/

# Helpful!

* http://www.hivemq.com/blog/mqtt-client-library-enyclopedia-paho-android-service