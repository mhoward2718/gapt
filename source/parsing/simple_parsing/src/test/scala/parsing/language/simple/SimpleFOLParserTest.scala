/*
 * SimpleHOLParser.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package at.logic.parsing.language.simple

import org.specs._
import org.specs.runner._
import at.logic.language.hol.{HOLVar,HOLConst}
import at.logic.language.fol._
import at.logic.language.lambda.typedLambdaCalculus._
import at.logic.language.hol.logicSymbols.ConstantStringSymbol
import at.logic.language.lambda.symbols.VariableStringSymbol
import at.logic.language.lambda.types.ImplicitConverters._
import at.logic.language.lambda.types.Definitions._
import at.logic.language.lambda.types._
import at.logic.language.lambda.symbols.ImplicitConverters._
import at.logic.parsing.readers.StringReader

class SimpleFOLParserTest extends SpecificationWithJUnit {
  private class MyParser(input: String) extends StringReader(input) with SimpleFOLParser


  val var1 = FOLVar(new VariableStringSymbol("x1"))
  val const1 = FOLConst(new ConstantStringSymbol("c1"))
  val var2 = FOLVar(new VariableStringSymbol("x2"))
  val atom1 = Atom(new ConstantStringSymbol("A"),var1::var2::const1::Nil)
  val var3 = Atom(new ConstantStringSymbol("X3"), Nil)
  val func1 = Function(new ConstantStringSymbol("f"), var1::var2::const1::Nil)
  val and1 = And(atom1, var3)
  val or1 = Or(atom1, var3)
  val imp1 = Imp(atom1, var3)
  val neg1 = Neg(atom1)
  val ex1 = ExVar(var1,atom1)
  val all1 = AllVar(var1,atom1)
  val npx = Neg(Atom(new ConstantStringSymbol("p"), FOLVar(new VariableStringSymbol("x"))::Nil))

  "SimpleFOLParser" should {
    "parse correctly a variable" in {
        (new MyParser("x1").getTerm()) must beEqual (var1)
    }
    "parse correctly an constant" in {
        (new MyParser("c1").getTerm()) must beEqual (const1)
    }
    "parse correctly an atom" in {
        (new MyParser("A(x1, x2, c1)").getTerm()) must beEqual (atom1)
    }
    "parse correctly a formula" in {
        (new MyParser("X3").getTerm()) must beLike {case x: FOLFormula => true}
        (new MyParser("X3").getTerm()) must beEqual (var3)
    }
    "parse correctly a function 1" in {
      (new MyParser("f(x1)").getTerm()) must beEqual (Function(new ConstantStringSymbol("f"), var1::Nil))
    }
    "parse correctly a function 2" in {
        (new MyParser("f(x1, x2, c1)").getTerm()) must beEqual (func1)
    }
    "parse correctly an and" in {
        (new MyParser("And A(x1, x2, c1) X3").getTerm()) must beEqual (and1)
    }
    "parse correctly an or" in {
        (new MyParser("Or A(x1, x2, c1) X3").getTerm()) must beEqual (or1)
    }
    "parse correctly an imp" in {
        (new MyParser("Imp A(x1, x2, c1) X3").getTerm()) must beEqual (imp1)
    }
    "parse correctly an neg" in {
        (new MyParser("Neg A(x1, x2, c1)").getTerm()) must beEqual (neg1)
    }
    "parse correctly an exists" in {
        (new MyParser("Exists x1 A(x1, x2, c1)").getTerm()) must beEqual (ex1)
    }
    "parse correctly a forall" in {
        (new MyParser("Forall x1 A(x1, x2, c1)").getTerm()) must beEqual (all1)
    }
  }
}
