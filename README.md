himilsbach
==========

A tiny actor library for Clojure.

[![Build Status](https://secure.travis-ci.org/jstepien/himilsbach.png?branch=master)](http://travis-ci.org/jstepien/himilsbach)

Usage
-----

See [`test/himilsbach/test/benchmark.clj`][bm] or run it with

    lein run -m himilsbach.test.benchmark

Himilsbach is available on [Clojars][clojars].

    [org.clojure.jan/himilsbach "0.0.0"]

[bm]: https://github.com/jstepien/himilsbach/blob/master/test/himilsbach/test/benchmark.clj
[clojars]: https://clojars.org/org.clojure.jan/himilsbach

Related work
------------

  - [clojure_actors – actors implemented on top of Clojure agents][ca]
  - [Jobim – an actors library for Clojure][jobim]
  - [Rich Hickey, _Message Passing and Actors_][hickey]

[ca]: https://github.com/bitsai/clojure-actors
[jobim]: https://github.com/antoniogarrote/jobim
[hickey]: http://clojure.org/state#actors

Copyrights
----------

Copyright (c) 2012–2013 Jan Stępień

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
