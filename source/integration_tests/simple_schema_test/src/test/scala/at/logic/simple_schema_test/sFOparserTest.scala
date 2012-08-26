/*
 * sFONparserTest.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package at.logic.simple_schema_test

import at.logic.language.schema._
import at.logic.calculi.lk.base.Sequent
import at.logic.calculi.lk.propositionalRules.{OrLeftRule, NegLeftRule, Axiom}
import at.logic.calculi.lksk.Axiom
import at.logic.parsing.readers.StringReader
import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import at.logic.language.hol._
import at.logic.language.hol.Definitions._
import at.logic.language.hol.ImplicitConverters._
import at.logic.language.lambda.typedLambdaCalculus._
import at.logic.language.hol.logicSymbols.ConstantStringSymbol
import at.logic.language.lambda.symbols.VariableStringSymbol
import at.logic.language.lambda.types.ImplicitConverters._
import at.logic.language.lambda.types.Definitions._
import at.logic.language.lambda.types._
import at.logic.language.lambda.symbols.ImplicitConverters._
import at.logic.parsing.readers.StringReader
import scala.io._
import java.io.File.separator
import java.io.{FileInputStream, InputStreamReader}
import org.specs2.execute.Success
import at.logic.gui.prooftool.gui.Main
import at.logic.algorithms.shlk._

@RunWith(classOf[JUnitRunner])
class sFOparserTest extends SpecificationWithJUnit {
  private class MyParser extends StringReader("")
  "sFOparserTest" should {

    "parse correctly a SLK-proof" in {
      val var3 = HOLVarFormula(new VariableStringSymbol("x3"))
      val var4 = HOLVarFormula(new VariableStringSymbol("x4"))
      val ax1  = at.logic.calculi.lk.propositionalRules.Axiom(var3::Nil, var3::Nil)
      val ax2  = at.logic.calculi.lk.propositionalRules.Axiom(var4::Nil, var4::Nil)
      val negl = NegLeftRule(ax1, var3)
      val proof  = OrLeftRule(negl, ax2, var3, var4)

      //          ParseQMON.parseProof( "1 : ax(x3: o |- x3: o)  " +
      //                            "2 : negL( 1 , x3:o)" +
      //                            "3 : ax(x4: o |- x4: o)" +
      //                            "4 : orL(2, 3, x3:o, x4:o)", "4").toString must beEqualTo (proof.toString)

      val A0 = IndexedPredicate(new ConstantStringSymbol("A"), IntZero())
      val i = IntVar(new VariableStringSymbol("i"))
      val Ai2 = IndexedPredicate(new ConstantStringSymbol("A"), Succ(Succ(i)))
      val Ai = IndexedPredicate(new ConstantStringSymbol("A"), Succ(i))
      val f1 = at.logic.language.schema.And(A0, BigAnd(i,Ai,IntZero(),Succ(i)))
      val ax11 = at.logic.calculi.lk.propositionalRules.Axiom(A0::Nil, A0::Nil)
      //          println("\n\n"+ax11.root.toString)

      //          ParseQMON.parseProof( "1 : ax(A(i+2) |- And A(0) BigAnd(i,0,s(i),A(i)))" +
      //                           "2 : negR(1,A(i+2))","2").root.toString

      //          ParseQMON.parseProof( "1 : pLink((psi,k+1) , A(0) |- A(0))","1").root.toString
      //          println("\n\n")



      //          val p = ParseQMON.parseProof(  "1 : pLink((psi,k)  A(0), BigAnd(i=0..k , (~A(i) \\/ A(i+1) ) ) |- A(k+1))" +
      //                                    "2 : ax(A(k+1) |- A(k+1))" +
      //                                    "3 : negL(2, A(k+1))" +
      //                                    "4 : ax(A(k+2) |- A(k+2))" +
      //                                    "5 : orL(3, 4, ~A(k+1), A(k+2))" +
      //                                    "6 : cut(1, 5, A(k+1))" +
      //                                    "root : andL(6, BigAnd(i=0..k , ( ~A(i) \\/ A(i+1) ) ), (~A(k+1) \\/ A(k+2) ) )", "root")
      //          println("\n\np = "+  p.root.toString())
      //          p.root.toString must beEqualTo ("(i.((¬(A(i)) ∨ A(s(i)))) ⋀ 0)(s(k)), A(0) :- A(s(s(k)))")
      //          val s = Source.fromFile("/home/cvetan/gapt-trunk/source/integration_tests/simple_schema_test/src/test/resources/input1.lks").toList.foldLeft("")((ch,res) => ch + res)
      //          val s = Source.fromFile("target" + separator + "test-classes" + separator + "input_multi_indxs.lks").toList.foldLeft("")((ch,res) => ch + res)
//      val s = new InputStreamReader(new FileInputStream("target" + separator + "test-classes" + separator + "sINDauto.lks"))
      val s = new InputStreamReader(new FileInputStream("target" + separator + "test-classes" + separator + "journal_example.lks"))

      val map = ParseQMON.parseProof(s)
      //          println("\n\np = "+  map.get("chi").get._1.get("root").get.root.toString()  )
      //                       val p = map.get("chi").get._2.get("root").get
      //          println("\n\npsi_b = "+  map.get("psi").get._1.get("root").get.root.toString()  )
      //          println("\n\npsi_s = "+  map.get("psi").get._2.get("root").get.root.toString()  )
      //          println("\n\nchi_b = "+  map.get("chi").get._1.get("root").get.root.toString()  )
      //          println("\n\nchi_s = "+  map.get("chi").get._2.get("root").get.root.toString()  )
      //          println("\n\nphi_b = "+  map.get("phi").get._1.get("root").get.root.toString()  )
      //          println("\n\nphi_s = "+  map.get("phi").get._2.get("root").get.root.toString()  )
      //          Main.display("Proof", map.head._2._1) ; while(true){}

      //          Main.display("phi", map.get("phi").get._2.get("root").get) ;


      //          val seq = ParseQMON.parseSequent("P(n,f(0,x)) |- ")
      //          val seq = ParseQMON.parseSequent("Forall x P(f(k,x)), P(x(k)) |- ")
      //          println(seq)

      println(Console.RED+"\n\n-------- sFO --------\n\n"+Console.RESET)
//      val p = map.get("\\sigma").get._2.get("root").get
//      val p1 = map.get("\\tau").get._2.get("root").get
//      val p2 = map.get("\\varphi").get._2.get("root").get
      val p2 = map.get("\\psi").get._2.get("root").get
//      val p3 = map.get("\\chi").get._2.get("root").get
      println("\n\n")
      printSchemaProof(p2)
      println("\n\n")
//      Main.display("\\sigma (k+1)", p)
//      Main.dispslay("\\tau (k+1)", p1)
//      Main.display("\\varphi (k+1)", p2)
//      Main.display("\\psi (k+1)", p2)
//      Main.display("\\chi (k+1)", p3);
//      val sig../..
// ma = applySchemaSubstitution("\\sigma",1)
//      println("\nvarphi = "+sigma.root)
//            Main.display("sigma", sigma);

      def f = HOLConst(new ConstantStringSymbol("f"), Ti()->Ti())
      def h = HOLConst(new ConstantStringSymbol("h"), ->(Tindex() , ->(Ti(), Ti())))
      def g = HOLConst(new ConstantStringSymbol("g"), ->(Tindex() , ->(Ti(), Ti())))
      val k = IntVar(new VariableStringSymbol("k"))
      val x = hol.createVar(new VariableStringSymbol("x"), Ti(), None).asInstanceOf[HOLVar]
      val base = x
      val step = foTerm("f",  sTerm(g, Succ(k), x::Nil)::Nil)
      val db = dbTRS(g, base, step)
//      val varphi = applySchemaSubstitution2("\\varphi",1, db)
//      va
// l varphi = applySchemaSubstitution2("\\tau",1, db)
//      val varphi = applySchemaSubstitution2("\\sigma",3, db)

//      Main.display("varphi", varphi);
//      while(true){}
      // specs2 require a least one Result, see org.specs2.specification.Example
      Success()

    }
  }
}