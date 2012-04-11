--TEST--
For loop
--FILE--
<?php
for (i = 0; i < c; ++i) {}
for (;a,b;) {}
for (;;) {}
for (;;):
endfor;
--EXPECT--
<?php
for ($i = 0; $i < $c; ++$i) {}
for (;$a,$b;) {}
for (;;) {}
for (;;):
endfor;