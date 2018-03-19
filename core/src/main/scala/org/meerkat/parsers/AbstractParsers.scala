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
import org.meerkat.sppf.SPPFLookup

import scala.reflect.ClassTag
import org.meerkat.sppf.Slot
import org.meerkat.tree.NonterminalSymbol
import org.meerkat.tree.TerminalSymbol

import scala.collection.mutable
import scala.language.higherKinds

trait MonadPlus[+T, M[+F] <: MonadPlus[F, M]] {
  def map[U](f: T => U)(implicit m: Memoizable[T]): M[U]
  def flatMap[U](f: T => M[U])(implicit m: Memoizable[T]): M[U]
  def orElse[U >: T](r: => M[U]): M[U]
  def filter(pred: T => Boolean): M[T]

  // Specialization only for optimization purposes
  def smap[U](f: T => U): M[U]
}

trait AbstractParsers {

  type Result[+T] <: MonadPlus[T, Result]
  trait AbstractParser[+L, +T] extends ((Input [L], Int, SPPFLookup[L]) => Result[T]) {
    def symbol: org.meerkat.tree.Symbol
    def reset(): Unit = {}
  }

//  implicit class ParserOps[-E, -N, +T] (parser:  AbstractParser[L, T]) extends AnyRef {
//    def withI
//  }

  type AbstractSequence[L,+T] = AbstractParser[L,T] with Slot { def size: Int; def symbol: org.meerkat.tree.Sequence }

  type AbstractAlternation[L,+T] = AbstractParser[L,T] { def symbol: org.meerkat.tree.Alt }

  type AbstractSymbol[L, +T, +V] = AbstractParser[L,T] { def name: String; def action: Option[Any => V] }

  type AbstractNonterminal[L,+T, +V] = AbstractSymbol[L,T, V] { def symbol: org.meerkat.tree.NonterminalSymbol }

  type Head[L] = AbstractNonterminal[L,Any, Any]

  type AbstractSequenceBuilder[L,+T, +V]    = (Slot => AbstractSequence[L,T]) { def action: Option[Any => V] }
  type AbstractAlternationBuilder[L,+T, +V] = (Head[L] => AbstractAlternation[L,T]) { def action: Option[Any => V] }

  trait CanBuildSequence[L,A, B, ValA, ValB] {
    implicit val m1: Memoizable[A]
    implicit val m2: Memoizable[B]

    type T; type V

    type Sequence <: AbstractSequence[L,T]
    def sequence(p: AbstractSequence[L,T]): Sequence

    def index(a: A): Int
    def intermediate(a: A, b: B, p: Slot, sppfLookup: SPPFLookup[L]): T

    type SequenceBuilder <: (Slot => Sequence) { def action: Option[Any => V] }
    def builderSeq(f: Slot => Sequence): SequenceBuilder
  }

  trait CanBuildAlternative[L,A] {
    implicit val m: Memoizable[A]
    def result(e: A, p: Slot, nt: Head[L], sppfLookup: SPPFLookup[L]): A
  }

  trait CanBuildAlternation[L,A, B, ValA, ValB] {
    implicit val m1: Memoizable[A]
    implicit val m2: Memoizable[B]

    implicit val o1: CanBuildAlternative[L,A]
    implicit val o2: CanBuildAlternative[L,B]

    type Alternation <: AbstractAlternation[L,B]
    def alternation(f: AbstractParser[L,B]): Alternation

    type AlternationBuilder <: (Head[L] => Alternation) { def action: Option[Any => ValB] }
    def builderAlt(f: Head[L] => Alternation): AlternationBuilder
  }

  trait CanBuildNonterminal[L,A, ValA] {
    implicit val m: Memoizable[A]
    type Nonterminal <: AbstractNonterminal[L,A, ValA]
    def nonterminal(name: String, p: AbstractParser[L,A]): Nonterminal

    type Symbol <: AbstractSymbol[L,A, ValA]
    def symbol(p: AbstractSymbol[L,A, ValA]): Symbol
  }

  trait CanBuildNegative[L,A, ValA] {
    implicit val m: Memoizable[A]
    type Symbol <: AbstractSymbol[L,A, ValA]
    type Nonterminal <: AbstractNonterminal[L,A, ValA]

    def not(name: String, p: AbstractParser[L,A]): Nonterminal
    def not(p: AbstractSymbol[L,A, ValA]): Symbol
  }

  trait CanBuildEBNF[L, A, ValA] {
    implicit val m: Memoizable[A]

    type Regular <: AbstractNonterminal[L,A, ValA]
    type Group <: AbstractNonterminal[L,A, ValA]

    def regular(symbol: org.meerkat.tree.NonterminalSymbol, p: AbstractParser[L,A]): Regular
    def group(p: AbstractParser[L,A]): Group
  }

  trait CanMap[L,A, B, Val] {
    implicit val m: Memoizable[A]
    type Nonterminal <: AbstractNonterminal[L,B, Val]
    def nonterminal(name: String, p: AbstractParser[L,B]): Nonterminal

    def index(a: A): Int
    def intermediate(a: A, b: B, p: Slot, sppfLookup: SPPFLookup[L]): B

    type Sequence <: AbstractSequence[L,B]
    def sequence(p: AbstractSequence[L,B]): Sequence

    type SequenceBuilder <: (Slot => Sequence) { def action: Option[Any => Val] }
    def builderSeq(f: Slot => Sequence): SequenceBuilder
  }

  object AbstractParser {

    def seq[L, A, B, ValA, ValB](p1: AbstractSequenceBuilder[L,A, ValA], p2: AbstractSymbol[L,B, ValB])(
      implicit builder: CanBuildSequence[L,A, B, ValA, ValB]
    ): builder.SequenceBuilder = builder.builderSeq { slot =>
      val q1 = p1(slot); sequence(slot, q1, p2, q1.size + 1)
    }

    def seq[L, A, B, ValA, ValB](p1: AbstractSymbol[L,A, ValA], p2: AbstractSymbol[L,B, ValB])(
      implicit builder: CanBuildSequence[L,A, B, ValA, ValB]
    ): builder.SequenceBuilder = builder.builderSeq { slot =>
      sequence(slot, p1, p2, 2)
    }

    /*protected def sequence[A,B,ValA,ValB](slot: Slot, p1: AbstractParser[L,A], p2: AbstractParser[L,B], s: Int)
                                         (implicit builder: CanBuildSequence[L,A,B,ValA,ValB]): builder.Sequence = { import builder._
      builder sequence (new AbstractParser[L,T] with Slot {
        def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) = p1(input,i,sppfLookup) flatMap { x => p2(input,index(x),sppfLookup).smap { intermediate(x,_,this,sppfLookup) } }
        def size = s; def symbol = org.meerkat.tree.Sequence(p1.symbol, p2.symbol)
        def ruleType = org.meerkat.tree.PartialRule(slot.ruleType.head, slot.ruleType.body, s)
        override def reset = { p1.reset; p2.reset }
        override def toString = s"[${ruleType.toString()},$size]"
      })
    }*/
    protected def sequence[L, A, B, ValA, ValB](slot: Slot, p1: AbstractParser[L,A], p2: AbstractParser[L,B], s: Int)(
      implicit builder: CanBuildSequence[L,A, B, ValA, ValB]
    ): builder.Sequence = {
      import builder._
      builder.sequence(new AbstractParser[L,T] with Slot {
        def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]): Result[builder.T] = p1(input, i, sppfLookup) flatMap {
          x =>
            p2(input, index(x), sppfLookup).smap {
              intermediate(x, _, this, sppfLookup)
            }
        }

        def size: Int = s

        def symbol = org.meerkat.tree.Sequence(p1.symbol, p2.symbol)

        def ruleType = org.meerkat.tree.PartialRule(slot.ruleType.head, slot.ruleType.body, s)

        override def reset(): Unit = {
          p1.reset()
          p2.reset()
        }

        override def toString = s"[${ruleType.toString()},$size]"
      })
    }

    def altAlt[L, A, B >: A, ValA, ValB >: ValA](
      p1: AbstractAlternationBuilder[L,A, ValA],
      p2: AbstractAlternationBuilder[L,B, ValB]
    )(implicit builder: CanBuildAlternation[L,A, B, ValA, ValB]): builder.AlternationBuilder = builder.builderAlt {
      head =>
        alternation(p1(head), p2(head))
    }

    def altAltSeq[L, A, B >: A, ValA, ValB >: ValA](
      p1: AbstractAlternationBuilder[L,A, ValA],
      p2: AbstractSequenceBuilder[L,B, ValB]
    )(implicit builder: CanBuildAlternation[L,A, B, ValA, ValB]): builder.AlternationBuilder = {
      import builder._
      builderAlt { head =>
        this.alternation(p1(head), alt(head, p2))
      }
    }

    def altSeqAlt[L, A, B >: A, ValA, ValB >: ValA](
      p1: AbstractSequenceBuilder[L,A, ValA],
      p2: AbstractAlternationBuilder[L,B, ValB]
    )(implicit builder: CanBuildAlternation[L,A, B, ValA, ValB]): builder.AlternationBuilder = {
      import builder._
      builderAlt { head =>
        this.alternation(alt(head, p1), p2(head))
      }
    }

    def altAltSym[L, A, B >: A, ValA, ValB >: ValA](p1: AbstractAlternationBuilder[L,A, ValA], p2: AbstractSymbol[L,B, ValB])(
      implicit builder: CanBuildAlternation[L,A, B, ValA, ValB]
    ): builder.AlternationBuilder = {
      import builder._
      builderAlt { head =>
        this.alternation(p1(head), alt(head, p2))
      }
    }

    def altSymAlt[L, A, B >: A, ValA, ValB >: ValA](p1: AbstractSymbol[L,A, ValA], p2: AbstractAlternationBuilder[L,B, ValB])(
      implicit builder: CanBuildAlternation[L,A, B, ValA, ValB]
    ): builder.AlternationBuilder = {
      import builder._
      builderAlt { head =>
        this.alternation(alt(head, p1), p2(head))
      }
    }

    def altSeq[L, A, B >: A, ValA, ValB >: ValA](
      p1: AbstractSequenceBuilder[L,A, ValA],
      p2: AbstractSequenceBuilder[L,B, ValB]
    )(implicit builder: CanBuildAlternation[L,A, B, ValA, ValB]): builder.AlternationBuilder = {
      import builder._
      builderAlt { head =>
        this.alternation(alt(head, p1), alt(head, p2))
      }
    }

    def altSymSeq[L, A, B >: A, ValA, ValB >: ValA](p1: AbstractSymbol[L,A, ValA], p2: AbstractSequenceBuilder[L,B, ValB])(
      implicit builder: CanBuildAlternation[L,A, B, ValA, ValB]
    ): builder.AlternationBuilder = {
      import builder._
      builderAlt { head =>
        this.alternation(alt(head, p1), alt(head, p2))
      }
    }

    def altSeqSym[L, A, B >: A, ValA, ValB >: ValA](p1: AbstractSequenceBuilder[L,A, ValA], p2: AbstractSymbol[L,B, ValB])(
      implicit builder: CanBuildAlternation[L,A, B, ValA, ValB]
    ): builder.AlternationBuilder = {
      import builder._
      builder.builderAlt { head =>
        this.alternation(alt(head, p1), alt(head, p2))
      }
    }

    def altSym[L, A, B >: A, ValA, ValB >: ValA](p1: AbstractSymbol[L,A, ValA], p2: AbstractSymbol[L,B, ValB])(
      implicit builder: CanBuildAlternation[L,A, B, ValA, ValB]
    ): builder.AlternationBuilder = {
      import builder._
      builderAlt { head =>
        this.alternation(alt(head, p1), alt(head, p2))
      }
    }

    def alt[L, B, Val](head: Head[L], p: AbstractSequenceBuilder[L,B, Val])(implicit builder: CanBuildAlternative[L,B]) = {
      import builder._
      new AbstractParser[L,B] with Slot {
        val q = p(this)
        def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) = q(input, i, sppfLookup).map { x =>
          builder.result(x, this, head, sppfLookup)
        }
        def symbol                 = q.symbol
        lazy val ruletype          = { val rule = org.meerkat.tree.Rule(head.symbol, this.symbol); rule.action = p.action; rule }
        def ruleType               = ruletype
        override def reset(): Unit = q.reset()
        override def toString      = s"p${this.hashCode}"
      }
    }

    def alt[L, B, Val](head: Head[L], p: AbstractSymbol[L,B, Val])(implicit builder: CanBuildAlternative[L,B]) = {
      import builder._
      new AbstractParser[L,B] with Slot {
        def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) = p(input, i, sppfLookup).map { x =>
          builder.result(x, this, head, sppfLookup)
        }
        def symbol            = p.symbol
        lazy val ruletype     = { val rule = org.meerkat.tree.Rule(head.symbol, this.symbol); rule.action = p.action; rule }
        def ruleType          = ruletype
        override def reset    = p.reset
        override def toString = s"p${this.hashCode}"
      }
    }

    protected def alternation[L, A, B >: A, ValA, ValB](p1: AbstractParser[L,A], p2: AbstractParser[L,B])(
      implicit builder: CanBuildAlternation[L,A, B, ValA, ValB]
    ): builder.Alternation =
      builder.alternation(new AbstractParser[L,B] {
        def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) =
          p1(input, i, sppfLookup).orElse(p2(input, i, sppfLookup))
        def symbol = org.meerkat.tree.Alt(p1.symbol, p2.symbol)
        override def reset(): Unit = {
          p1.reset()
          p2.reset()
        }
      })

    def map[L, A, B, ValA](p: AbstractSequenceBuilder[L,A, ValA],
                        f: A => B)(implicit builder: CanMap[L,A, B, ValA]): builder.SequenceBuilder = {
      import builder._
      builderSeq { slot =>
        val q = p(slot)
        builder.sequence(new AbstractParser[L,B] with Slot {
          def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) = q(input, i, sppfLookup) map f
          def size                                                = q.size
          def ruleType                                            = q.ruleType
          def symbol                                              = q.symbol
          override def reset(): Unit                              = q.reset()
        })
      }
    }

    def map[L, A, B, ValA](p: AbstractSymbol[L,A, ValA],
                        f: A => B)(implicit builder: CanMap[L,A, B, ValA]): builder.Nonterminal = {
      import builder._
      nonterminal(p.name, new AbstractParser[L,B] {
        def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) = p(input, i, sppfLookup) map f
        def symbol                                              = p.symbol
        override def reset(): Unit                              = p.reset()
      })
    }

    def map[L, A, B, ValA](p: AbstractSymbol[L,A, ValA],
                        f: (Input[L], A) => B)(implicit builder: CanMap[L,A, B, ValA]): builder.Nonterminal = {
      import builder._
      nonterminal(p.name, new AbstractParser[L,B] {
        def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) = p(input, i, sppfLookup) map { f(input, _) }
        def symbol                                              = p.symbol
        override def reset(): Unit                              = p.reset()
      })
    }

    def flatMap[L, A, B, ValA, ValB](p: AbstractSequenceBuilder[L,A, ValA], f: A => AbstractSymbol[L,B, ValB])(
      implicit builder: CanBuildSequence[L,A, B, ValA, ValB]
    ): builder.SequenceBuilder = {
      import builder._
      builderSeq { slot =>
        val q = p(slot)
        builder.sequence(new AbstractParser[L,builder.T] with Slot {
          def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) =
            q(input, i, sppfLookup) flatMap { x =>
              f(x)(input, index(x), sppfLookup).smap {
                intermediate(x, _, this, sppfLookup)
              }
            }
          def size                   = q.size + 1
          def symbol                 = org.meerkat.tree.Sequence(q.symbol, org.meerkat.tree.SimpleNonterminal(s"${f.hashCode}"))
          def ruleType               = org.meerkat.tree.PartialRule(slot.ruleType.head, slot.ruleType.body, size)
          override def toString      = s"[${ruleType.toString()},$size]"
          override def reset(): Unit = q.reset()
        })
      }
    }

//    def flatMap2[A,B,ValA,ValB](p: AbstractSymbol[L,A,ValA], f: A => AbstractSymbol[L,B,ValB])(implicit builder: CanBuildSequence[L,A,B,ValA,ValB]): builder.SequenceBuilder = {
//      import builder._
//      builderSeq { slot =>
//        builder.sequence(new AbstractParser[L,builder.T] with Slot {
//          def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) = p(input,i,sppfLookup) flatMap { x => f(x)(input,index(x),sppfLookup).smap { intermediate(x,_,this,sppfLookup) } }
//          def size = 2; def symbol = org.meerkat.tree.Sequence(p.symbol, org.meerkat.tree.SimpleNonterminal(s"${f.hashCode}"))
//          def ruleType = org.meerkat.tree.PartialRule(slot.ruleType.head, slot.ruleType.body, size)
//          override def toString = s"[${ruleType.toString()},$size]"
//          override def reset = p.reset
//        }) }
//    }
  }
}

object AbstractCPSParsers extends AbstractParsers {
  import AbstractParser._

  type Result[+T] = CPSResult[T]

  def negativeSym[L, A, ValA](
    name: String,
    p: => AbstractSymbol[L,A, ValA]
  )(implicit builder: CanBuildNegative[L,A, ValA], obj: ClassTag[Result[A]]): builder.Nonterminal = {
    import builder._
    lazy val q: Nonterminal = not(name, p); q
  }

  def nonterminalSym[L, A, ValA](name: String, p: => AbstractSymbol[L,A, ValA])(
    implicit builder: CanBuildNonterminal[L,A, ValA],
    b: CanBuildAlternative[L,A],
    obj: ClassTag[Result[A]]
  ): builder.Nonterminal = {
    import builder._
    lazy val q: Nonterminal = nonterminal(name, memoize(alt(q, p))); q
  }

  def nonterminalSeq[L, A, ValA](name: String, p: => AbstractSequenceBuilder[L,A, ValA])(
    implicit builder: CanBuildNonterminal[L,A, ValA],
    b: CanBuildAlternative[L,A],
    obj: ClassTag[Result[A]]
  ): builder.Nonterminal = {
    import builder._
    lazy val q: Nonterminal = nonterminal(name, memoize(alt(q, p))); q
  }

  def nonterminalAlt[L, A, ValA](
    name: String,
    p: => AbstractAlternationBuilder[L,A, ValA]
  )(implicit builder: CanBuildNonterminal[L,A, ValA], obj: ClassTag[Result[A]]): builder.Nonterminal = {
    import builder._
    lazy val q: Nonterminal = builder.nonterminal(name, memoize(p(q))); q
  }


  def regular[L, A, ValA](
    symbol: org.meerkat.tree.NonterminalSymbol,
    p: => AbstractAlternationBuilder[L,A, ValA]
  )(implicit builder: CanBuildEBNF[L, A, ValA], obj: ClassTag[Result[A]]): builder.Regular = {
    import builder._
    lazy val q: Regular = builder.regular(symbol, memoize(p(q))); q
  }

  def groupSeq[L, A, ValA](
    p: => AbstractSequenceBuilder[L,A, _]
  )(implicit builder: CanBuildEBNF[L, A, ValA], b: CanBuildAlternative[L,A], obj: ClassTag[Result[A]]): builder.Group = {
    import builder._
    lazy val q: Group = builder.group(memoize(alt(q, p))); q
  }

  def groupAlt[L, A, ValA](p: => AbstractAlternationBuilder[L,A, _])(implicit builder: CanBuildEBNF[L, A, ValA],
                                                                obj: ClassTag[Result[A]]): builder.Group = {
    import builder._
    lazy val q: Group = builder.group(memoize(p(q))); q
  }

  def preFilter[L, B, Val](p: AbstractSymbol[L,B, Val], pred: (Input[L], Int) => Boolean, prefix: String)(
    implicit builder: CanBuildNonterminal[L,B, Val]
  ): builder.Symbol =
    builder.symbol(new AbstractParser[L,B] {
      def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) =
        if (pred(input, i)) p(input, i, sppfLookup) else CPSResult.failure[B]
      def name              = s"$prefix ${p.name}"
      override def toString = name
      def symbol = p.symbol match {
        case nt: NonterminalSymbol => NonterminalSymbol(name)
        case TerminalSymbol(_)     => TerminalSymbol(name)
        case _                     => throw new RuntimeException("Shouldn't have happened!")
      }
      def action: Option[Any => Val] = None
      override def reset(): Unit     = p.reset()
    })

  def postFilter[L, B, Val](p: AbstractSymbol[L,B, Val], pred: (Input[L], B) => Boolean, postfix: String)(
    implicit builder: CanBuildNonterminal[L,B, Val]
  ): builder.Symbol =
    builder.symbol(new AbstractParser[L,B] {
      def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) = p(input, i, sppfLookup).filter { pred(input, _) }
      def name                                                = s"${p.name} $postfix"
      override def toString: String                           = name
      def symbol = p.symbol match {
        case nt: NonterminalSymbol => NonterminalSymbol(name)
        case TerminalSymbol(_)     => TerminalSymbol(name)
        case _                     => throw new RuntimeException("Shouldn't have happened!")
      }
      def action: Option[Any => Val] = None
      override def reset(): Unit     = p.reset()
    })

  import CPSResult.memo

  protected def memoize[L, A: Memoizable](
    p: => AbstractParser[L,A]
  )(implicit obj: ClassTag[Result[A]]): AbstractParser[L,A] = {
    lazy val q: AbstractParser[L,A] = p
    val results                   = new mutable.HashMap[Int, Result[A]]()
    new AbstractParser[L,A] {
      def apply(input: Input[L], i: Int, sppfLookup: SPPFLookup[L]) =
        results.get(i) match {
          case Some(res) => res
          case _ =>
            val res = memo(q(input, i, sppfLookup))
            results += (i -> res)
            res
        }
      def symbol = q.symbol
      override def reset(): Unit = {
        val done = results.isEmpty
        if (!done) {
          results.clear()
          q.reset()
        }
      }
    }
  }

}
