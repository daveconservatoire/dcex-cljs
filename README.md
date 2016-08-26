# Dave Conservatorie Site

## Usage

Install node packages:

```
npm install
```

Running the Javascripts compilation:

```
lein run -m clojure.main script/figwheel.clj
```

Setup the server by copying the file `server.example.edn` to `server.edn` and filling
the required information for database and social networks.

Running the server (remember to start the compilation first):

```
npm run dev
```

## Tests

Running server tests

```
npm test
```

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
