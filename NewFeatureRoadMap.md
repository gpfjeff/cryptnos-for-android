# New Feature Road Map #

This page documents a few new features and enhancements I'm considering adding in future releases. Whether or not they get implemented depends on (a) user feedback (i.e. whether the feature is user requested or commented upon), (b) level of difficulty (some more complicated tasks may require more time to implement), and (c) time (I wear many hats, so development may take longer to implement as my other duties shift and change).

## Help menu on each activity ##
**Status:** Implemented, ready for next release (1.1.1 or 1.2)

**Level of Difficulty:** Low

**Implementation Time:** Minor; mostly write once then repetitive copy-paste-tweak

**Details:** In addition to a main menu item that takes the user to the help index, add a menu item on each activity that takes the user to the relevant help screen for that activity (e.g. selecting Menu -> Help on the Export activity will take you to the Export/Import help). This should be activated by the Menu hard button.

## "Remove Confusing Characters" option in password generation ##
**Status:** Considering, not started

**Level of Difficulty:** Moderate

**Implementation Time:** Significant; requires UI, database, business logic, and import/export format changes

**Details:** After a good deal of thought, I'm considering adding a new password generation option to remove "confusing" characters from the generated password. For example, the number one is easily confused with both the lowercase L and capital I characters; removing all three would make retyping, memorization, and verbal conveyance of the password easier and less confusing. To keep the same level of entropy, these "confusing characters" should be replaced with symbols, which requires identifying symbols that are not commonly used as delimiters, wild cards, and other uses that might prove problematic. The current idea would be to implement this as a new checkbox/toggle separate from other options so it may be combined with other parameters. The default would have this setting turned off, which implements the current behavior.

This won't be an easy one, which is why I may not implement it. Adding a new parameter means changes to the business logic (SiteParameters class), database (adding a new column that defaults to null/false), UI (a new checkbox/toggle button), and import/export file format (XML schema changes with the corresponding element being optional to preserve old exports). This also requires the Windows client to be upgraded with the same feature to make sure they are in sync. It's possible that other parameters may clobber the benefits of this setting; for example, changing the character class to "Alphanumerics only" eliminates the substituted symbols, effectively reducing the characters per slot from 62 to 56 (assuming six "confusing" characters were substituted). So this feature may or may not get implemented depending on whether or not users feel it is a worthwhile addition.

## Encrypt database with random key from [Random.org](http://www.random.org/) ##
**Status:** Considering, not started

**Level of Difficulty:** Moderate

**Implementation Time:** Significant; requires UI, database, and business logic changes

**Details:** Currently, the database stores parameter data encrypted using AES-256 with a key that includes a salt derived from `android.provider.Settings.System.ANDROID_ID`, a value that should be unique per device. Unfortunately, this value is only unique if the user has visited the Android Market. If the user has never been to the Market, this value is null and the salt value is based solely on a hard-coded salt in the code which will not be unique per install. Additionally, `Settings.System.ANDROID_ID` is actually deprecated now in favor of `Settings.Secure.ANDROID_ID`, which cannot be modified by code. (I intentionally used the old value to target Android 1.1+ to get the widest possible install base; implementing a helper class that dynamically picks the right one for the device's API should be another item on this list.)

While I still think it was a good idea to encrypt the database with a unique device-specific key, it may not be the best long term solution. While the number of users who bypass the Market may be few, it likely won't be insignificant. I've been toying with the idea of adding a small HTTP client class that fetches a string of truly random bytes from [Random.org](http://www.random.org/) and using that (or more likely that value hashed and salted multiple times) as the encryption key. This would offer a bit more security in that the encryption key is not based on `Settings.System.ANDROID_ID` and should be available even if the user has never visited the Market.

Known problems with this idea:
  * Obviously, the HTTP client must be implemented, including for good measure code to check the bit quota to prevent abuse. (I've already done most of this as a coding exercise anyway.)
  * The random bytes, once fetched, must be stored somewhere, ideally not in the database (as it should not be in the same place as the encrypted data). I'm not currently using the built-in preferences API, so that might be an option.
  * A key derivation scheme must be chosen or designed so we do not use the random bytes directly (i.e. acquiring these bytes will not directly compromise the encrypted data).
  * New UI elements must be added to ask the user's preference on this option; we don't want to do this without letting them know we're doing it, even if it's in their best interests.
  * This option must be backward compatible in that if the user chooses not to use a truly random key it should default to the current implementation.
  * If we're going this far, it makes sense to make this option available on-demand. Current users will need to re-key old `Settings.System.ANDROID_ID` keyed data anyway, so we might as well allow the user to re-key with a new random key any time they want.
  * And, of course, adding any kind of network access requires the `android.permission.INTERNET` permission to be added to the manifest. Comments in the Market seem to indicate that users _like_ the fact that Cryptnos doesn't access the Internet, so this addition could backfire. We could always get around this by using the built-in pseudo-random number generators provided by the operating system, but that defeats the benefits of using a _truly_ random key.

One way to get around the negative of adding extra and confusing permissions: pull this code out into a separate app dedicated to do this task and make that an optional download. If the user wants that functionality, we'll point them to the Market and let them download it, then enable the feature in Cryptnos. Cryptnos can then pass a request for so many random bytes to the new app via an Intent and get the bits back in return. If the user decides not to take this route, we can disable the feature within Cryptnos.

## Generate pseudo-random database encryption key from hardware sources ##
**Status:** Considering, not started

**Level of Difficulty:** Moderate

**Implementation Time:** Significant; requires UI, database, and business logic changes

**Details:** This is somewhat related to the Random.org idea, but could easily be used in place of it. Instead of or in addition to getting a random database encryption key from Random.org, we could let the user generate one by using environmental data derived from one of several hardware sources. Data could be collected from any number of hardware sensors--the touch screen, microphone, accelerometer, camera, GPS, etc.--and temporarily stored. Once collection is complete, the data will need to be munged, likely using cryptographic hashes already present, to create the key seed. Then, like the Random.org idea, this key seed would be stored and the actual encryption key derived from it each time the application is started.

This avoids the problem of requiring network access like Random.org, but introduces a new problem: not all Android devices have every hardware feature. That's why this should be presented as an option rather than a requirement. We'll need to use the built-in Android mechanisms of ensuring each hardware feature is present (the constants from `android.content.pm.PackageManager`). Some features may have caveats of their own; for example, the user might wonder why a password manager wants fine GPS location data. Even though the data is used once to generate the key and essentially thrown away afterward, the user will still get a warning that the feature is requested at install time.

Possible hardware entropy sources may include:
  * Finger gestures: Requires `PackageManager.FEATURE_TOUCHSCREEN`. Have the user "randomly" move their finger over the screen. For those familiar with [PuTTY](http://www.chiark.greenend.org.uk/~sgtatham/putty/) or [TrueCrypt](http://www.truecrypt.org/), this is similar to tactics those applications use for key generation. In addition to pseudo-random data collected from the system, both of these applications ask the user to move the mouse in an erratic fashion over a particular UI control. The UI captures the mouse movements and stirs that information into an entropy pool. Once the entropy pool is sufficiently seeded, random data is extracted and used as the key. For this idea, we would present the user with a UI and ask them to move their finger erratically over the designated area. A progress bar should indicate how much entropy is collected to date. The user does not need to repeat this process unless they wish to regenerate a new encryption key, so they can be as erratic as they want.
  * Microphone: Requires `PackageManager.FEATURE_MICROPHONE`. Capture ambient sound data and hash that to get the key seed. Ask the user go somewhere with a lot of random-sounding background noise, or to say something or make random noises. This option may not be ideal as sound recognition could be used against a recording of same sounds, but perhaps once hashed the data might be sufficient to use as a seed. A specific recording length should be decided upon and a progress bar displayed.
  * Accelerometer: Requires `PackageManager.FEATURE_SENSOR_ACCELEROMETER`. There are plenty of "shake to generate passwords" apps in the Market. This is the same principle; have the user shake the phone to generate entropy. Like the above, a progress bar should indicate entropy gathering progress.
  * Camera: Requires `PackageManager.FEATURE_CAMERA`. We don't need auto-focus or a flash here; we just need to capture an image and hash it. The user should be encouraged to point the camera at something with a lot of "activity", i.e. something with a lot of non-regular, noise-like patterns, like a forest. Once the image is captured, hash it to get the seed and discard the image. This one could be hard for users to accept, however, as the camera is often viewed as a very private thing and it would seem odd for a password manager to request access to it.
  * GPS or other location data: Requires `PackageManager.FEATURE_LOCATION` at a minimum, `PackageManager.FEATURE_LOCATION_GPS` for GPS and `PackageManager.FEATURE_LOCATION_NETWORK` for network-based location. I like this option the least because (a) location data is far from random and may be predictable and (b) users are a lot less likely to give their location away for password generation, even if that information is never stored and never leaves the device. I throw this in more for brainstorming than anything else, for completeness to show what kind of data we could use.

The same "optional second app" idea mentioned above could apply here. Roll all of these features into the new app and enable the "get random bits by intent" feature if it's installed.