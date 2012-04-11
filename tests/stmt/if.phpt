--TEST--
If/Elseif/Else
--FILE--
<?php
if      (a) {}
elseif  (b) {}
elseif  (c) {}
else         {}
if (a) {} 
if      (a):
elseif  (b):
elseif  (c):
else        :
endif;
if (a): endif; 
--EXPECT--
<?php
if      ($a) {}
elseif  ($b) {}
elseif  ($c) {}
else         {}
if ($a) {} 
if      ($a):
elseif  ($b):
elseif  ($c):
else        :
endif;
if ($a): endif; 