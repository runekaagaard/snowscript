--TEST--
Error suppression
--FILE--
<?php
@a;
--EXPECT--
<?php
@$a;