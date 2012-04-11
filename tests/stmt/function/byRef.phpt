--TEST--
Return and pass by ref
--FILE--
<?php
function a(&b) {}
function &a(b) {}
--EXPECT--
<?php
function a(&$b) {}
function &a($b) {}