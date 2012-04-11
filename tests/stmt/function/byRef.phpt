--TEST--
Return and pass by ref
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
function a(&b) {}
function &a(b) {}
', 0);
--EXPECT--
<?php
function a(&$b) {}
function &a($b) {}