/*
 * HigherOrderLogicTest.scala
 */

package at.logic.language.hol

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import at.logic.language.lambda.types._
import at.logic.language.lambda._
import at.logic.language.lambda.symbols._
import logicSymbols._
import at.logic.language.lambda.types.Definitions._
import skolemSymbols._
import at.logic.language.lambda.BetaReduction._
import at.logic.language.lambda.BetaReduction.ImplicitStandardStrategy._

@RunWith(classOf[JUnitRunner])
class HigherOrderLogicTest extends SpecificationWithJUnit {

  "HigherOrderLogic" should {
    val c1 = HOLConst("a", i->o)
    val v1 = HOLVar("x", i)
    val a1 = HOLApp(c1, v1)
    val c2 = HOLVar("a", i->(i->o))
    val v21 = HOLVar("x", i)
    val v22 = HOLVar("y", i)
    val a21 = HOLApp(c2, v21)
    val a22 = HOLApp(a21, v22)

    "mix correctly the formula trait (1)" in {
      val result = a1 match {
        case x: HOLFormula => true
        case _ => false
      }
      result must beTrue
    }
    "mix correctly the formula trait (2)" in {
      val result = a22 match {
        case x: HOLFormula => true
        case _ => false
      }
      result must beTrue
    }
    "mix correctly the formula trait (3)" in {
      val at1 = Atom(HOLVar("P", ->(c2.exptype, ->(a22.exptype, o))), c2::a22::Nil)
      // Another way to construct P's type is: FunctionType(To(), args.map(a => a.exptype) )
      val result = at1 match {
        case x: HOLFormula => true
        case _ => false
      }
      result must beTrue
    }
    "And connective should return the right And formula" in {
      val c1 = HOLConstFormula("a")
      val c2 = HOLConstFormula("b")
      val result = And(c1, c2) match {
        case HOLApp(HOLApp(andC, c1), c2) => true
        case _ => false
      }
      result must beTrue
      }
    "Or connective should return the right formula" in {
      val c1 = HOLConstFormula("a")
      val c2 = HOLConstFormula("b")
      val result = Or(c1, c2) match {
        case HOLApp(HOLApp(orC, c1), c2) => true
        case _ => false
      }
      result must beTrue
    }
    "Imp connective should return the right formula" in {
      val c1 = HOLVar("a", o).asInstanceOf[HOLFormula]
      val c2 = HOLVar("b", o).asInstanceOf[HOLFormula]
      val result = Imp(c1, c2) match {
        case HOLApp(HOLApp(impC, c1), c2) => true
        case _ => false
      }
      result must beTrue
    }
    "Neg connective should " in {
      "return the right formula" in {
        val c1 = HOLVar("a", o).asInstanceOf[HOLFormula]
        val result = Neg(c1) match {
          case HOLApp(negC, c1) => true
          case _ => false
        }
        result must beTrue
      }
      "be extracted correctly" in {
        val e = HOLApp(HOLConst("g","(i -> i)"), HOLConst("c", "i")::Nil)
        val result = e match {
          case Neg(_) => false
          case _ => true
        }
        result must beTrue
      }
    }
    "substitute and normalize correctly when Substitution is applied" in {
      val x = HOLVar("X", i -> o )
      val f = HOLVar("f", (i -> o) -> i )
      val xfx = HOLApp(x, HOLApp( f, x ) )

      val z = HOLVar("z", i)
      val p = HOLVar("P", i -> o)
      val Pz = HOLApp( p, z )
      val t = HOLAbs( z, Pz )

      val sigma = Substitution( x, t )

      betaNormalize( sigma( xfx ) ) must beEqualTo( HOLApp( p, HOLApp( f, t ) ) )
    }
  }
  "Exists quantifier" should {
    val c1 = HOLConst("a", i->o)
    val v1 = HOLVar("x", i)
    val f1 = HOLAppFormula(c1,v1)
    "create a term of the right type" in {
      (ExVar(v1, f1).exptype) must beEqualTo (o)
    }
  }

  "Forall quantifier" should {
    val c1 = HOLConst("a", i->o)
    val v1 = HOLVar("x", i)
    val f1 = HOLAppFormula(c1,v1)
    "create a term of the right type" in {
      (AllVar(v1, f1).exptype) must beEqualTo (o)
    }
  }

  "Atoms" should {
    "be extracted correctly" in {
      val p = HOLConst("P", o)
      val result = p match {
        case Atom(HOLConst("P", o), Nil) => true
        case _ => false
      }
      result must beTrue
    }
  }
  
  "Equation" should {
    "be of the right type" in {
      val c1 = HOLConst("f1", i -> i)
      val c2 = HOLConst("f2", i -> i)
      val eq = Equation(c1,c2)
      val HOLApp(HOLApp(t,_), _) = eq
      t.exptype must beEqualTo ((i -> i) -> ((i -> i) -> o))
    }
  }

  "Substitution" should {
    "work correctly on some testcase involving free/bound vars" in {
      val s0 = HOLConst("s_{0}", i -> i)
      val C = HOLConst("C", i -> i)
      val T = HOLConst("T", i -> i)
      val sCTn = Function(s0, Function( C, Function( T, HOLConst("n", i)::Nil)::Nil)::Nil )
      val u = HOLVar("u", i)
      val v = HOLVar("v", i)
      val P1 = Atom( HOLVar("P", ->(sCTn.exptype, ->(i, o))), sCTn::u::Nil)
      val P2 = Atom( HOLVar("P", ->(sCTn.exptype, ->(i, o))), sCTn::v::Nil)
      val q_form = AllVar(u, ExVar(v, Imp(P1, P2)))
      
      q_form match {
        case AllVar(x, f) => {
          val a = HOLConst("a", x.exptype)
          val sub = Substitution( x, a )
          val P3 = Atom(HOLVar("P", ->(sCTn.exptype, ->(a.exptype, o))), sCTn::a::Nil)
          val s = sub( f )
          val result = s match {
            case ExVar(v, Imp(P3, P2)) => true
            case _ => false
          }
          result must beTrue
        }
      }
    }
  }

  "SkolemSymbolFactory" should {
      val x = HOLVar("x", i)
      val y = HOLVar("y", i)
      val f = AllVar( x, Atom(HOLVar("P", ->(i, o)), x::Nil ) )
      val s0 = new StringSymbol( "s_{0}" )
      val s1 = new StringSymbol( "s_{2}" )
      val s2 = new StringSymbol( "s_{4}" )
      val s3 = new StringSymbol( "s_{6}" )

      SkolemSymbolFactory.reset
      val stream = SkolemSymbolFactory.getSkolemSymbols

    "return a correct stream of Skolem symbols" in {
      stream.head must beEqualTo( s0 )
      stream.tail.head must beEqualTo( s1 )
      stream.tail.tail.head must beEqualTo( s2 )
    }
  }

  "Higher Order Formula matching" should {
    "not allow P and P match as an Atom " in {
      val f = And(Atom(HOLVar("P", o),Nil), Atom(HOLVar("P", o),Nil))

      f must beLike {
        case Atom(_,_) => println("Is an atom"); ko
        case Function(_,_,_) => ko
        case AllVar(_,_) => ko
        case ExVar(_,_) => ko
        case Or(_,_) => ko
        case Imp(_,_) => ko
        case And(_,_) => ok
        case _ => ko
      }
    }
  }
}
