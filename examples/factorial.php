<?php
/**
 * Recursive factorial function.
 * 
 * @param int $n
 *     Must 0 or above.
 * @return int
 */
function fac1($n) {
    if ($n == 0) { 
        return 1;
    }
    return $n * fac1($n-1);
}
var_dump(fac1(0));
var_dump(fac1(1));
var_dump(fac1(10));

/**
 * Procedural factorial function.
 * 
 * @param int $n
 *     Must 0 or above.
 * @return int
 */
function fac2($n) {
    if ($n == 0) {
        return 1;
    }
    for ($i=$n-1;$i>1;--$i) {
        $n *= $i;
    }
    return $n;
}
var_dump(fac2(0));
var_dump(fac2(1));
var_dump(fac2(10));
