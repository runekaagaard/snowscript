--TEST--
Closures
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
function(a) { a }
function(a) use(b) {}
function() use(a, &b) {}
function &(a) {}
static function() {}
', 0);
--EXPECT--
<?php
function($a) { $a; };
function($a) use($b) {};
function() use($a, &$b) {};
function &($a) {};
static function() {};