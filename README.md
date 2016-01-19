# PROMTIUS

API for message pushing via cloud messaging systems. Currently implementations exist for APNS and GCM (but open to other systems).

## Characteristics

* supports Apple Push Notification Service (APNS)
* supports Google Cloud Messaging (GCM)
* handles the queueing and delivery of push notifications to client applications
* APNS support is implemented with the [java-apns](https://github.com/notnoop/java-apns) library (and thus still uses the Binary Provider API instead of the newer HTTP/2-based API)
* GCM support is implemented through a forked version of the [gcm-server](https://github.com/google/gcm) code

If you've found an error in this project's code, please file an issue:  
https://github.com/appfoundry/Promtius/issues

## License

Promtius is available under the Apache v2.0 license. See the LICENSE file for more info.