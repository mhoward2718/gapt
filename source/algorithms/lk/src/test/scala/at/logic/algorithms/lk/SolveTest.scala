package at.logic.algorithms.lk

import at.logic.calculi.lk.base.LKProof
import at.logic.calculi.lk.base.FSequent
import at.logic.calculi.lk.propositionalRules.{Axiom, NegLeftRule}
import at.logic.calculi.occurrences.{FormulaOccurrence, defaultFormulaOccurrenceFactory}
import at.logic.language.hol._
import at.logic.language.hol.logicSymbols.{LogicalSymbolsA, ConstantStringSymbol}
import at.logic.language.lambda.symbols.VariableStringSymbol
import at.logic.language.schema.{And => AndS, Or => OrS, Neg => NegS, Imp => ImpS, _}
import java.io.File.separator
import scala.io._
import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.execute.Success
import at.logic.language.lambda.types.{To, Ti}
import at.logic.algorithms.lk.statistics._
import at.logic.calculi.lk.lkSpecs.beSyntacticFSequentEqual
import at.logic.calculi.expansionTrees.{ExpansionTree, ExpansionSequent, Atom => AtomET, Neg => NegET, Or => OrET, WeakQuantifier => WeakQuantifierET, StrongQuantifier => StrongQuantifierET, toSequent}

@RunWith(classOf[JUnitRunner])
class SolveTest extends SpecificationWithJUnit {
  implicit val factory = defaultFormulaOccurrenceFactory
  "SolveTest" should {
/*    "solve the sequents" in {
      val k = IntVar(new VariableStringSymbol("k"))
      val real_n = IntVar(new VariableStringSymbol("n"))
      val n = k
      val n1 = Succ(k); val n2 = Succ(n1); val n3 = Succ(n2)
      val k1 = Succ(k); val k2 = Succ(n1); val k3 = Succ(n2)
      val s = Set[FormulaOccurrence]()

      val i = IntVar(new VariableStringSymbol("i"))
      val A = IndexedPredicate(new ConstantStringSymbol("A"), i)
      val B = IndexedPredicate(new ConstantStringSymbol("B"), i)
      val C = IndexedPredicate(new ConstantStringSymbol("C"), i)
      val zero = IntZero(); val one = Succ(IntZero()); val two = Succ(Succ(IntZero())); val three = Succ(Succ(Succ(IntZero())))
      val four = Succ(three);val five = Succ(four); val six = Succ(Succ(four));val seven = Succ(Succ(five));       val A0 = IndexedPredicate(new ConstantStringSymbol("A"), IntZero())
      val A1 = IndexedPredicate(new ConstantStringSymbol("A"), one)
      val A2 = IndexedPredicate(new ConstantStringSymbol("A"), two)
      val A3 = IndexedPredicate(new ConstantStringSymbol("A"), three)

      val B0 = IndexedPredicate(new ConstantStringSymbol("B"), IntZero())

      val Ak = IndexedPredicate(new ConstantStringSymbol("A"), k)
      val Ai = IndexedPredicate(new ConstantStringSymbol("A"), i)
      val Ai1 = IndexedPredicate(new ConstantStringSymbol("A"), Succ(i))
      val orneg = OrS(at.logic.language.schema.Neg(Ai).asInstanceOf[SchemaFormula], Ai1.asInstanceOf[SchemaFormula]).asInstanceOf[SchemaFormula]

      val Ak1 = IndexedPredicate(new ConstantStringSymbol("A"), Succ(k))
      val An = IndexedPredicate(new ConstantStringSymbol("A"), k)
      val An1 = IndexedPredicate(new ConstantStringSymbol("A"), n1)
      val An2 = IndexedPredicate(new ConstantStringSymbol("A"), n2)
      val An3 = IndexedPredicate(new ConstantStringSymbol("A"), n3)

      val biga = BigAnd(i, A, zero, one)
      val bigo = BigOr(i, A, zero, one)
      val biga2 = BigAnd(i, A, zero, two)
      val bigo2 = BigOr(i, A, zero, two)

      val fseq = FSequent(A0 :: A1 :: Nil, bigo :: Nil)

      val p = solve.solvePropositional(fseq)

      // TODO: something with these...
      solve.solvePropositional(FSequent(Neg(And(Neg(A), Neg(B))) :: Nil, Or(A , B) :: Nil))
      solve.solvePropositional(FSequent(Or(Or(A, B), C) :: Nil, A :: B :: C :: Nil))
      solve.solvePropositional(FSequent(And(A , B) :: Nil, Neg(Or(Neg(A), Neg(B))) :: Nil))
      solve.solvePropositional(FSequent(A0 :: A1 :: A2 :: Nil, biga2 :: Nil))
      solve.solvePropositional(FSequent(A :: B :: C :: Nil, And(And(A, B), C) :: Nil))
      solve.solvePropositional(FSequent(bigo2 :: Nil, A0 :: A1 :: A2 :: Nil))
      
      val c2 = HOLConst(new ConstantStringSymbol("c"), Ti())
      val d2 = HOLConst(new ConstantStringSymbol("d"), Ti())
      val e2 = HOLConst(new ConstantStringSymbol("e"), Ti())
      val Pc2 = Atom(new ConstantStringSymbol("P"), c2::Nil)
      val Pd2 = Atom(new ConstantStringSymbol("P"), d2::Nil)
      val Pe2 = Atom(new ConstantStringSymbol("P"), e2::Nil)
      val andPc2Pd2 = And(Pc2, Pd2)
      val impPc2Pd2 = Imp(Pc2, Pd2)
      val imp_andPc2Pd2_Pe2 = Imp(andPc2Pd2, Pe2)
      val orPc2Pd2 = Or(Pc2, Pd2)
      val seq11 = FSequent(Pc2::Nil, Pc2::Nil)
      val seq12 = FSequent(andPc2Pd2::Nil, Pc2::Nil)
      val seq13 = FSequent(Pc2::Nil, orPc2Pd2::Nil)
      val seq14 = FSequent(andPc2Pd2::Nil, orPc2Pd2::Nil)
      val seq15 = FSequent(Pc2::impPc2Pd2::imp_andPc2Pd2_Pe2::Nil, Pe2::Nil)
      val seq16 = FSequent(Pc2::Nil, Pd2::Nil)

      solve.solvePropositional(seq16) must beEqualTo (None)
    }

    "prove non-atomic axioms (1)" in {
      import at.logic.language.hol._
      val List(x,y,z)    = List("x","y","z") map (x => HOLVar(VariableStringSymbol(x), Ti()))
      val List(u,v,w) = List("u","v","w") map (x => HOLVar(VariableStringSymbol(x), Ti() -> Ti()))
      val List(a,b,c, zero)    = List("a","b","c","0") map (x => HOLConst(ConstantStringSymbol(x), Ti()))
      val List(f,g,h,s)    = List("f","g","h","s") map (x => HOLConst(ConstantStringSymbol(x), Ti() -> Ti()))
      val List(p,q)      = List("P","Q") map (x => HOLConst(ConstantStringSymbol(x), Ti() -> Ti()))
      val List(_Xsym,_Ysym)    = List("X","Y") map (x => VariableStringSymbol(x))
      val List(_X,_Y)    = List(_Xsym,_Ysym) map (x => HOLVar(x, Ti() -> To()))

      val xzero = Atom(_X,List(zero))
      val xx = Atom(_X,List(x))
      val xsx = Atom(_X,List(Function(s,List(x))))

      val ind = AllVar(_X, Imp(And(xzero, AllVar(x, Imp(xx,xsx) )), AllVar(x, xx) ))
      val fs = FSequent(ind::Nil,ind::Nil)
      val proof = AtomicExpansion(fs)
      //check if the derived end-sequent is correct
      proof.root.toFSequent must beSyntacticFSequentEqual (fs)

      //check if three different eigenvariables were introduced and nothing more
      val psymbols = proof.nodes.flatMap(x => x.asInstanceOf[LKProof].root.toFormula.symbols).filterNot(_.isInstanceOf[LogicalSymbolsA]).toSet
      val fssymbols = fs.formulas.flatMap(_.symbols).filterNot(_.isInstanceOf[LogicalSymbolsA]).toSet
      //println(psymbols)
      (psymbols diff fssymbols).size must_== 3
      (fssymbols diff psymbols) must beEmpty
    }

    "prove non-atomic axioms (2)" in {
      import at.logic.language.hol._
      val List(x,y,z)    = List("x","y","z") map (x => HOLVar(VariableStringSymbol(x), Ti()))
      val List(u,v,w) = List("u","v","w") map (x => HOLVar(VariableStringSymbol(x), Ti() -> Ti()))
      val List(a,b,c, zero)    = List("a","b","c","0") map (x => HOLConst(ConstantStringSymbol(x), Ti()))
      val List(f,g,h,s)    = List("f","g","h","s") map (x => HOLConst(ConstantStringSymbol(x), Ti() -> Ti()))
      val List(psym,qsym)      = List("P","Q") map (x => ConstantStringSymbol(x))
      val List(_Xsym,_Ysym)    = List("X","Y") map (x => VariableStringSymbol(x))
      val List(_X,_Y)    = List(_Xsym,_Ysym) map (x => HOLVar(x, Ti() -> To()))

      val xzero = Atom(psym,List(y, Function(s,List(x))))

      val formula = AllVar(x, Neg(ExVar(y, xzero)))
      val fs = FSequent(List(Atom(psym,x::Nil), formula),List(formula, Atom(psym,y::Nil)))
      val proof = AtomicExpansion(fs)
      //check if the derived end-sequent is correct
      proof.root.toFSequent must beSyntacticFSequentEqual (fs)

      //check if two different eigenvariables were introduced and nothing more
      val psymbols = proof.nodes.flatMap(x => x.asInstanceOf[LKProof].root.toFormula.symbols).filterNot(_.isInstanceOf[LogicalSymbolsA]).toSet
      val fssymbols = fs.formulas.flatMap(_.symbols).filterNot(_.isInstanceOf[LogicalSymbolsA]).toSet
      (psymbols diff fssymbols).size must_== 2
      (fssymbols diff psymbols) must beEmpty
    }
*/

    // tests of expansionProofToLKProof also in MiscTest, such that it can be used in combination with extractExpansionTrees

    "prove sequent where quantifier order matters" in {
      // example from Chaudhuri et.al.: A multi-focused proof system ...
      val List(x,y,u,v)    = List("x","y","u","v") map (x => HOLVar(VariableStringSymbol(x), Ti()))
      val c = HOLConst(ConstantStringSymbol("c"), Ti())
      val d = ConstantStringSymbol("d")


      val formula = ExVar(x, Or( Neg( Atom(d, x::Nil) ), AllVar(y, Atom(d, y::Nil)))) // exists x (-d(x) or forall y d(y))

      val inst1 = OrET(
        NegET( AtomET( Atom(d, u::Nil))), // -d(u)
        StrongQuantifierET( AllVar(y, Atom(d, y::Nil)), v, AtomET(Atom(d, v::Nil))) // forall y d(y) +^v d(v)
      )

      val inst2 = OrET(
        NegET( AtomET( Atom(d, c::Nil))), // -d(c)
        StrongQuantifierET( AllVar(y, Atom(d, y::Nil)), u, AtomET(Atom(d, u::Nil))) // forall y d(y) +^u d(u)
      )

      // here, the second tree, containing c, must be expanded before u, as u is used as eigenvar in the c branch
      val et = WeakQuantifierET.applyWithoutMerge(formula, List( (inst1, u), (inst2, c)))
      val etSeq = new ExpansionSequent(Nil, et::Nil)

      val lkProof = solve.expansionProofToLKProof( toSequent(etSeq).toFSequent, etSeq )
      lkProof.isDefined must beTrue
    }

  }
}

