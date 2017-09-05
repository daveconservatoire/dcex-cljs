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

Make sure Redis is running.

Setup the server by copying the file `server.example.edn` to `server.edn` and filling
the required information for database and social networks.

Caveat: to run in development you must update the index.html file at `resources/public/index.html`.
Change the `<script>` src from `"/site-min/site-min.js"` to `"/site/site.js"`.

Running the server (remember to start the compilation first):

```
npm run dev
```

## Troubleshooting

If there are problems with compilation, do the following steps:

1. Stop the figwheel process
2. Delete the folders: `target` `resources/public/site` and `resources/public/site-min`
3. Start the compilation again

## Tests

Running server tests

```
npm test
```

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
