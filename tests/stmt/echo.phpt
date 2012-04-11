--TEST--
Echo
--FILE--
<?php
echo 'Hallo World!';
echo 'Hallo', ' ', 'World', '!';
--EXPECT--
<?php
echo 'Hallo World!';
echo 'Hallo', ' ', 'World', '!';