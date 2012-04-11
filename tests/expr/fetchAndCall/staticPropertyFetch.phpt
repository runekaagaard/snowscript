--TEST--
Static property fetches
--FILE--
<?php
A::b;
A::b;
A::{'b'};
A::b['c'];
A::b{'c'};

--EXPECT--
<?php
A::$b;
A::$$b;
A::${'b'};
A::$b['c'];
A::$b{'c'};
