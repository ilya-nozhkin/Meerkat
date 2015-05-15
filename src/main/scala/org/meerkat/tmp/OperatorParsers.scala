package org.meerkat.tmp

import org.meerkat.sppf.NonPackedNode
import org.meerkat.sppf.SPPFLookup
import org.meerkat.util.Input
import java.util.HashMap

object OperatorParsers {
  
  import AbstractOperatorParsers._
  
  trait HasAlternationOp extends AbstractOperatorParser[NonPackedNode] {
    def | (p: Sequence): Alternation = alternation(this, p)   
    def | (p: Nonterminal): Alternation = alternation(this, p)

    def | (p: Parsers.Sequence): Alternation = alternation(this, p)
    def | (p: Parsers.Symbol): Alternation = alternation(this, p)
    
    def |> (p: Sequence): Alternation = greater(this, p)
    def |> (p: Nonterminal): Alternation = greater(this, p)
    
  }
  
  trait Sequence extends HasAlternationOp { 
    override def isSequence = true
    
    private var lvl: Int = -1
    override def level: Int = lvl
    
    private var group: Group = _
    override def pass(group: Group) = {
      if (isLeftRec || isRightRec) {
        this.group = group
        this.lvl =  this.group.get(this.assoc)
      }
    }
    
    protected def condition: Prec => Boolean = {
      if (isLeftRec && isRightRec) {
        if (group.canClimb(level)) {
          return prec => group.maximum >= prec._1 && group.maximum >= prec._2  
        } else {
          assoc match {
            case Assoc.UNDEFINED => return prec => group.maximum >= prec._1 && group.maximum >= prec._2
            case Assoc.LEFT      => return prec => group.maximum >= prec._1 && group.maximum >= prec._2 && level != prec._2
            case Assoc.RIGHT     => return prec => group.maximum >= prec._1 && group.maximum >= prec._2 && level != prec._1
            case _ =>
          }
        }
      } else if (isLeftRec) {
        if (group.canClimb(level)) {
          return prec => group.maximum >= prec._2  
        } else {
          assoc match {
            case Assoc.UNDEFINED => return prec => group.maximum >= prec._2
            case Assoc.LEFT      => return prec => group.maximum >= prec._2 && level != prec._2
            case _ =>
          }
        }
      } else if (isRightRec) {
        if (group.canClimb(level)) {
          return prec => group.maximum >= prec._1  
        } else {
          assoc match {
            case Assoc.UNDEFINED => return prec => group.maximum >= prec._1
            case Assoc.RIGHT     => return prec => group.maximum >= prec._1 && level != prec._1
            case _ =>
          }
        }
      }
      return null
    }
    
    protected def arguments: (Prec, Prec) = {
      if (isLeftRec || isRightRec) {
        if (group.canClimb(level)) {
          assoc match {
            case Assoc.UNDEFINED => 
              return (if (isLeftRec) (level, level) else $, 
                      if (isRightRec) (level, level) else $)
            case Assoc.LEFT => 
              return (if (isLeftRec) (level, level) else $, if (isRightRec) (level + 1, level + 1) else $)
            case Assoc.RIGHT => 
              return (if (isLeftRec) (level + 1, level + 1) else $, if (isRightRec) (level, level) else $)
          }
        } else {
          return (if (isLeftRec) (level, level) else $, if (isRightRec) (level, level) else $)
        }
      }
      return ($,$)
    }
    
  }
  
  trait Alternation extends HasAlternationOp { override def isAlternation = true }
  
  def alternation(p1: AbstractOperatorParser[NonPackedNode], p2: AbstractOperatorParser[NonPackedNode]) 
    = new Alternation { import Parsers._
        def apply(prec: Prec): Parsers.Alternation = AbstractCPSParsers.AbstractParser.alt(p1(prec), p2(prec))
        
        override def pass(head: AbstractOperatorParser[Any]) = { 
          if (!p1.isNonterminal) p1 pass head
          if (!p2.isNonterminal) p2 pass head 
        }
    
        override def pass(group: Group) = {
          if (!p1.isNonterminal) p1 pass group
          if (!p2.isNonterminal) p2 pass group
        }
      }
  
  def alternation(p1: AbstractOperatorParser[NonPackedNode], p2: AbstractCPSParsers.AbstractParser[NonPackedNode]) 
    = new Alternation { import Parsers._
        def apply(prec: Prec): Parsers.Alternation = AbstractCPSParsers.AbstractParser.alt(p1(prec), p2)
        
        override def pass(head: AbstractOperatorParser[Any]) = { if (!p1.isNonterminal) p1 pass head }
        override def pass(group: Group) = { if (!p1.isNonterminal) p1 pass group }
      }
  
  def alternation(p1: AbstractCPSParsers.AbstractParser[NonPackedNode], p2: AbstractOperatorParser[NonPackedNode]) 
    = new Alternation { import Parsers._
        def apply(prec: Prec): Parsers.Alternation = AbstractCPSParsers.AbstractParser.alt(p1, p2(prec))
        override def pass(head: AbstractOperatorParser[Any]) = { if (!p2.isNonterminal) p2 pass head }
        override def pass(group: Group) = { if (!p2.isNonterminal) p2 pass group }
      }
  
  def greater(p1: AbstractOperatorParser[NonPackedNode], p2: AbstractOperatorParser[NonPackedNode]) 
    = new Alternation { import Parsers._
        def apply(prec: Prec): Parsers.Alternation = AbstractCPSParsers.AbstractParser.alt(p1(prec), p2(prec))
        
        override def pass(head: AbstractOperatorParser[Any]) = { 
          if (!p1.isNonterminal) p1 pass head
          if (!p2.isNonterminal) p2 pass head 
        }
    
        override def pass(group: Group) = {
          if (!p1.isNonterminal) p1 pass group.startNew(Assoc.UNDEFINED)
          if (!p2.isNonterminal) p2 pass group
        }
      }
    
  trait Nonterminal extends HasAlternationOp {
    override def isNonterminal = true
    
    def ~ (p: Parsers.Symbol): Postfix = Postfix(this, p)
    def ~ (p: Nonterminal): Infix = Infix(this, p)
  }
  
  case class Postfix(p1: AbstractOperatorParser[NonPackedNode], p2: AbstractCPSParsers.AbstractParser[NonPackedNode]) extends Sequence { import Parsers._
    
    lazy val cond = condition
    
    def apply(prec: Prec): Parsers.Sequence = {
      if (cond == null || cond(prec)) {
        AbstractCPSParsers.AbstractParser.seq(p1(prec), p2)
      }
      ???
    }
    
    protected var rec: Rec.Rec = Rec.UNDEFINED
    override def isLeftRec = rec == Rec.LEFT
    
    override def pass(head: AbstractOperatorParser[Any]) = {
      val isLeftRec = if (p1 isNonterminal) p1 == head
                      else { p1 pass head; p1 isLeftRec }
      if (isLeftRec) rec = Rec.LEFT
    }
  
    def ~ (p: Parsers.Symbol): Postfix = Postfix(this, p)
    def ~ (p: OperatorParsers.Nonterminal): Infix = Infix(this, p)
  }
  
  case class Prefix(p1: AbstractCPSParsers.AbstractParser[NonPackedNode], p2: Nonterminal) extends Sequence { import Parsers._
    
    def apply(prec: Prec): Parsers.Sequence = AbstractCPSParsers.AbstractParser.seq(p1, p2(prec))
    
    protected var rec: Rec.Rec = Rec.UNDEFINED
    override def isRightRec = rec == Rec.RIGHT
    
    override def pass(head: AbstractOperatorParser[Any]) = if (p2 == head) rec = Rec.RIGHT
        
    def ~ (p: Parsers.Symbol): Parsers.Sequence = AbstractCPSParsers.AbstractParser.seq(Prefix.this($), p)
    def ~ (p: OperatorParsers.Nonterminal): Prefix = Prefix(Prefix.this($), p)
  }
  
  case class Infix(p1: AbstractOperatorParser[NonPackedNode], p2: Nonterminal) extends Sequence { import Parsers._
    
    def apply(prec: Prec): Parsers.Sequence = AbstractCPSParsers.AbstractParser.seq(p1(prec), p2(prec))
    
    protected var rec: Rec.Rec = Rec.UNDEFINED
    
    override def isLeftRec = rec == Rec.LEFT || rec == Rec.BOTH
    override def isRightRec = rec == Rec.RIGHT || rec == Rec.BOTH
    
    override def pass(head: AbstractOperatorParser[Any]) = {
      val isLeftRec = if (p1 isNonterminal) p1 == head
                      else { p1 pass head; p1 isLeftRec }
      
      if (isLeftRec) {
        if (p2 == head) rec = Rec.BOTH
        else rec = Rec.LEFT
      } else {
        if (p2 == head) rec = Rec.RIGHT
      }
    }
    
    def ~ (p: Parsers.Symbol): Postfix = Postfix(Postfix(p1, p2($)), p)
    def ~ (p: OperatorParsers.Nonterminal): Infix = Infix(Postfix(p1, p2($)), p)
  }
   
  def left(p: Infix): Infix = new Infix(p.p1, p.p2) { override def assoc = Assoc.LEFT }
  def right(p: Infix): Infix = new Infix(p.p1, p.p2) { override def assoc = Assoc.RIGHT }
  
  def left(p: Alternation): Alternation = ???
  def right(p: Alternation): Alternation = ???
  
  implicit class ParsersOp(p: AbstractCPSParsers.AbstractParser[NonPackedNode]) {
    def ~ (q: OperatorParsers.Nonterminal): Prefix = Prefix(p, q)
    
    def | (q: Sequence): Alternation = alternation(p, q)
    
    def | (q: Nonterminal): Alternation = alternation(p, q)
  }
  
  def op_nt(name: String)(p: => AbstractOperatorParser[NonPackedNode]): Nonterminal
    = new Nonterminal {
        import Parsers._
        val table: java.util.Map[Prec, Parsers.Nonterminal] = new HashMap()
        
        def apply(prec: Prec) 
          = if (table.containsKey(prec)) table.get(prec) 
            else { 
              val nt = AbstractCPSParsers.memoize(p(prec), name + s"$prec")
              table.put(prec, nt)
              nt
            }
        
      }
  
}