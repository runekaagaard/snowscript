--TEST--
New expression dereferencing
--FILE--
<?php
(new A)->b;
(new A)->b();
(new A)['b'];
(new A)['b']['c'];
--EXPECT--
<?php
(new A)->b;
(new A)->b();
(new A)['b'];
(new A)['b']['c'];