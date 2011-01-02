"""monkey-patch the compiler module

This adds an "AssignExpr" AST node and bytecode generation code.

The compiler module is hard to extend and the easiest implementation
was to insert new methods into the class.

"""

# Written by Andrew Dalke
# Copyright (c) 2008 by Dalke Scientific, AB
# 
# See LICENSE for details.


import compiler
from compiler import ast, pycodegen, symbols, syntax

# An AssignExpr node does assignments in an expression.
#
# It might look like this, assuming grammar support
#  if (x=5)


# Identical except that I need a new name so the dispatching works
# correctly.
class AssignExpr(ast.Assign):
    def __repr__(self):
        return "AssignExpr(%s, %s)" % (repr(self.nodes), repr(self.expr))

# Based on visitAssign but it leaves an extra copy on the
# stack so it can be used in the expression
def visitAssignExpr(self, node):
    self.set_lineno(node)
    self.visit(node.expr)
    for elt in node.nodes:
        self.emit("DUP_TOP")
        if isinstance(elt, ast.Node):
            self.visit(elt)

ast.AssignExpr = AssignExpr
pycodegen.CodeGenerator.visitAssignExpr = visitAssignExpr
symbols.SymbolVisitor.visitAssignExpr = symbols.SymbolVisitor.visitAssign

