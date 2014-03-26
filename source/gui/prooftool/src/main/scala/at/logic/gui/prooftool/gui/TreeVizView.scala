package at.logic.gui.prooftool.gui

import scala.swing.{Action, BorderPanel}
import java.awt.event.{ActionEvent, MouseMotionListener}
import at.logic.calculi.treeProofs.TreeProof
import ch.randelshofer.tree._
import at.logic.utils.ds.trees.{BinaryTree, UnaryTree, LeafTree}
import javax.swing.event.ChangeListener
import java.awt.Color
import at.logic.calculi.lk.base.{Sequent, AuxiliaryFormulas, LKProof, PrincipalFormulas}
import at.logic.algorithms.hlk.HybridLatexExporter.{fsequentString, getFormulaString}
import at.logic.calculi.lk.propositionalRules._
import at.logic.calculi.lk.quantificationRules.{ExistsLeftRuleType, ForallRightRuleType, ExistsRightRuleType, ForallLeftRuleType}
import at.logic.calculi.lksk.{ExistsSkLeftRuleType, ForallSkRightRuleType, ExistsSkRightRuleType, ForallSkLeftRuleType}
import at.logic.calculi.lk.equationalRules.{EquationRight2RuleType, EquationRight1RuleType, EquationLeft2RuleType, EquationLeft1RuleType}
import java.beans.PropertyChangeListener
import at.logic.parsing.calculi.xml.{BinaryRuleType, UnaryRuleType, NullaryRuleType}
import at.logic.calculi.lk.base.types.FSequent

/**
 * Created by marty on 3/18/14.
 */

/* Wapper from gapt proofs to TreeViz trees */
case class ProofNode[T](proof : TreeProof[T]) extends TreeNode {
  import collection.convert.wrapAsJava.seqAsJavaList
  val children : java.util.List[TreeNode] = proof match {
    case LeafTree(v) => List[TreeNode]()
    case UnaryTree(v, parent) => List(ProofNode(parent.asInstanceOf[TreeProof[T]]))
    case BinaryTree(v, p1, p2) => List(p1,p2).map(x => ProofNode(x.asInstanceOf[TreeProof[T]]))
  }

  val getAllowsChildren = ! children.isEmpty

}

class ProofNodeInfo[T] extends NodeInfo {
  var root : Option[ProofNode[T]] = None
  var weighter : Option[Weighter] = None
  val colorizer = new ProofColorizer
  private var actions = Map[TreeProof[T], Action]()


  def genShowAction(x: TreeProof[T]) = new Action("Show node in LK Viewer") {
    def apply() = {
      root match {
        case Some(proof) =>
          Main.display("", proof.proof)
          Main.scrollToProof(x)
          //TODO: fix this in some better way

        case None => ()
      }
    }

  }

  def init(root : TreeNode) = root match {
    case p : ProofNode[T] =>
      this.root = Some(p)
      this.weighter = Some(new ProofWeighter())
      this.weighter.get.init(this.root.get)
      this.actions = Map[TreeProof[T], Action]()

    case _ =>
      throw new Exception("ProofNodeInfo only accepts ProofNodes as tree!")
  }

  def getName(path : TreePath2[TreeNode]) = path.getLastPathComponent() match {
    case ProofNode( p : TreeProof[_]) =>
      p.name
  }



  def getColor(path : TreePath2[TreeNode]) = {
    import Rainbow._
    val rule = path.getLastPathComponent().asInstanceOf[ProofNode[T]].proof.rule
    if (rule == CutRuleType) {
      green
    }
    else if (rule == InitialRuleType || rule == WeakeningLeftRuleType || rule == WeakeningRightRuleType || rule == ContractionLeftRuleType || rule == ContractionRightRuleType) {
      Color.LIGHT_GRAY
    }
    else if (rule == AndLeft1RuleType || rule == AndLeft2RuleType || rule == OrRight1RuleType || rule == OrRight2RuleType || rule == ImpRightRuleType) {
      orange
    }
    else if (rule == AndRightRuleType || rule == OrLeftRuleType || rule == ImpLeftRuleType) {
      yellow
    }
    else if (rule == ForallLeftRuleType || rule == ExistsRightRuleType || rule == ForallSkLeftRuleType || rule == ExistsSkRightRuleType) {
      blue
    }
    else if (rule == ForallRightRuleType || rule == ExistsLeftRuleType || rule == ForallSkRightRuleType || rule == ExistsSkLeftRuleType) {
      red
    }
    else if (rule == EquationLeft1RuleType || rule == EquationLeft2RuleType || rule == EquationRight1RuleType || rule == EquationRight2RuleType) {
      violet
    } else{
      Color.MAGENTA
    }
  }

  def getWeight(path : TreePath2[TreeNode]) = {
    1
  }

  def getCumulatedWeight(path : TreePath2[TreeNode]) = {
    path.getLastPathComponent().asInstanceOf[ProofNode[T]].proof.size()
  }

  def getWeightFormatted(path : TreePath2[TreeNode]) = {
    getWeight(path).toString
  }

  val sequentNameCache = collection.mutable.Map[FSequent, String]()
  def getTooltip(path : TreePath2[TreeNode]) = {
    val es = path.getLastPathComponent.asInstanceOf[ProofNode[T]].proof.root
    es match {
      case s : Sequent =>
        val fs = s.toFSequent()
        if (! (sequentNameCache contains fs))
          sequentNameCache(fs) = fsequentString(s.toFSequent, false)
        sequentNameCache(fs)

      case _ => es.toString()
    }

  }

  def getActions(path : TreePath2[TreeNode]) = {
    val node = path.getLastPathComponent().asInstanceOf[ProofNode[T]].proof
    if (! (this.actions contains node))
      this.actions = this.actions + ((node, genShowAction(node)))
    Array(this.actions(node).peer)
  }

  def getImage(path : TreePath2[TreeNode]) = null

  def addChangeListener(l : ChangeListener) = {

  }

  def removeChangeListener(l : ChangeListener) = {

  }

  def getWeighter = this.weighter match {
    case None => null
    case Some(w) => w
  }

  def toggleColorWeighter() = {

  }

  def getColorizer = this.colorizer

}


object Rainbow {
  val red = new Color(255,0,0)
  val orange = new Color(255,128,0)
  val yellow = new Color(255,255,0)
  val green = new Color(0,128,0)
  val blue = new Color(0,0,255)
  val indigo = new Color(75,0,130)
  val violet = new Color(148,0,211)
}

class ProofWeighter[T] extends Weighter {
  var root : Option[ProofNode[T]] = None
  var histogram : Option[Array[Int]] = None

  def init(root: TreeNode) = {
    root match  {
      case p : ProofNode[T] =>
        this.root = Some(p)
      case _ =>
        throw new Exception("Proof Weighter only works for ProofTrees!")
    }


  }

  def getWeight(path: TreePath2[_]) : Float = {
    1.0f
  }

  def getHistogram: Array[Int] = histogram.getOrElse(Array[Int]())

  def getHistogramLabel(index: Int): String = index.toString

  def getMinimumWeightLabel: String = "0"

  def getMaximumWeightLabel: String = "max"

  def getMedianWeight: Float = 1.0f
}

class ProofColorizer extends Colorizer {
  def get(w:Float) = new Color(255*w, 255*w, 255*w)
}

class FormulaBox[V](var inference : TreeProof[V]) extends BorderPanel {
  inference match {
    case a:AuxiliaryFormulas =>
      if (inference.rule == NullaryRuleType) {
      } else if (inference.rule == UnaryRuleType) {
        a.aux
      } else if (inference.rule == BinaryRuleType) {

      } else {

      }

  }
}
