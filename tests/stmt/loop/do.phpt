--TEST--
Do loop
--FILE--
<?php
do {
} while (a);
--EXPECT--
<?php
do {
} while ($a);