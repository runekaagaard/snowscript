--TEST--
Shell execution
--FILE--
<?php
``;
`test`;
`test A`;
`test \``;
`test \"`;
--EXPECT--
<?php
``;
`test`;
`test $A`;
`test \``;
`test \"`;