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

package org.meerkat

import org.meerkat.input.Input
import org.meerkat.util._
import org.meerkat.sppf.SPPFLookup
import org.meerkat.sppf.DefaultSPPFLookup
import org.meerkat.sppf.SemanticAction
import org.meerkat.sppf.TreeBuilder
import org.meerkat.sppf.NonPackedNode
import org.meerkat.util.converters.{BFSConverter, DFSConverter, EnumeratingConverter$, extractNonAmbiguousSPPFs}

import scala.collection.mutable

package object parsers {

  case class ~[+A, +B](_1: A, _2: B)

  trait <:<[-A, B]
  trait <:!<[A, B]

  type ![T] = { type f[U] = U <:!< T }

  sealed trait NoValue

  trait |~|[-A, B] { type R }

  type &[A <: { type Abstract[_] }, T] = A#Abstract[T]

  type &&[T] = { type action[F] }

  trait EBNF[-Val] {
    type OptOrSeq; type Seq; type Group
    val add: (OptOrSeq ~ Val) => OptOrSeq
    val unit: Val => OptOrSeq
    val empty: String => OptOrSeq
    val group: Val => Group
  }

  object |~| {
    implicit def f1[A <: NoValue, B <: NoValue]       = new |~|[NoValue, NoValue] { type R = NoValue }
    implicit def f2[A <: NoValue, B: ![NoValue]#f]    = new |~|[NoValue, B]       { type R = B       }
    implicit def f3[A: ![NoValue]#f, B <: NoValue]    = new |~|[A, NoValue]       { type R = A       }
    implicit def f4[A: ![NoValue]#f, B: ![NoValue]#f] = new |~|[A, B]             { type R = A ~ B   }
  }

  object <:< {
    implicit def sub[A, B >: A]: A <:< B = null
  }

  object <:!< {
    implicit def nsub[A, B]: A <:!< B          = null
    implicit def nsubAmb1[A, B >: A]: A <:!< B = null
    implicit def nsubAmb2[A, B >: A]: A <:!< B = null
  }

  object EBNF {
    implicit val ebnf1 = new EBNF[NoValue] {
      type OptOrSeq = NoValue; type Group = NoValue
      val add: (OptOrSeq ~ NoValue) => OptOrSeq = _ => null
      val unit: NoValue => OptOrSeq             = _ => null
      val empty: String => OptOrSeq             = _ => null
      val group: NoValue => Group               = _ => null
    }

    implicit def ebnf2[Val: ![NoValue]#f] = new EBNF[Val] {
      type OptOrSeq = List[Val]; type Group = Val
      val add: (OptOrSeq ~ Val) => OptOrSeq = { case s ~ x => s.:+(x) }
      val unit: Val => OptOrSeq             = x => List(x)
      val empty: String => OptOrSeq         = _ => List()
      val group: Val => Group               = x => x
    }
  }

  type Prec = (Int, Int)
  val $ : Prec = (0, 0)

  def run[L, N, T](input: Input[L, N], sppfs: SPPFLookup[L, N], parser: AbstractCPSParsers.AbstractParser[L, N, T])
                  (parserAction: T => Unit = (_: T) => {}): Unit = {
    parser(input, 0, sppfs)(parserAction)
    Trampoline.run
  }

  def getSPPFLookup[L, N, T, V](parser: AbstractCPSParsers.AbstractSymbol[L, N,T, V], input: Input[L, N]): DefaultSPPFLookup[L, N] = {
    val sppfLookup = new DefaultSPPFLookup[L, N](input)
    run(input, sppfLookup, parser)(t => {})
    sppfLookup
  }

  def runWithStatistics(action: => Unit): ParseTimeStatistics = {
    val startUserTime   = getUserTime
    val startSystemTime = getCpuTime
    val startNanoTime   = System.nanoTime

    action

    val endUserTime   = getUserTime
    val endSystemTime = getCpuTime
    val endNanoTime   = System.nanoTime

    ParseTimeStatistics(
      (endNanoTime - startNanoTime) / 1000000,
      (endUserTime - startUserTime) / 1000000,
      (endSystemTime - startSystemTime) / 1000000
    )
  }

  def getSPPFs[L, N, T, V](
    parser: AbstractCPSParsers.AbstractSymbol[L, N, T, V],
    input: Input[L, N]
  ): ParseResult[ParseError, (List[T], ParseTimeStatistics, SPPFStatistics)] = {
    parser.reset()
    val sppfLookup = new DefaultSPPFLookup[L, N](input)
    val roots = mutable.MutableList[T]()
    val parseTimeStatistics = runWithStatistics {
      run(input, sppfLookup, parser)(t => roots += t)
    }
    val sppftatistics = SPPFStatistics(sppfLookup)

    if (roots.isEmpty)
      Left(ParseError(0, " "))
    else
      Right((roots.toList, parseTimeStatistics, sppftatistics))
  }
  def getAllSPPFs[L, N, T, V](
    parser: AbstractCPSParsers.AbstractSymbol[L, N, T, V],
    input: Input[L, N]
  ): List[T] = {
    parser.reset()
    val sppfLookup = new DefaultSPPFLookup[L, N](input)
    val nodesCount = input.edgesCount
    parser.reset()

    val roots = mutable.MutableList[T]()
    for (i <- 0 until nodesCount) {
      val res = parser(input, i, sppfLookup)(t => roots += t)
      Trampoline.run
    }

    roots.toList
    //(0 until nodesCount).flatMap(i => {sppfLookup.getStartNodes(parser, i, input.edgesCount).toList}).flatten.toList
  }

  def executeQuery[L, N, T <: NonPackedNode, V](
    parser: AbstractCPSParsers.AbstractSymbol[L, N, T, V],
    input: Input[L, N]): Stream[V] =
      extractNonAmbiguousSPPFs(getAllSPPFs(parser, input), DFSConverter)
        .map(sppf => SemanticAction.execute(sppf)(input))
        .map{ case (x: V) => x }


  def getSPPF[L, N, T, V](
    parser: AbstractCPSParsers.AbstractSymbol[L, N, T, V],
    input: Input[L, N]
  ): ParseResult[ParseError, (NonPackedNode, ParseTimeStatistics, SPPFStatistics)] = {
    parser.reset()
    val sppfLookup = new DefaultSPPFLookup[L, N](input)
    val parseTimeStatistics = runWithStatistics {
      run(input, sppfLookup, parser)(t => {})
    }
    val sppftatistics = SPPFStatistics(sppfLookup)
    sppfLookup.getStartNode(parser, 0, input.edgesCount) match {
      case None       => Left(ParseError(0, " "))
      case Some(root) => Right((root, parseTimeStatistics, sppftatistics))
    }
  }

  def parse[L, N, T, V](parser: AbstractCPSParsers.AbstractSymbol[L, N,T, V],
                  input: Input[L, N]): ParseResult[ParseError, ParseSuccess] =
    getSPPF(parser, input) match {
      case Left(error) => Left(error)
      case Right((root, parseTimeStat, sppfStat)) => {
        val startUserTime   = getUserTime
        val startSystemTime = getCpuTime
        val startNanoTime   = System.nanoTime

        val t = TreeBuilder.build(root)(input)

        val endUserTime   = getUserTime
        val endSystemTime = getCpuTime
        val endNanoTime   = System.nanoTime

        val treeBuildingStatistics = TreeBuildingStatistics(
          (endNanoTime - startNanoTime) / 1000000,
          (endUserTime - startUserTime) / 1000000,
          (endSystemTime - startSystemTime) / 1000000
        )
        val treeStatistics = TreeStatistics(0, 0, 0)

        Right(
          ParseSuccess(
            t,
            parseTimeStat,
            treeBuildingStatistics,
            sppfStat,
            treeStatistics
          )
        )
      }
    }

  def parseGraph[L, N, T <: NonPackedNode, V](parser: AbstractCPSParsers.AbstractSymbol[L, N, T, V],
                       input: Input[L, N]): ParseResult[ParseError, ParseGraphSuccess] =
    getSPPFs(parser, input) match {
      case Left(error) => Left(error)
      case Right((roots, parseTimeStat, sppfStat)) => {
        Right(ParseGraphSuccess(roots, parseTimeStat, sppfStat))
      }
    }

  def parseGraphAndGetSppfStatistics[L, N, T <: NonPackedNode, V](parser: AbstractCPSParsers.AbstractSymbol[L, N,T, V],
                                           input: Input[L, N]): Option[SPPFStatistics] =
    parseGraph(parser, input).map { case ParseGraphSuccess(_, _, stat) => stat }
      .left.map{x => println(x); x}.toOption


  def exec[L, N, T, V](parser: AbstractCPSParsers.AbstractSymbol[L, N,T, V],
                       input: Input[L, N]): ParseResult[ParseError, V] =
    getSPPF(parser, input) match {
      case Left(error) => Left(error)
      case Right((root, parseTimeStat, sppfStat)) => {
        val x = SemanticAction.execute(root)(input).asInstanceOf[V]
        Right(x)
      }
    }

  def execGraph[L, N, T <: NonPackedNode, V](parser: AbstractCPSParsers.AbstractSymbol[L, N,T, V],
                      input: Input[L, N]): ParseResult[ParseError, ParseSemanticSuccess[V]] =
    getSPPFs(parser, input) match {
      case Left(error) => Left(error)
      case Right((roots, parseTimeStat, sppfStat)) => {
        val x = roots.map(root => SemanticAction.execute(root)(input).asInstanceOf[V])
        Right(ParseSemanticSuccess(x, parseTimeStat, sppfStat))
      }
    }

  type ParseResult[A, B] = Either[A, B]

  implicit class ParseResultOps[A, B](result: ParseResult[A, B]) {
    def isSuccess = result.isRight
    def isFailure = result.isLeft
    def asSuccess = result.right.get
    def asFailure = result.left.get
  }

}
