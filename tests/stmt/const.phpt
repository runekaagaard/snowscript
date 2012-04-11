--TEST--
Global constants
--FILE--
<?php
const A = 0, B = 1.0, C = 'A', D = E;
--EXPECT--
<?php
const A = 0, B = 1.0, C = 'A', D = E;