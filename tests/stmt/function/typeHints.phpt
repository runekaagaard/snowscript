--TEST--
Type hints
--FILE--
<?php
function a(b, array c, callable d, E f) {}
--EXPECT--
<?php
function a($b, array $c, callable $d, E $f) {}