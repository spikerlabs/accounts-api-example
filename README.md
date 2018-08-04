[![CircleCI](https://circleci.com/gh/spikerlabs/accounts-api-example/tree/master.svg?style=shield)](https://circleci.com/gh/spikerlabs/accounts-api-example/tree/master)
[![codecov](https://codecov.io/gh/spikerlabs/accounts-api-exercise/branch/master/graph/badge.svg)](https://codecov.io/gh/spikerlabs/accounts-api-exercise)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/1f3cd587671a419e981f4f17613bff13)](https://www.codacy.com/app/asarturas/payments-api-exercise?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=spikerlabs/payments-api-exercise&amp;utm_campaign=Badge_Grade)

## Accounts Api

Design and implement a RESTful API (including data model and the backing implementation) for money
transfers between accounts.

### Explicit requirements

1. keep it simple and to the point (e.g. no need to implement any authentication, assume the APi is invoked by another internal system/service)
2. use whatever frameworks/libraries you like (except Spring, sorry!) but don't forget about the requirement #1
3. the datastore should run in-memory for the sake of this test
4. the final result should be executable as a standalone program (should not require a pre-installed container/server)
5. demonstrate with tests that the API works as expected

## Usage guide

1. Go to list of releases and download the latest build
2. Run the api with single java command like
```
java -jar accounts-api-assembly-0.2.jar
```

## Developer's guide

Requires up to date Scala and SBT on the machine

Run tests with
```
% sbt test

# at the moment the build e2e verification is not run locally - only in ci
```

Build fat jar with
```
% sbt assembly

# resulting jar will be at the path like ./target/scala-2.12/accounts-api-assembly-0.2.jar
```

Run api in dev mode with
```
% sbt run
```

## Implementation

### Request Flow

StreamApp -> Blaze Server -> Http4s Service -> accounts Service -> Http4s Service -> Blaze Server.

Where only accounts Service are talking to Storage and working with Account aggregates.

It is a streaming app, where results are evaludated only at the edge of the system.

Storage assumed to be an event store, storing transactions only.

### Api Endpoints and examples

#### Transfer between accounts

```
% curl http://localhost:8080/transfer --data '{"source":{"id":"d19a413b-def9-44a3-b9eb-e357a3225795"},"destination":{"id":"bd7857ac-8730-4165-b89c-cb37b1f3faa4"},"funds":{"amount":10}}' --verbose
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> POST /transfer HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.54.0
> Accept: */*
> Content-Length: 138
> Content-Type: application/x-www-form-urlencoded
>
* upload completely sent off: 138 out of 138 bytes
< HTTP/1.1 200 OK
< Date: Tue, 20 Mar 2018 00:30:49 GMT
< Content-Length: 0
<
```

#### Create an account

```
% curl http://localhost:8080/create -XPOST --verbose
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> POST /create HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.54.0
> Accept: */*
>
< HTTP/1.1 200 OK
< Content-Type: application/json
< Date: Tue, 20 Mar 2018 00:28:27 GMT
< Content-Length: 80
<
* Connection #0 to host localhost left intact
{"account":{"id":"d19a413b-def9-44a3-b9eb-e357a3225795"},"balance":{"amount":0}}%
```

#### Deposit funds to an account

```
% curl http://localhost:8080/deposit --data '{"account": {"id": "d19a413b-def9-44a3-b9eb-e357a3225795"}, "funds": {"amount": 30}}' --verbose
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> POST /deposit HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.54.0
> Accept: */*
> Content-Length: 84
> Content-Type: application/x-www-form-urlencoded
>
* upload completely sent off: 84 out of 84 bytes
< HTTP/1.1 200 OK
< Content-Type: application/json
< Date: Tue, 20 Mar 2018 00:29:48 GMT
< Content-Length: 81
<
* Connection #0 to host localhost left intact
{"account":{"id":"d19a413b-def9-44a3-b9eb-e357a3225795"},"balance":{"amount":30}}%
```

#### Health check

```
% curl http://localhost:8080/healthcheck --verbose
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> GET /healthcheck HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.54.0
> Accept: */*
>
< HTTP/1.1 200 OK
< Date: Tue, 20 Mar 2018 00:34:42 GMT
< Content-Length: 0
<
```

### Some of notable limitations of current implementation

* It does not allow to configure the server host or port
* It does have production mode with a persistent database
* There is limited validation (for example, it's still possible to deposit negative amount)
* The transfer transaction is currently split into two (limitation of previous version, which did not have transaction ids, but could be fixed in current one)
* The storage does not check for concurrency issues and happily store the order with inconsisten transactions
