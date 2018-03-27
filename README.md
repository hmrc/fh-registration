
# fh-registration

[ ![Download](https://api.bintray.com/packages/hmrc/releases/fh-registration/images/download.svg) ](https://bintray.com/hmrc/releases/fh-registration/_latestVersion)


## Summary

This service provides the backend endpoint for the [Fulfilment House Registration Scheme](https://www.gov.uk/guidance/fulfilment-house-due-diligence-scheme) project, allowing a customer to apply for the Fulfilment House Registration Scheme.

## Requirements

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run. It also
requires [MongoDB 3.2](https://www.mongodb.com/).


## List of APIs

| PATH | Supported Methods | Description |
| --------------- | --------------- | --------------- |
| /subscription/:fhddsRegistrationNumber/status | GET | Checks the status of organisation application |
| /subscription/:fhddsRegistrationNumber/get | GET | Get data of organisation's application |
| /subscription/subscribe/:safeId | POST | Send a new application data to DES |
| /subscription/amend/:fhddsRegistrationNumber | POST | Send an amends application data to DES |
| /subscription/withdrawal/:fhddsRegistrationNumber | POST | Send an withdraw application data to DES |

where,

| Parameter | Description | Valid values | Example |
| --------------- | --------------- | --------------- | --------------- |
| fhddsRegistrationNumber | the organisation reference | string | XDFH00000123456 |
| safeId | safeId of the entity etmp  | string | XDFH00000123456 |


and possible responses are:-

| Response code | Message |
| --------------- | --------------- |
| 200 | OK |
| 404 | Not Found |
| 400 | Bad request |
| 503 | Service unavailable |
| 500 | Internal server error |

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

[JRE]: http://www.oracle.com/technetwork/java/javase/overview/index.html