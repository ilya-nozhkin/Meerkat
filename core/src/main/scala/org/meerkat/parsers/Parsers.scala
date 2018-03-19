/*
 * Copyright (c) 2015, Anastasia Izmaylova and Ali Afroozeh, Centrum Wiskunde & Informatica (CWI)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this
 *    list of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 */

package org.meerkat.parsers

import org.meerkat.input.Input
import org.meerkat.sppf.{NonPackedNode, SPPFLookup, Slot, TerminalNode}
import org.meerkat.tree

import scala.util.matching.Regex
import scala.collection.mutable
import org.meerkat.tree.{TerminalSymbol, VertexSymbol}

object Parsers {
  import AbstractCPSParsers._

  implicit def obj1[L, ValA, ValB](implicit vals: ValA |~| ValB) =
    new CanBuildSequence[L,NonPackedNode, NonPackedNode, ValA, ValB] {
      implicit val m1 = obj4; implicit val m2 = obj4

      type T = NonPackedNode; type V = vals.R

      type Sequence = Parsers.Sequence[L]
      def sequence(p: AbstractSequence[L,NonPackedNode]): Sequence = new Sequence {
        def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) = p(input, i, sppfLookup)
        def size                                                      = p.size; def symbol = p.symbol; def ruleType = p.ruleType
        override def reset                                            = p.reset
      }
      def index(a: T): Int                                             = a.rightExtent
      def intermediate(a: T, b: T, p: Slot, sppfLookup: SPPFLookup[L]): T = sppfLookup.getIntermediateNode(p, a, b)

      type SequenceBuilder = Parsers.SequenceBuilder[L,V]
      def builderSeq(f: Slot => Sequence) = new Parsers.SequenceBuilder[L,V] { def apply(slot: Slot) = f(slot) }
    }

  implicit def obj2[L] = new  CanBuildAlternative[L,NonPackedNode] {
    implicit val m                                                          = obj4
    def result(e: NonPackedNode, p: Slot, nt: Head[L], sppfLookup: SPPFLookup[L]) = sppfLookup.getNonterminalNode(nt, p, e)
  }

  implicit def obj3[L, ValA, ValB] = new CanBuildAlternation[L,NonPackedNode, NonPackedNode, ValA, ValB] {

    implicit val m1 = obj4; implicit val m2 = obj4
    implicit val o1 = obj2; implicit val o2 = obj2

    type Alternation = Parsers.Alternation[L]
    def alternation(p: AbstractParser[L,NonPackedNode]): Alternation = new Alternation {
      def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) = p(input, i, sppfLookup)
      def symbol                                                    = p.symbol.asInstanceOf[org.meerkat.tree.Alt]
      override def reset                                            = p.reset
    }
    type AlternationBuilder = Parsers.AlternationBuilder[L,ValB]
    def builderAlt(f: Head[L] => Alternation) = new Parsers.AlternationBuilder[L,ValB] { def apply(head: Head[L]) = f(head) }
  }
  //Возвращает правый предел
  implicit object obj4 extends Memoizable[NonPackedNode] {
    type U = Int
    def value(t: NonPackedNode): Int = t.rightExtent;
  }

  implicit def obj5[L, Val] = new CanBuildNonterminal[L,NonPackedNode, Val] {
    implicit val m = obj4

    type Nonterminal = Parsers.AbstractNonterminal[L,Val]
    def nonterminal(nt: String, p: AbstractParser[L,NonPackedNode]) = new Parsers.AbstractNonterminal[L,Val] {
      def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) = p(input, i, sppfLookup)
      def symbol                                                    = org.meerkat.tree.SimpleNonterminal(nt)
      def name                                                      = nt; override def toString = name
      override def reset                                            = p.reset
    }

    type Symbol = Parsers.Symbol[L, Val]
    def symbol(p: AbstractSymbol[L,NonPackedNode, Val]) = new Parsers.Symbol[L, Val] {
      def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) = p(input, i, sppfLookup)
      def name                                                      = p.name; def symbol = p.symbol
      override def reset                                            = p.reset
    }
  }

  implicit def obj6[L, Val] = new CanBuildEBNF[L, NonPackedNode, Val] {
    implicit val m = obj4

    type T       = NonPackedNode
    type Regular = AbstractNonterminal[L,Val]
    type Group   = AbstractNonterminal[L,Val]

    def regular(sym: org.meerkat.tree.NonterminalSymbol, p: AbstractParser[L,NonPackedNode]): Regular =
      new AbstractNonterminal[L,Val] {
        def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) = p(input, i, sppfLookup)
        def name                                                      = symbol.toString; def symbol = sym
        override def toString                                         = name
        override def reset                                            = p.reset
      }
    def group(p: AbstractParser[L,NonPackedNode]): Group = new AbstractNonterminal[L,Val] {
      def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) = p(input, i, sppfLookup)
      def name                                                      = symbol.toString; def symbol = org.meerkat.tree.Group(p.symbol)
      override def toString                                         = name
      override def reset                                            = p.reset
    }
  }


  implicit def obj8[L, Val] = new CanBuildNegative[L,NonPackedNode, Val] {
    implicit val m = obj4

    type Nonterminal = Parsers.AbstractNonterminal[L,Val]
    def not(nt: String, p: AbstractParser[L,NonPackedNode]) = new Parsers.AbstractNonterminal[L,Val] {
      def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) =
        p(input, if (i == 0) Int.MinValue else -i, sppfLookup)
      def symbol         = org.meerkat.tree.SimpleNonterminal(nt)
      def name           = "-" + nt; override def toString = name
      override def reset = p.reset
    }

    type Symbol = Parsers.Symbol[L, Val]
    def not(p: AbstractSymbol[L,NonPackedNode, Val]) = new Parsers.Symbol[L, Val] {
      def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) =
        p(input, if (i == 0) Int.MinValue else -i, sppfLookup)
      def name           = "-" + p.name; def symbol = p.symbol
      override def reset = p.reset
    }
  }

  trait Sequence[L] extends AbstractParser[L,NonPackedNode] with Slot {
    def size: Int; def symbol: org.meerkat.tree.Sequence
  }
  trait Alternation[L]  extends AbstractParser[L, NonPackedNode] { def symbol: org.meerkat.tree.Alt }

  trait AbstractNonterminal[L,+V] extends Symbol[L, V] {
    def symbol: org.meerkat.tree.NonterminalSymbol
    type Abstract[+X] = AbstractNonterminal[L, X]
  }
  type Nonterminal[L] = AbstractNonterminal[L,NoValue]

  trait Terminal[+L] extends Symbol[L, NoValue] { def symbol: org.meerkat.tree.TerminalSymbol }
  trait Vertex[+L]   extends Symbol[L, NoValue] { def symbol: org.meerkat.tree.VertexSymbol   }

  def ε = new Terminal[Nothing] {
    def apply(input: Input[Nothing], i: Int, sppfLookup: SPPFLookup[Nothing]) =
      CPSResult.success(sppfLookup.getEpsilonNode(i))
    def symbol                                                    = TerminalSymbol(name)
    def name                                                      = "epsilon"
    override def toString = name

  }

  // TODO: disallow terminals/nonterminals to be defined as epsilon
  def epsilon = ε

  trait SequenceBuilder[L, +V] extends (Slot => Sequence[L]) with SequenceBuilderOps[L,V] {
    import AbstractParser._
    def action: Option[Any => V] = None

    def ~[U](p: Symbol[L, U])(implicit tuple: V |~| U)                = seq(this, p)

    def &[U](f: V => U) = new SequenceBuilderWithAction[L,U] {
      def apply(slot: Slot) = SequenceBuilder.this(slot)
      def action            =
        /*Option ({ x => x match
                            {case g: Set[V] => g.map(i => f(i))
                              case _=> f(x.asInstanceOf[V])}})*/ Option({ x =>
          f(x.asInstanceOf[V])
        })
    }

    def ^[U](f: String => U)(implicit sub: V <:< NoValue) = new SequenceBuilderWithAction[L,U] {
      def apply(slot: Slot) = SequenceBuilder.this(slot)
      def action =
        Option({ x =>
          f(x.asInstanceOf[String])
        })
    }

    var group: Option[AbstractNonterminal[L,_]] = None
    def !(implicit ebnf: EBNF[V]): AbstractNonterminal[L,ebnf.Group] = {
      type T = AbstractNonterminal[L,ebnf.Group]
      group
        .asInstanceOf[Option[T]]
        .getOrElse({
          val p = groupSeq(this); group = Option(p); p
        })
    }

    def ~[U](q: OperatorParsers.AbstractOperatorNonterminal[L,U])(implicit tuple: V |~| U) =
      AbstractOperatorParsers.AbstractOperatorParser.seqSeqNt(this, q)(
        OperatorParsers.OperatorImplicits.obj1[L, V, U](tuple)
      )
  }

  trait SequenceBuilderWithAction[L, +V] extends (Slot => Sequence[L])
    with SequenceBuilderOps[L, V] {
    import AbstractParser._
    def action: Option[Any => V]
  }

  trait SequenceBuilderOps[L, +V] extends (Slot => Sequence[L]) {
    import AbstractParser._
    def action: Option[Any => V]

    def |[U >: V](p: AlternationBuilder[L,U]) = altSeqAlt(this, p)
    def |[U >: V](p: SequenceBuilder[L, U])    = altSeq(this, p)
    def |[U >: V](p: Symbol[L, U])             = altSeqSym(this, p)

    def |[U >: V](q: SequenceBuilderWithAction[L,U]) = altSeq(this, q)
    def |[U >: V](q: SymbolWithAction[L,U])          = altSeqSym(this, q)
  }

  trait AlternationBuilder[L,+V] extends (Head[L] => Alternation[L]) {
    import AbstractParser._
    def action: Option[Any => V] = None

    def |[U >: V](p: AlternationBuilder[L,U]) = altAlt(this, p)
    def |[U >: V](p: SequenceBuilder[L,U])    = altAltSeq(this, p)
    def |[U >: V](p: Symbol[L, U])             = altAltSym(this, p)

    def |[U >: V](q: SequenceBuilderWithAction[L,U]) = altAltSeq(this, q)
    def |[U >: V](q: SymbolWithAction[L,U])          = altAltSym(this, q)

    var group: Option[AbstractNonterminal[L,_]] = None
    def !(implicit ebnf: EBNF[V]): AbstractNonterminal[L,ebnf.Group] = {
      type T = AbstractNonterminal[L,ebnf.Group]
      group.asInstanceOf[Option[T]].getOrElse({ val p = groupAlt(this); group = Option(p); p })
    }
  }

  trait Symbol[+L, +V] extends AbstractParser[L,NonPackedNode]
    with SymbolOps[L,V]
    with EBNFs[L, V]
    with CharLevelDisambiguation[L, V] {
    import AbstractParser._
    def name: String
    def action: Option[Any => V] = None

//    def ?[M >: L] = this.asInstanceOf[Symbol[M, V]]


    def ~[U, M >: L](p: Symbol[M, U])(implicit tuple: V |~| U)                = seq(this, p)
//    def ~(p: String)(implicit tuple: V |~| NoValue) = seq(this, p)

    def &[U](f: V => U) = new SymbolWithAction[L,U] {
      def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) = Symbol.this(input, i, sppfLookup)
      def name                                                      = Symbol.this.name; def symbol = Symbol.this.symbol
      def action =
        Option({ x =>
          f(x.asInstanceOf[V])
        })
      override def reset = Symbol.this.reset
    }
    def ^[U](f: String => U)(implicit sub: V <:< NoValue) = new SymbolWithAction[L,U] {
      def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) = Symbol.this(input, i, sppfLookup)
      def name                                                      = Symbol.this.name; def symbol = Symbol.this.symbol
      def action =
        Option({ x =>
          f(x.asInstanceOf[String])
        })
      override def reset = Symbol.this.reset
    }
  }

  trait SymbolWithAction[+L, +V] extends AbstractParser[L, NonPackedNode]
    with SymbolOps[L,V] {
    import AbstractParser._
    def name: String
    def action: Option[Any => V]
  }

  trait SymbolOps[+L,+V] extends AbstractParser[L,NonPackedNode] {
    import AbstractParser._
    def name: String
    def action: Option[Any => V]

    def |[U >: V, M >: L](p: AlternationBuilder[M, U]) = altSymAlt(this, p)
    def |[U >: V, M >: L](p: SequenceBuilder[M, U])    = altSymSeq(this, p)
    def |[U >: V, M >: L](p: Symbol[M, U])             = altSym(this, p)

    def |[U >: V, M >: L](q: SequenceBuilderWithAction[M, U]) = altSymSeq(this, q)
    def |[U >: V, M >: L](q: SymbolWithAction[M, U])          = altSym(this, q)
  }

  implicit def toTerminal[E](label: E): Terminal[E] =
    terminal(label)


  def terminal[L](label: L): Terminal[L] = new Terminal[L] {
    def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]): CPSResult[TerminalNode[L]] =
      input.filterEdges(i, label) match {
        case edges if edges.isEmpty => CPSResult.failure
        case edges =>
          val terminals = edges.map {
            case (edgeName: L, to: Int) =>
              CPSResult.success(sppfLookup.getTerminalNode(edgeName, i, to))
          }
          terminals.reduceLeft(_.orElse(_))
      }
    
    override def name: String     = label.toString // TODO: add type signature 
    override def symbol           = TerminalSymbol(label)
    override def toString: String = name.toString
  }


  def ntAlt[L, Val](name: String, p: => AlternationBuilder[L,Val])            = nonterminalAlt(name, p)
  def ntSeq[L, Val](name: String, p: => SequenceBuilder[L,Val])               = nonterminalSeq(name, p)
  def ntSym[L,Val](name: String, p: => AbstractSymbol[L,NonPackedNode, Val]) = nonterminalSym(name, p)

  def notSym[L, Val](name: String, p: => AbstractSymbol[L,NonPackedNode, Val]) = negativeSym(name, p)

  // TODO: fix
  trait EBNFs[+L, +V] { self: Symbol[L, V] =>
    var opt: Option[AbstractNonterminal[_,_]] = None
    def ?[M >: L](implicit ebnf: EBNF[V]): AbstractNonterminal[M, ebnf.OptOrSeq] = {
      type T = AbstractNonterminal[L,ebnf.OptOrSeq]
      opt
        .asInstanceOf[Option[T]]
        .getOrElse({
          val p =
            regular[L, NonPackedNode, ebnf.OptOrSeq](org.meerkat.tree.Opt(this.symbol),
              this & ebnf.unit | ε ^ ebnf.empty)
          opt = Option(p)
          p
        }).asInstanceOf[AbstractNonterminal[M, ebnf.OptOrSeq]]
    }

    var star: Option[AbstractNonterminal[_, _]] = None
    def *[M >: L](implicit ebnf: EBNF[V]): AbstractNonterminal[M,ebnf.OptOrSeq] = {
      type T = AbstractNonterminal[L,ebnf.OptOrSeq]
      star
        .asInstanceOf[Option[T]]
        .getOrElse({
          val p = regular[L, NonPackedNode, ebnf.OptOrSeq](
            org.meerkat.tree.Star(this.symbol),
            star.asInstanceOf[Option[T]].get ~ this & ebnf.add | ε ^ ebnf.empty
          )
          star = Option(p)
          p
        }).asInstanceOf[AbstractNonterminal[M, ebnf.OptOrSeq] ]
    }



    val star_sep: mutable.Map[String, AbstractNonterminal[_, _]] = mutable.HashMap.empty
    def *[M >: L](sep: Terminal[M])(implicit ebnf: EBNF[V]): AbstractNonterminal[M,ebnf.OptOrSeq] = {
      type T = AbstractNonterminal[L,ebnf.OptOrSeq]
      star_sep
        .getOrElseUpdate(sep.name, {
          regular[L, NonPackedNode, ebnf.OptOrSeq](
            org.meerkat.tree.Star(this.symbol),
            this.+(sep).asInstanceOf[AlternationBuilder[L, ebnf.OptOrSeq]] | ε ^ ebnf.empty)
        })
        .asInstanceOf[AbstractNonterminal[M, ebnf.OptOrSeq] ]
    }

    var plus: Option[AbstractNonterminal[_, _]] = None
    def +[M >: L](implicit ebnf: EBNF[V]): AbstractNonterminal[M,ebnf.OptOrSeq] = {
      type T = AbstractNonterminal[L,ebnf.OptOrSeq]
      plus
        .asInstanceOf[Option[T]]
        .getOrElse({
          val p = regular[L, NonPackedNode, ebnf.OptOrSeq](
            org.meerkat.tree.Plus(this.symbol),
            plus.asInstanceOf[Option[T]].get ~ this & ebnf.add | this & ebnf.unit
          )
          plus = Option(p); p
        }).asInstanceOf[AbstractNonterminal[M, ebnf.OptOrSeq] ]
    }


    val plus_sep: mutable.Map[String, AbstractNonterminal[_, _]] = mutable.HashMap.empty
    def +[M >: L](sep: M)(implicit ebnf: EBNF[V]): AbstractNonterminal[M, ebnf.OptOrSeq] = {
      type T = AbstractNonterminal[L, ebnf.OptOrSeq]
      plus_sep
        .getOrElseUpdate(sep.name, {
          regular[M, NonPackedNode, ebnf.OptOrSeq](
            org.meerkat.tree.Plus(this.symbol),
            plus_sep(sep.name).asInstanceOf[T] ~ terminal(sep) ~ this & ebnf.add | this & ebnf.unit
          )
        })
        .asInstanceOf[AbstractNonterminal[M, ebnf.OptOrSeq]]
    }
  }
  trait CharLevelDisambiguation[+L, +V] { self: Symbol[L, V] =>
    def \(arg: String) =
      postFilter(this, (input: Input[String], t: NonPackedNode) => arg != input.substring(t.leftExtent, t.rightExtent), s" \\ $arg")
    def \(args: Set[String]) =
      postFilter(
        this,
        (input: Input[String], t: NonPackedNode) => !args.contains(input.substring(t.leftExtent, t.rightExtent)),
        " \\ " + args.mkString(",")
      )
    def \(args: String*) =
      postFilter(
        this,
        (input: Input[String], t: NonPackedNode) => !args.contains(input.substring(t.leftExtent, t.rightExtent)),
        " \\ " + args.mkString(",")
      )
//    def \(arg: Regex) =
//      postFilter(this, (input, t: NonPackedNode) => !input.matchRegex(arg, t.leftExtent, t.rightExtent), s" \\ $arg")
    def \(arg: Char) =
      postFilter(
        this,
        (input: Input[String], t: NonPackedNode) => !(t.rightExtent - t.leftExtent == 1 && input.charAt(t.leftExtent) == arg),
        s" \\ $arg"
      )

    //def !>>(arg: String) = postFilter(this, (input,t:NonPackedNode) => !input.startsWith(arg, t.rightExtent), s" !>> $arg")
    //def !>>(args: String*) = postFilter(this, (input,t:NonPackedNode) => !args.exists(input.startsWith(_, t.rightExtent)), " !>> " + args.mkString(","))
//    def !>>(arg: Regex) =
//      postFilter(this, (input, t: NonPackedNode) => input.matchRegex(arg, t.rightExtent) == -1, s" !>> $arg")
    def !>>(arg: Char) = postFilter(this, (input: Input[String], t: NonPackedNode) => input.charAt(t.rightExtent) != arg, s" !>> $arg")

    def !<<(arg: String) = preFilter(this, (input: Input[String], i) => !input.substring(0, i).endsWith(arg), s"$arg !<< ")
    def !<<(args: String*) =
      preFilter(
        this,
        (input: Input[String], i) => { val sub = input.substring(0, i); args.filter(sub.endsWith(_)).isEmpty },
        args.mkString(",") + " !<< "
      )
//    def !<<(arg: Regex) = preFilter(this, (input, i) => !input.matchRegex(arg, i - 1, i), s"$arg !<< ")
    def !<<(arg: Char)  = preFilter(this, (input: Input[String], i) => !(i > 0 && input.charAt(i - 1) == arg), s"$arg !<< ")
  }

}
