package at.logic.parsing.language.tptp

import at.logic.language.hol._
import at.logic.language.lambda.types._
import at.logic.calculi.lk.base.{FSequent, LKProof}
import at.logic.language.hol.logicSymbols.LogicalSymbolA

/**
 * Created by marty on 12/10/13.
 */
object TPTPHOLExporter extends TPTPHOLExporter
class TPTPHOLExporter {
  def apply(l : List[FSequent]) : String = {
    require(l.nonEmpty, "Cannot export an empty sequent list!")
    val (vs, vnames, cs, cnames) = createNamesFromSequent(l)
    var index = 0
    val vdecs_ = for(v <- vs) yield {
      index = index +1
      thf_type_dec(index, v, vnames) + "\n"
    }

    val vdecs = vdecs_.foldLeft("")(_ ++ _)


    val cdecs_ = for (c <- cs) yield {
      index = index +1
      thf_type_dec(index, c, cnames) + "\n"
    }
    val cdecs = cdecs_.foldLeft("")(_ ++ _)

//    val sdecs_ = for (fs <- l) yield {
//      index = index +1
//      thf_sequent_dec(index, fs, vnames, cnames) + "\n"
//    }
//    val sdecs = sdecs_.foldLeft("")(_ ++ _)
    val negClauses = Neg(conj(l.map(closedFormula)))
    index = index +1
    val sdecs = thf_formula_dec(index, negClauses, vnames, cnames)


    //"% variable type declarations\n" + vdecs +
      "% constant type declarations\n" + cdecs +
      "% sequents\n" + sdecs
  }


  type NameMap = Map[HOLVar, String]
  val emptyNameMap = Map[HOLVar, String]()
  type CNameMap = Map[HOLConst, String]
  val emptyCNameMap = Map[HOLConst, String]()

  def createFormula(f:HOLExpression, map : Map[HOLVar,String]) = f match {
    case HOLVar(_,_) => map(f.asInstanceOf[HOLVar])
  }

  def createNamesFromSequent(l: List[FSequent]) : (List[HOLVar], NameMap, List[HOLConst], CNameMap) = {
    val vs = l.foldLeft(Set[HOLVar]())((set, fs) =>  getVars(fs.toFormula, set)  ).toList
    val cs = l.foldLeft(Set[HOLConst]())((set, fs) =>  getConsts(fs.toFormula, set)  ).toList
    (vs, createNamesFromVar(vs), cs, createNamesFromConst(cs))
  }

  def createNamesFromVar(l : List[HOLVar]) : NameMap = l.foldLeft(emptyNameMap)( (map, v) => {
    if (map contains v)
      map
    else {
      val name =  mkVarName(v.name.toString(), map)
      map + ((v,name))
    }
  }
  )

  def closedFormula(fs : FSequent) : HOLFormula = {
    val f = fs.toFormula
    freeVariables(f).foldRight(f)( (v,g) => AllVar(v,g) )
  }

  def conj(l:List[HOLFormula]) : HOLFormula = l match {
    case Nil =>
      throw new Exception("Empty sequent list given to export!")
    case x::Nil =>
      x
    case x::xs =>
      And(x, conj(xs))
  }

  def createNamesFromConst(l : List[HOLConst]) : CNameMap = l.foldLeft(emptyCNameMap)( (map, v) => {
    if (map contains v)
      map
    else {
      val name = mkConstName(v.name.toString(), map)
      map + ((v,name))
    }
  }
  )

  def thf_sequent_dec(i:Int, f:FSequent, vmap : NameMap, cmap : CNameMap) = {
    "thf("+i+", plain, ["+
      (f.antecedent.map(f => thf_formula(f,vmap,cmap)).mkString(",")) +
     "] --> [" +
      (f.succedent.map(f => thf_formula(f,vmap,cmap)).mkString(",")) +
    "] )."
  }

  def thf_formula_dec(i:Int, f:HOLFormula, vmap : NameMap, cmap : CNameMap) = {
    "thf("+i+", plain, "+ thf_formula(f,vmap,cmap) +" )."
  }


  def thf_formula(f:HOLExpression, vmap : NameMap, cmap : CNameMap) : String = {
    f match {
      case Neg(x) => " ~("+thf_formula(x, vmap, cmap) +")"
      case And(x,y) => "("+thf_formula(x, vmap, cmap) +" & " +thf_formula(y, vmap, cmap)+")"
      case Or(x,y) => "("+thf_formula(x, vmap, cmap) +" | " +thf_formula(y, vmap, cmap)+")"
      case Imp(x,y) => "("+thf_formula(x, vmap, cmap) +" => " +thf_formula(y, vmap, cmap)+")"
      case AllVar(x,t) => "!["+ vmap(x) +" : "+getTypeString(x.exptype)+"] : ("+ thf_formula(t,vmap,cmap)+")"
      case ExVar(x,t) => "?["+ vmap(x) +" : "+getTypeString(x.exptype)+"] : ("+ thf_formula(t,vmap,cmap)+")"
      case HOLAbs(x,t) => "^["+ vmap(x) +" : "+getTypeString(x.exptype)+"] : ("+ thf_formula(t,vmap,cmap)+")"
      case HOLApp(s,t) => "(" + thf_formula(s, vmap, cmap) + " @ " +thf_formula(t, vmap,cmap) +")"
      case HOLVar(_,_) => vmap(f.asInstanceOf[HOLVar])
      case HOLConst(_,_) => cmap(f.asInstanceOf[HOLConst])
      case _ => throw new Exception("TPTP export does not support outermost connective of "+f)
    }
  }

  def thf_type_dec(i:Int, v : HOLVar, vmap : NameMap) : String = {
    require(vmap.contains(v), "Did not generate an export name for "+v+"!")
    "thf("+i+", type, "+vmap(v) + ": "+getTypeString(v.exptype) +" )."
  }

  def thf_type_dec(i:Int, c : HOLConst, cmap : CNameMap) : String = {
    require(cmap.contains(c), "Did not generate an export name for "+c+"!")
    "thf("+i+", type, "+cmap(c) + ": "+ getTypeString(c.exptype) +" )."
  }


  def getTypeString(t : TA) : String = getTypeString(t,true)
  def getTypeString(t : TA, outer : Boolean) : String = t match {
    case Ti => "$i"
    case To => "$o"
    case t1 -> t2 if outer => getTypeString(t1, false) + " > " + getTypeString(t2, false)
    case t1 -> t2 => "(" + getTypeString(t1, false) + " > " + getTypeString(t2, false) +")"
    case _ => throw new Exception("TPTP type export for "+t+" not implemented!")
  }

  def mkVarName(str:String, map : Map[HOLVar,String]) = {
    val fstr = str.filter(_.toString.matches("[a-zA-Z0-9]"))
    val prefix = if (fstr.head.isDigit) "X"+fstr
                 else fstr.head.toUpper + fstr.tail
    val values = map.toList.map(_._2)
    if (values contains prefix)
      appendPostfix(prefix,values)
    else
      prefix
  }

  def mkConstName(str:String, map : CNameMap) = {
    val fstr = str.filter(_.toString.matches("[a-zA-Z0-9]"))
    val prefix = if (fstr.head.isDigit) "c"+fstr
    else fstr.head.toLower + fstr.tail
    val values = map.toList.map(_._2)
    if (values contains prefix)
      appendPostfix(prefix,values)
    else
      prefix
  }

  def appendPostfix(str:String, l : List[String]) = {
    var i = 100
    while (l contains (str+i)) {
      i = i+1
    }
    str+i
  }

  def getVars(t:HOLExpression, set : Set[HOLVar]) : Set[HOLVar] = t match {
    case HOLConst(_,_) => set
    case HOLVar(_,_) => set + t.asInstanceOf[HOLVar]
    case HOLApp(s,t) => getVars(s, getVars(t, set))
    case HOLAbs(x,t) => getVars(t, set + x)
  }

  def getConsts(t:HOLExpression, set : Set[HOLConst]) : Set[HOLConst] = t match {
    case HOLConst(_,_) =>
      val c = t.asInstanceOf[HOLConst]
      if (c.sym.isInstanceOf[LogicalSymbolA])
        set
      else
        set + c
    case HOLVar(_,_) => set
    case HOLApp(s,t) => getConsts(s, getConsts(t, set))
    case HOLAbs(x,t) => getConsts(t, set)
  }




}