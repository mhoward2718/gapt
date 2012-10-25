/** 
 * Description: 
**/
package at.logic.integration_tests

import at.logic.transformations.ceres.clauseSets.profile._

import at.logic.transformations.ceres.struct.StructCreators
import at.logic.transformations.ceres.clauseSets.StandardClauseSet
import  at.logic.transformations.ceres.clauseSets.profile

import at.logic.parsing.language.xml.XMLParser._
import at.logic.parsing.readers.XMLReaders._
import at.logic.algorithms.lk.simplification._
import at.logic.algorithms.lk.statistics._
import at.logic.algorithms.lk._
import at.logic.parsing.calculus.xml.saveXML

import at.logic.calculi.lk._
import at.logic.calculi.lk.base._
import at.logic.algorithms.lk.simplification._
import at.logic.algorithms.lk._
import at.logic.transformations.skolemization.lksk.LKtoLKskc

import java.util.zip.GZIPInputStream
import java.io.File.separator

import at.logic.transformations.skolemization.skolemize
import at.logic.transformations.ceres.projections.Projections
import at.logic.parsing.language.tptp.TPTPFOLExporter

import at.logic.provers.prover9._
import java.io.{IOException, FileReader, FileInputStream, InputStreamReader}
import org.junit.runner.RunWith
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LatticeTest extends SpecificationWithJUnit {
  val box = List()
  def checkForProverOrSkip = Prover9.refute(box) must not(throwA[IOException]).orSkip


  def sequentToString( s: Sequent ) = {
    var ret = ""
    s.antecedent.foreach( formula => ret += formula.toStringSimple + ", ")
    ret += " :- "
    s.succedent.foreach( formula => ret += formula.toStringSimple + ", ")
    ret
  }

  def printStats( p: LKProof ) = {
    val stats = getStatistics( p )
    print("unary: " + stats.unary + "\n")
    print("binary: " + stats.binary + "\n")
    print("cuts: " + stats.cuts + "\n")
  }

  "The system" should {
    "parse, transform to LKsk, and extract the clause set for the lattice proof" in {
      val proofdb = (new XMLReader(new InputStreamReader(new FileInputStream("target" + separator + "test-classes" + separator + "lattice.xml"))) with XMLProofDatabaseParser).getProofDatabase()
      proofdb.proofs.size must beEqualTo(1)
      val proof = proofdb.proofs.head._2
      printStats( proof )

      val proof_sk = LKtoLKskc( proof )
      val s = StructCreators.extract( proof_sk )
/*
      val cs = StandardClauseSet.transformStructToClauseSet( s )
      val dcs = deleteTautologies( cs )
      val css = setNormalize( dcs )
      val cs_path = "target" + separator + "test-classes" + separator + "lattice-cs.xml"
      saveXML( Nil, Pair("cs", cs)::Pair("dcs", dcs)::Pair("css", (css.toList))::Nil, cs_path )
      (new java.io.File( cs_path ) ).exists() must beEqualTo( true )
*/
    }

    "parse and skolemize the lattice proof" in {
      checkForProverOrSkip

      val proofdb = (new XMLReader(new InputStreamReader(new FileInputStream("target" + separator + "test-classes" + separator + "lattice.xml"))) with XMLProofDatabaseParser).getProofDatabase()
      proofdb.proofs.size must beEqualTo(1)
      val proof = proofdb.proofs.head._2

      val proof_sk = skolemize( proof )
      val s = StructCreators.extract( proof_sk )

      val prf = deleteTautologies(proofProfile(s, proof_sk).map( _.toFSequent ))

      val tptp_prf = TPTPFOLExporter.tptp_problem( prf )
      val writer_prf = new java.io.FileWriter("target" + separator + "test-classes" + separator + "lattice-prf.tptp")
      writer_prf.write( tptp_prf )
      writer_prf.flush



      val cs = deleteTautologies( StandardClauseSet.transformStructToClauseSet( s ).map( _.toFSequent ) )
      val tptp = TPTPFOLExporter.tptp_problem( cs )
      val writer = new java.io.FileWriter("target" + separator + "test-classes" + separator + "lattice-cs.tptp")
      writer.write( tptp )
      writer.flush

      val prf_cs_intersect = prf.filter(seq => cs.contains(seq))


      // refute it with prover9
      Prover9.refute( prf ) match {
        case None => "" must beEqualTo("refutation of proof profile failed")
        case Some(_) => true must beEqualTo(true)
      }
      Prover9.refute( cs ) match {
        case None => "" must beEqualTo("refutation of struct cs in tptp format failed")
        case Some(_) => true must beEqualTo(true)
      }

      val projs = Projections( proof_sk )
      val path = "target" + separator + "test-classes" + separator + "lattice-sk.xml"
      saveXML( Pair("lattice-sk", proof_sk) ::
        projs.map( p => p._1 ).toList.zipWithIndex.map( p => Pair( "\\psi_{" + p._2 + "}", p._1 ) ),
        Pair("cs", cs)::Pair("prf",prf)::Pair("cs_prf_intersection", prf_cs_intersect)::Nil, path )
      (new java.io.File( path ) ).exists() must beEqualTo( true )
    }
  }
}
