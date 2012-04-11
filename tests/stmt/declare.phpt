--TEST--
Declare
--FILE--
<?php
declare (A='B', C='D') {}
declare (A='B', C='D'):
enddeclare;
--EXPECT--
<?php
declare (A='B', C='D') {}
declare (A='B', C='D'):
enddeclare;