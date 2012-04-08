<?php

/**
 * Autoloader. Stolen from PHP-Parser.
 */
class Snowscript_Autoloader
{
    /**
    * Registers PHPParser_Autoloader as an SPL autoloader.
    */
    static public function register()
    {
        ini_set('unserialize_callback_func', 'spl_autoload_call');
        spl_autoload_register(array(__CLASS__, 'autoload'));
    }

    /**
    * Handles autoloading of classes.
    *
    * @param string $class A class name.
    */
    static public function autoload($class)
    {
        if (0 !== strpos($class, 'Snowscript')) {
            return;
        }

        $file = dirname(dirname(__FILE__)) . '/' . strtr($class, '_', '/') . '.php';
        if (is_file($file)) {
            require $file;
        }
    }
}