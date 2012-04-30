--TEST--
Foreach loop
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php('
for b in a
    pass
for &b in a
    pass
for b,c in a
    pass
for b,&c in a
    pass
for b in [1,2,3]
    pass
for b in 1 to 10
    echo b
for b in 10 downto 10
    echo b
for b in 1 to 10 step 2
    echo b
for b in 10 downto 10 step 2
    echo b
for b in Foo::getIt() downto func() step YES_WE_CAN
    echo b
', 0);
--EXPECT--
<?php
foreach ($a as $b) {
    
}
unset($b);
foreach ($a as &$b) {
    
}
unset($b);
foreach ($a as $b => $c) {
    
}
unset($b, $c);
foreach ($a as $b => &$c) {
    
}
unset($b, $c);
foreach (array(1, 2, 3) as $b) {
    
}
unset($b);
for ($b = 1; $b <= 10; ++$b) {
    echo $b;
}
unset($b);
for ($b = 10; $b >= 10; --$b) {
    echo $b;
}
unset($b);
for ($b = 1; $b <= 10; $var += 2) {
    echo $b;
}
unset($b);
for ($b = 10; $b >= 10; $var -= 2) {
    echo $b;
}
unset($b);
for ($b = func(); $b >= Foo::getIt(); $var -= YES_WE_CAN) {
    echo $b;
}
unset($b);