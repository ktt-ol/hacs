# Hackspace Access Control System


# Create keystore with cert for the mqtt connection

```sh
echo -n | openssl s_client -connect mainframe.io:8883 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > mqtt_mainframe.io.crt
keytool -import -trustcacerts -keystore hacs_keystore.bks -storepass keystorepw -noprompt -alias mqtt_mainframe -file mqtt_mainframe.io.crt -storetype BKS -providerClass org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath bcprov-ext-jdk14-1.53.jar
```


## TODO

* Error Handling, Rückgabe ssh
* ignore options berücksichtigen
* Launcher icon
* Make better progressbar/waiting

# Libs

* https://www.eclipse.org/paho/clients/android/

# Helpful!

* http://www.hivemq.com/blog/mqtt-client-library-enyclopedia-paho-android-service