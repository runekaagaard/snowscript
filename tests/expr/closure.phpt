--TEST--
Closures
--FILE--
<?php
function(a) { a; };
function(a) use(b) {};
function() use(a, &b) {};
function &(a) {};
static function() {};
--EXPECT--
<?php
function($a) { $a; };
function($a) use($b) {};
function() use($a, &$b) {};
function &($a) {};
static function() {};