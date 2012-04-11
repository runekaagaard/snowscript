--TEST--
While loop
--FILE--
<?php
while (a) {}
while (a):
endwhile;
--EXPECT--
<?php
while ($a) {}
while ($a):
endwhile;