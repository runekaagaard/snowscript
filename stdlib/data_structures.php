<?php
class SnowList implements ArrayAccess, IteratorAggregate, Countable
{
    public $arr = 1;
    
    public function __construct($arr)
    {
        $this->arr = $arr;
    }
    
    public function _assert_int($i)
    {
        if (\snow_neq(gettype($i), 'integer')) {
            throw new Exception(gettype('Index must be an integer, was ' . $i));
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
        if (!$this->offsetExists($i)) {
            throw new IndexError('Index does not exist: ' . $i);
        }
        $i2 = $this->_get_index($i);
        return $this->arr[$i2];
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
    
    public function getIterator()
    {
        return new ArrayIterator($this->arr);
    }
    
    public function count()
    {
        return count($this->arr);
    }
    
    public function slice($a, $b)
    {
        if (\snow_lt($b, 0)) {
            return new SnowList(array_slice($this->arr, $a, $b));
        } else {
            return new SnowList(array_slice($this->arr, $a, $this->count() - $b));
        }
    }
    
    public function append($x)
    {
        $this->arr[] = $x;
        return $this;
    }
    
    public function pop($i = -1)
    {
        if (\snow_eq($i, -1)) {
            return array_pop($this->arr);
        } else {
            $this->offsetGet($i);
            $i = $this->_get_index($i);
            $splice = array_splice($this->arr, $i);
            return $splice[0];
        }
    }
    
    public function extend($xs)
    {
        foreach ($xs as $x) {
            $this->append($x);
        }
        unset($x);
        return $this;
    }
    
    public function get($i)
    {
        return $this->offsetGet($i);
    }
    
    public function reversed()
    {
        return new SnowList(array_reverse($this->arr));
    }
    
    public function copy()
    {
        return unserialize(serialize($this));
    }
}
class SnowDict implements ArrayAccess, IteratorAggregate, Countable
{
    public $arr = 1;
    
    public function __construct($arr)
    {
        $this->arr = $arr;
    }
    
    public function _assert_type($k)
    {
        $type = gettype($k);
        if (\snow_neq($type, 'string') && \snow_neq($type, 'integer')) {
            throw new Exception("dict key type invalid: " . $type);
        }
    }
    
    public function offsetSet($k, $x)
    {
        $this->_assert_type($k);
        $this->arr[$k] = $x;
    }
    
    public function offsetGet($k)
    {
        $this->_assert_type($k);
        if (!$this->offsetExists($k)) {
            throw new KeyError('Key does not exist: ' . $k);
        }
        return $this->arr[$k];
    }
    
    public function offsetExists($k)
    {
        $this->_assert_type($k);
        return isset($this->arr[$k]);
    }
    
    public function offsetUnset($k)
    {
        $this->_assert_type($k);
        $this->offsetGet($k);
        unset($this->arr[$k]);
    }
    
    public function getIterator()
    {
        return new ArrayIterator($this->arr);
    }
    
    public function count()
    {
        return count($this->arr);
    }
    
    public function &__get($k)
    {
        $x = $this->offsetGet($k);
        return $x;
    }
    
    public function __set($k, $x)
    {
        $this->offsetSet($k, $x);
    }
    
    public function keys()
    {
        return array_keys($this->arr);
    }
    
    public function get($k, $_default = null)
    {
        $this->_assert_type($k);
        if ($this->offsetExists($k)) {
            return $this->offsetGet($k);
        } else {
            return $_default;
        }
    }
    
    public function copy()
    {
        return unserialize(serialize($this));
    }
}
