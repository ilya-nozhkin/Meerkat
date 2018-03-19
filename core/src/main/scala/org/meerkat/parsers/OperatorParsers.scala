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

import org.meerkat.sppf.NonPackedNode
import java.util.HashMap
import org.meerkat.util.visualization._
import scala.collection.mutable

object OperatorParsers {

  import AbstractOperatorParsers._
  import Parsers._

  object OperatorImplicits {

    implicit def obj1[L, ValA, ValB](implicit vals: ValA |~| ValB) =
      new CanBuildSequence[L,NonPackedNode, NonPackedNode, ValA, ValB] {
        implicit val o = Parsers.obj1[L, ValA, ValB](vals)

        type OperatorSequence = OperatorParsers.OperatorSequence[L,o.V]

        def sequence(p: AbstractOperatorSequence): OperatorSequence = new OperatorSequence {
          def apply(prec1: Prec, prec2: Prec) = p(prec1, prec2)
          def infix                           = p.infix; def prefix = p.prefix; def postfix = p.postfix
          def assoc                           = p.assoc
        }

        def assoc(p: OperatorSequence, a: Assoc.Assoc): OperatorSequence =
          if ((p.infix && a != Assoc.NON_ASSOC)
              || ((p.infix || p.prefix || p.postfix) && a == Assoc.NON_ASSOC))
            new OperatorSequence {
              def apply(prec1: Prec, prec2: Prec) = p(prec1, prec2)
              def infix                           = p.infix; def prefix = p.prefix; def postfix = p.postfix
              def assoc: Assoc.Assoc              = a
            } else p

        type OperatorSequenceBuilder = OperatorParsers.OperatorSequenceBuilder[L,o.V]
        def builderSeq(f: Head[L] => OperatorSequence): OperatorSequenceBuilder = new OperatorSequenceBuilder {
          def apply(head: Head[L]) = f(head)
        }
      }

    implicit def obj2[L, ValA, ValB] = new CanBuildAlternation[L,NonPackedNode, NonPackedNode, ValA, ValB] {
      implicit val o = Parsers.obj3[L, ValA, ValB]

      type OperatorAlternation = OperatorParsers.OperatorAlternation[L, ValB]
      def alternation(f: Prec => o.AlternationBuilder): OperatorAlternation = new OperatorAlternation {
        def apply(prec: Prec) = f(prec)
      }

      type OperatorAlternationBuilder = OperatorParsers.OperatorAlternationBuilder[L,ValB]
      def builderAlt(
        f: (Head[L], Group) => (Group => OperatorAlternation, Group, Option[Group])
      ): OperatorAlternationBuilder = new OperatorAlternationBuilder {
        def apply(head: Head[L], group: Group) = f(head, group)
      }
    }

    implicit def obj3[L, Val] = new CanBuildNonterminal[L,NonPackedNode, Val] {
      implicit val o1 = Parsers.obj5[L, Val]
      implicit val o2 = Parsers.obj2

      type OperatorNonterminal = OperatorParsers.AbstractOperatorNonterminal[L,Val]
      def nonterminal(ntName: String, f: Prec => o1.Nonterminal): OperatorNonterminal = new OperatorNonterminal {
        val table: java.util.Map[Prec, o1.Nonterminal] = new HashMap()
        def apply(prec: Prec) =
          if (table.containsKey(prec)) table.get(prec)
          else { val nt = f(prec); table.put(prec, nt); nt }
        def name = ntName; override def toString = ntName
      }
    }
  }

  trait OperatorSequence[L,+V] extends ((Prec, Prec) => Parsers.SequenceBuilder[L,V]) {
    def infix: Boolean; def prefix: Boolean; def postfix: Boolean; def assoc: Assoc.Assoc
  }

  trait OperatorSequenceWithAction[L,+V] extends ((Prec, Prec) => Parsers.SequenceBuilderWithAction[L,V]) {
    def infix: Boolean; def prefix: Boolean; def postfix: Boolean; def assoc: Assoc.Assoc
  }

  trait OperatorAlternation[L, +V] extends (Prec => Parsers.AlternationBuilder[L,V])

  trait AbstractOperatorNonterminal[L,+V] extends (Prec => Parsers.AbstractNonterminal[L,V]) with EBNFs[L, V] {
    import OperatorImplicits._; import AbstractOperatorParser._
    type Abstract[+X] = AbstractOperatorNonterminal[L,X]
    def name: String

    def ~[U](p: AbstractOperatorNonterminal[L,U])(implicit tuple: V |~| U)                = seqNt(this, p)
    def ~[U](p: Symbol[L, U])(implicit tuple: V |~| U)                                     = seqNtSym(this, p)

    def |[U >: V](p: OperatorAlternationBuilder[L,U])        = altOpSymOpAlt(this, p)
    def |[U >: V](p: OperatorSequenceBuilder[L,U])           = altOpSymOpSeq(this, p)
    def |[U >: V](p: AbstractOperatorNonterminal[L,U])       = altOpSym(this, p)
    def |[U >: V](p: OperatorSequenceBuilderWithAction[L,U]) = altOpSymOpSeq(this, p)
    def |[U >: V](p: OperatorNonterminalWithAction[L, U])     = altOpSym(this, p)

    def |[U >: V](p: AlternationBuilder[L,U])        = altOpSymOpAlt(this, altAltOpAlt(p))
    def |[U >: V](p: SequenceBuilder[L,U])           = altOpSymOpSeq(this, altSeqOpSeq(p))
    def |[U >: V](p: Symbol[L, U])                    = altOpSym(this, altSymOpSym(p))
    def |[U >: V](p: SequenceBuilderWithAction[L,U]) = altOpSymOpSeq(this, altSeqOpSeq(p))
    def |[U >: V](p: SymbolWithAction[L,U])          = altOpSym(this, altSymOpSym(p))

    def &[U](f: V => U) = new OperatorNonterminalWithAction[L,U] {
      def apply(prec: Prec) = AbstractOperatorNonterminal.this(prec) & f
      def name              = AbstractOperatorNonterminal.this.name
    }

    def ^[U](f: String => U)(implicit sub: V <:< NoValue) = new OperatorNonterminalWithAction[L,U] {
      def apply(prec: Prec) = AbstractOperatorNonterminal.this(prec) ^ f
      def name              = AbstractOperatorNonterminal.this.name
    }
  }

  trait OperatorNonterminalWithAction[L, +V] extends (Prec => Parsers.SymbolWithAction[L,V]) {
    import OperatorImplicits._; import AbstractOperatorParser._
    def name: String

    def |[U >: V](p: OperatorAlternationBuilder[L,U])        = altOpSymOpAlt(this, p)
    def |[U >: V](p: OperatorSequenceBuilder[L,U])           = altOpSymOpSeq(this, p)
    def |[U >: V](p: AbstractOperatorNonterminal[L,U])       = altOpSym(this, p)
    def |[U >: V](p: OperatorSequenceBuilderWithAction[L,U]) = altOpSymOpSeq(this, p)
    def |[U >: V](p: OperatorNonterminalWithAction[L,U])     = altOpSym(this, p)

    def |[U >: V](p: AlternationBuilder[L,U])        = altOpSymOpAlt(this, altAltOpAlt(p))
    def |[U >: V](p: SequenceBuilder[L,U])           = altOpSymOpSeq(this, altSeqOpSeq(p))
    def |[U >: V](p: Symbol[L, U])                    = altOpSym(this, altSymOpSym(p))
    def |[U >: V](p: SequenceBuilderWithAction[L,U]) = altOpSymOpSeq(this, altSeqOpSeq(p))
    def |[U >: V](p: SymbolWithAction[L,U])          = altOpSym(this, altSymOpSym(p))
  }

  type OperatorNonterminal[L] = AbstractOperatorNonterminal[L,NoValue]

  trait OperatorSequenceBuilder[L,+V] extends (Head[L] => OperatorSequence[L,V]) {
    import OperatorImplicits._; import AbstractOperatorParser._

    def ~[U](p: AbstractOperatorNonterminal[L,U])(implicit tuple: V |~| U) = seqOpSeqNt(this, p)
    def ~[U](p: Symbol[L, U])(implicit tuple: V |~| U)                      = seqOpSeqSym(this, p)

    def |[U >: V](p: OperatorAlternationBuilder[L,U])        = altOpSeqOpAlt(this, p)
    def |[U >: V](p: OperatorSequenceBuilder[L,U])           = altOpSeq(this, p)
    def |[U >: V](p: AbstractOperatorNonterminal[L,U])       = altOpSeqOpSym(this, p)
    def |[U >: V](p: OperatorSequenceBuilderWithAction[L,U]) = altOpSeq(this, p)
    def |[U >: V](p: OperatorNonterminalWithAction[L,U])     = altOpSeqOpSym(this, p)

    def |[U >: V](p: AlternationBuilder[L,U])        = altOpSeqOpAlt(this, altAltOpAlt(p))
    def |[U >: V](p: SequenceBuilder[L,U])           = altOpSeq(this, altSeqOpSeq(p))
    def |[U >: V](p: Symbol[L, U])                    = altOpSeqOpSym(this, altSymOpSym(p))
    def |[U >: V](p: SequenceBuilderWithAction[L,U]) = altOpSeq(this, altSeqOpSeq(p))
    def |[U >: V](p: SymbolWithAction[L,U])          = altOpSeqOpSym(this, altSymOpSym(p))

    def |>[U >: V](p: OperatorAlternationBuilder[L,U])        = greaterOpSeqOpAlt(this, p)
    def |>[U >: V](p: OperatorSequenceBuilder[L,U])           = greaterOpSeq(this, p)
    def |>[U >: V](p: OperatorSequenceBuilderWithAction[L,U]) = greaterOpSeq(this, p)

    def &[U](f: V => U) = new OperatorSequenceBuilderWithAction[L,U] {
      def apply(head: Head[L]) = {
        val p = OperatorSequenceBuilder.this(head)
        new OperatorSequenceWithAction[L,U] {
          def apply(prec1: Prec, prec2: Prec) = p(prec1, prec2) & f
          def infix                           = p.infix; def prefix = p.prefix; def postfix = p.postfix; def assoc = p.assoc
        }
      }
    }

    def ^[U](f: String => U)(implicit sub: V <:< NoValue) = new OperatorSequenceBuilderWithAction[L,U] {
      def apply(head: Head[L]) = {
        val p = OperatorSequenceBuilder.this(head)
        new OperatorSequenceWithAction[L,U] {
          def apply(prec1: Prec, prec2: Prec) = p(prec1, prec2) ^ f
          def infix                           = p.infix; def prefix = p.prefix; def postfix = p.postfix; def assoc = p.assoc
        }
      }
    }
  }

  trait OperatorSequenceBuilderWithAction[L,+V] extends (Head[L] => OperatorSequenceWithAction[L,V]) {
    import OperatorImplicits._; import AbstractOperatorParser._
    def |[U >: V](p: OperatorAlternationBuilder[L,U])        = altOpSeqOpAlt(this, p)
    def |[U >: V](p: OperatorSequenceBuilder[L,U])           = altOpSeq(this, p)
    def |[U >: V](p: AbstractOperatorNonterminal[L,U])       = altOpSeqOpSym(this, p)
    def |[U >: V](p: OperatorSequenceBuilderWithAction[L,U]) = altOpSeq(this, p)
    def |[U >: V](p: OperatorNonterminalWithAction[L,U])     = altOpSeqOpSym(this, p)

    def |[U >: V](p: AlternationBuilder[L,U])        = altOpSeqOpAlt(this, altAltOpAlt(p))
    def |[U >: V](p: SequenceBuilder[L,U])           = altOpSeq(this, altSeqOpSeq(p))
    def |[U >: V](p: Symbol[L, U])                    = altOpSeqOpSym(this, altSymOpSym(p))
    def |[U >: V](p: SequenceBuilderWithAction[L,U]) = altOpSeq(this, altSeqOpSeq(p))
    def |[U >: V](p: SymbolWithAction[L,U])          = altOpSeqOpSym(this, altSymOpSym(p))

    def |>[U >: V](p: OperatorAlternationBuilder[L,U])        = greaterOpSeqOpAlt(this, p)
    def |>[U >: V](p: OperatorSequenceBuilder[L,U])           = greaterOpSeq(this, p)
    def |>[U >: V](p: OperatorSequenceBuilderWithAction[L,U]) = greaterOpSeq(this, p)
  }

  trait OperatorAlternationBuilder[L,+V]
      extends ((Head[L], Group) => (Group => OperatorAlternation[L, V], Group, Option[Group])) {
    import OperatorImplicits._; import AbstractOperatorParser._
    def |[U >: V](p: OperatorAlternationBuilder[L,U])        = altOpAlt(this, p)
    def |[U >: V](p: OperatorSequenceBuilder[L,U])           = altOpAltOpSeq(this, p)
    def |[U >: V](p: AbstractOperatorNonterminal[L,U])       = altOpAltOpSym(this, p)
    def |[U >: V](p: OperatorSequenceBuilderWithAction[L,U]) = altOpAltOpSeq(this, p)
    def |[U >: V](p: OperatorNonterminalWithAction[L,U])     = altOpAltOpSym(this, p)

    def |[U >: V](p: AlternationBuilder[L,U])        = altOpAlt(this, altAltOpAlt(p))
    def |[U >: V](p: SequenceBuilder[L,U])           = altOpAltOpSeq(this, altSeqOpSeq(p))
    def |[U >: V](p: Symbol[L, U])                    = altOpAltOpSym(this, altSymOpSym(p))
    def |[U >: V](p: SequenceBuilderWithAction[L,U]) = altOpAltOpSeq(this, altSeqOpSeq(p))
    def |[U >: V](p: SymbolWithAction[L,U])          = altOpAltOpSym(this, altSymOpSym(p))

    def |>[U >: V](p: OperatorAlternationBuilder[L,U])        = greaterOpAlt(this, p)
    def |>[U >: V](p: OperatorSequenceBuilder[L,U])           = greaterOpAltOpSeq(this, p)
    def |>[U >: V](p: OperatorSequenceBuilderWithAction[L,U]) = greaterOpAltOpSeq(this, p)
  }

  implicit class ParsersSeqOps[L,V](p: Parsers.Symbol[L, V]) {
    import OperatorImplicits._; import AbstractOperatorParser._
    def ~[U](q: AbstractOperatorNonterminal[L,U])(implicit tuple: V |~| U)                = seqSymNt(p, q)

    def |[U >: V](q: OperatorAlternationBuilder[L,U])        = altOpSymOpAlt(altSymOpSym(p), q)
    def |[U >: V](q: OperatorSequenceBuilder[L,U])           = altOpSymOpSeq(altSymOpSym(p), q)
    def |[U >: V](q: AbstractOperatorNonterminal[L,U])       = altOpSym(altSymOpSym(p), q)
    def |[U >: V](q: OperatorSequenceBuilderWithAction[L,U]) = altOpSymOpSeq(altSymOpSym(p), q)
    def |[U >: V](q: OperatorNonterminalWithAction[L,U])     = altOpSym(altSymOpSym(p), q)
  }

  implicit class ParsersAltOps2[L,V](p: Parsers.AlternationBuilder[L,V]) {
    import OperatorImplicits._; import AbstractOperatorParser._
    def |[U >: V](q: OperatorAlternationBuilder[L,U])        = altOpAlt(altAltOpAlt(p), q)
    def |[U >: V](q: OperatorSequenceBuilder[L,U])           = altOpAltOpSeq(altAltOpAlt(p), q)
    def |[U >: V](q: AbstractOperatorNonterminal[L,U])       = altOpAltOpSym(altAltOpAlt(p), q)
    def |[U >: V](q: OperatorSequenceBuilderWithAction[L,U]) = altOpAltOpSeq(altAltOpAlt(p), q)
    def |[U >: V](q: OperatorNonterminalWithAction[L,U])     = altOpAltOpSym(altAltOpAlt(p), q)
  }

  implicit class ParsersAltOps1[L, +V](p: Parsers.SequenceBuilder[L,V]) {
    import OperatorImplicits._; import AbstractOperatorParser._
    def |[U >: V](q: OperatorAlternationBuilder[L,U])        = altOpSeqOpAlt(altSeqOpSeq(p), q)
    def |[U >: V](q: OperatorSequenceBuilder[L,U])           = altOpSeq(altSeqOpSeq(p), q)
    def |[U >: V](q: AbstractOperatorNonterminal[L,U])       = altOpSeqOpSym(altSeqOpSeq(p), q)
    def |[U >: V](q: OperatorSequenceBuilderWithAction[L,U]) = altOpSeq(altSeqOpSeq(p), q)
    def |[U >: V](q: OperatorNonterminalWithAction[L,U])     = altOpSeqOpSym(altSeqOpSeq(p), q)
  }

//  implicit class StringSeqOps[L](term: E) {
//    import OperatorImplicits._; import AbstractOperatorParser._
//    val p =
//      Parsers.toTerminal(term)
//    def ~[ U](q: AbstractOperatorNonterminal[L,U])
//             (implicit tuple: NoValue |~| U)
//    = seqSymNt(p, q)
//  }

//  implicit class StringAltOps(term: String) { import OperatorImplicits._; import AbstractOperatorParser._
//    val p: Symbol { type Value = NoValue } = term
//
//    def | (q: OperatorAlternationBuilder[L,NoValue]) = altOpSymOpAlt(altSymOpSym(p), q)
//    def | (q: OperatorSequenceBuilder[L,NoValue]) = altOpSymOpSeq(altSymOpSym(p), q)
//    def | (q: AbstractOperatorNonterminal[L,NoValue]) = altOpSym(altSymOpSym(p), q)
//    def | (q: OperatorSequenceBuilderWithAction[L,NoValue]) = altOpSymOpSeq(altSymOpSym(p), q)
//    def | (q: OperatorNonterminalWithAction[L,NoValue]) = altOpSym(altSymOpSym(p), q)
//
//    def ^ [U](f: String => U)(implicit sub: p.Value <:< NoValue) = p ^ f
//  }

  def left[L, V](p: OperatorSequenceBuilder[L,V]): OperatorSequenceBuilder[L,V] = {
    import OperatorImplicits._
    val o = obj1[L, V, V](new |~|[V, V] { type R = V })
    o.builderSeq(head => o.assoc(p(head), Assoc.LEFT))
  }

  def right[L, V](p: OperatorSequenceBuilder[L,V]): OperatorSequenceBuilder[L,V] = {
    import OperatorImplicits._
    val o = obj1[L, V, V](new |~|[V, V] { type R = V })
    o.builderSeq(head => o.assoc(p(head), Assoc.RIGHT))
  }

  def non_assoc[L, V](p: OperatorSequenceBuilder[L,V]): OperatorSequenceBuilder[L,V] = {
    import OperatorImplicits._
    val o = obj1[L, V, V](new |~|[V, V] { type R = V })
    o.builderSeq(head => o.assoc(p(head), Assoc.NON_ASSOC))
  }

  def left[L, Val](p: OperatorAlternationBuilder[L,Val]): OperatorAlternationBuilder[L,Val] = {
    import OperatorImplicits._; import AbstractOperatorParser.assocAlt
    assocAlt(obj2[L, Val, Val])(p, Assoc.LEFT)
  }

  def right[L, Val](p: OperatorAlternationBuilder[L,Val]): OperatorAlternationBuilder[L,Val] = {
    import OperatorImplicits._; import AbstractOperatorParser.assocAlt
    assocAlt(obj2[L, Val, Val])(p, Assoc.RIGHT)
  }

  def non_assoc[L, Val](p: OperatorAlternationBuilder[L,Val]): OperatorAlternationBuilder[L,Val] = {
    import OperatorImplicits._; import AbstractOperatorParser.assocAlt
    assocAlt(obj2[L, Val, Val])(p, Assoc.NON_ASSOC)
  }

  def ntAlt[L, Val](name: String, p: => OperatorAlternationBuilder[L,Val]): AbstractOperatorNonterminal[L,Val] = {
    import OperatorImplicits._; import AbstractOperatorParser.nonterminalAlt
    nonterminalAlt(name, p)
  }
  def ntSeq[L, Val](name: String, p: => OperatorSequenceBuilder[L,Val]): AbstractOperatorNonterminal[L,Val] = {
    import OperatorImplicits._; import AbstractOperatorParser.nonterminalSeq
    nonterminalSeq(name, p)
  }
  def ntSym[L, Val](name: String, p: => AbstractOperatorNonterminal[L,Val]): AbstractOperatorNonterminal[L,Val] = {
    import OperatorImplicits._; import AbstractOperatorParser.nonterminalSym
    nonterminalSym(name, p)
  }
  def ntSeqWithAction[L, Val](name: String,
                           p: => OperatorSequenceBuilderWithAction[L,Val]): AbstractOperatorNonterminal[L,Val] = {
    import OperatorImplicits._; import AbstractOperatorParser.nonterminalSeq
    nonterminalSeq(name, p)
  }
  def ntSymWithAction[L, Val](name: String, p: => OperatorNonterminalWithAction[L,Val]): AbstractOperatorNonterminal[L,Val] = {
    import OperatorImplicits._; import AbstractOperatorParser.nonterminalSym
    nonterminalSym(name, p)
  }

  // TODO: the same as in Parsers.scala?
  trait EBNFs[L, +V] { self: AbstractOperatorNonterminal[L,V] =>
//    var star: Option[AbstractOperatorNonterminal[L,_]] = None
//    def *(implicit ebnf: EBNF[V]): AbstractOperatorNonterminal[L,ebnf.OptOrSeq] =
//      star
//        .getOrElse({
//          val p = new AbstractOperatorNonterminal[L,ebnf.OptOrSeq] {
//            def apply(prec: Prec) = self($).*(ebnf); def name = self.name + "*"
//          }
//          star = Option(p); p
//        })
//        .asInstanceOf[AbstractOperatorNonterminal[L,ebnf.OptOrSeq]]
//
//    val star_sep: mutable.Map[E, AbstractOperatorNonterminal[L,_]] =
//      mutable.HashMap()
//    def *(sep: E)(implicit ebnf: EBNF[V]): AbstractOperatorNonterminal[L,ebnf.OptOrSeq] =
//      star_sep
//        .getOrElseUpdate(sep, new AbstractOperatorNonterminal[L,ebnf.OptOrSeq] {
//          def apply(prec: Prec) = self($).*(sep)(ebnf); def name = s"{${self.name} $sep}*"
//        })
//        .asInstanceOf[AbstractOperatorNonterminal[L,ebnf.OptOrSeq]]
//
//    var plus: Option[AbstractOperatorNonterminal[L,_]] = None
//    def +(implicit ebnf: EBNF[V]): AbstractOperatorNonterminal[L,ebnf.OptOrSeq] =
//      plus
//        .getOrElse({
//          val p = new AbstractOperatorNonterminal[L,ebnf.OptOrSeq] {
//            def apply(prec: Prec) = self($).+; def name = self.name + "+"
//          }
//          plus = Option(p); p
//        })
//        .asInstanceOf[AbstractOperatorNonterminal[L,ebnf.OptOrSeq]]
//
//    val plus_sep: mutable.Map[E, AbstractOperatorNonterminal[L,_]] =
//      mutable.HashMap()
//    def +(sep: E)(implicit ebnf: EBNF[V]): AbstractOperatorNonterminal[L,ebnf.OptOrSeq] =
//      plus_sep
//        .getOrElseUpdate(sep, new AbstractOperatorNonterminal[L,ebnf.OptOrSeq] {
//          def apply(prec: Prec) = self($).+(sep)(ebnf); def name = s"{${self.name} $sep}+"
//        })
//        .asInstanceOf[AbstractOperatorNonterminal[L,ebnf.OptOrSeq]]
  }
}
