# technobabble

[![CircleCI](https://circleci.com/gh/Doesntmeananything/technobabble.svg?style=svg)](https://circleci.com/gh/Doesntmeananything/technobabble)

## Live version 

[You can find a live version here](https://technobabble-app.herokuapp.com/). It's hosted on Heroku and runs on free dynos, so you may need to wait a while until it wakes up.

## Installation

### Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

You'll also need a PostgreSQL 9.4.4 database running. The default configuration assumes it's on localhost.

### Using basic configuration

The repository provides `dev-config.default.edn` and `test-config.default.edn` in the `resources` folder. Copy them to the root and rename them to `dev-config.edn` and `test-config.edn` if you want to use the default settings.

### Creating the test and dev databases

You can use the provided script to set up the database:

```shell
psql -d postgres < db-setup.sql
```

This will create the test and development databases, add the necessary extensions, and create a test user.

Then run the migrations on both dev and test with:

```shell
lein run migrate
lein with-profile test run migrate
```

### Deploying live

Technobabble uses token authentication. The tokens are signed using a private key, which is expected to be configured in the environment. 

The repository includes a private/public key pair for development purposes. When deploying your own instance, you'll need to replace `prod_auth_pub_key.pem` with your own public key. Then you can provide your private key and passphrase as environment variables in your hosting setup:

```
AUTH_CONF__PASSPHRASE=*your passphrase*
AUTH_CONF__PRIVKEY=*your private key*
```

## Running

To start a web server for the application on port `3000`, run:

    lein run

Compile clojurescript using:

    lein cljsbuild auto

You can also run figwheel with:

    lein figwheel

## Testing

Running `lein test` will run the tests against the `technobabble_test` database on the local host.  
