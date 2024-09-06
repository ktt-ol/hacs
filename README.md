# Hackspace Access Control System

An Android app for our "keyholder" to view and manage the door states.

It uses an mqtt service to get the current status and sends door commands per ssh.

# Create keystore with cert for the mqtt connection

We need a Android compatible keystore with the endpoint's ssl certificate in it. Download the `bcprov-ext-jdk14-1.53.jar`, e.g. from [here](http://repo2.maven.org/maven2/org/bouncycastle/bcprov-ext-jdk14/1.53/) and run:

```sh
echo -n | openssl s_client -connect mainframe.io:8883 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > mqtt_mainframe.io.crt
keytool -import -trustcacerts -keystore hacs_keystore.bks -storepass keystorepw -noprompt -alias mqtt_mainframe -file mqtt_mainframe.io.crt -storetype BKS -providerClass org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath bcprov-ext-jdk14-1.53.jar
```

Copy the resulting `hacs_keystore.bks` to the `src/main/assets/keystore/` folder.


# ssh keys

Must start with `mf-door.` and end with `.key` and must have a password.

# Download

[<img src="https://f-droid.org/badge/get-it-on.png"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/packages/io.mainframe.hacs/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
      alt="Get it on Google Play"
      height="80">](https://play.google.com/store/apps/details?id=io.mainframe.hacs)
      
## TODO

* [ ] Save passwords more secure, e.g. https://developer.android.com/samples/BasicAndroidKeyStore/index.html

# Libs

* https://github.com/googlesamples/easypermissions
* https://github.com/ACRA/acra.git
* http://www.jcraft.com/jsch/

# Helpful!

* https://developer.android.com/training/scheduling/alarms#java
* https://medium.com/@anugrahasb1997/implementing-server-sent-events-sse-in-android-with-okhttp-eventsource-226dc9b2599d

# Acknowledgements

* The 3 led buttons come from https://openclipart.org/user-cliparts/bnielsen 
