import java.io._
import scala.io.Source
import scala.collection.immutable.HashMap
import org.slf4j.LoggerFactory

import at.logic.utils.executionModels.timeout._
import at.logic.calculi.expansionTrees.{ExpansionTree,removeFromExpansionSequent,quantRulesNumber => quantRulesNumberET}
import at.logic.algorithms.cutIntroduction._
import at.logic.provers.prover9._
import at.logic.provers._

/**********
 * test script for the cut-introduction algorithm on output proofs from prover9,
 * veriT and example proof sequences.
 * usage example from CLI:
 *
 * scala> :load ../testing/testCutIntro.scala
 *
 * scala> testCutIntro.findNonTrivialTSTPExamples( "../testing/prover9-TSTP/", 60 )
 *
 * test the tests by
 * scala> testCutIntro.compressTSTP( "../testing/resultsCutIntro/tstp_minitest.csv", 60, false, false )
 * scala> testCutIntro.compressVeriT( "../testing/veriT-SMT-LIB/QF_UF/eq_diamond/", 60, false )
 * scala> testCutIntro.compressProofSequences( 60, false, false )
 *
 * run the tests by
 * scala> testCutIntro.compressAll
 * see compressAll for how to call specific tests
 **********/

val TestCutIntroLogger = LoggerFactory.getLogger("TestCutIntroLogger$")
val CutIntroDataLogger = LoggerFactory.getLogger("CutIntroDataLogger$")

// for testCutIntro.compressProofSequences
:load ../examples/ProofSequences.scala

object testCutIntro {

  def compressAll() = {
    // note: the "now starting" - lines are logged in the data file so that it can be separated into the particular test runs later
    CutIntroDataLogger.trace( "---------- now starting ProofSeq/cut-intro/DefaultProver" )
    compressProofSequences( 60, false, false )
    CutIntroDataLogger.trace( "---------- now starting ProofSeq/generalized cut-intro/DefaultProver" )
    compressProofSequences( 60, true, false )
    CutIntroDataLogger.trace( "---------- now starting ProofSeq/cut-intro/Prover9Prover" )
    compressProofSequences( 60, false, true )
    CutIntroDataLogger.trace( "---------- now starting ProofSeq/generalized cut-intro/Prover9Prover" )
    compressProofSequences( 60, true, true )

    CutIntroDataLogger.trace( "---------- now starting TSTP-Prover9/cut-intro/DefaultProver" )
    compressTSTP( "../testing/resultsCutIntro/tstp_non_trivial_termset.csv", 60, false, false )
    CutIntroDataLogger.trace( "---------- now starting TSTP-Prover9/generalized cut-intro/DefaultProver" )
    compressTSTP( "../testing/resultsCutIntro/tstp_non_trivial_termset.csv", 60, true, false )
    CutIntroDataLogger.trace( "---------- now starting TSTP-Prover9/cut-intro/Prover9Prover" )
    compressTSTP( "../testing/resultsCutIntro/tstp_non_trivial_termset.csv", 60, false, true )
    CutIntroDataLogger.trace( "---------- now starting TSTP-Prover9/generalized cut-intro/Prover9Prover" )
    compressTSTP( "../testing/resultsCutIntro/tstp_non_trivial_termset.csv", 60, true, true )

    CutIntroDataLogger.trace( "---------- now starting SMT-LIB-QF_UF-veriT/cut-intro/DefaultProver" )
    compressVeriT( "../testing/veriT-SMT-LIB/QF_UF/", 60, false )
    CutIntroDataLogger.trace( "---------- now starting SMT-LIB-QF_UF-veriT/generalized cut-intro/DefaultProver" )
    compressVeriT( "../testing/veriT-SMT-LIB/QF_UF/", 60, true )
  }

  var total = 0
  var error_parser = 0
  var error_parser_OOM = 0
  var error_parser_SO = 0
  var error_parser_other = 0
  var error_term_extraction = 0
  var error_cut_intro = 0
  var out_of_memory = 0
  var stack_overflow = 0
  var error_rule_count = 0
  var finished = 0
  // Hashmap containing proofs with non-trivial termsets
  var termsets = HashMap[String, FlatTermSet]()
  // File name -> q-rules before cut, rules before cut, q-rules after
  // cut, rules after cut
  var rulesInfo = HashMap[String, (Int, Int, Int, Int)]()

  def apply() = {
    println("Usage: for example calls please see the implementation of testCutIntro.compressAll()")
  }

  /************** finding non-trival prover9-TSTP proofs **********************/

  def findNonTrivialTSTPExamples ( str : String, timeout : Int ) = {
    
    TestCutIntroLogger.trace("================ Finding TSTP non-trivial examples ===============")

    nonTrivialTermset(str, timeout)
    val file = new File("../testing/resultsCutIntro/tstp_non_trivial_termset.csv")
    val summary = new File("../testing/resultsCutIntro/tstp_non_trivial_summary.txt")
    file.createNewFile()
    summary.createNewFile()
    val fw = new FileWriter(file.getAbsoluteFile)
    val bw = new BufferedWriter(fw)
    val fw_s = new FileWriter(summary.getAbsoluteFile)
    val bw_s = new BufferedWriter(fw_s)

    var instance_per_formula = 0.0
    var ts_size = 0
    val data = termsets.foldLeft("") {
      case (acc, (k, v)) =>
        val tssize = v.termset.size
        val n_functions = v.formulaFunction.size
        instance_per_formula += tssize.toFloat/n_functions.toFloat
        ts_size += tssize
        k + "," + n_functions + "," + tssize + "\n" + acc
    }

    val avg_inst_per_form = instance_per_formula/termsets.size
    val avg_ts_size = ts_size.toFloat/termsets.size.toFloat

    bw.write(data)
    bw.close()

    bw_s.write("Total number of proofs: " + total + "\n")
    bw_s.write("Total number of proofs with non-trivial termsets: " + termsets.size + "\n")
    bw_s.write("Time limit exceeded or exception during parsing: " + error_parser + "\n")
    bw_s.write("Time limit exceeded or exception during terms extraction: " + error_term_extraction + "\n")
    bw_s.write("Average instances per quantified formula: " + avg_inst_per_form + "\n")
    bw_s.write("Average termset size: " + avg_ts_size + "\n")
    bw_s.close()

  }
  def nonTrivialTermset(str : String, timeout : Int) : Unit = {
    val file = new File(str)
    if (file.isDirectory) {
      val children = file.listFiles
      children.foreach(f => nonTrivialTermset(f.getAbsolutePath, timeout))
    }
    else if (file.getName.endsWith(".out")) {
      total += 1
      TestCutIntroLogger.trace("\nFILE: " + file.getAbsolutePath)
      runWithTimeout(timeout * 1000){ loadProver9LKProof(file.getAbsolutePath) } match {
        case Some(p) => 
          runWithTimeout(timeout * 1000){ 
            val ts = extractTerms(p)
            val tssize = ts.termset.size
            val n_functions = ts.formulaFunction.size
            if(tssize > n_functions) {
              termsets += (file.getAbsolutePath -> ts)
              tssize
            }
            else 0
            //cutIntro(p) 
          } match {
            case Some(n) =>
              if(n > 0) {
                TestCutIntroLogger.trace("File: " + file.getAbsolutePath + " has term-set of size " + n)
              }
            case None => error_term_extraction += 1
          }
        case None => error_parser += 1
      }
    }       
  }

  // Compress the prover9-TSTP proofs whose names are in the csv-file passed as parameter str
  def compressTSTP( str: String, timeout: Int, useGenCutIntro: Boolean, useProver9Prover: Boolean ) = {
    
    TestCutIntroLogger.trace("================ Compressing non-trivial TSTP examples ===============")
    
    var number = 0

    // Process each file
    val lines = Source.fromFile(str).getLines().toList
    lines.foreach{ case l =>
      number += 1
      val data = l.split(",")
      TestCutIntroLogger.trace("Processing proof number: " + number)
      compressTSTPProof( data(0), timeout, useGenCutIntro, useProver9Prover )
    }
  }

  /// compress the prover9-TSTP proof found in file fn
  def compressTSTPProof( fn: String, timeout: Int, useGenCutIntro: Boolean, useProver9Prover: Boolean ) = {
    var log_ptime_ninfcf_nqinfcf = ""
    var parsing_status = "ok"
    var cutintro_status = "ok"
    var cutintro_logline = ""

    TestCutIntroLogger.trace( "FILE: " + fn )

    val file = new File( fn )
    if (file.getName.endsWith(".out")) {
      val expproof = try { withTimeout( timeout * 1000 ) {
        val t0 = System.currentTimeMillis
        val p = loadProver9LKProof( file.getAbsolutePath )
        val ep = extractExpansionTrees( p )
        val t1 = System.currentTimeMillis
        
        log_ptime_ninfcf_nqinfcf = "," + (t1 - t0) + "," + rulesNumber(p) + "," + quantRulesNumber(p) // log ptime, #infcf, #qinfcf

        Some(ep)
      } } catch {
        case e: TimeOutException =>
          TestCutIntroLogger.trace("Parsing: Timeout")
          parsing_status = "parsing_timeout"
          None
        case e: OutOfMemoryError =>
          TestCutIntroLogger.trace("Parsing: OutOfMemory: " + e)
          parsing_status = "parsing_out_of_memory"
          None
        case e: StackOverflowError =>
          TestCutIntroLogger.trace("Parsing: StackOverflow: " + e)
          parsing_status = "parsing_stack_overflow"
          None
        case e: Exception =>
          TestCutIntroLogger.trace("Parsing: Other exception: " + e)
          parsing_status = "parsing_other_exception"
          None
      }

      expproof match {
        case Some(ep) =>
          val prover = if ( useProver9Prover ) new Prover9Prover() else new DefaultProver()
          val r = compressExpansionProof( ep, prover, useGenCutIntro, timeout )
          cutintro_status = r._1
          cutintro_logline = r._2
        case None => ()
      }

      if ( parsing_status == "ok" ) {
        if ( cutintro_status == "ok" ) {
          CutIntroDataLogger.trace( fn + ",ok" + log_ptime_ninfcf_nqinfcf + cutintro_logline )
        }
        else {
          CutIntroDataLogger.trace( fn + "," + cutintro_status )
        }
      }
      else {
        CutIntroDataLogger.trace( fn + "," + parsing_status )
      }
    }
  }

  /****************************** VeriT SMT-LIB ******************************/

  /// compress all veriT-proof found recursively in the directory str
  def compressVeriT( str: String, timeout: Int, useGenCutIntro: Boolean ) = {
    TestCutIntroLogger.trace("================ Compressing VeriT proofs ===============")
    recCompressVeriT( str, timeout, useGenCutIntro )
  }

  def recCompressVeriT( str: String, timeout: Int, useGenCutIntro: Boolean ): Unit = {
    val file = new File( str )
    if (file.isDirectory) {
      val children = file.listFiles
      children.foreach( f => recCompressVeriT( f.getPath, timeout, useGenCutIntro ))
    }
    else if (file.getName.endsWith(".proof_flat")) {
      compressVeriTProof( file.getPath, timeout, useGenCutIntro )
    }
  }

  /// compress the veriT-proof str
  def compressVeriTProof( str: String, timeout: Int, useGenCutIntro: Boolean ) {
    var parsing_status = "ok"
    var cutintro_status = "ok"
    var cutintro_logline = ""
    var log_ptime_ninfcf_nqinfcf = ""

    TestCutIntroLogger.trace("FILE: " + str)

    val expproof = try { withTimeout( timeout * 1000 ) {
      val t0 = System.currentTimeMillis
      val ep = loadVeriTProof( str )
      val t1 = System.currentTimeMillis

      log_ptime_ninfcf_nqinfcf = "," + (t1 - t0) + ",n/a,n/a" // log ptime, #infcf, #qinfcf

      Some(ep)
    } } catch {
      case e: TimeOutException =>
        TestCutIntroLogger.trace("Parsing: Timeout")
        parsing_status = "parsing_timeout"
        None
      case e: OutOfMemoryError =>
        TestCutIntroLogger.trace("Parsing: OutOfMemory: " + e)
        parsing_status = "parsing_out_of_memory"
        None
      case e: StackOverflowError => 
        TestCutIntroLogger.trace("Parsing: StackOverflow: " + e)
        parsing_status = "parsing_stack_overflow"
        None
      case e: Exception =>
        TestCutIntroLogger.trace("Parsing: Other exception: " + e)
        parsing_status = "parsing_other_exception"
        None
    }

    expproof match {
      case Some(ep) =>
        val r = compressExpansionProof( ep, new DefaultProver(), useGenCutIntro, timeout )
        cutintro_status = r._1
        cutintro_logline = r._2
      case None => ()
    }

    if ( parsing_status == "ok" ) {
      if ( cutintro_status == "ok" ) {
        CutIntroDataLogger.trace( str + ",ok" + log_ptime_ninfcf_nqinfcf + cutintro_logline )
      }
      else {
        CutIntroDataLogger.trace( str + "," + cutintro_status )
      }
    }
    else {
      CutIntroDataLogger.trace( str + "," + parsing_status )
    }
  }

  /***************************** Proof Sequences ******************************/

  def compressProofSequences( timeout: Int, useGenCutIntro: Boolean, moduloEq: Boolean ) {
    TestCutIntroLogger.trace("================ Compressing proof sequences " + ( if (moduloEq) "(modulo equality)" else "(modulo propositional logic)" ) + "===============")

    for ( i <- 1 to 12 ) {
      val pn = "LinearExampleProof(" + i + ")"
      TestCutIntroLogger.trace( "PROOF: " + pn )
      compressLKProof( pn, LinearExampleProof( i ), timeout, useGenCutIntro, moduloEq )
    }

    for ( i <- 1 to 13 ) {
      val pn = "SquareDiagonalExampleProof(" + i + ")"
      TestCutIntroLogger.trace( "PROOF: " + pn )
      compressLKProof( pn, SquareDiagonalExampleProof( i ), timeout, useGenCutIntro, moduloEq )
    }

    for ( i <- 1 to 8 ) {
      val pn = "SquareEdgesExampleProof(" + i + ")"
      TestCutIntroLogger.trace( "PROOF: " + pn )
      compressLKProof( pn, SquareEdgesExampleProof( i ), timeout, useGenCutIntro, moduloEq )
    }

    for ( i <- 1 to 12 ) {
      val pn = "SquareEdges2DimExampleProof(" + i + ")"
      TestCutIntroLogger.trace( "PROOF: " + pn )
      compressLKProof( pn, SquareEdges2DimExampleProof( i ), timeout, useGenCutIntro, moduloEq )
    }

    for ( i <- 1 to 10 ) {
      val pn = "LinearEqExampleProof(" + i + ")"
      TestCutIntroLogger.trace( "PROOF: " + pn )
      compressLKProof( pn, LinearEqExampleProof( i ), timeout, useGenCutIntro, moduloEq )
    }

    for ( i <- 1 to 5 ) {
      val pn = "SumOfOnesF2ExampleProof(" + i + ")"
      TestCutIntroLogger.trace( "PROOF: " + pn )
      compressLKProof( pn, SumOfOnesF2ExampleProof( i ), timeout, useGenCutIntro, moduloEq )
    }

    for ( i <- 1 to 5 ) {
      val pn = "SumOfOnesFExampleProof(" + i + ")"
      TestCutIntroLogger.trace( "PROOF: " + pn )
      compressLKProof( pn, SumOfOnesFExampleProof( i ), timeout, useGenCutIntro, moduloEq )
    }

    for ( i <- 1 to 6 ) {
      val pn = "SumOfOnesExampleProof(" + i + ")"
      TestCutIntroLogger.trace( "PROOF: " + pn )
      compressLKProof( pn, SumOfOnesExampleProof( i ), timeout, useGenCutIntro, moduloEq )
    }

    for ( i <- 1 to 2 ) {
      val pn = "UniformAssociativity3ExampleProof(" + i + ")"
      TestCutIntroLogger.trace( "PROOF: " + pn )
      compressLKProof( pn, UniformAssociativity3ExampleProof( i ), timeout, useGenCutIntro, moduloEq )
    }
  }

  def compressLKProof( name: String, p: LKProof, timeout: Int, useGenCutIntro: Boolean, moduloEq: Boolean ) = {
    val r = if ( moduloEq )
      compressExpansionProof( removeEqAxioms( extractExpansionTrees( p )), new Prover9Prover(), useGenCutIntro, timeout )
    else
      compressExpansionProof( extractExpansionTrees( p ), new DefaultProver(), useGenCutIntro, timeout )

    val cutintro_status = r._1
    val cutintro_logline = r._2

    if ( cutintro_status == "ok" ) {
      CutIntroDataLogger.trace( name + ",ok,n/a," + rulesNumber( p ) + "," + quantRulesNumber( p ) + cutintro_logline )
    }
    else {
      CutIntroDataLogger.trace( name + "," + cutintro_status )
    }
  }

  def removeEqAxioms( eseq: (Seq[ExpansionTree], Seq[ExpansionTree]) ) = {
   // removes all equality axioms that appear in ../examples/ProofSequences.scala
   val R = parse.fol( "Forall x =(x,x)" )
   val S = parse.fol( "Forall x Forall y Imp =(x,y) =(y,x)" )
   val T = parse.fol( "Forall x Forall y Forall z Imp And =(x,y) =(y,z) =(x,z)" )
   val Tprime = parse.fol( "Forall x Forall y Forall z Imp =(x,y) Imp =(y,z) =(x,z)" )
   val CSuc = parse.fol( "Forall x Forall y Imp =(x,y) =(s(x),s(y))" )
   val CPlus = parse.fol( "Forall x Forall y Forall u Forall v Imp =(x,y) Imp =(u,v) =(+(x,u),+(y,v))" )
   val CPlusL = parse.fol( "Forall x Forall y Forall z Imp =(y,z) =(+(y,x),+(z,x))" ) // congruence plus left
   val CgR = parse.fol( "Forall x Forall y Forall z Imp =(y,z) =(g(x,y),g(x,z))" ) // congruence of g on the right
   val CMultR = parse.fol( "Forall x Forall y Forall z Imp =(x,y) =(*(z,x),*(z,y))" ) // congruence of mult right

   val eqaxioms = new FSequent( R::S::T::Tprime::CSuc::CPlus::CPlusL::CgR::CMultR::Nil, Nil )
   
   removeFromExpansionSequent( eseq, eqaxioms )
  }


  /******************* auxiliary stuff for all test-suites ********************/

  /**
   * Runs experimental cut-introduction on the expansion proof ep. This is the
   * only call to cut-introduction in this test-script.
   *
   * @return ( status, logline )
   **/
  def compressExpansionProof( ep: (Seq[ExpansionTree],Seq[ExpansionTree]), prover: Prover, useGenCutIntro: Boolean, timeout: Int ) : ( String, String ) = {
    var status = "ok"
    var logline = "," + quantRulesNumberET( ep )

    try {
      val r = if ( useGenCutIntro )
        Generalized.CutIntroduction.applyStat( ep, new Generalized.Deltas.UnboundedVariableDelta(), prover, timeout )
      else
        CutIntroduction.applyExp( ep, prover, timeout )
      status = r._2
      logline += r._3

      if ( status.endsWith( "timeout" ) ) TestCutIntroLogger.trace( "Timeout" )

    } catch {
      case e: OutOfMemoryError =>
        TestCutIntroLogger.trace("OutOfMemory: " + e)
        status = "cutintro_out_of_memory"
      case e: StackOverflowError =>
        TestCutIntroLogger.trace("StackOverflow: " + e)
        status = "cutintro_stack_overflow"
      case e: CutIntroUncompressibleException =>
        TestCutIntroLogger.trace("Input Uncompressible: " + e)
        status = "cutintro_uncompressible"
      case e: CutIntroEHSUnprovableException =>
        TestCutIntroLogger.trace("Extended Herbrand Sequent unprovable: " + e)
        status = "cutintro_ehs_unprovable"
      case e: Exception =>
        TestCutIntroLogger.trace("Other exception: " + e)
        status = "cutintro_other_exception"
    }

    ( status, logline )
  }

  /**
   * Run f
   *
   * If f terminates within to milliseconds return its result, if it throws an
   * exception or does not terminate within to milliseconds, return None.
   **/
  private def runWithTimeout[T >: Nothing]( to: Long )( f: => T ) : Option[T] = {
    var output:Option[T] = None

    val r = new Runnable { def run() { output = Some( f ) } }
    val t = new Thread( r )
    t.start()
    t.join( to )
    if ( t.isAlive() ) {
      TestCutIntroLogger.trace("TIMEOUT.")
      t.stop()
    }

    output
  }



}

