--TEST--
Constant fetches
--FILE--
<?php
A;
A::B;
--EXPECT--
<?php
A;
A::B;