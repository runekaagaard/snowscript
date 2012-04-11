--TEST--
Different name types
--FILE--
<?php
A;
A\B;
\A\B;
namespace\A\B;
--EXPECT--
<?php
A;
A\B;
\A\B;
namespace\A\B;