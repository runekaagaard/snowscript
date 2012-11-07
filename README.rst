About
+++++

Snowscript is a language that compiles to PHP. Its syntax is inspired by 
Python, Lua, Coffescript, Go and Scala and strives to be DRY, clean and 
easy to read as well as write.

Roadmap
+++++++

The current status as of October 3rd, 2012 is that both the lexer and parser
actually works. A lot of Snowscript can be compiled to PHP. But there is still
tons of work until it's usable. Come join the fun!

Todo 0.4
========

- Webpage.
- Documentation.
- Classes part2.
- Command line compile tool.
- Full examples.
- Some bugs in strings and comments.
- Tolerable error messages.
- Code cleanup.

Done
==== 

- Ternary operator.
- Control structures.
- For loops.
- Function style casts.
- Classes part1.
- Destructuring.
- Parsing of basic syntax.
- Transformations for the non LALR(1) compatible features of Snowscript like
  implicit parenthesis and significant whitespace.
- Lexer.

Todo 0.5
========

- Named parameters.
- List comprehension.
- Inner functions.
- Parser written in Snowscript.
- Existance.

Todo 0.6
========

- Closures.

Todo 0.7
========

- Great error messages.
- Namespaces.

Todo 0.8
========

- Macros.

Documentation
+++++++++++++

Whitespace
==========

Snowscript has significant whitespace, meaning that the code structure is 
managed by indenting/dedenting and not by curly brackets "{}". Whitespace is not
significant inside strings and brackets "()[]".

The only allowed indention format is 4 spaces.

snowscript::

    fn how_big_is_it(number)
        if number < 100
            <- NOT_VERY_BIG
        else
            <- BIG

php::

    function how_big_is_it($number) {
        if ($number < 100) {
            return NOT_VERY_BIG;
        } else {
            return BIG;
        } 
    }

Comments
========

snowscript::

    # Single line.
    # Single line as docblock. #
    # This is a docblock,

      spanning multiple lines.

php::

    // Single line.
    /**
     * Single line as docblock. 
     */
    /**
     * This is a docblock,
     *
     * spanning multiple lines. 
     */

Arrays
======

Arrays are defined using square brackets "[]". Items are separated by ",". A
trailing "," is allowed.

Arrays can contain key/value pairs seperated with "=". The keys can be omitted
and running integers will be assigned. Keys are always interpreted stringy.
Keys not matching the regex "[a-zA-Z_][a-zA-Z0-9_]+" can be made by surrounding
the key with quotes.

snowscript::

    pianists = ["McCoy Tyner", "Fred Hersch", "Bill Evans"]
    series = [
        Heroes = [
            genre = "Science Fiction",
            creator = "Tim Kring",
            seasons = 4,
        ],
        "Game Of Thrones" = [
            genre = "Medieval fantasy",
            creator = "David Benioff",
            seasons = 2,
        ],
    ]

php::

    $pianists = array("McCoy Tyner", "Fred Hersch", "Bill Evans");
    
    $series = array(
        'Heroes' => array(
            'genre' => "Science Fiction",
            'creator' => "Tim Kring",
            'seasons' => 4,
        ),
        "Game Of Thrones" => array(
            'genre' => "Medieval fantasy",
            'creator' => "David Benioff",
            'seasons' => 2,
        ),
    );

Accessing items is done using square brackets "[]".

snowscript::

    echo answers[0]['options'][0]['help_text']

php::

    echo $answers[0]['options'][0]['help_text'];

Outside of bracket "[]()" context arrays can be defined without "[]".

snowscript::

    fn phone_home
        <- dial(NUMBER), 0
    message, status = phone_home()

php::

    function phone_home() {
        return array(dial(NUMBER), 0);
    }
    list($message, $status) = phone_home();

Strings
=======

There are four kind of strings: """, ", ''' and ', all multiline.

Whitespace before the current indentation level is stripped. All empty lines
ending in "\\" is stripped.

Quoted
------

Code inside "{}" concatenates to the string.

snowscript::

    fn travel
        echo "\
            The {animal} went to {world.place()}
            with his {NUM} friends. 
        \"

    """<a href="https://snowscript.org">Snowscript</a>\n"""

php::

    function travel() {
        echo "The " . $animal . " went to " . $world->place() . "\n"
        " with his " . NUM  . " friends.";
        
    }
    "<a href=\"https://snowscript.org\">Snowscript</a>";

Unquoted
--------

snowscript::

    'No {magic} here\n'
    '''{nor()} here.'''

php::

    'No {magic} here\n';
    '''{nor()} here.''';

Concatenation
-------------

Strings can be concatenated with the "%" operator, but the 
``"Hello {current_planet()}"`` form is preferred.

snowscript::

    echo 'I am' % ' legend!'

php::

    echo 'I am' . ' legend!';

Functions
=========

The "fn" keyword is used to define functions, and "<-" to return a value.

snowscript::

    fn titlefy(FancyString fancystring)
        <- fancystring.make_fancy()

php::

    function titlefy(FancyString $fancystring) {
        return $fancystring->make_fancy();
    }
    
Arguments passed as reference must have a prefixing "&".

snowscript::

    fn init_ab(&a, &b)
        a = 10
        b = 10
    init_ab(&a, &b)
    
php::

    function init_ab(&$a, &$b) {
        $a = 10;
        $b = 10;
    }
    init_ab($a, $b);

Optional parameters
-------------------

Functions does not allow to be defined with optional parameters. Functions in
PHP land using optional parameters can of course be called.

Named parameters
----------------

Named parameters uses variable declaration syntax.

snowscript::

    fn render(template, allow_html=true, klingon=false)
        echo template.render(allow_html, klingon)

    render("index.html", klingon=true)

php::

    function render($template, $options_) {
        $defaults_ = array(
            'allow_html' => true, 
            'klingon' => false,
        );
        $options_ += $defaults_;
        echo $template->render($options_['allow_html'], $options_['klingon']);
    }

    render("index.html", array('klingon'=> true);

Chaining
--------

Function calls can be chained using the "->" operator which passes the prior 
expression along as the first argument to the function.

snowscript::

    "peter"->ucfirst()->str_rot13()

php::

    str_rot13(ucfirst("peter"));

Inner functions
---------------

Functions inside functions are defined at compile time, and only available
inside the scope where they are defined. Nesting can go arbitrarily deep.

snowscript::

    fn wash_car(Car car)
        fn apply_water(car)
            pass
        fn dry(car)
            pass
        <- car->apply_water()->dry()

php::
    
    function _wash_car_apply_water_($car) {}
    function _wash_car_dry_($car) {}
    function wash_car(Car $car) {
        return _wash_car_dry_(_wash_car_apply_water_($car));
    }

Closures
--------

Anonymous functions are declared like a normal function without the function 
name and surrounded with "()".

A "+" before the variable name binds a variable from the outer scope.

snowscript::
    
    use_me = get_use_me()
    little_helper = (fn(input, +use_me)
        <- polish(input, use_me))

    little_helper(Lamp())
    
    takes_functions(
        (fn(x)
            y = give_me_a_y(x)
            <- x * 2, y
        ),
        (fn(y, c)
            <- y * c
        ),
    )

php::

    $use_me = get_use_me();
    $little_helper = function($input) use ($use_me) {
        return polish(input, $use_me);
    }

    little_helper(new Lamp);
    
    takes_functions(
        function(x) {
            $y = give_me_a_y($x);
            return array(x * 2, $y);
        },
        function(y, c) {
            return y * c;
        }
    )

As the only structure in Snowscript, closures has a single line mode.

snowscript::

    filter(guys, (fn(guy) <- weight(guy) > 100))

php::

    filter($guys, function() {
        return weight($guy) > 100;
    });

Destructuring
=============

Snowscript has simple destructuring.

snowscript::

    a, b, c = b, c, a
    [a, b, [c, d]] = letters

php::

    list($a, $b, $c) = [$b, $c, $a];
    list($a, $b, list($c, $d)) = $letters;

Control structures
==================

Two control structures are available: "if" and the ternary operator.

if
--

snowscript::

    if white_walkers.numbers < 500
        fight_valiantly()
    elif feeling_lucky
        improvise()
    else
        run()

php::

    if ($white_walkers->numbers < 500) {
        fight_valiantly();
    } elif ($feeling_lucky) { 
        improvise();
    } else {
        run();
    }

Ternary operator
----------------

Ternary operator is a oneline ``if a then b else c`` syntax.

snowscript::

    echo if height > 199 then "tall" else "small"
    
php::

    echo ($height > 199 ? "tall" : "small");


Existence
=========

There are two existence operators "?" and "??". The first checks with 
``isset(expr)``, the second with ``!empty(expr)``.

snowscript::

    if field['title']?
        do_stuff()

    stuff = try_this() ?? that ?? "Default"

php::

    if (isset($field['title'])) {
        do_stuff();
    }

    $stuff = false;
    $tmp_ = try_this();
    if ($tmp_) {
        $stuff = $tmp_;
    } elseif(!empty($that)) {
        $stuff = $that;
    } else {
        $stuff = "Default";
    }
    unset($tmp_);
    
Type casting
============

To cast an expression to a type, use the ``array``, ``bool``, ``float``, 
``int``, ``object`` or ``str`` functions.

php::

    array(a)

php::

    (array) $a;

Loops
=====

For
---

Two kind of for loops are supported. Iterating over a collection, and iterating
over a numeric range. Both key and value are local to the loop. An "&" can be 
used to designate the value as by-reference.

snowscript::

    for title, data in flowers
        echo "{data.id}: title"
    for &n in numbers
        n *= 2

    for i in 1 to 10 step 2
        echo i
    for i in 10 downto 1
        echo i

php::

    foreach ($flowers as $title => $data) {
        echo $data->id . ": " . $title;
    }
    unset($title, $data);
    foreach ($numbers as $n) {
        $n *= 2;
    }
    unset($n);

    for ($i=1, $i <= 10, $i+=2) {
        echo $i;
    }
    unset($i);
    for ($i=10, $i >= 0, --$i) {
        echo $i;
    }
    unset($i);

While
-----

snow::
    
    while frog.ass.is_watertight
        echo "Rinse and repeat."

php::

    while ($frog->ass->is_watertight) {
        echo "Rinse and repeat.";
    }

Array comprehension
===================

Snowscript has array comprehension similiar to that of Python and others.

snowscript::

    [x, y for x in [1,2,3] for y in [3,1,4] if x != y]->var_dump()
    
    fights = [fight(samurai, villain)
              for samurai in seven_samurais
                  if samurai.is_awake()
                    for villain in seven_vaillains
                        if not villain.is_in_jail()]

php::

    $result_ = array();
    foreach (array(1, 2, 3) as $x) {
        foreach (array(3, 1, 4) as $y) {
            if ($x != $y) {
                $result_[$x] = $y;
            }
        }
    }
    unset($x, $y);
    var_dump($result_);

    $fights = array();
    foreach ($seven_samurais as $samurai) {
        if (!$samurai->is_awake()) {
            continue;
        }
        foreach ($seven_villains as $villain) {
            if ($villain->is_in_jail()) {
                continue;
            }
            $fights[] = fight($samurai, $villain);
        }
    }
    unset($samurai, $villain);

Naming conventions
==================

Snowscript uses naming conventions to strip out some of PHP's operators. 
Classes are PascalCase, constants are ALL_CAPS while variables, methods and
functions are whats left.

snowscript::
    
    foo    
    foo()
    Foo()
    FOO
    
    bar.foo
    bar::foo
    bar::FOO
    Bar::foo
    Bar::FOO
     
php::

    $foo;
    foo();
    new Foo;
    FOO;
    
    $bar->foo;
    $bar::$foo;
    $bar::FOO;
    Bar::$foo;
    Bar::FOO;
    
Classes
=======

Declaration
-----------

The arguments to the class is given after the class name.

The "." is used to access the class instance.

snowscript::

    class TabularWriter(File path, filesystem, title)
        # Properties. #
        title = title
        _filehandle = null
        
        # Constant by convention.
        VERSION = 0.4
            
        # Methods. #
        fn check_filesystem(filesystem)
            if not filesystems()[filesystem]?
                throw UnsupportedFilesystemError()
        
        fn init_file(path)
            if not file_exists(path)
                throw FileMissingError()
            else
                ._filehandle = open_file(path)

        # Initialize object.
        check_filesystem(filesystem)
        init_file(path)

php::

    class TabularWriter {
        /**
         * Properties.
         */
        public $title;
        public $_filehandle;
        
        /**
         * Constants.
         */        
        const VERSION = 0.4;

        /**
         * Constructor.
         */
        public function __construct(File path, title) {
            $this->title = $title;
            $filesystem_ = new Filesystem;
            self::$filesystem = $filesystem_.get();
            unset($filesystem_);
            $this->check_filesystem();
            $this->init_file($path);
        }

        /**
         * Methods.
         */
         
        public function check_filesystem() {
            $tmp_ = supported_filesystems();
            if (!isset($tmp_[self::$filesystem])) {
                throw new UnsupportedFilesystemError;
            }
            unset($tmp_);
        }

        public function init_file($path) {
            if (!file_exists($path)) {
                throw new FileMissingError;
            } else {
                $this->filehandle = open_file($path);
            }
        }
    }
    
Protected and private visibility using "private" and "protected" is supported 
but not considered very "snowy", after all "we're all consenting adults here". 
Instead it's recommended to prefix members with a "_" to mark them as a 
implementation detail. The "public", "final", "static" and "abstract" keywords 
are supported as well, but not recommended.

"::" is used to access the class.

Functions and properties can be indented below modifier keywords.

A class can inherit a single class, implement multiple interfaces and use
multiple traits.

snowscript::

    abstract class FactoryFactory
        extends AbstractBuilder 
        implements IFactoryFactory, IBuilder
        use FactoryBehaviour, LoggingBehaviour

        DEFAULT_FACTORY = "DefaultFactory"

        protected static 
            factories = []
            version = 1.0

        public static fn getInstance(factoryClassName)
            <- ::factories[factoryClassName]

php::

    abstract class FactoryFactory extends AbstractBuilder implements FactoryFactoryInterface, IBuilder {
        use FactoryBehaviour;
        use LoggingBehaviour;

        const DEFAULT_FACTORY = "DefaultFactory";

        protected static $factories = [];
        protected static $version = 1.0;

        public static function getInstance($factoryClassName) {
            return self::factories[$factoryClassName];
        }
            
    }

Usage
-----

Class instantiation uses function notation.

snowscript::

    Bicycle(Rider())

php::

    new Bicycle(new Rider));

Properties and methods on instantiated classes is accessed with the "."
operator. Using "::" accesses static members.

snowscript::

    wind = Wind(52, 12)
    wind.blow()
    Newspaper().read()
    
    Player::register("Ronaldo")
    Player::MALE
    Player::genders

php::

    $wind = Wind(52, 12);
    $wind->blow();
    (new Newspaper())->read();
    
    Player::register("Ronaldo");
    Player::MALE;
    Player::$genders;

Operators
=========

Stub.

A number of operators has changed from PHP.

================= ============================
PHP               Snow
================= ============================
&&                and
!                 not
||                or
and               _and_ (Not recommended)
or                _or_ (Not recommended)
%                 mod
$a  %= $b         a mod= b
.                 %
$a .= $b          a %= b
&                 band
\|                 bor
^                 bxor
<<                bleft
>>                bright
~                 bnot
================= ============================

Namespaces
==========

Stub.

General
-------

A namespace is defined by adding an empty file called "__namespace.snow" in the 
folder which should be the root of the namespace. So given a directory structure
as::

    .
    └── starwars
        ├── __namespace.snow
        ├── battle.snow
        ├── galaxy.snow
        └── settings.snow

the file "battle.snow" would have the namespace "starwars.battle". If no
"__namespace.snow" file is found in the same folder or above, the namespace will 
be that of the file itself.

Classes, interfaces, traits, functions, constants and variables can be imported 
from a namespace. Sub-namespaces are separated with ":".

If any member is prefixed with "_" it is a warning that it should not be 
accessed from outside its file.

snowscript::

    # Import a class, function, variable, constant and namespace respectively.
    from starwars:battle use (XFighter(), set_trap(), fighters, WHAT_TO_TRUST, 
                              deathstar:)
 
    # Aliasing.
    from Starwars use XFighter() as X(), set_trap() as st()
    use Db:Fields as F

Namespaces (importing)
----------------------

Namespaces can be imported and must be postfixed with a ":".

snowscript::

    from Db use Fields:, Transaction:

php::

    use \Db\Fields;
    use \Db\Transaction;

Classes, interfaces and traits
------------------------------

Classes, interfaces and traits can be imported from other namespaces. Their 
names must be PascalCase and postfixed with "()".

snowscript::

    # In the file battle.snow.
    from starwars:galaxy use Dagobah(), Alderaan(), Sullust()
    planet = Dagobah()

php::

    namespace \starwars\battle;

    use \starwars\galaxy\Dagobah;
    use \starwars\galaxy\Alderaan;
    use \starwars\galaxy\Sullust;
    $planet = new Dagobah();

Functions
---------

Functions can opposed to PHP be imported too.

Their names must not be PascalCase nor ALL_CAPS. They must be postfixed with 
"()".

snowscript::
    
    # In the file galaxy.snow.
    from starwars:battle use attack()
    attack()

php::
    
    namespace \starwars\galaxy;

    use \starwars\battle;
    battle.attack();

Constants
---------

Constants must be ALL_CAPS.

snowscript::

    from starwars:settings use NUMBER_OF_OCEANS
    echo NUMBER_OF_OCEANS

php::

    use \starwars\settings\NUMBER_OF_OCEANS;
    echo NUMBER_OF_OCEANS;

Variables
---------

Opposed to PHP, variables assigned in the body of a file belongs to the
namespace of that file, not in the global namespace. Their names must not be
PascalCase nor ALL_CAPS.

snowscript::
    
    # In the file settings.snow.
    jedis = ['Luke', 'Obi-Wan', 'Yoda']

php::

    namespace \starwars\settings;
    global $starwars_settings_jedis = array('Luke', 'Obi-Wan', 'Yoda');

This means that variables can be imported.

snowscript::

    # In the file battle.snow.
    from starwars:settings use jedis

    fn print_jedis
        <- ["<li>{jedi}</li>" for jedi in jedis]->implode()

php::

    namespace \starwars\battle;

    function print_jedis();
        global $starwars_settings_jedis;
        $result_ = array();
        foreach ($starwars_settings_jedis as $jedi) {
            $result_[] = '<li>' . $jedi . '</li>'; 
        }
        return implode($result_);

Global Space
------------

The global namespace can be accessed directly with a prefixing ":".

snowscript::

    :trim(" A string")

php::

    \trim(" A string")

Scoping rules
=============

Stub.

my_var = 42

fn foo()
    # Outputs 42.
    echo my_var

    # Compile error.
    my_var = 42

    # Compile error.
    bar(&my_var)

fn foo2()
    mutates my_var

    # OK.
    my_var = 43

    # OK.
    bar(&my_var)

Traits
======

Stub.

Macros
======

Stub.
