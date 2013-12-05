<?php
class SnowList implements ArrayAccess
{
    
    public function __construct($arr)
    {
        $this->arr = $arr;
    }
    
    public function _assert_int($i)
    {
        if (\snow_neq(gettype($i), 'integer')) {
            throw new IndexError(gettype('Index must be an integer, was ' . $i));
        }
    }
    
    public function _get_index($i)
    {
        $this->_assert_int($i);
        return \snow_lt($i, 0) ? count($this->arr) + $i : $i;
    }
    
    public function offsetSet($i, $x)
    {
        if (\snow_eq($i, null)) {
            throw new Exception('[]= operator not supported. Use .append() instead');
        } else {
            $this->offsetGet($i);
            $i = $this->_get_index($i);
            $this->arr[$i] = $x;
        }
    }
    
    public function offsetGet($i)
    {
        $i = $this->_get_index($i);
        if (!$this->offsetExists($i)) {
            throw new IndexError('Index does not exist: ' . $i);
        }
        return $this->arr[$i];
    }
    
    public function offsetExists($i)
    {
        $i = $this->_get_index($i);
        return isset($this->arr[$i]);
    }
    
    public function offsetUnset($i)
    {
        $this->offsetGet($i);
        $i = $this->_get_index($i);
        unset($this->arr[$i]);
    }
    
    public function append($x)
    {
        $this->arr[] = $x;
    }
}
class SnowDict extends ArrayObject
{
    
    public function copy()
    {
        return unserialize($this->serialize());
    }
}
